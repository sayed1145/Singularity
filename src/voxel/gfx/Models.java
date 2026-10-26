package voxel.gfx;

/** Registry of every 3D block model (used by the baker and the preview/draw tests). */
public final class Models{
    private Models(){}

    public static BlockModel[] all(){
        return new BlockModel[]{ForgeModel.instance, AssemblerModel.instance};
    }
}
