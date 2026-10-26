package astro.ui;

/**
 * Pure geometry of the pilot panel (no game classes, so it can be tested offline).
 * All numbers are in "dp" at scale 1 and measured from the panel's bottom-left corner (y up).
 * Element ids: 0..9 abilities (stab, slash, grab, orb, lance, overload, aegis, drain, warp (unused), release),
 * 10 warp to the aim point, 11 warp to the enemy, 12 warp home, 13 warp by map, 20 AUTO, 21 PRO, 22 fold.
 */
public final class PanelGeom{
    public static final int B_AIM = 10, B_FOE = 11, B_HOME = 12, B_MAP = 13, B_AUTO = 20, B_PRO = 21, B_FOLD = 22;
    public static final int MAX = 24;

    /** the ability buttons in grid order (ability ids of DetainerType.AB_*) */
    public static final int[] GRID = {0, 1, 2, 3, 4, 5, 6, 7, 9};

    public static final float W = 264f, PAD = 8f, GAP = 5f, HEAD = 30f, BAR = 13f, BTN_H = 50f, WARP_H = 46f, FOOT = 17f;

    /** a rectangle */
    public static final class R{
        public float x, y, w, h;
        void set(float x, float y, float w, float h){ this.x = x; this.y = y; this.w = w; this.h = h; }
    }

    public static final class Layout{
        public float w, h;
        public int n;
        public final int[] id = new int[MAX];
        public final float[] x = new float[MAX], y = new float[MAX], bw = new float[MAX], bh = new float[MAX];
        public final R title = new R(), energy = new R(), charge = new R(), msg = new R();
        public boolean pro, folded, compact, hasMsg, wide;

        void add(int i, float px, float py, float pw, float ph){
            id[n] = i; x[n] = px; y[n] = py; bw[n] = pw; bh[n] = ph; n++;
        }

        public int index(int elementId){
            for(int i = 0; i < n; i++) if(id[i] == elementId) return i;
            return -1;
        }
    }

    /** builds the layout; wide = the low, wide strip for landscape phones; compact = smaller buttons; folded = header and bars only */
    public static Layout build(boolean pro, boolean folded, boolean compact, boolean wide){
        return wide ? buildWide(pro, folded, compact) : buildTall(pro, folded, compact);
    }

    static Layout buildWide(boolean pro, boolean folded, boolean compact){
        Layout L = new Layout();
        L.pro = pro; L.folded = folded; L.compact = compact; L.wide = true;
        float k = compact ? 0.85f : 1f;
        float bh = 38f * k, hd = 26f;
        float w = pro ? 428f : 330f;
        float inner = w - PAD * 2f;
        float t = PAD;
        float headT = t; t += hd + 4f;
        float barT = t; t += 12f + 5f;
        float rowT = t;
        if(!folded) t += pro ? bh * 2f + GAP : bh;
        t += PAD - (folded ? 5f : 0f);
        float H = t;
        L.w = w; L.h = H;
        float segW = 46f, foldW = 26f;
        float foldX = w - PAD - foldW, proX = foldX - GAP - segW, autoX = proX - segW - 1f;
        float hy = H - headT - hd;
        L.add(B_FOLD, foldX, hy, foldW, hd);
        L.add(B_PRO, proX, hy, segW, hd);
        L.add(B_AUTO, autoX, hy, segW, hd);
        L.title.set(PAD, hy, autoX - GAP - PAD, hd);
        float bwid = (inner - GAP) / 2f;
        L.energy.set(PAD, H - barT - 12f, bwid, 12f);
        L.charge.set(PAD + bwid + GAP, H - barT - 12f, bwid, 12f);
        L.hasMsg = false;
        if(!folded){
            if(pro){
                float c1 = (inner - 6f * GAP) / 7f, c2 = (inner - 5f * GAP) / 6f;
                int[] order = {0, 1, 2, 3, 4, 5, 6, 7, 9, B_AIM, B_FOE, B_HOME, B_MAP};
                for(int i = 0; i < 13; i++){
                    if(i < 7) L.add(order[i], PAD + i * (c1 + GAP), H - rowT - bh, c1, bh);
                    else L.add(order[i], PAD + (i - 7) * (c2 + GAP), H - rowT - bh * 2f - GAP, c2, bh);
                }
            }else{
                float ww = (inner - 3f * GAP) / 4f;
                for(int i = 0; i < 4; i++) L.add(B_AIM + i, PAD + i * (ww + GAP), H - rowT - bh, ww, bh);
            }
        }
        return L;
    }

    static Layout buildTall(boolean pro, boolean folded, boolean compact){
        Layout L = new Layout();
        L.pro = pro; L.folded = folded; L.compact = compact;
        float k = compact ? 0.82f : 1f;
        float bh = BTN_H * k, wh = WARP_H * k;
        boolean foot = !compact;
        L.hasMsg = foot;
        float inner = W - PAD * 2f;

        //heights from the top edge downwards
        float t = PAD;
        float headT = t; t += HEAD + GAP;
        float eT = t; t += BAR + 3f;
        float cT = t; t += BAR + GAP;
        float gridT = t;
        if(!folded){
            if(pro) t += 3f * bh + 2f * GAP + GAP;
        }
        float warpT = t;
        if(!folded) t += wh + GAP;
        float msgT = t;
        if(foot) t += FOOT;
        t += PAD - (foot ? 0f : GAP);
        float H = t;
        L.w = W; L.h = H;

        float segW = 54f, foldW = 30f;
        float foldX = W - PAD - foldW, proX = foldX - GAP - segW, autoX = proX - segW - 1f;
        float hy = H - headT - HEAD;
        L.add(B_FOLD, foldX, hy, foldW, HEAD);
        L.add(B_PRO, proX, hy, segW, HEAD);
        L.add(B_AUTO, autoX, hy, segW, HEAD);
        L.title.set(PAD, hy, autoX - GAP - PAD, HEAD);
        L.energy.set(PAD, H - eT - BAR, inner, BAR);
        L.charge.set(PAD, H - cT - BAR, inner, BAR);

        if(!folded){
            if(pro){
                float cw = (inner - 2f * GAP) / 3f;
                for(int i = 0; i < GRID.length; i++){
                    int c = i % 3, r = i / 3;
                    L.add(GRID[i], PAD + c * (cw + GAP), H - (gridT + r * (bh + GAP)) - bh, cw, bh);
                }
            }
            float ww = (inner - 3f * GAP) / 4f;
            for(int i = 0; i < 4; i++) L.add(B_AIM + i, PAD + i * (ww + GAP), H - warpT - wh, ww, wh);
        }
        if(foot) L.msg.set(PAD, H - msgT - FOOT, inner, FOOT);
        return L;
    }

    public static int hit(Layout L, float px, float py){
        for(int i = L.n - 1; i >= 0; i--){
            if(px >= L.x[i] && px <= L.x[i] + L.bw[i] && py >= L.y[i] && py <= L.y[i] + L.bh[i]) return L.id[i];
        }
        return -1;
    }
}
