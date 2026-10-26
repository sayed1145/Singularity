#!/usr/bin/env python3
"""v8.5.1 belt + conduit art: 流明/通量 传送带与管道。

为什么重写
----------
这两族贴图是靠"相邻格子在同一条边上的开口严丝合缝"来接成一条线的。原贴图的边缘没铺满：

* 直线件：下边缘整行透明（原版是 0-31 全铺满），拼起来每格底边一条缝；
* 拐角件：左边缘和下边缘都透明，拐弯处直接断开；
* 管道：管口条带只到 0-5 / 23-30，比原版的 0-7 / 24-31 差 1~2 像素，每个接头都错开一点。

几何按官方仓库 core/assets-raw 里原版 conveyor / conduit 的贴图形状来（通道半宽 8px，
居中；拐角是以右上顶点为圆心、半径 16 的四分之一圆弧，外角按 39.6 的半径切圆角）。
配色沿用各家族现有的主色。

跑法（项目根目录）：

    python3 tools/gen_v851_belts.py
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[1] / "assets" / "sprites" / "blocks" / "aurelia"
OUT.mkdir(parents=True, exist_ok=True)

N = 32
# 所有距离都按"像素中心"算（像素 (x,y) 的中心是 (x+0.5, y+0.5)），
# 这样通道正好落在第 8..23 行/列，和原版一格不差。
C = 16.0          # 通道中心（连续坐标）
HALF = 8.0        # 通道半宽 -> 通道占 8..23，和原版一致
CUT = 39.6        # 拐角外角切圆角的半径阈值，取自原版 conveyor-1 / conduit-top-1 的边缘开口
BOTTOM_CURVE = 24.9   # 拐角液体层铺到内半径多少（原版 conduit-bottom-1 的边缘开口反推）

BELT_FRAMES = 4
CLEAT_STEP = 2.0  # 每帧前进的像素（周期 8px / 4 帧）


# -----------------------------------------------------------------------------------------------------------
# 路径片：每片给出 (sd, s, dir)
#   sd  < 0 表示像素落在通道里；s 是沿流向的弧长参数；dir=+1 表示流向 s 增大的方向
# -----------------------------------------------------------------------------------------------------------

def h_full(xc, yc):
    """横穿：西 -> 东"""
    return abs(yc - C) - HALF, xc, 1.0


def v_full(xc, yc):
    """竖穿：北 <-> 南"""
    return abs(xc - C) - HALF, yc, 1.0


def east_half(xc, yc):
    """中线 -> 东边缘"""
    return abs(yc - C) - HALF if xc >= C else 1e9, xc, 1.0


def west_half(xc, yc):
    """西边缘 -> 中线"""
    return abs(yc - C) - HALF if xc <= C else 1e9, xc, 1.0


def north_half(xc, yc):
    """北边缘 -> 中线"""
    return abs(xc - C) - HALF if yc <= C else 1e9, yc, 1.0


def south_half(xc, yc):
    """南边支路：由南边缘汇入中线，流向是 -y"""
    return abs(xc - C) - HALF if yc >= C else 1e9, yc, -1.0


ARC_X, ARC_Y, ARC_R = 32.0, 0.0, 16.0


def curve_ne(xc, yc):
    """北 -> 东 的拐角：以格子右上角 (32,0) 为圆心、半径 16 的四分之一圆弧。"""
    dx, dy = xc - ARC_X, yc - ARC_Y
    d = math.hypot(dx, dy)
    ang = math.atan2(dy, dx)                 # -pi/2 (北) .. 0 (东)
    return abs(d - ARC_R) - HALF, (ang + math.pi / 2) * ARC_R, 1.0


def curve_dist(xc, yc):
    return math.hypot(xc - ARC_X, yc - ARC_Y)


# 每个 variant 由哪些片组成（顺序无关，取 sd 最小的那片）
VARIANTS = {
    0: [h_full],                       # 直线 西->东
    1: [curve_ne],                     # 拐角 北->东（镜像后就是 南->东）
    2: [h_full, south_half],           # 直线 + 南侧支路
    3: [h_full, v_full],               # 十字
    4: [v_full, east_half],            # 三通：北、南 -> 东
}
CONVEYOR_VARIANTS = 7                  # 引擎按 lengths={7,4} 取 0..6；5、6 官方用不到，照直线画
CONDUIT_VARIANTS = 5


def sample(x, y, pieces):
    best = None
    for fn in pieces:
        sd, s, d = fn(x, y)
        if best is None or sd < best[0]:
            best = (sd, s, d)
    return best


def in_tile_cut(x, y, pieces):
    """拐角件的外角要切掉，形状跟原版一致。"""
    if curve_ne not in pieces:
        return False
    return curve_dist(x + 0.5, y + 0.5) > CUT


# -----------------------------------------------------------------------------------------------------------
# 传送带
# -----------------------------------------------------------------------------------------------------------

def conveyor_sprite(pieces, frame, pal):
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    px = im.load()
    for y in range(N):
        for x in range(N):
            if in_tile_cut(x, y, pieces):
                continue
            sd, s, d = sample(x + 0.5, y + 0.5, pieces)
            if sd <= 0:
                # 通道里：带面 + 随帧前进的挡条
                phase = (s * d - CLEAT_STEP * frame) % 8.0
                col = pal["cleat"] if phase < 2.6 else pal["belt"]
                # 通道靠边的一圈压暗，做出凹槽感
                if sd > -1.4:
                    col = shade(col, 0.72)
                px[x, y] = col + (255,)
            else:
                # 通道外：机体。离通道 1~2 像素处亮一道边，再往外是普通机体色
                if sd < 1.6:
                    col = pal["rail"]
                elif sd < 3.4:
                    col = shade(pal["rail"], 0.82)
                else:
                    col = pal["body"]
                px[x, y] = col + (255,)
    return im


# -----------------------------------------------------------------------------------------------------------
# 管道：top 是管壁（通道处透空，让液体从 bottom 透出来），bottom 是液体通道
# -----------------------------------------------------------------------------------------------------------

def conduit_top(pieces, pal):
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    px = im.load()
    for y in range(N):
        for x in range(N):
            if in_tile_cut(x, y, pieces):
                continue
            sd, s, d = sample(x + 0.5, y + 0.5, pieces)
            if sd <= 0:
                continue                       # 通道：留给 bottom（液体）
            if sd < 1.8:
                col = pal["accent"]            # 管口的一圈亮边
            elif sd < 4.0:
                col = pal["wall"]
            else:
                col = pal["wallDark"]
            px[x, y] = col + (255,)
    return im


def conduit_bottom(pieces, pal):
    """
    液体层。它画在管壁下面，只有管壁镂空的地方才看得见，所以原版在拐角和接头处
    直接把整个格子铺满（conduit-bottom-2/3/4 的边缘开口都是 0-31），直线件才只铺通道。
    照抄这个铺法，接头和弯管里就不会露出底色。
    """
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    px = im.load()
    full = curve_ne not in pieces and len(pieces) > 1      # 有支路的接头：整格铺满
    for y in range(N):
        for x in range(N):
            xc, yc = x + 0.5, y + 0.5
            if curve_ne in pieces:
                # 弯管：铺到内半径，把弧内侧填满
                if curve_dist(xc, yc) > BOTTOM_CURVE:
                    continue
                sd = abs(curve_dist(xc, yc) - ARC_R) - HALF
            elif full:
                sd = -8.0
            else:
                sd = sample(xc, yc, pieces)[0]
                if sd > 0:
                    continue
            col = pal["liquid"] if sd < -1.6 else shade(pal["liquid"], 0.86)
            px[x, y] = col + (255,)
    return im


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))),
            max(0, min(255, int(c[2] * f))))


# -----------------------------------------------------------------------------------------------------------

CONVEYORS = {
    "lumen-conveyor": dict(body=(0x60, 0x6a, 0x7d), rail=(0x8c, 0x97, 0xad),
                           belt=(0x3d, 0x45, 0x54), cleat=(0x8f, 0xe9, 0xff)),
    "flux-conveyor": dict(body=(0x63, 0x58, 0x84), rail=(0x8b, 0x7e, 0xb0),
                          belt=(0x3e, 0x36, 0x55), cleat=(0xcc, 0xba, 0xff)),
}

CONDUITS = {
    "lumen-conduit": dict(wall=(0xff, 0xff, 0xff), wallDark=(0x83, 0x87, 0x8d),
                          accent=(0xae, 0xf3, 0xff), liquid=(0xae, 0xb7, 0xc5)),
    "flux-conduit": dict(wall=(0xc7, 0xf5, 0xff), wallDark=(0x6a, 0x86, 0x8c),
                         accent=(0xaa, 0xf0, 0xff), liquid=(0x9a, 0xc4, 0xd6)),
}


def main():
    for name, pal in CONVEYORS.items():
        for v in range(CONVEYOR_VARIANTS):
            pieces = VARIANTS.get(v, VARIANTS[0])
            for f in range(BELT_FRAMES):
                conveyor_sprite(pieces, f, pal).save(OUT / f"{name}-{v}-{f}.png")

    for name, pal in CONDUITS.items():
        for v in range(CONDUIT_VARIANTS):
            pieces = VARIANTS[v]
            conduit_top(pieces, pal).save(OUT / f"{name}-top-{v}.png")
            conduit_bottom(pieces, pal).save(OUT / f"{name}-bottom-{v}.png")

    print(f"v8.5.1 belt/conduit art written to {OUT}")


if __name__ == "__main__":
    main()
