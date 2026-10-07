package blackhole;

import arc.struct.*;
import mindustry.content.TechTree.*;
import mindustry.ctype.*;
import mindustry.game.Objectives.*;
import mindustry.type.*;

import static blackhole.AureliaContent.*;

/**
 * Aurelia research tree (v7.1). Its own root (the Aurelia core) and only Aurelia content: no vanilla item or block
 * appears anywhere in it. Research costs come from each block's / unit's own {@code researchRequirements()} (the
 * standard formula from its build cost), so research is neither free nor harsh. Resources are unlocked by
 * producing them.
 *
 * <p>The TechNode constructor overwrites {@code content.techNode}; for content that also has a node on another
 * planet (the shared allocation route) the previous pointer is restored so vanilla lookups keep working.
 */
public final class AureliaTech{
    public static TechNode root;

    private AureliaTech(){}

    public static TechNode build(Planet planet){
        root = new TechNode(null, aureliaCore, ItemStack.empty);
        root.name = "aurelia";
        root.planet = planet;
        mindustry.content.TechTree.roots.add(root);

        //resources
        TechNode lum = produce(root, lumenite);
        TechNode aur = produce(lum, aurite);
        TechNode sil = produce(lum, silt);
        TechNode glass = produce(sil, prismGlass);
        TechNode shard = produce(aur, resonanceShard);
        TechNode alloy = produce(shard, prismAlloy);
        produce(alloy, concordCore);
        TechNode tide = produce(sil, tidewater);
        produce(tide, lumenPlasma);

        //transport
        TechNode conv = node(root, lumenConveyor);
        TechNode junc = node(conv, lumenJunction);
        TechNode router = node(junc, lumenRouter);
        node(router, lumenSorter);
        TechNode bridge = node(junc, lumenBridge);
        node(bridge, lumenUnloader);

        //production + crafting
        TechNode drill = node(root, lumenExtractor);
        TechNode kiln = node(drill, siltKiln, new Produce(silt));
        TechNode pump = node(kiln, tidePump);
        TechNode conduit = node(pump, lumenConduit);
        node(conduit, lumenLiquidRouter);
        node(conduit, lumenLiquidBridge);
        TechNode anchor = node(drill, riftAnchor, new Produce(aurite));
        TechNode press = node(anchor, prismPress, new Produce(resonanceShard));
        TechNode mixer = node(press, plasmaMixer, new Produce(prismGlass));
        node(mixer, prismResonator, new Produce(lumenPlasma));
        node(press, harmonicRelay, new Produce(prismAlloy));

        //power
        TechNode burner = node(root, lumenBurner);
        TechNode nodeN = node(burner, lumenNode);
        node(nodeN, lumenCell);
        TechNode panel = node(nodeN, auroraPanel);
        node(panel, tideTurbine, new Produce(tidewater));

        //defence
        TechNode spark = node(root, lumenSpark);
        TechNode volley = node(spark, shardVolley, new Produce(aurite));
        TechNode pylon = node(volley, prismPylon, new Produce(prismAlloy));
        node(pylon, arcMortar, new Produce(concordCore));
        TechNode wall = node(spark, lumeniteWall);
        node(wall, lumeniteWallLarge);
        TechNode awall = node(wall, auriteWall, new Produce(aurite));
        node(awall, auriteWallLarge);
        TechNode mender = node(wall, lumenMender, new Produce(prismGlass));
        node(mender, lumenVault);

        //units
        TechNode fab = node(root, aureliaFabricator, new Produce(resonanceShard));
        TechNode rover = node(fab, shardRover);
        node(rover, auriteBulwark, new Produce(prismAlloy));
        TechNode gl = node(fab, glint);
        TechNode guard = node(gl, auroraGuard, new Produce(concordCore));
        node(guard, auroraHeavy);
        TechNode dock = node(fab, tideDock, new Produce(prismGlass));
        TechNode skiff = node(dock, tideSkiff);
        node(skiff, tideWarden, new Produce(prismAlloy));

        //logic
        TechNode proc = node(root, lumenProcessor, new Produce(prismGlass));
        node(proc, lumenSwitch);
        node(proc, lumenMessage);
        node(proc, lumenMemory);
        node(proc, lumenDisplay);
        return root;
    }

    /** Research node costing the content's own standard research requirements. */
    static TechNode node(TechNode parent, UnlockableContent c, Objective... objectives){
        TechNode prev = c.techNode;
        TechNode n = new TechNode(parent, c, c.researchRequirements());
        n.objectives.addAll(objectives);
        if(prev != null) c.techNode = prev;
        return n;
    }

    /** Resource node: free, unlocked by producing the resource. */
    static TechNode produce(TechNode parent, UnlockableContent c){
        TechNode prev = c.techNode;
        TechNode n = new TechNode(parent, c, ItemStack.empty);
        n.objectives.add(new Produce(c));
        if(prev != null) c.techNode = prev;
        return n;
    }

    /** Node in another planet's tree (allocation route), keeping the content's primary node pointer. */
    public static TechNode foreign(TechNode parent, UnlockableContent c, ItemStack[] cost){
        TechNode prev = c.techNode;
        TechNode n = new TechNode(parent, c, cost);
        if(prev != null) c.techNode = prev;
        return n;
    }
}
