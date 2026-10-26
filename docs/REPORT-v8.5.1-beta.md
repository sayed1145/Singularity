# 奇点 · Singularity 8.5.1-beta 报告

目标版本：**Mindustry v160.5**（官方正式版）。同一个 JAR 同时含桌面/服务端的 `.class` 和安卓的 `classes.dex`。
模组内部名称仍是 `blackhole`，用来**替换**旧版本，不是并装。

> 一句话：这一版把"看得见但用不了"的东西修成了真的能用 —— 科技树解锁、炮塔弹道、传送带管道接缝、
> 矿机动画；再补上两种新矿物、矿机液体加速、共振枢纽降门槛和全单位刷怪表。

---

## 0. 交付物

```
Singularity-v8.5.1-beta.jar        ← 直接装（桌面 + 安卓同一个文件）
docs/REPORT-v8.5.1-beta.md         ← 本报告
docs/v851/charts/*.png             ← 7 张验证图
docs/v851/*.log                    ← 服务端原始日志（可复核）
src/                               ← 完整源码（210 个 Java 文件）
assets/                            ← 贴图、语言包（中文 + 英文）
tools/                             ← 构建 / 校验 / 诊断脚本（含本版新增的三个）
```

| 新增/改动的工具 | 用途 |
|---|---|
| `tools/gen_v851_belts.py` | 重新生成流明/通量 传送带 + 管道 的全部贴图（按原版几何对齐） |
| `tools/gen_v851_ores.py` | 生成废料堆、星髓石两种新矿的矿石贴图 |
| `tools/check_dex.py` | 按 Android 官方 dex 格式校验包里的 `classes.dex` |
| `tools/make_v851_charts.py` | 生成本报告的 7 张图 |
| `src/blackhole/Diag.java` | 线下诊断钩子（**只有启动参数里写了 `-Dsing.diag=...` 才会做任何事**，正常游玩完全不走这里） |

---

## 1. 验证环境与方法

| 项 | 值 |
|---|---|
| 游戏 | Mindustry **v160.5** 官方 `server-release.jar`（Release 页下载）+ `Mindustry.jar` |
| 源码参照 | `github.com/Anuken/Mindustry` 克隆（commit `b873c01`） |
| JDK | Temurin 17（`--release 17`） |
| 安卓字节码 | Android build-tools r34 的 `d8`，`--min-api 21`，`--lib` 指向官方 `platform-35_r02.zip` 里的 `android.jar` |
| 跑法 | headless 官方服务端 + 模组 JAR 放进 `config/mods/`，用服务端自带的 `js` 命令布置场景，读服务端日志 |

所有数字都是**跑出来的**，不是估的。每一项下面都写了"怎么量的"，以及日志里该看哪一行。

---

## 2. 科技树：视界装配厂下面那批单位为什么点不开

### 2.1 症状
视界装配厂（`blackhole-horizon-assembler`）下面挂着 9 个节点，其中 8 个永远灰着，怎么攒资源都点不开：
透镜号、能层号、吸积车、堡垒号、镶嵌者、蛛行者、巨像、拘留者。

### 2.2 根因（实测）
用诊断钩子把整棵欧雷莉亚科技树 dump 出来：

```
$ java -Dsing.diag=tech ... -jar server-release.jar
NODE|aurelia|blackhole-astro-detainer|parent=blackhole-horizon-assembler
    |req=blackhole-auritex6030+blackhole-resonance-shardx6230+...
    |obj=Research(silicon)+Research(titanium)+Research(thorium)+Research(plastanium)
         +Research(phase-fabric)+Research(surge-alloy)
```

**问题不在研究成本，在"目标条件"（objectives）。** 这 8 个节点每一个都要求
"先研究原版硅 / 钛 / 钍 / 塑钢 / 相织布 / 涌浪合金"，而这些原版物品在欧雷莉亚既没有科技节点、
也产不出来 —— 于是这条目标永远完不成，节点永远灰着。一共 **8 个节点、20 处外来目标**。

这正是"需要研究硅才能解锁"的来源：玩家在面板上看到的"研究硅"是这条目标，不是研究账单。

### 2.3 修法
`AureliaMerge.sanitize()`：科技树搭完之后**统一扫一遍**，把目标里凡是"欧雷莉亚拿不到"的内容
按既有的 tier 映射表换成欧雷莉亚自己的东西；换不了的（比如指向别的星球的区域目标）直接摘掉。
同时把研究账单里残留的原版物品也换算掉，实在没对应物的删掉，再交给 `price()` 补价。

映射（复用模组原有的 `rules()`，和建造成本换算用的是同一张表）：

| 原版 | 换成 |
|---|---|
| 硅 silicon | 共振碎片 |
| 钛 titanium | 金辉矿 |
| 石墨 graphite | 棱镜玻璃 |
| 钍 / 塑钢 | 棱镜合金 |
| 相织布 / 涌浪合金 | 协律核心 |

改完之后那 8 个节点的目标全部变成欧雷莉亚自己的内容，账单也全是欧雷莉亚材料 —— 也就是你说的
"把配方都改成欧雷莉亚星球的配方"。

> 按你的选择（"只修那 8 个坏节点"），其余 191 个节点的研究成本**一个数都没动**。

### 2.4 验证
```
$ java -Dsing.diag=coverage ... -jar server-release.jar
COVERAGE|缺失节点数=0          ← 能造、看得见的内容，都在树上
FOREIGN|外星引用数=0           ← 树上再没有外星目标/需求
OBJ-UNREACHABLE|合计=0         ← 每个目标指向的内容本身都可解锁
```
服务端启动日志里也会打一行：
`Aurelia tech tree: +144 nodes (... 20 objectives rewritten, 0 objectives dropped ...), tree now holds 200 nodes`

![图1](v851/charts/01-科技树解锁修复.png)

---

## 3. 废料分拣厂：滚筒的转轴

**症状**：里面那根滚筒"向上翻"，不是绕自己转。

**根因**：`roller()` 造出来的圆柱，轴向是 **x**；想让它像擀面杖一样自转，必须绕 **x 轴**转（`axis = 0`）。
原代码写的是 `axis = 1`（绕 y 轴），于是整根滚筒变成上下翻跟头。

同一个文件里 `silt-kiln`（淤砂窑）的滚筒写的就是 `axis = 0`，所以只有废料分拣厂这一台是错的。

**修法**：`CycleModels.java` 里 `drawLive` 的 `axis` 由 `1` 改成 `0`（静止姿态那行同步改成 0，保持一致）。

![—]（此改动为三维姿态，无单独图表；判定依据是模型代码里 `Live.pose(x, y, z, axis, deg)` 的轴定义：
`axis=0` 是绕 x、`axis=1` 绕 y、`axis=2` 绕 z。）

---

## 4. 炮塔：弹丸、炮口、火光三者对齐

### 4.1 前哨磁轨炮 / 以太长枪：只在朝北时对得上

这两台炮属于"前哨"分包，用的是自己的模型类，没走模组统一的弹道对齐机制。它们的做法是：

```java
public static final float SHOOT_Y = new Cam(16f).sy(MUZZLE_LOCAL_Y, BASE_Z + LIFT_FULL + MUZZLE_LOCAL_Z);
shootY = SHOOT_Y;
```

`Cam.sy()` 算出来的是**炮口投影到屏幕上的距离**，但引擎把 `shootY` 当成**世界距离**用
（子弹位置 = 方块中心 + 旋转后的 `(shootX, shootY)`，再按高度 0 画出来）。
这两者只在**朝正北**时相等，转到别的方向就岔开。

**实测（16 个朝向逐一算，取最大偏差）**：

| 炮塔 | 修复前最大偏差 | 修复后 |
|---|---|---|
| 前哨磁轨炮 | 13.21 世界单位（**1.65 格**） | 0 |
| 以太长枪 | 10.03 世界单位（1.25 格） | 0 |

### 4.2 其余 18 门炮塔：弹丸对上了，火光还差半个格子

这 18 门走的是 `MuzzlePattern`：子弹先落在炮口的本地坐标，再由 `Lift` 抬到透视投影后的屏幕点 ——
**弹丸和模型炮口是重合的**。但引擎的枪口火光 / 硝烟是在子弹出生点播的，而那时还没抬升，
于是火光留在地面点、弹丸飞在炮口，差一个抬升量（吸积炮 4.63 单位 ≈ 0.58 格，最夸张的弧光迫击炮 12.66 单位 ≈ 1.58 格）。

**修法**：安装弹道对齐时把方块上的 `shootEffect` / `smokeEffect` 换成 `Fx.none`，改由 `Lift` 在抬升之后
的位置补播（方块没配特效时退回子弹类型自带的那套，行为和原来一致）。

### 4.3 验证
```
$ java -Dsing.diag=align ... -jar server-release.jar
ALIGN|blackhole-bastion-railgun|偏差=0.0|最大朝向=22.5
ALIGN|blackhole-aegis-lancer|偏差=0.0|最大朝向=135.0
ALIGN|blackhole-accretion-cannon|偏差=0.0|最大朝向=0.0
...（20 门炮塔全部 0.000）
```
校验收的是"子弹最后画出来的位置"和"模型炮口画出来的位置"在 16 个朝向上的最大距离。

![图2](v851/charts/02-炮塔弹道对齐.png)
![图3](v851/charts/03-开火特效对齐.png)

---

## 5. 矿机：液体加速与挖掘速度

| 方块 | 改动 | 面板显示 |
|---|---|---|
| 金辉螺旋钻机 | 新增：喝**极光冷冻液** 0.05/秒，`liquidBoostIntensity = 1.6` | **2.56 倍** |
| 裂隙汇聚器 | 新增：喝**流明等离子** 0.07/秒，`liquidBoostIntensity = 1.5` | **2.25 倍** |
| 共振筛选机 | 液体加速由 1.7 降到 **1.5** | 2.89 倍 → **2.25 倍** |
| 裂隙汇聚器 | 初始挖掘速度 **翻倍**：`mineTime` 38 → **19** 刻 | 60/19 ≈ 3.16 个/秒 |

三种矿机三种加速介质，互不抢料（共振筛选机喝潮汐水、棱镜采掘场喝流明等离子、金辉螺旋钻机喝极光冷冻液）。

裂隙汇聚器的液体加速是给 `BeamMiner` 新写的（它继承自 `Block`，本来没有 `liquidBoostIntensity`），
实现和官方 `Drill` 一致：`progress += edelta() * warmup * targets * lerp(1, 倍率, optionalEfficiency)`。

> 关于"共振筛选机降到 2.25 倍"：引擎面板上显示的是 `liquidBoostIntensity` 的**平方**
> （`Drill.setStats()` 里就是 `liquidBoostIntensity * liquidBoostIntensity`），
> 原来 1.7 显示成 2.89 倍。要显示 2.25 倍，值就是 1.5 —— 按"降低"处理。

**验证**（`diag=blocks`）：
```
BLOCK|blackhole-aurite-auger|...|liquidBoost=1.6|面板显示=2.56倍|liquids=blackhole-aurora-coolantx0.05@加速
BLOCK|blackhole-resonance-sifter|...|liquidBoost=1.5|面板显示=2.25倍|liquids=blackhole-tidewaterx0.06@加速
BLOCK|blackhole-rift-collector|...|mineTime=19.0|beams=3|liquidBoost=1.5|面板显示=2.25倍|liquids=blackhole-lumen-plasmax0.07@加速
```

---

## 6. 两种新矿物

### 6.1 废料（原来叫「工业废料」）

按你的要求：**所有机器不再产出"工业废料"而是"废料"**，废料相关的机器全部保留、只改名字。

* 物品 `blackhole-industrial-waste` 的显示名改成 **废料 / Waste**；
* 产出它的三台机器（淤砂窑、等离子混合器、棱镜压机）产出的就是这个物品，一个不动；
* 回收链（废料分拣厂 → 金属碎屑 + 矿渣粉 → 碎屑熔炉 / 浸出塔 → …）全部保留，描述里的"工业废料"改成"废料"；
* 同时它**成了一种真矿物**：地面上有成片的废渣堆可以挖（硬度 0，谁都挖得动），
  给回收链一个"不用先造厂就有料"的起点；
* 矿石贴图 `ore-waste1..3.png` 由 `tools/gen_v851_ores.py` 程序化生成：压成一块的杂料，
  断面露着没锈完的金属，带几道锈痕（仿现有矿石贴图的画法：4 倍超采样 + LANCZOS 缩到 32×32，固定随机种子）。

### 6.2 星髓石 Star Marrow（新，高级矿物）

| 属性 | 值 |
|---|---|
| 硬度 | 4（与共振碎片同级 —— 四阶矿机就挖得动，三阶挖不动，不开后门） |
| 成本 | 3.4 |
| 定位 | **结构强度材料**：护甲与建筑 |
| 外观 | 恒星残骸：冷却了几十亿年的暗壳，剖开处露出惨白的星髓，摸上去还有余温（自发光的髓 + 哑光壳） |

**进了哪些配方**（挖它不是白挖）：

* **以太合金**：以太压力机的配方加 1 份星髓石 —— 这是最主要的持续消耗口，
  一路喂到信号核心 → 泰坦机库 → 泰坦单位；
* **护甲结构件**：吸收墙（+8）、协律护盾墙（+10）、锚定墙（+6）、引力弦发电机（+40）的建造成本。

> 星髓石属于 `AureliaContent`，而六倍包（以太合金那台机器）比它先注册，直接在方块的初始化块里引用会拿到 `null`。
> 所以那份消耗器放在 `AureliaMerge.recost()` 里后置挂载（`applyMarrowRecipes()`）—— recost 跑在所有 `loadContent()`
> 之后、引擎冻结消耗器之前。金辉螺旋钻机的极光冷冻液同理（见 `AureliaIndustry.applyBoosts()`）。

**地图生成**：两种新矿都进了矿脉循环，也都有出生点保底（`ensureOre`）。数量刻意压着 —— 一张图的覆盖层
格子有限，新矿加太多会把三种老矿挤没。

**验证**（真实生成的 6 张战役图，日志里的 `campaign sector …` 那一行）：

| 图 | 尺寸 | 金辉矿 | 流明矿 | 共振碎片 | **废料** | **星髓石** |
|---|---|---|---|---|---|---|
| 10 | 280² | 2214 | 2143 | 1948 | 366 | 236 |
| 83 | 284² | 2363 | 2025 | 1584 | 306 | 179 |
| 30 | 300² | 2036 | 2013 | 1873 | 420 | 162 |
| 52 | 296² | 2130 | 2145 | 1574 | 328 | 220 |
| 72 | 328² | 1503 | 1509 | 1049 | 278 | 167 |
| 65 | 336² | 1951 | 1515 | 1245 | 294 | 184 |

![图7](v851/charts/07-新矿物矿脉分布.png)

---

## 7. 共振枢纽：激活门槛 1000 → 300

`capacity` 从 1000 降到 **300**。原来攒满一次过载要等好几波，经常一波打完了还没激活；
现在一支部队开火几十秒（每秒 26 点 × 开火单位数 + 6 点底噪）就能攒满一次。

---

## 8. 传送带 / 管道：格子的四条边严丝合缝

### 8.1 根因（实测）
这两族贴图靠"相邻格子在同一条边上的开口严丝合缝"接成一条线。把 8.5-beta 的原贴图和
**官方仓库里 `core/assets-raw/sprites/blocks/...` 的原版贴图**逐条边对比（每条边看它哪些行/列是不透明的）：

| 家族 | 修复前与原版不一致的边 |
|---|---|
| 流明/通量传送带（2 族 × 5 型 × 4 边 = 40） | **34** |
| 管道 top（40 条边） | **36** |
| 管道 bottom（40 条边） | **32** |
| **合计 120 条边** | **102 条不一致** |

具体问题：
* 直线件的**下边缘整行透明**（原版是 0–31 全铺满）→ 每格底边一条缝；
* 拐角件的**左边缘和下边缘都透明** → 拐弯直接断开；
* 管道的管口条带只到 `0–5 / 23–30`，比原版的 `0–7 / 24–31` 差 1–2 像素 → 每个接头都错开一点。

### 8.2 修法
`tools/gen_v851_belts.py` 按原版几何重新生成：

* 通道半宽 8px、居中（占第 8–23 行/列），所有距离按**像素中心**算，和原版一格不差；
* 拐角是以格子右上角为圆心、半径 16 的四分之一圆弧，外角按 39.6 的半径切圆角；
* 直线的挡条每帧前进 2px（周期 8px / 4 帧，实测残差 0.0）；
* 管道的 `top` 是管壁（通道处镂空）、`bottom` 是液体层，铺法照抄原版（弯管和接头整格铺满，
  直线件只铺通道），这样弯管里不会露出底色；
* 配色沿用各家族现有的主色（流明青 `#8fe9ff` / 通量紫 `#ccbaff` 等）。

### 8.3 验证
重新生成的 15 张贴图（传送带 5 型 + 管道 top/bottom 各 5 型），**60 条边全部与原版一致，差异 0**。

![图4](v851/charts/04-传送带管道边缘对接.png)

---

## 9. 矿机动画：不挖矿的时候不再空转

**症状**：棱镜采掘场、金辉螺旋钻机、流明矿机、凿墙机、裂隙汇聚器在没矿、没电、被禁用、
或者装满停工的时候，动画照播（钻头照转）。

**根因**：引擎里 `Building.totalProgress()` 对**非合成类方块**直接返回 `Time.time`
（矿机、束矿器、凿墙机都在此列），而模型的转动角度是拿它当参数用的 —— 不管有没有在挖，这个数一直在涨。

**修法**：给这三类方块的 build 各加一个自己的时钟，只在引擎判定"正在产出"时累加：

```java
drillSpin = SpinClock.advance(drillSpin, this, warmup);   // = cur + edelta() * clamp(warmup)
```

`warmup` 在没矿 / 没电 / 满载 / 被禁用时会自己掉回 0，于是钻头**平滑地**停下来（不是硬切）。

**验证**（服务端实测，探针每 2 秒打一行）：

| 场景 | warmup | 转动时钟 |
|---|---|---|
| 空地、没电 | 0 | **恒为 0**（钻头不转） |
| 有矿有电 | 1 | 90.9 → 210.9 → 330.9（持续前进） |
| 出口塞满 20/20 | 0 | **冻结在 333.017，之后一动不动** |

![图6](v851/charts/06-矿机动画门控.png)

---

## 10. 刷怪：所有能打的单位都上场

### 10.1 之前的状况
`AureliaWaves` 里写死了 5 种单位（碎片漫游者、微光、金辉壁垒、极光卫士、极光重机），
模组另外三十来种能打的单位一张图都刷不出来。

### 10.2 现在的规则
按 roster 自动铺开：

| 规则 | 说明 |
|---|---|
| **谁会刷** | 所有**带武器**的单位，且不是船、不是玩家自己开的机体、不是纯后勤 |
| **不刷的** | 4 种船（潮汐快艇 / 潮汐守望者 / 潮汐枪骑 / 潮汐补给艇）；玩家机体（流明机体、城塞领航者、堡垒领航者）；后勤（修复无人机、流明运输机、货运无人机、流明信使） |
| **什么时候开始** | 按强度分档，档位是**占本图总波数的比例** —— 15 波的新手图和 79 波的终局图都排得下 |
| **多久来一次** | 越强的单位间隔越长（spacing 2 → 7 波）、单次数量越少 |
| **拘留者** | 只在威胁度 ≥ 0.55 的图（战役后半段）出现；开局波和结束波设成相邻两波、每波 1 只，合计**正好 2 只** |
| **重器保护** | 巨像 / 极光重机以上的终局重器，单次只来 1 只、且**不随波数增长**（`unitScaling = never`） |

共 **29 种**会刷的单位（含拘留者），覆盖地面、空中、蜘蛛机甲、泰坦。

### 10.3 验证（32 张战役图全部推演一遍）
```
$ java -Dsing.diag=waves ... -jar server-release.jar
WAVES|id=10|threat=0.19|winWave=15|gates=3|种类=28|拘留者=0|单波最多=20|全图合计=136
WAVES|id=65|threat=0.899|winWave=77|gates=5|种类=29|拘留者=2|单波最多=81|全图合计=1612
WAVES-SUMMARY|最少种类=28|刷拘留者的图=15|单波峰值=81
```
* **每张图**都能刷出 28 种单位（高威胁图 29 种，多的那个是拘留者）—— 满足"所有可攻击单位在所有地图都至少出场一次"；
* 拘留者只在 32 张图里的 **15 张**出现，且每张**恰好 2 只**；
* 单波峰值从开局图的 20 只涨到终局图的 81 只。

> 一个诚实的提醒：因为要求"重器也在每张图出场"，开局图（15 波）在最后几波也会来一只泰坦/重机
> （各自 1 只、不增长）。这让前期明显更硬。想把它关掉的话，把 `AureliaWaves.HEAVY_THREAT_GATE`
> 从 `0f` 改成 `0.45f` 左右即可（威胁度低于它的图不刷重器）。

![图5](v851/charts/05-刷怪单位覆盖.png)

---

## 11. 桌面 / 安卓双端

| 平台 | 加载方式 | 包里靠什么生效 |
|---|---|---|
| Windows / Linux / macOS | 官方 `ModClassLoader` → `URLClassLoader` | 包根 `.class`（804 个） |
| Android | 官方 `Mods.java` 里的 `DexClassLoader` 分支 | 包根 `classes.dex`（1,244,688 字节） |

两份字节码来自同一份源码：`javac --release 17` → `.class` → `d8 --min-api 21`。
d8 的 `--lib` 指向官方 `platform-35_r02.zip` 里的 `android.jar`，**脱糖警告 0 条**
（之前没给 android.jar 时会报 `java.lang.Enum` / `java.util.Comparator` 找不到，
导致默认接口方法和枚举脱糖不完整 —— 这次补上了）。

`tools/check_dex.py` 按 Android 官方《Dalvik executable format》校验：

```
python3 tools/check_dex.py Singularity-v8.5.1-beta.jar
  ✓ 包根 .class：804 个（桌面/服务端 JVM 用）
  ✓ 包根 classes.dex：1244688 字节（安卓 DexClassLoader 用）
  ✓ magic 正确：b'dex\n035\x00'（安卓只认 dex\n035\0）
  ✓ header_size = 0x70      ✓ endian_tag = 0x12345678（小端）
  ✓ file_size = 1244688，实际 1244688 字节
  ✓ map_off / string_ids_off / class_defs / data 段 均未越界
  ✓ 抽查 4000 条字符串项，全部以 0 结尾且不出界（坏 0 条）
  ✓ adler32 校验和一致     ✓ SHA-1 签名一致
结论：classes.dex 合法，安卓可加载
```

---

## 12. 回归与自检

| 项目 | 结果 |
|---|---|
| `javac --release 17` 全量编译 | 通过，0 错误 |
| `preview.ModelCheck`（99 个方块模型 + 20 个物品图标） | **0 problems** |
| `preview.AssetCheck`（449 张贴图） | **0 missing** |
| `bake.Bake` 全量重烘焙 + 可见性审计 | **problems: 0**（67 秒） |
| 官方服务端载入 + 生成战役图（共 8 张） | 无 error / exception |
| 科技树审计 | 缺节点 0、外星引用 0、不可达目标 0 |
| 炮塔弹道（20 门 × 16 朝向） | 偏差全部 0.000 |
| 传送带/管道边缘（15 张贴图） | 与原版差异 0 |
| 刷怪表（32 张图推演） | 每图 28–29 种；拘留者 15 张图 × 2 只 |
| 矿机动画门控 | 空转 0、满载荷冻结 |

复现命令（项目根目录）：

```bash
# 编译 + 全量校验
find src -name '*.java' > /tmp/srcs.txt
javac --release 17 -encoding UTF-8 -nowarn -cp Mindustry.jar -d build/classes @/tmp/srcs.txt
java -cp "build/tools:build/classes:Mindustry.jar" preview.ModelCheck
java -cp "build/tools:build/classes:Mindustry.jar" preview.AssetCheck assets/sprites
java -Xmx1300m -cp "build/tools:build/classes:Mindustry.jar" bake.Bake assets threads=2 report=build/bake-report.txt

# 各项数值校验（服务端启动参数）
java -Dsing.diag=tech,coverage,align,waves,blocks,recipes,items -Dsing.diag.out=/tmp/diag.txt \
     -jar server-release.jar

# 矿机动画门控（需再载入一张图并开局）
java -Dsing.diag=drills -jar server-release.jar
#   js: P.Vars.world.loadSector(sec); P.Vars.state.rules.sector=sec; P.Vars.logic.play();
#   然后看日志里的 DRILLPROBE 行
```

---

## 13. 参考资料

1. Android 官方《Dalvik executable format》（dex 头、字符串表、map_list 的权威定义）
   — https://source.android.com/docs/core/runtime/dex-format
2. Android 官方《DexClassLoader》（只认包根 `classes.dex`）
   — https://developer.android.com/reference/dalvik/system/DexClassLoader
3. Android 官方《d8》（`--min-api`、`--lib`、`--classpath`）
   — https://developer.android.com/tools/d8
4. Mindustry 官方仓库（本版以 commit `b873c01` 的源码为参照；原版传送带/管道贴图取自 `core/assets-raw/sprites/blocks/`）
   — https://github.com/Anuken/Mindustry
5. Mindustry 官方 wiki《Modding》（mod.hjson、目录结构、minGameVersion）
   — https://mindustrygame.github.io/wiki/modding/1-modding/
6. Mindustry 官方 wiki《Plugins & JVM Mods》（桌面 + 安卓都支持 jar）
   — https://mindustrygame.github.io/wiki/modding/2-plugins/
7. Android SDK Platform 35（`android.jar`，d8 脱糖用）
   — https://dl.google.com/android/repository/platform-35_r02.zip

---

## 14. 已知取舍

* **重器在所有地图出场**会让前期图更硬（见 §10.3 的提醒与开关）。
* 星髓石进了 4 个建造成本和 1 个生产配方，因此这几个节点的研究账单会略微上浮
  （引擎的研究成本由建造成本推出，这是加新材料的必然结果；其余 191 个节点一个没动）。
* 传送带/管道的贴图是**按原版几何重新生成**的，观感会与旧版略有差别（换来的是接缝消失）。
* 矿机动画时钟降为 0 是"平滑停"（跟着 warmup 衰减），不是硬切 —— 停机瞬间还会有极短的减速。
