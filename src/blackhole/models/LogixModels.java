package blackhole.models;

import arc.graphics.*;
import arc.math.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import mindustry.gen.*;

import static blackhole.g3d.Kit.*;

/**
 * v8.1 block models: the drone-network nodes (supply post, buffer post, vault), the wireless liquid relay, the
 * two liquid tanks and the overflow / underflow gates.
 *
 * <p>Same rules as the rest of the kit: plain Aurelia pearl and silver industry, at most one moving part per
 * block, every part sitting on the body it belongs to, nothing crossing another part and nothing leaving the
 * block's own footprint.
 */
public final class LogixModels{
    private LogixModels(){}

    static final Color lumen = Color.valueOf("8fe9ff"), prism = Color.valueOf("b69cff"), tide = Color.valueOf("3f7fc0");

    /** Supply post (2x2): an open hopper on a low plinth; belts feed the sides, drones lift from the top. */

    /** Buffer post (2x2): two stacked drums with a selector dial - the local stock of one item. */

    /** Logistics vault (3x3): four crate bays around a service mast - the network's warehouse. */

    /** Wireless liquid relay (2x2): a pylon with a resonance ring that spins while liquid is moving. */
    public static class LiquidRelay extends KitModel{
        final Mesh ring = new Mesh();

        public LiquidRelay(){
            super("lumen-liquid-relay", 2, aurelia);
            //the emitter ring: one flat torus about the vertical axis, nothing else moves
            torus(ring, Color.valueOf("8fe9ff"), Mesh.emissive, 0, 0, 0, 2.5f, 0.3f, 0.3f, 14);
            for(int i = 0; i < 3; i++){
                trim(ring, s).at(0, 0, 0).rot(2, i * 120f).box(-0.22f, 0.9f, -0.12f, 0.22f, 2.5f, 0.12f);
            }
            ring.at(0, 0, 0);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //base collar and pylon
            dark(m, s).at(0, 0, z).ring(12, 2.4f, 3.3f, 0f, 0.55f);
            hull2(m, s).at(0, 0, 0).taper(0, 0, z + 0.55f, 2.6f, 2.6f, z + 4.2f, 1.3f, 1.3f, 0, 0, true);
            steel(m, s).at(0, 0, z + 4.2f).cyl(8, 0.7f, 0f, 1.4f);
            //two liquid ports, east and west, with their own short runs - nothing crosses the pylon
            for(int i = -1; i <= 1; i += 2){
                hull(m, s).at(0, 0, 0).cbevel(i * 5.2f, 0f, z, 1.2f, 2.0f, 1.3f, 0.2f);
                conduit(m, s, i * 4.0f, 0f, z + 0.8f, i * 2.4f, 0f, z + 0.8f, 0.38f);
                glow(m, s).box(i * 6.2f - 0.12f, -0.9f, z + 0.4f, i * 6.2f + 0.12f, 0.9f, z + 1.2f);
            }
            bolts(m, s, 0, 0, z + 0.55f, 2.9f, 8);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-3f, -3f, deckZ + 4.6f, 3f, 3f, deckZ + 6.6f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(ring, 0f, 0f, deckZ + 5.6f, 2, 0f);
        }

        @Override
        public void drawLive(Building b){
            draw(ring, b, 0f, 0f, deckZ + 5.6f, 2, total(b) * 1.1f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 5.6f, 2.6f, lumen, 0.2f + Mathf.absin(time(b), 12f, 0.08f));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Liquid tank (2x2): one banded cylinder with a gauge - plain, and it reads as a tank at any zoom. */
    public static class TankSmall extends KitModel{
        public TankSmall(){
            super("lumen-tank", 2, aurelia);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            dark(m, s).at(0, 0, z).ring(14, 3.6f, 4.4f, 0f, 0.5f);
            //body: closed at both ends, two bands
            hull2(m, s).at(0, 0, z + 0.5f).lathe(14, 12.8f, 0.001f, 0f, 3.9f, 0.5f, 3.9f, 3.6f, 0.001f, 4.2f);
            trim(m, s).at(0, 0, z).ring(14, 3.9f, 4.1f, 1.4f, 1.7f);
            trim(m, s).at(0, 0, z).ring(14, 3.9f, 4.1f, 2.9f, 3.2f);
            //gauge strip on the south face and a port to the north
            m.at(0, 0, 0);
            glow(m, s).box(-0.5f, -4.2f, z + 1.0f, 0.5f, -4.0f, z + 3.4f);
            conduit(m, s, 0f, 3.6f, z + 1.0f, 0f, 5.4f, z + 1.0f, 0.4f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-4.4f, -4.4f, deckZ + 3.5f, 4.4f, 4.4f, deckZ + 5f);
        }

        @Override
        public void buildRest(Mesh m){
        }

        @Override
        public void drawLive(Building b){
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, -4.2f, deckZ + 2.2f, 1.4f, tide, 0.14f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Large reservoir (3x3): the same tank language, one size up, with a stair and a dome roof. */
    public static class TankLarge extends KitModel{
        public TankLarge(){
            super("lumen-reservoir", 3, aurelia);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            dark(m, s).at(0, 0, z).ring(16, 5.6f, 6.6f, 0f, 0.6f);
            //wall, then a shallow dome lid on top of it
            hull2(m, s).at(0, 0, z + 0.6f).lathe(16, 11.25f, 0.001f, 0f, 6.1f, 0.6f, 6.1f, 5.0f, 4.6f, 6.0f, 0.001f, 6.6f);
            trim(m, s).at(0, 0, z).ring(16, 6.1f, 6.35f, 2.0f, 2.35f);
            trim(m, s).at(0, 0, z).ring(16, 6.1f, 6.35f, 4.0f, 4.35f);
            //gauge column and two ports, each on its own face
            m.at(0, 0, 0);
            glow(m, s).box(-0.6f, -6.4f, z + 1.2f, 0.6f, -6.15f, z + 4.8f);
            conduit(m, s, 0f, 5.8f, z + 1.2f, 0f, 8.2f, z + 1.2f, 0.46f);
            conduit(m, s, 5.8f, 0f, z + 1.2f, 8.2f, 0f, z + 1.2f, 0.46f);
            console(m, s, -7.2f, -7.2f, z);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-6.6f, -6.6f, deckZ + 5f, 6.6f, 6.6f, deckZ + 7.2f);
        }

        @Override
        public void buildRest(Mesh m){
        }

        @Override
        public void drawLive(Building b){
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 7f, 2.4f, tide, 0.12f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Overflow gate (1x1): a low cross plate with one lit outlet arrow per side. */
    public static class OverflowGate extends KitModel{
        final boolean invert;

        public OverflowGate(String name, boolean invert){
            super(name, 1, aurelia);
            this.invert = invert;
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            hull2(m, s).at(0, 0, 0).cbevel(0f, 0f, z, 3.0f, 3.0f, 0.9f, 0.3f);
            //the marking: a diamond for overflow, a square for underflow, so the two never get mixed up
            if(invert){
                dark(m, s).box(-1.5f, -1.5f, z + 0.9f, 1.5f, 1.5f, z + 1.05f);
                glow2(m, s).at(0, 0, z + 1.05f).ring(4, 0.8f, 1.5f, 0f, 0.12f);
            }else{
                dark(m, s).at(0, 0, z + 0.9f).rot(2, 45f).box(-1.5f, -1.5f, 0f, 1.5f, 1.5f, 0.15f);
                m.at(0, 0, 0);
                glow(m, s).at(0, 0, z + 1.05f).rot(2, 45f).ring(4, 0.8f, 1.5f, 0f, 0.12f);
                m.at(0, 0, 0);
            }
            //four outlet lips, one per side, none of them touching
            for(int i = 0; i < 4; i++){
                trim(m, s).at(0, 0, 0).rot(2, i * 90f).box(2.9f, -1.1f, z + 0.1f, 3.5f, 1.1f, z + 0.55f);
                m.at(0, 0, 0);
            }
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-3.5f, -3.5f, deckZ + 0.4f, 3.5f, 3.5f, deckZ + 1.2f);
        }

        @Override
        public void buildRest(Mesh m){
        }

        @Override
        public void drawLive(Building b){
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 1.1f, 1.3f, invert ? prism : lumen, 0.16f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }
}
