package sixfold;

import arc.Core;
import arc.util.Time;
import mindustry.entities.Units;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Bullet;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;

/** Original "gravity well" projectile mechanic: a slow drifting orb that deals no
 * contact damage at all. While alive it tethers every enemy inside its radius - strong
 * slow plus a continuous pull toward the orb - and stores how many units are tethered.
 * When the well collapses (despawn) it detonates, dealing bonus damage per tethered
 * enemy: the more it caught, the harder it hits. No reused vanilla weapon behaviour. */
public class TetherBulletType extends BasicBulletType{
    public static final Stat tetherStat = new Stat("sixfold-tether", StatCat.function);

    public float tetherRadius = 80f;
    /** pull acceleration added to tethered units per frame */
    public float pull = 0.085f;
    public float bonusPerEnemy = 45f;
    public int maxBonus = 6;

    public TetherBulletType(float speed, float damage){
        super(speed, damage);
        collides = false;
        collidesTiles = false;
        collidesAir = false;
        keepVelocity = false;
        hittable = false;
        absorbable = false;
        hitSize = 6f;
    }

    @Override
    public void update(Bullet b){
        super.update(b);
        float[] count = {0f};
        boolean pullOn = !(b.owner instanceof TetherTurret.TetherTurretBuild tb) || tb.pull;
        Units.nearbyEnemies(b.team, b.x, b.y, tetherRadius, u -> {
            if(u.dead() || !u.within(b.x, b.y, tetherRadius)) return;
            u.apply(SixfoldMod.tether, 30f);
            if(pullOn){
                float dx = b.x - u.x, dy = b.y - u.y;
                float d = Math.max(10f, (float)Math.hypot(dx, dy));
                u.vel.add(dx / d * pull * Time.delta, dy / d * pull * Time.delta);
            }
            count[0]++;
        });
        b.fdata = count[0];
        if(((int)Time.time) % 6 == 0){
            EmberEffects.tetherRing.at(b.x, b.y, tetherRadius / 80f);
        }
    }

    @Override
    public void despawned(Bullet b){
        float bonus = Math.min(maxBonus, b.fdata) * bonusPerEnemy;
        if(bonus > 0f){
            float r = tetherRadius;
            Units.nearbyEnemies(b.team, b.x, b.y, r, u -> {
                if(!u.dead() && u.within(b.x, b.y, r)) u.damage(bonus);
            });
        }
        super.despawned(b);
    }
}
