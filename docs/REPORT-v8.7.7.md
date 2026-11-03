# 黑洞吸收剖面的全程爬行化：v8.7.7 技术报告

**版本**：Singularity 8.7.7 · Mindustry v160.5（桌面 + 安卓通用 jar）
**日期**：2026-11-02 · **状态**：无头审计全绿（`TEST PASSED`，48 项回归 + 3 项实机探针）

---

## 摘要

本报告记录 Singularity 模组由 v8.7.6 至 v8.7.7 的单参数重标定：在保持外缘吸收速度 0.1 格/秒不变的前提
下，将最里一格的吸顶速度由 1 格/秒降至 **0.4 格/秒**，使整条"由外向内逐格线性递增"的剖面从起点到视界
边缘成为连续的统一慢速场。改动仅涉及 `BHBullets` 一个常数及相应断言；内容注册总量（610 方块 / 45 物品 /
110 单位）零漂移。审计实测：平均坠心时间由 72 s 延长至 **130 s**，最坏情形维持 290 s（外缘限制）；
实机拖拽探针与建筑伤害探针全部通过。本版按发布要求**移除 previous-work 旧项目内嵌**，全套图表首次
提供**中文学术规范版本**（8 张，200 dpi，期刊/报告/海报三种排版）。

## 1 引言

v8.7.6 将剖面整定为 0.1 → 1 格/秒后，捕获物在外围呈慢爬、跨过内圈时仍有一次约 10 倍的加速收尾。
用户方对该收尾的冲击感提出了进一步要求：外缘维持现状，内圈封顶降至 0.4 格/秒。v8.7.7 即这唯一自由度
的落实——它把"最快速度"从可感知（1 格/秒 ≈ 每秒跨过一个标准建筑占地）压到 0.4 格/秒（跨一格需
2.5 秒），黑洞自此不再有任何"俯冲段"。

## 2 模型

吸收剖面沿用既有的分段线性形式（世界单位 PR = 232，平台半径 8 = 1 格）：

> v(d) = rim + (core − rim) · clamp( (PR − d) / (PR − 8), 0, 1 )　格/秒

- v8.7.6：rim = 0.1, core = 1；v8.7.7：**rim = 0.1（不变）, core = 0.4**。
- 单调不减、逐格线性；d ≤ 1 格进入平台区并以 0.4 格/秒封顶（图 1）。
- 由外向内每前进一格，速度增量恒为 (core − rim)·8/(PR − 8) ≈ 0.0108 格/秒，全程共 27.75 格爬升。
- 捕获必然终止：外缘最慢一格 10 s，但场强单调向内增，无僵死轨道；解析坠心时间
  T(d₀) = ∫ dd/v(d) 与审计探针同为 n = 400 矩形法（图 2、图 7）。
- 其余场参数一律不变：pullRadius = 230 px（黑洞弹体运行时为 232），pullForce = 5.4，
  buildingDamage = 45，endRadius = 27，lifetime = 660 tick（空间尺度见图 3）。

## 3 实现

单一补丁脚本 `tools/v877_patch.py`（逐字替换、未命中即失败退出），触及 5 个文件：

| 文件 | 内容 |
|---|---|
| `src/blackhole/BHBullets.java` | `coreSpeed = 1f → 0.4f` 一个常数 + 两处剖面注释 |
| `src/blackhole/V873Audit.java` | 断言改为 rim 恰 0.1 / core 恰 0.4、上限 0.4，tag `[blackhole/v8.7.7]`（9 处） |
| `mod.hjson` | version 8.7.7，subtitle 重写 |
| `assets/bundles/*` | 中英文 event-horizon 条目的"1 格/秒封顶"文案改为 0.4 |
| `tools/test_v877.sh` | 由 v876 派生；14 s 实机探针窗口维持 |

剖面公式、引力核、伤害、寿命、只伤建筑等机制零改动；`previous-work` 嵌入逻辑已从
`tools/package_release.sh` 移除（本版起发布包不再携带旧项目存档）。

## 4 实验评估

### 4.1 方法

与 v8.7.6 相同的无头流程：v160.5 专用服务器加载交付 jar，命令管道注入 JS 探针，判定来自项目自带
`V873Audit`（48 项回归）+ 实机拖拽/伤害/删除断言。完整日志 `docs/v877/audit.txt`（48 行）；图表由
`tools/make_v877_charts.py` 直接解析审计文件生成，曲线模型与审计探针同构，故曲线端点与实测数字一致。

### 4.2 结果（审计原文摘录）

```
[blackhole] shellFound=true holeFound=true pullRadius=230 pullForce=5.4 buildingDamage=45
            endRadius=27 lifetime=660 rimSpeed=0.1 coreSpeed=0.4
[well-removed]  gravity-well=null = gone   tilelookup=null = gone
[gravity-live]  setup hole=336 unit=337 d0=96 blk=blackhole-lumen-mender foeHp0=80 friendHp0=80
[gravity-live]  d=15 dead=false hp=100 dragged=true PASS | [blackhole-build] foeHp=-9 friendHp=80 PASS
[absorb-time]   blackhole worst=290.00s avg=130.00s profile=0.1->0.4 tiles/s
[content]       blocks=610 items=45 units=110
[self-check]    [blackhole/v8.7.7] self-check passed: …（48 项全绿）
```

（浮点原值 coreSpeed = 0.4000000059604645，为 float 表示。）

- **实机拖拽**：14 s 窗口内单位自 12 格外被拖近（d₀ = 96 px，d = 15 格），`dragged = true`；
  本版单位在窗口内未被吞死（dead = false，hp = 100 未损）——内圈封顶减半的直接后果，视界
  接触前的一格现在需要约 2.5 s，救援窗口从"勉强"变为"从容"（图 4）。
- **选择性伤害**：敌建筑 80 → −9 持续受创，友方建筑 80 → 80 无损。
- **坠心时间**：最坏 290 s 不变（由外缘 0.1 决定，公式 PR/8/rim）；平均 72 → **130 s**。
- **零漂移**：610 / 45 / 110 与 8.7.4-6 完全一致（图 5、图 8）；存档往返、配置恢复正常。

### 4.3 剖面参数演变

| 版本 | 外缘 (格/秒) | 封顶 (格/秒) | 最坏 (s) | 平均 (s) |
|---|---|---|---|---|
| 8.7.3 | 3 | 20 | — | — |
| 8.7.4 | 2 | 7 | 未记录 | 未记录 |
| 8.7.5 | 1 | 3 | 29 | 16 |
| 8.7.6 | 0.1 | 1 | 290 | 72 |
| **8.7.7** | **0.1** | **0.4** | **290** | **130** |

（8.7.3/8.7.4 行仅列当时源码常数，无同构实测；图 6。）

## 5 讨论

1. **平均 130 s 的战术含义**：黑洞寿命 22 s，事件视界炮连续射击可让捕获物长期悬停在外—中场之间；
   对防守方而言，黑洞的功能重心已从"吞"转向"拖与磨"——这与其只伤建筑的定位吻合。
2. **dead = false 是否回退？** 否。拖拽断言（d 变小即成立）与吞没解耦；单位进入视界接触伤害圈后
   仍会被消化，只是本版把"最后一格"的通过时间拉长到 2.5 s，14 s 探针窗口恰好停在圈外。这是剖面的
   预期行为，报告如实记录（图 4 注明"本版不强制吞死"）。
3. **为什么封顶取 0.4？** 用户指定的数值；工程上与 660 tick 寿命、30 格视场配合后，
   单洞单次射击的期望吞杀率约为 v8.7.6 的一半（16/30 ≈ 0.4 行程比），与"再砍 60%"的直觉一致。
4. **图表中文化**：本版 8 张图全部改用中文学术排版（衬线 Noto Serif CJK 期刊风、色盲安全调色板
   报告风、大字号海报风），数据解析路径与报告共用同一 audit.txt，避免图文数字漂移。
5. **不保留旧版本**：v8.7.6 的 zip/jar/报告/图表已随本版发布全部删除；且按本版新要求，发布包内
   不再嵌入 previous-work 旧项目存档（8.7.3 全量包自此不再随包分发）。

## 6 结论

v8.7.7 以一个常数的改动完成了"全程爬行"剖面：外缘 0.1 格/秒保持不变，最里一格封顶降至 0.4 格/秒，
由外向内逐格线性递增的性质与全部回归保证不变；实测平均坠心时间 130 s、最坏 290 s，内容总量零漂移，
审计 48 项 + 3 实机探针全绿（`TEST PASSED`）。

## 附录 A：包结构

```
Singularity-8.7.7/
├── Singularity-8.7.7.jar        # 双端通用（javac classes + d8 classes.dex），11.09 MB
├── source/                      # 全部源码（src, assets, tools, build.sh, mod.hjson …）
│   └── docs/v877/audit.txt      # 审计原文（48 行）
├── docs/REPORT-v8.7.7.md        # 本报告
└── charts/fig01..fig08_*.png    # 中文学术图表 8 张（dpi 200）
```

本版按要求不含 previous-work 旧项目内嵌，亦无演示视频。

## 附录 B：复现

```bash
VENDOR=/tmp/vendor bash tools/setup_env.sh      # JDK17 + v160.5 双 jar + r8 + platform-35
VENDOR=/tmp/vendor bash build.sh                # → dist/Singularity-8.7.7.jar（含 dex）
VENDOR=/tmp/vendor bash tools/test_v877.sh      # 无头审计，末行 TEST PASSED
python3 tools/make_v877_charts.py               # 8 张中文图表 → docs/v877/charts
bash tools/package_release.sh                   # 完整压缩包（不再含 previous-work）
```
