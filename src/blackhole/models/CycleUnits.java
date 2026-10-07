package blackhole.models;

import arc.graphics.*;
import blackhole.g3d.*;
import blackhole.g3d.Kit.*;
import blackhole.g3d.Mesh;

import static blackhole.g3d.Kit.*;

/**
 * v8.3 unit rig: the citadel pilot, the tier-3 core chassis.
 *
 * <p>It has to read as the third member of the pilot family and still be its own machine. The lumen pilot is
 * a dart and the bastion pilot is a delta with lift pods; this one is a short, wide "lifter" - a blunt body
 * carried by two ducted fans on fixed outriggers, with a cutter head under the nose for the wall seams it is
 * the only core unit allowed to mine. Moving parts: the two fan rotors (turning in their own ducts, flat in
 * the duct plane) and the cutter drum (turning about its own transverse axis). Nothing detaches, nothing
 * crosses another part.
 */
public final class CycleUnits{
    private CycleUnits(){}

    static final Color canopy = Color.valueOf("5a86c8");

    public static int FANL, FANR, CUTTER;

    /** Citadel pilot: twin ducted fans on fixed outriggers, a cutter drum under the nose. */
    public static Rig citadelPilot(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        FANL = r.bone(body, -5.0f, -0.4f, 0.55f);
        FANR = r.bone(body, 5.0f, -0.4f, 0.55f);
        CUTTER = r.bone(body, 0, 5.4f, -0.95f);

        //---- fuselage: short and square-shouldered, so it does not look like the delta
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -2.8f, -4.6f, -1.0f, 2.8f, -4.6f, -1.0f, 1.5f, 6.4f, -0.7f, -1.5f, 6.4f, -0.7f,
            -2.2f, -4.0f, 1.4f, 2.2f, -4.0f, 1.4f, 0.9f, 5.6f, 0.9f, -0.9f, 5.6f, 0.9f);
        r.shadow(0);
        int hullI = r.lastIndex();

        //canopy, set into the spine
        m = r.part(body);
        glass(m, canopy).taper(0, 1.2f, 1.0f, 2.4f, 3.2f, 1.9f, 1.0f, 1.6f, 0, -0.4f, false);
        r.on(hullI);

        //dorsal cargo deck: the tier-3 pilot carries a lot, and you can see it
        m = r.part(body);
        hull2(m, s).box(-2.0f, -3.9f, 1.4f, 2.0f, -1.0f, 2.0f);
        r.on(hullI);
        int deck = r.lastIndex();
        m = r.part(body);
        dark(m, s).box(-1.6f, -3.6f, 2.0f, 1.6f, -1.3f, 2.1f);
        r.on(deck).detail();
        m = r.part(body);
        team(m).box(-1.0f, -4.62f, -0.2f, 1.0f, -4.58f, 0.9f);
        r.on(hullI).detail();
        m = r.part(body);
        glow(m, s).box(-0.3f, -0.9f, 1.42f, 0.3f, 4.6f, 1.5f);
        r.on(hullI).detail();

        for(int sd = -1; sd <= 1; sd += 2){
            //---- outrigger: a short fixed spar from the hull shoulder to the duct
            m = r.part(body);
            hull2(m, s).box(sd < 0 ? -4.4f : 2.4f, -1.4f, -0.1f, sd < 0 ? -2.4f : 4.4f, 0.6f, 0.5f);
            r.on(hullI);
            int spar = r.lastIndex();
            m = r.part(body);
            steel(m, s).box(sd < 0 ? -4.2f : 2.6f, -1.0f, 0.5f, sd < 0 ? -2.6f : 4.2f, 0.2f, 0.72f);
            r.on(spar).detail();

            //---- duct: a ring standing on the spar end, rotor inside it
            m = r.part(body);
            hull(m, s).at(sd * 5.0f, -0.4f, 0.55f).ring(12, 1.55f, 2.1f, -0.75f, 0.75f);
            r.shadowRound(1 + (sd < 0 ? 0 : 1));
            int duct = r.lastIndex();
            m = r.part(body);
            trim(m, s).at(sd * 5.0f, -0.4f, 0.55f).ring(12, 2.1f, 2.25f, -0.35f, 0.35f);
            r.on(duct).detail();
            m = r.part(body);
            glow(m, s).at(sd * 5.0f, -0.4f, 0.55f).ring(12, 1.95f, 2.1f, -0.78f, -0.6f);
            r.on(duct).detail();

            //---- rotor on its own bone, flat inside the duct, turning about the vertical axis
            int fan = sd < 0 ? FANL : FANR;
            m = r.part(fan);
            steel(m, s).at(0, 0, 0).cyl(8, 0.45f, -0.22f, 0.26f);
            int rotor = r.lastIndex();
            for(int i = 0; i < 4; i++){
                m = r.part(fan);
                trim(m, s).at(0, 0, 0).rot(2, i * 90f).box(-0.17f, 0.42f, -0.06f, 0.17f, 1.5f, 0.1f);
                r.on(rotor).detail();
            }
            t.engine(fan, 0, 0f, -0.9f, 0.85f);
        }

        //---- cutter head under the nose: a drum in a yoke, the citadel's wall-mining tool
        m = r.part(body);
        hull2(m, s).box(-1.25f, 4.4f, -1.3f, 1.25f, 5.6f, -0.45f);
        r.on(hullI);
        r.lastIndex();
        m = r.part(CUTTER);
        dark(m, s).at(0, 0, 0).rot(0, 90).lathe(10, 18f, 0.001f, -1.05f, 0.62f, -0.85f, 0.62f, 0.85f, 0.001f, 1.05f);
        int drumI = r.lastIndex();
        m = r.part(CUTTER);
        glow2(m, s).at(0, 0, 0).rot(0, 90).ring(10, 0.62f, 0.78f, -0.18f, 0.18f);
        r.on(drumI).detail();
        t.weapon(0, -1, CUTTER, 0, 0.9f, 0);

        r.half = 9.6f;
        r.height = 3.2f;
        t.extra = (type, unit, d, delta) -> {
            //rotors spin up with throttle; the cutter idles and bites when the ship is working
            d.a += delta * (12f + 34f * d.speedF);
            d.b += delta * 9f;
            type.rig.rot(FANL, 2, d.a);
            type.rig.rot(FANR, 2, -d.a);
            type.rig.rot(CUTTER, 0, d.b);
        };
        return r.finish();
    }
}
