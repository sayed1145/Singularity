package blackhole.g3d;

/**
 * Shared, deliberately restrained palette: off-white armour, two greys, near-black joints, one team stripe
 * and one cyan glow. "Clean and plain" comes from few colours on well-shaped panels, not from noise.
 */
public final class Palette{
    private Palette(){}

    public static void armor(Mesh m){ m.color(0.86f, 0.87f, 0.88f).style(0, Mesh.matPlain); }
    public static void armorShade(Mesh m){ m.color(0.74f, 0.75f, 0.77f).style(0, Mesh.matPlain); }
    public static void grey(Mesh m){ m.color(0.50f, 0.52f, 0.55f).style(Mesh.metal, Mesh.matPlain); }
    public static void steel(Mesh m){ m.color(0.62f, 0.64f, 0.67f).style(Mesh.metal, Mesh.matPlain); }
    public static void dark(Mesh m){ m.color(0.20f, 0.21f, 0.23f).style(Mesh.metal, Mesh.matPlain); }
    public static void black(Mesh m){ m.color(0.11f, 0.11f, 0.12f).style(0, Mesh.matPlain); }
    /** team-coloured stripe (tinted by the unit's team at draw time) */
    public static void team(Mesh m){ m.color(1f, 1f, 1f).style(Mesh.team, Mesh.matPlain); }
    /** team-coloured light */
    public static void teamGlow(Mesh m){ m.color(1f, 1f, 1f).style(Mesh.team | Mesh.emissive, Mesh.matPlain); }
    public static void glow(Mesh m){ m.color(0.56f, 0.93f, 1f).style(Mesh.emissive, Mesh.matPlain); }
    public static void glowWhite(Mesh m){ m.color(0.92f, 0.99f, 1f).style(Mesh.emissive, Mesh.matPlain); }
    public static void amber(Mesh m){ m.color(1f, 0.70f, 0.30f).style(Mesh.emissive, Mesh.matPlain); }
    public static void red(Mesh m){ m.color(1f, 0.33f, 0.28f).style(Mesh.emissive, Mesh.matPlain); }
}
