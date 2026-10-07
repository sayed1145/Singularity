package astro.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;

/**
 * Laser absorption that keeps the laser alive.
 *
 * <p>The old code called {@code bullet.absorb()} on a beam the moment it touched the containment field: the beam
 * simply vanished, a continuous laser (Meltdown, Lustre...) was deleted after one damage tick so its turret lost the
 * bullet and restarted it (a continuous beam degraded to a flicker of instant ones, or nothing at all).
 *
 * <p>Now, when a hostile beam crosses the field of a Detainer:
 * <ul>
 *   <li>the original bullet object is <b>kept</b> (the turret still owns it, keeps it alive and rotates it), only its
 *   {@code type} is swapped for a harmless {@link Shadow} of the same lifetime: no more damage, no collision;</li>
 *   <li>the shadow redraws the beam in the original colours but <b>cut at the field edge</b>: the tip retracts with a
 *   short bite animation, a white-hot bulb sits on the field, energy streaks spiral down into the nearest claw and
 *   rings converge on the contact point; the claw reaches for the contact point;</li>
 *   <li>continuous beams feed energy every tick for as long as they last (and are thrown back in pulses), instant
 *   beams feed one burst;</li>
 *   <li>if a continuous beam leaves the field (the Detainer moves, the turret turns) the original type is restored
 *   and the beam is a normal laser again.</li>
 * </ul>
 * The swap runs from {@code Trigger.update} and again from {@code Trigger.preDraw}, so a beam created this frame is
 * already cut before its first frame is drawn (no one-frame full-length flash).
 */
public final class BeamAbsorber{
    static final class Beam{
        Bullet b;
        int id;
        BulletType orig;
        Unit det;
        boolean continuous;
        float len0, cut, shown, age, miss, sinkX, sinkY, tipX, tipY, counterT, fx;
        Color[] colors;
        float width;
        float fade = 16f;
        int arm;
        boolean counted;
    }

    static final IntMap<Beam> beams = new IntMap<>();
    static final ObjectMap<BulletType, Shadow> shadows = new ObjectMap<>();
    static final IntMap<Unit> dets = new IntMap<>();
    static final Seq<Beam> tmp = new Seq<>();
    static boolean registered;
    /** statistics for tests */
    public static int swapped, restored, peakAlive;

    private BeamAbsorber(){}

    public static void register(){
        if(registered) return;
        registered = true;
        Events.run(Trigger.update, () -> process(true));
        Events.run(Trigger.preDraw, () -> process(false));
        Events.on(WorldLoadEvent.class, e -> { beams.clear(); dets.clear(); });
    }

    /** called by every live Detainer each tick */
    static void note(Unit unit){
        dets.put(unit.id, unit);
    }

    public static int alive(){
        return beams.size;
    }

    // ---------------------------------------------------------------------------------------------------

    static boolean handles(BulletType t){
        return t instanceof LaserBulletType || t instanceof ContinuousBulletType || t instanceof PointLaserBulletType
            || t instanceof ShrapnelBulletType || t instanceof RailBulletType || t instanceof SapBulletType;
    }

    static boolean isContinuous(BulletType t){
        return t instanceof ContinuousBulletType cb ? cb.continuous : t instanceof PointLaserBulletType;
    }

    static float lengthOf(Bullet b){
        BulletType t = b.type;
        if(t instanceof PointLaserBulletType) return Math.max(b.dst(b.aimX, b.aimY), 16f);
        if(t instanceof ContinuousBulletType cb) return cb.currentLength(b);
        if(b.fdata > 1f) return b.fdata;
        return Math.max(DetainerType.beamLength(t), 40f);
    }

    /** distance along the beam at which it enters the circle, or -1 */
    static float contact(float ox, float oy, float rot, float len, float cx, float cy, float R){
        float vx = Mathf.cosDeg(rot), vy = Mathf.sinDeg(rot);
        float px = cx - ox, py = cy - oy;
        float along = px * vx + py * vy;
        float perp2 = px * px + py * py - along * along;
        if(perp2 > R * R) return -1f;
        float h = (float)Math.sqrt(R * R - perp2);
        float t0 = along - h, t1 = along + h;
        if(t1 < 0f || t0 > len) return -1f;
        return Math.max(t0, 0f);
    }

    static float fieldRadius(Unit u){
        return u.type instanceof DetainerType t ? t.catchRadius * 1.05f : 80f;
    }

    // ---------------------------------------------------------------------------------------------------

    static boolean accrue;

    public static void process(boolean doAccrue){
        accrue = doAccrue;
        //forget detainers that are gone
        if(dets.size > 0 && doAccrue){
            var it = dets.values();
            Seq<Unit> dead = null;
            while(it.hasNext()){
                Unit u = it.next();
                if(u == null || !u.isAdded() || u.dead || !(u.type instanceof DetainerType t) || Time.time - t.logic(u).seen > 12f){
                    if(dead == null) dead = new Seq<>();
                    dead.add(u);
                }
            }
            if(dead != null) for(Unit u : dead) if(u != null) dets.remove(u.id);
        }
        if(dets.size > 0) Groups.bullet.each(BeamAbsorber::consider);
        if(beams.size > 0) updateBeams(doAccrue);
    }

    static void consider(Bullet b){
        BulletType t = b.type;
        if(t == null || t instanceof Shadow || !handles(t) || t.absorbable || b.absorbed || !b.isAdded()) return;
        float len = lengthOf(b);
        float bestT = Float.MAX_VALUE;
        Unit best = null;
        float half = Math.min(widthOf(t), 30f) * 0.3f;
        for(var e : dets){
            Unit u = e.value;
            if(u == null || u.team == b.team || !u.isAdded() || u.dead || !(u.type instanceof DetainerType dt)) continue;
            DetainerType.Logic L = dt.logic(u);
            //a beam is only absorbed where it touches an arm or a claw; otherwise it burns the hull like any other beam
            float c = firstContact(u, dt, L, b, len, half);
            if(c >= 0f){
                if(c < bestT){ bestT = c; best = u; }
            }else if(isContinuous(t)){
                prepare(u, dt, L, b, len);
            }
        }
        if(best == null) return;
        swap(b, best, bestT, len);
    }

    /**
     * Where the beam first touches an arm, or -1. A beam that reaches the hull before it reaches any arm is not absorbed at all
     * (it burns the body and only passes the arms afterwards).
     */
    static float firstContact(Unit det, DetainerType dt, DetainerType.Logic L, Bullet b, float len, float half){
        float c = dt.armBeamContact(L, b.x, b.y, b.rotation(), len, half);
        if(c < 0f) return -1f;
        float h = contact(b.x, b.y, b.rotation(), len, det.x, det.y, det.hitSize * 0.5f);
        if(h >= 0f && h < c - 1f) return -1f;
        return c;
    }

    /** a continuous beam is about to cross the body: the nearest free claw moves into its path (it has to be in the way to absorb it) */
    static void prepare(Unit det, DetainerType dt, DetainerType.Logic L, Bullet b, float len){
        float c = contact(b.x, b.y, b.rotation(), len, det.x, det.y, 70f);
        if(c < 0f) return;
        float hx = b.x + Mathf.cosDeg(b.rotation()) * c, hy = b.y + Mathf.sinDeg(b.rotation()) * c;
        int arm = dt.nearestArm(det, L, hx, hy, true);
        if(arm < 0) return;
        L.tx[arm] = hx; L.ty[arm] = hy; L.tz[arm] = DetainerType.ATZ + 2f; L.tt[arm] = 8f;
        if(L.mode[arm] == DetainerType.IDLE) L.mode[arm] = DetainerType.CATCH;
    }

    static void swap(Bullet b, Unit det, float cut, float len){
        BulletType orig = b.type;
        Beam s = new Beam();
        s.b = b; s.id = b.id; s.orig = orig; s.det = det;
        s.continuous = isContinuous(orig);
        s.len0 = len; s.cut = cut; s.shown = len;
        s.colors = colorsOf(orig);
        s.width = widthOf(orig);
        if(orig instanceof ContinuousLaserBulletType cl) s.fade = cl.fadeTime;
        s.sinkX = det.x; s.sinkY = det.y;
        s.tipX = b.x + Mathf.cosDeg(b.rotation()) * len; s.tipY = b.y + Mathf.sinDeg(b.rotation()) * len;
        beams.put(b.id, s);
        Shadow sh = shadows.get(orig);
        if(sh == null){ sh = new Shadow(orig); shadows.put(orig, sh); }
        b.type = sh;
        swapped++;
        peakAlive = Math.max(peakAlive, beams.size);

        if(det.type instanceof DetainerType dt){
            DetainerType.Logic L = dt.logic(det);
            L.beams++;
            L.absorbed++;
            s.counted = true;
            L.absorbGlow = 1f;
            //first contact: the claw nearest to the contact point reaches for it
            float hx = b.x + Mathf.cosDeg(b.rotation()) * cut, hy = b.y + Mathf.sinDeg(b.rotation()) * cut;
            dt.armBeamContact(L, b.x, b.y, b.rotation(), len, Math.min(widthOf(orig), 30f) * 0.3f);
            s.arm = dt.beamArm >= 0 ? dt.beamArm : dt.nearestArm(det, L, hx, hy, false);
            if(!s.continuous){
                //one burst of energy for an instant beam, part of it is thrown back
                float dmg = b.damage > 0 ? b.damage : orig.damage;
                float gain = Math.min(dmg * dt.absorbScale * 0.5f, dt.maxEnergy * 0.4f) + dt.absorbBonus;
                L.energy = Math.min(dt.maxEnergy, L.energy + gain);
                L.absorbedEnergy += gain;
                //the hit on the hull that already happened when the laser was created is given back
                det.heal(Math.min(dmg, det.maxHealth * 0.05f) * 0.9f);
                counter(det, dt, L, s, dmg);
            }
            reach(det, dt, L, s, hx, hy);
            AstroFx.absorb.at(hx, hy, Angles.angle(hx, hy, det.x, det.y), dt.fieldColor);
        }
    }

    static Color[] colorsOf(BulletType t){
        if(t instanceof LaserBulletType l) return l.colors;
        if(t instanceof ContinuousLaserBulletType c) return c.colors;
        if(t instanceof ContinuousFlameBulletType f) return f.colors;
        Color c = t.hitColor == null ? Color.white : t.hitColor;
        if(t instanceof ShrapnelBulletType s) c = s.toColor;
        if(t instanceof SapBulletType s) c = s.color;
        return new Color[]{new Color(c.r, c.g, c.b, 0.4f), c, Color.white};
    }

    static float widthOf(BulletType t){
        if(t instanceof LaserBulletType l) return l.width;
        if(t instanceof ContinuousLaserBulletType c) return c.width;
        if(t instanceof ContinuousFlameBulletType f) return f.width * 1.2f;
        if(t instanceof ShrapnelBulletType s) return s.width;
        if(t instanceof SapBulletType s) return s.width * 1.6f;
        return 7f;
    }

    static void reach(Unit det, DetainerType dt, DetainerType.Logic L, Beam s, float hx, float hy){
        int arm = s.arm;
        L.flash[arm] = 1f; L.kick[arm] = Math.max(L.kick[arm], 0.6f);
        L.tx[arm] = hx; L.ty[arm] = hy; L.tz[arm] = DetainerType.ATZ + 2f; L.tt[arm] = 8f;
        if(L.mode[arm] == DetainerType.IDLE) L.mode[arm] = DetainerType.CATCH;
    }

    static final float[] mp = new float[2];

    static void counter(Unit det, DetainerType dt, DetainerType.Logic L, Beam s, float dmg){
        if(L.counterCd > 0f || dt.counterBeam == null || !(s.b.owner instanceof Posc src)) return;
        dt.muzzleWorld(det, L, s.arm, mp);
        float ang = Angles.angle(mp[0], mp[1], src.getX(), src.getY());
        dt.counterBeam.create(det, det.team, mp[0], mp[1], ang, Math.min(dmg * 1.2f + 160f, 1800f), 1f, 1f, null);
        AstroFx.reflect.at(mp[0], mp[1], ang, AstroFx.amber);
        L.counterCd = s.continuous ? 50f : 20f;
        L.reflected++;
    }

    static void updateBeams(boolean doAccrue){
        tmp.clear();
        for(var e : beams) tmp.add(e.value);
        for(int k = 0; k < tmp.size; k++){
            Beam s = tmp.get(k);
            Bullet b = s.b;
            if(b == null || !b.isAdded() || b.id != s.id || !(b.type instanceof Shadow)){
                beams.remove(s.id);
                continue;
            }
            Unit det = s.det;
            if(det == null || !det.isAdded() || det.dead || !(det.type instanceof DetainerType dt)){
                restore(s);
                continue;
            }
            DetainerType.Logic L = dt.logic(det);
            //current geometry: the turret may have turned, the Detainer may have moved
            float len = s.continuous ? lengthOf(s, b) : s.len0;
            float c = firstContact(det, dt, L, b, len, Math.min(s.width, 30f) * 0.3f);
            if(c < 0f){
                if(doAccrue) s.miss += Time.delta;
                if(s.continuous && s.miss > 8f){ restore(s); continue; }
            }else{
                s.miss = 0f;
                s.cut = c;
            }
            if(!doAccrue) continue;
            float dt_ = Time.delta;
            s.age += dt_;
            s.len0 = len;
            //bite: the tip retracts towards the field edge (fast at first, then it settles)
            float target = Math.min(s.cut, len);
            s.shown += (target - s.shown) * (1f - (float)Math.pow(0.78f, dt_));
            if(Math.abs(s.shown - target) < 0.4f) s.shown = target;
            float hx = b.x + Mathf.cosDeg(b.rotation()) * s.shown, hy = b.y + Mathf.sinDeg(b.rotation()) * s.shown;
            s.tipX = hx; s.tipY = hy;
            if(c < 0f) continue;
            //claw that eats the beam: it is the one that touches it, and keeps reaching for the contact point
            dt.armBeamContact(L, b.x, b.y, b.rotation(), len, Math.min(s.width, 30f) * 0.3f);
            if(dt.beamArm >= 0) s.arm = dt.beamArm;
            reach(det, dt, L, s, hx, hy);
            dt.muzzleWorld(det, L, s.arm, mp);
            s.sinkX = mp[0]; s.sinkY = mp[1];
            L.absorbGlow = 1f;
            if(s.continuous){
                float dps = b.type instanceof Shadow sh && sh.orig instanceof ContinuousBulletType cb ? cb.damage / cb.damageInterval * 60f : 200f;
                dps *= Math.max(b.damageMultiplier(), 0.1f);
                float gain = Math.min(dps * 0.22f * dt.absorbScale * 0.5f, dt.maxEnergy * 0.25f) * dt_ / 60f * 2f;
                L.energy = Math.min(dt.maxEnergy, L.energy + gain);
                L.absorbedEnergy += gain;
                s.counterT -= dt_;
                if(s.counterT <= 0f){
                    s.counterT = 50f;
                    counter(det, dt, L, s, Math.min(dps * 0.25f, 600f));
                }
            }
            s.fx -= dt_;
            if(s.fx <= 0f){
                s.fx = 4f;
                AstroFx.absorb.at(hx, hy, Angles.angle(hx, hy, s.sinkX, s.sinkY), dt.fieldColor);
            }
        }
    }

    static float lengthOf(Beam s, Bullet b){
        if(s.orig instanceof PointLaserBulletType) return Math.max(b.dst(b.aimX, b.aimY), 16f);
        if(s.orig instanceof ContinuousBulletType cb) return cb.currentLength(b);
        return s.len0;
    }

    static void restore(Beam s){
        beams.remove(s.id);
        Bullet b = s.b;
        if(b != null && b.isAdded() && b.id == s.id && b.type instanceof Shadow){
            b.type = s.orig;
            restored++;
        }
    }

    public static void clear(){
        Seq<Beam> all = new Seq<>();
        for(var e : beams) all.add(e.value);
        for(Beam s : all) restore(s);
        dets.clear();
    }

    // ---------------------------------------------------------------------------------------------------

    /** the drawn-only remainder of an absorbed beam */
    public static class Shadow extends BulletType{
        public final BulletType orig;

        Shadow(BulletType orig){
            this.orig = orig;
            damage = 0f;
            speed = 0f;
            collides = false;
            collidesTiles = false;
            collidesAir = false;
            collidesGround = false;
            collidesTeam = false;
            absorbable = false;
            hittable = false;
            reflectable = false;
            pierce = true;
            keepVelocity = false;
            lifetime = orig.lifetime;
            optimalLifeFract = orig.optimalLifeFract;
            drawSize = Math.max(orig.drawSize, 500f);
            layer = orig.layer;
            hitEffect = Fx.none;
            despawnEffect = Fx.none;
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
            hitSound = Sounds.none;
            despawnSound = Sounds.none;
            lightOpacity = 0f;
            trailLength = 0;
            status = StatusEffects.none;
        }

        @Override public void init(Bullet b){}
        @Override public void init(){}
        @Override public void update(Bullet b){}
        @Override public void hit(Bullet b, float x, float y){}
        @Override public void hit(Bullet b){}
        @Override public void despawned(Bullet b){}
        @Override public void removed(Bullet b){}
        @Override public void drawLight(Bullet b){}
        @Override public float continuousDamage(){ return -1f; }

        @Override
        public void draw(Bullet b){
            Beam s = beams.get(b.id);
            if(s == null) return;
            float rot = b.rotation();
            float f;
            if(s.continuous){
                f = Mathf.clamp(b.time > b.lifetime - s.fade ? 1f - (b.time - (b.lifetime - s.fade)) / s.fade : 1f);
            }else{
                f = b.fout();
            }
            float len = Math.max(s.shown, 0f);
            float tx = b.x + Mathf.cosDeg(rot) * len, ty = b.y + Mathf.sinDeg(rot) * len;
            Color[] cols = s.colors;
            int n = cols.length;
            float osc = s.continuous ? Mathf.absin(Time.time, 0.8f, 1.5f) : 0f;
            float w0 = (s.width * (s.continuous ? 0.55f : 0.45f) + osc) * f;
            float bite = Mathf.clamp((len - Math.min(s.cut, s.len0)) / Math.max(s.len0 * 0.35f, 30f));

            Draw.z(Layer.bullet);
            //beam body, flat on the field: every colour layer a little thinner
            for(int i = 0; i < n; i++){
                Draw.color(Tmp.c1.set(cols[i]).mul(1f + Mathf.absin(Time.time, 1f, 0.1f)));
                float w = w0 * Mathf.lerp(1f, 0.3f, i / (float)Math.max(n - 1, 1)) * (1f + bite * 0.35f);
                Lines.stroke(w);
                Lines.line(b.x, b.y, tx, ty, false);
                Fill.circle(b.x, b.y, w * 0.5f);
            }
            if(len < 6f){ Draw.reset(); return; }

            //funnel: the beam is squeezed into the claw - a cone shrinking towards the sink
            float sx = s.sinkX, sy = s.sinkY;
            float a = Angles.angle(tx, ty, sx, sy);
            Draw.blend(Blending.additive);
            for(int i = 0; i < n; i++){
                Draw.color(cols[i], 0.55f);
                float w = w0 * Mathf.lerp(1f, 0.3f, i / (float)Math.max(n - 1, 1));
                float cone = Math.min(Mathf.dst(tx, ty, sx, sy), 120f) * 0.6f;
                Drawf.tri(tx, ty, w * 1.6f, cone, a);
            }
            //white-hot bulb on the field
            float pulse = 1f + Mathf.absin(Time.time, 3f, 0.25f);
            Draw.color(AstroFx.gold, 0.6f * f);
            Fill.circle(tx, ty, w0 * 1.5f * pulse);
            Draw.color(Color.white, 0.9f * f);
            Fill.circle(tx, ty, w0 * 0.75f * pulse);

            //rings that converge on the contact point
            Lines.stroke(1.6f * f);
            for(int k = 0; k < 3; k++){
                float ph = (Time.time * 0.045f + k / 3f + s.id * 0.13f) % 1f;
                Draw.color(AstroFx.gold, (1f - ph) * 0.8f * f);
                Lines.circle(tx, ty, (1f - ph) * w0 * 3.2f + 2f);
            }

            //energy streaks spiral from the contact point into the claw
            float sw = Mathf.dst(tx, ty, sx, sy);
            float perp = Angles.angle(tx, ty, sx, sy) + 90f;
            for(int k = 0; k < 10; k++){
                float ph = (Time.time * 0.05f + k * 0.1f + s.id * 0.071f) % 1f;
                float e = ph * ph * (3f - 2f * ph);
                float rad = (1f - e) * w0 * 1.4f * Mathf.sin(ph * 10f + k * 1.7f);
                float px = Mathf.lerp(tx, sx, e) + Mathf.cosDeg(perp) * rad;
                float py = Mathf.lerp(ty, sy, e) + Mathf.sinDeg(perp) * rad;
                float size = (1f - ph) * (w0 * 0.22f + 0.8f) + 0.7f;
                Draw.color(k % 2 == 0 ? AstroFx.gold : cols[Math.min(1, n - 1)], 0.9f * f);
                Fill.circle(px, py, size);
                Draw.color(Color.white, 0.8f * f);
                Fill.circle(px, py, size * 0.45f);
            }

            Draw.blend();
            Draw.reset();
        }
    }
}
