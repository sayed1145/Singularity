# 奇点 · Singularity 8.5.2-beta 报告

Mindustry **v160.5** · 模组内部名称 `blackhole` · 一个 JAR 同时含桌面 class 与 Android DEX。

```
Singularity-8.5.2-beta.jar        ← 直接装（桌面 + 安卓同一个文件）
docs/REPORT-v8.5.2-beta.md        ← 本报告
docs/v852/shots/                  ← 截图（全部由实际 draw() 顶点录制后软件光栅化生成）
src/                              ← 完整源码
```

---

## 1. 这一版做了什么

| # | 项 | 内容 |
|---|---|---|
| 1 | 刷怪 | **空波次修好**：波次表改成"把能出场的单位按权重切进每一波"，不再用连乘总数（会溢出）；每波保底 1 只；金币权重单独按百分比换算 |
| 2 | 废料嬗变炉 | 新机器 4×4：吃 **废料 + 流明浆 + 电力**，按权重随机吐基础矿石（含金辉矿 / 流明矿 / 淤砂），一次最多两种，面板直接写概率 |
| 3 | 星髓石 | 不再只做以太合金：**建造花销**也能用（承重结构、护甲方块、部分炮塔与工厂）。地面矿石一个没加 |
| 4 | 流明液体桥 | 重做：圆阀体 + 法兰 + 视镜 + 阀轮 + 一段真正的三维连接管；和流明桥（平顶、带皮带）一眼分得开 |
| 5 | 液体中继塔 | 可以**手动指定连接**了（像星索缆道那样点选，最多 6 条，再点一次取消），清空选择回到自动互联；老存档照旧 |
| 6 | 锚钉炮 / 锚钉手 | 新的炮塔 + 新的单位：弹药带官方「不可吸收」标志，拘留者的爪子抓不走它；数值压在同级别常规兵器量级，不针对任何单位 |
| 7 | 晶格蠕者 | 上半身重做（平顶、零件不打架）；它自己的开火 / 硝烟特效缩到 **1/3** |
| 8 | 城塞领航者 | 模型重做；前面那个旋转件**转错轴**了（在绕 X 翻跟头），改成绕自身圆柱轴自转 |
| 9 | 性能 | 只优化性能，画面一动没动；设置里多了「奇点」专页，性能选项 + 原来的引力透镜开关都搬进去了 |
| 10 | 版本 | `mod.hjson` → `8.5.2-beta`；战役版本标签 `blackhole-campaign-v852`；README / 报告 / 双语描述同步 |

---

## 2. 逐项说明

### 2.1 刷怪：每一波都出兵

**现象**：有的波次一只都不出，感觉只有第一波有效。

**原因**（查官方 `mindustry/game/SpawnGroup.java`）：

```java
public int getSpawned(int wave){
    if(end < 0 || begin > wave) return 0;
    wave -= begin - 1;
    if(wave < 0) return 0;
    return type.spawned(wave - 1, spacing, end - begin, effect);   // <- (wave-begin) % spacing != 0 → 0
}
```

`WaveSpawner.spawnEnemies()` 用的是 `getSpawned(state.wave - 1)`。也就是说**只有落在 `begin + n*spacing` 上的波次才出兵**。
而 8.5.1 的 roster 里 `spacing` 是 2..7、`begin` 还带空档，加上数量是按组连乘出来的（大波次直接溢出被 clamp），
于是大量波次 `getSpawned() == 0` —— 就是玩家看到的"空波次"。

**改法**（`src/blackhole/AureliaWaves.java`）：不再让引擎去解释 `begin/end/spacing`，
而是**按当前波次自己切 roster**：

- `roster(wave)` 先算出这一波"能出场"的单位（有科技、威胁分不超过本波预算）；
- 把预算按权重摊到这些单位上，**权重切分**而不是数量连乘，不会再溢出；
- `ensureEveryWave()`：摊完如果这一波一个人都没分到，就把预算全部给权重最高的那一个 —— **保底每波 1 只**；
- 金币按官方公式 `f(x) = 0.6 * wave^(1 - 1/1.8) * x` 反解成"预算的百分比"，不是直接当数量用；
- 每个 `SpawnGroup` 写成 `begin = wave, end = wave, spacing = 1`，这样官方的取模永远命中。

### 2.2 废料嬗变炉（waste-transmuter，4×4）

按用户指定的机制实现：

- 输入 **只有废料** + **一种液体（流明浆）** + 电力；
- 产出：**除废料以外**的任意基础矿物，按权重随机，面板把取整后的百分比列出来；
- **一次最多两种**：`rolls` 夹在 1..2，每 `rolls` 次独立掷一次权重轮盘；
- 随机种子 `((long)id << 32) ^ tile.pos()`，同一台机器同一格结果稳定，重开游戏也一致（不是每帧乱跳）；
- 占满就停（官方 `GenericCrafter` 的 `shouldConsume` 行为），不会白吃材料。

### 2.3 星髓石用得更多

用户选的是"**加到更多配方里**"，而不是"多挖矿"。实现上是 `hasCost` / `addCost` / `addPlanCost`：
承重结构与护甲方块、若干炮塔与工厂的**建造花销**里加一份星髓石（护甲越厚用得越多）。

> 为什么不是"加矿石"：星髓石是高级矿物，往地面矿脉里再塞一种会把整张科技树的前置与产出撑坏，
> 而它本来就是"结构材料"的定位，走建造花销才符合设定。

### 2.4 流明液体桥重做

原来和「流明桥」几乎分不出来（都是平顶 + 皮带）。现在照着"它是一台**阀门**"重做：

- 圆形阀体坐在法兰盘上，四角螺栓；
- 北面一块能看液位的**视镜**；
- 四个方向各一个短管接头；
- 顶上**阀轮**只在液体流动时转；
- 两台桥之间由一段**真正的三维管子**连起来（不是贴图）。

### 2.5 液体中继塔可以手动指定连接

照「星索缆道」的做法：

- `LiquidRelay` 现在 `configurable / saveConfig / clearOnDoubleTap`；
- 用扳手点它、再点另一座中继塔 → 把这条链接**钉住**（最多 6 条）；再点一次取消；
- 距离外 / 已拆的链接在 `drawSelect` 里用 `Pal.remove` 画成红色；
- **清空选择就回到原来的自动互联**（`chosen.isEmpty()` 时走老代码路径），所以默认手感没变；
- 存档 `version() = 1`：`revision == 0`（老存档）完全不读新字段，老存档不受影响。

### 2.6 锚钉炮 / 锚钉手 —— 反拘留者

**机制只有一条**：弹药的官方标志位 `BulletType.absorbable = false`。

官方 `astro/content/DetainerType.java` 的 `fieldAndClaws()` 里判定是：

```java
if(!b.type.absorbable){ ...只抬起护盾...; return; }   // 不抓走、不转化成能量
```

拘留者的爪子**抓不走**这种弹丸，也就没法把它变成自己的能量或反射弹。

**平衡**（按用户"别太强、别针对、对其它单位照旧友好"的要求）：

- 不加伤害、不加射程、不加速度 —— 数值与同尺寸电力炮塔 / 同级地面单位一个量级；
- **不认识拘留者**：代码里没有任何针对它的分支，打任何单位都是同一发弹丸、同一个伤害；
- 拘留者的**圣盾（aegis）照常生效** —— 护盾是按"这类弹丸能不能伤到我"抬起来的，与能不能吸收无关，
  所以这套兵器打拘留者依然要先把盾打掉，不是免费击杀。

**新的开火方式**（`AnchorParts.SpearPattern`）：官方的两种基本花样是 `ShootAlternate`（左右分炮管，横向错开、同一刻出膛）
和 `ShootSpread`（扇形散布，角度错开）。这里是第三种 —— **纵向排队**：
第 i 发的出膛点比上一发沿炮管向后 `backoff` 个世界单位，并晚 `spacing` 刻击发，
于是同一轮在炮口排成一条纵向队列依次飞出，落点是一条紧密的线而不是一片扇面。纯几何，不改弹丸本身。

- 锚钉炮：3×3 电力炮塔，一轮 4 发；
- 锚钉手：履带式地面单位，一轮 3 发。

### 2.7 晶格蠕者 / 城塞领航者

- **晶格蠕者**：上半身重建 —— 平顶削边箱体 + 四角加强筋 + 南面一条灯带 + 队色标 + 下沉的装填鼓（只留口沿）
  + 低矮八棱炮塔座圈。武器与两侧履带一行没动。它自己的开火 / 硝烟特效（`OwnFx.muzzleSmall` /
  `muzzleSmokeSmall`）是原版 `muzzle` / `muzzleSmoke` 的**所有尺寸 ÷ 3**（含裁剪半径），形状与配色完全不变。
- **城塞领航者**：整机重建 —— 平顶机身、齐平座舱、背部货台、脊线灯带 + 队色标、左右外伸短梁 →
  十二边涵道 + 四叶旋翼（挂在 `FANL` / `FANR`，自转 **轴 2**）。
  **旋转 bug**：原来写的是 `rot(CUTTER, 0, d.b)`，轴 0 是俯仰 —— 于是滚筒在**绕 X 翻跟头**；
  滚筒是 `.rot(0, 90)` 建的，它的自身轴是局部 Y，也就是**轴 1**，改成 `rot(CUTTER, 1, d.b)`。

### 2.8 性能：只优化性能，画面一动没动

**验证方式（最硬的一条）**：把 v8.5.1 已发布的 JAR 里全部 837 张 sprite 解出来，
用 v8.5.2 的代码**重新烘焙全部 102 个模型 + 23 个单位**，然后逐文件比对：

```
assets/sprites/blocks/3d  : 0 个文件发生变化（只有 3 个新模型的 12 个新文件）
assets/sprites/units/3d   : 0 个文件发生变化（只有重做的 2 个单位 + 1 个新单位）
```

也就是说：**除了本次明确要求重做的模型，画面与 8.5.1 逐字节相同**，性能改动没有碰到任何一根顶点。

**设置页**（`src/blackhole/BHSettings.java`）：用官方的
`Vars.ui.settings.addCategory(title, Cons<SettingsTable>)` 开了「奇点 Singularity」专页：

| 键 | 默认 | 作用 |
|---|---|---|
| `bh-live-quads` | 14000 | 每帧实时几何预算（四边形数），用完的模型退回烘培静帧 |
| `bh-min-ppu` | 1.35 | 屏幕上一个世界单位不足这么多像素时，直接跳过实时零件（远缩放） |
| `bh-always-baked` | false | 最强省电档：所有模型一律画烘培静帧，不画任何实时零件 |
| `bh-lens` | true | 黑洞引力透镜 —— **从官方「图像」页搬过来的** |

页面底部有实时读数：`上一帧：实时 N / 预算 M 个四边形，K 个模型退回静帧`，
所以上面两个数字不是拍脑袋定的。三个性能选项是**真正接线的**（写进 `Budget.maxQuads` /
`Budget.minPpu` / `Budget.forceFallback`），默认值与 8.5.1 完全一致 —— 不动它们，画面就是原来的样子。

**合并重复代码**：只合并了**确实等价**的部分。八套 3D 渲染包（astro / voxel / rbmk / outpost / sixfold）
是**各自独立演化的**：`blackhole/g3d/Rig.java` 有 `Lift` / `Twist` 子骨架与 `stackSort`，
`astro/g3d/Rig.java` 另有 `Prop` 与不同的 `scale` 语义；`voxel/gfx` 的 `MeshUv` 用了
`trisA` / `trisB` / `arcMirror`，`outpost/gfx` 又是一套。**这些只能合并成"看起来一样但行为不同"的代码，
所以没有碰。** 真正做掉的重复是：把每个模型每帧新建 list 的写法统一到已有的共享分配器上
（`Budget` 那套 per-frame 预算），以及统一"实时零件画不动就退回静帧"的判断路径。

### 2.9 真机（Android）热修 —— 四个问题的根因

真机跑起来报的四个问题，逐条查了官方源码后修掉。都不是"看起来像"，而是能指到具体一行。

#### (1) 所有炮塔开火就崩：`AbstractMethodError: BulletHandler.shoot(float,float,float,float)`

官方 v160.5 的 `mindustry.entities.pattern.ShootPattern$BulletHandler` 长这样：

```java
public interface BulletHandler{
    public default  void shoot(float, float, float, float);            // 4 参：默认方法
    public abstract void shoot(float, float, float, float, Mover);     // 5 参：抽象方法
}
```

`ShootAlternate` / `ShootSpread` 这些官方弹道用的是 **4 参那个默认方法**。而 `MuzzlePattern`
（模组给炮塔挂枪口的那一层）原来是这么写的：

```java
base.shoot(totalShots, (x, y, rotation, delay, mover) -> { ... }, barrelIncrementer);   // lambda
```

lambda 只能实现**抽象**方法（5 参），4 参那个必须靠**继承来的默认方法**。桌面 JVM 上没问题，
但 d8 在 `min-api < 26` 时会把 lambda **脱糖成一个真实类**，这个类只带抽象方法、不带默认方法，
ART 于是找不到 4 参那一份 → `AbstractMethodError`。

**改法**（`blackhole/g3d/MuzzlePattern.java`）：不用 lambda，改成一个显式匿名类，**两个重载都实现**，
4 参直接转发到 5 参。这样运行时不需要继承任何默认方法，任何 API 等级、脱不脱糖都不会崩。

#### (2) 流明液体桥一连线就崩：`NullPointerException: ItemModule.take()`

官方 `ItemBridgeBuild.updateTransport()` 是**运物品**的版本，第一行就是 `Item item = items.take();`。
而官方 `LiquidBridge` 里 `hasItems = false` —— 也就是 `items` 是 `null`。
v8.5.2 新加的 `LumenLiquidBridgeBuild` 错继承成了 `ItemBridge.ItemBridgeBuild`（运物品），
于是每刻都在 `null` 上调 `take()`。

**改法**：继承 `LiquidBridge.LiquidBridgeBuild`（官方运液体的那个 build，它的
`updateTransport` 是 `moveLiquid(other, liquids.current())`）。

#### (3) 废料嬗变炉"材料齐全却不产出"

`shouldConsume()` 原来写的是 `items.total() < itemCapacity`。
问题是：**进来的废料和产出的矿石共用一个物品模块**。传送带会把废料一路塞到 `itemCapacity`，
此时 `total()` 到顶 → `shouldConsume()` 恒假 → `efficiency = 0` → 不再推进进度；
而废料**只能通过 `consume()` 出去**，`consume()` 又只在 `craft()` 里、`craft()` 又需要进度 ——
**死锁**：机器被废料填满后再也转不起来。

**改法**：只看**它自己能产出的那几种矿**有没有位置，废料永不参与判断。

#### (4) 晶格蠕者开火特效还是很大

`LiftedWeapon.bullet()` 里播的是 `bullet.shootEffect` / `bullet.smokeEffect`（`Weapon` 在 v160.5
根本没有 `shootEffect` 字段，所以只有 bullet 这一条来源），所以确实切到了 `OwnFx.muzzleSmall`。
但原版 `OwnFx.muzzle` 本身是给大炮用的（长 9 个世界单位、宽 5.2，差不多半截车身），
1/3 之后仍有 3 × 1.7，在这台短粗的迫击炮上依然显眼。

**改法**：把 `muzzleSmall` 再收一档（长 `1.6*fout+0.45`、宽 `1.2*fout`、核心 `0.36*fout`），
大约是原版大炮火光的 **1/4**；硝烟 `muzzleSmokeSmall` 同步收小；并显式把 `ejectEffect` 设为 `Fx.none`。

> 附带说明：`OwnFx.scrub()` 会把模组内容里的**原版** `Fx` 引用换成模组自带特效，
> 而 `OwnFx.muzzleSmall` 不是原版 `Fx`，不会在载入后被换回大号版本 —— 这一点也核对过了。


---

## 3. 验证

| 检查 | 结果 |
|---|---|
| 全量编译（JDK 17，`-source 17 -target 17`） | 通过，无警告升级、无错误 |
| `ModelCheck`（离线模型自检） | **102 个方块模型 + 20 个图标，0 problems** |
| 全量烘焙（102 模型 + 23 单位） | **problems: 0**，87.8 s |
| 与 v8.5.1 已发布 sprite 逐字节比对 | **只有本次要求重做的模型不同，其余 0 差异** |
| 无头服务器实际加载（`server-release.jar`） | 通过：`3D drawers installed on 48 blocks (102 models)`、`Aurelia tech tree: +147 nodes`、`0 objectives dropped`，无异常 |
| 热修后复核 | 全量重烘焙 `problems: 0`；与 v8.5.1 的 sprite 逐字节比对仍是**只有 2 个重做单位**不同；重新打包后 DEX 仍为 `035`、`check_dex.py` 判定合法；无头加载无异常 |
| 弹道回归`（离线）` | `MuzzlePattern` 包 `ShootAlternate` 后 4/4 发全部击发、炮口左右交替（`x=-3 / +3`），不再依赖默认方法继承 |
| DEX | `classes.dex` 由 d8 生成，桌安卓同一包 |
| 存档兼容 | 液体中继塔 revision 1，revision 0（老存档）走原路径 |

```
# 复现方式
javac -encoding UTF-8 -source 17 -target 17 -cp Mindustry.jar -d out $(find src -name '*.java')
SKIP_BAKE=1 bash build.sh          # dist/Singularity-8.5.2-beta.jar
bash tools/check_dex.py dist/Singularity-8.5.2-beta.jar
```

---

## 4. 截图

`docs/v852/shots/`：

| 文件 | 内容 |
|---|---|
| `blocks.png` | 全部非炮塔方块模型的总览（一张图，7 列） |
| `turrets.png` | 全部炮塔模型的总览 |
| `anchor-spike.png` / `anchor-spike-hd.png` | 新的锚钉炮 |
| `waste-transmuter.png` / `waste-transmuter-hd.png` | 新的废料嬗变炉 |
| `lumen-liquid-bridge.png` / `lumen-liquid-bridge-hd.png` | 重做后的流明液体桥 |
| `unit-spike-warden.png` | 新的锚钉手 |
| `unit-lattice-crawler.png` | 重做上半身后的晶格蠕者 |
| `unit-citadel-pilot.png` | 重做后的城塞领航者 |

说明：这些图是**实际 `draw()` 调用录制后软件光栅化**出来的（离线预览工具），
不是游戏内实机截图，也没有 FPS 含义。

---

## 5. 安装

1. 备份存档与战役数据；
2. **删掉旧的 8.5.1 JAR**，不要同时装两份（模组内部名称都是 `blackhole`，会冲突）；
3. 用游戏内模组菜单导入 `Singularity-8.5.2-beta.jar`，重启。

## 6. 这一版**没有**动的东西

- 没有改任何用户没要求改的数值、配方或方块；
- 没有给拘留者加任何针对性逻辑（那套兵器不认识它）；
- 没有把八套渲染包强行合并成一套（只有确实等价的部分才合并）；
- 没有新增地面矿石（星髓石走的是建造花销）；
- 没有把任何实时模型改成贴图回退 —— 烘培静帧只在超预算 / 远缩放时才用，且默认阈值与 8.5.1 相同。
