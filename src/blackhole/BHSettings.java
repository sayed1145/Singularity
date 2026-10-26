package blackhole;

import arc.Core;
import arc.Events;
import arc.scene.ui.layout.Table;
import blackhole.g3d.Budget;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.ui.dialogs.SettingsMenuDialog;

/**
 * v8.5.2：奇点自己的设置页。
 *
 * <p>以前模组只有一个「黑洞引力透镜」开关，寄放在官方的图像页里。现在单独开一页，
 * 把那个开关搬过来，并补上几个真正会生效的性能选项。
 *
 * <p><b>所有默认值都保持 v8.5.1 的画面表现</b> —— 这一版只优化性能、不改画面。
 * 下面这些选项是把"拿画面换帧数"的选择权交给玩家：不动它们，看到的就是原来的样子。
 */
public final class BHSettings{
    private BHSettings(){}

    static boolean built;

    // ------------------------------------------------------------------ setting accessors

    /** 引力透镜（从官方「图像」页搬过来的那一个）。 */
    public static boolean lens(){
        return Core.settings == null || Core.settings.getBool("bh-lens", true);
    }

    /** 每帧实时几何预算（四边形数）；用完的模型退回烘培静帧。 */
    public static int liveQuads(){
        return Core.settings == null ? 14000 : Core.settings.getInt("bh-live-quads", 14000);
    }

    /** 屏幕上一个世界单位不足这么多像素时，直接跳过实时零件（远缩放）。 */
    public static float minPpu(){
        return (Core.settings == null ? 135 : Core.settings.getInt("bh-min-ppu", 135)) / 100f;
    }

    /** 最强省电档：所有模型一律画烘培静帧，不画任何实时零件（会失去动画）。 */
    public static boolean alwaysBaked(){
        return Core.settings != null && Core.settings.getBool("bh-always-baked", false);
    }

    /** 把设置推给运行时的静态字段。装设置、改设置、读档都会走一次。 */
    public static void apply(){
        Budget.maxQuads = Math.max(0, liveQuads());
        Budget.minPpu = minPpu();
        Budget.forceFallback = alwaysBaked();
        astro.g3d.Budget.maxQuads = Budget.maxQuads;
        astro.g3d.Budget.minPpu = Budget.minPpu;
        astro.g3d.Budget.forceFallback = Budget.forceFallback;
    }

    // ------------------------------------------------------------------ install

    public static void install(){
        apply();
        //the numbers are read straight off the settings at every draw, so a change on the title screen
        //still lands on the world that is loaded afterwards
        Events.on(WorldLoadEvent.class, e -> apply());
        if(Vars.ui == null || Vars.ui.settings == null || built) return;
        built = true;
        Vars.ui.settings.addCategory(tr("bh.settings.title", "奇点 Singularity"), BHSettings::build);
    }

    // ------------------------------------------------------------------ page

    static String tr(String key, String def){
        return Core.bundle == null ? def : Core.bundle.get(key, def);
    }

    static void build(SettingsMenuDialog.SettingsTable t){
        t.row();
        t.add(tr("bh.settings.perf", "性能 Performance")).color(arc.graphics.Color.lightGray).left().padTop(8f);
        t.row();
        t.labelWrap(tr("bh.settings.note",
            "默认值与 v8.5.1 的画面完全一致：这一版只优化性能。下面几项是把画面换帧数的选项，不动它们就还是原来的样子。"))
            .color(arc.graphics.Color.gray).left().padTop(2f).width(560f);
        t.row();

        t.sliderPref("bh-live-quads", 14000, 1000, 40000, 1000,
            i -> i + " " + tr("bh.settings.quads", "quads"), v -> apply());
        t.sliderPref("bh-min-ppu", 135, 50, 300, 5,
            i -> (i / 100f) + " " + tr("bh.settings.ppu", "px/unit"), v -> apply());
        t.checkPref("bh-always-baked", false, v -> apply());

        //live readout, so the two numbers above are not guesswork
        t.row();
        t.label(() -> Core.bundle.format("bh.settings.stats", Budget.lastUsed, Budget.maxQuads, Budget.lastFallbacks))
            .color(arc.graphics.Color.lightGray).left().padTop(4f).row();

        t.row();
        t.add(tr("bh.settings.video", "画面 Video")).color(arc.graphics.Color.lightGray).left().padTop(14f).row();
        //v8.5.1 kept this single toggle on the vanilla graphics page; it belongs with the rest of the mod
        t.checkPref("bh-lens", true);

        t.row();
        t.button(tr("bh.settings.reset", "恢复默认"), () -> {
            Core.settings.put("bh-lens", true);
            Core.settings.put("bh-live-quads", 14000);
            Core.settings.put("bh-min-ppu", 135);
            Core.settings.put("bh-always-baked", false);
            apply();
            //the sliders read their stored value once, when the page is built; the restored numbers show
            //up the next time this category is opened
        }).size(400f, 54f).padTop(14f).row();
    }
}
