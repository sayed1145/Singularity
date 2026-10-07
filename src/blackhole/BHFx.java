package blackhole;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.graphics.*;

import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;

/**
 * 全部特效均为本模组自制，不引用任何原版 Fx。
 * 设计原则：几何语言统一（环 / 径向线 / 吸积旋臂 / 六边形约束场），
 * 配色统一走 BHPal，让整套模组看起来是一个体系而不是拼凑。
 */
public class BHFx{

    // ==================================================================
    // 通用小工具
    // ==================================================================

    /** 画一条带弧度的吸积旋臂。 */
    static void spiralArm(float cx, float cy, float ang, float r0, float r1, int seg){
        float px = cx + trnsx(ang, r0), py = cy + trnsy(ang, r0);
        for(int i = 1; i <= seg; i++){
            float f = i / (float)seg;
            float r = Mathf.lerp(r0, r1, f);
            float a = ang + f * 58f;
            float nx = cx + trnsx(a, r), ny = cy + trnsy(a, r);
            Lines.line(px, py, nx, ny);
            px = nx; py = ny;
        }
    }

    /** 六边形约束场轮廓（工厂/装配厂共用视觉母题）。 */
    static void hexRing(float cx, float cy, float r, float rot){
        Lines.poly(cx, cy, 6, r, rot);
    }

    // ==================================================================
    // 黑洞塌缩系
    // ==================================================================

    /** 塌缩：物质被向内抽吸，然后爆出引力波。 */
    public static Effect collapse = new Effect(58f, 260f, e -> {
        Draw.blend(Blending.additive);

        randLenVectors(e.id, 26, 4f + 62f * (1f - e.fin()), (x, y) -> {
            color(BHPal.accretion, BHPal.core, e.fin());
            Fill.circle(e.x + x, e.y + y, e.fout() * 3.2f + 0.6f);
        });

        color(Color.black);
        Fill.circle(e.x, e.y, 22f * Mathf.curve(e.fin(), 0f, 0.35f) * e.fout(0.35f));

        color(BHPal.core, e.fout());
        stroke(e.fout() * 3.4f);
        Lines.circle(e.x, e.y, 6f + e.fin() * 84f);
        color(BHPal.rim, e.fout() * 0.7f);
        stroke(e.fout() * 2f);
        Lines.circle(e.x, e.y, 6f + e.fin() * 126f);

        color(BHPal.hawking, e.fout() * 0.8f);
        stroke(e.fout() * 2.6f);
        for(int i = 0; i < 4; i++){
            float a = i * 90f + 45f;
            lineAngle(e.x + trnsx(a, 10f), e.y + trnsy(a, 10f), a, e.fin() * 70f);
        }

        Draw.blend();
    }).layer(Layer.effect + 1f);

    /** 持久黑洞寿命终结时的最终塌缩 —— 大范围引力波 + 吸积旋臂甩出。 */
    public static Effect finalCollapse = new Effect(85f, 520f, e -> {
        Draw.blend(Blending.additive);

        for(int i = 0; i < 3; i++){
            float off = i * 0.13f;
            float fin = Mathf.clamp((e.fin() - off) / (1f - off));
            if(fin <= 0f) continue;
            float fout = 1f - fin;
            color(i == 0 ? BHPal.core : (i == 1 ? BHPal.accretion : BHPal.rim), fout * 0.9f);
            stroke(fout * (4.5f - i));
            Lines.circle(e.x, e.y, 10f + fin * (190f + i * 55f));
        }

        // 甩出的吸积旋臂
        color(BHPal.accretion, e.fout() * 0.85f);
        stroke(e.fout() * 2.4f);
        for(int i = 0; i < 5; i++){
            spiralArm(e.x, e.y, i * 72f + e.fin() * 90f, 12f, 30f + e.fin() * 150f, 7);
        }

        randLenVectors(e.id, 34, 18f + e.fin() * 165f, (x, y) -> {
            color(BHPal.core, BHPal.rim, e.fin());
            Fill.circle(e.x + x, e.y + y, e.fout() * 4.6f + 0.6f);
        });

        color(Color.white, e.fout(0.25f));
        Fill.circle(e.x, e.y, e.fout(0.25f) * 26f);

        Draw.blend();
    }).layer(Layer.effect + 1f);

    public static Effect collapseSmall = new Effect(34f, 120f, e -> {
        Draw.blend(Blending.additive);
        randLenVectors(e.id, 12, 2f + 26f * (1f - e.fin()), (x, y) -> {
            color(BHPal.accretion, BHPal.core, e.fin());
            Fill.circle(e.x + x, e.y + y, e.fout() * 2f + 0.4f);
        });
        color(Color.black);
        Fill.circle(e.x, e.y, 9f * Mathf.curve(e.fin(), 0f, 0.35f) * e.fout(0.35f));
        color(BHPal.core, e.fout());
        stroke(e.fout() * 1.8f);
        Lines.circle(e.x, e.y, 3f + e.fin() * 38f);
        Draw.blend();
    }).layer(Layer.effect + 1f);

    /** 单位被撕碎（潮汐面条化）。 */
    public static Effect devour = new Effect(26f, e -> {
        Draw.blend(Blending.additive);
        color(e.color, Color.white, e.fin() * 0.6f);
        stroke(e.fout() * 1.7f);
        for(int i = 0; i < 3; i++){
            float a = e.rotation + Mathf.randomSeedRange(e.id + i * 31L, 26f);
            lineAngle(e.x, e.y, a, 4f + e.fin() * 22f);
        }
        Fill.circle(e.x, e.y, e.fout() * 2.2f);
        Draw.blend();
    }).layer(Layer.effect);

    /** 吸积盘火花。 */
    public static Effect accretionSpark = new Effect(24f, e -> {
        Draw.blend(Blending.additive);
        float rad = e.data instanceof Float f ? f : 4f;
        randLenVectors(e.id, 3, rad * 2.3f, rad * 1.7f, (x, y) -> {
            color(BHPal.core, BHPal.rim, e.fin());
            Fill.circle(e.x + x, e.y + y, e.fout() * 1.6f);
        });
        Draw.blend();
    }).layer(Layer.effect);

    // ==================================================================
    // 炮口 / 充能系（替代原版 shootBigSmoke2 等）
    // ==================================================================

    /** 自制炮口烟：径向喷出的暗色尘团，带一点吸积色。 */
    public static Effect muzzleDust = new Effect(23f, e -> {
        color(BHPal.rim, BHPal.horizon, e.fin());
        alpha(e.fout() * 0.7f);
        randLenVectors(e.id, 7, 3f + e.fin() * 19f, e.rotation, 22f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.7f + e.fout() * 2.6f);
        });
    }).layer(Layer.bullet - 0.01f);

    /** 自制炮口闪光：一道横向压扁的锥形闪。 */
    public static Effect muzzleFlash = new Effect(14f, e -> {
        Draw.blend(Blending.additive);
        color(BHPal.core, Color.white, e.fin() * 0.5f);
        alpha(e.fout());
        Drawf.tri(e.x, e.y, 5.5f * e.fout(), 21f * e.fout(), e.rotation);
        Drawf.tri(e.x, e.y, 4f * e.fout(), 7f * e.fout(), e.rotation + 180f);
        Fill.circle(e.x, e.y, e.fout() * 3.4f);
        Draw.blend();
    }).layer(Layer.effect);

    /** 奇点充能：物质向内汇聚 + 六边形约束场收缩。 */
    public static Effect singularityCharge = new Effect(58f, 160f, e -> {
        Draw.blend(Blending.additive);
        color(BHPal.accretion, BHPal.core, e.fin());
        stroke(e.fin() * 2.6f);
        Lines.circle(e.x, e.y, 54f * e.fout() + 6f);

        // 收缩的六边约束场
        color(BHPal.hawking, e.fin() * 0.8f);
        stroke(e.fin() * 1.4f);
        hexRing(e.x, e.y, 46f * e.fout() + 8f, e.fin() * 120f);

        randLenVectors(e.id, 12, 54f * e.fout() + 7f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fin() * 2.8f);
        });
        color(Color.black);
        Fill.circle(e.x, e.y, e.fin() * 6f);
        Draw.blend();
    }).layer(Layer.effect);

    /** 霍金发射器充能。 */
    public static Effect hawkingCharge = new Effect(44f, 120f, e -> {
        Draw.blend(Blending.additive);
        color(BHPal.hawking, Color.white, e.fin());
        stroke(e.fin() * 2f);
        Lines.circle(e.x, e.y, 38f * e.fout() + 4f);
        randLenVectors(e.id, 8, 38f * e.fout() + 5f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fin() * 2.2f, 45f);
        });
        Draw.blend();
    }).layer(Layer.effect);

    /** 霍金辐射命中。 */
    public static Effect radiationHit = new Effect(18f, e -> {
        Draw.blend(Blending.additive);
        color(BHPal.hawking, Color.white, e.fin());
        stroke(e.fout() * 1.6f);
        Lines.circle(e.x, e.y, 2f + e.fin() * 18f);
        randLenVectors(e.id, 5, 3f + e.fin() * 17f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fout() * 1.6f, 45f);
        });
        Draw.blend();
    }).layer(Layer.effect);

    /** 简并弹命中：小型内爆。 */
    public static Effect degenerateHit = new Effect(20f, e -> {
        Draw.blend(Blending.additive);
        color(BHPal.degenerate, BHPal.core, e.fin());
        stroke(e.fout() * 1.9f);
        Lines.circle(e.x, e.y, 1.5f + e.fin() * 15f);
        for(int i = 0; i < 4; i++){
            float a = i * 90f + e.rotation;
            lineAngle(e.x + trnsx(a, 3f + e.fin() * 9f), e.y + trnsy(a, 3f + e.fin() * 9f), a, e.fout() * 5f);
        }
        Draw.blend();
    }).layer(Layer.effect);

    /** 帧拖拽炮命中：切向扭矩环。 */
    public static Effect dragHit = new Effect(22f, e -> {
        Draw.blend(Blending.additive);
        color(BHPal.degenerate, e.fout());
        stroke(e.fout() * 1.5f);
        Lines.circle(e.x, e.y, 2f + e.fin() * 13f);
        // 切向小弧，表达"空间被拖着转"
        for(int i = 0; i < 3; i++){
            float a = i * 120f + e.fin() * 140f;
            Lines.arc(e.x, e.y, 4f + e.fin() * 11f, 0.13f, a);
        }
        Draw.blend();
    }).layer(Layer.effect);

    /** 类星体喷流持续灼烧点。 */
    public static Effect quasarBurn = new Effect(26f, e -> {
        Draw.blend(Blending.additive);
        color(BHPal.core, BHPal.accretion, e.fin());
        alpha(e.fout() * 0.9f);
        Fill.circle(e.x, e.y, e.fout() * 3.6f);
        stroke(e.fout() * 1.2f);
        Lines.circle(e.x, e.y, 3f + e.fin() * 14f);
        randLenVectors(e.id, 4, 2f + e.fin() * 13f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fout() * 1.5f, 45f);
        });
        Draw.blend();
    }).layer(Layer.effect);

    /** 引力弹拖尾。 */
    public static Effect gravTrail = new Effect(28f, e -> {
        Draw.blend(Blending.additive);
        color(BHPal.accretion, BHPal.horizon, e.fin());
        alpha(e.fout() * 0.8f);
        Fill.circle(e.x, e.y, e.fout() * 2.4f);
        Draw.blend();
    }).layer(Layer.bullet - 0.02f);

    // ==================================================================
    // 工厂 / 建筑系
    // ==================================================================

    /** 工厂环状脉冲。 */
    public static Effect horizonPulse = new Effect(40f, e -> {
        Draw.blend(Blending.additive);
        color(BHPal.degenerate, e.fout() * 0.8f);
        stroke(e.fout() * 1.4f);
        Lines.circle(e.x, e.y, 4f + e.fin() * 22f);
        Draw.blend();
    }).layer(Layer.effect);

    /** 引力压机工作：向内压缩的方形场。 */
    public static Effect pressCrush = new Effect(32f, e -> {
        Draw.blend(Blending.additive);
        color(BHPal.degenerate, e.fout() * 0.85f);
        stroke(e.fout() * 1.6f);
        float r = 14f * e.fout() + 3f;
        Lines.square(e.x, e.y, r, 45f);
        Lines.square(e.x, e.y, r * 0.6f, 0f);
        randLenVectors(e.id, 5, r * 1.3f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fout() * 1.4f, 45f);
        });
        Draw.blend();
    }).layer(Layer.effect);

    /** 冷凝器工作：低温雾气 + 霍金粒子析出。 */
    public static Effect condenseMist = new Effect(46f, e -> {
        color(BHPal.hawking);
        alpha(e.fout() * 0.5f);
        randLenVectors(e.id, 6, 4f + e.fin() * 15f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 1.2f + e.fout() * 2.4f);
        });
        Draw.blend(Blending.additive);
        color(Color.white, e.fout() * 0.7f);
        randLenVectors(e.id + 7L, 3, 3f + e.fin() * 13f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fout() * 1.1f, 45f);
        });
        Draw.blend();
    }).layer(Layer.effect);

    /** 锻炉出货：奇点核心成型。 */
    public static Effect forgeBloom = new Effect(54f, e -> {
        Draw.blend(Blending.additive);
        color(BHPal.core, BHPal.accretion, e.fin());
        stroke(e.fout() * 2.2f);
        hexRing(e.x, e.y, 6f + e.fin() * 26f, e.fin() * 90f);
        color(Color.white, e.fout() * 0.8f);
        Fill.circle(e.x, e.y, e.fout() * 4f);
        Draw.blend();
    }).layer(Layer.effect);

    /** 单位下线。 */
    public static Effect unitSpawned = new Effect(60f, e -> {
        Draw.blend(Blending.additive);
        color(BHPal.horizon, BHPal.core, e.fin());
        stroke(e.fout() * 2.4f);
        hexRing(e.x, e.y, 10f + e.fin() * 30f, -e.fin() * 70f);
        stroke(e.fout() * 1.4f);
        Lines.circle(e.x, e.y, 6f + e.fin() * 44f);
        randLenVectors(e.id, 10, 8f + e.fin() * 34f, (x, y) -> {
            color(BHPal.core, e.fout());
            Fill.circle(e.x + x, e.y + y, e.fout() * 2f);
        });
        Draw.blend();
    }).layer(Layer.effect);

    // ==================================================================
    // 移动作战平台专用
    // ==================================================================

    /** 平台展开/收起时的地锚冲击。 */
    public static Effect anchorSlam = new Effect(38f, 180f, e -> {
        Draw.blend(Blending.additive);
        color(BHPal.degenerate, e.fout());
        stroke(e.fout() * 3.2f);
        Lines.circle(e.x, e.y, 8f + e.fin() * 58f);
        color(BHPal.rim, e.fout() * 0.8f);
        stroke(e.fout() * 1.8f);
        hexRing(e.x, e.y, 12f + e.fin() * 40f, 0f);
        randLenVectors(e.id, 9, 10f + e.fin() * 48f, (x, y) -> {
            Fill.square(e.x + x, e.y + y, e.fout() * 2.6f, 45f);
        });
        Draw.blend();
    }).layer(Layer.effect);

    /** 平台履带扬尘。 */
    public static Effect platformTread = new Effect(30f, e -> {
        color(BHPal.horizon);
        alpha(e.fout() * 0.45f);
        randLenVectors(e.id, 3, 2f + e.fin() * 7f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 1f + e.fout() * 2.2f);
        });
    }).layer(Layer.groundUnit - 0.01f);

    /** 平台护盾投射脉冲。 */
    public static Effect platformShield = new Effect(44f, e -> {
        Draw.blend(Blending.additive);
        color(BHPal.hawking, e.fout() * 0.7f);
        stroke(e.fout() * 1.8f);
        hexRing(e.x, e.y, 20f + e.fin() * 52f, e.fin() * 40f);
        Draw.blend();
    }).layer(Layer.effect);
}
