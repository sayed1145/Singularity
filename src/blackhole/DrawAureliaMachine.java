package blackhole;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.draw.*;

/** Low-luminance animated drawer for Aurelia factories, relays and generators. */
public class DrawAureliaMachine extends DrawBlock{
    private TextureRegion base, glow;
    private final Color tint;
    private final float speed;

    public DrawAureliaMachine(Color tint, float speed){
        this.tint = tint;
        this.speed = speed;
    }

    @Override
    public void load(Block block){
        base = block.region;
        glow = Core.atlas.find(block.name + "-glow");
    }

    @Override
    public void draw(Building build){
        Draw.rect(base, build.x, build.y, build.drawrot());
        if(glow == null || !glow.found()) return;
        float activity = build.efficiency <= 0f ? 0.22f : 0.55f + 0.45f * Mathf.absin(Time.time + build.id, 25f, 1f);
        float z = Draw.z();
        Draw.z(z + 0.01f);
        Draw.blend(Blending.additive);
        Draw.color(tint);
        Draw.alpha(0.11f + 0.22f * activity);
        Draw.rect(glow, build.x, build.y, Time.time * speed + build.id % 360);
        Draw.blend();
        Draw.reset();
        Draw.z(z);
    }

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(base, plan.drawx(), plan.drawy(), block.rotate ? plan.rotation * 90f : 0f);
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{base};
    }
}
