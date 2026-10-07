package astro.content;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;

/** Effects of the Astro Detainer. Plain Draw/Lines calls only: no shaders, safe on Android. */
public final class AstroFx{
    private AstroFx(){}

    public static final Color gold = Color.valueOf("ffd24a"), hot = Color.valueOf("fff7c4"), amber = Color.valueOf("ffa42e");

    /** a projectile is swallowed by a claw: converging sparks and a closing ring */
    public static final Effect absorb = new Effect(26f, 90f, e -> {
        color(e.color, Color.white, e.fout() * 0.6f);
        stroke(e.fout() * 2.2f + 0.3f);
        Lines.circle(e.x, e.y, e.fin(Interp.pow3Out) * 11f);
        Angles.randLenVectors(e.id, 6, 4f + e.fin() * 16f, (x, y) -> {
            float len = 1.5f + e.fout() * 4f;
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y) + 180f, len);
        });
        color(Color.white, e.color, 0.4f);
        Fill.circle(e.x, e.y, e.fout() * 3.2f);
    }).layer(Layer.bullet + 1f);

    /** a projectile is batted back: star burst */
    public static final Effect reflect = new Effect(24f, 100f, e -> {
        color(Color.white, e.color, e.fin());
        stroke(e.fout() * 2.4f);
        for(int i = 0; i < 4; i++){
            lineAngle(e.x, e.y, e.rotation + 45f + i * 90f, 3f + e.fin(Interp.pow2Out) * 12f);
        }
        Lines.circle(e.x, e.y, e.fin(Interp.pow3Out) * 9f);
        Fill.circle(e.x, e.y, e.fout() * 3f);
    }).layer(Layer.bullet + 1f);

    /** lance / overload muzzle */
    public static final Effect dischargeMuzzle = new Effect(34f, 200f, e -> {
        color(e.color, Color.white, e.fout());
        Fill.circle(e.x, e.y, e.fout() * 9f * e.rotation * 0.01f + e.fout() * 3f);
        stroke(e.fout() * 3f);
        Lines.circle(e.x, e.y, e.fin(Interp.pow3Out) * 28f);
        Angles.randLenVectors(e.id, 10, 6f + e.fin() * 30f, (x, y) -> lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 2f + e.fout() * 6f));
    }).layer(Layer.bullet + 1f);

    /** orb muzzle flash */
    public static final Effect orbFlash = new Effect(14f, 60f, e -> {
        color(e.color, Color.white, e.fout());
        Fill.circle(e.x, e.y, 1.5f + e.fout() * 4f);
        stroke(e.fout() * 1.6f);
        Lines.circle(e.x, e.y, e.fin() * 8f);
    }).layer(Layer.bullet + 1f);

    /** magnetic crushing */
    public static final Effect crush = new Effect(22f, 80f, e -> {
        color(Color.white, e.color, e.fin());
        stroke(e.fout() * 2f);
        Angles.randLenVectors(e.id, 8, 2f + e.fin() * 18f, (x, y) -> lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1.5f + e.fout() * 4f));
        Lines.circle(e.x, e.y, e.fin() * 12f);
    }).layer(Layer.bullet + 1f);

    /** energy from a drained shield travelling home */
    public static final Effect drain = new Effect(18f, 60f, e -> {
        color(e.color, Color.white, e.fout() * 0.5f);
        Fill.circle(e.x, e.y, 0.8f + e.fout() * 2.4f);
    }).layer(Layer.bullet + 1f);

    /** blade hit: a bright cut across the victim plus sparks */
    public static final Effect slash = new Effect(20f, 120f, e -> {
        color(Color.white, e.color, e.fin());
        stroke(e.fout() * 3.2f);
        float len = 14f + e.fin(Interp.pow3Out) * 22f;
        lineAngle(e.x - Angles.trnsx(e.rotation + 90f, len * 0.5f), e.y - Angles.trnsy(e.rotation + 90f, len * 0.5f), e.rotation + 90f, len);
        stroke(e.fout() * 1.6f);
        Angles.randLenVectors(e.id, 7, 4f + e.fin(Interp.pow2Out) * 26f, e.rotation, 50f, (x, y) -> lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 2f + e.fout() * 6f));
        Fill.circle(e.x, e.y, e.fout() * 4f);
    }).layer(Layer.bullet + 1f);

    /** the Detainer's final discharge when it is destroyed while charged */
    public static final Effect overloadBlast = new Effect(60f, 500f, e -> {
        color(e.color, Color.white, e.fout() * 0.7f);
        stroke(e.fout() * 6f);
        Lines.circle(e.x, e.y, e.fin(Interp.pow3Out) * e.rotation);
        Fill.circle(e.x, e.y, e.fout() * e.rotation * 0.22f);
        Angles.randLenVectors(e.id, 26, e.fin(Interp.pow2Out) * e.rotation, (x, y) -> lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 3f + e.fout() * 12f));
    }).layer(Layer.bullet + 2f);

    // ---------------------------------------------------------------------------------------------------
    // warp (the Astro Toilets' hyper-speed warp drive: the ring spins up into an orange tornado, a flash and an air blast
    // where it leaves, a bright streak across the map, a flash and an air blast where it arrives)
    // ---------------------------------------------------------------------------------------------------

    /** wind-up: light streams into the ring, which spins faster and tighter, then everything collapses into one point. Follows the unit. */
    public static final Effect warpCharge = new Effect(30f, 420f, e -> {
        float x = e.x, y = e.y;
        if(e.data instanceof Unit u && u.isAdded()){ x = u.x; y = u.y; }
        float f = e.fin(Interp.pow2In), o = 1f - f;
        Draw.blend(Blending.additive);
        Draw.z(Layer.effect + 1f);
        //the spinning warp ring: three tilted loops, radius shrinking from 58 to 8
        float rad = 8f + 50f * o;
        for(int k = 0; k < 3; k++){
            float a0 = e.time * (11f + 26f * f) + k * 120f;
            color(Color.white, amber, 0.25f + 0.5f * o);
            stroke(1.2f + 2.2f * f);
            for(int sg = 0; sg < 18; sg++){
                float u0 = sg / 18f;
                float ang = a0 + u0 * 140f;
                float px = x + Mathf.cosDeg(ang) * rad * 1.1f, py = y + Mathf.sinDeg(ang) * rad * 0.62f + 4f * o;
                float ang2 = a0 + (sg + 1) / 18f * 140f;
                float qx = x + Mathf.cosDeg(ang2) * rad * 1.1f, qy = y + Mathf.sinDeg(ang2) * rad * 0.62f + 4f * o;
                alpha(Mathf.clamp(u0 * 1.4f) * (0.35f + 0.65f * f));
                line(px, py, qx, qy);
            }
        }
        //light pouring into the centre
        for(int i = 0; i < 18; i++){
            float ang = Mathf.randomSeed(e.id * 13L + i, 360f);
            float start = 70f + Mathf.randomSeed(e.id * 29L + i, 130f);
            float r0 = start * o, r1 = Math.max(r0 - 14f - 26f * f, 0f);
            if(r0 - r1 < 0.5f) continue;
            color(gold, Color.white, f);
            alpha(0.2f + 0.8f * f);
            stroke(0.8f + 1.4f * f);
            line(x + Mathf.cosDeg(ang) * r0, y + Mathf.sinDeg(ang) * r0 * 0.85f, x + Mathf.cosDeg(ang) * r1, y + Mathf.sinDeg(ang) * r1 * 0.85f);
        }
        color(amber, 0.25f * f);
        Fill.circle(x, y, 6f + 26f * f);
        color(Color.white, 0.75f * f);
        Fill.circle(x, y, 2f + 7f * f);
        Draw.blend();
        Draw.reset();
    }).followParent(false).layer(Layer.effect + 1f);

    /** leaving: the flash and the air blast at the place the Detainer was */
    public static final Effect warpOut = new Effect(36f, 420f, e -> {
        Draw.blend(Blending.additive);
        color(Color.white, e.color, e.fin(Interp.pow2Out));
        Fill.circle(e.x, e.y, e.fout(Interp.pow3Out) * 46f);
        color(e.color, Color.white, e.fout() * 0.6f);
        stroke(e.fout() * 6f);
        Lines.circle(e.x, e.y, e.fin(Interp.pow3Out) * 150f);
        stroke(e.fout() * 3f);
        Lines.circle(e.x, e.y, e.fin(Interp.pow2Out) * 92f);
        Angles.randLenVectors(e.id, 22, 12f + e.fin(Interp.pow3Out) * 120f, (x, y) -> lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 3f + e.fout() * 14f));
        Draw.blend();
        Draw.reset();
    }).layer(Layer.effect + 1f);

    /** arriving: a brighter flash, light rays and an air blast out to the blast radius */
    public static final Effect warpIn = new Effect(44f, 420f, e -> {
        Draw.blend(Blending.additive);
        color(Color.white, e.color, e.fin(Interp.pow2Out));
        Fill.circle(e.x, e.y, e.fout(Interp.pow4Out) * 64f);
        color(e.color, Color.white, e.fout() * 0.7f);
        stroke(e.fout() * 7f);
        Lines.circle(e.x, e.y, e.fin(Interp.pow3Out) * 125f);
        stroke(e.fout() * 3.4f);
        Lines.circle(e.x, e.y, e.fin(Interp.pow2Out) * 70f);
        //rays
        for(int i = 0; i < 16; i++){
            float ang = i * 22.5f + e.id * 7f;
            float len = 18f + (i % 2 == 0 ? 54f : 30f) * e.fin(Interp.pow3Out);
            stroke(e.fout() * (i % 2 == 0 ? 3.2f : 1.8f));
            color(Color.white, e.color, e.fin());
            line(e.x + Mathf.cosDeg(ang) * 10f, e.y + Mathf.sinDeg(ang) * 10f, e.x + Mathf.cosDeg(ang) * (10f + len), e.y + Mathf.sinDeg(ang) * (10f + len));
        }
        Draw.blend();
        Draw.reset();
    }).layer(Layer.effect + 2f);

    /** the trail: a comet of light from where the Detainer left to where it landed (data = Vec2 destination) */
    public static final Effect warpStreak = new Effect(34f, 14000f, e -> {
        if(!(e.data instanceof Vec2 to)) return;
        float len = Mathf.dst(e.x, e.y, to.x, to.y);
        if(len < 1f) return;
        float ang = Mathf.angle(to.x - e.x, to.y - e.y);
        float head = len * Interp.pow5Out.apply(Mathf.clamp(e.fin() * 2.6f));
        float tail = len * Interp.pow2In.apply(Mathf.clamp((e.fin() - 0.22f) / 0.78f));
        if(tail >= head - 0.5f) return;
        float c = Mathf.cosDeg(ang), s = Mathf.sinDeg(ang);
        Draw.blend(Blending.additive);
        Draw.z(Layer.effect + 1f);
        float w = e.fout(Interp.pow2Out);
        color(amber, 0.30f * w);
        stroke(13f * w + 1f);
        line(e.x + c * tail, e.y + s * tail, e.x + c * head, e.y + s * head, false);
        color(gold, 0.55f * w);
        stroke(6f * w + 0.6f);
        line(e.x + c * tail, e.y + s * tail, e.x + c * head, e.y + s * head, false);
        color(Color.white, 0.95f * w);
        stroke(2.4f * w + 0.4f);
        line(e.x + c * tail, e.y + s * tail, e.x + c * head, e.y + s * head, false);
        //sparks that stay behind along the path
        int n = Mathf.clamp((int)(len / 40f), 6, 60);
        for(int i = 0; i < n; i++){
            float u = Mathf.randomSeed(e.id * 131L + i);
            float pos = len * u;
            if(pos < tail || pos > head) continue;
            float off = (Mathf.randomSeed(e.id * 57L + i) - 0.5f) * 26f * (0.4f + e.fin());
            float fade = Mathf.clamp(1f - (head - pos) / Math.max(head - tail, 1f)) * w;
            color(Color.white, gold, 0.5f);
            alpha(fade);
            Fill.circle(e.x + c * pos - s * off, e.y + s * pos + c * off, 0.8f + 2.4f * fade);
        }
        Draw.blend();
        Draw.reset();
    }).layer(Layer.effect + 1f);
}
