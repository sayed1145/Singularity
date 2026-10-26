package blackhole.g3d;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.struct.FloatSeq;
import arc.struct.IntSeq;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Crawlc;
import mindustry.gen.CrawlUnit;
import mindustry.gen.Unit;

/**
 * 蛇式移动：一节一节跟着走的躯干（snake / serpentine locomotion）。
 *
 * <p>实体用的是官方 {@link CrawlUnit}，也就是引擎里唯一真正的"蛇"——它自带
 * {@link Crawlc} 分量，每帧推进两件事（v160.5 {@code mindustry.entities.comp.CrawlComp}）：
 * <pre>
 * segmentRot = Angles.moveToward(segmentRot, rotation, type.segmentRotSpeed * Time.delta);
 * segmentRot = Angles.clampRange(segmentRot, rotation, type.segmentMaxRot);
 * crawlTime += deltaLen();          // 累加走过的距离，不是时间
 * </pre>
 * 于是 {@code segmentRot} 是"身体整体朝向"（比机头朝向滞后），{@code crawlTime} 是"走了多远"。
 * 物理、寻路、减速（{@code crawlSlowdown}）、碾压伤害全部照旧由引擎负责，这里只画。
 *
 * <h2>建模参考（两处，都写在这里）</h2>
 *
 * <p><b>1. 引擎官方公式</b>（v160.5 {@code UnitType.drawCrawl}），分节的摆放与相位：
 * <pre>
 * float trns = Mathf.sin(crawlTime + i * segmentPhase, segmentScl, segmentMag);
 * float rot  = Mathf.slerp(crawl.segmentRot(), unit.rotation, i / (float)(segments - 1));
 * Draw.rect(regions[i], unit.x + Angles.trnsx(rot, trns), unit.y + Angles.trnsy(rot, trns), rot - 90);
 * </pre>
 * 两点要照抄，两点要反过来：
 * <ul>
 *   <li><b>照抄</b>：{@code Mathf.sin(a, b, c)} 是 {@code sin(a / b) * c}（反编译 arc 字节码核对过）；
 *       这个 {@code trns} 是<b>沿身体轴</b>的偏移（官方用 {@code Angles.trnsx(rot, trns)}），
 *       所以是纵向的一松一紧，像毛虫那样蠕动。</li>
 *   <li><b>反过来</b>：官方的第 0 节拿 {@code segmentRot}、最后一节才拿 {@code unit.rotation}，
 *       也就是官方把<b>最后一节当成头</b>来画。这边的骨架第 0 节就是头，所以插值方向要倒过来
 *       （见 {@link #animate}）—— 机头指向当前航向，尾巴拖着滞后的 {@code segmentRot}。
 *       不倒过来的话蛇会倒着游。</li>
 * </ul>
 *
 * <p><b>2. 真实蛇类的蜿蜒（serpentine locomotion）</b>：蛇前进靠的是从头传向尾的<b>横向行波</b>，
 * 身体每一段侧向推地面，波往后走、蛇往前走。三个特征是这套动画照着调的：
 * <ul>
 *   <li><b>波从头传到尾</b> —— 相位写成 {@code crawlTime / waveScl - i * wavePhase}，
 *       同一个波峰随着 {@code crawlTime} 增大要往更大的 {@code i} 走，也就是往尾巴走；</li>
 *   <li><b>头部幅度小、尾部幅度大</b> —— 真蛇的头基本走直线（要盯着猎物/看路），
 *       摆幅由头到尾递增，所以用 {@code headAmp + (1 - headAmp) * f} 做增幅；</li>
 *   <li><b>腹鳞（scute）依次蹬地</b> —— 蛇没有腿，靠一排腹鳞轮流抓地，
 *       所以每节底下有一片腹鳞，按同一个行波错相拍动。</li>
 * </ul>
 *
 * <p>这两套叠起来就是：引擎给的纵向蠕动 + 真实蛇类的横向行波。
 * 没有履带、没有轮子、没有腿 —— 原来的 {@code Tank3D} 履带整套去掉了。
 */
public class Serpent3D extends Unit3DType<Serpent3D.SerpentState>{
    public interface Builder{ Rig build(Serpent3D type); }
    public interface Extra{ void animate(Serpent3D type, Unit unit, SerpentState s, float delta); }

    public static class SerpentState extends Unit3DType.State{
        public float crawl, bend, speedF, warm;
        public final float[] recoil = new float[8];
        public final int[] shots = new int[8];
    }

    public Builder builder;
    public Extra extra;

    /** 躯干各节的骨头，第 0 节是头。 */
    public final IntSeq segments = new IntSeq();
    /** 每节底下的腹鳞骨头（没有就是 -1）。 */
    public final IntSeq scutes = new IntSeq();
    /** 头（= 第 0 节）。 */
    public int BODY;
    public final IntSeq weaponBone = new IntSeq(), barrelBone = new IntSeq();
    public final FloatSeq muzzle = new FloatSeq();

    /** 相邻两节中心相距多少设计单位（静止姿态）。 */
    public float spacing = 2.9f;

    /** 引擎官方那套纵向蠕动：{@code sin((crawlTime + i*phase)/scl) * mag}，加在步长上，单位是设计单位。 */
    public float mag = 0.4f, scl = 4f, phase = 5f;

    /** 真实蛇类的横向行波：每节的<b>局部弯折幅度</b>，单位是度。 */
    public float waveAmp = 13f;
    /** 横向行波：走过多少距离算一个弧度。越大波越长。 */
    public float waveScl = 11f;
    /** 横向行波：每往后一节滞后多少弧度。越大身上同时容纳的波越多。 */
    public float wavePhase = 0.9f;

    /** 腹鳞拍动的幅度（度）与空间频率。 */
    public float scuteAmp = 26f, scuteScl = 7f, scutePhase = 0.9f;
    /** 躯干上下起伏的幅度（度）—— 蛇不是贴着地面平移，身体会一段段微微抬起。 */
    public float liftAmp = 4f, liftScl = 9f, liftPhase = 0.7f;

    public float recoilDist = 1.2f;
    public Color flashColor = Color.valueOf("ffffff");


    public Serpent3D(String name){
        super(name);
        constructor = CrawlUnit::create;
        omniMovement = false;
        shadowAlpha = 0.3f;
        shadowLength = 0.3f;
        //CrawlComp 每帧要读的四个量：身体朝向追机头的速度、最大弯折、踩到建筑上的减速、减速所需的实心比例
        segmentRotSpeed = 2.6f;
        segmentMaxRot = 55f;
        crawlSlowdown = 0.72f;
        crawlSlowdownFrac = 0.6f;
    }

    public void weapon(int mount, int yawBone, int recoilBone, float mx, float my, float mz){
        while(weaponBone.size <= mount){ weaponBone.add(-1); barrelBone.add(-1); muzzle.addAll(0, 0, 0); }
        weaponBone.set(mount, yawBone);
        barrelBone.set(mount, recoilBone);
        muzzle.set(mount * 3, mx);
        muzzle.set(mount * 3 + 1, my);
        muzzle.set(mount * 3 + 2, mz);
    }

    /** 登记一节躯干（头先登记）。scute 是它底下的腹鳞骨头，没有就传 -1。 */
    public void segment(int bone, int scute){
        segments.add(bone);
        scutes.add(scute);
    }

    @Override
    protected Rig buildRig(){
        segments.clear();
        scutes.clear();
        weaponBone.clear();
        barrelBone.clear();
        muzzle.clear();
        return builder.build(this);
    }

    @Override
    protected SerpentState newState(){
        return new SerpentState();
    }

    @Override
    public void init(){
        super.init();
        alignWeapons(weaponBone, barrelBone, muzzle, 0f);
    }

    /**
     * 摆好整条脊柱。out 每节三个数：{@code x} 横向、{@code y} 纵向、{@code yaw} 该节朝向（相对机头，度）。
     *
     * <p><b>位置是沿脊柱按弧长积分出来的</b>，不是各节各算各的。这是关键：
     * 第一步先算好每节的朝向，第二步从机头起一节一节往前走，每步走 {@code spacing + 引擎纵向蠕动}
     * 这么长、方向取相邻两节朝向的角平分线。于是相邻两节中心的距离<b>恒等于步长</b>，
     * 躯干在任何相位、任何弯折下都不可能被拉开（自检工具 {@code preview.SerpentProbe} 量的就是这个）。
     *
     * <p>早期版本是每节独立摆 {@code (横向偏移, -i*spacing)} —— 横向行波的幅度一大，
     * 尾巴那几节就会被甩开，实测最大裂到 8.5 个设计单位。改成积分之后这个失效模式从根上没有了。
     */
    public void spine(int n, float crawlTime, float bodyRot, float unitRot, float[] out){
        //pass 1 - every section's yaw, in the head's frame
        for(int i = 0; i < n; i++){
            //(1) engine: fan from the lagged body heading (tail) to the current heading (head).
            //    Interpolated the other way round from UnitType.drawCrawl, which draws the LAST
            //    segment as the head; here segment 0 IS the head.
            float f = n > 1 ? i / (float)(n - 1) : 0f;
            float base = Mathf.slerp(bodyRot, unitRot, 1f - f) - unitRot;
            //(2) real serpentine: a wave of local bends travelling head -> tail. cos here, because the
            //    heading is what bends; integrating it gives the sine-shaped body curve.
            float wave = Mathf.cos(crawlTime / waveScl - i * wavePhase) * waveAmp;
            //normalise to (-180, 180]: slerp is free to hand back, say, 343.7 for -16.3, and the
            //bisector below has to average two angles the SHORT way round or the spine folds.
            ang[i] = wrap180(base + wave);
        }

        //pass 2 - walk the spine: one step per gap, turning on the bisector of the two sections
        float px = 0f, py = 0f;
        for(int i = 0; i < n; i++){
            out[i * 3] = px;
            out[i * 3 + 1] = py;
            out[i * 3 + 2] = ang[i];
            if(i + 1 < n){
                //shortest-arc bisector: never average 350 and 10 into 180
                float am = (ang[i] + wrap180(ang[i + 1] - ang[i]) * 0.5f) * Mathf.degRad;
                //(3) engine: the longitudinal bunch/stretch rides on the step length
                float step = spacing + Mathf.sin(crawlTime + i * phase, scl, mag);
                px += Mathf.sin(am) * step;
                py -= Mathf.cos(am) * step;
            }
        }
    }

    /** scratch for {@link #spine}, reused every frame (drawing is single threaded) */
    private final float[] ang = new float[64];
    private final float[] spineOut = new float[64 * 3];

    @Override
    protected void animate(Unit unit, SerpentState s, float delta, boolean payload){
        float scale = modelScale * Draw.xscl;
        int n = segments.size;

        //engine: crawlTime is distance travelled, segmentRot is the lagged body heading
        Crawlc crawl = (Crawlc)unit;
        float crawlTime = crawl.crawlTime();
        float bodyRot = crawl.segmentRot();

        spine(n, crawlTime, bodyRot, unit.rotation, spineOut);

        for(int i = 0; i < n; i++){
            int b = segments.get(i);
            //rig local frame: +y is forward, and spine() has already integrated the curve
            rig.place(b, spineOut[i * 3], spineOut[i * 3 + 1], 0f);
            rig.rot(b, 2, spineOut[i * 3 + 2]);
            //(4) real serpentine: each section lifts slightly as the wave passes
            rig.rot(b, 0, Mathf.sin(crawlTime / liftScl - i * liftPhase) * liftAmp);

            int sc = scutes.get(i);
            if(sc >= 0){
                //belly scutes take turns gripping the ground, same travelling wave, one phase later
                rig.rot(sc, 0, Mathf.sin(crawlTime / scuteScl - i * scutePhase - 1.2f) * scuteAmp);
            }
        }
        if(n > 0) rig.glow[segments.get(0)] = 0.8f + 0.2f * unit.healthf();

        for(int i = 0; i < unit.mounts.length && i < weaponBone.size; i++){
            WeaponMount m = unit.mounts[i];
            if(m.totalShots != s.shots[i]){
                if(s.init) s.recoil[i] = 1f;
                s.shots[i] = m.totalShots;
            }
            s.recoil[i] = Mathf.approach(s.recoil[i], 0f, 0.06f * delta);
            int wb = weaponBone.get(i);
            if(wb >= 0 && m.weapon.rotate) rig.rot(wb, 2, wrap180(m.rotation));
            int bb = barrelBone.get(i);
            if(bb >= 0){
                rig.moveLocal(bb, 0, -s.recoil[i] * recoilDist * Math.max(m.weapon.recoil / 2f, 0.5f), 0);
                rig.glow[bb] = 0.75f + s.recoil[i] * 0.6f;
            }
            if(i == 0) s.warm = m.warmup;
        }

        if(extra != null) extra.animate(this, unit, s, delta);
        renderer.pose(rig, unit.rotation, 0f, 0f, 0f, scale);
    }

    @Override
    protected void drawExtras(Unit unit, SerpentState s, float z, boolean step, boolean payload){
        if(payload) return;
        Draw.z(z + 0.015f);
        Draw.blend(Blending.additive);
        for(int i = 0; i < unit.mounts.length && i < barrelBone.size; i++){
            if(s.recoil[i] > 0.35f && barrelBone.get(i) >= 0){
                screen(unit, barrelBone.get(i), muzzle.get(i * 3), muzzle.get(i * 3 + 1), muzzle.get(i * 3 + 2), v3);
                Draw.color(flashColor, s.recoil[i]);
                Fill.circle(v3[0], v3[1], 2.2f * s.recoil[i] * modelScale + 0.5f);
            }
        }
        Draw.blend();
        Draw.color();
    }
}
