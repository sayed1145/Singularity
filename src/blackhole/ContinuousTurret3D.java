package blackhole;

import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Tmp;
import arc.util.Time;
import mindustry.entities.Effect;
import mindustry.Vars;
import mindustry.gen.Bullet;
import mindustry.world.blocks.defense.turrets.ContinuousTurret;

/**
 * v8.5.5 重写：让持续光束炮（类星体喷流）的枪口对得上，并且能正常开火。
 *
 * <h2>v8.5.3 那一版为什么开不了火</h2>
 *
 * <p>引擎 {@code ContinuousTurret.updateBullet} 每一刻都把光束按
 * {@code shootX / shootY} 重新摆一次，而 {@code shootY} 是<b>沿炮管方向</b>的偏移 ——
 * 引擎里根本没有"起点高度"这个概念。所以要用 {@link blackhole.g3d.Cam} 的透视把炮口抬起来。
 *
 * <p>v8.5.3 用的公式是：
 *
 * <pre>
 * float s = camD / max(camD - muzzleZ, 1f);
 * nx = x + (b.x - x) * s;
 * ny = y + camCy + (b.y - camCy) * s;   // &lt;-- b.y 是"世界坐标"
 * </pre>
 *
 * <p>而 {@link blackhole.g3d.MuzzlePattern.Lift} 里真正正确的是：
 *
 * <pre>
 * float rx = b.x - o.getX(), ry = b.y - o.getY();   // &lt;-- 先减掉，取"相对偏移"
 * nx = o.getX() + rx * s;
 * ny = o.getY() + cy + (ry - cy) * s;
 * </pre>
 *
 * <p>差别就在那个减法。拿世界坐标 {@code y ≈ 2000} 直接乘 s（≈1.06），
 * 光束会被甩到 {@code y + 78 + 2000×1.06 ≈ 4100}，也就是<b>离炮塔两百多格</b>的地方 ——
 * 看不见、也打不到任何东西。这就是"不能开火、没有开火特效"的全部原因。
 *
 * <p>这一版不再调用 {@code super} 后打补丁，而是把引擎那段整个照抄一遍、
 * 把炮口偏移直接并进去，避免第二处问题：引擎用
 * {@code curLength = dst(aimX, aimY)}（<b>从炮塔中心</b>量）来算光束长度，
 * 如果只把起点挪走而不改量法，长度每刻都会把"抬升量"累加进去，光束会自己长出去。
 * 这里改成<b>从枪口量</b>，长度就稳定了。
 *
 * <p>纯几何；伤害、射程、耗电、射速一个都没动。服务端与客户端算法一致。
 */
public class ContinuousTurret3D extends ContinuousTurret{
    /** 炮口在模型坐标系里的横向偏移（像素）。由 {@link blackhole.models.Models#install()} 从 3D 模型上取。 */
    public float muzzleX = 0f;
    /** 炮口在模型坐标系里的前后偏移（沿炮管方向，像素）。 */
    public float muzzleY = 0f;
    /** 炮口离地高度（像素）＝ 塔头抬升 + 炮口局部 Z。 */
    public float muzzleZ = 0f;
    /** 相机参数，同样从模型上取。 */
    public float camD = 1f, camCy = 0f;

    /** 开火时在枪口补播的特效（引擎那一份被 MuzzlePattern 摘走了，这里自己来）。 */
    public Effect muzzleFx = null;
    /** 补播间隔（刻）。 */
    public float muzzleFxInterval = 6f;

    public ContinuousTurret3D(String name){
        super(name);
        buildType = ContinuousTurret3DBuild::new;
    }

    public class ContinuousTurret3DBuild extends ContinuousTurretBuild{
        float fxTimer = 0f;

        // ------------------------------------------------------------------ v8.5.6：及时止损（音效）
        //
        // 症状：光束早灭了、甚至炮塔都被拆了，循环音效还在响。
        //
        // 引擎这边是这么接的（v160.5 源码）：
        //   ContinuousTurretBuild.shouldActiveSound()  -> bullets.any()
        //   ContinuousTurretBuild.activeSoundVolume()  -> 1f
        //   TurretBuild.updateTile()                   -> soundLoop.update(x, y, shouldActiveSound(), ...)
        //
        // 三个坑叠在一起：
        //   1. 判定只看"还有没有光束子弹"。而 ContinuousTurretBuild.updateBullet 每刻把
        //      bullet.time 钉在 lifetime * optimalLifeFract * warmup 上，只有在 isShooting 变 false
        //      之后才放手让它自然到期 —— quasarBeam 的 lifetime 是 80，于是目标没了之后
        //      子弹还要再活几十刻，声音就跟着再响几十刻。
        //   2. SoundLoop.update() 里 id >= 0 而 play == false 时，用固定的 fadeSpeed = 0.05 淡出，
        //      也就是再拖 20 刻（0.33 秒）才真的 Core.audio.stop()。
        //   3. SoundLoop 只在 updateTile() 里被驱动；实体一旦不再更新，就再没有人去停它。
        //
        // 所以这里做两件事：把"真的在开火"加进判定条件，并且一不满足就**立刻** stop()，
        // 不等那 20 刻的淡出。remove() / onDestroyed() 再各兜一次底。

        @Override
        public boolean shouldActiveSound(){
            //super 是 bullets.any()；再要求"仍锁着目标、还活着、没被关掉"
            return super.shouldActiveSound() && isShooting && !dead && enabled;
        }

        @Override
        public void updateTile(){
            super.updateTile();   //里面已经跑过 bullets.removeAll(...)，这里看到的列表是最新的
            if(soundLoop != null && !shouldActiveSound()) soundLoop.stop();
        }

        @Override
        public void remove(){
            super.remove();
            if(soundLoop != null) soundLoop.stop();
        }

        @Override
        public void onDestroyed(){
            super.onDestroyed();
            if(soundLoop != null) soundLoop.stop();
        }

        @Override
        protected void updateBullet(BulletEntry entry){
            //no modelled muzzle: behave exactly like the vanilla ContinuousTurret
            if(muzzleZ <= 0f){
                super.updateBullet(entry);
                return;
            }

            Bullet b = entry.bullet;
            if(b == null) return;

            //------------------------------------------------------------------ 1. 枪口在屏幕上的位置
            //Cam.sx / Cam.sy, applied to the muzzle's own model offset (not to a world coordinate)
            float s = camD / Math.max(camD - muzzleZ, 1f);
            float ox = Angles.trnsx(rotation - 90f, muzzleX, muzzleY);
            float oy = Angles.trnsy(rotation - 90f, muzzleX, muzzleY);
            float mx = x + ox * s;
            float my = y + camCy + (oy - camCy) * s;

            //------------------------------------------------------------------ 2. 光束从这里射出
            b.rotation(rotation + entry.rotation);
            b.set(mx, my);

            //------------------------------------------------------------------ 3. 长度
            //engine measures dst(aimX, aimY) from the building centre; measuring from the muzzle
            //instead is what keeps the smoothed length from adding the lift every single tick
            float shootLength = Math.min(dst(targetPos), range);
            float curLength = Mathf.dst(mx, my, b.aimX, b.aimY);
            float resultLength = Mathf.approachDelta(curLength, shootLength, aimChangeSpeed);
            Tmp.v1.trns(rotation, lastLength = resultLength).add(mx, my);
            b.aimX = Tmp.v1.x;
            b.aimY = Tmp.v1.y;

            //------------------------------------------------------------------ 4. 其余照抄引擎
            if(scaleDamageEfficiency){
                b.damage = b.type.damage * Math.min(efficiency, 1f) * timeScale * b.damageMultiplier();
            }

            if(isShooting && hasAmmo()){
                b.time = b.lifetime * b.type.optimalLifeFract * Math.min(shootWarmup, efficiency);
                b.keepAlive = true;
            }

            //------------------------------------------------------------------ 5. 枪口的开火特效
            if(muzzleFx != null && !Vars.headless && (fxTimer += Time.delta) >= muzzleFxInterval){
                fxTimer = 0f;
                muzzleFx.at(mx, my, rotation);
            }
        }
    }
}
