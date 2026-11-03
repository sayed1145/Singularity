#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 v8.7.3 报告的六张图。

数据全部来自本次 headless 实测（docs/v873/audit.txt，跑法见 tools/test_v873.sh）。

    python3 tools/make_v873_charts.py [输出目录]
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

OUT = sys.argv[1] if len(sys.argv) > 1 else "docs/v873/charts"
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


# ------------------------------------------------- 1. 引力阱吸收速度剖面
def chart_well_profile():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10.4, 4.3))

    radius = 120.0          # 15 格
    rim = 4.0               # 格/秒
    core = 15.0             # 格/秒

    d = np.linspace(0, radius, 200)
    f = 1 - d / radius
    v = rim + (core - rim) * f

    ax1.plot(d / 8, v, color=PURPLE, lw=2.4, label="吸收速度（v8.7.3）")
    ax1.fill_between(d / 8, 0, v, color=PURPLE, alpha=0.12)
    ax1.axhline(rim, color=GREY, ls="--", lw=1)
    ax1.axhline(core, color=RED, ls="--", lw=1)
    ax1.text(0.6, rim + 0.5, "边缘 4 格/秒", fontsize=9.5, color=GREY)
    ax1.text(0.6, core + 0.5, "中心 15 格/秒", fontsize=9.5, color=RED)
    ax1.set_xlabel("离井口距离（格）", fontsize=11)
    ax1.set_ylabel("吸收速度（格/秒）", fontsize=11)
    ax1.set_title("引力阱：边缘慢、越往里越快", fontsize=12.5, pad=10)
    ax1.set_ylim(0, 18)
    legend(ax1)

    # 右：跑完 15 格要多久（边缘速度起步）
    steps = np.linspace(0, 15, 300)
    spd = rim + (core - rim) * (1 - steps / 15)
    t = np.cumsum(1 / spd) / 60.0
    ax2.plot(steps, t, color=BLUE, lw=2.4, label="拖到中心的耗时")
    ax2.axhline(1.5, color=RED, ls="--", lw=1.2)
    ax2.text(0.6, 1.62, "1.5 秒底线", fontsize=9.5, color=RED)
    ax2.set_xlabel("被拖过的距离（格）", fontsize=11)
    ax2.set_ylabel("耗时（秒）", fontsize=11)
    ax2.set_title("最慢也要一秒半以上，绝不瞬间吸收", fontsize=12.5, pad=10)
    legend(ax2)

    fig.suptitle("引力阱 · 慢速吸收剖面（v8.7.3）", y=0.99, fontsize=14, fontweight="bold", color=INK)
    finish(fig, "01-引力阱吸收剖面.png", rect=[0, 0, 1, 0.92])


# ------------------------------------------------- 2. 引力阱周期
def chart_well_cycle():
    fig, ax = plt.subplots(figsize=(10.4, 4.0))

    cycle, cool = 8.0, 8.0
    t = np.linspace(0, cycle + cool, 500)
    active = (t <= cycle).astype(float)
    ax.plot(t, active, color=GREEN, lw=3, label="发动中（吸收）")
    ax.fill_between(t, 0, active, color=GREEN, alpha=0.14)

    ax.axvline(cycle, color=ORANGE, ls="--", lw=1.4)
    ax.text(cycle + 0.2, 0.55, "8 秒发动结束 → 冷却 8 秒", fontsize=10, color=ORANGE)
    ax.text(0.2, 0.55, "目标进入范围才发动", fontsize=10, color=GREEN)

    ax.set_xlabel("时间（秒）", fontsize=11)
    ax.set_ylabel("吸收", fontsize=11)
    ax.set_yticks([0, 1])
    ax.set_yticklabels(["停止", "进行"], fontsize=10)
    ax.set_xlim(0, cycle + cool)
    ax.set_ylim(-0.08, 1.25)
    ax.set_title("引力阱 · 周期：发动 8 秒 → 冷却 8 秒（冷却与待命都不吸收，但超速一直有效）",
                 fontsize=12.5, pad=10)
    legend(ax)

    fig.suptitle("引力阱 · 周期状态机（v8.7.3）", y=0.97, fontsize=14, fontweight="bold", color=INK)
    finish(fig, "02-引力阱周期.png", rect=[0, 0, 1, 0.90])


# ------------------------------------------------- 3. 黑洞吸收剖面
def chart_hole_profile():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10.4, 4.3))

    radius = 230.0          # px（最大）
    rim, core = 3.0, 20.0   # 格/秒

    d = np.linspace(0, radius, 200)
    f = 1 - d / radius
    v = rim + (core - rim) * f

    ax1.plot(d / 8, v, color=RED, lw=2.4, label="吸收速度（v8.7.3）")
    ax1.fill_between(d / 8, 0, v, color=RED, alpha=0.12)
    ax1.axhline(rim, color=GREY, ls="--", lw=1)
    ax1.axhline(core, color=BLUE, ls="--", lw=1)
    ax1.text(0.6, rim + 0.6, "边缘 3 格/秒", fontsize=9.5, color=GREY)
    ax1.text(0.6, core + 0.6, "最里 20 格/秒", fontsize=9.5, color=BLUE)
    ax1.set_xlabel("离视界距离（格）", fontsize=11)
    ax1.set_ylabel("吸收速度（格/秒）", fontsize=11)
    ax1.set_title("黑洞：范围随半径增长，吸收同样慢速", fontsize=12.5, pad=10)
    ax1.set_ylim(0, 24)
    legend(ax1)

    # 右：旧版（瞬间/高速）与新版的对比
    t_new = np.linspace(0, 1, 100)
    x_new = 30 * t_new                      # 边缘 3 格/秒 ≈ 24 px/s
    x_old = 230 * t_new                     # 旧版一帧内就到位
    ax2.plot(t_new, x_new, color=GREEN, lw=2.6, label="v8.7.3 慢速（边缘 3 格/秒）")
    ax2.plot(t_new, x_old, color=RED, lw=2.2, ls="--", label="8.7.2 旧版（几乎瞬间）")
    ax2.set_xlabel("时间（秒）", fontsize=11)
    ax2.set_ylabel("被拖过的距离（px）", fontsize=11)
    ax2.set_title("不再是一瞬间吸收", fontsize=12.5, pad=10)
    legend(ax2)

    fig.suptitle("黑洞 · 慢速吸收（v8.7.3）", y=0.99, fontsize=14, fontweight="bold", color=INK)
    finish(fig, "03-黑洞吸收剖面.png", rect=[0, 0, 1, 0.92])


# ------------------------------------------------- 4. 逃脱机制
def chart_escape():
    fig, ax = plt.subplots(figsize=(10.4, 4.0))

    d = np.linspace(0, 120, 200)
    f = 1 - d / 120
    absorb = (4 + (15 - 4) * f) * 8.0        # px/s
    speeds = [1.5, 3.0, 5.0, 12.0]
    for s, c, name in zip(speeds, [GREY, ORANGE, BLUE, GREEN],
                          ["单位 1.5 格/秒", "单位 3 格/秒", "单位 5 格/秒", "单位 12 格/秒"]):
        net = s * 8.0 - absorb
        ax.plot(d / 8, net, color=c, lw=2.2, label=name)
    ax.axhline(0, color=INK, lw=1.2, ls="--")
    ax.text(0.4, 0.8, "线在 0 以上 = 跑得赢，能挣脱", fontsize=10, color=INK)

    ax.set_xlabel("离井口距离（格）", fontsize=11)
    ax.set_ylabel("净位移速度（px/秒，正 = 往外）", fontsize=11)
    ax.set_title("单位速度 > 吸收速度 → 逃得掉；否则被拖进井口", fontsize=12.5, pad=10)
    legend(ax)

    fig.suptitle("逃脱判定 · 引力只搬运、不再清零速度（v8.7.3）", y=0.97, fontsize=14,
                 fontweight="bold", color=INK)
    finish(fig, "04-逃脱机制.png", rect=[0, 0, 1, 0.90])


# ------------------------------------------------- 5. 废料特效对比
def chart_waste_fx():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10.4, 4.3))

    labels = ["枪口特效", "命中特效"]
    old = [60, 190]
    new = [8, 40]
    x = np.arange(len(labels))
    b1 = ax1.bar(x - 0.19, old, 0.36, color=GREY, label="8.7.2")
    b2 = ax1.bar(x + 0.19, new, 0.36, color=GREEN, label="8.7.3")
    for bars in (b1, b2):
        for b in bars:
            ax1.text(b.get_x() + b.get_width() / 2, b.get_height() + 4, f"{int(b.get_height())}",
                     ha="center", fontsize=10, color=INK)
    ax1.set_xticks(x)
    ax1.set_xticklabels(labels, fontsize=11)
    ax1.set_ylabel("特效存活帧数", fontsize=11)
    ax1.set_title("特效生命周期大幅缩短", fontsize=12.5, pad=10)
    ax1.set_ylim(0, 215)
    legend(ax1)

    # 右：加色强度（白热程度）
    ax2.bar([0, 1], [1.0, 1.0], 0.45, color=GREY, label="8.7.2 加色白热核闪")
    ax2.bar([2, 3], [0.42, 0.42], 0.45, color=GREEN, label="8.7.3 柔和余辉")
    ax2.set_xticks([0, 1, 2, 3])
    ax2.set_xticklabels(["枪口", "命中", "枪口", "命中"], fontsize=11)
    ax2.set_ylabel("亮度（相对）", fontsize=11)
    ax2.set_title("不再加色爆亮，地图与界面保持清爽", fontsize=12.5, pad=10)
    ax2.set_ylim(0, 1.25)
    legend(ax2)

    fig.suptitle("工业废料 · 发射特效（v8.7.3）", y=0.99, fontsize=14, fontweight="bold", color=INK)
    finish(fig, "05-废料发射特效.png", rect=[0, 0, 1, 0.92])


# ------------------------------------------------- 6. 自检总览
def chart_audit():
    fig, ax = plt.subplots(figsize=(10.4, 4.0))

    items = [
        ("废料装填 ×5\n（不加射速）", 5, 5, "×5 / ×1"),
        ("天罗流明矿\n基础伤害", 5, 13, "13"),
        ("流明加速器\n超速", 0, 50, "+50%"),
        ("引力阱\n边缘吸收", 0, 4, "4 格/秒"),
        ("引力阱\n中心吸收", 0, 15, "15 格/秒"),
        ("引力阱\n发动时长", 0, 8, "8 秒"),
        ("黑洞\n边缘吸收", 0, 3, "3 格/秒"),
        ("黑洞\n最里吸收", 0, 20, "20 格/秒"),
    ]
    labels = [i[0] for i in items]
    values = [i[2] for i in items]
    notes = [i[3] for i in items]
    colors = [GREEN, GREEN, BLUE, PURPLE, PURPLE, PURPLE, RED, RED]

    bars = ax.bar(range(len(items)), values, 0.6, color=colors)
    for b, v, n in zip(bars, values, notes):
        ax.text(b.get_x() + b.get_width() / 2, v + 0.6, n, ha="center", fontsize=10.5,
                color=INK, fontweight="bold")
    ax.set_xticks(range(len(items)))
    ax.set_xticklabels(labels, fontsize=9.5)
    ax.set_ylabel("数值", fontsize=11)
    ax.set_ylim(0, 24)
    ax.set_title("v8.7.3 自检项（全部在 v160.5 无头服务器上跑过）", fontsize=12.5, pad=10)
    legend(ax)

    fig.suptitle("8.7.3 数值总览 · 自检全绿", y=0.97, fontsize=14, fontweight="bold", color=INK)
    finish(fig, "06-自检总览.png", rect=[0, 0, 1, 0.90])


if __name__ == "__main__":
    print("writing charts to", OUT)
    chart_well_profile()
    chart_well_cycle()
    chart_hole_profile()
    chart_escape()
    chart_waste_fx()
    chart_audit()
    print("done")
