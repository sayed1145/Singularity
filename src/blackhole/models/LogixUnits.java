package blackhole.models;

import arc.graphics.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import blackhole.g3d.Kit.*;

import static blackhole.g3d.Kit.*;

/**
 * v8.1 unit rigs: the heavy hauler, the ground vanguard and the tanker.
 *
 * <p>Kit rules as always - one solid hull, plates bolted onto it, every moving part on a bone that hangs off
 * the body, nothing floating and nothing crossing another part.
 */
public final class LogixUnits{
    private LogixUnits(){}

    static final Color canopy = Color.valueOf("5a86c8"), tide = Color.valueOf("3f7fc0");

    /**
     * Lumen hauler: the heavy freight flier. A long open frame with four container bays and a lift fan at each
     * corner - the shape says "load carrier" at a glance and it is clearly bigger than the little drone.
     */
    public static Rig lumenHauler(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);

        Mesh m = r.part(body);
        hull(m, s).hexa(true, -3.2f, -6.4f, -0.7f, 3.2f, -6.4f, -0.7f, 2.4f, 6.8f, -0.6f, -2.4f, 6.8f, -0.6f,
            -2.6f, -5.6f, 1.5f, 2.6f, -5.6f, 1.5f, 1.7f, 6.0f, 1.2f, -1.7f, 6.0f, 1.2f);
        r.shadow(0);
        int hullI = r.lastIndex();

        //cockpit at the nose
        m = r.part(body);
        glass(m, canopy).taper(0, 4.6f, 1.15f, 1.3f, 1.9f, 1.55f, 0.6f, 1.0f, 0, -0.25f, false);
        r.on(hullI);

        //two container bays along the spine, each its own box, with a gap between them
        for(int i = 0; i < 2; i++){
            float cy = i == 0 ? 1.6f : -2.8f;
            m = r.part(body);
            dark(m, s).cbevel(0, cy, 1.4f, 2.1f, 1.8f, 1.5f, 0.22f);
            r.on(hullI);
            int crate = r.lastIndex();
            m = r.part(body);
            trim(m, s).box(-2.15f, cy - 0.3f, 1.6f, 2.15f, cy + 0.3f, 2.7f);
            r.on(crate).detail();
        }
        m = r.part(body);
        team(m).box(-1.2f, -5.4f, 1.51f, 1.2f, -4.2f, 1.56f);
        r.on(hullI).detail();

        //four lift arms with fans, one per corner, all clear of the bays
        for(int i = 0; i < 4; i++){
            float sx = (i & 1) == 0 ? -1 : 1, sy = (i & 2) == 0 ? -1 : 1;
            m = r.part(body);
            hull2(m, s).hexa(true, sx * 2.4f, sy * 2.6f, -0.2f, sx * 2.4f, sy * 4.0f, -0.2f, sx * 5.4f, sy * 4.4f, -0.1f, sx * 5.4f, sy * 3.0f, -0.1f,
                sx * 2.4f, sy * 2.6f, 0.6f, sx * 2.4f, sy * 4.0f, 0.6f, sx * 5.4f, sy * 4.4f, 0.4f, sx * 5.4f, sy * 3.0f, 0.4f);
            r.shadow(1);
            int arm = r.lastIndex();
            m = r.part(body);
            steel(m, s).at(sx * 5.4f, sy * 3.7f, 0.15f).lathe(10, 0, 1.25f, 0f, 1.25f, 0.4f, 0.7f, 0.7f);
            r.on(arm).detail();
            t.engine(body, sx * 5.4f, sy * 3.7f, 0.15f, 0.8f);
        }

        r.half = 7.5f;
        r.height = 3.4f;
        return r.finish();
    }

    /**
     * Aurite vanguard: the line-breaker. A wide tracked hull with a sloped glacis, a single heavy turret and a
     * short coaxial support gun; the turret drum and the barrel are the only moving parts.
     */
    public static Rig auriteVanguard(Tank3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int tur = r.bone(body, 0, -1.0f, 4.6f);
        int bar = r.bone(tur, 0, 1.6f, 1.6f);
        int coax = r.bone(tur, 2.2f, 1.4f, 0.6f);

        for(int sd = -1; sd <= 1; sd += 2){
            t.track(r, body, sd * 4.8f, 11.0f, 1.45f, 2.1f, 12, 5, s);
            Mesh m = r.part(body);
            hull2(m, s).taper(sd * 4.8f, 0, 2.9f, 2.4f, 12.8f, 3.5f, 2.0f, 12.0f, 0, 0, true);
            r.shadow(0);
        }

        Mesh m = r.part(body);
        hull(m, s).hexa(true, -4.8f, -9.6f, 1.1f, 4.8f, -9.6f, 1.1f, 3.9f, 9.4f, 1.1f, -3.9f, 9.4f, 1.1f,
            -4.1f, -8.6f, 4.3f, 4.1f, -8.6f, 4.3f, 3.0f, 8.4f, 3.9f, -3.0f, 8.4f, 3.9f);
        r.shadow(0);
        int hullI = r.lastIndex();

        //sloped glacis and side skirts
        m = r.part(body);
        hull2(m, s).taper(0, 6.2f, 3.95f, 2.9f, 5.2f, 4.9f, 2.1f, 3.0f, 0, -0.6f, false);
        r.on(hullI);
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(body);
            trim(m, s).box(sd * 4.1f - 0.4f, -7.0f, 1.5f, sd * 4.1f + 0.4f, 6.4f, 3.6f);
            r.on(hullI).detail();
        }
        m = r.part(body);
        team(m).box(-1.8f, -8.4f, 4.31f, 1.8f, -6.8f, 4.36f);
        r.on(hullI).detail();

        //turret drum
        m = r.part(tur);
        hull(m, s).at(0, 0, 0).lathe(10, 18f, 3.6f, -0.5f, 3.6f, 1.8f, 2.7f, 2.4f, 0.001f, 2.4f);
        r.on(hullI);
        int drum = r.lastIndex();
        m = r.part(tur);
        trim(m, s).at(0, -1.6f, 1.9f).cbevel(0, 0, 0, 1.5f, 1.1f, 0.7f, 0.14f);
        r.on(drum).detail();

        //main gun
        m = r.part(bar);
        steel(m, s).at(0, 1.2f, 0f).rot(0, -90).cyl(10, 0.95f, 0f, 7.4f);
        r.on(drum);
        int barI = r.lastIndex();
        m = r.part(bar);
        trim(m, s).at(0, 5.6f, 0f).rot(0, -90).lathe(10, 0, 1.25f, 0f, 1.25f, 1.5f, 0.95f, 1.7f);
        r.on(barI).detail();
        m = r.part(bar);
        glow(m, s).at(0, 8.7f, 0f).rot(0, -90).lathe(8, 0, 0.5f, 0f, 0.001f, 0.6f);
        r.on(barI).detail();
        t.weapon(0, tur, bar, 0, 9.0f, 1.6f);

        //coaxial support gun on the turret cheek
        m = r.part(coax);
        steel(m, s).at(0, 0.8f, 0f).rot(0, -90).cyl(8, 0.45f, 0f, 3.2f);
        r.on(drum).detail();
        t.weapon(1, tur, coax, 2.2f, 5.0f, 0.6f);

        r.half = 13.5f;
        r.height = 7.2f;
        return r.finish();
    }

    /**
     * Tide tender: a small naval support hull that carries liquid. A flat deck with a covered cistern amidships
     * and a pump mast at the stern - plain, and unmistakably a tanker.
     */
    public static Rig tideTender(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);

        Mesh m = r.part(body);
        hull(m, s).hexa(true, -3.0f, -6.0f, -0.9f, 3.0f, -6.0f, -0.9f, 1.6f, 7.4f, -0.8f, -1.6f, 7.4f, -0.8f,
            -3.4f, -5.4f, 1.2f, 3.4f, -5.4f, 1.2f, 1.9f, 7.0f, 1.0f, -1.9f, 7.0f, 1.0f);
        r.shadow(0);
        int hullI = r.lastIndex();

        //cistern amidships
        m = r.part(body);
        hull2(m, s).at(0, 0.4f, 1.2f).rot(0, 90).lathe(12, 15f, 0.001f, -3.4f, 2.1f, -3.0f, 2.1f, 3.0f, 0.001f, 3.4f);
        r.on(hullI);
        int cist = r.lastIndex();
        m = r.part(body);
        trim(m, s).box(-2.15f, -0.3f, 1.2f, 2.15f, 0.3f, 3.4f);
        r.on(cist).detail();
        //bridge at the bow, pump at the stern
        m = r.part(body);
        glass(m, canopy).taper(0, 5.2f, 1.0f, 1.2f, 1.4f, 1.9f, 0.6f, 0.8f, 0, -0.2f, false);
        r.on(hullI);
        m = r.part(body);
        steel(m, s).at(0, -4.4f, 1.2f).cyl(8, 0.85f, 0f, 1.8f);
        r.on(hullI).detail();
        m = r.part(body);
        glow(m, s).box(-0.5f, -4.9f, 3.0f, 0.5f, -3.9f, 3.15f);
        r.on(hullI).detail();
        m = r.part(body);
        team(m).box(-1.3f, -3.2f, 1.21f, 1.3f, -2.2f, 1.26f);
        r.on(hullI).detail();

        t.engine(body, 0f, -6.1f, -0.3f, 0.9f);
        r.half = 8f;
        r.height = 4f;
        return r.finish();
    }
}
