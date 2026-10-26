package sixfold;

/**
 * Integration bridge for the former Sixfold Arsenal source pack.
 *
 * It is deliberately not a second Mod entry point: all content is now created while
 * Singularity's own mod context is active, so the game gives every imported content
 * ID the single, collision-safe `blackhole-` prefix.
 */
public final class AegisExpansion{
    private static final SixfoldMod content = new SixfoldMod();
    private static boolean loaded;

    private AegisExpansion(){}

    public static void load(){
        if(loaded) return;
        loaded = true;
        content.loadContent();
    }

    /** Called from the owning mod after every Java/HJSON content file has been created. */
    public static void validate(){
        content.init();
    }
}
