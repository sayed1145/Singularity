package astro.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import astro.content.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static astro.content.DetainerType.*;

/**
 * The pilot panel: one element that draws itself (no scene2d tables, so nothing can overlap) and answers touches and clicks.
 * The same code runs on desktop and on Android. Geometry comes from PanelGeom / PanelLayout.
 */
public class PanelView extends Element{
    static final Color BG = new Color(0.05f, 0.06f, 0.08f, 0.82f), EDGE = Color.valueOf("ffb838"), EDGE_DIM = new Color(0.45f, 0.42f, 0.36f, 1f),
        READY = new Color(0.20f, 0.15f, 0.05f, 1f), WAIT = new Color(0.10f, 0.10f, 0.12f, 1f), AIC = new Color(0.08f, 0.12f, 0.20f, 1f),
        AIE = new Color(0.36f, 0.62f, 1f, 1f), ON = new Color(0.95f, 0.62f, 0.12f, 1f), TXT = new Color(1f, 0.93f, 0.78f, 1f), DIM = new Color(0.58f, 0.58f, 0.6f, 1f),
        BAD = new Color(1f, 0.45f, 0.35f, 1f), WARP = Color.valueOf("ff8a1c");
    static final GlyphLayout gl = new GlyphLayout();
    static final Color tmpC = new Color();

    static Color col(float r, float g, float b, float a){
        return tmpC.set(r, g, b, a);
    }

    public PanelGeom.Layout lay = PanelGeom.build(true, false, false, false);
    final PanelLayout.Pick pick = new PanelLayout.Pick();
    public float ue = 1f;
    float alpha = 1f, lastTouch = -1e9f;
    String lastKey = "";
    public int hover = -1;
    final int[] pressed = new int[10];
    final float[] pressT = new float[10];
    final boolean[] consumed = new boolean[10];
    String msg = "";
    float msgT;
    int msgBad;

    public PanelView(){
        touchable = Touchable.enabled;
        java.util.Arrays.fill(pressed, -1);
        addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                lastTouch = Time.time;
                if(pointer >= pressed.length) return true;
                pressed[pointer] = at(x, y);
                pressT[pointer] = Time.time;
                consumed[pointer] = false;
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                lastTouch = Time.time;
                if(pointer >= pressed.length) return;
                int id = pressed[pointer];
                pressed[pointer] = -1;
                if(id >= 0 && !consumed[pointer] && at(x, y) == id) AstroUI.activate(id);
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                hover = at(x, y);
                lastTouch = Time.time;
                return false;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
                if(pointer < 1) hover = -1;
            }
        });
    }

    int at(float px, float py){
        return PanelGeom.hit(lay, px / ue, py / ue);
    }

    public void say(String s, boolean bad){
        msg = s; msgT = 240f; msgBad = bad ? 1 : 0;
    }

    /** recompute the layout when the screen or the settings changed */
    void relayout(){
        float W = Core.graphics.getWidth(), H = Core.graphics.getHeight();
        float ml = Math.max(0f, Core.scene.marginLeft), mr = Math.max(0f, Core.scene.marginRight), mt = Math.max(0f, Core.scene.marginTop), mb = Math.max(0f, Core.scene.marginBottom);
        float unit = Scl.scl(1f);
        boolean pro = PanelPrefs.pro(), folded = PanelPrefs.folded(), compact = PanelPrefs.compact();
        String key = W + "," + H + "," + unit + "," + pro + folded + compact + "," + PanelPrefs.style() + "," + PanelPrefs.scale() + "," + PanelPrefs.anchor() + "," + PanelPrefs.offX() + "," + PanelPrefs.offY() + "," + ml + mr + mt + mb + Vars.mobile;
        if(key.equals(lastKey)) return;
        lastKey = key;
        PanelLayout.choose(W - ml - mr, H - mt - mb, unit, Vars.mobile, pro, folded, compact, PanelPrefs.style(), PanelPrefs.scale(), PanelPrefs.anchor(), PanelPrefs.offX(), PanelPrefs.offY(), pick);
        lay = pick.layout;
        ue = unit * pick.out.scale;
        setBounds(pick.out.x + ml, pick.out.y + mb, lay.w * ue, lay.h * ue);
    }

    @Override
    public void act(float delta){
        super.act(delta);
        relayout();
        if(msgT > 0f) msgT -= Time.delta;
        //long press on an ability hands it to the AI / takes it back
        for(int p = 0; p < pressed.length; p++){
            if(pressed[p] >= 0 && !consumed[p] && Time.time - pressT[p] > 32f){
                int id = pressed[p];
                if(id < 10 && id != AB_WARP){
                    consumed[p] = true;
                    AstroUI.toggleAI(id);
                }
            }
        }
        float target = PanelPrefs.opacity();
        if(PanelPrefs.fade() && Time.time - lastTouch > 300f && hover < 0) target *= 0.35f;
        alpha = Mathf.lerpDelta(alpha, target, 0.15f);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // drawing
    // ---------------------------------------------------------------------------------------------------------------

    static String tr(String key, String def){
        return Core.bundle == null ? def : Core.bundle.get(key, def);
    }

    static void text(String s, float cx, float cy, float scale, Color c, float maxW, boolean left){
        Font f = Fonts.outline;
        if(f == null) return; //offline preview: no fonts
        boolean ints = f.usesIntegerPositions();
        f.setUseIntegerPositions(false);
        f.getData().setScale(scale);
        gl.setText(f, s);
        if(maxW > 0f && gl.width > maxW){
            scale *= maxW / gl.width;
            f.getData().setScale(scale);
            gl.setText(f, s);
        }
        f.setColor(c);
        f.draw(s, left ? cx : cx - gl.width / 2f, cy + gl.height / 2f);
        f.getData().setScale(1f);
        f.setColor(Color.white);
        f.setUseIntegerPositions(ints);
    }

    @Override
    public void draw(){
        Unit unit = AstroUI.pilot();
        if(unit == null) return;
        AstroNet.Status st = AstroUI.status();
        boolean live = st != null;
        if(!live) st = new AstroNet.Status();
        paint(st, live, (DetainerType)unit.type, alpha * parentAlpha);
    }

    /** draws the panel with the given status (also used by the offline preview) */
    public void paint(AstroNet.Status st, boolean live, DetainerType type, float a){
        float bx = x, by = y;
        Draw.reset();
        //body
        Draw.color(BG.r, BG.g, BG.b, BG.a * a);
        Fill.rect(bx + width / 2f, by + height / 2f, width, height);
        Lines.stroke(Math.max(1f, ue * 1.2f));
        Draw.color(EDGE.r, EDGE.g, EDGE.b, 0.85f * a);
        Lines.rect(bx, by, width, height);
        //corner ticks
        float cl = 10f * ue;
        Draw.color(1f, 0.75f, 0.3f, a);
        Lines.stroke(Math.max(1.5f, ue * 2f));
        Lines.line(bx, by + height, bx + cl, by + height);
        Lines.line(bx, by + height, bx, by + height - cl);
        Lines.line(bx + width, by, bx + width - cl, by);
        Lines.line(bx + width, by, bx + width, by + cl);

        //header
        PanelGeom.R t = lay.title;
        String title = msgT > 0f ? msg : tr("astro.ui.title", "ASTRO DETAINER");
        Color tc = msgT > 0f ? (msgBad == 1 ? BAD : TXT) : EDGE;
        if(msgT <= 0f && live){
            title += "  " + Math.round(st.hp * 100f) + "%";
            if(st.held > 0) title += "  " + tr("astro.ui.held", "held") + " " + st.held;
        }else if(!live) title = tr("astro.ui.link", "linking...");
        text(title, bx + (t.x + 2f) * ue, by + (t.y + t.h / 2f) * ue, 0.5f * ue, col(tc.r, tc.g, tc.b, a), (t.w - 2f) * ue, true);

        boolean pro = PanelPrefs.pro();
        for(int i = 0; i < lay.n; i++){
            int id = lay.id[i];
            float px = bx + lay.x[i] * ue, py = by + lay.y[i] * ue, pw = lay.bw[i] * ue, ph = lay.bh[i] * ue;
            boolean down = false;
            for(int p = 0; p < pressed.length; p++) if(pressed[p] == id) down = true;
            boolean hov = hover == id;
            if(id == PanelGeom.B_AUTO || id == PanelGeom.B_PRO){
                boolean sel = (id == PanelGeom.B_PRO) == pro;
                segment(px, py, pw, ph, tr(id == PanelGeom.B_PRO ? "astro.ui.pro" : "astro.ui.auto", id == PanelGeom.B_PRO ? "PRO" : "AUTO"), sel, down || hov, a);
            }else if(id == PanelGeom.B_FOLD){
                segment(px, py, pw, ph, "", false, down || hov, a);
                Draw.color(EDGE.r, EDGE.g, EDGE.b, a);
                float cx = px + pw / 2f, cy = py + ph / 2f, r = 5f * ue;
                if(PanelPrefs.folded()) Fill.tri(cx - r, cy - r * 0.5f, cx + r, cy - r * 0.5f, cx, cy + r * 0.6f);
                else Fill.tri(cx - r, cy + r * 0.5f, cx + r, cy + r * 0.5f, cx, cy - r * 0.6f);
            }else{
                button(id, px, py, pw, ph, st, live, type, down, hov, a);
            }
        }

        //bars
        energyBar(bx, by, st, type, a);
        chargeBar(bx, by, st, a);
        //footer: what the pointer is on / what is wrong
        if(lay.hasMsg){
            PanelGeom.R m = lay.msg;
            String s = hint(st, live);
            text(s, bx + (m.x + 2f) * ue, by + (m.y + m.h / 2f) * ue, 0.42f * ue, col(DIM.r, DIM.g, DIM.b, a), (m.w - 4f) * ue, true);
        }
        Draw.reset();
    }

    String hint(AstroNet.Status st, boolean live){
        if(AstroUI.picking()) return tr("astro.ui.pick", "Tap the map to warp there (Esc cancels)");
        int id = hover;
        for(int p = 0; p < pressed.length; p++) if(pressed[p] >= 0) id = pressed[p];
        if(id >= 0 && id < 14) return tr("astro.tip." + id, "");
        if(!PanelPrefs.pro()) return tr("astro.ui.autohint", "AUTO: you fly and warp, the AI fights");
        return tr("astro.ui.prohint", "PRO: you command. Hold a button to hand it to the AI");
    }

    void segment(float px, float py, float pw, float ph, String label, boolean sel, boolean hot, float a){
        Color f = sel ? ON : WAIT;
        Draw.color(f.r * (hot ? 1.15f : 1f), f.g * (hot ? 1.15f : 1f), f.b * (hot ? 1.15f : 1f), (sel ? 0.9f : 0.9f) * a);
        Fill.rect(px + pw / 2f, py + ph / 2f, pw, ph);
        Lines.stroke(Math.max(1f, ue));
        Draw.color(EDGE.r, EDGE.g, EDGE.b, (sel ? 1f : 0.55f) * a);
        Lines.rect(px, py, pw, ph);
        if(!label.isEmpty()) text(label, px + pw / 2f, py + ph / 2f, 0.5f * ue, col(sel ? 0.1f : 1f, sel ? 0.07f : 0.85f, sel ? 0.02f : 0.55f, a), pw - 4f * ue, false);
    }

    /** the state of an element: a DetainerCtl reason code (0 = usable) */
    int stateOf(int id, AstroNet.Status st, boolean live){
        if(!live) return DetainerCtl.NOTHING;
        if(id < 10){
            if(!PanelPrefs.pro()) return DetainerCtl.AI_OWNED;
            if(PanelPrefs.ai(id)) return DetainerCtl.AI_OWNED;
            return st.st[id];
        }
        //warp buttons
        if(st.wind > 0f) return DetainerCtl.BUSY;
        if(st.warpCd > 0f) return DetainerCtl.COOLDOWN;
        float need = id == PanelGeom.B_FOE ? st.costEnemy : id == PanelGeom.B_HOME ? st.costHome : 0.16f;
        if(id == PanelGeom.B_FOE && !st.enemyOk) return DetainerCtl.NO_TARGET;
        if(id == PanelGeom.B_HOME && !st.homeOk) return DetainerCtl.NO_TARGET;
        if(st.charge < need) return DetainerCtl.NO_CHARGE;
        return DetainerCtl.OK;
    }

    void button(int id, float px, float py, float pw, float ph, AstroNet.Status st, boolean live, DetainerType type, boolean down, boolean hov, float a){
        int s = stateOf(id, st, live);
        boolean ai = id < 10 && (!PanelPrefs.pro() || PanelPrefs.ai(id));
        boolean ready = s == DetainerCtl.OK;
        boolean on = (id == AB_ORB && st.orb) || (id == AB_AEGIS && st.aegis);
        boolean warp = id >= PanelGeom.B_AIM;
        Color fill = on ? ON : ready ? READY : ai ? AIC : WAIT;
        Color edge = on ? Color.white : ready ? (warp ? WARP : EDGE) : ai ? AIE : EDGE_DIM;
        float k = (down ? 1.5f : hov ? 1.2f : 1f);
        Draw.color(Math.min(1f, fill.r * k), Math.min(1f, fill.g * k), Math.min(1f, fill.b * k), 0.96f * a);
        Fill.rect(px + pw / 2f, py + ph / 2f, pw, ph);
        Lines.stroke(Math.max(1f, ue * (ready ? 1.6f : 1f)));
        Draw.color(edge.r, edge.g, edge.b, (ready || on ? 1f : 0.7f) * a);
        Lines.rect(px + ue * 0.5f, py + ue * 0.5f, pw - ue, ph - ue);
        float cx = px + pw / 2f, r = Math.min(pw, ph) * 0.2f;
        boolean compact = lay.compact || lay.wide;
        float gy = py + ph * (compact ? 0.62f : 0.66f);
        Color gc = on ? col(0.1f, 0.06f, 0.02f, 1f) : ready ? (warp ? WARP : TXT) : ai ? AIE : DIM;
        glyph(id, cx, gy, r, col(gc.r, gc.g, gc.b, a));
        String label = tr("astro.ab." + id, "?");
        Color lc = on ? col(0.1f, 0.06f, 0.02f, a) : col(ready ? 1f : 0.7f, ready ? 0.93f : 0.7f, ready ? 0.8f : 0.72f, a);
        text(label, cx, py + ph * (compact ? 0.30f : 0.33f), 0.46f * ue, lc, pw - 4f * ue, false);
        if(!(lay.compact)){
            //last line: hot key when usable, else the short reason
            String small;
            Color sc = DIM;
            if(ready){
                KeyCode key = Vars.mobile ? null : PanelPrefs.key(id);
                small = key == null || !PanelPrefs.keys() ? "" : key.value.toUpperCase();
                if(id == PanelGeom.B_FOE && live) small = Math.round(st.costEnemy * 100f) + "%" + (small.isEmpty() ? "" : " " + small);
                if(id == PanelGeom.B_HOME && live) small = Math.round(st.costHome * 100f) + "%" + (small.isEmpty() ? "" : " " + small);
            }else{
                small = tr("astro.why.short." + s, "");
                sc = s == DetainerCtl.AI_OWNED ? AIE : BAD;
            }
            if(!small.isEmpty()) text(small, cx, py + ph * 0.11f, 0.36f * ue, col(sc.r, sc.g, sc.b, a * 0.95f), pw - 3f * ue, false);
        }
        if(ai && id < 10){
            //AI badge
            text("AI", px + pw - 8f * ue, py + ph - 6f * ue, 0.36f * ue, col(AIE.r, AIE.g, AIE.b, a), 0f, false);
        }
    }

    /** small vector icons, drawn with lines and fills so that nothing has to be loaded */
    void glyph(int id, float cx, float cy, float r, Color c){
        Draw.color(c);
        float w = Math.max(1.2f, r * 0.22f);
        Lines.stroke(w);
        switch(id){
            case AB_STAB -> {
                Fill.tri(cx, cy + r, cx - r * 0.38f, cy - r * 0.15f, cx + r * 0.38f, cy - r * 0.15f);
                Lines.line(cx, cy - r * 0.15f, cx, cy - r);
            }
            case AB_SLASH -> {
                Lines.stroke(w * 1.4f);
                Lines.arc(cx - r * 0.25f, cy - r * 0.25f, r * 1.15f, 0.3f, 25f);
                Lines.stroke(w * 0.8f);
                Lines.arc(cx - r * 0.25f, cy - r * 0.25f, r * 0.75f, 0.22f, 30f);
            }
            case AB_GRAB -> {
                Lines.line(cx, cy - r, cx, cy - r * 0.2f);
                Lines.line(cx, cy - r * 0.2f, cx - r * 0.85f, cy + r * 0.8f);
                Lines.line(cx, cy - r * 0.2f, cx + r * 0.85f, cy + r * 0.8f);
                Lines.line(cx, cy - r * 0.2f, cx, cy + r);
            }
            case AB_ORB -> {
                Fill.circle(cx, cy, r * 0.55f);
                Lines.circle(cx, cy, r * 0.95f);
            }
            case AB_LANCE -> {
                Lines.line(cx - r * 0.9f, cy - r * 0.9f, cx + r * 0.5f, cy + r * 0.5f);
                Fill.tri(cx + r, cy + r, cx + r * 0.25f, cy + r * 0.55f, cx + r * 0.55f, cy + r * 0.25f);
                Lines.line(cx - r * 0.55f, cy - r * 0.15f, cx - r * 0.15f, cy - r * 0.55f);
            }
            case AB_OVERLOAD -> {
                for(int i = 0; i < 8; i++){
                    float ang = i * 45f + 22.5f;
                    Lines.line(cx + Mathf.cosDeg(ang) * r * 0.45f, cy + Mathf.sinDeg(ang) * r * 0.45f, cx + Mathf.cosDeg(ang) * r, cy + Mathf.sinDeg(ang) * r);
                }
                Fill.circle(cx, cy, r * 0.3f);
            }
            case AB_AEGIS -> {
                Lines.stroke(w * 1.3f);
                Lines.arc(cx, cy - r * 0.45f, r * 1.05f, 0.5f, 0f);
                Lines.line(cx - r * 1.05f, cy - r * 0.45f, cx + r * 1.05f, cy - r * 0.45f);
                Fill.circle(cx, cy - r * 0.05f, r * 0.22f);
            }
            case AB_DRAIN -> {
                for(int i = 0; i < 4; i++){
                    float ang = i * 90f + 45f;
                    float ox = Mathf.cosDeg(ang), oy = Mathf.sinDeg(ang);
                    Fill.tri(cx + ox * r * 0.25f, cy + oy * r * 0.25f, cx + ox * r * 0.85f - oy * r * 0.3f, cy + oy * r * 0.85f + ox * r * 0.3f, cx + ox * r * 0.85f + oy * r * 0.3f, cy + oy * r * 0.85f - ox * r * 0.3f);
                }
            }
            case AB_RELEASE -> {
                Lines.line(cx - r * 0.8f, cy - r * 0.8f, cx - r * 0.25f, cy - r * 0.25f);
                Lines.line(cx + r * 0.8f, cy + r * 0.8f, cx + r * 0.25f, cy + r * 0.25f);
                Lines.line(cx - r * 0.8f, cy + r * 0.8f, cx - r * 0.25f, cy + r * 0.25f);
                Lines.line(cx + r * 0.8f, cy - r * 0.8f, cx + r * 0.25f, cy - r * 0.25f);
                Lines.circle(cx, cy, r * 0.12f);
            }
            case PanelGeom.B_AIM -> {
                Lines.circle(cx, cy, r * 0.7f);
                Lines.line(cx - r, cy, cx - r * 0.35f, cy);
                Lines.line(cx + r, cy, cx + r * 0.35f, cy);
                Lines.line(cx, cy - r, cx, cy - r * 0.35f);
                Lines.line(cx, cy + r, cx, cy + r * 0.35f);
            }
            case PanelGeom.B_FOE -> {
                Fill.tri(cx, cy + r, cx - r * 0.9f, cy - r * 0.7f, cx + r * 0.9f, cy - r * 0.7f);
                Draw.color(BG.r, BG.g, BG.b, 1f);
                Lines.line(cx, cy + r * 0.35f, cx, cy - r * 0.15f);
                Fill.circle(cx, cy - r * 0.42f, Math.max(1f, r * 0.11f));
                Draw.color(c);
            }
            case PanelGeom.B_HOME -> {
                Fill.tri(cx - r, cy + r * 0.1f, cx + r, cy + r * 0.1f, cx, cy + r);
                Fill.rect(cx, cy - r * 0.45f, r * 1.3f, r * 0.9f);
            }
            case PanelGeom.B_MAP -> {
                Lines.rect(cx - r, cy - r * 0.8f, r * 2f, r * 1.6f);
                Lines.line(cx - r * 0.33f, cy - r * 0.8f, cx - r * 0.33f, cy + r * 0.8f);
                Lines.line(cx + r * 0.33f, cy - r * 0.8f, cx + r * 0.33f, cy + r * 0.8f);
                Lines.line(cx - r, cy, cx + r, cy);
            }
            default -> {}
        }
        Draw.color();
    }

    void energyBar(float bx, float by, AstroNet.Status st, DetainerType type, float a){
        PanelGeom.R e = lay.energy;
        float px = bx + e.x * ue, py = by + e.y * ue, pw = e.w * ue, ph = e.h * ue;
        float f = Mathf.clamp(st.energy / Math.max(1f, st.maxEnergy));
        Draw.color(0.02f, 0.02f, 0.03f, 0.9f * a);
        Fill.rect(px + pw / 2f, py + ph / 2f, pw, ph);
        Draw.color(1f, 0.72f, 0.2f, 0.95f * a);
        if(f > 0.001f) Fill.rect(px + pw * f / 2f, py + ph / 2f, pw * f, ph);
        //thresholds: aegis, lance, full store
        Draw.color(1f, 1f, 1f, 0.75f * a);
        Lines.stroke(Math.max(1f, ue * 0.8f));
        float[] marks = {(type == null ? 260f : type.aegisMinEnergy) / st.maxEnergy, (type == null ? 900f : type.lanceMin) / st.maxEnergy, 0.985f};
        for(float m : marks) Lines.line(px + pw * m, py, px + pw * m, py + ph);
        Lines.stroke(Math.max(1f, ue * 0.8f));
        Draw.color(EDGE.r, EDGE.g, EDGE.b, 0.6f * a);
        Lines.rect(px, py, pw, ph);
        text(tr("astro.ui.energy", "ENERGY") + " " + (int)st.energy, px + 3f * ue, py + ph / 2f, 0.36f * ue, col(1f, 1f, 1f, a), pw - 6f * ue, true);
    }

    void chargeBar(float bx, float by, AstroNet.Status st, float a){
        PanelGeom.R e = lay.charge;
        float px = bx + e.x * ue, py = by + e.y * ue, pw = e.w * ue, ph = e.h * ue;
        float f = Mathf.clamp(st.charge);
        Draw.color(0.02f, 0.02f, 0.03f, 0.9f * a);
        Fill.rect(px + pw / 2f, py + ph / 2f, pw, ph);
        Color c = f >= 0.999f ? Color.white : WARP;
        Draw.color(c.r, c.g, c.b, 0.95f * a);
        if(f > 0.001f) Fill.rect(px + pw * f / 2f, py + ph / 2f, pw * f, ph);
        Draw.color(1f, 1f, 1f, 0.75f * a);
        Lines.stroke(Math.max(1f, ue * 0.8f));
        for(float m : new float[]{0.16f, Math.max(0.16f, st.costEnemy), Math.max(0.16f, st.costHome)}) Lines.line(px + pw * Mathf.clamp(m), py, px + pw * Mathf.clamp(m), py + ph);
        Draw.color(WARP.r, WARP.g, WARP.b, 0.7f * a);
        Lines.rect(px, py, pw, ph);
        text(tr("astro.ui.warp", "WARP") + " " + Math.round(f * 100f) + "%", px + 3f * ue, py + ph / 2f, 0.36f * ue, col(f > 0.5f ? 0.1f : 1f, f > 0.5f ? 0.05f : 1f, f > 0.5f ? 0.02f : 1f, a), pw - 6f * ue, true);
    }
}
