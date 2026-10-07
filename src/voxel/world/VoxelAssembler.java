package voxel.world;

import arc.*;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.graphics.*;
import mindustry.world.blocks.units.*;
import voxel.gfx.*;

/**
 * Unit factory rendered as a 3D gantry printer ({@link AssemblerModel}). Vanilla {@link UnitFactory} logic,
 * plans, payload output and networking are untouched; only drawing is replaced. The unit being built is the
 * real 3D rig of that unit type, printed layer by layer.
 */
public class VoxelAssembler extends UnitFactory{
    public final AssemblerModel model = AssemblerModel.instance;
    public TextureRegion preview;

    public VoxelAssembler(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();
        model.load();
        preview = Core.atlas.find(name + "-preview", region);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find(name + "-preview", region)};
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        TextureRegion p = preview != null ? preview : region;
        Draw.rect(p, plan.drawx(), plan.drawy(), size * 8f, size * 8f);
        Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
    }

    public class VoxelAssemblerBuild extends UnitFactoryBuild{
        @Override
        public void draw(){
            model.draw(this);
            Draw.z(Layer.blockOver);
            payRotation = rotdeg();
            drawPayload();
            Draw.reset();
        }

        @Override
        public void drawLight(){
            super.drawLight();
            Drawf.light(x, y, 26f + 14f * Mathf.clamp(fraction()), AssemblerModel.cyan, 0.5f * Math.max(efficiency, 0.25f));
        }
    }
}
