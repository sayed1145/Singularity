package blackhole;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import sixfold.*;

public class BlackHoleMod extends Mod{

    public BlackHoleMod(){
        Log.info("[blackhole] 奇点模组已载入类。");

        // 着色器必须在 GL 上下文就绪后才能编译，因此放在客户端加载完成事件里，
        // 而不是 loadContent()（那时还没有 GL 上下文，且服务端根本不该创建）。
        // 3D drawers for every turret / crafter / generator (Java and HJSON content), before textures load.
        Events.on(ContentInitEvent.class, e -> blackhole.models.Models.install());

        //every Aurelia sector load drops any vanilla item left in the saved launch payload
        Events.on(WorldLoadEvent.class, e -> AureliaLaunch.sanitizeLaunchResources());

        Events.on(ClientLoadEvent.class, e -> {
            BHShaders.init();
            //schematics are read from disk during load, so the loadout is registered once they exist
            AureliaLaunch.install();
            //建造动画与首次绘制的卡顿：预先构建全部实时网格/炮塔头，并把每个方块的建造图标压到一张小图
            blackhole.models.Warmup.install();
            if(Vars.ui != null && Vars.ui.settings != null){
                Vars.ui.settings.graphics.checkPref("bh-lens", true);
            }
            V6IntegrityAudit.checkClientAssets();
            Log.info("[blackhole] 客户端就绪，引力透镜"
                + (BHShaders.lens != null && BHShaders.lens.isCompiled() ? "已启用。" : "不可用（退化为无扭曲模式）。"));
        });
    }

    @Override
    public void init(){
        // Runs after Java and HJSON content has been created. The imported expansion
        // verifies its tech links, all three Titan Bay plans and config codecs here.
        MergedMods.init();
        AegisExpansion.validate();
        AureliaExpansion.validate();
        //v7.1: Aurelia shows only Aurelia content, and every mod content uses the mod's own particle effects
        //v7.3 全量合并：三个模组的内容全部进入欧雷莉亚（可见性 + 科技树 + 以欧雷莉亚资源计价）
        //HJSON 内容在所有 Java 模组的 loadContent() 之后才解析，所以这里再跑一次（含炮塔弹药重映射）
        AureliaMerge.recost();
        AureliaMerge.include();
        AureliaMerge.retree();
        //v7.8: the mod is Aurelia-only - nothing of it is left on Serpulo or Erekir
        AureliaMerge.purge();
        OwnFx.scrub("blackhole-");
        //v8.0: derive every unit's command list from what it can actually do (mine / build / carry / heal)
        UnitCapabilities.apply();
        //v8.2: launching to another sector must cost Aurelia items, not the vanilla core-shard fallback
        AureliaLaunch.install();
        Log.info("[blackhole] v5 科技树、泰坦矩阵与 v6 欧雷利亚远征完整性校验通过。");
    }

    @Override
    public void loadContent(){
        Log.info("[blackhole] 正在加载内容……");
        BHItems.load();
        BHBullets.load();
        BHUnits.load();
        BHBlocks.load();

        // 原生奇点科技树先建立；随后完整接入「以太泰坦矩阵」扩展内容。
        BHTechTree.load();
        AegisExpansion.load();
        AureliaContent.load();

        //v7.5：新的矿机 / 发电厂 / 单位修复站（含定向采集的裂隙汇聚器）
        AureliaIndustry.load();

        //v7.6：新流体 + 缆道运输 + 引力阱 / 回馈阵 / 风暴线圈 + 共振摇篮与三支新单位
        AureliaFrontier.load();
        AureliaBastion.load();
        AureliaNova.load();
        AureliaTerra.load();
        AureliaForge.load();

        //v8.1：无人机物流网 + 无线液网 + 液体储罐 + 溢流门 + 三支新单位
        AureliaLogix.load();

        //v8.2：两台范围超速投影仪 + 泰坦级单位「棱镜巨碑」
        AureliaCommand.load();

        //v8.3：工业废料循环（六步回收链）+ 三级核心「欧雷莉亚堡垒」+ 守护力场穹顶
        AureliaCycle.load();

        //v7.4/v7.5：天文拘留者 + RBMK 白色反应堆 + 体素工业 + 体素前哨 并入本 jar
        MergedMods.loadContent();

        //必须在 ContentLoader.init() 之前：那一步会把 requirements 固化成建造成本与消耗器数组
        AureliaMerge.recost();

        Log.info("[blackhole] 内容加载完成：奇点主线 + 以太泰坦矩阵 + 欧雷利亚远征已注册。");
    }
}
