package rbmk.gfx;

import arc.graphics.*;
import arc.math.*;
import mindustry.gen.*;

import static rbmk.gfx.Mesh.*;

/**
 * Cladding mill: cold pilger mill. A flywheel crank drives the roll stand back and forth through an exact
 * slider-crank linkage; the grooved rolls counter-rotate by rolling contact (angle = travel / radius); the tube
 * hollow is reduced from r 0.42 to r 0.30 at the stand and advances with feed marks moving along it.
 */
public class CladdingMillModel extends FactoryModel{
    public static final CladdingMillModel instance = new CladdingMillModel();
    static final float tubeZ = 3.6f, rollR = 0.75f, fx = 6.2f, fy = -6.0f, fz = 4.0f, crank = 1.3f, rodL = 6.0f;
    static final float tubeY0 = -11f, tubeY1 = 8.0f;

    final Mesh flywheel = new Mesh(), plates = new Mesh(), bridge = new Mesh(), roll = new Mesh(), unitTube = new Mesh(), unitBox = new Mesh(), mark = new Mesh();

    public CladdingMillModel(){
        super("cladding-mill", new Color(0.40f, 0.62f, 0.86f));
        //flywheel along x (built along z then turned), west face carries spokes and the crank pin
        flywheel.color(Pal.offWhite).style(metal, 0).at(0, 0, 0).rot(1, -90).lathe(28, 0, 0, 0, 2.1f, 0, 2.1f, 0.7f, 0, 0.7f);
        flywheel.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).rot(1, -90).lathe(28, 0, 1.75f, 0.7f, 2.12f, 0.7f, 2.12f, 0.72f, 1.75f, 0.72f, 1.75f, 0.7f);
        for(int i = 0; i < 6; i++){
            flywheel.color(i == 0 ? accent : Pal.darkSteel).style(metal, 0).at(0, 0, 0).rot(0, i * 60f).box(-0.76f, -0.16f, 0.35f, -0.7f, 0.16f, 1.72f, false);
        }
        flywheel.color(Pal.dark).style(metal, 0).at(0, 0, 0).rot(1, -90).cyl(12, 0.45f, 0.7f, 0.95f);
        flywheel.color(Pal.steel).style(metal, 0).at(0, crank, 0).rot(1, -90).cyl(10, 0.26f, 0.7f, 1.25f);

        for(int s = -1; s <= 1; s += 2){
            float x0 = s < 0 ? -2.0f : 1.25f, x1 = s < 0 ? -1.25f : 2.0f;
            plates.color(Pal.white).style(metal, matPlate).at(0, 0, 0).bevel(x0, -1.6f, 1.9f, x1, 1.6f, 5.6f, 0.15f);
            plates.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).bevel(x0 - 0.12f, -1.75f, 1.9f, x1 + 0.12f, 1.75f, 2.35f, 0.08f);
        }
        plates.color(Pal.dark).style(metal, 0).at(2.0f, 0, 3.6f).rot(1, 90).cyl(10, 0.34f, 0f, 0.3f);
        bridge.color(accent).style(metal, 0).at(0, 0, 0).bevel(-2.1f, -1.35f, 5.6f, 2.1f, 1.35f, 6.25f, 0.15f);
        bridge.color(Pal.white).style(metal, 0).at(0, 0, 6.25f).cyl(12, 0.7f, 0f, 0.35f);
        //roll along x: alternating facets + a groove band so rotation reads clearly
        for(int i = 0; i < 12; i++){
            float a0 = i * 30f, a1 = a0 + 30f;
            float y0 = Mathf.cosDeg(a0) * rollR, z0 = Mathf.sinDeg(a0) * rollR, y1 = Mathf.cosDeg(a1) * rollR, z1 = Mathf.sinDeg(a1) * rollR;
            roll.color(i % 3 == 0 ? Pal.darkSteel : Pal.steel).style(metal, 0).at(0, 0, 0)
                .quad(-1.25f, y0, z0, 1.25f, y0, z0, 1.25f, y1, z1, -1.25f, y1, z1, 0, Mathf.cosDeg(a0 + 15f), Mathf.sinDeg(a0 + 15f));
        }
        roll.color(Pal.dark).style(metal, 0).at(0, 0, 0).rot(1, 90).lathe(12, 15f, rollR + 0.02f, -0.35f, rollR + 0.06f, -0.35f, rollR + 0.06f, 0.35f, rollR + 0.02f, 0.35f, rollR + 0.02f, -0.35f);
        unitTube.color(Pal.zirc).style(metal, 0).at(0, 0, 0).lathe(14, 0, 0, 0f, 0.5f, 0f, 0.5f, 1f, 0, 1f);
        unitBox.color(Pal.steel).style(metal, 0).at(0, 0, 0).box(-0.5f, -0.5f, 0f, 0.5f, 0.5f, 1f, true);
        mark.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).lathe(14, 0, 0.5f, 0f, 0.5f, 1f);
    }

    @Override
    public void buildStatic(Mesh m){
        deck(m);
        //rails under the stand
        for(int s = -1; s <= 1; s += 2){
            m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).bevel(s * 1.625f - 0.35f, -5.4f, deckZ, s * 1.625f + 0.35f, 2.8f, 1.9f, 0.08f);
        }
        m.color(Pal.graphite).style(0, 0).box(-1.2f, -5.4f, deckZ, 1.2f, 2.8f, deckZ + 0.02f);
        //infeed and outfeed roller stands
        float[] stands = {-9.4f, -6.9f, 4.3f, 6.9f};
        for(int i = 0; i < stands.length; i++){
            float y = stands[i], rTube = y < 0 ? 0.42f : 0.30f, rz = tubeZ - rTube - 0.28f;
            for(int s = -1; s <= 1; s += 2) m.color(Pal.offWhite).style(metal, 0).at(0, 0, 0).cbevel(s * 0.95f, y, deckZ, 0.4f, 0.8f, rz - deckZ + 0.5f, 0.08f);
            m.color(Pal.mid).style(metal, 0).cbevel(0, y, deckZ, 2.4f, 1.0f, 0.3f, 0.08f);
            m.color(Pal.steel).style(metal, 0).at(-0.75f, y, rz).rot(1, 90).cyl(10, 0.28f, 0f, 1.5f);
        }
        //finished tube rack (north west, kept low)
        for(int s = 0; s < 2; s++) m.color(Pal.dark).style(metal, 0).at(0, 0, 0).cbox(-9.6f + s * 4.4f, 7.9f, deckZ, 0.5f, 4.4f, 0.3f);
        for(int i = 0; i < 5; i++){
            m.color(Pal.zirc).style(metal, 0).pipe(12, 0.28f, -10.6f, 6.2f + i * 0.78f, 1.98f, -4.2f, 6.2f + i * 0.78f, 1.98f);
        }
        //flywheel bearing, motor and drive cabinet
        m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0).cbevel(7.25f, fy, deckZ, 0.9f, 1.8f, fz - deckZ + 0.4f, 0.12f);
        m.color(Pal.dark).style(metal, 0).at(6.55f, fy, fz).rot(1, 90).cyl(12, 0.5f, 0f, 1.3f);
        m.color(Pal.white).style(metal, 0).at(0, 0, 0).cbevel(9.6f, fy, deckZ, 2.8f, 3.4f, 2.9f, 0.3f);
        m.color(Pal.light).style(0, matFins).cbox(9.6f, fy, deckZ + 2.9f, 2.0f, 2.8f, 0.08f);
        m.color(accent).style(0, 0).box(8.2f, fy - 1.72f, deckZ + 1.8f, 11.0f, fy - 1.70f, deckZ + 2.1f);
        m.color(Pal.offWhite).style(0, matPlate).cbevel(8.2f, 3.8f, deckZ, 5.0f, 4.6f, 2.4f, 0.3f);
        m.color(Pal.dark).style(0, matGrate).cbox(8.2f, 3.8f, deckZ + 2.4f, 3.6f, 3.2f, 0.05f);
        //flywheel pit guard rail
        m.color(Pal.yellow).style(0, matHazard).cbox(fx, fy - 2.9f, deckZ, 3.6f, 0.35f, 0.9f);
        console(m, -7.4f, -8.4f);
    }

    @Override
    public void buildEnvelope(Mesh m){
        m.at(0, 0, 0).box(-2.25f, -4.4f, 1.85f, 2.4f, 2.1f, 6.65f);
        m.box(-0.5f, tubeY0, tubeZ - 0.5f, 0.5f, tubeY1, tubeZ + 0.5f);
        m.at(5.2f, fy, fz).rot(1, 90).cyl(20, 2.25f, 0f, 1.45f);
        m.at(0, 0, 0).box(2.0f, -7.6f, 2.3f, 5.7f, 0.7f, 5.7f);
    }

    @Override
    public void buildRest(Mesh m){
        float ys = standY(0f);
        m.add(flywheel, fx + 0.35f, fy, fz, 0, 0f);
        m.add(plates, 0, ys, 0, 2, 0f);
        m.add(roll, 0, ys, tubeZ - 0.42f - rollR, 0, 0f);
        m.add(roll, 0, ys, tubeZ + 0.42f + rollR, 0, 0f);
        m.add(bridge, 0, ys, 0, 2, 0f);
        Mesh tube = new Mesh();
        tube.color(Pal.zirc).style(metal, 0).pipe(14, 0.42f, 0, tubeY0, tubeZ, 0, ys, tubeZ);
        tube.pipe(14, 0.30f, 0, ys, tubeZ, 0, tubeY1, tubeZ);
        m.add(tube, 0, 0, 0, 2, 0f);
    }

    static float pinY(float phi){ return fy + crank * Mathf.cosDeg(phi); }
    static float pinZ(float phi){ return fz + crank * Mathf.sinDeg(phi); }

    /** Exact slider-crank: stand pin at x=2.2,z=3.6 connected by a rod of length rodL to the crank pin. */
    static float standY(float phi){
        float dx = (fx + 0.35f - 0.97f) - 2.3f, dz = pinZ(phi) - 3.6f;
        return pinY(phi) + (float)Math.sqrt(rodL * rodL - dx * dx - dz * dz);
    }

    @Override
    public void drawLive(Building b){
        float wx = b.x, wy = b.y, tp = total(b);
        float phi = tp * 3.2f;
        float ys = standY(phi);
        float roll1 = (ys * Mathf.radiansToDegrees) / rollR;

        //flywheel and connecting rod
        Live.pose(fx + 0.35f, fy, fz, 0, phi);
        Live.draw(flywheel, cam, wx, wy);
        float px = fx + 0.35f - 0.97f, py = pinY(phi), pz = pinZ(phi);
        Live.link(px, py, pz, 2.3f, ys, 3.6f, 0.42f, 0.55f);
        Live.draw(unitBox, cam, wx, wy);

        //stand
        Live.pose(0, ys, 0, 2, 0f);
        Live.draw(plates, cam, wx, wy);
        Live.pose(0, ys, tubeZ - 0.42f - rollR, 0, roll1);
        Live.draw(roll, cam, wx, wy);

        //tube hollow (north reduced part first, then the incoming stock), with moving feed marks
        float feed = (tp * 0.012f) % 1.8f;
        Live.link(0, ys, tubeZ, 0, tubeY1, tubeZ, 0.6f, 0.6f);
        Live.draw(unitTube, cam, wx, wy);
        for(float y = ys + 0.6f + feed * 1.4f; y < tubeY1 - 0.2f; y += 2.5f){
            Live.link(0, y, tubeZ, 0, y + 0.12f, tubeZ, 0.64f, 0.64f);
            Live.draw(mark, cam, wx, wy);
        }
        Live.link(0, tubeY0, tubeZ, 0, ys, tubeZ, 0.84f, 0.84f);
        Live.draw(unitTube, cam, wx, wy);
        for(float y = tubeY0 + 0.4f + feed; y < ys - 1.2f; y += 1.8f){
            Live.link(0, y, tubeZ, 0, y + 0.12f, tubeZ, 0.88f, 0.88f);
            Live.draw(mark, cam, wx, wy);
        }

        Live.pose(0, ys, tubeZ + 0.42f + rollR, 0, -roll1);
        Live.draw(roll, cam, wx, wy);
        Live.pose(0, ys, 0, 2, 0f);
        Live.draw(bridge, cam, wx, wy);
    }
}
