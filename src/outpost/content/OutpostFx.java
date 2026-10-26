package outpost.content;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import mindustry.entities.Effect;

/**
 * v2 self-made effects. No vanilla Fx.* is referenced anywhere in the mod:
 * every shoot / trail / hit / despawn / craft effect below is original Java.
 * No shaders, no external textures, headless-server safe.
 */
public final class OutpostFx{
    public static final Color cyan = Color.valueOf("8de9f5");
    public static final Color cyanDeep = Color.valueOf("2fa8c9");
    public static final Color amber = Color.valueOf("ffc35c");
    public static final Color amberDeep = Color.valueOf("c97a1e");
    public static final Color white = Color.white;

    private OutpostFx(){}

    // ---------------- factories ----------------

    /** Slug press: crisp hydraulic impact ring + rising sparks. */
    public static final Effect pressSpark = new Effect(24f, e -> {
        float fout = e.fout(), fin = e.fin();
        Draw.color(cyan, fout);
        Lines.stroke(2.2f * fout);
        Lines.circle(e.x, e.y, 3f + fin * 13f);
        Draw.color(white, fout);
        Fill.circle(e.x, e.y, 3.2f * fout);
        Draw.color(cyanDeep, fout);
        for(int i = 0; i < 6; i++){
            float a = i * 60f + e.id * 17f;
            float d = 4f + fin * 16f;
            Fill.circle(e.x + Angles.trnsx(a, d), e.y + Angles.trnsy(a, d), 1.4f * fout);
        }
        Draw.reset();
    });

    /** Coil winder: warm winding flash + orbiting sparks. */
    public static final Effect winderSpark = new Effect(26f, e -> {
        float fout = e.fout(), fin = e.fin();
        Draw.color(amber, fout);
        Lines.stroke(2.0f * fout);
        Lines.circle(e.x, e.y, 2.5f + fin * 11f);
        Draw.color(white, fout);
        Fill.circle(e.x, e.y, 2.4f * fout);
        Draw.color(amberDeep, fout);
        for(int i = 0; i < 5; i++){
            float a = e.id * 31f + i * 72f + fin * 140f;
            float d = 3f + fin * 13f;
            Fill.circle(e.x + Angles.trnsx(a, d), e.y + Angles.trnsy(a, d), 1.2f * fout);
        }
        Draw.reset();
    });

    // ---------------- heavy railgun ----------------

    /** Heavy muzzle: directional cone + double ring, drawn exactly at the bullet spawn. */
    public static final Effect muzzleHeavy = new Effect(22f, 90f, e -> {
        float fout = e.fout(), fin = e.fin();
        float rot = e.rotation;
        Draw.blend(Blending.additive);
        Draw.color(cyan, 0.9f * fout);
        Lines.stroke(4.2f * fout);
        Lines.circle(e.x, e.y, 3f + fin * 22f);
        Draw.color(white, fout);
        Lines.stroke(1.6f * fout);
        Lines.circle(e.x, e.y, 2f + fin * 15f);
        // forward cone along the shot direction
        Draw.color(cyan, 0.75f * fout);
        for(int s = -1; s <= 1; s += 2){
            float a = rot + s * 9f;
            float x0 = e.x + Angles.trnsx(rot, 4f), y0 = e.y + Angles.trnsy(rot, 4f);
            float x1 = e.x + Angles.trnsx(a, 26f * fout + 8f), y1 = e.y + Angles.trnsy(a, 26f * fout + 8f);
            Lines.stroke(2.4f * fout);
            Lines.line(x0, y0, x1, y1);
        }
        Fill.circle(e.x, e.y, 5.5f * fout);
        Draw.blend();
        Draw.reset();
    });

    /** Heavy charge: converging rings while the coils spool up. */
    public static final Effect chargeHeavy = new Effect(30f, 60f, e -> {
        float fin = e.fin(), fout = e.fout();
        Draw.blend(Blending.additive);
        Draw.color(cyan, 0.7f * fout + 0.2f * fin);
        Lines.stroke(1.8f * fout + 0.4f);
        Lines.circle(e.x, e.y, 16f * (1f - fin) + 3f);
        Draw.color(white, 0.8f * fin);
        Fill.circle(e.x, e.y, 2.5f * fin);
        Draw.blend();
        Draw.reset();
    });

    /** Heavy trail: elongated streak left by the lance slug. */
    public static final Effect trailHeavy = new Effect(14f, e -> {
        float fout = e.fout();
        Draw.blend(Blending.additive);
        Draw.color(cyan, 0.7f * fout);
        Fill.circle(e.x, e.y, 2.0f * fout);
        Draw.color(white, 0.85f * fout);
        Fill.circle(e.x, e.y, 0.9f * fout);
        Draw.blend();
        Draw.reset();
    });

    /** Heavy pierce: directional cross flash when punching through a target. */
    public static final Effect pierceHeavy = new Effect(20f, e -> {
        float fout = e.fout(), fin = e.fin();
        float rot = e.rotation;
        Draw.blend(Blending.additive);
        Draw.color(cyan, fout);
        Lines.stroke(2.6f * fout);
        Lines.circle(e.x, e.y, 3f + fin * 14f);
        Draw.color(white, fout);
        for(int i = 0; i < 4; i++){
            float a = rot + i * 90f;
            Lines.stroke(1.8f * fout);
            Lines.line(e.x + Angles.trnsx(a, 2f), e.y + Angles.trnsy(a, 2f),
                e.x + Angles.trnsx(a, 6f + fin * 16f), e.y + Angles.trnsy(a, 6f + fin * 16f));
        }
        Draw.blend();
        Draw.reset();
    });

    /** Heavy hit: terminal impact, rings + forward debris. */
    public static final Effect hitHeavy = new Effect(30f, 80f, e -> {
        float fout = e.fout(), fin = e.fin();
        float rot = e.rotation;
        Draw.blend(Blending.additive);
        Draw.color(cyan, 0.85f * fout);
        Lines.stroke(3.2f * fout);
        Lines.circle(e.x, e.y, 4f + fin * 24f);
        Draw.color(white, fout);
        Fill.circle(e.x, e.y, 4.5f * fout);
        Draw.color(cyanDeep, fout);
        for(int i = 0; i < 8; i++){
            float a = rot + Mathf.range(70f) + i * 12f;
            float d = 5f + fin * (18f + (i % 3) * 7f);
            Fill.circle(e.x + Angles.trnsx(a, d), e.y + Angles.trnsy(a, d), 1.6f * fout);
        }
        Draw.blend();
        Draw.reset();
    });

    /** Heavy despawn: soft collapse at max range. */
    public static final Effect despawnHeavy = new Effect(18f, e -> {
        float fout = e.fout(), fin = e.fin();
        Draw.color(cyan, 0.6f * fout);
        Lines.stroke(1.6f * fout);
        Lines.circle(e.x, e.y, 2f + fin * 10f);
        Draw.color(white, 0.5f * fout);
        Fill.circle(e.x, e.y, 1.8f * fout);
        Draw.reset();
    });

    // ---------------- light lancer ----------------

    public static final Effect muzzleLight = new Effect(16f, 60f, e -> {
        float fout = e.fout(), fin = e.fin();
        float rot = e.rotation;
        Draw.blend(Blending.additive);
        Draw.color(amber, 0.9f * fout);
        Lines.stroke(2.8f * fout);
        Lines.circle(e.x, e.y, 2f + fin * 13f);
        Draw.color(white, fout);
        Fill.circle(e.x, e.y, 3.0f * fout);
        Draw.color(amber, 0.7f * fout);
        Lines.stroke(1.6f * fout);
        Lines.line(e.x, e.y, e.x + Angles.trnsx(rot, 14f * fout + 4f), e.y + Angles.trnsy(rot, 14f * fout + 4f));
        Draw.blend();
        Draw.reset();
    });

    public static final Effect trailLight = new Effect(11f, e -> {
        float fout = e.fout();
        Draw.blend(Blending.additive);
        Draw.color(amber, 0.7f * fout);
        Fill.circle(e.x, e.y, 1.4f * fout);
        Draw.color(white, 0.7f * fout);
        Fill.circle(e.x, e.y, 0.6f * fout);
        Draw.blend();
        Draw.reset();
    });

    public static final Effect hitLight = new Effect(20f, 50f, e -> {
        float fout = e.fout(), fin = e.fin();
        Draw.blend(Blending.additive);
        Draw.color(amber, 0.85f * fout);
        Lines.stroke(2.2f * fout);
        Lines.circle(e.x, e.y, 2.5f + fin * 15f);
        Draw.color(white, fout);
        Fill.circle(e.x, e.y, 2.6f * fout);
        Draw.color(amberDeep, fout);
        for(int i = 0; i < 5; i++){
            float a = i * 72f + e.id * 23f;
            float d = 3f + fin * 12f;
            Fill.circle(e.x + Angles.trnsx(a, d), e.y + Angles.trnsy(a, d), 1.1f * fout);
        }
        Draw.blend();
        Draw.reset();
    });

    public static final Effect despawnLight = new Effect(14f, e -> {
        float fout = e.fout(), fin = e.fin();
        Draw.color(amber, 0.55f * fout);
        Lines.stroke(1.3f * fout);
        Lines.circle(e.x, e.y, 1.5f + fin * 8f);
        Draw.reset();
    });
}
