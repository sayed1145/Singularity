package sixfold;

import arc.Core;
import blackhole.BHTechTree;
import blackhole.BHItems;
import arc.graphics.Color;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Planets;
import mindustry.content.StatusEffects;
import mindustry.entities.bullet.ArtilleryBulletType;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.MissileBulletType;
import mindustry.entities.bullet.RailBulletType;
import mindustry.gen.LegsUnit;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.mod.Mod;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.content.TechTree.TechNode;
import mindustry.game.Objectives.Produce;
import mindustry.ctype.UnlockableContent;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.type.Category;

import static mindustry.type.ItemStack.with;

/** v9 / Android: Java classes plus a classes.dex are bundled into the installable jar.
 * v3 adds the Pyroclast Titan (original ember-halo / flare-fin / magma-core animation,
 * magma artillery with burning shards, twin flame sweepers and the Ember Crown nova),
 * the ember-burn status, second bay plans and second-planet-capable research nodes.
 * Serpulo and Erekir each keep a complete crafting -> item research ->
 * factory -> titan path; the six v1 turrets remain as HJSON. */
public class SixfoldMod extends Mod{
    public static Item aegisAlloy, signalCore;
    public static StatusEffect assault, emberBurn, tether;
    public static UnitType titan, pyroclast;
    /** v7.7: one single Aether Press and one single Signal Encoder, both planet neutral. */
    public static Block forgeSerpulo, encoderSerpulo;
    public static Block baySerpulo;
    public static Block loom, well;

    public static boolean isTitan(Unit u){
        return u.type == titan || u.type == pyroclast;
    }

    @Override
    public void loadContent(){
        loadItems();
        loadStatus();
        loadTitan();
        loadPyroclast();
        loadIndustry();
        loadTurrets();
        loadResearch();
        Log.info("[blackhole/aegis] 2 imported Titans, 2 materials, 2 research routes, 20 blocks registered.");
    }

    @Override
    public void init(){
        if(Vars.headless){
            // Late enough for the six bundled HJSON turrets to have been parsed.
            //v7.8: the research of this pack lives in the Aurelia tree only (built later, in
            //AureliaMerge.retree()), so here we only verify the content itself and the bay plans.
            if(((UnitFactory)baySerpulo).plans.size != 2 ||
                ((UnitFactory)baySerpulo).plans.first().unit != titan ||
                ((UnitFactory)baySerpulo).plans.get(1).unit != pyroclast){
                throw new IllegalStateException("Sixfold production chain broken: bayPlans=" + ((UnitFactory)baySerpulo).plans.size);
            }
            for(String name : new String[]{"needlefall", "sky-net", "prism-lance", "crater", "boreline", "halo", "loom", "well"}){
                if(Vars.content.block("blackhole-" + name) == null){
                    throw new IllegalStateException("Sixfold turret missing: " + name);
                }
            }
            Log.info("[blackhole/aegis] checked: the Titan Bay plans and ten turrets (research lives in the Aurelia tree).");
        }
    }

    private static void loadItems(){
        aegisAlloy = new Item("aegis-alloy", Color.valueOf("8fd9bf")){{
            cost = 1.7f;
            healthScaling = 0.25f;
            shownPlanets.add(Planets.serpulo);
            shownPlanets.add(Planets.erekir);
        }};
        signalCore = new Item("signal-core", Color.valueOf("c5a0fc")){{
            cost = 2.1f;
            charge = 0.7f;
            shownPlanets.add(Planets.serpulo);
            shownPlanets.add(Planets.erekir);
        }};
    }

    private static void loadStatus(){
        assault = new StatusEffect("titan-overclock"){{
            color = Color.valueOf("ab98f1");
            damageMultiplier = 1.22f;
            reloadMultiplier = 1.18f;
            speedMultiplier = 1.08f;
            show = false;
        }};
        emberBurn = new StatusEffect("ember-burn"){{
            color = Color.valueOf("ff9e5c");
            damage = 1.15f;
            show = true;
        }};
        tether = new StatusEffect("grav-tether"){{
            color = Color.valueOf("9ecbff");
            speedMultiplier = 0.45f;
            reloadMultiplier = 0.8f;
            show = true;
        }};
    }

    private static void loadTitan(){
        titan = new blackhole.g3d.Walker3D("aegis-titan"){{
            builder = blackhole.models.TitanModels::aegis;
            modelScale = 2f;
            shownPlanets.add(Planets.serpulo);
            shownPlanets.add(Planets.erekir);
            health = 42000f;
            armor = 28f;
            hitSize = 39f;
            speed = 0.46f;
            rotateSpeed = 1.45f;
            drag = 0.14f;
            //own 3D gait (Walker3D): vanilla legs are off, steps below
            stepDamage = 95f;
            stepRange = 46f;
            stepSnd = Sounds.walkerStep;
            stepVolume = 1.05f;
            stepShakeAmount = 1.25f;
            stepEffect = TitanEffects.step;
            stepColor = Color.valueOf("c9d1cf");
            flashColor = Color.valueOf("8ce5d3");
            lightColor = Color.valueOf("8ce5d3");
            lightRadius = 130f;
            drawCell = false;

            abilities.add(new blackhole.PrismWardAbility(73f, 2.1f, 1900f, 800f){{ color = Color.valueOf("8ce5d3"); height = 16f; }});

            weapons.add(new blackhole.g3d.LiftedWeapon("blackhole-titan-lance"){{
                mirror = false;
                top = true;
                x = 0f;
                y = 3f;
                shootY = 20f;
                reload = 215f;
                recoil = 4.5f;
                shake = 3.2f;
                shootSound = Sounds.shootForeshadow;
                chargeSound = Sounds.chargeLancer;
                shoot.firstShotDelay = 42f;
                bullet = new RailBulletType(){{
                    damage = 540f;
                    length = 370f;
                    pierceCap = 5;
                    pierceDamageFactor = 0.27f;
                    hitColor = trailColor = Color.valueOf("8ce5d3");
                    chargeEffect = TitanEffects.charge;
                    pierceEffect = TitanEffects.fracture;
                    pointEffect = TitanEffects.fracture;
                    pointEffectSpace = 58f;
                    endEffect = TitanEffects.fracture;
                    lineEffect = TitanEffects.railLine;
                }};
            }});

            weapons.add(new blackhole.g3d.LiftedWeapon("blackhole-titan-pod"){{
                x = 16.5f;
                y = -2f;
                shootY = 10f;
                mirror = true;
                alternate = true;
                reload = 66f;
                recoil = 2.5f;
                rotate = true;
                rotateSpeed = 3.0f;
                shootSound = Sounds.shootMissileLarge;
                bullet = new MissileBulletType(5.1f, 100f){{
                    sprite = "blackhole-titan-missile";
                    lifetime = 70f;
                    width = 11f;
                    height = 15f;
                    homingPower = 0.085f;
                    homingRange = 108f;
                    splashDamage = 75f;
                    splashDamageRadius = 30f;
                    frontColor = Color.valueOf("fff5ed");
                    backColor = trailColor = hitColor = Color.valueOf("b89bff");
                    hitEffect = despawnEffect = TitanEffects.fracture;
                }};
            }});
        }};
    }

    private static void loadPyroclast(){
        pyroclast = new blackhole.g3d.Walker3D("pyroclast-titan"){{
            builder = blackhole.models.TitanModels::pyroclast;
            modelScale = 1.8f;
            shownPlanets.add(Planets.serpulo);
            shownPlanets.add(Planets.erekir);
            health = 36000f;
            armor = 24f;
            hitSize = 34f;
            speed = 0.62f;
            rotateSpeed = 2.1f;
            drag = 0.12f;
            stepDamage = 70f;
            stepRange = 34f;
            stepSnd = Sounds.walkerStep;
            stepVolume = 0.9f;
            stepShakeAmount = 1.0f;
            stepEffect = EmberEffects.step;
            stepColor = Color.valueOf("ff9e5c");
            flashColor = Color.valueOf("ff9e5c");
            lightColor = Color.valueOf("ff9e5c");
            lightRadius = 110f;
            drawCell = false;
            immunities.add(StatusEffects.burning);

            abilities.add(new EmberCrownAbility(120f, 130f, 60f, 150f));

            // Cataclysm turret: a rotating twin-cannon turret (no longer a mortar) firing a flat, fast magma
            // shell that still scatters burning shards on impact.
            weapons.add(new blackhole.g3d.LiftedWeapon("blackhole-cataclysm"){{
                mirror = false;
                top = true;
                rotate = true;
                rotateSpeed = 2.4f;
                barrelSpread = 3.1f;
                x = 0f;
                y = 4f;
                shootY = 18f;
                reload = 150f;
                recoil = 5f;
                shake = 4f;
                inaccuracy = 3f;
                shoot = new mindustry.entities.pattern.ShootAlternate(3.1f);
                shootSound = Sounds.shootScepter;
                bullet = new BasicBulletType(7.4f, 640f){{
                    sprite = "blackhole-ember-shell";
                    width = 15f;
                    height = 15f;
                    lifetime = 84f;
                    trailLength = 9;
                    trailWidth = 2.1f;
                    shrinkY = 0f;
                    splashDamage = 190f;
                    splashDamageRadius = 52f;
                    buildingDamageMultiplier = 0.8f;
                    collidesTiles = true;
                    hitShake = 2f;
                    status = emberBurn;
                    statusDuration = 300f;
                    frontColor = Color.valueOf("f2e6d8");
                    backColor = trailColor = Color.valueOf("ff9e5c");
                    hitEffect = despawnEffect = EmberEffects.burst;
                    fragBullets = 8;
                    fragVelocityMin = 0.4f;
                    fragBullet = new BasicBulletType(3.2f, 46f){{
                        sprite = "blackhole-ember-shard";
                        width = 7f;
                        height = 7f;
                        lifetime = 26f;
                        status = emberBurn;
                        statusDuration = 180f;
                        frontColor = Color.valueOf("f2e6d8");
                        backColor = trailColor = Color.valueOf("e05545");
                        hitEffect = despawnEffect = EmberEffects.vent;
                    }};
                }};
            }});

            // Twin close-range flame sweepers.
            weapons.add(new blackhole.g3d.LiftedWeapon("blackhole-sweeper"){{
                x = 14.5f;
                y = -4f;
                shootY = 8f;
                mirror = true;
                alternate = true;
                reload = 22f;
                recoil = 1.6f;
                rotate = true;
                rotateSpeed = 4.5f;
                inaccuracy = 6f;
                shootSound = Sounds.shootFlame;
                bullet = new BasicBulletType(7f, 40f){{
                    sprite = "blackhole-ember-bolt";
                    lifetime = 24f;
                    width = 9f;
                    height = 12f;
                    pierce = true;
                    pierceCap = 2;
                    status = emberBurn;
                    statusDuration = 200f;
                    frontColor = Color.valueOf("f2e6d8");
                    backColor = trailColor = Color.valueOf("e05545");
                    shootEffect = hitEffect = despawnEffect = EmberEffects.vent;
                }};
            }});
        }};
    }

    private static void loadTurrets(){
        // Loom: harmonic cycle turret (kinetic -> thermal -> volt, repeating).
        loom = new CycleTurret("loom"){{
            requirements(Category.turret, with(Items.silicon, 260, Items.plastanium, 150, signalCore, 12));
            shownPlanets.add(Planets.serpulo);
            size = 3;
            health = 1600;
            range = 250f;
            reload = 46f;
            rotateSpeed = 4.5f;
            inaccuracy = 2f;
            recoil = 3f;
            shake = 2f;
            shootY = 7f;
            shootSound = Sounds.shootLocus;
            consumePower(7f);
            cycle.addAll(
                new BasicBulletType(6.5f, 95f){{
                    sprite = "blackhole-loom-bolt";
                    width = 9f; height = 13f; lifetime = 40f;
                    pierce = true; pierceCap = 3;
                    frontColor = Color.valueOf("f2e6d8");
                    backColor = trailColor = Color.valueOf("9ecbff");
                    hitEffect = despawnEffect = EmberEffects.vent;
                }},
                new BasicBulletType(5f, 55f){{
                    sprite = "blackhole-loom-thermal";
                    width = 9f; height = 13f; lifetime = 52f;
                    splashDamage = 34f; splashDamageRadius = 26f;
                    status = emberBurn; statusDuration = 240f;
                    frontColor = Color.valueOf("f2e6d8");
                    backColor = trailColor = Color.valueOf("ff9e5c");
                    hitEffect = despawnEffect = EmberEffects.burst;
                }},
                new BasicBulletType(7.5f, 35f){{
                    sprite = "blackhole-loom-volt";
                    width = 9f; height = 13f; lifetime = 34f;
                    lightning = 3; lightningLength = 10; lightningDamage = 28f;
                    status = StatusEffects.shocked; statusDuration = 120f;
                    frontColor = Color.valueOf("e8defc");
                    backColor = trailColor = Color.valueOf("bb9bee");
                    hitEffect = despawnEffect = EmberEffects.vent;
                }}
            );
        }};

        // Gravwell: tether orb turret (slow + pull, collapse damage scales with catches).
        well = new TetherTurret("well"){{
            requirements(Category.turret, with(Items.titanium, 300, Items.surgeAlloy, 120, signalCore, 18));
            shownPlanets.add(Planets.serpulo);
            size = 3;
            health = 1500;
            range = 260f;
            reload = 210f;
            recoil = 4f;
            shake = 3f;
            shootY = 6f;
            shootSound = Sounds.shootRetusa;
            consumePower(9f);
            shootType = new TetherBulletType(1.5f, 110f){{
                sprite = "blackhole-tether-orb";
                width = 12f; height = 12f;
                lifetime = 140f;
                splashDamage = 60f; splashDamageRadius = 46f;
                tetherRadius = 80f;
                despawnEffect = hitEffect = EmberEffects.burst;
            }};
        }};

    }

    private static void loadIndustry(){
        // Both planets output the *same* two modded items, but require different
        // native raw materials. No imported ore is required on existing maps.
        //v7.7: ONE Aether Press. The two planet twins made the same alloy from different raws, so they are
        //merged into a single planet-neutral machine: silicon and graphite exist on Serpulo and on Erekir alike,
        //and the mod's own degenerate matter is produced by the Singularity line on either planet.
        forgeSerpulo = new GenericCrafter("aegis-press-s"){{
            hasPower = true;
            requirements(Category.crafting, with(Items.silicon, 180, Items.graphite, 210, BHItems.degenerateMatter, 40));
            size = 3;
            health = 900;
            itemCapacity = 45;
            craftTime = 126f;
            outputItem = new ItemStack(aegisAlloy, 2);
            consumeItems(with(Items.silicon, 4, Items.graphite, 5, BHItems.degenerateMatter, 1));
            consumePower(5.5f);
        }};

        //v7.7: ONE Signal Encoder, same reasoning as the press above.
        encoderSerpulo = new GenericCrafter("signal-encoder-s"){{
            hasPower = true;
            requirements(Category.crafting, with(Items.silicon, 240, Items.graphite, 170, aegisAlloy, 90));
            size = 3;
            health = 950;
            itemCapacity = 45;
            craftTime = 180f;
            outputItem = new ItemStack(signalCore, 1);
            consumeItems(with(aegisAlloy, 3, Items.silicon, 7, BHItems.hawkingDust, 1));
            consumePower(6.8f);
        }};

        baySerpulo = new blackhole.models.Assembler3D("titan-bay-s"){{
            //v7.6: the Erekir twin is gone - this is the one and only Titan Bay, so its recipe is planet neutral
            //(both alloys are produced by either home-planet forge chain) and it shows up on every planet.
            requirements(Category.units, with(aegisAlloy, 260, signalCore, 95));
            shownPlanets.add(Planets.serpulo);
            shownPlanets.add(Planets.erekir);
            size = 5;
            health = 2500;
            itemCapacity = 220;
            plans = Seq.with(new UnitFactory.UnitPlan(titan, 60f * 230f,
                with(aegisAlloy, 75, signalCore, 35, Items.phaseFabric, 130, Items.surgeAlloy, 110,
                    BHItems.singularityCore, 12)));
            plans.add(new UnitFactory.UnitPlan(pyroclast, 60f * 300f,
                with(aegisAlloy, 110, signalCore, 55, Items.phaseFabric, 160, Items.surgeAlloy, 140,
                    BHItems.singularityCore, 18)));
            consumePower(13.5f);
        }};
    }

    private static TechNode link(UnlockableContent parent, UnlockableContent content, ItemStack[] cost){
        if(parent.techNode == null) throw new IllegalStateException("No vanilla tech node for " + parent.name);
        return new TechNode(parent.techNode, content, cost);
    }

    private static TechNode link(TechNode parent, UnlockableContent content, ItemStack[] cost){
        if(parent == null) throw new IllegalStateException("Null tech parent for " + content.name);
        return new TechNode(parent, content, cost);
    }

    private static TechNode produced(TechNode parent, Item item, ItemStack[] cost){
        TechNode result = link(parent, item, cost);
        result.objectives.add(new Produce(item));
        return result;
    }

    /**
     * v7.8 - the expedition is an Aurelia mod. No node is added to the Serpulo or Erekir trees any more;
     * {@code AureliaMerge.retree()} hangs every block and unit of this pack into the Aurelia tree instead.
     */
    private static void loadResearch(){
    }
}
