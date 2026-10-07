package blackhole;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.draw.*;

/** Keeps normal aiming/recoil behavior, then adds a restrained rotating Aurelia prism layer. */
public class DrawAureliaTurret extends DrawTurret{
    private TextureRegion glow;
    private final Color tint;

    public DrawAureliaTurret(Color tint){
        this.tint = tint;
    }

    @Override
    public void load(Block block){
        super.load(block);
        glow = Core.atlas.find(block.name + "-glow");
    }

    @Override
    public void draw(Building build){
        super.draw(build);
        if(glow == null || !glow.found()) return;
        Turret.TurretBuild turret = (Turret.TurretBuild)build;
        float z = Draw.z();
        Draw.z(z + 0.01f);
        Draw.blend(Blending.additive);
        Draw.color(tint);
        Draw.alpha(0.12f + 0.24f * (0.3f + turret.warmup()));
        Draw.rect(glow, build.x, build.y, Time.time * 0.7f + turret.rotation);
        Draw.blend();
        Draw.reset();
        Draw.z(z);
    }
}
