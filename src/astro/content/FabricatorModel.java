package astro.content;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import astro.g3d.*;
import astro.g3d.Mesh;

import static astro.g3d.Mesh.*;

/**
 * The Astro Warp Gate (the Detainer "fabricator"): instead of printing a unit three times its own size inside a
 * 5x5 hall, the block is a summoning gate. A circular aperture sunk into a white deck, eight emitter nodes, four
 * emitter pylons on the corners and a levitating warp ring with a tilted gyro ring inside it. While the plan is
 * being charged the rings rise and spin up, light pours into the aperture and a holographic lock ring converges;
 * when the charge completes the gate flashes and the Detainer materialises above it (a translucent scan print
 * while the payload moves out, then the real unit arrives with a warp flash).
 *
 * <p>{@link #base()} is the static part (baked into the block sprite by the offline baker), {@link #live()} the
 * animated part drawn every frame with the shared 3D renderer. Model space: 1 unit = 1 world unit, origin at the
 * block centre, +y north, z up (the block camera of {@link Cam} with half = 20).
 */
public final class FabricatorModel{
    private FabricatorModel(){}

    public static final float HALF = 20f;
    /** model units -> world units: the deck is drawn a little smaller than the footprint so that its leaning north edge stays inside the 5x5 sprite */
    public static final float SCALE = 0.88f;
    /** pylon positions (|x| = |y|) and the height of their flat tops that carry the emitter crystals */
    public static final float PYL = 11.5f, PYL_TOP = 9.0f;
    /** rest heights of the ring centres and how far they rise at full charge */
    public static final float RING_Z = 5.8f, RING_LIFT = 4.0f, GYRO_Z = 7.2f, GYRO_LIFT = 4.5f;

    public static int ROOT, RING, GYRO;
    public static final int[] EMIT = new int[4];

    static final Color concrete = c("59616e"), white = c("ece6dc"), whiteDark = c("c9c0b3"), bay = c("1d2027"), grey = c("a6b2c1"),
        greyDark = c("6d7684"), amber = c("ffa42e"), gold = c("ffd04a"), hazard = c("f0c24a"), black = c("23262d"), ringC = c("3a3f49"),
        ringDark = c("262a32");

    static Color c(String hex){ return Color.valueOf(hex); }

    private static Rig liveRig;
    private static UnitRenderer renderer;

    private static Mesh mat(Mesh m, Color col){
        return m.at(0, 0, 0).color(col).style(metal, matPlain);
    }

    private static Mesh glow(Mesh m, Color col){
        return m.at(0, 0, 0).color(col).style(emissive, matPlain);
    }

    // ------------------------------------------------------------------------------------------------ static part

    /** Everything that never moves: baked into astro-fabricator.png. */
    public static Rig base(){
        Rig r = new Rig();
        r.half = HALF;
        r.height = 10f;
        int B = r.bone(-1, 0, 0, 0);

        //foundation slab
        mat(r.part(B), concrete).bevel(-20, -20, 0, 20, 20, 3.0f, 0.7f);
        int found = r.lastIndex();
        //aperture floor: a dark gloss plate (thick enough to sort cleanly above the slab) with amber guide ring, gold inner ring, hub and eight radial ticks
        mat(r.part(B), bay).cyl(24, 11.9f, 3.0f, 3.25f);
        int floor = r.lastIndex();
        r.on(found);
        glow(r.part(B), amber).lathe(24, 0f, 11.0f, 3.28f, 10.5f, 3.28f);
        r.on(floor);
        glow(r.part(B), gold).lathe(24, 0f, 6.6f, 3.28f, 6.2f, 3.28f);
        r.on(floor);
        glow(r.part(B), gold).lathe(24, 0f, 2.2f, 3.28f, 0f, 3.28f);
        r.on(floor);
        for(int k = 0; k < 8; k++){
            glow(r.part(B), amber).at(0, 0, 0).rot(2, 22.5f + k * 45f).cbox(8.6f, 0, 3.25f, 2.6f, 0.5f, 0.05f);
            r.on(floor);
        }

        //white deck with the 24-gon aperture: one piece made of 24 slices (top, inner wall, sloped outer wall each)
        {
            Mesh m = mat(r.part(B), white);
            float ri = 12f, ro = 18.3f, z0 = 3.0f, z1 = 4.6f, inset = 0.4f;
            float ov = 0.25f * Mathf.degRad; //slices overlap a hair so the offline rasteriser shows no seams
            for(int k = 0; k < 24; k++){
                float a0 = k * 15f * Mathf.degRad - ov, a1 = (k + 1) * 15f * Mathf.degRad + ov;
                float c0 = (float)Math.cos(a0), s0 = (float)Math.sin(a0), c1 = (float)Math.cos(a1), s1 = (float)Math.sin(a1);
                float px0 = ri * c0, py0 = ri * s0, px1 = ri * c1, py1 = ri * s1;
                float t0 = ro / Math.max(Math.abs(c0), Math.abs(s0)), t1 = ro / Math.max(Math.abs(c1), Math.abs(s1));
                float qx0 = t0 * c0, qy0 = t0 * s0, qx1 = t1 * c1, qy1 = t1 * s1;
                //square edge normal of this slice (both outer points lie on the same edge)
                float am = (a0 + a1) / 2f, cm = (float)Math.cos(am), sm = (float)Math.sin(am);
                float nx = Math.abs(cm) > Math.abs(sm) ? Math.signum(cm) : 0f, ny = Math.abs(cm) > Math.abs(sm) ? 0f : Math.signum(sm);
                float rx0 = qx0 - nx * inset, ry0 = qy0 - ny * inset, rx1 = qx1 - nx * inset, ry1 = qy1 - ny * inset;
                //top
                m.quad(px0, py0, z1, px1, py1, z1, rx1, ry1, z1, rx0, ry0, z1, 0, 0, 1);
                //inner wall (faces the centre)
                m.quad(px0, py0, z0, px1, py1, z0, px1, py1, z1, px0, py0, z1, -cm, -sm, 0);
                //outer wall (leans inward)
                m.quad(qx0, qy0, z0, qx1, qy1, z0, rx1, ry1, z1, rx0, ry0, z1, nx, ny, 0.25f);
            }
            m.at(0, 0, 0);
        }
        int deck = r.lastIndex();
        r.on(found);
        //rim lip around the aperture
        mat(r.part(B), whiteDark).ring(24, 12.0f, 13.0f, 4.6f, 5.0f);
        r.on(deck);
        //corner plates carrying the pylons
        int[] plates = new int[4];
        for(int k = 0; k < 4; k++){
            float sx = (k & 1) == 0 ? -1f : 1f, sy = (k & 2) == 0 ? -1f : 1f;
            mat(r.part(B), whiteDark).box(Math.min(sx * 9.6f, sx * 17.6f), Math.min(sy * 9.6f, sy * 17.6f), 4.6f, Math.max(sx * 9.6f, sx * 17.6f), Math.max(sy * 9.6f, sy * 17.6f), 4.9f);
            plates[k] = r.lastIndex();
            r.on(deck);
            //pylon: dark base, tapered grey column with a black band, flat top for the crystal (live part)
            mat(r.part(B), greyDark).cbevel(sx * PYL, sy * PYL, 4.9f, 4.6f, 4.6f, 1.2f, 0.35f);
            int pb = r.lastIndex();
            r.on(plates[k]);
            mat(r.part(B), grey).taper(sx * PYL, sy * PYL, 6.1f, 3.2f, 3.2f, PYL_TOP, 2.4f, 2.4f, 0, 0, false);
            int col = r.lastIndex();
            r.on(pb);
            mat(r.part(B), black).cbox(sx * PYL, sy * PYL, 7.3f, 3.0f, 3.0f, 0.3f);
            r.on(col);
        }
        //eight emitter nodes between the pylons
        for(int k = 0; k < 8; k++){
            float ang = 22.5f + k * 45f, rad = 15.2f;
            float x = Mathf.cosDeg(ang) * rad, y = Mathf.sinDeg(ang) * rad;
            mat(r.part(B), grey).at(x, y, 0).rot(2, ang).cbevel(0, 0, 4.6f, 2.0f, 2.8f, 1.4f, 0.35f);
            int node = r.lastIndex();
            r.on(deck);
            glow(r.part(B), amber).at(x, y, 0).rot(2, ang).cbox(0, 0, 6.0f, 1.0f, 1.6f, 0.16f);
            r.on(node);
        }
        //south console with a lit screen and two conduits running to the rim
        mat(r.part(B), grey).cbevel(0, -16.4f, 4.6f, 11f, 3.0f, 2.6f, 0.4f);
        int con = r.lastIndex();
        r.on(deck);
        glow(r.part(B), gold).cbox(0, -16.4f, 7.2f, 8.6f, 1.6f, 0.12f);
        r.on(con);
        mat(r.part(B), black).cbox(0, -17.9f, 5.2f, 9f, 0.12f, 1.2f);
        r.on(con);
        for(int sx = -1; sx <= 1; sx += 2){
            mat(r.part(B), greyDark).pipe(8, 0.5f, sx * 4f, -14.9f, 5.0f, sx * 4f, -12.5f, 5.0f);
            r.on(deck);
        }
        //east / west coolant units with black fins
        for(int sx = -1; sx <= 1; sx += 2){
            mat(r.part(B), whiteDark).cbevel(sx * 16.6f, 0, 4.6f, 2.8f, 9.0f, 2.2f, 0.35f);
            int unit = r.lastIndex();
            r.on(deck);
            for(int k = 0; k < 3; k++){
                mat(r.part(B), black).cbox(sx * 16.6f, -2.6f + k * 2.6f, 6.8f, 2.0f, 0.5f, 0.5f);
                r.on(unit);
            }
        }
        //hazard stripes along the north edge
        for(int k = 0; k < 10; k++){
            mat(r.part(B), k % 2 == 0 ? hazard : black).cbox(-9.9f + k * 2.2f, 16.6f, 4.6f, 2.2f, 1.4f, 0.22f);
            r.on(deck);
        }
        r.finish();
        return r;
    }

    // ------------------------------------------------------------------------------------------------ live part

    /** The animated part: warp ring, gyro ring and the four emitter crystals. */
    public static Rig live(){
        Rig r = new Rig();
        r.half = HALF;
        r.height = 16f;
        ROOT = r.bone(-1, 0, 0, 0);
        RING = r.bone(ROOT, 0, 0, RING_Z);
        GYRO = r.bone(ROOT, 0, 0, GYRO_Z);

        //warp ring: chamfered annulus, eight amber arcs on top (gaps show the rotation), four clamp nodes
        float ri = 11.0f, ro = 12.8f, h = 0.6f, b = 0.3f;
        mat(r.part(RING), ringC).lathe(24, 7.5f,
            ri, -h + b, ri + b, -h, ro - b, -h, ro, -h + b, ro, h - b, ro - b, h, ri + b, h, ri, h - b, ri, -h + b);
        int ring = r.lastIndex();
        r.shadowRound(0);
        arcs(r.part(RING), 24, 7.5f, 8, 2, ri + 0.5f, ro - 0.5f, h + 0.01f);
        r.on(ring);
        for(int k = 0; k < 4; k++){
            float ang = 7.5f + k * 90f;
            mat(r.part(RING), ringDark).at(0, 0, 0).rot(2, ang).cbox((ri + ro) / 2f, 0, h, 1.4f, 2.2f, 0.5f);
            int node = r.lastIndex();
            r.on(ring);
            glow(r.part(RING), gold).at(0, 0, 0).rot(2, ang).cbox((ri + ro) / 2f, 0, h + 0.5f, 0.8f, 1.4f, 0.14f);
            r.on(node);
        }

        //gyro ring: thinner, tilted by the animation
        float gi = 7.6f, go = 8.8f, gh = 0.4f, gb = 0.2f;
        mat(r.part(GYRO), ringC).lathe(20, 9f,
            gi, -gh + gb, gi + gb, -gh, go - gb, -gh, go, -gh + gb, go, gh - gb, go - gb, gh, gi + gb, gh, gi, gh - gb, gi, -gh + gb);
        int gyro = r.lastIndex();
        arcs(r.part(GYRO), 20, 9f, 4, 3, gi + 0.3f, go - 0.3f, gh + 0.01f);
        r.on(gyro);
        for(int k = 0; k < 2; k++){
            mat(r.part(GYRO), ringDark).at(0, 0, 0).rot(2, 9f + k * 180f).cbox((gi + go) / 2f, 0, gh, 1.2f, 1.6f, 0.4f);
            r.on(gyro);
        }

        //emitter crystals on the pylon tops
        for(int k = 0; k < 4; k++){
            float sx = (k & 1) == 0 ? -1f : 1f, sy = (k & 2) == 0 ? -1f : 1f;
            EMIT[k] = r.bone(ROOT, sx * PYL, sy * PYL, PYL_TOP);
            mat(r.part(EMIT[k]), greyDark).cbox(0, 0, 0, 2.2f, 2.2f, 0.3f);
            int cap = r.lastIndex();
            glow(r.part(EMIT[k]), amber).taper(0, 0, 0.3f, 1.6f, 1.6f, 1.9f, 0.25f, 0.25f, 0, 0, false);
            r.on(cap);
        }
        r.finish();
        return r;
    }

    /** n emissive arcs of {@code segs} lathe segments each, evenly spaced on a ring of {@code sides} segments (flat, normal +z). */
    private static void arcs(Mesh m, int sides, float angleOffset, int n, int segs, float r0, float r1, float z){
        glow(m, amber);
        int per = sides / n;
        for(int a = 0; a < n; a++){
            for(int s = 0; s < segs; s++){
                int i = a * per + s;
                float a0 = (angleOffset + i * 360f / sides) * Mathf.degRad, a1 = (angleOffset + (i + 1) * 360f / sides) * Mathf.degRad;
                float c0 = (float)Math.cos(a0), s0 = (float)Math.sin(a0), c1 = (float)Math.cos(a1), s1 = (float)Math.sin(a1);
                m.quad(r0 * c0, r0 * s0, z, r1 * c0, r1 * s0, z, r1 * c1, r1 * s1, z, r0 * c1, r0 * s1, z, 0, 0, 1);
            }
        }
    }

    /** lazily built shared live rig / renderer (also used by the offline preview) */
    public static Rig liveRig(){
        if(liveRig == null) liveRig = live();
        return liveRig;
    }

    public static UnitRenderer renderer(){
        if(renderer == null) renderer = new UnitRenderer(HALF);
        return renderer;
    }

    /** Projected screen offset (world units, relative to the block centre) of a model point at height z. */
    public static float sx(float x, float z){ return renderer().cam.sx(x * SCALE, z * SCALE); }
    public static float sy(float y, float z){ return renderer().cam.sy(y * SCALE, z * SCALE); }
    /** perspective scale of a model height */
    public static float sc(float z){ return renderer().cam.scale(z * SCALE) * SCALE; }

    /**
     * Draws the live gate at world position (x,y).
     * @param charge 0..1 build progress (rings rise and spin up with it)
     * @param lift   smoothed 0..1 height of the rings
     * @param spin   ring angle in degrees
     * @param flash  1 right after the gate fired, decays to 0
     * @param active 0..1 whether the plan is being worked on right now (light streams)
     * @param z      draw layer for the 3D parts
     */
    public static void draw(float x, float y, Color team, float charge, float lift, float spin, float flash, float active, float time, float z){
        Rig r = liveRig();
        UnitRenderer ur = renderer();
        r.reset();
        ur.reset();
        ur.setTeam(team);
        float ch = Mathf.clamp(charge);
        //warp ring: rises, spins, wobbles a little while charged
        r.move(RING, 0, 0, lift * RING_LIFT);
        r.rot(RING, 2, spin);
        r.rot(RING, 0, Mathf.sin(time, 40f, 2.2f) * lift);
        r.rot(RING, 1, Mathf.cos(time, 53f, 2.2f) * lift);
        r.glow[RING] = 0.45f + 0.9f * ch + 1.2f * flash;
        //gyro ring: precesses the other way, tilts more as the charge grows
        r.move(GYRO, 0, 0, lift * GYRO_LIFT);
        r.rot(GYRO, 2, -spin * 1.6f);
        r.rot(GYRO, 0, 18f + 10f * lift);
        r.glow[GYRO] = 0.45f + 1.0f * ch + 1.2f * flash;
        //crystals: slow turn, glow with the charge
        for(int k = 0; k < 4; k++){
            r.rot(EMIT[k], 2, spin * 0.5f + k * 90f);
            r.glow[EMIT[k]] = 0.5f + 1.1f * ch + 1.4f * flash;
        }
        ur.pose(r, 90f, 0, 0, 0, SCALE);
        Draw.z(z);
        Draw.color();
        ur.draw(r, x, y);

        //additive light effects
        if(ch > 0.02f || flash > 0.02f || active > 0.02f){
            Draw.z(z + 0.02f);
            Draw.blend(Blending.additive);
            float ringZ = RING_Z + lift * RING_LIFT;
            //light pillar from the aperture floor up through the rings
            if(ch > 0.02f){
                float top = 10f + 22f * ch;
                for(int k = 0; k < 3; k++){
                    float w = (9f - k * 3f) * (0.35f + 0.65f * ch), a = (0.10f + 0.07f * k) * ch + 0.25f * flash;
                    float c0 = Color.toFloatBits(1f, 0.72f, 0.3f, a), c1 = Color.toFloatBits(1f, 0.9f, 0.6f, 0f);
                    Fill.quad(
                        x + sx(-w, 3f), y + sy(0, 3f), c0,
                        x + sx(w, 3f), y + sy(0, 3f), c0,
                        x + sx(w, top), y + sy(0, top), c1,
                        x + sx(-w, top), y + sy(0, top), c1);
                }
            }
            //progress lamps on the eight emitter nodes: one more lights up for every eighth of the charge
            for(int k = 0; k < 8; k++){
                float lit = Mathf.clamp((ch - k / 8f) * 8f);
                if(lit <= 0.02f && flash <= 0.02f) continue;
                float ang = k * 45f + 22.5f;
                float px = x + sx(Mathf.cosDeg(ang) * 15.2f, 6.2f), py = y + sy(Mathf.sinDeg(ang) * 15.2f, 6.2f);
                Draw.color(AstroFx.amber, Color.white, 0.4f * lit);
                Draw.alpha(Math.max(lit * (0.35f + 0.25f * Mathf.absin(time + k * 9f, 7f, 1f)), 0.6f * flash));
                Fill.circle(px, py, (0.9f + 0.5f * lit + 1.4f * flash) * SCALE);
            }
            //tethers: the pylon crystals feed the warp ring while a plan is being built
            float st = Mathf.clamp(active) * ch;
            if(st > 0.02f){
                Lines.stroke(0.8f * SCALE);
                for(int k = 0; k < 4; k++){
                    float a = 45f + k * 90f;
                    float cxp = x + sx(Mathf.cosDeg(a) * PYL * 1.4142f, 9.6f), cyp = y + sy(Mathf.sinDeg(a) * PYL * 1.4142f, 9.6f);
                    float ra = a + 18f * Mathf.sin(time + k * 31f, 9f, 1f);
                    float rxp = x + sx(Mathf.cosDeg(ra) * 12.2f, ringZ + 0.9f), ryp = y + sy(Mathf.sinDeg(ra) * 12.2f, ringZ + 0.9f);
                    Draw.color(AstroFx.gold, Color.white, 0.3f + 0.3f * Mathf.absin(time + k * 7f, 3f, 1f));
                    Draw.alpha(st * (0.35f + 0.4f * Mathf.absin(time * 1.7f + k * 13f, 4f, 1f)));
                    Lines.line(cxp, cyp, rxp, ryp);
                }
            }
            //holographic lock ring above the warp ring: converges and brightens with the charge
            if(ch > 0.02f){
                float hz = ringZ + 5f, rad = 9.5f - 5.5f * ch, s = sc(hz);
                float cx = x, cy = y + sy(0, hz);
                Draw.color(AstroFx.gold, Color.white, 0.3f * ch);
                Draw.alpha((0.25f + 0.55f * ch) * (0.7f + 0.3f * Mathf.absin(time, 5f, 1f)));
                Lines.stroke(1.2f + 0.8f * ch);
                Lines.circle(cx, cy, rad * s);
                for(int k = 0; k < 3; k++){
                    float a = -spin * 1.3f + k * 120f;
                    Lines.lineAngle(cx + Mathf.cosDeg(a) * rad * s, cy + Mathf.sinDeg(a) * rad * s, a, 3.2f * s * (0.4f + 0.6f * ch));
                }
            }
            //gate flash
            if(flash > 0.02f){
                Draw.color(Color.white, AstroFx.gold, 1f - flash);
                Draw.alpha(flash * 0.8f);
                Fill.circle(x, y + sy(0, ringZ), (6f + 22f * (1f - flash)) * SCALE);
            }
            Draw.blend();
            Draw.color();
        }
    }
}
