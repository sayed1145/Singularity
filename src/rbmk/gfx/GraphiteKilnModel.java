package rbmk.gfx;

import arc.graphics.*;
import arc.math.*;
import mindustry.gen.*;

import static rbmk.gfx.Mesh.*;

/**
 * Graphite calcining kiln: a long rotary kiln turns on two tyre / trunnion-roller piers, driven by a girth gear and a
 * counter-rotating pinion from the motor under its south flank. Hot gas leaves the feed hood (west) through the
 * preheater and the stack (steam puffs), raw coke comes up the bucket elevator; the burner hood (east) glows through
 * a sight glass and calcined graphite lumps leave it down a chute onto a cleated belt that carries them west into the
 * discharge hood over the product pit, cooling from orange to graphite black on the way. Four shell-cooling fans spin
 * along the north flank.
 *
 * <p>Every motion is a function of totalProgress only and all ratios are integers (gear 24:8, fans 4:1, belt 6 pitches
 * per turn, 2 puff cycles per turn), so one drum revolution is an exact loop and everything eases in/out with warm-up.
 */
public class GraphiteKilnModel extends FactoryModel{
    public static final GraphiteKilnModel instance = new GraphiteKilnModel();

    //drum: axis along x at (y=ay, z=az)
    static final float ay = 1.2f, az = 4.6f, R = 1.6f, dx0 = -6.6f, dx1 = 7.3f, hotX = 3.2f;
    static final float tyreA = -3.6f, tyreB = 4.6f, tyreR = 1.98f, gearX = 1.4f, gearR = 2.05f, toothR = 2.3f;
    static final int sides = 24, gearTeeth = 24, pinionTeeth = 8;
    //pinion and drive train, south of the drum under the girth gear
    static final float pinY = -1.45f, pinZ = 3.64f, pinR = 0.55f, pinX0 = 1.1f, pinX1 = 1.7f, pinPhase = 12.5f;
    //hoods
    static final float feedX0 = -9.1f, feedX1 = -5.9f, burnX0 = 6.6f, burnX1 = 10.0f;
    static final float hoodY0 = -1.0f, hoodY1 = 3.4f, feedTop = 7.2f, burnTop = 6.9f, preX = -7.5f, preY = 2.2f;
    //stack
    static final float stX = -7.9f, stY = -2.6f, stR = 0.7f, stTop = 11.2f;
    //product belt (runs east -> west), 9 cells of one cleat pitch each
    static final float beltY = -4.4f, beltTop = 2.2f, beltX0 = -4.6f, beltX1 = 8.9f, pitch = 1.5f, beltL = beltX1 - beltX0;
    static final int cells = 9;
    //chute from the burner hood down onto the belt
    static final float chX = 7.5f, chY0 = hoodY0, chZ0 = 4.0f, chY1 = beltY, chZ1 = 2.6f;
    static final float chL = (float)Math.hypot(chY0 - chY1, chZ0 - chZ1);
    static final float chA = (float)Math.toDegrees(Math.atan2(chZ0 - chZ1, chY0 - chY1));
    //shell cooling fans along the north flank
    static final float fanY = 7.0f, fanZ = 2.35f;
    static final float[] fanX = {-5.0f, -1.6f, 1.8f, 5.2f};
    //burner sight glass on the south face of the burner hood
    static final float winX = 9.0f, winZ = 4.6f;
    /** drum degrees per tick of totalProgress; every other motion is geared to the drum */
    public static final float drumDeg = 1.25f;

    final Mesh drum = new Mesh(), pinion = new Mesh(), fan = new Mesh(), cleat = new Mesh(), glow = new Mesh(), puff = new Mesh();
    final Mesh lumpA = new Mesh(), lumpB = new Mesh(), hotA = new Mesh(), hotB = new Mesh();
    final float[] puffU = new float[4];
    final int[] puffOrder = new int[4];

    public GraphiteKilnModel(){
        super("graphite-kiln", new Color(0.90f, 0.38f, 0.34f));
        //---- drum (origin on the axis, local x = axis)
        drum.color(Pal.offWhite).style(0, 0).at(dx0, 0, 0).rot(1, 90).tubeSide(sides, R, 0, hotX - dx0);
        drum.color(0.79f, 0.75f, 0.70f).style(0, 0).at(hotX, 0, 0).rot(1, 90).tubeSide(sides, R, 0, dx1 - hotX);
        for(int i = 0; i < 6; i++){
            drum.color(Pal.mid).style(0, 0).at(0, 0, 0).rot(0, i * 60f + 30f).box(dx0 + 0.5f, -0.1f, R - 0.02f, dx1 - 0.5f, 0.1f, R + 0.07f);
        }
        drum.color(Pal.darkSteel).style(metal, 0).at(hotX - 0.15f, 0, 0).rot(1, 90).lathe(sides, 180f / sides, R, 0, R + 0.1f, 0, R + 0.1f, 0.3f, R, 0.3f);
        for(float tx : new float[]{tyreA, tyreB}){
            for(int i = 0; i < 8; i++){
                for(int s = -1; s <= 1; s += 2){
                    drum.color(Pal.dark).style(metal, 0).at(tx + s * 0.68f, 0, 0).rot(0, i * 45f + 22.5f).box(-0.16f, -0.22f, R - 0.02f, 0.16f, 0.22f, R + 0.16f);
                }
            }
            drum.color(Pal.steel).style(metal, 0).at(tx - 0.45f, 0, 0).rot(1, 90).lathe(sides, 180f / sides, R, 0, tyreR, 0, tyreR, 0.9f, R, 0.9f);
        }
        for(float bx : new float[]{feedX1 + 0.3f, burnX0 - 0.55f}){
            drum.color(accent).style(0, 0).at(bx, 0, 0).rot(1, 90).lathe(sides, 180f / sides, R, 0, R + 0.2f, 0, R + 0.2f, 0.25f, R, 0.25f);
        }
        drum.color(Pal.dark).style(metal, 0).at(gearX - 0.3f, 0, 0).rot(1, 90).lathe(sides, 180f / sides, R, 0, gearR, 0, gearR, 0.6f, R, 0.6f);
        for(int i = 0; i < gearTeeth; i++){
            drum.color(Pal.light).style(metal, 0).at(gearX, 0, 0).rot(0, i * 360f / gearTeeth).box(-0.3f, -0.13f, gearR - 0.02f, 0.3f, 0.13f, toothR);
        }
        //---- pinion (origin on its own axis)
        pinion.color(Pal.offWhite).style(metal, 0).at(pinX0, 0, 0).rot(1, 90).cyl(12, pinR, 0, pinX1 - pinX0);
        pinion.color(accent).style(0, 0).at(pinX0 - 0.03f, 0, 0).rot(1, 90).cyl(12, pinR * 0.6f, 0, 0.03f);
        for(int i = 0; i < pinionTeeth; i++){
            pinion.color(Pal.darkSteel).style(metal, 0).at((pinX0 + pinX1) / 2f, 0, 0).rot(0, i * 360f / pinionTeeth).box(-0.28f, -0.12f, pinR - 0.02f, 0.28f, 0.12f, pinR + 0.2f);
        }
        //---- fan impeller (rotates about z)
        fan.color(Pal.dark).style(metal, 0).at(0, 0, 0).cyl(10, 0.22f, 0, 0.3f);
        for(int i = 0; i < 5; i++){
            fan.color(i == 0 ? accent : Pal.offWhite).style(metal, 0).at(0, 0, 0.15f).rot(2, i * 72f).rot(0, 30f).box(0.2f, -0.03f, -0.17f, 0.8f, 0.03f, 0.17f, true);
        }
        //---- belt cleat, graphite lumps (cold) and their hot overlays
        cleat.color(Pal.mid).style(0, 0).at(0, 0, 0).box(-0.06f, -0.72f, 0f, 0.06f, 0.72f, 0.14f);
        lumpA.color(Pal.graphite).style(metal, 0).at(0, 0, 0).cbevel(0, 0, 0, 0.6f, 0.46f, 0.42f, 0.14f);
        lumpB.color(Pal.graphite).style(metal, 0).at(0, 0, 0).cbevel(0, 0, 0, 0.48f, 0.4f, 0.5f, 0.12f);
        hotA.color(1f, 0.55f, 0.25f).style(emissive, 0).at(0, 0, 0).cbevel(0, 0, 0, 0.62f, 0.48f, 0.43f, 0.14f);
        hotB.color(1f, 0.55f, 0.25f).style(emissive, 0).at(0, 0, 0).cbevel(0, 0, 0, 0.5f, 0.42f, 0.51f, 0.12f);
        //---- sight glass disc facing south, steam puff (unit sphere from z=0 to 1, scaled with Live.link)
        glow.color(Pal.glowWhite).style(emissive, 0).at(0, 0, 0).rot(0, 90).cyl(16, 0.5f, 0, 0.03f);
        puff.color(0.85f, 0.86f, 0.88f).style(0, 0).at(0, 0, 0).lathe(8, 22.5f, 0, 0, 0.36f, 0.1f, 0.5f, 0.5f, 0.36f, 0.9f, 0, 1f);
    }

    /** Small sphere closing a pipe elbow. */
    static void elbow(Mesh m, float x, float y, float z, float r){
        m.at(x, y, z).lathe(10, 0, 0, -r, r * 0.72f, -r * 0.7f, r, 0, r * 0.72f, r * 0.7f, 0, r);
    }

    @Override
    public void buildStatic(Mesh m){
        deck(m);
        //---- piers with trunnion rollers
        for(float px : new float[]{tyreA, tyreB}){
            m.color(Pal.concrete).style(0, matConcrete).at(0, 0, 0).cbevel(px, ay, deckZ, 2.2f, 4.0f, 0.6f, 0.1f);
            for(int s = -1; s <= 1; s += 2){
                float ry = ay + s * 1.215f;
                for(int e = -1; e <= 1; e += 2) m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbox(px + e * 0.65f, ry, deckZ + 0.6f, 0.3f, 0.9f, 0.6f);
                m.color(Pal.steel).style(metal, 0).at(px - 0.5f, ry, 2.5f).rot(1, 90).cyl(12, 0.45f, 0, 1.0f);
            }
        }
        //---- drive train: base plate, motor, coupling, gearbox (the pinion is live)
        m.color(Pal.mid).style(metal, matPlate).at(0, 0, 0).cbevel(-0.5f, pinY, deckZ, 3.6f, 2.0f, 0.4f, 0.08f);
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbox(-1.2f, pinY, deckZ + 0.4f, 1.5f, 1.2f, pinZ - 0.45f - deckZ - 0.4f);
        m.color(Pal.white).style(metal, matFins).at(-2.2f, pinY, pinZ).rot(1, 90).lathe(16, 0, 0, 0, 0.5f, 0, 0.62f, 0.2f, 0.62f, 1.8f, 0.5f, 2.0f, 0, 2.0f);
        m.color(Pal.dark).style(metal, 0).at(-0.2f, pinY, pinZ).rot(1, 90).tubeSide(10, 0.2f, 0, 0.22f);
        m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0).cbevel(0.5f, pinY, deckZ + 0.4f, 1.0f, 1.5f, pinZ + 0.5f - deckZ - 0.4f, 0.1f);
        m.color(Pal.dark).style(metal, 0).at(1.0f, pinY, pinZ).rot(1, 90).tubeSide(10, 0.2f, 0, pinX0 - 1.0f + 0.02f);
        m.color(accent).style(emissive, 0).at(0.5f, pinY - 0.45f, pinZ + 0.5f).cyl(8, 0.1f, 0, 0.06f);
        //---- feed hood (west): seal ring, access door, preheater on top
        m.color(Pal.white).style(metal, matPlate).at(0, 0, 0).bevel(feedX0, hoodY0, deckZ, feedX1, hoodY1, feedTop, 0.25f);
        m.color(Pal.dark).style(metal, 0).at(feedX1, ay, az).rot(1, 90).lathe(sides, 180f / sides, R + 0.08f, 0, R + 0.5f, 0, R + 0.5f, 0.15f, R + 0.08f, 0.15f);
        m.color(Pal.dark).style(0, 0).at(0, 0, 0).quad(feedX0 + 0.9f, hoodY0 - 0.01f, deckZ + 1.0f, feedX0 + 2.3f, hoodY0 - 0.01f, deckZ + 1.0f,
            feedX0 + 2.3f, hoodY0 - 0.01f, deckZ + 3.2f, feedX0 + 0.9f, hoodY0 - 0.01f, deckZ + 3.2f, 0, -1, 0);
        m.color(Pal.offWhite).style(metal, matPlate).at(preX, preY, 0).lathe(24, 0, 0, feedTop - 0.02f, 1.05f, feedTop - 0.02f, 1.05f, 8.8f, 0.8f, 9.15f, 0.35f, 9.3f, 0, 9.3f);
        m.color(accent).style(0, 0).at(preX, preY, 0).lathe(24, 0, 1.04f, 8.2f, 1.11f, 8.2f, 1.11f, 8.45f, 1.04f, 8.45f);
        //bucket elevator on the west face of the hood, spout into the preheater, boot inlet
        m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0).bevel(feedX0 - 1.0f, 0.8f, deckZ, feedX0, 2.6f, 9.3f, 0.12f);
        m.color(accent).style(0, 0).at(0, 0, 0).box(feedX0 - 0.85f, 0.78f, 8.0f, feedX0 - 0.15f, 0.8f, 8.3f);
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).bevel(feedX0 - 1.0f, 0.7f, 9.3f, feedX0, 2.7f, 9.8f, 0.1f);
        m.color(Pal.steel).style(metal, 0).pipe(10, 0.28f, feedX0 - 0.1f, preY, 9.0f, preX - 0.7f, preY, 8.9f);
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbevel(feedX0 - 0.5f, 0.1f, deckZ, 1.2f, 1.4f, 1.3f, 0.1f);
        m.color(Pal.graphite).style(0, 0).at(0, 0, 0).cbox(feedX0 - 0.5f, 0.1f, deckZ + 1.3f, 0.8f, 1.0f, 0.02f);
        //duct and stack with a dark bore
        m.color(Pal.offWhite).style(metal, 0).at(0, 0, 0).box(stX - 0.6f, stY, 5.0f, stX + 0.6f, hoodY0 + 0.05f, 6.2f);
        m.color(Pal.white).style(metal, matPlate).at(stX, stY, 0).lathe(20, 0, 0, deckZ, stR + 0.3f, deckZ, stR + 0.3f, deckZ + 0.35f, stR, deckZ + 0.7f,
            stR, stTop - 0.35f, stR + 0.12f, stTop - 0.35f, stR + 0.12f, stTop, stR - 0.18f, stTop);
        m.color(Pal.graphite).style(0, 0).at(stX, stY, 0).lathe(20, 0, stR - 0.18f, stTop - 0.01f, 0, stTop - 0.6f);
        m.color(accent).style(0, 0).at(stX, stY, 0).lathe(20, 0, stR - 0.01f, 8.6f, stR + 0.07f, 8.6f, stR + 0.07f, 8.9f, stR - 0.01f, 8.9f);
        //---- burner hood (east): seal ring, hot discharge opening, sight glass bezel, burner lance
        m.color(Pal.white).style(metal, matPlate).at(0, 0, 0).bevel(burnX0, hoodY0, deckZ, burnX1, hoodY1, burnTop, 0.25f);
        m.color(Pal.dark).style(metal, 0).at(burnX0, ay, az).rot(1, -90).lathe(sides, 180f / sides, R + 0.08f, 0, R + 0.5f, 0, R + 0.5f, 0.15f, R + 0.08f, 0.15f);
        m.color(0.42f, 0.10f, 0.04f).style(emissive, 0).at(0, 0, 0).quad(chX - 0.65f, hoodY0 - 0.01f, chZ0 - 0.05f, chX + 0.65f, hoodY0 - 0.01f, chZ0 - 0.05f,
            chX + 0.65f, hoodY0 - 0.01f, chZ0 + 0.95f, chX - 0.65f, hoodY0 - 0.01f, chZ0 + 0.95f, 0, -1, 0);
        m.color(Pal.dark).style(metal, 0).at(winX, hoodY0, winZ).rot(0, 90).lathe(16, 0, 0.55f, 0, 0.8f, 0, 0.8f, 0.14f, 0.55f, 0.14f);
        m.color(Pal.graphite).style(0, 0).at(winX, hoodY0 + 0.01f, winZ).rot(0, 90).cyl(16, 0.54f, 0, 0.02f);
        m.color(Pal.darkSteel).style(metal, 0).at(burnX1, ay, az).rot(1, 90).lathe(14, 0, 0, 0, 0.62f, 0, 0.62f, 0.22f, 0.42f, 0.22f, 0.42f, 0.95f, 0.3f, 0.95f, 0.3f, 1.0f, 0, 1.0f);
        //gas line along the east edge from the bottle skid up to the lance
        m.color(Pal.steel).style(metal, 0).pipe(10, 0.17f, 10.5f, -8.6f, 1.8f, 10.5f, 0.1f, 1.8f);
        m.pipe(10, 0.17f, 10.5f, 0.1f, 1.8f, 10.5f, 0.1f, az);
        m.pipe(10, 0.17f, 10.5f, 0.1f, az, 10.5f, ay - 0.35f, az);
        elbow(m, 10.5f, 0.1f, 1.8f, 0.19f);
        elbow(m, 10.5f, 0.1f, az, 0.19f);
        //gas bottle skid (south east)
        m.color(Pal.mid).style(metal, matPlate).at(0, 0, 0).cbevel(9.9f, -8.6f, deckZ, 2.6f, 2.0f, 0.3f, 0.08f);
        for(int i = 0; i < 2; i++){
            float bx = 9.3f + i * 1.2f;
            m.color(i == 0 ? Pal.yellow : Pal.cyan).style(metal, 0).at(bx, -8.6f, 0).lathe(14, 0, 0, deckZ + 0.3f, 0.48f, deckZ + 0.3f, 0.48f, 3.4f, 0.3f, 3.7f, 0.14f, 3.75f, 0.14f, 3.95f, 0, 3.95f);
        }
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).cbox(9.9f, -9.12f, 2.5f, 2.2f, 0.08f, 0.3f);
        m.color(Pal.steel).style(metal, 0).pipe(10, 0.12f, 9.3f, -8.6f, 3.9f, 10.5f, -8.6f, 3.9f);
        m.pipe(10, 0.12f, 10.5f, -8.6f, 3.9f, 10.5f, -8.6f, 1.8f);
        elbow(m, 10.5f, -8.6f, 3.9f, 0.14f);
        elbow(m, 10.5f, -8.6f, 1.8f, 0.19f);
        //---- chute: plate with side lips, tilted about x so local -y runs south and down
        m.color(Pal.mid).style(0, 0).at(chX, chY0, chZ0).rot(0, chA);
        m.box(-0.6f, -chL, -0.12f, 0.6f, 0f, 0f);
        m.box(-0.6f, -chL, 0f, -0.48f, 0f, 0.28f);
        m.box(0.48f, -chL, 0f, 0.6f, 0f, 0.28f);
        //---- product belt: rails, belt, legs, pulleys, loading skirt and discharge hood, pit with grate
        for(int s = -1; s <= 1; s += 2) m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).box(beltX0 - 0.15f, beltY + s * 0.95f - 0.07f, 1.75f, beltX1 + 0.15f, beltY + s * 0.95f + 0.07f, 2.3f);
        m.color(Pal.rubber).style(0, matRubber).at(0, 0, 0).box(beltX0, beltY - 0.84f, 2.05f, beltX1, beltY + 0.84f, beltTop);
        for(float lx : new float[]{-3.0f, 0.4f, 3.8f, 7.2f}) m.color(Pal.mid).style(metal, 0).at(0, 0, 0).cbox(lx, beltY, deckZ, 0.3f, 2.0f, 1.75f - deckZ);
        for(float px : new float[]{beltX0, beltX1}) m.color(Pal.steel).style(metal, 0).at(px, beltY + 0.9f, 2.0f).rot(0, 90).cyl(12, 0.28f, 0, 1.8f);
        m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0).bevel(8.55f, beltY - 1.1f, deckZ, 9.45f, beltY + 1.1f, 3.0f, 0.1f);
        m.color(accent).style(0, 0).at(0, 0, 0).box(8.65f, beltY - 1.12f, 2.55f, 9.35f, beltY - 1.1f, 2.8f);
        m.color(Pal.darkSteel).style(metal, 0).at(0, 0, 0).bevel(-6.8f, beltY - 1.25f, deckZ, -4.1f, beltY + 1.25f, deckZ + 0.14f, 0.04f);
        m.color(Pal.graphite).style(0, matGrate).at(0, 0, 0).box(-6.6f, beltY - 1.05f, deckZ + 0.14f, -4.2f, beltY + 1.05f, deckZ + 0.15f);
        m.color(Pal.offWhite).style(metal, matPlate).at(0, 0, 0).bevel(-5.4f, beltY - 1.1f, deckZ + 0.14f, -4.15f, beltY + 1.1f, 3.0f, 0.1f);
        m.color(accent).style(0, 0).at(0, 0, 0).box(-5.3f, beltY - 1.12f, 2.55f, -4.25f, beltY - 1.1f, 2.8f);
        //---- pallet of finished graphite blocks (south west)
        m.color(Pal.mid).style(metal, 0).at(0, 0, 0).cbevel(-8.6f, -6.3f, deckZ, 2.6f, 2.0f, 0.3f, 0.06f);
        for(int i = 0; i < 6; i++){
            float bx = -9.4f + (i % 3) * 0.8f, by = -6.75f + (i / 3) * 0.9f;
            m.color(Pal.graphite).style(metal, 0).at(0, 0, 0).cbevel(bx, by, deckZ + 0.3f, 0.7f, 0.8f, 0.7f, 0.08f);
        }
        for(int i = 0; i < 2; i++) m.color(Pal.graphite).style(metal, 0).at(0, 0, 0).cbevel(-9.0f + i * 0.8f, -6.3f, deckZ + 1.0f, 0.7f, 0.8f, 0.7f, 0.08f);
        //---- shell cooling fan housings (impellers are live)
        for(float fx : fanX){
            m.color(Pal.white).style(metal, matPlate).at(fx, fanY, 0).lathe(24, 0, 0.86f, deckZ, 1.05f, deckZ, 1.05f, fanZ, 0.86f, fanZ, 0.86f, deckZ);
            m.color(accent).style(0, 0).at(fx, fanY, 0).lathe(24, 0, 1.05f, fanZ - 0.3f, 1.11f, fanZ - 0.3f, 1.11f, fanZ - 0.1f, 1.05f, fanZ - 0.1f);
            m.color(Pal.graphite).style(0, 0).at(fx, fanY, 0).cyl(24, 0.87f, deckZ, deckZ + 0.25f);
            m.color(Pal.darkSteel).style(metal, 0).at(fx, fanY, 0).cyl(10, 0.24f, deckZ + 0.25f, fanZ + 0.02f);
            for(int i = 0; i < 3; i++){
                m.color(Pal.darkSteel).style(metal, 0).at(fx, fanY, deckZ + 0.9f).rot(2, 90f + i * 120f).box(0.2f, -0.05f, 0f, 0.86f, 0.05f, 0.1f, true);
            }
        }
        console(m, -8.0f, -9.3f);
        m.color(Pal.yellow).style(0, matHazard).at(0, 0, 0).box(-2.5f, -10.9f, deckZ, 2.5f, -10.2f, deckZ + 0.03f);
    }

    /** Soft contact shadow of the (live) drum on the deck and piers, baked into the static layers. */
    @Override
    public boolean pattern(int mat, float x, float y, float z, float nz, float[] rgb){
        if(nz < 0.5f || z > 2.7f || x < dx0 - 0.4f || x > dx1 + 0.4f) return false;
        float k = 1f - Mathf.clamp((Math.abs(y - ay - 0.35f) - 1.2f) / 1.7f);
        if(k <= 0f) return false;
        float mul = 1f - 0.24f * k * k;
        rgb[0] *= mul; rgb[1] *= mul; rgb[2] *= mul;
        return true;
    }

    @Override
    public void buildEnvelope(Mesh m){
        //drum with tyres and gear, including the parts inside the hoods
        m.at(dx0, ay, az).rot(1, 90).cyl(24, tyreR + 0.06f, 0, dx1 - dx0);
        m.at(gearX - 0.35f, ay, az).rot(1, 90).cyl(24, toothR + 0.05f, 0, 0.7f);
        //pinion
        m.at(pinX0 - 0.05f, pinY, pinZ).rot(1, 90).cyl(16, pinR + 0.25f, 0, pinX1 - pinX0 + 0.1f);
        //cleats and lumps on the belt, lumps on the chute
        m.at(0, 0, 0).box(beltX0 - 0.2f, beltY - 0.8f, beltTop - 0.02f, beltX1 + 0.2f, beltY + 0.8f, beltTop + 0.6f);
        m.box(chX - 0.45f, chY1 - 0.1f, chZ1 - 0.1f, chX + 0.45f, chY0 + 0.1f, chZ0 + 0.6f);
        //fan impellers
        for(float fx : fanX) m.at(fx, fanY, fanZ - 0.02f).cyl(16, 0.85f, 0, 0.5f);
        //sight glass disc
        m.at(0, 0, 0).box(winX - 0.52f, hoodY0 - 0.13f, winZ - 0.52f, winX + 0.52f, hoodY0 - 0.02f, winZ + 0.52f);
    }

    @Override
    public void buildRest(Mesh m){
        m.add(drum, 0, ay, az, 0, 0f);
        m.add(pinion, 0, pinY, pinZ, 0, pinPhase);
        for(float fx : fanX) m.add(fan, fx, fanY, fanZ, 2, 0f);
        for(int k = 0; k < cells; k++) m.add(cleat, beltX1 - k * pitch - 0.3f, beltY, beltTop, 2, 0f);
        for(int k = 1; k < cells; k++) m.add(k % 2 == 0 ? lumpA : lumpB, beltX1 - k * pitch + 0.45f, beltY, beltTop, 2, 0f);
        m.add(lumpA, chX, Mathf.lerp(chY0, chY1, 0.5f), Mathf.lerp(chZ0, chZ1, 0.5f) + 0.02f, 0, chA);
        m.color(1f, 0.55f, 0.2f).style(emissive, 0).at(winX, hoodY0 - 0.08f, winZ).rot(0, 90).cyl(16, 0.5f, 0, 0.03f);
    }

    @Override
    public void drawLive(Building b){
        float wx = b.x, wy = b.y, w = warmup(b);
        float ang = total(b) * drumDeg, rev = ang / 360f, th = ang * Mathf.degRad;
        //shell cooling fans (far north, low) first
        for(float fx : fanX){
            Live.pose(fx, fanY, fanZ, 2, ang * 4f);
            Live.draw(fan, cam, wx, wy);
        }
        //burner sight glass: flame flicker that is periodic in one revolution
        float flick = 0.72f + 0.28f * Mathf.sin(th * 3f) * Mathf.sin(th * 5f + 1f);
        Live.glowR = 0.30f + 0.70f * w * flick;
        Live.glowG = 0.08f + 0.50f * w * flick;
        Live.glowB = 0.03f + 0.10f * w;
        Live.pose(winX, hoodY0 - 0.08f, winZ, 2, 0f);
        Live.draw(glow, cam, wx, wy);
        Live.glowR = Live.glowG = Live.glowB = 1f;
        //kiln drum, then the counter-rotating pinion (24:8, phased so the teeth interleave at the mesh point)
        Live.pose(0, ay, az, 0, ang);
        Live.draw(drum, cam, wx, wy);
        Live.pose(0, pinY, pinZ, 0, pinPhase - ang * gearTeeth / (float)pinionTeeth);
        Live.draw(pinion, cam, wx, wy);
        //product belt: 6 pitches per drum revolution
        float adv = Mathf.mod(rev * pitch * 6f, beltL);
        for(int k = 0; k < cells; k++){
            Live.pose(beltX1 - Mathf.mod(k * pitch + adv, beltL), beltY, beltTop, 2, 0f);
            Live.draw(cleat, cam, wx, wy);
        }
        //lumps: cell pattern with period 3 (chute lumps first, they are further north)
        for(int pass = 0; pass < 2; pass++){
            boolean chute = pass == 0;
            for(int k = 0; k < cells; k++){
                int v = k % 3;
                if(v == 0) lump(k * pitch + 0.75f, lumpA, hotA, 0.12f, chute, w, adv, wx, wy);
                else if(v == 1){
                    lump(k * pitch + 0.40f, lumpB, hotB, -0.15f, chute, w, adv, wx, wy);
                    lump(k * pitch + 1.08f, lumpA, hotA, 0.05f, chute, w, adv, wx, wy);
                }else lump(k * pitch + 0.82f, lumpB, hotB, -0.05f, chute, w, adv, wx, wy);
            }
        }
    }

    /** One graphite lump at belt-path coordinate s: slides down the chute, lands on the belt and cools. */
    void lump(float s, Mesh cold, Mesh hot, float side, boolean chutePass, float w, float adv, float wx, float wy){
        float d = Mathf.mod(s + adv, beltL);
        float slide = beltX1 - chX;
        boolean onChute = d < slide;
        if(onChute != chutePass) return;
        float x, y, z, tilt, heat;
        if(onChute){
            float u = d / slide;
            x = chX + side;
            y = Mathf.lerp(chY0, chY1, u);
            z = Mathf.lerp(chZ0, chZ1, u) + 0.02f;
            tilt = chA;
            heat = 1f;
        }else{
            float land = Mathf.clamp((d - slide) / 0.5f);
            x = beltX1 - d;
            y = beltY;
            z = Mathf.lerp(chZ1, beltTop, land);
            tilt = chA * (1f - land);
            heat = 1f - Mathf.clamp((d - slide) / 3.5f);
        }
        Live.pose(x, y, z, 0, tilt);
        Live.draw(cold, cam, wx, wy);
        if(heat > 0.01f && w > 0.02f){
            Live.alpha = heat * heat * (0.35f + 0.65f * w);
            Live.glowR = 1f; Live.glowG = 0.55f + 0.3f * heat; Live.glowB = 0.2f;
            Live.draw(hot, cam, wx, wy);
            Live.alpha = 1f;
            Live.glowR = Live.glowG = Live.glowB = 1f;
        }
    }

    @Override
    public void drawOver(Building b){
        float w = warmup(b);
        if(w < 0.02f) return;
        float rev = total(b) * drumDeg / 360f;
        //steam puffs above the stack, two cycles per revolution, drawn lowest (furthest) first
        for(int k = 0; k < 4; k++){
            puffU[k] = Mathf.mod(rev * 2f + k * 0.25f, 1f);
            puffOrder[k] = k;
        }
        for(int i = 1; i < 4; i++){
            int v = puffOrder[i], j = i - 1;
            while(j >= 0 && puffU[puffOrder[j]] > puffU[v]){ puffOrder[j + 1] = puffOrder[j]; j--; }
            puffOrder[j + 1] = v;
        }
        for(int n = 0; n < 4; n++){
            int k = puffOrder[n];
            float u = puffU[k];
            float s = 1.1f + 1.6f * u;
            float z = stTop + 0.2f + u * 2.6f;
            float x = stX + Mathf.sin(u * 5f + k * 1.7f) * 0.3f * u, y = stY + 0.25f * u;
            Live.alpha = w * 0.9f * (1f - u) * Math.min(1f, u * 5f);
            Live.link(x, y, z - s / 2f, x, y, z + s / 2f, s, s);
            Live.draw(puff, cam, b.x, b.y);
        }
        Live.alpha = 1f;
    }
}
