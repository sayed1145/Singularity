package blackhole;

import arc.math.*;
import arc.struct.*;
import arc.util.Time;
import mindustry.*;
import mindustry.core.World;
import mindustry.gen.*;

/**
 * v8.7.2 —— <b>引力束缚</b>：把单位真的拖进引力源，而不是"轻轻推一下"。
 *
 * <p>做法照搬 wither-storm（{@code github.com/sayed1145/wither-storm}，{@code src/wstorm/world/StormLift.java}
 * 与 {@code src/wstorm/world/StormLogic.pullCreature}）：
 * <ul>
 *   <li><b>不用 {@code impulse()}</b> —— 单位自己的 AI 会把冲量挣掉，用冲量的引力场其实"绑不住"任何东西；</li>
 *   <li><b>每帧直接把单位搬动一小段</b>，方向永远指向引力源；</li>
 *   <li><b>v8.7.3：不再清零速度</b> —— 单位自己的移动速度会叠加在搬运量上，
 *       所以速度跑赢吸收速度的单位<b>能挣脱出去</b>，跑不赢的才被拖到中心。</li>
 *   <li><b>落点被实心格子挡住时先把单位抬起来</b>（elevation = 1），这正是 wither-storm 让被抓住的
 *       单位能穿过墙体、落到井里的办法；</li>
 *   <li><b>引力源不再拉着它的时候把高度还回去</b>，单位不会莫名其妙浮在半空。</li>
 * </ul>
 *
 * <p>用法：{@link #pull(int, Unit, float, float, float, float)} 每次 tick 调一次；
 * 引力源消失时调 {@link #releaseOwner(int)} 把还攥在手里的单位全部放掉。
 */
public final class GravityField{
    /** 一次持有：owner 是引力源 id，elevation 是被抬起之前的高度 */
    private static class Lease{
        int owner;
        float elevation;
        /** 最后一次被拉动的时间，用来判断引力源是不是松手了 */
        float touch;
    }

    private static final IntMap<Lease> leases = new IntMap<>();
    private static final IntSeq remove = new IntSeq();

    private GravityField(){}

    /** 抬起单位并记住原来的高度（wither-storm 的 StormLift.hold） */
    public static void hold(int owner, Unit u){
        Lease l = leases.get(u.id);
        if(l == null){
            l = new Lease();
            l.elevation = u.elevation;
            leases.put(u.id, l);
        }
        l.owner = owner;
        l.touch = Time.time;
        u.elevation = 1f;
        u.drownTime = 0f;
    }

    /** 归还高度（单个单位） */
    public static void release(int owner, Unit u){
        Lease l = leases.get(u.id);
        if(l == null || l.owner != owner) return;
        leases.remove(u.id);
        u.elevation = l.elevation;
    }

    /** 引力源没了：把它攥着的单位全部放掉 */
    public static void releaseOwner(int owner){
        remove.clear();
        for(var e : leases.entries()){
            if(e.value.owner == owner) remove.add(e.key);
        }
        for(int i = 0; i < remove.size; i++){
            int id = remove.get(i);
            Lease l = leases.get(id);
            Unit u = Groups.unit.getByID(id);
            if(u != null) u.elevation = l.elevation;
            leases.remove(id);
        }
    }

    /**
     * 把单位往 (x,y) 拖。
     *
     * @param owner   引力源 id（用来归还高度）
     * @param u       被拖的单位
     * @param speed   拖拽速度，px/秒
     * @param arrival 到达判定距离（小于它就算到中心了）
     * @return true 表示已经到中心
     */
    public static boolean pull(int owner, Unit u, float x, float y, float speed, float arrival){
        float dx = x - u.x, dy = y - u.y;
        float dist = Mathf.len(dx, dy);
        if(dist <= arrival) return true;

        hold(owner, u);

        //wither-storm moves the victim in small steps toward the mouth, never straight to it:
        //the step target is usually still on solid ground, and only when a wall is in the way
        //does the fallback below look for a landing spot further along the same direction.
        float step = Math.min(dist, speed * Time.delta);
        if(dist > 0.0001f){
            dx = dx / dist * step;
            dy = dy / dist * step;
        }
        move(u, u.x + dx, u.y + dy);
        return false;
    }

    /**
     * 搬到 (x,y)。落点是实心格子就沿方向找一块能落脚的地方（wither-storm 的 StormLift.move）；
     * 找不到就站在原地 —— 总比把单位塞进墙里强。
     */
    private static void move(Unit u, float x, float y){
        /*
         * v8.7.3：<b>不再把速度清零</b>。
         *
         * 8.7.2 是照 wither-storm 的做法把速度清零的 —— 那能"绑住"单位，但也等于宣判死刑：
         * 单位再怎么跑都动不了，唯一的结局是被碾碎。
         *
         * 现在改成：引力只负责"搬运"，单位自己的速度照常保留并叠加在搬运量上。
         * 于是<b>单位的移动速度一旦超过当前的吸收速度，它就能挣脱出去</b>；
         * 跑不赢的单位才会被一路拖到中心。反过来说，吸收速度低的地方（边缘）最容易逃。
         */
        if(u.canPass(World.toTile(x), World.toTile(y))){
            u.set(x, y);
            return;
        }
        float dx = x - u.x, dy = y - u.y, len = Mathf.len(dx, dy);
        if(len < 0.0001f) return;
        dx /= len;
        dy /= len;
        float maxX = Vars.world.unitWidth() * Vars.tilesize, maxY = Vars.world.unitHeight() * Vars.tilesize;
        //wither-storm searches up to 256 px along the same direction: that is how a captured unit
        //crosses a wall instead of bumping into it forever
        for(float d = 8f; d <= 256f; d += 4f){
            float nx = x + dx * d, ny = y + dy * d;
            if(nx >= 0f && ny >= 0f && nx < maxX && ny < maxY && u.canPass(World.toTile(nx), World.toTile(ny))){
                u.set(nx, ny);
                return;
            }
        }
    }

    /**
     * 每帧兜底：引力源超过 0.25 秒没再拉某个单位，就认为它松手了，把高度还回去。
     * 引力源自己会在 {@link #releaseOwner(int)} 里立刻放人，这里只是防漏。
     */
    public static void update(){
        remove.clear();
        for(var e : leases.entries()){
            Unit u = Groups.unit.getByID(e.key);
            if(u == null || u.dead || !u.isAdded() || Time.time - e.value.touch > 0.25f){
                remove.add(e.key);
            }
        }
        for(int i = 0; i < remove.size; i++){
            int id = remove.get(i);
            Lease l = leases.get(id);
            Unit u = Groups.unit.getByID(id);
            if(u != null && l != null) u.elevation = l.elevation;
            leases.remove(id);
        }
    }
}
