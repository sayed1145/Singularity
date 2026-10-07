package voxel.unit;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import voxel.gfx.*;

import static voxel.gfx.DroneModel.*;

/**
 * Tessellus: a vanilla flying {@code UnitEntity} drawn as a 3D quad-duct drone. The hull yaws with
 * {@code unit.rotation}, banks and pitches with its real velocity, each duct tilts into the direction of
 * travel, rotors spin up with thrust, the belly turret follows its weapon mount.
 */
public class TessellusUnitType extends VoxelUnitType<TessellusUnitType.DroneState>{
    public float hoverHeight = 5.2f;
    public float rotorSpeed = 21f;
    public float bankScale = 9f, pitchScale = 6f, ductTilt = 22f;
    public Color engineColor = Color.valueOf("7ff0ff");

    public static class DroneState extends VoxelUnitType.State{
        public float pitch, roll, spin, recoil, lastShots, ductX, ductY;
        public int shots, barrel;
    }

    public TessellusUnitType(String name){
        super(name);
        constructor = UnitEntity::create;
        flying = true;
        lowAltitude = false;
        modelScale = 0.82f;
        shadowAlpha = 0.22f;
        shadowLength = 0.6f;
    }

    @Override
    protected Rig buildRig(){
        return DroneModel.build();
    }

    @Override
    protected DroneState newState(){
        return new DroneState();
    }

    @Override
    public void init(){
        super.init();
        for(var w : weapons){
            if(w instanceof LiftedWeapon l){
                l.cam = renderer.cam;
                l.flyZ = hoverHeight;
            }
        }
    }

    @Override
    protected void animate(Unit unit, DroneState d, float delta, boolean payload){
        float time = Time.time + unit.id * 17f;
        float elev = Mathf.clamp(unit.elevation);
        float scale = modelScale * Draw.xscl;
        float fx = Mathf.cosDeg(unit.rotation), fy = Mathf.sinDeg(unit.rotation);
        float fwd = unit.vel.x * fx + unit.vel.y * fy, lat = unit.vel.x * fy - unit.vel.y * fx;
        float spd = Math.max(speed, 0.01f);

        d.pitch = Mathf.lerp(d.pitch, Mathf.clamp(-fwd / spd * pitchScale, -14f, 14f), Mathf.clamp(0.12f * delta));
        d.roll = Mathf.lerp(d.roll, Mathf.clamp(lat / spd * bankScale, -20f, 20f), Mathf.clamp(0.12f * delta));
        d.ductX = Mathf.lerp(d.ductX, Mathf.clamp(-fwd / spd, -1f, 1f) * ductTilt, Mathf.clamp(0.1f * delta));
        d.ductY = Mathf.lerp(d.ductY, Mathf.clamp(lat / spd, -1f, 1f) * ductTilt, Mathf.clamp(0.1f * delta));
        float thrust = 0.55f + 0.45f * Mathf.clamp(unit.vel.len() / spd);
        d.spin += rotorSpeed * thrust * delta;

        rig.move(BODY, 0, 0, Mathf.sin(time, 26f, 0.5f));
        rig.rot(BODY, 0, d.pitch + Mathf.cos(time, 37f, 1.0f));
        rig.rot(BODY, 1, d.roll + Mathf.sin(time, 42f, 1.2f));

        for(int i = 0; i < 4; i++){
            rig.rot(DUCT[i], 0, d.ductX);
            rig.rot(DUCT[i], 1, d.ductY);
            rig.rot(ROTOR[i], 2, (i == 0 || i == 3 ? 1f : -1f) * d.spin + i * 40f);
            rig.alpha[BLUR[i]] = 0.12f + 0.12f * thrust;
            rig.glow[BLUR[i]] = 1f;
        }

        WeaponMount m = unit.mounts.length > 0 ? unit.mounts[0] : null;
        if(m != null){
            if(m.totalShots != d.shots){
                if(d.init){
                    d.recoil = 1f;
                    d.barrel = 1 - d.barrel;
                }
                d.shots = m.totalShots;
            }
            rig.rot(TURRET, 2, wrap180(m.rotation));
        }
        d.recoil = Mathf.approach(d.recoil, 0f, 0.1f * delta);
        rig.moveLocal(BARREL[d.barrel], 0, -d.recoil * 0.9f, 0);
        rig.glow[BARREL[d.barrel]] = 0.7f + d.recoil * 0.8f;
        rig.glow[BARREL[1 - d.barrel]] = 0.7f;

        //navigation lights: short double blink, alternating sides
        float t = (time % 90f) / 90f;
        boolean on0 = t < 0.06f || (t > 0.12f && t < 0.18f), on1 = t > 0.5f && t < 0.56f;
        rig.glow[NAV[0]] = on0 ? 1.2f : 0.3f;
        rig.glow[NAV[1]] = on1 ? 1.2f : 0.35f;
        rig.glow[BODY] = 0.85f + 0.15f * unit.healthf();

        renderer.pose(rig, unit.rotation, 0f, 0f, hoverHeight * elev, scale);
    }

    @Override
    protected void drawExtras(Unit unit, DroneState d, float z, boolean step, boolean payload){
        if(payload) return;
        float elev = Mathf.clamp(unit.elevation);
        if(elev < 0.05f) return;
        Draw.z(z + 0.015f);
        Draw.blend(Blending.additive);
        float time = Time.time + unit.id * 17f;
        float thrust = 0.45f + 0.55f * Mathf.clamp(unit.vel.len() / Math.max(speed, 0.01f));
        for(int i = 0; i < 4; i++){
            screen(unit, DUCT[i], 0, 0, -0.9f, v3);
            float rad = (1.3f + Mathf.absin(time + i * 7f, 5f, 0.4f)) * thrust * elev;
            Draw.color(engineColor, 0.28f);
            Fill.circle(v3[0], v3[1], rad * 1.9f);
        }
        if(d.recoil > 0.3f){
            screen(unit, BARREL[d.barrel], 0, muzzle, 0, v3);
            Draw.color(engineColor, d.recoil);
            Fill.circle(v3[0], v3[1], 1.8f * d.recoil);
        }
        Draw.blend();
        Draw.color();
    }

    @Override
    public void drawLight(Unit unit){
        super.drawLight(unit);
        Drawf.light(unit.x, unit.y, 22f, engineColor, 0.35f);
    }
}
