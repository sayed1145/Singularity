package rbmk.gfx;

import arc.graphics.*;
import arc.math.*;
import mindustry.gen.*;
import mindustry.world.blocks.production.GenericCrafter.*;

import static rbmk.gfx.Mesh.*;

/** Shared 3x3 factory scaffolding: plinth, deck, corner bolts, accent stripe and state helpers. */
public abstract class FactoryModel extends BlockModel{
    public static final float deckZ = 1.4f;
    public final Color accent;

    protected FactoryModel(String name, Color accent){
        super(name, 3);
        this.accent = accent;
    }

    protected void deck(Mesh m){
        m.color(Pal.concrete).style(0, matConcrete).at(0, 0, 0).bevel(-12, -12, 0, 12, 12, 0.7f, 0.3f);
        m.color(Pal.white).style(0, matPlate).bevel(-11.3f, -11.3f, 0.7f, 11.3f, 11.3f, deckZ, 0.3f);
        //accent strip along the south edge and corner fixings
        m.color(accent).style(0, 0).box(-10.6f, -11.34f, 0.95f, 10.6f, -11.3f, 1.2f);
        for(int sx = -1; sx <= 1; sx += 2){
            for(int sy = -1; sy <= 1; sy += 2){
                m.color(Pal.darkSteel).style(metal, 0).at(sx * 10.4f, sy * 10.4f, deckZ).cyl(8, 0.36f, 0f, 0.14f);
            }
        }
        m.at(0, 0, 0);
    }

    /** Small operator console with an emissive screen (static). */
    protected void console(Mesh m, float x, float y){
        m.color(Pal.offWhite).style(0, matPlate).at(0, 0, 0).cbevel(x, y, deckZ, 2.4f, 1.4f, 1.5f, 0.15f);
        m.color(Pal.dark).style(0, 0).quad(x - 1.0f, y - 0.72f, deckZ + 0.55f, x + 1.0f, y - 0.72f, deckZ + 0.55f,
            x + 1.0f, y - 0.72f, deckZ + 1.3f, x - 1.0f, y - 0.72f, deckZ + 1.3f, 0, -1, 0);
        m.color(0.45f, 0.95f, 0.8f).style(emissive, 0).quad(x - 0.85f, y - 0.74f, deckZ + 0.7f, x + 0.2f, y - 0.74f, deckZ + 0.7f,
            x + 0.2f, y - 0.74f, deckZ + 1.15f, x - 0.85f, y - 0.74f, deckZ + 1.15f, 0, -1, 0);
        m.color(accent).style(emissive, 0).quad(x + 0.4f, y - 0.74f, deckZ + 0.7f, x + 0.85f, y - 0.74f, deckZ + 0.7f,
            x + 0.85f, y - 0.74f, deckZ + 0.85f, x + 0.4f, y - 0.74f, deckZ + 0.85f, 0, -1, 0);
        m.plain();
    }

    protected static float warmup(Building b){
        return b instanceof GenericCrafterBuild g ? g.warmup : 0f;
    }

    protected static float total(Building b){
        return b instanceof GenericCrafterBuild g ? g.totalProgress : 0f;
    }

    protected static float progress(Building b){
        return b instanceof GenericCrafterBuild g ? g.progress : 0f;
    }

    protected static float smooth(float a, float b, float t){
        float u = Mathf.clamp((t - a) / (b - a));
        return u * u * (3f - 2f * u);
    }
}
