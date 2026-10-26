# Singularity 8.5.3-beta

适配 Mindustry **v160.5**（BE）。这一版只有两件事：

1. **类星体喷流的枪口对齐** —— 上一版剩下的唯一一处视觉问题。
2. **刷怪表重配** —— 每一波都出兵、所有能打的怪都能出场、高级怪降频、难度整体减半、新手图不出 T5。

> 本版**没有改动任何单位或方块的属性**（血量、伤害、射速、建造花销、移动速度等一律没碰）。
> 只改了刷怪频率与刷怪配置。

---

## 1. 类星体喷流：光束起点终于从枪口出来了

### 1.1 问题

类星体喷流（Quasar Lance，`quasar-lance`）是一门**连续光束炮**（`ContinuousTurret`）。它的炮管抬得很高，
模型里枪口在离地约 **2.05 格**的高度（`headZ = 4.6` + 枪口局部 Z `1.8`，换算成世界坐标 ≈ 2.05 格）。

Mindustry 的 `ContinuousTurret` 每一帧都会重新推算光束起点：

```java
// v160.5 core/src/mindustry/world/blocks/defense/turrets/ContinuousTurret.java
bulletX = x + Angles.trnsx(rotation - 90, shootX + entry.x, shootY + entry.y);
bulletY = y + Angles.trnsy(rotation - 90, shootX + entry.x, shootY + entry.y);
```

注意：`shootY` 是**沿炮管方向**的偏移，而 `shootX` 是横向偏移。也就是说，光束起点的**高度**在引擎里
根本没有对应的字段 —— `shootY` 写 0（默认值）就把起点直接钉在炮塔中心、贴着地面。

结果就是：光束从底座脚下的地面射出来，而炮管本身高高抬在空中，看上去像是"炮管是装饰，激光是从地里冒出来的"。
这也是上一版所有炮塔里**唯一**没对齐的一门 —— 其余炮塔的弹道走的是 `shoot.shootX/shootY` 的瞬时弹道路径，
8.5 那次统一对齐已经覆盖了；连续光束炮走的是另一条代码路径，所以漏了。

### 1.2 修复

新增 `src/blackhole/ContinuousTurret3D.java`，继承 `ContinuousTurret`，只重写一个方法：

```java
@Override
protected void updateBullet(ContinuousTurretBuild parent, ContinuousBulletEntry entry){
    super.updateBullet(parent, entry);        // 先跑引擎原本的算法
    if(muzzleZ <= 0f) return;                // 没配 3D 枪口就完全退回原版行为
    Bullet b = entry.bullet;
    float ox = b.x, oy = b.y;
    // 把枪口的 Z 高度按相机透视投影回屏幕：离相机越近放得越大
    float s = camD / Math.max(camD - muzzleZ, 1f);
    float nx = x + (ox - x) * s;
    float ny = y + camCy + (oy - camCy) * s;
    b.aimX += nx - ox;                       // 终点跟着起点一起挪
    b.aimY += ny - oy;                       // -> 长度和朝向完全不变，只是整条光束被抬起来
    b.set(nx, ny);
}
```

**关键点是 `aimX / aimY` 要跟着一起挪。** 光束的终点是 `aimX/aimY`，如果只挪起点而不挪终点，
光束的长度和朝向就变了（射程会缩水、会打偏）。两个点同增同减，射出去的还是原来那条射线，
只是整条往上抬到了枪口。

枪口高度不是手写的常数，而是在 `models/Models.java` 的 `install()` 里从 3D 模型直接读出来：

```java
ct.muzzleZ = tm.headZ + tm.muzzles[2];   // 塔头抬升 + 枪口局部 Z
ct.camD    = tm.cam.D;                   // 相机距离，用于透视缩放
ct.camCy   = tm.cam.cy;                  // 相机中心，缩放的锚点
```

这样以后改炮管模型，光束会自动跟着走，不用再改数字。

**没有改动**：射程、伤害、耗电、射速、炮塔血量 —— 一个属性都没碰，挪的只是起点和终点这两个绘制坐标。

---

## 2. 刷怪表重配

代码集中在 `src/blackhole/AureliaWaves.java`（另有一处上限在 `AureliaCampaign.balance()`）。

### 2.1 每一波都出兵（实测全部通过）

沿用 v8.5.2 的 `ensureEveryWave()`。引擎的 `SpawnGroup` 只在 `(wave - begin) % spacing == 0` 的波次出兵，
roster 的 `spacing` 是 2..7，必然留下整波空窗。这里扫描一遍，只在**真的空了**的波次补一只一次性分组
（`spacing = 1`、`begin = 该波`、`end = 该波`、`unitScaling = never`），补的是当时已经解锁的最弱单位。

### 2.2 所有能打的怪都能出场

roster 现在有 **30 个常规条目**（28 条手配 + 2 条自动补），并且**以后再加单位也不会漏**：
`autoAppend()` 会扫描 `Vars.content.units()`，把符合下面三条的单位自动补进来，强度按血量/火力粗估分档：

```java
if(!u.hasWeapons()) continue;                                  // 1. 不能攻击的
if(u.naval) continue;                                          // 2. 海军（潮汐系列）
if(chassis.contains(u)) continue;                              // 3. 玩家的机体（三座核心的 chassis）
if(u == astro.content.AstroContent.detainer) continue;          // 4. 拘留者走独立分组
```

实测被自动补进来的两个：`blackhole-lumen-medic`（医疗兵，T1）、`blackhole-spike-warden`（锚钉手，T3）。

被排除的三类（共 14 种），这是需求里点名要排除的：

| 类别 | 单位 |
|---|---|
| 不能攻击 | `lumen-courier`、`lumen-drone`、`lumen-hauler`、`assembly-drone`、`latum`、`mono`、`oct`、`renale` 等 |
| 海军 | `tide-lancer`、`tide-skiff`、`tide-tender`、`tide-warden` |
| 玩家机体 | `lumen-pilot`、`bastion-pilot`、`citadel-pilot` |

**拘留者（`astro-detainer`）** 不进常规 roster —— 它有自己那条"整张图正好 2 只、只在威胁度 ≥ 0.55 的图上出现"
的独立分组。混进常规表就会变成和普通怪一样按波数滚雪球，那正是上一版最难受的地方。

> Voxel 三件套（`tessellus` / `arachne` / `titan`）经核实**确实已注册**，只是加载顺序比 roster 首次构建晚，
> 现在它们正常进表。

### 2.3 高级怪降频 25%

| 档位 | 频率系数 | 间隔变化 |
|---|---|---|
| T1 | 0.60 | ×1.67 |
| T2 | 0.55 | ×1.82 |
| T3 | 0.55 | ×1.82 |
| **T4** | **0.75** | **×1.33（降频 25%）** |
| **T5** | **0.75 × 0.70 = 0.525** | **×1.90（降频 47.5%）** |

引擎里 `spacing` 越大出得越稀，所以"频率 ×0.75"实现为 `spacing / 0.75`。

### 2.4 新手图最高 T4，不出 T5

威胁度低于 `T5_THREAT_GATE = 0.34` 的图（战役前几张）**根本不生成 T5 分组**。威胁度越高的图，
T5 来得越早、越密 —— 后期依然是"特别多 T5"。

### 2.5 难度整体减半（实测）

> **为什么不能只砍"每组上限"。** 一开始按常规思路把每组的 `max` 打了对折，结果实测总量只掉了 22%。
> 原因是 `unitAmount` 和 `max` 的下限都是 **1**，而 `unitScaling`（每多来 1 只需要多少波）本来就很大，
> 绝大多数波次每组就是 1 只 —— 上限根本用不上。真正决定"一波来多少"的是 **Σ(1 / spacing)**，
> 也就是各组多久来一次。所以难度总闸主要落在**出怪间隔**上，而不是上限上。

最终生效的四个旋钮：

| 旋钮 | 值 | 作用 |
|---|---|---|
| `LOW_TIER_RATE` / `MID_TIER_RATE` | 0.60 / 0.55 | T1/T2/T3 出怪间隔拉长 |
| `HIGH_TIER_RATE` / `T5_RATE` | 0.75 / 0.70 | T4/T5 降频 25% 起 |
| `countNerf(tier, threat)` | 0.34 → 0.60（随威胁度） | 每组数量上限按档位 + 威胁度打折 |
| 涨速放缓系数 | ×2.2 → ×1.0（随威胁度回落） | 新手图几乎不长，终局图维持原速 |
| `DIFFICULTY` | 0.45 | 护盾成长 |

#### 实测数据

离线模拟：对 8 组（威胁度，守关波次）跑完整波次表，统计每一波的实际出兵数。

| 威胁度 | 守关波次 | 8.5.2 平均/波 | **8.5.3 平均/波** | 降幅 | 8.5.2 峰值 | **8.5.3 峰值** | T5 总数 旧→新 |
|---|---|---|---|---|---|---|---|
| 0.13（新手图） | 16 | 9 | **4** | −56% | 21 | **12** | 4 → **0** |
| 0.20 | 18 | 9 | **4** | −56% | 23 | **14** | 5 → **0** |
| 0.30 | 22 | 10 | **4** | −60% | 29 | **13** | 5 → **0** |
| 0.34 | 24 | 11 | **5** | −55% | 33 | **17** | 6 → 4 |
| 0.45 | 32 | 14 | **5** | −64% | 36 | **18** | 8 → 6 |
| 0.60 | 45 | 18 | **7** | −61% | 50 | **23** | 12 → 8 |
| 0.75 | 60 | 25 | **9** | −64% | 64 | **30** | 15 → 11 |
| 0.92（终局图） | 79 | 31 | **12** | −61% | 90 | **38** | 18 → 12 |

* 8 组配置的 **空波次数全部为 0**（每一波都出兵）。
* 前 3 组（威胁度 < 0.34）**T5 出兵数为 0**，即新手图确认为最高 T4。
* 出场种类：新手图 26 种，中期 30 种，终局图 31 种（含独立分组的拘留者）。
* 终局图（79 波）平均 12 只/波、峰值 38 只、含 12 只 T5 —— **难度依然是高的**，只是不再是开局就被压死。

---

## 3. 验证

| 检查 | 结果 |
|---|---|
| `javac`（JDK 17，全量源码） | 通过，0 error / 0 warning |
| `ModelCheck`（102 个方块模型 + 20 个图标） | **0 problems** |
| `bake.Bake`（模型烘焙 + 开放网格审计） | **problems: 0**，20 图标 open samples 0 |
| `check_dex.py` | **classes.dex 合法，安卓可加载**（magic `dex\n035\0`、adler32 与 SHA-1 一致、1392 个类） |
| 桌面 `.class` | 826 个（桌面 / 服务端 JVM） |
| 无头服务器实载 | `1 mods loaded` / `Server loaded`，**无 error / exception** |
| 波次离线模拟 | 8 组配置，空波次 0，T5 门槛生效 |
| 枪口对齐（ART 相关） | 新类是普通方法重写，无 lambda 传给引擎接口，无反射 |

---

## 4. 交付物

- `Singularity-8.5.3-beta.jar` —— 桌面 + 安卓双端，直接导入即可。
- 源码、文档、截图、报告在本 ZIP 内（ZIP 只是源码与报告包，**不是**另一个需要并装的模组）。

`docs/v853/shots/` 下是类星体喷流的烘焙图（`-preview` / `-hd` / `-icon`）与炮塔、单位总览图。
枪口对齐是**运行时的光束起点修正**，烘焙图只能显示炮塔模型本身，对齐效果需要在游戏里开火才能看到。

---

## 5. 已知未改动项

- 拘留者仍按 v8.5.1 的规则：只在威胁度 ≥ 0.55 的 15 张图上出现，每张图正好 2 只。
- `bh.settings.note`（设置页说明文字）里的版本号引用已同步到 v8.5.3。
- 本版没有新增地面矿石、没有新增方块、没有改动任何单位属性。
