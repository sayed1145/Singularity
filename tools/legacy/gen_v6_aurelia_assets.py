#!/usr/bin/env python3
"""Reproducible original raster art for the Aurelia v6 planet expansion.
No external sprites are used. Run from project root: python3 tools/gen_v6_aurelia_assets.py
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

OUT = Path(__file__).resolve().parents[1] / "assets" / "sprites"
OUT.mkdir(parents=True, exist_ok=True)
S = 4
WHITE=(231,244,255,255); INK=(18,31,52,255); BLUE=(102,214,255,255); VIOLET=(166,132,255,255); GOLD=(255,213,111,255); STEEL=(76,100,136,255)

def canvas(n): return Image.new("RGBA", (n*S,n*S), (0,0,0,0))
def poly(d, pts, fill, outline=None, width=1):
    pts=[(int(x*S),int(y*S)) for x,y in pts]; d.polygon(pts,fill=fill)
    if outline:d.line(pts+[pts[0]],fill=outline,width=width*S,joint="curve")
def line(d, pts, fill, width=1): d.line([(x*S,y*S) for x,y in pts],fill=fill,width=width*S,joint="curve")
def ellipse(d,box,fill,outline=None,width=1):d.ellipse(tuple(int(x*S) for x in box),fill=fill,outline=outline,width=width*S)
def rect(d,box,fill,outline=None,width=1):d.rectangle(tuple(int(x*S) for x in box),fill=fill,outline=outline,width=width*S)
def save(im,name): im.resize((im.width//S,im.height//S),Image.Resampling.LANCZOS).save(OUT/name)

def gem(name, core, n=32):
    im=canvas(n);d=ImageDraw.Draw(im)
    poly(d,[(n*.5,3),(n-5,n*.38),(n*.68,n-4),(n*.32,n-4),(5,n*.38)],INK)
    poly(d,[(n*.5,5),(n-7,n*.4),(n*.64,n-6),(n*.36,n-6),(7,n*.4)],core,INK,1)
    poly(d,[(n*.5,6),(n*.62,n*.43),(n*.5,n*.72),(n*.38,n*.43)],WHITE)
    line(d,[(n*.5,7),(n*.5,n*.72)],(255,255,255,160),1)
    save(im,name)

def floor(name, base, accent, variant=0):
    im=canvas(32);d=ImageDraw.Draw(im)
    rect(d,(0,0,32,32),base)
    # segmented sci-fi plate seams that tile acceptably
    line(d,[(0,6+variant),(15,3),(32,8+variant)],INK,1)
    line(d,[(0,24-variant),(12,27),(32,21-variant)],INK,1)
    line(d,[(16,3),(15,29)],(183,223,255,100),1)
    poly(d,[(6,12),(12,8),(18,12),(15,18),(8,18)],accent)
    ellipse(d,(10,12,14,16),WHITE)
    save(im,name)

def block(name,n,accent=BLUE,kind="relay"):
    im=canvas(n);d=ImageDraw.Draw(im)
    m=max(4,n*.13); poly(d,[(m,n*.35),(n*.5,m),(n-m,n*.35),(n-m,n*.75),(n*.5,n-m),(m,n*.75)],INK)
    poly(d,[(m+3,n*.36),(n*.5,m+3),(n-m-3,n*.36),(n-m-3,n*.72),(n*.5,n-m-3),(m+3,n*.72)],STEEL,INK,1)
    if kind=="anchor":
        ellipse(d,(n*.28,n*.28,n*.72,n*.72),(20,40,72,255),accent,2)
        ellipse(d,(n*.38,n*.38,n*.62,n*.62),accent,WHITE,1)
        for a,b in [(.5,.15),(.83,.5),(.5,.85),(.17,.5)]: line(d,[(n*.5,n*.5),(n*a,n*b)],accent,1)
    elif kind=="relay":
        for r,col in [(n*.27,VIOLET),(n*.17,BLUE),(n*.07,WHITE)]: ellipse(d,(n*.5-r,n*.5-r,n*.5+r,n*.5+r),(0,0,0,0),col,2)
        for a,b in [(.2,.22),(.8,.22),(.5,.83)]:line(d,[(n*.5,n*.5),(n*a,n*b)],BLUE,1)
    elif kind=="factory":
        rect(d,(n*.22,n*.26,n*.78,n*.74),INK,accent,2); rect(d,(n*.31,n*.34,n*.69,n*.62),(30,52,86,255),WHITE,1)
        for x in (.35,.5,.65):ellipse(d,(n*x-2,n*.7-2,n*x+2,n*.7+2),accent)
    elif kind=="turret":
        ellipse(d,(n*.22,n*.22,n*.78,n*.78),STEEL,INK,2); ellipse(d,(n*.38,n*.38,n*.62,n*.62),accent,WHITE,1)
        poly(d,[(n*.45,n*.42),(n*.55,n*.42),(n*.74,n*.08),(n*.26,n*.08)],accent,INK,1)
    else:
        poly(d,[(n*.5,n*.17),(n*.76,n*.33),(n*.76,n*.68),(n*.5,n*.83),(n*.24,n*.68),(n*.24,n*.33)],accent,INK,1)
    save(im,name)

def unit(name,n,accent,guard=False):
    im=canvas(n);d=ImageDraw.Draw(im)
    # unmistakably hovering aircraft silhouette, distinct from the bipedal Astral Aegis
    poly(d,[(n*.5,n*.08),(n*.65,n*.31),(n*.9,n*.54),(n*.68,n*.61),(n*.60,n*.88),(n*.4,n*.88),(n*.32,n*.61),(n*.1,n*.54),(n*.35,n*.31)],INK)
    poly(d,[(n*.5,n*.12),(n*.62,n*.35),(n*.84,n*.53),(n*.62,n*.57),(n*.56,n*.82),(n*.44,n*.82),(n*.38,n*.57),(n*.16,n*.53),(n*.38,n*.35)],WHITE,INK,1)
    poly(d,[(n*.5,n*.22),(n*.63,n*.42),(n*.5,n*.58),(n*.37,n*.42)],accent,INK,1)
    rect(d,(n*.43,n*.61,n*.57,n*.77),STEEL,INK,1)
    if guard:
        for x in (.17,.83): poly(d,[(n*x,n*.45),(n*(x-.12 if x<.5 else x+.12),n*.7),(n*x,n*.64)],accent,INK,1)
        ellipse(d,(n*.43,n*.64,n*.57,n*.78),GOLD,WHITE,1)
    save(im,name)

def glow(name, n, color, rings=2):
    im=canvas(n); d=ImageDraw.Draw(im)
    for i in range(rings):
        r=n*(.17+i*.10)
        ellipse(d,(n*.5-r,n*.5-r,n*.5+r,n*.5+r),(0,0,0,0),color,1)
    ellipse(d,(n*.5-2,n*.5-2,n*.5+2,n*.5+2),color)
    save(im,name)

def wall(name, base, accent, variant=0):
    im=canvas(32); d=ImageDraw.Draw(im)
    rect(d,(0,0,32,32),INK)
    poly(d,[(2,30),(4,8+variant),(12,3),(20,6),(29,2+variant),(31,29)],base,INK,1)
    line(d,[(5,12),(27,9+variant)],accent,1); line(d,[(4,23),(29,18)],STEEL,1)
    save(im,name)

def drill_parts():
    block("lumen-extractor.png",64,BLUE,"factory")
    im=canvas(64); d=ImageDraw.Draw(im)
    ellipse(d,(14,14,50,50),(0,0,0,0),BLUE,2); line(d,[(32,14),(32,50)],BLUE,2); line(d,[(14,32),(50,32)],BLUE,2); save(im,"lumen-extractor-rim.png")
    im=canvas(64); d=ImageDraw.Draw(im)
    poly(d,[(32,7),(42,23),(35,32),(42,41),(32,57),(22,41),(29,32),(22,23)],BLUE,INK,1); save(im,"lumen-extractor-rotator.png")
    im=canvas(64); d=ImageDraw.Draw(im)
    poly(d,[(17,48),(17,23),(32,15),(47,23),(47,48),(32,55)],(31,51,83,200),WHITE,1); save(im,"lumen-extractor-top.png")
    im=canvas(64); d=ImageDraw.Draw(im); ellipse(d,(27,27,37,37),WHITE,BLUE,1); save(im,"lumen-extractor-item.png")

# resources
for name,col in [("lumenite.png",BLUE),("resonance-shard.png",VIOLET),("prism-alloy.png",WHITE),("concord-core.png",GOLD)]:gem(name,col)
# terrain: every procedural floor, ore and cliff uses authored art.
floor("aurora-plate.png",(50,68,98,255),BLUE)
floor("aurora-plate1.png",(50,68,98,255),BLUE,0); floor("aurora-plate2.png",(56,74,105,255),BLUE,1); floor("aurora-plate3.png",(43,61,91,255),BLUE,2)
floor("resonance-bed.png",(77,58,112,255),VIOLET)
floor("resonance-bed1.png",(77,58,112,255),VIOLET,0); floor("resonance-bed2.png",(88,65,126,255),VIOLET,2)
wall("aurora-wall.png",(48,69,102,255),BLUE); wall("aurora-wall1.png",(48,69,102,255),BLUE,0); wall("aurora-wall2.png",(57,77,112,255),BLUE,2)
wall("resonance-wall.png",(75,57,105,255),VIOLET); wall("resonance-wall1.png",(75,57,105,255),VIOLET,0); wall("resonance-wall2.png",(88,63,121,255),VIOLET,2)
gem("ore-lumenite.png",BLUE,32)

# core and its calm rotating overlay; no borrowed coreShard frame is used.
block("aurelia-core.png",96,GOLD,"core"); block("aurelia-core-base.png",96,BLUE,"factory"); glow("aurelia-core-glow.png",96,GOLD,3)
for n in ("aurelia-core-thruster1.png","aurelia-core-thruster2.png"):
    im=canvas(32); d=ImageDraw.Draw(im); poly(d,[(16,2),(25,25),(16,30),(7,25)],BLUE,INK,1); save(im,n)

# field loop, factories and all animation overlays.
drilL = drill_parts()
for name,n,col,kind in [
    ("aurora-panel.png",64,BLUE,"relay"),("rift-anchor.png",64,BLUE,"anchor"),("harmonic-relay.png",96,VIOLET,"relay"),
    ("prism-press.png",96,BLUE,"factory"),("prism-resonator.png",96,GOLD,"factory"),("aurelia-fabricator.png",128,VIOLET,"factory"),
    ("serpulo-allocation-terminal.png",96,BLUE,"factory"),("erekir-allocation-terminal.png",96,VIOLET,"factory"),
    ("serpulo-concord-allocator.png",96,GOLD,"factory"),("erekir-concord-allocator.png",96,GOLD,"factory"),
    ("prism-pylon.png",96,BLUE,"turret"),("arc-mortar.png",128,GOLD,"turret")
]:
    block(name,n,col,kind)
    glow(name[:-4]+"-glow.png",n,col,3 if n>=96 else 2)
# Payload factories otherwise borrow generic factory top/out/in pieces; author those too.
for suffix, col in [("-top.png",WHITE),("-out.png",BLUE),("-in.png",VIOLET)]:
    im=canvas(128); d=ImageDraw.Draw(im)
    if suffix == "-top.png":
        ellipse(d,(42,42,86,86),(0,0,0,0),col,2)
    else:
        poly(d,[(64,10),(98,64),(64,118),(30,64)],(0,0,0,0),col,2)
    save(im,"aurelia-fabricator"+suffix)
# DrawTurret needs an authored base, otherwise it falls back to vanilla block-N plates.
for name,n,col in [("prism-pylon",96,BLUE),("arc-mortar",128,GOLD)]:
    im=canvas(n); d=ImageDraw.Draw(im); ellipse(d,(n*.12,n*.12,n*.88,n*.88),STEEL,INK,2); ellipse(d,(n*.30,n*.30,n*.70,n*.70),col,WHITE,1); save(im,name+"-base.png")

# self-controlled launch chassis + field units; all have animated glow layers.
unit("lumen-pilot.png",56,BLUE); glow("lumen-pilot-glow.png",56,BLUE,2)
unit("glint.png",48,BLUE); glow("glint-glow.png",48,BLUE,2)
unit("aurora-guard.png",72,VIOLET,True); glow("aurora-guard-glow.png",72,VIOLET,3)
# custom named weapon overlays
for name,col in [("lumen-pilot-emitter.png",BLUE),("glint-blade.png",BLUE),("aurora-guard-lance.png",VIOLET)]:
    im=canvas(32);d=ImageDraw.Draw(im);poly(d,[(15,30),(11,13),(15,2),(19,13)],col,INK,1);save(im,name)
print("wrote Aurelia v6.1 authored terrain, core, machine and unit sprite set to",OUT)
