package blackhole.g3d;

import arc.math.*;

/**
 * An articulated 3D model: a bone hierarchy plus convex mesh pieces attached to bones.
 *
 * <p>Model space is +x right, +y forward (the unit's facing), +z up, origin on the ground under the unit's
 * centre, 1 unit = 1 world unit (1/8 tile). Every bone has a local 3x4 matrix (rotation/scale + translation in
 * its parent's frame) that animation code writes each frame; {@link UnitRenderer} concatenates them.
 *
 * <p>Pieces are kept convex (one primitive each), so back-face culling alone resolves their own surface and the
 * renderer only has to order a few dozen pieces per frame instead of sorting every face. Small greebles are
 * marked {@link #detail()} and are skipped when the unit is small on screen.
 */
public class Rig{
    public static final int maxBones = 128;

    public int bones;
    public final int[] parent = new int[maxBones];
    public final float[] restX = new float[maxBones], restY = new float[maxBones], restZ = new float[maxBones];
    /** local pose matrices, row major 3x4: m00 m01 m02 tx | m10 m11 m12 ty | m20 m21 m22 tz */
    public final float[] local = new float[maxBones * 12];
    /** per bone emissive multiplier and alpha, reset every frame */
    public final float[] glow = new float[maxBones], alpha = new float[maxBones];
    public final boolean[] hidden = new boolean[maxBones];

    public Piece[] pieces = new Piece[64];
    public int pieceCount, shadowGroups, maxVerts;
    /** half extent used for the perspective camera (same ratios as the block camera) */
    public float half = 12f;
    /** approximate model height, used for printing/clipping */
    public float height = 10f;
    /**
     * Enables the vertical-stacking correction in {@link UnitRenderer}: a piece resting on top of another one
     * (turrets on a hull) is always drawn after it, whatever the centroid distances say. Needed for large
     * models whose hull centroid can be nearer to the camera than parts standing on it after a turn.
     */
    public boolean stackSort;

    public static final class Piece{
        public final Mesh mesh = new Mesh();
        public int bone;
        public boolean detail;
        /** index of the piece this one is mounted on (drawn right after it), or -1 */
        public int attach = -1;
        /** order relative to the attach piece: negative = drawn after it (decal), positive = drawn before it */
        public int attachOrder;
        public int shadowGroup = -1;
        /** shadow outline is the ellipse inscribed in the bounds (lathes, ducts) instead of the box */
        public boolean shadowRound;
        public float cx, cy, cz;
        public float minX, minY, minZ, maxX, maxY, maxZ;
    }

    public Rig(){
        for(int i = 0; i < maxBones; i++){ glow[i] = 1f; alpha[i] = 1f; }
    }

    /** Declares a bone whose origin sits at (x,y,z) in its parent's frame. Returns its index. */
    public int bone(int parentIndex, float x, float y, float z){
        if(parentIndex >= bones) throw new IllegalArgumentException("parent must be declared first");
        int b = bones++;
        parent[b] = parentIndex;
        restX[b] = x; restY[b] = y; restZ[b] = z;
        return b;
    }

    /** Starts a new convex piece on bone b and returns its mesh (coordinates in the bone's frame). */
    public Mesh part(int b){
        if(pieceCount == pieces.length) pieces = java.util.Arrays.copyOf(pieces, pieceCount * 2);
        Piece p = new Piece();
        p.bone = b;
        pieces[pieceCount++] = p;
        return p.mesh;
    }

    public Piece last(){
        return pieces[pieceCount - 1];
    }

    /** Last piece is a greeble: skipped at low zoom. */
    public Rig detail(){
        last().detail = true;
        return this;
    }

    /** Last piece is mounted on the surface of an earlier piece (drawn right after it). */
    public Rig on(int pieceIndex){
        last().attach = pieceIndex;
        last().attachOrder = -(pieceCount - 1 - pieceIndex);
        return this;
    }

    /** Last piece sits inside/behind an earlier piece: drawn just before it (level 1 = immediately before). */
    public Rig under(int pieceIndex, int level){
        last().attach = pieceIndex;
        last().attachOrder = level;
        return this;
    }

    /** Last piece casts a shadow in group g (one convex hull per group). */
    public Rig shadow(int g){
        last().shadowGroup = g;
        shadowGroups = Math.max(shadowGroups, g + 1);
        return this;
    }

    /** Like {@link #shadow(int)}, with a round outline (for lathed / cylindrical pieces). */
    public Rig shadowRound(int g){
        shadow(g);
        last().shadowRound = true;
        return this;
    }

    public int lastIndex(){
        return pieceCount - 1;
    }

    /** Computes piece centroids / bounds. Call once after building. */
    public Rig finish(){
        for(int i = 0; i < pieceCount; i++){
            Piece p = pieces[i];
            Mesh m = p.mesh;
            float sx = 0, sy = 0, sz = 0;
            p.minX = p.minY = p.minZ = Float.MAX_VALUE;
            p.maxX = p.maxY = p.maxZ = -Float.MAX_VALUE;
            for(int v = 0; v < m.verts; v++){
                sx += m.vx[v]; sy += m.vy[v]; sz += m.vz[v];
                p.minX = Math.min(p.minX, m.vx[v]); p.maxX = Math.max(p.maxX, m.vx[v]);
                p.minY = Math.min(p.minY, m.vy[v]); p.maxY = Math.max(p.maxY, m.vy[v]);
                p.minZ = Math.min(p.minZ, m.vz[v]); p.maxZ = Math.max(p.maxZ, m.vz[v]);
            }
            int n = Math.max(m.verts, 1);
            p.cx = sx / n; p.cy = sy / n; p.cz = sz / n;
            maxVerts = Math.max(maxVerts, m.verts);
        }
        reset();
        return this;
    }

    public int faces(){
        int f = 0;
        for(int i = 0; i < pieceCount; i++) f += pieces[i].mesh.faces;
        return f;
    }

    // ------------------------------------------------------------------ posing

    /** Rest pose: identity rotations, rest offsets, full glow/alpha, nothing hidden. */
    public void reset(){
        for(int b = 0; b < bones; b++){
            int o = b * 12;
            local[o] = 1; local[o + 1] = 0; local[o + 2] = 0; local[o + 3] = restX[b];
            local[o + 4] = 0; local[o + 5] = 1; local[o + 6] = 0; local[o + 7] = restY[b];
            local[o + 8] = 0; local[o + 9] = 0; local[o + 10] = 1; local[o + 11] = restZ[b];
            glow[b] = 1f; alpha[b] = 1f; hidden[b] = false;
        }
    }

    /** Applies bone b's local matrix to a point (gives the point in the parent's frame). */
    public void apply(int b, float x, float y, float z, float[] out){
        int o = b * 12;
        out[0] = local[o] * x + local[o + 1] * y + local[o + 2] * z + local[o + 3];
        out[1] = local[o + 4] * x + local[o + 5] * y + local[o + 6] * z + local[o + 7];
        out[2] = local[o + 8] * x + local[o + 9] * y + local[o + 10] * z + local[o + 11];
    }

    /** Sets translation (in the parent frame) and identity rotation. */
    public void place(int b, float x, float y, float z){
        basis(b, 1, 0, 0, 0, 1, 0, 0, 0, 1, x, y, z);
    }

    /** Post-multiplies a rotation about the bone's own axis (0=x pitch, 1=y roll, 2=z yaw). */
    public void rot(int b, int axis, float deg){
        if(deg == 0f) return;
        float c = Mathf.cosDeg(deg), s = Mathf.sinDeg(deg);
        int o = b * 12;
        for(int r = 0; r < 3; r++){
            int i = o + r * 4;
            float m0 = local[i], m1 = local[i + 1], m2 = local[i + 2];
            if(axis == 0){ local[i + 1] = m1 * c + m2 * s; local[i + 2] = -m1 * s + m2 * c; }
            else if(axis == 1){ local[i] = m0 * c - m2 * s; local[i + 2] = m0 * s + m2 * c; }
            else{ local[i] = m0 * c + m1 * s; local[i + 1] = -m0 * s + m1 * c; }
        }
    }

    /** Yaw (z), then pitch (x), then roll (y), all in degrees, about the bone origin. */
    public void euler(int b, float yaw, float pitch, float roll){
        rot(b, 2, yaw);
        rot(b, 0, pitch);
        rot(b, 1, roll);
    }

    /** Translation in the parent's frame (added to the rest offset). */
    public void move(int b, float dx, float dy, float dz){
        int o = b * 12;
        local[o + 3] += dx; local[o + 7] += dy; local[o + 11] += dz;
    }

    /** Translation along the bone's own (already rotated) axes, e.g. barrel recoil. */
    public void moveLocal(int b, float dx, float dy, float dz){
        int o = b * 12;
        local[o + 3] += local[o] * dx + local[o + 1] * dy + local[o + 2] * dz;
        local[o + 7] += local[o + 4] * dx + local[o + 5] * dy + local[o + 6] * dz;
        local[o + 11] += local[o + 8] * dx + local[o + 9] * dy + local[o + 10] * dz;
    }

    public void scale(int b, float sx, float sy, float sz){
        int o = b * 12;
        for(int r = 0; r < 3; r++){
            local[o + r * 4] *= sx; local[o + r * 4 + 1] *= sy; local[o + r * 4 + 2] *= sz;
        }
    }

    /** Sets the local matrix from basis columns (x, y, z axes expressed in the parent frame) and an origin. */
    public void basis(int b, float xx, float xy, float xz, float yx, float yy, float yz, float zx, float zy, float zz,
                      float tx, float ty, float tz){
        int o = b * 12;
        local[o] = xx; local[o + 1] = yx; local[o + 2] = zx; local[o + 3] = tx;
        local[o + 4] = xy; local[o + 5] = yy; local[o + 6] = zy; local[o + 7] = ty;
        local[o + 8] = xz; local[o + 9] = yz; local[o + 10] = zz; local[o + 11] = tz;
    }

    /**
     * Points bone b so that its local -z axis runs from (x0,y0,z0) to (x1,y1,z1) (both in the parent frame),
     * with its local x axis as close as possible to the hint (hx,hy,hz). Segment meshes are modelled hanging
     * down from their origin along -z, so this is how legs follow an IK solution. Returns the segment length.
     */
    public float aim(int b, float x0, float y0, float z0, float x1, float y1, float z1, float hx, float hy, float hz){
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float l = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if(l < 1e-5f){ dx = 0; dy = 0; dz = -1; l = 1e-5f; }
        //z axis = -direction
        float zx = -dx / l, zy = -dy / l, zz = -dz / l;
        //x axis = hint made perpendicular to z
        float d = hx * zx + hy * zy + hz * zz;
        float xx = hx - d * zx, xy = hy - d * zy, xz = hz - d * zz;
        float xl = (float)Math.sqrt(xx * xx + xy * xy + xz * xz);
        if(xl < 1e-5f){
            //hint parallel to the segment: pick any perpendicular
            xx = zz; xy = 0; xz = -zx;
            xl = (float)Math.sqrt(xx * xx + xz * xz);
            if(xl < 1e-5f){ xx = 1; xz = 0; xl = 1; }
        }
        xx /= xl; xy /= xl; xz /= xl;
        //y = z cross x
        float yx = zy * xz - zz * xy, yy = zz * xx - zx * xz, yz = zx * xy - zy * xx;
        basis(b, xx, xy, xz, yx, yy, yz, zx, zy, zz, x0, y0, z0);
        return l;
    }

    /**
     * Planar two-bone IK. Given hip H and target T (same frame), bone lengths a and b and a bend direction
     * (unit vector that the knee should bulge towards), writes the knee position into out[0..2].
     * Unreachable targets are clamped (leg fully stretched towards the target).
     */
    public static void knee(float hx, float hy, float hz, float tx, float ty, float tz, float a, float b,
                            float bx, float by, float bz, float[] out){
        float dx = tx - hx, dy = ty - hy, dz = tz - hz;
        float d = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if(d < 1e-4f){ out[0] = hx + bx * a; out[1] = hy + by * a; out[2] = hz + bz * a; return; }
        float ux = dx / d, uy = dy / d, uz = dz / d;
        float dc = Mathf.clamp(d, Math.abs(a - b) + 1e-3f, (a + b) * 0.9995f);
        //distance along the hip->target line to the knee's foot point, and the knee's offset from the line
        float along = (a * a - b * b + dc * dc) / (2f * dc);
        float off = (float)Math.sqrt(Math.max(a * a - along * along, 0f));
        //bend direction made perpendicular to the line
        float k = bx * ux + by * uy + bz * uz;
        float px = bx - k * ux, py = by - k * uy, pz = bz - k * uz;
        float pl = (float)Math.sqrt(px * px + py * py + pz * pz);
        if(pl < 1e-5f){ px = 0; py = 0; pz = 1; pl = 1; }
        px /= pl; py /= pl; pz /= pl;
        out[0] = hx + ux * along + px * off;
        out[1] = hy + uy * along + py * off;
        out[2] = hz + uz * along + pz * off;
    }
}
