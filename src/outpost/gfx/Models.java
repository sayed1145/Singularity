package outpost.gfx;

/** Registry of every v2 3D block model (used by the baker and the preview/draw tests). */
public final class Models{
    private Models(){}

    public static BlockModel[] all(){
        return new BlockModel[]{
            RailgunModel.instance,
            LancerModel.instance,
            SlugPressModel.instance,
            CoilWinderModel.instance
        };
    }

    public static BlockModel byName(String name){
        for(BlockModel m : all()) if(m.name.equals(name)) return m;
        return null;
    }
}
