# 奇点 · Singularity 8.3-beta（测试版）开发报告

作者：**sayed1145**（构想 / 架构设计 / 真机测试） · **NLM**（AI 工程实现，辅助）
目标游戏版本：Mindustry v160.5（`minGameVersion: 160.5`）
交付物：`Singularity-v8.3-beta.jar`（桌面 + 安卓通用单包）、`Singularity-v8.3-beta.zip`（**完整源码 + 本报告 + jar**）

---

## 一、本次需求与完成情况

| # | 需求 | 状态 |
|---|------|------|
| 1 | 加入「废料」，废料最终能产出一切，但必须经过**很深**的产线，不能一步到位 | 已完成（废料为副产物，六台机器的回收链，见第二节） |
| 2 | 加入三级核心 / 三级基地，并且要能造出来 | 已完成（欧雷利亚堡垒城 6×6 + 城塞领航者，见第三节） |
| 3 | 任何基地都不能开采全部等级的矿物，能采一部分即可 | 已完成（领航者按等级 / 墙体分工，共振永远要钻机，见第四节） |
| 4 | 修复天文（拘留者）模块：面板、设置、跃迁全部回来，只改名称 | 已完成（补回 `astro/ui` + `astro/ai` + 指挥方块，见第五节） |
| 5 | 地下水提取器抽水速度 ×2.5 | 已完成（7.00/s → 17.50/s，且任何地面都能抽，见第六节） |
| 6 | 版本号 8.3-beta，**模组简介不要改** | 已完成（`description` 仍为 `工业垃圾测试版` / `Industrial Junk Beta`，只改副标题） |
| 7 | 新护盾建筑：特效**全新自写**、朴素不刺眼、有容量上限与冷却、可用液体 / 物品增强 | 已完成（守护穹顶，见第七节） |
| 8 | 交付新的 zip + jar | 已完成（根目录两件套，v8.2 产物已删除） |
| 9 | 所有测试通过，无错误 | 已完成（四套无头服务器套件全绿，见第九节） |

---

## 二、废料循环（需求 1）

### 2.1 废料从哪来：它只能是副产物

新加入 1 种「原料」和 2 种中间物、1 种液体：

| 内容 | 作用 |
|------|------|
| 工业废料 `industrial-waste` | 淤砂窑、等离子混合器、棱镜压机在正常产出之外，每次合成附带掉落 1 份 |
| 金属碎屑 `metal-swarf` | 废料中的金属部分 |
| 矿渣粉 `slag-dust` | 废料中的矿物部分 |
| 流明浆 `lumen-slurry`（液体） | 矿渣洗出的富矿浆，是后三步的共同输入 |

关键设计：**没有任何建筑"生产"废料**，它只能作为生产的副产品出现。实测三台源头机器的产出：

```
silt-kiln[prism-glassx1 industrial-wastex1]
plasma-mixer[prism-alloyx1 industrial-wastex1]
prism-press[prism-glassx2 industrial-wastex1]
```

因此回收线的上限永远被"正在生产的工厂规模"卡住，不可能自给自足地无限滚雪球。

### 2.2 六步链：废料确实能变成一切，但要走很远

```
工业废料 ──废料分拣厂(4废料 → 1碎屑 + 1渣粉)
   ├─ 金属碎屑 ──碎屑熔炉(3碎屑 → 1金矿)
   └─ 矿渣粉  ──矿渣淋洗塔(3渣粉 + 潮汐水 → 流明浆)
                   ├─ 浆液结晶器(流明浆 → 2流明矿)
                   ├─ 共振再合成器(流明浆 + 4流明矿 + 3金矿 → 1共振碎晶)
                   └─ 棱晶再生炉(2共振碎晶 + 6棱镜玻璃 + 流明浆 → 2棱镜合金)
```

* **每一步都在减量**：4 份废料只换 1 碎屑 + 1 渣粉，3 碎屑只换 1 金矿。整条链是净亏的物质转换，不存在复制回路。
* **深度 = 6 台机器**：从废料到棱镜合金要跨 6 台设备、3 种中间物、1 种液体和 1 条水线，不是"一台机器出一切"。
* **废料仓**（3×3，容量 900，**只收废料**）负责缓冲：工厂是成批吐料，回收线是匀速吃料。

实测（无头服务器，逐台喂料跑到第一份产出）：

```
waste-reclaimer[1 metal-swarf in 70 ticks]   swarf-furnace[1 aurite in 81 ticks]
leach-tower[lumen-slurry 立即起浆]            slurry-crystalliser[2 lumenite in 76 ticks]
resonance-resynthesiser[1 resonance-shard in 151 ticks]
prism-reformer[2 prism-alloy in 121 ticks]
```

深度审计探针结论：`-> deep chain, no single machine turns waste into everything`。

---

## 三、三级核心：欧雷利亚堡垒城（需求 2）

| 项 | 一级 欧雷利亚核心 | 二级 堡垒核心 | **三级 堡垒城** |
|----|---|---|---|
| 尺寸 | 4×4 | 5×5 | **6×6** |
| 储量 | 8 000 | 16 000 | **26 000** |
| 单位上限 | — | +36 | **+54** |
| 生命 / 装甲 | — | — | **28 000 / 18** |
| 核心单位 | 流明领航者 | 堡垒领航者 | **城塞领航者** |

* 造价：`流明矿 9000 · 金矿 7000 · 棱镜合金 3000 · 共振核心 1200 · 共振碎晶 2400`，**全部是欧雷利亚资源**，在科技树里挂在堡垒核心下面，不免费解锁。
* 6×6 直接盖在 5×5 的堡垒核心上升级，和原版核心升级规则一致。实测放置：`build=AureliaCoreBuild storage=26000 team unit cap now 70 alive=true`。
* **城塞领航者**是全新机体（双涵道风扇 + 机首切割头的运载机，不是现有机体改色），可在欧雷利亚工厂打印：`70 s [流明矿420 金矿320 棱镜合金140 共振碎晶90]`，实测可打印、可飞、可开火。

---

## 四、核心分级采矿（需求 3）

Mindustry 的采矿判定在单位实体上：`canMine(item) = type.mineTier >= item.hardness`，`UnitType.mineItems` 实际上不会被 `MinerComp` 读取，所以"按矿种白名单"必须自写单位实体类（风险高）。本版改为用**矿物硬度 + 采矿等级 + 墙体权限**三者切分：

| 核心单位 | 采矿等级 | 墙体矿脉 | 可采 | 不可采 |
|---|---|---|---|---|
| 流明领航者 | 3 | 否 | 淤砂 / 流明矿 / 金矿 | 共振碎晶 |
| 堡垒领航者 | 3（原 5） | 否 | 同上，但**采矿速度 9.5 → 11**（三者最快） | 共振碎晶 |
| 城塞领航者 | 3 | **是** | 同上 + **墙体矿脉** | 共振碎晶 |

* **没有任何核心能采全部等级**：共振碎晶（硬度 4）对所有领航者关闭，永远需要四级以上钻机；墙体矿脉只有三级核心能挖。
* 堡垒领航者掉等级的同时提了采矿速度，是**换定位而不是单纯削弱**；钻机侧完全没动（最高 5 级，仍覆盖全部矿物）。
* 探针结论：`no core mines every grade, drills still cover them all`。

---

## 五、天文（拘留者）模块修复（需求 4）

### 5.1 根因

v7.4 / v7.5 合并时只导入了 `astro/content` 与 `astro/g3d`，**`astro/ui` 和 `astro/ai` 整个包被漏掉**。后果是：驾驶面板不显示、设置页没有分类、快捷键无效、跃迁选点（`AstroUI.warpAim`）消失、战术大脑（`TacticalBrain`）和指挥方块一起失踪——代码在，但没人去调用。

### 5.2 修复

* 补回 `src/astro/ui/`（`AstroUI` 411 行 + `PanelGeom` / `PanelLayout` / `PanelPrefs` / `PanelView`）与 `src/astro/ai/`（`TacticalBrain` 996 行 + `Micro` / `TacticalAI` / `Terrain` / `UnitProfile`）。
* 补回 `astro/content/TacticalCommander.java`（3×3 可配置指挥方块）。
* `MergedMods.init()` 现在显式调用 `astro.ui.AstroUI.init()`，并补齐 118 个 `astro.*` 文本键。
* **只改名称**，机制与数值一字未动：

| 对象 | 新名称 |
|---|---|
| 指挥方块 | 战术指挥核心 / Tactical Command Core |
| 驾驶面板 | 拘留者 / DETAINER |
| 设置分类 | 拘留者 · 驾驶面板 / Detainer pilot panel |
| 工厂 | 流明超级工厂 / Lumen Superfactory |

实测：`AstroUI=true PanelView=true TacticalBrain=true TacticalCommander=true warp targeting=true`，指挥方块放置后连续运行 240 tick 存活；拘留者数值仍为 `hp=38000 speed=0.66 armor=16`（未削弱）。

---

## 六、地下水提取器（需求 5）

`ground-well`（`AureliaForge.java`）：

| 项 | 8.2 | **8.3** |
|---|---|---|
| 抽水速率 | 7.00 / s | **17.50 / s（×2.5）** |
| 基础效率 | 0（干地不出水） | **0.4（任何地面都能抽，潮湿地面更快）** |
| 液体缓存 | 30 | 60 |

实测：`pumps 17.5/s (8.2 was 7.00/s, x2.5), base efficiency 0.4 -> runs on every floor`。

---

## 七、守护穹顶（需求 7）

3×3 的范围护盾，**不继承原版 `ForceProjector`**，整块逻辑与绘制都是自写的（`CycleParts.WardDome`）。

### 7.1 机制（全部有上限）

| 项 | 数值 |
|---|---|
| 半径 | 26 格 |
| 护盾容量 | 7 000 |
| 回充 | 9 / s（需供电） |
| 破裂冷却 | **14 s，期间完全不吸收，恢复时护盾为空** |
| 液体增强 | 极光冷却液：半径 +8 格，回充与重建 ×1.9 |
| 物品增强 | 共振碎晶：容量 +5 200，持续 22 s |

断电即落盾（但不算破裂，护盾值保留）；护盾被打空才算破裂，进入冷却。这样它永远不可能变成"无限墙"。

实测：`took 294 hostile shots in 147 ticks, swallowed 7000 damage, broke=true (cooldown 14 s), dead 120 ticks later=true, came back empty after 720 more ticks`；增强实测 `radius 26 -> 34 tiles`、`bank 7000 -> 12200`。

### 7.2 特效：全新、朴素、不刺眼

绘制在 `Layer.shields`，**没有使用原版护盾着色器，没有泛光（additive blending），没有全屏效果**：

* 双层细圆环（外环 1.5 px，内环 0.9 px 内缩 2.4 px），透明度随**剩余护盾比例**变化——盾快空时会肉眼可见地变薄；
* 一道缓慢环绕的"接缝"短弧，是整个穹顶唯一的运动；
* 12 根六边刻度短线，让它读起来是"结构"而不是一个圆；
* 命中处只画一个张开并淡出的小圆环；
* 破裂用新写的 `AureliaFx.wardBreak`：环裂成 12 段向外漂移熄灭；重建完成用 `wardRaise` 画一圈收拢的细环。

废料线的 `wasteVent`（灰色闷烟）与 `slurryDrip`（浆液滴环）同样是本版新写、同样不发光。

---

## 八、模型与资源

本版新增 9 个方块模型、1 套单位骨骼、4 个图标，全部**运行时程序化生成**，无手绘贴图：

| 模型 | 要点 |
|---|---|
| 废料分拣厂 3×3 | 开口破碎槽 + 横置带齿滚筒（唯一运动件，绕南北轴） |
| 碎屑熔炉 2×2 | 闭合熔锅 + 旋转盖环 + 出铁口 |
| 矿渣淋洗塔 3×3 | 洗涤塔柱 + 腰部喷淋环（绕竖轴） |
| 浆液结晶器 2×2 | 玻璃生长罐 + 罐内单晶（绕竖轴） |
| 共振再合成器 3×3 | 三只 120° 分布的料罐 + 中央反合成口与转环 |
| 棱晶再生炉 3×3 | 镜面浇铸床 + 沿导轨往复的龙门头（不脱轨） |
| 废料仓 3×3 | 灰黑大罐 + 液位窗 + 顶部排气扇 |
| 守护穹顶 3×3 | 三脚架 + 发射环 + 透镜（两件反向慢转） |
| 堡垒城核心 6×6 | 阶梯圆台 + 六个着陆位 + 中央主楼 + 信标与光环 |

烘焙审计（闭合性 / 内部采样 / 重叠 / 动画）：`problems: 0`。

---

## 九、构建与验收

| 关卡 | 结果 |
|------|------|
| `ModelCheck` | 97 个方块模型、19 个图标，**0 问题** |
| 模型烘焙 | `problems: 0`（97 个方块 + 23 个单位，闭合性 / 内部采样 / 动画帧全过） |
| `AssetCheck` | 440 张贴图，0 缺失 |
| `test_server.sh`（全量套件 + 本版新增 13 组 v8.3 探针） | **PASSED** |
| `cmdtest.sh` | **PASSED**（回归） |
| `logitest.sh` | **PASSED**（回归） |
| `maptest.sh` | **PASSED**（回归） |
| 深度审计 | 自有物品 22/22、液体 6/6 全部有消费者，**无死路** |

详细输出见 `docs/TEST-v8.3-beta.txt`。

---

## 十、文件清单（本版改动）

| 文件 | 说明 |
|------|------|
| `src/blackhole/AureliaCycle.java` | 新增：废料 3 物品 + 流明浆 + 6 台回收机 + 废料仓 + 守护穹顶 + 三级核心 + 城塞领航者 + 旧内容改造 |
| `src/blackhole/CycleParts.java` | 新增：`WasteSilo`（单品类缓冲仓）、`WardDome`（自写护盾逻辑与绘制） |
| `src/blackhole/models/CycleModels.java` | 新增：9 个方块 3D 模型 |
| `src/blackhole/models/CycleUnits.java` | 新增：城塞领航者骨骼与几何 |
| `src/blackhole/models/ItemModels.java` | 新增：废料 / 碎屑 / 渣粉 / 流明浆 4 个程序化图标 |
| `src/blackhole/AureliaFx.java` | 新增：`wardBreak`、`wardRaise`、`wasteVent`、`slurryDrip`（均不发光） |
| `src/blackhole/AureliaForge.java` | 地下水井 ×2.5 + 全地面可用 |
| `src/blackhole/AureliaMerge.java` | v8.3 科技树挂点（回收链挂窑、穹顶挂修复线、堡垒城挂堡垒核心） |
| `src/blackhole/BlackHoleMod.java` | 加载 v8.3 内容包 |
| `src/astro/ui/`、`src/astro/ai/`、`src/astro/content/TacticalCommander.java` | 恢复：面板 / 设置 / 快捷键 / 跃迁选点 / 战术大脑 / 指挥方块 |
| `src/blackhole/MergedMods.java` | 调用 `AstroUI.init()` |
| `assets/bundles/*.properties` | 中英文本（v8.3 全部新内容 + 天文改名 + 领航者描述更新） |
| `tools/bake/src/bake/Bake.java` | 新增城塞领航者的烘焙项 |
| `mod.hjson` / `build.sh` / 全部测试脚本 | 版本号 8.3-beta（**简介未改**） |
| `test_server.sh` | 新增 13 组 v8.3 探针（废料源 / 六步链 / 链深度 / 废料仓 / 核心采矿 / 领航者分工 / 三级核心 / 城塞领航者 / 核心放置 / 穹顶数值 / 穹顶实战 / 穹顶增强 / 水井 / 天文恢复 / 指挥方块） |
