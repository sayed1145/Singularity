package blackhole;

import arc.graphics.*;
import arc.struct.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.units.UnitFactory.*;
import mindustry.world.meta.*;

import static blackhole.AureliaContent.*;
import static mindustry.type.ItemStack.with;

/**
 * v8.0-beta content pack: the logistics / support tier.
 *
 * <ul>
 * <li><b>Wall bore</b> - a straight-laser drill that cuts the v7.9 ore seams out of solid rock. It is the
 *     industrial answer to wall ore: cheap per tile, but it only reaches rock it can see in a straight line.</li>
 * <li><b>Cargo station + drop pad</b> - air logistics. The station prints one cargo drone and keeps it; the
 *     drone carries items to any drop pad on the map, so an outpost needs no belt line home.</li>
 * <li><b>Large repair dome</b> and <b>large accumulator</b> - the second tier of the mender and the battery.</li>
 * <li><b>Ground well</b> - water out of damp soil, so a plant no longer has to sit on a lake.</li>
 * <li>Three units: the cargo drone, a repair drone and a ground engineer, all printable.</li>
 * </ul>
 */
public final class AureliaForge{
    public static Block wallBore, cargoStation, cargoPoint, mendDome, accumulator, groundWell;

    public static Ship3D lumenDrone, lumenMedic;
    public static Tank3D auriteWright;

    /** everything this pack adds, in tree order */
    public static final Seq<UnlockableContent> all = new Seq<>();

    private static boolean loaded;

    private AureliaForge(){}

    static <T extends UnlockableContent> T add(T c){
        all.add(c);
        AureliaContent.own.add(c);
        return c;
    }

    public static void load(){
        if(loaded) return;
        loaded = true;
        loadUnits();
        loadBlocks();
        loadPlans();
    }

    // =====================================================================================================
    // units
    // =====================================================================================================

    private static void loadUnits(){
        /*
         * The cargo drone. It is a tool, not a fighter: no weapons, low health, and it only exists while its
         * station stands. Its whole job is to move items between a station and the drop pads.
         */
        lumenDrone = add(new Ship3D("lumen-drone"){{
            builder = ForgeUnits::lumenDrone;
            //v8.1: the drone is a tethered entity with our own porter controller. The vanilla CargoAI needs a
            //BuildingTetherc body and quits on the first line without one - which is why no drone ever moved.
            constructor = BuildingTetherPayloadUnit::create;
            controller = u -> new LogixParts.PorterAI();
            isEnemy = false;
            allowedInPayloads = false;
            //v8.2: 玩家可以接管、编队指挥这些无人机；放手后它们自己回到物流网。
            //既然可被操控，就不能再是打不到的幽灵单位，否则就是无敌载具。
            logicControllable = true;
            playerControllable = true;
            allowChangeCommands = true;
            envDisabled = 0;
            targetable = true;
            hittable = true;
            hidden = true;
            lowAltitude = false;
            flying = true;
            speed = 3.4f;
            accel = 0.09f;
            drag = 0.04f;
            health = 180f;
            hitSize = 8f;
            itemCapacity = 90;
            useUnitCap = false;
            engineColor = AureliaFx.lumen;
            targetAir = targetGround = false;
        }});

        /*
         * The repair drone: it heals buildings with a continuous beam. Deliberately unarmed - it is the unit
         * you send with a push, not one that trades shots.
         */
        lumenMedic = add(new Ship3D("lumen-medic"){{
            builder = ForgeUnits::lumenMedic;
            lowAltitude = true;
            speed = 2.9f;
            accel = 0.08f;
            drag = 0.04f;
            health = 760f;
            armor = 3f;
            hitSize = 12f;
            hover = 6f;
            range = 120f;
            canHeal = true;
            engineColor = AureliaFx.lumen;
            targetAir = false;
            targetGround = false;
            //it can also patch up what it passes: a small build speed, so it can finish a half-built block
            buildSpeed = 0.8f;
            itemCapacity = 40;
            weapons.add(new LiftedWeapon(){{
                mirror = false;
                rotate = false;
                reload = 24f;
                shootCone = 40f;
                targetAir = false;
                targetGround = false;
                shootSound = BHSounds.pulse;
                bullet = new LaserBoltBulletType(5.6f, 0f){{
                    lifetime = 28f;
                    healPercent = 3.5f;
                    collidesTeam = true;
                    collidesGround = true;
                    collidesAir = false;
                    width = 3.4f;
                    height = 10f;
                    frontColor = Color.white;
                    backColor = hitColor = trailColor = AureliaFx.lumen;
                    hitEffect = despawnEffect = AureliaFx.lumenHit;
                }};
            }});
        }});

        /*
         * The ground engineer. Fast builder, carries a lot, and can repair a damaged building by rebuilding
         * plating; it mines the floor too, so a forward post can feed itself.
         */
        auriteWright = add(new Tank3D("aurite-wright"){{
            builder = ForgeUnits::auriteWright;
            speed = 0.72f;
            hitSize = 15f;
            health = 1500f;
            armor = 6f;
            rotateSpeed = 2.8f;
            omniMovement = false;
            range = 100f;
            targetAir = false;
            targetGround = false;
            buildSpeed = 2.4f;
            mineTier = 3;
            mineSpeed = 6.5f;
            mineFloor = true;
            itemCapacity = 150;
            canHeal = true;
            weapons.add(new LiftedWeapon(){{
                mirror = false;
                rotate = false;
                reload = 30f;
                shootCone = 45f;
                targetAir = false;
                targetGround = false;
                shootSound = BHSounds.pulse;
                bullet = new LaserBoltBulletType(5.2f, 0f){{
                    lifetime = 24f;
                    healPercent = 4.5f;
                    collidesTeam = true;
                    collidesGround = true;
                    collidesAir = false;
                    width = 3.2f;
                    height = 9f;
                    frontColor = Color.white;
                    backColor = hitColor = trailColor = AureliaFx.lumen;
                    hitEffect = despawnEffect = AureliaFx.lumenHit;
                }};
            }});
        }});
    }

    // =====================================================================================================
    // blocks
    // =====================================================================================================

    private static void loadBlocks(){
        /*
         * Wall bore. Two lanes, six tiles of reach, tier four - it takes lumenite, aurite and resonance seams
         * out of rock, and nothing else. Costed between the auger and the collector: it is the cheap way to
         * work a cliff, not a replacement for a ground drill line.
         */
        wallBore = add(new ForgeParts.WallBore("wall-bore"){{
            requirements(Category.production, with(lumenite, 120, aurite, 95, prismGlass, 55));
            size = 2;
            health = 520;
            range = 6;
            tier = 4;
            drillTime = 190f;
            laserWidth = 0.7f;
            itemCapacity = 20;
            consumePower(1.3f);
            //a splash of plasma makes the cut faster - the optional booster every drill line wants
            consumeLiquid(lumenPlasma, 0.08f).boost();
            optionalBoostIntensity = 2.1f;
        }});

        /*
         * Air logistics. The station is deliberately expensive to run (power + one drone) and slow compared
         * with a belt; what it buys is distance, and it cannot be used to cheat throughput: one drone, one
         * item stack at a time.
         */
        cargoStation = add(new LogixParts.Hub("lumen-cargo-station"){{
            requirements(Category.distribution, with(lumenite, 180, aurite, 120, prismAlloy, 45, resonanceShard, 30));
            size = 3;
            health = 640;
            droneType = lumenDrone;
            droneBuildTime = 60f * 16f;
            droneCap = 3;
            range = 58f * 8f;
            itemCapacity = 120;
            consumePower(1.8f);
        }});
        cargoPoint = add(new LogixParts.Request("lumen-cargo-point", 1){{
            requirements(Category.distribution, with(lumenite, 24, prismGlass, 16));
            health = 160;
            itemCapacity = 60;
        }});

        /*
         * Large repair dome: three times the radius of the mender and a much stronger pulse, but it is a
         * power-hungry 3x3 that also wants prism alloy, so the small mender is still what you spam on a line.
         */
        mendDome = add(new AureliaParts.LumenMenderBlock("lumen-mend-dome"){{
            requirements(Category.effect, with(lumenite, 160, prismAlloy, 60, prismGlass, 70, resonanceShard, 25));
            size = 3;
            health = 720;
            reload = 120f;
            range = 160f;
            healPercent = 24f;
            phaseBoost = 0f;
            consumePower(2.6f);
            consumeItem(prismGlass).boost();
        }});

        /*
         * Large accumulator: ten small cells in one 3x3 footprint, so a base can ride out a night without a
         * wall of batteries. It explodes harder than a cell, which is the trade.
         */
        accumulator = add(new AureliaParts.LumenCellBlock("lumen-accumulator"){{
            requirements(Category.power, with(lumenite, 120, aurite, 160, prismGlass, 60, resonanceShard, 20));
            size = 3;
            health = 760;
            consumePowerBuffered(14000f);
            baseExplosiveness = 6f;
        }});

        /*
         * Ground well: pumps water out of the ground. v8.3: 2.5x the old rate, and a base efficiency of 0.4,
         * so it now works on every floor instead of only on damp ground - damp ground is simply better. It
         * still needs power and is still beaten by the tide pump on open water.
         */
        groundWell = add(new ForgeParts.ModelSolidPump("ground-well"){{
            requirements(Category.liquid, with(lumenite, 60, aurite, 45, prismGlass, 30));
            size = 2;
            health = 380;
            result = tidewater;
            pumpAmount = 7f / 60f * 2.5f;
            liquidCapacity = 60f;
            attribute = Attribute.water;
            baseEfficiency = 0.4f;
            rotateSpeed = 0f;
            updateEffect = Fx.none;
            consumePower(1.1f);
        }});
    }

    /** The three new units are printed by the existing Aurelia factories. */
    private static void loadPlans(){
        mindustry.world.blocks.units.UnitFactory f = (mindustry.world.blocks.units.UnitFactory)aureliaFabricator;
        f.plans.add(new UnitPlan(lumenMedic, 60f * 32f, with(lumenite, 90, prismGlass, 60, resonanceShard, 18)));
        f.plans.add(new UnitPlan(auriteWright, 60f * 36f, with(lumenite, 120, aurite, 90, prismAlloy, 25)));
    }
}
