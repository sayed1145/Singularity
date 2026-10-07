package preview;

import arc.graphics.Color;
import arc.util.Time;
import blackhole.models.UtilityModels;
import blackhole.models.UtilityModels.LiveModel;
import blackhole.AureliaUtilities;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.entities.units.BuildPlan;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Runs the actual new block/plan drawers; no raster sprites are loaded. */
public class UtilityCheck{
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Path out=Path.of(args.length>0?args[0]:"docs/v84-models");Files.createDirectories(out);
        FakeBatch batch=FakeBatch.install();arc.Core.app=new arc.mock.MockApplication();arc.Core.settings=new arc.mock.MockSettings();Vars.content=new ContentLoader();
        Time.setDeltaProvider(()->1f);Time.delta=1f;
        var inc=new AureliaUtilities.LiveIncinerator("test-incinerator");
        var sor=new AureliaUtilities.LiveSorter("test-inverse");
        //Exercise client asset lifecycle with non-atlas font glyphs, as the real trash/filter icons are.
        var glyph=new arc.scene.style.TextureRegionDrawable(new arc.graphics.g2d.TextureRegion(arc.Core.atlas.white()));
        mindustry.gen.Icon.trash=glyph;mindustry.gen.Icon.filter=glyph;
        inc.loadIcon();inc.load();inc.createIcons(null);sor.loadIcon();sor.load();sor.createIcons(null);
        check(inc.teamRegion!=null&&sor.teamRegion!=null,"base region metadata missing");
        check(inc.uiIcon==glyph.getRegion()&&sor.uiIcon==glyph.getRegion(),"UI glyph replaced with missing sprite");
        System.out.println("PASS client icon lifecycle: loadIcon/load/createIcons preserve engine glyphs without Pixmap packing; base team metadata initialized");
        var ib=(AureliaUtilities.LiveIncinerator.LiveIncineratorBuild)inc.buildType.get();
        var sb=(AureliaUtilities.LiveSorter.LiveSorterBuild)sor.buildType.get();
        ib.heat=1f;
        for(LiveModel model:new LiveModel[]{UtilityModels.furnace,UtilityModels.sorter}){
            String name=model.name;Path frames=out.resolve(name);Files.createDirectories(frames);
            Set<Integer> hashes=new HashSet<>();long min=Long.MAX_VALUE,max=0;
            for(int i=0;i<48;i++){
                float angle=i*360f/48f;ib.phase=sb.phase=angle;
                batch.reset();if(model.sorting)sb.draw();else ib.draw();
                check(batch.regionDraws==0,name+" used a sprite");check(batch.quads>10,name+" missing geometry");
                for(int v=0;v<batch.n;v+=6){
                    check(Float.isFinite(batch.verts[v])&&Float.isFinite(batch.verts[v+1]),"non-finite vertex");
                    check(Math.abs(batch.verts[v])<=model.size*4f+.02f&&Math.abs(batch.verts[v+1])<=model.size*4f+.02f,name+" projected geometry overlaps adjacent tile");
                }
                hashes.add(Arrays.hashCode(Arrays.copyOf(batch.verts,batch.n)));
                min=Math.min(min,batch.quads);max=Math.max(max,batch.quads);
                //Every physical vertex remains inside its tile in the horizontal plane.
                for(int v=0;v<model.frame.verts;v++){
                    check(Math.abs(model.frame.vx[v])<=model.size*4f+.001f,name+" x protrusion");
                    check(Math.abs(model.frame.vy[v])<=model.size*4f+.001f,name+" y protrusion");
                    check(model.frame.vz[v]>=-.001f,name+" below floor");
                }
                ImageIO.write(Anim.composite(batch,model.size*4f,1.6f,20f,null,null,null,0),"png",frames.resolve(String.format("%03d.png",i)).toFile());
            }
            check(hashes.size()>6,name+" animation is static");
            check(model.rotorRadius()<(model.sorting?1.16f:2.14f),name+" rotor intersects shell");
            batch.reset();Time.time=43f;BuildPlan p=new BuildPlan(0,0,0,model.sorting?sor:inc);
            if(model.sorting)sor.drawPlanRegion(p,null);else inc.drawPlanRegion(p,null);
            check(batch.regionDraws==0&&batch.quads>10,"build plan not live geometry");
            // Exercise the far-zoom branch and an exhausted generic sprite budget explicitly.
            blackhole.g3d.Budget.maxQuads=0;blackhole.g3d.Budget.minPpu=999;
            arc.Core.graphics=new arc.mock.MockGraphics(){@Override public int getWidth(){return 100;}};
            arc.Core.camera=new arc.graphics.Camera();arc.Core.camera.resize(200,200);
            batch.reset();model.drawAt(0,0,23,1,Color.white);
            check(batch.regionDraws==0&&batch.quads>0,"forbidden sprite fallback");
            blackhole.g3d.Budget.maxQuads=14000;blackhole.g3d.Budget.minPpu=1.35f;arc.Core.camera=null;
            batch.record=false;for(int i=0;i<1000;i++)model.drawAt(0,0,i,1,Color.white);
            long t=System.nanoTime();for(int i=0;i<4000;i++)model.drawAt(0,0,i,1,Color.white);
            double us=(System.nanoTime()-t)/4e6;batch.record=true;
            System.out.printf(Locale.ROOT,"PASS %s: %d mesh faces; %d..%d visible quads; %d unique frames; 0 region draws; rotor clearance %.3f; CPU %.1f us/draw (mock GL, not FPS)%n",name,model.frame.faces+model.footing.faces,min,max,hashes.size(),(model.sorting?1.16f:2.14f)-model.rotorRadius(),us);
        }
    }
}
