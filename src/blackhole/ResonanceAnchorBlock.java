package blackhole;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;

/**
 * A field structure, not a conventional factory. It can only be placed on a natural
 * resonance bed and must be defended while it stabilizes the local rift.
 */
public class ResonanceAnchorBlock extends Block{
    public blackhole.g3d.BlockModel model3d;

    @Override
    public void load(){
        super.load();
        model3d = blackhole.models.Models.get(name);
        if(model3d != null) model3d.load();
    }

    @Override
    public arc.graphics.g2d.TextureRegion[] icons(){
        return new arc.graphics.g2d.TextureRegion[]{arc.Core.atlas.find(name + "-preview", region)};
    }

    public float stabilizeTime = 60f * 16f;
    public float outputTime = 60f * 7f;
    public float defendRadius = 84f;
    public Item output;

    public ResonanceAnchorBlock(String name){
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        hasItems = true;
        itemCapacity = 12;
        buildType = ResonanceAnchorBuild::new;
        sync = true;
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("rift-stability", (ResonanceAnchorBuild build) -> new Bar(
            () -> Core.bundle.get(build.threatened ? "bar.blackhole.rift-threatened" : "bar.blackhole.rift-stability"),
            () -> build.threatened ? Color.valueOf("ff7f8d") : Color.valueOf("72d7ff"),
            () -> build.stability
        ));
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        return super.canPlaceOn(tile, team, rotation) && tile.floor() == AureliaContent.resonanceBed;
    }

    public class ResonanceAnchorBuild extends Building{
        public float stability, outputProgress;
        private boolean threatened, wasStabilized;

        @Override
        public void updateTile(){
            final boolean[] hostile = {false};
            Units.nearbyEnemies(team, x, y, defendRadius, unit -> {
                if(!unit.dead() && unit.within(x, y, defendRadius)) hostile[0] = true;
            });
            threatened = hostile[0];

            if(efficiency > 0f && !threatened){
                stability = Math.min(1f, stability + edelta() / stabilizeTime);
            }else if(threatened){
                // Losing progress rather than destroying it makes defense important without
                // turning the mechanic into a one-failure soft lock.
                stability = Math.max(0f, stability - Time.delta / (stabilizeTime * 2.5f));
            }

            boolean stable = stabilized();
            if(stable && !wasStabilized){
                AureliaFx.concordBirth.at(x, y, 0f);
            }
            wasStabilized = stable;
            if(stable && efficiency > 0f && items.total() < itemCapacity){
                outputProgress += edelta();
                if(outputProgress >= outputTime){
                    outputProgress %= outputTime;
                    offload(output);
                    AureliaFx.lumenHit.at(x, y, 0f);
                }
            }
            dump();
        }

        public boolean stabilized(){
            return stability >= 0.999f;
        }

        @Override
        public void draw(){
            if(model3d != null) model3d.draw(this);
            else super.draw();
            float z = Draw.z();
            Draw.z(z + 0.01f);
            Draw.color(threatened ? Color.valueOf("ff7f8d") : Color.valueOf("72d7ff"));
            Draw.alpha(0.15f + stability * 0.38f);
            Lines.stroke(1f + stability * 0.75f);
            Lines.circle(x, y, 8f + stability * 9f + Mathf.absin(Time.time + id, 26f, 1.2f));
            Draw.reset();
            Draw.z(z);
        }
    }
}
