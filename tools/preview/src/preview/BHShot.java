package preview;

import arc.util.*;
import blackhole.*;

import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;

/**
 * Renders the black hole with the real runtime code into a PNG, so the actual frame can be looked at offline
 * (no GL context): v6.1 / v7.2 on the left, the current renderer on the right, same radius, tilt and time.
 *
 * <p>Arguments: {@code <radius> <tilt> <time> <ppu> <out.png>}.
 */
public class BHShot{
    public static void main(String[] args) throws Exception{
        float radius = Float.parseFloat(args[0]), tilt = Float.parseFloat(args[1]), time = Float.parseFloat(args[2]);
        float ppu = Float.parseFloat(args[3]);
        File out = new File(args[4]);
        FakeBatch batch = FakeBatch.install();
        Time.setDeltaProvider(() -> 1f);
        float half = radius * 5.2f;

        batch.reset();
        batch.record = true;
        BlackHoleRenderer61.draw(0f, 0f, radius, tilt, time, 1f);
        long oldQuads = batch.quads;
        BufferedImage left = Anim.composite(batch, half, 0f, ppu, null, null, null, 0f);

        batch.reset();
        batch.record = true;
        BlackHoleRenderer.quadsDrawn = 0;
        BlackHoleRenderer.drawNow(0f, 0f, radius, tilt, time, 1f);
        long newQuads = batch.quads;
        BufferedImage right = Anim.composite(batch, half, 0f, ppu, null, null, null, 0f);

        int w = left.getWidth(), h = left.getHeight();
        BufferedImage img = new BufferedImage(w * 2 + 12, h + 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(8, 8, 12));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.drawImage(left, 0, 30, null);
        g.drawImage(right, w + 12, 30, null);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.drawString("v6.1 / v7.2  -  80 x 14, flat thickness  (" + oldQuads + " quads)", 10, 20);
        g.drawString("v7.3  -  adaptive " + segs(radius) + ", shaded torus, doppler photon ring  (" + newQuads + " quads)", w + 22, 20);
        g.dispose();
        ImageIO.write(img, "png", out);
        // 同半径下的 CPU 成本对比（FakeBatch，不含 GL）
        for(int pass = 0; pass < 2; pass++){
            long t0 = System.nanoTime();
            for(int i = 0; i < 300; i++){ batch.reset(); BlackHoleRenderer61.draw(0f, 0f, radius, tilt, i, 1f); }
            long t1 = System.nanoTime();
            for(int i = 0; i < 300; i++){ batch.reset(); BlackHoleRenderer.drawNow(0f, 0f, radius, tilt, i, 1f); }
            long t2 = System.nanoTime();
            if(pass == 1) System.out.printf("r=%.0f  v6.1 %.3f ms/hole   v7.3 %.3f ms/hole%n", radius, (t1 - t0) / 3e8, (t2 - t1) / 3e8);
        }
        System.out.println("wrote " + out + "  old=" + oldQuads + " quads, new=" + newQuads + " quads");
    }

    static String segs(float radius){
        return radius < 18f ? "48 x 8" : radius < 48f ? "64 x 12" : radius < 130f ? "96 x 12" : "192 x 24";
    }
}
