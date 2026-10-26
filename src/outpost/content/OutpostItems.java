package outpost.content;

import arc.graphics.Color;
import mindustry.type.Item;

/** v2 items: the original magnetic slug plus a new flux coil for the light turret. */
public final class OutpostItems{
    public static Item magneticSlug, fluxCoil;

    private OutpostItems(){}

    public static void load(){
        magneticSlug = new Item("magnetic-slug", Color.valueOf("8ee7ec")){{
            hardness = 4;
            cost = 1.1f;
            charge = 0.28f;
            explosiveness = 0.04f;
        }};

        fluxCoil = new Item("flux-coil", Color.valueOf("ffc35c")){{
            hardness = 3;
            cost = 0.9f;
            charge = 0.35f;
            explosiveness = 0.03f;
        }};
    }
}
