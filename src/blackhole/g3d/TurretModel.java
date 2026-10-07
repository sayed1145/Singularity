package blackhole.g3d;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.defense.turrets.Turret.*;

/**
 * A turret rendered from a real 3D model.
 *
 * <p>The mount/base is static geometry baked like any {@link BlockModel}. The rotating head is <b>live 3D</b>
 * (v7.3): the head mesh is built once from the model code and drawn every frame through {@link Live} at the
 * exact heading, with the same perspective camera, the same Lambert/Blinn light and the same materials as the
 * baked base. Up to v7.2 the head was a sprite sheet of {@link #angles} baked headings plus a 2D residual
 * rotation, so the perspective was only correct every 22.5 degrees and the head visibly sheared in between;
 * now rotation is continuous and nothing about the model is limited by a sprite.
 *
 * <p>Cost: one head is a few dozen culled quads (faces are back-face culled exactly, painter-ordered far to
 * near and sub-pixel faces are dropped at far zoom), i.e. the same order as one animated machine.
 */
public abstract class TurretModel extends BlockModel{
    /** headings audited by the baker / model check (the live head itself is continuous) */
    public int angles = 16;
    /** head sheet pixels per world unit */
    public float headPpu = 5f;
    /** height of the head pivot above the ground (top of the ring) */
    public float headZ = 4f;
    /** head image: half size and centre offset in camera-screen units, measured by the baker */
    public float headHalf = 8f, headOffX, headOffY;
    /** muzzle points in head space (x, y, z) for flashes / heat glows */
    public float[] muzzles = {0f, 8f, 0f};
    public Color heatColor = Color.valueOf("ff9c5a"), glowColor = Color.valueOf("8fe9ff");

    private Mesh headMesh;
    private int[] headOrder;
    private float[] headKeys;
    private int headQuads = -1;
    private float headR = -1f, headT = -1f;
    private final float[] tmp = new float[2];

    protected TurretModel(String name, int size){
        super(name, size);
        liveCost = 0;
    }

    /** The head in head space: pivot at the origin, barrels along +y, z = 0 at the top of the mount ring. */
    public abstract void buildHead(Mesh m);

    /** Adds the head at heading {@code deg} (90 = north) to a block-space mesh (baker / previews). */
    public void addHead(Mesh out, float deg){
        Mesh h = new Mesh();
        buildHead(h);
        out.add(h, 0f, 0f, headZ, 2, deg - 90f);
    }

    /**
     * v7.3: the head is live, so it has a swept volume like any other moving part - a cylinder that contains
     * the head at every heading plus the recoil travel. The baker uses it to pull the static geometry that is
     * nearer to the camera than the head (mount horns, shields, cable masts) into the {@code -front-hd} layer,
     * which is drawn after the head: the mount can now correctly occlude the barrels.
     */
    @Override
    public void buildEnvelope(Mesh m){
        Mesh h = new Mesh();
        buildHead(h);
        float r = 0.5f, z0 = 0f, z1 = 0.5f;
        for(int i = 0; i < h.verts; i++){
            float x = h.vx[i], y = h.vy[i], z = h.vz[i];
            r = Math.max(r, (float)Math.sqrt(x * x + y * y));
            z0 = Math.min(z0, z);
            z1 = Math.max(z1, z);
        }
        m.plain().cyl(16, r + 0.6f, headZ + z0 - 0.4f, headZ + z1 + 0.4f);
    }

    @Override
    public void buildRest(Mesh m){
        addHead(m, 90f);
    }

    @Override
    public void drawLive(Building b){
    }

    private boolean framed;

    /**
     * Computes the common head frame (half size and centre in camera-screen units) over all baked headings. The same
     * code runs in the baker and in game, so the sheet cells and the runtime quad always agree.
     */
    public void frame(){
        if(framed) return;
        framed = true;
        float x0 = 1e9f, x1 = -1e9f, y0 = 1e9f, y1 = -1e9f;
        for(int i = 0; i < angles; i++){
            Mesh m = new Mesh();
            addHead(m, 90f + i * 360f / angles);
            for(int v = 0; v < m.verts; v++){
                float sx = cam.sx(m.vx[v], m.vz[v]), sy = cam.sy(m.vy[v], m.vz[v]);
                x0 = Math.min(x0, sx); x1 = Math.max(x1, sx); y0 = Math.min(y0, sy); y1 = Math.max(y1, sy);
            }
        }
        headOffX = (x0 + x1) / 2f;
        headOffY = (y0 + y1) / 2f;
        headHalf = Math.max(x1 - x0, y1 - y0) / 2f + 0.6f;
    }

    /** Builds (once) and returns the live head mesh: pivot at the origin, barrels along +y. */
    public Mesh headMesh(){
        if(headMesh == null){
            Mesh m = new Mesh();
            buildHead(m);
            headMesh = m;
            headOrder = new int[Math.max(1, m.faces)];
            headKeys = new float[Math.max(1, m.faces)];
            for(int i = 0; i < headOrder.length; i++) headOrder[i] = i;
            headQuads = Math.max(8, m.faces * 2 / 3);
        }
        return headMesh;
    }

    /** Pre-builds the head so the first shot / first placement never pays for it. */
    public void loadHeads(){
        frame();
        headMesh();
    }

    /** Largest xy radius of the head mesh (world units). */
    public float headRadius(){
        headMesh();
        if(headR < 0f){
            headR = 0.5f;
            for(int i = 0; i < headMesh.verts; i++){
                float x = headMesh.vx[i], y = headMesh.vy[i];
                headR = Math.max(headR, (float)Math.sqrt(x * x + y * y));
            }
        }
        return headR;
    }

    /** Height of the head above its pivot. */
    public float headTop(){
        headMesh();
        if(headT < 0f){
            headT = 0.5f;
            for(int i = 0; i < headMesh.verts; i++) headT = Math.max(headT, headMesh.vz[i]);
        }
        return headT;
    }

    /** Live quads one head draw costs (measured from the mesh). */
    public int headQuads(){
        headMesh();
        return headQuads;
    }

    /** Screen offset of a head-space point for heading {@code rot} (world units, relative to the block centre). */
    public void project(float rot, float hx, float hy, float hz, float[] out){
        float c = Mathf.cosDeg(rot - 90f), s = Mathf.sinDeg(rot - 90f);
        float x = hx * c - hy * s, y = hx * s + hy * c, z = hz + headZ;
        out[0] = cam.sx(x, z);
        out[1] = cam.sy(y, z);
    }

    /**
     * Draws the head live in 3D. rotation in degrees (Mindustry convention, 0 = east), recoil in world units.
     * Any heading is exact - there is no sprite and no residual 2D rotation.
     */
    public void drawHead(float x, float y, float rotation, float recoil){
        Mesh h = headMesh();
        Budget.spend(headQuads);
        //contact shadow: the head is live geometry, so the baked base cannot contain its shadow. One soft disc
        //cast with the same light direction as the baker grounds the head instead of letting it float.
        float hr = headRadius();
        float sh = (headZ + headTop() * 0.5f);
        Draw.color(0f, 0f, 0f, 0.22f);
        Fill.circle(x - Light.lx / Light.lz * sh, y - Light.ly / Light.lz * sh, hr * 0.92f);
        Draw.color();
        Live.resetTint();
        Live.lod();
        //recoil slides the whole head back along the aim, in block space
        float rx = -Mathf.cosDeg(rotation) * recoil, ry = -Mathf.sinDeg(rotation) * recoil;
        Live.pose(rx, ry, headZ, 2, rotation - 90f);
        Live.draw(h, cam, x, y, headOrder, headKeys);
        Live.minArea = 0f;
    }

    /** Standard turret draw: baked base, baked head, heat and charge glows. */
    public void drawTurret(TurretBuild b){
        Draw.color();
        Draw.mixcol();
        drawBase(b.x, b.y);
        Draw.z(Layer.turret);
        drawHead(b.x, b.y, b.rotation, b.curRecoil * recoilScale());
        float heat = b.heat;
        if(heat > 0.01f || glowAlpha(b) > 0.01f){
            Draw.z(Layer.turretHeat);
            Draw.blend(Blending.additive);
            for(int i = 0; i + 2 < muzzles.length; i += 3){
                project(b.rotation, muzzles[i], muzzles[i + 1] - b.curRecoil * recoilScale(), muzzles[i + 2], tmp);
                if(heat > 0.01f){
                    Draw.color(heatColor, heat * 0.85f);
                    Fill.circle(b.x + tmp[0], b.y + tmp[1], glowSize() * (0.6f + heat * 0.5f));
                }
                float g = glowAlpha(b);
                if(g > 0.01f){
                    Draw.color(glowColor, g * (0.55f + Mathf.absin(Time.time + b.id * 13f, 6f, 0.25f)));
                    Fill.circle(b.x + tmp[0], b.y + tmp[1], glowSize() * 0.7f);
                }
            }
            Draw.blend();
            Draw.color();
        }
        //static geometry that stands in front of the head (baked front layer), then model extras
        Draw.z(Layer.turret + 0.02f);
        Draw.color();
        drawFront(b.x, b.y);
        drawTurretOver(b);
        Draw.reset();
    }

    /** Charge / idle emissive strength (0..1). */
    public float glowAlpha(TurretBuild b){
        return 0f;
    }

    public float glowSize(){
        return 1.6f + size * 0.35f;
    }

    public float recoilScale(){
        return 1f;
    }

    /** Model specific extras (rings, arcs, charge effects), drawn after the head. */
    public void drawTurretOver(TurretBuild b){
    }
}
