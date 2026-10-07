#!/usr/bin/env python3
"""v3 贴图：为每个建筑建立独立配色 + 材质，并生成全部动画部件。

解决 v2 的两个问题：
  1. 所有东西都是同一个灰紫金属 -> 每座建筑有自己的主色、副色、强调色
  2. 炮塔没有动画部件 -> 生成 -barrel-l/-r、-ring、-spin、-blade、
     -emitter、-vent、-nozzle、-ring1/2、-arm、-core 等 RegionPart 贴图

这个文件在 gen_sprites.py 之后运行，覆盖/补充其产物。
"""
import math, os, random
from PIL import Image, ImageDraw, ImageFilter, ImageChops

OUT = os.path.join(os.path.dirname(__file__), "..", "assets", "sprites")
os.makedirs(OUT, exist_ok=True)
SS = 4

HOT   = (255, 246, 224)
ACC   = (255, 177,  78)
RIM   = (255,  90,  43)
HAWK  = (138, 216, 255)
DEGEN = (207, 165, 255)
VOID  = (140, 120, 255)
OUTL  = ( 18,  14,  24)


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def cl(c):
    return tuple(max(0, min(255, int(v))) for v in c)


# ---------------------------------------------------------------- 材质档案
class Mat:
    """一套金属材质：暗部/中间/亮部/高光 + 强调色。"""

    def __init__(self, dark, mid, lite, hi, accent, glowc):
        self.dark, self.mid, self.lite, self.hi = dark, mid, lite, hi
        self.accent, self.glow = accent, glowc


# 每座建筑独立配色，避免"全是一个色"
MAT = {
    # 生产链：偏工业冷灰 -> 紫（简并）
    "press":     Mat((44, 42, 54), (86, 82, 104), (132, 126, 158), (178, 172, 208), DEGEN, DEGEN),
    # 冷凝器：偏青蓝低温
    "condenser": Mat((36, 48, 60), (68, 92, 112), (104, 140, 168), (150, 194, 222), HAWK, HAWK),
    # 锻炉：暗铜金 + 橙
    "forge":     Mat((56, 44, 36), (104, 80, 58), (150, 118, 84), (204, 168, 122), ACC, ACC),
    # 吸积炮：铁灰 + 橙热
    "accretion": Mat((46, 44, 52), (88, 84, 98), (134, 128, 148), (184, 178, 202), ACC, ACC),
    # 帧拖曳：紫调轻甲
    "dragger":   Mat((44, 40, 58), (84, 76, 112), (128, 118, 164), (176, 166, 212), DEGEN, DEGEN),
    # 霍金发射器：白蓝科技感
    "hawking":   Mat((40, 50, 62), (76, 96, 118), (118, 146, 176), (168, 200, 230), HAWK, HAWK),
    # 类星体：深红黑 + 炽白
    "quasar":    Mat((52, 36, 36), (96, 66, 62), (140, 100, 92), (192, 148, 134), RIM, HOT),
    # 事件视界：近黑重甲 + 金
    "horizon":   Mat((32, 30, 40), (62, 58, 76), (98, 94, 120), (142, 138, 168), ACC, HOT),
    # 船坞：重工业绿灰
    "yard":      Mat((40, 48, 46), (76, 92, 86), (116, 138, 130), (162, 188, 178), HAWK, HAWK),
    # 装配厂
    "assembler": Mat((42, 44, 54), (80, 84, 102), (122, 128, 152), (170, 176, 204), VOID, VOID),
    # 单位
    "unit_l":    Mat((44, 42, 58), (84, 80, 110), (128, 124, 162), (176, 172, 212), HAWK, HAWK),
    "unit_h":    Mat((38, 32, 46), (74, 62, 88), (114, 98, 134), (160, 142, 182), ACC, ACC),
    "unit_tank": Mat((42, 46, 44), (80, 88, 84), (120, 132, 126), (166, 180, 172), DEGEN, DEGEN),
}


class Canvas:
    def __init__(self, w, h=None):
        h = w if h is None else h
        self.w, self.h = w, h
        self.img = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img, "RGBA")

    def s(self, v):
        return v * SS

    def ellipse(self, cx, cy, r, fill=None, outline=None, width=1, ry=None):
        ry = r if ry is None else ry
        self.d.ellipse([self.s(cx - r), self.s(cy - ry), self.s(cx + r), self.s(cy + ry)],
                       fill=fill, outline=outline, width=max(1, int(self.s(width))))

    def rect(self, x0, y0, x1, y1, fill=None, outline=None, width=1, radius=0):
        b = [self.s(x0), self.s(y0), self.s(x1), self.s(y1)]
        if radius > 0:
            self.d.rounded_rectangle(b, radius=self.s(radius), fill=fill,
                                     outline=outline, width=max(1, int(self.s(width))))
        else:
            self.d.rectangle(b, fill=fill, outline=outline, width=max(1, int(self.s(width))))

    def poly(self, pts, fill=None, outline=None, width=1):
        self.d.polygon([(self.s(x), self.s(y)) for x, y in pts],
                       fill=fill, outline=outline, width=max(1, int(self.s(width))))

    def line(self, x0, y0, x1, y1, fill, width=1):
        self.d.line([self.s(x0), self.s(y0), self.s(x1), self.s(y1)],
                    fill=fill, width=max(1, int(self.s(width))))

    def arc(self, cx, cy, r, a0, a1, fill, width=1):
        self.d.arc([self.s(cx - r), self.s(cy - r), self.s(cx + r), self.s(cy + r)],
                   a0, a1, fill=fill, width=max(1, int(self.s(width))))

    def finish(self):
        return self.img.resize((self.w, self.h), Image.LANCZOS)


def grad_rect(cv, x0, y0, x1, y1, m, radius=2, vert=True, flip=False):
    """带定向光照的金属块（顶亮底暗）。"""
    steps = max(2, int(cv.s(y1 - y0)) if vert else int(cv.s(x1 - x0)))
    layer = Image.new("RGBA", cv.img.size, (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer, "RGBA")
    for i in range(steps):
        t = i / max(1, steps - 1)
        if flip:
            t = 1 - t
        col = lerp(lerp(m.mid, m.hi, 0.38), lerp(m.mid, (0, 0, 0), 0.55), t ** 0.9)
        if vert:
            yy = y0 + (i / steps) * (y1 - y0)
            ld.line([cv.s(x0), cv.s(yy), cv.s(x1), cv.s(yy)], fill=cl(col) + (255,), width=SS)
        else:
            xx = x0 + (i / steps) * (x1 - x0)
            ld.line([cv.s(xx), cv.s(y0), cv.s(xx), cv.s(y1)], fill=cl(col) + (255,), width=SS)
    mask = Image.new("L", cv.img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [cv.s(x0), cv.s(y0), cv.s(x1), cv.s(y1)], radius=cv.s(radius), fill=255)
    layer.putalpha(ImageChops.multiply(layer.split()[3], mask))
    cv.img.alpha_composite(layer)
    cv.rect(x0, y0, x1, y1, outline=OUTL + (255,), width=0.5, radius=radius)


def panel_lines(cv, x0, y0, x1, y1, m, n=3, horiz=True):
    """面板分缝，增加细节密度。"""
    for i in range(1, n):
        f = i / n
        if horiz:
            y = y0 + (y1 - y0) * f
            cv.line(x0 + 0.6, y, x1 - 0.6, y, cl(lerp(m.mid, (0, 0, 0), 0.55)) + (170,), 0.32)
            cv.line(x0 + 0.6, y + 0.35, x1 - 0.6, y + 0.35, cl(m.hi) + (60,), 0.28)
        else:
            x = x0 + (x1 - x0) * f
            cv.line(x, y0 + 0.6, x, y1 - 0.6, cl(lerp(m.mid, (0, 0, 0), 0.55)) + (170,), 0.32)


def rivets(cv, pts, m, r=1.0):
    for cx, cy in pts:
        cv.ellipse(cx, cy, r, fill=cl(lerp(m.mid, (0, 0, 0), 0.35)) + (255,),
                   outline=OUTL + (190,), width=0.3)
        cv.ellipse(cx - r * 0.22, cy - r * 0.22, r * 0.4, fill=cl(m.hi) + (255,))


def vent_grill(cv, x0, y0, x1, y1, m, n=4):
    """散热格栅。"""
    cv.rect(x0, y0, x1, y1, fill=cl(lerp(m.dark, (0, 0, 0), 0.4)) + (255,),
            outline=OUTL + (200,), width=0.35, radius=0.6)
    for i in range(n):
        f = (i + 0.5) / n
        y = y0 + (y1 - y0) * f
        cv.line(x0 + 0.5, y, x1 - 0.5, y, cl(m.lite) + (150,), 0.3)


def glow_layer(img, radius=1.6, strength=0.45):
    bl = img.filter(ImageFilter.GaussianBlur(radius))
    return Image.blend(img, ImageChops.lighter(img, bl), strength)


def noise(img, amount=4, seed=0):
    random.seed(seed)
    px = img.load()
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            r, g, b, a = px[x, y]
            if a < 8:
                continue
            n = random.randint(-amount, amount)
            px[x, y] = (max(0, min(255, r + n)), max(0, min(255, g + n)),
                        max(0, min(255, b + n)), a)
    return img


def save(img, name):
    img.save(os.path.join(OUT, name + ".png"))
    print("  ", name + ".png", img.size)


def emissive(cv, cx, cy, r, col, ry=None):
    """自发光元件：核心白 + 外圈色。"""
    ry = r if ry is None else ry
    cv.ellipse(cx, cy, r * 1.8, ry=ry * 1.8, fill=cl(col) + (40,))
    cv.ellipse(cx, cy, r * 1.25, ry=ry * 1.25, fill=cl(col) + (110,))
    cv.ellipse(cx, cy, r, ry=ry, fill=cl(col) + (255,), outline=OUTL + (150,), width=0.25)
    cv.ellipse(cx - r * 0.2, cy - ry * 0.2, r * 0.42, ry=ry * 0.42,
               fill=cl(lerp(col, (255, 255, 255), 0.75)) + (255,))


def mini_hole(cv, cx, cy, r, tilt=0.34):
    """小尺寸黑洞图形（部件上用）。"""
    for i in range(24, 0, -1):
        f = i / 24
        rr = r * (1.3 + f * 1.9)
        for k in range(0, 360, 8):
            a = math.radians(k)
            dopp = 0.3 + 0.7 * (0.5 - 0.5 * math.cos(a))
            br = (dopp ** 1.5) * (1 - f * 0.7)
            if br < 0.06:
                continue
            col = lerp(lerp(HOT, ACC, min(1, f * 2)), RIM, max(0, f * 2 - 1))
            x = cx + math.cos(a) * rr
            y = cy + math.sin(a) * rr * tilt
            cv.ellipse(x, y, r * 0.11, fill=cl(col) + (int(185 * br),))
    cv.ellipse(cx, cy, r * 1.5, outline=cl(HOT) + (230,), width=0.3)
    cv.ellipse(cx, cy, r, fill=(0, 0, 0, 255))


# ================================================================ 炮塔底座
def turret_base(tiles, key, seed=0):
    """炮塔底座：八边形重甲 + 螺栓 + 散热格栅 + 强调色灯带。"""
    s = tiles * 32
    m = MAT[key]
    cv = Canvas(s)
    random.seed(seed)
    c = s / 2
    pad = 1.0

    # 八边形主体
    k = s * 0.205
    pts = [(pad + k, pad), (s - pad - k, pad), (s - pad, pad + k), (s - pad, s - pad - k),
           (s - pad - k, s - pad), (pad + k, s - pad), (pad, s - pad - k), (pad, pad + k)]
    # 渐变填充
    layer = Image.new("RGBA", cv.img.size, (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer, "RGBA")
    for i in range(int(cv.s(s))):
        t = i / cv.s(s)
        col = lerp(lerp(m.mid, m.hi, 0.42), lerp(m.mid, (0, 0, 0), 0.58), t ** 0.85)
        ld.line([0, i, cv.s(s), i], fill=cl(col) + (255,), width=1)
    mask = Image.new("L", cv.img.size, 0)
    ImageDraw.Draw(mask).polygon([(cv.s(x), cv.s(y)) for x, y in pts], fill=255)
    layer.putalpha(mask)
    cv.img.alpha_composite(layer)
    cv.poly(pts, outline=OUTL + (255,), width=0.6)

    # 内嵌环形凹槽
    inset = s * 0.13
    cv.ellipse(c, c, c - inset, fill=cl(lerp(m.dark, (0, 0, 0), 0.25)) + (235,),
               outline=OUTL + (210,), width=0.45)
    cv.ellipse(c, c, c - inset - 0.7, outline=cl(m.lite) + (90,), width=0.35)

    # 四角散热格栅
    g = s * 0.115
    for sx, sy in [(-1, -1), (1, -1), (-1, 1), (1, 1)]:
        gx = c + sx * (c - g * 1.25)
        gy = c + sy * (c - g * 1.25)
        vent_grill(cv, gx - g * 0.62, gy - g * 0.52, gx + g * 0.62, gy + g * 0.52, m, 3)

    # 强调色灯带（左右）
    for sx in (-1, 1):
        lx = c + sx * (c - s * 0.075)
        cv.rect(lx - s * 0.016, c - s * 0.12, lx + s * 0.016, c + s * 0.12,
                fill=cl(m.accent) + (220,), radius=0.4)

    # 螺栓
    r = 1.0 + tiles * 0.12
    off = s * 0.155
    rivets(cv, [(off, off), (s - off, off), (off, s - off), (s - off, s - off)], m, r)

    img = cv.finish()
    return noise(img, 4, seed)


# ================================================================ 各炮塔顶
def top_accretion(tiles=3):
    """吸积炮顶：双管基座 + 弹链仓。"""
    s = tiles * 32
    m = MAT["accretion"]
    cv = Canvas(s)
    c = s / 2
    # 中央机匣
    grad_rect(cv, c - s * 0.20, c - s * 0.20, c + s * 0.20, c + s * 0.26, m, radius=s * 0.05)
    panel_lines(cv, c - s * 0.20, c - s * 0.20, c + s * 0.20, c + s * 0.26, m, 3)
    # 侧面弹链仓
    for sx in (-1, 1):
        x = c + sx * s * 0.235
        grad_rect(cv, x - s * 0.06, c - s * 0.05, x + s * 0.06, c + s * 0.20, m, radius=s * 0.02)
    # 顶部瞄具
    emissive(cv, c, c - s * 0.10, s * 0.035, m.accent)
    rivets(cv, [(c - s * 0.14, c + s * 0.19), (c + s * 0.14, c + s * 0.19)], m, 0.9)
    return noise(glow_layer(cv.finish(), 1.1, 0.35), 3, 7)


def part_barrel(length, width, key, heat=True, seed=0):
    """独立炮管部件（供 RegionPart 使用，绘制在画布上半部）。"""
    m = MAT[key]
    w = int(width + 6)
    h = int(length + 6)
    cv = Canvas(w, h)
    cx = w / 2
    x0, x1 = cx - width / 2, cx + width / 2
    y0, y1 = 3.0, 3.0 + length

    grad_rect(cv, x0, y0, x1, y1, m, radius=width * 0.3, vert=False)
    # 中轴高光
    cv.rect(x0 + width * 0.18, y0 + 0.5, x0 + width * 0.34, y1 - 0.5,
            fill=cl(m.hi) + (185,))
    cv.rect(x1 - width * 0.3, y0 + 0.5, x1 - width * 0.14, y1 - 0.5,
            fill=cl(lerp(m.mid, (0, 0, 0), 0.55)) + (165,))
    # 散热环
    for i in range(3):
        yy = y0 + length * (0.30 + i * 0.16)
        cv.rect(x0 - width * 0.10, yy, x1 + width * 0.10, yy + width * 0.13,
                fill=cl(m.lite) + (225,), outline=OUTL + (170,), width=0.28, radius=0.3)
    # 炮口
    cv.rect(x0 - width * 0.14, y0, x1 + width * 0.14, y0 + width * 0.40,
            radius=width * 0.16, fill=cl(lerp(m.accent, (0, 0, 0), 0.25)) + (255,),
            outline=OUTL + (255,), width=0.36)
    if heat:
        cv.rect(x0 + width * 0.2, y0 + width * 0.07, x1 - width * 0.2, y0 + width * 0.30,
                fill=cl(m.accent) + (255,))
    return noise(glow_layer(cv.finish(), 0.9, 0.35), 3, seed)


def part_ring(size, key, teeth=8, inner=0.45, seed=0):
    """旋转环部件：齿环 + 强调色节点。"""
    m = MAT[key]
    cv = Canvas(size)
    c = size / 2
    ro = size * 0.44
    ri = ro * inner
    cv.ellipse(c, c, ro, fill=cl(m.mid) + (255,), outline=OUTL + (255,), width=0.5)
    cv.ellipse(c, c, ro * 0.86, outline=cl(m.hi) + (120,), width=0.35)
    # 齿
    for i in range(teeth):
        a = math.radians(i * 360 / teeth)
        x = c + math.cos(a) * ro * 0.93
        y = c + math.sin(a) * ro * 0.93
        cv.rect(x - size * 0.045, y - size * 0.045, x + size * 0.045, y + size * 0.045,
                fill=cl(m.lite) + (255,), outline=OUTL + (190,), width=0.3, radius=0.5)
        if i % 2 == 0:
            cv.ellipse(x, y, size * 0.019, fill=cl(m.accent) + (255,))
    # 中空
    cv.ellipse(c, c, ri, fill=(0, 0, 0, 0))
    cv.ellipse(c, c, ri, outline=OUTL + (220,), width=0.4)
    return noise(glow_layer(cv.finish(), 1.0, 0.4), 3, seed)


def part_blade(w, h, key, seed=0):
    """聚焦叶片（霍金发射器）。"""
    m = MAT[key]
    cv = Canvas(w, h)
    pts = [(w * 0.5, 1.0), (w * 0.86, h * 0.42), (w * 0.66, h - 1.0), (w * 0.34, h - 1.0),
           (w * 0.14, h * 0.42)]
    cv.poly(pts, fill=cl(m.mid) + (255,), outline=OUTL + (255,), width=0.5)
    cv.poly([(w * 0.5, 2.2), (w * 0.74, h * 0.44), (w * 0.5, h * 0.62), (w * 0.26, h * 0.44)],
            fill=cl(m.lite) + (215,))
    emissive(cv, w * 0.5, h * 0.44, w * 0.09, m.accent)
    return noise(glow_layer(cv.finish(), 1.0, 0.45), 3, seed)


def part_plate(w, h, key, accent_bar=True, seed=0):
    """通用装甲臂/板部件。"""
    m = MAT[key]
    cv = Canvas(w, h)
    grad_rect(cv, 1.0, 1.0, w - 1.0, h - 1.0, m, radius=min(w, h) * 0.16)
    panel_lines(cv, 1.0, 1.0, w - 1.0, h - 1.0, m, 3)
    if accent_bar:
        cv.rect(w * 0.36, h * 0.12, w * 0.64, h * 0.30,
                fill=cl(m.accent) + (235,), radius=0.4)
    rivets(cv, [(w * 0.26, h * 0.82), (w * 0.74, h * 0.82)], m, max(0.7, w * 0.05))
    return noise(glow_layer(cv.finish(), 0.9, 0.3), 3, seed)


# ================================================================ 炮塔总装
def gen_turrets():
    print("turrets (v3):")

    # ---- 1. 吸积炮 3x3 ----
    save(turret_base(3, "accretion", 11), "accretion-cannon-base")
    save(top_accretion(3), "accretion-cannon")
    save(part_barrel(30, 9, "accretion", seed=12), "accretion-cannon-barrel-l")
    save(part_barrel(30, 9, "accretion", seed=13), "accretion-cannon-barrel-r")
    save(part_ring(40, "accretion", teeth=8, inner=0.42, seed=14), "accretion-cannon-ring")

    # ---- 2. 帧拖曳炮 2x2 ----
    save(turret_base(2, "dragger", 21), "frame-dragger-base")
    m = MAT["dragger"]
    cv = Canvas(64)
    c = 32
    grad_rect(cv, c - 11, c - 10, c + 11, c + 13, m, radius=3)
    panel_lines(cv, c - 11, c - 10, c + 11, c + 13, m, 3)
    emissive(cv, c, c - 4, 2.4, m.accent)
    rivets(cv, [(c - 7, c + 9), (c + 7, c + 9)], m, 0.9)
    save(noise(glow_layer(cv.finish(), 1.0, 0.35), 3, 22), "frame-dragger")
    save(part_barrel(22, 7, "dragger", seed=23), "frame-dragger-barrel")
    save(part_ring(30, "dragger", teeth=6, inner=0.46, seed=24), "frame-dragger-spin")

    # ---- 3. 霍金发射器 3x3 ----
    save(turret_base(3, "hawking", 31), "hawking-emitter-base")
    m = MAT["hawking"]
    cv = Canvas(96)
    c = 48
    grad_rect(cv, c - 15, c - 14, c + 15, c + 18, m, radius=4)
    panel_lines(cv, c - 15, c - 14, c + 15, c + 18, m, 4)
    vent_grill(cv, c - 10, c + 6, c + 10, c + 15, m, 3)
    emissive(cv, c, c - 6, 3.6, m.accent)
    save(noise(glow_layer(cv.finish(), 1.2, 0.4), 3, 32), "hawking-emitter")
    save(part_blade(16, 34, "hawking", seed=33), "hawking-emitter-blade")
    save(part_barrel(34, 11, "hawking", seed=34), "hawking-emitter-emitter")

    # ---- 4. 类星体喷流 4x4 ----
    save(turret_base(4, "quasar", 41), "quasar-lance-base")
    m = MAT["quasar"]
    cv = Canvas(128)
    c = 64
    grad_rect(cv, c - 20, c - 18, c + 20, c + 24, m, radius=5)
    panel_lines(cv, c - 20, c - 18, c + 20, c + 24, m, 4)
    vent_grill(cv, c - 13, c + 8, c + 13, c + 21, m, 4)
    emissive(cv, c, c - 8, 4.6, m.accent)
    save(noise(glow_layer(cv.finish(), 1.4, 0.42), 3, 42), "quasar-lance")
    save(part_ring(56, "quasar", teeth=10, inner=0.40, seed=43), "quasar-lance-ring")
    save(part_plate(14, 30, "quasar", seed=44), "quasar-lance-vent")
    save(part_barrel(42, 15, "quasar", seed=45), "quasar-lance-nozzle")

    # ---- 5. 事件视界炮 5x5 ----
    save(turret_base(5, "horizon", 51), "event-horizon-base")
    m = MAT["horizon"]
    s = 160
    cv = Canvas(s)
    c = s / 2
    # 重甲机匣
    grad_rect(cv, c - 26, c - 24, c + 26, c + 30, m, radius=6)
    panel_lines(cv, c - 26, c - 24, c + 26, c + 30, m, 5)
    for sx in (-1, 1):
        vent_grill(cv, c + sx * 17 - 6, c + 8, c + sx * 17 + 6, c + 26, m, 4)
    rivets(cv, [(c - 20, c - 17), (c + 20, c - 17), (c - 20, c + 25), (c + 20, c + 25)], m, 1.5)
    save(noise(glow_layer(cv.finish(), 1.5, 0.4), 3, 52), "event-horizon")
    save(part_ring(88, "horizon", teeth=12, inner=0.46, seed=53), "event-horizon-ring1")
    save(part_ring(70, "horizon", teeth=8, inner=0.40, seed=54), "event-horizon-ring2")
    save(part_plate(18, 40, "horizon", seed=55), "event-horizon-arm")
    # 中央黑洞核心（发光部件）
    cv = Canvas(44)
    mini_hole(cv, 22, 22, 6.2, 0.36)
    save(glow_layer(cv.finish(), 1.8, 0.6), "event-horizon-core")

    # 保留 heat 贴图（DrawTurret 需要）
    for n, key, sz in [("accretion-cannon", "accretion", 96), ("frame-dragger", "dragger", 64),
                       ("hawking-emitter", "hawking", 96), ("quasar-lance", "quasar", 128),
                       ("event-horizon", "horizon", 160)]:
        mm = MAT[key]
        cv = Canvas(sz)
        cc = sz / 2
        cv.ellipse(cc, cc - sz * 0.06, sz * 0.05, fill=cl(mm.glow) + (255,))
        save(cv.finish(), n + "-heat")


# ================================================================ 工厂
def gen_crafters():
    print("crafters (v3):")

    def body(tiles, key, seed):
        s = tiles * 32
        m = MAT[key]
        cv = Canvas(s)
        pad = 1.0
        grad_rect(cv, pad, pad, s - pad, s - pad, m, radius=2.5 + tiles * 0.55)
        # 内嵌工作区
        p = 3.0 + tiles * 0.5
        cv.rect(p, p, s - p, s - p,
                fill=cl(lerp(m.dark, (0, 0, 0), 0.30)) + (240,),
                outline=OUTL + (215,), width=0.5, radius=2)
        cv.rect(p + 0.7, p + 0.7, s - p - 0.7, s - p - 0.7,
                outline=cl(m.lite) + (95,), width=0.35, radius=2)
        # 侧向管线
        for sx in (-1, 1):
            x = s / 2 + sx * (s / 2 - p * 0.52)
            cv.rect(x - s * 0.018, p + 1.4, x + s * 0.018, s - p - 1.4,
                    fill=cl(m.lite) + (200,), radius=0.4)
        off = 2.6 + tiles * 0.45
        rivets(cv, [(off, off), (s - off, off), (off, s - off), (s - off, s - off)],
               m, 1.0 + tiles * 0.1)
        return cv, m, s

    # 引力压机 2x2：上下压板
    cv, m, s = body(2, "press", 61)
    c = s / 2
    for sy in (-1, 1):
        y = c + sy * s * 0.20
        cv.rect(c - s * 0.24, y - s * 0.055, c + s * 0.24, y + s * 0.055,
                fill=cl(m.lite) + (255,), outline=OUTL + (220,), width=0.4, radius=0.8)
        cv.rect(c - s * 0.20, y - s * 0.028, c + s * 0.20, y - s * 0.004,
                fill=cl(m.hi) + (170,))
    emissive(cv, c, c, s * 0.055, m.accent)
    save(noise(glow_layer(cv.finish(), 1.1, 0.35), 4, 61), "graviton-press")

    # 霍金冷凝器 3x3：低温腔 + 冷凝盘管
    cv, m, s = body(3, "condenser", 62)
    c = s / 2
    cv.ellipse(c, c, s * 0.235, fill=cl(lerp(m.dark, HAWK, 0.16)) + (255,),
               outline=OUTL + (235,), width=0.5)
    for i in range(4):
        r = s * (0.085 + i * 0.045)
        cv.ellipse(c, c, r, outline=cl(lerp(m.lite, HAWK, 0.4)) + (170 - i * 28,), width=0.35)
    emissive(cv, c, c, s * 0.048, HAWK)
    for i in range(6):
        a = math.radians(i * 60 + 15)
        x, y = c + math.cos(a) * s * 0.30, c + math.sin(a) * s * 0.30
        cv.ellipse(x, y, s * 0.021, fill=cl(HAWK) + (225,), outline=OUTL + (170,), width=0.25)
    save(noise(glow_layer(cv.finish(), 1.3, 0.42), 4, 62), "hawking-condenser")

    # 奇点锻炉 4x4：约束环 + 中央黑洞位
    cv, m, s = body(4, "forge", 63)
    c = s / 2
    for i, rr in enumerate([0.30, 0.245, 0.19]):
        cv.ellipse(c, c, s * rr, outline=cl(lerp(m.lite, ACC, 0.35)) + (210 - i * 40,), width=0.5)
    # 六个约束柱
    for i in range(6):
        a = math.radians(i * 60)
        x, y = c + math.cos(a) * s * 0.265, c + math.sin(a) * s * 0.265
        cv.rect(x - s * 0.028, y - s * 0.028, x + s * 0.028, y + s * 0.028,
                fill=cl(m.lite) + (255,), outline=OUTL + (215,), width=0.32, radius=0.5)
        cv.ellipse(x, y, s * 0.012, fill=cl(ACC) + (255,))
    cv.ellipse(c, c, s * 0.135, fill=(6, 5, 10, 255), outline=OUTL + (255,), width=0.45)
    mini_hole(cv, c, c, s * 0.058, 0.35)
    save(noise(glow_layer(cv.finish(), 1.6, 0.48), 4, 63), "singularity-forge")

    # 视界装配厂 3x3
    cv, m, s = body(3, "assembler", 64)
    c = s / 2
    pts = [(c + math.cos(math.radians(a)) * s * 0.26,
            c + math.sin(math.radians(a)) * s * 0.26) for a in range(0, 360, 60)]
    cv.poly(pts, fill=cl(lerp(m.dark, VOID, 0.13)) + (250,), outline=OUTL + (235,), width=0.5)
    cv.poly([(c + (x - c) * 0.66, c + (y - c) * 0.66) for x, y in pts],
            outline=cl(VOID) + (165,), width=0.35)
    emissive(cv, c, c, s * 0.05, VOID)
    for i in range(3):
        a = math.radians(i * 120 + 30)
        x, y = c + math.cos(a) * s * 0.33, c + math.sin(a) * s * 0.33
        cv.rect(x - s * 0.030, y - s * 0.030, x + s * 0.030, y + s * 0.030,
                fill=cl(m.lite) + (255,), outline=OUTL + (205,), width=0.3, radius=0.6)
    save(noise(glow_layer(cv.finish(), 1.2, 0.4), 4, 64), "horizon-assembler")

    # 奇点船坞 5x5：重型龙门架
    cv, m, s = body(5, "yard", 65)
    c = s / 2
    # 龙门横梁
    for sy in (-1, 1):
        y = c + sy * s * 0.215
        grad_rect(cv, c - s * 0.33, y - s * 0.040, c + s * 0.33, y + s * 0.040, m, radius=1.0)
    # 纵向滑轨
    for sx in (-1, 1):
        x = c + sx * s * 0.20
        cv.rect(x - s * 0.020, c - s * 0.25, x + s * 0.020, c + s * 0.25,
                fill=cl(lerp(m.dark, (0, 0, 0), 0.3)) + (245,),
                outline=OUTL + (195,), width=0.3, radius=0.4)
        for i in range(5):
            yy = c - s * 0.21 + i * s * 0.105
            cv.line(x - s * 0.018, yy, x + s * 0.018, yy, cl(m.lite) + (170,), 0.26)
    # 中央装配台
    cv.rect(c - s * 0.115, c - s * 0.115, c + s * 0.115, c + s * 0.115,
            fill=cl(lerp(m.dark, HAWK, 0.10)) + (250,),
            outline=OUTL + (225,), width=0.45, radius=1.2)
    for i in range(4):
        a = math.radians(i * 90 + 45)
        emissive(cv, c + math.cos(a) * s * 0.075, c + math.sin(a) * s * 0.075, s * 0.016, HAWK)
    save(noise(glow_layer(cv.finish(), 1.4, 0.42), 4, 65), "citadel-yard")


# ================================================================ 单位
def air_unit(size, key, heavy=False, reactor=False, seed=0):
    """朝上的飞行器：机身 + 机翼 + 尾翼 + 座舱 + 引擎。"""
    m = MAT[key]
    cv = Canvas(size)
    c = size / 2
    L = size * 0.42
    W = size * (0.125 if heavy else 0.155)

    # 主翼
    sweep = 0.62 if heavy else 0.50
    wing = [(c, c - L * 0.30),
            (c + W * (3.6 if heavy else 3.0), c + L * sweep),
            (c + W * (2.5 if heavy else 2.1), c + L * 0.74),
            (c, c + L * 0.42)]
    cv.poly(wing, fill=cl(lerp(m.mid, (0, 0, 0), 0.22)) + (255,), outline=OUTL + (255,), width=0.45)
    cv.poly([(2 * c - x, y) for x, y in wing],
            fill=cl(lerp(m.mid, (0, 0, 0), 0.34)) + (255,), outline=OUTL + (255,), width=0.45)
    # 翼面高光
    cv.line(c + W * 0.5, c - L * 0.18, c + W * 2.4, c + L * 0.50, cl(m.hi) + (120,), 0.4)

    # 机身
    body = [(c, c - L), (c + W, c - L * 0.30), (c + W * 0.88, c + L * 0.72),
            (c, c + L * 0.92), (c - W * 0.88, c + L * 0.72), (c - W, c - L * 0.30)]
    layer_fill(cv, body, m)
    cv.poly(body, outline=OUTL + (255,), width=0.5)

    # 座舱
    ck = 0.42 if heavy else 0.56
    cv.poly([(c, c - L * 0.70), (c + W * ck, c - L * 0.30),
             (c, c - L * 0.14), (c - W * ck, c - L * 0.30)],
            fill=cl(lerp(m.accent, (0, 0, 0), 0.50)) + (255,), outline=OUTL + (220,), width=0.4)
    cv.poly([(c, c - L * 0.64), (c + W * ck * 0.6, c - L * 0.34), (c - W * ck * 0.15, c - L * 0.30)],
            fill=cl(lerp(m.accent, (255, 255, 255), 0.6)) + (235,))

    if heavy:
        cv.line(c, c - L * 0.98, c, c - L * 0.36, cl(lerp(m.mid, m.hi, 0.5)) + (200,), 0.45)
        for sx in (-1, 1):
            cv.line(c + sx * W * 0.42, c - L * 0.60, c + sx * W * 0.62, c + L * 0.50,
                    cl(lerp(m.mid, (0, 0, 0), 0.5)) + (160,), 0.4)
        # 肩部挂架
        for sx in (-1, 1):
            x = c + sx * W * 1.5
            cv.rect(x - size * 0.022, c - L * 0.10, x + size * 0.022, c + L * 0.26,
                    fill=cl(m.lite) + (255,), outline=OUTL + (210,), width=0.3, radius=0.5)

    if reactor:
        cv.ellipse(c, c + L * 0.24, size * 0.095, fill=(8, 6, 12, 255),
                   outline=OUTL + (255,), width=0.55)
        mini_hole(cv, c, c + L * 0.24, size * 0.038, 0.34)

    # 尾部引擎
    eo = size * (0.075 if heavy else 0.085)
    for sx in ((-1, 1) if not heavy else (-1, 1)):
        x = c + sx * eo
        emissive(cv, x, c + L * 0.80, size * (0.030 if heavy else 0.026), m.glow)

    return noise(glow_layer(cv.finish(), 1.3 if heavy else 1.0, 0.38), 3, seed)


def layer_fill(cv, pts, m):
    """多边形的定向渐变填充。"""
    layer = Image.new("RGBA", cv.img.size, (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer, "RGBA")
    ys = [p[1] for p in pts]
    y0, y1 = min(ys), max(ys)
    for i in range(int(cv.s(y1 - y0)) + 1):
        t = i / max(1, cv.s(y1 - y0))
        col = lerp(lerp(m.mid, m.hi, 0.40), lerp(m.mid, (0, 0, 0), 0.50), t ** 0.85)
        yy = cv.s(y0) + i
        ld.line([0, yy, cv.img.size[0], yy], fill=cl(col) + (255,), width=1)
    mask = Image.new("L", cv.img.size, 0)
    ImageDraw.Draw(mask).polygon([(cv.s(x), cv.s(y)) for x, y in pts], fill=255)
    layer.putalpha(mask)
    cv.img.alpha_composite(layer)


def cell_sprite(size, oy, rx, ry):
    cv = Canvas(size)
    c = size / 2
    cv.ellipse(c, c + oy, rx, ry=ry, fill=(255, 255, 255, 255))
    return cv.finish()


def tank_hull(size, key, seed=0, turret_ring=True):
    """履带式载具车体（朝上）。"""
    m = MAT[key]
    cv = Canvas(size)
    c = size / 2
    hw = size * 0.215   # 半宽（车体）
    hl = size * 0.40    # 半长

    # 车体主装甲（前端收窄）
    body = [(c - hw * 0.72, c - hl), (c + hw * 0.72, c - hl),
            (c + hw, c - hl * 0.42), (c + hw, c + hl * 0.86),
            (c + hw * 0.80, c + hl), (c - hw * 0.80, c + hl),
            (c - hw, c + hl * 0.86), (c - hw, c - hl * 0.42)]
    layer_fill(cv, body, m)
    cv.poly(body, outline=OUTL + (255,), width=0.55)

    # 前装甲斜面高光
    cv.poly([(c - hw * 0.70, c - hl * 0.97), (c + hw * 0.70, c - hl * 0.97),
             (c + hw * 0.86, c - hl * 0.52), (c - hw * 0.86, c - hl * 0.52)],
            fill=cl(lerp(m.lite, m.hi, 0.35)) + (150,))

    # 纵向加强筋
    for sx in (-1, 1):
        cv.line(c + sx * hw * 0.55, c - hl * 0.80, c + sx * hw * 0.62, c + hl * 0.86,
                cl(lerp(m.mid, (0, 0, 0), 0.5)) + (175,), 0.4)
    panel_lines(cv, c - hw * 0.8, c - hl * 0.35, c + hw * 0.8, c + hl * 0.80, m, 4)

    # 后部散热
    vent_grill(cv, c - hw * 0.55, c + hl * 0.52, c + hw * 0.55, c + hl * 0.88, m, 4)

    # 炮塔座圈
    if turret_ring:
        cv.ellipse(c, c - hl * 0.04, size * 0.135,
                   fill=cl(lerp(m.dark, (0, 0, 0), 0.3)) + (255,),
                   outline=OUTL + (235,), width=0.5)
        cv.ellipse(c, c - hl * 0.04, size * 0.115, outline=cl(m.lite) + (120,), width=0.35)

    rivets(cv, [(c - hw * 0.80, c - hl * 0.72), (c + hw * 0.80, c - hl * 0.72),
                (c - hw * 0.80, c + hl * 0.72), (c + hw * 0.80, c + hl * 0.72)],
           m, size * 0.018)
    return cv, m, c, hw, hl


def tread_frames(w, h, key, frames=4, seed=0):
    """履带动画帧：链节向后滚动。"""
    m = MAT[key]
    out = []
    pitch = h / 7.0
    for f in range(frames):
        cv = Canvas(int(w), int(h))
        # 履带底色
        cv.rect(0.4, 0.4, w - 0.4, h - 0.4,
                fill=cl(lerp(m.dark, (0, 0, 0), 0.35)) + (255,),
                outline=OUTL + (255,), width=0.4, radius=1.2)
        off = (f / frames) * pitch
        y = -pitch + off
        i = 0
        while y < h:
            cv.rect(1.0, y + pitch * 0.18, w - 1.0, y + pitch * 0.72,
                    fill=cl(m.lite if i % 2 == 0 else m.mid) + (255,),
                    outline=OUTL + (150,), width=0.25, radius=0.4)
            # 链节高光
            cv.line(1.4, y + pitch * 0.26, w - 1.4, y + pitch * 0.26, cl(m.hi) + (110,), 0.22)
            y += pitch
            i += 1
        out.append(noise(cv.finish(), 3, seed + f))
    return out


def gen_units():
    print("units (v3):")

    # 透镜号（轻型空中）
    s = 38
    save(air_unit(s, "unit_l", heavy=False, seed=41), "lensing")
    save(cell_sprite(s, -s * 0.10, 2.4, 3.0), "lensing-cell")
    m = MAT["unit_l"]
    w = Canvas(16)
    grad_rect(w, 5.2, 1.6, 10.8, 13.4, m, radius=1.6)
    w.rect(6.2, 2.4, 7.4, 12.4, fill=cl(m.hi) + (175,))
    w.rect(6.0, 1.0, 10.0, 2.9, radius=0.6, fill=cl(m.accent) + (255,),
           outline=OUTL + (205,), width=0.3)
    save(glow_layer(w.finish(), 0.9, 0.35), "lensing-weapon")

    # 能层号（重型空中）
    s = 78
    save(air_unit(s, "unit_h", heavy=True, reactor=True, seed=42), "ergosphere")
    save(cell_sprite(s, -s * 0.13, 3.4, 4.2), "ergosphere-cell")
    m = MAT["unit_h"]
    w = Canvas(32)
    grad_rect(w, 10.5, 3.2, 21.5, 27.8, m, radius=2.6)
    w.rect(12.2, 4.6, 14.0, 26.4, fill=cl(m.hi) + (175,))
    w.rect(19.0, 4.6, 20.4, 26.4, fill=cl(lerp(m.mid, (0, 0, 0), 0.5)) + (160,))
    grad_rect(w, 12.0, 1.0, 20.0, 11.0, m, radius=1.8)
    w.ellipse(16, 5.4, 3.4, fill=(8, 6, 12, 255), outline=OUTL + (255,), width=0.45)
    mini_hole(w, 16, 5.4, 1.35, 0.34)
    save(glow_layer(w.finish(), 1.2, 0.42), "ergosphere-weapon")

    # ---- 吸积车（履带支援车）----
    s = 52
    cv, m, c, hw, hl = tank_hull(s, "unit_tank", seed=44)
    # 车头推土铲
    cv.rect(c - hw * 0.92, c - hl * 1.10, c + hw * 0.92, c - hl * 0.92,
            fill=cl(m.lite) + (255,), outline=OUTL + (230,), width=0.4, radius=0.6)
    save(noise(glow_layer(cv.finish(), 1.1, 0.34), 3, 44), "accretor")
    save(cell_sprite(s, hl * 0.30, 2.6, 3.2), "accretor-cell")
    for i, im in enumerate(tread_frames(18, 60, "unit_tank", 4, 440)):
        save(im, f"accretor-treads{i + 1}")
    # 维修炮塔
    w = Canvas(26)
    mm = MAT["unit_tank"]
    grad_rect(w, 7.0, 5.0, 19.0, 21.0, mm, radius=2.2)
    panel_lines(w, 7.0, 5.0, 19.0, 21.0, mm, 3)
    grad_rect(w, 11.2, 0.8, 14.8, 8.0, mm, radius=0.9)
    emissive(w, 13, 3.0, 1.3, HAWK)
    emissive(w, 13, 14.0, 1.9, mm.accent)
    save(noise(glow_layer(w.finish(), 1.0, 0.4), 3, 45), "accretor-weapon")

    # ================================================================
    # ★ 堡垒号 移动作战平台
    # ================================================================
    s = 150
    cv, m, c, hw, hl = tank_hull(s, "unit_h", seed=46, turret_ring=False)

    # 甲板结构：前部指挥塔 + 中部主炮座 + 后部动力/维修舱
    # 前甲板斜装甲
    cv.poly([(c - hw * 0.62, c - hl * 0.92), (c + hw * 0.62, c - hl * 0.92),
             (c + hw * 0.50, c - hl * 0.62), (c - hw * 0.50, c - hl * 0.62)],
            fill=cl(lerp(m.lite, m.hi, 0.25)) + (200,), outline=OUTL + (190,), width=0.35)

    # 主炮座圈（中央）
    cv.ellipse(c, c + hl * 0.03, s * 0.125,
               fill=cl(lerp(m.dark, (0, 0, 0), 0.32)) + (255,),
               outline=OUTL + (240,), width=0.55)
    cv.ellipse(c, c + hl * 0.03, s * 0.105, outline=cl(m.lite) + (130,), width=0.4)
    for i in range(12):
        a = math.radians(i * 30)
        cv.ellipse(c + math.cos(a) * s * 0.115, c + hl * 0.03 + math.sin(a) * s * 0.115,
                   s * 0.008, fill=cl(m.lite) + (215,))

    # 两侧副炮座
    for sx in (-1, 1):
        cv.ellipse(c + sx * hw * 0.68, c - hl * 0.42, s * 0.050,
                   fill=cl(lerp(m.dark, (0, 0, 0), 0.25)) + (255,),
                   outline=OUTL + (225,), width=0.4)
        cv.ellipse(c + sx * hw * 0.72, c + hl * 0.48, s * 0.055,
                   fill=cl(lerp(m.dark, (0, 0, 0), 0.25)) + (255,),
                   outline=OUTL + (225,), width=0.4)

    # 后部动力/维修舱门（纯车体结构，不生产单位）
    cv.rect(c - hw * 0.40, c + hl * 0.66, c + hw * 0.40, c + hl * 0.94,
            fill=cl(lerp(m.dark, (0, 0, 0), 0.42)) + (255,),
            outline=OUTL + (235,), width=0.45, radius=0.8)
    for i in range(3):
        yy = c + hl * (0.71 + i * 0.075)
        cv.line(c - hw * 0.35, yy, c + hw * 0.35, yy, cl(m.accent) + (190,), 0.3)

    # 六边护盾投射器（四角）
    for sx, sy in [(-1, -1), (1, -1), (-1, 1), (1, 1)]:
        px = c + sx * hw * 0.86
        py = c + sy * hl * 0.66
        pts = [(px + math.cos(math.radians(a)) * s * 0.032,
                py + math.sin(math.radians(a)) * s * 0.032) for a in range(0, 360, 60)]
        cv.poly(pts, fill=cl(lerp(m.dark, HAWK, 0.22)) + (255,), outline=OUTL + (215,), width=0.35)
        emissive(cv, px, py, s * 0.012, HAWK)

    save(noise(glow_layer(cv.finish(), 1.8, 0.45), 3, 46), "citadel")
    save(cell_sprite(s, hl * 0.30, 5.0, 6.2), "citadel-cell")

    # 双侧履带
    for i, im in enumerate(tread_frames(34, 156, "unit_h", 4, 460)):
        save(im, f"citadel-treads{i + 1}")

    # 主炮（种黑洞的大炮）
    w = Canvas(64)
    mm = MAT["unit_h"]
    grad_rect(w, 18, 20, 46, 52, mm, radius=4)
    panel_lines(w, 18, 20, 46, 52, mm, 4)
    # 炮管
    grad_rect(w, 27, 2, 37, 26, mm, radius=2)
    w.rect(29, 3, 31, 24, fill=cl(mm.hi) + (170,))
    w.rect(34, 3, 35.6, 24, fill=cl(lerp(mm.mid, (0, 0, 0), 0.5)) + (160,))
    # 炮口约束环
    for i, rr in enumerate([5.0, 3.8]):
        w.ellipse(32, 5.5, rr, outline=cl(ACC) + (220 - i * 60,), width=0.5)
    # 后部奇点腔
    w.ellipse(32, 40, 7.0, fill=(6, 5, 10, 255), outline=OUTL + (255,), width=0.5)
    mini_hole(w, 32, 40, 2.8, 0.35)
    for i in range(6):
        a = math.radians(i * 60 + 20)
        emissive(w, 32 + math.cos(a) * 9.5, 40 + math.sin(a) * 9.5, 1.1, ACC)
    save(noise(glow_layer(w.finish(), 1.6, 0.5), 3, 47), "citadel-main")

    # 对空副炮
    w = Canvas(22)
    grad_rect(w, 6.5, 7.0, 15.5, 18.0, mm, radius=1.8)
    for sx in (-1, 1):
        x = 11 + sx * 2.4
        grad_rect(w, x - 1.1, 1.2, x + 1.1, 9.0, mm, radius=0.6)
        w.rect(x - 0.9, 1.0, x + 0.9, 2.4, fill=cl(HAWK) + (255,), radius=0.3)
    emissive(w, 11, 13.5, 1.5, HAWK)
    save(noise(glow_layer(w.finish(), 1.0, 0.42), 3, 48), "citadel-aa")

    # 对地副炮
    w = Canvas(26)
    grad_rect(w, 7.0, 8.0, 19.0, 21.0, mm, radius=2.0)
    panel_lines(w, 7.0, 8.0, 19.0, 21.0, mm, 3)
    grad_rect(w, 10.6, 1.5, 15.4, 10.0, mm, radius=1.2)
    w.rect(10.2, 1.2, 15.8, 3.2, radius=0.5, fill=cl(lerp(DEGEN, (0, 0, 0), 0.2)) + (255,),
           outline=OUTL + (215,), width=0.3)
    w.rect(11.4, 1.6, 14.6, 2.6, fill=cl(DEGEN) + (255,))
    emissive(w, 13, 15.0, 1.7, DEGEN)
    save(noise(glow_layer(w.finish(), 1.1, 0.42), 3, 49), "citadel-side")


if __name__ == "__main__":
    gen_turrets()
    gen_crafters()
    gen_units()

    # v4.1：最后覆盖泰坦履带源图和独立动画部件，确保履带切片坐标与 BHUnits 一致。
    from gen_titan_animation import main as gen_titan_animation
    gen_titan_animation()
    print("\nv4.1 贴图生成完成。")
