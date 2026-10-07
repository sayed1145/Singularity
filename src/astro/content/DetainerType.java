package astro.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import astro.g3d.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.meta.*;

import static astro.content.DetainerModel.*;

/**
 * Astro Detainer: a hovering jailer with three identical, telescopic magnet arms.
 *
 * <p>Every arm can do everything: catch hostile projectiles and beams (absorbing them as stored energy or
 * batting them back, boosted by the charge), throw energy orbs, fire the discharge lance, stab with a blade
 * that slides out between the jaws, slash in a wide arc, and grab small enemies, drag them in and crush them
 * while absorbing their life. The arms are real stretchable chains: upper arm and forearm lengthen or shorten
 * so the claw can reach any point in range, and every claw can roll around its own axis.
 *
 * <p>The whole pose is a pure function of the per-unit {@link Logic} advanced in {@link #update}, so the drawn
 * claws are exactly where projectiles are caught, blades cut and shots leave. Effects are only shown while
 * energy is actually being absorbed.
 */
public class DetainerType extends Unit3DType<DetainerType.DState>{
    /** design units -> world units (the whole unit is this much bigger than the first version) */
    public static final float SC = 1.6f;
    /** projection camera identical to the renderer's */
    public static final Cam cam = new Cam(HALF * SC, 7.5f, 3.0f);

    //arm modes
    public static final int IDLE = 0, CATCH = 1, STAB = 2, SLASH = 3, GRAB = 4, HOLD = 5;
    /** abilities a pilot can own (bit = 1 << id) */
    public static final int AB_STAB = 0, AB_SLASH = 1, AB_GRAB = 2, AB_ORB = 3, AB_LANCE = 4, AB_OVERLOAD = 5, AB_AEGIS = 6, AB_DRAIN = 7, AB_WARP = 8, AB_RELEASE = 9, AB_N = 10;
    public static final int WARP_AIM = 0, WARP_ENEMY = 1, WARP_HOME = 2;
    /** ticks to fill a whole warp charge, ticks of the wind-up before the jump */
    public static final float WARP_FILL = 600f, WARP_WIND = 26f;
    /** body frame height at which claws work on ground targets */
    public static final float ATZ = -5f;
    /** farthest the jaw centre can be from its shoulder (fully stretched arm), body frame */
    public static final float REACH = (U + F) * KMAX * 0.95f + S + J - 1.5f;
    /** the arms can stretch this far for a long grab (telescopic rods slide out far beyond the normal reach) */
    public static final float KLONG = 4.6f;
    public static final float LONGREACH = (U + F) * KLONG * 0.95f + S + J - 1.5f;
    /** attack cone: claws only work on targets inside +-CONE degrees of the heading (the body turns to face them first) */
    public static final float CONE = 120f;
    /**
     * strike zone of each arm (degrees from the heading, positive = right): a claw only works on what it can really reach.
     * Left claw: front-left and front, right claw: front and front-right, back claw: the front centre. Nothing behind.
     */
    /** margin a target needs inside an arm's zone before a strike may start / tolerance while it is under way */
    public static final float START = -5f, UNDERWAY = 10f;
    public static final float[][] ZONE = {{-85f, 28f}, {-28f, 85f}, {-55f, 55f}};

    public float maxEnergy = 3000f, regen = 0.15f;
    /** aegis (energy shield against what the claws cannot absorb): capacity, instant burst, regeneration per tick, shield points per energy, minimum energy */
    public float aegisMax = 3200f, aegisBurst = 1400f, aegisRegen = 10f, aegisRatio = 3f, aegisMinEnergy = 260f, aegisRange = 300f;
    /** projectiles closer than this (ground distance) are caught; claws start tracking at senseRadius */
    public float catchRadius = 76f, senseRadius = 170f;
    public float absorbScale = 1.2f, absorbBonus = 2f;
    public float reflectChance = 0.4f, reflectDamageBoost = 0.5f, reflectSpeed = 1.2f;
    public float hover = 10f;
    /** the AI of an unpiloted Detainer warps next to far enemies; damage / radius of the arrival blast */
    public boolean aiWarp = true;
    public float warpBlastDamage = 160f, warpBlastRadius = 120f;
    public float drainRange = 260f, drainPerPulse = 55f;
    public float grabRange = 120f, maxGrabSize = 52f, crushDps = 200f, crushTime = 150f, grabCost = 240f;
    public float lanceMin = 900f, lanceTake = 1300f, lanceBase = 260f, lanceScale = 0.4f, overloadDamage = 2200f;
    public float orbCost = 12f, orbBase = 46f, orbBonus = 1.3f;
    /** melee: engage ranges (world units from the unit centre), damage, energy gained per damage dealt */
    public float longGrabRange = 250f, turnRate = 3.4f, faceRange = 340f;
    public float stabRange = 150f, slashRange = 105f, stabDamage = 260f, slashDamage = 180f, meleeGain = 0.25f, crushGain = 0.35f;
    public BulletType orb, counter, lance, overload, counterBeam;
    public Color fieldColor = AstroFx.gold;

    public static class DState extends Unit3DType.State{
        public float pitch, roll, speedF, lastRot, turn;
        /** decorative gyro ring angle (degrees) */
        public float coreSpin;
    }

    /** All animation and gameplay state of one Detainer. */
    public static class Logic{
        /** test hook: beam absorber statistics */
        public String beamStat(){ return "swapped=" + BeamAbsorber.swapped + " restored=" + BeamAbsorber.restored + " alive=" + BeamAbsorber.alive() + " peakAlive=" + BeamAbsorber.peakAlive; }

        public float energy, seen, time, absorbGlow;
        public boolean init;
        /** body frame, 3 claws x (x,y,z): smoothed muzzle goal / facing; resolved muzzle; bracket; wrist; knee */
        public final float[] muz = new float[9], dir = new float[9], rmuz = new float[9], tip = new float[9], wrist = new float[9], knee = new float[9];
        public final float[] open = new float[3], charge = new float[3], flash = new float[3], kick = new float[3];
        /** claw roll angle / spin speed, blade extension, arm stretch factor */
        public final float[] roll = new float[3], spin = new float[3], blade = new float[3], stretch = {1f, 1f, 1f};
        /** current maximum stretch factor of each arm (rises to KLONG for a long grab) */
        public final float[] kmax = {KMAX, KMAX, KMAX};
        /** planned slash arc: start / end angle (body frame, degrees), muzzle radius, sweep duration */
        public final float[] slA0 = new float[3], slA1 = new float[3], slR = new float[3], slDur = {18f, 18f, 18f};
        public final boolean[] longGrab = new boolean[3];
        public Teamc faceTarget;
        public float faceT;
        public int longGrabs, slashMulti;
        /** the slash arc planned for each arm is inside its zone, reachable and covers the target */
        public final boolean[] slashOk = new boolean[3];
        /** bullet each claw is tracking (sticky: no flicking between bullets) */
        public final int[] trackId = {-1, -1, -1};
        /** strikes aborted before the damage window (target left the zone / died) and strikes that hit nothing */
        public int aborts, whiffs, strikes;
        /** motion quality of the claws (for tests): jumps of more than 26 units in one tick, and back-and-forth reversals of the claw's motion */
        public int teleports, reversals;
        public float maxStepSeen;
        final float[] prM = new float[9], prS = new float[9], prLen = new float[3];
        boolean prInit;
        public final int[] armStrikes = new int[3];
        /** world-space polyline of every arm as drawn: shoulder, knee, wrist, claw head, jaw tip (x,y each) */
        public final float[] aw = new float[30];
        /** per-arm cooldown between two catches, catches made, and the largest gap between a caught bullet and the arm surface (must stay ~0) */
        public final float[] catchCd = new float[3];
        public int catches, hullLeaks;
        public float maxCatchGap, lastCatchGap;
        /** aegis shield state */
        public boolean aegisOn;
        public float aegisHold, aegisCd, aegisVis, aegisHit, threatT, hurtT, lastHp, lastShield, aegisAbsorbed;
        public int aegisUps, aegisBreaks;
        /** world target a claw is reaching for (bullets) and how long it stays */
        public final float[] tx = new float[3], ty = new float[3], tz = new float[3], tt = new float[3];
        /** per arm state machine */
        public final int[] mode = new int[3], atkId = new int[3], combo = new int[3], held = {-1, -1, -1};
        public final float[] modeT = new float[3], cd = {0f, 14f, 28f}, holdT = new float[3], grabCd = new float[3], crushFx = new float[3];
        public final float[] gx = new float[3], gy = new float[3], sgn = {1f, -1f, 1f};
        public final boolean[] hitDone = new boolean[3];
        public final Building[] atkBuild = new Building[3];
        public final IntSet[] hits = {new IntSet(), new IntSet(), new IntSet()};
        public final IntSet beamsSeen = new IntSet();
        public final IntFloatMap thrown = new IntFloatMap();
        public float drainT, counterCd, overloadFlash;
        //---- v1.6 pilot control and warp
        public boolean pro, orbOn;
        /** abilities the pilot owns (AI may not use them while he pilots the unit): bit = 1 << AB_x */
        public int manual = 1 << AB_WARP;
        /** pending commands (ticks left), last failure reason and the reason shown to the pilot (+ its timer) */
        public final float[] req = new float[AB_N], whyT = new float[AB_N];
        public final int[] fail = new int[AB_N], why = new int[AB_N];
        public Teamc reqT;
        public mindustry.ai.types.CommandAI stanceAI;
        public float warpCharge = 0.5f, warpCd, warpWind, aiWarpCd = 150f, arrive, depart, aegisManual, drainBurst, lastAct, lastWarpDist, hold;
        public int warpKind, warps, aiWarps, acts;
        public float warpAx, warpAy, wFromX, wFromY, wToX, wToY;
        //statistics (for tests and the info panel)
        public int absorbed, reflected, drained, grabs, lances, overloads, healedHits, stabs, slashes, beams, meleeHits;
        public float absorbedEnergy, healedHp;
        public int heldCount(){
            int n = 0;
            for(int h : held) if(h != -1) n++;
            return n;
        }
    }

    protected final IntMap<Logic> logics = new IntMap<>();
    private float lastPrune;
    private final float[] p0 = new float[3], p1 = new float[3], p2 = new float[3];
    final float[] p3 = new float[3];

    public DetainerType(String name){
        super(name);
        constructor = UnitEntity::create;
        flying = true;
        lowAltitude = false;
        modelScale = SC;
        shadowLength = 0.4f;
        shadowAlpha = 0.2f;
        hitSize = 64f;
        health = 38000f;
        armor = 16f;
        speed = 0.66f;
        accel = 0.035f;
        drag = 0.05f;
        //the body is turned by the claws' own logic (turn to the nearest enemy, else along the movement), never by the pilot
        rotateSpeed = 0f;
        faceTarget = false;
        range = 110f;
        targetAir = true;
        targetGround = true;
        engineSize = 0f;
        buildSpeed = 0f;
        outlines = false;
        drawCell = false;
        allowedInPayloads = false;
        lodSize = 250f;
        neverLod = true;
        detailPpu = 0.9f;
        drawShields = false; //the aegis dome is drawn by drawAegis
        clipSize = 900f; //arms reach 4.6x and the beams even further: never frustum-cull while any part is on screen
        targetFlags = new BlockFlag[]{BlockFlag.turret, BlockFlag.generator, BlockFlag.battery};
    }

    @Override
    public void ensureRig(){
        boolean fresh = rig == null;
        super.ensureRig();
        if(fresh) renderer.cam = cam;
    }

    @Override
    protected Rig buildRig(){
        return DetainerModel.build();
    }

    @Override
    protected DState newState(){
        return new DState();
    }

    // ===================================================================================================
    // geometry: body frame <-> world
    // ===================================================================================================

    public float hoverOf(Unit u){
        return hover * Mathf.clamp(u.elevation);
    }

    /** body frame point -> world position as drawn by the perspective camera. out = {x, y} */
    public void toWorld(Unit u, float bx, float by, float bz, float[] out){
        float yaw = u.rotation - 90f, c = Mathf.cosDeg(yaw), s = Mathf.sinDeg(yaw);
        float rx = (bx * c - by * s) * SC, ry = (bx * s + by * c) * SC, zz = (BZ + bz) * SC + hoverOf(u);
        out[0] = u.x + cam.sx(rx, zz);
        out[1] = u.y + cam.sy(ry, zz);
    }

    /** world position seen on screen at body height bz -> body frame (x, y); out[2] = bz */
    public void toBody(Unit u, float wx, float wy, float bz, float[] out){
        float yaw = u.rotation - 90f, c = Mathf.cosDeg(yaw), s = Mathf.sinDeg(yaw);
        float zz = (BZ + bz) * SC + hoverOf(u), k = (cam.D - zz) / cam.D;
        float rx = (wx - u.x) * k, ry = cam.cy + (wy - u.y - cam.cy) * k;
        out[0] = (rx * c + ry * s) / SC;
        out[1] = (-rx * s + ry * c) / SC;
        out[2] = bz;
    }

    public void muzzleWorld(Unit u, Logic L, int i, float[] out){
        toWorld(u, L.rmuz[i * 3], L.rmuz[i * 3 + 1], L.rmuz[i * 3 + 2], out);
    }

    /** world end of the stab blade of claw i (world x,y of the blade tip when fully out) */
    public void bladeTipWorld(Unit u, Logic L, int i, float len, float[] out){
        int o = i * 3;
        toWorld(u, L.rmuz[o] + L.dir[o] * len, L.rmuz[o + 1] + L.dir[o + 1] * len, L.rmuz[o + 2] + L.dir[o + 2] * len, out);
    }

    // ===================================================================================================
    // state
    // ===================================================================================================

    public Logic logic(Unit unit){
        Logic L = logics.get(unit.id);
        if(L == null){
            logics.put(unit.id, L = new Logic());
            restPose(L);
            L.energy = maxEnergy * 0.12f;
        }
        L.seen = Time.time;
        return L;
    }

    public boolean hasLogic(Unit unit){
        return logics.containsKey(unit.id);
    }

    static final float[][] REST_MUZ = {{-40f, 36f, 4f}, {40f, 36f, 4f}, {0f, 22f, 34f}};
    static final float[][] REST_DIR = {{-0.45f, 0.85f, -0.15f}, {0.45f, 0.85f, -0.15f}, {0f, 0.95f, -0.3f}};

    void restPose(Logic L){
        for(int i = 0; i < 3; i++){
            for(int k = 0; k < 3; k++){
                L.muz[i * 3 + k] = REST_MUZ[i][k];
                L.dir[i * 3 + k] = REST_DIR[i][k];
            }
            L.open[i] = 0.45f;
            resolve(L, i);
        }
        L.init = true;
    }

    // ===================================================================================================
    // arm solver (shared by the drawn pose and the projectile spawn point): telescopic two-bone chain
    // ===================================================================================================

    static final float[] hint = new float[3];

    /** clamps the smoothed claw goal to what the stretched arm can reach and solves the elbow. */
    public void resolve(Logic L, int i){
        float[] s = SHOULDER[i];
        int o = i * 3;
        float dx = L.dir[o], dy = L.dir[o + 1], dz = L.dir[o + 2];
        float dl = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if(!(dl > 1e-4f)){ dx = 0; dy = 1; dz = 0; dl = 1f; }
        dx /= dl; dy /= dl; dz /= dl;
        float ext = J - 1.5f;
        float wx = L.muz[o] - dx * (ext + S), wy = L.muz[o + 1] - dy * (ext + S), wz = L.muz[o + 2] - dz * (ext + S);
        float vx = wx - s[0], vy = wy - s[1], vz = wz - s[2];
        float d = (float)Math.sqrt(vx * vx + vy * vy + vz * vz);
        if(Float.isNaN(d)){
            vx = REST_MUZ[i][0] - s[0]; vy = REST_MUZ[i][1] - s[1]; vz = REST_MUZ[i][2] - s[2];
            d = (float)Math.sqrt(vx * vx + vy * vy + vz * vz);
        }
        float base = U + F;
        float maxR = 0.985f * base * L.kmax[i], minR = 0.22f * base;
        if(d > maxR || d < minR){
            float k = (d > maxR ? maxR : minR) / Math.max(d, 1e-3f);
            vx *= k; vy *= k; vz *= k;
            d = Math.min(Math.max(d, minR), maxR);
        }
        //telescoping: stretch when the goal is beyond the rest length, shorten when it is close
        float k = 1f;
        if(d > 0.94f * base) k = d / (0.94f * base);
        else if(d < 0.6f * base) k = Mathf.clamp(d / (0.6f * base), 0.74f, 1f);
        L.stretch[i] = k;
        float a = U * k, b = F * k;
        wx = s[0] + vx; wy = s[1] + vy; wz = s[2] + vz;
        L.wrist[o] = wx; L.wrist[o + 1] = wy; L.wrist[o + 2] = wz;
        if(i == 2){ hint[0] = 0f; hint[1] = -0.2f; hint[2] = 1f; }
        else{ hint[0] = i == 0 ? -0.45f : 0.45f; hint[1] = -1f; hint[2] = 0.35f; }
        float hl = (float)Math.sqrt(hint[0] * hint[0] + hint[1] * hint[1] + hint[2] * hint[2]);
        Rig.knee(s[0], s[1], s[2], wx, wy, wz, a, b, hint[0] / hl, hint[1] / hl, hint[2] / hl, p0);
        L.knee[o] = p0[0]; L.knee[o + 1] = p0[1]; L.knee[o + 2] = p0[2];
        //the knee solver clamps unreachable targets; keep the wrist exactly b from the knee
        float kx = wx - p0[0], ky = wy - p0[1], kz = wz - p0[2];
        float kl = (float)Math.sqrt(kx * kx + ky * ky + kz * kz);
        if(kl > 1e-4f && Math.abs(kl - b) > 0.02f){
            float q = b / kl;
            wx = p0[0] + kx * q; wy = p0[1] + ky * q; wz = p0[2] + kz * q;
            L.wrist[o] = wx; L.wrist[o + 1] = wy; L.wrist[o + 2] = wz;
        }
        L.tip[o] = wx + dx * S; L.tip[o + 1] = wy + dy * S; L.tip[o + 2] = wz + dz * S;
        L.rmuz[o] = L.tip[o] + dx * ext; L.rmuz[o + 1] = L.tip[o + 1] + dy * ext; L.rmuz[o + 2] = L.tip[o + 2] + dz * ext;
    }

    // ===================================================================================================
    // gameplay
    // ===================================================================================================

    public static boolean isEnergy(BulletType t){
        return t instanceof LaserBulletType || t instanceof ContinuousBulletType || t instanceof LightningBulletType
            || t instanceof LaserBoltBulletType || t instanceof PointLaserBulletType || t instanceof SapBulletType
            || t instanceof EmpBulletType || t.status == StatusEffects.shocked || t.status == StatusEffects.electrified
            || t.lightning > 0 || t.hitColor.equals(mindustry.graphics.Pal.lancerLaser) || t.hitColor.equals(mindustry.graphics.Pal.heal);
    }

    /** instant / continuous beams are not projectiles: they are absorbed on sight instead of caught in flight */
    public static boolean isBeam(BulletType t){
        return t instanceof LaserBulletType || t instanceof ContinuousBulletType || t instanceof PointLaserBulletType
            || t instanceof ShrapnelBulletType || t instanceof RailBulletType || t instanceof SapBulletType;
    }

    static float beamLength(BulletType t){
        float l = t.range;
        if(t instanceof LaserBulletType lb) l = Math.max(l, lb.length);
        else if(t instanceof ContinuousBulletType cb) l = Math.max(l, cb.length);
        else if(t instanceof ShrapnelBulletType sb) l = Math.max(l, sb.length);
        else if(t instanceof RailBulletType rb) l = Math.max(l, rb.length);
        return l;
    }

    static boolean registered;

    /** Direct energy damage that still lands on the hull (lasers, lightning, arcs) is refunded: healing + stored energy. */
    public static void registerEvents(){
        if(registered) return;
        registered = true;
        BeamAbsorber.register();
        Events.on(UnitDamageEvent.class, e -> {
            if(e.unit == null || e.bullet == null || !(e.unit.type instanceof DetainerType t)) return;
            t.onHit(e.unit, e.bullet);
        });
    }

    /**
     * Hull hits are never refunded any more: energy only comes from bullets and beams that really touch a claw
     * (catchBullet / BeamAbsorber), otherwise the whole body would act as an absorbing shield.
     */
    public void onHit(Unit unit, Bullet b){
        if(unit.team == b.team) return;
        logic(unit).hullLeaks++;
    }

    /** hooks for tests / tools (the mod's static classes are not reachable from the server console) */
    public void hookCfg(Player p, String s){ AstroNet.serverCfg(p, s); }
    public void hookAct(Player p, String s){ AstroNet.serverAct(p, s); }
    public int hookDirect(Unit u, int ab, float ax, float ay, int kind){ return DetainerCtl.act(this, u, ab, ax, ay, kind); }
    public String hookStatus(Unit u){ return AstroNet.statusOf(this, u); }
    public float hookCost(float dist){ return DetainerCtl.warpCost(dist); }
    public void hookLicense(Unit u, boolean on){ DetainerCtl.grant(u, on); }
    public boolean hookLicensed(Unit u){ return DetainerCtl.licensed(u); }
    public mindustry.ai.UnitStance hookStance(){ return AstroContent.warpStance; }

    @Override
    public void update(Unit unit){
        super.update(unit);
        Logic L = logic(unit);
        float dt = Time.delta;
        L.time += dt;

        if(Time.time - lastPrune > 900f){
            lastPrune = Time.time;
            IntSeq dead = new IntSeq();
            for(var e : logics.entries()) if(Time.time - e.value.seen > 900f) dead.add(e.key);
            for(int i = 0; i < dead.size; i++) logics.remove(dead.get(i));
        }

        for(int i = 0; i < 3; i++){
            L.flash[i] = Math.max(0f, L.flash[i] - dt / 22f);
            L.kick[i] = Math.max(0f, L.kick[i] - dt / 14f);
            L.tt[i] = Math.max(0f, L.tt[i] - dt);
            L.cd[i] = Math.max(0f, L.cd[i] - dt);
            L.grabCd[i] = Math.max(0f, L.grabCd[i] - dt);
        }
        for(int i = 0; i < 3; i++) L.catchCd[i] = Math.max(0f, L.catchCd[i] - dt);
        L.counterCd = Math.max(0f, L.counterCd - dt);
        L.overloadFlash = Math.max(0f, L.overloadFlash - dt / 40f);
        L.absorbGlow = Math.max(0f, L.absorbGlow - dt / 50f);
        if(L.beamsSeen.size > 64) L.beamsSeen.clear();

        if(!unit.dead && unit.isAdded()){
            L.energy = Math.min(maxEnergy, L.energy + regen * dt);
            DetainerCtl.tick(this, unit, L, dt);
            fieldAndClaws(unit, L, dt);
            autoFace(unit, L, dt);
        }else{
            for(int i = 0; i < 3; i++) releasePrisoner(unit, L, i, false);
        }

        pose(unit, L, dt);
    }

    /**
     * The body turns to face what the claws are fighting (or the nearest enemy), else it follows the movement. The
     * pilot only flies: claws and cannons need a target in front of them, so the body must be turned to it first.
     */
    void autoFace(Unit unit, Logic L, float dt){
        Posc tg = null;
        if(unit.isPlayer() && L.warpWind <= 0f){
            //a pilot in pro mode: the body turns to what he commands (a pending strike / lance, the orbs) or to his aim while he fires
            boolean cmd = L.req[AB_STAB] > 0f || L.req[AB_SLASH] > 0f || L.req[AB_GRAB] > 0f || L.req[AB_LANCE] > 0f || L.req[AB_OVERLOAD] > 0f || L.orbOn;
            if(cmd && L.reqT != null){
                unit.rotation = Angles.moveToward(unit.rotation, unit.angleTo(L.reqT), turnRate * 1.5f * dt);
                return;
            }
            if(L.pro && unit.isShooting){
                float[] a = DetainerCtl.aim;
                DetainerCtl.aimOf(unit, a);
                if(Mathf.dst(unit.x, unit.y, a[0], a[1]) > 24f){
                    unit.rotation = Angles.moveToward(unit.rotation, unit.angleTo(a[0], a[1]), turnRate * dt);
                    return;
                }
            }
        }
        for(int i = 0; i < 3 && tg == null; i++){
            if(L.mode[i] == STAB || L.mode[i] == SLASH || L.mode[i] == GRAB || L.mode[i] == HOLD) tg = targetOf(L, i);
        }
        if(tg == null){
            L.faceT -= dt;
            if(L.faceT <= 0f){
                L.faceT = 12f;
                Teamc cand = Units.closestTarget(unit.team, unit.x, unit.y, faceRange, u -> u.checkTarget(true, true) && u.team != Team.derelict, t -> !t.block.underBullets && t.team != Team.derelict);
                //sticky: only turn away from the current one when the new one is clearly closer (no flipping between equals)
                if(L.faceTarget == null || Units.invalidateTarget(L.faceTarget, unit.team, unit.x, unit.y, faceRange + 40f)) L.faceTarget = cand;
                else if(cand != null && cand != L.faceTarget && unit.dst(cand) < unit.dst(L.faceTarget) * 0.7f) L.faceTarget = cand;
            }
            if(L.faceTarget != null && Units.invalidateTarget(L.faceTarget, unit.team, unit.x, unit.y, faceRange + 40f)) L.faceTarget = null;
            tg = L.faceTarget;
        }
        float ang;
        if(tg != null) ang = unit.angleTo(tg);
        else if(unit.vel.len() > 0.25f) ang = unit.vel.angle();
        else return;
        unit.rotation = Angles.moveToward(unit.rotation, ang, turnRate * dt);
    }

    // --------------------------------------------------------------------------------------------------
    // arm helpers
    // --------------------------------------------------------------------------------------------------

    final float[] zp = new float[3];

    /**
     * Can arm i really hit a thing of the given size at a world position? kind: 0 stab, 1 slash, 2 grab, 3 long grab.
     * Checks the arm's own strike sector (never behind the body, never across the body) and the real reach from the
     * arm's own shoulder, so that a claw never swings at something it cannot touch.
     */
    public boolean inZone(Unit unit, int i, float wx, float wy, float hitSize, int kind){
        return inZone(unit, i, wx, wy, hitSize, kind, 0f);
    }

    /**
     * extra > 0 is tolerance for a strike that is already under way (a target that only moved a little is still hit),
     * extra < 0 is the margin needed to START a strike (hysteresis: no start / abort / start flicker at the zone edge)
     */
    public boolean inZone(Unit unit, int i, float wx, float wy, float hitSize, int kind, float extra){
        toBody(unit, wx, wy, ATZ, zp);
        float px = zp[0], py = zp[1];
        float hr = hitSize * 0.5f / SC;
        if(py + hr * 0.4f < 4f) return false; //behind or beside the body centre line
        float ang = Mathf.atan2(py, px) * Mathf.radDeg;
        float slack = (kind >= 2 ? 8f : 0f) + extra;
        if(ang < ZONE[i][0] - slack || ang > ZONE[i][1] + slack) return false;
        float[] s = SHOULDER[i];
        float dz = s[2] - ATZ;
        float d = Mathf.len(px - s[0], py - s[1]) - hr * 0.6f;
        float max3 = switch(kind){
            case 0 -> REACH + BL * 0.6f;
            case 1 -> REACH * 0.94f + BL * 0.7f;
            case 2 -> REACH + 2f;
            default -> LONGREACH * 0.97f;
        };
        max3 += extra * 0.4f;
        float maxH = (float)Math.sqrt(Math.max(max3 * max3 - dz * dz, 1f));
        return d <= maxH;
    }

    boolean inZone(Unit unit, int i, Posc t, int kind){
        return inZone(unit, i, t, kind, 0f);
    }

    boolean inZone(Unit unit, int i, Posc t, int kind, float extra){
        float hs = t instanceof Hitboxc h ? h.hitSize() : 16f;
        return inZone(unit, i, t.getX(), t.getY(), hs, kind, extra);
    }

    /** a strike that was started but whose target left the zone / died before the damage window is cancelled (no swinging at air) */
    void abortStrike(Logic L, int i){
        L.mode[i] = IDLE;
        L.cd[i] = 14f;
        L.modeT[i] = 0f;
        L.hits[i].clear();
        L.hitDone[i] = false;
        L.aborts++;
    }

    boolean free(Logic L, int i){
        return L.mode[i] == IDLE || L.mode[i] == CATCH;
    }

    /** arm whose shoulder is nearest to a world point (optionally only arms that are not busy) */
    int nearestArm(Unit unit, Logic L, float wx, float wy, boolean freeOnly){
        int best = -1, any = 0;
        float bd = 1e18f, bd2 = 1e18f;
        for(int i = 0; i < 3; i++){
            float[] s = SHOULDER[i];
            toWorld(unit, s[0], s[1], s[2], p3);
            float d = Mathf.dst2(p3[0], p3[1], wx, wy);
            if(d < bd2){ bd2 = d; any = i; }
            if(freeOnly && !free(L, i)) continue;
            if(d < bd){ bd = d; best = i; }
        }
        return best == -1 ? (freeOnly ? -1 : any) : best;
    }

    // ===================================================================================================
    // pose: claw goals from the arm state machine, smoothing, arm solve
    // ===================================================================================================

    final float[] goalMuz = new float[9], goalDir = new float[9];
    final float[] goalOpen = new float[3], goalFollow = new float[3], goalSpin = new float[3], goalBlade = new float[3];

    public void setGoal(Logic L, int i, float x, float y, float z, float dx, float dy, float dz){
        int o = i * 3;
        float l = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        l = Math.max(l, 1e-4f);
        goalMuz[o] = x; goalMuz[o + 1] = y; goalMuz[o + 2] = z;
        goalDir[o] = dx / l; goalDir[o + 1] = dy / l; goalDir[o + 2] = dz / l;
    }

    /** claw reaches point (body frame) with its jaws, facing away from its shoulder */
    public void reach(Logic L, int i, float x, float y, float z){
        float[] s = SHOULDER[i];
        setGoal(L, i, x, y, z, x - s[0], y - s[1], z - s[2] - 2f);
    }

    public void reachClamped(Logic L, int i, float x, float y, float z, float maxR){
        float l = Mathf.len(x, y);
        if(l > maxR){ x *= maxR / l; y *= maxR / l; }
        reach(L, i, x, y, z);
    }

    static float smooth(float t){
        t = Mathf.clamp(t);
        return t * t * (3f - 2f * t);
    }

    /** rotates a body-frame point (x,y) about the body centre so it lies inside the attack cone (+-maxDeg from forward) */
    static void clampFront(float[] g, float maxDeg){
        float len = Mathf.len(g[0], g[1]);
        if(len < 1e-3f) return;
        float a = Mathf.atan2(g[1], g[0]) * Mathf.radDeg; //0 = forward (+y), positive = right
        if(Math.abs(a) <= maxDeg) return;
        a = Math.signum(a) * maxDeg;
        g[0] = Mathf.sinDeg(a) * len;
        g[1] = Mathf.cosDeg(a) * len;
    }

    public void pose(Unit unit, Logic L, float dt){
        float t = Time.time + unit.id * 7f;
        float[] g = p2;
        for(int i = 0; i < 3; i++){
            float follow = 0.22f, openGoal = 0.45f, spinGoal = 0f, bladeGoal = 0f, kGoal = KMAX;
            float sway = Mathf.sin(t + i * 61f, 47f, 1.9f);
            float[] s = SHOULDER[i];
            int mode = L.mode[i];
            float mt = L.modeT[i];
            if(mode == STAB){
                float[] tg = armTarget(unit, L, i, g);
                float ux = tg[0] - s[0], uy = tg[1] - s[1], ul = Math.max(Mathf.len(ux, uy), 1e-3f);
                ux /= ul; uy /= ul;
                float need = Mathf.clamp(ul - BL * 0.75f, 18f, REACH);
                float thrust = smooth((mt - 12f) / 5f), recover = smooth((mt - 17f) / 16f);
                float r = Mathf.lerp(need * 0.3f + 6f, need, thrust);
                r = Mathf.lerp(r, need * 0.5f + 6f, recover);
                setGoal(L, i, s[0] + ux * r, s[1] + uy * r, Mathf.lerp(ATZ + 6f, ATZ + 2f, thrust), ux, uy, -0.12f);
                follow = mt < 12f ? 0.4f : 1.4f;
                openGoal = mt < 12f ? 0.9f : 0.05f;
                bladeGoal = mt < 33f ? 1f : 0f;
                if(mt > 33f) L.mode[i] = IDLE;
            }else if(mode == SLASH){
                //adaptive slash: the arc, radius and speed were planned from the enemies around the target (planSlash)
                float dur = L.slDur[i];
                float wind = smooth(mt / 12f), sweep = smooth((mt - 12f) / dur), rec = smooth((mt - 12f - dur) / 14f);
                float a0 = L.slA0[i], a1 = L.slA1[i], sg = Math.signum(a1 - a0);
                float lead = 26f * sg;
                float phi = Mathf.lerp(a0 - lead * wind * (1f - sweep), a1, sweep); //wind-up swings back past the start
                float rr = Mathf.lerp(L.slR[i] * 0.78f, L.slR[i], wind);
                rr = Mathf.lerp(rr, L.slR[i] * 0.7f, rec);
                //body-frame angle phi: 0 = +x (right), 90 = forward
                float cx = Mathf.cosDeg(phi), cy = Mathf.sinDeg(phi);
                setGoal(L, i, cx * rr, cy * rr, ATZ + 3f, cx - sg * cy * 0.5f, cy + sg * cx * 0.5f, -0.08f);
                follow = mt < 12f ? 0.5f : 1.4f;
                openGoal = 0.05f;
                bladeGoal = mt < 12f + dur + 10f ? 1f : 0f;
                spinGoal = (mt > 10f && mt < 12f + dur) ? 22f : 0f;
                if(mt > 12f + dur + 14f) L.mode[i] = IDLE;
            }else if(mode == GRAB || mode == HOLD){
                Unit v = Groups.unit.getByID(L.atkId[i]);
                if(v != null && v.isAdded()){
                    toBody(unit, v.x, v.y, ATZ, g);
                    if(mode == GRAB){
                        reach(L, i, g[0], g[1], ATZ);
                        openGoal = 1f;
                        follow = L.longGrab[i] ? 1.3f : 0.8f;
                    }else{
                        float pull = smooth(L.holdT[i] / (L.longGrab[i] ? 60f : 36f));
                        float[] cp = crushPoint(i);
                        float sx2 = Mathf.lerp(L.gx[i], cp[0], pull), sy2 = Mathf.lerp(L.gy[i], cp[1], pull);
                        reach(L, i, sx2, sy2, ATZ + 2f);
                        openGoal = 0.04f;
                        follow = 1.0f;
                        spinGoal = L.crushFx[i] * 6f;
                    }
                    //stretch exactly as far as the target needs
                    float gx = goalMuz[i * 3] , gy = goalMuz[i * 3 + 1], gz = goalMuz[i * 3 + 2];
                    float dd = (float)Math.sqrt(Mathf.dst2(gx, gy, s[0], s[1]) + (gz - s[2]) * (gz - s[2]));
                    kGoal = Mathf.clamp((dd - (J - 1.5f) - S) / ((U + F) * 0.94f) + 0.12f, KMAX, KLONG);
                }
            }else if(L.tt[i] > 0f){
                toBody(unit, L.tx[i], L.ty[i], L.tz[i], g);
                clampFront(g, 125f);
                reachClamped(L, i, g[0], g[1], g[2], catchRadius / SC - 4f);
                follow = 0.6f;
                openGoal = L.flash[i] > 0.7f ? 0f : 1f;
                spinGoal = L.flash[i] > 0.3f ? 14f : 5f;
            }else{
                float gxx = REST_MUZ[i][0] + sway * 1.1f, gyy = REST_MUZ[i][1] + Mathf.cos(t + i * 31f, 39f, 1.8f), gzz = REST_MUZ[i][2] + Mathf.sin(t + i * 17f, 31f, 1.2f);
                setGoal(L, i, gxx, gyy, gzz, REST_DIR[i][0], REST_DIR[i][1], REST_DIR[i][2]);
                openGoal = 0.45f + 0.1f * Mathf.sin(t + i * 50f, 55f, 1f) + 0.5f * lanceCharge(unit, i);
                spinGoal = Mathf.sin(t + i * 33f, 90f, 0.6f) + 8f * lanceCharge(unit, i);
            }
            //recoil: the jaws snap back along their own axis when the claw shoots
            if(L.kick[i] > 0f && mode == IDLE){
                int o = i * 3;
                goalMuz[o] -= goalDir[o] * 5f * L.kick[i]; goalMuz[o + 1] -= goalDir[o + 1] * 5f * L.kick[i]; goalMuz[o + 2] -= goalDir[o + 2] * 5f * L.kick[i];
            }
            L.kmax[i] = Mathf.lerpDelta(L.kmax[i], kGoal, kGoal > L.kmax[i] ? 0.22f : 0.12f);
            L.open[i] = Mathf.lerpDelta(L.open[i], Mathf.clamp(openGoal, 0f, 1f), 0.25f);
            L.spin[i] = Mathf.lerpDelta(L.spin[i], spinGoal, 0.18f);
            L.roll[i] += L.spin[i] * dt;
            if(Math.abs(L.spin[i]) < 0.7f && mode == IDLE) L.roll[i] = Mathf.lerpDelta(L.roll[i], Mathf.round(L.roll[i] / 180f) * 180f + sway * 4f, 0.05f);
            L.blade[i] = Mathf.lerpDelta(L.blade[i], bladeGoal, bladeGoal > L.blade[i] ? 0.35f : 0.2f);
            float maxStep = mode == STAB || mode == SLASH ? 17f : mode == GRAB ? 9f : mode == HOLD ? 7f : L.tt[i] > 0f ? 6.5f : 4.5f;
            smoothFollow(L, i, follow, dt, maxStep);
        }
        for(int i = 0; i < 3; i++) resolve(L, i);
        trackMotion(L);
        armGeometry(unit, L);
        //glow of each claw: only while energy is moving through it (absorbing, charging a lance, crushing a prisoner)
        for(int i = 0; i < 3; i++){
            float c = L.flash[i];
            float lc = lanceCharge(unit, i);
            c = Math.max(c, lc * 0.85f + L.overloadFlash);
            if(L.held[i] != -1) c = Math.max(c, 0.7f + 0.3f * Mathf.absin(Time.time, 4f, 1f));
            if(L.mode[i] == STAB || L.mode[i] == SLASH) c = Math.max(c, 0.35f);
            L.charge[i] = Mathf.lerpDelta(L.charge[i], Mathf.clamp(c, 0f, 1.2f), 0.25f);
        }
    }

    /** motion statistics of the drawn claw positions: a claw must move continuously and must not jitter back and forth */
    void trackMotion(Logic L){
        for(int i = 0; i < 3; i++){
            int o = i * 3;
            float sx = L.rmuz[o] - L.prM[o], sy = L.rmuz[o + 1] - L.prM[o + 1], sz = L.rmuz[o + 2] - L.prM[o + 2];
            float len = (float)Math.sqrt(sx * sx + sy * sy + sz * sz);
            if(L.prInit){
                if(len > 26f) L.teleports++;
                L.maxStepSeen = Math.max(L.maxStepSeen, len);
                float dot = sx * L.prS[o] + sy * L.prS[o + 1] + sz * L.prS[o + 2];
                if(len > 1f && L.prLen[i] > 1f && dot < -0.3f * len * L.prLen[i]) L.reversals++;
            }
            L.prS[o] = sx; L.prS[o + 1] = sy; L.prS[o + 2] = sz; L.prLen[i] = len;
            L.prM[o] = L.rmuz[o]; L.prM[o + 1] = L.rmuz[o + 1]; L.prM[o + 2] = L.rmuz[o + 2];
        }
        L.prInit = true;
    }

    /**
     * Plans the arc of claw i's slash from the enemies around its target: the arc covers every nearby enemy (plus
     * margin), the blade's middle is brought to their average distance, the swing leads a moving target and the
     * duration follows the arc width. Re-planned every tick of the wind-up so the blade follows the fight.
     */
    void planSlash(Unit unit, Logic L, int i){
        Posc tg = targetOf(L, i);
        if(tg == null) return;
        toBody(unit, tg.getX(), tg.getY(), ATZ, p0);
        float phiT = Mathf.atan2(p0[0], p0[1]) * Mathf.radDeg;
        float tx = tg.getX(), ty = tg.getY();
        final float[] acc = {0f, 0f, 0f, 1e9f, -1e9f}; //sum rho*w, sum w, count, dmin, dmax
        final float pT = phiT;
        float reachW = (REACH * 0.95f + BL * 0.5f) * SC;
        acc[0] = Mathf.len(p0[0], p0[1]) * 2f; acc[1] = 2f; acc[2] = 1f;
        float hsT = tg instanceof Hitboxc h ? h.hitSize() : 16f;
        float rhoT = Math.max(Mathf.len(p0[0], p0[1]), 8f);
        float padT = (float)Math.toDegrees(Math.atan(hsT * 0.5f / rhoT / SC));
        acc[3] = -padT; acc[4] = padT;
        float[] tmp = new float[3];
        Units.nearbyEnemies(unit.team, tx - 70f, ty - 70f, 140f, 140f, u -> {
            if(u == tg || !u.isAdded() || u.dead || u.team == Team.derelict || u.dst(unit) > reachW) return;
            toBody(unit, u.x, u.y, ATZ, tmp);
            float ph = Mathf.atan2(tmp[0], tmp[1]) * Mathf.radDeg;
            float dl = wrap180(ph - pT);
            if(Math.abs(dl) > 95f) return;
            float rh = Math.max(Mathf.len(tmp[0], tmp[1]), 8f);
            float pad = (float)Math.toDegrees(Math.atan(u.hitSize * 0.5f / rh / SC));
            acc[0] += rh * u.hitSize; acc[1] += u.hitSize; acc[2]++;
            acc[3] = Math.min(acc[3], dl - pad); acc[4] = Math.max(acc[4], dl + pad);
        });
        float lo = acc[3] - 14f, hi = acc[4] + 14f;
        //lead a moving target: angular velocity of the target around the body (deg per tick) * time to impact
        float vx = tg instanceof Velc vv ? vv.vel().x : 0f, vy = tg instanceof Velc vv2 ? vv2.vel().y : 0f;
        float rx = tx - unit.x, ry = ty - unit.y, rl2 = Math.max(rx * rx + ry * ry, 100f);
        float angVel = (rx * vy - ry * vx) / rl2 * Mathf.radDeg; //deg per tick, world CCW
        float lead = Mathf.clamp(angVel * 30f, -28f, 28f);
        lo += lead; hi += lead;
        if(hi - lo < 76f){ float m = (hi + lo) / 2f; lo = m - 38f; hi = m + 38f; }
        if(hi - lo > 170f){ float m = (hi + lo) / 2f; lo = m - 85f; hi = m + 85f; }
        //the arm swings from its outer side across the front: left arm (angle grows to the left) sweeps downwards
        boolean decreasing = i == 0 ? true : i == 1 ? false : L.sgn[i] < 0f;
        L.slA0[i] = pT + (decreasing ? hi : lo);
        L.slA1[i] = pT + (decreasing ? lo : hi);
        float avgRho = acc[0] / Math.max(acc[1], 1f);
        L.slR[i] = Mathf.clamp(avgRho - BL * 0.42f, 24f, REACH * 0.92f);
        L.slDur[i] = Mathf.clamp((hi - lo) / 3.4f, 14f, 30f);
        L.slashMulti = Math.max(L.slashMulti, (int)acc[2]);
        L.slashOk[i] = validateSlash(L, i, pT, rhoT, hsT);
    }

    /**
     * Keeps the planned arc inside the arm's own sector (body frame angle phi = 90 - heading angle), shrinks the radius until every
     * point of the arc is reachable from the arm's shoulder, and checks that the blade still covers the target. False = do not slash.
     */
    boolean validateSlash(Logic L, int i, float phiT, float rhoT, float hitSize){
        float zmin = 90f - ZONE[i][1], zmax = 90f - ZONE[i][0];
        boolean dec = L.slA0[i] > L.slA1[i];
        float a = Math.min(L.slA0[i], L.slA1[i]), b = Math.max(L.slA0[i], L.slA1[i]);
        a = Math.max(a, zmin); b = Math.min(b, zmax);
        if(b - a < 34f) return false;
        if(phiT < a - 1f || phiT > b + 1f) return false;
        L.slA0[i] = dec ? b : a;
        L.slA1[i] = dec ? a : b;
        L.slDur[i] = Mathf.clamp((b - a) / 3.4f, 14f, 30f);
        float[] s = SHOULDER[i];
        float dz = s[2] - ATZ;
        float maxD = REACH - 3f;
        float rr = L.slR[i];
        for(int tries = 0; tries < 12; tries++){
            boolean ok = true;
            for(int k = 0; k <= 6 && ok; k++){
                float ph = Mathf.lerp(a, b, k / 6f);
                float mx = Mathf.cosDeg(ph) * rr - s[0], my = Mathf.sinDeg(ph) * rr - s[1];
                if(mx * mx + my * my + dz * dz > maxD * maxD) ok = false;
            }
            if(ok) break;
            rr -= 3f;
            if(rr < 24f) return false;
        }
        L.slR[i] = rr;
        //the blade (muzzle radius + most of BL along the swing) has to reach the target's body
        return rhoT - hitSize * 0.3f / SC <= rr + BL * 0.85f;
    }

    /** 0..1: how close claw i's lance is to being ready and loaded */
    public float lanceCharge(Unit unit, int i){
        if(unit.mounts.length < 6) return 0f;
        Logic L = logic(unit);
        WeaponMount m = unit.mounts[3 + i];
        float ready = 1f - Mathf.clamp(m.reload / Math.max(m.weapon.reload, 1f));
        float loaded = Mathf.clamp((L.energy - lanceMin * 0.5f) / (lanceMin * 0.5f));
        return ready * loaded * (0.5f + 0.5f * ready) * (unit.isShooting ? 1f : 0.35f);
    }

    /** body frame point (x,y,ATZ) of the current melee / grab target of arm i, written into out[0..2] */
    float[] armTarget(Unit unit, Logic L, int i, float[] out){
        Posc tg = targetOf(L, i);
        if(tg == null) toBody(unit, L.tx[i], L.ty[i], ATZ, out);
        else toBody(unit, tg.getX(), tg.getY(), ATZ, out);
        return out;
    }

    Posc targetOf(Logic L, int i){
        if(L.atkId[i] >= 0){
            Unit u = Groups.unit.getByID(L.atkId[i]);
            if(u != null && u.isAdded() && !u.dead) return u;
        }
        if(L.atkBuild[i] != null && L.atkBuild[i].isValid()) return L.atkBuild[i];
        return null;
    }

    /** where a prisoner is crushed (body frame) */
    float[] crushPoint(int i){
        return CRUSH[i];
    }

    static final float[][] CRUSH = {{-15f, 30f}, {15f, 30f}, {0f, 38f}};

    void smoothFollow(Logic L, int i, float follow, float dt, float maxStep){
        int o = i * 3;
        float k = 1f - (float)Math.exp(-follow * 3f * dt);
        //the claw moves toward its goal with a bounded speed: it never teleports, however fast the goal jumps
        float mx = (goalMuz[o] - L.muz[o]) * k, my = (goalMuz[o + 1] - L.muz[o + 1]) * k, mz = (goalMuz[o + 2] - L.muz[o + 2]) * k;
        float ml = (float)Math.sqrt(mx * mx + my * my + mz * mz), lim = maxStep * dt;
        if(ml > lim){ float q = lim / ml; mx *= q; my *= q; mz *= q; }
        L.muz[o] += mx; L.muz[o + 1] += my; L.muz[o + 2] += mz;
        float kd = Math.min(k, 0.5f);
        for(int a = 0; a < 3; a++) L.dir[o + a] += (goalDir[o + a] - L.dir[o + a]) * kd;
        float l = (float)Math.sqrt(L.dir[o] * L.dir[o] + L.dir[o + 1] * L.dir[o + 1] + L.dir[o + 2] * L.dir[o + 2]);
        if(l > 1e-4f){ L.dir[o] /= l; L.dir[o + 1] /= l; L.dir[o + 2] /= l; }
        else{ L.dir[o] = goalDir[o]; L.dir[o + 1] = goalDir[o + 1]; L.dir[o + 2] = goalDir[o + 2]; }
    }

    // ===================================================================================================
    // field: catching projectiles and beams, the arm state machines
    // ===================================================================================================

    /**
     * Aegis: against what the claws cannot absorb (non-absorbable bullets, flames, contact damage, anything that reaches the
     * hull) the unit raises an energy shield. It uses the vanilla unit shield (so every damage source respects it), costs
     * stored energy, turns part of the absorbed damage back into energy and collapses (with a cool-down) when overwhelmed.
     */
    /** the dome goes up: burst of shield paid with stored energy (by the AI on a threat, or by the pilot's command) */
    void raiseAegis(Unit unit, Logic L){
        L.aegisOn = true; L.aegisHold = 0f; L.aegisUps++;
        float add = Math.min(aegisBurst, aegisMax - unit.shield);
        unit.shield += add;
        L.energy -= add / aegisRatio;
        L.aegisHit = 1f;
        AstroFx.reflect.at(unit.x, unit.y, 0f, AstroFx.gold);
    }

    void aegisTick(Unit unit, Logic L, float dt){
        if(Vars.net != null && Vars.net.client()){
            //clients only show the dome (the shield value is synchronised by the server)
            L.aegisVis = Mathf.lerpDelta(L.aegisVis, unit.shield > 1f ? Mathf.clamp(unit.shield / 600f, 0.25f, 1f) : 0f, 0.12f);
            L.aegisHit = Math.max(0f, L.aegisHit - dt / 12f);
            return;
        }
        L.threatT = Math.max(0f, L.threatT - dt);
        L.hurtT = Math.max(0f, L.hurtT - dt);
        L.aegisCd = Math.max(0f, L.aegisCd - dt);
        L.aegisHit = Math.max(0f, L.aegisHit - dt / 12f);
        //1. shield damage taken since last tick becomes a little energy and lights the dome
        float drop = L.lastShield - unit.shield;
        if(L.aegisOn && drop > 0.5f){
            L.aegisAbsorbed += drop;
            L.energy = Math.min(maxEnergy, L.energy + drop * 0.08f);
            L.aegisHit = 1f;
            L.hurtT = 30f;
        }
        //2. hull damage that got through (the heal refunds of absorbed energy are not damage)
        if(L.lastHp - unit.health > 2f && !L.aegisOn) L.hurtT = 45f;
        boolean wasUp = L.lastShield > 1f;
        if(L.aegisOn){
            if(unit.shield <= 0.5f && wasUp){
                //overwhelmed: the field collapses and needs time to recover
                L.aegisOn = false; L.aegisCd = 240f; L.aegisBreaks++;
                AstroFx.reflect.at(unit.x, unit.y, 0f, AstroFx.hot);
            }else if(L.threatT <= 0f && L.hurtT <= 0f && L.aegisManual <= 0f){
                L.aegisHold += dt;
                if(L.aegisHold > 80f) L.aegisOn = false;
            }else L.aegisHold = 0f;
        }else if((L.threatT > 0f || L.hurtT > 0f) && L.aegisCd <= 0f && L.energy >= aegisMinEnergy && DetainerCtl.aiMay(unit, L, AB_AEGIS)){
            raiseAegis(unit, L);
        }
        if(L.aegisOn && unit.shield < aegisMax && L.energy > aegisMinEnergy * 0.4f){
            float add = Math.min(aegisRegen * dt, aegisMax - unit.shield);
            unit.shield += add;
            L.energy -= add / aegisRatio;
        }
        if(!L.aegisOn && unit.shield > 0f && L.aegisCd <= 0f) unit.shield = Math.max(0f, unit.shield - Math.max(2f, unit.shield * 0.004f) * dt); //an idle shield leaks away
        L.lastShield = unit.shield;
        L.lastHp = unit.health;
        L.aegisVis = Mathf.lerpDelta(L.aegisVis, unit.shield > 1f ? Mathf.clamp(unit.shield / 600f, 0.25f, 1f) : 0f, 0.12f);
    }

    // --------------------------------------------------------------------------------------------------
    // arm contact: energy is only absorbed where a bullet or beam really touches an arm or a claw
    // --------------------------------------------------------------------------------------------------

    /** collision radius (world units) of upper arm, forearm and claw; the claw is the fattest part (open jaws) */
    public static final float R_UPPER = 6.5f, R_FORE = 7.5f, R_CLAW = 13f;
    static final float[] SEG_R = {R_UPPER, R_FORE, R_CLAW, R_CLAW};

    /** world-space polyline of the arms exactly as drawn (same projection as muzzleWorld) */
    public void armGeometry(Unit unit, Logic L){
        float[] q = new float[2];
        for(int i = 0; i < 3; i++){
            int o = i * 3, w = i * 10;
            float[] s = SHOULDER[i];
            toWorld(unit, s[0], s[1], s[2], q); L.aw[w] = q[0]; L.aw[w + 1] = q[1];
            toWorld(unit, L.knee[o], L.knee[o + 1], L.knee[o + 2], q); L.aw[w + 2] = q[0]; L.aw[w + 3] = q[1];
            toWorld(unit, L.wrist[o], L.wrist[o + 1], L.wrist[o + 2], q); L.aw[w + 4] = q[0]; L.aw[w + 5] = q[1];
            toWorld(unit, L.tip[o], L.tip[o + 1], L.tip[o + 2], q); L.aw[w + 6] = q[0]; L.aw[w + 7] = q[1];
            toWorld(unit, L.rmuz[o], L.rmuz[o + 1], L.rmuz[o + 2], q); L.aw[w + 8] = q[0]; L.aw[w + 9] = q[1];
        }
    }

    /** closest distance between segments AB and CD; t[0] = parameter (0..1) on AB of the closest point */
    static float segSeg(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, float[] t){
        float ux = bx - ax, uy = by - ay, vx = dx - cx, vy = dy - cy, wx = ax - cx, wy = ay - cy;
        float a = ux * ux + uy * uy, b = ux * vx + uy * vy, c = vx * vx + vy * vy, d = ux * wx + uy * wy, e = vx * wx + vy * wy;
        float den = a * c - b * b, sN, sD = den, tN, tD = den;
        if(den < 1e-6f){ sN = 0f; sD = 1f; tN = e; tD = c; }
        else{
            sN = b * e - c * d; tN = a * e - b * d;
            if(sN < 0f){ sN = 0f; tN = e; tD = c; }
            else if(sN > sD){ sN = sD; tN = e + b; tD = c; }
        }
        if(tN < 0f){
            tN = 0f;
            if(-d < 0f) sN = 0f; else if(-d > a) sN = sD; else{ sN = -d; sD = a; }
        }else if(tN > tD){
            tN = tD;
            if(-d + b < 0f) sN = 0f; else if(-d + b > a) sN = sD; else{ sN = -d + b; sD = a; }
        }
        float sc = Math.abs(sN) < 1e-6f ? 0f : sN / sD, tc = Math.abs(tN) < 1e-6f ? 0f : tN / tD;
        if(t != null) t[0] = sc;
        float px = wx + sc * ux - tc * vx, py = wy + sc * uy - tc * vy;
        return (float)Math.sqrt(px * px + py * py);
    }

    final float[] segT = new float[1];

    /**
     * The arm whose surface the path (x0,y0)-(x1,y1) of something with the given radius touches, or -1. out[0] = the gap
     * (distance to the arm axis minus the arm radius minus the radius of the thing); out[1] = the arm's segment index.
     */
    public int armTouching(Logic L, float x0, float y0, float x1, float y1, float radius, float[] out){
        int best = -1;
        float bg = 1e9f;
        for(int i = 0; i < 3; i++){
            int w = i * 10;
            for(int k = 0; k < 4; k++){
                float d = segSeg(x0, y0, x1, y1, L.aw[w + k * 2], L.aw[w + k * 2 + 1], L.aw[w + k * 2 + 2], L.aw[w + k * 2 + 3], segT);
                float gap = d - SEG_R[k] - radius;
                if(gap < bg){ bg = gap; best = gap <= 0f ? i : best; if(out != null) out[1] = k; }
            }
        }
        if(out != null) out[0] = bg;
        return bg <= 0f ? best : -1;
    }

    /** the distance along a beam (origin, heading, length, half width) at which it first touches an arm, or -1 */
    public int beamArm = -1;

    public float armBeamContact(Logic L, float ox, float oy, float rot, float len, float halfW){
        beamArm = -1;
        float ex = ox + Mathf.cosDeg(rot) * len, ey = oy + Mathf.sinDeg(rot) * len;
        float bestT = -1f;
        for(int i = 0; i < 3; i++){
            int w = i * 10;
            for(int k = 0; k < 4; k++){
                float r = SEG_R[k] + halfW;
                float d = segSeg(ox, oy, ex, ey, L.aw[w + k * 2], L.aw[w + k * 2 + 1], L.aw[w + k * 2 + 2], L.aw[w + k * 2 + 3], segT);
                if(d <= r){
                    float t = segT[0] * len - (float)Math.sqrt(Math.max(r * r - d * d, 0f));
                    t = Math.max(t, 0f);
                    if(bestT < 0f || t < bestT){ bestT = t; beamArm = i; }
                }
            }
        }
        return bestT;
    }

    final float[] gapInfo = new float[2];

    public void fieldAndClaws(Unit unit, Logic L, float dt){
        float R = senseRadius;
        float best = 1e9f;

        //1. containment field: track, catch, then absorb or reflect hostile projectiles (swept test: fast bullets cannot skip the field)
        Groups.bullet.intersect(unit.x - R, unit.y - R, R * 2f, R * 2f, b -> {
            if(b.team == unit.team || b.absorbed || b.vel.len() < 0.05f) return;
            float d = b.dst(unit);
            if(!b.type.absorbable){
                //what the claws cannot take: the aegis shield is raised when such a bullet can hurt us
                if(d < aegisRange && (b.type.collidesAir || !unit.isFlying()) && b.type.damage + b.type.splashDamage > 4f){
                    float toward = (unit.x - b.x) * b.vel.x + (unit.y - b.y) * b.vel.y;
                    if(toward > 0f) L.threatT = 45f;
                }
                return;
            }
            if(d > R) return;
            //only what can really reach the body (or an arm) is worth a claw: bullets flying past are left alone
            float vx = b.vel.x, vy = b.vel.y, v2 = vx * vx + vy * vy;
            float rx = unit.x - b.x, ry = unit.y - b.y;
            float tc = v2 > 1e-4f ? (rx * vx + ry * vy) / v2 : 0f;      //ticks until the closest approach to the body centre
            float cxp = b.x + vx * Math.max(tc, 0f), cyp = b.y + vy * Math.max(tc, 0f);
            float perp = Mathf.dst(cxp, cyp, unit.x, unit.y);
            boolean inbound = tc > -2f && tc < 50f && perp < unit.hitSize * 0.5f + 46f;
            //1. a bullet that touches an arm or claw right now (swept along its path this tick) is caught
            float[] info = gapInfo;
            int touch = armTouching(L, b.lastX, b.lastY, b.x, b.y, Math.max(b.type.hitSize * 0.5f, 2f), info);
            if(touch != -1 && L.catchCd[touch] <= 0f){
                L.lastCatchGap = info[0];
                L.maxCatchGap = Math.max(L.maxCatchGap, info[0]);
                catchBullet(unit, L, b, touch);
                return;
            }
            if(!inbound) return;
            //2. otherwise a free claw goes to where the bullet will enter the guard ring around the body
            float G = 62f;
            float px, py;
            float dnow = Mathf.dst(b.x, b.y, unit.x, unit.y);
            if(dnow <= G + 6f || v2 < 1e-4f){ px = b.x + vx * 2f; py = b.y + vy * 2f; }
            else{
                float vl = (float)Math.sqrt(v2);
                float back = (float)Math.sqrt(Math.max(G * G - perp * perp, 0f));
                float gate = perp >= G ? 0f : back;
                px = cxp - vx / vl * gate; py = cyp - vy / vl * gate;
            }
            int arm = -1;
            for(int k = 0; k < 3; k++) if(L.trackId[k] == b.id && L.tt[k] > 0f && free(L, k)){ arm = k; break; }
            if(arm == -1){
                float bd = 1e18f;
                for(int k = 0; k < 3; k++){
                    if(!free(L, k) || L.tt[k] > 3f) continue;
                    float[] sh = SHOULDER[k];
                    toWorld(unit, sh[0], sh[1], sh[2], p3);
                    float dd = Mathf.dst2(p3[0], p3[1], px, py);
                    if(dd < bd){ bd = dd; arm = k; }
                }
            }
            if(arm != -1){
                L.trackId[arm] = b.id;
                L.tx[arm] = px; L.ty[arm] = py; L.tz[arm] = ATZ + 2f; L.tt[arm] = 10f;
                if(L.mode[arm] == IDLE) L.mode[arm] = CATCH;
            }
        });
        for(int i = 0; i < 3; i++) if(L.mode[i] == CATCH && L.tt[i] <= 0f) L.mode[i] = IDLE;

        //2. beams (lasers, continuous beams, shrapnel, rails) crossing the field are absorbed as well
        BeamAbsorber.note(unit);
        aegisTick(unit, L, dt);

        //3. arms: melee + grab state machines, prisoners
        DetainerCtl.serve(this, unit, L, dt);
        for(int i = 0; i < 3; i++) armAI(unit, L, i, dt);
        magnetPull(unit, L, dt);

        //4. draining shields and batteries (no visible link: the energy just arrives)
        L.drainT -= dt;
        if(L.drainT <= 0f){
            L.drainT = 10f;
            if(DetainerCtl.aiMay(unit, L, AB_DRAIN) || L.drainBurst > 0f) drain(unit, L);
        }
    }

    public void catchBullet(Unit unit, Logic L, Bullet b, int arm){
        float gain = Math.max(b.damage, 1f) * absorbScale + absorbBonus;
        float ef = L.energy / maxEnergy;
        boolean reflect = b.type.reflectable && b.vel.len() >= 0.1f && Mathf.chance(reflectChance + 0.3f * ef);
        L.energy = Math.min(maxEnergy, L.energy + gain);
        L.absorbedEnergy += gain;
        L.absorbed++;
        L.catches++;
        L.catchCd[arm] = 2f;
        L.absorbGlow = 1f;
        L.flash[arm] = 1f;
        L.kick[arm] = 1f;
        L.tt[arm] = Math.max(L.tt[arm], 4f);
        if(reflect){
            Posc src = b.owner instanceof Posc p ? p : null;
            float ang = src != null ? Angles.angle(b.x, b.y, src.getX(), src.getY()) : b.rotation() + 180f + Mathf.range(14f);
            float spd = Math.max(b.vel.len(), 1f) * reflectSpeed;
            b.vel.trns(ang, spd);
            b.rotation(ang);
            b.owner = unit;
            b.team = unit.team;
            b.time = Math.min(b.time, b.lifetime * 0.35f);
            b.damage *= 1f + reflectDamageBoost + ef * 0.6f;
            b.buildingDamageMultiplier = Math.max(b.buildingDamageMultiplier, 0.7f);
            b.hit = false;
            L.reflected++;
            AstroFx.reflect.at(b.x, b.y, ang, AstroFx.amber);
        }else{
            AstroFx.absorb.at(b.x, b.y, b.angleTo(unit), fieldColor);
            b.absorb();
        }
    }

    // --------------------------------------------------------------------------------------------------
    // the arm state machine: pick a target, then grab / stab / slash
    // --------------------------------------------------------------------------------------------------

    void armAI(Unit unit, Logic L, int i, float dt){
        if(L.mode[i] == STAB || L.mode[i] == SLASH){
            L.modeT[i] += dt;
            meleeTick(unit, L, i);
            if(L.mode[i] == IDLE){
                if(L.hitDone[i]){
                    if(L.hits[i].size == 0) L.whiffs++;
                    L.cd[i] = Math.max(L.cd[i], 24f);
                }
                L.hitDone[i] = false;
                L.hits[i].clear();
            }
            return;
        }
        if(L.mode[i] == GRAB || L.mode[i] == HOLD){
            L.modeT[i] += dt;
            grabTick(unit, L, i, dt);
            return;
        }
        if(L.cd[i] > 0f) return;
        boolean aiStab = DetainerCtl.aiMay(unit, L, AB_STAB), aiSlash = DetainerCtl.aiMay(unit, L, AB_SLASH), aiGrab = DetainerCtl.aiMay(unit, L, AB_GRAB);
        if(!(aiStab || aiSlash || aiGrab)) return; //the pilot owns all three: they only start on his command
        //every claw picks its own target, only among things it can really reach: the pilot only flies, the claws fight on their own
        pickTargets(unit, L, i);
        Unit grabT = aiGrab ? pickedGrab : null;
        Posc tg = pickedMelee;
        float gd = grabT == null ? 0f : grabT.dst(unit);
        if(grabT != null){
            //long reach: squishy / ranged / flying enemies are fished in from far away by a fully stretched arm
            boolean far = gd > grabRange;
            if(far && !(L.combo[i] % 2 == 0 || tg == null)) grabT = null;
            if(grabT != null){
                L.longGrab[i] = far;
                if(far) L.longGrabs++;
                startGrab(unit, L, i, grabT);
                return;
            }
        }
        if(tg == null) return;
        float size = tg instanceof Hitboxc h ? h.hitSize() : 16f;
        L.atkId[i] = tg instanceof Unit u ? u.id : -1;
        L.atkBuild[i] = tg instanceof Building b ? b : null;
        float d = Mathf.dst(unit.x, unit.y, tg.getX(), tg.getY());
        if(aiSlash && d <= slashRange + size * 0.4f && (L.combo[i] & 1) == 1 && inZone(unit, i, tg, 1, START)){
            L.sgn[i] = (L.combo[i] + 1 & 2) == 0 ? 1f : -1f;
            planSlash(unit, L, i);
            if(L.slashOk[i]){
                L.mode[i] = SLASH; L.modeT[i] = 0f; L.hitDone[i] = false; L.hits[i].clear(); L.combo[i]++;
                return;
            }
        }
        if(aiStab && inZone(unit, i, tg, 0, START)){
            L.mode[i] = STAB; L.modeT[i] = 0f; L.hitDone[i] = false; L.hits[i].clear(); L.combo[i]++;
        }
    }

    boolean grabbing(Logic L, int id){
        for(int k = 0; k < 3; k++) if((L.mode[k] == GRAB || L.mode[k] == HOLD) && L.atkId[k] == id) return true;
        return false;
    }

    boolean isHeld(Logic L, int id){
        for(int k = 0; k < 3; k++) if(L.held[k] == id) return true;
        return false;
    }

    Unit pickedGrab;
    Posc pickedMelee;

    /** is this enemy worth a long reach? ranged attackers, fliers, healers and bosses that keep away from the melee */
    boolean longGrabWorthy(Unit u){
        return u.type.range >= 150f || u.isFlying() || u.type.canHeal || u.isBoss();
    }

    /**
     * One pass over the enemies in reach of arm i (only inside the attack cone): the nearest one for melee and the best
     * grab candidate (close enemies, or far ones that are worth a long reach). Targets already taken by other arms are avoided.
     */
    void pickTargets(Unit unit, Logic L, int i){
        pickedGrab = null;
        pickedMelee = null;
        toWorld(unit, SHOULDER[i][0], SHOULDER[i][1], SHOULDER[i][2], p3);
        final float sx = p3[0], sy = p3[1];
        final float range = longGrabRange + 40f;
        final boolean canGrabNow = L.grabCd[i] <= 0f && L.energy >= grabCost;
        final Unit[] best = {null, null};
        final float[] bs = {1e18f, 1e18f};
        Units.nearbyEnemies(unit.team, unit.x - range, unit.y - range, range * 2f, range * 2f, u -> {
            if(!u.isAdded() || u.dead || u.team == Team.derelict || !u.checkTarget(true, true)) return;
            float d = u.dst(unit);
            if(d > range + u.hitSize / 2f) return;
            float score = Mathf.dst2(sx, sy, u.x, u.y);
            for(int j = 0; j < 3; j++) if(j != i && L.mode[j] != IDLE && L.atkId[j] == u.id) score += 1e5f;
            if(score < bs[0] && inZone(unit, i, u, 0, START)){ bs[0] = score; best[0] = u; }
            if(canGrabNow && u.hitSize <= maxGrabSize && !u.type.internal && L.thrown.get(u.id, 0f) <= Time.time && !isHeld(L, u.id) && !grabbing(L, u.id)){
                float gs = score;
                boolean close = d <= grabRange && inZone(unit, i, u, 2);
                boolean far = d <= longGrabRange && longGrabWorthy(u) && inZone(unit, i, u, 3);
                if(close && (L.combo[i] % 3 == 2 || u.hitSize < 26f) || far){
                    if(far && !close) gs -= 4e4f; //a far, worthy target comes first
                    if(gs < bs[1]){ bs[1] = gs; best[1] = u; }
                }
            }
        });
        pickedGrab = best[1];
        if(best[0] != null){ pickedMelee = best[0]; return; }
        //buildings only when no unit is in reach (and this arm can reach them)
        Building b = Vars.indexer.findEnemyTile(unit.team, unit.x, unit.y, stabRange + 30f, bd -> inZone(unit, i, bd.x, bd.y, bd.hitSize(), 0, START));
        pickedMelee = b;
    }

    // --------------------------------------------------------------------------------------------------
    // melee: stab and slash
    // --------------------------------------------------------------------------------------------------

    void meleeTick(Unit unit, Logic L, int i){
        int o = i * 3;
        float mt = L.modeT[i];
        boolean stab = L.mode[i] == STAB;
        //every tick until the blade has passed: no victim left (it died, fled out of reach, or left this arm's sector) = retract at
        //once. The wind-up is strict, a strike under way tolerates a little movement of its target. An arm never finishes a swing at air.
        if(mt < (stab ? 24f : 12f + L.slDur[i])){
            boolean windup = mt < (stab ? 15f : 13f);
            Posc tg = targetOf(L, i);
            boolean ok = tg != null && inZone(unit, i, tg, stab ? 0 : 1, windup ? 0f : UNDERWAY);
            if(!ok && !stab && !windup) ok = swingVictim(unit, L, i);
            if(!ok){ abortStrike(L, i); return; }
            if(windup){
                if(!stab){
                    planSlash(unit, L, i);
                    if(!L.slashOk[i]) abortStrike(L, i);
                }
                return;
            }
        }
        if(mt < (stab ? 15f : 13f)) return;
        boolean active = stab ? mt < 24f : mt < 12f + L.slDur[i] + 2f;
        if(!active) return;
        float dmg = stab ? stabDamage + L.energy * 0.06f : slashDamage + L.energy * 0.04f;
        muzzleWorld(unit, L, i, p0);
        bladeTipWorld(unit, L, i, BL, p1);
        float x0 = p0[0], y0 = p0[1], x1 = p1[0], y1 = p1[1];
        if(!L.hitDone[i]){
            L.hitDone[i] = true;
            L.strikes++; L.armStrikes[i]++;
            if(stab) L.stabs++; else L.slashes++;
            L.kick[i] = 1f;
        }
        float rad = BL * SC + 20f;
        float cx = (x0 + x1) / 2f, cy = (y0 + y1) / 2f;
        final int arm = i;
        Units.nearbyEnemies(unit.team, cx - rad, cy - rad, rad * 2f, rad * 2f, u -> {
            if(!u.isAdded() || u.dead || L.hits[arm].contains(u.id) || u.team == Team.derelict) return;
            float d = Intersector.distanceSegmentPoint(x0, y0, x1, y1, u.x, u.y);
            if(d > u.hitSize / 2f + 7f) return;
            L.hits[arm].add(u.id);
            float before = u.health + u.shield;
            if(stab) u.damagePierce(dmg); else u.damage(dmg);
            u.apply(StatusEffects.electrified, 90f);
            float dealt = Math.min(before, dmg);
            gainMelee(unit, L, arm, dealt);
            AstroFx.slash.at(u.x, u.y, Angles.angle(x0, y0, x1, y1), stab ? AstroFx.hot : AstroFx.gold);
            u.vel.add(Angles.trnsx(Angles.angle(x0, y0, x1, y1), 2.5f), Angles.trnsy(Angles.angle(x0, y0, x1, y1), 2.5f));
        });
        Units.nearbyBuildings(cx, cy, rad, bd -> {
            if(bd.team == unit.team || bd.team == Team.derelict || !bd.isValid() || L.hits[arm].contains(bd.id())) return;
            float d = Intersector.distanceSegmentPoint(x0, y0, x1, y1, bd.x, bd.y);
            if(d > bd.hitSize() / 2f + 7f) return;
            L.hits[arm].add(bd.id());
            float before = bd.health;
            bd.damage(unit.team, dmg * 0.8f);
            gainMelee(unit, L, arm, Math.min(before, dmg * 0.8f));
            AstroFx.slash.at(bd.x, bd.y, Angles.angle(x0, y0, x1, y1), stab ? AstroFx.hot : AstroFx.gold);
        });
    }

    boolean victimFlag;

    /** another enemy (not yet hit by this swing) that the arm's slash can still reach */
    boolean swingVictim(Unit unit, Logic L, int i){
        victimFlag = false;
        float r = slashRange + 50f;
        Units.nearbyEnemies(unit.team, unit.x - r, unit.y - r, r * 2f, r * 2f, u -> {
            if(victimFlag || !u.isAdded() || u.dead || u.team == Team.derelict || L.hits[i].contains(u.id)) return;
            if(inZone(unit, i, u, 1, UNDERWAY)) victimFlag = true;
        });
        return victimFlag;
    }

    void gainMelee(Unit unit, Logic L, int i, float dealt){
        L.meleeHits++;
        float gain = dealt * meleeGain;
        L.energy = Math.min(maxEnergy, L.energy + gain);
        L.absorbedEnergy += gain;
        L.absorbGlow = 1f;
        L.flash[i] = 1f;
    }

    // --------------------------------------------------------------------------------------------------
    // grab: reach, close the jaws, drag the prisoner in, crush it and drink its life
    // --------------------------------------------------------------------------------------------------

    void startGrab(Unit unit, Logic L, int i, Unit v){
        L.mode[i] = GRAB; L.modeT[i] = 0f; L.atkId[i] = v.id; L.combo[i]++;
        L.energy -= grabCost * 0.4f;
    }

    void releasePrisoner(Unit unit, Logic L, int i, boolean throwIt){
        Unit v = L.held[i] == -1 ? null : Groups.unit.getByID(L.held[i]);
        if(v != null && v.isAdded() && throwIt){
            float ang = unit.rotation + (i - 1) * 25f;
            v.vel.trns(ang, 15f);
            v.apply(StatusEffects.electrified, 120f);
            L.thrown.put(v.id, Time.time + 120f);
            AstroFx.crush.at(v.x, v.y, 0f, AstroFx.hot);
        }
        L.held[i] = -1; L.holdT[i] = 0f; L.crushFx[i] = 0f; L.longGrab[i] = false;
        if(L.mode[i] == GRAB || L.mode[i] == HOLD){
            L.mode[i] = IDLE;
            L.cd[i] = 40f;
            L.grabCd[i] = throwIt ? 200f : 90f;
        }
    }

    void grabTick(Unit unit, Logic L, int i, float dt){
        Unit v = Groups.unit.getByID(L.atkId[i]);
        if(v == null || !v.isAdded() || v.dead || v.team == unit.team){
            releasePrisoner(unit, L, i, false);
            return;
        }
        muzzleWorld(unit, L, i, p0);
        if(L.mode[i] == GRAB){
            if(L.modeT[i] > 8f && !inZone(unit, i, v, L.longGrab[i] ? 3 : 2)){ releasePrisoner(unit, L, i, false); return; }
            float d = Mathf.dst(p0[0], p0[1], v.x, v.y);
            if(d < v.hitSize * 0.45f + 10f){
                L.mode[i] = HOLD; L.holdT[i] = 0f; L.held[i] = v.id; L.grabs++;
                toBody(unit, v.x, v.y, ATZ, p2);
                L.gx[i] = p2[0]; L.gy[i] = p2[1];
                L.kick[i] = 1f; L.flash[i] = 1f;
                AstroFx.crush.at(v.x, v.y, 0f, AstroFx.hot);
            }else if(L.modeT[i] > (L.longGrab[i] ? 120f : 75f) || v.dst(unit) > (L.longGrab[i] ? LONGREACH : REACH) * SC + 140f){
                releasePrisoner(unit, L, i, false);
            }
            return;
        }
        //HOLD
        v.vel.setZero();
        v.x += (p0[0] - v.x) * 0.45f * dt;
        v.y += (p0[1] - v.y) * 0.45f * dt;
        v.apply(StatusEffects.disarmed, 12f);
        L.holdT[i] += dt;
        L.crushFx[i] = Mathf.clamp(L.holdT[i] / crushTime);
        if(L.holdT[i] > 36f){
            float dps = crushDps * (0.6f + 0.8f * L.energy / maxEnergy) * (0.4f + 1.4f * L.crushFx[i]);
            float before = v.health + v.shield;
            v.damagePierce(dps / 60f * dt);
            float dealt = Math.min(before, dps / 60f * dt);
            L.energy = Math.min(maxEnergy, L.energy + dealt * crushGain);
            L.absorbedEnergy += dealt * crushGain;
            L.absorbGlow = 1f;
            if(Mathf.chance(0.18f * dt)) AstroFx.crush.at(v.x + Mathf.range(v.hitSize / 3f), v.y + Mathf.range(v.hitSize / 3f), 0f, fieldColor);
        }
        if(L.holdT[i] >= crushTime){
            v.damagePierce(250f + L.energy * 0.12f);
            releasePrisoner(unit, L, i, true);
            L.kick[i] = 1f;
        }
    }

    /** gentle magnetic pull on light enemies around the unit */
    void magnetPull(Unit unit, Logic L, float dt){
        float R = catchRadius * 1.5f;
        Units.nearbyEnemies(unit.team, unit.x - R - 40f, unit.y - R - 40f, (R + 40f) * 2f, (R + 40f) * 2f, u -> {
            if(!u.isAdded() || u.dead || u.hitSize > maxGrabSize * 1.7f || isHeld(L, u.id)) return;
            if(L.thrown.get(u.id, 0f) > Time.time) return;
            float d = u.dst(unit);
            if(d > R + 40f || d < 1f) return;
            float f = (1f - d / (R + 40f)) * 0.45f * (0.4f + 0.6f * L.energy / maxEnergy);
            Tmp.v1.set(unit.x - u.x, unit.y - u.y).setLength(f * u.mass() * 0.02f * dt);
            u.impulse(Tmp.v1);
        });
    }

    public void drain(Unit unit, Logic L){
        int[] n = {0};
        float R = drainRange;
        Units.nearbyEnemies(unit.team, unit.x - R, unit.y - R, R * 2f, R * 2f, u -> {
            if(n[0] >= 3 || !u.isAdded() || u.dead || u.shield < 6f || u.dst(unit) > R) return;
            float take = Math.min(u.shield, drainPerPulse);
            u.shield -= take;
            tendril(unit, L, n[0]++, u.x, u.y, take);
        });
        if(n[0] < 3){
            Units.nearbyBuildings(unit.x, unit.y, R, bd -> {
                if(n[0] >= 3 || bd.team == unit.team || bd.team == Team.derelict || !bd.isValid()) return;
                float got = 0f;
                if(bd instanceof ForceProjector.ForceBuild fb && !fb.broken && fb.buildup < 1e6f){
                    float take = drainPerPulse * 1.5f;
                    fb.buildup += take;
                    got = take;
                }else if(bd.power != null && bd.power.graph != null && bd.power.graph.getBatteryStored() > 5f){
                    got = bd.power.graph.useBatteries(drainPerPulse * 2f) * 0.5f;
                }
                if(got > 0f) tendril(unit, L, n[0]++, bd.x, bd.y, got);
            });
        }
    }

    /** energy taken from a shield / battery arrives without any visible link */
    public void tendril(Unit unit, Logic L, int slot, float x, float y, float amount){
        L.energy = Math.min(maxEnergy, L.energy + amount * 0.9f);
        L.absorbedEnergy += amount * 0.9f;
        L.drained++;
        L.absorbGlow = 1f;
        int arm = nearestArm(unit, L, x, y, false);
        L.flash[arm] = Math.max(L.flash[arm], 0.5f);
        AstroFx.drain.at(x, y, 0f, fieldColor);
    }

    @Override
    public void killed(Unit unit){
        super.killed(unit);
        Logic L = logics.get(unit.id);
        if(L == null) return;
        for(int i = 0; i < 3; i++) releasePrisoner(unit, L, i, false);
        //a charged Detainer detonates: its store is released as a shockwave
        float e = L.energy;
        if(e > 200f){
            float radius = 90f + e * 0.05f;
            Damage.damage(unit.team, unit.x, unit.y, radius, e * 0.5f, true);
            AstroFx.overloadBlast.at(unit.x, unit.y, radius, AstroFx.gold);
        }
    }

    // ===================================================================================================
    // drawing
    // ===================================================================================================

    @Override
    protected void animate(Unit unit, DState d, float delta, boolean payload){
        Logic L = logic(unit);
        float time = Time.time + unit.id * 17f;
        float elev = payload ? 0f : Mathf.clamp(unit.elevation);
        float fx = Mathf.cosDeg(unit.rotation), fy = Mathf.sinDeg(unit.rotation);
        float fwd = unit.vel.x * fx + unit.vel.y * fy, lat = unit.vel.x * fy - unit.vel.y * fx;
        float spd = Math.max(speed, 0.01f);
        float turn = delta > 0f ? Mathf.clamp(Angles.angleDist(d.lastRot, unit.rotation) * Mathf.sign(wrap180(unit.rotation - d.lastRot)) / Math.max(delta, 0.01f) / 4f, -1f, 1f) : d.turn;
        d.lastRot = unit.rotation;
        d.turn = Mathf.lerp(d.turn, turn, Mathf.clamp(0.1f * delta));
        d.speedF = Mathf.lerp(d.speedF, Mathf.clamp(unit.vel.len() / spd), Mathf.clamp(0.1f * delta));
        d.pitch = Mathf.lerp(d.pitch, Mathf.clamp(-fwd / spd * 5f, -8f, 8f), Mathf.clamp(0.1f * delta));
        d.roll = Mathf.lerp(d.roll, Mathf.clamp(lat / spd * 8f - d.turn * 8f, -14f, 14f), Mathf.clamp(0.1f * delta));

        rig.move(BODY, 0, 0, Mathf.sin(time, 28f, 0.45f) * elev);
        rig.rot(BODY, 0, d.pitch + Mathf.cos(time, 37f, 0.7f) * elev);
        rig.rot(BODY, 1, d.roll + Mathf.sin(time, 43f, 0.8f) * elev);

        float ef = L.energy / maxEnergy;
        rig.glow[BODY] = 0.85f + 0.3f * ef + 0.25f * L.absorbGlow + L.overloadFlash + L.arrive * 1.4f + Mathf.clamp(L.warpWind / WARP_WIND) * 0.9f;
        rig.glow[THR] = 0.65f + 0.45f * d.speedF + Mathf.absin(time, 6f, 0.15f);
        //gyro ring around the reactor dome: idles slowly, races while energy is stored / absorbed / after a warp
        d.coreSpin = (d.coreSpin + delta * (1.4f + 5f * ef + 7f * Mathf.clamp(L.absorbGlow) + 5f * L.arrive)) % 360f;
        rig.rot(CORE, 2, d.coreSpin);
        rig.glow[CORE] = rig.glow[BODY];

        for(int i = 0; i < 3; i++){
            int o = i * 3;
            float[] s = SHOULDER[i];
            float ch = L.charge[i];
            float kx = L.knee[o], ky = L.knee[o + 1], kz = L.knee[o + 2];
            float wx = L.wrist[o], wy = L.wrist[o + 1], wz = L.wrist[o + 2];
            //upper arm: outer sleeve (fixed length) + inner rod that slides out when the arm stretches
            float l1 = rig.aim(UPI[i], s[0], s[1], s[2], kx, ky, kz, 0f, 0f, 1f);
            rig.scale(UPI[i], 1f, 1f, l1 / U);
            rig.aim(UPO[i], s[0], s[1], s[2], kx, ky, kz, 0f, 0f, 1f);
            rig.scale(UPO[i], 1f, 1f, Math.min(1f, l1 / (U * 0.82f)));
            //elbow block follows the upper arm's orientation
            rig.aim(ELB[i], kx, ky, kz, kx + (kx - s[0]), ky + (ky - s[1]), kz + (kz - s[2]), 0f, 0f, 1f);
            float l2 = rig.aim(FOI[i], kx, ky, kz, wx, wy, wz, 0f, 0f, 1f);
            rig.scale(FOI[i], 1f, 1f, l2 / F);
            rig.aim(FOO[i], kx, ky, kz, wx, wy, wz, 0f, 0f, 1f);
            rig.scale(FOO[i], 1f, 1f, Math.min(1f, l2 / (F * 0.82f)));
            //claw roll: the jaws open along a hint that rotates around the claw axis
            float ddx = L.dir[o], ddy = L.dir[o + 1], ddz = L.dir[o + 2];
            float e1x = -ddy, e1y = ddx, e1z = 0f;
            float el = Mathf.len(e1x, e1y);
            if(el < 0.05f){ e1x = 1f; e1y = 0f; el = 1f; }
            e1x /= el; e1y /= el;
            //e2 = dir x e1
            float e2x = ddy * e1z - ddz * e1y, e2y = ddz * e1x - ddx * e1z, e2z = ddx * e1y - ddy * e1x;
            float ca = Mathf.cosDeg(L.roll[i]), sa = Mathf.sinDeg(L.roll[i]);
            float hx = e1x * ca + e2x * sa, hy = e1y * ca + e2y * sa, hz = e1z * ca + e2z * sa;
            rig.aim(WRI[i], wx, wy, wz, L.tip[o], L.tip[o + 1], L.tip[o + 2], hx, hy, hz);
            rig.aim(CL[i], wx, wy, wz, L.tip[o], L.tip[o + 1], L.tip[o + 2], hx, hy, hz);
            float ang = Mathf.lerp(-15f, 27f, L.open[i]);
            rig.rot(JL[i], 1, ang);
            rig.rot(JR[i], 1, -ang);
            rig.glow[SH[i]] = 0.6f + ch * 0.9f;
            rig.glow[UPO[i]] = 0.7f + ch * 0.9f + L.flash[i] * 0.4f;
            rig.glow[UPI[i]] = 0.7f + ch * 0.6f;
            rig.glow[FOO[i]] = 0.7f + ch * 0.9f + L.flash[i] * 0.4f;
            rig.glow[FOI[i]] = 0.7f + ch * 0.6f;
            rig.glow[JL[i]] = 0.7f + ch * 0.8f;
            rig.glow[JR[i]] = 0.7f + ch * 0.8f;
            //blade
            boolean bl = L.blade[i] > 0.04f;
            rig.hidden[BLADE[i]] = !bl;
            if(bl){
                rig.scale(BLADE[i], 1f, 1f, L.blade[i]);
                rig.glow[BLADE[i]] = 0.9f + 0.6f * L.blade[i];
            }
            //beam between the jaws: only while energy flows through the claw
            float w = Mathf.clamp(ch * 1.25f);
            boolean show = w > 0.12f;
            rig.hidden[BEAM[i]] = rig.hidden[HALO[i]] = !show;
            if(show){
                float wb = 0.35f + 0.85f * w + 0.15f * Mathf.absin(time + i * 9f, 3f, 1f);
                rig.scale(BEAM[i], wb, wb, 1f);
                rig.scale(HALO[i], wb * (0.9f + 0.25f * Mathf.absin(time + i * 5f, 4f, 1f)), wb * 1.1f, 1f);
                rig.alpha[BEAM[i]] = Mathf.clamp(w * 1.6f);
                rig.alpha[HALO[i]] = 0.8f * Mathf.clamp(w * 1.6f);
            }
        }
        renderer.pose(rig, unit.rotation, 0f, 0f, hover * elev, modelScale * Draw.xscl);
    }

    @Override
    protected void drawExtras(Unit unit, DState d, float z, boolean step, boolean payload){
        if(payload) return;
        Logic L = logic(unit);
        float time = Time.time + unit.id * 13f;
        float elev = Mathf.clamp(unit.elevation);
        if(L.aegisVis > 0.03f) drawAegis(unit, L, z, elev);
        float ag = Mathf.clamp(L.absorbGlow);
        boolean any = ag > 0.02f;
        for(int i = 0; i < 3 && !any; i++) any |= L.charge[i] > 0.12f || L.held[i] != -1;
        if(!any){
            Draw.reset();
            return;
        }
        float ef = L.energy / maxEnergy;
        Draw.z(z + 0.015f);
        Draw.blend(Blending.additive);

        //energy swirl around the core (the orange outline of the reference): only while absorbing
        if(ag > 0.02f){
            screen(unit, BODY, 0f, 0f, 4.5f, v3);
            Lines.stroke(1.6f);
            for(int k = 0; k < 3; k++){
                float px = 0, py = 0; boolean first = true;
                for(int sgm = 0; sgm <= 16; sgm++){
                    float u = sgm / 16f;
                    float ang = time * 3.4f + k * 120f + u * 240f;
                    float rad = 9f + 11f * u;
                    screen(unit, BODY, Mathf.cosDeg(ang) * rad, Mathf.sinDeg(ang) * rad * 0.8f, 6f + Mathf.sin(u * Mathf.PI) * 3.4f, v3);
                    Draw.color(AstroFx.amber, Color.white, u * 0.5f);
                    Draw.alpha(0.75f * ag * (1f - u * u) * elev);
                    if(!first) Lines.line(px, py, v3[0], v3[1]);
                    px = v3[0]; py = v3[1]; first = false;
                }
            }
        }

        //claws: glow between the jaws and arcs between the magnet tips, only while charged by an absorption
        for(int i = 0; i < 3; i++){
            float ch = L.charge[i];
            if(ch < 0.12f) continue;
            int jl = JL[i], jr = JR[i];
            screen(unit, jl, 2.0f, 0f, -J + 1.2f, p0);
            screen(unit, jr, -2.0f, 0f, -J + 1.2f, p1);
            float cx = (p0[0] + p1[0]) * 0.5f, cy = (p0[1] + p1[1]) * 0.5f;
            //these glows are drawn additively over the whole unit: never let them shine through the hull when the claw is turned away behind it
            screen(unit, BODY, 0f, 0f, 4.8f, v3);
            float hullTop = v3[2], hx = v3[0], hy = v3[1];
            float gapH = (p0[2] + p1[2]) * 0.5f;
            if(gapH < hullTop && Mathf.dst(cx, cy, hx, hy) < 11f * modelScale) continue;
            Draw.color(fieldColor, 0.25f * Mathf.clamp(ch));
            Fill.circle(cx, cy, 3.6f + 6f * ch + 3f * L.overloadFlash * 3f);
            Draw.color(Color.white, 0.6f * Mathf.clamp(ch));
            Fill.circle(cx, cy, 1.2f + 2.6f * ch);
            if(ch > 0.3f){
                Mathf.rand.setSeed(unit.id * 131L + i * 17L + (long)(Time.time / 2f));
                float px = p0[0], py = p0[1];
                Lines.stroke(1f + ch);
                Draw.color(AstroFx.hot, Mathf.clamp(ch));
                int n = 5;
                for(int k = 1; k <= n; k++){
                    float u = k / (float)n;
                    float nx = Mathf.lerp(p0[0], p1[0], u) + (k == n ? 0f : Mathf.range(1.8f));
                    float ny = Mathf.lerp(p0[1], p1[1], u) + (k == n ? 0f : Mathf.range(1.8f));
                    Lines.line(px, py, nx, ny);
                    px = nx; py = ny;
                }
            }
        }

        //prisoners: closing ring while they are crushed
        for(int i = 0; i < 3; i++){
            if(L.held[i] == -1) continue;
            Unit v = Groups.unit.getByID(L.held[i]);
            if(v == null) continue;
            Draw.color(AstroFx.hot, 0.5f + 0.4f * L.crushFx[i]);
            Lines.stroke(1.2f + L.crushFx[i] * 1.6f);
            Lines.circle(v.x, v.y, v.hitSize * (0.75f - 0.25f * L.crushFx[i]) + Mathf.absin(time, 3f, 1.2f));
        }

        Draw.blend();
        Draw.color();
    }

    /** the aegis dome: a soft gold bubble with rotating hexagons and a strength arc; flashes where damage lands */
    void drawAegis(Unit unit, Logic L, float z, float elev){
        float a = L.aegisVis * elev;
        float r = 80f + Mathf.absin(Time.time, 14f, 1.6f) + 3f * L.aegisHit;
        Draw.z(z + 0.02f);
        Draw.blend(Blending.additive);
        Tmp.c1.set(fieldColor).lerp(Color.white, L.aegisHit * 0.7f);
        Fill.light(unit.x, unit.y, Lines.circleVertices(r), r, Color.clear, Tmp.c2.set(Tmp.c1).a(0.32f * a));
        Lines.stroke(1.6f + 1.4f * L.aegisHit);
        Draw.color(Tmp.c1, 0.75f * a);
        Lines.circle(unit.x, unit.y, r);
        Lines.stroke(1f);
        for(int k = 0; k < 3; k++){
            Draw.color(AstroFx.amber, 0.3f * a);
            Lines.poly(unit.x, unit.y, 6, r * (0.46f + 0.17f * k), Time.time * (k % 2 == 0 ? 0.6f : -0.8f) + k * 20f);
        }
        Lines.stroke(2.4f);
        Draw.color(Color.white, 0.8f * a);
        Lines.arc(unit.x, unit.y, r + 4f, Mathf.clamp(unit.shield / aegisMax), 90f);
        Draw.blend();
        Draw.reset();
    }

    @Override
    public void drawLight(Unit unit){
        super.drawLight(unit);
        if(!hasLogic(unit)) return;
        Drawf.light(unit.x, unit.y, hitSize * 2f, fieldColor, 0.1f + 0.3f * logic(unit).absorbGlow);
    }
}
