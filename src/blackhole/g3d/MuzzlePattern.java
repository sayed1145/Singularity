package blackhole.g3d;

import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.Fx;
import mindustry.entities.*;
import mindustry.entities.pattern.*;
import mindustry.gen.*;

/**
 * Shoot pattern wrapper that makes every turret shot leave one of the model's drawn barrels.
 *
 * <p>Vanilla patterns place bullets on the flat ground plane (ShootAlternate spreads them by an arbitrary
 * distance, ShootPattern fires from the pivot line). A 3D turret head is drawn in perspective: its barrels have a
 * real lateral spacing and sit {@code headZ + z} above the ground, so the drawn muzzle is displaced north of the
 * ground point beneath it. This wrapper
 * <ul>
 *   <li>keeps the wrapped pattern's timing, shot count and angular spread,</li>
 *   <li>replaces each shot's offset with the model's muzzle (cycling barrels shot by shot), and</li>
 *   <li>attaches a one-shot {@link Mover} that, on the bullet's first update (before it is ever drawn), moves it
 *   to the exact projected screen position of that muzzle and re-aims it at the turret's target.</li>
 * </ul>
 *
 * <p><b>v8.5.1 补一刀：枪口火光也要跟着抬。</b>引擎在子弹出生点（还没抬升的地面点）播 shootEffect /
 * smokeEffect，而子弹和模型炮口都在抬升后的位置，于是"火光在脚下、弹丸在炮口"，差着整整一个透视抬升量
 * （吸积炮约 4.5 个世界单位 ≈ 半个格子）。所以安装时会把方块上的 shootEffect / smokeEffect 换成
 * {@link Fx#none}，改由 {@link Lift} 在抬升后的位置补播，火光、硝烟、弹丸三者落在同一个点。
 * Pure geometry on the bullet itself, so it behaves identically on servers and clients.
 */
public class MuzzlePattern extends ShootPattern{
    public final ShootPattern base;
    /** muzzle points in head space (x, y, z above the ring), triplets */
    public final float[] muzzles;
    public final float headZ, camD, camCy;
    /** 从方块上摘下来的开火/硝烟特效，抬升之后由 Lift 在正确的位置补播；为 null 表示没有。 */
    public final Effect shootFx, smokeFx;

    public MuzzlePattern(ShootPattern base, float[] muzzles, float headZ, Cam cam){
        this(base, muzzles, headZ, cam.D, cam.cy, null, null);
    }

    public MuzzlePattern(ShootPattern base, float[] muzzles, float headZ, Cam cam, Effect shootFx, Effect smokeFx){
        this(base, muzzles, headZ, cam.D, cam.cy, shootFx, smokeFx);
    }

    /** 相机参数版本：别的分包（前哨 / 体素）有自己的 Cam 类，数学完全一样，传数值就行。 */
    public MuzzlePattern(ShootPattern base, float[] muzzles, float headZ, float camD, float camCy,
                         Effect shootFx, Effect smokeFx){
        this.base = base;
        this.muzzles = muzzles;
        this.headZ = headZ;
        this.camD = camD;
        this.camCy = camCy;
        this.shootFx = shootFx;
        this.smokeFx = smokeFx;
        this.shots = base.shots;
        this.firstShotDelay = base.firstShotDelay;
        this.shotDelay = base.shotDelay;
    }

    public int barrels(){
        return Math.max(1, muzzles.length / 3);
    }

    @Override
    public void shoot(int totalShots, BulletHandler handler, @Nullable Runnable barrelIncrementer){
        final int[] k = {0};
        final int n = barrels();
        /*
         * v8.5.2 hotfix: this is deliberately an anonymous class, not a lambda.
         *
         * BulletHandler declares the 5-argument shoot() as abstract and the 4-argument one as a *default*
         * method. Official patterns (ShootAlternate, ShootSpread, ...) call the 4-argument default, and a
         * lambda that d8 has desugared to a real class only carries the abstract method - so ART cannot
         * find the inherited default and throws
         *   AbstractMethodError: abstract method "void BulletHandler.shoot(float, float, float, float)"
         * which crashed every turret on an Android device.
         *
         * An explicit class implements BOTH overloads, so nothing has to be inherited at runtime and the
         * crash cannot happen at any API level, with or without desugaring.
         */
        BulletHandler lifted = new BulletHandler(){
            @Override
            public void shoot(float x, float y, float rotation, float delay){
                shoot(x, y, rotation, delay, null);
            }

            @Override
            public void shoot(float x, float y, float rotation, float delay, @Nullable Mover mover){
                int i = ((totalShots + k[0]++) % n + n) % n;
                float mx = muzzles[i * 3], my = muzzles[i * 3 + 1], mz = muzzles[i * 3 + 2];
                handler.shoot(mx, my, rotation, delay, new Lift(mover, headZ + mz, camD, camCy, shootFx, smokeFx));
            }
        };
        base.shoot(totalShots, lifted, barrelIncrementer);
    }

    /** Lifts a bullet from the ground point under a muzzle to where the camera shows that muzzle. */
    public static class Lift implements Mover{
        final @Nullable Mover inner;
        final float z, d, cy;
        final @Nullable Effect shootFx, smokeFx;
        boolean done;

        public Lift(@Nullable Mover inner, float z, float d, float cy,
                    @Nullable Effect shootFx, @Nullable Effect smokeFx){
            this.inner = inner;
            this.z = z;
            this.d = d;
            this.cy = cy;
            this.shootFx = shootFx;
            this.smokeFx = smokeFx;
        }

        @Override
        public void move(Bullet b){
            if(!done){
                done = true;
                if(b.owner instanceof Posc o){
                    float s = d / Math.max(d - z, 1f);
                    float rx = b.x - o.getX(), ry = b.y - o.getY();
                    float nx = o.getX() + rx * s, ny = o.getY() + cy + (ry - cy) * s;
                    b.set(nx, ny);
                    //keep hitting what was aimed at (never bend a shot more than 30 degrees)
                    float angle = b.rotation();
                    if(Mathf.dst(nx, ny, b.aimX, b.aimY) > 12f && (b.aimX != 0f || b.aimY != 0f)){
                        float to = Angles.angle(nx, ny, b.aimX, b.aimY);
                        float diff = Angles.angleDist(angle, to);
                        angle = diff < 30f ? to : Angles.moveToward(angle, to, 30f);
                        b.rotation(angle);
                    }
                    //v8.5.1: 火光和硝烟跟着一起抬，不然它们留在脚下的地面点上。
                    //方块自己没配特效时，退回子弹类型自带的那一套（引擎原本就是这么兜底的）。
                    Effect fx = shootFx != null ? shootFx : (b.type != null ? b.type.shootEffect : null);
                    Effect sm = smokeFx != null ? smokeFx : (b.type != null ? b.type.smokeEffect : null);
                    Color tint = b.type != null ? b.type.hitColor : null;
                    if(fx != null) fx.at(nx, ny, angle, tint);
                    if(sm != null) sm.at(nx, ny, angle, tint);
                }
            }
            if(inner != null) inner.move(b);
        }
    }
}
