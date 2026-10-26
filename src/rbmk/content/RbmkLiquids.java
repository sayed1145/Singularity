package rbmk.content;
import arc.graphics.*;
import mindustry.type.*;
public class RbmkLiquids{
    public static Liquid demineralizedWater;
    public static void load(){
        demineralizedWater = new Liquid("demineralized-water", Color.valueOf("d9f4ff")){{
            heatCapacity = 1.05f; temperature = 0.32f; viscosity = 0.48f; coolant = true;
        }};
    }
}
