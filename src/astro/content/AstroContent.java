package astro.content;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.entities.pattern.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.units.*;
import mindustry.world.meta.*;

import static mindustry.type.ItemStack.*;

/** Registers the Astro Detainer, its projectiles and the fabricator that builds it. */
public final class AstroContent{
    private AstroContent(){}

    public static DetainerType detainer;
    public static UnitFactory fabricator;
    /** v8.3: restored from the standalone package - the command core that drives the tactical brain */
    public static TacticalCommander commander;
    public static mindustry.type.StatusEffect warpLicense;
    /** command-menu toggle: the AI of this unit may warp by itself (off by default) */
    public static mindustry.ai.UnitStance warpStance;
    public static BulletType orb, counter, lance, overload, counterBeam;

    public static void load(){
        loadUnits();
        loadBlocks();
    }

    /** projectiles and the unit (no dependency on items / blocks: also used by the offline renderer) */
    public static void loadUnits(){
        DetainerType.registerEvents();
        warpLicense = new mindustry.type.StatusEffect("astro-warp-license"){{
            permanent = true;
            show = false;
        }};
        DetainerCtl.license = warpLicense;
        warpStance = new mindustry.ai.UnitStance("warp", "planet", null);

        Color gold = AstroFx.gold, amber = AstroFx.amber;

        orb = new EnergyOrb(5f, 46f){{
            glow = gold;
            size = 5.5f;
            lifetime = 54f;
        }};
        counter = new EnergyOrb(7f, 120f){{
            glow = amber;
            size = 4.5f;
            lifetime = 70f;
            homingPower = 0.11f;
            homingRange = 240f;
            lightning = 0;
            splashDamage = 0f;
            trailLength = 18;
        }};
        lance = new LaserBulletType(320f){{
            length = 500f;
            width = 26f;
            lifetime = 24f;
            colors = new Color[]{gold.cpy().a(0.4f), gold, Color.white};
            lightningSpacing = 44f;
            lightningLength = 6;
            lightningDamage = 36f;
            lightningColor = gold;
            hitColor = gold;
            buildingDamageMultiplier = 0.7f;
            status = StatusEffects.electrified;
            statusDuration = 90f;
            hitEffect = Fx.hitLancer;
            largeHit = true;
            sideAngle = 25f;
            sideLength = 40f;
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
        }};
        overload = new LaserBulletType(3200f){{
            length = 720f;
            width = 58f;
            lifetime = 46f;
            colors = new Color[]{amber.cpy().a(0.4f), gold, Color.white};
            lightningSpacing = 34f;
            lightningLength = 9;
            lightningDamage = 90f;
            lightningColor = amber;
            hitColor = amber;
            buildingDamageMultiplier = 0.8f;
            status = StatusEffects.electrified;
            statusDuration = 180f;
            hitEffect = Fx.massiveExplosion;
            largeHit = true;
            sideAngle = 30f;
            sideLength = 70f;
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
        }};

        counterBeam = new LaserBulletType(300f){{
            length = 430f;
            width = 18f;
            lifetime = 18f;
            colors = new Color[]{amber.cpy().a(0.4f), amber, Color.white};
            lightningSpacing = 50f;
            lightningLength = 4;
            lightningDamage = 20f;
            lightningColor = amber;
            hitColor = amber;
            buildingDamageMultiplier = 0.6f;
            status = StatusEffects.electrified;
            statusDuration = 60f;
            sideAngle = 20f;
            sideLength = 24f;
            absorbable = false;
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
        }};

        detainer = new DetainerType("astro-detainer"){{
            orb = AstroContent.orb;
            counter = AstroContent.counter;
            lance = AstroContent.lance;
            overload = AstroContent.overload;
            counterBeam = AstroContent.counterBeam;
            researchCostMultiplier = 1.4f;
            isEnemy = true;
            //command menu: the usual combat stances plus the warp permission of the AI (off by default)
            if(mindustry.ai.UnitStance.stop != null) stances.addAll(mindustry.ai.UnitStance.stop, mindustry.ai.UnitStance.holdFire, mindustry.ai.UnitStance.pursueTarget, mindustry.ai.UnitStance.patrol);
            stances.add(warpStance);
            //three identical claws: each one throws energy orbs (mounts 0-2) and fires the discharge lance (mounts 3-5)
            weapons.add(claw(0, false, -17f, 0f, 30f, 0f, orb));
            weapons.add(claw(1, false, 17f, 0f, 30f, 10f, orb));
            weapons.add(claw(2, false, 0f, -15f, 30f, 20f, orb));
            weapons.add(claw(0, true, -17f, 0f, 230f, 0f, lance));
            weapons.add(claw(1, true, 17f, 0f, 230f, 70f, lance));
            weapons.add(claw(2, true, 0f, -15f, 230f, 140f, lance));
        }};

    }

    public static void loadBlocks(){

        //v8.3: the tactical brain is back. It is registered before the block that switches it on.
        astro.ai.TacticalBrain.register();
        commander = new TacticalCommander("astro-commander"){{
            requirements(Category.units, with(Items.silicon, 300, Items.titanium, 250, Items.plastanium, 120, Items.surgeAlloy, 80));
            health = 900;
            consumePower(3f);
            researchCostMultiplier = 0.2f;
        }};

        fabricator = new AstroFabricator("astro-fabricator"){{
            requirements(Category.units, with(Items.silicon, 1400, Items.titanium, 1200, Items.thorium, 600, Items.plastanium, 500, Items.phaseFabric, 300, Items.surgeAlloy, 400));
            size = 5;
            health = 4200;
            consumePower(9f);
            liquidCapacity = 60f;
            floating = true;
            //v7.8: well above any vanilla T5 - this is the single most expensive unit in the pack
            plans.add(new UnitPlan(detainer, 60f * 210f,
                with(Items.silicon, 6400, Items.titanium, 4800, Items.thorium, 2800, Items.plastanium, 1900, Items.phaseFabric, 1500, Items.surgeAlloy, 2000)));
            researchCostMultiplier = 0.2f;
        }};
    }

    static ClawWeapon claw(int index, boolean cannon, float x, float y, float reload, float delay, BulletType bullet){
        ClawWeapon w = new ClawWeapon(index, cannon);
        w.x = x;
        w.y = y;
        w.reload = reload;
        w.bullet = bullet;
        w.shoot = new ShootPattern();
        w.shoot.firstShotDelay = delay;
        w.shootSound = cannon ? Sounds.shootLancer : Sounds.shootLaser;
        w.shootSoundVolume = cannon ? 1f : 0.55f;
        w.recoil = 0f;
        return w;
    }
}
