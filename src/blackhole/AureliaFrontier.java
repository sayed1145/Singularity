package blackhole;

import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import blackhole.g3d.LiftedWeapon;
import blackhole.g3d.Ship3D;
import blackhole.g3d.Tank3D;
import blackhole.models.FrontierUnits;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.pattern.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.units.UnitFactory.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static blackhole.AureliaContent.*;
import static mindustry.type.ItemStack.with;

/**
 * v7.6 Aurelia frontier pack.
 *
 * <p>Two new fluids and the industry that makes them, a ropeway that moves items over the terrain instead of
 * through it, two faster transport lines, three defensive structures built on mechanics that do not exist in
 * vanilla (a gravity trap, a damage bank that fires back, a chain coil), a cheap early unit cradle, logic memory
 * that is shared across the map, and three units - ground, air and naval - with attack patterns the planet did
 * not have before.
 *
 * <p>Balance rule for the whole pack: nothing already in the mod changed. Every number here was chosen against
 * the existing Aurelia line (lumen conveyor 6.3 items/s, shard volley 22-36 damage, aurelia fabricator 2.4 power)
 * so the new blocks sit one step above their neighbours in cost and one step above in capability.
 */
public final class AureliaFrontier{
    public static Liquid auroraCoolant, gravitonSlurry;
    public static Block coolantCondenser, gravitonChurn, fluxConveyor, fluxConduit, skylineCableway,
        gravityWell, aegisLattice, tempestCoil, resonanceCradle, latticeLink;
    public static UnitType mote, latticeCrawler, tideLancer;

    /** Everything this pack adds, in tech order. */
    public static final Seq<UnlockableContent> added = new Seq<>();

    private AureliaFrontier(){}

    static <T extends UnlockableContent> T add(T c){
        added.add(c);
        own.add(c);
        return c;
    }

    public static void load(){
        loadLiquids();
        loadUnits();
        loadBlocks();
    }

    private static void loadLiquids(){
        //a cryogenic working fluid: turret coolant and crafter booster, made from tidewater
        auroraCoolant = add(new Liquid("aurora-coolant", Color.valueOf("a9f0ff")){{
            heatCapacity = 0.75f;
            temperature = 0.28f;
            viscosity = 0.35f;
            explosiveness = 0f;
            flammability = 0f;
            coolant = true;
            effect = brineSoaked;
        }});
        //heavy, slow, faintly luminous: the fuel of the gravity tier
        gravitonSlurry = add(new Liquid("graviton-slurry", Color.valueOf("9a7cff")){{
            heatCapacity = 0.4f;
            temperature = 0.6f;
            viscosity = 0.82f;
            explosiveness = 0.25f;
            flammability = 0f;
            effect = resonanceShock;
        }});
    }

    private static void loadUnits(){
        // ---------------------------------------------------------------- air: the mote
        //a cheap swarm drone available almost immediately: it mines, it rebuilds, and it dies easily.
        mote = add(new Ship3D("mote"){{
            builder = FrontierUnits::mote;
            flying = true;
            lowAltitude = false;
            speed = 3.1f;
            accel = 0.1f;
            drag = 0.05f;
            rotateSpeed = 9f;
            health = 170f;
            armor = 0f;
            hitSize = 8f;
            range = 72f;
            hover = 5f;
            engineColor = AureliaFx.lumen;
            mineTier = 3;
            mineSpeed = 4.2f;
            buildSpeed = 0.5f;
            targetAir = true;
            targetGround = true;
            weapons.add(new LiftedWeapon(){{
                x = 0f; y = 3.2f; mirror = false; rotate = false; reload = 34f;
                shootSound = BHSounds.pulse;
                bullet = new BasicBulletType(5.2f, 11f){{
                    lifetime = 22f;
                    width = 5f; height = 8f;
                    frontColor = Color.white;
                    backColor = trailColor = hitColor = AureliaFx.lumen;
                    hitEffect = despawnEffect = AureliaFx.lumenHit;
                    shootEffect = OwnFx.muzzle;
                    smokeEffect = OwnFx.muzzleSmoke;
                }};
            }});
        }});

        // ---------------------------------------------------------------- ground: the lattice crawler
        //new attack: it lobs a resonance charge that does nothing on impact and instead sits on the ground for a
        //moment before collapsing into a ring of shards - area denial rather than a bigger gun.
        latticeCrawler = add(new Tank3D("lattice-crawler"){{
            builder = FrontierUnits::latticeCrawler;
            speed = 0.72f;
            hitSize = 17f;
            health = 1250f;
            armor = 9f;
            rotateSpeed = 2.6f;
            omniMovement = false;
            range = 215f;
            targetAir = false;
            targetGround = true;
            weapons.add(new LiftedWeapon(){{
                mirror = false; rotate = true; rotateSpeed = 2.2f; top = true; reload = 95f; recoil = 2.4f;
                inaccuracy = 2f;
                shootSound = BHSounds.thump;
                shoot = new ShootSpread(2, 7f);
                bullet = new ArtilleryBulletType(2.6f, 38f){{
                    lifetime = 84f;
                    width = 13f; height = 13f;
                    collidesTiles = false;
                    splashDamage = 46f;
                    splashDamageRadius = 26f;
                    frontColor = Color.white;
                    backColor = trailColor = hitColor = AureliaFx.resonance;
                    trailLength = 12; trailWidth = 2.4f;
                    hitEffect = despawnEffect = AureliaFx.detainImpact;
                    shootEffect = OwnFx.muzzle;
                    smokeEffect = OwnFx.muzzleSmoke;
                    status = latticeGrip;
                    statusDuration = 140f;
                    //the collapse: six shards thrown outwards where the charge lands
                    fragBullets = 6;
                    fragLifeMin = 0.3f;
                    fragBullet = new BasicBulletType(3.4f, 16f){{
                        lifetime = 22f;
                        width = 6f; height = 9f;
                        pierce = true; pierceCap = 2;
                        frontColor = Color.white;
                        backColor = trailColor = hitColor = AureliaFx.resonance;
                        hitEffect = despawnEffect = AureliaFx.lumenHit;
                        collidesAir = false;
                    }};
                }};
            }});
        }});

        // ---------------------------------------------------------------- naval: the tide lancer
        //new attack: torpedoes. They run slowly under the surface, cannot be shot at by anything that only sees
        //air, and hit far harder than any gun of the same tier - a naval unit that threatens shorelines.
        tideLancer = add(new Ship3D("tide-lancer"){{
            builder = FrontierUnits::tideLancer;
            flying = false;
            naval = true;
            constructor = UnitWaterMove::create;
            hover = 0f;
            bankScale = 5f;
            pitchScale = 3f;
            speed = 1.05f;
            drag = 0.14f;
            accel = 0.32f;
            health = 1450f;
            armor = 7f;
            hitSize = 19f;
            range = 205f;
            engineColor = AureliaFx.lumen;
            targetAir = false;
            targetGround = true;
            weapons.add(new LiftedWeapon(){{
                x = 0f; y = 2.4f; mirror = false; rotate = true; rotateSpeed = 2.6f; reload = 110f; recoil = 2f;
                shootSound = BHSounds.thump;
                shoot = new ShootAlternate(3.4f);
                bullet = new BasicBulletType(2.4f, 125f){{
                    lifetime = 95f;
                    width = 9f; height = 17f;
                    collidesAir = false;
                    collidesTiles = true;
                    splashDamage = 70f;
                    splashDamageRadius = 30f;
                    frontColor = Color.white;
                    backColor = trailColor = hitColor = Color.valueOf("3f7fc0");
                    trailLength = 22; trailWidth = 2.2f;
                    trailEffect = OwnFx.trail;
                    trailChance = 0.3f;
                    hitEffect = despawnEffect = AureliaFx.detainImpact;
                    shootEffect = OwnFx.muzzle;
                    smokeEffect = OwnFx.muzzleSmoke;
                    status = brineSoaked;
                    statusDuration = 200f;
                    homingPower = 0.04f;
                    homingRange = 90f;
                }};
            }});
            weapons.add(new LiftedWeapon(){{
                x = 0f; y = -4.4f; mirror = false; rotate = true; rotateSpeed = 5f; reload = 26f;
                shootSound = BHSounds.pulse;
                bullet = new BasicBulletType(5.4f, 20f){{
                    lifetime = 30f;
                    width = 6f; height = 10f;
                    frontColor = Color.white;
                    backColor = trailColor = hitColor = AureliaFx.lumen;
                    hitEffect = despawnEffect = AureliaFx.lumenHit;
                    shootEffect = OwnFx.muzzle;
                    smokeEffect = OwnFx.muzzleSmoke;
                }};
            }});
        }});
    }

    private static void loadBlocks(){
        // ---------------------------------------------------------------- new fluid industry
        coolantCondenser = add(new GenericCrafter("coolant-condenser"){{
            requirements(Category.crafting, with(lumenite, 110, aurite, 70, prismGlass, 60));
            size = 3;
            health = 720;
            craftTime = 60f;
            hasLiquids = true;
            liquidCapacity = 60f;
            outputLiquid = new LiquidStack(auroraCoolant, 0.2f);
            consumePower(1.8f);
            consumeLiquid(tidewater, 0.26f);
            envEnabled = Env.any;
            craftEffect = OwnFx.smoke;
        }});

        gravitonChurn = add(new GenericCrafter("graviton-churn"){{
            requirements(Category.crafting, with(lumenite, 420, aurite, 300, prismAlloy, 160, concordCore, 40));
            size = 4;
            health = 1700;
            craftTime = 90f;
            hasLiquids = true;
            liquidCapacity = 80f;
            itemCapacity = 30;
            outputLiquid = new LiquidStack(gravitonSlurry, 0.12f);
            consumePower(5.5f);
            consumeItem(aurite, 2);
            consumeLiquid(lumenPlasma, 0.12f);
            envEnabled = Env.any;
            craftEffect = AureliaFx.lumenHit;
        }});

        // ---------------------------------------------------------------- faster transport
        fluxConveyor = add(new AureliaParts.LumenConveyor("flux-conveyor"){{
            requirements(Category.distribution, with(lumenite, 2, prismGlass, 1, prismAlloy, 1));
            health = 75;
            speed = 0.098f;
            displayedSpeed = 13.2f;
            buildCostMultiplier = 1.4f;
            junction = lumenJunction;
            bridge = lumenBridge;
        }});

        fluxConduit = add(new AureliaParts.LumenConduit("flux-conduit"){{
            requirements(Category.liquid, with(prismGlass, 2, prismAlloy, 1));
            health = 70;
            liquidCapacity = 22f;
            liquidPressure = 1.9f;
        }});

        //the new transport mechanic: a ropeway. Pods fly over walls, water and enemy lines.
        skylineCableway = add(new FrontierParts.Cableway("skyline-cableway"){{
            requirements(Category.distribution, with(lumenite, 140, aurite, 90, prismGlass, 60, prismAlloy, 35));
            size = 2;
            health = 520;
            range = 26;
            podItems = 5;
            maxPods = 6;
            podSpeed = 0.5f;
            loadTime = 9f;
            itemCapacity = 30;
        }});

        // ---------------------------------------------------------------- defence with new mechanics
        gravityWell = add(new FrontierParts.GravityWell("gravity-well"){{
            requirements(Category.effect, with(lumenite, 280, aurite, 190, prismAlloy, 120, resonanceShard, 80));
            size = 3;
            health = 1150;
            radius = 112f;
            crushRadius = 24f;
            pull = 1.5f;
            crushDps = 95f;
            apply = latticeGrip;
            consumePower(4.2f);
            //graviton slurry deepens the well instead of making it faster
            consumeLiquid(gravitonSlurry, 0.05f).boost();
        }});

        aegisLattice = add(new FrontierParts.AegisLattice("aegis-lattice"){{
            requirements(Category.effect, with(lumenite, 240, prismGlass, 120, prismAlloy, 140, concordCore, 30));
            size = 3;
            health = 1400;
            radius = 92f;
            capacity = 1400f;
            waveScale = 0.85f;
            waveRadius = 104f;
            consumePower(3.6f);
        }});

        //chain coil: one bolt that walks from target to target, hitting harder with every jump
        tempestCoil = add(new PowerTurret("tempest-coil"){{
            requirements(Category.turret, with(aurite, 130, prismGlass, 70, resonanceShard, 45));
            size = 2;
            health = 640;
            range = 196f;
            reload = 46f;
            rotateSpeed = 6f;
            recoil = 0.6f;
            shootSound = BHSounds.pulse;
            targetAir = true;
            targetGround = true;
            consumePower(3.4f);
            coolant = new ConsumeLiquid(auroraCoolant, 0.1f);
            consume(coolant);
            shootType = chainBolt();
            envEnabled = Env.any;
        }});

        // ---------------------------------------------------------------- functional
        //the early unit line: cheap, small, available right after the first drills
        resonanceCradle = add(new AureliaUnitFactory("resonance-cradle"){{
            requirements(Category.units, with(lumenite, 90, aurite, 45));
            size = 2;
            health = 520;
            consumePower(0.8f);
            plans.add(new UnitPlan(mote, 60f * 11f, with(lumenite, 18, aurite, 10)));
            plans.add(new UnitPlan(latticeCrawler, 60f * 42f, with(lumenite, 90, aurite, 60, resonanceShard, 18)));
            envEnabled = Env.any;
        }});

        latticeLink = add(new FrontierParts.LatticeLink("lattice-link"){{
            requirements(Category.logic, with(prismGlass, 30, aurite, 25, prismAlloy, 10));
            size = 1;
            health = 160;
            memoryCapacity = 64;
        }});
    }

    /** The tempest coil's bolt: on every hit it looks for the next victim and jumps, 22% stronger each time. */
    static BulletType chainBolt(){
        BasicBulletType bolt = new BasicBulletType(6.4f, 34f){
            @Override
            public void hitEntity(Bullet b, Hitboxc entity, float health){
                super.hitEntity(b, entity, health);
                chain(b, entity instanceof Unit u ? u : null);
            }
        };
        bolt.lifetime = 32f;
        bolt.width = 7f;
        bolt.height = 12f;
        bolt.pierce = false;
        bolt.frontColor = Color.white;
        bolt.backColor = bolt.trailColor = bolt.hitColor = Color.valueOf("b69cff");
        bolt.trailLength = 10;
        bolt.trailWidth = 2.1f;
        bolt.hitEffect = bolt.despawnEffect = AureliaFx.lumenHit;
        bolt.shootEffect = OwnFx.muzzle;
        bolt.smokeEffect = OwnFx.muzzleSmoke;
        bolt.status = resonanceShock;
        bolt.statusDuration = 90f;
        bolt.lightning = 0;
        return bolt;
    }

    /** Up to four jumps, each to the nearest enemy not hit yet, each 22% harder than the last. */
    static void chain(Bullet b, @Nullable Unit from){
        if(from == null || b.owner == null) return;
        float damage = b.damage * 1.22f;
        float x = from.x, y = from.y;
        Unit last = from;
        IntSeq hit = new IntSeq();
        hit.add(from.id);
        for(int i = 0; i < 4; i++){
            final float fx = x, fy = y;
            Unit next = Units.closestEnemy(b.team, fx, fy, 64f, u -> !hit.contains(u.id) && !u.dead);
            if(next == null) break;
            hit.add(next.id);
            Lightning.create(b.team, Color.valueOf("b69cff"), damage, fx, fy, Angles.angle(fx, fy, next.x, next.y),
                (int)(Mathf.dst(fx, fy, next.x, next.y) / 2f) + 4);
            next.damage(damage);
            if(AureliaContent.resonanceShock != null) next.apply(AureliaContent.resonanceShock, 90f);
            damage *= 1.22f;
            x = next.x;
            y = next.y;
            last = next;
        }
        if(last != from) AureliaFx.detainDischarge.at(last.x, last.y, 0f, Color.valueOf("b69cff"));
    }
}
