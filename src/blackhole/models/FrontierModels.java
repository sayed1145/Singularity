package blackhole.models;

import arc.graphics.*;
import arc.math.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import mindustry.gen.*;

import static blackhole.g3d.Kit.*;
import static blackhole.g3d.Mesh.*;

/**
 * v7.6 frontier models: the two new fluid plants, the ropeway mast, the gravity trap and the feedback lattice.
 *
 * <p>House rules, unchanged since v7.3: plain pearl / silver industrial shapes, at most two moving groups per
 * block, every solid inside its own footprint so a row of them never overlaps, and nothing above the north
 * clearance line {@code z <= 1.186 * (half - y)} so the baked perspective stays inside the block's own tile.
 */
public final class FrontierModels{
    private FrontierModels(){}

    static final Color coolant = Color.valueOf("a9f0ff"), graviton = Color.valueOf("9a7cff"),
        lumen = Color.valueOf("8fe9ff"), tide = Color.valueOf("3f7fc0");

    // ------------------------------------------------------------------------------------------ fluids

    /** Coolant condenser (3x3): a chilled column with a spinning compressor fan and two frost tanks. */
    public static class CoolantCondenser extends KitModel{
        final Mesh fan = new Mesh(), cap = new Mesh();

        public CoolantCondenser(){
            super("coolant-condenser", 3, aurelia);
            //compressor fan: four thin blades, two-sided so they never vanish while turning
            for(int i = 0; i < 4; i++){
                steel(fan, s).at(0, 0, 0).rot(2, i * 90f).hexa(true,
                    -0.35f, 0.4f, -0.06f, 0.35f, 0.4f, -0.06f, 0.35f, 2.5f, -0.06f, -0.35f, 2.5f, -0.06f,
                    -0.35f, 0.4f, 0.06f, 0.35f, 0.4f, 0.06f, 0.35f, 2.5f, 0.3f, -0.35f, 2.5f, 0.3f);
            }
            fan.twoSidedAll();
            dark(fan, s).at(0, 0, 0).cyl(8, 0.55f, -0.3f, 0.45f);
            trim(cap, s).at(0, 0, 0).lathe(8, 22.5f, 2.1f, 0f, 1.9f, 0.5f, 1.2f, 0.85f, 0.001f, 0.95f);
            glow(cap, s).at(0, 0, 0.1f).ring(8, 1.3f, 1.75f, 0.5f, 0.6f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //condensing column
            hull(m, s).at(0, 0, z).lathe(8, 22.5f, 4.6f, 0f, 4.6f, 3.2f, 4.0f, 4.0f, 4.0f, 6.0f, 0.001f, 6.0f);
            trim(m, s).at(0, 0, z).ring(8, 4.05f, 4.65f, 1.4f, 1.7f);
            glass(m, Color.valueOf("2d4f66")).at(0, 0, z).ring(8, 4.1f, 4.2f, 2.0f, 3.0f);
            dark(m, s).at(0, 0, z + 6.0f).ring(8, 1.9f, 4.0f, 0f, 0.3f);
            //two frost tanks on the south corners, linked to the column by a low pipe
            for(int sd = -1; sd <= 1; sd += 2){
                tank(m, s, sd * 8.0f, -7.4f, z, 1.9f, 3.4f, true);
                conduit(m, s, sd * 6.4f, -7.0f, z + 1.4f, sd * 3.4f, -3.4f, z + 1.4f, 0.5f);
            }
            //intake grate at the north edge
            grate(m, s).box(-3.2f, 6.6f, z, 3.2f, 9.2f, z + 0.16f);
            trim(m, s).box(-3.4f, 6.4f, z, -3.1f, 9.4f, z + 0.5f);
            trim(m, s).box(3.1f, 6.4f, z, 3.4f, 9.4f, z + 0.5f);
            hazard(m).at(0, 0, 0).box(-3.4f, -11.3f, z, 3.4f, -10.7f, z + 0.03f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ + 5.8f).cyl(12, 3.0f, 0f, 1.6f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(cap, 0, 0, deckZ + 6.3f, 2, 0);
            m.add(fan, 0, 0, deckZ + 6.5f, 2, 20f);
        }

        @Override
        public void drawLive(Building b){
            float w = warm(b), spin = total(b) * 9f;
            lit(0.6f + 0.4f * w);
            draw(cap, b, 0, 0, deckZ + 6.3f, 2, 0);
            draw(fan, b, 0, 0, deckZ + 6.5f, 2, spin);
            lit(1f);
        }

        @Override
        public void drawOver(Building b){
            float w = warm(b);
            light(b, 0, 0, deckZ + 6.9f, 3.2f, coolant, w * (0.2f + Mathf.absin(time(b), 8f, 0.1f)));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Graviton churn (4x4): a heavy drum turning on its side between two bearing blocks. */
    public static class GravitonChurn extends KitModel{
        final Mesh drum = new Mesh(), weight = new Mesh();

        public GravitonChurn(){
            super("graviton-churn", 4, aurelia);
            //the drum lies along x; it is built upright and laid down when drawn
            hull2(drum, s).at(0, 0, 0).lathe(12, 0f, 0.001f, -3.6f, 2.8f, -3.2f, 3.3f, -2.4f, 3.3f, 2.4f, 2.8f, 3.2f, 0.001f, 3.6f);
            dark(drum, s).at(0, 0, 0).ring(12, 2.9f, 3.35f, -0.25f, 0.25f);
            for(int i = 0; i < 3; i++){
                glow2(drum, s).at(0, 0, 0).rot(2, i * 120f).box(-0.3f, 2.9f, -2.2f, 0.3f, 3.42f, 2.2f);
            }
            steel(weight, s).at(0, 0, 0).cbevel(0, 0, 0, 1.5f, 1.5f, 1.2f, 0.2f);
            glow(weight, s).box(-0.5f, -0.55f, 1.2f, 0.5f, 0.55f, 1.3f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //two bearing housings carry the drum
            for(int sd = -1; sd <= 1; sd += 2){
                hull(m, s).at(0, 0, 0).cbevel(sd * 9.4f, 0f, z, 3.2f, 5.0f, 6.4f, 0.4f);
                dark(m, s).at(sd * 9.4f, 0, z + 4.6f).rot(0, 90).cyl(10, 1.5f, -0.2f, 2.2f);
                trim(m, s).box(sd * 9.4f - 2.6f, -5.0f, z + 6.4f, sd * 9.4f + 2.6f, 5.0f, z + 6.7f);
            }
            //service deck at the south, feed pipes at the north
            hull2(m, s).at(0, 0, 0).box(-5.4f, -12.2f, z, 5.4f, -8.6f, z + 1.4f);
            console(m, s, 0f, -10.4f, z + 1.4f);
            for(int sd = -1; sd <= 1; sd += 2){
                conduit(m, s, sd * 3.2f, 12.4f, z + 1.6f, sd * 3.2f, 6.4f, z + 1.6f, 0.62f);
                tank(m, s, sd * 12.2f, 9.4f, z, 1.7f, 2.6f, false);
            }
            grate(m, s).box(-4.6f, 5.2f, z, 4.6f, 7.4f, z + 0.16f);
            hazard(m).at(0, 0, 0).box(-4.6f, -15.3f, z, 4.6f, -14.6f, z + 0.03f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ + 4.6f).rot(0, 90).cyl(12, 3.6f, -7.0f, 7.0f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(drum, 0, 0, deckZ + 4.6f, 0, 90f);
            m.add(weight, 0, -10.4f, deckZ + 1.4f, 2, 0f);
        }

        @Override
        public void drawLive(Building b){
            float w = warm(b), spin = total(b) * 3.4f;
            lit(0.55f + 0.45f * w);
            //the drum turns about its own long axis: built upright, laid down, then rolled
            Live.pose(0, 0, deckZ + 4.6f, 0, 90f);
            Live.draw(drum, cam, b.x, b.y);
            lit(1f);
            draw(weight, b, 0, -10.4f, deckZ + 1.4f, 2, spin * 2f);
        }

        @Override
        public void drawOver(Building b){
            float w = warm(b);
            light(b, 0, 0, deckZ + 5.0f, 5.0f, graviton, w * (0.22f + Mathf.absin(time(b), 11f, 0.1f)));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    // ------------------------------------------------------------------------------------------ transport

    /** Skyline cableway (2x2): an A-frame mast with a pulley wheel and a counterweight. */
    public static class SkylineCableway extends KitModel{
        final Mesh wheel = new Mesh();

        public SkylineCableway(){
            super("skyline-cableway", 2, aurelia);
            trim(wheel, s).at(0, 0, 0).rot(0, 90).ring(12, 1.25f, 1.7f, -0.22f, 0.22f);
            dark(wheel, s).at(0, 0, 0).rot(0, 90).cyl(8, 0.45f, -0.3f, 0.3f);
            for(int i = 0; i < 4; i++){
                steel(wheel, s).at(0, 0, 0).rot(0, 90).rot(2, i * 45f).box(-0.14f, -1.3f, -0.1f, 0.14f, 1.3f, 0.1f);
            }
            wheel.twoSidedAll();
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //two legs leaning together into a mast head
            for(int sd = -1; sd <= 1; sd += 2){
                hull(m, s).hexa(true, sd * 4.6f - 0.8f, -1.1f, z, sd * 4.6f + 0.8f, -1.1f, z, sd * 4.6f + 0.8f, 1.1f, z, sd * 4.6f - 0.8f, 1.1f, z,
                    sd * 1.5f - 0.55f, -0.8f, z + 6.2f, sd * 1.5f + 0.55f, -0.8f, z + 6.2f, sd * 1.5f + 0.55f, 0.8f, z + 6.2f, sd * 1.5f - 0.55f, 0.8f, z + 6.2f);
                trim(m, s).box(sd * 4.6f - 1.1f, -1.4f, z, sd * 4.6f + 1.1f, 1.4f, z + 0.45f);
            }
            //mast head carries the pulley axle
            hull2(m, s).at(0, 0, 0).cbevel(0, 0, z + 6.2f, 2.4f, 1.4f, 1.1f, 0.2f);
            dark(m, s).at(0, 0, z + 6.9f).rot(0, 90).cyl(8, 0.35f, -1.9f, 1.9f);
            glow(m, s).box(-1.0f, -1.42f, z + 6.5f, 1.0f, -1.4f, z + 6.8f);
            //loading platform to the south with a hazard stripe
            grate(m, s).box(-3.2f, -6.4f, z, 3.2f, -2.6f, z + 0.16f);
            hazard(m).at(0, 0, 0).box(-2.4f, -7.4f, z, 2.4f, -6.9f, z + 0.03f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ + 6.9f).rot(0, 90).cyl(10, 1.9f, -0.5f, 0.5f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(wheel, 0, 0, deckZ + 6.9f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            //the pulley only turns while pods are actually on the rope
            float spin = total(b) * 6f;
            draw(wheel, b, 0, 0, deckZ + 6.9f, 1, spin);
        }
    }

    // ------------------------------------------------------------------------------------------ defence

    /** Gravity well (3x3): three inward leaning pylons around a dark funnel with a floating core. */
    public static class GravityWell extends KitModel{
        final Mesh core = gem(graviton, true, 1.5f, 2.6f), halo = new Mesh();

        public GravityWell(){
            super("gravity-well", 3, aurelia);
            trim(halo, s).at(0, 0, 0).ring(6, 3.0f, 3.5f, -0.18f, 0.18f);
            glow2(halo, s).at(0, 0, 0).ring(6, 3.05f, 3.45f, 0.18f, 0.26f);
            halo.twoSidedAll();
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //funnel: a dark cone sunk into the deck
            //closed cone: an open lathe profile leaves the baker looking at the inside of the mesh
            dark(m, s).at(0, 0, z).lathe(12, 0f, 0.001f, 0f, 5.0f, 0f, 4.4f, 0.9f, 2.4f, 1.8f, 0.001f, 1.95f);
            glow(m, s).at(0, 0, z).ring(12, 1.5f, 2.3f, 1.95f, 2.05f);
            //three pylons leaning in, 120 degrees apart
            for(int i = 0; i < 3; i++){
                float a = 90f + i * 120f, c = Mathf.cosDeg(a), sn = Mathf.sinDeg(a);
                hull(m, s).at(0, 0, 0).hexa(true, c * 8.4f - sn * 1.5f, sn * 8.4f + c * 1.5f, z, c * 8.4f + sn * 1.5f, sn * 8.4f - c * 1.5f, z,
                    c * 10.2f + sn * 1.2f, sn * 10.2f - c * 1.2f, z, c * 10.2f - sn * 1.2f, sn * 10.2f + c * 1.2f, z,
                    c * 5.0f - sn * 0.7f, sn * 5.0f + c * 0.7f, z + 6.2f, c * 5.0f + sn * 0.7f, sn * 5.0f - c * 0.7f, z + 6.2f,
                    c * 6.1f + sn * 0.6f, sn * 6.1f - c * 0.6f, z + 5.6f, c * 6.1f - sn * 0.6f, sn * 6.1f + c * 0.6f, z + 5.6f);
                glow2(m, s).at(c * 5.2f, sn * 5.2f, z + 6.2f).lathe(6, 30f, 0.5f, 0f, 0.001f, 0.9f);
                trim(m, s).at(0, 0, 0).cbox(c * 9.2f, sn * 9.2f, z, 2.6f, 2.6f, 0.5f);
            }
            hazard(m).at(0, 0, 0).box(-3.0f, -11.3f, z, 3.0f, -10.8f, z + 0.03f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ + 2.0f).cyl(12, 3.6f, 0f, 4.2f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(halo, 0, 0, deckZ + 3.8f, 2, 0);
            m.add(core, 0, 0, deckZ + 3.6f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float w = warm(b), t = time(b);
            float lift = deckZ + 3.6f;
            lit(0.5f + 0.9f * w);
            draw(halo, b, 0, 0, lift + 0.2f, 2, -t * 0.9f);
            draw(core, b, 0, 0, lift, 2, t * 1.6f);
            lit(1f);
        }

        @Override
        public void drawOver(Building b){
            float w = warm(b);
            light(b, 0, 0, deckZ + 3.8f, 4.6f, graviton, w * (0.25f + Mathf.absin(time(b), 9f, 0.12f)));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Aegis lattice (3x3): six emitter prisms around a hovering hexagonal plate. */
    public static class AegisLattice extends KitModel{
        final Mesh plate = new Mesh();

        public AegisLattice(){
            super("aegis-lattice", 3, aurelia);
            hull2(plate, s).at(0, 0, 0).lathe(6, 0f, 3.4f, -0.3f, 3.4f, 0.3f, 2.4f, 0.75f, 0.001f, 0.85f);
            glow(plate, s).at(0, 0, 0).ring(6, 2.5f, 3.3f, 0.3f, 0.38f);
            trim(plate, s).at(0, 0, 0).ring(6, 3.4f, 3.7f, -0.2f, 0.2f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //hex base ring
            hull(m, s).at(0, 0, z).lathe(6, 0f, 9.6f, 0f, 9.4f, 1.2f, 6.4f, 1.8f, 6.4f, 0.6f, 0.001f, 0.6f);
            dark(m, s).at(0, 0, z + 0.6f).lathe(6, 0f, 6.2f, 0f, 5.0f, 0.5f, 0.001f, 0.6f);
            trim(m, s).at(0, 0, z).ring(6, 9.0f, 9.5f, 1.2f, 1.5f);
            //six emitter prisms on the ring, leaning slightly outwards
            for(int i = 0; i < 6; i++){
                float a = i * 60f, c = Mathf.cosDeg(a), sn = Mathf.sinDeg(a);
                hull2(m, s).at(0, 0, 0).hexa(true, c * 7.6f - sn * 0.9f, sn * 7.6f + c * 0.9f, z + 1.8f, c * 7.6f + sn * 0.9f, sn * 7.6f - c * 0.9f, z + 1.8f,
                    c * 8.8f + sn * 0.8f, sn * 8.8f - c * 0.8f, z + 1.8f, c * 8.8f - sn * 0.8f, sn * 8.8f + c * 0.8f, z + 1.8f,
                    c * 7.8f - sn * 0.5f, sn * 7.8f + c * 0.5f, z + 4.4f, c * 7.8f + sn * 0.5f, sn * 7.8f - c * 0.5f, z + 4.4f,
                    c * 8.6f + sn * 0.45f, sn * 8.6f - c * 0.45f, z + 4.2f, c * 8.6f - sn * 0.45f, sn * 8.6f + c * 0.45f, z + 4.2f);
                glow2(m, s).at(c * 8.2f, sn * 8.2f, z + 4.4f).lathe(6, 30f, 0.42f, 0f, 0.001f, 0.8f);
            }
            hazard(m).at(0, 0, 0).box(-3.0f, -11.3f, z, 3.0f, -10.8f, z + 0.03f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ + 1.6f).cyl(12, 4.0f, 0f, 3.0f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(plate, 0, 0, deckZ + 2.8f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float w = warm(b), t = time(b), f = Mathf.clamp(b.progress());
            lit(0.45f + 1.1f * f * w + 0.2f * w);
            draw(plate, b, 0, 0, deckZ + 2.6f, 2, t * (0.5f + f * 2.5f));
            lit(1f);
        }

        @Override
        public void drawOver(Building b){
            float f = Mathf.clamp(b.progress());
            light(b, 0, 0, deckZ + 3.4f, 4.2f + f * 2f, lumen, warm(b) * (0.18f + f * 0.4f));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }
}
