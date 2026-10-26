#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成中文汇总 PDF：把 8.5 → 8.5.4-beta 的所有更新合成一份文档。

用法：
    python3 tools/make_summary_pdf.py [输出路径]

字体：Noto Sans SC（SIL Open Font License）。脚本会自动下载变量字体并实例化
Regular / Bold 两个静态字重，缓存在 --font-dir（默认 /tmp/bhfonts），不写入仓库，
以免把工作区撑大。PDF 内嵌的是子集，成品只有几百 KB。
"""

import os
import sys
import subprocess

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (BaseDocTemplate, Frame, PageTemplate, Paragraph,
                                Spacer, Table, TableStyle, KeepTogether)

VF_URL = ("https://github.com/google/fonts/raw/main/ofl/notosanssc/"
          "NotoSansSC%5Bwght%5D.ttf")
FONT_DIR = os.environ.get("BH_FONT_DIR", "/tmp/bhfonts")


# ----------------------------------------------------------------------------- fonts
def ensure_fonts():
    """准备 Regular / Bold 两个静态 TTF，返回路径对。"""
    os.makedirs(FONT_DIR, exist_ok=True)
    reg = os.path.join(FONT_DIR, "NotoSansSC-Regular.ttf")
    bold = os.path.join(FONT_DIR, "NotoSansSC-Bold.ttf")
    if os.path.exists(reg) and os.path.exists(bold):
        return reg, bold

    vf = os.path.join(FONT_DIR, "NotoSansSC-VF.ttf")
    if not os.path.exists(vf):
        print("downloading Noto Sans SC ...")
        subprocess.run(["curl", "-sSL", "-o", vf, VF_URL], check=True)

    from fontTools.ttLib import TTFont as FTTTFont
    from fontTools.varLib import instancer
    for w, out in ((400, reg), (700, bold)):
        f = FTTTFont(vf)
        inst = instancer.instantiateVariableFont(
            f, {"wght": w}, inplace=True, updateFontNames=True)
        inst.save(out)
        print("  %s %.1f MB" % (os.path.basename(out), os.path.getsize(out) / 1e6))
    return reg, bold


REG, BOLD = ensure_fonts()
pdfmetrics.registerFont(TTFont("SC", REG))
pdfmetrics.registerFont(TTFont("SC-B", BOLD))
pdfmetrics.registerFontFamily("SC", normal="SC", bold="SC-B")

# ----------------------------------------------------------------------------- styles
INK = colors.HexColor("#1b1f24")
MUTED = colors.HexColor("#5b6570")
ACCENT = colors.HexColor("#1f6feb")
LINE = colors.HexColor("#d6dbe1")
HEADBG = colors.HexColor("#eef2f6")
ZEBRA = colors.HexColor("#f7f9fb")


def S(name, **kw):
    base = dict(fontName="SC", fontSize=9.5, leading=15.5, textColor=INK,
                alignment=TA_LEFT, spaceBefore=0, spaceAfter=0)
    base.update(kw)
    return ParagraphStyle(name, **base)


stTitle = S("t", fontName="SC-B", fontSize=25, leading=33, textColor=INK)
stSub = S("s", fontSize=11.5, leading=18, textColor=MUTED)
stH1 = S("h1", fontName="SC-B", fontSize=16, leading=22, textColor=ACCENT,
         spaceBefore=15, spaceAfter=7)
stH2 = S("h2", fontName="SC-B", fontSize=11.5, leading=17, textColor=INK,
         spaceBefore=10, spaceAfter=4)
stP = S("p", spaceAfter=5)
stLi = S("li", leftIndent=11, bulletIndent=2, spaceAfter=3)
stNote = S("n", fontSize=8.8, leading=14, textColor=MUTED,
           leftIndent=8, spaceAfter=5)
stCell = S("c", fontSize=8.6, leading=13)
stCellB = S("cb", fontName="SC-B", fontSize=8.6, leading=13)
stHead = S("hd", fontName="SC-B", fontSize=8.8, leading=13,
           textColor=colors.HexColor("#243b55"))
stFoot = S("f", fontSize=7.8, textColor=MUTED)


def P(t, s=stP):
    return Paragraph(t, s)


def B(t):
    return Paragraph(t, stLi, bulletText="•")


def cells(rows, header, widths, align_right=()):
    """rows: list of list[str]"""
    data = [[Paragraph(h, stHead) for h in header]]
    for r in rows:
        data.append([Paragraph(c, stCellB if i == 0 and len(r) > 2 else stCell)
                     for i, c in enumerate(r)])
    t = Table(data, colWidths=widths, repeatRows=1, hAlign="LEFT")
    sty = [
        ("BACKGROUND", (0, 0), (-1, 0), HEADBG),
        ("LINEBELOW", (0, 0), (-1, 0), 0.9, ACCENT),
        ("LINEBELOW", (0, 1), (-1, -1), 0.35, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("TOPPADDING", (0, 0), (-1, -1), 4.5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4.5),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
    ]
    for i in range(1, len(data)):
        if i % 2 == 0:
            sty.append(("BACKGROUND", (0, i), (-1, i), ZEBRA))
    for c in align_right:
        sty.append(("ALIGN", (c, 1), (c, -1), "RIGHT"))
    t.setStyle(TableStyle(sty))
    return t


# ----------------------------------------------------------------------------- content
def build(story):
    A = story.append

    A(P("奇点 · Singularity", stTitle))
    A(Spacer(1, 3))
    A(P("版本总览　<b>8.5 → 8.5.4-beta</b>", stSub))
    A(P("适配 Mindustry v160.5（BE）· 桌面与安卓双端 · 2026-10-26", stSub))
    A(Spacer(1, 9))

    # ---------------------------------------------------------------- 版本一览
    A(P("1　五个版本各做了什么", stH1))
    A(cells([
        ["8.5-beta", "修复与验证",
         "解开科技树上的三处死循环（以太压力机 / 信号编码器 / 霍金冷凝器），补上视界装配厂与奇点船坞的解锁配方，"
         "流明焚烧炉与反向分选器补上真正的贴图。官方无头服务端跑通 <b>7,389</b> 条断言。"],
        ["8.5.1-beta", "两种新矿物 · 弹道对齐",
         "新增<b>废料</b>（原「工业废料」，并成为可挖的真矿物）与<b>星髓石</b>（高级结构材料）。"
         "炮塔的弹丸 / 炮口 / 火光三者对齐；矿机液体加速；共振枢纽门槛 1000 → 300；传送带与管道接缝清零；"
         "矿机不空转；29 种能打的单位全部上场。"],
        ["8.5.2-beta", "每一波都出兵 · 反拘留者",
         "波次表重写，<b>空波次修好</b>。新增废料嬗变炉（废料 + 流明浆 + 电力 → 随机基础矿）。"
         "星髓石扩用到建造花销。流明液体桥重做、液体中继塔可手动指定连接。新增锚钉炮 / 锚钉手（弹药不可吸收）。"
         "晶格蠕者上半身重做、城塞领航者旋转件修轴。性能优化并新增「奇点」设置页。安卓真机四个崩溃热修。"],
        ["8.5.3-beta", "枪口对齐 · 难度减半",
         "<b>类星体喷流</b>的光束起点从地面挪到枪口（连续光束炮走的是另一条代码路径，之前漏了）。"
         "刷怪表重配：每一波都出兵、所有能打的怪都出场、高级怪降频、难度整体减半、新手图最高 T4。"],
        ["<b>8.5.4-beta</b>", "<b>特效 · 提速 · 星髓石</b>",
         "晶格蠕者的<b>落点爆炸</b>缩到 1/3；炮弹速度 ×1.25（射程不变）；废料炉<b>双倍速</b>并产出星髓石；"
         "拘留者的<b>生产成本</b>里加星髓石；星髓石再扩用到 22 个方块与 13 个单位方案。"],
    ], ["版本", "主题", "主要内容"], [52, 62, 300]))

    # ---------------------------------------------------------------- 8.5.4 详述
    A(P("2　8.5.4-beta 逐项说明", stH1))

    A(P("2.1　晶格蠕者的落点爆炸缩到 1/3", stH2))
    A(P("它打出去的炮弹落地时，用的是 <font face='SC-B'>AureliaFx.detainImpact</font> —— "
        "那个特效原本是给锚钉炮那类大口径弹写的（溅射半径 26 格），装在晶格蠕者这门短筒迫击炮上，"
        "一朵火球几乎和车体一样大。"))
    A(P("新增 <font face='SC-B'>AureliaFx.detainImpactSmall</font>，"
        "<b>所有长度都除以 3</b>：描边 3 → 1、圆环基半径 4 → 1.33、外扩 6+28 → 2+9.33、火花点半径 2 → 0.67。"
        "混合模式、配色、时长、旋转节奏一律不动 —— 形状、颜色、节奏完全一致，只是小了一圈。"))
    A(cells([
        ["描边 strokes", "3 × fout", "1 × fout"],
        ["圆环半径", "4 + fin × (rot + 26)", "1.33 + fin × (rot + 26) / 3"],
        ["六边形边长", "2 + fin × (rot × 0.6 + 16)", "0.67 + fin × (rot × 0.6 + 16) / 3"],
        ["火花扩散（10 颗）", "6 + fin × 28", "2 + fin × 9.33"],
        ["火花点半径", "2 × fout", "0.67 × fout"],
        ["时长 / 裁剪半径", "40 / 120 刻", "40 / 40 刻"],
    ], ["尺寸项", "原来（detainImpact）", "现在（1/3 版）"], [95, 165, 154]))
    A(Spacer(1, 4))
    A(P("单独做一份而不是直接改原特效：锚钉炮和拘留者那条线也在用 "
        "<font face='SC-B'>detainImpact</font>，一起缩小等于把那些重型武器的打击感也削掉了。", stNote))
    A(P("注意这<b>不是</b> 8.5.2 那一次：8.5.2 缩的是<b>枪口火光</b>"
        "（<font face='SC-B'>muzzleSmall / muzzleSmokeSmall</font>），这一次缩的是<b>炮弹落点后的爆炸</b>。", stNote))

    A(P("2.2　炮弹速度 ×1.25，射程不变", stH2))
    A(cells([
        ["炮弹速度 speed", "2.6", "<b>3.25</b>（×1.25）"],
        ["存活时间 lifetime", "84 刻", "<b>67 刻</b>（÷1.25）"],
        ["实际射程 speed × lifetime", "218.4 格", "<b>217.75 格</b>（基本不变）"],
        ["单位瞄准距离 range", "215 格", "215 格（不动）"],
    ], ["项", "8.5.3", "8.5.4"], [130, 100, 184], align_right=(1, 2)))
    A(Spacer(1, 4))
    A(P("两个数字一起改，是为了让炮弹<b>飞得更快但打不到更远</b>：更容易命中移动目标，"
        "单位强度一点没变。伤害、溅射范围、射速、装填时间全部没动。", stNote))

    A(P("2.3　废料嬗变炉：双倍速，并且产出星髓石", stH2))
    A(B("<b>生产速度 ×2</b>：craftTime 110 刻 → <b>55 刻</b>。"))
    A(B("<b>产出表加入星髓石</b>，权重最低。新表：流明矿 36% / 金辉矿 28% / 淤砂 23% / "
        "共振碎片 8% / <b>星髓石 5%</b>。"))
    A(B("一次最多两份（rolls = 2），所以每炉至少出一份星髓石的概率约 "
        "<b>1 − 0.95² ≈ 9.75%</b>；配合双倍速，星髓石的期望产出是原来的两倍还多。"))
    A(B("进料没变：仍然只吃<b>废料 + 流明浆 + 电力</b>。"))
    A(P("刻意<b>没有</b>加进来的是棱镜玻璃、棱镜合金、协和核心 —— 那些是成品材料，"
        "能从废料里直接印出来的话，后面那几台越级的机器就没有存在意义了。", stNote))

    A(P("2.4　拘留者：生产成本里加星髓石", stH2))
    A(P("实测这条生产方案现在的耗材（无头服务端读出的真实数据）："))
    A(cells([
        ["金辉矿 blackhole-aurite", "4,320"],
        ["共振碎片 blackhole-resonance-shard", "4,480"],
        ["棱镜合金 blackhole-prism-alloy", "2,540"],
        ["协和核心 blackhole-concord-core", "1,150"],
        ["<b>星髓石 blackhole-star-marrow</b>", "<b>10</b>"],
        ["生产时间", "12,600 刻（3.5 分钟）"],
    ], ["拘留者的生产方案（UnitPlan）", "数量"], [300, 114], align_right=(1,)))
    A(Spacer(1, 4))
    A(B("另外，造那台 5×5 的 Astro 制造机<b>本体</b>也要 6 份星髓石。"))
    A(P("有一处引擎细节值得说明：v160.5 的 <font face='SC-B'>UnitType</font> "
        "<b>根本没有 requirements 字段</b>，单位的生产成本完全由工厂里的 "
        "<font face='SC-B'>UnitPlan</font> 决定。所以当初那行写着硅 / 钛 / 钍 / 塑钢 / 相织布 / 涌流合金的 "
        "<font face='SC-B'>requirements(...)</font> 其实是<b>制造机方块</b>的建造成本，"
        "不是拘留者的。你直觉没错 —— 它已经被 <font face='SC-B'>recost()</font> "
        "换算成了欧雷莉亚材料（上面表里已经全是模组材料了）。", stNote))

    A(P("2.5　星髓石扩用（第二轮）", stH2))
    A(P("沿用 8.5.2 那条规则：<b>只加一次性建造成本</b>，不加消耗器、不改产出、不改耗电、不改研究成本。"
        "数量按原造价定，越贵给得越多。"))
    A(P("现在全包共有 <b>22 个方块</b>和 <b>13 个单位生产方案</b>用到星髓石（8.5.3 时是 6 处写在源码里）。"
        "这一版新增："))
    A(cells([
        ["堡垒核心 bastionCore", "10", "壁垒守卫 bastionWarden", "4"],
        ["棱镜采集机 prismHarvester", "3", "玄武岩卫 basaltGuard", "4"],
        ["引力搅动机 gravitonChurn", "3", "金辉先锋 auriteVanguard", "3"],
        ["棱镜过载器 prismOvercharger", "3", "裂隙呼唤者 ventCaller", "3"],
        ["裂隙汇聚器 riftCollector", "2", "共振攻城 resonanceSiege", "3"],
        ["极光维修穹顶 auroraRepairDome", "2", "极光重装 auroraHeavy", "3"],
        ["等离子发电机 plasmaDynamo", "2", "极光卫队 auroraGuard", "2"],
        ["共振枢纽 resonanceHub", "2", "拘留者 astro-detainer", "10"],
        ["神盾阵列 aegisLattice", "2", "（制造机本体 fabricator）", "6"],
    ], ["方块（建造成本）", "星髓石", "单位（生产方案）", "星髓石"],
        [166, 42, 166, 42], align_right=(1, 3)))
    A(Spacer(1, 4))
    A(P("方块那格的「星髓石」是建造成本；单位那格是每次生产要额外吃掉的量。", stNote))

    # ---------------------------------------------------------------- 前几版回溯
    A(P("3　前四个版本的关键内容", stH1))

    A(P("3.1　8.5.3-beta", stH2))
    A(B("<b>类星体喷流的枪口对齐。</b>它是连续光束炮，引擎每帧都从 "
        "<font face='SC-B'>(shootX, shootY)</font> 反推光束起点，而 "
        "<font face='SC-B'>shootY = 0</font> 会把起点钉死在地面 —— 光束看着像从炮塔脚下的地里冒出来。"
        "新增 <font face='SC-B'>ContinuousTurret3D</font>：按相机透视把枪口的 Z 高度投影回屏幕，"
        "起点和终点一起挪，射程与朝向一点不变。枪口高度从 3D 模型直接读取，以后改炮管会自动跟着走。"))
    A(B("<b>刷怪表重配。</b>每一波都出兵（8 组配置离线模拟，空波次全部为 0）；"
        "roster 扩到 30 条并加了自动扫描，以后新增单位也不会漏；排除的只有不能攻击的、海军、玩家机体三类。"))
    A(B("<b>难度减半。</b>实测平均出兵数：新手图 9 → 4（−56%），终局图 31 → 12（−61%），峰值 90 → 38。"))
    A(B("<b>新手图不出 T5。</b>威胁度低于 0.34 的图根本不生成 T5 分组，最高只到 T4。"))
    A(P("弯路记一笔：一开始按常规思路把每组上限打对折，实测总量只掉了 22%。因为 "
        "<font face='SC-B'>unitAmount</font> 和 <font face='SC-B'>max</font> 的下限都是 1，"
        "而涨速本来就慢，绝大多数波次每组就是 1 只 —— 上限根本用不上。真正决定「一波来多少」的是 "
        "<b>Σ(1 / spacing)</b>，所以总闸最后落在出怪间隔上。", stNote))

    A(P("3.2　8.5.2-beta", stH2))
    A(B("<b>每一波都出兵。</b>波次表从「连乘总数」改成「按权重切进每一波」，每波保底 1 只。"))
    A(B("<b>废料嬗变炉。</b>吃废料 + 流明浆 + 电力，按权重随机吐基础矿，一次最多两种。"))
    A(B("<b>锚钉炮 / 锚钉手。</b>弹药带官方「不可吸收」标志，拘留者的爪子抓不走它。"
        "数值压在同级别常规兵器的量级，拘留者的圣盾照常生效。"))
    A(B("<b>晶格蠕者开火特效缩到 1/3</b>（8.5.4 又补上了落点爆炸那一档）、上半身重做；"
        "城塞领航者模型重做并修好旋转件的轴。"))
    A(B("<b>性能。</b>只优化性能，画面一动没动 —— 全部 100 多个模型的烘焙图与 8.5.1 逐字节相同。"
        "设置里多了「奇点」专页。"))
    A(B("<b>安卓真机四个热修。</b>炮塔开火就崩（<font face='SC-B'>AbstractMethodError</font>，"
        "ART 上 lambda 不能当 <font face='SC-B'>BulletHandler</font> 用）、"
        "流明液体桥一连线就崩（继承了物品桥的 Building）、废料炉材料齐全却不产出、开火特效还是很大。"))

    A(P("3.3　8.5.1-beta", stH2))
    A(B("<b>两种新矿物。</b>废料（原「工业废料」，并成为地面可挖的真矿物，有废渣堆）；"
        "星髓石（硬度 4，与共振碎片同级，定位是结构强度材料）。"))
    A(B("<b>炮塔三者对齐。</b>前哨磁轨炮 / 以太长枪原来只在朝北时对得上，最大偏 1.65 格；"
        "其余 18 门补上枪口火光的抬升。"))
    A(B("<b>矿机液体加速</b>、<b>共振枢纽门槛 1000 → 300</b>、"
        "<b>传送带 / 管道接缝清零</b>（120 条边从 102 条不一致 → 0）、<b>矿机不空转</b>、"
        "<b>刷怪全部上场</b>。"))

    A(P("3.4　8.5-beta", stH2))
    A(B("解开科技树三处死循环：以太压力机不再吃神盾合金；科技树顺序改为「霍金尘 → 以太压力机 → 神盾合金」；"
        "霍金冷凝器的预初始化液体消耗器从原版低温液改成极光冷冻液。"))
    A(B("补上视界装配厂与奇点船坞的解锁配方（原来能白嫖）；"
        "流明焚烧炉与反向分选器补上真正的贴图，替掉字形图标。"))

    # ---------------------------------------------------------------- 验证
    A(P("4　8.5.4-beta 的验证", stH1))
    A(cells([
        ["javac（JDK 17，全量源码）", "通过，0 error / 0 warning"],
        ["ModelCheck（102 个方块模型 + 20 个图标）", "<b>0 problems</b>"],
        ["bake（模型烘焙 + 开放网格审计）", "<b>problems: 0</b>，20 图标 open samples 0"],
        ["classes.dex 校验", "<b>合法，安卓可加载</b>（magic dex 035、adler32 与 SHA-1 一致）"],
        ["无头服务端实载", "<b>1 mods loaded</b> / Server loaded，0 error / 0 exception"],
        ["运行时数值复核", "拘留者生产方案、废料炉产出表与 craftTime、"
                          "晶格蠕者炮弹速度 / 存活 / 落点特效，均由服务端日志逐项核对"],
    ], ["检查项", "结果"], [175, 239]))

    A(P("5　安装", stH1))
    A(B("先备份游戏存档及战役数据。"))
    A(B("删除或替换旧的 Singularity JAR，只保留 <font face='SC-B'>Singularity-8.5.4-beta.jar</font>。"))
    A(B("从 Mindustry 的「模组 → 导入模组」导入 JAR，重启游戏。"))
    A(B("ZIP 是源码与报告包，<b>不是</b>另一个需要并装的模组。"))
    A(P("星髓石进了更多建造成本与生产方案，但<b>没有新增地面矿石</b>，"
        "所以老存档不需要重开；已经建好的东西也不会变化。", stNote))


# ----------------------------------------------------------------------------- doc
def footer(canv, doc):
    canv.saveState()
    canv.setStrokeColor(LINE)
    canv.setLineWidth(0.4)
    canv.line(18 * mm, 14 * mm, A4[0] - 18 * mm, 14 * mm)
    canv.setFont("SC", 7.8)
    canv.setFillColor(MUTED)
    canv.drawString(18 * mm, 10 * mm,
                    "奇点 Singularity · 版本总览 8.5 → 8.5.4-beta · Mindustry v160.5")
    canv.drawRightString(A4[0] - 18 * mm, 10 * mm, "第 %d 页" % doc.page)
    canv.restoreState()


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else \
        "docs/Singularity-8.5-8.5.4-beta-中文汇总.pdf"
    os.makedirs(os.path.dirname(out) or ".", exist_ok=True)
    doc = BaseDocTemplate(out, pagesize=A4,
                          leftMargin=18 * mm, rightMargin=18 * mm,
                          topMargin=16 * mm, bottomMargin=20 * mm,
                          title="奇点 Singularity 版本总览 8.5 → 8.5.4-beta",
                          author="Singularity Mod")
    frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="n")
    doc.addPageTemplates([PageTemplate(id="all", frames=[frame], onPage=footer)])
    story = []
    build(story)
    doc.build(story)
    print("wrote %s (%.0f KB)" % (out, os.path.getsize(out) / 1024))


if __name__ == "__main__":
    main()
