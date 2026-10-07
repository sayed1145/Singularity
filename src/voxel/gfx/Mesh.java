package voxel.gfx;

import arc.graphics.*;

/**
 * Compact quad mesh with a small modelling toolkit (bevelled boxes, lathes, tubes, arbitrary quads).
 *
 * <p>Every face is stored with an outward winding, so the live renderer can derive exact normals after any
 * rotation, and the baker can shade it identically. Triangles are stored as quads with a repeated last vertex.
 */
public class Mesh{
    public static final int emissive = 1, metal = 2, glass = 4, team = 8;
    public static final int matPlain = 0, matPlate = 1, matConcrete = 2, matGrate = 3, matHazard = 4,
        matBolts = 5, matGlass = 6, matSocket = 7, matWater = 8, matRubber = 9, matFins = 10, matStripe = 11;

    public int verts, faces;
    public float[] vx = new float[64], vy = new float[64], vz = new float[64];
    public int[] f0 = new int[32], f1 = new int[32], f2 = new int[32], f3 = new int[32], flags = new int[32], mat = new int[32];
    public float[] cr = new float[32], cg = new float[32], cb = new float[32];

    //current modelling transform: p' = R p + T
    private float m00 = 1, m01, m02, m10, m11 = 1, m12, m20, m21, m22 = 1, tx, ty, tz;
    private float colR = 1, colG = 1, colB = 1;
    private int curFlags, curMat;

    // ---------------------------------------------------------------- state

    public Mesh at(float x, float y, float z){
        m00 = 1; m01 = 0; m02 = 0; m10 = 0; m11 = 1; m12 = 0; m20 = 0; m21 = 0; m22 = 1;
        tx = x; ty = y; tz = z;
        return this;
    }

    /** Post-multiplies a local rotation (axis 0=x, 1=y, 2=z). */
    public Mesh rot(int axis, float deg){
        float c = (float)Math.cos(Math.toRadians(deg)), s = (float)Math.sin(Math.toRadians(deg));
        float a00, a01, a02, a10, a11, a12, a20, a21, a22;
        if(axis == 0){ a00 = 1; a01 = 0; a02 = 0; a10 = 0; a11 = c; a12 = -s; a20 = 0; a21 = s; a22 = c; }
        else if(axis == 1){ a00 = c; a01 = 0; a02 = s; a10 = 0; a11 = 1; a12 = 0; a20 = -s; a21 = 0; a22 = c; }
        else{ a00 = c; a01 = -s; a02 = 0; a10 = s; a11 = c; a12 = 0; a20 = 0; a21 = 0; a22 = 1; }
        float n00 = m00 * a00 + m01 * a10 + m02 * a20, n01 = m00 * a01 + m01 * a11 + m02 * a21, n02 = m00 * a02 + m01 * a12 + m02 * a22;
        float n10 = m10 * a00 + m11 * a10 + m12 * a20, n11 = m10 * a01 + m11 * a11 + m12 * a21, n12 = m10 * a02 + m11 * a12 + m12 * a22;
        float n20 = m20 * a00 + m21 * a10 + m22 * a20, n21 = m20 * a01 + m21 * a11 + m22 * a21, n22 = m20 * a02 + m21 * a12 + m22 * a22;
        m00 = n00; m01 = n01; m02 = n02; m10 = n10; m11 = n11; m12 = n12; m20 = n20; m21 = n21; m22 = n22;
        return this;
    }

    /** Places the local frame at p0 with local +z pointing to p1 (for pipes and rods). */
    public Mesh axis(float x0, float y0, float z0, float x1, float y1, float z1){
        at(x0, y0, z0);
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float l = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= l; dy /= l; dz /= l;
        //pick a helper that is not parallel
        float hx = Math.abs(dz) < 0.9f ? 0f : 1f, hy = 0f, hz = Math.abs(dz) < 0.9f ? 1f : 0f;
        float ux = hy * dz - hz * dy, uy = hz * dx - hx * dz, uz = hx * dy - hy * dx;
        float ul = (float)Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul; uy /= ul; uz /= ul;
        float wx = dy * uz - dz * uy, wy = dz * ux - dx * uz, wz = dx * uy - dy * ux;
        m00 = ux; m01 = wx; m02 = dx;
        m10 = uy; m11 = wy; m12 = dy;
        m20 = uz; m21 = wz; m22 = dz;
        return this;
    }

    public Mesh color(Color c){
        colR = c.r; colG = c.g; colB = c.b;
        return this;
    }

    public Mesh color(float r, float g, float b){
        colR = r; colG = g; colB = b;
        return this;
    }

    public Mesh style(int flags, int mat){
        curFlags = flags; curMat = mat;
        return this;
    }

    public Mesh plain(){
        return style(0, matPlain);
    }

    // ---------------------------------------------------------------- raw

    public int vert(float x, float y, float z){
        if(verts == vx.length){
            vx = grow(vx); vy = grow(vy); vz = grow(vz);
        }
        vx[verts] = m00 * x + m01 * y + m02 * z + tx;
        vy[verts] = m10 * x + m11 * y + m12 * z + ty;
        vz[verts] = m20 * x + m21 * y + m22 * z + tz;
        return verts++;
    }

    /** Adds a face; the winding is corrected so the normal agrees with the expected local normal (ex,ey,ez). */
    public void face(int a, int b, int c, int d, float ex, float ey, float ez){
        float wx = m00 * ex + m01 * ey + m02 * ez, wy = m10 * ex + m11 * ey + m12 * ez, wz = m20 * ex + m21 * ey + m22 * ez;
        float nx = 0, ny = 0, nz = 0;
        int[] q = {a, b, c, d};
        for(int i = 0; i < 4; i++){
            int p = q[i], n = q[(i + 1) & 3];
            nx += (vy[p] - vy[n]) * (vz[p] + vz[n]);
            ny += (vz[p] - vz[n]) * (vx[p] + vx[n]);
            nz += (vx[p] - vx[n]) * (vy[p] + vy[n]);
        }
        if(nx * wx + ny * wy + nz * wz < 0f){
            if(c == d){ int t = a; a = c; c = t; d = c; }
            else{ int t = b; b = d; d = t; }
        }
        rawFace(a, b, c, d);
    }

    public void rawFace(int a, int b, int c, int d){
        if(faces == f0.length){
            f0 = grow(f0); f1 = grow(f1); f2 = grow(f2); f3 = grow(f3); flags = grow(flags); mat = grow(mat);
            cr = grow(cr); cg = grow(cg); cb = grow(cb);
        }
        f0[faces] = a; f1[faces] = b; f2[faces] = c; f3[faces] = d;
        flags[faces] = curFlags; mat[faces] = curMat;
        cr[faces] = colR; cg[faces] = colG; cb[faces] = colB;
        faces++;
    }

    // ---------------------------------------------------------------- primitives

    /** Axis-aligned box in local space. bottom=false skips the (never visible) bottom face. */
    public Mesh box(float x0, float y0, float z0, float x1, float y1, float z1, boolean bottom){
        int a = vert(x0, y0, z0), b = vert(x1, y0, z0), c = vert(x1, y1, z0), d = vert(x0, y1, z0);
        int e = vert(x0, y0, z1), f = vert(x1, y0, z1), g = vert(x1, y1, z1), h = vert(x0, y1, z1);
        face(e, f, g, h, 0, 0, 1);
        face(a, b, f, e, 0, -1, 0);
        face(c, d, h, g, 0, 1, 0);
        face(b, c, g, f, 1, 0, 0);
        face(d, a, e, h, -1, 0, 0);
        if(bottom) face(a, b, c, d, 0, 0, -1);
        return this;
    }

    public Mesh box(float x0, float y0, float z0, float x1, float y1, float z1){
        return box(x0, y0, z0, x1, y1, z1, false);
    }

    /** Centered box helper: centre x,y, bottom z, size w,d,h. */
    public Mesh cbox(float x, float y, float z, float w, float d, float h){
        return box(x - w / 2f, y - d / 2f, z, x + w / 2f, y + d / 2f, z + h, false);
    }

    /** Box with a chamfered top edge of width b, which catches a thin highlight like machined steel. */
    public Mesh bevel(float x0, float y0, float z0, float x1, float y1, float z1, float b){
        float zm = z1 - b;
        int a = vert(x0, y0, z0), bb = vert(x1, y0, z0), c = vert(x1, y1, z0), d = vert(x0, y1, z0);
        int e = vert(x0, y0, zm), f = vert(x1, y0, zm), g = vert(x1, y1, zm), h = vert(x0, y1, zm);
        int E = vert(x0 + b, y0 + b, z1), F = vert(x1 - b, y0 + b, z1), G = vert(x1 - b, y1 - b, z1), H = vert(x0 + b, y1 - b, z1);
        face(E, F, G, H, 0, 0, 1);
        face(a, bb, f, e, 0, -1, 0);
        face(c, d, h, g, 0, 1, 0);
        face(bb, c, g, f, 1, 0, 0);
        face(d, a, e, h, -1, 0, 0);
        face(e, f, F, E, 0, -1, 1);
        face(g, h, H, G, 0, 1, 1);
        face(f, g, G, F, 1, 0, 1);
        face(h, e, E, H, -1, 0, 1);
        return this;
    }

    /**
     * Convex hexahedron from 8 corners: bottom quad a,b,c,d then top quad e,f,g,h (e above a, ...).
     * Face orientation is derived from the solid's centroid, so any convex armour plate / wedge works.
     * bottom=false skips the bottom face.
     */
    public Mesh hexa(boolean bottom, float... p){
        int[] v = new int[8];
        float cx = 0, cy = 0, cz = 0;
        for(int i = 0; i < 8; i++){
            v[i] = vert(p[i * 3], p[i * 3 + 1], p[i * 3 + 2]);
            cx += p[i * 3]; cy += p[i * 3 + 1]; cz += p[i * 3 + 2];
        }
        cx /= 8f; cy /= 8f; cz /= 8f;
        int[][] q = {{4, 5, 6, 7}, {0, 1, 5, 4}, {1, 2, 6, 5}, {2, 3, 7, 6}, {3, 0, 4, 7}, {0, 1, 2, 3}};
        for(int k = 0; k < (bottom ? 6 : 5); k++){
            int[] f = q[k];
            float fx = 0, fy = 0, fz = 0;
            for(int i : f){ fx += p[i * 3]; fy += p[i * 3 + 1]; fz += p[i * 3 + 2]; }
            //expected normal in local (pre-transform) space: from the solid centre to the face centre
            face(v[f[0]], v[f[1]], v[f[2]], v[f[3]], fx / 4f - cx, fy / 4f - cy, fz / 4f - cz);
        }
        return this;
    }

    /**
     * Tapered block (a wedge/frustum): bottom rectangle w0 x d0 centred at (x,y) on z0, top rectangle w1 x d1
     * centred at (x+sx, y+sy) on z1. The workhorse for sloped, clean armour.
     */
    public Mesh taper(float x, float y, float z0, float w0, float d0, float z1, float w1, float d1, float sx, float sy, boolean bottom){
        float a = w0 / 2f, b = d0 / 2f, c = w1 / 2f, d = d1 / 2f, X = x + sx, Y = y + sy;
        return hexa(bottom,
            x - a, y - b, z0, x + a, y - b, z0, x + a, y + b, z0, x - a, y + b, z0,
            X - c, Y - d, z1, X + c, Y - d, z1, X + c, Y + d, z1, X - c, Y + d, z1);
    }

    public Mesh cbevel(float x, float y, float z, float w, float d, float h, float b){
        return bevel(x - w / 2f, y - d / 2f, z, x + w / 2f, y + d / 2f, z + h, b);
    }

    /**
     * Solid of revolution around local z. The (r,z) profile must be counter-clockwise (right = +r, up = +z);
     * points with r = 0 close the solid with a fan.
     */
    public Mesh lathe(int sides, float angleOffset, float... rz){
        int n = rz.length / 2;
        for(int k = 0; k < n - 1; k++){
            float r0 = rz[k * 2], z0 = rz[k * 2 + 1], r1 = rz[k * 2 + 2], z1 = rz[k * 2 + 3];
            if(r0 <= 0f && r1 <= 0f) continue;
            float dr = r1 - r0, dz = z1 - z0;
            for(int i = 0; i < sides; i++){
                float a0 = (float)Math.toRadians(angleOffset + i * 360f / sides), a1 = (float)Math.toRadians(angleOffset + (i + 1) * 360f / sides);
                float c0 = (float)Math.cos(a0), s0 = (float)Math.sin(a0), c1 = (float)Math.cos(a1), s1 = (float)Math.sin(a1);
                float cm = (float)Math.cos((a0 + a1) / 2f), sm = (float)Math.sin((a0 + a1) / 2f);
                //outward normal in the (r,z) plane of a ccw profile edge is (dz, -dr)
                float ex = dz * cm, ey = dz * sm, ez = -dr;
                if(r0 <= 0f){
                    int p = vert(0, 0, z0), q = vert(r1 * c0, r1 * s0, z1), w = vert(r1 * c1, r1 * s1, z1);
                    face(p, q, w, w, ex, ey, ez);
                }else if(r1 <= 0f){
                    int p = vert(r0 * c0, r0 * s0, z0), q = vert(r0 * c1, r0 * s1, z0), w = vert(0, 0, z1);
                    face(p, q, w, w, ex, ey, ez);
                }else{
                    int p = vert(r0 * c0, r0 * s0, z0), q = vert(r0 * c1, r0 * s1, z0), w = vert(r1 * c1, r1 * s1, z1), u = vert(r1 * c0, r1 * s0, z1);
                    face(p, q, w, u, ex, ey, ez);
                }
            }
        }
        return this;
    }

    /** Closed cylinder along local z from z0 to z1. */
    public Mesh cyl(int sides, float r, float z0, float z1){
        return lathe(sides, 180f / sides, 0, z0, r, z0, r, z1, 0, z1);
    }

    /** Cylinder without caps (e.g. a shaft whose ends are hidden). */
    public Mesh tubeSide(int sides, float r, float z0, float z1){
        return lathe(sides, 180f / sides, r, z0, r, z1);
    }

    /** Thick ring (annulus) from z0 to z1. */
    public Mesh ring(int sides, float rin, float rout, float z0, float z1){
        return lathe(sides, 180f / sides, rin, z0, rout, z0, rout, z1, rin, z1, rin, z0);
    }

    /** Pipe between two points. */
    public Mesh pipe(int sides, float r, float x0, float y0, float z0, float x1, float y1, float z1){
        float l = (float)Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));
        axis(x0, y0, z0, x1, y1, z1);
        return tubeSide(sides, r, 0, l);
    }

    /** Arbitrary planar quad in local space with an expected normal. */
    public Mesh quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                     float ex, float ey, float ez){
        face(vert(ax, ay, az), vert(bx, by, bz), vert(cx, cy, cz), vert(dx, dy, dz), ex, ey, ez);
        return this;
    }

    /** Appends another mesh with a rigid transform (used for rest-pose icons of live pieces). */
    public Mesh add(Mesh src, float ox, float oy, float oz, int axisId, float deg){
        at(ox, oy, oz);
        if(deg != 0f) rot(axisId, deg);
        int base = verts;
        for(int i = 0; i < src.verts; i++) vert(src.vx[i], src.vy[i], src.vz[i]);
        for(int f = 0; f < src.faces; f++){
            curFlags = src.flags[f]; curMat = src.mat[f];
            colR = src.cr[f]; colG = src.cg[f]; colB = src.cb[f];
            rawFace(base + src.f0[f], base + src.f1[f], base + src.f2[f], base + src.f3[f]);
        }
        at(0, 0, 0);
        return this;
    }

    /** Appends src transformed by the 3x4 row-major matrix w[o..o+11] (used to flatten posed rigs for baking). */
    public Mesh addMatrix(Mesh src, float[] w, int o){
        at(0, 0, 0);
        int base = verts;
        for(int i = 0; i < src.verts; i++){
            float x = src.vx[i], y = src.vy[i], z = src.vz[i];
            vert(w[o] * x + w[o + 1] * y + w[o + 2] * z + w[o + 3],
                w[o + 4] * x + w[o + 5] * y + w[o + 6] * z + w[o + 7],
                w[o + 8] * x + w[o + 9] * y + w[o + 10] * z + w[o + 11]);
        }
        for(int f = 0; f < src.faces; f++){
            curFlags = src.flags[f]; curMat = src.mat[f];
            colR = src.cr[f]; colG = src.cg[f]; colB = src.cb[f];
            rawFace(base + src.f0[f], base + src.f1[f], base + src.f2[f], base + src.f3[f]);
        }
        return this;
    }

    public float maxZ(){
        float m = -1e9f;
        for(int i = 0; i < verts; i++) m = Math.max(m, vz[i]);
        return m;
    }

    private static float[] grow(float[] a){
        float[] b = new float[a.length * 2];
        System.arraycopy(a, 0, b, 0, a.length);
        return b;
    }

    private static int[] grow(int[] a){
        int[] b = new int[a.length * 2];
        System.arraycopy(a, 0, b, 0, a.length);
        return b;
    }
}
