package blackhole.g3d;

import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

/**
 * Generic 3D multi-legged walker (titans). A vanilla LegsUnit for movement, physics and pathing (it steps over
 * buildings), but with {@code legCount = 0}: the vanilla leg simulation, its 2D leg drawing and its hardcoded
 * vanilla step effect never run. Instead this type runs its own gait in {@link #update}:
 *
 * <ul>
 * <li>every foot has a rest point on its own ray (restDeg) around the hull, led slightly along the velocity;</li>
 * <li>a planted foot that drifts more than {@link #stride} from that point steps, provided neither neighbour is
 * in the air (alternating tripod / diagonal gait); turning in place re-plants the feet the same way;</li>
 * <li>a landing foot deals the step splash damage, shakes the camera and plays the unit's own step effect.</li>
 * </ul>
 *
 * Drawing: every foot is clamped into its own angular sector ({@link LegGuard}), the knee is solved by planar
 * two-bone IK in the vertical plane through hip and foot, so the legs can never cross. The upper body (weapon
 * yaw bones) turns freely like vanilla, recoil and muzzle flashes follow each mount.
 */
public class Walker3D extends Unit3DType<Walker3D.WalkState>{
    public interface Builder{ Rig build(Walker3D type); }
    public interface Extra{ void animate(Walker3D type, Unit unit, WalkState s, float delta); }
    public interface Glow{ void draw(Walker3D type, Unit unit, WalkState s, float z); }

    public static final int maxLegs = 8;

    public static class LegDef{
        public int femur, tibia, foot;
        public float restDeg, mountR, hipZ, femurLen, tibiaLen;
    }

    public static class WalkState extends Unit3DType.State{
        public final float[] fx = new float[maxLegs], fy = new float[maxLegs], sx = new float[maxLegs], sy = new float[maxLegs];
        /** step progress 0..1 while in the air, -1 when planted */
        public final float[] t = new float[maxLegs];
        public boolean planted;
        public float bob, speedF, walk, warm, lean;
        public final float[] recoil = new float[8];
        public final int[] shots = new int[8];
        public float a, b, c;
    }

    public Builder builder;
    public Extra extra;
    public Glow glow;
    public int BODY;
    public final Seq<LegDef> legDefs = new Seq<>();
    public final IntSeq weaponBone = new IntSeq(), barrelBone = new IntSeq();
    public final FloatSeq muzzle = new FloatSeq();

    /** gait, in design units / ticks */
    public float footR = 20f, stride = 7f, stepTime = 22f, lift = 4.5f, lead = 9f, sectorHalf = 24f;
    public float hullZ = 12f, bobAmount = 0.35f;
    /** own step: damage/range in world units */
    public float stepDamage = 0f, stepRange = 0f, stepShakeAmount = 0f;
    public Sound stepSnd = Sounds.none;
    public float stepVolume = 1f;
    public Effect stepEffect = mindustry.content.Fx.none;
    public Color stepColor = Color.valueOf("c9d1cf");
    public float recoilDist = 1.4f;
    public Color flashColor = Color.white;

    private final float[] tmp = new float[3];

    public Walker3D(String name){
        super(name);
        constructor = LegsUnit::create;
        //vanilla legs off: no 2D legs, no vanilla step Fx; gait + drawing are ours
        legCount = 0;
        hovering = true;
        allowLegStep = true;
        omniMovement = true;
        shadowAlpha = 0.3f;
        shadowLength = 0.5f;
    }

    /** Declares a leg; call from the builder in the order of the legs (restDeg: 0 = forward, 90 = right). */
    public LegDef leg(int femur, int tibia, int foot, float restDeg, float mountR, float hipZ, float femurLen, float tibiaLen){
        LegDef l = new LegDef();
        l.femur = femur; l.tibia = tibia; l.foot = foot;
        l.restDeg = restDeg; l.mountR = mountR; l.hipZ = hipZ; l.femurLen = femurLen; l.tibiaLen = tibiaLen;
        legDefs.add(l);
        return l;
    }

    public void weapon(int mount, int yawBone, int recoilBone, float mx, float my, float mz){
        while(weaponBone.size <= mount){ weaponBone.add(-1); barrelBone.add(-1); muzzle.addAll(0, 0, 0); }
        weaponBone.set(mount, yawBone);
        barrelBone.set(mount, recoilBone);
        muzzle.set(mount * 3, mx);
        muzzle.set(mount * 3 + 1, my);
        muzzle.set(mount * 3 + 2, mz);
    }

    @Override
    protected Rig buildRig(){
        weaponBone.clear(); barrelBone.clear(); muzzle.clear(); legDefs.clear();
        return builder.build(this);
    }

    @Override
    protected WalkState newState(){
        return new WalkState();
    }

    @Override
    public void init(){
        super.init();
        alignWeapons(weaponBone, barrelBone, muzzle, 0f);
    }

    // ------------------------------------------------------------------ gait (server + client)

    /** world position of leg i's rest foot point (with velocity lead) */
    private void restFoot(Unit unit, LegDef l, float leadX, float leadY, float[] out){
        float a = unit.rotation - l.restDeg; //restDeg is clockwise from forward
        out[0] = unit.x + Mathf.cosDeg(a) * footR * modelScale + leadX;
        out[1] = unit.y + Mathf.sinDeg(a) * footR * modelScale + leadY;
    }

    @Override
    public void update(Unit unit){
        super.update(unit);
        ensureRig();
        WalkState s = state(unit);
        int n = Math.min(legDefs.size, maxLegs);
        float leadX = unit.vel.x * lead, leadY = unit.vel.y * lead;
        if(!s.planted){
            for(int i = 0; i < n; i++){
                restFoot(unit, legDefs.get(i), 0f, 0f, tmp);
                s.fx[i] = tmp[0]; s.fy[i] = tmp[1]; s.t[i] = -1f;
            }
            s.planted = true;
        }
        float spd = Math.max(speed, 0.01f);
        float moving = Mathf.clamp(unit.vel.len() / spd);
        float dur = stepTime / (0.75f + 0.5f * moving);
        float threshold = stride * modelScale;
        int airborne = 0;
        for(int i = 0; i < n; i++) if(s.t[i] >= 0f) airborne++;

        for(int i = 0; i < n; i++){
            LegDef l = legDefs.get(i);
            restFoot(unit, l, leadX, leadY, tmp);
            if(s.t[i] >= 0f){
                s.t[i] += Time.delta / dur;
                float f = Interp.smoother.apply(Mathf.clamp(s.t[i]));
                s.fx[i] = Mathf.lerp(s.sx[i], tmp[0], f);
                s.fy[i] = Mathf.lerp(s.sy[i], tmp[1], f);
                if(s.t[i] >= 1f){
                    s.t[i] = -1f;
                    land(unit, s.fx[i], s.fy[i]);
                }
            }else{
                float d = Mathf.dst(s.fx[i], s.fy[i], tmp[0], tmp[1]);
                //a foot nearing the edge of its sector (body turned / strafed under it) counts as drifted too
                if(d > threshold * 0.4f && nearEdge(unit, l, s.fx[i], s.fy[i])) d = Math.max(d, threshold * 1.01f);
                int prev = (i + n - 1) % n, next = (i + 1) % n;
                boolean neighboursDown = s.t[prev] < 0f && s.t[next] < 0f;
                //teleports / respawns: snap
                if(d > threshold * 6f){
                    s.fx[i] = tmp[0]; s.fy[i] = tmp[1];
                }else if(d > threshold && neighboursDown && airborne < (n + 1) / 2){
                    s.t[i] = 0f;
                    s.sx[i] = s.fx[i]; s.sy[i] = s.fy[i];
                    airborne++;
                }
            }
        }
        s.walk += Time.delta * moving;
    }

    private boolean nearEdge(Unit unit, LegDef l, float fx, float fy){
        float cr = Mathf.cosDeg(unit.rotation), sr = Mathf.sinDeg(unit.rotation);
        float dx = (fx - unit.x) / modelScale, dy = (fy - unit.y) / modelScale;
        float ly = dx * cr + dy * sr, lx = dx * sr - dy * cr;
        float hx = Mathf.sinDeg(l.restDeg) * l.mountR, hy = Mathf.cosDeg(l.restDeg) * l.mountR;
        float vx = lx - hx, vy = ly - hy;
        float ang = (float)Math.toDegrees(Math.atan2(vx, vy)); //0 = forward, 90 = right, like restDeg
        float dev = Math.abs(wrap180(ang - l.restDeg));
        float reach = (l.femurLen + l.tibiaLen) * 0.97f;
        float rMax = (float)Math.sqrt(Math.max(reach * reach - l.hipZ * l.hipZ, 1f));
        return dev > sectorHalf * 0.8f || Mathf.len(vx, vy) > rMax * 0.95f;
    }

    private void land(Unit unit, float x, float y){
        if(stepDamage > 0f && !unit.disarmed && Vars.state != null && Vars.state.rules != null){
            Damage.damage(unit.team, x, y, stepRange, stepDamage * Vars.state.rules.unitDamage(unit.team), false, true);
        }
        if(!Vars.headless && (Vars.player == null || !unit.inFogTo(Vars.player.team()))){
            stepEffect.at(x, y, hitSize, stepColor);
            stepSnd.at(x, y, 1f + Mathf.range(0.08f), stepVolume);
            if(stepShakeAmount > 0f) Effect.shake(stepShakeAmount, stepShakeAmount, x, y);
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void animate(Unit unit, WalkState s, float delta, boolean payload){
        float scale = modelScale * Draw.xscl;
        float spd = Math.max(speed, 0.01f);
        s.speedF = Mathf.lerp(s.speedF, Mathf.clamp(unit.vel.len() / spd), Mathf.clamp(0.12f * delta));
        int n = Math.min(legDefs.size, maxLegs);

        //body bob: dips a little each time a foot is in the air
        float air = 0f;
        for(int i = 0; i < n; i++) if(s.t[i] >= 0f) air = Math.max(air, Mathf.slope(s.t[i]));
        float bob = payload || !s.planted ? 0f : (-bobAmount * 0.5f + air * bobAmount) * s.speedF;
        s.bob = Mathf.lerp(s.bob, bob, Mathf.clamp(0.25f * delta + (delta == 0f ? 0f : 0.01f)));
        rig.move(BODY, 0f, 0f, s.bob);

        float cr = Mathf.cosDeg(unit.rotation), sr = Mathf.sinDeg(unit.rotation);
        for(int i = 0; i < n; i++){
            LegDef l = legDefs.get(i);
            float lx, ly, lz = 0f;
            if(payload || !s.planted){
                lx = Mathf.sinDeg(l.restDeg) * footR;
                ly = Mathf.cosDeg(l.restDeg) * footR;
            }else{
                float dx = (s.fx[i] - unit.x) / modelScale, dy = (s.fy[i] - unit.y) / modelScale;
                //world -> body frame: y along the heading, x to its right
                ly = dx * cr + dy * sr;
                lx = dx * sr - dy * cr;
                if(s.t[i] >= 0f) lz = Mathf.slope(Mathf.clamp(s.t[i])) * lift;
            }
            solveLeg(l, sectorHalf, lx, ly, lz, s.bob, sol);
            pose(l, sol);
        }

        for(int i = 0; i < unit.mounts.length && i < weaponBone.size; i++){
            WeaponMount m = unit.mounts[i];
            if(m.totalShots != s.shots[i]){
                if(s.init) s.recoil[i] = 1f;
                s.shots[i] = m.totalShots;
            }
            s.recoil[i] = Mathf.approach(s.recoil[i], 0f, 0.05f * delta);
            int wb = weaponBone.get(i);
            if(wb >= 0 && m.weapon.rotate) rig.rot(wb, 2, wrap180(m.rotation));
            int bb = barrelBone.get(i);
            if(bb >= 0){
                rig.moveLocal(bb, 0, -s.recoil[i] * recoilDist * Math.max(m.weapon.recoil / 2f, 0.5f), 0);
                rig.glow[bb] = 0.75f + s.recoil[i] * 0.6f;
            }
            if(i == 0) s.warm = Mathf.approach(s.warm, m.charging ? 1f : 0f, 0.04f * delta);
        }
        rig.glow[BODY] = 0.8f + 0.2f * unit.healthf();
        if(extra != null) extra.animate(this, unit, s, delta);
        renderer.pose(rig, unit.rotation, 0f, 0f, 0f, scale);
    }

    private final float[] sol = new float[9];
    private static final float[] T3 = new float[3];

    /**
     * Solves one leg in the body frame (x right, y forward, z up): the foot target is clamped into the leg's own
     * sector and reach, the knee lies in the vertical plane through hip and foot, bulging up and outwards.
     * out = {hip xyz, knee xyz, foot xyz}. Pure math, shared by drawing and the regression test.
     */
    public static void solveLeg(LegDef l, float sectorHalf, float lx, float ly, float lz, float bob, float[] out){
        float hx = Mathf.sinDeg(l.restDeg) * l.mountR, hy = Mathf.cosDeg(l.restDeg) * l.mountR, hz = l.hipZ + bob;
        //the sector's apex is the leg's own hip: rays from neighbouring hips on the rim diverge, so legs never cross
        float reach = (l.femurLen + l.tibiaLen) * 0.97f, dz = hz - lz;
        float rMax = (float)Math.sqrt(Math.max(reach * reach - dz * dz, 1f));
        float rMin = Math.min(Math.abs(l.femurLen - l.tibiaLen) + 2f, rMax * 0.5f);
        float[] t = T3;
        LegGuard.clamp(lx - hx, ly - hy, l.restDeg, sectorHalf, rMin, rMax, t);
        lx = hx + t[0]; ly = hy + t[1];
        float ox = lx - hx, oy = ly - hy, ol = (float)Math.sqrt(ox * ox + oy * oy);
        if(ol < 1e-3f){ ox = Mathf.sinDeg(l.restDeg); oy = Mathf.cosDeg(l.restDeg); ol = 1f; }
        ox /= ol; oy /= ol;
        Rig.knee(hx, hy, hz, lx, ly, lz, l.femurLen, l.tibiaLen, ox * 0.45f, oy * 0.45f, 1f, t);
        out[0] = hx; out[1] = hy; out[2] = hz;
        out[3] = t[0]; out[4] = t[1]; out[5] = t[2];
        out[6] = lx; out[7] = ly; out[8] = lz;
    }

    private void pose(LegDef l, float[] o){
        float ox = o[6] - o[0], oy = o[7] - o[1], ol = Mathf.len(ox, oy);
        if(ol < 1e-3f){ ox = Mathf.sinDeg(l.restDeg); oy = Mathf.cosDeg(l.restDeg); ol = 1f; }
        ox /= ol; oy /= ol;
        rig.aim(l.femur, o[0], o[1], o[2], o[3], o[4], o[5], -oy, ox, 0f);
        rig.aim(l.tibia, o[3], o[4], o[5], o[6], o[7], o[8], -oy, ox, 0f);
        rig.place(l.foot, o[6], o[7], o[8]);
    }

    @Override
    public void restPose(){
        super.restPose();
        //legs standing at rest (icons, LOD bake, weapon alignment)
        for(LegDef l : legDefs){
            solveLeg(l, sectorHalf, Mathf.sinDeg(l.restDeg) * footR, Mathf.cosDeg(l.restDeg) * footR, 0f, 0f, sol);
            pose(l, sol);
        }
    }

    @Override
    protected void drawExtras(Unit unit, WalkState s, float z, boolean step, boolean payload){
        if(payload) return;
        Draw.z(z + 0.015f);
        Draw.blend(Blending.additive);
        for(int i = 0; i < unit.mounts.length && i < barrelBone.size; i++){
            if(s.recoil[i] > 0.3f && barrelBone.get(i) >= 0){
                screen(unit, barrelBone.get(i), muzzle.get(i * 3), muzzle.get(i * 3 + 1), muzzle.get(i * 3 + 2), v3);
                Draw.color(flashColor, s.recoil[i]);
                Fill.circle(v3[0], v3[1], 2.4f * s.recoil[i] * modelScale + 0.6f);
                Draw.color(Color.white, s.recoil[i] * 0.8f);
                Fill.circle(v3[0], v3[1], 1.1f * s.recoil[i] * modelScale + 0.3f);
            }
        }
        if(glow != null) glow.draw(this, unit, s, z);
        Draw.blend();
        Draw.color();
    }
}
