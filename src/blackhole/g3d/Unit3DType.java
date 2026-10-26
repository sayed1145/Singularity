package blackhole.g3d;

import arc.*;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.abilities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;


/**
 * Base for every unit whose body is a live 3D {@link Rig}.
 *
 * <p>The unit keeps 100% vanilla gameplay (entity class, controller, weapons, abilities, networking); only
 * drawing is replaced. Per-unit animation state lives in a small map keyed by unit id and is advanced exactly
 * once per rendered frame. One rig and one renderer are shared per type (drawing is single threaded): pose,
 * draw, done - nothing is cached per unit except a handful of floats.
 */
public abstract class Unit3DType<S extends Unit3DType.State> extends UnitType{
    public Rig rig;
    public UnitRenderer renderer;
    /** design units -> world units */
    public float modelScale = 1f;
    public float shadowLength = 0.55f, shadowAlpha = 0.26f;
    /** below this many screen pixels per design unit, greebles are skipped */
    public float detailPpu = 2.2f;

    protected final IntMap<S> states = new IntMap<>();
    /** baked rest-pose sprite used when the frame's live budget is spent or the camera is far out */
    private TextureRegion lod;
    private TextureAtlas lodFrom;
    /** world size of the baked LOD sprite (set by the baker via unitLodSize) */
    public float lodSize = -1f;
    private float lastPrune;
    protected final float[] v3 = new float[3];

    public static class State{
        public float seen;
        public long frame = -1;
        public boolean init;
        public float lastX, lastY;
    }

    public Unit3DType(String name){
        super(name);
        drawCell = false;
        drawBody = false;
        drawSoftShadow = false;
        outlines = false;
        engineSize = 0f;
    }

    protected abstract Rig buildRig();

    protected abstract S newState();

    /**
     * Poses {@link #rig} for this unit and calls {@code renderer.pose(...)}. {@code delta} is 0 when the unit
     * is drawn a second time in the same frame (e.g. minimap/payload) so animation never runs twice.
     */
    protected abstract void animate(Unit unit, S s, float delta, boolean payload);

    /** Additive glows, trails and effects anchored to bones (the rig is posed when this runs). */
    protected void drawExtras(Unit unit, S s, float z, boolean step, boolean payload){
    }

    public S state(Unit unit){
        S s = states.get(unit.id);
        if(s == null){
            states.put(unit.id, s = newState());
            s.lastX = unit.x;
            s.lastY = unit.y;
        }
        s.seen = Time.time;
        return s;
    }

    /** Poses the rig standing still (assembler print, icons). */
    public void restPose(){
        rig.reset();
    }

    public void ensureRig(){
        if(rig == null){
            rig = buildRig();
            renderer = new UnitRenderer(rig.half * modelScale);
        }
    }

    @Override
    public void init(){
        super.init();
        ensureRig();
        clipSize = Math.max(clipSize, rig.half * modelScale * 5f);
    }

    /**
     * Places every {@link LiftedWeapon} exactly on its modelled barrel: mount position from the yaw bone, shoot offset
     * from the muzzle point, muzzle height from the rest pose. Twin barrels ({@link LiftedWeapon#barrelSpread})
     * rewrite ShootAlternate / ShootBarrel patterns so each bolt leaves its own barrel. Uses the rest pose facing
     * north (rotation 90), so any fixed barrel pitch must be modelled into the mesh.
     */
    protected void alignWeapons(IntSeq weaponBone, IntSeq barrelBone, FloatSeq muzzle, float fly){
        ensureRig();
        restPose();
        renderer.pose(rig, 90f, 0f, 0f, 0f, modelScale);
        float[] p = new float[3], w = new float[3];
        for(int i = 0; i < weapons.size; i++){
            if(!(weapons.get(i) instanceof LiftedWeapon l)) continue;
            l.cam = renderer.cam;
            l.flyZ = fly;
            if(i >= barrelBone.size) continue;
            int bb = barrelBone.get(i), wb = weaponBone.get(i);
            int mb = bb >= 0 ? bb : wb;
            if(mb < 0) continue;
            renderer.point(mb, muzzle.get(i * 3), muzzle.get(i * 3 + 1), muzzle.get(i * 3 + 2), p);
            if(l.rotate && wb >= 0){
                renderer.point(wb, 0f, 0f, 0f, w);
                l.x = w[0];
                l.y = w[1];
                l.shootX = p[0] - w[0];
                l.shootY = p[1] - w[1];
            }else{
                l.x = p[0];
                l.y = p[1];
                l.shootX = 0f;
                l.shootY = 0f;
            }
            l.muzzleZ = p[2];
            if(l.barrelSpread > 0f){
                float sp = l.barrelSpread * modelScale;
                if(l.shoot instanceof mindustry.entities.pattern.ShootAlternate alt){
                    alt.spread = sp;
                    alt.barrels = 2;
                }else if(l.shoot instanceof mindustry.entities.pattern.ShootBarrel bar){
                    bar.barrels = new float[]{-sp / 2f, 0f, 0f, sp / 2f, 0f, 0f};
                }
            }
        }
        rig.reset();
    }

    @Override
    public void update(Unit unit){
        super.update(unit);
        if(Time.time - lastPrune > 600f){
            lastPrune = Time.time;
            IntSeq dead = new IntSeq();
            for(var e : states.entries()){
                if(Time.time - e.value.seen > 600f) dead.add(e.key);
            }
            for(int i = 0; i < dead.size; i++) states.remove(dead.get(i));
        }
    }

    public float layer(Unit unit, boolean payload){
        return payload ? Draw.z() :
            unit.elevation > 0.5f || (flying && unit.dead) ? flyingLayer :
            groundLayer + Mathf.clamp(hitSize / 4000f, 0, 0.01f);
    }

    @Override
    public void draw(Unit unit){
        ensureRig();
        if(Vars.player != null && unit.inFogTo(Vars.player.team())) return;
        S s = state(unit);
        boolean payload = !unit.isAdded();
        float z = layer(unit, payload);

        if(buildSpeed > 0f && !payload) unit.drawBuilding();
        if(unit.mining() && !payload) drawMining(unit);

        long frame = Core.graphics == null ? s.frame + 1 : Core.graphics.getFrameId();
        boolean step = frame != s.frame;
        s.frame = frame;

        if(!payload && !Budget.take(rig.faces() / 2)){
            drawLod(unit, z);
            return;
        }

        rig.reset();
        renderer.reset();
        renderer.setTeam(unit.team.color);
        float ppu = BlockModel.pixelsPerUnit() * modelScale;
        renderer.lowDetail = ppu < detailPpu;
        animate(unit, s, step ? Time.delta : 0f, payload);
        s.init = true;

        if(!payload){
            Draw.z(Math.min(Layer.darkness, z - 1f));
            renderer.shadow(rig, unit.x, unit.y, shadowLength, shadowAlpha * (1f - unit.drownTime));
        }

        Draw.z(z);
        Draw.color();
        Draw.mixcol(Color.white, unit.hitTime);
        renderer.draw(rig, unit.x, unit.y);
        Draw.mixcol();

        drawExtras(unit, s, z, step, payload);
        Draw.reset();
        Draw.z(z);

        if(!payload && Vars.renderer != null) drawLight(unit);
        if(drawItems) drawItems(unit);
        if(unit.shieldAlpha > 0f && drawShields) drawShield(unit);
        if(!payload && Vars.renderer != null){
            for(Ability a : unit.abilities){
                Draw.reset();
                a.draw(unit);
            }
        }
        Draw.reset();
        Draw.z(z);
    }

    /** Cheap path: one baked sprite (rest pose, facing north) rotated to the unit, plus its soft shadow. */
    protected void drawLod(Unit unit, float z){
        if(lodFrom != Core.atlas || lod == null){
            lodFrom = Core.atlas;
            lod = Core.atlas.find(name + "-lod", Core.atlas.find(name));
        }
        float w = lodSize > 0 ? lodSize : rig.half * modelScale * 2.6f;
        float rot = lodRotation(unit);
        Draw.z(Math.min(Layer.darkness, z - 1f));
        Draw.color(0f, 0f, 0f, shadowAlpha * 0.9f);
        float e = unit.elevation;
        Draw.rect(lod, unit.x + 2f + e * 6f, unit.y - 3f - e * 8f, w, w, rot - 90f);
        Draw.z(z);
        Draw.color();
        Draw.mixcol(Color.white, unit.hitTime);
        Draw.rect(lod, unit.x, unit.y, w, w, rot - 90f);
        Draw.mixcol();
        Draw.reset();
        if(unit.shieldAlpha > 0f && drawShields) drawShield(unit);
    }

    /** Heading used by the LOD sprite (body heading, e.g. baseRotation for walkers). */
    protected float lodRotation(Unit unit){
        return unit.rotation;
    }

    /** World position of a bone-local point, projected like the mesh (for effects). out = {x, y, height}. */
    public void screen(Unit unit, int bone, float x, float y, float z, float[] out){
        renderer.point(bone, x, y, z, out);
        float h = out[2];
        out[0] = unit.x + renderer.screenX(out[0], h);
        out[1] = unit.y + renderer.screenY(out[1], h);
    }

    /** Ground point directly below a bone-local point (for dust / decals). */
    public void ground(Unit unit, int bone, float x, float y, float z, float[] out){
        renderer.point(bone, x, y, z, out);
        out[0] += unit.x;
        out[1] += unit.y;
    }

    protected static float wrap180(float a){
        a = a % 360f;
        if(a <= -180f) a += 360f;
        if(a > 180f) a -= 360f;
        return a;
    }
}
