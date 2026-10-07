package rbmk.gfx;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;

import static rbmk.gfx.Mesh.*;

/**
 * Fuel assembly plant: a three-axis gantry robot takes each fuel element from the feeder, carries it over the
 * bundle and inserts it into the next of 18 slots (6 inner + 12 outer, like an RBMK assembly half). The bundle
 * fills with craft progress and is shipped when the craft completes; a weld flash marks every insertion.
 */
public class FuelAssemblyModel extends FactoryModel{
    public static final FuelAssemblyModel instance = new FuelAssemblyModel();
    static final float bx = 4.0f, by = -1.5f, pickX = -5.5f, pickY = -1.5f;
    static final float pinBase = 1.9f, pinLen = 3.2f, gripLow = pinBase + pinLen, gripHigh = 8.55f;
    static final float railZ = 9.0f, bridgeZ = 9.4f;
    static final int slots = 18;
    static final float[] sx = new float[slots], sy = new float[slots];
    static final int[] drawOrder = new int[slots];

    static{
        for(int i = 0; i < 6; i++){ sx[i] = bx + Mathf.cosDeg(i * 60f + 30f) * 0.95f; sy[i] = by + Mathf.sinDeg(i * 60f + 30f) * 0.95f; }
        for(int i = 0; i < 12; i++){ sx[6 + i] = bx + Mathf.cosDeg(i * 30f + 15f) * 1.75f; sy[6 + i] = by + Mathf.sinDeg(i * 30f + 15f) * 1.75f; }
        Integer[] idx = new Integer[slots];
        for(int i = 0; i < slots; i++) idx[i] = i;
        java.util.Arrays.sort(idx, (a, b) -> Float.compare(sy[b], sy[a]));
        for(int i = 0; i < slots; i++) drawOrder[i] = idx[i];
    }

    final Mesh pin = new Mesh(), unitPin = new Mesh(), bridge = new Mesh(), carriage = new Mesh(), gripper = new Mesh(), unitBox = new Mesh(), spark = new Mesh();

    public FuelAssemblyModel(){
        super("fuel-assembly-plant", new Color(1f, 0.70f, 0.28f));
        pin.color(Pal.zirc).style(metal, 0).at(0, 0, 0).lathe(8, 22.5f, 0, 0, 0.28f, 0, 0.28f, pinLen - 0.12f, 0.2f, pinLen, 0, pinLen);
        pin.color(Pal.pellet).style(0, 0).at(0, 0, 0).lathe(8, 22.5f, 0.285f, 0.25f, 0.29f, 0.25f, 0.29f, 0.45f, 0.285f, 0.45f, 0.285f, 0.25f);
        unitPin.color(Pal.zirc).style(metal, 0).at(0, 0, 0).lathe(8, 22.5f, 0.5f, 0f, 0.5f, 0.97f, 0.36f, 1f, 0, 1f);
        bridge.color(accent).style(metal, matPlate).at(0, 0, 0).bevel(-9.9f, -0.6f, 0f, 9.9f, 0.6f, 0.8f, 0.12f);
        bridge.color(Pal.darkSteel).style(metal, 0).box(-9.9f, -0.62f, 0.25f, 9.9f, -0.6f, 0.4f);
        for(int s = -1; s <= 1; s += 2) bridge.color(Pal.white).style(metal, 0).at(0, 0, 0).bevel(s * 9.6f - 0.6f, -0.9f, -0.1f, s * 9.6f + 0.6f, 0.9f, 1.05f, 0.12f);
        carriage.color(Pal.white).style(metal, matPlate).at(0, 0, 0).bevel(-0.9f, -0.8f, 0f, 0.9f, 0.8f, 0.9f, 0.12f);
        carriage.color(Pal.dark).style(metal, matFins).box(-0.6f, -0.5f, 0.9f, 0.6f, 0.5f, 1.2f);
        carriage.color(accent).style(emissive, 0).at(0.55f, -0.55f, 0.9f).cyl(8, 0.14f, 0f, 0.12f);
        gripper.color(Pal.offWhite).style(metal, 0).at(0, 0, 0).bevel(-0.45f, -0.45f, 0f, 0.45f, 0.45f, 0.42f, 0.08f);
        gripper.color(Pal.darkSteel).style(metal, 0).box(-0.42f, -0.12f, -0.5f, -0.28f, 0.12f, 0f, true);
        gripper.box(0.28f, -0.12f, -0.5f, 0.42f, 0.12f, 0f, true);
        unitBox.color(Pal.steel).style(metal, 0).at(0, 0, 0).box(-0.5f, -0.5f, 0f, 0.5f, 0.5f, 1f, false);
        spark.color(Pal.glowWhite).style(emissive, 0).at(0, 0, 0).lathe(8, 0, 0, 0, 0.42f, 0.02f, 0, 0.3f);
    }

    @Override
    public void buildStatic(Mesh m){
        deck(m);
        //gantry posts and rails
        for(int s = -1; s <= 1; s += 2){
            for(float py : new float[]{-8.0f, 3.0f}){
                m.color(Pal.white).style(metal, matPlate).at(0, 0, 0).cbevel(s * 9.6f, py, deckZ, 0.9f, 0.9f, railZ - deckZ, 0.12f);
                m.color(Pal.darkSteel).style(metal, 0).cbevel(s * 9.6f, py, deckZ, 1.4f, 1.4f, 0.3f, 0.08f);
            }
            m.color(Pal.offWhite).style(metal, 0).at(0, 0, 0).bevel(s * 9.6f - 0.4f, -8.6f, railZ, s * 9.6f + 0.4f, 3.6f, bridgeZ, 0.08f);
            m.color(Pal.yellow).style(0, matHazard).box(s * 9.6f - 0.42f, -8.62f, railZ + 0.05f, s * 9.6f + 0.42f, -8.6f, bridgeZ - 0.05f);
        }
        //bundle cradle: base, centre tube, spacer ring on three posts
        m.color(Pal.mid).style(metal, matPlate).at(bx, by, 0).lathe(28, 0, 0, deckZ, 2.5f, deckZ, 2.5f, pinBase - 0.1f, 2.3f, pinBase, 0, pinBase);
        m.color(Pal.graphite).style(0, 0).at(bx, by, pinBase).cyl(28, 2.1f, 0f, 0.01f);
        for(int i = 0; i < slots; i++) m.color(Pal.dark).style(0, 0).at(sx[i], sy[i], pinBase + 0.01f).cyl(8, 0.34f, 0f, 0.01f);
        m.color(Pal.steel).style(metal, 0).at(bx, by, 0).cyl(12, 0.32f, pinBase, 5.45f);
        for(int i = 0; i < 3; i++){
            float a = 90f + i * 120f;
            m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbox(bx + Mathf.cosDeg(a) * 2.45f, by + Mathf.sinDeg(a) * 2.45f, pinBase, 0.28f, 0.28f, 4.5f - pinBase);
        }
        m.color(Pal.white).style(metal, 0).at(bx, by, 0).ring(28, 2.15f, 2.6f, 4.25f, 4.5f);
        m.color(accent).style(0, 0).at(bx, by, 0).ring(28, 2.58f, 2.64f, 4.3f, 4.42f);
        //feeder with the next element rising out of it
        m.color(Pal.offWhite).style(metal, 0).at(pickX, pickY, 0).lathe(16, 0, 0.36f, deckZ, 0.75f, deckZ, 0.75f, 2.4f, 0.6f, 2.6f, 0.36f, 2.6f, 0.36f, deckZ);
        m.color(Pal.graphite).style(0, 0).at(pickX, pickY, 0).cyl(12, 0.37f, deckZ, 2.0f);
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbevel(pickX, pickY - 1.6f, deckZ, 1.6f, 1.3f, 0.9f, 0.1f);
        m.pipe(8, 0.22f, pickX - 0.8f, pickY + 0.2f, 1.7f, pickX - 3.0f, pickY + 3.4f, 1.7f);
        //element magazine
        m.color(Pal.white).style(metal, matPlate).at(0, 0, 0).cbevel(-7.0f, 4.0f, deckZ, 4.2f, 3.4f, 0.5f, 0.1f);
        for(int i = 0; i < 9; i++){
            float px = -8.2f + (i % 3) * 1.2f, pyy = 3.0f + (i / 3) * 1.0f;
            m.color(Pal.zirc).style(metal, 0).at(px, pyy, deckZ + 0.5f).lathe(8, 22.5f, 0, 0, 0.26f, 0, 0.26f, 2.7f, 0.18f, 2.8f, 0, 2.8f);
        }
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbox(-7.0f, 5.7f, deckZ + 0.5f, 4.2f, 0.25f, 2.0f);
        m.color(accent).style(0, 0).box(-9.1f, 5.56f, deckZ + 2.1f, -4.9f, 5.58f, deckZ + 2.35f);
        console(m, 7.2f, -8.6f);
        m.color(Pal.yellow).style(0, matHazard).at(0, 0, 0).box(-3.0f, -10.9f, deckZ, 3.0f, -10.2f, deckZ + 0.03f);
    }

    @Override
    public void buildEnvelope(Mesh m){
        //only the parts drawn before the front layer: installed / low elements
        m.at(bx, by, pinBase - 0.02f).cyl(24, 2.12f, 0f, pinLen + 0.12f);
        m.at(pickX, pickY, pinBase - 0.02f).cyl(12, 0.4f, 0f, pinLen + 0.12f);
    }

    @Override
    public void buildRest(Mesh m){
        for(int i = 0; i < 12; i++) m.add(pin, sx[i], sy[i], pinBase, 2, 0f);
        m.add(pin, pickX, pickY, pinBase, 2, 0f);
        m.add(bridge, 0, pickY, bridgeZ, 2, 0f);
        m.add(carriage, pickX, pickY, bridgeZ + 0.8f, 2, 0f);
        m.add(gripper, pickX, pickY, gripHigh, 2, 0f);
    }

    //---------------------------------------------------------------- motion
    float gx, gy, gz, carriedZ;
    boolean carrying, feederFull;
    float feederLen, weld;
    int filled, current;

    void solve(float progress){
        float p = Mathf.clamp(progress) * slots;
        current = Math.min((int)p, slots - 1);
        filled = current;
        float u = p - (int)p;
        if(progress >= 0.9999f){ u = 0.999f; }
        float tx = sx[current], ty = sy[current];
        weld = 0f;
        carrying = false;
        feederLen = pinLen;
        if(u < 0.12f){ gx = pickX; gy = pickY; gz = Mathf.lerp(gripHigh, gripLow, smooth(0f, 0.12f, u)); }
        else if(u < 0.20f){ gx = pickX; gy = pickY; gz = gripLow; }
        else if(u < 0.34f){ gx = pickX; gy = pickY; gz = Mathf.lerp(gripLow, gripHigh, smooth(0.20f, 0.34f, u)); carrying = true; feederLen = 0f; }
        else if(u < 0.58f){ float k = smooth(0.34f, 0.58f, u); gx = Mathf.lerp(pickX, tx, k); gy = Mathf.lerp(pickY, ty, k); gz = gripHigh; carrying = true; feederLen = 0f; }
        else if(u < 0.72f){ gx = tx; gy = ty; gz = Mathf.lerp(gripHigh, gripLow, smooth(0.58f, 0.72f, u)); carrying = true; feederLen = 0f; }
        else if(u < 0.80f){ gx = tx; gy = ty; gz = gripLow; filled = current + 1; weld = 1f - Math.abs((u - 0.76f) / 0.04f); feederLen = pinLen * smooth(0.72f, 1f, u) * 0.4f; }
        else if(u < 0.90f){ gx = tx; gy = ty; gz = Mathf.lerp(gripLow, gripHigh, smooth(0.80f, 0.90f, u)); filled = current + 1; feederLen = pinLen * smooth(0.72f, 1f, u); }
        else{ float k = smooth(0.90f, 1f, u); gx = Mathf.lerp(tx, pickX, k); gy = Mathf.lerp(ty, pickY, k); gz = gripHigh; filled = current + 1; feederLen = pinLen * smooth(0.72f, 1f, u); }
        carriedZ = gz - pinLen;
    }

    @Override
    public void drawLive(Building b){
        float wx = b.x, wy = b.y;
        solve(progress(b));
        boolean low = carrying && carriedZ < pinBase + pinLen + 0.1f;
        //installed elements north of the carried one, the carried one (while low), then the rest
        int k = 0;
        for(; k < slots; k++){
            int i = drawOrder[k];
            if(low && sy[i] <= gy) break;
            if(i < filled && !(carrying && i == current)) drawPin(sx[i], sy[i], pinBase, wx, wy);
        }
        if(low) drawPin(gx, gy, carriedZ, wx, wy);
        for(; k < slots; k++){
            int i = drawOrder[k];
            if(i < filled && !(carrying && i == current)) drawPin(sx[i], sy[i], pinBase, wx, wy);
        }
        //feeder: next element pushed up out of the feeder
        if(feederLen > 0.05f){
            Live.link(pickX, pickY, pinBase, pickX, pickY, pinBase + feederLen, 0.56f, 0.56f);
            Live.draw(unitPin, cam, wx, wy);
        }
    }

    void drawPin(float x, float y, float z, float wx, float wy){
        Live.pose(x, y, z, 2, 0f);
        Live.draw(pin, cam, wx, wy);
    }

    @Override
    public void drawOver(Building b){
        float wx = b.x, wy = b.y;
        boolean high = carrying && carriedZ >= pinBase + pinLen + 0.1f;
        if(high) drawPin(gx, gy, carriedZ, wx, wy);
        if(weld > 0f){
            float f = weld * (0.7f + 0.3f * Mathf.sin(Time.time * 1.7f));
            Live.glowR = 1f; Live.glowG = 0.85f + 0.15f * f; Live.glowB = 0.5f + 0.5f * f; Live.alpha = Mathf.clamp(f);
            Live.pose(gx, gy, pinBase + pinLen + 0.05f, 2, Time.time * 9f);
            Live.draw(spark, cam, wx, wy);
            Live.resetTint();
        }
        //gripper, telescopic arm, bridge, carriage
        Live.pose(gx, gy, gz, 2, 0f);
        Live.draw(gripper, cam, wx, wy);
        Live.link(gx, gy, gz + 0.42f, gx, gy, bridgeZ + 0.1f, 0.36f, 0.36f);
        Live.draw(unitBox, cam, wx, wy);
        Live.link(gx, gy, Math.max(gz + 0.42f, bridgeZ - 1.2f), gx, gy, bridgeZ + 0.1f, 0.52f, 0.52f);
        Live.draw(unitBox, cam, wx, wy);
        Live.pose(0, gy, bridgeZ, 2, 0f);
        Live.draw(bridge, cam, wx, wy);
        Live.glowR = 1f; Live.glowG = Mathf.sin(Time.time * 0.2f) > 0 ? 0.75f : 0.35f; Live.glowB = 0.3f;
        Live.pose(gx, gy, bridgeZ + 0.8f, 2, 0f);
        Live.draw(carriage, cam, wx, wy);
    }
}
