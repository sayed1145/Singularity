package astro.content;

import arc.graphics.Color;
import arc.math.*;
import astro.g3d.*;

import static astro.g3d.Mesh.*;

/**
 * The Astro Detainer, "obsidian" livery: a broad arrow-head chassis in pure black with three articulated claw arms.
 *
 * <p>Chassis: a long chamfered fuselage with a sloped nose, two swept wing plates carrying the shoulder pauldrons,
 * a dorsal ridge with the reactor dome and a free-spinning gyro ring, a rear pylon that carries the third shoulder,
 * a triple thruster block at the back and four hover pads under the wings. Everything is matte black with amber
 * emissive accents; the only lighter tone is the steel of the telescopic rods, so that an extending arm reads.
 *
 * <p>Every arm has the same anatomy as before: shoulder socket, telescopic upper arm (octagonal sleeve + inner rod),
 * elbow block, telescopic forearm, wrist cube, pillar, tapered bracket and two hinged jaws. Arm segments hang down
 * their local -z so the two-bone IK in {@link DetainerType} can aim them with {@link Rig#aim}. All kinematic
 * constants ({@link #U}, {@link #F}, {@link #S}, {@link #J}, {@link #SHOULDER}, ...) are unchanged.
 *
 * <p>All coordinates of this class are in the BODY frame (x right, y forward, z up, origin at the core).
 */
public final class DetainerModel{
    private DetainerModel(){}

    /** height of the body origin above the model floor (before the hover lift) */
    public static final float BZ = 11f;
    /** upper arm, forearm, pillar and jaw length (rest values: every arm section is telescopic) */
    public static final float U = 17f, F = 17f, S = 9f, J = 9f;
    /** longest stretch factor of the telescopic upper arm / forearm */
    public static final float KMAX = 1.8f;
    /** length of the stab blade that slides out between the jaws */
    public static final float BL = 26f;
    /** bracket centre to jaw hinge distance */
    public static final float HINGE = 4.6f;
    public static final float HALF = 48f;

    /** shoulder sockets: left, right, back (body frame) */
    public static final float[][] SHOULDER = {{-10.6f, 0f, 5.0f}, {10.6f, 0f, 5.0f}, {0f, -9.6f, 15.4f}};

    /** body, thrusters (speed glow) and the purely decorative gyro ring around the reactor dome */
    public static int BODY, THR, CORE;
    public static final int[] SH = new int[3], UPI = new int[3], UPO = new int[3], ELB = new int[3], FOI = new int[3],
        FOO = new int[3], WRI = new int[3], CL = new int[3], JL = new int[3], JR = new int[3], BEAM = new int[3],
        HALO = new int[3], BLADE = new int[3];

    //all-black livery: three near-black tones for the armour, gunmetal for joints, steel only for the sliding rods
    static final Color hull = Color.valueOf("111216"), plate = Color.valueOf("1e2127"), trim = Color.valueOf("31353e"),
        deep = Color.valueOf("06070a"), rod = Color.valueOf("4a505b"), jawA = Color.valueOf("1e2026"), jawB = Color.valueOf("2b2f37"),
        amber = Color.valueOf("ff8a1c"), amberHi = Color.valueOf("ffb23e"), beamCore = Color.valueOf("fff3c2"),
        beamHalo = Color.valueOf("ffa726"), bladeC = Color.valueOf("f2f6ff");

    /** leading / trailing edge of the swept wings (y as a function of |x|), root at |x| = 8.5, tip at |x| = 21 */
    static float lead(float ax){ return 9f - 6f * (ax - 8.5f) / 12.5f; }
    static float trail(float ax){ return -13f + 5f * (ax - 8.5f) / 12.5f; }

    private static Mesh mat(Mesh m, Color c, boolean metal){
        return m.at(0, 0, 0).color(c).style(metal ? Mesh.metal : 0, matPlain);
    }

    private static Mesh glow(Mesh m, Color c){
        return m.at(0, 0, 0).color(c).style(emissive, matPlain);
    }

    public static Rig build(){
        Rig r = new Rig();
        r.half = HALF;
        r.height = 70f;
        r.stackSort = false;

        BODY = r.bone(-1, 0, 0, BZ);
        THR = r.bone(BODY, 0, 0, 0);
        CORE = r.bone(BODY, 0, 4f, 0);

        //---------------------------------------------------------------- fuselage: long chamfered hull, keel below, sloped nose in front
        mat(r.part(BODY), hull, true).cbevel(0, -1f, -4.0f, 17f, 30f, 6.6f, 1.0f);
        int fus = r.lastIndex();
        r.shadow(0);
        mat(r.part(BODY), deep, true).taper(0, -1.5f, -6.4f, 9f, 20f, -4.0f, 13f, 25f, 0, 0, false);
        r.under(fus, 1).shadow(0);
        //nose: its root matches the front face of the fuselage (below the chamfer), its top continues the chamfer slope
        mat(r.part(BODY), hull, true).hexa(false,
            -8.5f, 14f, -4.0f, 8.5f, 14f, -4.0f, 8.5f, 14f, 1.6f, -8.5f, 14f, 1.6f,
            -3.2f, 21f, -2.6f, 3.2f, 21f, -2.6f, 3.2f, 21f, -0.6f, -3.2f, 21f, -0.6f);
        int nose = r.lastIndex();
        r.shadow(0);
        glow(r.part(BODY), amberHi).cbox(0, 21f, -1.95f, 4.4f, 0.16f, 0.5f);
        r.on(nose).detail();
        //two sensor slits on the nose slope (flat strips lying on the sloped top face)
        {
            //top face of the nose runs from (y 14, z 1.6) to (y 21, z -0.6): tilt the strips by the same slope
            float slope = Mathf.atan2(7f, -2.2f) * Mathf.radDeg; //Arc: atan2(x, y) = angle of the vector (x, y)
            for(int sx = -1; sx <= 1; sx += 2){
                glow(r.part(BODY), amber).at(sx * 3.4f, 17f, 1.6f - 2.2f * 3f / 7f).rot(0, slope).cbox(0, 0, 0, 0.3f, 3.2f, 0.14f);
                r.on(nose).detail();
            }
        }

        //---------------------------------------------------------------- wings: inner slab + chamfered outer tip, plate on top, pauldron at the root
        int[] house = new int[3];
        for(int i = 0; i < 2; i++){
            float sx = i == 0 ? -1f : 1f;
            float l0 = lead(8.5f), t0 = trail(8.5f), l1 = lead(15f), t1 = trail(15f), l2 = lead(21f), t2 = trail(21f);
            mat(r.part(BODY), hull, true).hexa(false,
                sx * 8.5f, t0, -3.4f, sx * 8.5f, l0, -3.4f, sx * 8.5f, l0, 1.6f, sx * 8.5f, t0, 1.6f,
                sx * 15f, t1, -3.4f, sx * 15f, l1, -3.4f, sx * 15f, l1, 1.6f, sx * 15f, t1, 1.6f);
            int inner = r.lastIndex();
            r.shadow(0);
            mat(r.part(BODY), hull, true).hexa(false,
                sx * 15f, t1, -3.4f, sx * 15f, l1, -3.4f, sx * 15f, l1, 1.6f, sx * 15f, t1, 1.6f,
                sx * 21f, t2, -1.8f, sx * 21f, l2, -1.8f, sx * 21f, l2, 1.6f, sx * 21f, t2, 1.6f);
            int outer = r.lastIndex();
            r.shadow(0);
            //armour plate on the outer wing (inset 1.2 from the edges, a panel line separates it from the root block)
            float px0 = 16.2f, px1 = 20.2f;
            mat(r.part(BODY), plate, true).hexa(false,
                sx * px0, trail(px0) + 1.2f, 1.6f, sx * px0, lead(px0) - 1.2f, 1.6f, sx * px0, lead(px0) - 1.2f, 2.2f, sx * px0, trail(px0) + 1.2f, 2.2f,
                sx * px1, trail(px1) + 1.2f, 1.6f, sx * px1, lead(px1) - 1.2f, 1.6f, sx * px1, lead(px1) - 1.2f, 2.2f, sx * px1, trail(px1) + 1.2f, 2.2f);
            int wplate = r.lastIndex();
            r.on(outer);
            //amber edge light parallel to the leading edge of the plate
            {
                float ax0 = px0, ay0 = lead(px0) - 1.2f, ax1 = px1, ay1 = lead(px1) - 1.2f;
                float dx = ax1 - ax0, dy = ay1 - ay0, len = Mathf.len(dx, dy);
                dx /= len; dy /= len;
                float nx = dy, ny = -dx; //inward (towards the trailing edge)
                float cx = (ax0 + ax1) / 2f + nx * 0.9f, cy = (ay0 + ay1) / 2f + ny * 0.9f;
                float deg = Mathf.atan2(dx, dy) * Mathf.radDeg;
                glow(r.part(BODY), amber).at(sx * cx, cy, 2.2f).rot(2, sx > 0 ? deg : 180f - deg).cbox(0, 0, 0, 3.4f, 0.3f, 0.14f);
                r.on(wplate).detail();
            }
            //amber lights along the trailing edges (the south faces, always visible in the oblique camera)
            {
                float edge = Mathf.atan2(12.5f, 5f) * Mathf.radDeg, deg = sx > 0 ? edge : 180f - edge;
                glow(r.part(BODY), amber).at(sx * 11.75f, trail(11.75f), 0.1f).rot(2, deg).cbox(0, 0, 0, 5.0f, 0.16f, 0.5f);
                r.on(inner).detail();
                glow(r.part(BODY), amber).at(sx * 18f, trail(18f), 0.1f).rot(2, deg).cbox(0, 0, 0, 4.0f, 0.16f, 0.5f);
                r.on(outer).detail();
            }
            //navigation light on the wing tip face
            glow(r.part(BODY), amberHi).cbox(sx * 21f, -2.5f, -0.4f, 0.16f, 2.0f, 0.6f);
            r.on(outer).detail();
            //root block fills the step between the wing top and the fuselage top, the pauldron stands on it
            mat(r.part(BODY), hull, true).box(sx > 0 ? 8.5f : -15.9f, -5.2f, 1.6f, sx > 0 ? 15.9f : -8.5f, 5.2f, 2.6f);
            r.on(inner);
            //pauldron: octagonal frustum (flat faces on the axes), the shoulder drum stands on its top
            float r0 = 5.6f, r1 = 5.0f, z0 = 2.6f, z1 = 4.65f;
            mat(r.part(BODY), plate, true).at(sx * 10.6f, 0, 0).lathe(8, 22.5f, r0, z0, r1, z1, 0, z1);
            house[i] = r.lastIndex();
            r.shadow(0);
            mat(r.part(BODY), deep, false).at(sx * 10.6f, 0, 0).ring(14, 4.25f, 4.5f, 4.65f, 4.75f);
            r.on(house[i]).detail();
            //amber strip on the rear (south) face of the pauldron, tilted like that face
            {
                float c = Mathf.cosDeg(22.5f), zc = 3.4f;
                float d0 = r0 * c, d1 = r1 * c, dz = z1 - z0, dy = d0 - d1;
                float yc = -(d0 - dy * (zc - z0) / dz), a = Mathf.atan2(dz, dy) * Mathf.radDeg;
                glow(r.part(BODY), amber).at(sx * 10.6f, yc, zc).rot(0, -a).cbox(0, 0, -0.25f, 2.8f, 0.16f, 0.5f);
                r.on(house[i]).detail();
            }
        }

        //panel lines on the fuselage top
        for(int sx = -1; sx <= 1; sx += 2){
            mat(r.part(BODY), deep, false).cbox(sx * 5.6f, 5f, 2.6f, 0.16f, 15f, 0.1f);
            r.on(fus).detail();
        }

        //---------------------------------------------------------------- dorsal ridge with the reactor dome and the gyro ring
        mat(r.part(BODY), plate, true).cbevel(0, 4f, 2.6f, 7f, 14f, 3.2f, 0.9f);
        int ridge = r.lastIndex();
        r.shadow(0);
        glow(r.part(BODY), amber).cbox(0, -0.75f, 5.8f, 1.1f, 2.7f, 0.14f);
        r.on(ridge).detail();
        glow(r.part(BODY), amber).cbox(0, 8.75f, 5.8f, 1.1f, 2.7f, 0.14f);
        r.on(ridge).detail();
        for(int sx = -1; sx <= 1; sx += 2){
            mat(r.part(BODY), deep, false).cbox(sx * 3.5f, 4f, 3.4f, 0.14f, 9f, 0.7f);
            r.on(ridge).detail();
        }
        mat(r.part(BODY), hull, true).at(0, 4f, 0).lathe(12, 15f, 0, 5.8f, 2.6f, 5.8f, 2.6f, 6.5f, 1.9f, 7.5f, 0, 7.5f);
        int dome = r.lastIndex();
        r.on(ridge);
        glow(r.part(BODY), amberHi).at(0, 4f, 0).lathe(12, 15f, 1.3f, 7.52f, 0, 7.52f);
        r.on(dome);
        //gyro ring: floats around the dome and spins (CORE bone)
        mat(r.part(CORE), trim, true).ring(12, 3.9f, 4.7f, 6.3f, 6.8f);
        int gyro = r.lastIndex();
        glow(r.part(CORE), amber).lathe(12, 15f, 4.55f, 6.81f, 4.05f, 6.81f);
        r.on(gyro).detail();
        for(int k = 0; k < 4; k++){
            glow(r.part(CORE), amberHi).at(0, 0, 0).rot(2, k * 90f).cbox(4.3f, 0, 6.8f, 1.0f, 1.0f, 0.3f);
            r.on(gyro).detail();
        }

        //---------------------------------------------------------------- rear pylon (third shoulder) and the thruster block
        mat(r.part(BODY), hull, true).cbevel(0, -9.6f, 2.6f, 12.4f, 10.8f, 2.2f, 0.6f);
        int pbase = r.lastIndex();
        r.shadow(0);
        mat(r.part(BODY), plate, true).taper(0, -9.6f, 4.8f, 10.6f, 9.6f, 15.05f, 9.4f, 8.8f, 0, 0, false);
        house[2] = r.lastIndex();
        r.shadow(0);
        {
            //amber slit on the (slightly leaning) front face of the pylon
            float dz = 15.05f - 4.8f, dyF = 0.4f; //front face moves back by 0.4 over its height
            float a = Mathf.atan2(dz, dyF) * Mathf.radDeg;
            float zc = 10f, yc = -4.8f - dyF * (zc - 4.8f) / dz;
            glow(r.part(BODY), amber).at(0, yc, zc).rot(0, a).cbox(0, 0, -3.0f, 2.4f, 0.16f, 6.0f);
            r.on(house[2]).detail();
        }
        //swept dorsal blade behind the pylon: its front edge follows the leaning rear face of the pylon
        {
            float yA = -14.4f, zA = 4.8f, yB = -19.6f, zB = 6.2f, yC = -16.6f, zC = 14.4f, yD = -14.4f + 0.4f * (14.4f - 4.8f) / 10.25f, zD = 14.4f;
            mat(r.part(BODY), plate, true).hexa(true,
                -0.6f, yA, zA, -0.6f, yB, zB, -0.6f, yC, zC, -0.6f, yD, zD,
                0.6f, yA, zA, 0.6f, yB, zB, 0.6f, yC, zC, 0.6f, yD, zD);
            int blade = r.lastIndex();
            r.on(house[2]).shadow(0);
            //amber trailing-edge light on the rear slope (B -> C)
            float ey = yC - yB, ez = zC - zB, a = Mathf.atan2(ez, ey) * Mathf.radDeg;
            glow(r.part(BODY), amber).at(0, (yB + yC) / 2f, (zB + zC) / 2f).rot(0, -a).cbox(0, 0, -3.0f, 0.8f, 0.16f, 6.0f);
            r.on(blade).detail();
        }
        mat(r.part(BODY), deep, false).at(0, -9.6f, 0).ring(14, 4.25f, 4.35f, 15.05f, 15.15f);
        r.on(house[2]).detail();
        //intake slits flanking the pylon base
        for(int sx = -1; sx <= 1; sx += 2){
            mat(r.part(BODY), deep, false).cbox(sx * 7.0f, -9.6f, 2.6f, 0.7f, 8f, 0.12f);
            r.on(fus).detail();
        }
        //thruster block
        mat(r.part(BODY), hull, true).cbevel(0, -19f, -3.2f, 20f, 6f, 5.2f, 0.8f);
        int rear = r.lastIndex();
        r.shadow(0);
        for(int k = -1; k <= 1; k++){
            float x = k * 6.2f;
            mat(r.part(THR), trim, true).at(x, -22f, -0.8f).rot(0, 90).tubeSide(8, 2.1f, 0, 2.8f);
            int noz = r.lastIndex();
            r.on(rear);
            glow(r.part(THR), amber).at(x, -22f, -0.8f).rot(0, 90).lathe(8, 22.5f, 1.7f, 2.6f, 0, 2.6f);
            r.on(noz);
        }

        //---------------------------------------------------------------- hover pads under the wings (THR: speed glow)
        for(int k = 0; k < 4; k++){
            float px = (k & 1) == 0 ? -12f : 12f, py = (k & 2) == 0 ? -8f : 2.5f;
            mat(r.part(THR), trim, true).at(px, py, 0).tubeSide(8, 2.4f, -7.0f, -3.4f);
            int pad = r.lastIndex();
            r.shadow(0);
            glow(r.part(THR), beamHalo).at(px, py, 0).cyl(8, 1.9f, -7.5f, -7.0f);
            r.under(pad, 1).detail();
        }

        //---------------------------------------------------------------- arms
        for(int i = 0; i < 3; i++) arm(r, i, house[i]);

        r.finish();
        return r;
    }

    private static void arm(Rig r, int i, int house){
        float[] s = SHOULDER[i];
        SH[i] = r.bone(BODY, s[0], s[1], s[2]);
        UPI[i] = r.bone(BODY, s[0], s[1], s[2]);
        UPO[i] = r.bone(BODY, s[0], s[1], s[2]);
        ELB[i] = r.bone(BODY, s[0], s[1], s[2]);
        FOI[i] = r.bone(BODY, s[0], s[1], s[2]);
        FOO[i] = r.bone(BODY, s[0], s[1], s[2]);
        WRI[i] = r.bone(BODY, s[0], s[1], s[2]);
        CL[i] = r.bone(BODY, s[0], s[1], s[2]);
        JL[i] = r.bone(CL[i], -HINGE, 0, -S - 2.0f);
        JR[i] = r.bone(CL[i], HINGE, 0, -S - 2.0f);
        BEAM[i] = r.bone(CL[i], 0, 0, -S - 2.0f);
        HALO[i] = r.bone(CL[i], 0, 0, -S - 2.0f);
        BLADE[i] = r.bone(CL[i], 0, 0, -S - 1.4f);

        //shoulder socket: gunmetal drum with a recessed collar and an amber eye
        mat(r.part(SH[i]), trim, true).cyl(14, 4.2f, -0.35f, 2.4f);
        int sock = r.lastIndex();
        r.on(house);
        mat(r.part(SH[i]), deep, true).cyl(14, 2.7f, 2.4f, 3.1f);
        r.on(sock).detail();
        glow(r.part(SH[i]), amber).cyl(14, 1.9f, 3.1f, 3.26f);
        r.on(sock).detail();

        //upper arm: telescopic. octagonal black sleeve; the steel rod (drawn right before it) slides out when the arm stretches
        float us = U * 0.82f;
        mat(r.part(UPO[i]), hull, true).cyl(8, 2.3f, -us, 0);
        int upo = r.lastIndex();
        r.shadow(1 + i);
        mat(r.part(UPI[i]), rod, true).cyl(8, 1.6f, -U, 0);
        r.under(upo, 1).shadow(1 + i);
        float fx = 2.3f * Mathf.cosDeg(22.5f); //distance of the flat octagon faces
        glow(r.part(UPO[i]), amber).cbox(fx, 0, -us * 0.88f, 0.14f, 1.3f, us * 0.72f);
        r.on(upo);
        glow(r.part(UPO[i]), amber).cbox(-fx, 0, -us * 0.88f, 0.14f, 1.3f, us * 0.72f);
        r.on(upo).detail();
        mat(r.part(UPO[i]), trim, true).cyl(8, 2.6f, -us - 1.0f, -us);
        r.on(upo).detail();

        //elbow: black block with gunmetal axle caps on both sides
        mat(r.part(ELB[i]), plate, true).cbevel(0, 0, -3.2f, 6.6f, 6.6f, 6.4f, 1.1f);
        int elbow = r.lastIndex();
        r.shadow(1 + i);
        for(int sx = -1; sx <= 1; sx += 2){
            mat(r.part(ELB[i]), trim, true).at(0, 0, 0).rot(1, 90).cyl(10, 2.1f, sx > 0 ? 3.3f : -3.8f, sx > 0 ? 3.8f : -3.3f);
            int cap = r.lastIndex();
            r.on(elbow).detail();
            glow(r.part(ELB[i]), amber).at(0, 0, 0).rot(1, 90).cyl(10, 1.0f, sx > 0 ? 3.8f : -3.95f, sx > 0 ? 3.95f : -3.8f);
            r.on(cap).detail();
        }

        //forearm: telescopic as well
        float fs = F * 0.82f;
        mat(r.part(FOO[i]), hull, true).cyl(8, 2.1f, -fs, 0);
        int foo = r.lastIndex();
        r.shadow(1 + i);
        mat(r.part(FOI[i]), rod, true).cyl(8, 1.45f, -F, 0);
        r.under(foo, 1).shadow(1 + i);
        float ff = 2.1f * Mathf.cosDeg(22.5f);
        glow(r.part(FOO[i]), amber).cbox(ff, 0, -fs * 0.88f, 0.14f, 1.2f, fs * 0.72f);
        r.on(foo);
        glow(r.part(FOO[i]), amber).cbox(-ff, 0, -fs * 0.88f, 0.14f, 1.2f, fs * 0.72f);
        r.on(foo).detail();
        mat(r.part(FOO[i]), trim, true).cyl(8, 2.4f, -fs - 1.0f, -fs);
        r.on(foo).detail();

        //wrist: black cube with axle caps
        mat(r.part(WRI[i]), plate, true).cbevel(0, 0, -2.9f, 5.8f, 5.8f, 5.8f, 1.0f);
        int wr = r.lastIndex();
        r.shadow(1 + i);
        for(int sx = -1; sx <= 1; sx += 2){
            mat(r.part(WRI[i]), trim, true).at(0, 0, 0).rot(1, 90).cyl(10, 2.0f, sx > 0 ? 2.9f : -3.3f, sx > 0 ? 3.3f : -2.9f);
            int cap = r.lastIndex();
            r.on(wr).detail();
            glow(r.part(WRI[i]), amber).at(0, 0, 0).rot(1, 90).cyl(10, 1.1f, sx > 0 ? 3.3f : -3.45f, sx > 0 ? 3.45f : -3.3f);
            r.on(cap).detail();
        }

        //pillar and the tapered bracket that carries the jaws
        mat(r.part(CL[i]), hull, true).taper(0, 0, -S + 0.6f, 3.6f, 3.6f, -2.9f, 4.4f, 4.4f, 0, 0, false);
        int stem = r.lastIndex();
        r.shadow(1 + i);
        mat(r.part(CL[i]), trim, true).cbox(0, 0, -S * 0.55f, 4.8f, 4.8f, 1.0f);
        r.on(stem).detail();
        mat(r.part(CL[i]), plate, true).taper(0, 0, -S - 2.2f, 13.6f, 4.2f, -S + 0.6f, 11.0f, 4.2f, 0, 0, true);
        int brk = r.lastIndex();
        r.shadow(1 + i);
        glow(r.part(CL[i]), amberHi).cbox(0, 2.1f, -S - 1.5f, 2.2f, 0.14f, 1.0f);
        r.on(brk).detail();
        glow(r.part(CL[i]), amberHi).cbox(0, -2.1f, -S - 1.5f, 2.2f, 0.14f, 1.0f);
        r.on(brk).detail();

        //jaws: two tapered magnet fingers with a vertical inner face that carries the amber pole strip, sharp tooth at the tip
        for(int k = 0; k < 2; k++){
            int bone = k == 0 ? JL[i] : JR[i];
            float sx = k == 0 ? 1f : -1f; //direction of the inner face
            Color body = k == 0 ? jawA : jawB;
            mat(r.part(bone), body, true).taper(sx * 0.6f, 0, -J, 2.8f, 3.6f, -0.3f, 4.0f, 4.4f, -sx * 0.6f, 0, false);
            int jaw = r.lastIndex();
            r.shadow(1 + i);
            mat(r.part(bone), deep, true).taper(sx * 0.9f, 0, -J - 1.6f, 1.4f, 2.2f, -J, 2.8f, 3.6f, -sx * 0.3f, 0, true);
            r.on(jaw);
            glow(r.part(bone), amberHi).cbox(sx * 2.0f, 0, -J + 2.4f, 0.14f, 2.4f, J - 4.0f);
            r.on(jaw);
            mat(r.part(bone), trim, true).at(0, 0, 0).rot(0, 90).cyl(8, 1.2f, -2.7f, 2.7f).at(0, 0, 0);
            r.on(jaw).detail();
        }

        //blade: a white-hot spike that slides out between the jaws for stabs and slashes (hidden while sheathed)
        mat(r.part(BLADE[i]), bladeC, true).taper(0, 0, -BL, 0.7f, 0.5f, 0, 2.4f, 1.0f, 0, 0, true);
        int bl = r.lastIndex();
        r.shadow(1 + i);
        glow(r.part(BLADE[i]), amber).taper(0, 0, -BL * 0.9f, 0.3f, 0.6f, -BL * 0.1f, 0.9f, 1.12f, 0, 0, false);
        r.on(bl);

        //the white-hot beam between the jaws (only while energy is being absorbed)
        //gold glow sleeve with a taller white core on top (the core must stick out of it or the opaque halo would hide it)
        glow(r.part(HALO[i]), beamHalo).cbox(0, 0, -J + 0.3f, 3.6f, 3.0f, J - 1.4f);
        int halo = r.lastIndex();
        glow(r.part(BEAM[i]), beamCore).cbox(0, 0, -J + 0.3f, 1.5f, 1.5f, J - 0.5f);
        r.on(halo);
    }
}
