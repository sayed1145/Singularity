package voxel.content;

import arc.graphics.*;
import mindustry.type.*;

/** Items added by the mod. They are ordinary vanilla-compatible items usable in any vanilla block. */
public class VoxelItems{
    public static Item voxelAlloy;

    public static void load(){
        voxelAlloy = new Item("voxel-alloy", Color.valueOf("8fe9ff")){{
            cost = 1.35f;
            hardness = 4;
            charge = 0.15f;
            healthScaling = 0.1f;
            explosiveness = 0f;
            flammability = 0f;
            radioactivity = 0f;
        }};
    }
}
