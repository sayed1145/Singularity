package outpost.world;

import arc.math.Mathf;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.Mover;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.Turret;
import outpost.gfx.RailgunModel;

/**
 * Heavy bastion turret. Vanilla ItemTurret targeting/ammo/netcode; only the
 * draw path is 3D, and the bullet spawn is shifted back by the same recoil
 * distance the 3D barrel travels, so muzzle flash and projectile agree.
 */
public class RailgunTurret extends ItemTurret{
    public RailgunTurret(String name){
        super(name);
        drawer = new DrawModel(RailgunModel.instance);
        buildType = RailgunBuild::new;
    }

    public class RailgunBuild extends ItemTurretBuild{
        @Override
        protected void bullet(BulletType type, float xOffset, float yOffset, float angleOffset, Mover mover){
            float pow = 1.8f, dist = 2.2f;
            if(block instanceof Turret t){ pow = t.recoilPow; dist = t.recoil; }
            float back = Mathf.pow(Mathf.clamp(curRecoil), pow) * dist;
            super.bullet(type, xOffset, yOffset - back, angleOffset, mover);
        }
    }
}
