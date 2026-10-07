package blackhole.models;

import arc.struct.*;
import blackhole.g3d.*;
import mindustry.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;

import static blackhole.g3d.Kit.*;

/** Registry of every 3D block model, keyed by the (unprefixed) block name. Used by the game and the offline baker. */
public final class Models{
    private static final ObjectMap<String, BlockModel> map = new ObjectMap<>();
    private static final Seq<BlockModel> list = new Seq<>();
    private static boolean loaded;

    private Models(){}

    public static synchronized void load(){
        if(loaded) return;
        loaded = true;
        TurretModels.load();
        for(SimpleTurret t : TurretModels.all()) add(t);
        add(new BlockModels.GravitonPress());
        add(new BlockModels.HawkingCondenser());
        add(new BlockModels.SingularityForge());
        add(new FoundryModel());
        add(new AssemblerModel("horizon-assembler", 3, sing));
        add(new AssemblerModel("citadel-yard", 5, sing));
        add(new BlockModels.AegisPress("aegis-press-s", serpulo));
        add(new BlockModels.SignalEncoder("signal-encoder-s", serpulo));
        add(new AssemblerModel("titan-bay-s", 5, serpulo));
        add(new BlockModels.AureliaCore());
        add(new BlockModels.LumenExtractor());
        add(new BlockModels.AuroraPanel());
        add(new BlockModels.RiftAnchor());
        add(new BlockModels.HarmonicRelay());
        add(new BlockModels.PrismPress());
        add(new BlockModels.PrismResonator());
        add(new AssemblerModel("aurelia-fabricator", 4, aurelia));
        add(new AssemblerModel("tide-dock", 3, aurelia));
        add(new AureliaModels.LumenBurner());
        add(new AureliaModels.TideTurbine());
        add(new AureliaModels.LumenCell());
        add(new AureliaModels.SiltKiln());
        add(new AureliaModels.PlasmaMixer());
        add(new AureliaModels.TidePump());
        add(new AureliaModels.LumenMender());
        add(new AureliaModels.LumenVault());
        //v7.5 industry pack
        add(new IndustryModels.AuriteAuger());
        add(new IndustryModels.ResonanceSifter());
        add(new IndustryModels.PrismHarvester());
        add(new IndustryModels.RiftCollector());
        add(new IndustryModels.PlasmaDynamo());
        add(new IndustryModels.AuroraArray());
        add(new IndustryModels.LumenRepairBeam());
        add(new IndustryModels.AuroraRepairDome());
        //v8.0-beta logistics / support tier
        add(new ForgeModels.WallBorer());
        add(new ForgeModels.CargoStation());
        add(new ForgeModels.CargoPoint());
        add(new ForgeModels.MendDome());
        add(new ForgeModels.Accumulator());
        add(new ForgeModels.GroundWell());
        //v8.1 logistics / liquid tier
        add(new LogixModels.SupplyPost());
        add(new LogixModels.BufferPost());
        add(new LogixModels.LogiVault());
        add(new LogixModels.LiquidRelay());
        add(new LogixModels.TankSmall());
        add(new LogixModels.TankLarge());
        add(new LogixModels.OverflowGate("lumen-overflow-gate", false));
        add(new LogixModels.OverflowGate("lumen-underflow-gate", true));
        //v8.2 command pack: the two overdrive projectors
        add(new CommandModels.Accelerator());
        add(new CommandModels.Overcharger());
        //v8.3 cycle pack: the waste chain, the ward dome and the tier-3 core
        add(new CycleModels.WasteReclaimer());
        add(new CycleModels.SwarfFurnace());
        add(new CycleModels.LeachTower());
        add(new CycleModels.SlurryCrystalliser());
        add(new CycleModels.Resynthesiser());
        add(new CycleModels.PrismReformer());
        add(new CycleModels.WasteSilo());
        add(new CycleModels.WardDome());
        add(new CycleModels.CitadelCore());
        //v7.6 frontier pack
        add(new FrontierModels.CoolantCondenser());
        add(new FrontierModels.GravitonChurn());
        add(new FrontierModels.SkylineCableway());
        add(new FrontierModels.GravityWell());
        add(new FrontierModels.AegisLattice());
        add(new AssemblerModel("resonance-cradle", 2, aurelia));
        //v7.7 bastion pack: ten walls, one power plant, the tier-2 core
        add(new BastionModels.WallModel("sinter-wall", 1, BastionModels.Crown.flat, BastionModels.siltGrey, BastionModels.lumen));
        add(new BastionModels.WallModel("graphite-brick-wall", 2, BastionModels.Crown.brick, BastionModels.graphiteGrey, BastionModels.siltGrey));
        add(new BastionModels.WallModel("prism-mirror-wall", 1, BastionModels.Crown.prism, BastionModels.prismBlue, BastionModels.prismBlue));
        add(new BastionModels.WallModel("stasis-wall", 2, BastionModels.Crown.ring, BastionModels.siltGrey, BastionModels.lumen));
        add(new BastionModels.WallModel("echo-wall", 2, BastionModels.Crown.coil, BastionModels.graphiteGrey, BastionModels.graviton));
        add(new BastionModels.WallModel("frost-wall", 2, BastionModels.Crown.tank, BastionModels.prismBlue, BastionModels.frost));
        add(new BastionModels.WallModel("anchor-wall", 2, BastionModels.Crown.funnel, BastionModels.graphiteGrey, BastionModels.graviton));
        add(new BastionModels.WallModel("amplifier-wall", 2, BastionModels.Crown.mast, BastionModels.siltGrey, BastionModels.lumen));
        add(new BastionModels.WallModel("absorber-wall", 2, BastionModels.Crown.cage, BastionModels.graphiteGrey, BastionModels.graviton));
        add(new BastionModels.WallModel("concord-shield-wall", 2, BastionModels.Crown.emitter, BastionModels.concordGold, BastionModels.concordGold));
        add(new BastionModels.GravitonDynamo());
        add(new BastionModels.BastionCore());
        //v7.8 nova pack: four zero-config transport blocks, the large power node, the resonance hub
        add(new NovaModels.LargeNode());
        add(new NovaModels.ResonanceHubModel());
    }

    static void add(BlockModel m){
        map.put(m.name, m);
        list.add(m);
    }

    public static Seq<BlockModel> all(){
        load();
        return list;
    }

    public static BlockModel get(String name){
        load();
        if(name.startsWith(BlockModel.prefix)) name = name.substring(BlockModel.prefix.length());
        return map.get(name);
    }

    /**
     * Installs drawers on every block whose class draws through a DrawBlock (turrets incl. the HJSON ones, crafters,
     * generators). Called from ContentInitEvent: after Java and HJSON content exist, before textures are loaded.
     */
    public static void install(){
        load();
        int n = 0;
        for(Block b : Vars.content.blocks()){
            BlockModel m = get(b.name);
            if(m == null) continue;
            if(b instanceof Turret t && m instanceof TurretModel tm){
                t.drawer = new DrawTurret3D(tm);
                //every shot leaves a drawn barrel (lateral spacing + projected height), whatever pattern it had
                if(!(t.shoot instanceof MuzzlePattern) && tm.muzzles != null && tm.muzzles.length >= 3){
                    t.shoot = new MuzzlePattern(t.shoot, tm.muzzles, tm.headZ, tm.cam);
                    t.shootX = 0f;
                    t.shootY = 0f;
                }
                n++;
            }else if(b instanceof GenericCrafter g){
                g.drawer = new DrawModel(m);
                n++;
            }else if(b instanceof PowerGenerator p){
                p.drawer = new DrawModel(m);
                n++;
            }else{
                //any other block that draws through a public DrawBlock field (pumps, batteries, ...)
                try{
                    java.lang.reflect.Field f = b.getClass().getField("drawer");
                    if(mindustry.world.draw.DrawBlock.class.isAssignableFrom(f.getType())){
                        f.set(b, new DrawModel(m));
                        n++;
                    }
                }catch(NoSuchFieldException | IllegalAccessException ignored){
                }
            }
        }
        arc.util.Log.info("[blackhole] 3D drawers installed on @ blocks (@ models)", n, list.size);
    }
}
