package astro.ui;

/**
 * Where the panel goes on the screen: an anchor plus the user's offsets, pushed out of every area the game's own HUD
 * uses (and out of the middle of the screen, where the Detainer is), shrunk when there is no room.
 * Pure geometry in pixels (y up), testable offline.
 */
public final class PanelLayout{
    public static final int BOTTOM_LEFT = 0, BOTTOM_RIGHT = 1, MID_LEFT = 2, MID_RIGHT = 3, TOP_LEFT = 4, TOP_RIGHT = 5, ANCHORS = 6;
    public static final float MIN_SCALE = 0.5f;
    static final float EDGE = 6f;

    public static final class Out{
        public float x, y, scale;
        /** true when the panel had to be folded or could not avoid everything */
        public boolean squeezed, overlaps;
    }

    /** reserved rectangles: x, y, w, h per entry */
    public static int reserved(float W, float H, float unit, boolean mobile, float[] out){
        int n = 0;
        //top left: waves, fps / ping lines (mobile: the button row as well)
        n = rect(out, n, 0, H - Math.min(mobile ? 190f * unit : 150f * unit, H * 0.45f), Math.min(mobile ? 345f * unit : 345f * unit, W * 0.45f), Math.min(mobile ? 190f * unit : 150f * unit, H * 0.45f));
        //top centre: core items (desktop)
        if(!mobile){
            float cw = Math.min(520f * unit, W * 0.5f);
            n = rect(out, n, (W - cw) / 2f, H - 56f * unit, cw, 56f * unit);
        }
        //top right: minimap
        float ms = Math.min(240f * unit, Math.min(W, H) * 0.42f);
        n = rect(out, n, W - ms, H - ms, ms, ms);
        //bottom left: chat (desktop), the movement stick (mobile)
        if(mobile) n = rect(out, n, 0, 0, Math.min(W * 0.36f, 300f * unit), Math.min(H * 0.42f, 230f * unit));
        else n = rect(out, n, 0, 0, Math.min(W * 0.36f, 360f * unit), Math.min(90f * unit, H * 0.2f));
        //bottom right (mobile): the buttons of the game
        if(mobile) n = rect(out, n, W - Math.min(W * 0.2f, 170f * unit), 0, Math.min(W * 0.2f, 170f * unit), Math.min(H * 0.22f, 120f * unit));
        //the middle of the screen: the camera follows the unit, nothing of the panel may cover it
        float gw = W * 0.2f, gh = H * 0.3f;
        n = rect(out, n, (W - gw) / 2f, (H - gh) / 2f, gw, gh);
        return n;
    }

    static int rect(float[] out, int n, float x, float y, float w, float h){
        out[n * 4] = x; out[n * 4 + 1] = y; out[n * 4 + 2] = w; out[n * 4 + 3] = h;
        return n + 1;
    }

    static boolean hits(float x, float y, float w, float h, float[] r, int n){
        for(int i = 0; i < n; i++){
            float rx = r[i * 4], ry = r[i * 4 + 1], rw = r[i * 4 + 2], rh = r[i * 4 + 3];
            if(x < rx + rw && x + w > rx && y < ry + rh && y + h > ry) return true;
        }
        return false;
    }

    /**
     * @param pw panel width in px at scale 1 (panel dp width * unit), ph the same for the height
     * @param scale the user's scale; offX / offY in -1..1 (fractions of the free room, +y up)
     */
    public static Out place(float W, float H, float pw, float ph, float scale, int anchor, float offX, float offY, float unit, boolean mobile, Out o){
        if(o == null) o = new Out();
        float[] res = new float[64];
        int n = reserved(W, H, unit, mobile, res);
        float s = Math.min(scale, Math.min((W - EDGE * 2f) / pw, (H - EDGE * 2f) / ph));
        o.squeezed = false; o.overlaps = false;
        for(;;){
            float w = pw * s, h = ph * s;
            float minX = EDGE, minY = EDGE, maxX = W - w - EDGE, maxY = H - h - EDGE;
            if(maxX >= minX && maxY >= minY){
                float ax = anchor == BOTTOM_LEFT || anchor == MID_LEFT || anchor == TOP_LEFT ? minX : maxX;
                float ay = anchor == BOTTOM_LEFT || anchor == BOTTOM_RIGHT ? minY : anchor == TOP_LEFT || anchor == TOP_RIGHT ? maxY : (minY + maxY) / 2f;
                float dx = ax + offX * (maxX - minX) * 0.5f;
                float dy = ay + offY * (maxY - minY) * 0.5f;
                dx = clamp(dx, minX, maxX); dy = clamp(dy, minY, maxY);
                if(!hits(dx, dy, w, h, res, n)){ o.x = dx; o.y = dy; o.scale = s; return o; }
                //nearest free spot on a grid
                float step = Math.max(6f, Math.min(W, H) / 90f);
                float bd = Float.MAX_VALUE, bx = 0, by = 0;
                boolean found = false;
                for(float y = minY; y <= maxY + 0.01f; y += step){
                    for(float x = minX; x <= maxX + 0.01f; x += step){
                        if(hits(x, y, w, h, res, n)) continue;
                        float d = (x - dx) * (x - dx) + (y - dy) * (y - dy);
                        if(d < bd){ bd = d; bx = x; by = y; found = true; }
                    }
                }
                if(found){ o.x = bx; o.y = by; o.scale = s; return o; }
            }
            if(s <= MIN_SCALE){
                o.squeezed = true;
                break;
            }
            s = Math.max(MIN_SCALE, s * 0.93f);
        }
        //nothing is free even at the smallest size: keep the anchor and report it
        float w = pw * s, h = ph * s;
        o.scale = s;
        o.x = clamp(anchor == BOTTOM_LEFT || anchor == MID_LEFT || anchor == TOP_LEFT ? EDGE : W - w - EDGE, EDGE, Math.max(EDGE, W - w - EDGE));
        o.y = clamp(anchor == BOTTOM_LEFT || anchor == BOTTOM_RIGHT ? EDGE : anchor == TOP_LEFT || anchor == TOP_RIGHT ? H - h - EDGE : (H - h) / 2f, EDGE, Math.max(EDGE, H - h - EDGE));
        o.overlaps = hits(o.x, o.y, w, h, res, n);
        return o;
    }

    static float clamp(float v, float lo, float hi){
        return v < lo ? lo : v > hi ? hi : v;
    }

    /** result of choose(): the layout variant and where it goes */
    public static final class Pick{
        public PanelGeom.Layout layout;
        public final Out out = new Out();
    }

    /**
     * Picks the layout variant (tall grid or low wide strip) and the place. style: 0 = automatic, 1 = tall, 2 = wide.
     * The variant that can keep more of the asked scale wins; a folded panel is only used when nothing else fits properly.
     */
    public static Pick choose(float W, float H, float unit, boolean mobile, boolean pro, boolean folded, boolean compact, int style,
                              float scale, int anchor, float offX, float offY, Pick p){
        if(p == null) p = new Pick();
        Out a = new Out(), b = new Out();
        PanelGeom.Layout tall = PanelGeom.build(pro, folded, compact, false), wide = PanelGeom.build(pro, folded, compact, true);
        if(style == 1) fill(p, tall, W, H, unit, mobile, scale, anchor, offX, offY);
        else if(style == 2) fill(p, wide, W, H, unit, mobile, scale, anchor, offX, offY);
        else{
            place(W, H, tall.w * unit, tall.h * unit, scale, anchor, offX, offY, unit, mobile, a);
            place(W, H, wide.w * unit, wide.h * unit, scale, anchor, offX, offY, unit, mobile, b);
            boolean aBad = a.overlaps || a.squeezed, bBad = b.overlaps || b.squeezed;
            boolean useWide;
            if(aBad && bBad) useWide = b.scale > a.scale;
            else if(aBad) useWide = true;
            else if(bBad) useWide = false;
            else useWide = b.scale > a.scale + 0.001f; //a tie goes to the roomier tall grid
            fill(p, useWide ? wide : tall, W, H, unit, mobile, scale, anchor, offX, offY);
        }
        //too small to use: fold it (header and bars only)
        if(!folded && (p.out.squeezed || p.out.overlaps || p.out.scale < Math.min(scale, 1f) * 0.6f)){
            Pick q = choose(W, H, unit, mobile, pro, true, compact, style, scale, anchor, offX, offY, new Pick());
            if(!(q.out.squeezed || q.out.overlaps)){ p.layout = q.layout; p.out.x = q.out.x; p.out.y = q.out.y; p.out.scale = q.out.scale; p.out.squeezed = true; p.out.overlaps = false; }
        }
        return p;
    }

    static void fill(Pick p, PanelGeom.Layout L, float W, float H, float unit, boolean mobile, float scale, int anchor, float offX, float offY){
        p.layout = L;
        place(W, H, L.w * unit, L.h * unit, scale, anchor, offX, offY, unit, mobile, p.out);
    }
}
