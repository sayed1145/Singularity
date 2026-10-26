package blackhole;

import arc.files.*;
import arc.math.Mathf;
import arc.struct.*;
import arc.util.*;
import mindustry.world.Tile;
import mindustry.Vars;
import mindustry.content.TechTree;
import mindustry.content.TechTree.*;
import mindustry.ctype.*;
import mindustry.game.Objectives.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;

/**
 * 线下诊断钩子（只在启动参数里显式打开时工作，正常游玩完全不走这里）。
 *
 * <p>用法：java -Dsing.diag=tech,spawn,blocks,items,recipes -Dsing.diag.out=/tmp/diag.txt -jar server-release.jar
 *
 * <p>为什么不直接在控制台跑 js：服务端 js 命令对多行脚本的括号解析很脆弱（会把正常代码报成
 * "missing ) after argument list"），而这里要遍历整棵科技树、全部单位和方块，用 Java 落文件最稳。
 */
public final class Diag{
    private static final String modes = System.getProperty("sing.diag", "");

    private Diag(){}

    public static void run(){
        if(modes.isEmpty()) return;
        StringBuilder out = new StringBuilder();
        for(String m : modes.split(",")){
            switch(m.trim()){
                case "tech" -> tech(out);
                case "spawn" -> spawn(out);
                case "blocks" -> blocks(out);
                case "items" -> items(out);
                case "recipes" -> recipes(out);
                case "coverage" -> coverage(out);
                case "turrets" -> turrets(out);
                case "align" -> align(out);
                case "drills" -> installDrillProbe();
                case "waves" -> waves(out);
                default -> Log.warn("[sing/diag] 未知诊断项：@", m);
            }
        }
        String path = System.getProperty("sing.diag.out", System.getProperty("user.dir") + "/sing-diag.txt");
        try{
            new Fi(path).writeString(out.toString());
            Log.info("[sing/diag] 诊断输出已写入 @（@ 字节）", path, out.length());
        }catch(Throwable t){
            Log.err("[sing/diag] 写诊断文件失败", t);
        }
    }

    // ------------------------------------------------------------------ 科技树

    private static void collect(Seq<TechNode> into, TechNode n){
        into.add(n);
        for(TechNode c : n.children) collect(into, c);
    }

    private static String reqs(TechNode n){
        if(n.requirements == null || n.requirements.length == 0) return "(空)";
        StringBuilder b = new StringBuilder();
        for(ItemStack s : n.requirements){
            if(b.length() > 0) b.append('+');
            b.append(s.item == null ? "null" : s.item.name).append('x').append(s.amount);
        }
        return b.toString();
    }

    private static String objectives(TechNode n){
        if(n.objectives == null || n.objectives.isEmpty()) return "无";
        StringBuilder b = new StringBuilder();
        for(Objective o : n.objectives){
            if(b.length() > 0) b.append('+');
            String content = "?";
            if(o instanceof Research r) content = str(r.content);
            else if(o instanceof Produce p) content = str(p.content);
            else if(o instanceof SectorComplete sc) content = sc.preset == null ? "null" : sc.preset.name;
            else if(o instanceof OnPlanet op) content = "planet=" + (op.planet == null ? "null" : op.planet.name);
            else if(o instanceof OnSector os) content = "sector=" + (os.preset == null ? "null" : os.preset.name);
            b.append(o.getClass().getSimpleName()).append('(').append(content).append(')');
        }
        return b.toString();
    }

    private static void tech(StringBuilder out){
        for(TechNode root : TechTree.roots){
            Seq<TechNode> nodes = new Seq<>();
            collect(nodes, root);
            out.append("ROOT|").append(root.name)
               .append("|planet=").append(root.planet == null ? "null" : root.planet.name)
               .append("|content=").append(str(root.content))
               .append("|nodes=").append(nodes.size).append('\n');
            for(TechNode n : nodes){
                out.append("NODE|").append(root.name)
                   .append('|').append(str(n.content))
                   .append("|parent=").append(n.parent == null ? "null" : str(n.parent.content))
                   .append("|req=").append(reqs(n))
                   .append("|obj=").append(objectives(n))
                   .append("|kids=").append(n.children.size)
                   .append("|unlocked=").append(unlocked(n))
                   .append('\n');
            }
        }
    }

    private static boolean unlocked(TechNode n){
        try{
            return n.content != null && n.content.unlocked();
        }catch(Throwable t){
            return false;
        }
    }

    private static String str(UnlockableContent c){
        return c == null ? "null" : c.name;
    }

    // ------------------------------------------------------------------ 单位 / 刷怪

    private static void spawn(StringBuilder out){
        for(UnitType u : Vars.content.units()){
            out.append("UNIT|").append(u.name)
               .append("|hidden=").append(u.isHidden())
               .append("|player=").append(u.playerControllable)
               .append("|logic=").append(u.logicControllable)
               .append("|healer=").append(hasRepairBeam(u))
               .append("|builder=").append(hasBuildWeapon(u))
               .append("|flying=").append(u.flying)
               .append("|naval=").append(u.naval)
               .append("|health=").append(u.health)
               .append("|dps≈").append(estimateDps(u))
               .append("|canAttack=").append(u.canAttack)
               .append("|weapons=").append(u.weapons == null ? 0 : u.weapons.size)
               .append("|buildSpeed=").append(u.buildSpeed)
               .append("|mineSpeed=").append(u.mineSpeed)
               .append("|range=").append(u.range)
               .append("|speed=").append(u.speed)
               .append('\n');
        }
        for(mindustry.type.Planet p : Vars.content.planets()){
            int n = p.sectors == null ? 0 : p.sectors.size;
            out.append("PLANET|").append(p.name).append("|sectors=").append(n).append('\n');
        }
    }

    private static boolean hasRepairBeam(UnitType u){
        if(u.weapons == null) return false;
        for(mindustry.type.Weapon w : u.weapons){
            if(w instanceof mindustry.type.weapons.RepairBeamWeapon) return true;
        }
        return false;
    }

    private static boolean hasBuildWeapon(UnitType u){
        if(u.weapons == null) return false;
        for(mindustry.type.Weapon w : u.weapons){
            if(w instanceof mindustry.type.weapons.BuildWeapon) return true;
        }
        return false;
    }

    private static double estimateDps(UnitType u){
        if(u.weapons == null || u.weapons.isEmpty()) return 0;
        double dps = 0;
        for(mindustry.type.Weapon w : u.weapons){
            if(w == null || w.bullet == null) continue;
            float dmg = w.bullet.damage + (w.bullet.splashDamage > 0 ? w.bullet.splashDamage * 0.5f : 0f);
            dps += w.reload > 0 ? dmg / w.reload : 0;
        }
        return Math.round(dps);
    }

    // ------------------------------------------------------------------ 方块

    private static void blocks(StringBuilder out){
        for(Block b : Vars.content.blocks()){
            if(b == null || !b.name.startsWith("blackhole-")) continue;
            StringBuilder extra = new StringBuilder();
            if(b instanceof Drill d){
                extra.append("drillTime=").append(d.drillTime)
                     .append("|hardnessMul=").append(d.hardnessDrillMultiplier)
                     .append("|liquidBoost=").append(d.liquidBoostIntensity)
                     .append("|面板显示=").append(round(d.liquidBoostIntensity * d.liquidBoostIntensity)).append("倍")
                     .append("|liquids=").append(liquidStr(b));
            }
            if(b instanceof blackhole.IndustryParts.BeamMiner bm){
                extra.append("mineTime=").append(bm.mineTime)
                     .append("|tier=").append(bm.tier)
                     .append("|beams=").append(bm.beams)
                     .append("|liquidBoost=").append(bm.liquidBoostIntensity)
                     .append("|面板显示=").append(round(bm.liquidBoostIntensity * bm.liquidBoostIntensity)).append("倍")
                     .append("|liquids=").append(liquidStr(b));
            }
            if(b instanceof GenericCrafter g){
                extra.append("craftTime=").append(g.craftTime)
                     .append("|out=").append(stackStr(g.outputItems))
                     .append("|in=").append(stackStr(consumeItems(b)))
                     .append("|liquids=").append(liquidStr(b));
            }
            out.append("BLOCK|").append(b.name)
               .append('|').append(b.getClass().getSimpleName())
               .append("|size=").append(b.size)
               .append("|cat=").append(b.category)
               .append("|health=").append(b.health)
               .append('|').append(extra).append('\n');
        }
    }

    /** 一个方块可能有好几组物品消耗器（后置挂载的配方就是另开一组），要全部拼起来。 */
    private static ItemStack[] consumeItems(Block b){
        if(b.consumers == null) return null;
        Seq<ItemStack> all = new Seq<>();
        for(Consume c : b.consumers){
            if(c instanceof ConsumeItems ci && ci.items != null){
                for(ItemStack st : ci.items) all.add(st);
            }
        }
        return all.size == 0 ? null : all.toArray(ItemStack.class);
    }

    private static String liquidStr(Block b){
        if(b.consumers == null) return "无";
        StringBuilder s = new StringBuilder();
        for(Consume c : b.consumers){
            if(c instanceof ConsumeLiquid cl && cl.liquid != null){
                if(s.length() > 0) s.append(',');
                s.append(cl.liquid.name).append('x').append(cl.amount)
                 .append('@').append(cl.booster ? "加速" : "消耗");
            }
        }
        return s.length() == 0 ? "无" : s.toString();
    }

    private static String stackStr(ItemStack[] stacks){
        if(stacks == null || stacks.length == 0) return "无";
        StringBuilder b = new StringBuilder();
        for(ItemStack s : stacks){
            if(b.length() > 0) b.append('+');
            b.append(s.item == null ? "null" : s.item.name).append('x').append(s.amount);
        }
        return b.toString();
    }

    // ------------------------------------------------------------------ 物品 / 液体

    private static void items(StringBuilder out){
        for(Item i : Vars.content.items()){
            out.append("ITEM|").append(i.name).append("|cost=").append(i.cost)
               .append("|hardness=").append(i.hardness)
               .append("|radioactivity=").append(i.radioactivity)
               .append("|flammability=").append(i.flammability)
               .append("|charge=").append(i.charge)
               .append("|lowPriority=").append(i.lowPriority).append('\n');
        }
        for(Liquid l : Vars.content.liquids()){
            out.append("LIQUID|").append(l.name).append("|viscosity=").append(l.viscosity)
               .append("|temperature=").append(l.temperature)
               .append("|flammability=").append(l.flammability).append('\n');
        }
    }

    // ------------------------------------------------------------------ 炮塔弹道对齐

    /**
     * 炮塔的"炮口画在哪"和"子弹从哪出来"必须是同一个点。
     *
     * <p>模型是透视图：炮口在 headZ+z 的高度上，画出来的屏幕位置是
     * (cam.sx(x,z), cam.sy(y,z))。子弹是二维的，引擎把它放在世界坐标
     * 中心 + 旋转后的 (shootX, shootY)，再按 z=0 画出来 —— 也就是"世界偏移 = 屏幕偏移"。
     * 所以只有当 (shootX, shootY) 恰好等于炮口的屏幕偏移时才对齐；而屏幕偏移随朝向变化，
     * 用固定的 shootY 只能在朝北那一个角度对上。
     */
    private static void turrets(StringBuilder out){
        for(Block b : Vars.content.blocks()){
            if(!(b instanceof mindustry.world.blocks.defense.turrets.Turret t)) continue;
            String model = "无", muzzle = "无";
            float headZ = 0f, d = 0f, cy = 0f;
            blackhole.g3d.BlockModel bm = blackhole.models.Models.get(b.name);
            if(bm != null){
                model = bm.getClass().getName();
                if(bm instanceof blackhole.g3d.TurretModel tm){
                    StringBuilder ms = new StringBuilder();
                    for(float f : tm.muzzles){
                        if(ms.length() > 0) ms.append(',');
                        ms.append(f);
                    }
                    muzzle = ms.toString();
                    headZ = tm.headZ;
                    d = tm.cam.D;
                    cy = tm.cam.cy;
                }
            }
            out.append("TURRET|").append(b.name)
               .append("|size=").append(b.size)
               .append("|cls=").append(b.getClass().getSimpleName())
               .append("|shootX=").append(t.shootX)
               .append("|shootY=").append(t.shootY)
               .append("|defaultShootY=").append(b.size * 8f / 2f)
               .append("|pattern=").append(t.shoot == null ? "null" : t.shoot.getClass().getName())
               .append("|patternShots=").append(t.shoot == null ? 0 : t.shoot.shots)
               .append("|reload=").append(t.reload)
               .append("|model=").append(model)
               .append("|muzzle=").append(muzzle)
               .append("|headZ=").append(headZ)
               .append("|camD=").append(d)
               .append("|camCy=").append(cy)
               .append('\n');
        }
    }

    // ------------------------------------------------------------------ 刷怪表校验

    /**
     * 把 32 张战役图的刷怪表全部推演一遍：每张图能刷出多少种单位、拘留者来几只、
     * 单波最多多少只、整张图一共多少只。
     */
    private static void waves(StringBuilder out){
        ObjectSet<String> missing = new ObjectSet<>();
        int detainerMaps = 0, minTypes = 999, maxWave = 0;
        for(AureliaPlanetGenerator.Preset p : AureliaPlanetGenerator.campaign){
            if(p == null) continue;
            arc.math.Rand rand = new arc.math.Rand(p.id * 7919 + 3);
            arc.struct.IntSeq gates = new arc.struct.IntSeq();
            for(int i = 0; i < Math.max(1, p.spawns); i++) gates.add(i);
            Seq<mindustry.game.SpawnGroup> groups;
            try{
                groups = AureliaCampaign.waves(p, rand, gates);
            }catch(Throwable t){
                out.append("WAVES|id=").append(p.id).append("|生成失败: ").append(t).append('\n');
                continue;
            }
            ObjectSet<String> seen = new ObjectSet<>();
            int detainer = 0, total = 0, peak = 0;
            for(int w = 0; w <= p.winWave; w++){
                int n = 0;
                for(mindustry.game.SpawnGroup g : groups){
                    int c = g.getSpawned(w);
                    if(c <= 0) continue;
                    n += c;
                    seen.add(g.type.name);
                    if(g.type == astro.content.AstroContent.detainer) detainer += c;
                }
                total += n;
                peak = Math.max(peak, n);
            }
            maxWave = Math.max(maxWave, peak);
            minTypes = Math.min(minTypes, seen.size);
            if(detainer > 0) detainerMaps++;
            out.append("WAVES|id=").append(p.id)
               .append("|threat=").append(round(p.threat))
               .append("|winWave=").append(p.winWave)
               .append("|gates=").append(p.spawns)
               .append("|种类=").append(seen.size)
               .append("|拘留者=").append(detainer)
               .append("|单波最多=").append(peak)
               .append("|全图合计=").append(total).append('\n');
        }
        out.append("WAVES-SUMMARY|最少种类=").append(minTypes)
           .append("|刷拘留者的图=").append(detainerMaps)
           .append("|单波峰值=").append(maxWave).append('\n');
    }

    // ------------------------------------------------------------------ 矿机动画时钟探针

    /**
     * 每两秒把地图上所有采矿机的状态打一行日志，用来看"不干活时动画到底停没停"。
     *
     * <p>看什么：clock 是模型的转动时钟（以前直接等于 Time.time，永远在涨）；
     * warmup 是引擎判定的"正在产出"程度。没矿/没电/满载时 warmup 应该是 0，clock 也该停住。
     */
    private static void installDrillProbe(){
        final int[] tick = {0};
        mindustry.game.EventType.Trigger tr = mindustry.game.EventType.Trigger.update;
        arc.Events.run(tr, () -> {
            if(mindustry.Vars.world == null || mindustry.Vars.state == null || !mindustry.Vars.state.isGame()) return;
            if(tick[0]++ % 120 != 0) return;
            StringBuilder sb = new StringBuilder("DRILLPROBE t=").append(tick[0]);
            for(mindustry.gen.Building b : mindustry.gen.Groups.build){
                if(!isMiner(b.block)) continue;
                sb.append(" | ").append(b.block.name)
                  .append(" warm=").append(round(b.warmup()))
                  .append(" clock=").append(round(b.totalProgress()))
                  .append(" eff=").append(round(b.efficiency))
                  .append(" items=").append(b.items == null ? 0 : b.items.total());
            }
            arc.util.Log.info("@", sb);
        });
    }

    private static boolean isMiner(Block b){
        return b instanceof mindustry.world.blocks.production.Drill
            || b instanceof blackhole.IndustryParts.BeamMiner
            || b instanceof blackhole.ForgeParts.WallBore;
    }

    // ------------------------------------------------------------------ 运行时小工具（控制台 js 调）

    /** 统计当前地图上每种矿石覆盖层的格子数，给控制台 js 用。 */
    public static String oreCensus(){
        if(Vars.world == null || Vars.world.tiles == null) return "no world";
        ObjectIntMap<String> counts = new ObjectIntMap<>();
        for(int y = 0; y < Vars.world.height(); y++){
            for(int x = 0; x < Vars.world.width(); x++){
                mindustry.world.Tile t = Vars.world.tile(x, y);
                if(t == null || t.overlay() == null) continue;
                if(t.overlay() instanceof mindustry.world.blocks.environment.OreBlock ob && ob.itemDrop != null){
                    counts.increment(ob.itemDrop.name, 1);
                }
            }
        }
        StringBuilder sb = new StringBuilder("ORECENSUS");
        for(var e : counts){
            sb.append(" ").append(e.key).append("=").append(e.value);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ 弹道对齐校验

    /**
     * 逐个炮塔、逐个朝向算一遍：子弹最后画出来的位置，和模型炮口画出来的位置，差多少世界单位。
     *
     * <p>模型是透视投影，炮口在 headZ+z 的高度上；子弹是二维的，MuzzlePattern 先把它放在炮口的
     * 本地坐标上，再由 Lift 抬到投影后的屏幕点。这两条路径用的是不是同一组参数，量一下就知道。
     */
    private static void align(StringBuilder out){
        for(Block b : Vars.content.blocks()){
            if(!(b instanceof mindustry.world.blocks.defense.turrets.Turret t)) continue;
            blackhole.g3d.BlockModel bm = blackhole.models.Models.get(b.name);
            if(bm == null){
                // 前哨分包的两台炮有自己的模型类（不是 blackhole 的 TurretModel）
                if(!b.name.equals("blackhole-bastion-railgun") && !b.name.equals("blackhole-aegis-lancer")) continue;
            }
            if(!(t.shoot instanceof blackhole.g3d.MuzzlePattern mp)){
                out.append("ALIGN|").append(b.name).append("|未接入统一对齐|shootY=").append(t.shootY).append('\n');
                continue;
            }
            float worst = 0f, worstRot = 0f;
            for(int ri = 0; ri < 16; ri++){
                float rot = ri * 22.5f;
                for(int i = 0; i + 2 < mp.muzzles.length; i += 3){
                    float hx = mp.muzzles[i], hy = mp.muzzles[i + 1], hz = mp.muzzles[i + 2];
                    //1) 子弹：引擎先放在中心 + 旋转后的本地炮口，Lift 再抬到投影点
                    float c = Mathf.cosDeg(rot - 90f), sn = Mathf.sinDeg(rot - 90f);
                    float mx = hx * c - hy * sn, my = hx * sn + hy * c;
                    float z = mp.headZ + hz;
                    float sc = mp.camD / Math.max(mp.camD - z, 1f);
                    float bx = mx * sc, by = mp.camCy + (my - mp.camCy) * sc;
                    //2) 模型炮口画在哪
                    float px, py;
                    if(bm instanceof blackhole.g3d.TurretModel tm){
                        float[] o = new float[2];
                        tm.project(rot, hx, hy, hz, o);
                        px = o[0]; py = o[1];
                    }else if(b.name.equals("blackhole-bastion-railgun")){
                        float[] mw = outpost.gfx.RailgunModel.instance.muzzleWorld(rot - 90f,
                            outpost.gfx.RailgunModel.LIFT_FULL, 0f);
                        px = outpost.gfx.RailgunModel.instance.cam.sx(mw[0], mw[2]);
                        py = outpost.gfx.RailgunModel.instance.cam.sy(mw[1], mw[2]);
                    }else{
                        float[] mw = outpost.gfx.LancerModel.instance.muzzleWorld(rot - 90f,
                            outpost.gfx.LancerModel.LIFT_FULL, 0f);
                        px = outpost.gfx.LancerModel.instance.cam.sx(mw[0], mw[2]);
                        py = outpost.gfx.LancerModel.instance.cam.sy(mw[1], mw[2]);
                    }
                    float d = Mathf.dst(bx, by, px, py);
                    if(d > worst){ worst = d; worstRot = rot; }
                }
            }
            out.append("ALIGN|").append(b.name).append("|偏差=").append(round(worst))
               .append("|最大朝向=").append(worstRot).append('\n');
        }
    }

    private static float round(float v){
        return Math.round(v * 1000f) / 1000f;
    }

    // ------------------------------------------------------------------ 覆盖率 / 可解锁性

    /**
     * 检查两件事：
     * 1) 有没有"能造、看得见，却不在任何科技树上"的内容 —— 这种东西永远解锁不了；
     * 2) 欧雷莉亚树上还有没有指向外星物品的目标或需求。
     */
    private static void coverage(StringBuilder out){
        ObjectMap<UnlockableContent, TechNode> onAurelia = new ObjectMap<>();
        ObjectSet<UnlockableContent> anywhere = new ObjectSet<>();
        for(TechNode root : TechTree.roots){
            Seq<TechNode> nodes = new Seq<>();
            collect(nodes, root);
            for(TechNode n : nodes){
                anywhere.add(n.content);
                if(root.name.equals("aurelia")) onAurelia.put(n.content, n);
            }
        }

        int missing = 0;
        for(Block b : Vars.content.blocks()){
            if(b == null || !b.name.startsWith("blackhole-")) continue;
            if(b.buildVisibility == mindustry.world.meta.BuildVisibility.hidden
                || b.buildVisibility == mindustry.world.meta.BuildVisibility.debugOnly
                || b.buildVisibility == mindustry.world.meta.BuildVisibility.sandboxOnly) continue;
            if(b.requirements == null || b.requirements.length == 0) continue;
            if(!onAurelia.containsKey(b)){
                missing++;
                out.append("MISSING-AURELIA-NODE|block|").append(b.name).append('\n');
            }
        }
        for(UnitType u : Vars.content.units()){
            if(u == null || !u.name.startsWith("blackhole-") || u.isHidden()) continue;
            if(!onAurelia.containsKey(u)){
                missing++;
                out.append("MISSING-AURELIA-NODE|unit|").append(u.name).append('\n');
            }
        }
        for(Item i : Vars.content.items()){
            if(i == null || !i.name.startsWith("blackhole-")) continue;
            if(!onAurelia.containsKey(i)){
                missing++;
                out.append("MISSING-AURELIA-NODE|item|").append(i.name).append('\n');
            }
        }
        out.append("COVERAGE|缺失节点数=").append(missing).append('\n');

        // 欧雷莉亚树上残留的外星引用
        int bad = 0;
        for(ObjectMap.Entry<UnlockableContent, TechNode> e : onAurelia.entries()){
            TechNode n = e.value;
            if(n.requirements != null){
                for(ItemStack st : n.requirements){
                    if(st.item != null && !st.item.name.startsWith("blackhole-")){
                        bad++;
                        out.append("FOREIGN-REQ|").append(e.key.name).append('|').append(st.item.name).append('\n');
                    }
                }
            }
            for(Objective o : n.objectives){
                UnlockableContent c = null;
                if(o instanceof Research r) c = r.content;
                else if(o instanceof Produce p) c = p.content;
                if(c != null && !c.name.startsWith("blackhole-")){
                    bad++;
                    out.append("FOREIGN-OBJ|").append(e.key.name).append('|').append(c.name).append('\n');
                }
            }
        }
        out.append("FOREIGN|外星引用数=").append(bad).append('\n');

        // 目标指向的内容本身必须可解锁，否则这个目标永远完成不了
        int unreachable = 0;
        for(ObjectMap.Entry<UnlockableContent, TechNode> e : onAurelia.entries()){
            for(Objective o : e.value.objectives){
                UnlockableContent c = null;
                if(o instanceof Research r) c = r.content;
                else if(o instanceof Produce p) c = p.content;
                if(c == null) continue;
                if(onAurelia.containsKey(c) || c.alwaysUnlocked) continue;
                unreachable++;
                out.append("OBJ-UNREACHABLE|").append(e.key.name).append('|').append(c.name).append('\n');
            }
        }
        out.append("OBJ-UNREACHABLE|合计=").append(unreachable).append('\n');
    }

    // ------------------------------------------------------------------ 配方

    private static void recipes(StringBuilder out){
        for(Block b : Vars.content.blocks()){
            if(b instanceof GenericCrafter g){
                out.append("RECIPE|").append(b.name)
                   .append("|in=").append(stackStr(consumeItems(b)))
                   .append("|out=").append(stackStr(g.outputItems))
                   .append("|liquids=").append(liquidStr(b))
                   .append("|time=").append(g.craftTime).append('\n');
            }
        }
    }
}
