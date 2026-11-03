package sixfold;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import mindustry.entities.Effect;

/**
 * v8.7 —— <b>炽霄泰坦攻击特效的 6.1 原版逐字回退</b>。
 *
 * <p>这一版的三个攻击特效是从 {@code Singularity-v6.1-AureliaRecovery} 的
 * {@code src/sixfold/EmberEffects.java} <b>一字不改</b>抄过来的：线条颜色、笔画数量、
 * 长度、寿命、缓动全都保持 6.1 的原值，只是类名从 {@code EmberEffects} 改成了
 * {@code LegacyEmber}，以便和 v7.1 重做后的 pseudo-3D 版本
 * （{@link EmberEffects}）并存。
 *
 * <table border="1" cellpadding="3">
 *   <tr><th>6.1 名字</th><th>本类名字</th><th>用途</th></tr>
 *   <tr><td>{@code vent}</td><td>{@link #legacyVent}</td><td>炮口热辐射 / 破片命中</td></tr>
 *   <tr><td>{@code burst}</td><td>{@link #legacyBurst}</td><td>主炮炮弹命中</td></tr>
 *   <tr><td>{@code mote}</td><td>{@link #legacyMote}</td><td>余烬微粒（6.1 未被引用，一并保留）</td></tr>
 * </table>
 *
 * <p><b>只有炽霄泰坦用这一套。</b>v3.2 的六门炮塔、天罗、陨坑、井架、织机、
 * 以及 {@link EmberCrownAbility}、{@link TetherBulletType} 继续用 v7.1 之后的
 * pseudo-3D 版本 —— 那些内容从 6.1 之后已经重做过，不在这次回退范围内。
 *
 * <p>机身周围的特效不在这里：<b>v8.7.2 已经把 8.7 新加的机身特效层整个删掉</b>，
 * 炽霄泰坦回到最初的裸机体版本（余烬观感由模型/精灵本身提供）。
 */
public class LegacyEmber{
    private static final Color ember = Color.valueOf("ff9e5c");
    private static final Color magma = Color.valueOf("e05545");
    private static final Color bone = Color.valueOf("f2e6d8");
    private static final Color steel = Color.valueOf("9ecbff");

    /** Short searing vent: three radiating heat strokes. */
    public static final Effect legacyVent = new Effect(12f, 70f, e -> {
        float f = e.fin();
        Draw.color(magma, ember, f);
        Lines.stroke(2f * e.fout() + 0.4f);
        for(int i = 0; i < 3; i++){
            Lines.lineAngle(e.x, e.y, e.rotation + i * 120f + f * 20f, 4f + f * 11f);
        }
        Draw.reset();
    });

    /** Artillery impact: one expanding ring with four magma spokes. */
    public static final Effect legacyBurst = new Effect(26f, 170f, e -> {
        float f = e.fin();
        Draw.color(ember, magma, f);
        Lines.stroke(3f * e.fout() + 0.4f);
        Lines.circle(e.x, e.y, f * 38f);
        Lines.stroke(2.2f * e.fout() + 0.3f);
        for(int i = 0; i < 4; i++){
            Lines.lineAngle(e.x, e.y, i * 90f + e.rotation, 5f + f * 24f);
        }
        Draw.reset();
    });

    /** Fading ember mote (frag impact). */
    public static final Effect legacyMote = new Effect(14f, 50f, e -> {
        float f = e.fin();
        Draw.color(ember, magma, f);
        Draw.alpha(e.fout());
        Lines.stroke(1.4f * e.fout() + 0.3f);
        Lines.circle(e.x, e.y, 1.5f + f * 4f);
        Draw.reset();
    });

    /** Gravity well tether pulse: single steel ring. */
    public static final Effect legacyTetherRing = new Effect(18f, 120f, e -> {
        float f = e.fin();
        Draw.color(steel);
        Draw.alpha(0.7f * e.fout());
        Lines.stroke(1.6f * e.fout() + 0.3f);
        Lines.circle(e.x, e.y, e.rotation * 80f * (0.4f + 0.6f * f));
        Draw.reset();
    });

    private LegacyEmber(){}
}
