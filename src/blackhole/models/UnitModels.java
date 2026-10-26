package blackhole.models;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import blackhole.g3d.Kit.*;

import static blackhole.g3d.Kit.*;

/**
 * 3D rigs of the aircraft (Aurelia fleet + Singularity air wing). Design units = world units (+y forward,
 * z up). Every piece is one convex solid; limbs / wings live on their own bones.
 */
public final class UnitModels{
    private UnitModels(){}

    static final Color canopy = Color.valueOf("5a86c8"), canopySing = Color.valueOf("c28a4a");

    // =====================================================================================================
    // Aurelia
    // =====================================================================================================

    public static int PILOT_WL, PILOT_WR, PILOT_EMIT;

    /** Lumen pilot: the player's pearl delta interceptor; the wings sweep back with speed, a lumen lens under the nose mines and builds. */
    public static Rig lumenPilot(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        PILOT_WL = r.bone(body, -1.5f, -0.6f, 0.1f);
        PILOT_WR = r.bone(body, 1.5f, -0.6f, 0.1f);
        PILOT_EMIT = r.bone(body, 0, 6.9f, -0.35f);

        Mesh m = r.part(body);
        hull(m, s).hexa(true, -1.8f, -5f, -0.7f, 1.8f, -5f, -0.7f, 0.5f, 7.4f, -0.4f, -0.5f, 7.4f, -0.4f,
            -1.3f, -4.6f, 1.1f, 1.3f, -4.6f, 1.1f, 0.25f, 6.4f, 0.35f, -0.25f, 6.4f, 0.35f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        glass(m, canopy).taper(0, 1.2f, 0.72f, 1.5f, 3.2f, 1.45f, 0.6f, 1.6f, 0, -0.4f, false);
        r.on(hullI);
        m = r.part(body);
        glow(m, s).hexa(false, -0.5f, -4.7f, 1.0f, 0.5f, -4.7f, 1.0f, 0.5f, -2.5f, 0.92f, -0.5f, -2.5f, 0.92f,
            -0.4f, -4.7f, 1.2f, 0.4f, -4.7f, 1.2f, 0.4f, -2.5f, 1.08f, -0.4f, -2.5f, 1.08f);
        r.on(hullI).detail();
        for(int sd = -1; sd <= 1; sd += 2){
            int wb = sd < 0 ? PILOT_WL : PILOT_WR;
            m = r.part(wb);
            hull2(m, s).hexa(true, 0, 2.2f, -0.15f, 0, -3.0f, -0.15f, sd * 5.6f, -5.2f, -0.1f, sd * 5.6f, -3.9f, -0.1f,
                0, 1.8f, 0.25f, 0, -2.8f, 0.25f, sd * 5.6f, -5.0f, 0.05f, sd * 5.6f, -4.0f, 0.05f);
            r.shadow(1 + (sd < 0 ? 0 : 1));
            int w = r.lastIndex();
            m = r.part(wb);
            team(m).taper(sd * 5.6f, -4.5f, 0.05f, 0.3f, 1.3f, 1.4f, 0.15f, 0.7f, 0, -0.5f, false);
            r.on(w);
            m = r.part(wb);
            glow(m, s).cbox(sd * 5.55f, -3.75f, -0.1f, 0.35f, 0.4f, 0.2f);
            r.on(w).detail();
            m = r.part(body);
            steel(m, s).at(sd * 1.25f, -4.3f, 0.15f).rot(0, 90).lathe(10, 0, 0.9f, 0f, 0.95f, 1.6f, 0.75f, 2.2f, 0.001f, 2.2f);
            r.shadowRound(0);
            t.engine(body, sd * 1.25f, -6.6f, 0.15f, 0.95f);
        }
        m = r.part(PILOT_EMIT);
        glow(m, s).at(0, 0, 0).rot(0, -90).lathe(8, 0, 0.42f, 0f, 0.001f, 0.8f);
        t.weapon(0, -1, PILOT_EMIT, 0, 0.8f, 0);
        r.half = 8.4f;
        r.height = 2.6f;
        t.extra = (type, unit, d, delta) -> {
            float sweep = d.speedF * 13f;
            type.rig.rot(PILOT_WL, 2, sweep);
            type.rig.rot(PILOT_WR, 2, -sweep);
            type.rig.glow[PILOT_EMIT] = unit.mining() || unit.activelyBuilding() ? 1.3f + Mathf.absin(Time.time, 3f, 0.3f) : 0.8f;
        };
        return r.finish();
    }

    public static int GLINT_SPIN;

    /** Glint: a lumen dart; two crescent blades barrel-roll around the hull. */
    public static Rig glint(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        GLINT_SPIN = r.bone(body, 0, -0.4f, 0);
        int bl = r.bone(GLINT_SPIN, -2.5f, 0, 0), br = r.bone(GLINT_SPIN, 2.5f, 0, 0);
        int nose = r.bone(body, 0, 5.6f, 0);

        Mesh m = r.part(body);
        hull(m, s).at(0, 0, 0).rot(0, -90).lathe(6, 30f, 0.001f, -4.2f, 1.3f, -2.2f, 1.5f, 0.8f, 0.9f, 3.6f, 0.001f, 5.6f);
        r.shadowRound(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        glow(m, s).at(0, -1.0f, 0).rot(0, -90).ring(6, 1.2f, 1.62f, 0f, 0.35f);
        r.on(hullI);
        m = r.part(body);
        glow2(m, s).at(0, -4.1f, 0).rot(0, 90).lathe(6, 30f, 0.55f, 0f, 0.001f, 0.7f);
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(sd < 0 ? bl : br);
            hull2(m, s).hexa(true, 0, 1.6f, -0.12f, 0, -2.4f, -0.12f, sd * 1.2f, -3.4f, -0.08f, sd * 1.4f, -0.2f, -0.08f,
                0, 1.6f, 0.12f, 0, -2.4f, 0.12f, sd * 1.2f, -3.4f, 0.08f, sd * 1.4f, -0.2f, 0.08f);
            r.shadow(1 + (sd < 0 ? 0 : 1));
            int w = r.lastIndex();
            m = r.part(sd < 0 ? bl : br);
            glow(m, s).hexa(false, sd * 1.25f, -0.4f, -0.1f, sd * 1.45f, -0.2f, -0.1f, sd * 1.25f, -3.2f, -0.1f, sd * 1.05f, -3.3f, -0.1f,
                sd * 1.25f, -0.4f, 0.1f, sd * 1.45f, -0.2f, 0.1f, sd * 1.25f, -3.2f, 0.1f, sd * 1.05f, -3.3f, 0.1f);
            r.on(w);
        }
        t.engine(body, 0, -4.9f, 0, 0.9f);
        t.weapon(0, -1, nose, 0, 0.3f, 0);
        r.half = 6f;
        r.height = 3f;
        t.extra = (type, unit, d, delta) -> type.rig.rot(GLINT_SPIN, 1, d.spin * 7f);
        return r.finish();
    }

    public static int GUARD_EMIT;

    /** Aurora guard: heavy pearl escort; two lance pods track targets, a shield emitter crowns the spine. */
    public static Rig auroraGuard(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        GUARD_EMIT = r.bone(body, 0, -5.5f, 2.6f);
        int[] pod = new int[2], bar = new int[2];
        for(int i = 0; i < 2; i++){
            float sd = i == 0 ? 1 : -1;
            pod[i] = r.bone(body, sd * 9.6f, 1.2f, 0.4f);
            bar[i] = r.bone(pod[i], 0, 1.6f, 0.3f);
        }

        Mesh m = r.part(body);
        hull(m, s).hexa(true, -6f, -9f, -1.2f, 6f, -9f, -1.2f, 3.2f, 10f, -0.8f, -3.2f, 10f, -0.8f,
            -4.6f, -8f, 2.0f, 4.6f, -8f, 2.0f, 2.2f, 8.6f, 1.2f, -2.2f, 8.6f, 1.2f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        dark(m, s).taper(0, 0, -2.2f, 2.0f, 10f, -1.15f, 4.4f, 14f, 0, 0, false);
        m = r.part(body);
        hull2(m, s).taper(0, -1f, 1.85f, 3.0f, 12f, 3.0f, 1.4f, 9f, 0, -0.5f, false);
        r.on(hullI);
        int spine = r.lastIndex();
        m = r.part(body);
        glass(m, canopy).taper(0, 6.2f, 1.3f, 1.8f, 2.6f, 1.9f, 0.9f, 1.6f, 0, -0.3f, false);
        r.on(hullI);
        m = r.part(body);
        team(m).hexa(false, -4.7f, -7.6f, 1.2f, -4.4f, -7.6f, 1.95f, -2.6f, 4.0f, 1.55f, -2.9f, 4.0f, 0.8f,
            -4.9f, -7.6f, 1.2f, -4.6f, -7.6f, 1.95f, -2.8f, 4.0f, 1.55f, -3.1f, 4.0f, 0.8f);
        r.on(hullI).detail();
        m = r.part(body);
        team(m).hexa(false, 4.7f, -7.6f, 1.2f, 4.4f, -7.6f, 1.95f, 2.6f, 4.0f, 1.55f, 2.9f, 4.0f, 0.8f,
            4.9f, -7.6f, 1.2f, 4.6f, -7.6f, 1.95f, 2.8f, 4.0f, 1.55f, 3.1f, 4.0f, 0.8f);
        r.on(hullI).detail();
        //shield emitter
        m = r.part(GUARD_EMIT);
        trim(m, s).at(0, 0, 0).lathe(12, 0, 1.6f, 0f, 2.4f, 0.6f, 2.2f, 0.9f, 0.001f, 0.7f);
        r.on(spine);
        int dish = r.lastIndex();
        m = r.part(GUARD_EMIT);
        glow2(m, s).at(0, 0, 0.7f).lathe(6, 30f, 0.001f, 0f, 0.8f, 0.8f, 0.001f, 2.4f);
        r.on(dish);
        for(int i = 0; i < 2; i++){
            float sd = i == 0 ? 1 : -1;
            m = r.part(body);
            hull2(m, s).hexa(true, sd * 4.4f, -4f, -0.3f, sd * 4.4f, 4f, -0.3f, sd * 8.6f, 2.4f, 0f, sd * 8.6f, -2.8f, 0f,
                sd * 4.2f, -3.6f, 1.0f, sd * 4.2f, 3.4f, 0.9f, sd * 8.6f, 2.0f, 0.5f, sd * 8.6f, -2.4f, 0.5f);
            r.shadow(1 + i);
            m = r.part(pod[i]);
            hull(m, s).cbevel(0, 0, -0.9f, 2.6f, 5.0f, 2.0f, 0.3f);
            r.shadow(3 + i);
            int p = r.lastIndex();
            m = r.part(pod[i]);
            glow(m, s).box(-1.32f, -1.8f, -0.2f, -1.3f, 1.4f, 0.3f);
            r.on(p).detail();
            m = r.part(bar[i]);
            steel(m, s).at(0, 0, 0).rot(0, -90).cyl(8, 0.45f, 0f, 5.2f);
            m = r.part(bar[i]);
            trim(m, s).at(0, 2.0f, 0).rot(0, -90).cyl(8, 0.8f, 0f, 0.6f);
            m = r.part(bar[i]);
            glow2(m, s).at(0, 5.2f, 0).rot(0, -90).lathe(6, 0, 0.32f, 0f, 0.001f, 0.6f);
            t.weapon(i, pod[i], bar[i], 0, 5.8f, 0);
        }
        for(int k = 0; k < 4; k++){
            float x = -3.3f + k * 2.2f;
            m = r.part(body);
            steel(m, s).at(x, -8.6f, 0.4f).rot(0, 90).lathe(10, 0, 0.9f, 0f, 0.95f, 1.4f, 0.8f, 2.0f, 0.001f, 2.0f);
            r.shadowRound(0);
            t.engine(body, x, -10.8f, 0.4f, 1.0f);
        }
        r.half = 12f;
        r.height = 4.6f;
        t.extra = (type, unit, d, delta) -> {
            type.rig.rot(GUARD_EMIT, 2, d.spin * 2f);
            type.rig.glow[GUARD_EMIT] = 0.8f + 0.4f * Mathf.clamp(unit.shield / 950f) + Mathf.absin(Time.time, 10f, 0.2f);
        };
        return r.finish();
    }

    // =====================================================================================================
    // Singularity air wing
    // =====================================================================================================

    /** Lensing: a saucer scout with an amber accretion rim and two fixed spray guns. */
    public static Rig lensing(Ship3D t){
        Style s = sing;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int gl = r.bone(body, -4.5f, 2f, -0.2f), gr = r.bone(body, 4.5f, 2f, -0.2f);
        Mesh m = r.part(body);
        hull(m, s).at(0, 0, 0).lathe(16, 0, 0.001f, -0.8f, 4.6f, -0.4f, 5.6f, 0.1f, 4.2f, 0.8f, 1.6f, 1.4f, 0.001f, 1.5f);
        r.shadowRound(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        glow(m, s).at(0, 0, 0).ring(16, 5.4f, 5.75f, -0.05f, 0.2f);
        r.on(hullI);
        m = r.part(body);
        glass(m, canopySing).at(0, 0.6f, 1.25f).lathe(10, 0, 1.3f, 0f, 1.0f, 0.5f, 0.001f, 0.75f);
        r.on(hullI);
        m = r.part(body);
        glow2(m, s).at(0, -2.6f, 1.05f).cyl(8, 0.6f, 0f, 0.12f);
        r.on(hullI).detail();
        for(int sd = -1; sd <= 1; sd += 2){
            int g = sd < 0 ? gl : gr;
            m = r.part(g);
            dark(m, s).at(0, -1.2f, 0).rot(0, -90).cyl(8, 0.7f, 0f, 1.6f);
            m = r.part(g);
            steel(m, s).at(0, 0.4f, 0).rot(0, -90).cyl(6, 0.35f, 0f, 2.4f);
        }
        m = r.part(body);
        steel(m, s).at(0, -4.8f, 0.2f).rot(0, 90).lathe(10, 0, 1.0f, 0f, 1.0f, 1.2f, 0.7f, 1.8f, 0.001f, 1.8f);
        t.engine(body, 0, -6.7f, 0.2f, 1.1f);
        t.weapon(0, -1, gr, 0, 2.9f, 0);
        t.weapon(1, -1, gl, 0, 2.9f, 0);
        r.half = 7f;
        r.height = 2.2f;
        return r.finish();
    }

    public static int ERGO_CORE, ERGO_RING, ERGO_FIN_L, ERGO_FIN_R;

    /**
     * Ergosphere (v7.1 refinement): heavy gunship. Layered hull with a pointed nose and a split dorsal spine, the
     * violet ergosphere core cradled mid-ship with a tilted precessing ring, swept sponsons carrying two armoured
     * twin-barrel turrets (barrels at x +-0.7 of the turret, muzzles at y 5.4), wing-tip fins that bank with
     * the roll, underwing nacelles and three main engines.
     */
    public static Rig ergosphere(Ship3D t){
        Style s = sing;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        ERGO_CORE = r.bone(body, 0, -1f, 2.2f);
        ERGO_RING = r.bone(ERGO_CORE, 0, 0, 0.9f);
        int[] tur = new int[2], bar = new int[2];
        for(int i = 0; i < 2; i++){
            float sd = i == 0 ? 1 : -1;
            tur[i] = r.bone(body, sd * 10.6f, 0.4f, 0.9f);
            bar[i] = r.bone(tur[i], 0, 1.2f, 0.9f);
        }
        //main hull
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -7f, -11f, -1.4f, 7f, -11f, -1.4f, 4.2f, 10.6f, -1.0f, -4.2f, 10.6f, -1.0f,
            -5.4f, -10f, 2.0f, 5.4f, -10f, 2.0f, 2.9f, 9.6f, 1.4f, -2.9f, 9.6f, 1.4f);
        r.shadow(0);
        int hullI = r.lastIndex();
        //nose
        m = r.part(body);
        hull2(m, s).hexa(true, -4.2f, 10.6f, -1.0f, 4.2f, 10.6f, -1.0f, 1.0f, 14.6f, -0.3f, -1.0f, 14.6f, -0.3f,
            -2.9f, 9.6f, 1.4f, 2.9f, 9.6f, 1.4f, 0.6f, 14.2f, 0.4f, -0.6f, 14.2f, 0.4f);
        r.shadow(0);
        //belly keel
        m = r.part(body);
        dark(m, s).hexa(true, -3.4f, -9.6f, -2.2f, 3.4f, -9.6f, -2.2f, 2.0f, 8.6f, -1.8f, -2.0f, 8.6f, -1.8f,
            -5.0f, -10f, -1.4f, 5.0f, -10f, -1.4f, 3.2f, 9.4f, -1.0f, -3.2f, 9.4f, -1.0f);
        r.under(hullI, 1);
        //split dorsal spine (front and rear of the core)
        m = r.part(body);
        hull2(m, s).bevel(-1.3f, 2.6f, 1.6f, 1.3f, 8.4f, 2.7f, 0.3f);
        r.on(hullI);
        m = r.part(body);
        hull2(m, s).bevel(-1.6f, -9.6f, 1.8f, 1.6f, -4.4f, 2.9f, 0.3f);
        r.on(hullI);
        int rearSpine = r.lastIndex();
        m = r.part(body);
        glass(m, canopySing).taper(0, 6.4f, 2.65f, 1.8f, 2.4f, 3.25f, 1.0f, 1.4f, 0, -0.3f, false);
        r.on(hullI);
        //side light strips
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(body);
            glow2(m, s).box(sd * 4.05f - 0.12f, 2.0f, 0.2f, sd * 4.05f + 0.12f, 8.6f, 0.6f);
            r.on(hullI).detail();
        }
        //ergosphere core: dark cradle, glowing sphere, tilted ring (precesses)
        m = r.part(ERGO_CORE);
        dark(m, s).at(0, 0, 0).lathe(10, 0, 3.0f, 0f, 2.8f, 0.7f, 2.0f, 1.1f);
        r.on(hullI);
        int cradle = r.lastIndex();
        m = r.part(ERGO_CORE);
        glow2(m, s).at(0, 0, 0.6f).lathe(10, 0, 0.001f, 0f, 1.25f, 0.45f, 1.35f, 1.0f, 0.9f, 1.6f, 0.001f, 1.85f);
        r.on(cradle);
        m = r.part(ERGO_RING);
        trim(m, s).at(0, 0, 0).ring(16, 2.3f, 2.75f, -0.14f, 0.14f);
        r.on(cradle);
        //team stripe on the rear spine
        m = r.part(body);
        team(m).box(-0.7f, -9.2f, 2.9f, 0.7f, -5.0f, 2.96f);
        r.on(rearSpine).detail();

        for(int i = 0; i < 2; i++){
            float sd = i == 0 ? 1 : -1;
            //swept sponson
            m = r.part(body);
            hull2(m, s).hexa(true, sd * 5.6f, -5f, -0.8f, sd * 5.6f, 5f, -0.8f, sd * 12.8f, 2.6f, -0.4f, sd * 12.8f, -4.4f, -0.4f,
                sd * 5.4f, -4.6f, 1.0f, sd * 5.4f, 4.4f, 1.0f, sd * 12.8f, 2.2f, 0.8f, sd * 12.8f, -4.0f, 0.8f);
            r.shadow(1 + i);
            int spon = r.lastIndex();
            m = r.part(body);
            team(m).box(sd * 7.0f - 0.5f, -3.8f, 1.0f, sd * 7.0f + 0.5f, -1.4f, 1.06f);
            r.on(spon).detail();
            //underwing nacelle
            m = r.part(body);
            steel(m, s).at(sd * 8.6f, -1.4f, -1.2f).rot(0, 90).lathe(8, 22.5f, 1.0f, 0f, 1.1f, 2.6f, 0.9f, 4.2f, 0.001f, 4.4f);
            r.shadowRound(0);
            t.engine(body, sd * 8.6f, -5.8f, -1.2f, 0.9f);
            //wing-tip fin (banks with the roll)
            int fin = r.bone(body, sd * 12.8f, -1.2f, 0.8f);
            if(i == 0) ERGO_FIN_R = fin; else ERGO_FIN_L = fin;
            m = r.part(fin);
            trim(m, s).hexa(true, -0.18f, -3.0f, 0f, 0.18f, -3.0f, 0f, 0.18f, 2.8f, 0f, -0.18f, 2.8f, 0f,
                -0.12f, -2.6f, 2.2f, 0.12f, -2.6f, 2.2f, 0.12f, -0.8f, 2.2f, -0.12f, -0.8f, 2.2f);
            m = r.part(fin);
            glow(m, s).box(-0.22f, -2.5f, 1.7f, 0.22f, -1.0f, 2.0f);
            r.detail();

            //turret: base ring, armoured mantlet, twin barrels with shrouds and brakes
            m = r.part(tur[i]);
            dark(m, s).at(0, 0, 0).lathe(8, 22.5f, 2.3f, 0f, 2.1f, 0.8f, 1.5f, 1.2f);
            int tu = r.lastIndex();
            m = r.part(tur[i]);
            hull(m, s).bevel(-1.6f, -1.8f, 0.8f, 1.6f, 1.6f, 1.9f, 0.35f);
            r.on(tu);
            int mant = r.lastIndex();
            m = r.part(tur[i]);
            glow(m, s).box(-0.3f, -1.84f, 1.1f, 0.3f, -1.78f, 1.6f);
            r.on(mant).detail();
            for(int b = -1; b <= 1; b += 2){
                m = r.part(bar[i]);
                steel(m, s).at(b * 0.7f, 0, 0).rot(0, -90).cyl(8, 0.34f, 0f, 4.9f);
                m = r.part(bar[i]);
                dark(m, s).at(b * 0.7f, 0, 0).rot(0, -90).cyl(8, 0.5f, 0f, 1.5f);
                r.detail();
                m = r.part(bar[i]);
                trim(m, s).at(b * 0.7f, 4.6f, 0).rot(0, -90).cyl(8, 0.44f, 0f, 0.7f);
                r.detail();
            }
            t.weapon(i, tur[i], bar[i], 0, 5.4f, 0);
        }
        for(int k = 0; k < 3; k++){
            float x = -3.2f + k * 3.2f;
            m = r.part(body);
            steel(m, s).at(x, -10.6f, 0.4f).rot(0, 90).lathe(10, 0, 1.2f, 0f, 1.25f, 1.6f, 1.0f, 2.4f, 0.001f, 2.4f);
            r.shadowRound(0);
            t.engine(body, x, -13.2f, 0.4f, 1.3f);
        }
        r.half = 14f;
        r.height = 5.2f;
        r.stackSort = true;
        t.extra = (type, unit, d, delta) -> {
            type.rig.rot(ERGO_CORE, 2, d.spin * 3f);
            type.rig.rot(ERGO_RING, 0, 24f + Mathf.sin(d.spin, 40f, 6f));
            type.rig.rot(ERGO_RING, 2, d.spin * 7f);
            type.rig.glow[ERGO_CORE] = 0.85f + Mathf.absin(d.spin, 9f, 0.3f);
            //fins lean into the turn / strafe
            type.rig.rot(ERGO_FIN_R, 1, -d.roll * 0.8f);
            type.rig.rot(ERGO_FIN_L, 1, -d.roll * 0.8f);
        };
        return r.finish();
    }
    // =====================================================================================================
    // Singularity ground vehicles
    // =====================================================================================================

    public static int ACC_EMIT;

    /** Accretor: tracked repair tank; domed turret with a degenerate-shell cannon, a spinning repair emitter mast. */
    public static Rig accretor(Tank3D t){
        Style s = sing;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int tur = r.bone(body, 0, -1f, 5.6f), bar = r.bone(tur, 0, 2.2f, 1.2f);
        ACC_EMIT = r.bone(body, 0, -7.6f, 5.6f);
        for(int sd = -1; sd <= 1; sd += 2){
            t.track(r, body, sd * 7.8f, 15f, 2.0f, 3.2f, 16, 5, s);
            Mesh m = r.part(body);
            hull2(m, s).taper(sd * 7.8f, 0, 4.3f, 3.8f, 20f, 5.0f, 3.3f, 19f, 0, 0, true);
            r.shadow(0);
            int f = r.lastIndex();
            m = r.part(body);
            team(m).box(sd * 7.8f - 0.9f, -8f, 4.98f, sd * 7.8f + 0.9f, -3.5f, 5.08f);
            r.on(f).detail();
            m = r.part(body);
            glow(m, s).box(sd * 4.2f - 0.6f, 8.4f, 2.4f, sd * 4.2f + 0.6f, 8.9f, 3.2f);
            r.detail();
        }
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -5.8f, -10f, 1.0f, 5.8f, -10f, 1.0f, 5.8f, 10f, 1.0f, -5.8f, 10f, 1.0f,
            -5.4f, -9.2f, 5.6f, 5.4f, -9.2f, 5.6f, 4.6f, 6.5f, 5.6f, -4.6f, 6.5f, 5.6f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(tur);
        hull2(m, s).at(0, 0, 0).lathe(10, 0, 3.6f, 0f, 3.4f, 1.2f, 2.2f, 2.2f, 0.001f, 2.4f);
        int dome = r.lastIndex();
        m = r.part(tur);
        glow2(m, s).at(-1.2f, -1.6f, 1.95f).cyl(8, 0.5f, 0f, 0.3f);
        r.on(dome).detail();
        m = r.part(bar);
        dark(m, s).cbox(0, 0.2f, 0, 1.8f, 1.4f, 1.4f);
        m = r.part(bar);
        steel(m, s).at(0, 0, 0).rot(0, -90).cyl(8, 0.7f, 0f, 6.4f);
        m = r.part(bar);
        hull(m, s).at(0, 5.4f, 0).rot(0, -90).cyl(8, 1.0f, 0f, 1.4f);
        t.weapon(0, tur, bar, 0, 7f, 0);
        m = r.part(ACC_EMIT);
        trim(m, s).at(0, 0, 0).cyl(6, 0.5f, 0f, 2.6f);
        r.on(hullI);
        m = r.part(ACC_EMIT);
        glow(m, s).at(0, 0, 2.6f).lathe(8, 0, 0.3f, 0f, 1.9f, 0.2f, 1.7f, 0.45f, 0.001f, 0.55f);
        r.half = 11f;
        r.stackSort = true;
        r.height = 9.5f;
        t.extra = (type, unit, d, delta) -> {
            type.rig.rot(ACC_EMIT, 2, d.spin * 3f);
            type.rig.glow[ACC_EMIT] = 0.8f + Mathf.absin(Time.time + unit.id, 12f, 0.5f);
        };
        return r.finish();
    }

    public static int CIT_ARRAY, CIT_CORE, CIT_VENT_L, CIT_VENT_R, CIT_HATCH_L, CIT_HATCH_R;

    /**
     * Citadel: titan-class mobile platform. Two broad tread units, a stepped hull, the collapsar main gun in a
     * heavy turret whose vents fan open while it charges, two AA mounts and two howitzers on the fenders, and a
     * rotating constraint array over the rear deck; the rear hatches open with the main gun's warmup.
     */
    public static Rig citadel(Tank3D t){
        Style s = sing;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int main = r.bone(body, 0, 4f, 13f), mbar = r.bone(main, 0, 4f, 2.4f);
        CIT_VENT_L = r.bone(main, -6.4f, -2.5f, 1.6f);
        CIT_VENT_R = r.bone(main, 6.4f, -2.5f, 1.6f);
        CIT_ARRAY = r.bone(body, 0, -17f, 10f);
        CIT_CORE = r.bone(body, 0, -17f, 10f);
        CIT_HATCH_L = r.bone(body, -13.5f, -13f, 10f);
        CIT_HATCH_R = r.bone(body, 13.5f, -13f, 10f);
        int[] aa = new int[2], aab = new int[2], sd2 = new int[2], sdb = new int[2];
        for(int i = 0; i < 2; i++){
            float sd = i == 0 ? 1 : -1;
            aa[i] = r.bone(body, sd * 22f, 15f, 8.6f);
            aab[i] = r.bone(aa[i], 0, 1.2f, 1.6f);
            sd2[i] = r.bone(body, sd * 22f, -17f, 8.6f);
            sdb[i] = r.bone(sd2[i], 0, 1.8f, 1.8f);
        }
        for(int k = 0; k < 2; k++){
            float sd = k == 0 ? -1 : 1;
            t.track(r, body, sd * 22f, 44f, 3.6f, 8f, 26, 7, s);
            Mesh m = r.part(body);
            hull2(m, s).taper(sd * 22f, 0, 7.6f, 10f, 53f, 8.6f, 9.4f, 51f, 0, 0, true);
            r.shadow(0);
            int f = r.lastIndex();
            m = r.part(body);
            team(m).box(sd * 22f - 4.6f, -4f, 8.55f, sd * 22f + 4.6f, 4f, 8.65f);
            r.on(f).detail();
            m = r.part(body);
            glow(m, s).box(sd * 22f - 2.5f, 26.4f, 5.2f, sd * 22f + 2.5f, 26.9f, 6.4f);
            r.detail();
        }
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -17f, -26f, 2f, 17f, -26f, 2f, 17f, 24f, 2f, -17f, 24f, 2f,
            -16f, -24f, 10f, 16f, -24f, 10f, 14f, 17f, 10f, -14f, 17f, 10f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        hull2(m, s).taper(0, 2f, 10f, 22f, 24f, 12.4f, 18f, 20f, 0, 0, false);
        r.on(hullI);
        int deck = r.lastIndex();
        m = r.part(body);
        glass(m, Color.valueOf("c28a4a")).taper(0, 16.4f, 9.2f, 10f, 2.6f, 10.4f, 8f, 1.4f, 0, -0.4f, false);
        r.on(hullI);
        //main turret
        m = r.part(main);
        hull(m, s).at(0, 0, 0).lathe(8, 22.5f, 8f, -0.6f, 7.6f, 2.4f, 5f, 4f, 0.001f, 4.2f);
        r.on(deck);
        int tu = r.lastIndex();
        m = r.part(main);
        team(m).at(0, -2f, 4.05f).cyl(8, 1.6f, 0f, 0.25f);
        r.on(tu).detail();
        m = r.part(mbar);
        dark(m, s).cbox(0, 0.5f, 0, 4.4f, 3f, 3.2f);
        m = r.part(mbar);
        steel(m, s).at(0, 0, 0).rot(0, -90).cyl(10, 1.8f, 0f, 14f);
        for(float y : new float[]{6f, 10f}){
            m = r.part(mbar);
            glow2(m, s).at(0, y, 0).rot(0, -90).cyl(10, 2.3f, 0f, 0.8f);
        }
        m = r.part(mbar);
        dark(m, s).at(0, 13.2f, 0).rot(0, -90).lathe(10, 0, 2.5f, 0f, 2.6f, 1.6f, 2.0f, 2.2f, 0.001f, 2.2f);
        t.weapon(0, main, mbar, 0, 16f, 0);
        for(int k = 0; k < 2; k++){
            float sd = k == 0 ? -1 : 1;
            m = r.part(k == 0 ? CIT_VENT_L : CIT_VENT_R);
            trim(m, s).hexa(true, 0, -2.4f, 0, sd * 0.6f, -2.4f, 0, sd * 0.6f, 2.4f, 0, 0, 2.4f, 0,
                0, -2.0f, 2.2f, sd * 0.4f, -2.0f, 2.2f, sd * 0.4f, 1.6f, 2.2f, 0, 1.6f, 2.2f);
            m = r.part(k == 0 ? CIT_HATCH_L : CIT_HATCH_R);
            hull2(m, s).box(-2.4f, -4f, 0f, 2.4f, 4f, 0.5f);
            m = r.part(k == 0 ? CIT_HATCH_L : CIT_HATCH_R);
            glow(m, s).box(-1.6f, -3.2f, -0.02f, 1.6f, 3.2f, 0.01f);
            r.detail();
        }
        //constraint array + core
        m = r.part(CIT_CORE);
        dark(m, s).at(0, 0, 0).lathe(8, 22.5f, 4.2f, 0f, 3.8f, 1.2f, 0.001f, 1.2f);
        r.on(hullI);
        int base = r.lastIndex();
        m = r.part(CIT_CORE);
        glow2(m, s).at(0, 0, 1.2f).lathe(8, 0, 0.001f, -0.4f, 2.0f, 0.6f, 1.6f, 1.8f, 0.001f, 2.4f);
        r.on(base);
        for(int k = 0; k < 3; k++){
            float a = k * 120f;
            m = r.part(CIT_ARRAY);
            trim(m, s).at(Mathf.cosDeg(a) * 4.8f, Mathf.sinDeg(a) * 4.8f, 0).lathe(6, 0, 0.9f, 0f, 0.7f, 3.2f, 0.001f, 3.8f);
            m = r.part(CIT_ARRAY);
            glow(m, s).at(Mathf.cosDeg(a) * 4.8f, Mathf.sinDeg(a) * 4.8f, 3.0f).lathe(6, 30f, 0.001f, 0f, 0.6f, 0.5f, 0.001f, 1.3f);
        }
        //AA mounts (1, 2) and howitzers (3, 4) on the fenders
        for(int i = 0; i < 2; i++){
            m = r.part(aa[i]);
            dark(m, s).at(0, 0, 0).lathe(8, 22.5f, 2.6f, 0f, 2.4f, 1.4f, 1.4f, 2.2f, 0.001f, 2.2f);
            int d = r.lastIndex();
            m = r.part(aa[i]);
            glow(m, s).at(0, 0, 2.15f).ring(8, 0.8f, 1.3f, 0f, 0.1f);
            r.on(d).detail();
            for(int b = -1; b <= 1; b += 2){
                m = r.part(aab[i]);
                steel(m, s).at(b * 0.75f, 0, 0).rot(0, -90).cyl(6, 0.35f, 0f, 5f);
            }
            t.weapon(1 + i, aa[i], aab[i], 0, 5.4f, 0);
        }
        for(int i = 0; i < 2; i++){
            m = r.part(sd2[i]);
            hull(m, s).cbevel(0, 0, 0, 5f, 5.6f, 2.6f, 0.5f);
            m = r.part(sdb[i]);
            steel(m, s).at(0, 0, 0).rot(0, -90).cyl(8, 0.9f, 0f, 6f);
            m = r.part(sdb[i]);
            dark(m, s).at(0, 5f, 0).rot(0, -90).cyl(8, 1.2f, 0f, 1.4f);
            t.weapon(3 + i, sd2[i], sdb[i], 0, 6.6f, 0);
        }
        r.half = 30f;
        r.stackSort = true;
        r.height = 20f;
        t.extra = (type, unit, d, delta) -> {
            Rig g = type.rig;
            g.rot(CIT_ARRAY, 2, d.spin * 2.4f);
            g.glow[CIT_CORE] = 0.7f + Mathf.absin(Time.time + unit.id, 28f, 0.6f);
            float w = d.warm;
            g.rot(CIT_VENT_L, 1, -w * 30f);
            g.rot(CIT_VENT_R, 1, w * 30f);
            g.move(CIT_HATCH_L, -w * 1.2f, 0, w * 0.6f);
            g.rot(CIT_HATCH_L, 1, -w * 24f);
            g.move(CIT_HATCH_R, w * 1.2f, 0, w * 0.6f);
            g.rot(CIT_HATCH_R, 1, w * 24f);
            g.glow[CIT_HATCH_L] = g.glow[CIT_HATCH_R] = 0.5f + w;
        };
        return r.finish();
    }

    // =====================================================================================================
    // Aurelia v7.1: ground and naval line units
    // =====================================================================================================

    /** Shard rover: light scout tank; low wedge hull on two tracks, a small dome turret with one shard gun. */
    public static Rig shardRover(Tank3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int tur = r.bone(body, 0, -0.6f, 3.3f), bar = r.bone(tur, 0, 1.2f, 0.9f);
        for(int sd = -1; sd <= 1; sd += 2){
            t.track(r, body, sd * 4.3f, 9.6f, 1.3f, 2.0f, 12, 4, s);
            Mesh m = r.part(body);
            hull2(m, s).taper(sd * 4.3f, 0, 2.7f, 2.4f, 11.6f, 3.2f, 2.0f, 10.8f, 0, 0, true);
            r.shadow(0);
        }
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -3.3f, -5.4f, 0.8f, 3.3f, -5.4f, 0.8f, 3.3f, 5.6f, 0.8f, -3.3f, 5.6f, 0.8f,
            -3.0f, -4.9f, 3.3f, 3.0f, -4.9f, 3.3f, 2.4f, 3.2f, 3.3f, -2.4f, 3.2f, 3.3f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        glow(m, s).box(-2.0f, 5.62f, 1.6f, 2.0f, 5.7f, 2.1f);
        r.on(hullI).detail();
        m = r.part(body);
        team(m).box(-1.2f, -4.6f, 3.31f, 1.2f, -3.2f, 3.36f);
        r.on(hullI).detail();
        m = r.part(tur);
        hull2(m, s).at(0, 0, 0).lathe(8, 22.5f, 2.3f, 0f, 2.2f, 0.9f, 1.4f, 1.6f, 0.001f, 1.7f);
        int dome = r.lastIndex();
        m = r.part(tur);
        glow(m, s).at(0.9f, -0.9f, 1.35f).cyl(6, 0.3f, 0f, 0.2f);
        r.on(dome).detail();
        m = r.part(bar);
        steel(m, s).at(0, 0, 0).rot(0, -90).cyl(8, 0.4f, 0f, 4.2f);
        m = r.part(bar);
        dark(m, s).at(0, 3.6f, 0).rot(0, -90).cyl(8, 0.6f, 0f, 0.8f);
        t.weapon(0, tur, bar, 0, 4.5f, 0);
        r.half = 7f;
        r.height = 5.4f;
        r.stackSort = true;
        return r.finish();
    }

    /** Aurite bulwark: heavy line tank; broad armoured hull, side skirts, a twin-barrel turret (barrels at x +-1.1). */
    public static Rig auriteBulwark(Tank3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int tur = r.bone(body, 0, -1.4f, 5.4f), bar = r.bone(tur, 0, 2.6f, 1.4f);
        for(int sd = -1; sd <= 1; sd += 2){
            t.track(r, body, sd * 7.2f, 16f, 2.0f, 3.2f, 16, 5, s);
            Mesh m = r.part(body);
            hull2(m, s).taper(sd * 7.4f, 0, 4.2f, 3.8f, 19f, 5.0f, 3.2f, 18f, 0, 0, true);
            r.shadow(0);
            int f = r.lastIndex();
            m = r.part(body);
            trim(m, s).box(sd * 7.4f - 1.5f, -8.6f, 4.22f, sd * 7.4f + 1.5f, -8.0f, 4.9f);
            r.on(f).detail();
        }
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -5.6f, -9.4f, 1.0f, 5.6f, -9.4f, 1.0f, 5.6f, 9.6f, 1.0f, -5.6f, 9.6f, 1.0f,
            -5.2f, -8.8f, 5.4f, 5.2f, -8.8f, 5.4f, 4.4f, 6.2f, 5.4f, -4.4f, 6.2f, 5.4f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        glow2(m, s).box(-3.0f, 9.62f, 2.4f, 3.0f, 9.7f, 3.0f);
        r.on(hullI).detail();
        m = r.part(body);
        team(m).box(-2.0f, -8.4f, 5.41f, 2.0f, -6.4f, 5.46f);
        r.on(hullI).detail();
        m = r.part(tur);
        hull2(m, s).at(0, 0, 0).lathe(8, 22.5f, 4.2f, 0f, 4.0f, 1.3f, 3.0f, 2.4f, 0.001f, 2.5f);
        int dome = r.lastIndex();
        m = r.part(tur);
        hull(m, s).at(0, 0, 0).cbevel(0, 1.6f, 0.6f, 4.0f, 3.4f, 1.8f, 0.2f);
        r.on(dome);
        m = r.part(tur);
        glow(m, s).at(-2.2f, -1.8f, 2.4f).cyl(6, 0.4f, 0f, 0.25f);
        r.on(dome).detail();
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(bar);
            steel(m, s).at(sd * 1.1f, 0, 0).rot(0, -90).cyl(8, 0.55f, 0f, 7.2f);
            m = r.part(bar);
            dark(m, s).at(sd * 1.1f, 6.2f, 0).rot(0, -90).cyl(8, 0.8f, 0f, 1.2f);
        }
        t.weapon(0, tur, bar, 0, 7.4f, 0);
        r.half = 12f;
        r.height = 8.5f;
        r.stackSort = true;
        return r.finish();
    }

    /** Tide skiff: small patrol boat; pearl hull with a raised bow, cabin and a stern jet. */
    public static Rig tideSkiff(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int tur = r.bone(body, 0, 2.2f, 1.9f), bar = r.bone(tur, 0, 0.8f, 0.6f);
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -2.2f, -5.8f, -0.8f, 2.2f, -5.8f, -0.8f, 0.6f, 6.6f, -0.3f, -0.6f, 6.6f, -0.3f,
            -2.8f, -6.2f, 1.2f, 2.8f, -6.2f, 1.2f, 0.4f, 7.4f, 1.5f, -0.4f, 7.4f, 1.5f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        hull2(m, s).at(0, 0, 0).cbevel(0, -1.8f, 1.2f, 3.0f, 3.2f, 1.6f, 0.25f);
        r.on(hullI);
        int cab = r.lastIndex();
        m = r.part(body);
        glass(m, canopy).box(-1.3f, -0.22f, 1.7f, 1.3f, -0.18f, 2.5f);
        r.on(cab).detail();
        m = r.part(body);
        team(m).box(-2.81f, -4.8f, 0.4f, -2.76f, -2.2f, 0.9f);
        r.on(hullI).detail();
        m = r.part(tur);
        dark(m, s).at(0, 0, 0).cyl(8, 1.0f, 0f, 0.7f);
        m = r.part(bar);
        steel(m, s).at(0, 0, 0).rot(0, -90).cyl(6, 0.3f, 0f, 2.6f);
        t.weapon(0, tur, bar, 0, 2.8f, 0);
        t.engine(body, 0, -6.4f, 0.1f, 0.8f);
        r.half = 7.4f;
        r.height = 3f;
        return r.finish();
    }

    /** Tide warden: naval frigate; long hull, bridge tower, two twin-gun turrets fore and aft. */
    public static Rig tideWarden(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int t0 = r.bone(body, 0, 6.0f, 2.2f), b0 = r.bone(t0, 0, 1.2f, 0.8f);
        int t1 = r.bone(body, 0, -6.4f, 2.2f), b1 = r.bone(t1, 0, 1.2f, 0.8f);
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -3.6f, -11f, -1.2f, 3.6f, -11f, -1.2f, 1.0f, 12f, -0.6f, -1.0f, 12f, -0.6f,
            -4.4f, -11.6f, 1.6f, 4.4f, -11.6f, 1.6f, 0.6f, 13.4f, 2.0f, -0.6f, 13.4f, 2.0f);
        r.shadow(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        hull2(m, s).at(0, 0, 0).cbevel(0, -0.6f, 1.6f, 4.4f, 5.6f, 2.6f, 0.3f);
        r.on(hullI);
        int bridge = r.lastIndex();
        m = r.part(body);
        glass(m, canopy).box(-1.8f, 2.19f, 3.0f, 1.8f, 2.23f, 3.9f);
        r.on(bridge).detail();
        m = r.part(body);
        steel(m, s).at(0, -1.4f, 4.2f).cyl(6, 0.3f, 0f, 2.4f);
        r.on(bridge);
        m = r.part(body);
        glow(m, s).at(0, -1.4f, 6.6f).cyl(6, 0.45f, 0f, 0.3f);
        r.on(bridge).detail();
        m = r.part(body);
        team(m).box(-4.41f, -9.0f, 0.6f, -4.36f, -5.0f, 1.2f);
        r.on(hullI).detail();
        int[][] turrets = {{t0, b0}, {t1, b1}};
        for(int[] tb : turrets){
            m = r.part(tb[0]);
            hull2(m, s).at(0, 0, 0).lathe(8, 22.5f, 1.9f, 0f, 1.8f, 0.8f, 1.2f, 1.3f, 0.001f, 1.35f);
            for(int sd = -1; sd <= 1; sd += 2){
                m = r.part(tb[1]);
                steel(m, s).at(sd * 0.55f, 0, 0).rot(0, -90).cyl(6, 0.3f, 0f, 3.2f);
            }
        }
        t.weapon(0, t0, b0, 0, 3.4f, 0);
        t.weapon(1, t1, b1, 0, 3.4f, 0);
        t.engine(body, -1.6f, -11.8f, 0.2f, 0.9f);
        t.engine(body, 1.6f, -11.8f, 0.2f, 0.9f);
        r.half = 13.4f;
        r.height = 7f;
        r.stackSort = true;
        return r.finish();
    }
}
