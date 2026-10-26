package astro.ui;

import arc.*;
import arc.input.*;

/** the player's settings for the pilot panel (stored in the game's settings) */
public final class PanelPrefs{
    private PanelPrefs(){}

    /** ability ids that can be handed to the AI in pro mode */
    public static final int[] DELEGATABLE = {0, 1, 2, 3, 4, 5, 6, 7, 9};
    /** every id that has a hotkey */
    public static final int[] KEYED = {0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13};
    static final KeyCode[] DEFAULT_KEYS = new KeyCode[16];
    static{
        DEFAULT_KEYS[0] = KeyCode.j; DEFAULT_KEYS[1] = KeyCode.k; DEFAULT_KEYS[2] = KeyCode.l;
        DEFAULT_KEYS[3] = KeyCode.u; DEFAULT_KEYS[4] = KeyCode.i; DEFAULT_KEYS[5] = KeyCode.o;
        DEFAULT_KEYS[6] = KeyCode.y; DEFAULT_KEYS[7] = KeyCode.p; DEFAULT_KEYS[9] = KeyCode.semicolon;
        DEFAULT_KEYS[10] = KeyCode.g; DEFAULT_KEYS[11] = KeyCode.h; DEFAULT_KEYS[12] = KeyCode.b; DEFAULT_KEYS[13] = KeyCode.v;
    }

    public static boolean show(){ return Core.settings.getBool("astro-panel", true); }
    public static boolean pro(){ return Core.settings.getBool("astro-pro", true); }
    public static void pro(boolean v){ Core.settings.put("astro-pro", v); }
    public static int anchor(){ return Math.max(0, Math.min(PanelLayout.ANCHORS - 1, Core.settings.getInt("astro-anchor", PanelLayout.MID_RIGHT))); }
    public static int style(){ return Math.max(0, Math.min(2, Core.settings.getInt("astro-style", 0))); }
    public static float scale(){ return Math.max(0.5f, Math.min(1.8f, Core.settings.getInt("astro-scale", 100) / 100f)); }
    public static float offX(){ return Core.settings.getInt("astro-offx", 0) / 100f; }
    public static float offY(){ return Core.settings.getInt("astro-offy", 0) / 100f; }
    public static float opacity(){ return Math.max(0.3f, Math.min(1f, Core.settings.getInt("astro-opacity", 90) / 100f)); }
    public static boolean fade(){ return Core.settings.getBool("astro-fade", false); }
    public static boolean compact(){ return Core.settings.getBool("astro-compact", false); }
    public static boolean folded(){ return Core.settings.getBool("astro-folded", false); }
    public static void folded(boolean v){ Core.settings.put("astro-folded", v); }
    public static boolean keys(){ return Core.settings.getBool("astro-keys", true); }
    public static boolean ai(int ab){ return Core.settings.getBool("astro-ai-" + ab, false); }
    public static void ai(int ab, boolean v){ Core.settings.put("astro-ai-" + ab, v); }

    /** the abilities the pilot keeps for himself (bit = 1 << id); the warp always stays his */
    public static int mask(){
        int m = (1 << 10) - 1;
        for(int ab : DELEGATABLE) if(ai(ab)) m &= ~(1 << ab);
        return m;
    }

    public static KeyCode key(int id){
        String s = Core.settings.getString("astro-key-" + id, DEFAULT_KEYS[id] == null ? "none" : DEFAULT_KEYS[id].name());
        if(s == null || s.equals("none")) return null;
        try{ return KeyCode.valueOf(s); }catch(Exception e){ return DEFAULT_KEYS[id]; }
    }

    public static void key(int id, KeyCode k){ Core.settings.put("astro-key-" + id, k == null ? "none" : k.name()); }

    public static void reset(){
        String[] ks = {"astro-panel", "astro-pro", "astro-anchor", "astro-style", "astro-scale", "astro-offx", "astro-offy", "astro-opacity", "astro-fade", "astro-compact", "astro-folded", "astro-keys"};
        for(String k : ks) Core.settings.remove(k);
        for(int i = 0; i < 16; i++){ Core.settings.remove("astro-ai-" + i); Core.settings.remove("astro-key-" + i); }
    }
}
