# Singularity 8.5.5-beta

Mindustry v160.5 · 完整 JAR（桌面 class + 安卓 DEX）

这一版做了四件事：修好类星体喷流、把晶格蠕者的攻击特效整套重写、让星髓石可以当炮弹用，
并给这发炮弹配了一套完全自制的特效、过百的伤害和一个全新的减益状态。

每一项都附**实测数据**：特效大小不是照代码估的，是用无 GL 的录制型 Batch 把每个特效
逐帧跑一遍量出来的；枪口位置也是直接算出来的。复跑方式见文末。

---

## 1. 类星体喷流：修好了，现在能开火也有开火特效

症状是"开不了火，而且一点开火特效都没有"。原因不在伤害、射程或者耗电，而在**枪口的坐标算错了**。

连续光束炮和普通炮不一样：引擎每一刻都会把光束按 `shootY`（沿炮管方向的偏移）重新摆一次位置。
这个模组为了让 3D 炮口和画面上的炮塔对得上，要用相机透视把炮口往上抬。v8.5.3 那版抬的时候写成了：

```
ny = y + camCy + (b.y - camCy) * s
```

问题是 `b.y` 是一个**世界坐标**（比如 2000），而 `camCy` 是相对炮塔的（−78.4）。
正确写法要**先减掉炮塔中心**，取相对偏移再投影。下面是直接算出来的对照
（`tools/preview/src/preview/QuasarGeom.java`，炮塔摆在 (1000, 2000)）：

| 写法 | 算出来的枪口落点 | 离炮塔多远 |
|---|---|---|
| v8.5.3（错误的写法） | 1016, 4142 | **2142 像素（268 格）** |
| **v8.5.5（正确）** | 1016, 2021 | **21 像素（2.6 格）** |

光束被甩到两百多格之外 —— 看不见、也打不到任何东西，看起来就是"根本没开火"。
修正之后枪口正好落在塔头该在的地方（2.6 格，对一座 4×4 的炮塔来说是正常的）。

顺带修的另外两点：

- **长度改成从枪口量。** 引擎原本从炮塔中心量，只挪起点不改量法的话，光束长度每一刻
  都会把抬升量再累加一次，会自己越长越长。
- **补上了枪口开火特效。** 之前连续光束炮的开火特效被"3D 炮口"那套逻辑摘走了，
  而那条路径又不管光束炮 —— 结果就是谁都不播。现在由方块自己在枪口补播一个全新的
  `AureliaFx.quasarMuzzle`（三道聚焦环向内收束，实测峰值半径 13.7 像素）。

伤害、射程、耗电、射速**一个都没动**，改的只有几何和特效。

改动文件：`src/blackhole/ContinuousTurret3D.java`（重写 `updateBullet`）、
`src/blackhole/models/Models.java`（连续光束炮不再被摘走开火特效）、
`src/blackhole/AureliaFx.java`（新增 `quasarMuzzle`）。

---

## 2. 晶格蠕者：攻击特效整套重写，而且真的变小了

8.5.4 那次已经把落点爆炸"缩了 1/3"，但打起来还是一大团。原因查出来了：
原来那套特效把 `e.rotation` **当成半径在用**：

```java
Lines.circle(e.x, e.y, 4f + e.fin() * (e.rotation + 26f));
Lines.poly(e.x, e.y, 6, 2f + e.fin() * (e.rotation * 0.6f + 16f));
```

炮弹的 rotation 是 0～360 的**朝向角**，不是尺寸。所以那朵爆炸的真实大小是随炮弹朝哪个方向飞
而变的 —— 除以 3 只是缩放了一个会乱跳的量。

所以这一版**从零重写**（新文件 `src/blackhole/CrawlerFx.java`），所有半径都是写死的常数，
跟朝向完全无关。下面是**实测**的峰值半径（8 像素 = 1 格），换五个朝向各量一遍：

| 特效 | 8.5.1 原版 | 8.5.4（÷3） | **8.5.5（重写）** |
|---|---|---|---|
| 枪口火光 | 9.0 | 2.0 | **1.6** |
| 枪口硝烟 | 11.6 | 3.9 | **2.8** |
| 落点爆炸 | 389.0（10～389） | 129.7（10～130） | **9.6（恒定）** |
| 破片命中 | 14.0 | 14.0 | **1.7** |

落点爆炸相对 8.5.4 小了 **13.5 倍**，相对 8.5.1 原版小了 **40 倍**。
更要紧的是括号里那一列：8.5.4 只有炮弹**碰巧朝北飞**的时候才是 10 像素，
换个朝向就一路涨到 130 像素（16 格）。8.5.5 在任何朝向下都是 9.6 像素（1.2 格）。

### 数字是怎么量出来的

- **顶点法**（`tools/preview/src/preview/FxSize.java`）：用无 GL 的录制型 Batch
  （`FakeBatch`）接管 `Core.batch`，把每个特效从 0 跑到寿命结束，收下所有顶点，
  算离中心最远的那个有多远；换五个朝向（0/90/180/259/359）各跑一遍。
  "半径随朝向变化"就是这么抓出来的。
- **像素法**（`tools/preview/src/preview/FxShot.java`）：把同一批特效光栅化成 PNG
  （`docs/v855/shots/`），再按像素独立量一遍作为交叉验证。
  两种方法在同一朝向下给出 9.6 / 10.6 像素，吻合。

顺带一提，`AureliaFx.detainImpact` 原样保留 —— 定锚 spike 和 Detainer 还在用它，
那边 26 格的溅射是设计如此。

---

## 3. 星髓石可以当炮弹了

星髓石原来是纯建造材料。这一版把它注册成了**四门炮弹炮塔**的弹药：
吸积炮、参考系拖曳炮、视界，以及 `shardVolley`。打出去的叫**星髓长枪**。

### 3.1 伤害过百

| 项 | 值 | 说明 |
|---|---|---|
| 直伤 | **165** | 需求是"伤害过百" |
| 范围伤害 / 半径 | 90 / 26 像素（3.25 格） | 命中后额外炸一圈 |
| 射程 | 300 像素（37.5 格） | 介于吸积炮 235 与视界 400 之间 |
| 每份矿几发 | 4 发 | 星髓石稀有，所以一份顶四发 |
| 对建筑 | ×0.75 | 打建筑略软 |

### 3.2 附带 buff：星髓灼印

命中后给目标烙上一个新的减益状态（`marrow-brand`，中文名"星髓灼印"），持续 6 秒：

- **持续灼烧**：约 45 点/秒，6 秒共约 270 点。
- **减速 38%**：跑不掉，也追不上。
- **输出 −28%、射速 −35%**：它自己打人也变软。

把直伤 165 + 范围 90 + 灼烧 270 加起来，一发的总期望伤害约 **525**。

> 这里有个坑值得一提：网上查到的资料说状态效果有 `armorMultiplier`（护甲倍率）这个字段，
> 我一开始就是照着写的，结果编译不过 —— 把 `javap` 打开 v160.5 的实际 class 一看，
> 这个字段**根本不存在**。最后用的是确实存在的
> `damage / intervalDamage / speedMultiplier / damageMultiplier / reloadMultiplier`。
> 所以文档不能全信，得对着 jar 核一遍。

### 3.3 攻击特效完全自制

新文件 `src/blackhole/MarrowFx.java`，六个特效全部从零手写，没有一个是拿现成特效缩放
或改色得来的。造型语言取自星髓石本身的设定（恒星残骸：冷却了几十亿年的暗壳，
剖开处露出惨白的星髓）—— **惨白的芯 + 冷紫的外壳 + 一圈正在塌陷的暗环**，
三个颜色常量（`core` / `shell` / `dark`）全组共用，所以枪口、弹道、落点一眼能认出是同一种弹药。

| 特效 | 画的是什么 | 峰值半径（像素） |
|---|---|---|
| `shoot`（炮口） | 六角星塌陷 + 两条反向自转的环 + 暗壳推开 | 11.5 |
| `wake`（弹道尾迹） | 一粒惨白余烬拖着暗壳碎屑 | 5.4 |
| `hit`（命中） | 八芒星爆开 + 双层冲击环 + 12 粒碎壳 + 留在地上的暗痕 | **31.1** |
| `despawn`（射程尽头） | 半个爆闪，没有冲击环 | 19.8 |
| `brand`（灼印持续中） | 单位身上冒火星 + 缓慢脉动的环 | 6.5 |
| `brandApply`（烙上瞬间） | 收紧的环 + 四角星 | 13.8 |

同样是实测值，而且同样**不随朝向变化**。图在 `docs/v855/shots/fx-marrow.png`。

---

## 4. 查过的资料

| 来源 | 用来确认什么 |
|---|---|
| `ContinuousTurret.java` / `ItemTurret.java`（v160.5 源码） | `updateBullet()` 每一刻都按 shootX/shootY 重算光束起点；引擎是从炮塔中心量长度的；`ammo()` 怎么建弹药表、消耗器什么时候被冻结 |
| StatusEffect / BulletType 文档 | 状态效果与弹药的字段清单；**其中 armorMultiplier 一条是错的**，已对着 jar 纠正 |
| `javap` 反编译 v160.5 的 jar | 核实 `StatusEffect` 到底有哪些字段，以及 `Effect$EffectContainer.set()` 的参数顺序 |

---

## 5. 验证

提交前跑过的检查，全部通过：

| 检查项 | 结果 |
|---|---|
| 编译（JDK 17） | 0 错误 |
| 模型检查 ModelCheck | **102 个方块模型，20 个图标，0 问题** |
| 资源检查 AssetCheck | 461 个精灵，0 缺失 |
| 光照烘焙 Bake | **problems: 0** |
| 安卓 DEX 校验 | classes.dex 合法，**安卓可加载**（1415 个类） |
| 无头服务器启动 | **1 mods loaded** / **Server loaded.**，0 错误 |
| 星髓弹药注册（运行时日志） | **4/4 门炮塔**已注册，伤害 165 + 范围 90 |

### 复跑测试数据

```bash
export JDK=/home/user/.cache/vendor/jdk-17.0.21+9
export V=/home/user/.cache/vendor

# 特效半径实测（顶点法）
$JDK/bin/javac -nowarn -encoding UTF-8 -cp "$V/Mindustry.jar:src" \
    -d /tmp/fx tools/preview/src/preview/{FakeBatch,FxSize}.java
$JDK/bin/java -Dfile.encoding=UTF-8 -cp "$V/Mindustry.jar:/tmp/fx" preview.FxSize

# 特效图 + 像素法交叉验证
$JDK/bin/javac -nowarn -encoding UTF-8 -cp "$V/Mindustry.jar:src" \
    -d /tmp/fx tools/preview/src/preview/{FakeBatch,Anim,FxShot}.java
$JDK/bin/java -Dfile.encoding=UTF-8 -cp "$V/Mindustry.jar:/tmp/fx" preview.FxShot docs/v855/shots

# 类星体枪口几何对照
$JDK/bin/javac -nowarn -encoding UTF-8 -cp "$V/Mindustry.jar" -d /tmp/geo \
    tools/preview/src/preview/QuasarGeom.java
$JDK/bin/java -Dfile.encoding=UTF-8 -cp "$V/Mindustry.jar:/tmp/geo" preview.QuasarGeom
```

输出已存档：

- `docs/fx-sizes-8.5.5.txt` —— 特效半径实测（顶点法）
- `docs/fx-shot-measurements.txt` —— 像素法交叉验证
- `docs/quasar-geom-8.5.5.txt` —— 枪口几何对照
- `docs/v855/shots/` —— `fx-crawler.png` / `fx-marrow.png` / `fx-compare.png` / `fx-quasar.png`

---

## 6. 安装

- 删除或替换旧的 Singularity JAR，只保留 `Singularity-8.5.5-beta.jar`。
- 改动只涉及特效几何、弹药表和状态效果；**已建成的建筑不会变**，老存档也不需要重开。
- 星髓石现在既是建造材料也是弹药 —— 往上面四门炮塔里塞就能用。
