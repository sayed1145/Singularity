package blackhole;

import arc.*;
import arc.struct.*;
import arc.util.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.*;
import mindustry.type.*;
import mindustry.world.*;

/**
 * Runtime audit (v7): every 3D block model must have its baked layers (and turret head sheets), every 3D unit its
 * icon and LOD sprite, every Aurelia terrain block its variant sprites (no "oh no" tiles anywhere) and the Aurelia
 * campaign its core / chassis / tech root. Missing assets are listed in the log instead of crashing the game.
 */
public final class V6IntegrityAudit{
    private static boolean checked;
    private V6IntegrityAudit(){}

    public static void checkClientAssets(){
        if(Vars.headless || checked) return;
        checked = true;
        Seq<String> missing = new Seq<>();
        int count = 0;
        for(BlockModel m : Models.all()){
            String base = BlockModel.prefix + m.name;
            count += need(missing, base + "-hd", base + "-preview");
            if(m instanceof TurretModel) count += need(missing, base + "-heads");
        }
        for(UnitType u : Vars.content.units()){
            if(u instanceof Unit3DType<?> && u.name.startsWith("blackhole-")) count += need(missing, u.name, u.name + "-lod");
        }
        for(Block b : AureliaContent.terrain()) count += need(missing, b.name + "1");
        for(Block b : AureliaTerra.floors()) count += need(missing, b.name + "1");
        for(mindustry.ctype.UnlockableContent c : AureliaContent.own){
            if(c instanceof Item || c instanceof Liquid || c instanceof StatusEffect) count += need(missing, c.name);
        }
        if(missing.size > 0) Log.err("[blackhole] v7 asset audit: @ of @ regions missing: @", missing.size, count, missing.toString(", "));

        if(AureliaContent.aurelia == null || AureliaContent.aurelia.generator == null || AureliaContent.aurelia.techTree == null ||
            AureliaContent.aurelia.defaultCore != AureliaContent.aureliaCore ||
            AureliaContent.aureliaCore.unitType != AureliaContent.lumenPilot){
            Log.err("[blackhole] v7 Aurelia campaign core, player chassis or tech root is incomplete");
        }else if(missing.isEmpty()){
            Log.info("[blackhole] v7 asset audit passed: @ regions (3D block layers, turret head sheets, unit icons/LOD, Aurelia terrain, items).", count);
        }
    }

    /** Counts the names; records those without an atlas region. A name pair "a", "b" means either is acceptable only for the status icon. */
    private static int need(Seq<String> missing, String... names){
        if(names.length == 2 && names[0].startsWith("status-")){
            if(!Core.atlas.has(names[0]) && !Core.atlas.has(names[1])) missing.add(names[1]);
            return 1;
        }
        for(String n : names) if(!Core.atlas.has(n)) missing.add(n);
        return names.length;
    }
}
