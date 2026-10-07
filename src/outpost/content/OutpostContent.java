package outpost.content;

import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.world.consumers.ConsumeLiquidFilter;
import mindustry.content.TechTree.TechNode;
import mindustry.gen.Sounds;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import outpost.gfx.CoilWinderModel;
import outpost.gfx.LancerModel;
import outpost.gfx.RailgunModel;
import outpost.gfx.SlugPressModel;
import outpost.world.DrawModel;
import outpost.world.LancerTurret;
import outpost.world.RailgunTurret;

import static mindustry.type.ItemStack.with;

/** v2 content: 2 items, 2 factories, 2 turrets, one Serpulo research chain. */
public final class OutpostContent{
    public static Block slugPress, coilWinder, bastionRailgun, aegisLancer;

    private OutpostContent(){}

    public static void load(){
        OutpostItems.load();

        slugPress = new GenericCrafter("slug-press"){{
            requirements(Category.crafting, with(
                Items.copper, 150, Items.lead, 120, Items.silicon, 90, Items.titanium, 65
            ));
            size = 3;
            health = 1600;
            itemCapacity = 40;
            craftTime = 80f;
            outputItem = new ItemStack(OutpostItems.magneticSlug, 2);
            drawer = new DrawModel(SlugPressModel.instance);
            craftEffect = OutpostFx.pressSpark;
            updateEffect = OutpostFx.pressSpark;
            updateEffectChance = 0.02f;
            ambientSound = Sounds.loopSmelter;
            ambientSoundVolume = 0.04f;
            consumeItems(with(Items.graphite, 2, Items.titanium, 2, Items.silicon, 1));
            consumePower(3.2f);
        }};

        coilWinder = new GenericCrafter("coil-winder"){{
            requirements(Category.crafting, with(
                Items.copper, 130, Items.lead, 110, Items.silicon, 100, Items.graphite, 60
            ));
            size = 3;
            health = 1450;
            itemCapacity = 40;
            craftTime = 70f;
            outputItem = new ItemStack(OutpostItems.fluxCoil, 2);
            drawer = new DrawModel(CoilWinderModel.instance);
            craftEffect = OutpostFx.winderSpark;
            updateEffect = OutpostFx.winderSpark;
            updateEffectChance = 0.02f;
            ambientSound = Sounds.loopMachineSpin;
            ambientSoundVolume = 0.04f;
            consumeItems(with(Items.copper, 3, Items.silicon, 2));
            consumePower(2.8f);
        }};

        bastionRailgun = new RailgunTurret("bastion-railgun"){{
            requirements(Category.turret, with(
                Items.copper, 420, Items.lead, 330, Items.graphite, 230,
                Items.silicon, 250, Items.titanium, 200
            ));
            size = 4;
            health = 3850;
            range = 260f;
            reload = 110f;
            rotateSpeed = 2.2f;
            shootCone = 3f;
            shootX = 0f;
            shootY = RailgunModel.SHOOT_Y;
            maxAmmo = 30;
            ammoPerShot = 1;
            minWarmup = 0.65f;
            shootWarmupSpeed = 0.04f;
            warmupMaintainTime = 45f;
            recoil = 2.2f;
            recoilPow = 1.8f;
            cooldownTime = 45f;
            shake = 3f;
            shootSound = Sounds.shootForeshadow;
            shootSoundVolume = 0.65f;
            shoot.firstShotDelay = 8f;
            targetAir = true;
            targetGround = true;
            //v7.7: the siege railgun is a real siege weapon now - a four-digit slug that is never stopped by
            //the first thing it hits, and a water jacket that lets the rails cycle faster. Plain water works;
            //RBMK demineralised water carries 1.05 heat capacity against water's 0.4, so the same jacket spins
            //the loader far harder - the stronger acceleration the player asked for falls straight out of the
            //vanilla coolant formula (reload bonus scales with the liquid's heat capacity).
            ammo(
                OutpostItems.magneticSlug, new OutpostBullets.LanceSlugType(17f, 1150f){{
                    //infinite penetration: the slug keeps going through every unit and every wall in its lane
                    pierce = true;
                    pierceBuilding = true;
                    pierceCap = -1;
                    pierceDamageFactor = 0.94f;
                    buildingDamageMultiplier = 0.8f;
                    knockback = 2.4f;
                }}
            );
            liquidCapacity = 60f;
            coolantMultiplier = 1.35f;
            ConsumeLiquidFilter jacket = new ConsumeLiquidFilter(
                l -> l == Liquids.water || l == rbmk.content.RbmkLiquids.demineralizedWater, 0.28f);
            jacket.boost();
            coolant = consume(jacket);
            consumePower(5.2f);
        }};

        aegisLancer = new LancerTurret("aegis-lancer"){{
            requirements(Category.turret, with(
                Items.copper, 260, Items.lead, 210, Items.graphite, 130,
                Items.silicon, 180, Items.titanium, 110
            ));
            size = 3;
            health = 1950;
            range = 195f;
            reload = 30f;
            rotateSpeed = 5.5f;
            shootCone = 8f;
            shootX = 0f;
            shootY = LancerModel.SHOOT_Y;
            maxAmmo = 40;
            ammoPerShot = 1;
            minWarmup = 0.4f;
            shootWarmupSpeed = 0.08f;
            warmupMaintainTime = 30f;
            recoil = 1.4f;
            recoilPow = 1.8f;
            cooldownTime = 30f;
            inaccuracy = 2f;
            shake = 1.2f;
            shootSound = Sounds.shootLancer;
            shootSoundVolume = 0.5f;
            shoot.shots = 3;
            shoot.shotDelay = 4f;
            targetAir = true;
            targetGround = true;
            ammo(
                OutpostItems.fluxCoil, new OutpostBullets.CoilBoltType(9f, 34f)
            );
            consumePower(3.6f);
        }};

        TechNode parent = Blocks.siliconSmelter.techNode;
        if(parent != null){
            TechNode press = new TechNode(parent, slugPress, with(
                Items.copper, 850, Items.lead, 650, Items.silicon, 500, Items.titanium, 380
            ));
            new TechNode(press, bastionRailgun, with(
                Items.copper, 2300, Items.lead, 1600, Items.graphite, 1250,
                Items.silicon, 1350, Items.titanium, 1000
            ));
            TechNode winder = new TechNode(press, coilWinder, with(
                Items.copper, 700, Items.lead, 550, Items.silicon, 600, Items.graphite, 350
            ));
            new TechNode(winder, aegisLancer, with(
                Items.copper, 1400, Items.lead, 1000, Items.graphite, 700,
                Items.silicon, 950, Items.titanium, 600
            ));
        }
    }
}
