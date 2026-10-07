package blackhole.models;

import arc.*;
import arc.graphics.g2d.*;
import arc.util.*;
import blackhole.g3d.*;
import mindustry.entities.units.*;
import mindustry.world.blocks.production.*;

/** Drill drawn from a 3D model (live spinning bit); vanilla mining logic. */
public class Drill3D extends Drill{
    public BlockModel model;
    public TextureRegion preview;

    public Drill3D(String name){
        super(name);
        //without this the vanilla DrillBuild draws rotator / rim / top sprites that a 3D drill never bakes,
        //which is exactly the "oh no" error region players saw on the lumen extractor
        buildType = Drill3DBuild::new;
    }

    @Override
    public void getRegionsToOutline(arc.struct.Seq<TextureRegion> out){
        //the baked model already carries its own outline
    }

    @Override
    public void load(){
        super.load();
        model = Models.get(name);
        if(model != null) model.load();
        preview = Core.atlas.find(name + "-preview", region);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find(name + "-preview", region)};
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(preview != null ? preview : region, plan.drawx(), plan.drawy(), size * 8f, size * 8f);
    }

    public class Drill3DBuild extends DrillBuild{
        @Override
        public void draw(){
            if(model == null){
                super.draw();
                return;
            }
            model.draw(this);
            //v7.4: the ore indicator used to be a string atlas lookup for a drill-top region. That region does not exist in the
            //v8 atlas, so Core.atlas.find() returned the error texture and the drill showed "oh no" right in
            //its middle. It is now drawn procedurally - a hex pellet in the ore colour, no sprite lookup at all.
            if(dominantItem != null && drawMineItem){
                drawOre(x, y, size, dominantItem.color);
            }
        }
    }

    /** Procedural ore indicator: a hex pellet in the ore colour with a contact shadow and a highlight rim. */
    public static void drawOre(float x, float y, int size, arc.graphics.Color color){
        float r = 1.2f + size * 0.5f;
        Draw.z(mindustry.graphics.Layer.block + 0.02f);
        Draw.color(0f, 0f, 0f, 0.35f);
        Fill.poly(x, y - 0.35f, 6, r * 1.05f);
        Draw.color(color);
        Fill.poly(x, y, 6, r);
        Draw.color(1f, 1f, 1f, 0.55f);
        Lines.stroke(0.5f);
        Lines.poly(x, y, 6, r);
        Draw.color();
        Draw.reset();
    }
}
