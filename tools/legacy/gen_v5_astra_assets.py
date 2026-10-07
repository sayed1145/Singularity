#!/usr/bin/env python3
"""Hand-drawn v5 assets: the white Astral Aegis Titan and the safe forge retrofit.

No external art is downloaded or composited.  The resulting textures are deliberately
layered so PaladinTitanType can animate armor, fins, halo and crest as cheap quads.
"""
from __future__ import annotations

import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

OUT = Path(__file__).resolve().parents[1] / "assets" / "sprites"
OUT.mkdir(parents=True, exist_ok=True)
S = 4

INK = (20, 30, 47, 255)
NAVY = (39, 57, 82, 255)
STEEL = (91, 116, 148, 255)
WHITE = (242, 247, 255, 255)
SILVER = (196, 215, 234, 255)
CYAN = (114, 215, 255, 255)
BLUE = (93, 143, 255, 255)
GOLD = (255, 204, 95, 255)
PURPLE = (180, 149, 255, 255)


def a(c, alpha): return (c[0], c[1], c[2], alpha)


class Canvas:
    def __init__(self, w, h=None):
        self.w, self.h = w, h or w
        self.im = Image.new("RGBA", (w * S, (h or w) * S), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im, "RGBA")

    def poly(self, pts, fill=None, outline=None, width=1):
        p = [(round(x*S), round(y*S)) for x, y in pts]
        self.d.polygon(p, fill=fill)
        if outline:
            self.d.line(p + [p[0]], fill=outline, width=max(1, round(width*S)), joint="curve")

    def rect(self, x0,y0,x1,y1, fill=None, outline=None, width=1, radius=0):
        r=(round(x0*S),round(y0*S),round(x1*S),round(y1*S))
        if radius:
            self.d.rounded_rectangle(r, radius=round(radius*S), fill=fill, outline=outline, width=max(1,round(width*S)))
        else:
            self.d.rectangle(r, fill=fill, outline=outline, width=max(1,round(width*S)))

    def line(self, pts, fill, width=1):
        self.d.line([(round(x*S),round(y*S)) for x,y in pts], fill=fill, width=max(1,round(width*S)), joint="curve")

    def ellipse(self, x0,y0,x1,y1, fill=None, outline=None, width=1):
        self.d.ellipse((round(x0*S),round(y0*S),round(x1*S),round(y1*S)),fill=fill,outline=outline,width=max(1,round(width*S)))

    def save(self, name, glow=0):
        out = self.im.resize((self.w,self.h),Image.Resampling.LANCZOS)
        if glow:
            out = Image.alpha_composite(out.filter(ImageFilter.GaussianBlur(glow)), out)
        out.save(OUT/(name+'.png'))
        print(f'  {name}.png {self.w}x{self.h}')


def hex_pts(cx,cy,r,phase=0):
    return [(cx+math.cos(math.radians(phase+i*60))*r,cy+math.sin(math.radians(phase+i*60))*r) for i in range(6)]


def astral_body():
    c=Canvas(192); x=96
    # Backpack and shoulder silhouette.
    c.poly([(42,70),(69,52),(82,62),(110,62),(123,52),(150,70),(159,116),(139,141),(119,136),(96,166),(73,136),(53,141),(33,116)], NAVY, INK, 2)
    # Wide layered shoulder armor.
    for s in (-1,1):
        c.poly([(x+s*16,70),(x+s*57,62),(x+s*70,84),(x+s*59,108),(x+s*30,101)], WHITE, INK, 2)
        c.poly([(x+s*27,74),(x+s*53,70),(x+s*59,84),(x+s*31,92)], SILVER, a(INK,220), 1.3)
        c.rect(x+s*56-5,88,x+s*56+5,105, fill=BLUE, outline=INK, width=1, radius=2)
    # Core torso.
    c.poly([(72,82),(120,82),(132,111),(117,147),(96,163),(75,147),(60,111)], WHITE, INK,2)
    c.poly([(78,91),(114,91),(122,112),(111,137),(96,148),(81,137),(70,112)], SILVER,a(INK,230),1.2)
    c.poly([(83,100),(109,100),(116,114),(96,133),(76,114)], WHITE,a(INK,220),1)
    c.poly([(96,104),(105,115),(96,127),(87,115)], fill=CYAN, outline=INK, width=1)
    c.ellipse(89,108,103,122, fill=a(BLUE,230), outline=a(WHITE,230),width=1)
    # Waist modules.
    c.poly([(67,139),(91,145),(96,162),(74,169),(61,158)], STEEL, INK,1.5)
    c.poly([(125,139),(101,145),(96,162),(118,169),(131,158)], STEEL, INK,1.5)
    # Head: pale helmet + cyan visor + gold V-crest.
    c.poly([(80,43),(96,30),(112,43),(110,67),(96,78),(82,67)], WHITE, INK,2)
    c.poly([(85,47),(107,47),(105,57),(96,62),(87,57)], fill=CYAN,outline=INK,width=1)
    c.poly([(80,42),(71,24),(91,40),(96,18),(101,40),(121,24),(112,42)], GOLD, INK,1.3)
    # Armor panel seams and small emitters.
    for yy in (119,127,135): c.line([(80,yy),(112,yy)],a(NAVY,185),.8)
    for sx in (-1,1):
        c.ellipse(96+sx*31-4,100,96+sx*31+4,108,fill=CYAN,outline=INK,width=.7)
    c.save('astral-aegis',1.05)


def leg_assets():
    c=Canvas(40,76)
    c.poly([(8,3),(31,7),(35,31),(26,43),(30,68),(16,74),(9,44),(4,30)], WHITE, INK,1.5)
    c.poly([(12,10),(28,12),(29,27),(17,33),(9,26)], SILVER,a(INK,210),.8)
    c.rect(15,38,28,58,fill=STEEL,outline=INK,width=1,radius=2)
    c.line([(18,42),(26,54)],a(CYAN,190),1)
    c.save('astral-aegis-leg',.5)
    c=Canvas(44)
    c.ellipse(5,5,39,39,fill=NAVY,outline=INK,width=1.4)
    c.ellipse(11,11,33,33,fill=SILVER,outline=a(INK,220),width=1)
    c.ellipse(17,17,27,27,fill=CYAN,outline=INK,width=.8)
    c.save('astral-aegis-joint',.6)
    c=Canvas(44)
    c.ellipse(4,4,40,40,fill=STEEL,outline=INK,width=1.2)
    c.ellipse(12,12,32,32,outline=a(WHITE,200),width=1)
    c.save('astral-aegis-joint-base')
    c=Canvas(48,32)
    c.poly([(3,7),(33,4),(46,17),(40,28),(7,29)],WHITE,INK,1.5)
    c.poly([(9,11),(33,9),(39,17),(14,21)],SILVER,a(INK,220),.8)
    c.rect(17,23,38,27,fill=BLUE,outline=INK,width=.7,radius=1)
    c.save('astral-aegis-foot',.45)
    c=Canvas(48)
    c.ellipse(4,4,44,44,fill=NAVY,outline=INK,width=1.3)
    c.ellipse(10,10,38,38,outline=a(CYAN,180),width=1)
    c.ellipse(16,16,32,32,fill=a(BLUE,160),outline=INK,width=.8)
    c.save('astral-aegis-base',.7)


def overlays():
    c=Canvas(192)
    # transparent overlay: symmetric plate outlines/light strips over the chassis
    for s in (-1,1):
        c.line([(96+s*25,74),(96+s*59,85),(96+s*50,116),(96+s*24,126)],a(CYAN,170),1.1)
        c.line([(96+s*22,134),(96+s*43,144),(96+s*28,157)],a(WHITE,190),1)
    c.poly([(96,78),(117,111),(96,151),(75,111)],outline=a(BLUE,185),width=1.2)
    c.save('astral-aegis-armor',.8)

    c=Canvas(72); cx=cy=36
    for r,col,al in ((30,CYAN,180),(23,BLUE,160),(15,WHITE,145)):
        c.ellipse(cx-r,cy-r,cx+r,cy+r,outline=a(col,al),width=1.1)
    for i in range(6):
        ang=math.radians(i*60+30); px=cx+math.cos(ang)*26; py=cy+math.sin(ang)*26
        c.ellipse(px-2,py-2,px+2,py+2,fill=CYAN,outline=INK,width=.35)
    c.save('astral-aegis-crest',1.05)

    c=Canvas(82,104)
    c.poly([(10,5),(71,15),(78,39),(53,52),(70,95),(42,89),(23,57),(7,42)],WHITE,INK,1.5)
    c.poly([(16,13),(65,20),(67,34),(39,43),(16,33)],SILVER,a(INK,220),.8)
    c.poly([(27,55),(57,67),(64,88),(44,82)],STEEL,a(INK,210),.8)
    c.line([(22,38),(59,48)],a(CYAN,210),1.2)
    c.save('astral-aegis-wing',.65)

    c=Canvas(184);cx=cy=92
    for r,col,al in ((80,CYAN,75),(67,BLUE,115),(51,WHITE,90)):
        c.ellipse(cx-r,cy-r,cx+r,cy+r,outline=a(col,al),width=1.3)
    c.poly(hex_pts(cx,cy,75,30),outline=a(CYAN,110),width=1)
    c.save('astral-aegis-halo',1.2)


def weapons():
    c=Canvas(72,96)
    c.poly([(18,88),(54,88),(62,54),(49,18),(39,4),(33,4),(23,18),(10,54)],WHITE,INK,1.6)
    c.poly([(25,80),(47,80),(52,51),(43,23),(36,12),(29,23),(20,51)],SILVER,a(INK,220),.9)
    for y in (31,43,55): c.rect(27,y,45,y+4,fill=STEEL,outline=INK,width=.6,radius=1)
    c.rect(30,6,42,32,fill=NAVY,outline=INK,width=1,radius=2)
    c.rect(33,7,39,27,fill=CYAN,outline=a(WHITE,180),width=.7,radius=1)
    c.save('astral-aegis-lance',.8)
    c=Canvas(52)
    c.poly([(7,10),(44,8),(49,38),(38,48),(10,43)],WHITE,INK,1.3)
    c.poly([(13,16),(39,14),(42,26),(16,30)],SILVER,a(INK,210),.8)
    for yy in (34,39):c.rect(18,yy,36,yy+3,fill=BLUE,outline=INK,width=.5,radius=.5)
    c.ellipse(22,19,30,27,fill=CYAN,outline=INK,width=.7)
    c.save('astral-aegis-pod',.55)
    c=Canvas(24,32)
    c.poly([(8,2),(16,2),(21,22),(12,30),(3,22)],WHITE,INK,1)
    c.poly([(10,5),(14,5),(16,20),(12,24),(8,20)],CYAN,a(INK,220),.6)
    c.save('astral-aegis-missile',.5)


def safe_forge():
    c=Canvas(128);cx=cy=64
    # white/blue containment reactor: explicitly no dark event horizon or orange bloom
    c.rect(5,5,123,123,fill=NAVY,outline=INK,width=1.5,radius=8)
    c.rect(13,13,115,115,fill=STEEL,outline=INK,width=1,radius=6)
    c.poly([(64,19),(104,41),(104,87),(64,109),(24,87),(24,41)],fill=WHITE,outline=INK,width=1.2)
    c.poly(hex_pts(cx,cy,34,30),fill=NAVY,outline=a(CYAN,240),width=1.3)
    c.ellipse(45,45,83,83,fill=a(BLUE,150),outline=a(WHITE,220),width=1.2)
    c.ellipse(55,55,73,73,fill=CYAN,outline=INK,width=1)
    for i in range(4):
        x=22+i*28
        c.rect(x,103,x+14,110,fill=BLUE,outline=INK,width=.5,radius=1)
    c.save('singularity-forge',.75)
    c=Canvas(96);cx=cy=48
    for r,col,al in ((40,CYAN,180),(31,PURPLE,150),(23,WHITE,120)):
        c.ellipse(cx-r,cy-r,cx+r,cy+r,outline=a(col,al),width=1.1)
    c.poly(hex_pts(cx,cy,36,0),outline=a(CYAN,180),width=1)
    for i in range(6):
        ang=math.radians(i*60);x=cx+math.cos(ang)*35;y=cy+math.sin(ang)*35
        c.ellipse(x-2,y-2,x+2,y+2,fill=CYAN,outline=INK,width=.3)
    c.save('singularity-forge-array',1.0)
    c=Canvas(48);cx=cy=24
    for r,col,al in ((20,CYAN,45),(15,BLUE,90),(10,WHITE,160)):
        c.ellipse(cx-r,cy-r,cx+r,cy+r,fill=a(col,al))
    c.ellipse(15,15,33,33,fill=CYAN,outline=a(WHITE,220),width=1)
    c.ellipse(20,20,28,28,fill=WHITE)
    c.save('singularity-forge-core',1.35)


def main():
    print('Generating v5 Astral Aegis and safe-reactor assets:')
    astral_body(); leg_assets(); overlays(); weapons(); safe_forge()
    print('Done.')

if __name__ == '__main__': main()
