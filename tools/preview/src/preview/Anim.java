package preview;

import arc.util.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.gen.*;

import javax.imageio.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.*;

/**
 * Offline animation frames (PNG sequence, turned into GIFs by tools/gif.py) produced by the very same drawing code
 * the game runs, against a recording batch instead of the GPU:
 * <ul>
 * <li>{@code batch <model> <frames> <out> [ppu] [pad]} - the real {@code BlockModel.draw(Building)} of a running,
 * warmed-up building: its quads (live parts, glows, beams, the black hole) are composited over the baked
 * {@code -hd} / under the {@code -front-hd} sprites in draw order, including additive blending. For the
 * singularity forge a {@code lens.txt} sidecar carries the inputs of the v6.1 lens shader (gif.py applies it).</li>
 * <li>{@code turret <model> <frames> <out> [ppu] [pad]} - the baked base plus the real {@link TurretModel#drawHead}
 * (nearest baked heading + residual rotation + recoil) sweeping a full turn.</li>
 * <li>{@code sheet <turrets|blocks> <frames> <out> [ppu] [pad]} - all turrets / all other blocks on one grid.</li>
 * </ul>
 * Arguments: {@code <assetsDir> <mode> <model|sheet kind> <frames> <outDir> [ppu=6] [pad=4 world units]}.
 */
public class Anim{
    public static void main(String[] args) throws Exception{
        String assets = args[0], mode = args[1], name = args[2];
        int frames = Integer.parseInt(args[3]);
        File out = new File(args[4]);
        float ppu = args.length > 5 ? Float.parseFloat(args[5]) : 6f;
        out.mkdirs();
        FakeBatch batch = FakeBatch.install();
        Models.load();
        BlockModel model = null;
        for(BlockModel m : Models.all()) if(m.name.equals(name)) model = m;
        if(model == null && !mode.equals("sheet")) throw new IllegalArgumentException("no model " + name);
        Time.setDeltaProvider(() -> 1f);
        float pad = args.length > 6 ? Float.parseFloat(args[6]) : 4f;
        switch(mode){
            case "batch" -> batchFrames(batch, model, assets, frames, out, ppu, pad);
            case "turret" -> turret(batch, (TurretModel)model, assets, frames, out, ppu, pad);
            case "sheet" -> sheet(batch, name, assets, frames, out, ppu, pad);
            //v7.4 proof shot: the drill plus the procedural ore indicator that replaced the "oh no" region
            case "drill" -> drillFrames(batch, model, assets, frames, out, ppu, pad);
            default -> throw new IllegalArgumentException(mode);
        }
        System.out.println("frames written to " + out);
    }

    // ------------------------------------------------------------------ batch mode

    static BufferedImage sprite(String assets, String name){
        File f = new File(assets, "sprites/blocks/3d/" + name + ".png");
        if(!f.isFile()) return null;
        try{
            return ImageIO.read(f);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    static final float LOOP = 240f; //ticks per animation loop (4 s)

    static void batchFrames(FakeBatch batch, BlockModel model, String assets, int frames, File out, float ppu, float pad) throws IOException{
        Building b = new PreviewBuilding();
        model.load();
        model.draw(b); //warm-up / lazy costs
        lensSidecar(model, out, ppu, pad, frames);
        for(int i = 0; i < frames; i++){
            Time.time = i * (LOOP / frames);
            BufferedImage img = blockFrame(batch, model, b, assets, ppu, pad);
            ImageIO.write(img, "png", new File(out, String.format("%03d.png", i)));
        }
    }

    /** v7.4: the drill as the game draws it - model plus {@link blackhole.models.Drill3D#drawOre} on top, no atlas lookup. */
    static void drillFrames(FakeBatch batch, BlockModel model, String assets, int frames, File out, float ppu, float pad) throws IOException{
        Building b = new PreviewBuilding();
        model.load();
        model.draw(b);
        int size = Math.max(1, Math.round(model.half / 4f));
        arc.graphics.Color ore = arc.graphics.Color.valueOf("ffd27a"); //lumenite
        for(int i = 0; i < frames; i++){
            Time.time = i * (LOOP / frames);
            BufferedImage base = sprite(assets, model.name + "-hd"), front = sprite(assets, model.name + "-front-hd");
            batch.reset();
            batch.record = true;
            Budget.frame = -1;
            model.draw(b);
            blackhole.models.Drill3D.drawOre(0f, 0f, size, ore);
            ImageIO.write(composite(batch, model.half, pad, ppu, base, front, null, 0f), "png", new File(out, String.format("%03d.png", i)));
        }
    }

    /** one in-game frame of a block model at the current Time.time: baked sprites + recorded live quads */
    static BufferedImage blockFrame(FakeBatch batch, BlockModel model, Building b, String assets, float ppu, float pad){
        BufferedImage base = sprite(assets, model.name + "-hd"), front = sprite(assets, model.name + "-front-hd");
        batch.reset();
        batch.record = true;
        Budget.frame = -1;
        model.draw(b);
        return composite(batch, model.half, pad, ppu, base, front, null, 0f);
    }

    /**
     * The forge bends the frame through the v6.1 full-screen lens shader, which has no headless equivalent: write
     * the shader inputs (hole position in image pixels, radius, strength, time per frame) so tools/gif.py can apply
     * the identical formula offline.
     */
    static void lensSidecar(BlockModel model, File out, float ppu, float pad, int frames){
        if(!(model instanceof BlockModels.SingularityForge)) return;
        try{
            java.lang.reflect.Field fz = model.getClass().getDeclaredField("hz");
            fz.setAccessible(true);
            float hz = fz.getFloat(model);
            float hx = model.cam.sx(0, hz), hy = model.cam.sy(0, hz);
            try(PrintWriter w = new PrintWriter(new File(out, "lens.txt"))){
                w.printf(Locale.ROOT, "%f %f %f %f %f %f %d%n", hx, hy, 1.5f, BlockModels.SingularityForge.lensStrength, ppu, model.half + pad, frames);
                w.printf(Locale.ROOT, "%f%n", LOOP);
            }
        }catch(ReflectiveOperationException | IOException e){
            throw new RuntimeException(e);
        }
    }

    // ------------------------------------------------------------------ turret mode

    static void turret(FakeBatch batch, TurretModel t, String assets, int frames, File out, float ppu, float pad) throws IOException{
        t.load();
        t.loadHeads();
        t.frame();
        for(int i = 0; i < frames; i++){
            ImageIO.write(turretFrame(batch, t, assets, i, frames, ppu, pad), "png", new File(out, String.format("%03d.png", i)));
        }
    }

    /** frame i of a full clockwise turn starting north, with a short recoil kick every quarter turn */
    static BufferedImage turretFrame(FakeBatch batch, TurretModel t, String assets, int i, int frames, float ppu, float pad){
        BufferedImage base = sprite(assets, t.name + "-hd");
        float rot = 90f - i * 360f / frames;
        int q = Math.max(1, frames / 4);
        float recoil = i % q < 3 ? 1.2f * (3 - i % q) / 3f : 0f;
        batch.reset();
        batch.record = true;
        t.drawBase(0f, 0f);
        //v7.3: the head is live geometry, it arrives as quads in the batch - no sheet, any heading exact
        t.drawHead(0f, 0f, rot, recoil);
        return composite(batch, t.half, pad, ppu, base, null, null, t.headHalf);
    }

    // ------------------------------------------------------------------ sheet mode

    /** every turret (sweeping) or every other block (running) on one grid, one PNG per frame */
    static void sheet(FakeBatch batch, String which, String assets, int frames, File out, float ppu, float pad) throws IOException{
        boolean turrets = which.equals("turrets");
        List<BlockModel> list = new ArrayList<>();
        for(BlockModel m : Models.all()) if((m instanceof TurretModel) == turrets) list.add(m);
        list.sort(Comparator.comparing(m -> m.name));
        int cellPx = Math.round((5 * 8 + 2 * pad) * ppu);
        int cols = turrets ? 7 : 7, rows = (list.size() + cols - 1) / cols;
        Building b = new PreviewBuilding();
        for(BlockModel m : list){
            m.load();
            if(m instanceof TurretModel t){ t.loadHeads(); t.frame(); }
            else m.draw(b);
        }
        for(int i = 0; i < frames; i++){
            Time.time = i * (LOOP / frames);
            BufferedImage canvas = new BufferedImage(cols * cellPx, rows * cellPx, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            for(int n = 0; n < list.size(); n++){
                BlockModel m = list.get(n);
                BufferedImage img = m instanceof TurretModel t ? turretFrame(batch, t, assets, i, frames, ppu, pad) : blockFrame(batch, m, b, assets, ppu, pad);
                int cx = (n % cols) * cellPx + cellPx / 2, cy = (n / cols) * cellPx + cellPx / 2;
                g.drawImage(img, cx - img.getWidth() / 2, cy - img.getHeight() / 2, null);
            }
            g.dispose();
            ImageIO.write(canvas, "png", new File(out, String.format("%03d.png", i)));
        }
        try(PrintWriter w = new PrintWriter(new File(out, "names.txt"))){
            for(BlockModel m : list) w.println(m.name);
            w.println("cols " + cols + " cell " + cellPx);
        }
    }

    /** a running, fully warmed-up building (plain Buildings report warmup 0) */
    static final class PreviewBuilding extends Building{
        @Override public float warmup(){ return 1f; }
        @Override public float totalProgress(){ return Time.time; }
        @Override public float progress(){ return (Time.time % 240f) / 240f; }
    }

    // ------------------------------------------------------------------ compositing

    /**
     * Replays the recorded primitives: region draws of the block size become the baked base / front sprites, a
     * region draw of the head size becomes the head cell (rotated by the residual), quads are gouraud filled with
     * normal or additive blending.
     */
    static BufferedImage composite(FakeBatch batch, float half, float pad, float ppu, BufferedImage base, BufferedImage front, BufferedImage head, float headHalf){
        int px = Math.round((half + pad) * 2 * ppu);
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int regionDraws = 0, o = 0;
        float[] v = batch.verts;
        half += pad; //image origin
        Painter painter = new Painter(img, half, ppu);
        for(int n = 0; n < batch.kinds.size; n++){
            int kind = batch.kinds.get(n), blend = batch.blends.get(n);
            if(kind == 1){
                //Draw.rect hands the batch the bottom-left corner (origin 0 when unrotated, half size when rotated)
                float w = v[o + 4], h = v[o + 5], x = v[o] + w / 2f, y = v[o + 1] + h / 2f, rot = v[o + 6];
                o += 8;
                BufferedImage src;
                if(head != null && Math.abs(w - headHalf * 2f) < 1e-3f) src = head;
                else src = regionDraws++ == 0 ? base : front;
                if(src == null) continue;
                float cx = (x + half) * ppu, cy = (half - y) * ppu;
                AffineTransform at = new AffineTransform();
                at.translate(cx, cy);
                at.rotate(Math.toRadians(-rot));
                at.scale(w * ppu / src.getWidth(), h * ppu / src.getHeight());
                at.translate(-src.getWidth() / 2.0, -src.getHeight() / 2.0);
                g.drawImage(src, at, null);
            }else{
                painter.quad(v, o, blend == 1);
                o += 24;
            }
        }
        g.dispose();
        return img;
    }

    /** Minimal gouraud quad filler on an ARGB image (straight alpha), normal or additive blending. */
    static final class Painter{
        final BufferedImage img;
        final float half, ppu;
        final int w, h;

        Painter(BufferedImage img, float half, float ppu){
            this.img = img; this.half = half; this.ppu = ppu;
            w = img.getWidth(); h = img.getHeight();
        }

        void quad(float[] v, int o, boolean additive){
            //arc vertex layout: x, y, colour(float bits), u, v, mixColour(float bits) - 6 floats per vertex
            float[][] p = new float[4][];
            for(int k = 0; k < 4; k++){
                int q = o + k * 6;
                int c = Float.floatToRawIntBits(v[q + 2]);
                //Color.toFloatBits packs ABGR with the top bit masked; recover channels
                float r = (c & 0xff) / 255f, gg = ((c >>> 8) & 0xff) / 255f, b = ((c >>> 16) & 0xff) / 255f, a = ((c >>> 24) & 0xfe) / 254f;
                p[k] = new float[]{(v[q] + half) * ppu, (half - v[q + 1]) * ppu, r, gg, b, a};
            }
            tri(p[0], p[1], p[2], additive);
            tri(p[0], p[2], p[3], additive);
        }

        void tri(float[] a, float[] b, float[] c, boolean additive){
            float area = (b[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (b[1] - a[1]);
            if(Math.abs(area) < 1e-6f) return;
            int x0 = Math.max(0, (int)Math.floor(Math.min(a[0], Math.min(b[0], c[0])))), x1 = Math.min(w - 1, (int)Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
            int y0 = Math.max(0, (int)Math.floor(Math.min(a[1], Math.min(b[1], c[1])))), y1 = Math.min(h - 1, (int)Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
            float inv = 1f / area;
            final int SS = 2;
            for(int y = y0; y <= y1; y++) for(int x = x0; x <= x1; x++){
                float cov = 0, sr = 0, sg = 0, sb = 0, sa = 0;
                for(int sy = 0; sy < SS; sy++) for(int sx = 0; sx < SS; sx++){
                    float X = x + (sx + 0.5f) / SS, Y = y + (sy + 0.5f) / SS;
                    float l0 = ((b[0] - X) * (c[1] - Y) - (c[0] - X) * (b[1] - Y)) * inv;
                    float l1 = ((c[0] - X) * (a[1] - Y) - (a[0] - X) * (c[1] - Y)) * inv;
                    float l2 = 1f - l0 - l1;
                    if(l0 < 0 || l1 < 0 || l2 < 0) continue;
                    cov += 1f;
                    sr += l0 * a[2] + l1 * b[2] + l2 * c[2]; sg += l0 * a[3] + l1 * b[3] + l2 * c[3];
                    sb += l0 * a[4] + l1 * b[4] + l2 * c[4]; sa += l0 * a[5] + l1 * b[5] + l2 * c[5];
                }
                if(cov == 0) continue;
                float k = cov / (SS * SS);
                float r = sr / cov, gg = sg / cov, bb = sb / cov, al = sa / cov * k;
                int d = img.getRGB(x, y);
                float da = ((d >>> 24) & 0xff) / 255f, dr = ((d >>> 16) & 0xff) / 255f, dg = ((d >>> 8) & 0xff) / 255f, db = (d & 0xff) / 255f;
                float nr, ng, nb, na;
                if(additive){
                    na = da;
                    nr = Math.min(1f, dr + r * al); ng = Math.min(1f, dg + gg * al); nb = Math.min(1f, db + bb * al);
                    if(da < 1f){ //glow over transparent background: fade in with its own alpha
                        na = Math.min(1f, da + al * Math.max(r, Math.max(gg, bb)));
                    }
                }else{
                    na = al + da * (1f - al);
                    if(na <= 0f) continue;
                    nr = (r * al + dr * da * (1f - al)) / na; ng = (gg * al + dg * da * (1f - al)) / na; nb = (bb * al + db * da * (1f - al)) / na;
                }
                img.setRGB(x, y, (Math.round(na * 255) << 24) | (Math.round(Math.min(1f, nr) * 255) << 16) | (Math.round(Math.min(1f, ng) * 255) << 8) | Math.round(Math.min(1f, nb) * 255));
            }
        }
    }
}
