package bake;

import blackhole.g3d.*;

/**
 * Supersampled software rasteriser for the mod's shift-lens {@link Cam}.
 *
 * <p>Visibility: every triangle is projected exactly like {@link Live} projects live geometry (perspective
 * division by D - z), with the same outward-winding back-face test, and resolved with a z-buffer (1/(D-z)).
 * Shading then runs once per visible sample: the sample's pixel ray is intersected with the triangle plane, so
 * materials, shadows and ambient occlusion are evaluated at the true surface point.
 *
 * <p>Shading is the live renderer's Lambert / Blinn model ({@link Light}) - so baked and live parts match - refined
 * with a key-light shadow map, hemispherical sky occlusion from {@link LightMaps}, the kit's procedural materials
 * (panel seams, grates, fins, hazard stripes, rubber, bolts, water ripples), glass fresnel and the live renderer's
 * own per-face height gradient. Emissive faces are written unlit and also into a glow buffer for bloom.
 */
public final class Raster{
    static final float EDGE_TOL = 0.01f, DEPTH_TIE = 2e-7f;
    public static final float[] TEAM = {1.0f, 0.83f, 0.5f};
    static final float[] SKY = {0.62f, 0.68f, 0.78f};

    public final Cam cam;
    /** output size in pixels, supersampling factor, pixels per screen unit, screen-unit origin (left / top) */
    public final int W, H, SS, SW, SH;
    public final float ppu, ox, oy;

    /** per sample: nearest 1/(D-z) (0 = empty) and triangle index (-1 = empty); back = nearest fragment is back-facing */
    public final float[] iw;
    public final int[] tri;
    public boolean[] back;
    public long backSamples, coveredSamples;

    /** shaded output per sample (straight colour; coverage is tri >= 0) and glow */
    public float[] rgb, glow;

    public Raster(Cam cam, int w, int h, int ss, float ppu, float ox, float oy){
        this.cam = cam;
        W = w; H = h; SS = ss; SW = w * ss; SH = h * ss;
        this.ppu = ppu; this.ox = ox; this.oy = oy;
        iw = new float[SW * SH];
        tri = new int[SW * SH];
        java.util.Arrays.fill(tri, -1);
    }

    public void clear(){
        java.util.Arrays.fill(iw, 0f);
        java.util.Arrays.fill(tri, -1);
        if(back != null) java.util.Arrays.fill(back, false);
        backSamples = coveredSamples = 0;
    }

    /** sample x of a screen-unit x */
    public float px(float sx){ return (sx - ox) * ppu * SS; }
    public float py(float sy){ return (oy - sy) * ppu * SS; }

    // ------------------------------------------------------------------ visibility

    /**
     * Rasterises the soup into the z-buffer. cull=true draws front faces only (the live renderer's behaviour);
     * cull=false keeps the nearest fragment of either side and records whether it was a back face (open-mesh audit).
     * Triangle indices are offset by {@code idOffset} so several soups can share one buffer.
     */
    public void raster(Soup s, boolean cull, int idOffset){
        if(!cull && back == null) back = new boolean[SW * SH];
        float[][] p = new float[3][3];
        for(int t = 0; t < s.tris; t++){
            int o = t * 9;
            float ax = s.pos[o], ay = s.pos[o + 1], az = s.pos[o + 2];
            float bx = s.pos[o + 3], by = s.pos[o + 4], bz = s.pos[o + 5];
            float cx = s.pos[o + 6], cy = s.pos[o + 7], cz = s.pos[o + 8];
            float e1x = bx - ax, e1y = by - ay, e1z = bz - az, e2x = cx - bx, e2y = cy - by, e2z = cz - bz;
            float nx = e1y * e2z - e1z * e2y, ny = e1z * e2x - e1x * e2z, nz = e1x * e2y - e1y * e2x;
            boolean facing = cam.facing(nx, ny, nz, ax, ay, az);
            if(cull && !facing) continue;
            boolean clip = false;
            for(int k = 0; k < 3; k++){
                float x = s.pos[o + k * 3], y = s.pos[o + k * 3 + 1], z = s.pos[o + k * 3 + 2];
                float w = cam.D - z;
                if(w < 0.5f) clip = true;
                p[k][0] = px(cam.sx(x, z));
                p[k][1] = py(cam.sy(y, z));
                p[k][2] = 1f / w;
            }
            if(clip) continue;
            float area = (p[1][0] - p[0][0]) * (p[2][1] - p[0][1]) - (p[2][0] - p[0][0]) * (p[1][1] - p[0][1]);
            if(Math.abs(area) < 1e-9f) continue;
            int x0 = Math.max(0, (int)Math.floor(Math.min(p[0][0], Math.min(p[1][0], p[2][0]))));
            int x1 = Math.min(SW - 1, (int)Math.ceil(Math.max(p[0][0], Math.max(p[1][0], p[2][0]))));
            int y0 = Math.max(0, (int)Math.floor(Math.min(p[0][1], Math.min(p[1][1], p[2][1]))));
            int y1 = Math.min(SH - 1, (int)Math.ceil(Math.max(p[0][1], Math.max(p[1][1], p[2][1]))));
            if(x0 > x1 || y0 > y1) continue;
            //edge functions are evaluated with a tiny (0.01 px) outward tolerance so that samples lying exactly on an
            //edge shared by two faces are claimed by both and the depth test decides, instead of leaking a hairline
            //crack to whatever lies behind; depth ties prefer front faces
            float sgn = area > 0 ? 1f : -1f, aabs = area * sgn, inv = 1f / aabs;
            float tol = facing ? EDGE_TOL : 0f; //back faces (audit only) are never widened past a silhouette
            float L0 = (float)Math.hypot(p[2][0] - p[1][0], p[2][1] - p[1][1]) * tol;
            float L1 = (float)Math.hypot(p[0][0] - p[2][0], p[0][1] - p[2][1]) * tol;
            float L2 = (float)Math.hypot(p[1][0] - p[0][0], p[1][1] - p[0][1]) * tol;
            for(int y = y0; y <= y1; y++){
                float sy = y + 0.5f;
                for(int x = x0; x <= x1; x++){
                    float sx = x + 0.5f;
                    float e0 = ((p[1][0] - sx) * (p[2][1] - sy) - (p[2][0] - sx) * (p[1][1] - sy)) * sgn;
                    if(e0 < -L0) continue;
                    float e1 = ((p[2][0] - sx) * (p[0][1] - sy) - (p[0][0] - sx) * (p[2][1] - sy)) * sgn;
                    if(e1 < -L1) continue;
                    float e2 = aabs - e0 - e1;
                    if(e2 < -L2) continue;
                    float l0 = Math.max(0f, e0 * inv), l1 = Math.max(0f, e1 * inv), l2 = Math.max(0f, 1f - l0 - l1);
                    float w = l0 * p[0][2] + l1 * p[1][2] + l2 * p[2][2];
                    int i = y * SW + x;
                    float cur = iw[i];
                    if(w < cur * (1f - DEPTH_TIE)) continue;
                    if(w <= cur * (1f + DEPTH_TIE) && (cull || !facing || !back[i])) continue; //tie: keep unless it replaces a back face
                    iw[i] = w;
                    tri[i] = t + idOffset;
                    if(!cull) back[i] = !facing;
                }
            }
        }
    }

    /** counts covered samples and samples whose nearest fragment is a back face (visible interior = open mesh) */
    public void audit(){
        backSamples = coveredSamples = 0;
        for(int i = 0; i < tri.length; i++){
            if(tri[i] < 0) continue;
            coveredSamples++;
            if(back != null && back[i]) backSamples++;
        }
    }

    // ------------------------------------------------------------------ shading

    /** material pattern multiplier evaluated in the plane of the face; hazard returns yellow coverage (handled by caller) */
    static float pattern(int mt, float x, float y, float z, float nx, float ny, float nz, float pix, float waterCx, float waterCy){
        float ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);
        float u, v;
        if(az >= ax && az >= ay){ u = x; v = y; }else if(ax >= ay){ u = y; v = z; }else{ u = x; v = z; }
        switch(mt){
            case 1: { //plate: panel seams every 2 units + faint per-panel tone
                float fu = frac(u / 2f + 0.5f), fv = frac(v / 2f + 0.5f);
                float e = Math.min(Math.min(fu, 1 - fu), Math.min(fv, 1 - fv)) * 2f;
                return 1f - 0.26f * line(e, 0.036f, pix) - 0.035f * hash((int)Math.floor(u / 2f + 0.5f), (int)Math.floor(v / 2f + 0.5f));
            }
            case 2: return 0.955f + 0.045f * hash((int)Math.floor(u * 3), (int)Math.floor(v * 3));
            case 3: return 1.0f - 0.38f * wave(u, 1f / 3f, pix);
            case 4: return wave(u + v, 1f / 0.9f, pix);
            case 5: { float fu = frac(u * 1.5f) - 0.5f, fv = frac(v * 1.5f) - 0.5f; return 1f + 0.25f * line((float)Math.sqrt(fu * fu + fv * fv) / 1.5f, 0.11f, pix); }
            case 7: { //socket: dark recess ring
                float fu = frac(u) - 0.5f, fv = frac(v) - 0.5f;
                return 1f - 0.3f * line((float)Math.sqrt(fu * fu + fv * fv), 0.25f, pix);
            }
            case 8: { //water: concentric ripples round the model's water centre
                float d = (float)Math.hypot(x - waterCx, y - waterCy);
                return 0.94f + 0.08f * (float)Math.sin(d * 5.2f) * clamp((0.6f - pix) / 0.6f);
            }
            case 9: return 0.85f;
            case 10: return 1.05f - 0.5f * wave(u, 1f / 3.3f, pix);
            case 11: return 1f - 0.2f * wave(u, 1f / 1.2f, pix);
            default: return 1f;
        }
    }

    /** anti-aliased line coverage: hw = half width (pattern units), e = distance, pix = world size of a sample */
    static float line(float e, float hw, float pix){
        float f = pix * 1.5f;
        return clamp((hw - e) / f + 0.5f) * clamp(hw * 2f / f);
    }

    /** anti-aliased square wave (duty 0.5) of period per; fades to its mean when finer than a sample */
    static float wave(float x, float per, float pix){
        float fr = frac(x / per);
        float f = pix * 1.5f / per;
        float edge = Math.min(fr, 1 - fr) * 2f - 0.5f;
        float v = clamp(edge / Math.max(f, 1e-4f) + 0.5f);
        float fade = clamp((0.45f - f) / 0.25f);
        return 0.5f + (v - 0.5f) * fade;
    }

    /**
     * Shades every covered sample of the z-buffer. {@code soups} are indexed by the id offsets used in raster();
     * {@code offsets[k]} is the first triangle id of soups[k].
     */
    public void shade(Soup[] soups, int[] offsets, LightMaps maps, float waterCx, float waterCy){
        rgb = new float[SW * SH * 3];
        glow = new float[SW * SH * 3];
        float D = cam.D, cy = cam.cy;
        for(int i = 0; i < tri.length; i++){
            int id = tri[i];
            if(id < 0) continue;
            int k = soups.length - 1;
            while(k > 0 && offsets[k] > id) k--;
            Soup s = soups[k];
            int t = id - offsets[k];
            int o = t * 9;
            float ax = s.pos[o], ay = s.pos[o + 1], az = s.pos[o + 2];
            float bx = s.pos[o + 3], by = s.pos[o + 4], bz = s.pos[o + 5];
            float cx = s.pos[o + 6], cy2 = s.pos[o + 7], cz = s.pos[o + 8];
            float e1x = bx - ax, e1y = by - ay, e1z = bz - az, e2x = cx - bx, e2y = cy2 - by, e2z = cz - bz;
            float nx = e1y * e2z - e1z * e2y, ny = e1z * e2x - e1x * e2z, nz = e1x * e2y - e1y * e2x;
            float nl = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
            if(nl < 1e-12f) continue;
            nx /= nl; ny /= nl; nz /= nl;
            //pixel ray: from the eye E = (0, cy, D) through the image-plane point (sx, sy, 0)
            int px = i % SW, py = i / SW;
            float sx = ox + (px + 0.5f) / (ppu * SS), sy = oy - (py + 0.5f) / (ppu * SS);
            float dx = sx, dy = sy - cy, dz = -D;
            float denom = nx * dx + ny * dy + nz * dz;
            float X, Y, Z;
            if(Math.abs(denom) < 1e-9f){
                X = ax; Y = ay; Z = az;
            }else{
                float tt = (nx * (ax - 0f) + ny * (ay - cy) + nz * (az - D)) / denom;
                X = dx * tt; Y = cy + dy * tt; Z = D + dz * tt;
            }
            //world size of one sample at this depth (material anti-aliasing)
            float pix = 1f / (ppu * SS * cam.scale(Z));
            int fl = s.flags[t], mt = s.mat[t];
            float cr = s.col[t * 3], cg = s.col[t * 3 + 1], cb = s.col[t * 3 + 2];
            if((fl & Mesh.team) != 0){ cr *= TEAM[0]; cg *= TEAM[1]; cb *= TEAM[2]; }
            boolean em = (fl & Mesh.emissive) != 0;
            float pat = pattern(mt, X, Y, Z, nx, ny, nz, pix, waterCx, waterCy);
            if(mt == 4){
                cr = lerp(0.05f, cr, pat); cg = lerp(0.05f, cg, pat); cb = lerp(0.055f, cb, pat);
                pat = 1f;
            }
            cr *= pat; cg *= pat; cb *= pat;
            int oi = i * 3;
            if(em){
                rgb[oi] = Math.min(1f, cr); rgb[oi + 1] = Math.min(1f, cg); rgb[oi + 2] = Math.min(1f, cb);
                glow[oi] = cr; glow[oi + 1] = cg; glow[oi + 2] = cb;
                continue;
            }
            boolean metal = (fl & Mesh.metal) != 0, glass = (fl & Mesh.glass) != 0;
            float ndl = nx * Light.lx + ny * Light.ly + nz * Light.lz;
            if(ndl < 0f) ndl = 0f;
            float off = 0.06f;
            float sh = ndl > 0f && maps != null ? maps.shadow(X + nx * off, Y + ny * off, Z + nz * off, ndl) : 1f;
            float vis = maps != null ? maps.ambient(X + nx * off, Y + ny * off, Z + nz * off, nx, ny, nz) : 1f;
            //ambient: the live renderer's constant term, shaped by the hemisphere and scaled by sky visibility
            float amb = Light.hemi(nz) * (0.42f + 0.58f * vis);
            float lam = amb + Light.diffuse * ndl * sh;
            //live renderer's vertical gradient within a face
            float grad = 1f + Math.max(-0.14f, Math.min(0.14f, (Z - s.midZ[t]) * 0.07f));
            //the live renderer's Blinn term (fixed half vector, so baked and live faces match exactly) ...
            float m = metal ? 1f : 0f;
            float spec = Light.spec(nx, ny, nz, m) * sh * (mt == 9 ? 0.15f : 1f);
            //... plus a small sharp highlight from the true view vector on metal and glass (the refined look)
            float vx = -X, vy = cy - Y, vz = D - Z;
            float vl = (float)Math.sqrt(vx * vx + vy * vy + vz * vz);
            vx /= vl; vy /= vl; vz /= vl;
            if(metal || glass){
                float hx = Light.lx + vx, hy = Light.ly + vy, hz = Light.lz + vz;
                float hl = (float)Math.sqrt(hx * hx + hy * hy + hz * hz);
                float nh = (nx * hx + ny * hy + nz * hz) / hl;
                if(nh > 0f){
                    float d2 = nh * nh, d4 = d2 * d2, d8 = d4 * d4, d16 = d8 * d8, d32 = d16 * d16;
                    spec += (glass ? 0.22f : 0.09f) * d32 * sh;
                }
            }
            float or = cr * lam * grad + spec, og = cg * lam * grad + spec, ob = cb * lam * grad + spec;
            if(glass){
                float ndv = nx * vx + ny * vy + nz * vz;
                float fres = (float)Math.pow(1f - Math.max(0f, ndv), 4) * 0.45f;
                or = or * 0.72f + SKY[0] * (0.10f + fres) * vis; og = og * 0.72f + SKY[1] * (0.10f + fres) * vis; ob = ob * 0.72f + SKY[2] * (0.12f + fres) * vis;
            }
            rgb[oi] = Light.tone(or); rgb[oi + 1] = Light.tone(og); rgb[oi + 2] = Light.tone(ob);
        }
    }

    static float clamp(float v){ return v < 0f ? 0f : v > 1f ? 1f : v; }
    static float lerp(float a, float b, float k){ return a + (b - a) * k; }
    static float frac(float v){ return v - (float)Math.floor(v); }
    static float hash(int x, int y){ int h = x * 374761393 + y * 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xffff) / 65535f; }
}
