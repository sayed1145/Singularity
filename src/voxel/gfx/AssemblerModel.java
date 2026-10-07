package voxel.gfx;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.world.blocks.units.UnitFactory.*;
import voxel.unit.*;

import static voxel.gfx.Mesh.*;

/**
 * Voxel assembler (3x3): a gantry printer. Four posts carry two rails; a bridge travels along y, a print head
 * along the bridge, and a laser from the head builds the actual 3D unit layer by layer on the round bed
 * (the unit's own rig, clipped at the print height, with a glowing scan band at the cut). Three hoppers feed
 * silicon, titanium and alloy from the north; a glowing chevron shows the output side.
 */
public class AssemblerModel extends FactoryModel{
    public static final Color cyan = Color.valueOf("8fe9ff");
    public static final AssemblerModel instance = new AssemblerModel();
    static final float cx = 0f, cy = -1.5f, bedZ = 2.2f, bedR = 5.4f, railZ = 8.6f, bridgeZ = 9.0f, postX = 9.6f;
    static final float postS = -8.2f, postN = 2.6f, printR = 6.4f;

    final Mesh bridge = new Mesh(), head = new Mesh(), nozzle = new Mesh(), chevron = new Mesh();
    final float[] v3 = new float[3];

    public AssemblerModel(){
        super("voxel-assembler", cyan);
        bridge.color(Pal.white).style(metal, matPlate).bevel(-postX + 0.2f, -0.65f, 0f, postX - 0.2f, 0.65f, 0.75f, 0.12f);
        bridge.color(accent).style(0, 0).box(-postX + 0.6f, -0.67f, 0.3f, postX - 0.6f, -0.65f, 0.48f);
        for(int s = -1; s <= 1; s += 2){
            bridge.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).bevel(s * postX - 0.65f, -1.0f, -0.25f, s * postX + 0.65f, 1.0f, 1.0f, 0.12f);
        }
        head.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0).bevel(-1.1f, -1.0f, -0.2f, 1.1f, 1.0f, 1.3f, 0.15f);
        head.color(Pal.dark).style(metal, matFins).box(-0.8f, -1.02f, 0.2f, 0.8f, -1.0f, 1.0f);
        head.color(cyan).style(emissive, 0).at(0.75f, -0.75f, 1.3f).cyl(8, 0.16f, 0f, 0.12f);
        nozzle.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).lathe(10, 0, 0, -1.6f, 0.28f, -1.4f, 0.55f, 0f, 0, 0f);
        nozzle.color(cyan).style(emissive, 0).at(0, 0, 0).cyl(8, 0.18f, -1.75f, -1.6f);
        //output chevron: three nested arrows lying on the deck, pointing +y (rotated at draw time)
        for(int k = 0; k < 3; k++){
            float y0 = 8.9f + k * 0.8f;
            chevron.color(cyan).style(emissive, 0).at(0, 0, 0)
                .quad(-1.6f, y0, 0, -1.2f, y0, 0, 0, y0 + 1.0f, 0, 0, y0 + 1.4f, 0, 0, 0, 1);
            chevron.quad(0, y0 + 1.0f, 0, 1.2f, y0, 0, 1.6f, y0, 0, 0, y0 + 1.4f, 0, 0, 0, 1);
        }
    }

    @Override
    public void buildStatic(Mesh m){
        deck(m);
        //print bed
        m.color(Pal.dark).style(metal, 0).at(cx, cy, 0).lathe(32, 0, 0, deckZ, bedR + 0.4f, deckZ, bedR + 0.4f, bedZ - 0.25f, bedR, bedZ, 0, bedZ);
        m.color(Pal.graphite).style(0, matGrate).at(cx, cy, bedZ).cyl(32, bedR - 0.5f, 0f, 0.01f);
        m.color(cyan).style(emissive, 0).at(cx, cy, 0).ring(32, bedR - 0.45f, bedR - 0.2f, bedZ, bedZ + 0.05f);
        for(int i = 0; i < 8; i++){
            float a = i * 45f + 22.5f;
            m.color(Pal.mid).style(metal, 0).at(0, 0, 0).cbox(cx + Mathf.cosDeg(a) * (bedR + 0.55f), cy + Mathf.sinDeg(a) * (bedR + 0.55f), deckZ, 0.7f, 0.7f, 0.6f);
        }
        //gantry posts, rails
        for(int s = -1; s <= 1; s += 2){
            for(float py : new float[]{postS, postN}){
                m.color(Pal.white).style(metal, matPlate).at(0, 0, 0).cbevel(s * postX, py, deckZ, 1.0f, 1.0f, railZ - deckZ, 0.12f);
                m.color(Pal.darkSteel).style(metal, 0).cbevel(s * postX, py, deckZ, 1.6f, 1.6f, 0.35f, 0.08f);
                m.color(accent).style(0, 0).box(s * postX - 0.52f, py - 0.52f, 5.0f, s * postX + 0.52f, py + 0.52f, 5.35f);
            }
            m.color(Pal.offWhite).style(metal, 0).at(0, 0, 0).bevel(s * postX - 0.45f, postS - 0.6f, railZ, s * postX + 0.45f, postN + 0.6f, bridgeZ - 0.02f, 0.08f);
            m.color(Pal.yellow).style(0, matHazard).box(s * postX - 0.47f, postS - 0.62f, railZ + 0.05f, s * postX + 0.47f, postS - 0.6f, bridgeZ - 0.07f);
            //side tool cabinets
            m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0).bevel(s * 10.9f - 0.9f, -6.6f, deckZ, s * 10.9f + 0.9f, 1.2f, 3.0f, 0.15f);
            m.color(Pal.dark).style(metal, matFins).box(s * 10.9f - 0.6f, -6.62f, deckZ + 0.5f, s * 10.9f + 0.6f, -6.6f, 2.6f);
        }
        //material hoppers (north): silicon, titanium, alloy
        float[] hx = {-5.8f, -1.6f, 2.6f};
        Color[] hc = {new Color(0.33f, 0.34f, 0.37f), new Color(0.72f, 0.66f, 0.86f), cyan};
        for(int i = 0; i < 3; i++){
            float x = hx[i], y = 7.2f;
            for(int k = 0; k < 4; k++){
                float lx = x + ((k & 1) == 0 ? -1.2f : 1.2f), ly = y + ((k & 2) == 0 ? -0.9f : 0.9f);
                m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbox(lx, ly, deckZ, 0.3f, 0.3f, 1.6f);
            }
            m.color(Pal.white).style(metal, matPlate).at(0, 0, 0).taper(x, y, deckZ + 1.3f, 1.2f, 1.0f, 4.0f, 3.2f, 2.5f, 0, 0, false);
            m.color(hc[i]).style(i == 2 ? emissive : 0, 0).at(0, 0, 0).taper(x, y, 4.0f, 3.0f, 2.3f, 4.1f, 2.9f, 2.2f, 0, 0, false);
            m.color(Pal.steel).style(metal, 0).pipe(8, 0.22f, x, y - 0.4f, deckZ + 1.4f, cx + (i - 1) * 1.6f, cy + bedR + 0.3f, deckZ + 0.6f);
        }
        console(m, 7.4f, -9.6f);
        m.color(Pal.yellow).style(0, matHazard).at(0, 0, 0).box(-3.4f, -10.9f, deckZ, 3.4f, -10.2f, deckZ + 0.03f);
    }

    @Override
    public void buildEnvelope(Mesh m){
        m.at(cx, cy, bedZ).cyl(24, printR, 0f, 12.5f);
        //chevrons on all four edges
        m.at(0, 0, 0).box(-1.7f, 8.8f, deckZ, 1.7f, 11.5f, deckZ + 0.1f);
        m.box(-1.7f, -11.5f, deckZ, 1.7f, -8.8f, deckZ + 0.1f);
        m.box(8.8f, -1.7f, deckZ, 11.5f, 1.7f, deckZ + 0.1f);
        m.box(-11.5f, -1.7f, deckZ, -8.8f, 1.7f, deckZ + 0.1f);
    }

    @Override
    public void buildRest(Mesh m){
        m.add(bridge, 0, cy, bridgeZ, 2, 0f);
        m.add(head, 1.5f, cy, bridgeZ + 0.75f, 2, 0f);
        m.add(nozzle, 1.5f, cy, bridgeZ - 0.2f, 2, 0f);
        m.add(chevron, 0, 0, deckZ + 0.02f, 2, -90f);
    }

    // ------------------------------------------------------------------ live

    float gx, gy, beamZ, fraction;
    boolean printing;
    VoxelUnitType<?> type;

    void solve(Building b){
        printing = false;
        type = null;
        fraction = 0f;
        if(b instanceof UnitFactoryBuild u && u.currentPlan != -1 && u.block instanceof mindustry.world.blocks.units.UnitFactory f){
            var plan = f.plans.get(u.currentPlan);
            fraction = Mathf.clamp(u.fraction());
            if(plan.unit instanceof VoxelUnitType<?> v) type = v;
            printing = u.efficiency > 0.01f && fraction < 0.999f;
        }
        float t = Time.time + b.id * 31f;
        if(type != null){
            float r = printR * 0.72f;
            gx = cx + Mathf.sin(t, 11f, r);
            gy = cy + Mathf.sin(t + 17f, 17f, r * 0.8f);
        }else{
            gx = cx + 1.5f;
            gy = cy;
        }
    }

    float printScale(VoxelUnitType<?> v){
        //fit the bed footprint, and stay under the print head (tall mechs are printed smaller)
        return Math.min(printR / Math.max(v.rig.half, 1f), 6.4f / Math.max(v.rig.height, 1f));
    }

    @Override
    public void drawLive(Building b){
        solve(b);
        //output chevron, pulsing towards the exit
        float t = Time.time + b.id * 7f;
        Live.glowR = Live.glowG = Live.glowB = 0.55f + 0.45f * Mathf.absin(t, 8f, 1f);
        Live.alpha = 0.9f;
        Live.pose(0, 0, deckZ + 0.02f, 2, b.rotdeg() - 90f);
        Live.draw(chevron, cam, b.x, b.y);
        Live.resetTint();

        if(type == null || fraction <= 0.001f) return;
        type.ensureRig();
        Rig rig = type.rig;
        UnitRenderer r = type.renderer;
        Cam old = r.cam;
        float ps = printScale(type);
        type.restPose();
        r.cam = cam;
        r.reset();
        r.setTeam(b.team.color);
        r.clipZ = bedZ + 0.2f + fraction * (rig.height * ps + 0.6f);
        r.clipBand = 0.9f;
        r.pose(rig, b.rotdeg(), cx, cy - 1.0f, bedZ, ps); //slightly south: the tall print leans north in the camera
        r.draw(rig, b.x, b.y);
        beamZ = r.clipZ;
        r.reset();
        r.cam = old;
    }

    @Override
    public void drawOver(Building b){
        float wx = b.x, wy = b.y;
        //laser from the nozzle to the current print layer
        if(printing && type != null){
            float t = Time.time + b.id * 5f;
            float nz = bridgeZ - 1.95f;
            float x0 = wx + cam.sx(gx, nz), y0 = wy + cam.sy(gy, nz);
            float x1 = wx + cam.sx(gx, beamZ), y1 = wy + cam.sy(gy, beamZ);
            Draw.blend(Blending.additive);
            Draw.color(cyan, 0.55f + 0.3f * Mathf.absin(t, 2f, 1f));
            Lines.stroke(0.9f);
            Lines.line(x0, y0, x1, y1);
            Draw.color(Color.white, 0.8f);
            Lines.stroke(0.35f);
            Lines.line(x0, y0, x1, y1);
            Draw.color(cyan, 0.6f);
            Fill.circle(x1, y1, 1.1f + Mathf.absin(t, 1.5f, 0.5f));
            Draw.blend();
            Draw.color();
        }
        Live.pose(gx, gy, bridgeZ - 0.2f, 2, 0f);
        Live.draw(nozzle, cam, wx, wy);
        Live.pose(0, gy, bridgeZ, 2, 0f);
        Live.draw(bridge, cam, wx, wy);
        Live.glowR = 0.6f; Live.glowG = 1f; Live.glowB = 1f;
        if(printing && Mathf.sin(Time.time * 0.25f) < 0f){ Live.glowR = 0.3f; Live.glowG = 0.5f; Live.glowB = 0.5f; }
        Live.pose(gx, gy, bridgeZ + 0.75f, 2, 0f);
        Live.draw(head, cam, wx, wy);
        Live.resetTint();
    }
}
