package blackhole;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.abilities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

/**
 * Prism ward: the mod's own projectile shield (replaces vanilla ForceFieldAbility and its hardcoded effects).
 * A faceted hexagonal prism drawn in the models' 3D language: a squashed hexagonal floor ring, six glass facets
 * rising from it with a brighter crown edge, and shard effects (OwnFx) on absorption and on break.
 */
public class PrismWardAbility extends Ability{
    public float radius = 60f, regen = 0.1f, max = 200f, cooldown = 300f;
    public Color color = Color.valueOf("b69cff");
    /** prism height in world units (drawn with the camera's 0.447 lift) */
    public float height = 10f;

    protected float radiusScale, alpha;
    protected boolean wasBroken = true;

    private static float realRad;
    private static Unit paramUnit;
    private static PrismWardAbility paramField;
    private static final Cons<Bullet> consumer = b -> {
        if(b.team != paramUnit.team && b.type.absorbable && paramUnit.shield > 0
            && Intersector.isInRegularPolygon(6, paramUnit.x, paramUnit.y, realRad, 30f, b.x, b.y)){
            b.absorb();
            OwnFx.hit.at(b.x, b.y, 0f, paramField.color);
            paramUnit.shield -= b.type.shieldDamage(b);
            paramField.alpha = 1f;
        }
    };

    public PrismWardAbility(float radius, float regen, float max, float cooldown){
        this.radius = radius;
        this.regen = regen;
        this.max = max;
        this.cooldown = cooldown;
    }

    PrismWardAbility(){}

    public float scaledMax(Unit unit){
        return max * Vars.state.rules.unitHealth(unit.team);
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(Core.bundle.format("bullet.range", Strings.autoFixed(radius / tilesize, 2)));
        t.row();
        t.add(abilityStat("shield", Strings.autoFixed(max, 2)));
        t.row();
        t.add(abilityStat("repairspeed", Strings.autoFixed(regen * 60f, 2)));
        t.row();
        t.add(abilityStat("cooldown", Strings.autoFixed(cooldown / 60f, 2)));
    }

    @Override
    public void update(Unit unit){
        if(unit.shield <= 0f && !wasBroken){
            unit.shield -= cooldown * regen;
            OwnFx.shield.at(unit.x, unit.y, radius, color);
        }
        wasBroken = unit.shield <= 0f;
        if(unit.shield < scaledMax(unit)) unit.shield += Time.delta * regen;
        alpha = Math.max(alpha - Time.delta / 10f, 0f);
        if(unit.shield > 0){
            radiusScale = Mathf.lerpDelta(radiusScale, 1f, 0.06f);
            paramUnit = unit;
            paramField = this;
            realRad = radiusScale * radius;
            Groups.bullet.intersect(unit.x - realRad, unit.y - realRad, realRad * 2f, realRad * 2f, consumer);
        }else{
            radiusScale = 0f;
        }
    }

    @Override
    public void death(Unit unit){
        if(unit.shield > 0f && !wasBroken) OwnFx.shield.at(unit.x, unit.y, radius, color);
    }

    @Override
    public void draw(Unit unit){
        if(unit.shield <= 0) return;
        float r = radiusScale * radius;
        if(r < 1f) return;
        float z = Draw.z();
        Draw.z(Layer.effect - 0.6f);
        float lift = height * 0.447f, sq = 0.894f;
        float hit = Mathf.clamp(alpha), t = Time.time * 0.6f;
        float[] xs = new float[6], ys = new float[6];
        for(int i = 0; i < 6; i++){
            float a = 30f + i * 60f;
            xs[i] = unit.x + Mathf.cosDeg(a) * r;
            ys[i] = unit.y + Mathf.sinDeg(a) * r * sq;
        }
        Draw.blend(Blending.additive);
        //facets: front ones brighter (the camera looks north, so south facets face it)
        for(int i = 0; i < 6; i++){
            int j = (i + 1) % 6;
            float mid = 30f + i * 60f + 30f;
            float facing = 0.5f - 0.5f * Mathf.sinDeg(mid);
            Draw.color(color, 0.05f + facing * 0.07f + hit * 0.18f);
            Fill.quad(xs[i], ys[i], xs[j], ys[j], xs[j], ys[j] + lift, xs[i], ys[i] + lift);
        }
        //floor ring + crown edge + vertical ribs
        Lines.stroke(1.1f);
        Draw.color(color, 0.35f + hit * 0.4f);
        for(int i = 0; i < 6; i++){
            int j = (i + 1) % 6;
            Lines.line(xs[i], ys[i], xs[j], ys[j]);
            Lines.line(xs[i], ys[i] + lift, xs[j], ys[j] + lift);
        }
        Draw.color(color, 0.18f + hit * 0.3f);
        Lines.stroke(0.7f);
        for(int i = 0; i < 6; i++) Lines.line(xs[i], ys[i], xs[i], ys[i] + lift);
        //travelling glint along the crown
        float g = (t % 60f) / 60f * 6f;
        int gi = (int)g;
        float gf = g - gi;
        int gj = (gi + 1) % 6;
        Draw.color(Color.white, 0.5f);
        Fill.circle(Mathf.lerp(xs[gi], xs[gj], gf), Mathf.lerp(ys[gi], ys[gj], gf) + lift, 1.2f);
        Draw.blend();
        Draw.color();
        Draw.z(z);
    }

    @Override
    public void displayBars(Unit unit, Table bars){
        bars.add(new Bar("stat.shieldhealth", Pal.accent, () -> unit.shield / scaledMax(unit))).row();
    }

    @Override
    public void created(Unit unit){
        unit.shield = scaledMax(unit);
    }

    /**
     * v8.1: every use of this ability is an anonymous subclass, whose simple class name is empty - the stat
     * panel then showed nothing at all. The display name is pinned to one bundle key instead.
     */
    @Override
    public String localized(){
        return arc.Core.bundle.get("ability.prismward");
    }
}
