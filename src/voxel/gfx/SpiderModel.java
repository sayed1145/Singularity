package voxel.gfx;

import static voxel.gfx.Palette.*;

/**
 * Arachne: a four-legged assault walker. Low armoured hull carried on four long legs (femur up, tibia down to a
 * claw), a 360-degree turret with a long main cannon, two rotary machine guns and a missile pod.
 * Legs follow the engine's own leg simulation (foot positions) and are solved with two-bone IK, knees high.
 */
public final class SpiderModel{
    public static int BASE, HULL, TURRET, CANNON, CBARREL, POD;
    public static final int[] MG = new int[2], MGB = new int[2], FEMUR = new int[4], TIBIA = new int[4], EYE = new int[1];
    /** leg mounts in the hull frame; index matches vanilla leg order (FL, BL, BR, FR) */
    public static final float[] mountX = {-5.6f, -5.6f, 5.6f, 5.6f}, mountY = {5.2f, -5.2f, -5.2f, 5.2f};
    public static final float hullZ = 12f, femurLen = 15f, tibiaLen = 19f, mountZ = -0.4f;
    public static final float cannonMuzzle = 11.2f, mgMuzzle = 3.3f, podExit = 2.3f;

    private SpiderModel(){}

    private static final float[] mount = new float[3], knee = new float[3], foot = new float[3], guard = new float[2];
    /** half opening of each leg's sector (legs are 90 degrees apart: 2 x 38 leaves a 14 degree gap) */
    public static final float sectorHalf = 38f, footMinR = 9f, footMaxR = 31f;
    /** ground projection of each leg after the last solve: hip, knee, foot (x, y) - read by the regression test */
    public static final float[][] projected = new float[4][6];

    /** Rest direction of leg i (seen from the hull centre and from the hip alike), degrees, atan2(x, y) convention (FL -45, BL -135, BR 135, FR 45). */
    public static float restAngle(int i){
        return (float)Math.toDegrees(Math.atan2(mountX[i], mountY[i]));
    }

    /**
     * Foot target for leg i from the engine's foot position (base frame): kept inside the leg's own sector so a
     * foot that stayed planted while the hull turned can never reach into the neighbouring leg's quadrant.
     */
    public static void guardedLeg(Rig rig, int i, float tx, float ty, float tz){
        //the cone's apex is the hip itself: hip, knee and foot then lie on one ray inside the cone, and the cones
        //of neighbouring hips diverge, so no two legs can ever intersect (independent of where the knee bends)
        LegGuard.clamp(tx - mountX[i], ty - mountY[i], restAngle(i), sectorHalf, footMinR, footMaxR, guard);
        leg(rig, i, mountX[i] + guard[0], mountY[i] + guard[1], tz);
    }

    /** Solves leg i for a foot target (tx,ty,tz) in the base frame; the hull local matrix must be final. */
    public static void leg(Rig rig, int i, float tx, float ty, float tz){
        rig.apply(HULL, mountX[i], mountY[i], mountZ, mount);
        float ox = tx - mount[0], oy = ty - mount[1];
        float ol = Math.max((float)Math.sqrt(ox * ox + oy * oy), 1e-3f);
        ox /= ol; oy /= ol;
        Rig.knee(mount[0], mount[1], mount[2], tx, ty, tz, femurLen, tibiaLen, ox * 0.3f, oy * 0.3f, 1f, knee);
        float kx = tx - knee[0], ky = ty - knee[1], kz = tz - knee[2];
        float kl = Math.max((float)Math.sqrt(kx * kx + ky * ky + kz * kz), 1e-4f);
        foot[0] = knee[0] + kx / kl * tibiaLen;
        foot[1] = knee[1] + ky / kl * tibiaLen;
        foot[2] = knee[2] + kz / kl * tibiaLen;
        float[] pj = projected[i];
        pj[0] = mount[0]; pj[1] = mount[1]; pj[2] = knee[0]; pj[3] = knee[1]; pj[4] = foot[0]; pj[5] = foot[1];
        rig.aim(FEMUR[i], mount[0], mount[1], mount[2], knee[0], knee[1], knee[2], -oy, ox, 0f);
        rig.aim(TIBIA[i], knee[0], knee[1], knee[2], foot[0], foot[1], foot[2], -oy, ox, 0f);
    }

    /** Default stance: each foot out along its diagonal at reach r (design units from the mount). */
    public static void stance(Rig rig, float r){
        for(int i = 0; i < 4; i++){
            float a = (float)Math.atan2(mountX[i], mountY[i]);
            guardedLeg(rig, i, mountX[i] + (float)Math.sin(a) * r, mountY[i] + (float)Math.cos(a) * r, 0f);
        }
    }

    public static void rest(Rig rig){
        rig.reset();
        rig.rot(CANNON, 0, 1.5f);
        rig.rot(POD, 0, 6f);
        stance(rig, 22f);
    }

    public static Rig build(){
        Rig r = new Rig();
        BASE = r.bone(-1, 0, 0, 0);
        HULL = r.bone(BASE, 0, 0, hullZ);
        TURRET = r.bone(HULL, 0, 0.4f, 1.7f);
        CANNON = r.bone(TURRET, 0, 4.3f, 1.9f);
        CBARREL = r.bone(CANNON, 0, 1.2f, 0);
        POD = r.bone(TURRET, 0, -3.3f, 3.3f);
        EYE[0] = r.bone(HULL, 0, 0, 0);
        for(int i = 0; i < 2; i++){
            float s = i == 0 ? -1f : 1f;
            MG[i] = r.bone(TURRET, s * 4.7f, 1.4f, 1.5f);
            MGB[i] = r.bone(MG[i], 0, 2.2f, 0.1f);
        }
        for(int i = 0; i < 4; i++){
            FEMUR[i] = r.bone(BASE, mountX[i], mountY[i], hullZ + mountZ);
            TIBIA[i] = r.bone(BASE, mountX[i] * 2f, mountY[i] * 2f, hullZ + 6f);
        }

        // ------------------------------------------------ hull
        Mesh m = r.part(HULL); dark(m);
        m.taper(0, -0.3f, -3.0f, 7.0f, 9.4f, -1.2f, 9.6f, 12.2f, 0, 0, false);
        r.shadow(0);
        m = r.part(HULL); armor(m);
        m.hexa(false,
            -5.0f, -6.2f, -1.2f, 5.0f, -6.2f, -1.2f, 5.0f, 6.8f, -1.2f, -5.0f, 6.8f, -1.2f,
            -4.2f, -5.4f, 1.8f, 4.2f, -5.4f, 1.8f, 4.2f, 5.0f, 1.8f, -4.2f, 5.0f, 1.8f);
        r.shadow(0);
        int hull = r.lastIndex();
        //nose wedge with sensor eyes
        m = r.part(HULL); armorShade(m);
        m.hexa(false,
            -3.2f, 6.8f, -1.2f, 3.2f, 6.8f, -1.2f, 2.2f, 8.6f, -1.0f, -2.2f, 8.6f, -1.0f,
            -2.8f, 5.0f, 1.2f, 2.8f, 5.0f, 1.2f, 1.8f, 7.4f, 0.4f, -1.8f, 7.4f, 0.4f);
        int nose = r.lastIndex();
        m = r.part(EYE[0]); red(m);
        m.box(-1.5f, 7.9f, -0.2f, -0.8f, 8.25f, 0.25f);
        m.box(0.8f, 7.9f, -0.2f, 1.5f, 8.25f, 0.25f);
        m.box(-0.45f, 8.1f, -0.55f, 0.45f, 8.45f, -0.15f);
        r.on(nose);
        //side stripes, rear engine deck
        for(int i = 0; i < 2; i++){
            float s = i == 0 ? -1f : 1f;
            m = r.part(HULL); team(m);
            m.hexa(false,
                s * 4.97f, -2.5f, -0.4f, s * 5.07f, -2.5f, -0.4f, s * 5.07f, 3.5f, -0.4f, s * 4.97f, 3.5f, -0.4f,
                s * 4.7f, -2.5f, 0.6f, s * 4.8f, -2.5f, 0.6f, s * 4.8f, 3.5f, 0.6f, s * 4.7f, 3.5f, 0.6f);
            r.on(hull).detail();
        }
        m = r.part(HULL); grey(m);
        m.taper(0, -6.6f, -1.6f, 6.4f, 2.4f, 1.0f, 5.6f, 1.6f, 0, 0.3f, false);
        int deck = r.lastIndex();
        m = r.part(HULL); glow(m);
        m.box(-2.2f, -7.83f, -0.9f, 2.2f, -7.7f, -0.1f);
        r.on(deck).detail();
        //leg mount drums
        for(int i = 0; i < 4; i++){
            m = r.part(HULL); dark(m);
            m.at(mountX[i], mountY[i], mountZ).cyl(10, 1.9f, -1.6f, 1.4f);
            m = r.part(HULL); steel(m);
            m.at(mountX[i], mountY[i], mountZ).cyl(10, 1.2f, 1.4f, 1.8f);
            r.detail();
        }

        // ------------------------------------------------ turret
        m = r.part(TURRET); dark(m);
        m.cyl(12, 4.1f, -0.3f, 0.5f);
        m = r.part(TURRET); armor(m);
        m.hexa(false,
            -4.0f, -4.4f, 0.4f, 4.0f, -4.4f, 0.4f, 4.4f, 3.8f, 0.4f, -4.4f, 3.8f, 0.4f,
            -3.4f, -3.9f, 3.3f, 3.4f, -3.9f, 3.3f, 3.2f, 2.6f, 3.3f, -3.2f, 2.6f, 3.3f);
        r.shadow(0);
        int turret = r.lastIndex();
        m = r.part(TURRET); team(m);
        m.cbox(0, -0.6f, 3.3f, 1.2f, 5.0f, 0.12f);
        r.on(turret).detail();
        m = r.part(TURRET); grey(m);
        m.taper(-2.1f, -2.6f, 3.3f, 1.6f, 1.4f, 4.1f, 1.2f, 1.1f, 0, 0, false);
        r.on(turret).detail();
        m = r.part(TURRET); glow(m);
        m.cbox(-2.1f, -2.05f, 3.6f, 1.0f, 0.12f, 0.35f);
        r.on(turret).detail();
        m = r.part(TURRET); grey(m);
        m.taper(2.8f, -3.6f, 3.2f, 0.25f, 0.25f, 7.8f, 0.12f, 0.12f, 0, -0.4f, false);
        r.detail();

        //main cannon: mantlet, barrel, muzzle brake, bore evacuator
        m = r.part(CANNON); dark(m);
        m.cbevel(0, 0.2f, -1.3f, 3.4f, 2.2f, 2.6f, 0.4f);
        m = r.part(CBARREL); steel(m);
        m.at(0, 0, 0).rot(0, -90).cyl(10, 0.72f, 0, 10.2f);
        m = r.part(CBARREL); dark(m);
        m.at(0, 0, 0).rot(0, -90).cyl(10, 1.05f, 4.2f, 5.9f);
        m = r.part(CBARREL); dark(m);
        m.cbox(0, 10.6f, -0.9f, 2.2f, 1.4f, 1.8f);

        //rotary machine guns
        for(int i = 0; i < 2; i++){
            float s = i == 0 ? -1f : 1f;
            m = r.part(MG[i]); grey(m);
            m.bevel(-1.0f, -1.6f, -1.0f, 1.0f, 2.2f, 1.0f, 0.3f);
            r.shadow(0);
            int box = r.lastIndex();
            m = r.part(MG[i]); black(m);
            m.box(s > 0 ? 1.0f : -1.1f, -1.0f, -0.6f, s > 0 ? 1.1f : -1.0f, 0.4f, 0.2f);
            r.on(box).detail();
            m = r.part(MGB[i]); dark(m);
            m.at(0, 0, 0).rot(0, -90).cyl(6, 0.75f, 0, 0.5f);
            m = r.part(MGB[i]); steel(m);
            for(int b = 0; b < 3; b++){
                float a = b * 120f;
                float bx = 0.42f * (float)Math.cos(Math.toRadians(a)), bz = 0.42f * (float)Math.sin(Math.toRadians(a));
                m.at(bx, 0, bz).rot(0, -90).cyl(5, 0.2f, 0.5f, 3.3f);
            }
            m = r.part(MGB[i]); dark(m);
            m.at(0, 0, 0).rot(0, -90).cyl(6, 0.72f, 2.6f, 3.0f);
            r.detail();
        }

        //missile pod
        m = r.part(POD); armorShade(m);
        m.bevel(-1.8f, -1.6f, 0, 1.8f, 2.2f, 1.9f, 0.3f);
        int pod = r.lastIndex();
        m = r.part(POD); black(m);
        m.box(-1.5f, 2.2f, 0.25f, 1.5f, 2.3f, 1.65f);
        r.on(pod);
        m = r.part(POD); amber(m);
        for(int k = 0; k < 4; k++){
            float x = -1.0f + (k % 2) * 1.4f, z = 0.45f + (k / 2) * 0.7f;
            m.box(x, 2.3f, z, x + 0.6f, 2.36f, z + 0.4f);
        }
        r.on(pod).detail();

        // ------------------------------------------------ legs (segments hang along -z from their joint)
        for(int i = 0; i < 4; i++){
            int g = 1 + i;
            m = r.part(FEMUR[i]); dark(m);
            m.at(0, 0, 0).rot(1, 90).cyl(8, 1.25f, -1.1f, 1.1f);
            m = r.part(FEMUR[i]); armor(m);
            m.taper(0, 0, -femurLen + 0.6f, 1.5f, 1.8f, 0.4f, 2.3f, 2.8f, 0, 0.1f, true);
            r.shadow(g);
            int fem = r.lastIndex();
            m = r.part(FEMUR[i]); team(m);
            m.taper(0, 1.02f, -femurLen * 0.72f, 0.7f, 0.1f, -2.6f, 0.9f, 0.1f, 0, 0.35f, true);
            r.on(fem).detail();

            m = r.part(TIBIA[i]); dark(m);
            m.at(0, 0, 0).rot(1, 90).cyl(8, 1.1f, -1.0f, 1.0f);
            m = r.part(TIBIA[i]); grey(m);
            m.taper(0, 0, -tibiaLen + 3.4f, 1.0f, 1.1f, 0.2f, 1.9f, 2.2f, 0, 0, true);
            r.shadow(g);
            m = r.part(TIBIA[i]); armorShade(m);
            m.taper(0, 0.75f, -7.5f, 1.3f, 0.7f, -0.8f, 1.7f, 1.0f, 0, 0.3f, true);
            r.detail();
            m = r.part(TIBIA[i]); black(m);
            m.taper(0, 0, -tibiaLen, 0.15f, 0.15f, -tibiaLen + 3.4f, 1.1f, 1.2f, 0, 0, true);
        }

        r.half = 22f;
        r.height = 18f;
        return r.finish();
    }
}
