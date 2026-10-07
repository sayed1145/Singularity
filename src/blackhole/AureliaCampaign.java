package blackhole;

import arc.math.*;
import arc.struct.*;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.Tile;

/** v8.4 bounded campaign pressure + explicit, finite, single-gate boss encounters. */
public final class AureliaCampaign{
    public static final String versionTag="blackhole-campaign-v84";
    private AureliaCampaign(){}

    public static IntSeq gates(){
        IntSeq out=new IntSeq();
        for(Tile t:Vars.world.tiles)if(t.overlay()==Blocks.spawn)out.add(t.pos());
        out.sort();return out;
    }
    public static Seq<SpawnGroup> waves(AureliaPlanetGenerator.Preset p,Rand random,IntSeq gates){
        Seq<SpawnGroup> out=AureliaWaves.generate(p.threat,random);
        balance(out,p,gates.size);
        addBosses(out,p,p.winWave,gates);
        return out;
    }
    /** A five-gate sector no longer multiplies the entire two-gate design into hundreds of units. */
    public static void balance(Seq<SpawnGroup> groups,AureliaPlanetGenerator.Preset p,int gates){
        int ordinary=groups.count(g->g.effect!=StatusEffects.boss);
        int perGroup=Math.max(1,regularBudget(p)/Math.max(1,gates)/Math.max(1,ordinary));
        for(SpawnGroup g:groups){
            if(g.effect==StatusEffects.boss)continue;
            g.max=Math.min(g.max,perGroup);g.unitAmount=Math.min(g.unitAmount,g.max);
            if(g.unitScaling<SpawnGroup.never)g.unitScaling*=Math.max(1f,gates/2f);
        }
    }
    public static int regularBudget(AureliaPlanetGenerator.Preset p){return 40+Math.round(50*p.threat);}
    public static int encounterCount(AureliaPlanetGenerator.Preset p){
        if(p.id==AureliaContent.aurelia.startSector)return 0;
        return p.threat>=.72f?3:p.threat>=.45f?2:1;
    }
    public static void addBosses(Seq<SpawnGroup> groups,AureliaPlanetGenerator.Preset p,int winWave,IntSeq gates){
        int count=encounterCount(p);
        if(count==0)return;
        if(gates.isEmpty())throw new IllegalStateException("Campaign boss has no spawn gate: "+p.id);
        for(int i=0;i<count;i++){
            int wave=i==count-1?Math.max(1,winWave-1):Math.max(8,Math.round(winWave*(i==0?.58f:.78f)));
            boolean finalWave=i==count-1;
            UnitType type=p.threat>=.65f&&finalWave?AureliaContent.auroraHeavy:AureliaContent.auriteBulwark;
            SpawnGroup boss=new SpawnGroup(type);
            boss.effect=StatusEffects.boss;
            boss.begin=boss.end=wave-1; //runWave spawns index state.wave-1, then increments state.wave; capture checks >= winWave.
            //Therefore the finale must be index winWave-2, not the unreachable winWave-1.
            boss.unitAmount=boss.max=(p.threat>=.84f&&finalWave)?2:1;
            boss.unitScaling=SpawnGroup.never;boss.shieldScaling=0;
            boss.shields=finalWave?Math.round(500*p.threat):Math.round(200*p.threat);
            boss.spawn=gates.get(Mathf.mod(p.id+i,gates.size));
            groups.add(boss);
        }
    }
    /** Old active saves get encounters without resetting terrain, items, research or the current wave. */
    public static void upgradeLoadedSector(){
        if(Vars.state==null||Vars.state.rules==null)return;
        Rules r=Vars.state.rules;Sector sector=r.sector;
        if(sector==null||sector.planet!=AureliaContent.aurelia||r.tags.containsKey(versionTag))return;
        AureliaPlanetGenerator.Preset p=AureliaPlanetGenerator.preset(sector.id);
        if(p==null)return;
        IntSeq gates=gates();if(gates.isEmpty())return;
        //No prior version ships boss groups; removing them makes manual test/re-entry idempotent too.
        r.spawns.removeAll(g->g.effect==StatusEffects.boss);
        balance(r.spawns,p,gates.size);
        if(encounterCount(p)>0 && r.waves && !sector.isCaptured())r.winWave=Math.max(r.winWave,Vars.state.wave+3);
        if(r.winWave<=0)r.winWave=p.winWave;
        addBosses(r.spawns,p,r.winWave,gates);
        protectLegacyBossRoutes(r,gates);
        r.tags.put(versionTag,"1");
        sector.info.winWave=r.winWave;sector.saveInfo();
        Log.info("[blackhole/v8.4] campaign sector @ upgraded: @ boss encounters, final encounter wave @",sector.id,encounterCount(p),r.winWave-1);
    }
    /** Do not excavate a player's saved base. Prefer a safe existing ground gate; otherwise use
     * a shielded flying sentry with the same nominal boss health budget, rather than a trapped tank. */
    private static void protectLegacyBossRoutes(Rules rules,IntSeq gates){
        var core=rules.defaultTeam.core();if(core==null)return;
        int w=Vars.world.width(),h=Vars.world.height();boolean[] open=new boolean[w*h],seen=new boolean[w*h];
        for(int y=1;y<h-1;y++)for(int x=1;x<w-1;x++){
            boolean ok=true;
            for(int dx=-1;dx<=1&&ok;dx++)for(int dy=-1;dy<=1&&ok;dy++){
                Tile t=Vars.world.tile(x+dx,y+dy);
                //Destructible player defenses are legitimate targets, not terrain obstructions.
                ok=!t.floor().isDeep()&&!t.block().isStatic();
            }
            open[x+y*w]=ok;
        }
        int[] queue=new int[w*h];int head=0,tail=0,start=core.tile.x+core.tile.y*w;
        queue[tail++]=start;seen[start]=true;
        while(head<tail){
            int at=queue[head++],x=at%w,y=at/w;
            for(int dir=0;dir<4;dir++){
                int nx=x+arc.math.geom.Geometry.d4x[dir],ny=y+arc.math.geom.Geometry.d4y[dir];
                if(nx<0||nx>=w||ny<0||ny>=h)continue;
                int next=nx+ny*w;if(open[next]&&!seen[next]){seen[next]=true;queue[tail++]=next;}
            }
        }
        IntSeq safe=new IntSeq();
        for(int i=0;i<gates.size;i++){
            Tile g=Vars.world.tile(gates.get(i));boolean ok=seen[g.x+g.y*w];
            for(int dx=-3;dx<=3&&ok;dx++)for(int dy=-3;dy<=3&&ok;dy++){
                Tile t=Vars.world.tile(g.x+dx,g.y+dy);ok=t!=null&&!t.floor().isDeep()&&!t.block().isStatic();
            }
            if(ok)safe.add(g.pos());
        }
        for(SpawnGroup g:rules.spawns){
            if(g.effect!=StatusEffects.boss||g.type.flying||safe.contains(g.spawn))continue;
            if(!safe.isEmpty())g.spawn=safe.get(Mathf.mod(g.begin,safe.size));
            else{
                g.shields+=Math.max(0f,g.type.health-AureliaNova.auroraSentry.health)*StatusEffects.boss.healthMultiplier;
                g.type=AureliaNova.auroraSentry;
            }
        }
    }

}
