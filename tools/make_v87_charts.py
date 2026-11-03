#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 v8.7-release 报告的七张图。

数据全部来自本次 headless 实测（docs/v87/audit.txt，跑法见 tools/test_v87.sh）：

    python3 tools/make_v87_charts.py [输出目录]
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

OUT = sys.argv[1] if len(sys.argv) > 1 else "docs/v87/charts"
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


# ------------------------------------------------- 1. 矿机速度
def chart_drills():
    fig, ax = plt.subplots(figsize=(8.6, 4.4))
    names = ["共振筛选机\n(潮汐水)", "金辉螺旋钻机\n(极光冷冻液)", "棱镜采掘场\n(流明等离子)",
             "凿墙机", "流明钻机"]
    before = [3.64, 9.64, 22.86, 0.63, 1.62]
    after = [10.80, 24.68, 51.43, 1.25, 1.62]
    x = np.arange(len(names))
    w = 0.36
    ax.bar(x - w / 2, before, w, color=GREY, label="8.6-release")
    ax.bar(x + w / 2, after, w, color=GREEN, label="8.7-release")
    for i, v in enumerate(before):
        ax.text(i - w / 2, v + 0.6, f"{v:.2f}", ha="center", fontsize=10, color=GREY)
    for i, v in enumerate(after):
        ax.text(i + w / 2, v + 0.6, f"{v:.2f}", ha="center", fontsize=10, color=GREEN)
    ax.set_xticks(x)
    ax.set_xticklabels(names, fontsize=10)
    ax.set_ylabel("矿/秒", fontsize=11)
    ax.set_title("图1  挖掘速度：共振筛选机反超流明钻机，凿墙机翻倍", fontsize=13.5, pad=12)
    ax.set_ylim(0, max(after) * 1.28)
    ax.text(0.5, -0.19, "8.6 一列是「实际」速度：那三台矿机的加速液槽根本没建，液送不进来；8.7 补上储液槽后才吃得到加速液。",
            transform=ax.transAxes, ha="center", fontsize=9.5, color=GREY)
    legend(ax)
    finish(fig, "01-挖掘速度对比.png")


# ------------------------------------------------- 2. 堡垒号伤害
def chart_citadel():
    fig, ax = plt.subplots(figsize=(8.6, 4.2))
    labels = ["喷流炮\n(直伤)", "坍缩炮\n(直伤)", "坍缩炮\n(溅射)", "主炮\n(直伤)", "主炮\n(溅射)"]
    before = [30, 90, 140, 70, 55]
    after = [40, 110, 150, 80, 60]
    x = np.arange(len(labels))
    w = 0.36
    ax.bar(x - w / 2, before, w, color=GREY, label="8.6-release")
    ax.bar(x + w / 2, after, w, color=RED, label="8.7-release")
    for i, v in enumerate(before):
        ax.text(i - w / 2, v + 2, str(v), ha="center", fontsize=10, color=GREY)
    for i, v in enumerate(after):
        ax.text(i + w / 2, v + 2, str(v), ha="center", fontsize=10, color=RED)
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=10)
    ax.set_ylabel("伤害", fontsize=11)
    ax.set_title("图2  堡垒号：五处伤害全部上调", fontsize=13.5, pad=12)
    ax.set_ylim(0, max(after) * 1.2)
    legend(ax)
    finish(fig, "02-堡垒号伤害.png")


# ------------------------------------------------- 3. 流明货运站
def chart_cargo():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(9.6, 4.2))
    # 左：每种物品的上限
    labels = ["8.6\n(所有物品合计)", "8.7\n(每种物品)"]
    values = [120, 100]
    bars = ax1.bar(labels, values, 0.45, color=[GREY, GREEN])
    for b, v in zip(bars, values):
        ax1.text(b.get_x() + b.get_width() / 2, v + 3, str(v), ha="center", fontsize=12, color=INK)
    ax1.set_ylabel("单位物品上限", fontsize=11)
    ax1.set_title("储存上限", fontsize=12.5, pad=10)
    ax1.set_ylim(0, 140)
    ax1.spines[["top", "right"]].set_visible(False)

    # 右：总容量
    labels2 = ["8.6\n合计 120", "8.7\n合计 2000\n(100 × 20 种)"]
    values2 = [120, 2000]
    bars2 = ax2.bar(labels2, values2, 0.45, color=[GREY, GREEN])
    for b, v in zip(bars2, values2):
        ax2.text(b.get_x() + b.get_width() / 2, v + 50, str(v), ha="center", fontsize=12, color=INK)
    ax2.set_ylabel("站点总容量", fontsize=11)
    ax2.set_title("总容量", fontsize=12.5, pad=10)
    ax2.set_ylim(0, 2300)
    ax2.spines[["top", "right"]].set_visible(False)

    fig.suptitle("图3  流明货运站：每种物品 100，不再是所有物品抢 120 的位置", fontsize=13.5, y=0.99)
    finish(fig, "03-流明货运站容量.png", rect=[0, 0, 1, 0.92])


# ------------------------------------------------- 4. 货运站半径
def chart_radius():
    fig, ax = plt.subplots(figsize=(8.6, 4.2))
    labels = ["8.6\n固定 58 格", "8.7 默认\n58 格", "8.7 滑条\n8–400 格", "8.7 超距运输\n600 格"]
    values = [58, 58, 400, 600]
    colors = [GREY, BLUE, GREEN, ORANGE]
    bars = ax.bar(labels, values, 0.5, color=colors)
    for b, v in zip(bars, values):
        ax.text(b.get_x() + b.get_width() / 2, v + 12, str(v), ha="center", fontsize=12, color=INK)
    ax.set_ylabel("运送半径（格）", fontsize=11)
    ax.set_title("图4  运送范围：默认不变，交给玩家调", fontsize=13.5, pad=12)
    ax.set_ylim(0, 700)
    ax.spines[["top", "right"]].set_visible(False)
    finish(fig, "04-货运站半径.png")


# ------------------------------------------------- 5. 守护穹顶
def chart_ward():
    fig, ax = plt.subplots(figsize=(8.6, 4.2))
    labels = ["基础护盾", "共振碎片加成", "深库上限\n(碎片充能后)"]
    before = [7000, 2000, 7000]
    after = [7000, 5000, 12000]
    x = np.arange(len(labels))
    w = 0.36
    ax.bar(x - w / 2, before, w, color=GREY, label="8.6-release")
    ax.bar(x + w / 2, after, w, color=PURPLE, label="8.7-release")
    for i, v in enumerate(before):
        ax.text(i - w / 2, v + 200, str(v), ha="center", fontsize=10, color=GREY)
    for i, v in enumerate(after):
        ax.text(i + w / 2, v + 200, str(v), ha="center", fontsize=10, color=PURPLE)
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=11)
    ax.set_ylabel("护盾值", fontsize=11)
    ax.set_title("图5  守护穹顶：碎片把上限抬到 12000，且不再掉回 7000", fontsize=13.5, pad=12)
    ax.set_ylim(0, 14000)
    legend(ax)
    finish(fig, "05-守护穹顶护盾.png")


# ------------------------------------------------- 6. 事件视界炮射速
def chart_horizon():
    fig, ax = plt.subplots(figsize=(8.6, 4.2))
    labels = ["不装星髓石", "装星髓石\n8.6", "装星髓石\n8.7"]
    values = [1.0, 0.85, 3.0]
    colors = [GREY, RED, GREEN]
    bars = ax.bar(labels, values, 0.45, color=colors)
    for b, v in zip(bars, values):
        ax.text(b.get_x() + b.get_width() / 2, v + 0.06, f"×{v:g}", ha="center", fontsize=13, color=INK)
    ax.axhline(1.0, color=BLUE, ls="--", lw=1.2)
    ax.text(2.42, 1.05, "基准", fontsize=10, color=BLUE)
    ax.set_ylabel("射速倍率（reloadMultiplier）", fontsize=11)
    ax.set_title("图6  事件视界炮：星髓石从「变慢」变成「+200%」", fontsize=13.5, pad=12)
    ax.set_ylim(0, 3.6)
    ax.spines[["top", "right"]].set_visible(False)
    finish(fig, "06-事件视界炮射速.png")


# ------------------------------------------------- 7. 工业废料炮弹
def chart_waste():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(9.6, 4.2))
    names = ["钨弹\n(星环自有)", "巨浪合金弹\n(星环自有)", "工业废料弹\n(新增)"]
    dmg = [56, 82, 40]
    splash = [52, 75, 58]
    x = np.arange(len(names))
    w = 0.36
    ax1.bar(x - w / 2, dmg, w, color=GREY, label="直伤")
    ax1.bar(x + w / 2, splash, w, color=BLUE, label="溅射")
    for i in range(len(names)):
        ax1.text(i - w / 2, dmg[i] + 1.5, str(dmg[i]), ha="center", fontsize=10, color=INK)
        ax1.text(i + w / 2, splash[i] + 1.5, str(splash[i]), ha="center", fontsize=10, color=INK)
    ax1.set_xticks(x)
    ax1.set_xticklabels(names, fontsize=10)
    ax1.set_ylabel("伤害", fontsize=11)
    ax1.set_title("伤害：废料略低于金属弹", fontsize=12.5, pad=10)
    ax1.set_ylim(0, 100)
    legend(ax1)

    reach = [45, 45, 54]
    bars = ax2.bar(names, reach, 0.45, color=[GREY, GREY, GREEN])
    for b, v in zip(bars, reach):
        ax2.text(b.get_x() + b.get_width() / 2, v + 1, f"{v} 格", ha="center", fontsize=11, color=INK)
    ax2.set_ylabel("射程（格）", fontsize=11)
    ax2.set_title("射程：比星环自有弹远 20%", fontsize=12.5, pad=10)
    ax2.set_ylim(0, 64)
    ax2.spines[["top", "right"]].set_visible(False)

    fig.suptitle("图7  工业废料弹：裂变特效 / 更远 / 追踪 / 装填 ×1.5", fontsize=13.5, y=0.99)
    finish(fig, "07-工业废料炮弹.png", rect=[0, 0, 1, 0.92])


if __name__ == "__main__":
    print("generating v8.7 charts ->", OUT)
    chart_drills()
    chart_citadel()
    chart_cargo()
    chart_radius()
    chart_ward()
    chart_horizon()
    chart_waste()
    print("done")
