package outpost.gfx;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.gen.Building;

import static outpost.gfx.Mesh.*;

/** v2 coil winder: spinning spindle, tension arms, live wires, spool racks. */
public class CoilWinderModel extends FactoryModel{
    public static final Color amber = Color.valueOf("ffc35c");
    public static final CoilWinderModel instance = new CoilWinderModel();

    private final Mesh spindle = new Mesh(), arm = new Mesh(), coil = new Mesh(), unitWire = new Mesh();
    private static final float z = 2.1f;

    private CoilWinderModel(){
        super("coil-winder", amber);
        // rotating spindle: shaft + chuck + wound pack
        spindle.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cyl(16, 0.55f, 0f, 3.2f);
        spindle.color(Pal.steel).style(metal, 0).at(0, 0, 0).cyl(16, 1.1f, 0f, 0.4f);
        spindle.color(Pal.copper).style(metal, 0).at(0, 0, 0).cyl(16, 0.95f, 0.5f, 2.2f);
        for(int i = 0; i < 3; i++){
            spindle.color(Pal.dark).style(0, 0).at(0, 0, 0.7f + i * 0.55f).ring(16, 0.96f, 1.0f, 0f, 0.06f);
        }
        spindle.color(Pal.white).style(metal, 0).at(0, 0, 0).cyl(16, 1.25f, 2.3f, 2.6f);
        spindle.color(amber).style(emissive, 0).at(0, 0, 2.62f).ring(12, 0.4f, 0.6f, 0f, 0.08f);

        // tension arm: pivot boss + tapered arm + eyelet
        arm.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cyl(10, 0.5f, 0f, 0.6f);
        arm.color(Pal.white).style(metal, matPlate).at(0, 0, 0)
            .taper(0, 1.6f, 0.3f, 0.7f, 3.4f, 0.9f, 0.5f, 2.8f, 0, 0, false);
        arm.color(Pal.steel).style(metal, 0).at(0, 3.0f, 0.6f).ring(10, 0.28f, 0.45f, 0f, 0.2f);

        // finished flux coil: flanged copper pack with a glowing core
        coil.color(Pal.copper).style(metal, 0).at(0, 0, 0).cyl(14, 0.95f, 0f, 0.9f);
        coil.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cyl(14, 1.15f, -0.12f, 0f);
        coil.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cyl(14, 1.15f, 0.9f, 1.02f);
        coil.color(amber).style(emissive, 0).at(0, 0, 0.35f).cyl(10, 0.3f, 0f, 0.2f);

        unitWire.color(Pal.copper).style(metal, 0).at(0, 0, 0).box(-0.5f, -0.5f, 0f, 0.5f, 0.5f, 1f, true);
    }

    @Override
    public void buildStatic(Mesh m){
        deck(m);
        // central pedestal
        m.color(Pal.dark).style(metal, matGrate).at(0, 0, 0)
            .cbevel(0, 0.5f, deckZ, 6.4f, 6.0f, 0.35f, 0.15f);
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cyl(20, 2.1f, deckZ + 0.2f, z + 0.2f);
        m.color(Pal.white).style(metal, matPlate).at(0, 0, 0).cyl(20, 1.7f, z + 0.2f, z + 0.5f);
        m.color(amber).style(emissive, 0).at(0, 0, z + 0.52f).ring(20, 1.75f, 1.9f, 0, 0.05f);
        // 4 supply spools with wound copper
        float[][] spools = {{-6.9f, 4.2f}, {6.9f, 4.2f}, {-6.9f, -3.6f}, {6.9f, -3.6f}};
        for(float[] sp : spools){
            float x = sp[0], y = sp[1];
            m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbevel(x, y, deckZ, 2.6f, 2.2f, 0.4f, 0.1f);
            for(int s = -1; s <= 1; s += 2){
                m.color(Pal.offWhite).style(metal, 0).at(0, 0, 0)
                    .cbevel(x + s * 0.95f, y, deckZ + 0.4f, 0.35f, 1.6f, 2.4f, 0.06f);
            }
            m.color(Pal.steel).style(metal, 0).at(x, y, deckZ + 1.2f).rot(1, 90).cyl(8, 0.22f, -1.0f, 1.0f);
            m.color(Pal.copper).style(metal, 0).at(x, y, deckZ + 1.2f).rot(1, 90).cyl(12, 0.75f, -0.6f, 0.6f);
            m.color(Pal.darkSteel).style(metal, 0).at(x, y, deckZ + 1.2f).rot(1, 90).cyl(12, 0.95f, -0.75f, -0.6f);
            m.color(Pal.darkSteel).style(metal, 0).at(x, y, deckZ + 1.2f).rot(1, 90).cyl(12, 0.95f, 0.6f, 0.75f);
        }
        // wire guide posts between the north spools and the spindle
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.darkSteel).style(metal, 0).at(s * 3.6f, 5.6f, deckZ).cyl(8, 0.22f, 0f, 3.0f);
            m.color(Pal.steel).style(metal, 0).at(s * 3.6f, 5.6f, deckZ + 3.0f).ring(8, 0.25f, 0.4f, 0f, 0.18f);
        }
        // rear drive cabinet
        m.color(Pal.white).style(metal, matPlate).at(0, 0, 0)
            .cbevel(0, 8.6f, deckZ, 7.2f, 2.6f, 2.8f, 0.25f);
        m.color(Pal.dark).style(0, matFins).at(0, 0, 0)
            .cbox(0, 8.6f, deckZ + 2.8f, 5.6f, 1.8f, 0.08f);
        m.color(amber).style(emissive, 0).at(0, 0, 0)
            .box(-2.2f, 7.28f, deckZ + 1.6f, 2.2f, 7.34f, deckZ + 1.9f);
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
            .box(-0.8f, 3.2f, deckZ + 0.2f, 0.8f, 7.3f, deckZ + 0.9f);
        // output bin (south) with guide chute
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
            .bevel(-2.0f, -10.6f, deckZ, 2.0f, -5.6f, z + 0.4f, 0.2f);
        m.color(Pal.mid).style(metal, matGrate).at(0, 0, 0)
            .box(-1.5f, -10.2f, z + 0.4f, 1.5f, -6.0f, z + 0.52f);
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.steel).style(metal, 0).at(0, 0, 0)
                .box(s * 1.7f - 0.12f, -10.4f, z + 0.4f, s * 1.7f + 0.12f, -5.8f, z + 1.0f);
        }
        m.color(amber).style(emissive, 0).at(0, 0, 0)
            .cbox(0, -10.65f, z + 0.6f, 1.2f, 0.08f, 0.2f);
        console(m, 8.6f, -8.8f);
        m.color(Pal.yellow).style(0, matHazard).at(0, 0, 0)
            .box(-3.5f, -11.1f, deckZ + 0.02f, 3.5f, -10.5f, deckZ + 0.08f);
    }

    @Override
    public void buildEnvelope(Mesh m){
        m.at(0, 0, 0).box(-2.2f, -1.6f, z, 2.2f, 2.6f, z + 4.2f);
        m.at(0, 0, 0).box(-4.6f, 4.4f, deckZ + 2f, 4.6f, 9.4f, deckZ + 4.6f);
        m.at(0, 0, 0).box(-1.6f, -10.6f, z, 1.6f, -4.8f, z + 1.6f);
    }

    @Override
    public void buildRest(Mesh m){
        m.add(spindle, 0, 0.5f, z + 0.5f, 2, 20);
        m.add(arm, -3.6f, 5.6f, deckZ + 3.0f, 2, -12);
        m.add(arm, 3.6f, 5.6f, deckZ + 3.0f, 2, 12);
        m.add(coil, 0, -8.0f, z + 0.55f, 2, 0);
    }

    @Override
    public void drawLive(Building b){
        drawPose(b.x, b.y, warmup(b), progress(b), total(b) + b.id * 5f);
    }

    public void drawPose(float wx, float wy, float warm, float progress, float t){
        // spindle spins with production
        Live.pose(0, 0.5f, z + 0.5f, 2, t * 4.2f);
        Live.glowR = 0.5f + warm * 0.6f;
        Live.draw(spindle, cam, wx, wy);
        Live.resetTint();
        // tension arms breathe against the wire
        for(int s = -1; s <= 1; s += 2){
            float swing = s * (10f + 8f * Mathf.sinDeg(t * 3.1f + s * 40f)) * (0.25f + 0.75f * warm);
            Live.pose(s * 3.6f, 5.6f, deckZ + 3.0f, 2, swing);
            Live.draw(arm, cam, wx, wy);
            // wire: north spool -> arm eyelet -> spindle top
            float ex = s * 3.6f - 3.0f * Mathf.sinDeg(swing);
            float ey = 5.6f + 3.0f * Mathf.cosDeg(swing);
            float ez = deckZ + 3.6f;
            Live.link(s * 6.9f, 4.2f, deckZ + 1.9f, ex, ey, ez, 0.16f, 0.16f);
            Live.draw(unitWire, cam, wx, wy);
            Live.link(ex, ey, ez, 0, 0.5f, z + 2.6f, 0.14f, 0.14f);
            Live.draw(unitWire, cam, wx, wy);
        }
        // finished coil rides the chute at the end of each cycle
        float ride = smooth(0.68f, 0.95f, progress);
        float cy = -5.2f - ride * 4.4f;
        Live.alpha = warm > 0.08f ? (0.35f + 0.65f * smooth(0.6f, 0.72f, progress)) : 0.12f;
        Live.glowR = 0.5f + 0.5f * warm;
        Live.pose(0, cy, z + 0.55f, 2, t * 0.6f);
        Live.draw(coil, cam, wx, wy);
        Live.resetTint();
    }

    @Override
    public void drawOver(Building b){
        float w = warmup(b);
        if(w < 0.12f) return;
        float cx = b.x + cam.sx(0, z + 3.4f), cy = b.y + cam.sy(0.5f, z + 3.4f);
        Draw.blend(Blending.additive);
        Draw.color(amber, w * 0.4f * (0.7f + 0.3f * Mathf.absin(Time.time, 9f, 1f)));
        Lines.stroke(0.6f);
        Lines.circle(cx, cy, 2.1f);
        Fill.circle(cx, cy, 0.8f);
        Draw.blend();
        Draw.reset();
    }
}
