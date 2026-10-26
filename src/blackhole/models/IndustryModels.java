package blackhole.models;

import arc.graphics.*;
import arc.math.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import mindustry.gen.*;

import static blackhole.g3d.Kit.*;
import static blackhole.g3d.Mesh.*;

/**
 * v7.5 Aurelia industry: three conventional mining rigs, the rift collector (directional, ground independent
 * mining), two generators and two unit repair stations.
 *
 * <p>Same rules as the rest of the kit: plain silver / pearl industrial shapes, one or two moving parts each,
 * every footprint inside its own area so nothing overlaps a neighbour, and nothing taller than the north
 * clearance line {@code z <= 1.186 * (half - y)} so the perspective bake stays inside the block.
 */
public final class IndustryModels{
    private IndustryModels(){}

    static final Color lumen = Color.valueOf("8fe9ff"), plasma = Color.valueOf("7ff0d8"), tide = Color.valueOf("3f7fc0"),
        prism = Color.valueOf("b69cff"), ember = Color.valueOf("ffb45a");

    // ------------------------------------------------------------------------------------------ drills

    /** Aurite auger (3x3): octagonal housing, four corner pylons, a heavy auger spindle turning in the middle. */
    public static class AuriteAuger extends KitModel{
        final Mesh auger = new Mesh(), collar = new Mesh();

        public AuriteAuger(){
            super("aurite-auger", 3, aurelia);
            //three stacked flights make a readable screw without a real helix
            for(int i = 0; i < 3; i++){
                steel(auger, s).at(0, 0, i * 1.5f).rot(2, i * 40f).hexa(true,
                    -0.5f, -2.6f, 0f, 0.5f, -2.6f, 0f, 0.5f, 2.6f, 0f, -0.5f, 2.6f, 0f,
                    -0.5f, -2.3f, 0.45f, 0.5f, -2.3f, 0.45f, 0.5f, 2.3f, 0.45f, -0.5f, 2.3f, 0.45f);
            }
            steel(auger, s).at(0, 0, 0).cyl(8, 0.75f, -0.6f, 4.9f);
            glow(auger, s).at(0, 0, 0).cyl(8, 0.45f, 4.8f, 5.3f);
            dark(collar, s).lathe(8, 22.5f, 2.3f, 0f, 2.3f, 0.7f, 1.7f, 1.0f, 1.7f, 0f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //corner pylons, inset so they never touch the deck rim
            for(int i = 0; i < 4; i++){
                float sx = (i & 1) == 0 ? -1 : 1, sy = (i & 2) == 0 ? -1 : 1;
                hull2(m, s).at(0, 0, 0).cbevel(sx * 8.2f, sy * 8.2f, z, 2.2f, 2.2f, 3.6f, 0.25f);
                trim(m, s).box(sx * 8.2f - 1.1f, sy * 8.2f - 1.1f, z + 3.5f, sx * 8.2f + 1.1f, sy * 8.2f + 1.1f, z + 3.7f);
            }
            //housing
            hull(m, s).at(0, 0, z).lathe(8, 22.5f, 5.4f, 0f, 5.4f, 3.6f, 4.8f, 4.4f, 4.8f, 6.4f, 0.001f, 6.4f);
            glass(m, Color.valueOf("38577c")).at(0, 0, z).ring(8, 4.85f, 4.95f, 1.2f, 2.6f);
            dark(m, s).at(0, 0, z + 6.4f).ring(8, 2.4f, 4.8f, 0f, 0.25f);
            glow(m, s).at(0, 0, z + 6.6f).ring(8, 2.4f, 2.8f, 0f, 0.1f);
            //spoil chute to the south, feed rail to the west: both low and clear of the housing
            hull2(m, s).at(0, 0, 0).taper(0, -8.4f, z, 2.6f, 1.6f, z + 1.9f, 2.0f, 1.2f, 0, 0, true);
            grate(m, s).box(-1.8f, -9.4f, z + 1.9f, 1.8f, -7.4f, z + 2.0f);
            conduit(m, s, -5.0f, 0f, z + 1.2f, -8.6f, 0f, z + 1.2f, 0.55f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ).cyl(12, 5.0f, 0f, 6.6f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(collar, 0, 0, deckZ + 6.4f, 2, 0);
            m.add(auger, 0, 0, deckZ + 1.4f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float w = warm(b), spin = total(b) * 5.5f;
            lit(0.55f + 0.45f * w);
            draw(auger, b, 0, 0, deckZ + 1.4f, 2, spin);
            lit(1f);
            draw(collar, b, 0, 0, deckZ + 6.4f, 2, -spin * 0.3f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 6.8f, 3.4f, lumen, warm(b) * (0.25f + Mathf.absin(time(b), 6f, 0.12f)));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Resonance sifter (2x2): a tilted vibrating screen over a tidewater trough. */
    public static class ResonanceSifter extends KitModel{
        final Mesh screen = new Mesh();

        public ResonanceSifter(){
            super("resonance-sifter", 2, aurelia);
            waterCx = 0;
            waterCy = -5.0f;
            grate(screen, s).rot(1, -12f).box(-3.0f, -2.0f, -0.14f, 3.0f, 2.0f, 0.14f);
            trim(screen, s).rot(1, -12f).box(-3.2f, -2.2f, -0.2f, -2.9f, 2.2f, 0.4f);
            trim(screen, s).rot(1, -12f).box(2.9f, -2.2f, -0.2f, 3.2f, 2.2f, 0.4f);
            screen.twoSidedAll();
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //shallow wash trough along the south edge
            dark(m, s).at(0, 0, 0).box(-5.4f, -6.8f, z, 5.4f, -3.6f, z + 1.1f);
            m.color(tide).style(glass, matWater).at(0, 0, 0).box(-5.0f, -6.4f, z + 0.9f, 5.0f, -4.0f, z + 1.12f);
            trim(m, s).box(-5.5f, -6.9f, z + 1.1f, 5.5f, -6.6f, z + 1.45f);
            //two low legs carry the screen deck over the northern half
            for(int sd = -1; sd <= 1; sd += 2) hull2(m, s).at(0, 0, 0).cbevel(sd * 3.6f, 1.0f, z, 1.4f, 4.4f, 2.2f, 0.18f);
            //feed chute at the north edge, kept low for the clearance line
            hull(m, s).at(0, 0, 0).taper(0, 5.6f, z, 4.0f, 2.2f, z + 2.4f, 2.6f, 1.4f, 0, 0, true);
            glow(m, s).box(-1.8f, 4.5f, z + 2.4f, 1.8f, 4.7f, z + 2.55f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-3.4f, -2.4f, deckZ + 2.0f, 3.4f, 2.4f, deckZ + 3.4f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(screen, 0, 1.0f, deckZ + 2.5f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float w = warm(b), sh = Mathf.sin(time(b), 3.2f, 0.3f) * w;
            draw(screen, b, 0, 1.0f + sh, deckZ + 2.5f - sh * 0.25f, 2, 0);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, -5.0f, deckZ + 1.3f, 2.4f, tide, warm(b) * 0.22f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Prism harvester (4x4): four legs, a toothed crown ring and a heavy cutter head under the tower. */
    public static class PrismHarvester extends KitModel{
        final Mesh crown = new Mesh(), cutter = new Mesh();

        public PrismHarvester(){
            super("prism-harvester", 4, aurelia);
            steel(crown, s).ring(12, 7.4f, 9.0f, 0f, 0.9f);
            for(int i = 0; i < 12; i++){
                float a = i * 30f;
                trim(crown, s).at(Mathf.cosDeg(a) * 9.2f, Mathf.sinDeg(a) * 9.2f, 0).rot(2, a).cbevel(0, 0, 0.1f, 0.9f, 1.5f, 0.9f, 0.15f);
            }
            dark(cutter, s).lathe(10, 0, 3.4f, 0f, 3.4f, 1.1f, 2.4f, 2.0f, 0.001f, 2.6f);
            for(int i = 0; i < 5; i++){
                float a = i * 72f;
                crystal(cutter, prism, true, Mathf.cosDeg(a) * 2.3f, Mathf.sinDeg(a) * 2.3f, 0.4f, 0.5f, 1.5f, 28f, a);
            }
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            for(int i = 0; i < 4; i++){
                float sx = (i & 1) == 0 ? -1 : 1, sy = (i & 2) == 0 ? -1 : 1;
                hull2(m, s).at(0, 0, 0).cbevel(sx * 11.4f, sy * 11.4f, z, 2.6f, 2.6f, 4.4f, 0.3f);
                dark(m, s).box(sx * 11.4f - 1.4f, sy * 11.4f - 1.4f, z + 4.3f, sx * 11.4f + 1.4f, sy * 11.4f + 1.4f, z + 4.6f);
                conduit(m, s, sx * 9.6f, sy * 9.6f, z + 2.4f, sx * 5.2f, sy * 5.2f, z + 2.4f, 0.5f);
            }
            //central tower
            hull(m, s).at(0, 0, z).lathe(8, 22.5f, 5.6f, 0f, 5.6f, 4.0f, 4.6f, 5.0f, 4.6f, 8.6f, 3.2f, 9.4f, 0.001f, 9.4f);
            glass(m, Color.valueOf("3b4f7a")).at(0, 0, z).ring(8, 4.68f, 4.8f, 5.4f, 7.2f);
            glow(m, s).at(0, 0, z + 9.5f).lathe(6, 0, 1.3f, 0f, 0.001f, 1.4f);
            dark(m, s).at(0, 0, z).ring(16, 6.6f, 7.4f, 0f, 0.8f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ).cyl(12, 9.4f, 0f, 1.4f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(crown, 0, 0, deckZ + 0.9f, 2, 0);
            m.add(cutter, 0, 0, deckZ + 1.3f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float w = warm(b), spin = total(b) * 2.4f;
            draw(crown, b, 0, 0, deckZ + 0.9f, 2, spin);
            lit(0.6f + 0.4f * w);
            draw(cutter, b, 0, 0, deckZ + 1.3f, 2, -spin * 2.6f);
            lit(1f);
        }

        @Override
        public void drawOver(Building b){
            float w = warm(b);
            light(b, 0, 0, deckZ + 9.6f, 3.6f, prism, w * (0.22f + Mathf.absin(time(b), 7f, 0.12f)));
            light(b, 0, 0, deckZ + 1.6f, 5.2f, prism, w * 0.18f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /**
     * Rift collector (3x3): a yoke carrying a tilted focusing dish. The dish turns to the facing the player gave
     * the block - the beam itself is drawn by the block, because it reaches tiles far outside the model.
     */
    public static class RiftCollector extends KitModel{
        final Mesh dish = new Mesh(), rotor = new Mesh();

        public RiftCollector(){
            super("rift-collector", 3, aurelia);
            //a shallow collector bowl, tilted 30 degrees, small enough to stay well inside the 3x3 footprint
            hull(dish, s).rot(1, -30f).lathe(10, 0, 3.0f, 0f, 3.0f, 0.4f, 2.3f, 1.2f, 1.3f, 1.5f, 0.001f, 1.5f);
            dark(dish, s).rot(1, -30f).ring(10, 3.0f, 3.25f, 0f, 0.5f);
            glow(dish, s).rot(1, -30f).lathe(6, 0, 0.8f, 1.5f, 0.001f, 2.3f);
            steel(rotor, s).lathe(8, 22.5f, 2.2f, 0f, 2.2f, 0.7f, 1.7f, 1.0f, 1.7f, 0f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //turntable, two short yoke posts, a straight capacitor bar in the south and a vent strip in the north
            dark(m, s).at(0, 0, z).ring(14, 2.4f, 3.4f, 0f, 0.8f);
            for(int sd = -1; sd <= 1; sd += 2){
                hull2(m, s).at(0, 0, 0).cbevel(sd * 4.0f, 0f, z + 0.8f, 1.5f, 2.6f, 3.4f, 0.22f);
                trim(m, s).box(sd * 4.0f - 0.8f, -1.3f, z + 4.1f, sd * 4.0f + 0.8f, 1.3f, z + 4.3f);
            }
            hull2(m, s).at(0, 0, 0).cbevel(0, -7.6f, z, 7.6f, 2.0f, 1.5f, 0.2f);
            for(int i = -1; i <= 1; i++) glow(m, s).box(i * 2.6f - 1.0f, -8.7f, z + 0.5f, i * 2.6f + 1.0f, -8.5f, z + 1.1f);
            vent(m, s, 0, 7.6f, z, 7.2f, 2.0f, 0.9f);
            conduit(m, s, -5.2f, -5.6f, z + 0.9f, -8.0f, -6.6f, z + 0.9f, 0.45f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-3.4f, -3.4f, deckZ + 1.6f, 3.4f, 3.4f, deckZ + 6.4f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(rotor, 0, 0, deckZ + 0.8f, 2, 0);
            m.add(dish, 0, 0, deckZ + 4.4f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float w = warm(b);
            float yaw = b != null && b.block.rotate ? b.rotdeg() - 90f : 0f;
            draw(rotor, b, 0, 0, deckZ + 0.8f, 2, yaw);
            lit(0.6f + 0.4f * w);
            draw(dish, b, 0, 0, deckZ + 4.4f, 2, yaw);
            lit(1f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 5.6f, 2.8f, lumen, warm(b) * (0.3f + Mathf.absin(time(b), 5f, 0.16f)));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    // ------------------------------------------------------------------------------------------ power

    /** Plasma dynamo (3x3): a horizontal turbine drum, intake manifold and an exhaust stack. */
    /**
     * Plasma dynamo (3x3), rebuilt in v7.9.
     *
     * <p>The old block was a drum lying east-west with its impeller spun about the Y axis - but the blades were
     * built around Z, so the wheel tumbled end over end instead of turning in its own plane. The same mistake
     * made the intake valve look like it was unscrewing sideways. Both moving parts are now built flat in the
     * XY plane and turned about Z, the one axis that is guaranteed to be the axis of the disc, so the rotor
     * spins the way a rotor spins and the handwheel turns the way a handwheel turns.
     *
     * <p>Shape: a vertical turbine column in the middle, a glass inspection band that shows the rotor, the
     * intake manifold with its valve on the west side, a condenser on the north-east and a short stack.
     */
    public static class PlasmaDynamo extends KitModel{
        final Mesh rotor = new Mesh(), wheel = new Mesh();

        public PlasmaDynamo(){
            super("plasma-dynamo", 3, aurelia);
            //rotor: eight blades around the Z axis, each one tilted a little so it reads as an impeller.
            //Built flat in XY - the disc normal is +Z, which is exactly the axis it is spun about.
            for(int i = 0; i < 8; i++){
                steel(rotor, s).at(0, 0, 0).rot(2, i * 45f).hexa(true,
                    -0.26f, 0.5f, -0.16f, 0.26f, 0.5f, -0.30f, 0.46f, 2.25f, -0.44f, -0.46f, 2.25f, -0.18f,
                    -0.26f, 0.5f, 0.16f, 0.26f, 0.5f, 0.02f, 0.46f, 2.25f, -0.12f, -0.46f, 2.25f, 0.14f);
            }
            rotor.twoSidedAll();
            dark(rotor, s).at(0, 0, 0).cyl(8, 0.78f, -0.45f, 0.45f);
            glow(rotor, s).at(0, 0, 0).cyl(8, 0.42f, 0.45f, 0.75f);
            //valve handwheel: a flat rim with four spokes, again built in XY and turned about Z
            trim(wheel, s).at(0, 0, 0).ring(12, 0.95f, 1.3f, -0.16f, 0.16f);
            for(int i = 0; i < 4; i++){
                steel(wheel, s).at(0, 0, 0).rot(2, i * 90f).box(-0.13f, 0f, -0.1f, 0.13f, 1.0f, 0.1f);
            }
            steel(wheel, s).at(0, 0, 0).cyl(6, 0.3f, -0.22f, 0.3f);
            wheel.twoSidedAll();
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //turbine column: a closed stepped cylinder standing on the deck
            hull(m, s).at(0, 0, z).lathe(12, 15f, 4.3f, 0f, 4.3f, 1.1f, 4.9f, 1.5f, 4.9f, 3.2f, 4.3f, 3.6f, 4.3f, 5.5f, 3.9f, 6.0f, 0.001f, 6.0f);
            //inspection band - the plasma loop glows behind this
            glass(m, Color.valueOf("3c6b7e")).at(0, 0, z).ring(12, 4.32f, 4.42f, 3.9f, 5.2f);
            steel(m, s).at(0, 0, z).ring(12, 4.3f, 4.6f, 3.55f, 3.9f);
            steel(m, s).at(0, 0, z).ring(12, 4.3f, 4.6f, 5.2f, 5.5f);
            //the rotor shroud: a short static collar on the head of the column. The impeller turns inside it,
            //so the only moving part of the block is ringed by static steel and can never read as detached.
            dark(m, s).at(0, 0, z + 6.0f).ring(12, 2.75f, 3.5f, 0f, 1.45f);
            steel(m, s).at(0, 0, z + 6.0f).ring(12, 2.75f, 3.6f, 1.45f, 1.75f);
            glow(m, s).at(0, 0, z + 6.0f).ring(12, 2.78f, 2.95f, 0.15f, 0.35f);
            //hub the impeller sits on
            dark(m, s).at(0, 0, z + 6.0f).cyl(8, 0.62f, 0f, 0.5f);
            //intake manifold and its valve body on the west side
            conduit(m, s, -4.6f, 0f, z + 1.6f, -8.3f, 0f, z + 1.6f, 0.62f);
            hull2(m, s).at(0, 0, 0).cbevel(-7.4f, -1.4f, z, 2.8f, 2.8f, 2.9f, 0.25f);
            dark(m, s).at(-6.0f, 0f, z + 2.9f).cyl(8, 0.42f, 0f, 0.95f);
            //condenser box north-east, low enough to stay under the north clearance line
            hull2(m, s).at(0, 0, 0).cbevel(5.6f, 5.0f, z, 3.0f, 2.6f, 1.7f, 0.2f);
            trim(m, s).box(4.3f, 3.9f, z + 1.7f, 6.9f, 6.1f, z + 1.85f);
            //exhaust stack south-east
            hull2(m, s).at(7.2f, -6.4f, z).lathe(8, 22.5f, 1.15f, 0f, 1.05f, 2.4f, 0.9f, 3.0f, 0.001f, 3.0f);
            dark(m, s).at(7.2f, -6.4f, z + 3.0f).ring(8, 0.45f, 0.9f, 0f, 0.22f);
            //cable trunk and console
            trim(m, s).at(0, 0, 0).box(-6.6f, -7.6f, z, 3.2f, -6.6f, z + 0.6f);
            console(m, s, -7.2f, -7.0f, z);
            bolts(m, s, 0, 0, z + 0.1f, 5.6f, 8);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-4.4f, -4.4f, deckZ + 0.4f, 4.4f, 4.4f, deckZ + 7.8f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(rotor, 0, 0, deckZ + 6.75f, 2, 0f);
            m.add(wheel, -6.0f, 0f, deckZ + 4.05f, 2, 0f);
        }

        @Override
        public void drawLive(Building b){
            //one axis, the disc's own axis: the rotor turns in its plane, the handwheel turns in its plane
            float spin = total(b) * 3.6f;
            draw(rotor, b, 0, 0, deckZ + 6.75f, 2, spin);
            draw(wheel, b, -6.0f, 0f, deckZ + 4.05f, 2, spin * 0.22f);
        }

        @Override
        public void drawOver(Building b){
            float e = warm(b) * (0.75f + Mathf.absin(time(b), 4f, 0.25f));
            light(b, 0, 0, deckZ + 7.0f, 3.2f, plasma, e * 0.35f);
            light(b, 7.2f, -6.4f, deckZ + 3.4f, 1.4f, ember, e * 0.22f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Aurora array (3x3): three tilted collector wings on a spine that track the sky. */
    public static class AuroraArray extends KitModel{
        final Mesh wing = new Mesh();

        public AuroraArray(){
            super("aurora-array", 3, aurelia);
            m2(wing);
            //thin tracking panels: never cull their back side, or the wings vanish as they tilt
            wing.twoSidedAll();
        }

        static void m2(Mesh wing){
            trim(wing, aurelia).rot(1, -22f).box(-5.4f, -1.9f, -0.26f, 5.4f, 1.9f, 0.26f);
            wing.color(Color.valueOf("5f7fb8")).style(glass, matGlass).rot(1, -22f).box(-5.1f, -1.65f, 0.26f, 5.1f, 1.65f, 0.34f);
            steel(wing, aurelia).rot(1, -22f).cyl(6, 0.32f, -0.9f, -0.3f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //spine runs north-south, three mounting collars; junction box in the south-west corner
            hull(m, s).at(0, 0, 0).box(-1.5f, -8.4f, z, 1.5f, 8.4f, z + 1.3f);
            for(int i = -1; i <= 1; i++){
                dark(m, s).at(0, i * 5.6f, z + 1.3f).cyl(8, 1.15f, 0f, 1.5f);
                steel(m, s).at(0, i * 5.6f, z + 2.8f).cyl(6, 0.5f, 0f, 0.4f);
            }
            hull2(m, s).at(0, 0, 0).cbevel(-7.2f, -7.2f, z, 2.4f, 2.4f, 1.8f, 0.2f);
            glow(m, s).box(-8.0f, -8.2f, z + 1.8f, -6.4f, -8.0f, z + 1.95f);
            bolts(m, s, 0, 0, z + 1.3f, 1.0f, 6);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-5.6f, -8.0f, deckZ + 2.6f, 5.6f, 8.0f, deckZ + 5.4f);
        }

        @Override
        public void buildRest(Mesh m){
            for(int i = -1; i <= 1; i++) m.add(wing, 0, i * 5.6f, deckZ + 3.2f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float t = Mathf.sin(time(b), 120f, 7f) * warm(b);
            for(int i = -1; i <= 1; i++) draw(wing, b, 0, i * 5.6f, deckZ + 3.2f, 2, t);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 3.4f, 5.4f, lumen, warm(b) * 0.16f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    // ------------------------------------------------------------------------------------------ unit repair

    /** Lumen repair beam (2x2): a mast with a gimbal head that turns to the unit being patched. */
    public static class LumenRepairBeam extends KitModel{
        final Mesh head = new Mesh();

        public LumenRepairBeam(){
            super("lumen-repair-beam", 2, aurelia);
            //compact emitter head: a faceted body with one short nozzle pointing east (model heading 0)
            hull(head, s).lathe(8, 22.5f, 1.5f, 0f, 1.5f, 1.0f, 1.1f, 1.6f, 0.001f, 1.8f);
            steel(head, s).at(0, 0, 0.6f).rot(1, 90).cyl(8, 0.5f, 1.2f, 2.4f);
            glow(head, s).at(0, 0, 0.6f).rot(1, 90).lathe(6, 0, 0.42f, 2.4f, 0.001f, 2.8f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            dark(m, s).at(0, 0, z).ring(12, 2.0f, 3.0f, 0f, 0.6f);
            hull2(m, s).at(0, 0, 0).taper(0, 0, z + 0.6f, 2.6f, 2.6f, z + 2.6f, 1.7f, 1.7f, 0, 0, true);
            bolts(m, s, 0, 0, z + 0.6f, 2.4f, 6);
            //flat service strip to the south and a low cable duct to the west: no loose parts
            hull2(m, s).at(0, 0, 0).cbevel(0, -5.4f, z, 5.0f, 1.6f, 1.2f, 0.18f);
            glow(m, s).box(-1.4f, -6.3f, z + 0.5f, 1.4f, -6.15f, z + 0.95f);
            conduit(m, s, -2.2f, -4.6f, z + 0.7f, -5.2f, -4.6f, z + 0.7f, 0.4f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-2.0f, -2.0f, deckZ + 2.6f, 3.4f, 2.0f, deckZ + 5.0f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(head, 0, 0, deckZ + 2.8f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            draw(head, b, 0, 0, deckZ + 2.8f, 2, aim(b));
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 3.8f, 2.2f, lumen, warm(b) * (0.25f + Mathf.absin(time(b), 4f, 0.15f)));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Aurora repair dome (3x3): a ribbed dome with a turning emitter crown that services several units. */
    public static class AuroraRepairDome extends KitModel{
        final Mesh crown = new Mesh();

        public AuroraRepairDome(){
            super("aurora-repair-dome", 3, aurelia);
            steel(crown, s).ring(12, 3.2f, 4.0f, 0f, 0.5f);
            for(int i = 0; i < 6; i++){
                float a = i * 60f;
                glow(crown, s).at(Mathf.cosDeg(a) * 3.6f, Mathf.sinDeg(a) * 3.6f, 0.5f).lathe(6, 0, 0.5f, 0f, 0.001f, 0.9f);
            }
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //dome, eight ribs, a service hatch to the south
            hull(m, s).at(0, 0, z).lathe(10, 0, 6.4f, 0f, 6.4f, 1.6f, 5.6f, 3.6f, 4.0f, 5.2f, 2.0f, 6.0f, 0.001f, 6.2f);
            for(int i = 0; i < 8; i++){
                float a = i * 45f + 22.5f;
                trim(m, s).at(Mathf.cosDeg(a) * 5.5f, Mathf.sinDeg(a) * 5.5f, z + 2.2f).rot(2, a).cbevel(0, 0, 0, 0.5f, 1.4f, 1.1f, 0.12f);
            }
            dark(m, s).at(0, 0, z).ring(16, 6.3f, 7.0f, 0f, 1.0f);
            glow(m, s).at(0, 0, z).ring(16, 7.0f, 7.2f, 0.45f, 0.6f);
            hull2(m, s).at(0, 0, 0).cbevel(0, -8.4f, z, 3.0f, 1.8f, 1.6f, 0.2f);
            console(m, s, 7.0f, -6.6f, z);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ + 6.0f).cyl(12, 4.2f, 0f, 1.6f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(crown, 0, 0, deckZ + 6.2f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            draw(crown, b, 0, 0, deckZ + 6.2f, 2, total(b) * 1.6f);
        }

        @Override
        public void drawOver(Building b){
            float w = warm(b);
            light(b, 0, 0, deckZ + 6.8f, 4.4f, lumen, w * (0.26f + Mathf.absin(time(b), 6f, 0.14f)));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Heading a repair station points at, in model space (0 = east), or a slow idle sweep when nothing is hurt. */
    static float aim(Building b){
        if(b instanceof blackhole.IndustryParts.RepairStation.RepairStationBuild r && r.aimAngle > -900f){
            return r.aimAngle - 90f;
        }
        return Mathf.sin(KitModel.time(b), 90f, 40f);
    }
}
