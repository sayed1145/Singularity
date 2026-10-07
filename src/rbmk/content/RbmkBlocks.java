package rbmk.content;

import rbmk.gfx.*;

import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;
import rbmk.world.*;
import static mindustry.type.ItemStack.*;

/** Complete, vanilla-obtainable fuel and coolant chain plus the reactor. */
public class RbmkBlocks{
    public static Block pelletPlant, claddingMill, fuelAssemblyPlant, waterTreatment, graphiteKiln, rbmkPlant;
    public static void load(){
        pelletPlant = new GenericCrafter("pellet-plant"){{
            requirements(Category.crafting, with(Items.copper,180,Items.lead,220,Items.silicon,140,Items.titanium,90));
            size=3; health=980; craftTime=120f; itemCapacity=30;
            outputItem=new ItemStack(RbmkItems.uraniumPellets,2);
            consumeItems(with(Items.thorium,2,Items.silicon,1)); consumePower(4.5f);
            craftEffect=Fx.none; updateEffect=Fx.none;
            drawer=new DrawModel(PelletPlantModel.instance);
            ambientSound=Sounds.loopSmelter; ambientSoundVolume=.08f;
        }};
        claddingMill = new GenericCrafter("cladding-mill"){{
            requirements(Category.crafting, with(Items.copper,190,Items.lead,180,Items.silicon,160,Items.titanium,130));
            size=3; health=1050; craftTime=90f; itemCapacity=30;
            outputItem=new ItemStack(RbmkItems.zirconiumCladding,2);
            consumeItems(with(Items.titanium,3,Items.metaglass,1)); consumePower(3.8f);
            craftEffect=Fx.none; updateEffect=Fx.none;
            drawer=new DrawModel(CladdingMillModel.instance);
            ambientSound=Sounds.loopMachineSpin; ambientSoundVolume=.06f;
        }};
        fuelAssemblyPlant = new GenericCrafter("fuel-assembly-plant"){{
            requirements(Category.crafting, with(Items.copper,260,Items.lead,240,Items.silicon,220,Items.titanium,180,Items.plastanium,80));
            size=3; health=1250; craftTime=180f; itemCapacity=40;
            outputItem=new ItemStack(RbmkItems.uraniumAssembly,1);
            consumeItems(with(RbmkItems.uraniumPellets,4,RbmkItems.zirconiumCladding,2,blackhole.AureliaBastion.graphiteBrick,2)); consumePower(6.5f);
            craftEffect=Fx.none; updateEffect=Fx.none;
            drawer=new DrawModel(FuelAssemblyModel.instance);
            ambientSound=Sounds.loopTech; ambientSoundVolume=.08f;
        }};
        waterTreatment = new GenericCrafter("water-treatment"){{
            requirements(Category.liquid, with(Items.copper,180,Items.lead,220,Items.silicon,160,Items.metaglass,120,Items.titanium,100));
            size=3; health=1100; craftTime=60f; liquidCapacity=120f;
            hasLiquids=true; outputLiquid=new LiquidStack(RbmkLiquids.demineralizedWater,.32f);
            consumeLiquid(Liquids.water,.36f); consumePower(5.2f);
            craftEffect=Fx.none; updateEffect=Fx.none;
            drawer=new DrawModel(WaterTreatmentModel.instance);
            ambientSound=Sounds.loopHum; ambientSoundVolume=.07f;
        }};
        graphiteKiln = new GenericCrafter("graphite-kiln"){{
            requirements(Category.crafting, with(Items.copper,150,Items.lead,120,Items.silicon,80,Items.titanium,70));
            size=3; health=1000; craftTime=90f; itemCapacity=30;
            //v7.7: the kiln finally has a product of its own - the graphite brick, which the bastion walls,
            //the graviton string dynamo and the fuel assembly line all need. Inputs are re-costed to silt +
            //lumenite by AureliaMerge; outputs are not, so the brick is named directly.
            outputItem=new ItemStack(blackhole.AureliaBastion.graphiteBrick,2);
            consumeItems(with(Items.coal,3,Items.copper,1)); consumePower(2.4f);
            craftEffect=Fx.none; updateEffect=Fx.none;
            drawer=new DrawModel(GraphiteKilnModel.instance);
            ambientSound=Sounds.loopMachine; ambientSoundVolume=.07f;
        }};
        rbmkPlant = new RbmkReactor("rbmk-plant"){{
            requirements(Category.power, with(Items.copper,1800,Items.lead,2100,Items.graphite,2400,
                Items.silicon,1400,Items.titanium,1200,Items.metaglass,700,Items.phaseFabric,360,Items.surgeAlloy,520));
            size=8; health=12000; armor=10f; itemCapacity=32; liquidCapacity=420f;
            // 500 at nominal load (>5x v1), 1000 at expert 200% load (>10x v1).
            powerProduction=500f;
            fuelItem=RbmkItems.uraniumAssembly; coolant=RbmkLiquids.demineralizedWater;
            fuelDuration=60f*18f; nominalCoolant=.055f;
            consumeItem(fuelItem,1).update(false);
            consumeLiquid(coolant,nominalCoolant).update(false);
            buildCostMultiplier=1.15f;
            ambientSound=Sounds.loopThoriumReactor; ambientSoundVolume=.14f;
            explosionRadius=38; explosionDamage=24000; explosionShake=22f; explosionShakeDuration=60f;
            explosionScorchSize=9; explosionIgnitionChance=.9f; explosionFireballs=34;
            explodeEffect=RbmkFx.rbmkExplosion; explodeSound=Sounds.explosionReactor;
        }};
    }
}
