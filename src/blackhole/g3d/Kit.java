package blackhole.g3d;

import arc.graphics.*;
import arc.math.*;

import static blackhole.g3d.Mesh.*;

/**
 * Parametric modelling kit shared by every Singularity block, turret and unit: faction styles plus the
 * recurring industrial parts (plinths, decks, ring mounts, vents, tanks, pylons, conduits, crystals, consoles).
 * Keeps each model a few dozen lines while giving the whole mod one coherent visual language.
 */
public final class Kit{
    private Kit(){}

    /** A faction's materials. */
    public static final class Style{
        public final Color hull, hull2, trim, dark, metal, glow, glow2, base;
        public Style(String hull, String hull2, String trim, String dark, String metal, String glow, String glow2, String base){
            this.hull = Color.valueOf(hull); this.hull2 = Color.valueOf(hull2); this.trim = Color.valueOf(trim);
            this.dark = Color.valueOf(dark); this.metal = Color.valueOf(metal); this.glow = Color.valueOf(glow);
            this.glow2 = Color.valueOf(glow2); this.base = Color.valueOf(base);
        }
    }

    /** Singularity: gunmetal and graphite with accretion-amber light and violet lensing accents. */
    public static final Style sing = new Style("596170", "7a8190", "c49a5a", "25272e", "9aa3b0", "ffb45a", "b58cff", "4a4e57");
    /** Aegis / Sixfold on Serpulo: pale steel with mint trims. */
    public static final Style serpulo = new Style("a9b4b2", "c9d1cf", "6fd6b4", "343b3c", "c3ccd0", "8ff5d0", "7fd8ff", "6d7574");
    /** Aegis / Sixfold on Erekir: oxide-red hull, beryllium teal trims, hot orange light. */
    public static final Style erekir = new Style("8a5e52", "a87566", "72c7b8", "2e2522", "b3a39b", "ff8a4c", "ffcf6a", "5b4a45");
    /** Aurelia: pearl white, pale blue, silver trims, lumen cyan and resonance violet light. */
    public static final Style aurelia = new Style("dfe6f0", "b9c8dd", "c9d3e0", "2f3a4e", "d6dde6", "8fe9ff", "b69cff", "8a98ad");

    // ------------------------------------------------------------------ materials

    public static Mesh hull(Mesh m, Style s){ return m.color(s.hull).style(metal, matPlate); }
    public static Mesh hull2(Mesh m, Style s){ return m.color(s.hull2).style(metal, matPlate); }
    public static Mesh plain(Mesh m, Style s){ return m.color(s.hull).style(metal, matPlain); }
    public static Mesh trim(Mesh m, Style s){ return m.color(s.trim).style(metal, matPlain); }
    public static Mesh dark(Mesh m, Style s){ return m.color(s.dark).style(metal, matPlain); }
    public static Mesh steel(Mesh m, Style s){ return m.color(s.metal).style(metal, matPlain); }
    public static Mesh glow(Mesh m, Style s){ return m.color(s.glow).style(emissive, matPlain); }
    public static Mesh glow2(Mesh m, Style s){ return m.color(s.glow2).style(emissive, matPlain); }
    public static Mesh team(Mesh m){ return m.color(1f, 1f, 1f).style(Mesh.team, matPlain); }
    public static Mesh fins(Mesh m, Style s){ return m.color(s.dark).style(metal, matFins); }
    public static Mesh grate(Mesh m, Style s){ return m.color(s.dark).style(metal, matGrate); }
    public static Mesh hazard(Mesh m){ return m.color(0.95f, 0.76f, 0.2f).style(0, matHazard); }
    public static Mesh glass(Mesh m, Color c){ return m.color(c).style(Mesh.glass | metal, matGlass); }

    // ------------------------------------------------------------------ blocks

    /** Plinth + armoured deck covering the whole footprint; returns the deck height. */
    public static float deck(Mesh m, Style s, float half, float h){
        m.at(0, 0, 0);
        m.color(s.base).style(0, matConcrete).bevel(-half, -half, 0, half, half, h * 0.45f, 0.35f);
        hull(m, s).bevel(-half + 0.7f, -half + 0.7f, h * 0.45f, half - 0.7f, half - 0.7f, h, 0.35f);
        //trim strip along the south edge and corner fixings
        trim(m, s).box(-half + 1.4f, -half + 0.66f, h * 0.55f, half - 1.4f, -half + 0.7f, h * 0.85f);
        for(int sx = -1; sx <= 1; sx += 2){
            for(int sy = -1; sy <= 1; sy += 2){
                steel(m, s).at(sx * (half - 1.6f), sy * (half - 1.6f), h).cyl(8, 0.36f, 0f, 0.14f);
            }
        }
        m.at(0, 0, 0);
        return h;
    }

    /** Square armoured corner posts (for frames / gantries). */
    public static void posts(Mesh m, Style s, float inset, float z0, float z1, float w){
        for(int sx = -1; sx <= 1; sx += 2){
            for(int sy = -1; sy <= 1; sy += 2){
                hull2(m, s).at(0, 0, 0).cbevel(sx * inset, sy * inset, z0, w, w, z1 - z0, 0.12f);
                dark(m, s).cbox(sx * inset, sy * inset, z1 - 1.2f, w + 0.1f, w + 0.1f, 0.35f);
            }
        }
        m.at(0, 0, 0);
    }

    /** Turret ring: bevelled pedestal, bearing race and a glowing seam. Returns the top height. */
    public static float ring(Mesh m, Style s, float r, float z0, float z1){
        dark(m, s).at(0, 0, 0).lathe(24, 0, r + 0.6f, z0, r + 0.6f, z0 + (z1 - z0) * 0.55f, r, z1 - 0.3f, r - 0.3f, z1, 0.001f, z1);
        steel(m, s).at(0, 0, 0).ring(24, r - 0.2f, r + 0.15f, z1 - 0.35f, z1 + 0.05f);
        glow(m, s).at(0, 0, 0).ring(24, r + 0.62f, r + 0.72f, z0 + 0.3f, z0 + 0.55f);
        m.at(0, 0, 0);
        return z1;
    }

    /** Louvred vent box. */
    public static void vent(Mesh m, Style s, float x, float y, float z, float w, float d, float h){
        hull2(m, s).at(0, 0, 0).cbevel(x, y, z, w, d, h, 0.1f);
        fins(m, s).box(x - w / 2f + 0.25f, y - d / 2f + 0.25f, z + h, x + w / 2f - 0.25f, y + d / 2f - 0.25f, z + h + 0.02f);
    }

    /** Vertical pressure tank with a domed cap and a glowing level stripe. */
    public static void tank(Mesh m, Style s, float x, float y, float z, float r, float h, boolean glowBand){
        hull2(m, s).at(x, y, z).lathe(14, 0, r * 0.9f, 0f, r, 0.3f, r, h - r * 0.4f, r * 0.7f, h, 0.001f, h + r * 0.15f);
        if(glowBand) glow(m, s).at(x, y, z).ring(14, r + 0.01f, r + 0.08f, h * 0.45f, h * 0.55f);
        dark(m, s).at(x, y, z).ring(14, r, r + 0.12f, h * 0.15f, h * 0.2f);
        m.at(0, 0, 0);
    }

    /** Tapering emitter pylon with an emissive tip. */
    public static void pylon(Mesh m, Style s, float x, float y, float z, float r, float h, boolean useGlow2){
        dark(m, s).at(x, y, z).cyl(8, r * 1.35f, 0f, 0.5f);
        hull(m, s).at(x, y, z).lathe(8, 22.5f, r, 0.5f, r * 0.85f, h * 0.7f, r * 0.45f, h, 0.001f, h);
        (useGlow2 ? glow2(m, s) : glow(m, s)).at(x, y, z + h * 0.72f).lathe(8, 22.5f, r * 0.5f, 0f, r * 0.55f, h * 0.14f, 0.001f, h * 0.36f);
        m.at(0, 0, 0);
    }

    /** Pipe between two points with flanges. */
    public static void conduit(Mesh m, Style s, float x0, float y0, float z0, float x1, float y1, float z1, float r){
        steel(m, s).pipe(8, r, x0, y0, z0, x1, y1, z1);
        dark(m, s).axis(x0, y0, z0, x1, y1, z1).cyl(8, r * 1.5f, 0f, 0.3f);
        float l = (float)Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));
        dark(m, s).axis(x0, y0, z0, x1, y1, z1).cyl(8, r * 1.5f, l - 0.3f, l);
        m.at(0, 0, 0);
    }

    /** Hexagonal crystal with a pointed tip, tilted away from dir. */
    public static void crystal(Mesh m, Color c, boolean emissiveCrystal, float x, float y, float z, float r, float len, float tilt, float dir){
        m.color(c).style(emissiveCrystal ? Mesh.emissive : (Mesh.glass | metal), emissiveCrystal ? matPlain : matGlass);
        m.at(x, y, z).rot(2, dir).rot(0, tilt);
        m.lathe(6, 30f, 0.001f, 0f, r * 0.85f, 0f, r, len * 0.55f, r * 0.8f, len * 0.8f, 0.001f, len); //closed base: no visible interior when the base pokes out
        m.at(0, 0, 0);
    }

    /** Operator console with a lit screen facing south. */
    public static void console(Mesh m, Style s, float x, float y, float z){
        hull2(m, s).at(0, 0, 0).cbevel(x, y, z, 2.4f, 1.4f, 1.5f, 0.15f);
        dark(m, s).quad(x - 1.0f, y - 0.72f, z + 0.55f, x + 1.0f, y - 0.72f, z + 0.55f, x + 1.0f, y - 0.72f, z + 1.3f, x - 1.0f, y - 0.72f, z + 1.3f, 0, -1, 0);
        glow(m, s).quad(x - 0.85f, y - 0.74f, z + 0.7f, x + 0.2f, y - 0.74f, z + 0.7f, x + 0.2f, y - 0.74f, z + 1.15f, x - 0.85f, y - 0.74f, z + 1.15f, 0, -1, 0);
        glow2(m, s).quad(x + 0.4f, y - 0.74f, z + 0.7f, x + 0.85f, y - 0.74f, z + 0.7f, x + 0.85f, y - 0.74f, z + 0.85f, x + 0.4f, y - 0.74f, z + 0.85f, 0, -1, 0);
        m.at(0, 0, 0);
    }

    /** Flat emissive disc (for runes, pads). */
    public static void disc(Mesh m, Style s, float x, float y, float z, float r, boolean useGlow2){
        (useGlow2 ? glow2(m, s) : glow(m, s)).at(x, y, z).cyl(20, r, 0f, 0.03f);
        m.at(0, 0, 0);
    }

    /** Ring of n bolts. */
    public static void bolts(Mesh m, Style s, float x, float y, float z, float r, int n){
        for(int i = 0; i < n; i++){
            float a = i * 360f / n;
            steel(m, s).at(x + Mathf.cosDeg(a) * r, y + Mathf.sinDeg(a) * r, z).cyl(6, 0.25f, 0f, 0.15f);
        }
        m.at(0, 0, 0);
    }

    /** Horizontal torus approximated by n straight segments (each a convex box) - for rings on the ground or live gimbals. */
    public static void torus(Mesh m, Color c, int flags, float x, float y, float z, float r, float thick, float h, int n){
        m.at(0, 0, 0);
        m.color(c).style(flags, matPlain);
        for(int i = 0; i < n; i++){
            float a0 = i * 360f / n, a1 = (i + 1) * 360f / n;
            float x0 = Mathf.cosDeg(a0), y0 = Mathf.sinDeg(a0), x1 = Mathf.cosDeg(a1), y1 = Mathf.sinDeg(a1);
            float ri = r - thick / 2f, ro = r + thick / 2f;
            int a = m.vert(x + x0 * ri, y + y0 * ri, z - h / 2f), b = m.vert(x + x1 * ri, y + y1 * ri, z - h / 2f);
            int cc = m.vert(x + x1 * ro, y + y1 * ro, z - h / 2f), d = m.vert(x + x0 * ro, y + y0 * ro, z - h / 2f);
            int e = m.vert(x + x0 * ri, y + y0 * ri, z + h / 2f), f = m.vert(x + x1 * ri, y + y1 * ri, z + h / 2f);
            int g = m.vert(x + x1 * ro, y + y1 * ro, z + h / 2f), hh = m.vert(x + x0 * ro, y + y0 * ro, z + h / 2f);
            float mx = (x0 + x1) / 2f, my = (y0 + y1) / 2f;
            m.face(e, f, g, hh, 0, 0, 1);
            m.face(a, b, cc, d, 0, 0, -1);
            m.face(d, cc, g, hh, mx, my, 0);
            m.face(b, a, e, f, -mx, -my, 0);
        }
        m.plain();
    }
}
