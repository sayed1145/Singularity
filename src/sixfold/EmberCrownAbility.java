package sixfold;

import arc.util.Time;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;

/** Stateless "wounded fury" nova: every {@link #interval} ticks (offset per unit id so a
 * group of Titans pulses out of phase) the crown flares, scorching and igniting nearby
 * enemies. The flare grows stronger as the Titan loses hull - the crown burns brightest
 * when the colossus is wounded. No per-unit mutable state is stored, so this is safe for
 * pooled entities, save files and multiplayer. */
public class EmberCrownAbility extends Ability{
    public float radius = 120f;
    public float maxDamage = 130f;
    public float interval = 42f;
    public float burnDuration = 150f;

    public EmberCrownAbility(float radius, float maxDamage, float interval, float burnDuration){
        this.radius = radius;
        this.maxDamage = maxDamage;
        this.interval = interval;
        this.burnDuration = burnDuration;
    }

    @Override
    public void update(Unit unit){
        if(unit.dead()) return;
        int iv = (int)interval;
        if(((int)Time.time) % iv != ((unit.id % iv) + iv) % iv) return;

        // 0.35..1.0 fury: a pristine Titan smolders, a wounded one detonates.
        float fury = 0.35f + 0.65f * (1f - unit.healthf());
        EmberEffects.crown.at(unit.x, unit.y, radius / 120f);
        Units.nearbyEnemies(unit.team, unit.x, unit.y, radius, e -> {
            if(e != unit && !e.dead() && e.within(unit.x, unit.y, radius)){
                e.damage(maxDamage * fury);
                e.apply(SixfoldMod.emberBurn, burnDuration);
            }
        });
    }
}
