#!/usr/bin/env python3
"""blackhole 模组全部贴图（Mindustry 规格：1 tile = 32px）。

v2 全面重做：改用超采样(SSAA 4x) + 真实光照模型(定向光+边缘光+环境遮蔽)绘制，
而不是直接在小画布上画硬线条，因此边缘干净、体积感明显、更贴近原版美术。
"""
import math, os, random
from PIL import Image, ImageDraw, ImageFilter, ImageChops

OUT = os.path.join(os.path.dirname(__file__), "..", "assets", "sprites")
os.makedirs(OUT, exist_ok=True)

SS = 4  # 超采样倍数

# ---- 调色板 ----
HOT    = (255, 246, 224)
ACC    = (255, 177,  78)
RIM    = (255,  90,  43)
HAWK   = (138, 216, 255)
DEGEN  = (207, 165, 255)
OUTL   = ( 18,  14,  24)   # 描边（Mindustry 惯例：近黑）
# 金属基色：暗冷灰，带一点紫调，贴合黑洞主题
M_DARK = ( 48,  46,  60)
M_MID  = ( 82,  80,  99)
M_LITE = (124, 122, 146)
M_HI   = (170, 168, 192)

LIGHT = (-0.55, -0.78)  # 光照方向（左上）


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def clamp255(c):
    return tuple(max(0, min(255, int(v))) for v in c)


class Canvas:
    """超采样画布。所有绘制在 SS 倍尺寸上进行，最后 LANCZOS 缩回。"""

    def __init__(self, size):
        self.size = size
        self.S = size * SS
        self.img = Image.new("RGBA", (self.S, self.S), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img, "RGBA")

    def s(self, v):
        return v * SS

    def ellipse(self, cx, cy, r, fill=None, outline=None, width=1, ry=None):
        ry = r if ry is None else ry
        b = [self.s(cx - r), self.s(cy - ry), self.s(cx + r), self.s(cy + ry)]
        self.d.ellipse(b, fill=fill, outline=outline, width=int(self.s(width)))

    def rect(self, x0, y0, x1, y1, fill=None, outline=None, width=1, radius=0):
        b = [self.s(x0), self.s(y0), self.s(x1), self.s(y1)]
        if radius > 0:
            self.d.rounded_rectangle(b, radius=self.s(radius), fill=fill,
                                     outline=outline, width=int(self.s(width)))
        else:
            self.d.rectangle(b, fill=fill, outline=outline, width=int(self.s(width)))

    def poly(self, pts, fill=None, outline=None, width=1):
        p = [(self.s(x), self.s(y)) for x, y in pts]
        self.d.polygon(p, fill=fill, outline=outline, width=int(self.s(width)))

    def line(self, x0, y0, x1, y1, fill, width=1):
        self.d.line([self.s(x0), self.s(y0), self.s(x1), self.s(y1)],
                    fill=fill, width=int(self.s(width)))

    def arc(self, cx, cy, r, a0, a1, fill, width=1):
        b = [self.s(cx - r), self.s(cy - r), self.s(cx + r), self.s(cy + r)]
        self.d.arc(b, a0, a1, fill=fill, width=int(self.s(width)))

    def finish(self):
        return self.img.resize((self.size, self.size), Image.LANCZOS)


def shade_plate(cv, x0, y0, x1, y1, base, radius=3, rim_light=True):
    """带光照的金属板：定向渐变 + 顶部高光 + 底部阴影 + 边缘光。"""
    w, h = x1 - x0, y1 - y0
    steps = int(cv.s(h))
    for i in range(steps):
        t = i / max(1, steps - 1)
        # 顶亮底暗
        col = lerp(lerp(base, M_HI, 0.30), lerp(base, (0, 0, 0), 0.52), t ** 0.85)
        yy = y0 + (i / steps) * h
        cv.d.line([cv.s(x0), cv.s(yy), cv.s(x1), cv.s(yy)], fill=clamp255(col) + (255,), width=SS)
    # 圆角遮罩
    mask = Image.new("L", (cv.S, cv.S), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [cv.s(x0), cv.s(y0), cv.s(x1), cv.s(y1)], radius=cv.s(radius), fill=255)
    cv.img.putalpha(ImageChops.multiply(cv.img.split()[3], mask))
    # 描边
    cv.rect(x0, y0, x1, y1, outline=OUTL + (255,), width=0.55, radius=radius)
    if rim_light:
        # 左上边缘光
        cv.d.arc([cv.s(x0 + 0.4), cv.s(y0 + 0.4), cv.s(x1 - 0.4), cv.s(y1 - 0.4)],
                 150, 300, fill=clamp255(lerp(base, M_HI, 0.75)) + (190,), width=int(cv.s(0.5)))


def plate(size_tiles, base=M_MID, seed=0, panel=True, rivets=True):
    """标准方块底板。"""
    s = size_tiles * 32
    cv = Canvas(s)
    random.seed(seed)
    m = 0.8
    shade_plate(cv, m, m, s - m, s - m, base, radius=2.5 + size_tiles * 0.5)

    if panel:
        # 内嵌面板，制造层次
        p = 3.2 + size_tiles * 0.35
        cv.rect(p, p, s - p, s - p, outline=clamp255(lerp(base, (0, 0, 0), 0.45)) + (220,),
                width=0.5, radius=2)
        cv.rect(p + 0.6, p + 0.6, s - p - 0.6, s - p - 0.6,
                outline=clamp255(lerp(base, M_HI, 0.35)) + (110,), width=0.4, radius=2)

    if rivets:
        off = 3.0 + size_tiles * 0.35
        for cx, cy in [(off, off), (s - off, off), (off, s - off), (s - off, s - off)]:
            cv.ellipse(cx, cy, 1.15, fill=clamp255(lerp(base, (0, 0, 0), 0.3)) + (255,),
                       outline=OUTL + (200,), width=0.35)
            cv.ellipse(cx - 0.25, cy - 0.25, 0.42, fill=clamp255(M_HI) + (255,))
    return cv


def add_noise(img, amount=5, seed=0):
    """轻微噪点，去掉塑料感。"""
    random.seed(seed)
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 8:
                continue
            n = random.randint(-amount, amount)
            px[x, y] = (max(0, min(255, r + n)), max(0, min(255, g + n)),
                        max(0, min(255, b + n)), a)
    return img


def glow(img, radius=2, strength=0.55):
    """发光层：模糊自身叠加，制造辉光。"""
    bl = img.filter(ImageFilter.GaussianBlur(radius))
    return Image.blend(img, ImageChops.lighter(img, bl), strength)


def save(img, name):
    img.save(os.path.join(OUT, name + ".png"))
    print("  ", name + ".png", img.size)


# ================================================================ 黑洞图形
def draw_blackhole(cv, cx, cy, r, tilt=0.32, rings=True):
    """静态 3D 黑洞（与游戏内渲染器同款外观）。"""
    # 吸积盘：多层椭圆，带多普勒不对称
    steps = 46
    for i in range(steps, 0, -1):
        f = i / steps
        rr = r * (1.35 + f * 2.15)
        for k in range(0, 360, 4):
            a = math.radians(k)
            # 多普勒：左侧(180°)更亮
            dopp = 0.3 + 0.7 * (0.5 - 0.5 * math.cos(a))
            br = (dopp ** 1.5) * (1 - f * 0.72)
            if br < 0.04:
                continue
            col = lerp(lerp(HOT, ACC, min(1, f * 2)), RIM, max(0, f * 2 - 1))
            col = lerp(col, (255, 255, 255), max(0, br - 0.55) * 1.1)
            x = cx + math.cos(a) * rr
            y = cy + math.sin(a) * rr * tilt
            cv.d.ellipse([cv.s(x - r * 0.10), cv.s(y - r * 0.10),
                          cv.s(x + r * 0.10), cv.s(y + r * 0.10)],
                         fill=clamp255(col) + (int(190 * br),))
    if rings:
        # 顶部透镜像
        for rr, al, wd in [(r * 1.5, 235, 0.42), (r * 1.78, 135, 0.34), (r * 2.05, 70, 0.3)]:
            cv.arc(cx, cy, rr, 188, 352, clamp255(HOT) + (al,), wd)
        # 光子环
        cv.ellipse(cx, cy, r * 1.5, outline=clamp255(HOT) + (255,), width=0.42)
    # 事件视界
    cv.ellipse(cx, cy, r, fill=(0, 0, 0, 255))


# ================================================================ 物品
def item_icon(name, kind):
    s = 16
    cv = Canvas(s)
    c = s / 2
    random.seed(hash(name) & 0xffff)

    if kind == "degenerate":
        # 致密晶体：六边形，内有晶格
        pts = [(c + math.cos(math.radians(a - 90)) * 6.2,
                c + math.sin(math.radians(a - 90)) * 6.2) for a in range(0, 360, 60)]
        cv.poly(pts, fill=clamp255(lerp(DEGEN, (0, 0, 0), 0.45)) + (255,),
                outline=OUTL + (255,), width=0.55)
        inner = [(c + math.cos(math.radians(a - 90)) * 3.6,
                  c + math.sin(math.radians(a - 90)) * 3.6) for a in range(0, 360, 60)]
        cv.poly(inner, fill=clamp255(DEGEN) + (255,))
        cv.poly([(c, c - 3.6), (c + 3.1, c - 1.8), (c, c)],
                fill=clamp255(lerp(DEGEN, (255, 255, 255), 0.6)) + (255,))
        for a in range(0, 360, 60):
            x = c + math.cos(math.radians(a - 90)) * 4.9
            y = c + math.sin(math.radians(a - 90)) * 4.9
            cv.ellipse(x, y, 0.5, fill=clamp255(lerp(DEGEN, (255, 255, 255), 0.8)) + (230,))

    elif kind == "hawking":
        # 辉光粒子团
        for i in range(16):
            a = random.random() * math.tau
            rr = random.random() ** 0.55 * 5.6
            x, y = c + math.cos(a) * rr, c + math.sin(a) * rr
            sz = 1.5 - rr / 6.0
            col = lerp(HAWK, (255, 255, 255), max(0, 1 - rr / 3.2))
            cv.ellipse(x, y, max(0.42, sz), fill=clamp255(col) + (255,))
        cv.ellipse(c, c, 2.0, fill=(240, 252, 255, 255))
        cv.ellipse(c, c, 6.0, outline=clamp255(lerp(HAWK, OUTL, 0.4)) + (150,), width=0.4)

    else:  # singularity
        cv.ellipse(c, c, 6.5, fill=(10, 8, 14, 255), outline=OUTL + (255,), width=0.5)
        draw_blackhole(cv, c, c, 1.75, 0.34, rings=True)
        cv.ellipse(c, c, 6.5, outline=clamp255(lerp(ACC, OUTL, 0.35)) + (210,), width=0.45)

    img = cv.finish()
    if kind in ("hawking", "singularity"):
        img = glow(img, 1.2, 0.5)
    return img


# ================================================================ 生产建筑
def gen_crafters():
    print("crafters:")

    # --- 引力压机 2x2 ---
    cv = plate(2, M_MID, seed=1)
    s, c = 64, 32
    # 中央压腔
    cv.ellipse(c, c, 13, fill=clamp255(lerp(M_DARK, (0, 0, 0), 0.35)) + (255,),
               outline=OUTL + (255,), width=0.6)
    cv.ellipse(c, c, 13, outline=clamp255(M_LITE) + (150,), width=0.4)
    # 三爪压头（带光照）
    for a in range(0, 360, 120):
        rad = math.radians(a - 90)
        x, y = c + math.cos(rad) * 8.4, c + math.sin(rad) * 8.4
        cv.poly([(c + math.cos(rad) * 13.2, c + math.sin(rad) * 13.2),
                 (x + math.cos(rad + 1.9) * 3.6, y + math.sin(rad + 1.9) * 3.6),
                 (x + math.cos(rad - 1.9) * 3.6, y + math.sin(rad - 1.9) * 3.6)],
                fill=clamp255(M_LITE) + (255,), outline=OUTL + (255,), width=0.45)
    # 被压缩的物质核心
    cv.ellipse(c, c, 4.4, fill=clamp255(lerp(DEGEN, (0, 0, 0), 0.5)) + (255,),
               outline=OUTL + (220,), width=0.4)
    cv.ellipse(c, c, 2.5, fill=clamp255(DEGEN) + (255,))
    cv.ellipse(c - 0.6, c - 0.6, 1.1, fill=(255, 255, 255, 235))
    save(add_noise(glow(cv.finish(), 1.4, 0.35), 4, 1), "graviton-press")

    # --- 霍金冷凝器 3x3 ---
    cv = plate(3, lerp(M_MID, HAWK, 0.07), seed=2)
    s, c = 96, 48
    cv.ellipse(c, c, 20, outline=clamp255(M_DARK) + (255,), width=0.7)
    # 冷凝腔（同心，带内发光）
    cv.ellipse(c, c, 15.5, fill=clamp255(lerp(M_DARK, (6, 16, 24), 0.55)) + (255,),
               outline=OUTL + (255,), width=0.6)
    for rr, al in [(12.5, 90), (9.5, 130), (6.5, 180)]:
        cv.ellipse(c, c, rr, outline=clamp255(HAWK) + (al,), width=0.45)
    cv.ellipse(c, c, 4.6, fill=clamp255(lerp(HAWK, (255, 255, 255), 0.4)) + (255,))
    cv.ellipse(c, c, 2.3, fill=(255, 255, 255, 255))
    # 四角低温罐（圆柱，带高光）
    for dx, dy in [(-1, -1), (1, -1), (-1, 1), (1, 1)]:
        x, y = c + dx * 16.5, c + dy * 16.5
        cv.rect(x - 4.2, y - 5.0, x + 4.2, y + 5.0, radius=2,
                fill=clamp255(M_LITE) + (255,), outline=OUTL + (255,), width=0.5)
        cv.rect(x - 2.9, y - 4.0, x - 1.6, y + 4.0, fill=clamp255(M_HI) + (200,))
        cv.ellipse(x, y + 2.6, 1.5, fill=clamp255(HAWK) + (255,), outline=OUTL + (180,), width=0.3)
        cv.line(x, y - 5.0, x, y - 8.5, clamp255(M_DARK) + (255,), 1.5)
    save(add_noise(glow(cv.finish(), 1.8, 0.45), 4, 2), "hawking-condenser")

    # --- 奇点熔炉 4x4 ---
    cv = plate(4, lerp(M_MID, (40, 28, 52), 0.42), seed=3)
    s, c = 128, 64
    # 外层磁约束环
    for rr, wd, col in [(41, 1.1, M_DARK), (35.5, 0.8, M_LITE), (27, 0.8, M_DARK)]:
        cv.ellipse(c, c, rr, outline=clamp255(col) + (255,), width=wd)
    # 约束线圈（12 根，带端子发光）
    for a in range(0, 360, 30):
        rad = math.radians(a)
        x1, y1 = c + math.cos(rad) * 27, c + math.sin(rad) * 27
        x2, y2 = c + math.cos(rad) * 41, c + math.sin(rad) * 41
        cv.line(x1, y1, x2, y2, clamp255(M_DARK) + (255,), 2.4)
        cv.line(x1, y1, x2, y2, clamp255(M_LITE) + (255,), 1.5)
        cv.ellipse(x2, y2, 2.0, fill=clamp255(lerp(ACC, (0, 0, 0), 0.25)) + (255,),
                   outline=OUTL + (255,), width=0.4)
        cv.ellipse(x2 - 0.3, y2 - 0.3, 0.9, fill=clamp255(ACC) + (255,))
    # 中心暗腔（游戏内会在这里叠加实时黑洞）
    cv.ellipse(c, c, 24.5, fill=(7, 5, 11, 255), outline=OUTL + (255,), width=0.9)
    cv.ellipse(c, c, 24.5, outline=clamp255(lerp(ACC, OUTL, 0.55)) + (150,), width=0.5)
    draw_blackhole(cv, c, c, 6.2, 0.30)
    save(add_noise(glow(cv.finish(), 2.2, 0.5), 4, 3), "singularity-forge")

    # --- 视界装配台 3x3 ---
    cv = plate(3, M_MID, seed=21)
    s, c = 96, 48
    cv.rect(c - 21, c - 21, c + 21, c + 21, outline=clamp255(M_DARK) + (255,),
            width=0.7, radius=3)
    # 装配凹槽
    cv.rect(c - 14.5, c - 14.5, c + 14.5, c + 14.5, radius=2.5,
            fill=clamp255(lerp(M_DARK, (30, 22, 40), 0.5)) + (255,),
            outline=OUTL + (255,), width=0.6)
    # 四角机械臂
    for a in range(0, 360, 90):
        rad = math.radians(a + 45)
        x, y = c + math.cos(rad) * 11.5, c + math.sin(rad) * 11.5
        cv.line(c + math.cos(rad) * 18, c + math.sin(rad) * 18, x, y,
                clamp255(M_LITE) + (255,), 2.0)
        cv.ellipse(x, y, 2.4, fill=clamp255(lerp(DEGEN, (0, 0, 0), 0.35)) + (255,),
                   outline=OUTL + (255,), width=0.4)
        cv.ellipse(x - 0.3, y - 0.3, 1.0, fill=clamp255(DEGEN) + (255,))
    # 中心成型光环
    cv.ellipse(c, c, 6.5, outline=clamp255(HAWK) + (210,), width=0.5)
    cv.ellipse(c, c, 3.0, fill=clamp255(lerp(HAWK, (255, 255, 255), 0.55)) + (255,))
    save(add_noise(glow(cv.finish(), 1.6, 0.4), 4, 21), "horizon-assembler")


# ================================================================ 炮塔
def barrel(cv, cx, cy, length, width, tipcol=ACC, muzzle=True):
    """带光照的炮管：中轴高光 + 侧面暗边 + 炮口。"""
    x0, x1 = cx - width / 2, cx + width / 2
    y0, y1 = cy - length, cy + width * 0.35
    cv.rect(x0, y0, x1, y1, radius=width * 0.28,
            fill=clamp255(M_MID) + (255,), outline=OUTL + (255,), width=0.5)
    # 左侧高光带
    cv.rect(x0 + width * 0.16, y0 + width * 0.18, x0 + width * 0.36, y1 - width * 0.2,
            fill=clamp255(M_HI) + (190,))
    # 右侧阴影
    cv.rect(x1 - width * 0.3, y0 + width * 0.18, x1 - width * 0.12, y1 - width * 0.2,
            fill=clamp255(lerp(M_MID, (0, 0, 0), 0.5)) + (170,))
    if muzzle:
        cv.rect(x0 - width * 0.12, y0, x1 + width * 0.12, y0 + width * 0.42,
                radius=width * 0.16, fill=clamp255(lerp(tipcol, (0, 0, 0), 0.2)) + (255,),
                outline=OUTL + (255,), width=0.4)
        cv.rect(x0 + width * 0.2, y0 + width * 0.08, x1 - width * 0.2, y0 + width * 0.3,
                fill=clamp255(tipcol) + (255,))


def gen_turrets():
    print("turrets:")

    # ---------- 参考系拖曳炮 2x2（入门炮）----------
    save(add_noise(plate(2, M_MID, seed=30).finish(), 4, 30), "frame-dragger-base")
    cv = Canvas(64)
    c = 32
    cv.ellipse(c, c, 13.5, fill=clamp255(M_MID) + (255,), outline=OUTL + (255,), width=0.6)
    cv.arc(c, c, 12.6, 150, 300, clamp255(M_HI) + (170,), 0.55)
    barrel(cv, c - 4.2, c - 3.5, 20, 5.4, DEGEN)
    barrel(cv, c + 4.2, c - 3.5, 20, 5.4, DEGEN)
    cv.rect(c - 6, c - 2, c + 6, c + 9, radius=2,
            fill=clamp255(M_LITE) + (255,), outline=OUTL + (255,), width=0.5)
    cv.rect(c - 4.6, c - 1, c - 3.2, c + 8, fill=clamp255(M_HI) + (180,))
    cv.ellipse(c, c + 3.5, 2.4, fill=clamp255(lerp(DEGEN, (0, 0, 0), 0.3)) + (255,),
               outline=OUTL + (200,), width=0.35)
    cv.ellipse(c - 0.3, c + 3.2, 1.0, fill=clamp255(DEGEN) + (255,))
    save(add_noise(glow(cv.finish(), 1.2, 0.3), 4, 31), "frame-dragger")
    hc = Canvas(64)
    hc.rect(c - 6.5, c - 24, c - 2, c - 21.5, fill=(255, 255, 255, 255), radius=0.6)
    hc.rect(c + 2, c - 24, c + 6.5, c - 21.5, fill=(255, 255, 255, 255), radius=0.6)
    save(hc.finish(), "frame-dragger-heat")

    # ---------- 吸积炮 3x3 ----------
    save(add_noise(plate(3, M_MID, seed=11).finish(), 4, 11), "accretion-cannon-base")
    cv = Canvas(96)
    c = 48
    cv.ellipse(c, c, 21, fill=clamp255(M_MID) + (255,), outline=OUTL + (255,), width=0.7)
    cv.arc(c, c, 19.8, 150, 300, clamp255(M_HI) + (165,), 0.6)
    cv.ellipse(c, c, 15.5, outline=clamp255(M_DARK) + (200,), width=0.5)
    barrel(cv, c - 6.6, c - 5, 31, 8.2, ACC)
    barrel(cv, c + 6.6, c - 5, 31, 8.2, ACC)
    # 中央供弹机匣
    cv.rect(c - 8, c - 3.5, c + 8, c + 13, radius=2.8,
            fill=clamp255(M_LITE) + (255,), outline=OUTL + (255,), width=0.55)
    cv.rect(c - 6.2, c - 2.2, c - 4.4, c + 11.6, fill=clamp255(M_HI) + (185,))
    # 吸积盘状能量核
    cv.ellipse(c, c + 4.5, 4.0, ry=2.4, fill=clamp255(lerp(ACC, (0, 0, 0), 0.55)) + (255,),
               outline=OUTL + (220,), width=0.4)
    cv.ellipse(c, c + 4.5, 2.6, ry=1.4, fill=clamp255(ACC) + (255,))
    cv.ellipse(c, c + 4.5, 1.1, ry=0.7, fill=(255, 250, 240, 255))
    save(add_noise(glow(cv.finish(), 1.5, 0.38), 4, 12), "accretion-cannon")
    hc = Canvas(96)
    hc.rect(c - 10.2, c - 37, c - 3.2, c - 33.5, fill=(255, 255, 255, 255), radius=0.8)
    hc.rect(c + 3.2, c - 37, c + 10.2, c - 33.5, fill=(255, 255, 255, 255), radius=0.8)
    save(hc.finish(), "accretion-cannon-heat")

    # ---------- 霍金发射器 3x3 ----------
    save(add_noise(plate(3, lerp(M_MID, HAWK, 0.10), seed=12).finish(), 4, 13),
         "hawking-emitter-base")
    cv = Canvas(96)
    c = 48
    cv.ellipse(c, c, 20.5, fill=clamp255(M_MID) + (255,), outline=OUTL + (255,), width=0.7)
    cv.arc(c, c, 19.3, 150, 300, clamp255(M_HI) + (165,), 0.6)
    # 锥形发射腔
    cv.poly([(c - 9.5, c + 11), (c - 5.5, c - 31), (c + 5.5, c - 31), (c + 9.5, c + 11)],
            fill=clamp255(M_LITE) + (255,), outline=OUTL + (255,), width=0.55)
    cv.poly([(c - 7.6, c + 10), (c - 4.3, c - 29.5), (c - 2.2, c - 29.5), (c - 4.6, c + 10)],
            fill=clamp255(M_HI) + (170,))
    # 内部能量通道
    cv.poly([(c - 2.9, c - 29), (c + 2.9, c - 29), (c + 2.2, c + 6), (c - 2.2, c + 6)],
            fill=clamp255(lerp(HAWK, (0, 0, 0), 0.45)) + (255,))
    cv.poly([(c - 1.3, c - 28), (c + 1.3, c - 28), (c + 1.0, c + 5), (c - 1.0, c + 5)],
            fill=clamp255(HAWK) + (220,))
    # 聚焦透镜
    cv.ellipse(c, c - 31.5, 4.2, ry=2.6, fill=clamp255(lerp(HAWK, (255, 255, 255), 0.35)) + (255,),
               outline=OUTL + (255,), width=0.45)
    cv.ellipse(c, c - 31.5, 2.0, ry=1.2, fill=(255, 255, 255, 255))
    # 散热鳍
    for sy in (-16, -8, 0, 8):
        for sx in (-1, 1):
            x = c + sx * 13.5
            cv.rect(x - 3.4, c + sy, x + 3.4, c + sy + 3.4, radius=0.8,
                    fill=clamp255(M_DARK) + (255,), outline=OUTL + (230,), width=0.35)
            cv.rect(x - 2.6, c + sy + 0.6, x + 2.6, c + sy + 1.4,
                    fill=clamp255(M_LITE) + (170,))
    save(add_noise(glow(cv.finish(), 1.8, 0.45), 4, 14), "hawking-emitter")
    hc = Canvas(96)
    hc.ellipse(c, c - 31.5, 4.2, ry=2.6, fill=(255, 255, 255, 255))
    hc.poly([(c - 2.9, c - 29), (c + 2.9, c - 29), (c + 2.2, c + 6), (c - 2.2, c + 6)],
            fill=(255, 255, 255, 190))
    save(hc.finish(), "hawking-emitter-heat")

    # ---------- 类星体喷流 4x4 ----------
    save(add_noise(plate(4, lerp(M_MID, (44, 32, 40), 0.3), seed=15).finish(), 4, 15),
         "quasar-lance-base")
    cv = Canvas(128)
    c = 64
    # 八边形主体
    pts = [(c + math.cos(math.radians(a + 22.5)) * 30,
            c + math.sin(math.radians(a + 22.5)) * 30) for a in range(0, 360, 45)]
    cv.poly(pts, fill=clamp255(M_MID) + (255,), outline=OUTL + (255,), width=0.8)
    pts2 = [(c + math.cos(math.radians(a + 22.5)) * 23,
             c + math.sin(math.radians(a + 22.5)) * 23) for a in range(0, 360, 45)]
    cv.poly(pts2, outline=clamp255(M_DARK) + (220,), width=0.5)
    cv.arc(c, c, 28, 150, 300, clamp255(M_HI) + (150,), 0.7)
    # 双极喷流管（类星体特征）
    cv.rect(c - 7.5, c - 52, c + 7.5, c + 4, radius=3,
            fill=clamp255(M_LITE) + (255,), outline=OUTL + (255,), width=0.6)
    cv.rect(c - 5.8, c - 50, c - 3.6, c + 2, fill=clamp255(M_HI) + (185,))
    # 加速环
    for yy in (-45, -37, -29, -21):
        cv.rect(c - 9.8, c + yy, c + 9.8, c + yy + 3.2, radius=1,
                fill=clamp255(M_DARK) + (255,), outline=OUTL + (230,), width=0.35)
        cv.ellipse(c - 8.2, c + yy + 1.6, 0.85, fill=clamp255(ACC) + (255,))
        cv.ellipse(c + 8.2, c + yy + 1.6, 0.85, fill=clamp255(ACC) + (255,))
    # 喷口
    cv.ellipse(c, c - 53, 6.0, ry=3.2, fill=(8, 6, 12, 255), outline=OUTL + (255,), width=0.55)
    cv.ellipse(c, c - 53, 3.6, ry=1.9, fill=clamp255(ACC) + (255,))
    cv.ellipse(c, c - 53, 1.6, ry=0.85, fill=(255, 255, 255, 255))
    # 后部吸积核心
    cv.ellipse(c, c + 12, 9.5, ry=5.5, fill=(10, 7, 14, 255), outline=OUTL + (255,), width=0.6)
    draw_blackhole(cv, c, c + 12, 3.0, 0.42, rings=False)
    save(add_noise(glow(cv.finish(), 2.0, 0.48), 4, 16), "quasar-lance")
    hc = Canvas(128)
    hc.ellipse(c, c - 53, 6.0, ry=3.2, fill=(255, 255, 255, 235))
    for yy in (-45, -37, -29, -21):
        hc.rect(c - 9.8, c + yy, c + 9.8, c + yy + 3.2, fill=(255, 255, 255, 120), radius=1)
    save(hc.finish(), "quasar-lance-heat")

    # ---------- 事件视界炮 5x5（旗舰）----------
    save(add_noise(plate(5, lerp(M_MID, (38, 26, 50), 0.45), seed=13).finish(), 4, 17),
         "event-horizon-base")
    cv = Canvas(160)
    c = 80
    # 六边形重装甲
    hexp = [(c + math.cos(math.radians(a - 90)) * 39,
             c + math.sin(math.radians(a - 90)) * 39) for a in range(0, 360, 60)]
    cv.poly(hexp, fill=clamp255(lerp(M_MID, (44, 32, 56), 0.4)) + (255,),
            outline=OUTL + (255,), width=0.9)
    hexp2 = [(c + math.cos(math.radians(a - 90)) * 30,
              c + math.sin(math.radians(a - 90)) * 30) for a in range(0, 360, 60)]
    cv.poly(hexp2, outline=clamp255(M_DARK) + (230,), width=0.6)
    cv.arc(c, c, 36, 150, 300, clamp255(M_HI) + (145,), 0.8)
    # 装甲板分割线
    for a in range(0, 360, 60):
        rad = math.radians(a - 90)
        cv.line(c + math.cos(rad) * 30, c + math.sin(rad) * 30,
                c + math.cos(rad) * 39, c + math.sin(rad) * 39,
                clamp255(M_DARK) + (255,), 0.7)
    # 主炮管
    cv.rect(c - 12.5, c - 66, c + 12.5, c + 6, radius=4.5,
            fill=clamp255(M_MID) + (255,), outline=OUTL + (255,), width=0.8)
    cv.rect(c - 10.2, c - 63, c - 7.2, c + 4, fill=clamp255(M_HI) + (185,))
    cv.rect(c + 7.6, c - 63, c + 10.4, c + 4,
            fill=clamp255(lerp(M_MID, (0, 0, 0), 0.5)) + (175,))
    # 磁约束环组
    for yy in (-58, -48, -38, -28):
        cv.rect(c - 15.5, c + yy, c + 15.5, c + yy + 4.2, radius=1.4,
                fill=clamp255(M_LITE) + (255,), outline=OUTL + (255,), width=0.45)
        cv.rect(c - 14, c + yy + 0.7, c + 14, c + yy + 1.7, fill=clamp255(M_HI) + (150,))
        cv.ellipse(c - 13.6, c + yy + 2.1, 1.15, fill=clamp255(ACC) + (255,),
                   outline=OUTL + (170,), width=0.25)
        cv.ellipse(c + 13.6, c + yy + 2.1, 1.15, fill=clamp255(ACC) + (255,),
                   outline=OUTL + (170,), width=0.25)
    # 炮口奇点腔
    cv.ellipse(c, c - 61, 11.5, fill=(6, 4, 10, 255), outline=OUTL + (255,), width=0.8)
    draw_blackhole(cv, c, c - 61, 4.4, 0.30)
    cv.ellipse(c, c - 61, 11.5, outline=clamp255(lerp(ACC, OUTL, 0.4)) + (190,), width=0.5)
    # 后部主反应堆
    cv.ellipse(c, c + 17, 12.5, fill=(8, 5, 12, 255), outline=OUTL + (255,), width=0.8)
    draw_blackhole(cv, c, c + 17, 4.0, 0.34)
    # 侧挂弹舱
    for sx in (-1, 1):
        x = c + sx * 29
        cv.rect(x - 8.5, c - 11, x + 8.5, c + 21, radius=2.8,
                fill=clamp255(M_LITE) + (255,), outline=OUTL + (255,), width=0.55)
        cv.rect(x - 6.8, c - 9.6, x - 5.2, c + 19.6, fill=clamp255(M_HI) + (175,))
        for yy in (-5, 3, 11):
            cv.ellipse(x, c + yy, 2.8, ry=2.2,
                       fill=clamp255(lerp(ACC, (0, 0, 0), 0.4)) + (255,),
                       outline=OUTL + (230,), width=0.4)
            cv.ellipse(x - 0.4, c + yy - 0.4, 1.2, ry=0.9, fill=clamp255(ACC) + (255,))
    save(add_noise(glow(cv.finish(), 2.4, 0.5), 4, 18), "event-horizon")
    hc = Canvas(160)
    hc.ellipse(c, c - 61, 11.5, fill=(255, 255, 255, 215))
    for yy in (-58, -48, -38, -28):
        hc.rect(c - 15.5, c + yy, c + 15.5, c + yy + 4.2, fill=(255, 255, 255, 115), radius=1.4)
    hc.ellipse(c, c + 17, 12.5, fill=(255, 255, 255, 140))
    save(hc.finish(), "event-horizon-heat")


# ================================================================ 单位
def unit_sprite(size, body, accent, heavy=False, reactor=False, seed=0):
    """朝上的飞行单位，带光照与装甲分块。"""
    cv = Canvas(size)
    c = size / 2
    L = size * 0.42
    # 重型机身相对更瘦长，避免显得臃肿
    W = size * (0.125 if heavy else 0.155)

    # 机翼（先画，位于机身下方）
    for sx in (-1, 1):
        cv.poly([(c + sx * W * 0.9, c - L * 0.12),
                 (c + sx * W * 2.7, c + L * 0.22),
                 (c + sx * W * 2.35, c + L * 0.56),
                 (c + sx * W * 0.8, c + L * 0.46)],
                fill=clamp255(lerp(body, (0, 0, 0), 0.30)) + (255,),
                outline=OUTL + (255,), width=0.5)
        # 翼面高光
        cv.poly([(c + sx * W * 1.1, c - L * 0.02),
                 (c + sx * W * 2.3, c + L * 0.25),
                 (c + sx * W * 2.1, c + L * 0.40),
                 (c + sx * W * 1.0, c + L * 0.30)],
                fill=clamp255(lerp(body, M_HI, 0.25 if sx < 0 else 0.05)) + (190,))

    # 机身
    cv.poly([(c, c - L),
             (c + W, c - L * 0.22), (c + W * 0.82, c + L * 0.66),
             (c - W * 0.82, c + L * 0.66), (c - W, c - L * 0.22)],
            fill=clamp255(body) + (255,), outline=OUTL + (255,), width=0.55)
    # 左侧受光
    cv.poly([(c, c - L * 0.96), (c - W * 0.9, c - L * 0.18),
             (c - W * 0.74, c + L * 0.6), (c - W * 0.2, c + L * 0.62), (c - W * 0.12, c - L * 0.7)],
            fill=clamp255(lerp(body, M_HI, 0.30)) + (205,))
    # 右侧阴影
    cv.poly([(c + W * 0.12, c - L * 0.7), (c + W * 0.9, c - L * 0.18),
             (c + W * 0.74, c + L * 0.6), (c + W * 0.2, c + L * 0.62)],
            fill=clamp255(lerp(body, (0, 0, 0), 0.34)) + (175,))
    # 座舱
    ck = 0.42 if heavy else 0.56   # 重型座舱占比更小
    cv.poly([(c, c - L * 0.70), (c + W * ck, c - L * 0.30),
             (c, c - L * 0.14), (c - W * ck, c - L * 0.30)],
            fill=clamp255(lerp(accent, (0, 0, 0), 0.45)) + (255,),
            outline=OUTL + (220,), width=0.4)
    cv.poly([(c, c - L * 0.64), (c + W * ck * 0.6, c - L * 0.34),
             (c - W * ck * 0.15, c - L * 0.30)],
            fill=clamp255(lerp(accent, (255, 255, 255), 0.55)) + (230,))

    if heavy:
        # 机首装甲棱线
        cv.line(c, c - L * 0.98, c, c - L * 0.36, clamp255(lerp(body, M_HI, 0.45)) + (200,), 0.45)
        for sx in (-1, 1):
            cv.line(c + sx * W * 0.42, c - L * 0.60, c + sx * W * 0.62, c + L * 0.50,
                    clamp255(lerp(body, (0, 0, 0), 0.45)) + (160,), 0.4)

    if reactor:
        cv.ellipse(c, c + L * 0.24, size * 0.095, fill=(8, 6, 12, 255),
                   outline=OUTL + (255,), width=0.55)
        draw_blackhole(cv, c, c + L * 0.24, size * 0.038, 0.34, rings=False)

    # 引擎
    for sx in ([-1, 1] if heavy else [0]):
        x = c + sx * W * 0.62
        cv.ellipse(x, c + L * 0.66, W * (0.4 if heavy else 0.55), ry=W * 0.3,
                   fill=clamp255(lerp(accent, (0, 0, 0), 0.25)) + (255,),
                   outline=OUTL + (230,), width=0.4)
        cv.ellipse(x, c + L * 0.66, W * (0.2 if heavy else 0.28), ry=W * 0.15,
                   fill=(255, 255, 255, 240))
    return cv


def cell_sprite(size, oy, rx, ry):
    cv = Canvas(size)
    c = size / 2
    cv.ellipse(c, c + oy, rx, ry=ry, fill=(255, 255, 255, 255))
    return cv.finish()


def gen_units():
    print("units:")
    # 透镜号（轻型）
    s = 38
    cv = unit_sprite(s, lerp(M_LITE, DEGEN, 0.22), HAWK, heavy=False, seed=41)
    save(add_noise(glow(cv.finish(), 1.0, 0.3), 3, 41), "lensing")
    save(cell_sprite(s, -s * 0.10, 2.4, 3.0), "lensing-cell")
    w = Canvas(16)
    w.rect(5.2, 1.4, 10.8, 13.5, radius=1.6, fill=clamp255(M_LITE) + (255,),
           outline=OUTL + (255,), width=0.5)
    w.rect(6.2, 2.4, 7.4, 12.4, fill=clamp255(M_HI) + (180,))
    w.rect(6.0, 1.0, 10.0, 2.8, radius=0.6, fill=clamp255(ACC) + (255,),
           outline=OUTL + (200,), width=0.3)
    save(glow(w.finish(), 0.8, 0.3), "lensing-weapon")

    # 能层号（重型）
    s = 78
    cv = unit_sprite(s, lerp(M_MID, (46, 34, 58), 0.45), ACC, heavy=True, reactor=True, seed=42)
    save(add_noise(glow(cv.finish(), 1.6, 0.4), 3, 42), "ergosphere")
    save(cell_sprite(s, -s * 0.13, 3.4, 4.2), "ergosphere-cell")
    w = Canvas(32)
    w.rect(10.5, 3.0, 21.5, 28.0, radius=2.6, fill=clamp255(M_MID) + (255,),
           outline=OUTL + (255,), width=0.55)
    w.rect(12.2, 4.4, 14.0, 26.6, fill=clamp255(M_HI) + (180,))
    w.rect(19.0, 4.4, 20.4, 26.6, fill=clamp255(lerp(M_MID, (0, 0, 0), 0.5)) + (160,))
    w.rect(12.0, 0.8, 20.0, 11.0, radius=1.8, fill=clamp255(M_LITE) + (255,),
           outline=OUTL + (255,), width=0.5)
    w.ellipse(16, 5.2, 3.4, fill=(8, 6, 12, 255), outline=OUTL + (255,), width=0.45)
    draw_blackhole(w, 16, 5.2, 1.35, 0.34, rings=False)
    save(glow(w.finish(), 1.2, 0.4), "ergosphere-weapon")


def gen_items():
    print("items:")
    save(item_icon("degenerate-matter", "degenerate"), "degenerate-matter")
    save(item_icon("hawking-dust", "hawking"), "hawking-dust")
    save(item_icon("singularity-core", "singularity"), "singularity-core")


def gen_icon():
    """模组图标：复用游戏内渲染器逻辑（见 preview.py）。"""
    import subprocess
    here = os.path.dirname(__file__)
    subprocess.run(["python3", "-c", f"""
import sys; sys.path.insert(0, {here!r})
import random
from PIL import Image, ImageDraw, ImageFilter
import preview as P
S = 256
img = P.render(S, 26, 0.60, 40, bg=(5, 4, 9)).convert("RGB")
d = ImageDraw.Draw(img)
random.seed(11)
for _ in range(260):
    x, y = random.randint(0, S-1), random.randint(0, S-1)
    r, g, b = img.getpixel((x, y))
    if r + g + b > 70: continue
    v = random.randint(70, 230)
    d.point((x, y), fill=(v, v, min(255, v+25)))
img.filter(ImageFilter.SMOOTH).save({os.path.join(here, '..', 'icon.png')!r})
print("   icon.png")
"""], check=True)


if __name__ == "__main__":
    gen_items()
    gen_crafters()
    gen_turrets()
    gen_units()
    gen_icon()
    print("done ->", os.path.abspath(OUT))
