package blackhole;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.util.*;
import blackhole.models.UtilityModels;
import mindustry.content.Fx;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.*;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.blocks.production.Incinerator;
import mindustry.world.blocks.distribution.Sorter;
import mindustry.ui.Bar;
import static blackhole.AureliaContent.*;
import static mindustry.type.ItemStack.with;

/** v8.4 utilities: vanilla behavior, procedural live 3D world/plan rendering. */
public final class AureliaUtilities{
    public static Block incinerator, invertedSorter;
    private AureliaUtilities(){}
    public static void load(){
        incinerator=new LiveIncinerator("lumen-incinerator"){{
            requirements(mindustry.type.Category.crafting,with(lumenite,40,aurite,25,prismGlass,12));
            size=2;health=320;consumePower(.5f);liquidCapacity=20;
            effect=Fx.none;
        }};
        invertedSorter=new LiveSorter("inverted-lumen-sorter"){{
            requirements(mindustry.type.Category.distribution,with(lumenite,2,aurite,2));
            buildCostMultiplier=3f;health=50;
        }};
        AureliaContent.own.add(incinerator);AureliaContent.own.add(invertedSorter);
    }
    /** Engine menu glyphs only. There is no block sprite, baked icon, or texture fallback. */
    private static TextureRegion glyph(TextureRegionDrawable icon){
        return icon==null?Core.atlas.white():icon.getRegion();
    }
    public static class LiveIncinerator extends Incinerator{
        public LiveIncinerator(String name){
            super(name);buildType=LiveIncineratorBuild::new;
            drawCached=false;drawDynamic=true;
        }
        @Override public void load(){
            super.load();loadIcon();region=fullIcon;
        }
        @Override public void loadIcon(){
            if(Core.atlas!=null)fullIcon=uiIcon=glyph(Icon.trash);
        }
        @Override public void createIcons(mindustry.graphics.MultiPacker packer){
            //Font glyphs are not sprite-atlas Pixmaps. Never feed them to Block.createIcons.
        }
        @Override public TextureRegion[] icons(){return new TextureRegion[]{region};}
        @Override public void getRegionsToOutline(Seq<TextureRegion> out){}
        @Override public void drawPlanRegion(BuildPlan p,Eachable<BuildPlan> plans){
            UtilityModels.furnace.drawAt(p.drawx(),p.drawy(),Time.time*.6f,.45f,UtilityModels.teal);
        }
        @Override public void setBars(){
            super.setBars();
            addBar("heat",(LiveIncineratorBuild b)->new Bar(()->Core.bundle.format("bar.blackhole-incinerator-heat",(int)(b.heat*100)),()->UtilityModels.teal,()->b.heat));
        }
        public class LiveIncineratorBuild extends IncineratorBuild{
            public float phase;
            @Override public void updateTile(){super.updateTile();phase=(phase+delta()*heat*1.2f)%360f;}
            @Override public void draw(){UtilityModels.furnace.drawAt(x,y,phase,heat,UtilityModels.teal);}
            //Official IncineratorBuild owns accept/handleItem, accept/handleLiquid and warm-up semantics.
        }
    }
    public static class LiveSorter extends Sorter{
        public LiveSorter(String name){
            super(name);invert=true;update=true;drawCached=false;drawDynamic=true;buildType=LiveSorterBuild::new;
        }
        @Override public void load(){
            super.load();loadIcon();region=cross=fullIcon;
        }
        @Override public void loadIcon(){
            if(Core.atlas!=null)fullIcon=uiIcon=glyph(Icon.filter);
        }
        @Override public void createIcons(mindustry.graphics.MultiPacker packer){
            //UI keeps the engine glyph; no generated body/icon sprite is packed.
        }
        @Override public TextureRegion[] icons(){return new TextureRegion[]{region};}
        @Override public void getRegionsToOutline(Seq<TextureRegion> out){}
        @Override public void drawPlanRegion(BuildPlan p,Eachable<BuildPlan> plans){
            UtilityModels.sorter.drawAt(p.drawx(),p.drawy(),Time.time*.35f,1,p.config instanceof Item i?i.color:UtilityModels.violet);
        }
        @Override public void drawPlanConfig(BuildPlan p,Eachable<BuildPlan> plans){}
        public class LiveSorterBuild extends SorterBuild{
            public float phase,activity;
            @Override public void updateTile(){
                activity=Math.max(0,activity-delta()*.04f);
                if(enabled)phase=(phase+delta()*(.3f+activity*2.4f))%360f;
            }
            @Override public void handleItem(Building source,Item item){super.handleItem(source,item);activity=1f;}
            @Override public void draw(){UtilityModels.sorter.drawAt(x,y,phase,1,sortItem==null?UtilityModels.violet:sortItem.color);}
            //Routing, alternating side selection, team checks, config and serialization stay vanilla.
        }
    }
}
