package voxel.gfx;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;

import static voxel.gfx.Mesh.*;

/**
 * Voxel forge (3x3): a containment pedestal between three field pylons. Above it a voxel core is assembled from
 * eight cubes that converge as the craft progresses, spun inside three nested gimbal rings; finished alloy
 * ingots slide down the chute onto the output tray. Cryo tank on the west, capacitor bank on the east.
 *
 * <p>Static geometry is baked; the core, rings and ingots are a small live {@link Rig} drawn with the block's
 * own camera, so they are exactly occluded by the baked front layer.
 */
public class ForgeModel extends FactoryModel{
    public static final Color alloy = Color.valueOf("8fe9ff");
    static final float[] pylonA = {90f, 210f, 330f};
    public static final ForgeModel instance = new ForgeModel();
    static final float cx = 0f, cy = -1f, coreZ = 5.7f, padZ = 2.6f, pylonR = 5.6f, pylonTop = 8.0f;
    static final float chX0 = 2.9f, chY0 = -3.9f, chZ0 = 2.75f, chX1 = 5.0f, chY1 = -6.2f, trayZ = deckZ + 0.55f;

    final Rig rig = new Rig();
    final UnitRenderer renderer;
    int ROOT, CORE, G0, G1, G2;
    final int[] CUBE = new int[8], INGOT = new int[3];

    public ForgeModel(){
        super("voxel-forge", alloy);
        renderer = new UnitRenderer(half);
        buildRig();
    }

    void buildRig(){
        Rig r = rig;
        ROOT = r.bone(-1, 0, 0, 0);
        CORE = r.bone(ROOT, cx, cy, coreZ);
        G0 = r.bone(ROOT, cx, cy, coreZ);
        G1 = r.bone(G0, 0, 0, 0);
        G2 = r.bone(G1, 0, 0, 0);
        for(int i = 0; i < 8; i++) CUBE[i] = r.bone(CORE, 0, 0, 0);
        for(int i = 0; i < 3; i++) INGOT[i] = r.bone(ROOT, 0, 0, 0);

        Mesh m = r.part(CORE); m.color(alloy).style(emissive, 0);
        m.cbevel(0, 0, -0.75f, 1.5f, 1.5f, 1.5f, 0.25f);
        for(int i = 0; i < 8; i++){
            m = r.part(CUBE[i]); m.color(0.62f, 0.80f, 0.88f).style(metal, 0);
            m.cbevel(0, 0, -0.5f, 1.0f, 1.0f, 1.0f, 0.18f);
        }
        m = r.part(G0); m.color(Pal.white).style(metal, 0);
        ring(m, 3.05f, 3.4f, 0.22f);
        m = r.part(G1); m.color(Pal.darkSteel).style(metal, 0);
        ring(m, 2.45f, 2.75f, 0.2f);
        m = r.part(G2); m.color(alloy).style(emissive, 0);
        ring(m, 1.9f, 2.1f, 0.16f);
        for(int i = 0; i < 3; i++){
            m = r.part(INGOT[i]); m.color(0.55f, 0.85f, 0.95f).style(metal, 0);
            m.taper(0, 0, 0, 1.5f, 0.8f, 0.45f, 1.15f, 0.55f, 0, 0, false);
        }
        r.half = half;
        r.finish();
    }

    /** Thin square-section ring; inner wall first in face order so it never paints over the outer wall. */
    static void ring(Mesh m, float rin, float rout, float h){
        m.lathe(28, 0, rin, h / 2f, rin, -h / 2f, rout, -h / 2f, rout, h / 2f, rin, h / 2f);
    }


    // ------------------------------------------------------------------ static

    @Override
    public void buildStatic(Mesh m){
        deck(m);
        //containment pedestal
        m.color(Pal.mid).style(metal, matPlate).at(cx, cy, 0).lathe(28, 0, 0, deckZ, 4.4f, deckZ, 4.4f, 2.1f, 3.8f, padZ, 0, padZ);
        m.color(Pal.graphite).style(0, 0).at(cx, cy, padZ).cyl(28, 2.55f, 0f, 0.01f);
        m.color(alloy).style(emissive, 0).at(cx, cy, 0).ring(28, 2.6f, 3.0f, padZ, padZ + 0.06f);
        for(int i = 0; i < 6; i++){
            float a = i * 60f + 30f;
            m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbox(cx + Mathf.cosDeg(a) * 4.0f, cy + Mathf.sinDeg(a) * 4.0f, 2.0f, 0.6f, 0.6f, 0.45f);
        }
        //field pylons
        for(float a : pylonA){
            float px = cx + Mathf.cosDeg(a) * pylonR, py = cy + Mathf.sinDeg(a) * pylonR;
            float dx = -Mathf.cosDeg(a), dy = -Mathf.sinDeg(a);
            m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbevel(px, py, deckZ, 2.0f, 2.0f, 0.8f, 0.15f);
            m.color(Pal.white).style(metal, matPlate).at(0, 0, 0).taper(px, py, deckZ + 0.8f, 1.3f, 1.3f, pylonTop - 0.6f, 0.9f, 0.9f, 0, 0, false);
            m.color(accent).style(0, 0).at(0, 0, 0).taper(px, py, 4.0f, 1.13f, 1.13f, 4.4f, 1.1f, 1.1f, 0, 0, false);
            m.color(Pal.dark).style(metal, 0).at(0, 0, 0).cbox(px, py, pylonTop - 0.6f, 1.1f, 1.1f, 0.5f);
            m.color(Pal.steel).style(metal, 0).pipe(8, 0.24f, px, py, pylonTop - 0.35f, px + dx * 1.7f, py + dy * 1.7f, pylonTop - 1.1f);
            m.color(alloy).style(emissive, 0).at(px + dx * 1.8f, py + dy * 1.8f, pylonTop - 1.15f).lathe(8, 0, 0, -0.35f, 0.34f, 0f, 0, 0.35f);
        }
        //cryo tank (west) on cradles
        m.color(Pal.white).style(metal, matPlate).at(-9.2f, -9.4f, 3.3f).rot(0, -90).lathe(20, 0, 0, 0, 1.35f, 0.3f, 1.6f, 1.0f, 1.6f, 9.4f, 1.35f, 10.1f, 0, 10.4f);
        m.color(Pal.cyan).style(metal, matGlass).at(-9.2f, -9.4f, 3.3f).rot(0, -90).cyl(20, 1.63f, 3.2f, 3.8f);
        m.color(Pal.cyan).style(metal, matGlass).at(-9.2f, -9.4f, 3.3f).rot(0, -90).cyl(20, 1.63f, 6.4f, 7.0f);
        for(float ty : new float[]{-7.6f, -1.6f}){
            m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbox(-9.2f, ty, deckZ, 2.6f, 0.8f, 1.2f);
        }
        m.color(Pal.offWhite).style(metal, 0).at(-8.6f, 5.0f, 0).lathe(16, 0, 0, deckZ, 1.5f, deckZ, 1.5f, 5.6f, 1.1f, 6.0f, 0, 6.0f);
        m.color(Pal.cyan).style(metal, matGlass).at(-8.6f, 5.0f, 0).cyl(16, 1.53f, 3.0f, 3.5f);
        m.color(Pal.steel).style(metal, 0).pipe(8, 0.3f, -8.0f, -3.0f, 2.2f, -4.2f, -1.6f, 2.2f);
        m.pipe(8, 0.3f, -8.0f, 4.0f, 2.4f, -3.2f, 1.2f, 2.2f);
        //capacitor bank (east)
        for(float ty : new float[]{-7.4f, -3.9f, -0.4f}){
            m.color(Pal.white).style(metal, matPlate).at(9.0f, ty, 0).lathe(16, 0, 0, deckZ, 1.25f, deckZ, 1.25f, 4.6f, 0.9f, 5.0f, 0, 5.0f);
            m.color(Pal.dark).style(0, 0).at(9.0f, ty, 0).cyl(16, 1.28f, 2.6f, 3.0f);
            m.color(Pal.amber).style(emissive, 0).at(9.0f, ty, 5.0f).cyl(8, 0.42f, 0f, 0.18f);
        }
        m.color(Pal.dark).style(0, 0).pipe(8, 0.26f, 7.8f, -3.9f, 2.0f, 4.3f, -1.8f, 2.0f);
        m.pipe(8, 0.26f, 7.8f, -0.4f, 2.0f, 4.1f, -0.3f, 2.0f);
        //pump housing (north-east, low)
        m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0).cbevel(5.2f, 6.9f, deckZ, 5.2f, 3.2f, 3.0f, 0.2f);
        m.color(Pal.dark).style(metal, matFins).at(0, 0, 0).box(3.0f, 5.28f, deckZ + 0.6f, 7.4f, 5.3f, deckZ + 2.4f);
        //output chute + tray (south-east)
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).hexa(false,
            chX0 - 0.75f, chY0 + 0.45f, chZ0 - 0.25f, chX0 + 0.35f, chY0 - 0.75f, chZ0 - 0.25f,
            chX1 + 0.35f, chY1 - 0.75f, trayZ - 0.25f, chX1 - 0.75f, chY1 + 0.45f, trayZ - 0.25f,
            chX0 - 0.75f, chY0 + 0.45f, chZ0, chX0 + 0.35f, chY0 - 0.75f, chZ0,
            chX1 + 0.35f, chY1 - 0.75f, trayZ, chX1 - 0.75f, chY1 + 0.45f, trayZ);
        m.color(Pal.mid).style(metal, matPlate).at(0, 0, 0).bevel(4.2f, -10.6f, deckZ, 9.6f, -6.0f, trayZ, 0.12f);
        m.color(accent).style(0, 0).box(4.2f, -10.64f, deckZ + 0.2f, 9.6f, -10.6f, deckZ + 0.4f);
        console(m, -5.6f, -9.4f);
        m.color(Pal.yellow).style(0, matHazard).at(0, 0, 0).box(-2.6f, -10.9f, deckZ, 2.6f, -10.2f, deckZ + 0.03f);
    }

    @Override
    public void buildEnvelope(Mesh m){
        m.at(cx, cy, padZ).cyl(24, 3.6f, 0f, coreZ + 3.6f - padZ);
        m.at(0, 0, 0).box(2.0f, -10.4f, trayZ - 0.2f, 9.4f, -3.2f, chZ0 + 0.7f);
    }

    @Override
    public void buildRest(Mesh m){
        pose(0.6f, 1f, 40f);
        renderer.pose(rig, 90f, 0, 0, 0, 1f);
        renderer.flatten(rig, m, true);
    }

    // ------------------------------------------------------------------ live

    void pose(float progress, float warm, float total){
        rig.reset();
        float t = total;
        rig.rot(CORE, 2, t * 2.2f);
        rig.rot(CORE, 0, 35.26f);
        rig.rot(CORE, 1, 45f);
        rig.glow[CORE] = 0.35f + warm * (0.75f + 0.2f * Mathf.sin(t * 0.3f));
        float spread = 0.62f + (1f - progress) * 0.55f * warm + 0.05f * Mathf.sin(t * 0.25f);
        for(int i = 0; i < 8; i++){
            float sx = (i & 1) == 0 ? -1f : 1f, sy = (i & 2) == 0 ? -1f : 1f, sz = (i & 4) == 0 ? -1f : 1f;
            rig.move(CUBE[i], sx * spread, sy * spread, sz * spread);
            rig.rot(CUBE[i], 2, t * (i % 2 == 0 ? 1.5f : -1.5f));
        }
        rig.rot(G0, 2, t * 1.1f);
        rig.rot(G0, 0, 12f * warm);
        rig.rot(G1, 0, t * 1.7f + 30f);
        rig.rot(G2, 1, t * 2.4f + 60f);
        rig.glow[G2] = 0.4f + 0.8f * warm;
        //ingots: slide down the chute, then along the tray
        for(int i = 0; i < 3; i++){
            float s = warm > 0.05f ? ((t * 0.006f + i / 3f) % 1f) : (0.72f + i * 0.1f);
            float x, y, z;
            if(s < 0.45f){
                float k = s / 0.45f;
                x = Mathf.lerp(chX0, chX1, k); y = Mathf.lerp(chY0, chY1, k); z = Mathf.lerp(chZ0, trayZ, k);
            }else{
                float k = (s - 0.45f) / 0.55f;
                x = Mathf.lerp(chX1, 5.6f + i * 1.3f, k); y = Mathf.lerp(chY1, -8.8f + (i % 2) * 1.0f, k); z = trayZ;
            }
            rig.place(INGOT[i], x, y, z);
            rig.rot(INGOT[i], 2, -40f + s * 40f);
            rig.alpha[INGOT[i]] = s < 0.04f ? s / 0.04f : s > 0.95f ? (1f - s) / 0.05f : 1f;
        }
    }

    @Override
    public void drawLive(Building b){
        float warm = warmup(b), prog = progress(b), total = total(b) + b.id * 7f;
        pose(prog, warm, total);
        renderer.reset();
        renderer.lowDetail = false;
        renderer.pose(rig, 90f, 0, 0, 0, 1f);
        renderer.draw(rig, b.x, b.y);
    }

    @Override
    public void drawOver(Building b){
        float warm = warmup(b);
        if(warm < 0.03f) return;
        float t = Time.time + b.id * 13f;
        float cxs = b.x + cam.sx(cx, coreZ), cys = b.y + cam.sy(cy, coreZ);
        Draw.blend(Blending.additive);
        Lines.stroke(0.6f + 0.25f * Mathf.absin(t, 3f, 1f));
        for(int i = 0; i < 3; i++){
            float a = pylonA[i];
            float px = cx + Mathf.cosDeg(a) * (pylonR - 1.8f), py = cy + Mathf.sinDeg(a) * (pylonR - 1.8f), pz = pylonTop - 1.15f;
            float flick = 0.55f + 0.45f * Mathf.absin(t + i * 17f, 2.3f, 1f);
            Draw.color(alloy, warm * 0.55f * flick);
            Lines.line(b.x + cam.sx(px, pz), b.y + cam.sy(py, pz), cxs, cys);
        }
        Draw.color(alloy, warm * 0.35f);
        Fill.circle(cxs, cys, 2.6f + Mathf.absin(t, 6f, 0.6f));
        Draw.blend();
        Draw.color();
    }
}
