package astro.g3d;

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
 * Generic 3D aircraft: a vanilla flying unit drawn from a {@link Rig}. The hull yaws with {@code unit.rotation},
 * banks into turns and pitches with acceleration, bobs while hovering; weapon bones follow their mounts with
 * recoil; engines glow with thrust. Unit specific motion goes into {@link #extra}.
 */
public class Ship3D extends Unit3DType<Ship3D.ShipState>{
    public interface Builder{ Rig build(Ship3D type); }
    public interface Extra{ void animate(Ship3D type, Unit unit, ShipState s, float delta); }

    public static class ShipState extends Unit3DType.State{
        public float pitch, roll, spin, speedF, turn, lastRot;
        public final float[] recoil = new float[8];
        public final int[] shots = new int[8];
        public float a, b, c, d; //free slots for unit specific animation
    }

    public Builder builder;
    public Extra extra;
    public int BODY;
    /** per mount index: bone yawed to the mount rotation (-1 = none) and bone recoiled on shots (-1 = none) */
    public final IntSeq weaponBone = new IntSeq(), barrelBone = new IntSeq();
    /** muzzle offsets in the barrel bone frame per mount (x, y, z) */
    public final FloatSeq muzzle = new FloatSeq();
    /** engine glow points: bone, x, y, z, radius */
    public final FloatSeq engines = new FloatSeq();
    public float hover = 6f, bankScale = 14f, pitchScale = 7f, recoilDist = 1.0f;
    public Color engineColor = Color.valueOf("8fe9ff"), flashColor = Color.valueOf("ffffff");

    public Ship3D(String name){
        super(name);
        constructor = UnitEntity::create;
        flying = true;
        lowAltitude = false;
        shadowAlpha = 0.22f;
        shadowLength = 0.62f;
    }

    public void weapon(int mount, int yawBone, int recoilBone, float mx, float my, float mz){
        while(weaponBone.size <= mount){ weaponBone.add(-1); barrelBone.add(-1); muzzle.addAll(0, 0, 0); }
        weaponBone.set(mount, yawBone);
        barrelBone.set(mount, recoilBone);
        muzzle.set(mount * 3, mx);
        muzzle.set(mount * 3 + 1, my);
        muzzle.set(mount * 3 + 2, mz);
    }

    public void engine(int bone, float x, float y, float z, float r){
        engines.addAll(bone, x, y, z, r);
    }

    @Override
    protected Rig buildRig(){
        weaponBone.clear(); barrelBone.clear(); muzzle.clear(); engines.clear();
        return builder.build(this);
    }

    @Override
    protected ShipState newState(){
        return new ShipState();
    }

    @Override
    public void init(){
        super.init();
        //pose() lifts the model by hover world units (after scaling)
        alignWeapons(weaponBone, barrelBone, muzzle, hover);
    }

    @Override
    protected void animate(Unit unit, ShipState d, float delta, boolean payload){
        float time = Time.time + unit.id * 17f;
        float elev = payload ? 0f : Mathf.clamp(unit.elevation);
        float scale = modelScale * Draw.xscl;
        float fx = Mathf.cosDeg(unit.rotation), fy = Mathf.sinDeg(unit.rotation);
        float fwd = unit.vel.x * fx + unit.vel.y * fy, lat = unit.vel.x * fy - unit.vel.y * fx;
        float spd = Math.max(speed, 0.01f);
        float turn = delta > 0f ? Mathf.clamp(Angles.angleDist(d.lastRot, unit.rotation) * Mathf.sign(wrap180(unit.rotation - d.lastRot)) / Math.max(delta, 0.01f) / 4f, -1f, 1f) : d.turn;
        d.lastRot = unit.rotation;
        d.turn = Mathf.lerp(d.turn, turn, Mathf.clamp(0.1f * delta));
        d.speedF = Mathf.lerp(d.speedF, Mathf.clamp(unit.vel.len() / spd), Mathf.clamp(0.1f * delta));
        d.pitch = Mathf.lerp(d.pitch, Mathf.clamp(-fwd / spd * pitchScale * 0.4f, -10f, 10f), Mathf.clamp(0.1f * delta));
        d.roll = Mathf.lerp(d.roll, Mathf.clamp(lat / spd * bankScale - d.turn * bankScale, -24f, 24f), Mathf.clamp(0.1f * delta));
        d.spin += delta * (0.6f + d.speedF);

        rig.move(BODY, 0, 0, Mathf.sin(time, 28f, 0.35f) * elev);
        rig.rot(BODY, 0, d.pitch + Mathf.cos(time, 37f, 0.8f) * elev);
        rig.rot(BODY, 1, d.roll + Mathf.sin(time, 43f, 0.9f) * elev);

        for(int i = 0; i < unit.mounts.length && i < weaponBone.size; i++){
            WeaponMount m = unit.mounts[i];
            if(m.totalShots != d.shots[i]){
                if(d.init) d.recoil[i] = 1f;
                d.shots[i] = m.totalShots;
            }
            d.recoil[i] = Mathf.approach(d.recoil[i], 0f, 0.08f * delta);
            int wb = weaponBone.get(i);
            if(wb >= 0 && m.weapon.rotate) rig.rot(wb, 2, wrap180(m.rotation));
            int bb = barrelBone.get(i);
            if(bb >= 0){
                rig.moveLocal(bb, 0, -d.recoil[i] * recoilDist, 0);
                rig.glow[bb] = 0.75f + d.recoil[i] * 0.6f;
            }
        }
        rig.glow[BODY] = 0.8f + 0.2f * unit.healthf();
        if(extra != null) extra.animate(this, unit, d, delta);
        renderer.pose(rig, unit.rotation, 0f, 0f, hover * elev, scale);
    }

    @Override
    protected void drawExtras(Unit unit, ShipState d, float z, boolean step, boolean payload){
        if(payload) return;
        float elev = Mathf.clamp(unit.elevation);
        float time = Time.time + unit.id * 17f;
        Draw.z(z + 0.015f);
        Draw.blend(Blending.additive);
        float thrust = 0.45f + 0.55f * d.speedF;
        for(int i = 0; i + 4 < engines.size; i += 5){
            screen(unit, (int)engines.get(i), engines.get(i + 1), engines.get(i + 2), engines.get(i + 3), v3);
            float r = engines.get(i + 4) * modelScale * (0.8f + Mathf.absin(time + i * 3f, 3f, 0.25f)) * thrust * Math.max(elev, 0.3f);
            Draw.color(engineColor, 0.35f);
            Fill.circle(v3[0], v3[1], r * 1.8f);
            Draw.color(Color.white, 0.55f);
            Fill.circle(v3[0], v3[1], r * 0.7f);
        }
        for(int i = 0; i < unit.mounts.length && i < barrelBone.size; i++){
            if(d.recoil[i] > 0.35f && barrelBone.get(i) >= 0){
                screen(unit, barrelBone.get(i), muzzle.get(i * 3), muzzle.get(i * 3 + 1), muzzle.get(i * 3 + 2), v3);
                Draw.color(flashColor, d.recoil[i]);
                Fill.circle(v3[0], v3[1], 1.6f * d.recoil[i] * modelScale + 0.4f);
            }
        }
        Draw.blend();
        Draw.color();
    }

    @Override
    public void drawLight(Unit unit){
        super.drawLight(unit);
        Drawf.light(unit.x, unit.y, hitSize * 1.6f, engineColor, 0.3f);
    }
}
