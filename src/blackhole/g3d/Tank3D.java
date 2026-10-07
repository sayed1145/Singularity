package blackhole.g3d;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

/**
 * Generic 3D tracked vehicle: a vanilla TankUnit drawn from a {@link Rig}. The hull follows {@code unit.rotation};
 * tread links run around their loop with the distance travelled (each side separately, so pivot turns counter-
 * rotate the tracks), road wheels spin with them, the hull pitches on acceleration and rocks on firing, turrets
 * follow their mounts with barrel recoil. Unit specific motion goes into {@link #extra}.
 */
public class Tank3D extends Unit3DType<Tank3D.TankState>{
    public interface Builder{ Rig build(Tank3D type); }
    public interface Extra{ void animate(Tank3D type, Unit unit, TankState s, float delta); }

    public static class TankState extends Unit3DType.State{
        public float pitch, roll, spin, speedF, lastRot, left, right, warm;
        public final float[] recoil = new float[8];
        public final int[] shots = new int[8];
        public float a, b, c, d;
    }

    /** one track: link bones and loop geometry */
    public static class Track{
        public float x, len, rTop, zMid;
        public final IntSeq links = new IntSeq(), wheels = new IntSeq();
        public float wheelR;
    }

    public Builder builder;
    public Extra extra;
    public int BODY;
    public final IntSeq weaponBone = new IntSeq(), barrelBone = new IntSeq();
    public final FloatSeq muzzle = new FloatSeq();
    public final Seq<Track> tracks = new Seq<>();
    public float recoilDist = 1.2f, rockScale = 1.5f;
    public Color flashColor = Color.valueOf("ffffff");

    public Tank3D(String name){
        super(name);
        constructor = TankUnit::create;
        omniMovement = false;
        shadowAlpha = 0.3f;
        shadowLength = 0.3f;
    }

    public void weapon(int mount, int yawBone, int recoilBone, float mx, float my, float mz){
        while(weaponBone.size <= mount){ weaponBone.add(-1); barrelBone.add(-1); muzzle.addAll(0, 0, 0); }
        weaponBone.set(mount, yawBone);
        barrelBone.set(mount, recoilBone);
        muzzle.set(mount * 3, mx);
        muzzle.set(mount * 3 + 1, my);
        muzzle.set(mount * 3 + 2, mz);
    }

    /**
     * Adds a tread loop at x (centre line of the track) with the given straight length, loop radius (half height),
     * width, number of links, link colour style and road wheels. The loop's lowest point touches z = 0.
     */
    public Track track(Rig r, int body, float x, float len, float rad, float width, int links, int wheels, Kit.Style s){
        Track t = new Track();
        t.x = x; t.len = len; t.rTop = rad; t.zMid = rad; t.wheelR = rad * 0.8f;
        for(int i = 0; i < links; i++){
            int b = r.bone(body, x, 0, rad);
            Mesh m = r.part(b);
            Kit.dark(m, s).box(-width / 2f, -0.55f, -0.22f, width / 2f, 0.55f, 0.22f);
            r.detail();
            t.links.add(b);
        }
        for(int i = 0; i < wheels; i++){
            float y = -len / 2f + len * i / Math.max(wheels - 1, 1);
            int b = r.bone(body, x, y, rad);
            Mesh m = r.part(b);
            Kit.steel(m, s).at(0, 0, 0).rot(1, 90).lathe(8, 22.5f, t.wheelR, -width * 0.42f, t.wheelR, width * 0.42f);
            t.wheels.add(b);
        }
        tracks.add(t);
        return t;
    }

    @Override
    protected Rig buildRig(){
        weaponBone.clear(); barrelBone.clear(); muzzle.clear(); tracks.clear();
        return builder.build(this);
    }

    @Override
    protected TankState newState(){
        return new TankState();
    }

    @Override
    public void init(){
        super.init();
        alignWeapons(weaponBone, barrelBone, muzzle, 0f);
    }

    static void placeLink(Rig rig, Track t, int b, float s){
        //loop: bottom run (front->back as s grows), rear arc, top run, front arc; perimeter P
        float r = t.rTop, L = t.len, P = 2f * L + Mathf.PI * 2f * r;
        s = ((s % P) + P) % P;
        float y, z, ang;
        if(s < L){ y = L / 2f - s; z = -r; ang = 0f; }
        else if(s < L + Mathf.PI * r){ float a = (s - L) / r; y = -L / 2f - Mathf.sin(a) * r; z = -Mathf.cos(a) * r; ang = a * Mathf.radDeg; }
        else if(s < 2f * L + Mathf.PI * r){ y = -L / 2f + (s - L - Mathf.PI * r); z = r; ang = 180f; }
        else{ float a = (s - 2f * L - Mathf.PI * r) / r; y = L / 2f + Mathf.sin(a) * r; z = Mathf.cos(a) * r; ang = 180f + a * Mathf.radDeg; }
        rig.place(b, t.x, y, t.zMid + z);
        rig.rot(b, 0, -ang);
    }

    @Override
    protected void animate(Unit unit, TankState d, float delta, boolean payload){
        float scale = modelScale * Draw.xscl;
        float fx = Mathf.cosDeg(unit.rotation), fy = Mathf.sinDeg(unit.rotation);
        float dx = unit.x - d.lastX, dy = unit.y - d.lastY;
        if(!d.init || dx * dx + dy * dy > 400f){ dx = 0; dy = 0; }
        d.lastX = unit.x; d.lastY = unit.y;
        float fwd = (dx * fx + dy * fy) / modelScale;
        float turn = d.init ? wrap180(unit.rotation - d.lastRot) : 0f;
        d.lastRot = unit.rotation;
        float spd = Math.max(speed, 0.01f);
        d.speedF = Mathf.lerp(d.speedF, delta > 0 ? Mathf.clamp(Math.abs(fwd) / Math.max(delta, 0.01f) / spd * modelScale) : d.speedF, Mathf.clamp(0.15f * delta));
        d.pitch = Mathf.lerp(d.pitch, Mathf.clamp(-fwd * 6f, -2.5f, 2.5f), Mathf.clamp(0.1f * delta));
        d.roll = Mathf.lerp(d.roll, Mathf.clamp(turn * 0.6f, -2f, 2f), Mathf.clamp(0.1f * delta));
        d.spin += delta;

        //per-side tread travel: forward plus the turn's arc length at the track's x
        for(int ti = 0; ti < tracks.size; ti++){
            Track t = tracks.get(ti);
            float travel = fwd + turn * Mathf.degRad * t.x;
            if(t.x < 0) d.left += travel; else d.right += travel;
        }

        rig.rot(BODY, 0, d.pitch);
        rig.rot(BODY, 1, d.roll);

        float rock = 0f;
        for(int i = 0; i < unit.mounts.length && i < weaponBone.size; i++){
            WeaponMount m = unit.mounts[i];
            if(m.totalShots != d.shots[i]){
                if(d.init) d.recoil[i] = 1f;
                d.shots[i] = m.totalShots;
            }
            d.recoil[i] = Mathf.approach(d.recoil[i], 0f, 0.06f * delta);
            rock = Math.max(rock, d.recoil[i] * m.weapon.recoil * 0.3f);
            int wb = weaponBone.get(i);
            if(wb >= 0 && m.weapon.rotate) rig.rot(wb, 2, wrap180(m.rotation));
            int bb = barrelBone.get(i);
            if(bb >= 0){
                rig.moveLocal(bb, 0, -d.recoil[i] * recoilDist * Math.max(m.weapon.recoil / 2f, 0.5f), 0);
                rig.glow[bb] = 0.75f + d.recoil[i] * 0.6f;
            }
            if(i == 0) d.warm = m.warmup;
        }
        rig.rot(BODY, 0, -rock * rockScale);

        for(int ti = 0; ti < tracks.size; ti++){
            Track t = tracks.get(ti);
            float travel = t.x < 0 ? d.left : d.right;
            float P = 2f * t.len + Mathf.PI * 2f * t.rTop;
            for(int k = 0; k < t.links.size; k++){
                placeLink(rig, t, t.links.get(k), travel + P * k / t.links.size);
            }
            for(int k = 0; k < t.wheels.size; k++){
                rig.rot(t.wheels.get(k), 0, -travel / t.wheelR * Mathf.radDeg);
            }
        }
        rig.glow[BODY] = 0.8f + 0.2f * unit.healthf();
        if(extra != null) extra.animate(this, unit, d, delta);
        renderer.pose(rig, unit.rotation, 0f, 0f, 0f, scale);
    }

    @Override
    public void restPose(){
        super.restPose();
        for(Track t : tracks){
            float P = 2f * t.len + Mathf.PI * 2f * t.rTop;
            for(int k = 0; k < t.links.size; k++) placeLink(rig, t, t.links.get(k), P * k / t.links.size);
        }
    }

    @Override
    protected void drawExtras(Unit unit, TankState d, float z, boolean step, boolean payload){
        if(payload) return;
        Draw.z(z + 0.015f);
        Draw.blend(Blending.additive);
        for(int i = 0; i < unit.mounts.length && i < barrelBone.size; i++){
            if(d.recoil[i] > 0.35f && barrelBone.get(i) >= 0){
                screen(unit, barrelBone.get(i), muzzle.get(i * 3), muzzle.get(i * 3 + 1), muzzle.get(i * 3 + 2), v3);
                Draw.color(flashColor, d.recoil[i]);
                Fill.circle(v3[0], v3[1], 2.2f * d.recoil[i] * modelScale + 0.5f);
            }
        }
        Draw.blend();
        Draw.color();
    }
}
