package blackhole;

import arc.struct.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.units.UnitFactory.*;
import mindustry.world.meta.*;

import static blackhole.AureliaContent.*;
import static mindustry.type.ItemStack.with;

/**
 * v8.1 content pack: the logistics network, the liquid tier and three new units.
 *
 * <ul>
 * <li><b>Supply post / request pad / vault / buffer post</b> - the four roles of the drone network. The cargo
 *     station from 8.0 is now the hub that drives them.</li>
 * <li><b>Liquid relay</b> - wireless liquid transport: relays level their contents across the whole linked set,
 *     so a pylon line crosses broken ground a conduit cannot.</li>
 * <li><b>Liquid tank / reservoir</b> - the small and large liquid stores the relay network buffers against.</li>
 * <li><b>Overflow and underflow gate</b> - the two sorting gates every belt layout wants.</li>
 * <li><b>aurite vanguard</b> (the ground line-breaker) and <b>tide tender</b> (naval liquid carrier).</li>
 *     <p>v8.7.1: the lumen hauler is gone - a network is scaled by adding hubs, not by adding freight fliers.</p>
 * </ul>
 */
public final class AureliaLogix{
    public static Block liquidRelay, liquidTank, reservoir, overflowGate, underflowGate;

    public static Ship3D tideTender;
    public static Tank3D auriteVanguard;

    /** everything this pack adds, in tree order */
    public static final Seq<UnlockableContent> all = new Seq<>();

    private static boolean loaded;

    private AureliaLogix(){}

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
    // units
    // =====================================================================================================

    private static void loadUnits(){
        /*
         * v8.7.1: the heavy freight flier (lumen hauler) was removed - the hub now prints and keeps its own
         * drones only, and a network is scaled by building another hub instead of another hauler.
         */

        /*
         * The vanguard: the heavy ground line-breaker Aurelia was missing between the lancer and the siege.
         * One real gun plus a coaxial, enough armour to lead a push, slow enough that it cannot chase fliers.
         */
        auriteVanguard = add(new Tank3D("aurite-vanguard"){{
            builder = LogixUnits::auriteVanguard;
            speed = 0.58f;
            hitSize = 26f;
            health = 4200f;
            armor = 11f;
            rotateSpeed = 1.5f;
            range = 220f;
            faceTarget = false;
            crushDamage = 1.3f;
            weapons.add(new Weapon("aurite-vanguard-gun"){{
                x = 0f;
                y = 0f;
                shootY = 0f;
                reload = 72f;
                recoil = 3.2f;
                rotate = true;
                rotateSpeed = 1.7f;
                inaccuracy = 1.5f;
                shootSound = BHSounds.thump;
                bullet = new BasicBulletType(5.2f, 135f){{
                    width = 13f;
                    height = 20f;
                    lifetime = 44f;
                    splashDamage = 60f;
                    splashDamageRadius = 28f;
                    pierceCap = 2;
                    pierceBuilding = true;
                    trailWidth = 2.4f;
                    trailLength = 8;
                    trailColor = hitColor = backColor = AureliaFx.lumen;
                    frontColor = arc.graphics.Color.white;
                    despawnEffect = new MultiEffect(Fx.hitBulletBig);
                }};
            }});
            weapons.add(new Weapon("aurite-vanguard-coax"){{
                x = 0f;
                y = 0f;
                shootY = 0f;
                reload = 14f;
                recoil = 0.8f;
                rotate = true;
                rotateSpeed = 3.2f;
                inaccuracy = 5f;
                targetAir = true;
                shootSound = Sounds.shoot;
                bullet = new BasicBulletType(4.4f, 23f){{
                    width = 6f;
                    height = 10f;
                    lifetime = 30f;
                    trailWidth = 1.1f;
                    trailLength = 5;
                    trailColor = hitColor = backColor = AureliaFx.lumen;
                    frontColor = arc.graphics.Color.white;
                }};
            }});
        }});

        /*
         * Tide tender: a naval liquid carrier. It is the ship half of the new liquid tier - it fills itself at
         * a shore tank and tops up a remote one, which is how an island outpost gets water before it has power
         * for a relay chain.
         */
        tideTender = add(new Ship3D("tide-tender"){{
            builder = LogixUnits::tideTender;
            constructor = UnitWaterMove::create;
            naval = true;
            flying = false;
            speed = 1.15f;
            drag = 0.14f;
            hitSize = 17f;
            health = 980f;
            armor = 4f;
            accel = 0.2f;
            rotateSpeed = 2.4f;
            faceTarget = false;
            targetAir = false;
            trailLength = 22;
            waveTrailX = 5f;
            waveTrailY = -7f;
            trailScl = 1.3f;
            weapons.add(new Weapon("tide-tender-hose"){{
                x = 0f;
                y = 2f;
                shootY = 0f;
                reload = 26f;
                rotate = true;
                rotateSpeed = 3f;
                targetAir = false;
                shootSound = BHSounds.pulse;
                bullet = new LiquidBulletType(AureliaContent.tidewater){{
                    speed = 3.6f;
                    damage = 14f;
                    knockback = 0.6f;
                    lifetime = 42f;
                    statusDuration = 60f;
                    drag = 0.01f;
                }};
            }});
        }});
    }

    // =====================================================================================================
    // blocks
    // =====================================================================================================

    private static void loadBlocks(){
        /*
         * The four network nodes. Prices follow the 8.0 line: a node is cheaper than the hub that drives it,
         * and the vault - which is a warehouse - is the expensive one.
         */
        //v8.7: 供货台（supply post）、缓存台（buffer post）、物流仓库（logistics vault）已删除。
        //物流网现在只剩两件东西：中枢站（LogixParts.Hub）和需求点（LogixParts.Request）。

        /*
         * Wireless liquid transport. The relay is power-free on purpose - it is a levelling network, not a
         * pump, so it can never push more than its transfer cap and cannot be used as a free pump either.
         */
        liquidRelay = add(new LogixParts.LiquidRelay("lumen-liquid-relay"){{
            requirements(Category.liquid, with(lumenite, 70, prismGlass, 45, resonanceShard, 12));
            health = 280;
            liquidCapacity = 320f;
            transferRate = 9f;
            range = 22f * 8f;
            maxLinks = 6;
        }});
        liquidTank = add(new LogixParts.ModelLiquidTank("lumen-tank"){{
            requirements(Category.liquid, with(lumenite, 60, prismGlass, 40));
            size = 2;
            health = 400;
            liquidCapacity = 1500f;
        }});
        reservoir = add(new LogixParts.ModelLiquidTank("lumen-reservoir"){{
            requirements(Category.liquid, with(lumenite, 180, aurite, 90, prismAlloy, 30));
            size = 3;
            health = 1100;
            liquidCapacity = 6500f;
        }});

        /*
         * The two sorting gates. Vanilla mechanics exactly - the overflow gate only passes what the forward
         * block will not take, the underflow gate passes to the emptiest side first - with our own models.
         */
        overflowGate = add(new LogixParts.ModelOverflowGate("lumen-overflow-gate"){{
            requirements(Category.distribution, with(lumenite, 14, prismGlass, 8));
            health = 90;
        }});
        underflowGate = add(new LogixParts.ModelOverflowGate("lumen-underflow-gate"){{
            requirements(Category.distribution, with(lumenite, 14, prismGlass, 10));
            health = 90;
            invert = true;
        }});
    }

    private static void loadPlans(){
        mindustry.world.blocks.units.UnitFactory f = (mindustry.world.blocks.units.UnitFactory)aureliaFabricator;
        f.plans.add(new UnitPlan(auriteVanguard, 60f * 52f, with(lumenite, 220, aurite, 190, prismAlloy, 60, resonanceShard, 30)));
        mindustry.world.blocks.units.UnitFactory d = (mindustry.world.blocks.units.UnitFactory)tideDock;
        d.plans.add(new UnitPlan(tideTender, 60f * 40f, with(lumenite, 80, aurite, 70, prismGlass, 45)));
        //v8.1: the tide lancer had a tech node but no factory anywhere - it is printable now
        d.plans.add(new UnitPlan(AureliaFrontier.tideLancer, 60f * 56f, with(lumenite, 140, aurite, 120, prismAlloy, 35, resonanceShard, 20)));
    }
}
