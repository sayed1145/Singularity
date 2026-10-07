package rbmk.gfx;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;

import static rbmk.gfx.Mesh.*;

/**
 * Pellet plant: rotary tablet press. A 12-station turret rotates under a press frame; every upper punch rides a
 * cam track and dives when it passes under the crossbeam; finished pellets slide down a chute into the tray;
 * the sinter furnace window breathes with the machine's warm-up.
 */
public class PelletPlantModel extends FactoryModel{
    public static final PelletPlantModel instance = new PelletPlantModel();
    static final float tx = 0f, ty = -0.8f, tableTop = 2.5f, punchR = 2.75f;
    static final int stations = 12;

    final Mesh turret = new Mesh(), punch = new Mesh(), pellet = new Mesh(), glow = new Mesh();
    final float[] pz = new float[stations], py = new float[stations], pa = new float[stations];
    final int[] porder = new int[stations];

    public PelletPlantModel(){
        super("pellet-plant", new Color(0.72f, 0.80f, 0.36f));
        turret.color(Pal.steel).style(metal, 0).at(0, 0, 0).lathe(32, 0, 0, 0, 3.6f, 0, 3.6f, 0.45f, 3.4f, 0.62f, 0, 0.62f);
        turret.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).lathe(32, 0, 1.1f, 0.62f, 1.9f, 0.62f, 1.9f, 0.7f, 1.1f, 0.7f, 1.1f, 0.62f);
        for(int i = 0; i < stations; i++){
            float a = i * 30f;
            turret.color(Pal.graphite).style(0, 0).at(Mathf.cosDeg(a) * punchR, Mathf.sinDeg(a) * punchR, 0.62f).cyl(8, 0.36f, 0f, 0.02f);
        }
        turret.color(Pal.offWhite).style(metal, 0).at(0, 0, 0.62f).cyl(12, 0.8f, 0f, 0.5f);
        turret.color(accent).style(0, 0).at(0, 0, 1.12f).cyl(12, 0.45f, 0f, 0.12f);
        punch.color(Pal.steel).style(metal, 0).at(0, 0, 0).cyl(8, 0.3f, 0f, 2.1f);
        punch.color(Pal.offWhite).style(metal, 0).at(0, 0, 2.1f).cyl(10, 0.46f, 0f, 0.28f);
        punch.color(accent).style(0, 0).at(0, 0, 1.2f).cyl(8, 0.34f, 0f, 0.12f);
        pellet.color(Pal.pellet).style(0, 0).at(0, 0, 0).cyl(8, 0.34f, 0f, 0.36f);
        glow.color(Pal.glowWhite).style(emissive, 0).at(0, 0, 0).box(-1.3f, -0.03f, 0f, 1.3f, 0.0f, 0.95f, false);
    }

    @Override
    public void buildStatic(Mesh m){
        deck(m);
        //press table
        m.color(Pal.mid).style(metal, matPlate).at(tx, ty, 0).lathe(32, 0, 0, deckZ, 4.4f, deckZ, 4.4f, 2.2f, 4.1f, tableTop - 0.05f, 0, tableTop - 0.05f);
        m.color(accent).style(0, 0).at(tx, ty, 0).lathe(32, 0, 4.38f, 1.7f, 4.45f, 1.7f, 4.45f, 1.95f, 4.38f, 1.95f, 4.38f, 1.7f);
        //press frame: columns, crossbeam, main ram
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.white).style(metal, matPlate).at(0, 0, 0).cbevel(s * 5.0f, 1.8f, deckZ, 1.7f, 1.7f, 6.2f, 0.2f);
            m.color(Pal.darkSteel).style(metal, 0).cbevel(s * 5.0f, 1.8f, deckZ, 2.3f, 2.3f, 0.4f, 0.1f);
        }
        m.color(Pal.white).style(metal, matPlate).at(0, 0, 0).cbevel(0, 1.8f, 7.6f, 12.2f, 2.5f, 1.3f, 0.25f);
        m.color(accent).style(0, 0).box(-5.8f, 0.53f, 7.95f, 5.8f, 0.55f, 8.35f);
        m.color(Pal.offWhite).style(metal, 0).at(0, 1.8f, 8.9f).lathe(16, 0, 0, 0, 1.0f, 0, 1.0f, 1.0f, 0.8f, 1.2f, 0, 1.2f);
        m.color(Pal.dark).style(metal, 0).at(0, 1.8f, 10.1f).cyl(10, 0.35f, 0f, 0.35f);
        //powder hopper on legs
        float hx = -8.2f, hy = 5.5f;
        for(int i = 0; i < 4; i++){
            float a = 45f + i * 90f;
            m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbox(hx + Mathf.cosDeg(a) * 1.2f, hy + Mathf.sinDeg(a) * 1.2f, deckZ, 0.3f, 0.3f, 2.2f);
        }
        m.color(Pal.white).style(metal, matPlate).at(hx, hy, 0).lathe(20, 0, 0, 3.0f, 0.5f, 3.0f, 1.9f, 4.5f, 1.9f, 5.1f, 0, 5.1f);
        m.color(accent).style(0, 0).at(hx, hy, 0).lathe(20, 0, 1.88f, 4.6f, 1.95f, 4.6f, 1.95f, 4.8f, 1.88f, 4.8f, 1.88f, 4.6f);
        m.color(Pal.steel).style(metal, 0).pipe(8, 0.3f, hx + 0.6f, hy - 0.8f, 3.2f, -3.5f, 2.1f, 3.3f);
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbevel(-3.5f, 2.1f, deckZ, 0.8f, 0.8f, 2.1f, 0.1f);
        //sinter furnace with window frame
        m.color(Pal.offWhite).style(0, matPlate).at(0, 0, 0).cbevel(8.4f, 3.6f, deckZ, 4.8f, 7.2f, 3.5f, 0.3f);
        m.color(Pal.dark).style(metal, 0).box(6.9f, -0.04f, 2.3f, 9.9f, 0.0f, 3.5f);
        m.color(0.55f, 0.22f, 0.10f).style(emissive, 0).box(7.1f, -0.06f, 2.45f, 9.7f, -0.04f, 3.35f);
        m.color(Pal.light).style(0, matFins).at(0, 0, 0).cbox(8.4f, 3.6f, deckZ + 3.5f, 3.6f, 5.0f, 0.1f);
        m.color(Pal.offWhite).style(metal, 0).at(9.6f, 6.0f, deckZ + 3.5f).cyl(12, 0.55f, 0f, 1.0f);
        m.color(Pal.dark).style(0, 0).at(9.6f, 6.0f, deckZ + 4.5f).cyl(12, 0.4f, 0f, 0.05f);
        //pellet chute and tray
        m.color(Pal.steel).style(metal, 0).at(0, 0, 0);
        m.quad(-2.2f, -3.0f, 2.9f, -1.5f, -3.6f, 2.9f, -5.5f, -8.0f, 1.9f, -6.2f, -7.4f, 1.9f, 0, 0, 1);
        m.color(Pal.darkSteel).style(metal, 0).cbevel(-6.8f, -8.6f, deckZ, 4.6f, 3.0f, 0.55f, 0.1f);
        m.color(Pal.graphite).style(0, 0).cbox(-6.8f, -8.6f, deckZ + 0.55f, 4.0f, 2.4f, 0.02f);
        for(int i = 0; i < 22; i++){
            float px = -8.5f + (i % 7) * 0.55f + (i / 7 % 2) * 0.25f, pyy = -9.5f + (i / 7) * 0.62f;
            m.color(Pal.pellet).style(0, 0).at(px, pyy, deckZ + 0.57f).cyl(8, 0.26f, 0f, 0.3f);
        }
        console(m, 7.2f, -8.3f);
        m.color(Pal.yellow).style(0, matHazard).at(0, 0, 0).box(-2.5f, -10.9f, deckZ, 2.5f, -10.2f, deckZ + 0.03f);
    }

    @Override
    public void buildEnvelope(Mesh m){
        m.at(tx, ty, tableTop - 0.02f).cyl(24, 3.7f, 0f, 4.5f);
        m.at(0, 0, 0).box(-6.4f, -8.2f, 1.8f, -1.3f, -2.8f, 3.4f);
        m.box(7.0f, -0.12f, 2.4f, 9.8f, -0.02f, 3.4f);
    }

    @Override
    public void buildRest(Mesh m){
        m.add(turret, tx, ty, tableTop, 2, 0f);
        for(int i = 0; i < stations; i++){
            float a = i * 30f;
            m.add(punch, tx + Mathf.cosDeg(a) * punchR, ty + Mathf.sinDeg(a) * punchR, cam(a), 2, 0f);
        }
    }

    /** Cam track: punches ride high and dive under the crossbeam (north, 90 degrees). */
    static float cam(float angle){
        float d = Math.abs(((angle - 90f) % 360f + 540f) % 360f - 180f);
        float dive = d < 40f ? 0.5f + 0.5f * Mathf.cosDeg(d / 40f * 180f) : 0f;
        return tableTop + 0.64f + 1.3f - 1.25f * dive;
    }

    @Override
    public void drawLive(Building b){
        float wx = b.x, wy = b.y, w = warmup(b), tp = total(b), t = Time.time;
        //furnace window
        float flick = 0.75f + 0.25f * Mathf.sin(t * 0.21f) * Mathf.sin(t * 0.077f + 1f);
        Live.glowR = 0.35f + 0.65f * w * flick; Live.glowG = 0.12f + 0.45f * w * flick; Live.glowB = 0.05f + 0.1f * w;
        Live.pose(8.4f, -0.05f, 2.4f, 2, 0f);
        Live.draw(glow, cam, wx, wy);

        float rot = tp * 1.6f;
        Live.pose(tx, ty, tableTop, 2, rot);
        Live.draw(turret, cam, wx, wy);
        //punches, back to front
        for(int i = 0; i < stations; i++){
            float a = rot + i * 30f;
            pa[i] = a;
            py[i] = ty + Mathf.sinDeg(a) * punchR;
            pz[i] = cam(a);
            porder[i] = i;
        }
        for(int i = 1; i < stations; i++){
            int v = porder[i], j = i - 1;
            while(j >= 0 && py[porder[j]] < py[v]){ porder[j + 1] = porder[j]; j--; }
            porder[j + 1] = v;
        }
        for(int k = 0; k < stations; k++){
            int i = porder[k];
            Live.pose(tx + Mathf.cosDeg(pa[i]) * punchR, py[i], pz[i], 2, pa[i] * 2f);
            Live.draw(punch, cam, wx, wy);
        }
        //pellets on the chute (top to bottom = back to front)
        if(w > 0.02f){
            Live.alpha = Mathf.clamp(w * 3f);
            for(int k = 0; k < 5; k++){
                float u = ((tp * 0.018f + k / 5f) % 1f);
                float x = Mathf.lerp(-1.85f, -5.85f, u), y = Mathf.lerp(-3.3f, -7.7f, u), z = Mathf.lerp(2.92f, 1.92f, u);
                Live.pose(x, y, z, 2, u * 200f);
                Live.draw(pellet, cam, wx, wy);
            }
            Live.alpha = 1f;
        }
    }
}
