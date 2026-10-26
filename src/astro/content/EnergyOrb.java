package astro.content;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;

/** A homing ball of stored energy, rendered with plain additive circles and a trail. */
public class EnergyOrb extends BulletType{
    public Color glow = AstroFx.gold, core = Color.white;
    public float size = 6f;

    public EnergyOrb(float speed, float damage){
        super(speed, damage);
        lifetime = 52f;
        collidesAir = collidesGround = true;
        hitEffect = Fx.hitLaserBlast;
        despawnEffect = Fx.hitLaserBlast;
        smokeEffect = Fx.none;
        shootEffect = Fx.none;
        hitSound = Sounds.explosionPlasmaSmall;
        hitSoundVolume = 0.6f;
        trailLength = 14;
        trailWidth = 2.4f;
        trailColor = glow;
        homingPower = 0.055f;
        homingRange = 170f;
        lightning = 2;
        lightningLength = 7;
        lightningDamage = 14f;
        lightningColor = glow;
        status = StatusEffects.electrified;
        statusDuration = 70f;
        splashDamage = 28f;
        splashDamageRadius = 26f;
        drawSize = 60f;
    }

    @Override
    public void init(){
        super.init();
        trailColor = glow;
        lightningColor = glow;
        hitColor = glow;
    }

    @Override
    public void draw(Bullet b){
        drawTrail(b);
        float pulse = 1f + Mathf.absin(b.time + b.id, 3.5f, 0.12f);
        float r = size * pulse * Mathf.clamp(b.fout() * 6f);
        Draw.color(glow, 0.28f);
        Fill.circle(b.x, b.y, r * 1.9f);
        Draw.color(glow);
        Fill.circle(b.x, b.y, r);
        Draw.color(core);
        Fill.circle(b.x, b.y, r * 0.52f);
        Draw.reset();
    }

    @Override
    public void drawLight(Bullet b){
        Drawf.light(b.x, b.y, size * 6f, glow, 0.6f);
    }
}
