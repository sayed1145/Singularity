package blackhole;

import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.Objectives.*;
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
    /** v8.5.1: 星髓石相关配方只挂一次（recost() 在 loadContent() 尾和 init() 里各跑一遍）。 */
    private static boolean marrowRecipesApplied;

    /**
     * v8.5.1: 以太合金吃星髓石 —— 护甲合金的骨架来自恒星残骸，也是星髓石最主要的持续消耗口。
     *
     * <p>为什么写在这里而不是方块的初始化块里：Sixfold（六倍包）的内容由 {@code AegisExpansion.load()}
     * 创建，比 {@link AureliaContent} 早，那时候 starMarrow 还是 null，写进去只会挂出一个空物品。
     * recost() 跑在所有 loadContent() 之后、ContentLoader.init() 冻结消耗器之前，是唯一安全的位置。
     */
    private static void applyMarrowRecipes(){
        if(marrowRecipesApplied || AureliaContent.starMarrow == null) return;
        marrowRecipesApplied = true;
        Item marrow = AureliaContent.starMarrow;
        //以太合金：星髓石最主要的持续消耗口（v8.5.1 起）
        if(sixfold.SixfoldMod.forgeSerpulo instanceof mindustry.world.blocks.production.GenericCrafter c){
            c.consumeItem(marrow, 1);
        }

        /*
         * v8.5.2：星髓石扩用 —— 后期机器与顶级单位的建造成本里都要它。
         *
         * 只加<b>一次性建造成本</b>，不加消耗器：产线不会因为缺星髓石而停摆，但星髓石从"堡垒线专用"
         * 变成了后期常规用料。数量按原造价定，最贵的机器给得最多，最便宜的给 2 —— 不改研究成本，
         * 也不改任何机器的产出与耗电。
         */
        addCost(AureliaCycle.resynthesiser, marrow, 2);
        addCost(AureliaCycle.prismReformer, marrow, 3);
        addCost(AureliaCycle.wardDome, marrow, 4);
        addCost(AureliaCycle.citadelCore, marrow, 12);
        addCost(AureliaFrontier.gravityWell, marrow, 3);
        addCost(BHBlocks.quasarLance, marrow, 3);

        //顶级单位：终端机与三台泰坦。找到哪个工厂在生产它们就改哪个方案。
        UnitType[] heavy = {
            AureliaCommand.prismMonolith,
            sixfold.SixfoldMod.titan,
            sixfold.SixfoldMod.pyroclast,
            voxel.content.VoxelUnits.titan,
            //v8.5.4 追加：重型与顶级单位的生产方案也要星髓石（按 60f*秒数 的建造时间定档）
            AureliaBastion.bastionWarden,      //60f * 160f
            AureliaTerra.basaltGuard,          //60f * 85f
            AureliaLogix.auriteVanguard,       //60f * 52f
            AureliaTerra.ventCaller,           //60f * 70f
            AureliaNova.resonanceSiege,        //60f * 105f
            AureliaContent.auroraHeavy,        //60f * 120f
            AureliaContent.auroraGuard,        //60f * 88f
            astro.content.AstroContent.detainer //60f * 210f —— 全包最贵的单位
        };
        int[] amounts = {10, 8, 8, 6, 4, 4, 3, 3, 3, 3, 2, 10};
        for(Block b : Vars.content.blocks()){
            if(b instanceof mindustry.world.blocks.units.UnitFactory uf && uf.plans != null){
                for(int i = 0; i < heavy.length; i++) addPlanCost(uf, heavy[i], marrow, amounts[i]);
            }
        }

        /*
         * v8.5.4 第二轮：后期主力机器的建造成本。和 v8.5.2 那批同一条规则 ——
         * 只加一次性建造成本，不加消耗器、不改产出、不改耗电、不改研究成本。
         * 数量按原造价定：越贵给得越多，最便宜的给 2。
         */
        addCost(AureliaBastion.bastionCore, marrow, 10);        //二级核心
        addCost(AureliaIndustry.prismHarvester, marrow, 3);
        addCost(AureliaIndustry.riftCollector, marrow, 2);
        addCost(AureliaIndustry.auroraRepairDome, marrow, 2);
        addCost(AureliaIndustry.plasmaDynamo, marrow, 2);
        addCost(AureliaCommand.prismOvercharger, marrow, 3);
        addCost(AureliaNova.resonanceHub, marrow, 2);
        addCost(AureliaFrontier.gravitonChurn, marrow, 3);
        addCost(AureliaFrontier.aegisLattice, marrow, 2);

        /*
         * v8.5.4：拘留者这一条线也要星髓石，两头都补：
         * <ul>
         *   <li><b>生产方案</b>（{@code UnitPlan}）—— 造一只要吃 10 份星髓石。这才是"拘留者的生产"，
         *       v160.5 的 {@code UnitType} 根本没有 {@code requirements} 字段，单位的生产成本
         *       完全由工厂里的 {@code UnitPlan} 决定。</li>
         *   <li><b>制造机本体的造价</b> —— 造那台 5x5 的 Astro 制造机也要 6 份。</li>
         * </ul>
         *
         * 顺带说明它为什么还在用原版材料：{@code recost()} 只遍历 {@code Vars.content.blocks()} 里的方块，
         * 而 Astro 这条线是合并进来的外部内容，所以拘留者那套硅 / 钛 / 钍 / 塑钢 / 相织布 /
         * 涌流合金从来没被换算成欧雷莉亚材料过。这一版按需求只补星髓石，不动它原来的材料。
         */
        addCost(astro.content.AstroContent.fabricator, marrow, 6);
    }

    /** 成本表里有没有这个物品（marrowRecipesApplied 之外再加一道保险，recost() 会跑两遍）。 */
    private static boolean hasCost(ItemStack[] stacks, Item item){
        if(stacks == null) return false;
        for(ItemStack s : stacks) if(s.item == item) return true;
        return false;
    }

    /** 往一个方块的建造成本里追加 N 份星髓石；Block.init() 已经跑过就按造价比例同步 buildTime。 */
    private static void addCost(Block b, Item marrow, int amount){
        if(b == null || b.requirements == null || amount <= 0 || hasCost(b.requirements, marrow)) return;
        float before = costOf(b.requirements);
        ItemStack[] req = new ItemStack[b.requirements.length + 1];
        System.arraycopy(b.requirements, 0, req, 0, b.requirements.length);
        req[b.requirements.length] = new ItemStack(marrow, amount);
        b.requirements = req;
        float after = costOf(req);
        //和 recost() 里同一条规则：buildTime > 0 说明 Block.init() 已经按旧成本算过一次
        if(b.buildTime > 0f && before > 0f) b.buildTime *= after / before;
    }

    /** 同上，给单位工厂里某个单位方案加。 */
    private static void addPlanCost(mindustry.world.blocks.units.UnitFactory uf, UnitType type, Item marrow, int amount){
        if(uf == null || uf.plans == null || type == null || amount <= 0) return;
        for(mindustry.world.blocks.units.UnitFactory.UnitPlan p : uf.plans){
            if(p.unit != type || p.requirements == null || hasCost(p.requirements, marrow)) continue;
            ItemStack[] req = new ItemStack[p.requirements.length + 1];
            System.arraycopy(p.requirements, 0, req, 0, p.requirements.length);
            req[p.requirements.length] = new ItemStack(marrow, amount);
            p.requirements = req;
        }
    }

    public static void recost(){
        rules();
        applyMarrowRecipes();
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
        int coolantPatches = repairHawkingCoolant();
        //v8.5.5: 星髓石炮弹必须在方块循环**之后**挂 —— convertAmmo() 会把整张弹药表换掉。
        MarrowAmmo.attach(AureliaContent.starMarrow);
        Log.info("[blackhole/merge] re-priced @ mod blocks in Aurelia resources (@ conversion rules; @ Hawking coolant repair)", recosted, rules.size, coolantPatches);
    }

    /**
     * The Singularity blocks are registered before Aurelia Frontier creates Aurora Coolant.  Rebind the
     * pre-init liquid consumer here, when both contents exist, so the actual recipe and its stats use
     * Aurora Coolant rather than vanilla cryofluid.  This is deliberately idempotent because recost() also
     * runs at mod init after consumers have been frozen.
     */
    private static int repairHawkingCoolant(){
        Block condenser = BHBlocks.hawkingCondenser;
        if(condenser == null || AureliaFrontier.auroraCoolant == null) return 0;
        ConsumeLiquid current = condenser.findConsumer(c -> c instanceof ConsumeLiquid liquid &&
            (liquid.liquid == Liquids.cryofluid || liquid.liquid == AureliaFrontier.auroraCoolant));
        if(current != null && current.liquid == AureliaFrontier.auroraCoolant) return 0;
        //Consumer arrays are immutable after Block.init(). A non-Aurora consumer at that time is an ordering
        //error, not something silently left as an inaccessible coolant recipe.
        if(condenser.consumers.length != 0){
            throw new IllegalStateException("Hawking condenser coolant was not repaired before init");
        }
        condenser.removeConsumers(c -> c instanceof ConsumeLiquid liquid && liquid.liquid == Liquids.cryofluid);
        condenser.consumeLiquid(AureliaFrontier.auroraCoolant, 0.12f);
        return 1;
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

        //The two end-game factories used to be free in this hand-built Aurelia spine. Both now have
        //non-empty research costs made exclusively of mod materials; the Yard additionally follows the
        //Horizon Assembler, so each prerequisite is obtainable from this mod's own chain.
        TechNode assembler = attach(core, BHBlocks.horizonAssembler, ItemStack.with(
            BHItems.hawkingDust, 90, BHItems.singularityCore, 16, prismAlloy, 150, resonanceShard, 120));
        TechNode lensing = attach(assembler, BHUnits.lensing);
        attach(lensing, BHUnits.ergosphere);
        TechNode yard = attach(assembler, BHBlocks.citadelYard, ItemStack.with(
            BHItems.hawkingDust, 180, BHItems.singularityCore, 42, SixfoldMod.aegisAlloy, 160,
            SixfoldMod.signalCore, 72, concordCore, 90));
        TechNode accretor = attach(yard, BHUnits.accretor);
        attach(accretor, BHUnits.citadel);

        //Aegis Titan matrix: press -> alloy -> encoder -> signal.  This direction matters: the press has no
        //alloy cost, and the encoder has no signal-core prerequisite, so the two production chains cannot loop.
        TechNode pressS = attach(dust, SixfoldMod.forgeSerpulo);
        TechNode alloy = attach(pressS, SixfoldMod.aegisAlloy);
        TechNode encS = attach(alloy, SixfoldMod.encoderSerpulo);
        TechNode signal = attach(encS, SixfoldMod.signalCore);
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
        //v8.5.2: the transmuter hangs off the crystalliser - it eats the same slurry, and it is the
        //recycling line's answer to a map that is short of a vein.
        attach(cryst, AureliaCycle.wasteTransmuter);
        attach(resyn, AureliaCycle.prismReformer);
        attach(aureliaNodes.get(AureliaForge.mendDome, AureliaTech.root), AureliaCycle.wardDome);
        TechNode citadel = attach(aureliaNodes.get(AureliaBastion.bastionCore, AureliaTech.root), AureliaCycle.citadelCore);
        attach(citadel, AureliaCycle.citadelPilot);

        //3.13 v8.5.2: the anti-Detainer pair. The turret hangs off the resonance line (it is a resonance
        //weapon), the gunner off the lattice crawler, which is the cradle's other product.
        TechNode anchor = attach(aureliaNodes.get(AureliaFrontier.latticeLink, AureliaTech.root), AnchorContent.anchorSpike);
        attach(anchor, AnchorContent.spikeWarden);

        //v8.4 utilities use natural, early Aurelia tech branches.
        attach(kilnNode, AureliaUtilities.incinerator);
        attach(aureliaNodes.get(lumenSorter, AureliaTech.root), AureliaUtilities.invertedSorter);

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

        //v8.5.1: the campaign is Aurelia-only. Nothing on the final tree may depend on something that does not
        //exist on Aurelia - neither a research requirement nor an objective ("research silicon first" can never
        //be satisfied here). sanitize() rewrites what the tier map knows and drops what it cannot, then price()
        //gives any node that lost its whole bill a real cost.
        int[] clean = sanitize();
        int priced = price();

        Log.info("[blackhole/merge] Aurelia tech tree: +@ nodes (@ research costs converted, @ objectives rewritten, @ objectives dropped, @ requirement stacks rewritten, @ nodes repriced), tree now holds @ nodes",
            added, costs, clean[0], clean[1], clean[2], priced, flatten(AureliaTech.root).size);
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
    /**
     * v8.5.1 - 欧雷莉亚树上不能出现"在欧雷莉亚永远做不到"的条件。
     *
     * <p>症状：视界装配厂下面挂着的一批单位（拘留者、巨像、蛛行者、镶嵌者、透镜号、能层号、吸积车、堡垒号）
     * 每一个都要求"先研究原版硅/钛/钍/塑钢/相织布/涌浪合金"，而这些原版物品在欧雷莉亚既没有科技节点、
     * 也产不出来，于是这几个节点永远灰着，玩家怎么攒资源都点不开。
     *
     * <p>做法：能按 tier 映射表（{@link #rules()}）换算成欧雷莉亚物品的目标就地改写
     * （硅 → 共振碎片，钛 → 金辉矿，石墨 → 棱镜玻璃，塑钢/钍 → 棱镜合金，相织布/涌浪合金 → 协律核心……），
     * 换算不了的（例如指向别的星球的区域目标）直接摘掉；研究需求里残留的原版物品同样换算，
     * 实在没有对应物的整条删掉，最后交给 {@link #price()} 补价。
     *
     * @return {改写掉的目标数, 摘掉的目标数, 改写掉的需求条数}
     */
    public static int[] sanitize(){
        rules();
        int rewritten = 0, dropped = 0, reqFixed = 0;

        for(TechNode n : flatten(AureliaTech.root)){
            //1) 目标条件
            if(n.objectives != null && !n.objectives.isEmpty()){
                Seq<Objective> kept = new Seq<>();
                ObjectSet<String> seen = new ObjectSet<>();
                for(Objective o : n.objectives){
                    Objective m = remap(o);
                    if(m == null){
                        dropped++;
                        continue;
                    }
                    if(m != o) rewritten++;
                    //换算之后可能两个目标指向同一个物品（钍和塑钢都 → 棱镜合金），去重
                    if(!seen.add(key(m))) continue;
                    kept.add(m);
                }
                n.objectives.clear();
                n.objectives.addAll(kept);
            }

            //2) 研究需求：原版物品换算成欧雷莉亚等价物；没有对应物的删掉
            if(n.requirements != null && n.requirements.length > 0){
                ItemStack[] conv = convert(n.requirements);
                OrderedMap<Item, Integer> acc = new OrderedMap<>();
                boolean changed = false;
                for(ItemStack st : conv){
                    if(st.item == null) continue;
                    if(!mod(st.item) && !onAurelia(st.item)){
                        changed = true;
                        reqFixed++;
                        continue;
                    }
                    if(st.item != null) acc.put(st.item, acc.get(st.item, 0) + Math.max(1, st.amount));
                }
                if(changed){
                    Seq<Item> keys = acc.orderedKeys().copy();
                    keys.sort(i -> i.cost);
                    ItemStack[] out = new ItemStack[keys.size];
                    for(int i = 0; i < keys.size; i++) out[i] = new ItemStack(keys.get(i), acc.get(keys.get(i)));
                    n.setupRequirements(out);
                }
            }
        }
        return new int[]{rewritten, dropped, reqFixed};
    }

    /** 这个物品在欧雷莉亚拿得到吗：本模组的物品，或者欧雷莉亚自己的原版掉落物。 */
    private static boolean onAurelia(UnlockableContent c){
        if(c == null) return false;
        if(mod(c)) return true;
        //欧雷莉亚穹顶自己产出的电力/液体不在科技树上，也不该出现在研究账单里
        return false;
    }

    private static String key(Objective o){
        if(o instanceof Research r) return "R:" + (r.content == null ? "?" : r.content.name);
        if(o instanceof Produce p) return "P:" + (p.content == null ? "?" : p.content.name);
        if(o instanceof SectorComplete sc) return "S:" + (sc.preset == null ? "?" : sc.preset.name);
        if(o instanceof OnSector os) return "O:" + (os.preset == null ? "?" : os.preset.name);
        if(o instanceof OnPlanet op) return "N:" + (op.planet == null ? "?" : op.planet.name);
        return o.getClass().getName() + "@" + System.identityHashCode(o);
    }

    /**
     * 把一个目标改写成欧雷莉亚版本。返回 null 表示这个目标在欧雷莉亚永远做不到，应当摘掉。
     */
    private static Objective remap(Objective o){
        if(o == null) return null;

        //"先研究某个物品" / "生产出某个物品"
        if(o instanceof Research r){
            Item like = r.content instanceof Item it ? it : null;
            UnlockableContent target = remapContent(r.content, like);
            if(target == null) return null;
            //本来就是欧雷莉亚内容时原样返回，别让计数器把"没动过"算成"改写过"
            return target == r.content ? o : new Research(target);
        }
        if(o instanceof Produce p){
            Item like = p.content instanceof Item it ? it : null;
            UnlockableContent target = remapContent(p.content, like);
            if(target == null) return null;
            return target == p.content ? o : new Produce(target);
        }

        //区域目标：只保留落在欧雷莉亚上的（星球/区域都直接挂在 SectorPreset 上）
        if(o instanceof OnPlanet op) return op.planet == aurelia ? o : null;
        if(o instanceof OnSector os) return os.preset != null && os.preset.planet == aurelia ? o : null;
        if(o instanceof SectorComplete sc) return sc.preset != null && sc.preset.planet == aurelia ? o : null;

        //其它类型的目标（波次、击杀……）在欧雷莉亚都能正常完成，原样保留
        return o;
    }

    /**
     * 外来内容 → 欧雷莉亚等价物。物品走 tier 映射表；方块/单位一律当成"做不到"，
     * 因为在欧雷莉亚装不出原版的生产链。
     */
    private static UnlockableContent remapContent(UnlockableContent c, Item asItem){
        if(c == null) return null;
        if(mod(c)) return c;
        if(asItem != null){
            Object[] r = rules.get(asItem);
            if(r != null && r[0] instanceof Item to) return to;
        }
        return null;
    }

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
        return attach(parent, c, null);
    }

    /** Adds a node with an optional explicit research bill. Explicit bills are used for the two repaired late
     * factories so their cost cannot accidentally collapse to a free or vanilla-resource unlock. */
    private static TechNode attach(TechNode parent, UnlockableContent c, ItemStack[] explicitRequirements){
        if(c == null || parent == null) return parent;
        TechNode existing = aureliaNodes.get(c);
        if(existing != null) return existing;
        TechNode prev = c.techNode;
        ItemStack[] req = explicitRequirements != null ? explicitRequirements :
            (prev != null && prev.requirements != null && prev.requirements.length > 0
                ? convert(prev.requirements) : convert(c.researchRequirements()));
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
