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
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * v7.7 bastion pack - the defence wall family.
 *
 * <p>Ten walls that differ in <b>material</b> (what they are built from, which sets hit points, armour and
 * price) and in <b>mechanism</b> (what they actually do while they stand there). None of them is a re-skin:
 * every class below owns one behaviour that no other block in the mod has.
 *
 * <p>All of them draw through the mod's own 3D model kit ({@link BlockModel}), so there is no sprite sheet to
 * keep in sync and a wall line reads as a line of solid blocks rather than a flat texture.
 */
public final class BastionParts{
    private BastionParts(){}

    // =================================================================================================
    // base: a wall drawn by the 3D kit
    // =================================================================================================

    /** A plain wall whose look comes from a procedural model instead of a sprite. */
    public static class ModelWall extends Wall{
        public BlockModel model;

        public ModelWall(String name){
            super(name);
            //every wall in this pack keeps the vanilla wall feel: solid, buildable, destructible
            update = false;
            solid = true;
            group = BlockGroup.walls;
            envEnabled = Env.any;
            buildCostMultiplier = 1f;
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

        public class ModelWallBuild extends Building{
            @Override
            public void draw(){
                if(model != null) model.draw(this); else super.draw();
            }
        }
    }

    // =================================================================================================
    // 1. 静滞墙 - stasis wall: whatever hits it is slowed down
    // =================================================================================================

    /**
     * Hitting this wall costs the attacker its footing: any unit that damages it - or simply stands against
     * it - is tagged with the lattice grip for a while. A line of these turns a charge into a crawl.
     */
    public static class StasisWall extends ModelWall{
        public float gripRadius = 26f, gripDuration = 90f;
        /** v7.8 balance: a grip is a burst, not a permanent field - it catches a few units and then recharges */
        public float gripCooldown = 240f;
        public int maxGripped = 4;
        public StatusEffect grip;
        public Color fieldColor = Color.valueOf("8fe9ff");
        public final int timerGrip = timers++;

        public StasisWall(String name){
            super(name);
            update = true;
        }

        @Override
        public void setStats(){
            super.setStats();
            stats.add(Stat.range, gripRadius / tilesize, StatUnit.blocks);
        }

        public class StasisWallBuild extends ModelWallBuild{
            public float pulse, cool;

            @Override
            public void updateTile(){
                pulse = Mathf.lerpDelta(pulse, 0f, 0.04f);
                if(cool > 0f){
                    cool -= Time.delta;
                    return;
                }
                //cheap: four checks a second, and only on units that are actually touching the block
                if(grip != null && timer(timerGrip, 15f)){
                    held = 0;
                    Units.nearbyEnemies(team, x - gripRadius, y - gripRadius, gripRadius * 2f, gripRadius * 2f, u -> {
                        if(held >= maxGripped) return;
                        if(u.within(this, gripRadius) && !u.isFlying()){
                            u.apply(grip, gripDuration);
                            held++;
                            pulse = 1f;
                        }
                    });
                    //v7.8: once it has caught its handful of units the field has to recharge
                    if(held > 0) cool = gripCooldown;
                }
            }

            private int held;

            @Override
            public void draw(){
                super.draw();
                if(pulse <= 0.01f) return;
                Draw.z(Layer.effect);
                Draw.color(fieldColor, pulse * 0.45f);
                Lines.stroke(1.1f);
                Lines.poly(x, y, 6, gripRadius * (0.5f + 0.5f * pulse), Time.time * 0.4f);
                Draw.reset();
            }
        }
    }

    // =================================================================================================
    // 2. 回响墙 - echo wall: banks the damage it takes and gives it back
    // =================================================================================================

    /**
     * Every point of damage this wall absorbs is stored. Once the bank fills - or the wall finally breaks -
     * the charge leaves as a ring of lightning. Shooting it is how you load it.
     */
    public static class EchoWall extends ModelWall{
        public float bankCapacity = 900f, releaseRadius = 56f, releaseScale = 0.85f;
        /** v7.8 balance: the arc is capped and the wall needs time before it can fire another one */
        public float maxRelease = 520f, releaseCooldown = 240f;
        public Color arcColor = Color.valueOf("b69cff");

        public EchoWall(String name){
            super(name);
            update = true;
        }

        @Override
        public void setStats(){
            super.setStats();
            stats.add(Stat.damage, bankCapacity * releaseScale, StatUnit.none);
            stats.add(Stat.range, releaseRadius / tilesize, StatUnit.blocks);
        }

        public class EchoWallBuild extends ModelWallBuild{
            public float bank, flash;

            @Override
            public boolean collide(Bullet other){
                return true;
            }

            @Override
            public void damage(float damage){
                super.damage(damage);
                //while it is recharging the wall is just a wall - damage is not banked
                if(cool <= 0f) bank = Math.min(bankCapacity, bank + damage);
            }

            @Override
            public void updateTile(){
                flash = Math.max(0f, flash - Time.delta / 22f);
                if(cool > 0f){
                    cool -= Time.delta;
                    return;
                }
                if(bank >= bankCapacity) release();
            }

            public float cool;

            void release(){
                float dmg = Math.min(maxRelease, bank * releaseScale);
                cool = releaseCooldown;
                bank = 0f;
                flash = 1f;
                Damage.damage(team, x, y, releaseRadius, dmg, false, true);
                for(int i = 0; i < 6; i++){
                    Lightning.create(team, arcColor, dmg * 0.18f, x, y, i * 60f + Mathf.random(20f), 9);
                }
                AureliaFx.detainDischarge.at(x, y, 0f, arcColor);
                BHSounds.pulse.at(this, 0.8f);
            }

            @Override
            public void onDestroyed(){
                //the stored charge always leaves, even if the wall does not survive to spend it
                if(bank > 1f && cool <= 0f){
                    float dmg = Math.min(maxRelease, bank * releaseScale);
                    Damage.damage(team, x, y, releaseRadius, dmg, false, true);
                    AureliaFx.detainDischarge.at(x, y, 0f, arcColor);
                }
                super.onDestroyed();
            }

            @Override
            public void draw(){
                super.draw();
                float f = bank / bankCapacity;
                if(f <= 0.02f && flash <= 0f) return;
                Draw.z(Layer.effect);
                Draw.color(arcColor, 0.25f * f + flash * 0.5f);
                Lines.stroke(1.2f);
                Lines.poly(x, y, 6, block.size * tilesize * 0.42f + f * 2f, Time.time * 0.5f);
                Draw.reset();
            }

            @Override
            public void write(Writes write){
                super.write(write);
                write.f(bank);
            }

            @Override
            public void read(Reads read, byte revision){
                super.read(read, revision);
                bank = read.f();
            }
        }
    }

    // =================================================================================================
    // 3. 霜凝墙 - frost wall: drinks coolant and heals itself
    // =================================================================================================

    /** Fed with aurora coolant, this wall knits itself back together instead of needing a mender. */
    public static class FrostWall extends ModelWall{
        public float repairFraction = 0.011f, coolantUse = 0.05f;
        public Liquid coolant;
        public Color frost = Color.valueOf("a9f0ff");

        public FrostWall(String name){
            super(name);
            update = true;
            hasLiquids = true;
            liquidCapacity = 30f;
        }

        @Override
        public void setStats(){
            super.setStats();
            stats.add(Stat.repairTime, (int)(1f / repairFraction / 60f), StatUnit.seconds);
        }

        public class FrostWallBuild extends ModelWallBuild{
            public float cool;

            @Override
            public boolean acceptLiquid(Building source, Liquid liquid){
                return liquid == coolant && liquids.get(liquid) < block.liquidCapacity;
            }

            @Override
            public void updateTile(){
                if(coolant == null) return;
                if(health < maxHealth && liquids.get(coolant) >= coolantUse * Time.delta){
                    liquids.remove(coolant, coolantUse * Time.delta);
                    heal(maxHealth * repairFraction * Time.delta / 60f * 60f);
                    cool = 1f;
                }
                cool = Mathf.lerpDelta(cool, liquids.get(coolant) > 0.1f ? 0.6f : 0f, 0.05f);
            }

            @Override
            public void draw(){
                super.draw();
                if(cool <= 0.02f) return;
                Draw.z(Layer.blockOver);
                Draw.color(frost, cool * 0.35f);
                Fill.poly(x, y, 6, block.size * tilesize * 0.3f, Time.time * 0.2f);
                Draw.reset();
            }
        }
    }

    // =================================================================================================
    // 4. 引力锚墙 - anchor wall: drags the attack onto itself
    // =================================================================================================

    /**
     * A deliberately heavy block that bends ground attackers towards it. It does no damage of its own; it
     * buys the line behind it the seconds it needs, and it is cheap enough to lose.
     */
    public static class AnchorWall extends ModelWall{
        public float pullRadius = 72f, pull = 0.55f;
        /** v7.8 balance: pulls in bursts and only on a handful of units at a time */
        public float pullTime = 180f, pullCooldown = 180f;
        public int maxPulled = 6;
        public Color wellColor = Color.valueOf("9a7cff");

        public AnchorWall(String name){
            super(name);
            update = true;
        }

        @Override
        public void setStats(){
            super.setStats();
            stats.add(Stat.range, pullRadius / tilesize, StatUnit.blocks);
        }

        public class AnchorWallBuild extends ModelWallBuild{
            public float phase;
            private int pulled;

            @Override
            public void updateTile(){
                phase += Time.delta;
                //v7.8: three seconds of pull, three seconds of rest
                if(phase > pullTime + pullCooldown) phase = 0f;
                if(phase > pullTime) return;
                pulled = 0;
                Units.nearbyEnemies(team, x - pullRadius, y - pullRadius, pullRadius * 2f, pullRadius * 2f, u -> {
                    if(pulled >= maxPulled) return;
                    if(u.isFlying() || !u.within(this, pullRadius) || u.dst(this) < 12f) return;
                    //impulse only: no teleporting, so clients and server agree
                    u.impulseNet(Tmp.v1.set(this).sub(u).limit(pull * u.mass() * Time.delta));
                    pulled++;
                });
            }

            @Override
            public void draw(){
                super.draw();
                Draw.z(Layer.effect);
                Draw.color(wellColor, 0.1f + Mathf.absin(Time.time, 22f, 0.08f));
                Lines.stroke(0.9f);
                Lines.circle(x, y, pullRadius * (0.3f + 0.08f * Mathf.absin(Time.time, 30f, 1f)));
                Draw.reset();
            }
        }
    }

    // =================================================================================================
    // 5. 晶格增幅墙 - amplifier wall: makes the turret behind it shoot faster
    // =================================================================================================

    /** Support masonry: every friendly turret inside its radius reloads faster while this wall stands. */
    public static class AmplifierWall extends ModelWall{
        public float boostRadius = 60f, reloadBonus = 0.22f;
        /** v7.8 balance: one wall boosts at most four turrets, and a turret is boosted by one wall only */
        public int maxServed = 4;
        static final arc.struct.IntSet boostedTick = new arc.struct.IntSet();
        static long boostFrame = -1;
        public Color latticeColor = Color.valueOf("8fe9ff");
        public final int timerScan = timers++;

        public AmplifierWall(String name){
            super(name);
            update = true;
        }

        @Override
        public void setStats(){
            super.setStats();
            stats.add(Stat.range, boostRadius / tilesize, StatUnit.blocks);
            stats.add(Stat.reload, reloadBonus * 100f, StatUnit.percent);
        }

        public class AmplifierWallBuild extends ModelWallBuild{
            public final Seq<Building> served = new Seq<>();
            public float warmup;

            @Override
            public void updateTile(){
                warmup = Mathf.lerpDelta(warmup, served.isEmpty() ? 0.25f : 1f, 0.05f);
                if(timer(timerScan, 30f)){
                    served.clear();
                    indexer.eachBlock(this, boostRadius, b -> b.block instanceof Turret, b -> {
                        if(served.size < maxServed) served.add(b);
                    });
                }
                long frame = (long)Time.time;
                if(frame != boostFrame){
                    boostFrame = frame;
                    boostedTick.clear();
                }
                //a flat slice of extra reload per tick: the turret still obeys its own ammo and power rules
                for(int i = 0; i < served.size; i++){
                    Building b = served.get(i);
                    if(!b.isValid()){
                        served.remove(i--);
                        continue;
                    }
                    if(b instanceof BaseTurret.BaseTurretBuild && b instanceof Turret.TurretBuild t){
                        //a turret may only take the bonus once per tick, however many walls surround it
                        if(boostedTick.add(t.id)) t.reloadCounter += reloadBonus * t.efficiency * Time.delta;
                    }
                }
            }

            @Override
            public void draw(){
                super.draw();
                Draw.z(Layer.effect);
                Draw.color(latticeColor, 0.12f + warmup * 0.2f);
                Lines.stroke(0.9f);
                Lines.poly(x, y, 6, boostRadius * 0.25f, -Time.time * 0.3f);
                for(int i = 0; i < served.size; i++){
                    Building b = served.get(i);
                    if(b.isValid()) Lines.line(x, y, b.x, b.y);
                }
                Draw.reset();
            }
        }
    }

    // =================================================================================================
    // 6. 裂隙吸收墙 - absorber wall: eats bullets and pays the grid back
    // =================================================================================================

    /**
     * The wall version of the feedback lattice: hostile projectiles that cross its small field are swallowed,
     * and the energy goes into the power grid rather than into a shockwave.
     */
    public static class AbsorberWall extends ModelWall{
        public float absorbRadius = 34f, powerPerDamage = 0.16f, maxOutput = 2.6f;
        /** v7.8 balance: it may swallow this many shots per second, the rest go through */
        public int absorbBudget = 6;
        /**
         * v8.0 balance: the hard ceiling. Absorbed damage piles up as heat; once it passes this the rift
         * collapses and the wall is inert for {@link #overheatTime} - it absorbs nothing and makes no power,
         * it is only a wall. Heat bleeds off at {@link #coolRate} per tick while it is working, so sustained
         * fire above roughly {@code coolRate * 60} = 21 damage per second will always break it eventually.
         */
        public float absorbCapacity = 2600f;
        public float overheatTime = 60f * 8f;
        public float coolRate = 0.35f;
        public Color riftColor = Color.valueOf("b69cff");
        public Color overheatColor = Color.valueOf("ff8a6a");

        public AbsorberWall(String name){
            super(name);
            update = true;
            hasPower = true;
            outputsPower = true;
            consumesPower = false;
            conductivePower = true;
        }

        @Override
        public void setStats(){
            super.setStats();
            stats.add(Stat.range, absorbRadius / tilesize, StatUnit.blocks);
            stats.add(Stat.basePowerGeneration, maxOutput * 60f, StatUnit.powerSecond);
            stats.add(Stat.shieldHealth, absorbCapacity);
            stats.add(Stat.cooldownTime, overheatTime / 60f, StatUnit.seconds);
        }

        public class AbsorberWallBuild extends ModelWallBuild{
            public float charge, flash;
            /** absorbed damage piled up in the rift; at {@link #absorbCapacity} it collapses */
            public float heat;
            /** ticks left of the inert phase; > 0 means the wall is doing nothing at all */
            public float overheat;

            private int budget;
            private float budgetTimer;

            /** the rift is collapsed and this is just a wall right now */
            public boolean inert(){
                return overheat > 0f;
            }

            @Override
            public void updateTile(){
                flash = Math.max(0f, flash - Time.delta / 18f);

                if(inert()){
                    //cooling down: no absorbing, no power, the stored charge is gone
                    overheat = Math.max(0f, overheat - Time.delta);
                    heat = absorbCapacity * (overheat / overheatTime);
                    charge = 0f;
                    return;
                }

                heat = Math.max(0f, heat - coolRate * Time.delta);

                if((budgetTimer -= Time.delta) <= 0f){
                    budgetTimer = 60f;
                    budget = absorbBudget;
                }
                Groups.bullet.intersect(x - absorbRadius, y - absorbRadius, absorbRadius * 2f, absorbRadius * 2f, b -> {
                    if(budget <= 0 || inert()) return;
                    if(b.team == team || !b.type.absorbable || !b.within(this, absorbRadius)) return;
                    budget--;
                    heat += b.damage;
                    charge = Math.min(maxOutput * 12f, charge + b.damage * powerPerDamage);
                    flash = 1f;
                    AureliaFx.detainAbsorb.at(b.x, b.y, b.angleTo(this), riftColor);
                    b.absorb();
                    if(heat >= absorbCapacity){
                        //hard ceiling reached - collapse
                        heat = absorbCapacity;
                        overheat = overheatTime;
                        charge = 0f;
                        flash = 1f;
                        AureliaFx.detainAbsorb.at(x, y, 0f, overheatColor);
                    }
                });
                charge = Math.max(0f, charge - getPowerProduction() * Time.delta);
            }

            @Override
            public float getPowerProduction(){
                return inert() ? 0f : Math.min(maxOutput, charge * 0.1f);
            }

            @Override
            public void draw(){
                super.draw();
                float warn = Mathf.clamp(heat / absorbCapacity);
                if(charge <= 0.01f && flash <= 0f && warn <= 0.01f) return;
                Draw.z(Layer.effect);
                if(inert()){
                    //visibly dead: a broken ring that shrinks as the cooldown runs out
                    Draw.color(overheatColor, 0.35f + flash * 0.3f);
                    Lines.stroke(1f);
                    for(int i = 0; i < 4; i++){
                        Lines.arc(x, y, absorbRadius * (0.35f + 0.25f * (overheat / overheatTime)), 0.12f, i * 90f + Time.time * 0.6f);
                    }
                }else{
                    Draw.color(riftColor, 0.2f * Mathf.clamp(charge / (maxOutput * 6f)) + flash * 0.4f);
                    Lines.stroke(1f);
                    Lines.circle(x, y, absorbRadius * (0.6f + 0.4f * flash));
                    if(warn > 0.5f){
                        //load warning: the ring reddens as the rift fills up
                        Draw.color(riftColor, overheatColor, (warn - 0.5f) * 2f);
                        Draw.alpha(0.25f + 0.35f * (warn - 0.5f) * 2f);
                        Lines.stroke(1.4f);
                        Lines.arc(x, y, absorbRadius * 0.45f, warn, 90f);
                    }
                }
                Draw.reset();
            }

            @Override
            public byte version(){
                return 1;
            }

            @Override
            public void write(Writes write){
                super.write(write);
                write.f(charge);
                write.f(heat);
                write.f(overheat);
            }

            @Override
            public void read(Reads read, byte revision){
                super.read(read, revision);
                charge = read.f();
                if(revision >= 1){
                    heat = read.f();
                    overheat = read.f();
                }
            }
        }
    }

    // =================================================================================================
    // 7. 协律力场墙 - a powered shield wall drawn by the kit
    // =================================================================================================

    /** Vanilla's shield wall mechanic (a regenerating shield in front of the block) on a modelled body. */
    public static class ModelShieldWall extends ShieldWall{
        public BlockModel model;

        public ModelShieldWall(String name){
            super(name);
            group = BlockGroup.walls;
            envEnabled = Env.any;
        }

        @Override
        public void load(){
            super.load();
            model = Models.get(name);
            if(model != null) model.load();
            //ShieldWall draws a glow sprite over the base region; the modelled body has its own glow, so the
            //region is pointed at the base one instead of a sheet that does not exist
            if(glowRegion == null || !glowRegion.found()) glowRegion = Core.atlas.find(name + "-preview", region);
        }

        @Override
        public TextureRegion[] icons(){
            return new TextureRegion[]{Core.atlas.find(name + "-preview", region)};
        }

        @Override
        public void getRegionsToOutline(Seq<TextureRegion> out){
        }

        public class ModelShieldWallBuild extends ShieldWallBuild{
            @Override
            public void draw(){
                if(model != null){
                    model.draw(this);
                    //the shield film itself, kept from the vanilla behaviour
                    if(shield > 0f){
                        Draw.z(Layer.blockOver);
                        Draw.color(glowColor, Mathf.clamp(shield / shieldHealth) * 0.5f);
                        Fill.square(x, y, block.size * tilesize / 2f - 1f);
                        Draw.reset();
                    }
                }else{
                    super.draw();
                }
            }
        }
    }
}
