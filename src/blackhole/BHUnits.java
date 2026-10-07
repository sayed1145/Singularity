package blackhole;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.abilities.*;
import mindustry.entities.part.*;
import mindustry.entities.part.DrawPart.*;
import mindustry.entities.pattern.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import blackhole.g3d.Ship3D;
import blackhole.g3d.Tank3D;
import blackhole.g3d.LiftedWeapon;
import blackhole.models.UnitModels;

/**
 * 单位设计避开"又一只蜘蛛"的套路：
 *   lensing     轻型空中侦察机
 *   ergosphere  重型空中炮艇
 *   accretor    履带式补给/维修车（地面，非腿）
 *   citadel     ★ 泰坦级移动作战平台：履带底盘 + 六边约束护盾 +
 *               全身机械动画与可展开的重炮，是一座会走路的前线基地
 */
public class BHUnits{
    public static UnitType lensing, ergosphere, accretor, citadel;

    public static void load(){

        // ==============================================================
        // 轻型空中：透镜号
        // ==============================================================
        lensing = new Ship3D("lensing"){{
            builder = UnitModels::lensing;
            hover = 5f;
            engineColor = BHPal.accretion;
            speed = 2.9f;
            accel = 0.08f;
            drag = 0.016f;
            health = 620f;
            armor = 3f;
            hitSize = 13f;
            engineOffset = 6.5f;
            engineSize = 2.6f;
            lowAltitude = true;
            range = 140f;
            outlineColor = BHPal.horizon;

            abilities.add(new PrismWardAbility(38f, 2.2f, 340f, 60f * 7f){{
                color = BHPal.degenerate; height = 7f;
            }});

            weapons.add(new LiftedWeapon(){{
                x = 4.5f;
                y = 5f;
                muzzleZ = -0.2f;
                reload = 18f;
                top = false;
                mirror = true;
                rotate = false;
                inaccuracy = 4f;
                bullet = BHBullets.accretionSpray;
                shootSound = BHSounds.pulse;
            }});
        }};

        // ==============================================================
        // 地面履带：吸积车（维修 / 支援，不是腿式）
        // ==============================================================
        accretor = new Tank3D("accretor"){{
            builder = UnitModels::accretor;
            speed = 0.86f;
            hitSize = 22f;
            health = 3200f;
            armor = 9f;
            rotateSpeed = 2.4f;
            omniMovement = false;
            crushDamage = 1.1f;
            range = 120f;
            targetAir = false;
            outlineColor = BHPal.horizon;

            // 真实履带动画：引擎从黑洞模组贴图中切出链节条带并在移动时滚动。
            // Rect 使用贴图像素坐标；单条履带由引擎自动镜像到车体另一侧。
            treadFrames = 4;
            treadPullOffset = 3;
            treadRects = new Rect[]{new Rect(-48f, -48f, 19f, 96f)};

            abilities.add(new AureliaAbilities.MendField(38f, 60f * 2.4f, 78f){{
                healEffect = BHFx.horizonPulse;
                activeEffect = BHFx.platformShield;
            }});

            weapons.add(new LiftedWeapon(){{
                x = 0f;
                y = -1f;
                muzzleZ = 6.8f;
                reload = 32f;
                top = true;
                mirror = false;
                rotate = true;
                rotateSpeed = 3.2f;
                recoil = 2f;
                shake = 1f;
                bullet = BHBullets.degenerateShell;
                shootSound = BHSounds.thump;
            }});
        }};

        // ==============================================================
        // 重型空中：能层号
        // ==============================================================
        ergosphere = new Ship3D("ergosphere"){{
            builder = UnitModels::ergosphere;
            hover = 7f;
            engineColor = BHPal.hawking;
            speed = 1.55f;
            accel = 0.05f;
            drag = 0.02f;
            health = 4800f;
            armor = 12f;
            hitSize = 30f;
            engineOffset = 13f;
            engineSize = 5.2f;
            range = 220f;
            targetAir = targetGround = true;
            outlineColor = BHPal.horizon;
            circleTarget = false;
            faceTarget = true;

            abilities.add(new PrismWardAbility(72f, 3.6f, 1400f, 60f * 9f){{
                color = BHPal.degenerate; height = 12f;
            }});
            abilities.add(new AureliaAbilities.Regrowth(){{
                percentAmount = 0.1f / 60f * 100f / 60f;
            }});

            weapons.add(new LiftedWeapon(){{
                x = 10.6f;
                y = 0.4f;
                muzzleZ = 1.8f;
                reload = 95f;
                top = true;
                mirror = true;
                rotate = true;
                rotateSpeed = 2.6f;
                shake = 2.5f;
                recoil = 3.2f;
                inaccuracy = 1.5f;
                bullet = BHBullets.microHole;
                shootSound = BHSounds.collapse;
                //one bolt from each drawn barrel (barrels 1.4 apart), same two shots per salvo as before
                barrelSpread = 1.4f;
                shoot = new ShootAlternate(1.4f){{ shots = 2; shotDelay = 5f; }};
            }});
        }};

        // ==============================================================
        // ★ 泰坦级移动作战平台：堡垒号
        //   一辆巨型履带载具 —— 会走的前线基地
        //   · 六边形约束护盾罩住身边的友军
        //   · 不再设有机库，也不会生产任何飞行单位
        //   · 一门主炮（种黑洞）+ 两门副炮（对空 / 对地）
        //   · 维修场修复周围单位；车体、炮塔和履带均有独立动画
        // ==============================================================
        citadel = new Tank3D("citadel"){{
            builder = UnitModels::citadel;
            speed = 0.42f;
            hitSize = 58f;
            health = 34000f;
            armor = 26f;
            rotateSpeed = 0.9f;
            omniMovement = false;
            crushDamage = 5.5f;
            range = 320f;
            targetAir = targetGround = true;
            outlineColor = BHPal.horizon;
            rotateMoveFirst = true;

            // 泰坦双履带动画。
            // 只定义左侧链节条带：TankUnit 会自动镜像并根据移动距离切换 4 帧，
            // 因此不会出现旧版「有履带贴图却不滚动」或采样到透明像素的问题。
            treadFrames = 4;
            treadPullOffset = 7;
            treadRects = new Rect[]{new Rect(-116f, -116f, 36f, 232f)};

            // 移动基地的防护/维修能力。机库与 UnitSpawnAbility 已完全移除：
            // 堡垒号不再生产任何飞行单位。
            abilities.add(new PrismWardAbility(132f, 7f, 9000f, 60f * 11f){{
                color = BHPal.degenerate; height = 18f;
            }});
            abilities.add(new AureliaAbilities.MendField(70f, 60f * 1.8f, 140f){{
                healEffect = BHFx.horizonPulse;
                activeEffect = BHFx.platformShield;
            }});

            // 车体动画（约束阵列旋转、核心脉冲、主炮蓄力时舱盖/导流翼展开）全部在 3D 模型 UnitModels.citadel 中。

            // 主炮：种黑洞（塔顶中央，慢速大威力）
            weapons.add(new LiftedWeapon(){{
                x = 0f;
                y = 8f;
                muzzleZ = 17.4f;
                reload = 60f * 7.5f;
                top = true;
                mirror = false;
                rotate = true;
                rotateSpeed = 1.3f;
                shake = 7f;
                recoil = 6.5f;
                inaccuracy = 0.6f;
                shootCone = 8f;
                bullet = BHBullets.citadelCollapsar;
                shootSound = BHSounds.collapse;
                chargeSound = BHSounds.charge;
                shootWarmupSpeed = 0.05f;
                minWarmup = 0.9f;
            }});

            // 副炮 ×2：对空速射
            weapons.add(new LiftedWeapon(){{
                x = 22f;
                y = 16f;
                muzzleZ = 10.2f;
                reload = 11f;
                top = true;
                mirror = true;
                rotate = true;
                rotateSpeed = 6.5f;
                recoil = 1.4f;
                inaccuracy = 5f;
                targetGround = false;
                bullet = BHBullets.accretionSpray;
                shootSound = BHSounds.pulse;
                //twin AA barrels at +-0.75 of the turret: alternate between exactly those two
                barrelSpread = 1.5f;
                shoot = new ShootAlternate(1.5f);
            }});

            // 副炮 ×2：对地榴弹
            weapons.add(new LiftedWeapon(){{
                x = 22f;
                y = -16f;
                muzzleZ = 10.4f;
                reload = 42f;
                top = true;
                mirror = true;
                rotate = true;
                rotateSpeed = 3f;
                recoil = 2.6f;
                shake = 1.6f;
                targetAir = false;
                bullet = BHBullets.degenerateShell;
                shootSound = BHSounds.thump;
            }});
        }};
    }
}
