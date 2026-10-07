package astro.g3d;

import arc.*;

/** Screen scale helper shared by the live 3D renderer (pixels per world unit at the current zoom). */
public final class View{
    private View(){}

    public static float pixelsPerUnit(){
        if(Core.graphics == null || Core.camera == null || Core.camera.width <= 0f) return 4f;
        return Core.graphics.getWidth() / Core.camera.width;
    }
}
