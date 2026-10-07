package rbmk.world;

import arc.*;
import arc.func.*;
import arc.util.*;
import arc.graphics.g2d.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.draw.*;
import rbmk.gfx.*;

/** Crafter drawer backed by a 3D {@link BlockModel}: baked base, live 3D parts, baked front layer. */
public class DrawModel extends DrawBlock{
    public final BlockModel model;
    private TextureRegion preview;

    public DrawModel(BlockModel model){
        this.model = model;
    }

    @Override
    public void load(Block block){
        model.load();
        preview = Core.atlas.find(block.name + "-preview", block.region);
    }

    @Override
    public void draw(Building build){
        model.draw(build);
    }

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        block.drawDefaultPlanRegion(plan, list);
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{preview != null ? preview : Core.atlas.find(block.name + "-preview", block.region)};
    }
}
