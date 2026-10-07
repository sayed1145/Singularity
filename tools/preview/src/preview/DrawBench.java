package preview;

import arc.util.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.gen.*;

/**
 * Headless per-model draw cost: runs every block model's full draw path (baked layers + live parts + overlays)
 * against the recording batch for a bare Building and reports quads / microseconds per draw, so the live budget
 * numbers and any per-frame hot spot can be checked without a GL context.
 */
public class DrawBench{
    public static void main(String[] args){
        FakeBatch b = FakeBatch.install();
        b.record = false;
        Models.load();
        Building bb = Building.create();
        bb.x = 0; bb.y = 0;
        Time.setDeltaProvider(() -> 1f);
        System.out.printf("%-30s %8s %10s %8s%n", "model", "quads", "us/draw", "liveCost");
        double totalUs = 0;
        for(BlockModel m : Models.all()){
            m.load();
            try{
                for(int i = 0; i < 20; i++) m.draw(bb); //warm-up
                Live.quadsDrawn = 0;
                b.reset();
                int n = 200;
                long t0 = System.nanoTime();
                for(int i = 0; i < n; i++){
                    Time.time += 1f;
                    m.draw(bb);
                }
                long t1 = System.nanoTime();
                double us = (t1 - t0) / 1e3 / n;
                totalUs += us;
                System.out.printf("%-30s %8d %10.1f %8d%n", m.name, Live.quadsDrawn / n, us, m.liveCost);
            }catch(Throwable t){
                System.out.printf("%-30s   draw threw %s%n", m.name, t);
            }
        }
        System.out.printf("sum of one draw of every model: %.2f ms (fake batch, CPU only)%n", totalUs / 1000.0);
    }
}
