package blackhole;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;

/**
 * 拟真 3D 黑洞渲染器 —— 基于 Gargantua（《星际穿越》）视觉模型的实时近似。
 *
 * 渲染分层（由远及近）：
 *  1. 背景引力透镜环（被拉伸的星光弧）
 *  2. 吸积盘「后半部分」—— 透视压扁的椭圆环
 *  3. 被引力弯折到上方的「顶部透镜像」（黑洞背面盘面绕过上方出现）
 *  4. 事件视界黑球 + 光子环（Photon Sphere）
 *  5. 吸积盘「前半部分」—— 覆盖在黑球下缘之上
 *  6. 被弯折到下方的「底部透镜像」
 *
 * 另外实现了相对论多普勒集束（Doppler beaming）：盘面朝向观察者一侧显著更亮更蓝，
 * 远离一侧更暗更红；以及吸积盘的差速湍流（内圈转得更快）。
 */
public class BlackHoleRenderer61{
    /** 吸积盘内圈（最热）颜色 */
    public static Color hot = Color.valueOf("fff6e0");
    /** 吸积盘中圈颜色 */
    public static Color mid = Color.valueOf("ffb14e");
    /** 吸积盘外圈（最冷）颜色 */
    public static Color cold = Color.valueOf("ff5a2b");

    private static final Color c1 = new Color(), c2 = new Color();
    private static final int segments = 80;
    /** 吸积盘径向细分数，越高越平滑 */
    private static final int diskRings = 14;
    /** 透镜像径向细分数 */
    private static final int lensRings = 8;

    /**
     * 绘制一个完整的 3D 黑洞。
     * @param x,y      中心
     * @param radius   事件视界半径（像素）
     * @param tilt     观察倾角 0（正面看，盘为圆）~ 1（侧面看，盘为线）
     * @param time     动画时间，用于盘面旋转
     * @param alpha    总体透明度
     */
    public static void draw(float x, float y, float radius, float tilt, float time, float alpha){
        if(alpha <= 0.001f || radius <= 0.01f) return;

        float squash = Mathf.lerp(1f, 0.16f, Mathf.clamp(tilt));
        float rIn = radius * 2.05f;
        float rOut = radius * 4.6f;
        float spin = time * 1.35f;
        // 盘厚度随黑洞尺寸缩放
        diskScale = radius * 0.5f;

        Blending pre = Draw.getBlend();
        Draw.blend(Blending.additive);

        // 1. 背景引力透镜：被拉伸的光弧
        lensHalo(x, y, radius, time, alpha * 0.5f);

        // 2. 吸积盘后半（远离观察者的一侧，在黑球之后）
        disk(x, y, rIn, rOut, squash, spin, alpha, true);

        // 3. 顶部透镜像：黑洞背面的盘被引力弯折，从上方绕出来
        lensArc(x, y, radius, rIn, rOut, spin, alpha * 0.85f, 1f);

        Draw.blend(pre);

        // 4. 事件视界：纯黑球体（非叠加混合，真正遮挡后方）
        Draw.color(Color.black);
        Fill.circle(x, y, radius);
        // 视界边缘的轻微暗晕，模拟光线逃逸减弱
        Draw.color(0f, 0f, 0f, 0.55f * alpha);
        Fill.circle(x, y, radius * 1.22f);

        Draw.blend(Blending.additive);

        // 光子环 —— 黑洞最标志性的亮环（r ≈ 1.5 Rs）。
        // 用多层递减的细线叠加出柔和的光晕，而不是一两道硬边圆。
        float ph = radius * 1.5f;
        float flick = 0.88f + Mathf.absin(time, 9f, 0.12f);
        int glowLayers = 7;
        for(int i = 0; i < glowLayers; i++){
            float f = i / (float)(glowLayers - 1);
            float rr = ph * Mathf.lerp(0.985f, 1.16f, f);
            float a = alpha * flick * Mathf.pow(1f - f, 2.2f) * 0.75f;
            c1.set(HOT_REF).lerp(mid, f * 0.85f);
            Draw.color(c1, a);
            Lines.stroke(Math.max(0.35f, radius * (0.05f + f * 0.07f)));
            Lines.circle(x, y, rr);
        }
        // 最内侧一道锐利的高光
        Draw.color(Color.white, alpha * flick * 0.55f);
        Lines.stroke(Math.max(0.3f, radius * 0.035f));
        Lines.circle(x, y, ph * 0.99f);

        // 5. 吸积盘前半（挡在黑球之前）
        disk(x, y, rIn, rOut, squash, spin, alpha, false);

        // 6. 底部透镜像
        lensArc(x, y, radius, rIn, rOut, spin, alpha * 0.55f, -1f);

        Draw.blend(pre);
        Draw.reset();
    }

    /** 吸积盘的一半。back=true 绘制远端（y 偏上），false 绘制近端（y 偏下）。 */
    private static void disk(float x, float y, float rIn, float rOut, float squash, float spin, float alpha, boolean back){
        for(int ring = 0; ring < diskRings; ring++){
            float f0 = ring / (float)diskRings, f1 = (ring + 1) / (float)diskRings;
            // 径向按指数分布：内圈更密，符合真实吸积盘的亮度梯度
            float r0 = Mathf.lerp(rIn, rOut, f0 * f0 * 0.55f + f0 * 0.45f);
            float r1 = Mathf.lerp(rIn, rOut, f1 * f1 * 0.55f + f1 * 0.45f);
            // 开普勒差速：内圈角速度更高
            float sp0 = spin * Mathf.pow(rIn / r0, 1.5f) * 34f;
            float sp1 = spin * Mathf.pow(rIn / r1, 1.5f) * 34f;
            // 盘的上下表面厚度：内薄外厚，让边缘有体积感
            float t0 = thickness(f0), t1 = thickness(f1);

            for(int i = 0; i < segments; i++){
                float a0 = i * 360f / segments, a1 = (i + 1) * 360f / segments;
                // 只画所需的半边（sin < 0 是近端/前方）
                float mida = (a0 + a1) / 2f;
                boolean isBack = Mathf.sinDeg(mida) >= 0f;
                if(isBack != back) continue;

                float b0 = brightness(a0, r0, rIn, rOut, sp0, alpha, f0);
                float b1 = brightness(a1, r0, rIn, rOut, sp0, alpha, f0);
                float b2 = brightness(a1, r1, rIn, rOut, sp1, alpha, f1);
                float b3 = brightness(a0, r1, rIn, rOut, sp1, alpha, f1);

                float col0 = tint(f0, b0), col1 = tint(f0, b1), col2 = tint(f1, b2), col3 = tint(f1, b3);

                // 主盘面
                Fill.quad(
                    px(x, a0, r0), py(y, a0, r0, squash), col0,
                    px(x, a1, r0), py(y, a1, r0, squash), col1,
                    px(x, a1, r1), py(y, a1, r1, squash), col2,
                    px(x, a0, r1), py(y, a0, r1, squash), col3
                );

                // 盘的垂直厚度（上下各一层半透明），让侧视时不是一条纸片
                if(t1 > 0.01f){
                    float dim0 = tint(f0, b0 * 0.42f), dim1 = tint(f0, b1 * 0.42f);
                    float dim2 = tint(f1, b2 * 0.42f), dim3 = tint(f1, b3 * 0.42f);
                    for(int s = -1; s <= 1; s += 2){
                        Fill.quad(
                            px(x, a0, r0), py(y, a0, r0, squash) + t0 * s, dim0,
                            px(x, a1, r0), py(y, a1, r0, squash) + t0 * s, dim1,
                            px(x, a1, r1), py(y, a1, r1, squash) + t1 * s, dim2,
                            px(x, a0, r1), py(y, a0, r1, squash) + t1 * s, dim3
                        );
                    }
                }
            }
        }
    }

    /** 吸积盘在半径 f(0~1) 处的垂直半厚度（像素比例，稍后乘上半径）。 */
    private static float thickness(float f){
        return diskScale * (0.06f + f * 0.34f);
    }

    private static float diskScale = 1f;
    private static final Color HOT_REF = Color.valueOf("fff6e0");

    /**
     * 引力弯折出的盘面镜像 —— Gargantua 最标志性的「光环绕过头顶」。
     *
     * 物理上这是黑洞背面（dir=+1）/正面下方（dir=-1）的吸积盘，光线绕过黑洞后
     * 在观察者视角里被抬到黑球的上方/下方，形成一条几乎不受倾角压扁的细带。
     * 这里用一条沿水平方向延展、在黑球正上方拱起的带状网格来近似，
     * 而不是同心圆（同心圆会看成一堆圈）。
     */
    private static void lensArc(float x, float y, float radius, float rIn, float rOut, float spin, float alpha, float dir){
        // 带的内外半径：紧贴光子环的一条窄带。做得窄+两侧渐隐，才不会看成一圈圈同心圆。
        float bandIn = radius * 1.40f;
        float bandOut = radius * 2.05f;
        float sp = spin * 46f;

        float start = dir > 0 ? 180f : 0f;
        float sweep = 180f;
        int segs = segments;

        for(int ring = 0; ring < lensRings; ring++){
            float f0 = ring / (float)lensRings, f1 = (ring + 1) / (float)lensRings;
            float r0 = Mathf.lerp(bandIn, bandOut, f0);
            float r1 = Mathf.lerp(bandIn, bandOut, f1);
            // 径向剖面：中间最亮，内外两侧都渐隐到 0 —— 关键，否则每层边界都可见
            float p0 = profile(f0), p1 = profile(f1);

            for(int i = 0; i < segs; i++){
                float a0 = start + i * sweep / segs;
                float a1 = start + (i + 1) * sweep / segs;
                float mida = (a0 + a1) / 2f;

                // 两端（水平方向）渐隐，与主盘平滑衔接
                float fall = Mathf.pow(Math.abs(Mathf.sinDeg(mida)), 0.5f);

                // 镜像采样盘的另一侧，多普勒方向相反
                float ma0 = a0 + 180f, ma1 = a1 + 180f;
                float b0 = brightness(ma0, rIn * 1.05f, rIn, rOut, sp, alpha, 0.05f) * fall * p0 * 1.45f;
                float b1 = brightness(ma1, rIn * 1.05f, rIn, rOut, sp, alpha, 0.05f) * fall * p0 * 1.45f;
                float b2 = brightness(ma1, rIn * 1.35f, rIn, rOut, sp, alpha, 0.25f) * fall * p1 * 1.45f;
                float b3 = brightness(ma0, rIn * 1.35f, rIn, rOut, sp, alpha, 0.25f) * fall * p1 * 1.45f;

                float col0 = tint(f0 * 0.35f, b0), col1 = tint(f0 * 0.35f, b1);
                float col2 = tint(0.15f + f1 * 0.45f, b2), col3 = tint(0.15f + f1 * 0.45f, b3);

                Fill.quad(
                    px(x, a0, r0), py(y, a0, r0, 1f), col0,
                    px(x, a1, r0), py(y, a1, r0, 1f), col1,
                    px(x, a1, r1), py(y, a1, r1, 1f), col2,
                    px(x, a0, r1), py(y, a0, r1, 1f), col3
                );
            }
        }
    }

    /** 径向渐隐剖面：f=0 与 f=1 处为 0，中部峰值 1。 */
    private static float profile(float f){
        return Mathf.pow(Mathf.sinDeg(Mathf.clamp(f) * 180f), 0.85f);
    }

    /** 背景星光被透镜拉长成的爱因斯坦弧。 */
    private static void lensHalo(float x, float y, float radius, float time, float alpha){
        int n = 5;
        for(int i = 0; i < n; i++){
            float a = (i * 360f / n) + time * 0.35f;
            float r = radius * Mathf.lerp(5.2f, 7.4f, (i * 0.37f) % 1f);
            Draw.color(c1.set(mid).lerp(Color.white, 0.35f), alpha * 0.22f * (0.6f + Mathf.absin(time + i * 13f, 22f, 0.4f)));
            Lines.stroke(radius * 0.06f);
            Lines.arc(x, y, r, 0.12f, a);
        }
    }

    private static float px(float x, float angle, float r){
        return x + Mathf.cosDeg(angle) * r;
    }

    private static float py(float y, float angle, float r, float squash){
        return y + Mathf.sinDeg(angle) * r * squash;
    }

    /** 多普勒集束 + 湍流亮度（不含径向分段，供镜像复用）。 */
    private static float brightness(float angle, float r, float rIn, float rOut, float spin, float alpha){
        return brightness(angle, r, rIn, rOut, spin, alpha, Mathf.clamp((r - rIn) / Math.max(0.001f, rOut - rIn)));
    }

    /**
     * 相对论多普勒集束 + 引力红移 + 湍流。
     * @param f 径向归一化位置（0 内 → 1 外）
     */
    private static float brightness(float angle, float r, float rIn, float rOut, float spin, float alpha, float f){
        // 多普勒集束：angle≈180° 一侧的物质朝观察者高速运动，亮度可差一个数量级。
        // 用 (1 - cos) 做基，指数拉开对比。
        float approach = 0.5f - 0.5f * Mathf.cosDeg(angle);
        float doppler = 0.10f + 2.35f * Mathf.pow(approach, 2.1f);
        // 内圈速度更快 → 集束更极端；外圈趋于均匀
        doppler = Mathf.lerp(doppler, 0.55f + doppler * 0.45f, f);

        // 径向：内侧最热最亮，外侧迅速变暗（引力红移 + 温度梯度）
        float radial = Mathf.pow(1f - f, 1.35f) * 1.15f + 0.08f;

        // 湍流条纹：多个不同频率的螺旋叠加，避免规则感
        float turb =
            0.70f
            + 0.18f * Mathf.sinDeg(angle * 3f + spin)
            + 0.12f * Mathf.sinDeg(angle * 7f - spin * 0.6f + f * 140f)
            + 0.08f * Mathf.sinDeg(angle * 13f + spin * 1.7f);

        return Mathf.clamp(doppler * radial * turb * 1.25f) * alpha;
    }

    /** 由径向位置 f(0 内→1 外) 与亮度取得打包颜色。 */
    private static float tint(float f, float bright){
        if(f < 0.5f){
            c2.set(hot).lerp(mid, f * 2f);
        }else{
            c2.set(mid).lerp(cold, (f - 0.5f) * 2f);
        }
        c2.a = Mathf.clamp(bright);
        // 高亮处向白色偏移，模拟蓝移
        if(bright > 0.62f) c2.lerp(Color.white, (bright - 0.62f) * 0.9f);
        c2.a = Mathf.clamp(bright);
        return c2.toFloatBits();
    }
}
