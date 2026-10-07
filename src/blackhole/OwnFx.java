package blackhole;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;

import java.lang.reflect.*;
import java.util.*;

import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;

/**
 * The mod's own particle effects, and the scrubber that makes every piece of mod content use them.
 *
 * <p>All effects are drawn in the same fake-3D language as the models: ground rings are squashed to the camera's
 * 0.894 aspect, debris flies on ballistic arcs (height mapped to screen with the camera's 0.447 factor) and casts a
 * small shadow, flashes are layered additive discs. They are cheap (a handful of fills each, no textures).
 *
 * <p>{@link #scrub(String)} walks every block / unit / status effect / bullet of this mod reflectively and swaps
 * any remaining vanilla {@link Fx} instance for its own counterpart (by role: hit, explosion, smoke, muzzle...).
 * Objects reachable from vanilla content are collected first and never modified, so shared vanilla bullets or
 * effects are untouched.
 */
public final class OwnFx{
    private OwnFx(){}

    public static final float squash = 0.894f, lift = 0.447f;
    static final Rand rand = new Rand();
    public static Rand rand(){ return rand; }
    static final Color tmp = new Color();

    // ------------------------------------------------------------------ helpers

    /** Ground ring squashed to the camera. */
    public static void ring(float x, float y, float r, float stroke){
        stroke(stroke);
        int seg = Mathf.clamp((int)(r * 0.8f) + 10, 12, 40);
        for(int i = 0; i < seg; i++){
            float a0 = i * Mathf.PI2 / seg, a1 = (i + 1) * Mathf.PI2 / seg;
            line(x + Mathf.cos(a0) * r, y + Mathf.sin(a0) * r * squash, x + Mathf.cos(a1) * r, y + Mathf.sin(a1) * r * squash, false);
        }
    }

    /** Filled ground disc squashed to the camera. */
    public static void disc(float x, float y, float r){
        int seg = Mathf.clamp((int)(r * 0.8f) + 10, 12, 32);
        for(int i = 0; i < seg; i++){
            float a0 = i * Mathf.PI2 / seg, a1 = (i + 1) * Mathf.PI2 / seg;
            Fill.tri(x, y, x + Mathf.cos(a0) * r, y + Mathf.sin(a0) * r * squash, x + Mathf.cos(a1) * r, y + Mathf.sin(a1) * r * squash);
        }
    }

    /** n chunks thrown on ballistic arcs with shadows. */
    public static void debris(Effect.EffectContainer e, int n, float range, float size, Color c){
        rand.setSeed(e.id);
        for(int i = 0; i < n; i++){
            float a = rand.random(360f), d = rand.random(0.4f, 1f) * range * e.finpow();
            float h = Mathf.sin(e.fin() * Mathf.PI) * rand.random(4f, 11f) * lift;
            float x = e.x + Angles.trnsx(a, d), y = e.y + Angles.trnsy(a, d) * squash;
            float s = size * rand.random(0.6f, 1.2f) * (1f - e.fin() * 0.6f);
            color(0f, 0f, 0f, 0.25f * e.fout());
            Fill.square(x, y - 0.6f, s * 0.9f, 0f);
            color(c, e.fout());
            Fill.square(x, y + h, s, e.fin() * 260f + a);
        }
    }

    /** Layered additive flash disc. */
    public static void flash(float x, float y, float r, Color c, float a){
        blend(Blending.additive);
        color(c, a * 0.45f);
        Fill.circle(x, y, r);
        color(Color.white, a * 0.7f);
        Fill.circle(x, y, r * 0.45f);
        blend();
    }

    /** Soft rising smoke puffs. */
    public static void puffs(Effect.EffectContainer e, int n, float range, float size, Color c){
        rand.setSeed(e.id * 7L);
        for(int i = 0; i < n; i++){
            float a = rand.random(360f), d = rand.random(0.2f, 1f) * range * e.finpow();
            float x = e.x + Angles.trnsx(a, d), y = e.y + Angles.trnsy(a, d) * squash + e.fin() * rand.random(3f, 8f) * lift;
            color(c, e.fout() * 0.55f);
            Fill.circle(x, y, size * rand.random(0.6f, 1.1f) * (0.5f + e.fin() * 0.8f));
        }
    }

    // ------------------------------------------------------------------ effects

    /** Small impact: spark ring and three chips. */
    public static final Effect hit = new Effect(16f, e -> {
        Color c = e.color == null || e.color.a <= 0f ? Pal.lightishGray() : e.color;
        color(c, e.fout());
        ring(e.x, e.y, 1.5f + e.fin() * 5f, 1.1f * e.fout());
        debris(e, 3, 7f, 0.9f, c);
        flash(e.x, e.y, 3f * e.fout(), c, e.fout());
        reset();
    });

    /** Larger impact. */
    public static final Effect hitBig = new Effect(24f, e -> {
        Color c = e.color == null || e.color.a <= 0f ? Pal.lightishGray() : e.color;
        color(c, e.fout());
        ring(e.x, e.y, 3f + e.fin() * 10f, 1.6f * e.fout());
        debris(e, 6, 13f, 1.2f, c);
        flash(e.x, e.y, 6f * e.fout(), c, e.fout());
        reset();
    });

    /** Explosion: flash, shock ring, debris and smoke. */
    public static final Effect explode = new Effect(34f, e -> {
        Color c = e.color == null || e.color.a <= 0f ? Color.valueOf("ffb45a") : e.color;
        flash(e.x, e.y, 9f * Mathf.clamp(e.fout() * 2f), c, e.fout());
        color(c, e.fout());
        ring(e.x, e.y, 4f + e.finpow() * 18f, 2.2f * e.fout());
        puffs(e, 6, 14f, 3.4f, Color.valueOf("4a4e57"));
        debris(e, 7, 20f, 1.3f, c);
        reset();
    });

    /** Big explosion (buildings, large units). */
    public static final Effect explodeBig = new Effect(50f, e -> {
        Color c = e.color == null || e.color.a <= 0f ? Color.valueOf("ffb45a") : e.color;
        flash(e.x, e.y, 18f * Mathf.clamp(e.fout() * 2f), c, e.fout());
        color(c, e.fout());
        ring(e.x, e.y, 8f + e.finpow() * 36f, 3f * e.fout());
        ring(e.x, e.y, 4f + e.finpow() * 22f, 1.4f * e.fout());
        puffs(e, 10, 28f, 5.5f, Color.valueOf("3f434c"));
        debris(e, 12, 38f, 1.8f, c);
        reset();
    });

    /** Grey smoke. */
    public static final Effect smoke = new Effect(40f, e -> {
        puffs(e, 3, 6f, 2.2f, Color.valueOf("5b606b"));
        reset();
    });

    /** Muzzle flash: a short cone of light along the shot. */
    public static final Effect muzzle = new Effect(10f, e -> {
        Color c = e.color == null || e.color.a <= 0f ? Color.valueOf("ffe29a") : e.color;
        blend(Blending.additive);
        color(c, e.fout());
        float len = 7f * e.fout() + 2f, w = 2.6f * e.fout();
        Fill.tri(e.x + Angles.trnsx(e.rotation + 90f, w), e.y + Angles.trnsy(e.rotation + 90f, w),
            e.x + Angles.trnsx(e.rotation - 90f, w), e.y + Angles.trnsy(e.rotation - 90f, w),
            e.x + Angles.trnsx(e.rotation, len), e.y + Angles.trnsy(e.rotation, len));
        color(Color.white, e.fout());
        Fill.circle(e.x, e.y, 1.6f * e.fout());
        blend();
        reset();
    });

    /** Muzzle smoke drifting along the shot. */
    public static final Effect muzzleSmoke = new Effect(28f, e -> {
        rand.setSeed(e.id);
        for(int i = 0; i < 4; i++){
            float d = rand.random(2f, 9f) * e.finpow(), a = e.rotation + rand.range(22f);
            color(Color.valueOf("6b707a"), e.fout() * 0.5f);
            Fill.circle(e.x + Angles.trnsx(a, d), e.y + Angles.trnsy(a, d) + e.fin() * 2f, 1.2f + e.fin() * 1.6f);
        }
        reset();
    });

    /** Ejected shell casing tumbling on an arc. */
    public static final Effect casing = new Effect(30f, e -> {
        float side = Mathf.sign(e.rotation) >= 0 ? 1f : -1f;
        float a = Math.abs(e.rotation) + 90f * side;
        float d = e.finpow() * 8f, h = Mathf.sin(e.fin() * Mathf.PI) * 5f * lift;
        color(0f, 0f, 0f, 0.22f * e.fout());
        Fill.rect(e.x + Angles.trnsx(a, d), e.y + Angles.trnsy(a, d) * squash - 0.5f, 1.2f, 0.6f);
        color(Color.valueOf("d8c28a"), e.fout());
        Fill.rect(e.x + Angles.trnsx(a, d), e.y + Angles.trnsy(a, d) * squash + h, 1.4f, 0.7f, e.fin() * 540f);
        reset();
    });

    /** Healing pulse: rising plus-sparks inside a squashed ring. */
    public static final Effect heal = new Effect(24f, e -> {
        Color c = e.color == null || e.color.a <= 0f ? Color.valueOf("8fe9ff") : e.color;
        color(c, e.fout());
        ring(e.x, e.y, 2f + e.fin() * 6f, e.fout());
        rand.setSeed(e.id);
        for(int i = 0; i < 3; i++){
            float x = e.x + rand.range(4f), y = e.y + rand.range(3f) + e.fin() * 6f * lift;
            Fill.rect(x, y, 1.6f * e.fout(), 0.5f * e.fout());
            Fill.rect(x, y, 0.5f * e.fout(), 1.6f * e.fout());
        }
        reset();
    });

    /** Shield break: prism shards scatter from the ward's edge. */
    public static final Effect shield = new Effect(40f, e -> {
        Color c = e.color == null || e.color.a <= 0f ? Color.valueOf("b69cff") : e.color;
        float r = Math.max(e.rotation, 8f);
        color(c, e.fout());
        stroke(2f * e.fout());
        Lines.poly(e.x, e.y, 6, r + e.fin() * 6f, 30f);
        rand.setSeed(e.id);
        for(int i = 0; i < 10; i++){
            float a = rand.random(360f), d = r + e.finpow() * rand.random(6f, 16f);
            Fill.poly(e.x + Angles.trnsx(a, d), e.y + Angles.trnsy(a, d), 3, 1.6f * e.fout(), a + e.fin() * 200f);
        }
        reset();
    });

    /** Unit warp-in: contracting squashed rings with a bright core. */
    public static final Effect spawn = new Effect(30f, e -> {
        Color c = e.color == null || e.color.a <= 0f ? Color.valueOf("8fe9ff") : e.color;
        color(c, e.fin());
        ring(e.x, e.y, 3f + e.fout() * 14f, 1.4f * e.fin());
        ring(e.x, e.y, 1f + e.fout() * 8f, 0.9f * e.fin());
        flash(e.x, e.y, 4f * e.fin(), c, e.fin() * 0.8f);
        reset();
    });

    /** Block placed: a scan square settling on the footprint. */
    public static final Effect place = new Effect(16f, e -> {
        float s = e.rotation * 4f + 1f;
        color(Color.valueOf("8fe9ff"), e.fout());
        stroke(1.4f * e.fout());
        Lines.rect(e.x - s - e.fout() * 2f, e.y - s * squash - e.fout() * 2f, (s + e.fout() * 2f) * 2f, (s * squash + e.fout() * 2f) * 2f);
        reset();
    });

    /** Block removed: panels fall apart. */
    public static final Effect breakBlock = new Effect(22f, e -> {
        debris(e, 5, e.rotation * 5f + 4f, 1.2f, Color.valueOf("8a98ad"));
        reset();
    });

    /** Ground unit tread / footfall dust. */
    public static final Effect dust = new Effect(34f, e -> {
        color(e.color == null || e.color.a <= 0f ? Color.valueOf("9c93ab") : e.color, e.fout() * 0.6f);
        disc(e.x, e.y, 1f + e.fin() * 3f);
        reset();
    });

    /** Fire / burning tick. */
    public static final Effect burn = new Effect(26f, e -> {
        Color c = e.color == null || e.color.a <= 0f ? Color.valueOf("ffb45a") : e.color;
        rand.setSeed(e.id);
        for(int i = 0; i < 3; i++){
            float x = e.x + rand.range(3f), y = e.y + rand.range(2f) + e.fin() * rand.random(3f, 7f) * lift * 2f;
            blend(Blending.additive);
            color(c, e.fout());
            Fill.circle(x, y, 1.3f * e.fout() + 0.3f);
            blend();
        }
        reset();
    });

    /** Liquid splash (wet/puddles). */
    public static final Effect splash = new Effect(24f, e -> {
        Color c = e.color == null || e.color.a <= 0f ? Color.valueOf("5fb7ff") : e.color;
        color(c, e.fout());
        ring(e.x, e.y, 1f + e.fin() * 5f, e.fout());
        debris(e, 3, 5f, 0.7f, c);
        reset();
    });

    /** Charge-up: sparks converge on the point. */
    public static final Effect charge = new Effect(40f, e -> {
        Color c = e.color == null || e.color.a <= 0f ? Color.valueOf("8fe9ff") : e.color;
        rand.setSeed(e.id);
        color(c, e.fin());
        for(int i = 0; i < 7; i++){
            float a = rand.random(360f), d = rand.random(8f, 16f) * e.fout();
            Fill.circle(e.x + Angles.trnsx(a, d), e.y + Angles.trnsy(a, d), 1.2f * e.fin());
        }
        flash(e.x, e.y, 4f * e.fin(), c, e.fin());
        reset();
    });

    /** Trail puff used when a vanilla trail effect is found. */
    public static final Effect trail = new Effect(20f, e -> {
        color(e.color == null || e.color.a <= 0f ? Color.valueOf("c9d3e0") : e.color, e.fout() * 0.8f);
        Fill.circle(e.x, e.y, (e.rotation > 0 ? e.rotation : 1.4f) * e.fout());
        reset();
    });

    // ------------------------------------------------------------------ scrubber

    private static final ObjectMap<Effect, Effect> map = new ObjectMap<>();
    private static final Set<Object> vanillaOwned = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    public static int replaced;

    /** Own counterpart of a vanilla effect, by the role its Fx field name implies. */
    static Effect counterpart(String n){
        String s = n.toLowerCase(Locale.ROOT);
        if(s.equals("none")) return null;
        if(s.contains("casing") || s.contains("shell")) return casing;
        if(s.contains("smoke") && s.contains("shoot")) return muzzleSmoke;
        if(s.contains("shoot") || s.contains("muzzle") || s.contains("flash")) return muzzle;
        if(s.contains("heal") || s.contains("mend") || s.contains("regen") || s.contains("overdrive")) return heal;
        if(s.contains("shield") || s.contains("absorb") || s.contains("force")) return shield;
        if(s.contains("spawn") || s.contains("teleport") || s.contains("unitland") || s.contains("launch")) return spawn;
        if(s.contains("place") || s.contains("upgradecore") || s.contains("rotate")) return place;
        if(s.contains("break") || s.contains("deconstruct")) return breakBlock;
        if(s.contains("big") && (s.contains("explo") || s.contains("blast"))) return explodeBig;
        if(s.contains("explo") || s.contains("blast") || s.contains("dynamic") || s.contains("massive") || s.contains("reactor") || s.contains("impact")) return explode;
        if(s.contains("charge")) return charge;
        if(s.contains("smoke") || s.contains("steam") || s.contains("vapor") || s.contains("fog") || s.contains("cloud")) return smoke;
        if(s.contains("fire") || s.contains("burn") || s.contains("ember") || s.contains("spark") && s.contains("fire")) return burn;
        if(s.contains("wet") || s.contains("splash") || s.contains("bubble") || s.contains("ripple") || s.contains("mud") || s.contains("puddle")) return splash;
        if(s.contains("dust") || s.contains("tread") || s.contains("step") || s.contains("crush") || s.contains("drill") || s.contains("pulverize") || s.contains("mine")) return dust;
        if(s.contains("trail") || s.contains("missile")) return trail;
        if(s.contains("big") || s.contains("large") || s.contains("hitlaser") || s.contains("flak")) return hitBig;
        return hit;
    }

    static void buildMap(){
        if(map.size > 0) return;
        for(Field f : Fx.class.getFields()){
            if(!Modifier.isStatic(f.getModifiers()) || !Effect.class.isAssignableFrom(f.getType())) continue;
            try{
                Effect v = (Effect)f.get(null);
                if(v == null) continue;
                Effect own = counterpart(f.getName());
                if(own != null) map.put(v, own);
            }catch(Throwable ignored){}
        }
    }

    /**
     * Replaces vanilla Fx in every content object whose name starts with {@code prefix} (and the bullets, weapons,
     * abilities, drawers and nested objects they own). Call from Mod.init, after all content exists.
     */
    public static void scrub(String prefix){
        buildMap();
        visited.clear();
        //everything reachable from vanilla / other mods' content is off limits
        vanillaOwned.clear();
        for(var list : new Seq[]{mindustry.Vars.content.blocks(), mindustry.Vars.content.units(), mindustry.Vars.content.statusEffects(),
            mindustry.Vars.content.liquids(), mindustry.Vars.content.items()}){
            for(Object o : list){
                if(o instanceof MappableContent c && !c.name.startsWith(prefix)) collect(o, vanillaOwned, 0);
            }
        }
        for(Field f : Fx.class.getFields()){
            try{ if(Modifier.isStatic(f.getModifiers())) vanillaOwned.add(f.get(null)); }catch(Throwable ignored){}
        }
        replaced = 0;
        for(var list : new Seq[]{mindustry.Vars.content.blocks(), mindustry.Vars.content.units(), mindustry.Vars.content.statusEffects(),
            mindustry.Vars.content.liquids(), mindustry.Vars.content.items()}){
            for(Object o : list){
                if(o instanceof MappableContent c && c.name.startsWith(prefix)) walk(o, 0);
            }
        }
        Log.info("[blackhole] own particle effects: @ vanilla effect references replaced", replaced);
        visited.clear();
        vanillaOwned.clear();
    }

    static boolean interesting(Class<?> t){
        if(t.isPrimitive() || t == String.class || t.isEnum()) return false;
        String n = t.getName();
        return !n.startsWith("java.") && !n.startsWith("arc.graphics") && !n.startsWith("arc.math") && !n.startsWith("arc.audio");
    }

    static void collect(Object o, Set<Object> out, int depth){
        if(o == null || depth > 6 || out.contains(o) || o instanceof Effect) return;
        Class<?> t = o.getClass();
        if(!interesting(t) && !(o instanceof Object[]) && !(o instanceof Iterable) && !(o instanceof ObjectMap)) return;
        out.add(o);
        eachChild(o, c -> collect(c, out, depth + 1));
    }

    static void walk(Object o, int depth){
        if(o == null || depth > 7 || visited.contains(o) || o instanceof Effect) return;
        if(vanillaOwned.contains(o) && !(o instanceof MappableContent mc && mc.name.startsWith("blackhole-"))) return;
        Class<?> t = o.getClass();
        if(!interesting(t) && !(o instanceof Object[]) && !(o instanceof Iterable) && !(o instanceof ObjectMap)) return;
        visited.add(o);
        //swap effect fields
        for(Class<?> c = t; c != null && c != Object.class; c = c.getSuperclass()){
            for(Field f : c.getDeclaredFields()){
                if(Modifier.isStatic(f.getModifiers()) || !Effect.class.isAssignableFrom(f.getType())) continue;
                try{
                    f.setAccessible(true);
                    Effect v = (Effect)f.get(o);
                    Effect own = v == null ? null : map.get(v);
                    if(own != null){
                        f.set(o, own);
                        replaced++;
                    }
                }catch(Throwable ignored){}
            }
        }
        eachChild(o, ch -> walk(ch, depth + 1));
    }

    interface Visitor{ void visit(Object o); }

    /** Children worth descending into: bullets, weapons, abilities, drawers, patterns, arrays, collections. */
    static void eachChild(Object o, Visitor v){
        if(o instanceof Object[] arr){
            for(Object e : arr) v.visit(e);
            return;
        }
        if(o instanceof Seq<?> s){
            for(Object e : s) v.visit(e);
            return;
        }
        if(o instanceof ObjectMap<?, ?> m){
            for(var e : m){ v.visit(e.key); v.visit(e.value); }
            return;
        }
        if(o instanceof Iterable<?> it){
            for(Object e : it) v.visit(e);
            return;
        }
        for(Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()){
            for(Field f : c.getDeclaredFields()){
                if(Modifier.isStatic(f.getModifiers())) continue;
                Class<?> ft = f.getType();
                if(ft.isPrimitive() || ft == String.class || Effect.class.isAssignableFrom(ft)) continue;
                if(!(BulletType.class.isAssignableFrom(ft) || Weapon.class.isAssignableFrom(ft) || Ability.class.isAssignableFrom(ft)
                    || ft.getName().startsWith("mindustry.world.draw") || ft.getName().startsWith("mindustry.entities.part")
                    || ft.getName().startsWith("mindustry.world.consumers") || ft.getName().startsWith("blackhole") || ft.getName().startsWith("sixfold")
                    || ft.isArray() || Seq.class.isAssignableFrom(ft) || ObjectMap.class.isAssignableFrom(ft))) continue;
                //never descend into other content (items, liquids, units referenced by a block are walked on their own)
                if(UnlockableContent.class.isAssignableFrom(ft) && !BulletType.class.isAssignableFrom(ft)) continue;
                try{
                    f.setAccessible(true);
                    Object ch = f.get(o);
                    if(ch instanceof UnlockableContent) continue;
                    v.visit(ch);
                }catch(Throwable ignored){}
            }
        }
    }

    /** Vanilla colour helper kept local so effects do not depend on Pal fields changing between versions. */
    static final class Pal{
        static Color lightishGray(){ return tmp.set(0.75f, 0.75f, 0.8f, 1f); }
    }
}
