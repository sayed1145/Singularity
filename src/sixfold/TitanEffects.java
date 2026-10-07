package sixfold;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import blackhole.OwnFx;
import mindustry.entities.*;

import static arc.graphics.g2d.Draw.*;

/**
 * Aegis Titan effects (v7.1 redo), in the same pseudo-3D language as the models: ground rings are squashed to the
 * camera, debris flies on arcs with shadows, flashes are additive. Nothing forwards to a vanilla Fx.
 */
public class TitanEffects{
    private static final Color mint = Color.valueOf("8ce5d3");
    private static final Color violet = Color.valueOf("b89bff");
    private static final Color dust = Color.valueOf("9aa3a6");

    /** lance charge: converging mint motes and a tightening squashed ring */
    public static final Effect charge = new Effect(42f, 300f, e -> {
        float f = e.fin();
        blend(Blending.additive);
        color(mint, violet, f * 0.5f);
        Draw.alpha(0.35f + f * 0.5f);
        OwnFx.ring(e.x, e.y, 22f * (1f - f) + 3f, 1.8f * (0.3f + f));
        Angles.randLenVectors(e.id, 10, 30f * (1f - f) + 2f, e.rotation, 360f, (x, y) -> {
            Draw.color(mint, 0.6f + 0.4f * f);
            Fill.circle(e.x + x, e.y + y * OwnFx.squash, 0.8f + f * 1.6f);
        });
        Draw.color(Color.white, f * 0.9f);
        Fill.circle(e.x, e.y, 1.5f + f * 3.5f);
        blend();
        reset();
    });

    /** rail beam: bright core, soft mint sheath, sparks along the line */
    public static final Effect railLine = new Effect(22f, 600f, e -> {
        if(!(e.data instanceof Vec2 end)) return;
        float fo = e.fout();
        blend(Blending.additive);
        Draw.color(mint, 0.45f * fo);
        Lines.stroke(9f * fo + 0.5f);
        Lines.line(e.x, e.y, end.x, end.y);
        Draw.color(mint, fo);
        Lines.stroke(4f * fo + 0.3f);
        Lines.line(e.x, e.y, end.x, end.y);
        Draw.color(Color.white, fo);
        Lines.stroke(1.4f * fo);
        Lines.line(e.x, e.y, end.x, end.y);
        float len = Mathf.dst(e.x, e.y, end.x, end.y), ang = Angles.angle(e.x, e.y, end.x, end.y);
        OwnFx.rand().setSeed(e.id);
        for(int i = 0; i < 14; i++){
            float d = OwnFx.rand().random(len), side = OwnFx.rand().range(1f) * 9f * e.fin();
            float px = e.x + Angles.trnsx(ang, d, side), py = e.y + Angles.trnsy(ang, d, side);
            Draw.color(mint, fo);
            Fill.square(px, py + e.fin() * 5f * OwnFx.lift, 1.1f * fo, ang + d);
        }
        blend();
        reset();
    });

    /** impact: flash, double ground ring, violet shards on arcs */
    public static final Effect fracture = new Effect(40f, 170f, e -> {
        float f = e.finpow();
        OwnFx.flash(e.x, e.y, 14f * Mathf.clamp(e.fout() * 2f), mint, e.fout());
        Draw.color(mint, violet, e.fin());
        Draw.alpha(e.fout());
        OwnFx.ring(e.x, e.y, 5f + f * 30f, 2.6f * e.fout());
        OwnFx.ring(e.x, e.y, 3f + f * 18f, 1.2f * e.fout());
        OwnFx.debris(e, 9, 30f, 1.5f, violet);
        reset();
    });

    /** titan footfall: dust ring and pebbles; e.rotation = unit hit size, e.color = ground tint */
    public static final Effect step = new Effect(34f, 120f, e -> {
        float s = Mathf.clamp(e.rotation / 40f, 0.5f, 1.5f);
        Color c = e.color == null || e.color.a <= 0f ? dust : e.color;
        Draw.color(c, 0.5f * e.fout());
        OwnFx.ring(e.x, e.y, (4f + e.finpow() * 14f) * s, 2.2f * e.fout());
        OwnFx.puffs(e, 5, 12f * s, 2.6f * s, dust);
        OwnFx.debris(e, 4, 10f * s, 0.9f, dust);
        reset();
    });

    private TitanEffects(){}
}
