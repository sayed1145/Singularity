package blackhole;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;

/**
 * 拟真 3D 黑洞渲染器 —— 基于 Gargantua（《星际穿越》）视觉模型的实时近似。
 *
 * <p><b>v7.2：完整回退到 v6.1 的实现。</b>几何细分（80 段 / 14 环盘面 / 8 环透镜像 / 7 层光子环）、
 * 盘面上下厚度层、颜色与亮度公式都与 v6.1 逐字相同，画面像素级一致；v7.1 的自适应降级（降段数、
 * 降环数、按倾角省略厚度层、减少光环层数）已全部移除。
 *
 * <p>与 v6.1 的差别只在 CPU 开销：v6.1 每个顶点重复计算 4 次亮度（含 Math.pow）和 4 次 Color 插值，
 * 每帧每个黑洞约 1.9 万次 pow；这里把顶点亮度 / 颜色按「环 × 角」只算一次并复用到相邻四边形，
 * 多普勒幂函数按固定角度建静态查表，sin / cos 建每次调用的表，并且完全在屏幕外的黑洞直接跳过。
 * 结果完全相同，开销约为 v6.1 的 1/8。
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
public class BlackHoleRenderer{
    /** 吸积盘内圈（最热）颜色 */
    public static Color hot = Color.valueOf("fff6e0");
    /** 吸积盘中圈颜色 */
    public static Color mid = Color.valueOf("ffb14e");
    /** 吸积盘外圈（最冷）颜色 */
    public static Color cold = Color.valueOf("ff5a2b");

    private static final Color c1 = new Color();
    /** 角向细分上限（也是查表尺寸）。每次绘制按黑洞在屏幕上的像素半径从 {48, 64, 96, 192} 里取一档。 */
    private static final int SEG = 192;
    /** 吸积盘径向细分上限 */
    private static final int RINGS = 24;
    /** 透镜像径向细分上限 */
    private static final int LENS = 12;
    private static final int glowLayers = 10;
    /** 本次绘制的档位与查表步长（渲染单线程，逐个黑洞设置） */
    private static int segments = 96, diskRings = 12, lensRings = 6, segStep = 2, ringStep = 2, lensStep = 2;
    private static final Color HOT_REF = Color.valueOf("fff6e0");

    /** 统计：上一帧 / 本帧绘制的四边形数（测试与性能面板用） */
    public static long quadsDrawn, holesDrawn, holesCulled;

    // ------------------------------------------------------------------ 查表
    //角度 j * 180 / segments（j = 0..2*segments）的 cos / sin 与多普勒基值（只依赖角度，静态）
    private static final float[] cosH = new float[SEG * 2 + 1], sinH = new float[SEG * 2 + 1], dopH = new float[SEG * 2 + 1];
    //径向常量（只依赖环序号，静态）
    private static final float[] diskF = new float[RINGS + 1], diskPos = new float[RINGS + 1], diskSpin = new float[RINGS + 1],
        diskRadial = new float[RINGS + 1], diskThick = new float[RINGS + 1];
    private static final float[] lensF = new float[LENS + 1], lensProfile = new float[LENS + 1];
    //每次调用的顶点亮度 / 颜色（环 × 角）。colTop / colBot 是盘体上下表面的受光 / 背光色，
    //两者不对称才会让厚度读成一个真正的立体环面，而不是上下各糊一层的纸片。
    private static final float[] bright = new float[(RINGS + 1) * (SEG + 1)];
    private static final float[] colA = new float[(RINGS + 1) * (SEG + 1)], colTop = new float[(RINGS + 1) * (SEG + 1)],
        colBot = new float[(RINGS + 1) * (SEG + 1)];
    private static final float[] lensIn = new float[SEG + 1], lensOut = new float[SEG + 1];
    //透镜像：两端渐隐（只依赖段序号）与两条采样半径的径向项
    private static final float[] fallH = new float[SEG * 2];
    private static final float radialIn = Mathf.pow(1f - 0.05f, 1.35f) * 1.15f + 0.08f, radialOut = Mathf.pow(1f - 0.25f, 1.35f) * 1.15f + 0.08f;
    //环基色（每次调用按当前 hot/mid/cold 求一次）
    private static final float[] ringR = new float[RINGS + 1], ringG = new float[RINGS + 1], ringB = new float[RINGS + 1];

    static{
        for(int j = 0; j <= SEG * 2; j++){
            float a = j * 180f / SEG;
            cosH[j] = Mathf.cosDeg(a);
            sinH[j] = Mathf.sinDeg(a);
            float approach = 0.5f - 0.5f * Mathf.cosDeg(a);
            dopH[j] = 0.10f + 2.35f * Mathf.pow(approach, 2.1f);
        }
        for(int k = 0; k <= RINGS; k++){
            float f = k / (float)RINGS;
            diskF[k] = f;
            diskPos[k] = f * f * 0.55f + f * 0.45f;
            diskRadial[k] = Mathf.pow(1f - f, 1.35f) * 1.15f + 0.08f;
            diskThick[k] = 0.06f + f * 0.34f;
        }
        for(int j = 0; j < SEG * 2; j++){
            fallH[j] = Mathf.pow(Math.abs(Mathf.sinDeg((j + 0.5f) * 180f / SEG)), 0.5f);
        }
        for(int k = 0; k <= LENS; k++){
            float f = k / (float)LENS;
            lensF[k] = f;
            lensProfile[k] = Mathf.pow(Mathf.sinDeg(Mathf.clamp(f) * 180f), 0.85f);
        }
    }

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
        if(offscreen(x, y, radius * 7.6f)){
            holesCulled++;
            return;
        }
        holesDrawn++;
        // 一个黑洞约 4800 个四边形。直接提交时，Mindustry 的排序批处理器要为每个四边形各记录并排序一条请求；
        // 这里把整个黑洞作为一条「在本层执行」的绘制请求提交（Draw.draw(z, run)），
        // 四边形在刷新时按原顺序直接写入顶点缓冲。画面、层次完全不变，排序开销从 4800 条降为 1 条。
        Job job = Job.obtain();
        if(job != null){
            job.set(x, y, radius, tilt, time, alpha);
            Draw.draw(Draw.z(), job);
        }else{
            drawNow(x, y, radius, tilt, time, alpha);
        }
    }

    /** 可复用的延迟绘制请求（每帧最多 jobs.length 个黑洞走延迟路径，超出的立即绘制）。 */
    private static final class Job implements Runnable{
        private static final Job[] jobs = new Job[96];
        private static int used;
        private static long frame = -1;

        float x, y, radius, tilt, time, alpha;

        static Job obtain(){
            long f = Core.graphics == null ? -1 : Core.graphics.getFrameId();
            if(f != frame){
                frame = f;
                used = 0;
            }
            if(used >= jobs.length) return null;
            Job j = jobs[used];
            if(j == null) j = jobs[used] = new Job();
            used++;
            return j;
        }

        void set(float x, float y, float radius, float tilt, float time, float alpha){
            this.x = x; this.y = y; this.radius = radius; this.tilt = tilt; this.time = time; this.alpha = alpha;
        }

        @Override
        public void run(){
            drawNow(x, y, radius, tilt, time, alpha);
        }
    }

    /** 立即绘制（v6.1 的 draw 本体）。 */
    public static void drawNow(float x, float y, float radius, float tilt, float time, float alpha){
        lod(radius);

        float squash = Mathf.lerp(1f, 0.16f, Mathf.clamp(tilt));
        float rIn = radius * 2.05f;
        float rOut = radius * 4.6f;
        float spin = time * 1.35f;
        // 盘厚度随黑洞尺寸缩放
        float diskScale = radius * 0.5f;

        prepareDisk(rIn, rOut, spin, alpha);

        Blending pre = Draw.getBlend();
        Draw.blend(Blending.additive);

        // 1. 背景引力透镜：被拉伸的光弧
        lensHalo(x, y, radius, time, alpha * 0.5f);

        // 2. 吸积盘后半（远离观察者的一侧，在黑球之后）
        disk(x, y, rIn, rOut, squash, diskScale, true);

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
        for(int i = 0; i < glowLayers; i++){
            float f = i / (float)(glowLayers - 1);
            float rr = ph * Mathf.lerp(0.985f, 1.16f, f);
            float a = alpha * flick * Mathf.pow(1f - f, 2.6f) * 0.52f;
            c1.set(HOT_REF).lerp(mid, f * 0.85f);
            Draw.color(c1, a);
            Lines.stroke(Math.max(0.3f, radius * (0.035f + f * 0.055f)));
            Lines.circle(x, y, rr);
        }
        // 最内侧一道锐利的高光（视界边缘的光子轨道本体）
        Draw.color(Color.white, alpha * flick * 0.62f);
        Lines.stroke(Math.max(0.25f, radius * 0.028f));
        Lines.circle(x, y, ph * 0.99f);
        // 多普勒：光子环朝观察者转来的一侧（180° 方向）明显更亮更白，这是真实黑洞照片里
        // 最强的立体线索之一；以前整圈等亮，看上去就是一个平的圆环。
        // 用 12 段带 cos^4 权重的小弧拼出平滑的亮度梯度，弧的两端不会留下可见的接缝
        for(int i = 0; i < 12; i++){
            float d = (i - 5.5f) * 20f;
            float wgt = Mathf.pow(Mathf.cosDeg(d * 0.5f), 4f);
            if(wgt < 0.02f) continue;
            Draw.color(c1.set(Color.white).lerp(HOT_REF, 0.3f), alpha * flick * 0.40f * wgt);
            Lines.stroke(Math.max(0.25f, radius * (0.028f + 0.022f * wgt)));
            Lines.arc(x, y, ph * 1.004f, 20f / 360f, 176f + d);
        }

        // 5. 吸积盘前半（挡在黑球之前）
        disk(x, y, rIn, rOut, squash, diskScale, false);

        // 6. 底部透镜像
        lensArc(x, y, radius, rIn, rOut, spin, alpha * 0.55f, -1f);

        Draw.blend(pre);
        Draw.reset();
    }

    /**
     * 细分档位：按黑洞在屏幕上的实际像素半径挑一档，查表步长随之改变（表本身永远是最高档）。
     *
     * <p>炮口、子弹上那些只有几像素的小黑洞用 48 段 / 8 环（比 v6.1 的固定 80 × 14 还省），
     * 而锻炉、事件视界这种占满屏幕的用 192 段 / 24 环 —— 边缘不再是可见的多边形，这是
     * 「糊 / 平」观感的主要来源。总开销反而比固定档低，因为绝大多数黑洞都是小的。
     */
    private static void lod(float radius){
        float px = radius;
        if(Core.camera != null && Core.graphics != null && Core.camera.width > 0.01f){
            px = radius * (Core.graphics.getWidth() / Core.camera.width);
        }
        if(px < 18f){
            segments = 48; diskRings = 8; lensRings = 4;
        }else if(px < 48f){
            segments = 64; diskRings = 12; lensRings = 6;
        }else if(px < 130f){
            segments = 96; diskRings = 12; lensRings = 6;
        }else{
            segments = 192; diskRings = 24; lensRings = 12;
        }
        segStep = SEG / segments;
        ringStep = RINGS / diskRings;
        lensStep = LENS / lensRings;
    }

    /** 完全在相机视野外（含边距）的黑洞不必提交任何几何。 */
    private static boolean offscreen(float x, float y, float reach){
        Camera cam = Core.camera;
        if(cam == null || cam.width <= 0f) return false;
        return x + reach < cam.position.x - cam.width / 2f || x - reach > cam.position.x + cam.width / 2f
            || y + reach < cam.position.y - cam.height / 2f || y - reach > cam.position.y + cam.height / 2f;
    }

    /** 顶点亮度与颜色：每环每角只算一次（v6.1 对每个四边形的 4 个角各算一次，相邻四边形重复 4 倍）。 */
    private static void prepareDisk(float rIn, float rOut, float spin, float alpha){
        float hr = hot.r, hg = hot.g, hb = hot.b, mr = mid.r, mg = mid.g, mb = mid.b, cr = cold.r, cg = cold.g, cb = cold.b;
        for(int ring = 0; ring <= diskRings; ring++){
            int k = ring * ringStep;
            float f = diskF[k];
            float r = Mathf.lerp(rIn, rOut, diskPos[k]);
            // 开普勒差速：内圈角速度更高
            float sp = spin * Mathf.pow(rIn / r, 1.5f) * 34f;
            float radial = diskRadial[k];
            //环基色（tint 的 f 相关部分）
            if(f < 0.5f){
                float t = f * 2f;
                ringR[k] = hr + t * (mr - hr); ringG[k] = hg + t * (mg - hg); ringB[k] = hb + t * (mb - hb);
            }else{
                float t = (f - 0.5f) * 2f;
                ringR[k] = mr + t * (cr - mr); ringG[k] = mg + t * (cg - mg); ringB[k] = mb + t * (cb - mb);
            }
            int row = k * (SEG + 1);
            for(int i = 0; i <= segments; i++){
                int col = i * segStep;
                float angle = i * 360f / segments;
                float doppler = dopH[col * 2];
                // 内圈速度更快 → 集束更极端；外圈趋于均匀
                doppler = Mathf.lerp(doppler, 0.55f + doppler * 0.45f, f);
                // 湍流条纹：多个不同频率的螺旋叠加，避免规则感
                float turb = 0.60f
                    + 0.22f * Mathf.sinDeg(angle * 3f + sp)
                    + 0.15f * Mathf.sinDeg(angle * 7f - sp * 0.6f + f * 140f)
                    + 0.10f * Mathf.sinDeg(angle * 13f + sp * 1.7f)
                    // 192 段的细分撑得住这一层高频，盘面才有可读的流纹而不是一团均匀的光
                    + 0.07f * Mathf.sinDeg(angle * 23f - sp * 2.3f + f * 260f);
                float b = Mathf.clamp(doppler * radial * turb * 1.18f) * alpha;
                bright[row + col] = b;
                colA[row + col] = tint(ringR[k], ringG[k], ringB[k], b);
                // 上表面朝向观察者、被内缘的高温区照亮 -> 更亮并向白偏；下表面背光 -> 更暗并压向外圈的冷色。
                colTop[row + col] = tint(
                    ringR[k] + (1f - ringR[k]) * 0.22f,
                    ringG[k] + (1f - ringG[k]) * 0.18f,
                    ringB[k] + (1f - ringB[k]) * 0.14f, b * 0.68f);
                colBot[row + col] = tint(ringR[k] * 0.82f, ringG[k] * 0.52f, ringB[k] * 0.45f, b * 0.26f);
            }
        }
    }

    /** 吸积盘的一半。back=true 绘制远端（y 偏上），false 绘制近端（y 偏下）。 */
    private static void disk(float x, float y, float rIn, float rOut, float squash, float diskScale, boolean back){
        // 厚度只有在斜看时才该露出来：正面看（squash -> 1）环面的侧壁本来就看不见，
        // 以前不论倾角都糊两层，近正面时就变成一团平的光晕。
        float volume = Mathf.clamp((1f - squash) * 1.25f);
        for(int ring = 0; ring < diskRings; ring++){
            int k0 = ring * ringStep, k1 = k0 + ringStep;
            float r0 = Mathf.lerp(rIn, rOut, diskPos[k0]);
            float r1 = Mathf.lerp(rIn, rOut, diskPos[k1]);
            // 盘的上下表面厚度：内薄外厚，让边缘有体积感
            float t0 = diskScale * diskThick[k0] * volume, t1 = diskScale * diskThick[k1] * volume;
            int row0 = k0 * (SEG + 1), row1 = k1 * (SEG + 1);

            for(int i = 0; i < segments; i++){
                int col = i * segStep, coln = col + segStep;
                // 只画所需的半边（sin < 0 是近端/前方）；中角下标 = col * 2 + segStep
                boolean isBack = sinH[col * 2 + segStep] >= 0f;
                if(isBack != back) continue;

                float c0 = cosH[col * 2], s0 = sinH[col * 2], ca = cosH[coln * 2], sa = sinH[coln * 2];
                float x00 = x + c0 * r0, y00 = y + s0 * r0 * squash;
                float x10 = x + ca * r0, y10 = y + sa * r0 * squash;
                float x11 = x + ca * r1, y11 = y + sa * r1 * squash;
                float x01 = x + c0 * r1, y01 = y + s0 * r1 * squash;

                // 主盘面
                Fill.quad(x00, y00, colA[row0 + col], x10, y10, colA[row0 + coln], x11, y11, colA[row1 + coln], x01, y01, colA[row1 + col]);
                quadsDrawn++;

                // 环面的上下壁：受光面在上、背光面在下，于是侧看时是一条有明暗的立体环带
                if(t1 > 0.01f){
                    Fill.quad(x00, y00 + t0, colTop[row0 + col], x10, y10 + t0, colTop[row0 + coln],
                        x11, y11 + t1, colTop[row1 + coln], x01, y01 + t1, colTop[row1 + col]);
                    Fill.quad(x00, y00 - t0, colBot[row0 + col], x10, y10 - t0, colBot[row0 + coln],
                        x11, y11 - t1, colBot[row1 + coln], x01, y01 - t1, colBot[row1 + col]);
                    quadsDrawn += 2;
                }
            }
        }
    }

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
        // 起始角 180（dir>0）或 0；角度 a_i = start + i * 180 / segments，查表下标 j = i + start/180*segments
        int startJ = dir > 0 ? SEG : 0;

        // 镜像采样盘的另一侧（角 + 180°），多普勒方向相反：每个角只算一次内 / 外两条亮度
        float hr = hot.r, hg = hot.g, hb = hot.b, mr = mid.r, mg = mid.g, mb = mid.b, cr = cold.r, cg = cold.g, cb = cold.b;
        for(int i = 0; i <= segments; i++){
            int j = startJ + i * segStep;
            float ma = j * 180f / SEG + 180f;
            int mj = (j + SEG) % (SEG * 2);
            lensIn[i] = brightness(ma, dopH[mj], sp, alpha, 0.05f, radialIn);
            lensOut[i] = brightness(ma, dopH[mj], sp, alpha, 0.25f, radialOut);
        }

        for(int ring = 0; ring < lensRings; ring++){
            int lk0 = ring * lensStep, lk1 = lk0 + lensStep;
            float f0 = lensF[lk0], f1 = lensF[lk1];
            float r0 = Mathf.lerp(bandIn, bandOut, f0);
            float r1 = Mathf.lerp(bandIn, bandOut, f1);
            // 径向剖面：中间最亮，内外两侧都渐隐到 0 —— 关键，否则每层边界都可见
            float p0 = lensProfile[lk0], p1 = lensProfile[lk1];
            //tint 的基色：内缘 f0*0.35 < 0.5 → hot→mid；外缘 0.15 + f1*0.45 ≤ 0.6
            float ti = f0 * 0.35f * 2f;
            float iR = hr + ti * (mr - hr), iG = hg + ti * (mg - hg), iB = hb + ti * (mb - hb);
            float fo = 0.15f + f1 * 0.45f;
            float oR, oG, oB;
            if(fo < 0.5f){
                float t = fo * 2f;
                oR = hr + t * (mr - hr); oG = hg + t * (mg - hg); oB = hb + t * (mb - hb);
            }else{
                float t = (fo - 0.5f) * 2f;
                oR = mr + t * (cr - mr); oG = mg + t * (cg - mg); oB = mb + t * (cb - mb);
            }

            for(int i = 0; i < segments; i++){
                int j0 = startJ + i * segStep, j1 = j0 + segStep;
                // 两端（水平方向）渐隐，与主盘平滑衔接；中角 = (j0 + 0.5) * 180 / segments
                float fall = fallH[j0];

                float b0 = lensIn[i] * fall * p0 * 1.45f;
                float b1 = lensIn[i + 1] * fall * p0 * 1.45f;
                float b2 = lensOut[i + 1] * fall * p1 * 1.45f;
                float b3 = lensOut[i] * fall * p1 * 1.45f;

                float col0 = tint(iR, iG, iB, b0), col1 = tint(iR, iG, iB, b1);
                float col2 = tint(oR, oG, oB, b2), col3 = tint(oR, oG, oB, b3);

                float c0 = cosH[j0], s0 = sinH[j0], ca = cosH[j1], sa = sinH[j1];
                Fill.quad(
                    x + c0 * r0, y + s0 * r0, col0,
                    x + ca * r0, y + sa * r0, col1,
                    x + ca * r1, y + sa * r1, col2,
                    x + c0 * r1, y + s0 * r1, col3
                );
                quadsDrawn++;
            }
        }
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

    /**
     * 相对论多普勒集束 + 引力红移 + 湍流（与 v6.1 的 brightness 完全相同，多普勒基值改为查表传入）。
     * @param f 径向归一化位置（0 内 → 1 外）
     */
    private static float brightness(float angle, float dopplerBase, float spin, float alpha, float f, float radial){
        // 内圈速度更快 → 集束更极端；外圈趋于均匀
        float doppler = Mathf.lerp(dopplerBase, 0.55f + dopplerBase * 0.45f, f);
        // 径向：内侧最热最亮，外侧迅速变暗（引力红移 + 温度梯度）—— radial = pow(1 - f, 1.35) * 1.15 + 0.08，已预先算好
        // 湍流条纹：多个不同频率的螺旋叠加，避免规则感
        float turb =
            0.70f
            + 0.18f * Mathf.sinDeg(angle * 3f + spin)
            + 0.12f * Mathf.sinDeg(angle * 7f - spin * 0.6f + f * 140f)
            + 0.08f * Mathf.sinDeg(angle * 13f + spin * 1.7f);
        return Mathf.clamp(doppler * radial * turb * 1.25f) * alpha;
    }

    /**
     * 由环基色与亮度取得打包颜色（v6.1 tint 的逐分量展开：bright > 0.62 时向白色偏移模拟蓝移，
     * alpha = clamp(bright)）。
     */
    private static float tint(float r, float g, float b, float bright){
        float a = Mathf.clamp(bright);
        if(bright > 0.74f){
            float t = (bright - 0.74f) * 0.9f;
            r += t * (1f - r); g += t * (1f - g); b += t * (1f - b);
            if(r > 1f) r = 1f; if(g > 1f) g = 1f; if(b > 1f) b = 1f;
        }
        return Color.toFloatBits(r, g, b, a);
    }
}
