package blackhole;

import arc.*;
import arc.graphics.*;
import arc.struct.*;
import blackhole.g3d.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.units.UnitFactory.*;
import mindustry.world.meta.*;
import blackhole.models.*;

import static blackhole.AureliaContent.*;
import static mindustry.type.ItemStack.with;

/**
 * v7.9 terrain pack.
 *
 * <ul>
 * <li>Seven new floors, each with its own feel: salt flats you cross quickly, ash that runs hot, frozen crust
 *     that slows you down, gravel that is itself worth mining, glowing silt, plasma vents and basalt shelf.</li>
 * <li>Three rock walls to go with them, and - new in this version - <b>ore inside walls</b>. A wall ore is an
 *     overlay on solid rock: the rift collector lifts it straight out of the cliff and the borer digs it out,
 *     so a map's rock faces are now worth something instead of just being in the way.</li>
 * <li>Four units: a mining engineer that can chew ore out of walls, a heavy line tank, a fast interceptor and
 *     a support gunship.</li>
 * </ul>
 */
public final class AureliaTerra{
    /** the seven new floors */
    public static Floor glassFlat, emberAsh, frostCrust, auriteGravel, resonanceSilt, plasmaVent, basaltShelf;
    /** rock that goes with the new ground */
    public static Block glassRidgeWall, emberWall, basaltWall;
    /** ore sitting inside rock - mine the wall, get the ore */
    public static OreBlock wallOreLumenite, wallOreAurite, wallOreResonance;

    public static Tank3D terraBorer, basaltGuard;
    public static Ship3D glassHarrier, ventCaller;

    /** everything this pack adds, in tree order */
    public static final Seq<UnlockableContent> all = new Seq<>();

    private static boolean loaded;

    private AureliaTerra(){}

    static <T extends UnlockableContent> T add(T c){
        all.add(c);
        AureliaContent.own.add(c);
        return c;
    }

    public static void load(){
        if(loaded) return;
        loaded = true;
        loadTerrain();
        loadUnits();
        loadPlans();
    }

    // =====================================================================================================
    // terrain
    // =====================================================================================================

    private static void loadTerrain(){
        glassRidgeWall = add(new StaticWall("glass-ridge-wall"){{
            variants = 2;
            mapColor = Color.valueOf("8fa6bd");
        }});
        emberWall = add(new StaticWall("ember-wall"){{
            variants = 2;
            mapColor = Color.valueOf("6b4a42");
        }});
        basaltWall = add(new StaticWall("basalt-wall"){{
            variants = 2;
            mapColor = Color.valueOf("3a3f4a");
        }});

        //1. salt flats: hard, bright, and quick to cross - the place to put a road
        glassFlat = add(new Floor("glass-flat"){{
            variants = 3;
            wall = glassRidgeWall;
            decoration = auroraBoulder;
            speedMultiplier = 1.12f;
            albedo = 0.85f;
            mapColor = Color.valueOf("9fb4c6");
            attributes.set(Attribute.water, 0.05f);
        }});

        //2. ash: warm ground, the attribute heat machinery likes
        emberAsh = add(new Floor("ember-ash"){{
            variants = 3;
            wall = emberWall;
            decoration = auroraBoulder;
            speedMultiplier = 0.95f;
            mapColor = Color.valueOf("5b4038");
            attributes.set(Attribute.heat, 0.35f);
            attributes.set(Attribute.oil, 0.4f);
        }});

        //3. frozen crust: slow going, and wet enough for condensers
        frostCrust = add(new Floor("frost-crust"){{
            variants = 3;
            wall = glassRidgeWall;
            decoration = resonanceCrystal;
            speedMultiplier = 0.88f;
            mapColor = Color.valueOf("adc8d8");
            attributes.set(Attribute.water, 0.3f);
        }});

        //4. aurite gravel: ground that is itself ore. A drill on bare gravel yields aurite, and the collector
        //   offers it the moment its cone covers a patch - which is the point of the v7.9 selector.
        auriteGravel = add(new Floor("aurite-gravel"){{
            variants = 3;
            wall = basaltWall;
            decoration = auroraBoulder;
            itemDrop = aurite;
            playerUnmineable = false;
            mapColor = Color.valueOf("b59a63");
        }});

        //5. resonance silt: soft glowing ground that yields silt
        resonanceSilt = add(new Floor("resonance-silt"){{
            variants = 3;
            wall = resonanceWall;
            decoration = resonanceCrystal;
            itemDrop = silt;
            speedMultiplier = 0.94f;
            mapColor = Color.valueOf("6a5c96");
            emitLight = true;
            lightRadius = 18f;
            lightColor = AureliaFx.resonance.cpy().a(0.12f);
            attributes.set(Attribute.heat, 0.1f);
        }});

        //6. plasma vents: the hottest ground on the planet, and it shows
        plasmaVent = add(new AureliaFloor("plasma-vent"){{
            variants = 3;
            wall = emberWall;
            decoration = auroraBoulder;
            pulse = true;
            pulseInterval = 95f;
            mapColor = Color.valueOf("7a5542");
            emitLight = true;
            lightRadius = 30f;
            lightColor = Color.valueOf("ffb45a").a(0.2f);
            attributes.set(Attribute.heat, 0.85f);
        }});

        //7. basalt shelf: plain, solid, unremarkable - every map needs ground you simply build on
        basaltShelf = add(new Floor("basalt-shelf"){{
            variants = 3;
            wall = basaltWall;
            decoration = auroraBoulder;
            mapColor = Color.valueOf("474d59");
            attributes.set(Attribute.oil, 0.6f);
        }});

        /*
         * Wall ore. In v7.7 all of Aurelia's ore moved to the ground; v7.9 puts some of it back into the rock,
         * but as something you have to dig for: a wall ore is only reachable with a tool that can work a solid
         * tile - the rift collector's beam, the terra borer, or any unit with wall mining. Hardness matches the
         * ground ore, so nothing about the economy changes except where you can get it.
         */
        wallOreLumenite = add(new SeamOre("wall-ore-lumenite", lumenite){{
            wallOre = true;
            mapColor = AureliaFx.lumen;
        }});
        wallOreAurite = add(new SeamOre("wall-ore-aurite", aurite){{
            wallOre = true;
            mapColor = Color.valueOf("d9b36a");
        }});
        wallOreResonance = add(new SeamOre("wall-ore-resonance", resonanceShard){{
            wallOre = true;
            mapColor = AureliaFx.resonance;
        }});
    }

    /** Every floor this pack adds, for the planet's terrain list. */
    public static Block[] floors(){
        return new Block[]{glassFlat, emberAsh, frostCrust, auriteGravel, resonanceSilt, plasmaVent, basaltShelf,
            glassRidgeWall, emberWall, basaltWall, wallOreLumenite, wallOreAurite, wallOreResonance};
    }

    // =====================================================================================================
    // units
    // =====================================================================================================

    private static void loadUnits(){
        //the mining engineer: slow, tough, and the only ground unit that digs ore out of rock walls
        terraBorer = add(new Tank3D("terra-borer"){{
            builder = TerraUnits::terraBorer;
            speed = 0.62f;
            hitSize = 16f;
            health = 1900f;
            armor = 8f;
            rotateSpeed = 2.6f;
            omniMovement = false;
            range = 110f;
            targetAir = false;
            mineTier = 5;
            mineSpeed = 9f;
            mineWalls = true;
            mineFloor = true;
            buildSpeed = 1.4f;
            itemCapacity = 120;
            weapons.add(new LiftedWeapon(){{
                mirror = false; rotate = true; rotateSpeed = 3.2f; top = true; reload = 34f; recoil = 1.1f;
                shootSound = BHSounds.pulse;
                bullet = AureliaContent.bolt(5f, 26f, 24f, AureliaFx.lumen);
            }});
        }});

        //the line tank
        basaltGuard = add(new Tank3D("basalt-guard"){{
            builder = TerraUnits::basaltGuard;
            speed = 0.58f;
            hitSize = 24f;
            health = 5200f;
            armor = 16f;
            rotateSpeed = 1.7f;
            omniMovement = false;
            range = 215f;
            targetAir = false;
            crushDamage = 1.4f;
            for(int i = 0; i < 2; i++){
                int mount = i;
                weapons.add(new LiftedWeapon(){{
                    x = (mount == 0 ? 1.5f : -1.5f); y = 0f;
                    mirror = false; rotate = true; rotateSpeed = 1.8f; top = true; reload = 72f; recoil = 2.6f;
                    shootSound = BHSounds.pulse;
                    bullet = new BasicBulletType(8f, 130f){{
                        lifetime = 28f;
                        width = 11f; height = 19f;
                        splashDamage = 55f; splashDamageRadius = 26f;
                        frontColor = Color.white;
                        backColor = trailColor = hitColor = AureliaFx.resonance;
                        trailLength = 9; trailWidth = 2.2f;
                        hitEffect = despawnEffect = AureliaFx.lumenHit;
                        shootEffect = OwnFx.muzzle;
                        smokeEffect = OwnFx.muzzleSmoke;
                    }};
                }});
            }
        }});

        //the fast interceptor
        glassHarrier = add(new Ship3D("glass-harrier"){{
            builder = TerraUnits::glassHarrier;
            lowAltitude = false;
            speed = 5.4f;
            accel = 0.14f;
            drag = 0.03f;
            health = 430f;
            armor = 2f;
            hitSize = 9f;
            hover = 7f;
            range = 140f;
            engineColor = AureliaFx.lumen;
            targetAir = targetGround = true;
            for(int i = 0; i < 2; i++){
                int mount = i;
                weapons.add(new LiftedWeapon(){{
                    x = (mount == 0 ? 3.4f : -3.4f); y = 1.6f;
                    mirror = false; rotate = false; reload = 11f; muzzleZ = 0f;
                    shootCone = 22f;
                    shootSound = BHSounds.pulse;
                    bullet = AureliaContent.bolt(8.4f, 19f, 22f, AureliaFx.lumen);
                }});
            }
        }});

        //the support gunship: a belly turret that lobs shells over walls
        ventCaller = add(new Ship3D("vent-caller"){{
            builder = TerraUnits::ventCaller;
            lowAltitude = true;
            speed = 1.9f;
            accel = 0.07f;
            drag = 0.05f;
            health = 2400f;
            armor = 7f;
            hitSize = 18f;
            hover = 6f;
            range = 260f;
            engineColor = Color.valueOf("ffb45a");
            targetAir = false;
            targetGround = true;
            weapons.add(new LiftedWeapon(){{
                mirror = false; rotate = true; rotateSpeed = 2.4f; top = false; reload = 96f; recoil = 2f;
                inaccuracy = 3f;
                shootSound = BHSounds.pulse;
                bullet = new ArtilleryBulletType(4.2f, 55f){{
                    lifetime = 64f;
                    width = 14f; height = 14f;
                    splashDamage = 150f; splashDamageRadius = 40f;
                    frontColor = Color.white;
                    backColor = trailColor = hitColor = Color.valueOf("ffb45a");
                    trailLength = 10; trailWidth = 2.6f;
                    hitEffect = despawnEffect = AureliaFx.lumenHit;
                    shootEffect = OwnFx.muzzle;
                    smokeEffect = OwnFx.muzzleSmoke;
                }};
            }});
        }});
    }

    /** Hangs the four new units on the Aurelia fabricator. */
    private static void loadPlans(){
        mindustry.world.blocks.units.UnitFactory f = (mindustry.world.blocks.units.UnitFactory)aureliaFabricator;
        f.plans.add(new UnitPlan(terraBorer, 60f * 40f, with(lumenite, 150, aurite, 120, prismAlloy, 40)));
        f.plans.add(new UnitPlan(glassHarrier, 60f * 28f, with(lumenite, 110, prismGlass, 70, prismAlloy, 30)));
        f.plans.add(new UnitPlan(basaltGuard, 60f * 85f, with(lumenite, 320, aurite, 260, prismAlloy, 120, resonanceShard, 70)));
        f.plans.add(new UnitPlan(ventCaller, 60f * 70f, with(lumenite, 240, prismAlloy, 110, concordCore, 25, resonanceShard, 55)));
    }
    /**
     * Wall ore that keeps its own name. Vanilla's {@code OreBlock.setup()} rebuilds the display name as
     * "<item> " + bundle "wallore", a key the v8 server bundle does not define, so the name came out as
     * "Lumenite ???wallore???". This re-applies the mod bundle's own name after init.
     */
    public static class SeamOre extends OreBlock{
        public SeamOre(String name, Item item){
            super(name, item);
        }

        @Override
        public void init(){
            super.init();
            localizedName = Core.bundle.get("block." + name + ".name", localizedName);
        }
    }

}
