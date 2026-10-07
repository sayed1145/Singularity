package blackhole;

import arc.graphics.*;
import arc.struct.*;
import blackhole.g3d.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.units.UnitFactory.*;
import mindustry.world.meta.*;

import static blackhole.AureliaContent.*;
import static mindustry.type.ItemStack.with;

/**
 * v8.3 content pack: the waste cycle, the tier-3 core and the ward dome.
 *
 * <h2>The waste cycle</h2>
 * Industrial waste is a <b>by-product</b>, never a source: the silt kiln, the plasma mixer and the prism press
 * each drop one unit of waste per craft on top of their normal output. Waste by itself is worth nothing - it
 * only becomes material through a five-step chain, and every step loses mass, so the cycle can never out-run
 * the factories that feed it (no closed loop, no free matter):
 *
 * <pre>
 * waste ──[reclaimer]──> swarf + dust
 *   swarf ──[swarf furnace]──> aurite
 *   dust  ──[leach tower + water]──> lumen slurry (liquid)
 *      slurry ──[crystalliser]──> lumenite
 *      slurry + lumenite + aurite ──[resynthesiser]──> resonance shard
 *          shard + prism glass + slurry ──[prism reformer]──> prism alloy
 * </pre>
 *
 * So waste really does lead to everything - but through five machines, not one.
 *
 * <h2>Tier-3 core</h2>
 * The citadel (6x6) sits above the bastion. Its point is not "more of everything": <b>no core can mine every
 * ore</b>. The citadel pilot is the only one that can cut ore out of <i>walls</i>, and no core pilot at any
 * tier can mine resonance - that ore always needs a drill.
 *
 * <h2>Ward dome</h2>
 * See {@link CycleParts.WardDome}: a capped, cooling area shield with its own drawing.
 */
public final class AureliaCycle{
    public static Item industrialWaste, metalSwarf, slagDust;
    public static Liquid lumenSlurry;

    public static Block wasteReclaimer, swarfFurnace, leachTower, slurryCrystalliser, resynthesiser, prismReformer, wasteSilo;
    public static Block wardDome;
    public static Block citadelCore;

    public static Ship3D citadelPilot;

    /** everything this pack adds, in tree order */
    public static final Seq<UnlockableContent> all = new Seq<>();

    private static boolean loaded;

    private AureliaCycle(){}

    static <T extends UnlockableContent> T add(T c){
        all.add(c);
        AureliaContent.own.add(c);
        return c;
    }

    public static void load(){
        if(loaded) return;
        loaded = true;
        loadResources();
        loadUnits();
        loadBlocks();
        loadPlans();
        retrofit();
    }

    // =====================================================================================================
    // resources
    // =====================================================================================================

    private static void loadResources(){
        industrialWaste = add(new Item("industrial-waste", Color.valueOf("6b6a74")){{
            hardness = 0;
            cost = 0.35f;
            lowPriority = true;
            buildable = false;
            explosiveness = 0.05f;
        }});
        metalSwarf = add(new Item("metal-swarf", Color.valueOf("a9b0bd")){{
            hardness = 0;
            cost = 0.8f;
            buildable = false;
        }});
        slagDust = add(new Item("slag-dust", Color.valueOf("8f8498")){{
            hardness = 0;
            cost = 0.7f;
            lowPriority = true;
            buildable = false;
        }});

        lumenSlurry = add(new Liquid("lumen-slurry", Color.valueOf("7fd9e8")){{
            viscosity = 0.72f;
            heatCapacity = 0.38f;
            temperature = 0.52f;
            coolant = false;
        }});
    }

    // =====================================================================================================
    // the tier-3 core pilot
    // =====================================================================================================

    private static void loadUnits(){
        /*
         * Citadel pilot. The late-game landing chassis: it builds fast and far, and it is the only core unit
         * that can cut ore out of a wall seam. It still cannot mine resonance - nothing with a cockpit can.
         */
        citadelPilot = add(new Ship3D("citadel-pilot"){{
            builder = blackhole.models.CycleUnits::citadelPilot;
            lowAltitude = true;
            playerControllable = true;
            logicControllable = true;
            speed = 3.2f;
            accel = 0.10f;
            drag = 0.046f;
            health = 1500f;
            armor = 8f;
            hitSize = 22f;
            hover = 6f;
            //the citadel perk: wall seams. Resonance stays out of reach - hardness 4 is above this tier.
            mineTier = 3;
            mineWalls = true;
            mineFloor = true;
            mineSpeed = 12f;
            mineRange = 120f;
            buildSpeed = 2.2f;
            buildRange = 360f;
            itemCapacity = 140;
            range = 180f;
            engineColor = AureliaFx.lumen;
            weapons.add(new blackhole.g3d.LiftedWeapon(){{
                x = 0f; y = 7.4f; mirror = false; rotate = false; reload = 12f; muzzleZ = 0f;
                shootSound = BHSounds.pulse;
                bullet = bolt(7.2f, 32f, 28f, AureliaFx.lumen);
            }});
        }});
    }

    // =====================================================================================================
    // blocks
    // =====================================================================================================

    private static void loadBlocks(){
        // ---------------------------------------------------------------- the waste chain
        /*
         * 1. Reclaimer: breaks waste into the two things worth keeping - metal swarf and mineral dust.
         * Four waste in, two different items out: the chain narrows at every step.
         */
        wasteReclaimer = add(new GenericCrafter("waste-reclaimer"){{
            requirements(Category.crafting, with(lumenite, 110, aurite, 80, prismGlass, 45));
            size = 3;
            health = 620;
            craftTime = 70f;
            itemCapacity = 30;
            outputItems = with(metalSwarf, 1, slagDust, 1);
            consumeItem(industrialWaste, 4);
            consumePower(1.5f);
            craftEffect = AureliaFx.wasteVent;
        }});

        /*
         * 2a. Swarf furnace: swarf back into structural metal. Three swarf per ingot - recycling is never as
         * good as ore.
         */
        swarfFurnace = add(new GenericCrafter("swarf-furnace"){{
            requirements(Category.crafting, with(lumenite, 130, aurite, 110, prismGlass, 50));
            size = 2;
            health = 480;
            craftTime = 80f;
            itemCapacity = 24;
            outputItem = new ItemStack(aurite, 1);
            consumeItems(with(metalSwarf, 3));
            consumePower(1.8f);
            craftEffect = AureliaFx.wasteVent;
        }});

        /*
         * 2b. Leach tower: washes the dust with water and pours out lumen slurry - the liquid half of the
         * cycle, and the input of everything below it.
         */
        leachTower = add(new GenericCrafter("leach-tower"){{
            requirements(Category.crafting, with(lumenite, 160, aurite, 90, prismGlass, 70));
            size = 3;
            health = 700;
            craftTime = 60f;
            itemCapacity = 24;
            liquidCapacity = 60f;
            hasLiquids = true;
            outputLiquid = new LiquidStack(lumenSlurry, 9f / 60f);
            consumeItems(with(slagDust, 3));
            consumeLiquid(tidewater, 14f / 60f);
            consumePower(2.1f);
            craftEffect = AureliaFx.slurryDrip;
        }});

        /*
         * 3. Crystalliser: slurry back into lumenite. This is the step that makes the whole cycle worth
         * running - a steady, power-hungry trickle of the base ore without a single drill.
         */
        slurryCrystalliser = add(new GenericCrafter("slurry-crystalliser"){{
            requirements(Category.crafting, with(lumenite, 150, aurite, 120, prismGlass, 60, resonanceShard, 25));
            size = 2;
            health = 520;
            craftTime = 75f;
            itemCapacity = 20;
            liquidCapacity = 60f;
            hasLiquids = true;
            outputItem = new ItemStack(lumenite, 2);
            consumeLiquid(lumenSlurry, 12f / 60f);
            consumePower(2.4f);
            craftEffect = AureliaFx.slurryDrip;
        }});

        /*
         * 4. Resynthesiser: the deep step. Slurry plus the two base metals re-grow a resonance shard - slow,
         * expensive, and the only way to get shards without a tier-4 drill on a resonance field.
         */
        resynthesiser = add(new GenericCrafter("resonance-resynthesiser"){{
            requirements(Category.crafting, with(lumenite, 320, aurite, 260, prismGlass, 140, prismAlloy, 70));
            size = 3;
            health = 940;
            craftTime = 150f;
            itemCapacity = 30;
            liquidCapacity = 80f;
            hasLiquids = true;
            outputItem = new ItemStack(resonanceShard, 1);
            consumeItems(with(lumenite, 4, aurite, 3));
            consumeLiquid(lumenSlurry, 16f / 60f);
            consumePower(4.6f);
            craftEffect = AureliaFx.prismCraft;
        }});

        /*
         * 5. Prism reformer: the top of the cycle. Shards, glass and slurry become prism alloy, so a base can
         * close its own material loop out of nothing but its own rubbish.
         */
        prismReformer = add(new GenericCrafter("prism-reformer"){{
            requirements(Category.crafting, with(lumenite, 420, aurite, 300, prismGlass, 200, prismAlloy, 120, resonanceShard, 80));
            size = 3;
            health = 1100;
            craftTime = 120f;
            itemCapacity = 30;
            liquidCapacity = 80f;
            hasLiquids = true;
            outputItem = new ItemStack(prismAlloy, 2);
            consumeItems(with(resonanceShard, 2, prismGlass, 6));
            consumeLiquid(lumenSlurry, 10f / 60f);
            consumePower(5.4f);
            craftEffect = AureliaFx.prismCraft;
        }});

        /*
         * The buffer the cycle needs: waste arrives in bursts from every factory floor, the chain eats it at a
         * constant rate. One item type, a lot of it.
         */
        wasteSilo = add(new CycleParts.WasteSilo("waste-silo"){{
            requirements(Category.effect, with(lumenite, 90, aurite, 60));
            size = 3;
            health = 760;
            itemCapacity = 900;
        }});

        // ---------------------------------------------------------------- the ward dome
        wardDome = add(new CycleParts.WardDome("ward-dome"){{
            requirements(Category.effect, with(lumenite, 540, aurite, 380, prismAlloy, 180, resonanceShard, 120));
            size = 3;
            health = 1500;
            armor = 6f;
            radius = 26f * 8f;
            shieldCapacity = 7000f;
            regen = 120f;
            breakCooldown = 60f * 14f;
            consumePower(6.5f);
            //the two upgrades: coolant widens the dome and speeds the rebuild, shards deepen the bank
            boostLiquid = AureliaFrontier.auroraCoolant;
            boostLiquidAmount = 9f / 60f;
            liquidRadiusBoost = 8f * 8f;
            liquidRegenBoost = 1.9f;
            boostItem = resonanceShard;
            itemCapacityBoost = 5200f;
            itemBoostDuration = 60f * 22f;
        }});

        // ---------------------------------------------------------------- the tier-3 core
        /*
         * Citadel: 6x6, so it is placed straight over the 5x5 bastion. It is the last core - more storage,
         * more unit cap, a pilot that works wall seams, and enough hull to be worth the alloy.
         */
        citadelCore = add(new AureliaCoreBlock("aurelia-citadel"){{
            requirements(Category.effect, with(lumenite, 9000, aurite, 7000, prismAlloy, 3000, concordCore, 1200, resonanceShard, 2400));
            size = 6;
            health = 28000;
            armor = 18f;
            itemCapacity = 26000;
            unitCapModifier = 54;
            unitType = citadelPilot;
            envEnabled = Env.any;
            isFirstTier = false;
            alwaysUnlocked = false;
        }});
    }

    private static void loadPlans(){
        //the citadel pilot is printable like every other core chassis
        if(aureliaFabricator instanceof mindustry.world.blocks.units.UnitFactory f){
            f.plans.add(new UnitPlan(citadelPilot, 60f * 70f,
                with(lumenite, 420, aurite, 320, prismAlloy, 140, resonanceShard, 90)));
        }
    }

    // =====================================================================================================
    // changes to content that already existed
    // =====================================================================================================

    /**
     * v8.3 retrofit, in one place so it is easy to audit:
     *
     * <ul>
     * <li>three existing crafters now drop industrial waste next to their normal output - this is where the
     *     cycle gets its feedstock, and it is the only source of waste in the game;</li>
     * <li>(the ground well's own 2.5x / all-floor change lives in {@code AureliaForge});</li>
     * <li>no core pilot can mine every ore any more (see the class comment).</li>
     * </ul>
     */
    private static void retrofit(){
        //---- waste by-products
        wasteOut(siltKiln, prismGlass, 1);
        wasteOut(plasmaMixer, prismAlloy, 1);
        wasteOut(prismPress, prismGlass, 2);

        //---- mining: every core keeps a job, none of them keeps all of them
        if(lumenPilot != null){
            lumenPilot.mineTier = 3;
            lumenPilot.mineWalls = false;
        }
        if(AureliaBastion.bastionPilot != null){
            //was tier 5 (it mined literally everything). Same ores as the shard pilot now, but faster.
            AureliaBastion.bastionPilot.mineTier = 3;
            AureliaBastion.bastionPilot.mineWalls = false;
            AureliaBastion.bastionPilot.mineSpeed = 11f;
        }
    }

    /** gives a crafter a second output: its normal product plus one industrial waste */
    private static void wasteOut(Block block, Item main, int amount){
        if(block instanceof GenericCrafter c){
            c.outputItem = null;
            c.outputItems = with(main, amount, industrialWaste, 1);
            if(c.itemCapacity < 20) c.itemCapacity = 20;
        }
    }
}
