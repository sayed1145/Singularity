#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成 8.5.5-beta 的中文汇总 PDF。

用法：
    python3 tools/make_v855_pdf.py [输出路径]

字体：Noto Sans SC（SIL Open Font License）。自动下载变量字体并实例化 Regular / Bold，
缓存在 --font-dir（默认 /tmp/bhfonts），不写入仓库。PDF 内嵌子集。
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
                                Spacer, Table, TableStyle)

VF_URL = ("https://github.com/google/fonts/raw/main/ofl/notosanssc/"
          "NotoSansSC%5Bwght%5D.ttf")
FONT_DIR = os.environ.get("BH_FONT_DIR", "/tmp/bhfonts")


def ensure_fonts():
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
        instancer.instantiateVariableFont(f, {"wght": w}, inplace=True, updateFontNames=True)
        f.save(out)
        print("  %s %.1f MB" % (os.path.basename(out), os.path.getsize(out) / 1e6))
    return reg, bold


REG, BOLD = ensure_fonts()
pdfmetrics.registerFont(TTFont("SC", REG))
pdfmetrics.registerFont(TTFont("SC-B", BOLD))
pdfmetrics.registerFontFamily("SC", normal="SC", bold="SC-B")

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
stNote = S("n", fontSize=8.8, leading=14, textColor=MUTED, leftIndent=8, spaceAfter=5)
stCell = S("c", fontSize=8.6, leading=13)
stCellB = S("cb", fontName="SC-B", fontSize=8.6, leading=13)
stHead = S("hd", fontName="SC-B", fontSize=8.8, leading=13, textColor=colors.HexColor("#243b55"))


def P(t, s=stP):
    return Paragraph(t, s)


def B(t):
    return Paragraph(t, stLi, bulletText="•")


def cells(rows, header, widths, align_right=()):
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


def build(story):
    A = story.append

    A(P("奇点 · Singularity", stTitle))
    A(Spacer(1, 3))
    A(P("8.5.5-beta 更新汇总　·　Mindustry v160.5", stSub))
    A(Spacer(1, 8))

    A(P("这一版做了四件事：修好类星体喷流（它之前根本开不了火）、把晶格蠕者的攻击特效"
        "整套重写、让星髓石可以当炮弹用，并且给这发炮弹配了一套完全自制的特效、"
        "过百的伤害和一个全新的减益状态。", stP))
    A(P("下面每一项都附了<b>实测数据</b>：特效大小不是照代码估的，是用无 GL 的录制型 Batch "
        "把每个特效逐帧跑一遍量出来的；枪口位置也是直接算出来的。", stP))

    # ------------------------------------------------------------------ 1
    A(P("1　类星体喷流：修好了，现在能开火也有开火特效", stH1))
    A(P("症状是“开不了火，而且一点开火特效都没有”。原因不在伤害、射程或者耗电，"
        "而在<b>枪口的坐标算错了</b>。", stP))
    A(P("连续光束炮和普通炮不一样：引擎每一刻都会把光束按 <font face='SC-B'>shootY</font>"
        "（沿炮管方向的偏移）重新摆一次位置。这个模组为了让 3D 炮口和画面上的炮塔对得上，"
        "要用相机透视把炮口往上抬。v8.5.3 那版抬的时候写成了：", stP))
    A(P("<font face='SC-B'>ny = y + camCy + (b.y − camCy) × s</font>", stNote))
    A(P("问题是 <font face='SC-B'>b.y</font> 是一个<b>世界坐标</b>（比如 2000），"
        "而 <font face='SC-B'>camCy</font> 是相对炮塔的（−78.4）。正确写法要<b>先减掉炮塔中心</b>，"
        "取相对偏移再投影。下面是直接算出来的对照：", stP))
    A(cells([
        ["v8.5.3（错误的写法）", "1000, 2000", "离炮塔 <b>2142 像素</b>（<b>268 格</b>）"],
        ["<b>v8.5.5（正确）</b>", "1000, 2000", "离炮塔 <b>21 像素</b>（<b>2.6 格</b>）"],
    ], ["写法", "炮塔所在位置", "算出来的枪口落在哪"], [130, 90, 204]))
    A(Spacer(1, 5))
    A(P("光束被甩到两百多格之外 —— 看不见、也打不到任何东西，看起来就是“根本没开火”。"
        "修正之后枪口正好落在塔头该在的地方（2.6 格，对一座 4×4 的炮塔来说是正常的）。", stP))
    A(P("顺带修的另外两点：", stP))
    A(B("长度改成<b>从枪口量</b>。引擎原本从炮塔中心量，只挪起点不改量法的话，"
        "光束长度每一刻都会把抬升量再累加一次，会自己越长越长。"))
    A(B("补上了<b>枪口开火特效</b>。之前连续光束炮的开火特效被“3D 炮口”那套逻辑摘走了，"
        "而那条路径又不管光束炮 —— 结果就是谁都不播。现在由方块自己在枪口补播一个全新的 "
        "<font face='SC-B'>AureliaFx.quasarMuzzle</font>（三道聚焦环向内收束，峰值半径 13.7 像素）。"))
    A(P("伤害、射程、耗电、射速<b>一个都没动</b>，改的只有几何和特效。", stNote))

    # ------------------------------------------------------------------ 2
    A(P("2　晶格蠕者：攻击特效整套重写，而且真的变小了", stH1))
    A(P("8.5.4 那次已经把落点爆炸“缩了 1/3”，但打起来还是一大团。原因查出来了："
        "原来那套特效把 <font face='SC-B'>e.rotation</font> <b>当成半径在用</b>。", stP))
    A(P("<font face='SC-B'>Lines.circle(e.x, e.y, 4f + e.fin() × (e.rotation + 26f))</font>", stNote))
    A(P("炮弹的 rotation 是 0～360 的<b>朝向角</b>，不是尺寸。所以那朵爆炸的真实大小"
        "是随炮弹朝哪个方向飞而变的 —— 除以 3 只是缩放了一个会乱跳的量。", stP))
    A(P("所以这一版<b>从零重写</b>（新文件 <font face='SC-B'>CrawlerFx.java</font>），"
        "所有半径都是写死的常数，跟朝向完全无关。下面是<b>实测</b>的峰值半径"
        "（8 像素 = 1 格），换五个朝向各量一遍：", stP))
    A(cells([
        ["枪口火光", "9.0", "2.0", "<b>1.6</b>"],
        ["枪口硝烟", "11.6", "3.9", "<b>2.8</b>"],
        ["落点爆炸", "389.0　（10～389）", "129.7　（10～130）", "<b>9.6</b>　（恒定）"],
        ["破片命中", "14.0", "14.0", "<b>1.7</b>"],
    ], ["特效", "8.5.1 原版", "8.5.4（÷3）", "<b>8.5.5（重写）</b>"], [86, 118, 108, 112]))
    A(Spacer(1, 5))
    A(P("落点爆炸相对 8.5.4 小了 <b>13.5 倍</b>，相对 8.5.1 原版小了 <b>40 倍</b>。"
        "更要紧的是括号里那一列：8.5.4 只有炮弹<b>碰巧朝北飞</b>的时候才是 10 像素，"
        "换个朝向就一路涨到 130 像素（16 格）。8.5.5 在任何朝向下都是 9.6 像素（1.2 格）。", stP))
    A(P("数字是怎么量出来的：", stP))
    A(B("<b>顶点法</b>（<font face='SC-B'>tools/preview/src/preview/FxSize.java</font>）："
        "用无 GL 的录制型 Batch 接管 <font face='SC-B'>Core.batch</font>，把每个特效从 0 "
        "跑到寿命结束，收下所有顶点，算离中心最远的那个有多远；换五个朝向各跑一遍。"
        "“半径随朝向变化”就是这么抓出来的。"))
    A(B("<b>像素法</b>（<font face='SC-B'>FxShot.java</font>）：把同一批特效光栅化成 PNG"
        "（<font face='SC-B'>docs/v855/shots/</font>），再按像素独立量一遍作为交叉验证。"
        "两种方法在同一朝向下给出 9.6 / 10.6 像素，吻合。"))
    A(P("顺带一提，<font face='SC-B'>AureliaFx.detainImpact</font> 原样保留 —— "
        "定锚 spike 和 Detainer 还在用它，那边 26 格的溅射是设计如此。", stNote))

    # ------------------------------------------------------------------ 3
    A(P("3　星髓石可以当炮弹了", stH1))
    A(P("星髓石原来是纯建造材料。这一版把它注册成了<b>四门炮弹炮塔</b>的弹药："
        "吸积炮、参考系拖曳炮、视界、以及 <font face='SC-B'>shardVolley</font>。"
        "打出去的叫<b>星髓长枪</b>。", stP))

    A(P("3.1　伤害过百", stH2))
    A(cells([
        ["直伤", "<b>165</b>", "需求是“伤害过百”"],
        ["范围伤害 / 半径", "90 / 26 像素（3.25 格）", "命中后额外炸一圈"],
        ["射程", "300 像素（37.5 格）", "介于吸积炮 235 与视界 400 之间"],
        ["每份矿几发", "4 发", "星髓石稀有，所以一份顶四发"],
        ["对建筑", "×0.75", "打建筑略软"],
    ], ["项", "值", "说明"], [110, 150, 164]))
    A(Spacer(1, 5))

    A(P("3.2　附带 buff：星髓灼印", stH2))
    A(P("命中后给目标烙上一个新的减益状态（<font face='SC-B'>marrow-brand</font>，"
        "中文名“星髓灼印”），持续 6 秒：", stP))
    A(B("<b>持续灼烧</b>：约 45 点/秒，6 秒共约 270 点。"))
    A(B("<b>减速 38%</b>：跑不掉，也追不上。"))
    A(B("<b>输出 −28%、射速 −35%</b>：它自己打人也变软。"))
    A(P("把直伤 165 + 范围 90 + 灼烧 270 加起来，一发的总期望伤害约 <b>525</b>。", stNote))
    A(P("这里有个坑值得一提：网上查到的资料说状态效果有 <font face='SC-B'>armorMultiplier</font>"
        "（护甲倍率）这个字段，我一开始就是照着写的，结果编译不过 —— "
        "把 <font face='SC-B'>javap</font> 打开 v160.5 的实际 class 一看，"
        "这个字段<b>根本不存在</b>。最后用的是确实存在的 "
        "<font face='SC-B'>damage / intervalDamage / speedMultiplier / damageMultiplier / "
        "reloadMultiplier</font>。所以文档不能全信，得对着 jar 核一遍。", stNote))

    A(P("3.3　攻击特效完全自制", stH2))
    A(P("新文件 <font face='SC-B'>MarrowFx.java</font>，六个特效全部从零手写，"
        "没有一个是拿现成特效缩放或改色得来的。造型语言取自星髓石本身的设定"
        "（恒星残骸：冷却了几十亿年的暗壳，剖开处露出惨白的星髓）—— "
        "<b>惨白的芯 + 冷紫的外壳 + 一圈正在塌陷的暗环</b>，"
        "三个颜色常量全组共用，所以枪口、弹道、落点一眼能认出是同一种弹药。", stP))
    A(cells([
        ["shoot（炮口）", "六角星塌陷 + 两条反向自转的环 + 暗壳推开", "11.5"],
        ["wake（弹道尾迹）", "一粒惨白余烬拖着暗壳碎屑", "5.4"],
        ["hit（命中）", "八芒星爆开 + 双层冲击环 + 12 粒碎壳 + 留在地上的暗痕", "<b>31.1</b>"],
        ["despawn（射程尽头）", "半个爆闪，没有冲击环", "19.8"],
        ["brand（灼印持续中）", "单位身上冒火星 + 缓慢脉动的环", "6.5"],
        ["brandApply（烙上瞬间）", "收紧的环 + 四角星", "13.8"],
    ], ["特效", "画的是什么", "峰值半径（像素）"], [128, 226, 96]))
    A(Spacer(1, 5))
    A(P("同样是实测值，而且同样<b>不随朝向变化</b>。图在 "
        "<font face='SC-B'>docs/v855/shots/fx-marrow.png</font>。", stNote))

    # ------------------------------------------------------------------ 4
    A(P("4　查过的资料", stH1))
    A(cells([
        ["ContinuousTurret.java（v160.5 源码）",
         "确认 <font face='SC-B'>updateBullet()</font> 每一刻都按 shootX/shootY 重算光束起点，"
         "以及引擎是从炮塔中心量长度的 —— 这两点决定了类星体喷流该怎么修"],
        ["ItemTurret.java（v160.5 源码）",
         "<font face='SC-B'>ammo()</font> 怎么建弹药表、<font face='SC-B'>ammoPerShot</font> / "
         "<font face='SC-B'>maxAmmo</font> 的语义，以及消耗器什么时候被冻结"],
        ["StatusEffect / BulletType 文档",
         "状态效果与弹药的字段清单；<b>其中 armorMultiplier 一条是错的</b>，已对着 jar 纠正"],
        ["javap 反编译 v160.5 的 jar",
         "核实 <font face='SC-B'>StatusEffect</font> 到底有哪些字段，"
         "以及 <font face='SC-B'>Effect$EffectContainer.set()</font> 的参数顺序"],
    ], ["来源", "用来确认什么"], [168, 282]))

    # ------------------------------------------------------------------ 5
    A(P("5　验证", stH1))
    A(P("提交前跑过的检查，全部通过：", stP))
    A(cells([
        ["编译（JDK 17）", "0 错误 0 警告"],
        ["模型检查 ModelCheck", "<b>102 个方块模型，20 个图标，0 问题</b>"],
        ["资源检查 AssetCheck", "461 个精灵，0 缺失"],
        ["光照烘焙 Bake", "<b>problems: 0</b>"],
        ["安卓 DEX 校验", "classes.dex 合法，<b>安卓可加载</b>（1415 个类）"],
        ["无头服务器启动", "<b>1 mods loaded</b> / <b>Server loaded.</b>，0 错误"],
        ["星髓弹药注册（运行时日志）",
         "<b>4/4 门炮塔</b>已注册，伤害 165 + 范围 90"],
    ], ["检查项", "结果"], [168, 282]))
    A(Spacer(1, 5))
    A(P("测试数据都留在仓库里可以复跑：<font face='SC-B'>docs/fx-sizes-8.5.5.txt</font>"
        "（特效半径实测）、<font face='SC-B'>docs/quasar-geom-8.5.5.txt</font>"
        "（枪口几何对照）、<font face='SC-B'>docs/fx-shot-measurements.txt</font>"
        "（像素法交叉验证）。", stNote))

    A(P("6　安装", stH1))
    A(B("删除或替换旧的 Singularity JAR，只保留 "
        "<font face='SC-B'>Singularity-8.5.5-beta.jar</font>。"))
    A(B("改动只涉及特效几何、弹药表和状态效果；<b>已建成的建筑不会变</b>，"
        "老存档也不需要重开。"))
    A(B("星髓石现在既是建造材料也是弹药 —— 往上面四门炮塔里塞就能用。"))


def footer(canv, doc):
    canv.saveState()
    canv.setStrokeColor(LINE)
    canv.setLineWidth(0.4)
    canv.line(18 * mm, 14 * mm, A4[0] - 18 * mm, 14 * mm)
    canv.setFont("SC", 7.8)
    canv.setFillColor(MUTED)
    canv.drawString(18 * mm, 10 * mm, "奇点 Singularity · 8.5.5-beta 更新汇总 · Mindustry v160.5")
    canv.drawRightString(A4[0] - 18 * mm, 10 * mm, "第 %d 页" % doc.page)
    canv.restoreState()


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "docs/Singularity-8.5.5-beta-中文汇总.pdf"
    os.makedirs(os.path.dirname(out) or ".", exist_ok=True)
    doc = BaseDocTemplate(out, pagesize=A4,
                          leftMargin=18 * mm, rightMargin=18 * mm,
                          topMargin=16 * mm, bottomMargin=20 * mm,
                          title="奇点 Singularity 8.5.5-beta 更新汇总",
                          author="Singularity Mod")
    frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="n")
    doc.addPageTemplates([PageTemplate(id="all", frames=[frame], onPage=footer)])
    story = []
    build(story)
    doc.build(story)
    print("wrote %s (%d bytes)" % (out, os.path.getsize(out)))


if __name__ == "__main__":
    main()
