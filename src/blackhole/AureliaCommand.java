package blackhole;

import arc.graphics.*;
import arc.struct.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.entities.pattern.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.units.UnitFactory.*;
import mindustry.world.meta.*;

import static blackhole.AureliaContent.*;
import static mindustry.type.ItemStack.with;

/**
 * v8.2 content pack: the two overdrive projectors and the titan.
 *
 * <ul>
 * <li><b>Lumen accelerator</b> (2x2) - the small range overdrive. Plain, cheap, early.</li>
 * <li><b>Prism overcharger</b> (3x3) - the strong one: wider field, higher ceiling, optional resonance boost.
 *     Overlapping projectors do not add up (vanilla rule), so the ceiling here is the ceiling for the base.</li>
 * <li><b>Prism monolith</b> - a titan-class siege platform. Not a walker and not another tank hull: a gravity
 *     sled, a single armoured slab riding three lift pods, with one vented lance and two air pods. The lance
 *     has a shot bank that overheats, which is the hard cap on what the unit can put out.</li>
 * </ul>
 */
public final class AureliaCommand{
    public static Block lumenAccelerator, prismOvercharger;
    public static Tank3D prismMonolith;

    /** everything this pack adds, in tree order */
    public static final Seq<UnlockableContent> all = new Seq<>();

    private static boolean loaded;

    private AureliaCommand(){}

    static <T extends UnlockableContent> T add(T c){
        all.add(c);
        AureliaContent.own.add(c);
        return c;
    }

    public static void load(){
        if(loaded) return;
        loaded = true;
        loadUnits();
        loadBlocks();
        loadPlans();
    }

    // =====================================================================================================
    // the titan
    // =====================================================================================================

    private static void loadUnits(){
        prismMonolith = add(new Tank3D("prism-monolith"){{
            builder = CommandUnits::prismMonolith;
            //ground unit: it can be answered by every ground defence, and it crushes what it rolls over
            speed = 0.44f;
            hitSize = 44f;
            health = 24000f;
            armor = 15f;
            rotateSpeed = 0.8f;
            range = 310f;
            faceTarget = false;
            crushDamage = 2.6f;
            targetAir = true;
            targetGround = true;
            rotateMoveFirst = true;
            //a bounded shield: it regrows, it has a ceiling, and it stays down for a while once broken
            abilities.add(new PrismWardAbility(52f, 0.55f, 5200f, 60f * 9f));

            /*
             * The lance. One charged shot, then a long reload - and a shot bank on top: four shots fill the
             * heat sink and the gun vents for six seconds. Sustained output is capped by construction.
             */
            weapons.add(new CommandParts.VentWeapon("prism-monolith-lance"){{
                x = 0f;
                y = 0f;
                shootY = 0f;
                //a single centred lance: Weapon.mirror defaults to true, which would give the titan two
                //overlapping lances - double damage and a shared heat bank that never cooled
                mirror = false;
                reload = 150f;
                recoil = 4.6f;
                rotate = true;
                rotateSpeed = 1.1f;
                inaccuracy = 0.6f;
                shake = 3.4f;
                shootSound = BHSounds.lance;
                chargeSound = BHSounds.charge;
                heatPerShot = 1f;
                heatCeiling = 4f;
                coolRate = 0.42f;
                ventTime = 60f * 6f;
                shoot = new ShootPattern(){{
                    firstShotDelay = 48f;
                }};
                bullet = new LaserBulletType(560f){{
                    length = 310f;
                    width = 20f;
                    lifetime = 17f;
                    pierceCap = 4;
                    sideAngle = 42f;
                    sideWidth = 1.4f;
                    sideLength = 54f;
                    lightColor = hitColor = AureliaFx.resonance;
                    colors = new Color[]{AureliaFx.resonance.cpy().a(0.4f), AureliaFx.resonance, Color.white};
                    shootEffect = Fx.shockwave;
                    hitEffect = Fx.hitLancer;
                    despawnEffect = Fx.none;
                }};
            }});

            /*
             * Two air pods on the shoulders. Short range, no splash, there so the titan is not free food for
             * fliers - they are support, not the main gun.
             */
            weapons.add(new Weapon("prism-monolith-pod"){{
                x = 5.6f;
                y = 2.4f;
                shootY = 0f;
                mirror = true;
                reload = 22f;
                recoil = 1.1f;
                rotate = true;
                rotateSpeed = 4f;
                inaccuracy = 6f;
                targetAir = true;
                targetGround = false;
                shootSound = Sounds.shoot;
                bullet = new BasicBulletType(5f, 26f){{
                    width = 7f;
                    height = 11f;
                    lifetime = 32f;
                    splashDamage = 18f;
                    splashDamageRadius = 16f;
                    trailWidth = 1.2f;
                    trailLength = 6;
                    trailColor = hitColor = backColor = AureliaFx.resonance;
                    frontColor = Color.white;
                }};
            }});
        }});
    }

    // =====================================================================================================
    // blocks
    // =====================================================================================================

    private static void loadBlocks(){
        /*
         * The small projector. Same shape of mechanic as the vanilla one: a flat speed boost inside a radius,
         * paid for in power. No stacking - two fields on one building take the better of the two.
         */
        lumenAccelerator = add(new CommandParts.ModelOverdrive("lumen-accelerator"){{
            requirements(Category.effect, with(lumenite, 130, prismGlass, 75, aurite, 60));
            size = 2;
            health = 320;
            range = 11f * 8f;
            speedBoost = 1.5f;
            useTime = 300f;
            hasBoost = false;
            consumePower(3.2f);
            baseColor = AureliaFx.lumen;
            phaseColor = AureliaFx.resonance;
        }});

        /*
         * The strong one. Wider, faster, and it can be fed resonance shard for the top of its band; the boost
         * ceiling (2.1 base, 2.6 with the booster) is the highest number any building on Aurelia can reach.
         */
        prismOvercharger = add(new CommandParts.ModelOverdrive("prism-overcharger"){{
            requirements(Category.effect, with(lumenite, 320, aurite, 210, prismAlloy, 95, resonanceShard, 60));
            size = 3;
            health = 760;
            range = 24f * 8f;
            speedBoost = 2.1f;
            speedBoostPhase = 0.5f;
            phaseRangeBoost = 5f * 8f;
            useTime = 420f;
            hasBoost = true;
            consumePower(9f);
            consumeItem(resonanceShard).boost();
            baseColor = AureliaFx.lumen;
            phaseColor = AureliaFx.resonance;
        }});
    }

    private static void loadPlans(){
        //the titan is printed by the Lumen Superfactory - the only Aurelia line heavy enough for a titan.
        //(astro.content.AstroContent.fabricator is still null this early in loadContent; this block is ours.)
        mindustry.world.blocks.units.UnitFactory f =
            (mindustry.world.blocks.units.UnitFactory)AureliaContent.aureliaFabricator;
        f.plans.add(new UnitPlan(prismMonolith, 60f * 150f,
            with(lumenite, 1100, aurite, 850, prismAlloy, 420, resonanceShard, 220)));
    }
}
