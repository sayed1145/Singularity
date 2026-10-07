package blackhole;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * v7.6 block types built around mechanics neither vanilla nor the merged mods have.
 *
 * <ul>
 * <li>{@link Cableway} - 星索缆道. A ropeway: two towers are linked over a long distance and item pods glide along
 * the rope <i>above</i> everything in between - walls, water, buildings, enemy lines. Unlike a bridge it does not
 * need a chain of hops and unlike a mass driver it never stops to batch: a loaded pod leaves as soon as it is
 * full, so the line delivers a steady stream at a throughput no conveyor on the planet reaches.</li>
 * <li>{@link GravityWell} - 引力阱. Not a turret: it bends the ground under hostile units. Everything hostile in
 * range is dragged towards the centre and held there, and whatever reaches the crushing radius is ground down.
 * It cannot shoot, cannot hit air, and friendly units pass through untouched.</li>
 * <li>{@link AegisLattice} - 晶格回馈阵. A defensive bank: it swallows hostile projectiles over an area, stores
 * their damage, and when the bank fills it fires the whole lot back out as a ring-shaped shockwave. The more the
 * enemy shoots at it the harder it hits back, and it is useless against an enemy that does not shoot.</li>
 * <li>{@link LatticeLink} - 晶格链路. Logic memory that every other lattice link of the same team shares. Two
 * processors on opposite sides of the map read and write the same cells without a single link in between.</li>
 * </ul>
 *
 * Everything here keeps its state on its own building, uses only public vanilla calls ({@code Bullet.absorb},
 * {@code Unit.impulse}, status effects, {@code Damage.damage}) and is drawn by procedural geometry, so nothing
 * can desynchronise and nothing can fall back to an error sprite.
 */
public final class FrontierParts{
    private FrontierParts(){}

    // =================================================================================================
    // 星索缆道 - Skyline Cableway
    // =================================================================================================

    /** A ropeway between two towers; pods of items glide over anything in between. */
    public static class Cableway extends Block{
        /** Maximum link distance, in tiles. */
        public int range = 26;
        /** Items one pod carries. */
        public int podItems = 5;
        /** Pods in flight at once. */
        public int maxPods = 6;
        /** Tiles a pod covers per tick. */
        public float podSpeed = 0.42f;
        /** Ticks between two pod departures. */
        public float loadTime = 9f;
        public Color ropeColor = Color.valueOf("b9c8dd"), podColor = Color.valueOf("8fe9ff");

        public BlockModel model;

        public Cableway(String name){
            super(name);
            update = true;
            solid = true;
            hasItems = true;
            configurable = true;
            saveConfig = true;
            clearOnDoubleTap = true;
            copyConfig = false;
            group = BlockGroup.transportation;
            envEnabled = Env.any;
            unloadable = false;
            itemCapacity = 30;

            config(Integer.class, (CablewayBuild b, Integer pos) -> b.link = b.linkValid(pos) ? pos : -1);
            config(Point2.class, (CablewayBuild b, Point2 p) -> {
                int pos = Point2.pack(p.x + b.tile.x, p.y + b.tile.y);
                b.link = b.linkValid(pos) ? pos : -1;
            });
            configClear((CablewayBuild b) -> b.link = -1);
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
            stats.add(Stat.range, range, StatUnit.blocks);
            stats.add(Stat.itemsMoved, podItems * 60f / loadTime, StatUnit.itemsSecond);
        }

        @Override
        public void drawPlace(int x, int y, int rotation, boolean valid){
            super.drawPlace(x, y, rotation, valid);
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range * tilesize, ropeColor);
        }

        public class CablewayBuild extends Building{
            /** packed tile position of the far tower, -1 when this is a terminus */
            public int link = -1;
            public float loadProgress;
            public final Seq<Pod> pods = new Seq<>();

            public boolean linkValid(int pos){
                if(pos == -1 || pos == tile.pos()) return false;
                Building other = world.build(pos);
                return other instanceof CablewayBuild && other.block == block && other.team == team
                    && Mathf.dst(tile.x, tile.y, Point2.x(pos), Point2.y(pos)) <= range;
            }

            public @Nullable CablewayBuild far(){
                return linkValid(link) && world.build(link) instanceof CablewayBuild b ? b : null;
            }

            @Override
            public Integer config(){
                return link;
            }

            @Override
            public void updateTile(){
                CablewayBuild other = far();
                if(other == null){
                    link = -1;
                    //terminus: hand everything to whatever is next to it
                    dump();
                }else{
                    //load a pod whenever one is ready and the rope has room
                    loadProgress += edelta();
                    if(loadProgress >= loadTime && items.total() > 0 && pods.size < maxPods){
                        loadProgress = 0f;
                        Item pick = null;
                        for(Item it : content.items()){
                            if(items.has(it)){
                                pick = it;
                                break;
                            }
                        }
                        if(pick != null){
                            int amount = Math.min(items.get(pick), podItems);
                            items.remove(pick, amount);
                            pods.add(new Pod(pick, amount));
                        }
                    }
                    float dist = Math.max(Mathf.dst(tile.x, tile.y, other.tile.x, other.tile.y), 0.001f);
                    for(int i = pods.size - 1; i >= 0; i--){
                        Pod p = pods.get(i);
                        p.f = Math.min(1f, p.f + podSpeed / dist * Time.delta);
                        if(p.f >= 1f){
                            int accepted = other.acceptStack(p.item, p.amount, this);
                            if(accepted > 0){
                                other.handleStack(p.item, accepted, this);
                                p.amount -= accepted;
                            }
                            //a full far tower simply holds the pod at the end of the rope: no item is ever lost
                            if(p.amount <= 0) pods.remove(i);
                        }
                    }
                }
            }

            @Override
            public boolean acceptItem(Building source, Item item){
                if(items.total() >= itemCapacity) return false;
                //a pod landing at the far end is always taken in, even though that tower is a terminus with no
                //rope of its own - otherwise the last mast of a line could never unload
                if(source.block instanceof Cableway) return true;
                //from the ground, only a tower that actually carries a rope loads up
                return far() != null && source.relativeTo(this) != -1;
            }

            @Override
            public boolean canDump(Building to, Item item){
                return far() == null && !(to.block instanceof Cableway);
            }

            @Override
            public boolean onConfigureBuildTapped(Building other){
                if(other == this){
                    configure(-1);
                    return false;
                }
                if(other instanceof CablewayBuild && other.block == block && linkValid(other.tile.pos())){
                    configure(other.tile.pos() == link ? -1 : other.tile.pos());
                    return false;
                }
                return true;
            }

            @Override
            public void drawConfigure(){
                Drawf.select(x, y, tile.block().size * tilesize / 2f + 2f, mindustry.graphics.Pal.accent);
                Drawf.dashCircle(x, y, range * tilesize, ropeColor);
                CablewayBuild other = far();
                if(other != null) Drawf.select(other.x, other.y, other.block.size * tilesize / 2f + 2f, podColor);
            }

            @Override
            public void draw(){
                if(model != null) model.draw(this); else super.draw();

                CablewayBuild other = far();
                if(other == null) return;
                //the rope sags a little between the two masts, the pods ride on it
                float mz = block.size * tilesize * 0.42f;
                float x0 = x, y0 = y + mz, x1 = other.x, y1 = other.y + mz;
                Draw.z(Layer.power + 0.1f);
                Draw.color(ropeColor, 0.75f);
                Lines.stroke(1.1f);
                Lines.line(x0, y0, x1, y1);
                Draw.color(Color.black, 0.18f);
                Lines.stroke(2.2f);
                Lines.line(x0, y0 - 1.6f, x1, y1 - 1.6f);
                for(Pod p : pods){
                    float px = Mathf.lerp(x0, x1, p.f), py = Mathf.lerp(y0, y1, p.f);
                    //sag: the pod hangs lowest in the middle of the span
                    py -= Mathf.sin(p.f * Mathf.PI) * 2.6f;
                    Draw.color(Color.black, 0.2f);
                    Fill.circle(px, py - 3.4f, 1.5f);
                    Draw.color(ropeColor);
                    Fill.poly(px, py, 6, 2.1f);
                    Draw.color(p.item.color);
                    Fill.poly(px, py, 6, 1.25f);
                    Draw.color(podColor, 0.5f);
                    Lines.stroke(0.5f);
                    Lines.line(px, py + 1.9f, px, py + 3.2f);
                }
                Draw.reset();
            }

            @Override
            public void write(Writes write){
                super.write(write);
                write.i(link);
                write.b(pods.size);
                for(Pod p : pods){
                    write.s(p.item.id);
                    write.s(p.amount);
                    write.f(p.f);
                }
            }

            @Override
            public void read(Reads read, byte revision){
                super.read(read, revision);
                link = read.i();
                int n = read.b();
                pods.clear();
                for(int i = 0; i < n; i++){
                    Item item = content.item(read.s());
                    int amount = read.s();
                    float f = read.f();
                    if(item != null){
                        Pod p = new Pod(item, amount);
                        p.f = f;
                        pods.add(p);
                    }
                }
            }
        }

        /** One hanging crate on the rope. */
        public static class Pod{
            public final Item item;
            public int amount;
            public float f;

            public Pod(Item item, int amount){
                this.item = item;
                this.amount = amount;
            }
        }
    }

    // =================================================================================================
    // 引力阱 - Gravity Well
    // =================================================================================================

    /** Drags hostile ground units into its centre and crushes whatever reaches it. */
    public static class GravityWell extends Block{
        public float radius = 112f, crushRadius = 22f;
        /** pull strength at the rim, damage per second inside the crushing radius */
        public float pull = 1.4f, crushDps = 95f;
        public float shake = 0.6f;
        public StatusEffect apply;
        public float applyDuration = 24f;
        public Color wellColor = Color.valueOf("b69cff");

        public BlockModel model;

        public GravityWell(String name){
            super(name);
            update = true;
            solid = true;
            hasPower = true;
            group = BlockGroup.projectors;
            envEnabled = Env.any;
            buildType = GravityWellBuild::new;
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
            stats.add(Stat.range, radius / tilesize, StatUnit.blocks);
            stats.add(Stat.damage, crushDps, StatUnit.perSecond);
        }

        @Override
        public void drawPlace(int x, int y, int rotation, boolean valid){
            super.drawPlace(x, y, rotation, valid);
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, radius, wellColor);
        }

        public class GravityWellBuild extends Building{
            public float warmup, spin;

            @Override
            public void updateTile(){
                warmup = Mathf.lerpDelta(warmup, efficiency > 0f ? 1f : 0f, 0.04f);
                spin += warmup * Time.delta;
                if(warmup < 0.05f) return;

                float dt = Time.delta * warmup;
                Units.nearbyEnemies(team, x - radius, y - radius, radius * 2f, radius * 2f, u -> {
                    if(u.dead || u.type.flying || u.isBoss() && u.hitSize > 40f) return;
                    float d = u.dst(this);
                    if(d > radius || d < 0.01f) return;
                    float f = (1f - d / radius);
                    Tmp.v1.set(x - u.x, y - u.y).setLength(pull * f * u.mass() * 0.05f * dt);
                    u.impulse(Tmp.v1);
                    if(apply != null) u.apply(apply, applyDuration);
                    if(d < crushRadius){
                        u.damageContinuousPierce(crushDps / 60f * warmup);
                        if(Mathf.chanceDelta(0.12f)) AureliaFx.detainAbsorb.at(u.x, u.y, u.angleTo(this), wellColor);
                    }
                });
                if(shake > 0f && Mathf.chanceDelta(0.05f)) Effect.shake(shake, 6f, this);
            }

            @Override
            public void draw(){
                if(model != null) model.draw(this); else super.draw();
                if(warmup <= 0.02f) return;
                Draw.z(Layer.effect);
                Draw.blend(Blending.additive);
                //three counter-rotating field rings plus in-falling motes: the well is visible, not guessed
                for(int i = 0; i < 3; i++){
                    float rr = radius * (0.95f - i * 0.22f);
                    Draw.color(wellColor, 0.10f * warmup + 0.05f);
                    Lines.stroke(1.2f - i * 0.25f);
                    Lines.poly(x, y, 6 + i * 3, rr, spin * (i % 2 == 0 ? 0.5f : -0.7f));
                }
                for(int i = 0; i < 10; i++){
                    float f = Mathf.mod(spin * 0.01f + i / 10f, 1f);
                    float a = i * 36f + spin * 0.6f;
                    float rr = radius * (1f - f);
                    Draw.color(wellColor, (1f - f) * 0.7f * warmup);
                    Fill.circle(x + Mathf.cosDeg(a) * rr, y + Mathf.sinDeg(a) * rr, 1.1f * (1f - f) + 0.4f);
                }
                Draw.color(wellColor, 0.35f * warmup);
                Fill.poly(x, y, 6, crushRadius * 0.35f + Mathf.absin(spin, 7f, 1.2f), -spin);
                Draw.blend();
                Draw.reset();
            }

            @Override
            public void drawSelect(){
                Drawf.dashCircle(x, y, radius, wellColor);
                Drawf.dashCircle(x, y, crushRadius, Color.valueOf("ff8d6b"));
            }
        }
    }

    // =================================================================================================
    // 晶格回馈阵 - Aegis Lattice
    // =================================================================================================

    /** Eats hostile projectiles over an area, banks their damage, and fires the bank back as a shockwave. */
    public static class AegisLattice extends Block{
        public float radius = 92f;
        /** bank size; the wave goes off when the bank is full */
        public float capacity = 1400f;
        /** damage of the wave per banked point, and its reach */
        public float waveScale = 0.85f, waveRadius = 104f;
        /** energy drained every second while idle, so it cannot sit charged forever */
        public float decay = 18f;
        //v8.1 nerf, the same shape as the absorber wall: the lattice has a hard intake ceiling and has to cool
        //down. It can no longer eat an unlimited barrage, and a discharge locks the field out for a while.
        /** total damage it may absorb before the field collapses */
        public float heatCeiling = 3000f;
        /** damage units of intake shed per second */
        public float coolRate = 24f;
        /** how long the field stays down after it overloads */
        public float rechargeTime = 60f * 7f;
        /** lockout after each wave - the wave is strong, so it may not be spammed */
        public float waveCooldown = 60f * 5f;
        public Color latticeColor = Color.valueOf("8fe9ff");

        public BlockModel model;

        public AegisLattice(String name){
            super(name);
            update = true;
            solid = true;
            hasPower = true;
            group = BlockGroup.projectors;
            envEnabled = Env.any;
            buildType = AegisLatticeBuild::new;
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
            stats.add(Stat.range, radius / tilesize, StatUnit.blocks);
            stats.add(Stat.damage, capacity * waveScale, StatUnit.none);
            stats.add(Stat.shootRange, heatCeiling, StatUnit.none);
            stats.add(Stat.reload, waveCooldown / 60f, StatUnit.seconds);
            stats.add(Stat.cooldownTime, rechargeTime / 60f, StatUnit.seconds);
        }

        @Override
        public void drawPlace(int x, int y, int rotation, boolean valid){
            super.drawPlace(x, y, rotation, valid);
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, radius, latticeColor);
        }

        public class AegisLatticeBuild extends Building{
            public float bank, flash, warmup;
            /** v8.1: absorbed damage since the last cool-off, and the two lockout timers */
            public float heat, inert, lockout;

            /** true while the field is actually catching bullets */
            public boolean fieldUp(){
                return warmup > 0.1f && inert <= 0f && lockout <= 0f;
            }

            @Override
            public void updateTile(){
                warmup = Mathf.lerpDelta(warmup, efficiency > 0f ? 1f : 0f, 0.05f);
                flash = Math.max(0f, flash - Time.delta / 26f);
                if(inert > 0f) inert -= Time.delta;
                if(lockout > 0f) lockout -= Time.delta;
                heat = Math.max(0f, heat - coolRate / 60f * Time.delta);
                if(fieldUp()){
                    Groups.bullet.intersect(x - radius, y - radius, radius * 2f, radius * 2f, b -> {
                        if(b.team == team || !b.type.absorbable || !b.within(this, radius)) return;
                        float dmg = Math.max(b.damage, 1f);
                        bank = Math.min(capacity, bank + dmg);
                        heat += dmg;
                        AureliaFx.detainAbsorb.at(b.x, b.y, b.angleTo(this), latticeColor);
                        b.absorb();
                    });
                    //overload: the field collapses and has to be rebuilt from nothing
                    if(heat >= heatCeiling){
                        heat = 0f;
                        inert = rechargeTime;
                        bank *= 0.5f;
                        AureliaFx.detainDischarge.at(x, y, 0f, latticeColor);
                        Effect.shake(2f, 10f, this);
                    }
                }
                bank = Math.max(0f, bank - decay / 60f * Time.delta);

                if(bank >= capacity && warmup > 0.5f && inert <= 0f && lockout <= 0f){
                    float damage = bank * waveScale;
                    Damage.damage(team, x, y, waveRadius, damage, false, true);
                    AureliaFx.detainDischarge.at(x, y, 0f, latticeColor);
                    OwnFx.shield.at(x, y, waveRadius, latticeColor);
                    Effect.shake(4f, 18f, this);
                    BHSounds.pulse.at(this, 0.9f);
                    bank = 0f;
                    flash = 1f;
                    lockout = waveCooldown;
                }
            }

            @Override
            public float progress(){
                return bank / capacity;
            }

            @Override
            public void draw(){
                if(model != null) model.draw(this); else super.draw();
                float f = bank / capacity;
                Draw.z(Layer.effect);
                Draw.blend(Blending.additive);
                Draw.color(latticeColor, (0.06f + f * 0.2f) * warmup + flash * 0.4f);
                Lines.stroke(1.3f);
                Lines.poly(x, y, 6, radius * (0.4f + 0.6f * Math.max(f, flash)), Time.time * 0.3f);
                //charge bars: one segment per sixth of the bank, so the state is readable at a glance
                int seg = (int)(f * 6f);
                for(int i = 0; i < seg; i++){
                    float a = i * 60f - Time.time * 0.3f;
                    Draw.color(latticeColor, 0.5f * warmup);
                    Fill.poly(x + Mathf.cosDeg(a) * (radius * 0.4f + 1f), y + Mathf.sinDeg(a) * (radius * 0.4f + 1f), 3, 1.8f, a);
                }
                if(flash > 0f){
                    Draw.color(latticeColor, flash * 0.5f);
                    Lines.stroke(3f * flash);
                    Lines.circle(x, y, waveRadius * (1f - flash));
                }
                Draw.blend();
                Draw.reset();
            }

            @Override
            public void drawSelect(){
                Drawf.dashCircle(x, y, radius, fieldUp() ? latticeColor : Color.gray);
                Drawf.dashCircle(x, y, waveRadius, Color.valueOf("b69cff"));
            }

            @Override
            public void write(Writes write){
                super.write(write);
                write.f(bank);
            }

            @Override
            public void read(Reads read, byte revision){
                super.read(read, revision);
                bank = read.f();
            }
        }
    }

    // =================================================================================================
    // 晶格链路 - Lattice Link (shared logic memory)
    // =================================================================================================

    /**
     * Logic memory shared by every lattice link of the same team, anywhere on the map.
     *
     * <p>Vanilla memory cells are local: a processor can only reach the cell it is linked to, so moving a number
     * across the map means a chain of processors. A lattice link is a window into one team-wide bank - write cell
     * 7 here, read cell 7 at the other end of the sector. It is not a memory cell subclass (v8 keeps that memory
     * private); it implements the logic read/write interfaces itself over the shared bank.
     */
    public static class LatticeLink extends Block{
        /** one bank per team, created on demand and dropped when a new world loads */
        static final ObjectMap<Team, double[]> banks = new ObjectMap<>();
        static boolean hooked;

        public int memoryCapacity = 64;

        public LatticeLink(String name){
            super(name);
            update = true;
            solid = true;
            destructible = true;
            group = BlockGroup.logic;
            drawDisabled = false;
            envEnabled = Env.any;
            buildType = LatticeLinkBuild::new;
            if(!hooked){
                hooked = true;
                Events.on(EventType.WorldLoadEvent.class, e -> banks.clear());
            }
        }

        @Override
        public void setStats(){
            super.setStats();
            stats.add(Stat.memoryCapacity, memoryCapacity, StatUnit.none);
        }

        public double[] bank(Team team){
            double[] bank = banks.get(team);
            if(bank == null || bank.length != memoryCapacity) banks.put(team, bank = new double[memoryCapacity]);
            return bank;
        }

        public class LatticeLinkBuild extends Building implements mindustry.logic.LReadable, mindustry.logic.LWritable{
            @Override
            public boolean readable(mindustry.logic.LExecutor exec){
                return true;
            }

            @Override
            public void read(mindustry.logic.LVar position, mindustry.logic.LVar output){
                int address = position.numi();
                double[] bank = bank(team);
                output.setnum(address < 0 || address >= bank.length ? 0d : bank[address]);
            }

            @Override
            public boolean writable(mindustry.logic.LExecutor exec){
                return true;
            }

            @Override
            public void write(mindustry.logic.LVar position, mindustry.logic.LVar value){
                int address = position.numi();
                double[] bank = bank(team);
                if(address >= 0 && address < bank.length) bank[address] = value.num();
            }

            @Override
            public double sense(mindustry.logic.LAccess sensor){
                if(sensor == mindustry.logic.LAccess.memoryCapacity) return memoryCapacity;
                return super.sense(sensor);
            }

            @Override
            public boolean collide(Bullet other){
                return false;
            }

            @Override
            public void draw(){
                super.draw();
                //a slow pulse marks it as a networked block rather than a local one
                Draw.z(Layer.blockOver);
                Draw.color(Color.valueOf("b69cff"), 0.35f + Mathf.absin(Time.time + x + y, 18f, 0.25f));
                Lines.stroke(0.8f);
                Lines.poly(x, y, 6, block.size * tilesize * 0.3f, Time.time * 0.4f);
                Draw.reset();
            }

            @Override
            public void write(Writes write){
                super.write(write);
                //the bank belongs to the team, but one link persists it so a reload keeps the numbers
                double[] bank = bank(team);
                write.s(bank.length);
                for(double v : bank) write.d(v);
            }

            @Override
            public void read(Reads read, byte revision){
                super.read(read, revision);
                int n = read.s();
                double[] bank = bank(team);
                for(int i = 0; i < n; i++){
                    double v = read.d();
                    if(i < bank.length) bank[i] = v;
                }
            }
        }
    }
}
