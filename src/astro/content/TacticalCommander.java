package astro.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import astro.ai.*;
import mindustry.Vars;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;

/**
 * Probability-space command core. Tap it and press the button: every combat unit of the team that is not flown by a
 * player is taken over by the {@link TacticalBrain} (squads, Monte-Carlo tactic sampling, focus fire, flanking, kiting,
 * retreat). Press again (or lose power) and every unit gets its own controller back. Works the same on desktop and Android.
 */
public class TacticalCommander extends Block{
    public Color glow = AstroFx.gold;

    public TacticalCommander(String name){
        super(name);
        size = 3;
        update = true;
        solid = true;
        hasPower = true;
        configurable = true;
        saveConfig = false;
        canOverdrive = false;
        sync = true;
        config(Boolean.class, (CommanderBuild b, Boolean v) -> b.on = v);
        //panel settings travel as Point2(kind, value): 0 mode, 1 micro, 2 raids, 3 roles, 4 aggression in percent
        config(Point2.class, (CommanderBuild b, Point2 p) -> b.setting(p.x, p.y));
    }

    /** the decision modes on the panel: AUTO first, then every tactic the brain knows */
    public static final int[] MODES = {TacticalBrain.AUTO, TacticalBrain.ENGAGE, TacticalBrain.FLANK, TacticalBrain.KITE, TacticalBrain.ADVANCE,
        TacticalBrain.PINCER, TacticalBrain.RAID, TacticalBrain.DEFEND, TacticalBrain.HOLD, TacticalBrain.RETREAT};

    static String modeKey(int m){
        return m < 0 || m >= TacticalBrain.NAMES.length ? "auto" : TacticalBrain.NAMES[m];
    }

    static String modeName(int m){
        return Core.bundle.get("astro.mode." + modeKey(m));
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("astro-tactics", (CommanderBuild e) -> new Bar(
            () -> e.on ? Core.bundle.format("bar.astro-tactics", TacticalBrain.taken(e.team), modeName(e.mode)) : Core.bundle.get("bar.astro-tactics-off"),
            () -> Pal.accent, () -> e.warmup));
    }

    public class CommanderBuild extends Building{
        public boolean on;
        public float warmup, phase;
        /** panel settings (AUTO = the brain decides everything by itself) */
        public int mode = TacticalBrain.AUTO;
        public boolean micro = true, raids = true, roles = true;
        public float aggression = 1f;
        /** live numbers shown on the panel (filled by the server, synchronised to clients) */
        public final TacticalBrain.Info snap = new TacticalBrain.Info();
        public float snapAvg, snapMicro;
        public int snapDecisions;
        float snapTimer;

        void setting(int kind, int value){
            switch(kind){
                case 0 -> mode = value < 0 || value >= TacticalBrain.NAMES.length ? TacticalBrain.AUTO : value;
                case 1 -> micro = value != 0;
                case 2 -> raids = value != 0;
                case 3 -> roles = value != 0;
                case 4 -> aggression = Mathf.clamp(value / 100f, 0.5f, 1.5f);
                default -> {}
            }
        }

        @Override
        public void updateTile(){
            boolean run = on && efficiency > 0.01f;
            warmup = Mathf.approachDelta(warmup, run ? 1f : 0f, 0.03f);
            phase += delta() * (0.4f + warmup * 2f);
            if(run){
                TacticalBrain.mark(team);
                TacticalBrain.Settings c = TacticalBrain.settings(team);
                c.mode = mode; c.micro = micro; c.raids = raids; c.roles = roles; c.aggression = aggression;
            }
            if(!Vars.net.client()){
                snapTimer += delta();
                if(snapTimer >= 10f){
                    snapTimer = 0f;
                    TacticalBrain.info(team, snap);
                    snapAvg = TacticalBrain.avgMs; snapMicro = Micro_avg();
                    snapDecisions = (int)Math.min(TacticalBrain.decisionTotal, Integer.MAX_VALUE);
                }
            }
        }

        float Micro_avg(){
            return TacticalBrain.microAvgMs();
        }

        @Override
        public boolean shouldConsume(){
            return on;
        }

        public String infoText(){
            StringBuilder sb = new StringBuilder();
            TacticalBrain.Info n = snap;
            sb.append(Core.bundle.format("astro.ui.status", n.units, n.squads, n.hosted)).append('\n');
            String verdict = !n.active ? "[lightgray]-" : n.enemyPower <= 1f ? "[lightgray]" + Core.bundle.get("astro.ui.noenemy") : n.weak ? "[scarlet]" + Core.bundle.get("astro.ui.weak") : "[accent]" + Core.bundle.get("astro.ui.strong");
            sb.append(Core.bundle.format("astro.ui.power", (int)n.power, (int)n.enemyPower, verdict + "[]")).append('\n');
            StringBuilder now = new StringBuilder();
            for(int t = 0; t < n.tactics.length; t++) if(n.tactics[t] > 0) now.append(modeName(t)).append(' ').append(n.tactics[t]).append("  ");
            sb.append(Core.bundle.format("astro.ui.now", mode == TacticalBrain.AUTO ? Core.bundle.get("astro.mode.auto") : modeName(mode), now.length() == 0 ? "-" : now.toString())).append('\n');
            sb.append(Core.bundle.format("astro.ui.cost", Strings.fixed(snapAvg, 2), Strings.fixed(snapMicro, 2), snapDecisions));
            return sb.toString();
        }

        @Override
        public void buildConfiguration(Table table){
            table.table(Styles.black6, t -> {
                t.margin(8f);
                t.defaults().pad(2f).left();
                t.table(h -> {
                    h.button(Icon.power, Styles.clearTogglei, () -> configure(!on)).size(56f).update(b -> b.setChecked(on));
                    h.label(() -> on ? "[accent]" + Core.bundle.get("astro.takeover.on") : "[lightgray]" + Core.bundle.get("astro.takeover.off")).padLeft(8f);
                }).left().row();
                t.label(this::infoText).left().wrap().width(340f).row();

                t.add(Core.bundle.get("astro.ui.mode")).color(Pal.accent).padTop(6f).row();
                t.table(g -> {
                    for(int k = 0; k < MODES.length; k++){
                        final int m = MODES[k];
                        g.button(modeName(m), Styles.togglet, () -> configure(new Point2(0, m))).size(108f, 38f).update(b -> b.setChecked(mode == m))
                            .tooltip(Core.bundle.get("astro.mode." + modeKey(m) + ".desc"));
                        if(k % 3 == 2) g.row();
                    }
                }).left().row();

                t.add(Core.bundle.get("astro.ui.options")).color(Pal.accent).padTop(6f).row();
                t.check(Core.bundle.get("astro.opt.micro"), micro, v -> configure(new Point2(1, v ? 1 : 0))).left().update(c -> c.setChecked(micro)).row();
                t.check(Core.bundle.get("astro.opt.raids"), raids, v -> configure(new Point2(2, v ? 1 : 0))).left().update(c -> c.setChecked(raids)).row();
                t.check(Core.bundle.get("astro.opt.roles"), roles, v -> configure(new Point2(3, v ? 1 : 0))).left().update(c -> c.setChecked(roles)).row();
                t.table(a -> {
                    a.add(Core.bundle.get("astro.opt.aggression")).padRight(8f);
                    Slider sl = new Slider(0.5f, 1.5f, 0.05f, false);
                    sl.setValue(aggression);
                    sl.moved(v -> configure(new Point2(4, Math.round(v * 100f))));
                    sl.update(() -> { if(!sl.isDragging()) sl.setValue(aggression); });
                    a.add(sl).width(170f);
                    a.label(() -> Math.round(aggression * 100f) + "%").padLeft(8f).width(50f);
                }).left().row();
                t.add(Core.bundle.get("astro.ui.hint")).color(Color.lightGray).left().wrap().width(340f).padTop(4f);
            });
        }

        @Override
        public Object config(){
            return on;
        }

        /** test hooks */
        public void tune(String what, float v){
            if(what.equals("dwell")) TacticalBrain.dwell = v;
            else if(what.equals("strafe")) astro.ai.Micro.strafeOn = v > 0f;
            else if(what.equals("dodge")) astro.ai.Micro.dodgeOn = v > 0f;
            else if(what.equals("kite")) astro.ai.Micro.kiteOn = v > 0f;
        }

        public boolean wouldTake(mindustry.gen.Unit u){ return TacticalBrain.canTake(u); }

        public String roleOf(String unitName){ return TacticalBrain.roleOf(mindustry.Vars.content.unit(unitName)); }

        /** short status used by tests and the info text */
        public String stat(){
            return "on=" + on + " warmup=" + Strings.fixed(warmup, 2) + " " + TacticalBrain.summary(team);
        }

        @Override
        public void draw(){
            super.draw();
            if(warmup < 0.02f) return;
            float z = Draw.z();
            Draw.z(Layer.blockOver);
            Draw.blend(Blending.additive);
            float cy = y + 1.2f;
            //probability cloud: dots on several orbits, each with its own speed, and a pulsing ring
            Lines.stroke(0.8f);
            Draw.color(glow, 0.5f * warmup);
            Lines.circle(x, cy, 6.4f + Mathf.sin(phase, 14f, 0.5f));
            for(int i = 0; i < 9; i++){
                float ang = phase * (1.1f + (i % 3) * 0.55f) * 3f + i * 40f, r = 3f + (i % 4) * 1.15f;
                Draw.color(i % 2 == 0 ? glow : AstroFx.hot, (0.35f + 0.5f * Mathf.absin(phase + i * 7f, 9f, 1f)) * warmup);
                Fill.circle(x + Mathf.cosDeg(ang) * r, cy + Mathf.sinDeg(ang) * r * 0.8f, 0.55f);
            }
            Draw.color(AstroFx.hot, 0.22f * warmup);
            Fill.circle(x, cy, 2.2f);
            Draw.blend();
            Draw.reset();
            Draw.z(z);
        }

        @Override
        public void drawLight(){
            Drawf.light(x, y, 50f * warmup + 10f, glow, 0.25f * warmup);
        }

        @Override
        public byte version(){
            return 1;
        }

        void writeSettings(Writes write){
            write.bool(on);
            write.b(mode); write.bool(micro); write.bool(raids); write.bool(roles); write.s((short)Math.round(aggression * 100f));
        }

        void readSettings(Reads read){
            on = read.bool();
            mode = read.b(); micro = read.bool(); raids = read.bool(); roles = read.bool(); aggression = read.s() / 100f;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            writeSettings(write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            on = read.bool();
            if(revision >= 1){
                mode = read.b(); micro = read.bool(); raids = read.bool(); roles = read.bool(); aggression = read.s() / 100f;
            }
        }

        /** network snapshot: the settings plus the live numbers of the panel */
        @Override
        public void writeSync(Writes write){
            writeSettings(write);
            write.bool(snap.active); write.bool(snap.weak);
            write.s((short)snap.units); write.s((short)snap.squads); write.s((short)snap.hosted);
            write.f(snap.power); write.f(snap.enemyPower);
            for(int i = 0; i < 9; i++) write.s((short)snap.tactics[i]);
            write.f(snapAvg); write.f(snapMicro); write.i(snapDecisions);
        }

        @Override
        public void readSync(Reads read, byte revision){
            readSettings(read);
            snap.active = read.bool(); snap.weak = read.bool();
            snap.units = read.s(); snap.squads = read.s(); snap.hosted = read.s();
            snap.power = read.f(); snap.enemyPower = read.f();
            for(int i = 0; i < 9; i++) snap.tactics[i] = read.s();
            snapAvg = read.f(); snapMicro = read.f(); snapDecisions = read.i();
        }
    }
}
