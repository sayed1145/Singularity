# 奇点 · Singularity 8.0-beta（测试版）开发报告

作者：sayed1145 · 目标游戏版本：Mindustry v160.5（`minGameVersion: 160.5`）
交付物：`Singularity-v8.0-beta.jar`（桌面 + 安卓通用）、`Singularity-v8.0-beta.zip`（**完整源码 + 本报告 + jar**）

---

## 一、本次版本的需求与完成情况

| # | 需求 | 状态 |
|---|------|------|
| 1 | 不只是堆内容，要有**深度**：所有产物都要有去处，所有单位都能造 | 已完成（新增自动审计，19/19 物品、5/5 流体都有消耗者） |
| 2 | 裂隙吸收墙太强：加**上限 / 冷却**，过载后暂时失效 | 已完成（2600 点吸收上限 + 8 秒熄火冷却） |
| 3 | 装卸器移入**运输（分配）分类** | 已完成 |
| 4 | 新钻机：对墙上的矿脉**直接连出激光**开采 | 已完成（凿墙机 `wall-bore`） |
| 5 | **建造速度 ×2** | 已完成（欧雷莉亚星球规则 `buildSpeedMultiplier = 2`） |
| 6 | 新的**运输机制** + 3D 模型 | 已完成（流明货运站 + 货运台 + 货运无人机） |
| 7 | **大型流明修复器**：修得更快 | 已完成（`lumen-mend-dome`，12 %/秒、20 格半径） |
| 8 | **大型流明电池**：容量大得多 | 已完成（`lumen-accumulator`，14000，11.7 倍） |
| 9 | **陆地抽水机**：远离水源也能抽水 | 已完成（`ground-well`，按土壤含水量出水） |
| 10 | 战役地图**全部随机、互不重复、至少 30 张**，且平衡可玩 | 已完成（32 个扇区，程序生成，抽样校验唯一且平衡） |
| 11 | 单位指令大修：能挖的自动挖、能造的协助建造，暴露全部能力；**更多单位**且都能造 | 已完成（34 个单位全部重算指令，新增 3 支单位） |
| 12 | 天文拘留者改名为**拘留者**，保留 T6 标记 | 已完成 |
| 13 | 参考官方源码研究更多后勤 / 采矿 / 逻辑内容 | 已完成（见第七节） |
| 14 | 完整的改动与平衡说明 | 本报告 |
| 15 | 不得削弱拘留者或任何已有单位 | 已遵守（新增 1 项方块负面改动，单位数值零改动） |

---

## 二、深度：所有东西都有用

新增 `v8.0 depth audit` 自动检查（`test_server.sh`）：遍历全部方块的建造成本、`ConsumeItems` / `ConsumeLiquid(s)` / `ConsumeItemFilter`、单位工厂配方、炮塔弹药表与单位弹药，统计每一种本模组物品 / 流体是否至少有一个消耗者。

结果：**19 / 19 物品、5 / 5 流体都有去处**，没有"产出来却没人要"的死胡同。本次新增的六个方块也都挂进了既有的生产链：

* 凿墙机消耗 `lumen-plasma`（可选加速），把原本只能被汇聚器慢慢刮的墙体矿脉变成一条正式产线。
* 货运站 / 货运台用 `prism-alloy`、`resonance-shard` 计价，是棱镜与共振支线的新出口。
* 陆地抽水机产出 `tidewater`，直接接到既有的涡轮、等离子与冷却链上。

---

## 三、裂隙吸收墙的平衡（唯一一处"削弱"）

`src/blackhole/BastionParts.java` 的 `AbsorberWall`：

| 参数 | v7.9 | v8.0-beta | 说明 |
|------|------|-----------|------|
| `absorbBudget` | 6 发 / 秒 | 6 发 / 秒（不变） | 每秒最多吞 6 发，其余照常命中 |
| `absorbCapacity` | 无 | **2600 点伤害** | 吸收量累计成"热"，到顶即崩溃 |
| `overheatTime` | 无 | **8 秒** | 崩溃后完全失效：不吸收、不发电，只是一堵墙 |
| `coolRate` | 无 | **21 点 / 秒** | 工作时热量自然消退；持续火力超过 21 点/秒必定会把它烧穿 |

要点：

* 热量与电荷分开记录，并写进存档（`version() = 1`，旧档按 0 版读取，不会损坏）。
* 失效期间 `getPowerProduction()` 恒为 0，电荷清零——不存在"过载前先把电充满"的套利。
* 画面上有反馈：蓄热过半时环会变红，熄火时画成四段断裂的弧，玩家一眼看得出它正在冷却。

实测（见 `docs/TEST-v8.0-beta.txt`）：在每刻 2 发的极端火力下吞下 2406 发后 **1203 刻（约 20 秒）熄火**，熄火期间输出为 0，再 **480 刻（8 秒）恢复**；正常交火强度下它只会间歇性罢工，不会再出现永远无敌的墙。

---

## 四、新增方块

| 方块 | 分类 | 规格 | 说明 |
|------|------|------|------|
| 凿墙机 `wall-bore` | 生产 | 2×2，射程 6 格，4 级 | 继承原版 `BeamDrill`：向正前方的岩壁射出**直线激光**，每条通道切一格墙体矿脉；注入 `lumen-plasma` 加速 2.1 倍 |
| 流明货运站 `lumen-cargo-station` | 分配 | 3×3 | `UnitCargoLoader`：自建并保有一架货运无人机，空运物品到任意货运台 |
| 流明货运台 `lumen-cargo-point` | 分配 | 1×1 | `UnitCargoUnloadPoint`：设定接收物品，转交给相邻方块 |
| 大型流明修复器 `lumen-mend-dome` | 效果 | 3×3 | 20 格半径、12 %/秒（小型修复器为 5 格、3.6 %/秒），棱镜玻璃可强化 |
| 大型流明电池 `lumen-accumulator` | 电力 | 3×3 | 14000 缓冲（流明电池 1200，11.7 倍），爆炸性同步提高 |
| 陆地抽水机 `ground-well` | 流体 | 2×2 | `SolidPump`：按脚下 `Attribute.water` 出 `tidewater`，不需要湖泊 |

全部六个方块都带**运行时程序化 3D 模型**（`src/blackhole/models/ForgeModels.java`）：凿墙机有会转向的发射头、货运站有停机坪与慢转风向标、修复穹顶的透镜随治疗脉冲转动、蓄电组中央的光柱随电量升降、陆地抽水机是一台 ±14° 点头的游梁式抽油机。所有旋转都在各自平面内，没有飞散零件；`ModelCheck` 与 `bake` 均 0 问题。

凿墙机的两点实现细节：

1. 原版 `BeamDrill` 的 `@-top`、`@-glow` 贴图**没有 fallback**，直接继承会渲染出错误贴图；因此重写 `load()` 把它们指向自己的预览图 / 置空。
2. 重写 `BeamDrillBuild.draw()`：先画 3D 模型，再用 `Lines.line` 为每条通道画一条到目标岩壁格的直线激光，并在岩面上画切割标记——与需求里"直接连出一条激光"的描述一致。

---

## 五、三支新单位（全部可造、不隐藏科技）

| 单位 | 底盘 | 定位 |
|------|------|------|
| 货运无人机 `lumen-drone` | `Ship3D` | 货运站自带，不参战、不可被玩家制造（与原版货运机一致） |
| 修复无人机 `lumen-medic` | `Ship3D` | 760 HP，修复光束治疗建筑，`buildSpeed = 0.8`，无攻击武器 |
| 工程车 `aurite-wright` | `Tank3D` | 1500 HP，`buildSpeed = 2.4`、载货 150、3 级采矿，能修补建筑 |

后两者加进了欧雷莉亚制造厂的配方表（32 秒 / 36 秒），并挂在科技树上（修复无人机接在流明信使后，工程车接在泰拉掘进者后）。

---

## 六、单位指令大修

`src/blackhole/UnitCapabilities.java`，在 `Mod.init()` 中对所有 `blackhole-` 单位执行一次能力推导：

| 条件 | 追加的指令 |
|------|-----------|
| 全部单位 | `moveCommand` |
| `mineTier > 0 且 (mineFloor 或 mineWalls)` | `mineCommand` |
| `buildSpeed > 0` | `rebuildCommand`、`assistCommand` |
| 武器弹体带治疗（`healPercent / healAmount > 0`）或 `canHeal` | `repairCommand` |
| `payloadCapacity > 0` | `enterPayload`、`loadUnits`、`loadBlocks`、`unloadPayload`、`loopPayload` |
| `allowedInPayloads` | `enterPayloadCommand` |

默认指令（出厂即开工）：

* **专职矿机**（能挖且 `mineSpeed ≥ 8`，或没有攻击武器）→ 默认 `mineCommand`，造出来就自己去挖矿；
* 不带攻击武器的工程 / 医疗单位 → 默认 `rebuildCommand` / `repairCommand`；
* 普通战斗单位保持默认 `moveCommand`，避免它们擅自离开防线——指令仍然能手动切换。

这一遍只**增加**指令、从不删除，且不修改任何单位的属性，因此"不得削弱任何单位"的约束得到遵守。实测 34 个单位全部通过，5 个单位（流明信使、堡垒驾驶员、泰拉掘进者、修复无人机、工程车）出厂即自动作业。

---

## 七、战役：32 个随机扇区

`AureliaPlanetGenerator.campaign` 不再是七张手写地图，而是用固定种子（`Rand(90210)`）一次性生成的 **32 个扇区**表：

* 落点在整颗星球的 92 个扇区里打散，首个扇区固定为 `startSector = 10`；
* 每个扇区自行抽取尺寸（272–348）、刷怪口数量（2–5）、水量、矿物富集度与通关波次，并保证**参数组合不重复**；
* 难度沿表的顺序平滑上升（威胁度 0.16 → 0.95，通关波次 15 → 80 上下）；
* 地形本身由扇区 id 驱动噪声与 `Rand` 生成，所以两张图不可能长得一样。

为了让"随机"不会变成"随机到不能玩"，生成器补了两处：

1. **墙体矿自适应**（`wallOres()`）：原先用固定噪声阈值 0.62 取矿脉，随机化尺寸后有的扇区只剩 15 格墙矿。现在先把所有暴露岩面按矿脉噪声值排序，取最富的约 7%（且不少于 150 格），矿脉形状仍然跟着岩层走，但每张图都有足够的墙矿可挖。
2. **资源兜底**（`ensureOre()` / `ensureWater()`）：生成结束、校验之前统计一次，若某种矿或水体低于可玩底线，就在**当前矿物最少的象限**、离核心 34 格以外补投矿脉 / 再挖一个浅水潟湖，直到达标。补投走的是既有的 `patch()` / `lagoon()`，不会破坏连通性，也不会把资源堆到核心脚下。

验收方式：

* **全量扫描**（一次性专项测试）：32 个扇区**全部**真实生成 —— `32/32 通过，墙体矿 39–222 格，地形指纹互不相同`。
* **常规回归**（`test_server.sh`）：在 32 个扇区里等间隔抽样 8 个真实生成，统计刷怪口到核心的距离、核心周围的开阔度、四象限矿物分布、新地形种类、墙体矿数量，并对地形采样做指纹比对确认没有两张图相同。旧版那套平衡红线（刷怪口等距、开阔中心 ≥1500 格、象限矿物差 ≤55%、至少 6 种新地形）全部保留。

---

## 八、其它改动

* **装卸器**：`Category.effect → Category.distribution`（`group` 原本就是 `transportation`）。
* **建造速度 ×2**：改在欧雷莉亚星球的 `ruleSetter` 里设 `rules.buildSpeedMultiplier = 2f`，不碰任何单位的 `buildSpeed`，所以不存在"顺手改了别的单位数值"的风险。
* **改名**：`Lumen Detainer / 流明拘留者` → `Detainer / 拘留者`；T6 标记移到描述首位（`T6. The heaviest unit…` / `T6：模组中最重的单位…`），血量 38000、速度 0.66、护甲 16 等全部数值未动。
* 新增 25 条中英文本（`assets/bundles/*.properties`），全部是朴素、简短的描述。

---

## 九、参考的官方源码（研究记录）

| 源码 | 用途 |
|------|------|
| `world/blocks/production/BeamDrill.java` | 墙体激光钻机的全部字段与 `updateFacing` / `updateLasers` 逻辑；得知 `@-top`、`@-glow` 无 fallback |
| `world/blocks/units/UnitCargoLoader / UnitCargoUnloadPoint` | 空运后勤的两端、`unitType` / `unitBuildTime` / `staleTimeDuration` |
| `ai/UnitCommand.java`、`type/UnitType.java` | 指令清单、`defaultCommand`、`allowChangeCommands`；确认 v8 没有 `boostCommand` |
| `world/blocks/production/SolidPump.java` | 陆地抽水机：`attribute`、`baseEfficiency`、`rotateSpeed` |
| `world/blocks/power/Battery.java`、`defense/MendProjector.java` | 大型电池与大型修复器直接复用既有子类，无需新基类 |

---

## 十、测试

`./test_server.sh` 以官方 v160.5 无头服务器跑完整套（启动 → 内容 / 科技树 / 星球隔离 → 扇区生成 → 全单位存活与开火 → 各类方块实机探针 → 存档读档）。本版新增 11 条 v8.0 探针，全部通过，完整输出见 `docs/TEST-v8.0-beta.txt`。

关键实测行：

```
v8.0 wall bore: 12 ore-bearing wall tiles in front, canPlaceOn=true, lifted 2 aurite out of the rock after 190 ticks
v8.0 absorber wall: ceiling=2600, cooling 21/s; 2406 shots in 1203 ticks -> inert, power 0, back after 480 ticks
v8.0 cargo station: drone built in 1321 ticks -> lumen-drone alive hp 180, drop pad accepts lumenite
v8.0 support tier: mender 5 tiles 3.6 %/s vs dome 20 tiles 12 %/s; cell 1200 vs accumulator 14000 (11.7x)
v8.0 ground well on frost-crust (water 0.3, liquid tiles under it 0): pumped tidewater in 29 ticks
v8.0 unit control: 34 units checked, 5 start working on their own
v8.0 depth audit: items with a consumer=19/19, liquids with a consumer=5/5
sector sweep: 32/32 generated, wall ore 39-222, identical maps=0
TEST PASSED: Singularity v8.0-beta on v160.5 headless
```

质量闸门：`ModelCheck 0 问题`、`bake 0 问题`、`AssetCheck 360 贴图 0 缺失`、服务器日志 0 异常。
