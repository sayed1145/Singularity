#!/usr/bin/env bash
# v8.1 terrain probe: generates Aurelia sectors with the new natural pipeline, prints the validator numbers for
# every sector and dumps an ASCII picture of a few of them so the shapes can be inspected.
set -uo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"; VENDOR="${VENDOR:-$HOME/.cache/vendor}"
JAVA_HOME="${JAVA_HOME:-$(find "$VENDOR" -maxdepth 1 -type d -name 'jdk-17*' | head -1)}"; JAVA="$JAVA_HOME/bin/java"
TEST="$ROOT/maptestrunq"; rm -rf "$TEST"; mkdir -p "$TEST/config/mods"; cp "$ROOT"/dist/Singularity-*.jar "$TEST/config/mods/"
cd "$TEST"; mkfifo cmd.fifo
HOME="$TEST" "$JAVA" -Xmx700m -jar "$VENDOR/server-release.jar" <cmd.fifo >server.log 2>&1 & PID=$!; exec 3>cmd.fifo
send(){ echo "$1" >&3; sleep "${2:-2}"; }
P='blackhole-'
sleep 12; send mods 2; send 'config autoPause false' 1

# per-sector generation sweep with the full statistics
send 'js global.gen=function(id){var p=Vars.content.planet("'$P'aurelia");var sec=p.sectors.get(id);var t0=Time.millis();try{Vars.logic.reset();Vars.world.loadSector(sec);Vars.state.rules.sector=sec;Vars.logic.play();}catch(e){return "sector "+id+" FAILED "+e;}var ms=Time.millis()-t0;var w=Vars.world.width(),h=Vars.world.height();var wall=0,water=0,deep=0,ore={},fl={},spawn=0,floorKinds={};Vars.world.tiles.eachTile(function(q){if(q.block().isStatic())wall++;if(q.floor().isLiquid){water++;if(q.floor().isDeep())deep++;}var f=q.floor().name.replace("'$P'","");floorKinds[f]=(floorKinds[f]||0)+1;var o=q.overlay();if(o!=null&&o!=Blocks.air){if(o==Blocks.spawn)spawn++;else ore[o.name.replace("'$P'","")]=(ore[o.name.replace("'$P'","")]||0)+1;}});var c=Vars.state.rules.defaultTeam.core();return "sector "+id+" "+w+"x"+h+" ms="+ms+" wall="+wall+"("+Math.round(wall*100/(w*h))+"%) water="+water+" deep="+deep+" spawns="+spawn+" kinds="+Object.keys(floorKinds).length+" ore="+JSON.stringify(ore)+" core="+(c==null?"NONE":c.tile.x+","+c.tile.y);};"gen ready"' 2
send 'js global.dump=function(id,name){var s="";var w=Vars.world.width(),h=Vars.world.height();for(var y=h-1;y>=0;y--){var row="";for(var x=0;x<w;x++){var q=Vars.world.tile(x,y);var ch=".";if(q.block().isStatic())ch="#";else if(q.block()!=Blocks.air)ch="B";else if(q.floor().isDeep())ch="D";else if(q.floor().isLiquid)ch="~";else if(q.overlay()==Blocks.spawn)ch="X";else if(q.overlay()!=Blocks.air)ch="o";else{var f=q.floor().name;ch=f.indexOf("sand")>=0?",":f.indexOf("resonance")>=0?"r":f.indexOf("ember")>=0?"e":f.indexOf("frost")>=0?"f":f.indexOf("glass")>=0?"g":f.indexOf("basalt")>=0?"b":f.indexOf("gravel")>=0?"v":f.indexOf("moss")>=0?"m":f.indexOf("plasma")>=0?"p":".";}row+=ch;}s+=row+"\n";}Vars.dataDirectory.child(name).writeString(s);return "dumped "+name+" "+w+"x"+h;};"dump ready"' 2

for S in 0 9 20; do
  send "js var p=Vars.content.planet(\"${P}aurelia\");var ids=[];p.sectors.each(function(s){if(Vars.content.planet(\"${P}aurelia\").generator.preset(s.id)!=null)ids.push(s.id);});ids.length>$S?global.gen(ids[$S]):\"no sector $S\"" 7
done
send 'js global.dump(0,"map-a.txt")' 4
send 'js var p=Vars.content.planet("'$P'aurelia");var ids=[];p.sectors.each(function(s){if(p.generator.preset(s.id)!=null)ids.push(s.id);});global.gen(ids[9])' 8
send 'js global.dump(0,"map-b.txt")' 4
send 'js var p=Vars.content.planet("'$P'aurelia");var ids=[];p.sectors.each(function(s){if(p.generator.preset(s.id)!=null)ids.push(s.id);});global.gen(ids[20])' 8
send 'js global.dump(0,"map-c.txt")' 4

send exit 3; exec 3>&-; wait $PID 2>/dev/null
cp "$TEST"/config/map-*.txt "$ROOT/" 2>/dev/null || cp "$TEST"/map-*.txt "$ROOT/" 2>/dev/null
grep -E "sector [0-9]+|dumped|Exception|FAILED" "$TEST/server.log" | sed 's/\x1b\[[0-9;]*m//g' | tail -60
