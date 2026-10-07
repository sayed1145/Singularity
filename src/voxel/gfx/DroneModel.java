package voxel.gfx;

import static voxel.gfx.Palette.*;

/**
 * Tessellus: a quad-ducted support drone. Hexagonal hull with a glass canopy, four ducted fans on diagonal
 * arms that tilt into the direction of travel, spinning rotors with a faint blur disc, a belly twin laser
 * turret and blinking navigation lights. Design units, +y forward.
 */
public final class DroneModel{
    public static int ROOT, BODY, TURRET;
    public static final int[] DUCT = new int[4], ROTOR = new int[4], BLUR = new int[4], BARREL = new int[2], NAV = new int[2];
    /** duct order: front-left, front-right, back-left, back-right */
    public static final float armR = 8.2f, ductR = 3.4f, muzzle = 3.6f;

    private DroneModel(){}

    public static Rig build(){
        Rig r = new Rig();
        ROOT = r.bone(-1, 0, 0, 0);
        BODY = r.bone(ROOT, 0, 0, 6f);
        TURRET = r.bone(BODY, 0, 1.6f, -1.3f);
        for(int i = 0; i < 2; i++){
            BARREL[i] = r.bone(TURRET, (i == 0 ? -0.7f : 0.7f), 1.4f, -0.9f);
            NAV[i] = r.bone(BODY, 0, 0, 0);
        }
        for(int i = 0; i < 4; i++){
            float sx = (i % 2 == 0) ? -1f : 1f, sy = i < 2 ? 1f : -1f;
            float c = 0.7071f * armR;
            DUCT[i] = r.bone(BODY, sx * c, sy * c, 0.4f);
            ROTOR[i] = r.bone(DUCT[i], 0, 0, 0.35f);
            BLUR[i] = r.bone(DUCT[i], 0, 0, 0.4f);
        }

        // ------------------------------------------------ hull
        Mesh m = r.part(BODY); armor(m);
        m.lathe(6, 0, 0, -1.3f, 3.9f, -1.3f, 5.3f, 0.1f, 4.7f, 1.1f, 0, 1.3f);
        r.shadowRound(0);
        int hull = r.lastIndex();
        m = r.part(BODY); dark(m);
        m.lathe(6, 0, 0, -2.0f, 2.8f, -2.0f, 3.95f, -1.25f, 0, -1.25f);
        m = r.part(BODY); m.color(0.30f, 0.62f, 0.72f).style(Mesh.metal, Mesh.matPlain);
        m.at(0, 1.2f, 1.15f).lathe(8, 22.5f, 2.1f, 0, 1.6f, 0.9f, 0, 1.25f);
        r.on(hull);
        m = r.part(BODY); team(m);
        m.at(0, -2.4f, 1.12f).lathe(6, 0, 1.6f, 0, 1.2f, 0.18f, 0, 0.18f);
        r.on(hull).detail();
        //sensor chin + tail fin
        m = r.part(BODY); grey(m);
        m.taper(0, 4.9f, -0.9f, 2.2f, 1.2f, 0.4f, 1.6f, 0.8f, 0, -0.2f, false);
        r.detail();
        m = r.part(BODY); armorShade(m);
        m.taper(0, -4.6f, 0.9f, 0.4f, 2.4f, 2.9f, 0.25f, 1.2f, 0, -0.6f, false);
        r.detail();

        // ------------------------------------------------ arms + ducted fans
        for(int i = 0; i < 4; i++){
            float sx = (i % 2 == 0) ? -1f : 1f, sy = i < 2 ? 1f : -1f;
            float ang = (float)Math.toDegrees(Math.atan2(sy, sx));
            m = r.part(BODY); grey(m);
            m.at(0, 0, 0).rot(2, ang - 90f).cbox(0, (armR - ductR) * 0.5f + 2.6f, -0.35f, 1.2f, armR - ductR - 1.0f, 0.9f);
            m = r.part(BODY); dark(m);
            m.at(0, 0, 0).rot(2, ang - 90f).cbox(0, armR - ductR - 0.1f, -0.6f, 1.6f, 1.2f, 1.4f);

            //duct: outer shell (convex, drawn last) + inner wall (drawn first) with the rotor between them
            m = r.part(DUCT[i]); armor(m);
            m.lathe(16, 0, ductR, -0.7f, ductR + 0.75f, -0.7f, ductR + 0.8f, 0.2f, ductR + 0.45f, 0.75f, ductR, 0.75f);
            r.shadowRound(1 + i);
            int shell = r.lastIndex();
            m = r.part(DUCT[i]); dark(m);
            m.lathe(16, 0, ductR, 0.75f, ductR, -0.7f);
            r.under(shell, 3);
            m = r.part(DUCT[i]); team(m);
            m.lathe(16, 0, ductR + 0.46f, 0.76f, ductR + 0.78f, 0.22f, ductR + 0.79f, 0.3f, ductR + 0.47f, 0.8f);
            r.on(shell).detail();
            //hub + stator
            m = r.part(DUCT[i]); dark(m);
            m.cyl(8, 0.7f, -0.8f, 0.5f);
            r.under(shell, 1);
            //blades: three thin plates
            m = r.part(ROTOR[i]); black(m);
            for(int b = 0; b < 3; b++){
                m.at(0, 0, 0).rot(2, b * 120f).rot(1, 12f).box(0.5f, -0.28f, -0.05f, ductR - 0.15f, 0.28f, 0.05f);
            }
            r.under(shell, 2);
            m = r.part(BLUR[i]); m.color(0.75f, 0.8f, 0.85f).style(Mesh.emissive, Mesh.matPlain);
            m.lathe(16, 0, 0, 0, ductR - 0.1f, 0, ductR - 0.1f, 0.02f, 0, 0.02f);
            r.under(shell, 1);
        }

        // ------------------------------------------------ belly turret
        m = r.part(TURRET); dark(m);
        m.cyl(8, 1.5f, -1.2f, 0.1f);
        m = r.part(TURRET); grey(m);
        m.taper(0, 0.6f, -2.0f, 2.8f, 2.8f, -0.8f, 2.2f, 2.0f, 0, 0.2f, true);
        for(int i = 0; i < 2; i++){
            m = r.part(BARREL[i]); steel(m);
            m.at(0, 0, 0).rot(0, -90).cyl(6, 0.32f, 0, 2.6f);
            m = r.part(BARREL[i]); glow(m);
            m.at(0, 0, 0).rot(0, -90).cyl(6, 0.36f, 2.6f, 3.0f);
            r.detail();
        }

        // ------------------------------------------------ navigation lights (port red, starboard team)
        m = r.part(NAV[0]); red(m);
        m.cbox(-5.15f, 0.2f, 0.05f, 0.35f, 0.9f, 0.3f);
        m = r.part(NAV[1]); teamGlow(m);
        m.cbox(5.15f, 0.2f, 0.05f, 0.35f, 0.9f, 0.3f);

        r.half = 12f;
        r.height = 9f;
        return r.finish();
    }
}
