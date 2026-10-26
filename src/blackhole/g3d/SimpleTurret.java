package blackhole.g3d;

import mindustry.world.blocks.defense.turrets.Turret.*;

/** A {@link TurretModel} assembled from two builder lambdas (base and head). */
public class SimpleTurret extends TurretModel{
    public interface Part{ void build(Mesh m); }
    public interface Over{ void draw(SimpleTurret t, TurretBuild b); }
    public interface Glow{ float get(TurretBuild b); }

    public final Kit.Style style;
    public Part base, head;
    public Over over;
    public Glow glow;

    public SimpleTurret(String name, int size, Kit.Style style){
        super(name, size);
        this.style = style;
        glowColor = style.glow;
    }

    public SimpleTurret base(Part p){ base = p; return this; }
    public SimpleTurret head(Part p){ head = p; return this; }
    public SimpleTurret over(Over o){ over = o; return this; }
    public SimpleTurret glow(Glow g){ glow = g; return this; }
    public SimpleTurret muzzles(float... m){ muzzles = m; return this; }
    public SimpleTurret headZ(float z){ headZ = z; return this; }

    @Override
    public void buildStatic(Mesh m){
        base.build(m);
    }

    @Override
    public void buildHead(Mesh m){
        head.build(m);
    }

    @Override
    public float glowAlpha(TurretBuild b){
        return glow == null ? 0f : glow.get(b);
    }

    @Override
    public void drawTurretOver(TurretBuild b){
        if(over != null) over.draw(this, b);
    }
}
