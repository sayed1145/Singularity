package preview;

import arc.math.Mathf;
import blackhole.g3d.Serpent3D;
import blackhole.models.AnchorModels;
import blackhole.g3d.Rig;

/**
 * 蛇式躯干的连续性自检：把身体摆过一整个波形，量相邻两节<b>装甲板边缘</b>之间有没有裂缝。
 *
 * <p>用的是 {@link Serpent3D#offset} —— 也就是动画真正调用的那套数，不是抄一份。
 * 每节装甲板在自身坐标系里沿 y 覆盖 [minY, maxY]（y 越小越靠尾巴），
 * 板的朝向是 {@code offset[2]}，所以把前后缘的中点旋到头部坐标系里再量距离。
 *
 * <p>距离 <= 0 表示两块板互相重叠（蛇甲本来就该叠着）；距离 > 0 就是肉眼能看见的漏洞。
 */
public final class SerpentProbe{

    public static void main(String[] args){
        //UnitType's constructor registers itself with the content loader (same as bake.Bake does)
        mindustry.Vars.content = new mindustry.core.ContentLoader();
        Serpent3D t = new Serpent3D("spike-warden");
        Rig r = AnchorModels.spikeWarden(t);

        int n = t.segments.size;
        System.out.println("spike-warden serpent rig");
        System.out.println("  bones=" + r.bones + " pieces=" + r.pieceCount
            + " faces=" + r.faces() + " half=" + r.half + " height=" + r.height);
        System.out.println("  segments=" + n + " spacing=" + t.spacing
            + " waveAmp=" + t.waveAmp + " waveScl=" + t.waveScl + " wavePhase=" + t.wavePhase
            + " mag=" + t.mag + " scl=" + t.scl + " phase=" + t.phase);
        System.out.println("  segmentRotSpeed=" + t.segmentRotSpeed + " segmentMaxRot=" + t.segmentMaxRot
            + " crawlSlowdown=" + t.crawlSlowdown + " crawlSlowdownFrac=" + t.crawlSlowdownFrac);

        //y extent of each segment's armour plate, in its own frame
        float[] minY = new float[n], maxY = new float[n];
        for(int i = 0; i < n; i++){
            int b = t.segments.get(i);
            minY[i] = Float.POSITIVE_INFINITY;
            maxY[i] = Float.NEGATIVE_INFINITY;
            for(int p = 0; p < r.pieceCount; p++){
                Rig.Piece pc = r.pieces[p];
                if(pc.bone != b) continue;
                minY[i] = Math.min(minY[i], pc.minY);
                maxY[i] = Math.max(maxY[i], pc.maxY);
            }
            if(minY[i] == Float.POSITIVE_INFINITY){ minY[i] = 0f; maxY[i] = 0f; }
        }
        StringBuilder ext = new StringBuilder("  plate y-extent per segment:");
        for(int i = 0; i < n; i++) ext.append(String.format(" [%d] %.2f..%.2f", i, minY[i], maxY[i]));
        System.out.println(ext);

        float[] o = new float[64 * 3];
        float[] worst = new float[n];
        for(int i = 0; i < n; i++) worst[i] = Float.NEGATIVE_INFINITY;
        float worstAny = Float.NEGATIVE_INFINITY;
        int worstSeg = -1;
        float worstOverlap = Float.POSITIVE_INFINITY;
        int nan = 0;

        //sweep the whole wave: crawlTime over several wavelengths, and every body bend the engine allows.
        //bodyRot is RELATIVE to unitRot: CrawlComp clamps it with Angles.clampRange(segmentRot, rotation,
        //segmentMaxRot), so |bodyRot - unitRot| can never exceed segmentMaxRot.
        float selfMin = Float.POSITIVE_INFINITY;
        int selfAt = -1;
        for(int ti = 0; ti < 720; ti++){
            float crawlTime = ti * 0.5f;
            for(int bi = -6; bi <= 6; bi++){
                float bend = bi * (t.segmentMaxRot / 6f);
                for(int ui = 0; ui < 4; ui++){
                    float unitRot = ui * 90f;
                    float bodyRot = unitRot + bend;

                    t.spine(n, crawlTime, bodyRot, unitRot, o);
                    float[] cx = new float[n], cy = new float[n], ca = new float[n];
                    for(int i = 0; i < n; i++){
                        if(Float.isNaN(o[i*3]) || Float.isNaN(o[i*3+1]) || Float.isNaN(o[i*3+2])) nan++;
                        cx[i] = o[i*3];
                        cy[i] = o[i*3+1];
                        ca[i] = o[i*3+2];                    //yaw in the head frame, as the rig applies it
                    }

                    for(int i = 0; i < n - 1; i++){
                        //back edge centre of segment i, and front edge centre of segment i+1
                        float ar = ca[i] * Mathf.degRad;
                        float bx = cx[i] + (float)(-Math.sin(ar) * minY[i]);
                        float by = cy[i] + (float)(Math.cos(ar) * minY[i]);
                        float br = ca[i + 1] * Mathf.degRad;
                        float fx = cx[i + 1] + (float)(-Math.sin(br) * maxY[i + 1]);
                        float fy = cy[i + 1] + (float)(Math.cos(br) * maxY[i + 1]);

                        float d = Mathf.dst(bx, by, fx, fy) * Math.signum(by - fy < 0 ? -1f : 1f);
                        //positive == the plates have pulled apart
                        if(d > worst[i]) worst[i] = d;
                        if(d > worstAny){ worstAny = d; worstSeg = i; }
                        if(d < worstOverlap) worstOverlap = d;
                    }

                    //self-intersection: sections that are not neighbours must not fold into each other
                    for(int i = 0; i < n; i++){
                        for(int j = i + 2; j < n; j++){
                            float dd = Mathf.dst(cx[i], cy[i], cx[j], cy[j]);
                            if(dd < selfMin){ selfMin = dd; selfAt = i * 100 + j; }
                        }
                    }
                }
            }
        }

        System.out.println();
        System.out.println("  NaN offsets: " + nan);
        System.out.print("  worst gap between consecutive plates:");
        for(int i = 0; i < n - 1; i++) System.out.printf(" %d->%d %.3f", i, i + 1, worst[i]);
        System.out.println();
        System.out.printf("  worst overall: %.3f (segment %d->%d), most negative (deepest overlap): %.3f%n",
            worstAny, worstSeg, worstSeg + 1, worstOverlap);

        System.out.printf("  closest non-neighbour sections: %.3f (sections %d and %d) - must exceed the plate width%n",
            selfMin, selfAt / 100, selfAt % 100);

        boolean ok = nan == 0 && worstAny <= 0f && selfMin > 2.0f;
        System.out.println();
        System.out.println(ok ? "  VERDICT: body stays joined - every plate overlaps its neighbour at every phase and bend"
                             : "  VERDICT: FAIL - the body comes apart");
        if(!ok) System.exit(1);
    }
}
