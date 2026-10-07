package blackhole;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

/**
 * v7.5 block types that vanilla has no equivalent for.
 *
 * <ul>
 * <li>{@link BeamMiner} - the rift collector. It does not mine the tile underneath it; it needs no ore at all to
 * be placed, only free space (it even floats over liquids). Instead it fires a collection beam in the direction
 * it was built facing and lifts ore out of any tile inside that cone, through walls, buildings and water.</li>
 * <li>{@link RepairStation} - repairs <b>units</b> (the mender line already covers buildings). It keeps a list of
 * damaged friendly units in range and pours health into the closest ones, with one beam per unit.</li>
 * </ul>
 *
 * Both are drawn by their 3D {@link BlockModel}; every extra element (beams, target markers, range rings) is
 * procedural geometry, so there is no sprite lookup that could fail and paint the error region.
 */
public final class IndustryParts{
    private IndustryParts(){}

    /** Directional, ground independent ore collection - the "Dyson" miner. */
    public static class BeamMiner extends Block{
        /** Highest ore hardness this collector can lift. */
        public int tier = 4;
        /** Ticks per item at full efficiency, before the hardness penalty. */
        public float mineTime = 42f;
        /** Reach of the collection beam, in world units. */
        public float range = 120f;
        /** Half angle of the collection cone, in degrees. */
        public float coneDeg = 32f;
        /** How many ore tiles are worked at once (one beam each). */
        public int beams = 1;
        public Color beamColor = Color.valueOf("8fe9ff");
        public float beamWidth = 1.4f;

        public BlockModel model;
        public TextureRegion preview;
        public final int timerScan = timers++;
        /** tile offsets of the cone, one list per heading - built once, see {@link #coneOffsets(int)} */
        private int[][] cone;

        public BeamMiner(String name){
            super(name);
            update = true;
            solid = true;
            rotate = true;
            //the dish is part of the 3D model, so the flat rotation of the base sprite must stay off
            rotateDraw = false;
            hasItems = true;
            hasPower = true;
            group = BlockGroup.drills;
            envEnabled = Env.any;
            //no ore requirement, and it can even be dropped on water: "wherever there is room"
            floating = true;
            ambientSound = Sounds.loopMineBeam;
            ambientSoundVolume = 0.04f;
            //v7.6: the collector is configurable - the player overrides which ore the beam lifts
            configurable = true;
            saveConfig = true;
            clearOnDoubleTap = true;
            buildType = BeamMinerBuild::new;

            config(Item.class, (BeamMinerBuild b, Item i) -> {
                b.forced = i;
                b.scan();
            });
            configClear((BeamMinerBuild b) -> {
                b.forced = null;
                b.scan();
            });
        }

        @Override
        public void load(){
            super.load();
            model = Models.get(name);
            if(model != null) model.load();
            preview = Core.atlas.find(name + "-preview", region);
        }

        @Override
        public TextureRegion[] icons(){
            return new TextureRegion[]{Core.atlas.find(name + "-preview", region)};
        }

        @Override
        public void getRegionsToOutline(Seq<TextureRegion> out){
            //the baked model carries its own outline
        }

        @Override
        public boolean outputsItems(){
            return true;
        }

        @Override
        public void setStats(){
            super.stats.timePeriod = mineTime;
            super.setStats();
            //v7.9: ground AND walls - anything in front of the dish whose hardness fits
            stats.add(Stat.drillTier, StatValues.blocks(b ->
                (b instanceof mindustry.world.blocks.environment.Floor f && f.itemDrop != null && f.itemDrop.hardness <= tier)
                || (b.itemDrop != null && b.itemDrop.hardness <= tier && b.solid)));
            stats.add(Stat.range, range / Vars.tilesize, StatUnit.blocks);
            stats.add(Stat.speed, 60f / mineTime, StatUnit.itemsSecond);
        }

        @Override
        public void drawPlace(int x, int y, int rotation, boolean valid){
            super.drawPlace(x, y, rotation, valid);
            float wx = x * Vars.tilesize + offset, wy = y * Vars.tilesize + offset;
            Drawf.dashCircle(wx, wy, range, beamColor);
            //the cone the collector will actually work in
            float dir = rotation * 90f;
            Draw.color(beamColor, 0.7f);
            Lines.stroke(1f);
            for(int sd = -1; sd <= 1; sd += 2){
                float a = dir + sd * coneDeg;
                Lines.line(wx, wy, wx + Angles.trnsx(a, range), wy + Angles.trnsy(a, range));
            }
            Lines.arc(wx, wy, range, coneDeg * 2f / 360f, dir - coneDeg);
            Draw.reset();

            int count = 0;
            Item found = null;
            for(int off : coneOffsets(Mathf.mod(rotation, 4))){
                Item drop = yieldOf(Vars.world.tile(x + Point2.x(off), y + Point2.y(off)));
                if(drop == null) continue;
                count++;
                if(found == null) found = drop;
            }
            if(count > 0 && found != null){
                drawPlaceText(Core.bundle.format("bar.drillspeed", Strings.fixed(60f / itemTime(found) * Math.min(beams, count), 2))
                    + " " + found.localizedName, x, y, valid);
            }else{
                drawPlaceText(Core.bundle.get("bar.noresources"), x, y, false);
            }
        }

        public boolean inCone(int dx, int dy, float dir){
            if(dx == 0 && dy == 0) return false;
            float dst = Mathf.dst(dx, dy) * Vars.tilesize;
            if(dst > range) return false;
            return Angles.angleDist(Angles.angle(dx, dy), dir) <= coneDeg;
        }

        /**
         * v7.9 performance: the cone never changes shape, only which way it points. The tile offsets of all four
         * headings are worked out once, on the first scan, and every later scan just walks that short list
         * (a few hundred entries) instead of testing the whole bounding square with trigonometry. Scanning is
         * what the collector does most, so this is where the frame time was going.
         */
        public int[] coneOffsets(int rotation){
            if(cone == null) cone = new int[4][];
            if(cone[rotation] != null) return cone[rotation];
            float dir = rotation * 90f;
            int tr = Mathf.ceil(range / Vars.tilesize);
            IntSeq out = new IntSeq();
            for(int dx = -tr; dx <= tr; dx++){
                for(int dy = -tr; dy <= tr; dy++){
                    if(inCone(dx, dy, dir)) out.add(Point2.pack(dx, dy));
                }
            }
            return cone[rotation] = out.toArray();
        }

        /**
         * v7.9: what a single tile is worth to this collector. The ground decides, not a hard-coded list - a
         * floor's own drop, an ore overlay on it, or the ore sitting inside a rock wall (walls answer with
         * {@code wallDrop}). Anything whose hardness is above the collector's tier is ignored.
         */
        public @Nullable Item yieldOf(Tile t){
            if(t == null) return null;
            Item drop = t.solid() ? t.wallDrop() : t.drop();
            return drop == null || drop.hardness > tier ? null : drop;
        }

        /** Ticks one item of this ore takes. */
        public float itemTime(Item item){
            return mineTime * (1f + item.hardness * 0.4f);
        }

        public class BeamMinerBuild extends Building{
            /** Tiles currently being lifted. */
            public Seq<Tile> targets = new Seq<>(4);
            public @Nullable Item item;
            /** v7.6: player override. null = the collector picks the richest ore in its cone itself. */
            public @Nullable Item forced;
            public float progress, warmup;
            /** v7.9: cached view of the ground in front, refreshed twice a second */
            private @Nullable Seq<Item> visible;
            private float visScan = -999f;
            private final ObjectIntMap<Item> counts = new ObjectIntMap<>();
            private final Seq<Tile> found = new Seq<>();
            private int lastRot = -1;

            @Override
            public @Nullable Item config(){
                return forced;
            }

            /**
             * v7.9: the selector is the ground. It lists exactly what the tiles in front of the dish yield right
             * now - floor drops, ore overlays and ore inside walls alike - so turning the collector or mining a
             * patch out changes the choices on the spot. There is no fixed ore list any more.
             */
            @Override
            public void buildConfiguration(arc.scene.ui.layout.Table table){
                Seq<Item> options = visibleOres();
                if(options.isEmpty()){
                    table.add("@bar.noresources").pad(6f);
                    return;
                }
                mindustry.world.blocks.ItemSelection.buildTable(BeamMiner.this, table, options,
                    () -> forced, this::configure, selectionRows, selectionColumns);
            }

            /**
             * Everything the cone can see at this moment, richest first. The result is cached for half a second:
             * the configuration table and the tooltip ask for it every frame, and re-walking the cone 60 times a
             * second is exactly the kind of thing that makes a map stutter.
             */
            public Seq<Item> visibleOres(){
                if(Time.time - visScan < 30f && visible != null) return visible;
                visScan = Time.time;
                if(visible == null) visible = new Seq<>(4);
                visible.clear();
                counts.clear();
                for(int off : coneOffsets(Mathf.mod(rotation, 4))){
                    Item drop = yieldOf(Vars.world.tile(tile.x + Point2.x(off), tile.y + Point2.y(off)));
                    if(drop != null) counts.increment(drop);
                }
                for(var e : counts) visible.add(e.key);
                visible.sort(i -> -counts.get(i, 0));
                return visible;
            }

            @Override
            public void updateTile(){
                if(lastRot != rotation){
                    //turned by the player: the ground in front is a different piece of ground
                    lastRot = rotation;
                    visScan = -999f;
                    scan();
                }
                if(timer(timerScan, 45f) || targets.size != Math.min(beams, targets.size) || !valid()){
                    scan();
                }

                boolean work = !targets.isEmpty() && item != null && efficiency > 0 && items.total() < itemCapacity;
                warmup = Mathf.lerpDelta(warmup, work ? 1f : 0f, 0.05f);

                if(work){
                    progress += edelta() * warmup * targets.size;
                    float need = itemTime(item);
                    while(progress >= need && items.total() < itemCapacity){
                        progress -= need;
                        offload(item);
                    }
                    if(progress > need) progress = need;
                }
                dump();
            }

            boolean valid(){
                for(Tile t : targets){
                    if(yieldOf(t) != item || item == null) return false;
                }
                return true;
            }

            public void scan(){
                targets.clear();
                item = null;
                counts.clear();
                found.clear();
                //v7.9: walk the precomputed cone, and let the ground say what is there
                for(int off : coneOffsets(Mathf.mod(rotation, 4))){
                    Tile t = Vars.world.tile(tile.x + Point2.x(off), tile.y + Point2.y(off));
                    Item drop = yieldOf(t);
                    if(drop == null) continue;
                    counts.increment(drop);
                    found.add(t);
                }
                if(found.isEmpty()){
                    //the ground in front changed and the forced ore is no longer there - forget it, so the
                    //collector goes back to picking for itself instead of standing idle for ever
                    forced = null;
                    return;
                }
                Item best = null;
                //a configured ore always wins, as long as the cone can actually see it; otherwise the collector
                //falls back to the richest ore in front of it.
                if(forced != null && counts.containsKey(forced)) best = forced;
                if(best == null){
                    int bestCount = -1;
                    for(var e : counts){
                        if(e.value > bestCount || (e.value == bestCount && best != null && e.key.hardness > best.hardness)){
                            best = e.key;
                            bestCount = e.value;
                        }
                    }
                }
                item = best;
                final Item sel = best;
                found.removeAll(t -> yieldOf(t) != sel);
                found.sort(t -> dst(t.worldx(), t.worldy()));
                if(found.size > beams) found.truncate(beams);
                targets.addAll(found);
            }

            @Override
            public void draw(){
                if(model != null){
                    model.draw(this);
                }else{
                    super.draw();
                }

                if(warmup > 0.02f && item != null){
                    float a = warmup * (0.55f + Mathf.absin(Time.time, 7f, 0.2f));
                    //emitter sits on top of the dish; the beam is pure geometry, no sprites
                    float ex = x, ey = y + size * 1.6f;
                    Draw.z(Layer.effect);
                    for(Tile t : targets){
                        float tx = t.worldx(), ty = t.worldy();
                        Draw.color(beamColor, a * 0.35f);
                        Lines.stroke(beamWidth * 2.2f);
                        Lines.line(ex, ey, tx, ty);
                        Draw.color(Color.white, item.color, 0.65f);
                        Draw.alpha(a);
                        Lines.stroke(beamWidth * 0.8f);
                        Lines.line(ex, ey, tx, ty);
                        //collection marker over the worked tile
                        Draw.color(item.color, a);
                        Lines.stroke(0.9f);
                        Lines.poly(tx, ty, 6, 2.6f + Mathf.absin(Time.time, 9f, 0.5f), Time.time * 1.2f);
                        Fill.poly(tx, ty, 6, 1.1f, -Time.time * 1.6f);
                    }
                    Draw.color(item.color, a * 0.8f);
                    Fill.circle(ex, ey, 1.3f + Mathf.absin(Time.time, 6f, 0.35f));
                    Draw.reset();
                }
            }

            @Override
            public void drawSelect(){
                Drawf.dashCircle(x, y, range, beamColor);
                //the cone, so the player can see what a different ore choice would have to fall into
                float dir = rotdeg();
                Draw.color(beamColor, 0.6f);
                Lines.stroke(1f);
                for(int sd = -1; sd <= 1; sd += 2){
                    float a = dir + sd * coneDeg;
                    Lines.line(x, y, x + Angles.trnsx(a, range), y + Angles.trnsy(a, range));
                }
                if(forced != null){
                    Draw.color(forced.color);
                    Fill.poly(x, y + size * Vars.tilesize / 2f + 3f, 6, 2.2f, Time.time);
                }
                Draw.reset();
            }

            @Override
            public float progress(){
                return item == null ? 0f : Mathf.clamp(progress / itemTime(item));
            }

            @Override
            public boolean shouldConsume(){
                return items.total() < itemCapacity && !targets.isEmpty();
            }

            @Override
            public void write(arc.util.io.Writes write){
                super.write(write);
                write.f(progress);
                write.s(forced == null ? -1 : forced.id);
            }

            @Override
            public void read(arc.util.io.Reads read, byte revision){
                super.read(read, revision);
                progress = read.f();
                short id = read.s();
                forced = id < 0 ? null : Vars.content.item(id);
            }
        }
    }

    /** Repairs friendly units in range (buildings are the menders' job). */
    public static class RepairStation extends Block{
        public float repairRadius = 80f;
        /** Health per second at full efficiency, per unit. */
        public float repairSpeed = 80f;
        /** How many units are serviced at once. */
        public int maxTargets = 1;
        public Color beamColor = Color.valueOf("8fe9ff");

        public BlockModel model;
        public final int timerTarget = timers++;

        public RepairStation(String name){
            super(name);
            update = true;
            solid = true;
            hasPower = true;
            group = BlockGroup.projectors;
            envEnabled = Env.any;
            flags = EnumSet.of(BlockFlag.repair);
            buildType = RepairStationBuild::new;
        }

        @Override
        public void load(){
            super.load();
            model = Models.get(name);
            if(model != null) model.load();
        }

        @Override
        public TextureRegion[] icons(){
            return new TextureRegion[]{Core.atlas.find(name + "-preview", region)};
        }

        @Override
        public void getRegionsToOutline(Seq<TextureRegion> out){
        }

        @Override
        public void setStats(){
            super.setStats();
            stats.add(Stat.repairSpeed, repairSpeed, StatUnit.perSecond);
            stats.add(Stat.range, repairRadius / Vars.tilesize, StatUnit.blocks);
            stats.add(Stat.targetsAir, true);
            stats.add(Stat.targetsGround, true);
        }

        @Override
        public void drawPlace(int x, int y, int rotation, boolean valid){
            super.drawPlace(x, y, rotation, valid);
            Drawf.dashCircle(x * Vars.tilesize + offset, y * Vars.tilesize + offset, repairRadius, beamColor);
        }

        public class RepairStationBuild extends Building{
            public Seq<Unit> patients = new Seq<>(4);
            public float warmup;
            /** Heading of the first patient, in degrees; -1000 when idle (the model then sweeps slowly). */
            public float aimAngle = -1000f;

            @Override
            public void updateTile(){
                if(timer(timerTarget, 20f)) findUnits();
                patients.removeAll(u -> u == null || u.dead() || !u.isAdded() || !u.damaged() || u.dst(this) > repairRadius);

                boolean work = !patients.isEmpty() && efficiency > 0;
                warmup = Mathf.lerpDelta(warmup, work ? 1f : 0f, 0.07f);

                if(work){
                    float heal = repairSpeed / 60f * edelta() * warmup;
                    for(Unit u : patients) u.heal(heal);
                    Unit first = patients.first();
                    aimAngle = Angles.angle(x, y, first.x, first.y);
                }else if(patients.isEmpty()){
                    aimAngle = -1000f;
                }
            }

            void findUnits(){
                patients.clear();
                Units.nearby(team, x - repairRadius, y - repairRadius, repairRadius * 2f, repairRadius * 2f, u -> {
                    if(u.dead() || !u.damaged() || u.dst(this) > repairRadius) return;
                    patients.add(u);
                });
                if(patients.size > 1) patients.sort(u -> u.healthf() * 1000f + u.dst(this));
                if(patients.size > maxTargets) patients.truncate(maxTargets);
            }

            @Override
            public void draw(){
                if(model != null){
                    model.draw(this);
                }else{
                    super.draw();
                }

                if(warmup > 0.02f){
                    float a = warmup * (0.6f + Mathf.absin(Time.time, 6f, 0.2f));
                    float ex = x, ey = y + size * 1.4f;
                    Draw.z(Layer.effect);
                    for(Unit u : patients){
                        Draw.color(beamColor, a * 0.3f);
                        Lines.stroke(2.6f);
                        Lines.line(ex, ey, u.x, u.y);
                        Draw.color(Color.white, beamColor, 0.6f);
                        Draw.alpha(a);
                        Lines.stroke(0.9f);
                        Lines.line(ex, ey, u.x, u.y);
                        Draw.color(beamColor, a * 0.8f);
                        Lines.stroke(1f);
                        Lines.circle(u.x, u.y, u.hitSize * 0.75f + Mathf.absin(Time.time, 8f, 1.2f));
                    }
                    Draw.color(beamColor, a);
                    Fill.circle(ex, ey, 1.1f + Mathf.absin(Time.time, 5f, 0.3f));
                    Draw.reset();
                }
            }

            @Override
            public void drawSelect(){
                Drawf.dashCircle(x, y, repairRadius, beamColor);
            }

            @Override
            public boolean shouldConsume(){
                return !patients.isEmpty();
            }
        }
    }
}
