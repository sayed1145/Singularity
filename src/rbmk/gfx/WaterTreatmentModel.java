package rbmk.gfx;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;

import static rbmk.gfx.Mesh.*;

/**
 * Water treatment: a circular clarifier with a rotating scraper bridge, two ion-exchange columns whose sight
 * glasses show the resin bed level cycling with bubbles rising through it, and a feed pump whose fan spins.
 */
public class WaterTreatmentModel extends FactoryModel{
    public static final WaterTreatmentModel instance = new WaterTreatmentModel();
    static final float tcx = -3.4f, tcy = -2.6f, tankIn = 5.0f, tankOut = 5.6f, wallTop = 3.4f, waterZ = 2.9f;
    static final float colX = 6.8f, colR = 1.7f, colTop = 8.0f;
    static final float[] colY = {1.2f, -5.0f};
    static final float fanX = -7.0f, fanY = 7.4f, fanZ = 2.4f;

    final Mesh arm = new Mesh(), scraper = new Mesh(), ripples = new Mesh(), unitBox = new Mesh(), bubble = new Mesh(), fan = new Mesh();

    public WaterTreatmentModel(){
        super("water-treatment", new Color(0.36f, 0.80f, 0.95f));
        waterCx = tcx; waterCy = tcy;
        //bridge: truss, handrail, rim carriage and drive head (rotates about the pier)
        arm.color(Pal.white).style(metal, matGrate).at(0, 0, 0).bevel(0.5f, -0.38f, 3.75f, 5.45f, 0.38f, 4.1f, 0.06f);
        arm.color(accent).style(0, 0).box(0.8f, 0.34f, 4.1f, 5.2f, 0.42f, 4.2f);
        arm.color(Pal.steel).style(metal, 0).box(0.9f, 0.3f, 4.2f, 5.2f, 0.38f, 4.62f);
        for(int i = 0; i < 4; i++) arm.color(Pal.steel).style(metal, 0).at(0, 0, 0).box(1.2f + i * 1.3f, 0.28f, 4.1f, 1.32f + i * 1.3f, 0.4f, 4.62f);
        arm.color(Pal.offWhite).style(metal, 0).at(0, 0, 0).bevel(4.95f, -0.55f, wallTop, 5.75f, 0.55f, 3.8f, 0.08f);
        arm.color(Pal.white).style(metal, 0).at(0, 0, 3.7f).lathe(16, 0, 0, 0, 0.95f, 0, 0.95f, 0.6f, 0.7f, 0.8f, 0, 0.8f);
        arm.color(accent).style(0, 0).at(0, 0, 4.5f).cyl(12, 0.42f, 0f, 0.1f);
        scraper.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).box(1.0f, -0.08f, waterZ + 0.05f, 4.8f, 0.06f, 3.75f, false);
        for(int i = 0; i < 3; i++) scraper.color(Pal.steel).style(metal, 0).at(0, 0, 0).box(1.4f + i * 1.5f, -0.05f, 3.2f, 1.55f + i * 1.5f, 0.05f, 3.75f, false);
        //faint moving highlight arcs on the water
        for(int k = 0; k < 7; k++){
            float r = 1.4f + k * 0.52f, a0 = k * 83f;
            for(int s = 0; s < 4; s++){
                float a = a0 + s * 9f, b = a + 9f;
                ripples.color(Pal.glowWhite).style(emissive, 0).at(0, 0, 0).quad(
                    Mathf.cosDeg(a) * r, Mathf.sinDeg(a) * r, 0, Mathf.cosDeg(b) * r, Mathf.sinDeg(b) * r, 0,
                    Mathf.cosDeg(b) * (r + 0.09f), Mathf.sinDeg(b) * (r + 0.09f), 0, Mathf.cosDeg(a) * (r + 0.09f), Mathf.sinDeg(a) * (r + 0.09f), 0, 0, 0, 1);
            }
        }
        unitBox.color(Pal.glowWhite).style(emissive, 0).at(0, 0, 0).box(-0.5f, -0.5f, 0f, 0.5f, 0.5f, 1f, false);
        bubble.color(Pal.glowWhite).style(emissive, 0).at(0, 0, 0).box(-0.06f, -0.02f, -0.06f, 0.06f, 0.0f, 0.06f, false);
        //fan along x, facing east
        fan.color(Pal.dark).style(metal, 0).at(0, 0, 0).rot(1, 90).cyl(10, 0.22f, 0f, 0.3f);
        for(int i = 0; i < 5; i++){
            fan.color(i == 0 ? accent : Pal.offWhite).style(metal, 0).at(0.12f, 0, 0).rot(0, i * 72f).rot(2, 22f).box(-0.04f, 0.18f, -0.14f, 0.04f, 0.66f, 0.14f, true);
        }
    }

    @Override
    public void buildStatic(Mesh m){
        deck(m);
        //clarifier
        m.color(Pal.white).style(metal, matPlate).at(tcx, tcy, 0).lathe(56, 0,
            tankIn, deckZ, tankOut, deckZ, tankOut, wallTop - 0.15f, tankOut - 0.15f, wallTop, tankIn, wallTop, tankIn, deckZ);
        m.color(accent).style(0, 0).at(tcx, tcy, 0).lathe(56, 0, tankOut - 0.02f, 2.2f, tankOut + 0.05f, 2.2f, tankOut + 0.05f, 2.45f, tankOut - 0.02f, 2.45f, tankOut - 0.02f, 2.2f);
        m.color(Pal.water).style(glass, matWater).at(tcx, tcy, 0).lathe(48, 0, 0, deckZ, tankIn, deckZ, tankIn, waterZ, 0, waterZ);
        m.color(Pal.offWhite).style(metal, 0).at(tcx, tcy, 0).cyl(14, 0.55f, deckZ, 3.7f);
        m.color(Pal.darkSteel).style(metal, 0).at(tcx, tcy, 0).lathe(56, 0, tankIn - 0.35f, waterZ, tankIn, waterZ, tankIn, waterZ + 0.25f, tankIn - 0.35f, waterZ + 0.25f, tankIn - 0.35f, waterZ);
        //ion exchange columns with sight-glass frames (open front; the live level sits in front of the back strip)
        for(float cy : colY){
            m.color(Pal.mid).style(metal, 0).at(0, 0, 0).cbevel(colX, cy, deckZ, 3.9f, 3.9f, 0.35f, 0.1f);
            m.color(Pal.white).style(metal, matPlate).at(colX, cy, 0).lathe(32, 0,
                0, deckZ + 0.35f, colR, deckZ + 0.35f, colR, colTop, colR * 0.8f, colTop + 0.45f, colR * 0.4f, colTop + 0.75f, 0, colTop + 0.8f);
            for(float bz : new float[]{2.3f, 7.6f}) m.color(accent).style(0, 0).at(colX, cy, 0).lathe(32, 0, colR - 0.02f, bz, colR + 0.06f, bz, colR + 0.06f, bz + 0.22f, colR - 0.02f, bz + 0.22f, colR - 0.02f, bz);
            float fy = cy - colR;
            m.color(Pal.graphite).style(0, 0).at(0, 0, 0).box(colX - 0.28f, fy - 0.06f, 2.6f, colX + 0.28f, fy + 0.2f, 7.3f);
            m.color(Pal.steel).style(metal, 0).box(colX - 0.42f, fy - 0.2f, 2.5f, colX - 0.28f, fy + 0.2f, 7.4f);
            m.box(colX + 0.28f, fy - 0.2f, 2.5f, colX + 0.42f, fy + 0.2f, 7.4f);
            m.color(Pal.offWhite).style(metal, 0).cbox(colX, fy, 7.4f, 0.95f, 0.45f, 0.18f);
            m.cbox(colX, fy, 2.35f, 0.95f, 0.45f, 0.18f);
            m.color(Pal.steel).style(metal, 0).pipe(10, 0.3f, tcx + tankOut - 0.1f, tcy, 2.3f, colX - colR + 0.1f, cy, 2.3f);
            m.color(Pal.offWhite).style(metal, 0).at(colX, cy, colTop + 0.8f).cyl(10, 0.3f, -0.1f, 0.35f);
        }
        m.color(Pal.offWhite).style(metal, 0).pipe(10, 0.28f, colX, colY[1], colTop + 1.0f, colX, colY[0], colTop + 1.0f);
        m.color(Pal.offWhite).style(metal, 0).pipe(10, 0.28f, colX + 0.3f, colY[0] + 0.3f, colTop + 1.0f, colX + 2.2f, 5.4f, 3.2f);
        //product tank
        m.color(Pal.white).style(metal, matPlate).at(8.6f, 7.0f, 0).lathe(28, 0, 0, deckZ, 1.7f, deckZ, 1.7f, 3.4f, 1.4f, 3.7f, 0, 3.7f);
        m.color(accent).style(0, 0).at(8.6f, 7.0f, 0).lathe(28, 0, 1.68f, 2.6f, 1.74f, 2.6f, 1.74f, 2.85f, 1.68f, 2.85f, 1.68f, 2.6f);
        //feed pump + inflow pipe
        m.color(Pal.mid).style(metal, matPlate).at(0, 0, 0).cbevel(-8.8f, fanY, deckZ, 4.6f, 2.2f, 0.3f, 0.08f);
        m.color(Pal.white).style(metal, matFins).at(-10.6f, fanY, fanZ).rot(1, 90).lathe(20, 0, 0, 0, 0.75f, 0, 0.82f, 0.2f, 0.82f, 3.2f, 0.7f, 3.4f, 0, 3.4f);
        m.color(Pal.dark).style(metal, 0).at(fanX - 0.2f, fanY, fanZ).rot(1, 90).ring(20, 0.72f, 0.8f, 0f, 0.35f);
        m.color(Pal.offWhite).style(metal, 0).pipe(10, 0.34f, -10.4f, fanY - 0.4f, 2.0f, tcx - 1.0f, tcy + tankOut - 0.1f, 2.0f);
        //chemical drums
        for(int i = 0; i < 2; i++){
            m.color(i == 0 ? Pal.yellow : Pal.cyan).style(0, 0).at(0.4f + i * 1.8f, 7.6f, 0).lathe(16, 0, 0, deckZ, 0.72f, deckZ, 0.72f, 2.75f, 0.62f, 2.85f, 0, 2.85f);
            m.color(Pal.dark).style(0, 0).at(0.4f + i * 1.8f, 7.6f, 0).lathe(16, 0, 0.73f, 1.9f, 0.76f, 1.9f, 0.76f, 2.0f, 0.73f, 2.0f, 0.73f, 1.9f);
        }
        console(m, -8.4f, -9.6f);
    }

    @Override
    public void buildEnvelope(Mesh m){
        m.at(tcx, tcy, waterZ - 0.02f).cyl(32, tankIn - 0.2f, 0f, 3.8f - waterZ);
        for(float cy : colY) m.at(0, 0, 0).box(colX - 0.26f, cy - colR - 0.2f, 2.55f, colX + 0.26f, cy - colR - 0.02f, 7.35f);
        m.at(fanX - 0.1f, fanY, fanZ).rot(1, 90).cyl(12, 0.75f, 0f, 0.5f);
    }

    @Override
    public void buildRest(Mesh m){
        m.add(arm, tcx, tcy, 0, 2, 35f);
        m.add(scraper, tcx, tcy, 0, 2, 35f);
        m.add(fan, fanX, fanY, fanZ, 0, 0f);
    }

    @Override
    public void drawLive(Building b){
        float wx = b.x, wy = b.y, w = warmup(b), tp = total(b), t = Time.time;
        float rot = tp * 0.45f;
        //water highlights
        Live.alpha = 0.16f + 0.1f * w;
        Live.pose(tcx, tcy, waterZ + 0.01f, 2, -t * 0.12f - tp * 0.3f);
        Live.draw(ripples, cam, wx, wy);
        Live.alpha = 1f;
        Live.pose(tcx, tcy, 0, 2, rot);
        Live.draw(scraper, cam, wx, wy);
        //resin bed / level in each column, cycling out of phase, with bubbles
        for(int c = 0; c < 2; c++){
            float cy = colY[c], fy = cy - colR - 0.12f;
            float lvl = 0.35f + 0.25f * w + 0.18f * Mathf.sin(tp * 0.02f + c * 3.14f);
            float top = 2.6f + 4.6f * Mathf.clamp(lvl);
            Live.glowR = 0.30f; Live.glowG = 0.72f; Live.glowB = 0.95f;
            Live.link(colX, fy, 2.6f, colX, fy, top, 0.1f, 0.44f);
            Live.draw(unitBox, cam, wx, wy);
            Live.glowR = Live.glowG = Live.glowB = 1f;
            for(int k = 0; k < 4; k++){
                float u = (t * (0.006f + 0.002f * k) * (0.3f + w) + k * 0.27f + c * 0.5f) % 1f;
                float z = 2.7f + u * (top - 2.8f);
                Live.pose(colX + Mathf.sin(u * 20f + k) * 0.1f, fy - 0.06f, z, 2, 0f);
                Live.draw(bubble, cam, wx, wy);
            }
        }
        //feed pump fan
        Live.pose(fanX, fanY, fanZ, 0, t * 6f * (0.25f + w));
        Live.draw(fan, cam, wx, wy);
    }

    @Override
    public void drawOver(Building b){
        Live.pose(tcx, tcy, 0, 2, total(b) * 0.45f);
        Live.draw(arm, cam, b.x, b.y);
    }
}
