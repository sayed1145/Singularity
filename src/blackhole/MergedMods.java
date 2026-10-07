package blackhole;

import arc.util.*;
import astro.content.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import rbmk.content.*;
import voxel.content.*;
import outpost.content.*;

/**
 * v7.4/v7.5: the Astro Detainer (v1.7.0), the RBMK White Reactor Mk.III (v4.1.0), Voxel Industry (v2.0.1) and
 * Voxel Outpost (v2.0.0) are merged into this jar.
 *
 * <p>Both used to be standalone mods with their own {@code Mod} main class, their own content prefix and their
 * own bundle namespace. Inside one jar there can only be one main class, so their loaders are called from
 * {@link BlackHoleMod} instead and everything they register now carries this mod's {@code blackhole-} prefix:
 * sprites, bundle keys and the RBMK model atlas prefix were rewritten to match, and the Astro unit was renamed
 * {@code detainer -> astro-detainer} because the Singularity expedition already owns a {@code detainer}.
 *
 * <p>Nothing about their stats is touched. Their recipes and research costs are re-priced into Aurelia
 * resources by {@link AureliaMerge}, exactly like every other piece of content in the jar, and they are hung
 * into the Aurelia tech tree by the same sweep.
 */
public final class MergedMods{
    private MergedMods(){}

    /** Content of both merged mods, called from {@code BlackHoleMod.loadContent()}. */
    public static void loadContent(){
        //---- RBMK White Reactor Mk.III
        RbmkItems.load();
        RbmkLiquids.load();
        RbmkBlocks.load();
        RbmkTech.load();

        //---- Astro Detainer
        AstroContent.load();
        TechNode parent = TechTree.all.find(n -> n.content == Blocks.tetrativeReconstructor);
        if(parent != null){
            TechNode fab = new TechNode(parent, AstroContent.fabricator, AstroContent.fabricator.researchRequirements());
            new TechNode(fab, AstroContent.detainer, AstroContent.detainer.researchRequirements());
            new TechNode(fab, AstroContent.commander, AstroContent.commander.researchRequirements());
        }
        //---- Voxel Industry (v7.5)
        VoxelItems.load();
        VoxelUnits.load();
        VoxelBlocks.load();
        VoxelTech.load();

        //---- Voxel Outpost (v7.5)
        OutpostContent.load();

        Log.info("[blackhole/merge] Astro Detainer + RBMK White Reactor + Voxel Industry + Voxel Outpost registered inside this jar");
    }

    /** Mod.init() half of both mods (net handlers; the Astro pilot panel is client only). */
    public static void init(){
        AstroNet.init();
        if(!Vars.headless){
            try{
                //v8.3: the pilot panel, the settings page and the hot keys are back. Without this call the
                //Detainer could only be flown around - no claw controls, no warp, no settings entry.
                astro.ui.AstroUI.init();
            }catch(Throwable t){
                Log.err("[blackhole/astro] pilot panel failed to load", t);
            }
        }
    }
}
