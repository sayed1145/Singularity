package blackhole.models;

import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import blackhole.g3d.*;
import blackhole.g3d.Mesh;

import static blackhole.g3d.Mesh.*;

/**
 * Procedural 3D models of every item and liquid of the mod (v7.2). The offline baker (tools/bake) renders them
 * with the same camera, light and materials as the blocks into the 32x32 icons in assets/sprites/items and
 * assets/sprites/liquids; nothing here runs in game.
 *
 * <p>Each model is a small closed mesh centred on the origin, standing on z = 0 and fitting in a 6.4 x 6.4 unit
 * footprint (the icon shows 8 x 8 units). Colours are the content's own {@code Item.color} / {@code Liquid.color}.
 */
public final class ItemModels{
    private ItemModels(){}

    /** One icon: sprite folder ("items" / "liquids"), name, model and a tilt that presents it to the camera. */
    public static final class IconModel{
        public final String folder, name;
        public final Mesh mesh;
        public final Color color;
        /** rotation about x applied before rendering (degrees; negative = top leans away from the camera) */
        public final float tilt;
        /** spin about z (degrees) for a 3/4 view */
        public final float spin;

        IconModel(String folder, String name, Color color, float tilt, float spin, Mesh mesh){
            this.folder = folder; this.name = name; this.color = color; this.tilt = tilt; this.spin = spin; this.mesh = mesh;
        }
    }

    public static Seq<IconModel> all(){
        Seq<IconModel> out = new Seq<>();
        out.add(new IconModel("items", "degenerate-matter", Color.valueOf("cfa5ff"), -22f, 28f, degenerateMatter()));
        out.add(new IconModel("items", "hawking-dust", Color.valueOf("8ad8ff"), -26f, 15f, hawkingDust()));
        out.add(new IconModel("items", "singularity-core", Color.valueOf("ffb14e"), -24f, 0f, singularityCore()));
        out.add(new IconModel("items", "lumenite", Color.valueOf("91e8ff"), -24f, 20f, lumenite()));
        out.add(new IconModel("items", "aurite", Color.valueOf("d9b36a"), -24f, 30f, aurite()));
        out.add(new IconModel("items", "silt", Color.valueOf("9c93ab"), -26f, 10f, silt()));
        out.add(new IconModel("items", "prism-glass", Color.valueOf("bfe6ff"), -22f, 32f, prismGlass()));
        out.add(new IconModel("items", "resonance-shard", Color.valueOf("b69cff"), -24f, 24f, resonanceShard()));
        out.add(new IconModel("items", "prism-alloy", Color.valueOf("e8f5ff"), -24f, 30f, prismAlloy()));
        out.add(new IconModel("items", "concord-core", Color.valueOf("ffd875"), -24f, 0f, concordCore()));
        out.add(new IconModel("items", "aegis-alloy", Color.valueOf("8fd9bf"), -24f, 30f, aegisAlloy()));
        out.add(new IconModel("items", "graphite-brick", Color.valueOf("4a4f58"), -24f, 18f, graphiteBrick()));
        out.add(new IconModel("items", "signal-core", Color.valueOf("c5a0fc"), -24f, 24f, signalCore()));
        out.add(new IconModel("items", "industrial-waste", Color.valueOf("6b6a74"), -24f, 16f, industrialWaste()));
        out.add(new IconModel("items", "metal-swarf", Color.valueOf("a9b0bd"), -24f, 26f, metalSwarf()));
        out.add(new IconModel("items", "slag-dust", Color.valueOf("8f8498"), -26f, 12f, slagDust()));
        out.add(new IconModel("liquids", "lumen-slurry", Color.valueOf("7fd9e8"), -18f, 0f, liquid(Color.valueOf("7fd9e8"), Color.valueOf("cdf6ff"), false)));
        out.add(new IconModel("liquids", "tidewater", Color.valueOf("3f7fc0"), -18f, 0f, liquid(Color.valueOf("3f7fc0"), Color.valueOf("8fd0ff"), false)));
        out.add(new IconModel("liquids", "lumen-plasma", Color.valueOf("7ff0d8"), -18f, 0f, liquid(Color.valueOf("7ff0d8"), Color.valueOf("e6fff8"), true)));
        return out;
    }

    // ------------------------------------------------------------------ helpers

    private static Color shade(Color c, float k){
        return new Color(Mathf.clamp(c.r * k), Mathf.clamp(c.g * k), Mathf.clamp(c.b * k), 1f);
    }

    private static Color mix(Color a, Color b, float t){
        return new Color(a).lerp(b, t);
    }

    /** gold / alloy ingot: trapezoid bar with a stamped groove */
    private static Mesh ingot(Color c, float w, float d, float h, boolean groove){
        Mesh m = new Mesh();
        m.color(c).style(metal, matPlain).taper(0, 0, 0f, w, d, h, w * 0.78f, d * 0.72f, 0, 0, true);
        if(groove){
            m.color(shade(c, 0.55f)).style(metal, matPlain).box(-w * 0.26f, -d * 0.09f, h, w * 0.26f, d * 0.09f, h + 0.06f, true);
        }
        return m;
    }

    // ------------------------------------------------------------------ items

    /** Degenerate matter: a crushed, ultra-dense slab - dark violet block whose fractures glow. */
    static Mesh degenerateMatter(){
        Mesh m = new Mesh();
        Color c = Color.valueOf("cfa5ff"), dark = Color.valueOf("4a3a66");
        m.color(dark).style(metal, matPlain).bevel(-2.2f, -1.7f, 0f, 2.2f, 1.7f, 1.5f, 0.35f);
        //glowing fracture lines across the top
        m.color(c).style(emissive, matPlain);
        m.box(-1.6f, -0.08f, 1.5f, 1.6f, 0.08f, 1.56f, true);
        m.box(-0.08f, -1.2f, 1.5f, 0.08f, 1.2f, 1.56f, true);
        m.box(0.6f, -1.2f, 1.5f, 1.5f, -1.06f, 1.56f, true);
        m.box(-1.5f, 0.75f, 1.5f, -0.5f, 0.9f, 1.56f, true);
        //a lit seam round the waist
        m.color(mix(c, Color.white, 0.2f)).style(emissive, matPlain);
        m.box(-2.26f, -1.76f, 0.62f, 2.26f, 1.76f, 0.74f, true);
        return m;
    }

    /** Hawking dust: a heap of luminous blue grains - a low dark mound studded with glowing crystals. */
    static Mesh hawkingDust(){
        Mesh m = new Mesh();
        Color c = Color.valueOf("8ad8ff"), base = Color.valueOf("22384f");
        m.color(base).style(0, matConcrete).lathe(14, 0, 0.001f, 0f, 2.8f, 0f, 2.3f, 0.35f, 1.1f, 0.6f, 0.001f, 0.7f);
        float[][] grains = {{0.2f, 0.1f, 0.55f, 0.75f}, {-1.3f, 0.5f, 0.45f, 0.6f}, {1.35f, -0.4f, 0.42f, 0.58f}, {-0.5f, -1.3f, 0.35f, 0.52f},
            {0.9f, 1.2f, 0.4f, 0.5f}, {-1.7f, -0.7f, 0.25f, 0.42f}, {1.9f, 0.7f, 0.2f, 0.4f}, {0.0f, -2.0f, 0.15f, 0.4f}, {-0.3f, 1.8f, 0.2f, 0.38f}};
        for(float[] g : grains){
            m.color(mix(c, Color.white, 0.3f)).style(emissive, matPlain);
            m.at(g[0], g[1], g[2]).rot(2, g[0] * 90f).rot(0, g[1] * 25f);
            m.lathe(5, 0, 0.001f, -g[3] * 0.8f, g[3], 0f, 0.001f, g[3] * 1.2f);
        }
        m.at(0, 0, 0);
        return m;
    }

    /** Singularity core: a tiny contained black hole - black sphere with an amber accretion ring in a cradle. */
    static Mesh singularityCore(){
        Mesh m = new Mesh();
        Color amber = Color.valueOf("ffb14e"), frame = Color.valueOf("596170");
        //cradle: three claws
        for(int i = 0; i < 3; i++){
            float a = 90f + i * 120f;
            m.color(frame).style(metal, matPlain).at(0, 0, 0).rot(2, a).taper(1.6f, 0f, 0f, 0.7f, 0.6f, 0.9f, 0.4f, 0.4f, -0.5f, 0f, true);
        }
        m.at(0, 0, 0);
        m.color(Color.valueOf("25272e")).style(metal, matPlain).cyl(12, 1.0f, 0f, 0.3f);
        //black sphere (closed lathe) above the cradle
        m.color(Color.valueOf("07060a")).style(0, matPlain).at(0, 0, 1.75f);
        m.lathe(12, 15f, 0.001f, -0.95f, 0.5f, -0.8f, 0.85f, -0.45f, 0.95f, 0f, 0.85f, 0.45f, 0.5f, 0.8f, 0.001f, 0.95f);
        //accretion ring: tilted emissive torus
        m.at(0, 0, 1.75f).rot(0, 62f).rot(2, 20f);
        m.color(amber).style(emissive, matPlain).ring(16, 1.25f, 1.6f, -0.08f, 0.08f);
        m.color(mix(amber, Color.white, 0.5f)).style(emissive, matPlain).ring(16, 1.6f, 1.72f, -0.03f, 0.03f);
        m.at(0, 0, 0);
        return m;
    }

    /** Lumenite: a cluster of three cyan crystals. */
    static Mesh lumenite(){
        Mesh m = new Mesh();
        Color c = Color.valueOf("91e8ff");
        m.color(Color.valueOf("2f3a4e")).style(0, matConcrete).lathe(8, 22.5f, 0.001f, 0f, 2.0f, 0f, 1.5f, 0.45f, 0.001f, 0.55f);
        float[][] cr = {{0f, 0.1f, 0.75f, 3.0f, -8f, 0f}, {-1.0f, -0.6f, 0.5f, 2.0f, 24f, 200f}, {1.0f, -0.4f, 0.45f, 1.7f, 26f, 320f}, {0.2f, 1.1f, 0.4f, 1.4f, 22f, 80f}};
        for(float[] k : cr){
            m.color(c).style(glass | metal, matGlass).at(k[0], k[1], 0.3f).rot(2, k[5]).rot(0, k[4]);
            m.lathe(6, 30f, 0.001f, -0.3f, k[2], 0f, k[2] * 0.85f, k[3] * 0.65f, 0.001f, k[3]);
        }
        m.at(0, 0, 0);
        return m;
    }

    /** Aurite: a gold ingot with a stamped groove. */
    static Mesh aurite(){
        return ingot(Color.valueOf("d9b36a"), 4.4f, 2.4f, 1.3f, true);
    }

    /** Silt: a soft mound of fine sediment with a few pebbles. */
    static Mesh silt(){
        Mesh m = new Mesh();
        Color c = Color.valueOf("9c93ab");
        m.color(c).style(0, matConcrete).lathe(16, 0, 0.001f, 0f, 2.7f, 0f, 2.3f, 0.45f, 1.5f, 0.95f, 0.6f, 1.25f, 0.001f, 1.35f);
        float[][] p = {{1.3f, 0.7f, 0.65f, 0.34f}, {-1.2f, 0.9f, 0.6f, 0.3f}, {-0.3f, -1.5f, 0.5f, 0.3f}, {0.6f, 1.6f, 0.4f, 0.26f}};
        for(float[] q : p){
            m.color(shade(c, 0.7f)).style(0, matPlain).at(q[0], q[1], q[2]).lathe(6, 0, 0.001f, -q[3], q[3], 0f, 0.001f, q[3]);
        }
        m.at(0, 0, 0);
        return m;
    }

    /** Graphite brick: two stacked fired bricks, the lower one chipped at a corner. */
    static Mesh graphiteBrick(){
        Mesh m = new Mesh();
        Color c = Color.valueOf("4a4f58");
        //lower course
        m.color(c).style(metal, matConcrete).box(-2.6f, -1.5f, 0f, 2.6f, 1.5f, 1.1f, true);
        //upper course, turned and a little smaller: reads as masonry, not as a slab
        m.color(shade(c, 1.22f)).style(metal, matConcrete).at(0, 0, 1.1f).rot(2, 18f)
            .box(-2.1f, -1.2f, 0f, 2.1f, 1.2f, 0.95f, true);
        m.at(0, 0, 0).rot(2, 0f);
        //binder seam
        m.color(Color.valueOf("9c93ab")).style(0, matPlain).box(-2.55f, -1.45f, 1.08f, 2.55f, 1.45f, 1.14f);
        return m;
    }

    /** Prism glass: a clear triangular prism on a small sill. */
    static Mesh prismGlass(){
        Mesh m = new Mesh();
        Color c = Color.valueOf("bfe6ff");
        m.color(Color.valueOf("3b4654")).style(metal, matPlain).box(-2.3f, -1.2f, 0f, 2.3f, 1.2f, 0.25f, true);
        //triangular prism along x: hexa with a degenerate top edge
        m.color(c).style(glass | metal, matGlass).hexa(true,
            -2.1f, -1.0f, 0.25f, 2.1f, -1.0f, 0.25f, 2.1f, 1.0f, 0.25f, -2.1f, 1.0f, 0.25f,
            -2.1f, -0.02f, 2.2f, 2.1f, -0.02f, 2.2f, 2.1f, 0.02f, 2.2f, -2.1f, 0.02f, 2.2f);
        return m;
    }

    /** Resonance shard: a long violet shard that hums - sharp bipyramid with an emissive core line. */
    static Mesh resonanceShard(){
        Mesh m = new Mesh();
        Color c = Color.valueOf("b69cff");
        m.color(c).style(glass | metal, matGlass).at(0, 0, 0.9f).rot(0, 90f).rot(2, 0f);
        m.lathe(6, 30f, 0.001f, -3.1f, 0.55f, -1.2f, 0.85f, 0f, 0.6f, 1.4f, 0.001f, 3.1f);
        m.at(0, 0, 0);
        m.color(mix(c, Color.white, 0.4f)).style(emissive, matPlain).at(0, 0, 0.9f).rot(0, 90f);
        m.lathe(6, 30f, 0.001f, -2.0f, 0.22f, 0f, 0.001f, 2.0f);
        m.at(0, 0, 0);
        //two little support feet
        m.color(Color.valueOf("3b4654")).style(metal, matPlain).box(-0.5f, -1.6f, 0f, 0.5f, -1.1f, 0.5f, true);
        m.color(Color.valueOf("3b4654")).style(metal, matPlain).box(-0.5f, 1.1f, 0f, 0.5f, 1.6f, 0.5f, true);
        return m;
    }

    /** Prism alloy: a bright hexagonal plate with a machined centre boss. */
    static Mesh prismAlloy(){
        Mesh m = new Mesh();
        Color c = Color.valueOf("e8f5ff");
        m.color(c).style(metal, matPlain).lathe(6, 0, 0.001f, 0f, 2.5f, 0f, 2.5f, 0.7f, 0.001f, 0.7f);
        m.color(shade(c, 0.78f)).style(metal, matPlain).at(0, 0, 0.7f).lathe(6, 30f, 0.001f, 0f, 1.1f, 0f, 0.95f, 0.45f, 0.001f, 0.45f);
        m.color(Color.valueOf("8fe9ff")).style(emissive, matPlain).at(0, 0, 1.15f).cyl(6, 0.45f, 0f, 0.05f);
        m.at(0, 0, 0);
        return m;
    }

    /** Concord core: a golden octahedron floating in a split ring. */
    static Mesh concordCore(){
        Mesh m = new Mesh();
        Color c = Color.valueOf("ffd875"), frame = Color.valueOf("c9d3e0");
        m.color(Color.valueOf("3b4654")).style(metal, matPlain).cyl(10, 0.9f, 0f, 0.3f);
        m.color(frame).style(metal, matPlain).at(0, 0, 1.7f).rot(0, 90f).ring(18, 1.9f, 2.25f, -0.18f, 0.18f);
        m.color(c).style(emissive, matPlain).at(0, 0, 1.7f).rot(0, 90f).ring(18, 1.75f, 1.9f, -0.08f, 0.08f);
        m.at(0, 0, 1.7f).rot(2, 45f);
        m.color(c).style(metal, matPlain).lathe(4, 0, 0.001f, -1.15f, 1.15f, 0f, 0.001f, 1.15f);
        m.at(0, 0, 0);
        return m;
    }

    /** Aegis alloy: a mint alloy ingot with a notch and a stamped mark. */
    static Mesh aegisAlloy(){
        Mesh m = ingot(Color.valueOf("8fd9bf"), 4.2f, 2.3f, 1.25f, false);
        m.color(Color.valueOf("6fd6b4").lerp(Color.black, 0.45f)).style(metal, matPlain).at(0, 0, 1.25f).lathe(6, 0, 0.001f, 0f, 0.55f, 0f, 0.55f, 0.08f, 0.001f, 0.08f);
        m.at(0, 0, 0);
        return m;
    }

    /** Signal core: a lavender logic chip with pins and a lit trace grid. */
    static Mesh signalCore(){
        Mesh m = new Mesh();
        Color c = Color.valueOf("c5a0fc");
        m.color(Color.valueOf("2a2535")).style(metal, matPlain).box(-2.0f, -1.5f, 0.3f, 2.0f, 1.5f, 0.9f, true);
        m.color(c).style(metal, matPlate).bevel(-1.6f, -1.1f, 0.9f, 1.6f, 1.1f, 1.25f, 0.1f);
        //pins
        for(int i = 0; i < 5; i++){
            float x = -1.6f + i * 0.8f;
            m.color(Color.valueOf("c3ccd0")).style(metal, matPlain).box(x - 0.12f, -2.0f, 0.2f, x + 0.12f, -1.5f, 0.45f, true);
            m.color(Color.valueOf("c3ccd0")).style(metal, matPlain).box(x - 0.12f, 1.5f, 0.2f, x + 0.12f, 2.0f, 0.45f, true);
        }
        //lit traces
        m.color(mix(c, Color.white, 0.45f)).style(emissive, matPlain);
        m.box(-1.2f, -0.05f, 1.25f, 1.2f, 0.05f, 1.29f, true);
        m.box(-0.05f, -0.8f, 1.25f, 0.05f, 0.8f, 1.29f, true);
        m.cyl(8, 0.3f, 1.25f, 1.3f);
        return m;
    }

    // ------------------------------------------------------------------ liquids

    /** Liquid: a plump droplet on a thin puddle; plasma adds an emissive core. */
    /** Industrial waste: a pressed briquette of mixed scrap - dull grey, with bits of swarf still sticking out. */
    static Mesh industrialWaste(){
        Mesh m = new Mesh();
        Color c = Color.valueOf("6b6a74");
        //the block itself, slightly irregular: two bevelled slabs, the upper one turned
        m.color(c).style(metal, matConcrete).bevel(-2.3f, -1.8f, 0f, 2.3f, 1.8f, 1.15f, 0.3f);
        m.color(shade(c, 1.2f)).style(metal, matConcrete).at(0, 0, 1.15f).rot(2, 14f)
            .bevel(-1.9f, -1.4f, 0f, 1.9f, 1.4f, 0.55f, 0.22f);
        m.at(0, 0, 0).rot(2, 0f);
        //scrap poking out of the press: four short bars on the top face, none crossing another
        float[][] bars = {{-1.1f, 0.6f, 24f}, {0.9f, 0.8f, -38f}, {1.0f, -0.7f, 10f}, {-0.9f, -0.8f, -14f}};
        for(float[] b : bars){
            m.color(shade(c, 1.45f)).style(metal, matPlain).at(b[0], b[1], 1.7f).rot(2, b[2])
                .box(-0.62f, -0.12f, 0f, 0.62f, 0.12f, 0.2f, true);
        }
        m.at(0, 0, 0).rot(2, 0f);
        return m;
    }

    /** Metal swarf: a bundle of curled turnings - three rings on a low tray, bright cut metal. */
    static Mesh metalSwarf(){
        Mesh m = new Mesh();
        Color c = Color.valueOf("a9b0bd");
        //tray
        m.color(shade(c, 0.6f)).style(metal, matPlain).box(-2.4f, -1.9f, 0f, 2.4f, 1.9f, 0.35f, true);
        //three curls, each in its own place, each a flat ring so it reads as a cutting
        float[][] curls = {{-1.1f, 0.45f, 0.95f, 0.22f}, {0.95f, 0.65f, 0.75f, 0.2f}, {0.35f, -0.95f, 0.85f, 0.2f}};
        for(float[] q : curls){
            m.color(c).style(metal, matPlain).at(q[0], q[1], 0.35f).ring(10, q[2] - q[3], q[2], 0f, 0.5f);
        }
        //one loose chip leaning on the tray wall
        m.color(shade(c, 1.25f)).style(metal, matPlain).at(1.6f, -1.2f, 0.35f).rot(2, 35f)
            .box(-0.8f, -0.1f, 0f, 0.8f, 0.1f, 0.5f, true);
        m.at(0, 0, 0).rot(2, 0f);
        return m;
    }

    /** Slag dust: a swept heap of mineral powder with a few unground lumps in it. */
    static Mesh slagDust(){
        Mesh m = new Mesh();
        Color c = Color.valueOf("8f8498");
        m.color(c).style(0, matConcrete).lathe(14, 0, 0.001f, 0f, 2.8f, 0f, 2.4f, 0.5f, 1.4f, 1.05f, 0.5f, 1.35f, 0.001f, 1.45f);
        float[][] lumps = {{1.35f, 0.55f, 0.6f, 0.3f}, {-1.25f, 0.8f, 0.55f, 0.26f}, {-0.2f, -1.45f, 0.5f, 0.28f}};
        for(float[] q : lumps){
            m.color(shade(c, 0.72f)).style(metal, matPlain).at(q[0], q[1], q[2]).lathe(6, 0, 0.001f, -q[3], q[3], 0f, 0.001f, q[3]);
        }
        m.at(0, 0, 0);
        return m;
    }

    static Mesh liquid(Color c, Color light, boolean plasma){
        Mesh m = new Mesh();
        m.color(c).style(glass | metal, matWater).lathe(18, 0, 0.001f, 0f, 3.0f, 0f, 2.7f, 0.22f, 0.001f, 0.26f);
        m.color(c).style(glass | metal, matGlass).at(0, 0, 0.26f);
        m.lathe(18, 0, 0.001f, 0f, 1.5f, 0.15f, 2.05f, 0.9f, 2.0f, 1.8f, 1.45f, 2.55f, 0.6f, 3.0f, 0.001f, 3.2f);
        if(plasma){
            m.color(light).style(emissive, matPlain).at(0, 0, 0.9f).lathe(10, 0, 0.001f, 0f, 0.85f, 0.4f, 0.6f, 1.3f, 0.001f, 1.9f);
        }
        m.at(0, 0, 0);
        return m;
    }
}
