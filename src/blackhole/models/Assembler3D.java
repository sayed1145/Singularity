package blackhole.models;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import blackhole.g3d.*;
import mindustry.entities.units.*;
import mindustry.graphics.*;
import mindustry.world.blocks.units.*;

/** Unit factory drawn as a 3D gantry printer ({@link AssemblerModel}); vanilla logic, plans and payload output. */
public class Assembler3D extends UnitFactory{
    public BlockModel model;
    public TextureRegion preview;

    public Assembler3D(String name){
        super(name);
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
        Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
    }

    public class Assembler3DBuild extends UnitFactoryBuild{
        @Override
        public void draw(){
            if(model == null){
                super.draw();
                return;
            }
            model.draw(this);
            Draw.z(Layer.blockOver);
            payRotation = rotdeg();
            drawPayload();
            Draw.reset();
        }

        @Override
        public void drawLight(){
            super.drawLight();
            if(model instanceof KitModel k){
                Drawf.light(x, y, size * 9f + size * 5f * Mathf.clamp(fraction()), k.s.glow, 0.5f * Math.max(efficiency, 0.25f));
            }
        }
    }
}
