# Singularity v8.4-beta — 变更与验证报告

日期：2026-10-07  
基线：用户提供的 `Singularity-v8.3-beta.zip.txt`（真实 ZIP，未使用先前独立 RBMK 项目）  
目标：官方 **Mindustry v160.5**；**JDK 17 + D8** 通用 JAR

## 1. 交付物

- `Singularity-v8.4-beta.jar`：桌面类与 Android DEX，作为 GitHub `8.4-beta` Release 的独立二进制附件。
- `Singularity-v8.4-beta-source-and-reports.zip`：本版完整源码、构建/测试脚本、报告、地图 CSV、模型动画与存档验证样本；**不包含 JAR**。GitHub 自动源码归档同样不含 JAR。
- 本地两个主文件放在工作区根目录；GitHub 仓库只保留源码、资源及报告。JAR 单独从 Release 下载，不提交到 Git，也不嵌入源码归档。

**JAR SHA-256**

```text
2c234b936798970df24b1984aa77ee83f63f595c7bacffcf77e346564bbccd2a
```

大小 **11,000,698 字节**；桌面类 **806** 个；DEX **1,218,520 字节**，格式 `035`，D8 输出 **1345** 个类定义。JAR CRC、每个桌面类和每份资产与最终工作树的逐字节比对通过。测试用注入类不在发布 JAR 中。

## 2. 请求逐项落实

### 配方

- 体素熔炉：`blackhole-aurora-coolant`，0.08/tick = **4.8/秒**。
- 除盐水处理站：`blackhole-tidewater`，0.36/tick = **21.6/秒**；除盐轻水产出仍为 **19.2/秒**。
- 使用本模组已有液体，不新建重名替代品。中文冷却液名称统一为“极光冷冻液”。
- 不仅核对消耗器：实际运行工厂确认新液体可生产、旧液体被拒绝，缺少指定液体时停产。

### 战术指挥核心：删除而非隐藏

删除方块注册、科技挂接、两种语言的方块名称/描述，以及以下实现：

- `astro/content/TacticalCommander.java`
- `astro/ai/Micro.java`
- `astro/ai/TacticalAI.java`
- `astro/ai/TacticalBrain.java`
- `astro/ai/Terrain.java`
- `astro/ai/UnitProfile.java`

源码、桌面类、DEX、运行时内容表均检查。拘留者及独立控制界面保留；它们不是被要求删除的建筑。旧存档的核心位置由官方缺失方块加载逻辑变为空地，没有注册占位物，也不返还材料。

### 两台新设备

| 方块 | 官方逻辑基础 | 占格 | 动态表现 |
|---|---|---|---|
| 流明焚化炉 | `Incinerator` / `IncineratorBuild` | 2×2 | 开口炉膛与底部热面；转子随炉温运转；独立烟管和驱动箱 |
| 反向流明分类器 | `Sorter` / `SorterBuild`，`invert=true` | 1×1 | 四向接口；低速旋转分流器，输送时加速；所选物品颜色指示 |

保留原版焚化接收/销毁与分类器的配置、轮流分流、敌我判定、防三连和保存格式。两者挂在欧雷利亚科技树上，造价使用欧雷利亚材料。

**机身和规划预览完全实时几何；没有新增机身贴图，没有远景贴图回退。** 底座先画，机器固定部件与转子共同排序，避免大底板错误遮住后部零件。UI/施工标识只借用引擎垃圾桶和筛选字形，明确跳过 `Block.createIcons` 的 Pixmap 打包，避免把字体区域误当贴图图集。

测试记录：

- 焚化炉 **852** 个网格面，实绘 **351～352** 个四边形；转子与炉壳的径向间隙约 **0.472** 世界单位。
- 分类器 **308** 个网格面，实绘 **93～94** 个四边形；转子与中心环的间隙约 **0.093** 世界单位。
- 每台录制 **48** 个不同旋转帧，有限坐标/物理占格/屏幕投影占格均通过。
- 正常、规划与远景/耗尽通用预算场景均无机身纹理区域绘制；`regionDraws = 0`。
- 模拟客户端 `loadIcon → load → createIcons`，使用非图集字体字形，确认 UI 图标与基础元数据初始化正常，且不走错误的图像打包路径。
- CPU 假批次耗时仅作局部参考，见 `geometry.log`；**不能当成桌面或手机 FPS**。

[自包含动画预览](v84/models.html) · [绘制验证日志](v84/geometry.log)

## 3. 战役优化与 Boss

- 全部 **32** 张地图生成并检查；初始区域 **10** 无 Boss。
- 其余 **31** 张都有终局遭遇；其中 **20** 张有多个遭遇。
- 默认组表合计 **61** 次 Boss 遭遇、**64** 个名义 Boss 单位。游戏难度仍可通过官方机制修正数量。
- Boss 使用原版 `StatusEffects.boss`；每组 `begin == end`，并指定一个真实出生口，避免被多入口重复放大。
- 全部 **114** 个入口都有干燥 **7×7** 集结区及可达核心的三格宽地面通路；保留噪声弯曲道路，不改成整齐直线。
- 常规兵力按入口数缩放并封顶。检查整个防守波次范围，默认规则下各地图峰值为 **15～45** 个常规单位/波，均低于对应预算。
- 检查地图签名不重复、水源下限、矿石四象限分布和入口距离。

### 实际引擎终局检查，不只检查表格

Mindustry 的 `runWave()` 先按 `state.wave - 1` 刷怪，再递增 `state.wave`；占领条件检查 `state.wave >= winWave`。因此终局组采用 **`begin = winWave - 2`**。若放在 `winWave - 1`，可能在它出场前就占领地图。

对 **31 张非初始图**均实际调用原版波次生成，验证：终局前不提前占领、生成的 Boss 数量正确且不按入口倍增、Boss 存活时不占领。

CSV 的 `win_wave` 是引擎的占领阈值，不是 Boss 组的零基索引。

[完整 32 图数据](v84/campaign-32.csv)

## 4. 守护穹顶修复

原实现的问题是**空盾启动、每秒仅恢复 9 点、重建后仍从空盾开始**，不是单纯“7000 容量不够大”。本次保留自定义外观与三个保存字段，修正生命周期：

| 行为 | 本版 |
|---|---|
| 新建 | 7000 满盾 |
| 基础恢复 | 120 HP/秒；原版力场投影器默认正常恢复约 105 HP/秒，作为量级参考 |
| 破盾 | 容量归零，暂停拦截 |
| 重建 | 840 个供电 tick 后满盾恢复，不再以几乎空盾重启 |
| 断电/禁用 | 不拦截；保留容量；断电不推进破盾重建 |
| 冷却液 | ×1.9 恢复/重建；测试重建 443 tick；半径 +8 格 |
| 碎片 | +5200 上限、22 秒；到期后即使离线也不超上限 |
| 损耗 | 官方 `shieldDamage`，跳过友军、已吸收和不可吸收弹丸 |
| 坠毁爆炸 | 接入官方 `ExplosionShield`，采用与原版相同的 2 倍坠毁损耗 |
| 显示 | 正常选择栏显示“当前 / 上限”和重建状态，逻辑 `shield` 可读 |

保存版本升级为 1，但自定义字段仍是 `shield/cooldown/charge` 三个 float。旧 revision 0 未破盾状态首次恢复满盾；旧重建计时保留；新 revision 1 精确保留受损值。非有限值与越界值也做了防御性限制。

## 5. 旧存档验证

使用**原始 8.3 JAR**创建真实 `.msav` 与对应 `SectorInfo`，放置旧战术核心、受损穹顶及 777 流明矿，将活动战役设为第 100 波，再由 8.4 加载：

- 指挥核心原位置为空地，其他验证建筑仍存在。
- 旧未破盾穹顶恢复到 7000。
- 当前第 100 波和 777 流明矿保留。
- 活动旧区域的终局安排在未来，重复升级不重复添加。
- 人为模拟已占领状态后，升级不会重启波次。

关键修复：规则升级挂在**保存加载事件**、引擎恢复 `SectorInfo` 之后，避免刚写入的终局阈值被旧区域资料覆盖。

不挖旧地图、不拆玩家建筑。对被自然地形堵住的旧入口，优先选安全地面入口；无安全入口时使用补足名义生命预算的带盾飞行单位。另有堵死旧入口的合成测试，确认不会困住地面 Boss，也不修改现存地形。

[迁移日志](v84/migration.log) · [原版样本生成日志](v84/old-v83-fixture.log)

## 6. 最终验证记录与边界

| 检查 | 结果 / 证据 |
|---|---|
| Java 17 编译 + Android D8 | 通过；`v84/build.log` |
| 模型结构 | **99 个方块模型、19 个图标模型、0 问题**；`v84/model-check.log` |
| 资产检查 | **440 项、0 缺失**；`v84/asset-check.log` |
| 新模型实际绘制路径 | 通过；含动画、投影、间隙、无贴图回退、UI 字形加载生命周期 |
| 官方服务器集成 | **6837 条断言通过**；`v84/integration.log` |
| 配方 | 消耗器与真实生产都通过 |
| 分类器 | 四向直行/侧向、轮流输出、满仓、敌方、三连、配置和保存通过 |
| 护盾 | 受击、功率、恢复、强化、传感器、状态条绑定和保存版本通过 |
| 新版完整存取 | 真实 `.msav` 回读保留受损盾和分类器配置 |
| 跨版本存取 | 原 8.3 → 8.4 通过 |
| JAR 内容 | CRC、类/资产一致、含 DEX、无核心实现/专用 AI、无测试类、无新机身 PNG |

**没有进行真实桌面 GPU、Android 设备、多人联网或长时间完整战役平衡试玩。** 数值状态条验证的是注册与绑定，不是实机 UI 截图。日志中的 JLine “无法创建系统终端”是沙箱无交互终端提示，不是模组加载错误。通过上述测试不能保证所有硬件、外部模组组合或损坏存档都没有问题。

## 7. 清理与可追溯性

- 删除旧版发布报告、旧地图截图/文本、过时测试驱动；新的 `test_server.sh` 运行本版测试。
- 不重复保留旧 JAR、编译缓存或 464 MB 工具链；最终工作区大小见根目录清单。
- 保留原始上传包作为基线，不计入新源码 ZIP。
- `src/` 与 `assets/` 基线比对：**13 项修改、6 项删除、3 项新增、1023 项不变**。既有 PNG 资产没有重绘。
- 详情见 [发布审计 JSON](v84/release-audit.json) 与 `v83-baseline-hashes.json`。

## 8. 官方 API 参考（固定 v160.5）

- [Incinerator.java](https://github.com/Anuken/Mindustry/blob/v160.5/core/src/mindustry/world/blocks/production/Incinerator.java)
- [Sorter.java](https://github.com/Anuken/Mindustry/blob/v160.5/core/src/mindustry/world/blocks/distribution/Sorter.java)
- [ForceProjector.java](https://github.com/Anuken/Mindustry/blob/v160.5/core/src/mindustry/world/blocks/defense/ForceProjector.java)
- [SpawnGroup.java](https://github.com/Anuken/Mindustry/blob/v160.5/core/src/mindustry/game/SpawnGroup.java)
- [WaveSpawner.java](https://github.com/Anuken/Mindustry/blob/v160.5/core/src/mindustry/ai/WaveSpawner.java)
- [Logic.java](https://github.com/Anuken/Mindustry/blob/v160.5/core/src/mindustry/core/Logic.java)
- [SectorInfo.java](https://github.com/Anuken/Mindustry/blob/v160.5/core/src/mindustry/game/SectorInfo.java)
