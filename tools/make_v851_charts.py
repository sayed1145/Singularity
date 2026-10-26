#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 v8.5.1-beta 报告的七张图。

数据全部来自本版本的实测（跑法见 docs/REPORT-v8.5.1-beta.md 每一节的"怎么量的"）：

    python3 tools/make_v851_charts.py [输出目录]
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

OUT = sys.argv[1] if len(sys.argv) > 1 else "docs/v851/charts"
os.makedirs(OUT, exist_ok=True)


def finish(fig, name):
    fig.tight_layout()
    path = os.path.join(OUT, name)
    fig.savefig(path, dpi=160, facecolor="white")
    plt.close(fig)
    print("  ->", path)


# ---------------------------------------------------------------- 1. 科技树
def chart_tech():
    fig, ax = plt.subplots(figsize=(8.2, 4.2))
    labels = ["被外来目标卡住\n的节点", "外来目标\n（处）", "树上内容\n缺节点", "目标指向\n不可解锁内容"]
    before = [8, 20, 0, 0]
    after = [0, 0, 0, 0]
    x = np.arange(len(labels))
    w = 0.36
    ax.bar(x - w / 2, before, w, color=RED, label="修复前（8.5-beta）")
    ax.bar(x + w / 2, after, w, color=GREEN, label="修复后（8.5.1-beta）")
    for i, v in enumerate(before):
        ax.text(i - w / 2, v + 0.4, str(v), ha="center", fontsize=11, color=INK)
        ax.text(i + w / 2, 0.4, "0", ha="center", fontsize=11, color=GREEN)
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=11)
    ax.set_ylabel("数量", fontsize=11)
    ax.set_title("图1 科技树解锁修复：视界装配厂下那批单位为什么点不开", fontsize=13.5, pad=12)
    ax.set_ylim(0, max(before) * 1.35)
    ax.legend(fontsize=10, frameon=False)
    ax.spines[["top", "right"]].set_visible(False)
    finish(fig, "01-科技树解锁修复.png")


# ---------------------------------------------------------------- 2. 炮塔弹道对齐
def chart_aim():
    """修复前：前哨磁轨炮 / 以太长枪 用"朝北时的投影屏幕距离"当世界距离，
    只在朝北对得上；转到其它朝向偏差最大 13.21 / 10.03 世界单位（1 格 = 8）。"""
    fig, ax = plt.subplots(figsize=(8.6, 4.2))
    names = ["前哨磁轨炮\n(4x4)", "以太长枪\n(3x3)", "吸积炮\n(3x3)", "弧光迫击炮\n(4x4)", "其余 16 门炮塔"]
    before = [13.21, 10.03, 0.0, 0.0, 0.0]
    after = [0.0, 0.0, 0.0, 0.0, 0.0]
    x = np.arange(len(names))
    w = 0.36
    b1 = ax.bar(x - w / 2, before, w, color=RED, label="修复前：弹丸落点 vs 画出来的炮口")
    ax.bar(x + w / 2, after, w, color=GREEN, label="修复后")
    for i, v in enumerate(before):
        if v > 0:
            ax.text(i - w / 2, v + 0.35, "%.2f\n(%.2f 格)" % (v, v / 8), ha="center", fontsize=10, color=INK)
        else:
            ax.text(i - w / 2, 0.4, "弹道已对齐", ha="center", fontsize=9, color=GREY)
        ax.text(i + w / 2, 0.4, "0.00", ha="center", fontsize=10, color=GREEN)
    ax.set_xticks(x)
    ax.set_xticklabels(names, fontsize=10.5)
    ax.set_ylabel("偏差（世界单位，1 格 = 8）", fontsize=11)
    ax.set_title("图2 炮塔弹道对齐（16 个朝向逐一算，取最大偏差）", fontsize=13.5, pad=12)
    ax.set_ylim(0, max(before) * 1.55)
    ax.legend(fontsize=10, frameon=False)
    ax.spines[["top", "right"]].set_visible(False)
    finish(fig, "02-炮塔弹道对齐.png")


# ---------------------------------------------------------------- 3. 开火特效
def chart_flash():
    """修复前：枪口火光/硝烟留在"未抬升"的地面点，弹丸被抬到透视后的炮口，两者错开一个抬升量。"""
    data = [
        ("弧光迫击炮", 12.66), ("陨坑炮", 10.49), ("风暴线圈", 6.62), ("事件视界炮", 6.36),
        ("类星长枪", 5.68), ("织机", 5.64), ("棱镜塔", 5.63), ("深井", 5.29),
        ("天网", 4.96), ("霍金辐射器", 4.86), ("吸积炮", 4.63), ("棱镜长枪", 4.46),
        ("光环", 4.19), ("碎片连射炮", 3.47), ("框架牵引器", 3.39), ("钻线炮", 3.38),
        ("针落", 3.35), ("流明火花", 3.26),
    ]
    data.sort(key=lambda t: -t[1])
    names = [d[0] for d in data]
    vals = [d[1] for d in data]
    fig, ax = plt.subplots(figsize=(9.4, 4.6))
    x = np.arange(len(names))
    ax.bar(x, vals, color=ORANGE, label="修复前：火光与弹丸的距离")
    ax.bar(x, [0] * len(names), color=GREEN, label="修复后：0")
    for i, v in enumerate(vals):
        ax.text(i, v + 0.25, "%.1f" % v, ha="center", fontsize=9, color=INK)
    ax.set_xticks(x)
    ax.set_xticklabels(names, rotation=38, ha="right", fontsize=9.5)
    ax.set_ylabel("偏差（世界单位，1 格 = 8）", fontsize=11)
    ax.set_title("图3 开火特效对齐：枪口火光原来比弹丸低半个多格子", fontsize=13.5, pad=12)
    ax.set_ylim(0, max(vals) * 1.25)
    ax.legend(fontsize=10, frameon=False)
    ax.spines[["top", "right"]].set_visible(False)
    finish(fig, "03-开火特效对齐.png")


# ---------------------------------------------------------------- 4. 传送带 / 管道
def chart_belts():
    fig, ax = plt.subplots(figsize=(7.6, 4.2))
    labels = ["流明/通量传送带\n（2 族 x 5 型 x 4 边）", "管道 top\n（2 族 x 5 型 x 4 边）", "管道 bottom\n（2 族 x 5 型 x 4 边）"]
    before = [34, 36, 32]
    total = [40, 40, 40]
    after = [0, 0, 0]
    x = np.arange(len(labels))
    w = 0.36
    ax.bar(x - w / 2, before, w, color=RED, label="修复前：和原版不一致的边")
    ax.bar(x - w / 2, [t - b for t, b in zip(total, before)], w, bottom=before, color=GREY,
           label="修复前：已经一致的边")
    ax.bar(x + w / 2, total, w, color=GREEN, label="修复后：全部 40 条边一致")
    for i, (b, t) in enumerate(zip(before, total)):
        ax.text(i - w / 2, b + 0.8, "%d" % b, ha="center", fontsize=11, color=INK)
        ax.text(i + w / 2, t + 0.8, "全部一致", ha="center", fontsize=10, color=GREEN)
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=10.5)
    ax.set_ylabel("边数", fontsize=11)
    ax.set_title("图4 传送带 / 管道：格子的四条边能不能和邻格严丝合缝", fontsize=13.5, pad=12)
    ax.set_ylim(0, 52)
    ax.legend(fontsize=9.5, frameon=False, loc="upper left")
    ax.spines[["top", "right"]].set_visible(False)
    finish(fig, "04-传送带管道边缘对接.png")


# ---------------------------------------------------------------- 5. 刷怪
def chart_spawn():
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10.4, 4.2), gridspec_kw={"width_ratios": [1.15, 1]})
    # 左：每张图能刷出的单位种类
    labels = ["修复前", "修复后\n（普通图）", "修复后\n（高威胁图）"]
    vals = [5, 28, 29]
    bars = ax1.bar(labels, vals, color=[RED, GREEN, BLUE])
    for b, v in zip(bars, vals):
        ax1.text(b.get_x() + b.get_width() / 2, v + 0.6, "%d 种" % v, ha="center", fontsize=11, color=INK)
    ax1.set_ylabel("一张图能刷出的单位种类", fontsize=11)
    ax1.set_ylim(0, 34)
    ax1.set_title("每张图的单位覆盖", fontsize=12.5, pad=10)
    ax1.spines[["top", "right"]].set_visible(False)

    # 右：32 张战役图的拘留者分布
    detainer = 15
    ax2.pie([detainer, 32 - detainer],
            labels=["刷拘留者\n%d 张（每图 2 只）" % detainer, "不刷\n%d 张" % (32 - detainer)],
            colors=[ORANGE, GREY], autopct="%1.0f%%", startangle=90,
            textprops={"fontsize": 10.5})
    ax2.set_title("拘留者在 32 张战役图里的分布", fontsize=12.5, pad=10)
    finish(fig, "05-刷怪单位覆盖.png")


# ---------------------------------------------------------------- 6. 矿机动画时钟
def chart_clock():
    """实测：空地矿机时钟恒为 0；有矿有电的矿机时钟一路涨；塞满出口后时钟冻结在 333.017。"""
    t = np.arange(0, 25, 1.0)
    idle = np.zeros_like(t)                       # 没矿 / 没电
    work = np.where(t < 12, t * 30.0, 333.017)    # 12 秒后塞满，时钟停住
    work[:12] = t[:12] * 30.0
    fig, ax = plt.subplots(figsize=(8.4, 4.2))
    ax.plot(t, idle, color=GREY, lw=2.4, label="空地 / 没电 / 被禁用：时钟恒为 0（钻头不转）")
    ax.plot(t, work, color=BLUE, lw=2.6, label="有矿有电：时钟随产出前进")
    ax.axvline(12, color=RED, ls="--", lw=1.6)
    ax.annotate("在这里把出口塞满\n时钟冻结在 333.017", xy=(12, 333), xytext=(13.4, 250),
                fontsize=10.5, color=RED,
                arrowprops=dict(arrowstyle="->", color=RED, lw=1.4))
    ax.set_xlabel("时间（秒，服务端实测）", fontsize=11)
    ax.set_ylabel("模型转动时钟（累计值）", fontsize=11)
    ax.set_title("图6 矿机动画门控：不挖矿的时候钻头不再空转", fontsize=13.5, pad=12)
    ax.legend(fontsize=10, frameon=False, loc="upper left")
    ax.spines[["top", "right"]].set_visible(False)
    finish(fig, "06-矿机动画门控.png")


# ---------------------------------------------------------------- 7. 新矿物
def chart_ore():
    """6 张真实生成的战役图里，五种矿的覆盖层格数。"""
    sectors = ["10\n280²", "83\n284²", "30\n300²", "52\n296²", "72\n328²", "65\n336²"]
    aurite = [2214, 2363, 2036, 2130, 1503, 1951]
    lumenite = [2143, 2025, 2013, 2145, 1509, 1515]
    resonance = [1948, 1584, 1873, 1574, 1049, 1245]
    waste = [366, 306, 420, 328, 278, 294]
    marrow = [236, 179, 162, 220, 167, 184]
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(10.6, 4.3), gridspec_kw={"width_ratios": [1.35, 1]})
    x = np.arange(len(sectors))
    w = 0.26
    ax1.bar(x - w, aurite, w, color="#d9b36a", label="金辉矿")
    ax1.bar(x, lumenite, w, color="#91e8ff", label="流明矿")
    ax1.bar(x + w, resonance, w, color="#b69cff", label="共振碎片")
    ax1.set_xticks(x)
    ax1.set_xticklabels(sectors, fontsize=10)
    ax1.set_title("三种既有矿（格）", fontsize=12, pad=8)
    ax1.legend(fontsize=9.5, frameon=False)
    ax1.spines[["top", "right"]].set_visible(False)

    ax2.bar(x - w / 2, waste, w, color="#6b6a74", label="废料（新，基础矿物）")
    ax2.bar(x + w / 2, marrow, w, color="#fff4e2", edgecolor="#2b2622", label="星髓石（新，高级矿物）")
    ax2.set_xticks(x)
    ax2.set_xticklabels(sectors, fontsize=10)
    ax2.set_title("两种新矿（格）", fontsize=12, pad=8)
    ax2.legend(fontsize=9.5, frameon=False)
    ax2.spines[["top", "right"]].set_visible(False)
    fig.suptitle("图7 新矿物在真实生成的战役图里的矿脉规模", fontsize=13.5)
    finish(fig, "07-新矿物矿脉分布.png")


def main():
    print("生成 v8.5.1-beta 报告图 ->", OUT)
    chart_tech()
    chart_aim()
    chart_flash()
    chart_belts()
    chart_spawn()
    chart_clock()
    chart_ore()
    print("完成")


if __name__ == "__main__":
    main()
