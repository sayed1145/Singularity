package preview;

import arc.math.Angles;
import arc.math.Mathf;

/**
 * v8.5.5：把类星体喷流的枪口几何<b>算一遍</b>，对照 v8.5.3 那个错的公式。
 *
 * <p>这个程序不跑游戏（跑游戏也测不到光束起点，那是个客户端的量，
 * 服务端根本不画）。它做的是纯几何复算：把炮塔摆在世界坐标里，
 * 按两种公式各算一遍炮口落点，看离炮塔中心有多远、偏了多少格。
 *
 * <p>用的常数取自 {@code TurretModels.quasarLance}：
 * {@code SimpleTurret("quasar-lance", 4, sing).headZ(4.6f).muzzles(0f, 15.4f, 1.8f)}，
 * 相机参数按 {@code Cam} 的规则由 size=4 推出来（half = size*8/2 = 16）。
 *
 * <p>用法：{@code java preview.QuasarGeom}
 */
public class QuasarGeom{
    //size 4 -> half = 4 * 8 / 2 = 16
    static final float half = 16f;
    static final float camD = half * 7f;      //D
    static final float camCy = -half * 4.9f;  //cy

    //TurretModels: .headZ(4.6f).muzzles(0f, 15.4f, 1.8f)
    static final float muzzleX = 0f, muzzleY = 15.4f;
    static final float muzzleZ = 4.6f + 1.8f; //headZ + local z

    public static void main(String[] args){
        System.out.printf("类星体喷流：size=4, half=%.0f, cam.D=%.0f, cam.cy=%.1f%n", half, camD, camCy);
        System.out.printf("枪口：模型偏移 (%.1f, %.1f, %.1f)，抬升后高度 z=%.1f%n", muzzleX, muzzleY, muzzleZ, muzzleZ);
        System.out.printf("透视缩放 s = D / (D - z) = %.0f / %.0f = %.4f%n%n", camD, camD - muzzleZ, scale());

        System.out.printf("%8s | %-28s | %-28s%n", "朝向", "v8.5.3（错的）", "v8.5.5（对的）");
        System.out.printf("%8s | %-28s | %-28s%n", "", "炮口 x, y（离中心格数）", "炮口 x, y（离中心格数）");
        System.out.println("-".repeat(78));

        float worstOld = 0f, worstNew = 0f;
        for(float rot = 0f; rot < 360f; rot += 45f){
            //炮塔摆在世界的这个位置（随便挑一个远离原点的，好让"绝对 Y"和"相对 Y"的差别显出来）
            float bx = 1000f, by = 2000f;

            float s = scale();
            float ox = Angles.trnsx(rot - 90f, muzzleX, muzzleY);
            float oy = Angles.trnsy(rot - 90f, muzzleX, muzzleY);

            //引擎把光束放在"未经抬升"的地面点上，也就是世界坐标 (bx+ox, by+oy)
            float bulletY = by + oy;

            //--- v8.5.3：直接拿这个**世界坐标**去减 camCy（错的 —— camCy 是相对炮塔的）
            float nxOld = bx + ox * s;                                  //x 那一行本来是对的
            float nyOld = by + camCy + (bulletY - camCy) * s;           //y 这一行量纲错了

            //--- v8.5.5：先减掉炮塔中心，取**相对偏移**再投影（对的）
            float nxNew = bx + ox * s;
            float nyNew = by + camCy + (oy - camCy) * s;

            float dOld = Mathf.dst(nxOld - bx, nyOld - by);
            float dNew = Mathf.dst(nxNew - bx, nyNew - by);
            worstOld = Math.max(worstOld, dOld);
            worstNew = Math.max(worstNew, dNew);

            System.out.printf("%8.0f | %7.1f,%7.1f  (%5.1f 格) | %7.1f,%7.1f  (%5.1f 格)%n",
                rot, nxOld, nyOld, dOld / 8f, nxNew, nyNew, dNew / 8f);
        }

        System.out.println();
        System.out.printf("最坏情况：v8.5.3 把枪口甩到离炮塔 %.1f 像素（%.1f 格）—— 光束根本不在炮塔上，也就不可能开火。%n",
            worstOld, worstOld / 8f);
        System.out.printf("          v8.5.5 是 %.1f 像素（%.1f 格）—— 正好在塔头该在的地方。%n",
            worstNew, worstNew / 8f);
        System.out.printf("改进倍数：%.0f 倍%n", worstOld / Math.max(worstNew, 0.001f));
    }

    static float scale(){
        return camD / Math.max(camD - muzzleZ, 1f);
    }
}
