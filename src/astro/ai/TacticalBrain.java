package astro.ai;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import java.util.Arrays;
import mindustry.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.meta.*;

/**
 * Probabilistic-space tactical brain (v1.3). While a tactical commander of a team is online, every combat unit of that
 * team that is not controlled or hosted by a player is taken over. Three layers work together:
 *
 * <ol>
 * <li><b>Planner</b> (this class): units are grouped into squads; each squad samples its future with a small
 *     Monte-Carlo Lanchester exchange for every candidate tactic (engage, flank, kite, retreat), with a matchup-aware
 *     damage model (air / ground coverage, armor), terrain awareness (openness, blocked flank routes) and an
 *     early-game / low-strength caution. Tactics are drawn from a softmax. Squads decide every 10 ticks in contact,
 *     immediately when they lose health, every 45 ticks otherwise.</li>
 * <li><b>Roles and missions</b>: anti-air, line, tank, siege, support and skirmisher units get role specific orders
 *     and overkill-aware focus allocation; fast squads are sent raiding undefended enemy infrastructure while the main
 *     body pushes (multi-front), sometimes with a pincer waypoint.</li>
 * <li><b>Reflex layer</b> ({@link Micro}): every tick, time budgeted, round robin: instant retarget, bullet dodging,
 *     kiting, strafing and corridor unjamming.</li>
 * </ol>
 *
 * Everything is budgeted (planner {@link #budgetMs}, reflexes {@link #microBudgetMs} per tick), so a thousand units
 * cost the same frame time as ten. Units controlled by a player, by logic, or carrying a player's order are never
 * touched.
 */
public final class TacticalBrain{
    private TacticalBrain(){}

    public static final int ENGAGE = 0, FLANK = 1, KITE = 2, RETREAT = 3, ADVANCE = 4, DEFEND = 5, RAID = 6, PINCER = 7, HOLD = 8;
    /** "no override": the brain decides by itself (the default, fully programmatic mode) */
    public static final int AUTO = -1;
    public static final String[] NAMES = {"engage", "flank", "kite", "retreat", "advance", "defend", "raid", "pincer", "hold"};
    /** minimum time a squad keeps a tactic before it may change it (ticks); emergencies and a new situation override it */
    public static float dwell = 70f;
    /** how often units changed their tactic (for the test and the panel); a stable brain keeps this low */
    public static int tacticFlips;
    /** per-tick time budget of the planner and of the reflex layer in milliseconds */
    public static float budgetMs = 0.7f, microBudgetMs = 0.35f;
    public static float cell = 200f, scanRange = 560f, horizon = 8f, temperature = 0.14f, markWindow = 12f;
    public static final int samples = 14, maxSquadSize = 28;
    /** decision intervals in ticks: fighting at close range / fighting / calm */
    public static float fastInterval = 10f, contactInterval = 20f, calmInterval = 45f;

    // statistics (visible on the commander)
    public static float lastMs, avgMs, maxMs;
    public static int squadCount, unitCount, raidSquads;
    public static final int[] decisions = new int[9];
    public static long decisionTotal;
    /** units that were ordered back because nothing in reach can be hit by them (role / matchup logic) */
    public static int idleUseless, supportHolds, siegeHolds, raidOrders, flankBlocked;

    /** test hook: battlefield role a unit type is given by the brain */
    public static String roleOf(mindustry.type.UnitType t){
        UnitProfile p = UnitProfile.of(t);
        return UnitProfile.ROLE_NAMES[p.role] + (p.hitsAir ? "+air" : "") + (p.hitsGround ? "+ground" : "");
    }

    /** what the player can choose on the command core panel (per team); the defaults are the fully automatic brain */
    public static class Settings{
        /** AUTO (-1) or a tactic index: the forced decision mode of every squad */
        public int mode = AUTO;
        /** reflex layer (dodging, kiting, strafing, unjamming, instant refocus) */
        public boolean micro = true;
        /** multi-front raids on undefended infrastructure */
        public boolean raids = true;
        /** role aware behaviour (healers behind, artillery at range, anti-air waits) */
        public boolean roles = true;
        /** 0.5 cautious ... 1.5 aggressive */
        public float aggression = 1f;
    }

    /** a live snapshot for the panel */
    public static class Info{
        public boolean active, weak;
        public int units, squads, hosted;
        public float power, enemyPower;
        public final int[] tactics = new int[9];
    }

    public static Settings settings(Team team){
        TeamState st = states.get(team);
        if(st == null) states.put(team, st = new TeamState(team));
        return st.cfg;
    }

    public static Info info(Team team, Info out){
        if(out == null) out = new Info();
        TeamState st = states.get(team);
        if(st == null){ out.active = false; out.units = out.squads = out.hosted = 0; java.util.Arrays.fill(out.tactics, 0); return out; }
        out.active = st.active; out.weak = st.weak; out.units = st.mine.size; out.squads = st.squads.size;
        out.power = st.power; out.enemyPower = st.enemyPower; out.hosted = st.hosted;
        System.arraycopy(st.tacticUnits, 0, out.tactics, 0, 9);
        return out;
    }

    private static final ObjectMap<Team, TeamState> states = new ObjectMap<>();
    private static boolean registered;

    static class Cmd{
        int kind; //0 = position, 1 = attack
        float x, y;
        int target = -1;
        float time;
    }

    /** memory of a squad (keyed by its map cell) between decisions */
    static class Meta{
        float due, hp = -1f, prio, commitHp = -1f;
        int tactic = -1;
        boolean contact;
        int mission; //0 none, 1 raid
        int raidPos = -1;
        float raidUntil, seen;
    }

    /** stable centre of a squad: units join the nearest anchor, so a squad keeps its identity (and its plan) while it moves */
    static class Anchor{
        int id, n;
        float x, y, sx, sy;
    }

    static class Squad{
        int key, tactic;
        final Seq<Unit> units = new Seq<>();
        float cx, cy, hp, dps, rng, spd;
        Meta meta;
    }

    static class TeamState{
        final Team team;
        float mark = -1e9f, timer, takeTimer, powerTimer, cleanTimer;
        boolean active, weak;
        float power, enemyPower;
        int microCursor;
        final Seq<Unit> mine = new Seq<>();
        final IntMap<Cmd> cmds = new IntMap<>();
        final IntFloatMap playerUntil = new IntFloatMap();
        final IntMap<Meta> meta = new IntMap<>();
        final IntSet claims = new IntSet();
        final Seq<Squad> squads = new Seq<>();
        final Seq<Squad> queue = new Seq<>();
        int taken, hosted, nextAnchor = 1;
        final Settings cfg = new Settings();
        final Seq<Anchor> anchors = new Seq<>();
        final int[] tacticUnits = new int[9];
        TeamState(Team t){ team = t; }
    }

    public static void register(){
        if(registered) return;
        registered = true;
        Events.run(Trigger.update, TacticalBrain::update);
        Events.on(WorldLoadEvent.class, e -> { states.clear(); UnitProfile.clear(); });
    }

    /** called every tick by an online commander building of this team */
    public static void mark(Team team){
        TeamState st = states.get(team);
        if(st == null) states.put(team, st = new TeamState(team));
        st.mark = Time.time;
    }

    public static boolean active(Team team){
        TeamState st = states.get(team);
        return st != null && st.active;
    }

    public static int taken(Team team){
        TeamState st = states.get(team);
        return st == null || !st.active ? 0 : st.mine.size;
    }

    // ===================================================================================================

    static void update(){
        if(Vars.net != null && Vars.net.client()) return;
        long t0 = Time.nanos();
        for(TeamState st : states.values()){
            boolean on = Time.time - st.mark < markWindow;
            if(on != st.active){
                st.active = on;
                if(!on) release(st);
            }
            if(on) tick(st, t0);
        }
        float ms = (Time.nanos() - t0) / 1e6f;
        if(ms > 0.0005f || squadCount > 0){
            lastMs = ms;
            avgMs = Mathf.lerp(avgMs, ms, 0.05f);
            maxMs = Math.max(maxMs * 0.998f, ms);
        }
    }

    /** A unit the brain may take over: an idle, AI-driven combat unit. Never a unit under player or logic control. */
    static boolean eligible(Unit u){
        if(u == null || !u.isValid() || u.isPlayer() || u.type == null || u.type.internal || u.spawnedByCore) return false;
        if(u.controller() instanceof Player) return false;
        if(u.type.weapons.size == 0 || u.type.mineTier > 0 || u.type.buildSpeed > 0f) return false;
        AIController c = u.controller() instanceof AIController a ? a : null;
        if(c == null) return false;
        if(c instanceof LogicAI) return false;
        if(c instanceof TacticalAI) return true;
        if(c instanceof CommandAI ca){
            //a command the player chose (assist, mine, rebuild, ...) stays the player's: only the default behaviour is taken
            return ca.command == null || ca.command == UnitCommand.moveCommand || ca.command == u.type.defaultCommand;
        }
        return c instanceof GroundAI || c instanceof FlyingAI || c instanceof SuicideAI || c.getClass() == AIController.class;
    }

    /** test hook and public query: would the brain take this unit over? (never for players, logic, hosted orders) */
    public static boolean canTake(Unit u){
        if(!eligible(u)) return false;
        if(u.controller() instanceof CommandAI ca && !(u.controller() instanceof TacticalAI)) return ca.targetPos == null && ca.attackTarget == null && !ca.commandQueue.any();
        return true;
    }

    static void takeOver(TeamState st, Unit u){
        AIController old = (AIController)u.controller();
        if(old instanceof TacticalAI) return;
        if(old instanceof CommandAI ca){
            //an order given by a player at this very moment: leave the unit alone, hosted units are never replaced
            if(ca.targetPos != null || ca.attackTarget != null || ca.commandQueue.any()){
                st.playerUntil.put(u.id, Time.time + 900f);
                return;
            }
        }
        TacticalAI ai = new TacticalAI();
        ai.original = old;
        u.controller(ai);
        if(old instanceof CommandAI ca){
            ai.command = ca.command;
            ai.stances.set(ca.stances);
        }
    }

    static void tick(TeamState st, long t0){
        st.timer += Time.delta;
        st.takeTimer += Time.delta;
        st.powerTimer += Time.delta;
        st.cleanTimer += Time.delta;
        Team team = st.team;

        //1. take over every eligible combat unit (twice a second)
        if(st.takeTimer >= 30f){
            st.takeTimer = 0f;
            st.mine.clear();
            Seq<Unit> all = team.data().units;
            int hosted = 0;
            for(int i = 0; i < all.size; i++){
                Unit u = all.get(i);
                if(!eligible(u)){
                    //a combat unit that somebody else controls (a player, a logic processor, a player's order) is left alone
                    if(u != null && u.isValid() && u.type != null && u.type.weapons.size > 0 && (u.isPlayer() || u.controller() instanceof Player || u.controller() instanceof LogicAI || st.playerUntil.get(u.id, 0f) > Time.time)) hosted++;
                    continue;
                }
                if(!(u.controller() instanceof TacticalAI)) takeOver(st, u);
                if(u.controller() instanceof CommandAI) st.mine.add(u);
            }
            unitCount = st.mine.size;
            st.taken = st.mine.size;
            st.hosted = hosted;
            //forget units that are gone
            if(st.cmds.size > st.mine.size * 2 + 16){
                IntSet alive = new IntSet();
                for(Unit u : st.mine) alive.add(u.id);
                var it = st.cmds.keys();
                IntSeq dead = new IntSeq();
                while(it.hasNext){ int k = it.next(); if(!alive.contains(k)) dead.add(k); }
                for(int i = 0; i < dead.size; i++) st.cmds.remove(dead.get(i));
            }
        }

        //2. relative strength (early game / low resources caution), twice per two seconds
        if(st.powerTimer >= 60f){
            st.powerTimer = 0f;
            assess(st);
        }
        if(st.cleanTimer >= 900f){
            st.cleanTimer = 0f;
            IntSeq dead = new IntSeq();
            for(var e : st.meta.entries()) if(Time.time - e.value.seen > 900f) dead.add(e.key);
            for(int i = 0; i < dead.size; i++) st.meta.remove(dead.get(i));
        }

        //3. every 10 ticks regroup the squads (cheap) and queue the ones that are due
        if(st.timer >= 10f && st.queue.isEmpty()){
            st.timer = 0f;
            buildSquads(st);
        }

        //4. decide squads, most urgent first, until the budget of this tick is used up
        float budgetNs = budgetMs * 1e6f;
        while(st.queue.size > 0 && Time.nanos() - t0 < budgetNs){
            Squad sq = st.queue.pop();
            if(sq.units.isEmpty()) continue;
            decide(st, sq);
        }
        squadCount = st.squads.size;

        //5. reflexes
        long t1 = Time.nanos();
        Micro.run(st, t1, microBudgetMs * 1e6f);
        float mms = (Time.nanos() - t1) / 1e6f;
        Micro.ms = mms;
        Micro.avgMs = Mathf.lerp(Micro.avgMs, mms, 0.05f);
    }

    /** our strength against the strength of every enemy team (sampled): decides whether to push or to hold */
    static void assess(TeamState st){
        float ours = 0f;
        for(int i = 0; i < st.mine.size; i++){
            Unit u = st.mine.get(i);
            ours += UnitProfile.of(u.type).dps * (float)Math.sqrt(Math.max(u.health + u.shield, 1f));
        }
        float theirs = 0f;
        for(Teams.TeamData td : Vars.state.teams.present){
            if(td.team == st.team || td.team == Team.derelict) continue;
            Seq<Unit> us = td.units;
            int stride = Math.max(1, us.size / 120);
            for(int i = 0; i < us.size; i += stride){
                Unit u = us.get(i);
                if(u.type == null || u.type.weapons.size == 0) continue;
                theirs += stride * UnitProfile.of(u.type).dps * (float)Math.sqrt(Math.max(u.health + u.shield, 1f));
            }
        }
        st.power = ours;
        st.enemyPower = theirs;
        st.weak = st.mine.size <= 4 || (theirs > 1f && ours < theirs * 0.75f / Mathf.clamp(st.cfg.aggression, 0.4f, 1.8f));
    }

    static final float joinRadius = 260f, mergeRadius = 120f;

    static void buildSquads(TeamState st){
        st.squads.clear();
        IntMap<Squad> byKey = new IntMap<>();
        boolean tiny = st.mine.size <= 8; //early game: one concentrated squad instead of scattered singles
        float ax = 0, ay = 0;
        if(tiny && st.mine.size > 0){
            for(Unit u : st.mine){ ax += u.x; ay += u.y; }
            ax /= st.mine.size; ay /= st.mine.size;
        }
        //anchors: a squad is the set of units nearest to one of a few slowly moving centres. Unlike fixed map cells this
        //does not reshuffle squads (and throw away their plans) whenever a unit crosses a cell border.
        Seq<Anchor> anchors = st.anchors;
        for(Anchor a : anchors){ a.n = 0; a.sx = 0; a.sy = 0; }
        for(int i = 0; i < anchors.size; i++){
            Anchor a = anchors.get(i);
            for(int j = anchors.size - 1; j > i; j--){
                Anchor b = anchors.get(j);
                if(Mathf.within(a.x, a.y, b.x, b.y, mergeRadius)) anchors.remove(j);
            }
        }
        for(Unit u : st.mine){
            if(!u.isValid()) continue;
            int key;
            if(tiny && Mathf.within(u.x, u.y, ax, ay, 700f)) key = 7;
            else{
                Anchor best = null;
                float bd = joinRadius * joinRadius;
                for(int i = 0; i < anchors.size; i++){
                    Anchor a = anchors.get(i);
                    float d = Mathf.dst2(a.x, a.y, u.x, u.y);
                    if(d < bd){ bd = d; best = a; }
                }
                if(best == null){
                    best = new Anchor();
                    best.id = 100 + st.nextAnchor++;
                    best.x = u.x; best.y = u.y;
                    anchors.add(best);
                }
                best.n++; best.sx += u.x; best.sy += u.y;
                key = best.id;
            }
            Squad sq = byKey.get(key);
            //big crowds split into several squads
            int part = 0;
            while(sq != null && sq.units.size >= maxSquadSize){
                part++;
                sq = byKey.get(key * 31 + part);
            }
            if(sq == null){
                sq = new Squad();
                sq.key = part == 0 ? key : key * 31 + part;
                byKey.put(sq.key, sq);
                st.squads.add(sq);
            }
            sq.units.add(u);
        }
        for(int i = anchors.size - 1; i >= 0; i--){
            Anchor a = anchors.get(i);
            if(a.n == 0){ anchors.remove(i); continue; }
            //drift to the centroid, but slowly: the identity must survive a moving column
            a.x = Mathf.lerp(a.x, a.sx / a.n, 0.35f);
            a.y = Mathf.lerp(a.y, a.sy / a.n, 0.35f);
        }
        st.queue.clear();
        st.claims.clear();
        for(Unit u : st.mine) if(u.controller() instanceof TacticalAI ta && ta.raidPos >= 0 && Time.time < ta.raidUntil) st.claims.add(ta.raidPos);
        float now = Time.time;
        java.util.Arrays.fill(st.tacticUnits, 0);
        for(Squad sq : st.squads){
            Meta m = st.meta.get(sq.key);
            if(m == null) st.meta.put(sq.key, m = new Meta());
            m.seen = now;
            sq.meta = m;
            if(m.tactic >= 0 && m.tactic < 9) st.tacticUnits[m.tactic] += sq.units.size;
            //event trigger: the squad is losing health faster than the plan expected
            float hp = 0f;
            for(int i = 0; i < sq.units.size; i++){ Unit u = sq.units.get(i); hp += u.health + u.shield; }
            boolean hurt = m.hp > 0f && hp < m.hp * 0.93f;
            m.hp = hp;
            if(hurt) m.due = 0f; //re-plan at once (the dwell time only holds back pointless flips)
            if(m.due <= now){
                m.prio = (m.contact ? 100f : 0f) + (hurt ? 50f : 0f) - sq.units.size * 0.01f;
                st.queue.add(sq);
            }
        }
        //pop() takes from the end: most urgent last
        st.queue.sort((a, b) -> Float.compare(a.meta.prio, b.meta.prio));
    }

    // ===================================================================================================
    // decision
    // ===================================================================================================

    static final Seq<Unit> foes = new Seq<>();
    static final float[] util = new float[6];
    static final float[] pr = new float[6];

    static float logn(float sigma){
        return (float)Math.exp(Mathf.range(sigma * 1.7f));
    }

    static void decide(TeamState st, Squad sq){
        Team team = st.team;
        Meta meta = sq.meta;
        int n = sq.units.size;
        float cx = 0, cy = 0, hp = 0, dps = 0, rng = 0, spd = 0, rw = 0, airW = 0, armorSum = 0;
        Unit sample = sq.units.first();
        for(int i = 0; i < n; i++){
            Unit u = sq.units.get(i);
            UnitProfile p = UnitProfile.of(u.type);
            cx += u.x; cy += u.y;
            hp += u.health + u.shield;
            dps += p.dps;
            rng += p.range * p.dps; rw += p.dps;
            spd += p.speed;
            if(p.flying) airW += p.dps;
            armorSum += p.armor * p.dps;
            if(!u.isFlying()) sample = u;
        }
        cx /= n; cy /= n; rng /= Math.max(rw, 1f); spd /= n;
        float ourAir = airW / Math.max(dps, 1f), ourArmor = armorSum / Math.max(dps, 1f);
        sq.cx = cx; sq.cy = cy; sq.hp = hp; sq.rng = rng; sq.spd = spd;

        //enemies around the squad
        foes.clear();
        final float R = scanRange;
        Units.nearbyEnemies(team, cx - R, cy - R, R * 2f, R * 2f, e -> {
            if(foes.size < 40 && e.isValid() && e.team != Team.derelict && !e.type.internal) foes.add(e);
        });
        float eHp = 0, eRaw = 0, eRng = 0, eW = 0, ex = 0, ey = 0, eSpd = 0, eAir = 0, eArmor = 0;
        for(int i = 0; i < foes.size; i++){
            Unit e = foes.get(i);
            UnitProfile ep = UnitProfile.of(e.type);
            float h = e.health + e.shield + 1f;
            eHp += h; eRaw += ep.dps; eRng += ep.range * ep.dps; eW += ep.dps; eSpd += ep.speed;
            if(e.isFlying()) eAir += ep.dps;
            eArmor += ep.armor * ep.dps;
            ex += e.x * ep.dps; ey += e.y * ep.dps;
        }
        //a hostile turret nearby counts as a static ground enemy
        Building turret = Vars.indexer.findEnemyTile(team, cx, cy, R, b -> b.block instanceof Turret);
        if(turret != null){
            float d = 28f + (turret.block.size - 1) * 14f;
            eHp += turret.health; eRaw += d; eRng += ((Turret)turret.block).range * d; eW += d;
            ex += turret.x * d; ey += turret.y * d; eArmor += 6f * d;
        }
        boolean contact = eW > 0f;
        float eAirShare = eW > 0f ? eAir / eW : 0f;
        eArmor = eW > 0f ? eArmor / eW : 0f;

        //matchup-aware effective damage: what can actually hit what (air / ground coverage, armor)
        float ourEff = 0f, enEff = 0f;
        for(int i = 0; i < n; i++){
            UnitProfile p = UnitProfile.of(sq.units.get(i).type);
            float cover = (p.hitsAir ? eAirShare : 0f) + (p.hitsGround ? 1f - eAirShare : 0f);
            ourEff += p.dps * cover * UnitProfile.armorFactor(p.perHit, eArmor);
        }
        for(int i = 0; i < foes.size; i++){
            UnitProfile ep = UnitProfile.of(foes.get(i).type);
            float cover = (ep.hitsAir ? ourAir : 0f) + (ep.hitsGround ? 1f - ourAir : 0f);
            enEff += ep.dps * cover * UnitProfile.armorFactor(ep.perHit, ourArmor);
        }
        if(turret != null){
            float d = 28f + (turret.block.size - 1) * 14f;
            enEff += d * UnitProfile.armorFactor(30f, ourArmor);
        }
        float effScale = ourEff / Math.max(dps, 1f);
        //neighbouring squads fight on the same side: count part of their strength so a squad does not
        //retreat from a battle its team is going to win together
        float sup = 0.7f, supR2 = 420f * 420f;
        for(int i = 0; i < st.mine.size; i++){
            Unit u = st.mine.get(i);
            if(u.isValid() && !sq.units.contains(u, true) && Mathf.dst2(cx, cy, u.x, u.y) < supR2){
                hp += (u.health + u.shield) * sup;
                ourEff += UnitProfile.of(u.type).dps * sup * effScale;
            }
        }
        sq.dps = ourEff;

        int tactic;
        float tx = cx, ty = cy;
        float nearest = 1e9f;
        if(contact){ ex /= eW; ey /= eW; eRng /= eW; eSpd = foes.size > 0 ? eSpd / foes.size : 0f; nearest = Mathf.dst(cx, cy, ex, ey); }
        Building core = Vars.state.teams.closestEnemyCore(cx, cy, team);
        if(!contact){
            meta.contact = false;
            tactic = st.cfg.mode == AUTO ? chooseMission(st, sq, meta, core) : forcedMission(st, sq, meta, core, st.cfg.mode);
            if(tactic == RAID){
                Building rb = Vars.world.build(meta.raidPos);
                if(rb != null){ tx = rb.x; ty = rb.y; }
            }else if(tactic == PINCER && core != null){
                float ddx = core.x - cx, ddy = core.y - cy, dl = Math.max(Mathf.len(ddx, ddy), 1f);
                float side = (sq.key & 1) == 0 ? 1f : -1f;
                tx = core.x - ddy / dl * side * 330f; ty = core.y + ddx / dl * side * 330f;
                if(Terrain.solid(sample, tx, ty)){ tx = core.x; ty = core.y; }
            }else if(core != null){ tx = core.x; ty = core.y; }
        }else{
            meta.contact = true;
            float dist = nearest;
            float open = Terrain.openness(sample, cx, cy);
            boolean lineClear = Terrain.clear(sample, cx, cy, ex, ey);
            Arrays.fill(util, 0f);
            int last = meta.tactic;
            float cautious = (st.weak ? 1.25f : 1f) / Mathf.clamp(st.cfg.aggression, 0.4f, 1.8f);
            for(int s = 0; s < samples; s++){
                float ed = logn(0.22f), ud = logn(0.15f), risk = (0.75f + Mathf.random(0.6f)) * cautious, es = logn(0.25f);
                float eEff = enEff * ed, uEff = ourEff * ud;
                for(int t = 0; t <= RETREAT; t++){
                    float ourM, enM, delay;
                    float engageDelay = Math.max(0f, dist - rng * 0.8f) / Math.max(spd * 60f, 1f);
                    float enDelay = Math.max(0f, dist - eRng * 0.9f) / Math.max(eSpd * es * 60f, 1f);
                    float rr = rng / Math.max(eRng, 1f);
                    switch(t){
                        case ENGAGE -> { ourM = 1.15f; enM = 1f; delay = engageDelay; }
                        case FLANK -> { ourM = 0.95f; enM = 0.72f; delay = engageDelay * 1.4f + 1.4f; }
                        case KITE -> {
                            delay = Math.max(0f, dist - rng * 0.9f) / Math.max(spd * 60f, 1f);
                            if(rr > 1.15f){ ourM = 0.9f; enM = 0.4f; }
                            else if(rr > 0.9f){ ourM = 0.85f; enM = 0.7f; }
                            else{ ourM = 0.6f; enM = 0.95f; }
                        }
                        default -> { ourM = 0f; enM = eSpd * es > spd ? 0.6f : 0.25f; delay = 99f; }
                    }
                    float uh = hp, eh = eHp, u0 = Math.max(hp, 1f), e0 = Math.max(eHp, 1f);
                    for(int step = 0; step < 4; step++){
                        float dt = horizon / 4f, t1 = dt * (step + 1);
                        float ourT = Mathf.clamp(t1 - delay, 0f, dt), enT = Mathf.clamp(t1 - enDelay * (t == RETREAT ? 2.5f : 1f), 0f, dt);
                        float dOut = uEff * ourM * Math.max(uh / u0, 0f) * ourT;
                        float dIn = eEff * enM * Math.max(eh / e0, 0f) * enT;
                        eh = Math.max(0f, eh - dOut);
                        uh = Math.max(0f, uh - dIn);
                    }
                    float ue = 1f - eh / e0, ul = 1f - uh / u0;
                    float v = ue - risk * ul * 0.9f;
                    if(eh <= 0f) v += 0.6f;
                    if(uh <= 0f) v -= 0.8f;
                    util[t] += v / samples;
                }
            }
            //terrain: a flank needs room and a way round; a squeezed corridor also punishes spreading out
            util[FLANK] -= (1f - open) * 0.35f + (lineClear ? 0f : 0.22f);
            util[KITE] -= (1f - open) * 0.12f;
            //commitment: a squad keeps its tactic for a minimum time unless something changed (new situation, heavy losses);
            //without it the probabilistic choice would flip every decision and the units would twitch between plans
            int prev = -1; float age = 1e9f;
            Arrays.fill(vote, 0);
            for(int i = 0; i < n; i++){
                if(sq.units.get(i).controller() instanceof TacticalAI ta && ta.tactic >= 0 && ta.tactic <= RETREAT){
                    vote[ta.tactic]++;
                    age = Math.min(age, Time.time - ta.tacticAt);
                }
            }
            int bestV = 0;
            for(int t = 0; t <= RETREAT; t++) if(vote[t] > bestV){ bestV = vote[t]; prev = t; }
            if(bestV * 2 < n) prev = -1;
            boolean emergency = meta.commitHp > 0f && hp < meta.commitHp * 0.72f;
            if(st.cfg.mode != AUTO){
                tactic = st.cfg.mode;
                meta.commitHp = hp;
            }else if(prev >= 0 && age < dwell && !emergency){
                tactic = prev;
            }else{
                //hysteresis, then sample the tactic from the softmax distribution
                if(prev >= 0) util[prev] += 0.2f;
                else if(last >= 0 && last <= RETREAT) util[last] += 0.08f;
                float mx = -1e9f;
                for(int t = 0; t <= RETREAT; t++) mx = Math.max(mx, util[t]);
                float sum = 0f;
                final float temp = temperature * 0.8f;
                for(int t = 0; t <= RETREAT; t++){ pr[t] = (float)Math.exp((util[t] - mx) / temp); sum += pr[t]; }
                float r = Mathf.random(sum);
                tactic = RETREAT;
                for(int t = 0; t <= RETREAT; t++){ r -= pr[t]; if(r <= 0f){ tactic = t; break; } }
                meta.commitHp = hp;
            }
            tx = ex; ty = ey;
            sq.dps = ourEff;
        }
        sq.tactic = tactic;
        meta.tactic = tactic;
        //adaptive reaction time: close fights are re-planned every 10 ticks, distant ones rarely
        float interval = !contact ? calmInterval : nearest < Math.max(rng, 120f) * 1.6f ? fastInterval : contactInterval;
        meta.due = Time.time + interval * (0.85f + Mathf.random(0.3f));
        decisions[tactic]++;
        decisionTotal++;
        assign(st, sq, tactic, tx, ty, turret != null ? turret : core, core);
        float nowT = Time.time;
        for(int i = 0; i < n; i++){
            if(!(sq.units.get(i).controller() instanceof TacticalAI ta)) continue;
            if(ta.tactic != tactic){
                if(ta.tactic >= 0 && ta.tactic <= RETREAT && tactic <= RETREAT) tacticFlips++;
                ta.tactic = tactic; ta.tacticAt = nowT;
            }
        }
    }

    /** the tactic a forced decision mode means while no enemy is in sight */
    static int forcedMission(TeamState st, Squad sq, Meta meta, Building core, int forced){
        switch(forced){
            case RAID -> {
                int t = chooseMission(st, sq, meta, core);
                if(t == RAID) return RAID;
                Building b = findRaidTarget(st, sq);
                if(b != null){
                    meta.mission = 1; meta.raidPos = b.pos(); meta.raidUntil = Time.time + 60f * 40f;
                    st.claims.add(b.pos());
                    for(int i = 0; i < sq.units.size; i++) if(sq.units.get(i).controller() instanceof TacticalAI ta){ ta.raidPos = b.pos(); ta.raidUntil = meta.raidUntil; }
                    return RAID;
                }
                return core == null ? DEFEND : ADVANCE;
            }
            case RETREAT, DEFEND, HOLD -> { return forced; }
            case PINCER -> { return core == null ? DEFEND : PINCER; }
            default -> { return core == null ? DEFEND : ADVANCE; }
        }
    }

    static final int[] vote = new int[9];

    /** ADVANCE / RAID / PINCER / DEFEND for a squad that currently sees no enemy */
    static int chooseMission(TeamState st, Squad sq, Meta meta, Building core){
        //a running raid continues while its target is alive (the mission is carried by the units, squads regroup constantly)
        {
            int[] votes = null; int best = -1, bestN = 0;
            for(int i = 0; i < sq.units.size; i++){
                if(!(sq.units.get(i).controller() instanceof TacticalAI ta) || ta.raidPos < 0 || Time.time >= ta.raidUntil) continue;
                int c = 0;
                for(int j = 0; j < sq.units.size; j++) if(sq.units.get(j).controller() instanceof TacticalAI tb && tb.raidPos == ta.raidPos) c++;
                if(c > bestN){ bestN = c; best = ta.raidPos; }
            }
            if(best >= 0 && bestN * 2 >= sq.units.size){
                Building rb = Vars.world.build(best);
                if(rb != null && rb.isValid() && rb.team != st.team){
                    meta.mission = 1; meta.raidPos = best;
                    return RAID;
                }
                for(int i = 0; i < sq.units.size; i++) if(sq.units.get(i).controller() instanceof TacticalAI ta && ta.raidPos == best) ta.raidPos = -1;
            }
            meta.mission = 0; meta.raidPos = -1;
        }
        int n = sq.units.size;
        boolean fast = sq.spd >= 0.85f;
        int flyers = 0;
        for(int i = 0; i < n; i++) if(sq.units.get(i).isFlying()) flyers++;
        fast |= flyers * 2 > n;
        //multi-front harassment: fast squads go for unguarded infrastructure while the main body pushes
        if(st.cfg.raids && fast && st.squads.size >= 2 && !(st.weak && st.mine.size < 4)){
            Building t = findRaidTarget(st, sq);
            if(t != null){
                meta.mission = 1; meta.raidPos = t.pos(); meta.raidUntil = Time.time + 60f * 40f;
                st.claims.add(t.pos());
                for(int i = 0; i < sq.units.size; i++) if(sq.units.get(i).controller() instanceof TacticalAI ta){ ta.raidPos = t.pos(); ta.raidUntil = meta.raidUntil; }
                return RAID;
            }
        }
        if(core == null) return DEFEND;
        //early game / outnumbered: hold the base instead of marching into a stronger enemy
        if(st.weak && st.power < st.enemyPower * 0.9f && st.enemyPower > 1f) return DEFEND;
        if(st.squads.size >= 3 && (sq.key & 1) == 1) return PINCER;
        return ADVANCE;
    }

    static final BlockFlag[] raidFlags = {BlockFlag.generator, BlockFlag.battery, BlockFlag.factory, BlockFlag.launchPad};

    static Building findRaidTarget(TeamState st, Squad sq){
        Building best = null;
        float bestD = 1e9f;
        for(BlockFlag flag : raidFlags){
            Seq<Building> list = Vars.indexer.getEnemy(st.team, flag);
            int stride = Math.max(1, list.size / 40);
            for(int i = 0; i < list.size; i += stride){
                Building b = list.get(i);
                if(!b.isValid() || st.claims.contains(b.pos())) continue;
                float d = Mathf.dst2(sq.cx, sq.cy, b.x, b.y);
                if(d >= bestD) continue;
                //skip infrastructure that sits under a turret umbrella unless the squad is strong
                Building guard = Vars.indexer.findEnemyTile(st.team, b.x, b.y, 200f, g -> g.block instanceof Turret);
                if(guard != null && sq.units.size < 10) continue;
                best = b; bestD = d;
            }
        }
        return best;
    }

    // ===================================================================================================
    // roles and orders
    // ===================================================================================================

    static final float[] fVal = new float[40], fInc = new float[40];
    static final int[] fDense = new int[40];

    static void assign(TeamState st, Squad sq, int tactic, float tx, float ty, Teamc fallback, Building core){
        Team team = st.team;
        int n = sq.units.size;
        //vanguard = the sturdiest third (by max health)
        float[] sorted = new float[n];
        for(int i = 0; i < n; i++) sorted[i] = sq.units.get(i).maxHealth;
        Arrays.sort(sorted);
        float vanguardHp = sorted[Math.max(0, n - 1 - n / 3)];
        float ax = tx - sq.cx, ay = ty - sq.cy;
        float al = Math.max(Mathf.len(ax, ay), 1f);
        float dx = ax / al, dy = ay / al;           //squad -> enemy
        float px = -dy, py = dx;                     //perpendicular
        var rally = Vars.state.teams.closestCore(sq.cx, sq.cy, team);

        //focus fire value of every enemy: threat per remaining health, near first; overkill is tracked per enemy
        int fc = Math.min(foes.size, 40);
        for(int i = 0; i < fc; i++){
            Unit e = foes.get(i);
            UnitProfile ep = UnitProfile.of(e.type);
            float h = e.health + e.shield + 1f;
            fVal[i] = (ep.dps + 6f) / h / (1f + Mathf.dst(sq.cx, sq.cy, e.x, e.y) / 380f);
            fInc[i] = 0f;
            int d = 0;
            for(int j = 0; j < fc; j++) if(i != j && foes.get(j).within(e, 70f)) d++;
            fDense[i] = d;
        }

        boolean tankPresent = false;
        for(int i = 0; i < n; i++) if(UnitProfile.of(sq.units.get(i).type).role == UnitProfile.TANK) tankPresent = true;

        for(int i = 0; i < n; i++){
            Unit u = sq.units.get(i);
            if(!(u.controller() instanceof TacticalAI ta)) continue;
            UnitProfile p = UnitProfile.of(u.type);
            //role aware, overkill aware target
            Teamc own = fc > 0 ? pickTarget(u, p, fc) : null;
            boolean cannotHit = fc > 0 && own == null;
            if(own == null) own = fallback != null && fc == 0 && (fallback instanceof Building fb ? fb.isValid() : true) ? fallback : null;
            ta.focus = own;
            boolean support = p.role == UnitProfile.SUPPORT;
            boolean vanguard = u.maxHealth >= vanguardHp && !support && p.role != UnitProfile.SIEGE;
            float range = Math.max(u.range(), 24f);
            boolean roles = st.cfg.roles;

            if(tactic == HOLD){
                //hold position: the unit keeps shooting whatever comes in range
                issuePos(st, u, u.x, u.y);
                continue;
            }
            if(cannotHit && tactic != RETREAT && roles){
                //nothing here this unit can shoot (ground gun vs. air swarm ...): stay behind our own line
                issuePos(st, u, sq.cx - dx * range * 0.8f, sq.cy - dy * range * 0.8f);
                idleUseless++;
                continue;
            }
            if(support && roles && tactic != RETREAT && tactic != RAID){
                //healers stay behind the line, next to the tanks
                issuePos(st, u, sq.cx - dx * range * 0.6f, sq.cy - dy * range * 0.6f);
                supportHolds++;
                continue;
            }
            if(roles && p.role == UnitProfile.SIEGE && own != null && (tactic == ENGAGE || tactic == FLANK || tactic == ADVANCE || tactic == PINCER)){
                //artillery shells the target from the edge of its range, never in front of the line
                float fx = own.getX(), fy = own.getY();
                siegeHolds++;
                float dist = Mathf.dst(u.x, u.y, fx, fy), want = p.range * 0.88f;
                if(dist > want + 12f) issueAttack(st, u, own);
                else issuePos(st, u, u.x + (u.x - fx) / Math.max(dist, 1f) * Math.max(0f, want - dist), u.y + (u.y - fy) / Math.max(dist, 1f) * Math.max(0f, want - dist));
                continue;
            }
            if(roles && p.role == UnitProfile.AA && own == null && tactic != RETREAT && tactic != RAID && tactic != DEFEND){
                //no air in sight: tuck in behind the ground army and wait for flyers
                issuePos(st, u, sq.cx - dx * range * 0.3f, sq.cy - dy * range * 0.3f);
                continue;
            }
            switch(tactic){
                case ENGAGE -> { if(own != null) issueAttack(st, u, own); else issuePos(st, u, tx, ty); }
                case FLANK -> {
                    if(own != null && (u.id & 1) == 0 && !vanguard && p.role != UnitProfile.TANK){
                        float ox = own.getX(), oy = own.getY();
                        float side = (u.id & 2) == 0 ? 1f : -1f;
                        boolean placed = false;
                        for(int att = 0; att < 2 && !placed; att++){
                            float fx = ox + px * side * range * 0.9f - dx * range * 0.25f;
                            float fy = oy + py * side * range * 0.9f - dy * range * 0.25f;
                            //the route has to exist: never flank into a wall
                            if(!Terrain.solid(u, fx, fy) && Terrain.clear(u, u.x, u.y, fx, fy)){
                                issuePos(st, u, fx, fy);
                                placed = true;
                            }else side = -side;
                        }
                        if(!placed){ flankBlocked++; issueAttack(st, u, own); }
                    }else if(own != null) issueAttack(st, u, own);
                    else issuePos(st, u, tx, ty);
                }
                case KITE -> {
                    if(own != null){
                        if(vanguard || p.role == UnitProfile.TANK) issueAttack(st, u, own);
                        else{
                            float fx = own.getX(), fy = own.getY();
                            float dist = Mathf.dst(u.x, u.y, fx, fy);
                            float want = range * 0.85f;
                            //keep the ideal distance band: back off when too close, step in when too far, and stand still inside
                            //the band (a moving destination would make the unit shuffle back and forth around it)
                            float ux = (u.x - fx) / Math.max(dist, 1f), uy = (u.y - fy) / Math.max(dist, 1f);
                            if(dist < want * 0.78f){
                                float kx = fx + ux * want, ky = fy + uy * want;
                                if(Terrain.solid(u, kx, ky)){ kx = u.x + ux * 30f; ky = u.y + uy * 30f; }
                                issuePos(st, u, kx, ky);
                            }else if(dist > want * 1.25f) issueAttack(st, u, own);
                            else issuePos(st, u, u.x, u.y);
                        }
                    }else issuePos(st, u, tx, ty);
                }
                case RETREAT -> {
                    if(rally != null){
                        float a = (u.id * 47f) % 360f, rad = 30f + (u.id % 4) * 12f; //fixed spot per unit: no re-rolled destination
                        issuePos(st, u, rally.x + Angles.trnsx(a, rad), rally.y + Angles.trnsy(a, rad));
                    }
                    else issuePos(st, u, sq.cx - dx * 260f, sq.cy - dy * 260f);
                }
                case RAID -> {
                    Building rb = Vars.world.build(sq.meta.raidPos);
                    raidOrders++;
                    if(rb != null) issueAttack(st, u, rb); else issuePos(st, u, tx, ty);
                }
                case ADVANCE -> { if(own != null) issueAttack(st, u, own); else issuePos(st, u, tx, ty); }
                case PINCER -> { if(own != null && own instanceof Unit) issueAttack(st, u, own); else issuePos(st, u, tx, ty); }
                default -> {
                    //defend: hold a ring around the nearest core
                    if(rally != null){
                        float a = (u.id * 47f) % 360f, rad = 60f + (u.id % 5) * 14f;
                        issuePos(st, u, rally.x + Angles.trnsx(a, rad), rally.y + Angles.trnsy(a, rad));
                    }
                }
            }
        }
    }

    /** the enemy this unit should shoot: one it can hit, valuable, not already doomed by the rest of the squad */
    static Teamc pickTarget(Unit u, UnitProfile p, int fc){
        int best = -1;
        float bestScore = -1f;
        for(int i = 0; i < fc; i++){
            Unit e = foes.get(i);
            if(e.isFlying() ? !p.hitsAir : !p.hitsGround) continue;
            float score = fVal[i];
            float health = e.health + e.shield + 1f;
            if(fInc[i] > health * 1.1f) score *= 0.12f;                       //overkill: somebody else finishes it
            if(p.role == UnitProfile.SIEGE) score *= 1f + fDense[i] * 0.35f;   //shell clusters
            if(p.role == UnitProfile.AA && e.isFlying()) score *= 2.5f;
            score /= 1f + Mathf.dst(u.x, u.y, e.x, e.y) / (p.range * 2.2f);
            if(score > bestScore){ bestScore = score; best = i; }
        }
        if(best < 0) return null;
        Unit e = foes.get(best);
        fInc[best] += p.dps * 2.5f * UnitProfile.armorFactor(p.perHit, e.armor);
        return e;
    }

    /** a player's order on a unit must survive the brain: true while the unit is considered hosted by a player */
    static boolean playerHosted(TeamState st, Unit u, CommandAI ai){
        return playerOwned(st, u, ai, st.cmds.get(u.id));
    }

    static boolean playerOwned(TeamState st, Unit u, CommandAI ai, Cmd old){
        if(u.isPlayer()) return true;
        if(st.playerUntil.get(u.id, 0f) > Time.time) return true;
        if(ai.targetPos == null && ai.attackTarget == null) return false;
        if(old != null){
            if(old.kind == 0 && ai.targetPos != null && ai.attackTarget == null && Mathf.within(ai.targetPos.x, ai.targetPos.y, old.x, old.y, 12f)) return false;
            if(old.kind == 0 && ai.targetPos != null && ai.attackTarget == null && ai.targetPos.epsilonEquals(old.x, old.y, 12f)) return false;
            if(old.kind == 1 && ai.attackTarget != null && ai.attackTarget.id() == old.target) return false;
        }
        //a command the brain did not give: the player's order wins for a while
        st.playerUntil.put(u.id, Time.time + 900f);
        return true;
    }

    static void issuePos(TeamState st, Unit u, float x, float y){
        if(!(u.controller() instanceof CommandAI ai)) return;
        Cmd old = st.cmds.get(u.id);
        if(playerOwned(st, u, ai, old)) return;
        //far destinations are snapped to a coarse grid so a big army shares a handful of flow fields
        if(!u.isFlying() && !Mathf.within(u.x, u.y, x, y, 160f)){ x = Terrain.snap(x, 40f); y = Terrain.snap(y, 40f); }
        if(old != null && old.kind == 0 && Mathf.within(old.x, old.y, x, y, 28f) && Time.time - old.time < 180f && (ai.targetPos != null)) return;
        ai.commandPosition(new Vec2(x, y));
        if(old == null) st.cmds.put(u.id, old = new Cmd());
        old.kind = 0; old.x = x; old.y = y; old.target = -1; old.time = Time.time;
    }

    static void issueAttack(TeamState st, Unit u, Teamc t){
        if(!(u.controller() instanceof CommandAI ai)) return;
        Cmd old = st.cmds.get(u.id);
        if(playerOwned(st, u, ai, old)) return;
        int id = t.id();
        if(old != null && old.kind == 1 && old.target == id && ai.attackTarget == t) return;
        ai.commandTarget(t, false);
        if(old == null) st.cmds.put(u.id, old = new Cmd());
        old.kind = 1; old.target = id; old.time = Time.time;
    }

    /** the commander went offline: every unit gets its own controller back and our orders are withdrawn */
    static void release(TeamState st){
        for(Unit u : st.mine){
            if(!u.isValid() || u.isPlayer()) continue;
            if(u.controller() instanceof TacticalAI ta){
                AIController o = ta.original;
                boolean owned = st.playerUntil.get(u.id, 0f) > Time.time;
                if(o != null){
                    if(o instanceof CommandAI oc){
                        oc.clearCommands();
                        if(owned){ if(ta.targetPos != null) oc.targetPos = ta.targetPos; oc.attackTarget = ta.attackTarget; }
                    }
                    u.controller(o);
                }else u.resetController();
            }else if(u.controller() instanceof CommandAI ai){
                Cmd c = st.cmds.get(u.id);
                if(c != null){
                    if(c.kind == 0 && ai.targetPos != null) ai.targetPos = null;
                    if(c.kind == 1) ai.attackTarget = null;
                }
            }
        }
        st.mine.clear();
        st.cmds.clear();
        st.squads.clear();
        st.queue.clear();
        st.meta.clear();
        st.claims.clear();
        st.taken = 0;
        unitCount = 0;
        squadCount = 0;
    }

    public static float microAvgMs(){
        return Micro.avgMs;
    }

    public static String modeName(int m){
        return m < 0 || m >= NAMES.length ? "auto" : NAMES[m];
    }

    public static String summary(Team team){
        TeamState st = states.get(team);
        StringBuilder sb = new StringBuilder();
        sb.append("active=").append(st != null && st.active).append(" units=").append(st == null ? 0 : st.mine.size)
            .append(" squads=").append(st == null ? 0 : st.squads.size).append(" weak=").append(st != null && st.weak).append(" power=").append((int)(st == null ? 0 : st.power)).append(" enemyPower=").append((int)(st == null ? 0 : st.enemyPower)).append(" total=").append(decisionTotal).append(" decisions=");
        for(int i = 0; i < NAMES.length; i++) sb.append(NAMES[i]).append('=').append(decisions[i]).append(' ');
        sb.append("mode=").append(modeName(settings(team).mode)).append(" flips=").append(tacticFlips).append(" hosted=").append(st == null ? 0 : st.hosted).append(" ");
        sb.append("roles[useless=").append(idleUseless).append(" support=").append(supportHolds).append(" siege=").append(siegeHolds).append(" raidOrders=").append(raidOrders).append(" flankBlocked=").append(flankBlocked).append("] ");
        sb.append("micro[dodge=").append(Micro.dodges).append(" kite=").append(Micro.kites).append(" strafe=").append(Micro.strafes)
            .append(" unjam=").append(Micro.unjams).append(" refocus=").append(Micro.refocus).append(" hostedSkips=").append(Micro.hostedSkips).append(" threats=").append(Micro.threats).append(" blockedDodges=").append(Micro.blockedDodges).append("] ");
        sb.append("brain ms last=").append(Strings.fixed(lastMs, 3)).append(" avg=").append(Strings.fixed(avgMs, 3)).append(" max=").append(Strings.fixed(maxMs, 3))
            .append(" micro avg=").append(Strings.fixed(Micro.avgMs, 3));
        return sb.toString();
    }
}
