package blackhole;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.ExplosionShield;
import mindustry.logic.Ranged;
import mindustry.world.meta.*;
import mindustry.ui.Bar;
import mindustry.logic.LAccess;

import static mindustry.Vars.*;

/**
 * v8.3 block logic: the waste silo and the ward dome.
 */
public final class CycleParts{
    private CycleParts(){}

    // =====================================================================================================
    // waste silo
    // =====================================================================================================

    /**
     * A storage block that takes exactly one item: industrial waste. Factories burp waste out in bursts and
     * the reclaimer line eats it at a steady rate, so the cycle needs a buffer - but a general container
     * would just fill up with everything on the belt and stall the chain.
     */
    public static class WasteSilo extends StorageBlock{
        public BlockModel model;

        public WasteSilo(String name){
            super(name);
            update = true;
            solid = true;
            hasItems = true;
            coreMerge = false;
            group = BlockGroup.none;
            buildType = WasteSiloBuild::new;
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
            stats.add(Stat.input, AureliaCycle.industrialWaste.localizedName);
        }

        public class WasteSiloBuild extends StorageBuild{
            @Override
            public boolean acceptItem(Building source, Item item){
                return item == AureliaCycle.industrialWaste && items.total() < itemCapacity;
            }

            @Override
            public void draw(){
                if(model != null){
                    model.draw(this);
                }else{
                    super.draw();
                }
            }
        }
    }

    // =====================================================================================================
    // ward dome
    // =====================================================================================================

    /**
     * Area shield with a bank, a ceiling and a real cool-down.
     *
     * <p>Everything about it is written here rather than inherited from the vanilla force projector, because
     * the vanilla one draws a shader-filled bubble and this one must not look like that. The dome is drawn as
     * a thin double rim with a slow hex ripple and small, short-lived impact rings - no fill, no bloom, no
     * flashing. It reads as "a line you cannot cross", not as a light show.
     *
     * <p>Bounds, so it can never be a wall that holds forever:
     * <ul>
     * <li>the bank is capped at {@link #shieldCapacity} (plus the item bonus) and regenerates at
     *     {@link #regen} per second while it is intact;</li>
     * <li>when the bank empties the dome <b>breaks</b>: it absorbs nothing at all for
     *     {@link #breakCooldown} ticks, and comes back fully restored;</li>
     * <li>it only works on power; cutting the power drops the dome immediately.</li>
     * </ul>
     *
     * <p>Two upgrades, both optional and both bounded: a coolant feed widens the dome and speeds the rebuild,
     * and one resonance shard deepens the bank for a while.
     */
    public static class WardDome extends Block{
        public BlockModel model;

        /** dome radius in world units */
        public float radius = 26f * 8f;
        /** the bank: damage it can swallow before it breaks */
        public float shieldCapacity = 7000f;
        /** bank rebuilt per second while the dome is up */
        public float regen = 120f;
        /** dead time after a break */
        public float breakCooldown = 60f * 14f;

        /** optional coolant feed */
        public @Nullable Liquid boostLiquid;
        public float boostLiquidAmount = 9f / 60f;
        public float liquidRadiusBoost = 8f * 8f;
        public float liquidRegenBoost = 1.9f;

        /** optional item charge */
        public @Nullable Item boostItem;
        public float itemCapacityBoost = 5200f;
        public float itemBoostDuration = 60f * 22f;

        public Color domeColor = Color.valueOf("8fe9ff");
        public Color chargeColor = Color.valueOf("b69cff");
        public Color brokenColor = Color.valueOf("ff9e6b");

        public WardDome(String name){
            super(name);
            update = true;
            solid = true;
            hasPower = true;
            hasLiquids = true;
            hasItems = true;
            canOverdrive = false;
            group = BlockGroup.projectors;
            flags = EnumSet.of(BlockFlag.shield);
            liquidCapacity = 60f;
            itemCapacity = 20;
            buildType = WardDomeBuild::new;
        }

        @Override
        public void init(){
            if(boostLiquid != null) consumeLiquid(boostLiquid, boostLiquidAmount).boost();
            if(boostItem != null) consumeItem(boostItem).boost();
            super.init();
            clipSize = Math.max(clipSize, (radius + liquidRadiusBoost) * 2f + 32f);
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
        public void setBars(){
            super.setBars();
            addBar("shield", (WardDomeBuild b) -> new Bar(
                () -> Core.bundle.format("bar.blackhole-ward-capacity", Strings.fixed(b.shield, 0), Strings.fixed(b.realCapacity(), 0)),
                () -> b.cooldown > 0f ? brokenColor : domeColor,
                () -> b.cooldown > 0f ? 0f : Mathf.clamp(b.shield / b.realCapacity())).blink(Color.white));
            addBar("rebuild", (WardDomeBuild b) -> new Bar(
                () -> b.cooldown > 0f ? Core.bundle.format("bar.blackhole-ward-rebuild", Strings.fixed(b.cooldown / 60f / (b.liquidBoost() ? liquidRegenBoost : 1f), 1))
                    : Core.bundle.get(b.efficiency > 0.01f ? "bar.blackhole-ward-ready" : "bar.blackhole-ward-unpowered"),
                () -> b.cooldown > 0f ? brokenColor : domeColor,
                () -> 1f - Mathf.clamp(b.cooldown / breakCooldown)));
        }

        @Override
        public void setStats(){
            super.setStats();
            stats.add(Stat.shieldHealth, shieldCapacity, StatUnit.none);
            stats.add(Stat.regenerationRate, regen, StatUnit.perSecond);
            stats.add(Stat.range, radius / tilesize, StatUnit.blocks);
            stats.add(Stat.cooldownTime, breakCooldown / 60f, StatUnit.seconds);
            if(boostLiquid != null){
                stats.add(Stat.boostEffect, Core.bundle.format("bh.ward.liquid",
                    (int)(liquidRadiusBoost / tilesize), Strings.autoFixed(liquidRegenBoost, 1)));
            }
            if(boostItem != null){
                stats.add(Stat.affinities, Core.bundle.format("bh.ward.item",
                    boostItem.localizedName, (int)itemCapacityBoost, Strings.autoFixed(itemBoostDuration / 60f, 0)));
            }
        }

        @Override
        public void drawPlace(int x, int y, int rotation, boolean valid){
            super.drawPlace(x, y, rotation, valid);
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, radius, domeColor);
        }

        public class WardDomeBuild extends Building implements Ranged, ExplosionShield{
            /** damage the bank can still take */
            public float shield = shieldCapacity;
            /** ticks of dead time left after a break; > 0 means the dome is down */
            public float cooldown;
            /** ticks left of the item charge */
            public float charge;
            /** 0..1 smoothed "the dome is standing" value, used for drawing only */
            public float warmup;
            /** 0..1 flash after a hit */
            public float hitAlpha;
            /** rolling list of impacts: angle, age */
            public final FloatSeq hits = new FloatSeq();

            public float realRadius(){
                return radius + (liquidBoost() ? liquidRadiusBoost : 0f);
            }

            public float realCapacity(){
                return shieldCapacity + (charge > 0f ? itemCapacityBoost : 0f);
            }

            public boolean liquidBoost(){
                return boostLiquid != null && liquids.get(boostLiquid) >= boostLiquidAmount;
            }

            /** the dome is standing and will swallow shots */
            public boolean up(){
                return enabled && cooldown <= 0f && efficiency > 0.01f && shield > 0.001f;
            }

            @Override
            public void updateTile(){
                hitAlpha = Math.max(0f, hitAlpha - Time.delta / 24f);
                for(int i = hits.size - 2; i >= 0; i -= 2){
                    hits.set(i + 1, hits.get(i + 1) + Time.delta / 26f);
                    if(hits.get(i + 1) >= 1f){
                        hits.removeRange(i, i + 1);
                    }
                }

                if(charge > 0f) charge = Math.max(0f, charge - Time.delta);
                shield = Math.min(shield, realCapacity());

                if(cooldown > 0f){
                    //v8.4: powered rebuild; return FULL, not with a 0.15 HP bank that breaks again.
                    if(efficiency > 0.01f && enabled){
                        cooldown = Math.max(0f, cooldown - edelta() * (liquidBoost() ? liquidRegenBoost : 1f));
                    }
                    shield = cooldown <= 0f ? realCapacity() : 0f;
                    warmup = Mathf.lerpDelta(warmup, 0f, 0.08f);
                    return;
                }

                if(efficiency <= 0.01f || !enabled){
                    //no power, no dome. The bank is kept, so a brown-out is not a break.
                    warmup = Mathf.lerpDelta(warmup, 0f, 0.08f);
                    return;
                }

                //the item charge is consumed one shard at a time and only when it has run out
                if(boostItem != null && charge <= 0f && items.has(boostItem)){
                    items.remove(boostItem, 1);
                    charge = itemBoostDuration;
                }

                float cap = realCapacity();
                shield = Math.min(cap, shield + regen / 60f * Time.delta * efficiency * (liquidBoost() ? liquidRegenBoost : 1f));
                warmup = Mathf.lerpDelta(warmup, 1f, 0.05f);

                if(shield <= 0.001f) return;

                float rad = realRadius();
                Groups.bullet.intersect(x - rad, y - rad, rad * 2f, rad * 2f, b -> {
                    if(shield <= 0.001f || cooldown > 0f) return;
                    if(b.team == team || b.absorbed || !b.type.absorbable || !b.within(this, rad)) return;

                    float damage = Math.max(0f, b.type.shieldDamage(b));
                    hitAlpha = 1f;
                    if(hits.size < 24){
                        hits.add(angleTo(b.x, b.y), 0f);
                    }
                    b.absorb();

                    damageShield(damage);
                });
            }

            /** Same crash-explosion contract as vanilla ForceProjector (2x crash damage). */
            @Override public float range(){ return realRadius(); }
            @Override public boolean absorbExplosion(float ex, float ey, float damage){
                if(!up() || !within(ex, ey, realRadius())) return false;
                hitAlpha = 1f;
                damageShield(Math.max(0f, damage) * 2f);
                return true;
            }
            public void damageShield(float damage){
                shield = Math.max(0f, shield - damage);
                if(shield <= 0f){
                    cooldown = breakCooldown;
                    AureliaFx.wardBreak.at(x, y, realRadius(), brokenColor);
                }
            }

            @Override
            public void draw(){
                if(model != null){
                    model.draw(this);
                }else{
                    super.draw();
                }
                drawDome();
                Draw.z(Layer.block);
            }

            @Override
            public void drawSelect(){
                Drawf.dashCircle(x, y, realRadius(), domeColor);
            }

            /**
             * The dome itself. Deliberately austere: two thin rings, a slow vertical "seam" sweep, and small
             * rings where shots were stopped. Alpha is tied to how full the bank is, so a dome that is nearly
             * out visibly thins instead of popping.
             */
            public void drawDome(){
                float rad = realRadius();
                if(cooldown > 0f){
                    //rebuilding: one faint, broken ring that closes as the cool-down runs out
                    float f = 1f - cooldown / breakCooldown;
                    Draw.z(Layer.shields);
                    Draw.color(brokenColor, 0.16f + 0.1f * f);
                    Lines.stroke(1.1f);
                    int seg = 36;
                    for(int i = 0; i < seg; i++){
                        float a0 = i * 360f / seg, a1 = a0 + 360f / seg * Mathf.clamp(f * 1.2f, 0.15f, 0.9f);
                        Lines.arc(x, y, rad, (a1 - a0) / 360f, a0);
                    }
                    Draw.reset();
                    return;
                }
                if(warmup <= 0.02f || shield <= 0.001f) return;

                float full = Mathf.clamp(shield / realCapacity());
                float a = (0.20f + 0.34f * full) * warmup;
                Color col = charge > 0f ? chargeColor : domeColor;

                Draw.z(Layer.shields);
                //outer rim
                Draw.color(col, a);
                Lines.stroke(1.5f);
                Lines.circle(x, y, rad);
                //inner rim, slightly inside, so the edge reads as a shell and not as a flat circle
                Draw.color(col, a * 0.5f);
                Lines.stroke(0.9f);
                Lines.circle(x, y, rad - 2.4f);

                //the seam: one slow mark travelling around the rim. This is the only moving part.
                float seam = (Time.time * 0.35f + id * 37f) % 360f;
                Draw.color(Color.white, a * 0.5f);
                Lines.stroke(1.5f);
                Lines.arc(x, y, rad, 0.045f, seam);

                //hex ticks: twelve short marks, so the dome has a structure instead of being a plain circle
                Draw.color(col, a * 0.65f);
                Lines.stroke(1.1f);
                for(int i = 0; i < 12; i++){
                    float ang = i * 30f + seam * 0.15f;
                    float cx = x + Angles.trnsx(ang, rad), cy = y + Angles.trnsy(ang, rad);
                    Lines.lineAngleCenter(cx, cy, ang + 90f, 4.2f);
                }

                //impacts: a small ring that opens and fades where a shot was stopped
                for(int i = 0; i < hits.size; i += 2){
                    float ang = hits.get(i), age = hits.get(i + 1);
                    float hx = x + Angles.trnsx(ang, rad), hy = y + Angles.trnsy(ang, rad);
                    Draw.color(col, (1f - age) * 0.55f * warmup);
                    Lines.stroke(1.4f * (1f - age));
                    Lines.circle(hx, hy, 2f + age * 9f);
                }
                Draw.reset();
            }

            @Override
            public boolean shouldConsume(){
                return enabled;
            }

            @Override
            public double sense(LAccess sensor){
                if(sensor == LAccess.shield) return cooldown > 0f ? 0f : shield;
                return super.sense(sensor);
            }

            @Override
            public byte version(){ return 1; }

            @Override
            public void write(Writes write){
                super.write(write);
                write.f(shield);
                write.f(cooldown);
                write.f(charge);
            }

            @Override
            public void read(Reads read, byte revision){
                super.read(read, revision);
                shield = read.f();
                cooldown = read.f();
                charge = read.f();
                if((Float.isNaN(shield) || Float.isInfinite(shield))) shield = 0f;
                if((Float.isNaN(cooldown) || Float.isInfinite(cooldown))) cooldown = breakCooldown;
                if((Float.isNaN(charge) || Float.isInfinite(charge))) charge = 0f;
                charge = Mathf.clamp(charge, 0f, itemBoostDuration);
                cooldown = Mathf.clamp(cooldown, 0f, breakCooldown);
                shield = Mathf.clamp(shield, 0f, realCapacity());
                //One-time migration from v8.3's empty-on-start/rebuild bug. New saves preserve exact damage.
                if(revision == 0 && cooldown <= 0f) shield = realCapacity();
            }
        }
    }
}
