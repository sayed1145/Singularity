package blackhole;

import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.ctype.*;
import mindustry.type.*;

/**
 * The native Singularity research spine.  Keeping this in Java makes every custom
 * item, factory, turret and unit discoverable in a normal campaign tech tree instead
 * of relying only on sandbox placement.
 */
public final class BHTechTree{
    public static TechNode singularityNode, citadelNode;
    private static boolean loaded;

    private BHTechTree(){}

    public static void load(){
        if(loaded) return;
        loaded = true;

        // Core material chain: phase weaving -> degenerate matter -> Hawking dust -> core.
        TechNode press = node(Blocks.phaseWeaver, BHBlocks.gravitonPress,
            ItemStack.with(Items.titanium, 180, Items.silicon, 130, Items.phaseFabric, 55));
        TechNode degenerate = node(press, BHItems.degenerateMatter,
            ItemStack.with(BHItems.degenerateMatter, 25));
        node(degenerate, BHBlocks.accretionFoundry,
            ItemStack.with(Items.titanium, 260, Items.silicon, 190, Items.plastanium, 70, BHItems.degenerateMatter, 90));
        TechNode condenser = node(degenerate, BHBlocks.hawkingCondenser,
            ItemStack.with(Items.plastanium, 170, BHItems.degenerateMatter, 90));
        TechNode dust = node(condenser, BHItems.hawkingDust,
            ItemStack.with(BHItems.hawkingDust, 28));
        singularityNode = node(dust, BHBlocks.singularityForge,
            ItemStack.with(Items.surgeAlloy, 180, Items.phaseFabric, 140, BHItems.hawkingDust, 95));
        TechNode core = node(singularityNode, BHItems.singularityCore,
            ItemStack.with(BHItems.singularityCore, 18));

        // Defense branch.
        TechNode dragger = node(degenerate, BHBlocks.frameDragger,
            ItemStack.with(Items.titanium, 160, BHItems.degenerateMatter, 55));
        TechNode cannon = node(dragger, BHBlocks.accretionCannon,
            ItemStack.with(Items.silicon, 220, BHItems.degenerateMatter, 95));
        TechNode emitter = node(cannon, BHBlocks.hawkingEmitter,
            ItemStack.with(Items.silicon, 260, BHItems.hawkingDust, 70));
        TechNode lance = node(emitter, BHBlocks.quasarLance,
            ItemStack.with(Items.surgeAlloy, 170, BHItems.hawkingDust, 105));
        node(lance, BHBlocks.eventHorizon,
            ItemStack.with(Items.surgeAlloy, 260, BHItems.singularityCore, 52));

        // Unit branch.
        TechNode assembler = node(core, BHBlocks.horizonAssembler,
            ItemStack.with(Items.silicon, 250, BHItems.degenerateMatter, 110));
        TechNode lensing = node(assembler, BHUnits.lensing,
            ItemStack.with(Items.silicon, 120, BHItems.degenerateMatter, 70));
        node(lensing, BHUnits.ergosphere,
            ItemStack.with(Items.surgeAlloy, 100, BHItems.hawkingDust, 75));
        TechNode yard = node(core, BHBlocks.citadelYard,
            ItemStack.with(Items.surgeAlloy, 280, BHItems.singularityCore, 40));
        TechNode accretor = node(yard, BHUnits.accretor,
            ItemStack.with(Items.titanium, 240, BHItems.degenerateMatter, 100));
        citadelNode = node(accretor, BHUnits.citadel,
            ItemStack.with(Items.surgeAlloy, 290, BHItems.singularityCore, 48));
    }

    public static TechNode node(UnlockableContent parent, UnlockableContent child, ItemStack[] requirements){
        if(parent == null || parent.techNode == null){
            throw new IllegalStateException("Research parent unavailable for " + child.name);
        }
        return node(parent.techNode, child, requirements);
    }

    public static TechNode node(TechNode parent, UnlockableContent child, ItemStack[] requirements){
        if(parent == null) throw new IllegalStateException("Null research parent for " + child.name);
        return new TechNode(parent, child, requirements);
    }
}
