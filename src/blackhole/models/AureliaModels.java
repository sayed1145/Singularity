package blackhole.models;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import mindustry.gen.*;
import mindustry.world.blocks.power.PowerGenerator.*;

import static blackhole.g3d.Kit.*;
import static blackhole.g3d.Mesh.*;

/**
 * v7.1 Aurelia machines. Plain, readable industrial shapes in the pearl / silver / lumen kit: every machine shows
 * what it does (a burner with a fan, a water turbine, a rotary kiln, a mixing bowl, a piston pump), one or two
 * moving parts each, so they stay cheap.
 */
public final class AureliaModels{
    private AureliaModels(){}

    static final Color lumen = Color.valueOf("8fe9ff"), plasma = Color.valueOf("7ff0d8"), tide = Color.valueOf("3f7fc0"),
        ember = Color.valueOf("ffb45a"), glassC = Color.valueOf("9cc8ee");

    static float gen(Building b){
        return b instanceof GeneratorBuild g ? g.productionEfficiency : 0f;
    }

    static float eff(Building b){
        return b == null ? 0f : Mathf.clamp(b.efficiency);
    }

    // ------------------------------------------------------------------------------------------ power

    /** Lumen burner: octagonal combustion chamber with window band, exhaust stack and a cooling fan on top. */
    public static class LumenBurner extends KitModel{
        final Mesh blade = new Mesh(), hub = new Mesh();

        public LumenBurner(){
            super("lumen-burner", 2, aurelia);
            steel(blade, s).box(-3.1f, -0.42f, -0.08f, 3.1f, 0.42f, 0.08f);
            blade.twoSidedAll();
            dark(hub, s).cyl(8, 0.8f, 0f, 0.5f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            hull(m, s).at(0, 0, z).lathe(8, 22.5f, 4.9f, 0f, 4.9f, 0.6f, 4.6f, 1.0f, 4.6f, 4.2f, 4.2f, 4.6f, 0.001f, 4.6f);
            glass(m, Color.valueOf("3a5f86")).at(0, 0, z).ring(8, 4.6f, 4.68f, 1.6f, 2.6f);
            dark(m, s).at(0, 0, z).ring(8, 3.4f, 4.25f, 4.6f, 4.9f);
            grate(m, s).at(0, 0, z + 4.6f).cyl(8, 3.4f, 0f, 0.05f);
            //exhaust stack in the south-east corner, feed hopper on the west
            hull2(m, s).at(5.6f, -5.6f, z).lathe(8, 22.5f, 1.3f, 0f, 1.2f, 4.4f, 1.0f, 5.2f, 0.001f, 5.2f);
            dark(m, s).at(5.6f, -5.6f, z + 5.2f).ring(8, 0.55f, 1.0f, 0f, 0.3f);
            hull2(m, s).at(0, 0, 0).taper(-6.0f, 3.0f, z, 2.4f, 2.4f, z + 2.2f, 3.2f, 3.2f, 0, 0, true);
            trim(m, s).box(-7.3f, 1.35f, z + 1.6f, -4.7f, 1.4f, z + 2.0f);
            steel(m, s).at(0, 0, 0).rot(1, 90).cyl(8, 0.4f, -1f, 1f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-3.4f, -3.4f, deckZ + 4.6f, 3.4f, 3.4f, deckZ + 5.6f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(hub, 0, 0, deckZ + 4.7f, 2, 0);
            m.add(blade, 0, 0, deckZ + 5.1f, 2, 20f);
            m.add(blade, 0, 0, deckZ + 5.1f, 2, 110f);
        }

        @Override
        public void drawLive(Building b){
            float a = total(b) * 9f;
            draw(hub, b, 0, 0, deckZ + 4.7f, 2, 0);
            draw(blade, b, 0, 0, deckZ + 5.1f, 2, a);
            draw(blade, b, 0, 0, deckZ + 5.1f, 2, a + 90f);
        }

        @Override
        public void drawOver(Building b){
            float e = gen(b), f = e * (0.8f + Mathf.absin(time(b), 3f, 0.2f));
            light(b, 0, -4.7f, deckZ + 2.1f, 3.2f, ember, f * 0.45f);
            light(b, 5.6f, -5.6f, deckZ + 5.4f, 1.4f, ember, f * 0.25f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Tide turbine: a round basin of tidewater with a three-blade rotor turning in it. */
    public static class TideTurbine extends KitModel{
        final Mesh blade = new Mesh(), hub = new Mesh();

        public TideTurbine(){
            super("tide-turbine", 2, aurelia);
            waterCx = 0;
            waterCy = 0;
            hull2(blade, s).hexa(true, -0.35f, 0.8f, -0.12f, 0.35f, 0.8f, -0.12f, 0.9f, 5.0f, -0.08f, -0.2f, 5.0f, -0.08f,
                -0.35f, 0.8f, 0.12f, 0.35f, 0.8f, 0.12f, 0.9f, 5.0f, 0.08f, -0.2f, 5.0f, 0.08f);
            blade.twoSidedAll();
            steel(hub, s).lathe(10, 0, 1.1f, 0f, 1.1f, 0.5f, 0.7f, 0.9f, 0.001f, 1.0f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            dark(m, s).at(0, 0, z).ring(16, 5.6f, 6.6f, 0f, 1.2f);
            trim(m, s).at(0, 0, z).ring(16, 6.5f, 6.7f, 1.0f, 1.3f);
            m.color(tide).style(glass, matWater).at(0, 0, z).cyl(16, 5.6f, 0f, 0.6f);
            //bearing mast (north) with a status light
            hull(m, s).at(0, 0, 0).cbevel(0, 6.4f, z, 1.6f, 1.2f, 2.6f, 0.15f);
            glow(m, s).box(-0.3f, 5.78f, z + 1.7f, 0.3f, 5.8f, z + 2.2f);
            for(int sd = -1; sd <= 1; sd += 2) conduit(m, s, sd * 6.6f, -5.4f, z + 0.5f, sd * 7.6f, -7.6f, z + 0.5f, 0.45f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ + 0.4f).cyl(12, 5.3f, 0f, 1.2f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(hub, 0, 0, deckZ + 0.5f, 2, 0);
            for(int i = 0; i < 3; i++) m.add(blade, 0, 0, deckZ + 0.9f, 2, i * 120f);
        }

        @Override
        public void drawLive(Building b){
            float a = total(b) * 4f;
            draw(hub, b, 0, 0, deckZ + 0.5f, 2, a);
            for(int i = 0; i < 3; i++) draw(blade, b, 0, 0, deckZ + 0.9f, 2, a + i * 120f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 5.8f, deckZ + 2f, 1.2f, lumen, gen(b) * 0.4f);
        }
    }

    /** Lumen cell: a pearl capsule battery; the charge shows as a lit column. */
    public static class LumenCell extends KitModel{
        public LumenCell(){
            super("lumen-cell", 1, aurelia);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            hull(m, s).at(0, 0.3f, deckZ).lathe(10, 0, 2.5f, 0f, 2.6f, 0.4f, 2.6f, 3.6f, 1.9f, 4.4f, 0.001f, 4.6f);
            dark(m, s).at(0, 0.3f, deckZ).ring(10, 2.6f, 2.72f, 0.9f, 1.2f);
            m.at(0, 0, 0);
            dark(m, s).box(-0.55f, -2.32f, deckZ + 0.8f, 0.55f, -2.2f, deckZ + 3.6f);
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
            float c = b.power == null ? 0f : b.power.status;
            int n = Mathf.clamp((int)Math.ceil(c * 4f), 0, 4);
            for(int i = 0; i < n; i++) light(b, 0, -2.35f, deckZ + 1.1f + i * 0.7f, 0.55f, lumen, 0.8f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    // ------------------------------------------------------------------------------------------ crafting

    /** Silt kiln: a rotary kiln drum on two cradles; burner at the west end, chute at the east. */
    public static class SiltKiln extends KitModel{
        final Mesh drum;

        public SiltKiln(){
            super("silt-kiln", 2, aurelia);
            drum = roller(Color.valueOf("c9a27a"), Color.valueOf("9c7a5a"), 2.4f, 9.4f, 12);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            for(int sd = -1; sd <= 1; sd += 2){
                hull2(m, s).at(0, 0, 0).cbevel(sd * 3.2f, 0, z, 1.4f, 5.6f, 1.6f, 0.15f);
                steel(m, s).at(sd * 3.2f, 0, z + 3.1f).rot(1, 90).ring(12, 2.4f, 2.8f, -0.45f, 0.45f);
            }
            //burner housing (west) and output chute (east)
            hull(m, s).at(0, 0, 0).cbevel(-6.6f, 0, z, 2.4f, 5.4f, 5.0f, 0.2f);
            dark(m, s).at(-5.38f, 0, z + 3.1f).rot(1, 90).cyl(10, 1.6f, 0f, 0.1f);
            hull2(m, s).at(0, 0, 0).taper(6.4f, -2.6f, z, 2.6f, 2.2f, z + 1.6f, 2.0f, 1.6f, 0, 0, true);
            dark(m, s).box(5.4f, -3.9f, z + 1.1f, 7.4f, -3.2f, z + 1.3f);
            steel(m, s).at(-6.6f, 1.8f, z + 5.0f).cyl(8, 0.55f, 0f, 1.6f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-4.8f, -2.6f, deckZ + 0.6f, 4.8f, 2.6f, deckZ + 5.6f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(drum, 0, 0, deckZ + 3.1f, 0, 0);
        }

        @Override
        public void drawLive(Building b){
            draw(drum, b, 0, 0, deckZ + 3.1f, 0, total(b) * 2.5f);
        }

        @Override
        public void drawOver(Building b){
            float w = warm(b);
            light(b, -5.3f, 0, deckZ + 3.1f, 2.2f, ember, w * (0.35f + Mathf.absin(time(b), 4f, 0.1f)));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Plasma mixer: two feed tanks pour into an open mixing bowl where a lumen impeller spins. */
    public static class PlasmaMixer extends KitModel{
        final Mesh impeller, paddle = new Mesh();

        public PlasmaMixer(){
            super("plasma-mixer", 2, aurelia);
            impeller = gem(plasma, true, 1.1f, 2.2f);
            steel(paddle, s).box(-2.6f, -0.3f, -0.12f, 2.6f, 0.3f, 0.12f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            tank(m, s, -5.0f, 4.6f, z, 1.8f, 4.2f, true);
            tank(m, s, 5.0f, 4.6f, z, 1.8f, 4.2f, true);
            conduit(m, s, -3.6f, 3.6f, z + 3.0f, -2.2f, 1.4f, z + 2.2f, 0.4f);
            conduit(m, s, 3.6f, 3.6f, z + 3.0f, 2.2f, 1.4f, z + 2.2f, 0.4f);
            hull(m, s).at(0, -0.6f, z).lathe(12, 0, 4.4f, 0f, 4.6f, 0.4f, 4.6f, 1.9f, 4.2f, 2.1f, 0.001f, 2.1f);
            m.color(Color.valueOf("2f8f86")).style(emissive, matPlain).at(0, -0.6f, z + 2.1f).cyl(12, 3.9f, 0f, 0.04f);
            trim(m, s).at(0, -0.6f, z).ring(12, 4.6f, 4.72f, 1.2f, 1.5f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, -0.6f, deckZ + 2.1f).cyl(10, 2.8f, 0f, 3.4f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(paddle, 0, -0.6f, deckZ + 2.4f, 2, 30f);
            m.add(impeller, 0, -0.6f, deckZ + 3.2f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float a = total(b) * 5f, w = warm(b);
            draw(paddle, b, 0, -0.6f, deckZ + 2.4f, 2, a);
            lit(0.5f + 0.8f * w);
            draw(impeller, b, 0, -0.6f, deckZ + 3.2f, 2, -a * 0.6f);
            lit(1f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, -0.6f, deckZ + 3.4f, 3.2f, plasma, warm(b) * 0.35f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    // ------------------------------------------------------------------------------------------ liquids

    /** Tide pump: a piston pump over an intake grate; the plunger strokes while it pumps. */
    public static class TidePump extends KitModel{
        final Mesh plunger = new Mesh(), rod;

        public TidePump(){
            super("tide-pump", 2, aurelia);
            hull2(plunger, s).lathe(10, 0, 1.9f, 0f, 1.9f, 1.2f, 1.4f, 1.6f, 0.001f, 1.6f);
            rod = rod(s.metal, metal);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            grate(m, s).at(0, 0, z).cyl(12, 3.4f, 0f, 0.05f);
            m.color(tide).style(glass, matWater).at(0, 0, z - 0.05f).cyl(12, 3.3f, 0f, 0.06f);
            dark(m, s).at(0, 0, z).ring(12, 3.4f, 4.0f, 0f, 0.8f);
            //two side columns and a low crossbeam carrying the cylinder
            for(int sd = -1; sd <= 1; sd += 2){
                hull(m, s).at(0, 0, 0).cbevel(sd * 5.2f, 0, z, 1.6f, 2.4f, 5.6f, 0.15f);
                glow(m, s).box(sd * 5.2f - 0.3f, -1.22f, z + 3.6f, sd * 5.2f + 0.3f, -1.2f, z + 4.4f);
            }
            hull2(m, s).at(0, 0, 0).cbevel(0, 0, z + 5.6f, 12.0f, 2.4f, 1.1f, 0.2f);
            steel(m, s).at(0, 0, z + 5.6f).cyl(10, 1.0f, -0.6f, 0f);
            conduit(m, s, 4.0f, -2.2f, z + 0.5f, 7.6f, -6.4f, z + 0.5f, 0.55f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ + 0.6f).cyl(10, 2.0f, 0f, 4.6f);
        }

        float stroke(Building b){
            return 0.5f + 0.5f * Mathf.sin(total(b) * 0.12f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(plunger, 0, 0, deckZ + 1.6f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float zp = deckZ + 0.8f + stroke(b) * 2.2f;
            link(rod, b, 0, 0, zp + 1.5f, 0, 0, deckZ + 5.0f, 0.7f);
            draw(plunger, b, 0, 0, zp, 2, 0);
        }

        @Override
        public void drawOver(Building b){
            float e = eff(b);
            light(b, -5.2f, -1.3f, deckZ + 4f, 0.9f, lumen, e * 0.5f);
            light(b, 5.2f, -1.3f, deckZ + 4f, 0.9f, lumen, e * 0.5f);
        }
    }

    // ------------------------------------------------------------------------------------------ effect

    /** Lumen mender: a small pylon with a floating lumen crystal that pulses when it heals. */
    public static class LumenMender extends KitModel{
        final Mesh crystal;
        public float pulse;

        public LumenMender(){
            super("lumen-mender", 1, aurelia);
            crystal = gem(lumen, true, 0.9f, 1.8f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            hull(m, s).at(0, 0, deckZ).lathe(6, 30f, 2.6f, 0f, 2.4f, 0.8f, 1.5f, 1.6f, 0.001f, 1.7f);
            for(int i = 0; i < 3; i++){
                float a = 90f + i * 120f;
                trim(m, s).at(0, 0, 0).cbevel(Mathf.cosDeg(a) * 2.2f, Mathf.sinDeg(a) * 2.2f, deckZ + 0.6f, 0.6f, 0.6f, 1.8f, 0.1f);
            }
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ + 1.7f).cyl(6, 1.1f, 0f, 3.0f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(crystal, 0, 0, deckZ + 2.4f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float w = eff(b);
            lit(0.5f + 0.7f * w);
            draw(crystal, b, 0, 0, deckZ + 2.4f, 2, total(b) * 1.5f + time(b) * 0.4f);
            lit(1f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 3.2f, 1.8f, lumen, eff(b) * (0.25f + Mathf.absin(time(b), 10f, 0.15f)));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Lumen vault: a banded pearl strongbox (static only, drawn by the vanilla storage block). */
    public static class LumenVault extends KitModel{
        public LumenVault(){
            super("lumen-vault", 3, aurelia);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            hull(m, s).at(0, 0, 0).bevel(-9.4f, -9.0f, z, 9.4f, 8.6f, z + 4.6f, 0.5f);
            for(int i = -1; i <= 1; i++){
                trim(m, s).box(i * 5.2f - 0.5f, -9.1f, z + 0.3f, i * 5.2f + 0.5f, 8.7f, z + 4.75f);
            }
            dark(m, s).at(0, 0, 0).cbevel(0, -9.1f, z + 1.0f, 5.0f, 0.3f, 2.8f, 0.1f);
            team(m).box(-2.0f, -9.3f, z + 3.5f, 2.0f, -9.2f, z + 3.9f);
            glow(m, s).box(-0.5f, -9.28f, z + 2.1f, 0.5f, -9.26f, z + 2.5f);
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
}
