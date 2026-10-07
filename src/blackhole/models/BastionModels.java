package blackhole.models;

import arc.graphics.*;
import arc.math.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import mindustry.gen.*;

import static blackhole.g3d.Kit.*;
import static blackhole.g3d.Mesh.*;

/**
 * v7.7 bastion models: the ten defence walls, the graviton string dynamo and the bastion core.
 *
 * <p>The walls share one parametric body ({@link WallModel}) so a mixed line still reads as one wall family:
 * the same plinth, the same chamfer, the same height. What changes per wall is the <b>crown</b> - the piece
 * on top that tells you at a glance which mechanism you are looking at - and the material colour. House rules
 * as always: nothing leaves its own tile, nothing overlaps its neighbour, two moving groups at most.
 */
public final class BastionModels{
    private BastionModels(){}

    public static final Color graphiteGrey = Color.valueOf("4a4f58"), siltGrey = Color.valueOf("9c93ab"),
        prismBlue = Color.valueOf("bfe6ff"), concordGold = Color.valueOf("ffd875"),
        lumen = Color.valueOf("8fe9ff"), graviton = Color.valueOf("9a7cff"), frost = Color.valueOf("a9f0ff");

    /** Which crown sits on the wall - this is the readable difference between the ten walls. */
    public enum Crown{
        /** plain capstone (basic masonry) */ flat,
        /** stacked brick courses (graphite) */ brick,
        /** faceted mirror prism (deflector) */ prism,
        /** a floating emitter plate (shield) */ emitter,
        /** a slow turning ring (stasis) */ ring,
        /** a charged coil (echo) */ coil,
        /** a frosted tank (coolant) */ tank,
        /** a sunken funnel (anchor) */ funnel,
        /** a lattice mast (amplifier) */ mast,
        /** an open collector cage (absorber) */ cage
    }

    // =================================================================================================
    // walls
    // =================================================================================================

    public static class WallModel extends KitModel{
        public final Crown crown;
        public final Color tone, accent;
        /** scale factor: 1 for a 1x1 wall, 2 for a 2x2 one, so both read as the same design */
        public final float u;
        final Mesh moving = new Mesh();
        final boolean spins;

        public WallModel(String name, int size, Crown crown, Color tone, Color accent){
            super(name, size, aurelia);
            this.crown = crown;
            this.tone = tone;
            this.accent = accent;
            this.u = half / 4f;
            //v7.8: walls are completely static. A wall is the block you build by the hundred, so it
            //must be the cheapest thing on screen: no live pass, no moving piece, nothing to detach.
            this.spins = false;
            buildMoving();
        }

        void buildMoving(){
            float r = half * 0.55f;
            switch(crown){
                case ring -> {
                    moving.color(accent).style(emissive, matPlain).at(0, 0, 0).ring(10, r * 0.42f, r * 0.62f, -0.1f * u, 0.1f * u);
                    moving.color(s.metal).style(metal, matPlain).at(0, 0, 0).ring(10, r * 0.62f, r * 0.72f, -0.16f * u, 0.16f * u);
                    for(int i = 0; i < 3; i++){
                        moving.color(s.metal).style(metal, matPlain).at(0, 0, 0).rot(2, i * 120f)
                            .box(-0.1f * u, 0f, -0.07f * u, 0.1f * u, r * 0.66f, 0.07f * u);
                    }
                    moving.rot(2, 0f);
                }
                case coil -> {
                    for(int i = 0; i < 3; i++){
                        moving.color(accent).style(emissive, matPlain).at(0, 0, (i * 0.42f - 0.42f) * u)
                            .ring(8, r * (0.3f + i * 0.06f), r * (0.42f + i * 0.06f), -0.08f * u, 0.08f * u);
                    }
                    moving.at(0, 0, 0);
                }
                case cage -> {
                    for(int i = 0; i < 4; i++){
                        moving.color(s.metal).style(metal, matPlain).at(0, 0, 0).rot(2, i * 90f)
                            .box(-0.1f * u, r * 0.42f, -0.55f * u, 0.1f * u, r * 0.56f, 0.55f * u);
                    }
                    moving.rot(2, 0f).color(accent).style(emissive, matPlain).at(0, 0, 0)
                        .ring(10, r * 0.4f, r * 0.56f, -0.08f * u, 0.08f * u);
                }
                default -> {
                }
            }
        }

        @Override
        public void buildStatic(Mesh m){
            float h = half;
            //shared body, identical on every wall in the family: dark plinth, chamfered block, dark cap deck.
            //every crown below is sized off the cap (r = h * 0.55 at most), so nothing leaves its own tile.
            m.color(s.dark).style(metal, matPlain).at(0, 0, 0)
                .box(-h + 0.15f * u, -h + 0.15f * u, 0f, h - 0.15f * u, h - 0.15f * u, 0.45f * u);
            m.color(tone).style(metal, matPlate).at(0, 0, 0)
                .taper(0, 0, 0.45f * u, (h - 0.3f * u) * 2f, (h - 0.3f * u) * 2f, 2.5f * u, (h - 1.3f * u) * 2f, (h - 1.3f * u) * 2f, 0, 0, true);
            m.color(s.dark).style(metal, matPlain).at(0, 0, 0)
                .box(-h + 1.25f * u, -h + 1.25f * u, 2.5f * u, h - 1.25f * u, h - 1.25f * u, 2.75f * u);
            //v7.8: one plain joint line instead of four corner posts - fewer faces, cleaner row
            m.color(s.metal).style(metal, matPlain).at(0, 0, 0)
                .box(-h + 0.4f * u, -h + 0.4f * u, 2.3f * u, h - 0.4f * u, h - 0.4f * u, 2.5f * u);
            float z = 2.75f * u, r = h * 0.55f;
            switch(crown){
                case flat -> {
                    m.color(s.trim).style(metal, matPlain).at(0, 0, 0).box(-r, -r, z, r, r, z + 0.35f * u);
                    m.color(accent).style(emissive, matPlain).at(0, 0, 0)
                        .box(-r * 0.55f, -r * 0.55f, z + 0.35f * u, r * 0.55f, r * 0.55f, z + 0.42f * u);
                }
                case brick -> {
                    //three stacked courses, each a little smaller and turned: unmistakably masonry
                    for(int i = 0; i < 3; i++){
                        float w = r * (1f - i * 0.18f);
                        m.color(i % 2 == 0 ? tone : accent).style(metal, matPlain).at(0, 0, 0).rot(2, i * 12f - 12f)
                            .box(-w, -w * 0.62f, z + i * 0.38f * u, w, w * 0.62f, z + (0.34f + i * 0.38f) * u);
                    }
                    m.rot(2, 0f);
                }
                case prism -> {
                    m.color(s.trim).style(metal, matPlain).at(0, 0, 0).ring(6, r * 0.9f, r, z, z + 0.25f * u);
                    m.color(accent).style(Mesh.glass | metal, matGlass).at(0, 0, z + 0.2f)
                        .lathe(6, 30f, 0.001f, 0f, r * 0.85f, 0.3f * u, r * 0.6f, 1.1f * u, 0.001f, 1.5f * u);
                }
                case emitter -> {
                    m.color(s.metal).style(metal, matPlain).at(0, 0, 0).cyl(8, r * 0.26f, z, z + 1.0f * u);
                    m.color(accent).style(emissive, matPlain).at(0, 0, 0).ring(8, r * 0.5f, r * 0.9f, z + 1.0f * u, z + 1.2f * u);
                    m.color(s.trim).style(metal, matPlain).at(0, 0, 0).ring(8, r * 0.9f, r, z, z + 0.3f * u);
                }
                case ring -> {
                    m.color(s.metal).style(metal, matPlain).at(0, 0, 0).cyl(6, r * 0.3f, z, z + 0.8f * u);
                    m.color(s.trim).style(metal, matPlain).at(0, 0, 0).ring(6, r * 0.88f, r, z, z + 0.25f * u);
                    m.color(accent).style(emissive, matPlain).at(0, 0, 0).ring(8, r * 0.45f, r * 0.65f, z + 0.72f * u, z + 0.9f * u);
                }
                case coil -> {
                    m.color(s.dark).style(metal, matPlain).at(0, 0, 0).cyl(6, r * 0.26f, z, z + 1.5f * u);
                    m.color(s.trim).style(metal, matPlain).at(0, 0, 0).ring(6, r * 0.88f, r, z, z + 0.22f * u);
                    for(int i = 0; i < 2; i++){
                        m.color(accent).style(emissive, matPlain).at(0, 0, 0)
                            .ring(6, r * 0.32f, r * 0.46f, z + (0.5f + i * 0.5f) * u, z + (0.66f + i * 0.5f) * u);
                    }
                }
                case tank -> {
                    m.color(s.hull2).style(metal, matPlate).at(0, 0, z)
                        .lathe(8, 18f, 0.001f, 0f, r * 0.78f, 0.2f * u, r * 0.78f, 1.1f * u, 0.001f, 1.45f * u);
                    m.color(accent).style(emissive, matPlain).at(0, 0, 0)
                        .ring(8, r * 0.78f, r * 0.86f, z + 0.45f * u, z + 0.85f * u);
                    m.color(s.trim).style(metal, matPlain).at(0, 0, 0).ring(8, r * 0.9f, r, z, z + 0.25f * u);
                }
                case funnel -> {
                    m.color(s.dark).style(metal, matPlain).at(0, 0, z)
                        .lathe(8, 18f, r, 0f, r * 0.82f, 0.5f * u, r * 0.26f, 1.1f * u, 0.001f, 1.15f * u);
                    m.color(accent).style(emissive, matPlain).at(0, 0, z + 0.15f * u).ring(8, r * 0.22f, r * 0.5f, 0f, 0.1f * u);
                }
                case mast -> {
                    m.color(s.metal).style(metal, matPlain).at(0, 0, 0).cyl(6, r * 0.2f, z, z + 2.0f * u);
                    m.color(accent).style(emissive, matPlain).at(0, 0, 0).ring(6, r * 0.3f, r * 0.52f, z + 1.45f * u, z + 1.7f * u);
                    m.color(s.trim).style(metal, matPlain).at(0, 0, 0).ring(6, r * 0.88f, r, z, z + 0.3f * u);
                }
                case cage -> {
                    m.color(s.dark).style(metal, matPlain).at(0, 0, 0).cyl(6, r * 0.24f, z, z + 0.9f * u);
                    m.color(s.trim).style(metal, matPlain).at(0, 0, 0).ring(6, r * 0.88f, r, z, z + 0.25f * u);
                    for(int i = 0; i < 4; i++){
                        m.color(s.metal).style(metal, matPlain).at(0, 0, 0).rot(2, i * 90f)
                            .box(-0.09f * u, r * 0.46f, z + 0.2f * u, 0.09f * u, r * 0.6f, z + 1.0f * u);
                    }
                    m.rot(2, 0f);
                    m.color(accent).style(emissive, matPlain).at(0, 0, 0).ring(6, r * 0.42f, r * 0.58f, z + 0.95f * u, z + 1.05f * u);
                }
            }
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-half + 0.1f * u, -half + 0.1f * u, 0f, half - 0.1f * u, half - 0.1f * u, 4.3f * u);
        }

        @Override
        public void buildRest(Mesh m){
            if(spins) m.add(moving, 0, 0, restZ(), 2, 0);
        }

        float restZ(){
            return u * switch(crown){
                case ring -> 3.75f;
                case coil -> 3.9f;
                case cage -> 3.85f;
                default -> 3.2f;
            };
        }

        @Override
        public void drawLive(Building b){
            //v7.8: nothing - the wall is drawn entirely from its baked layers
        }

        @Override
        public void drawOver(Building b){
            //v7.8: no per-wall light. A wall line is hundreds of blocks; one light each was the most
            //expensive thing the defence pack did per frame, and it bought almost nothing visually.
        }
    }

    // =================================================================================================
    // 引力弦发电机 - graviton string dynamo (5x5)
    // =================================================================================================

    /**
     * The new power plant: a caged mass on a vertical axis, wrapped by two counter-rotating gimbal rings and
     * tapped by four field pylons. Three moving groups at most (inner mass, inner ring, outer ring).
     */
    public static class GravitonDynamo extends KitModel{
        final Mesh inner = new Mesh(), outer = new Mesh(), mass = new Mesh();
        static final float axis = 9.0f;

        public GravitonDynamo(){
            super("graviton-string-dynamo", 5, aurelia);
            //inner gimbal: a slim ring that turns about y
            inner.color(s.metal).style(metal, matPlain).at(0, 0, 0).rot(0, 90f).ring(16, 5.6f, 6.3f, -0.36f, 0.36f);
            inner.color(graviton).style(emissive, matPlain).at(0, 0, 0).rot(0, 90f).ring(16, 5.2f, 5.6f, -0.16f, 0.16f);
            inner.at(0, 0, 0).rot(0, 0f);
            //outer gimbal: a heavier ring about x, turning the other way
            outer.color(s.hull2).style(metal, matPlate).at(0, 0, 0).rot(1, 90f).ring(18, 7.5f, 8.5f, -0.48f, 0.48f);
            outer.color(s.trim).style(metal, matPlain).at(0, 0, 0).rot(1, 90f).ring(18, 7.1f, 7.5f, -0.2f, 0.2f);
            outer.at(0, 0, 0).rot(1, 0f);
            //the suspended mass itself
            mass.color(graviton).style(emissive, matPlain).at(0, 0, 0).lathe(12, 15f,
                0.001f, -3.2f, 2.0f, -2.4f, 3.1f, 0f, 2.0f, 2.4f, 0.001f, 3.2f);
            mass.color(Color.valueOf("efe6ff")).style(Mesh.glass | metal, matGlass).at(0, 0, 0).ring(12, 3.1f, 3.4f, -0.4f, 0.4f);
        }

        @Override
        public void buildStatic(Mesh m){
            float z = deck(m, s, half, deckZ);
            //cradle: four buttresses holding the gimbal cage over a sunken well
            for(int i = 0; i < 4; i++){
                float a = 45f + i * 90f;
                float px = Mathf.cosDeg(a) * 15.5f, py = Mathf.sinDeg(a) * 15.5f;
                pylon(m, s, px, py, z, 2.0f, axis, i % 2 == 0);
                conduit(m, s, px * 0.7f, py * 0.7f, z + axis - 1.4f, 0f, 0f, z + axis, 0.55f);
            }
            dark(m, s).at(0, 0, 0).ring(16, 9.2f, 11.0f, z, z + 0.7f);
            hull(m, s).at(0, 0, z).lathe(16, 11.25f, 11.0f, 0f, 11.0f, 1.7f, 8.8f, 2.9f, 0.001f, 2.9f);
            glass(m, Color.valueOf("22314a")).at(0, 0, 0).ring(16, 8.9f, 9.1f, z + 1.1f, z + 2.6f);
            //a low hazard collar: this thing is dangerous and should look it
            hazard(m).at(0, 0, 0).ring(16, 11.4f, 12.6f, z, z + 0.05f);
            trim(m, s).at(0, 0, 0).ring(16, 11.0f, 11.4f, z, z + 1.1f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ + axis).cyl(14, 8.8f, -6.0f, 6.0f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(outer, 0, 0, deckZ + axis, 1, 0);
            m.add(inner, 0, 0, deckZ + axis, 0, 0);
            m.add(mass, 0, 0, deckZ + axis, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float w = warm(b), t = total(b);
            lit(0.55f + 0.45f * w);
            draw(outer, b, 0, 0, deckZ + axis, 1, t * 1.1f * (0.25f + w));
            draw(inner, b, 0, 0, deckZ + axis, 0, -t * 1.7f * (0.25f + w));
            //the mass bobs along the string as the field winds up
            draw(mass, b, 0, 0, deckZ + axis, 2, t * 2.4f * w);
            lit(1f);
        }

        @Override
        public void drawOver(Building b){
            float w = warm(b);
            light(b, 0, 0, deckZ + axis, 13f, graviton, w * (0.22f + Mathf.absin(time(b), 9f, 0.12f)));
        }
    }

    // =================================================================================================
    // 欧雷利亚堡垒核心 - bastion core (5x5)
    // =================================================================================================

    /** The upgraded core: the landing pad of the standard core, widened, with a ring of launch cradles. */
    public static class BastionCore extends KitModel{
        final Mesh heart = new Mesh(), halo = new Mesh();

        public BastionCore(){
            super("aurelia-bastion", 5, aurelia);
            heart.color(lumen).style(emissive, matPlain).at(0, 0, 0).lathe(10, 18f,
                0.001f, -3.0f, 2.9f, -1.7f, 3.6f, 0f, 2.9f, 1.7f, 0.001f, 3.0f);
            halo.color(Color.valueOf("b69cff")).style(emissive, matPlain).at(0, 0, 0).ring(18, 5.4f, 6.0f, -0.18f, 0.18f);
            halo.color(s.metal).style(metal, matPlain).at(0, 0, 0).ring(18, 6.0f, 6.4f, -0.3f, 0.3f);
        }

        @Override
        public void buildStatic(Mesh m){
            float z = deck(m, s, half, deckZ);
            //stepped landing platform, nearly the full 5x5 pad
            hull(m, s).at(0, 0, z).lathe(16, 11.25f, 16.6f, 0f, 16.6f, 1.6f, 14.2f, 2.8f, 14.2f, 3.6f, 10.4f, 4.8f, 0.001f, 4.8f);
            trim(m, s).at(0, 0, 0).ring(16, 14.0f, 14.9f, z + 2.7f, z + 3.3f);
            dark(m, s).at(0, 0, 0).ring(16, 10.0f, 10.8f, z + 4.5f, z + 5.0f);
            //four launch cradles on the diagonals, each with a team-coloured panel
            for(int i = 0; i < 4; i++){
                float a = 45f + i * 90f;
                float px = Mathf.cosDeg(a) * 12.0f, py = Mathf.sinDeg(a) * 12.0f;
                steel(m, s).at(0, 0, 0).rot(2, a).box(px - 2.6f, py - 2.6f, z + 4.8f, px + 2.6f, py + 2.6f, z + 6.2f);
                team(m).at(0, 0, 0).rot(2, a).box(px - 1.9f, py - 1.9f, z + 6.2f, px + 1.9f, py + 1.9f, z + 6.4f);
            }
            m.at(0, 0, 0).rot(2, 0f);
            //service masts north and south
            for(int sy = -1; sy <= 1; sy += 2){
                pylon(m, s, 0f, sy * 13.6f, z, 1.8f, 7.2f, sy > 0);
            }
            glass(m, Color.valueOf("22314a")).at(0, 0, 0).ring(16, 10.4f, 10.6f, z + 3.6f, z + 4.5f);
            hazard(m).at(0, 0, 0).ring(16, 15.2f, 16.6f, z, z + 0.05f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ + 9.2f).cyl(12, 6.6f, -3.6f, 4.2f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(heart, 0, 0, deckZ + 9.0f, 2, 0);
            m.add(halo, 0, 0, deckZ + 9.0f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float t = time(b);
            draw(heart, b, 0, 0, deckZ + 9.0f, 2, t * 0.5f);
            draw(halo, b, 0, 0, deckZ + 9.0f, 2, -t * 0.8f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 9.4f, 16f, lumen, 0.2f + Mathf.absin(time(b), 30f, 0.07f));
        }
    }
}
