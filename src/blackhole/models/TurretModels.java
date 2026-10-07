package blackhole.models;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import blackhole.g3d.Kit.*;
import mindustry.graphics.*;
import mindustry.world.blocks.defense.turrets.Turret.*;

import static blackhole.g3d.Kit.*;
import static blackhole.g3d.Mesh.*;

/**
 * Every turret of the mod as a real 3D model (19 blocks, 16 designs; Serpulo/Erekir twins share geometry and
 * differ in faction materials). Bases are baked with AO; heads are baked at 16 headings (see {@link TurretModel}).
 */
public final class TurretModels{
    private TurretModels(){}

    public static SimpleTurret accretionCannon, frameDragger, hawkingEmitter, quasarLance, eventHorizon,
        loom, well, boreline, crater, halo, needlefall, prismLance, skyNet, prismPylon, arcMortar,
        lumenSpark, shardVolley, tempestCoil;

    public static SimpleTurret[] all(){
        return new SimpleTurret[]{accretionCannon, frameDragger, hawkingEmitter, quasarLance, eventHorizon,
            loom, well, boreline, crater, halo, needlefall, prismLance, skyNet, prismPylon, arcMortar,
            lumenSpark, shardVolley, tempestCoil};
    }

    // ------------------------------------------------------------------ shared base

    /** Standard mount: deck, octagonal armour skirt, bearing ring; corner equipment by style. Returns ring top. */
    static float mount(Mesh m, Style s, int size, float ringR, float top, int extras){
        float half = size * 4f, dh = 1.0f + size * 0.12f;
        deck(m, s, half, dh);
        hull2(m, s).at(0, 0, 0).lathe(8, 22.5f, ringR + 2.0f, dh, ringR + 1.9f, dh + 0.4f, ringR + 1.1f, top - 0.6f, ringR + 0.6f, top - 0.45f, 0.001f, top - 0.45f);
        ring(m, s, ringR, top - 0.9f, top);
        float c = half - 2.0f;
        //corner equipment: vents on two corners, ammo/power boxes on the others
        for(int i = 0; i < 4; i++){
            float sx = (i & 1) == 0 ? -1 : 1, sy = (i & 2) == 0 ? -1 : 1;
            if(size <= 2){
                steel(m, s).at(sx * c, sy * c, dh).cyl(8, 0.55f, 0f, 0.5f);
                continue;
            }
            if(((i + extras) & 1) == 0) vent(m, s, sx * (c - 0.3f), sy * (c - 0.3f), dh, 2.4f, 2.4f, 1.0f + size * 0.12f);
            else{
                hull(m, s).at(0, 0, 0).cbevel(sx * (c - 0.3f), sy * (c - 0.3f), dh, 2.4f, 2.4f, 1.3f + size * 0.12f, 0.15f);
                glow(m, s).box(sx * (c - 0.3f) - 0.8f, sy * (c - 0.3f) - 1.22f, dh + 0.5f, sx * (c - 0.3f) + 0.8f, sy * (c - 0.3f) - 1.2f, dh + 0.75f);
            }
        }
        //hazard band on the south apron
        hazard(m).at(0, 0, 0).box(-half * 0.35f, -half + 0.72f, dh, half * 0.35f, -half + 1.35f, dh + 0.03f);
        m.at(0, 0, 0);
        return top;
    }

    /** Barrel along +y from y0 to y1 at (x, z): jacket, muzzle brake, emissive bore. */
    static void barrel(Mesh m, Style s, float x, float z, float r, float y0, float y1, boolean brake){
        steel(m, s).axis(x, y0, z, x, y1, z).cyl(10, r, 0f, y1 - y0);
        dark(m, s).axis(x, y0, z, x, y1, z).cyl(10, r * 1.35f, 0f, (y1 - y0) * 0.28f);
        if(brake){
            dark(m, s).axis(x, y1 - r * 2.2f, z, x, y1 + 0.2f, z).cyl(8, r * 1.3f, 0f, r * 2.4f);
        }
        glow(m, s).axis(x, y1 + 0.21f, z, x, y1 + 0.25f, z).cyl(8, r * 0.55f, 0f, 0.04f);
        m.at(0, 0, 0);
    }

    static float heatGlow(TurretBuild b){ return b.heat * 0.4f; }

    private static final float[] ehTmp = new float[2];

    static float power(TurretBuild b){ return Mathf.clamp(b.efficiency) * 0.45f; }

    // ------------------------------------------------------------------ designs

    public static void load(){
        // ============ Singularity (black hole arsenal) ============
        accretionCannon = new SimpleTurret("accretion-cannon", 3, sing).headZ(3.9f).base(m -> {
            mount(m, sing, 3, 5.6f, 3.9f, 0);
            conduit(m, sing, -9f, 6f, 1.6f, -5f, 3f, 3.0f, 0.35f);
            conduit(m, sing, 9f, 6f, 1.6f, 5f, 3f, 3.0f, 0.35f);
        }).head(m -> {
            //armoured mantlet with an accretion ring round the breech, twin heavy barrels
            hull(m, sing).hexa(false, -4.2f, -4.6f, 0f, 4.2f, -4.6f, 0f, 3.6f, 3.6f, 0f, -3.6f, 3.6f, 0f,
                -3.4f, -3.8f, 3.0f, 3.4f, -3.8f, 3.0f, 2.8f, 2.8f, 2.4f, -2.8f, 2.8f, 2.4f);
            dark(m, sing).box(-3.0f, -5.2f, 0.4f, 3.0f, -4.4f, 2.4f);
            fins(m, sing).box(-2.4f, -5.22f, 0.8f, 2.4f, -5.2f, 2.0f);
            torus(m, sing.glow, emissive, 0, 1.2f, 1.2f, 3.9f, 0.35f, 0.5f, 16);
            dark(m, sing).at(0, 0, 0).cbox(0, 2.2f, 1.2f, 6.4f, 2.6f, 2.2f);
            barrel(m, sing, -1.7f, 1.3f, 0.75f, 2.8f, 11.0f, true);
            barrel(m, sing, 1.7f, 1.3f, 0.75f, 2.8f, 11.0f, true);
            glow2(m, sing).box(-0.4f, -3.0f, 3.0f, 0.4f, 1.8f, 3.05f);
            team(m).box(-3.45f, -3.2f, 2.0f, -3.35f, 1.8f, 2.6f);
            team(m).box(3.35f, -3.2f, 2.0f, 3.45f, 1.8f, 2.6f);
        }).muzzles(-1.7f, 11.3f, 1.3f, 1.7f, 11.3f, 1.3f).glow(TurretModels::heatGlow);
        accretionCannon.heatColor = sing.glow;

        frameDragger = new SimpleTurret("frame-dragger", 2, sing).headZ(2.7f).base(m -> {
            mount(m, sing, 2, 3.7f, 2.7f, 0);
        }).head(m -> {
            //compact body, spinning gravitic drum at the rear, split twin barrels
            hull(m, sing).at(0, 0, 0).cbevel(0, 0, 0, 5.0f, 5.6f, 2.2f, 0.4f);
            dark(m, sing).at(0, -2.9f, 1.3f).rot(1, 90).cyl(12, 1.5f, -2.2f, 2.2f);
            glow2(m, sing).at(0, -2.9f, 1.3f).rot(1, 90).ring(12, 1.52f, 1.6f, -0.6f, 0.6f);
            barrel(m, sing, -1.1f, 1.1f, 0.5f, 2.0f, 7.2f, false);
            barrel(m, sing, 1.1f, 1.1f, 0.5f, 2.0f, 7.2f, false);
            trim(m, sing).box(-0.3f, -1.8f, 2.2f, 0.3f, 2.4f, 2.4f);
        }).muzzles(-1.1f, 7.4f, 1.1f, 1.1f, 7.4f, 1.1f).glow(TurretModels::heatGlow);

        hawkingEmitter = new SimpleTurret("hawking-emitter", 3, sing).headZ(3.8f).base(m -> {
            mount(m, sing, 3, 5.4f, 3.8f, 1);
            tank(m, sing, -8.6f, 7.8f, 1.4f, 1.5f, 3.4f, true);
            tank(m, sing, 8.6f, 7.8f, 1.4f, 1.5f, 3.4f, true);
        }).head(m -> {
            //emitter crystal held between two swept blades
            hull2(m, sing).at(0, 0, 0).lathe(8, 22.5f, 3.6f, 0f, 3.4f, 1.2f, 2.4f, 2.2f, 0.001f, 2.4f);
            for(int sd = -1; sd <= 1; sd += 2){
                float x = sd * 3.2f;
                hull(m, sing).hexa(false, x - 0.6f * sd, -3.4f, 0.4f, x + 0.7f * sd, -2.8f, 0.4f, x + 0.5f * sd, 7.2f, 0.9f, x - 0.1f * sd, 7.8f, 0.9f,
                    x - 0.5f * sd, -3.0f, 3.2f, x + 0.5f * sd, -2.5f, 3.0f, x + 0.3f * sd, 6.4f, 2.0f, x - 0.1f * sd, 7.0f, 2.1f);
                glow2(m, sing).box(x - 0.08f, -1.8f, 3.1f, x + 0.08f, 5.6f, 3.16f);
            }
            dark(m, sing).axis(0, 0.5f, 1.8f, 0, 6.5f, 1.8f).cyl(8, 0.9f, 0f, 6.0f);
            crystal(m, sing.glow2, true, 0, 6.6f, 1.8f, 0.9f, 2.4f, -90f, 0f);
        }).muzzles(0f, 9.2f, 1.8f).glow(b -> power(b) + b.heat * 0.4f);
        hawkingEmitter.glowColor = sing.glow2;
        hawkingEmitter.heatColor = sing.glow2;

        quasarLance = new SimpleTurret("quasar-lance", 4, sing).headZ(4.6f).base(m -> {
            mount(m, sing, 4, 7.0f, 4.6f, 0);
            conduit(m, sing, -12f, -10f, 1.8f, -6f, -5f, 3.6f, 0.45f);
            conduit(m, sing, 12f, -10f, 1.8f, 6f, -5f, 3.6f, 0.45f);
        }).head(m -> {
            //heavy beam projector: long armoured housing, magnetic focusing rings, side vents
            hull(m, sing).hexa(false, -4.8f, -6.4f, 0f, 4.8f, -6.4f, 0f, 3.8f, 6.0f, 0f, -3.8f, 6.0f, 0f,
                -4.0f, -5.6f, 3.6f, 4.0f, -5.6f, 3.6f, 3.0f, 5.2f, 3.0f, -3.0f, 5.2f, 3.0f);
            for(int sd = -1; sd <= 1; sd += 2){
                vent(m, sing, sd * 5.6f, -2.6f, 0.4f, 1.6f, 5.2f, 2.4f);
                glow(m, sing).box(sd * 6.42f - 0.02f, -4.6f, 1.0f, sd * 6.42f + 0.02f, -0.6f, 2.2f);
            }
            dark(m, sing).axis(0, 5.0f, 1.8f, 0, 15.0f, 1.8f).cyl(12, 1.4f, 0f, 10f);
            for(int k = 0; k < 4; k++){
                float y = 7.0f + k * 2.2f;
                steel(m, sing).axis(0, y, 1.8f, 0, y + 0.6f, 1.8f).cyl(12, 2.2f - k * 0.18f, 0f, 0.6f);
                glow(m, sing).axis(0, y + 0.6f, 1.8f, 0, y + 0.7f, 1.8f).ring(12, 1.3f, 1.9f - k * 0.15f, 0f, 0.1f);
            }
            glow(m, sing).axis(0, 15.0f, 1.8f, 0, 15.1f, 1.8f).cyl(12, 1.0f, 0f, 0.1f);
            m.at(0, 0, 0); //reset the frame left by the axis() calls above
            team(m).box(-4.05f, -4.8f, 2.6f, -3.95f, 2.0f, 3.2f);
            team(m).box(3.95f, -4.8f, 2.6f, 4.05f, 2.0f, 3.2f);
        }).muzzles(0f, 15.4f, 1.8f).glow(b -> power(b) + b.heat * 0.5f);

        eventHorizon = new SimpleTurret("event-horizon", 5, sing).headZ(5.2f).base(m -> {
            mount(m, sing, 5, 8.8f, 5.2f, 0);
            for(int i = 0; i < 4; i++){
                float a = 45f + i * 90f;
                pylon(m, sing, Mathf.cosDeg(a) * 15.5f, Mathf.sinDeg(a) * 15.5f, 1.6f, 0.9f, 5.5f, true);
            }
        }).head(m -> {
            //two cradle arms around an open containment throat where a live singularity is drawn
            hull2(m, sing).at(0, 0, 0).lathe(10, 18f, 7.4f, 0f, 7.2f, 1.2f, 5.8f, 2.4f, 0.001f, 2.6f);
            for(int sd = -1; sd <= 1; sd += 2){
                float x = sd * 5.4f;
                hull(m, sing).hexa(false, x - 1.8f * sd, -6.4f, 0.6f, x + 1.8f * sd, -5.4f, 0.6f, x + 1.4f * sd, 11.4f, 1.6f, x - 0.8f * sd, 12.4f, 1.6f,
                    x - 1.4f * sd, -5.8f, 5.6f, x + 1.4f * sd, -5.0f, 5.2f, x + 1.0f * sd, 10.2f, 3.6f, x - 0.6f * sd, 11.0f, 3.8f);
                glow(m, sing).box(x - 0.1f, -3.0f, 5.45f, x + 0.1f, 8.0f, 5.5f);
                fins(m, sing).box(x + sd * 1.1f - 0.3f, -4.6f, 2.0f, x + sd * 1.1f + 0.3f, 4.0f, 4.6f);
            }
            dark(m, sing).at(0, 0, 0).cbox(0, -5.2f, 1.0f, 8.2f, 3.0f, 3.8f);
            fins(m, sing).box(-3.2f, -6.72f, 1.6f, 3.2f, -6.7f, 4.2f);
            //throat collar where the singularity sits (y = 7)
            torus(m, sing.dark, metal, 0, 7.0f, 2.8f, 3.4f, 0.8f, 1.2f, 16);
            torus(m, sing.glow, emissive, 0, 7.0f, 3.5f, 3.1f, 0.3f, 0.3f, 16);
            team(m).box(-8.0f, -2.0f, 3.0f, -7.9f, 4.0f, 3.8f);
            team(m).box(7.9f, -2.0f, 3.0f, 8.0f, 4.0f, 3.8f);
        }).muzzles(0f, 7.0f, 2.8f).glow(TurretModels::heatGlow).over((t, b) -> {
            //live miniature singularity in the throat (v6.1 renderer; no per-frame allocation)
            float[] p = ehTmp;
            t.project(b.rotation, 0f, 7.0f - b.curRecoil, 2.8f, p);
            float warm = 0.55f + 0.45f * Mathf.clamp(b.heat + b.efficiency * 0.3f);
            Draw.z(Layer.turret + 0.5f);
            blackhole.BlackHoleRenderer.draw(b.x + p[0], b.y + p[1], 1.9f * warm, 0.72f, Time.time + b.id * 13f, warm);
            Draw.reset();
        });
        eventHorizon.heatColor = sing.glow;

        // ============ Aegis arsenal ============
        loom = loom("loom", serpulo);
        well = well("well", serpulo);

        boreline = new SimpleTurret("boreline", 2, serpulo).headZ(2.7f).base(m -> mount(m, serpulo, 2, 3.7f, 2.7f, 0)).head(m -> {
            hull(m, serpulo).hexa(false, -2.8f, -3.2f, 0f, 2.8f, -3.2f, 0f, 2.2f, 2.2f, 0f, -2.2f, 2.2f, 0f,
                -2.3f, -2.7f, 2.2f, 2.3f, -2.7f, 2.2f, 1.7f, 1.7f, 1.8f, -1.7f, 1.7f, 1.8f);
            for(int sd = -1; sd <= 1; sd += 2){
                //rail barrels with drill-fluted jackets
                steel(m, serpulo).axis(sd * 1.2f, 1.0f, 1.0f, sd * 1.2f, 8.4f, 1.0f).cyl(6, 0.55f, 0f, 7.4f);
                trim(m, serpulo).axis(sd * 1.2f, 3.0f, 1.0f, sd * 1.2f, 4.0f, 1.0f).cyl(6, 0.72f, 0f, 1.0f);
                trim(m, serpulo).axis(sd * 1.2f, 5.6f, 1.0f, sd * 1.2f, 6.4f, 1.0f).cyl(6, 0.68f, 0f, 0.8f);
                glow(m, serpulo).axis(sd * 1.2f, 8.4f, 1.0f, sd * 1.2f, 8.45f, 1.0f).cyl(6, 0.3f, 0f, 0.05f);
            }
            dark(m, serpulo).at(0, 0, 0).cbox(0, -3.4f, 0.4f, 3.6f, 0.6f, 1.4f);
        }).muzzles(-1.2f, 8.6f, 1.0f, 1.2f, 8.6f, 1.0f).glow(TurretModels::heatGlow);
        boreline.heatColor = Color.valueOf("82cbaa");

        needlefall = new SimpleTurret("needlefall", 2, serpulo).headZ(2.6f).base(m -> mount(m, serpulo, 2, 3.5f, 2.6f, 0)).head(m -> {
            hull(m, serpulo).at(0, 0, 0).lathe(8, 22.5f, 2.8f, 0f, 2.6f, 1.2f, 1.6f, 2.0f, 0.001f, 2.1f);
            for(int sd = -1; sd <= 1; sd += 2){
                steel(m, serpulo).axis(sd * 0.9f, 0.5f, 1.1f, sd * 0.9f, 8.0f, 1.1f).cyl(6, 0.3f, 0f, 7.5f);
                dark(m, serpulo).axis(sd * 0.9f, 0.5f, 1.1f, sd * 0.9f, 3.0f, 1.1f).cyl(6, 0.5f, 0f, 2.5f);
            }
            hull2(m, serpulo).at(0, 0, 0).cbevel(0, -2.6f, 0.2f, 3.2f, 1.6f, 1.6f, 0.2f);
            glow(m, serpulo).box(-1.0f, -3.42f, 0.8f, 1.0f, -3.4f, 1.2f);
        }).muzzles(-0.9f, 8.1f, 1.1f, 0.9f, 8.1f, 1.1f).glow(TurretModels::heatGlow);
        needlefall.heatColor = Color.valueOf("79cbd6");

        skyNet = new SimpleTurret("sky-net", 2, serpulo).headZ(2.6f).base(m -> mount(m, serpulo, 2, 3.6f, 2.6f, 0)).head(m -> {
            //flak box: 2x2 short tubes raked skywards, radar fin at the back
            hull(m, serpulo).at(0, 0, 0).cbevel(0, 0, 0, 5.2f, 4.4f, 2.2f, 0.35f);
            for(int i = 0; i < 4; i++){
                float x = (i & 1) == 0 ? -1.1f : 1.1f, z = (i & 2) == 0 ? 1.2f : 2.4f;
                steel(m, serpulo).axis(x, 1.0f, z - 0.4f, x, 5.2f, z + 0.9f).cyl(8, 0.5f, 0f, 4.4f);
                dark(m, serpulo).axis(x, 5.0f, z + 0.84f, x, 5.4f, z + 0.96f).cyl(8, 0.62f, 0f, 0.42f);
            }
            m.at(0, 0, 0); //reset the frame left by the axis() calls above
            dark(m, serpulo).box(-0.15f, -2.8f, 2.2f, 0.15f, -1.4f, 4.2f);
            glow(m, serpulo).box(-0.17f, -2.4f, 3.6f, 0.17f, -1.8f, 4.0f);
        }).muzzles(-1.1f, 5.4f, 1.7f, 1.1f, 5.4f, 1.7f, -1.1f, 5.4f, 3.0f, 1.1f, 5.4f, 3.0f).glow(TurretModels::heatGlow);
        skyNet.heatColor = Color.valueOf("e9c87c");

        crater = new SimpleTurret("crater", 3, serpulo).headZ(3.6f).base(m -> {
            mount(m, serpulo, 3, 5.4f, 3.6f, 1);
            for(int i = 0; i < 3; i++) steel(m, serpulo).at(-9.2f + i * 1.3f, 9.0f, 1.4f).cyl(8, 0.55f, 0f, 1.1f);
        }).head(m -> {
            //short, fat mortar tube raised 35 degrees in a trunnion yoke with recoil cylinders
            hull2(m, serpulo).at(0, 0, 0).lathe(8, 22.5f, 4.2f, 0f, 4.0f, 1.0f, 3.0f, 1.8f, 0.001f, 1.9f);
            for(int sd = -1; sd <= 1; sd += 2){
                hull(m, serpulo).at(0, 0, 0).cbevel(sd * 3.0f, -0.4f, 1.0f, 1.2f, 4.2f, 3.6f, 0.2f);
                steel(m, serpulo).axis(sd * 2.0f, -2.2f, 1.8f, sd * 2.0f, 1.8f, 3.6f).cyl(6, 0.35f, 0f, 4.2f);
            }
            steel(m, serpulo).axis(0, -2.4f, 1.6f, 0, 6.6f, 7.9f).cyl(12, 1.8f, 0f, 11.0f);
            dark(m, serpulo).axis(0, -2.4f, 1.6f, 0, 6.6f, 7.9f).cyl(12, 2.2f, 1.5f, 4.5f);
            dark(m, serpulo).axis(0, -2.4f, 1.6f, 0, 6.6f, 7.9f).cyl(12, 2.1f, 9.6f, 11.0f);
            glow(m, serpulo).axis(0, -2.4f, 1.6f, 0, 6.6f, 7.9f).cyl(12, 1.1f, 11.0f, 11.05f);
        }).muzzles(0f, 6.7f, 8.0f).glow(TurretModels::heatGlow);
        crater.heatColor = Color.valueOf("eaa06c");

        halo = new SimpleTurret("halo", 3, serpulo).headZ(3.6f).base(m -> mount(m, serpulo, 3, 5.4f, 3.6f, 0)).head(m -> {
            //missile rack: 3 cells in an armoured box, halo ring around the rack
            hull(m, serpulo).at(0, 0, 0).cbevel(0, 0.4f, 0, 7.2f, 7.0f, 3.2f, 0.4f);
            for(int i = 0; i < 3; i++){
                float x = -2.3f + i * 2.3f;
                dark(m, serpulo).at(x, 3.95f, 1.6f).rot(0, -90).cyl(10, 0.9f, 0f, 0.05f);
                glow(m, serpulo).at(x, 3.98f, 1.6f).rot(0, -90).cyl(10, 0.45f, 0f, 0.02f);
            }
            torus(m, serpulo.trim, metal, 0, 0.4f, 1.8f, 5.0f, 0.35f, 0.5f, 16);
            glow2(m, serpulo).box(-3.0f, -3.12f, 1.2f, 3.0f, -3.1f, 1.6f);
            team(m).box(-3.65f, -1.6f, 2.2f, -3.55f, 2.8f, 2.8f);
            team(m).box(3.55f, -1.6f, 2.2f, 3.65f, 2.8f, 2.8f);
        }).muzzles(-2.3f, 4.1f, 1.6f, 0f, 4.1f, 1.6f, 2.3f, 4.1f, 1.6f).glow(TurretModels::heatGlow);
        halo.heatColor = Color.valueOf("bd9ce7");

        prismLance = new SimpleTurret("prism-lance", 3, serpulo).headZ(3.7f).base(m -> {
            mount(m, serpulo, 3, 5.4f, 3.7f, 1);
            crystal(m, Color.valueOf("b6acff"), true, -9.2f, 9.2f, 1.4f, 0.6f, 2.6f, 12f, 30f);
            crystal(m, Color.valueOf("b6acff"), true, 9.2f, 9.2f, 1.4f, 0.6f, 2.6f, 12f, -30f);
        }).head(m -> {
            //long prismatic lance: focusing rings along a slim spine, big prism at the breech
            hull(m, serpulo).hexa(false, -3.0f, -4.6f, 0f, 3.0f, -4.6f, 0f, 2.2f, 3.0f, 0f, -2.2f, 3.0f, 0f,
                -2.4f, -4.0f, 2.6f, 2.4f, -4.0f, 2.6f, 1.6f, 2.4f, 2.2f, -1.6f, 2.4f, 2.2f);
            crystal(m, Color.valueOf("c7bfff"), false, 0, -2.2f, 2.3f, 1.2f, 3.6f, 0f, 0f);
            steel(m, serpulo).axis(0, 2.0f, 1.3f, 0, 11.4f, 1.3f).cyl(8, 0.55f, 0f, 9.4f);
            for(int k = 0; k < 3; k++){
                float y = 4.2f + k * 2.4f;
                trim(m, serpulo).axis(0, y, 1.3f, 0, y + 0.4f, 1.3f).cyl(8, 1.5f - k * 0.2f, 0f, 0.4f);
                m.color(Color.valueOf("b6acff")).style(emissive, 0).axis(0, y + 0.4f, 1.3f, 0, y + 0.45f, 1.3f).ring(8, 0.6f, 1.2f - k * 0.2f, 0f, 0.05f);
            }
        }).muzzles(0f, 11.6f, 1.3f).glow(b -> power(b) + b.heat * 0.5f);
        prismLance.glowColor = Color.valueOf("b6acff");
        prismLance.heatColor = Color.valueOf("b6acff");

        // ============ Aurelia ============
        prismPylon = new SimpleTurret("prism-pylon", 3, aurelia).headZ(4.2f).base(m -> {
            mount(m, aurelia, 3, 5.2f, 4.2f, 0);
            for(int i = 0; i < 4; i++){
                float a = 45f + i * 90f;
                crystal(m, aurelia.glow, false, Mathf.cosDeg(a) * 9.6f, Mathf.sinDeg(a) * 9.6f, 1.3f, 0.5f, 2.2f, 18f, a - 90f);
            }
        }).head(m -> {
            //pearl housing cradling a large lumen crystal that fires along +y
            hull(m, aurelia).at(0, 0, 0).lathe(8, 22.5f, 4.0f, 0f, 3.8f, 1.4f, 2.4f, 2.6f, 0.001f, 2.8f);
            for(int sd = -1; sd <= 1; sd += 2){
                hull2(m, aurelia).hexa(false, sd * 2.2f, -2.2f, 0.8f, sd * 3.6f, -1.4f, 0.8f, sd * 2.6f, 5.8f, 1.6f, sd * 1.8f, 6.2f, 1.6f,
                    sd * 2.0f, -1.8f, 3.6f, sd * 3.2f, -1.2f, 3.4f, sd * 2.2f, 4.8f, 2.8f, sd * 1.6f, 5.2f, 2.9f);
                trim(m, aurelia).box(sd * 3.2f - 0.1f, -1.0f, 2.4f, sd * 3.2f + 0.1f, 3.6f, 2.6f);
            }
            crystal(m, aurelia.glow, true, 0, -1.4f, 2.4f, 1.2f, 8.4f, -90f, 0f);
        }).muzzles(0f, 7.2f, 2.4f).glow(b -> power(b) + b.heat * 0.5f);

        arcMortar = new SimpleTurret("arc-mortar", 4, aurelia).headZ(4.6f).base(m -> {
            mount(m, aurelia, 4, 6.8f, 4.6f, 1);
            for(int i = 0; i < 2; i++) pylon(m, aurelia, i == 0 ? -12.4f : 12.4f, 12.4f, 1.5f, 0.7f, 4.4f, true);
        }).head(m -> {
            //pearl-white mortar raised in a split yoke with a concord-gold resonance core at the breech
            hull2(m, aurelia).at(0, 0, 0).lathe(10, 18f, 5.6f, 0f, 5.4f, 1.2f, 4.0f, 2.2f, 0.001f, 2.4f);
            for(int sd = -1; sd <= 1; sd += 2){
                hull(m, aurelia).at(0, 0, 0).cbevel(sd * 3.9f, -0.4f, 1.2f, 1.4f, 5.4f, 4.6f, 0.25f);
                trim(m, aurelia).box(sd * 4.62f - 0.03f, -2.4f, 2.4f, sd * 4.62f + 0.03f, 1.8f, 4.8f);
            }
            hull(m, aurelia).axis(0, -3.2f, 2.2f, 0, 8.0f, 9.6f).lathe(12, 0, 2.6f, 0f, 2.4f, 2.0f, 2.1f, 11.6f, 2.4f, 12.4f, 1.4f, 12.6f, 0.001f, 12.6f);
            m.color(Color.valueOf("ffe29a")).style(emissive, 0).axis(0, -3.2f, 2.2f, 0, 8.0f, 9.6f).cyl(12, 2.62f, 3.2f, 4.0f);
            glow2(m, aurelia).axis(0, -3.2f, 2.2f, 0, 8.0f, 9.6f).cyl(12, 1.3f, 12.6f, 12.65f);
            m.color(Color.valueOf("ffe29a")).style(emissive, 0).at(0, -4.6f, 1.2f).lathe(8, 0, 1.3f, 0f, 1.5f, 1.2f, 0.001f, 2.6f);
        }).muzzles(0f, 8.1f, 9.7f).glow(b -> power(b) + b.heat * 0.5f);
        arcMortar.heatColor = Color.valueOf("ffe29a");
        arcMortar.glowColor = Color.valueOf("ffe29a");

        //Lumen spark: the first Aurelia turret. A low pearl drum with one short emitter; plain and readable.
        lumenSpark = new SimpleTurret("lumen-spark", 2, aurelia).headZ(2.6f).base(m -> mount(m, aurelia, 2, 3.4f, 2.6f, 0)).head(m -> {
            hull(m, aurelia).at(0, 0, 0).lathe(8, 22.5f, 2.9f, 0f, 2.8f, 1.1f, 2.0f, 1.9f, 0.001f, 2.0f);
            trim(m, aurelia).at(0, 0, 0).ring(8, 2.82f, 2.95f, 0.5f, 0.75f);
            hull2(m, aurelia).at(0, 0, 0).cbevel(0, 1.8f, 0.5f, 1.8f, 2.6f, 1.4f, 0.2f);
            barrel(m, aurelia, 0, 1.2f, 0.45f, 2.6f, 5.4f, false);
            glow(m, aurelia).at(0, -1.0f, 1.95f).cyl(8, 0.55f, 0f, 0.2f);
        }).muzzles(0f, 5.6f, 1.2f).glow(b -> power(b) + b.heat * 0.6f);

        //Shard volley: twin-barrel ammo turret (aurite / resonance shard slugs); a split breech between two barrels.
        shardVolley = new SimpleTurret("shard-volley", 2, aurelia).headZ(2.7f).base(m -> {
            mount(m, aurelia, 2, 3.6f, 2.7f, 0);
            hull(m, aurelia).at(0, 0, 0).cbevel(-5.2f, -5.2f, 1.2f, 1.6f, 1.6f, 1.2f, 0.15f);
            glow2(m, aurelia).box(-5.8f, -6.02f, 1.6f, -4.6f, -6.0f, 2.0f);
        }).head(m -> {
            hull2(m, aurelia).at(0, 0, 0).lathe(8, 22.5f, 3.1f, 0f, 3.0f, 1.0f, 2.3f, 2.0f, 0.001f, 2.1f);
            hull(m, aurelia).at(0, 0, 0).cbevel(0, 0.6f, 0.4f, 3.4f, 3.6f, 1.9f, 0.25f);
            dark(m, aurelia).box(-0.25f, -1.0f, 1.0f, 0.25f, 2.2f, 2.35f);
            barrel(m, aurelia, -1.0f, 1.25f, 0.42f, 2.2f, 6.2f, true);
            barrel(m, aurelia, 1.0f, 1.25f, 0.42f, 2.2f, 6.2f, true);
            glow2(m, aurelia).box(-1.5f, -1.4f, 2.32f, 1.5f, -1.1f, 2.4f);
        }).muzzles(-1.0f, 6.45f, 1.25f, 1.0f, 6.45f, 1.25f).glow(b -> b.heat * 0.6f);

        //v7.6 Tempest coil: no barrel at all. Three insulator stacks carry a floating emitter ring that the bolt
        //leaves from, so the chain weapon reads as a coil rather than as another gun.
        tempestCoil = new SimpleTurret("tempest-coil", 2, aurelia).headZ(2.8f).base(m -> {
            mount(m, aurelia, 2, 3.4f, 2.8f, 1);
            hull2(m, aurelia).at(0, 0, 0).cbevel(-5.4f, -5.4f, 1.24f, 1.8f, 1.8f, 1.1f, 0.15f);
        }).head(m -> {
            dark(m, aurelia).at(0, 0, 0).lathe(8, 22.5f, 2.6f, 0f, 2.5f, 0.8f, 1.9f, 1.2f, 1.9f, 1.4f, 0.001f, 1.5f);
            for(int i = 0; i < 3; i++){
                float a = 90f + i * 120f, c = Mathf.cosDeg(a), sn = Mathf.sinDeg(a);
                //insulator stack: three discs on a post
                steel(m, aurelia).at(c * 1.5f, sn * 1.5f, 1.4f).cyl(6, 0.3f, 0f, 2.6f);
                for(int k = 0; k < 3; k++){
                    trim(m, aurelia).at(c * 1.5f, sn * 1.5f, 1.8f + k * 0.8f).ring(8, 0.34f, 0.78f, 0f, 0.18f);
                }
            }
            m.at(0, 0, 0);
            trim(m, aurelia).at(0, 0, 4.0f).ring(10, 1.5f, 2.0f, 0f, 0.3f);
            glow(m, aurelia).at(0, 0, 4.3f).ring(10, 1.55f, 1.95f, 0f, 0.14f);
            glow2(m, aurelia).at(0, 1.1f, 4.45f).lathe(6, 30f, 0.4f, 0f, 0.001f, 0.7f);
        }).muzzles(0f, 1.1f, 5.1f).glow(b -> power(b) * 0.6f + b.heat);
    }

    /** Loom: tri-barrel weaver on a rotating spindle. */
    static SimpleTurret loom(String name, Style s){
        return new SimpleTurret(name, 3, s).headZ(3.7f).base(m -> {
            mount(m, s, 3, 5.4f, 3.7f, 0);
            tank(m, s, -9.0f, -8.4f, 1.4f, 1.3f, 2.6f, true);
        }).head(m -> {
            hull(m, s).hexa(false, -3.6f, -4.2f, 0f, 3.6f, -4.2f, 0f, 3.0f, 2.6f, 0f, -3.0f, 2.6f, 0f,
                -3.0f, -3.6f, 2.8f, 3.0f, -3.6f, 2.8f, 2.4f, 2.0f, 2.4f, -2.4f, 2.0f, 2.4f);
            dark(m, s).axis(0, 2.0f, 1.5f, 0, 3.4f, 1.5f).cyl(10, 2.1f, 0f, 1.4f);
            for(int k = 0; k < 3; k++){
                float a = 90f + k * 120f;
                float x = Mathf.cosDeg(a) * 1.15f, z = 1.5f + Mathf.sinDeg(a) * 1.15f;
                steel(m, s).axis(x, 3.2f, z, x, 10.0f, z).cyl(6, 0.42f, 0f, 6.8f);
                glow(m, s).axis(x, 10.0f, z, x, 10.05f, z).cyl(6, 0.22f, 0f, 0.05f);
            }
            trim(m, s).axis(0, 7.0f, 1.5f, 0, 7.6f, 1.5f).cyl(10, 1.9f, 0f, 0.6f);
            m.at(0, 0, 0); //reset the frame left by the axis() calls above
            glow2(m, s).box(-2.0f, -4.22f, 1.0f, 2.0f, -4.2f, 1.6f);
            team(m).box(-3.1f, -2.8f, 1.6f, -3.0f, 1.6f, 2.2f);
            team(m).box(3.0f, -2.8f, 1.6f, 3.1f, 1.6f, 2.2f);
        }).muzzles(0f, 10.2f, 2.65f, -1.0f, 10.2f, 0.9f, 1.0f, 10.2f, 0.9f).glow(TurretModels::heatGlow);
    }

    /** Well: gravity-tether dish with a floating orb emitter. */
    static SimpleTurret well(String name, Style s){
        return new SimpleTurret(name, 3, s).headZ(3.6f).base(m -> {
            mount(m, s, 3, 5.4f, 3.6f, 1);
            conduit(m, s, 9.2f, -9.2f, 1.6f, 5.2f, -4.8f, 3.2f, 0.35f);
        }).head(m -> {
            hull2(m, s).at(0, 0, 0).lathe(8, 22.5f, 3.6f, 0f, 3.4f, 1.0f, 2.2f, 1.8f, 0.001f, 1.9f);
            //parabolic dish facing +y (lathe along the aim axis)
            hull(m, s).axis(0, -0.6f, 2.8f, 0, 4.0f, 2.8f).lathe(14, 0, 0.8f, 0f, 2.6f, 1.2f, 4.0f, 2.8f, 4.2f, 3.1f, 3.6f, 3.0f, 2.2f, 1.6f, 0.001f, 1.0f);
            dark(m, s).axis(0, -0.6f, 2.8f, 0, 4.0f, 2.8f).cyl(10, 0.9f, -1.8f, 0f);
            for(int k = 0; k < 3; k++){
                float a = 90f + k * 120f;
                steel(m, s).pipe(6, 0.18f, Mathf.cosDeg(a) * 3.6f, 2.4f, 2.8f + Mathf.sinDeg(a) * 3.6f, 0, 5.4f, 2.8f);
            }
            glow2(m, s).at(0, 5.4f, 2.8f).lathe(10, 0, 0.001f, -0.9f, 0.8f, -0.5f, 0.9f, 0f, 0.8f, 0.5f, 0.001f, 0.9f);
        }).muzzles(0f, 5.4f, 2.8f).glow(b -> 0.25f + b.heat * 0.5f);
    }

}
