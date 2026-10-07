package preview;

import blackhole.g3d.*;
import blackhole.models.*;

import java.io.*;
import java.util.*;

/** Verifies that every sprite the runtime looks up exists in assets/sprites (fails the build otherwise). */
public class AssetCheck{
    public static void main(String[] args){
        File sprites = new File(args.length > 0 ? args[0] : "assets/sprites");
        Models.load();
        List<String> missing = new ArrayList<>();
        int checked = 0;
        for(BlockModel m : Models.all()){
            for(String suffix : new String[]{"-hd", "-preview", "-icon", ""}){
                checked++;
                if(!new File(sprites, "blocks/3d/" + m.name + suffix + ".png").isFile()) missing.add("blocks/3d/" + m.name + suffix + ".png");
            }
            //v7.3: turret heads are live 3D, there is no -heads sheet any more - make sure none is left behind
            if(new File(sprites, "blocks/3d/" + m.name + "-heads.png").isFile()) missing.add("stale sheet blocks/3d/" + m.name + "-heads.png");
        }
        for(ItemModels.IconModel im : ItemModels.all()){
            checked++;
            if(!new File(sprites, im.folder + "/" + im.name + ".png").isFile()) missing.add(im.folder + "/" + im.name + ".png");
        }
        //v7.4: the merged mods (Astro Detainer, RBMK White Reactor) ship pre-baked sprites instead of baked models;
        //every region their draw code looks up must exist, or the game paints the error texture ("oh no") on them.
        String[] rbmkBlocks = {"pellet-plant", "cladding-mill", "fuel-assembly-plant", "water-treatment", "graphite-kiln", "rbmk-plant"};
        for(String n : rbmkBlocks){
            for(String suffix : new String[]{"", "-hd", "-front-hd", "-preview"}){
                checked++;
                if(!new File(sprites, "rbmk/" + n + suffix + ".png").isFile()) missing.add("rbmk/" + n + suffix + ".png");
            }
        }
        for(String n : new String[]{"uranium-pellets", "zirconium-cladding", "uranium-assembly", "demineralized-water"}){
            checked++;
            if(!new File(sprites, "rbmk/" + n + ".png").isFile()) missing.add("rbmk/" + n + ".png");
        }
        for(String n : new String[]{"astro-detainer", "astro-detainer-lod", "astro-fabricator", "astro-fabricator-top", "astro-fabricator-out"}){
            checked++;
            if(!new File(sprites, "astro/" + n + ".png").isFile()) missing.add("astro/" + n + ".png");
        }

        System.out.println("AssetCheck: " + checked + " sprites checked, " + missing.size() + " missing");
        for(String s : missing) System.out.println("  !! missing " + s);
        if(!missing.isEmpty()) System.exit(1);
    }
}
