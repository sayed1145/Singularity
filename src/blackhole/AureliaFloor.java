package blackhole;

import arc.math.*;
import arc.util.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

/** A normal cached terrain tile with a deliberately sparse, low-cost pulse on Resonance Beds. */
public class AureliaFloor extends Floor{
    public boolean pulse;
    public float pulseInterval = 100f;

    public AureliaFloor(String name){
        super(name);
    }

    @Override
    public boolean updateRender(Tile tile){
        // Only one out of sixteen tiles receives a live update. Terrain remains static
        // and legible, while the field still communicates that it is a special place.
        return pulse && (Mathf.randomSeed(tile.pos(), 0, 15) == 0);
    }

    @Override
    public void renderUpdate(UpdateRenderState state){
        state.data += Time.delta;
        if(state.data >= pulseInterval){
            state.data %= pulseInterval;
            AureliaFx.terrainPulse.at(state.tile.worldx(), state.tile.worldy(), Mathf.randomSeed(state.tile.pos(), 360f));
        }
    }
}
