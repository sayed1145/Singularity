package preview;

import blackhole.*;

public class BHBench{
    public static void main(String[] args){
        FakeBatch b = FakeBatch.install();
        b.record = false;
        for(int round = 0; round < 3; round++){
            long t0 = System.nanoTime();
            for(int i = 0; i < 300; i++) BlackHoleRenderer61.draw(100, 50, 4f, 0.72f, i * 0.7f, 1f);
            long t1 = System.nanoTime();
            for(int i = 0; i < 300; i++) BlackHoleRenderer.draw(100, 50, 4f, 0.72f, i * 0.7f, 1f);
            long t2 = System.nanoTime();
            System.out.printf("round %d: v6.1 %.3f ms/hole   v7.2 %.3f ms/hole  (CPU only, fake batch)%n", round, (t1 - t0) / 300e6, (t2 - t1) / 300e6);
        }
    }
}
