package sixfold;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import blackhole.OwnFx;
import mindustry.entities.*;

import static arc.graphics.g2d.Draw.*;

/**
 * Pyroclast Titan and v3.2 turret effects (v7.1 redo): pseudo-3D like the models (squashed ground rings, rising
 * heat, embers on arcs, additive cores). Short lifetimes and few primitives; nothing forwards to a vanilla Fx.
 */
public class EmberEffects{
    private static final Color ember = Color.valueOf("ff9e5c");
    private static final Color magma = Color.valueOf("e05545");
    private static final Color bone = Color.valueOf("f2e6d8");
    private static final Color steel = Color.valueOf("9ecbff");
    private static final Color ash = Color.valueOf("4d403c");

    /** searing vent: a short hot cone along e.rotation plus two sparks */
    public static final Effect vent = new Effect(14f, 70f, e -> {
        float f = e.fin();
        blend(Blending.additive);
        Draw.color(bone, ember, f);
        Draw.alpha(e.fout());
        float len = 4f + f * 10f, w = 2.6f * e.fout();
        Fill.tri(e.x + Angles.trnsx(e.rotation + 90f, w), e.y + Angles.trnsy(e.rotation + 90f, w),
            e.x + Angles.trnsx(e.rotation - 90f, w), e.y + Angles.trnsy(e.rotation - 90f, w),
            e.x + Angles.trnsx(e.rotation, len), e.y + Angles.trnsy(e.rotation, len));
        Angles.randLenVectors(e.id, 2, 3f + f * 12f, e.rotation, 40f, (x, y) -> {
            Draw.color(ember, magma, f);
            Fill.circle(e.x + x, e.y + y + f * 3f * OwnFx.lift, 0.9f * e.fout() + 0.2f);
        });
        blend();
        reset();
    });

    /** artillery impact: flash, magma ring, embers thrown on arcs, ash puffs */
    public static final Effect burst = new Effect(30f, 170f, e -> {
        OwnFx.flash(e.x, e.y, 12f * Mathf.clamp(e.fout() * 2f), ember, e.fout());
        Draw.color(ember, magma, e.fin());
        Draw.alpha(e.fout());
        OwnFx.ring(e.x, e.y, 4f + e.finpow() * 34f, 2.8f * e.fout());
        OwnFx.puffs(e, 5, 20f, 3.6f, ash);
        OwnFx.debris(e, 8, 30f, 1.2f, ember);
        reset();
    });

    /** Ember Crown nova: a hot squashed ring rolling outwards, a thinner echo and rising heat; e.rotation = radius scale */
    public static final Effect crown = new Effect(26f, 240f, e -> {
        float r = e.rotation * 120f, f = e.finpow();
        blend(Blending.additive);
        Draw.color(ember, magma, e.fin());
        Draw.alpha(0.8f * e.fout());
        OwnFx.ring(e.x, e.y, f * r, 3f * e.fout() + 0.5f);
        Draw.alpha(0.4f * e.fout());
        OwnFx.ring(e.x, e.y, f * r * 0.72f, 1.4f * e.fout());
        Draw.color(ember, 0.18f * e.fout());
        OwnFx.disc(e.x, e.y, f * r * 0.6f);
        blend();
        reset();
    });

    /** fading ember mote (frag impact) */
    public static final Effect mote = new Effect(16f, 50f, e -> {
        blend(Blending.additive);
        Draw.color(ember, magma, e.fin());
        Draw.alpha(e.fout());
        Fill.circle(e.x, e.y + e.fin() * 4f * OwnFx.lift, 1.6f * e.fout() + 0.3f);
        blend();
        reset();
    });

    /** gravity-well tether pulse: squashed steel ring */
    public static final Effect tetherRing = new Effect(18f, 120f, e -> {
        Draw.color(steel);
        Draw.alpha(0.7f * e.fout());
        OwnFx.ring(e.x, e.y, e.rotation * 80f * (0.4f + 0.6f * e.fin()), 1.6f * e.fout() + 0.3f);
        reset();
    });

    /** pyroclast footfall: scorched ring, ash and a few embers; e.rotation = unit hit size */
    public static final Effect step = new Effect(36f, 120f, e -> {
        float s = Mathf.clamp(e.rotation / 40f, 0.5f, 1.5f);
        Draw.color(magma, 0.45f * e.fout());
        OwnFx.disc(e.x, e.y, 5f * s * e.fout());
        Draw.color(ember, 0.6f * e.fout());
        OwnFx.ring(e.x, e.y, (4f + e.finpow() * 13f) * s, 2f * e.fout());
        OwnFx.puffs(e, 4, 11f * s, 2.6f * s, ash);
        OwnFx.debris(e, 3, 9f * s, 0.8f, ember);
        reset();
    });

    private EmberEffects(){}
}
