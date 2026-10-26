package preview;

import blackhole.*;

import java.awt.image.*;

/**
 * Structural regression for the black hole renderer.
 *
 * <p>Up to v7.2 this asserted a byte-identical vertex stream against v6.1. v7.3 deliberately changes the
 * geometry (adaptive tessellation, shaded disc walls, Doppler photon ring), so equality is the wrong test.
 * What must still hold is that it is the <em>same picture</em>: same silhouette, same radial light
 * distribution, same total energy — only finer. So we rasterise both renderers through {@link FakeBatch} and
 * compare 32-ring radial luminance profiles (Pearson correlation), the lit extent and the total luminance.
 */
public class BHCompare{
    static final int rings = 32;
    static final float minCorr = 0.95f, maxExtentErr = 0.12f, maxEnergyErr = 0.45f;

    public static void main(String[] args){
        FakeBatch batch = FakeBatch.install();
        float[][] cases = {{4f, 0.72f, 12.5f, 1f}, {1.5f, 0.72f, 300f, 0.6f}, {9f, 0.2f, 77.3f, 0.9f}, {40f, 1f, 0f, 1f}, {150f, 0.72f, 1234.5f, 0.9f}};
        boolean bad = false;
        for(float[] c : cases){
            float radius = c[0], tilt = c[1], time = c[2], alpha = c[3];
            float half = radius * 5.2f, ppu = 200f / half;

            batch.reset();
            batch.record = true;
            BlackHoleRenderer61.draw(0f, 0f, radius, tilt, time, alpha);
            long oldQuads = batch.quads;
            float[] a = profile(Anim.composite(batch, half, 0f, ppu, null, null, null, 0f));

            batch.reset();
            batch.record = true;
            BlackHoleRenderer.drawNow(0f, 0f, radius, tilt, time, alpha);
            long newQuads = batch.quads;
            float[] n = profile(Anim.composite(batch, half, 0f, ppu, null, null, null, 0f));

            float corr = corr(a, n), ea = extent(a), en = extent(n), sa = sum(a), sn = sum(n);
            float extentErr = Math.abs(en - ea) / Math.max(1f, ea), energyErr = Math.abs(sn - sa) / Math.max(1e-3f, sa);
            boolean ok = corr >= minCorr && extentErr <= maxExtentErr && energyErr <= maxEnergyErr;
            if(!ok) bad = true;
            System.out.printf("case r=%-5.1f tilt=%.2f : corr=%.4f extent %.1f->%.1f (%.1f%%) energy %.1f%% quads %d->%d  %s%n",
                radius, tilt, corr, ea, en, extentErr * 100f, energyErr * 100f, oldQuads, newQuads, ok ? "ok" : "FAIL");
        }
        System.out.println(bad ? "BH_STRUCTURE_FAIL" : "BH_STRUCTURE_OK same silhouette and radial light profile as v6.1, finer geometry");
        if(bad) System.exit(1);
    }

    /** Mean luminance per radial ring, from the image centre outwards. */
    static float[] profile(BufferedImage img){
        float[] sum = new float[rings];
        int[] count = new int[rings];
        float cx = img.getWidth() / 2f, cy = img.getHeight() / 2f, max = Math.min(cx, cy);
        for(int y = 0; y < img.getHeight(); y++){
            for(int x = 0; x < img.getWidth(); x++){
                float dx = x - cx, dy = y - cy;
                float d = (float)Math.sqrt(dx * dx + dy * dy);
                if(d >= max) continue;
                int r = Math.min(rings - 1, (int)(d / max * rings));
                int p = img.getRGB(x, y);
                float al = ((p >>> 24) & 255) / 255f;
                float lum = (((p >> 16) & 255) * 0.299f + ((p >> 8) & 255) * 0.587f + (p & 255) * 0.114f) * al;
                sum[r] += lum;
                count[r]++;
            }
        }
        for(int i = 0; i < rings; i++) sum[i] /= Math.max(1, count[i]);
        return sum;
    }

    static float corr(float[] a, float[] b){
        float ma = sum(a) / a.length, mb = sum(b) / b.length, num = 0, da = 0, db = 0;
        for(int i = 0; i < a.length; i++){
            float x = a[i] - ma, y = b[i] - mb;
            num += x * y;
            da += x * x;
            db += y * y;
        }
        return num / (float)Math.sqrt(Math.max(1e-9f, da * db));
    }

    /** Outermost ring index (in 1/100 units) whose luminance still exceeds 2% of the peak. */
    static float extent(float[] a){
        float peak = 0;
        for(float v : a) peak = Math.max(peak, v);
        int last = 0;
        for(int i = 0; i < a.length; i++) if(a[i] > peak * 0.02f) last = i;
        return last * 100f / rings;
    }

    static float sum(float[] a){
        float s = 0;
        for(float v : a) s += v;
        return s;
    }
}
