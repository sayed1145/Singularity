package blackhole.models;

import arc.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import blackhole.g3d.*;
import mindustry.*;
import mindustry.world.*;

/**
 * Client warm-up and construction-animation cost control (v7.3).
 *
 * <p>Two things used to happen on the main thread the first time a block appeared and while it was being
 * built, and both are removed here:
 *
 * <ul>
 * <li><b>First-draw mesh building.</b> Every model built its live mesh (and, since v7.3, its live turret head)
 *     lazily inside the first {@code draw()} - a visible hitch exactly when a freshly built turret popped in.
 *     {@link #warm()} builds all of them once at client load instead.</li>
 * <li><b>The construction animation.</b> Vanilla {@code ConstructBlock.ConstructBuild.draw()} loops over
 *     {@code block.getGeneratedIcons()} and calls {@code Draw.flush()} <i>per region</i>, with
 *     {@code Shaders.blockbuild} bound: every extra icon is another full batch flush plus a shader rebind, for
 *     every block under construction, every frame. Mod blocks that fall back to vanilla icon sets contribute
 *     2-3 regions each, and the regions are the full-resolution 3D previews (up to 400 px for a 5x5), sampled
 *     minified without mipmaps. {@link #slimConstructIcons()} pins every mod block to exactly one small baked
 *     {@code -icon} region, so a constructing block costs one flush and a texture a few dozen pixels wide.</li>
 * </ul>
 */
public final class Warmup{
    public static int warmed, slimmed;
    public static long warmQuads;

    private Warmup(){}

    /** Builds every live mesh / turret head up front (client only, ~10 ms total). */
    public static void warm(){
        warmed = 0;
        warmQuads = 0;
        for(BlockModel m : Models.all()){
            try{
                m.load();
                if(m.liveCost < 0) m.liveCost = m.measureLiveCost();
                if(m instanceof TurretModel t){
                    t.frame();
                    t.headMesh();
                    warmQuads += t.headQuads();
                }
                warmQuads += Math.max(0, m.liveCost);
                warmed++;
            }catch(Throwable t){
                Log.err("[blackhole] warm-up failed for " + m.name, t);
            }
        }
        Log.info("[blackhole] warmed @ models up front (@ live quads at full detail), no first-draw hitch", warmed, warmQuads);
    }

    /**
     * One small region per mod block for the build animation. {@code generatedIcons} is protected and is read
     * (and cached) by {@code getGeneratedIcons()}; it is written here after icon generation has finished, so
     * the UI icons that were produced at pack time are untouched.
     */
    public static void slimConstructIcons(){
        slimmed = 0;
        if(Core.atlas == null) return;
        java.lang.reflect.Field field;
        try{
            field = Block.class.getDeclaredField("generatedIcons");
            field.setAccessible(true);
        }catch(Throwable t){
            Log.warn("[blackhole] generatedIcons not accessible, construction icons left as they are: " + t);
            return;
        }
        for(Block b : Vars.content.blocks()){
            if(b.name == null || !b.name.startsWith(BlockModel.prefix)) continue;
            TextureRegion icon = Core.atlas.find(b.name + "-icon");
            if(!Core.atlas.isFound(icon)){
                TextureRegion[] cur = b.getGeneratedIcons();
                if(cur == null || cur.length <= 1) continue;
                icon = cur[0];
            }
            try{
                field.set(b, new TextureRegion[]{icon});
                slimmed++;
            }catch(Throwable ignored){
            }
        }
        Log.info("[blackhole] construction animation: @ mod blocks pinned to a single small icon (1 flush each)", slimmed);
    }

    /** Both steps, for ClientLoadEvent. */
    public static void install(){
        warm();
        slimConstructIcons();
    }

    /** Names of the blocks that still have more than one construction icon (test / audit helper). */
    public static Seq<String> fatIcons(){
        Seq<String> out = new Seq<>();
        for(Block b : Vars.content.blocks()){
            if(b.name != null && b.name.startsWith(BlockModel.prefix)){
                TextureRegion[] ic = b.getGeneratedIcons();
                if(ic != null && ic.length > 1) out.add(b.name + "x" + ic.length);
            }
        }
        return out;
    }
}
