package blackhole.models;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import blackhole.g3d.Kit.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.units.*;
import mindustry.world.blocks.units.UnitFactory.*;

import static blackhole.g3d.Kit.*;
import static blackhole.g3d.Mesh.*;

/**
 * Parametric gantry printer used by every unit factory of the mod (3x3 horizon assembler, 4x4 Aurelia fabricator,
 * 5x5 citadel yard and titan bays). Designed on a 3x3 grid and uniformly scaled to the block size. Four posts carry
 * two rails; a bridge travels along y, a head along the bridge, and a laser prints the unit layer by layer on the
 * round bed. 3D units are printed from their real rig (clipped at the print height with a glowing scan band);
 * sprite units (the Aegis / Pyroclast titans) are revealed rear-to-front under the same scan line.
 */
public class AssemblerModel extends KitModel{
    static final float cx = 0f, cy = -1.5f, bedZ0 = 2.2f, bedR0 = 5.4f, railZ0 = 8.6f, bridgeZ0 = 9.0f, postX0 = 9.6f,
        postS0 = -8.2f, postN0 = 2.6f, printR0 = 6.4f, dz0 = 1.4f;
    final float k;
    final Mesh bridge = new Mesh(), head = new Mesh(), nozzle = new Mesh(), chevron = new Mesh();
    final TextureRegion clip = new TextureRegion();

    public AssemblerModel(String name, int size, Style st){
        super(name, size, st);
        k = half / 12f;
        Color c = st.glow;
        bridge.color(st.hull).style(metal, matPlate).bevel(-postX0 + 0.2f, -0.65f, 0f, postX0 - 0.2f, 0.65f, 0.75f, 0.12f);
        bridge.color(st.trim).style(0, 0).box(-postX0 + 0.6f, -0.67f, 0.3f, postX0 - 0.6f, -0.65f, 0.48f);
        for(int sd = -1; sd <= 1; sd += 2){
            bridge.color(st.dark).style(metal, 0).at(0, 0, 0).bevel(sd * postX0 - 0.65f, -1.0f, -0.25f, sd * postX0 + 0.65f, 1.0f, 1.0f, 0.12f);
        }
        head.color(st.hull2).style(metal, matPlate).at(0, 0, 0).bevel(-1.1f, -1.0f, -0.2f, 1.1f, 1.0f, 1.3f, 0.15f);
        head.color(st.dark).style(metal, matFins).box(-0.8f, -1.02f, 0.2f, 0.8f, -1.0f, 1.0f);
        head.color(c).style(emissive, 0).at(0.75f, -0.75f, 1.3f).cyl(8, 0.16f, 0f, 0.12f);
        nozzle.color(st.dark).style(metal, 0).at(0, 0, 0).lathe(10, 0, 0, -1.6f, 0.28f, -1.4f, 0.55f, 0f, 0, 0f);
        nozzle.color(c).style(emissive, 0).at(0, 0, 0).cyl(8, 0.18f, -1.75f, -1.6f);
        for(int i = 0; i < 3; i++){
            float y0 = 8.9f + i * 0.8f;
            chevron.color(c).style(emissive, 0).at(0, 0, 0)
                .quad(-1.6f, y0, 0, -1.2f, y0, 0, 0, y0 + 1.0f, 0, 0, y0 + 1.4f, 0, 0, 0, 1);
            chevron.quad(0, y0 + 1.0f, 0, 1.2f, y0, 0, 1.6f, y0, 0, 0, y0 + 1.4f, 0, 0, 0, 1);
        }
        scale(bridge); scale(head); scale(nozzle); scale(chevron);
    }

    void scale(Mesh m){
        for(int i = 0; i < m.verts; i++){ m.vx[i] *= k; m.vy[i] *= k; m.vz[i] *= k; }
    }

    @Override
    public void buildStatic(Mesh out){
        Mesh m = new Mesh();
        Color c = s.glow;
        deck(m, s, 12f, dz0);
        m.color(s.dark).style(metal, 0).at(cx, cy, 0).lathe(32, 0, 0, dz0, bedR0 + 0.4f, dz0, bedR0 + 0.4f, bedZ0 - 0.25f, bedR0, bedZ0, 0, bedZ0);
        m.color(s.dark).style(0, matGrate).at(cx, cy, bedZ0).cyl(32, bedR0 - 0.5f, 0f, 0.01f);
        m.color(c).style(emissive, 0).at(cx, cy, 0).ring(32, bedR0 - 0.45f, bedR0 - 0.2f, bedZ0, bedZ0 + 0.05f);
        for(int i = 0; i < 8; i++){
            float a = i * 45f + 22.5f;
            m.color(s.metal).style(metal, 0).at(0, 0, 0).cbox(cx + Mathf.cosDeg(a) * (bedR0 + 0.55f), cy + Mathf.sinDeg(a) * (bedR0 + 0.55f), dz0, 0.7f, 0.7f, 0.6f);
        }
        for(int sd = -1; sd <= 1; sd += 2){
            for(float py : new float[]{postS0, postN0}){
                m.color(s.hull).style(metal, matPlate).at(0, 0, 0).cbevel(sd * postX0, py, dz0, 1.0f, 1.0f, railZ0 - dz0, 0.12f);
                m.color(s.dark).style(metal, 0).cbevel(sd * postX0, py, dz0, 1.6f, 1.6f, 0.35f, 0.08f);
                m.color(s.trim).style(0, 0).box(sd * postX0 - 0.52f, py - 0.52f, 5.0f, sd * postX0 + 0.52f, py + 0.52f, 5.35f);
            }
            m.color(s.hull2).style(metal, 0).at(0, 0, 0).bevel(sd * postX0 - 0.45f, postS0 - 0.6f, railZ0, sd * postX0 + 0.45f, postN0 + 0.6f, bridgeZ0 - 0.02f, 0.08f);
            hazard(m).box(sd * postX0 - 0.47f, postS0 - 0.62f, railZ0 + 0.05f, sd * postX0 + 0.47f, postS0 - 0.6f, bridgeZ0 - 0.07f);
            m.color(s.hull2).style(metal, matPlate).at(0, 0, 0).bevel(sd * 10.9f - 0.9f, -6.6f, dz0, sd * 10.9f + 0.9f, 1.2f, 3.0f, 0.15f);
            m.color(s.dark).style(metal, matFins).box(sd * 10.9f - 0.6f, -6.62f, dz0 + 0.5f, sd * 10.9f + 0.6f, -6.6f, 2.6f);
        }
        float[] hx = {-5.8f, -1.6f, 2.6f};
        Color[] hc = {s.metal, s.glow2, s.glow};
        for(int i = 0; i < 3; i++){
            float x = hx[i], y = 7.2f;
            for(int q = 0; q < 4; q++){
                float lx = x + ((q & 1) == 0 ? -1.2f : 1.2f), ly = y + ((q & 2) == 0 ? -0.9f : 0.9f);
                m.color(s.dark).style(metal, 0).at(0, 0, 0).cbox(lx, ly, dz0, 0.3f, 0.3f, 1.6f);
            }
            m.color(s.hull).style(metal, matPlate).at(0, 0, 0).taper(x, y, dz0 + 1.3f, 1.2f, 1.0f, 4.0f, 3.2f, 2.5f, 0, 0, false);
            m.color(hc[i]).style(i == 0 ? 0 : emissive, 0).at(0, 0, 0).taper(x, y, 4.0f, 3.3f, 2.6f, 4.1f, 2.9f, 2.2f, 0, 0, true); //lid overlaps the hopper rim so the open hopper interior is never visible
            m.color(s.metal).style(metal, 0).pipe(8, 0.22f, x, y - 0.4f, dz0 + 1.4f, cx + (i - 1) * 1.6f, cy + bedR0 + 0.3f, dz0 + 0.6f);
        }
        console(m, s, 7.4f, -9.6f, dz0);
        hazard(m).at(0, 0, 0).box(-3.4f, -10.9f, dz0, 3.4f, -10.2f, dz0 + 0.03f);
        scale(m);
        out.add(m, 0, 0, 0, 2, 0);
    }

    @Override
    public void buildEnvelope(Mesh m){
        m.at(cx * k, cy * k, bedZ0 * k).cyl(24, printR0 * k, 0f, 12.5f * k);
        float a = 8.8f * k, b = 11.5f * k, w = 1.7f * k, d = dz0 * k;
        m.at(0, 0, 0).box(-w, a, d, w, b, d + 0.1f);
        m.box(-w, -b, d, w, -a, d + 0.1f);
        m.box(a, -w, d, b, w, d + 0.1f);
        m.box(-b, -w, d, -a, w, d + 0.1f);
    }

    @Override
    public void buildRest(Mesh m){
        m.add(bridge, 0, cy * k, bridgeZ0 * k, 2, 0f);
        m.add(head, 1.5f * k, cy * k, (bridgeZ0 + 0.75f) * k, 2, 0f);
        m.add(nozzle, 1.5f * k, cy * k, (bridgeZ0 - 0.2f) * k, 2, 0f);
        m.add(chevron, 0, 0, (dz0 + 0.02f) * k, 2, -90f);
    }

    // ------------------------------------------------------------------ live

    float gx, gy, beamZ, fraction;
    boolean printing;
    UnitType type;

    void solve(Building b){
        printing = false;
        type = null;
        fraction = 0f;
        if(b instanceof UnitFactoryBuild u && u.currentPlan != -1 && u.block instanceof UnitFactory f && u.currentPlan < f.plans.size){
            var plan = f.plans.get(u.currentPlan);
            fraction = Mathf.clamp(u.fraction());
            type = plan.unit;
            printing = u.efficiency > 0.01f && fraction < 0.999f;
        }
        float t = Time.time + b.id * 31f;
        if(type != null){
            float r = printR0 * 0.72f;
            gx = (cx + Mathf.sin(t, 11f, r)) * k;
            gy = (cy + Mathf.sin(t + 17f, 17f, r * 0.8f)) * k;
        }else{
            gx = (cx + 1.5f) * k;
            gy = cy * k;
        }
    }

    @Override
    public void drawLive(Building b){
        solve(b);
        float t = Time.time + b.id * 7f;
        Live.glowR = Live.glowG = Live.glowB = 0.55f + 0.45f * Mathf.absin(t, 8f, 1f);
        Live.alpha = 0.9f;
        Live.pose(0, 0, (dz0 + 0.02f) * k, 2, b.rotdeg() - 90f);
        Live.draw(chevron, cam, b.x, b.y);
        Live.resetTint();
        if(type == null || fraction <= 0.001f) return;

        float bedZ = bedZ0 * k, printR = printR0 * k;
        if(type instanceof Unit3DType<?> v){
            v.ensureRig();
            Rig rig = v.rig;
            UnitRenderer r = v.renderer;
            Cam old = r.cam;
            float ps = Math.min(printR / Math.max(rig.half, 1f), 6.4f * k / Math.max(rig.height, 1f));
            v.restPose();
            r.cam = cam;
            r.reset();
            r.setTeam(b.team.color);
            r.clipZ = bedZ + 0.2f + fraction * (rig.height * ps + 0.6f);
            r.clipBand = 0.9f;
            r.pose(rig, b.rotdeg(), cx * k, (cy - 1.0f) * k, bedZ, ps);
            r.draw(rig, b.x, b.y);
            beamZ = r.clipZ;
            r.reset();
            r.cam = old;
        }else{
            //sprite unit: reveal rear-to-front
            TextureRegion icon = type.fullIcon;
            if(icon == null) return;
            float w = icon.width, h = icon.height;
            float fit = printR * 2f / Math.max(w, h);
            float W = w * fit, H = h * fit;
            float px = b.x + cam.sx(cx * k, bedZ), py = b.y + cam.sy(cy * k, bedZ);
            float rot = b.rotdeg();
            clip.set(icon);
            float v0 = icon.v, v1 = icon.v2;
            clip.v = v1 + (v0 - v1) * fraction;
            clip.v2 = v1;
            float hh = H * fraction;
            float off = -H / 2f + hh / 2f;
            Draw.color();
            Draw.rect(clip, px + Angles.trnsx(rot, off), py + Angles.trnsy(rot, off), W, hh, rot - 90f);
            //scan line across the cut
            float lineOff = -H / 2f + hh;
            float lx = px + Angles.trnsx(rot, lineOff), ly = py + Angles.trnsy(rot, lineOff);
            Draw.blend(Blending.additive);
            Draw.color(s.glow, 0.8f);
            Lines.stroke(1.1f);
            Lines.lineAngleCenter(lx, ly, rot + 90f, W);
            Draw.blend();
            Draw.color();
            beamZ = bedZ + 0.3f;
            gx = cx * k + Angles.trnsx(rot, lineOff) * 0.9f;
            gy = cy * k + Angles.trnsy(rot, lineOff) * 0.9f;
        }
    }

    @Override
    public void drawOver(Building b){
        float wx = b.x, wy = b.y, bridgeZ = bridgeZ0 * k;
        if(printing && type != null){
            float t = Time.time + b.id * 5f;
            float nz = bridgeZ - 1.95f * k;
            float x0 = wx + cam.sx(gx, nz), y0 = wy + cam.sy(gy, nz);
            float x1 = wx + cam.sx(gx, beamZ), y1 = wy + cam.sy(gy, beamZ);
            Draw.blend(Blending.additive);
            Draw.color(s.glow, 0.55f + 0.3f * Mathf.absin(t, 2f, 1f));
            Lines.stroke(0.9f * k);
            Lines.line(x0, y0, x1, y1);
            Draw.color(Color.white, 0.8f);
            Lines.stroke(0.35f * k);
            Lines.line(x0, y0, x1, y1);
            Draw.color(s.glow, 0.6f);
            Fill.circle(x1, y1, (1.1f + Mathf.absin(t, 1.5f, 0.5f)) * k);
            Draw.blend();
            Draw.color();
        }
        Live.pose(gx, gy, bridgeZ - 0.2f * k, 2, 0f);
        Live.draw(nozzle, cam, wx, wy);
        Live.pose(0, gy, bridgeZ, 2, 0f);
        Live.draw(bridge, cam, wx, wy);
        Live.glowR = 0.6f; Live.glowG = 1f; Live.glowB = 1f;
        if(printing && Mathf.sin(Time.time * 0.25f) < 0f){ Live.glowR = 0.3f; Live.glowG = 0.5f; Live.glowB = 0.5f; }
        Live.pose(gx, gy, bridgeZ + 0.75f * k, 2, 0f);
        Live.draw(head, cam, wx, wy);
        Live.resetTint();
    }

    @Override
    public void drawCheap(Building b){
    }
}
