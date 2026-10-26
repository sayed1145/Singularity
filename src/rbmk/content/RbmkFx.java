package rbmk.content;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.*;
import mindustry.graphics.*;
public class RbmkFx{
    public static final Effect rbmkExplosion = new Effect(180f, 620f, e -> {
        float f = e.fin(), fout = e.fout();
        Draw.color(Color.white, Color.valueOf("ffd37f"), f);
        Lines.stroke(10f * fout);
        Lines.circle(e.x, e.y, 18f + f * 230f);
        Draw.color(Color.valueOf("ff7b3d"), Color.valueOf("40302a"), f);
        for(int i=0;i<32;i++){
            float a = i * 360f / 32f + Mathf.sin(i * 9f) * 8f;
            float d = f * (45f + (i%7)*24f);
            Fill.circle(e.x + Angles.trnsx(a,d), e.y + Angles.trnsy(a,d), fout * (12f + i%5));
        }
        Draw.color(Color.white);
        Fill.circle(e.x, e.y, 44f * fout);
        Draw.reset();
    });
}
