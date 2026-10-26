package blackhole;

import arc.graphics.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

import static blackhole.AureliaContent.*;
import static mindustry.type.ItemStack.*;

/**
 * v7.5 Aurelia industry pack: three more mining rigs, the rift collector (a miner with a new mechanic), two
 * generators and two unit repair stations. Everything is priced in Aurelia resources, drawn by a procedural
 * 3D model and hung into the Aurelia tech tree by {@link AureliaMerge}.
 */
public final class AureliaIndustry{
    public static Block auriteAuger, resonanceSifter, prismHarvester, riftCollector,
        plasmaDynamo, auroraArray, lumenRepairBeam, auroraRepairDome;

    /** Everything this pack adds, in tech order. */
    public static final Seq<UnlockableContent> added = new Seq<>();

    private AureliaIndustry(){}

    static <T extends Block> T add(T b){
        added.add(b);
        own.add(b);
        return b;
    }

    /**
     * v8.5.1: 金辉螺旋钻机的冷却液消耗器。
     *
     * <p>极光冷冻液在 {@link AureliaFrontier} 里注册，而那个包在 {@link BlackHoleMod#loadContent()} 里排在
     * 本包后面 —— 直接在方块初始化块里写 {@code consumeLiquid(auroraCoolant, ...)} 拿到的是 null，
     * 引擎初始化消耗器时会直接空指针崩掉。所以放到 Frontier 加载完之后再挂，
     * 只要在 {@code ContentLoader.init()} 冻结消耗器之前就行。
     */
    public static void applyBoosts(){
        if(auriteAuger instanceof mindustry.world.blocks.production.Drill d && d.liquidBoostIntensity != 1f){
            d.consumeLiquid(AureliaFrontier.auroraCoolant, 0.05f).boost();
        }
    }

    public static void load(){
        // -------------------------------------------------------------- mining
        auriteAuger = add(new blackhole.models.Drill3D("aurite-auger"){{
            requirements(Category.production, with(lumenite, 90, aurite, 55, prismGlass, 30));
            size = 3;
            health = 760;
            tier = 4;
            drillTime = 56f;
            itemCapacity = 40;
            drawRim = true;
            heatColor = AureliaFx.lumen;
            drillEffect = AureliaFx.lumenHit;
            updateEffect = AureliaFx.lumenHit;
            updateEffectChance = 0.005f;
            drillEffectChance = 0.04f;
            rotateSpeed = 2.4f;
            envEnabled = Env.any;
            consumePower(0.9f);
            //v8.5.1: 钻头也能吃冷却液 —— 极光冷冻液给 1.6 倍（面板显示 1.6² = 2.56 倍），
            //和共振筛选机的潮汐水、棱镜采掘场的流明等离子错开，三种矿机三种加速介质。
            //消耗器本身在 applyBoosts() 里挂：极光冷冻液属于 AureliaFrontier，比本包注册得晚。
            liquidBoostIntensity = 1.6f;
        }});

        resonanceSifter = add(new blackhole.models.Drill3D("resonance-sifter"){{
            requirements(Category.production, with(lumenite, 55, aurite, 25));
            size = 2;
            health = 400;
            tier = 3;
            drillTime = 66f;
            itemCapacity = 24;
            drawRim = true;
            heatColor = AureliaFx.lumen;
            drillEffect = AureliaFx.lumenHit;
            drillEffectChance = 0.05f;
            rotateSpeed = 1.6f;
            envEnabled = Env.any;
            //v8.5.1: 面板上写的是 liquidBoostIntensity 的平方（引擎就这么算的），
            //原来 1.7 显示成 2.89 倍太快，压到 1.5 —— 面板正好 2.25 倍。
            liquidBoostIntensity = 1.5f;
            consumeLiquid(tidewater, 0.06f).boost();
        }});

        prismHarvester = add(new blackhole.models.Drill3D("prism-harvester"){{
            requirements(Category.production, with(lumenite, 320, aurite, 240, prismGlass, 140, resonanceShard, 90));
            size = 4;
            health = 1560;
            tier = 5;
            drillTime = 42f;
            itemCapacity = 70;
            drawRim = true;
            heatColor = AureliaFx.lumen;
            drillEffect = AureliaFx.lumenHit;
            updateEffect = AureliaFx.lumenHit;
            updateEffectChance = 0.004f;
            drillEffectChance = 0.03f;
            rotateSpeed = 1.1f;
            envEnabled = Env.any;
            consumePower(2.6f);
            liquidBoostIntensity = 1.5f;
            consumeLiquid(lumenPlasma, 0.08f).boost();
        }});

        //the new mechanic: no ore underneath, no ground needed, mines a cone in the direction it faces
        riftCollector = add(new IndustryParts.BeamMiner("rift-collector"){{
            requirements(Category.production, with(lumenite, 260, aurite, 180, resonanceShard, 120, prismGlass, 90));
            size = 3;
            health = 820;
            tier = 4;
            //v8.5.1: 初始挖掘速度提到上一版的两倍（每份矿所需刻数减半）
            mineTime = 19f;
            range = 128f;
            coneDeg = 34f;
            beams = 3;
            itemCapacity = 50;
            beamColor = AureliaFx.lumen;
            consumePower(3.2f);
            //v8.5.1: 汇聚器也能喝液体了 —— 流明等离子给 1.5 倍（面板 2.25 倍）
            liquidBoostIntensity = 1.5f;
            consumeLiquid(lumenPlasma, 0.07f).boost();
        }});

        // -------------------------------------------------------------- power
        plasmaDynamo = add(new ConsumeGenerator("plasma-dynamo"){{
            requirements(Category.power, with(lumenite, 180, aurite, 120, prismGlass, 80, resonanceShard, 45));
            size = 3;
            health = 900;
            powerProduction = 7.2f;
            itemDuration = 150f;
            hasLiquids = true;
            liquidCapacity = 40f;
            consumeItem(resonanceShard);
            consumeLiquid(lumenPlasma, 0.1f);
            generateEffect = OwnFx.burn;
            effectChance = 0.04f;
            ambientSound = Sounds.loopCombustion;
            ambientSoundVolume = 0.05f;
            drawer = new blackhole.g3d.DrawModel(blackhole.models.Models.get("plasma-dynamo"));
        }});

        auroraArray = add(new SolarGenerator("aurora-array"){{
            requirements(Category.power, with(lumenite, 120, aurite, 70, prismGlass, 55));
            size = 3;
            health = 620;
            powerProduction = 2.1f;
            drawer = new blackhole.g3d.DrawModel(blackhole.models.Models.get("aurora-array"));
        }});

        // -------------------------------------------------------------- unit repair
        lumenRepairBeam = add(new IndustryParts.RepairStation("lumen-repair-beam"){{
            requirements(Category.effect, with(lumenite, 70, aurite, 45, prismGlass, 25));
            size = 2;
            health = 320;
            repairRadius = 112f;
            repairSpeed = 110f;
            maxTargets = 1;
            beamColor = AureliaFx.lumen;
            consumePower(1.2f);
        }});

        auroraRepairDome = add(new IndustryParts.RepairStation("aurora-repair-dome"){{
            requirements(Category.effect, with(lumenite, 240, aurite, 160, prismAlloy, 60, resonanceShard, 80));
            size = 3;
            health = 980;
            repairRadius = 176f;
            repairSpeed = 240f;
            maxTargets = 4;
            beamColor = Color.valueOf("b69cff");
            consumePower(3.4f);
        }});
    }
}
