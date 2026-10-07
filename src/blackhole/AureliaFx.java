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
