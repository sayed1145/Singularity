package voxel.gfx;

import static voxel.gfx.Palette.*;

/**
 * Colossus (titan): a two-legged assault mech built from clean convex armour panels.
 *
 * <p>Design units, +y forward, z up, origin on the ground. The lower body (base, pelvis, legs) follows the
 * walking direction; the whole upper body (torso, head, shoulder cannons, arm guns, backpack) sits on a
 * slewing ring and turns freely through 360 degrees. Legs are solved with two-bone IK every frame, feet are
 * planted by a distance-driven gait, hydraulic pistons are re-aimed between thigh and shin.
 */
public final class TitanModel{
    public static int BASE, PELVIS, TORSO, HEAD, PACK;
    public static final int[] POD = new int[2], BARREL = new int[2], ARM = new int[2], GUN = new int[2], GUNB = new int[2],
        RACK = new int[2], LID = new int[2], HIP = new int[2], THIGH = new int[2], SHIN = new int[2], FOOT = new int[2], PISTON = new int[2];
    public static final int[] THR = new int[4];

    public static final float hipX = 5.5f, hipZ = 15f, hipDrop = 0.6f, thighLen = 8.5f, shinLen = 8.5f, ankleZ = 2.2f;
    /** muzzle positions in their bone frames (y forward) */
    public static final float barrelMuzzle = 7.2f, gunMuzzle = 4.4f, rackExit = 2.4f;
    /** piston anchors: on the thigh (thigh frame) and on the shin (shin frame) */
    public static final float pistonThighY = -1.9f, pistonThighZ = -2.2f, pistonShinY = -1.5f, pistonShinZ = -3.8f;

    private TitanModel(){}

    private static final float[] hip = new float[3], knee = new float[3], ank = new float[3], p1 = new float[3], p2 = new float[3];

    /**
     * Poses both legs. The pelvis local matrix must already be final. u = gait phase (0..1, one full cycle),
     * amp = step amplitude (0 = standing), elev = flight (0..1, tucks the legs). Feet: linear stance from +stride
     * to -stride (so a planted foot moves exactly opposite to the body), smoothstep swing with a sine lift.
     */
    public static void legs(Rig rig, float u, float amp, float elev, float stride, float lift){
        for(int i = 0; i < 2; i++){
            float side = i == 0 ? -1f : 1f;
            float ui = u + (i == 0 ? 0f : 0.5f);
            ui -= (float)Math.floor(ui);
            float fyT, lz, pitch;
            if(ui < 0.5f){
                float t = ui * 2f;
                fyT = stride * (1f - 2f * t);
                lz = 0f;
                pitch = 0f;
            }else{
                float t = (ui - 0.5f) * 2f;
                float e = t * t * (3f - 2f * t);
                fyT = -stride + 2f * stride * e;
                lz = (float)Math.sin(t * Math.PI) * lift;
                pitch = -(float)Math.sin(t * Math.PI * 2) * 14f;
            }
            float tx = side * (hipX + 0.5f), ty = fyT * amp, tz = ankleZ + lz * amp;
            pitch *= amp;
            ty = ty + (-1.8f - ty) * elev;
            tz = tz + (ankleZ + 5.5f - tz) * elev;
            tx = tx + (side * (hipX + 1.2f) - tx) * elev;
            pitch = pitch + (28f - pitch) * elev;

            rig.apply(PELVIS, side * hipX, 0, -hipDrop, hip);
            Rig.knee(hip[0], hip[1], hip[2], tx, ty, tz, thighLen, shinLen, side * 0.12f, 1f, 0f, knee);
            float kx = tx - knee[0], ky = ty - knee[1], kz = tz - knee[2];
            float kl = Math.max((float)Math.sqrt(kx * kx + ky * ky + kz * kz), 1e-4f);
            ank[0] = knee[0] + kx / kl * shinLen;
            ank[1] = knee[1] + ky / kl * shinLen;
            ank[2] = knee[2] + kz / kl * shinLen;
            rig.aim(THIGH[i], hip[0], hip[1], hip[2], knee[0], knee[1], knee[2], 1f, 0f, 0f);
            rig.aim(SHIN[i], knee[0], knee[1], knee[2], ank[0], ank[1], ank[2], 1f, 0f, 0f);
            rig.place(FOOT[i], ank[0], ank[1], ank[2]);
            rig.rot(FOOT[i], 0, pitch);

            rig.apply(THIGH[i], 0, pistonThighY, pistonThighZ, p1);
            rig.apply(SHIN[i], 0, pistonShinY, pistonShinZ, p2);
            float len = rig.aim(PISTON[i], p1[0], p1[1], p1[2], p2[0], p2[1], p2[2], 1f, 0f, 0f);
            rig.scale(PISTON[i], 1f, 1f, len);
        }
    }

    /** Standing rest pose (icons, previews, the assembler print). */
    public static void rest(Rig rig, float u, float amp){
        rig.reset();
        for(int i = 0; i < 2; i++){
            rig.rot(ARM[i], 0, -4f);
            rig.rot(GUN[i], 0, 4f);
            rig.rot(POD[i], 0, 2f);
        }
        legs(rig, u, amp, 0f, 4.6f, 2.6f);
    }

    public static Rig build(){
        Rig r = new Rig();
        BASE = r.bone(-1, 0, 0, 0);
        PELVIS = r.bone(BASE, 0, 0, hipZ);
        TORSO = r.bone(PELVIS, 0, 0, 1.2f);
        HEAD = r.bone(TORSO, 0, 1.6f, 9.0f);
        PACK = r.bone(TORSO, 0, -4.2f, 1.8f);
        for(int i = 0; i < 2; i++){
            float s = i == 0 ? -1f : 1f;
            HIP[i] = r.bone(PELVIS, s * hipX, 0, -hipDrop);
            POD[i] = r.bone(TORSO, s * 9.4f, -0.2f, 7.2f);
            BARREL[i] = r.bone(POD[i], 0, 4.0f, 0.2f);
            ARM[i] = r.bone(TORSO, s * 7.6f, 0.8f, 4.0f);
            GUN[i] = r.bone(ARM[i], s * 0.6f, 0.6f, -4.6f);
            GUNB[i] = r.bone(GUN[i], 0, 5.0f, 0);
            RACK[i] = r.bone(PACK, s * 2.8f, -1.6f, 6.6f);
            LID[i] = r.bone(RACK[i], 0, 2.2f, 2.2f);
            THIGH[i] = r.bone(BASE, s * hipX, 0, hipZ - hipDrop);
            SHIN[i] = r.bone(BASE, s * hipX, 0, hipZ - hipDrop - thighLen);
            FOOT[i] = r.bone(BASE, s * hipX, 0, ankleZ);
            PISTON[i] = r.bone(BASE, s * hipX, 0, hipZ - 3f);
        }
        for(int i = 0; i < 4; i++){
            THR[i] = r.bone(PACK, (i % 2 == 0 ? -1f : 1f) * (i < 2 ? 3.3f : 2.0f), -3.4f, i < 2 ? 1.5f : 4.4f);
        }

        // ------------------------------------------------ pelvis
        Mesh m = r.part(PELVIS); grey(m);
        m.taper(0, 0, -2.4f, 6.2f, 4.4f, 0.6f, 8.6f, 5.4f, 0, 0, false);
        r.shadow(0);
        int pelvis = r.lastIndex();
        m = r.part(PELVIS); armorShade(m);
        m.taper(0, 2.9f, -2.8f, 3.2f, 0.7f, 0.2f, 4.2f, 1.1f, 0, 0.2f, false);
        r.on(pelvis).detail();
        m = r.part(PELVIS); dark(m);
        m.taper(0, -3.0f, -2.2f, 3.6f, 0.8f, 0.4f, 4.4f, 1.0f, 0, -0.1f, false);
        r.on(pelvis).detail();

        for(int i = 0; i < 2; i++){
            float s = i == 0 ? -1f : 1f;
            //hip actuator drum + outer armour cap
            m = r.part(HIP[i]); dark(m);
            m.at(0, 0, 0).rot(1, 90).cyl(10, 1.9f, -1.5f, 1.5f);
            int drum = r.lastIndex();
            m = r.part(HIP[i]); armor(m);
            m.taper(s * 1.95f, 0, -1.9f, 0.9f, 4.2f, 1.6f, 0.9f, 3.4f, 0, 0, false);
            r.on(drum);
            m = r.part(HIP[i]); team(m);
            m.cbox(s * 2.45f, 0, -0.6f, 0.12f, 2.2f, 1.2f);
            r.on(r.lastIndex() - 1).detail();
        }

        // ------------------------------------------------ torso (slewing upper body)
        m = r.part(TORSO); dark(m);
        m.cyl(12, 3.1f, -1.5f, 0.3f);
        m = r.part(TORSO); steel(m);
        m.taper(0, 0, 0, 9.4f, 6.4f, 3.4f, 12.6f, 8.4f, 0, 0.3f, false);
        r.shadow(0);
        m = r.part(TORSO); armor(m);
        m.taper(0, 0.3f, 3.4f, 13.0f, 8.6f, 8.2f, 11.4f, 7.4f, 0, -0.2f, false);
        r.shadow(0);
        int chest = r.lastIndex();
        //sloped front glacis + team stripe
        m = r.part(TORSO); armor(m);
        m.taper(0, 4.7f, 2.6f, 8.4f, 1.2f, 7.4f, 9.6f, 1.0f, 0, -0.55f, false);
        r.on(chest);
        int glacis = r.lastIndex();
        m = r.part(TORSO); team(m);
        m.hexa(false,
            -0.8f, 5.25f, 3.6f, 0.8f, 5.25f, 3.6f, 0.8f, 5.05f, 3.6f, -0.8f, 5.05f, 3.6f,
            -0.8f, 4.75f, 7.0f, 0.8f, 4.75f, 7.0f, 0.8f, 4.55f, 7.0f, -0.8f, 4.55f, 7.0f);
        r.on(glacis);
        //side intakes with a soft cyan core
        for(int i = 0; i < 2; i++){
            float s = i == 0 ? -1f : 1f;
            m = r.part(TORSO); dark(m);
            m.cbox(s * 6.2f, 0.6f, 4.6f, 0.5f, 3.6f, 2.0f);
            r.on(chest).detail();
            m = r.part(TORSO); glow(m);
            m.cbox(s * 6.45f, 0.6f, 5.2f, 0.12f, 2.4f, 0.5f);
            r.on(chest).detail();
        }
        //collar and top vents
        m = r.part(TORSO); dark(m);
        m.taper(0, -0.2f, 8.2f, 8.0f, 5.6f, 9.1f, 7.0f, 5.0f, 0, 0, false);
        for(int i = 0; i < 2; i++){
            float s = i == 0 ? -1f : 1f;
            m = r.part(TORSO); black(m);
            m.cbox(s * 4.3f, -2.3f, 8.1f, 2.4f, 2.0f, 0.3f);
            r.on(chest).detail();
        }

        // ------------------------------------------------ head
        m = r.part(HEAD); armor(m);
        m.taper(0, 0, 0, 4.6f, 4.8f, 2.4f, 3.6f, 3.9f, 0, -0.3f, false);
        r.shadow(0);
        int head = r.lastIndex();
        m = r.part(HEAD); glow(m);
        m.hexa(false,
            -1.9f, 2.42f, 0.7f, 1.9f, 2.42f, 0.7f, 1.9f, 2.3f, 0.7f, -1.9f, 2.3f, 0.7f,
            -1.7f, 2.2f, 1.6f, 1.7f, 2.2f, 1.6f, 1.7f, 2.08f, 1.6f, -1.7f, 2.08f, 1.6f);
        r.on(head);
        m = r.part(HEAD); team(m);
        m.cbox(0, -0.3f, 2.4f, 0.5f, 3.0f, 0.22f);
        r.on(head).detail();
        m = r.part(HEAD); grey(m);
        m.taper(1.4f, -1.4f, 2.2f, 0.28f, 1.1f, 4.4f, 0.16f, 0.35f, 0, -0.7f, false);
        r.detail();

        // ------------------------------------------------ shoulder cannons
        for(int i = 0; i < 2; i++){
            float s = i == 0 ? -1f : 1f;
            m = r.part(TORSO); dark(m);
            m.at(s * 6.2f, -0.2f, 7.2f).rot(1, 90).cyl(10, 1.5f, s < 0 ? -1.6f : 0f, s < 0 ? 0f : 1.6f);

            m = r.part(POD[i]); armor(m);
            m.bevel(-2.3f, -3.8f, -1.8f, 2.3f, 3.4f, 2.2f, 0.5f);
            r.shadow(0);
            int pod = r.lastIndex();
            m = r.part(POD[i]); team(m);
            m.box(s > 0 ? 2.3f : -2.42f, -2.6f, -0.7f, s > 0 ? 2.42f : -2.3f, 1.6f, 0.9f);
            r.on(pod);
            m = r.part(POD[i]); dark(m);
            m.cbox(0, -1.7f, 2.2f, 2.8f, 2.4f, 0.3f);
            r.on(pod).detail();
            m = r.part(POD[i]); grey(m);
            m.box(-1.9f, 3.4f, -1.3f, 1.9f, 4.1f, 1.7f);

            m = r.part(BARREL[i]); steel(m);
            m.at(-0.8f, 0, 0).rot(0, -90).cyl(8, 0.6f, 0, 6.6f);
            m = r.part(BARREL[i]); steel(m);
            m.at(0.8f, 0, 0).rot(0, -90).cyl(8, 0.6f, 0, 6.6f);
            m = r.part(BARREL[i]); dark(m);
            m.cbox(0, 6.4f, -0.85f, 3.3f, 1.2f, 1.7f);
            m = r.part(BARREL[i]); glow(m);
            m.cbox(0, 1.5f, -0.75f, 2.9f, 0.5f, 1.5f);
            r.detail();
        }

        // ------------------------------------------------ arm autocannons
        for(int i = 0; i < 2; i++){
            float s = i == 0 ? -1f : 1f;
            m = r.part(ARM[i]); dark(m);
            m.at(0, 0, 0).rot(1, 90).cyl(8, 1.4f, -1.0f, 1.0f);
            m = r.part(ARM[i]); grey(m);
            m.taper(s * 0.6f, 0, -4.4f, 2.0f, 2.4f, 0.2f, 2.6f, 2.8f, 0, 0, false);
            m = r.part(GUN[i]); dark(m);
            m.at(0, 0, 0).rot(1, 90).cyl(8, 1.15f, -1.1f, 1.1f);
            m = r.part(GUN[i]); armor(m);
            m.bevel(-1.5f, -2.3f, -1.4f, 1.5f, 5.0f, 1.3f, 0.4f);
            r.shadow(0);
            int gun = r.lastIndex();
            m = r.part(GUN[i]); team(m);
            m.box(s > 0 ? 1.5f : -1.62f, -0.8f, -0.4f, s > 0 ? 1.62f : -1.5f, 3.6f, 0.4f);
            r.on(gun).detail();
            m = r.part(GUNB[i]); dark(m);
            m.at(0, 0, -0.05f).rot(0, -90).cyl(8, 0.85f, 0, 1.5f);
            m = r.part(GUNB[i]); steel(m);
            m.at(0, 0, -0.05f).rot(0, -90).cyl(8, 0.5f, 1.5f, 4.4f);
        }

        // ------------------------------------------------ backpack, reactor, thrusters, missile racks
        m = r.part(PACK); grey(m);
        m.bevel(-5.2f, -3.4f, 0, 5.2f, 0.4f, 6.6f, 0.6f);
        r.shadow(0);
        int pack = r.lastIndex();
        m = r.part(PACK); glow(m);
        m.box(-1.5f, -3.56f, 1.4f, 1.5f, -3.4f, 5.4f);
        r.on(pack);
        for(int i = 0; i < 2; i++){
            float s = i == 0 ? -1f : 1f;
            m = r.part(PACK); black(m);
            m.box(s * 4.9f - 0.3f, -2.8f, 0.8f, s * 4.9f + 0.3f, -0.4f, 5.6f);
            r.on(pack).detail();
        }
        for(int i = 0; i < 4; i++){
            boolean big = i < 2;
            m = r.part(THR[i]); dark(m);
            m.at(0, 0, 0).rot(0, 90).lathe(10, 0, 0, 0, big ? 1.0f : 0.7f, 0, big ? 1.45f : 1.0f, big ? 2.2f : 1.6f, 0, big ? 2.2f : 1.6f);
            int noz = r.lastIndex();
            m = r.part(THR[i]); glowWhite(m);
            m.at(0, 0, 0).rot(0, 90).cyl(10, big ? 1.1f : 0.75f, big ? 2.2f : 1.6f, big ? 2.3f : 1.7f);
            r.on(noz);
        }
        for(int i = 0; i < 2; i++){
            m = r.part(RACK[i]); armor(m);
            m.bevel(-2.0f, -1.8f, 0, 2.0f, 2.2f, 2.2f, 0.3f);
            r.shadow(0);
            int rack = r.lastIndex();
            m = r.part(RACK[i]); black(m);
            m.box(-1.7f, 2.2f, 0.25f, 1.7f, 2.3f, 1.95f);
            r.on(rack);
            m = r.part(RACK[i]); amber(m);
            m.box(-1.2f, 2.3f, 0.6f, -0.5f, 2.36f, 0.9f);
            m.box(0.5f, 2.3f, 0.6f, 1.2f, 2.36f, 0.9f);
            m.box(-1.2f, 2.3f, 1.3f, -0.5f, 2.36f, 1.6f);
            m.box(0.5f, 2.3f, 1.3f, 1.2f, 2.36f, 1.6f);
            r.on(rack).detail();
            m = r.part(LID[i]); armorShade(m);
            m.box(-1.95f, 0, -2.15f, 1.95f, 0.4f, 0.05f, true);
            int lid = r.lastIndex();
            m = r.part(LID[i]); team(m);
            m.box(-0.4f, 0.4f, -1.9f, 0.4f, 0.48f, -0.2f);
            r.on(lid).detail();
        }

        // ------------------------------------------------ legs
        for(int i = 0; i < 2; i++){
            float s = i == 0 ? -1f : 1f;
            int g = 1 + i;
            //thigh
            m = r.part(THIGH[i]); armor(m);
            m.taper(0, 0.2f, -7.4f, 2.8f, 3.2f, 0.8f, 3.9f, 4.6f, 0, 0.35f, true);
            r.shadow(g);
            int thigh = r.lastIndex();
            m = r.part(THIGH[i]); dark(m);
            m.box(-0.9f, pistonThighY - 0.8f, -6.5f, 0.9f, pistonThighY + 0.8f, -1.2f, true);
            r.detail();
            m = r.part(THIGH[i]); team(m);
            m.box(s > 0 ? 1.62f : -1.78f, -0.6f, -5.2f, s > 0 ? 1.78f : -1.62f, 1.0f, -1.6f, true);
            r.on(thigh).detail();
            //shin
            m = r.part(SHIN[i]); dark(m);
            m.at(0, 0, 0).rot(1, 90).cyl(10, 1.6f, -1.5f, 1.5f);
            m = r.part(SHIN[i]); steel(m);
            m.taper(0, -0.2f, -8.0f, 2.3f, 2.4f, -0.6f, 3.0f, 3.0f, 0, 0, true);
            r.shadow(g);
            m = r.part(SHIN[i]); armor(m);
            m.taper(0, 1.8f, -7.6f, 2.8f, 1.0f, -1.2f, 3.4f, 1.4f, 0, 0.3f, true);
            r.shadow(g);
            int shin = r.lastIndex();
            m = r.part(SHIN[i]); team(m);
            m.taper(0, 1.9f, -1.4f, 2.6f, 1.6f, 1.4f, 2.2f, 1.3f, 0, 0.1f, true);
            m = r.part(SHIN[i]); black(m);
            m.box(-0.8f, 2.35f, -6.6f, 0.8f, 2.55f, -5.4f, true);
            r.on(shin).detail();
            //foot
            m = r.part(FOOT[i]); dark(m);
            m.at(0, 0, 0).rot(1, 90).cyl(8, 1.2f, -1.25f, 1.25f);
            m = r.part(FOOT[i]); grey(m);
            m.taper(0, 0.4f, -ankleZ, 3.9f, 7.2f, -0.5f, 3.0f, 4.6f, 0, -0.2f, true);
            r.shadow(g);
            int foot = r.lastIndex();
            m = r.part(FOOT[i]); armor(m);
            m.taper(0, 3.6f, -ankleZ + 0.05f, 3.7f, 2.2f, -1.0f, 3.0f, 1.3f, 0, -0.35f, false);
            r.on(foot);
            m = r.part(FOOT[i]); dark(m);
            m.taper(0, -3.1f, -ankleZ, 2.4f, 1.4f, -1.1f, 1.6f, 0.9f, 0, 0.3f, false);
            //hydraulic rod (unit length along -z, scaled per frame)
            m = r.part(PISTON[i]); steel(m);
            m.cyl(6, 0.42f, -1f, 0f);
            r.detail();
        }

        r.half = 17.5f;
        r.height = 27f;
        return r.finish();
    }
}
