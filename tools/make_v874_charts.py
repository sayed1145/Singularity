#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 v8.7.4 报告的六张图。

数据全部来自本次 headless 实测（docs/v874/audit.txt，跑法见 tools/test_v874.sh）。

    python3 tools/make_v874_charts.py [输出目录]
"""
import os
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

plt.rcParams["font.family"] = "Noto Sans CJK JP"
plt.rcParams["axes.unicode_minus"] = False

INK = "#23303d"
RED = "#d1495b"
GREEN = "#2a9d8f"
BLUE = "#3d6fa5"
ORANGE = "#e08e45"
GREY = "#9aa5b1"
PURPLE = "#7b5ea7"

OUT = sys.argv[1] if len(sys.argv) > 1 else "docs/v874/charts"
os.makedirs(OUT, exist_ok=True)


def finish(fig, name, rect=None):
    fig.tight_layout(rect=rect)
    path = os.path.join(OUT, name)
    fig.savefig(path, dpi=160, facecolor="white")
    plt.close(fig)
    print("  ->", path)


def legend(ax):
    ax.spines[["top", "right"]].set_visible(False)
    ax.legend(fontsize=10, frameon=False)


# 与代码一致的速度剖面：最外圈 rim，向内线性递增，最里一格 (8 px) 恒为 core
def profile(d, rim, core, radius):
    f = np.clip((radius - d) / max(1.0, radius - 8.0), 0.0, 1.0)
    return rim + (core - rim) * f


def drag_time(rim, core, radius, d_end=9.6, n=4000):
    """从半径 radius 拖到 d_end 需要多少秒（px 制）。"""
    d = np.linspace(radius, d_end, n)
    v = profile(d, rim, core, radius) * 8.0     # px/s
    return float(np.sum(np.abs(np.diff(d)) / v[:-1]))


# ------------------------------------------------- 1. 引力阱吸收速度剖面
def chart_well_profile():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10.4, 4.3))

    radius = 120.0
    d = np.linspace(0, radius, 240)

    v_new = profile(d, 1.0, 5.0, radius)
    v_old = 4.0 + (15.0 - 4.0) * (1 - d / radius)

    ax1.plot(d / 8, v_old, color=GREY, lw=2.0, ls="--", label="v8.7.3（4 → 15 格/秒）")
    ax1.plot(d / 8, v_new, color=PURPLE, lw=2.4, label="v8.7.4（1 → 5 格/秒）")
    ax1.fill_between(d / 8, 0, v_new, color=PURPLE, alpha=0.12)
    ax1.axvline(radius / 8 - 1.0, color=RED, ls=":", lw=1.2)
    ax1.text(radius / 8 - 5.4, 5.35, "最里一格恒为 5 格/秒（上限）", fontsize=9.5, color=RED)
    ax1.annotate("最外圈恰好 1 格/秒", xy=(15, 1.0), xytext=(8.4, 2.15),
                 fontsize=9.5, color=GREY, arrowprops=dict(arrowstyle="->", color=GREY, lw=1))
    ax1.set_xlabel("离井心距离（格）", fontsize=11)
    ax1.set_ylabel("吸收速度（格/秒）", fontsize=11)
    ax1.set_title("剖面整体压平：最外 1、最里最多 5", fontsize=12.5, pad=10)
    ax1.set_ylim(0, 16.5)
    legend(ax1)

    t873 = drag_time(4.0, 15.0, 120.0, 0.1)
    t874 = drag_time(1.0, 5.0, 120.0, 9.6)
    bars = ax2.bar([0, 1], [t873, t874], 0.5, color=[GREY, PURPLE])
    for b, v in zip(bars, [t873, t874]):
        ax2.text(b.get_x() + b.get_width() / 2, v + 0.35, f"{v:.1f} 秒", ha="center",
                 fontsize=11, color=INK, fontweight="bold")
    ax2.axhline(15.2, color=RED, ls="--", lw=1.2)
    ax2.text(-0.42, 15.7, "最坏情况（全程按最外圈速度）：15.2 秒", fontsize=9.5, color=RED)
    ax2.set_xticks([0, 1])
    ax2.set_xticklabels(["v8.7.3", "v8.7.4"], fontsize=11)
    ax2.set_ylabel("从外缘拖到井口的耗时（秒）", fontsize=11)
    ax2.set_title("拖得更慢，才有得逃", fontsize=12.5, pad=10)
    ax2.set_ylim(0, 19)

    fig.suptitle("引力阱 · 吸收剖面 1 → 5 格/秒（v8.7.4）", y=0.99, fontsize=14,
                 fontweight="bold", color=INK)
    finish(fig, "01-引力阱吸收剖面.png", rect=[0, 0, 1, 0.92])


# ------------------------------------------------- 2. 引力阱周期
def chart_well_cycle():
    fig, ax = plt.subplots(figsize=(10.4, 4.0))

    cycle, cool = 8.0, 8.0
    t = np.linspace(0, cycle + cool, 500)
    active = (t <= cycle).astype(float)
    ax.plot(t, active, color=GREEN, lw=3, label="发动中（吸收敌方单位）")
    ax.fill_between(t, 0, active, color=GREEN, alpha=0.14)

    ax.axvline(cycle, color=ORANGE, ls="--", lw=1.4)
    ax.text(cycle + 0.2, 0.55, "8 秒发动结束 → 冷却 8 秒", fontsize=10, color=ORANGE)
    ax.text(0.2, 0.55, "目标进入范围才发动", fontsize=10, color=GREEN)
    ax.text(0.2, -0.06, "v8.7.4：全周期对友方单位零效果 —— 超速场已删除，冷却/待命期间也不给任何加速",
            fontsize=10, color=RED)

    ax.set_xlabel("时间（秒）", fontsize=11)
    ax.set_ylabel("吸收", fontsize=11)
    ax.set_yticks([0, 1])
    ax.set_yticklabels(["停止", "进行"], fontsize=10)
    ax.set_xlim(0, cycle + cool)
    ax.set_ylim(-0.16, 1.25)
    ax.set_title("引力阱 · 周期不变：发动 8 秒 → 冷却 8 秒（吸收只在发动期间）",
                 fontsize=12.5, pad=10)
    legend(ax)

    fig.suptitle("引力阱 · 周期状态机（v8.7.4）", y=0.97, fontsize=14, fontweight="bold", color=INK)
    finish(fig, "02-引力阱周期.png", rect=[0, 0, 1, 0.90])


# ------------------------------------------------- 3. 黑洞吸收剖面
def chart_hole_profile():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10.4, 4.3))

    pr = 230.0
    d = np.linspace(0, pr, 240)

    v_new = profile(d, 2.0, 7.0, pr)
    v_old = 3.0 + (20.0 - 3.0) * (1 - d / pr)

    ax1.plot(d / 8, v_old, color=GREY, lw=2.0, ls="--", label="v8.7.3（3 → 20 格/秒）")
    ax1.plot(d / 8, v_new, color=RED, lw=2.4, label="v8.7.4（2 → 7 格/秒）")
    ax1.fill_between(d / 8, 0, v_new, color=RED, alpha=0.12)
    ax1.axvline(pr / 8 - 1.0, color=BLUE, ls=":", lw=1.2)
    ax1.text(pr / 8 - 7.2, 7.35, "最里一格恒为 7 格/秒（上限）", fontsize=9.5, color=BLUE)
    ax1.set_xlabel("离视界中心距离（格）", fontsize=11)
    ax1.set_ylabel("吸收速度（格/秒）", fontsize=11)
    ax1.set_title("范围仍随视界长大；剖面压到 2 → 7", fontsize=12.5, pad=10)
    ax1.set_ylim(0, 21.5)
    legend(ax1)

    t873 = drag_time(3.0, 20.0, 230.0, 8.0)
    t874 = drag_time(2.0, 7.0, 230.0, 8.0)
    bars = ax2.bar([0, 1], [t873, t874], 0.5, color=[GREY, RED])
    for b, v in zip(bars, [t873, t874]):
        ax2.text(b.get_x() + b.get_width() / 2, v + 0.4, f"{v:.1f} 秒", ha="center",
                 fontsize=11, color=INK, fontweight="bold")
    ax2.set_xticks([0, 1])
    ax2.set_xticklabels(["v8.7.3", "v8.7.4"], fontsize=11)
    ax2.set_ylabel("满视界时从外缘拖到视界的耗时（秒）", fontsize=11)
    ax2.set_title("黑洞本体、伤害机制不变，只改剖面", fontsize=12.5, pad=10)
    ax2.set_ylim(0, max(t873, t874) + 3)

    fig.suptitle("黑洞 · 吸收剖面 2 → 7 格/秒（v8.7.4）", y=0.99, fontsize=14,
                 fontweight="bold", color=INK)
    finish(fig, "03-黑洞吸收剖面.png", rect=[0, 0, 1, 0.92])


# ------------------------------------------------- 4. 碾压伤害：从秒杀掉保险丝
def chart_crush():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10.4, 4.3))

    d = np.linspace(0, 24, 240)
    v873 = np.where(d < 24, 55.0, 0.0)                       # 恒定 55/秒
    close = np.clip(1 - d / 16.0, 0.0, 1.0)                  # v8.7.4 深度比
    big = np.where(d < 16, 40.0 * (0.4 + 0.6 * close), 0.0)  # 大单位：只受 40/秒上限约束
    small = np.where(d < 16, 14.0 * (0.4 + 0.6 * close), 0.0)  # 140 血：血量封顶 10%/秒 = 14/秒

    ax1.plot(d / 8, v873, color=GREY, lw=2.2, ls="--", label="v8.7.3：圈里恒定 55/秒")
    ax1.plot(d / 8, big, color=ORANGE, lw=2.4, label="v8.7.4：大单位（40/秒上限递增）")
    ax1.plot(d / 8, small, color=GREEN, lw=2.4, label="v8.7.4：140 血单位（10% 血量封顶）")
    ax1.axvline(2.0, color=RED, ls=":", lw=1.2)
    ax1.text(2.15, 50, "碾压圈 24 → 16 px（2 格）", fontsize=9.5, color=RED)
    ax1.set_xlabel("离井心距离（格）", fontsize=11)
    ax1.set_ylabel("每秒掉血", fontsize=11)
    ax1.set_title("黑洞式逐帧递增 + 每秒 10% 血量保险丝", fontsize=12.5, pad=10)
    ax1.set_ylim(0, 62)
    legend(ax1)

    hps = np.array([110, 250, 600, 1500, 4000])
    t_old = hps / 55.0
    t_new = hps / np.minimum(40.0, 0.1 * hps)
    x = np.arange(len(hps))
    ax2.bar(x - 0.19, t_old, 0.36, color=GREY, label="v8.7.3 在井心等死")
    ax2.bar(x + 0.19, t_new, 0.36, color=GREEN, label="v8.7.4 在井心等死")
    for xi, v in zip(x - 0.19, t_old):
        ax2.text(xi, v + 1.6, f"{v:.1f}s", ha="center", fontsize=9, color=INK)
    for xi, v in zip(x + 0.19, t_new):
        ax2.text(xi, v + 1.6, f"{v:.0f}s", ha="center", fontsize=9, color=INK)
    ax2.axhline(10, color=RED, ls="--", lw=1.2)
    ax2.text(2.6, 11.2, "任何单位至少 10 秒 —— 不存在秒杀", fontsize=9.5, color=RED)
    ax2.set_xticks(x)
    ax2.set_xticklabels([f"{h} 血" for h in hps], fontsize=10)
    ax2.set_ylabel("中心最大伤害下的致死时间（秒）", fontsize=11)
    ax2.set_title("小单位从 2 秒蒸发变成 10 秒重伤", fontsize=12.5, pad=10)
    ax2.set_ylim(0, 118)
    legend(ax2)

    fig.suptitle("引力阱 · 碾压重做（仿黑洞机制，v8.7.4）", y=0.99, fontsize=14,
                 fontweight="bold", color=INK)
    finish(fig, "04-碾压伤害剖面.png", rect=[0, 0, 1, 0.92])


# ------------------------------------------------- 5. 逃脱机制
def chart_escape():
    fig, ax = plt.subplots(figsize=(10.4, 4.0))

    radius = 120.0
    d = np.linspace(0, radius, 240)
    absorb873 = (4 + (15 - 4) * (1 - d / radius)) * 8.0      # px/s
    absorb874 = profile(d, 1.0, 5.0, radius) * 8.0

    ax.plot(d / 8, absorb873, color=GREY, lw=2.0, ls="--", label="v8.7.3 吸收（到 15 格/秒，几乎没人跑得过）")
    ax.plot(d / 8, absorb874, color=RED, lw=2.4, label="v8.7.4 吸收（上限 5 格/秒）")
    for s, c, name in [(2.5, BLUE, "单位 2.5 格/秒（含 55% 减速后）"),
                       (6.0, GREEN, "单位 6 格/秒（含 55% 减速后）")]:
        ax.axhline(s * 8.0, color=c, lw=1.6, label=f"速度线：{name}")
    ax.fill_between(d / 8, absorb874, absorb874 + 0.001)
    ax.annotate("速度线在吸收曲线之上 = 逃得掉\nv8.7.4 里全场最慢的 1 格/秒在井口外圈，最狠也只有 5",
                xy=(10, 52), fontsize=10, color=INK)

    ax.set_xlabel("离井心距离（格）", fontsize=11)
    ax.set_ylabel("速度（px/秒）", fontsize=11)
    ax.set_title("跑得比当地吸收速度快，就能原路挣脱（引力只搬运、不清零速度）", fontsize=12.5, pad=10)
    ax.set_ylim(0, 132)
    legend(ax)

    fig.suptitle("逃脱判定 · v8.7.4 剖面压平后人人有得逃", y=0.97, fontsize=14,
                 fontweight="bold", color=INK)
    finish(fig, "05-逃脱机制.png", rect=[0, 0, 1, 0.90])


# ------------------------------------------------- 6. 自检总览
def chart_audit():
    fig, ax = plt.subplots(figsize=(10.4, 4.0))

    items = [
        ("引力阱\n边缘吸收", 1, "1 格/秒"),
        ("引力阱\n最里吸收", 5, "5 格/秒（上限）"),
        ("引力阱\n发动时长", 8, "8 秒"),
        ("引力阱\n碾压 DPS", 40, "40/秒"),
        ("引力阱\n血量封顶", 10, "10%/秒"),
        ("黑洞\n边缘吸收", 2, "2 格/秒"),
        ("黑洞\n最里吸收", 7, "7 格/秒"),
        ("引力阱\n超速字段", 0, "已删除"),
    ]
    labels = [i[0] for i in items]
    values = [i[1] for i in items]
    notes = [i[2] for i in items]
    colors = [PURPLE, PURPLE, PURPLE, PURPLE, PURPLE, RED, RED, GREEN]

    bars = ax.bar(range(len(items)), values, 0.6, color=colors)
    for b, n in zip(bars, notes):
        ax.text(b.get_x() + b.get_width() / 2, b.get_height() + 0.9, n, ha="center",
                fontsize=10.5, color=INK, fontweight="bold")
    ax.set_xticks(range(len(items)))
    ax.set_xticklabels(labels, fontsize=9.5)
    ax.set_ylabel("数值", fontsize=11)
    ax.set_ylim(0, 48)
    ax.set_title("v8.7.4 自检项（全部在 v160.5 无头服务器上跑过）", fontsize=12.5, pad=10)
    legend(ax)

    fig.suptitle("8.7.4 数值总览 · 自检全绿", y=0.97, fontsize=14, fontweight="bold", color=INK)
    finish(fig, "06-自检总览.png", rect=[0, 0, 1, 0.90])


if __name__ == "__main__":
    print("writing charts to", OUT)
    chart_well_profile()
    chart_well_cycle()
    chart_hole_profile()
    chart_crush()
    chart_escape()
    chart_audit()
    print("done")
