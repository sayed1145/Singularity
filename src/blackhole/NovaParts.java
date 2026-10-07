package blackhole;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import blackhole.g3d.BlockModel;
import blackhole.models.*;
import arc.math.geom.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * v7.8 block classes.
 *
 * <p>Two groups:
 *
 * <ul>
 * <li>{@link ResonanceHub} - the resonance field, the new mechanic of this version.</li>
 * </ul>
 */
public final class NovaParts{
    private NovaParts(){}

    // =================================================================================================
    // transport family
    // =================================================================================================

    /**
     * A transport block with no configuration at all.
     *
     * <p>Every tick it holds an item it looks at its neighbours and picks the best one: a machine that consumes
     * the item first, then a core or vault that stores it, then another transport block, and it never hands an
     * item straight back to whoever gave it. That single rule makes it a conveyor (it moves things along), a
     * router (it splits between several targets) and a sorter (a neighbour that cannot take the item is simply
     * skipped) at the same time. {@code pullStorage} adds the fourth role: it empties adjacent containers by
     * itself, but only while something downstream still wants the item.
     */
    public static class ModelPowerNode extends PowerNode{
        public BlockModel model;

        public ModelPowerNode(String name){
            super(name);
            buildType = ModelPowerNodeBuild::new;
        }

        @Override
        public void load(){
            super.load();
            model = Models.get(name);
            if(model != null) model.load();
        }

        @Override
        public TextureRegion[] icons(){
            TextureRegion r = Core.atlas.find(name + "-preview");
            return r.found() ? new TextureRegion[]{r} : super.icons();
        }

        @Override
        public void getRegionsToOutline(Seq<TextureRegion> out){
        }

        public class ModelPowerNodeBuild extends PowerNodeBuild{
            @Override
            public void drawCached(){
                //the body is drawn live from the model, not into the static block cache
            }

            @Override
            public void draw(){
                if(model != null) model.draw(this);
                //vanilla PowerNodeBuild.draw only draws the power lasers
                super.draw();
            }
        }
    }

    // =================================================================================================
    // 共振枢纽 - resonance hub: the new v7.8 mechanic
    // =================================================================================================

    /**
     * The resonance field.
     *
     * <p>Fighting units charge it: every friendly unit inside the radius that is firing feeds the hub. When the
     * bank fills, the hub releases it in one pulse - every friendly unit in the radius is overloaded for a few
     * seconds (faster, harder hitting) and every friendly building in the radius is repaired. Then the bank is
     * empty and the hub has to be filled again, so it rewards fighting near it instead of running on its own.
     */
    public static class ResonanceHub extends Block{
        public float radius = 200f;
        public float capacity = 1000f;
        /** charge per second from one firing unit, and the trickle the hub makes on its own */
        public float chargePerUnit = 26f, idleCharge = 6f;
        /** how long the overload lasts, and how long the hub needs before it may fire again */
        public float overloadDuration = 360f, cooldown = 420f;
        public float healPercent = 12f;
        public StatusEffect overload;
        public Color fieldColor = Color.valueOf("b69cff");
        public BlockModel model;

        public ResonanceHub(String name){
            super(name);
            update = true;
            solid = true;
            hasPower = true;
            hasItems = false;
            group = BlockGroup.projectors;
            envEnabled = Env.any;
            buildType = ResonanceHubBuild::new;
        }

        @Override
        public void load(){
            super.load();
            model = Models.get(name);
            if(model != null) model.load();
        }

        @Override
        public TextureRegion[] icons(){
            TextureRegion r = Core.atlas.find(name + "-preview");
            return r.found() ? new TextureRegion[]{r} : super.icons();
        }

        @Override
        public void getRegionsToOutline(Seq<TextureRegion> out){
        }

        @Override
        public void setBars(){
            super.setBars();
            addBar("resonance", (ResonanceHubBuild b) -> new mindustry.ui.Bar(
                () -> Core.bundle.format("bar.resonance", (int)b.charge, (int)capacity),
                () -> fieldColor, () -> b.charge / capacity));
        }

        @Override
        public void setStats(){
            super.setStats();
            stats.add(Stat.range, radius / tilesize, StatUnit.blocks);
            stats.add(Stat.reload, 60f / (cooldown / 60f) / 60f, StatUnit.none);
        }

        @Override
        public void drawPlace(int x, int y, int rotation, boolean valid){
            super.drawPlace(x, y, rotation, valid);
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, radius, fieldColor);
        }

        public class ResonanceHubBuild extends Building{
            public float charge, wave, cool;

            @Override
            public void updateTile(){
                if(efficiency <= 0.001f) return;
                if(cool > 0f){
                    cool -= Time.delta;
                    return;
                }
                float gain = idleCharge;
                int fighters = 0;
                for(Unit u : Groups.unit){
                    if(u.team != team || !u.within(this, radius)) continue;
                    if(u.isShooting()) fighters++;
                }
                gain += fighters * chargePerUnit;
                charge = Math.min(capacity, charge + gain * Time.delta / 60f * efficiency);

                if(charge >= capacity){
                    release();
                }
            }

            /** one pulse: every friendly unit is overloaded, every friendly building is repaired. */
            public void release(){
                charge = 0f;
                cool = cooldown;
                wave = 1f;
                if(overload != null){
                    Units.nearby(team, x, y, radius, u -> u.apply(overload, overloadDuration));
                }
                indexer.eachBlock(this, radius, b -> b.damaged(), b -> {
                    b.heal(b.maxHealth() * healPercent / 100f);
                    Fx.healBlockFull.at(b.x, b.y, b.block.size, fieldColor, b.block);
                });
                Effect.shake(3f, 12f, this);
                Fx.dynamicWave.at(x, y, radius / 4f, fieldColor);
            }

            @Override
            public void draw(){
                if(model != null) model.draw(this); else super.draw();
                if(wave > 0f){
                    wave = Math.max(0f, wave - Time.delta / 45f);
                    Draw.z(Layer.effect);
                    Draw.color(fieldColor, wave * 0.5f);
                    Lines.stroke(2f * wave);
                    Lines.circle(x, y, radius * (1f - wave));
                    Draw.reset();
                }
            }

            @Override
            public void drawSelect(){
                Drawf.dashCircle(x, y, radius, fieldColor);
            }

            @Override
            public void write(Writes write){
                super.write(write);
                write.f(charge);
                write.f(cool);
            }

            @Override
            public void read(Reads read, byte revision){
                super.read(read, revision);
                charge = read.f();
                cool = read.f();
            }
        }
    }
}
