#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 v8.7.1 报告的六张图。

数据全部来自本次 headless 实测（docs/v871/audit.txt，跑法见 tools/test_v871.sh）
与编译产物（javap 反编译 buildConfiguration 的调用序列）。

    python3 tools/make_v871_charts.py [输出目录]
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

OUT = sys.argv[1] if len(sys.argv) > 1 else "docs/v871/charts"
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


# ------------------------------------------------- 1. 滑条参数顺序（崩溃根因）
def chart_slider():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(9.8, 4.3))

    # 左：Arc Table.slider 的实参顺序
    names = ["第 1 个\n(min)", "第 2 个\n(max)", "第 3 个\n(step)", "第 4 个\n(默认值)"]
    before = [58, 8, 400, 1]
    after = [8, 400, 1, 58]
    x = np.arange(len(names))
    w = 0.36
    ax1.bar(x - w / 2, before, w, color=RED, label="8.7-release（崩溃）")
    ax1.bar(x + w / 2, after, w, color=GREEN, label="8.7.1（已修）")
    for i, v in enumerate(before):
        ax1.text(i - w / 2, v + 9, str(v), ha="center", fontsize=10, color=RED)
    for i, v in enumerate(after):
        ax1.text(i + w / 2, v + 9, str(v), ha="center", fontsize=10, color=GREEN)
    ax1.set_xticks(x)
    ax1.set_xticklabels(names, fontsize=10)
    ax1.set_ylabel("传入的数值", fontsize=11)
    ax1.set_title("Arc Table.slider(min, max, step, defvalue, listener)", fontsize=12, pad=10)
    ax1.set_ylim(0, 560)
    ax1.legend(fontsize=9.5, frameon=False, loc="upper center", ncol=2)

    # 右：min 与 max 的合法性
    labels = ["8.7-release\nmin=58, max=8", "8.7.1\nmin=8, max=400"]
    ok = [0, 1]
    colors = [RED, GREEN]
    bars = ax2.bar(labels, ok, 0.45, color=colors)
    for b, v in zip(bars, ok):
        ax2.text(b.get_x() + b.get_width() / 2, v + 0.04,
                 "可打开面板" if v else "IllegalArgumentException\nmax must be > min", ha="center", fontsize=10,
                 color=INK if v else RED, fontweight="bold" if not v else "normal")
    ax2.set_ylim(0, 1.35)
    ax2.set_yticks([])
    ax2.set_title("面板能不能打开", fontsize=12, pad=10)
    ax2.spines[["top", "right", "left"]].set_visible(False)

    fig.suptitle("图1  崩溃根因：Arc 滑条的第 1 个参数是最小值，不是当前值", fontsize=13.5, y=0.99)
    fig.text(0.5, 0.015,
             "旧代码 table.slider(rangeTiles, 8f, 400f, 1f, …) 把 58 当成 min、8 当成 max，"
             "ProgressBar 构造时立刻抛异常；安卓上一点就崩，整局游戏结束。",
             ha="center", fontsize=9.5, color=GREY)
    finish(fig, "01-滑条参数顺序修复.png", rect=[0, 0.04, 1, 0.92])


# ------------------------------------------------- 2. 两种废料弹
def chart_waste():
    fig, (ax1, ax2, ax3) = plt.subplots(1, 3, figsize=(11.2, 4.2))

    # 直伤
    labels = ["星环\n(halo)", "天罗\n(sky-net)"]
    before = [40, 40]
    after = [40, 20]
    x = np.arange(len(labels))
    w = 0.36
    ax1.bar(x - w / 2, before, w, color=GREY, label="8.7-release")
    ax1.bar(x + w / 2, after, w, color=ORANGE, label="8.7.1")
    for i, v in enumerate(before):
        ax1.text(i - w / 2, v + 1, str(v), ha="center", fontsize=10, color=GREY)
    for i, v in enumerate(after):
        ax1.text(i + w / 2, v + 1, str(v), ha="center", fontsize=10, color=ORANGE)
    ax1.set_xticks(x)
    ax1.set_xticklabels(labels, fontsize=10)
    ax1.set_ylabel("直伤", fontsize=11)
    ax1.set_title("直伤", fontsize=12.5, pad=10)
    ax1.set_ylim(0, 50)
    legend(ax1)

    # 装填
    before = [1.5, 1.5]
    after = [5, 5]
    ax2.bar(x - w / 2, before, w, color=GREY, label="8.7-release")
    ax2.bar(x + w / 2, after, w, color=GREEN, label="8.7.1")
    for i, v in enumerate(before):
        ax2.text(i - w / 2, v + 0.12, f"×{v}", ha="center", fontsize=10, color=GREY)
    for i, v in enumerate(after):
        ax2.text(i + w / 2, v + 0.12, f"×{v}", ha="center", fontsize=10, color=GREEN)
    ax2.set_xticks(x)
    ax2.set_xticklabels(labels, fontsize=10)
    ax2.set_ylabel("装填速率倍率", fontsize=11)
    ax2.set_title("装填速度", fontsize=12.5, pad=10)
    ax2.set_ylim(0, 6.2)
    legend(ax2)

    # 对建筑的伤害
    before = [80, 80]
    after = [100, 100]
    ax3.bar(x - w / 2, before, w, color=GREY, label="8.7-release")
    ax3.bar(x + w / 2, after, w, color=BLUE, label="8.7.1")
    for i, v in enumerate(before):
        ax3.text(i - w / 2, v + 2, f"{v}%", ha="center", fontsize=10, color=GREY)
    for i, v in enumerate(after):
        ax3.text(i + w / 2, v + 2, f"{v}%", ha="center", fontsize=10, color=BLUE)
    ax3.set_xticks(x)
    ax3.set_xticklabels(labels, fontsize=10)
    ax3.set_ylabel("对建筑的伤害", fontsize=11)
    ax3.set_title("对建筑伤害", fontsize=12.5, pad=10)
    ax3.set_ylim(0, 125)
    legend(ax3)

    fig.suptitle("图2  工业废料弹：拆成两种，都装填 ×5，都不再衰减建筑伤害", fontsize=13.5, y=0.99)
    finish(fig, "02-工业废料弹.png", rect=[0, 0, 1, 0.92])


# ------------------------------------------------- 3. 伤害数值
def chart_damage():
    fig, ax = plt.subplots(figsize=(9.4, 4.3))
    labels = ["堡垒号\n速射炮", "流明火花", "天罗\n玻璃弹", "天罗\n玻璃弹碎片"]
    before = [40, 18, 7, 7]
    after = [45, 36, 21, 21]
    note = ["+5", "×2", "×3", "×3"]
    x = np.arange(len(labels))
    w = 0.36
    ax.bar(x - w / 2, before, w, color=GREY, label="8.7-release")
    ax.bar(x + w / 2, after, w, color=RED, label="8.7.1")
    for i, v in enumerate(before):
        ax.text(i - w / 2, v + 1, str(v), ha="center", fontsize=10, color=GREY)
    for i, v in enumerate(after):
        ax.text(i + w / 2, v + 1, f"{v}  ({note[i]})", ha="center", fontsize=10, color=RED)
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=10)
    ax.set_ylabel("单发伤害", fontsize=11)
    ax.set_title("图3  伤害：堡垒号 40→45，流明火花 ×2，钢化玻璃弹 ×3", fontsize=13.5, pad=12)
    ax.set_ylim(0, 52)
    ax.text(0.5, -0.18, "玻璃弹 = 天罗的钢化玻璃（欧雷利亚上计价为棱镜玻璃），一枚主弹带 5 片碎片，两者的伤害一起 ×3。",
            transform=ax.transAxes, ha="center", fontsize=9.5, color=GREY)
    legend(ax)
    finish(fig, "03-伤害数值.png")


# ------------------------------------------------- 4. 攻击范围
def chart_range():
    fig, ax = plt.subplots(figsize=(9.4, 4.3))
    labels = ["前哨磁轨炮\n(4×4 攻城磁轨炮)", "炽霄泰坦\n(主炮可及范围)"]
    before = [32.5, 77.2]
    after = [45, 60]
    x = np.arange(len(labels))
    w = 0.36
    ax.bar(x - w / 2, before, w, color=GREY, label="8.7-release")
    ax.bar(x + w / 2, after, w, color=BLUE, label="8.7.1")
    for i, v in enumerate(before):
        ax.text(i - w / 2, v + 1.5, f"{v} 格", ha="center", fontsize=10, color=GREY)
    for i, v in enumerate(after):
        ax.text(i + w / 2, v + 1.5, f"{v} 格", ha="center", fontsize=10, color=BLUE)
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=10)
    ax.set_ylabel("攻击范围（格）", fontsize=11)
    ax.set_title("图4  攻击范围：磁轨炮 32.5→45 格，炽霄泰坦 77.2→60 格", fontsize=13.5, pad=12)
    ax.set_ylim(0, 92)
    ax.text(0.5, -0.18, "泰坦的主炮弹寿命同时由 84 帧降到 65.4 帧（射程 621→484 px），炮管射程、单位索敌范围和面板数字三者一致。",
            transform=ax.transAxes, ha="center", fontsize=9.5, color=GREY)
    legend(ax)
    finish(fig, "04-攻击范围.png")


# ------------------------------------------------- 5. 流明加速器
def chart_accelerator():
    fig, ax = plt.subplots(figsize=(9.0, 4.1))
    labels = ["加速倍率\n(speedBoost)", "脉冲强化\n(speedBoostPhase)", "强化道具槽\n(hasBoost)",
              "面板上的\n强化数值行"]
    before = [150, 0, 0, 1]
    after = [100, 0, 0, 0]
    x = np.arange(len(labels))
    w = 0.36
    ax.bar(x - w / 2, before, w, color=GREY, label="8.7-release")
    ax.bar(x + w / 2, after, w, color=GREEN, label="8.7.1")
    for i, v in enumerate(before):
        ax.text(i - w / 2, v + 3, ("150%" if i == 0 else ("1 行" if i == 3 else "0")), ha="center", fontsize=10, color=GREY)
    for i, v in enumerate(after):
        ax.text(i + w / 2, v + 3, ("100%\n(无强化)" if i == 0 else "0"), ha="center", fontsize=10, color=GREEN)
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=10)
    ax.set_ylabel("数值", fontsize=11)
    ax.set_title("图5  流明加速器：强化彻底归零，面板不再显示强化", fontsize=13.5, pad=12)
    ax.set_ylim(0, 185)
    ax.text(0.5, -0.2, "speedBoost = 1f（倍率就是 1），并移除 Stat.speedIncrease / Stat.boostEffect 两行与强化条。",
            transform=ax.transAxes, ha="center", fontsize=9.5, color=GREY)
    legend(ax)
    finish(fig, "05-流明加速器.png")


# ------------------------------------------------- 6. 内容与自检
def chart_content():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10.2, 4.2))

    # 左：内容数量
    labels = ["方块", "物品", "单位"]
    before = [611, 45, 111]
    after = [611, 45, 110]
    x = np.arange(len(labels))
    w = 0.36
    ax1.bar(x - w / 2, before, w, color=GREY, label="8.7-release")
    ax1.bar(x + w / 2, after, w, color=RED, label="8.7.1")
    for i, v in enumerate(before):
        ax1.text(i - w / 2, v + 1.5, str(v), ha="center", fontsize=10, color=GREY)
    for i, v in enumerate(after):
        ax1.text(i + w / 2, v + 1.5, str(v), ha="center", fontsize=10, color=RED)
    ax1.set_xticks(x)
    ax1.set_xticklabels(labels, fontsize=10)
    ax1.set_ylabel("注册内容数", fontsize=11)
    ax1.set_title("内容清单", fontsize=12.5, pad=10)
    ax1.set_ylim(0, 135)
    legend(ax1)

    # 右：自检项
    checks = ["滑条边界", "加速器无强化", "两种废料弹", "伤害数值", "两处范围", "移除项"]
    values = [1] * len(checks)
    ax2.barh(checks, values, 0.55, color=GREEN)
    for i, v in enumerate(values):
        ax2.text(v + 0.03, i, "通过", va="center", fontsize=10, color=GREEN, fontweight="bold")
    ax2.set_xlim(0, 1.25)
    ax2.set_xticks([])
    ax2.set_title("启动自检（V871Audit，服务器加载即校验）", fontsize=12.5, pad=10)
    ax2.spines[["top", "right", "bottom"]].set_visible(False)
    ax2.invert_yaxis()

    fig.suptitle("图6  移除流明运输机（-1 单位），并给每一个改动加上启动自检", fontsize=13.5, y=0.99)
    finish(fig, "06-内容与自检.png", rect=[0, 0, 1, 0.92])


def main():
    chart_slider()
    chart_waste()
    chart_damage()
    chart_range()
    chart_accelerator()
    chart_content()
    print("charts written to", OUT)


if __name__ == "__main__":
    main()
