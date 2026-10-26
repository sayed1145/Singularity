package blackhole.g3d;

import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;

/**
 * Draws a posed {@link Rig} with the same perspective camera and light as the baked buildings.
 *
 * <p>Per frame: concatenate bone matrices (≤64 small 3x4 products), order the convex pieces far-to-near by
 * the distance of their centroid to the camera (a persistent order array, re-sorted by insertion sort, so
 * it is nearly free when the pose changes smoothly), then transform, cull and shade each piece's faces
 * into {@code Fill.quad}. No per-face sort, no allocation, no shader.
 *
 * <p>The legacy renderer insertion-sorted every face of the model and rebuilt a shadow hull from all
 * vertices each frame; this one sorts ~40 pieces and hulls a few dozen bounding-box corners.
 */
public final class UnitRenderer{
    public Cam cam;
    /** world matrices of the current pose */
    private float[] wm = new float[Rig.maxBones * 12];
    private float[] tx = new float[64], ty = new float[64], tz = new float[64], px = new float[64], py = new float[64];
    private float[] keys = new float[64];
    private int[] order = new int[64];
    private Rig orderOwner;
    private final float[] hull = new float[512], pts = new float[1024];
    private static final float[] RC = new float[8], RS = new float[8];
    static{
        for(int i = 0; i < 8; i++){ RC[i] = (float)Math.cos(i * Math.PI / 4 + Math.PI / 8); RS[i] = (float)Math.sin(i * Math.PI / 4 + Math.PI / 8); }
    }

    /** per draw settings */
    public float teamR = 1f, teamG = 0.827f, teamB = 0.498f, alpha = 1f;
    /** printing: faces above clipZ are removed, faces in the band below it glow */
    public float clipZ = Float.MAX_VALUE, clipBand = 1.2f;
    public float scanR = 0.55f, scanG = 0.95f, scanB = 1f;
    /** skip pieces flagged as detail */
    public boolean lowDetail;
    /** statistics */
    public long quads;

    public UnitRenderer(float half){
        cam = new Cam(half);
    }

    public void setTeam(Color c){
        teamR = c.r; teamG = c.g; teamB = c.b;
    }

    public void reset(){
        alpha = 1f;
        clipZ = Float.MAX_VALUE;
        lowDetail = false;
    }

    /**
     * Computes world matrices. root = T(ox,oy,oz) · Rz(yaw - 90) · S(scale), so model +y points along the
     * Mindustry rotation {@code yaw}; (ox,oy) are relative to the camera origin (normally the unit position).
     */
    public void pose(Rig r, float yaw, float ox, float oy, float oz, float scale){
        float c = Mathf.cosDeg(yaw - 90f) * scale, s = Mathf.sinDeg(yaw - 90f) * scale;
        float[] L = r.local, W = wm;
        for(int b = 0; b < r.bones; b++){
            int o = b * 12, p = r.parent[b];
            if(p < 0){
                //root x local
                float a0 = L[o], a1 = L[o + 1], a2 = L[o + 2], a3 = L[o + 3];
                float b0 = L[o + 4], b1 = L[o + 5], b2 = L[o + 6], b3 = L[o + 7];
                W[o] = c * a0 - s * b0; W[o + 1] = c * a1 - s * b1; W[o + 2] = c * a2 - s * b2; W[o + 3] = c * a3 - s * b3 + ox;
                W[o + 4] = s * a0 + c * b0; W[o + 5] = s * a1 + c * b1; W[o + 6] = s * a2 + c * b2; W[o + 7] = s * a3 + c * b3 + oy;
                W[o + 8] = scale * L[o + 8]; W[o + 9] = scale * L[o + 9]; W[o + 10] = scale * L[o + 10]; W[o + 11] = scale * L[o + 11] + oz;
            }else{
                int q = p * 12;
                for(int row = 0; row < 3; row++){
                    float p0 = W[q + row * 4], p1 = W[q + row * 4 + 1], p2 = W[q + row * 4 + 2], p3 = W[q + row * 4 + 3];
                    W[o + row * 4] = p0 * L[o] + p1 * L[o + 4] + p2 * L[o + 8];
                    W[o + row * 4 + 1] = p0 * L[o + 1] + p1 * L[o + 5] + p2 * L[o + 9];
                    W[o + row * 4 + 2] = p0 * L[o + 2] + p1 * L[o + 6] + p2 * L[o + 10];
                    W[o + row * 4 + 3] = p0 * L[o + 3] + p1 * L[o + 7] + p2 * L[o + 11] + p3;
                }
            }
        }
    }

    /** Transforms a point in bone b's frame to camera-origin-relative 3D; out = {x,y,z}. */
    public void point(int b, float x, float y, float z, float[] out){
        int o = b * 12;
        out[0] = wm[o] * x + wm[o + 1] * y + wm[o + 2] * z + wm[o + 3];
        out[1] = wm[o + 4] * x + wm[o + 5] * y + wm[o + 6] * z + wm[o + 7];
        out[2] = wm[o + 8] * x + wm[o + 9] * y + wm[o + 10] * z + wm[o + 11];
    }

    /** Screen-space offset (world units, relative to the camera origin) of a 3D point. */
    public float screenX(float x, float z){
        return cam.sx(x, z);
    }

    public float screenY(float y, float z){
        return cam.sy(y, z);
    }

    /** World direction of bone b's local axis (0=x,1=y,2=z), normalised; out={x,y,z}. */
    public void axis(int b, int axis, float[] out){
        int o = b * 12;
        float x = wm[o + axis], y = wm[o + 4 + axis], z = wm[o + 8 + axis];
        float l = (float)Math.sqrt(x * x + y * y + z * z);
        if(l < 1e-6f) l = 1f;
        out[0] = x / l; out[1] = y / l; out[2] = z / l;
    }

    // ------------------------------------------------------------------ drawing

    /** Draws the posed rig; (wx,wy) is the world position of the camera origin. */
    public void draw(Rig r, float wx, float wy){
        int n = r.pieceCount;
        if(order.length < n){
            order = new int[Integer.highestOneBit(n) << 1];
            keys = new float[order.length];
            orderOwner = null;
        }
        if(orderOwner != r){
            for(int i = 0; i < n; i++) order[i] = i;
            orderOwner = r;
        }
        if(tx.length < r.maxVerts){
            int cap = Integer.highestOneBit(r.maxVerts) << 1;
            tx = new float[cap]; ty = new float[cap]; tz = new float[cap]; px = new float[cap]; py = new float[cap];
        }
        float D = cam.D, cy = cam.cy;
        //keys: squared distance from the camera to the piece centroid (decals: just nearer than their base)
        for(int i = 0; i < n; i++){
            Rig.Piece p = r.pieces[i];
            Rig.Piece base = p.attach >= 0 ? r.pieces[p.attach] : p;
            int o = base.bone * 12;
            float X = wm[o] * base.cx + wm[o + 1] * base.cy + wm[o + 2] * base.cz + wm[o + 3];
            float Y = wm[o + 4] * base.cx + wm[o + 5] * base.cy + wm[o + 6] * base.cz + wm[o + 7];
            float Z = wm[o + 8] * base.cx + wm[o + 9] * base.cy + wm[o + 10] * base.cz + wm[o + 11];
            float dy = Y - cy, dz = D - Z;
            keys[i] = X * X + dy * dy + dz * dz + (p.attach >= 0 ? 0.05f * p.attachOrder : 0f);
        }
        if(r.stackSort) stack(r, n);
        //insertion sort, descending key (far first); order persists between frames
        for(int i = 1; i < n; i++){
            int v = order[i];
            float k = keys[v];
            int j = i - 1;
            while(j >= 0 && keys[order[j]] < k){
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = v;
        }
        for(int i = 0; i < n; i++){
            Rig.Piece p = r.pieces[order[i]];
            if(r.hidden[p.bone] || (lowDetail && p.detail)) continue;
            drawPiece(r, p, wx, wy);
        }
    }

    private float[] bx0 = new float[64], bx1 = new float[64], by0 = new float[64], by1 = new float[64], bz0 = new float[64], bz1 = new float[64];

    /**
     * Vertical stacking: if piece A rests on piece B (A's lowest point is at or above B's top and their footprints
     * overlap), A must be drawn after B, i.e. get a smaller key. World bounds come from the bone matrix and the
     * piece's local box. Three relaxation passes resolve chains (turret on deck on chassis). Attached decals then
     * follow their (corrected) base.
     */
    private void stack(Rig r, int n){
        if(bx0.length < n){
            int cap = Integer.highestOneBit(n) << 1;
            bx0 = new float[cap]; bx1 = new float[cap]; by0 = new float[cap]; by1 = new float[cap]; bz0 = new float[cap]; bz1 = new float[cap];
        }
        for(int i = 0; i < n; i++){
            Rig.Piece p = r.pieces[i];
            int o = p.bone * 12;
            float mx = (p.minX + p.maxX) * 0.5f, my = (p.minY + p.maxY) * 0.5f, mz = (p.minZ + p.maxZ) * 0.5f;
            float hx = (p.maxX - p.minX) * 0.5f, hy = (p.maxY - p.minY) * 0.5f, hz = (p.maxZ - p.minZ) * 0.5f;
            float cx = wm[o] * mx + wm[o + 1] * my + wm[o + 2] * mz + wm[o + 3];
            float cy = wm[o + 4] * mx + wm[o + 5] * my + wm[o + 6] * mz + wm[o + 7];
            float cz = wm[o + 8] * mx + wm[o + 9] * my + wm[o + 10] * mz + wm[o + 11];
            float ex = Math.abs(wm[o]) * hx + Math.abs(wm[o + 1]) * hy + Math.abs(wm[o + 2]) * hz;
            float ey = Math.abs(wm[o + 4]) * hx + Math.abs(wm[o + 5]) * hy + Math.abs(wm[o + 6]) * hz;
            float ez = Math.abs(wm[o + 8]) * hx + Math.abs(wm[o + 9]) * hy + Math.abs(wm[o + 10]) * hz;
            bx0[i] = cx - ex; bx1[i] = cx + ex; by0[i] = cy - ey; by1[i] = cy + ey; bz0[i] = cz - ez; bz1[i] = cz + ez;
        }
        for(int pass = 0; pass < 3; pass++){
            boolean changed = false;
            for(int a = 0; a < n; a++){
                if(r.pieces[a].attach >= 0) continue;
                for(int b = 0; b < n; b++){
                    if(a == b || r.pieces[b].attach >= 0) continue;
                    float tol = (bz1[b] - bz0[b]) * 0.25f + 0.05f;
                    if(bz0[a] < bz1[b] - tol || bz1[a] <= bz1[b]) continue;
                    //footprint overlap, shrunk slightly so pieces merely touching side by side do not count
                    float sx = Math.min(bx1[a], bx1[b]) - Math.max(bx0[a], bx0[b]);
                    float sy = Math.min(by1[a], by1[b]) - Math.max(by0[a], by0[b]);
                    if(sx <= 0.2f || sy <= 0.2f) continue;
                    if(keys[a] >= keys[b] - 0.01f){
                        keys[a] = keys[b] - 0.02f;
                        changed = true;
                    }
                }
            }
            if(!changed) break;
        }
        //decals: key of their base (already includes the attach bias computed before)
        for(int i = 0; i < n; i++){
            Rig.Piece p = r.pieces[i];
            if(p.attach >= 0) keys[i] = keys[p.attach] + 0.0001f * p.attachOrder;
        }
    }

    private void drawPiece(Rig r, Rig.Piece p, float wx, float wy){
        Mesh m = p.mesh;
        int o = p.bone * 12;
        float D = cam.D, cy = cam.cy;
        float m00 = wm[o], m01 = wm[o + 1], m02 = wm[o + 2], t0 = wm[o + 3];
        float m10 = wm[o + 4], m11 = wm[o + 5], m12 = wm[o + 6], t1 = wm[o + 7];
        float m20 = wm[o + 8], m21 = wm[o + 9], m22 = wm[o + 10], t2 = wm[o + 11];
        boolean clipping = clipZ < 1e8f;
        for(int i = 0; i < m.verts; i++){
            float x = m.vx[i], y = m.vy[i], z = m.vz[i];
            float X = m00 * x + m01 * y + m02 * z + t0, Y = m10 * x + m11 * y + m12 * z + t1, Z = m20 * x + m21 * y + m22 * z + t2;
            tx[i] = X; ty[i] = Y; tz[i] = Z;
            float Zp = clipping ? Math.min(Z, clipZ) : Z;
            float s = D / Math.max(D - Zp, 1f);
            px[i] = wx + X * s;
            py[i] = wy + cy + (Y - cy) * s;
        }
        float boneAlpha = alpha * r.alpha[p.bone], glow = r.glow[p.bone];
        if(boneAlpha <= 0.004f) return;
        for(int f = 0; f < m.faces; f++){
            int a = m.f0[f], b = m.f1[f], c = m.f2[f], d = m.f3[f];
            float e1x = tx[b] - tx[a], e1y = ty[b] - ty[a], e1z = tz[b] - tz[a];
            float e2x = tx[c] - tx[b], e2y = ty[c] - ty[b], e2z = tz[c] - tz[b];
            float nx = e1y * e2z - e1z * e2y, ny = e1z * e2x - e1x * e2z, nz = e1x * e2y - e1y * e2x;
            if(!cam.facing(nx, ny, nz, tx[a], ty[a], tz[a])) continue;
            float scan = 0f;
            if(clipping){
                float lo = Math.min(Math.min(tz[a], tz[b]), Math.min(tz[c], tz[d]));
                if(lo > clipZ) continue;
                float hi = Math.max(Math.max(tz[a], tz[b]), Math.max(tz[c], tz[d]));
                if(hi > clipZ - clipBand) scan = Mathf.clamp(1f - (clipZ - hi) / clipBand);
            }
            float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
            if(len < 1e-7f) continue;
            nx /= len; ny /= len; nz /= len;
            int fl = m.flags[f];
            float cr = m.cr[f], cg = m.cg[f], cb = m.cb[f];
            if((fl & Mesh.team) != 0){ cr *= teamR; cg *= teamG; cb *= teamB; }
            float R, G, B;
            if((fl & Mesh.emissive) != 0){
                R = cr * glow; G = cg * glow; B = cb * glow;
            }else{
                float sh = Light.diffuse(nx, ny, nz);
                float sp = Light.spec(nx, ny, nz, (fl & Mesh.metal) != 0 ? 1f : 0f);
                R = cr * sh + sp; G = cg * sh + sp; B = cb * sh + sp;
            }
            if(scan > 0f){
                R += (scanR - R) * scan; G += (scanG - G) * scan; B += (scanB - B) * scan;
            }
            if((fl & Mesh.emissive) != 0 || lowDetail){
                float c0 = Color.toFloatBits(Math.min(R, 1f), Math.min(G, 1f), Math.min(B, 1f), boneAlpha);
                Fill.quad(px[a], py[a], c0, px[b], py[b], c0, px[c], py[c], c0, px[d], py[d], c0);
            }else{
                float mid = (tz[a] + tz[b] + tz[c] + tz[d]) * 0.25f;
                Fill.quad(
                    px[a], py[a], grad(R, G, B, tz[a] - mid, boneAlpha),
                    px[b], py[b], grad(R, G, B, tz[b] - mid, boneAlpha),
                    px[c], py[c], grad(R, G, B, tz[c] - mid, boneAlpha),
                    px[d], py[d], grad(R, G, B, tz[d] - mid, boneAlpha));
            }
            quads++;
        }
    }

    private static float grad(float r, float g, float b, float dz, float a){
        float k = 1f + Math.max(-0.12f, Math.min(0.12f, dz * 0.06f));
        return Color.toFloatBits(Math.min(r * k, 1f), Math.min(g * k, 1f), Math.min(b * k, 1f), a);
    }

    /** Bakes the current pose into one static mesh (offline icons / previews). Hidden bones and alpha ~0 are skipped. */
    public Mesh flatten(Rig r, Mesh out, boolean details){
        for(int i = 0; i < r.pieceCount; i++){
            Rig.Piece p = r.pieces[i];
            if(r.hidden[p.bone] || r.alpha[p.bone] < 0.5f || (!details && p.detail)) continue;
            out.addMatrix(p.mesh, wm, p.bone * 12);
        }
        return out;
    }

    // ------------------------------------------------------------------ shadow

    /**
     * Soft contact shadow: for each shadow group, the bounding-box corners of its pieces are projected onto
     * the ground along the light direction (shortened by lengthScale) and filled as one convex hull.
     * The ground maps 1:1 to the world, so no perspective is needed.
     */
    public void shadow(Rig r, float wx, float wy, float lengthScale, float a){
        if(a <= 0.004f) return;
        float kx = -Light.lx / Light.lz * lengthScale, ky = -Light.ly / Light.lz * lengthScale;
        Draw.color(0f, 0f, 0f, a);
        for(int g = 0; g < r.shadowGroups; g++){
            int n = 0;
            for(int i = 0; i < r.pieceCount && n < pts.length - 32; i++){
                Rig.Piece p = r.pieces[i];
                if(p.shadowGroup != g || r.hidden[p.bone]) continue;
                int o = p.bone * 12;
                boolean round = p.shadowRound;
                float mx = (p.minX + p.maxX) * 0.5f, my = (p.minY + p.maxY) * 0.5f, rx = (p.maxX - p.minX) * 0.5f, ry = (p.maxY - p.minY) * 0.5f;
                for(int c = 0; c < (round ? 16 : 8); c++){
                    float x, y, z;
                    if(round){
                        int k = c & 7;
                        x = mx + rx * RC[k];
                        y = my + ry * RS[k];
                        z = c < 8 ? p.minZ : p.maxZ;
                    }else{
                        x = (c & 1) == 0 ? p.minX : p.maxX; y = (c & 2) == 0 ? p.minY : p.maxY; z = (c & 4) == 0 ? p.minZ : p.maxZ;
                    }
                    float X = wm[o] * x + wm[o + 1] * y + wm[o + 2] * z + wm[o + 3];
                    float Y = wm[o + 4] * x + wm[o + 5] * y + wm[o + 6] * z + wm[o + 7];
                    float Z = Math.max(wm[o + 8] * x + wm[o + 9] * y + wm[o + 10] * z + wm[o + 11], 0f);
                    pts[n++] = wx + X + kx * Z;
                    pts[n++] = wy + Y + ky * Z;
                }
            }
            int h = hull(pts, n / 2);
            if(h >= 3) Fill.poly(hull, h * 2);
        }
        Draw.color();
    }

    /** Andrew's monotone chain into {@link #hull}; returns the vertex count. */
    private int hull(float[] p, int n){
        if(n < 3) return 0;
        //shell sort by x then y (in place, pairs)
        for(int gap = n / 2; gap > 0; gap /= 2){
            for(int i = gap; i < n; i++){
                float x = p[i * 2], y = p[i * 2 + 1];
                int j = i;
                while(j >= gap && (p[(j - gap) * 2] > x || (p[(j - gap) * 2] == x && p[(j - gap) * 2 + 1] > y))){
                    p[j * 2] = p[(j - gap) * 2];
                    p[j * 2 + 1] = p[(j - gap) * 2 + 1];
                    j -= gap;
                }
                p[j * 2] = x; p[j * 2 + 1] = y;
            }
        }
        int k = 0, max = hull.length / 2 - 1;
        for(int i = 0; i < n && k < max; i++){
            while(k >= 2 && cross(hull, k, p[i * 2], p[i * 2 + 1]) <= 0) k--;
            hull[k * 2] = p[i * 2]; hull[k * 2 + 1] = p[i * 2 + 1]; k++;
        }
        for(int i = n - 2, t = k + 1; i >= 0 && k < max; i--){
            while(k >= t && cross(hull, k, p[i * 2], p[i * 2 + 1]) <= 0) k--;
            hull[k * 2] = p[i * 2]; hull[k * 2 + 1] = p[i * 2 + 1]; k++;
        }
        return k - 1;
    }

    private static float cross(float[] h, int k, float x, float y){
        float ox = h[(k - 2) * 2], oy = h[(k - 2) * 2 + 1], ax = h[(k - 1) * 2], ay = h[(k - 1) * 2 + 1];
        return (ax - ox) * (y - oy) - (ay - oy) * (x - ox);
    }
}
