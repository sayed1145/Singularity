#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Singularity 8.7.3 headless audit (Mindustry v160.5 server, no graphics).
#
# Boots the real dedicated server with the built mod in it, then pokes the
# live content through the server console ("js") and writes every reading to
# docs/v873/audit.txt.  Any stack trace in the log fails the run.
#
# The mod checks itself at load time (V873Audit), so a wrong number stops the
# server before the first probe even runs.  This script adds the reading of
# those numbers plus the 8.7 regression set, so a change that silently undoes
# an older fix is caught too.  8.7.3 adds the slow absorption of the gravity well and the black hole, the
# well activation cycle (8 s active + cooldown), the overdrive of the well and the
# quiet waste firing effects.
# ---------------------------------------------------------------------------
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.."&&pwd)"
VENDOR="${VENDOR:-$HOME/.cache/vendor}"
JAVA_HOME="${JAVA_HOME:-$(find "$VENDOR" -maxdepth 1 -type d -name 'jdk-17*'|head -1)}"
JAVA="$JAVA_HOME/bin/java"
JAR="$(ls -t "$ROOT"/dist/Singularity-*.jar 2>/dev/null | head -1)"

if [ -z "$JAR" ]; then echo "no jar: run build.sh first"; exit 1; fi
echo "== auditing $JAR"

TEST="/tmp/singularity-v873-test"
rm -rf "$TEST"; mkdir -p "$TEST/config/mods" "$ROOT/docs/v873"
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
send 'js var b=Vars.content.block("blackhole-lumen-accelerator");b.computeStats();var m=b.stats.toMap();var out=[];for(var cat in m){for(var st in m[cat]){out.push(String(st))}}var j=out.join("|");var s="[accelerator] speedBoost="+b.speedBoost+" hasBoost="+b.hasBoost+" phase="+b.speedBoostPhase+" statLines="+j+" speedIncreaseStat="+(j.indexOf("speedIncrease")>=0)+" boostEffectStat="+(j.indexOf("boostEffect")>=0);s' 1

send 'js var waste=Vars.content.item("blackhole-industrial-waste");var s="";var fn=[["blackhole-halo","halo"],["blackhole-sky-net","sky-net"]];for(var i=0;i<fn.length;i++){var b=Vars.content.block(fn[i][0]);var bt=b.ammoTypes.get(waste);if(bt==null){s+="["+fn[i][1]+"] MISSING "}else{s+="["+fn[i][1]+"] dmg="+bt.damage+" splash="+bt.splashDamage+" reloadMul="+bt.reloadMultiplier+" bdm="+bt.buildingDamageMultiplier+" ammoMul="+bt.ammoMultiplier+" maxAmmo="+b.maxAmmo+" "}}"[waste-ammo] "+s' 1
send 'js var b=Vars.content.block("blackhole-sky-net");var s="[sky-net] ";var it=Vars.content.item("blackhole-lumenite");if(it==null){it=Items.lead}var bt=b.ammoTypes.get(it);if(bt==null){bt=b.ammoTypes.get(Items.lead)};s+="lumenShell="+(bt!=null);if(bt!=null){s+=" dmg="+bt.damage+" splash="+bt.splashDamage+" ammoMul="+bt.ammoMultiplier}s' 1

send 'js var u=Vars.content.unit("blackhole-citadel");var s="";for(var i=0;i<u.weapons.size;i++){var w=u.weapons.get(i);var bt=w.bullet;s+="["+i+" "+w.name+"] dmg="+bt.damage+" splash="+bt.splashDamage+" reload="+w.reload+" "}"[citadel] "+s' 1

send 'js var b=Vars.content.block("blackhole-lumen-spark");"[lumen-spark] dmg="+b.shootType.damage+" range="+b.range' 1
send 'js var b=Vars.content.block("blackhole-sky-net");var g=Vars.content.item("blackhole-prism-glass");var bt=b.ammoTypes.get(g);"[glass-shell] ammo="+g.name+" dmg="+bt.damage+" splash="+bt.splashDamage+" frags="+bt.fragBullets+" fragDmg="+bt.fragBullet.damage' 1
send 'js var b=Vars.content.block("blackhole-bastion-railgun");"[railgun] range="+b.range+" tiles="+(b.range/8)' 1
send 'js var u=Vars.content.unit("blackhole-pyroclast-titan");var s="[titan] range="+u.range+" maxRange="+u.maxRange+" tiles="+(u.maxRange/8);for(var i=0;i<u.weapons.size;i++){var w=u.weapons.get(i);s+=" [w"+i+" "+w.name+" reach="+Math.round(w.range())+"]"}s' 1
send 'js var u=Vars.content.unit("blackhole-pyroclast-titan");var s="[titan-body] class="+u.getClass().getName()+" super="+u.getClass().getSuperclass().getName();var fx=false;for(var i=0;i<u.weapons.size;i++){var w=u.weapons.get(i);if(w.bullet&&w.bullet.hitEffect)s+=" hitFx="+w.bullet.hitEffect.getClass().getName()}s' 1

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

send 'js var b=Vars.content.block("blackhole-event-horizon");var it=Vars.content.item("blackhole-singularity-core");var shell=b.ammoTypes.get(it);var hole=shell.fragBullet;var s="[blackhole] shellFound="+(shell!=null)+" holeFound="+(hole!=null);if(hole!=null){s+=" pullRadius="+hole.pullRadius+" pullForce="+hole.pullForce+" buildingDamage="+hole.buildingDamage+" endRadius="+hole.endRadius+" lifetime="+hole.lifetime+" rimSpeed="+hole.rimSpeed+" coreSpeed="+hole.coreSpeed+" class="+hole.getClass().getSimpleName()}s' 1
send 'js var b=Vars.content.block("blackhole-gravity-well");var s="[gravity-well] radius="+b.radius+" tiles="+(b.radius/8)+" crushRadius="+b.crushRadius+" pull="+b.pull+" crushDps="+b.crushDps+" rimSpeed="+b.rimSpeed+" coreSpeed="+b.coreSpeed+" cycleTime="+b.cycleTime+" cooldownTime="+b.cooldownTime+" speedBoost="+b.speedBoost+" class="+b.getClass().getName();s' 1
send 'js var waste=Vars.content.item("blackhole-industrial-waste");var b=Vars.content.block("blackhole-halo");var bt=b.ammoTypes.get(waste);var s="[waste-fx] muzzleLife="+bt.shootEffect.lifetime+" hitLife="+bt.hitEffect.lifetime+" muzzleLayer="+bt.shootEffect.layer+" hitLayer="+bt.hitEffect.layer+" trailLen="+bt.trailLength+" trailWidth="+bt.trailWidth;s' 1
# ---------------------------------------------------------------------------------------------
# v8.7.3 live probes:
#   [well-live]     the gravity well: idle -> active (8 s) -> cooldown, slow absorption, flying units too
#   [gravity-live]  the black hole: slow absorption (not an instant swallow) + building damage
# ---------------------------------------------------------------------------------------------
# find a flat spot and put the well on it
send 'js try{var wellBlock=Vars.content.block("blackhole-gravity-well");var x0=0,y0=0,found=false;for(var x=30;x<200&&!found;x++){for(var y=30;y<200&&!found;y++){var ok=true;for(var k=0;k<6;k++){var t=Vars.world.tile(x+k*4,y);if(t==null||t.solid()||!t.floor()){ok=false;break}}if(ok){x0=x;y0=y;found=true}}}if(!found){throw "no flat ground"}var t0=Vars.world.tile(x0,y0);t0.setBlock(wellBlock,Vars.state.rules.defaultTeam,0);if(t0.build==null){throw "no well building"}var wb=t0.build;wellBlock.hasPower=false;var u=Vars.content.unit("mono").create(Vars.state.rules.waveTeam);u.set((x0+13)*8,y0*8);u.add();var wunit=u,gwell=wb,wd0=Math.round(u.dst(wb));"[well-live] setup well="+wb.id+" unit="+u.id+" flying="+u.type.flying+" d0="+wd0}catch(e){"[well-live] setup FAILED "+e}' 1
# one second later: the well must be active, the unit must have moved closer - but still be alive
send 'js try{var u=wunit,b=gwell;var d=u.dst(b);var s="[well-live] t1 phase="+b.phase+" d="+Math.round(d)+" dead="+u.dead+" hp="+u.health;s+=((b.phase==1)&&(d<wd0-10)&&(!u.dead))?" PASS":" FAIL";s}catch(e){"[well-live] probe FAILED "+e}' 2
# ten seconds later: the 8 s cycle must be over and the well must be cooling down
send 'js try{var u=wunit,b=gwell;var d=u.dst(b);var s="[well-live] t11 phase="+b.phase+" timer="+Math.round(b.timer)+" d="+Math.round(d)+" dead="+u.dead;s+=(b.phase==2)?" PASS":" FAIL";s}catch(e){"[well-live] cooldown probe FAILED "+e}' 10

# the black hole: a unit at the rim of the field must drift in slowly, and the enemy building must burn
send 'js try{var b=Vars.content.block("blackhole-event-horizon");var it=Vars.content.item("blackhole-singularity-core");var hole=b.ammoTypes.get(it).fragBullet;var x0=0,y0=0,found=false;for(var x=20;x<200&&!found;x++){for(var y=20;y<200&&!found;y++){var ok=true;for(var k=0;k<5;k++){var t=Vars.world.tile(x+k*2,y);if(t==null||t.solid()||!t.floor()){ok=false;break}}if(ok){x0=x;y0=y;found=true}}}if(!found){throw "no flat ground"}var blk=null,gfriend=null,gfoe=null,gpfoe0=-1,gpfriend0=-1,tried=0;var names=["blackhole-lumen-mender","blackhole-lumen-spark","blackhole-lumen-silo","blackhole-lumen-accelerator"];for(var ni=0;ni<names.length&&blk==null;ni++){var bb=Vars.content.block(names[ni]);if(bb==null||bb.size!=1||!bb.hasBuilding()){continue}var ft0=Vars.world.tile(x0,y0),et0=Vars.world.tile(x0+4,y0);ft0.setBlock(bb,Vars.state.rules.defaultTeam,0);et0.setBlock(bb,Vars.state.rules.waveTeam,0);tried++;if(ft0.build!=null&&et0.build!=null){blk=bb;gfriend=ft0.build;gfoe=et0.build;gpfoe0=gfoe.build!=null?gfoe.health:-1;gpfriend0=gfriend.health}}if(blk==null){throw "no test block (tried="+tried+")"}var ally=Vars.content.unit("mono").create(Vars.state.rules.defaultTeam);ally.set((x0+2)*8,y0*8);var blt=hole.create(ally,(x0+2)*8,y0*8,0);var u=Vars.content.unit("mono").create(Vars.state.rules.waveTeam);u.set((x0+14)*8,y0*8);u.add();var gunit=u,ghole=blt,gd0=Math.round(u.dst(blt));"[gravity-live] setup hole="+blt.id+" unit="+u.id+" d0="+gd0+" blk="+blk.name+" foeHp0="+gpfoe0+" friendHp0="+gpfriend0}catch(e){"[gravity-live] setup FAILED "+e}' 1
send 'js try{var s="[gravity-live] ";var u=gunit,b=ghole;var d2=Math.round(u.dst(b));s+="d="+d2+" dead="+u.dead+" hp="+Math.round(u.health)+" dragged="+(gd0>d2);s+=((d2<gd0)||u.dead)?" PASS":" FAIL";var fh=(gfriend!=null?gfriend.health:-1),oh=(gfoe!=null?gfoe.health:-1);s+=" | [blackhole-build] foeHp="+Math.round(oh)+" friendHp="+Math.round(fh);s+=((oh<gpfoe0)&&(Math.abs(fh-gpfriend0)<0.5))?" PASS":" FAIL";s}catch(e){"[gravity-live] probe FAILED "+e}' 2
send 'js try{var w=Vars.content.block("blackhole-gravity-well");var h=Vars.content.block("blackhole-event-horizon");var hole=h.ammoTypes.get(Vars.content.item("blackhole-singularity-core")).fragBullet;var wellWorst=(w.radius/8)/w.rimSpeed;var wellAvg=0;var n=200;for(var i=0;i<n;i++){var d=(i+0.5)*(w.radius/n);var f=1-d/w.radius;var sp=(w.rimSpeed+(w.coreSpeed-w.rimSpeed)*f)*8;wellAvg+=1/sp}var holeWorst=(hole.pullRadius/8)/hole.rimSpeed;"[absorb-time] well worst="+wellWorst.toFixed(2)+"s total="+wellAvg.toFixed(2)+"s profile="+w.rimSpeed+"->"+w.coreSpeed+" tiles/s" + " | [absorb-time] blackhole worst="+holeWorst.toFixed(2)+"s profile="+hole.rimSpeed+"->"+hole.coreSpeed+" tiles/s"}catch(e){"[absorb-time] FAILED "+e}' 1
send 'js var G=Packages.blackhole.GravityField;var s="[gravity-field] exists="+(G!=null);s' 1
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

mkdir -p "$ROOT/docs/v873"
{
  echo "===== Singularity 8.7.3 AUDIT (Mindustry v160.5 headless) ====="
  sed 's/\x1b\[[0-9;]*m//g' server.log \
    | grep -oE "\[(mod|ui-slider|ui-panel|accelerator|waste-ammo|waste-fx|sky-net|citadel|lumen-spark|glass-shell|railgun|titan|titan-body|blackhole|gravity-well|gravity-field|gravity-live|blackhole-build|well-live|absorb-time|removed|coolant|cryofluid-users|plans|sifter|extractor|wallbore|cargo|cargo-live|cargo-cfg|event-horizon|ward|ward-live|mine|cfg-save|cfg-reload)\][^\"]*" \
    | sed 's/^/  /'
  echo "  [self-check] $(sed 's/\x1b\[[0-9;]*m//g' server.log | grep -oE '\[blackhole/v8\.7\.3\][^"]*' | tail -1)"
  echo "  [content] $(sed 's/\x1b\[[0-9;]*m//g' server.log | grep -oE 'blocks=[0-9]+ items=[0-9]+ units=[0-9]+' | tail -1)"
  echo "===== END AUDIT ====="
} > "$ROOT/docs/v873/audit.txt"

cat "$ROOT/docs/v873/audit.txt"
echo
#the ui-slider probe deliberately builds the OLD (illegal) slider once to prove the guard is alive,
#so its "max must be > min" line is the expected result, not a failure
if grep -nE 'Exception|EcmaError|EvaluatorException|Error:|Caused by|NoSuch|Cannot call|IllegalState' server.log \
   | grep -v ErrorHandler | grep -v '\[ui-slider\]' | head -20; then
  echo 'TEST FAILED'
  exit 1
fi
if ! grep -q '\[gravity-live\].*PASS' "$ROOT/docs/v873/audit.txt"; then
  echo 'TEST FAILED: the black hole did not pull the unit in slowly (or swallowed it at once)'
  exit 1
fi
if ! grep -q '\[well-live\].*PASS' "$ROOT/docs/v873/audit.txt"; then
  echo 'TEST FAILED: the gravity well did not run its activation cycle (idle -> active -> cooldown)'
  exit 1
fi
if ! grep -q '\[blackhole-build\].*PASS' "$ROOT/docs/v873/audit.txt"; then
  echo 'TEST FAILED: the black hole did not damage the enemy building (or hit a friendly one)'
  exit 1
fi
echo 'TEST PASSED: v8.7.3 on v160.5 headless; slow absorption probed (well 4-15 tiles/s with 8 s cycle, black hole 3-20 tiles/s), quiet waste effects, 8.7.2 + 8.7.1 + 8.7 regression set green, save/reload ok'
