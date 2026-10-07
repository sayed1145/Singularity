package blackhole.g3d;

import arc.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.Turret.*;
import mindustry.world.draw.*;

/** Turret drawer backed by a {@link TurretModel}. Keeps DrawTurret's API (parts are ignored). */
public class DrawTurret3D extends DrawTurret{
    public final TurretModel model;
    private TextureRegion preview;

    public DrawTurret3D(TurretModel model){
        this.model = model;
    }

    @Override
    public void load(Block block){
        model.load();
        model.loadHeads();
        preview = Core.atlas.find(block.name + "-preview", block.region);
        base = preview;
        //never the error region: if any vanilla path ever draws it, the player sees "oh no" on the turret
        heat = Core.atlas.find("clear");
        outline = block.region;
    }

    @Override
    public void draw(Building build){
        if(build instanceof TurretBuild t) model.drawTurret(t);
    }

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(preview != null ? preview : block.region, plan.drawx(), plan.drawy(), block.size * 8f, block.size * 8f);
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{preview != null ? preview : Core.atlas.find(block.name + "-preview", block.region)};
    }

    @Override
    public void getRegionsToOutline(Block block, arc.struct.Seq<TextureRegion> out){
    }
}
