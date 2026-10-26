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

    /**
     * Citadel pilot: twin ducted fans on fixed outriggers, a cutter drum under the nose.
     *
     * <p><b>v8.5.2 重做。</b>保留了它认得出的三件事（两侧涵道、背上的货舱、机头底下的切割鼓），
     * 但把机身改成与模组其它飞行器同一套语言：平顶方箱、一条灯带、一块队伍色，所有面板都在同一个平面上，
     * 没有悬空件、没有互相穿插的零件。
     *
     * <p>同时修掉了机头切割鼓的转向：鼓是 {@code .rot(0, 90)} 建的，轴沿着机身 Y（前后方向），
     * 原来却在绕 X 转，整只鼓在翻跟头；改成绕 Y（{@code rot(CUTTER, 1, ...)}），才是"绕着圆转"。
     */
    public static Rig citadelPilot(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        FANL = r.bone(body, -5.0f, -0.4f, 0.55f);
        FANR = r.bone(body, 5.0f, -0.4f, 0.55f);
        CUTTER = r.bone(body, 0, 5.4f, -0.95f);

        //---- fuselage: a flat-topped box with squared shoulders, not a wedge
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -2.8f, -4.6f, -1.0f, 2.8f, -4.6f, -1.0f, 1.5f, 6.4f, -1.0f, -1.5f, 6.4f, -1.0f,
            -2.5f, -4.1f, 1.4f, 2.5f, -4.1f, 1.4f, 1.2f, 5.9f, 1.4f, -1.2f, 5.9f, 1.4f);
        r.shadow(0);
        int hullI = r.lastIndex();

        //canopy, set flush into a flat spine so nothing floats above the hull
        m = r.part(body);
        glass(m, canopy).taper(0, 1.2f, 1.35f, 2.0f, 3.0f, 1.9f, 1.0f, 1.5f, 0, -0.2f, false);
        r.on(hullI);

        //dorsal cargo deck: the tier-3 pilot carries a lot, and you can see it
        m = r.part(body);
        hull2(m, s).box(-2.0f, -3.9f, 1.4f, 2.0f, -1.0f, 1.95f);
        r.on(hullI);
        int deck = r.lastIndex();
        m = r.part(body);
        dark(m, s).box(-1.6f, -3.6f, 1.95f, 1.6f, -1.3f, 2.05f);
        r.on(deck).detail();

        //one light strip down the spine and the team colour on the flank - same house rule as the hulls
        m = r.part(body);
        glow(m, s).box(-0.28f, -0.9f, 1.4f, 0.28f, 4.5f, 1.48f);
        r.on(hullI).detail();
        m = r.part(body);
        team(m).box(-1.0f, -4.14f, -0.2f, 1.0f, -4.1f, 0.9f);
        r.on(hullI).detail();

        for(int sd = -1; sd <= 1; sd += 2){
            //---- outrigger: one straight spar from the hull shoulder to the duct
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

        //---- nose: a plain fairing that carries the cutter yoke, so the drum is not hanging in mid air
        m = r.part(body);
        hull2(m, s).box(-1.3f, 4.2f, -1.0f, 1.3f, 5.9f, 0.2f);
        r.on(hullI);
        int nose = r.lastIndex();
        m = r.part(body);
        trim(m, s).box(-1.35f, 4.1f, -0.75f, 1.35f, 4.25f, 0.1f);
        r.on(nose).detail();

        //---- cutter head: a drum in the yoke, the citadel's wall-mining tool
        m = r.part(body);
        hull2(m, s).box(-1.25f, 4.5f, -1.3f, 1.25f, 5.7f, -0.45f);
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
            //v8.5.2: the drum is built with .rot(0, 90), which lays its lathe axis along local Y (fore-aft).
            //Spinning it about X (axis 0) tumbled the whole drum end over end; it has to turn about its own
            //axis - "around the circle" - which is Y, i.e. axis 1.
            type.rig.rot(CUTTER, 1, d.b);
        };
        return r.finish();
    }
}
