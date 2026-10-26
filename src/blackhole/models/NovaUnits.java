package blackhole.models;

import arc.graphics.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import blackhole.g3d.Kit.*;

import static blackhole.g3d.Kit.*;

/**
 * v7.8 unit rigs. Plain shapes only: one hull solid, flat plates, a turret that sits on the hull and barrels
 * that sit in the turret. Nothing floats - every piece is attached to a bone that is attached to the body, so
 * no part can ever drift away from the unit while it animates.
 */
public final class NovaUnits{
    private NovaUnits(){}

    static final Color canopy = Color.valueOf("5a86c8");

    public static int HEAVY_TURRET, HEAVY_BARREL;

    /** Aurora heavy: plain heavy gunship - flat hull, one rotating top turret, two fixed side guns. */
    public static Rig auroraHeavy(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        HEAVY_TURRET = r.bone(body, 0, 2.5f, 2.3f);
        HEAVY_BARREL = r.bone(HEAVY_TURRET, 0, 0, 0);
        int[] gun = new int[2];
        for(int i = 0; i < 2; i++) gun[i] = r.bone(body, (i == 0 ? 1 : -1) * 8.4f, -2f, 0.2f);

        //hull
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -6.4f, -10.5f, -1.4f, 6.4f, -10.5f, -1.4f, 3.6f, 11.5f, -1.0f, -3.6f, 11.5f, -1.0f,
            -5.0f, -9.4f, 1.9f, 5.0f, -9.4f, 1.9f, 2.4f, 10.2f, 1.5f, -2.4f, 10.2f, 1.5f);
        r.shadow(0);
        int hullI = r.lastIndex();

        //spine plate
        m = r.part(body);
        hull2(m, s).taper(0, 0.5f, 1.85f, 3.4f, 13f, 2.9f, 1.8f, 9.5f, 0, -0.4f, false);
        r.on(hullI);
        int spine = r.lastIndex();

        //canopy
        m = r.part(body);
        glass(m, canopy).taper(0, 7.6f, 1.4f, 1.9f, 2.8f, 2.0f, 1.0f, 1.7f, 0, -0.3f, false);
        r.on(hullI);

        //team stripes
        for(int i = 0; i < 2; i++){
            float sd = i == 0 ? 1 : -1;
            m = r.part(body);
            team(m).hexa(false, sd * 5.0f, -9.0f, 1.2f, sd * 4.6f, -9.0f, 1.9f, sd * 2.8f, 5.0f, 1.55f, sd * 3.2f, 5.0f, 0.85f,
                sd * 5.2f, -9.0f, 1.2f, sd * 4.8f, -9.0f, 1.9f, sd * 3.0f, 5.0f, 1.55f, sd * 3.4f, 5.0f, 0.85f);
            r.on(hullI).detail();
        }

        //side wings + fixed guns
        for(int i = 0; i < 2; i++){
            float sd = i == 0 ? 1 : -1;
            m = r.part(body);
            hull2(m, s).hexa(true, sd * 4.8f, -5.0f, -0.4f, sd * 4.8f, 3.4f, -0.4f, sd * 8.8f, 2.0f, -0.2f, sd * 8.8f, -4.0f, -0.2f,
                sd * 4.6f, -4.6f, 0.9f, sd * 4.6f, 3.0f, 0.8f, sd * 8.8f, 1.6f, 0.4f, sd * 8.8f, -3.6f, 0.4f);
            r.shadow(1 + i);
            m = r.part(gun[i]);
            steel(m, s).at(0, 0, 0).rot(0, -90).cyl(8, 0.62f, 0f, 6.2f);
            m = r.part(gun[i]);
            trim(m, s).at(0, 2.4f, 0).rot(0, -90).cyl(8, 0.95f, 0f, 0.8f);
            t.weapon(1 + i, gun[i], gun[i], 0, 6.4f, 0);
        }

        //top turret: a short drum with one barrel
        m = r.part(HEAVY_TURRET);
        hull(m, s).at(0, 0, 0).cbevel(0, 0, -0.3f, 3.4f, 4.2f, 2.1f, 0.5f);
        r.on(spine);
        int drum = r.lastIndex();
        m = r.part(HEAVY_TURRET);
        trim(m, s).at(0, 0, 1.8f).lathe(10, 0, 1.5f, 0f, 1.5f, 0.35f, 0.9f, 0.6f);
        r.on(drum).detail();
        m = r.part(HEAVY_BARREL);
        steel(m, s).at(0, 1.6f, 0.4f).rot(0, -90).cyl(10, 0.9f, 0f, 7.0f);
        r.on(drum);
        m = r.part(HEAVY_BARREL);
        glow(m, s).at(0, 8.5f, 0.4f).rot(0, -90).lathe(8, 0, 0.45f, 0f, 0.001f, 0.7f);
        r.on(drum).detail();
        t.weapon(0, HEAVY_TURRET, HEAVY_BARREL, 0, 8.8f, 0.4f);

        //engines
        for(int k = 0; k < 4; k++){
            float x = -3.9f + k * 2.6f;
            m = r.part(body);
            steel(m, s).at(x, -10.0f, 0.3f).rot(0, 90).lathe(10, 0, 1.0f, 0f, 1.05f, 1.5f, 0.85f, 2.2f, 0.001f, 2.2f);
            r.shadowRound(0);
            t.engine(body, x, -12.2f, 0.3f, 1.05f);
        }

        r.half = 13f;
        r.height = 5.2f;
        return r.finish();
    }

    // =====================================================================================================
    // v7.8 ground units
    // =====================================================================================================

    /** Prism scout: light tracked scout - low hull, small dome turret, one short barrel. */
    public static Rig prismScout(Tank3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int tur = r.bone(body, 0, -0.4f, 2.8f), bar = r.bone(tur, 0, 1.0f, 0.7f);
        for(int sd = -1; sd <= 1; sd += 2){
            t.track(r, body, sd * 3.5f, 7.6f, 1.1f, 1.7f, 10, 4, s);
            Mesh m = r.part(body);
            hull2(m, s).taper(sd * 3.5f, 0, 2.3f, 2.0f, 9.2f, 2.7f, 1.7f, 8.6f, 0, 0, true);
            r.shadow(0);
        }
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -2.7f, -4.3f, 0.7f, 2.7f, -4.3f, 0.7f, 2.7f, 4.5f, 0.7f, -2.7f, 4.5f, 0.7f,
            -2.4f, -3.9f, 2.8f, 2.4f, -3.9f, 2.8f, 1.9f, 2.6f, 2.8f, -1.9f, 2.6f, 2.8f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        glow(m, s).box(-1.5f, 4.52f, 1.3f, 1.5f, 4.6f, 1.7f);
        r.on(hullI).detail();
        m = r.part(body);
        team(m).box(-1.0f, -3.6f, 2.81f, 1.0f, -2.4f, 2.86f);
        r.on(hullI).detail();
        m = r.part(tur);
        hull2(m, s).at(0, 0, 0).lathe(8, 22.5f, 1.9f, 0f, 1.8f, 0.8f, 1.1f, 1.3f, 0.001f, 1.4f);
        int dome = r.lastIndex();
        m = r.part(tur);
        glow(m, s).at(0.7f, -0.7f, 1.1f).cyl(6, 0.25f, 0f, 0.18f);
        r.on(dome).detail();
        m = r.part(bar);
        steel(m, s).at(0, 0, 0).rot(0, -90).cyl(8, 0.34f, 0f, 3.4f);
        t.weapon(0, tur, bar, 0, 3.6f, 0);
        r.half = 6f;
        r.height = 4.6f;
        r.stackSort = true;
        return r.finish();
    }

    /** Aurite lancer: medium tank with one long gun; a plain wedge hull and a low turret. */
    public static Rig auriteLancer(Tank3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int tur = r.bone(body, 0, -1.0f, 4.0f), bar = r.bone(tur, 0, 1.8f, 1.0f);
        for(int sd = -1; sd <= 1; sd += 2){
            t.track(r, body, sd * 5.4f, 12.4f, 1.6f, 2.5f, 14, 5, s);
            Mesh m = r.part(body);
            hull2(m, s).taper(sd * 5.5f, 0, 3.3f, 3.0f, 14.6f, 4.0f, 2.5f, 13.6f, 0, 0, true);
            r.shadow(0);
        }
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -4.2f, -7.0f, 0.9f, 4.2f, -7.0f, 0.9f, 4.2f, 7.2f, 0.9f, -4.2f, 7.2f, 0.9f,
            -3.8f, -6.4f, 4.0f, 3.8f, -6.4f, 4.0f, 3.0f, 4.4f, 4.0f, -3.0f, 4.4f, 4.0f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        glow(m, s).box(-2.2f, 7.22f, 1.8f, 2.2f, 7.3f, 2.4f);
        r.on(hullI).detail();
        m = r.part(body);
        team(m).box(-1.5f, -6.0f, 4.01f, 1.5f, -4.4f, 4.06f);
        r.on(hullI).detail();
        m = r.part(tur);
        hull2(m, s).at(0, 0, 0).lathe(8, 22.5f, 3.0f, 0f, 2.9f, 1.1f, 2.0f, 1.9f, 0.001f, 2.0f);
        int dome = r.lastIndex();
        m = r.part(tur);
        trim(m, s).at(0, 1.6f, 0.9f).rot(0, -90).cyl(8, 0.85f, 0f, 1.2f);
        r.on(dome);
        m = r.part(bar);
        steel(m, s).at(0, 0, 0).rot(0, -90).cyl(8, 0.55f, 0f, 6.4f);
        r.on(dome);
        m = r.part(bar);
        dark(m, s).at(0, 5.6f, 0).rot(0, -90).cyl(8, 0.8f, 0f, 0.9f);
        r.on(dome).detail();
        t.weapon(0, tur, bar, 0, 6.8f, 0);
        r.half = 9f;
        r.height = 6.2f;
        r.stackSort = true;
        return r.finish();
    }

    /** Resonance siege: heavy artillery tank - wide hull, broad tracks, one thick mortar tube. */
    public static Rig resonanceSiege(Tank3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int tur = r.bone(body, 0, -2.0f, 5.2f), bar = r.bone(tur, 0, 2.2f, 1.4f);
        for(int sd = -1; sd <= 1; sd += 2){
            t.track(r, body, sd * 6.8f, 15.4f, 1.9f, 3.0f, 16, 5, s);
            Mesh m = r.part(body);
            hull2(m, s).taper(sd * 6.9f, 0, 4.0f, 3.6f, 18f, 4.8f, 3.0f, 17f, 0, 0, true);
            r.shadow(0);
        }
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -5.2f, -8.8f, 1.0f, 5.2f, -8.8f, 1.0f, 5.2f, 8.4f, 1.0f, -5.2f, 8.4f, 1.0f,
            -4.8f, -8.0f, 5.0f, 4.8f, -8.0f, 5.0f, 4.0f, 5.4f, 5.0f, -4.0f, 5.4f, 5.0f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        glow2(m, s).box(-2.8f, 8.42f, 2.2f, 2.8f, 8.5f, 2.9f);
        r.on(hullI).detail();
        m = r.part(body);
        team(m).box(-1.8f, -7.6f, 5.01f, 1.8f, -5.6f, 5.06f);
        r.on(hullI).detail();
        //two spades at the back: they stay on the hull, they do not deploy
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(body);
            dark(m, s).box(sd * 3.4f - 0.9f, -9.6f, 1.2f, sd * 3.4f + 0.9f, -8.6f, 3.4f);
            r.on(hullI);
        }
        m = r.part(tur);
        hull2(m, s).at(0, 0, 0).lathe(8, 22.5f, 3.6f, 0f, 3.4f, 1.3f, 2.4f, 2.1f, 0.001f, 2.2f);
        int dome = r.lastIndex();
        m = r.part(bar);
        steel(m, s).at(0, 0, 0).rot(0, -90).cyl(10, 1.1f, 0f, 5.6f);
        r.on(dome);
        m = r.part(bar);
        trim(m, s).at(0, 4.6f, 0).rot(0, -90).ring(10, 1.1f, 1.5f, 0f, 0.7f);
        r.on(dome).detail();
        m = r.part(bar);
        glow2(m, s).at(0, 5.7f, 0).rot(0, -90).lathe(8, 0, 0.6f, 0f, 0.001f, 0.8f);
        r.on(dome).detail();
        t.weapon(0, tur, bar, 0, 6.2f, 0);
        r.half = 10f;
        r.height = 7.4f;
        r.stackSort = true;
        return r.finish();
    }

    // =====================================================================================================
    // v7.8 air units
    // =====================================================================================================

    /** Lumen courier: plain support flier - flat body, a cargo box underneath, a mining/building lens at the nose. */
    public static Rig lumenCourier(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int lens = r.bone(body, 0, 5.6f, -0.9f);

        Mesh m = r.part(body);
        hull(m, s).hexa(true, -3.4f, -5.6f, -0.9f, 3.4f, -5.6f, -0.9f, 2.2f, 6.2f, -0.7f, -2.2f, 6.2f, -0.7f,
            -2.6f, -5.0f, 1.3f, 2.6f, -5.0f, 1.3f, 1.4f, 5.6f, 1.1f, -1.4f, 5.6f, 1.1f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        glass(m, canopy).taper(0, 3.8f, 1.1f, 1.4f, 2.2f, 1.6f, 0.7f, 1.3f, 0, -0.2f, false);
        r.on(hullI);
        m = r.part(body);
        dark(m, s).cbevel(0, -0.6f, -2.2f, 3.2f, 5.4f, 1.4f, 0.2f);
        r.on(hullI);
        m = r.part(body);
        team(m).box(-1.2f, -4.8f, 1.31f, 1.2f, -3.4f, 1.36f);
        r.on(hullI).detail();
        m = r.part(lens);
        trim(m, s).at(0, 0, 0).lathe(8, 0, 0.9f, 0f, 0.7f, 0.5f, 0.001f, 0.8f);
        r.on(hullI);
        int lensI = r.lastIndex();
        m = r.part(lens);
        glow(m, s).at(0, 0, -0.5f).cyl(8, 0.45f, 0f, 0.2f);
        r.on(lensI).detail();
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(body);
            hull2(m, s).hexa(true, sd * 2.6f, -3.0f, -0.2f, sd * 2.6f, 2.4f, -0.2f, sd * 5.4f, 1.4f, 0f, sd * 5.4f, -2.6f, 0f,
                sd * 2.5f, -2.8f, 0.7f, sd * 2.5f, 2.2f, 0.6f, sd * 5.4f, 1.2f, 0.3f, sd * 5.4f, -2.4f, 0.3f);
            r.shadow(1);
            m = r.part(body);
            steel(m, s).at(sd * 4.6f, -3.4f, 0.1f).rot(0, 90).lathe(8, 0, 0.8f, 0f, 0.85f, 1.1f, 0.001f, 1.6f);
            r.shadowRound(0);
            t.engine(body, sd * 4.6f, -4.9f, 0.1f, 0.85f);
        }
        r.half = 7f;
        r.height = 3.4f;
        return r.finish();
    }

    /** Aurora sentry: small interceptor - one wedge, two fixed guns, two engines. Nothing articulated. */
    public static Rig auroraSentry(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int[] gun = new int[2];
        for(int i = 0; i < 2; i++) gun[i] = r.bone(body, (i == 0 ? 1 : -1) * 2.6f, 2.0f, -0.1f);

        Mesh m = r.part(body);
        hull(m, s).hexa(true, -2.6f, -4.4f, -0.6f, 2.6f, -4.4f, -0.6f, 1.1f, 5.6f, -0.4f, -1.1f, 5.6f, -0.4f,
            -1.9f, -3.9f, 1.1f, 1.9f, -3.9f, 1.1f, 0.6f, 5.0f, 0.8f, -0.6f, 5.0f, 0.8f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        glass(m, canopy).taper(0, 2.6f, 1.0f, 1.2f, 2.0f, 1.4f, 0.6f, 1.1f, 0, -0.2f, false);
        r.on(hullI);
        m = r.part(body);
        team(m).box(-0.9f, -3.8f, 1.11f, 0.9f, -2.6f, 1.16f);
        r.on(hullI).detail();
        for(int i = 0; i < 2; i++){
            float sd = i == 0 ? 1 : -1;
            m = r.part(body);
            hull2(m, s).hexa(true, sd * 1.9f, -2.6f, -0.2f, sd * 1.9f, 2.2f, -0.2f, sd * 4.6f, 0.9f, 0f, sd * 4.6f, -2.2f, 0f,
                sd * 1.8f, -2.4f, 0.6f, sd * 1.8f, 2.0f, 0.5f, sd * 4.6f, 0.7f, 0.25f, sd * 4.6f, -2.0f, 0.25f);
            r.shadow(1);
            m = r.part(gun[i]);
            steel(m, s).at(0, 0, 0).rot(0, -90).cyl(6, 0.32f, 0f, 2.8f);
            t.weapon(i, gun[i], gun[i], 0, 2.9f, 0);
            m = r.part(body);
            steel(m, s).at(sd * 3.4f, -3.2f, 0f).rot(0, 90).lathe(8, 0, 0.7f, 0f, 0.75f, 1.0f, 0.001f, 1.4f);
            r.shadowRound(0);
            t.engine(body, sd * 3.4f, -4.5f, 0f, 0.75f);
        }
        r.half = 6f;
        r.height = 2.8f;
        return r.finish();
    }
}
