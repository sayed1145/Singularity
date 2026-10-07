package astro.ai;

import arc.struct.*;
import mindustry.entities.bullet.*;
import mindustry.type.*;

/**
 * Cached combat profile of a unit type: what it can hit, how hard, how far and which battlefield role it plays.
 * Used by the brain for matchup-aware exchange models (an anti-ground unit adds nothing against air) and for
 * role-specific orders (anti-air, siege, support, tank, skirmisher).
 */
final class UnitProfile{
    static final int LINE = 0, TANK = 1, AA = 2, SIEGE = 3, SUPPORT = 4, SKIRM = 5;
    static final String[] ROLE_NAMES = {"line", "tank", "aa", "siege", "support", "skirmisher"};

    private static final ObjectMap<UnitType, UnitProfile> cache = new ObjectMap<>();

    float dps, perHit, range, speed, hp, armor, hitSize;
    boolean hitsAir, hitsGround, flying;
    int role;

    static UnitProfile of(UnitType t){
        UnitProfile p = cache.get(t);
        if(p == null){
            p = new UnitProfile(t);
            cache.put(t, p);
        }
        return p;
    }

    static void clear(){
        cache.clear();
    }

    private UnitProfile(UnitType t){
        dps = Math.max(t.estimateDps(), 1f);
        range = Math.max(t.maxRange, 24f);
        speed = t.speed;
        hp = t.health;
        armor = t.armor;
        hitSize = t.hitSize;
        flying = t.flying;
        boolean artillery = false, anyWeapon = false;
        for(var w : t.weapons){
            if(w.bullet == null) continue;
            anyWeapon = true;
            BulletType b = w.bullet;
            float hit = b.damage + b.splashDamage * 0.6f + b.lightningDamage * 0.3f;
            perHit = Math.max(perHit, hit);
            if(b.collidesAir && t.targetAir) hitsAir = true;
            if(b.collidesGround && t.targetGround) hitsGround = true;
            if(b instanceof ArtilleryBulletType) artillery = true;
        }
        if(!anyWeapon){ hitsAir = t.targetAir; hitsGround = t.targetGround; }
        if(perHit <= 0f) perHit = 10f;
        boolean fast = speed >= 1.0f && hp < 700f;
        if(t.canHeal) role = SUPPORT;
        else if(artillery || (range >= 190f && !hitsAir)) role = SIEGE;
        else if(hitsAir && !hitsGround) role = AA;
        else if(armor >= 6f || hp >= 1200f) role = TANK;
        else if(fast || t.flying) role = SKIRM;
        else role = LINE;
    }

    /** fraction of one hit that survives the armor (vanilla: never below a quarter) */
    static float armorFactor(float perHit, float armor){
        return Math.max(0.25f, (perHit - armor) / Math.max(perHit, 1f));
    }
}
