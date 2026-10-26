package blackhole;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.Effect;

/**
 * v8.5.5：星髓石炮弹专用的一整套特效 —— <b>从零手写，没有一处是拿现成特效改的</b>。
 *
 * <p>造型语言取自星髓石本身的设定（恒星残骸：冷却了几十亿年的暗壳，剖开处露出惨白的星髓）：
 * <b>惨白的芯 + 冷紫的外壳 + 一圈正在塌陷的暗环</b>。整套特效共用三个颜色常量
 * （{@link #core} / {@link #shell} / {@link #dark}），所以不管是枪口、弹道还是落点，
 * 一眼就能认出是同一种弹药。
 *
 * <p>所有半径都是<b>写死的常数</b>，不会像旧的那套那样把 {@code e.rotation}（0..360 的朝向角）
 * 当成尺寸来用 —— 那正是晶格蠕者的爆炸"缩了 1/3 还是一大团"的原因。
 *
 * <table border="1" cellpadding="3">
 *   <caption>峰值半径（像素；8 像素 = 1 格）</caption>
 *   <tr><th>特效</th><th>用途</th><th>峰值半径</th></tr>
 *   <tr><td>{@link #shoot}</td><td>炮口</td><td>11</td></tr>
 *   <tr><td>{@link #wake}</td><td>弹道尾迹</td><td>5</td></tr>
 *   <tr><td>{@link #hit}</td><td>命中</td><td><b>32</b></td></tr>
 *   <tr><td>{@link #despawn}</td><td>射程尽头消散</td><td>18</td></tr>
 *   <tr><td>{@link #brand}</td><td>灼印持续期间（在单位身上）</td><td>7</td></tr>
 *   <tr><td>{@link #brandApply}</td><td>灼印附上的一瞬间</td><td>14</td></tr>
 * </table>
 */
public final class MarrowFx{
    private MarrowFx(){}

    /** 星髓：惨白，带一点暖调。 */
    public static final Color core = Color.valueOf("fff4e2");
    /** 外壳：冷却的恒星残骸，冷紫。 */
    public static final Color shell = Color.valueOf("b58cff");
    /** 塌陷的暗环。 */
    public static final Color dark = Color.valueOf("40323f");

    private static final Rand rand = new Rand();

    /** 画一个 n 角星：n 条从中心射出的线，长度 len，线宽 w，整体转 rot 度。 */
    private static void spikes(float x, float y, int n, float len, float w, float rot){
        Lines.stroke(w);
        for(int i = 0; i < n; i++){
            float a = rot + i * (360f / n);
            Lines.lineAngle(x, y, a, len);
        }
    }

    /**
     * 炮口：一枚六角星爆闪，两条反向自转的环，外加一圈被推开的暗壳。
     *
     * <p>星角从 9 像素迅速收回到 2 像素（"塌陷"的感觉就来自这里），外环 4 → 11，
     * 内环 11 → 3（反向收缩）。峰值半径 <b>11</b> 像素。
     */
    public static final Effect shoot = new Effect(16f, 26f, e -> {
        Draw.blend(Blending.additive);
        //the star collapses: 9 -> 2
        Draw.color(core, e.fout());
        spikes(e.x, e.y, 6, 2f + 7f * e.fout(), 1.5f * e.fout() + 0.3f, e.rotation);
        Draw.color(shell, e.fout() * 0.9f);
        spikes(e.x, e.y, 6, 1.2f + 4.4f * e.fout(), 0.9f * e.fout(), e.rotation + 30f);
        Draw.color(Color.white, e.fout());
        Fill.circle(e.x, e.y, 1.9f * e.fout());
        //outer ring expanding 4 -> 11
        Draw.color(shell, e.fout() * 0.8f);
        Lines.stroke(1.2f * e.fout());
        Lines.circle(e.x, e.y, 4f + e.finpow() * 7f);
        //inner ring collapsing 11 -> 3
        Draw.color(core, e.fout() * 0.6f);
        Lines.stroke(0.9f * e.fout());
        Lines.circle(e.x, e.y, 11f - e.finpow() * 8f);
        Draw.blend();
        Draw.reset();
    });

    /** 弹道尾迹：一粒惨白的余烬，拖一小团暗壳的碎屑。峰值半径 5 像素。 */
    public static final Effect wake = new Effect(14f, 12f, e -> {
        rand.setSeed(e.id);
        Draw.color(dark, e.fout() * 0.5f);
        Fill.circle(e.x, e.y, 1.2f + e.fin() * 2.6f);
        Draw.blend(Blending.additive);
        Draw.color(core, e.fout() * 0.8f);
        Fill.circle(e.x, e.y, 1.0f * e.fout());
        Draw.color(shell, e.fout() * 0.5f);
        for(int i = 0; i < 2; i++){
            float a = rand.range(180f), d = rand.random(1f, 3.4f) * e.fin();
            Fill.circle(e.x + Angles.trnsx(a, d), e.y + Angles.trnsy(a, d), 0.35f * e.fout());
        }
        Draw.blend();
        Draw.reset();
    });

    /**
     * 命中：八芒星爆开 + 双层冲击环 + 十二粒碎壳 + 一圈留在地上的暗痕。
     *
     * <p>峰值半径 <b>32</b> 像素（4 格）：主环扩到 28，碎壳最远 30（半径 1.6）。
     * 星角 24 → 3，暗痕从 6 慢慢扩到 22 并淡出 —— 这是"星髓石炸开之后留下的焦壳"。
     */
    public static final Effect hit = new Effect(30f, 72f, e -> {
        float f = e.fout(), fin = e.fin();

        //the lingering dark scar left on the ground: 6 -> 22, fading out
        Draw.color(dark, f * 0.85f);
        Fill.circle(e.x, e.y, 6f + e.finpow() * 16f);

        //main shock ring 5 -> 28
        Draw.color(shell, f);
        Lines.stroke(2.2f * f);
        Lines.circle(e.x, e.y, 5f + e.finpow() * 23f);
        //second, faster ring
        Draw.color(core, f * 0.7f);
        Lines.stroke(1.1f * f);
        Lines.circle(e.x, e.y, 3f + Mathf.clamp(fin * 1.6f) * 20f);

        //eight-point burst, 24 -> 3
        Draw.blend(Blending.additive);
        Draw.color(core, f);
        spikes(e.x, e.y, 8, 3f + 21f * f, 2.4f * f + 0.3f, e.rotation);
        Draw.color(Color.white, f * 0.9f);
        Fill.circle(e.x, e.y, 3.4f * f);
        Draw.blend();

        //12 shards of shell, no further than 30
        Draw.color(shell, f);
        Angles.randLenVectors(e.id, 12, 8f + fin * 22f,
            (x, y) -> Fill.circle(e.x + x, e.y + y, 1.6f * f));
        Draw.reset();
    });

    /** 射程尽头消散：只放半个爆闪，没有冲击环。峰值半径 18 像素。 */
    public static final Effect despawn = new Effect(18f, 40f, e -> {
        Draw.color(dark, e.fout() * 0.6f);
        Fill.circle(e.x, e.y, 4f + e.finpow() * 10f);
        Draw.blend(Blending.additive);
        Draw.color(core, e.fout() * 0.8f);
        spikes(e.x, e.y, 6, 2f + 11f * e.fout(), 1.4f * e.fout() + 0.2f, e.rotation);
        Fill.circle(e.x, e.y, 2f * e.fout());
        Draw.blend();
        Draw.reset();
    });

    /**
     * 灼印持续期间在单位身上冒的火星 + 一圈缓慢脉动的环。
     *
     * <p>由 {@code StatusEffect.effect} 按 {@code effectChance} 随机播放，
     * 峰值半径 <b>7</b> 像素 —— 小到不会糊住单位本身，但一眼能看出"这只被烙上了"。
     */
    public static final Effect brand = new Effect(26f, 20f, e -> {
        rand.setSeed(e.id);
        Draw.blend(Blending.additive);
        Draw.color(shell, e.fout() * 0.9f);
        Lines.stroke(1.0f * e.fout());
        Lines.circle(e.x, e.y, 2f + e.fin() * 4.5f);
        Draw.color(core, e.fout());
        for(int i = 0; i < 3; i++){
            float a = 90f + rand.range(60f), d = rand.random(1f, 5.5f) * e.fin();
            Fill.circle(e.x + Angles.trnsx(a, d), e.y + Angles.trnsy(a, d), 0.42f * e.fout());
        }
        Draw.blend();
        Draw.reset();
    });

    /** 灼印附上的一瞬间：一圈收紧的环 + 四角星。峰值半径 14 像素。 */
    public static final Effect brandApply = new Effect(14f, 32f, e -> {
        Draw.color(shell, e.fout());
        Lines.stroke(1.6f * e.fout());
        Lines.circle(e.x, e.y, 13f - e.finpow() * 9f);
        Draw.blend(Blending.additive);
        Draw.color(core, e.fout());
        spikes(e.x, e.y, 4, 2f + 9f * e.fout(), 1.3f * e.fout() + 0.2f, e.rotation);
        Draw.blend();
        Draw.reset();
    });
}
