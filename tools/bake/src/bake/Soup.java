package bake;

import blackhole.g3d.Mesh;

/**
 * Flat triangle soup with per-triangle colour / flags / material and a part id, filled from {@link Mesh}es
 * (quads split in two, exactly like the live renderer draws them, so baked and live shading agree).
 */
public final class Soup{
    public float[] pos = new float[9 * 4096];
    public float[] col = new float[3 * 4096];
    public int[] flags = new int[4096], mat = new int[4096], part = new int[4096];
    /** face mid z (for the live renderer's vertical gradient) */
    public float[] midZ = new float[4096];
    public int tris;
    public float minX = 1e9f, minY = 1e9f, minZ = 1e9f, maxX = -1e9f, maxY = -1e9f, maxZ = -1e9f;

    private final float[] m = new float[12];
    private int curPart = -1;

    public Soup(){
        identity();
    }

    public void clear(){
        tris = 0;
        minX = minY = minZ = 1e9f;
        maxX = maxY = maxZ = -1e9f;
        identity();
    }

    public void identity(){
        for(int i = 0; i < 12; i++) m[i] = 0f;
        m[0] = m[5] = m[10] = 1f;
    }

    /** rotation about one axis through the origin then translation (same convention as Live.pose / Mesh.add) */
    public void pose(float x, float y, float z, int axis, float deg){
        double c = Math.cos(Math.toRadians(deg)), s = Math.sin(Math.toRadians(deg));
        identity();
        if(axis == 0){ m[5] = (float)c; m[6] = (float)-s; m[9] = (float)s; m[10] = (float)c; }
        else if(axis == 1){ m[0] = (float)c; m[2] = (float)s; m[8] = (float)-s; m[10] = (float)c; }
        else{ m[0] = (float)c; m[1] = (float)-s; m[4] = (float)s; m[5] = (float)c; }
        m[3] = x; m[7] = y; m[11] = z;
    }

    public void matrix(float[] w, int o){
        System.arraycopy(w, o, m, 0, 12);
    }

    public void part(int id){
        curPart = id;
    }

    public void add(Mesh s){
        for(int f = 0; f < s.faces; f++){
            int a = s.f0[f], b = s.f1[f], c = s.f2[f], d = s.f3[f];
            float mz = (s.vz[a] + s.vz[b] + s.vz[c] + s.vz[d]) * 0.25f;
            tri(s, a, b, c, f, mz);
            if(d != c && d != a) tri(s, a, c, d, f, mz);
        }
    }

    private void tri(Mesh s, int a, int b, int c, int f, float localMidZ){
        if(tris == flags.length){
            int n = tris * 2;
            pos = java.util.Arrays.copyOf(pos, n * 9); col = java.util.Arrays.copyOf(col, n * 3);
            flags = java.util.Arrays.copyOf(flags, n); mat = java.util.Arrays.copyOf(mat, n); part = java.util.Arrays.copyOf(part, n);
            midZ = java.util.Arrays.copyOf(midZ, n);
        }
        int o = tris * 9;
        int[] v = {a, b, c};
        for(int k = 0; k < 3; k++){
            float x = s.vx[v[k]], y = s.vy[v[k]], z = s.vz[v[k]];
            float X = m[0] * x + m[1] * y + m[2] * z + m[3];
            float Y = m[4] * x + m[5] * y + m[6] * z + m[7];
            float Z = m[8] * x + m[9] * y + m[10] * z + m[11];
            pos[o + k * 3] = X; pos[o + k * 3 + 1] = Y; pos[o + k * 3 + 2] = Z;
            if(X < minX) minX = X; if(X > maxX) maxX = X;
            if(Y < minY) minY = Y; if(Y > maxY) maxY = Y;
            if(Z < minZ) minZ = Z; if(Z > maxZ) maxZ = Z;
        }
        //mid z of the face in world space (z row of the transform applied to the local mid point is not available
        //without the full quad; use the triangle's own mid z, which differs only for split quads)
        midZ[tris] = (pos[o + 2] + pos[o + 5] + pos[o + 8]) / 3f;
        col[tris * 3] = s.cr[f]; col[tris * 3 + 1] = s.cg[f]; col[tris * 3 + 2] = s.cb[f];
        flags[tris] = s.flags[f];
        mat[tris] = s.mat[f];
        part[tris] = curPart;
        tris++;
    }

    public void append(Soup o){
        for(int t = 0; t < o.tris; t++){
            if(tris == flags.length){
                int n = tris * 2;
                pos = java.util.Arrays.copyOf(pos, n * 9); col = java.util.Arrays.copyOf(col, n * 3);
                flags = java.util.Arrays.copyOf(flags, n); mat = java.util.Arrays.copyOf(mat, n); part = java.util.Arrays.copyOf(part, n);
                midZ = java.util.Arrays.copyOf(midZ, n);
            }
            System.arraycopy(o.pos, t * 9, pos, tris * 9, 9);
            System.arraycopy(o.col, t * 3, col, tris * 3, 3);
            flags[tris] = o.flags[t]; mat[tris] = o.mat[t]; part[tris] = o.part[t]; midZ[tris] = o.midZ[t];
            tris++;
        }
        minX = Math.min(minX, o.minX); minY = Math.min(minY, o.minY); minZ = Math.min(minZ, o.minZ);
        maxX = Math.max(maxX, o.maxX); maxY = Math.max(maxY, o.maxY); maxZ = Math.max(maxZ, o.maxZ);
    }
}
