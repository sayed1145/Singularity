package blackhole;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.*;
import mindustry.graphics.*;

/** Quiet, self-contained visual language for Aurelia. No black-hole or full-screen effects. */
public final class AureliaFx{
    public static final Color lumen = Color.valueOf("91e8ff");
    public static final Color resonance = Color.valueOf("b69cff");
    public static final Color concord = Color.valueOf("ffd875");

    public static final Effect terrainPulse = new Effect(28f, 34f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(resonance, Color.white, e.fin());
        Draw.alpha(e.fout() * 0.22f);
        Lines.stroke(1f + e.fin());
        Lines.poly(e.x, e.y, 6, 2f + e.fin() * 8f, e.rotation);
        Draw.blend();
        Draw.reset();
    });

    public static final Effect lumenHit = new Effect(20f, 55f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(lumen, Color.white, e.fin());
        Draw.alpha(e.fout() * 0.75f);
        Lines.stroke(1.2f * e.fout());
        Lines.circle(e.x, e.y, 2f + e.fin() * 12f);
        for(int i = 0; i < 4; i++){
            float a = i * 90f + e.rotation;
            Lines.lineAngle(e.x, e.y, a, 4f + e.fin() * 10f);
        }
        Draw.blend();
        Draw.reset();
    });

    public static final Effect prismCraft = new Effect(34f, 70f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(resonance, concord, e.fin());
        Draw.alpha(e.fout() * 0.62f);
        Lines.stroke(1.4f * e.fout());
        Lines.poly(e.x, e.y, 4, 5f + e.fin() * 18f, 45f + e.rotation);
        Fill.circle(e.x, e.y, 2.2f * e.fout());
        Draw.blend();
        Draw.reset();
    });

    public static final Effect concordBirth = new Effect(48f, 110f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(concord, Color.white, e.fin());
        Draw.alpha(e.fout() * 0.58f);
        Lines.stroke(1.8f * e.fout());
        for(int i = 0; i < 3; i++){
            Lines.poly(e.x, e.y, 6, 9f + e.fin() * (10f + i * 8f), e.rotation + i * 30f);
        }
        Draw.blend();
        Draw.reset();
    });

    public static final Effect detainAbsorb = new Effect(18f, 90f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(e.color, Color.white, e.fin());
        Draw.alpha(e.fout() * 0.8f);
        float d = e.fin() * 14f;
        Lines.stroke(1.1f * e.fout());
        Lines.lineAngle(e.x + Angles.trnsx(e.rotation, d), e.y + Angles.trnsy(e.rotation, d), e.rotation, 5f * e.fout());
        Fill.circle(e.x + Angles.trnsx(e.rotation, d), e.y + Angles.trnsy(e.rotation, d), 1.3f * e.fout());
        Draw.blend();
        Draw.reset();
    });

    public static final Effect detainDischarge = new Effect(26f, 90f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(e.color, Color.white, e.fin());
        Draw.alpha(e.fout());
        Lines.stroke(2f * e.fout());
        Lines.circle(e.x, e.y, 3f + e.fin() * 16f);
        for(int i = 0; i < 6; i++){
            Lines.lineAngle(e.x, e.y, e.rotation + (i - 2.5f) * 14f, 4f + e.fin() * 16f);
        }
        Draw.blend();
        Draw.reset();
    });

    public static final Effect detainGrab = new Effect(30f, 60f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(e.color);
        Draw.alpha(e.fout() * 0.7f);
        Lines.stroke(1.5f * e.fout());
        Lines.poly(e.x, e.y, 3, 14f - e.fin() * 9f, e.fin() * 120f);
        Lines.poly(e.x, e.y, 3, 14f - e.fin() * 9f, 60f - e.fin() * 120f);
        Draw.blend();
        Draw.reset();
    });

    public static final Effect detainThrow = new Effect(22f, 80f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(e.color, Color.white, e.fout());
        Draw.alpha(e.fout() * 0.7f);
        Lines.stroke(2f * e.fout());
        for(int i = -2; i <= 2; i++){
            Lines.lineAngle(e.x, e.y, e.rotation + i * 9f, 6f + e.fin() * 22f);
        }
        Draw.blend();
        Draw.reset();
    });

    public static final Effect detainImpact = new Effect(40f, 120f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(e.color, resonance, e.fin());
        Draw.alpha(e.fout() * 0.8f);
        Lines.stroke(3f * e.fout());
        Lines.circle(e.x, e.y, 4f + e.fin() * (e.rotation + 26f));
        Lines.poly(e.x, e.y, 6, 2f + e.fin() * (e.rotation * 0.6f + 16f), e.fin() * 40f);
        Angles.randLenVectors(e.id, 10, 6f + e.fin() * 28f, (x, y) -> Fill.circle(e.x + x, e.y + y, 2f * e.fout()));
        Draw.blend();
        Draw.reset();
    });

    /**
     * v8.5.4：晶格蠕者专用 —— {@link #detainImpact} 的 <b>1/3 尺寸</b>版。
     *
     * <p>原特效是按"反拘留者"那门大炮的口径写的（溅射半径 26 格的重型弹），装在晶格蠕者这门
     * 短筒迫击炮上，一朵火球几乎和车体一样大。这里所有<b>长度</b>都除以 3，混合模式、配色、
     * 时长和旋转节奏一律不动 —— 所以形状、颜色、节奏完全一致，只是小了一圈。
     *
     * <p>单独做一个而不是直接改 {@link #detainImpact}：那个特效锚钉炮、拘留者那条线也在用，
     * 一起缩小等于把那些重型武器的打击感也削掉了。
     */
    public static final Effect detainImpactSmall = new Effect(40f, 40f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(e.color, resonance, e.fin());
        Draw.alpha(e.fout() * 0.8f);
        //every length below is detainImpact / 3: stroke 3->1, circle base 4->1.33, growth (rot+26)->(rot+26)/3
        Lines.stroke(1f * e.fout());
        Lines.circle(e.x, e.y, 1.33f + e.fin() * ((e.rotation + 26f) / 3f));
        Lines.poly(e.x, e.y, 6, 0.67f + e.fin() * ((e.rotation * 0.6f + 16f) / 3f), e.fin() * 40f);
        //10 sparks: spread 6+fin*28 -> 2+fin*9.33, dot radius 2 -> 0.67
        Angles.randLenVectors(e.id, 10, 2f + e.fin() * 9.33f, (x, y) -> Fill.circle(e.x + x, e.y + y, 0.67f * e.fout()));
        Draw.blend();
        Draw.reset();
    });

    /**
     * v8.5.5：类星体喷流的枪口特效 —— 三道聚焦环向内收束 + 一点白热的芯 + 五粒溅出的光屑。
     *
     * <p>引擎那一份开火特效被 {@link blackhole.g3d.MuzzlePattern} 摘走了（它要在抬升后的炮口补播），
     * 但连续光束炮压根不走那条路径，所以类星体喷流开火时什么都看不到。这一份由
     * {@link ContinuousTurret3D} 在枪口直接补播。峰值半径 <b>13</b> 像素。
     */
    public static final Effect quasarMuzzle = new Effect(14f, 30f, e -> {
        Draw.blend(Blending.additive);
        //three focusing rings collapsing inward
        for(int i = 0; i < 3; i++){
            float f = Mathf.clamp(e.fin() * 1.6f - i * 0.18f);
            Draw.color(resonance, e.fout() * (1f - i * 0.25f));
            Lines.stroke(1.3f * e.fout());
            Lines.circle(e.x, e.y, 13f - f * 9f);
        }
        Draw.color(Color.white, e.fout());
        Fill.circle(e.x, e.y, 2.2f * e.fout());
        Draw.color(lumen, e.fout() * 0.8f);
        Angles.randLenVectors(e.id, 5, 2f + e.fin() * 9f,
            (x, y) -> Fill.circle(e.x + x, e.y + y, 0.5f * e.fout()));
        Draw.blend();
        Draw.reset();
    });

    // ---------------------------------------------------------------------------------------------------
    // v8.3 - the ward dome and the waste cycle. Written from scratch and deliberately dim: no additive
    // blending, no bloom, no screen shake, nothing that washes out the map at night.
    // ---------------------------------------------------------------------------------------------------

    /** dome collapse: the rim breaks into twelve segments that drift outwards and go out. e.rotation = radius */
    public static final Effect wardBreak = new Effect(70f, 900f, e -> {
        float rad = e.rotation;
        Draw.color(e.color);
        Draw.alpha(e.fout() * 0.5f);
        Lines.stroke(1.6f * e.fout());
        for(int i = 0; i < 12; i++){
            float ang = i * 30f + 15f;
            float off = e.fin() * 10f;
            Lines.arc(e.x + Angles.trnsx(ang, off), e.y + Angles.trnsy(ang, off), rad, 0.055f, ang);
        }
        Draw.alpha(e.fout() * 0.3f);
        Lines.stroke(1f * e.fout());
        Lines.circle(e.x, e.y, rad * (1f + e.fin() * 0.04f));
        Draw.reset();
    });

    /** dome back up: one ring drawn inwards, once. e.rotation = radius */
    public static final Effect wardRaise = new Effect(40f, 900f, e -> {
        Draw.color(e.color);
        Draw.alpha(e.fout() * 0.45f);
        Lines.stroke(1.4f * e.fout());
        Lines.circle(e.x, e.y, e.rotation * (1.12f - e.fin() * 0.12f));
        Draw.reset();
    });

    /** the waste line: a dull grey puff, nothing bright */
    public static final Effect wasteVent = new Effect(42f, e -> {
        Draw.color(Color.valueOf("6b6a74"), Color.valueOf("3a3a42"), e.fin());
        Draw.alpha(e.fout() * 0.55f);
        Angles.randLenVectors(e.id, 5, 2f + e.fin() * 9f, (x, y) -> Fill.circle(e.x + x, e.y + y, 1.6f * e.fout()));
        Draw.reset();
    });

    /** slurry step: a slow falling drip ring */
    public static final Effect slurryDrip = new Effect(38f, e -> {
        Draw.color(Color.valueOf("7fd9e8"));
        Draw.alpha(e.fout() * 0.6f);
        Lines.stroke(1.1f * e.fout());
        Lines.circle(e.x, e.y, 2f + e.fin() * 7f);
        Angles.randLenVectors(e.id, 3, 1f + e.fin() * 6f, (x, y) -> Fill.circle(e.x + x, e.y + y, 1.3f * e.fout()));
        Draw.reset();
    });

    private AureliaFx(){}
}
