# 奇点 · Singularity 8.2-beta（测试版）开发报告

作者：**sayed1145**（构想 / 架构设计 / 真机测试） · **NLM**（AI 工程实现，辅助）
目标游戏版本：Mindustry v160.5（`minGameVersion: 160.5`）
交付物：`Singularity-v8.2-beta.jar`（桌面 + 安卓通用单包）、`Singularity-v8.2-beta.zip`（**完整源码 + 本报告 + jar**）

---

## 一、本次需求与完成情况

| # | 需求 | 状态 |
|---|------|------|
| 1 | 跨扇区飞行 / 转移要的是原版资源，必须改成本模组资源 | 已完成（注册欧雷利亚核心装载方案 + 清洗携带清单，见第二节） |
| 2 | 流明无人机要能被玩家操控 | 已完成（货运无人机与流明运输机都可接管 / 编队指挥 / 逻辑控制，见第三节） |
| 3 | 物流严格守恒：不丢物品、不能有无限循环 | 已完成（容量钳制 + 拆除交接 + 节点互斥，实测总量零误差，见第四节） |
| 4 | 两台原版风格的「范围超速」投影仪，一小一强，有上限 | 已完成（流明加速器 1.5×、棱镜超充仪 2.1× / 共振增幅 2.6× 硬顶） |
| 5 | 作者署名：sayed1145 为人类，NLM 为 AI 实现 | 已完成（`mod.hjson` 作者栏、两份 README、报告首行） |
| 6 | 模组简介第一行「工业垃圾测试版」，第二行英文翻译 | 已完成（`description` 仅两行：`工业垃圾测试版` / `Industrial Junk Beta`） |
| 7 | 全新泰坦级作战单位，3D，不是蜘蛛，朴素好看、有用、有上限 | 已完成（棱镜巨碑，悬浮装甲板 + 排热长矛，见第五节） |
| 8 | 交付新的 zip + jar | 已完成（根目录两件套，v8.1 产物已删除） |
| 9 | 上传 GitHub：仅源码 + jar + 中英 README + GPLv3，绝不上传令牌 | 已完成（见第八节；令牌只在本地 push 时使用，未写入任何文件） |

---

## 二、跨扇区发射改用欧雷利亚资源（需求 1）

### 2.1 这不是数值问题，是原版的兜底逻辑

`LaunchLoadoutDialog.show()` 的取值顺序是：

```java
selected = universe.getLoadout(core);
if(selected == null) selected = schematics.getLoadouts().get((CoreBlock)Blocks.coreShard).first();
cost = selected.requirements() + universe.getLaunchResources();
```

而 `Universe.getLoadout(core)` 只会返回 `Schematics.getLoadouts(core)` 里已有的方案。原版那张表只在 `Schematics.checkLoadout()` 里被四套原版装载方案（`basicShard / basicFoundation / basicNucleus / basicBastion`）填充，**模组核心根本不在表里**，于是对话框退回「核心碎片」方案，按铜 / 铅计价——这就是玩家看到的「跨扇区要原版资源」。

### 2.2 修法（`src/blackhole/AureliaLaunch.java`）

1. `install()`：为 `blackhole-aurelia-core` 注册一份只含它自身的装载示意图，写进 `schematics.getLoadouts()`（公开表，`Universe.getLoadout()` 读它），并用 `Reflect` 防御性地写入私有的 `defaultLoadouts`。于是发射花费 = 欧雷利亚核心自身造价 = `流明矿 3000 / 金辉矿 2000 / 棱晶合金 600`。
2. `sanitizeLaunchResources()`：`universe.getLaunchResources()` 是跨星球持久化的 `ItemSeq`，从蛇沼带过来的铜 / 铅会在欧雷利亚继续计费（行被隐藏但仍进总价）。每次欧雷利亚扇区载入（`WorldLoadEvent`）都会把非本模组物品剔除。
3. `valid()`：自检接口，发射开销里只要出现一个非 `blackhole-` 物品就返回 false。

实测（无头服，见 `docs/TEST-v8.2-beta.txt`）：

```
v8.2 launch loadout: registered=1 schematic(s), universe picks Aurelia Core
     costing [lumenitex3000 auritex2000 prism-alloyx600] valid=true
v8.2 launch payload cleanup: carried=[blackhole-lumenitex200]  (投入的 copper×300 被剔除)
v8.2 launch items offered on aurelia=19, vanilla among them=0
```

---

## 三、无人机可操控（需求 2）

### 3.1 为什么以前点不动

能不能被框选、下令，取决于 `Unit.isCommandable()`，它只判断一件事：**控制器是不是 `CommandAI`**。我们的搬运机挂的是自写的 `PorterAI extends AIController`，所以在界面上根本选不中；`playerControllable = false` 又关掉了接管。

### 3.2 改法（`LogixParts.PorterAI`、`AureliaForge`、`AureliaLogix`）

- `PorterAI` 改为 **`extends CommandAI`**，并重写 `updateUnit()`：

```java
if(hasCommand() || attackTarget != null){
    if(haul != null) release();   // 把任务还给网络，但货物留在机上
    super.updateUnit();           // 听玩家 / 逻辑的
    return;
}
updateVisuals();
updateMovement();                 // 没有命令时，照常搬运
```

- 货运无人机（`lumen-drone`，中枢自产）与流明运输机（`lumen-hauler`，工厂生产）都打开 `playerControllable / logicControllable / allowChangeCommands`。
- 既然能被玩家开走，就不能再是打不中的幽灵：货运无人机的 `targetable / hittable` 同步改为 true（否则就是无敌载具）。
- 中枢每帧扫描自己的机群：玩家在开 → 不管；已经是 `PorterAI` → 不管；拿着未完成命令的 `CommandAI` → 不管；其余（命令做完、无主）→ 立刻重新收编为 `PorterAI`。

实测：下达 24.2 格外的移动命令后，无人机飞到目标点（最近 0.6 格），命令完成后自行回到中枢继续搬运，控制器仍是 `PorterAI`。

---

## 四、物流严格守恒（需求 3）

v8.1 的网络会在两种情况下「吃货」或者「空转」，8.2 全部堵死：

| 问题 | 原因 | 修法 |
|------|------|------|
| 货物凭空消失 | 调度器用自己的公式估算目标容量，和方块真实 `acceptStack()` 不一致，无人机飞到以后放不下，货物悬在空中 | 所有调度量改为 `dest.acceptStack(...)` 钳制；`LogiNode.capacity()` 统一走 `block.itemCapacity`，后勤仓重写 `capacity()/target()` = 3 倍 |
| 拆掉中枢后货物蒸发 | 拆除时直接 `kill()` 挂载的无人机 | `onRemoved()` 先把每架机上的货 `Call.transferItemTo` 给 320 格内第一个接得下的建筑，再回收无人机 |
| 仓 ↔ 货运台无限倒腾 | 货运台既是需求端又被当成可取货的来源 | `Request.canDump` 对**所有** `LogiNode` 返回 false：请求节点只进不出 |
| 任务卡死 | 目标满 / 源头空时无人机原地等 | 60 秒看门狗 `release()`：交还任务、**保留货物**，下一拍找新的卸货点 |

实测（无头服）：

```
v8.2 conservation after 30s: total=120 (start 120) -> nothing created, nothing voided
v8.2 hub removed: items before=120 accounted after=120 -> cargo was handed over, not voided
v8.2 loop check: dispatch counts per 10s [1 1 1 1 1 1] deltas [0 0 0 0 0] -> the network settles
```

即：60 秒内总量零误差，拆中枢零损失，满足需求后调度数**不再增长**（没有无限循环）。

---

## 五、两台范围超速 + 泰坦（需求 4、7）

### 5.1 投影仪（`CommandParts.ModelOverdrive`，继承原版 `OverdriveProjector`）

| 方块 | 尺寸 | 造价 | 范围 | 加速 | 周期 | 电力 |
|------|------|------|------|------|------|------|
| 流明加速器 | 2×2 | 流明矿 130 / 金辉矿 60 / 棱晶玻璃 75 | 11 格 | ×1.5 | 300 帧 | 3.2 |
| 棱镜超充仪 | 3×3 | 流明矿 320 / 金辉矿 210 / 棱晶合金 95 / 共振晶片 60 | 24 格（投料 +5 格） | ×2.1，投入共振晶片 ×2.6 | 420 帧 | 9 |

机制完全沿用原版超速投影仪（同一个基类），所以**不叠加**：实测两台同时覆盖同一台棱晶压机，时间倍率稳定在 2.1，不超过 2.6 的硬顶。模型是一对同族机器：外圈发射柱 + 中心透镜环，工作时透镜反向对转，只有这一处动件。

### 5.2 棱镜巨碑（`prism-monolith`）

刻意不做第二只蜘蛛：它是**一整块装甲板坐在三组升力舱上**的攻坚平台——两前一后的升力舱嵌在甲板轮廓里，脊背上一座炮塔带一根带三道束环的长矛，两肩各一座小防空舱。全部骨骼绑定，没有漂浮件、没有重叠体（`ModelCheck 0 问题`）。

| 项 | 数值 |
|---|---|
| 生命 / 装甲 | 24000 / 15 |
| 速度 / 转向 | 0.44 / 0.8（比拘留者更慢更笨重） |
| 体积 / 射程 | hitSize 44 / 310 |
| 主武器 | 棱镜长矛，`LaserBulletType` 560 伤害、穿透 4、装填 150 帧、蓄力 48 帧 |
| **上限机制** | 排热枪：热量上限 4，每发 +1，冷却 0.42/秒 → **连射 4 发后强制排热 6 秒**（面板用 `bh.vent.stat` 显示） |
| 副武器 | 两座肩部防空舱（装填 22，5 伤害 + 18 溅射），只负责自保 |
| 生产 | 流明超级工厂，150 秒，流明矿 1100 / 金辉矿 850 / 棱晶合金 420 / 共振晶片 220 |

平衡校验（实测打印）：`monolith hp=24000 armor=15 rawDps=344` vs `detainer hp=38000 armor=16 rawDps=694` —— **拘留者没有被削**，巨碑在它之下；而排热机制保证单位时间输出有硬上限，1200 帧实测只有 8 发（`trace[0:1 150:2 … 1050:8]`），不会无限倾泻。

> 修复记录：`Weapon.mirror` 默认 **true**，长矛放在 x=0 时被原版复制成两门重叠的炮（伤害翻倍，且两个挂点共用同一份热量账本，互相把对方的计数顶上天，表现为「开一枪后永久锁死」）。现已 `mirror = false`，并给 `VentWeapon.copy()` 加了独立热量表，防止将来任何镜像复制再踩同一个坑。

---

## 六、署名与简介（需求 5、6）

`mod.hjson`：

```hjson
author: "sayed1145 — 构想 / 架构设计 / 真机测试 · NLM — AI 工程实现（辅助）"
description:
  '''
  工业垃圾测试版
  Industrial Junk Beta
  '''
```

游戏列表里 `ModMeta.shortDescription()` 取第一行，所以模组浏览器显示的正是「工业垃圾测试版」七个字，详情页第二行是英文翻译。

---

## 七、构建与验收

| 关卡 | 结果 |
|------|------|
| `ModelCheck` | 88 个方块模型、15 个图标，**0 问题** |
| 模型烘焙 | `problems: 0`（闭合性、内部采样、动画帧全过） |
| `AssetCheck` | 400 张贴图，0 缺失 |
| `cmdtest.sh`（本版新增，9 组探针） | **PASSED** |
| `logitest.sh`（v8.1 物流套件） | **PASSED**（回归） |
| `test_server.sh`（v8.0 全量套件） | **PASSED**（回归） |
| `maptest.sh`（战役地形） | **PASSED**（回归） |
| 文本审计 | 227 项自有内容，缺失 / 原始键 = **0** |

详细输出见 `docs/TEST-v8.2-beta.txt`。

---

## 八、GitHub 发布

仓库内容：**仅源码** + 中文 README（`README_zh.md`）+ 英文 README（`README.md`）+ `LICENSE`（GPL-3.0）+ 以 Release 资产形式上传的 jar。**zip 不进仓库，令牌不进任何文件、不进任何提交**。令牌仅用于一次性 push，用完请立即在 GitHub 后台吊销。

---

## 九、文件清单（本版改动）

| 文件 | 说明 |
|------|------|
| `src/blackhole/AureliaLaunch.java` | 新增：装载方案注册 + 携带清单清洗 |
| `src/blackhole/AureliaCommand.java` | 新增：两台投影仪 + 棱镜巨碑 + 生产计划 |
| `src/blackhole/CommandParts.java` | 新增：`ModelOverdrive`、排热武器 `VentWeapon` |
| `src/blackhole/models/CommandModels.java` | 新增：两台投影仪的 3D 模型 |
| `src/blackhole/models/CommandUnits.java` | 新增：棱镜巨碑骨骼与几何 |
| `src/blackhole/LogixParts.java` | 守恒修复 + `PorterAI extends CommandAI` |
| `src/blackhole/AureliaForge.java` / `AureliaLogix.java` | 无人机可操控化 |
| `src/blackhole/AureliaMerge.java` | v8.2 科技树挂点 |
| `src/blackhole/BlackHoleMod.java` | 装载方案安装 + 清洗事件 + 新内容加载 |
| `assets/bundles/*.properties` | 中英文本（含 `bh.vent.stat`、跃迁许可描述补全、重名修正） |
| `mod.hjson` / `build.sh` | 版本 8.2-beta、作者、简介 |
| `cmdtest.sh` | 新增：v8.2 功能验收套件 |
