package blackhole.g3d;

import arc.math.Mathf;
import mindustry.gen.Building;

/**
 * v8.5.1：给采矿机用的"干活时钟"。
 *
 * <p>引擎里 {@code Building.totalProgress()} 对非合成类方块（矿机、束矿器、凿墙机都在此列）
 * 直接返回 {@code Time.time} —— 也就是说**不管有没有在挖，这个数一直在往前走**。
 * 模型的转动动画是拿它当角度用的，于是矿机在没矿、没电、被禁用、或者装满停工的
 * 时候钻头照转不误，看着像一直在干活。
 *
 * <p>这里改成自己维护一个只在真正在产出时才累加的时钟：
 * 累加量 = {@code edelta() * clamp(warmup)}，而 warmup 在方块停工时会自己掉回 0
 * （矿机满载/没矿/断电的判定本来就写在引擎的 updateTile 里），于是钻头跟着平滑地停下来。
 */
public final class SpinClock{
    private SpinClock(){}

    /**
     * @param cur  当前累计值
     * @param b    方块（提供 edelta：包含效率，没电时为 0）
     * @param gate 门控，通常传 warmup；0 表示停工，1 表示满速
     */
    public static float advance(float cur, Building b, float gate){
        return cur + b.edelta() * Mathf.clamp(gate);
    }
}
