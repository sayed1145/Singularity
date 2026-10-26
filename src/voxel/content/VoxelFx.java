package voxel.content;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.*;
import mindustry.graphics.*;

/**
 * Custom particle / render effects for the Colossus titan.
 *
 * <p>Everything is drawn with {@link Fill}, {@link Lines} and {@link Draw} - the same primitives the vanilla
 * {@code Fx} class uses. There is no GLSL, no framebuffer and no custom {@code Shader} anywhere in here, so
 * these effects cannot fail to compile on an Android GLES driver.
 */
public class VoxelFx{
    public static final Color
        plume = Color.valueOf("9ff0ff"),
        plumeCore = Color.valueOf("ffffff"),
        emberColor = Color.valueOf("ffb45f"),
        dustColor = Color.valueOf("b0a89a"),
        ringColor = Color.valueOf("cfefff");

    /** dust + a small shock ring under a foot that just landed */
    public static final Effect titanStep = new Effect(26f, 90f, e -> {
        Draw.color(dustColor, Color.lightGray, e.fin() * 0.6f);
        Angles.randLenVectors(e.id, 7, 3f + 15f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 1.9f * e.fout() + 0.4f);
        });

        Draw.color(ringColor, 0.5f * e.fout());
        Lines.stroke(1.7f * e.fout());
        Lines.circle(e.x, e.y, 3f + 17f * e.finpow());

        //two dust wedges kicked sideways
        Draw.color(dustColor, 0.55f * e.fout());
        for(int s : Mathf.signs){
            float a = e.rotation + 90f * s;
            float len = 4f + 10f * e.finpow();
            Fill.circle(e.x + Angles.trnsx(a, len), e.y + Angles.trnsy(a, len), 2.4f * e.fout());
        }
        Draw.reset();
    }).layer(Layer.debris);

    /** the titan crouches, the pack floods and it leaves the ground */
    public static final Effect titanLiftoff = new Effect(52f, 220f, e -> {
        //ground blast ring
        Draw.color(ringColor, e.fout(0.35f) * 0.8f);
        Lines.stroke(4.2f * e.fout());
        Lines.circle(e.x, e.y, 6f + 62f * e.finpow());

        Draw.color(plume, plumeCore, e.fin());
        Angles.randLenVectors(e.id, 16, 8f + 60f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 4.6f * e.fout() + 0.6f);
        });

        Draw.color(dustColor, 0.75f * e.fout());
        Angles.randLenVectors(e.id + 1, 22, 10f + 78f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 5.5f * e.fout());
        });

        //hot embers thrown outwards
        Draw.color(emberColor);
        Angles.randLenVectors(e.id + 2, 12, 14f + 70f * e.finpow(), (x, y) -> {
            Fill.square(e.x + x, e.y + y, 1.5f * e.fout(), 45f);
        });
        Draw.reset();
    }).layer(Layer.debris);

    /** heavy touchdown: dust wall, cracks and a bright impact flash */
    public static final Effect titanLand = new Effect(46f, 220f, e -> {
        Draw.color(Color.white, ringColor, e.fin());
        Lines.stroke(5.5f * e.fout());
        Lines.circle(e.x, e.y, 4f + 52f * e.finpow());

        Draw.color(dustColor);
        Angles.randLenVectors(e.id, 20, 6f + 56f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 6.5f * e.fout() + 0.5f);
        });

        //debris shards flung out low
        Draw.color(Color.valueOf("5c5f6a"));
        Angles.randLenVectors(e.id + 3, 10, 8f + 48f * e.finpow(), (x, y) -> {
            Fill.square(e.x + x, e.y + y, 2.6f * e.fout(), Mathf.randomSeed(e.id + 3, 360f));
        });

        Draw.color(plumeCore, e.fout() * 0.65f);
        Fill.circle(e.x, e.y, 13f * e.fout());
        Draw.reset();
    }).layer(Layer.debris);

    /** continuous nozzle plume, spawned at a thruster bone */
    public static final Effect titanThrust = new Effect(17f, 70f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(plume, plumeCore, e.fin());
        float w = 3.4f * e.fout() * (0.7f + Mathf.randomSeed(e.id, 0.6f));
        Fill.circle(e.x, e.y, w);
        Draw.color(plumeCore, 0.55f * e.fout());
        Fill.circle(e.x, e.y, w * 0.45f);
        Draw.blend();

        Draw.color(plume, 0.35f * e.fout());
        Angles.randLenVectors(e.id, 2, 2f + 9f * e.finpow(), e.rotation, 26f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 1.5f * e.fout());
        });
        Draw.reset();
    }).layer(Layer.flyingUnit + 0.02f);

    /** hover downwash on the ground below the titan */
    public static final Effect titanDownwash = new Effect(30f, 120f, e -> {
        Draw.color(dustColor, 0.5f * e.fout());
        Lines.stroke(1.6f * e.fout());
        Lines.circle(e.x, e.y, 5f + 26f * e.finpow());
        Angles.randLenVectors(e.id, 4, 4f + 24f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 2.2f * e.fout());
        });
        Draw.reset();
    }).layer(Layer.debris);

    /** shoulder cannon muzzle blast */
    public static final Effect titanShoot = new Effect(21f, 100f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(plumeCore, plume, e.fin());
        //muzzle star
        for(int i = 0; i < 4; i++){
            float rot = e.rotation + 90f * i + e.fin() * 18f;
            float len = (i % 2 == 0 ? 21f : 10f) * e.fout();
            Drawf.tri(e.x, e.y, 5.5f * e.fout(), len, rot);
        }
        Fill.circle(e.x, e.y, 5.4f * e.fout());
        Draw.blend();

        //cone of sparks down the barrel line
        Draw.color(plume);
        Angles.randLenVectors(e.id, 9, 9f + 42f * e.finpow(), e.rotation, 17f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 2.1f * e.fout());
        });

        Draw.color(Color.gray, 0.45f * e.fout());
        Angles.randLenVectors(e.id + 5, 5, 6f + 26f * e.finpow(), e.rotation, 30f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 3.4f * e.fout());
        });
        Draw.reset();
    });

    /** charge glow that builds up in the barrel before the shot */
    public static final Effect titanCharge = new Effect(38f, 80f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(plume, plumeCore, e.fin());
        Fill.circle(e.x, e.y, 4.6f * e.fin());
        Lines.stroke(1.2f * e.fin());
        Lines.circle(e.x, e.y, 13f * e.fout() + 3f);
        Angles.randLenVectors(e.id, 6, 20f * e.fout(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 1.5f * e.fin());
        });
        Draw.blend();
        Draw.reset();
    });

    /** heat vented out of the side panels */
    public static final Effect titanVent = new Effect(34f, 70f, e -> {
        Draw.color(emberColor, Color.lightGray, e.fin());
        Angles.randLenVectors(e.id, 3, 2f + 13f * e.finpow(), e.rotation, 34f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 2.6f * e.fslope() + 0.3f);
        });
        Draw.reset();
    }).layer(Layer.flyingUnit + 0.01f);

    /** damaged-armour sparks */
    public static final Effect titanSpark = new Effect(24f, 60f, e -> {
        Draw.color(emberColor, Color.white, e.fin());
        Angles.randLenVectors(e.id, 4, 2f + 14f * e.finpow(), (x, y) -> {
            Lines.stroke(1.1f * e.fout());
            Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 2.6f * e.fout());
        });
        Draw.reset();
    }).layer(Layer.flyingUnit + 0.03f);

    /** shield-like ring that flashes when the titan is hit hard */
    public static final Effect titanImpact = new Effect(22f, 90f, e -> {
        Draw.color(ringColor, e.fout() * 0.7f);
        Lines.stroke(2.4f * e.fout());
        Lines.circle(e.x, e.y, 10f + 26f * e.finpow());
        Draw.reset();
    });

    /** gameplay slam: a heavy shockwave ring + ground cracks + debris column on a flight landing */
    public static final Effect titanSlam = new Effect(44f, 300f, e -> {
        //outer shock ring, double for weight
        Draw.color(Color.white, ringColor, e.fin());
        Lines.stroke(6.5f * e.fout());
        Lines.circle(e.x, e.y, 5f + 74f * e.finpow());
        Draw.color(ringColor, 0.55f * e.fout());
        Lines.stroke(3.2f * e.fout());
        Lines.circle(e.x, e.y, 2f + 48f * e.finpow());

        //ground cracks radiating out
        Draw.color(Color.valueOf("3d414c"), 0.85f * e.fout());
        Lines.stroke(2.2f * e.fout());
        for(int i = 0; i < 8; i++){
            float a = i * 45f + e.id % 17;
            float len = 16f + 40f * e.finpow() + (i % 3) * 7f;
            float cx = e.x + Angles.trnsx(a, len * 0.55f);
            float cy = e.y + Angles.trnsy(a, len * 0.55f);
            Lines.lineAngle(cx, cy, a + 12f, len * 0.5f);
        }

        //dust wall
        Draw.color(dustColor, 0.85f * e.fout());
        Angles.randLenVectors(e.id, 26, 8f + 66f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 5.8f * e.fout() + 0.5f);
        });

        //hot debris kicked straight up
        Draw.color(emberColor, Color.white, e.fin());
        Angles.randLenVectors(e.id + 7, 14, 4f + 40f * e.finpow(), (x, y) -> {
            Fill.square(e.x + x, e.y + y, 2.2f * e.fout(), 45f);
        });

        //impact flash
        Draw.color(plumeCore, e.fout() * 0.7f);
        Fill.circle(e.x, e.y, 15f * e.fout());
        Draw.reset();
    }).layer(Layer.debris);

    /** missile popping out of a back rack: white flash, smoke sleeve, sparks along the flight path */
    public static final Effect titanMissileLaunch = new Effect(18f, 90f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(plumeCore, plume, e.fin());
        Fill.circle(e.x, e.y, 4.8f * e.fout() + 1f);
        for(int i = 0; i < 3; i++){
            float rot = e.rotation + 120f * i;
            Drawf.tri(e.x, e.y, 3.4f * e.fout(), 9f * e.fout(), rot);
        }
        Draw.blend();

        //launch smoke sleeve trailing behind the missile
        Draw.color(Color.gray, 0.55f * e.fout());
        Angles.randLenVectors(e.id, 5, 4f + 18f * e.finpow(), e.rotation, 22f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 2.6f * e.fslope() + 0.4f);
        });

        //split sparks
        Draw.color(emberColor);
        Angles.randLenVectors(e.id + 3, 6, 6f + 24f * e.finpow(), e.rotation, 30f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 1.2f * e.fout());
        });
        Draw.reset();
    }).layer(Layer.flyingUnit + 0.02f);

    /** compact muzzle flash for the arm autocannons */
    public static final Effect titanGunFlash = new Effect(11f, 60f, e -> {
        Draw.blend(Blending.additive);
        Draw.color(plumeCore, plume, e.fin());
        Fill.circle(e.x, e.y, 3.1f * e.fout());
        float len = 11f * e.fout();
        Drawf.tri(e.x, e.y, 2.6f * e.fout(), len, e.rotation);
        Drawf.tri(e.x, e.y, 2.0f * e.fout(), len * 0.6f, e.rotation + 180f);
        Draw.blend();

        Draw.color(plume);
        Angles.randLenVectors(e.id, 4, 4f + 14f * e.finpow(), e.rotation, 20f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 1.0f * e.fout());
        });
        Draw.reset();
    });

    public static void load(){
        //effects register themselves in Effect.all when the class loads; this just forces that
    }
}
