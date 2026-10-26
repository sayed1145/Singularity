#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成《Singularity 8.5 ~ 8.5.7 全部版本更新合并汇总》中文 PDF。

用法：
    python3 tools/make_all_versions_pdf.py [输出路径]

字体：Noto Sans SC（SIL Open Font License）。自动下载变量字体并实例化 Regular / Bold，
缓存在 BH_FONT_DIR（默认 /tmp/bhfonts），不写入仓库。PDF 内嵌子集。
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

ACCENT = colors.HexColor("#1b3a63")
ACCENT2 = colors.HexColor("#2f6ea8")
GREY = colors.HexColor("#5a6672")
RULE = colors.HexColor("#c9d4de")
BG = colors.HexColor("#f2f6fa")
GOOD = colors.HexColor("#1c6b3f")


def ensure_fonts():
    os.makedirs(FONT_DIR, exist_ok=True)
    reg = os.path.join(FONT_DIR, "NotoSansSC-Regular.ttf")
    bold = os.path.join(FONT_DIR, "NotoSansSC-Bold.ttf")
    if os.path.exists(reg) and os.path.exists(bold):
        return reg, bold

    vf = os.path.join(FONT_DIR, "NotoSansSC-VF.ttf")
    if not os.path.exists(vf):
        print("下载 Noto Sans SC 变量字体…")
        subprocess.run(["curl", "-sSL", "-o", vf, VF_URL], check=True)
    try:
        from fontTools.ttLib import TTFont as _TTFont
        from fontTools.varLib import instancer
        #open the font object first: instantiateVariableFont on a bare path trips
        #sanityCheckVariableTables on some fontTools versions
        base = _TTFont(vf)
        if "fvar" not in base:
            raise RuntimeError("downloaded font has no fvar axis")
        for tag, name, out in ((400, "Regular", reg), (700, "Bold", bold)):
            f = instancer.instantiateVariableFont(base, {"wght": tag}, inplace=False)
            #instancing keeps the VF's own name records (which read "Thin"), so relabel them
            for rec in f["name"].names:
                if rec.nameID in (1, 16):
                    rec.string = "Noto Sans SC"
                elif rec.nameID == 4:
                    rec.string = "Noto Sans SC " + name
                elif rec.nameID in (2, 17):
                    rec.string = name
                elif rec.nameID == 6:
                    rec.string = "NotoSansSC-" + name
            f.save(out)
            print("  instantiated %s (wght=%d)" % (name, tag))
    except Exception as e:
        print("实例化失败(%s)，回退到变量字体本身" % e, file=sys.stderr)
        reg = bold = vf
    return reg, bold


REG, BOLD = ensure_fonts()
pdfmetrics.registerFont(TTFont("SC", REG))
pdfmetrics.registerFont(TTFont("SC-B", BOLD))

W, H = A4
LM = RM = 18 * mm
TM = 20 * mm
BM = 18 * mm

S = {
    "title": ParagraphStyle("t", fontName="SC-B", fontSize=21, leading=27,
                            textColor=ACCENT, spaceAfter=4),
    "sub": ParagraphStyle("s", fontName="SC", fontSize=10.5, leading=15,
                          textColor=GREY, spaceAfter=10),
    "h1": ParagraphStyle("h1", fontName="SC-B", fontSize=14.5, leading=19,
                         textColor=ACCENT, spaceBefore=13, spaceAfter=6),
    "h2": ParagraphStyle("h2", fontName="SC-B", fontSize=11.5, leading=16,
                         textColor=ACCENT2, spaceBefore=9, spaceAfter=4),
    "h3": ParagraphStyle("h3", fontName="SC-B", fontSize=10, leading=14,
                         textColor=colors.HexColor("#2b3a47"), spaceBefore=6,
                         spaceAfter=3),
    "b": ParagraphStyle("b", fontName="SC", fontSize=9.4, leading=14.6,
                        alignment=TA_LEFT, spaceAfter=4),
    "li": ParagraphStyle("li", fontName="SC", fontSize=9.4, leading=14.6,
                         leftIndent=11, bulletIndent=2, spaceAfter=2.5),
    "code": ParagraphStyle("c", fontName="SC", fontSize=8.4, leading=12.4,
                           textColor=colors.HexColor("#20313f"), leftIndent=8,
                           backColor=colors.HexColor("#eef3f8"),
                           borderPadding=(4, 5, 4, 5), spaceBefore=3, spaceAfter=5),
    "cap": ParagraphStyle("cap", fontName="SC", fontSize=8.2, leading=12,
                          textColor=GREY, spaceBefore=2, spaceAfter=8),
}


def P(t, s="b"):
    return Paragraph(t, S[s])


def B(t):
    return Paragraph(t, S["li"], bulletText="•")


def code(t):
    return Paragraph(t.replace("&", "&amp;").replace("<", "&lt;")
                     .replace(">", "&gt;").replace(" ", "&nbsp;")
                     .replace("\n", "<br/>"), S["code"])


def table(data, widths, header=True, align=None):
    rows = [[Paragraph(c, S["b"]) for c in r] for r in data]
    t = Table(rows, colWidths=widths, repeatRows=1 if header else 0, hAlign="LEFT")
    st = [
        ("GRID", (0, 0), (-1, -1), 0.4, RULE),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("TOPPADDING", (0, 0), (-1, -1), 3.5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 3.5),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
    ]
    if header:
        st += [("BACKGROUND", (0, 0), (-1, 0), BG),
               ("TEXTCOLOR", (0, 0), (-1, 0), ACCENT)]
        st += [("ROWBACKGROUNDS", (0, 1), (-1, -1),
                [colors.white, colors.HexColor("#f7fafd")])]
    if align:
        st += align
    t.setStyle(TableStyle(st))
    return t


def rule():
    t = Table([[""]], colWidths=[W - LM - RM], rowHeights=[1.1])
    t.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, -1), RULE)]))
    return t


def on_page(canvas, doc):
    canvas.saveState()
    canvas.setFont("SC", 7.8)
    canvas.setFillColor(GREY)
    canvas.drawString(LM, H - 12 * mm,
                      "Singularity 8.5 ~ 8.5.7 全部版本更新合并汇总")
    canvas.drawRightString(W - RM, H - 12 * mm, "Mindustry v160.5")
    canvas.setStrokeColor(RULE)
    canvas.setLineWidth(0.4)
    canvas.line(LM, H - 14 * mm, W - RM, H - 14 * mm)
    canvas.line(LM, 14 * mm, W - RM, 14 * mm)
    canvas.drawString(LM, 10 * mm, "奇点 · Singularity")
    canvas.drawRightString(W - RM, 10 * mm, "第 %d 页" % doc.page)
    canvas.restoreState()


def build(path):
    doc = BaseDocTemplate(path, pagesize=A4, leftMargin=LM, rightMargin=RM,
                          topMargin=TM, bottomMargin=BM,
                          title="Singularity 8.5 ~ 8.5.7 全部版本更新合并汇总",
                          author="Singularity")
    frame = Frame(LM, BM, W - LM - RM, H - TM - BM, id="n")
    doc.addPageTemplates([PageTemplate(id="all", frames=[frame], onPage=on_page)])
    F = []

    # ---------- 封面区 ----------
    F.append(P("奇点 · Singularity", "title"))
    F.append(P("8.5 ~ 8.5.6 全部版本更新合并汇总　·　Mindustry v160.5", "sub"))
    F.append(rule())
    F.append(Spacer(1, 5))

    F.append(P("本文件把 8.5、8.5.1、8.5.2、8.5.3、8.5.4、8.5.5、8.5.6、8.5.7 共八个版本的更新"
               "合并成一份，按“从旧到新、每版一节”排列。最新一版 8.5.7 在最前面先给结论，"
               "后面是逐版明细。全部数值均为无头服务器内实测，非照代码估算。", "b"))

    F.append(P("最新一版 8.5.7 一眼看完", "h1"))
    F.append(table([
        ["项目", "内容"],
        ["锚钉炮（3x3 炮塔）", "射速 <b>×1.25</b>（84 → 67.2 刻）、伤害 <b>×4.5</b>（34 → 153），"
                              "<b>建造成本完全不变</b>。"],
        ["锚钉手 · 三门炮", "主炮射速 ×1.2（72 → 60 刻）、伤害 ×6（27 → <b>162</b>）；"
                          "新增两门变种炮，射速是主炮的 <b>2.25 倍</b>，伤害 <b>58</b> 与 <b>84</b>"
                          "（都在要求的 42～97 之间）。三门一律 <b>absorbable = false</b>，"
                          "拘留者依然抓不走。"],
        ["锚钉手 · 蛇式移动", "履带整套去掉，换成引擎官方 <b>CrawlUnit</b> 的分节躯干："
                            "楔形头节 + 六节装甲 + 一排轮流蹬地的腹鳞。"
                            "建模参考<b>两处都用了</b>：官方 "
                            "<font face='SC'>drawCrawl</font> 公式 + 真实蛇类蜿蜒行波。"],
        ["锚钉手 · 血量", "2350 → <b>12000</b>（要求 8000 以上、15000 以下）。"],
        ["锚钉手 · 造价", "提到 <b>T5 档</b>：58 秒 → <b>130 秒</b>，并要 <b>concordCore</b>。"],
        ["顺带修掉的老问题", "锚钉手的生产计划<b>从 v8.5.2 起就是坏的</b>（加载顺序导致 "
                            "<font face='SC'>UnitPlan.unit</font> 为 null，单位根本造不出来）。已修。"],
        ["类星体喷流音效", "循环音效及时止损。原来目标没了甚至炮塔被拆了还在响，"
                          "现在判定改为“确实在开火且未死亡且已启用”，不满足立刻 "
                          "<b>stop()</b>，不等那 20 刻的淡出。"],
        ["星髓石弹药", "只保留 <b>吸积炮</b>（165 直伤，不变）与 <b>事件视界炮</b>"
                       "（改制 <b>320</b> 重弹）。参考系拖曳炮、shardVolley 已去掉。"],
        ["奇点核心弹", "直伤 <b>120 → 150</b>，范围伤害 <b>180 → 210</b>，其余一律不动。"],
        ["地图尺寸", "战役图 <b>494×494 ～ 600×600</b>（原 272 ～ 348）；"
                     "非战役扇区默认 <b>494</b>。矿脉数量按面积缩放。"],
        ["刷怪强度", "增长 <b>×1.25</b>（随威胁度线性介入，<b>新手图不变</b>），"
                     "数量 <b>×1.2</b>。"],
        ["覆盖性实测", "全 32 张战役图：缺矿 <b>0</b>、无刷怪门 <b>0</b>；"
                       "刷怪：roster 30 档位全部出现（含 31 个不同单位），<b>空波 0</b>。"],
    ], [30 * mm, W - LM - RM - 30 * mm]))

    # ---------- 8.5.7 ----------
    F.append(P("8.5.7-beta　锚钉炮提速增伤 · 锚钉手改蛇式三门炮 · 提到 T5", "h1"))

    F.append(P("① 锚钉炮（3×3 炮塔）：射速 ×1.25、伤害 ×4.5，造价不变", "h2"))
    F.append(table([
        ["项", "8.5.6", "8.5.7"],
        ["装填 reload", "84 刻", "67.2 刻（射速 ×1.25）"],
        ["弹丸伤害", "34", "153（×4.5）"],
        ["建造成本", "lumenite 460 / aurite 320 / 棱晶玻璃 170 / 共振碎片 70 / 星髓石 8", "完全不变"],
        ["射程 / 血量 / 耗电 / 开火方式", "—", "一律不动"],
    ], [46 * mm, 46 * mm, W - LM - RM - 92 * mm]))

    F.append(P("② 锚钉手：三门炮", "h2"))
    F.append(table([
        ["炮", "8.5.6", "8.5.7", ""],
        ["主炮", "72 刻 / 27 伤害 / 三轮齐射", "60 刻 / 162 伤害", "射速 ×1.2，伤害 ×6"],
        ["变种炮 A", "—", "26.667 刻 / 58 伤害", "新增，射速是主炮的 2.25 倍"],
        ["变种炮 B", "—", "26.667 刻 / 84 伤害", "新增，射速是主炮的 2.25 倍"],
    ], [22 * mm, 50 * mm, 38 * mm, W - LM - RM - 110 * mm]))
    F.append(B("两门变种炮的伤害<b>各不相同</b>，都落在要求的 <b>42～97</b> 之间。"))
    F.append(B("三门炮的弹药<b>一律保持</b> <font face='SC'>absorbable = false</font> —— "
               "拘留者的爪子抓不走任何一发，也就没法把它们变成自己的能量或反射弹。"
               "拘留者的圣盾（aegis）照常生效，所以不是免费击杀。"))
    F.append(B("两门变种炮除了伤害与射速，弹道（速度、射程、寿命、特效）与主炮完全一致 —— "
               "换的只是弹头装药。"))
    F.append(P("运行时实测，合计单体 DPS <b>805.5</b>（8.5.6 是 67.5）：", "b"))
    F.append(code(
        "[0] reload=60.0t        damage=162.0  absorbable=false  shotsPerVolley=3\n"
        "[1] reload=26.666664t   damage=58.0   absorbable=false  shotsPerVolley=1\n"
        "[2] reload=26.666664t   damage=84.0   absorbable=false  shotsPerVolley=1\n"
        "combined single-target DPS = 805.5"))

    F.append(P("③ 蛇式移动：去掉履带", "h2"))
    F.append(P("原来是一台 <font face='SC'>TankUnit</font>（履带）。现在换成引擎官方的 "
               "<b>CrawlUnit</b> —— 这是 v160.5 里<b>唯一真正的「蛇」</b>：它自带 "
               "<font face='SC'>Crawlc</font> 分量，每帧推进两件事"
               "（<font face='SC'>mindustry.entities.comp.CrawlComp</font>）：", "b"))
    F.append(code(
        "segmentRot = Angles.moveToward(segmentRot, rotation, type.segmentRotSpeed * Time.delta);\n"
        "segmentRot = Angles.clampRange(segmentRot, rotation, type.segmentMaxRot);\n"
        "crawlTime += deltaLen();          // 累加走过的距离，不是时间"))
    F.append(P("物理、寻路、踩到建筑上的减速、碾压伤害全部照旧由引擎负责，这边只负责把躯干画出来。", "b"))
    F.append(table([
        ["项", "值", "说明"],
        ["segmentRotSpeed", "2.6", "躯干朝向追机头的速度"],
        ["segmentMaxRot", "55°", "躯干最多能弯到偏离机头多少度"],
        ["crawlSlowdown", "0.72", "在密集建筑上有减速（官方 crawler 行为的一部分）"],
        ["crawlSlowdownFrac", "0.6", "脚下多大比例是实心才吃到满额减速"],
        ["crushDamage", "0", "不加新行为"],
    ], [38 * mm, 20 * mm, W - LM - RM - 58 * mm]))
    F.append(P("模型：一个楔形头节拖着<b>六节</b>装甲躯干加一条尖尾，"
               "<b>没有履带、没有轮子、没有腿</b>，每节底下有一片腹鳞轮流蹬地。", "b"))

    F.append(P("建模参考（两处都用了）", "h3"))
    F.append(P("<b>(a) 引擎官方公式</b>（v160.5 <font face='SC'>UnitType.drawCrawl</font>）：", "b"))
    F.append(code(
        "float trns = Mathf.sin(crawlTime + i * segmentPhase, segmentScl, segmentMag);\n"
        "float rot  = Mathf.slerp(crawl.segmentRot(), unit.rotation, i / (float)(segments - 1));"))
    F.append(B("照抄：<font face='SC'>Mathf.sin(a, b, c)</font> 是 "
               "<font face='SC'>sin(a / b) * c</font>（反编译 arc 字节码核对过）。"))
    F.append(B("照抄：这个 <font face='SC'>trns</font> 是<b>沿身体轴</b>的偏移"
               "（官方用 <font face='SC'>Angles.trnsx(rot, trns)</font>），"
               "所以是纵向的一松一紧，像毛虫那样蠕动 —— 这边把它加在「步长」上。"))
    F.append(B("<b>反过来</b>：官方的第 0 节拿 <font face='SC'>segmentRot</font>、"
               "最后一节才拿 <font face='SC'>unit.rotation</font>，"
               "也就是官方把<b>最后一节当成头</b>来画。这边的骨架第 0 节就是头，"
               "所以插值方向要倒过来（机头 = 当前航向，尾巴 = 滞后的 "
               "<font face='SC'>segmentRot</font>）。"
               "<b>不倒过来的话蛇会倒着游。</b>"))
    F.append(P("<b>(b) 真实蛇类的蜿蜒（serpentine locomotion）</b>：蛇前进靠的是从头传向尾的"
               "<b>横向行波</b>，身体每一段侧向推地面，波往后走、蛇往前走。三个特征照着调的：", "b"))
    F.append(B("<b>波从头传到尾</b> —— 相位写成 "
               "<font face='SC'>cos(crawlTime / waveScl - i * wavePhase)</font>，"
               "同一个波峰随 <font face='SC'>crawlTime</font> 增大要往更大的 "
               "<font face='SC'>i</font> 走，也就是往尾巴走；"))
    F.append(B("<b>腹鳞依次蹬地</b> —— 蛇没有腿，靠一排腹鳞轮流抓地，"
               "所以每节底下的腹鳞按同一个行波错相拍动；"))
    F.append(B("<b>身体一段段微微抬起</b> —— 蛇不是贴着地面平移。"))

    F.append(P("关键实现：沿脊柱按弧长积分", "h3"))
    F.append(P("第一版是每节各算各的 <font face='SC'>(横向偏移, -i * spacing)</font>。"
               "用 <font face='SC'>SerpentProbe</font> 一量，<b>躯干会被甩开</b>："
               "尾巴那几节之间最大裂到 <b>8.5 个设计单位</b>（约 1 格），肉眼看得见缝。"
               "改成<b>沿脊柱按弧长积分</b>：先算好每节的朝向，再从机头起一节一节往前走，"
               "每步走 <font face='SC'>spacing + 引擎纵向蠕动</font> 这么长、"
               "方向取相邻两节朝向的<b>角平分线</b>。"
               "于是相邻两节中心的距离<b>恒等于步长</b>，"
               "躯干在任何相位、任何弯折下都不可能被拉开。", "b"))
    F.append(P("还踩了一个坑：角平分线一开始写成 "
               "<font face='SC'>(ang[i] + ang[i+1]) / 2</font>。"
               "<font face='SC'>slerp</font> 有可能返回 343.7 来表示 −16.3，"
               "于是 −13 和 343.7 平均成了 <b>+165°</b>，方向整个反过来，身体往机头前面长。"
               "改成<b>取最短弧</b>（先 <font face='SC'>wrap180</font> 两角之差再取半）才对。", "b"))
    F.append(P("自检输出（扫过 720 个波形相位 × 13 个弯折角 × 4 个朝向）：", "b"))
    F.append(code(
        "NaN offsets: 0\n"
        "worst gap between consecutive plates: 0->1 -0.885 1->2 -0.489 2->3 -0.391\n"
        "                                      3->4 -0.273 4->5 -0.126 5->6 -0.047\n"
        "closest non-neighbour sections: 5.078 (sections 1 and 3)\n"
        "VERDICT: body stays joined - every plate overlaps its neighbour at every phase and bend"))
    F.append(P("全是负数 = 每块装甲板在<b>任何</b>相位和弯折下都跟邻居叠着"
               "（负数表示重叠，正数才是裂缝）。非相邻节最近 <b>5.078</b>，远大于板宽，不会自交。", "b"))

    F.append(P("④ 血量 2350 → 12000", "h2"))
    F.append(P("落在要求的 <b>8000 以上、15000 以下</b>。护甲 12、速度 0.78 都没动。", "b"))

    F.append(P("⑤ 造价：提到 T5 档", "h2"))
    F.append(table([
        ["", "8.5.6", "8.5.7"],
        ["建造时间", "58 s", "130 s"],
        ["成本", "lumenite 150 / aurite 120 / 棱晶玻璃 60 / 共振碎片 30 / 星髓石 4",
         "lumenite 640 / aurite 420 / 棱晶玻璃 180 / 棱晶合金 260 / 共振碎片 150 / "
         "concordCore 48 / 星髓石 40"],
    ], [20 * mm, 52 * mm, W - LM - RM - 72 * mm]))
    F.append(P("同档位参照：resonanceSiege 105 s、auroraHeavy 120 s、prismMonolith 150 s、"
               "bastionWarden 160 s。仍然在「共振摇篮」里造（那是最早的单位线），"
               "但价格和耗时已经完全是 T5 的东西。", "b"))

    F.append(P("⑥ 顺带修掉：锚钉手的生产计划从 v8.5.2 起就是坏的", "h2"))
    F.append(P("查造价的时候发现的，跟这次的改动无关，但既然碰上了就修掉。"
               "<font face='SC'>BlackHoleMod.loadContent()</font> 的加载顺序是：", "b"))
    F.append(code(
        "AureliaFrontier.load()      <- 在这里 new UnitPlan(AnchorContent.spikeWarden, ...)\n"
        "...\n"
        "AnchorContent.load()        <- 到这一步 spikeWarden 才被造出来"))
    F.append(P("也就是说建生产计划的时候 <font face='SC'>AnchorContent.spikeWarden</font> "
               "<b>还是 null</b>，于是那个 <font face='SC'>UnitPlan</font> 的 "
               "<font face='SC'>unit</font> 字段一直是 null。引擎里 "
               "<font face='SC'>UnitFactory</font> 到处在用 "
               "<font face='SC'>plan.unit.isBanned()</font> / "
               "<font face='SC'>plan.unit.uiIcon</font> / "
               "<font face='SC'>plan.unit.localizedName</font>，null 会直接炸 —— "
               "<b>从 v8.5.2 起，锚钉手就从来没能真的从共振摇篮里造出来过。</b>"
               "修法：把这个计划挪到 <font face='SC'>AnchorContent.load()</font> 之后注册。", "b"))

    # ---------- 8.5.6 ----------
    F.append(P("8.5.6-beta　类星体音效止损 · 星髓石收敛 · 全图放大 · 刷怪加强", "h1"))

    F.append(P("① 类星体喷流：循环音效及时止损", "h2"))
    F.append(P("症状：光束早灭了、甚至炮塔都被拆了，循环音效还在响。", "b"))
    F.append(P("根因（对着 v160.5 源码查出来的）—— 三个坑叠在一起：", "b"))
    F.append(B("<b>判定只看“还有没有光束子弹”。</b> <font face='SC'>ContinuousTurretBuild"
               ".updateBullet</font> 每刻把 <font face='SC'>bullet.time</font> 钉在 "
               "<font face='SC'>lifetime × optimalLifeFract × warmup</font> 上，"
               "只有在 <font face='SC'>isShooting</font> 变 false 之后才放手让它自然到期 "
               "—— <font face='SC'>quasarBeam</font> 的 lifetime 是 80，"
               "于是目标没了之后子弹还要再活几十刻，声音就跟着再响几十刻。"))
    F.append(B("<b>SoundLoop 自己还要再拖 20 刻。</b> <font face='SC'>fadeSpeed</font> "
               "写死为 <font face='SC'>0.05</font>，<font face='SC'>play == false</font> "
               "之后要用 <font face='SC'>1 / 0.05 = 20</font> 刻（0.33 秒）才真的 "
               "<font face='SC'>Core.audio.stop()</font>。"))
    F.append(B("<b>SoundLoop 只在 updateTile() 里被驱动。</b> 实体一旦不再更新，"
               "就再没有人去停它。"))
    F.append(P("改法（<font face='SC'>src/blackhole/ContinuousTurret3D.java</font>）："
               "把“真的在开火”加进判定，并且一不满足就<b>立刻</b> stop()，"
               "不等那 20 刻的淡出。", "b"))
    F.append(code(
        "@Override public boolean shouldActiveSound(){\n"
        "    return super.shouldActiveSound() && isShooting && !dead && enabled;\n"
        "}\n"
        "@Override public void updateTile(){\n"
        "    super.updateTile();                        //里面已跑过 bullets.removeAll(...)\n"
        "    if(soundLoop != null && !shouldActiveSound()) soundLoop.stop();\n"
        "}\n"
        "@Override public void remove(){      super.remove();      if(soundLoop != null) soundLoop.stop(); }\n"
        "@Override public void onDestroyed(){ super.onDestroyed(); if(soundLoop != null) soundLoop.stop(); }"))
    F.append(P("伤害、射程、耗电、射速、光束长度<b>一个都没动</b>，改的只有音效什么时候停。", "b"))

    F.append(P("② 星髓石弹药：只留两门炮，事件视界炮换重弹", "h2"))
    F.append(table([
        ["", "8.5.5", "8.5.6"],
        ["能装星髓石的炮塔", "吸积炮、参考系拖曳炮、视界、shardVolley（4 门）",
         "吸积炮、事件视界炮（2 门）"],
        ["吸积炮的星髓弹", "165 直伤 + 90 范围", "不变（165 + 90）"],
        ["事件视界炮的星髓弹", "165（同一发）", "320 直伤 + 190 范围"],
    ], [34 * mm, 58 * mm, W - LM - RM - 92 * mm]))
    F.append(P("事件视界炮拿的是新的 <font face='SC'>marrowLanceHeavy</font>（<b>320</b>，"
               "落在 300～350 区间内）。它和 165 那发同一条弹道 —— 速度、射程、"
               "每份矿 4 发、特效、灼印完全一致，换的只是弹头装药。"
               "事件视界炮本来就是“一发定音”的炮（冷却 9 秒、一次吃 2 份弹药、射程 400），"
               "让它打 165 太浪费。", "b"))
    F.append(P("运行时日志（无头服务器实测）：", "b"))
    F.append(code("[blackhole/marrow] star marrow registered as turret ammo on 2/2 turrets\n"
                  "                  (accretion-cannon 165+90, event-horizon 320+190)"))

    F.append(P("③ 奇点核心弹：伤害 120 → 150，范围 180 → 210", "h2"))
    F.append(table([
        ["项", "8.5.5", "8.5.6"],
        ["直伤", "120", "150"],
        ["范围伤害", "180", "210"],
        ["范围半径", "60 像素", "不变"],
        ["速度 / 寿命 / 弹道 / 黑洞机制 / 特效", "—", "一律不动"],
    ], [58 * mm, 28 * mm, W - LM - RM - 86 * mm]))
    F.append(P("已确认 <font face='SC'>collapsarShell</font> 只被事件视界炮引用"
               "（<font face='SC'>BHBlocks.java:315</font>），"
               "所以这次上调<b>不会波及其他炮塔</b>。", "b"))

    F.append(P("④ 地图全面放大：494×494 ～ 600×600", "h2"))
    F.append(table([
        ["项", "8.5.5", "8.5.6"],
        ["战役图尺寸", "272 ～ 348", "494 ～ 600"],
        ["非战役扇区（默认）", "引擎默认（比战役图小一截）", "494"],
        ["格子数（最小图）", "272² = 73,984", "494² = 244,036（3.3 倍）"],
        ["格子数（最大图）", "348² = 121,104", "600² = 360,000（3.0 倍）"],
    ], [46 * mm, 40 * mm, W - LM - RM - 86 * mm]))
    F.append(P("配套改动：矿脉数量按面积缩放。这几个目标数字"
               "（1150 / 520 / 380 / 300 / 140）当年是按约 310×310 的图调的，"
               "地图放大后不跟着放大矿会稀到没法开局：", "b"))
    F.append(code(
        "private static final float oreReferenceArea = 310f * 310f;\n"
        "private static float areaScale(int w, int h){ return (w * h) / oreReferenceArea; }"))
    F.append(P("矿脉目标数、<font face='SC'>ensureOre(...)</font> 保底数、"
               "<font face='SC'>ensureWater(...)</font> 保底数、"
               "矿脉尝试次数上限，全部乘 <font face='SC'>areaScale</font>。", "b"))
    F.append(P("<b>全 32 张战役图实测</b>：尺寸全部落在 494 ～ 600（实测最小 494、最大 600）；"
               "<b>缺矿的图 0 / 32</b>，五种矿每张图都有；<b>没有刷怪门的图 0 / 32</b>；"
               "单张生成耗时 1.7 ～ 3.5 秒。单张 594×594 在 <font face='SC'>-Xmx320m</font> "
               "下也能生成成功（游戏一次只生成一张图；审计进程连跑 32 张才被 OOM 杀掉，"
               "那是审计脚本自己的问题，不是模组的）。", "b"))

    F.append(P("⑤ 刷怪：强度增长 ×1.25（新手图不变），数量 ×1.2", "h2"))
    F.append(P("引擎里 <font face='SC'>unitScaling</font> 是“每多刷 1 只需要的波数”，"
               "<b>越小涨得越快</b>。所以“强度增长 1.25 倍” = 把 "
               "<font face='SC'>unitScaling</font> 乘到 <b>1 / 1.25 = 0.8</b>，"
               "而且是<b>随威胁度线性介入</b>的：", "b"))
    F.append(code(
        "static float growthScale(float d){\n"
        "    return 1f / Mathf.lerp(1f, GROWTH_BOOST, Mathf.clamp(d));   //GROWTH_BOOST = 1.25f\n"
        "}\n"
        "// d = 0（新手图）-> 乘 1.0，一点不变\n"
        "// d = 1（终局图）-> 乘 0.8，涨速 1.25 倍"))
    F.append(P("这就是“新手图强度不变，只是说强度增长 1.25” —— "
               "<b>变的是坡度，不是起点</b>。数量 ×1.2 的 "
               "<font face='SC'>COUNT_BOOST</font> 同时作用在每组上限 "
               "<font face='SC'>g.max</font>、一次来几只 <font face='SC'>g.unitAmount</font>、"
               "以及 <font face='SC'>AureliaCampaign.balance()</font> 里的 cap"
               "（否则会把刚抬上去的数量又压回来）。", "b"))
    F.append(P("实测（<font face='SC'>SpawnGroup.getSpawned()</font> 按 v160.5 "
               "反编译后原样复刻）：", "b"))
    F.append(table([
        ["威胁度", "波数", "8.5.5 平均/峰值", "8.5.6 平均/峰值", "倍数"],
        ["0.13", "16", "5.1 / 11", "5.1 / 11", "1.000"],
        ["0.20", "18", "5.2 / 11", "5.3 / 11", "1.032"],
        ["0.30", "22", "5.8 / 12", "5.8 / 12", "1.000"],
        ["0.34", "24", "5.9 / 11", "6.0 / 12", "1.021"],
        ["0.45", "32", "6.8 / 15", "7.1 / 17", "1.041"],
        ["0.60", "45", "8.7 / 22", "9.6 / 23", "1.097"],
        ["0.75", "60", "10.9 / 25", "12.8 / 32", "1.169"],
        ["0.92", "79", "14.3 / 45", "17.1 / 54", "1.193"],
    ], [20 * mm, 16 * mm, 38 * mm, 38 * mm, W - LM - RM - 112 * mm],
        align=[("ALIGN", (0, 1), (-1, -1), "CENTER"),
               ("TEXTCOLOR", (4, 1), (4, 1), GOOD),
               ("TEXTCOLOR", (4, 8), (4, 8), GOOD)]))
    F.append(P("新手图 <b>1.000 倍（完全不变）</b>，终局图 <b>1.193 倍 ≈ 1.2</b>，"
               "中间平滑过渡。", "b"))

    F.append(P("所有怪仍然全部刷得出来", "h3"))
    F.append(B("roster 30 个档位，<b>31 个不同单位</b>在威胁档里出现过"
               "（31 &gt; 30 是因为拘留者 Detainer 在 threat ≥ 0.55 时"
               "作为独立 boss 组额外加入）→ <b>没有单位被漏掉</b>。"))
    F.append(B("<b>空波 0</b>：8 个威胁档共 296 波，每一波都刷得出东西。"))

    # ---------- 8.5.5 ----------
    F.append(P("8.5.5-beta　类星体几何修正 · 蠕者特效重写 · 星髓石作为炮塔弹药", "h1"))
    F.append(table([
        ["项", "内容"],
        ["类星体喷流几何", "开火与枪口几何修正。最坏情况（90°）下枪口偏差由 "
                          "<b>2142.3 像素（267.8 格）</b> 降到 <b>21.1 像素（2.6 格）</b>，"
                          "<b>102 倍</b>改善。依据存档在 "
                          "<font face='SC'>docs/quasar-geom-8.5.5.txt</font>。"],
        ["CrawlerFx 重写", "晶格蠕者特效<b>从头重写一遍</b>（不是缩放）。"
                           "枪口焰 9.0 → <b>1.6</b>；枪口烟 11.6 → <b>2.8</b>；"
                           "<b>落点 389.0 → 129.7 → 9.6</b>（13.5 倍缩小）；"
                           "破片命中 14.0 → <b>1.7</b>。像素级交叉复核：18.60 → 9.27 → "
                           "<b>1.32 格</b>。"],
        ["星髓石弹药", "星髓石成为炮塔弹药，<b>165 伤害</b>，装在 4 门炮上，"
                       "配<b>完全自制</b>的 <font face='SC'>MarrowFx</font> "
                       "与<b>星髓灼印</b> debuff。"],
    ], [30 * mm, W - LM - RM - 30 * mm]))
    F.append(P("本版确立了“特效必须<b>完全自制</b>”的规矩 —— 不抄 "
               "<font face='SC'>AureliaFx.detainImpact</font>，也不抄原版 "
               "<font face='SC'>Fx</font>。", "cap"))

    # ---------- 8.5.4 ----------
    F.append(P("8.5.4-beta　蠕者落点缩小 · 嬗变炉提速 · 星髓石扩用第二轮", "h1"))
    F.append(B("晶格蠕者落点爆炸 <b>×1/3</b>。"))
    F.append(B("晶格蠕者炮弹速度 <b>×1.25</b>，<b>射程不变</b>。"))
    F.append(B("废料嬗变炉<b>双倍速</b>，并且产出星髓石。"))
    F.append(B("拘留者：<b>生产成本</b>里加星髓石。"))
    F.append(B("星髓石扩用（第二轮）：进入 <b>22 个方块</b> 与 <b>13 个单位</b> 的配方。"))

    # ---------- 8.5.3 ----------
    F.append(P("8.5.3-beta　类星体枪口对齐 · 刷怪表重配", "h1"))
    F.append(B("类星体喷流：光束起点终于从枪口出来（枪口对齐）。"))
    F.append(B("刷怪表重配：<b>每一波都刷得出东西</b>；"
               "所有<b>有攻击能力、非海军、非玩家</b>的单位都可刷；"
               "拘留者每图 2 只上限；高级单位出现频率 <b>−25%</b>；"
               "整体难度约<b>减半</b>；<b>新手图封顶 T4</b>。"))
    F.append(B("单位属性冻结 —— 本版不动单位本身的数值。"))

    # ---------- 8.5.2 ----------
    F.append(P("8.5.2-beta　星髓石配方 · 流明桥重建 · 反拘留者 · 蠕者与堡垒重做", "h1"))
    F.append(B("星髓石配方（新增）。"))
    F.append(B("废料 → 矿的机器（新增）。"))
    F.append(B("流明桥 <b>3D 重建</b>。"))
    F.append(B("反拘留者炮塔 / 单位（新增）。"))
    F.append(B("晶格蠕者、堡垒驾驶员<b>重做</b>。"))

    # ---------- 8.5.1 ----------
    F.append(P("8.5.1-beta　两种新矿物 · 炮口对齐 · 共振枢纽降门槛", "h1"))
    F.append(B("两种新矿物：<b>废料</b> 与 <b>星髓石</b>。"))
    F.append(B("炮塔：<b>弹丸、炮口、火光三者对齐</b>。"))
    F.append(B("矿机：液体加速与挖掘速度提升。"))
    F.append(B("共振枢纽：激活门槛 <b>1000 → 300</b>。"))
    F.append(B("传送带 / 管道：格子的四条边严丝合缝。"))
    F.append(B("矿机动画：不挖矿的时候不再空转。"))
    F.append(B("刷怪：<b>29 个攻击单位全部可刷</b>。"))
    F.append(B("桌面 / 安卓双端同步。"))

    # ---------- 8.5 ----------
    F.append(P("8.5-beta　科技树死锁修复 · 两块贴图", "h1"))
    F.append(B("修复<b>三处科技树死锁</b>（“视界装配厂下面那批单位点不开”的问题）。"))
    F.append(B("流明焚化炉（Lumen Incinerator）、倒置分拣器（Inverted Sorter）贴图。"))

    # ---------- 验证 ----------
    F.append(P("8.5.6 交付前的完整验证", "h1"))
    F.append(table([
        ["检查项", "结果"],
        ["编译（JDK 17）", "0 错误"],
        ["模型检查 ModelCheck", "102 个方块模型，20 个图标，<b>0 问题</b>"],
        ["资源检查 AssetCheck", "461 个精灵，<b>0 缺失</b>"],
        ["光照烘焙 Bake", "<b>problems: 0</b>"],
        ["安卓 DEX 校验", "classes.dex 合法，<b>安卓可加载</b>"],
        ["无头服务器启动", "<b>1 mods loaded</b> / <b>Server loaded.</b>，0 错误"],
        ["星髓弹药注册（运行时）", "<b>2/2</b> 门炮塔（吸积炮 165+90，事件视界炮 320+190）"],
        ["全 32 张战役图生成", "尺寸 494～600，<b>缺矿 0，无刷怪门 0</b>"],
        ["刷怪审计（8 档 / 296 波）", "roster 全覆盖，<b>空波 0</b>"],
    ], [46 * mm, W - LM - RM - 46 * mm]))

    F.append(P("安装", "h1"))
    F.append(B("删除或替换旧的 Singularity JAR，只保留 "
               "<font face='SC'>Singularity-8.5.6-beta.jar</font>。"))
    F.append(B("改动只涉及音效时机、弹药表、两处伤害数值、地图尺寸和刷怪配置；"
               "<b>已建成的建筑不会变</b>，老存档也不需要重开"
               "（不过老存档仍然是旧尺寸的小图）。"))
    F.append(B("星髓石现在只有<b>吸积炮</b>和<b>事件视界炮</b>能用。"))

    F.append(P("实测数据存档", "h1"))
    F.append(B("<font face='SC'>docs/v856/wave-audit.txt</font> —— 刷怪审计"
               "（每威胁档的 8.5.5 / 8.5.6 对照 + 空波检查）。"))
    F.append(B("<font face='SC'>docs/v856/map-audit.txt</font> —— 全 32 张战役图的尺寸、"
               "生成耗时、五种矿的数量、刷怪门数。"))
    F.append(B("<font face='SC'>docs/quasar-geom-8.5.5.txt</font> —— 8.5.5 类星体几何证明。"))
    F.append(B("<font face='SC'>docs/fx-sizes-8.5.5.txt</font>、"
               "<font face='SC'>docs/fx-shot-measurements.txt</font> —— 8.5.5 特效尺寸实测。"))

    doc.build(F)
    print("written %s" % path)


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "docs/Singularity-8.5-8.5.6-beta-中文汇总.pdf"
    os.makedirs(os.path.dirname(out) or ".", exist_ok=True)
    build(out)
