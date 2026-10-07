package blackhole;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.Autotiler.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.liquid.*;

import static mindustry.Vars.*;

/**
 * Aurelia block classes that need small changes to vanilla behaviour so that nothing vanilla leaks in:
 * placement replacements that would otherwise insert vanilla junctions / bridges, a conduit icon built from vanilla
 * sprites, and a mender whose heal effect is hardcoded in vanilla.
 */
public final class AureliaParts{
    private AureliaParts(){}

    /** Conveyor whose drag-placement junction / bridge replacements are Aurelia blocks (vanilla forces its own). */
    public static class LumenConveyor extends Conveyor{
        public Block junction, bridge;

        public LumenConveyor(String name){
            super(name);
        }

        @Override
        public void init(){
            super.init();
            junctionReplacement = junction;
            bridgeReplacement = bridge;
        }
    }

    /**
     * Battery drawn from its baked 3D model.
     *
     * <p>Vanilla {@link mindustry.world.blocks.power.Battery} installs a default
     * {@code DrawMulti(DrawDefault, DrawPower, DrawRegion("-top"))} and builds its menu / placement icon from it.
     * That happens while the mod's sprites are packed, i.e. before {@code Models.install()} can swap the drawer in,
     * so the generated icon contained the atlas error region - the "oh no" players saw on the lumen cell. The model
     * drawer is therefore installed here (before any icon work) and the icon is taken straight from the baked
     * {@code -preview} sprite.
     */
    public static class LumenCellBlock extends mindustry.world.blocks.power.Battery{
        public BlockModel model;

        public LumenCellBlock(String name){
            super(name);
        }

        @Override
        public void load(){
            model = Models.get(name);
            if(model != null){
                model.load();
                if(!(drawer instanceof blackhole.g3d.DrawModel)) drawer = new blackhole.g3d.DrawModel(model);
            }
            super.load();
        }

        @Override
        public TextureRegion[] icons(){
            return new TextureRegion[]{Core.atlas.find(name + "-preview", region)};
        }

        @Override
        public void drawPlanRegion(mindustry.entities.units.BuildPlan plan, arc.util.Eachable<mindustry.entities.units.BuildPlan> list){
            Draw.rect(Core.atlas.find(name + "-preview", region), plan.drawx(), plan.drawy(), size * 8f, size * 8f);
        }

        @Override
        public void getRegionsToOutline(arc.struct.Seq<TextureRegion> out){
            //baked model carries its own outline
        }
    }

    /** Conduit with an own bottom sprite in its icon and Aurelia (or no) replacements. */
    public static class LumenConduit extends Conduit{
        public Block bridge;

        public LumenConduit(String name){
            super(name);
            buildType = LumenConduitBuild::new;
        }

        @Override
        public void init(){
            super.init();
            //no Aurelia liquid junction yet: crossing lines keep the conduit instead of inserting a vanilla junction
            junctionReplacement = null;
            bridgeReplacement = bridge;
            rotBridgeReplacement = null;
        }

        @Override
        public TextureRegion[] icons(){
            return new TextureRegion[]{botRegions[0], topRegions[0]};
        }

        /**
         * Draws the carried liquid itself.
         *
         * <p>Vanilla's {@code Conduit.ConduitBuild.drawAt} renders the fluid with the shared animated
         * {@code renderer.fluidFrames} quad, sized and padded for the vanilla conduit trough, at an alpha of
         * {@code smoothLiquid} - with Aurelia's own (wider, prism-glass) trough sprites that quad ends up under the
         * opaque rails and at a very low alpha, so the solution looked completely transparent. Aurelia therefore
         * tints its own bottom sprite with the liquid colour - the exact shape of this conduit's channel - with a
         * floor on the alpha so a moving trickle is still clearly readable, plus a soft additive bloom for the
         * emissive lumen plasma.
         */
        public class LumenConduitBuild extends ConduitBuild{
            @Override
            protected void drawAt(float x, float y, int bits, int rotation, SliceMode slice, boolean under){
                float angle = rotation * 90f;
                TextureRegion bot = sliced(botRegions[bits], slice);
                if(under){
                    Draw.rect(bot, x, y, angle);
                    return;
                }

                Liquid liquid = liquids.current();
                float fill = Math.max(smoothLiquid, liquids.currentAmount() / liquidCapacity);
                if(liquid != null && fill > 0.0015f){
                    float alpha = Mathf.clamp(0.35f + fill * 0.65f);
                    Draw.color(liquid.color, alpha);
                    Draw.rect(bot, x, y, angle);
                    if(liquid.lightColor.a > 0.01f || liquid == AureliaContent.lumenPlasma){
                        Draw.blend(Blending.additive);
                        Draw.color(liquid.color, 0.14f * fill * (0.75f + 0.25f * Mathf.absin(Time.time, 9f, 1f)));
                        Draw.rect(bot, x, y, angle);
                        Draw.blend();
                    }
                    Draw.color();
                }

                Draw.rect(sliced(topRegions[bits], slice), x, y, angle);
            }
        }
    }

    /** Mender drawn as a 3D model, healing with the mod's own effect. */
    public static class LumenMenderBlock extends MendProjector{
        public BlockModel model;

        public LumenMenderBlock(String name){
            super(name);
            buildType = LumenMenderBuild::new;
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

        public class LumenMenderBuild extends MendBuild{
            @Override
            public void updateTile(){
                boolean canHeal = !checkSuppression();
                smoothEfficiency = Mathf.lerpDelta(smoothEfficiency, efficiency, 0.08f);
                heat = Mathf.lerpDelta(heat, efficiency > 0 && canHeal ? 1f : 0f, 0.08f);
                charge += heat * delta();
                phaseHeat = Mathf.lerpDelta(phaseHeat, optionalEfficiency, 0.1f);
                if(optionalEfficiency > 0 && timer(timerUse, useTime / timeScale) && canHeal) consume();

                if(charge >= reload && canHeal){
                    float realRange = range + phaseHeat * phaseRangeBoost;
                    charge = 0f;
                    boolean[] any = {false};
                    indexer.eachBlock(this, realRange, b -> b.damaged() && !b.isHealSuppressed(), other -> {
                        other.heal(other.maxHealth() * (healPercent + phaseHeat * phaseBoost) / 100f * efficiency);
                        other.recentlyHealed();
                        OwnFx.heal.at(other.x, other.y, other.block.size, baseColor);
                        any[0] = true;
                    });
                    if(any[0]){
                        mendSound.at(this, 1f + Mathf.range(0.1f), mendSoundVolume);
                        OwnFx.spawn.at(x, y, 0f, baseColor);
                    }
                }
            }

            @Override
            public void drawCached(){
            }

            @Override
            public void draw(){
                if(model != null) model.draw(this);
                else Draw.rect(block.region, x, y);
            }
        }
    }
}
