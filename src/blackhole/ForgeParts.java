package blackhole;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.units.*;

/**
 * v8.0-beta block types (the air-cargo pair moved to AureliaLogix/LogixParts in 8.1).
 *
 * <ul>
 * <li>{@link WallBore} - the wall drill. It takes nothing from the ground: it fires one straight laser per
 * lane into the rock in front of it and cuts the ore seams out of the wall, which is what the v7.9 wall ore
 * was waiting for. Range and tier are its limits, so a seam has to be reachable in a straight line.</li>
 * <li>{@link ModelSolidPump} - the ground well: water out of damp soil, away from any lake.</li>
 * </ul>
 *
 * Both are drawn by their 3D {@link BlockModel}; every extra element (lasers, markers) is procedural, so
 * no sprite lookup can fail and paint the error region.
 */
public final class ForgeParts{
    private ForgeParts(){}

    /** Straight-laser wall drill. Mines ore seams inside solid rock, one lane per tile of width. */
    public static class WallBore extends BeamDrill{
        public BlockModel model;
        public Color laserColor = Color.valueOf("8fe9ff");
        public float beamWidth = 1.1f;

        public WallBore(String name){
            super(name);
            buildType = WallBoreBuild::new;
        }

        @Override
        public void load(){
            super.load();
            model = Models.get(name);
            if(model != null) model.load();
            //the vanilla drill draws a -top and a -glow sprite; this block is a 3D model, so both are disabled
            topRegion = Core.atlas.find(name + "-preview", region);
            glowRegion = Core.atlas.find("error-not-a-region");
        }

        @Override
        public TextureRegion[] icons(){
            return new TextureRegion[]{Core.atlas.find(name + "-preview", region)};
        }

        @Override
        public void getRegionsToOutline(Seq<TextureRegion> out){
            //the baked model carries its own outline
        }

        public class WallBoreBuild extends BeamDrillBuild{
            @Override
            public void draw(){
                if(model != null){
                    model.draw(this);
                }else{
                    super.draw();
                }
                if(isPayload()) return;

                //one straight laser per lane, drawn as geometry - the "direct line" to the seam it is cutting
                float a = warmup * (0.6f + Mathf.absin(arc.util.Time.time, 6f, 0.2f));
                if(a <= 0.02f) return;
                Draw.z(Layer.effect);
                for(int i = 0; i < size; i++){
                    Tile face = facing[i];
                    if(face == null) continue;
                    Item drop = face.wallDrop();
                    if(drop == null) continue;
                    var p = lasers[i];
                    float sx = p.x * mindustry.Vars.tilesize, sy = p.y * mindustry.Vars.tilesize;
                    float ex = face.worldx(), ey = face.worldy();
                    Draw.color(laserColor, a * 0.35f);
                    Lines.stroke(beamWidth * 2.4f);
                    Lines.line(sx, sy, ex, ey);
                    Draw.color(Color.white, drop.color, 0.6f);
                    Draw.alpha(a);
                    Lines.stroke(beamWidth * 0.8f);
                    Lines.line(sx, sy, ex, ey);
                    //cut marker on the rock face
                    Draw.color(drop.color, a);
                    Lines.stroke(0.9f);
                    Lines.poly(ex, ey, 4, 2.4f + Mathf.absin(arc.util.Time.time, 8f, 0.6f), arc.util.Time.time * 1.1f);
                    Fill.poly(ex, ey, 4, 1.0f, -arc.util.Time.time * 1.4f);
                }
                Draw.reset();
            }
        }
    }

    /** Ground well: a solid pump with a 3D model. */
    public static class ModelSolidPump extends SolidPump{
        public BlockModel model;

        public ModelSolidPump(String name){
            super(name);
            buildType = ModelSolidPumpBuild::new;
        }

        @Override
        public void load(){
            super.load();
            model = Models.get(name);
            if(model != null) model.load();
            rotatorRegion = Core.atlas.find("error-not-a-region");
        }

        @Override
        public TextureRegion[] icons(){
            return new TextureRegion[]{Core.atlas.find(name + "-preview", region)};
        }

        @Override
        public void getRegionsToOutline(Seq<TextureRegion> out){
        }

        public class ModelSolidPumpBuild extends SolidPumpBuild{
            @Override
            public void draw(){
                if(model != null){
                    model.draw(this);
                }else{
                    super.draw();
                }
            }
        }
    }
}
