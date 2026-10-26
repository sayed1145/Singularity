package blackhole.models;

import arc.graphics.Color;
import arc.math.Mathf;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import mindustry.gen.*;

import static blackhole.g3d.Kit.*;
import static blackhole.g3d.Mesh.*;
import static blackhole.models.TurretModels.mount;

/**
 * v8.5.2 反拘留者兵器：锚钉炮的 3D 模型，与「锚钉手」单位的骨架。
 *
 * <p>和模组其它内容同一套规矩：程序化建模、一套配色、一个运动件、零件各就各位不互相穿插、
 * 不悬空。造型刻意做得朴素 —— 一座矮座圈上架一根长刺，一眼能看出它是"钉东西"的，
 * 不是又一台发光的水晶炮。
 */
public final class AnchorModels{
    private AnchorModels(){}

    static final Color spike = Color.valueOf("9fd8ff"), hot = Color.valueOf("ffe1a8");

    // =====================================================================================================
    // 炮塔：锚钉炮 (3x3)
    // =====================================================================================================

    /** Anchor spike (3x3): a low ring mount carrying one long spear rail. */
    public static SimpleTurret anchorSpike = new SimpleTurret("anchor-spike", 3, aurelia).headZ(4.0f)
        .base(m -> {
            mount(m, aurelia, 3, 5.2f, 4.0f, 0);
            //four anchor feet, one per corner: this gun kicks, so it is bolted down
            for(int i = 0; i < 4; i++){
                float a = 45f + i * 90f, c = Mathf.cosDeg(a), s = Mathf.sinDeg(a);
                hull2(m, aurelia).at(0, 0, 0).cbox(c * 9.4f, s * 9.4f, 0.9f, 2.0f, 2.0f, 1.1f);
                dark(m, aurelia).at(0, 0, 0).ring(8, 1.0f, 1.45f, 1.0f, 1.25f).at(c * 9.4f, s * 9.4f, 0f);
            }
        })
        .head(m -> {
            //a low slab mantlet, not a dome
            hull(m, aurelia).hexa(false, -3.0f, -3.6f, 0f, 3.0f, -3.6f, 0f, 2.4f, 2.6f, 0f, -2.4f, 2.6f, 0f,
                -2.6f, -3.2f, 2.4f, 2.6f, -3.2f, 2.4f, 2.0f, 2.2f, 2.2f, -2.0f, 2.2f, 2.2f);
            trim(m, aurelia).box(-2.65f, -2.0f, 1.6f, 2.65f, -1.8f, 2.1f);
            //the spear rail: one long thin tube with three stiffening collars
            steel(m, aurelia).axis(0, 1.6f, 1.5f, 0, 9.2f, 1.5f).cyl(8, 0.52f, 0f, 7.6f);
            for(int k = 0; k < 3; k++){
                float y = 3.2f + k * 2.2f;
                trim(m, aurelia).axis(0, y, 1.5f, 0, y + 0.5f, 1.5f).cyl(8, 0.95f - k * 0.12f, 0f, 0.5f);
            }
            //muzzle ring and the one light strip along the rail
            m.color(spike).style(emissive, 0).axis(0, 9.0f, 1.5f, 0, 9.15f, 1.5f).ring(8, 0.34f, 0.62f, 0f, 0.05f);
            //reset the frame: axis() frames must not leak into the axis-aligned boxes below
            m.at(0, 0, 0);
            glow(m, aurelia).box(-0.22f, 1.8f, 1.93f, 0.22f, 8.6f, 2.0f);
            team(m).box(-2.7f, -1.4f, 0.9f, -2.55f, 1.8f, 1.9f);
            team(m).box(2.55f, -1.4f, 0.9f, 2.7f, 1.8f, 1.9f);
        })
        .muzzles(0f, 9.3f, 1.5f)
        .glow(b -> TurretModels.power(b) * 0.8f + b.heat * 0.5f);

    // =====================================================================================================
    // 单位：锚钉手
    // =====================================================================================================

    // =====================================================================================================
    // 单位：锚钉手 —— v8.5.7 起改为蛇式（分节躯干），履带整套去掉
    // =====================================================================================================

    // 躯干各节的尺寸：wF 前段半宽、wB 后段半宽、len 半长、z0 腹面、z1 背面
    private static final float[][] SNAKE = {
        // 半长一律明显大于 Serpent3D.spacing(2.9)，让相邻装甲板永远叠着 —— 蛇甲本来就该叠着，
        // 缩短到 spacing 以下就会在弯折时被拉开露出缝（SerpentProbe 量的就是这个）
        {2.75f, 2.98f, 1.95f, 0.80f, 3.30f},   // i=1 颈
        {2.45f, 2.75f, 1.90f, 0.80f, 3.10f},   // i=2 前段（副炮 A）
        {2.15f, 2.45f, 1.85f, 0.80f, 2.90f},   // i=3 中段
        {1.85f, 2.15f, 1.78f, 0.80f, 2.60f},   // i=4 后段（副炮 B）
        {1.50f, 1.85f, 1.70f, 0.80f, 2.25f},   // i=5 尾根
        {0.95f, 1.50f, 1.70f, 0.80f, 1.80f},   // i=6 尾尖
    };

    /**
     * Spike warden: a segmented armoured serpent. No treads, no wheels, no legs — a wedge head,
     * six tapering body sections and a row of belly scutes that take turns gripping the ground.
     *
     * <p>和模组其它内容同一套规矩：程序化建模、一套配色、一个运动件、零件各就各位不互相穿插、不悬空。
     * 每一节都是独立的骨头，由 {@link Serpent3D} 按引擎公式 + 真实蛇类蜿蜒逐帧摆放。
     */
    public static Rig spikeWarden(Serpent3D t){
        Style s = aurelia;
        Rig r = new Rig();
        int root = r.bone(-1, 0, 0, 0);
        int head = t.BODY = r.bone(root, 0, 0, 0);

        // ---------------------------------------------------------------- head (segment 0)
        //wedge skull: wide at the back, narrow snout at the front (+y is forward)
        Mesh m = r.part(head);
        hull(m, s).hexa(true,
            -2.95f, -2.30f, 0.70f, 2.95f, -2.30f, 0.70f, 2.00f, 2.50f, 0.70f, -2.00f, 2.50f, 0.70f,
            -2.72f, -1.95f, 3.40f, 2.72f, -1.95f, 3.40f, 1.62f, 2.15f, 3.40f, -1.62f, 2.15f, 3.40f);
        r.shadow(0);
        int headI = r.lastIndex();

        //the snout: two mandible plates that jut past the skull
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(head);
            hull2(m, s).hexa(true,
                sd < 0 ? -1.62f : 0.30f, 2.05f, 0.85f, sd < 0 ? -0.30f : 1.62f, 2.05f, 0.85f,
                sd < 0 ? -0.30f : 1.20f, 3.35f, 0.85f, sd < 0 ? -1.20f : 0.30f, 3.35f, 0.85f,
                sd < 0 ? -1.40f : 0.26f, 2.15f, 2.05f, sd < 0 ? -0.26f : 1.40f, 2.15f, 2.05f,
                sd < 0 ? -0.26f : 1.00f, 3.20f, 2.05f, sd < 0 ? -1.00f : 0.26f, 3.20f, 2.05f);
            r.on(headI).detail();
        }

        //dorsal ridge over the skull, and the two eyes
        m = r.part(head);
        trim(m, s).box(-0.42f, -1.90f, 3.40f, 0.42f, 1.70f, 3.90f);
        r.on(headI).detail();
        for(int sd = -1; sd <= 1; sd += 2){
            m = r.part(head);
            glow(m, s).box(sd < 0 ? -2.30f : 1.60f, 0.55f, 2.30f, sd < 0 ? -1.60f : 2.30f, 1.45f, 2.90f);
            r.on(headI).detail();
        }

        //team colour: a band across the back of the skull
        m = r.part(head);
        team(m).box(-2.60f, -2.20f, 1.55f, 2.60f, -1.95f, 2.65f);
        r.on(headI).detail();
        m = r.part(head);
        team(m).box(-1.85f, 2.28f, 1.30f, 1.85f, 2.48f, 2.20f);
        r.on(headI).detail();

        //belly scute of the head, hinged at the back of the jaw
        int headScute = r.bone(head, 0, -2.0f, 0.70f);
        m = r.part(headScute);
        dark(m, s).box(-1.95f, -1.55f, -0.26f, 1.95f, 0.15f, 0.04f);
        t.segment(head, headScute);

        // ---------------------------------------------------------------- body segments 1..6
        int[] segBone = new int[SNAKE.length + 1];
        for(int i = 0; i < SNAKE.length; i++){
            int idx = i + 1;
            float wF = SNAKE[i][0], wB = SNAKE[i][1], len = SNAKE[i][2], z0 = SNAKE[i][3], z1 = SNAKE[i][4];
            float sp = t.spacing;

            int seg = r.bone(head, 0f, -idx * sp, 0f);
            segBone[idx] = seg;

            //the section itself: a frustum, wide at the back, narrower at the front
            m = r.part(seg);
            hull(m, s).hexa(true,
                -wB, -len, z0, wB, -len, z0, wF, len, z0, -wF, len, z0,
                -wB * 0.90f, -len * 0.94f, z1, wB * 0.90f, -len * 0.94f, z1,
                wF * 0.90f, len * 0.94f, z1, -wF * 0.90f, len * 0.94f, z1);
            r.shadow(0);
            int secI = r.lastIndex();

            //dorsal ridge
            m = r.part(seg);
            trim(m, s).box(-0.34f, -len * 0.86f, z1, 0.34f, len * 0.86f, z1 + 0.42f);
            r.on(secI).detail();

            //one glow strip down each flank, so the wave reads even at a glance
            for(int sd = -1; sd <= 1; sd += 2){
                m = r.part(seg);
                glow(m, s).box(sd < 0 ? -wB * 0.90f - 0.06f : wB * 0.90f, -len * 0.55f, z0 + 0.75f,
                    sd < 0 ? -wB * 0.90f : wB * 0.90f + 0.06f, len * 0.55f, z0 + 1.35f);
                r.on(secI).detail();
            }

            //belly scute: hinged at the front of the section, swings down to grip
            int sc = r.bone(seg, 0f, -len * 0.85f, z0 - 0.05f);
            m = r.part(sc);
            dark(m, s).box(-wB * 0.74f, -len * 1.55f, -0.24f, wB * 0.74f, 0.10f, 0.02f);
            m = r.part(sc);
            trim(m, s).box(-wB * 0.74f, -len * 1.62f, -0.40f, wB * 0.74f, -len * 1.48f, -0.24f);
            t.segment(seg, sc);
        }

        // ---------------------------------------------------------------- weapons
        // 0: the main spear, on a ring mount over the skull
        int tur = r.bone(head, 0f, -0.35f, 3.40f);
        m = r.part(tur);
        hull2(m, s).at(0, 0, 0).lathe(8, 22.5f, 2.15f, 0f, 2.15f, 0.75f, 1.70f, 1.45f, 0.001f, 1.55f);
        int dom = r.lastIndex();
        m = r.part(tur);
        trim(m, s).at(0, 0, 0).ring(8, 2.08f, 2.24f, 0.30f, 0.48f);
        r.on(dom).detail();

        int bar = r.bone(tur, 0f, 1.20f, 0.90f);
        //the spear: a long thin tube, the same silhouette as the turret's rail
        m = r.part(bar);
        steel(m, s).at(0, 0, 0).rot(0, -90).lathe(8, 22.5f, 0.50f, 0f, 0.50f, 3.60f, 0.62f, 6.40f, 0.42f, 6.85f);
        m = r.part(bar);
        glow(m, s).at(0, 6.75f, 0).rot(0, -90).ring(8, 0.30f, 0.60f, 0f, 0.10f);
        t.weapon(0, tur, bar, 0f, 7.05f, 0f);

        // 1 + 2: the two variant guns, on sections 2 and 4 — short and slim, then long and heavy
        t.weapon(1, pod(r, s, segBone[2], 0f, 2.20f, SNAKE[1][4], 1, 0.34f, 2.05f), -1, 0f, 2.35f, 0f);
        t.weapon(2, pod(r, s, segBone[4], 0f, 2.20f, SNAKE[3][4], 1, 0.46f, 3.30f), -1, 0f, 3.65f, 0f);

        r.half = 12.6f;
        r.height = 4.6f;
        r.stackSort = true;
        return r.finish();
    }

    /**
     * A small ring mount with one stubby barrel, bolted on top of a body section.
     * Returns the barrel bone so the caller can hand it straight to {@link Serpent3D#weapon}.
     */
    private static int pod(Rig r, Style s, int seg, float ox, float oy, float oz, int sides, float bore, float len){
        int yaw = r.bone(seg, ox, oy, oz);
        Mesh m = r.part(yaw);
        hull2(m, s).at(0, 0, 0).lathe(8, 22.5f, 1.30f, 0f, 1.30f, 0.28f, 1.05f, 0.52f, 0.001f, 0.58f);
        int dom = r.lastIndex();
        m = r.part(yaw);
        trim(m, s).at(0, 0, 0).ring(8, 1.24f, 1.36f, 0.10f, 0.22f);
        r.on(dom).detail();

        int barrel = r.bone(yaw, 0f, 0.55f, 0.34f);
        m = r.part(barrel);
        steel(m, s).at(0, 0, 0).rot(0, -90).lathe(sides, 22.5f, bore, 0f, bore, len, bore * 1.18f, len + 0.30f, bore * 1.18f, len + 0.36f);
        m = r.part(barrel);
        glow(m, s).at(0, len + 0.30f, 0).rot(0, -90).ring(8, bore * 0.75f, bore * 1.30f, 0f, 0.09f);
        return barrel;
    }

    static{
        anchorSpike.glowColor = spike;
        anchorSpike.heatColor = hot;
    }
}
