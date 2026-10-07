package blackhole;

import arc.util.*;

/** Server-safe registration audit for Aurelia's campaign, zero-start chain and cross-planet routes. */
public final class AureliaExpansion{
    private AureliaExpansion(){}

    public static void validate(){
        if(AureliaContent.aurelia == null || AureliaContent.aurelia.generator == null || AureliaContent.aurelia.techTree == null){
            throw new IllegalStateException("Aurelia planet, generator or independent research root is missing");
        }
        if(AureliaContent.aurelia.defaultCore != AureliaContent.aureliaCore ||
            AureliaContent.aureliaCore.unitType != AureliaContent.lumenPilot ||
            AureliaContent.aurelia.techTree.children.isEmpty()){
            throw new IllegalStateException("Aurelia custom core, player chassis or progression root is incomplete");
        }
        if(!(AureliaContent.riftAnchor instanceof ResonanceAnchorBlock) ||
            !(AureliaContent.harmonicRelay instanceof HarmonicRelayBlock) ||
            AureliaContent.lumenExtractor == null || AureliaContent.auroraPanel == null){
            throw new IllegalStateException("Aurelia zero-start extractor/power/field loop did not register");
        }
        //v7.8: the mod is Aurelia-only - the unit factory is researched here and nowhere else
        if(AureliaContent.aureliaFabricator.techNodes.size < 1){
            throw new IllegalStateException("Aurelia unit factory has no research node");
        }
        //v7.7: the bastion line - brick, ten walls, the dynamo, the tier-2 core and its two units
        if(AureliaBastion.graphiteBrick == null || AureliaBastion.bastionCore == null ||
            AureliaBastion.bastionCore.size != 5 || AureliaBastion.gravitonStringDynamo == null ||
            AureliaBastion.added.count(c -> c instanceof mindustry.world.Block b && b.category == mindustry.type.Category.defense) != 10){
            throw new IllegalStateException("Aurelia v7.7 bastion pack did not register (brick, 10 walls, dynamo, tier-2 core)");
        }
        if(AureliaNova.resonanceHub == null || AureliaNova.auroraNode == null || AureliaNova.resonanceOverload == null ||
            AureliaNova.all.count(c -> c instanceof mindustry.type.UnitType) != 5){
            throw new IllegalStateException("Aurelia v7.8 nova pack did not register (large node, four transport blocks, hub, five units)");
        }
        Log.info("[blackhole/aurelia] custom core + player chassis, zero-start extractor/power chain, authored terrain, three-anchor field network, and Aurelia-only research registered.");
    }
}
