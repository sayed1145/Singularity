#!/usr/bin/env python3
"""Static release checks for the unified Singularity v5 expansion."""
from __future__ import annotations

import sys
import zipfile
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "Singularity.jar"
SPRITES = ROOT / "assets" / "sprites"

REQUIRED_JAVA = {
    "blackhole/BHTechTree.class", "blackhole/AstraContent.class", "blackhole/PaladinTitanType.class",
    "sixfold/AegisExpansion.class", "sixfold/SixfoldMod.class", "sixfold/WarpTurret.class",
}
REQUIRED_CONTENT = {
    "content/blocks/needlefall.hjson", "content/blocks/sky-net.hjson", "content/blocks/prism-lance.hjson",
    "content/blocks/crater.hjson", "content/blocks/boreline.hjson", "content/blocks/halo.hjson",
}
REQUIRED_SPRITES = {
    "astral-aegis.png", "astral-aegis-leg.png", "astral-aegis-joint.png", "astral-aegis-joint-base.png",
    "astral-aegis-foot.png", "astral-aegis-base.png", "astral-aegis-armor.png", "astral-aegis-crest.png",
    "astral-aegis-wing.png", "astral-aegis-halo.png", "astral-aegis-lance.png", "astral-aegis-pod.png",
    "astral-aegis-missile.png", "singularity-forge-array.png", "singularity-forge-core.png",
}


def need(value: bool, message: str):
    if not value:
        raise AssertionError(message)


def main() -> int:
    blocks = (ROOT / "src" / "blackhole" / "BHBlocks.java").read_text(encoding="utf-8")
    mod = (ROOT / "src" / "blackhole" / "BlackHoleMod.java").read_text(encoding="utf-8")
    sixfold = (ROOT / "src" / "sixfold" / "SixfoldMod.java").read_text(encoding="utf-8")
    astra = (ROOT / "src" / "blackhole" / "AstraContent.java").read_text(encoding="utf-8")

    forge_section = blocks[blocks.index('singularityForge ='):blocks.index('// ---------------- 炮塔')]
    need('new DrawContainmentArray()' in forge_section, 'Forge does not use the safe containment visual')
    need('new DrawBlackHole' not in forge_section, 'Forge still invokes the black-hole visual')
    need('lightRadius = 0f' in forge_section, 'Forge still creates a gameplay light source')
    need('animationBudget = 18' in blocks and 'Core.graphics.getFrameId()' in blocks,
         'Safe visual global animation budget is missing')
    need('BHTechTree.load();' in mod and 'AegisExpansion.load();' in mod and 'AstraContent.load();' in mod,
         'Unified content/tech load sequence is incomplete')
    need('blackhole-' in sixfold and 'sixfold-arsenal-' not in sixfold,
         'Imported Java still references the old independent mod namespace')
    need('astralAegis' in astra and 'plans.add' in astra and 'BHTechTree.node' in astra,
         'White Titan plan or tech tree link is missing')

    for sprite in REQUIRED_SPRITES:
        p = SPRITES / sprite
        need(p.is_file(), f'Missing hand-drawn v5 sprite: {sprite}')
        im = Image.open(p).convert('RGBA')
        need(im.getbbox() is not None, f'Empty sprite: {sprite}')

    need(JAR.is_file(), 'Singularity.jar has not been built')
    with zipfile.ZipFile(JAR) as z:
        names = set(z.namelist())
        need('classes.dex' in names, 'Android classes.dex is missing')
        for entry in REQUIRED_JAVA | REQUIRED_CONTENT:
            need(entry in names, f'Jar missing: {entry}')
        for sprite in REQUIRED_SPRITES:
            need(f'sprites/{sprite}' in names, f'Jar missing sprite: {sprite}')
        need(z.testzip() is None, 'Jar has compressed-data corruption')

    print('PASS: v5 unified content, tech, safe forge visual, white Titan art and packaged jar are consistent.')
    return 0


if __name__ == '__main__':
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f'FAIL: {exc}', file=sys.stderr)
        raise SystemExit(1)
