#!/usr/bin/env python3
"""Builds the v8.3 showcase sheet from the sprites the build already baked."""
from PIL import Image, ImageDraw, ImageFont

S = "build/jar/sprites"
CJK = "/usr/share/fonts/opentype/noto/NotoSerifCJK-Bold.ttc"
LAT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

W, H = 1280, 940
BG = (11, 13, 20)
PANEL = (19, 23, 34)
EDGE = (44, 54, 76)
WHITE = (236, 242, 250)
BLUE = (143, 233, 255)
GREY = (150, 160, 178)
GOLD = (255, 216, 117)


def f(path, size):
    return ImageFont.truetype(path, size)


def sprite(name, folder="blocks/3d", box=None):
    im = Image.open(f"{S}/{folder}/{name}.png").convert("RGBA")
    if box:
        k = min(box / im.width, box / im.height)
        im = im.resize((max(1, int(im.width * k)), max(1, int(im.height * k))), Image.LANCZOS)
    return im


def panel(d, x0, y0, x1, y1, r=14):
    d.rounded_rectangle([x0, y0, x1, y1], r, fill=PANEL, outline=EDGE, width=2)


img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)

# background wash
for y in range(H):
    t = y / H
    d.line([(0, y), (W, y)], fill=(int(11 + 9 * t), int(13 + 11 * t), int(20 + 18 * t)))

# ---- header
d.text((44, 38), "奇点 · Singularity  8.3-beta", font=f(CJK, 44), fill=WHITE)
d.text((48, 96), "Industrial Junk Beta  ·  new in this build", font=f(LAT, 19), fill=BLUE)
d.line([(44, 128), (W - 44, 128)], fill=EDGE, width=2)

# ---- row 1: three headline cards
cards = [
    (sprite("aurelia-citadel-preview", box=168), "欧雷利亚堡垒城", "Aurelia Citadel Core",
     ["· 三级核心 6×6，盖在堡垒核心上升级", "· 储量 26000 / 单位上限 +54 / 28000 生命", "· 派出全新「城塞领航者」"]),
    (sprite("ward-dome-preview", box=168), "守护穹顶", "Ward Dome",
     ["· 26 格范围护盾，特效全部自写不刺眼", "· 7000 护盾上限，打空后破裂停机 14 秒", "· 冷却液扩圈 ×1.9，共振碎晶 +5200"]),
    (sprite("citadel-pilot", folder="units/3d", box=190), "城塞领航者", "Citadel Pilot",
     ["· 双涵道风扇运载机，机首切割头", "· 唯一能开采墙体矿脉的核心单位", "· 仍然采不了共振碎晶（需钻机）"]),
]

cx = 44
cw = (W - 88 - 2 * 18) // 3
for im, zh, en, lines in cards:
    panel(d, cx, 148, cx + cw, 502)
    img.paste(im, (cx + (cw - im.width) // 2, 168 + (190 - im.height) // 2), im)
    d.text((cx + 24, 372), zh, font=f(CJK, 25), fill=WHITE)
    d.text((cx + 24, 404), en, font=f(LAT, 16), fill=BLUE)
    yy = 430
    for ln in lines:
        d.text((cx + 24, yy), ln, font=f(CJK, 14), fill=GREY)
        yy += 22
    cx += cw + 18

# ---- row 2: the waste chain strip
panel(d, 44, 524, W - 44, 740)
d.text((68, 540), "废料循环 · 六步回收链", font=f(CJK, 24), fill=WHITE)
d.text((70, 574), "Waste cycle — waste is a by-product, and it takes six machines to become anything",
       font=f(LAT, 15), fill=BLUE)

chain = [
    ("industrial-waste", "items", "工业废料"),
    ("waste-reclaimer", "blocks/3d", "废料分拣厂"),
    ("swarf-furnace", "blocks/3d", "碎屑熔炉"),
    ("leach-tower", "blocks/3d", "矿渣淋洗塔"),
    ("slurry-crystalliser", "blocks/3d", "浆液结晶器"),
    ("resonance-resynthesiser", "blocks/3d", "共振再合成器"),
    ("prism-reformer", "blocks/3d", "棱晶再生炉"),
    ("prism-alloy", "items", "棱镜合金"),
]

n = len(chain)
x0, x1 = 72, W - 72
step = (x1 - x0) / n
for i, (name, folder, label) in enumerate(chain):
    im = sprite(name, folder, box=72 if folder != "items" else 44)
    px = int(x0 + step * i + (step - im.width) / 2)
    img.paste(im, (px, 612 + (76 - im.height) // 2), im)
    tw = d.textlength(label, font=f(CJK, 13))
    d.text((x0 + step * i + (step - tw) / 2, 696), label, font=f(CJK, 13),
           fill=GOLD if folder == "items" else WHITE)
    if i < n - 1:
        ax = int(x0 + step * (i + 1)) - 10
        d.line([(ax - 8, 648), (ax + 6, 648)], fill=EDGE, width=3)
        d.polygon([(ax + 6, 642), (ax + 14, 648), (ax + 6, 654)], fill=EDGE)

# ---- row 3: the rest, as bullets
panel(d, 44, 760, W - 44, 884)
bullets = [
    ("核心分级采矿", "任何核心都采不全：共振永远要钻机，墙体矿脉只有三级核心能挖"),
    ("地下水提取器 ×2.5", "7.00/s → 17.50/s，基础效率 0.4，任何地面都能抽水"),
    ("天文拘留者面板修复", "面板 / 设置 / 快捷键 / 跃迁选点 / 战术大脑全部回归，只改名称"),
]
yy = 782
for zh, note in bullets:
    d.text((70, yy), "◆", font=f(CJK, 14), fill=BLUE)
    d.text((94, yy), zh, font=f(CJK, 16), fill=BLUE)
    d.text((440, yy), note, font=f(CJK, 15), fill=WHITE)
    yy += 34

d.text((44, 900), "sayed1145 — 构想 / 架构设计 / 真机测试   ·   NLM — AI 工程实现（辅助）   ·   GPL-3.0",
       font=f(CJK, 14), fill=(110, 120, 140))

img.save("/home/user/v83-new-content.png")
print("written /home/user/v83-new-content.png", img.size)
