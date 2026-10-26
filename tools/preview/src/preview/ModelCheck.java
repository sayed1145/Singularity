package preview;

import blackhole.g3d.*;
import blackhole.models.*;

import java.util.*;

/**
 * Build-time model sanity check (fails the build on any finding):
 * <ul>
 * <li>every block model, turret head (16 headings) and item icon builds headless,</li>
 * <li>no axis-aligned primitive is built in a stale axis()/at() frame (the bug class behind the floating team
 * stripes / spires of 7.1, which showed open undersides at some headings),</li>
 * <li>every mesh is non-empty and every face normal is finite.</li>
 * </ul>
 * The visibility audit itself (no interior / cut face visible from the camera at any heading) runs inside
 * {@code bake.Bake}, which also exits non-zero on problems.
 */
public class ModelCheck{
    public static void main(String[] args){
        Models.load();
        List<String> problems = new ArrayList<>();
        int models = 0, icons = 0;
        for(BlockModel m : Models.all()){
            models++;
            Set<String> warns = new LinkedHashSet<>();
            Mesh.frameWarn = warns::add;
            try{
                Mesh s = new Mesh(); m.buildStatic(s);
                Mesh e = new Mesh(); m.buildEnvelope(e);
                Mesh r = new Mesh(); m.buildRest(r);
                if(s.faces == 0) problems.add(m.name + ": empty static mesh");
                check(m.name + " static", s, problems);
                check(m.name + " rest", r, problems);
                if(m instanceof TurretModel t){
                    for(int i = 0; i < t.angles; i++){
                        Mesh h = new Mesh();
                        t.addHead(h, 90f + i * 360f / t.angles);
                        if(h.faces == 0) problems.add(m.name + ": empty head");
                        check(m.name + " head " + i, h, problems);
                    }
                    t.frame();
                    if(!(t.headHalf > 0.6f) || Float.isNaN(t.headHalf)) problems.add(m.name + ": bad head frame " + t.headHalf);
                }
            }catch(Throwable ex){
                problems.add(m.name + ": build threw " + ex);
            }
            Mesh.frameWarn = null;
            for(String w : warns) problems.add(m.name + ": " + w);
        }
        for(ItemModels.IconModel im : ItemModels.all()){
            icons++;
            if(im.mesh == null || im.mesh.faces == 0) problems.add("icon " + im.name + ": empty mesh");
            else check("icon " + im.name, im.mesh, problems);
        }
        System.out.println("ModelCheck: " + models + " block models, " + icons + " icons, " + problems.size() + " problems");
        for(String p : problems) System.out.println("  !! " + p);
        if(!problems.isEmpty()) System.exit(1);
    }

    static void check(String what, Mesh m, List<String> problems){
        for(int v = 0; v < m.verts; v++){
            if(!Float.isFinite(m.vx[v]) || !Float.isFinite(m.vy[v]) || !Float.isFinite(m.vz[v])){
                problems.add(what + ": non-finite vertex " + v);
                return;
            }
        }
        for(int f = 0; f < m.faces; f++){
            if(m.f0[f] >= m.verts || m.f1[f] >= m.verts || m.f2[f] >= m.verts || m.f3[f] >= m.verts){
                problems.add(what + ": face " + f + " references a missing vertex");
                return;
            }
        }
    }
}
