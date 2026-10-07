package astro.g3d;

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
        if(!legacy){
            if(exact && !lowDetail && Budget.sortAllowed()){ long t0 = System.nanoTime(); boolean ok = drawFaces(r, wx, wy); long dt = System.nanoTime() - t0; exactNanos += dt; Budget.sortNs += dt; if(ok) return; }
            drawSorted(r, wx, wy);
            return;
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
            drawPiece(r, p, wx, wy, order[i]);
        }
    }

    /** old (centroid distance) ordering, kept so the audit tool can compare the two */
    public static boolean legacy;
    /** optional face sink for the audit tool (piece index, projected quad, view-space quad) */
    public interface Sink{
        void face(int piece, float[] px, float[] py, float[] x, float[] y, float[] z);
    }
    public static Sink sink;
    private final float[] sqx = new float[4], sqy = new float[4], sox = new float[4], soy = new float[4], soz = new float[4];

    // ------------------------------------------------------------------ exact ordering by separating planes

    private float[] corner = new float[64 * 24], pnorm = new float[64 * 18], poff = new float[64 * 6], sbox = new float[64 * 4], pkey = new float[64];
    private int[] act = new int[64], eFrom = new int[512], eTo = new int[512], indeg = new int[64], head = new int[64], nextE = new int[512], out = new int[64];
    private boolean[] done = new boolean[64];
    private int edges;
    public static final float SEP_TOL = 0.14f;
    /** statistics: pairs that had no separating plane (resolved by centroid distance) */
    public long ambiguous, pairsTested;

    private void growNodes(int n){
        if(pkey.length >= n) return;
        int cap = Integer.highestOneBit(n) << 1;
        corner = new float[cap * 24]; pnorm = new float[cap * 18]; poff = new float[cap * 6]; sbox = new float[cap * 4]; pkey = new float[cap];
        act = new int[cap]; indeg = new int[cap]; head = new int[cap]; out = new int[cap]; done = new boolean[cap];
        uf = new int[cap]; clusterSize = new int[cap]; nodeKey = new float[cap];
    }

    private void addEdge(int from, int to){
        if(edges == eFrom.length){
            eFrom = java.util.Arrays.copyOf(eFrom, edges * 2); eTo = java.util.Arrays.copyOf(eTo, edges * 2); nextE = java.util.Arrays.copyOf(nextE, edges * 2);
        }
        eFrom[edges] = from; eTo[edges] = to;
        nextE[edges] = head[from]; head[from] = edges;
        indeg[to]++;
        edges++;
    }

    /** 1 = a plane of P has Q entirely beyond it and the camera is outside (P first), 2 = camera inside (Q first), 0 = none */
    private int sep(int P, int Q, float camX, float camY, float camZ){
        int qo = Q * 24;
        for(int pl = 0; pl < 6; pl++){
            int po = P * 18 + pl * 3;
            float nx = pnorm[po], ny = pnorm[po + 1], nz = pnorm[po + 2], d = poff[P * 6 + pl] - SEP_TOL;
            boolean ok = true;
            for(int c = 0; c < 8; c++){
                if(nx * corner[qo + c * 3] + ny * corner[qo + c * 3 + 1] + nz * corner[qo + c * 3 + 2] < d){ ok = false; break; }
            }
            if(ok) return nx * camX + ny * camY + nz * camZ > poff[P * 6 + pl] ? 1 : 2;
        }
        return 0;
    }

    /** exact face level ordering (used when the unit is big on screen); false falls back to the piece level order */
    public boolean exact = true;
    /** visible face count above which the exact sort is skipped */
    public static int maxExactFaces = 2400;
    public long exactFrames, exactFaces, exactPairs, exactEdges;

    private float[] xV = new float[256 * 12];
    private int[] xOrder = new int[256], xHeap = new int[256];
    private float[] fZ0 = new float[256], fZ1 = new float[256];
    private long[] xSort = new long[256];
    private int[] xActive = new int[256];

    public long scans, exactNanos, tCollect, tSweep, exactCycles;
    private boolean drawFaces(Rig r, float wx, float wy){
        currentRig = r;
        long tStart = System.nanoTime();
        int n = r.pieceCount;
        ensureCache(n);
        fN = 0;
        float D = cam.D, cy = cam.cy;
        for(int pi = 0; pi < n; pi++){
            Rig.Piece p = r.pieces[pi];
            if(r.hidden[p.bone] || r.alpha[p.bone] * alpha <= 0.004f) continue;
            transform(r, pi, wx, wy);
            Mesh mesh = p.mesh;
            float[] tx = cx[pi], ty = cy_[pi], tz = cz[pi], px = cpx[pi], py = cpy[pi];
            for(int f = 0; f < mesh.faces; f++){
                if((mesh.flags[f] & Mesh.buried) != 0) continue;
                int a = mesh.f0[f], b = mesh.f1[f], c = mesh.f2[f], d = mesh.f3[f];
                float e1x = tx[b] - tx[a], e1y = ty[b] - ty[a], e1z = tz[b] - tz[a];
                float e2x = tx[c] - tx[b], e2y = ty[c] - ty[b], e2z = tz[c] - tz[b];
                float nx = e1y * e2z - e1z * e2y, ny = e1z * e2x - e1x * e2z, nz = e1x * e2y - e1y * e2x;
                if(!cam.facing(nx, ny, nz, tx[a], ty[a], tz[a])) continue;
                float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
                if(len < 1e-7f) continue;
                if(fN == fPiece.length){
                    int cap = fN * 2;
                    fPiece = java.util.Arrays.copyOf(fPiece, cap); fFace = java.util.Arrays.copyOf(fFace, cap); fIn = new int[cap]; fHead = new int[cap];
                    fPl = java.util.Arrays.copyOf(fPl, cap * 4); fBox = java.util.Arrays.copyOf(fBox, cap * 4); fKey = java.util.Arrays.copyOf(fKey, cap); fDone = new boolean[cap]; fZ0 = new float[cap]; fZ1 = new float[cap];
                }
                if(fN == xSort.length){
                    int cap = fN * 2;
                    xSort = new long[cap]; xOrder = new int[cap]; xHeap = new int[cap]; xActive = new int[cap]; xV = java.util.Arrays.copyOf(xV, cap * 12);
                }
                if(fN >= maxExactFaces) return false;
                int i = fN++;
                fPiece[i] = pi; fFace[i] = f;
                nx /= len; ny /= len; nz /= len;
                fPl[i * 4] = nx; fPl[i * 4 + 1] = ny; fPl[i * 4 + 2] = nz; fPl[i * 4 + 3] = nx * tx[a] + ny * ty[a] + nz * tz[a];
                fBox[i * 4] = Math.min(Math.min(px[a], px[b]), Math.min(px[c], px[d])); fBox[i * 4 + 1] = Math.min(Math.min(py[a], py[b]), Math.min(py[c], py[d]));
                fBox[i * 4 + 2] = Math.max(Math.max(px[a], px[b]), Math.max(px[c], px[d])); fBox[i * 4 + 3] = Math.max(Math.max(py[a], py[b]), Math.max(py[c], py[d]));
                float kx = (tx[a] + tx[b] + tx[c] + tx[d]) * 0.25f, ky = (ty[a] + ty[b] + ty[c] + ty[d]) * 0.25f, kz = (tz[a] + tz[b] + tz[c] + tz[d]) * 0.25f;
                fZ0[i] = Math.min(Math.min(tz[a], tz[b]), Math.min(tz[c], tz[d])); fZ1[i] = Math.max(Math.max(tz[a], tz[b]), Math.max(tz[c], tz[d]));
                fKey[i] = -kz + 1e-4f * (kx * kx + (ky - cy) * (ky - cy));
                int o = i * 12;
                xV[o] = tx[a]; xV[o + 1] = ty[a]; xV[o + 2] = tz[a]; xV[o + 3] = tx[b]; xV[o + 4] = ty[b]; xV[o + 5] = tz[b];
                xV[o + 6] = tx[c]; xV[o + 7] = ty[c]; xV[o + 8] = tz[c]; xV[o + 9] = tx[d]; xV[o + 10] = ty[d]; xV[o + 11] = tz[d];
            }
        }
        exactFrames++; exactFaces += fN; long tA = System.nanoTime(); tCollect += tA - tStart;
        for(int i = 0; i < fN; i++){ fHead[i] = -1; fIn[i] = 0; fDone[i] = false; }
        fEdges = 0;
        //sweep and prune on the screen x extent
        for(int i = 0; i < fN; i++){
            xSort[i] = ((long)(Float.floatToIntBits(fBox[i * 4]) ^ ((Float.floatToIntBits(fBox[i * 4]) >> 31) & 0x7fffffff)) << 32) | i;
        }
        java.util.Arrays.sort(xSort, 0, fN);
        int act2 = 0;
        for(int s = 0; s < fN; s++){
            int a = (int)(xSort[s] & 0xffffffffL);
            float ax0 = fBox[a * 4], ay0 = fBox[a * 4 + 1], ay1 = fBox[a * 4 + 3];
            int w = 0;
            for(int q = 0; q < act2; q++){
                int b = xActive[q]; scans++;
                if(fBox[b * 4 + 2] < ax0) continue; //expired
                xActive[w++] = b;
                if(fBox[b * 4 + 1] > ay1 || fBox[b * 4 + 3] < ay0) continue;
                if(fPiece[a] == fPiece[b]) continue;
                exactPairs++;
                //NOTE: a "z ranges are disjoint" shortcut is NOT valid here (measured 14% wrong pixels), always do the plane test
                int res = faceSep(a, b);
                if(res == 0) res = -faceSep(b, a);
                if(res == 0){
                    Rig.Piece pa = r.pieces[fPiece[a]], pb = r.pieces[fPiece[b]];
                    if(pb.attach == fPiece[a]) res = pb.attachOrder < 0 ? 1 : -1;
                    else if(pa.attach == fPiece[b]) res = pa.attachOrder < 0 ? -1 : 1;
                    else if(pa.attach >= 0 && pa.attach == pb.attach) res = pa.attachOrder > pb.attachOrder ? 1 : -1;
                }
                if(res > 0) faceEdge(a, b); else if(res < 0) faceEdge(b, a);
            }
            xActive[w++] = a;
            act2 = w;
        }
        exactEdges += fEdges; long tB = System.nanoTime(); tSweep += tB - tA;
        //Kahn with a max-heap on the distance key (farthest first)
        int hn = 0;
        for(int i = 0; i < fN; i++){
            if(fIn[i] == 0) hn = heapPush(hn, i);
            int kb = Float.floatToIntBits(fKey[i]);
            xSort[i] = ((long)(~(kb ^ ((kb >> 31) & 0x7fffffff))) << 32) | i; //descending key
        }
        java.util.Arrays.sort(xSort, 0, fN);
        int cursor = 0;
        int done = 0;
        while(done < fN){
            int pick;
            if(hn > 0){
                pick = xHeap[0];
                hn = heapPop(hn);
            }else{
                //cycle: break it at the farthest remaining face
                pick = -1; exactCycles++;
                while(cursor < fN && fDone[(int)(xSort[cursor] & 0xffffffffL)]) cursor++;
                pick = (int)(xSort[cursor] & 0xffffffffL);
            }
            if(fDone[pick]) continue;
            fDone[pick] = true;
            done++;
            for(int e = fHead[pick]; e >= 0; e = fNext[e]){
                int t = fTo[e];
                if(--fIn[t] == 0 && !fDone[t]) hn = heapPush(hn, t);
            }
            int pi = fPiece[pick];
            emitFace(r, r.pieces[pi], pi, fFace[pick]);
        }
        return true;
    }

    private int heapPush(int hn, int v){
        int i = hn;
        xHeap[hn++] = v;
        while(i > 0){
            int p = (i - 1) >> 1;
            if(fKey[xHeap[p]] >= fKey[xHeap[i]]) break;
            int t = xHeap[p]; xHeap[p] = xHeap[i]; xHeap[i] = t;
            i = p;
        }
        return hn;
    }

    private int heapPop(int hn){
        hn--;
        xHeap[0] = xHeap[hn];
        int i = 0;
        while(true){
            int l = i * 2 + 1, rr = l + 1, m = i;
            if(l < hn && fKey[xHeap[l]] > fKey[xHeap[m]]) m = l;
            if(rr < hn && fKey[xHeap[rr]] > fKey[xHeap[m]]) m = rr;
            if(m == i) break;
            int t = xHeap[m]; xHeap[m] = xHeap[i]; xHeap[i] = t;
            i = m;
        }
        return hn;
    }

    private Rig currentRig;

    private void drawSorted(Rig r, float wx, float wy){
        currentRig = r;
        int n = r.pieceCount;
        growNodes(n);
        ensureCache(n);
        float D = cam.D, cy = cam.cy;
        int m = 0;
        for(int i = 0; i < n; i++){
            Rig.Piece p = r.pieces[i];
            if(r.hidden[p.bone] || (lowDetail && p.detail)) continue;
            if(r.alpha[p.bone] * alpha <= 0.004f) continue;
            int o = p.bone * 12;
            int k = m++;
            act[k] = i;
            float mx = (p.minX + p.maxX) * 0.5f, my = (p.minY + p.maxY) * 0.5f, mz = (p.minZ + p.maxZ) * 0.5f;
            float hx = (p.maxX - p.minX) * 0.5f, hy = (p.maxY - p.minY) * 0.5f, hz = (p.maxZ - p.minZ) * 0.5f;
            float cX = wm[o] * mx + wm[o + 1] * my + wm[o + 2] * mz + wm[o + 3];
            float cY = wm[o + 4] * mx + wm[o + 5] * my + wm[o + 6] * mz + wm[o + 7];
            float cZ = wm[o + 8] * mx + wm[o + 9] * my + wm[o + 10] * mz + wm[o + 11];
            pkey[k] = cX * cX + (cY - cy) * (cY - cy) + (D - cZ) * (D - cZ);
            float[] hh = {hx, hy, hz};
            float minSX = 1e9f, minSY = 1e9f, maxSX = -1e9f, maxSY = -1e9f;
            for(int ax = 0; ax < 3; ax++){
                float cx = wm[o + ax], cyy = wm[o + 4 + ax], cz = wm[o + 8 + ax];
                float len = (float)Math.sqrt(cx * cx + cyy * cyy + cz * cz);
                float inv = len > 1e-6f ? 1f / len : 0f;
                float nx = cx * inv, ny = cyy * inv, nz = cz * inv;
                float dc = nx * cX + ny * cY + nz * cZ, ext = len * hh[ax];
                int po = k * 18 + ax * 6;
                pnorm[po] = nx; pnorm[po + 1] = ny; pnorm[po + 2] = nz;
                poff[k * 6 + ax * 2] = dc + ext;
                pnorm[po + 3] = -nx; pnorm[po + 4] = -ny; pnorm[po + 5] = -nz;
                poff[k * 6 + ax * 2 + 1] = -dc + ext;
            }
            for(int c = 0; c < 8; c++){
                float sx = (c & 1) == 0 ? -1f : 1f, sy = (c & 2) == 0 ? -1f : 1f, sz = (c & 4) == 0 ? -1f : 1f;
                float X = cX + sx * wm[o] * hx + sy * wm[o + 1] * hy + sz * wm[o + 2] * hz;
                float Y = cY + sx * wm[o + 4] * hx + sy * wm[o + 5] * hy + sz * wm[o + 6] * hz;
                float Z = cZ + sx * wm[o + 8] * hx + sy * wm[o + 9] * hy + sz * wm[o + 10] * hz;
                // the face normals in pnorm are ordered x+,x-,y+,y-,z+,z-: matches the plane tests above
                corner[k * 24 + c * 3] = X; corner[k * 24 + c * 3 + 1] = Y; corner[k * 24 + c * 3 + 2] = Z;
                float s = D / Math.max(D - Z, 1f);
                float psx = X * s, psy = cy + (Y - cy) * s;
                minSX = Math.min(minSX, psx); maxSX = Math.max(maxSX, psx);
                minSY = Math.min(minSY, psy); maxSY = Math.max(maxSY, psy);
            }
            sbox[k * 4] = minSX; sbox[k * 4 + 1] = minSY; sbox[k * 4 + 2] = maxSX; sbox[k * 4 + 3] = maxSY;
        }
        edges = 0;
        java.util.Arrays.fill(head, 0, m, -1);
        java.util.Arrays.fill(indeg, 0, m, 0);
        java.util.Arrays.fill(done, 0, m, false);
        for(int i = 0; i < m; i++) uf[i] = i;
        pairN = 0;
        for(int a = 0; a < m; a++){
            for(int b = a + 1; b < m; b++){
                if(sbox[a * 4] > sbox[b * 4 + 2] || sbox[b * 4] > sbox[a * 4 + 2] || sbox[a * 4 + 1] > sbox[b * 4 + 3] || sbox[b * 4 + 1] > sbox[a * 4 + 3]) continue;
                pairsTested++;
                int rr = sep(a, b, 0f, cy, D);
                int res = 0; //+1: a before b, -1: b before a
                if(rr != 0) res = rr == 1 ? 1 : -1;
                else{
                    rr = sep(b, a, 0f, cy, D);
                    if(rr != 0) res = rr == 1 ? -1 : 1;
                }
                if(res == 0){
                    //mounted decals / inner rods: honour the declared mounting order
                    Rig.Piece pa = r.pieces[act[a]], pb = r.pieces[act[b]];
                    if(pb.attach == act[a]) res = pb.attachOrder < 0 ? 1 : -1;
                    else if(pa.attach == act[b]) res = pa.attachOrder < 0 ? -1 : 1;
                    else if(pa.attach >= 0 && pa.attach == pb.attach) res = pa.attachOrder > pb.attachOrder ? 1 : -1;
                    else{
                        //two pieces that really interpenetrate (a joint): no piece order is right, their faces are sorted together
                        ambiguous++;
                        union(a, b);
                        continue;
                    }
                }
                if(pairN == pairA.length){ pairA = java.util.Arrays.copyOf(pairA, pairN * 2); pairB = java.util.Arrays.copyOf(pairB, pairN * 2); }
                pairA[pairN] = res > 0 ? a : b; pairB[pairN] = res > 0 ? b : a; pairN++;
            }
        }
        //nodes are the union-find roots: a piece, or a cluster of interpenetrating pieces
        for(int i = 0; i < m; i++) nodeKey[i] = -1f;
        for(int i = 0; i < m; i++){ int rt = find(i); nodeKey[rt] = Math.max(nodeKey[rt], pkey[i]); clusterSize[rt] = 0; }
        for(int i = 0; i < m; i++) clusterSize[find(i)]++;
        for(int e = 0; e < pairN; e++){
            int from = find(pairA[e]), to = find(pairB[e]);
            if(from != to) addEdge(from, to);
        }
        int nodes = 0;
        for(int i = 0; i < m; i++) if(find(i) == i) nodes++;
        for(int step = 0; step < nodes; step++){
            int pick = -1;
            for(int i = 0; i < m; i++) if(uf[i] == i && !done[i] && indeg[i] == 0 && (pick < 0 || nodeKey[i] > nodeKey[pick])) pick = i;
            if(pick < 0) for(int i = 0; i < m; i++) if(uf[i] == i && !done[i] && (pick < 0 || nodeKey[i] > nodeKey[pick])) pick = i;
            done[pick] = true;
            for(int e = head[pick]; e >= 0; e = nextE[e]) indeg[eTo[e]]--;
            if(clusterSize[pick] == 1){
                int idx = act[pick];
                drawPiece(r, r.pieces[idx], wx, wy, idx);
            }else{
                drawCluster(r, pick, m, wx, wy);
            }
        }
    }

    private int[] uf = new int[64], pairA = new int[256], pairB = new int[256], clusterSize = new int[64];
    private float[] nodeKey = new float[64];
    private int pairN;
    public long clusters, clusterFaces;

    private int find(int x){
        while(uf[x] != x){ uf[x] = uf[uf[x]]; x = uf[x]; }
        return x;
    }

    private void union(int a, int b){
        a = find(a); b = find(b);
        if(a != b) uf[b] = a;
    }

    // ---- face level sort inside a cluster of interpenetrating pieces
    private int fN;
    private int[] fPiece = new int[256], fFace = new int[256], fIn = new int[256], fHead = new int[256], fNext = new int[1024], fTo = new int[1024];
    private float[] fPl = new float[256 * 4], fBox = new float[256 * 4], fKey = new float[256];
    private boolean[] fDone = new boolean[256];
    private int fEdges;

    private void faceEdge(int from, int to){
        if(fEdges == fTo.length){ fTo = java.util.Arrays.copyOf(fTo, fEdges * 2); fNext = java.util.Arrays.copyOf(fNext, fEdges * 2); }
        fTo[fEdges] = to; fNext[fEdges] = fHead[from]; fHead[from] = fEdges++; fIn[to]++;
    }


    private void drawCluster(Rig r, int root, int m, float wx, float wy){
        clusters++;
        fN = 0;
        float D = cam.D, cy = cam.cy;
        for(int k = 0; k < m; k++){
            if(find(k) != root) continue;
            int pi = act[k];
            Rig.Piece p = r.pieces[pi];
            ensureCache(r.pieceCount);
            transform(r, pi, wx, wy);
            Mesh mesh = p.mesh;
            float[] tx = cx[pi], ty = cy_[pi], tz = cz[pi], px = cpx[pi], py = cpy[pi];
            for(int f = 0; f < mesh.faces; f++){
                if((mesh.flags[f] & Mesh.buried) != 0) continue;
                int a = mesh.f0[f], b = mesh.f1[f], c = mesh.f2[f], d = mesh.f3[f];
                float e1x = tx[b] - tx[a], e1y = ty[b] - ty[a], e1z = tz[b] - tz[a];
                float e2x = tx[c] - tx[b], e2y = ty[c] - ty[b], e2z = tz[c] - tz[b];
                float nx = e1y * e2z - e1z * e2y, ny = e1z * e2x - e1x * e2z, nz = e1x * e2y - e1y * e2x;
                if(!cam.facing(nx, ny, nz, tx[a], ty[a], tz[a])) continue;
                float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
                if(len < 1e-7f) continue;
                if(fN == fPiece.length){
                    int cap = fN * 2;
                    fPiece = java.util.Arrays.copyOf(fPiece, cap); fFace = java.util.Arrays.copyOf(fFace, cap); fIn = new int[cap]; fHead = new int[cap];
                    fPl = java.util.Arrays.copyOf(fPl, cap * 4); fBox = java.util.Arrays.copyOf(fBox, cap * 4); fKey = java.util.Arrays.copyOf(fKey, cap); fDone = new boolean[cap];
                }
                int i = fN++;
                fPiece[i] = pi; fFace[i] = f;
                nx /= len; ny /= len; nz /= len;
                fPl[i * 4] = nx; fPl[i * 4 + 1] = ny; fPl[i * 4 + 2] = nz; fPl[i * 4 + 3] = nx * tx[a] + ny * ty[a] + nz * tz[a];
                fBox[i * 4] = Math.min(Math.min(px[a], px[b]), Math.min(px[c], px[d])); fBox[i * 4 + 1] = Math.min(Math.min(py[a], py[b]), Math.min(py[c], py[d]));
                fBox[i * 4 + 2] = Math.max(Math.max(px[a], px[b]), Math.max(px[c], px[d])); fBox[i * 4 + 3] = Math.max(Math.max(py[a], py[b]), Math.max(py[c], py[d]));
                float kx = (tx[a] + tx[b] + tx[c] + tx[d]) * 0.25f, ky = (ty[a] + ty[b] + ty[c] + ty[d]) * 0.25f, kz = (tz[a] + tz[b] + tz[c] + tz[d]) * 0.25f;
                fKey[i] = kx * kx + (ky - cy) * (ky - cy) + (D - kz) * (D - kz);
            }
        }
        clusterFaces += fN;
        Arrays_fill(fHead, fN, -1); Arrays_fill(fIn, fN, 0);
        fEdges = 0;
        for(int i = 0; i < fN; i++) fDone[i] = false;
        for(int a = 0; a < fN; a++){
            for(int b = a + 1; b < fN; b++){
                if(fBox[a * 4] > fBox[b * 4 + 2] || fBox[b * 4] > fBox[a * 4 + 2] || fBox[a * 4 + 1] > fBox[b * 4 + 3] || fBox[b * 4 + 1] > fBox[a * 4 + 3]) continue;
                int res = faceSep(a, b);
                if(res == 0){ res = -faceSep(b, a); }
                if(res > 0) faceEdge(a, b); else if(res < 0) faceEdge(b, a);
            }
        }
        for(int step = 0; step < fN; step++){
            int pick = -1;
            for(int i = 0; i < fN; i++) if(!fDone[i] && fIn[i] == 0 && (pick < 0 || fKey[i] > fKey[pick])) pick = i;
            if(pick < 0) for(int i = 0; i < fN; i++) if(!fDone[i] && (pick < 0 || fKey[i] > fKey[pick])) pick = i;
            fDone[pick] = true;
            for(int e = fHead[pick]; e >= 0; e = fNext[e]) fIn[fTo[e]]--;
            int pi = fPiece[pick];
            emitFace(r, r.pieces[pi], pi, fFace[pick]);
        }
    }

    private static void Arrays_fill(int[] a, int n, int v){
        for(int i = 0; i < n; i++) a[i] = v;
    }

    /** +1: face a must be drawn before b, -1: b before a (by a's plane, b's vertices), 0: undecided */
    private int faceSep(int a, int b){
        float nx = fPl[a * 4], ny = fPl[a * 4 + 1], nz = fPl[a * 4 + 2], d = fPl[a * 4 + 3];
        float hi = -1e9f, lo = -1e9f; //hi: how far b sticks out of a's plane, lo: how deep it sinks into it
        int o = b * 12;
        for(int v = 0; v < 4; v++){
            float sd = nx * xV[o + v * 3] + ny * xV[o + v * 3 + 1] + nz * xV[o + v * 3 + 2] - d;
            if(sd > hi) hi = sd;
            if(-sd > lo) lo = -sd;
        }
        final float small = 0.02f, big = 0.75f;
        if(hi < small && lo < small) return 0; //coplanar
        //face a is visible: the camera is on its outer side. b (almost) entirely outside: b is in front of a
        if(lo <= big && hi > lo) return 1;
        if(hi <= big && lo > hi) return -1;
        return 0; //the faces cut through each other
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

    // per piece transformed vertices (world, camera relative) and projected positions of the current frame
    private float[][] cx = new float[0][], cy_ = new float[0][], cz = new float[0][], cpx = new float[0][], cpy = new float[0][];
    private boolean[] cached = new boolean[0];

    private void ensureCache(int n){
        if(cx.length >= n) return;
        cx = java.util.Arrays.copyOf(cx, n); cy_ = java.util.Arrays.copyOf(cy_, n); cz = java.util.Arrays.copyOf(cz, n);
        cpx = java.util.Arrays.copyOf(cpx, n); cpy = java.util.Arrays.copyOf(cpy, n);
        cached = new boolean[n];
    }

    private void transform(Rig r, int pi, float wx, float wy){
        Rig.Piece p = r.pieces[pi];
        Mesh m = p.mesh;
        if(cx[pi] == null || cx[pi].length < m.verts){
            cx[pi] = new float[m.verts]; cy_[pi] = new float[m.verts]; cz[pi] = new float[m.verts];
            cpx[pi] = new float[m.verts]; cpy[pi] = new float[m.verts];
        }
        float[] tx = cx[pi], ty = cy_[pi], tz = cz[pi], px = cpx[pi], py = cpy[pi];
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
    }

    private void drawPiece(Rig r, Rig.Piece p, float wx, float wy, int pieceIndex){
        ensureCache(r.pieceCount);
        transform(r, pieceIndex, wx, wy);
        if(alpha * r.alpha[p.bone] <= 0.004f) return;
        for(int f = 0; f < p.mesh.faces; f++) emitFace(r, p, pieceIndex, f);
    }

    private void emitFace(Rig r, Rig.Piece p, int pieceIndex, int f){
        Mesh m = p.mesh;
        float[] tx = cx[pieceIndex], ty = cy_[pieceIndex], tz = cz[pieceIndex], px = cpx[pieceIndex], py = cpy[pieceIndex];
        boolean clipping = clipZ < 1e8f;
        float boneAlpha = alpha * r.alpha[p.bone], glow = r.glow[p.bone];
        if(boneAlpha <= 0.004f) return;
        {
            if((m.flags[f] & Mesh.buried) != 0) return;
            int a = m.f0[f], b = m.f1[f], c = m.f2[f], d = m.f3[f];
            float e1x = tx[b] - tx[a], e1y = ty[b] - ty[a], e1z = tz[b] - tz[a];
            float e2x = tx[c] - tx[b], e2y = ty[c] - ty[b], e2z = tz[c] - tz[b];
            float nx = e1y * e2z - e1z * e2y, ny = e1z * e2x - e1x * e2z, nz = e1x * e2y - e1y * e2x;
            if(!cam.facing(nx, ny, nz, tx[a], ty[a], tz[a])) return;
            float scan = 0f;
            if(clipping){
                float lo = Math.min(Math.min(tz[a], tz[b]), Math.min(tz[c], tz[d]));
                if(lo > clipZ) return;
                float hi = Math.max(Math.max(tz[a], tz[b]), Math.max(tz[c], tz[d]));
                if(hi > clipZ - clipBand) scan = Mathf.clamp(1f - (clipZ - hi) / clipBand);
            }
            float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
            if(len < 1e-7f) return;
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
            if(sink != null){
                sqx[0] = px[a]; sqx[1] = px[b]; sqx[2] = px[c]; sqx[3] = px[d];
                sqy[0] = py[a]; sqy[1] = py[b]; sqy[2] = py[c]; sqy[3] = py[d];
                sox[0] = tx[a]; sox[1] = tx[b]; sox[2] = tx[c]; sox[3] = tx[d];
                soy[0] = ty[a]; soy[1] = ty[b]; soy[2] = ty[c]; soy[3] = ty[d];
                soz[0] = tz[a]; soz[1] = tz[b]; soz[2] = tz[c]; soz[3] = tz[d];
                sink.face(pieceIndex, sqx, sqy, sox, soy, soz);
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
