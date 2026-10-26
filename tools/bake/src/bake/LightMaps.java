package bake;

import blackhole.g3d.*;

/**
 * Directional visibility maps rendered with an orthographic rasteriser: one for the key light (hard shadows with
 * 3x3 PCF) and a ring of eight sky directions plus the zenith whose average visibility is the ambient occlusion.
 * Both are evaluated per shaded sample, so contact shadows, pits, undersides of gantries and the gaps between
 * barrels all darken exactly where real light could not reach.
 */
public final class LightMaps{
    /** key light = the mod's shared {@link Light} direction, so baked and live Lambert terms agree */
    public static final float[] SUN = {Light.lx, Light.ly, Light.lz};
    static final int AO_DIRS = 8;
    static final float AO_ELEV = 52f;

    final Map sun;
    final Map[] ao = new Map[AO_DIRS + 1];

    public LightMaps(Soup s, int sunRes, int aoRes){
        sun = new Map(SUN, s, sunRes);
        for(int i = 0; i < AO_DIRS; i++){
            double az = Math.toRadians(i * 360.0 / AO_DIRS + 22.5), el = Math.toRadians(AO_ELEV);
            ao[i] = new Map(new float[]{(float)(Math.cos(az) * Math.cos(el)), (float)(Math.sin(az) * Math.cos(el)), (float)Math.sin(el)}, s, aoRes);
        }
        ao[AO_DIRS] = new Map(new float[]{0.001f, 0.001f, 1f}, s, aoRes);
    }

    /** fraction of the key light reaching p (0 = fully shadowed) */
    public float shadow(float x, float y, float z, float ndl){
        return sun.visible(x, y, z, 0.035f + 0.07f * (1f - ndl), true);
    }

    /** sky visibility of p for a surface with normal n (0..1) */
    public float ambient(float x, float y, float z, float nx, float ny, float nz){
        float sum = 0, wsum = 0;
        for(Map m : ao){
            float cos = m.dir[0] * nx + m.dir[1] * ny + m.dir[2] * nz;
            if(cos <= 0f) continue; //direction below the surface: contributes nothing either way
            float w = cos;
            sum += m.visible(x, y, z, 0.06f, false) * w;
            wsum += w;
        }
        return wsum <= 0f ? 1f : sum / wsum;
    }

    static final class Map{
        final float[] dir, right, up;
        final int n;
        final float[] depth;
        final float u0, v0, scale;

        Map(float[] d, Soup s, int res){
            float l = (float)Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
            dir = new float[]{d[0] / l, d[1] / l, d[2] / l};
            //basis
            float[] helper = Math.abs(dir[2]) < 0.95f ? new float[]{0, 0, 1} : new float[]{1, 0, 0};
            float[] r = cross(dir, helper);
            float rl = (float)Math.sqrt(r[0] * r[0] + r[1] * r[1] + r[2] * r[2]);
            right = new float[]{r[0] / rl, r[1] / rl, r[2] / rl};
            up = cross(right, dir);
            n = res;
            depth = new float[n * n];
            java.util.Arrays.fill(depth, -1e9f);
            //extent from the soup's corners
            float umin = 1e9f, umax = -1e9f, vmin = 1e9f, vmax = -1e9f;
            float[] xs = {s.minX, s.maxX}, ys = {s.minY, s.maxY}, zs = {s.minZ, s.maxZ};
            for(float x : xs) for(float y : ys) for(float z : zs){
                float u = x * right[0] + y * right[1] + z * right[2], v = x * up[0] + y * up[1] + z * up[2];
                umin = Math.min(umin, u); umax = Math.max(umax, u); vmin = Math.min(vmin, v); vmax = Math.max(vmax, v);
            }
            float ext = Math.max(umax - umin, vmax - vmin) * 1.04f + 0.5f;
            scale = n / ext;
            u0 = (umin + umax) / 2f - ext / 2f;
            v0 = (vmin + vmax) / 2f - ext / 2f;
            raster(s);
        }

        private void raster(Soup s){
            float[] px = new float[3], py = new float[3], pd = new float[3];
            for(int t = 0; t < s.tris; t++){
                for(int k = 0; k < 3; k++){
                    float x = s.pos[t * 9 + k * 3], y = s.pos[t * 9 + k * 3 + 1], z = s.pos[t * 9 + k * 3 + 2];
                    px[k] = ((x * right[0] + y * right[1] + z * right[2]) - u0) * scale;
                    py[k] = ((x * up[0] + y * up[1] + z * up[2]) - v0) * scale;
                    pd[k] = x * dir[0] + y * dir[1] + z * dir[2];
                }
                int x0 = Math.max(0, (int)Math.floor(Math.min(px[0], Math.min(px[1], px[2])))), x1 = Math.min(n - 1, (int)Math.ceil(Math.max(px[0], Math.max(px[1], px[2]))));
                int y0 = Math.max(0, (int)Math.floor(Math.min(py[0], Math.min(py[1], py[2])))), y1 = Math.min(n - 1, (int)Math.ceil(Math.max(py[0], Math.max(py[1], py[2]))));
                float area = (px[1] - px[0]) * (py[2] - py[0]) - (px[2] - px[0]) * (py[1] - py[0]);
                if(Math.abs(area) < 1e-7f) continue;
                for(int yy = y0; yy <= y1; yy++) for(int xx = x0; xx <= x1; xx++){
                    float cx = xx + 0.5f, cy = yy + 0.5f;
                    float l0 = ((px[1] - cx) * (py[2] - cy) - (px[2] - cx) * (py[1] - cy)) / area;
                    float l1 = ((px[2] - cx) * (py[0] - cy) - (px[0] - cx) * (py[2] - cy)) / area;
                    float l2 = 1 - l0 - l1;
                    if(l0 < -0.002f || l1 < -0.002f || l2 < -0.002f) continue;
                    float d = l0 * pd[0] + l1 * pd[1] + l2 * pd[2];
                    int i = yy * n + xx;
                    if(d > depth[i]) depth[i] = d;
                }
            }
        }

        /** 3x3 PCF visibility of a point */
        float visible(float x, float y, float z, float bias, boolean wide){
            float u = ((x * right[0] + y * right[1] + z * right[2]) - u0) * scale;
            float v = ((x * up[0] + y * up[1] + z * up[2]) - v0) * scale;
            float d = x * dir[0] + y * dir[1] + z * dir[2] + bias;
            float lit = 0;
            float step = wide ? 1.0f : 0.8f;
            for(int oy = -1; oy <= 1; oy++) for(int ox = -1; ox <= 1; ox++){
                int xi = (int)Math.floor(u + ox * step), yi = (int)Math.floor(v + oy * step);
                if(xi < 0 || yi < 0 || xi >= n || yi >= n){ lit += 1; continue; }
                lit += d >= depth[yi * n + xi] ? 1 : 0;
            }
            return lit / 9f;
        }
    }

    static float[] cross(float[] a, float[] b){
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }
}
