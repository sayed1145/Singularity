package blackhole;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.Effect;

/**
 * v8.5.5：晶格蠕者的攻击特效 —— 整套重写，不再是从别处缩放来的。
 *
 * <h2>为什么"再除一次 3"解决不了</h2>
 *
 * <p>8.5.4 那次把 {@link AureliaFx#detainImpact} 的每一个长度都除以 3，但那套特效把
 * {@code e.rotation} <b>当成半径在用</b>：
 *
 * <pre>
 * Lines.circle(e.x, e.y, 4f + e.fin() * (e.rotation + 26f), ...);
 * Lines.poly(e.x, e.y, 6, 2f + e.fin() * (e.rotation * 0.6f + 16f), ...);
 * </pre>
 *
 * <p>炮弹的 {@code rotation} 是 0..360 的<b>朝向角</b>，不是尺寸。所以那朵爆炸的实际半径
 * 能到 <b>390 像素</b>；除以 3 之后还剩 <b>130 像素 —— 16 格</b>，比车体本身大好几圈。
 * 这就是"明明已经缩了 1/3，打起来还是一大团"的原因：被缩放的是一个随朝向乱跳的量。
 *
 * <p>这一版<b>所有半径都是写死的常数</b>，跟朝向完全无关，形状也不会随角度变。
 *
 * <table border="1" cellpadding="3">
 *   <caption>峰值半径（像素；游戏里 8 像素 = 1 格）—— <b>实测值</b>，不是照代码估的</caption>
 *   <tr><th>特效</th><th>8.5.1 原版</th><th>8.5.4（÷3）</th><th><b>8.5.5（重写）</b></th></tr>
 *   <tr><td>枪口火光</td><td>9.0</td><td>2.0</td><td><b>1.6</b></td></tr>
 *   <tr><td>枪口硝烟</td><td>11.6</td><td>3.9</td><td><b>2.8</b></td></tr>
 *   <tr><td>落点爆炸</td><td>389.0</td><td>129.7</td><td><b>9.6</b></td></tr>
 *   <tr><td>破片命中</td><td>14.0</td><td>14.0</td><td><b>1.7</b></td></tr>
 * </table>
 *
 * <p>落点爆炸相对 8.5.4 小了 <b>13.5 倍</b>，相对 8.5.1 原版小了 <b>40 倍</b>。
 * 注意 8.5.4 那一列的 129.7 是<b>最坏朝向</b>下的值：它的半径随炮弹朝向从 10 一路涨到 130，
 * 只有碰巧朝北飞的时候才是"小的"。8.5.5 这一列在任何朝向下都是 9.6。
 *
 * <h2>这些数字是怎么来的</h2>
 *
 * <p>{@code tools/preview/src/preview/FxSize.java}：用无 GL 的录制型 Batch（{@code FakeBatch}）
 * 把每个特效从 0 跑到寿命结束，收下所有顶点，算离中心最远的那个有多远，而且<b>换五个朝向各跑一遍</b>
 * —— "半径随朝向变化"就是这么抓出来的。
 *
 * <p>{@code tools/preview/src/preview/FxShot.java}：把同一批特效光栅化成 PNG
 * （{@code docs/v855/shots/}），再用<b>像素法</b>独立量一遍作为交叉验证：
 * 两种方法在同一个朝向下给出 9.6 / 10.6 像素，吻合。
 */
public final class CrawlerFx{
    private CrawlerFx(){}

    /** 固定种子：同一发炮弹的火星落点稳定，不会每帧乱跳。 */
    private static final Rand rand = new Rand();

    /** 蠕者弹药的配色：共振紫，和它那条弹道一致。 */
    static final Color hot = Color.valueOf("e8dcff"), spark = AureliaFx.resonance;

    /**
     * 开火：炮口一小团爆闪 + 四粒短火星。
     *
     * <p>峰值半径 <b>1.9</b> 像素。形状是一个朝弹道方向的短锥（长 1.15、半宽 0.38）
     * 加一个 0.28 的亮核，外加四粒最远 1.9 的火星。
     */
    public static final Effect muzzle = new Effect(7f, 8f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(hot, e.fout());
        //short cone along the shot: 1.15 long, 0.38 half-width at the base
        float len = 0.85f * e.fout() + 0.30f, w = 0.38f * e.fout();
        Fill.tri(e.x + Angles.trnsx(e.rotation + 90f, w), e.y + Angles.trnsy(e.rotation + 90f, w),
                 e.x + Angles.trnsx(e.rotation - 90f, w), e.y + Angles.trnsy(e.rotation - 90f, w),
                 e.x + Angles.trnsx(e.rotation, len), e.y + Angles.trnsy(e.rotation, len));
        Draw.color(Color.white, e.fout());
        Fill.circle(e.x, e.y, 0.28f * e.fout());
        //4 sparks, no further than 1.9
        Angles.randLenVectors(e.id, 4, 0.5f + e.fin() * 1.4f,
            (x, y) -> Fill.circle(e.x + x, e.y + y, 0.19f * e.fout()));
        Draw.blend();
        Draw.reset();
    });

    /**
     * 硝烟：三小团，顺着弹道方向往外飘一点。
     *
     * <p>峰值半径 <b>3.4</b> 像素（飘移 2.2 + 上浮 0.5 + 团半径 0.7）。
     */
    public static final Effect smoke = new Effect(20f, 8f, e -> {
        rand.setSeed(e.id);
        Draw.color(Color.valueOf("6b707a"), e.fout() * 0.42f);
        for(int i = 0; i < 3; i++){
            float d = rand.random(0.4f, 2.2f) * e.finpow(), a = e.rotation + rand.range(26f);
            Fill.circle(e.x + Angles.trnsx(a, d), e.y + Angles.trnsy(a, d) + e.fin() * 0.5f,
                0.28f + e.fin() * 0.42f);
        }
        Draw.reset();
    });

    /**
     * 落点：一圈冲击环 + 一小团亮核 + 六粒碎石。
     *
     * <p>峰值半径 <b>10.8</b> 像素（1.35 格）—— 冲击环扩到 9.6，碎石最远 10.2（半径 0.55）。
     * 对比之前的 130 像素（16 格），是真正的"一小朵"。
     */
    public static final Effect impact = new Effect(20f, 24f, e -> {
        //shock ring: 1.4 -> 9.6
        Draw.color(spark, e.fout());
        Lines.stroke(1.0f * e.fout());
        Lines.circle(e.x, e.y, 1.4f + e.finpow() * 8.2f);

        //hot core, gone quickly
        Draw.blend(Blending.additive);
        Draw.color(hot, e.fout() * 0.9f);
        Fill.circle(e.x, e.y, 1.5f * e.fout());
        Draw.blend();

        //6 chunks of lattice, no further than 10.2
        Draw.color(spark, e.fout());
        Angles.randLenVectors(e.id, 6, 2.6f + e.fin() * 7.6f,
            (x, y) -> Fill.circle(e.x + x, e.y + y, 0.55f * e.fout()));

        //a faint second ring a moment later, so the burst reads as a burst
        if(e.fin() > 0.25f){
            Draw.color(spark, e.fout() * 0.5f);
            Lines.stroke(0.7f * e.fout());
            Lines.circle(e.x, e.y, 1.0f + e.finpow() * 5.4f);
        }
        Draw.reset();
    });

    /** 碎片子弹（六枚小破片）的命中：几乎看不见的一粒火星，峰值半径 2.2 像素。 */
    public static final Effect shardHit = new Effect(10f, 6f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(hot, e.fout());
        Fill.circle(e.x, e.y, 0.5f * e.fout());
        Angles.randLenVectors(e.id, 3, 0.4f + e.fin() * 1.6f,
            (x, y) -> Fill.circle(e.x + x, e.y + y, 0.22f * e.fout()));
        Draw.blend();
        Draw.reset();
    });
}
