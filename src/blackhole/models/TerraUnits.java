package blackhole.models;

import arc.graphics.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import blackhole.g3d.Kit.*;

import static blackhole.g3d.Kit.*;

/**
 * v7.9 unit rigs: the four units that come with the terrain update.
 *
 * <p>Same rules as the rest of the kit - one hull solid per unit, flat plates on top of it, turrets that sit
 * on the hull and barrels that sit in the turret. Every piece hangs off a bone that hangs off the body, so no
 * part can drift away while the unit animates, and every live offset is identical in the rest pose.
 */
public final class TerraUnits{
    private TerraUnits(){}

    static final Color canopy = Color.valueOf("5a86c8");

    /**
     * Terra borer: the mining engineer. A tracked hull with a drill head on a short arm at the front - the head
     * is what chews ore out of rock walls - plus a cargo box and a small defensive gun.
     */
    public static Rig terraBorer(Tank3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int head = r.bone(body, 0, 8.2f, 0.6f);
        int tur = r.bone(body, 0, -3.4f, 2.9f), bar = r.bone(tur, 0, 0.9f, 0.5f);

        for(int sd = -1; sd <= 1; sd += 2){
            t.track(r, body, sd * 4.2f, 8.4f, 1.25f, 1.9f, 10, 4, s);
            Mesh m = r.part(body);
            hull2(m, s).taper(sd * 4.2f, 0, 2.5f, 2.2f, 10.2f, 3.0f, 1.9f, 9.6f, 0, 0, true);
            r.shadow(0);
        }

        //hull
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -4.3f, -7.4f, 1.0f, 4.3f, -7.4f, 1.0f, 3.5f, 7.2f, 1.0f, -3.5f, 7.2f, 1.0f,
            -3.6f, -6.6f, 3.8f, 3.6f, -6.6f, 3.8f, 2.8f, 6.4f, 3.6f, -2.8f, 6.4f, 3.6f);
        r.shadow(0);
        int hullI = r.lastIndex();

        //cargo box over the rear deck
        m = r.part(body);
        dark(m, s).cbevel(0, -4.4f, 3.7f, 2.9f, 2.4f, 1.5f, 0.25f);
        r.on(hullI);
        m = r.part(body);
        team(m).box(-1.6f, -5.0f, 5.21f, 1.6f, -3.8f, 5.26f);
        r.on(hullI).detail();

        //drill arm and head: a stubby cone of cutters on a fixed mount, so it never reads as a loose part
        m = r.part(body);
        steel(m, s).at(0, 6.0f, 1.9f).rot(0, -90).cyl(8, 1.0f, 0f, 2.6f);
        r.on(hullI);
        int armI = r.lastIndex();
        m = r.part(head);
        trim(m, s).at(0, 0, 0).rot(0, -90).lathe(10, 0, 2.0f, 0f, 2.0f, 1.5f, 1.2f, 2.6f, 0.001f, 3.2f);
        r.on(armI);
        int headI = r.lastIndex();
        //four cutter ribs, kept inside the silhouette of the head so the nose stays one tidy shape
        for(int i = 0; i < 4; i++){
            m = r.part(head);
            steel(m, s).at(0, 0, 0).rot(0, -90).rot(2, i * 90f + 45f).box(-0.3f, 1.3f, 0.2f, 0.3f, 1.95f, 1.45f);
            r.on(headI).detail();
        }
        m = r.part(head);
        glow(m, s).at(0, 2.2f, 0).rot(0, -90).cyl(8, 0.5f, 0f, 0.9f);
        r.on(headI).detail();

        //small rear turret
        m = r.part(tur);
        hull(m, s).at(0, 0, 0).cbevel(0, 0, -0.4f, 2.1f, 2.4f, 1.6f, 0.35f);
        r.on(hullI);
        int drum = r.lastIndex();
        m = r.part(bar);
        steel(m, s).at(0, 0.6f, 0.1f).rot(0, -90).cyl(8, 0.52f, 0f, 3.6f);
        r.on(drum);
        t.weapon(0, tur, bar, 0, 4.4f, 0.1f);

        r.half = 10.5f;
        r.height = 5.6f;
        return r.finish();
    }

    /**
     * Basalt guard: the heavy line tank. A broad hull on wide tracks, a thick turret with two short barrels,
     * and armour plates bolted along the flanks.
     */
    public static Rig basaltGuard(Tank3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int tur = r.bone(body, 0, -0.6f, 4.0f);
        int[] bar = new int[2];
        for(int i = 0; i < 2; i++) bar[i] = r.bone(tur, (i == 0 ? 1 : -1) * 1.6f, 1.2f, 1.9f);

        for(int sd = -1; sd <= 1; sd += 2){
            t.track(r, body, sd * 5.4f, 10.2f, 1.5f, 2.3f, 12, 5, s);
            Mesh m = r.part(body);
            hull2(m, s).taper(sd * 5.4f, 0, 3.0f, 2.6f, 12.4f, 3.6f, 2.2f, 11.6f, 0, 0, true);
            r.shadow(0);
        }

        Mesh m = r.part(body);
        hull(m, s).hexa(true, -5.4f, -9.2f, 1.2f, 5.4f, -9.2f, 1.2f, 4.4f, 9.0f, 1.2f, -4.4f, 9.0f, 1.2f,
            -4.6f, -8.2f, 4.6f, 4.6f, -8.2f, 4.6f, 3.4f, 8.0f, 4.2f, -3.4f, 8.0f, 4.2f);
        r.shadow(0);
        int hullI = r.lastIndex();

        //glacis plate and flank armour
        m = r.part(body);
        hull2(m, s).taper(0, 5.6f, 4.25f, 3.2f, 5.6f, 5.1f, 2.4f, 3.2f, 0, -0.5f, false);
        r.on(hullI);
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(body);
            trim(m, s).box(sd * 4.6f - 0.45f, -6.6f, 1.6f, sd * 4.6f + 0.45f, 6.0f, 3.9f);
            r.on(hullI).detail();
        }
        m = r.part(body);
        team(m).box(-2.0f, -8.0f, 4.61f, 2.0f, -6.4f, 4.66f);
        r.on(hullI).detail();

        //turret: a faceted drum with twin barrels
        m = r.part(tur);
        hull(m, s).at(0, 0, 0).lathe(8, 22.5f, 4.0f, -0.5f, 4.0f, 1.9f, 3.1f, 2.6f, 0.001f, 2.6f);
        r.on(hullI);
        int drum = r.lastIndex();
        m = r.part(tur);
        trim(m, s).at(0, 0, 2.5f).lathe(8, 22.5f, 2.2f, 0f, 2.2f, 0.4f, 1.3f, 0.7f);
        r.on(drum).detail();
        for(int i = 0; i < 2; i++){
            m = r.part(bar[i]);
            steel(m, s).at(0, 1.4f, 0.1f).rot(0, -90).cyl(8, 0.82f, 0f, 6.6f);
            r.on(drum);
            int b = r.lastIndex();
            m = r.part(bar[i]);
            glow(m, s).at(0, 7.9f, 0.1f).rot(0, -90).lathe(8, 0, 0.42f, 0f, 0.001f, 0.6f);
            r.on(b).detail();
            t.weapon(i, tur, bar[i], (i == 0 ? 1 : -1) * 1.6f, 8.3f, 0.1f);
        }

        r.half = 13f;
        r.height = 7f;
        return r.finish();
    }

    /** Glass harrier: a small, very fast delta interceptor - one wedge, two wing guns, two engines. */
    public static Rig glassHarrier(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int[] gun = new int[2];
        for(int i = 0; i < 2; i++) gun[i] = r.bone(body, (i == 0 ? 1 : -1) * 3.4f, 1.6f, -0.1f);

        Mesh m = r.part(body);
        hull(m, s).hexa(true, -2.2f, -5.0f, -0.6f, 2.2f, -5.0f, -0.6f, 0.9f, 6.6f, -0.4f, -0.9f, 6.6f, -0.4f,
            -1.6f, -4.4f, 1.2f, 1.6f, -4.4f, 1.2f, 0.5f, 6.0f, 0.8f, -0.5f, 6.0f, 0.8f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        glass(m, canopy).taper(0, 3.0f, 1.1f, 1.1f, 2.2f, 1.5f, 0.55f, 1.2f, 0, -0.2f, false);
        r.on(hullI);
        m = r.part(body);
        team(m).box(-0.8f, -4.2f, 1.21f, 0.8f, -3.0f, 1.26f);
        r.on(hullI).detail();

        for(int i = 0; i < 2; i++){
            float sd = i == 0 ? 1 : -1;
            //swept delta wing
            m = r.part(body);
            hull2(m, s).hexa(true, sd * 1.6f, -3.6f, -0.2f, sd * 1.6f, 3.2f, -0.2f, sd * 5.8f, 0.4f, 0f, sd * 5.8f, -3.2f, 0f,
                sd * 1.5f, -3.4f, 0.6f, sd * 1.5f, 3.0f, 0.5f, sd * 5.8f, 0.2f, 0.25f, sd * 5.8f, -3.0f, 0.25f);
            r.shadow(1);
            int wing = r.lastIndex();
            //wing fence, so the silhouette is not a bare triangle
            m = r.part(body);
            trim(m, s).box(sd * 4.2f - 0.12f, -2.4f, 0.1f, sd * 4.2f + 0.12f, 0.8f, 0.95f);
            r.on(wing).detail();
            m = r.part(gun[i]);
            steel(m, s).at(0, 0, 0).rot(0, -90).cyl(6, 0.3f, 0f, 3.0f);
            t.weapon(i, gun[i], gun[i], 0, 3.1f, 0);
            m = r.part(body);
            steel(m, s).at(sd * 2.8f, -3.8f, 0f).rot(0, 90).lathe(8, 0, 0.72f, 0f, 0.78f, 1.0f, 0.001f, 1.5f);
            r.shadowRound(0);
            t.engine(body, sd * 2.8f, -5.2f, 0f, 0.78f);
        }
        r.half = 7f;
        r.height = 2.8f;
        return r.finish();
    }

    /**
     * Vent caller: a slow support gunship. A deep fuselage with a rotating belly turret that lobs shells, and
     * two outrigger engine pods.
     */
    public static Rig ventCaller(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int tur = r.bone(body, 0, 0.6f, 2.5f), bar = r.bone(tur, 0, 0.8f, 0.3f);

        Mesh m = r.part(body);
        hull(m, s).hexa(true, -4.0f, -7.2f, -1.2f, 4.0f, -7.2f, -1.2f, 2.6f, 7.8f, -1.0f, -2.6f, 7.8f, -1.0f,
            -3.2f, -6.4f, 1.8f, 3.2f, -6.4f, 1.8f, 1.8f, 7.0f, 1.5f, -1.8f, 7.0f, 1.5f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        hull2(m, s).taper(0, 0.4f, 1.75f, 2.4f, 9.4f, 2.6f, 1.3f, 7.0f, 0, -0.4f, false);
        r.on(hullI);
        int spine = r.lastIndex();
        m = r.part(body);
        glass(m, canopy).taper(0, 5.4f, 1.45f, 1.5f, 2.4f, 2.0f, 0.8f, 1.5f, 0, -0.3f, false);
        r.on(hullI);
        m = r.part(body);
        team(m).box(-1.4f, -6.0f, 1.81f, 1.4f, -4.6f, 1.86f);
        r.on(hullI).detail();

        //deck turret, seated between two static cheek plates so it can never read as a loose ring
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(body);
            dark(m, s).box(sd * 2.6f - 0.3f, -1.2f, 1.5f, sd * 2.6f + 0.3f, 2.6f, 3.1f);
            r.on(spine).detail();
        }
        m = r.part(tur);
        hull(m, s).at(0, 0, 0).lathe(8, 22.5f, 2.0f, -0.7f, 2.0f, 0.7f, 1.4f, 1.4f, 0.001f, 1.4f);
        r.on(spine);
        int drum = r.lastIndex();
        m = r.part(tur);
        trim(m, s).at(0, 0, 1.3f).lathe(8, 22.5f, 1.1f, 0f, 1.1f, 0.3f, 0.6f, 0.5f);
        r.on(drum).detail();
        m = r.part(bar);
        steel(m, s).at(0, 1.0f, 0f).rot(0, -90).cyl(8, 0.66f, 0f, 4.0f);
        r.on(drum);
        int bi = r.lastIndex();
        m = r.part(bar);
        glow(m, s).at(0, 5.2f, 0f).rot(0, -90).lathe(8, 0, 0.36f, 0f, 0.001f, 0.5f);
        r.on(bi).detail();
        t.weapon(0, tur, bar, 0, 5.4f, 0f);

        //outrigger pods
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(body);
            hull2(m, s).hexa(true, sd * 3.0f, -3.4f, -0.4f, sd * 3.0f, 2.6f, -0.4f, sd * 6.4f, 1.6f, -0.2f, sd * 6.4f, -2.8f, -0.2f,
                sd * 2.9f, -3.2f, 0.9f, sd * 2.9f, 2.4f, 0.8f, sd * 6.4f, 1.4f, 0.35f, sd * 6.4f, -2.6f, 0.35f);
            r.shadow(1);
            m = r.part(body);
            steel(m, s).at(sd * 5.4f, -4.0f, 0.1f).rot(0, 90).lathe(8, 0, 0.9f, 0f, 0.95f, 1.2f, 0.001f, 1.8f);
            r.shadowRound(0);
            t.engine(body, sd * 5.4f, -5.6f, 0.1f, 0.95f);
        }
        r.half = 9f;
        r.height = 4.2f;
        return r.finish();
    }
}
