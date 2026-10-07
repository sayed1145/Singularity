package blackhole.g3d;

import arc.*;

/**
 * Per-frame live-geometry budget shared by every 3D block and unit.
 *
 * <p>Everything static is baked, so a model's live cost is a few dozen quads. Still, a late-game base can put
 * hundreds of animated machines and a swarm of units on screen at once; to keep the frame time bounded on
 * phones, each model asks the budget before drawing its live parts. When the frame's budget is spent (or the
 * camera is zoomed far out) the model falls back to its baked rest-pose sprite (1-3 texture draws), which is
 * visually identical at that zoom. Nearby / first-drawn objects keep full animation.
 */
public final class Budget{
    /** live quads per frame before models fall back to baked sprites (desktop and phone alike) */
    public static int maxQuads = 14000;
    /** below this many screen pixels per world unit, live parts are skipped entirely (far zoom) */
    public static float minPpu = 1.35f;
    public static long frame = -1;
    public static int used, fallbacks;
    /** statistics of the last completed frame */
    public static int lastUsed, lastFallbacks;

    private Budget(){}

    private static void roll(){
        long f = Core.graphics == null ? frame : Core.graphics.getFrameId();
        if(f != frame){
            frame = f;
            lastUsed = used;
            lastFallbacks = fallbacks;
            used = 0;
            fallbacks = 0;
        }
    }

    /** Books quads that are drawn unconditionally (turret heads: they are the silhouette, never dropped). */
    public static void spend(int quads){
        roll();
        used += quads;
    }

    /** Reserves {@code quads} live quads for this frame. False = draw the baked fallback instead. */
    public static boolean take(int quads){
        roll();
        if(BlockModel.pixelsPerUnit() < minPpu || used + quads > maxQuads){
            fallbacks++;
            return false;
        }
        used += quads;
        return true;
    }
}
