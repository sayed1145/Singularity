package voxel.unit;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import voxel.content.*;
import voxel.gfx.*;

import static voxel.gfx.TitanModel.*;

/**
 * Colossus: a two-legged mech with a fully independent upper body.
 *
 * <h3>Control model (identical to vanilla)</h3>
 * The unit is a plain vanilla {@code MechUnit} with {@code omniMovement}: WASD / the stick / the AI move it in
 * any direction, and vanilla input turns {@code unit.rotation} towards the cursor while shooting (or towards
 * the movement / build target otherwise). This type adds no steering of its own - it only <i>shows</i> it:
 * <ul>
 *   <li>the <b>upper body</b> (torso, head, cannons, backpack) always points exactly at {@code unit.rotation},
 *   full 360 degrees, no clamp;</li>
 *   <li>the <b>lower body</b> turns towards the actual velocity; when moving more than ~115 degrees away from
 *   where the torso faces it walks backwards instead of spinning round (hysteresis to 70 degrees), and
 *   sideways movement is a real strafe;</li>
 *   <li>standing still with the torso twisted past 75 degrees, the legs shuffle round to follow.</li>
 * </ul>
 * The gait is driven by distance travelled along the legs' heading, so planted feet never slide.
 */
public class TitanUnitType extends VoxelUnitType<TitanUnitType.TitanState>{
    public float flyHeight = 20f;
    /** half stride, design units */
    public float stride = 4.6f, lift = 2.6f;
    public float legTurnSpeed = 7f, idleTurnSpeed = 2.4f;
    public float slamRadius = 60f, slamDamage = 180f, slamKnock = 3.2f, slamCooldown = 150f;
    public float boostRange = 300f, landRange = 200f, stuckTime = 60f;
    public Color plumeColor = Color.valueOf("8fe6ff");

    public static class TitanState extends VoxelUnitType.State{
        public float legYaw, gait, amp, lastGait;
        public boolean reversed, turning;
        public float lean, roll, lastVx, lastVy, headYaw;
        public float thrust, impact, slamPulse, lastElev = -1f, logicElev, slamAt = -9999f;
        public float heat, washTimer, sparkTimer;
        public final float[] recoil = new float[6];
        public final int[] shots = new int[6];
        public final Trail[] trails = {new Trail(14), new Trail(14), new Trail(14), new Trail(14)};
        public boolean flying;
        public float stuck, stuckCheck, sx, sy;
    }

    public TitanUnitType(String name){
        super(name);
        constructor = MechUnit::create;
        modelScale = 1.25f;
        omniMovement = true;
        faceTarget = true;
        mechStepParticles = false;
        mechLandShake = 0f;
        stepSound = Sounds.none;
        mechFrontSway = 0f;
        mechSideSway = 0f;
        shadowAlpha = 0.28f;
    }

    @Override
    protected Rig buildRig(){
        return TitanModel.build();
    }

    @Override
    public void restPose(){
        TitanModel.rest(rig, 0f, 0f);
    }

    @Override
    protected TitanState newState(){
        return new TitanState();
    }

    @Override
    public void init(){
        super.init();
        for(var w : weapons){
            if(w instanceof LiftedWeapon l){
                l.cam = renderer.cam;
                l.flyZ = flyHeight;
            }
        }
    }

    // ------------------------------------------------------------------ logic (server safe)

    @Override
    public void update(Unit unit){
        super.update(unit);
        TitanState d = state(unit);
        updateSlam(unit, d);

        //players, logic and RTS command AI drive boosting themselves
        if(unit.isPlayer()) return;
        UnitController con = unit.controller();
        if(con instanceof LogicAI || con instanceof CommandAI) return;

        if(Time.time - d.stuckCheck > 12f){
            d.stuckCheck = Time.time;
            float moved = Mathf.dst(unit.x, unit.y, d.sx, d.sy);
            d.sx = unit.x;
            d.sy = unit.y;
            d.stuck = moved < 1.2f && unit.moving() ? d.stuck + 12f : Math.max(0f, d.stuck - 24f);
        }
        Teamc target = unit.mounts.length > 0 ? unit.mounts[0].target : null;
        float dst = target == null ? -1f : unit.dst(target);
        boolean want = unit.onSolid() || d.stuck > stuckTime || dst > boostRange || (d.flying && dst > landRange);
        d.flying = want;
        unit.updateBoosting(want);
        if(want && unit.elevation > 0.8f) d.stuck = 0f;
    }

    protected void updateSlam(Unit unit, TitanState d){
        float elev = Mathf.clamp(unit.elevation);
        if(d.logicElev > 0.4f && elev <= 0.04f && unit.isGrounded() && Time.time - d.slamAt > slamCooldown){
            d.slamAt = Time.time;
            Damage.damage(unit.team, unit.x, unit.y, slamRadius, slamDamage, false, true, true);
            Units.nearby(null, unit.x, unit.y, slamRadius, u -> {
                if(u.team == unit.team || u.dead || !u.hittable()) return;
                float a = Angles.angle(unit.x, unit.y, u.x, u.y);
                u.velAddNet(Mathf.cosDeg(a) * slamKnock, Mathf.sinDeg(a) * slamKnock);
            });
            VoxelFx.titanSlam.at(unit.x, unit.y, unit.rotation);
            if(!Vars.headless) Effect.shake(4.5f, 26f, unit.x, unit.y);
        }
        d.logicElev = elev;
    }

    // ------------------------------------------------------------------ animation

    @Override
    protected void animate(Unit unit, TitanState d, float delta, boolean payload){
        float elev = Mathf.clamp(unit.elevation);
        float time = Time.time + unit.id * 13f;
        float torso = unit.rotation;
        float spd = unit.vel.len();
        if(!d.init){
            d.legYaw = torso;
            d.lastElev = elev;
        }

        //---------------- lower body heading
        float dx = unit.x - d.lastX, dy = unit.y - d.lastY;
        d.lastX = unit.x;
        d.lastY = unit.y;
        if(dx * dx + dy * dy > 24f * 24f || payload){ dx = 0f; dy = 0f; }
        boolean moving = spd > speed * 0.12f && elev < 0.5f;
        float before = d.legYaw;
        if(delta > 0f){
            if(moving){
                float mv = unit.vel.angle();
                float diff = Math.abs(wrap180(mv - torso));
                if(d.reversed){
                    if(diff < 70f) d.reversed = false;
                }else if(diff > 115f){
                    d.reversed = true;
                }
                d.legYaw = Angles.moveToward(d.legYaw, d.reversed ? mv + 180f : mv, legTurnSpeed * delta);
                d.turning = false;
            }else if(elev < 0.5f){
                float diff = Math.abs(wrap180(torso - d.legYaw));
                if(diff > 75f) d.turning = true;
                if(d.turning){
                    d.legYaw = Angles.moveToward(d.legYaw, torso, idleTurnSpeed * delta);
                    if(diff < 6f) d.turning = false;
                }
            }else{
                //airborne: legs slowly align under the torso
                d.legYaw = Angles.moveToward(d.legYaw, torso, 1.5f * delta);
            }
        }
        float legYaw = d.legYaw;

        //---------------- gait: distance along the leg heading, so planted feet stay planted
        float scale = modelScale * Draw.xscl;
        float fwd = dx * Mathf.cosDeg(legYaw) + dy * Mathf.sinDeg(legYaw);
        d.lastGait = d.gait;
        d.gait += fwd / (4f * stride * scale);
        if(d.turning) d.gait += Math.abs(wrap180(legYaw - before)) / 80f;
        boolean walking = (moving || d.turning) && elev < 0.3f;
        d.amp = Mathf.approach(d.amp, walking ? 1f : 0f, 0.07f * delta);
        float amp = d.amp * (1f - elev);
        float u = d.gait - (float)Math.floor(d.gait);

        //---------------- body dynamics
        float ax = unit.vel.x - d.lastVx, ay = unit.vel.y - d.lastVy;
        if(delta > 0f){ d.lastVx = unit.vel.x; d.lastVy = unit.vel.y; }
        float fx = Mathf.cosDeg(legYaw), fy = Mathf.sinDeg(legYaw);
        float fwdV = unit.vel.x * fx + unit.vel.y * fy, latV = unit.vel.x * fy - unit.vel.y * fx;
        float accel = delta > 0f ? (ax * fx + ay * fy) / delta : 0f;
        float leanT = Mathf.clamp(-fwdV / Math.max(speed, 0.01f) * 3.5f - accel * 60f, -9f, 9f) * (1f - elev)
            - Mathf.clamp(fwdV * 6f, -14f, 14f) * elev;
        float rollT = Mathf.clamp(latV * 5f, -8f, 8f) * elev;
        d.lean = Mathf.lerp(d.lean, leanT, Mathf.clamp(0.1f * delta));
        d.roll = Mathf.lerp(d.roll, rollT, Mathf.clamp(0.1f * delta));

        float rising = delta > 0f ? Mathf.clamp((elev - d.lastElev) / delta * 40f) : 0f;
        d.thrust = Mathf.lerp(d.thrust, Mathf.clamp(elev * 1.2f + rising), Mathf.clamp(0.15f * delta));
        d.impact = Mathf.approach(d.impact, 0f, 0.04f * delta);
        d.slamPulse = Mathf.approach(d.slamPulse, 0f, 0.035f * delta);
        float crouch = d.slamPulse * 2.8f + d.impact * 1.6f;

        //weapons: recoil impulses from shot counters, heat
        float charge = 0f, warm = 0f, open = 0f;
        for(int i = 0; i < unit.mounts.length && i < 6; i++){
            WeaponMount m = unit.mounts[i];
            if(m.totalShots != d.shots[i]){
                if(d.init) d.recoil[i] = 1f;
                d.shots[i] = m.totalShots;
                if(i < 2) d.heat = Math.min(1f, d.heat + 0.25f);
            }
            d.recoil[i] = Mathf.approach(d.recoil[i], 0f, 0.06f * delta);
            if(i < 2){ charge = Math.max(charge, m.charge); warm = Math.max(warm, m.warmup); }
            if(i >= 4) open = Math.max(open, Math.max(Mathf.clamp(d.recoil[i] * 2f), m.warmup));
        }
        d.heat = Mathf.approach(d.heat, 0f, 0.004f * delta);

        //---------------- pelvis
        float bob = -amp * 0.55f * (0.5f + 0.5f * Mathf.cos(u * Mathf.PI2 * 2f));
        float sway = amp * 2.4f * Mathf.sin(u * Mathf.PI2);
        rig.move(PELVIS, 0, 0, bob - crouch);
        rig.rot(PELVIS, 1, sway + d.roll);
        rig.rot(PELVIS, 0, d.lean);

        //---------------- upper body: exactly unit.rotation
        float twist = wrap180(torso - legYaw);
        rig.rot(TORSO, 2, twist);
        rig.rot(TORSO, 1, -sway * 0.7f);
        rig.rot(TORSO, 0, -d.lean * 0.5f + (d.recoil[0] + d.recoil[1]) * 1.4f);
        boolean shooting = unit.isShooting();
        d.headYaw = Mathf.lerp(d.headYaw, shooting ? 0f : Mathf.sin(time, 70f, 10f), Mathf.clamp(0.06f * delta));
        rig.rot(HEAD, 2, d.headYaw);

        for(int i = 0; i < 2; i++){
            float side = i == 0 ? -1f : 1f;
            WeaponMount pod = unit.mounts.length > i ? unit.mounts[i] : null;
            rig.rot(POD[i], 2, pod == null ? 0f : wrap180(pod.rotation));
            rig.rot(POD[i], 0, 2f + warm * 4f);
            rig.moveLocal(BARREL[i], 0, -d.recoil[i] * 1.9f, 0);
            rig.glow[BARREL[i]] = 0.35f + charge * 1.6f + d.heat * 0.5f;

            float swing = amp * 5f * Mathf.sin(u * Mathf.PI2 + (i == 0 ? 0f : Mathf.PI));
            rig.rot(ARM[i], 0, swing - 4f);
            rig.rot(ARM[i], 1, side * 3f);
            rig.rot(GUN[i], 0, -swing + 4f);
            rig.rot(GUN[i], 1, -side * 3f);
            rig.moveLocal(GUNB[i], 0, -d.recoil[2 + i] * 1.3f, 0);

            rig.rot(LID[i], 0, open * 105f);
        }
        rig.glow[PACK] = (0.75f + Mathf.absin(time, 12f, 0.25f)) * (0.6f + 0.4f * unit.healthf()) + d.heat * 0.5f;
        rig.glow[HEAD] = 0.85f + Mathf.absin(time, 20f, 0.15f);
        for(int i = 0; i < 4; i++){
            //nozzles point back; in flight they swivel down and trim against the drift
            rig.rot(THR[i], 0, elev * (85f - Mathf.clamp(fwdV * 12f, -25f, 25f)));
            rig.glow[THR[i]] = 0.2f + d.thrust * 1.1f + Mathf.absin(time + i * 5f, 3f, 0.15f) * d.thrust;
        }

        //---------------- legs: foot targets -> two-bone IK -> pistons
        TitanModel.legs(rig, u, amp, elev, stride, lift);

        renderer.pose(rig, legYaw, 0f, 0f, flyHeight * elev + Mathf.sin(time, 30f, 0.8f) * elev, scale);
    }

    // ------------------------------------------------------------------ extras

    @Override
    protected void drawExtras(Unit unit, TitanState d, float z, boolean step, boolean payload){
        if(payload) return;
        float elev = Mathf.clamp(unit.elevation);
        float time = Time.time + unit.id * 13f;

        //thruster trails
        if(elev > 0.02f){
            Draw.z(z - 0.02f);
            for(int i = 0; i < 4; i++){
                screen(unit, THR[i], 0, -2.3f, 0, v3);
                //stationary hover: let the trail retract instead of piling points on the same spot
                if(step){
                    if(unit.vel.len2() > 0.04f) d.trails[i].update(v3[0], v3[1], (1f + d.thrust * 1.4f) * elev);
                    else d.trails[i].shorten();
                }
                d.trails[i].draw(plumeColor, (i < 2 ? 2.4f : 1.6f) * elev);
            }
        }else if(step){
            for(Trail t : d.trails) t.clear();
        }

        Draw.z(z + 0.02f);
        Draw.blend(Blending.additive);
        if(d.thrust > 0.03f){
            for(int i = 0; i < 4; i++){
                screen(unit, THR[i], 0, -2.4f, 0, v3);
                float rad = (i < 2 ? 2.2f : 1.5f) * d.thrust * (1f + Mathf.absin(time + i * 9f, 4f, 0.25f));
                Draw.color(plumeColor, 0.5f);
                Fill.circle(v3[0], v3[1], rad * 2f);
                Draw.color(Color.white, 0.8f);
                Fill.circle(v3[0], v3[1], rad * 0.9f);
            }
        }
        //shoulder cannon charge
        for(int i = 0; i < 2 && i < unit.mounts.length; i++){
            WeaponMount m = unit.mounts[i];
            float ch = m.charge;
            if(ch > 0.01f){
                screen(unit, BARREL[i], 0, barrelMuzzle, 0, v3);
                Draw.color(plumeColor, 0.55f * ch);
                Fill.circle(v3[0], v3[1], 3f + 5f * ch);
                Draw.color(Color.white, 0.85f * ch);
                Fill.circle(v3[0], v3[1], 1.3f + 2f * ch);
                Draw.color(plumeColor, 0.5f * ch);
                Lines.stroke(1.1f);
                Lines.circle(v3[0], v3[1], 6f + 9f * (1f - ch));
            }
            if(d.recoil[i] > 0.05f){
                screen(unit, BARREL[i], 0, barrelMuzzle, 0, v3);
                Draw.color(VoxelFx.plumeCore, 0.7f * d.recoil[i]);
                Fill.circle(v3[0], v3[1], 4.2f * d.recoil[i]);
            }
        }
        Draw.blend();
        Draw.color();

        if(!step || Vars.headless) return;

        //footfalls: a foot plants when the gait crosses a half cycle
        if(d.amp > 0.4f && elev < 0.05f){
            float a = d.lastGait * 2f, b = d.gait * 2f;
            if(Math.floor(a) != Math.floor(b)){
                int k = (int)Math.floor(Math.max(a, b));
                int foot = (k & 1) == 0 ? 0 : 1;
                ground(unit, FOOT[foot], 0, 1.2f, -ankleZ, v3);
                VoxelFx.titanStep.at(v3[0], v3[1], unit.rotation);
                Effect.shake(1.3f, 5f, v3[0], v3[1]);
                Sounds.mechStepHeavy.at(v3[0], v3[1], Mathf.random(0.85f, 1.05f), 0.4f);
            }
        }

        //takeoff / landing
        if(d.lastElev >= 0f){
            if(d.lastElev < 0.06f && elev >= 0.06f){
                VoxelFx.titanLiftoff.at(unit.x, unit.y);
                Effect.shake(4f, 24f, unit.x, unit.y);
                Sounds.padLaunch.at(unit.x, unit.y, 0.85f, 0.6f);
            }
            if(d.lastElev > 0.05f && elev <= 0.05f){
                VoxelFx.titanLand.at(unit.x, unit.y);
                Effect.shake(5f, 28f, unit.x, unit.y);
                Sounds.padLand.at(unit.x, unit.y, 0.8f, 0.8f);
                d.impact = 1f;
                if(d.lastElev > 0.4f) d.slamPulse = 1f;
            }
        }
        d.lastElev = elev;

        if(d.thrust > 0.06f){
            if(Mathf.chanceDelta(0.5f * d.thrust)){
                int i = Mathf.random(3);
                screen(unit, THR[i], 0, -2.6f, 0, v3);
                float dir = unit.vel.len() > 0.05f ? unit.vel.angle() + 180f : unit.rotation + 180f;
                VoxelFx.titanThrust.at(v3[0], v3[1], dir);
            }
            d.washTimer += Time.delta * d.thrust;
            if(d.washTimer > 9f && elev < 0.9f){
                d.washTimer = 0f;
                VoxelFx.titanDownwash.at(unit.x, unit.y);
            }
        }

        if(d.heat > 0.25f && Mathf.chanceDelta(0.3f * d.heat)){
            int side = Mathf.randomBoolean() ? 0 : 1;
            screen(unit, POD[side], 0, -1.7f, 2.4f, v3);
            VoxelFx.titanVent.at(v3[0], v3[1], unit.rotation + 180f);
        }

        if(unit.healthf() < 0.55f){
            d.sparkTimer += Time.delta * (0.6f - unit.healthf());
            if(d.sparkTimer > 6f){
                d.sparkTimer = 0f;
                int bone = Mathf.randomBoolean() ? TORSO : (Mathf.randomBoolean() ? SHIN[0] : THIGH[1]);
                screen(unit, bone, Mathf.range(3f), Mathf.range(3f), bone == TORSO ? 5f : -3f, v3);
                VoxelFx.titanSpark.at(v3[0], v3[1]);
            }
        }
    }

    @Override
    public void drawLight(Unit unit){
        super.drawLight(unit);
        TitanState d = states.get(unit.id);
        float thrust = d == null ? 0f : d.thrust;
        if(thrust > 0.05f){
            Drawf.light(unit.x, unit.y, (46f + Mathf.absin(Time.time, 9f, 8f)) * thrust, plumeColor, 0.7f * thrust);
        }
        Drawf.light(unit.x, unit.y, 30f, unit.team.color, 0.4f);
    }
}
