package blackhole;

import arc.struct.Seq;
import blackhole.g3d.LiftedWeapon;
import blackhole.g3d.Serpent3D;
import blackhole.models.AnchorModels;
import mindustry.gen.Sounds;
import mindustry.ctype.UnlockableContent;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.type.Category;

import static blackhole.AureliaContent.*;
import static mindustry.type.ItemStack.with;

/**
 * v8.5.2：反拘留者兵器 —— 锚钉炮（炮塔）与锚钉手（单位）。
 *
 * <p>两者的"特殊机制"是同一件事，也只有这一件事：弹药的官方标志位 {@code absorbable = false}
 * （见 {@link AnchorParts#spike}）。拘留者的爪子抓不走它，也就没法把它变成自己的能量或反射弹。
 *
 * <p>它对任何单位都是同一发弹丸、同一个伤害 —— 不"针对"拘留者。
 * 拘留者的圣盾（aegis）依然照常工作，所以这套兵器不是免费击杀，只是不再给它充能。
 *
 * <p><b>v8.5.7</b>：锚钉炮射速 x1.25、伤害 x4.5（造价不变）；锚钉手主炮射速 x1.2、伤害 x6，
 * 另加两门变种炮（射速是主炮的 2.25 倍，伤害 58 / 84），移动改为蛇式分节躯干（去掉履带），
 * 血量提到 12000，建造成本按 T5 档重算。三发弹药一律保持 {@code absorbable = false}。
 */
public final class AnchorContent{
    /** 锚钉炮：3x3 电力炮塔。 */
    public static Block anchorSpike;
    /** 锚钉手：v8.5.7 起是蛇式分节地面单位（原为履带式）。 */
    public static UnitType spikeWarden;

    // ---------------------------------------------------------------- v8.5.7 数值
    // 主炮：射速 x1.2（72 -> 60 刻）、伤害 x6（27 -> 162）
    public static final float MAIN_RELOAD = 72f / 1.2f;
    public static final float MAIN_DAMAGE = 27f * 6f;
    // 两门变种炮：伤害各不相同，都落在要求的 42~97 之间
    public static final float VARIANT_A_DAMAGE = 58f;
    public static final float VARIANT_B_DAMAGE = 84f;

    /** everything this pack adds, in tree order */
    public static final Seq<UnlockableContent> all = new Seq<>();

    private static boolean loaded;

    private AnchorContent(){}

    static <T extends UnlockableContent> T add(T c){
        all.add(c);
        AureliaContent.own.add(c);
        return c;
    }

    public static void load(){
        if(loaded) return;
        loaded = true;

        /*
         * 1. The turret. Vanilla PowerTurret in every respect - no ammo, power only, official targeting,
         * official reload and heat - with two things swapped: the bullet (absorbable = false) and the
         * firing method (SpearPattern instead of the usual alternating pair or spread).
         */
        anchorSpike = add(new AnchorParts.AnchorSpike("anchor-spike"){{
            requirements(Category.turret, with(lumenite, 460, aurite, 320, prismGlass, 170, resonanceShard, 70, starMarrow, 8));
            size = 3;
            health = 1520;
            range = 300f;
            //v8.5.7: 射速 x1.25 —— 84 / 1.25 = 67.2 刻
            reload = 67.2f;
            targetAir = true;
            targetGround = true;
            inaccuracy = 1.5f;
            shootSound = Sounds.shootBreach;
            //new firing method: four rounds queued down the barrel, fired 2.5 ticks apart
            shoot = new AnchorParts.SpearPattern(){{
                shots = 4;
                backoff = 5.5f;
                spacing = 2.5f;
            }};
            //28 ticks x 11 units/tick = 308 world units, so it dies just past the turret's own range
            //v8.5.7: 伤害 x4.5 —— 34 * 4.5 = 153
            shootType = AnchorParts.spike(11f, 153f, 28f);
            consumePower(4.6f);
            envEnabled = mindustry.world.meta.Env.any;
        }});

        /*
         * 2. The unit: the same round, now on a segmented serpentine body (v8.5.7).
         *
         * v8.5.7 的改动：
         *   - 主炮射速 x1.2（72 -> 60 刻）、伤害 x6（27 -> 162）；
         *   - 新增两门变种炮，射速是主炮的 2.25 倍（60 / 2.25 = 26.667 刻），
         *     伤害各不相同但都落在 42~97 之间（58 与 84），弹药同样 absorbable = false；
         *   - 移动从履带改为引擎官方 CrawlUnit 的蛇式分节（见 Serpent3D），履带整套去掉；
         *   - 血量 2350 -> 12000（要求 8000 以上、15000 以下）；
         *   - 建造成本另见 AureliaFrontier —— 它现在按 T5 档计价。
         */
        spikeWarden = add(new Serpent3D("spike-warden"){{
            builder = AnchorModels::spikeWarden;
            speed = 0.78f;
            hitSize = 16f;
            health = 12000f;
            armor = 12f;
            rotateSpeed = 2.4f;
            range = 230f;
            targetAir = true;
            targetGround = true;

            //0: the main spear gun — 3 rounds queued down the barrel, 162 each
            weapons.add(new LiftedWeapon(){{
                mirror = false;
                rotate = true;
                rotateSpeed = 2.6f;
                top = true;
                reload = 60f;
                recoil = 2.2f;
                inaccuracy = 2f;
                shootSound = Sounds.shootBreach;
                //the same queue, three rounds instead of four
                shoot = new AnchorParts.SpearPattern(){{
                    shots = 3;
                    backoff = 4.5f;
                    spacing = 2.5f;
                }};
                //22 ticks x 11 units/tick = 242 world units, just past the unit's own range
                bullet = AnchorParts.spike(11f, MAIN_DAMAGE, 22f);
            }});

            //1: the light variant — 58, fires 2.25x as fast as the main gun
            weapons.add(new LiftedWeapon(){{
                mirror = false;
                rotate = true;
                rotateSpeed = 4.4f;
                top = true;
                reload = MAIN_RELOAD / 2.25f;
                recoil = 1.1f;
                inaccuracy = 3f;
                shootSound = Sounds.shootBreach;
                bullet = AnchorParts.spike(11f, VARIANT_A_DAMAGE, 22f);
            }});

            //2: the heavy variant — 84, same cadence as the light one
            weapons.add(new LiftedWeapon(){{
                mirror = false;
                rotate = true;
                rotateSpeed = 4.4f;
                top = true;
                reload = MAIN_RELOAD / 2.25f;
                recoil = 1.6f;
                inaccuracy = 3f;
                shootSound = Sounds.shootBreach;
                bullet = AnchorParts.spike(11f, VARIANT_B_DAMAGE, 22f);
            }});
        }});
    }
}
