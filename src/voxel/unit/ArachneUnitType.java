package voxel.unit;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import voxel.gfx.*;

import static voxel.gfx.SpiderModel.*;

/**
 * Arachne: a vanilla {@code LegsUnit} (4 legs) drawn as a 3D walker.
 *
 * <p>Feet are exactly the engine's own {@code Leg.base} positions (so splash damage, wall stepping and footstep
 * effects line up with what you see); the model only adds the third dimension: step lift, two-bone IK with
 * high knees, hull bob and lean. The hull follows {@code baseRotation} (movement), the turret follows
 * {@code unit.rotation} (aim) through a full 360 degrees - the same split as vanilla leg units.
 */
public class ArachneUnitType extends VoxelUnitType<ArachneUnitType.ArachneState>{
    public float stepLift = 5.5f;
    public Color muzzleColor = Color.valueOf("ffd27f");

    public static class ArachneState extends VoxelUnitType.State{
        public final float[] recoil = new float[4];
        public final int[] shots = new int[4];
        public final float[] spin = new float[2], spinV = new float[2];
        public float bob, pitch, roll, lastVx, lastVy;
    }

    public ArachneUnitType(String name){
        super(name);
        constructor = LegsUnit::create;
        modelScale = 1f;
        omniMovement = true;
        faceTarget = true;
        legCount = 4;
        allowLegStep = true;
        hovering = true;
        shadowAlpha = 0.24f;
        shadowLength = 0.5f;
    }

    @Override
    protected Rig buildRig(){
        return SpiderModel.build();
    }

    @Override
    public void restPose(){
        SpiderModel.rest(rig);
    }

    @Override
    protected ArachneState newState(){
        return new ArachneState();
    }

    @Override
    public void init(){
        super.init();
        for(var w : weapons){
            if(w instanceof LiftedWeapon l) l.cam = renderer.cam;
        }
    }

    @Override
    protected void animate(Unit unit, ArachneState d, float delta, boolean payload){
        float time = Time.time + unit.id * 11f;
        float scale = modelScale * Draw.xscl;
        float base = unit instanceof Legsc l ? l.baseRotation() : unit.rotation;
        Leg[] legs = unit instanceof Legsc l ? l.legs() : null;

        //weapons
        for(int i = 0; i < unit.mounts.length && i < 4; i++){
            WeaponMount m = unit.mounts[i];
            if(m.totalShots != d.shots[i]){
                if(d.init) d.recoil[i] = 1f;
                d.shots[i] = m.totalShots;
            }
            d.recoil[i] = Mathf.approach(d.recoil[i], 0f, (i == 0 ? 0.035f : 0.12f) * delta);
        }
        for(int k = 0; k < 2; k++){
            //mount 1 is the right gun (x > 0), mount 2 its mirrored left twin
            WeaponMount m = unit.mounts.length > 2 - k ? unit.mounts[2 - k] : null;
            boolean firing = m != null && (m.shoot || m.reload > 0f && unit.isShooting());
            d.spinV[k] = Mathf.approach(d.spinV[k], firing ? 26f : 0f, (firing ? 1.4f : 0.35f) * delta);
            d.spin[k] = (d.spin[k] + d.spinV[k] * delta) % 360f;
        }

        //hull dynamics: bob with the legs, lean into acceleration
        float lifted = 0f, fl = 0f, fr = 0f, ff = 0f, fb = 0f;
        if(legs != null && legs.length == 4){
            for(int i = 0; i < 4; i++){
                float l = legs[i].moving ? Mathf.sin(legs[i].stage * Mathf.PI) : 0f;
                lifted += l;
                if(mountX[i] < 0) fl += l; else fr += l;
                if(mountY[i] > 0) ff += l; else fb += l;
            }
        }
        float ax = unit.vel.x - d.lastVx, ay = unit.vel.y - d.lastVy;
        if(delta > 0f){ d.lastVx = unit.vel.x; d.lastVy = unit.vel.y; }
        float fx = Mathf.cosDeg(base), fy = Mathf.sinDeg(base);
        float acc = delta > 0f ? (ax * fx + ay * fy) / delta : 0f;
        float lat = delta > 0f ? (ax * fy - ay * fx) / delta : 0f;
        d.bob = Mathf.lerp(d.bob, -lifted * 0.35f + Mathf.sin(time, 40f, 0.25f), Mathf.clamp(0.2f * delta));
        d.pitch = Mathf.lerp(d.pitch, (fb - ff) * 1.6f - acc * 40f - d.recoil[0] * 2.5f, Mathf.clamp(0.15f * delta));
        d.roll = Mathf.lerp(d.roll, (fr - fl) * 1.6f + lat * 40f, Mathf.clamp(0.15f * delta));

        rig.move(HULL, 0, 0, d.bob);
        rig.rot(HULL, 0, Mathf.clamp(d.pitch, -7f, 7f));
        rig.rot(HULL, 1, Mathf.clamp(d.roll, -7f, 7f));
        rig.glow[EYE[0]] = 0.75f + Mathf.absin(time, 16f, 0.3f);
        rig.glow[HULL] = 0.8f + Mathf.absin(time, 9f, 0.2f);

        //turret: the full aim, 360 degrees
        rig.rot(TURRET, 2, wrap180(unit.rotation - base));
        rig.rot(CANNON, 0, 1.5f + d.recoil[0] * 4f);
        rig.moveLocal(CBARREL, 0, -d.recoil[0] * 2.4f, 0);
        for(int k = 0; k < 2; k++){
            rig.rot(MGB[k], 1, d.spin[k]);
            rig.moveLocal(MGB[k], 0, -d.recoil[2 - k] * 0.35f, 0);
        }
        WeaponMount podMount = unit.mounts.length > 3 ? unit.mounts[3] : null;
        float podWarm = podMount == null ? 0f : Math.max(podMount.warmup, d.recoil[3]);
        rig.rot(POD, 2, podMount == null ? 0f : wrap180(podMount.rotation) * 0.5f);
        rig.rot(POD, 0, 6f + podWarm * 16f);

        //legs: the engine's own foot positions, lifted while stepping
        float sb = Mathf.sinDeg(base), cb = Mathf.cosDeg(base);
        if(legs != null && legs.length == 4 && !payload){
            for(int i = 0; i < 4; i++){
                Leg leg = legs[i];
                float rx = leg.base.x - unit.x, ry = leg.base.y - unit.y;
                float tz = leg.moving ? Mathf.sin(leg.stage * Mathf.PI) * stepLift : 0f;
                //sector-guarded: planted feet that lag behind a turning hull never cross a neighbouring leg
                SpiderModel.guardedLeg(rig, i, (rx * sb - ry * cb) / scale, (rx * cb + ry * sb) / scale, tz);
            }
        }else{
            SpiderModel.stance(rig, legLength * 0.85f / Math.max(modelScale, 0.01f));
        }

        renderer.pose(rig, base, 0f, 0f, 0f, scale);
    }

    @Override
    protected void drawExtras(Unit unit, ArachneState d, float z, boolean step, boolean payload){
        if(payload) return;
        Draw.z(z + 0.02f);
        Draw.blend(Blending.additive);
        if(d.recoil[0] > 0.3f){
            screen(unit, CBARREL, 0, cannonMuzzle + 0.8f, 0, v3);
            float a = (d.recoil[0] - 0.3f) / 0.7f;
            Draw.color(muzzleColor, 0.7f * a);
            Fill.circle(v3[0], v3[1], 5f * a);
        }
        for(int k = 0; k < 2; k++){
            float r = d.recoil[2 - k];
            if(r > 0.4f){
                screen(unit, MGB[k], 0, mgMuzzle + 0.4f, 0, v3);
                Draw.color(muzzleColor, 0.8f * r);
                Fill.circle(v3[0], v3[1], 2.2f * r);
            }
        }
        Draw.blend();
        Draw.color();
    }

    @Override
    public void drawLight(Unit unit){
        super.drawLight(unit);
        Drawf.light(unit.x, unit.y, 24f, Color.valueOf("ff5a4a"), 0.3f);
    }
}
