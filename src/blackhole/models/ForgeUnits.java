package blackhole.models;

import arc.graphics.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import blackhole.g3d.Kit.*;

import static blackhole.g3d.Kit.*;

/**
 * v8.0-beta unit rigs: the cargo drone, the repair drone and the ground engineer.
 *
 * <p>Kit rules as always - one hull solid, flat plates on it, every part on a bone that hangs off the body, so
 * nothing can drift off while the unit animates.
 */
public final class ForgeUnits{
    private ForgeUnits(){}

    static final Color canopy = Color.valueOf("5a86c8");

    /** Lumen drone: the cargo flier the stations launch. A flat body with a cargo cradle slung underneath. */
    public static Rig lumenDrone(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);

        Mesh m = r.part(body);
        hull(m, s).hexa(true, -2.4f, -3.2f, -0.5f, 2.4f, -3.2f, -0.5f, 1.8f, 3.6f, -0.4f, -1.8f, 3.6f, -0.4f,
            -1.8f, -2.8f, 1.1f, 1.8f, -2.8f, 1.1f, 1.2f, 3.2f, 0.9f, -1.2f, 3.2f, 0.9f);
        r.shadow(0);
        int hullI = r.lastIndex();
        //cargo cradle
        m = r.part(body);
        dark(m, s).cbevel(0, -0.2f, -1.5f, 1.9f, 2.6f, 1.1f, 0.2f);
        r.on(hullI);
        m = r.part(body);
        team(m).box(-0.9f, -2.6f, 1.11f, 0.9f, -1.6f, 1.16f);
        r.on(hullI).detail();
        m = r.part(body);
        glass(m, canopy).taper(0, 2.2f, 0.95f, 1.0f, 1.6f, 1.3f, 0.5f, 1.0f, 0, -0.2f, false);
        r.on(hullI);
        //four short arms with lift fans
        for(int i = 0; i < 4; i++){
            float sx = (i & 1) == 0 ? -1 : 1, sy = (i & 2) == 0 ? -1 : 1;
            m = r.part(body);
            hull2(m, s).hexa(true, sx * 1.6f, sy * 1.2f, -0.1f, sx * 1.6f, sy * 2.2f, -0.1f, sx * 3.6f, sy * 2.6f, 0f, sx * 3.6f, sy * 1.6f, 0f,
                sx * 1.6f, sy * 1.2f, 0.5f, sx * 1.6f, sy * 2.2f, 0.5f, sx * 3.6f, sy * 2.6f, 0.35f, sx * 3.6f, sy * 1.6f, 0.35f);
            r.shadow(1);
            int arm = r.lastIndex();
            m = r.part(body);
            steel(m, s).at(sx * 3.6f, sy * 2.1f, 0.1f).lathe(8, 0, 0.9f, 0f, 0.9f, 0.3f, 0.5f, 0.5f);
            r.on(arm).detail();
            t.engine(body, sx * 3.6f, sy * 2.1f, 0.1f, 0.6f);
        }
        r.half = 5f;
        r.height = 2.6f;
        return r.finish();
    }

    /** Lumen medic: a support flier with a repair lens in a static ring under the nose. */
    public static Rig lumenMedic(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int lens = r.bone(body, 0, 4.2f, 1.5f);

        Mesh m = r.part(body);
        hull(m, s).hexa(true, -3.0f, -4.8f, -0.8f, 3.0f, -4.8f, -0.8f, 2.0f, 5.4f, -0.6f, -2.0f, 5.4f, -0.6f,
            -2.3f, -4.2f, 1.3f, 2.3f, -4.2f, 1.3f, 1.3f, 4.8f, 1.0f, -1.3f, 4.8f, 1.0f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        glass(m, canopy).taper(0, 3.0f, 1.2f, 1.3f, 2.2f, 1.6f, 0.65f, 1.2f, 0, -0.25f, false);
        r.on(hullI);
        m = r.part(body);
        team(m).box(-1.1f, -4.0f, 1.31f, 1.1f, -2.8f, 1.36f);
        r.on(hullI).detail();
        //the repair emitter sits on top of the nose, not under it, so the glowing lens is visible from above:
        //a static collar bolted to the hull with the lens turning inside it
        m = r.part(body);
        trim(m, s).at(0, 4.2f, 1.0f).ring(12, 1.15f, 1.65f, 0f, 0.75f);
        r.on(hullI);
        int ring = r.lastIndex();
        m = r.part(lens);
        glow(m, s).at(0, 0, 0).lathe(10, 0, 1.1f, -0.5f, 1.0f, 0.1f, 0.001f, 0.55f);
        r.on(ring).detail();
        t.weapon(0, lens, lens, 0, 4.2f, 1.5f);
        //two cooling fins beside the collar - the silhouette that tells it apart from the courier
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(body);
            steel(m, s).box(sd * 1.9f - 0.3f, 2.0f, 1.0f, sd * 1.9f + 0.3f, 4.4f, 2.1f);
            r.on(hullI).detail();
        }
        //wings and engines
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(body);
            hull2(m, s).hexa(true, sd * 2.2f, -2.8f, -0.2f, sd * 2.2f, 2.2f, -0.2f, sd * 5.0f, 1.2f, 0f, sd * 5.0f, -2.4f, 0f,
                sd * 2.1f, -2.6f, 0.6f, sd * 2.1f, 2.0f, 0.5f, sd * 5.0f, 1.0f, 0.3f, sd * 5.0f, -2.2f, 0.3f);
            r.shadow(1);
            m = r.part(body);
            steel(m, s).at(sd * 4.0f, -3.2f, 0.05f).rot(0, 90).lathe(8, 0, 0.75f, 0f, 0.8f, 1.0f, 0.001f, 1.5f);
            r.shadowRound(0);
            m = r.part(body);
            glow(m, s).at(0, 0, 0).box(sd * 2.6f, -1.6f, 0.52f, sd * 4.6f, -1.0f, 0.56f);
            r.on(hullI).detail();
            t.engine(body, sd * 4.0f, -4.6f, 0.05f, 0.8f);
        }
        r.half = 6.5f;
        r.height = 3f;
        return r.finish();
    }

    /** Aurite wright: a tracked engineer. Flat deck, a build gantry over it, a small plating arm at the nose. */
    public static Rig auriteWright(Tank3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int arm = r.bone(body, 0, 6.2f, 3.2f);

        for(int sd = -1; sd <= 1; sd += 2){
            t.track(r, body, sd * 3.8f, 7.2f, 1.15f, 1.7f, 10, 4, s);
            Mesh m = r.part(body);
            hull2(m, s).taper(sd * 3.8f, 0, 2.3f, 2.0f, 8.8f, 2.6f, 1.7f, 8.2f, 0, 0, true);
            r.shadow(0);
        }

        Mesh m = r.part(body);
        hull(m, s).hexa(true, -3.8f, -6.2f, 0.9f, 3.8f, -6.2f, 0.9f, 3.0f, 6.0f, 0.9f, -3.0f, 6.0f, 0.9f,
            -3.1f, -5.6f, 3.2f, 3.1f, -5.6f, 3.2f, 2.3f, 5.4f, 3.0f, -2.3f, 5.4f, 3.0f);
        r.shadow(0);
        int hullI = r.lastIndex();
        //material rack on the deck
        m = r.part(body);
        dark(m, s).cbevel(0, -3.0f, 3.1f, 2.5f, 2.2f, 1.3f, 0.22f);
        r.on(hullI);
        m = r.part(body);
        team(m).box(-1.4f, -4.2f, 4.41f, 1.4f, -3.2f, 4.46f);
        r.on(hullI).detail();
        //gantry: two static posts with a cross beam, the plating arm hangs inside it
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(body);
            steel(m, s).box(sd * 2.4f - 0.35f, 3.4f, 3.0f, sd * 2.4f + 0.35f, 4.1f, 5.4f);
            r.on(hullI).detail();
        }
        m = r.part(body);
        steel(m, s).box(-2.8f, 3.4f, 5.4f, 2.8f, 4.1f, 5.9f);
        r.on(hullI).detail();
        m = r.part(arm);
        trim(m, s).at(0, 0, 0).lathe(8, 22.5f, 1.1f, -0.5f, 1.1f, 0.5f, 0.6f, 0.9f);
        r.on(hullI);
        int armI = r.lastIndex();
        m = r.part(arm);
        glow(m, s).at(0, 0, -0.8f).cyl(8, 0.5f, 0f, 0.35f);
        r.on(armI).detail();
        t.weapon(0, arm, arm, 0, 7.4f, 2.8f);

        r.half = 9f;
        r.height = 5.4f;
        return r.finish();
    }
}
