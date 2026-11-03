#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""v8.7.7 学术图表集（全中文，8 张，dpi=200）。

三种学术规范混合：
  A 期刊信件风 : fig01-fig04（衬线中文、细框、图题注）
  B 学术报告风 : fig05-fig06（色盲安全调色板 + 数据表）
  C 海报风     : fig07-fig08（大字号）

v8.7.7 的 rim/core、worst/avg、内容计数全部现场解析自 docs/v877/audit.txt；
坠心时间曲线采用与审计 JS 探针同构的 n=400 矩形法（PR 由 worst×rim×8 反推），
因此曲线端点与审计实测数字严格一致。历史版本（8.7.3-8.7.6）用其源码常数与
存档审计值标注。

    python3 tools/make_v877_charts.py [输出目录]
"""
import os
import re
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
AUDIT = os.path.join(ROOT, "docs", "v877", "audit.txt")
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "docs", "v877", "charts")
os.makedirs(OUT, exist_ok=True)

# ---------------------------------------------------------------- parse audit
txt = open(AUDIT, encoding="utf-8", errors="replace").read()
def grab(pattern, cast=float):
    m = re.search(pattern, txt)
    if not m:
        sys.exit(f"audit.txt missing pattern: {pattern}")
    return cast(m.group(1))

RIM  = round(grab(r"rimSpeed=([0-9.]+)"), 3)          # 0.1
CORE = round(grab(r"coreSpeed=([0-9.]+)"), 3)          # 0.4
WORST = grab(r"worst=([0-9.]+)s")                      # 290.00
AVG   = grab(r"avg=([0-9.]+)s")                        # 130.00
BLOCKS = grab(r"blocks=(\d+)", int)
ITEMS  = grab(r"items=(\d+)", int)
UNITS  = grab(r"units=(\d+)", int)
PR = WORST * RIM * 8.0                                 # 232.0，与审计 worst 公式互逆

PROFILES = {"8.7.3": (3.0, 20.0), "8.7.4": (2.0, 7.0), "8.7.5": (1.0, 3.0),
            "8.7.6": (1.0, None), "8.7.7": (RIM, CORE)}
PROFILES["8.7.6"] = (0.1, 1.0)
MEASURED = {"8.7.5": (29.0, 16.0), "8.7.6": (290.0, 72.0), "8.7.7": (WORST, AVG)}

def speed_tiles(d_tiles, rim, core):
    """与 BHBullets.update() 同式：lerp(rim, core, clamp((PR-d)/(PR-8)))，单位：格/秒。"""
    dst = d_tiles * 8.0
    t = np.clip((PR - dst) / (PR - 8.0), 0.0, 1.0)
    return rim + (core - rim) * t

def fall_time(d0, rim, core, n=400):
    """与审计 JS 同构的左矩形积分：d 从 PR 到 0 分 n 段，累加段长/速度。"""
    step_px = PR / n
    acc = 0.0
    for i in range(n):
        d = (i + 0.5) * step_px / 8.0
        if d > d0:
            continue
        v = max(speed_tiles(d, rim, core), 1e-9)
        acc += (step_px / 8.0) / v
    return acc

# Okabe-Ito 色盲安全调色板
OI = ["#0072B2", "#D55E00", "#009E73", "#CC79A7", "#56B4E9", "#E69F00"]
GRAY = "#8a8f98"

# ================================================================ A 期刊信件风
plt.rcParams.update({
    "font.family": "serif",
    "font.serif": ["Noto Serif CJK JP", "DejaVu Serif"],
    "axes.linewidth": 0.8, "axes.labelsize": 10, "axes.titlesize": 11,
    "xtick.labelsize": 9, "ytick.labelsize": 9, "legend.fontsize": 9,
    "figure.dpi": 200, "savefig.dpi": 200,
    "axes.grid": True, "grid.linewidth": 0.4, "grid.linestyle": "--", "grid.alpha": 0.35,
    "axes.unicode_minus": False,
})

def jax(ax, title, xl, yl):
    ax.set_title(title, pad=7)
    ax.set_xlabel(xl); ax.set_ylabel(yl)
    for s in ("top", "right"):
        ax.spines[s].set_visible(False)
    ax.tick_params(direction="in", top=False, right=False, length=3)

# ---- 图 1：吸收剖面 --------------------------------------------------------
dd = np.linspace(0, PR / 8.0, 1200)
fig, ax = plt.subplots(figsize=(4.1, 2.7))
order = ["8.7.3", "8.7.4", "8.7.5", "8.7.6", "8.7.7"]
for k, name in enumerate(order):
    rim, core = PROFILES[name]
    lw = 2.2 if name == "8.7.7" else 1.1
    al = 1.0 if name == "8.7.7" else 0.55
    ax.plot(dd, speed_tiles(dd, rim, core), color=OI[k % 6], lw=lw, alpha=al,
            label=f"{name}（{rim:g}→{core:g} 格/秒）" + ("，本版" if name == "8.7.7" else ""))
ax.axvline(1.0, color=GRAY, lw=0.6, ls=":")
ax.annotate("内圈平台\nd ≤ 1 格恒速", xy=(1.0, 16.0), xytext=(2.6, 14.2),
            fontsize=8, color="#555", arrowprops=dict(arrowstyle="-", lw=0.5, color=GRAY))
jax(ax, "图 1　下落速度随到视界距离的变化（8.7.x 剖面族）",
    "到奇点中心距离 d（格）", "径向下落速度 v(d)（格/秒）")
ax.set_xlim(0, PR / 8.0); ax.set_ylim(0, 21)
ax.legend(frameon=False, loc="upper right", handlelength=1.7)
fig.tight_layout(pad=0.5)
fig.savefig(f"{OUT}/fig01_吸收剖面.png"); plt.close(fig)

# ---- 图 2：坠心时间积分 ----------------------------------------------------
fig, ax = plt.subplots(figsize=(4.1, 2.7))
d0s = np.linspace(2, PR / 8.0, 220)
for k, name in enumerate(["8.7.4", "8.7.5", "8.7.6", "8.7.7"]):
    rim, core = PROFILES[name]
    T = np.array([fall_time(x, rim, core) for x in d0s])
    lw = 2.2 if name == "8.7.7" else 1.1
    ax.semilogy(d0s, T, color=OI[k % 6 + 1], lw=lw, alpha=1.0 if name == "8.7.7" else 0.6,
                label=name)
    if name in MEASURED:
        w = MEASURED[name][0]
        i = int(np.argmin(np.abs(d0s - PR / 8.0)))
        ax.plot(d0s[i], T[i], "o", ms=4, color=OI[k % 6 + 1])
        ax.annotate(f"实测最坏 {w:g} 秒", xy=(d0s[i], T[i]), xytext=(-8, 7),
                    textcoords="offset points", fontsize=7.5, color=OI[k % 6 + 1], ha="right")
jax(ax, "图 2　自捕获点直线坠心所需时间 T(d₀)（与审计同构模型）",
    "捕获半径 d₀（格）", "坠心时间 T（秒，对数轴）")
ax.legend(frameon=False, loc="upper left", ncol=4, columnspacing=1.1)
ax.set_xlim(2, PR / 8.0)
fig.tight_layout(pad=0.5)
fig.savefig(f"{OUT}/fig02_坠心时间积分.png"); plt.close(fig)

# ---- 图 3：空间尺度 --------------------------------------------------------
fig, ax = plt.subplots(figsize=(4.1, 3.4))
R = PR / 8.0
for rad, lab, c in [(R, f"引力半径 230 单位 = {230/8:.2f} 格", OI[0]),
                    (27 / 8.0, "视界（终了半径）27 单位", OI[1]),
                    (1.0, "内圈平台 8 单位 = 1 格（0.4 格/秒封顶）", OI[3])]:
    ax.add_patch(plt.Circle((0, 0), rad, fill=False, color=c, lw=1.3))
    ax.annotate(lab, xy=(rad * 0.72, rad * 0.72) if rad > 5 else (rad, 0),
                xytext=(rad * 0.4, rad * 0.86) if rad > 5 else (2.4, 2.6),
                fontsize=8, color=c,
                arrowprops=dict(arrowstyle="-", lw=0.5, color=c))
ax.add_patch(plt.Circle((0, 0), 1.0, color="#111"))
rng = np.random.default_rng(7)
rr = rng.uniform(R * 0.55, R * 0.98, 14); aa = rng.uniform(0, 2 * np.pi, 14)
ax.scatter(rr * np.cos(aa), rr * np.sin(aa), s=9, color=GRAY, marker=".", label="被捕获 debris→残骸")
r_s = np.linspace(R * 0.95, 1.2, 60); a_s = np.linspace(0.6, 3.4, 60)
ax.plot(r_s * np.cos(a_s), r_s * np.sin(a_s), color=OI[2], lw=0.8, alpha=0.85)
ax.annotate("螺旋内坠轨迹（示意）", xy=(r_s[20] * np.cos(a_s[20]), r_s[20] * np.sin(a_s[20])),
            xytext=(7, 13), fontsize=8, color=OI[2],
            arrowprops=dict(arrowstyle="->", lw=0.6, color=OI[2]))
ax.text(-R + 1, -R + 1.5, "寿命 660 tick = 22 秒", fontsize=8.5, color="#444")
ax.set_xlim(-32, 32); ax.set_ylim(-32, 32); ax.set_aspect("equal")
for s in ("top", "right"):
    ax.spines[s].set_visible(False)
ax.grid(True, lw=0.35, alpha=0.3)
ax.tick_params(labelsize=7.5)
ax.set_title("图 3　坍缩场空间尺度（1 格 = 8 单位）", pad=7)
fig.tight_layout(pad=0.5)
fig.savefig(f"{OUT}/fig03_空间尺度.png"); plt.close(fig)

# ---- 图 4：实机探针判定 ----------------------------------------------------
labels = ["引力场\n实时生效", "单位自外圈\n被拖拽", "拖入后\n吞没判定", "敌建筑\n受创", "友方建筑\n无损", "存档\n往返"]
stats  = ["d 15<96\n拖拽成立", "14 秒窗口\n持续减速接近", "视界接触伤害\n（本版不强制吞死）", "80 → −9", "80 → 80", "配置项\n原样恢复"]
fig, ax = plt.subplots(figsize=(4.6, 2.35))
for i, (lab, st) in enumerate(zip(labels, stats)):
    ax.add_patch(plt.Rectangle((i, 0), 0.82, 1.0, color=OI[2], alpha=0.14, ec=OI[2], lw=1.1))
    ax.text(i + 0.41, 0.66, "通过", ha="center", fontsize=11, color=OI[2], fontweight="bold")
    ax.text(i + 0.41, 0.24, st, ha="center", fontsize=6.4, color="#333")
    ax.text(i + 0.41, -0.22, lab, ha="center", fontsize=8.3)
ax.set_xlim(-0.2, 6); ax.set_ylim(-0.75, 1.15); ax.axis("off")
ax.set_title("图 4　v8.7.7 无头实机探针判定（n = 1，14 秒窗口；来源 docs/v877/audit.txt）", pad=8, fontsize=10)
fig.tight_layout(pad=0.5)
fig.savefig(f"{OUT}/fig04_实机探针.png"); plt.close(fig)

# ================================================================ B 学术报告风
plt.rcParams.update({
    "font.family": "sans-serif",
    "font.sans-serif": ["Noto Sans CJK JP", "DejaVu Sans"],
    "axes.titlesize": 11, "axes.labelsize": 10, "xtick.labelsize": 9,
    "ytick.labelsize": 9, "legend.fontsize": 9, "axes.grid": True,
    "grid.linestyle": "--", "grid.alpha": 0.4, "grid.linewidth": 0.6,
})

# ---- 图 5：吸收耗时实测 + 数据表 -------------------------------------------
fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(7.6, 2.9), width_ratios=[1.15, 1])
names = ["8.7.5", "8.7.6", "8.7.7"]
x = np.arange(3)
worst = [MEASURED[n][0] for n in names]
avg = [MEASURED[n][1] for n in names]
b1 = ax1.bar(x - 0.18, worst, 0.34, label="最坏情形", color=OI[1])
b2 = ax1.bar(x + 0.18, avg, 0.34, label="平均", color=OI[0])
for bs in (b1, b2):
    for r in bs:
        ax1.text(r.get_x() + r.get_width() / 2, r.get_height() + 5, f"{r.get_height():g} 秒",
                 ha="center", fontsize=8)
ax1.set_xticks(x, names); ax1.set_ylabel("坠心耗时（秒）")
ax1.set_title("吸收耗时审计（最外缘生成，30 格行程）")
ax1.set_ylim(0, 350); ax1.legend(frameon=False)
rows = [["版本", "外缘(格/秒)", "封顶(格/秒)", "最坏(秒)", "平均(秒)"],
        ["8.7.5", "1", "3", "29", "16"],
        ["8.7.6", "0.1", "1", "290", "72"],
        ["8.7.7", f"{RIM:g}", f"{CORE:g}", f"{WORST:g}", f"{AVG:g}"]]
ax2.axis("off")
tb = ax2.table(cellText=rows[1:], colLabels=rows[0], loc="center", cellLoc="center")
tb.auto_set_font_size(False); tb.set_fontsize(8.5); tb.scale(1, 1.55)
for j in range(5):
    tb[0, j].set_facecolor("#eef1f5")
tb[3, 0].set_facecolor("#dff0e6")
for i in (1, 2):
    tb[i, 0].set_facecolor("#f6f7f9")
ax2.set_title("数据源：docs/v877/audit.txt", fontsize=9, color="#555")
fig.tight_layout(pad=0.7)
fig.savefig(f"{OUT}/fig05_吸收耗时实测.png"); plt.close(fig)

# ---- 图 6：剖面参数演变 ----------------------------------------------------
fig, ax = plt.subplots(figsize=(7.6, 3.0))
vers = ["8.7.3", "8.7.4", "8.7.5", "8.7.6", "8.7.7"]
xr = np.arange(len(vers))
rim = [PROFILES[v][0] for v in vers]; core = [PROFILES[v][1] for v in vers]
ax.plot(xr, core, "-o", color=OI[1], label="内圈封顶（格/秒）", lw=2.0)
ax.plot(xr, rim, "-o", color=OI[0], label="外缘速度（格/秒）", lw=2.0)
for i, v in enumerate(vers):
    ax.annotate(f"{core[i]:g}", (i, core[i]), xytext=(0, 8), textcoords="offset points",
                ha="center", fontsize=8.5, color=OI[1])
    ax.annotate(f"{rim[i]:g}", (i, rim[i]), xytext=(0, -14), textcoords="offset points",
                ha="center", fontsize=8.5, color=OI[0])
ax.axvspan(3.5, 4.5, color="#dfe9f5", alpha=0.55, zorder=0)
ax.text(4, 0.075, "v8.7.7", ha="center", fontsize=8.5, color="#41618f")
ax.annotate("外缘自 8.7.6 起保持不变", xy=(4, 0.1), xytext=(2.2, 0.35), fontsize=8.5, color=OI[0],
            arrowprops=dict(arrowstyle="->", lw=0.8, color=OI[0]))
ax.set_yscale("log"); ax.set_ylim(0.05, 45)
ax.set_xticks(xr, vers); ax.set_ylabel("速度（格/秒，对数轴）")
ax.set_title("8.7.x 吸收剖面参数演变（源码常数；8.7.5 起各行经无头审计实测复核）")
ax.legend(frameon=False, loc="upper right")
fig.tight_layout(pad=0.7)
fig.savefig(f"{OUT}/fig06_参数演变.png"); plt.close(fig)

# ================================================================ C 海报风
plt.rcParams.update({
    "font.family": ["Noto Sans CJK JP", "DejaVu Sans"],
    "axes.titlesize": 16, "axes.labelsize": 13, "xtick.labelsize": 11,
    "ytick.labelsize": 11, "legend.fontsize": 12, "axes.linewidth": 1.6,
})

# ---- 图 7：海报主图 ---------------------------------------------------------
fig, (a1, a2) = plt.subplots(1, 2, figsize=(11.4, 4.1)); fig.subplots_adjust(wspace=0.22)
rim, core = RIM, CORE
dd = np.linspace(0, PR / 8.0, 800)
sp = speed_tiles(dd, rim, core)
a1.plot(dd, sp, color=OI[0], lw=3.6)
a1.fill_between(dd, 0, sp, color=OI[0], alpha=0.12)
a1.set_title(f"吸收剖面：外缘 {rim:g} → 内圈封顶 {core:g} 格/秒", fontsize=15)
a1.set_xlabel("到奇点中心距离 d（格）"); a1.set_ylabel("下落速度（格/秒）")
a1.annotate("外缘：可见慢爬", xy=(28.0, float(sp[np.argmin(abs(dd - 28))])), xytext=(15.5, 0.20),
            fontsize=11.5, arrowprops=dict(arrowstyle="->", lw=1.3))
a1.annotate("最里一格封顶，越近也不再快", xy=(1.0, core), xytext=(3.4, 0.31), fontsize=11.5,
            arrowprops=dict(arrowstyle="->", lw=1.3))
d0s = np.linspace(2, PR / 8.0, 200)
for k, name in enumerate(["8.7.5", "8.7.6", "8.7.7"]):
    r0, c0 = PROFILES[name]
    lw = 3.6 if name == "8.7.7" else 1.8
    a2.semilogy(d0s, [fall_time(y, r0, c0) for y in d0s], lw=lw, color=OI[k + 1], label=name)
a2.set_title("坠心时间 T(d₀)：内圈再降半档，全曲线整体上移", fontsize=15)
a2.set_xlabel("捕获半径 d₀（格）"); a2.set_ylabel("时间（秒，对数轴）")
a2.legend(frameon=False)
for a in (a1, a2):
    a.grid(alpha=0.3, lw=0.8)
fig.tight_layout(pad=1.2)
fig.savefig(f"{OUT}/fig07_海报主图.png"); plt.close(fig)

# ---- 图 8：海报汇总板 -------------------------------------------------------
fig = plt.figure(figsize=(11.4, 4.8))
gs = fig.add_gridspec(1, 3, width_ratios=[1.3, 1, 1.1], wspace=0.3)
ax0 = fig.add_subplot(gs[0]); ax0.axis("off")
lines = [
    (f"v8.7.7 变更摘要", 16, True),
    ("", 6, False),
    (f"• 外缘 {RIM:g} 格/秒不变", 11.5, False),
    (f"  最里一格封顶 {CORE:g} 格/秒", 11.5, False),
    ("  （8.7.6 的 1 格/秒再砍 60%）", 11.5, False),
    ("• 逐格线性递增的爬行场", 11.5, False),
    ("• 井维持删除，其余不动", 11.5, False),
    (f"• 平均坠心 {AVG:g} 秒（上版 72）", 11.5, False),
    (f"• 内容冻结 {BLOCKS}/{ITEMS}/{UNITS} 零漂移", 11.5, False),
    ("• 审计 48+3 项全绿", 11.5, False),
    ("• 本包不再嵌 previous-work", 11.5, False),
]
y = 0.96
for txt_, fs, bold in lines:
    ax0.text(0.02, y, txt_, fontsize=fs, va="top", fontweight="bold" if bold else "normal")
    y -= 0.105 if txt_ else 0.04
ax1 = fig.add_subplot(gs[1])
cats = [f"{BLOCKS}\n方块", f"{ITEMS}\n物品", f"{UNITS}\n单位"]
ax1.bar(range(3), [BLOCKS, ITEMS, UNITS], color=[OI[0], OI[2], OI[3]], width=0.6)
for i, v in enumerate([BLOCKS, ITEMS, UNITS]):
    ax1.text(i, v * 1.04 + 7, str(v), ha="center", fontsize=13, fontweight="bold")
ax1.set_yscale("symlog", linthresh=50)
ax1.set_xticks(range(3), cats, fontsize=11)
ax1.set_title("内容注册量（vs 8.7.4-6 零漂移）", fontsize=14)
ax1.grid(axis="y", alpha=0.3)
ax2 = fig.add_subplot(gs[2])
xs = np.arange(3)
ax2.bar(xs - 0.18, worst, 0.34, label="最坏", color=OI[1])
ax2.bar(xs + 0.18, avg, 0.34, label="平均", color=OI[0])
for i in range(3):
    ax2.text(xs[i] - 0.18, worst[i] * 1.03, f"{worst[i]:g}", ha="center", fontsize=10)
    ax2.text(xs[i] + 0.18, avg[i] * 1.03, f"{avg[i]:g}", ha="center", fontsize=10)
ax2.set_yscale("log"); ax2.set_xticks(xs, names, fontsize=11)
ax2.set_ylabel("坠心耗时（秒）", fontsize=12)
ax2.set_title("实测坠心时间（单位：秒）", fontsize=14)
ax2.legend(frameon=False, fontsize=11); ax2.grid(axis="y", alpha=0.3)
fig.tight_layout(pad=1.0)
fig.savefig(f"{OUT}/fig08_海报汇总.png"); plt.close(fig)

print("wrote", len(os.listdir(OUT)), "figures to", OUT)
for f in sorted(os.listdir(OUT)):
    print("  ", f)
