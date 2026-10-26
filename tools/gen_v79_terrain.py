#!/usr/bin/env python3
"""v7.9 terrain art: seven floors, three rock walls and three wall ores.

Everything is drawn here from scratch - no external sprites. Tiles are generated at 4x and downsampled, and
each variant uses a fixed seed so the result is reproducible. Run from the project root:

    python3 tools/gen_v79_terrain.py
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

OUT = Path(__file__).resolve().parents[1] / "assets" / "sprites" / "blocks" / "environment"
OUT.mkdir(parents=True, exist_ok=True)
S = 4
N = 32
INK = (16, 22, 34, 255)


def canvas(n=N, bg=(0, 0, 0, 0)):
    return Image.new("RGBA", (n * S, n * S), bg)


def save(im, name):
    im.resize((im.width // S, im.height // S), Image.Resampling.LANCZOS).save(OUT / name)


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))), max(0, min(255, int(c[2] * f))), c[3] if len(c) > 3 else 255)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def grain(d, rng, base, count, lo=0.86, hi=1.16, size=(1, 2)):
    """speckle that reads as ground texture at 32 px"""
    for _ in range(count):
        x, y = rng.random() * N, rng.random() * N
        r = rng.uniform(*size)
        d.ellipse((x * S, y * S, (x + r) * S, (y + r) * S), fill=shade(base, rng.uniform(lo, hi)))


def cracks(d, rng, col, count, width=1, length=(6, 14)):
    for _ in range(count):
        x, y = rng.random() * N, rng.random() * N
        a = rng.random() * math.tau
        pts = [(x, y)]
        for _ in range(3):
            a += rng.uniform(-0.6, 0.6)
            l = rng.uniform(*length) / 3
            x, y = x + math.cos(a) * l, y + math.sin(a) * l
            pts.append((x, y))
        d.line([(p[0] * S, p[1] * S) for p in pts], fill=col, width=width * S, joint="curve")


def flecks(d, rng, col, count, r=(0.8, 1.6)):
    for _ in range(count):
        x, y, rr = rng.random() * N, rng.random() * N, rng.uniform(*r)
        d.ellipse((x * S, y * S, (x + rr) * S, (y + rr) * S), fill=col)


# ----------------------------------------------------------------------------------------------------------
# floors
# ----------------------------------------------------------------------------------------------------------

def floor_glass(v):
    """salt flat: pale, hard, broken into wide plates"""
    rng = random.Random(1700 + v)
    base = mix((150, 172, 190), (255, 255, 255), v * 0.06)
    im = canvas(bg=base)
    d = ImageDraw.Draw(im)
    grain(d, rng, base, 120, 0.94, 1.08, (1.0, 2.4))
    seam = shade(base, 0.74)
    for _ in range(3):
        y = rng.random() * N
        d.line([(0, y * S), (N * 0.4 * S, (y + rng.uniform(-3, 3)) * S), (N * S, (y + rng.uniform(-4, 4)) * S)], fill=seam, width=S, joint="curve")
    for _ in range(2):
        x = rng.random() * N
        d.line([(x * S, 0), ((x + rng.uniform(-3, 3)) * S, N * 0.5 * S), ((x + rng.uniform(-4, 4)) * S, N * S)], fill=seam, width=S, joint="curve")
    flecks(d, rng, (238, 248, 255, 190), 14, (0.9, 1.7))
    return im


def floor_ash(v):
    """ember ash: dark warm dust with cooling embers"""
    rng = random.Random(1710 + v)
    base = mix((74, 54, 47), (96, 66, 54), v * 0.4)
    im = canvas(bg=base)
    d = ImageDraw.Draw(im)
    grain(d, rng, base, 200, 0.8, 1.25, (0.9, 2.6))
    for _ in range(16):
        x, y, r = rng.random() * N, rng.random() * N, rng.uniform(1.4, 3.2)
        d.ellipse((x * S, y * S, (x + r) * S, (y + r) * S), fill=shade(base, 0.72))
    flecks(d, rng, (226, 118, 56, 215), 9, (0.8, 1.5))
    flecks(d, rng, (255, 186, 96, 170), 5, (0.6, 1.1))
    return im


def floor_frost(v):
    """frozen crust: pale blue slab split by frost lines"""
    rng = random.Random(1720 + v)
    base = mix((163, 190, 206), (196, 216, 228), v * 0.4)
    im = canvas(bg=base)
    d = ImageDraw.Draw(im)
    grain(d, rng, base, 140, 0.95, 1.06, (1.2, 2.8))
    cracks(d, rng, (238, 250, 255, 190), 5, 1, (9, 16))
    cracks(d, rng, shade(base, 0.8), 4, 1, (7, 13))
    for _ in range(10):
        x, y, r = rng.random() * N, rng.random() * N, rng.uniform(1.0, 2.0)
        d.ellipse((x * S, y * S, (x + r) * S, (y + r) * S), fill=(255, 255, 255, 120))
    return im


def floor_gravel(v):
    """aurite gravel: loose stones with metal in them - the ground is the ore"""
    rng = random.Random(1730 + v)
    base = mix((122, 104, 74), (142, 122, 86), v * 0.4)
    im = canvas(bg=base)
    d = ImageDraw.Draw(im)
    grain(d, rng, base, 90, 0.82, 1.14, (1.4, 3.0))
    for _ in range(26):
        x, y, r = rng.random() * N, rng.random() * N, rng.uniform(1.6, 3.4)
        c = shade(base, rng.uniform(0.74, 1.2))
        d.ellipse((x * S, y * S, (x + r) * S, (y + r) * S), fill=c, outline=shade(base, 0.6), width=S // 2)
    flecks(d, rng, (231, 190, 112, 230), 12, (0.9, 1.7))
    flecks(d, rng, (255, 228, 168, 200), 6, (0.6, 1.1))
    return im


def floor_silt(v):
    """resonance silt: soft violet dust that glows a little"""
    rng = random.Random(1740 + v)
    base = mix((96, 84, 134), (116, 100, 156), v * 0.4)
    im = canvas(bg=base)
    d = ImageDraw.Draw(im)
    grain(d, rng, base, 230, 0.86, 1.16, (1.0, 2.4))
    glow = Image.new("RGBA", im.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    for _ in range(9):
        x, y, r = rng.random() * N, rng.random() * N, rng.uniform(1.6, 3.4)
        gd.ellipse((x * S, y * S, (x + r) * S, (y + r) * S), fill=(192, 164, 255, 190))
    glow = glow.filter(ImageFilter.GaussianBlur(2.2 * S))
    im.alpha_composite(glow)
    flecks(ImageDraw.Draw(im), rng, (214, 192, 255, 225), 10, (0.7, 1.3))
    return im


def floor_vent(v):
    """plasma vent: cracked hot rock with light coming up through it"""
    rng = random.Random(1750 + v)
    base = mix((78, 60, 52), (96, 72, 58), v * 0.4)
    im = canvas(bg=base)
    d = ImageDraw.Draw(im)
    grain(d, rng, base, 150, 0.84, 1.12, (1.1, 2.6))
    hot = Image.new("RGBA", im.size, (0, 0, 0, 0))
    hd = ImageDraw.Draw(hot)
    cracks(hd, rng, (255, 170, 72, 235), 4, 1, (10, 18))
    for _ in range(4):
        x, y, r = rng.random() * N, rng.random() * N, rng.uniform(1.8, 3.6)
        hd.ellipse((x * S, y * S, (x + r) * S, (y + r) * S), fill=(255, 200, 110, 230))
    halo = hot.filter(ImageFilter.GaussianBlur(2.6 * S))
    im.alpha_composite(halo)
    im.alpha_composite(hot)
    cracks(ImageDraw.Draw(im), rng, shade(base, 0.66), 4, 1, (8, 14))
    return im


def floor_basalt(v):
    """basalt shelf: plain dark bedrock, jointed into columns"""
    rng = random.Random(1760 + v)
    base = mix((63, 69, 80), (78, 85, 97), v * 0.4)
    im = canvas(bg=base)
    d = ImageDraw.Draw(im)
    grain(d, rng, base, 120, 0.9, 1.1, (1.2, 2.8))
    #hex-ish column joints
    seam = shade(base, 0.72)
    for cx in range(-1, 4):
        for cy in range(-1, 4):
            x = cx * 11 + (cy % 2) * 5.5 + rng.uniform(-0.8, 0.8)
            y = cy * 9.5 + rng.uniform(-0.8, 0.8)
            pts = [(x + math.cos(a) * 6.2, y + math.sin(a) * 6.2) for a in [math.tau * i / 6 + 0.26 for i in range(6)]]
            d.line([(p[0] * S, p[1] * S) for p in pts] + [(pts[0][0] * S, pts[0][1] * S)], fill=seam, width=S, joint="curve")
    flecks(d, rng, shade(base, 1.25), 16, (0.8, 1.6))
    return im


# ----------------------------------------------------------------------------------------------------------
# walls
# ----------------------------------------------------------------------------------------------------------

def wall(base, accent, v, seed):
    """a rock face: dark outline, lit top edge, a couple of facets"""
    rng = random.Random(seed + v)
    im = canvas(bg=INK)
    d = ImageDraw.Draw(im)
    body = [(2, 30), (3, 9 + v), (10, 3), (19, 5), (28, 2 + v), (30, 29)]
    d.polygon([(p[0] * S, p[1] * S) for p in body], fill=base)
    #top light, bottom shadow
    d.line([(3 * S, (9 + v) * S), (10 * S, 3 * S), (19 * S, 5 * S), (28 * S, (2 + v) * S)], fill=shade(base, 1.3), width=S, joint="curve")
    d.polygon([(2 * S, 30 * S), (30 * S, 29 * S), (30 * S, 24 * S), (2 * S, 25 * S)], fill=shade(base, 0.7))
    for _ in range(3):
        x1, y1 = rng.uniform(4, 26), rng.uniform(7, 24)
        d.line([(x1 * S, y1 * S), ((x1 + rng.uniform(-6, 6)) * S, (y1 + rng.uniform(3, 8)) * S)], fill=shade(base, 0.78), width=S)
    d.line([(5 * S, 13 * S), (26 * S, (10 + v) * S)], fill=accent, width=S)
    grain(d, rng, base, 40, 0.9, 1.12, (0.8, 1.6))
    return im


def wall_large(base, accent, seed):
    """the 2x2 'large' variant some static walls draw on wide rock"""
    rng = random.Random(seed + 77)
    im = canvas(64, INK)
    d = ImageDraw.Draw(im)
    body = [(3, 61), (5, 16), (20, 4), (42, 7), (59, 3), (61, 60)]
    d.polygon([(p[0] * S, p[1] * S) for p in body], fill=base)
    d.line([(5 * S, 16 * S), (20 * S, 4 * S), (42 * S, 7 * S), (59 * S, 3 * S)], fill=shade(base, 1.3), width=S, joint="curve")
    d.polygon([(3 * S, 61 * S), (61 * S, 60 * S), (61 * S, 50 * S), (3 * S, 52 * S)], fill=shade(base, 0.7))
    for _ in range(7):
        x1, y1 = rng.uniform(8, 54), rng.uniform(14, 48)
        d.line([(x1 * S, y1 * S), ((x1 + rng.uniform(-12, 12)) * S, (y1 + rng.uniform(6, 16)) * S)], fill=shade(base, 0.78), width=S)
    d.line([(9 * S, 26 * S), (54 * S, 20 * S)], fill=accent, width=S)
    for _ in range(90):
        x, y = rng.random() * 64, rng.random() * 64
        r = rng.uniform(0.8, 1.8)
        d.ellipse((x * S, y * S, (x + r) * S, (y + r) * S), fill=shade(base, rng.uniform(0.9, 1.12)))
    return im


# ----------------------------------------------------------------------------------------------------------
# wall ore: a cluster of crystals on a transparent tile, drawn over the rock
# ----------------------------------------------------------------------------------------------------------

def wall_ore(core, v, seed):
    rng = random.Random(seed + v)
    im = canvas()
    d = ImageDraw.Draw(im)
    for _ in range(rng.randint(4, 5)):
        x, y = rng.uniform(6, 26), rng.uniform(6, 26)
        r = rng.uniform(2.6, 4.4)
        pts = [(x, y - r), (x + r * 0.8, y - r * 0.2), (x + r * 0.5, y + r * 0.8), (x - r * 0.5, y + r * 0.8), (x - r * 0.8, y - r * 0.2)]
        d.polygon([(p[0] * S, p[1] * S) for p in pts], fill=INK)
        pts2 = [(x, y - r + 0.8), (x + r * 0.6, y - r * 0.15), (x + r * 0.38, y + r * 0.55), (x - r * 0.38, y + r * 0.55), (x - r * 0.6, y - r * 0.15)]
        d.polygon([(p[0] * S, p[1] * S) for p in pts2], fill=core)
        d.polygon([((x) * S, (y - r + 1.0) * S), ((x + r * 0.26) * S, (y) * S), ((x) * S, (y + r * 0.42) * S), ((x - r * 0.26) * S, y * S)],
                  fill=shade(core, 1.35))
    return im


def main():
    floors = [
        ("glass-flat", floor_glass),
        ("ember-ash", floor_ash),
        ("frost-crust", floor_frost),
        ("aurite-gravel", floor_gravel),
        ("resonance-silt", floor_silt),
        ("plasma-vent", floor_vent),
        ("basalt-shelf", floor_basalt),
    ]
    for name, fn in floors:
        for v in range(3):
            save(fn(v), f"{name}{v + 1}.png")

    walls = [
        ("glass-ridge-wall", (138, 160, 184), (214, 234, 248, 255), 2100),
        ("ember-wall", (104, 72, 62), (226, 128, 70, 255), 2200),
        ("basalt-wall", (58, 64, 76), (142, 162, 190, 255), 2300),
    ]
    for name, base, accent, seed in walls:
        for v in range(2):
            save(wall(base, accent, v, seed), f"{name}{v + 1}.png")
        save(wall_large(base, accent, seed), f"{name}-large.png")

    ores = [
        ("wall-ore-lumenite", (120, 216, 255, 255), 2400),
        ("wall-ore-aurite", (224, 182, 104, 255), 2500),
        ("wall-ore-resonance", (176, 142, 255, 255), 2600),
    ]
    for name, core, seed in ores:
        for v in range(3):
            save(wall_ore(core, v, seed), f"{name}{v + 1}.png")

    print(f"v7.9 terrain art written to {OUT}")


if __name__ == "__main__":
    main()
