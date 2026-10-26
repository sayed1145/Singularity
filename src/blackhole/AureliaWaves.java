package blackhole;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.game.*;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.world.blocks.storage.CoreBlock;


/**
 * Aurelia wave generator.
 *
 * <p>v8.5.1 之前这里写死了五种单位，模组里另外三十来种能打的单位一张图都刷不出来。
 * v8.5.2 改成按 roster 铺开并补上了"每一波都得出兵"的洞。
 *
 * <h2>v8.5.3 这次改了什么（只动刷怪频率与刷怪配置，一个单位属性都没碰）</h2>
 * <ol>
 *   <li><b>难度整体减半。</b>全局 {@link #DIFFICULTY} 作用在"单波上限 / 涨速 / 护盾"上：
 *       上限按分档打折、涨速放缓（{@code unitScaling} —— 引擎里这个值越大涨得越慢，放缓幅度随威胁度回落）、
 *       护盾成长减半。</li>
 *   <li><b>高级怪刷新频率 −25%。</b>T4 / T5 的间隔拉长到 {@code 1 / 0.75}；T5 再额外打七折。
 *       T3 也顺手拉长一点。</li>
 *   <li><b>新手图不出 T5，最高只到 T4。</b>威胁度低于 {@link #T5_THREAT_GATE} 的图（战役前几张）
 *       直接不生成 T5 分组；威胁度越高的图，T5 来得越早、越密 —— 后期依然是"特别多 T5"。</li>
 *   <li><b>把所有能刷的怪都刷出来。</b>除了<b>没有武器的</b>、<b>海军</b>（潮汐系列）和
 *       <b>玩家的机体</b>（三座核心各自的 chassis）之外，roster 里没有的单位会被自动补进来，
 *       强度按血量 / 火力粗估分档。以后再加单位也不会漏。</li>
 *   <li><b>每一波都出兵</b>：沿用 v8.5.2 的 {@link #ensureEveryWave} —— 官方只在
 *       {@code (wave - begin) % spacing == 0} 的波次出兵，roster 的 spacing 是 2..7，
 *       必然留下整波空窗；这里只在真的空了的波次上补一只一次性分组。</li>
 * </ol>
 */
public final class AureliaWaves{
    private AureliaWaves(){}

    // =====================================================================================================
    // v8.5.3 难度旋钮（全部只作用在刷怪配置上）
    // =====================================================================================================

    /**
     * 难度总闸：0.5 = 数量与护盾成长减半、涨速放慢一倍。
     * 只乘在 SpawnGroup 的 {@code max / shieldScaling / unitScaling} 上，不动任何 UnitType 属性。
     */
    public static float DIFFICULTY = 0.45f;

    /** 新手图（威胁度低于此值）不刷 T5，最高只到 T4。 */
    public static float T5_THREAT_GATE = 0.34f;

    /** T4 / T5 的刷新频率：0.75 = 频率降低 25%（间隔拉长到 1/0.75）—— 需求里点名的那一条。 */
    public static float HIGH_TIER_RATE = 0.75f;

    /** T5 在高级折扣之上再打的折 —— 让"特别多 T5"只出现在后期图上。 */
    public static float T5_RATE = 0.7f;

    /**
     * T1 / T2 / T3 的刷新频率。
     *
     * <p>只砍 {@code max} 到不了"整体减半"：{@code unitAmount} 和 {@code max} 的下限都是 1，
     * 而 {@code unitScaling}（每 +1 只需要的波数）本来就很大，绝大多数波次每组就是 1 只 ——
     * 上限根本用不上。真正决定"一波来多少"的是 <b>Σ(1 / spacing)</b>，也就是各组多久来一次，
     * 所以难度总闸主要落在这里。
     */
    public static float LOW_TIER_RATE = 0.60f;
    public static float MID_TIER_RATE = 0.55f;

    /** 数量削弱的上下界：新手图削到 {@link #nerfLow}，终局图削到 {@link #nerfHigh}。 */
    public static float nerfLow = 0.34f, nerfHigh = 0.60f;

    /**
     * v8.5.6：<b>强度增长倍数 1.25</b>。
     *
     * <p>引擎里 {@code unitScaling} 是"每多刷 1 只需要的波数"，<b>越小涨得越快</b>。
     * 所以"强度增长 1.25 倍" = 把 unitScaling 乘到 1 / 1.25 = 0.8。
     *
     * <p>而且它是<b>随威胁度线性介入</b>的：新手图（threat = 0）乘 1.0（一点不变），
     * 终局图（threat = 1）乘 0.8（涨速 1.25 倍）。这就是"新手图强度不变，
     * 只是说强度增长 1.25" —— 变的是坡度，不是起点。
     */
    public static float GROWTH_BOOST = 1.25f;

    /** v8.5.6：<b>刷怪数量 ×1.2</b>。同时作用在每组的上限和"一次来几只"上。 */
    public static float COUNT_BOOST = 1.2f;

    /** 强度增长在威胁度 d 处的系数：d=0 → 1.0（新手图不变），d=1 → 1/1.25 = 0.8。 */
    static float growthScale(float d){
        return 1f / Mathf.lerp(1f, GROWTH_BOOST, Mathf.clamp(d));
    }

    /** 一个出场条目。start 是"从总波数的百分之几开始"，spacing 是隔几波来一次。 */
    public static final class Slot{
        public final String tag;
        public final UnitType type;
        /** 0..1，越大越晚、越稀 */
        public final float power;
        /** 1..5，分档。5 = 终局重器 */
        public final int tier;
        public final float start;
        public final int spacing;
        public final int max;

        Slot(String tag, UnitType type, float power, int tier, float start, int spacing, int max){
            this.tag = tag; this.type = type; this.power = power; this.tier = tier;
            this.start = start; this.spacing = spacing; this.max = max;
        }
    }

    /** 会刷的单位清单。power 越大 = 越强 = 出场越晚、越稀；tier 是分档（1..5）。 */
    private static final Slot[] ROSTER = {
        // ---- T1：开局就能遇到，数量靠堆 ----
        new Slot("微光", AureliaContent.glint, 0.05f, 1, 0.00f, 2, 20),
        new Slot("碎片漫游者", AureliaContent.shardRover, 0.06f, 1, 0.00f, 2, 20),
        new Slot("微尘", AureliaFrontier.mote, 0.04f, 1, 0.00f, 2, 20),
        new Slot("棱镜侦察车", AureliaNova.prismScout, 0.07f, 1, 0.04f, 2, 18),
        new Slot("盐晶掠击机", AureliaTerra.glassHarrier, 0.08f, 1, 0.06f, 2, 18),
        new Slot("极光哨卫机", AureliaNova.auroraSentry, 0.12f, 1, 0.08f, 3, 16),
        new Slot("镶嵌者", voxel.content.VoxelUnits.tessellus, 0.14f, 1, 0.10f, 3, 14),

        // ---- T2 ----
        new Slot("金辉突击车", AureliaNova.auriteLancer, 0.22f, 2, 0.14f, 3, 14),
        new Slot("晶格蠕者", AureliaFrontier.latticeCrawler, 0.20f, 2, 0.14f, 3, 14),
        new Slot("掘岩工程车", AureliaTerra.terraBorer, 0.26f, 2, 0.18f, 3, 12),
        new Slot("工程车", AureliaForge.auriteWright, 0.24f, 2, 0.18f, 3, 12),
        new Slot("喷口支援机", AureliaTerra.ventCaller, 0.30f, 2, 0.20f, 3, 12),
        new Slot("金辉壁垒", AureliaContent.auriteBulwark, 0.34f, 2, 0.22f, 3, 12),
        new Slot("透镜号", BHUnits.lensing, 0.38f, 2, 0.26f, 3, 10),

        // ---- T3 ----
        new Slot("金辉先锋", AureliaLogix.auriteVanguard, 0.44f, 3, 0.30f, 4, 10),
        new Slot("蛛行者", voxel.content.VoxelUnits.arachne, 0.46f, 3, 0.32f, 4, 9),
        new Slot("共振炮车", AureliaNova.resonanceSiege, 0.48f, 3, 0.34f, 4, 9),
        new Slot("堡垒守卫", AureliaBastion.bastionWarden, 0.50f, 3, 0.34f, 4, 9),
        new Slot("吸积车", BHUnits.accretor, 0.52f, 3, 0.36f, 4, 8),
        new Slot("玄武卫士", AureliaTerra.basaltGuard, 0.54f, 3, 0.38f, 4, 8),
        new Slot("极光卫士", AureliaContent.auroraGuard, 0.58f, 3, 0.40f, 4, 8),
        new Slot("能层号", BHUnits.ergosphere, 0.62f, 3, 0.42f, 5, 7),

        // ---- T4 ----
        new Slot("极光重机", AureliaContent.auroraHeavy, 0.70f, 4, 0.48f, 5, 6),
        new Slot("巨像", voxel.content.VoxelUnits.titan, 0.76f, 4, 0.54f, 6, 5),

        // ---- T5：终局重器，新手图不刷 ----
        new Slot("堡垒号", BHUnits.citadel, 0.86f, 5, 0.62f, 6, 3),
        new Slot("棱镜巨碑", AureliaCommand.prismMonolith, 0.88f, 5, 0.66f, 7, 3),
        new Slot("炽霄泰坦", sixfold.SixfoldMod.pyroclast, 0.92f, 5, 0.70f, 7, 3),
        new Slot("苍盾泰坦", sixfold.SixfoldMod.titan, 0.94f, 5, 0.74f, 7, 3),
    };

    /** 拘留者：太超标，只在战役后半段的高威胁图出现，而且整张图只来两次。 */
    private static final float DETAINER_THREAT = 0.55f;
    private static final float DETAINER_START = 0.72f;

    /**
     * "重器"的门槛：power 到这个数就算终局重器（T4 / T5）。
     * 重器也遵守"每张图至少出场一次"的要求，但做了两点保护：单次只来 1 只、不随波数增长。
     */
    private static final float HEAVY_POWER = 0.7f;

    /** 想把重器只留给中后期地图的话改这个数；默认 0 —— 按"所有可攻击单位在所有地图都出场"执行。 */
    public static final float HEAVY_THREAT_GATE = 0f;

    // ------------------------------------------------------------------ 分档削弱

    /** 各档的刷新频率系数（<1 = 更稀）。T4 / T5 是需求里点名的"降低 25%"。 */
    static float tierRate(int tier){
        if(tier >= 5) return HIGH_TIER_RATE * T5_RATE;
        if(tier >= 4) return HIGH_TIER_RATE;
        if(tier >= 3) return MID_TIER_RATE;
        return LOW_TIER_RATE;
    }

    /**
     * 各档的数量系数。整体在 {@link #DIFFICULTY} 附近（"难度降低一半"），但按威胁度倾斜：
     * 新手图削到 0.40（压力最大、科技最少），终局图只削到 0.65 —— 后期依然是高难度。
     */
    public static float countNerf(int tier, float threat){
        float byTier = tier >= 5 ? 0.70f : tier >= 4 ? 0.85f : tier >= 3 ? 0.95f : 1f;
        return Mathf.lerp(nerfLow, nerfHigh, Mathf.clamp(threat)) * byTier;
    }

    /** 强度 → 分档。给自动补进来的单位和 {@link AureliaCampaign#balance} 用。 */
    public static int tierOf(float power){
        return power < 0.15f ? 1 : power < 0.35f ? 2 : power < 0.65f ? 3 : power < 0.85f ? 4 : 5;
    }

    // ------------------------------------------------------------------ roster（含自动补全）

    private static Seq<Slot> full;

    /** 完整 roster：手写那 28 条 + 任何"能打、不是海军、不是玩家机体"却漏掉的单位。 */
    static synchronized Seq<Slot> roster(){
        if(full != null) return full;

        Seq<Slot> list = new Seq<>();
        ObjectSet<UnitType> seen = new ObjectSet<>();
        for(Slot s : ROSTER){
            if(s.type == null) continue;
            list.add(s);
            seen.add(s.type);
        }
        autoAppend(list, seen);
        //ordered by `start` first: filler() breaks out of the loop on the first slot that starts too late
        list.sort((a, b) -> Float.compare(a.start, b.start) != 0 ? Float.compare(a.start, b.start) : Float.compare(a.power, b.power));
        return full = list;
    }

    /**
     * 把 roster 里没有的单位补进来。
     *
     * <p>三条排除规则，正是需求里那条"除了不能攻击的 / 玩家的机体 / 海军以外，把所有怪都刷出来"：
     * <ul>
     *   <li><b>不能攻击</b>：{@code !hasWeapons()}（没有武器的后勤机 —— 运输、建造、搬运）</li>
     *   <li><b>海军</b>：{@code naval}（潮汐系列只能在水上走，陆地波次刷出来会卡住）</li>
     *   <li><b>玩家的机体</b>：任何一座核心的 {@code unitType}（三座核心各自的 chassis）</li>
     * </ul>
     */
    private static void autoAppend(Seq<Slot> list, ObjectSet<UnitType> seen){
        if(Vars.content == null) return;

        ObjectSet<UnitType> chassis = new ObjectSet<>();
        for(var b : Vars.content.blocks()){
            if(b instanceof CoreBlock c && c.unitType != null) chassis.add(c.unitType);
        }

        for(UnitType u : Vars.content.units()){
            if(u == null || u.internal || seen.contains(u)) continue;
            if(!u.name.startsWith("blackhole-")) continue;   //Aurelia 只看得到模组自己的内容
            if(!u.hasWeapons()) continue;                    //1. 不能攻击
            if(u.naval) continue;                            //2. 海军
            if(chassis.contains(u)) continue;                //3. 玩家的机体
            //4. 拘留者不走常规 roster：它由下面那段"整张图只来两只"的独立分组管，
            //   混进常规表就会变成和普通怪一样按波数滚雪球。
            if(u == astro.content.AstroContent.detainer) continue;

            float power = estimate(u);
            int tier = tierOf(power);
            float start = tier == 1 ? 0.02f : tier == 2 ? 0.14f : tier == 3 ? 0.30f : tier == 4 ? 0.48f : 0.62f;
            int spacing = tier == 1 ? 2 : tier == 2 ? 3 : tier == 3 ? 4 : tier == 4 ? 5 : 6;
            int max = tier == 1 ? 16 : tier == 2 ? 12 : tier == 3 ? 9 : tier == 4 ? 6 : 3;
            list.add(new Slot(u.localizedName == null ? u.name : u.name, u, power, tier, start, spacing, max));
            seen.add(u);
        }
    }

    /** 按血量与火力粗估强度 0..1，只给自动补进来的单位用（手写那 28 条是人工校准过的）。 */
    static float estimate(UnitType u){
        float dps = 0f;
        for(Weapon w : u.weapons){
            if(w.bullet == null) continue;
            dps += w.bullet.damage * 60f / Math.max(w.reload, 1f) * (w.mirror ? 2f : 1f);
        }
        float v = Math.max(u.health * (1f + dps / 120f), 50f);
        float lo = (float)Math.log(500f), hi = (float)Math.log(60000f);
        return Mathf.clamp((float)((Math.log(v) - lo) / (hi - lo)));
    }

    // ------------------------------------------------------------------ 生成

    public static Seq<SpawnGroup> generate(float threat, int winWave, Rand rand){
        float d = Mathf.clamp(threat, 0f, 1f);
        //harder sectors ramp faster (lower scaling = more units per wave)
        //v8.5.3：再叠一层随威胁度回落的放缓系数 —— 新手图几乎不长（×2.2），
        //终局图维持原速（×1.0）。"难度减半"削的是前期被压死，后期图照旧滚雪球。
        float ramp = Mathf.lerp(1.25f, 0.7f, d) * Mathf.lerp(2.2f, 1.0f, d);
        //v8.5.6：强度增长 1.25 —— 新手图乘 1.0（不变），终局图乘 0.8（涨速快 1.25 倍）
        ramp *= growthScale(d);
        int shift = rand.random(0, 2);
        Seq<SpawnGroup> out = new Seq<>();

        for(Slot s : roster()){
            if(s.type == null) continue;
            //新手图不出 T5，最高只到 T4
            if(s.tier >= 5 && d < T5_THREAT_GATE) continue;
            if(s.power >= HEAVY_POWER && d < HEAVY_THREAT_GATE) continue;

            SpawnGroup g = new SpawnGroup(s.type);
            //档位按总波数的比例算，保证 15 波的新手图和 79 波的终局图都排得下
            g.begin = Math.max(1, Math.round(winWave * s.start)) + (s.start <= 0f ? shift : 0);
            g.begin = Math.min(g.begin, Math.max(1, winWave - 1));
            //v8.5.3：高级怪刷新频率降低 —— 间隔拉长到 1 / rate
            g.spacing = Math.max(1, Math.round(s.spacing / tierRate(s.tier)));
            //v8.5.6：一次来几只也 ×1.2（1 -> 1，2 只起的部分才会往上抬）
            g.unitAmount = Math.max(1, Math.round(1f * COUNT_BOOST));
            if(s.power >= HEAVY_POWER){
                //重器：来 1 只就够，而且不随波数增长
                g.unitScaling = SpawnGroup.never;
                g.max = 1;
            }else{
                //v8.5.3：涨速放慢一倍（引擎里 unitScaling 越大，每 +1 只需要的波数越多）
                g.unitScaling = Mathf.lerp(2f, 6f, s.power) * ramp;
                int base = Math.max(1, Math.round(Mathf.lerp(s.max, Math.max(2, s.max / 2f), d)));
                //v8.5.6：数量 ×1.2
                g.max = Math.max(1, Math.round(base * countNerf(s.tier, d) * COUNT_BOOST));
            }
            //v8.5.3：护盾成长同步减半
            g.shieldScaling = Mathf.lerp(0f, 45f, Mathf.clamp(s.power * 1.2f)) * d * DIFFICULTY;
            out.add(g);
        }

        if(d >= DETAINER_THREAT && astro.content.AstroContent.detainer != null){
            SpawnGroup boss = new SpawnGroup(astro.content.AstroContent.detainer);
            //只在相邻的两波各来 1 只：整张图合计正好 2 只，不会滚雪球
            boss.begin = Math.min(Math.max(1, Math.round(winWave * DETAINER_START)), Math.max(1, winWave - 2));
            boss.end = boss.begin + 1;
            boss.spacing = 1;
            boss.unitAmount = 1;
            boss.unitScaling = SpawnGroup.never;
            boss.max = 1;
            boss.shields = 900f * d * DIFFICULTY;
            out.add(boss);
        }

        ensureEveryWave(out, d, winWave);
        return out;
    }

    /**
     * 保证每一波都刷得出东西。
     *
     * <p>官方 {@code mindustry.ai.WaveSpawner#spawnEnemies()} 用 {@code group.getSpawned(state.wave - 1)}
     * 取数量，而 {@code SpawnGroup#getSpawned}（Mindustry v160.5 源码）的判定是：
     * <pre>
     * if(wave &lt; begin || wave &gt; end || (wave - begin) % spacing != 0) return 0;
     * </pre>
     * 即<b>只有落在 begin + n*spacing 上的波次才会出兵</b>。
     *
     * <p>这里不去改既有分组的 spacing / begin，只在"确实一只都不刷"的波次上补一组一次性分组：
     * {@code begin = end = 那一波}、{@code spacing = 1}、{@code unitAmount = 1}、
     * {@code unitScaling = never}。已经能刷的波次，数值一个字都不动。
     */
    private static void ensureEveryWave(Seq<SpawnGroup> out, float threat, int winWave){
        int last = Math.max(0, winWave - 1);
        for(int w = 0; w <= last; w++){
            if(covered(out, w)) continue;
            out.add(filler(w, last, threat));
        }
    }

    /** 这一波有没有任何一组会出兵（含已经排好的 boss 与拘留者）。 */
    private static boolean covered(Seq<SpawnGroup> groups, int wave){
        for(int i = 0; i < groups.size; i++){
            SpawnGroup g = groups.get(i);
            if(g.type == null) continue;
            if(g.getSpawned(wave) > 0) return true;
        }
        return false;
    }

    /** 给空缺的那一波补一组：按这张图的进度挑一个已经"解锁"的单位，只来 1 只。 */
    private static SpawnGroup filler(int wave, int last, float threat){
        float t = last <= 0 ? 0f : (float)wave / last;
        Slot pick = null;
        for(Slot s : roster()){
            //roster 按 power 升序排列，后面的只会更晚
            if(s.start > t + 1e-4f) break;
            if(s.type == null) continue;
            if(s.tier >= 5 && threat < T5_THREAT_GATE) continue;   //新手图补位也绝不能补出 T5
            if(s.power >= HEAVY_POWER && threat < HEAVY_THREAT_GATE) continue;
            pick = s;
        }
        if(pick == null) pick = roster().first();
        SpawnGroup g = new SpawnGroup(pick.type);
        g.begin = g.end = wave;
        g.spacing = 1;
        g.unitAmount = 1;
        g.unitScaling = SpawnGroup.never;
        g.max = 1;
        g.shieldScaling = 0f;
        return g;
    }

    /** 旧签名：拿不到总波数时用一个保守值。仅用于兼容，正式路径都走 {@link #generate(float, int, Rand)}。 */
    public static Seq<SpawnGroup> generate(float threat, Rand rand){
        return generate(threat, 40, rand);
    }

    /**
     * 强度 0..1：给 {@link AureliaCampaign#balance} 用来压上限 —— 弱的单位可以成群，
     * 强的单位一张图里同时存在的一两只就够了。
     */
    public static float power(UnitType type){
        for(Slot s : roster()){
            if(s.type == type) return s.power;
        }
        if(type == astro.content.AstroContent.detainer) return 1f;
        return 0.5f;
    }

    /** 分档 1..5：给 {@link AureliaCampaign#balance} 用来按档削弱。 */
    public static int tier(UnitType type){
        for(Slot s : roster()){
            if(s.type == type) return s.tier;
        }
        if(type == astro.content.AstroContent.detainer) return 5;
        return tierOf(estimate(type));
    }
}
