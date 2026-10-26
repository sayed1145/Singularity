package blackhole.g3d;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.world.blocks.production.GenericCrafter.*;
import mindustry.world.blocks.units.UnitFactory.*;

/** {@link BlockModel} with the kit's faction style and the helpers shared by every animated machine. */
public abstract class KitModel extends BlockModel{
    public final Kit.Style s;
    protected final float deckZ;
    protected final float[] p2 = new float[2];

    protected KitModel(String name, int size, Kit.Style s){
        super(name, size);
        this.s = s;
        this.deckZ = 1.0f + size * 0.12f;
    }

    // ------------------------------------------------------------------ building state

    public static float warm(Building b){
        return b == null ? 0f : Mathf.clamp(b.warmup());
    }

    public static float prog(Building b){
        if(b instanceof GenericCrafterBuild g) return g.progress;
        if(b instanceof UnitFactoryBuild u) return u.fraction();
        return 0f;
    }

    public static float total(Building b){
        return b == null ? Time.time : b.totalProgress();
    }

    public static float time(Building b){
        return Time.time + (b == null ? 0 : b.id * 37f);
    }

    public static float smooth(float a, float b, float t){
        float u = Mathf.clamp((t - a) / (b - a));
        return u * u * (3f - 2f * u);
    }

    // ------------------------------------------------------------------ live drawing

    protected void lit(float g){
        Live.glowR = Live.glowG = Live.glowB = g;
    }

    protected void draw(Mesh m, Building b, float x, float y, float z, int axis, float deg){
        Live.pose(x, y, z, axis, deg);
        Live.draw(m, cam, b.x, b.y);
    }

    /**
     * Draws n copies of m orbiting (cx,cy,cz) at radius r, starting at angle a0 (degrees). Pieces are yawed to face
     * along the orbit. back=true draws only the far half (call before the centre piece), false the near half.
     */
    protected void orbit(Mesh m, Building b, int n, float cx, float cy, float cz, float r, float a0, boolean back){
        for(int i = 0; i < n; i++){
            float a = a0 + i * 360f / n;
            boolean far = Mathf.sinDeg(a) > 0f;
            if(far != back) continue;
            draw(m, b, cx + Mathf.cosDeg(a) * r, cy + Mathf.sinDeg(a) * r, cz, 2, a - 90f);
        }
    }

    /** Adds the orbit's rest pose to a mesh (icons / baker). */
    protected static void orbitRest(Mesh out, Mesh m, int n, float cx, float cy, float cz, float r, float a0){
        for(int i = 0; i < n; i++){
            float a = a0 + i * 360f / n;
            out.add(m, cx + Mathf.cosDeg(a) * r, cy + Mathf.sinDeg(a) * r, cz, 2, a - 90f);
        }
    }

    /** Screen position of a block-space point. */
    protected float sx(Building b, float x, float z){ return b.x + cam.sx(x, z); }
    protected float sy(Building b, float y, float z){ return b.y + cam.sy(y, z); }

    /** Additive light blob at a block-space point. */
    protected void light(Building b, float x, float y, float z, float rad, Color c, float a){
        if(a <= 0.01f) return;
        Draw.blend(Blending.additive);
        Draw.color(c, Mathf.clamp(a));
        Fill.circle(sx(b, x, z), sy(b, y, z), rad);
        Draw.blend();
        Draw.color();
    }

    /** Additive beam between two block-space points. */
    protected void beam(Building b, float x0, float y0, float z0, float x1, float y1, float z1, float w, Color c, float a){
        if(a <= 0.01f) return;
        Draw.blend(Blending.additive);
        Draw.color(c, Mathf.clamp(a));
        Lines.stroke(w);
        Lines.line(sx(b, x0, z0), sy(b, y0, z0), sx(b, x1, z1), sy(b, y1, z1));
        Draw.color(Color.white, Mathf.clamp(a) * 0.7f);
        Lines.stroke(w * 0.35f);
        Lines.line(sx(b, x0, z0), sy(b, y0, z0), sx(b, x1, z1), sy(b, y1, z1));
        Draw.blend();
        Draw.color();
    }

    // ------------------------------------------------------------------ live mesh factories

    /** Hexagonal bipyramid (crystal) centred on the origin. */
    public static Mesh gem(Color c, boolean emissive, float r, float h){
        Mesh m = new Mesh();
        m.color(c).style(emissive ? Mesh.emissive : (Mesh.glass | Mesh.metal), emissive ? Mesh.matPlain : Mesh.matGlass);
        m.lathe(6, 30f, 0.001f, -h * 0.6f, r, 0f, r * 0.8f, h * 0.25f, 0.001f, h);
        return m;
    }

    /** Cylinder along local x with alternating light / dark faces so its spin is visible. */
    public static Mesh roller(Color a, Color b, float r, float len, int sides){
        Mesh m = new Mesh();
        m.style(Mesh.metal, Mesh.matPlain);
        float h = len / 2f;
        for(int i = 0; i < sides; i++){
            float a0 = i * 360f / sides, a1 = (i + 1) * 360f / sides;
            float y0 = Mathf.cosDeg(a0) * r, z0 = Mathf.sinDeg(a0) * r, y1 = Mathf.cosDeg(a1) * r, z1 = Mathf.sinDeg(a1) * r;
            m.color((i & 1) == 0 ? a : b);
            float my = Mathf.cosDeg((a0 + a1) / 2f), mz = Mathf.sinDeg((a0 + a1) / 2f);
            m.quad(-h, y0, z0, h, y0, z0, h, y1, z1, -h, y1, z1, 0, my, mz);
        }
        m.color(b);
        for(int sd = -1; sd <= 1; sd += 2){
            int[] v = new int[sides];
            for(int i = 0; i < sides; i++){
                float an = i * 360f / sides;
                v[i] = m.vert(sd * h, Mathf.cosDeg(an) * r, Mathf.sinDeg(an) * r);
            }
            int c = m.vert(sd * h, 0, 0);
            for(int i = 0; i < sides; i++) m.face(c, v[i], v[(i + 1) % sides], v[(i + 1) % sides], sd, 0, 0);
        }
        return m;
    }

    /** Unit box (x,y in +-0.5, z 0..1) for {@link Live#link} rods. */
    public static Mesh rod(Color c, int flags){
        Mesh m = new Mesh();
        m.color(c).style(flags, Mesh.matPlain).box(-0.5f, -0.5f, 0f, 0.5f, 0.5f, 1f, true);
        return m;
    }

    /** Draws a rod mesh between two block-space points. */
    protected void link(Mesh rod, Building b, float x0, float y0, float z0, float x1, float y1, float z1, float w){
        Live.link(x0, y0, z0, x1, y1, z1, w, w);
        Live.draw(rod, cam, b.x, b.y);
    }
}
