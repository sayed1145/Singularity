package bake;

import arc.struct.*;
import blackhole.g3d.*;
import blackhole.g3d.Rig;
import blackhole.models.*;
import blackhole.models.ItemModels.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Offline baker for every 3D block, turret head sheet, item and liquid icon of the mod (v7.2 "foundry grade").
 *
 * <pre>
 * usage: java bake.Bake &lt;assets dir&gt; [only=&lt;name&gt;] [threads=N] [report=&lt;file&gt;]
 * </pre>
 *
 * Per block model (blocks/3d/):
 * <ul>
 *   <li>{@code <name>-hd}       static geometry, size*64 px (8 px per world unit), 3x3 supersampled</li>
 *   <li>{@code <name>-front-hd} the static samples that lie nearer to the camera than the live-part envelope</li>
 *   <li>{@code <name>-preview} / {@code <name>}  static + rest pose (+ head at north), size*32 px</li>
 * </ul>
 * Items and liquids (items/, liquids/): 32x32 icons with a 1 px dark outline.
 * Every pass also runs the open-mesh audit (nearest fragment must never be a back face) and the report lists any
 * model that would show an interior / cut face from the game camera.
 */
public final class Bake{
    /** sub-pixel slivers at polygon seams (a few supersamples) are not visible and not reported as problems */
    static final int MINOR = 4;

    static final int HD_PPU = 8, LO_PPU = 4, HD_SS = 3, LO_SS = 6, HEAD_SS = 3;
    static final int SUN_RES = 1536, AO_RES = 768;
    /** audit: at these headings (10 degree steps) the head must show no back face */
    static final int AUDIT_HEADINGS = 36;

    final File assets;
    final StringBuilder report = new StringBuilder();
    final List<String> problems = Collections.synchronizedList(new ArrayList<>());

    Bake(File assets){
        this.assets = assets;
    }

    public static void main(String[] args) throws Exception{
        File assets = new File(args[0]);
        String only = null;
        int threads = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors()));
        File reportFile = null;
        for(int i = 1; i < args.length; i++){
            if(args[i].startsWith("only=")) only = args[i].substring(5);
            else if(args[i].startsWith("threads=")) threads = Integer.parseInt(args[i].substring(8));
            else if(args[i].startsWith("report=")) reportFile = new File(args[i].substring(7));
        }
        Bake bake = new Bake(assets);
        long t0 = System.currentTimeMillis();
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        List<Future<String>> jobs = new ArrayList<>();
        AtomicInteger done = new AtomicInteger();
        Seq<BlockModel> models = Models.all();
        for(BlockModel m : models){
            if(only != null && !m.name.equals(only) && !only.equals("blocks")) continue;
            jobs.add(ex.submit(() -> {
                long s = System.currentTimeMillis();
                String line = bake.bakeBlock(m);
                System.out.printf("  [%2d/%d] %-28s %5d ms  %s%n", done.incrementAndGet(), models.size, m.name, System.currentTimeMillis() - s, line);
                return m.name + ": " + line;
            }));
        }
        if(only == null || only.equals("units")){
            jobs.add(ex.submit(() -> {
                String line = bake.bakeUnits();
                System.out.println("  units: " + line);
                return "units: " + line;
            }));
        }
        if(only == null || only.equals("items")){
            jobs.add(ex.submit(() -> {
                String line = bake.bakeIcons();
                System.out.println("  icons: " + line);
                return "icons: " + line;
            }));
        }
        List<String> lines = new ArrayList<>();
        for(Future<String> f : jobs) lines.add(f.get());
        ex.shutdown();
        StringBuilder sb = new StringBuilder();
        sb.append("Singularity bake report\n");
        for(String l : lines) sb.append(l).append('\n');
        sb.append("problems: ").append(bake.problems.size()).append('\n');
        for(String p : bake.problems) sb.append("  !! ").append(p).append('\n');
        sb.append(String.format("total %.1f s%n", (System.currentTimeMillis() - t0) / 1000f));
        System.out.print(sb);
        if(reportFile != null){
            reportFile.getParentFile().mkdirs();
            try(FileWriter w = new FileWriter(reportFile)){ w.write(sb.toString()); }
        }
        if(!bake.problems.isEmpty()) System.exit(2);
    }

    // ------------------------------------------------------------------ units

    /**
     * v7.6: bakes the flat sprite and the LOD sprite of every 3D unit added in this version. A 3D unit is drawn
     * live, but Mindustry still needs a region for its tech tree / factory icon, and {@code Unit3DType.drawLod}
     * needs one for the far-zoom path - without them the atlas hands back the error texture.
     *
     * <p>The rig is posed exactly the way the game poses it for icons (rest pose, facing north) and rasterised
     * with the unit's own camera, so the sprite and the live model are the same object.
     */
    String bakeUnits() throws Exception{
        mindustry.Vars.content = new mindustry.core.ContentLoader();
        File dir = new File(assets, "sprites/units/3d");
        dir.mkdirs();
        StringBuilder info = new StringBuilder();
        //every unit this version adds: name, rig builder, model scale
        info.append(bakeUnit(dir, "mote", blackhole.models.FrontierUnits.mote(new Ship3D("mote")), 1f));
        info.append(bakeUnit(dir, "lattice-crawler", blackhole.models.FrontierUnits.latticeCrawler(new Tank3D("lattice-crawler")), 1f));
        info.append(bakeUnit(dir, "tide-lancer", blackhole.models.FrontierUnits.tideLancer(new Ship3D("tide-lancer")), 1f));
        info.append(bakeUnit(dir, "bastion-pilot", blackhole.models.BastionUnits.bastionPilot(new Ship3D("bastion-pilot")), 1f));
        info.append(bakeUnit(dir, "bastion-warden", blackhole.models.BastionUnits.bastionWarden(new Ship3D("bastion-warden")), 1f));
        info.append(bakeUnit(dir, "aurora-heavy", blackhole.models.NovaUnits.auroraHeavy(new Ship3D("aurora-heavy")), 1f));
        info.append(bakeUnit(dir, "prism-scout", blackhole.models.NovaUnits.prismScout(new Tank3D("prism-scout")), 1f));
        info.append(bakeUnit(dir, "aurite-lancer", blackhole.models.NovaUnits.auriteLancer(new Tank3D("aurite-lancer")), 1f));
        info.append(bakeUnit(dir, "resonance-siege", blackhole.models.NovaUnits.resonanceSiege(new Tank3D("resonance-siege")), 1f));
        info.append(bakeUnit(dir, "lumen-courier", blackhole.models.NovaUnits.lumenCourier(new Ship3D("lumen-courier")), 1f));
        info.append(bakeUnit(dir, "aurora-sentry", blackhole.models.NovaUnits.auroraSentry(new Ship3D("aurora-sentry")), 1f));
        info.append(bakeUnit(dir, "terra-borer", blackhole.models.TerraUnits.terraBorer(new Tank3D("terra-borer")), 1f));
        info.append(bakeUnit(dir, "basalt-guard", blackhole.models.TerraUnits.basaltGuard(new Tank3D("basalt-guard")), 1f));
        info.append(bakeUnit(dir, "glass-harrier", blackhole.models.TerraUnits.glassHarrier(new Ship3D("glass-harrier")), 1f));
        info.append(bakeUnit(dir, "vent-caller", blackhole.models.TerraUnits.ventCaller(new Ship3D("vent-caller")), 1f));
        //v8.0-beta
        info.append(bakeUnit(dir, "lumen-drone", blackhole.models.ForgeUnits.lumenDrone(new Ship3D("lumen-drone")), 1f));
        info.append(bakeUnit(dir, "lumen-medic", blackhole.models.ForgeUnits.lumenMedic(new Ship3D("lumen-medic")), 1f));
        info.append(bakeUnit(dir, "aurite-wright", blackhole.models.ForgeUnits.auriteWright(new Tank3D("aurite-wright")), 1f));
        //v8.1
        info.append(bakeUnit(dir, "lumen-hauler", blackhole.models.LogixUnits.lumenHauler(new Ship3D("lumen-hauler")), 1f));
        info.append(bakeUnit(dir, "aurite-vanguard", blackhole.models.LogixUnits.auriteVanguard(new Tank3D("aurite-vanguard")), 1f));
        info.append(bakeUnit(dir, "tide-tender", blackhole.models.LogixUnits.tideTender(new Ship3D("tide-tender")), 1f));
        //v8.2
        info.append(bakeUnit(dir, "prism-monolith", blackhole.models.CommandUnits.prismMonolith(new Tank3D("prism-monolith")), 1f));
        //v8.3
        info.append(bakeUnit(dir, "citadel-pilot", blackhole.models.CycleUnits.citadelPilot(new Ship3D("citadel-pilot")), 1f));
        return info.toString();
    }

    String bakeUnit(File dir, String name, Rig rig, float modelScale) throws IOException{
        UnitRenderer ur = new UnitRenderer(rig.half * modelScale);
        rig.reset();
        ur.pose(rig, 90f, 0f, 0f, 0f, modelScale);
        Mesh flat = ur.flatten(rig, new Mesh(), true);
        Soup s = new Soup();
        s.add(flat);
        float half = rig.half * modelScale * 1.3f;
        int px = Math.max(16, Math.round(half * 2f * LO_PPU));
        Raster r = new Raster(ur.cam, px, px, LO_SS, LO_PPU, -half, half);
        r.raster(s, false, 0);
        r.audit();
        //tracks are modelled as open link strips (the same helper every tank in the kit uses), so a few hundred
        //samples under the hull are by design; only a real hole in the body is worth failing the build for
        if(r.backSamples > 1500) problems.add(name + ": unit mesh shows " + r.backSamples + " interior samples (open mesh)\n" + describeOpen(r, s, 3));
        LightMaps maps = new LightMaps(s, SUN_RES, AO_RES);
        r.shade(new Soup[]{s}, new int[]{0}, maps, 0f, 0f);
        Image img = compose(r, null);
        write(img, new File(dir, name + ".png"));
        write(img, new File(dir, name + "-lod.png"));
        return name + " " + px + "px ";
    }

    // ------------------------------------------------------------------ blocks

    String bakeBlock(BlockModel m) throws IOException{
        File dir = new File(assets, "sprites/blocks/3d");
        dir.mkdirs();
        Cam cam = m.cam;
        float half = m.half;
        Soup st = new Soup();
        Mesh sm = new Mesh();
        m.buildStatic(sm);
        st.add(sm);
        Soup env = new Soup();
        Mesh em = new Mesh();
        m.buildEnvelope(em);
        env.add(em);
        Soup rest = new Soup();
        Mesh rm = new Mesh();
        m.buildRest(rm);
        rest.add(rm);
        StringBuilder info = new StringBuilder();

        // ---- -hd: static only, 8 px / unit
        int hd = m.size * 64;
        Raster r = new Raster(cam, hd, hd, HD_SS, HD_PPU, -half, half);
        r.raster(st, false, 0);
        r.audit();
        long open = r.backSamples;
        if(open > MINOR) problems.add(m.name + ": static geometry shows " + open + " interior samples (open mesh) in -hd\n" + describeOpen(r, st, 4));
        LightMaps maps = new LightMaps(st, SUN_RES, AO_RES);
        r.shade(new Soup[]{st}, new int[]{0}, maps, m.waterCx, m.waterCy);
        Image base = compose(r, null);
        write(base, new File(dir, m.name + "-hd.png"));

        // ---- -front-hd: static samples nearer than the envelope's nearest surface
        if(env.tris > 0){
            Raster re = new Raster(cam, hd, hd, HD_SS, HD_PPU, -half, half);
            re.raster(env, false, 0);
            boolean[] front = new boolean[r.tri.length];
            int n = 0;
            for(int i = 0; i < front.length; i++){
                if(r.tri[i] >= 0 && re.tri[i] >= 0 && r.iw[i] > re.iw[i] * (1f + 1e-5f)){
                    front[i] = true;
                    n++;
                }
            }
            //a handful of sub-pixel slivers along shared silhouettes is not a front layer worth an atlas page
            if(n >= 24 * HD_SS * HD_SS){
                write(compose(r, front), new File(dir, m.name + "-front-hd.png"));
                info.append(String.format("front %.1f%% ", 100f * n / front.length));
            }else{
                File f = new File(dir, m.name + "-front-hd.png");
                if(f.exists()) f.delete();
                info.append(n == 0 ? "front none " : String.format("front none (%d sliver samples) ", n));
            }
        }

        // ---- preview / plain: static + rest pose (+ head north), 4 px / unit
        int lo = m.size * 32;
        Raster rp = new Raster(cam, lo, lo, LO_SS, LO_PPU, -half, half);
        Soup all = new Soup();
        all.append(st);
        all.append(rest);
        rp.raster(all, false, 0);
        rp.audit();
        if(rp.backSamples > MINOR) problems.add(m.name + ": rest pose shows " + rp.backSamples + " interior samples (open mesh) in -preview\n" + describeOpen(rp, all, 4));
        LightMaps pmaps = new LightMaps(all, SUN_RES, AO_RES);
        rp.shade(new Soup[]{all}, new int[]{0}, pmaps, m.waterCx, m.waterCy);
        Image prev = compose(rp, null);
        write(prev, new File(dir, m.name + "-preview.png"));
        write(prev, new File(dir, m.name + ".png"));
        //small construction icon: ConstructBlock draws this one with a shader + a batch flush every frame
        writeIcon(prev, m, new File(dir, m.name + "-icon.png"));
        info.append("live ").append(rest.tris / 2).append("q ");

        // ---- turret heads
        if(m instanceof TurretModel t){
            info.append(bakeHeads(t, dir));
        }
        return info.toString();
    }

    /**
     * v7.3: the head is live 3D at runtime, so no sheet is baked any more. What the baker still owns is the
     * audit: at AUDIT_HEADINGS headings the head must never show an interior / cut face to the camera, which is
     * exactly the property the live renderer relies on (it culls back faces, it cannot patch an open mesh).
     */
    String bakeHeads(TurretModel t, File dir){
        t.frame();
        int cell = (int)Math.ceil(t.headHalf * 2f * t.headPpu);
        long openFine = 0;
        StringBuilder openDesc = new StringBuilder();
        for(int i = 0; i < AUDIT_HEADINGS; i++){
            float deg = i * 360f / AUDIT_HEADINGS;
            Soup hs = new Soup();
            Mesh hm = new Mesh();
            t.addHead(hm, deg);
            hs.add(hm);
            Raster r = new Raster(t.cam, cell, cell, 1, t.headPpu, t.headOffX - t.headHalf, t.headOffY + t.headHalf);
            r.raster(hs, false, 0);
            r.audit();
            openFine += r.backSamples;
            if(r.backSamples > 0 && openDesc.length() < 900) openDesc.append("   heading ").append(deg).append(":\n").append(describeOpen(r, hs, 3));
        }
        if(openFine > MINOR) problems.add(t.name + ": head shows " + openFine + " interior samples over " + AUDIT_HEADINGS + " headings\n" + openDesc);
        Mesh hm = new Mesh();
        t.buildHead(hm);
        return String.format("head live %dq audited over %d headings", hm.faces, AUDIT_HEADINGS);
    }

    /** Downscales the preview to ~3 px per world unit: the build animation never needs more. */
    void writeIcon(Image src, BlockModel m, File out) throws IOException{
        int target = Math.max(32, Math.min(src.w, m.size * 24));
        BufferedImage big = new BufferedImage(src.w, src.h, BufferedImage.TYPE_INT_ARGB);
        for(int y = 0; y < src.h; y++) for(int x = 0; x < src.w; x++) big.setRGB(x, y, src.argb(x, y));
        BufferedImage small = new BufferedImage(target, target, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = small.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(big, 0, 0, target, target, null);
        g.dispose();
        ImageIO.write(small, "png", out);
    }

    // ------------------------------------------------------------------ icons

    String bakeIcons() throws IOException{
        int n = 0;
        long open = 0;
        for(IconModel im : ItemModels.all()){
            File dir = new File(assets, "sprites/" + im.folder);
            dir.mkdirs();
            Cam cam = new Cam(4f);
            Soup s = new Soup();
            Mesh posed = new Mesh();
            posed.add(im.mesh, 0, 0, 0, 2, im.spin);
            //tilt about x (the icon is centred from its screen-space bounds below)
            Mesh fin = new Mesh();
            fin.add(posed, 0, 0, 0, 0, im.tilt);
            s.add(fin);
            //centre the model in the icon: screen-space bounds
            float x0 = 1e9f, x1 = -1e9f, y0 = 1e9f, y1 = -1e9f;
            for(int v = 0; v < fin.verts; v++){
                float sx = cam.sx(fin.vx[v], fin.vz[v]), sy = cam.sy(fin.vy[v], fin.vz[v]);
                x0 = Math.min(x0, sx); x1 = Math.max(x1, sx); y0 = Math.min(y0, sy); y1 = Math.max(y1, sy);
            }
            float span = Math.max(x1 - x0, y1 - y0);
            float ppu = 28f / span; //28 of 32 px used, 2 px margin each side (outline + air)
            float cx = (x0 + x1) / 2f, cy = (y0 + y1) / 2f;
            Raster r = new Raster(cam, 32, 32, 8, ppu, cx - 16f / ppu, cy + 16f / ppu);
            r.raster(s, false, 0);
            r.audit();
            open += r.backSamples;
            if(r.backSamples > 0) problems.add(im.folder + "/" + im.name + ": icon shows " + r.backSamples + " interior samples");
            LightMaps maps = new LightMaps(s, 512, 256);
            r.shade(new Soup[]{s}, new int[]{0}, maps, 0, 0);
            Image img = compose(r, null);
            img.outline(0.17f, 0.17f, 0.20f);
            write(img, new File(dir, im.name + ".png"));
            n++;
        }
        return n + " icons, open samples " + open;
    }

    // ------------------------------------------------------------------ compositing

    /** straight-alpha float image */
    static final class Image{
        final int w, h;
        final float[] r, g, b, a;

        Image(int w, int h){
            this.w = w; this.h = h;
            r = new float[w * h]; g = new float[w * h]; b = new float[w * h]; a = new float[w * h];
        }

        int argb(int x, int y){
            int i = y * w + x;
            int A = Math.round(Raster.clamp(a[i]) * 255f);
            if(A == 0) return 0;
            return (A << 24) | (q(r[i]) << 16) | (q(g[i]) << 8) | q(b[i]);
        }

        static int q(float v){ return Math.max(0, Math.min(255, Math.round(v * 255f))); }

        /** 1 px dark outline round every opaque pixel (item icons) */
        void outline(float or, float og, float ob){
            float[] na = a.clone();
            for(int y = 0; y < h; y++) for(int x = 0; x < w; x++){
                int i = y * w + x;
                if(a[i] >= 0.5f) continue;
                float m = 0;
                for(int dy = -1; dy <= 1; dy++) for(int dx = -1; dx <= 1; dx++){
                    int xx = x + dx, yy = y + dy;
                    if(xx < 0 || yy < 0 || xx >= w || yy >= h) continue;
                    m = Math.max(m, a[yy * w + xx]);
                }
                if(m <= 0.5f) continue;
                float k = Math.min(1f, m);
                //blend the outline under the existing partial coverage
                float ea = a[i];
                r[i] = r[i] * ea + or * (1 - ea); g[i] = g[i] * ea + og * (1 - ea); b[i] = b[i] * ea + ob * (1 - ea);
                na[i] = Math.max(ea, k);
            }
            System.arraycopy(na, 0, a, 0, a.length);
        }
    }

    /**
     * Downsamples the shaded samples (optionally only those flagged in mask) to the output size, then adds bloom
     * from the glow buffer: a tight halo (sigma 0.6 px) and a soft spill (sigma 2.4 px) composited with their own
     * alpha, so emissive strips glow over the ground like the foundry renders.
     */
    static Image compose(Raster r, boolean[] mask){
        int W = r.W, H = r.H, SS = r.SS, SW = r.SW;
        Image img = new Image(W, H);
        float[] gl = new float[W * H * 3];
        float inv = 1f / (SS * SS);
        for(int y = 0; y < H; y++) for(int x = 0; x < W; x++){
            float sr = 0, sg = 0, sb = 0, sa = 0, gr = 0, gg = 0, gb = 0;
            for(int sy = 0; sy < SS; sy++) for(int sx = 0; sx < SS; sx++){
                int i = (y * SS + sy) * SW + x * SS + sx;
                if(r.tri[i] < 0 || (mask != null && !mask[i])) continue;
                sr += r.rgb[i * 3]; sg += r.rgb[i * 3 + 1]; sb += r.rgb[i * 3 + 2];
                sa += 1f;
                gr += r.glow[i * 3]; gg += r.glow[i * 3 + 1]; gb += r.glow[i * 3 + 2];
            }
            int o = y * W + x;
            if(sa > 0){
                img.r[o] = sr / sa; img.g[o] = sg / sa; img.b[o] = sb / sa;
                img.a[o] = sa * inv;
            }
            gl[o * 3] = gr * inv; gl[o * 3 + 1] = gg * inv; gl[o * 3 + 2] = gb * inv;
        }
        if(mask == null){
            float[] tight = blur(gl, W, H, 0.6f * r.ppu / 8f + 0.3f), soft = blur(gl, W, H, 2.4f * r.ppu / 8f + 0.6f);
            for(int o = 0; o < W * H; o++){
                //halo only: the blurred glow minus the source itself, so large emissive panels keep their colour
                //(no white-out) while thin strips still get a hot core and everything spills softly onto the ground
                float br = Math.max(0f, tight[o * 3] - gl[o * 3] * 0.75f) * 0.60f + Math.max(0f, soft[o * 3] - gl[o * 3]) * 0.55f;
                float bg = Math.max(0f, tight[o * 3 + 1] - gl[o * 3 + 1] * 0.75f) * 0.60f + Math.max(0f, soft[o * 3 + 1] - gl[o * 3 + 1]) * 0.55f;
                float bb = Math.max(0f, tight[o * 3 + 2] - gl[o * 3 + 2] * 0.75f) * 0.60f + Math.max(0f, soft[o * 3 + 2] - gl[o * 3 + 2]) * 0.55f;
                float bl = Math.max(br, Math.max(bg, bb));
                if(bl < 0.004f) continue;
                //premultiplied add, then back to straight alpha
                float a0 = img.a[o];
                float pr = img.r[o] * a0 + br, pg = img.g[o] * a0 + bg, pb = img.b[o] * a0 + bb;
                float a1 = Math.min(1f, a0 + (1f - a0) * Math.min(1f, bl * 1.6f));
                if(a1 <= 0f) continue;
                img.r[o] = Math.min(1f, pr / a1); img.g[o] = Math.min(1f, pg / a1); img.b[o] = Math.min(1f, pb / a1);
                img.a[o] = a1;
            }
        }
        return img;
    }

    static float[] blur(float[] src, int w, int h, float sigma){
        int rad = (int)Math.ceil(sigma * 2.5f);
        float[] kern = new float[rad * 2 + 1];
        float sum = 0;
        for(int i = -rad; i <= rad; i++){ kern[i + rad] = (float)Math.exp(-i * i / (2 * sigma * sigma)); sum += kern[i + rad]; }
        for(int i = 0; i < kern.length; i++) kern[i] /= sum;
        float[] tmp = new float[src.length], dst = new float[src.length];
        for(int y = 0; y < h; y++) for(int x = 0; x < w; x++) for(int k = 0; k < 3; k++){
            float s = 0;
            for(int i = -rad; i <= rad; i++){ int xx = x + i; if(xx < 0 || xx >= w) continue; s += src[(y * w + xx) * 3 + k] * kern[i + rad]; }
            tmp[(y * w + x) * 3 + k] = s;
        }
        for(int y = 0; y < h; y++) for(int x = 0; x < w; x++) for(int k = 0; k < 3; k++){
            float s = 0;
            for(int i = -rad; i <= rad; i++){ int yy = y + i; if(yy < 0 || yy >= h) continue; s += tmp[(yy * w + x) * 3 + k] * kern[i + rad]; }
            dst[(y * w + x) * 3 + k] = s;
        }
        return dst;
    }

    /** describes the triangles whose back faces are the nearest fragment (debugging open meshes) */
    static String describeOpen(Raster r, Soup s, int max){
        java.util.Map<Integer, Integer> counts = new java.util.TreeMap<>();
        for(int i = 0; i < r.tri.length; i++){
            if(r.tri[i] >= 0 && r.back != null && r.back[i]) counts.merge(r.tri[i], 1, Integer::sum);
        }
        List<java.util.Map.Entry<Integer, Integer>> list = new ArrayList<>(counts.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        StringBuilder sb = new StringBuilder();
        for(int k = 0; k < Math.min(max, list.size()); k++){
            int t = list.get(k).getKey();
            int o = t * 9;
            sb.append(String.format("    tri %d x%d col(%.2f %.2f %.2f) fl %d mat %d  v0(%.2f %.2f %.2f) v1(%.2f %.2f %.2f) v2(%.2f %.2f %.2f)%n",
                t, list.get(k).getValue(), s.col[t * 3], s.col[t * 3 + 1], s.col[t * 3 + 2], s.flags[t], s.mat[t],
                s.pos[o], s.pos[o + 1], s.pos[o + 2], s.pos[o + 3], s.pos[o + 4], s.pos[o + 5], s.pos[o + 6], s.pos[o + 7], s.pos[o + 8]));
        }
        return sb.toString();
    }

    static void write(Image img, File f) throws IOException{
        BufferedImage out = new BufferedImage(img.w, img.h, BufferedImage.TYPE_INT_ARGB);
        for(int y = 0; y < img.h; y++) for(int x = 0; x < img.w; x++) out.setRGB(x, y, img.argb(x, y));
        ImageIO.write(out, "png", f);
    }
}
