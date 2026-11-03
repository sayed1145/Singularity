package blackhole;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.*;
import mindustry.graphics.Layer;

/**
 * v8.7 —— <b>裂变特效</b>。
 *
 * <p>工业废料弹的命中特效（见 {@link WasteAmmo}）。裂变不是爆炸：它没有火球，
 * 只有一次白热的核闪、两层先后扩散的壳层环、八根从爆心甩出去的中子辐条，
 * 以及几块被甩飞的废渣。
 *
 * <p>和本模组其它特效一样，全部用几何图元画出来，不引用任何原版 {@code Fx}。
 *
 * <p><b>v8.7.3 —— 把"发光"收干净</b>：星环一次齐射 15 发（3 连发 × 装填倍数 5），
 * 原来的枪口特效是<b>加色</b>的白热圆斑、生命周期 60 帧，命中特效还有一层
 * 白热核闪 —— 一堆加色亮斑叠在炮口和战场上，亮到看不清地图、糊住界面。
 * <ul>
 *   <li>枪口特效：改成<b>普通混合</b>的小烟团，生命周期缩到 8 帧；</li>
 *   <li>命中特效：去掉白热核闪（改成柔和的暖色余辉），生命周期 190 → 40 帧；</li>
 *   <li>两个特效都显式钉在 {@link Layer#effect}（地图层）上，绝不画到 UI 层；</li>
 *   <li>弹道拖尾缩短，轨迹不再糊成一片。</li>
 * </ul>
 */
public final class FissionFx{
    private static final Color hot = Color.valueOf("fff2c8");
    private static final Color shell = Color.valueOf("c9b6ff");
    private static final Color waste = Color.valueOf("6b6a74");

    /** 裂变命中：余辉 + 双壳层环 + 八根辐条 + 碎渣（v8.7.3：去掉了白热核闪） */
    public static final Effect fission = new Effect(24f, 40f, e -> {
        float f = e.fin();

        //v8.7.3: 原本这里是加色的白热核闪（OwnFx.flash）—— 15 发齐射时叠成一团亮斑，
        //现在改成普通混合的暖色余辉：有冲击感，但不会把地图和界面照花。
        Draw.color(hot, shell, f);
        Draw.alpha(0.42f * e.fout());
        Fill.circle(e.x, e.y, 6f + f * 9f);

        //the fast neutron shell
        Draw.color(shell, hot, f);
        Draw.alpha(0.75f * e.fout());
        OwnFx.ring(e.x, e.y, 3f + e.finpow() * 28f, 2.4f * e.fout() + 0.4f);

        //the slower shock ring
        Draw.alpha(0.38f * e.fout());
        OwnFx.ring(e.x, e.y, 3f + e.finpow() * 40f, 1.4f * e.fout());

        //eight fission spokes thrown out of the core
        Draw.color(hot, shell, f);
        Draw.alpha(0.6f * e.fout());
        for(int i = 0; i < 8; i++){
            float a = i * 45f + e.rotation;
            float len = 7f + e.finpow() * 20f;
            Lines.stroke(1.8f * e.fout() + 0.3f);
            Lines.line(e.x + Angles.trnsx(a, len * 0.3f), e.y + Angles.trnsy(a, len * 0.3f),
                e.x + Angles.trnsx(a, len), e.y + Angles.trnsy(a, len));
        }

        //chunks of slag thrown out with it
        OwnFx.debris(e, 5, 20f, 1.2f, waste);

        Draw.reset();
    }).layer(Layer.effect);

    /** 裂变弹的枪口效果：一小团冷掉的壳层雾（v8.7.3：普通混合，寿命 8 帧，不再加色爆亮） */
    public static final Effect fissionMuzzle = new Effect(6f, 8f, e -> {
        float f = e.fin();
        Draw.color(shell, hot, f);
        Draw.alpha(0.34f * e.fout());
        Fill.circle(e.x, e.y, 2.5f + f * 4.5f);
        Draw.reset();
    }).layer(Layer.effect);

    private FissionFx(){}
}
