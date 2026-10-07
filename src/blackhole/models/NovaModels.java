package blackhole.models;

import arc.graphics.*;
import arc.math.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import mindustry.gen.*;

import static blackhole.g3d.Kit.*;
import static blackhole.g3d.Mesh.*;

/**
 * v7.8 models: the smart transport family, the large power node and the resonance hub.
 *
 * <p>House rules of this version: plain shapes, few faces, and at most one slow moving piece per block - and
 * that piece always sits on the body, never beside it, so nothing can look detached while it turns.
 */
public final class NovaModels{
    private NovaModels(){}

    public static final Color lumen = Color.valueOf("8fe9ff"), graviton = Color.valueOf("b69cff");

    // =================================================================================================
    // transport family: one shared plinth, a different top per role
    // =================================================================================================

    /** Kinds of transport block, i.e. what sits on the shared plinth. */
    public static class LargeNode extends KitModel{
        public LargeNode(){
            super("aurora-node", 2, aurelia);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //four insulators at the corners
            for(int sx = -1; sx <= 1; sx += 2){
                for(int sy = -1; sy <= 1; sy += 2){
                    m.color(s.dark).style(metal, matPlain).at(sx * 4.4f, sy * 4.4f, z).cyl(6, 0.8f, 0f, 1.6f);
                    m.color(s.trim).style(metal, matPlain).at(sx * 4.4f, sy * 4.4f, z + 1.6f).ring(6, 0.5f, 1.0f, 0f, 0.35f);
                }
            }
            //central mast
            m.color(s.hull).style(metal, matPlate).at(0, 0, 0).cbevel(0, 0, z, 4.4f, 4.4f, 1.4f, 0.3f);
            m.color(s.metal).style(metal, matPlain).at(0, 0, z + 1.4f).cyl(8, 1.5f, 0f, 4.4f);
            m.color(s.trim).style(metal, matPlain).at(0, 0, z + 4.2f).ring(8, 1.5f, 2.2f, 0f, 0.5f);
            //lens on top
            m.color(lumen).style(emissive, matPlain).at(0, 0, z + 5.8f).lathe(10, 0, 1.4f, 0f, 1.0f, 1.0f, 0.001f, 1.5f);
            m.color(1f, 1f, 1f).style(Mesh.team, matPlain).at(0, 0, 0).box(-1.1f, -4.5f, z + 0.9f, 1.1f, -4.3f, z + 1.3f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
        }

        @Override
        public void buildRest(Mesh m){
        }

        @Override
        public void drawLive(Building b){
        }
    }

    // =================================================================================================
    // 共振枢纽
    // =================================================================================================

    /** Resonance hub: a 3x3 drum with a ring that lights up with the bank. The ring does not turn - it glows. */
    public static class ResonanceHubModel extends KitModel{
        public ResonanceHubModel(){
            super("resonance-hub", 3, aurelia);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            m.color(s.hull).style(metal, matPlate).at(0, 0, 0).cbevel(0, 0, z, 9.6f, 9.6f, 1.6f, 0.4f);
            //drum
            m.color(s.hull2).style(metal, matPlate).at(0, 0, z + 1.6f).cyl(12, 4.6f, 0f, 3.2f);
            m.color(s.dark).style(metal, matPlain).at(0, 0, z + 4.8f).ring(12, 3.4f, 4.8f, 0f, 0.5f);
            //three uprights carrying the top ring - they stand on the drum, so nothing floats
            for(int i = 0; i < 3; i++){
                m.color(s.metal).style(metal, matPlain).at(0, 0, 0).rot(2, i * 120f)
                    .box(-0.5f, 3.6f, z + 4.8f, 0.5f, 4.6f, z + 7.4f);
            }
            m.at(0, 0, 0).rot(2, 0f);
            m.color(s.trim).style(metal, matPlain).at(0, 0, z + 7.4f).ring(12, 3.2f, 4.4f, 0f, 0.7f);
            m.color(graviton).style(emissive, matPlain).at(0, 0, z + 7.5f).ring(12, 2.4f, 3.2f, 0f, 0.35f);
            //corner cabinets
            for(int sx = -1; sx <= 1; sx += 2){
                for(int sy = -1; sy <= 1; sy += 2){
                    m.color(s.dark).style(metal, matPlain).at(0, 0, 0)
                        .box(sx * 6.4f - 1.6f, sy * 6.4f - 1.6f, z, sx * 6.4f + 1.6f, sy * 6.4f + 1.6f, z + 2.2f);
                    m.color(lumen).style(emissive, matPlain).at(0, 0, 0)
                        .box(sx * 6.4f - 0.8f, sy * 6.4f - 1.65f, z + 0.7f, sx * 6.4f + 0.8f, sy * 6.4f - 1.62f, z + 1.5f);
                }
            }
            m.color(1f, 1f, 1f).style(Mesh.team, matPlain).at(0, 0, 0).box(-1.6f, -9.7f, z + 0.4f, 1.6f, -9.4f, z + 1.2f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
        }

        @Override
        public void buildRest(Mesh m){
        }

        @Override
        public void drawLive(Building b){
        }

        @Override
        public void drawOver(Building b){
            float f = b instanceof blackhole.NovaParts.ResonanceHub.ResonanceHubBuild h
                ? Mathf.clamp(h.charge / ((blackhole.NovaParts.ResonanceHub)b.block).capacity) : 0f;
            light(b, 0, 0, deckZ + 7.6f, 3.4f, graviton, 0.3f + f * 0.7f);
        }
    }
}
