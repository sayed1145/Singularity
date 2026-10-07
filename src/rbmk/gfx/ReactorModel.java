package rbmk.gfx;

import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import rbmk.world.*;

import static rbmk.gfx.Mesh.*;

/**
 * RBMK plant, 8x8 tiles. Static: plinth, deck, biological-shield ring with bolted top, graphite well floor with
 * 709 channel sockets, four steam-separator drums on saddles, eight main circulation pumps, turbine-generator,
 * risers, downcomers, headers and auxiliary cabinets. Live: 709 channel columns, 8 pump rotors, turbine shaft
 * and couplings, 4 drum level columns and 16 status lamps.
 */
public class ReactorModel extends BlockModel{
    public static final ReactorModel instance = new ReactorModel();

    static final float deckZ = 2.0f, ringIn = 22.4f, ringOut = 25.2f, ringTop = 4.2f;
    static final float drumX = 28.4f, drumR = 1.9f, drumZ = 4.4f;
    static final float[] drumY0 = {-13.5f, 1.5f}, drumY1 = {-1.5f, 13.5f};
    public static final float[][] pumps = {
        {-28.4f, -18.2f}, {-28.4f, -24.6f}, {-22.6f, -29.0f}, {-16.2f, -29.0f},
        {28.4f, -18.2f}, {28.4f, -24.6f}, {22.6f, -29.0f}, {16.2f, -29.0f}
    };
    static final float pumpTop = 6.45f, turbY = -29.6f, turbZ = 4.1f;
    static final int lampCount = 16;
    static final float lampR = 23.85f;

    final Mesh rotor = new Mesh(), rotorRing = new Mesh(), shaft = new Mesh(), coupling = new Mesh(), lamp = new Mesh(), column = new Mesh();

    public ReactorModel(){
        super("rbmk-plant", 8);
        //pump rotor: hub + 6 pitched blades
        rotor.color(Pal.steel).style(metal, 0);
        rotor.at(0, 0, 0).cyl(10, 0.46f, 0f, 0.62f);
        rotor.color(Pal.darkSteel).at(0, 0, 0.62f).cyl(10, 0.26f, 0f, 0.16f);
        for(int i = 0; i < 6; i++){
            rotor.color(i % 2 == 0 ? Pal.offWhite : Pal.light).style(metal, 0);
            rotor.at(0, 0, 0.2f).rot(2, i * 60f).rot(0, 18f).box(0.35f, -0.17f, -0.06f, 1.28f, 0.17f, 0.08f, true);
        }
        rotorRing.color(Pal.glowWhite).style(emissive, 0).at(0, 0, 0).ring(14, 1.36f, 1.5f, 0f, 0.12f);
        //shaft along x with alternating facets so spin is visible
        for(int i = 0; i < 8; i++){
            shaft.color(i % 2 == 0 ? Pal.steel : Pal.darkSteel).style(metal, 0);
            float a0 = i * 45f, a1 = (i + 1) * 45f, r = 0.52f;
            float y0 = Mathf.cosDeg(a0) * r, z0 = Mathf.sinDeg(a0) * r, y1 = Mathf.cosDeg(a1) * r, z1 = Mathf.sinDeg(a1) * r;
            float my = Mathf.cosDeg(a0 + 22.5f), mz = Mathf.sinDeg(a0 + 22.5f);
            shaft.at(0, 0, 0).quad(-0.9f, y0, z0, 0.9f, y0, z0, 0.9f, y1, z1, -0.9f, y1, z1, 0, my, mz);
        }
        coupling.color(Pal.offWhite).style(metal, 0).at(-0.2f, 0, 0).rot(1, 90).cyl(12, 0.9f, 0f, 0.4f);
        for(int i = 0; i < 6; i++){
            coupling.color(Pal.dark).style(metal, 0).at(0, 0, 0).rot(0, i * 60f).box(-0.3f, 0.5f, -0.12f, 0.3f, 0.72f, 0.12f, true);
        }
        lamp.color(Pal.glowWhite).style(emissive, 0).at(0, 0, 0).cbox(0, 0, 0, 0.46f, 0.46f, 0.26f);
        column.color(Pal.glowWhite).style(emissive, 0).at(0, 0, 0).box(-0.5f, -0.5f, 0f, 0.5f, 0.5f, 1f, false);
    }

    // =================================================================== static (baked)

    @Override
    public void buildStatic(Mesh m){
        //plinth + deck
        m.color(Pal.concrete).style(0, matConcrete).at(0, 0, 0).bevel(-32, -32, 0, 32, 32, 0.9f, 0.35f);
        m.color(Pal.white).style(0, matPlate).bevel(-30.8f, -30.8f, 0.9f, 30.8f, 30.8f, deckZ, 0.4f);
        //hazard apron around the shield
        m.color(Pal.yellow).style(0, matHazard).lathe(96, 0, ringOut - 0.05f, deckZ, 26.1f, deckZ, 26.1f, deckZ + 0.04f, ringOut - 0.05f, deckZ + 0.04f, ringOut - 0.05f, deckZ);
        //biological shield ring with chamfers
        m.color(Pal.white).style(metal, matBolts).lathe(128, 0,
            ringIn, deckZ, ringOut, deckZ, ringOut, ringTop - 0.3f, ringOut - 0.3f, ringTop, ringIn + 0.5f, ringTop, ringIn, ringTop - 0.4f, ringIn, deckZ);
        m.color(Pal.red).style(0, matPlain).lathe(128, 0, ringOut - 0.02f, 2.85f, ringOut + 0.06f, 2.85f, ringOut + 0.06f, 3.2f, ringOut - 0.02f, 3.2f, ringOut - 0.02f, 2.85f);
        //graphite well floor with sockets (procedural)
        m.color(Pal.graphite).style(0, matSocket).lathe(96, 0, 0, deckZ, ringIn, deckZ, ringIn, RodField.floorZ, 0, RodField.floorZ);
        //lamp sockets on the ring
        for(int i = 0; i < lampCount; i++){
            float a = 11.25f + i * 22.5f;
            m.color(Pal.dark).style(metal, 0).at(Mathf.cosDeg(a) * lampR, Mathf.sinDeg(a) * lampR, ringTop).cyl(10, 0.42f, 0f, 0.08f);
        }

        //steam separator drums on saddles, with bands and level-gauge frames
        for(int sx = -1; sx <= 1; sx += 2){
            float x = sx * drumX;
            for(int d = 0; d < 2; d++){
                float y0 = drumY0[d], y1 = drumY1[d], len = y1 - y0;
                for(int s = 0; s < 2; s++){
                    float y = y0 + len * (s == 0 ? 0.25f : 0.75f);
                    m.color(Pal.mid).style(0, matPlate).at(0, 0, 0).cbevel(x, y, deckZ, 3.2f, 0.9f, drumZ - deckZ - 0.9f, 0.15f);
                }
                m.color(Pal.white).style(metal, matPlate).at(x, y0, drumZ).rot(0, -90).lathe(28, 0,
                    0, 0, 1.15f, 0.12f, 1.65f, 0.5f, drumR, 1.1f, drumR, len - 1.1f, 1.65f, len - 0.5f, 1.15f, len - 0.12f, 0, len);
                for(int b = 1; b <= 2; b++){
                    float z0 = len * b / 3f;
                    m.color(Pal.darkSteel).style(metal, 0).at(x, y0, drumZ).rot(0, -90).lathe(28, 0, drumR - 0.02f, z0 - 0.18f, drumR + 0.07f, z0 - 0.18f, drumR + 0.07f, z0 + 0.18f, drumR - 0.02f, z0 + 0.18f, drumR - 0.02f, z0 - 0.18f);
                }
                //level gauge: back plate + side rails, open front (the live column sits in front of the plate)
                float gy = y0 - 1.05f;
                m.color(Pal.dark).style(metal, 0).at(0, 0, 0).box(x - 0.45f, gy + 0.05f, deckZ, x + 0.45f, gy + 0.35f, deckZ + 4.6f);
                m.color(Pal.steel).style(metal, 0).box(x - 0.55f, gy - 0.35f, deckZ, x - 0.35f, gy + 0.35f, deckZ + 4.7f);
                m.box(x + 0.35f, gy - 0.35f, deckZ, x + 0.55f, gy + 0.35f, deckZ + 4.7f);
                m.color(Pal.offWhite).cbox(x, gy, deckZ + 4.6f, 1.2f, 0.8f, 0.25f);
                m.color(Pal.mid).cbox(x, gy, deckZ, 1.3f, 0.9f, 0.3f);
                //risers from the shield to the drum
                for(int r = 0; r < 3; r++){
                    float yy = y0 + 1.8f + r * (len - 3.6f) / 2f;
                    float xr = (float)Math.sqrt(Math.max(0f, (ringOut - 0.3f) * (ringOut - 0.3f) - yy * yy));
                    if(xr < 18f) continue;
                    m.color(Pal.steel).style(metal, 0).pipe(8, 0.2f, sx * (xr - 0.2f), yy, ringTop - 0.05f, sx * (drumX - 1.2f), yy, drumZ + 1.2f);
                }
                //downcomer to the pumps (low, along the deck)
                m.color(Pal.offWhite).style(metal, 0).pipe(10, 0.42f, x, y0 + 0.4f, deckZ + 0.55f, x, y0 - 2.0f, deckZ + 0.55f);
            }
            //steam lines north to the header
            m.color(Pal.offWhite).style(metal, 0).pipe(10, 0.45f, x, 13.0f, deckZ + 0.5f, x, 28.4f, deckZ + 0.5f);
        }
        //headers along the north edge
        m.color(Pal.offWhite).style(metal, 0).pipe(12, 0.48f, -29f, 27.8f, deckZ + 0.5f, 29f, 27.8f, deckZ + 0.5f);
        m.color(Pal.light).style(metal, 0).pipe(12, 0.36f, -26f, 29.1f, deckZ + 0.38f, 26f, 29.1f, deckZ + 0.38f);
        for(int i = -3; i <= 3; i++){
            m.color(Pal.mid).style(0, 0).at(0, 0, 0).cbox(i * 8f, 28.45f, deckZ, 0.9f, 2.6f, 0.35f);
        }

        //main circulation pumps
        for(int i = 0; i < pumps.length; i++){
            float x = pumps[i][0], y = pumps[i][1];
            m.color(Pal.mid).style(0, matPlate).at(0, 0, 0).cbevel(x, y, deckZ, 4.9f, 4.9f, 0.3f, 0.12f);
            m.color(Pal.white).style(metal, 0).at(x, y, 0).lathe(24, 7.5f,
                0, deckZ + 0.3f, 2.15f, deckZ + 0.3f, 2.15f, deckZ + 0.75f, 1.9f, deckZ + 0.9f, 1.9f, 4.3f, 1.7f, 4.5f, 1.45f, 4.5f, 1.45f, 6.15f, 1.3f, pumpTop, 0, pumpTop);
            m.color(Pal.light).style(0, matFins).at(x, y, 0).lathe(24, 7.5f, 1.46f, 4.6f, 1.54f, 4.6f, 1.54f, 6.0f, 1.46f, 6.0f, 1.46f, 4.6f);
            m.color(Pal.dark).style(metal, 0).at(x, y, 0).lathe(20, 0, 1.36f, pumpTop - 0.02f, 1.55f, pumpTop - 0.02f, 1.55f, pumpTop + 0.02f, 1.36f, pumpTop + 0.02f, 1.36f, pumpTop - 0.02f);
            //discharge into the shield
            float ang = (float)Math.atan2(y, x);
            float ex = (float)Math.cos(ang) * (ringOut - 0.2f), ey = (float)Math.sin(ang) * (ringOut - 0.2f);
            float sx0 = x - (float)Math.cos(ang) * 1.7f, sy0 = y - (float)Math.sin(ang) * 1.7f;
            m.color(Pal.offWhite).style(metal, 0).pipe(10, 0.4f, sx0, sy0, 3.3f, ex, ey, 3.3f);
            //flange collar
            m.color(Pal.darkSteel).pipe(10, 0.52f, sx0 - (float)Math.cos(ang) * 0.2f, sy0 - (float)Math.sin(ang) * 0.2f, 3.3f, sx0 + (float)Math.cos(ang) * 0.5f, sy0 + (float)Math.sin(ang) * 0.5f, 3.3f);
        }

        //turbine-generator
        m.color(Pal.mid).style(0, matPlate).at(0, 0, 0).cbevel(0, turbY, deckZ, 25f, 3.9f, 0.7f, 0.2f);
        m.color(Pal.white).style(metal, matPlate).at(-11f, turbY, turbZ).rot(1, 90).lathe(24, 0,
            0, 0, 0.9f, 0, 1.35f, 0.45f, 1.5f, 1.4f, 1.6f, 6.0f, 1.3f, 6.8f, 0, 6.8f);
        m.color(Pal.white).style(metal, matPlate).at(-3f, turbY, turbZ).rot(1, 90).lathe(28, 0,
            0, 0, 1.3f, 0, 1.85f, 0.6f, 1.95f, 3.4f, 1.85f, 6.2f, 1.3f, 6.8f, 0, 6.8f);
        m.color(Pal.red).style(0, 0).at(-3f, turbY, turbZ).rot(1, 90).lathe(28, 0, 1.9f, 3.2f, 2.0f, 3.2f, 2.0f, 3.6f, 1.9f, 3.6f, 1.9f, 3.2f);
        m.color(Pal.white).style(metal, matPlate).at(0, 0, 0).cbevel(8f, turbY, deckZ + 0.7f, 6.1f, 3.2f, 2.8f, 0.35f);
        m.color(Pal.light).style(0, matFins).cbox(8f, turbY, deckZ + 3.5f, 4.6f, 2.4f, 0.12f);
        m.color(Pal.red).style(0, 0).cbox(8f, turbY - 1.61f, deckZ + 2.35f, 6.1f, 0.02f, 0.35f);
        m.color(Pal.darkSteel).style(metal, 0).at(11.05f, turbY, turbZ).rot(1, 90).cyl(16, 0.8f, 0f, 0.9f);
        //bearing pedestals
        for(float bx : new float[]{-11.6f, -3.6f, 4.4f}){
            m.color(Pal.dark).style(metal, 0).at(0, 0, 0).cbevel(bx, turbY, deckZ + 0.7f, 1.1f, 1.8f, 0.8f, 0.12f);
        }

        //auxiliary equipment in the north corners (kept low: they sit at the back of the footprint)
        m.color(Pal.offWhite).style(0, matFins).at(0, 0, 0).cbevel(-26.5f, 24.8f, deckZ, 6.2f, 4.4f, 1.9f, 0.3f);
        m.color(Pal.dark).style(0, matGrate).cbox(-26.5f, 24.8f, deckZ + 1.9f, 4.8f, 3.0f, 0.06f);
        m.color(Pal.red).style(0, 0).cbox(-29.2f, 22.55f, deckZ + 0.4f, 0.5f, 0.05f, 1.0f);
        m.color(Pal.offWhite).style(0, matPlate).cbevel(26.5f, 24.8f, deckZ, 6.2f, 4.4f, 1.6f, 0.3f);
        for(int i = 0; i < 2; i++){
            m.color(Pal.mid).style(metal, matGrate).at(24.9f + i * 3.2f, 24.8f, deckZ + 1.6f).cyl(16, 1.25f, 0f, 0.12f);
            m.color(Pal.dark).style(metal, 0).at(24.9f + i * 3.2f, 24.8f, deckZ + 1.6f).cyl(8, 0.3f, 0f, 0.22f);
        }
        //south-west / south-east corner service hatches
        m.color(Pal.light).style(0, matGrate).at(0, 0, 0).cbox(-29.6f, -29.7f, deckZ, 2.2f, 2.2f, 0.08f);
        m.cbox(29.6f, -29.7f, deckZ, 2.2f, 2.2f, 0.08f);
    }

    @Override
    public boolean pattern(int mat, float x, float y, float z, float nz, float[] rgb){
        if(mat == matSocket && nz > 0.7f){
            int i = Math.round(x / RodField.pitch), j = Math.round(y / RodField.pitch);
            float k;
            if(i * i + j * j <= 225){
                float dx = Math.abs(x - i * RodField.pitch), dy = Math.abs(y - j * RodField.pitch), d = Math.max(dx, dy);
                k = d < 0.52f ? 0.45f : d < 0.60f ? 2.3f : d < 0.66f ? 1.2f : 0.85f;
            }else{
                k = (Math.abs(x % 2f) < 0.06f || Math.abs(y % 2f) < 0.06f) ? 0.7f : 1f;
            }
            rgb[0] *= k; rgb[1] *= k; rgb[2] *= k;
            return true;
        }
        if(mat == matBolts && nz > 0.7f){
            float r = (float)Math.hypot(x, y);
            if(Math.abs(r - 24.55f) < 0.4f){
                float a = (float)Math.toDegrees(Math.atan2(y, x));
                float step = 3.75f;
                float da = (a - Math.round(a / step) * step) * (float)Math.PI / 180f * r;
                float d = (float)Math.hypot(da, r - 24.55f);
                float k = d < 0.13f ? 1.12f : d < 0.2f ? 0.62f : 1f;
                rgb[0] *= k; rgb[1] *= k; rgb[2] *= k;
            }else if(Math.abs(r - 23.15f) < 0.05f){
                rgb[0] *= 0.8f; rgb[1] *= 0.8f; rgb[2] *= 0.8f;
            }
            return true;
        }
        return false;
    }

    @Override
    public void buildEnvelope(Mesh m){
        RodField.addEnvelope(m, cam);
        for(float[] p : pumps) m.at(p[0], p[1], pumpTop - 0.05f).cyl(16, 1.6f, 0f, 1.2f);
        m.at(0, 0, 0).box(-12.4f, turbY - 1f, turbZ - 1f, -10.7f, turbY + 1f, turbZ + 1f);
        m.box(-4.6f, turbY - 1f, turbZ - 1f, -2.6f, turbY + 1f, turbZ + 1f);
        m.box(3.4f, turbY - 1f, turbZ - 1f, 5.4f, turbY + 1f, turbZ + 1f);
        for(int sx = -1; sx <= 1; sx += 2){
            for(int d = 0; d < 2; d++){
                float gy = drumY0[d] - 1.05f;
                m.box(sx * drumX - 0.3f, gy - 0.25f, deckZ + 0.3f, sx * drumX + 0.3f, gy + 0.05f, deckZ + 4.4f);
            }
        }
        for(int i = 0; i < lampCount; i++){
            float a = 11.25f + i * 22.5f;
            m.at(Mathf.cosDeg(a) * lampR, Mathf.sinDeg(a) * lampR, ringTop).cbox(0, 0, 0, 0.5f, 0.5f, 0.4f);
        }
    }

    @Override
    public void buildRest(Mesh m){
        RodField.addRest(m, cam);
        for(float[] p : pumps){
            m.add(rotor, p[0], p[1], pumpTop, 2, 15f);
        }
        m.add(shaft, -11.5f, turbY, turbZ, 0, 10f);
        m.add(shaft, -3.6f, turbY, turbZ, 0, 10f);
        m.add(shaft, 4.4f, turbY, turbZ, 0, 10f);
        m.add(coupling, -3.6f, turbY, turbZ, 0, 10f);
        m.add(coupling, 4.4f, turbY, turbZ, 0, 10f);
    }

    // =================================================================== live

    @Override
    public void drawLive(Building build){
        RbmkReactor.RbmkBuild b = (RbmkReactor.RbmkBuild)build;
        RodField.init(cam);
        float wx = b.x, wy = b.y, t = Time.time;
        float ppu = pixelsPerUnit();
        int lod = ppu >= 2.6f ? 2 : ppu >= 1.5f ? 1 : 0;
        float danger = Mathf.clamp(b.dangerTime / 60f);

        drawLamps(b, wx, wy, t, true);

        RodField.simulate(b.rodActual, b.powerLevel, b.voidFraction, t);
        int sel = mindustry.Vars.control != null && mindustry.Vars.control.input != null && mindustry.Vars.control.input.config.isShown()
            && mindustry.Vars.control.input.config.getSelected() == b ? b.selectedRod : -1;
        RodField.draw(cam, wx, wy, sel, danger, t, lod);

        //drum level columns
        for(int sx = -1; sx <= 1; sx += 2){
            for(int d = 0; d < 2; d++){
                float gy = drumY0[d] - 1.05f, x = sx * drumX;
                float lvl = Mathf.clamp(b.drumLevel + Mathf.sin(t * 0.07f + d * 2f + sx) * 0.012f);
                boolean bad = b.drumLevel < 0.2f || b.drumLevel > 0.85f;
                Live.glowR = bad ? 1f : 0.45f; Live.glowG = bad ? 0.35f : 0.86f; Live.glowB = bad ? 0.3f : 1f;
                Live.link(x, gy - 0.1f, deckZ + 0.35f, x, gy - 0.1f, deckZ + 0.35f + 0.05f + lvl * 3.9f, 0.3f, 0.46f);
                Live.draw(column, cam, wx, wy);
            }
        }

        //pumps: status ring (running = cyan, stopped = dim) and rotor
        for(int i = 0; i < pumps.length; i++){
            float x = pumps[i][0], y = pumps[i][1], sp = b.pumpSet[i] / 100f;
            Live.glowR = Mathf.lerp(0.25f, 0.45f, sp); Live.glowG = Mathf.lerp(0.28f, 0.9f, sp); Live.glowB = Mathf.lerp(0.3f, 1f, sp);
            Live.pose(x, y, pumpTop, 2, 0f);
            Live.draw(rotorRing, cam, wx, wy);
            Live.pose(x, y, pumpTop, 2, b.pumpPhase[i]);
            Live.draw(rotor, cam, wx, wy);
        }

        //turbine shaft and couplings, drawn west to east
        float ta = b.turbinePhase;
        Live.pose(-11.5f, turbY, turbZ, 0, ta); Live.draw(shaft, cam, wx, wy);
        Live.pose(-3.6f, turbY, turbZ, 0, ta); Live.draw(shaft, cam, wx, wy);
        Live.pose(-3.6f, turbY, turbZ, 0, ta); Live.draw(coupling, cam, wx, wy);
        Live.pose(4.4f, turbY, turbZ, 0, ta); Live.draw(shaft, cam, wx, wy);
        Live.pose(4.4f, turbY, turbZ, 0, ta); Live.draw(coupling, cam, wx, wy);

        drawLamps(b, wx, wy, t, false);
    }

    void drawLamps(RbmkReactor.RbmkBuild b, float wx, float wy, float t, boolean north){
        float r, g, bl;
        boolean blink = Mathf.sin(t * 0.25f) > 0f;
        if(b.dangerTime > 0f){ r = blink ? 1f : 0.35f; g = blink ? 0.22f : 0.08f; bl = 0.1f; }
        else if(b.scrammed){ r = 1f; g = 0.68f; bl = 0.2f; }
        else if(b.productionEfficiency > 0.01f){ r = 0.45f; g = 1f; bl = 0.62f; }
        else{ r = 0.32f; g = 0.38f; bl = 0.4f; }
        for(int i = 0; i < lampCount; i++){
            float a = 11.25f + i * 22.5f;
            float y = Mathf.sinDeg(a) * lampR;
            if((y > 0f) != north) continue;
            //a slow chase makes the ring read as alive even at idle
            float chase = 0.78f + 0.22f * Mathf.sin(t * 0.05f - i * 0.8f);
            Live.glowR = r * chase; Live.glowG = g * chase; Live.glowB = bl * chase;
            Live.pose(Mathf.cosDeg(a) * lampR, y, ringTop + 0.08f, 2, a);
            Live.draw(lamp, cam, wx, wy);
        }
    }
}
