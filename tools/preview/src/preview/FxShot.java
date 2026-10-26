package preview;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import blackhole.*;
import mindustry.entities.Effect;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * v8.5.5：把特效<b>画出来</b>看，而不是只在表上说小了多少。
 *
 * <p>和 {@link FxSize} 一样走 {@link FakeBatch}（无 GL），但这次是把录到的三角形
 * 用 {@link Anim.Painter}（和 {@link Anim} 生成动画帧用的是同一套填充代码）光栅化到 PNG 上。
 * 每张图都带同心圆比例尺：一圈 = 1 格（8 像素）。
 *
 * <p>产出：
 * <ul>
 *   <li>{@code fx-crawler.png}     —— 8.5.5 晶格蠕者四件套的时间展开</li>
 *   <li>{@code fx-marrow.png}      —— 8.5.5 星髓石炮弹六件套</li>
 *   <li>{@code fx-compare.png}     —— <b>同一比例尺</b>下 8.5.1 / 8.5.4 / 8.5.5 落点爆炸对照</li>
 * </ul>
 *
 * <p>用法：{@code java preview.FxShot <outDir>}
 */
public class FxShot{
    static final float ROT = 259f;   //故意挑一个"不正"的朝向：旧特效在这个角度下最大

    public static void main(String[] args) throws Exception{
        File out = new File(args[0]);
        out.mkdirs();
        FakeBatch b = FakeBatch.install();

        strip(b, "fx-crawler.png", out, 34f,
            row("炮口 CrawlerFx.muzzle", CrawlerFx.muzzle),
            row("硝烟 CrawlerFx.smoke", CrawlerFx.smoke),
            row("落点 CrawlerFx.impact", CrawlerFx.impact),
            row("破片 CrawlerFx.shardHit", CrawlerFx.shardHit));

        strip(b, "fx-marrow.png", out, 40f,
            row("炮口 MarrowFx.shoot", MarrowFx.shoot),
            row("弹道尾迹 MarrowFx.wake", MarrowFx.wake),
            row("命中 MarrowFx.hit", MarrowFx.hit),
            row("射程尽头 MarrowFx.despawn", MarrowFx.despawn),
            row("灼印（持续）MarrowFx.brand", MarrowFx.brand),
            row("灼印（附上）MarrowFx.brandApply", MarrowFx.brandApply));

        //同一比例尺的对照 —— 这张才是"真的变小了"的证据
        strip(b, "fx-compare.png", out, 150f,
            row("8.5.1 detainImpact", AureliaFx.detainImpact),
            row("8.5.4 detainImpactSmall（缩 1/3）", AureliaFx.detainImpactSmall),
            row("8.5.5 CrawlerFx.impact（重写）", CrawlerFx.impact));

        strip(b, "fx-quasar.png", out, 40f,
            row("类星体喷流枪口 AureliaFx.quasarMuzzle（v8.5.5 新增）", AureliaFx.quasarMuzzle));

        System.out.println("shots written to " + out);
    }

    /** 上一格刚量出来的峰值半径（世界像素）。 */
    static float measured = 0f;

    /**
     * 把一次区域绘制（Fill.* 走的就是这条路）画成一个四边形。
     *
     * <p>绕序不能瞎猜 —— {@link Anim.Painter#tri} 用的是带符号面积，符号反了整块都会被判成"在三角形外"而丢弃。
     * 所以先按一个顺序算一次面积，负了就把四个角倒过来。
     */
    static void rect(Anim.Painter p, float x, float y, float ox, float oy, float w, float h, float rotDeg,
                     int bits, boolean additive, float half, float ppu){
        //锚点：origin 为 0 时 x/y 就是左下角，否则 x/y 是中心
        float bx = x - ox, by = y - oy;
        float[] xs = {bx, bx + w, bx + w, bx}, ys = {by, by, by + h, by + h};
        if(rotDeg != 0f){
            float rad = rotDeg * (float)Math.PI / 180f, c = (float)Math.cos(rad), sn = (float)Math.sin(rad);
            for(int k = 0; k < 4; k++){
                float dx = xs[k] - x, dy = ys[k] - y;
                xs[k] = x + dx * c - dy * sn;
                ys[k] = y + dx * sn + dy * c;
            }
        }
        if(signedArea(xs[0], ys[0], xs[1], ys[1], xs[2], ys[2], half, ppu) < 0f){
            //reverse: 0,3,2,1
            float tx = xs[1], ty = ys[1];
            xs[1] = xs[3]; ys[1] = ys[3];
            xs[3] = tx;    ys[3] = ty;
        }
        float[] q = new float[24];
        float col = Float.intBitsToFloat(bits);
        for(int k = 0; k < 4; k++){
            q[k * 6] = xs[k]; q[k * 6 + 1] = ys[k]; q[k * 6 + 2] = col;
        }
        p.quad(q, 0, additive);
    }

    /** 和 {@link Anim.Painter#tri} 完全一样的带符号面积（同样把 +y 翻成屏幕的向下）。 */
    static float signedArea(float x0, float y0, float x1, float y1, float x2, float y2, float half, float ppu){
        float X0 = (x0 + half) * ppu, Y0 = (half - y0) * ppu;
        float X1 = (x1 + half) * ppu, Y1 = (half - y1) * ppu;
        float X2 = (x2 + half) * ppu, Y2 = (half - y2) * ppu;
        return (X1 - X0) * (Y2 - Y0) - (X2 - X0) * (Y1 - Y0);
    }

    /** 扫描一张刚画完、还没画比例尺的格子图，返回离中心最远的"点亮"像素的距离（世界像素）。 */
    static float litRadius(BufferedImage img, float ppu){
        int w = img.getWidth(), h = img.getHeight();
        int bg = 0x14121c;
        float best = 0f;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                int p = img.getRGB(x, y);
                if(((p >>> 24) & 0xff) < 8) continue;                       //全透明 = 没画东西
                int dr = ((p >>> 16) & 0xff) - ((bg >> 16) & 0xff);
                int dg = ((p >>> 8) & 0xff) - ((bg >> 8) & 0xff);
                int db = (p & 0xff) - (bg & 0xff);
                if(Math.abs(dr) + Math.abs(dg) + Math.abs(db) < 6) continue; //和背景几乎一样（茸毛级的淡光也要算进去）
                float d = (float)Math.hypot(x - w / 2f, y - h / 2f) / ppu;
                if(d > best) best = d;
            }
        }
        return best;
    }

    static Row row(String name, Effect fx){ return new Row(name, fx); }
    record Row(String name, Effect fx){}

    /** 每种特效一行，每行 5 帧（0%、25%、50%、75%、100% 寿命）。 */
    static void strip(FakeBatch b, String filename, File out, float half, Row... rows) throws Exception{
        int frames = 5, cell = 190, labelW = 300;
        BufferedImage img = new BufferedImage(labelW + frames * cell, rows.length * cell, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new java.awt.Color(0x14121c));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));

        for(int r = 0; r < rows.length; r++){
            Row row = rows[r];
            float rowPeak = 0f;
            for(int f = 0; f < frames; f++){
                float t = row.fx().lifetime * f / (frames - 1f);
                BufferedImage cellImg = render(b, row.fx(), t, half, cell);
                rowPeak = Math.max(rowPeak, measured);
                if(f == frames - 1) System.out.printf("  %-46s 实测峰值半径 %7.1f px = %6.2f 格%n", row.name(), rowPeak, rowPeak / 8f);
                g.drawImage(cellImg, labelW + f * cell, r * cell, null);
                g.setColor(new java.awt.Color(0x2a2735));
                g.drawRect(labelW + f * cell, r * cell, cell - 1, cell - 1);
            }
            g.setColor(new java.awt.Color(0xd8d4e8));
            g.drawString(row.name(), 10, r * cell + cell / 2 - 4);
            g.setColor(new java.awt.Color(0x9c96b8));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g.drawString(String.format("实测峰值半径 %.1f px = %.2f 格", rowPeak, rowPeak / 8f), 10, r * cell + cell / 2 + 14);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        }

        g.setColor(new java.awt.Color(0x8f8aa8));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        g.drawString("每一列为该特效寿命的 0% / 25% / 50% / 75% / 100%；虚线圆圈 = 1 格（8 像素）", 10,
            rows.length * cell - 8);
        g.dispose();
        ImageIO.write(img, "png", new File(out, filename));
        System.out.println("  " + filename + "  " + img.getWidth() + "x" + img.getHeight());
    }

    /** 在 (0,0) 处把 fx 在时刻 t 的样子画成一张 cell×cell 的图。 */
    static BufferedImage render(FakeBatch b, Effect fx, float t, float half, int cell){
        BufferedImage img = new BufferedImage(cell, cell, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new java.awt.Color(0x14121c));
        g.fillRect(0, 0, cell, cell);
        g.dispose();

        Effect.EffectContainer c = new Effect.EffectContainer();
        c.set(1, Color.white, t, fx.lifetime, ROT, 0f, 0f, null);
        b.reset();
        b.record = true;
        try{
            fx.render(c);
        }catch(Throwable e){
            System.out.println("    [render threw] " + e);
            return img;
        }
        b.flush();
        if(System.getenv("FXDEBUG") != null)
            System.out.println("    [dbg] kinds=" + b.kinds.toString().replace(", ", "") + " n=" + b.n + " quads=" + b.quads);

        Anim.Painter p = new Anim.Painter(img, half, cell / (2f * half));
        int o = 0;
        for(int k = 0; k < b.kinds.size; k++){
            int kind = b.kinds.get(k);
            int stride = kind == 0 ? 24 : 8;
            if(o + stride > b.n) break;
            if(kind == 0){
                p.quad(b.verts, o, b.blends.get(k) == 1);          //Lines 的描边几何
            }else{
                //Fill.* 走的是区域绘制：8 个 float = x, y, originX, originY, w, h, rotation, packedColour
                //x/y 是锚点（Fill.circle 的 origin 是 0，所以 x/y 就是左下角）
                rect(p, b.verts[o], b.verts[o + 1], b.verts[o + 2], b.verts[o + 3],
                    b.verts[o + 4], b.verts[o + 5], b.verts[o + 6],
                    Float.floatToRawIntBits(b.verts[o + 7]), b.blends.get(k) == 1, half, cell / (2f * half));
            }
            o += stride;
        }
        Draw.reset();

        //在画比例尺之前，先把这一格实际点亮的范围量出来（和 FxSize 的顶点法互为交叉验证）
        measured = litRadius(img, cell / (2f * half));

        //比例尺：同心圆，一圈 = 1 格
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float ppu = cell / (2f * half);
        g2.setColor(new java.awt.Color(120, 116, 150, 130));
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, new float[]{3f, 4f}, 0f));
        for(int tiles = 1; tiles * 8f <= half; tiles++){
            int rad = (int)(tiles * 8f * ppu);
            g2.drawOval(cell / 2 - rad, cell / 2 - rad, rad * 2, rad * 2);
        }
        g2.dispose();
        return img;
    }
}
