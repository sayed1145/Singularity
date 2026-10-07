package outpost.world;

import arc.math.Mathf;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.Mover;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.Turret;
import outpost.gfx.LancerModel;

/** Light rapid turret; same recoil-aware spawn correction as the heavy railgun. */
public class LancerTurret extends ItemTurret{
    public LancerTurret(String name){
        super(name);
        drawer = new DrawModel(LancerModel.instance);
        buildType = LancerBuild::new;
    }

    public class LancerBuild extends ItemTurretBuild{
        @Override
        protected void bullet(BulletType type, float xOffset, float yOffset, float angleOffset, Mover mover){
            float pow = 1.8f, dist = 1.4f;
            if(block instanceof Turret t){ pow = t.recoilPow; dist = t.recoil; }
            float back = Mathf.pow(Mathf.clamp(curRecoil), pow) * dist;
            super.bullet(type, xOffset, yOffset - back, angleOffset, mover);
        }
    }
}
