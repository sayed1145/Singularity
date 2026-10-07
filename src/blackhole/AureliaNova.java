package blackhole;

import arc.graphics.*;
import arc.struct.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.units.UnitFactory.*;
import mindustry.world.meta.*;

import static blackhole.AureliaContent.*;
import static mindustry.type.ItemStack.with;

/**
 * v7.8 content pack.
 *
 * <ul>
 * <li>One large power node.</li>
 * <li>The resonance field - the new mechanic: fighting near a {@link NovaParts.ResonanceHub} charges it, and a
 *     full hub overloads every friendly unit around it and repairs the buildings.</li>
 * <li>Five new units, all built on Aurelia.</li>
 * </ul>
 */
public final class AureliaNova{
    public static StatusEffect resonanceOverload;

    public static Block auroraNode, resonanceHub;

    public static Ship3D lumenCourier, auroraSentry;
    public static Tank3D prismScout, auriteLancer, resonanceSiege;

    /** everything this pack adds, in tree order */
    public static final Seq<UnlockableContent> all = new Seq<>();

    private static boolean loaded;

    private AureliaNova(){}

    static <T extends UnlockableContent> T add(T c){
        all.add(c);
        AureliaContent.own.add(c);
        return c;
    }

    public static void load(){
        if(loaded) return;
        loaded = true;
        loadStatus();
        loadUnits();
        loadBlocks();
        loadPlans();
    }

    private static void loadStatus(){
        resonanceOverload = add(new StatusEffect("resonance-overload"){{
            color = Color.valueOf("b69cff");
            damageMultiplier = 1.35f;
            speedMultiplier = 1.18f;
            reloadMultiplier = 1.25f;
            healthMultiplier = 1.1f;
            effect = AureliaFx.lumenHit;
            permanent = false;
        }});
    }

    private static void loadUnits(){
        prismScout = add(new Tank3D("prism-scout"){{
            builder = NovaUnits::prismScout;
            speed = 1.15f;
            hitSize = 11f;
            health = 480f;
            armor = 2f;
            rotateSpeed = 4.2f;
            omniMovement = false;
            range = 125f;
            targetAir = false;
            weapons.add(new LiftedWeapon(){{
                mirror = false; rotate = true; rotateSpeed = 4.5f; top = true; reload = 20f; recoil = 0.8f;
                shootSound = BHSounds.pulse;
                bullet = bolt(5.4f, 20f, 25f, AureliaFx.lumen);
            }});
        }});

        auriteLancer = add(new Tank3D("aurite-lancer"){{
            builder = NovaUnits::auriteLancer;
            speed = 0.78f;
            hitSize = 17f;
            health = 1750f;
            armor = 7f;
            rotateSpeed = 2.8f;
            omniMovement = false;
            range = 190f;
            targetAir = false;
            weapons.add(new LiftedWeapon(){{
                mirror = false; rotate = true; rotateSpeed = 2.6f; top = true; reload = 54f; recoil = 2.2f;
                shootSound = BHSounds.pulse;
                bullet = new BasicBulletType(9f, 115f){{
                    lifetime = 24f;
                    width = 10f; height = 18f;
                    pierce = true; pierceCap = 2;
                    frontColor = Color.white;
                    backColor = trailColor = hitColor = AureliaFx.resonance;
                    trailLength = 8; trailWidth = 2f;
                    hitEffect = despawnEffect = AureliaFx.lumenHit;
                    shootEffect = OwnFx.muzzle;
                    smokeEffect = OwnFx.muzzleSmoke;
                }};
            }});
        }});

        resonanceSiege = add(new Tank3D("resonance-siege"){{
            builder = NovaUnits::resonanceSiege;
            speed = 0.5f;
            hitSize = 23f;
            health = 4200f;
            armor = 12f;
            rotateSpeed = 1.8f;
            omniMovement = false;
            range = 290f;
            targetAir = false;
            weapons.add(new LiftedWeapon(){{
                mirror = false; rotate = true; rotateSpeed = 1.6f; top = true; reload = 135f; recoil = 3.4f;
                inaccuracy = 4f;
                shootSound = BHSounds.pulse;
                bullet = new ArtilleryBulletType(3.6f, 60f){{
                    lifetime = 82f;
                    width = 16f; height = 16f;
                    splashDamage = 190f; splashDamageRadius = 46f;
                    frontColor = Color.white;
                    backColor = trailColor = hitColor = AureliaFx.resonance;
                    trailLength = 12; trailWidth = 3f;
                    hitEffect = despawnEffect = AureliaFx.lumenHit;
                    shootEffect = OwnFx.muzzle;
                    smokeEffect = OwnFx.muzzleSmoke;
                }};
            }});
        }});

        lumenCourier = add(new Ship3D("lumen-courier"){{
            builder = NovaUnits::lumenCourier;
            lowAltitude = true;
            speed = 3.1f;
            accel = 0.09f;
            drag = 0.04f;
            health = 620f;
            armor = 2f;
            hitSize = 12f;
            hover = 6f;
            range = 60f;
            engineColor = AureliaFx.lumen;
            //the support role: it mines and it builds, it does not fight
            mineTier = 4;
            mineSpeed = 7.5f;
            buildSpeed = 1.1f;
            itemCapacity = 60;
            targetAir = targetGround = false;
        }});

        auroraSentry = add(new Ship3D("aurora-sentry"){{
            builder = NovaUnits::auroraSentry;
            lowAltitude = true;
            speed = 4.1f;
            accel = 0.12f;
            drag = 0.035f;
            health = 560f;
            armor = 3f;
            hitSize = 10f;
            hover = 5f;
            range = 150f;
            engineColor = AureliaFx.lumen;
            targetAir = targetGround = true;
            for(int i = 0; i < 2; i++){
                int mount = i;
                weapons.add(new LiftedWeapon(){{
                    x = (mount == 0 ? 2.6f : -2.6f); y = 2f; mirror = false; rotate = false; reload = 16f; muzzleZ = 0f;
                    shootCone = 25f;
                    shootSound = BHSounds.pulse;
                    bullet = bolt(7.2f, 26f, 26f, AureliaFx.lumen);
                }});
            }
        }});
    }

    private static void loadBlocks(){
        // ------------------------------------------------------------------ power
        auroraNode = add(new NovaParts.ModelPowerNode("aurora-node"){{
            requirements(Category.power, with(lumenite, 60, aurite, 40, prismAlloy, 15));
            size = 2;
            health = 320;
            maxNodes = 24;
            laserRange = 16f;
            laserColor1 = Color.white;
            laserColor2 = AureliaFx.lumen;
        }});

        // ------------------------------------------------------------------ the resonance field
        resonanceHub = add(new NovaParts.ResonanceHub("resonance-hub"){{
            requirements(Category.effect, with(lumenite, 320, prismAlloy, 140, concordCore, 40, resonanceShard, 90));
            size = 3;
            health = 1400;
            consumePower(3.2f);
            radius = 216f;
            capacity = 1000f;
            chargePerUnit = 26f;
            idleCharge = 6f;
            overloadDuration = 360f;
            cooldown = 420f;
            healPercent = 12f;
            overload = resonanceOverload;
        }});
    }

    /** the new units are printed by the existing Aurelia factories */
    private static void loadPlans(){
        ((mindustry.world.blocks.units.UnitFactory)aureliaFabricator).plans.add(
            new UnitPlan(prismScout, 60f * 18f, with(lumenite, 30, resonanceShard, 6)));
        ((mindustry.world.blocks.units.UnitFactory)aureliaFabricator).plans.add(
            new UnitPlan(lumenCourier, 60f * 26f, with(lumenite, 45, aurite, 20, resonanceShard, 10)));
        ((mindustry.world.blocks.units.UnitFactory)aureliaFabricator).plans.add(
            new UnitPlan(auroraSentry, 60f * 30f, with(lumenite, 50, prismAlloy, 15, resonanceShard, 12)));
        ((mindustry.world.blocks.units.UnitFactory)aureliaFabricator).plans.add(
            new UnitPlan(auriteLancer, 60f * 62f, with(lumenite, 140, prismAlloy, 45, resonanceShard, 25)));
        ((mindustry.world.blocks.units.UnitFactory)aureliaFabricator).plans.add(
            new UnitPlan(resonanceSiege, 60f * 105f, with(prismAlloy, 110, concordCore, 25, lumenite, 220, resonanceShard, 55)));
    }
}
