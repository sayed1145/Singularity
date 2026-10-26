package blackhole;

import arc.util.Log;
import mindustry.content.Fx;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.type.*;
import mindustry.world.blocks.defense.turrets.ItemTurret;

/**
 * v8.5.5：<b>星髓石当炮弹</b>。
 *
 * <p>星髓石原来只有一种用法 —— 当建造材料。这一版让它也能塞进炮塔当弹药，
 * 但<b>只有两门炮能用</b>（v8.5.6 起）：
 * <ul>
 *   <li><b>吸积炮</b> —— 星髓长枪，伤害 <b>165</b>（含 90 的范围伤害）。</li>
 *   <li><b>事件视界炮</b> —— 星髓重枪，伤害 <b>320</b>（含 190 的范围伤害）。</li>
 * </ul>
 * 两发都给目标烙上同一个减益状态 {@link #marrowBrand}（星髓灼印）。
 *
 * <p>整套特效在 {@link MarrowFx} 里，从零手写，没有复用任何现成特效。
 *
 * <h2>为什么放在这里而不是方块的初始化块里</h2>
 *
 * <p>和星髓石建造成本同一个道理：{@link AureliaContent}（星髓石在那儿）比
 * {@link BHBlocks}（炮塔在那儿）注册得晚，而 {@code recost()} 跑在所有
 * {@code loadContent()} 之后、引擎冻结消耗器之前 —— 是唯一两边都拿得到的位置。
 *
 * <h2>数值</h2>
 *
 * <table border="1" cellpadding="3">
 *   <tr><th>项</th><th>值</th><th>说明</th></tr>
 *   <tr><td>伤害（吸积炮 / 事件视界炮）</td><td><b>165 / 320</b></td><td>需求是"伤害过百"</td></tr>
 *   <tr><td>范围伤害 / 半径（吸积炮）</td><td>90 / 26 像素（3.25 格）</td><td>命中后额外炸一圈</td></tr>
 *   <tr><td>范围伤害 / 半径（事件视界炮）</td><td>190 / 34 像素（4.25 格）</td><td>装药更足</td></tr>
 *   <tr><td>射程</td><td>7.5 × 40 = 300 像素（37.5 格）</td><td>介于吸积炮 235 与视界 400 之间</td></tr>
 *   <tr><td>每份矿几发</td><td>4（{@code ammoMultiplier}）</td><td>星髓石稀有，所以一份顶四发</td></tr>
 *   <tr><td>附带状态</td><td>星髓灼印，6 秒</td><td>见 {@link #marrowBrand}</td></tr>
 * </table>
 */
public final class MarrowAmmo{
    private MarrowAmmo(){}

    /**
     * 星髓灼印 —— 星髓石炮弹打中之后留下的减益（buff）。
     *
     * <p>四层效果叠在一起，让被打中的目标<b>明显变软</b>：
     * <ul>
     *   <li><b>持续掉血</b>：每秒约 20 点，6 秒共约 123 点 —— 和直伤 165 加起来，
     *       一发总的期望伤害是 288。</li>
     *   <li><b>减速 38%</b>：跑不掉，也追不上。</li>
     *   <li><b>输出 −28%、射速 −35%</b>：它自己打人也变软。</li>
     * </ul>
     */
    public static StatusEffect marrowBrand;

    /** 星髓长枪 —— 星髓石炮弹（普通档，吸积炮用）。 */
    public static BulletType marrowLance;

    /**
     * 星髓重枪 —— 同一个弹种，但由<b>事件视界炮</b>发射时改用这一发，伤害 <b>320</b>。
     *
     * <p>事件视界炮的冷却是 9 秒、一次吃掉 2 份弹药、射程 400，本来就是"一发定音"的炮；
     * 让它打 165 太浪费，所以单独给一发 300～350 区间里的 320。
     * 除了伤害和范围伤害，其余（速度、射程、每份几发、特效、灼印）和两个版本完全一致 ——
     * 换的是弹头装药，不是弹道。
     */
    public static BulletType marrowLanceHeavy;

    private static boolean loaded, attached;

    /** 创建状态与弹药。必须在 {@link AureliaContent#load()} 之后。 */
    public static void load(){
        if(loaded) return;
        loaded = true;

        marrowBrand = new StatusEffect("marrow-brand"){{
            color = MarrowFx.shell;
            //持续灼烧：0.3/刻 ≈ 18 点/秒
            damage = 0.30f;
            //每 45 刻（0.75 秒）再炸一次 20 点 —— 合计约 45 点/秒，6 秒共约 270 点
            intervalDamage = 20f;
            intervalDamageTime = 45f;
            //被打中的目标整体变软
            speedMultiplier = 0.62f;      //减速 38%
            damageMultiplier = 0.72f;     //输出 -28%
            reloadMultiplier = 0.65f;     //射速 -35%
            effectChance = 0.25f;
            effect = MarrowFx.brand;
            applyEffect = MarrowFx.brandApply;
        }};

        marrowLance = new BasicBulletType(7.5f, 165f){{
            lifetime = 40f;                 //7.5 * 40 = 300 px of range
            width = 9f;
            height = 15f;
            hitSize = 5f;
            splashDamage = 90f;
            splashDamageRadius = 26f;
            knockback = 2.2f;
            ammoMultiplier = 4f;            //one star marrow is four shots
            reloadMultiplier = 0.85f;       //heavy round: the turret cycles a little slower
            buildingDamageMultiplier = 0.75f;

            frontColor = MarrowFx.core;
            backColor = MarrowFx.shell;
            trailColor = MarrowFx.shell;
            trailLength = 12;
            trailWidth = 2.6f;

            //a wake every 3 ticks along the flight path
            trailEffect = MarrowFx.wake;
            trailInterval = 3f;

            shootEffect = MarrowFx.shoot;
            smokeEffect = Fx.none;
            hitEffect = MarrowFx.hit;
            despawnEffect = MarrowFx.hit;

            status = marrowBrand;
            statusDuration = 60f * 6f;
        }};

        //事件视界炮专用：同样的弹道，装药更足
        marrowLanceHeavy = new BasicBulletType(7.5f, 320f){{
            lifetime = 40f;
            width = 11f;
            height = 18f;
            hitSize = 6f;
            splashDamage = 190f;
            splashDamageRadius = 34f;
            knockback = 3.2f;
            ammoMultiplier = 4f;
            reloadMultiplier = 0.85f;
            buildingDamageMultiplier = 0.75f;

            frontColor = MarrowFx.core;
            backColor = MarrowFx.shell;
            trailColor = MarrowFx.shell;
            trailLength = 16;
            trailWidth = 3.4f;

            trailEffect = MarrowFx.wake;
            trailInterval = 3f;

            shootEffect = MarrowFx.shoot;
            smokeEffect = Fx.none;
            hitEffect = MarrowFx.hit;
            despawnEffect = MarrowFx.hit;

            status = marrowBrand;
            statusDuration = 60f * 6f;
        }};
    }

    /**
     * 把星髓石注册成各炮塔的弹药。必须在 {@code recost()} 的方块循环之后跑，
     * 否则 {@code convertAmmo()} 会把整张弹药表换掉。
     *
     * <p>v8.5.6：只挂在<b>吸积炮</b>和<b>事件视界炮</b>上（之前是四门）。
     * 事件视界炮拿 {@link #marrowLanceHeavy}（伤害 320），吸积炮保持 {@link #marrowLance}（165）。
     */
    public static void attach(Item marrow){
        if(attached || marrow == null || marrowLance == null) return;
        attached = true;
        int ok = 0;
        if(addAmmo(BHBlocks.accretionCannon, marrow, marrowLance)) ok++;
        if(addAmmo(BHBlocks.eventHorizon, marrow, marrowLanceHeavy)) ok++;
        Log.info("[blackhole/marrow] star marrow registered as turret ammo on @/2 turrets "
            + "(accretion-cannon @+@, event-horizon @+@)",
            ok, (int)marrowLance.damage, (int)marrowLance.splashDamage,
            (int)marrowLanceHeavy.damage, (int)marrowLanceHeavy.splashDamage);
    }

    /** 往一个 {@link ItemTurret} 的弹药表里追加一种弹药；已经有就跳过。返回是否真的加上了。 */
    private static boolean addAmmo(mindustry.world.Block b, Item item, BulletType type){
        if(!(b instanceof ItemTurret t) || t.ammoTypes == null) return false;
        if(t.ammoTypes.containsKey(item)) return false;
        t.ammoTypes.put(item, type);
        //the turret must be able to hold at least one shot of it
        t.maxAmmo = Math.max(t.maxAmmo, 8);
        return true;
    }
}
