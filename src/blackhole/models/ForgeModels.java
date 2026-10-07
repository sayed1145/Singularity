package blackhole.models;

import arc.graphics.*;
import arc.math.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import mindustry.gen.*;

import static blackhole.g3d.Kit.*;

/**
 * v8.0-beta block models: the wall borer, the cargo station and its drop pad, the large repair dome, the large
 * accumulator and the ground well.
 *
 * <p>Same rules as the rest of the kit - plain Aurelia pearl/silver industrial shapes, at most two moving parts
 * per block, every part attached to the body, nothing outside its own footprint.
 */
public final class ForgeModels{
    private ForgeModels(){}

    static final Color lumen = Color.valueOf("8fe9ff"), tide = Color.valueOf("3f7fc0"), prism = Color.valueOf("b69cff");

    /**
     * Wall borer (2x2): a squat laser head on a turntable. The emitter block turns with the block facing, so the
     * player can see which wall the lasers are cutting.
     */
    public static class WallBorer extends KitModel{
        final Mesh head = new Mesh();

        public WallBorer(){
            super("wall-bore", 2, aurelia);
            //emitter: a short barrel block with two lens apertures, built pointing east (model heading 0)
            hull(head, s).at(0, 0, 0).lathe(8, 22.5f, 2.3f, 0f, 2.3f, 1.5f, 1.7f, 2.2f, 0.001f, 2.2f);
            for(int i = -1; i <= 1; i += 2){
                steel(head, s).at(0, i * 1.1f, 0.7f).rot(1, 90).cyl(8, 0.52f, 1.6f, 3.2f);
                glow(head, s).at(0, i * 1.1f, 0.7f).rot(1, 90).lathe(6, 0, 0.42f, 3.2f, 0.001f, 3.6f);
            }
            dark(head, s).at(0, 0, 0).ring(8, 2.3f, 2.5f, 0.2f, 0.6f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //turntable base
            dark(m, s).at(0, 0, z).ring(12, 2.6f, 3.6f, 0f, 0.7f);
            hull2(m, s).at(0, 0, 0).taper(0, 0, z + 0.7f, 3.2f, 3.2f, z + 2.2f, 2.4f, 2.4f, 0, 0, true);
            bolts(m, s, 0, 0, z + 0.7f, 3.0f, 8);
            //power cabinet to the west and a spoil chute to the south, both low and inside the footprint
            hull2(m, s).at(0, 0, 0).cbevel(-5.0f, 0.6f, z, 1.6f, 2.4f, 2.2f, 0.2f);
            glow(m, s).box(-5.9f, -0.2f, z + 1.2f, -5.75f, 1.4f, z + 1.9f);
            hull2(m, s).at(0, 0, 0).cbevel(0.6f, -5.2f, z, 2.6f, 1.5f, 1.0f, 0.18f);
            conduit(m, s, -3.4f, 0.6f, z + 0.8f, -1.6f, 0.6f, z + 0.8f, 0.35f);
            console(m, s, 4.6f, -4.6f, z);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-3.6f, -3.6f, deckZ + 2.2f, 3.6f, 3.6f, deckZ + 5.2f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(head, 0, 0, deckZ + 2.3f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            draw(head, b, 0, 0, deckZ + 2.3f, 2, b.rotdeg());
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 3.4f, 2.4f, lumen, warm(b) * (0.22f + Mathf.absin(time(b), 5f, 0.12f)));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Cargo station (3x3): an open landing deck with a service mast; the drone sits on the pad between runs. */
    public static class CargoStation extends KitModel{
        final Mesh vane = new Mesh();

        public CargoStation(){
            super("lumen-cargo-station", 3, aurelia);
            //a slow guidance vane on the mast - one moving part, flat in XY, spun about the vertical axis
            for(int i = 0; i < 3; i++){
                trim(vane, s).at(0, 0, 0).rot(2, i * 120f).box(-0.3f, 0.6f, 0f, 0.3f, 3.4f, 0.32f);
            }
            steel(vane, s).at(0, 0, 0).cyl(8, 0.55f, -0.2f, 0.5f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //landing pad: a shallow dish with a lit rim, kept clear so the drone has somewhere to sit
            dark(m, s).at(0, 0, z).ring(12, 4.6f, 5.6f, 0f, 0.5f);
            glow(m, s).at(0, 0, z + 0.5f).ring(12, 4.7f, 5.3f, 0f, 0.12f);
            hull2(m, s).at(0, 0, z).lathe(12, 15f, 5.6f, 0f, 5.6f, 0.9f, 4.8f, 1.2f);
            //corner clamps
            for(int i = 0; i < 4; i++){
                float sx = (i & 1) == 0 ? -1 : 1, sy = (i & 2) == 0 ? -1 : 1;
                hull2(m, s).at(0, 0, 0).cbevel(sx * 8.4f, sy * 8.4f, z, 2.0f, 2.0f, 1.6f, 0.22f);
            }
            //mast on the west side, item hopper on the east
            hull(m, s).at(0, 0, 0).cbevel(-7.4f, 0f, z, 1.8f, 2.6f, 5.4f, 0.3f);
            glow(m, s).box(-8.3f, -1.0f, z + 3.2f, -8.15f, 1.0f, z + 4.6f);
            hull2(m, s).at(0, 0, 0).taper(7.3f, 0f, z, 2.2f, 3.2f, z + 3.4f, 1.5f, 2.4f, 0, 0, true);
            conduit(m, s, -5.6f, 0f, z + 1.1f, 5.4f, 0f, z + 1.1f, 0.38f);
            console(m, s, 4.8f, -7.6f, z);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-9f, -9f, deckZ + 0.4f, 9f, 9f, deckZ + 6.4f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(vane, -7.4f, 0f, deckZ + 5.6f, 2, 0f);
        }

        @Override
        public void drawLive(Building b){
            draw(vane, b, -7.4f, 0f, deckZ + 5.6f, 2, total(b) * 1.4f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 1.2f, 4.2f, lumen, warm(b) * 0.2f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Cargo drop pad (1x1): a plain marked plate the drones unload onto. */
    public static class CargoPoint extends KitModel{
        public CargoPoint(){
            super("lumen-cargo-point", 1, aurelia);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            dark(m, s).at(0, 0, z).ring(10, 1.6f, 2.6f, 0f, 0.4f);
            hull2(m, s).at(0, 0, z).lathe(10, 18f, 2.6f, 0f, 2.6f, 0.5f, 2.0f, 0.8f);
            glow(m, s).at(0, 0, z + 0.8f).ring(10, 0.8f, 1.5f, 0f, 0.1f);
            for(int i = 0; i < 4; i++){
                float sx = (i & 1) == 0 ? -1 : 1, sy = (i & 2) == 0 ? -1 : 1;
                trim(m, s).at(0, 0, 0).box(sx * 2.6f - 0.4f, sy * 2.6f - 0.4f, z, sx * 2.6f + 0.4f, sy * 2.6f + 0.4f, z + 0.6f);
            }
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-3f, -3f, deckZ + 0.2f, 3f, 3f, deckZ + 1.2f);
        }

        @Override
        public void buildRest(Mesh m){
            //no separate meshes - the pad is one piece
        }

        @Override
        public void drawLive(Building b){
            //nothing moves on a drop pad
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 0.9f, 1.6f, lumen, 0.18f);
        }
    }

    /** Large repair dome (3x3): a ribbed dome with a lens on top; the lens pulses when it heals. */
    public static class MendDome extends KitModel{
        final Mesh lens = new Mesh();

        public MendDome(){
            super("lumen-mend-dome", 3, aurelia);
            glass(lens, Color.valueOf("4e86b8")).at(0, 0, 0).lathe(12, 15f, 2.4f, 0f, 2.2f, 1.1f, 1.4f, 1.9f, 0.001f, 2.2f);
            glow(lens, s).at(0, 0, 0).cyl(10, 0.8f, 2.1f, 2.5f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            dark(m, s).at(0, 0, z).ring(12, 5.2f, 6.4f, 0f, 0.8f);
            hull(m, s).at(0, 0, z).lathe(12, 15f, 6.2f, 0f, 6.2f, 1.6f, 5.2f, 3.2f, 4.0f, 4.4f, 0.001f, 4.8f);
            //rib cage, flat plates laid on the dome
            for(int i = 0; i < 6; i++){
                //offset by half a step so no rib sits exactly on the axis (and none is built in an identity frame)
                trim(m, s).at(0, 0, z + 1.6f).rot(2, i * 60f + 15f).box(-0.28f, 3.6f, 0f, 0.28f, 5.4f, 1.5f);
            }
            glass(m, Color.valueOf("3c6b7e")).at(0, 0, z).ring(12, 5.25f, 5.45f, 1.8f, 2.8f);
            //service cabinet south, cable trunk west
            hull2(m, s).at(0, 0, 0).cbevel(0f, -7.6f, z, 3.2f, 1.6f, 1.8f, 0.2f);
            conduit(m, s, -6.0f, -2.2f, z + 0.9f, -2.6f, -2.2f, z + 0.9f, 0.4f);
            console(m, s, 6.2f, -6.2f, z);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-6.6f, -6.6f, deckZ + 4.4f, 6.6f, 6.6f, deckZ + 7.6f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(lens, 0, 0, deckZ + 4.6f, 2, 0f);
        }

        @Override
        public void drawLive(Building b){
            draw(lens, b, 0, 0, deckZ + 4.6f, 2, total(b) * 0.6f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 6.4f, 4.6f, lumen, 0.2f + warm(b) * (0.2f + Mathf.absin(time(b), 9f, 0.15f)));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Large accumulator (3x3): four cells in a frame with a charge column in the middle. */
    public static class Accumulator extends KitModel{
        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //one continuous base plate, so the deck does not show through between the cells
            plain(m, s).at(0, 0, 0).box(-8.6f, -8.6f, z, 8.6f, 8.6f, z + 0.45f);
            //frame
            for(int i = 0; i < 4; i++){
                float sx = (i & 1) == 0 ? -1 : 1, sy = (i & 2) == 0 ? -1 : 1;
                hull(m, s).at(0, 0, 0).cbevel(sx * 5.4f, sy * 5.4f, z + 0.45f, 3.0f, 3.0f, 2.2f, 0.35f);
                glass(m, Color.valueOf("3a6f86")).box(sx * 5.4f - 2.1f, sy * 5.4f - 2.1f, z + 2.2f, sx * 5.4f + 2.1f, sy * 5.4f + 2.1f, z + 4.6f);
                //small cap only - the cell glass stays visible from above
                trim(m, s).box(sx * 5.4f - 1.5f, sy * 5.4f - 1.5f, z + 4.6f, sx * 5.4f + 1.5f, sy * 5.4f + 1.5f, z + 5.0f);
            }
            //middle column
            dark(m, s).at(0, 0, z + 0.45f).ring(10, 1.6f, 2.4f, 0f, 0.6f);
            hull2(m, s).at(0, 0, z + 0.45f).lathe(10, 18f, 2.2f, 0.6f, 2.2f, 4.6f, 1.5f, 5.4f, 0.001f, 5.8f);
            glow(m, s).at(0, 0, z + 0.45f).ring(10, 2.25f, 2.4f, 1.2f, 4.2f);
            //bus bars to the cells, laid flat on the deck
            for(int i = 0; i < 4; i++){
                float sx = (i & 1) == 0 ? -1 : 1, sy = (i & 2) == 0 ? -1 : 1;
                conduit(m, s, sx * 1.6f, sy * 1.6f, z + 0.95f, sx * 4.2f, sy * 4.2f, z + 0.95f, 0.34f);
            }
            console(m, s, 0f, -8.0f, z);
            m.at(0, 0, 0);
        }

        public Accumulator(){
            super("lumen-accumulator", 3, aurelia);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-9f, -9f, deckZ + 0.4f, 9f, 9f, deckZ + 6.2f);
        }

        @Override
        public void buildRest(Mesh m){
            //no separate meshes - the bank is one piece
        }

        @Override
        public void drawLive(Building b){
            //no moving parts - the bank only lights up
        }

        @Override
        public void drawOver(Building b){
            //the column brightens with the charge in the bank
            float f = b.power != null && b.power.graph != null ? Mathf.clamp(b.power.status) : 0f;
            light(b, 0, 0, deckZ + 4.4f, 3.6f, lumen, 0.12f + f * 0.4f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Ground well (2x2): a derrick with a walking beam that nods while it pumps water out of the soil. */
    public static class GroundWell extends KitModel{
        final Mesh beam = new Mesh();

        public GroundWell(){
            super("ground-well", 2, aurelia);
            //walking beam, built in the XZ plane and rocked about the Y axis, so it stays in its own plane
            steel(beam, s).at(0, 0, 0).box(-3.4f, -0.45f, -0.3f, 3.4f, 0.45f, 0.3f);
            hull2(beam, s).at(0, 0, 0).cbevel(2.9f, 0f, -0.9f, 0.9f, 0.8f, 1.2f, 0.18f);
            trim(beam, s).at(0, 0, 0).box(-3.6f, -0.7f, -0.7f, -2.6f, 0.7f, 0.7f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //well head and sump
            dark(m, s).at(0, 0, z).ring(10, 1.5f, 2.4f, 0f, 0.5f);
            //closed at both ends: start and finish with a collapsed radius so the sump is a solid body
            hull2(m, s).at(2.8f, 0f, z).lathe(10, 18f, 0.001f, 0f, 1.6f, 0f, 1.6f, 1.4f, 1.0f, 1.8f, 0.001f, 1.9f);
            //derrick mast, a plain tapered box frame
            hull(m, s).at(0, 0, 0).taper(-1.6f, 0f, z, 2.2f, 2.2f, z + 4.6f, 1.2f, 1.2f, 0, 0, true);
            trim(m, s).box(-2.8f, -1.3f, z + 4.6f, -0.4f, 1.3f, z + 4.9f);
            //filter tank to the south, outlet pipe to the north
            tank(m, s, -4.4f, -3.6f, z, 1.5f, 2.6f, true);
            conduit(m, s, 2.8f, 1.2f, z + 0.9f, 2.8f, 5.2f, z + 0.9f, 0.42f);
            glow(m, s).box(-0.8f, 5.0f, z + 0.55f, 0.8f, 5.3f, z + 1.25f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-4.2f, -2.2f, deckZ + 4.4f, 4.2f, 2.2f, deckZ + 7.4f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(beam, -1.6f, 0f, deckZ + 5.4f, 1, 0f);
        }

        @Override
        public void drawLive(Building b){
            //a nod, not a spin: the beam rocks +-14 degrees about its pivot, in its own plane
            draw(beam, b, -1.6f, 0f, deckZ + 5.4f, 1, Mathf.sin(total(b) * 0.09f) * 14f * warm(b));
        }

        @Override
        public void drawOver(Building b){
            light(b, 2.8f, 0f, deckZ + 1.6f, 2.2f, tide, warm(b) * 0.3f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }
}
