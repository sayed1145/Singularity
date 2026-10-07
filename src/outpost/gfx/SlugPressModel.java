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

/** v2 slug press: enclosed feed chutes, gantry hoist, hydraulic rods, guide-railed output. */
public class SlugPressModel extends FactoryModel{
    public static final Color cyan = Color.valueOf("8ee7ec");
    public static final SlugPressModel instance = new SlugPressModel();

    private final Mesh ram = new Mesh(), rotor = new Mesh(), pellet = new Mesh();
    private final Mesh hook = new Mesh(), unitRod = new Mesh();
    private static final float z = 2.1f;

    private SlugPressModel(){
        super("slug-press", cyan);
        ram.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).bevel(-1.95f, -2.25f, 0,
            1.95f, 2.25f, 1.25f, 0.2f);
        ram.color(Pal.white).style(metal, matPlate).at(0, 0, 0).bevel(-1.7f, -1.95f, 1.25f,
            1.7f, 1.95f, 2.35f, 0.32f);
        ram.color(Pal.dark).style(0, 0).at(0, 0, 0).box(1.65f, -1.55f, 1.5f, 1.71f, 1.55f, 1.95f);
        ram.color(cyan).style(emissive, 0).at(0, 0, 0).box(1.71f, -1.2f, 1.66f, 1.74f, 1.2f, 1.84f);
        for(int i = -1; i <= 1; i += 2){
            ram.color(Pal.yellow).style(0, matHazard).at(0, 0, 0)
                .cbox(-0.35f, i * 1.8f, 2.36f, 0.55f, 0.22f, 0.1f);
        }
        // piston back clevis the hydraulic rod plugs into
        ram.color(Pal.steel).style(metal, 0).at(0, 0, 0).cbevel(-2.2f, 0, 0.8f, 0.7f, 1.2f, 1.0f, 0.1f);

        rotor.color(Pal.steel).style(metal, 0).at(0, 0, 0).ring(24, 2.0f, 2.55f, 0.0f, 0.45f);
        rotor.color(cyan).style(emissive, 0).at(0, 0, 0).ring(24, 2.55f, 2.72f, 0.14f, 0.23f);
        for(int i = 0; i < 8; i++){
            float a = i * 45f;
            rotor.color(Pal.white).style(metal, 0).at(0, 0, 0).rot(2, a)
                .cbevel(2.7f, 0, 0.03f, 0.8f, 0.55f, 0.58f, 0.09f);
        }
        pellet.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
            .taper(0, 0, 0, 1.2f, 0.8f, 0.75f, 0.95f, 0.7f, 0, 0, false);
        pellet.color(cyan).style(emissive, 0).at(0, 0, 0).cbox(0, 0, 0.76f, 0.45f, 0.4f, 0.11f);

        // gantry hook: block + sheave + hook point
        hook.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbevel(0, 0, 0.6f, 1.0f, 0.7f, 0.9f, 0.1f);
        hook.color(Pal.yellow).style(0, matHazard).at(0, 0, 0).cbox(0, 0, 1.5f, 0.8f, 0.5f, 0.12f);
        hook.color(Pal.steel).style(metal, 0).at(0, 0, 0).cyl(10, 0.18f, -0.4f, 0.6f);
        hook.color(cyan).style(emissive, 0).at(0, 0, 0).cbox(0, -0.36f, 0.9f, 0.3f, 0.06f, 0.3f);

        unitRod.color(Pal.steel).style(metal, 0).at(0, 0, 0).box(-0.5f, -0.5f, 0f, 0.5f, 0.5f, 1f, true);
    }

    @Override
    public void buildStatic(Mesh m){
        deck(m);
        m.color(Pal.dark).style(metal, matGrate).at(0, 0, 0)
            .bevel(-6.9f, -5.1f, deckZ, 6.9f, 4.5f, deckZ + 0.35f, 0.24f);
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
            .cyl(24, 3.35f, deckZ + 0.1f, z + 0.3f);
        m.color(Pal.graphite).style(0, 0).at(0, 0, z + 0.32f).cyl(24, 2.8f, 0, 0.06f);
        m.color(cyan).style(emissive, 0).at(0, 0, z + 0.39f).ring(24, 2.88f, 3.1f, 0, 0.04f);
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
                .bevel(s * 6.4f - 1.45f, -4.1f, deckZ, s * 6.4f + 1.45f, 4.1f, deckZ + 1.5f, 0.18f);
            // hydraulic power unit, larger with top tank + gauge
            m.color(Pal.white).style(metal, matPlate).at(0, 0, 0)
                .cbevel(s * 8.35f, -0.1f, deckZ, 3.1f, 5.8f, 3.4f, 0.25f);
            m.color(Pal.dark).style(0, matFins).at(0, 0, 0)
                .cbox(s * 8.35f, -3.12f, deckZ + 1.6f, 2.0f, 0.14f, 1.5f);
            m.color(cyan).style(emissive, 0).at(0, 0, 0)
                .cbox(s * 8.35f, -3.19f, deckZ + 2.7f, 1.1f, 0.10f, 0.18f);
            m.color(Pal.steel).style(metal, 0).at(s * 8.35f, 1.6f, deckZ + 3.4f).cyl(10, 0.7f, 0f, 0.8f);
            m.color(Pal.darkSteel).style(metal, 0).at(s * 8.35f, -1.6f, deckZ + 3.4f).cyl(8, 0.4f, 0f, 0.5f);
            // enclosed guide housing the live rod slides through
            m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
                .cbevel(s * 6.9f, 0, z + 0.4f, 1.6f, 1.6f, 1.6f, 0.12f);
        }
        // feed hoppers with ENCLOSED chutes (no floating pipes)
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0)
                .taper(s * 3.4f, 8.0f, deckZ, 2.8f, 2.4f, 4.0f, 3.8f, 3.1f, 0, 0, false);
            m.color(Pal.dark).style(metal, 0).at(s * 3.4f, 8.0f, 4f).cyl(12, 1.35f, 0, 0.1f);
            m.color(Pal.steel).style(metal, matPlate).at(0, 0, 0)
                .box(s * 3.4f - 0.55f, 4.6f, deckZ + 0.4f, s * 3.4f + 0.55f, 7.2f, deckZ + 1.6f);
            m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
                .box(s * 2.2f - 0.45f, 2.8f, z - 0.4f, s * 3.1f + 0.45f, 5.0f, z + 0.6f);
        }
        // rear gantry: columns + beam + end stops
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0)
                .cbevel(s * 8.6f, 6.2f, deckZ, 1.4f, 1.4f, 4.6f, 0.12f);
            m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
                .cbevel(s * 8.6f, 6.2f, deckZ, 1.9f, 1.9f, 0.5f, 0.1f);
        }
        m.color(Pal.white).style(metal, matPlate).at(0, 0, 0)
            .box(-8.6f, 5.7f, deckZ + 4.6f, 8.6f, 6.7f, deckZ + 5.5f);
        m.color(Pal.dark).style(0, matFins).at(0, 0, 0)
            .box(-8.6f, 5.85f, deckZ + 5.5f, 8.6f, 6.55f, deckZ + 5.62f);
        m.color(Pal.yellow).style(0, matHazard).at(0, 0, 0)
            .box(-1.2f, 5.68f, deckZ + 4.7f, 1.2f, 5.74f, deckZ + 5.1f);
        // output tray with guide rails + end stop
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
            .bevel(-1.4f, -10.9f, deckZ, 1.4f, -4.1f, z, 0.2f);
        m.color(Pal.mid).style(metal, matGrate).at(0, 0, 0)
            .bevel(-0.9f, -10.5f, z, 0.9f, -4.5f, z + 0.14f, 0.06f);
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.steel).style(metal, 0).at(0, 0, 0)
                .box(s * 1.15f - 0.12f, -10.7f, z, s * 1.15f + 0.12f, -4.3f, z + 0.55f);
        }
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0)
            .cbevel(0, -10.85f, z, 2.6f, 0.4f, 0.8f, 0.08f);
        // status mast
        m.color(Pal.darkSteel).style(metal, 0).at(-9.8f, -9.6f, deckZ).cyl(8, 0.2f, 0f, 2.6f);
        m.color(Pal.white).style(metal, 0).at(0, 0, 0).cbevel(-9.8f, -9.6f, deckZ + 2.6f, 0.7f, 0.7f, 0.5f, 0.08f);
        console(m, 8.7f, -9f);
        m.color(Pal.yellow).style(0, matHazard).at(0, 0, 0)
            .box(-3.5f, -11.1f, deckZ + 0.02f, 3.5f, -10.5f, deckZ + 0.08f);
    }

    @Override
    public void buildEnvelope(Mesh m){
        m.at(0, 0, 0).box(-7.2f, -5.6f, z, 7.2f, 5.4f, z + 4.0f);
        m.at(0, 0, 0).box(-1.3f, -11f, z, 1.3f, -3.4f, z + 1.3f);
        m.at(0, 0, 0).box(-1.0f, 5.4f, z + 1f, 1.0f, 7.0f, deckZ + 5.6f);
    }

    @Override
    public void buildRest(Mesh m){
        m.add(rotor, 0, 0, z + 2.6f, 2, 26);
        m.add(ram, -4.3f, 0, z + 0.75f, 2, 0);
        m.add(ram, 4.3f, 0, z + 0.75f, 2, 180);
        m.add(pellet, 0, 0, z + 0.6f, 2, 0);
        m.add(pellet, 0, -7.3f, z + 0.2f, 2, 0);
        m.add(hook, 0, 6.2f, deckZ + 3.4f, 2, 0);
    }

    @Override
    public void drawLive(Building b){
        float warm = warmup(b), prog = progress(b), t = total(b) + b.id * 3f;
        drawPose(b.x, b.y, warm, prog, t);
    }

    public void drawPose(float wx, float wy, float warm, float progress, float t){
        float stroke = warm * (0.5f - 0.5f * Mathf.cosDeg(progress * 360f));
        float x = 4.75f - 2.25f * stroke;
        // hydraulic rods from the HPUs into the moving rams
        for(int s = -1; s <= 1; s += 2){
            float ramBackX = s * (x + 2.2f);
            Live.link(s * 8.0f, 0, z + 1.15f, ramBackX, 0, z + 1.15f, 0.55f, 0.55f);
            Live.draw(unitRod, cam, wx, wy);
        }
        Live.pose(0, 0, z + 2.6f, 2, t * 2.4f);
        Live.glowR = 0.45f + warm * 0.7f;
        Live.draw(rotor, cam, wx, wy);
        Live.resetTint();
        Live.pose(-x, 0, z + 0.75f, 2, 0);
        Live.draw(ram, cam, wx, wy);
        Live.pose(x, 0, z + 0.75f, 2, 180);
        Live.draw(ram, cam, wx, wy);
        // gantry hook dips once per cycle
        float dip = smooth(0.05f, 0.35f, progress) * (1f - smooth(0.55f, 0.9f, progress));
        float hookZ = deckZ + 4.4f - dip * 1.6f * warm;
        Live.pose(0, 6.2f, hookZ, 2, 0);
        Live.draw(hook, cam, wx, wy);
        Live.link(0, 6.2f, hookZ + 1.5f, 0, 6.2f, deckZ + 5.4f, 0.22f, 0.22f);
        Live.draw(unitRod, cam, wx, wy);
        for(int i = 0; i < 2; i++){
            float p = ((t * 0.008f + i * 0.5f) % 1f + 1f) % 1f;
            float y = -3.9f - p * 6.0f;
            Live.glowR = 0.40f + 0.60f * warm;
            Live.alpha = warm > 0.08f ? 1f : 0.12f;
            Live.pose(0, y, z + 0.29f, 2, 0);
            Live.draw(pellet, cam, wx, wy);
        }
        Live.resetTint();
    }

    @Override
    public void drawOver(Building b){
        float w = warmup(b);
        if(w < 0.1f) return;
        float cx = b.x + cam.sx(0, z + 3f), cy = b.y + cam.sy(0, z + 3f);
        Draw.blend(Blending.additive);
        Draw.color(cyan, w * 0.45f * (0.7f + 0.3f * Mathf.absin(Time.time, 10f, 1f)));
        Lines.stroke(0.6f);
        Lines.circle(cx, cy, 2.4f);
        Fill.circle(cx, cy, 0.9f);
        Draw.blend();
        Draw.reset();
    }
}
