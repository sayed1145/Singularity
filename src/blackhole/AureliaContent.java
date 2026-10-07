package blackhole;

import arc.graphics.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.entities.pattern.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import sixfold.*;
import blackhole.g3d.Ship3D;
import blackhole.g3d.Tank3D;
import blackhole.g3d.LiftedWeapon;
import blackhole.models.UnitModels;

import static mindustry.type.ItemStack.with;

/**
 * Aurelia (v7.1): a fully self-contained campaign planet. Nothing vanilla is used or shown there: own items,
 * liquids, status effects, terrain, transport, liquid handling, power, production, crafting, defence, storage,
 * units (ground, air, naval), turrets, particle effects ({@link OwnFx}) and research tree. Logic blocks reuse the
 * vanilla logic classes, built from Aurelia materials. {@link #isolate()} hides every non-Aurelia content from
 * Aurelia and Aurelia-only content from the other planets.
 */
public final class AureliaContent{
    public static Planet aurelia;
    // resources
    public static Item lumenite, aurite, silt, prismGlass, resonanceShard, prismAlloy, concordCore;
    public static Liquid tidewater, lumenPlasma;
    public static StatusEffect latticeGrip, stasisLock;
    public static StatusEffect brineSoaked, lumenBurn, resonanceShock;
    // terrain
    public static AureliaFloor auroraPlate, resonanceBed;
    public static Floor auroraSand, prismShale, lumenMoss, auroraWater, auroraSandWater, deepAuroraWater;
    public static OreBlock oreLumenite, oreAurite, oreResonance;
    public static StaticWall auroraWall, resonanceWall, prismShaleWall, auroraDuneWall;
    public static Prop auroraBoulder, resonanceCrystal;
    // blocks
    public static AureliaCoreBlock aureliaCore;
    public static Block lumenConveyor, lumenJunction, lumenRouter, lumenSorter, lumenBridge,
        tidePump, lumenConduit, lumenLiquidRouter, lumenLiquidBridge,
        lumenBurner, auroraPanel, tideTurbine, lumenNode, lumenCell,
        lumenExtractor,
        siltKiln, prismPress, plasmaMixer, prismResonator, riftAnchor, harmonicRelay,
        lumeniteWall, lumeniteWallLarge, auriteWall, auriteWallLarge,
        lumenSpark, shardVolley, prismPylon, arcMortar,
        lumenMender, lumenVault, lumenUnloader,
        aureliaFabricator, tideDock,
        lumenProcessor, lumenSwitch, lumenMessage, lumenMemory, lumenDisplay;
    // units
    public static Ship3D lumenPilot, glint, auroraGuard, auroraHeavy, tideSkiff, tideWarden;
    public static Tank3D shardRover, auriteBulwark;

    /** every piece of Aurelia content (shown on Aurelia only, see {@link #isolate()}) */
    public static final Seq<UnlockableContent> own = new Seq<>();
    /** v7.8: kept empty - no Aurelia content is shared with Serpulo or Erekir any more */
    public static final Seq<UnlockableContent> shared = new Seq<>();
    private static boolean loaded;

    private AureliaContent(){}

    static <T extends UnlockableContent> T own(T c){
        own.add(c);
        return c;
    }

    public static void load(){
        if(loaded) return;
        loaded = true;
        loadResources();
        loadTerrain();
        loadUnits();
        loadBlocks();
        loadPlanet();
        //v7.8: nothing is shared with Serpulo / Erekir any more - see AureliaMerge.purge()
    }

    private static void loadResources(){
        lumenite = own(new Item("lumenite", AureliaFx.lumen){{
            hardness = 2;
            cost = 1.0f;
        }});
        aurite = own(new Item("aurite", Color.valueOf("d9b36a")){{
            hardness = 2;
            cost = 1.2f;
            healthScaling = 0.1f;
        }});
        silt = own(new Item("silt", Color.valueOf("9c93ab")){{
            hardness = 0;
            lowPriority = true;
            buildable = false;
        }});
        prismGlass = own(new Item("prism-glass", Color.valueOf("bfe6ff")){{
            cost = 1.4f;
        }});
        resonanceShard = own(new Item("resonance-shard", AureliaFx.resonance){{
            //v7.7: hardness so the new ground ore needs a tier-4 drill (auger / collector) or better
            hardness = 4;
            cost = 1.35f;
            charge = 0.28f;
        }});
        prismAlloy = own(new Item("prism-alloy", Color.valueOf("e8f5ff")){{
            cost = 1.75f;
            charge = 0.52f;
            healthScaling = 0.16f;
        }});
        concordCore = own(new Item("concord-core", AureliaFx.concord){{
            cost = 2.35f;
            charge = 0.80f;
        }});

        brineSoaked = own(new StatusEffect("brine-soaked"){{
            color = Color.valueOf("5fb7ff");
            speedMultiplier = 0.92f;
            effect = OwnFx.splash;
            effectChance = 0.09f;
        }});
        lumenBurn = own(new StatusEffect("lumen-burn"){{
            color = AureliaFx.lumen;
            damage = 0.12f;
            effect = OwnFx.burn;
            effectChance = 0.12f;
        }});
        resonanceShock = own(new StatusEffect("resonance-shock"){{
            color = AureliaFx.resonance;
            reloadMultiplier = 0.8f;
            speedMultiplier = 0.88f;
            damage = 0.04f;
            effect = OwnFx.hit;
            effectChance = 0.08f;
        }});
        //v7.6: the Aegis Keeper's containment lattice
        latticeGrip = own(new StatusEffect("lattice-grip"){{
            color = Color.valueOf("8fe9ff");
            speedMultiplier = 0.55f;
            reloadMultiplier = 0.7f;
            effect = OwnFx.hit;
            effectChance = 0.05f;
        }});
        stasisLock = own(new StatusEffect("stasis-lock"){{
            color = Color.valueOf("b69cff");
            speedMultiplier = 0.001f;
            reloadMultiplier = 0.001f;
            disarm = true;
            effect = OwnFx.hit;
            effectChance = 0.12f;
        }});

        brineSoaked.init(() -> {
            //wet + shock: the brine conducts, a burst of extra damage
            brineSoaked.affinity(resonanceShock, (unit, result, time) -> {
                unit.damagePierce(16f);
                result.set(resonanceShock, time);
            });
            brineSoaked.opposite(lumenBurn);
        });

        tidewater = own(new Liquid("tidewater", Color.valueOf("3f7fc0")){{
            heatCapacity = 0.45f;
            effect = brineSoaked;
            boilPoint = 0.55f;
            alwaysUnlocked = false;
        }});
        lumenPlasma = own(new Liquid("lumen-plasma", Color.valueOf("7ff0d8")){{
            temperature = 0.75f;
            viscosity = 0.4f;
            effect = lumenBurn;
            lightColor = Color.valueOf("7ff0d8").a(0.4f);
        }});
    }

    private static void loadTerrain(){
        auroraWall = own(new StaticWall("aurora-wall"){{
            variants = 2;
            mapColor = Color.valueOf("5a74a0");
        }});
        resonanceWall = own(new StaticWall("resonance-wall"){{
            variants = 2;
            mapColor = Color.valueOf("7c62ad");
        }});
        prismShaleWall = own(new StaticWall("prism-shale-wall"){{
            variants = 2;
            mapColor = Color.valueOf("3e4452");
        }});
        auroraDuneWall = own(new StaticWall("aurora-dune-wall"){{
            variants = 2;
            mapColor = Color.valueOf("b9aec9");
        }});
        auroraBoulder = own(new Prop("aurora-boulder"){{
            variants = 2;
            mapColor = Color.valueOf("56637d");
        }});
        resonanceCrystal = own(new Prop("resonance-crystal"){{
            variants = 2;
            mapColor = Color.valueOf("a88af0");
        }});
        auroraPlate = own(new AureliaFloor("aurora-plate"){{
            variants = 3;
            wall = auroraWall;
            decoration = auroraBoulder;
            mapColor = Color.valueOf("4d6388");
            albedo = 0.5f;
            attributes.set(Attribute.water, 0.1f);
        }});
        resonanceBed = own(new AureliaFloor("resonance-bed"){{
            variants = 3;
            wall = resonanceWall;
            decoration = resonanceCrystal;
            pulse = true;
            pulseInterval = 115f;
            mapColor = Color.valueOf("5e4b8a");
            albedo = 0.6f;
            emitLight = true;
            lightRadius = 28f;
            lightColor = AureliaFx.resonance.cpy().a(0.18f);
            attributes.set(Attribute.heat, 0.15f);
        }});
        auroraSand = own(new Floor("aurora-sand"){{
            variants = 3;
            itemDrop = silt;
            wall = auroraDuneWall;
            decoration = auroraBoulder;
            mapColor = Color.valueOf("9c93ab");
            attributes.set(Attribute.oil, 0.7f);
        }});
        prismShale = own(new Floor("prism-shale"){{
            variants = 3;
            wall = prismShaleWall;
            decoration = auroraBoulder;
            mapColor = Color.valueOf("30343f");
            attributes.set(Attribute.oil, 1.2f);
        }});
        lumenMoss = own(new Floor("lumen-moss"){{
            variants = 3;
            wall = auroraWall;
            decoration = resonanceCrystal;
            mapColor = Color.valueOf("2f5a5f");
            attributes.set(Attribute.spores, 0.3f);
            emitLight = true;
            lightRadius = 20f;
            lightColor = AureliaFx.lumen.cpy().a(0.12f);
        }});
        auroraWater = own(new Floor("aurora-water"){{
            variants = 2;
            speedMultiplier = 0.5f;
            status = brineSoaked;
            statusDuration = 90f;
            liquidDrop = tidewater;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
            albedo = 0.9f;
            supportsOverlay = true;
            mapColor = Color.valueOf("3a6c9c");
        }});
        auroraSandWater = own(new Floor("aurora-sand-water"){{
            shallow = true;
            variants = 2;
            speedMultiplier = 0.8f;
            statusDuration = 50f;
            status = brineSoaked;
            liquidDrop = tidewater;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
            albedo = 0.9f;
            supportsOverlay = true;
            mapColor = Color.valueOf("7f93b5");
        }});
        deepAuroraWater = own(new Floor("deep-aurora-water"){{
            variants = 2;
            speedMultiplier = 0.2f;
            liquidDrop = tidewater;
            liquidMultiplier = 1.5f;
            isLiquid = true;
            status = brineSoaked;
            statusDuration = 120f;
            drownTime = 200f;
            cacheLayer = CacheLayer.water;
            albedo = 0.9f;
            supportsOverlay = true;
            mapColor = Color.valueOf("1e3b66");
        }});
        /*
         * v7.7: the ground is the mine. Aurelia's ore is floor overlay only - no wall ore, no crystal props to
         * break - and the patches are wider and more frequent than in v7.6 (threshold 0.86/0.84 -> 0.80/0.79,
         * scale 22/24 -> 27/29), so a drill line on open ground is the main supply of both base metals.
         */
        oreLumenite = own(new OreBlock("ore-lumenite", lumenite){{
            oreDefault = true;
            oreThreshold = 0.80f;
            oreScale = 27f;
            mapColor = AureliaFx.lumen;
        }});
        oreAurite = own(new OreBlock("ore-aurite", aurite){{
            oreDefault = true;
            oreThreshold = 0.79f;
            oreScale = 29f;
            mapColor = Color.valueOf("d9b36a");
        }});
        //v7.7: the third ground ore - resonance shard straight out of the soil, the target the tier-4 and
        //tier-5 drills never had. Rare on purpose: the sifter is still the steady supply, this is the bonus.
        oreResonance = own(new OreBlock("ore-resonance", resonanceShard){{
            oreDefault = true;
            oreThreshold = 0.885f;
            oreScale = 19f;
            mapColor = AureliaFx.resonance;
        }});
    }

    /** Standard lumen bolt used by several Aurelia weapons. */
    static BasicBulletType bolt(float speed, float damage, float life, Color c){
        return new BasicBulletType(speed, damage){{
            lifetime = life;
            width = 5f + damage * 0.04f;
            height = width * 1.8f;
            frontColor = Color.white;
            backColor = trailColor = hitColor = c;
            hitEffect = despawnEffect = AureliaFx.lumenHit;
            shootEffect = OwnFx.muzzle;
            smokeEffect = OwnFx.muzzleSmoke;
        }};
    }

    private static void loadUnits(){
        // Player chassis: 3D pearl delta whose wings sweep with speed; the nose lens mines and builds.
        lumenPilot = own(new Ship3D("lumen-pilot"){{
            builder = UnitModels::lumenPilot;
            lowAltitude = true;
            playerControllable = true;
            logicControllable = true;
            speed = 3.15f;
            accel = 0.09f;
            drag = 0.045f;
            health = 520f;
            armor = 3f;
            hitSize = 15f;
            hover = 5f;
            mineTier = 3;
            mineSpeed = 6.2f;
            mineRange = 85f;
            buildSpeed = 0.85f;
            range = 130f;
            engineColor = AureliaFx.lumen;
            weapons.add(new LiftedWeapon(){{
                x = 0f; y = 6f; mirror = false; rotate = false; reload = 16f; muzzleZ = 0f;
                shootSound = BHSounds.pulse;
                bullet = bolt(6.4f, 22f, 25f, AureliaFx.lumen);
            }});
        }});

        glint = own(new Ship3D("glint"){{
            builder = UnitModels::glint;
            lowAltitude = true;
            speed = 3.7f;
            accel = 0.10f;
            drag = 0.035f;
            health = 420f;
            armor = 2f;
            hitSize = 11f;
            hover = 5f;
            range = 145f;
            engineColor = AureliaFx.lumen;
            weapons.add(new LiftedWeapon(){{
                x = 0f; y = 5f; reload = 18f; mirror = false; rotate = false; muzzleZ = 0f;
                shootSound = BHSounds.pulse;
                bullet = bolt(7f, 28f, 28f, AureliaFx.lumen);
            }});
        }});

        auroraGuard = own(new Ship3D("aurora-guard"){{
            builder = UnitModels::auroraGuard;
            lowAltitude = true;
            speed = 1.65f;
            accel = 0.05f;
            drag = 0.022f;
            health = 5200f;
            armor = 14f;
            hitSize = 30f;
            hover = 7f;
            range = 230f;
            engineColor = AureliaFx.resonance;
            abilities.add(new PrismWardAbility(58f, 2.6f, 950f, 60f * 8f){{ color = AureliaFx.resonance; height = 11f; }});
            weapons.add(new LiftedWeapon(){{
                x = 9.6f; y = 7f; reload = 72f; mirror = true; rotate = true; rotateSpeed = 3f; muzzleZ = 0.7f;
                recoil = 2.6f; shootSound = BHSounds.pulse;
                bullet = new BasicBulletType(6.2f, 85f){{
                    lifetime = 42f;
                    width = 9f; height = 15f;
                    pierce = true; pierceCap = 3;
                    frontColor = Color.white;
                    backColor = trailColor = hitColor = AureliaFx.resonance;
                    hitEffect = despawnEffect = AureliaFx.lumenHit;
                    shootEffect = OwnFx.muzzle;
                    smokeEffect = OwnFx.muzzleSmoke;
                    status = resonanceShock;
                    statusDuration = 90f;
                }};
            }});
        }});

        // v7.8: the old detainer was deleted. This is a plain heavy gunship in its place - no special
        // systems, just a tough airframe with a pulse cannon and a pair of side guns.
        auroraHeavy = own(new Ship3D("aurora-heavy"){{
            builder = blackhole.models.NovaUnits::auroraHeavy;
            lowAltitude = true;
            speed = 1.75f;
            accel = 0.06f;
            drag = 0.045f;
            rotateSpeed = 3.2f;
            health = 9000f;
            armor = 16f;
            hitSize = 32f;
            hover = 9f;
            range = 210f;
            engineColor = AureliaFx.lumen;
            targetAir = targetGround = true;
            faceTarget = true;
            weapons.add(new LiftedWeapon(){{
                x = 0f; y = 10f; mirror = false; rotate = true; rotateSpeed = 3.4f; reload = 48f; muzzleZ = 2f;
                shootCone = 24f;
                shootSound = BHSounds.pulse;
                bullet = new BasicBulletType(8f, 130f){{
                    lifetime = 28f;
                    width = 11f; height = 20f;
                    pierce = true; pierceCap = 2;
                    frontColor = Color.white;
                    backColor = trailColor = hitColor = AureliaFx.resonance;
                    trailLength = 9; trailWidth = 2.4f;
                    splashDamage = 45f; splashDamageRadius = 22f;
                    hitEffect = despawnEffect = AureliaFx.lumenHit;
                    shootEffect = OwnFx.muzzle;
                    smokeEffect = OwnFx.muzzleSmoke;
                }};
            }});
            weapons.add(new LiftedWeapon(){{
                x = 9.5f; y = -2f; mirror = true; rotate = false; reload = 20f; muzzleZ = 0.8f;
                shootCone = 30f;
                inaccuracy = 3f;
                shootSound = BHSounds.pulse;
                bullet = bolt(7.5f, 36f, 30f, AureliaFx.lumen);
            }});
        }});

        shardRover = own(new Tank3D("shard-rover"){{
            builder = UnitModels::shardRover;
            speed = 0.95f;
            hitSize = 13f;
            health = 640f;
            armor = 4f;
            rotateSpeed = 3.4f;
            omniMovement = false;
            range = 130f;
            targetAir = false;
            weapons.add(new LiftedWeapon(){{
                mirror = false; rotate = true; rotateSpeed = 4f; top = true; reload = 24f; recoil = 1f;
                shootSound = BHSounds.pulse;
                bullet = bolt(5.2f, 24f, 26f, AureliaFx.resonance);
            }});
        }});

        auriteBulwark = own(new Tank3D("aurite-bulwark"){{
            builder = UnitModels::auriteBulwark;
            speed = 0.62f;
            hitSize = 24f;
            health = 3600f;
            armor = 11f;
            rotateSpeed = 2.2f;
            omniMovement = false;
            crushDamage = 1f;
            range = 190f;
            targetAir = false;
            weapons.add(new LiftedWeapon(){{
                mirror = false; rotate = true; rotateSpeed = 2.4f; top = true; reload = 30f; recoil = 1.6f; shake = 1f;
                barrelSpread = 2.2f;
                shoot = new ShootAlternate(2.2f);
                shootSound = BHSounds.thump;
                bullet = new BasicBulletType(5.6f, 70f){{
                    lifetime = 34f;
                    width = 9f; height = 13f;
                    frontColor = Color.white;
                    backColor = trailColor = hitColor = Color.valueOf("d9b36a");
                    splashDamage = 30f; splashDamageRadius = 18f;
                    hitEffect = despawnEffect = OwnFx.hitBig;
                    shootEffect = OwnFx.muzzle;
                    smokeEffect = OwnFx.muzzleSmoke;
                }};
            }});
        }});

        tideSkiff = own(new Ship3D("tide-skiff"){{
            builder = UnitModels::tideSkiff;
            flying = false;
            naval = true;
            constructor = UnitWaterMove::create;
            hover = 0f;
            bankScale = 6f;
            pitchScale = 3f;
            speed = 1.2f;
            drag = 0.13f;
            accel = 0.4f;
            health = 380f;
            armor = 2f;
            hitSize = 12f;
            range = 150f;
            engineColor = AureliaFx.lumen;
            weapons.add(new LiftedWeapon(){{
                mirror = false; rotate = true; rotateSpeed = 5f; reload = 20f;
                shootSound = BHSounds.pulse;
                bullet = bolt(5.6f, 18f, 28f, AureliaFx.lumen);
            }});
        }});

        tideWarden = own(new Ship3D("tide-warden"){{
            builder = UnitModels::tideWarden;
            flying = false;
            naval = true;
            constructor = UnitWaterMove::create;
            hover = 0f;
            bankScale = 4f;
            pitchScale = 2f;
            speed = 0.85f;
            drag = 0.16f;
            accel = 0.3f;
            health = 1900f;
            armor = 7f;
            hitSize = 22f;
            range = 200f;
            engineColor = AureliaFx.lumen;
            for(int i = 0; i < 2; i++){
                weapons.add(new LiftedWeapon(){{
                    mirror = false; rotate = true; rotateSpeed = 3f; reload = 26f; recoil = 1f;
                    barrelSpread = 1.1f;
                    shoot = new ShootAlternate(1.1f);
                    shootSound = BHSounds.pulse;
                    bullet = bolt(6f, 34f, 32f, AureliaFx.lumen);
                }});
            }
        }});
    }

    private static void loadBlocks(){
        // ------------------------------------------------------------------ core, storage
        aureliaCore = own(new AureliaCoreBlock("aurelia-core"){{
            requirements(Category.effect, with(lumenite, 3000, aurite, 2000, prismAlloy, 600));
            size = 4;
            health = 6800;
            itemCapacity = 8000;
            unitCapModifier = 18;
            unitType = lumenPilot;
            envEnabled = Env.any;
            alwaysUnlocked = true;
        }});
        lumenVault = own(new StorageBlock("lumen-vault"){{
            requirements(Category.effect, with(aurite, 250, lumenite, 125));
            size = 3;
            health = 900;
            itemCapacity = 1000;
            coreMerge = true;
        }});
        lumenUnloader = own(new Unloader("lumen-unloader"){{
            //v8.0: it is a transport block, so it lives with the conveyors and routers, not in effect
            requirements(Category.distribution, with(aurite, 25, prismGlass, 30));
            speed = 60f / 11f;
            group = BlockGroup.transportation;
        }});
        lumenMender = own(new AureliaParts.LumenMenderBlock("lumen-mender"){{
            requirements(Category.effect, with(lumenite, 30, prismGlass, 12));
            consumePower(0.3f);
            size = 1;
            reload = 200f;
            range = 40f;
            healPercent = 12f;
            phaseBoost = 0f;
            phaseRangeBoost = 0f;
            health = 80;
            baseColor = AureliaFx.lumen;
        }});

        // ------------------------------------------------------------------ transport
        lumenConveyor = own(new AureliaParts.LumenConveyor("lumen-conveyor"){{
            requirements(Category.distribution, with(lumenite, 1));
            health = 50;
            speed = 0.045f;
            displayedSpeed = 6.3f;
            buildCostMultiplier = 2f;
        }});
        lumenJunction = own(new Junction("lumen-junction"){{
            requirements(Category.distribution, with(lumenite, 3));
            speed = 26;
            capacity = 6;
            health = 40;
            buildCostMultiplier = 6f;
        }});
        lumenRouter = own(new Router("lumen-router"){{
            requirements(Category.distribution, with(lumenite, 3));
            buildCostMultiplier = 4f;
        }});
        lumenSorter = own(new Sorter("lumen-sorter"){{
            requirements(Category.distribution, with(lumenite, 2, aurite, 2));
            buildCostMultiplier = 3f;
        }});
        lumenBridge = own(new BufferedItemBridge("lumen-bridge"){{
            requirements(Category.distribution, with(lumenite, 6, aurite, 6));
            fadeIn = moveArrows = false;
            range = 4;
            speed = 74f;
            arrowSpacing = 6f;
            bufferCapacity = 14;
        }});
        ((AureliaParts.LumenConveyor)lumenConveyor).junction = lumenJunction;
        ((AureliaParts.LumenConveyor)lumenConveyor).bridge = lumenBridge;

        // ------------------------------------------------------------------ liquids
        tidePump = own(new Pump("tide-pump"){{
            requirements(Category.liquid, with(lumenite, 30, aurite, 15));
            size = 2;
            pumpAmount = 7f / 60f;
            liquidCapacity = 40f;
            consumePower(0.15f);
        }});
        lumenLiquidBridge = own(new LiquidBridge("lumen-liquid-bridge"){{
            requirements(Category.liquid, with(prismGlass, 6, aurite, 4));
            fadeIn = moveArrows = false;
            arrowSpacing = 6f;
            range = 4;
            hasPower = false;
            liquidCapacity = 100f;
        }});
        lumenConduit = own(new AureliaParts.LumenConduit("lumen-conduit"){{
            requirements(Category.liquid, with(prismGlass, 1, aurite, 1));
            health = 45;
            liquidCapacity = 14f;
            liquidPressure = 1.05f;
        }});
        ((AureliaParts.LumenConduit)lumenConduit).bridge = lumenLiquidBridge;
        lumenLiquidRouter = own(new LiquidRouter("lumen-liquid-router"){{
            requirements(Category.liquid, with(prismGlass, 3, aurite, 2));
            liquidCapacity = 30f;
            liquidPadding = 3f / 4f;
            solid = false;
        }});

        // ------------------------------------------------------------------ power
        lumenBurner = own(new ConsumeGenerator("lumen-burner"){{
            requirements(Category.power, with(lumenite, 28, aurite, 14));
            size = 2;
            powerProduction = 1.1f;
            itemDuration = 120f;
            consumeItem(lumenite);
            generateEffect = OwnFx.burn;
            effectChance = 0.05f;
            ambientSound = Sounds.loopSmelter;
            ambientSoundVolume = 0.03f;
        }});
        auroraPanel = own(new SolarGenerator("aurora-panel"){{
            requirements(Category.power, with(lumenite, 24, aurite, 12));
            size = 2;
            health = 360;
            powerProduction = 0.72f;
        }});
        tideTurbine = own(new ConsumeGenerator("tide-turbine"){{
            requirements(Category.power, with(aurite, 40, prismGlass, 20, lumenite, 30));
            size = 2;
            powerProduction = 1.8f;
            consumeLiquid(tidewater, 0.2f);
            liquidCapacity = 30f;
            generateEffect = OwnFx.splash;
            effectChance = 0.03f;
        }});
        lumenNode = own(new PowerNode("lumen-node"){{
            requirements(Category.power, with(lumenite, 2, aurite, 1));
            maxNodes = 10;
            laserRange = 6.5f;
            laserColor1 = Color.white;
            laserColor2 = AureliaFx.lumen;
        }});
        lumenCell = own(new AureliaParts.LumenCellBlock("lumen-cell"){{
            requirements(Category.power, with(lumenite, 10, aurite, 20));
            consumePowerBuffered(1200f);
            baseExplosiveness = 1f;
        }});

        // ------------------------------------------------------------------ production
        lumenExtractor = own(new blackhole.models.Drill3D("lumen-extractor"){{
            requirements(Category.production, with(lumenite, 14));
            size = 2;
            health = 420;
            tier = 3;
            drillTime = 74f;
            itemCapacity = 20;
            drawRim = true;
            heatColor = AureliaFx.lumen;
            drillEffect = AureliaFx.lumenHit;
            updateEffect = AureliaFx.lumenHit;
            updateEffectChance = 0.006f;
            drillEffectChance = 0.05f;
            rotateSpeed = 3f;
            envEnabled = Env.any;
        }});

        // ------------------------------------------------------------------ crafting
        siltKiln = own(new GenericCrafter("silt-kiln"){{
            requirements(Category.crafting, with(lumenite, 40, aurite, 25));
            size = 2;
            health = 320;
            craftTime = 50f;
            outputItem = new ItemStack(prismGlass, 1);
            consumeItems(with(silt, 2, lumenite, 1));
            consumePower(0.6f);
            craftEffect = OwnFx.smoke;
        }});
        plasmaMixer = own(new GenericCrafter("plasma-mixer"){{
            requirements(Category.crafting, with(aurite, 60, prismGlass, 40, lumenite, 40));
            size = 2;
            health = 380;
            craftTime = 60f;
            hasLiquids = true;
            liquidCapacity = 30f;
            outputLiquid = new LiquidStack(lumenPlasma, 0.1f);
            consumeLiquid(tidewater, 0.12f);
            consumeItem(lumenite, 1);
            consumePower(0.8f);
            craftEffect = OwnFx.splash;
        }});
        prismPress = own(new GenericCrafter("prism-press"){{
            requirements(Category.crafting, with(aurite, 80, prismGlass, 40, resonanceShard, 16));
            size = 3;
            health = 980;
            craftTime = 72f;
            outputItem = new ItemStack(prismAlloy, 1);
            consumeItems(with(lumenite, 3, resonanceShard, 1));
            consumePower(1.45f);
            craftEffect = AureliaFx.prismCraft;
        }});
        prismResonator = own(new GenericCrafter("prism-resonator"){{
            requirements(Category.crafting, with(aurite, 120, prismGlass, 60, prismAlloy, 40));
            size = 3;
            health = 1260;
            craftTime = 128f;
            hasLiquids = true;
            outputItem = new ItemStack(prismAlloy, 2);
            consumeItems(with(lumenite, 3, resonanceShard, 2));
            consumeLiquid(lumenPlasma, 0.1f);
            consumePower(2.4f);
            craftEffect = AureliaFx.prismCraft;
        }});
        riftAnchor = own(new ResonanceAnchorBlock("rift-anchor"){{
            requirements(Category.crafting, with(lumenite, 60, aurite, 40));
            size = 2;
            health = 850;
            output = resonanceShard;
            consumePower(0.75f);
        }});
        harmonicRelay = own(new HarmonicRelayBlock("harmonic-relay"){{
            requirements(Category.crafting, with(aurite, 120, lumenite, 90, resonanceShard, 30, prismGlass, 40));
            size = 3;
            health = 1500;
            output = concordCore;
            consumePower(1.65f);
        }});

        // ------------------------------------------------------------------ defence
        lumeniteWall = own(new Wall("lumenite-wall"){{
            requirements(Category.defense, with(lumenite, 6));
            health = 90 * 4;
        }});
        lumeniteWallLarge = own(new Wall("lumenite-wall-large"){{
            requirements(Category.defense, with(lumenite, 24));
            health = 90 * 4 * 4;
            size = 2;
        }});
        auriteWall = own(new Wall("aurite-wall"){{
            requirements(Category.defense, with(aurite, 6));
            health = 120 * 4;
            armor = 3f;
        }});
        auriteWallLarge = own(new Wall("aurite-wall-large"){{
            requirements(Category.defense, with(aurite, 24));
            health = 120 * 4 * 4;
            armor = 3f;
            size = 2;
        }});

        lumenSpark = own(new PowerTurret("lumen-spark"){{
            requirements(Category.turret, with(lumenite, 35, aurite, 15));
            size = 2;
            health = 420;
            range = 150f;
            reload = 22f;
            rotateSpeed = 8f;
            recoil = 1f;
            consumePower(1.2f);
            shootSound = BHSounds.pulse;
            shootType = new BasicBulletType(6f, 18f){{
                lifetime = 26f;
                width = 6f; height = 10f;
                frontColor = Color.white;
                backColor = trailColor = hitColor = AureliaFx.lumen;
                hitEffect = despawnEffect = AureliaFx.lumenHit;
                shootEffect = OwnFx.muzzle;
                smokeEffect = OwnFx.muzzleSmoke;
                status = resonanceShock;
                statusDuration = 40f;
            }};
        }});
        shardVolley = own(new ItemTurret("shard-volley"){{
            requirements(Category.turret, with(aurite, 45, lumenite, 30));
            size = 2;
            health = 520;
            range = 185f;
            reload = 18f;
            rotateSpeed = 7f;
            recoil = 1.2f;
            maxAmmo = 30;
            shoot = new ShootAlternate(2f);
            shootSound = BHSounds.thump;
            coolant = new ConsumeLiquid(tidewater, 0.1f);
            consume(coolant);
            ammo(
                aurite, new BasicBulletType(5.5f, 22f){{
                    lifetime = 34f;
                    width = 7f; height = 10f;
                    frontColor = Color.white;
                    backColor = trailColor = hitColor = Color.valueOf("d9b36a");
                    hitEffect = despawnEffect = OwnFx.hit;
                    shootEffect = OwnFx.muzzle;
                    smokeEffect = OwnFx.muzzleSmoke;
                    ammoMultiplier = 2f;
                }},
                resonanceShard, new BasicBulletType(6f, 36f){{
                    lifetime = 31f;
                    width = 8f; height = 12f;
                    pierce = true; pierceCap = 2;
                    frontColor = Color.white;
                    backColor = trailColor = hitColor = AureliaFx.resonance;
                    hitEffect = despawnEffect = AureliaFx.lumenHit;
                    shootEffect = OwnFx.muzzle;
                    smokeEffect = OwnFx.muzzleSmoke;
                    status = resonanceShock;
                    statusDuration = 60f;
                    ammoMultiplier = 3f;
                }}
            );
        }});
        prismPylon = own(new PowerTurret("prism-pylon"){{
            requirements(Category.turret, with(aurite, 140, prismGlass, 60, prismAlloy, 36, resonanceShard, 18));
            size = 3;
            health = 1250;
            range = 255f;
            reload = 44f;
            rotateSpeed = 4.8f;
            consumePower(2.6f);
            shootSound = BHSounds.pulse;
            shootType = new BasicBulletType(7f, 100f){{
                lifetime = 37f;
                width = 8f; height = 14f;
                pierce = true; pierceCap = 4;
                frontColor = Color.white;
                backColor = trailColor = hitColor = AureliaFx.lumen;
                hitEffect = despawnEffect = AureliaFx.lumenHit;
                shootEffect = OwnFx.muzzle;
                smokeEffect = OwnFx.muzzleSmoke;
            }};
        }});
        arcMortar = own(new PowerTurret("arc-mortar"){{
            requirements(Category.turret, with(aurite, 260, prismGlass, 90, prismAlloy, 100, concordCore, 16));
            size = 4;
            health = 2100;
            range = 310f;
            minRange = 55f;
            reload = 125f;
            rotateSpeed = 2.8f;
            consumePower(4.5f);
            shootSound = BHSounds.thump;
            shootType = new ArtilleryBulletType(4.5f, 260f){{
                lifetime = 70f;
                width = 14f; height = 16f;
                splashDamage = 180f; splashDamageRadius = 50f;
                frontColor = Color.white;
                backColor = trailColor = hitColor = AureliaFx.concord;
                hitEffect = despawnEffect = AureliaFx.concordBirth;
                shootEffect = OwnFx.muzzle;
                smokeEffect = OwnFx.muzzleSmoke;
                trailEffect = OwnFx.trail;
            }};
        }});

        // ------------------------------------------------------------------ units
        aureliaFabricator = own(new AureliaUnitFactory("aurelia-fabricator"){{
            requirements(Category.units, with(lumenite, 260, resonanceShard, 50));
            size = 4;
            health = 2100;
            consumePower(2.4f);
            plans.add(new UnitPlan(shardRover, 60f * 20f, with(lumenite, 35, resonanceShard, 8)));
            plans.add(new UnitPlan(glint, 60f * 24f, with(lumenite, 30, resonanceShard, 10)));
            plans.add(new UnitPlan(auriteBulwark, 60f * 50f, with(lumenite, 120, prismAlloy, 40, resonanceShard, 20)));
            plans.add(new UnitPlan(auroraGuard, 60f * 88f, with(prismAlloy, 70, concordCore, 18, lumenite, 90)));
            plans.add(new UnitPlan(auroraHeavy, 60f * 120f, with(prismAlloy, 160, concordCore, 40, lumenite, 200, resonanceShard, 60)));
            envEnabled = Env.any;
        }});
        tideDock = own(new AureliaUnitFactory("tide-dock"){{
            requirements(Category.units, with(lumenite, 150, aurite, 120, prismGlass, 80));
            size = 3;
            health = 1200;
            consumePower(1.2f);
            floating = true;
            plans.add(new UnitPlan(tideSkiff, 60f * 30f, with(lumenite, 25, aurite, 20)));
            plans.add(new UnitPlan(tideWarden, 60f * 60f, with(aurite, 90, prismGlass, 40, prismAlloy, 20)));
        }});

        // ------------------------------------------------------------------ logic (vanilla logic, Aurelia materials)
        lumenProcessor = own(new LogicBlock("lumen-processor"){{
            requirements(Category.logic, with(lumenite, 60, aurite, 40, prismGlass, 30));
            instructionsPerTick = 3;
            size = 1;
        }});
        lumenMemory = own(new MemoryBlock("lumen-memory"){{
            requirements(Category.logic, with(aurite, 30, prismGlass, 20));
            memoryCapacity = 64;
        }});
        lumenSwitch = own(new SwitchBlock("lumen-switch"){{
            requirements(Category.logic, with(lumenite, 5, aurite, 5));
        }});
        lumenMessage = own(new MessageBlock("lumen-message"){{
            requirements(Category.logic, with(lumenite, 5, aurite, 5));
        }});
        lumenDisplay = own(new LogicDisplay("lumen-display"){{
            requirements(Category.logic, with(aurite, 60, prismGlass, 50, lumenite, 40));
            displaySize = 80;
            size = 3;
        }});

    }

    private static void loadPlanet(){
        aurelia = new Planet("aurelia", Planets.sun, 0.86f, 3){{
            generator = new AureliaPlanetGenerator();
            meshLoader = () -> new HexMesh(this, 6);
            cloudMeshLoader = () -> new MultiMesh(
                new HexSkyMesh(this, 17, 0.10f, 0.13f, 5, Color.valueOf("8cdcff").a(0.48f), 2, 0.44f, 0.92f, 0.42f),
                new HexSkyMesh(this, 29, 0.34f, 0.16f, 5, Color.valueOf("b69cff").a(0.32f), 2, 0.46f, 1.15f, 0.46f)
            );
            alwaysUnlocked = true;
            accessible = true;
            allowLaunchToNumbered = true;
            allowLaunchLoadout = false;
            allowLaunchSchematics = false;
            allowSelfSectorLaunch = true;
            allowSectorInvasion = true;
            allowWaves = true;
            allowCampaignRules = true;
            defaultCore = aureliaCore;
            defaultEnv = Env.terrestrial | Env.groundWater | Env.oxygen;
            atmosphereColor = Color.valueOf("6b8fd5");
            landCloudColor = Color.valueOf("b7e8ff").a(0.35f);
            iconColor = Color.valueOf("91e8ff");
            atmosphereRadIn = 0.02f;
            atmosphereRadOut = 0.28f;
            startSector = 10;
            //v7.5: you land with the core only, exactly like Serpulo and Erekir. Everything else has to be
            //researched - nothing in the Aurelia tree unlocks itself for free any more.
            unlockedOnLand.add(aureliaCore);
            ruleSetter = rules -> {
                rules.waveTeam = mindustry.game.Team.crux;
                rules.placeRangeCheck = false;
                rules.hideSpawns = false;
                rules.coreDestroyClear = true;
                //v7.6: fog of war hid most of the sector and players could not look at the whole map - off.
                rules.fog = false;
                rules.staticFog = false;
                //unit cap = sum of the cores' unitCapModifier (the Aurelia core gives 18); v7.0 set this to false,
                //leaving a fixed cap of 0, so no unit could ever be built
                rules.unitCapVariable = true;
                rules.unitCap = 0;
                //Logic.play() clears the core and refills it from rules.loadout (vanilla default: copper 100) on
                //planets without launch loadouts - Aurelia starts with its own stock only
                rules.loadout = ItemStack.list(lumenite, 400, aurite, 80);
                //v8.0: building on Aurelia was half the speed it felt like it should be - everything a unit
                //builds now goes up twice as fast. This is a planet rule, so no unit stat is touched and no
                //unit is weakened; deconstruct refund stays vanilla.
                rules.buildSpeedMultiplier = 2f;
            };
        }};

        aurelia.techTree = AureliaTech.build(aurelia);
        aurelia.techTree.addPlanet(aurelia);

        //v7.8: no branch is added to Serpulo or Erekir - every mod content is researched on Aurelia only.
    }

    /** Every terrain block, for audits and the offline sprite check. */
    public static Block[] terrain(){
        return new Block[]{auroraPlate, resonanceBed, auroraSand, prismShale, lumenMoss, auroraWater, auroraSandWater, deepAuroraWater,
            auroraWall, resonanceWall, prismShaleWall, auroraDuneWall, auroraBoulder, resonanceCrystal, oreLumenite, oreAurite, oreResonance};
    }

    /**
     * Planet isolation (call from Mod.init, after Block.postInit filled shownPlanets from the requirements):
     * Aurelia shows only Aurelia content; everything else is hidden there. Content with an empty shownPlanets set
     * counts as "shown everywhere" in vanilla, so it is pinned to Serpulo and Erekir.
     */
    public static void isolate(){
        ObjectSet<UnlockableContent> mine = new ObjectSet<>();
        mine.addAll(own);
        for(var list : new Seq[]{mindustry.Vars.content.blocks(), mindustry.Vars.content.items(), mindustry.Vars.content.liquids(),
            mindustry.Vars.content.units(), mindustry.Vars.content.statusEffects()}){
            for(Object o : list){
                UnlockableContent c = (UnlockableContent)o;
                if(mine.contains(c)){
                    c.shownPlanets.clear();
                    c.shownPlanets.add(aurelia);
                }else{
                    c.shownPlanets.remove(aurelia);
                    if(c.shownPlanets.isEmpty()) c.shownPlanets.addAll(Planets.serpulo, Planets.erekir);
                }
            }
        }
        arc.util.Log.info("[blackhole/aurelia] planet isolation: @ Aurelia content, all other content hidden on Aurelia.", own.size);
    }
}
