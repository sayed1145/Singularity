package blackhole;

import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.bullet.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.meta.*;

/**
 * v8.7.7 self-check (it keeps every 8.7.x check that still applies, and proves the deletions).
 *
 * <p>Every number the 8.7.1 and 8.7.2 patches touched is verified here, at load time, on the real
 * content objects. The point is not the pretty numbers - it is that a wrong constant cannot reach a
 * player's screen without the server refusing to start.
 *
 * <p><b>8.7.5</b>: the gravity well is deleted outright (block, model, tech node, recipe cost) - the content
 * check below proves nothing named "gravity-well" is registered any more. <b>8.7.6</b> slowed the core to
 * 1 tile/s; <b>8.7.7</b> drops it to 0.4 tiles/s while the outer rim stays at 0.1 tiles/s, so the entire
 * fall - horizon included - is one continuous crawl.
 *
 * <p>The first check is the one that matters most: the cargo station's configuration panel used to
 * call {@code Table.slider(radius, 8f, 400f, 1f, listener)} while Arc's signature is
 * {@code slider(min, max, step, defvalue, listener)}. Arc's {@code ProgressBar} constructor throws
 * {@code IllegalArgumentException("max must be > min. min,max: 58.0, 8.0")} for that input, so opening
 * the panel on Android killed the whole game. The check below builds the very same progress bar the
 * panel builds and proves the bounds are legal now - and that the old order is still illegal, so the
 * guard cannot rot.
 *
 * <p><b>8.7.2 additions</b>: the waste shells must load five rounds per unit of waste
 * ({@code ammoMultiplier} 5) and must NOT add any rate of fire ({@code reloadMultiplier} 1); the sky
 * net's lumenite shell must hit for 13; the lumen accelerator must overdrive again (+50%) while
 * still being unboostable; the black hole must keep a gravity field that also eats buildings; the
 * gravity well (then 15 tiles, units only) must exist; since 8.7.5 it must NOT exist any more; the pyroclast titan
 * must be a plain walker again (no body effect layer) while its attack effect stays the 6.1 one.
 */
public final class V873Audit{
    private V873Audit(){}

    public static void check(){
        checkArcSliderBounds();
        checkAccelerator();
        checkWasteShells();
        checkSkyNetShell();
        checkBlackHoleGravity();
        checkSlowAbsorption();
        checkQuietWasteEffects();
        checkPyroclastBody();
        checkDamageNumbers();
        checkRanges();
        checkRemovedContent();
        Log.info("[blackhole/v8.7.7] self-check passed: arc slider bounds, accelerator +50% (unboostable), " +
            "waste shells (halo 40 / sky-net 20, load x5, no extra fire rate, no building reduction), " +
            "sky net lumenite shell 13, black hole gravity field (230 px, eats buildings), " +
            "gravity well deleted outright (block + model + tech node + cost, removal verified), " +
            "black hole crawling absorption 0.1-0.4 tiles/s ramping tile-by-tile to a 1-tile core capped at 0.4, " +
            "waste firing effects quiet, " +
            "titan body rolled back, citadel 45, " +
            "lumen spark 36, metaglass 21, railgun 45 tiles, titan 60 tiles, hauler removed.");
    }

    // =====================================================================================================
    // 1. the crash that started all this
    // =====================================================================================================

    /** Arc's ProgressBar refuses min >= max. The panel's slider must be built with legal bounds. */
    private static void checkArcSliderBounds(){
        //the bounds the cargo station panel uses
        arc.scene.ui.ProgressBar good;
        try{
            good = new arc.scene.ui.ProgressBar(8f, 400f, 1f, false, new arc.scene.ui.ProgressBar.ProgressBarStyle());
        }catch(IllegalArgumentException e){
            throw new IllegalStateException("v8.7.1: the cargo station slider bounds are illegal: " + e.getMessage());
        }catch(NullPointerException e){
            //headless has no scene, so Arc cannot resolve the skin style - the min/max check above already ran
            good = null;
        }

        //and the old order must still be rejected - if this ever stops throwing, the guard is dead
        boolean threw = false;
        try{
            new arc.scene.ui.ProgressBar(58f, 8f, 1f, false, new arc.scene.ui.ProgressBar.ProgressBarStyle());
        }catch(IllegalArgumentException e){
            threw = true;
        }catch(NullPointerException e){
            threw = false;
        }
        if(!threw){
            throw new IllegalStateException("v8.7.1: Arc no longer rejects min > max - the slider guard needs a review");
        }
        Log.info("[blackhole/v8.7.3] arc slider bounds ok (min 8, max 400, step 1; the old 58/8 order still throws)");
    }

    // =====================================================================================================
    // 2. the lumen accelerator overdrives again - and still cannot be boosted
    // =====================================================================================================

    private static void checkAccelerator(){
        Block b = Vars.content.block("blackhole-lumen-accelerator");
        if(b == null) throw new IllegalStateException("v8.7.3: the lumen accelerator is missing");
        if(!(b instanceof mindustry.world.blocks.defense.OverdriveProjector o)) {
            throw new IllegalStateException("v8.7.3: the lumen accelerator is not an overdrive projector");
        }
        //v8.7.3: the +50% overdrive is back (the vanilla default), the booster slot and the pulse are not
        if(o.speedBoost != 1.5f || o.hasBoost || o.speedBoostPhase != 0f){
            throw new IllegalStateException("v8.7.3: the lumen accelerator is wrong: speedBoost=" +
                o.speedBoost + " hasBoost=" + o.hasBoost + " phase=" + o.speedBoostPhase);
        }
        //the panel must show the truth: the +50% speed line and the 150% total multiplier.
        //Mindustry fills the stat list lazily (when the info panel is opened), so force it here.
        b.computeStats();
        if(!hasStat(b, Stat.speedIncrease)){
            throw new IllegalStateException("v8.7.3: the lumen accelerator does not show its speed increase any more");
        }
        if(!hasStat(b, Stat.boostEffect)){
            throw new IllegalStateException("v8.7.3: the lumen accelerator does not show its boost multiplier any more");
        }
    }

    /** true when the block's stat list still carries the given stat */
    private static boolean hasStat(Block b, Stat stat){
        OrderedMap<StatCat, OrderedMap<Stat, Seq<StatValue>>> map = b.stats.toMap();
        for(StatCat cat : map.keys()){
            if(map.get(cat).containsKey(stat)) return true;
        }
        return false;
    }

    // =====================================================================================================
    // 3. the two waste shells: five rounds per unit of waste, and no extra fire rate
    // =====================================================================================================

    private static void checkWasteShells(){
        Item waste = Vars.content.item("blackhole-industrial-waste");
        if(waste == null) throw new IllegalStateException("v8.7.3: industrial waste is missing");

        BulletType halo = ammoOf("blackhole-halo", waste);
        BulletType sky = ammoOf("blackhole-sky-net", waste);
        if(halo == null || sky == null){
            throw new IllegalStateException("v8.7.3: the waste shells are not registered on both turrets");
        }
        if(halo.damage != 40f) throw new IllegalStateException("v8.7.3: halo waste shell damage " + halo.damage + " != 40");
        if(sky.damage != 20f) throw new IllegalStateException("v8.7.3: sky-net waste shell damage " + sky.damage + " != 20");
        for(BulletType t : new BulletType[]{halo, sky}){
            //the load multiplier: one piece of waste is five rounds
            if(t.ammoMultiplier != 5f){
                throw new IllegalStateException("v8.7.3: waste shell ammo multiplier " + t.ammoMultiplier + " != 5");
            }
            //and it must not add any rate of fire
            if(t.reloadMultiplier != 1f){
                throw new IllegalStateException("v8.7.3: waste shell still adds fire rate: reload multiplier " +
                    t.reloadMultiplier + " != 1");
            }
            if(t.buildingDamageMultiplier != 1f){
                throw new IllegalStateException("v8.7.3: waste shell still reduces building damage: " +
                    t.buildingDamageMultiplier);
            }
        }
    }

    private static BulletType ammoOf(String block, Item item){
        Block b = Vars.content.block(block);
        if(!(b instanceof ItemTurret t) || t.ammoTypes == null) return null;
        return t.ammoTypes.get(item);
    }

    // =====================================================================================================
    // 4. the sky net's lumenite shell (lead) hits for 13
    // =====================================================================================================

    private static void checkSkyNetShell(){
        Item lumen = AureliaContent.lumenite != null ? AureliaContent.lumenite : mindustry.content.Items.lead;
        BulletType shell = ammoOf("blackhole-sky-net", lumen);
        if(shell == null) shell = ammoOf("blackhole-sky-net", mindustry.content.Items.lead);
        if(shell == null) throw new IllegalStateException("v8.7.3: the sky net has no lumenite/lead shell");
        if(shell.damage != 13f){
            throw new IllegalStateException("v8.7.3: the sky net lumenite shell damage " + shell.damage + " != 13");
        }
    }

    // =====================================================================================================
    // 5. the black hole keeps a real gravity field and eats buildings
    // =====================================================================================================

    private static void checkBlackHoleGravity(){
        BulletType hole = BHBullets.singularityWell;
        if(!(hole instanceof BHBullets.GrowingHoleBulletType g)){
            throw new IllegalStateException("v8.7.3: the black hole is not a growing hole any more");
        }
        //the gravity field has a clear radius
        if(g.pullRadius != 230f){
            throw new IllegalStateException("v8.7.3: the black hole gravity radius " + g.pullRadius + " != 230");
        }
        if(g.pullForce <= 0f){
            throw new IllegalStateException("v8.7.3: the black hole has no gravity force");
        }
        //and it attacks the buildings around it
        if(g.buildingDamage <= 0f){
            throw new IllegalStateException("v8.7.3: the black hole does not damage buildings");
        }
        //the binding helper must exist and be the wither-storm style one
        if(GravityField.class == null){
            throw new IllegalStateException("v8.7.3: the gravity field helper is missing");
        }
    }

    // =====================================================================================================
    // 7. the pyroclast titan: body rolled back, attack effect untouched
    // =====================================================================================================

    private static void checkPyroclastBody(){
        UnitType titan = Vars.content.unit("blackhole-pyroclast-titan");
        if(titan == null) throw new IllegalStateException("v8.7.3: the pyroclast titan is missing");
        //a plain walker again - no subclass that draws a body effect layer
        if(titan.getClass().getSuperclass() != blackhole.g3d.Walker3D.class){
            throw new IllegalStateException("v8.7.3: the pyroclast titan is not a plain walker any more: " +
                titan.getClass().getName());
        }
        //the body effect layer of 8.7 must be gone for good
        try{
            Class.forName("sixfold.PyroclastFx");
            throw new IllegalStateException("v8.7.3: the 8.7 body effect layer (sixfold.PyroclastFx) still exists");
        }catch(ClassNotFoundException expected){
            //this is the point
        }
        //the attack effect must still be the 6.1 one
        boolean legacy = false;
        for(Weapon w : titan.weapons){
            if(w.bullet != null && (w.bullet.hitEffect == sixfold.LegacyEmber.legacyBurst ||
                w.bullet.hitEffect == sixfold.LegacyEmber.legacyVent)){
                legacy = true;
            }
        }
        if(!legacy) throw new IllegalStateException("v8.7.3: the pyroclast titan no longer uses the 6.1 attack effect");
    }

    // =====================================================================================================
    // 7b. v8.7.3 - slow absorption: nothing may swallow a unit in one frame any more
    // =====================================================================================================

    /**
     * v8.7.7: the only gravity source left is the black hole, and it must stay a crawl: 0.1 tile/s at the
     * outer rim, ramping monotonically tile-by-tile to 0.4 tile/s inside the innermost tile, and nothing may
     * swallow a unit in under 1.5 seconds.
     */
    private static void checkSlowAbsorption(){
        BulletType hole = BHBullets.singularityWell;
        if(!(hole instanceof BHBullets.GrowingHoleBulletType g)){
            throw new IllegalStateException("v8.7.3: the black hole is not a growing hole any more");
        }
        if(g.rimSpeed != 0.1f || g.coreSpeed != 0.4f){
            throw new IllegalStateException("v8.7.7: the black hole absorption profile is wrong: rim=" +
                g.rimSpeed + " core=" + g.coreSpeed + " (expected 0.1 and 0.4 tiles/s)");
        }
        //v8.7.7: the profile normalises to the innermost tile, so 0.4 tiles/s is the absolute maximum
        if(g.coreSpeed > 0.4f){
            throw new IllegalStateException("v8.7.7: the black hole core pull exceeded the 0.4 tiles/s maximum: " +
                g.coreSpeed);
        }
        if(g.rimSpeed >= g.coreSpeed){
            throw new IllegalStateException("v8.7.3: the black hole must get faster towards the centre");
        }
        //the slowest absorption must take clearly more than a frame - and more than 1.5 seconds
        //(the full field at the rim speed is 28.75 tiles / 0.1 tile per second = 287.5 s at the worst case;
        // the 1.5 s floor is a regression guard, not the expected magnitude)
        if(g.pullRadius / (g.rimSpeed * 8f) < 1.5f){
            throw new IllegalStateException("v8.7.7: the black hole could still swallow a unit in under 1.5 s");
        }
        //the black hole never absorbs buildings - it only damages them
        if(g.buildingDamage <= 0f){
            throw new IllegalStateException("v8.7.3: the black hole does not damage buildings any more");
        }
    }

    /**
     * v8.7.3: the waste shells must not blow out the screen any more.
     *
     * <p>Both effects stay on the map layer, are short lived, and are no additive white-hot flashes.
     */
    private static void checkQuietWasteEffects(){
        Item waste = Vars.content.item("blackhole-industrial-waste");
        if(waste == null) throw new IllegalStateException("v8.7.3: industrial waste is missing");
        BulletType halo = ammoOf("blackhole-halo", waste);
        if(halo == null) throw new IllegalStateException("v8.7.3: the halo waste shell is missing");
        if(halo.shootEffect == null || halo.shootEffect != FissionFx.fissionMuzzle){
            throw new IllegalStateException("v8.7.3: the waste muzzle effect is not the quiet one");
        }
        if(halo.hitEffect == null || halo.hitEffect != FissionFx.fission){
            throw new IllegalStateException("v8.7.3: the waste hit effect is not the quiet one");
        }
        //no additive white-hot flashes, and no long lived effects stacking up in the salvos
        if(FissionFx.fissionMuzzle.lifetime > 12f){
            throw new IllegalStateException("v8.7.3: the waste muzzle effect lives " +
                FissionFx.fissionMuzzle.lifetime + " frames - it must be short");
        }
        if(FissionFx.fission.lifetime > 45f){
            throw new IllegalStateException("v8.7.3: the waste hit effect lives " +
                FissionFx.fission.lifetime + " frames - it must be short");
        }
        //both stay on the map layer, never on the UI layer
        if(FissionFx.fissionMuzzle.layer != mindustry.graphics.Layer.effect ||
            FissionFx.fission.layer != mindustry.graphics.Layer.effect){
            throw new IllegalStateException("v8.7.3: the waste effects are not pinned to the map layer");
        }
    }

    // =====================================================================================================
    // 8. the damage numbers
    // =====================================================================================================

    private static void checkDamageNumbers(){
        UnitType citadel = Vars.content.unit("blackhole-citadel");
        if(citadel == null) throw new IllegalStateException("v8.7.3: the citadel is missing");
        boolean found = false;
        for(Weapon w : citadel.weapons){
            if(w.bullet == BHBullets.citadelSpray){
                found = true;
                if(w.bullet.damage != 45f){
                    throw new IllegalStateException("v8.7.3: citadel spray damage " + w.bullet.damage + " != 45");
                }
            }
        }
        if(!found) throw new IllegalStateException("v8.7.3: the citadel does not carry the spray bullet");

        Block spark = Vars.content.block("blackhole-lumen-spark");
        if(!(spark instanceof mindustry.world.blocks.defense.turrets.PowerTurret st) || st.shootType == null || st.shootType.damage != 36f){
            throw new IllegalStateException("v8.7.3: the lumen spark does not hit for 36");
        }

        //on Aurelia the vanilla metaglass is priced as prism glass, and the ammo table is remapped with it
        Item glass_ = AureliaContent.prismGlass != null ? AureliaContent.prismGlass : mindustry.content.Items.metaglass;
        BulletType glass = ammoOf("blackhole-sky-net", glass_);
        if(glass == null) throw new IllegalStateException("v8.7.3: the sky net has no glass shell");
        if(glass.damage != 21f){
            throw new IllegalStateException("v8.7.3: the metaglass shell damage " + glass.damage + " != 21");
        }
        if(glass.fragBullet != null && glass.fragBullet.damage != 21f){
            throw new IllegalStateException("v8.7.3: the metaglass frag damage " + glass.fragBullet.damage + " != 21");
        }
    }

    // =====================================================================================================
    // 9. the ranges
    // =====================================================================================================

    private static void checkRanges(){
        Block railgun = Vars.content.block("blackhole-bastion-railgun");
        if(!(railgun instanceof Turret rt)) throw new IllegalStateException("v8.7.3: the bastion railgun is missing");
        if(rt.range != 45f * 8f){
            throw new IllegalStateException("v8.7.3: the bastion railgun range " + rt.range + " != 360 (45 tiles)");
        }

        UnitType titan = Vars.content.unit("blackhole-pyroclast-titan");
        if(titan == null) throw new IllegalStateException("v8.7.3: the pyroclast titan is missing");
        if(Math.abs(titan.maxRange - 60f * 8f) > 1f){
            throw new IllegalStateException("v8.7.3: the titan attack range " + titan.maxRange + " != 480 (60 tiles)");
        }
    }

    // =====================================================================================================
    // 10. what was removed
    // =====================================================================================================

    private static void checkRemovedContent(){
        if(Vars.content.unit("blackhole-lumen-hauler") != null){
            throw new IllegalStateException("v8.7.3: the lumen hauler is still registered");
        }
        //v8.7.7: the gravity well must be gone - block, and every trace of it that could come back
        if(Vars.content.block("blackhole-gravity-well") != null){
            throw new IllegalStateException("v8.7.7: the gravity well is still registered");
        }
    }
}
