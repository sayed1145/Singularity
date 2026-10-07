package rbmk.content;
import arc.graphics.*;
import mindustry.type.*;
public class RbmkItems{
    public static Item uraniumPellets, zirconiumCladding, uraniumAssembly;
    public static void load(){
        uraniumPellets = new Item("uranium-pellets", Color.valueOf("d8e7a5")){{
            cost=2.1f; hardness=4; radioactivity=.75f; explosiveness=.05f;
        }};
        zirconiumCladding = new Item("zirconium-cladding", Color.valueOf("e7eff1")){{
            cost=1.8f; hardness=4; healthScaling=.12f;
        }};
        uraniumAssembly = new Item("uranium-assembly", Color.valueOf("f2f4f1")){{
            cost=4.8f; hardness=5; radioactivity=1.25f; explosiveness=.18f; charge=.08f;
        }};
    }
}
