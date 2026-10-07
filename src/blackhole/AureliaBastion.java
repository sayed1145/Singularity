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
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.blocks.units.UnitFactory.*;
import mindustry.world.meta.*;

import static blackhole.AureliaContent.*;
import static mindustry.type.ItemStack.with;

/**
 * v7.7 bastion pack.
 *
 * <p>What this version adds to Aurelia: the <b>graphite brick</b> - the first product the old graphite kiln has
 * ever had a reason to make - a full line of <b>ten defence walls</b> that differ in material and in mechanism,
 * a single new power plant (the <b>graviton string dynamo</b>, thousands of power per second), and a
 * <b>tier-2 core</b> that is built straight on top of the standard core, with its own core unit and escort.
 *
 * <p>Balance rule, unchanged since v7.3: nothing that already existed gets a stat change unless the change was
 * asked for. Every number here is set against its Aurelia neighbour - the aurite wall (480 hp, 3 armour), the
 * plasma dynamo (7.2 power/tick) and the standard core (6800 hp, 8000 items).
 */
public final class AureliaBastion{
    public static Item graphiteBrick;

    public static Block sinterWall, graphiteBrickWall, prismMirrorWall, stasisWall, echoWall, frostWall,
        anchorWall, amplifierWall, absorberWall, concordShieldWall, gravitonStringDynamo, bastionCore;
    public static Ship3D bastionPilot, bastionWarden;

    /** Everything this pack adds, in tech order. */
    public static final Seq<UnlockableContent> added = new Seq<>();

    private AureliaBastion(){}

    static <T extends UnlockableContent> T add(T c){
        added.add(c);
        own.add(c);
        return c;
    }

    public static void load(){
        loadItems();
        loadUnits();
        loadBlocks();
    }

    // =================================================================================================
    // the material
    // =================================================================================================

    private static void loadItems(){
        //fired silt bound with lumenite: cheap, heavy, and the reason the graphite kiln is now worth building
        graphiteBrick = add(new Item("graphite-brick", Color.valueOf("4a4f58")){{
            cost = 0.9f;
            hardness = 2;
            healthScaling = 0.14f;
            flammability = 0f;
            explosiveness = 0f;
        }});

    }

    // =================================================================================================
    // the units of the tier-2 core
    // =================================================================================================

    private static void loadUnits(){
        //core unit of the bastion core: everything the lumen pilot does, one tier up
        bastionPilot = add(new Ship3D("bastion-pilot"){{
            builder = BastionUnits::bastionPilot;
            lowAltitude = true;
            playerControllable = true;
            logicControllable = true;
            speed = 3.4f;
            accel = 0.10f;
            drag = 0.044f;
            health = 980f;
            armor = 6f;
            hitSize = 19f;
            hover = 6f;
            mineTier = 5;
            mineSpeed = 9.5f;
            mineRange = 110f;
            buildSpeed = 1.6f;
            buildRange = 310f;
            range = 165f;
            engineColor = AureliaFx.lumen;
            weapons.add(new LiftedWeapon(){{
                x = 0f; y = 7f; mirror = false; rotate = false; reload = 13f; muzzleZ = 0f;
                shootSound = BHSounds.pulse;
                bullet = bolt(7f, 30f, 28f, AureliaFx.lumen);
            }});
        }});

        //escort: slower, tougher, two mounts - the thing you build once the bastion core stands
        bastionWarden = add(new Ship3D("bastion-warden"){{
            builder = BastionUnits::bastionWarden;
            lowAltitude = true;
            speed = 1.7f;
            accel = 0.06f;
            drag = 0.05f;
            health = 3400f;
            armor = 12f;
            hitSize = 29f;
            hover = 7f;
            range = 215f;
            targetAir = true;
            targetGround = true;
            engineColor = AureliaFx.lumen;
            weapons.add(new LiftedWeapon(){{
                x = 0f; y = 3.4f; mirror = false; rotate = true; rotateSpeed = 3.4f;
                reload = 34f; muzzleZ = 3.3f; shootSound = BHSounds.thump;
                bullet = new BasicBulletType(5.2f, 86f){{
                    lifetime = 42f;
                    width = 10f;
                    height = 17f;
                    splashDamage = 42f;
                    splashDamageRadius = 26f;
                    frontColor = Color.white;
                    backColor = trailColor = hitColor = AureliaFx.concord;
                    hitEffect = despawnEffect = AureliaFx.lumenHit;
                    shootEffect = OwnFx.muzzle;
                    smokeEffect = OwnFx.muzzleSmoke;
                }};
            }});
        }});
    }

    // =================================================================================================
    // blocks
    // =================================================================================================

    private static void loadBlocks(){
        // ------------------------------------------------------------------ the ten walls
        // 1. sinter wall - the cheap one: fired silt, insulated against arcs and heat
        sinterWall = add(new BastionParts.ModelWall("sinter-wall"){{
            requirements(Category.defense, with(graphiteBrick, 3, lumenite, 3));
            health = 100 * 4;
            armor = 1f;
            insulated = true;
            absorbLasers = false;
        }});
        // 2. graphite brick wall - solid masonry that eats beams
        graphiteBrickWall = add(new BastionParts.ModelWall("graphite-brick-wall"){{
            requirements(Category.defense, with(graphiteBrick, 8, aurite, 4));
            size = 2;
            health = 150 * 4 * 4;
            armor = 5f;
            absorbLasers = true;
            insulated = true;
        }});
        // 3. prism mirror wall - a faceted mirror that throws shots back
        prismMirrorWall = add(new BastionParts.ModelWall("prism-mirror-wall"){{
            requirements(Category.defense, with(prismGlass, 10, aurite, 6, graphiteBrick, 4));
            health = 115 * 4;
            armor = 2f;
            chanceDeflect = 11f;
            flashHit = true;
        }});
        // 4. stasis wall - anything that touches it loses its footing
        stasisWall = add(new BastionParts.StasisWall("stasis-wall"){{
            requirements(Category.defense, with(resonanceShard, 14, lumenite, 20, graphiteBrick, 10));
            size = 2;
            health = 125 * 4 * 4;
            armor = 4f;
            //the Aegis Keeper's containment lattice, reused: one grip status for the whole planet
            grip = latticeGrip;
            gripRadius = 30f;
            gripDuration = 110f;
        }});
        // 5. echo wall - banks the damage it takes and gives it back as lightning
        echoWall = add(new BastionParts.EchoWall("echo-wall"){{
            requirements(Category.defense, with(prismAlloy, 16, resonanceShard, 14, graphiteBrick, 12));
            size = 2;
            health = 135 * 4 * 4;
            armor = 5f;
            bankCapacity = 1100f;
            releaseRadius = 60f;
            releaseScale = 0.9f;
        }});
        // 6. frost wall - drinks coolant and repairs itself
        frostWall = add(new BastionParts.FrostWall("frost-wall"){{
            requirements(Category.defense, with(aurite, 22, prismGlass, 14, graphiteBrick, 10));
            size = 2;
            health = 140 * 4 * 4;
            armor = 5f;
            coolant = AureliaFrontier.auroraCoolant;
            repairFraction = 0.013f;
            coolantUse = 0.05f;
        }});
        // 7. anchor wall - drags ground attackers onto itself
        anchorWall = add(new BastionParts.AnchorWall("anchor-wall"){{
            requirements(Category.defense, with(graphiteBrick, 18, prismAlloy, 10, aurite, 20));
            size = 2;
            health = 170 * 4 * 4;
            armor = 7f;
            pullRadius = 76f;
            pull = 0.6f;
        }});
        // 8. amplifier wall - masonry that makes the turrets behind it shoot faster
        amplifierWall = add(new BastionParts.AmplifierWall("amplifier-wall"){{
            requirements(Category.defense, with(resonanceShard, 18, prismGlass, 16, graphiteBrick, 12));
            size = 2;
            health = 120 * 4 * 4;
            armor = 4f;
            boostRadius = 64f;
            reloadBonus = 0.3f;
        }});
        // 9. absorber wall - swallows projectiles and pays the grid back
        absorberWall = add(new BastionParts.AbsorberWall("absorber-wall"){{
            requirements(Category.defense, with(concordCore, 10, prismAlloy, 18, graphiteBrick, 14));
            size = 2;
            health = 130 * 4 * 4;
            armor = 5f;
            absorbRadius = 36f;
            powerPerDamage = 0.22f;
            maxOutput = 4.5f;
        }});
        // 10. concord shield wall - a powered shield in front of the line
        concordShieldWall = add(new BastionParts.ModelShieldWall("concord-shield-wall"){{
            requirements(Category.defense, with(concordCore, 14, prismAlloy, 24, graphiteBrick, 16));
            size = 2;
            health = 150 * 4 * 4;
            armor = 6f;
            shieldHealth = 1800f;
            breakCooldown = 60f * 9f;
            regenSpeed = 1.4f;
            glowColor = AureliaFx.concord;
            consumePower(1.1f);
        }});

        // ------------------------------------------------------------------ the new power plant
        /*
         * Graviton string dynamo: the only power block v7.7 adds. A mass is held on a graviton string between
         * two counter-rotating gimbals; the string is kept wound by graviton slurry and the brick moderator
         * stops it from unwinding. 58 power/tick = 3480 power/second, i.e. eight plasma dynamos in one 5x5,
         * and it needs the whole gravity line (churn -> slurry) plus a brick feed to run.
         */
        gravitonStringDynamo = add(new ConsumeGenerator("graviton-string-dynamo"){{
            requirements(Category.power, with(prismAlloy, 420, concordCore, 180, aurite, 560, resonanceShard, 260, graphiteBrick, 300));
            size = 5;
            health = 3600;
            armor = 8f;
            powerProduction = 58f;
            itemDuration = 60f * 4f;
            itemCapacity = 20;
            hasLiquids = true;
            liquidCapacity = 120f;
            consumeItem(graphiteBrick);
            consumeLiquid(AureliaFrontier.gravitonSlurry, 0.3f / 60f);
            generateEffect = OwnFx.burn;
            effectChance = 0.05f;
            explosionRadius = 20;
            explosionDamage = 2600;
            ambientSound = Sounds.loopTech;
            ambientSoundVolume = 0.12f;
            drawer = new DrawModel(Models.get("graviton-string-dynamo"));
        }});

        // ------------------------------------------------------------------ the tier-2 core
        /*
         * Bastion core: 5x5, so vanilla's core upgrade rule applies - it is placed directly over the 4x4
         * aurelia core and takes its contents over. Twice the storage, twice the unit cap, and a core unit
         * that mines the top ore tier.
         */
        bastionCore = add(new AureliaCoreBlock("aurelia-bastion"){{
            requirements(Category.effect, with(lumenite, 5500, aurite, 4000, prismAlloy, 1400, concordCore, 600, graphiteBrick, 1200));
            size = 5;
            health = 15000;
            armor = 12f;
            itemCapacity = 16000;
            unitCapModifier = 36;
            unitType = bastionPilot;
            envEnabled = Env.any;
            isFirstTier = false;
        }});

        //the escort is built in the fabricator that is already on the planet - no new factory for one unit
        if(aureliaFabricator instanceof UnitFactory f){
            f.plans.add(new UnitPlan(bastionWarden, 60f * 160f, with(prismAlloy, 240, concordCore, 90, graphiteBrick, 160, lumenite, 300)));
        }
    }
}
