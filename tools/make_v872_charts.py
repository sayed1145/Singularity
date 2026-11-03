#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 v8.7.2 报告的六张图。

数据全部来自本次 headless 实测（docs/v872/audit.txt，跑法见 tools/test_v872.sh）
与编译产物（javap 反编译 build/classes）。

    python3 tools/make_v872_charts.py [输出目录]
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

OUT = sys.argv[1] if len(sys.argv) > 1 else "docs/v872/charts"
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


# ------------------------------------------------- 1. 废料装填：语义修正
def chart_waste():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10.4, 4.3))

    # 左：装填倍数（一个废料 = 几发）
    labels = ["8.7.1", "8.7.2"]
    values = [6, 5]
    bars = ax1.bar(labels, values, 0.45, color=[GREY, GREEN])
    for b, v in zip(bars, values):
        ax1.text(b.get_x() + b.get_width() / 2, v + 0.12, f"×{v}",
                 ha="center", fontsize=11, color=INK, fontweight="bold")
    ax1.set_ylabel("装填倍数 ammoMultiplier", fontsize=11)
    ax1.set_title("装填倍数：一份废料 = 五发", fontsize=12.5, pad=10)
    ax1.set_ylim(0, 7.4)
    ax1.text(0.5, -0.2, "面板上的“装填倍数”对应 ammoMultiplier：一个废料能装五发。",
             transform=ax1.transAxes, ha="center", fontsize=9.5, color=GREY)
    ax1.spines[["top", "right"]].set_visible(False)

    # 右：开火速率倍率（必须回到 1）
    labels = ["8.7.1\n（误解）", "8.7.2\n（修正）"]
    values = [5, 1]
    bars = ax2.bar(labels, values, 0.45, color=[RED, BLUE])
    for b, v in zip(bars, values):
        ax2.text(b.get_x() + b.get_width() / 2, v + 0.12, f"×{v}",
                 ha="center", fontsize=11, color=INK, fontweight="bold")
    ax2.set_ylabel("开火速率倍率 reloadMultiplier", fontsize=11)
    ax2.set_title("开火速率：废料不加任何射速", fontsize=12.5, pad=10)
    ax2.set_ylim(0, 6.2)
    ax2.text(0.5, -0.2, "面板上的“开火速率”对应 reloadMultiplier：8.7.1 误把它当成“装填×5”，"
                         "8.7.2 归 1。", transform=ax2.transAxes, ha="center", fontsize=9.5, color=GREY)
    ax2.spines[["top", "right"]].set_visible(False)

    fig.suptitle("图1  工业废料：装填 ×5 真的实现了，且不再偷偷加快射速", fontsize=13.5, y=0.99)
    finish(fig, "01-废料装填语义.png", rect=[0, 0.05, 1, 0.92])


# ------------------------------------------------- 2. 天罗流明矿弹
def chart_sky_net():
    fig, ax = plt.subplots(figsize=(9.4, 4.3))
    labels = ["天罗\n流明矿弹（lead）", "天罗\n钢化玻璃弹", "天罗\n玻璃弹碎片"]
    before = [5, 21, 21]
    after = [13, 21, 21]
    note = ["+8", "不变", "不变"]
    x = np.arange(len(labels))
    w = 0.36
    ax.bar(x - w / 2, before, w, color=GREY, label="8.7.1")
    ax.bar(x + w / 2, after, w, color=RED, label="8.7.2")
    for i, v in enumerate(before):
        ax.text(i - w / 2, v + 0.6, str(v), ha="center", fontsize=10, color=GREY)
    for i, v in enumerate(after):
        ax.text(i + w / 2, v + 0.6, f"{v}  ({note[i]})", ha="center", fontsize=10, color=RED)
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=10)
    ax.set_ylabel("单发伤害", fontsize=11)
    ax.set_title("图2  天罗流明矿弹：基础伤害 5 → 13", fontsize=13.5, pad=12)
    ax.set_ylim(0, 28)
    ax.text(0.5, -0.18, "欧雷利亚上铅被折算成流明矿，所以天罗的 lead 弹就是流明矿弹；"
                        "钢化玻璃弹与其碎片维持 8.7.1 的 21。",
            transform=ax.transAxes, ha="center", fontsize=9.5, color=GREY)
    legend(ax)
    finish(fig, "02-天罗流明矿弹.png")


# ------------------------------------------------- 3. 流明加速器
def chart_accelerator():
    fig, ax = plt.subplots(figsize=(9.2, 4.2))
    labels = ["超速倍率\n(speedBoost)", "脉冲强化\n(speedBoostPhase)", "强化道具槽\n(hasBoost)",
              "面板超速行\n(speedIncrease)", "面板总倍率行\n(boostEffect)"]
    before = [1.0, 0.0, 0.0, 0.0, 0.0]
    after = [1.5, 0.0, 0.0, 1.0, 1.0]
    text_before = ["×1（无超速）", "0", "0", "无", "无"]
    text_after = ["×1.5（+50%）", "0", "0", "+50%", "150%"]
    x = np.arange(len(labels))
    w = 0.36
    ax.bar(x - w / 2, before, w, color=GREY, label="8.7.1")
    ax.bar(x + w / 2, after, w, color=GREEN, label="8.7.2")
    for i, v in enumerate(before):
        ax.text(i - w / 2, v + 0.04, text_before[i], ha="center", fontsize=9.5, color=GREY)
    for i, v in enumerate(after):
        ax.text(i + w / 2, v + 0.04, text_after[i], ha="center", fontsize=9.5, color=GREEN)
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=9.5)
    ax.set_ylabel("数值", fontsize=11)
    ax.set_title("图3  流明加速器：恢复“超速”，但仍然不可强化", fontsize=13.5, pad=12)
    ax.set_ylim(0, 1.95)
    ax.text(0.5, -0.2, "speedBoost 回到原版的 1.5（周围建筑 +50%）；没有强化道具槽、没有脉冲强化，"
                        "面板显示的就是真实的 +50% 与 150%。",
            transform=ax.transAxes, ha="center", fontsize=9.5, color=GREY)
    legend(ax)
    finish(fig, "03-流明加速器.png")


# ------------------------------------------------- 4. 炽霄泰坦机身特效回退
def chart_titan():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10.2, 4.2))

    # 左：机身特效层
    labels = ["8.7 / 8.7.1", "8.7.2"]
    values = [1, 0]
    bars = ax1.bar(labels, values, 0.42, color=[RED, BLUE])
    for b, v in zip(bars, values):
        ax1.text(b.get_x() + b.get_width() / 2, v + 0.03,
                 "热浪鳍 + 余烬光环 + 熔核脉冲" if v else "无代码机身特效（裸 Walker3D）",
                 ha="center", fontsize=10, color=INK)
    ax1.set_ylabel("机身特效层数量", fontsize=11)
    ax1.set_title("机身周围特效", fontsize=12.5, pad=10)
    ax1.set_ylim(0, 1.45)
    ax1.set_yticks([0, 1])
    ax1.spines[["top", "right"]].set_visible(False)

    # 右：攻击特效
    labels = ["8.7 / 8.7.1", "8.7.2"]
    values = [1, 1]
    bars = ax2.bar(labels, values, 0.42, color=[GREEN, GREEN])
    for b, v in zip(bars, values):
        ax2.text(b.get_x() + b.get_width() / 2, v + 0.03, "6.1 LegacyEmber（逐字不变）",
                 ha="center", fontsize=10, color=INK)
    ax2.set_ylabel("攻击特效是否为 6.1 版", fontsize=11)
    ax2.set_title("攻击特效", fontsize=12.5, pad=10)
    ax2.set_ylim(0, 1.45)
    ax2.set_yticks([0, 1])
    ax2.spines[["top", "right"]].set_visible(False)

    fig.suptitle("图4  炽霄泰坦：机身特效回退到最初版本，攻击特效一个字都没动", fontsize=13.5, y=0.99)
    fig.text(0.5, 0.015,
             "8.7 新增的 PyroclastFx（热浪鳍 / 余烬光环 / 熔核脉冲）整层删除；"
             "主炮命中与破片命中仍是 6.1 的 legacyBurst / legacyVent。",
             ha="center", fontsize=9.5, color=GREY)
    finish(fig, "04-炽霄泰坦机身回退.png", rect=[0, 0.05, 1, 0.92])


# ------------------------------------------------- 5. 黑洞引力场
def chart_blackhole():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10.4, 4.3))

    # 左：引力范围
    labels = ["8.7.1\n冲量式（不束缚）", "8.7.2\n真束缚（直接搬运）"]
    values = [0, 230]
    bars = ax1.bar(labels, values, 0.45, color=[GREY, PURPLE])
    for b, v in zip(bars, values):
        ax1.text(b.get_x() + b.get_width() / 2, v + 6, "只推一下" if v == 0 else "230 px ≈ 29 格",
                 ha="center", fontsize=10, color=INK)
    ax1.set_ylabel("引力范围（px）", fontsize=11)
    ax1.set_title("黑洞的引力场范围", fontsize=12.5, pad=10)
    ax1.set_ylim(0, 275)
    ax1.spines[["top", "right"]].set_visible(False)

    # 右：对建筑的伤害
    labels = ["8.7.1", "8.7.2"]
    values = [0, 45]
    bars = ax2.bar(labels, values, 0.42, color=[GREY, RED])
    for b, v in zip(bars, values):
        ax2.text(b.get_x() + b.get_width() / 2, v + 1.5, "不碰建筑" if v == 0 else "45 / 秒（随体积缩放）",
                 ha="center", fontsize=10, color=INK)
    ax2.set_ylabel("对建筑的伤害（每秒）", fontsize=11)
    ax2.set_title("黑洞的范围攻击", fontsize=12.5, pad=10)
    ax2.set_ylim(0, 56)
    ax2.spines[["top", "right"]].set_visible(False)

    fig.suptitle("图5  黑洞：真引力场 + 范围攻击（照 wither-storm 的束缚方式）", fontsize=13.5, y=0.99)
    fig.text(0.5, 0.015,
             "不再用 impulse()（单位会自己挣开），而是每帧直接搬运单位、抬起越过障碍（elevation = 1），"
             "并把速度清零 —— 见 src/blackhole/GravityField.java。",
             ha="center", fontsize=9.5, color=GREY)
    finish(fig, "05-黑洞引力场.png", rect=[0, 0.05, 1, 0.92])


# ------------------------------------------------- 6. 引力阱范围 + 自检
def chart_well_and_audit():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10.6, 4.2))

    # 左：引力阱范围
    labels = ["8.7.1", "8.7.2"]
    values = [14, 15]
    bars = ax1.bar(labels, values, 0.42, color=[GREY, BLUE])
    for b, v in zip(bars, values):
        ax1.text(b.get_x() + b.get_width() / 2, v + 0.35, f"{v} 格（{v * 8} px）",
                 ha="center", fontsize=10, color=INK)
    ax1.set_ylabel("作用范围（格）", fontsize=11)
    ax1.set_title("引力阱范围", fontsize=12.5, pad=10)
    ax1.set_ylim(0, 18.5)
    ax1.text(0.5, -0.22, "只束缚单位，绝不作用于建筑；8.7.1 的冲量式牵引几乎拉不动任何东西。",
             transform=ax1.transAxes, ha="center", fontsize=9.5, color=GREY)
    ax1.spines[["top", "right"]].set_visible(False)

    # 右：启动自检项
    checks = ["滑条边界（8.7.1）", "加速器 +50% 不可强化", "废料装填 ×5 / 无射速",
              "天罗流明矿弹 13", "黑洞引力场 + 建筑伤害", "引力阱 15 格（仅单位）", "泰坦机身回退"]
    values = [1] * len(checks)
    ax2.barh(checks, values, 0.55, color=GREEN)
    for i, v in enumerate(values):
        ax2.text(v + 0.03, i, "通过", va="center", fontsize=10, color=GREEN, fontweight="bold")
    ax2.set_xlim(0, 1.3)
    ax2.set_xticks([])
    ax2.set_title("启动自检（V872Audit，服务器加载即校验）", fontsize=12.5, pad=10)
    ax2.spines[["top", "right", "bottom"]].set_visible(False)
    ax2.invert_yaxis()

    fig.suptitle("图6  引力阱 14 → 15 格，并且给 8.7.2 的每一处改动加上启动自检", fontsize=13.5, y=0.99)
    finish(fig, "06-引力阱与自检.png", rect=[0, 0.04, 1, 0.92])


def main():
    chart_waste()
    chart_sky_net()
    chart_accelerator()
    chart_titan()
    chart_blackhole()
    chart_well_and_audit()
    print("charts written to", OUT)


if __name__ == "__main__":
    main()
