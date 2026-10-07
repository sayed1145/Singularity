package blackhole;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * v8.1 logistics pack - the drone network.
 *
 * <p>Vanilla's cargo station is a single tether: one drone, one station, and the drone only moves if it is a
 * {@code BuildingTetherc} entity (ours was not, which is why nothing ever flew). This pack throws that away and
 * implements a real logistic network, modelled on the role system factory games settled on after years of
 * iteration (Factorio's provider / requester / storage / buffer chests) with one addition of our own: the
 * assignment is scored by distance, so the nearest free drone takes the nearest matching source instead of
 * robots criss-crossing the map.
 *
 * <p>Roles:
 * <ul>
 * <li><b>Hub</b> ({@link Hub}) - the brain and the depot. It prints and keeps its own drones, adopts any stray
 *     hauler that has no network, scans every node inside its radius and issues the hauls. It is also an
 *     <i>active provider</i>: whatever a belt puts into it is pushed out to whoever needs it.</li>
 * <li><b>Supply post</b> ({@link Supply}) - a <i>passive provider</i>. Belts fill it; drones only take from it
 *     when something actually asked for the item.</li>
 * <li><b>Request pad</b> ({@link Request}) - a <i>requester</i>. Set one item, it is kept full, and the pad
 *     hands the load to whatever it touches.</li>
 * <li><b>Vault</b> ({@link Vault}) - <i>storage</i>. Surplus is dumped here, and a request is served from
 *     storage before any passive provider is touched, so stock circulates instead of piling up.</li>
 * <li><b>Buffer post</b> ({@link Buffer}) - requester and provider at once. It keeps a local stock of one item
 *     near where that item is consumed and serves the requesters around it, which is what breaks the long
 *     round trips on a big base.</li>
 * </ul>
 *
 * <p>Everything is bounded: a hub has a hard drone cap, a hard radius, one haul per drone, and reservations are
 * recomputed from the live hauls every cycle, so no amount can ever be booked twice.
 */
public final class LogixParts{
    /** one haul in flight: take {@code amount} of {@code item} from {@code from}, put it into {@code to} */
    public static class Haul{
        public Building from, to;
        public Item item;
        public int amount;

        public Haul(Building from, Building to, Item item, int amount){
            this.from = from;
            this.to = to;
            this.item = item;
            this.amount = amount;
        }

        public boolean valid(){
            return from != null && to != null && from.isValid() && to.isValid() && item != null && amount > 0;
        }
    }

    /** every logistics node implements this - it is all the network needs to know about a building */
    public interface LogiNode{
        Building build();

        /** 0 = active provider (pushes), 1 = passive provider, 2 = requester, 3 = storage, 4 = buffer */
        int role();

        /** the one item a requester or buffer wants, null for the rest */
        default @Nullable Item want(){
            return null;
        }

        /** how much of this item the node is willing to keep */
        default int target(Item item){
            return build().block.itemCapacity;
        }

        /**
         * v8.2: the real total this node can hold. The vault accepts three times its block capacity, and the
         * scheduler used to guess that number with its own formula - when the two disagreed a drone was sent to
         * a node that then refused the load, and the cargo was stranded in the air. One number, one source.
         */
        default int capacity(){
            return build().block.itemCapacity;
        }
    }

    /** every porter controller alive, so reservations can be recomputed from the real hauls each cycle */
    public static final Seq<PorterAI> porters = new Seq<>();

    private LogixParts(){}

    public static int stock(Building b, Item item){
        return b.items == null ? 0 : b.items.get(item);
    }

    // =====================================================================================================
    // the drone controller
    // =====================================================================================================

    /**
     * Porter AI. Idle drones hover over their hub; a hauled load is picked up at the source and dropped at the
     * destination, and anything that goes wrong (source emptied, destination removed or full) simply returns the
     * drone to the pool - it is never left hovering with a load it cannot place.
     */
    public static class PorterAI extends CommandAI{
        public @Nullable Building home;
        public @Nullable Haul haul;
        public float strayTime, workTime, offloadTime;

        public static final float transferRange = 22f, moveRange = 7f, smoothing = 18f;

        {
            porters.add(this);
        }

        public boolean idle(){
            return haul == null;
        }

        /**
         * v8.2: the hauler is a commandable unit. Extending {@link CommandAI} is what makes it selectable and
         * orderable at all ({@code Unit.isCommandable()} tests for exactly this class), so the drone obeys a
         * player order while one is standing, and falls straight back to hauling when the order is done.
         *
         * <p>A commanded drone gives its reservation back first - it must not hold a job it is not flying - but
         * it keeps whatever is in its hold, and the idle branch below puts that down at the next node.
         */
        @Override
        public void updateUnit(){
            if(hasCommand() || attackTarget != null){
                if(haul != null) release();
                super.updateUnit();
                return;
            }
            updateVisuals();
            updateMovement();
        }

        public @Nullable Unit who(){
            return unit;
        }

        @Override
        public void updateMovement(){
            if(unit == null) return;

            //a tethered drone belongs to its hub; a free hauler looks for one
            if(unit instanceof BuildingTetherc t && t.building() != null) home = t.building();
            if(home != null && !home.isValid()) home = null;
            if(home == null){
                strayTime += Time.delta;
                if(strayTime > 60f){
                    strayTime = 0f;
                    home = findHub();
                }
            }

            if(haul != null && !haul.valid()) release();

            if(haul == null){
                //idle. A drone that still carries something always looks for a place to put it down: it is
                //never thrown away and never parked with a full hold (v8.2 - that was the "items disappear" bug)
                if(unit.hasItem()){
                    offloadTime += Time.delta;
                    if(offloadTime > 30f){
                        offloadTime = 0f;
                        Building drop = bestAcceptor(unit.item(), unit.stack.amount);
                        if(drop != null) haul = new Haul(drop, drop, unit.item(), unit.stack.amount);
                    }
                    if(home != null) moveTo(home, 14f + (unit.id % 5) * 2f, smoothing);
                }else if(home != null){
                    moveTo(home, 14f + (unit.id % 5) * 2f, smoothing);
                }
                return;
            }

            workTime += Time.delta;
            if(workTime > 60f * 60f){
                //a haul that has taken a minute is a haul that will never finish - give the job back, keep the load
                release();
                return;
            }

            boolean carrying = unit.hasItem() && unit.item() == haul.item;
            //from == to means "this is only a drop-off", so the pick-up leg is skipped
            if(!carrying && haul.from != haul.to){
                //pick-up leg
                moveTo(haul.from, moveRange, smoothing);
                if(unit.within(haul.from, transferRange)){
                    if(unit.hasItem() && unit.item() != haul.item){
                        //carrying something else: drop that job, the idle branch will place the old load
                        release();
                        return;
                    }
                    int take = Math.min(Math.min(haul.amount, unit.type.itemCapacity - unit.stack.amount), stock(haul.from, haul.item));
                    if(take > 0){
                        Call.takeItems(haul.from, haul.item, take, unit);
                        haul.amount = unit.stack.amount;
                    }else{
                        //the source ran dry while we flew - nothing was moved, nothing is lost
                        release();
                    }
                }
            }else if(unit.hasItem()){
                //delivery leg
                moveTo(haul.to, moveRange, smoothing);
                if(unit.within(haul.to, transferRange)){
                    int max = Math.min(unit.stack.amount, haul.to.acceptStack(unit.item(), unit.stack.amount, unit));
                    if(max > 0){
                        Call.transferItemTo(unit, unit.item(), max, unit.x, unit.y, haul.to);
                    }
                    if(!unit.hasItem()){
                        haul = null;
                        workTime = 0f;
                    }else if(max <= 0){
                        //target full: keep the cargo, look for somewhere else next tick
                        release();
                    }
                }
            }else{
                //nothing to deliver any more
                release();
            }
        }

        /** gives the job back without ever destroying cargo - the load stays on the drone until it fits somewhere */
        public void release(){
            haul = null;
            workTime = 0f;
            offloadTime = 30f;
        }

        /** nearest node that will really take this load; null when the whole network is full */
        private @Nullable Building bestAcceptor(Item item, int amount){
            Building best = null;
            float bd = Float.MAX_VALUE;
            for(Building p : porterTargets(unit.team, unit.x, unit.y, 520f)){
                if(p.acceptStack(item, amount, unit) <= 0) continue;
                float d = unit.dst(p);
                if(d < bd){
                    bd = d;
                    best = p;
                }
            }
            if(best == null && home != null && home.acceptStack(item, amount, unit) > 0) best = home;
            return best;
        }

        private @Nullable Building findHub(){
            Building[] found = {null};
            float[] bd = {Float.MAX_VALUE};
            indexer.eachBlock(unit.team, unit.x, unit.y, 900f, b -> b instanceof Hub.HubBuild, b -> {
                float d = unit.dst(b);
                if(d < bd[0] && ((Hub.HubBuild)b).canAdopt()){
                    bd[0] = d;
                    found[0] = b;
                }
            });
            if(found[0] != null) ((Hub.HubBuild)found[0]).adopt(unit);
            return found[0];
        }

        private static Seq<Building> porterTargets(Team team, float x, float y, float range){
            Seq<Building> out = new Seq<>();
            indexer.eachBlock(team, x, y, range, b -> b instanceof LogiNode, out::add);
            return out;
        }

        @Override
        public boolean useFallback(){
            return false;
        }
    }

    // =====================================================================================================
    // the hub
    // =====================================================================================================

    /** The network brain: prints drones, adopts strays, scans its radius and issues the hauls. */
    public static class Hub extends Block{
        public BlockModel model;
        public UnitType droneType;
        public int droneCap = 3;
        public float droneBuildTime = 60f * 14f;
        public float range = 58f * tilesize;
        /** nothing smaller than this is worth a flight */
        public int minHaul = 10;

        public Hub(String name){
            super(name);
            update = true;
            solid = true;
            hasItems = true;
            hasPower = true;
            configurable = false;
            itemCapacity = 120;
            group = BlockGroup.transportation;
            flags = EnumSet.of(BlockFlag.unitAssembler);
            buildType = HubBuild::new;
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
            stats.add(Stat.range, range / tilesize, StatUnit.blocks);
            stats.add(Stat.itemCapacity, droneCap, StatUnit.none);
        }

        @Override
        public void drawPlace(int x, int y, int rotation, boolean valid){
            super.drawPlace(x, y, rotation, valid);
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, mindustry.graphics.Pal.accent);
        }

        public class HubBuild extends Building implements LogiNode{
            public Seq<Unit> drones = new Seq<>();
            public float progress;
            /** how many hauls this hub has completed - shown on the block, and used by the tests */
            public int dispatched;
            /** diagnostics: nodes seen and requests raised on the last cycle */
            public int lastNodes, lastRequests;

            @Override
            public Building build(){
                return this;
            }

            @Override
            public int role(){
                return 0;
            }

            public boolean canAdopt(){
                //adoption does not need power - an unpowered hub still owns its drones, it just cannot dispatch
                return drones.size < droneCap + 4;
            }

            public void adopt(Unit u){
                if(!drones.contains(u)) drones.add(u);
            }

            @Override
            public void updateTile(){
                drones.removeAll(u -> u == null || u.dead || !u.isAdded() || u.team != team);

                //v8.2: the drones are controllable. A player (or a logic block) can take one over or give it a
                //command; when the order is finished and nobody is flying it, it goes back to hauling by itself.
                for(Unit u : drones){
                    //a player flying it, or a porter already doing its job, is left alone
                    if(u.isPlayer() || u.controller() instanceof PorterAI) continue;
                    //a plain command controller with nothing left to do is handed back to the network
                    if(u.controller() instanceof CommandAI cai && (cai.hasCommand() || cai.attackTarget != null)) continue;
                    u.controller(new PorterAI());
                }

                //print a drone when short, slowly and only on power
                if(drones.size < droneCap && efficiency > 0.01f){
                    progress += edelta() * state.rules.unitBuildSpeed(team);
                    if(progress >= droneBuildTime){
                        progress = 0f;
                        if(!net.client()){
                            Unit u = droneType.create(team);
                            u.set(x, y);
                            u.rotation = 90f;
                            if(u instanceof BuildingTetherc t) t.building(this);
                            u.add();
                            drones.add(u);
                            Fx.spawn.at(x, y);
                        }
                    }
                }

                //the network cycle
                if(efficiency > 0.01f && timer(timerDump, 24f)){
                    schedule();
                }
            }

            /** builds the work list for this cycle and hands one haul to each idle drone */
            public void schedule(){
                //1. collect the nodes of this network
                Seq<LogiNode> nodes = new Seq<>();
                indexer.eachBlock(team, x, y, range, b -> b instanceof LogiNode, b -> nodes.add((LogiNode)b));
                if(!nodes.contains(this)) nodes.add(this);
                lastNodes = nodes.size;

                //2. recompute reservations from the hauls that are actually in flight - nothing can leak
                porters.removeAll(p -> p.unit() == null || p.unit().dead || !p.unit().isAdded());
                ObjectMap<Building, ObjectIntMap<Item>> in = new ObjectMap<>(), out = new ObjectMap<>();
                for(PorterAI p : porters){
                    Haul hl = p.haul;
                    if(hl == null || !hl.valid()) continue;
                    if(!p.unit().hasItem()){
                        out.get(hl.from, ObjectIntMap::new).increment(hl.item, hl.amount);
                    }
                    in.get(hl.to, ObjectIntMap::new).increment(hl.item, hl.amount);
                }

                //3. which drones are free
                Seq<PorterAI> free = new Seq<>();
                for(Unit u : drones){
                    if(u.controller() instanceof PorterAI ai && ai.idle() && !u.hasItem()) free.add(ai);
                }
                if(free.isEmpty()) return;

                //4. every unmet request, most starved first
                Seq<Object[]> requests = new Seq<>();
                for(LogiNode n : nodes){
                    if(n.role() != 2 && n.role() != 4) continue;
                    Item it = n.want();
                    if(it == null) continue;
                    Building b = n.build();
                    int have = stock(b, it) + in.get(b, ObjectIntMap::new).get(it, 0);
                    int need = n.target(it) - have;
                    if(need < minHaul) continue;
                    //a requester outranks a buffer, and the emptier node is served first
                    float pri = (n.role() == 4 ? 1f : 0f) + (1f - need / (float)Math.max(1, n.target(it)));
                    requests.add(new Object[]{n, it, need, pri});
                }
                requests.sort(o -> (Float)o[3]);
                lastRequests = requests.size;

                //5. match each request with the cheapest source: storage first, then active, then passive
                for(Object[] req : requests){
                    if(free.isEmpty()) break;
                    LogiNode dest = (LogiNode)req[0];
                    Item item = (Item)req[1];
                    int need = (Integer)req[2];
                    LogiNode src = null;
                    float bestScore = Float.MAX_VALUE;
                    int bestAmount = 0;
                    for(LogiNode n : nodes){
                        if(n == dest) continue;
                        int role = n.role();
                        if(role == 2) continue;                                   //requesters never give
                        if(role == 4 && dest.role() == 4) continue;               //buffers do not feed buffers
                        if(role == 4 && n.want() != item) continue;
                        Building b = n.build();
                        int keep = role == 4 ? Math.max(0, n.target(item) / 2) : 0;
                        int avail = stock(b, item) - out.get(b, ObjectIntMap::new).get(item, 0) - keep;
                        if(avail < Math.min(minHaul, need)) continue;
                        float rolePenalty = role == 3 ? 0f : role == 0 ? 120f : role == 4 ? 180f : 240f;
                        float score = rolePenalty + b.dst(dest.build()) / tilesize;
                        if(score < bestScore){
                            bestScore = score;
                            src = n;
                            bestAmount = avail;
                        }
                    }
                    if(src == null) continue;

                    //6. the nearest free drone takes it
                    PorterAI pick = null;
                    float bd = Float.MAX_VALUE;
                    for(PorterAI p : free){
                        float d = p.unit().dst(src.build());
                        if(d < bd){
                            bd = d;
                            pick = p;
                        }
                    }
                    if(pick == null) break;
                    int amount = Math.min(Math.min(need, bestAmount), pick.unit().type.itemCapacity);
                    amount = Math.min(amount, dest.build().acceptStack(item, amount, pick.unit()));
                    if(amount < Math.min(minHaul, need)) continue;
                    pick.haul = new Haul(src.build(), dest.build(), item, amount);
                    pick.workTime = 0f;
                    free.remove(pick);
                    out.get(src.build(), ObjectIntMap::new).increment(item, amount);
                    in.get(dest.build(), ObjectIntMap::new).increment(item, amount);
                    dispatched++;
                }

                //7. surplus: an active provider that nobody asked from pushes its stock into storage
                if(free.isEmpty()) return;
                for(LogiNode n : nodes){
                    if(n.role() != 0 || free.isEmpty()) continue;
                    Building b = n.build();
                    if(b.items == null || b.items.total() < minHaul) continue;
                    Item[] pickItem = {null};
                    b.items.each((item, amount) -> {
                        if(pickItem[0] == null && amount - out.get(b, ObjectIntMap::new).get(item, 0) >= minHaul) pickItem[0] = item;
                    });
                    if(pickItem[0] == null) continue;
                    LogiNode store = null;
                    float bd = Float.MAX_VALUE;
                    for(LogiNode s : nodes){
                        if(s.role() != 3) continue;
                        Building sb = s.build();
                        //v8.2: ask the node itself how much it holds - the old guess sent drones to full vaults
                        if(sb.items.total() >= s.capacity()) continue;
                        float d = sb.dst(b);
                        if(d < bd){
                            bd = d;
                            store = s;
                        }
                    }
                    if(store == null) continue;
                    PorterAI pick = free.first();
                    int amount = Math.min(b.items.get(pickItem[0]), pick.unit().type.itemCapacity);
                    amount = Math.min(amount, store.build().acceptStack(pickItem[0], amount, pick.unit()));
                    if(amount < minHaul) continue;
                    pick.haul = new Haul(b, store.build(), pickItem[0], amount);
                    pick.workTime = 0f;
                    free.remove(pick);
                    out.get(b, ObjectIntMap::new).increment(pickItem[0], amount);
                    dispatched++;
                }
            }

            @Override
            public boolean acceptItem(Building source, Item item){
                return items.total() < block.itemCapacity;
            }

            @Override
            public void drawSelect(){
                Drawf.dashCircle(x, y, range, mindustry.graphics.Pal.accent);
            }

            @Override
            public void draw(){
                if(model != null){
                    model.draw(this);
                }else{
                    super.draw();
                }
            }

            @Override
            public void onRemoved(){
                super.onRemoved();
                for(Unit u : drones){
                    if(u == null || u.dead) continue;
                    if(u.controller() instanceof PorterAI ai && ai.home == this){
                        ai.home = null;
                        ai.release();
                    }
                    //v8.2: a tethered drone dies with its hub, but its cargo does not vanish - it is handed to
                    //the nearest node that will take it, and only dropped on the ground if nothing will
                    if(u instanceof BuildingTetherc){
                        if(u.hasItem()){
                            Building[] best = {null};
                            float[] bd = {Float.MAX_VALUE};
                            indexer.eachBlock(u.team, u.x, u.y, 320f, b -> b.acceptStack(u.item(), u.stack.amount, u) > 0, b -> {
                                float d = u.dst(b);
                                if(d < bd[0]){
                                    bd[0] = d;
                                    best[0] = b;
                                }
                            });
                            if(best[0] != null) Call.transferItemTo(u, u.item(), Math.min(u.stack.amount,
                                best[0].acceptStack(u.item(), u.stack.amount, u)), u.x, u.y, best[0]);
                        }
                        u.kill();
                    }
                }
                drones.clear();
            }

            @Override
            public void write(Writes write){
                super.write(write);
                write.f(progress);
            }

            @Override
            public void read(Reads read, byte revision){
                super.read(read, revision);
                progress = read.f();
            }
        }
    }

    // =====================================================================================================
    // the passive nodes
    // =====================================================================================================

    /** shared plumbing for the four non-hub nodes: 3D model, item module, no sprite lookups */
    public abstract static class Node extends Block{
        public BlockModel model;

        public Node(String name, int size){
            super(name);
            this.size = size;
            update = true;
            solid = true;
            hasItems = true;
            group = BlockGroup.transportation;
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

        public abstract class NodeBuild extends Building{
            @Override
            public void draw(){
                if(model != null){
                    model.draw(this);
                }else{
                    super.draw();
                }
            }
        }
    }

    /** Passive provider: belts fill it, drones only take from it when something asked. */
    public static class Supply extends Node{
        public Supply(String name){
            super(name, 2);
            itemCapacity = 120;
            buildType = SupplyBuild::new;
        }

        public class SupplyBuild extends NodeBuild implements LogiNode{
            @Override
            public Building build(){
                return this;
            }

            @Override
            public int role(){
                return 1;
            }

            @Override
            public boolean acceptItem(Building source, Item item){
                return items.total() < block.itemCapacity;
            }
        }
    }

    /** Storage: the dump target for surplus, and the first place a request is served from. */
    public static class Vault extends Node{
        public Vault(String name){
            super(name, 3);
            itemCapacity = 300;
            buildType = VaultBuild::new;
        }

        public class VaultBuild extends NodeBuild implements LogiNode{
            @Override
            public Building build(){
                return this;
            }

            @Override
            public int role(){
                return 3;
            }

            @Override
            public int capacity(){
                return block.itemCapacity * 3;
            }

            @Override
            public int target(Item item){
                return block.itemCapacity * 3;
            }

            @Override
            public boolean acceptItem(Building source, Item item){
                return items.total() < capacity();
            }

            @Override
            public int acceptStack(Item item, int amount, Teamc source){
                return Math.max(0, Math.min(amount, capacity() - items.total()));
            }
        }
    }

    /** Requester: one item, kept topped up, handed to whatever the pad touches. */
    public static class Request extends Node{
        public Request(String name, int size){
            super(name, size);
            itemCapacity = 60;
            configurable = true;
            saveConfig = true;
            clearOnDoubleTap = true;
            buildType = RequestBuild::new;

            config(Item.class, (RequestBuild build, Item item) -> build.sought = item);
            configClear((RequestBuild build) -> build.sought = null);
        }

        @Override
        public boolean outputsItems(){
            //the pad hands its load on, so it must be able to feed a belt
            return true;
        }

        public class RequestBuild extends NodeBuild implements LogiNode{
            public @Nullable Item sought;

            @Override
            public Building build(){
                return this;
            }

            @Override
            public int role(){
                return 2;
            }

            @Override
            public @Nullable Item want(){
                return sought;
            }

            @Override
            public void updateTile(){
                if(items.total() > 0 && timer(timerDump, 4f)) dump();
            }

            @Override
            public boolean acceptItem(Building source, Item item){
                //only the drones fill a pad; a belt cannot push into it
                return false;
            }

            @Override
            public int acceptStack(Item item, int amount, Teamc source){
                if(sought == null || item != sought) return 0;
                return Math.max(0, Math.min(amount, block.itemCapacity - items.total()));
            }

            @Override
            public boolean canDump(Building to, Item item){
                //v8.2: a pad feeds machines and belts, never another logistics node. Dumping back into the
                //network let a pad and a vault trade the same load forever - the drones never stopped flying.
                return !(to instanceof LogiNode);
            }

            @Override
            public void buildConfiguration(Table table){
                ItemSelection.buildTable(block, table, content.items().select(i -> i.unlockedNow() &&
                    i.name.startsWith("blackhole-")), () -> sought, this::configure, selectionRows, selectionColumns);
            }

            @Override
            public Item config(){
                return sought;
            }

            @Override
            public void drawSelect(){
                if(sought != null){
                    float dx = x - size * tilesize / 2f, dy = y + size * tilesize / 2f;
                    Draw.rect(sought.fullIcon, dx, dy, 4f, 4f);
                    Draw.reset();
                }
            }

            @Override
            public void write(Writes write){
                super.write(write);
                write.s(sought == null ? -1 : sought.id);
            }

            @Override
            public void read(Reads read, byte revision){
                super.read(read, revision);
                int id = read.s();
                sought = id == -1 ? null : content.item(id);
            }
        }
    }

    /**
     * Buffer: a requester that is also a provider. It parks a stock of one item next to where that item is
     * used, so the short hauls are local and the long hauls happen once.
     */
    public static class Buffer extends Node{
        public Buffer(String name){
            super(name, 2);
            itemCapacity = 200;
            configurable = true;
            saveConfig = true;
            clearOnDoubleTap = true;
            buildType = BufferBuild::new;

            config(Item.class, (BufferBuild build, Item item) -> build.sought = item);
            configClear((BufferBuild build) -> build.sought = null);
        }

        public class BufferBuild extends NodeBuild implements LogiNode{
            public @Nullable Item sought;

            @Override
            public Building build(){
                return this;
            }

            @Override
            public int role(){
                return 4;
            }

            @Override
            public @Nullable Item want(){
                return sought;
            }

            @Override
            public boolean acceptItem(Building source, Item item){
                return sought != null && item == sought && items.total() < block.itemCapacity;
            }

            @Override
            public int acceptStack(Item item, int amount, Teamc source){
                if(sought == null || item != sought) return 0;
                return Math.max(0, Math.min(amount, block.itemCapacity - items.total()));
            }

            @Override
            public void buildConfiguration(Table table){
                ItemSelection.buildTable(block, table, content.items().select(i -> i.unlockedNow() &&
                    i.name.startsWith("blackhole-")), () -> sought, this::configure, selectionRows, selectionColumns);
            }

            @Override
            public Item config(){
                return sought;
            }

            @Override
            public void write(Writes write){
                super.write(write);
                write.s(sought == null ? -1 : sought.id);
            }

            @Override
            public void read(Reads read, byte revision){
                super.read(read, revision);
                int id = read.s();
                sought = id == -1 ? null : content.item(id);
            }
        }
    }

    // =====================================================================================================
    // liquids
    // =====================================================================================================

    /**
     * Wireless liquid relay - the new liquid transport. A relay links itself to every other relay in range
     * (no configuration, no pipes) and the whole linked set is levelled every tick: liquid flows from the
     * fuller relay to the emptier one, at a capped rate, and each relay feeds the blocks it touches. One
     * pylon line replaces a conduit run over broken ground, and because it levels rather than pumps, it can
     * never move more than the cap - there is no infinite throughput exploit.
     */
    public static class LiquidRelay extends Block{
        public BlockModel model;
        public float range = 22f * tilesize;
        /** units of liquid moved per tick between two relays */
        public float transferRate = 9f;
        public int maxLinks = 6;

        public LiquidRelay(String name){
            super(name);
            size = 2;
            update = true;
            solid = true;
            hasLiquids = true;
            outputsLiquid = true;
            liquidCapacity = 300f;
            group = BlockGroup.liquids;
            buildType = RelayBuild::new;
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
            stats.add(Stat.range, range / tilesize, StatUnit.blocks);
            stats.add(Stat.maxConsecutive, maxLinks, StatUnit.none);
        }

        @Override
        public void drawPlace(int x, int y, int rotation, boolean valid){
            super.drawPlace(x, y, rotation, valid);
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, mindustry.graphics.Pal.accent);
        }

        public class RelayBuild extends Building{
            public Seq<RelayBuild> links = new Seq<>();

            @Override
            public void updateTile(){
                if(timer(timerDump, 40f)){
                    links.clear();
                    indexer.eachBlock(team, x, y, range, b -> b instanceof RelayBuild && b != this, b -> {
                        if(links.size < maxLinks) links.add((RelayBuild)b);
                    });
                }

                Liquid cur = liquids.current();
                if(liquids.get(cur) > 0.01f){
                    //level with the emptiest link
                    RelayBuild low = null;
                    float best = liquids.get(cur);
                    for(RelayBuild r : links){
                        if(!r.isValid()) continue;
                        float amount = r.liquids.get(cur);
                        if(amount < best - 1f && r.acceptLiquid(this, cur)){
                            best = amount;
                            low = r;
                        }
                    }
                    if(low != null){
                        float move = Math.min(Math.min((liquids.get(cur) - best) / 2f, transferRate * edelta()),
                            low.block.liquidCapacity - low.liquids.get(cur));
                        if(move > 0.01f){
                            liquids.remove(cur, move);
                            low.liquids.add(cur, move);
                        }
                    }
                    dumpLiquid(cur);
                }
            }

            @Override
            public boolean acceptLiquid(Building source, Liquid liquid){
                return liquids.current() == liquid || liquids.currentAmount() < 0.2f;
            }

            @Override
            public void drawSelect(){
                Drawf.dashCircle(x, y, range, mindustry.graphics.Pal.accent);
                for(RelayBuild r : links){
                    if(r.isValid()) Drawf.line(mindustry.graphics.Pal.accent, x, y, r.x, r.y);
                }
            }

            @Override
            public void draw(){
                if(model != null){
                    model.draw(this);
                }else{
                    super.draw();
                }
            }
        }
    }

    /** Liquid tank: a plain store, drawn by its 3D model. Small and large share this class. */
    public static class ModelLiquidTank extends LiquidRouter{
        public BlockModel model;

        public ModelLiquidTank(String name){
            super(name);
            buildType = TankBuild::new;
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

        public class TankBuild extends LiquidRouterBuild{
            @Override
            public void draw(){
                if(model != null){
                    model.draw(this);
                    //the level is drawn on top of the model, so a tank reads at a glance
                    if(liquids.currentAmount() > 0.01f){
                        Draw.color(liquids.current().color, liquids.currentAmount() / block.liquidCapacity * 0.75f);
                        Draw.alpha(Math.min(1f, liquids.currentAmount() / block.liquidCapacity) * 0.5f);
                        Fill.square(x, y, (size * tilesize - 6f) / 2f);
                        Draw.color();
                    }
                }else{
                    super.draw();
                }
            }
        }
    }

    /** Overflow / underflow gate with a 3D model instead of a sprite. */
    public static class ModelOverflowGate extends OverflowGate{
        public BlockModel model;

        public ModelOverflowGate(String name){
            super(name);
            buildType = GateBuild::new;
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

        public class GateBuild extends OverflowGateBuild{
            @Override
            public void draw(){
                if(model != null){
                    model.draw(this);
                }else{
                    super.draw();
                }
            }
        }
    }
}
