package blackhole;

import arc.math.*;
import arc.struct.*;
import mindustry.game.*;

import static blackhole.AureliaContent.*;

/**
 * Aurelia wave generator: only Aurelia units (vanilla Waves.generate would spawn Serpulo units). Ground pressure
 * from shard rovers and aurite bulwarks, air raids by glints and aurora guards, and on harder sectors
 * (threat > 0.3) an aurora heavy spearhead every few waves.
 */
public final class AureliaWaves{
    private AureliaWaves(){}

    public static Seq<SpawnGroup> generate(float threat, Rand rand){
        float d = Mathf.clamp(threat, 0f, 1f);
        //harder sectors ramp faster (lower scaling = more units per wave)
        float ramp = Mathf.lerp(1.25f, 0.7f, d);
        int shift = rand.random(0, 2);
        Seq<SpawnGroup> out = new Seq<>();

        out.add(new SpawnGroup(shardRover){{
            end = 12 + shift;
            unitScaling = 2f * ramp;
            max = 24;
        }});
        out.add(new SpawnGroup(glint){{
            begin = 3 + shift;
            end = 40;
            unitScaling = 2.6f * ramp;
            spacing = 2;
            max = 16;
        }});
        out.add(new SpawnGroup(shardRover){{
            begin = 10 + shift;
            unitScaling = 1.6f * ramp;
            unitAmount = 3;
            shieldScaling = 12f;
            max = 30;
        }});
        out.add(new SpawnGroup(auriteBulwark){{
            begin = 8 + shift;
            unitScaling = 3f * ramp;
            spacing = 2;
            max = 12;
            shieldScaling = 20f;
        }});
        out.add(new SpawnGroup(glint){{
            begin = 16 + shift;
            unitScaling = 1.8f * ramp;
            unitAmount = 4;
            spacing = 3;
            shieldScaling = 10f;
        }});
        out.add(new SpawnGroup(auroraGuard){{
            begin = 20 + shift;
            unitScaling = 3.5f * ramp;
            spacing = 3;
            max = 8;
            shieldScaling = 30f;
        }});
        out.add(new SpawnGroup(auriteBulwark){{
            begin = 26 + shift;
            unitScaling = 2f * ramp;
            unitAmount = 2;
            shieldScaling = 40f;
        }});
        if(d > 0.3f){
            out.add(new SpawnGroup(auroraHeavy){{
                begin = Math.max(14, 30 - (int)(d * 14f)) + shift;
                spacing = Math.max(5, 10 - (int)(d * 5f));
                unitAmount = 1;
                unitScaling = 6f;
                max = 4;
                shields = 200f * d;
                shieldScaling = 60f;
            }});
        }
        return out;
    }
}
