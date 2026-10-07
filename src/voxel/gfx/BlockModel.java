package voxel.gfx;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.gen.*;

/**
 * A building rendered as a real 3D model with a fixed camera.
 *
 * <p>Static geometry ({@link #buildStatic}) is ray-rasterised offline by tools/bake with a z-buffer, SSAA,
 * height-based ambient occlusion and procedural materials into {@code <name>-hd}. Moving parts are drawn live
 * with {@link Live}. Static geometry that is nearer to the camera than any moving part (worked out exactly from
 * {@link #buildEnvelope}) is additionally baked into {@code <name>-front-hd} and drawn after the live parts,
 * which gives correct 3D occlusion between baked and live geometry without any depth buffer at runtime.
 */
public abstract class BlockModel{
    public static final String prefix = "blackhole-";
    public final String name;
    public final int size;
    public final float half;
    public final Cam cam;
    /** centre of the procedural water ripple material (baker) */
    public float waterCx, waterCy;
    private TextureRegion base, front;
    /** atlas the cached regions were resolved from; Mindustry replaces Core.atlas after mod sprite packing */
    private TextureAtlas loadedFrom;

    protected BlockModel(String name, int size){
        this.name = name;
        this.size = size;
        this.half = size * 4f;
        this.cam = new Cam(half);
    }

    /** Geometry baked into textures. Only called by the offline baker. */
    public abstract void buildStatic(Mesh m);

    /** Conservative swept volume of every live part (used by the baker to extract the front layer). */
    public abstract void buildEnvelope(Mesh m);

    /** Live parts in a representative pose (used by the baker for icons/previews). */
    public abstract void buildRest(Mesh m);

    /**
     * Optional model specific procedural material. rgb holds the lit base colour and may be modified.
     * @return true if handled
     */
    public boolean pattern(int mat, float x, float y, float z, float nz, float[] rgb){
        return false;
    }

    /**
     * Resolves the baked layers. Mindustry calls Block.load() once while it is still packing mod sprites (Core.atlas
     * is then a temporary atlas whose textures are disposed right afterwards) and again after the final atlas is
     * built, so regions are re-resolved whenever Core.atlas changes or a cached texture has been disposed.
     * Caching the first result made every placed block sample a deleted texture, i.e. draw solid black.
     */
    public void load(){
        TextureAtlas atlas = Core.atlas;
        if(atlas == null) return;
        if(atlas == loadedFrom && valid(base) && (front == null || valid(front))) return;
        loadedFrom = atlas;
        base = atlas.find(prefix + name + "-hd", atlas.find(prefix + name));
        TextureRegion f = atlas.find(prefix + name + "-front-hd");
        front = atlas.isFound(f) ? f : null;
    }

    private static boolean valid(TextureRegion r){
        return r != null && r.texture != null && !r.texture.isDisposed();
    }

    public void drawBase(float x, float y){
        if(Core.atlas != loadedFrom) load();
        if(base != null) Draw.rect(base, x, y, half * 2f, half * 2f);
    }

    public void drawFront(float x, float y){
        if(front != null) Draw.rect(front, x, y, half * 2f, half * 2f);
    }

    /** Full draw: baked base, live 3D parts, baked front layer. */
    public void draw(Building b){
        Draw.color();
        Draw.mixcol();
        drawBase(b.x, b.y);
        Live.resetTint();
        drawLive(b);
        Live.resetTint();
        Draw.color();
        drawFront(b.x, b.y);
        drawOver(b);
        Live.resetTint();
    }

    /** Live parts drawn before the front layer (they can be hidden by static geometry). */
    public abstract void drawLive(Building b);

    /** Live parts that are always nearer than any static geometry (drawn after the front layer). */
    public void drawOver(Building b){
    }

    /** Current on-screen pixels per world unit (1 when unknown, e.g. in tests). */
    public static float pixelsPerUnit(){
        if(Core.graphics == null || Core.camera == null || Core.camera.width <= 0f) return 4f;
        return Core.graphics.getWidth() / Core.camera.width;
    }
}
