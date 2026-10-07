package blackhole;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class BHBullets{
    public static BulletType
        collapsarShell, singularityWell,
        microHole, hawkingLance, accretionSpray,
        frameDragShot, quasarBeam,
        degenerateShell,
        citadelCollapsar, citadelWell;

    public static void load(){

        // ============================================================
        // 事件视界炮：新机制
        // 炮弹本身只是投送载具，落点会生成一颗【持续存在且不断长大】的真黑洞
        // ============================================================

        // 落点生成的那颗持久黑洞（本模组的核心机制）
        singularityWell = new GrowingHoleBulletType(){{
            lifetime = 60f * 11f;      // 存在 11 秒
            startRadius = 3.5f;
            endRadius = 27f;           // 持续扩大到这么大
            pullRadius = 230f;
            pullForce = 5.4f;
            damage = 0f;
            despawnEffect = BHFx.finalCollapse;
            hitEffect = BHFx.collapseSmall;
            lensStrength = 1.25f;
        }};

        // 投送炮弹
        collapsarShell = new BasicBulletType(3.1f, 120f){{
            lifetime = 70f;
            width = 15f;
            height = 22f;
            backColor = BHPal.rim;
            frontColor = BHPal.core;
            shrinkX = shrinkY = 0f;
            trailLength = 26;
            trailWidth = 4.2f;
            trailColor = BHPal.accretion;
            hitShake = 7f;
            despawnShake = 7f;
            collidesAir = collidesGround = true;
            pierceArmor = true;
            hittable = false;
            absorbable = false;
            splashDamage = 180f;
            splashDamageRadius = 60f;
            hitEffect = BHFx.collapse;
            despawnEffect = BHFx.collapse;
            shootEffect = BHFx.singularityCharge;
            smokeEffect = BHFx.muzzleDust;
            lightRadius = 80f;
            lightColor = BHPal.accretion;

            // 命中/落地即在该点种下持久黑洞
            fragOnHit = true;
            fragOnDespawn = true;
            fragBullet = singularityWell;
            fragBullets = 1;
            fragVelocityMin = 0f;
            fragVelocityMax = 0f;
            fragLifeMin = 1f;
            fragLifeMax = 1f;
        }};

        // ============================================================
        // 其余炮塔弹种
        // ============================================================

        // 小型黑洞弹（吸积炮 / 能层号用）—— 飞行中拖拽，命中塌缩
        microHole = new TravellingHoleBulletType(3.6f, 42f){{
            lifetime = 54f;
            holeRadius = 3.1f;
            pullRadius = 78f;
            pullForce = 2.1f;
            splashDamage = 62f;
            splashDamageRadius = 38f;
            hitEffect = BHFx.collapseSmall;
            despawnEffect = BHFx.collapseSmall;
            trailLength = 16;
            trailWidth = 2.3f;
            trailColor = BHPal.accretion;
            collidesAir = collidesGround = true;
            hittable = false;
            lightRadius = 50f;
            lightColor = BHPal.accretion;
            lensStrength = 0.75f;
        }};

        // 霍金发射器：穿透光束
        hawkingLance = new LaserBulletType(340f){{
            length = 258f;
            width = 24f;
            lifetime = 24f;
            colors = new Color[]{BHPal.hawking.cpy().a(0.3f), BHPal.hawking, Color.white};
            hitEffect = BHFx.radiationHit;
            sideAngle = 18f;
            sideWidth = 1.2f;
            sideLength = 72f;
            pierceCap = 8;
            shootEffect = BHFx.hawkingCharge;
            status = StatusEffects.electrified;
            statusDuration = 60f * 4f;
            lightColor = BHPal.hawking;
        }};

        // 吸积炮：速射弹幕
        accretionSpray = new BasicBulletType(5.4f, 30f){{
            lifetime = 33f;
            width = 8f;
            height = 14f;
            backColor = BHPal.rim;
            frontColor = Color.white;
            trailLength = 10;
            trailWidth = 1.7f;
            trailColor = BHPal.accretion;
            hitEffect = despawnEffect = BHFx.degenerateHit;
            homingPower = 0.09f;
            homingRange = 70f;
        }};

        // 参考系拖曳炮：高速动能弹，附带击退
        frameDragShot = new BasicBulletType(7.2f, 58f){{
            lifetime = 30f;
            width = 10f;
            height = 18f;
            backColor = BHPal.degenerate;
            frontColor = Color.white;
            trailLength = 13;
            trailWidth = 2f;
            trailColor = BHPal.degenerate;
            knockback = 2.6f;
            pierce = true;
            pierceCap = 3;
            hitEffect = despawnEffect = BHFx.dragHit;
            status = StatusEffects.slow;
            statusDuration = 60f * 2f;
        }};

        // 类星体喷流：持续激光
        quasarBeam = new ContinuousLaserBulletType(78f){{
            length = 215f;
            lifetime = 80f;
            width = 8f;
            drawSize = 460f;
            shake = 1.2f;
            colors = new Color[]{
                BHPal.accretion.cpy().a(0.32f),
                BHPal.accretion.cpy().a(0.65f),
                BHPal.core,
                Color.white
            };
            hitEffect = BHFx.quasarBurn;
            status = StatusEffects.melting;
            statusDuration = 60f * 2f;
            incendChance = 0.1f;
            incendSpread = 5f;
            incendAmount = 1;
        }};

        // ============================================================
        // 新单位弹种
        // ============================================================

        // 简并榴弹：吸积车 / 平台副炮
        degenerateShell = new BasicBulletType(4.2f, 70f){{
            lifetime = 42f;
            width = 11f;
            height = 16f;
            backColor = BHPal.degenerate;
            frontColor = Color.white;
            trailLength = 12;
            trailWidth = 2.1f;
            trailColor = BHPal.degenerate;
            splashDamage = 55f;
            splashDamageRadius = 34f;
            hitEffect = despawnEffect = BHFx.degenerateHit;
            hitShake = 1.6f;
            knockback = 1.2f;
        }};

        // 平台主炮种下的黑洞（比炮塔版稍小、持续稍短）
        citadelWell = new GrowingHoleBulletType(){{
            lifetime = 60f * 8f;
            startRadius = 3f;
            endRadius = 20f;
            pullRadius = 190f;
            pullForce = 4.6f;
            damage = 0f;
            despawnEffect = BHFx.finalCollapse;
            hitEffect = BHFx.collapseSmall;
            lensStrength = 1.1f;
        }};

        citadelCollapsar = new BasicBulletType(3.4f, 90f){{
            lifetime = 62f;
            width = 13f;
            height = 19f;
            backColor = BHPal.rim;
            frontColor = BHPal.core;
            shrinkX = shrinkY = 0f;
            trailLength = 22;
            trailWidth = 3.6f;
            trailColor = BHPal.accretion;
            hitShake = 6f;
            despawnShake = 6f;
            collidesAir = collidesGround = true;
            pierceArmor = true;
            hittable = false;
            absorbable = false;
            splashDamage = 140f;
            splashDamageRadius = 52f;
            hitEffect = BHFx.collapse;
            despawnEffect = BHFx.collapse;
            shootEffect = BHFx.singularityCharge;
            smokeEffect = BHFx.muzzleDust;
            lightRadius = 70f;
            lightColor = BHPal.accretion;

            fragOnHit = true;
            fragOnDespawn = true;
            fragBullet = citadelWell;
            fragBullets = 1;
            fragVelocityMin = 0f;
            fragVelocityMax = 0f;
            fragLifeMin = 1f;
            fragLifeMax = 1f;
        }};
    }

    // ================================================================
    // 机制一：落点生成、持续扩大的黑洞
    // ================================================================
    public static class GrowingHoleBulletType extends BulletType{
        public float startRadius = 3f, endRadius = 24f;
        public float pullRadius = 200f, pullForce = 5f;
        public float lensStrength = 1.2f;
        /** 生长曲线指数，<1 表示前期长得快 */
        public float growPow = 0.62f;

        public GrowingHoleBulletType(){
            super(0f, 0f);
            speed = 0f;
            collides = false;
            collidesTiles = false;
            collidesAir = collidesGround = false;
            hittable = false;
            absorbable = false;
            keepVelocity = false;
            despawnHit = false;
            drawSize = 720f;
            layer = Layer.effect;
        }

        /** 当前半径 */
        public float radius(Bullet b){
            float f = Mathf.pow(b.fin(), growPow);
            // 末尾 12% 快速塌缩收束
            float close = 1f - Mathf.curve(b.fin(), 0.88f, 1f);
            return Mathf.lerp(startRadius, endRadius, f) * close;
        }

        @Override
        public void update(Bullet b){
            super.update(b);

            float r = radius(b);
            if(r <= 0.05f) return;

            // 半径越大，吞噬范围与力度同步增长
            float grow = r / Math.max(0.001f, endRadius);
            float pr = pullRadius * (0.45f + grow * 0.55f);
            float pf = pullForce * (0.5f + grow * 0.5f);

            Units.nearbyEnemies(b.team, b.x - pr, b.y - pr, pr * 2f, pr * 2f, u -> {
                float dst = u.dst(b);
                if(dst > pr) return;
                float scl = 1f - dst / pr;
                Tmp.v1.set(b.x - u.x, b.y - u.y).nor().scl(pf * scl * scl * 9f * Time.delta);
                u.impulse(Tmp.v1);

                // 越过视界 -> 被撕碎
                if(dst < r * 1.45f){
                    u.damagePierce(58f * Time.delta * (0.4f + grow));
                    if(Mathf.chanceDelta(0.5f)){
                        BHFx.devour.at(u.x, u.y, b.angleTo(u), BHPal.accretion);
                    }
                }
            });

            // 吞噬敌方弹幕
            Groups.bullet.intersect(b.x - pr, b.y - pr, pr * 2f, pr * 2f, o -> {
                if(o == b || o.team == b.team || !o.type.absorbable) return;
                if(o.dst(b) < r * 2.2f){
                    o.remove();
                    BHFx.devour.at(o.x, o.y, 0f, BHPal.rim);
                }
            });

            // 吸积盘火花
            if(Mathf.chanceDelta(0.85f)){
                BHFx.accretionSpark.at(b.x, b.y, r, BHPal.accretion);
            }

            // 屏幕震动随体积增长
            if(Mathf.chanceDelta(0.25f)){
                Effect.shake(grow * 1.6f, 5f, b.x, b.y);
            }
        }

        @Override
        public void draw(Bullet b){
            float r = radius(b);
            if(r <= 0.05f) return;

            // 注册真实屏幕空间引力透镜
            BHShaders.addLens(b.x, b.y, r, lensStrength);

            Draw.z(Layer.effect + 0.5f);
            BlackHoleRenderer.draw(b.x, b.y, r, 0.72f, Time.time + b.id * 7f, 1f);
            Draw.reset();
        }

        @Override
        public void drawLight(Bullet b){
            Drawf.light(b.x, b.y, radius(b) * 8f, BHPal.accretion, 0.85f);
        }
    }

    // ================================================================
    // 机制二：飞行途中拖拽的小黑洞弹
    // ================================================================
    public static class TravellingHoleBulletType extends BulletType{
        public float holeRadius = 3f;
        public float pullRadius = 80f, pullForce = 2f;
        public float lensStrength = 0.8f;
        public float tilt = 0.75f;

        public TravellingHoleBulletType(float speed, float damage){
            super(speed, damage);
            drawSize = 420f;
            despawnHit = true;
            keepVelocity = false;
        }

        @Override
        public void update(Bullet b){
            super.update(b);
            float pr = pullRadius;

            Units.nearbyEnemies(b.team, b.x - pr, b.y - pr, pr * 2f, pr * 2f, u -> {
                float dst = u.dst(b);
                if(dst > pr) return;
                float scl = 1f - dst / pr;
                Tmp.v1.set(b.x - u.x, b.y - u.y).nor().scl(pullForce * scl * scl * 8f * Time.delta);
                u.impulse(Tmp.v1);
                if(dst < holeRadius * 2.1f){
                    u.damagePierce(damage * 0.2f * Time.delta);
                    BHFx.devour.at(u.x, u.y, b.angleTo(u), BHPal.accretion);
                }
            });

            if(Mathf.chanceDelta(0.5f)){
                BHFx.accretionSpark.at(b.x, b.y, holeRadius, BHPal.accretion);
            }
        }

        @Override
        public void draw(Bullet b){
            drawTrail(b);
            float f = Mathf.curve(b.fin(), 0f, 0.1f) * (1f - Mathf.curve(b.fin(), 0.9f, 1f));
            float r = holeRadius * f;
            if(r <= 0.02f) return;

            BHShaders.addLens(b.x, b.y, r, lensStrength * f);

            Draw.z(Layer.effect + 0.5f);
            BlackHoleRenderer.draw(b.x, b.y, r, tilt, Time.time + b.id * 7f, 1f);
            Draw.reset();
        }

        @Override
        public void drawLight(Bullet b){
            Drawf.light(b.x, b.y, lightRadius, lightColor, lightOpacity);
        }
    }
}
