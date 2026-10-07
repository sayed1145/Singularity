package blackhole.g3d;

import arc.math.*;
import arc.util.*;
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
 * Pure geometry on the bullet itself, so it behaves identically on servers and clients.
 */
public class MuzzlePattern extends ShootPattern{
    public final ShootPattern base;
    /** muzzle points in head space (x, y, z above the ring), triplets */
    public final float[] muzzles;
    public final float headZ;
    public final Cam cam;

    public MuzzlePattern(ShootPattern base, float[] muzzles, float headZ, Cam cam){
        this.base = base;
        this.muzzles = muzzles;
        this.headZ = headZ;
        this.cam = cam;
        this.shots = base.shots;
        this.firstShotDelay = base.firstShotDelay;
        this.shotDelay = base.shotDelay;
    }

    public int barrels(){
        return Math.max(1, muzzles.length / 3);
    }

    @Override
    public void shoot(int totalShots, BulletHandler handler, @Nullable Runnable barrelIncrementer){
        int[] k = {0};
        int n = barrels();
        base.shoot(totalShots, (x, y, rotation, delay, mover) -> {
            int i = ((totalShots + k[0]++) % n + n) % n;
            float mx = muzzles[i * 3], my = muzzles[i * 3 + 1], mz = muzzles[i * 3 + 2];
            handler.shoot(mx, my, rotation, delay, new Lift(mover, headZ + mz, cam));
        }, barrelIncrementer);
    }

    /** Lifts a bullet from the ground point under a muzzle to where the camera shows that muzzle. */
    public static class Lift implements Mover{
        final @Nullable Mover inner;
        final float z;
        final Cam cam;
        boolean done;

        public Lift(@Nullable Mover inner, float z, Cam cam){
            this.inner = inner;
            this.z = z;
            this.cam = cam;
        }

        @Override
        public void move(Bullet b){
            if(!done){
                done = true;
                if(b.owner instanceof Posc o){
                    float s = cam.D / Math.max(cam.D - z, 1f);
                    float rx = b.x - o.getX(), ry = b.y - o.getY();
                    float nx = o.getX() + rx * s, ny = o.getY() + cam.cy + (ry - cam.cy) * s;
                    b.set(nx, ny);
                    //keep hitting what was aimed at (never bend a shot more than 30 degrees)
                    if(Mathf.dst(nx, ny, b.aimX, b.aimY) > 12f && (b.aimX != 0f || b.aimY != 0f)){
                        float base = b.rotation(), to = Angles.angle(nx, ny, b.aimX, b.aimY);
                        float d = Angles.angleDist(base, to);
                        b.rotation(d < 30f ? to : Angles.moveToward(base, to, 30f));
                    }
                }
            }
            if(inner != null) inner.move(b);
        }
    }
}
