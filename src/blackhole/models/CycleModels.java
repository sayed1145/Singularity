package blackhole.models;

import arc.graphics.*;
import arc.math.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import mindustry.gen.*;

import static blackhole.g3d.Kit.*;
import static blackhole.g3d.Mesh.*;

/**
 * v8.3 block models: the seven machines of the waste cycle, the ward dome and the tier-3 core.
 *
 * <p>Same house rules as the rest of the kit - Aurelia pearl and silver, at most one moving part per block,
 * every part standing on the body it belongs to, nothing overlapping and nothing hanging in the air. The
 * waste line is deliberately grubbier than the rest of the mod: soot-grey drums and open hoppers, so a
 * player can tell the recycling yard from the ore line at a glance.
 */
public final class CycleModels{
    private CycleModels(){}

    static final Color lumen = Color.valueOf("8fe9ff"), prism = Color.valueOf("b69cff"),
        slurry = Color.valueOf("7fd9e8"), soot = Color.valueOf("6b6a74"), ember = Color.valueOf("ff9e6b");

    // =====================================================================================================
    // the waste chain
    // =====================================================================================================

    /** Waste reclaimer (3x3): an open shredder trough with a toothed drum turning across it. */
    public static class WasteReclaimer extends KitModel{
        final Mesh drum = roller(soot, Color.valueOf("3a3a42"), 2.5f, 8.4f, 12);

        public WasteReclaimer(){
            super("waste-reclaimer", 3, aurelia);
        }

        @Override
        public void buildStatic(Mesh m){
            float z = deck(m, s, half, deckZ);
            //trough walls, north and south of the drum, leaving the middle clear for the roller
            for(int sy = -1; sy <= 1; sy += 2){
                hull2(m, s).at(0, 0, 0).box(-5.4f, sy * 5.2f - 1.6f, z, 5.4f, sy * 5.2f + 1.6f, z + 4.4f);
                dark(m, s).box(-5.4f, sy * 5.2f - 1.7f, z + 4.4f, 5.4f, sy * 5.2f + 1.7f, z + 4.7f);
            }
            //intake hopper on the west end, above the trough floor
            hull(m, s).at(0, 0, 0).taper(-8.6f, 0f, z + 1.2f, 2.0f, 3.4f, z + 5.6f, 3.0f, 4.6f, 0, 0, true);
            dark(m, s).at(-8.6f, 0f, 0).ring(4, 3.0f, 3.5f, z + 5.6f, z + 5.9f);
            m.at(0, 0, 0);
            //discharge chute on the east end, lower than the hopper so the flow reads left to right
            steel(m, s).at(0, 0, 0).box(5.6f, -2.4f, z + 0.6f, 9.6f, 2.4f, z + 2.6f);
            glow(m, s).box(9.6f, -1.6f, z + 0.9f, 9.75f, 1.6f, z + 2.3f);
            //bearing blocks the drum sits in
            for(int sy = -1; sy <= 1; sy += 2){
                steel(m, s).at(0, 0, 0).box(-1.6f, sy * 4.6f - 0.9f, z + 2.2f, 1.6f, sy * 4.6f + 0.9f, z + 4.0f);
            }
            console(m, s, -8.4f, -8.4f, z);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-3f, -4.6f, deckZ + 1.2f, 3f, 4.6f, deckZ + 4.4f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(drum, 0f, 0f, deckZ + 3.1f, 1, 0f);
        }

        @Override
        public void drawLive(Building b){
            //one part, turning about the north-south axis, inside the trough
            draw(drum, b, 0f, 0f, deckZ + 3.1f, 1, total(b) * 2.4f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 9.0f, 0f, deckZ + 1.8f, 2.4f, soot, 0.12f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Swarf furnace (2x2): a squat melt pot with a tilting lid ring. */
    public static class SwarfFurnace extends KitModel{
        final Mesh lid = new Mesh();

        public SwarfFurnace(){
            super("swarf-furnace", 2, aurelia);
            dark(lid, s).at(0, 0, 0).ring(12, 1.4f, 3.1f, 0f, 0.5f);
            trim(lid, s).at(0, 0, 0).ring(12, 3.1f, 3.4f, -0.1f, 0.6f);
        }

        @Override
        public void buildStatic(Mesh m){
            float z = deck(m, s, half, deckZ);
            //pot: closed lathe, widest at the belt line
            hull(m, s).at(0, 0, z).lathe(14, 12.8f, 0.001f, 0f, 3.4f, 0.4f, 4.0f, 2.2f, 3.6f, 4.4f, 0.001f, 4.6f);
            steel(m, s).at(0, 0, 0).ring(14, 4.0f, 4.4f, z + 1.6f, z + 2.4f);
            //feed chute from the south-east corner, resting on the deck
            hull2(m, s).at(0, 0, 0).box(2.6f, -5.6f, z, 4.2f, -2.8f, z + 3.4f);
            conduit(m, s, 3.4f, -2.8f, z + 2.9f, 1.4f, -1.4f, z + 3.6f, 0.4f);
            //tap spout to the west, pointing off the pot
            steel(m, s).at(0, 0, 0).box(-5.4f, -0.8f, z + 1.1f, -3.2f, 0.8f, z + 2.1f);
            glow(m, s).box(-5.55f, -0.6f, z + 1.3f, -5.4f, 0.6f, z + 1.9f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).cyl(10, 3.4f, deckZ + 4.6f, deckZ + 5.2f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(lid, 0f, 0f, deckZ + 4.6f, 2, 0f);
        }

        @Override
        public void drawLive(Building b){
            draw(lid, b, 0f, 0f, deckZ + 4.6f, 2, total(b) * 0.9f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0f, 0f, deckZ + 5.0f, 3.0f, ember, 0.10f + warm(b) * (0.08f + Mathf.absin(time(b), 22f, 0.04f)));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Leach tower (3x3): a tall wash column with a slow spray ring around its waist. */
    public static class LeachTower extends KitModel{
        final Mesh sprayer = new Mesh();

        public LeachTower(){
            super("leach-tower", 3, aurelia);
            steel(sprayer, s).at(0, 0, 0).ring(12, 3.4f, 3.9f, 0f, 0.5f);
            for(int i = 0; i < 6; i++){
                glow(sprayer, s).at(0, 0, 0).rot(2, i * 60f).box(-0.2f, 3.9f, 0.05f, 0.2f, 4.6f, 0.45f);
            }
            sprayer.at(0, 0, 0).rot(2, 0f);
        }

        @Override
        public void buildStatic(Mesh m){
            float z = deck(m, s, half, deckZ);
            //column: closed at both ends
            hull(m, s).at(0, 0, z).lathe(14, 12.8f, 0.001f, 0f, 4.4f, 0.6f, 4.4f, 9.2f, 3.2f, 10.4f, 0.001f, 10.8f);
            dark(m, s).at(0, 0, 0).ring(14, 4.4f, 4.8f, z + 0.6f, z + 1.4f);
            trim(m, s).at(0, 0, 0).ring(14, 4.4f, 4.7f, z + 8.2f, z + 8.8f);
            //dust bin on the south side
            hull2(m, s).at(0, 0, 0).cbevel(0f, -7.6f, z, 4.2f, 2.6f, 3.0f, 0.3f);
            conduit(m, s, 0f, -6.4f, z + 2.6f, 0f, -4.0f, z + 4.4f, 0.45f);
            //water line in from the east
            steel(m, s).at(0, 0, 0).box(4.6f, -1.0f, z + 5.4f, 9.2f, 1.0f, z + 6.4f);
            dark(m, s).box(9.2f, -1.3f, z + 5.1f, 9.9f, 1.3f, z + 6.7f);
            //slurry outlet to the west, low
            steel(m, s).at(0, 0, 0).box(-9.2f, -1.2f, z + 1.0f, -4.4f, 1.2f, z + 2.2f);
            glow(m, s).box(-9.35f, -0.9f, z + 1.2f, -9.2f, 0.9f, z + 2.0f);
            bolts(m, s, 0, 0, z + 0.6f, 4.0f, 8);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).cyl(12, 4.6f, deckZ + 4.0f, deckZ + 5.0f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(sprayer, 0f, 0f, deckZ + 4.2f, 2, 0f);
        }

        @Override
        public void drawLive(Building b){
            draw(sprayer, b, 0f, 0f, deckZ + 4.2f, 2, total(b) * 0.7f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0f, 0f, deckZ + 10.8f, 3.0f, slurry, 0.14f + warm(b) * 0.06f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Slurry crystalliser (2x2): a glass growth jar with a single crystal turning inside it. */
    public static class SlurryCrystalliser extends KitModel{
        final Mesh seed = gem(slurry, true, 1.5f, 4.2f);

        public SlurryCrystalliser(){
            super("slurry-crystalliser", 2, aurelia);
        }

        @Override
        public void buildStatic(Mesh m){
            float z = deck(m, s, half, deckZ);
            //base collar and the jar wall - a tube, so the crystal inside stays visible
            dark(m, s).at(0, 0, z).ring(12, 2.4f, 3.6f, 0f, 0.8f);
            glass(m, Color.valueOf("2b4356")).at(0, 0, 0).tubeSide(12, 3.2f, z + 0.8f, z + 5.4f);
            steel(m, s).at(0, 0, 0).ring(12, 3.2f, 3.6f, z + 5.4f, z + 6.0f);
            hull(m, s).at(0, 0, z).lathe(12, 15f, 0.001f, 6.0f, 3.2f, 6.0f, 2.4f, 6.8f, 0.001f, 7.0f);
            //slurry feed from the north-west, on the deck
            hull2(m, s).at(0, 0, 0).box(-5.6f, 2.4f, z, -2.8f, 5.6f, z + 2.4f);
            conduit(m, s, -4.2f, 3.0f, z + 1.8f, -1.6f, 1.2f, z + 1.2f, 0.4f);
            //output chute south-east
            steel(m, s).at(0, 0, 0).box(2.4f, -5.6f, z + 0.5f, 4.4f, -2.6f, z + 1.9f);
            glow(m, s).box(2.6f, -5.75f, z + 0.8f, 4.2f, -5.6f, z + 1.6f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).cyl(10, 1.6f, deckZ + 1.2f, deckZ + 5.2f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(seed, 0f, 0f, deckZ + 1.2f, 2, 0f);
        }

        @Override
        public void drawLive(Building b){
            draw(seed, b, 0f, 0f, deckZ + 1.2f, 2, total(b) * 0.55f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0f, 0f, deckZ + 3.4f, 2.6f, slurry, 0.12f + prog(b) * 0.1f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Resonance resynthesiser (3x3): three reagent drums feeding a middle socket with a turning collar. */
    public static class Resynthesiser extends KitModel{
        final Mesh collar = new Mesh();

        public Resynthesiser(){
            super("resonance-resynthesiser", 3, aurelia);
            steel(collar, s).at(0, 0, 0).ring(12, 2.6f, 3.2f, 0f, 0.7f);
            for(int i = 0; i < 3; i++){
                trim(collar, s).at(0, 0, 0).rot(2, i * 120f).box(-0.5f, 3.2f, 0f, 0.5f, 4.2f, 0.6f);
            }
            collar.at(0, 0, 0).rot(2, 0f);
        }

        @Override
        public void buildStatic(Mesh m){
            float z = deck(m, s, half, deckZ);
            //three drums on a 120 degree spread, each clear of the next
            for(int i = 0; i < 3; i++){
                float a = 90f + i * 120f;
                tank(m, s, Mathf.cosDeg(a) * 7.4f, Mathf.sinDeg(a) * 7.4f, z, 2.5f, 3.6f, i == 0);
                conduit(m, s, Mathf.cosDeg(a) * 5.4f, Mathf.sinDeg(a) * 5.4f, z + 2.6f,
                    Mathf.cosDeg(a) * 2.8f, Mathf.sinDeg(a) * 2.8f, z + 3.4f, 0.4f);
            }
            //the socket in the middle: a closed cone with a dark throat
            hull(m, s).at(0, 0, z).lathe(12, 15f, 0.001f, 0f, 4.2f, 0.5f, 3.4f, 3.2f, 2.6f, 4.6f, 0.001f, 5.0f);
            dark(m, s).at(0, 0, 0).ring(12, 1.6f, 2.6f, z + 4.6f, z + 4.9f);
            hazard(m).at(0, 0, 0).ring(16, 9.6f, 10.4f, z, z + 0.05f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).cyl(12, 4.2f, deckZ + 5.0f, deckZ + 6.0f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(collar, 0f, 0f, deckZ + 5.0f, 2, 0f);
        }

        @Override
        public void drawLive(Building b){
            draw(collar, b, 0f, 0f, deckZ + 5.0f, 2, -total(b) * 0.8f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0f, 0f, deckZ + 5.4f, 3.4f, prism, 0.12f + prog(b) * 0.12f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Prism reformer (3x3): a mirrored casting bed with a gantry head running along it. */
    public static class PrismReformer extends KitModel{
        final Mesh head = new Mesh();

        public PrismReformer(){
            super("prism-reformer", 3, aurelia);
            hull2(head, s).at(0, 0, 0).cbevel(0f, 0f, 0f, 1.8f, 2.4f, 1.6f, 0.25f);
            glow(head, s).at(0, 0, 0).box(-1.1f, -1.6f, -0.35f, 1.1f, 1.6f, 0f);
        }

        @Override
        public void buildStatic(Mesh m){
            float z = deck(m, s, half, deckZ);
            //the bed: a shallow pan down the middle
            dark(m, s).at(0, 0, 0).box(-8.2f, -3.6f, z, 8.2f, 3.6f, z + 0.5f);
            hull(m, s).at(0, 0, 0).box(-8.6f, -4.4f, z, -8.2f, 4.4f, z + 1.8f);
            hull(m, s).at(0, 0, 0).box(8.2f, -4.4f, z, 8.6f, 4.4f, z + 1.8f);
            glass(m, Color.valueOf("31425e")).at(0, 0, 0).box(-8.2f, -3.6f, z + 0.5f, 8.2f, 3.6f, z + 0.65f);
            //the two rails the gantry rides, raised on legs at the ends only
            for(int sy = -1; sy <= 1; sy += 2){
                steel(m, s).at(0, 0, 0).box(-8.6f, sy * 5.0f - 0.5f, z + 3.2f, 8.6f, sy * 5.0f + 0.5f, z + 3.8f);
                for(int sxx = -1; sxx <= 1; sxx += 2){
                    hull2(m, s).at(0, 0, 0).box(sxx * 8.0f - 0.8f, sy * 5.0f - 0.8f, z, sxx * 8.0f + 0.8f, sy * 5.0f + 0.8f, z + 3.2f);
                }
            }
            //hopper for glass on the north edge, behind the rail
            hull2(m, s).at(0, 0, 0).cbevel(-4.6f, 7.8f, z, 2.6f, 1.8f, 3.0f, 0.3f);
            //slurry drum on the south edge
            tank(m, s, 5.2f, -7.8f, z, 2.0f, 3.0f, true);
            console(m, s, 8.4f, 8.4f, z);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-2f, -2.6f, deckZ + 2.6f, 2f, 2.6f, deckZ + 5.4f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(head, 0f, 0f, deckZ + 4.0f, 2, 0f);
        }

        @Override
        public void drawLive(Building b){
            //the one moving part slides along its rails and never leaves them
            float t = Mathf.sin(total(b) * 0.04f) * 7.0f;
            draw(head, b, t, 0f, deckZ + 4.0f, 2, 0f);
        }

        @Override
        public void drawOver(Building b){
            float t = Mathf.sin(total(b) * 0.04f) * 7.0f;
            light(b, t, 0f, deckZ + 3.2f, 2.6f, prism, 0.12f + prog(b) * 0.1f);
        }

        @Override
        public void drawCheap(Building b){
            light(b, 0f, 0f, deckZ + 3.2f, 2.6f, prism, 0.12f);
        }
    }

    /** Waste silo (3x3): one fat soot-grey drum with a level window and a capped top vent. */
    public static class WasteSilo extends KitModel{
        final Mesh vane = new Mesh();

        public WasteSilo(){
            super("waste-silo", 3, aurelia);
            dark(vane, s).at(0, 0, 0).ring(10, 0.6f, 2.2f, 0f, 0.35f);
            trim(vane, s).at(0, 0, 0).box(-0.3f, -2.2f, 0.35f, 0.3f, 2.2f, 0.6f);
        }

        @Override
        public void buildStatic(Mesh m){
            float z = deck(m, s, half, deckZ);
            //drum, closed top and bottom
            hull(m, s).at(0, 0, z).lathe(16, 11.25f, 0.001f, 0f, 8.4f, 0.8f, 8.4f, 7.6f, 6.4f, 9.0f, 0.001f, 9.4f);
            dark(m, s).at(0, 0, 0).ring(16, 8.4f, 8.9f, z + 0.8f, z + 1.6f);
            trim(m, s).at(0, 0, 0).ring(16, 8.4f, 8.7f, z + 4.4f, z + 5.0f);
            //level window: a tall glass strip on the south face, flush with the wall
            glass(m, Color.valueOf("2b2b33")).at(0, 0, 0).box(-1.2f, -8.9f, z + 1.8f, 1.2f, -8.3f, z + 7.0f);
            //loading chute north, standing on the deck
            hull2(m, s).at(0, 0, 0).box(-2.0f, 7.6f, z, 2.0f, 10.2f, z + 4.4f);
            conduit(m, s, 0f, 7.8f, z + 4.0f, 0f, 5.6f, z + 6.4f, 0.5f);
            //vent stack cap support
            steel(m, s).at(0, 0, 0).ring(10, 1.4f, 2.0f, z + 9.4f, z + 10.0f);
            bolts(m, s, 0, 0, z + 0.8f, 7.8f, 10);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).cyl(10, 2.3f, deckZ + 10.0f, deckZ + 10.8f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(vane, 0f, 0f, deckZ + 10.0f, 2, 0f);
        }

        @Override
        public void drawLive(Building b){
            //the extractor fan on the cap, turning slowly whenever the silo holds anything
            float f = b.items != null && b.items.total() > 0 ? 1f : 0.25f;
            draw(vane, b, 0f, 0f, deckZ + 10.0f, 2, time(b) * 0.5f * f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0f, -8.6f, deckZ + 4.4f, 2.6f, soot, 0.10f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    // =====================================================================================================
    // ward dome
    // =====================================================================================================

    /**
     * Ward dome (3x3): a tripod emitter. Three legs carry a ring, the ring carries a lens that tips with the
     * charge. The dome itself is drawn in 2D by the block - this is only the machine that makes it.
     */
    public static class WardDome extends KitModel{
        final Mesh ring = new Mesh(), lens = new Mesh();

        public WardDome(){
            super("ward-dome", 3, aurelia);
            //the emitter ring: one flat torus about the vertical axis
            torus(ring, lumen, Mesh.emissive, 0, 0, 0, 3.4f, 0.34f, 0.34f, 16);
            steel(ring, s).at(0, 0, 0).ring(16, 3.8f, 4.3f, -0.25f, 0.25f);
            //the lens: a shallow double cone, closed, turning with the ring
            lens.color(Color.valueOf("bfe6ff")).style(Mesh.glass | metal, matGlass).at(0, 0, 0)
                .lathe(12, 15f, 0.001f, -1.2f, 2.0f, -0.4f, 2.4f, 0f, 2.0f, 0.4f, 0.001f, 1.2f);
        }

        @Override
        public void buildStatic(Mesh m){
            float z = deck(m, s, half, deckZ);
            //plinth
            hull(m, s).at(0, 0, z).lathe(12, 15f, 0.001f, 0f, 6.4f, 0f, 6.4f, 1.4f, 4.6f, 2.6f, 0.001f, 2.8f);
            dark(m, s).at(0, 0, 0).ring(12, 4.6f, 5.4f, z + 1.4f, z + 2.0f);
            //three legs, each on its own third of the plinth, leaning in to the ring
            for(int i = 0; i < 3; i++){
                float a = 90f + i * 120f;
                float x0 = Mathf.cosDeg(a) * 4.4f, y0 = Mathf.sinDeg(a) * 4.4f;
                float x1 = Mathf.cosDeg(a) * 2.4f, y1 = Mathf.sinDeg(a) * 2.4f;
                conduit(m, s, x0, y0, z + 2.6f, x1, y1, z + 7.4f, 0.62f);
                hull2(m, s).at(0, 0, 0).rot(2, a).box(3.6f, -1.2f, z + 2.0f, 5.4f, 1.2f, z + 3.0f);
                m.at(0, 0, 0).rot(2, 0f);
            }
            //collar the ring sits on
            steel(m, s).at(0, 0, 0).ring(12, 1.9f, 2.6f, z + 7.4f, z + 8.2f);
            //two low cable trunks to the deck edge, west and east
            for(int sxx = -1; sxx <= 1; sxx += 2){
                steel(m, s).at(0, 0, 0).box(sxx * 6.4f - 1.0f, -1.0f, z, sxx * 6.4f + 1.0f, 1.0f, z + 1.2f);
            }
            hazard(m).at(0, 0, 0).ring(18, 9.8f, 10.4f, z, z + 0.05f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).cyl(12, 4.4f, deckZ + 8.2f, deckZ + 9.6f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(ring, 0f, 0f, deckZ + 8.6f, 2, 0f);
            m.add(lens, 0f, 0f, deckZ + 8.6f, 2, 0f);
        }

        @Override
        public void drawLive(Building b){
            float t = time(b);
            draw(ring, b, 0f, 0f, deckZ + 8.6f, 2, t * 0.35f);
            draw(lens, b, 0f, 0f, deckZ + 8.6f, 2, -t * 0.5f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0f, 0f, deckZ + 9.0f, 4.0f, lumen, 0.12f + warm(b) * 0.08f);
        }

        @Override
        public void drawCheap(Building b){
            light(b, 0f, 0f, deckZ + 9.0f, 4.0f, lumen, 0.12f);
        }
    }

    // =====================================================================================================
    // tier-3 core
    // =====================================================================================================

    /**
     * Aurelia citadel (6x6): the last core. A ringed landing table with six bays, a stepped keep in the
     * middle and a slow beacon over it - bigger than the bastion but built from the same shapes, so the
     * three cores read as one family.
     */
    public static class CitadelCore extends KitModel{
        final Mesh beacon = new Mesh(), halo = new Mesh();

        public CitadelCore(){
            super("aurelia-citadel", 6, aurelia);
            beacon.color(lumen).style(emissive, matPlain).at(0, 0, 0).lathe(12, 15f,
                0.001f, -3.6f, 2.6f, -2.0f, 3.2f, 0f, 2.6f, 2.0f, 0.001f, 3.6f);
            halo.color(prism).style(emissive, matPlain).at(0, 0, 0).ring(20, 6.4f, 7.0f, -0.2f, 0.2f);
            halo.color(s.metal).style(metal, matPlain).at(0, 0, 0).ring(20, 7.0f, 7.5f, -0.32f, 0.32f);
        }

        @Override
        public void buildStatic(Mesh m){
            float z = deck(m, s, half, deckZ);
            //stepped table, inside the 6x6 pad
            hull(m, s).at(0, 0, z).lathe(18, 10f, 0.001f, 0f, 20.4f, 0f, 20.4f, 1.8f, 17.6f, 3.2f, 17.6f, 4.2f, 13.0f, 5.6f, 0.001f, 5.6f);
            trim(m, s).at(0, 0, 0).ring(18, 17.2f, 18.4f, z + 3.0f, z + 3.7f);
            dark(m, s).at(0, 0, 0).ring(18, 12.6f, 13.4f, z + 5.3f, z + 5.9f);
            //six landing bays around the rim, one per sextant, each with a team panel
            for(int i = 0; i < 6; i++){
                float a = i * 60f;
                float px = Mathf.cosDeg(a) * 15.2f, py = Mathf.sinDeg(a) * 15.2f;
                steel(m, s).at(0, 0, 0).rot(2, a).box(px - 2.8f, py - 2.8f, z + 4.2f, px + 2.8f, py + 2.8f, z + 5.6f);
                team(m).at(0, 0, 0).rot(2, a).box(px - 2.0f, py - 2.0f, z + 5.6f, px + 2.0f, py + 2.0f, z + 5.8f);
            }
            m.at(0, 0, 0).rot(2, 0f);
            //the keep: a closed drum stepping up to the beacon collar
            hull2(m, s).at(0, 0, z).lathe(16, 11.25f, 0.001f, 5.6f, 9.4f, 5.6f, 9.4f, 9.4f, 7.0f, 11.0f, 4.2f, 11.8f, 0.001f, 11.8f);
            glass(m, Color.valueOf("22314a")).at(0, 0, 0).ring(16, 9.3f, 9.6f, z + 6.6f, z + 8.8f);
            steel(m, s).at(0, 0, 0).ring(16, 7.0f, 7.6f, z + 10.8f, z + 11.4f);
            //four service masts on the diagonals, standing on the table, clear of the bays
            for(int i = 0; i < 4; i++){
                float a = 30f + i * 90f;
                pylon(m, s, Mathf.cosDeg(a) * 11.6f, Mathf.sinDeg(a) * 11.6f, z + 5.6f, 1.5f, 5.4f, (i & 1) == 0);
            }
            hazard(m).at(0, 0, 0).ring(20, 18.8f, 20.4f, z, z + 0.05f);
            bolts(m, s, 0, 0, z + 5.6f, 10.2f, 12);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ + 15.4f).cyl(12, 7.6f, -4.0f, 4.2f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(beacon, 0, 0, deckZ + 15.2f, 2, 0);
            m.add(halo, 0, 0, deckZ + 15.2f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float t = time(b);
            draw(beacon, b, 0, 0, deckZ + 15.2f, 2, t * 0.45f);
            draw(halo, b, 0, 0, deckZ + 15.2f, 2, -t * 0.7f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 15.6f, 18f, lumen, 0.18f + Mathf.absin(time(b), 34f, 0.06f));
        }
    }
}
