package blackhole.models;

import arc.graphics.*;
import blackhole.g3d.*;
import blackhole.g3d.Kit.*;
import blackhole.g3d.Mesh;

import static blackhole.g3d.Kit.*;

/**
 * v7.7 unit rigs: the bastion pilot (the tier-2 core unit) and the warden (its escort).
 *
 * <p>Same conventions as {@link UnitModels} and {@link FrontierUnits}: design units = world units, +y forward,
 * z up, one convex solid per piece, moving parts on their own bones. The pilot has to be readable next to the
 * lumen pilot it replaces, so it keeps the same wing silhouette and adds the one thing the tier-2 core gives
 * you - a pair of tilting lift pods and a heavier build arm.
 */
public final class BastionUnits{
    private BastionUnits(){}

    static final Color canopy = Color.valueOf("5a86c8");

    public static int PILOT_PODL, PILOT_PODR, PILOT_ARM;
    public static int WARDEN_DISH, WARDEN_RINGL, WARDEN_RINGR;

    /** Bastion pilot: wide delta shuttle, two tilting lift pods, a build / mining arm under the nose. */
    public static Rig bastionPilot(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        PILOT_PODL = r.bone(body, -4.6f, -0.8f, 0.2f);
        PILOT_PODR = r.bone(body, 4.6f, -0.8f, 0.2f);
        PILOT_ARM = r.bone(body, 0, 6.2f, -0.6f);

        //fuselage
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -2.4f, -5.8f, -0.9f, 2.4f, -5.8f, -0.9f, 0.7f, 8.0f, -0.5f, -0.7f, 8.0f, -0.5f,
            -1.7f, -5.2f, 1.5f, 1.7f, -5.2f, 1.5f, 0.35f, 7.0f, 0.5f, -0.35f, 7.0f, 0.5f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        glass(m, canopy).taper(0, 1.6f, 1.05f, 2.0f, 3.6f, 1.95f, 0.8f, 1.8f, 0, -0.5f, false);
        r.on(hullI);
        m = r.part(body);
        trim(m, s).box(-2.0f, -2.2f, 1.1f, 2.0f, -0.6f, 1.45f);
        r.on(hullI).detail();
        m = r.part(body);
        team(m).box(-0.9f, -5.1f, 1.46f, 0.9f, -3.3f, 1.52f);
        r.on(hullI).detail();
        //spine glow strip: tells the two core units apart at a glance
        m = r.part(body);
        glow(m, s).box(-0.35f, -5.0f, 1.5f, 0.35f, 4.4f, 1.6f);
        r.on(hullI).detail();

        for(int sd = -1; sd <= 1; sd += 2){
            //fixed wing root
            m = r.part(body);
            hull2(m, s).hexa(true, 0, 2.6f, -0.2f, 0, -3.4f, -0.2f, sd * 4.6f, -4.4f, -0.15f, sd * 4.6f, -2.6f, -0.15f,
                0, 2.2f, 0.35f, 0, -3.2f, 0.35f, sd * 4.6f, -4.2f, 0.1f, sd * 4.6f, -2.8f, 0.1f);
            r.shadow(1 + (sd < 0 ? 0 : 1));
            int w = r.lastIndex();
            m = r.part(body);
            steel(m, s).box(sd * 4.3f - 0.3f, -3.8f, -0.25f, sd * 4.3f + 0.3f, -2.4f, 0.45f);
            r.on(w).detail();
            //tilting lift pod on its own bone
            int pod = sd < 0 ? PILOT_PODL : PILOT_PODR;
            m = r.part(pod);
            hull(m, s).at(0, 0, 0).rot(0, -90).lathe(8, 22.5f, 0.001f, -1.9f, 1.2f, -1.1f, 1.35f, 0.9f, 0.001f, 2.1f);
            r.shadowRound(1 + (sd < 0 ? 0 : 1));
            int podI = r.lastIndex();
            m = r.part(pod);
            glow(m, s).at(0, 0, 0).rot(0, -90).ring(8, 0.8f, 1.25f, -2.0f, -1.85f);
            r.on(podI).detail();
            t.engine(pod, 0, -2.2f, 0f, 0.9f);
        }

        //build / mining arm
        m = r.part(PILOT_ARM);
        steel(m, s).at(0, 0, 0).rot(0, -90).lathe(6, 30f, 0.45f, -0.6f, 0.5f, 0.9f, 0.3f, 1.5f, 0.001f, 1.6f);
        int arm = r.lastIndex();
        m = r.part(PILOT_ARM);
        glow2(m, s).at(0, 1.7f, 0).rot(0, -90).lathe(6, 30f, 0.38f, 0f, 0.001f, 0.55f);
        r.on(arm).detail();
        t.weapon(0, -1, PILOT_ARM, 0, 1.8f, 0);

        r.half = 9.2f;
        r.height = 3.0f;
        t.extra = (type, unit, d, delta) -> {
            //pods tilt forward with speed and counter-roll in a turn: the shuttle leans on them
            float tilt = -22f * d.speedF;
            type.rig.rot(PILOT_PODL, 0, tilt - d.turn * 8f);
            type.rig.rot(PILOT_PODR, 0, tilt + d.turn * 8f);
        };
        return r.finish();
    }

    /** Warden: the escort the tier-2 core unlocks - a blunt hull with a turning scan dish and two lift rings. */
    public static Rig bastionWarden(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int tur = r.bone(body, 0, 3.4f, 2.2f), bar = r.bone(tur, 0, 1.2f, 0.2f);
        WARDEN_DISH = r.bone(body, 0, -3.2f, 2.6f);
        WARDEN_RINGL = r.bone(body, -6.2f, -0.4f, 0.4f);
        WARDEN_RINGR = r.bone(body, 6.2f, -0.4f, 0.4f);

        //hull: short, wide, flat-topped
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -3.4f, -7.2f, -1.2f, 3.4f, -7.2f, -1.2f, 1.6f, 8.6f, -0.8f, -1.6f, 8.6f, -0.8f,
            -2.8f, -6.4f, 1.9f, 2.8f, -6.4f, 1.9f, 1.1f, 7.6f, 1.5f, -1.1f, 7.6f, 1.5f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        hull2(m, s).taper(0, -1.2f, 1.9f, 5.0f, 7.4f, 2.6f, 3.8f, 5.6f, 0, 0, false);
        r.on(hullI);
        int deckI = r.lastIndex();
        m = r.part(body);
        glass(m, canopy).taper(0, 5.4f, 1.5f, 2.2f, 2.4f, 2.3f, 1.2f, 1.2f, 0, -0.3f, false);
        r.on(hullI);
        m = r.part(body);
        team(m).box(-1.3f, -6.0f, 1.91f, 1.3f, -4.4f, 1.97f);
        r.on(hullI).detail();
        m = r.part(body);
        glow(m, s).box(-2.4f, 7.3f, -0.4f, 2.4f, 7.45f, 0.4f);
        r.on(hullI).detail();

        //forward turret
        m = r.part(tur);
        hull2(m, s).at(0, 0, 0).lathe(8, 22.5f, 1.9f, 0f, 1.8f, 0.8f, 1.1f, 1.4f, 0.001f, 1.5f);
        int dome = r.lastIndex();
        m = r.part(tur);
        trim(m, s).at(0, 0, 0).ring(8, 1.82f, 1.98f, 0.3f, 0.5f);
        r.on(dome).detail();
        m = r.part(bar);
        steel(m, s).at(0, 0, 0).rot(0, -90).lathe(8, 22.5f, 0.5f, 0f, 0.5f, 2.4f, 0.65f, 2.4f, 0.65f, 3.1f);
        m = r.part(bar);
        glow(m, s).at(0, 3.15f, 0).rot(0, -90).ring(8, 0.3f, 0.6f, 0f, 0.1f);
        t.weapon(0, tur, bar, 0, 3.3f, 0);

        //scan dish (turns constantly)
        m = r.part(WARDEN_DISH);
        steel(m, s).at(0, 0, -0.5f).cyl(8, 0.4f, 0f, 0.7f);
        int mast = r.lastIndex();
        m = r.part(WARDEN_DISH);
        trim(m, s).at(0, 0, 0.3f).rot(0, -28f).lathe(10, 18f, 0.001f, -0.18f, 1.5f, -0.1f, 1.5f, 0.1f, 0.001f, 0.18f);
        r.on(mast).detail();
        m = r.part(WARDEN_DISH);
        glow2(m, s).at(0, 0.25f, 0.45f).lathe(6, 30f, 0.3f, 0f, 0.001f, 0.4f);
        r.on(mast).detail();

        //two lift rings on stubby pylons
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(body);
            steel(m, s).box(sd * 3.2f - 0.4f, -1.4f, 0.1f, sd * 5.6f + 0.4f, 0.6f, 0.7f);
            r.shadow(1 + (sd < 0 ? 0 : 1));
            int ring = sd < 0 ? WARDEN_RINGL : WARDEN_RINGR;
            m = r.part(ring);
            trim(m, s).at(0, 0, 0).ring(10, 1.7f, 2.0f, -0.14f, 0.14f);
            int ri = r.lastIndex();
            m = r.part(ring);
            glow(m, s).at(0, 0, 0).ring(10, 1.74f, 1.96f, 0.14f, 0.2f);
            r.on(ri).detail();
            for(int i = 0; i < 3; i++){
                m = r.part(ring);
                steel(m, s).at(0, 0, 0).box(-0.14f, 0f, -0.08f, 0.14f, 1.8f, 0.08f).rot(2, 60f + i * 120f);
                r.on(ri).detail();
            }
            t.engine(body, sd * 2.2f, -7.0f, 0.2f, 0.8f);
        }

        r.half = 9.6f;
        r.height = 4.4f;
        t.extra = (type, unit, d, delta) -> {
            type.rig.rot(WARDEN_DISH, 2, arc.util.Time.time * 1.3f);
            type.rig.rot(WARDEN_RINGL, 2, d.spin * 9f);
            type.rig.rot(WARDEN_RINGR, 2, -d.spin * 9f);
        };
        return r.finish();
    }
}
