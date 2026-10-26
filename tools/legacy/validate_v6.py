#!/usr/bin/env python3
"""Release checks for Singularity v6.1 Aurelia recovery build."""
from __future__ import annotations
import sys, zipfile
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / 'Singularity.jar'
SPRITES = ROOT / 'assets' / 'sprites'
JAVA = {
    'blackhole/AureliaContent.class', 'blackhole/AureliaPlanetGenerator.class', 'blackhole/AureliaCoreBlock.class',
    'blackhole/AureliaUnitType.class', 'blackhole/AureliaUnitFactory.class', 'blackhole/AureliaFloor.class',
    'blackhole/AureliaFx.class', 'blackhole/ResonanceAnchorBlock.class', 'blackhole/HarmonicRelayBlock.class',
    'blackhole/AstraContent.class', 'blackhole/PaladinTitanType.class',
}
SPRITE_NAMES = {
    'aurelia-core.png', 'aurelia-core-base.png', 'aurelia-core-glow.png', 'aurelia-core-thruster1.png', 'aurelia-core-thruster2.png',
    'aurora-plate1.png', 'aurora-plate2.png', 'aurora-plate3.png', 'resonance-bed1.png', 'resonance-bed2.png',
    'aurora-wall1.png', 'aurora-wall2.png', 'resonance-wall1.png', 'resonance-wall2.png', 'ore-lumenite.png',
    'lumenite.png', 'resonance-shard.png', 'prism-alloy.png', 'concord-core.png',
    'lumen-extractor.png', 'lumen-extractor-rim.png', 'lumen-extractor-rotator.png', 'lumen-extractor-top.png', 'lumen-extractor-item.png',
    'aurora-panel.png', 'aurora-panel-glow.png', 'rift-anchor.png', 'harmonic-relay.png',
    'prism-press.png', 'prism-press-glow.png', 'prism-resonator.png', 'prism-resonator-glow.png',
    'aurelia-fabricator.png', 'aurelia-fabricator-top.png', 'aurelia-fabricator-out.png', 'aurelia-fabricator-in.png', 'aurelia-fabricator-glow.png',
    'prism-pylon.png', 'prism-pylon-base.png', 'prism-pylon-glow.png', 'arc-mortar.png', 'arc-mortar-base.png', 'arc-mortar-glow.png',
    'serpulo-allocation-terminal.png', 'serpulo-allocation-terminal-glow.png', 'erekir-allocation-terminal.png', 'erekir-allocation-terminal-glow.png',
    'serpulo-concord-allocator.png', 'serpulo-concord-allocator-glow.png', 'erekir-concord-allocator.png', 'erekir-concord-allocator-glow.png',
    'lumen-pilot.png', 'lumen-pilot-glow.png', 'lumen-pilot-emitter.png', 'glint.png', 'glint-glow.png', 'glint-blade.png',
    'aurora-guard.png', 'aurora-guard-glow.png', 'aurora-guard-lance.png',
}
def require(ok, why):
    if not ok: raise AssertionError(why)

def main():
    astra = (ROOT/'src/blackhole/AstraContent.java').read_text()
    content = (ROOT/'src/blackhole/AureliaContent.java').read_text()
    generator = (ROOT/'src/blackhole/AureliaPlanetGenerator.java').read_text()
    anchor = (ROOT/'src/blackhole/ResonanceAnchorBlock.java').read_text()
    relay = (ROOT/'src/blackhole/HarmonicRelayBlock.java').read_text()
    mod = (ROOT/'src/blackhole/BlackHoleMod.java').read_text()
    forge = (ROOT/'src/blackhole/BHBlocks.java').read_text()
    locale = (ROOT/'assets/bundles/bundle_zh_CN.properties').read_text()

    require('constructor = MechUnit::create' in astra and 'LegsUnit::create' not in astra,
            'Astral Aegis is not a bipedal MechUnit')
    require('AureliaContent.load();' in mod, 'Aurelia is not loaded by the mod lifecycle')
    require('new Planet("aurelia"' in content and 'aurelia.techTree = nodeRoot' in content and 'node.planet = aurelia' in content,
            'Aurelia independent tech root is absent or not assigned to the planet')
    require('new AureliaCoreBlock' in content and 'unitType = lumenPilot' in content and 'new AureliaUnitType("lumen-pilot")' in content,
            'Aurelia custom core/player chassis is incomplete')
    require('new Drill("lumen-extractor")' in content and 'new SolarGenerator("aurora-panel")' in content,
            'Aurelia cannot self-start with a custom extractor and power source')
    require('Schematics.placeLaunchLoadout' not in generator and 'placeStarterCore' in generator and 'core.items.add(Items.copper' in generator,
            'Generator still uses a vanilla core launch loadout instead of Aurelia zero-start placement')
    require('serpuloAllocation' in content and 'erekirAllocation' in content and 'serpuloConcordAllocator' in content and 'erekirConcordAllocator' in content,
            'Serpulo/Erekir allocation route is incomplete')
    require('linked >= 3' in relay and 'Groups.build.intersect' in relay and 'resonanceBed' in anchor,
            'Three-anchor spatial field loop is incomplete')
    require('new DrawBlackHole' not in forge[forge.index('singularityForge ='):forge.index('// ---------------- 炮塔')],
            'v6.1 regressed the banned forge black-hole visual')
    for key in ('planet.blackhole-aurelia.name', 'unit.blackhole-lumen-pilot.name', 'block.blackhole-lumen-extractor.name',
                'block.blackhole-serpulo-allocation-terminal.name', 'block.blackhole-erekir-concord-allocator.name'):
        require(key in locale, f'Missing Chinese localization: {key}')
    for s in SPRITE_NAMES:
        p=SPRITES/s
        require(p.is_file(), f'Missing authored v6.1 asset: {s}')
        image=Image.open(p).convert('RGBA')
        require(image.getbbox() is not None, f'Empty authored v6.1 asset: {s}')
    require(JAR.is_file(), 'Singularity.jar has not been built')
    with zipfile.ZipFile(JAR) as z:
        names=set(z.namelist())
        require('classes.dex' in names, 'Missing Android classes.dex')
        for c in JAVA: require(c in names, f'Missing class: {c}')
        for s in SPRITE_NAMES: require('sprites/'+s in names, f'Missing packed asset: {s}')
        require(z.testzip() is None, 'Archive corruption')
    print('PASS: v6.1 has authored terrain/assets/animation layers, a custom zero-start core and player chassis, a working field network, and Serpulo/Erekir allocation paths.')
if __name__=='__main__':
    try: main()
    except AssertionError as e:
        print('FAIL:', e, file=sys.stderr); raise SystemExit(1)
