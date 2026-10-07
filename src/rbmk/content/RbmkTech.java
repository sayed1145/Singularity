package rbmk.content;
import arc.util.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import static mindustry.type.ItemStack.*;
public class RbmkTech{
    public static void load(){
        if(Blocks.thoriumReactor.techNode==null){Log.warn("[rbmk] thorium reactor tech node missing");return;}
        TechNode pellets=new TechNode(Blocks.thoriumReactor.techNode,RbmkBlocks.pelletPlant,with(Items.thorium,900,Items.silicon,700,Items.titanium,400));
        TechNode clad=new TechNode(pellets,RbmkBlocks.claddingMill,with(Items.titanium,900,Items.metaglass,500,Items.silicon,600));
        TechNode fuel=new TechNode(clad,RbmkBlocks.fuelAssemblyPlant,with(Items.thorium,1500,Items.titanium,1200,Items.plastanium,600,RbmkItems.uraniumPellets,300,RbmkItems.zirconiumCladding,250));
        TechNode water=new TechNode(Blocks.cryofluidMixer.techNode,RbmkBlocks.waterTreatment,with(Items.silicon,900,Items.metaglass,700,Items.titanium,600));
        if(Blocks.multiPress.techNode!=null) new TechNode(Blocks.multiPress.techNode,RbmkBlocks.graphiteKiln,with(Items.copper,600,Items.lead,500,Items.silicon,400,Items.titanium,300));
        else Log.warn("[rbmk] multi-press tech node missing; graphite kiln not added to the tech tree");
        new TechNode(fuel,RbmkBlocks.rbmkPlant,with(Items.copper,7000,Items.lead,8000,Items.graphite,9000,Items.silicon,6000,Items.titanium,5000,Items.metaglass,3200,Items.phaseFabric,1800,Items.surgeAlloy,2200,RbmkItems.uraniumAssembly,80));
        // waterTreatment is deliberately a parallel prerequisite visually; reactor still lists its required liquid clearly.
    }
}
