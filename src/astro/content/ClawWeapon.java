package astro.content;

import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

/**
 * Weapon that fires from the exact place a claw of the drawn model is. Side claws throw energy orbs (their
 * damage grows with the stored charge and each shot costs a little); the back claw is the discharge cannon and
 * only fires when enough energy is stored.
 */
public class ClawWeapon extends Weapon{
    public final int claw;
    public final boolean cannon;

    public ClawWeapon(int claw, boolean cannon){
        super("");
        this.claw = claw;
        this.cannon = cannon;
        mirror = false;
        rotate = true;
        rotateSpeed = 14f;
        shootCone = 50f;
        top = false;
        alternate = false;
        ignoreRotation = true;
        shootOnDeath = false;
        useAttackRange = true;
        //the weapons find and track their own targets: the pilot (or the AI) only has to fly
        controllable = false;
        autoTarget = true;
        //never fire backwards: the body is turned to the target by the unit's own logic
        rotationLimit = 170f;
        targetInterval = 6f;
        targetSwitchInterval = 14f;
    }

    /** is a world point in front of the body (within limit degrees of the heading, measured from the body centre)? */
    static boolean front(Unit unit, float x, float y, float limit){
        return Angles.angleDist(unit.angleTo(x, y), unit.rotation) <= limit;
    }

    /** claws only pick targets in front of the body (the body turns to its enemies first): never behind or beside the hull */
    @Override
    protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
        if(unit.isPlayer() && unit.type instanceof DetainerType t){
            //a pilot owns this weapon: it only fires on his command, at the target he chose
            DetainerType.Logic L = t.logic(unit);
            if(cannon){
                boolean full = L.energy >= t.maxEnergy * 0.985f;
                if(L.req[DetainerType.AB_LANCE] > 0f || L.req[DetainerType.AB_OVERLOAD] > 0f) return L.reqT != null ? L.reqT : DetainerCtl.manualTarget(unit, 520f);
                if(!(DetainerCtl.aiMay(unit, L, DetainerType.AB_LANCE) || full && DetainerCtl.aiMay(unit, L, DetainerType.AB_OVERLOAD))) return null;
            }else if(!DetainerCtl.aiMay(unit, L, DetainerType.AB_ORB)){
                if(!L.orbOn) return null;
                return L.reqT != null ? L.reqT : DetainerCtl.manualTarget(unit, range);
            }
        }
        return Units.closestTarget(unit.team, x, y, range + Math.abs(shootY),
            u -> u.checkTarget(air, ground) && front(unit, u.x, u.y, 82f),
            t -> ground && front(unit, t.x, t.y, 82f) && (unit.type.targetUnderBlocks || !t.block.underBullets));
    }

    @Override
    protected boolean checkTarget(Unit unit, Teamc target, float x, float y, float range){
        return super.checkTarget(unit, target, x, y, range) || !front(unit, target.getX(), target.getY(), 90f);
    }

    @Override
    protected float bulletRotation(Unit unit, WeaponMount mount, float bulletX, float bulletY){
        return Angles.angle(bulletX, bulletY, mount.aimX, mount.aimY);
    }

    @Override
    protected void bullet(Unit unit, WeaponMount mount, float xOffset, float yOffset, float angleOffset, Mover mover){
        if(!unit.isAdded() || !(unit.type instanceof DetainerType type)) return;
        DetainerType.Logic L = type.logic(unit);
        float[] p = new float[2];
        type.muzzleWorld(unit, L, claw, p);
        float bx = p[0], by = p[1];
        float ang = bulletRotation(unit, mount, bx, by) + angleOffset;
        //last line of defence against rear attacks: a shot that would leave the claw backwards is not fired
        if(Angles.angleDist(ang, unit.rotation) > 110f){
            mount.reload = 6f;
            return;
        }
        float dmg;
        BulletType bt = bullet;
        float volume = 1f;
        if(cannon){
            if(L.energy < type.lanceMin){
                mount.reload = 20f; //not charged: check again shortly
                return;
            }
            boolean full = L.energy >= type.maxEnergy * 0.985f;
            boolean over = full;
            boolean asked = false;
            if(unit.isPlayer()){
                boolean lr = L.req[DetainerType.AB_LANCE] > 0f, orq = L.req[DetainerType.AB_OVERLOAD] > 0f;
                if(orq && full){ over = true; asked = true; }
                else if(lr){ over = false; asked = true; }
                else if(full && DetainerCtl.aiMay(unit, L, DetainerType.AB_OVERLOAD)) over = true;
                else if(DetainerCtl.aiMay(unit, L, DetainerType.AB_LANCE)) over = false;
                else{ mount.reload = 10f; return; }
                if(asked) L.req[over ? DetainerType.AB_OVERLOAD : DetainerType.AB_LANCE] = 0f;
            }
            float take = over ? L.energy : Math.min(L.energy, type.lanceTake);
            dmg = over ? type.overloadDamage : type.lanceBase + take * type.lanceScale;
            L.energy -= take;
            bt = over ? type.overload : type.lance;
            L.kick[claw] = 1f;
            if(over){ L.overloads++; L.overloadFlash = 1f; }else L.lances++;
            AstroFx.dischargeMuzzle.at(bx, by, over ? 200f : 60f, AstroFx.gold);
            Effect.shake(over ? 14f : 5f, over ? 40f : 14f, bx, by);
        }else{
            if(unit.isPlayer() && !DetainerCtl.aiMay(unit, L, DetainerType.AB_ORB) && !L.orbOn){ mount.reload = 6f; return; }
            if(L.energy < type.orbCost){
                mount.reload = 15f; //nothing absorbed yet: the claws only throw what they have stored
                return;
            }
            float ef = L.energy / type.maxEnergy;
            dmg = type.orbBase * (1f + type.orbBonus * ef);
            L.energy = Math.max(0f, L.energy - type.orbCost);
            L.kick[claw] = 1f;
        }
        Entityc shooter = unit;
        float baseLife = 1f;
        float lifeScl = bt.scaleLife ? baseLife * Mathf.clamp(Mathf.dst(bx, by, mount.aimX, mount.aimY) / bt.range) : baseLife;
        float angle = ang + Mathf.range(inaccuracy + bt.inaccuracy);
        mount.bullet = bt.create(unit, shooter, unit.team, bx, by, angle, dmg, 1f, lifeScl, null, mover, mount.aimX, mount.aimY, mount.target);
        handleBullet(unit, mount, mount.bullet);
        shootSound.at(bx, by, Mathf.random(soundPitchMin, soundPitchMax), shootSoundVolume);
        mount.recoil = 1f;
        mount.heat = 1f;
    }
}
