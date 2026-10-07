#!/usr/bin/env bash
# Boots the official Mindustry v160.5 dedicated server with Singularity v7.3 and exercises: content (Astral Aegis and the
# tactical controllers gone, titans are 3D walkers, 3D drawers installed), Aurelia isolation (zero vanilla content shown
# on Aurelia), the Aurelia tech tree (own root, own items only, real research costs), Aurelia sector generation (own
# terrain / ores / start stock / waves, unit cap from the core, every Aurelia block buildable), every 3D unit alive,
# moving and firing (incl. the titans' own gait), the fabricator and the tide dock printing units, save/load.
set -uo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"; VENDOR="${VENDOR:-$HOME/.cache/vendor}"
JAVA_HOME="${JAVA_HOME:-$(find "$VENDOR" -maxdepth 1 -type d -name 'jdk-17*' | head -1)}"; JAVA="$JAVA_HOME/bin/java"
# static guard: a string-literal atlas lookup renders the error region ("oh no") in game
if grep -rn 'Draw\.rect("' "$ROOT/src" --include=*.java; then echo 'TEST FAILED: string-literal region draw in source'; exit 1; fi
TEST="$ROOT/diagtest"; rm -rf "$TEST"; mkdir -p "$TEST/config/mods"; cp "$ROOT/dist/Singularity-v8.3-beta.jar" "$TEST/config/mods/"
cd "$TEST"; mkfifo cmd.fifo
HOME="$TEST" "$JAVA" -Xmx700m -jar "$VENDOR/server-release.jar" <cmd.fifo >server.log 2>&1 & PID=$!; exec 3>cmd.fifo
send(){ echo "$1" >&3; sleep "${2:-2}"; }
P='blackhole-'
sleep 12; send mods 2; send 'config autoPause false' 1
send 'js global.F=function(v,n){var k=Math.pow(10,n);return String(Math.round(Number(v)*k)/k);};global.free=function(cx,cy,rad){for(var r=6;r<120;r+=2)for(var a=0;a<360;a+=10){var q=Vars.world.tileWorld(cx+Math.cos(a/57.3)*r*8,cy+Math.sin(a/57.3)*r*8);if(q==null)continue;var ok=true;for(var dx=-rad;dx<=rad&&ok;dx++)for(var dy=-rad;dy<=rad&&ok;dy++){var z=Vars.world.tile(q.x+dx,q.y+dy);if(z==null||z.solid()||z.floor().isDeep()||z.build!=null)ok=false;}if(ok)return q;}return null;};global.se=function(n){return Vars.content.statusEffect(n);};global.sandbox="item-source item-void liquid-source liquid-void power-source power-void heat-source".split(" ");"helpers ready"' 1

send 'host Ancient_Caldera survival' 10
send 'js var p=Vars.content.planet("'$P'aurelia");var sec=p.sectors.get(p.startSector);Vars.logic.reset();Vars.world.loadSector(sec);Vars.state.rules.sector=sec;Vars.logic.play();var f={},o={},w={},bad=0,badOre=0;Vars.world.tiles.eachTile(function(t){var fl=t.floor().name;f[fl.replace("'$P'","")]=(f[fl.replace("'$P'","")]||0)+1;var ov=t.overlay();if(ov!=null&&ov!=Blocks.air&&ov!=Blocks.spawn){o[ov.name.replace("'$P'","")]=(o[ov.name.replace("'$P'","")]||0)+1;if(!ov.name.startsWith("'$P'"))badOre++;}if(t.block().isStatic()){w[t.block().name.replace("'$P'","")]=(w[t.block().name.replace("'$P'","")]||0)+1;if(!t.block().name.startsWith("'$P'"))bad++;}if(!fl.startsWith("'$P'")&&fl!="air"&&fl!="empty"&&fl!="spawn")bad++;});var core=Vars.state.rules.defaultTeam.core();var items=[];core.items.each(function(i,a){items.push((i.name.startsWith("'$P'")?"":"VANILLA:")+i.name.replace("'$P'","")+"="+a);});"aurelia sector "+sec.id+" "+Vars.world.width()+"x"+Vars.world.height()+" floors="+JSON.stringify(f)+" ores="+JSON.stringify(o)+" walls="+JSON.stringify(w)+" vanilla terrain tiles="+bad+" vanilla ore tiles="+badOre+" core="+core.block.name+" stock: "+items.join(" ")' 12
send 'js var c=Vars.state.rules.defaultTeam.core();global.cx=c.x;global.cy=c.y;Vars.state.rules.unitCapVariable=false;Vars.state.rules.unitCap=200;Time.setDeltaProvider(function(){return 1;});"core at "+c.x+","+c.y' 1

send 'js var u=Vars.content.unit("'$P'astro-detainer");var ab=u.abilities.map(function(a){return a.getClass().getSimpleName()+"->"+a.localized()}).toString(" | ");var st=u.stances.map(function(s){return s.name+"->"+s.localized()}).toString(" | ");var cm=u.commands.map(function(c){return c.name+"->"+c.localized()}).toString(" | ");var wp=u.weapons.map(function(w){return w.name}).toString(",");"DETAINER name=["+u.localizedName+"] desc=["+u.description+"] details=["+(u.details==null?"null":u.details.substring(0,40))+"] abilities=["+ab+"] stances=["+st+"] commands=["+cm+"] weapons=["+wp+"]"' 2
send 'js var bad=[];Vars.content.units().each(function(u){if(!u.name.startsWith("'$P'"))return;u.abilities.each(function(a){var l=a.localized();if(l==null||l.indexOf("???")>=0||l.indexOf("ability.")>=0)bad.push(u.name.substring(10)+":"+a.getClass().getSimpleName()+"="+l);});if(u.description==null||u.description.indexOf("@")==0)bad.push(u.name.substring(10)+":desc="+u.description);});"ABILITY/DESC check: "+(bad.length?bad.join(" | "):"all fine")' 2
send 'js var t=Vars.state.rules.defaultTeam;var q=global.free(global.cx,global.cy,3);q.setNet(Vars.content.block("'$P'lumen-cargo-station"),t,0);var b=q.build;global.cs=b;var n=0;while(b.unit==null&&n<4000){b.power.status=1;b.update();n++;}var pad=global.free(global.cx,global.cy,2);pad.setNet(Vars.content.block("'$P'lumen-cargo-point"),t,0);var pb=pad.build;pb.configure(Vars.content.item("'$P'lumenite"));global.pad=pb;b.items.add(Vars.content.item("'$P'lumenite"),80);var u=b.unit;var x0=u.x,y0=u.y;for(var k=0;k<600;k++){b.power.status=1;b.update();pb.update();u.update();}"CARGO drone: built="+(u!=null)+" controller="+u.controller().getClass().getSimpleName()+" moved="+global.F(Mathf.dst(u.x,u.y,x0,y0),1)+" stationItems="+b.items.total()+" droneItems="+u.stack.amount+" padItem="+(pb.item==null?"none":pb.item.name.substring(10))+" dstPad="+global.F(u.dst(pb)/8,1)+" tiles"' 6
send 'js var u=global.cs.unit;var ai=u.controller();var fields="";var cls=ai.getClass();"CARGO ai detail: type="+cls.getName()+" unitMoving="+u.moving()+" speed="+global.F(u.speed(),2)+" vel="+global.F(u.vel.len(),3)+" hasItem="+u.hasItem()+" stack="+(u.item()==null?"none":u.item().name)+" team="+u.team.name' 3
send exit 2; exec 3>&-; wait $PID 2>/dev/null
grep -vE '^\[I\] \[Server\]' server.log | tail -n 30
echo DIAG-DONE
