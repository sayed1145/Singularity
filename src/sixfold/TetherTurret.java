package sixfold;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.ui.Bar;
import mindustry.world.blocks.defense.turrets.PowerTurret;

/** Gravwell turret host: v3.5 makes the turret operable - tap to toggle the gravity
 * pull on/off (slow still applies when pull is off). Synced, saved, and shown in a
 * status bar so the block always reads as active/controllable. */
public class TetherTurret extends PowerTurret{
    private static final Color steel = Color.valueOf("9ecbff");

    public TetherTurret(String name){
        super(name);
        buildType = TetherTurretBuild::new;
        config(Integer.class, (TetherTurretBuild b, Integer v) -> b.pull = v != 0);
        addBar("pull", b -> new Bar(
            () -> Core.bundle.format("sixfold.well.pull",
                Core.bundle.get(((TetherTurretBuild)b).pull ? "sixfold.on" : "sixfold.off")),
            () -> steel,
            () -> ((TetherTurretBuild)b).pull ? 1f : 0.25f));
    }

    @Override
    public void setStats(){
        super.setStats();
        TetherBulletType tb = (TetherBulletType)shootType;
        stats.add(TetherBulletType.tetherStat, t -> t.add(Core.bundle.format("stat.sixfold-tether.detail",
            Math.round(tb.tetherRadius / 8f), Math.round(tb.pull * 1000f), (int)tb.bonusPerEnemy)).left());
    }

    public class TetherTurretBuild extends PowerTurretBuild{
        public boolean pull = true;

        public TetherTurretBuild(){
            super();
        }

        @Override
        public void buildConfiguration(Table table){
            table.button(Core.bundle.format("sixfold.well.pull",
                Core.bundle.get(pull ? "sixfold.on" : "sixfold.off")),
                () -> configure(pull ? 0 : 1)).size(230f, 54f);
        }

        @Override
        public Integer config(){
            return pull ? 1 : 0;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(pull ? 1 : 0);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            pull = read.b() != 0;
        }
    }
}
