package blackhole.models;

import arc.graphics.*;
import blackhole.g3d.*;
import blackhole.g3d.Kit.*;
import blackhole.g3d.Mesh;

import static blackhole.g3d.Kit.*;

/**
 * v7.6 unit rigs: the mote (air), the lattice crawler (ground) and the tide lancer (naval).
 *
 * <p>Same conventions as {@link UnitModels}: design units = world units, +y forward, z up, one convex solid per
 * piece, moving parts on their own bones. Each silhouette has to be recognisable at campaign zoom, so every unit
 * gets one strong shape - a ring for the mote, a wedge with a drum for the crawler, a long prow with torpedo
 * tubes for the lancer - instead of a pile of greebles.
 */
public final class FrontierUnits{
    private FrontierUnits(){}

    static final Color canopy = Color.valueOf("5a86c8");

    public static int MOTE_RING;

    /** Mote: a palm-sized drone - a lifting ring around a tiny pod, with a mining lens below the nose. */
    public static Rig mote(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        MOTE_RING = r.bone(body, 0, 0, -0.2f);
        int nose = r.bone(body, 0, 2.6f, 0);

        //pod
        Mesh m = r.part(body);
        hull(m, s).at(0, 0, 0).rot(0, -90).lathe(6, 30f, 0.001f, -2.0f, 1.1f, -1.0f, 1.25f, 0.6f, 0.8f, 2.0f, 0.001f, 2.7f);
        r.shadowRound(0);
        int pod = r.lastIndex();
        m = r.part(body);
        glass(m, canopy).at(0, 1.1f, 0.5f).lathe(6, 0f, 0.55f, 0f, 0.001f, 0.45f);
        r.on(pod).detail();
        m = r.part(body);
        team(m).box(-0.5f, -1.6f, 0.62f, 0.5f, -0.9f, 0.66f);
        r.on(pod).detail();
        //lifting ring (spins)
        m = r.part(MOTE_RING);
        trim(m, s).at(0, 0, 0).ring(10, 2.0f, 2.35f, -0.12f, 0.12f);
        int ring = r.lastIndex();
        for(int i = 0; i < 3; i++){
            float a = 90f + i * 120f;
            m = r.part(MOTE_RING);
            steel(m, s).at(0, 0, 0).box(-0.16f, 0f, -0.08f, 0.16f, 2.1f, 0.08f).rot(2, a);
            r.on(ring).detail();
        }
        m = r.part(MOTE_RING);
        glow(m, s).at(0, 0, 0).ring(10, 2.05f, 2.3f, 0.12f, 0.17f);
        r.on(ring).detail();
        //mining lens
        m = r.part(nose);
        glow2(m, s).at(0, 0, -0.35f).lathe(6, 30f, 0.45f, 0f, 0.001f, -0.5f);
        t.engine(body, 0, -2.3f, 0f, 0.6f);
        t.weapon(0, -1, nose, 0, 0.2f, 0);
        r.half = 3.2f;
        r.height = 1.6f;
        t.extra = (type, unit, d, delta) -> type.rig.rot(MOTE_RING, 2, d.spin * 11f);
        return r.finish();
    }

    /**
     * Lattice crawler: low tracked hull with a rotating charge drum and a stubby mortar.
     *
     * <p><b>v8.5.2 上半身重做。</b>原来是"楔形车壳 + 斜插的装药鼓 + 圆顶炮塔"，三件互相穿插，
     * 而且和模组里其它单位（方箱、平顶、一条灯带）完全不是一个路数。现在改成同一套语言的素净版本：
     * <ul>
     *   <li>一块平顶方箱车壳（只有倒角，不楔），四条竖直加强筋；</li>
     *   <li>南面一条灯带 + 一小块队伍色，和堡垒/前哨的车体同一处理；</li>
     *   <li>装药鼓压扁、沉进甲板，只露一圈环，不再有东西斜插在炮塔后面；</li>
     *   <li>炮塔改成矮八边座圈，武器（短粗迫击炮）尺寸与位置不变。</li>
     * </ul>
     * <b>下半身（两条履带）一个顶点都没改。</b>
     */
    public static Rig latticeCrawler(Tank3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int tur = r.bone(body, 0, -1.0f, 4.1f), bar = r.bone(tur, 0, 1.4f, 1.1f);

        for(int sd = -1; sd <= 1; sd += 2){
            t.track(r, body, sd * 5.2f, 12.4f, 1.6f, 2.4f, 13, 4, s);
            Mesh m = r.part(body);
            hull2(m, s).taper(sd * 5.2f, 0, 3.2f, 2.8f, 14.2f, 3.8f, 2.2f, 13.2f, 0, 0, true);
            r.shadow(0);
        }

        //flat-topped box hull: bevelled, not wedged, and level with the tracks at the front
        Mesh m = r.part(body);
        hull(m, s).hexa(true, -4.0f, -6.6f, 1.0f, 4.0f, -6.6f, 1.0f, 3.4f, 7.0f, 1.0f, -3.4f, 7.0f, 1.0f,
            -3.7f, -6.0f, 4.3f, 3.7f, -6.0f, 4.3f, 3.1f, 6.4f, 4.3f, -3.1f, 6.4f, 4.3f);
        r.shadow(0);
        int hullI = r.lastIndex();

        //four plain corner stiffeners - they stop the box reading as a bare slab
        for(int sd = -1; sd <= 1; sd += 2){
            for(float y : new float[]{-4.4f, 3.0f}){
                m = r.part(body);
                trim(m, s).box(sd < 0 ? -4.02f : 3.42f, y - 0.5f, 1.0f, sd < 0 ? -3.72f : 3.72f, y + 0.5f, 4.3f);
                r.on(hullI).detail();
            }
        }

        //one light strip and the team colour on the south face, like the rest of the mod
        m = r.part(body);
        glow(m, s).box(-2.2f, -6.72f, 1.7f, 2.2f, -6.62f, 2.6f);
        r.on(hullI).detail();
        m = r.part(body);
        team(m).box(-1.4f, -6.68f, 3.1f, 1.4f, -6.6f, 3.9f);
        r.on(hullI).detail();

        //charge drum: sunk flush into the deck, only the rim shows - nothing leans out behind the turret
        m = r.part(body);
        dark(m, s).at(0, -4.0f, 4.28f).rot(0, -90).cyl(10, 1.32f, -1.5f, 1.5f);
        r.on(hullI);
        int drum = r.lastIndex();
        m = r.part(body);
        glow2(m, s).at(0, -4.0f, 4.3f).rot(0, -90).ring(10, 0.94f, 1.28f, -1.55f, -1.4f);
        r.on(drum).detail();

        //turret: a low octagonal ring instead of a dome
        m = r.part(tur);
        hull2(m, s).at(0, 0, 0).lathe(8, 22.5f, 2.5f, 0f, 2.5f, 0.9f, 2.0f, 1.7f, 0.001f, 1.8f);
        int dome = r.lastIndex();
        m = r.part(tur);
        trim(m, s).at(0, 0, 0).ring(8, 2.42f, 2.58f, 0.35f, 0.6f);
        r.on(dome).detail();

        //short wide mortar barrel (unchanged: it is the weapon)
        m = r.part(bar);
        steel(m, s).at(0, 0, 0).rot(0, -90).lathe(8, 22.5f, 0.7f, 0f, 0.7f, 2.6f, 0.9f, 2.6f, 0.9f, 3.4f);
        m = r.part(bar);
        glow(m, s).at(0, 3.45f, 0).rot(0, -90).ring(8, 0.46f, 0.84f, 0f, 0.12f);
        t.weapon(0, tur, bar, 0, 3.6f, 0);
        r.half = 8.2f;
        r.height = 6.2f;
        r.stackSort = true;
        return r.finish();
    }

    /** Tide lancer: long naval hull, raised bridge, a torpedo turret forward and a light gun aft. */
    public static Rig tideLancer(Ship3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int body = t.BODY = r.bone(root, 0, 0, 0);
        int tubeTur = r.bone(body, 0, 4.4f, 2.0f), tube = r.bone(tubeTur, 0, 1.0f, 0.4f);
        int aftTur = r.bone(body, 0, -5.6f, 2.1f), aftBar = r.bone(aftTur, 0, 0.9f, 0.5f);

        Mesh m = r.part(body);
        hull(m, s).hexa(true, -3.0f, -8.6f, -1.0f, 3.0f, -8.6f, -1.0f, 0.8f, 10.2f, -0.5f, -0.8f, 10.2f, -0.5f,
            -3.6f, -9.2f, 1.6f, 3.6f, -9.2f, 1.6f, 0.6f, 11.2f, 1.9f, -0.6f, 11.2f, 1.9f);
        r.shadow(0);
        int hullI = r.lastIndex();
        //bridge
        m = r.part(body);
        hull2(m, s).at(0, 0, 0).cbevel(0, -1.4f, 1.6f, 3.2f, 4.0f, 2.1f, 0.3f);
        r.on(hullI);
        int cab = r.lastIndex();
        m = r.part(body);
        glass(m, canopy).box(-1.5f, 0.52f, 2.2f, 1.5f, 0.56f, 3.1f);
        r.on(cab).detail();
        m = r.part(body);
        trim(m, s).at(0, -1.4f, 3.7f).cyl(6, 0.3f, 0f, 1.5f);
        r.on(cab).detail();
        m = r.part(body);
        team(m).box(-3.61f, -7.2f, 0.4f, -3.56f, -4.2f, 1.0f);
        r.on(hullI).detail();
        m = r.part(body);
        glow(m, s).box(-0.5f, 10.6f, 1.2f, 0.5f, 10.8f, 1.7f);
        r.on(hullI).detail();
        //forward torpedo mount: two tubes side by side
        m = r.part(tubeTur);
        dark(m, s).at(0, 0, 0).lathe(8, 22.5f, 1.8f, 0f, 1.7f, 0.8f, 1.1f, 1.3f, 0.001f, 1.4f);
        int mount = r.lastIndex();
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(tube);
            steel(m, s).at(sd * 0.85f, 0, 0).rot(0, -90).cyl(8, 0.62f, 0f, 4.0f);
            r.on(mount);
            int tb = r.lastIndex();
            m = r.part(tube);
            glow2(m, s).at(sd * 0.85f, 4.0f, 0).rot(0, -90).ring(8, 0.35f, 0.6f, 0f, 0.12f);
            r.on(tb).detail();
        }
        t.weapon(0, tubeTur, tube, 0, 4.2f, 0);
        //aft gun
        m = r.part(aftTur);
        hull2(m, s).at(0, 0, 0).lathe(8, 22.5f, 1.5f, 0f, 1.4f, 0.7f, 0.9f, 1.2f, 0.001f, 1.3f);
        m = r.part(aftBar);
        steel(m, s).at(0, 0, 0).rot(0, -90).cyl(6, 0.32f, 0f, 2.8f);
        t.weapon(1, aftTur, aftBar, 0, 3.0f, 0);
        t.engine(body, 0, -9.4f, 0.1f, 1.0f);
        r.half = 11.4f;
        r.height = 4.4f;
        return r.finish();
    }
}
