package voxel.content;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.*;
import voxel.unit.*;

/**
 * The mod's units. Stats are unchanged from 1.1 for tessellus and titan; arachne is new and sits between them.
 * Every weapon is a {@link LiftedWeapon}: bullets leave the drawn 3D muzzle and still hit the aim point.
 */
public class VoxelUnits{
    public static UnitType tessellus, arachne, titan;

    static final Color cyan = Color.valueOf("8fe6ff"), amber = Color.valueOf("ffd27f");

    public static void load(){
        //================================================================================
        // TESSELLUS - quad-duct support drone
        //================================================================================
        tessellus = new TessellusUnitType("tessellus"){{
            health = 760f;
            armor = 5f;
            hitSize = 17f;
            speed = 2.1f;
            accel = 0.07f;
            drag = 0.05f;
            rotateSpeed = 4.4f;
            faceTarget = true;
            targetAir = true;
            targetGround = true;
            circleTarget = false;
            buildSpeed = 0.9f;
            mineTier = 3;
            mineSpeed = 6.2f;
            itemCapacity = 70;
            payloadCapacity = 0f;
            hoverHeight = 5.2f;

            weapons.add(new LiftedWeapon(){{
                x = 0f;
                y = 1.3f;
                shootY = 3.0f;
                muzzleZ = 3.1f;
                reload = 19f;
                rotate = true;
                rotateSpeed = 5.5f;
                mirror = false;
                inaccuracy = 1.5f;
                shootCone = 22f;
                showStatSprite = false;
                shootSound = Sounds.shootLaser;
                ejectEffect = Fx.none;
                bullet = new LaserBoltBulletType(5.4f, 24f){{
                    lifetime = 28f;
                    width = 2.4f;
                    height = 9f;
                    homingPower = 0.055f;
                    homingRange = 70f;
                    shootEffect = Fx.shootHeal;
                    smokeEffect = Fx.hitLaser;
                    hitEffect = Fx.hitLaser;
                    despawnEffect = Fx.hitLaser;
                    backColor = lightColor = Color.valueOf("8fe9ff");
                    frontColor = Color.white;
                    lightRadius = 20f;
                }};
            }});
        }};

        //================================================================================
        // ARACHNE - four-legged assault walker: main cannon, twin rotary guns, missile pod
        //================================================================================
        arachne = new ArachneUnitType("arachne"){{
            health = 4300f;
            armor = 9f;
            hitSize = 26f;
            speed = 0.62f;
            rotateSpeed = 3.2f;
            drag = 0.1f;
            legLength = 26f;
            legBaseOffset = 7.7f;
            legMoveSpace = 1.05f;
            legSpeed = 0.14f;
            legMinLength = 0.5f;
            legMaxLength = 1.2f;
            legForwardScl = 0.8f;
            legSplashDamage = 26f;
            legSplashRange = 22f;
            rippleScale = 1.6f;
            stepShake = 0.4f;
            stepSound = Sounds.mechStep;
            stepSoundVolume = 0.35f;
            targetAir = true;
            targetGround = true;
            itemCapacity = 60;

            //main cannon: fixed forward, the whole turret aims
            weapons.add(new LiftedWeapon(){{
                x = 0f;
                y = 5.9f;
                shootY = 11.2f;
                muzzleZ = 15.6f;
                reload = 92f;
                rotate = false;
                mirror = false;
                shootCone = 8f;
                inaccuracy = 0.8f;
                shake = 2.2f;
                showStatSprite = false;
                shootSound = Sounds.shootArtillery;
                ejectEffect = Fx.casing3;
                bullet = new BasicBulletType(7.2f, 175f){{
                    lifetime = 32f;
                    width = 11f;
                    height = 17f;
                    splashDamage = 60f;
                    splashDamageRadius = 26f;
                    knockback = 1.2f;
                    pierceCap = 2;
                    shootEffect = Fx.shootBig2;
                    smokeEffect = Fx.shootSmokeTitan;
                    hitEffect = despawnEffect = Fx.blastExplosion;
                    hitSound = Sounds.explosionArtillery;
                    backColor = lightColor = trailColor = amber;
                    frontColor = Color.white;
                    trailLength = 8;
                    trailWidth = 2.2f;
                }};
            }});

            //twin rotary machine guns, alternating
            weapons.add(new LiftedWeapon(){{
                x = 4.7f;
                y = 4.0f;
                shootY = 3.3f;
                muzzleZ = 15.3f;
                reload = 7f;
                rotate = false;
                mirror = true;
                alternate = true;
                shootCone = 18f;
                inaccuracy = 4.5f;
                velocityRnd = 0.08f;
                showStatSprite = false;
                shootSound = Sounds.shootDuo;
                ejectEffect = Fx.casing1;
                bullet = new BasicBulletType(6.4f, 15f){{
                    lifetime = 30f;
                    width = 5f;
                    height = 8f;
                    shootEffect = Fx.shootSmall;
                    smokeEffect = Fx.shootSmallSmoke;
                    hitEffect = despawnEffect = Fx.hitBulletSmall;
                    backColor = amber;
                    frontColor = Color.white;
                    collidesAir = true;
                }};
            }});

            //turret-top missile pod
            weapons.add(new LiftedWeapon(){{
                x = 0f;
                y = -2.9f;
                shootY = 2.3f;
                muzzleZ = 18f;
                reload = 150f;
                rotate = true;
                rotateSpeed = 4f;
                rotationLimit = 60f;
                mirror = false;
                shootCone = 30f;
                shake = 1f;
                showStatSprite = false;
                shootSound = Sounds.shootMissileSmall;
                ejectEffect = Fx.none;
                shoot.shots = 4;
                shoot.shotDelay = 6f;
                bullet = new MissileBulletType(3.1f, 38f){{
                    lifetime = 64f;
                    width = 6f;
                    height = 9f;
                    homingPower = 0.08f;
                    homingRange = 90f;
                    splashDamage = 30f;
                    splashDamageRadius = 22f;
                    weaveScale = 6f;
                    weaveMag = 1.2f;
                    trailColor = backColor = Color.valueOf("ffb45f");
                    frontColor = Color.white;
                    hitEffect = despawnEffect = Fx.blastExplosion;
                    collidesAir = true;
                }};
            }});
        }};

        //================================================================================
        // COLOSSUS - two-legged assault mech, independent 360-degree upper body
        //================================================================================
        titan = new TitanUnitType("titan"){{
            health = 11500f;
            armor = 16f;
            hitSize = 38f;
            speed = 0.52f;
            rotateSpeed = 4.8f;
            baseRotateSpeed = 8f;
            legTurnSpeed = 8f;
            legPhysicsLayer = false;
            targetAir = true;
            targetGround = true;
            crushDamage = 1.6f;
            itemCapacity = 140;
            buildSpeed = 0f;
            mineTier = -1;
            drownTimeMultiplier = 5f;
            mechStride = 10f;

            canBoost = true;
            boostMultiplier = 2.7f;
            riseSpeed = 0.026f;
            descentSpeed = 0.032f;
            boostWhenBuilding = false;
            fallSpeed = 0.012f;
            flyHeight = 20f;
            boostRange = 300f;
            landRange = 200f;

            abilities.add(new ForceFieldAbility(75f, 0.9f, 1200f, 60f * 10));

            //1/2: shoulder plasma cannons
            for(int i = 0; i < 2; i++){
                float side = i == 0 ? -1f : 1f;
                weapons.add(new LiftedWeapon(){{
                    x = side * 11.75f;
                    y = -0.25f;
                    shootY = 13.75f;
                    muzzleZ = 29.5f;
                    reload = 78f;
                    recoil = 0f;
                    rotate = true;
                    rotateSpeed = 6f;
                    rotationLimit = 30f;
                    mirror = false;
                    inaccuracy = 2.5f;
                    shootCone = 16f;
                    shake = 3.4f;
                    showStatSprite = false;
                    shootSound = Sounds.shootSmite;
                    chargeSound = Sounds.chargeLancer;
                    ejectEffect = Fx.none;
                    shoot.firstShotDelay = 24f;
                    parentizeEffects = false;
                    bullet = new BasicBulletType(6.2f, 160f){{
                        lifetime = 42f;
                        width = 14f;
                        height = 21f;
                        shrinkY = 0.2f;
                        splashDamage = 75f;
                        splashDamageRadius = 34f;
                        knockback = 1.6f;
                        pierceCap = 2;
                        pierceBuilding = false;
                        chargeEffect = Fx.none;
                        shootEffect = VoxelFx.titanShoot;
                        smokeEffect = Fx.none;
                        hitEffect = despawnEffect = Fx.blastExplosion;
                        hitSound = Sounds.explosionArtillery;
                        trailLength = 14;
                        trailWidth = 2.6f;
                        trailColor = backColor = lightColor = cyan;
                        frontColor = Color.white;
                        lightRadius = 32f;
                        lightOpacity = 0.6f;
                    }};
                }});
            }

            //3/4: arm autocannons
            for(int i = 0; i < 2; i++){
                float side = i == 0 ? -1f : 1f;
                weapons.add(new LiftedWeapon(){{
                    x = side * 10.25f;
                    y = 1.75f;
                    shootY = 11.75f;
                    muzzleZ = 19.5f;
                    reload = 11f;
                    recoil = 0f;
                    rotate = false;
                    mirror = false;
                    inaccuracy = 6f;
                    shootCone = 26f;
                    shake = 0.5f;
                    showStatSprite = false;
                    shootSound = Sounds.shootScepter;
                    ejectEffect = Fx.casing1;
                    bullet = new BasicBulletType(7.4f, 34f){{
                        lifetime = 26f;
                        width = 7f;
                        height = 11f;
                        homingPower = 0.04f;
                        homingRange = 60f;
                        shootEffect = VoxelFx.titanGunFlash;
                        smokeEffect = Fx.none;
                        hitEffect = despawnEffect = Fx.hitBulletSmall;
                        backColor = lightColor = Color.valueOf("9ff0ff");
                        frontColor = Color.white;
                        trailLength = 6;
                        trailWidth = 1.1f;
                        trailColor = Color.valueOf("9ff0ff");
                    }};
                }});
            }

            //5/6: backpack missile racks
            for(int i = 0; i < 2; i++){
                float side = i == 0 ? -1f : 1f;
                weapons.add(new LiftedWeapon(){{
                    x = side * 3.5f;
                    y = -7.25f;
                    shootY = 3f;
                    muzzleZ = 32f;
                    reload = 190f;
                    recoil = 0f;
                    rotate = true;
                    rotateSpeed = 5f;
                    rotationLimit = 90f;
                    mirror = false;
                    inaccuracy = 0f;
                    shootCone = 14f;
                    shake = 1.8f;
                    showStatSprite = false;
                    shootSound = Sounds.shootMissile;
                    ejectEffect = Fx.none;
                    parentizeEffects = false;
                    shoot.shots = 2;
                    shoot.shotDelay = 9f;
                    bullet = new MissileBulletType(2.7f, 95f){{
                        lifetime = 110f;
                        width = 7f;
                        height = 10f;
                        shrinkY = 0f;
                        drag = -0.002f;
                        homingPower = 0.07f;
                        homingRange = 110f;
                        weaveScale = 7f;
                        weaveMag = 1.7f;
                        splashDamage = 55f;
                        splashDamageRadius = 30f;
                        trailLength = 9;
                        trailWidth = 1.5f;
                        trailColor = backColor = Color.valueOf("9ff0ff");
                        frontColor = Color.white;
                        shootEffect = VoxelFx.titanMissileLaunch;
                        smokeEffect = Fx.none;
                        hitEffect = despawnEffect = Fx.blastExplosion;
                        hitSound = Sounds.explosionMissile;
                        lightRadius = 26f;
                        lightOpacity = 0.55f;
                    }};
                }});
            }
        }};
    }
}
