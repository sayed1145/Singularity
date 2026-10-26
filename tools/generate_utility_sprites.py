#!/usr/bin/env python3
"""Generate the two v8.5 raster block sprites without vector/SVG assets or runtime font glyphs."""
from pathlib import Path
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[1] / "assets/sprites/blocks/3d"
OUT.mkdir(parents=True, exist_ok=True)

# Palette matches the existing Aurelia steel, violet controls, teal glow and orange furnace flame.
DARK=(36,48,54,255); STEEL=(218,230,228,255); MID=(113,135,138,255)
TEAL=(95,170,169,255); TEAL_L=(157,233,226,255); VIOLET=(163,149,195,255)
VIOLET_L=(206,191,244,255); FIRE=(255,151,63,255); FIRE_L=(255,225,156,255)
CLEAR=(0,0,0,0)

def px(im, scale):
    """Paint a scaled grid texture: deliberately crisp, raster Mindustry-style sprites."""
    return ImageDraw.Draw(im)

def furnace(grid=48):
    im=Image.new("RGBA",(grid,grid),CLEAR); d=px(im,1)
    # 2x2 machine base and chamfered frame.
    d.rounded_rectangle((2,3,45,45),radius=4,fill=DARK,outline=STEEL,width=2)
    d.polygon([(6,6),(41,6),(45,11),(45,39),(40,43),(7,43),(3,39),(3,10)],fill=MID)
    d.rectangle((6,7,42,41),fill=STEEL)
    # offset cylindrical burn chamber: rings, hot interior and rim highlights.
    d.ellipse((7,10,31,34),fill=DARK,outline=(20,29,31,255),width=2)
    d.ellipse((10,13,28,31),fill=(89,61,43,255),outline=FIRE,width=2)
    d.ellipse((13,16,25,28),fill=FIRE)
    d.ellipse((16,19,22,25),fill=FIRE_L)
    d.arc((7,10,31,34),208,335,fill=TEAL_L,width=2)
    # rear exhaust and a positively readable teal service cabinet.
    d.rounded_rectangle((32,8,40,26),radius=1,fill=DARK,outline=STEEL,width=1)
    d.rectangle((34,5,38,10),fill=MID,outline=STEEL)
    d.rectangle((33,30,43,40),fill=DARK,outline=STEEL)
    d.rectangle((35,32,41,34),fill=TEAL)
    for y in (36,38): d.line((35,y,41,y),fill=MID,width=1)
    # feet and rivets keep the silhouette crisp at icon scale.
    for x,y in ((6,40),(40,40),(6,7),(40,7)):
        d.rectangle((x,y,x+2,y+2),fill=TEAL)
    return im

def sorter(grid=32):
    im=Image.new("RGBA",(grid,grid),CLEAR);d=px(im,1)
    d.rounded_rectangle((1,1,30,30),radius=3,fill=DARK,outline=STEEL,width=1)
    d.rectangle((4,4,27,27),fill=STEEL)
    # Four recessed ports; the violet channels distinguish the reverse sorter from a standard sorter.
    d.rectangle((12,3,19,11),fill=DARK); d.rectangle((13,3,18,8),fill=VIOLET)
    d.rectangle((12,20,19,28),fill=DARK); d.rectangle((13,23,18,28),fill=VIOLET)
    d.rectangle((3,12,11,19),fill=DARK); d.rectangle((3,13,8,18),fill=VIOLET)
    d.rectangle((20,12,28,19),fill=DARK); d.rectangle((23,13,28,18),fill=VIOLET)
    d.ellipse((9,9,22,22),fill=DARK,outline=TEAL_L,width=1)
    d.ellipse((12,12,19,19),fill=TEAL)
    d.rectangle((15,10,17,21),fill=VIOLET_L)
    d.polygon([(10,15),(15,12),(15,15),(12,17)],fill=VIOLET_L)
    d.polygon([(22,17),(17,14),(17,17),(20,19)],fill=VIOLET_L)
    return im

def save_family(name, image, icon_size):
    # Normal and preview are identical on purpose: all detail/database/plan surfaces show the same real texture.
    image.save(OUT / f"{name}.png")
    image.save(OUT / f"{name}-preview.png")
    image.resize((icon_size,icon_size),Image.Resampling.LANCZOS).save(OUT / f"{name}-icon.png")
    image.resize((image.width*2,image.height*2),Image.Resampling.NEAREST).save(OUT / f"{name}-hd.png")

save_family("lumen-incinerator", furnace(), 36)
save_family("inverted-lumen-sorter", sorter(), 24)
