package outpost.content;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Bullet;

/**
 * v2 original ammunition. Both types extend BulletType directly with fully
 * custom load / draw / update / pierce behaviour and dedicated mod sprites.
 * Nothing here reuses RailBulletType, LaserBulletType or any vanilla ammo.
 */
public final class OutpostBullets{
    private OutpostBullets(){}

    /** Heavy lance slug: fast physical projectile, deep pierce, custom streak rendering. */
    public static class LanceSlugType extends BulletType{
        public String sprite = "blackhole-lance-slug";
        public TextureRegion front;
        public Color coreColor = OutpostFx.cyan;
        public float width = 10f, height = 18f;

        public LanceSlugType(float speed, float damage){
            super(speed, damage);
            // projectile (not hitscan): visible travel, trail, terminal impact
            collides = true;
            collidesAir = true;
            collidesGround = true;
            collidesTiles = true;
            pierce = true;
            pierceBuilding = true;
            pierceCap = 4;
            pierceDamageFactor = 0.55f;
            hitSize = 7f;
            ammoMultiplier = 1f;
            buildingDamageMultiplier = 0.6f;
            hitColor = OutpostFx.cyan;
            trailColor = OutpostFx.cyan;
            // all original effects
            shootEffect = OutpostFx.muzzleHeavy;
            smokeEffect = OutpostFx.trailHeavy;
            hitEffect = OutpostFx.hitHeavy;
            despawnEffect = OutpostFx.despawnHeavy;
            trailEffect = OutpostFx.trailHeavy;
            trailInterval = 2.5f;
            trailChance = 0f;
            despawnHit = false;
            keepVelocity = true;
            reflectable = false;
            absorbable = true;
            hittable = true;
        }

        @Override
        public void load(){
            super.load();
            front = Core.atlas.find(sprite, Core.atlas.find("bullet"));
        }

        @Override
        public void draw(Bullet b){
            super.draw(b);
            float fout = b.fout();
            float pulse = 1f + 0.12f * Mathf.sin(b.time * 0.9f);
            // dedicated sprite, facing up in the file, rotated to the flight direction
            if(front != null && front.found()){
                Draw.color(Color.white);
                Draw.rect(front, b.x, b.y, width * pulse * (0.6f + 0.4f * fout), height * pulse, b.rotation() - 90f);
            }else{
                // fallback diamond if the atlas is missing (never in-game, only in bare tests)
                Draw.color(coreColor);
                Fill.circle(b.x, b.y, 3f * fout);
            }
            // self-made additive streak along the velocity
            Draw.blend(Blending.additive);
            Draw.color(coreColor, 0.75f * fout);
            Lines.stroke(3.4f * fout);
            float back = 14f * fout + 6f;
            Lines.line(b.x, b.y, b.x - Angles.trnsx(b.rotation(), back), b.y - Angles.trnsy(b.rotation(), back));
            Draw.color(Color.white, 0.9f * fout);
            Lines.stroke(1.2f * fout);
            Lines.line(b.x, b.y, b.x - Angles.trnsx(b.rotation(), back * 0.7f), b.y - Angles.trnsy(b.rotation(), back * 0.7f));
            Draw.blend();
            Draw.reset();
        }

        @Override
        public void update(Bullet b){
            super.update(b);
            // slight coil shimmer: no homing, the heavy slug flies dead straight
        }

        @Override
        public void handlePierce(Bullet b, float initialHealth, float x, float y){
            // original falloff: each pierced target costs a fraction of remaining damage
            float sub = Math.max(initialHealth * pierceDamageFactor, damage * 0.12f);
            if(b.damage > 0f){
                OutpostFx.pierceHeavy.at(x, y, b.rotation(), hitColor);
            }
            b.damage -= Math.min(b.damage, sub);
            if(b.damage <= 0f){
                b.hit = true;
                b.remove();
            }
        }
    }

    /** Light coil bolt: burst-fired homing bolt with weave and a warm glow. */
    public static class CoilBoltType extends BulletType{
        public String sprite = "blackhole-coil-bolt";
        public TextureRegion front;
        public Color coreColor = OutpostFx.amber;
        public float width = 7f, height = 11f;

        public CoilBoltType(float speed, float damage){
            super(speed, damage);
            collides = true;
            collidesAir = true;
            collidesGround = true;
            collidesTiles = true;
            pierce = true;
            pierceBuilding = false;
            pierceCap = 1;
            pierceDamageFactor = 0.4f;
            hitSize = 4.5f;
            ammoMultiplier = 1f;
            homingPower = 0.09f;
            homingDelay = 4f;
            homingRange = 90f;
            weaveMag = 1.6f;
            weaveScale = 9f;
            hitColor = OutpostFx.amber;
            trailColor = OutpostFx.amber;
            shootEffect = OutpostFx.muzzleLight;
            smokeEffect = OutpostFx.trailLight;
            hitEffect = OutpostFx.hitLight;
            despawnEffect = OutpostFx.despawnLight;
            trailEffect = OutpostFx.trailLight;
            trailInterval = 3.5f;
            despawnHit = false;
            keepVelocity = true;
            reflectable = false;
        }

        @Override
        public void load(){
            super.load();
            front = Core.atlas.find(sprite, Core.atlas.find("bullet"));
        }

        @Override
        public void draw(Bullet b){
            super.draw(b);
            float fout = b.fout();
            float spin = Mathf.sin(b.time * 1.4f, 8f);
            if(front != null && front.found()){
                Draw.color(Color.white);
                Draw.rect(front, b.x, b.y, width * (0.7f + 0.3f * fout), height, b.rotation() - 90f + spin);
            }else{
                Draw.color(coreColor);
                Fill.circle(b.x, b.y, 2.2f * fout);
            }
            Draw.blend(Blending.additive);
            Draw.color(coreColor, 0.7f * fout);
            Fill.circle(b.x, b.y, 3.4f * fout);
            Draw.color(Color.white, 0.8f * fout);
            Fill.circle(b.x, b.y, 1.4f * fout);
            Draw.blend();
            Draw.reset();
        }

        @Override
        public void handlePierce(Bullet b, float initialHealth, float x, float y){
            float sub = Math.max(initialHealth * pierceDamageFactor, damage * 0.35f);
            if(b.damage > 0f){
                OutpostFx.hitLight.at(x, y, b.rotation(), hitColor);
            }
            b.damage -= Math.min(b.damage, sub);
            if(b.damage <= 0f){
                b.hit = true;
                b.remove();
            }
        }
    }
}
