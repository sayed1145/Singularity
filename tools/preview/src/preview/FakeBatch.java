package preview;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;

/**
 * Headless recording batch: Arc's Fill / Lines / Draw.rect calls end up here as raw vertex data, so renderers can
 * be exercised, counted and compared without a GL context.
 */
public class FakeBatch extends Batch{
    public long quads, regionDraws, flushes;
    public float[] verts = new float[1 << 16];
    public int n;
    public boolean record = true;
    /** per recorded primitive: kind (0 = quad of 24 floats, 1 = region draw of 8 floats) and blend (0 normal, 1 additive) */
    public arc.struct.IntSeq kinds = new arc.struct.IntSeq(), blends = new arc.struct.IntSeq();
    private int blendId;

    public static FakeBatch install(){
        if(Core.gl == null){
            arc.mock.MockGL20 gl = new arc.mock.MockGL20();
            Core.gl = gl;
            Core.gl20 = gl;
        }
        if(Core.graphics == null) Core.graphics = new arc.mock.MockGraphics();
        FakeBatch b = new FakeBatch();
        Core.batch = b;
        final Texture tex = new Texture(new Pixmap(4, 4));
        Core.atlas = new TextureAtlas(){
            final AtlasRegion w = new AtlasRegion(tex, 1, 1, 1, 1);
            @Override public AtlasRegion white(){ return w; }
            @Override public AtlasRegion find(String name){ return w; }
            @Override public AtlasRegion find(String name, TextureRegion def){ return w; }
            @Override public boolean isFound(TextureRegion region){ return region != null && region != w; }
        };
        return b;
    }

    public void reset(){
        quads = regionDraws = flushes = 0;
        n = 0;
        kinds.clear();
        blends.clear();
    }

    @Override
    protected void setBlending(Blending blending){
        this.blending = blending;
        blendId = blending == Blending.additive ? 1 : 0;
    }

    private void push(float[] v, int off, int count){
        if(!record) return;
        if(n + count > verts.length){
            float[] nv = new float[Math.max(verts.length * 2, n + count)];
            System.arraycopy(verts, 0, nv, 0, n);
            verts = nv;
        }
        System.arraycopy(v, off, verts, n, count);
        n += count;
    }

    @Override
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
        quads += count / 24;
        if(!record) return;
        for(int q = 0; q < count / 24; q++){
            kinds.add(0);
            blends.add(blendId);
        }
        push(spriteVertices, offset, count);
    }

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
        regionDraws++;
        if(!record) return;
        kinds.add(1);
        blends.add(blendId);
        float[] v = {x, y, originX, originY, width, height, rotation, colorPacked};
        push(v, 0, v.length);
    }

    @Override
    protected void flush(){
        flushes++;
    }
}
