#!/usr/bin/env python3
"""Static integrity checks for Singularity v4.1.

This checks the source tree *and* the packaged jar, including the TankUnit tread
source column that Mindustry slices into movement-driven frames. Run after
``/home/user/build/build.sh`` (or your equivalent release build).
"""
from __future__ import annotations

import sys
import zipfile
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SPRITES = ROOT / "assets" / "sprites"
JAR = ROOT / "Singularity.jar"

REQUIRED = {
    "citadel-treads.png", "accretor-treads.png",
    "citadel-array.png", "citadel-core.png", "citadel-hatch.png", "citadel-armor.png",
    "citadel-main-ring.png", "citadel-main-vent.png", "citadel-main-core.png",
    "citadel-aa-ring.png", "citadel-side-ring.png",
}
FORBIDDEN = {"drone.png", "drone-cell.png", "drone-weapon.png"}


def require(condition: bool, message: str):
    if not condition:
        raise AssertionError(message)


def dimensions(name: str, expected: tuple[int, int], sample_column: int):
    image = Image.open(SPRITES / name).convert("RGBA")
    require(image.size == expected, f"{name}: expected {expected}, got {image.size}")
    alpha = image.getchannel("A")
    # TankUnit's generated animation samples this whole source column.
    require(all(alpha.getpixel((sample_column, y)) > 0 for y in range(image.height)),
            f"{name}: sampled tread column {sample_column} contains transparency")


def main() -> int:
    units = (ROOT / "src" / "blackhole" / "BHUnits.java").read_text(encoding="utf-8")
    bullets = (ROOT / "src" / "blackhole" / "BHBullets.java").read_text(encoding="utf-8")
    zh = (ROOT / "assets" / "bundles" / "bundle_zh_CN.properties").read_text(encoding="utf-8")
    en = (ROOT / "assets" / "bundles" / "bundle.properties").read_text(encoding="utf-8")

    require("new UnitSpawnAbility" not in units, "Citadel still has a UnitSpawnAbility reference")
    require("drone" not in units.lower(), "BHUnits still defines or references the drone unit")
    require("droneShot" not in bullets, "BHBullets still defines the removed drone ammunition")
    require("unit.blackhole-drone" not in zh and "unit.blackhole-drone" not in en,
            "Drone localization entries were not fully removed")
    require("new Rect(-116f, -116f, 36f, 232f)" in units,
            "Citadel does not use the verified full-height tread source rect")
    require("treadFrames = 4" in units and "treadPullOffset = 7" in units,
            "Citadel tread animation settings are incomplete")

    for filename in REQUIRED:
        require((SPRITES / filename).is_file(), f"Missing required sprite: {filename}")
    for filename in FORBIDDEN:
        require(not (SPRITES / filename).exists(), f"Removed drone sprite still exists: {filename}")

    dimensions("citadel-treads.png", (232, 232), 0)
    dimensions("accretor-treads.png", (96, 96), 0)

    require(JAR.is_file(), "Singularity.jar has not been built")
    with zipfile.ZipFile(JAR) as archive:
        names = set(archive.namelist())
        require("classes.dex" in names, "Jar is missing Android classes.dex")
        require("blackhole/BHUnits.class" in names, "Jar is missing compiled BHUnits.class")
        require("mod.hjson" in names, "Jar is missing mod.hjson")
        for filename in REQUIRED:
            require(f"sprites/{filename}" in names, f"Jar is missing sprites/{filename}")
        for filename in FORBIDDEN:
            require(f"sprites/{filename}" not in names, f"Jar still contains removed sprites/{filename}")

    print("PASS: v4.1 source, tread columns, sprite set, and packaged jar are consistent.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as error:
        print(f"FAIL: {error}", file=sys.stderr)
        raise SystemExit(1)
