package astro.content;

import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.blocks.storage.*;

import static astro.content.DetainerType.*;
import static astro.content.DetainerModel.SHOULDER;

/**
 * v1.6: everything about a pilot (a player who possesses the Detainer) and about warping.
 *
 * <p>Two control modes, chosen by the pilot:
 * <ul>
 * <li><b>auto</b>: the pilot only moves and warps. The AI (the Detainer's own logic) uses every other ability.</li>
 * <li><b>pro</b>: the pilot owns the abilities he did not hand back to the AI (a bitmask, {@code Logic.manual}); an owned
 * ability only ever fires on his command, and only when its conditions hold (energy, cooldown, a target that one of the arms
 * can really reach...). Abilities handed to the AI keep working automatically.</li>
 * </ul>
 * When nobody pilots the Detainer the AI owns everything, including the warp.
 */
public final class DetainerCtl{
    private DetainerCtl(){}

    // reason codes shown by the panel (0 = ready)
    public static final int OK = 0, NO_TARGET = 1, COOLDOWN = 2, NO_ENERGY = 3, BUSY = 4, SHIELD_CD = 5, NO_CHARGE = 6, AI_OWNED = 7, NOTHING = 8, OUT_OF_REACH = 9, NO_LICENSE = 10;

    /**
     * Warp permission of the AI. A pilot can always warp (the charge is the only limit). An unpiloted Detainer warps by itself only
     * while the "allow warp" stance is on: the stance is a vanilla command-menu button (select the Detainer, press it), off by default.
     * The permission is kept as a permanent invisible status effect so it survives a change of controller and is saved with the unit;
     * {@link #syncStance} keeps it equal to the stance of the unit's CommandAI.
     */
    public static mindustry.type.StatusEffect license;

    public static boolean licensed(Unit u){
        return license != null && u.hasEffect(license);
    }

    public static void grant(Unit u, boolean on){
        if(license == null) return;
        if(on){ if(!u.hasEffect(license)) u.apply(license, Float.POSITIVE_INFINITY); }
        else u.unapply(license);
    }

    /** server: a toggle of the stance in the command menu changes the permission; a brand new controller gets the stored permission */
    static void syncStance(Unit unit, Logic L){
        var stance = AstroContent.warpStance;
        if(stance == null) return;
        if(!(unit.controller() instanceof mindustry.ai.types.CommandAI ai)){ L.stanceAI = null; return; }
        boolean lic = licensed(unit), has = ai.hasStance(stance);
        if(ai != L.stanceAI){
            L.stanceAI = ai;
            if(has != lic) ai.setStance(stance, lic);
        }else if(has != lic){
            grant(unit, has);
        }
    }

    /** how long (ticks) a command stays valid while the arms / the body get in position */
    static float reqTime(int ab){
        return ab == AB_LANCE || ab == AB_OVERLOAD ? 70f : ab == AB_DRAIN ? 1f : 36f;
    }

    public static boolean owns(Logic L, int ab){
        return (L.manual & (1 << ab)) != 0;
    }

    /** may the AI use this ability right now? (always, unless a pilot owns it) */
    public static boolean aiMay(Unit unit, Logic L, int ab){
        return !unit.isPlayer() || !owns(L, ab);
    }

    // ---------------------------------------------------------------------------------------------------
    // targets
    // ---------------------------------------------------------------------------------------------------

    /** the pilot's aim point (mouse on the desktop, the tapped point on a phone) */
    public static void aimOf(Unit unit, float[] out){
        Player pl = unit.getPlayer();
        if(pl != null){ out[0] = pl.mouseX; out[1] = pl.mouseY; }
        else{ out[0] = unit.aimX; out[1] = unit.aimY; }
    }

    static final float[] aim = new float[2];

    /** the enemy closest to the pilot's aim point that is within range of the unit, else simply the closest enemy in range */
    public static Teamc manualTarget(Unit unit, float range){
        return manualTarget(unit, range, true);
    }

    /** the enemy nearest to the pilot's aim point (else the nearest one in reach); melee commands never pick air units */
    public static Teamc manualTarget(Unit unit, float range, boolean air){
        aimOf(unit, aim);
        Teamc t = Units.closestTarget(unit.team, aim[0], aim[1], 170f,
            u -> u.checkTarget(air, true) && u.team != Team.derelict && u.dst(unit) <= range + u.hitSize / 2f,
            b -> b.team != Team.derelict && !b.block.underBullets && b.dst(unit) <= range + b.hitSize() / 2f);
        if(t == null){
            t = Units.closestTarget(unit.team, unit.x, unit.y, range,
                u -> u.checkTarget(air, true) && u.team != Team.derelict,
                b -> b.team != Team.derelict && !b.block.underBullets);
        }
        return t;
    }

    static boolean grabbable(DetainerType t, Unit unit, Logic L, Unit u){
        return L.energy >= t.grabCost && u.hitSize <= t.maxGrabSize && !u.type.internal && L.thrown.get(u.id, 0f) <= Time.time
            && !t.isHeld(L, u.id) && !t.grabbing(L, u.id);
    }

    // ---------------------------------------------------------------------------------------------------
    // commands (server side; the same entry is used by single player)
    // ---------------------------------------------------------------------------------------------------

    /**
     * A command of the pilot. Returns 0 when it was accepted, else a reason code.
     * kind = warp kind for AB_WARP.
     */
    public static int act(DetainerType t, Unit unit, int ab, float ax, float ay, int kind){
        Logic L = t.logic(unit);
        int r = act0(t, unit, L, ab, ax, ay, kind);
        L.why[ab] = r;
        L.whyT[ab] = 100f;
        return r;
    }

    static int act0(DetainerType t, Unit unit, Logic L, int ab, float ax, float ay, int kind){
        if(unit.dead || !unit.isAdded() || !unit.isPlayer()) return AI_OWNED;
        if(ab < 0 || ab >= AB_N) return NOTHING;
        if(ab != AB_WARP && ab != AB_RELEASE && !owns(L, ab)) return AI_OWNED;
        L.acts++;
        switch(ab){
            case AB_STAB, AB_SLASH, AB_GRAB -> {
                if(ab == AB_GRAB && L.energy < t.grabCost) return NO_ENERGY;
                float range = ab == AB_STAB ? t.stabRange : ab == AB_SLASH ? t.slashRange : t.longGrabRange;
                Teamc tg = manualTarget(unit, range + 50f, false);
                if(tg == null) return NO_TARGET;
                AstroNet.req(unit, ab, tg);
                return OK;
            }
            case AB_ORB -> {
                if(L.orbOn){ AstroNet.orb(unit, false); return OK; }
                if(L.energy < t.orbCost) return NO_ENERGY;
                AstroNet.orb(unit, true);
                return OK;
            }
            case AB_LANCE, AB_OVERLOAD -> {
                float need = ab == AB_LANCE ? t.lanceMin : t.maxEnergy * 0.985f;
                if(L.energy < need) return NO_ENERGY;
                Teamc tg = manualTarget(unit, 520f);
                if(tg == null) return NO_TARGET;
                AstroNet.req(unit, ab, tg);
                return OK;
            }
            case AB_AEGIS -> {
                if(L.aegisOn){
                    L.aegisOn = false; L.aegisManual = 0f; L.aegisHold = 0f;
                    return OK;
                }
                if(L.aegisCd > 0f) return SHIELD_CD;
                if(L.energy < t.aegisMinEnergy) return NO_ENERGY;
                t.raiseAegis(unit, L);
                L.aegisManual = 420f;
                return OK;
            }
            case AB_DRAIN -> {
                if(L.energy >= t.maxEnergy * 0.98f) return NO_ENERGY;
                AstroNet.req(unit, ab, null);
                return OK;
            }
            case AB_RELEASE -> {
                if(L.heldCount() == 0) return NOTHING;
                AstroNet.req(unit, ab, null);
                return OK;
            }
            case AB_WARP -> {
                return requestWarp(t, unit, L, kind, ax, ay);
            }
            default -> { return NOTHING; }
        }
    }

    /** applied on the server and on every client (see AstroNet): a manual command is armed */
    public static void applyReq(DetainerType t, Unit unit, Logic L, int ab, Teamc tg){
        if(ab == AB_RELEASE){
            for(int i = 0; i < 3; i++) t.releasePrisoner(unit, L, i, true);
            return;
        }
        if(ab == AB_DRAIN){
            L.drainBurst = 70f;
            L.drainT = 0f;
            return;
        }
        L.req[ab] = reqTime(ab);
        L.fail[ab] = NOTHING;
        L.reqT = tg;
    }

    public static void applyOrb(Logic L, boolean on){
        L.orbOn = on;
    }

    // ---------------------------------------------------------------------------------------------------
    // per tick
    // ---------------------------------------------------------------------------------------------------

    public static void tick(DetainerType t, Unit unit, Logic L, float dt){
        boolean client = Vars.net != null && Vars.net.client();
        L.warpCd = Math.max(0f, L.warpCd - dt);
        if(!client) syncStance(unit, L);
        L.arrive = Math.max(0f, L.arrive - dt / 26f);
        L.depart = Math.max(0f, L.depart - dt / 18f);
        if(!client) L.warpCharge = Math.min(1f, L.warpCharge + dt / WARP_FILL);
        if(L.warpWind > 0f){
            L.warpWind -= dt;
            if(L.warpWind <= 0f){
                L.warpWind = 0f;
                if(!client) doWarp(t, unit, L);
            }
        }
        if(L.hold > 0f && !client){
            L.hold -= dt;
            if(unit.isPlayer() && !unit.isLocal()){
                unit.set(L.wToX, L.wToY);
                unit.snapInterpolation();
                Player pl = unit.getPlayer();
                if(L.hold < 16f && L.hold + dt >= 16f && pl != null && pl.con != null) Call.setPosition(pl.con, L.wToX, L.wToY);
            }
        }
        L.aegisManual = Math.max(0f, L.aegisManual - dt);
        L.drainBurst = Math.max(0f, L.drainBurst - dt);

        if(!unit.isPlayer()){
            //nobody pilots it: the AI owns every ability, the warp included
            L.orbOn = false;
            for(int a = 0; a < AB_N; a++) L.req[a] = 0f;
            L.reqT = null;
            if(!client && t.aiWarp && L.warpWind <= 0f){
                L.aiWarpCd -= dt;
                if(L.aiWarpCd <= 0f){
                    L.aiWarpCd = 40f;
                    aiWarp(t, unit, L);
                }
            }
            return;
        }
        for(int a = 0; a < AB_N; a++){
            if(L.whyT[a] > 0f) L.whyT[a] -= dt; else L.why[a] = 0;
        }
        if(L.reqT != null && Units.invalidateTarget(L.reqT, unit.team, unit.x, unit.y)) L.reqT = null;
        if(L.orbOn && L.energy < t.orbCost) L.orbOn = false;
    }

    /** runs just before the arm state machines: arms start the strikes the pilot asked for */
    public static void serve(DetainerType t, Unit unit, Logic L, float dt){
        if(!unit.isPlayer()) return;
        for(int ab = AB_STAB; ab <= AB_GRAB; ab++){
            if(L.req[ab] <= 0f) continue;
            if(L.reqT == null){ L.req[ab] = 0f; continue; }
            if(tryStart(t, unit, L, ab, L.reqT)){
                L.req[ab] = 0f;
            }else{
                L.req[ab] -= dt;
                if(L.req[ab] <= 0f){ L.why[ab] = L.fail[ab]; L.whyT[ab] = 90f; }
            }
        }
        //lance / overload requests run out on their own when no claw could fire them
        for(int ab = AB_LANCE; ab <= AB_OVERLOAD; ab++){
            if(L.req[ab] > 0f){
                L.req[ab] -= dt;
                if(L.req[ab] <= 0f){ L.why[ab] = OUT_OF_REACH; L.whyT[ab] = 90f; }
            }
        }
    }

    /** start a stab / slash / grab with a free arm that can really reach the target; records why not otherwise */
    static boolean tryStart(DetainerType t, Unit unit, Logic L, int ab, Posc tg){
        float size = tg instanceof Hitboxc h ? h.hitSize() : 16f;
        float d = Mathf.dst(unit.x, unit.y, tg.getX(), tg.getY());
        boolean anyFree = false, anyReady = false;
        int best = -1;
        float bd = 1e18f;
        boolean far = false;
        for(int i = 0; i < 3; i++){
            if(!t.free(L, i)) continue;
            anyFree = true;
            if(ab == AB_GRAB ? L.grabCd[i] > 0f : L.cd[i] > 0f) continue;
            anyReady = true;
            boolean ok;
            if(ab == AB_STAB) ok = t.inZone(unit, i, tg, 0);
            else if(ab == AB_SLASH) ok = d <= t.slashRange + size * 0.4f && t.inZone(unit, i, tg, 1);
            else{
                if(!(tg instanceof Unit u) || !grabbable(t, unit, L, u)){
                    L.fail[ab] = L.energy < t.grabCost ? NO_ENERGY : OUT_OF_REACH;
                    return false;
                }
                ok = d <= t.grabRange ? t.inZone(unit, i, u, 2) : d <= t.longGrabRange && t.inZone(unit, i, u, 3);
            }
            if(!ok) continue;
            float[] s = SHOULDER[i];
            t.toWorld(unit, s[0], s[1], s[2], t.p3);
            float dd = Mathf.dst2(t.p3[0], t.p3[1], tg.getX(), tg.getY());
            if(dd < bd){ bd = dd; best = i; far = d > t.grabRange; }
        }
        if(best == -1){
            L.fail[ab] = !anyFree ? BUSY : !anyReady ? COOLDOWN : OUT_OF_REACH;
            return false;
        }
        int i = best;
        L.atkId[i] = tg instanceof Unit u ? u.id : -1;
        L.atkBuild[i] = tg instanceof Building b ? b : null;
        if(ab == AB_GRAB){
            L.longGrab[i] = far;
            if(far) L.longGrabs++;
            t.startGrab(unit, L, i, (Unit)tg);
            return true;
        }
        if(ab == AB_SLASH){
            L.sgn[i] = (L.combo[i] + 1 & 2) == 0 ? 1f : -1f;
            t.planSlash(unit, L, i);
            if(!L.slashOk[i]){ L.fail[ab] = OUT_OF_REACH; return false; }
            L.mode[i] = SLASH;
        }else L.mode[i] = STAB;
        L.modeT[i] = 0f; L.hitDone[i] = false; L.hits[i].clear(); L.combo[i]++;
        return true;
    }

    // ---------------------------------------------------------------------------------------------------
    // status for the panel: one code per ability
    // ---------------------------------------------------------------------------------------------------

    /** fills out[0..AB_N-1] with OK or a reason why the ability cannot be used right now */
    public static void states(DetainerType t, Unit unit, Logic L, int[] out){
        Teamc tg = manualTarget(unit, t.longGrabRange + 50f);
        float d = tg == null ? 0f : unit.dst(tg);
        for(int ab = 0; ab < AB_N; ab++){
            int s = OK;
            if(ab != AB_WARP && ab != AB_RELEASE && !owns(L, ab)) s = AI_OWNED;
            else switch(ab){
                case AB_STAB, AB_SLASH, AB_GRAB -> {
                    float range = ab == AB_STAB ? t.stabRange : ab == AB_SLASH ? t.slashRange : t.longGrabRange;
                    if(ab == AB_GRAB && L.energy < t.grabCost) s = NO_ENERGY;
                    else if(tg == null || d > range + 50f) s = NO_TARGET;
                    else{
                        boolean free = false, ready = false, reach = false;
                        for(int i = 0; i < 3; i++){
                            if(!t.free(L, i)) continue;
                            free = true;
                            if(ab == AB_GRAB ? L.grabCd[i] > 0f : L.cd[i] > 0f) continue;
                            ready = true;
                            if(ab == AB_STAB ? t.inZone(unit, i, tg, 0) : ab == AB_SLASH ? t.inZone(unit, i, tg, 1)
                                : tg instanceof Unit u && grabbable(t, unit, L, u) && (d <= t.grabRange ? t.inZone(unit, i, u, 2) : t.inZone(unit, i, u, 3))) reach = true;
                        }
                        s = !free ? BUSY : !ready ? COOLDOWN : !reach ? OUT_OF_REACH : OK;
                    }
                }
                case AB_ORB -> s = L.orbOn ? OK : L.energy < t.orbCost ? NO_ENERGY : OK;
                case AB_LANCE -> s = L.energy < t.lanceMin ? NO_ENERGY : tg == null ? NO_TARGET : OK;
                case AB_OVERLOAD -> s = L.energy < t.maxEnergy * 0.985f ? NO_ENERGY : tg == null ? NO_TARGET : OK;
                case AB_AEGIS -> s = L.aegisOn ? OK : L.aegisCd > 0f ? SHIELD_CD : L.energy < t.aegisMinEnergy ? NO_ENERGY : OK;
                case AB_DRAIN -> s = L.energy >= t.maxEnergy * 0.98f ? NO_ENERGY : OK;
                case AB_RELEASE -> s = L.heldCount() == 0 ? NOTHING : OK;
                case AB_WARP -> s = L.warpCd > 0f || L.warpWind > 0f ? COOLDOWN : OK;
                default -> {}
            }
            out[ab] = s;
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // warp
    // ---------------------------------------------------------------------------------------------------

    public static float worldDiag(){
        if(Vars.world == null || Vars.world.width() <= 0) return 6000f;
        return Mathf.len(Vars.world.unitWidth(), Vars.world.unitHeight());
    }

    /** share of a full charge a jump of this length needs: short hops are cheap, crossing the whole map needs a full charge */
    public static float warpCost(float dist){
        return Mathf.clamp(0.16f + 0.84f * dist / Math.max(worldDiag() * 0.55f, 1f), 0.16f, 1f);
    }

    static final float[] wd = new float[2];
    static Teamc warpTarget;

    /** where a jump of the given kind would land (written to wd), or false when there is nowhere to go */
    static boolean warpDest(DetainerType t, Unit unit, int kind, float ax, float ay){
        warpTarget = null;
        float margin = 28f;
        float maxX = Vars.world.unitWidth() - margin, maxY = Vars.world.unitHeight() - margin;
        if(kind == WARP_AIM){
            wd[0] = Mathf.clamp(ax, margin, maxX);
            wd[1] = Mathf.clamp(ay, margin, maxY);
            return true;
        }
        float tx, ty, stand;
        if(kind == WARP_ENEMY){
            Teamc tg = Units.closestTarget(unit.team, ax, ay, 420f,
                u -> u.checkTarget(true, true) && u.team != Team.derelict,
                b -> b.team != Team.derelict && !b.block.underBullets);
            if(tg == null) tg = Units.closestTarget(unit.team, unit.x, unit.y, 1e6f, u -> u.checkTarget(true, true) && u.team != Team.derelict, b -> false);
            if(tg == null) tg = Vars.indexer.findEnemyTile(unit.team, unit.x, unit.y, 12000f, b -> b.team != Team.derelict && !b.block.underBullets);
            if(tg == null) return false;
            warpTarget = tg;
            tx = tg.getX(); ty = tg.getY();
            stand = 62f + (tg instanceof Hitboxc h ? Math.min(h.hitSize(), 60f) * 0.35f : 10f);
        }else{
            CoreBlock.CoreBuild core = Vars.state.teams.closestCore(unit.x, unit.y, unit.team);
            if(core == null) return false;
            tx = core.x; ty = core.y;
            stand = 70f + core.hitSize() * 0.5f;
        }
        //land on the side the Detainer comes from
        float ang = Angles.angle(tx, ty, unit.x, unit.y);
        if(Mathf.dst(tx, ty, unit.x, unit.y) < stand) ang = unit.rotation + 180f;
        wd[0] = Mathf.clamp(tx + Angles.trnsx(ang, stand), margin, maxX);
        wd[1] = Mathf.clamp(ty + Angles.trnsy(ang, stand), margin, maxY);
        return true;
    }

    public static int requestWarp(DetainerType t, Unit unit, Logic L, int kind, float ax, float ay){
        if(Vars.net != null && Vars.net.client()) return OK;
        if(unit.dead || !unit.isAdded()) return AI_OWNED;
        if(!unit.isPlayer() && !licensed(unit)) return NO_LICENSE;
        if(L.warpWind > 0f) return BUSY;
        if(L.warpCd > 0f) return COOLDOWN;
        if(!warpDest(t, unit, kind, ax, ay)) return NO_TARGET;
        float dist = Mathf.dst(unit.x, unit.y, wd[0], wd[1]);
        if(dist < 60f) return OUT_OF_REACH;
        if(L.warpCharge < warpCost(dist)) return NO_CHARGE;
        L.warpKind = kind; L.warpAx = ax; L.warpAy = ay;
        L.warpWind = WARP_WIND;
        AstroNet.warpStart(unit);
        return OK;
    }

    /** the jump itself (server): leave, land, blast */
    static void doWarp(DetainerType t, Unit unit, Logic L){
        if(unit.dead || !unit.isAdded()) return;
        if(!warpDest(t, unit, L.warpKind, L.warpAx, L.warpAy)){ L.warpCd = 20f; return; }
        float fx = unit.x, fy = unit.y, tx = wd[0], ty = wd[1];
        Teamc tg = warpTarget;
        float dist = Mathf.dst(fx, fy, tx, ty);
        float cost = warpCost(dist);
        if(L.warpCharge < cost * 0.98f){ L.warpCd = 20f; return; }
        L.warpCharge = Math.max(0f, L.warpCharge - cost);
        L.warpCd = 40f;
        L.warps++;
        L.lastWarpDist = dist;
        for(int i = 0; i < 3; i++){
            t.releasePrisoner(unit, L, i, false);
            if(L.mode[i] != IDLE) t.abortStrike(L, i);
            L.mode[i] = IDLE;
        }
        L.wToX = tx; L.wToY = ty;
        AstroNet.warpGo(unit, fx, fy, tx, ty);
        unit.set(tx, ty);
        unit.vel.scl(0.25f);
        Player pl = unit.getPlayer();
        if(pl != null){
            //a remote pilot's position is driven by his client's snapshots: move the interpolator too, tell his client,
            //and hold the spot for a moment so that snapshots that were already on their way cannot pull the unit back
            pl.set(tx, ty);
            unit.snapInterpolation();
            if(pl.con != null) Call.setPosition(pl.con, tx, ty);
            L.hold = 30f;
        }
        if(tg != null) unit.rotation = Angles.angle(tx, ty, tg.getX(), tg.getY());
        L.faceTarget = null;
        L.faceT = 0f;
        blast(t, unit, tx, ty);
    }

    /** the air blast of the arrival: damages and shoves away whatever stands next to the landing spot */
    static void blast(DetainerType t, Unit unit, float x, float y){
        float R = t.warpBlastRadius;
        Damage.damage(unit.team, x, y, R, t.warpBlastDamage, true, true, true);
        Units.nearbyEnemies(unit.team, x - R, y - R, R * 2f, R * 2f, u -> {
            float d = Mathf.dst(x, y, u.x, u.y);
            if(d > R || u.dead) return;
            float k = (1f - d / R) * 9f / Math.max(u.hitSize / 24f, 1f);
            u.impulse(Tmp.v1.set(u.x - x, u.y - y).nor().scl(k * u.mass() * 0.35f));
        });
        Effect.shake(6f, 18f, x, y);
    }

    // ---------------------------------------------------------------------------------------------------
    // the AI: warps next to the enemy when it is far away, and home when it is nearly dead
    // ---------------------------------------------------------------------------------------------------

    static void aiWarp(DetainerType t, Unit unit, Logic L){
        if(!licensed(unit)) return;
        if(L.warpCd > 0f || L.warpCharge < 0.3f || Vars.world == null || Vars.world.width() <= 0) return;
        Teamc near = Units.closestTarget(unit.team, unit.x, unit.y, 380f, u -> u.checkTarget(true, true) && u.team != Team.derelict, b -> false);
        CoreBlock.CoreBuild core = Vars.state.teams.closestCore(unit.x, unit.y, unit.team);
        if(near != null && unit.health < unit.maxHealth * 0.28f && core != null && core.dst(unit) > 560f){
            if(requestWarp(t, unit, L, WARP_HOME, 0f, 0f) == OK){ L.aiWarpCd = 900f; L.aiWarps++; return; }
        }
        if(unit.health < unit.maxHealth * 0.4f || near != null) return;
        Teamc foe = Units.closestTarget(unit.team, unit.x, unit.y, 1e6f, u -> u.checkTarget(true, true) && u.team != Team.derelict, b -> false);
        if(foe == null) foe = Vars.indexer.findEnemyTile(unit.team, unit.x, unit.y, 12000f, b -> b.team != Team.derelict && !b.block.underBullets);
        if(foe == null) return;
        if(unit.dst(foe) < 560f) return;
        if(requestWarp(t, unit, L, WARP_ENEMY, foe.getX(), foe.getY()) == OK){
            L.aiWarpCd = 360f + Mathf.random(120f);
            L.aiWarps++;
        }
    }
}
