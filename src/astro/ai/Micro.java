package astro.ai;

import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.gen.*;

import astro.ai.TacticalBrain.TeamState;

/**
 * Reflex layer of the tactical brain. The probabilistic planner decides what a squad wants every 10-45 ticks; this
 * layer runs every tick on a round-robin slice of units (time budgeted) and handles what cannot wait: instant
 * retargeting when the target dies or something better/closer appears, dodging bullets that will hit, backing away
 * from an enemy that got too close while the unit outranges it (kiting), light strafing while shooting, and unjamming
 * units that are stuck in a corridor. Every decision is a few ticks of steering that ends by itself.
 */
public final class Micro{
    private Micro(){}

    public static boolean strafeOn = true, dodgeOn = true, kiteOn = true;
    static int dodges, kites, strafes, unjams, refocus, visits, hostedSkips, threats, blockedDodges;
    static float ms, avgMs;

    // working state of the bullet scan (static to avoid lambda garbage)
    private static Unit cur;
    private static float bestT, bdx, bdy, bestDmg;
    private static final float scanR = 64f;

    private static final arc.func.Cons<Bullet> bulletScan = b -> {
        Unit u = cur;
        if(b.team == u.team || b.type == null) return;
        float sp = b.vel.len();
        if(sp < 0.6f) return;
        boolean air = u.isFlying();
        if(air ? !b.type.collidesAir : !b.type.collidesGround) return;
        if(b.type.damage + b.type.splashDamage < Math.min(u.maxHealth * 0.003f, 4f) + 3f) return;
        float rx = u.x - b.x, ry = u.y - b.y;
        float v2 = sp * sp;
        float t = (rx * b.vel.x + ry * b.vel.y) / v2;
        if(t <= 0f || t > 22f ) return;
        float cx = rx - b.vel.x * t, cy = ry - b.vel.y * t; //offset of the unit from the bullet path at closest approach
        float hit = u.hitSize * 0.5f + Math.max(b.type.hitSize, b.type.splashDamageRadius * 0.4f) * 0.5f + 3f;
        if(cx * cx + cy * cy > hit * hit) return;
        threats++;
        if(t < bestT){
            bestT = t;
            bestDmg = b.type.damage + b.type.splashDamage;
            //sidestep perpendicular to the bullet, away from its path
            float px = -b.vel.y / sp, py = b.vel.x / sp;
            float side = cx * px + cy * py >= 0f ? 1f : -1f;
            bdx = px * side; bdy = py * side;
        }
    };

    static void run(TeamState st, long t0, float budgetNs){
        int n = st.mine.size;
        if(n == 0 || !st.cfg.micro) return;
        int per = Math.min(n, Math.max(12, n / 6));
        for(int k = 0; k < per; k++){
            if(Time.nanos() - t0 > budgetNs) break;
            if(st.microCursor >= n) st.microCursor = 0;
            Unit u = st.mine.get(st.microCursor++);
            visits++;
            think(st, u);
        }
    }

    static void think(TeamState st, Unit u){
        if(!u.isValid() || u.isPlayer() || !(u.controller() instanceof TacticalAI ta)) return;
        if(TacticalBrain.playerHosted(st, u, ta)){ hostedSkips++; return; }
        UnitProfile p = UnitProfile.of(u.type);
        float now = Time.time;
        float dt = Math.min(now - ta.lastMicro, 30f);
        ta.lastMicro = now;

        // ---- dodge (highest priority): only light units with some speed
        boolean light = u.hitSize <= 26f && p.role != UnitProfile.TANK && p.role != UnitProfile.SIEGE && p.speed > 0.35f;
        if(dodgeOn && light && Groups.bullet.size() > 0){
            cur = u; bestT = 1e9f; bdx = 0; bdy = 0; bestDmg = 0f;
            Groups.bullet.intersect(u.x - scanR, u.y - scanR, scanR * 2f, scanR * 2f, bulletScan);
            boolean threat = bestT < 1e8f;
            //a unit that just sidestepped keeps going (and keeps shooting) instead of twitching left and right at every
            //bullet: only a hit that would really hurt (a quarter of its health) may interrupt the dodge rhythm
            boolean big = bestDmg >= (u.health + u.shield) * 0.25f;
            if(threat && (now >= ta.dodgeReady || big)){
                float dur = Mathf.clamp(bestT + 5f, 9f, 24f);
                if(!Terrain.stepFree(u, bdx, bdy, 16f) && !Terrain.stepFree(u, -bdx, -bdy, 16f)) blockedDodges++;
                else{
                    if(Terrain.stepFree(u, bdx, bdy, 16f)) ta.micro(bdx, bdy, dur, 1);
                    else ta.micro(-bdx, -bdy, dur, 1);
                    ta.dodgeReady = now + dur + 30f;
                    dodges++;
                    return;
                }
            }
        }

        // ---- retarget instantly when the current target is gone (do not wait for the planner)
        Teamc f = ta.focus;
        boolean focusBad = f == null || (f instanceof Healthc h && !h.isValid()) || !f.isAdded()
            || (f instanceof Unit fu && !p.hitsAir && fu.isFlying()) || (f instanceof Unit fu2 && !p.hitsGround && fu2.isGrounded());
        if(focusBad && now - ta.lastScan > 8f){
            ta.lastScan = now;
            Teamc best = pick(u, p);
            if(best != null){
                ta.focus = best;
                ta.setMainTarget(best);
                refocus++;
                f = best;
            }
        }

        // ---- stuck detection: a unit that wants to move but does not get anywhere in a corridor
        if(now >= ta.nextCheck){
            boolean wants = ta.targetPos != null && !u.isFlying();
            float moved = Mathf.dst(u.x, u.y, ta.lastX, ta.lastY);
            ta.lastX = u.x; ta.lastY = u.y; ta.nextCheck = now + 30f;
            boolean inRange = f != null && u.within(f, p.range * 0.95f);
            if(wants && !inRange && moved < 2.5f && p.speed > 0.3f){
                if(++ta.stuck >= 3){
                    ta.stuck = 0;
                    unjam(u, ta);
                    unjams++;
                    return;
                }
            }else ta.stuck = 0;
        }

        if(!(f instanceof Unit tu) || !tu.isValid()) return;
        float dist = u.dst(tu);
        UnitProfile tp = UnitProfile.of(tu.type);

        // ---- kiting: we outrange an enemy that has closed in and is not faster than us
        boolean outranges = p.range > tp.range * 1.08f && tp.speed <= p.speed * 1.2f + 0.05f;
        boolean canKite = p.role == UnitProfile.SIEGE ? dist < p.range * 0.45f : p.role != UnitProfile.TANK && outranges;
        float kiteGap = Math.max(p.range * 0.55f, tp.range * 1.05f + 8f);
        //hysteresis: once backing off, keep going until the gap is clearly comfortable (no back-and-forth at the threshold)
        if(ta.kiting && dist > kiteGap * 1.3f) ta.kiting = false;
        if(kiteOn && canKite && (dist < kiteGap || ta.kiting && dist < kiteGap * 1.3f) && p.speed > 0.2f){
            ta.kiting = true;
            float dx = u.x - tu.x, dy = u.y - tu.y;
            if(Terrain.stepFree(u, dx, dy, 18f)){ ta.micro(dx, dy, 12f, 2); kites++; return; }
            //pinned against a wall: slide along it
            if(Terrain.stepFree(u, -dy, dx, 18f)){ ta.micro(-dy, dx, 12f, 2); kites++; return; }
            if(Terrain.stepFree(u, dy, -dx, 18f)){ ta.micro(dy, -dx, 12f, 2); kites++; return; }
        }

        // ---- strafe: light shooters inside their range circle the target instead of standing still.
        // Hysteresis on both ends (enter < 0.85 range, leave > 1.0 range) and a smooth radial correction keep the unit on one
        // steady circle: the old band edges flipped it between "circle" and "walk in" every few ticks (shaking).
        boolean canStrafe = strafeOn && light && p.role != UnitProfile.SUPPORT && p.dps > 12f && dist > 18f + tu.hitSize * 0.5f;
        if(ta.strafing ? dist > p.range * 1.0f : dist > p.range * 0.85f) ta.strafing = false;
        else if(canStrafe) ta.strafing = true;
        if(ta.strafing && canStrafe){
            if(now > ta.strafeUntil){
                //circling direction is kept for several seconds; every unit starts with its own fixed side
                boolean first = ta.strafeUntil == 0f;
                ta.strafeUntil = now + 240f + Mathf.random(200f);
                if(first) ta.strafeSign = (u.id & 1) == 0 ? 1f : -1f;
                else if(Mathf.chance(0.2)) ta.strafeSign = -ta.strafeSign;
            }
            float dx = tu.x - u.x, dy = tu.y - u.y;
            float inv = 1f / Math.max(dist, 1f);
            float sx = -dy * inv * ta.strafeSign, sy = dx * inv * ta.strafeSign;
            //continuous correction towards a circle of 0.68 range (no dead band edges)
            float hold = Mathf.clamp((dist - p.range * 0.68f) / (p.range * 0.2f), -1f, 1f) * 0.6f;
            sx += dx * inv * hold; sy += dy * inv * hold;
            if(Terrain.stepFree(u, sx, sy, 14f)){ ta.micro(sx, sy, 10f, 3); strafes++; }
            else if(Terrain.stepFree(u, -sx, -sy, 14f)){
                //blocked on this side: turn around ONCE and keep the new side for a good while (no flipping at every step)
                ta.strafeSign = -ta.strafeSign;
                ta.strafeUntil = now + 150f + Mathf.random(90f);
            }else ta.strafeUntil = now + 40f; //walled in on both sides: stand and shoot instead of shaking
        }
    }

    /** nearest hittable target, preferring weak and dangerous ones (kill securing) */
    static Teamc pick(Unit u, UnitProfile p){
        float r = Math.max(p.range * 1.15f, 60f);
        Teamc best = Units.bestTarget(u.team, u.x, u.y, r, e -> e.checkTarget(p.hitsAir, p.hitsGround) && !e.type.internal, b -> false,
            (e, x, y) -> {
                float ehp = Math.max(e.health + e.shield, 1f);
                UnitProfile ep = UnitProfile.of(e.type);
                //cheap to kill and dangerous first, distance as tie breaker
                return Mathf.dst2(x, y, e.x, e.y) * (0.6f + 0.4f * Mathf.clamp(ehp / 900f, 0f, 4f)) / (1f + ep.dps / 40f);
            });
        if(best != null) return best;
        return Units.closestTarget(u.team, u.x, u.y, r, e -> false, b -> p.hitsGround);
    }

    static void unjam(Unit u, TacticalAI ta){
        //try the eight directions starting from a random one, keep the first free for ~5 tiles
        float a0 = Mathf.random(360f);
        for(int k = 0; k < 8; k++){
            float a = a0 + k * 45f;
            float dx = Angles.trnsx(a, 1f), dy = Angles.trnsy(a, 1f);
            if(Terrain.stepFree(u, dx, dy, 40f)){
                ta.micro(dx, dy, 55f, 4);
                return;
            }
        }
    }
}
