# Singularity 8.5.6-beta

Mindustry v160.5 · 完整 JAR（桌面 class + 安卓 DEX）

这一版做了五件事：类星体喷流的循环音效及时止损、星髓石弹药收敛到两门炮（事件视界炮换重弹 320）、
奇点核心弹伤害与范围伤害上调、地图全面放大到 494×494 ～ 600×600、刷怪强度增长 1.25 且数量 ×1.2。

每一项都附**实测数据**（无头服务器里跑出来的，不是照代码估的）。复现方式见文末。

---

## 1. 类星体喷流：循环音效及时止损

症状：光束早灭了、甚至炮塔都被拆了，循环音效还在响。

### 根因（对着 v160.5 源码查出来的）

引擎这边是这么接的：

```java
// ContinuousTurretBuild
public boolean shouldActiveSound(){ return bullets.any(); }
public float  activeSoundVolume(){ return 1f; }

// TurretBuild.updateTile()
soundLoop.update(x, y, shouldActiveSound(), activeSoundVolume());
```

三个坑叠在一起：

1. **判定只看"还有没有光束子弹"。** `ContinuousTurretBuild.updateBullet` 每刻把
   `bullet.time` 钉在 `lifetime × optimalLifeFract × warmup` 上，只有在 `isShooting` 变 false
   之后才放手让它自然到期 —— `quasarBeam` 的 lifetime 是 80，于是目标没了之后子弹还要再活几十刻，
   声音就跟着再响几十刻。
2. **`SoundLoop` 自己还要再拖 20 刻。** `fadeSpeed` 是写死的 `0.05`，`play == false` 之后
   要用 `1 / 0.05 = 20` 刻（0.33 秒）才真的 `Core.audio.stop()`。
3. **`SoundLoop` 只在 `updateTile()` 里被驱动。** 实体一旦不再更新，就再没有人去停它。

### 改法（`src/blackhole/ContinuousTurret3D.java`）

把"真的在开火"加进判定条件，并且一不满足就**立刻** `stop()`，不等那 20 刻的淡出：

```java
@Override public boolean shouldActiveSound(){
    return super.shouldActiveSound() && isShooting && !dead && enabled;
}
@Override public void updateTile(){
    super.updateTile();                                  //里面已跑过 bullets.removeAll(...)
    if(soundLoop != null && !shouldActiveSound()) soundLoop.stop();
}
@Override public void remove(){       super.remove();       if(soundLoop != null) soundLoop.stop(); }
@Override public void onDestroyed(){  super.onDestroyed();  if(soundLoop != null) soundLoop.stop(); }
```

伤害、射程、耗电、射速、光束长度**一个都没动**，改的只有音效什么时候停。

---

## 2. 星髓石弹药：只留两门炮，事件视界炮换重弹

| | 8.5.5 | **8.5.6** |
|---|---|---|
| 能装星髓石的炮塔 | 吸积炮、参考系拖曳炮、视界、shardVolley（4 门） | **吸积炮、事件视界炮（2 门）** |
| 吸积炮的星髓弹 | 165 直伤 + 90 范围 | **不变（165 + 90）** |
| 事件视界炮的星髓弹 | 165（同一发） | **320 直伤 + 190 范围** |

事件视界炮拿的是新的 `marrowLanceHeavy`（**320**，落在你给的 300～350 区间里）。
它和 165 那发是同一条弹道 —— 速度、射程、每份矿 4 发、特效、灼印完全一致，换的只是弹头装药。

事件视界炮本来就是"一发定音"的炮（冷却 9 秒、一次吃 2 份弹药、射程 400），让它打 165 太浪费。

运行时日志（无头服务器实测）：

```
[blackhole/marrow] star marrow registered as turret ammo on 2/2 turrets (accretion-cannon 165+90, event-horizon 320+190)
```

---

## 3. 奇点核心弹：伤害 120 → 150，范围伤害 180 → 210

`BHBullets.collapsarShell`（事件视界炮 `BHItems.singularityCore` 那一发）：

| 项 | 8.5.5 | **8.5.6** |
|---|---|---|
| 直伤 | 120 | **150** |
| 范围伤害 | 180 | **210** |
| 范围半径 | 60 像素 | 不变 |
| 速度 / 寿命 / 弹道 / 黑洞机制 / 特效 | — | **一律不动** |

已确认 `collapsarShell` 只被事件视界炮引用（`BHBlocks.java:315`），所以这次上调**不会波及其他炮塔**。

---

## 4. 地图全面放大：494×494 ～ 600×600

| 项 | 8.5.5 | **8.5.6** |
|---|---|---|
| 战役图尺寸 | 272 ～ 348 | **494 ～ 600** |
| 非战役扇区（默认） | 引擎默认（比战役图小一截） | **494** |
| 格子数（最小图） | 272² = 73,984 | 494² = **244,036**（3.3 倍） |
| 格子数（最大图） | 348² = 121,104 | 600² = **360,000**（3.0 倍） |

### 配套改动：矿脉数量按面积缩放

这几个目标数字（1150 / 520 / 380 / 300 / 140）当年是按约 310×310 的图调的。
地图放大之后如果不跟着放大，矿会稀到没法开局 —— 所以按面积比例缩放：

```java
private static final float oreReferenceArea = 310f * 310f;
private static float areaScale(int w, int h){ return (w * h) / oreReferenceArea; }
```

矿脉目标数、`ensureOre(...)` 保底数、`ensureWater(...)` 保底数、矿脉尝试次数上限全部乘 `areaScale`。

### 全 32 张战役图实测（`docs/v856/map-audit.txt`）

- **尺寸全部落在 494 ～ 600**（实测最小 494，最大 600）
- **缺矿的图：0 / 32** —— 五种矿（金砂石 / 流明石 / 共振石 / 废料 / 星髓石）每张图都有
- **没有刷怪门的图：0 / 32**
- 单张生成耗时 1.7 ～ 3.5 秒
- 单张 594×594 在 `-Xmx320m` 下也能生成成功（游戏一次只生成一张图；
  审计进程连跑 32 张才被 OOM 杀掉，那是审计脚本自己的问题，不是模组的）

---

## 5. 刷怪：强度增长 ×1.25（新手图不变），数量 ×1.2

### 5.1 强度增长 1.25

引擎里 `unitScaling` 是"每多刷 1 只需要的波数"，**越小涨得越快**。
所以"强度增长 1.25 倍" = 把 `unitScaling` 乘到 `1 / 1.25 = 0.8`，
而且是**随威胁度线性介入**的：

```java
static float growthScale(float d){
    return 1f / Mathf.lerp(1f, GROWTH_BOOST, Mathf.clamp(d));   //GROWTH_BOOST = 1.25f
}
// d = 0（新手图）-> 乘 1.0，一点不变
// d = 1（终局图）-> 乘 0.8，涨速 1.25 倍
```

这就是"新手图强度不变，只是说强度增长 1.25" —— **变的是坡度，不是起点**。

### 5.2 数量 ×1.2

`COUNT_BOOST = 1.2f`，同时作用在：

- 每组上限 `g.max`
- 一次来几只 `g.unitAmount`
- `AureliaCampaign.balance()` 里那个 `cap`（否则会把刚抬上去的数量又压回来）

### 5.3 实测（`docs/v856/wave-audit.txt`）

`SpawnGroup.getSpawned()` 按 v160.5 反编译后的原样复刻：

```java
if(spacing == 0) spacing = 1;
if(wave < begin || wave > end || (wave - begin) % spacing != 0) return 0;
return Math.min(unitAmount + (int)((wave - begin) / spacing / unitScaling), max);
```

| 威胁度 | 波数 | 8.5.5 平均/峰值 | **8.5.6 平均/峰值** | 倍数 |
|---|---|---|---|---|
| 0.13 | 16 | 5.1 / 11 | 5.1 / 11 | **1.000** |
| 0.20 | 18 | 5.2 / 11 | 5.3 / 11 | 1.032 |
| 0.30 | 22 | 5.8 / 12 | 5.8 / 12 | 1.000 |
| 0.34 | 24 | 5.9 / 11 | 6.0 / 12 | 1.021 |
| 0.45 | 32 | 6.8 / 15 | 7.1 / 17 | 1.041 |
| 0.60 | 45 | 8.7 / 22 | 9.6 / 23 | 1.097 |
| 0.75 | 60 | 10.9 / 25 | 12.8 / 32 | 1.169 |
| 0.92 | 79 | 14.3 / 45 | **17.1 / 54** | **1.193** |

新手图 **1.000 倍（完全不变）**，终局图 **1.193 倍 ≈ 1.2**，中间平滑过渡。

### 5.4 所有怪仍然全部刷得出来

- roster 30 个档位，**31 个不同单位**在威胁档里出现过（31 > 30 是因为拘留者 Detainer
  在 threat ≥ 0.55 时作为独立 boss 组额外加入）→ **没有单位被漏掉**
- **空波 0**：8 个威胁档共 296 波，每一波都刷得出东西（见 `docs/v856/wave-audit.txt`）

---

## 6. 查过的资料

| 来源 | 用来确认什么 |
|---|---|
| `ContinuousTurret.java` / `Turret.java` / `SoundLoop.java`（v160.5 源码） | 循环音效到底由谁驱动、什么时候停、淡出有多久 —— 这是定位音效 bug 的依据 |
| `SpawnGroup.java`（v160.5，`javap` 反编译） | `getSpawned(wave)` 的**确切**算法（含 `spacing` 取模这道闸门），审计脚本按这个原样复刻 |
| `PlanetGenerator.java` / `BasicGenerator`（v160.5 源码） | 扇区尺寸怎么取、生成流程需要哪些前置状态 |

---

## 7. 验证

| 检查项 | 结果 |
|---|---|
| 编译（JDK 17） | 0 错误 |
| 模型检查 ModelCheck | **102 个方块模型，20 个图标，0 问题** |
| 资源检查 AssetCheck | 461 个精灵，0 缺失 |
| 光照烘焙 Bake | **problems: 0** |
| 安卓 DEX 校验 | classes.dex 合法，**安卓可加载** |
| 无头服务器启动 | **1 mods loaded** / **Server loaded.**，0 错误 |
| 星髓弹药注册（运行时日志） | **2/2 门炮塔**（吸积炮 165+90，事件视界炮 320+190） |
| 全 32 张战役图生成 | 尺寸 494～600，**缺矿 0，无刷怪门 0** |
| 刷怪审计（8 个威胁档 / 296 波） | roster 全覆盖，**空波 0** |

## 8. 安装

- 删除或替换旧的 Singularity JAR，只保留 `Singularity-8.5.6-beta.jar`。
- 改动只涉及音效时机、弹药表、两处伤害数值、地图尺寸和刷怪配置；
  **已建成的建筑不会变**，老存档也不需要重开（不过老存档还是旧尺寸的小图）。
- 星髓石现在只有**吸积炮**和**事件视界炮**能用。

## 9. 实测数据存档与复现

- `docs/v856/wave-audit.txt` —— 刷怪审计（每威胁档的 8.5.5 / 8.5.6 对照 + 空波检查）
- `docs/v856/map-audit.txt` —— 全 32 张战役图的尺寸、生成耗时、五种矿的数量、刷怪门数

复现方式：这两份数据是在无头服务器内、内容加载完成之后跑出来的。
做法是临时挂一个审计入口（`src/blackhole/WaveAudit.java` + `BlackHoleMod` 里一行调用），
跑完取日志后**已完整移除**（源码树与 jar 内均无残留，已核对）。

关键点有两个，踩过坑记在这里：

1. `getSpawned()` 里有一道 `(wave - begin) % spacing == 0` 的闸门 —— 漏掉它算出来的数会偏大。
2. 无头服务器里跑 `PlanetGenerator.generate()` 之前，必须先 `world.resize()`、
   **把每个格子预先填上 `Tile`**、再 `Events.fire(new WorldLoadEvent())`，
   否则 `BlockIndexer` / `WaveSpawner` 会在 `TileChangeEvent` 里 NPE
   （`resize()` 只建数组不填格子）。这是审计脚本的问题，不是模组的。
