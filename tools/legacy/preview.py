#!/usr/bin/env python3
"""把 BlackHoleRenderer.java 的逻辑 1:1 移植到 Python，渲染预览图/动图，
用来在不开游戏的情况下检查黑洞的实际观感。"""
import math, os
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "..", "preview")
os.makedirs(OUT, exist_ok=True)

HOT = (255, 246, 224)
MID = (255, 177, 78)
COLD = (255, 90, 43)
SEG = 80
DISK_RINGS = 14
LENS_RINGS = 8
DISK_SCALE = [1.0]


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def clamp(v, lo=0.0, hi=1.0):
    return max(lo, min(hi, v))


def brightness(angle, r, rin, rout, spin, alpha, f=None):
    if f is None:
        f = clamp((r - rin) / max(0.001, rout - rin))
    approach = 0.5 - 0.5 * math.cos(math.radians(angle))
    dopp = 0.10 + 2.35 * (approach ** 2.1)
    dopp = dopp + (0.55 + dopp * 0.45 - dopp) * f
    radial = ((1 - f) ** 1.35) * 1.15 + 0.08
    turb = (0.70
            + 0.18 * math.sin(math.radians(angle * 3 + spin))
            + 0.12 * math.sin(math.radians(angle * 7 - spin * 0.6 + f * 140))
            + 0.08 * math.sin(math.radians(angle * 13 + spin * 1.7)))
    return clamp(dopp * radial * turb * 1.25) * alpha


def tint(f, bright):
    if f < 0.5:
        c = lerp(HOT, MID, f * 2)
    else:
        c = lerp(MID, COLD, (f - 0.5) * 2)
    if bright > 0.62:
        c = lerp(c, (255, 255, 255), (bright - 0.62) * 0.9)
    return c, clamp(bright)


class Additive:
    """模拟 Blending.additive 的画布。"""
    def __init__(self, w, h, bg=(0, 0, 0)):
        self.w, self.h = w, h
        self.buf = [[list(bg) for _ in range(w)] for _ in range(h)]

    def add_quad(self, pts, cols):
        # pts: 4x(x,y), cols: 4x((r,g,b),a)  —— 用重心插值填充
        xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
        x0, x1 = int(math.floor(min(xs))), int(math.ceil(max(xs)))
        y0, y1 = int(math.floor(min(ys))), int(math.ceil(max(ys)))
        if x1 - x0 > 900 or y1 - y0 > 900:
            return
        for tri in ((0, 1, 2), (0, 2, 3)):
            self._tri([pts[i] for i in tri], [cols[i] for i in tri])

    def _tri(self, p, c):
        (ax, ay), (bx, by), (cx, cy) = p
        det = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
        if abs(det) < 1e-9:
            return
        x0 = max(0, int(math.floor(min(ax, bx, cx))))
        x1 = min(self.w - 1, int(math.ceil(max(ax, bx, cx))))
        y0 = max(0, int(math.floor(min(ay, by, cy))))
        y1 = min(self.h - 1, int(math.ceil(max(ay, by, cy))))
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                px, py = x + 0.5, y + 0.5
                l1 = ((by - cy) * (px - cx) + (cx - bx) * (py - cy)) / det
                l2 = ((cy - ay) * (px - cx) + (ax - cx) * (py - cy)) / det
                l3 = 1 - l1 - l2
                if l1 < -0.001 or l2 < -0.001 or l3 < -0.001:
                    continue
                col = [l1 * c[0][0][i] + l2 * c[1][0][i] + l3 * c[2][0][i] for i in range(3)]
                a = l1 * c[0][1] + l2 * c[1][1] + l3 * c[2][1]
                dst = self.buf[y][x]
                for i in range(3):
                    dst[i] = min(255.0, dst[i] + col[i] * a)

    def add_ring(self, cx, cy, r, color, alpha, stroke):
        steps = max(24, int(r * 8))
        for k in range(steps):
            a = k * 2 * math.pi / steps
            for w in range(max(1, int(stroke * 2))):
                rr = r - stroke / 2 + w * 0.5
                x, y = cx + math.cos(a) * rr, cy + math.sin(a) * rr
                xi, yi = int(x), int(y)
                if 0 <= xi < self.w and 0 <= yi < self.h:
                    dst = self.buf[yi][xi]
                    for i in range(3):
                        dst[i] = min(255.0, dst[i] + color[i] * alpha)

    def fill_circle_opaque(self, cx, cy, r, color):
        for y in range(max(0, int(cy - r - 1)), min(self.h, int(cy + r + 2))):
            for x in range(max(0, int(cx - r - 1)), min(self.w, int(cx + r + 2))):
                if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                    self.buf[y][x] = list(color)

    def darken_circle(self, cx, cy, r, amount):
        for y in range(max(0, int(cy - r - 1)), min(self.h, int(cy + r + 2))):
            for x in range(max(0, int(cx - r - 1)), min(self.w, int(cx + r + 2))):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d <= r:
                    k = 1.0 - amount * (1.0 - d / r)
                    self.buf[y][x] = [v * k for v in self.buf[y][x]]

    def image(self):
        img = Image.new("RGB", (self.w, self.h))
        img.putdata([tuple(int(v) for v in self.buf[y][x]) for y in range(self.h) for x in range(self.w)])
        return img


def thickness(f):
    return DISK_SCALE[0] * (0.06 + f * 0.34)


def draw_disk(cv, x, y, rin, rout, squash, spin, alpha, back):
    for ring in range(DISK_RINGS):
        f0, f1 = ring / DISK_RINGS, (ring + 1) / DISK_RINGS
        r0 = rin + (rout - rin) * (f0 * f0 * 0.55 + f0 * 0.45)
        r1 = rin + (rout - rin) * (f1 * f1 * 0.55 + f1 * 0.45)
        sp0 = spin * (rin / r0) ** 1.5 * 34
        sp1 = spin * (rin / r1) ** 1.5 * 34
        t0, t1 = thickness(f0), thickness(f1)
        for i in range(SEG):
            a0, a1 = i * 360 / SEG, (i + 1) * 360 / SEG
            mid = (a0 + a1) / 2
            if (math.sin(math.radians(mid)) >= 0) != back:
                continue
            b0 = brightness(a0, r0, rin, rout, sp0, alpha, f0)
            b1 = brightness(a1, r0, rin, rout, sp0, alpha, f0)
            b2 = brightness(a1, r1, rin, rout, sp1, alpha, f1)
            b3 = brightness(a0, r1, rin, rout, sp1, alpha, f1)
            cols = [tint(f0, b0), tint(f0, b1), tint(f1, b2), tint(f1, b3)]
            def pt(a, r, dy=0.0):
                return (x + math.cos(math.radians(a)) * r,
                        y + math.sin(math.radians(a)) * r * squash + dy)
            cv.add_quad([pt(a0, r0), pt(a1, r0), pt(a1, r1), pt(a0, r1)], cols)
            if t1 > 0.01:
                dim = [tint(f0, b0 * 0.42), tint(f0, b1 * 0.42),
                       tint(f1, b2 * 0.42), tint(f1, b3 * 0.42)]
                for sgn in (-1, 1):
                    cv.add_quad([pt(a0, r0, t0 * sgn), pt(a1, r0, t0 * sgn),
                                 pt(a1, r1, t1 * sgn), pt(a0, r1, t1 * sgn)], dim)


def profile(f):
    return math.sin(math.radians(clamp(f) * 180)) ** 0.85


def draw_lens_arc(cv, x, y, radius, rin, rout, spin, alpha, dir_):
    band_in, band_out = radius * 1.40, radius * 2.05
    sp = spin * 46
    start = 180.0 if dir_ > 0 else 0.0
    sweep = 180.0
    for ring in range(LENS_RINGS):
        f0, f1 = ring / LENS_RINGS, (ring + 1) / LENS_RINGS
        r0 = band_in + (band_out - band_in) * f0
        r1 = band_in + (band_out - band_in) * f1
        p0, p1 = profile(f0), profile(f1)
        for i in range(SEG):
            a0 = start + i * sweep / SEG
            a1 = start + (i + 1) * sweep / SEG
            mid = (a0 + a1) / 2
            fall = abs(math.sin(math.radians(mid))) ** 0.5
            ma0, ma1 = a0 + 180, a1 + 180
            b0 = brightness(ma0, rin * 1.05, rin, rout, sp, alpha, 0.05) * fall * p0 * 1.45
            b1 = brightness(ma1, rin * 1.05, rin, rout, sp, alpha, 0.05) * fall * p0 * 1.45
            b2 = brightness(ma1, rin * 1.35, rin, rout, sp, alpha, 0.25) * fall * p1 * 1.45
            b3 = brightness(ma0, rin * 1.35, rin, rout, sp, alpha, 0.25) * fall * p1 * 1.45
            cols = [tint(f0 * 0.35, b0), tint(f0 * 0.35, b1),
                    tint(0.15 + f1 * 0.45, b2), tint(0.15 + f1 * 0.45, b3)]
            def pt(a, r):
                return (x + math.cos(math.radians(a)) * r, y + math.sin(math.radians(a)) * r)
            cv.add_quad([pt(a0, r0), pt(a1, r0), pt(a1, r1), pt(a0, r1)], cols)


def render(size, radius, tilt, time, bg=(6, 5, 10)):
    cv = Additive(size, size, bg)
    x = y = size / 2
    squash = 1 + (0.16 - 1) * clamp(tilt)
    rin, rout = radius * 2.05, radius * 4.6
    spin = time * 1.35
    DISK_SCALE[0] = radius * 0.5

    draw_disk(cv, x, y, rin, rout, squash, spin, 1.0, True)
    draw_lens_arc(cv, x, y, radius, rin, rout, spin, 0.85, 1)

    cv.fill_circle_opaque(x, y, radius, (0, 0, 0))
    cv.darken_circle(x, y, radius * 1.22, 0.55)

    ph = radius * 1.5
    flick = 0.88
    layers = 7
    for i in range(layers):
        f = i / (layers - 1)
        rr = ph * (0.985 + (1.16 - 0.985) * f)
        a = flick * ((1 - f) ** 2.2) * 0.75
        col = lerp(HOT, MID, f * 0.85)
        cv.add_ring(x, y, rr, col, a, max(0.35, radius * (0.05 + f * 0.07)))
    cv.add_ring(x, y, ph * 0.99, (255, 255, 255), flick * 0.55, max(0.3, radius * 0.035))

    draw_disk(cv, x, y, rin, rout, squash, spin, 1.0, False)
    draw_lens_arc(cv, x, y, radius, rin, rout, spin, 0.55, -1)
    return cv.image()


if __name__ == "__main__":
    print("渲染静帧…")
    render(360, 26, 0.74, 40).resize((360, 360), Image.NEAREST).save(os.path.join(OUT, "blackhole_still.png"))
    print("渲染不同倾角对比…")
    strip = Image.new("RGB", (300 * 4, 300), (6, 5, 10))
    for i, t in enumerate([0.15, 0.45, 0.74, 0.92]):
        strip.paste(render(300, 22, t, 40), (i * 300, 0))
    strip.save(os.path.join(OUT, "tilt_compare.png"))
    print("渲染动画…")
    frames = [render(240, 18, 0.74, 10 + f * 3.2) for f in range(24)]
    frames[0].save(os.path.join(OUT, "blackhole_spin.gif"), save_all=True,
                   append_images=frames[1:], duration=70, loop=0, optimize=True)
    print("done ->", os.path.abspath(OUT))
