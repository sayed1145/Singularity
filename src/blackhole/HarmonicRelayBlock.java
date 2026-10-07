package blackhole;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;

/**
 * The second half of Aurelia's new gameplay loop. A relay scans for three defended,
 * stabilized Rift Anchors around it. Only that spatial formation can synthesize a
 * Concord Core; ordinary conveyors and factory chains cannot substitute for it.
 */
public class HarmonicRelayBlock extends Block{
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

    public float linkRadius = 230f;
    public float outputTime = 60f * 12f;
    public Item output;

    public HarmonicRelayBlock(String name){
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        hasItems = true;
        itemCapacity = 10;
        buildType = HarmonicRelayBuild::new;
        sync = true;
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("harmonic-links", (HarmonicRelayBuild build) -> new Bar(
            () -> Core.bundle.format("bar.blackhole.harmonic-links", build.linked, 3),
            () -> build.linked >= 3 ? Color.valueOf("ffd875") : Color.valueOf("b69cff"),
            () -> Math.min(build.linked, 3) / 3f
        ));
    }

    public class HarmonicRelayBuild extends Building{
        public int linked;
        public float alignment, outputProgress;

        @Override
        public void updateTile(){
            // Re-evaluate only twice per second; this keeps large maps from turning the
            // formation mechanic into an every-tick building-group scan.
            if(timer(timerDump, 30f)){
                // Groups.build is built without a quadtree (EntityGroup(Building.class, false, false)), so
                // Groups.build.intersect() dereferences a null tree and threw a NullPointerException on the very
                // first tick after this block was placed - that was the crash on placing a Harmonic Relay.
                // The block indexer is the supported (and cheaper, tile-indexed, team-filtered) spatial query.
                final int[] count = {0};
                mindustry.Vars.indexer.eachBlock(this, linkRadius,
                    other -> other instanceof ResonanceAnchorBlock.ResonanceAnchorBuild anchor && anchor.stabilized(),
                    other -> count[0]++);
                linked = count[0];
            }

            boolean complete = linked >= 3 && efficiency > 0f;
            alignment = Mathf.lerpDelta(alignment, complete ? 1f : 0f, complete ? 0.028f : 0.065f);
            if(complete && items.total() < itemCapacity){
                outputProgress += edelta() * alignment;
                if(outputProgress >= outputTime){
                    outputProgress %= outputTime;
                    offload(output);
                    AureliaFx.concordBirth.at(x, y, 0f);
                }
            }
            dump();
        }

        @Override
        public void draw(){
            if(model3d != null) model3d.draw(this);
            else super.draw();
            if(alignment <= 0.01f) return;
            float z = Draw.z();
            Draw.z(z + 0.01f);
            Draw.blend(Blending.additive);
            Draw.color(Color.valueOf("b7e8ff"), Color.valueOf("9a83ff"), 0.5f + 0.5f * Mathf.sin(Time.time / 22f));
            Draw.alpha(0.12f + alignment * 0.28f);
            Lines.stroke(1.2f);
            Lines.circle(x, y, 14f + alignment * 10f);
            Draw.blend();
            Draw.reset();
            Draw.z(z);
        }
    }
}
