package blackhole;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.type.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/**
 * v8.2: sector launching with Aurelia resources.
 *
 * <p>The bug: flying to another sector asked for <b>vanilla</b> items (copper, lead). It is not a balance
 * mistake, it is a fallback in the game. {@code LaunchLoadoutDialog} picks the loadout schematic like this:
 *
 * <pre>
 * selected = universe.getLoadout(core);
 * if(selected == null) selected = schematics.getLoadouts().get((CoreBlock)Blocks.coreShard).first();
 * </pre>
 *
 * and {@code Universe.getLoadout(core)} only ever returns something that is inside
 * {@code Schematics.getLoadouts(core)}. That map is filled from the four vanilla loadouts
 * ({@code Loadouts.basicShard / basicFoundation / basicNucleus / basicBastion}) - a modded core is not in it,
 * so the dialog fell back to the <i>core shard</i> schematic and charged its vanilla cost.
 *
 * <p>The fix is to register a loadout for the Aurelia core, so the launch cost is the Aurelia core's own cost
 * in Aurelia items and the resources you carry across are Aurelia items as well. Nothing else is touched:
 * the schematic is one core block, exactly like the vanilla basic loadouts.
 */
public final class AureliaLaunch{
    /** the registered loadout, kept so the integrity audit can check it */
    public static @Nullable Schematic loadout;

    private AureliaLaunch(){}

    /** called once the game is loaded (client and server both have a Schematics instance by then) */
    public static void install(){
        try{
            CoreBlock core = AureliaContent.aureliaCore;
            if(core == null || schematics == null) return;

            Seq<Schematic> list = schematics.getLoadouts(core);
            if(list.any()){
                loadout = list.first();
                return;
            }

            StringMap tags = new StringMap();
            tags.put("name", core.localizedName);
            tags.put("description", Core.bundle.get("aurelia.loadout.desc", "Aurelia landing core."));
            Schematic schem = new Schematic(Seq.with(new Stile(core, 0, 0, null, (byte)0)), tags, core.size, core.size);

            //the public map is the one Universe.getLoadout() reads
            schematics.getLoadouts().get(core, Seq::new).add(schem);
            //and this is the one getDefaultLoadout() reads - private, so it is set defensively
            try{
                ObjectMap<CoreBlock, Schematic> def = Reflect.get(schematics, "defaultLoadouts");
                if(def != null) def.put(core, schem);
            }catch(Throwable ignored){
                //older/newer builds may rename the field - the public map above is enough to fix the cost
            }

            loadout = schem;
            Log.info("[blackhole] launch loadout registered for @ (@ item kinds, Aurelia priced)",
                core.name, core.requirements.length);
        }catch(Throwable t){
            Log.err("[blackhole] could not register the Aurelia launch loadout", t);
        }
    }

    /**
     * Second half of the fix: the extra resources you carry across are a saved {@link mindustry.type.ItemSeq}
     * that survives between planets. Coming from Serpulo it still holds copper and lead, and the launch dialog
     * would charge for them on Aurelia even though the rows are hidden there. Whenever an Aurelia sector loads,
     * anything that is not ours is dropped from that list, so a launch is only ever paid in Aurelia items.
     */
    public static void sanitizeLaunchResources(){
        try{
            if(state.rules.sector == null || AureliaContent.aurelia == null) return;
            if(state.rules.sector.planet != AureliaContent.aurelia) return;

            ItemSeq seq = universe.getLaunchResources();
            ItemSeq clean = new ItemSeq();
            boolean changed = false;
            for(ItemStack stack : seq){
                if(stack.amount <= 0) continue;
                if(stack.item.name.startsWith("blackhole-")){
                    clean.add(stack.item, stack.amount);
                }else{
                    changed = true;
                }
            }
            if(changed) universe.updateLaunchResources(clean);
        }catch(Throwable t){
            Log.err("[blackhole] could not clean the launch resources", t);
        }
    }

    /** true when the sector launch cost is paid in this mod's items only */
    public static boolean valid(){
        if(loadout == null) return false;
        boolean ok = true;
        for(mindustry.type.ItemStack stack : loadout.requirements()){
            if(stack.amount > 0 && !stack.item.name.startsWith("blackhole-")) ok = false;
        }
        return ok;
    }
}
