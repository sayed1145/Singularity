package blackhole;

import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.units.*;
import mindustry.world.blocks.units.UnitFactory.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import rbmk.content.RbmkBlocks;
import sixfold.*;

import static blackhole.AureliaContent.*;

/**
 * v7.3 full merge: the three mods become one Aurelia game.
 *
 * <p>Up to v7.2 the Singularity spine lived on Serpulo, the Aegis Titan matrix on Serpulo and Erekir, and
 * Aurelia was isolated to its own content. This class merges all of it:
 *
 * <ul>
 * <li>{@link #recost()} rewrites every vanilla item out of every mod recipe - build requirements, unit factory
 *     plans and item consumption - into Aurelia resources through one deterministic tier map. It runs at the end
 *     of {@code loadContent()}, i.e. before {@code ContentLoader.init()} turns requirements into build costs.</li>
 * <li>{@link #include()} makes every mod content visible on Aurelia (vanilla content stays hidden there).</li>
 * <li>{@link #retree()} converts the research cost of every mod node with the same map and then hangs every
 *     researchable mod content that is not yet in the Aurelia tree into it - a hand written spine for the
 *     Singularity / Aegis chain plus a category sweep that guarantees nothing is left out.</li>
 * </ul>
 */
public final class AureliaMerge{
    /** vanilla item -> Aurelia item, with the amount factor that keeps the price sane across tiers */
    private static final ObjectMap<Item, Object[]> rules = new ObjectMap<>();
    private static final ObjectMap<UnlockableContent, TechNode> aureliaNodes = new ObjectMap<>();
    public static int recosted, added, visible;

    private AureliaMerge(){}

    private static void rule(Item from, Item to, float scale){
        rules.put(from, new Object[]{to, scale});
    }

    private static void rules(){
        if(rules.size > 0) return;
        //tier 1 bulk
        rule(Items.copper, lumenite, 1f);
        rule(Items.lead, lumenite, 0.85f);
        rule(Items.scrap, lumenite, 0.8f);
        rule(Items.sporePod, lumenite, 0.8f);
        rule(Items.beryllium, lumenite, 0.9f);
        //tier 1 processed
        rule(Items.sand, silt, 1f);
        rule(Items.coal, silt, 0.8f);
        rule(Items.pyratite, silt, 0.7f);
        //tier 2 metal
        rule(Items.titanium, aurite, 0.9f);
        rule(Items.tungsten, aurite, 0.85f);
        //tier 2 glass / conductor
        rule(Items.graphite, prismGlass, 0.85f);
        rule(Items.metaglass, prismGlass, 1f);
        rule(Items.oxide, prismGlass, 0.7f);
        rule(Items.silicon, resonanceShard, 0.7f);
        //tier 3 alloy
        rule(Items.plastanium, prismAlloy, 0.6f);
        rule(Items.thorium, prismAlloy, 0.5f);
        rule(Items.carbide, prismAlloy, 0.6f);
        rule(Items.blastCompound, prismAlloy, 0.5f);
        //tier 4 exotic
        rule(Items.surgeAlloy, concordCore, 0.35f);
        rule(Items.phaseFabric, concordCore, 0.3f);
        rule(Items.fissileMatter, concordCore, 0.3f);
        rule(Items.dormantCyst, concordCore, 0.3f);
    }

    /** Mod content: everything this jar registers carries the mod prefix. */
    public static boolean mod(UnlockableContent c){
        return c != null && c.name != null && c.name.startsWith("blackhole-");
    }

    private static boolean vanilla(ItemStack[] stacks){
        if(stacks == null) return false;
        for(ItemStack s : stacks) if(rules.containsKey(s.item)) return true;
        return false;
    }

    /** Converts one cost list; mod items (Aurelia resources, degenerate matter, aegis alloy, ...) pass through. */
    public static ItemStack[] convert(ItemStack[] in){
        if(in == null || in.length == 0 || !vanilla(in)) return in;
        OrderedMap<Item, Integer> acc = new OrderedMap<>();
        for(ItemStack s : in){
            Object[] r = rules.get(s.item);
            Item item = r == null ? s.item : (Item)r[0];
            int amount = r == null ? s.amount : Math.max(1, Math.round(s.amount * (Float)r[1]));
            acc.put(item, acc.get(item, 0) + amount);
        }
        Seq<Item> keys = acc.orderedKeys().copy();
        keys.sort(i -> i.cost);
        ItemStack[] out = new ItemStack[keys.size];
        for(int i = 0; i < keys.size; i++) out[i] = new ItemStack(keys.get(i), acc.get(keys.get(i)));
        return out;
    }

    /**
     * Re-prices every mod block, unit plan and item recipe in Aurelia resources. Must run before
     * {@code ContentLoader.init()}, which freezes requirements into build costs and consumer arrays.
     */
    public static void recost(){
        rules();
        recosted = 0;
        for(Block b : Vars.content.blocks()){
            if(!mod(b)) continue;
            //HJSON content is parsed after every Java mod's loadContent(), so this runs twice: the second pass
            //(from init()) catches the HJSON turrets. convert() is a no-op once a cost holds no vanilla item.
            float before = costOf(b.requirements);
            ItemStack[] req = convert(b.requirements);
            if(req != b.requirements){
                b.requirements = req;
                //buildTime is derived from the requirements inside Block.init(); if that already ran, scale it
                //by the cost ratio - that is exact for any formula linear in the summed item cost.
                float after = costOf(req);
                if(b.buildTime > 0f && before > 0f) b.buildTime *= after / before;
                recosted++;
            }
            if(b instanceof UnitFactory uf && uf.plans != null){
                for(UnitPlan p : uf.plans) p.requirements = convert(p.requirements);
            }
            //item recipes, both before init() (consumeBuilder) and after it (consumers)
            patch(consumers(b));
            if(b.consumers != null) patchArray(b.consumers);
            if(b.nonOptionalConsumers != null) patchArray(b.nonOptionalConsumers);
            if(b.updateConsumers != null) patchArray(b.updateConsumers);
            if(b.optionalConsumers != null) patchArray(b.optionalConsumers);
            //ammo: an item turret on Aurelia must eat Aurelia items
            if(b instanceof ItemTurret t && t.ammoTypes != null && t.ammoTypes.size > 0) t.ammoTypes = convertAmmo(t.ammoTypes);
        }
        Log.info("[blackhole/merge] re-priced @ mod blocks in Aurelia resources (@ conversion rules)", recosted, rules.size);
    }

    private static float costOf(ItemStack[] stacks){
        float c = 0f;
        if(stacks != null) for(ItemStack s : stacks) c += s.amount * s.item.cost;
        return c;
    }

    /** Swaps every ConsumeItems in a live list (ConsumeItems.items is final). */
    private static void patch(Seq<Consume> cons){
        for(int i = 0; i < cons.size; i++){
            Consume next = patched(cons.get(i));
            if(next != null) cons.set(i, next);
        }
    }

    private static void patchArray(Consume[] cons){
        for(int i = 0; i < cons.length; i++){
            Consume next = patched(cons[i]);
            if(next != null) cons[i] = next;
        }
    }

    private static Consume patched(Consume c){
        if(c == null || c.getClass() != ConsumeItems.class) return null;
        ConsumeItems ci = (ConsumeItems)c;
        ItemStack[] conv = convert(ci.items);
        if(conv == ci.items) return null;
        ConsumeItems next = new ConsumeItems(conv);
        next.optional = ci.optional;
        next.booster = ci.booster;
        next.multiplier = ci.multiplier;
        return next;
    }

    /** Remaps ammo item keys; if two vanilla items land on the same Aurelia item the stronger bullet wins. */
    private static ObjectMap<Item, mindustry.entities.bullet.BulletType> convertAmmo(ObjectMap<Item, mindustry.entities.bullet.BulletType> in){
        ObjectMap<Item, mindustry.entities.bullet.BulletType> out = new ObjectMap<>();
        for(var e : in){
            Object[] r = rules.get(e.key);
            Item item = r == null ? e.key : (Item)r[0];
            var prev = out.get(item);
            if(prev == null || e.value.estimateDPS() > prev.estimateDPS()) out.put(item, e.value);
        }
        return out;
    }

    /** {@code Block.consumeBuilder} is protected; before init() it is the only place consumers live. */
    @SuppressWarnings("unchecked")
    private static Seq<Consume> consumers(Block b){
        try{
            java.lang.reflect.Field f = Block.class.getDeclaredField("consumeBuilder");
            f.setAccessible(true);
            Seq<Consume> s = (Seq<Consume>)f.get(b);
            return s == null ? new Seq<>() : s;
        }catch(Throwable t){
            return new Seq<>();
        }
    }

    /**
     * Vanilla sandbox utilities that are explicitly wanted on Aurelia (unchanged, sandbox only):
     * item source / void, liquid source / void, power source / void and the heat source.
     */
    public static Seq<Block> sandboxBlocks(){
        Seq<Block> out = new Seq<>();
        for(Block b : new Block[]{Blocks.itemSource, Blocks.itemVoid, Blocks.liquidSource, Blocks.liquidVoid,
            Blocks.powerSource, Blocks.powerVoid, Blocks.heatSource,
            //v7.6: the payload source and payload void join them, unchanged and sandbox only
            Blocks.payloadSource, Blocks.payloadVoid}){
            if(b != null) out.add(b);
        }
        return out;
    }

    /**
     * v7.5 fix - the Core Database tab a content appears under is {@code databaseTabs}, NOT {@code shownPlanets}.
     * {@code UnlockableContent.postInit()} copies shownPlanets into databaseTabs once, long before this merge runs,
     * so everything we move onto Aurelia afterwards was still filed under its old tab (or under no tab at all,
     * which is why the merged mods were invisible in the database). Re-sync the two sets here.
     */
    static void tabs(UnlockableContent c){
        c.databaseTabs.clear();
        c.databaseTabs.addAll(c.shownPlanets);
    }

    /** Mod content is shown on Aurelia as well as on its original planets; vanilla content stays hidden there. */
    public static void include(){
        visible = 0;
        for(Seq<?> list : new Seq[]{Vars.content.blocks(), Vars.content.items(), Vars.content.liquids(),
            Vars.content.units(), Vars.content.statusEffects()}){
            for(Object o : list){
                UnlockableContent c = (UnlockableContent)o;
                if(mod(c)){
                    //v7.8: every mod content lives on Aurelia and nowhere else
                    c.shownPlanets.clear();
                    c.shownPlanets.add(aurelia);
                    tabs(c);
                    visible++;
                }else{
                    c.shownPlanets.remove(aurelia);
                    if(c.shownPlanets.isEmpty()) c.shownPlanets.addAll(Planets.serpulo, Planets.erekir);
                    c.databaseTabs.remove(aurelia);
                    if(c.databaseTabs.isEmpty()) c.databaseTabs.addAll(c.shownPlanets);
                }
            }
        }
        //the sandbox sources / voids are vanilla, but they are wanted on Aurelia as they are
        int sandbox = 0;
        for(Block b : sandboxBlocks()){
            b.shownPlanets.add(aurelia);
            b.databaseTabs.add(aurelia);
            //Aurelia is terrestrial|groundWater|oxygen; the Erekir-only heat source would be filtered out otherwise
            b.envEnabled |= aurelia.defaultEnv;
            b.envRequired = 0;
            sandbox++;
        }
        Log.info("[blackhole/merge] @ mod contents available on Aurelia (+@ vanilla sandbox sources/voids), all other vanilla content hidden there", visible, sandbox);
    }

    /** Research costs in Aurelia resources + every mod content hung into the Aurelia tree. */
    public static void retree(){
        rules();
        //1. research costs follow the same conversion
        int costs = 0;
        for(TechNode root : TechTree.roots){
            for(TechNode n : flatten(root)){
                if(mod(n.content) && vanilla(n.requirements)){
                    n.setupRequirements(convert(n.requirements));
                    costs++;
                }
            }
        }

        //2. index what Aurelia already has
        aureliaNodes.clear();
        for(TechNode n : flatten(AureliaTech.root)) aureliaNodes.put(n.content, n);

        //3. the Singularity / Aegis spine, hung off the top of the Aurelia crafting line
        added = 0;
        TechNode top = aureliaNodes.get(prismResonator, aureliaNodes.get(harmonicRelay, AureliaTech.root));
        TechNode press = attach(top, BHBlocks.gravitonPress);
        TechNode degen = attach(press, BHItems.degenerateMatter);
        attach(degen, BHBlocks.accretionFoundry);
        TechNode cond = attach(degen, BHBlocks.hawkingCondenser);
        TechNode dust = attach(cond, BHItems.hawkingDust);
        TechNode forge = attach(dust, BHBlocks.singularityForge);
        TechNode core = attach(forge, BHItems.singularityCore);

        TechNode dragger = attach(degen, BHBlocks.frameDragger);
        TechNode cannon = attach(dragger, BHBlocks.accretionCannon);
        TechNode emitter = attach(cannon, BHBlocks.hawkingEmitter);
        TechNode lance = attach(emitter, BHBlocks.quasarLance);
        TechNode horizon = attach(lance, BHBlocks.eventHorizon);

        TechNode assembler = attach(core, BHBlocks.horizonAssembler);
        TechNode lensing = attach(assembler, BHUnits.lensing);
        attach(lensing, BHUnits.ergosphere);
        TechNode yard = attach(core, BHBlocks.citadelYard);
        TechNode accretor = attach(yard, BHUnits.accretor);
        attach(accretor, BHUnits.citadel);

        //Aegis Titan matrix: both planet variants land on Aurelia too
        TechNode alloy = attach(dust, SixfoldMod.aegisAlloy);
        TechNode pressS = attach(alloy, SixfoldMod.forgeSerpulo);
        TechNode signal = attach(alloy, SixfoldMod.signalCore);
        TechNode encS = attach(signal, SixfoldMod.encoderSerpulo);
        TechNode bayS = attach(signal, SixfoldMod.baySerpulo);
        TechNode titan = attach(bayS, SixfoldMod.titan);
        attach(titan, SixfoldMod.pyroclast);

        //3.5 v7.6 frontier pack: placed by hand so the new line reads as a line instead of being swept in by
        //category. Everything hangs off the Aurelia node it actually extends.
        TechNode pumpNode = aureliaNodes.get(tidePump, AureliaTech.root);
        TechNode condenser = attach(pumpNode, AureliaFrontier.coolantCondenser);
        attach(condenser, AureliaFrontier.auroraCoolant);
        TechNode resonator = aureliaNodes.get(prismResonator, condenser);
        TechNode churn = attach(resonator, AureliaFrontier.gravitonChurn);
        attach(churn, AureliaFrontier.gravitonSlurry);

        TechNode convNode = aureliaNodes.get(lumenConveyor, AureliaTech.root);
        attach(convNode, AureliaFrontier.fluxConveyor);
        attach(aureliaNodes.get(lumenBridge, convNode), AureliaFrontier.skylineCableway);
        attach(aureliaNodes.get(lumenConduit, condenser), AureliaFrontier.fluxConduit);

        TechNode cradle = attach(aureliaNodes.get(lumenExtractor, AureliaTech.root), AureliaFrontier.resonanceCradle);
        attach(cradle, AureliaFrontier.mote);
        attach(cradle, AureliaFrontier.latticeCrawler);
        attach(aureliaNodes.get(tideDock, cradle), AureliaFrontier.tideLancer);

        attach(aureliaNodes.get(shardVolley, AureliaTech.root), AureliaFrontier.tempestCoil);
        TechNode well = attach(aureliaNodes.get(arcMortar, aureliaNodes.get(prismPylon, AureliaTech.root)), AureliaFrontier.gravityWell);
        attach(well, AureliaFrontier.aegisLattice);
        attach(aureliaNodes.get(lumenProcessor, AureliaTech.root), AureliaFrontier.latticeLink);

        //3.6 v7.7 bastion pack: the brick first (it gates the whole line), then the walls off the wall node,
        //then the dynamo off the gravity line, then the tier-2 core and its units off the core node.
        TechNode kilnNode = aureliaNodes.get(RbmkBlocks.graphiteKiln, press);
        TechNode brick = attach(kilnNode, AureliaBastion.graphiteBrick);
        TechNode sinter = attach(brick, AureliaBastion.sinterWall);
        TechNode brickWall = attach(sinter, AureliaBastion.graphiteBrickWall);
        attach(brickWall, AureliaBastion.prismMirrorWall);
        TechNode stasis = attach(brickWall, AureliaBastion.stasisWall);
        attach(stasis, AureliaBastion.echoWall);
        attach(stasis, AureliaBastion.frostWall);
        TechNode anchorW = attach(brickWall, AureliaBastion.anchorWall);
        attach(anchorW, AureliaBastion.amplifierWall);
        TechNode absorber = attach(anchorW, AureliaBastion.absorberWall);
        attach(absorber, AureliaBastion.concordShieldWall);

        attach(churn, AureliaBastion.gravitonStringDynamo);

        TechNode bastion = attach(aureliaNodes.get(AureliaContent.aureliaCore, AureliaTech.root), AureliaBastion.bastionCore);
        attach(bastion, AureliaBastion.bastionPilot);
        attach(bastion, AureliaBastion.bastionWarden);

        //3.7 v7.8 nova pack: the zero-config transport family off the conveyor node, the large node off the
        //power line, the resonance hub off the core, and the five new units off the fabricator.
        attach(aureliaNodes.get(AureliaContent.lumenNode, AureliaTech.root), AureliaNova.auroraNode);

        TechNode fabNode = aureliaNodes.get(AureliaContent.aureliaFabricator, AureliaTech.root);
        TechNode hub = attach(aureliaNodes.get(AureliaContent.lumenMender, fabNode), AureliaNova.resonanceHub);
        TechNode scout = attach(fabNode, AureliaNova.prismScout);
        attach(scout, AureliaNova.lumenCourier);
        TechNode sentry = attach(scout, AureliaNova.auroraSentry);
        TechNode lancer = attach(sentry, AureliaNova.auriteLancer);
        attach(lancer, AureliaNova.resonanceSiege);

        //3.8 v7.9 terra pack: the borer hangs off the courier (both are engineering units), the harrier off the sentry,
        //the guard off the lancer and the caller off the siege - each new unit next to the one it resembles.
        attach(aureliaNodes.get(AureliaNova.lumenCourier, fabNode), AureliaTerra.terraBorer);
        attach(sentry, AureliaTerra.glassHarrier);
        attach(lancer, AureliaTerra.basaltGuard);
        attach(fabNode, AureliaTerra.ventCaller);

        //3.9 v8.0 forge pack: the logistics / support tier. Each new block hangs off the block it extends.
        attach(aureliaNodes.get(lumenExtractor, press), AureliaForge.wallBore);
        TechNode station = attach(aureliaNodes.get(AureliaContent.lumenBridge, AureliaTech.root), AureliaForge.cargoStation);
        attach(station, AureliaForge.cargoPoint);
        attach(aureliaNodes.get(lumenMender, AureliaTech.root), AureliaForge.mendDome);
        attach(aureliaNodes.get(lumenCell, AureliaTech.root), AureliaForge.accumulator);
        attach(aureliaNodes.get(AureliaContent.lumenConduit, AureliaTech.root), AureliaForge.groundWell);
        attach(aureliaNodes.get(AureliaNova.lumenCourier, fabNode), AureliaForge.lumenMedic);
        attach(aureliaNodes.get(AureliaTerra.terraBorer, fabNode), AureliaForge.auriteWright);

        //3.10 v8.1 logistics pack: the network nodes hang off the hub that drives them, the liquid tier off the
        //conduit line, the gates off the router, and the three new units off the units they stand next to.
        TechNode netHub = aureliaNodes.get(AureliaForge.cargoStation, AureliaTech.root);
        TechNode supply = attach(netHub, AureliaLogix.supplyPost);
        attach(supply, AureliaLogix.bufferPost);
        attach(netHub, AureliaLogix.logiVault);
        TechNode conduitNode = aureliaNodes.get(AureliaContent.lumenConduit, AureliaTech.root);
        TechNode tankNode = attach(conduitNode, AureliaLogix.liquidTank);
        attach(tankNode, AureliaLogix.reservoir);
        attach(tankNode, AureliaLogix.liquidRelay);
        TechNode gate = attach(aureliaNodes.get(AureliaContent.lumenRouter, AureliaTech.root), AureliaLogix.overflowGate);
        attach(gate, AureliaLogix.underflowGate);
        attach(aureliaNodes.get(AureliaForge.lumenMedic, fabNode), AureliaLogix.lumenHauler);
        attach(aureliaNodes.get(AureliaNova.auriteLancer, fabNode), AureliaLogix.auriteVanguard);
        attach(aureliaNodes.get(AureliaFrontier.tideLancer, fabNode), AureliaLogix.tideTender);

        //3.11 v8.2 command pack: the small projector hangs off the mender line, the big one off the small one,
        //and the titan off the vanguard - the heaviest ground unit the tree had.
        TechNode accel = attach(aureliaNodes.get(lumenMender, AureliaTech.root), AureliaCommand.lumenAccelerator);
        attach(accel, AureliaCommand.prismOvercharger);
        attach(aureliaNodes.get(AureliaLogix.auriteVanguard, fabNode), AureliaCommand.prismMonolith);

        //3.12 v8.3 cycle pack: the recycling line grows out of the kiln that makes the waste, the silo sits
        //next to the reclaimer that drains it, the dome hangs off the mender line, and the citadel is the
        //only child of the bastion - the tier-3 core.
        TechNode reclaim = attach(kilnNode, AureliaCycle.wasteReclaimer);
        attach(reclaim, AureliaCycle.wasteSilo);
        attach(reclaim, AureliaCycle.swarfFurnace);
        TechNode leach = attach(reclaim, AureliaCycle.leachTower);
        TechNode cryst = attach(leach, AureliaCycle.slurryCrystalliser);
        TechNode resyn = attach(cryst, AureliaCycle.resynthesiser);
        attach(resyn, AureliaCycle.prismReformer);
        attach(aureliaNodes.get(AureliaForge.mendDome, AureliaTech.root), AureliaCycle.wardDome);
        TechNode citadel = attach(aureliaNodes.get(AureliaBastion.bastionCore, AureliaTech.root), AureliaCycle.citadelCore);
        attach(citadel, AureliaCycle.citadelPilot);

        //4. sweep: anything researchable this jar owns that is still missing gets a home by category
        ObjectMap<Category, TechNode> hubs = new ObjectMap<>();
        hubs.put(Category.turret, horizon);
        hubs.put(Category.units, assembler);
        hubs.put(Category.crafting, press);
        hubs.put(Category.production, aureliaNodes.get(lumenExtractor, press));
        hubs.put(Category.distribution, aureliaNodes.get(lumenBridge, AureliaTech.root));
        hubs.put(Category.liquid, aureliaNodes.get(lumenConduit, AureliaTech.root));
        hubs.put(Category.power, aureliaNodes.get(lumenCell, AureliaTech.root));
        hubs.put(Category.defense, aureliaNodes.get(lumenVault, AureliaTech.root));
        hubs.put(Category.effect, aureliaNodes.get(lumenMender, AureliaTech.root));
        hubs.put(Category.logic, aureliaNodes.get(lumenProcessor, AureliaTech.root));

        Seq<Block> blocks = Vars.content.blocks().select(b -> mod(b) && researchable(b) && !aureliaNodes.containsKey(b));
        blocks.sort(b -> cost(b.requirements));
        for(Block b : blocks) attach(hubs.get(b.category, top), b);

        Seq<UnitType> units = Vars.content.units().select(u -> mod(u) && !u.isHidden() && !aureliaNodes.containsKey(u));
        units.sort(u -> u.health);
        for(UnitType u : units) attach(assembler, u);

        Seq<Item> items = Vars.content.items().select(i -> mod(i) && !aureliaNodes.containsKey(i));
        for(Item i : items) attach(top, i);

        //re-index: the sweep added nodes after step 2
        for(TechNode n : flatten(AureliaTech.root)) aureliaNodes.put(n.content, n);

        int priced = price();

        Log.info("[blackhole/merge] Aurelia tech tree: +@ nodes (@ research costs converted, @ free nodes priced), tree now holds @ nodes",
            added, costs, priced, flatten(AureliaTech.root).size);
    }

    /**
     * v7.8 - the mod is an Aurelia mod: Serpulo and Erekir get nothing. Every node this jar added to a vanilla
     * tree is dropped here (after {@link #retree()} re-hung the same content on Aurelia), and each content's
     * primary {@code techNode} pointer is moved to its Aurelia node so research, the database and the
     * "unlocked" checks all follow the planet it actually belongs to.
     */
    public static int purge(){
        ObjectSet<TechNode> keep = new ObjectSet<>();
        for(TechNode n : flatten(AureliaTech.root)) keep.add(n);

        Seq<TechNode> doomed = new Seq<>();
        for(TechNode n : TechTree.all.copy()){
            if(!keep.contains(n) && mod(n.content)) doomed.add(n);
        }
        for(TechNode n : doomed){
            n.remove();
            n.content.techNodes.remove(n);
            for(TechNode child : n.children.copy()){
                //a vanilla content hung under a mod node would be orphaned - reparent it onto the mod node's parent
                child.parent = n.parent;
                if(n.parent != null) n.parent.children.add(child);
            }
            n.children.clear();
        }
        int repointed = 0;
        for(Seq<?> list : new Seq[]{Vars.content.blocks(), Vars.content.items(), Vars.content.liquids(),
            Vars.content.units(), Vars.content.statusEffects()}){
            for(Object o : list){
                UnlockableContent c = (UnlockableContent)o;
                if(!mod(c)) continue;
                TechNode a = aureliaNodes.get(c);
                if(a == null) a = c.techNodes.find(keep::contains);
                if(a != null && c.techNode != a){
                    c.techNode = a;
                    repointed++;
                }
                c.techNodes.removeAll(n -> !keep.contains(n));
            }
        }
        Log.info("[blackhole/merge] planet purge: @ mod nodes removed from the Serpulo / Erekir trees, @ contents repointed at their Aurelia node", doomed.size, repointed);
        return doomed.size;
    }

    private static boolean researchable(Block b){
        return b.buildVisibility != BuildVisibility.hidden && b.buildVisibility != BuildVisibility.debugOnly
            && b.buildVisibility != BuildVisibility.sandboxOnly && b.requirements != null && b.requirements.length > 0;
    }

    private static float cost(ItemStack[] stacks){
        float c = 0;
        if(stacks != null) for(ItemStack s : stacks) c += s.amount * s.item.cost;
        return c;
    }

    /**
     * Adds a node to the Aurelia tree. The node's primary {@code content.techNode} pointer is restored, so the
     * content keeps the node of its original planet and vanilla lookups (and the existing trees) stay intact.
     */
    /**
     * v7.5 fix - no node may be free. Anything that reached the tree without requirements (items, liquids and a
     * few blocks whose research cost collapsed to zero during the conversion) gets a real price, so nothing in
     * the tree unlocks itself the moment you look at it. The planet root (the core) stays free, as in vanilla.
     */
    static int price(){
        int priced = 0;
        for(TechNode n : flatten(AureliaTech.root)){
            if(n == AureliaTech.root) continue;
            //an always-unlocked content would skip research entirely - only the landing core may do that
            if(n.content != AureliaContent.aureliaCore && n.content.alwaysUnlocked){
                n.content.alwaysUnlocked = false;
            }
            if(cost(n.requirements) > 0) continue;

            ItemStack[] req;
            if(n.content instanceof Item item){
                req = ItemStack.with(item, 120);
            }else if(n.content instanceof Liquid){
                req = ItemStack.with(lumenite, 150, aurite, 60);
            }else{
                ItemStack[] own = convert(n.content.researchRequirements());
                req = cost(own) > 0 ? own : ItemStack.with(lumenite, 120, aurite, 40);
            }
            n.setupRequirements(req);
            priced++;
        }
        return priced;
    }

    private static TechNode attach(TechNode parent, UnlockableContent c){
        if(c == null || parent == null) return parent;
        TechNode existing = aureliaNodes.get(c);
        if(existing != null) return existing;
        TechNode prev = c.techNode;
        ItemStack[] req = prev != null && prev.requirements != null && prev.requirements.length > 0
            ? convert(prev.requirements) : convert(c.researchRequirements());
        TechNode n = new TechNode(parent, c, req);
        n.planet = aurelia;
        if(prev != null) c.techNode = prev;
        aureliaNodes.put(c, n);
        added++;
        return n;
    }

    private static Seq<TechNode> flatten(TechNode root){
        Seq<TechNode> out = new Seq<>();
        if(root == null) return out;
        out.add(root);
        for(int i = 0; i < out.size; i++) out.addAll(out.get(i).children);
        return out;
    }
}
