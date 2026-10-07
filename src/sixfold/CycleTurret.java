package sixfold;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.entities.bullet.BulletType;
import mindustry.ui.Bar;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;

/** Original "harmonic" mechanic: a no-ammo power turret whose volleys cycle through a
 * fixed sequence of distinct shots (kinetic / thermal / volt). The cycle index derives
 * from the synced shot counter. v3.5: the build is now operable - tap to open a config
 * that locks the turret to a single harmonic or returns it to auto cycling; the choice
 * is synced, saved and shown in a status bar. */
public class CycleTurret extends PowerTurret{
    public static final Stat harmonicStat = new Stat("sixfold-harmonic", StatCat.function);

    public final Seq<BulletType> cycle = new Seq<>();
    public Color[] harmonicColors = {
        Color.valueOf("9ecbff"), Color.valueOf("ff9e5c"), Color.valueOf("bb9bee")
    };

    public CycleTurret(String name){
        super(name);
        buildType = CycleTurretBuild::new;
        config(Integer.class, (CycleTurretBuild b, Integer i) -> b.mode = Math.max(0, Math.min(i, cycle.size)));
        addBar("harmonic", b -> new Bar(
            () -> Core.bundle.format("sixfold.loom.mode", ((CycleTurretBuild)b).modeName()),
            () -> harmonicColors[((CycleTurretBuild)b).currentIndex()],
            () -> 1f));
    }

    @Override
    public void init(){
        // The info page derives damage stats from shootType; default it to the first
        // harmonic so the turret always displays complete stats.
        if(shootType == null && cycle.size > 0) shootType = cycle.first();
        super.init();
    }

    @Override
    public void setStats(){
        super.setStats();
        for(int i = 0; i < cycle.size; i++){
            BulletType b = cycle.get(i);
            int n = i + 1;
            stats.add(harmonicStat, t -> t.add(Core.bundle.format("stat.sixfold-harmonic.detail",
                n, (int)b.damage)).left());
        }
    }

    public class CycleTurretBuild extends TurretBuild{
        /** 0 = auto cycle, 1..N = locked harmonic */
        public int mode;

        public CycleTurretBuild(){
            super();
        }

        public int currentIndex(){
            return mode == 0 ? totalShots % cycle.size : Math.min(mode, cycle.size) - 1;
        }

        public String modeName(){
            return mode == 0 ? Core.bundle.get("sixfold.loom.auto") : Core.bundle.format("sixfold.loom.h", mode);
        }

        @Override
        public boolean hasAmmo(){
            return true;
        }

        @Override
        public BulletType peekAmmo(){
            return cycle.get(currentIndex());
        }

        @Override
        public BulletType useAmmo(){
            return peekAmmo();
        }

        @Override
        public void buildConfiguration(Table table){
            table.button(Core.bundle.format("sixfold.loom.mode", modeName()),
                () -> configure((mode + 1) % (cycle.size + 1))).size(230f, 54f);
        }

        @Override
        public Integer config(){
            return mode;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(mode);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            mode = read.b();
        }
    }
}
