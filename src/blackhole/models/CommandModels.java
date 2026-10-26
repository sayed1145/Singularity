package blackhole.models;

import arc.graphics.*;
import arc.math.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import mindustry.gen.*;

import static blackhole.g3d.Kit.*;

/**
 * v8.2 block models: the two overdrive projectors.
 *
 * <p>They are the same machine at two sizes, so they read as a pair: emitter posts around a central lens, the
 * big one with a second ring and a booster hopper. Plain Aurelia pearl and silver, exactly one moving part per
 * block (the lens ring, which turns while the projector works), nothing overlapping, nothing off the footprint.
 */
public final class CommandModels{
    private CommandModels(){}

    static final Color lumen = Color.valueOf("8fe9ff"), prism = Color.valueOf("b69cff");

    /** Lumen accelerator (2x2): four emitter posts around one spinning lens collar. */
    public static class Accelerator extends KitModel{
        final Mesh lens = new Mesh();

        public Accelerator(){
            super("lumen-accelerator", 2, aurelia);
            trim(lens, s).ring(10, 1.5f, 2.2f, -0.3f, 0.3f);
            glow(lens, s).lathe(10, 0, 0.001f, -0.25f, 1.3f, -0.25f, 1.3f, 0.45f, 0.001f, 0.9f);
            lens.at(0, 0, 0);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            hull(m, s).at(0, 0, 0).lathe(10, 18f, 0.001f, z, 3.1f, z, 3.1f, z + 0.9f, 2.5f, z + 1.6f, 0.001f, z + 1.6f);
            dark(m, s).at(0, 0, 0).ring(10, 2.5f, 3.1f, z + 1.55f, z + 1.7f);
            for(int i = 0; i < 4; i++){
                float a = 45f + i * 90f;
                float px = Mathf.cosDeg(a) * 4.4f, py = Mathf.sinDeg(a) * 4.4f;
                steel(m, s).at(px, py, 0).cyl(8, 0.62f, z, z + 2.6f);
                m.at(0, 0, 0);
                trim(m, s).at(px, py, 0).lathe(8, 0, 0.95f, z + 2.6f, 0.95f, z + 3.0f, 0.5f, z + 3.3f);
                m.at(0, 0, 0);
                glow(m, s).at(px, py, 0).cyl(8, 0.42f, z + 3.3f, z + 3.5f);
                m.at(0, 0, 0);
            }
            bolts(m, s, 0, 0, z + 0.05f, 3.4f, 8);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-2.6f, -2.6f, deckZ + 1.6f, 2.6f, 2.6f, deckZ + 3.6f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(lens, 0f, 0f, deckZ + 2.1f, 2, 0f);
        }

        @Override
        public void drawLive(Building b){
            draw(lens, b, 0f, 0f, deckZ + 2.1f, 2, total(b) * 1.6f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 2.4f, 2.2f, lumen, 0.14f + Mathf.absin(time(b), 14f, 0.08f) * warm(b));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Prism overcharger (3x3): six posts, twin counter-turning rings, a booster hopper. */
    public static class Overcharger extends KitModel{
        final Mesh outer = new Mesh(), inner = new Mesh();

        public Overcharger(){
            super("prism-overcharger", 3, aurelia);
            trim(outer, s).ring(12, 2.4f, 3.5f, -0.35f, 0.35f);
            glow(outer, s).lathe(12, 0, 0.001f, -0.3f, 2.0f, -0.3f, 2.0f, 0.5f, 0.001f, 1.25f);
            outer.at(0, 0, 0);
            steel(inner, s).ring(12, 1.1f, 1.9f, -0.3f, 0.3f);
            glow2(inner, s).lathe(12, 0, 0.001f, -0.25f, 1.0f, -0.25f, 0.001f, 0.85f);
            inner.at(0, 0, 0);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            hull(m, s).at(0, 0, 0).lathe(12, 15f, 0.001f, z, 5.0f, z, 5.0f, z + 1.1f, 4.1f, z + 2.1f, 0.001f, z + 2.1f);
            dark(m, s).at(0, 0, 0).ring(12, 4.1f, 5.0f, z + 2.05f, z + 2.25f);
            for(int i = 0; i < 6; i++){
                float a = 30f + i * 60f;
                float px = Mathf.cosDeg(a) * 7.3f, py = Mathf.sinDeg(a) * 7.3f;
                hull2(m, s).at(0, 0, 0).cbevel(px, py, z + 0.8f, 1.1f, 1.1f, 0.8f, 0.2f);
                steel(m, s).at(px, py, 0).cyl(8, 0.7f, z + 1.6f, z + 4.2f);
                m.at(0, 0, 0);
                trim(m, s).at(px, py, 0).lathe(8, 0, 1.1f, z + 4.2f, 1.1f, z + 4.6f, 0.55f, z + 5.0f);
                m.at(0, 0, 0);
                glow2(m, s).at(px, py, 0).cyl(8, 0.46f, z + 5.0f, z + 5.25f);
                m.at(0, 0, 0);
            }
            //booster hopper on the south face - where the resonance shard goes in
            dark(m, s).at(0, 0, 0).cbevel(0f, -6.4f, z + 1.5f, 1.8f, 1.2f, 1.5f, 0.25f);
            m.at(0, 0, 0);
            glow(m, s).box(-1.2f, -6.8f, z + 3.0f, 1.2f, -6.0f, z + 3.15f);
            m.at(0, 0, 0);
            bolts(m, s, 0, 0, z + 0.05f, 5.4f, 12);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-4.2f, -4.2f, deckZ + 2.2f, 4.2f, 4.2f, deckZ + 5.4f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(outer, 0f, 0f, deckZ + 2.8f, 2, 0f);
            m.add(inner, 0f, 0f, deckZ + 3.6f, 2, 0f);
        }

        @Override
        public void drawLive(Building b){
            float t = total(b);
            draw(outer, b, 0f, 0f, deckZ + 2.8f, 2, t * 1.1f);
            draw(inner, b, 0f, 0f, deckZ + 3.6f, 2, -t * 0.8f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 3.6f, 3.4f, prism, 0.16f + Mathf.absin(time(b), 16f, 0.1f) * warm(b));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }
}
