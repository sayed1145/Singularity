package voxel.content;

import arc.util.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.type.*;

import static mindustry.type.ItemStack.*;

/** Hooks the mod content into the vanilla Serpulo research tree. */
public class VoxelTech{

    public static void load(){
        TechNode parent = Blocks.siliconSmelter.techNode;
        if(parent == null){
            Log.warn("[voxel-industry] vanilla tech node missing, skipping research tree integration");
            return;
        }

        TechNode forge = new TechNode(parent, VoxelBlocks.voxelForge, with(
            Items.copper, 900,
            Items.lead, 700,
            Items.titanium, 350
        ));

        TechNode assembler = new TechNode(forge, VoxelBlocks.voxelAssembler, with(
            Items.copper, 1200,
            Items.lead, 900,
            Items.silicon, 600,
            VoxelItems.voxelAlloy, 200
        ));

        TechNode tessellus = new TechNode(assembler, VoxelUnits.tessellus, with(
            Items.silicon, 800,
            Items.titanium, 500,
            VoxelItems.voxelAlloy, 300
        ));

        TechNode arachne = new TechNode(tessellus, VoxelUnits.arachne, with(
            Items.silicon, 1600,
            Items.titanium, 1100,
            Items.graphite, 900,
            VoxelItems.voxelAlloy, 700
        ));

        new TechNode(arachne, VoxelUnits.titan, with(
            Items.silicon, 3000,
            Items.titanium, 2200,
            Items.plastanium, 1200,
            VoxelItems.voxelAlloy, 1500
        ));
    }
}
