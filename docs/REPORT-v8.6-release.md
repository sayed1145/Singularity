# Singularity 8.6-release

Mindustry v160.5 · 完整 JAR（桌面 class + 安卓 DEX）

这一版的主角是「锚钉」这一对反拘留者兵器：**锚钉炮**（3×3 炮塔）与**锚钉手**（地面单位）。
锚钉手这一版几乎重做了一遍 —— 换了蛇式移动、加了三门炮、血量涨了五倍、造价提到 T5 档。

顺带查出一个**从 v8.5.2 就存在、一直没被发现的问题**：锚钉手的生产计划是坏的（见第 6 节）。

---

## 1. 锚钉炮（3×3 炮塔）

| 项 | 8.5.6 | **8.6-release** |
|---|---|---|
| 装填 `reload` | 84 刻 | **67.2 刻**（射速 ×1.25） |
| 弹丸伤害 | 34 | **153**（×4.5） |
| **建造成本** | lumenite 460 / aurite 320 / 棱晶玻璃 170 / 共振碎片 70 / 星髓石 8 | **完全不变** |
| 射程 / 血量 / 耗电 / 开火方式 | — | 一律不动 |

运行时实测（`docs/v86/audit.txt`）：

```
[anchor-spike] reload=67.2t   (was 84 -> x1.25 = 67.2)
               bullet damage=153.0   (was 34 -> x4.5 = 153.0)
               absorbable=false  reflectable=false
    build cost: lumenitex460, auritex320, prism-glassx170, resonance-shardx70, star-marrowx8
```

## 2. 锚钉手：三门炮

| 炮 | 8.5.6 | **8.6-release** | |
|---|---|---|---|
| 主炮 | 72 刻 / 27 伤害 / 三轮齐射 | **60 刻 / 162 伤害** | 射速 ×1.2，伤害 ×6 |
| 变种炮 A | — | **26.667 刻 / 58 伤害** | 新增，射速是主炮的 2.25 倍 |
| 变种炮 B | — | **26.667 刻 / 84 伤害** | 新增，射速是主炮的 2.25 倍 |

- 两门变种炮的伤害**各不相同**，都落在要求的 **42～97** 之间。
- 三门炮的弹药**一律保持 `absorbable = false`** —— 拘留者的爪子抓不走任何一发，
  也就没法把它们变成自己的能量或反射弹。拘留者的圣盾（aegis）照常生效，所以不是免费击杀。
- 两门变种炮除了伤害与射速，弹道（速度、射程、寿命、特效）与主炮完全一致 —— 换的只是弹头装药。

合计单体 DPS：**805.5**（8.5.6 是 67.5）。

```
    [0] reload=60.0t        damage=162.0  absorbable=false  shotsPerVolley=3
    [1] reload=26.666664t   damage=58.0   absorbable=false  shotsPerVolley=1
    [2] reload=26.666664t   damage=84.0   absorbable=false  shotsPerVolley=1
    combined single-target DPS = 805.5
```

## 3. 锚钉手：蛇式移动（去掉履带）

原来是一台 `TankUnit`（履带）。现在换成引擎官方的 **`CrawlUnit`** —— 这是 v160.5 里唯一真正的
「蛇」：它自带 `Crawlc` 分量，每帧推进两件事
（`mindustry.entities.comp.CrawlComp`）：

```java
segmentRot = Angles.moveToward(segmentRot, rotation, type.segmentRotSpeed * Time.delta);
segmentRot = Angles.clampRange(segmentRot, rotation, type.segmentMaxRot);
crawlTime += deltaLen();          // 累加走过的距离，不是时间
```

物理、寻路、踩到建筑上的减速（`crawlSlowdown`）、碾压伤害全部照旧由引擎负责，
这边只负责把躯干画出来。取值：

| 项 | 值 | 说明 |
|---|---|---|
| `segmentRotSpeed` | 2.6 | 躯干朝向追机头的速度 |
| `segmentMaxRot` | 55° | 躯干最多能弯到偏离机头多少度 |
| `crawlSlowdown` | 0.72 | 在密集建筑上有减速（这是官方 crawler 行为的一部分） |
| `crawlSlowdownFrac` | 0.6 | 脚下多大比例是实心才吃到满额减速 |
| `crushDamage` | 0 | 不加新行为 |

模型：一个楔形头节拖着六节装甲躯干 + 一条尖尾，**没有履带、没有轮子、没有腿**，
每节底下有一片腹鳞轮流蹬地。

### 建模参考（两处）

**(a) 引擎官方公式**（v160.5 `UnitType.drawCrawl`）：

```java
float trns = Mathf.sin(crawlTime + i * segmentPhase, segmentScl, segmentMag);
float rot  = Mathf.slerp(crawl.segmentRot(), unit.rotation, i / (float)(segments - 1));
```

两点照抄，**一点反过来**：

- 照抄：`Mathf.sin(a, b, c)` 是 `sin(a / b) * c`（反编译 arc 字节码核对过）。
- 照抄：这个 `trns` 是**沿身体轴**的偏移（官方用 `Angles.trnsx(rot, trns)`），
  所以是纵向的一松一紧，像毛虫那样蠕动 —— 这边把它加在「步长」上。
- **反过来**：官方的第 0 节拿 `segmentRot`、最后一节才拿 `unit.rotation`，
  也就是官方把**最后一节当成头**来画。这边的骨架第 0 节就是头，
  所以插值方向要倒过来（机头 = 当前航向，尾巴 = 滞后的 `segmentRot`）。
  **不倒过来的话蛇会倒着游。**

**(b) 真实蛇类的蜿蜒（serpentine locomotion）**：蛇前进靠的是从头传向尾的**横向行波**，
身体每一段侧向推地面，波往后走、蛇往前走。三个特征照着调的：

- **波从头传到尾** —— 相位写成 `cos(crawlTime / waveScl - i * wavePhase)`，
  同一个波峰随 `crawlTime` 增大要往更大的 `i` 走，也就是往尾巴走；
- **腹鳞依次蹬地** —— 蛇没有腿，靠一排腹鳞轮流抓地，所以每节底下的腹鳞按同一个行波错相拍动；
- **身体一段段微微抬起** —— 蛇不是贴着地面平移。

### 关键实现：沿脊柱按弧长积分

第一版是每节各算各的 `(横向偏移, -i * spacing)`。用 `SerpentProbe` 一量，**躯干会被甩开**：
尾巴那几节之间最大裂到 **8.5 个设计单位**（约 1 格），肉眼看得见缝。

改成**沿脊柱按弧长积分**：先算好每节的朝向，再从机头起一节一节往前走，
每步走 `spacing + 引擎纵向蠕动` 这么长、方向取相邻两节朝向的**角平分线**。
于是相邻两节中心的距离**恒等于步长**，躯干在任何相位、任何弯折下都不可能被拉开。

> 这里还踩了一个坑：角平分线一开始写成 `(ang[i] + ang[i+1]) / 2`。
> `slerp` 有可能返回 343.7 来表示 −16.3，于是 −13 和 343.7 平均成了 **+165°**，
> 方向整个反过来，身体往机头前面长。改成**取最短弧**（先 `wrap180` 两角之差再取半）才对。

`docs/v86/serpent-probe.txt` 是自检工具的完整输出 —— 它扫过 720 个波形相位 × 13 个弯折角 ×
4 个朝向，量相邻两节装甲板边缘的距离：

```
NaN offsets: 0
worst gap between consecutive plates: 0->1 -0.885 1->2 -0.489 2->3 -0.391
                                      3->4 -0.273 4->5 -0.126 5->6 -0.047
closest non-neighbour sections: 5.078 (sections 1 and 3)
VERDICT: body stays joined - every plate overlaps its neighbour at every phase and bend
```

全是负数 = 每块装甲板在**任何**相位和弯折下都跟邻居叠着（负数表示重叠，正数才是裂缝）。
另外还查了非相邻节之间会不会自己叠到自己：最近 **5.078**，远大于板宽，不会自交。

## 4. 血量

`2350 → 12000`，落在要求的 **8000 以上、15000 以下**。护甲 12、速度 0.78 都没动。

## 5. 建造成本：提到 T5 档

它现在有 12000 血、三门炮、805 DPS，按你说的「几乎已经成了 T4~T5 的单位」，
造价从 **58 秒提到 130 秒**，并且要 concordCore：

| | 8.5.6 | **8.5.7** |
|---|---|---|
| 建造时间 | 58 s | **130 s** |
| 成本 | lumenite 150 / aurite 120 / 棱晶玻璃 60 / 共振碎片 30 / 星髓石 4 | lumenite **640** / aurite **420** / 棱晶玻璃 **180** / 棱晶合金 **260** / 共振碎片 **150** / **concordCore 48** / 星髓石 **40** |

同档位参照：resonanceSiege 105 s、auroraHeavy 120 s、prismMonolith 150 s、bastionWarden 160 s。
仍然在「共振摇篮」里造（那是最早的单位线），但价格和耗时已经完全是 T5 的东西。

## 6. 顺带修掉的一个老问题：锚钉手的生产计划从 v8.5.2 起就是坏的

查造价的时候发现的，跟这次的改动无关，但既然碰上了就修掉。

`BlackHoleMod.loadContent()` 的加载顺序是：

```
AureliaFrontier.load()      <- 在这里 new UnitPlan(AnchorContent.spikeWarden, ...)
...
AnchorContent.load()        <- 到这一步 spikeWarden 才被造出来
```

也就是说 `AureliaFrontier` 建生产计划的时候，`AnchorContent.spikeWarden` **还是 null**，
于是那个 `UnitPlan` 的 `unit` 字段一直是 null。引擎里 `UnitFactory` 到处在用
`plan.unit.isBanned()` / `plan.unit.uiIcon` / `plan.unit.localizedName`，null 会直接炸 ——
**从 v8.5.2 起，锚钉手就从来没能真的从共振摇篮里造出来过。**

修法：把这个计划挪到 `AnchorContent.load()` 之后注册（新增
`AureliaFrontier.addSpikeWardenPlan()`，在 `BlackHoleMod` 里晚一步调用）。
修完实测：

```
plan unit=blackhole-mote           time=11.0s   matches=false
plan unit=blackhole-lattice-crawler time=42.0s  matches=false
plan unit=blackhole-spike-warden   time=130.0s  matches=true   <- 不再是 null
```

## 7. 查过的资料

| 来源 | 用来确认什么 |
|---|---|
| `mindustry/entities/comp/CrawlComp.java`（v160.5 源码） | `crawlTime` 累加的是**距离**不是时间；`segmentRot` 怎么追机头、怎么被 `segmentMaxRot` 夹住 |
| `mindustry/type/UnitType.java`（v160.5 源码，`drawCrawl`） | 官方分节公式；以及「官方把最后一节当头画」这个必须反过来的细节 |
| `arc.math.Mathf` 字节码反编译 | `Mathf.sin(a, b, c)` 确切是 `sin(a / b) * c`；`slerp` 可能返回 343.7 表示 −16.3 |
| `javap mindustry.gen.CrawlUnit` / `Crawlc` | `CrawlUnit.create()` 存在；`Crawlc` 提供 `crawlTime()` / `segmentRot()` |
| `javap mindustry.type.UnitType` | 全套 `segment*` / `crawl*` 字段与默认值；`segments` 只喂精灵加载与 `drawCrawl`（后者已被本模组的 3D 绘制整个取代），所以留 0 即可 |
| `javap mindustry.world.blocks.units.UnitFactory` | `UnitPlan.unit` 是普通字段，构造时传进去；`plan.unit` 在多处被解引用 —— 这就是第 6 节那个问题会炸的原因 |

## 8. 验证

| 检查项 | 结果 |
|---|---|
| 编译（JDK 17） | 0 错误 |
| 模型检查 ModelCheck | **102 个方块模型，20 个图标，0 问题** |
| 资源检查 AssetCheck | 461 个精灵，0 缺失 |
| 光照烘焙 Bake | **problems: 0** |
| 安卓 DEX 校验 | classes.dex 合法，**安卓可加载** |
| 无头服务器启动 | **1 mods loaded** / **Server loaded.**，0 错误 |
| 蛇式躯干自检 SerpentProbe | **每块装甲板在任何相位与弯折下都跟邻居叠着**，无 NaN，无自交 |
| 运行时数值审计 | 炮塔 67.2 刻 / 153 伤害 / 造价未变；单位 CrawlUnit / 12000 血 / 三门炮 162+58+84 / 全部 `absorbable=false`；计划 130 s 且不再为 null |

## 9. 安装

- 删掉或替换旧的 Singularity JAR，只保留 `Singularity-8.5.7-beta.jar`。
- 改的是炮塔/单位数值、锚钉手的骨架与移动方式、以及造价。老存档能开，
  但**已经在场上的旧锚钉手仍是履带版**（实体不会变形），新造出来的才是蛇式。
- 锚钉手的造价涨了很多 —— 它现在是 T5。

## 10. 实测数据存档

- `docs/v86/audit.txt` —— 运行时数值审计（炮塔、单位三门炮、生产计划）
- `docs/v86/serpent-probe.txt` —— 蛇式躯干连续性自检（含第一版被甩开 8.5 单位的失败记录说明）

复现：这两份数据都是临时挂一个审计入口（或独立自检工具）跑出来的。
审计入口跑完已完整移除，源码树与 jar 内均无残留（已核对）。
`SerpentProbe` 是常驻的自检工具，留在 `tools/preview/` 里，以后调波形参数可以直接重跑。
