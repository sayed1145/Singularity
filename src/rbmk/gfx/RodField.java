package rbmk.gfx;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;

import java.util.*;

/**
 * The 709 channel columns of the core as real 3D cuboids.
 *
 * <p>Lattice: the reactor-audio-visualizer rule x,z in [-15,15], hypot <= 15 (exactly 709 cells), spread over
 * the whole well with a 1.445 unit pitch and 1.0 unit columns, i.e. a clean 0.445 unit gap everywhere.
 * The 211 CPS rods are chosen by deterministic farthest-point sampling, so they are evenly scattered over the
 * entire disc instead of forming stripes or clumps; the SAR/ER/AC/MR classes are interleaved along that
 * sequence so every class is itself evenly spread.
 *
 * <p>Per frame each column costs 4 projected corners and 3-4 {@code Fill.quad}s (top, front, one side, cap).
 * Positions, bottom corners, neighbour weights and the exact back-to-front order are computed once.
 */
public final class RodField{
    public static final int channels = 709, controls = 211;
    public static final float pitch = 1.445f, hw = 0.5f, floorZ = 2.4f, maxH = 3.7f;

    public static final float[] cx = new float[channels], cy = new float[channels];
    /** CPS index for a channel, or -1 for a fuel/pressure channel */
    public static final int[] control = new int[channels];
    /** channel of each CPS rod */
    public static final int[] channelOf = new int[controls];
    static final int[] order = new int[channels];
    static final float[] bottom = new float[channels * 8];
    static final int[] nb = new int[channels * 3];
    static final float[] nw = new float[channels * 3];
    static final float[] prof = new float[channels], ax = new float[channels], ay = new float[channels], phase = new float[channels], freq = new float[channels];
    private static boolean ready;

    private RodField(){}

    public static synchronized void init(Cam cam){
        if(ready) return;
        int n = 0;
        for(int x = -15; x <= 15; x++){
            for(int z = -15; z <= 15; z++){
                if(Math.sqrt(x * x + z * z) > 15.0) continue;
                cx[n] = x * pitch; cy[n] = z * pitch;
                n++;
            }
        }
        if(n != channels) throw new IllegalStateException("lattice must contain 709 channels, got " + n);

        //farthest point sampling for 211 evenly scattered CPS rods (deterministic)
        Arrays.fill(control, -1);
        float[] best = new float[channels];
        Arrays.fill(best, Float.MAX_VALUE);
        boolean[] picked = new boolean[channels];
        int[] seq = new int[controls];
        int cur = channels / 2; //the centre cell
        for(int k = 0; k < controls; k++){
            seq[k] = cur; picked[cur] = true;
            int next = -1; float far = -1f;
            for(int i = 0; i < channels; i++){
                if(picked[i]) continue;
                float dx = cx[i] - cx[cur], dy = cy[i] - cy[cur];
                float d = dx * dx + dy * dy;
                if(d < best[i]) best[i] = d;
                //tiny radial bias keeps a slightly lower density at the rim, like a real CPS map
                float score = best[i] * (1f - 0.06f * (cx[i] * cx[i] + cy[i] * cy[i]) / (22f * 22f)) + (i * 7919 % 101) * 1e-5f;
                if(score > far){ far = score; next = i; }
            }
            cur = next;
        }
        //interleave classes SAR 24 / ER 24 / AC 24 / MR 139 along the sampling sequence
        int[] quota = {24, 24, 24, 139}, start = {0, 24, 48, 72}, used = new int[4];
        for(int k = 0; k < controls; k++){
            int cls = 3; float deficit = -1e9f;
            for(int c = 0; c < 4; c++){
                if(used[c] >= quota[c]) continue;
                float d = quota[c] * (k + 1f) / controls - used[c];
                if(d > deficit){ deficit = d; cls = c; }
            }
            int rod = start[cls] + used[cls]++;
            control[seq[k]] = rod;
            channelOf[rod] = seq[k];
        }

        //three nearest CPS rods per fuel channel, inverse distance weights
        for(int i = 0; i < channels; i++){
            float d0 = 1e9f, d1 = 1e9f, d2 = 1e9f; int i0 = 0, i1 = 0, i2 = 0;
            for(int r = 0; r < controls; r++){
                int c = channelOf[r];
                float dx = cx[c] - cx[i], dy = cy[c] - cy[i];
                float d = dx * dx + dy * dy + 0.5f;
                if(d < d0){ d2 = d1; i2 = i1; d1 = d0; i1 = i0; d0 = d; i0 = r; }
                else if(d < d1){ d2 = d1; i2 = i1; d1 = d; i1 = r; }
                else if(d < d2){ d2 = d; i2 = r; }
            }
            float w0 = 1f / d0, w1 = 1f / d1, w2 = 1f / d2, ws = w0 + w1 + w2;
            nb[i * 3] = i0; nb[i * 3 + 1] = i1; nb[i * 3 + 2] = i2;
            nw[i * 3] = w0 / ws; nw[i * 3 + 1] = w1 / ws; nw[i * 3 + 2] = w2 / ws;
            float rn = (float)Math.sqrt(cx[i] * cx[i] + cy[i] * cy[i]) / 21.7f;
            prof[i] = 0.42f + 0.58f * (float)Math.cos(rn * 1.22f);
            float ang = (float)Math.atan2(cy[i], cx[i]);
            ax[i] = rn * (float)Math.cos(ang); ay[i] = rn * (float)Math.sin(ang);
            float h = Math.abs((float)Math.sin(cx[i] * 12.9898f + cy[i] * 78.233f) * 43758.547f);
            h -= (float)Math.floor(h);
            phase[i] = h * Mathf.PI2;
            freq[i] = 0.022f + h * 0.026f;
        }

        //exact painter order: farthest from the camera nadir first
        Integer[] idx = new Integer[channels];
        for(int i = 0; i < channels; i++) idx[i] = i;
        final float ncy = cam.cy;
        Arrays.sort(idx, (a, b) -> Float.compare(
            cx[b] * cx[b] + (cy[b] - ncy) * (cy[b] - ncy),
            cx[a] * cx[a] + (cy[a] - ncy) * (cy[a] - ncy)));
        for(int i = 0; i < channels; i++) order[i] = idx[i];

        //bottom corners (never move): fl, fr, bl, br
        for(int i = 0; i < channels; i++){
            int o = i * 8;
            bottom[o] = cam.sx(cx[i] - hw, floorZ); bottom[o + 1] = cam.sy(cy[i] - hw, floorZ);
            bottom[o + 2] = cam.sx(cx[i] + hw, floorZ); bottom[o + 3] = cam.sy(cy[i] - hw, floorZ);
            bottom[o + 4] = cam.sx(cx[i] - hw, floorZ); bottom[o + 5] = cam.sy(cy[i] + hw, floorZ);
            bottom[o + 6] = cam.sx(cx[i] + hw, floorZ); bottom[o + 7] = cam.sy(cy[i] + hw, floorZ);
        }
        ready = true;
    }

    // ------------------------------------------------------------------ per-frame state

    /** heights of the last frame (for tests / inspection) */
    public static final float[] height = new float[channels];
    static final float[] local = new float[channels];

    static final float topShade = Light.diffuse(0, 0, 1) + Light.spec(0, 0, 1, 0.3f);
    static final float frontShade = Light.diffuse(0, -1, 0);
    static final float westShade = Light.diffuse(-1, 0, 0);
    static final float eastShade = Light.diffuse(1, 0, 0);

    /**
     * Computes every column height from the reactor state.
     * @param rods rod withdrawal 0..100 per CPS rod
     */
    public static void simulate(float[] rods, float power, float voidFraction, float time){
        float level = Mathf.clamp(power / 2.3f);
        float tilt = 0.10f + 0.10f * voidFraction;
        float pc = Mathf.cos(time * 0.0045f), ps = Mathf.sin(time * 0.0045f);
        float breathing = 0.035f + 0.09f * level;
        for(int i = 0; i < channels; i++){
            int c = control[i];
            float wob = Mathf.sin(time * freq[i] + phase[i]);
            if(c >= 0){
                float v = rods[c] / 100f;
                height[i] = 0.5f + 2.95f * v + wob * 0.03f;
                local[i] = v;
            }else{
                int o = i * 3;
                float rodAvg = (rods[nb[o]] * nw[o] + rods[nb[o + 1]] * nw[o + 1] + rods[nb[o + 2]] * nw[o + 2]) / 100f;
                float azim = 1f + tilt * (ax[i] * pc + ay[i] * ps);
                float l = level * 1.75f * prof[i] * azim * (0.45f + 0.55f * rodAvg);
                local[i] = l;
                height[i] = 0.26f + 1.2f * Math.min(l, 1.5f) + wob * breathing;
            }
        }
    }

    /**
     * Draws all columns. selected = highlighted CPS rod (-1 none).
     * @param lod 2 = full detail, 1 = no caps, 0 = tops and fronts only (far zoom)
     */
    public static void draw(Cam cam, float wx, float wy, int selected, float danger, float time, int lod){
        float D = cam.D, ncy = cam.cy;
        float pulse = 0.5f + 0.5f * Mathf.sin(time * 0.16f);
        for(int oi = 0; oi < channels; oi++){
            int i = order[oi];
            int c = control[i];
            float x = cx[i], y = cy[i];
            float zt = floorZ + height[i];
            float s = D / (D - zt);
            float xl = wx + (x - hw) * s, xr = wx + (x + hw) * s;
            float yf = wy + ncy + (y - hw - ncy) * s, yb = wy + ncy + (y + hw - ncy) * s;
            int o = i * 8;
            float bflx = wx + bottom[o], bfly = wy + bottom[o + 1], bfrx = wx + bottom[o + 2], bfry = wy + bottom[o + 3];

            //body colours
            float br, bg, bb;
            if(c >= 0){ br = 0.86f; bg = 0.87f; bb = 0.865f; }
            else{ br = 0.94f; bg = 0.95f; bb = 0.945f; }
            float hk = 0.92f + 0.08f * Math.min(height[i] / 2.5f, 1f);

            //side face (only one side can face the camera; the centre column shows none)
            if(lod > 0){
                if(x > hw){
                    float sh = westShade * hk;
                    float top = Color.toFloatBits(br * sh, bg * sh, bb * sh, 1f), bot = Color.toFloatBits(br * sh * 0.45f, bg * sh * 0.47f, bb * sh * 0.48f, 1f);
                    Fill.quad(wx + bottom[o + 4], wy + bottom[o + 5], bot, bflx, bfly, bot, xl, yf, top, xl, yb, top);
                }else if(x < -hw){
                    float sh = eastShade * hk;
                    float top = Color.toFloatBits(br * sh, bg * sh, bb * sh, 1f), bot = Color.toFloatBits(br * sh * 0.45f, bg * sh * 0.47f, bb * sh * 0.48f, 1f);
                    Fill.quad(bfrx, bfry, bot, wx + bottom[o + 6], wy + bottom[o + 7], bot, xr, yb, top, xr, yf, top);
                }
            }
            //front face
            float fsh = frontShade * hk;
            float ftop = Color.toFloatBits(br * fsh, bg * fsh, bb * fsh, 1f), fbot = Color.toFloatBits(br * fsh * 0.42f, bg * fsh * 0.44f, bb * fsh * 0.45f, 1f);
            Fill.quad(bflx, bfly, fbot, bfrx, bfry, fbot, xr, yf, ftop, xl, yf, ftop);

            //top face
            float tr, tg, tb;
            if(c >= 0){ tr = 0.80f; tg = 0.30f; tb = 0.26f; }
            else{ tr = br; tg = bg; tb = bb; }
            float ts = topShade;
            float topc = Color.toFloatBits(Math.min(tr * ts, 1f), Math.min(tg * ts, 1f), Math.min(tb * ts, 1f), 1f);
            Fill.quad(xl, yf, topc, xr, yf, topc, xr, yb, topc, xl, yb, topc);

            //cap: emissive indicator showing local flux (fuel) or withdrawal (CPS)
            if(lod > 1){
                float cr, cg, cb;
                if(c >= 0){
                    float v = local[i];
                    cr = 1f; cg = 0.36f + 0.22f * v; cb = 0.30f + 0.12f * v;
                    if(c == selected){ cr = 1f; cg = 0.78f + 0.2f * pulse; cb = 0.25f + 0.5f * pulse; }
                }else{
                    float l = local[i];
                    if(l < 0.55f){ float t = l / 0.55f; cr = Mathf.lerp(0.30f, 0.62f, t); cg = Mathf.lerp(0.36f, 0.90f, t); cb = Mathf.lerp(0.38f, 1.0f, t); }
                    else if(l < 1.1f){ float t = (l - 0.55f) / 0.55f; cr = Mathf.lerp(0.62f, 1.0f, t); cg = Mathf.lerp(0.90f, 0.86f, t); cb = Mathf.lerp(1.0f, 0.55f, t); }
                    else{ float t = Math.min((l - 1.1f) / 0.5f, 1f); cr = 1f; cg = Mathf.lerp(0.86f, 0.42f, t); cb = Mathf.lerp(0.55f, 0.28f, t); }
                }
                if(danger > 0f){ float d = danger * (0.5f + 0.5f * Mathf.sin(time * 0.35f)); cr = Mathf.lerp(cr, 1f, d); cg = Mathf.lerp(cg, 0.2f, d); cb = Mathf.lerp(cb, 0.15f, d); }
                float in = 0.26f;
                float ixl = wx + (x - in) * s, ixr = wx + (x + in) * s;
                float iyf = wy + ncy + (y - in - ncy) * s, iyb = wy + ncy + (y + in - ncy) * s;
                float cc = Color.toFloatBits(cr, cg, cb, 1f);
                Fill.quad(ixl, iyf, cc, ixr, iyf, cc, ixr, iyb, cc, ixl, iyb, cc);
            }
        }
        Live.quadsDrawn += channels * (lod > 1 ? 4 : lod > 0 ? 3 : 2);
    }

    /** Static (baker) representation: sockets are procedural, columns are added at a rest pose for icons. */
    public static void addRest(Mesh m, Cam cam){
        init(cam);
        for(int i = 0; i < channels; i++){
            int c = control[i];
            float h = c >= 0 ? 0.5f + 2.95f * 0.5f : 0.26f + 1.2f * 0.95f * prof[i];
            m.at(0, 0, 0);
            if(c >= 0) m.color(0.86f, 0.87f, 0.865f); else m.color(0.94f, 0.95f, 0.945f);
            m.plain().box(cx[i] - hw, cy[i] - hw, floorZ, cx[i] + hw, cy[i] + hw, floorZ + h);
            if(c >= 0){ m.color(1f, 0.42f, 0.34f).style(Mesh.emissive, 0); }
            else{ m.color(0.66f, 0.9f, 1f).style(Mesh.emissive, 0); }
            m.box(cx[i] - 0.26f, cy[i] - 0.26f, floorZ + h, cx[i] + 0.26f, cy[i] + 0.26f, floorZ + h + 0.02f);
        }
        m.plain();
    }

    public static void addEnvelope(Mesh m, Cam cam){
        init(cam);
        for(int i = 0; i < channels; i++){
            m.at(0, 0, 0).box(cx[i] - hw, cy[i] - hw, floorZ - 0.01f, cx[i] + hw, cy[i] + hw, floorZ + maxH);
        }
    }

    public static int mappedControl(int channel){
        init(new Cam(32f));
        return channel < 0 || channel >= channels ? -1 : control[channel];
    }

    /** @return time helper so tests can drive animation without the game loop */
    public static float now(){
        return Time.time;
    }
}
