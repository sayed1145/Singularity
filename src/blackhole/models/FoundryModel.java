package blackhole.models;

import arc.graphics.*;
import arc.math.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;
import blackhole.g3d.Kit.*;
import mindustry.gen.*;

import static blackhole.g3d.Kit.*;
import static blackhole.g3d.Mesh.*;

/**
 * Accretion Foundry (4x4): a fully procedural, indexing casting line in the Singularity style.
 *
 * <pre>
 *   north:  crucible furnace (west) -> hot conduit -> cooling fan tower (east), low capacitor bank between
 *   centre: indexing mould conveyor running west -> east between two covered hoods
 *           station -8.8  pour     : the crucible trough pours molten metal into an empty mould
 *           station  0.0  press    : the hydraulic ram stamps the cooled billet into an emblem plate
 *           station +8.8  pick     : a 3-axis arm lifts the plate out of its mould
 *   south:  the arm places the plate on a roller conveyor that carries it into the output housing
 * </pre>
 *
 * Every moving part is posed by {@link #animate}, a pure function of (progress, time, warmup) that emits parts to a
 * {@link Sink}. The live renderer and the offline exporter/baker use the same function, so the in-game motion and
 * the rendered previews are guaranteed to match. The layout keeps a clearance between every pair of moving and
 * static parts over the whole cycle (verified by tools/foundry: foundry.sh check).
 */
public class FoundryModel extends KitModel{
    // ------------------------------------------------------------------ layout constants (block units, half = 16)
    /** belt centre line, belt top, mould pitch */
    public static final float beltY = -1f, beltTopH = 1.3f, pitch = 4.4f;
    /** mould: outer half size, wall, floor and height */
    public static final float mouldR = 1.3f, mouldWall = 0.3f, mouldFloor = 0.3f, mouldH = 1.1f, cleatH = 0.03f;
    /** molten fill: half size, full billet height, pressed plate height, emblem thickness */
    public static final float fillR = 0.97f, billetH = 0.7f, plateH = 0.45f, emblemH = 0.02f;
    /** stations */
    public static final float pourX = -8.8f, pressX = 0f, pickX = 8.8f;
    /** press: column y, column size, beam bottom height (above deck), die size / height, head size / height */
    public static final float colYS = -4.6f, colYN = 2.6f, colW = 1.6f, beamH = 9.0f, dieR = 0.9f, dieH = 0.9f, headR = 1.3f, headH = 1.2f;
    /** pour nozzle tip height above deck */
    public static final float nozzleH = 4.0f;
    /** robot arm: base, shoulder height, link lengths, gripper (wrist -> pad bottom) */
    /** lateral offset and width of the arm's side-plate links */
    public static final float linkOff = 0.52f, linkW = 0.55f;
    /** height of the arm pedestal top above the deck (hazard pad 0.03 + pedestal 0.77) */
    public static final float pedestalH = 0.8f;
    public static final float armX = 4.5f, armY = -8f, shoulderH = 2.6f, L1 = 5.5f, L2 = 5.2f, padR = 0.7f, padH = 0.35f, wristRod = 0.9f;
    /** output roller conveyor */
    public static final float outY = -11.3f, outX0 = 6.8f, outX1 = 11.4f, dropX = 8.5f, rollerR = 0.4f, rollerZ = 1.2f, rollerPitch = 0.9f;
    /** plate travel on the rollers per cycle (= roller surface speed: 4 turns per cycle) */
    public static final float outSpeed = Mathf.PI2 * rollerR * 4f;
    /** cooling fan */
    public static final float fanX = 8.8f, fanY = 8.0f, fanBase = 2.2f, fanDuctR = 2.9f;
    /** hoods at both belt ends */
    public static final float hoodX = 11.0f, hoodOut = 15.2f, hoodRoof = 2.6f;

    /** Receiver of posed parts: the live renderer draws them, the exporter records them. */
    public interface Sink{
        /** multiplier for emissive faces of the following parts */
        void glow(float r, float g, float b);
        /** rigid part: rotation about one local axis through the mesh origin, then translation */
        void part(String id, Mesh m, float x, float y, float z, int axis, float deg);
        /** unit rod stretched from p0 to p1 with cross-section w x h */
        void link(String id, Mesh rod, float x0, float y0, float z0, float x1, float y1, float z1, float w, float h);
        /** additive light blob */
        void light(float x, float y, float z, float rad, Color c, float a);
        /** additive beam */
        void beam(float x0, float y0, float z0, float x1, float y1, float z1, float w, Color c, float a);
    }

    public final Mesh cleat, mould, mouldFront, plate, emblem, fill, stream, ram, fan, rollerY, turret, joint, upper, fore, gripper;
    public final Color hot = Color.valueOf("ffd9a0"), molten = Color.valueOf("ff8a3a"), spark = Color.valueOf("ffc27a");

    public FoundryModel(){
        super("accretion-foundry", 4, sing);
        float Z = deckZ;

        cleat = part();
        steel(cleat, s).box(-0.09f, -1.35f, 0f, 0.09f, 1.35f, cleatH);

        //open mould: floor + four walls, trim lip
        //split in a back half (floor + north wall) and a front half so contents are painted between them
        mould = part();
        mouldFront = part();
        float r = mouldR, w = mouldWall;
        dark(mould, s).box(-r, -r, 0f, r, r, mouldFloor);
        hull2(mould, s).box(-r, r - w, mouldFloor, r, r, mouldH);
        hull2(mouldFront, s).box(-r, -r, mouldFloor, r, -r + w, mouldH);
        hull2(mouldFront, s).box(-r, -r + w, mouldFloor, -r + w, r - w, mouldH);
        hull2(mouldFront, s).box(r - w, -r + w, mouldFloor, r, r - w, mouldH);
        trim(mouldFront, s).box(-r - 0.02f, -0.35f, mouldH - 0.3f, -r, 0.35f, mouldH - 0.1f);
        trim(mouldFront, s).box(r, -0.35f, mouldH - 0.3f, r + 0.02f, 0.35f, mouldH - 0.1f);

        //pressed plate: lit slab with a hot (emissive, tinted by temperature) top
        plate = part();
        plate.color(s.metal).style(metal, matPlain).bevel(-fillR, -fillR, 0f, fillR, fillR, plateH, 0.08f);
        plate.color(1f, 1f, 1f).style(emissive, 0).quad(-fillR + 0.1f, -fillR + 0.1f, plateH + 0.002f, fillR - 0.1f, -fillR + 0.1f, plateH + 0.002f,
            fillR - 0.1f, fillR - 0.1f, plateH + 0.002f, -fillR + 0.1f, fillR - 0.1f, plateH + 0.002f, 0, 0, 1);
        emblem = part();
        glow2(emblem, s).at(0, 0, plateH).ring(16, 0.38f, 0.58f, 0f, emblemH);

        fill = rod(Color.white, emissive);
        stream = rod(Color.white, emissive);

        //press ram: die (fits inside the mould) + head
        ram = part();
        steel(ram, s).cbevel(0, 0, 0, dieR * 2f, dieR * 2f, dieH, 0.1f);
        hull(ram, s).cbevel(0, 0, dieH, headR * 2f, headR * 2f, headH, 0.2f);
        trim(ram, s).box(-headR - 0.001f, -0.5f, dieH + 0.3f, -headR, 0.5f, dieH + 0.8f);
        glow(ram, s).box(-0.5f, -headR - 0.02f, dieH + 0.45f, 0.5f, -headR, dieH + 0.75f);

        //cooling fan: hub with lit cap and five pitched blades
        fan = part();
        //hub stands on the duct grate (grate top = 0.01)
        dark(fan, s).cyl(12, 0.6f, 0.01f, 0.7f);
        glow(fan, s).cyl(12, 0.3f, 0.7f, 0.74f);
        for(int i = 0; i < 5; i++){
            float a = i * 72f;
            fan.at(0, 0, 0).rot(2, a);
            steel(fan, s).hexa(true,
                0.5f, -0.35f, 0.30f, 2.6f, -0.5f, 0.28f, 2.6f, 0.5f, 0.02f, 0.5f, 0.35f, 0.08f,
                0.5f, -0.35f, 0.42f, 2.6f, -0.5f, 0.36f, 2.6f, 0.5f, 0.10f, 0.5f, 0.35f, 0.20f);
        }
        fan.at(0, 0, 0);

        //roller whose axis runs along y (spins about y)
        Mesh rx = roller(s.metal, s.dark, rollerR, 2.9f, 10);
        rollerY = part();
        rollerY.add(rx, 0, 0, 0, 2, 90f);

        //robot arm
        turret = part();
        dark(turret, s).cyl(16, 1.35f, 0f, 0.35f);
        hull(turret, s).at(0, 0, 0).lathe(16, 0, 1.25f, 0.35f, 1.1f, 1.0f, 0.001f, 1.1f);
        glow(turret, s).at(0, 0, 0).ring(16, 1.35f, 1.42f, 0.12f, 0.24f);
        for(int sd = -1; sd <= 1; sd += 2){
            hull2(turret, s).at(0, 0, 0).box(-0.55f, sd > 0 ? 0.83f : -1.09f, 0.9f, 0.55f, sd > 0 ? 1.09f : -0.83f, shoulderH - pedestalH + 0.5f);
        }
        turret.at(0, 0, 0);
        joint = part();
        dark(joint, s).at(0, 0, 0).rot(0, 90f).cyl(12, 0.5f, -0.79f, 0.79f);
        steel(joint, s).at(0, 0, 0).rot(0, 90f).cyl(12, 0.25f, 0.79f, 0.81f);
        steel(joint, s).at(0, 0, 0).rot(0, 90f).cyl(12, 0.25f, -0.81f, -0.79f);
        joint.at(0, 0, 0);
        //round links: their cross-section is independent of the link frame's roll
        upper = part();
        hull(upper, s).cyl(12, 0.5f, 0f, 1f);
        fore = part();
        hull2(fore, s).cyl(12, 0.5f, 0f, 1f);
        gripper = part();
        steel(gripper, s).cbox(0, 0, -wristRod, 0.4f, 0.4f, wristRod - 0.5f);
        hull(gripper, s).at(0, 0, -wristRod - padH).cyl(14, padR, 0f, padH);
        //status band around the pad rim (the pad bottom stays flat so it can sit on the emblem)
        glow(gripper, s).at(0, 0, -wristRod - padH).ring(14, padR, padR + 0.03f, 0.1f, 0.25f);
        gripper.at(0, 0, 0);
    }

    /** Factory for live meshes (the exporter overrides it to record convex primitives). */
    protected Mesh part(){
        return new Mesh();
    }

    // ------------------------------------------------------------------ static geometry

    @Override
    public void buildStatic(Mesh m){
        float Z = deck(m, s, half, deckZ), zb = Z + beltTopH;

        //---- mould conveyor: frame, rubber, rails, end drums
        dark(m, s).at(0, 0, 0).bevel(-14.6f, beltY - 2.0f, Z, 14.6f, beltY + 2.0f, Z + 1.2f, 0.12f);
        m.color(0.13f, 0.13f, 0.15f).style(0, matRubber).box(-14.6f, beltY - 1.4f, Z + 1.2f, 14.6f, beltY + 1.4f, zb);
        for(int sd = -1; sd <= 1; sd += 2){
            trim(m, s).box(-14.6f, beltY + sd * 2.0f - (sd > 0 ? 0.6f : 0f), Z + 1.2f, 14.6f, beltY + sd * 2.0f + (sd < 0 ? 0.6f : 0f), Z + 1.7f);
        }
        for(float px : new float[]{-6.6f, -2.2f, 2.2f, 6.6f}){
            hazard(m).box(px - 0.5f, beltY - 2.02f, Z + 0.3f, px + 0.5f, beltY - 2.0f, Z + 0.9f);
        }

        //---- hoods over both belt ends (open towards the stations)
        for(int sd = -1; sd <= 1; sd += 2){
            float xi = sd * hoodX, xo = sd * hoodOut, x0 = Math.min(xi, xo), x1 = Math.max(xi, xo);
            hull2(m, s).at(0, 0, 0).box(x0, beltY - 2.6f, Z, x1, beltY - 2.1f, Z + hoodRoof);
            hull2(m, s).box(x0, beltY + 2.1f, Z, x1, beltY + 2.6f, Z + hoodRoof);
            hull2(m, s).box(sd > 0 ? x1 - 0.6f : x0, beltY - 2.1f, Z, sd > 0 ? x1 : x0 + 0.6f, beltY + 2.1f, Z + hoodRoof);
            hull(m, s).bevel(x0 - 0.1f, beltY - 2.7f, Z + hoodRoof, x1 + 0.1f, beltY + 2.7f, Z + hoodRoof + 0.5f, 0.2f);
            fins(m, s).box(x0 + 0.6f, beltY - 1.9f, Z + hoodRoof + 0.5f, x1 - 0.6f, beltY - 0.2f, Z + hoodRoof + 0.52f);
            glow(m, s).box(x0 + 0.8f, beltY + 0.6f, Z + hoodRoof + 0.5f, x1 - 0.8f, beltY + 1.2f, Z + hoodRoof + 0.53f);
        }

        //---- press gantry
        for(float cy : new float[]{colYS, colYN}){
            hull2(m, s).at(0, 0, 0).cbevel(pressX, cy, Z, colW, colW, beamH, 0.15f);
            dark(m, s).cbevel(pressX, cy, Z, colW + 0.6f, colW + 0.6f, 0.4f, 0.08f);
            dark(m, s).box(pressX - 0.5f, cy - colW / 2f - 0.02f, Z + 2.0f, pressX + 0.5f, cy - colW / 2f, Z + 7.0f);
            glow2(m, s).box(pressX - 0.25f, cy - colW / 2f - 0.04f, Z + 5.6f, pressX + 0.25f, cy - colW / 2f - 0.02f, Z + 6.6f);
        }
        hull(m, s).at(0, 0, 0).bevel(pressX - 1.5f, colYS - 0.9f, Z + beamH, pressX + 1.5f, colYN + 0.9f, Z + beamH + 1.2f, 0.25f);
        hazard(m).box(pressX - 1.52f, colYS - 0.92f, Z + beamH + 0.2f, pressX + 1.52f, colYS - 0.9f, Z + beamH + 0.7f);
        steel(m, s).at(pressX, beltY, Z + beamH + 1.2f).lathe(12, 0, 1.2f, 0f, 1.2f, 0.9f, 0.8f, 1.3f, 0.001f, 1.3f);
        dark(m, s).at(pressX, beltY, Z + beamH + 1.2f).ring(12, 1.2f, 1.36f, 0.2f, 0.45f);

        //---- crucible furnace with pouring trough
        float fx = pourX, fy = 6.8f, fr = 3.0f;
        dark(m, s).at(fx, fy, Z).cyl(20, fr + 0.4f, 0f, 0.5f);
        hull(m, s).at(fx, fy, Z + 0.5f).lathe(20, 0, fr, 0f, fr, 3.2f, fr - 0.5f, 4.2f, fr - 0.9f, 4.4f, 0.001f, 4.4f);
        dark(m, s).at(fx, fy, Z + 0.5f).ring(20, fr, fr + 0.12f, 2.0f, 2.3f);
        trim(m, s).at(fx, fy, Z + 4.9f).ring(20, 1.25f, 1.7f, 0f, 0.3f);
        glow(m, s).at(fx, fy, Z + 4.9f).cyl(20, 1.25f, 0f, 0.2f);
        for(int i = 0; i < 4; i++){
            float a = 45f + i * 90f;
            m.at(fx, fy, Z + 0.5f).rot(2, a);
            hull2(m, s).hexa(false, fr - 0.1f, -0.3f, 0f, fr + 0.7f, -0.3f, 0f, fr + 0.7f, 0.3f, 0f, fr - 0.1f, 0.3f, 0f,
                fr - 0.1f, -0.3f, 3.0f, fr + 0.05f, -0.3f, 3.0f, fr + 0.05f, 0.3f, 3.0f, fr - 0.1f, 0.3f, 3.0f);
        }
        m.at(0, 0, 0);
        dark(m, s).box(fx - 0.7f, beltY - 0.6f, Z + 4.6f, fx + 0.7f, fy - 2.0f, Z + 5.3f);
        glow(m, s).box(fx - 0.35f, beltY - 0.3f, Z + 5.3f, fx + 0.35f, fy - 2.0f, Z + 5.32f);
        trim(m, s).box(fx - 0.72f, beltY - 0.62f, Z + 5.1f, fx + 0.72f, fy - 2.0f, Z + 5.2f);
        steel(m, s).at(fx, beltY, Z + nozzleH).lathe(10, 0, 0.18f, 0f, 0.5f, 0.6f, 0.001f, 0.6f);
        glow(m, s).at(fx, beltY, Z + nozzleH - 0.02f).cyl(10, 0.16f, 0f, 0.02f);
        hull2(m, s).at(0, 0, 0).cbevel(fx, 2.4f, Z, 0.8f, 0.8f, 4.6f, 0.1f);

        //---- hot conduit furnace -> fan tower, with saddles
        conduit(m, s, fx + fr, fy, Z + 1.2f, fanX - 3.5f, fy, Z + 1.2f, 0.4f);
        for(float px : new float[]{-2.5f, 2.5f}) dark(m, s).at(0, 0, 0).cbox(px, fy, Z, 0.8f, 0.9f, 0.8f);
        glow(m, s).at(0, 0, 0).axis(-1.4f, fy, Z + 1.2f, 1.4f, fy, Z + 1.2f).ring(8, 0.4f, 0.46f, 0f, 2.8f);
        m.at(0, 0, 0);

        //---- cooling fan tower
        hull(m, s).at(0, 0, 0).bevel(fanX - 3.5f, fanY - 3.5f, Z, fanX + 3.5f, fanY + 3.5f, Z + fanBase, 0.3f);
        grate(m, s).at(fanX, fanY, Z + fanBase).cyl(24, fanDuctR - 0.05f, 0f, 0.01f);
        hull2(m, s).at(fanX, fanY, Z + fanBase).ring(24, fanDuctR, fanDuctR + 0.45f, 0f, 1.4f);
        glow(m, s).at(fanX, fanY, Z + fanBase).ring(24, fanDuctR + 0.45f, fanDuctR + 0.53f, 0.9f, 1.1f);
        for(int sx = -1; sx <= 1; sx += 2){
            for(int sy = -1; sy <= 1; sy += 2){
                steel(m, s).at(fanX + sx * 2.9f, fanY + sy * 2.9f, Z + fanBase).cyl(8, 0.3f, 0f, 0.15f);
            }
        }
        m.at(0, 0, 0);

        //---- low capacitor bank on the north edge
        dark(m, s).at(0, 0, 0).bevel(-3.6f, 10.4f, Z, 3.6f, 13.6f, Z + 0.35f, 0.08f);
        for(int i = 0; i < 4; i++){
            float x = -2.7f + i * 1.8f;
            hull2(m, s).cbevel(x, 12.0f, Z + 0.35f, 1.4f, 2.6f, 0.55f, 0.1f);
            glow2(m, s).box(x - 0.3f, 10.68f, Z + 0.5f, x + 0.3f, 10.7f, Z + 0.75f);
        }

        //---- side tank, console, coolant tank, vent
        tank(m, s, -13.6f, 6.5f, Z, 1.1f, 3.0f, true);
        console(m, s, -9.5f, -10.0f, Z);
        tank(m, s, -3.0f, -10.5f, Z, 1.4f, 3.2f, true);
        vent(m, s, -13.0f, -12.8f, Z, 2.4f, 2.4f, 0.8f);

        //---- robot arm pedestal on a hazard pad
        hazard(m).at(0, 0, 0).box(armX - 2.2f, armY - 2.4f, Z, armX + 2.2f, armY + 2.4f, Z + 0.03f);
        dark(m, s).at(armX, armY, Z + 0.03f).lathe(20, 0, 1.8f, 0f, 1.8f, 0.4f, 1.5f, 0.77f, 0.001f, 0.77f);

        //---- output roller conveyor and housing
        dark(m, s).at(0, 0, 0).bevel(outX0, outY - 1.5f, Z, outX1, outY + 1.5f, Z + 0.6f, 0.1f);
        for(int sd = -1; sd <= 1; sd += 2){
            trim(m, s).box(outX0, outY + sd * 1.5f - (sd > 0 ? 0f : 0.4f), Z, outX1, outY + sd * 1.5f + (sd > 0 ? 0.4f : 0f), Z + 1.9f);
        }
        hull2(m, s).box(outX1, outY - 2.6f, Z, hoodOut, outY - 1.9f, Z + 2.4f);
        hull2(m, s).box(outX1, outY + 1.9f, Z, hoodOut, outY + 2.6f, Z + 2.4f);
        hull2(m, s).box(hoodOut - 0.6f, outY - 1.9f, Z, hoodOut, outY + 1.9f, Z + 2.4f);
        dark(m, s).box(outX1, outY - 1.9f, Z, hoodOut - 0.6f, outY + 1.9f, Z + 0.6f);
        hull(m, s).bevel(outX1 - 0.1f, outY - 2.7f, Z + 2.4f, hoodOut + 0.1f, outY + 2.7f, Z + 2.9f, 0.2f);
        glow(m, s).box(outX1 + 0.7f, outY + 0.8f, Z + 2.9f, hoodOut - 0.7f, outY + 1.4f, Z + 2.93f);
        fins(m, s).box(outX1 + 0.6f, outY - 1.9f, Z + 2.9f, hoodOut - 0.6f, outY - 0.3f, Z + 2.92f);
        m.at(0, 0, 0);
    }

    @Override
    public void buildEnvelope(Mesh m){
        float Z = deckZ, zb = Z + beltTopH;
        //moulds and their contents along the belt
        m.at(0, 0, 0).box(-14.6f, beltY - mouldR, zb, 14.6f, beltY + mouldR, zb + cleatH + mouldH + 0.1f);
        //press ram + rod
        m.box(pressX - headR, beltY - headR, zb, pressX + headR, beltY + headR, Z + beamH);
        //pour stream
        m.box(pourX - 0.3f, beltY - 0.3f, zb, pourX + 0.3f, beltY + 0.3f, Z + nozzleH);
        //robot arm working volume (stays west of the hoods)
        m.box(armX - 2.6f, -13.6f, Z + pedestalH, 10.4f, 0.6f, Z + shoulderH + L1 + 1.2f);
        //rollers and plates on the output conveyor
        m.box(outX0, outY - 1.45f, Z + rollerZ - rollerR, hoodOut - 0.6f, outY + 1.45f, Z + rollerZ + rollerR + plateH + emblemH + 0.1f);
        //fan
        m.at(fanX, fanY, Z + fanBase).cyl(16, 2.7f, 0f, 0.8f);
        m.at(0, 0, 0);
    }

    @Override
    public void buildRest(Mesh m){
        animate(0.47f, 0.47f * craftTicks, 1f, new MeshSink(m));
    }

    // ------------------------------------------------------------------ animation (pure)

    /** ticks per craft; the fan turns 360 degrees and the rollers 4 turns per craft so every cycle loops seamlessly */
    public static final float craftTicks = 120f;

    /** belt index progress 0..1 (moves during the first 35% of the craft, dwells for the stations after) */
    public static float index(float p){
        return smooth(0f, 0.35f, p);
    }

    /** temperature 1 (white hot) .. 0 (cold) of metal at belt / output position x */
    public static float heat(float x, boolean output){
        if(output) return Mathf.clamp(0.24f - (x - dropX) * 0.02f);
        return Mathf.clamp(1f - (x - pourX) / (pickX - pourX) * 0.76f);
    }

    /** emissive tint for a temperature */
    public static void heatTint(Sink s, float t){
        //blackbody-ish ramp: dark red -> orange -> pale yellow
        float r = Mathf.clamp(0.25f + t * 1.6f), g = Mathf.clamp(-0.1f + t * 1.1f), b = Mathf.clamp(-0.35f + t * 0.95f);
        s.glow(r, Math.max(g, 0.06f), Math.max(b, 0.03f));
    }

    /** height of the die's bottom above the mould floor */
    public static float dieLift(float p){
        float up = mouldH - mouldFloor + 0.6f, contact = billetH, pressed = plateH + emblemH;
        if(p < 0.42f) return up;
        if(p < 0.50f){ float u = (p - 0.42f) / 0.08f; return Mathf.lerp(up, contact, u * u); }
        if(p < 0.56f) return Mathf.lerp(contact, pressed, smooth(0.50f, 0.56f, p));
        if(p < 0.62f) return pressed;
        if(p < 0.85f) return Mathf.lerp(pressed, up, smooth(0.62f, 0.85f, p));
        return up;
    }

    /** pad-bottom target of the robot arm (x, y, z above deck) and whether it holds a plate */
    public void armTarget(float p, float[] out){
        float pickZ = mouldFloor + beltTopH + cleatH + plateH + emblemH, dropZ = rollerZ + rollerR + plateH + emblemH, hover = 7.0f;
        float x, y, z;
        if(p < 0.30f){
            float u = smooth(0f, 0.30f, p);
            x = Mathf.lerp(dropX, pickX, u); y = Mathf.lerp(outY, beltY, u); z = hover;
        }else if(p < 0.38f){
            x = pickX; y = beltY; z = hover;
        }else if(p < 0.48f){
            x = pickX; y = beltY; z = Mathf.lerp(hover, pickZ, smooth(0.38f, 0.48f, p));
        }else if(p < 0.52f){
            x = pickX; y = beltY; z = pickZ;
        }else if(p < 0.62f){
            x = pickX; y = beltY; z = Mathf.lerp(pickZ, hover, smooth(0.52f, 0.62f, p));
        }else if(p < 0.78f){
            float u = smooth(0.62f, 0.78f, p);
            x = Mathf.lerp(pickX, dropX, u); y = Mathf.lerp(beltY, outY, u); z = hover;
        }else if(p < 0.86f){
            x = dropX; y = outY; z = Mathf.lerp(hover, dropZ, smooth(0.78f, 0.86f, p));
        }else if(p < 0.88f){
            x = dropX; y = outY; z = dropZ;
        }else{
            x = dropX; y = outY; z = Mathf.lerp(dropZ, hover, smooth(0.88f, 1f, p));
        }
        out[0] = x; out[1] = y; out[2] = z;
    }

    final float[] tgt = new float[3];

    /**
     * Poses every moving part for craft progress p (0..1), continuous time t (ticks) and warmup w, in painter order
     * (far / low parts first) as required by the depth-buffer-free live renderer.
     */
    public void animate(float p, float t, float w, Sink out){
        float Z = deckZ, zb = Z + beltTopH, mb = zb + cleatH, floor = mb + mouldFloor;
        float idx = index(p);
        float shift = idx * pitch;

        //---- fan (north, far)
        out.glow(0.6f + 0.4f * w, 0.6f + 0.4f * w, 0.6f + 0.4f * w);
        out.part("fan", fan, fanX, fanY, Z + fanBase, 2, t * 3f);

        //---- belt cleats (the belt moves exactly 4 cleat pitches per craft)
        out.glow(1f, 1f, 1f);
        float cp = 1.1f, off = shift % cp;
        for(int i = 0; i < 22; i++){
            float x = -11.55f + i * cp + off;
            if(x > 11.6f) continue;
            out.part("cleat" + i, cleat, x, beltY, zb, 2, 0f);
        }

        //---- moulds and contents; mould k starts the craft at slot k and ends it at slot k+1
        float die = dieLift(p);
        for(int k = 0; k < 6; k++){
            float x = -13.2f + k * pitch + shift;
            out.glow(1f, 1f, 1f);
            out.part("mould" + k, mould, x, beltY, mb, 2, 0f);
            float hh = heat(x, false);
            if(k == 0){
                float h = billetH * smooth(0.42f, 0.78f, p);
                if(h > 0.01f){
                    heatTint(out, 1f);
                    out.link("fill" + k, fill, x, beltY, floor, x, beltY, floor + h, fillR * 2f, fillR * 2f);
                }
            }else if(k == 1){
                heatTint(out, hh);
                out.link("fill" + k, fill, x, beltY, floor, x, beltY, floor + billetH, fillR * 2f, fillR * 2f);
            }else if(k == 2){
                boolean pressedPlate = p >= 0.56f && die >= plateH + emblemH + 0.12f;
                if(pressedPlate){
                    heatTint(out, hh);
                    out.part("plate" + k, plate, x, beltY, floor, 2, 0f);
                    out.glow(0.5f + 0.5f * w, 0.5f + 0.5f * w, 0.5f + 0.5f * w);
                    out.part("emblem" + k, emblem, x, beltY, floor, 2, 0f);
                }else{
                    heatTint(out, hh);
                    float h = Math.min(billetH, die);
                    out.link("fill" + k, fill, x, beltY, floor, x, beltY, floor + h, fillR * 2f, fillR * 2f);
                }
            }else if(k == 3 || (k == 4 && p < 0.52f)){
                heatTint(out, hh);
                out.part("plate" + k, plate, x, beltY, floor, 2, 0f);
                out.glow(0.5f + 0.5f * w, 0.5f + 0.5f * w, 0.5f + 0.5f * w);
                out.part("emblem" + k, emblem, x, beltY, floor, 2, 0f);
            }
            out.glow(1f, 1f, 1f);
            out.part("mouldF" + k, mouldFront, x, beltY, mb, 2, 0f);
        }

        //---- pour stream (falls in, then detaches from the nozzle)
        if(p > 0.40f && p < 0.80f){
            float surf = floor + billetH * smooth(0.42f, 0.78f, p), tip = Z + nozzleH - 0.02f;
            float bot = Mathf.lerp(tip, surf, Mathf.clamp((p - 0.40f) / 0.02f));
            float top = p > 0.78f ? Mathf.lerp(tip, surf, (p - 0.78f) / 0.02f) : tip;
            if(top - bot > 0.02f){
                heatTint(out, 1f);
                out.link("stream", stream, pourX, beltY, bot, pourX, beltY, top, 0.3f, 0.3f);
            }
        }

        //---- press ram and piston rod
        out.glow(0.4f + 0.6f * w, 0.4f + 0.6f * w, 0.4f + 0.6f * w);
        float dz = floor + die;
        out.part("ram", ram, pressX, beltY, dz, 2, 0f);
        out.link("rod", rodMesh(), pressX, beltY, dz + dieH + headH, pressX, beltY, Z + beamH, 0.8f, 0.8f);

        //---- output rollers and plates travelling into the housing
        out.glow(1f, 1f, 1f);
        float spin = p * 360f * 4f;
        for(int i = 0; i < 8; i++){
            float x = outX0 + 0.45f + i * rollerPitch;
            out.part("roller" + i, rollerY, x, outY, Z + rollerZ, 1, spin);
        }
        //plate released this craft (p >= 0.88) and the one released last craft (p < 0.35)
        float onRoll = Z + rollerZ + rollerR;
        if(p >= 0.86f){
            float x = dropX + outSpeed * Math.max(0f, p - 0.88f);
            outPlate(out, "outA", x, onRoll, w);
        }
        if(p < 0.35f){
            float x = dropX + outSpeed * (p + 0.12f);
            outPlate(out, "outB", x, onRoll, w);
        }

        //---- robot arm
        armTarget(p, tgt);
        float px = tgt[0], py = tgt[1], pz = Z + tgt[2];
        boolean holding = p >= 0.52f && p < 0.86f;
        if(holding){
            float x = px, y = py, zPlate = pz - emblemH - plateH;
            heatTint(out, heat(pickX, false));
            out.part("carried", plate, x, y, zPlate, 2, 0f);
            out.glow(0.5f + 0.5f * w, 0.5f + 0.5f * w, 0.5f + 0.5f * w);
            out.part("carriedEmblem", emblem, x, y, zPlate, 2, 0f);
        }
        float sx = armX, sy = armY, sz = Z + shoulderH;
        float wx = px, wy = py, wz = pz + padH + wristRod;
        float yaw = (float)Math.toDegrees(Math.atan2(wy - sy, wx - sx));
        float d = Mathf.dst(sx, sy, wx, wy), h = wz - sz, r = Math.min((float)Math.sqrt(d * d + h * h), L1 + L2 - 0.01f);
        float a1 = (float)Math.atan2(h, d) + (float)Math.acos(Mathf.clamp((L1 * L1 + r * r - L2 * L2) / (2f * L1 * r), -1f, 1f));
        float ed = (float)Math.cos(a1) * L1, ez = (float)Math.sin(a1) * L1;
        float ex = sx + (float)Math.cos(Math.toRadians(yaw)) * ed, ey = sy + (float)Math.sin(Math.toRadians(yaw)) * ed, ezz = sz + ez;
        out.glow(0.5f + 0.5f * w, 0.5f + 0.5f * w, 0.5f + 0.5f * w);
        out.part("turret", turret, armX, armY, Z + pedestalH, 2, yaw);
        out.part("shoulder", joint, sx, sy, sz, 2, yaw);
        //upper arm and forearm are side plates on opposite sides of the pin joints, so they never touch
        float lx = -(float)Math.sin(Math.toRadians(yaw)) * linkOff, ly = (float)Math.cos(Math.toRadians(yaw)) * linkOff;
        out.link("upper", upper, sx - lx, sy - ly, sz, ex - lx, ey - ly, ezz, linkW, linkW);
        out.part("elbow", joint, ex, ey, ezz, 2, yaw);
        out.link("fore", fore, ex + lx, ey + ly, ezz, wx + lx, wy + ly, wz, linkW, linkW);
        float g = holding || (p >= 0.48f && p < 0.52f) ? 1f : 0.25f;
        out.glow(g, g, g);
        out.part("gripper", gripper, wx, wy, wz, 2, 0f);
        out.part("wrist", joint, wx, wy, wz, 2, yaw);

        //---- light and sparks (additive, always on top)
        out.light(pourX, 6.8f, Z + 5.0f, 2.2f, molten, w * (0.35f + 0.15f * Mathf.absin(t, 7f, 1f)));
        if(p > 0.40f && p < 0.80f) out.light(pourX, beltY, floor + 0.5f, 1.9f, molten, w * 0.55f);
        float impact = p >= 0.50f && p < 0.64f ? 1f - (p - 0.50f) / 0.14f : 0f;
        if(impact > 0f){
            out.light(pressX, beltY, floor + 0.8f, 2.0f + (1f - impact) * 2.2f, spark, w * impact * 0.8f);
            for(int i = 0; i < 14; i++){
                float ang = i * 137.5f, sp = 2.6f + (i * 7 % 5) * 0.55f, life = 1f - impact;
                float rx = pressX + Mathf.cosDeg(ang) * sp * life * 1.6f, ry = beltY + Mathf.sinDeg(ang) * sp * life * 1.6f;
                float rz = floor + 0.8f + life * 2.8f - life * life * 3.4f;
                out.light(rx, ry, Math.max(rz, zb), 0.28f, spark, w * impact);
            }
        }
        if(holding) out.light(px, py, pz - 0.2f, 0.9f, molten, w * 0.35f);
    }

    void outPlate(Sink out, String id, float x, float z, float w){
        if(x > 13.2f) return;
        heatTint(out, heat(x, true));
        out.part(id, plate, x, outY, z, 2, 0f);
        out.glow(0.5f + 0.5f * w, 0.5f + 0.5f * w, 0.5f + 0.5f * w);
        out.part(id + "Emblem", emblem, x, outY, z, 2, 0f);
    }

    Mesh rodMesh;

    Mesh rodMesh(){
        if(rodMesh == null) rodMesh = rod(s.metal, metal);
        return rodMesh;
    }

    // ------------------------------------------------------------------ in-game drawing

    /** Draws posed parts with {@link Live}; lights and beams are deferred to {@link #drawOver}. */
    final class LiveSink implements Sink{
        Building b;
        boolean fx;

        @Override public void glow(float r, float g, float bl){ Live.glowR = r; Live.glowG = g; Live.glowB = bl; }
        @Override public void part(String id, Mesh m, float x, float y, float z, int axis, float deg){ if(!fx) draw(m, b, x, y, z, axis, deg); }
        @Override public void link(String id, Mesh rod, float x0, float y0, float z0, float x1, float y1, float z1, float w, float h){
            if(fx) return;
            Live.link(x0, y0, z0, x1, y1, z1, w, h);
            Live.draw(rod, cam, b.x, b.y);
        }
        @Override public void light(float x, float y, float z, float rad, Color c, float a){ if(fx) FoundryModel.this.light(b, x, y, z, rad, c, a); }
        @Override public void beam(float x0, float y0, float z0, float x1, float y1, float z1, float w, Color c, float a){ if(fx) FoundryModel.this.beam(b, x0, y0, z0, x1, y1, z1, w, c, a); }
    }

    final LiveSink live = new LiveSink();

    float craftP(Building b){
        return b == null ? 0f : Mathf.clamp(prog(b)) % 1f;
    }

    @Override
    public void drawLive(Building b){
        live.b = b;
        live.fx = false;
        animate(craftP(b), total(b), warm(b), live);
        Live.resetTint();
    }

    @Override
    public void drawOver(Building b){
        live.b = b;
        live.fx = true;
        animate(craftP(b), total(b), warm(b), live);
        Live.resetTint();
    }

    @Override
    public void drawCheap(Building b){
        float w = warm(b);
        light(b, pourX, 6.8f, deckZ + 5.0f, 2.2f, molten, w * 0.4f);
    }

    /** Flattens posed parts into one mesh (rest pose for icons / baker previews). */
    public static final class MeshSink implements Sink{
        final Mesh out;
        final float[] mat = new float[12];

        public MeshSink(Mesh out){
            this.out = out;
        }

        @Override public void glow(float r, float g, float b){}
        @Override public void part(String id, Mesh m, float x, float y, float z, int axis, float deg){ out.add(m, x, y, z, axis, deg); }
        @Override public void link(String id, Mesh rod, float x0, float y0, float z0, float x1, float y1, float z1, float w, float h){
            linkMatrix(x0, y0, z0, x1, y1, z1, w, h, mat);
            out.addMatrix(rod, mat, 0);
        }
        @Override public void light(float x, float y, float z, float rad, Color c, float a){}
        @Override public void beam(float x0, float y0, float z0, float x1, float y1, float z1, float w, Color c, float a){}
    }

    /** Same frame as {@link Live#link}, as a row-major 3x4 matrix. */
    public static void linkMatrix(float x0, float y0, float z0, float x1, float y1, float z1, float w, float h, float[] o){
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float l = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if(l < 1e-4f){ dx = 0; dy = 0; dz = 1e-4f; l = 1e-4f; }
        float nx = dx / l, ny = dy / l, nz = dz / l;
        float hx = Math.abs(nz) < 0.9f ? 0f : 1f, hz = Math.abs(nz) < 0.9f ? 1f : 0f;
        float ux = -hz * ny, uy = hz * nx - hx * nz, uz = hx * ny;
        float ul = (float)Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul; uy /= ul; uz /= ul;
        float vx = ny * uz - nz * uy, vy = nz * ux - nx * uz, vz = nx * uy - ny * ux;
        o[0] = ux * w; o[1] = vx * h; o[2] = dx; o[3] = x0;
        o[4] = uy * w; o[5] = vy * h; o[6] = dy; o[7] = y0;
        o[8] = uz * w; o[9] = vz * h; o[10] = dz; o[11] = z0;
    }
}
