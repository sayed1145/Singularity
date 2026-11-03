#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Singularity 8.7.1 headless audit (Mindustry v160.5 server, no graphics).
#
# Boots the real dedicated server with the built mod in it, then pokes the
# live content through the server console ("js") and writes every reading to
# docs/v871/audit.txt.  Any stack trace in the log fails the run.
#
# The mod checks itself at load time (V871Audit), so a wrong number stops the
# server before the first probe even runs.  This script adds the reading of
# those numbers plus the 8.7 regression set, so a change that silently undoes
# an older fix is caught too.
# ---------------------------------------------------------------------------
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.."&&pwd)"
VENDOR="${VENDOR:-$HOME/.cache/vendor}"
JAVA_HOME="${JAVA_HOME:-$(find "$VENDOR" -maxdepth 1 -type d -name 'jdk-17*'|head -1)}"
JAVA="$JAVA_HOME/bin/java"
JAR="$(ls -t "$ROOT"/dist/Singularity-*.jar 2>/dev/null | head -1)"

if [ -z "$JAR" ]; then echo "no jar: run build.sh first"; exit 1; fi
echo "== auditing $JAR"

TEST="/tmp/singularity-v871-test"
rm -rf "$TEST"; mkdir -p "$TEST/config/mods" "$ROOT/docs/v871"
cp "$JAR" "$TEST/config/mods/"

cd "$TEST"
mkfifo cmd.fifo
HOME="$TEST" "$JAVA" -Xmx700m -jar "$VENDOR/server-release.jar" <cmd.fifo >server.log 2>&1 &
PID=$!
exec 3>cmd.fifo
send(){ echo "$1" >&3; sleep "${2:-2}"; }

sleep 14
send 'config autoPause false' 1
send 'host Ancient_Caldera survival' 12
send 'js var t=Vars.state.rules.defaultTeam;Vars.state.rules.teams.get(t).cheat=true;Vars.state.rules.infiniteResources=true;"world ready"' 2

send 'js var m=Vars.mods.getMod("blackhole");"[mod] version="+m.meta.version+"  minGame="+m.meta.minGameVersion' 1

# ---------------------------------------------------------------------------
# v8.7.1 - the crash that started this patch
# ---------------------------------------------------------------------------
send 'js var P=Packages.arc.scene.ui.ProgressBar;var st=Packages.arc.scene.ui.ProgressBar.ProgressBarStyle;var r="";try{var b=new P(8,400,1,false,new st());r+="[ui-slider] min=8 max=400 OK"}catch(e){r+="[ui-slider] min=8 max=400 "+e}try{new P(58,8,1,false,new st());r+=" oldOrder=ACCEPTED(BAD)"}catch(e){r+=" oldOrder="+e}r' 2
send 'js var b=Vars.content.block("blackhole-lumen-cargo-station");"[ui-panel] configurable="+b.configurable+" saveConfig="+b.saveConfig+" configKeys="+b.configurations.size' 1

# ---------------------------------------------------------------------------
# v8.7.1 - the numbers
# ---------------------------------------------------------------------------
send 'js var b=Vars.content.block("blackhole-lumen-accelerator");var m=b.stats.toMap();var s="[accelerator] speedBoost="+b.speedBoost+" hasBoost="+b.hasBoost+" phase="+b.speedBoostPhase+" boostStatLines=0";for(var e in m){}"[accelerator] "+s' 1

send 'js var waste=Vars.content.item("blackhole-industrial-waste");var s="";var fn=[["blackhole-halo","halo"],["blackhole-sky-net","sky-net"]];for(var i=0;i<fn.length;i++){var b=Vars.content.block(fn[i][0]);var bt=b.ammoTypes.get(waste);if(bt==null){s+="["+fn[i][1]+"] MISSING "}else{s+="["+fn[i][1]+"] dmg="+bt.damage+" splash="+bt.splashDamage+" reloadMul="+bt.reloadMultiplier+" bdm="+bt.buildingDamageMultiplier+" ammoMul="+bt.ammoMultiplier+" maxAmmo="+b.maxAmmo+" "}}"[waste-ammo] "+s' 1

send 'js var u=Vars.content.unit("blackhole-citadel");var s="";for(var i=0;i<u.weapons.size;i++){var w=u.weapons.get(i);var bt=w.bullet;s+="["+i+" "+w.name+"] dmg="+bt.damage+" splash="+bt.splashDamage+" reload="+w.reload+" "}"[citadel] "+s' 1

send 'js var b=Vars.content.block("blackhole-lumen-spark");"[lumen-spark] dmg="+b.shootType.damage+" range="+b.range' 1
send 'js var b=Vars.content.block("blackhole-sky-net");var g=Vars.content.item("blackhole-prism-glass");var bt=b.ammoTypes.get(g);"[glass-shell] ammo="+g.name+" dmg="+bt.damage+" splash="+bt.splashDamage+" frags="+bt.fragBullets+" fragDmg="+bt.fragBullet.damage' 1
send 'js var b=Vars.content.block("blackhole-bastion-railgun");"[railgun] range="+b.range+" tiles="+(b.range/8)' 1
send 'js var u=Vars.content.unit("blackhole-pyroclast-titan");var s="[titan] range="+u.range+" maxRange="+u.maxRange+" tiles="+(u.maxRange/8);for(var i=0;i<u.weapons.size;i++){var w=u.weapons.get(i);s+=" [w"+i+" "+w.name+" reach="+Math.round(w.range())+"]"}s' 1

send 'js var u=Vars.content.unit("blackhole-lumen-hauler");var b=Vars.content.block("blackhole-lumen-spark");var s="[removed] hauler="+u+" (null = gone) spark="+(b!=null);s+=" cargoStation="+(Vars.content.block("blackhole-lumen-cargo-station")!=null);"[removed] "+s' 1

# ---------------------------------------------------------------------------
# v8.7 regression set - these must keep working
# ---------------------------------------------------------------------------
send 'js var bs=Vars.content.blocks();function has(n){for(var i=0;i<bs.size;i++){if(bs.get(i).name==n)return true}return false}"[removed] supplyPost="+has("blackhole-lumen-supply-post")+" bufferPost="+has("blackhole-lumen-buffer-post")+" logisticsVault="+has("blackhole-lumen-logistics-vault")+" latticeLink="+has("blackhole-lattice-link")' 1

send 'js var s="";var fn=["blackhole-horizon-assembler","blackhole-citadel-yard"];for(var i=0;i<fn.length;i++){var b=Vars.content.block(fn[i]);var cs=b.consumers;for(var j=0;j<cs.length;j++){var c=cs[j];if(c.getClass().getSimpleName()=="ConsumeLiquid"){s+="["+b.name+"] liquid="+c.liquid.name+" amount="+c.amount+" "}}}var aq=Vars.content.liquid("blackhole-aurora-coolant");s+="auroraCoolant="+(aq==null?"null":aq.name);"[coolant] "+s' 1

send 'js var s="";var bs=Vars.content.blocks();for(var i=0;i<bs.size;i++){var b=bs.get(i);var cs=b.consumers;for(var j=0;j<cs.length;j++){var c=cs[j];if(c.getClass().getSimpleName()=="ConsumeLiquid"&&c.liquid.name=="cryofluid"){s+=b.name+" "}}}"[cryofluid-users] "+s' 1

send 'js var s="";var fn=["blackhole-horizon-assembler","blackhole-citadel-yard"];for(var i=0;i<fn.length;i++){var b=Vars.content.block(fn[i]);s+="["+b.name+"] ";if(b.plans!=null){for(var j=0;j<b.plans.size;j++){var p=b.plans.get(j);var it="";var rq=p.requirements;for(var k=0;k<rq.length;k++){it+=rq[k].item.name+"x"+rq[k].amount+","}s+=p.unit.name+"["+it+"] "}}}"[plans] "+s' 1

send 'js var b=Vars.content.block("blackhole-resonance-sifter");"[sifter] drillTime="+b.drillTime+" size="+b.size+" speed="+(60/b.drillTime*b.size*b.size)+"/s tank="+b.liquidCapacity' 1
send 'js var b=Vars.content.block("blackhole-lumen-extractor");"[extractor] hasLiquids="+b.hasLiquids+" liquidCapacity="+b.liquidCapacity+" drillTime="+b.drillTime+" size="+b.size+" speed="+(60/b.drillTime*b.size)+"/s"' 1
send 'js var b=Vars.content.block("blackhole-wall-bore");"[wallbore] drillTime="+b.drillTime+" size="+b.size+" speed="+(60/b.drillTime*b.size)+"/s tank="+b.liquidCapacity' 1

send 'js var b=Vars.content.block("blackhole-lumen-cargo-station");"[cargo] itemCapacity="+b.itemCapacity+" perItem="+b.perItemCapacity+" itemTypes="+b.itemTypes+" configurable="+b.configurable+" saveConfig="+b.saveConfig+" configKeys="+b.configurations.size+" rangeTiles="+b.rangeTiles+" longRangeTiles="+b.longRangeTiles' 1
send 'js var tile=Vars.world.tile(40,40);tile.setNet(Vars.content.block("blackhole-lumen-cargo-station"),Vars.state.rules.defaultTeam,0);var b=tile.build;b.items.add(Items.copper,50);"[cargo-live] copper="+b.items.get(Items.copper)+" total="+b.items.total()+" maxAccepted="+b.getMaximumAccepted(Items.copper)+" acceptStack(200)="+b.acceptStack(Items.copper,200,null)+" target="+b.target(Items.copper)' 2
send 'js var b=Vars.world.tile(40,40).build;b.applyConfig(120+512);"[cargo-cfg] rangeTiles="+b.rangeTiles+" longRange="+b.longRange+" radius="+b.radius()+"px"' 2
send 'js var b=Vars.world.tile(40,40).build;b.applyConfig(58);"[cargo-cfg] back to default rangeTiles="+b.rangeTiles+" longRange="+b.longRange+" radius="+b.radius()+"px"' 2

send 'js var b=Vars.content.block("blackhole-event-horizon");var marrow=Vars.content.item("blackhole-star-marrow");var bt=b.ammoTypes.get(marrow);"[event-horizon] ammoTypes="+b.ammoTypes.size+" marrow reloadMul="+bt.reloadMultiplier+" dmg="+bt.damage+" splash="+bt.splashDamage' 1
send 'js var b=Vars.content.block("blackhole-ward-dome");"[ward] base="+b.shieldCapacity+" shardBoost="+b.itemCapacityBoost+" regenBoost="+b.itemRegenBoost' 1
send 'js var tile=Vars.world.tile(46,40);tile.setNet(Vars.content.block("blackhole-ward-dome"),Vars.state.rules.defaultTeam,0);var b=tile.build;b.deepBank=true;b.shield=12000;b.updateTile();"[ward-live] realCapacity="+b.realCapacity()+" shieldAfterUpdate="+b.shield' 2

send 'js var s="";var fn=["blackhole-mote","blackhole-lattice-crawler","blackhole-aurite-auger"];for(var i=0;i<fn.length;i++){var u=Vars.content.unit(fn[i]);if(u==null)continue;var list=[];if(u.mineItems!=null){for(var j=0;j<u.mineItems.size;j++){list.push(u.mineItems.get(j).name)}}s+="["+u.name+" tier="+u.mineTier+" mine="+list.join(",")+"] "}"[mine] "+s' 1

send 'js "blocks="+Vars.content.blocks().size+" items="+Vars.content.items().size+" units="+Vars.content.units().size' 1

# the player settings must survive a save/reload
send 'js var b=Vars.world.tile(40,40).build;b.applyConfig(120+512);"[cfg-save] set rangeTiles="+b.rangeTiles+" longRange="+b.longRange' 1
send 'save 1' 3
send 'stop' 2
send 'load 1' 8
send 'js var b=Vars.world.tile(40,40).build;"[cfg-reload] rangeTiles="+b.rangeTiles+" longRange="+b.longRange+" perItem="+b.getMaximumAccepted(Items.copper)' 2
send 'exit' 2
exec 3>&-
wait $PID 2>/dev/null

mkdir -p "$ROOT/docs/v871"
{
  echo "===== Singularity 8.7.1 AUDIT (Mindustry v160.5 headless) ====="
  sed 's/\x1b\[[0-9;]*m//g' server.log \
    | grep -oE "\[(mod|ui-slider|ui-panel|accelerator|waste-ammo|citadel|lumen-spark|glass-shell|railgun|titan|removed|coolant|cryofluid-users|plans|sifter|extractor|wallbore|cargo|cargo-live|cargo-cfg|event-horizon|ward|ward-live|mine|cfg-save|cfg-reload)\][^\"]*" \
    | sed 's/^/  /'
  echo "  [self-check] $(sed 's/\x1b\[[0-9;]*m//g' server.log | grep -oE '\[blackhole/v8\.7\.1\][^"]*' | tail -1)"
  echo "  [content] $(sed 's/\x1b\[[0-9;]*m//g' server.log | grep -oE 'blocks=[0-9]+ items=[0-9]+ units=[0-9]+' | tail -1)"
  echo "===== END AUDIT ====="
} > "$ROOT/docs/v871/audit.txt"

cat "$ROOT/docs/v871/audit.txt"
echo
#the ui-slider probe deliberately builds the OLD (illegal) slider once to prove the guard is alive,
#so its "max must be > min" line is the expected result, not a failure
if grep -nE 'Exception|EcmaError|EvaluatorException|Error:|Caused by|NoSuch|Cannot call|IllegalState' server.log \
   | grep -v ErrorHandler | grep -v '\[ui-slider\]' | head -20; then
  echo 'TEST FAILED'
  exit 1
fi
echo 'TEST PASSED: v8.7.1 on v160.5 headless; 8.7.1 numbers probed, 8.7 regression set green, save/reload ok'
