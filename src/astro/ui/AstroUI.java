package astro.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import astro.content.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.dialogs.*;

import static astro.content.DetainerType.*;

/**
 * Client side of the pilot controls: the panel, the settings page, the hot keys, the warp targeting, and the link to the
 * server (mode / ownership, commands). Never runs on a dedicated server.
 */
public final class AstroUI{
    private AstroUI(){}

    static PanelView view;
    static boolean built, settingsBuilt;
    static float pickArm = -1f, lastCfg = -1e9f, lastAimT = -1e9f, lastAx, lastAy;
    static boolean picking;
    static int lastUnit = -1, lastMask = -1;
    static boolean lastPro;
    static BaseDialog mapDialog;
    static float mapX, mapY;
    static boolean mapSet;

    public static void init(){
        Events.on(ClientLoadEvent.class, e -> tryBuild());
        Events.run(Trigger.update, AstroUI::update);
        Events.run(Trigger.drawOver, AstroUI::drawPick);
        tryBuild();
    }

    static void tryBuild(){
        if(Vars.ui == null) return;
        if(!built && Vars.ui.hudGroup != null){
            built = true;
            view = new PanelView();
            view.visible(() -> Vars.state != null && Vars.state.isGame() && PanelPrefs.show() && pilot() != null);
            Vars.ui.hudGroup.addChild(view);
        }
        if(!settingsBuilt && Vars.ui.settings != null){
            settingsBuilt = true;
            Vars.ui.settings.addCategory(Core.bundle.get("astro.settings.title", "Astro Detainer"), AstroUI::buildSettings);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // state
    // ---------------------------------------------------------------------------------------------------------------

    /** the Detainer the local player is flying */
    public static Unit pilot(){
        Player p = Vars.player;
        if(p == null || p.dead()) return null;
        Unit u = p.unit();
        return u != null && u.isAdded() && u.type instanceof DetainerType ? u : null;
    }

    /** the newest status from the server, or null when it is stale */
    public static AstroNet.Status status(){
        Unit u = pilot();
        AstroNet.Status st = AstroNet.status;
        if(u == null || st == null || st.unit != u.id || Time.time - st.time > 120f) return null;
        return st;
    }

    public static boolean picking(){ return picking; }

    static void say(String s, boolean bad){
        if(view != null) view.say(s, bad);
    }

    static String tr(String k, String d){ return Core.bundle.get(k, d); }

    // ---------------------------------------------------------------------------------------------------------------
    // per frame
    // ---------------------------------------------------------------------------------------------------------------

    static void update(){
        if(!built || Vars.state == null || !Vars.state.isGame()) return;
        Unit u = pilot();
        if(u == null){
            picking = false;
            lastUnit = -1;
            return;
        }
        //where the pilot is aiming
        if(Vars.mobile){
            if(Vars.player.mouseX != 0f || Vars.player.mouseY != 0f){ lastAx = Vars.player.mouseX; lastAy = Vars.player.mouseY; lastAimT = Time.time; }
        }else if(!Core.scene.hasMouse()){
            lastAx = Core.input.mouseWorldX(); lastAy = Core.input.mouseWorldY(); lastAimT = Time.time;
        }
        //mode and ownership go to the server whenever they change, and every few seconds
        boolean pro = PanelPrefs.pro();
        int mask = PanelPrefs.mask();
        AstroNet.Status st = status();
        boolean mismatch = st != null && Time.time - lastCfg > 40f && st.pro != pro;
        if(u.id != lastUnit || pro != lastPro || mask != lastMask || Time.time - lastCfg > 180f || mismatch){
            sendCfg();
            lastUnit = u.id;
        }
        //hot keys (desktop, or a keyboard on a phone)
        if(PanelPrefs.keys() && !Core.scene.hasKeyboard() && !Core.scene.hasDialog() && !Vars.ui.chatfrag.shown()){
            for(int id : PanelPrefs.KEYED){
                KeyCode k = PanelPrefs.key(id);
                if(k == null || !Core.input.keyTap(k)) continue;
                if(id == PanelGeom.B_AIM) warpAim(Core.input.mouseWorldX(), Core.input.mouseWorldY());
                else activate(id);
            }
        }
        if(picking){
            if(Core.input.keyTap(KeyCode.escape)){
                picking = false;
                say(tr("astro.ui.cancel", "warp cancelled"), false);
            }else if(Time.time > pickArm && Core.input.justTouched() && !Core.scene.hasMouse()){
                picking = false;
                warpAim(Core.input.mouseWorldX(), Core.input.mouseWorldY());
            }
        }
    }

    static void sendCfg(){
        boolean pro = PanelPrefs.pro();
        int mask = PanelPrefs.mask();
        lastPro = pro; lastMask = mask; lastCfg = Time.time;
        AstroNet.toServer(AstroNet.CFG, (pro ? 1 : 0) + "," + mask);
    }

    static void sendAct(int ab, float ax, float ay, int kind){
        AstroNet.toServer(AstroNet.ACT, ab + "," + ax + "," + ay + "," + kind);
    }

    static float[] aim(){
        Unit u = pilot();
        if(u == null || Time.time - lastAimT > 600f) return new float[]{u == null ? 0f : u.x, u == null ? 0f : u.y};
        return new float[]{lastAx, lastAy};
    }

    // ---------------------------------------------------------------------------------------------------------------
    // what a button does
    // ---------------------------------------------------------------------------------------------------------------

    public static void toggleAI(int id){
        boolean v = !PanelPrefs.ai(id);
        PanelPrefs.ai(id, v);
        sendCfg();
        say(tr("astro.ab." + id, "?") + (v ? tr("astro.ui.toai", " -> AI") : tr("astro.ui.toyou", " -> you")), false);
    }

    public static void activate(int id){
        Unit u = pilot();
        if(u == null || view == null) return;
        AstroNet.Status st = status();
        switch(id){
            case PanelGeom.B_AUTO -> { PanelPrefs.pro(false); sendCfg(); say(tr("astro.ui.mode.auto", "AUTO: you fly and warp"), false); return; }
            case PanelGeom.B_PRO -> { PanelPrefs.pro(true); sendCfg(); say(tr("astro.ui.mode.pro", "PRO: you command"), false); return; }
            case PanelGeom.B_FOLD -> { PanelPrefs.folded(!PanelPrefs.folded()); return; }
            default -> {}
        }
        if(st == null){ say(tr("astro.ui.link", "linking..."), true); return; }
        int s = view.stateOf(id, st, true);
        if(id < 10){
            if(s != DetainerCtl.OK){
                say(s == DetainerCtl.AI_OWNED ? (PanelPrefs.pro() ? tr("astro.why.7", "the AI handles this") : tr("astro.why.auto", "AUTO: switch to PRO to use it")) : tr("astro.why." + s, "not now"), s != DetainerCtl.AI_OWNED);
                return;
            }
            float[] a = aim();
            sendAct(id, a[0], a[1], 0);
            return;
        }
        if(id == PanelGeom.B_MAP){
            if(s == DetainerCtl.NO_CHARGE || s == DetainerCtl.COOLDOWN || s == DetainerCtl.BUSY){ say(tr("astro.why." + s, "not now") + " " + Math.round(st.charge * 100f) + "%", true); return; }
            openMap();
            return;
        }
        if(s != DetainerCtl.OK){
            String extra = s == DetainerCtl.NO_CHARGE ? " " + Math.round(st.charge * 100f) + "/" + Math.round((id == PanelGeom.B_FOE ? st.costEnemy : id == PanelGeom.B_HOME ? st.costHome : 0.16f) * 100f) + "%" : "";
            say(tr("astro.why." + s, "not now") + extra, true);
            return;
        }
        if(id == PanelGeom.B_AIM){
            picking = true;
            pickArm = Time.time + 8f;
            say(tr("astro.ui.pickshort", "tap the map"), false);
        }else{
            float[] a = aim();
            sendAct(AB_WARP, a[0], a[1], id == PanelGeom.B_FOE ? WARP_ENEMY : WARP_HOME);
        }
    }

    static void warpAim(float wx, float wy){
        Unit u = pilot();
        AstroNet.Status st = status();
        if(u == null || st == null) return;
        if(st.wind > 0f || st.warpCd > 0f){ say(tr("astro.why." + (st.wind > 0f ? 4 : 2), "not now"), true); return; }
        float cost = DetainerCtl.warpCost(Mathf.dst(u.x, u.y, wx, wy));
        if(st.charge < cost){
            say(tr("astro.why.6", "warp charging") + " " + Math.round(st.charge * 100f) + "/" + Math.round(cost * 100f) + "%", true);
            return;
        }
        sendAct(AB_WARP, wx, wy, WARP_AIM);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // the marker while choosing a warp point
    // ---------------------------------------------------------------------------------------------------------------

    static void drawPick(){
        if(!picking) return;
        Unit u = pilot();
        if(u == null) return;
        float mx = Core.input.mouseWorldX(), my = Core.input.mouseWorldY();
        AstroNet.Status st = status();
        float cost = DetainerCtl.warpCost(Mathf.dst(u.x, u.y, mx, my));
        boolean ok = st != null && st.charge >= cost;
        Color c = ok ? Color.valueOf("ffb838") : Color.valueOf("ff5a4a");
        Draw.z(Layer.overlayUI);
        Lines.stroke(1.5f, c);
        Draw.alpha(0.6f);
        Lines.dashLine(u.x, u.y, mx, my, (int)(Mathf.dst(u.x, u.y, mx, my) / 14f) + 1);
        Draw.alpha(1f);
        Lines.stroke(2f, c);
        float r = 14f + Mathf.absin(Time.time, 6f, 3f);
        Lines.circle(mx, my, r);
        Lines.line(mx - r - 6f, my, mx - r * 0.4f, my);
        Lines.line(mx + r + 6f, my, mx + r * 0.4f, my);
        Lines.line(mx, my - r - 6f, mx, my - r * 0.4f);
        Lines.line(mx, my + r + 6f, mx, my + r * 0.4f);
        Draw.color();
        Draw.reset();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // warp by the map of the whole world
    // ---------------------------------------------------------------------------------------------------------------

    static void openMap(){
        if(mapDialog == null){
            mapDialog = new BaseDialog(tr("astro.map.title", "Warp: choose a point on the map"));
            MapPick pick = new MapPick();
            mapDialog.cont.add(pick).grow().minSize(Scl.scl(260f), Scl.scl(200f));
            mapDialog.cont.row();
            mapDialog.cont.label(() -> {
                Unit u = pilot();
                AstroNet.Status st = status();
                if(!mapSet || u == null) return tr("astro.map.hint", "Tap the map to choose where to land");
                float cost = DetainerCtl.warpCost(Mathf.dst(u.x, u.y, mapX, mapY));
                return tr("astro.map.cost", "Cost") + " " + Math.round(cost * 100f) + "%   " + tr("astro.ui.warp", "WARP") + " " + (st == null ? 0 : Math.round(st.charge * 100f)) + "%";
            }).pad(6f);
            mapDialog.buttons.defaults().size(210f, 64f);
            mapDialog.buttons.button("@back", Icon.left, mapDialog::hide);
            mapDialog.buttons.button(tr("astro.map.go", "Warp here"), Icon.ok, () -> {
                if(!mapSet) return;
                mapDialog.hide();
                warpAim(mapX, mapY);
            }).disabled(b -> !mapSet);
        }
        mapSet = false;
        mapDialog.show();
    }

    /** the whole map as a picture; a tap chooses the landing point */
    static class MapPick extends Element{
        float rx, ry, rw, rh;

        MapPick(){
            touchable = Touchable.enabled;
            addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent e, float x, float y, int pointer, KeyCode button){
                    choose(x, y);
                    return true;
                }

                @Override
                public void touchDragged(InputEvent e, float x, float y, int pointer){
                    choose(x, y);
                }
            });
        }

        void choose(float lx, float ly){
            if(Vars.world == null || rw <= 0f) return;
            float fx = (lx - (rx - this.x)) / rw, fy = (ly - (ry - this.y)) / rh;
            if(fx < 0f || fx > 1f || fy < 0f || fy > 1f) return;
            mapX = fx * Vars.world.unitWidth();
            mapY = fy * Vars.world.unitHeight();
            mapSet = true;
        }

        @Override
        public void draw(){
            Draw.color(0f, 0f, 0f, 0.85f * parentAlpha);
            Fill.rect(x + width / 2f, y + height / 2f, width, height);
            if(Vars.world == null || Vars.world.width() <= 0) return;
            float ratio = (float)Vars.world.height() / Vars.world.width();
            rw = Math.min(width, height / ratio);
            rh = rw * ratio;
            rx = x + (width - rw) / 2f;
            ry = y + (height - rh) / 2f;
            var tex = Vars.renderer.minimap.getTexture();
            if(tex != null){
                Draw.color();
                Draw.alpha(parentAlpha);
                Draw.rect(Draw.wrap(tex), rx + rw / 2f, ry + rh / 2f, rw, rh);
                Vars.renderer.minimap.drawEntities(rx, ry, rw, rh, true);
            }
            Lines.stroke(Scl.scl(1.5f), Color.valueOf("ffb838"));
            Lines.rect(rx, ry, rw, rh);
            Unit u = pilot();
            if(u != null){
                Draw.color(Color.white);
                Lines.stroke(Scl.scl(1.5f));
                Lines.circle(rx + u.x / Vars.world.unitWidth() * rw, ry + u.y / Vars.world.unitHeight() * rh, Scl.scl(6f));
            }
            if(mapSet){
                float px = rx + mapX / Vars.world.unitWidth() * rw, py = ry + mapY / Vars.world.unitHeight() * rh;
                float r = Scl.scl(9f + Mathf.absin(Time.time, 6f, 2f));
                Draw.color(Color.valueOf("ffb838"));
                Lines.stroke(Scl.scl(2f));
                Lines.circle(px, py, r);
                Lines.line(px - r * 1.6f, py, px - r * 0.5f, py);
                Lines.line(px + r * 1.6f, py, px + r * 0.5f, py);
                Lines.line(px, py - r * 1.6f, px, py - r * 0.5f);
                Lines.line(px, py + r * 1.6f, px, py + r * 0.5f);
            }
            Draw.reset();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // settings page
    // ---------------------------------------------------------------------------------------------------------------

    static void buildSettings(SettingsMenuDialog.SettingsTable t){
        t.checkPref("astro-panel", true);
        t.checkPref("astro-pro", true, v -> { if(pilot() != null) sendCfg(); });
        t.sliderPref("astro-anchor", PanelLayout.MID_RIGHT, 0, PanelLayout.ANCHORS - 1, 1, i -> tr("astro.anchor." + i, "" + i));
        t.sliderPref("astro-style", 0, 0, 2, 1, i -> tr("astro.style." + i, "" + i));
        t.sliderPref("astro-scale", 100, 50, 180, 5, i -> i + "%");
        t.sliderPref("astro-offx", 0, -100, 100, 5, i -> i + "%");
        t.sliderPref("astro-offy", 0, -100, 100, 5, i -> i + "%");
        t.sliderPref("astro-opacity", 90, 30, 100, 5, i -> i + "%");
        t.checkPref("astro-fade", false);
        t.checkPref("astro-compact", false);
        t.checkPref("astro-folded", false);
        t.checkPref("astro-keys", true);

        t.row();
        t.add(tr("astro.settings.ai", "Hand to the AI (pro mode)")).left().padTop(12f).row();
        for(int id : PanelPrefs.DELEGATABLE){
            final int ab = id;
            t.checkPref("astro-ai-" + ab, false, v -> { if(pilot() != null) sendCfg(); });
        }
        t.row();
        t.add(tr("astro.settings.keys", "Hot keys (desktop)")).left().padTop(12f).row();
        for(int id : PanelPrefs.KEYED){
            final int ab = id;
            t.table(row -> {
                row.left();
                row.add(tr("astro.ab." + ab, "?")).width(Scl.scl(190f)).left();
                TextButton[] b = new TextButton[1];
                b[0] = row.button("", () -> rebind(ab)).size(Scl.scl(170f), Scl.scl(40f)).get();
                b[0].update(() -> {
                    KeyCode k = PanelPrefs.key(ab);
                    b[0].setText(k == null ? tr("astro.key.none", "none") : k.value);
                });
            }).left().padTop(4f).row();
        }
        t.row();
        t.button(tr("astro.settings.reset", "Reset panel settings"), () -> {
            PanelPrefs.reset();
            if(pilot() != null) sendCfg();
        }).size(Scl.scl(300f), Scl.scl(54f)).padTop(14f).row();
    }

    static void rebind(int id){
        BaseDialog d = new BaseDialog(tr("astro.key.title", "Press a key"));
        d.cont.add(tr("astro.ab." + id, "?")).row();
        d.cont.add(tr("astro.key.hint", "Press the new key. Esc cancels, Backspace clears.")).pad(8f);
        d.addCloseButton();
        float t0 = Time.time;
        d.update(() -> {
            if(Time.time - t0 < 10f) return;
            for(KeyCode k : KeyCode.all){
                if(k == KeyCode.unset || k == KeyCode.anyKey || k == KeyCode.unknown) continue;
                String n = k.name();
                if(n.startsWith("mouse") || n.startsWith("controller")) continue;
                if(!Core.input.keyTap(k)) continue;
                if(k == KeyCode.escape){ d.hide(); return; }
                PanelPrefs.key(id, k == KeyCode.backspace || k == KeyCode.del ? null : k);
                d.hide();
                return;
            }
        });
        d.show();
    }
}
