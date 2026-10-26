package preview;

import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import blackhole.*;
import mindustry.entities.Effect;

/**
 * v8.5.5：把特效的<b>实际峰值半径量出来</b>，而不是照着代码估。
 *
 * <p>做法：用 {@link FakeBatch}（无 GL 的录制型 Batch）接管 {@code Core.batch}，
 * 把每个特效从 0 到 lifetime 每一刻都跑一遍，收集所有顶点，算离特效中心最远的那个有多远。
 *
 * <p>每个特效都会用<b>多个朝向角</b>跑（0/90/180/259/359 度）。这一步不是多余：
 * v8.5.4 那个"缩了 1/3"的爆炸就是在这儿露的马脚 —— 它把 {@code e.rotation}（朝向角）
 * 当半径用了，所以半径会随朝向从几十像素一路涨到几百像素。
 *
 * <p>用法：{@code java preview.FxSize}
 */
public class FxSize{
    /** 用来量半径的朝向角。特意包含一个接近 360 的，好让"rotation 当半径用"这类 bug 现形。 */
    static final float[] ROTS = {0f, 90f, 180f, 259f, 359f};

    public static void main(String[] args){
        FakeBatch b = FakeBatch.install();
        selfTest(b);

        System.out.printf("%-28s %7s %7s %7s %7s   %s%n",
            "effect", "rot=0", "rot=90", "rot=180", "rot=359", "peak px / tiles");
        System.out.println("-".repeat(96));

        row(b, "8.5.5 CrawlerFx.muzzle", CrawlerFx.muzzle);
        row(b, "8.5.5 CrawlerFx.smoke", CrawlerFx.smoke);
        row(b, "8.5.5 CrawlerFx.impact", CrawlerFx.impact);
        row(b, "8.5.5 CrawlerFx.shardHit", CrawlerFx.shardHit);
        System.out.println("-".repeat(96));
        row(b, "8.5.4 detainImpactSmall", AureliaFx.detainImpactSmall);
        row(b, "8.5.1 detainImpact", AureliaFx.detainImpact);
        row(b, "8.5.1 OwnFx.muzzle", OwnFx.muzzle);
        row(b, "8.5.2 OwnFx.muzzleSmall", OwnFx.muzzleSmall);
        row(b, "8.5.1 OwnFx.muzzleSmoke", OwnFx.muzzleSmoke);
        row(b, "8.5.2 OwnFx.muzzleSmokeSmall", OwnFx.muzzleSmokeSmall);
        row(b, "8.5.1 AureliaFx.lumenHit", AureliaFx.lumenHit);
        System.out.println("-".repeat(96));
        row(b, "8.5.5 MarrowFx.shoot", MarrowFx.shoot);
        row(b, "8.5.5 MarrowFx.wake", MarrowFx.wake);
        row(b, "8.5.5 MarrowFx.hit", MarrowFx.hit);
        row(b, "8.5.5 MarrowFx.despawn", MarrowFx.despawn);
        row(b, "8.5.5 MarrowFx.brand", MarrowFx.brand);
        row(b, "8.5.5 MarrowFx.brandApply", MarrowFx.brandApply);
        System.out.println("-".repeat(96));
        row(b, "8.5.5 AureliaFx.quasarMuzzle", AureliaFx.quasarMuzzle);
        System.out.println();
        System.out.println("半径单位：像素。8 像素 = 1 格（tile）。");
        System.out.println("\"varies\" = 半径随朝向角变化 → 说明该特效把 rotation 当尺寸用了。");
    }

    static void row(FakeBatch b, String name, Effect fx){
        float[] per = new float[ROTS.length];
        float worst = 0f;
        for(int i = 0; i < ROTS.length; i++){
            per[i] = peak(b, fx, ROTS[i]);
            worst = Math.max(worst, per[i]);
        }
        float at0 = per[0], at359 = per[per.length - 1];
        String note = String.format("%.1f px / %.2f 格", worst, worst / 8f);
        if(at0 > 1f && Math.abs(at359 - at0) / at0 > 0.25f) note += "  <-- varies with rotation!";
        System.out.printf("%-28s %7.1f %7.1f %7.1f %7.1f   %s%n",
            name, per[0], per[1], per[2], per[per.length - 1], note);
    }

    /**
     * 把 fx 从 0 跑到 lifetime，返回离中心最远的顶点距离。
     *
     * <p>顶点不是挨着放的：每个图元是 24 个 float（4 个顶点 × 6）或 8 个 float（一次区域绘制）。
     * 所以按 {@code kinds} 里记的步长走，只读每个顶点的头两个 float（x, y）。
     */
    static float peak(FakeBatch b, Effect fx, float rot){
        float best = 0f;
        for(float t = 0f; t <= fx.lifetime; t += 1f){
            Effect.EffectContainer c = new Effect.EffectContainer();
            //真实签名是 set(id, color, time, lifetime, rotation, x, y, data) —— 顺序反了的话
            //fin() 会变成 0/0 = NaN，量出来全是 0，看着就像"特效没画东西"一样。
            c.set(1, Color.white, t, fx.lifetime, rot, 0f, 0f, null);
            b.reset();
            b.record = true;
            try{
                fx.render(c);
            }catch(Throwable e){
                Draw.reset();
                return Float.NaN;
            }
            b.flush();

            int p = 0, stride = 0, verts = 0;
            for(int k = 0; k < b.kinds.size; k++){
                int kind = b.kinds.get(k);
                stride = kind == 0 ? 24 : 8;
                verts  = kind == 0 ? 4 : 1;
                if(p + stride > b.n) break;
                for(int v = 0; v < verts; v++){
                    float vx = b.verts[p + v * 6], vy = b.verts[p + v * 6 + 1];
                    float d = Mathf.dst(vx, vy);
                    if(d > best) best = d;
                }
                p += stride;
            }
            Draw.reset();
        }
        return best;
    }

    /**
     * 自检：画一个已知大小的正方形，看量出来的半径对不对。
     * 顶点布局要是猜错了（x,y 不在最前面、或者步长不是 6），这个数就会是错的。
     */
    static void selfTest(FakeBatch b){
        b.reset();
        b.record = true;
        Fill.rect(0f, 0f, 20f, 20f);   //半角 10，外接圆半径 = 10√2 ≈ 14.14
        b.flush();
        float best = 0f;
        for(int k = 0, p = 0; k < b.kinds.size; k++){
            int stride = b.kinds.get(k) == 0 ? 24 : 8, verts = b.kinds.get(k) == 0 ? 4 : 1;
            if(p + stride > b.n) break;
            for(int v = 0; v < verts; v++){
                best = Math.max(best, Mathf.dst(b.verts[p + v * 6], b.verts[p + v * 6 + 1]));
            }
            p += stride;
        }
        Draw.reset();
        boolean ok = Math.abs(best - 14.142f) < 0.05f;
        System.out.println("自检（20x20 正方形，应为 14.14）：量得 " + String.format("%.3f", best)
            + (ok ? "  OK" : "  <-- 顶点布局假设有误，下面的数字不可信！"));
        System.out.println();
    }
}
