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
import outpost.world.LancerTurret;

import static outpost.gfx.Mesh.*;

/** v2 Aegis lancer: compact 3x3 rapid coilgun in a walled post. */
public class LancerModel extends BlockModel{
    public static final Color amber = Color.valueOf("ffc35c");
    public static final LancerModel instance = new LancerModel();

    public static final float BASE_Z = 1.8f;
    public static final float LIFT_FULL = 0.8f;
    public static final float MUZZLE_LOCAL_Y = 7.5f;
    public static final float MUZZLE_LOCAL_Z = 4.0f;
    public static final float SHOOT_Y = new Cam(12f).sy(MUZZLE_LOCAL_Y, BASE_Z + LIFT_FULL + MUZZLE_LOCAL_Z);

    private final Mesh turret = new Mesh(), barrel = new Mesh(), coils = new Mesh(), marker = new Mesh();

    private LancerModel(){
        super("aegis-lancer", 3);
        // yawing hull
        turret.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cyl(24, 3.9f, 0f, 0.9f);
        turret.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0)
            .bevel(-3.9f, -3.9f, 0.9f, 3.9f, 3.2f, 3.1f, 0.55f);
        turret.color(Pal.steel).style(metal, matPlate).at(0, 0, 0)
            .bevel(-3.1f, -3.1f, 3.1f, 3.1f, 2.5f, 3.7f, 0.2f);
        turret.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
            .bevel(-2.0f, 1.6f, 1.6f, 2.0f, 3.6f, 4.6f, 0.3f);
        turret.color(Pal.graphite).style(metal, 0).at(0, 0, 0)
            .box(-1.2f, 3.0f, 2.6f, 1.2f, 3.62f, 4.2f);
        // rear cell + vents
        turret.color(Pal.white).style(metal, matPlate).at(0, 0, 0)
            .bevel(-2.4f, -5.0f, 1.0f, 2.4f, -3.2f, 3.4f, 0.3f);
        turret.color(amber).style(emissive, 0).at(0, 0, 0)
            .box(-1.2f, -5.02f, 1.9f, 1.2f, -4.96f, 2.5f);
        for(int s = -1; s <= 1; s += 2){
            turret.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
                .cbevel(s * 3.6f, -0.4f, 0.9f, 1.2f, 4.6f, 1.7f, 0.16f);
            turret.color(amber).style(emissive, 0).at(0, 0, 0)
                .box(s * 3.6f - 0.62f * Math.signum(s) - 0.03f, -1.8f, 1.6f,
                    s * 3.6f - 0.62f * Math.signum(s) + 0.03f, 1.0f, 1.85f);
            // side capacitor drums ride the turret
            turret.color(Pal.steel).style(metal, 0).at(s * 2.9f, -2.2f, 3.7f).cyl(10, 0.65f, 0f, 1.1f);
            turret.color(Pal.dark).style(metal, 0).at(s * 2.9f, -2.2f, 4.8f).cyl(10, 0.7f, 0f, 0.14f);
        }
        turret.color(Pal.darkSteel).style(metal, 0).at(2.4f, 1.8f, 3.7f).cyl(8, 0.18f, 0f, 1.4f);
        turret.color(amber).style(emissive, 0).at(0, 0, 0).cbox(2.4f, 1.8f, 5.1f, 0.4f, 0.4f, 0.18f);

        // recoiling single barrel
        barrel.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
            .bevel(-1.1f, -2.6f, 2.8f, 1.1f, 1.8f, 4.6f, 0.22f);
        barrel.color(Pal.steel).style(metal, matPlate).at(0, 0, 0)
            .bevel(-1.2f, -2.8f, 4.6f, 1.2f, 1.4f, 5.1f, 0.18f);
        barrel.color(Pal.steel).style(metal, matPlate).at(0, 0, 0)
            .bevel(-1.25f, 1.2f, 3.1f, 1.25f, 7.5f, 4.9f, 0.24f);
        barrel.color(Pal.white).style(metal, 0).at(0, 0, 0)
            .bevel(-1.1f, 5.6f, 4.9f, 1.1f, 7.5f, 5.35f, 0.16f);
        barrel.color(Pal.dark).style(metal, matFins).at(0, 0, 0)
            .box(-1.27f, 1.8f, 3.5f, 1.27f, 5.2f, 3.85f);
        barrel.color(Pal.white).style(metal, 0).at(0, 0, 0)
            .bevel(-1.35f, 6.9f, 2.9f, 1.35f, 8.1f, 5.1f, 0.18f);
        barrel.color(Pal.graphite).style(0, 0).at(0, 0, 0)
            .box(-0.4f, 8.05f, 3.5f, 0.4f, 8.12f, 4.6f);

        float[] coilY = {2.2f, 4.2f, 6.0f};
        for(float y : coilY){
            coils.color(Pal.darkSteel).style(metal, 0).at(0, y, 4.0f).rot(0, -90f)
                .ring(16, 1.25f, 1.75f, -0.24f, 0.24f);
            coils.color(amber).style(emissive, 0).at(0, y, 4.0f).rot(0, -90f)
                .ring(16, 1.73f, 1.9f, -0.1f, 0.1f);
        }
        coils.color(amber).style(emissive, 0).at(0, 0, 0)
            .box(-0.22f, 1.4f, 4.9f, 0.22f, 7.3f, 5.08f);

        marker.color(amber).style(emissive, 0).at(0, 0, 0).rot(0, -90f)
            .ring(14, 0.6f, 0.82f, -0.1f, 0.1f);
        marker.color(Color.white).style(emissive, 0).at(0, 0, 0).rot(0, -90f)
            .ring(10, 0.2f, 0.36f, -0.08f, 0.08f);
    }

    @Override
    public void buildStatic(Mesh m){
        m.color(Pal.concrete).style(0, matConcrete).at(0, 0, 0)
            .bevel(-12, -12, 0, 12, 12, 0.7f, 0.3f);
        m.color(Pal.white).style(0, matPlate).at(0, 0, 0)
            .bevel(-11.3f, -11.3f, 0.7f, 11.3f, 11.3f, 1.4f, 0.3f);
        m.color(Pal.dark).style(metal, matGrate).at(0, 0, 0)
            .cbevel(0, 0, 1.4f, 18.6f, 18.6f, 0.3f, 0.14f);
        m.color(Pal.graphite).style(metal, 0).at(0, 0, 0).cyl(28, 5.1f, 1.68f, 2.15f);
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cyl(28, 4.1f, 2.18f, 2.5f);
        m.color(amber).style(emissive, 0).at(0, 0, 0).ring(28, 4.25f, 4.5f, 2.24f, 2.28f);

        // corner posts + connecting low walls
        for(int sx = -1; sx <= 1; sx += 2){
            for(int sy = -1; sy <= 1; sy += 2){
                float x = sx * 8.4f, y = sy * 8.0f;
                m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
                    .cbevel(x, y, 1.6f, 3.4f, 3.2f, 0.9f, 0.18f);
                m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0)
                    .cbevel(x, y, 2.5f, 2.5f, 2.3f, 1.5f, 0.16f);
                m.color(amber).style(emissive, 0).at(0, 0, 0)
                    .cbox(x, y - sy * 1.18f, 3.0f, 1.0f, 0.07f, 0.18f);
                m.color(Pal.dark).style(metal, 0).at(0, 0, 0)
                    .pipe(8, 0.22f, sx * 6.6f, sy * 6.2f, 2.0f, sx * 4.6f, sy * 4.2f, 2.3f);
            }
        }
        // low curtain walls on all four sides
        m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0)
            .taper(0, 10.0f, 1.4f, 13.6f, 1.9f, 2.8f, 12.8f, 1.4f, 0, 0, false);
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0)
                .taper(s * 5.9f, -10.1f, 1.4f, 5.4f, 1.8f, 2.6f, 5.0f, 1.3f, 0, 0, false);
            m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0)
                .taper(s * 10.1f, 0, 1.4f, 1.9f, 12.6f, 2.7f, 1.4f, 11.8f, 0, 0, false);
        }
        m.color(Pal.dark).style(metal, matGrate).at(0, 0, 0)
            .box(-6.4f, 9.5f, 2.8f, 6.4f, 10.5f, 2.92f);

        // rear capacitor + tray
        m.color(Pal.white).style(metal, matPlate).at(0, 0, 0)
            .cbevel(0, 8.2f, 1.4f, 3.2f, 2.2f, 2.9f, 0.2f);
        m.color(amber).style(emissive, 0).at(0, 0, 0)
            .box(-0.9f, 7.08f, 2.8f, 0.9f, 7.14f, 3.1f);
        m.color(Pal.dark).style(0, matFins).at(0, 0, 0)
            .box(-1.2f, 7.1f, 1.8f, 1.2f, 7.16f, 2.6f);
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
            .box(-0.6f, 4.4f, 1.8f, 0.6f, 7.1f, 2.3f);

        // south gate + drums
        m.color(Pal.white).style(metal, matPlate).at(0, 0, 0)
            .cbevel(0, -10.3f, 1.4f, 5.2f, 1.4f, 1.4f, 0.14f);
        m.color(Pal.dark).style(0, 0).at(0, 0, 0)
            .box(-1.6f, -11.02f, 1.9f, 1.6f, -11.0f, 2.4f);
        m.color(amber).style(emissive, 0).at(0, 0, 0)
            .box(-1.3f, -11.06f, 2.05f, 1.0f, -11.02f, 2.25f);
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.steel).style(metal, 0).at(s * 4.2f, -10.0f, 1.4f).cyl(12, 0.95f, 0f, 1.4f);
            m.color(Pal.darkSteel).style(metal, 0).at(s * 4.2f, -10.0f, 2.0f).ring(12, 0.95f, 1.05f, 0f, 0.12f);
        }
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
            .box(-0.7f, -9.4f, 1.7f, 0.7f, -4.4f, 2.2f);
    }

    @Override
    public void buildEnvelope(Mesh m){
        m.at(0, 0, 0).cyl(24, 9.6f, 1.6f, 8.6f);
    }

    @Override
    public void buildRest(Mesh m){
        float yaw = -20f, open = 0.9f, lift = open * LIFT_FULL;
        m.add(turret, 0, 0, BASE_Z + lift, 2, yaw);
        m.add(barrel, 0, 0, BASE_Z + lift, 2, yaw);
        m.add(coils, 0, 0, BASE_Z + lift, 2, yaw);
    }

    @Override
    public void drawLive(Building b){
        if(b instanceof LancerTurret.LancerBuild tb && b.block instanceof Turret turret){
            float charge = Mathf.clamp(tb.reloadCounter / Math.max(turret.reload, 1f));
            if(tb.charging()) charge = Math.max(charge, tb.charge);
            drawPose(b.x, b.y, tb.rotation - 90f, tb.shootWarmup, charge,
                tb.curRecoil, turret.recoilPow, turret.recoil, Time.time + b.id * 7f);
        }else{
            drawPose(b.x, b.y, 0f, 0.4f, 0.25f, 0f, 1.8f, 1.4f, Time.time);
        }
    }

    public void drawPose(float wx, float wy, float yaw, float deploy, float charge,
                         float recoil, float recoilPow, float recoilDist, float time){
        float open = Mathf.clamp(deploy), lift = open * LIFT_FULL;
        float back = Mathf.pow(Mathf.clamp(recoil), recoilPow) * recoilDist;
        float backX = back * Mathf.sinDeg(yaw), backY = -back * Mathf.cosDeg(yaw);

        Live.pose(0, 0, BASE_Z + lift, 2, yaw);
        Live.draw(turret, cam, wx, wy);
        Live.pose(backX, backY, BASE_Z + lift, 2, yaw);
        Live.draw(barrel, cam, wx, wy);
        Live.glowR = 0.4f + 0.7f * Mathf.clamp(charge) + 0.2f * Mathf.sinDeg(time * 3.2f);
        Live.glowG = 0.35f + 0.55f * Mathf.clamp(charge);
        Live.glowB = 0.25f + 0.3f * Mathf.clamp(charge);
        Live.pose(backX, backY, BASE_Z + lift, 2, yaw);
        Live.draw(coils, cam, wx, wy);
        Live.resetTint();

        float mx = backX - MUZZLE_LOCAL_Y * Mathf.sinDeg(yaw);
        float my = backY + MUZZLE_LOCAL_Y * Mathf.cosDeg(yaw);
        Live.glowR = 0.45f + charge * 0.8f;
        Live.glowG = 0.4f + charge * 0.6f;
        Live.glowB = 0.3f + charge * 0.35f;
        Live.pose(mx, my, BASE_Z + lift + MUZZLE_LOCAL_Z, 2, yaw);
        Live.draw(marker, cam, wx, wy);
        Live.resetTint();
    }

    public float[] muzzleWorld(float yaw, float lift, float back){
        return new float[]{
            back * Mathf.sinDeg(yaw) - MUZZLE_LOCAL_Y * Mathf.sinDeg(yaw),
            -back * Mathf.cosDeg(yaw) + MUZZLE_LOCAL_Y * Mathf.cosDeg(yaw),
            BASE_Z + lift + MUZZLE_LOCAL_Z
        };
    }

    @Override
    public void drawOver(Building b){
        if(!(b instanceof LancerTurret.LancerBuild t)) return;
        if(t.heat < 0.05f) return;
        float open = Mathf.clamp(t.shootWarmup), lift = open * LIFT_FULL;
        float recoilPow = 1.8f, recoilDist = 1.4f;
        if(b.block instanceof Turret turret){ recoilPow = turret.recoilPow; recoilDist = turret.recoil; }
        float back = Mathf.pow(Mathf.clamp(t.curRecoil), recoilPow) * recoilDist;
        float yaw = t.rotation - 90f;
        float[] mw = muzzleWorld(yaw, lift, back);
        float mx = b.x + cam.sx(mw[0], mw[2]);
        float my = b.y + cam.sy(mw[1], mw[2]);
        Draw.blend(Blending.additive);
        Draw.color(amber, t.heat * 0.6f);
        Lines.stroke(0.9f * t.heat + 0.3f);
        Lines.circle(mx, my, 1.2f + (1f - t.heat) * 6f);
        Fill.circle(mx, my, 1.6f * t.heat);
        Draw.blend();
        Draw.reset();
    }
}
