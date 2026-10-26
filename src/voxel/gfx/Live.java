package voxel.gfx;

import arc.graphics.*;
import arc.graphics.g2d.*;

/**
 * Live 3D path for moving parts: rigid transform -> exact perspective projection -> exact back-face culling ->
 * the same Lambert/Blinn light as the baked textures -> {@code Fill.quad} with per-vertex height gradient.
 *
 * <p>Only moving parts go through here; everything static is baked offline by the very same camera and light,
 * so a whole factory costs a few dozen quads per frame. No shader, no allocation after warm-up.
 */
public final class Live{
    private static float[] tx = new float[256], ty = new float[256], tz = new float[256], px = new float[256], py = new float[256];
    //current transform p' = M p + T
    private static float m00 = 1, m01, m02, m10, m11 = 1, m12, m20, m21, m22 = 1, t0, t1, t2;
    /** multiplies every emissive face */
    public static float glowR = 1f, glowG = 1f, glowB = 1f;
    /** multiplies every lit face */
    public static float tintR = 1f, tintG = 1f, tintB = 1f, alpha = 1f;
    /** statistics for tests */
    public static long quadsDrawn;

    private Live(){}

    public static void resetTint(){
        glowR = glowG = glowB = 1f;
        tintR = tintG = tintB = 1f;
        alpha = 1f;
    }

    /** Rotation about a local axis through the mesh origin, then translation. */
    public static void pose(float ox, float oy, float oz, int axis, float deg){
        float c = arc.math.Mathf.cosDeg(deg), s = arc.math.Mathf.sinDeg(deg);
        if(axis == 0){ m00 = 1; m01 = 0; m02 = 0; m10 = 0; m11 = c; m12 = -s; m20 = 0; m21 = s; m22 = c; }
        else if(axis == 1){ m00 = c; m01 = 0; m02 = s; m10 = 0; m11 = 1; m12 = 0; m20 = -s; m21 = 0; m22 = c; }
        else{ m00 = c; m01 = -s; m02 = 0; m10 = s; m11 = c; m12 = 0; m20 = 0; m21 = 0; m22 = 1; }
        t0 = ox; t1 = oy; t2 = oz;
    }

    /** Frame whose local z runs from p0 to p1 (length-scaled), local x/y scaled by w/h: for rods and links. */
    public static void link(float x0, float y0, float z0, float x1, float y1, float z1, float w, float h){
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float l = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if(l < 1e-4f){ dx = 0; dy = 0; dz = 1; l = 1e-4f; }
        float nx = dx / l, ny = dy / l, nz = dz / l;
        float hx = Math.abs(nz) < 0.9f ? 0f : 1f, hz = Math.abs(nz) < 0.9f ? 1f : 0f;
        float ux = -hz * ny, uy = hz * nx - hx * nz, uz = hx * ny;
        float ul = (float)Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul; uy /= ul; uz /= ul;
        float vx = ny * uz - nz * uy, vy = nz * ux - nx * uz, vz = nx * uy - ny * ux;
        m00 = ux * w; m01 = vx * h; m02 = dx;
        m10 = uy * w; m11 = vy * h; m12 = dy;
        m20 = uz * w; m21 = vz * h; m22 = dz;
        t0 = x0; t1 = y0; t2 = z0;
    }

    /** Draws mesh m with the current pose; (wx,wy) is the building centre in world units. */
    public static void draw(Mesh m, Cam cam, float wx, float wy){
        int n = m.verts;
        if(tx.length < n){
            int cap = Integer.highestOneBit(n) << 1;
            tx = new float[cap]; ty = new float[cap]; tz = new float[cap]; px = new float[cap]; py = new float[cap];
        }
        float D = cam.D, cy = cam.cy;
        for(int i = 0; i < n; i++){
            float x = m.vx[i], y = m.vy[i], z = m.vz[i];
            float X = m00 * x + m01 * y + m02 * z + t0, Y = m10 * x + m11 * y + m12 * z + t1, Z = m20 * x + m21 * y + m22 * z + t2;
            tx[i] = X; ty[i] = Y; tz[i] = Z;
            float s = D / (D - Z);
            px[i] = wx + X * s;
            py[i] = wy + cy + (Y - cy) * s;
        }
        for(int f = 0; f < m.faces; f++){
            int a = m.f0[f], b = m.f1[f], c = m.f2[f], d = m.f3[f];
            float e1x = tx[b] - tx[a], e1y = ty[b] - ty[a], e1z = tz[b] - tz[a];
            float e2x = tx[c] - tx[b], e2y = ty[c] - ty[b], e2z = tz[c] - tz[b];
            float nx = e1y * e2z - e1z * e2y, ny = e1z * e2x - e1x * e2z, nz = e1x * e2y - e1y * e2x;
            if(!cam.facing(nx, ny, nz, tx[a], ty[a], tz[a])) continue;
            float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
            if(len < 1e-7f) continue;
            nx /= len; ny /= len; nz /= len;
            int fl = m.flags[f];
            float r, g, bl;
            if((fl & Mesh.emissive) != 0){
                r = m.cr[f] * glowR; g = m.cg[f] * glowG; bl = m.cb[f] * glowB;
                float c0 = Color.toFloatBits(Math.min(r, 1f), Math.min(g, 1f), Math.min(bl, 1f), alpha);
                Fill.quad(px[a], py[a], c0, px[b], py[b], c0, px[c], py[c], c0, px[d], py[d], c0);
            }else{
                float sh = Light.diffuse(nx, ny, nz);
                float sp = Light.spec(nx, ny, nz, (fl & Mesh.metal) != 0 ? 1f : 0f);
                r = m.cr[f] * tintR * sh + sp; g = m.cg[f] * tintG * sh + sp; bl = m.cb[f] * tintB * sh + sp;
                float mid = (tz[a] + tz[b] + tz[c] + tz[d]) * 0.25f;
                Fill.quad(
                    px[a], py[a], grad(r, g, bl, tz[a] - mid),
                    px[b], py[b], grad(r, g, bl, tz[b] - mid),
                    px[c], py[c], grad(r, g, bl, tz[c] - mid),
                    px[d], py[d], grad(r, g, bl, tz[d] - mid));
            }
            quadsDrawn++;
        }
    }

    private static float grad(float r, float g, float b, float dz){
        float k = 1f + Math.max(-0.14f, Math.min(0.14f, dz * 0.07f));
        return Color.toFloatBits(Math.min(r * k, 1f), Math.min(g * k, 1f), Math.min(b * k, 1f), alpha);
    }
}
