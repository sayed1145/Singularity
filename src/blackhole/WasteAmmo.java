package blackhole;

import arc.graphics.Color;
import arc.util.*;
import mindustry.*;
import mindustry.content.Fx;
import mindustry.entities.bullet.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.meta.*;

/**
 * v8.7 —— <b>工业废料当炮弹</b>。
 *
 * <p>废料是欧雷利亚循环链的渣滓：到处都溢出来，除了回收线本身几乎一无是处。
 * 这一版把它变成一种<b>免费的防空弹</b>：两门炮能装，其中一门必须是星环。
 *
 * <p><b>v8.7.1</b> —— 两种废料弹（同一门炮，不同的弹）：
 * <ul>
 *   <li><b>星环</b>（{@code blackhole-halo}）—— 直伤仍是 40，其余不动。</li>
 *   <li><b>天罗</b>（{@code blackhole-sky-net}）—— 直伤降到 <b>20</b>。</li>
 * </ul>
 * 两种弹都是<b>装填倍数 ×5</b>（一个废料 = 五发，{@code ammoMultiplier} 5），
 * 并且<b>取消对建筑的伤害衰减</b>（{@code buildingDamageMultiplier} 0.8 → 1.0）。
 *
 * <p><b>v8.7.2</b> —— 修正 8.7.1 的误解：<b>废料不额外加任何开火速率</b>
 * （{@code reloadMultiplier} 回到 1）。中文面板里“装填倍数”对应 {@code ammoMultiplier}，
 * “开火速率”才对应 {@code reloadMultiplier}，两者不能混为一谈。
 *
 * <table border="1" cellpadding="3">
 *   <tr><th>项</th><th>废料弹（星环）</th><th>废料弹（天罗）</th><th>对照（星环 · 钨弹）</th></tr>
 *   <tr><td>直伤</td><td><b>40</b></td><td><b>20</b></td><td>56（金辉矿）</td></tr>
 *   <tr><td>范围伤害 / 半径</td><td>58 / 30 px</td><td>58 / 30 px</td><td>52 / 27 px</td></tr>
 *   <tr><td>射程</td><td><b>434 px（54 格）</b></td><td>434 px（54 格）</td><td>319 px（40 格）</td></tr>
 *   <tr><td>追踪</td><td>有（0.13 / 120 px）</td><td>有（0.13 / 120 px）</td><td>有（0.11 / 105 px）</td></tr>
 *   <tr><td>装填倍数（每份矿几发）</td><td><b>×5</b>（ammoMultiplier 5）</td><td><b>×5</b></td><td>×4</td></tr>
 *   <tr><td>开火速率</td><td>×1（不加）</td><td>×1（不加）</td><td>×1</td></tr>
 *   <tr><td>对建筑伤害</td><td><b>100%</b></td><td><b>100%</b></td><td>100%</td></tr>
 *   <tr><td>命中特效</td><td><b>裂变</b>（{@link FissionFx#fission}）</td><td>裂变</td><td>原版爆炸</td></tr>
 * </table>
 *
 * <p>和 {@link MarrowAmmo} 一样，弹药必须挂在 {@code recost()} 的方块循环之后 ——
 * {@code convertAmmo()} 会把整张弹药表换掉。
 */
public final class WasteAmmo{
    /** the waste shell for the halo - a homing fission missile, 40 damage */
    public static BulletType wasteShell;
    /** the waste shell for the sky net - the same missile at half the damage */
    public static BulletType wasteShellSky;

    private static boolean loaded;

    private WasteAmmo(){}

    /** Creates the bullets. Must run after the waste item exists ({@link AureliaCycle#industrialWaste}). */
    public static void load(){
        if(loaded) return;
        loaded = true;

        //v8.7.1: two shells out of the same factory - the halo keeps the heavy one, the sky net gets a light one.
        wasteShell = shell(40f);
        wasteShellSky = shell(20f);
    }

    /** one waste shell; the damage is the only thing that differs between the two turrets */
    private static MissileBulletType shell(float damage){
        return new MissileBulletType(6.2f, damage){{
            //6.2 * 70 = 434 px of reach - a good 35% further than the halo's tungsten missile
            lifetime = 70f;
            width = 9f;
            height = 12f;
            hitSize = 5f;
            //same homing contract as the halo's own missiles, only a little stronger
            homingPower = 0.13f;
            homingRange = 120f;
            collidesGround = false;
            collidesTiles = false;
            splashDamage = 58f;
            splashDamageRadius = 30f;
            knockback = 1.4f;
            //v8.7.2: one unit of waste is five shells - this is the "load multiplier" the request asks
            //for (ammoMultiplier = how many rounds one piece of ammo is worth)
            ammoMultiplier = 5f;
            //v8.7.2: waste adds NO rate of fire - reloadMultiplier 1 means the turret fires exactly as
            //fast as it would with any other shell. (8.7.1 wrongly set this to 5.)
            reloadMultiplier = 1f;
            //v8.7.1: no reduction against buildings any more - the shell hits a wall as hard as it hits a drone
            buildingDamageMultiplier = 1f;

            frontColor = Color.valueOf("d7d3e2");
            backColor = trailColor = hitColor = Color.valueOf("8f8aa3");

            //v8.7.3: 发射特效改成"安静"的版本 —— 枪口不再加色爆亮，命中不再有白热核闪，
            //生命周期也砍短了；星环一次齐射 15 发，旧的特效会在炮口和战场上堆出一片亮斑，
            //亮到糊住地图与界面。两个特效都钉在地图层（Layer.effect），不会画到 UI 上。
            shootEffect = FissionFx.fissionMuzzle;
            smokeEffect = Fx.none;
            hitEffect = despawnEffect = FissionFx.fission;

            //v8.7.3: 拖尾也收短一点，别在地图上糊成一条亮带
            trailLength = 6;
            trailWidth = 1.6f;
        }};
    }

    /**
     * Registers the waste shell on the turrets that can fire it. Runs after the ammo table has been
     * converted, so it is called at the end of {@code recost()} - which runs twice, and the second
     * pass is the one that sees the HJSON turrets.
     */
    public static void attach(Item waste){
        if(waste == null || wasteShell == null || wasteShellSky == null) return;

        int ok = 0;
        //the halo: the turret the request names - it keeps the heavy 40 damage shell
        if(addAmmo(Vars.content.block("blackhole-halo"), waste, wasteShell)) ok++;
        //the sky net: the second turret - an anti-air gun, so a homing shell fits it, but it is the
        //lighter one (20 damage) because the sky net fires in fast bursts
        if(addAmmo(Vars.content.block("blackhole-sky-net"), waste, wasteShellSky)) ok++;

        Log.info("[blackhole/waste] industrial waste registered as turret ammo on @/2 turrets " +
            "(halo @ dmg, sky-net @ dmg, load multiplier x@, no extra fire rate, no building reduction)",
            ok, (int)wasteShell.damage, (int)wasteShellSky.damage, wasteShell.ammoMultiplier);
    }

    /** Adds one ammo type to a turret and refreshes its ammo stat line. */
    private static boolean addAmmo(Block b, Item item, BulletType type){
        if(!(b instanceof ItemTurret t) || t.ammoTypes == null) return false;
        if(t.ammoTypes.containsKey(item)) return false;
        t.ammoTypes.put(item, type);
        //the turret must be able to hold at least one shot of it
        t.maxAmmo = Math.max(t.maxAmmo, 8);
        //the stat list was built before the ammo was attached - rebuild just that one line
        t.stats.remove(Stat.ammo);
        t.stats.add(Stat.ammo, StatValues.ammo(t.ammoTypes, t.name));
        return true;
    }
}
