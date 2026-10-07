package astro.ai;

import arc.math.geom.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

/**
 * Controller that replaces a unit's own AI while a tactical commander is online. It is a {@link CommandAI}, so
 * ground units use the real flow-field path finding and every command is an ordinary RTS order; the brain issues
 * those orders. On top of that it has a reflex layer: for a few ticks at a time {@link Micro} can take the steering
 * (dodge a bullet, back off from a melee enemy, strafe, unjam) and retarget instantly; afterwards the normal order
 * continues. Never installed on a unit that a player controls.
 */
public class TacticalAI extends CommandAI{
    /** the controller that was replaced (restored when the commander goes offline) */
    public AIController original;
    /** target chosen for this unit by the squad planner (focused fire, role aware) */
    public Teamc focus;

    // reflex layer state
    float microUntil, mx, my;
    int microKind;
    boolean strafing;
    float dodgeReady;
    float lastMicro, lastScan, strafeUntil, nextCheck, lastX, lastY;
    float strafeSign = 1f;
    int stuck;
    /** raid mission carried by the unit (survives squad regrouping): building position, expiry time */
    int raidPos = -1;
    float raidUntil;
    /** last tactic given to this unit (by its squad) and when; squads keep a tactic for a minimum time */
    int tactic = -1;
    float tacticAt;
    /** kiting hysteresis: once a unit starts backing off it keeps doing so until the gap is comfortable */
    boolean kiting;

    public void setMainTarget(Teamc t){
        target = t;
    }

    public Teamc mainTarget(){
        return target;
    }

    /** steer by the given direction for {@code ticks} ticks; kind: 1 dodge, 2 kite, 3 strafe, 4 unjam */
    void micro(float dx, float dy, float ticks, int kind){
        if(microKind != 0 && kind > microKind && Time.time < microUntil) return; //a dodge is never overridden by a strafe
        mx = dx; my = dy; microUntil = Time.time + ticks; microKind = kind;
    }

    public boolean microActive(){
        return Time.time < microUntil;
    }

    @Override
    public void updateUnit(){
        if(unit.isPlayer()) return; //never steer a unit a player has taken over
        if(Time.time < microUntil && unit.type != null){
            updateVisuals();
            updateTargeting();
            Vec2 v = Tmp.v1.set(mx, my);
            if(v.len2() > 0.0001f){
                v.setLength(unit.speed());
                unit.movePref(v);
            }
            if(target != null && unit.hasWeapons()) faceTarget();
            return;
        }
        microKind = 0;
        super.updateUnit();
    }

    @Override
    public Teamc findMainTarget(float x, float y, float range, boolean air, boolean ground){
        if(focus != null && !Units.invalidateTarget(focus, unit.team, x, y, range)) return focus;
        return super.findMainTarget(x, y, range, air, ground);
    }
}
