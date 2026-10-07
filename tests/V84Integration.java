package blackhole.tests;

import arc.Core;
import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import blackhole.*;
import blackhole.AureliaUtilities.*;
import blackhole.CycleParts.WardDome;
import blackhole.CycleParts.WardDome.WardDomeBuild;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.SaveIO;
import mindustry.logic.LAccess;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.OreBlock;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.world.consumers.ConsumeLiquid;
import java.io.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;

/** Official-server integration tests. Injected only into the temporary test jar, never the released mod. */
public final class V84Integration{
    static int assertions,testX,testY;
    static void check(boolean b,String m){assertions++;if(!b)throw new AssertionError(m);}
    static void near(double a,double b,String m){check(Math.abs(a-b)<.02,m+": "+a+" != "+b);}
    static void log(String s){Log.info("[V84-PASS] @",s);}
    static Block block(String n){return Vars.content.block("blackhole-"+n);}
    static void tick(Building b,int count){for(int i=0;i<count;i++){Time.delta=1f;if(b.power!=null)b.power.status=1f;b.update();}}
    static Building put(Block b,int x,int y){Tile t=Vars.world.tile(x,y);t.setBlock(b,Team.sharded,0);return t.build;}
    static void flat(int x,int y,int radius){for(int dx=-radius;dx<=radius;dx++)for(int dy=-radius;dy<=radius;dy++){Tile t=Vars.world.tile(x+dx,y+dy);if(t!=null){t.setBlock(Blocks.air);t.setFloor(AureliaContent.auroraPlate);t.setOverlay(Blocks.air);}}}
    static byte[] write(Building b)throws Exception{var out=new ByteArrayOutputStream();b.write(new Writes(new DataOutputStream(out)));return out.toByteArray();}
    static void read(Building b,byte[] bytes,byte version)throws Exception{b.read(new Reads(new DataInputStream(new ByteArrayInputStream(bytes))),version);}
    static void load(int id){Vars.logic.reset();Vars.world.loadSector(AureliaContent.aurelia.sectors.get(id));Vars.state.rules.sector=AureliaContent.aurelia.sectors.get(id);Vars.logic.play();Time.delta=1f;}
    static boolean pass(Tile t,CoreBuild core){return t!=null&&!t.floor().isDeep()&&(!t.solid()||t.build==core);}
    static boolean[] reach(CoreBuild core,int clearance){
        int w=Vars.world.width(),h=Vars.world.height();boolean[] allowed=new boolean[w*h],seen=new boolean[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            boolean ok=true;for(int dx=-clearance;dx<=clearance&&ok;dx++)for(int dy=-clearance;dy<=clearance&&ok;dy++)ok=pass(Vars.world.tile(x+dx,y+dy),core);
            allowed[x+y*w]=ok;
        }
        int[] queue=new int[w*h];int n=0,c=0;int start=core.tile.x+core.tile.y*w;queue[n++]=start;seen[start]=true;
        while(c<n){int k=queue[c++],x=k%w,y=k/w;for(int d=0;d<4;d++){int nx=x+arc.math.geom.Geometry.d4x[d],ny=y+arc.math.geom.Geometry.d4y[d];if(nx<0||nx>=w||ny<0||ny>=h)continue;int next=nx+ny*w;if(allowed[next]&&!seen[next]){seen[next]=true;queue[n++]=next;}}}
        return seen;
    }
    static void content()throws Exception{
        for(String n:new String[]{"voxel-forge","water-treatment","ward-dome","lumen-incinerator","inverted-lumen-sorter"})check(block(n)!=null,"missing "+n);
        ConsumeLiquid forge=block("voxel-forge").findConsumer(c->c instanceof ConsumeLiquid);
        ConsumeLiquid water=block("water-treatment").findConsumer(c->c instanceof ConsumeLiquid);
        check(forge.liquid==AureliaFrontier.auroraCoolant,"forge coolant");near(forge.amount,.08,"forge rate");
        check(water.liquid==AureliaContent.tidewater,"water input");near(water.amount,.36,"water rate");
        check(block("astro-commander")==null,"deleted commander registered");
        for(String c:new String[]{"astro.content.TacticalCommander","astro.ai.TacticalBrain","astro.ai.CommanderAI"}){
            try{V84Integration.class.getClassLoader().loadClass(c);throw new AssertionError("deleted class exists: "+c);}catch(ClassNotFoundException expected){}
        }
        check(AureliaUtilities.incinerator instanceof mindustry.world.blocks.production.Incinerator,"official incinerator API");
        check(AureliaUtilities.invertedSorter instanceof mindustry.world.blocks.distribution.Sorter,"official sorter API");
        for(Block b:new Block[]{AureliaUtilities.incinerator,AureliaUtilities.invertedSorter}){
            check(b.techNode!=null,"no tech node "+b.name);check(b.update&&b.drawDynamic&&!b.drawCached,"renderer flags "+b.name);
            check(b.localizedName!=null&&!b.localizedName.equals(b.name),"name missing "+b.name);
            for(ItemStack s:b.requirements)check(s.item.name.startsWith("blackhole-"),"vanilla price "+b.name);
            check(b.isOnPlanet(AureliaContent.aurelia),"not buildable on Aurelia");
        }
        log("recipes, official utility inheritance, tech/research costs, localization and commander deletion");
    }
    static void campaign(Path report)throws Exception{
        StringBuilder csv=new StringBuilder("sector,width,height,gates,win_wave,boss_encounters,boss_units,peak_regular,budget,min_gate_distance,max_gate_distance,ore_Q1,ore_Q2,ore_Q3,ore_Q4,water_tiles,wide_routes,signature\n");
        Set<Long> signatures=new HashSet<>();int nonstart=0,multiple=0;
        for(var p:AureliaPlanetGenerator.campaign){
            load(p.id);Rules rules=Vars.state.rules;CoreBuild core=rules.defaultTeam.core();check(core!=null,"core "+p.id);
            IntSeq gates=AureliaCampaign.gates();check(gates.size==p.spawns,"gates "+p.id);
            boolean[] wide=reach(core,1);int wideCount=0;float min=Float.MAX_VALUE,max=0;
            for(int i=0;i<gates.size;i++){
                Tile t=Vars.world.tile(gates.get(i));float d=Mathf.dst(t.x,t.y,core.tile.x,core.tile.y);min=Math.min(min,d);max=Math.max(max,d);
                check(wide[t.x+t.y*Vars.world.width()],"no 3-tile-wide ground route at sector "+p.id+" gate "+i);wideCount++;
                for(int dx=-3;dx<=3;dx++)for(int dy=-3;dy<=3;dy++){Tile q=Vars.world.tile(t.x+dx,t.y+dy);check(q!=null&&!q.solid()&&!q.floor().isLiquid,"blocked/wet boss staging "+p.id);}
            }
            check(min>=60&&max-min<62,"unfair gate distance "+p.id);
            int[] ores=new int[4];int water=0;long sig=1469598103934665603L;
            for(Tile t:Vars.world.tiles){
                if(t.floor().isLiquid)water++;
                if(t.overlay() instanceof OreBlock o&&!o.wallOre)ores[(t.x<core.tile.x?0:1)+(t.y<core.tile.y?0:2)]++;
                sig=(sig^(t.floor().id*31L+t.block().id*7L+t.overlay().id))*1099511628211L;
            }
            check(water>=420,"water supply "+p.id);int omin=Arrays.stream(ores).min().getAsInt(),omax=Arrays.stream(ores).max().getAsInt();check(omin>=omax*.45,"ore imbalance "+p.id);
            check(signatures.add(sig),"duplicate map "+p.id);
            int boss=0,units=0;boolean finalBoss=false;
            for(SpawnGroup g:rules.spawns){
                check(g.type.name.startsWith("blackhole-"),"vanilla wave unit");
                if(g.effect==StatusEffects.boss){
                    boss++;units+=g.unitAmount;check(g.begin==g.end,"repeating boss");check(g.spawn!=-1&&gates.contains(g.spawn),"boss multiplied across gates");
                    check(g.begin>=0&&g.begin<rules.winWave,"boss outside capture window");
                    check(g.getSpawned(g.begin)==g.unitAmount&&g.getSpawned(g.begin+1)==0&&g.getSpawned(g.begin-1)==0,"boss count");
                    Unit u=g.createUnit(rules.waveTeam,g.begin);check(u.hasEffect(StatusEffects.boss),"missing actual boss status");u.remove();
                    finalBoss|=g.begin==rules.winWave-2;
                }
            }
            check(boss==AureliaCampaign.encounterCount(p),"boss count mismatch "+p.id);
            if(p.id==AureliaContent.aurelia.startSector)check(boss==0,"initial sector boss");else{nonstart++;check(boss>=1&&finalBoss,"no final boss "+p.id);if(boss>1)multiple++;}
            int peak=0;for(int w=0;w<rules.winWave;w++){int n=0;for(SpawnGroup g:rules.spawns)if(g.effect!=StatusEffects.boss)n+=g.getSpawned(w)*(g.spawn==-1?gates.size:1);peak=Math.max(peak,n);}
            check(peak<=AureliaCampaign.regularBudget(p),"regular budget exceeded "+p.id+": "+peak);
            if(boss>0){
                //Exercise the engine, not merely the schedule: the sector must not capture before its boss.
                Vars.state.wave=rules.winWave-1;Vars.state.enemies=0;
                Method capture=Vars.logic.getClass().getDeclaredMethod("checkGameState");capture.setAccessible(true);capture.invoke(Vars.logic);
                check(rules.waves,"captured before final boss at sector "+p.id);
                int expected=0;for(SpawnGroup g:rules.spawns)if(g.effect==StatusEffects.boss)expected+=Math.max(0,g.getSpawned(Vars.state.wave-1));
                expected=Math.max(1,(int)(expected*rules.sector.planet.campaignRules.difficulty.enemySpawnMultiplier));
                Vars.logic.runWave();Time.setDeltaProvider(()->1f);for(int i=0;i<180;i++)Time.update();
                int actual=Groups.unit.count(u->u.team==rules.waveTeam&&u.hasEffect(StatusEffects.boss));
                check(actual==expected,"actual final boss count sector "+p.id+": "+actual+" != "+expected);
                Vars.state.enemies=Groups.unit.count(u->u.team==rules.waveTeam&&!u.dead);
                capture.invoke(Vars.logic);check(rules.waves,"captured with boss still alive "+p.id);
            }
            int oldSize=rules.spawns.size,oldWin=rules.winWave;AureliaCampaign.upgradeLoadedSector();AureliaCampaign.upgradeLoadedSector();check(rules.spawns.size==oldSize&&rules.winWave==oldWin,"non-idempotent upgrade");
            csv.append(String.format(Locale.ROOT,"%d,%d,%d,%d,%d,%d,%d,%d,%d,%.1f,%.1f,%d,%d,%d,%d,%d,%d,%d%n",p.id,Vars.world.width(),Vars.world.height(),gates.size,rules.winWave,boss,units,peak,AureliaCampaign.regularBudget(p),min,max,ores[0],ores[1],ores[2],ores[3],water,wideCount,sig));
            log("map "+p.id+": "+gates.size+" gates, "+boss+" boss encounters / "+units+" bosses, regular peak "+peak+", wide routes "+wideCount);
        }
        check(nonstart==31,"not 31 noninitial maps");check(multiple>0,"no multi-boss maps");Files.writeString(report.resolve("campaign-32.csv"),csv);
        log("ALL 32 campaign maps: 31 noninitial boss sectors, "+multiple+" multi-encounter sectors; unique terrain, balanced ores, water, clearance, wide paths, finite single-gate bosses and bounded regular waves");
    }
    static void blockedLegacy(){
        load(41);var rules=Vars.state.rules;var pre=AureliaPlanetGenerator.preset(41);IntSeq gates=AureliaCampaign.gates();
        for(int i=0;i<gates.size;i++){Tile g=Vars.world.tile(gates.get(i));for(int dx=-3;dx<=3;dx++)for(int dy=-3;dy<=3;dy++)if(dx!=0||dy!=0)Vars.world.tile(g.x+dx,g.y+dy).setBlock(AureliaContent.auroraPlate.wall);}
        int walls=0;for(Tile t:Vars.world.tiles)if(t.block().isStatic())walls++;
        rules.spawns=AureliaWaves.generate(pre.threat,new arc.math.Rand(41));rules.tags.remove(AureliaCampaign.versionTag);
        Vars.state.wave=6;rules.sector.info.wasCaptured=false;rules.sector.info.waves=true;AureliaCampaign.upgradeLoadedSector();
        int count=0;for(SpawnGroup g:rules.spawns)if(g.effect==StatusEffects.boss){count++;check(g.type.flying&&g.shields>3000,"blocked legacy boss has no safe fallback");}
        int after=0;for(Tile t:Vars.world.tiles)if(t.block().isStatic())after++;check(walls==after,"legacy terrain modified");check(count==1,"legacy fallback boss count");
        log("blocked legacy routes: shielded flying boss fallback, existing terrain/structures untouched");
    }
    static void utilities()throws Exception{
        load(AureliaContent.aurelia.startSector);testX=Team.sharded.core().tile.x+35;testY=Team.sharded.core().tile.y+35;int x=testX,y=testY;flat(x,y,15);
        var forge=(mindustry.world.blocks.production.GenericCrafter)block("voxel-forge");
        Building fb=put(forge,x-9,y-8);
        for(var c:forge.consumers)if(c instanceof mindustry.world.consumers.ConsumeItems ci)for(ItemStack stack:ci.items)fb.items.add(stack.item,stack.amount*4);
        check(!fb.acceptLiquid(null,Liquids.cryofluid),"forge accepts removed cryofluid");tick(fb,90);check(fb.items.get(forge.outputItem.item)==0,"forge crafts without coolant");
        fb.liquids.add(AureliaFrontier.auroraCoolant,30);tick(fb,80);check(fb.items.get(forge.outputItem.item)>=2,"forge does not craft with aurora coolant");
        var treatment=(mindustry.world.blocks.production.GenericCrafter)block("water-treatment");Building wb=put(treatment,x-9,y+8);
        check(!wb.acceptLiquid(null,Liquids.water),"treatment still accepts vanilla water");tick(wb,60);near(wb.liquids.get(treatment.outputLiquid.liquid),0,"water treatment without tidewater");
        wb.liquids.add(AureliaContent.tidewater,100);tick(wb,60);near(wb.liquids.get(treatment.outputLiquid.liquid),19.2,"demineralized output per second");near(wb.liquids.get(AureliaContent.tidewater),78.4,"tidewater consumed per second");
        log("live production: new coolant forges alloy; tidewater 21.6/sec -> demineralized water 19.2/sec; old liquids rejected");
        var inc=(LiveIncinerator.LiveIncineratorBuild)put(AureliaUtilities.incinerator,x-6,y);
        check(!inc.acceptItem(null,AureliaContent.lumenite),"cold incinerator accepts");tick(inc,30);check(inc.heat>.99,"no warmup");
        check(inc.acceptItem(null,AureliaCycle.industrialWaste),"hot incinerator rejects items");
        int total=inc.items==null?0:inc.items.total();for(int i=0;i<100;i++)inc.handleItem(null,AureliaCycle.industrialWaste);check((inc.items==null?0:inc.items.total())==total,"items stored instead of destroyed");
        check(inc.acceptLiquid(null,AureliaContent.tidewater),"incinerable liquid rejected");float before=inc.liquids.get(AureliaContent.tidewater);inc.handleLiquid(null,AureliaContent.tidewater,10);near(inc.liquids.get(AureliaContent.tidewater),before,"liquid not destroyed");
        boolean old=AureliaContent.tidewater.incinerable;AureliaContent.tidewater.incinerable=false;check(!inc.acceptLiquid(null,AureliaContent.tidewater),"non-incinerable liquid accepted");AureliaContent.tidewater.incinerable=old;
        inc.enabled=false;check(!inc.acceptItem(null,AureliaContent.lumenite)&&!inc.acceptLiquid(null,AureliaContent.tidewater),"disabled incinerator accepts");inc.enabled=true;
        for(int i=0;i<30;i++){inc.power.status=0;Time.delta=1;inc.update();}check(inc.heat<.01&&!inc.acceptItem(null,AureliaContent.lumenite),"unpowered incinerator never cools");
        var s=(LiveSorter.LiveSorterBuild)put(AureliaUtilities.invertedSorter,x+5,y);
        Building source=put(Blocks.router,x+4,y),east=put(Blocks.router,x+6,y),north=put(Blocks.router,x+5,y+1),south=put(Blocks.router,x+5,y-1);
        s.configure(AureliaContent.lumenite);check(s.config()==AureliaContent.lumenite,"config failed");
        check(s.acceptItem(source,AureliaContent.aurite),"other item rejected");s.handleItem(source,AureliaContent.aurite);check(east.items.get(AureliaContent.aurite)==1,"other item did not pass straight");
        int nc=0,sc=0;
        for(int i=0;i<6;i++){
            check(s.acceptItem(source,AureliaContent.lumenite),"selected item rejected");s.handleItem(source,AureliaContent.lumenite);
            nc+=north.removeStack(AureliaContent.lumenite,1);sc+=south.removeStack(AureliaContent.lumenite,1);
        }
        check(nc==3&&sc==3,"selected item not alternated to sides");
        check(east.items.get(AureliaContent.lumenite)==0,"selected item went straight");
        east.items.set(AureliaContent.aurite,east.block.itemCapacity);check(!s.acceptItem(source,AureliaContent.aurite),"full output accepted");
        east.removeStack(AureliaContent.aurite,999);check(s.acceptItem(source,AureliaContent.aurite),"drained output blocked");east.team=Team.crux;check(!s.acceptItem(source,AureliaContent.aurite),"enemy output accepted");east.team=Team.sharded;
        for(Building input:new Building[]{source,east,north,south}){
            int dir=input.relativeTo(s.tile.x,s.tile.y);
            check(s.getTileTarget(AureliaContent.aurite,input,false)==s.nearby(dir),"straight path orientation");
            Building side=s.getTileTarget(AureliaContent.lumenite,input,false);
            check(side==s.nearby(Mathf.mod(dir-1,4))||side==s.nearby(Mathf.mod(dir+1,4)),"side path orientation");
        }
        Building chainSource=put(Blocks.sorter,x+4,y);put(Blocks.sorter,x+6,y);
        check(!s.acceptItem(chainSource,AureliaContent.aurite),"three instant-transfer sorter chain accepted");
        float phase=s.phase;tick(s,10);check(s.phase!=phase,"sorter does not animate in update");
        byte[] bytes=write(s);s.sortItem=null;read(s,bytes,s.version());check(s.sortItem==AureliaContent.lumenite,"sorter config serialization");
        s.configured(null,null);check(s.sortItem==null,"clear config");
        log("incinerator: cold/hot/power/disable, item+liquid destruction and incinerable guard; inverse sorter: straight/side/alternation/full/enemy/config/save");
    }
    static void shields()throws Exception{
        int x=testX,y=testY;flat(x,y,15);
        WardDome d=(WardDome)AureliaCycle.wardDome;WardDomeBuild b=(WardDomeBuild)put(d,x,y);
        near(b.shield,7000,"initial shield");tick(b,1);b.shield=6000;tick(b,60);near(b.shield,6120,"120 HP/s");
        b.shield=7000;
        BasicBulletType bt=new BasicBulletType(0,100){@Override public float shieldDamage(Bullet bullet){return 250f;}};
        bt.lifetime=120;bt.absorbable=true;
        Bullet shot=bt.create(b,Team.crux,b.x+20,b.y,0);Groups.bullet.update();Groups.bullet.updatePhysics();b.efficiency=1;b.updateTile();near(b.shield,6750,"official shieldDamage");check(shot.absorbed||!shot.isAdded(),"shot not absorbed");
        Groups.bullet.clear();b.shield=7000;
        bt.create(b,Team.sharded,b.x+20,b.y,0);Groups.bullet.update();Groups.bullet.updatePhysics();b.updateTile();near(b.shield,7000,"friendly shot absorbed");Groups.bullet.clear();
        bt.absorbable=false;bt.create(b,Team.crux,b.x+20,b.y,0);Groups.bullet.update();Groups.bullet.updatePhysics();b.updateTile();near(b.shield,7000,"unabsorbable shot counted");Groups.bullet.clear();bt.absorbable=true;
        b.enabled=false;bt.create(b,Team.crux,b.x+20,b.y,0);Groups.bullet.update();Groups.bullet.updatePhysics();b.updateTile();near(b.shield,7000,"disabled shield interception");Groups.bullet.clear();b.enabled=true;
        check(b.absorbExplosion(b.x+20,b.y,100),"vanilla crash explosion contract");near(b.shield,6800,"crash multiplier");check(!b.absorbExplosion(b.x+d.radius+10,b.y,100),"outside explosion absorbed");
        b.damageShield(7000);near(b.shield,0,"break shield");near(b.cooldown,840,"14s cooldown");check(b.shouldConsume(),"broken shield cannot request power");
        for(int i=0;i<60;i++){b.power.status=0;b.update();}near(b.cooldown,840,"unpowered rebuild advanced");
        tick(b,839);check(b.cooldown>0&&b.shield==0,"early rebuild");tick(b,1);near(b.cooldown,0,"rebuild timing");near(b.shield,7000,"rebuild did not refill shield");
        b.liquids.add(d.boostLiquid,d.liquidCapacity);b.shield=6000;tick(b,60);near(b.shield,6228,"coolant regen");check(b.realRadius()>d.radius,"coolant radius");
        b.items.add(d.boostItem,1);tick(b,1);near(b.realCapacity(),12200,"shard cap");check(b.items.get(d.boostItem)==0&&b.charge>0,"shard consumption");
        b.cooldown=840;b.shield=0;int ticks=0;while(b.cooldown>0&&ticks<900){b.liquids.set(d.boostLiquid,60);tick(b,1);ticks++;}check(ticks==443,"coolant rebuild "+ticks);near(b.shield,12200,"boosted full rebuild");
        b.enabled=false;b.charge=1;tick(b,2);near(b.realCapacity(),7000,"boost expiry");near(b.shield,7000,"offline shield exceeds cap");b.enabled=true;
        b.shield=4321;b.cooldown=0;b.charge=0;byte[] saved=write(b);b.shield=1;read(b,saved,(byte)1);near(b.shield,4321,"revision1 exact shield save");read(b,saved,(byte)0);near(b.shield,7000,"revision0 migration");
        b.shield=0;b.cooldown=321;saved=write(b);read(b,saved,(byte)0);near(b.cooldown,321,"old broken cooldown");near(b.shield,0,"old broken shield");near(b.sense(LAccess.shield),0,"broken sensor");
        b.cooldown=0;b.shield=2345;near(b.sense(LAccess.shield),2345,"shield sensor");
        Field bars=Block.class.getDeclaredField("barMap");bars.setAccessible(true);var map=(arc.struct.OrderedMap)bars.get(d);check(map.containsKey("shield")&&map.containsKey("rebuild"),"always-present shield/rebuild bars");
        check(Core.bundle.has("bar.blackhole-ward-capacity"),"numeric bar translation");
        log("shield: full start, 120 HP/s, shieldDamage, friendly/nonabsorbable/disabled guards, crash explosions, power pause, 840-tick full rebuild, coolant 443-tick rebuild, shard cap/expiry, sensor, numeric bar bindings, revisions 0/1");
    }
    static void save(Path report)throws Exception{
        Fi file=new Fi(report.resolve("v84-roundtrip.msav").toFile());int x=testX,y=testY;
        WardDomeBuild b=(WardDomeBuild)Vars.world.tile(x,y).build;b.shield=3456;b.cooldown=0;
        var sorter=(LiveSorter.LiveSorterBuild)put(AureliaUtilities.invertedSorter,x+8,y);sorter.configure(AureliaContent.lumenite);
        SaveIO.save(file);check(SaveIO.isSaveValid(file),"invalid new save");SaveIO.load(file);
        b=(WardDomeBuild)Vars.world.tile(x,y).build;near(b.shield,3456,"full-save shield preservation");
        sorter=(LiveSorter.LiveSorterBuild)Vars.world.tile(x+8,y).build;check(sorter.sortItem==AureliaContent.lumenite,"full-save sorter config");
        log("real .msav save/load roundtrip preserves damaged shield and inverse-sorter configuration");
    }
    public static String runSafe(){try{run();return "V84-ALL-PASSED";}catch(Throwable t){t.printStackTrace();return "V84-FAIL: "+t;}}
    public static String oldSafe(){try{oldSave();return "V84-OLD-PASSED";}catch(Throwable t){t.printStackTrace();return "V84-FAIL: "+t;}}
    public static void run()throws Exception{
        Path out=Path.of(System.getProperty("v84.report","reports"));Files.createDirectories(out);
        content();campaign(out);blockedLegacy();utilities();shields();save(out);
        log("INTEGRATION COMPLETE: "+assertions+" assertions passed");
    }
    public static void oldSave()throws Exception{
        Fi f=new Fi(System.getProperty("v84.oldsave"));
        AureliaContent.aurelia.sectors.get(65).info=mindustry.io.JsonIO.read(SectorInfo.class,f.sibling("v83-sector-info.json").readString());
        SaveIO.load(f);Time.delta=1f;
        testX=Team.sharded.core().tile.x+35;testY=Team.sharded.core().tile.y+35;
        int x=testX,y=testY;
        check(Vars.world.tile(x+10,y).block()==Blocks.air,"removed commander not replaced with air");
        WardDomeBuild b=(WardDomeBuild)Vars.world.tile(x,y).build;near(b.shield,7000,"old save shield upgrade");
        check(Vars.state.wave==100,"old wave reset: "+Vars.state.wave);check(Vars.state.rules.winWave>=103,"old active final boss not scheduled in future");
        check(Team.sharded.core().items.get(AureliaContent.lumenite)==777,"old inventory lost");
        int n=Vars.state.rules.spawns.count(g->g.effect==StatusEffects.boss);check(n>=1,"old save has no bosses");
        AureliaCampaign.upgradeLoadedSector();AureliaCampaign.upgradeLoadedSector();check(n==Vars.state.rules.spawns.count(g->g.effect==StatusEffects.boss),"duplicate old save bosses");
        int win=Vars.state.rules.winWave;Vars.state.rules.waves=false;Vars.state.rules.sector.info.wasCaptured=true;Vars.state.rules.sector.info.waves=false;
        Vars.state.rules.tags.remove(AureliaCampaign.versionTag);AureliaCampaign.upgradeLoadedSector();
        check(!Vars.state.rules.waves&&Vars.state.rules.winWave==win,"captured legacy sector restarted");
        log("ACTUAL v8.3 -> v8.4 save migration: commander becomes air, shield restored, inventory and wave100 retained, future final boss, idempotent patch");
    }
}
