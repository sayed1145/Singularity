package blackhole;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.content.Fx;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * v8.2 parts: the two overdrive projectors and the vented siege weapon.
 *
 * <p>Both are deliberately bounded. The projectors use the vanilla rule that overlapping boosts take the
 * highest value instead of adding up, so a field of them can never exceed one block's ceiling; the vented
 * weapon has a shot bank that overheats and locks the gun out, so the titan cannot hold a beam forever.
 */
public final class CommandParts{
    private CommandParts(){}

    // =====================================================================================================
    // overdrive projectors
    // =====================================================================================================

    /** Overdrive projector drawn by its own 3D model. Vanilla mechanics, Aurelia parts. */
    public static class ModelOverdrive extends OverdriveProjector{
        public BlockModel model;

        public ModelOverdrive(String name){
            super(name);
            buildType = OverBuild::new;
        }

        @Override
        public void load(){
            super.load();
            model = Models.get(name);
            if(model != null) model.load();
        }

        @Override
        public TextureRegion[] icons(){
            return new TextureRegion[]{Core.atlas.find(name + "-preview", region)};
        }

        @Override
        public void getRegionsToOutline(Seq<TextureRegion> out){
        }

        @Override
        public void setStats(){
            super.setStats();
            //the ceiling is what matters for balance: two projectors on the same building do not add up
            stats.add(Stat.boostEffect, (speedBoost + speedBoostPhase) * 100f, StatUnit.percent);
        }

        public class OverBuild extends OverdriveBuild{
            @Override
            public void draw(){
                if(model != null){
                    model.draw(this);
                    //a slow ring pulse while it is actually boosting - the only animation it has
                    if(efficiency > 0.01f){
                        float f = Mathf.absin(Time.time, 22f, 1f);
                        Draw.z(Layer.effect);
                        Draw.color(baseColor, phaseColor, phaseHeat);
                        Draw.alpha(0.22f + 0.18f * f);
                        Lines.stroke(1.1f);
                        Lines.circle(x, y, (size * tilesize / 2f) + 1.4f + f * 1.6f);
                        Draw.reset();
                    }
                }else{
                    super.draw();
                }
            }
        }
    }

    // =====================================================================================================
    // the vented siege weapon
    // =====================================================================================================

    /**
     * A weapon with a shot bank. Every shot adds heat; at the ceiling the gun vents and cannot fire for
     * {@link #ventTime}. Heat bleeds off at {@link #coolRate} per second while it is working, so sustained
     * fire is capped no matter how the unit is microed - this is the upper bound on the titan's damage.
     *
     * <p>State lives per unit id in a small map that is swept of dead units, so nothing accumulates.
     */
    public static class VentWeapon extends Weapon{
        /** heat added per shot, heat ceiling, heat removed per second, lockout after a vent */
        public float heatPerShot = 1f, heatCeiling = 4f, coolRate = 0.55f, ventTime = 60f * 6f;
        public Color ventColor = Color.valueOf("ff9e6b");

        /** unit id -> {heat, lock, lastTotalShots} */
        private transient IntMap<float[]> state = new IntMap<>();
        private transient float sweep;

        public VentWeapon(String name){
            super(name);
        }

        @Override
        public Weapon copy(){
            Weapon w = super.copy();
            //Weapon.copy() is a shallow clone: without this, a mirrored copy would write into the same heat
            //bank as the original and both mounts would cook each other's counters
            if(w instanceof VentWeapon v) v.state = new IntMap<>();
            return w;
        }

        public float heatOf(Unit unit){
            float[] st = state.get(unit.id);
            return st == null ? 0f : st[0] / Math.max(heatCeiling, 0.001f);
        }

        public boolean locked(Unit unit){
            float[] st = state.get(unit.id);
            return st != null && st[1] > 0f;
        }

        @Override
        public void update(Unit unit, WeaponMount mount){
            float[] st = state.get(unit.id);
            if(st == null){
                st = new float[]{0f, 0f, mount.totalShots};
                state.put(unit.id, st);
            }

            //count the shots that actually left the barrel
            int fired = mount.totalShots - (int)st[2];
            if(fired > 0){
                st[0] += heatPerShot * fired;
                st[2] = mount.totalShots;
            }else if(mount.totalShots < (int)st[2]){
                st[2] = mount.totalShots;
            }

            if(st[1] > 0f){
                //venting: the gun is dead weight, it may still track but it will not fire
                st[1] -= Time.delta;
                mount.shoot = false;
                mount.reload = Math.max(mount.reload, reload * 0.5f);
                if(st[1] <= 0f){
                    st[0] = 0f;
                }else if(Mathf.chanceDelta(0.25f) && !headless){
                    Tmp.v1.trns(unit.rotation + 90f, y, -x);
                    Fx.steam.at(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, ventColor);
                }
            }else{
                st[0] = Math.max(0f, st[0] - coolRate * Time.delta / 60f);
                if(st[0] >= heatCeiling){
                    st[1] = ventTime;
                    mount.shoot = false;
                    if(!headless) Fx.shootBigSmoke2.at(unit.x, unit.y, unit.rotation, ventColor);
                }
            }

            super.update(unit, mount);

            //cheap periodic sweep so the map cannot grow with dead units
            sweep += Time.delta;
            if(sweep > 600f){
                sweep = 0f;
                IntSeq ids = state.keys().toSeq();
                for(int i = 0; i < ids.size; i++){
                    int id = ids.get(i);
                    if(Groups.unit.getByID(id) == null) state.remove(id);
                }
            }
        }

        @Override
        public void addStats(UnitType u, arc.scene.ui.layout.Table t){
            super.addStats(u, t);
            t.row();
            t.add(Core.bundle.format("bh.vent.stat", (int)(heatCeiling / Math.max(heatPerShot, 0.001f)),
                Strings.autoFixed(ventTime / 60f, 1))).padTop(4f).left();
        }
    }
}
