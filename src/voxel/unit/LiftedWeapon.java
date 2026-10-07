package voxel.unit;

import arc.math.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import voxel.gfx.*;

/**
 * A weapon whose muzzle is part of a 3D model standing {@link #muzzleZ} world units above the ground.
 *
 * <p>Vanilla spawns bullets on the ground plane under the gun; with a perspective camera the drawn barrel of a
 * tall mech sits well north of that point, so shots appeared to come out of the mech's feet. Here the spawn
 * point is moved to where the camera actually shows the muzzle, and the shot is re-aimed from there to the
 * aim point, so projectiles leave the barrel and still land exactly where the player / AI aimed.
 * Pure geometry: identical on server and client, no rendering state involved.
 */
public class LiftedWeapon extends Weapon{
    /** muzzle height above the unit's ground point, world units (flying height is added automatically) */
    public float muzzleZ = 10f;
    /** extra height per unit of elevation (the unit type's fly height) */
    public float flyZ = 0f;
    /** set by the unit type: its camera */
    public Cam cam;

    public LiftedWeapon(){
        super();
    }

    public LiftedWeapon(String name){
        super(name);
    }

    protected float height(Unit unit){
        return muzzleZ + flyZ * Mathf.clamp(unit.elevation);
    }

    @Override
    protected float bulletRotation(Unit unit, WeaponMount mount, float bulletX, float bulletY){
        //re-aim from the lifted muzzle unless the aim point is inside the lean (would flip the shot)
        if(cam != null && Mathf.dst(bulletX, bulletY, mount.aimX, mount.aimY) > 12f){
            float base = super.bulletRotation(unit, mount, bulletX, bulletY);
            float to = Angles.angle(bulletX, bulletY, mount.aimX, mount.aimY);
            //never bend a shot by more than 40 degrees from where the gun points
            float d = Angles.angleDist(base, to);
            if(d < 40f) return to;
            return Angles.moveToward(base, to, 40f);
        }
        return super.bulletRotation(unit, mount, bulletX, bulletY);
    }

    @Override
    protected void bullet(Unit unit, WeaponMount mount, float xOffset, float yOffset, float angleOffset, Mover mover){
        if(!unit.isAdded()) return;

        mount.charging = false;
        float
        xSpread = Mathf.range(xRand),
        ySpread = Mathf.range(yRand),
        weaponRotation = unit.rotation - 90 + (rotate ? mount.rotation : baseRotation),
        mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
        mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y),
        bulletX = mountX + Angles.trnsx(weaponRotation, this.shootX + xOffset + xSpread, this.shootY + yOffset + ySpread),
        bulletY = mountY + Angles.trnsy(weaponRotation, this.shootX + xOffset + xSpread, this.shootY + yOffset + ySpread);

        if(cam != null){
            float h = height(unit);
            float s = cam.D / Math.max(cam.D - h, 1f);
            float rx = bulletX - unit.x, ry = bulletY - unit.y;
            bulletX = unit.x + rx * s;
            bulletY = unit.y + cam.cy + (ry - cam.cy) * s;
        }

        float
        shootAngle = bulletRotation(unit, mount, bulletX, bulletY) + angleOffset,
        baseLife = (1f - lifeRnd) + Mathf.random(lifeRnd) + extraLife,
        lifeScl = bullet.scaleLife ? baseLife * Mathf.clamp(Mathf.dst(bulletX, bulletY, mount.aimX, mount.aimY) / bullet.range) : baseLife,
        angle = shootAngle + Mathf.range(inaccuracy + bullet.inaccuracy);

        Entityc shooter = unit.controller() instanceof MissileAI ai ? ai.shooter : unit;
        mount.bullet = bullet.create(unit, shooter, unit.team, bulletX, bulletY, angle, -1f, (1f - velocityRnd) + Mathf.random(velocityRnd) + extraVelocity, lifeScl, null, mover, mount.aimX, mount.aimY, mount.target);
        handleBullet(unit, mount, mount.bullet);

        if(!continuous){
            shootSound.at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax), shootSoundVolume);
        }else{
            initialShootSound.at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax), shootSoundVolume);
        }

        if(mount.allowShootEffects){
            ejectEffect.at(mountX, mountY, angle * Mathf.sign(this.x));
            bullet.shootEffect.at(bulletX, bulletY, angle, bullet.hitColor, unit);
            bullet.smokeEffect.at(bulletX, bulletY, angle, bullet.hitColor, unit);
        }

        unit.vel.add(Tmp.v1.trns(shootAngle + 180f, bullet.recoil));
        Effect.shake(shake, shake, bulletX, bulletY);
        mount.recoil = 1f;
        if(recoils > 0){
            mount.recoils[mount.barrelCounter % recoils] = 1f;
        }
        mount.heat = 1f;
    }
}
