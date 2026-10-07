package blackhole.models;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import blackhole.g3d.*;
import mindustry.gen.Building;
import static blackhole.g3d.Mesh.*;

/** v8.4: entirely live geometry. No atlas lookup, baked layer, sprite, or sprite-budget fallback. */
public final class UtilityModels{
    private UtilityModels(){}
    public static final Color white = Color.valueOf("e5e9e7"), grey = Color.valueOf("929e9f"),
        dark = Color.valueOf("404b50"), teal = Color.valueOf("5faaa9"), violet = Color.valueOf("a395c3");
    public static final LiveModel furnace = new LiveModel("lumen-incinerator", 2, false);
    public static final LiveModel sorter = new LiveModel("inverted-lumen-sorter", 1, true);

    public static final class LiveModel extends BlockModel{
        public final boolean sorting;
        public final Mesh footing = new Mesh(), fixed = new Mesh(), rotor = new Mesh(), frame = new Mesh();
        private final int rotorVertex, rotorFace;
        private final int[] order;
        private final float[] keys;
        private final int[] footingOrder;
        private final float[] footingKeys;
        private final float[] baseR, baseG, baseB;
        private final float centerX, centerY, centerZ;

        LiveModel(String name, int size, boolean sorting){
            super(name, size);
            this.sorting = sorting;
            if(sorting){makeSorter(footing, fixed, rotor);centerX=0;centerY=0;centerZ=1.08f;}
            else{makeFurnace(footing, fixed, rotor);centerX=-1.3f;centerY=-.3f;centerZ=2.25f;}
            frame.add(fixed,0,0,0,2,0);
            rotorVertex=frame.verts;rotorFace=frame.faces;
            frame.add(rotor,centerX,centerY,centerZ,2,0);
            order=new int[frame.faces];keys=new float[frame.faces];
            footingOrder=new int[footing.faces];footingKeys=new float[footing.faces];
            for(int i=0;i<footingOrder.length;i++)footingOrder[i]=i;
            baseR=frame.cr.clone();baseG=frame.cg.clone();baseB=frame.cb.clone();
            for(int i=0;i<order.length;i++)order[i]=i;
            liveCost=frame.faces+footing.faces;
        }
        @Override public void load(){ /* deliberately no textures, including at far zoom */ }
        @Override public void buildStatic(Mesh m){m.add(footing,0,0,0,2,0);m.add(fixed,0,0,0,2,0);}
        @Override public void buildRest(Mesh m){m.add(rotor,centerX,centerY,centerZ,2,0);}
        @Override public void buildEnvelope(Mesh m){
            float r=sorting?1.12f:1.76f;
            m.at(centerX,centerY,centerZ-.16f).cyl(20,r,0,sorting?.65f:.5f);
        }
        @Override public void drawLive(Building b){drawAt(b.x,b.y,0,1,teal);}
        @Override public void draw(Building b){drawLive(b);}
        /** Persistent arrays and meshes, foundation first, then one depth-sorted static+dynamic machinery pass. */
        public void drawAt(float x,float y,float degrees,float heat,Color selected){
            float c=arc.math.Mathf.cosDeg(degrees),s=arc.math.Mathf.sinDeg(degrees);
            for(int i=0;i<rotor.verts;i++){
                frame.vx[rotorVertex+i]=centerX+rotor.vx[i]*c-rotor.vy[i]*s;
                frame.vy[rotorVertex+i]=centerY+rotor.vx[i]*s+rotor.vy[i]*c;
                frame.vz[rotorVertex+i]=centerZ+rotor.vz[i];
            }
            for(int i=0;i<frame.faces;i++){
                float k=(frame.flags[i]&emissive)!=0 ? .22f+.78f*arc.math.Mathf.clamp(heat) : 1f;
                frame.cr[i]=baseR[i]*k;frame.cg[i]=baseG[i]*k;frame.cb[i]=baseB[i]*k;
                if(sorting && i>=rotorFace && (frame.flags[i]&emissive)!=0){
                    frame.cr[i]=selected.r;frame.cg[i]=selected.g;frame.cb[i]=selected.b;
                }
            }
            Live.resetTint();Draw.color();Draw.mixcol();
            float previous=Live.minArea;Live.lod();Live.pose(0,0,0,2,0);
            //The foundation is below every machine part. Drawing its large top face first avoids
            //centroid painter-sort occlusion of small components at the far edge of the tile.
            Live.draw(footing,cam,x,y,footingOrder,footingKeys);
            Live.draw(frame,cam,x,y,order,keys);
            Live.minArea=previous;Live.resetTint();Draw.reset();
        }
        public float rotorRadius(){
            float max=0;for(int i=0;i<rotor.verts;i++)max=Math.max(max,(float)Math.hypot(rotor.vx[i],rotor.vy[i]));return max;
        }
    }
    private static void box(Mesh m,Color color,float x,float y,float z,float w,float d,float h,float bevel){
        m.color(color).style(metal,0).at(0,0,0).cbevel(x,y,z,w,d,h,bevel);
    }
    private static void makeFurnace(Mesh base,Mesh m,Mesh r){
        box(base,dark,0,0,0,15.2f,14.8f,.42f,.12f);
        box(base,white,0,0,.42f,14.8f,14.2f,.38f,.12f);
        box(base,teal,0,-7.13f,.50f,11.6f,.04f,.13f,.009f);
        // True open chamber: bottom, inner lining and a closed annular shell, not overlapping solids.
        m.color(dark).style(metal,0).at(-1.3f,-.3f,.8f).cyl(24,3.08f,0,.30f);
        m.color(white).style(metal,0).at(-1.3f,-.3f,0).lathe(28,0,
            2.14f,1.10f,2.94f,1.10f,2.94f,4.85f,2.80f,5.02f,2.30f,5.02f,2.14f,4.85f,2.14f,1.10f);
        m.color(teal).style(metal,0).at(-1.3f,-.3f,0).ring(28,2.941f,2.985f,3.85f,4.02f);
        m.color(.92f,.40f,.13f).style(emissive,0).at(-1.3f,-.3f,1.13f).cyl(28,2.12f,0,.07f);
        // Separate rear exhaust stack: low enough to stay inside the tile's north projection limit.
        box(m,grey,3.6f,3.9f,.8f,2.2f,2.2f,.25f,.08f);
        m.color(white).style(metal,0).at(3.6f,3.9f,1.05f).ring(16,.53f,.80f,0,1.90f);
        m.color(dark).style(metal,0).at(3.6f,3.9f,2.95f).ring(16,.53f,.91f,0,.18f);
        // Drive cabinet and service gap; the pipe mates to ports at matching heights.
        box(m,grey,4.5f,-2.6f,.8f,3.0f,3.6f,.23f,.06f);
        box(m,white,4.5f,-2.6f,1.03f,2.7f,3.3f,1.62f,.16f);
        for(int i=0;i<5;i++)box(m,dark,4.5f,-3.65f+i*.47f,2.65f,1.9f,.12f,.05f,.015f);
        box(m,teal,4.5f,-4.27f,1.71f,2.25f,.03f,.20f,.015f);
        m.color(grey).style(metal,0).pipe(12,.20f,1.5f,-1.9f,1.65f,3.15f,-1.9f,1.65f);
        for(int s:new int[]{-1,1})for(int t:new int[]{-1,1})m.color(grey).style(metal,0).at(s*6.65f,t*6.25f,.81f).cyl(6,.14f,0,.09f);
        // Rotor stays entirely within the 2.14-unit inner bore.
        r.color(grey).style(metal,0).at(0,0,0).cyl(12,.34f,0,.30f);
        for(int i=0;i<6;i++){
            r.color(i==0?teal:grey).style(metal,0).at(0,0,.12f).rot(2,i*60f).rot(0,12f)
                .box(.34f,-.16f,-.05f,1.66f,.16f,.05f,true);
        }
    }
    private static void makeSorter(Mesh base,Mesh m,Mesh r){
        box(base,dark,0,0,0,7.4f,7.4f,.28f,.10f);
        box(base,white,0,0,.28f,7.1f,7.1f,.27f,.07f);
        // Four separated low ports around an open diverter; no collision with the rotating vane.
        for(int i=0;i<4;i++){
            m.color(grey).style(metal,0).at(0,0,0).rot(2,i*90f).box(-.62f,1.48f,.55f,.62f,3.2f,.76f,true);
            m.color(violet).style(0,0).at(0,0,0).rot(2,i*90f).box(-.52f,2.56f,.78f,.52f,2.85f,.87f,true);
        }
        m.color(dark).style(metal,0).at(0,0,.55f).cyl(20,1.32f,0,.35f);
        m.color(white).style(metal,0).at(0,0,.90f).ring(20,1.16f,1.34f,0,.13f);
        r.color(white).style(metal,0).at(0,0,0).cyl(12,.29f,0,.40f);
        for(int i=0;i<3;i++)r.color(violet).style(metal,0).at(0,0,.18f).rot(2,i*120f).box(.29f,-.12f,-.08f,1.06f,.12f,.08f,true);
        r.color(teal).style(emissive,0).at(0,0,.4f).cyl(12,.22f,0,.045f);
        // Inversion chevrons are geometry, not a painted arrow or a region draw.
        for(int s:new int[]{-1,1}){
            m.color(violet).style(0,0).at(s*2.5f,-2.25f,.57f).rot(2,s*35f).box(-.50f,-.08f,0,.5f,.08f,.05f,true);
        }
    }
}
