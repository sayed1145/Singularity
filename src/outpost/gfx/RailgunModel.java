package outpost.gfx;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.world.blocks.defense.turrets.Turret;
import outpost.world.RailgunTurret;

import static outpost.gfx.Mesh.*;

/**
 * v2 Bastion railgun: a connected armored bastion ring, not four floating posts.
 * Four corner bastions are joined by sloped curtain walls, the north rear holds a
 * twin capacitor bank + reactor core, and the turret itself is a chunky yawing hull
 * with a recoiling twin-rail barrel. Muzzle and bullet spawn share one math model.
 */
public class RailgunModel extends BlockModel{
    public static final Color cyan = Color.valueOf("8de9f5");
    public static final RailgunModel instance = new RailgunModel();

    // ---- muzzle / shooting alignment (single source of truth) ----
    public static final float BASE_Z = 2.0f;
    public static final float LIFT_FULL = 1.2f;
    /** Muzzle tip in turret-local units (barrel points along local +Y). */
    public static final float MUZZLE_LOCAL_Y = 11.0f;
    public static final float MUZZLE_LOCAL_Z = 5.5f;
    /** Bullet spawn distance: projected screen Y of the deployed muzzle tip. */
    public static final float SHOOT_Y = new Cam(16f).sy(MUZZLE_LOCAL_Y, BASE_Z + LIFT_FULL + MUZZLE_LOCAL_Z);

    private final Mesh turret = new Mesh(), barrel = new Mesh(), coils = new Mesh();
    private final Mesh wing = new Mesh(), cap = new Mesh(), marker = new Mesh();

    private RailgunModel(){
        super("bastion-railgun", 4);

        // ================= yawing turret hull (does NOT recoil; the barrel slides in it) =================
        turret.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cyl(28, 5.3f, 0f, 1.1f);
        turret.color(Pal.graphite).style(metal, 0).at(0, 0, 0).cyl(28, 4.4f, 1.1f, 1.5f);
        turret.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0)
            .bevel(-5.6f, -5.6f, 1.0f, 5.6f, 4.6f, 4.0f, 0.7f);
        turret.color(Pal.steel).style(metal, matPlate).at(0, 0, 0)
            .bevel(-4.6f, -4.6f, 4.0f, 4.6f, 3.6f, 4.7f, 0.25f);
        // front mantlet the barrel slides through
        turret.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
            .bevel(-3.0f, 2.4f, 2.0f, 3.0f, 5.2f, 6.4f, 0.4f);
        turret.color(Pal.graphite).style(metal, 0).at(0, 0, 0)
            .box(-1.9f, 4.6f, 3.4f, 1.9f, 5.25f, 6.0f);
        // barrel cradle rails
        for(int s = -1; s <= 1; s += 2){
            turret.color(Pal.steel).style(metal, 0).at(0, 0, 0)
                .box(s * 1.5f - 0.35f, -1.0f, 3.2f, s * 1.5f + 0.35f, 4.6f, 3.9f);
        }
        // rear counterweight + power cell + vents
        turret.color(Pal.white).style(metal, matPlate).at(0, 0, 0)
            .bevel(-3.6f, -7.0f, 1.2f, 3.6f, -4.2f, 4.6f, 0.4f);
        turret.color(cyan).style(emissive, 0).at(0, 0, 0)
            .box(-1.8f, -7.02f, 2.6f, 1.8f, -6.96f, 3.4f);
        turret.color(Pal.dark).style(0, matFins).at(0, 0, 0)
            .box(-2.8f, -6.4f, 4.6f, 2.8f, -4.6f, 4.78f);
        // side skirts with light slits
        for(int s = -1; s <= 1; s += 2){
            turret.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
                .cbevel(s * 5.3f, -0.5f, 1.0f, 1.7f, 7.2f, 2.3f, 0.2f);
            turret.color(Pal.white).style(metal, matPlate).at(0, 0, 0)
                .cbevel(s * 5.3f, -0.5f, 3.3f, 1.5f, 6.6f, 0.7f, 0.14f);
            turret.color(cyan).style(emissive, 0).at(0, 0, 0)
                .box(s * 5.3f - 0.9f * Math.signum(s) - 0.03f, -2.8f, 2.1f,
                    s * 5.3f - 0.9f * Math.signum(s) + 0.03f, 1.6f, 2.35f);
            // shield hinge posts
            turret.color(Pal.steel).style(metal, 0).at(s * 4.3f, 0.5f, 2.6f).cyl(10, 0.5f, 0f, 1.7f);
            turret.color(Pal.dark).style(metal, 0).at(s * 4.3f, 0.5f, 4.3f).cyl(10, 0.62f, 0f, 0.25f);
        }
        // top sensor mast (north-west of the roof, clear of the barrel sweep)
        turret.color(Pal.darkSteel).style(metal, 0).at(-3.4f, -3.2f, 4.7f).cyl(8, 0.24f, 0f, 2.1f);
        turret.color(Pal.white).style(metal, 0).at(0, 0, 0)
            .cbevel(-3.4f, -3.2f, 6.8f, 1.2f, 1.0f, 0.8f, 0.12f);
        turret.color(cyan).style(emissive, 0).at(0, 0, 0)
            .cbox(-3.4f, -2.68f, 7.0f, 0.7f, 0.08f, 0.4f);
        // roof bolts + hazard chevron on the mantlet
        for(int sx = -1; sx <= 1; sx += 2){
            for(int sy = -1; sy <= 1; sy += 2){
                turret.color(Pal.darkSteel).style(metal, 0).at(sx * 3.9f, sy * 2.9f - 0.5f, 4.7f).cyl(6, 0.22f, 0f, 0.12f);
            }
        }
        turret.color(Pal.yellow).style(0, matHazard).at(0, 0, 0)
            .box(-2.2f, 5.22f, 2.4f, 2.2f, 5.28f, 2.9f);

        // ================= recoiling barrel assembly =================
        barrel.color(Pal.graphite).style(metal, 0).at(0, 0, 0)
            .bevel(-1.7f, -4.2f, 3.6f, 1.7f, 2.2f, 6.0f, 0.3f);
        // twin rails, thick and long
        for(int s = -1; s <= 1; s += 2){
            float cx = s * 2.15f;
            barrel.color(Pal.steel).style(metal, matPlate).at(0, 0, 0)
                .bevel(cx - 0.9f, -2.0f, 4.1f, cx + 0.9f, 11.0f, 6.7f, 0.3f);
            barrel.color(Pal.white).style(metal, 0).at(0, 0, 0)
                .bevel(cx - 0.82f, 6.5f, 6.7f, cx + 0.82f, 11.0f, 7.25f, 0.2f);
            barrel.color(Pal.dark).style(metal, matFins).at(0, 0, 0)
                .box(cx - 0.92f, -1.2f, 4.6f, cx + 0.92f, 5.8f, 5.1f);
            // muzzle brake prongs
            barrel.color(Pal.white).style(metal, 0).at(0, 0, 0)
                .bevel(cx - 0.95f, 10.2f, 3.9f, cx + 0.95f, 11.6f, 7.1f, 0.22f);
            barrel.color(Pal.graphite).style(0, 0).at(0, 0, 0)
                .box(cx - 0.5f, 11.55f, 4.6f, cx + 0.5f, 11.62f, 6.4f);
            // side armor cheeks
            barrel.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0)
                .bevel(cx + s * 0.9f - 0.02f, 0.5f, 3.3f, cx + s * 1.9f, 8.0f, 5.9f, 0.22f);
        }
        // central spine between the rails
        barrel.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
            .box(-0.6f, -1.8f, 4.3f, 0.6f, 10.6f, 5.0f);
        // transverse breech drum
        barrel.color(Pal.darkSteel).style(metal, 0).at(0, -3.6f, 5.0f).rot(1, 90).cyl(14, 1.25f, -2.1f, 2.1f);
        barrel.color(Pal.steel).style(metal, 0).at(0, -3.6f, 5.0f).rot(1, 90).cyl(14, 0.5f, -2.3f, 2.3f);

        // ================= glowing coils (same pose as the barrel) =================
        float[] coilY = {0.4f, 2.9f, 5.4f, 7.9f, 9.9f};
        for(float y : coilY){
            coils.color(Pal.darkSteel).style(metal, 0).at(0, y, 5.55f).rot(0, -90f)
                .ring(18, 1.7f, 2.5f, -0.3f, 0.3f);
            coils.color(cyan).style(emissive, 0).at(0, y, 5.55f).rot(0, -90f)
                .ring(18, 2.48f, 2.68f, -0.13f, 0.13f);
        }
        coils.color(cyan).style(emissive, 0).at(0, 0, 0)
            .box(-0.3f, -1.6f, 5.0f, 0.3f, 10.4f, 5.25f);
        coils.color(cyan).style(emissive, 0).at(0, -2.6f, 5.0f).rot(0, -90f)
            .ring(14, 1.3f, 1.5f, -0.1f, 0.1f);

        // ================= folding side shields =================
        wing.color(Pal.white).style(metal, matPlate).at(0, 0, 0)
            .taper(0, 0, 0, 2.4f, 5.2f, 3.4f, 1.7f, 4.4f, 0, 0.4f, false);
        wing.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
            .cbevel(0, 0.2f, 0f, 1.1f, 3.8f, 0.6f, 0.1f);
        wing.color(cyan).style(emissive, 0).at(0, 0, 0)
            .cbox(0, 0.6f, 1.6f, 0.3f, 1.6f, 0.1f);
        wing.color(Pal.yellow).style(0, matHazard).at(0, 0, 0)
            .cbox(0, -2.2f, 0.4f, 1.8f, 0.5f, 0.12f);

        // ================= bastion cap (small live part on each corner tower) =================
        cap.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cyl(12, 1.5f, 0f, 0.55f);
        cap.color(Pal.white).style(metal, 0).at(0, 0, 0).cyl(12, 1.1f, 0.55f, 1.0f);
        cap.color(cyan).style(emissive, 0).at(0, 0, 1.0f).ring(12, 0.7f, 0.95f, 0f, 0.1f);

        // ================= muzzle marker (at the barrel tip) =================
        marker.color(cyan).style(emissive, 0).at(0, 0, 0).rot(0, -90f)
            .ring(16, 0.85f, 1.1f, -0.12f, 0.12f);
        marker.color(Color.white).style(emissive, 0).at(0, 0, 0).rot(0, -90f)
            .ring(12, 0.3f, 0.5f, -0.1f, 0.1f);
    }

    @Override
    public void buildStatic(Mesh m){
        // ---- ground ----
        m.color(Pal.concrete).style(0, matConcrete).at(0, 0, 0)
            .bevel(-16, -16, 0, 16, 16, 0.60f, 0.4f);
        m.color(Pal.white).style(0, matPlate).at(0, 0, 0)
            .bevel(-15.15f, -15.15f, 0.61f, 15.15f, 15.15f, 1.7f, 0.45f);
        m.color(Pal.dark).style(metal, matGrate).at(0, 0, 0)
            .cbevel(0, 0, 1.7f, 24.6f, 24.6f, 0.35f, 0.16f);
        // central traverse race
        m.color(Pal.graphite).style(metal, 0).at(0, 0, 0).cyl(36, 7.4f, 2.04f, 2.6f);
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cyl(36, 6.0f, 2.63f, 3.05f);
        m.color(cyan).style(emissive, 0).at(0, 0, 0).ring(36, 6.2f, 6.55f, 2.69f, 2.73f);
        for(int i = 0; i < 12; i++){
            float a = i * 30f;
            m.color(Pal.steel).style(metal, 0).at(6.7f * Mathf.cosDeg(a), 6.7f * Mathf.sinDeg(a), 2.6f).cyl(6, 0.2f, 0f, 0.12f);
        }

        // ---- four corner bastions, CONNECTED by curtain walls ----
        for(int sx = -1; sx <= 1; sx += 2){
            for(int sy = -1; sy <= 1; sy += 2){
                float x = sx * 10.4f, y = sy * 9.9f;
                m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
                    .cbevel(x, y, 1.9f, 5.6f, 5.2f, 1.1f, 0.25f);
                m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0)
                    .cbevel(x, y, 3.0f, 4.1f, 3.9f, 1.9f, 0.2f);
                m.color(Pal.white).style(metal, matPlate).at(0, 0, 0)
                    .cbevel(x, y, 4.9f, 3.3f, 3.1f, 0.7f, 0.15f);
                m.color(Pal.graphite).style(0, 0).at(x, y, 5.6f).cyl(12, 1.0f, 0, 0.08f);
                m.color(cyan).style(emissive, 0).at(0, 0, 0)
                    .cbox(x, y - sy * 1.98f, 3.8f, 1.6f, 0.08f, 0.24f);
                // armored power conduit from each bastion to the central race
                m.color(Pal.dark).style(metal, 0).at(0, 0, 0)
                    .pipe(8, 0.3f, sx * 8.2f, sy * 7.8f, 2.4f, sx * 5.8f, sy * 5.2f, 2.7f);
                m.color(Pal.steel).style(metal, 0).at(0, 0, 0)
                    .box(Math.min(sx * 8.2f, sx * 5.8f) - 0.4f, Math.min(sy * 7.8f, sy * 5.2f) - 0.4f, 2.15f,
                        Math.max(sx * 8.2f, sx * 5.8f) + 0.4f, Math.max(sy * 7.8f, sy * 5.2f) + 0.4f, 2.45f);
            }
        }
        // curtain walls: north / south / east / west (sloped armor, walkable tops)
        // north wall (behind the turret, tall)
        m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0)
            .taper(0, 13.2f, 1.7f, 16.4f, 2.6f, 3.6f, 15.2f, 1.9f, 0, 0, false);
        m.color(Pal.dark).style(metal, matGrate).at(0, 0, 0)
            .box(-7.6f, 12.4f, 3.6f, 7.6f, 14.0f, 3.75f);
        // south wall (lower, with a central ammo gate)
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0)
                .taper(s * 8.2f, -13.4f, 1.7f, 8.0f, 2.4f, 3.1f, 7.4f, 1.8f, 0, 0, false);
            m.color(Pal.dark).style(metal, matGrate).at(0, 0, 0)
                .box(s * 8.2f - 3.7f, -14.2f, 3.1f, s * 8.2f + 3.7f, -12.6f, 3.22f);
        }
        // east / west walls
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0)
                .taper(s * 13.4f, 0, 1.7f, 2.6f, 15.0f, 3.4f, 1.9f, 14.0f, 0, 0, false);
            m.color(Pal.dark).style(metal, matGrate).at(0, 0, 0)
                .box(s * 13.4f - 0.8f, -7.0f, 3.4f, s * 13.4f + 0.8f, 7.0f, 3.52f);
            m.color(cyan).style(emissive, 0).at(0, 0, 0)
                .box(s * 13.4f - 0.9f, -1.0f, 2.5f, s * 13.4f + 0.9f, 1.0f, 2.7f);
        }

        // ---- north rear power complex (fills the old empty back) ----
        for(int s = -1; s <= 1; s += 2){
            float x = s * 6.6f;
            // capacitor cabinet
            m.color(Pal.white).style(metal, matPlate).at(0, 0, 0)
                .cbevel(x, 11.0f, 1.7f, 3.6f, 2.8f, 3.6f, 0.25f);
            m.color(Pal.dark).style(0, matFins).at(0, 0, 0)
                .cbox(x, 11.0f, 5.3f, 2.6f, 2.0f, 0.1f);
            m.color(cyan).style(emissive, 0).at(0, 0, 0)
                .box(x - 1.1f, 9.58f, 3.6f, x + 1.1f, 9.64f, 3.9f);
            m.color(Pal.dark).style(0, matFins).at(0, 0, 0)
                .box(x - 1.5f, 9.6f, 2.2f, x + 1.5f, 9.66f, 3.2f);
            // armored cable tray from cabinet to the race
            m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
                .box(x - 0.7f, 6.2f, 2.1f, x + 0.7f, 9.7f, 2.7f);
            m.color(Pal.steel).style(metal, 0).at(0, 0, 0)
                .pipe(8, 0.24f, x, 6.4f, 2.8f, x * 0.55f, 4.6f, 2.9f);
        }
        // central reactor core
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cyl(20, 2.5f, 1.7f, 2.3f);
        m.color(Pal.steel).style(metal, 0).at(0, 12.6f, 2.3f).cyl(20, 2.0f, 0f, 2.6f);
        m.color(Pal.white).style(metal, 0).at(0, 12.6f, 4.9f).cyl(20, 1.5f, 0f, 0.7f);
        m.color(cyan).style(emissive, 0).at(0, 12.6f, 3.6f).ring(20, 2.02f, 2.2f, 0f, 0.22f);
        m.color(Pal.dark).style(metal, 0).at(0, 0, 0)
            .pipe(8, 0.3f, -1.6f, 11.2f, 2.6f, -3.2f, 7.5f, 2.8f);
        m.color(Pal.dark).style(metal, 0).at(0, 0, 0)
            .pipe(8, 0.3f, 1.6f, 11.2f, 2.6f, 3.2f, 7.5f, 2.8f);
        // exhaust stacks
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.offWhite).style(metal, 0).at(s * 2.9f, 14.0f, 3.75f).cyl(10, 0.55f, 0f, 1.4f);
            m.color(Pal.graphite).style(0, 0).at(s * 2.9f, 14.0f, 5.15f).cyl(10, 0.62f, 0f, 0.18f);
        }

        // ---- side energy stores, tied into the walls ----
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0)
                .cbevel(s * 11.2f, -2.4f, 1.75f, 3.0f, 5.4f, 2.6f, 0.23f);
            m.color(Pal.dark).style(0, matFins).at(0, 0, 0)
                .box(s * 11.2f - 1.15f, -5.15f, 2.2f, s * 11.2f + 1.15f, -5.12f, 3.5f);
            m.color(cyan).style(emissive, 0).at(0, 0, 0)
                .box(s * 11.2f - 0.6f, -5.2f, 3.7f, s * 11.2f + 0.6f, -5.14f, 3.9f);
            m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
                .box(s * 9.4f - 0.5f, -2.4f, 2.1f, s * 11.2f + 0.2f, -1.4f, 2.6f);
        }

        // ---- south bunker + ammo drums + feed chute ----
        m.color(Pal.white).style(metal, matPlate).at(0, 0, 0)
            .cbevel(0, -13.6f, 1.7f, 7.6f, 1.8f, 1.7f, 0.16f);
        m.color(Pal.dark).style(0, 0).at(0, 0, 0)
            .box(-2.5f, -14.53f, 2.25f, 2.5f, -14.51f, 2.95f);
        m.color(cyan).style(emissive, 0).at(0, 0, 0)
            .box(-2.15f, -14.57f, 2.48f, 1.7f, -14.53f, 2.70f);
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.steel).style(metal, 0).at(s * 5.6f, -13.2f, 1.7f).cyl(14, 1.3f, 0f, 1.9f);
            m.color(Pal.darkSteel).style(metal, 0).at(s * 5.6f, -13.2f, 2.5f).ring(14, 1.3f, 1.42f, 0f, 0.16f);
            m.color(Pal.darkSteel).style(metal, 0).at(s * 5.6f, -13.2f, 3.3f).ring(14, 1.3f, 1.42f, 0f, 0.16f);
        }
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
            .box(-1.0f, -12.4f, 2.0f, 1.0f, -6.0f, 2.7f);
        m.color(Pal.mid).style(metal, matGrate).at(0, 0, 0)
            .box(-0.7f, -12.2f, 2.7f, 0.7f, -6.2f, 2.82f);

        // hazard edging, restrained
        m.color(Pal.yellow).style(0, matHazard).at(0, 0, 0)
            .box(-5.5f, 14.65f, 1.74f, 5.5f, 15.0f, 1.80f);
        m.color(Pal.yellow).style(0, matHazard).at(0, 0, 0)
            .box(-2.6f, -15.0f, 1.74f, 2.6f, -14.65f, 1.80f);
    }

    @Override
    public void buildEnvelope(Mesh m){
        // yaw sweep of the whole upper turret + barrel + shields
        m.at(0, 0, 0).cyl(28, 13.6f, 2.0f, 12.5f);
        for(int sx = -1; sx <= 1; sx += 2){
            for(int sy = -1; sy <= 1; sy += 2){
                m.at(0, 0, 0).box(sx * 10.4f - 1.9f, sy * 9.9f - 1.9f, 5.4f,
                    sx * 10.4f + 1.9f, sy * 9.9f + 1.9f, 7.4f);
            }
        }
    }

    @Override
    public void buildRest(Mesh m){
        float yaw = -24f, open = 0.85f, lift = open * LIFT_FULL;
        m.add(turret, 0, 0, BASE_Z + lift, 2, yaw);
        m.add(barrel, 0, 0, BASE_Z + lift, 2, yaw);
        m.add(coils, 0, 0, BASE_Z + lift, 2, yaw);
        for(int s = -1; s <= 1; s += 2){
            float lx = s * 4.3f, ly = 0.5f;
            float hx = lx * Mathf.cosDeg(yaw) - ly * Mathf.sinDeg(yaw);
            float hy = lx * Mathf.sinDeg(yaw) + ly * Mathf.cosDeg(yaw);
            m.add(wing, hx, hy, BASE_Z + lift + 2.6f, 2, yaw + s * (75f - 50f * open));
        }
        for(int sx = -1; sx <= 1; sx += 2){
            for(int sy = -1; sy <= 1; sy += 2){
                m.add(cap, sx * 10.4f, sy * 9.9f, 5.65f + open * 0.45f, 2, 0);
            }
        }
    }

    @Override
    public void drawLive(Building b){
        if(b instanceof RailgunTurret.RailgunBuild tb && b.block instanceof Turret turret){
            float charge = Mathf.clamp(tb.reloadCounter / Math.max(turret.reload, 1f));
            if(tb.charging()) charge = Math.max(charge, tb.charge);
            drawPose(b.x, b.y, tb.rotation - 90f, tb.shootWarmup, charge,
                tb.curRecoil, turret.recoilPow, turret.recoil, Time.time + b.id * 4f);
        }else{
            drawPose(b.x, b.y, 0f, 0.35f, 0.2f, 0f, 1.8f, 2.2f, Time.time);
        }
    }

    /** Shared gameplay/promotion pose. Recoil uses the turret's own pow/dist so art and bullets agree. */
    public void drawPose(float wx, float wy, float yaw, float deploy, float charge,
                         float recoil, float recoilPow, float recoilDist, float time){
        float open = Mathf.clamp(deploy), lift = open * LIFT_FULL;
        float back = Mathf.pow(Mathf.clamp(recoil), recoilPow) * recoilDist;
        float backX = back * Mathf.sinDeg(yaw), backY = -back * Mathf.cosDeg(yaw);

        // corner caps first (north pair, then south pair)
        for(int sy = 1; sy >= -1; sy -= 2){
            for(int sx = -1; sx <= 1; sx += 2){
                Live.glowR = 0.35f + open * 0.75f;
                Live.glowG = Live.glowB = Math.max(0.4f, Live.glowR);
                Live.pose(sx * 10.4f, sy * 9.9f, 5.65f + open * 0.45f, 2, time * 0.4f * sx * sy);
                Live.draw(cap, cam, wx, wy);
            }
        }
        Live.resetTint();

        Live.pose(0, 0, BASE_Z + lift, 2, yaw);
        Live.draw(turret, cam, wx, wy);

        for(int s = -1; s <= 1; s += 2){
            float lx = s * 4.3f, ly = 0.5f;
            float hx = lx * Mathf.cosDeg(yaw) - ly * Mathf.sinDeg(yaw);
            float hy = lx * Mathf.sinDeg(yaw) + ly * Mathf.cosDeg(yaw);
            Live.pose(hx, hy, BASE_Z + lift + 2.6f, 2, yaw + s * (75f - 50f * open));
            Live.draw(wing, cam, wx, wy);
        }

        Live.pose(backX, backY, BASE_Z + lift, 2, yaw);
        Live.draw(barrel, cam, wx, wy);

        Live.glowR = 0.32f + 0.75f * Mathf.clamp(charge) + 0.22f * Mathf.sinDeg(time * 2.1f);
        Live.glowG = Live.glowB = Math.max(0.35f, Live.glowR);
        Live.pose(backX, backY, BASE_Z + lift, 2, yaw);
        Live.draw(coils, cam, wx, wy);
        Live.resetTint();

        // muzzle marker rides the recoiling barrel tip
        float mx = backX - MUZZLE_LOCAL_Y * Mathf.sinDeg(yaw);
        float my = backY + MUZZLE_LOCAL_Y * Mathf.cosDeg(yaw);
        Live.glowR = 0.35f + charge * 0.85f;
        Live.glowG = Live.glowB = Math.max(0.4f, Live.glowR);
        Live.pose(mx, my, BASE_Z + lift + MUZZLE_LOCAL_Z, 2, yaw);
        Live.draw(marker, cam, wx, wy);
        Live.resetTint();
    }

    /** World-space muzzle tip (before camera projection), shared by art and previews. */
    public float[] muzzleWorld(float yaw, float lift, float back){
        return new float[]{
            back * Mathf.sinDeg(yaw) - MUZZLE_LOCAL_Y * Mathf.sinDeg(yaw),
            -back * Mathf.cosDeg(yaw) + MUZZLE_LOCAL_Y * Mathf.cosDeg(yaw),
            BASE_Z + lift + MUZZLE_LOCAL_Z
        };
    }

    @Override
    public void drawOver(Building b){
        if(!(b instanceof RailgunTurret.RailgunBuild t)) return;
        if(t.heat < 0.04f && t.charge < 0.04f) return;
        float open = Mathf.clamp(t.shootWarmup), lift = open * LIFT_FULL;
        float recoilPow = 1.8f, recoilDist = 2.2f;
        if(b.block instanceof Turret turret){ recoilPow = turret.recoilPow; recoilDist = turret.recoil; }
        float back = Mathf.pow(Mathf.clamp(t.curRecoil), recoilPow) * recoilDist;
        float yaw = t.rotation - 90f;
        float[] mw = muzzleWorld(yaw, lift, back);
        float mx = b.x + cam.sx(mw[0], mw[2]);
        float my = b.y + cam.sy(mw[1], mw[2]);
        Draw.blend(Blending.additive);
        if(t.heat >= 0.04f){
            Draw.color(cyan, t.heat * 0.6f);
            Lines.stroke(1.1f * t.heat + 0.3f);
            Lines.circle(mx, my, 1.5f + (1f - t.heat) * 8f);
            Fill.circle(mx, my, 2.2f * t.heat);
        }
        if(t.charge > 0.04f){
            Draw.color(Color.white, t.charge * 0.5f);
            Lines.stroke(0.8f);
            Lines.circle(mx, my, 5f * (1f - t.charge) + 2f);
        }
        Draw.blend();
        Draw.reset();
    }
}
