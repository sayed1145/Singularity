package voxel.content;

import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.units.UnitFactory.*;
import voxel.gfx.*;
import voxel.world.*;

import static mindustry.type.ItemStack.*;

/** The production line: forge -> alloy -> assembler -> 3D unit. */
public class VoxelBlocks{
    public static Block voxelForge, voxelAssembler;

    public static void load(){
        voxelForge = new GenericCrafter("voxel-forge"){{
            requirements(Category.crafting, with(
                Items.copper, 135,
                Items.lead, 110,
                Items.silicon, 70,
                Items.titanium, 45
            ));
            size = 3;
            health = 260 * size * size;
            craftTime = 75f;
            outputItem = new ItemStack(VoxelItems.voxelAlloy, 2);
            itemCapacity = 20;
            hasPower = true;
            hasLiquids = true;
            liquidCapacity = 30f;
            craftEffect = Fx.smeltsmoke;
            updateEffect = Fx.none;
            ambientSound = Sounds.loopSmelter;
            ambientSoundVolume = 0.07f;
            drawer = new DrawModel(ForgeModel.instance);
            consumeItems(with(Items.silicon, 3, Items.titanium, 2));
            consumeLiquid(Liquids.cryofluid, 0.08f);
            consumePower(2.6f);
        }};

        voxelAssembler = new VoxelAssembler("voxel-assembler"){{
            requirements(Category.units, with(
                Items.copper, 180,
                Items.lead, 160,
                Items.silicon, 120,
                VoxelItems.voxelAlloy, 60
            ));
            size = 3;
            health = 240 * size * size;
            consumePower(3.6f);

            plans.add(new UnitPlan(VoxelUnits.tessellus, 60f * 32f, with(
                Items.silicon, 45,
                Items.titanium, 30,
                VoxelItems.voxelAlloy, 25
            )));
            plans.add(new UnitPlan(VoxelUnits.arachne, 60f * 75f, with(
                Items.silicon, 160,
                Items.titanium, 120,
                Items.graphite, 90,
                VoxelItems.voxelAlloy, 95
            )));
            plans.add(new UnitPlan(VoxelUnits.titan, 60f * 155f, with(
                Items.silicon, 420,
                Items.titanium, 340,
                Items.plastanium, 180,
                VoxelItems.voxelAlloy, 260
            )));
        }};
    }
}
