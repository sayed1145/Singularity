#!/usr/bin/env python3
"""v8.5.1 ore art: 废料堆 (waste) and 星髓石 (star marrow).

Same pipeline as tools/gen_v79_terrain.py - everything is drawn from scratch at 4x and
downsampled with LANCZOS, each variant uses a fixed seed so the result is reproducible.
Ore sprites are 32x32 with a transparent background; the engine (OreBlock.createIcons)
adds the drop shadow itself, so the art only draws the ore.

Run from the project root:

    python3 tools/gen_v851_ores.py
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[1] / "assets" / "sprites" / "blocks" / "environment"
OUT.mkdir(parents=True, exist_ok=True)
S = 4          # supersample
N = 32         # final tile size


def canvas(bg=(0, 0, 0, 0)):
    return Image.new("RGBA", (N * S, N * S), bg)


def save(im, name):
    im.resize((im.width // S, im.height // S), Image.Resampling.LANCZOS).save(OUT / name)


def rgba(c):
    """矿石贴图必须带 alpha：三元组会被 PIL 当成 (r,g,b,0)，画出来整块全透明。"""
    return tuple(c) + (255,) if len(c) == 3 else tuple(c)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))),
            max(0, min(255, int(c[2] * f))), 255)


def chunk(d, rng, cx, cy, r, outline, fill, lit, squash=0.85, sides=None):
    outline, fill, lit = rgba(outline), rgba(fill), rgba(lit)
    """One angular lump: dark outline, body, and a lit facet facing the camera (north-west)."""
    n = sides or rng.randint(5, 7)
    pts = []
    for i in range(n):
        a = math.tau * i / n + rng.uniform(-0.22, 0.22)
        rr = r * rng.uniform(0.62, 1.0)
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr * squash))
    d.polygon([(p[0] * S, p[1] * S) for p in pts], fill=outline)
    # body, shrunk towards the centre so the outline reads as a rim
    inner = [(cx + (p[0] - cx) * 0.74, cy + (p[1] - cy) * 0.74) for p in pts]
    d.polygon([(p[0] * S, p[1] * S) for p in inner], fill=fill)
    # lit facet: the north-west third of the body
    top = [(cx + (p[0] - cx) * 0.40, cy + (p[1] - cy) * 0.40 - r * 0.12) for p in inner[:3]]
    if len(top) >= 3:
        d.polygon([(p[0] * S, p[1] * S) for p in top], fill=lit)
    return pts


def flecks(d, rng, col, count, r=(0.6, 1.5), box=None):
    col = rgba(col)
    x0, y0, x1, y1 = box or (4, 4, 28, 28)
    for _ in range(count):
        x, y = rng.uniform(x0, x1), rng.uniform(y0, y1)
        rr = rng.uniform(*r)
        d.ellipse((x * S, y * S, (x + rr) * S, (y + rr) * S), fill=col)


def crack(d, rng, pts, col, width=1):
    col = rgba(col)
    d.line([(p[0] * S, p[1] * S) for p in pts], fill=col, width=width * S, joint="curve")


# -----------------------------------------------------------------------------------------------------------
# 废料堆 - heaps of pressed industrial scrap: dull, grey-brown, with bare metal showing
# -----------------------------------------------------------------------------------------------------------

def ore_waste(v):
    rng = random.Random(5100 + v)
    im = canvas()
    d = ImageDraw.Draw(im)
    base = (107, 106, 116)     # #6b6a74, the item's own colour
    dark = (46, 45, 52)
    lit = (163, 162, 176)
    rust = (122, 106, 85)

    # three or four pressed lumps sitting in a loose pile
    for _ in range(rng.randint(5, 6)):
        cx, cy = rng.uniform(8, 24), rng.uniform(8, 24)
        chunk(d, rng, cx, cy, rng.uniform(3.0, 4.6), dark, base, shade(base, 1.35))
    # smaller loose pieces around them
    for _ in range(rng.randint(7, 9)):
        cx, cy = rng.uniform(6, 26), rng.uniform(6, 26)
        chunk(d, rng, cx, cy, rng.uniform(1.4, 2.4), dark, shade(base, 0.92), lit)
    # bare metal where the press sheared something
    flecks(d, rng, lit, rng.randint(4, 7), r=(0.7, 1.4))
    # a couple of rusty streaks - it has been lying in the weather a long time
    for _ in range(rng.randint(1, 2)):
        x, y = rng.uniform(8, 24), rng.uniform(8, 24)
        a = rng.uniform(0, math.tau)
        crack(d, rng, [(x, y), (x + math.cos(a) * 4, y + math.sin(a) * 4),
                       (x + math.cos(a) * 7 + rng.uniform(-1, 1), y + math.sin(a) * 7 + rng.uniform(-1, 1))],
              rust, width=1)
    return im


# -----------------------------------------------------------------------------------------------------------
# 星髓石 - stellar debris: a cooled black shell split open over a bone-white core
# -----------------------------------------------------------------------------------------------------------

def ore_star_marrow(v):
    rng = random.Random(5200 + v)
    im = canvas()
    d = ImageDraw.Draw(im)
    shell = (43, 38, 34)        # #2b2622
    shell_dark = (20, 17, 15)
    shell_lit = (84, 74, 64)
    core = (255, 244, 226)      # #fff4e2
    core_hot = (255, 230, 176)

    # 1) the shell: two or three chunks of cooled crust
    for _ in range(rng.randint(3, 4)):
        cx, cy = rng.uniform(9, 23), rng.uniform(9, 23)
        chunk(d, rng, cx, cy, rng.uniform(3.4, 5.0), shell_dark, shell, shell_lit)
    # grit around them
    for _ in range(rng.randint(6, 8)):
        cx, cy = rng.uniform(6, 26), rng.uniform(6, 26)
        chunk(d, rng, cx, cy, rng.uniform(1.2, 2.1), shell_dark, shell, shell_lit)

    # 2) the exposed marrow: glowing cracks running across the whole tile,
    #    drawn last so they read as light spilling out of the shell
    for _ in range(rng.randint(2, 3)):
        x, y = rng.uniform(5, 27), rng.uniform(5, 27)
        a = rng.uniform(0, math.tau)
        pts = [(x, y)]
        for _ in range(3):
            a += rng.uniform(-0.7, 0.7)
            step = rng.uniform(3.0, 5.5)
            x, y = x + math.cos(a) * step, y + math.sin(a) * step
            pts.append((x, y))
        crack(d, rng, pts, core, width=2)
        crack(d, rng, pts, core_hot, width=1)
    # 3) and a few bright splinters that caught the light
    flecks(d, rng, core, rng.randint(3, 5), r=(0.6, 1.2))
    return im


def main():
    ores = [
        ("ore-waste", ore_waste),
        ("ore-star-marrow", ore_star_marrow),
    ]
    for name, fn in ores:
        for v in range(3):
            save(fn(v), f"{name}{v + 1}.png")
    print(f"v8.5.1 ore art written to {OUT}")


if __name__ == "__main__":
    main()
