#!/usr/bin/env python3
"""Static release audit: original hashes, exact class/asset packaging, DEX and deleted content."""
from pathlib import Path
import hashlib,json,struct,sys,zipfile
root=Path(__file__).resolve().parents[1]
jar=Path(sys.argv[1]) if len(sys.argv)>1 else next((p for p in [root/'dist/Singularity-v8.4-beta.jar',root/'Singularity-v8.4-beta.jar',root.parent/'Singularity-v8.4-beta.jar'] if p.exists()),root/'dist/Singularity-v8.4-beta.jar')
sha=lambda b:hashlib.sha256(b).hexdigest()
base=json.loads((root/'docs/v83-baseline-hashes.json').read_text())
current={p.relative_to(root).as_posix():sha(p.read_bytes()) for folder in ['src','assets'] for p in (root/folder).rglob('*') if p.is_file()}
changes={'modified':sorted(k for k in base.keys()&current.keys() if base[k]!=current[k]),'deleted':sorted(base.keys()-current.keys()),'added':sorted(current.keys()-base.keys()),'unchanged':sum(base[k]==current[k] for k in base.keys()&current.keys())}
with zipfile.ZipFile(jar) as z:
    assert z.testzip() is None,'JAR CRC failure'
    names=z.namelist();classes=[n for n in names if n.endswith('.class')]
    assert len(names)==len(set(names)),'duplicate JAR entries'
    assert not any('astro/ai/' in n or 'TacticalCommander' in n or n.startswith('blackhole/tests/') for n in names),'deleted content / test instrumentation packaged'
    assert not any('lumen-incinerator' in n or 'inverted-lumen-sorter' in n for n in names if n.endswith('.png')),'new machine sprite found'
    meta=z.read('mod.hjson').decode();assert 'version: "8.4-beta"' in meta
    dex=z.read('classes.dex');assert dex.startswith(b'dex\n'),'missing Android DEX'
    assert b'Lblackhole/AureliaUtilities' in dex and b'Lblackhole/AureliaCampaign;' in dex,'new classes absent from DEX'
    assert b'Lastro/ai/' not in dex and b'TacticalCommander' not in dex,'deleted commander in DEX'
    for name in ['blackhole/AureliaUtilities.class','blackhole/models/UtilityModels.class','blackhole/AureliaCampaign.class']:
        assert name in names,name
    for p in (root/'assets').rglob('*'):
        if p.is_file():assert z.read(p.relative_to(root/'assets').as_posix())==p.read_bytes(),'asset mismatch: '+str(p)
    compiled=list((root/'build/classes').rglob('*.class'))
    if compiled:
        assert len(compiled)==len(classes),'compiled/JAR class count mismatch'
        for p in compiled:assert z.read(p.relative_to(root/'build/classes').as_posix())==p.read_bytes(),'class mismatch: '+str(p)
    for fn in ['bundles/bundle.properties','bundles/bundle_zh_CN.properties']:
        text=z.read(fn).decode();assert 'block.blackhole-astro-commander.' not in text
        for key in ['bar.blackhole-ward-capacity','block.blackhole-lumen-incinerator.name','block.blackhole-inverted-lumen-sorter.name']:assert key in text,key
    report={'jar':jar.name,'bytes':jar.stat().st_size,'sha256':sha(jar.read_bytes()),'desktop_classes':len(classes),'dex_bytes':len(dex),'dex_version':dex[4:7].decode(),'dex_class_definitions':struct.unpack_from('<I',dex,96)[0],'jar_crc':'PASS','class_and_asset_match':'PASS','commander_absence':'PASS','no_new_machine_sprite':'PASS','test_instrumentation_absence':'PASS','source_asset_delta_from_v83':changes}
(root/'docs/v84/release-audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({k:v for k,v in report.items() if k!='source_asset_delta_from_v83'},ensure_ascii=False,indent=2))
print('Changed:',len(changes['modified']),'deleted:',len(changes['deleted']),'added:',len(changes['added']),'unchanged:',changes['unchanged'])
