package blackhole.models;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import blackhole.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import blackhole.g3d.Kit.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static blackhole.g3d.Kit.*;
import static blackhole.g3d.Mesh.*;

/**
 * Every production / utility block as a real 3D model with its own animation. Static geometry is baked (AO,
 * materials, perspective); moving parts are small live meshes drawn with the block's camera and occluded by the
 * baked front layer. Serpulo / Erekir twins share geometry and differ in faction materials.
 */
public final class BlockModels{
    private BlockModels(){}

    public static final Color violet = Color.valueOf("b58cff"), amber = Color.valueOf("ffb45a"), lumen = Color.valueOf("8fe9ff"),
        resonance = Color.valueOf("b69cff"), concord = Color.valueOf("ffe29a");

    // =====================================================================================================
    // Singularity
    // =====================================================================================================

    /**
     * Graviton press (2x2), v7.1: a plain, recognisable forging press. A steel die with a violet gravity ring sits on
     * a low bed between two columns; a crossbeam carries a hydraulic cylinder whose rod drives the ram (hammer block)
     * and its guide skirt. The ram rises slowly, then slams onto the die with a violet flash; pressed blocks wait on
     * the output tray.
     */
    public static class GravitonPress extends KitModel{
        static final float cy = -0.4f, dieTop = 1.8f, beamLo = 6.2f, ramH = 1.1f, travel = 3.2f;
        final Mesh ram = new Mesh(), skirt = new Mesh(), rod;

        public GravitonPress(){
            super("graviton-press", 2, sing);
            hull(ram, s).bevel(-2.2f, -1.6f, 0f, 2.2f, 1.6f, ramH, 0.2f);
            steel(skirt, s).box(-4.2f, -0.9f, 0f, 4.2f, 0.9f, 0.4f);
            rod = rod(s.metal, metal);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            float z = deckZ;
            //bed and die
            dark(m, s).at(0, 0, 0).bevel(-4.4f, cy - 3.0f, z, 4.4f, cy + 3.0f, z + 0.6f, 0.2f);
            steel(m, s).at(0, cy, z + 0.6f).lathe(4, 45f, 3.3f, 0f, 3.3f, 0.8f, 2.6f, dieTop - 0.6f, 0.001f, dieTop - 0.6f);
            glow2(m, s).at(0, cy, z + dieTop).ring(16, 1.2f, 1.6f, 0f, 0.04f);
            //columns with status lights
            for(int sd = -1; sd <= 1; sd += 2){
                hull2(m, s).at(0, 0, 0).cbevel(sd * 5.3f, cy, z, 1.8f, 2.4f, beamLo, 0.2f);
                dark(m, s).box(sd * 5.3f - 0.6f, cy - 1.22f, z + 1.0f, sd * 5.3f + 0.6f, cy - 1.2f, z + 4.8f);
                glow2(m, s).box(sd * 5.3f - 0.3f, cy - 1.24f, z + 3.6f, sd * 5.3f + 0.3f, cy - 1.22f, z + 4.4f);
            }
            //crossbeam and hydraulic cylinder
            hull(m, s).at(0, 0, 0).bevel(-6.3f, cy - 1.4f, z + beamLo, 6.3f, cy + 1.4f, z + beamLo + 1.0f, 0.25f);
            steel(m, s).at(0, cy, z + beamLo + 1.0f).lathe(10, 0, 1.2f, 0f, 1.2f, 0.9f, 0.8f, 1.2f, 0.001f, 1.2f);
            dark(m, s).at(0, cy, z + beamLo + 1.0f).ring(10, 1.2f, 1.35f, 0.2f, 0.4f);
            //output tray with three pressed blocks
            dark(m, s).at(0, 0, 0).bevel(2.4f, -7.4f, z, 7.2f, -4.4f, z + 0.35f, 0.1f);
            for(int i = 0; i < 3; i++){
                m.color(Color.valueOf("2a2433")).style(metal, matPlain).at(0, 0, 0)
                    .bevel(2.9f + i * 1.45f, -6.8f, z + 0.35f, 4.0f + i * 1.45f, -5.2f, z + 1.2f, 0.12f);
            }
            vent(m, s, -5.2f, -6.0f, z, 2.2f, 1.6f, 0.8f);
            m.at(0, 0, 0);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-4.4f, cy - 1.8f, deckZ + dieTop, 4.4f, cy + 1.8f, deckZ + beamLo);
        }

        /** 0 = ram on the die, 1 = fully raised: slow rise to p = 0.82, slam within 0.06. */
        float lift(Building b){
            float p = prog(b);
            if(p < 0.82f) return smooth(0f, 0.82f, p);
            return 1f - Mathf.clamp((p - 0.82f) / 0.06f);
        }

        float impact(Building b){
            float p = prog(b);
            return p >= 0.88f && p < 0.98f ? 1f - (p - 0.88f) / 0.1f : 0f;
        }

        @Override
        public void buildRest(Mesh m){
            float zb = deckZ + dieTop + 0.35f * travel;
            m.add(ram, 0, cy, zb, 2, 0);
            m.add(skirt, 0, cy, zb + ramH, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float zb = deckZ + dieTop + 0.02f + lift(b) * travel;
            link(rod, b, 0, cy, zb + ramH + 0.4f, 0, cy, deckZ + beamLo, 0.9f);
            draw(ram, b, 0, cy, zb, 2, 0);
            draw(skirt, b, 0, cy, zb + ramH, 2, 0);
        }

        @Override
        public void drawOver(Building b){
            float w = warm(b), f = impact(b);
            light(b, 0, cy, deckZ + dieTop + 0.2f, 2.0f + f * 2.4f, violet, w * (0.2f + f * 0.8f));
            light(b, -5.3f, cy - 1.3f, deckZ + 4.0f, 0.8f, violet, w * 0.4f);
            light(b, 5.3f, cy - 1.3f, deckZ + 4.0f, 0.8f, violet, w * 0.4f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Hawking condenser (3x3): a caged radiation orb with four condenser plates orbiting it. */
    public static class HawkingCondenser extends KitModel{
        final Mesh orb = new Mesh(), plate = new Mesh();
        static final float oz = 6.0f, oy = 1.0f;

        public HawkingCondenser(){
            super("hawking-condenser", 3, sing);
            orb.color(violet).style(emissive, 0).lathe(10, 0, 0.001f, -2.0f, 1.3f, -1.5f, 2.0f, 0f, 1.3f, 1.5f, 0.001f, 2.0f);
            hull2(plate, s).hexa(false, -1.4f, -0.3f, -1.4f, 1.4f, -0.3f, -1.4f, 1.1f, 0.3f, -1.1f, -1.1f, 0.3f, -1.1f,
                -1.4f, -0.3f, 1.4f, 1.4f, -0.3f, 1.4f, 1.1f, 0.3f, 1.1f, -1.1f, 0.3f, 1.1f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            dark(m, s).at(0, oy, 0).lathe(12, 0, 5.2f, deckZ, 5.0f, deckZ + 1.2f, 3.6f, deckZ + 2.0f, 0.001f, deckZ + 2.0f);
            glow2(m, s).at(0, oy, 0).ring(20, 3.0f, 3.4f, deckZ + 2.0f, deckZ + 2.05f);
            for(int i = 0; i < 3; i++){
                float a = 90f + i * 120f, x = Mathf.cosDeg(a) * 5.4f, y = oy + Mathf.sinDeg(a) * 5.4f;
                hull(m, s).at(0, 0, 0).cbevel(x, y, deckZ, 1.2f, 1.2f, 8.6f, 0.15f);
                glow(m, s).at(x, y, deckZ + 8.6f).cyl(8, 0.35f, 0f, 0.4f);
            }
            tank(m, s, -9.0f, 8.6f, deckZ, 1.6f, 4.2f, true);
            tank(m, s, 9.0f, 8.6f, deckZ, 1.6f, 4.2f, true);
            vent(m, s, -8.8f, -8.4f, deckZ, 3.2f, 2.6f, 1.2f);
            console(m, s, 7.6f, -8.8f, deckZ);
            //v7.8: rail + posts, so the orbiting plates visibly run on the machine instead of floating
            steel(m, s).at(0, oy, 0).ring(16, 3.2f, 3.5f, oz - 0.16f, oz + 0.16f);
            for(int i = 0; i < 3; i++){
                float a = 30f + i * 120f;
                steel(m, s).at(Mathf.cosDeg(a) * 3.35f, oy + Mathf.sinDeg(a) * 3.35f, deckZ + 1.6f).cyl(6, 0.28f, 0f, oz - deckZ - 1.6f);
            }
            conduit(m, s, -9.0f, 7.0f, deckZ + 1.0f, -4.0f, 3.4f, deckZ + 1.0f, 0.35f);
            conduit(m, s, 9.0f, 7.0f, deckZ + 1.0f, 4.0f, 3.4f, deckZ + 1.0f, 0.35f);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, oy, oz - 3f).cyl(16, 4.6f, 0f, 6f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(orb, 0, oy, oz, 2, 0);
            orbitRest(m, plate, 4, 0, oy, oz, 3.4f, 20f);
        }

        @Override
        public void drawLive(Building b){
            float w = warm(b), a = total(b) * 1.6f;
            orbit(plate, b, 4, 0, oy, oz, 3.4f, a, true);
            lit(0.45f + 0.55f * w + Mathf.absin(time(b), 8f, 0.15f) * w);
            draw(orb, b, 0, oy, oz, 2, a * 0.5f);
            lit(1f);
            orbit(plate, b, 4, 0, oy, oz, 3.4f, a, false);
        }

        @Override
        public void drawOver(Building b){
            float w = warm(b);
            light(b, 0, oy, oz, 4.2f, violet, w * 0.35f);
            for(int i = 0; i < 3; i++){
                float an = 90f + i * 120f;
                beam(b, Mathf.cosDeg(an) * 5.4f, oy + Mathf.sinDeg(an) * 5.4f, deckZ + 9.0f, 0, oy, oz, 0.7f, violet, w * (0.3f + Mathf.absin(time(b) + i * 9f, 5f, 0.3f)));
            }
        }

        @Override
        public void drawCheap(Building b){
            light(b, 0, oy, oz, 4.2f, violet, warm(b) * 0.5f);
        }
    }

    /** Singularity forge (4x4): a containment pit between four pylons; a live micro black hole is fed by beams. */
    public static class SingularityForge extends KitModel{
        final Mesh pod = new Mesh();
        static final float hz = 6.4f;
        /** screen-space lens strength of the centre singularity at full warmup (projectiles use 0.75 - 1.25) */
        public static float lensStrength = 1.0f;

        public SingularityForge(){
            super("singularity-forge", 4, sing);
            dark(pod, s).lathe(8, 22.5f, 0.001f, -1.0f, 0.9f, -0.6f, 1.0f, 0.4f, 0.5f, 1.0f, 0.001f, 1.1f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            //containment pit: armoured rim around a dark well
            hull2(m, s).at(0, 0, 0).lathe(16, 0, 7.6f, deckZ, 7.4f, deckZ + 1.4f, 6.2f, deckZ + 1.8f, 5.6f, deckZ + 1.6f, 5.4f, deckZ + 0.6f);
            m.color(0.05f, 0.05f, 0.07f).style(0, 0).at(0, 0, deckZ + 0.6f).cyl(16, 5.45f, 0f, 0.02f);
            glow(m, s).at(0, 0, 0).ring(24, 5.5f, 5.8f, deckZ + 1.62f, deckZ + 1.66f);
            for(int i = 0; i < 4; i++){
                float a = 45f + i * 90f, x = Mathf.cosDeg(a) * 10.8f, y = Mathf.sinDeg(a) * 10.8f;
                dark(m, s).at(x, y, deckZ).lathe(6, 0, 2.0f, 0f, 1.9f, 0.8f, 0.001f, 0.8f);
                hull(m, s).at(x, y, deckZ + 0.8f).lathe(6, 0, 1.3f, 0f, 1.0f, 7.0f, 0.6f, 8.2f, 0.001f, 8.4f);
                glow(m, s).at(x, y, deckZ + 6.2f).ring(6, 1.05f, 1.15f, 0f, 0.8f);
            }
            for(int sd = -1; sd <= 1; sd += 2){
                vent(m, s, sd * 12.6f, 0f, deckZ, 2.4f, 6.0f, 1.4f);
                conduit(m, s, sd * 12.0f, -3.4f, deckZ + 0.8f, sd * 7.0f, -2.0f, deckZ + 1.2f, 0.4f);
            }
            //v7.8: the pod ring runs on a rail carried by three posts off the pit rim
            steel(m, s).at(0, 0, 0).ring(16, 4.25f, 4.55f, hz - 0.18f, hz + 0.18f);
            for(int i = 0; i < 3; i++){
                float a = 60f + i * 120f;
                steel(m, s).at(Mathf.cosDeg(a) * 4.4f, Mathf.sinDeg(a) * 4.4f, deckZ + 1.6f).cyl(6, 0.3f, 0f, hz - deckZ - 1.6f);
            }
            console(m, s, 0f, -13.0f, deckZ);
            hazard(m).at(0, 0, 0).box(-5f, -14.6f, deckZ, 5f, -14.0f, deckZ + 0.03f);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, hz - 2.5f).cyl(16, 5.4f, 0f, 5f);
        }

        @Override
        public void buildRest(Mesh m){
            orbitRest(m, pod, 3, 0, 0, hz, 4.4f, 30f);
        }

        @Override
        public void drawLive(Building b){
            float a = total(b) * 2.2f;
            orbit(pod, b, 3, 0, 0, hz, 4.4f, a, true);
        }

        @Override
        public void drawOver(Building b){
            float w = warm(b), a = total(b) * 2.2f, bob = Mathf.sin(time(b), 24f, 0.4f);
            if(w > 0.02f){
                //v7.2: the centre singularity is the v6.1 black hole (Gargantua disk + photon ring) and it bends the
                //whole frame through the v6.1 full-screen gravitational lens, exactly like the weapon projectiles
                float hx = b.x + cam.sx(0, hz), hy = b.y + cam.sy(0, hz), r = 1.5f * w;
                BHShaders.addLens(hx, hy, r, lensStrength * w);
                Draw.z(Layer.block + 0.05f);
                BlackHoleRenderer.draw(hx, hy, r, 0.72f, time(b), w);
                Draw.z(Layer.block);
            }
            for(int i = 0; i < 4; i++){
                float an = 45f + i * 90f;
                beam(b, Mathf.cosDeg(an) * 10.8f, Mathf.sinDeg(an) * 10.8f, deckZ + 8.8f, 0, 0, hz, 0.8f, amber, w * (0.25f + Mathf.absin(time(b) + i * 11f, 4f, 0.35f)));
            }
            orbit(pod, b, 3, 0, 0, hz + bob, 4.4f, a, false);
        }

        @Override
        public void drawCheap(Building b){
            light(b, 0, 0, hz, 5f, amber, warm(b) * 0.5f);
        }
    }

    // =====================================================================================================
    // Aegis (Serpulo / Erekir)
    // =====================================================================================================

    /** Aegis press (3x3): furnace, two counter-rotating rollers and an alloy slab sliding down the tray. */
    public static class AegisPress extends KitModel{
        final Mesh roll, slab = new Mesh();

        public AegisPress(String name, Style st){
            super(name, 3, st);
            roll = roller(st.metal, st.dark, 1.3f, 8.0f, 10);
            slab.color(st.glow).style(emissive, 0).bevel(-2.2f, -1.2f, 0f, 2.2f, 1.2f, 0.5f, 0.12f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            //furnace (north)
            hull(m, s).at(0, 0, 0).bevel(-7.6f, 2.6f, deckZ, 7.6f, 9.6f, deckZ + 6.2f, 0.35f);
            dark(m, s).box(-3.2f, 2.56f, deckZ + 0.6f, 3.2f, 2.6f, deckZ + 3.6f);
            glow(m, s).box(-2.6f, 2.54f, deckZ + 0.9f, 2.6f, 2.56f, deckZ + 3.2f);
            fins(m, s).box(-7.0f, 3.2f, deckZ + 6.2f, 7.0f, 9.0f, deckZ + 6.22f);
            steel(m, s).at(5.4f, 7.4f, deckZ + 6.2f).cyl(10, 0.9f, 0f, 2.6f);
            steel(m, s).at(-5.4f, 7.4f, deckZ + 6.2f).cyl(10, 0.9f, 0f, 2.0f);
            //roller cheeks
            for(int sd = -1; sd <= 1; sd += 2){
                hull2(m, s).at(0, 0, 0).bevel(sd * 4.6f - 0.7f, -6.6f, deckZ, sd * 4.6f + 0.7f, 1.6f, deckZ + 4.6f, 0.15f);
                steel(m, s).at(sd * 4.6f, -1.0f, deckZ + 3.2f).rot(1, 90).cyl(8, 0.7f, -0.8f, 0.8f);
                steel(m, s).at(sd * 4.6f, -4.0f, deckZ + 3.2f).rot(1, 90).cyl(8, 0.7f, -0.8f, 0.8f);
            }
            //output tray
            dark(m, s).at(0, 0, 0).bevel(-2.8f, -11.2f, deckZ, 2.8f, -1.6f, deckZ + 0.5f, 0.1f);
            trim(m, s).box(-2.9f, -11.2f, deckZ + 0.5f, -2.5f, -1.6f, deckZ + 0.9f);
            trim(m, s).box(2.5f, -11.2f, deckZ + 0.5f, 2.9f, -1.6f, deckZ + 0.9f);
            vent(m, s, -8.6f, -8.6f, deckZ, 3.0f, 3.0f, 1.2f);
            tank(m, s, 8.6f, -8.4f, deckZ, 1.5f, 3.4f, true);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-4.0f, -5.5f, deckZ + 1.6f, 4.0f, 0.5f, deckZ + 4.8f);
            m.box(-2.4f, -11.0f, deckZ + 0.5f, 2.4f, -1.4f, deckZ + 1.2f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(roll, 0, -1.0f, deckZ + 3.2f, 0, 0);
            m.add(roll, 0, -4.0f, deckZ + 3.2f, 0, 0);
        }

        @Override
        public void drawLive(Building b){
            float t = total(b) * 6f, p = prog(b), w = warm(b);
            float sy = Mathf.lerp(-1.6f, -9.8f, p);
            lit(0.5f + 0.5f * w);
            if(w > 0.05f) draw(slab, b, 0, sy, deckZ + 0.5f, 2, 0);
            lit(1f);
            draw(roll, b, 0, -4.0f, deckZ + 3.2f, 0, t);
            draw(roll, b, 0, -1.0f, deckZ + 3.2f, 0, -t);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 2.5f, deckZ + 2f, 3.2f, s.glow, warm(b) * (0.3f + Mathf.absin(time(b), 6f, 0.15f)));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Signal encoder (3x3): two server racks, a console and a tracking dish antenna on a mast. */
    public static class SignalEncoder extends KitModel{
        final Mesh dish = new Mesh(), head = new Mesh();

        public SignalEncoder(String name, Style st){
            super(name, 3, st);
            head.color(st.hull2).style(metal, matPlate).cbevel(0, 0, 0, 1.6f, 1.6f, 1.2f, 0.15f);
            //dish facing +y, tilted up 30 degrees
            dish.color(st.hull).style(metal, 0).at(0, 0, 0).rot(0, -60f)
                .lathe(14, 0, 0.001f, -0.3f, 1.0f, -0.25f, 2.6f, 0.4f, 3.2f, 1.0f, 2.8f, 1.0f, 2.2f, 0.7f, 0.001f, 0.35f);
            dish.color(st.glow).style(emissive, 0).at(0, 0, 0).rot(0, -60f).cyl(6, 0.3f, 0.4f, 1.9f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            for(int sd = -1; sd <= 1; sd += 2){
                float x = sd * 7.4f;
                hull(m, s).at(0, 0, 0).bevel(x - 2.2f, -3.6f, deckZ, x + 2.2f, 9.8f, deckZ + 7.0f, 0.25f);
                fins(m, s).box(x - 1.8f, -3.62f, deckZ + 0.8f, x + 1.8f, -3.6f, deckZ + 6.2f);
                for(int k = 0; k < 4; k++){
                    (k % 2 == 0 ? glow(m, s) : glow2(m, s)).box(x - 1.6f, -3.64f, deckZ + 1.2f + k * 1.3f, x - 0.4f, -3.62f, deckZ + 1.5f + k * 1.3f);
                }
                trim(m, s).box(x - 2.25f, -3.65f, deckZ + 6.4f, x + 2.25f, 9.85f, deckZ + 6.7f);
            }
            dark(m, s).at(0, 3.6f, 0).lathe(8, 22.5f, 1.8f, deckZ, 1.6f, deckZ + 1.0f, 0.9f, deckZ + 1.4f, 0.001f, deckZ + 1.4f);
            steel(m, s).at(0, 3.6f, deckZ + 1.4f).cyl(8, 0.55f, 0f, 5.2f);
            console(m, s, 0f, -7.4f, deckZ);
            conduit(m, s, -5.2f, -6.0f, deckZ + 0.4f, -1.2f, -6.8f, deckZ + 0.4f, 0.25f);
            conduit(m, s, 5.2f, -6.0f, deckZ + 0.4f, 1.2f, -6.8f, deckZ + 0.4f, 0.25f);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 3.6f, deckZ + 6.0f).cyl(14, 3.4f, 0f, 4.2f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(head, 0, 3.6f, deckZ + 6.6f, 2, 0);
            m.add(dish, 0, 3.6f, deckZ + 8.0f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
        }

        @Override
        public void drawOver(Building b){
            float w = warm(b), yaw = Mathf.sin(total(b) * 0.4f, 60f, 70f) + b.id * 17f;
            draw(head, b, 0, 3.6f, deckZ + 6.6f, 2, yaw);
            lit(0.5f + 0.5f * w * (0.6f + Mathf.absin(time(b), 4f, 0.4f)));
            draw(dish, b, 0, 3.6f, deckZ + 8.0f, 2, yaw);
            lit(1f);
            for(int sd = -1; sd <= 1; sd += 2){
                boolean on = ((int)(time(b) / 11f + sd) & 3) != 0;
                light(b, sd * 7.4f, -3.7f, deckZ + 3.0f, 1.4f, s.glow, w * (on ? 0.35f : 0.1f));
            }
        }

        @Override
        public void drawCheap(Building b){
            light(b, 0, 3.6f, deckZ + 8f, 2f, s.glow, warm(b) * 0.4f);
        }
    }

    /** Aurelia core (4x4): pearl landing platform, three curved spires and a floating lumen heart with orbiting shards. */
    public static class AureliaCore extends KitModel{
        final Mesh heart, shard;
        static final float hz = 9.0f;

        public AureliaCore(){
            super("aurelia-core", 4, aurelia);
            heart = gem(lumen, true, 1.9f, 3.4f);
            shard = gem(resonance, true, 0.55f, 1.3f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            //hexagonal landing platform
            hull2(m, s).at(0, 0, 0).lathe(6, 0, 11.4f, deckZ, 11.2f, deckZ + 1.0f, 10.4f, deckZ + 1.4f, 0.001f, deckZ + 1.4f);
            trim(m, s).at(0, 0, 0).ring(6, 10.3f, 10.6f, deckZ + 1.4f, deckZ + 1.5f);
            glow(m, s).at(0, 0, 0).ring(24, 4.6f, 5.0f, deckZ + 1.4f, deckZ + 1.46f);
            //central pedestal
            hull(m, s).at(0, 0, deckZ + 1.4f).lathe(12, 0, 3.8f, 0f, 3.4f, 1.6f, 2.0f, 2.6f, 1.2f, 3.4f, 0.001f, 3.5f);
            glow(m, s).at(0, 0, deckZ + 4.5f).cyl(12, 1.0f, 0f, 0.4f);
            m.at(0, 0, 0);
            //three swept spires
            for(int i = 0; i < 3; i++){
                float a = 90f + i * 120f, c = Mathf.cosDeg(a), sn = Mathf.sinDeg(a);
                float x0 = c * 8.2f, y0 = sn * 8.2f, x1 = c * 5.6f, y1 = sn * 5.6f;
                hull(m, s).hexa(false, x0 - sn * 1.3f, y0 + c * 1.3f, deckZ + 1.4f, x0 + sn * 1.3f, y0 - c * 1.3f, deckZ + 1.4f,
                    x0 + c * 1.8f + sn * 1.0f, y0 + sn * 1.8f - c * 1.0f, deckZ + 1.4f, x0 + c * 1.8f - sn * 1.0f, y0 + sn * 1.8f + c * 1.0f, deckZ + 1.4f,
                    x1 - sn * 0.5f, y1 + c * 0.5f, deckZ + 11.6f, x1 + sn * 0.5f, y1 - c * 0.5f, deckZ + 11.6f,
                    x1 + c * 0.7f + sn * 0.4f, y1 + sn * 0.7f - c * 0.4f, deckZ + 11.0f, x1 + c * 0.7f - sn * 0.4f, y1 + sn * 0.7f + c * 0.4f, deckZ + 11.0f);
                glow(m, s).at(x1, y1, deckZ + 11.6f).lathe(6, 0, 0.45f, 0f, 0.001f, 1.3f);
                trim(m, s).at(0, 0, 0).cbox(x0 + c * 0.9f, y0 + sn * 0.9f, deckZ + 1.4f, 2.6f, 2.6f, 0.5f);
            }
            //corner lumen lamps and service hatches
            for(int i = 0; i < 4; i++){
                float sx = (i & 1) == 0 ? -1 : 1, sy = (i & 2) == 0 ? -1 : 1;
                pylon(m, s, sx * 13.6f, sy * 13.6f, deckZ, 0.7f, 3.2f, i % 2 == 0);
            }
            //v7.8: shard rail, braced to the three spires
            steel(m, s).at(0, 0, 0).ring(16, 4.05f, 4.35f, hz - 0.2f, hz + 0.2f);
            for(int i = 0; i < 3; i++){
                float a = 90f + i * 120f, c = Mathf.cosDeg(a), sn = Mathf.sinDeg(a);
                steel(m, s).at(0, 0, 0).hexa(true, c * 4.2f - sn * 0.3f, sn * 4.2f + c * 0.3f, hz - 0.18f, c * 4.2f + sn * 0.3f, sn * 4.2f - c * 0.3f, hz - 0.18f,
                    c * 5.9f + sn * 0.3f, sn * 5.9f - c * 0.3f, hz - 0.18f, c * 5.9f - sn * 0.3f, sn * 5.9f + c * 0.3f, hz - 0.18f,
                    c * 4.2f - sn * 0.3f, sn * 4.2f + c * 0.3f, hz + 0.18f, c * 4.2f + sn * 0.3f, sn * 4.2f - c * 0.3f, hz + 0.18f,
                    c * 5.9f + sn * 0.3f, sn * 5.9f - c * 0.3f, hz + 0.18f, c * 5.9f - sn * 0.3f, sn * 5.9f + c * 0.3f, hz + 0.18f);
            }
            hazard(m).at(0, 0, 0).box(-4f, -15.2f, deckZ, 4f, -14.6f, deckZ + 0.03f);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, hz - 3.5f).cyl(16, 5.2f, 0f, 7f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(heart, 0, 0, hz, 2, 0);
            orbitRest(m, shard, 3, 0, 0, hz, 4.2f, 30f);
        }

        @Override
        public void drawLive(Building b){
            float t = time(b), a = t * 1.1f;
            orbit(shard, b, 3, 0, 0, hz, 4.2f, a, true);
            lit(0.8f + Mathf.absin(t, 12f, 0.25f));
            draw(heart, b, 0, 0, hz, 2, t * 0.7f);
            lit(1f);
            orbit(shard, b, 3, 0, 0, hz, 4.2f, a, false);
        }

        @Override
        public void drawOver(Building b){
            float t = time(b);
            light(b, 0, 0, hz, 6f, lumen, 0.18f + Mathf.absin(t, 12f, 0.1f));
            for(int i = 0; i < 3; i++){
                float an = 90f + i * 120f;
                beam(b, Mathf.cosDeg(an) * 5.6f, Mathf.sinDeg(an) * 5.6f, deckZ + 12.4f, 0, 0, hz + 1f, 0.5f, lumen, 0.18f + Mathf.absin(t + i * 13f, 7f, 0.2f));
            }
        }

        @Override
        public void drawCheap(Building b){
            light(b, 0, 0, hz, 6f, lumen, 0.3f);
        }
    }

    /** Lumen extractor (2x2): four-legged frame over a spinning crystal-toothed bit. */
    public static class LumenExtractor extends KitModel{
        final Mesh bit = new Mesh(), collar = new Mesh();

        public LumenExtractor(){
            super("lumen-extractor", 2, aurelia);
            hull2(collar, s).lathe(8, 22.5f, 2.2f, 0f, 2.2f, 0.9f, 1.6f, 1.3f, 0.001f, 1.3f);
            for(int i = 0; i < 6; i++){
                float a = i * 60f;
                bit.color(lumen).style(emissive, 0).at(Mathf.cosDeg(a) * 1.3f, Mathf.sinDeg(a) * 1.3f, 0).rot(2, a).rot(1, 20f)
                    .lathe(4, 45f, 0.35f, 0f, 0.3f, -0.9f, 0.001f, -1.5f);
            }
            bit.at(0, 0, 0);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            m.color(0.1f, 0.12f, 0.16f).style(0, 0).at(0, 0, deckZ).cyl(16, 2.6f, 0f, 0.02f);
            glow(m, s).at(0, 0, 0).ring(16, 2.6f, 2.9f, deckZ, deckZ + 0.06f);
            for(int i = 0; i < 4; i++){
                float sx = (i & 1) == 0 ? -1 : 1, sy = (i & 2) == 0 ? -1 : 1;
                hull(m, s).at(0, 0, 0).hexa(false, sx * 5.8f - 0.7f, sy * 5.8f - 0.7f, deckZ, sx * 5.8f + 0.7f, sy * 5.8f - 0.7f, deckZ,
                    sx * 5.8f + 0.7f, sy * 5.8f + 0.7f, deckZ, sx * 5.8f - 0.7f, sy * 5.8f + 0.7f, deckZ,
                    sx * 3.2f - 0.5f, sy * 3.2f - 0.5f, deckZ + 6.6f, sx * 3.2f + 0.5f, sy * 3.2f - 0.5f, deckZ + 6.6f,
                    sx * 3.2f + 0.5f, sy * 3.2f + 0.5f, deckZ + 6.6f, sx * 3.2f - 0.5f, sy * 3.2f + 0.5f, deckZ + 6.6f);
            }
            hull2(m, s).at(0, 0, deckZ + 6.4f).lathe(8, 22.5f, 3.9f, 0f, 3.9f, 0.8f, 2.8f, 1.5f, 0.001f, 1.6f);
            glow(m, s).at(0, 0, deckZ + 8.0f).lathe(6, 0, 0.8f, 0f, 0.001f, 1.2f);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ).cyl(12, 2.6f, 0f, 6.5f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(collar, 0, 0, deckZ + 4.2f, 2, 0);
            m.add(bit, 0, 0, deckZ + 4.2f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float w = warm(b), t = time(b);
            float spin = (b instanceof mindustry.world.blocks.production.Drill.DrillBuild d ? d.timeDrilled : t) * 4f;
            float z = deckZ + 3.4f + 0.8f * (1f - w) + Mathf.sin(t, 6f, 0.25f) * w;
            lit(0.5f + 0.5f * w);
            draw(bit, b, 0, 0, z, 2, spin);
            lit(1f);
            draw(collar, b, 0, 0, z, 2, spin * 0.25f);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 0.5f, 3.2f, lumen, warm(b) * (0.25f + Mathf.absin(time(b), 5f, 0.15f)));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Aurora panel (2x2): a sun-tracking photovoltaic wing on a pearl pedestal. */
    public static class AuroraPanel extends KitModel{
        final Mesh wing = new Mesh();

        public AuroraPanel(){
            super("aurora-panel", 2, aurelia);
            trim(wing, s).bevel(-6.6f, -4.6f, -0.35f, 6.6f, 4.6f, 0f, 0.15f);
            for(int i = 0; i < 4; i++){
                for(int j = 0; j < 3; j++){
                    float x = -6.0f + i * 3.05f, y = -4.0f + j * 2.75f;
                    wing.color(i % 2 == j % 2 ? Color.valueOf("3b5f9a") : Color.valueOf("32538a")).style(metal | glass, matGlass).box(x, y, 0f, x + 2.85f, y + 2.55f, 0.08f);
                }
            }
            wing.color(lumen).style(emissive, 0).box(-6.4f, -4.5f, 0.02f, 6.4f, -4.3f, 0.1f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            hull(m, s).at(0, 0, deckZ).lathe(8, 22.5f, 2.2f, 0f, 1.8f, 1.2f, 0.9f, 3.2f, 0.001f, 3.2f);
            steel(m, s).at(0, 0, deckZ + 2.6f).rot(1, 90).cyl(8, 0.5f, -1.4f, 1.4f);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, 0).box(-6.8f, -5.0f, deckZ + 0.5f, 6.8f, 5.0f, deckZ + 5.6f);
        }

        @Override
        public void buildRest(Mesh m){
            m.at(0, 0, 0);
            Mesh t = new Mesh();
            t.add(wing, 0, 0, 0, 0, 15f);
            m.add(t, 0, 0, deckZ + 3.3f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float tilt = 15f + Mathf.sin(Time.time * 0.01f + b.id, 1f, 6f);
            draw(wing, b, 0, 0, deckZ + 3.3f, 0, tilt);
        }

        @Override
        public void drawOver(Building b){
            float e = b instanceof mindustry.world.blocks.power.PowerGenerator.GeneratorBuild g ? g.productionEfficiency : 0f;
            light(b, 0, -4.2f, deckZ + 2.2f, 1.8f, lumen, e * 0.3f);
        }
    }

    /** Rift anchor (2x2): three claws hold a floating resonance shard over a rift seal. */
    public static class RiftAnchor extends KitModel{
        final Mesh shard;
        static final float hz = 6.2f;

        public RiftAnchor(){
            super("rift-anchor", 2, aurelia);
            shard = gem(resonance, true, 1.1f, 2.4f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            dark(m, s).at(0, 0, 0).lathe(6, 0, 3.8f, deckZ, 3.6f, deckZ + 0.8f, 2.4f, deckZ + 1.1f, 0.001f, deckZ + 1.1f);
            glow2(m, s).at(0, 0, 0).ring(12, 1.6f, 2.1f, deckZ + 1.1f, deckZ + 1.15f);
            for(int i = 0; i < 3; i++){
                float a = 90f + i * 120f, c = Mathf.cosDeg(a), sn = Mathf.sinDeg(a);
                m.at(0, 0, 0);
                hull(m, s).hexa(false, c * 5.6f - sn * 0.8f, sn * 5.6f + c * 0.8f, deckZ, c * 5.6f + sn * 0.8f, sn * 5.6f - c * 0.8f, deckZ,
                    c * 6.8f + sn * 0.7f, sn * 6.8f - c * 0.7f, deckZ, c * 6.8f - sn * 0.7f, sn * 6.8f + c * 0.7f, deckZ,
                    c * 3.4f - sn * 0.4f, sn * 3.4f + c * 0.4f, deckZ + 6.4f, c * 3.4f + sn * 0.4f, sn * 3.4f - c * 0.4f, deckZ + 6.4f,
                    c * 4.0f + sn * 0.35f, sn * 4.0f - c * 0.35f, deckZ + 6.0f, c * 4.0f - sn * 0.35f, sn * 4.0f + c * 0.35f, deckZ + 6.0f);
                glow2(m, s).at(c * 3.5f, sn * 3.5f, deckZ + 6.4f).lathe(4, 45f, 0.35f, 0f, 0.001f, 0.8f);
            }
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, hz - 2f).cyl(8, 1.4f, 0f, 4.5f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(shard, 0, 0, hz, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float t = time(b), w = Mathf.clamp(b.efficiency);
            lit(0.55f + 0.45f * w);
            draw(shard, b, 0, 0, hz, 2, t * (0.6f + w * 1.8f));
            lit(1f);
        }

        @Override
        public void drawOver(Building b){
            float w = Mathf.clamp(b.efficiency);
            light(b, 0, 0, hz, 3.2f, resonance, 0.12f + w * 0.3f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Harmonic relay (3x3): two tuning-fork pylons ring a concord gem inside four orbiting harmonic vanes. */
    public static class HarmonicRelay extends KitModel{
        final Mesh gemM, vane = new Mesh();
        static final float hz = 7.0f;

        public HarmonicRelay(){
            super("harmonic-relay", 3, aurelia);
            gemM = gem(concord, true, 1.4f, 2.8f);
            hull2(vane, s).hexa(false, -1.2f, -0.2f, -1.6f, 1.2f, -0.2f, -1.6f, 1.0f, 0.2f, -1.4f, -1.0f, 0.2f, -1.4f,
                -0.6f, -0.2f, 1.6f, 0.6f, -0.2f, 1.6f, 0.5f, 0.2f, 1.4f, -0.5f, 0.2f, 1.4f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            hull2(m, s).at(0, 0, 0).lathe(12, 0, 5.4f, deckZ, 5.2f, deckZ + 1.0f, 3.8f, deckZ + 1.6f, 0.001f, deckZ + 1.6f);
            glow(m, s).at(0, 0, 0).ring(20, 3.0f, 3.4f, deckZ + 1.6f, deckZ + 1.65f);
            for(int sd = -1; sd <= 1; sd += 2){
                float x = sd * 8.4f;
                hull(m, s).at(0, 0, 0).cbevel(x, 0, deckZ, 2.2f, 2.2f, 3.0f, 0.2f);
                for(int k = -1; k <= 1; k += 2){
                    hull(m, s).at(0, 0, 0).cbevel(x, k * 0.8f, deckZ + 3.0f, 1.0f, 0.6f, 7.0f, 0.12f);
                }
                glow(m, s).box(x - 0.1f, -0.5f, deckZ + 5.0f, x + 0.1f, 0.5f, deckZ + 9.6f);
            }
            for(int i = 0; i < 4; i++){
                float sx = (i & 1) == 0 ? -1 : 1, sy = (i & 2) == 0 ? -1 : 1;
                crystal(m, resonance, true, sx * 9.6f, sy * 9.6f, deckZ, 0.5f, 2.0f, 15f, sx * sy * 45f);
            }
            //v7.8: vane rail on three posts
            steel(m, s).at(0, 0, 0).ring(16, 3.45f, 3.75f, hz - 0.16f, hz + 0.16f);
            for(int i = 0; i < 3; i++){
                float a = 30f + i * 120f;
                steel(m, s).at(Mathf.cosDeg(a) * 3.6f, Mathf.sinDeg(a) * 3.6f, deckZ + 1.4f).cyl(6, 0.26f, 0f, hz - deckZ - 1.4f);
            }
            console(m, s, 0f, -9.6f, deckZ);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, hz - 2.5f).cyl(16, 5.0f, 0f, 5f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(gemM, 0, 0, hz, 2, 0);
            orbitRest(m, vane, 4, 0, 0, hz, 3.6f, 45f);
        }

        float align(Building b){
            return b instanceof HarmonicRelayBlock.HarmonicRelayBuild h ? h.alignment : warm(b);
        }

        @Override
        public void drawLive(Building b){
            float al = align(b), t = time(b), a = t * (0.3f + al * 2.2f);
            orbit(vane, b, 4, 0, 0, hz, 3.6f, a, true);
            lit(0.45f + 0.55f * al);
            draw(gemM, b, 0, 0, hz, 2, -a * 0.6f);
            lit(1f);
            orbit(vane, b, 4, 0, 0, hz, 3.6f, a, false);
        }

        @Override
        public void drawOver(Building b){
            float al = align(b);
            light(b, 0, 0, hz, 4.4f, concord, 0.1f + al * 0.35f);
            for(int sd = -1; sd <= 1; sd += 2){
                beam(b, sd * 8.4f, 0, deckZ + 9.8f, 0, 0, hz, 0.6f, lumen, al * (0.25f + Mathf.absin(time(b) + sd * 7f, 5f, 0.3f)));
            }
        }

        @Override
        public void drawCheap(Building b){
            light(b, 0, 0, hz, 4.4f, concord, 0.1f + align(b) * 0.35f);
        }
    }

    /** Prism press (3x3): a pearl press frame stamping prism alloy over a glowing lumen die. */
    public static class PrismPress extends KitModel{
        final Mesh plate = new Mesh(), rod, prism;

        public PrismPress(){
            super("prism-press", 3, aurelia);
            hull(plate, s).lathe(6, 0, 3.4f, 0f, 3.4f, 1.0f, 2.8f, 1.6f, 0.001f, 1.6f);
            rod = rod(s.metal, metal);
            prism = gem(Color.valueOf("e8f5ff"), false, 1.0f, 1.6f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            for(int i = 0; i < 3; i++){
                float a = 90f + i * 120f, x = Mathf.cosDeg(a) * 6.6f, y = Mathf.sinDeg(a) * 6.6f;
                hull2(m, s).at(0, 0, 0).cbevel(x, y, deckZ, 1.8f, 1.8f, 9.6f, 0.2f);
                glow(m, s).box(x - 0.2f, y - 0.92f, deckZ + 2f, x + 0.2f, y - 0.9f, deckZ + 7f);
            }
            hull(m, s).at(0, 0, deckZ + 9.6f).lathe(6, 0, 7.8f, 0f, 7.8f, 0.8f, 6.8f, 1.4f, 0.001f, 1.4f);
            dark(m, s).at(0, 0, deckZ).lathe(6, 0, 3.8f, 0f, 3.8f, 1.2f, 3.2f, 1.6f, 0.001f, 1.6f);
            glow(m, s).at(0, 0, 0).ring(6, 2.6f, 3.1f, deckZ + 1.6f, deckZ + 1.65f);
            for(int sd = -1; sd <= 1; sd += 2) tank(m, s, sd * 9.4f, -8.8f, deckZ, 1.2f, 3.0f, true);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ + 1.6f).cyl(6, 3.6f, 0f, 8f);
        }

        float lift(Building b){
            float p = prog(b);
            return p < 0.8f ? smooth(0f, 0.8f, p) : 1f - Mathf.clamp((p - 0.8f) / 0.08f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(prism, 0, 0, deckZ + 2.6f, 2, 0);
            m.add(plate, 0, 0, deckZ + 6.0f, 2, 0);
        }

        @Override
        public void drawLive(Building b){
            float l = lift(b), w = warm(b), z = deckZ + 3.8f + l * 3.6f;
            lit(0.4f + 0.6f * w);
            draw(prism, b, 0, 0, deckZ + 2.6f, 2, total(b) * 2f);
            lit(1f);
            link(rod, b, 0, 0, z + 1.6f, 0, 0, deckZ + 9.6f, 1.0f);
            draw(plate, b, 0, 0, z, 2, 0);
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 0, deckZ + 2.6f, 3f, lumen, warm(b) * (0.25f + (1f - lift(b)) * 0.35f));
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }

    /** Prism resonator (3x3): three prisms orbit a tall resonance spire above a tuned dish. */
    public static class PrismResonator extends KitModel{
        final Mesh prism, spire;
        static final float hz = 6.0f;

        public PrismResonator(){
            super("prism-resonator", 3, aurelia);
            prism = gem(Color.valueOf("e8f5ff"), false, 0.9f, 2.0f);
            spire = gem(concord, true, 1.0f, 3.6f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            hull2(m, s).at(0, 0, deckZ).lathe(16, 0, 6.2f, 0f, 6.0f, 0.8f, 4.8f, 1.8f, 1.8f, 1.2f, 0.001f, 1.2f);
            glow(m, s).at(0, 0, 0).ring(20, 4.9f, 5.2f, deckZ + 1.7f, deckZ + 1.8f);
            hull(m, s).at(0, 0, deckZ + 1.2f).lathe(6, 0, 1.4f, 0f, 1.0f, 2.4f, 0.001f, 2.5f);
            for(int i = 0; i < 4; i++){
                float sx = (i & 1) == 0 ? -1 : 1, sy = (i & 2) == 0 ? -1 : 1;
                pylon(m, s, sx * 9.6f, sy * 9.6f, deckZ, 0.8f, 4.4f, (i & 1) == 0);
            }
            //v7.8: prism rail on three posts
            steel(m, s).at(0, 0, 0).ring(16, 3.65f, 3.95f, hz - 0.68f, hz - 0.32f);
            for(int i = 0; i < 3; i++){
                float a = 30f + i * 120f;
                steel(m, s).at(Mathf.cosDeg(a) * 3.8f, Mathf.sinDeg(a) * 3.8f, deckZ + 1.6f).cyl(6, 0.26f, 0f, hz - 0.5f - deckZ - 1.6f);
            }
            vent(m, s, 0f, 9.6f, deckZ, 5.0f, 2.2f, 1.0f);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 0, deckZ + 2f).cyl(16, 5.0f, 0f, 8f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(spire, 0, 0, hz, 2, 0);
            orbitRest(m, prism, 3, 0, 0, hz - 0.5f, 3.8f, 30f);
        }

        @Override
        public void drawLive(Building b){
            float w = warm(b), a = total(b) * 2.6f, t = time(b);
            orbit(prism, b, 3, 0, 0, hz - 0.5f, 3.8f, a, true);
            lit(0.45f + 0.55f * w);
            draw(spire, b, 0, 0, hz, 2, -a * 0.3f);
            lit(1f);
            orbit(prism, b, 3, 0, 0, hz - 0.5f, 3.8f, a, false);
        }

        @Override
        public void drawOver(Building b){
            float w = warm(b), a = total(b) * 2.6f;
            light(b, 0, 0, hz + 1f, 4.2f, concord, w * 0.3f);
            for(int i = 0; i < 3; i++){
                float an = a + i * 120f;
                beam(b, Mathf.cosDeg(an) * 3.8f, Mathf.sinDeg(an) * 3.8f, hz - 0.5f, 0, 0, hz + 1.5f, 0.5f, lumen, w * 0.45f);
            }
        }

        @Override
        public void drawCheap(Building b){
            light(b, 0, 0, hz + 1f, 4.2f, concord, warm(b) * 0.4f);
        }
    }

    /** Allocation terminal (3x3): a requisition console with a rotating holographic manifest ring. */
    public static class Allocation extends KitModel{
        final Mesh seg = new Mesh(), core;
        final Color tone;
        static final float hz = 6.4f;

        public Allocation(String name, Color tone){
            super(name, 3, aurelia);
            this.tone = tone;
            seg.color(tone).style(emissive, 0).hexa(false, -1.3f, -0.1f, -0.5f, 1.3f, -0.1f, -0.5f, 1.3f, 0.1f, -0.5f, -1.3f, 0.1f, -0.5f,
                -1.3f, -0.1f, 0.5f, 1.3f, -0.1f, 0.5f, 1.3f, 0.1f, 0.5f, -1.3f, 0.1f, 0.5f);
            core = gem(tone, true, 0.9f, 1.8f);
        }

        @Override
        public void buildStatic(Mesh m){
            deck(m, s, half, deckZ);
            hull2(m, s).at(0, 1.2f, deckZ).lathe(8, 22.5f, 3.6f, 0f, 3.4f, 1.4f, 2.2f, 2.4f, 0.001f, 2.4f);
            m.color(tone).style(emissive, 0).at(0, 1.2f, 0).ring(16, 1.6f, 2.0f, deckZ + 2.4f, deckZ + 2.45f);
            hull(m, s).at(0, 0, 0).bevel(-9.6f, 6.0f, deckZ, 9.6f, 9.8f, deckZ + 4.4f, 0.3f);
            fins(m, s).box(-9.0f, 5.98f, deckZ + 0.8f, 9.0f, 6.0f, deckZ + 3.8f);
            for(int k = 0; k < 5; k++){
                m.color(k % 2 == 0 ? tone : s.glow).style(emissive, 0).box(-8.0f + k * 3.6f, 5.96f, deckZ + 3.2f, -6.4f + k * 3.6f, 5.98f, deckZ + 3.6f);
            }
            //v7.8: segment rail on three posts
            steel(m, s).at(0, 1.2f, 0).ring(16, 3.05f, 3.35f, hz - 0.16f, hz + 0.16f);
            for(int i = 0; i < 3; i++){
                float a = 30f + i * 120f;
                steel(m, s).at(Mathf.cosDeg(a) * 3.2f, 1.2f + Mathf.sinDeg(a) * 3.2f, deckZ + 2.2f).cyl(6, 0.24f, 0f, hz - deckZ - 2.2f);
            }
            console(m, s, -6.6f, -8.2f, deckZ);
            console(m, s, 6.6f, -8.2f, deckZ);
            hazard(m).at(0, 0, 0).box(-3f, -11.0f, deckZ, 3f, -10.4f, deckZ + 0.03f);
        }

        @Override
        public void buildEnvelope(Mesh m){
            m.at(0, 1.2f, hz - 2f).cyl(12, 4.0f, 0f, 4f);
        }

        @Override
        public void buildRest(Mesh m){
            m.add(core, 0, 1.2f, hz, 2, 0);
            orbitRest(m, seg, 5, 0, 1.2f, hz, 3.2f, 0f);
        }

        @Override
        public void drawLive(Building b){
            float w = warm(b), a = total(b) * 1.8f, t = time(b);
            Live.alpha = 0.85f;
            orbit(seg, b, 5, 0, 1.2f, hz, 3.2f, a, true);
            Live.alpha = 1f;
            lit(0.5f + 0.5f * w);
            draw(core, b, 0, 1.2f, hz, 2, t);
            lit(1f);
            Live.alpha = 0.85f;
            orbit(seg, b, 5, 0, 1.2f, hz, 3.2f, a, false);
            Live.alpha = 1f;
        }

        @Override
        public void drawOver(Building b){
            light(b, 0, 1.2f, hz, 3.6f, tone, 0.1f + warm(b) * 0.25f);
        }

        @Override
        public void drawCheap(Building b){
            drawOver(b);
        }
    }
}
