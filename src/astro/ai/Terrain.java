package astro.ai;

import arc.math.*;
import mindustry.*;
import mindustry.gen.*;

/** Cheap terrain queries for the brain: straight-line clearance, local openness and target quantising. */
final class Terrain{
    private Terrain(){}

    static boolean solid(Unit u, float wx, float wy){
        if(u.isFlying()) return false;
        int tx = Mathf.round(wx / 8f), ty = Mathf.round(wy / 8f);
        if(Vars.world.tile(tx, ty) == null) return true;
        return !u.canPass(tx, ty);
    }

    /** true when a straight walk from A to B (sampled every 6 units) never enters a cell the unit cannot pass */
    static boolean clear(Unit u, float x0, float y0, float x1, float y1){
        if(u.isFlying()) return true;
        float dx = x1 - x0, dy = y1 - y0, len = Mathf.len(dx, dy);
        int steps = Math.min(Math.max(1, (int)(len / 6f)), 90);
        for(int i = 1; i <= steps; i++){
            float f = i / (float)steps;
            if(solid(u, x0 + dx * f, y0 + dy * f)) return false;
        }
        return true;
    }

    /** can the unit take a step of {@code dist} in direction (dx,dy) without hitting anything */
    static boolean stepFree(Unit u, float dx, float dy, float dist){
        if(u.isFlying()) return true;
        float l = Math.max(Mathf.len(dx, dy), 0.0001f);
        float nx = dx / l, ny = dy / l;
        for(float d = 6f; d <= dist + 0.01f; d += 6f){
            if(solid(u, u.x + nx * d, u.y + ny * d)) return false;
        }
        return !solid(u, u.x + nx * dist, u.y + ny * dist);
    }

    /** 1 = wide open, 0 = squeezed in a corridor: fraction of 8 directions free for 5 tiles */
    static float openness(Unit u, float cx, float cy){
        if(u.isFlying()) return 1f;
        int free = 0;
        for(int k = 0; k < 8; k++){
            float a = k * 45f;
            boolean ok = true;
            for(float d = 10f; d <= 44f; d += 10f){
                if(solid(u, cx + Angles.trnsx(a, d), cy + Angles.trnsy(a, d))){ ok = false; break; }
            }
            if(ok) free++;
        }
        return free / 8f;
    }

    /** units sent to (almost) the same place share one flow field: snap far destinations to a coarse grid */
    static float snap(float v, float g){
        return Math.round(v / g) * g;
    }
}
