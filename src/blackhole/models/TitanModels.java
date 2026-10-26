package blackhole.models;

import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import blackhole.g3d.*;
import blackhole.g3d.Kit.*;

import static blackhole.g3d.Kit.*;

/**
 * 3D rigs of the two imported titans (v7.1), drawn by {@link Walker3D}.
 *
 * <ul>
 * <li>Aegis Titan: six legs on a hexagonal armoured hull (Serpulo palette, mint glow), a fixed twin-rail lance on
 * a low dome and two rotating missile pods on the shoulders.</li>
 * <li>Pyroclast Titan: four heavy legs on a squat octagonal furnace hull (Erekir palette, ember glow), a fixed
 * rotating twin-cannon turret, two rotating flame sweepers and the ember crown ring.</li>
 * </ul>
 *
 * Plain, readable shapes: every part is one convex solid; greebles are flagged as detail and dropped at low zoom.
 */
public final class TitanModels{
    private TitanModels(){}

    public static int AEGIS_LANCE, AEGIS_EYE, PYRO_CROWN, PYRO_TURRET, PYRO_CORE;

    /** one leg: femur and tibia segments hang from their bone along -z, the foot pad sits on the ground */
    static void leg(Walker3D t, Rig r, int root, Style s, float restDeg, float mountR, float hipZ, float a, float b, float thick){
        int femur = r.bone(root, 0, 0, 0), tibia = r.bone(root, 0, 0, 0), foot = r.bone(root, 0, 0, 0);
        //femur: armoured beam, wider at the hip
        Mesh m = r.part(femur);
        hull2(m, s).taper(0, 0, -a, thick * 1.5f, thick * 1.2f, 0, thick * 2.3f, thick * 1.7f, 0, 0, true);
        int fem = r.lastIndex();
        //actuator along the femur's back
        m = r.part(femur);
        steel(m, s).at(0, thick * 0.95f, 0).cyl(6, thick * 0.32f, -a * 0.8f, -a * 0.12f);
        r.detail();
        m = r.part(femur);
        glow(m, s).box(-thick * 0.25f, -thick * 0.9f, -a * 0.62f, thick * 0.25f, -thick * 0.84f, -a * 0.34f);
        r.on(fem).detail();
        //knee ball on the tibia origin
        m = r.part(tibia);
        steel(m, s).at(0, 0, 0).lathe(8, 22.5f, 0.001f, -thick * 1.05f, thick * 1.05f, -thick * 0.55f, thick * 1.05f, thick * 0.55f, 0.001f, thick * 1.05f);
        //tibia: tapering spike
        m = r.part(tibia);
        dark(m, s).taper(0, 0, -b, thick * 0.55f, thick * 0.55f, -thick * 0.6f, thick * 1.5f, thick * 1.2f, 0, 0, true);
        int tib = r.lastIndex();
        m = r.part(tibia);
        trim(m, s).box(-thick * 0.62f, -thick * 0.2f, -b * 0.45f, thick * 0.62f, thick * 0.2f, -b * 0.3f);
        r.on(tib).detail();
        //foot pad
        m = r.part(foot);
        steel(m, s).at(0, 0, 0).lathe(8, 22.5f, thick * 1.5f, 0f, thick * 1.35f, thick * 0.5f, thick * 0.7f, thick * 0.9f, 0.001f, thick * 0.95f);
        r.shadowRound(1 + t.legDefs.size);
        t.leg(femur, tibia, foot, restDeg, mountR, hipZ, a, b);
    }

    /** Aegis Titan (modelScale 2): legs 6, hull at z 13, hips on the hull rim (9.6), femur 13, tibia 17. */
    public static Rig aegis(Walker3D t){
        Style s = serpulo;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        float z = 13f, top = z + 3.0f;
        t.hullZ = z;
        t.footR = 23f;
        t.stride = 6f;
        t.stepTime = 24f;
        t.lift = 4.2f;
        t.lead = 10f;
        t.sectorHalf = 24f;

        //hull: faceted hexagonal armour, flat deck
        Mesh m = r.part(body);
        hull(m, s).at(0, 0, 0).lathe(6, 30f, 7.4f, z - 3.6f, 10.6f, z - 1.4f, 10.6f, z + 0.9f, 8.4f, top, 0.001f, top);
        r.shadowRound(0);
        int hullI = r.lastIndex();
        //belly
        m = r.part(body);
        dark(m, s).at(0, 0, 0).lathe(6, 30f, 0.001f, z - 5.0f, 5.2f, z - 4.8f, 7.2f, z - 3.5f);
        r.under(hullI, 1);
        //hip housings on the rim
        for(int i = 0; i < 6; i++){
            float deg = 60f * i + 30f;
            float hx = Mathf.sinDeg(deg) * 9.6f, hy = Mathf.cosDeg(deg) * 9.6f;
            m = r.part(body);
            steel(m, s).at(hx, hy, z).lathe(8, 22.5f, 0.001f, -1.9f, 1.9f, -1.1f, 1.9f, 1.1f, 0.001f, 1.9f);
            r.detail();
        }
        //deck plates: team stripe aft, mint sensor eye forward
        m = r.part(body);
        team(m).box(-2.6f, -7.4f, top + 0.01f, 2.6f, -5.6f, top + 0.07f);
        r.on(hullI).detail();
        m = r.part(body);
        trim(m, s).box(-4.8f, 4.6f, top + 0.01f, 4.8f, 5.3f, top + 0.09f);
        r.on(hullI).detail();
        AEGIS_EYE = r.bone(body, 0, 9.2f, z + 0.2f);
        m = r.part(AEGIS_EYE);
        glow(m, s).box(-2.2f, 0f, -0.45f, 2.2f, 0.5f, 0.45f);
        r.on(hullI);

        //lance dome
        m = r.part(body);
        hull2(m, s).at(0, -0.8f, 0).lathe(8, 22.5f, 5.0f, top, 4.8f, top + 1.4f, 3.4f, top + 2.3f, 0.001f, top + 2.4f);
        int dome = r.lastIndex();
        //lance: twin rails with a glowing core between (fixed, points where the titan faces)
        int lance = AEGIS_LANCE = r.bone(body, 0, 0.6f, top + 2.2f);
        m = r.part(lance);
        dark(m, s).box(-1.5f, -3.6f, -0.9f, 1.5f, 1.4f, 1.0f);
        int breech = r.lastIndex();
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(lance);
            steel(m, s).box(sd * 0.55f - 0.42f, 0.6f, -0.55f, sd * 0.55f + 0.42f, 14.8f, 0.55f);
            m = r.part(lance);
            trim(m, s).box(sd * 0.55f - 0.5f, 12.6f, -0.65f, sd * 0.55f + 0.5f, 14.2f, 0.65f);
            r.detail();
        }
        m = r.part(lance);
        glow(m, s).box(-0.14f, 1.4f, -0.2f, 0.14f, 14.0f, 0.2f);
        m = r.part(lance);
        trim(m, s).box(-1.6f, -3.2f, 1.0f, 1.6f, -1.0f, 1.3f);
        r.on(breech).detail();
        t.weapon(0, -1, lance, 0f, 15.2f, 0f);

        //missile pods on the shoulders (mount 1 = right, mount 2 = mirrored left)
        for(int sd = 1; sd >= -1; sd -= 2){
            int yaw = r.bone(body, sd * 7.0f, -3.0f, top + 0.1f);
            m = r.part(yaw);
            steel(m, s).at(0, 0, 0).cyl(8, 1.4f, 0f, 0.8f);
            int pod = r.bone(yaw, 0, 0, 0.8f);
            m = r.part(pod);
            hull2(m, s).bevel(-1.9f, -2.6f, 0f, 1.9f, 2.4f, 2.7f, 0.35f);
            int box = r.lastIndex();
            m = r.part(pod);
            dark(m, s).box(-1.6f, 2.4f, 0.3f, 1.6f, 2.55f, 2.4f);
            r.on(box);
            for(int k = 0; k < 4; k++){
                float x = (k % 2 == 0 ? -0.75f : 0.75f), zz = (k < 2 ? 0.85f : 1.85f);
                m = r.part(pod);
                glow(m, s).box(x - 0.3f, 2.56f, zz - 0.3f, x + 0.3f, 2.6f, zz + 0.3f);
                r.on(box).detail();
            }
            m = r.part(pod);
            team(m).box(-1.95f, -1.8f, 1.0f, -1.9f + 0.02f, 0.8f, 1.8f);
            r.on(box).detail();
            t.weapon(sd > 0 ? 1 : 2, yaw, pod, 0f, 2.8f, 1.35f);
        }

        for(int i = 0; i < 6; i++) leg(t, r, root, s, 60f * i + 30f, 9.6f, z, 13f, 17f, 1.05f);

        r.half = 26f;
        r.height = top + 3f;
        t.extra = (type, unit, st, delta) -> {
            type.rig.glow[AEGIS_LANCE] = 0.8f + st.warm * 0.9f;
            type.rig.glow[AEGIS_EYE] = 0.75f + Mathf.absin(Time.time + unit.id * 7f, 18f, 0.35f);
        };
        Color mint = Color.valueOf("8ce5d3");
        t.glow = (type, unit, st, zz) -> {
            if(st.warm <= 0.01f) return;
            float[] p = tmpP;
            type.screen(unit, AEGIS_LANCE, 0f, 15.2f, 0f, p);
            float w = st.warm;
            Draw.color(mint, 0.5f * w);
            Fill.circle(p[0], p[1], 7f * w);
            Draw.color(Color.white, 0.8f * w);
            Fill.circle(p[0], p[1], 2.8f * w);
            Lines.stroke(1.2f * w, mint);
            for(int i = 0; i < 4; i++){
                float a = Time.time * 3f + i * 90f;
                Lines.lineAngle(p[0] + Angles.trnsx(a, 12f * (1f - w) + 4f), p[1] + Angles.trnsy(a, 12f * (1f - w) + 4f), a + 180f, 3f * w);
            }
        };
        return r.finish();
    }

    private static final float[] tmpP = new float[3];

    /** Pyroclast Titan (modelScale 1.8): legs 4, hull at z 12, hips on the rim (8.4), femur 15, tibia 19. */
    public static Rig pyroclast(Walker3D t){
        Style s = erekir;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        float z = 12f, top = z + 3.4f;
        t.hullZ = z;
        t.footR = 24f;
        t.stride = 7.5f;
        t.stepTime = 20f;
        t.lift = 5f;
        t.lead = 9f;
        t.sectorHalf = 32f;

        //squat octagonal furnace hull
        Mesh m = r.part(body);
        hull(m, s).at(0, 0, 0).lathe(8, 22.5f, 6.6f, z - 3.8f, 9.4f, z - 1.6f, 9.4f, z + 1.0f, 7.6f, top, 0.001f, top);
        r.shadowRound(0);
        int hullI = r.lastIndex();
        m = r.part(body);
        dark(m, s).at(0, 0, 0).lathe(8, 22.5f, 0.001f, z - 5.2f, 4.4f, z - 5.0f, 6.4f, z - 3.7f);
        r.under(hullI, 1);
        //magma vents around the flank (four glowing slots between the hips)
        for(int i = 0; i < 4; i++){
            float deg = 90f * i;
            m = r.part(body);
            glow(m, s).at(Mathf.sinDeg(deg) * 9.35f, Mathf.cosDeg(deg) * 9.35f, z - 0.3f).rot(2, -deg).box(-1.6f, -0.1f, -0.5f, 1.6f, 0.12f, 0.5f);
            r.on(hullI).detail();
        }
        for(int i = 0; i < 4; i++){
            float deg = 90f * i + 45f;
            float hx = Mathf.sinDeg(deg) * 8.4f, hy = Mathf.cosDeg(deg) * 8.4f;
            m = r.part(body);
            steel(m, s).at(hx, hy, z).lathe(8, 22.5f, 0.001f, -2.2f, 2.2f, -1.3f, 2.2f, 1.3f, 0.001f, 2.2f);
            r.detail();
        }
        m = r.part(body);
        team(m).box(-2.2f, -7.0f, top + 0.01f, 2.2f, -5.5f, top + 0.07f);
        r.on(hullI).detail();

        //ember crown: a low glowing ring on the deck with six short fins
        PYRO_CROWN = r.bone(body, 0, -1.0f, top);
        m = r.part(PYRO_CROWN);
        dark(m, s).at(0, 0, 0).ring(12, 4.6f, 5.4f, 0f, 0.5f);
        for(int i = 0; i < 6; i++){
            float deg = i * 60f + 30f;
            m = r.part(PYRO_CROWN);
            glow(m, s).at(Mathf.sinDeg(deg) * 5.0f, Mathf.cosDeg(deg) * 5.0f, 0.5f).rot(2, -deg).taper(0, 0, 0f, 0.7f, 0.5f, 1.6f, 0.2f, 0.2f, 0, 0, false);
            r.detail();
        }
        //furnace core in the middle of the crown
        PYRO_CORE = r.bone(body, 0, -1.0f, top);
        m = r.part(PYRO_CORE);
        glow(m, s).at(0, 0, 0).lathe(8, 22.5f, 2.2f, 0f, 1.8f, 0.8f, 0.001f, 1.1f);

        //main gun: a full rotating turret on a race ring above the ember crown (the old fixed 55-degree mortar
        //tube is gone). Yaw bone = the turret house, child bone = the cradle with the twin cannons, so the
        //house tracks the target and only the guns take the recoil.
        m = r.part(body);
        dark(m, s).at(0, -1.0f, top + 0.45f).lathe(8, 22.5f, 3.05f, 0f, 3.05f, 1.0f, 2.65f, 1.45f);
        int pedestal = r.lastIndex();
        m = r.part(body);
        trim(m, s).at(0, -1.0f, top + 1.6f).ring(16, 2.9f, 3.9f, 0f, 0.3f);
        r.on(pedestal).detail();

        int turret = PYRO_TURRET = r.bone(body, 0, -1.0f, top + 1.9f);
        //octagonal turret house, slightly tapered towards the roof
        m = r.part(turret);
        hull2(m, s).at(0, 0, 0).lathe(8, 22.5f, 3.6f, 0f, 4.15f, 0.55f, 4.15f, 2.5f, 3.15f, 3.25f, 0.001f, 3.25f);
        int houseI = r.lastIndex();
        //sloped mantlet across the front of the house
        m = r.part(turret);
        steel(m, s).at(0, 3.25f, 0.6f).taper(0, 0, 0f, 4.3f, 1.5f, 2.3f, 3.1f, 0.9f, 0f, -0.45f, false);
        r.on(houseI).detail();
        //ammunition bustle at the back, with two glowing heat slots
        m = r.part(turret);
        hull2(m, s).cbevel(0, -4.3f, 0.5f, 5.2f, 2.8f, 1.9f, 0.4f);
        int bustle = r.lastIndex();
        for(int i = -1; i <= 1; i += 2){
            m = r.part(turret);
            glow(m, s).box(i * 1.5f - 0.55f, -5.72f, 1.0f, i * 1.5f + 0.55f, -5.6f, 1.85f);
            r.on(bustle).detail();
        }
        //commander cupola with a lens
        m = r.part(turret);
        steel(m, s).at(-2.0f, -0.9f, 3.2f).lathe(8, 22.5f, 1.2f, 0f, 1.2f, 0.75f, 0.8f, 1.05f);
        int cupola = r.lastIndex();
        m = r.part(turret);
        glow(m, s).at(-2.0f, 0.25f, 3.65f).rot(0, -90f).cyl(8, 0.34f, 0f, 0.3f);
        r.on(cupola).detail();
        m = r.part(turret);
        team(m).box(1.5f, -1.4f, 3.24f, 3.0f, 0.1f, 3.3f);
        r.on(houseI).detail();

        //cradle + twin cannons (this bone is the recoil bone)
        int cradle = r.bone(turret, 0, 0, 1.25f);
        m = r.part(cradle);
        hull2(m, s).bevel(-2.3f, -0.8f, -0.55f, 2.3f, 3.6f, 0.95f, 0.35f);
        int cradleI = r.lastIndex();
        float pitch = 12f, bl = 7.4f, by = 2.3f, bz = 0.2f;
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(cradle);
            steel(m, s).at(sd * 1.55f, by, bz).rot(0, -(90f - pitch)).cyl(10, 0.92f, 0f, bl);
            r.on(cradleI).detail();
            m = r.part(cradle);
            dark(m, s).at(sd * 1.55f, by, bz).rot(0, -(90f - pitch)).cyl(10, 1.28f, bl - 1.9f, bl - 0.45f);
            r.detail();
            m = r.part(cradle);
            glow(m, s).at(sd * 1.55f, by, bz).rot(0, -(90f - pitch)).cyl(8, 0.56f, bl - 0.12f, bl);
            r.detail();
        }
        t.weapon(0, turret, cradle, 0f, by + bl * Mathf.cosDeg(pitch), bz + bl * Mathf.sinDeg(pitch));

        //flame sweepers (mount 1 = right, mount 2 = mirrored left)
        for(int sd = 1; sd >= -1; sd -= 2){
            int yaw = r.bone(body, sd * 6.6f, -3.6f, top - 0.6f);
            m = r.part(yaw);
            steel(m, s).at(0, 0, 0).cyl(8, 1.5f, 0f, 0.8f);
            int gun = r.bone(yaw, 0, 0, 1.4f);
            m = r.part(gun);
            hull2(m, s).bevel(-1.3f, -1.8f, -0.6f, 1.3f, 1.4f, 1.3f, 0.3f);
            int housing = r.lastIndex();
            m = r.part(gun);
            steel(m, s).at(0, 1.2f, 0.35f).rot(0, -90).cyl(8, 0.55f, 0f, 3.0f);
            m = r.part(gun);
            glow(m, s).at(0, 4.2f, 0.35f).rot(0, -90).cyl(8, 0.4f, 0f, 0.25f);
            r.detail();
            m = r.part(gun);
            steel(m, s).at(0, -1.2f, 1.3f).cyl(6, 0.7f, 0f, 1.0f);
            r.on(housing).detail();
            t.weapon(sd > 0 ? 1 : 2, yaw, gun, 0f, 4.4f, 0.35f);
        }

        for(int i = 0; i < 4; i++) leg(t, r, root, s, 90f * i + 45f, 8.4f, z, 15f, 19f, 1.25f);

        r.half = 27f;
        r.height = top + 7.6f;
        t.extra = (type, unit, st, delta) -> {
            float fury = 1f - unit.healthf();
            type.rig.rot(PYRO_CROWN, 2, (Time.time + unit.id * 13f) * (0.4f + fury));
            type.rig.glow[PYRO_CROWN] = 0.8f + Mathf.absin(Time.time + unit.id, 9f, 0.3f + fury * 0.4f);
            type.rig.glow[PYRO_CORE] = 0.9f + Mathf.absin(Time.time + unit.id * 3f, 6f, 0.4f);
        };
        Color ember = Color.valueOf("ff9e5c");
        t.glow = (type, unit, st, zz) -> {
            float[] p = tmpP;
            type.screen(unit, PYRO_CORE, 0f, 0f, 1.0f, p);
            float fury = 1f - unit.healthf();
            float pulse = 0.55f + Mathf.absin(Time.time + unit.id * 3f, 6f, 0.25f) + fury * 0.3f;
            Draw.color(ember, 0.22f * pulse);
            Fill.circle(p[0], p[1], 9f * type.modelScale * 0.5f * pulse);
            Draw.color(Color.white, 0.35f * pulse);
            Fill.circle(p[0], p[1], 2.2f * type.modelScale * 0.5f);
        };
        return r.finish();
    }
}
