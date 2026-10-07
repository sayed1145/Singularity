package blackhole;

import arc.struct.*;
import mindustry.world.blocks.environment.*;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.maps.generators.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/**
 * Procedural Aurelia campaign maps (v7). Biomes: crystal plate plains, prism shale ridges, lumen-moss hollows,
 * lilac dunes around aurora lakes (shallow and deep water), resonance beds and raised crystal massifs. Every
 * landing gets the Aurelia Core stocked with lumenite and aurite, lumenite and aurite ore next to it, a silt dune
 * and a tidewater pond, four resonance-bed objectives and visible wave approaches. Only Aurelia content is used
 * (v7.1): no vanilla ore, wall ore, decoration or wave unit.
 */
public class AureliaPlanetGenerator extends PlanetGenerator{
    private static final Color deep = Color.valueOf("1e3b66"), water = Color.valueOf("3a6c9c"), sand = Color.valueOf("9c93ab");
    private static final Color plate = Color.valueOf("536b8f"), bed = Color.valueOf("8067a8"), shale = Color.valueOf("3a3f4c"), moss = Color.valueOf("2f6a6f");

    /**
     * v7.8: Aurelia is a seven map campaign. These are the only sectors you can land on, and each one is a
     * hand tuned layout rather than "whatever the noise produced": a wide open middle, wave approaches at the
     * same distance from the core and far enough away to give you room, ore spread evenly over the whole map,
     * and a connectivity pass that guarantees nothing - friendly or hostile - can end up walled in.
     */
    public static final class Preset{
        public final int id, size, spawns;
        public final float threat, water, ore;
        public final int winWave;

        Preset(int id, int size, int spawns, float threat, float water, float ore, int winWave){
            this.id = id;
            this.size = size;
            this.spawns = spawns;
            this.threat = threat;
            this.water = water;
            this.ore = ore;
            this.winWave = winWave;
        }
    }

    /**
     * v8.0: the campaign is no longer seven hand written maps. {@link #campaign} is built once, from a fixed
     * seed, as <b>32 sectors</b> spread over the whole globe. Every entry draws its own size, spawn count,
     * water level, ore richness and win wave, and the terrain itself is generated from the sector id, so no
     * two sectors can come out the same map. The table is still ordered: index 0 is the landing sector and
     * threat climbs smoothly to the last one, so the run stays playable from start to finish.
     */
    public static final int campaignSectors = 32;

    /** every landing site, in the order you are meant to play them */
    public static final Preset[] campaign = buildCampaign();

    private static Preset[] buildCampaign(){
        //a planet of grid size 3 has 10*3^2+2 = 92 sectors; the landing sites are spread over all of them
        final int total = 92;
        Rand r = new Rand(90210);
        IntSeq pool = new IntSeq();
        for(int i = 0; i < total; i++) pool.add(i);
        //the start sector is fixed so the planet's startSector is always a landing site
        pool.removeValue(10);
        //shuffle, then take them in stride so the picks are scattered over the sphere instead of clustered
        int[] items = pool.toArray();
        for(int i = items.length - 1; i > 0; i--){
            int j = r.random(i);
            int t = items[i]; items[i] = items[j]; items[j] = t;
        }

        Preset[] out = new Preset[campaignSectors];
        IntSet used = new IntSet();
        ObjectSet<String> shapes = new ObjectSet<>();
        int taken = 0;
        for(int i = 0; i < campaignSectors; i++){
            int id = i == 0 ? 10 : items[taken++];
            used.add(id);
            //difficulty curve: 0 at the landing sector, 1 at the last one
            float t = i / (float)(campaignSectors - 1);

            int size, spawns, winWave;
            float threat, water, ore;
            String key;
            int guard = 0;
            do{
                //size grows with the curve, with a random step so neighbours never look alike
                size = Mathf.round((276 + t * 64f + r.range(10f)) / 4f) * 4;
                size = Mathf.clamp(size, 272, 348);
                spawns = Mathf.clamp(2 + Mathf.round(t * 3f + r.range(0.8f)), 2, 5);
                threat = Mathf.clamp(0.16f + t * 0.74f + r.range(0.05f), 0.12f, 0.95f);
                water = Mathf.clamp(0.5f + r.random(1.4f), 0.4f, 1.9f);
                ore = Mathf.clamp(1.18f - t * 0.22f + r.range(0.08f), 0.88f, 1.25f);
                winWave = 15 + i * 2 + r.random(0, 3);
                key = size + "/" + spawns + "/" + Mathf.round(water * 10f) + "/" + Mathf.round(ore * 100f);
            }while(!shapes.add(key) && ++guard < 40);

            out[i] = new Preset(id, size, spawns, threat, water, ore, winWave);
        }
        return out;
    }

    public static Preset preset(int id){
        for(Preset p : campaign){
            if(p.id == id) return p;
        }
        return null;
    }

    @Override
    public boolean allowLanding(Sector target){
        //only the campaign sectors are landing sites - 32 of them, all generated
        return preset(target.id) != null;
    }

    @Override
    public int getSectorSize(Sector sector){
        Preset p = preset(sector.id);
        return p != null ? p.size : super.getSectorSize(sector);
    }

    float elev(Vec3 p){
        return Simplex.noise3d(seed, 5, 0.56, 1.45f, p.x, p.y, p.z);
    }

    @Override
    public float getHeight(Vec3 position){
        float e = elev(position);
        return 0.52f + Math.max(e, -0.35f) * 0.11f;
    }

    @Override
    public void getColor(Vec3 position, Color out){
        Block floor = pick(position);
        out.set(floor.mapColor);
        out.a(1f);
    }

    /**
     * v7.9: one place decides what ground a point on the planet has, so the globe and the map always agree.
     * Three extra noise fields were added for the new terrain - heat (ash and plasma vents), frost (crust and
     * salt flats) and a coarse gravel field - layered over the v7 water/resonance/moss/ridge bands.
     */
    Block pick(Vec3 position){
        float e = elev(position);
        float resonance = Simplex.noise3d(seed + 33, 4, 0.6, 2.4f, position.x, position.y, position.z);
        float moist = Simplex.noise3d(seed + 71, 3, 0.5, 1.8f, position.x, position.y, position.z);
        float heat = Simplex.noise3d(seed + 17, 3, 0.55, 1.9f, position.x, position.y, position.z);
        float frost = Simplex.noise3d(seed + 91, 3, 0.5, 2.1f, position.x, position.y, position.z);
        float gravel = Simplex.noise3d(seed + 123, 2, 0.5, 2.8f, position.x, position.y, position.z);

        if(e < -0.42f) return AureliaContent.deepAuroraWater;
        if(e < -0.34f) return AureliaContent.auroraWater;
        if(e < -0.29f) return AureliaContent.auroraSandWater;
        if(e < -0.2f) return AureliaContent.auroraSand;
        if(heat > 0.56f) return AureliaTerra.plasmaVent;
        if(heat > 0.36f) return AureliaTerra.emberAsh;
        if(frost > 0.44f) return AureliaTerra.frostCrust;
        if(frost > 0.28f && e > 0.08f) return AureliaTerra.glassFlat;
        if(resonance > 0.5f) return AureliaTerra.resonanceSilt;
        if(resonance > 0.38f) return AureliaContent.resonanceBed;
        if(moist > 0.3f) return AureliaContent.lumenMoss;
        if(e > 0.38f) return AureliaTerra.basaltShelf;
        if(e > 0.3f) return AureliaContent.prismShale;
        if(gravel > 0.5f) return AureliaTerra.auriteGravel;
        return AureliaContent.auroraPlate;
    }

    @Override
    public void genTile(Vec3 position, TileGen tile){
        float e = elev(position);
        Block floor = pick(position);
        tile.floor = floor;
        tile.block = e > 0.46f ? floor.asFloor().wall : Blocks.air;
    }

    private void paint(int cx, int cy, int radius, Floor floor){
        for(int px = -radius; px <= radius; px++){
            for(int py = -radius; py <= radius; py++){
                if(!Mathf.within(px, py, radius)) continue;
                Tile tile = tiles.get(cx + px, cy + py);
                if(tile != null){
                    tile.setFloor(floor);
                    tile.setBlock(Blocks.air);
                    tile.setOverlay(Blocks.air);
                }
            }
        }
    }

    /**
     * v7.6: a lagoon - a shallow aurora-water disc with a small deep middle and a sand beach. Shallow water is
     * walkable, so this never cuts a sector in two; the deep middle only exists so the water reads as a lake.
     */
    private void lagoon(int cx, int cy, int radius){
        paint(cx, cy, radius + 2, AureliaContent.auroraSand);
        paint(cx, cy, radius, AureliaContent.auroraSandWater);
        paint(cx, cy, Math.max(2, (int)(radius * 0.62f)), AureliaContent.auroraWater);
        paint(cx, cy, Math.max(1, (int)(radius * 0.28f)), AureliaContent.deepAuroraWater);
    }

    /** v7.6: a shallow water channel between two lagoons, wide enough for a hull. */
    private void channel(int x0, int y0, int x1, int y1, int radius){
        float dst = Mathf.dst(x0, y0, x1, y1);
        int steps = Math.max(1, (int)(dst * 2f));
        for(int i = 0; i <= steps; i++){
            float f = i / (float)steps;
            int cx = (int)Mathf.lerp(x0, x1, f), cy = (int)Mathf.lerp(y0, y1, f);
            paint(cx, cy, radius + 1, AureliaContent.auroraSand);
            paint(cx, cy, radius, AureliaContent.auroraSandWater);
        }
    }

    /** A round ore patch (overlay only on buildable floor). */
    private void patch(int cx, int cy, float radius, Block ore, float density){
        int r = (int)Math.ceil(radius);
        for(int x = -r; x <= r; x++){
            for(int y = -r; y <= r; y++){
                Tile tile = tiles.get(cx + x, cy + y);
                if(tile != null && Mathf.within(x, y, radius) && !tile.floor().isLiquid && tile.block() == Blocks.air && rand.chance(density)){
                    tile.setOverlay(ore);
                }
            }
        }
    }

    /**
     * v7.7: scatters ore fields across the whole map.
     *
     * <p>Vanilla's {@code ores()} pass ignores {@code oreThreshold} / {@code oreScale} entirely - it hard-codes two
     * noise bands and the ore's index in the list, so the first ore in the list takes almost everything (3257
     * lumenite against 214 aurite on the v7.6 seed). Ground mining is meant to be the backbone of the economy in
     * v7.7, so the thin ores get their own patch pass on top of the noise one.
     */
    private void scatter(Block ore, int count, float rmin, float rmax, float density){
        for(int i = 0; i < count; i++){
            int cx = rand.random(6, width - 7), cy = rand.random(6, height - 7);
            patch(cx, cy, rand.random(rmin, rmax), ore, density);
        }
    }

    /**
     * v7.9 biome pass: drops regional blotches of the new ground over the finished map. Only the floor is
     * touched - overlays, walls and water are left exactly as they are - so this can run after the lakes and
     * roads without undoing any of them. Centres come off a jittered grid, the same way ore is spread, so the
     * variety is spaced over the whole map instead of clumping in one corner.
     */
    private void biomes(int coreX, int coreY){
        Floor[] kinds = {
            AureliaTerra.glassFlat, AureliaTerra.emberAsh, AureliaTerra.frostCrust, AureliaTerra.auriteGravel,
            AureliaTerra.resonanceSilt, AureliaTerra.plasmaVent, AureliaTerra.basaltShelf
        };
        int cells = 4;
        float cw = (width - 24f) / cells, ch = (height - 24f) / cells;
        int k = rand.random(kinds.length - 1);
        for(int gx = 0; gx < cells; gx++){
            for(int gy = 0; gy < cells; gy++){
                Floor floor = kinds[k++ % kinds.length];
                int cx = (int)(12 + cw * (gx + 0.5f) + rand.range(cw * 0.3f));
                int cy = (int)(12 + ch * (gy + 0.5f) + rand.range(ch * 0.3f));
                //never swallow the base plate: the middle of the map stays the plate you land on
                if(Mathf.dst(cx, cy, coreX, coreY) < 34f) continue;
                float radius = rand.random(9f, 17f);
                int ir = (int)radius + 3;
                for(int x = -ir; x <= ir; x++){
                    for(int y = -ir; y <= ir; y++){
                        Tile tile = tiles.get(cx + x, cy + y);
                        if(tile == null) continue;
                        float d = Mathf.dst(x, y);
                        //soft, noisy edge instead of a circle
                        if(d > radius * (0.72f + Simplex.noise2d(seed + 404, 2, 0.5, 1 / 9f, cx + x, cy + y) * 0.5f)) continue;
                        if(tile.floor().isLiquid || tile.overlay() == Blocks.spawn) continue;
                        //the resonance-bed objectives are the map's goals - they are never painted over
                        if(tile.floor() == AureliaContent.resonanceBed) continue;
                        if(Mathf.dst(tile.x, tile.y, coreX, coreY) < 26f) continue;
                        tile.setFloor(floor);
                    }
                }
            }
        }
    }

    /**
     * v7.9 wall ore. Rock faces are now worth mining: a clustered noise field marks seams in the static walls,
     * and only walls that touch open ground are marked, so everything placed can actually be reached by the
     * rift collector's beam or dug out by a borer. Nothing is placed on the walls of the starting plate.
     */
    /** how many tiles currently carry this overlay */
    private int countOverlay(Block ore){
        int n = 0;
        for(Tile tile : tiles){
            if(tile.overlay() == ore) n++;
        }
        return n;
    }

    /** which quadrant around the core currently holds the least ore - the next patch goes there */
    private int weakestQuadrant(int coreX, int coreY){
        int[] q = new int[4];
        for(Tile tile : tiles){
            if(tile.overlay() instanceof OreBlock && !((OreBlock)tile.overlay()).wallOre){
                q[(tile.x < coreX ? 0 : 1) + (tile.y < coreY ? 0 : 2)]++;
            }
        }
        int best = 0;
        for(int i = 1; i < 4; i++){
            if(q[i] < q[best]) best = i;
        }
        return best;
    }

    /**
     * v8.0: the 32 sectors draw their own richness, so a map can come out short on one resource. Rather than
     * rejecting it, top it up - extra patches are dropped in the quadrant that currently has the least ore,
     * well away from the core clearing, until the sector clears the playability floor.
     */
    private void ensureOre(Block ore, int min, int coreX, int coreY){
        int have = countOverlay(ore), guard = 0;
        while(have < min && guard++ < 50){
            int qd = weakestQuadrant(coreX, coreY);
            int x0 = (qd & 1) == 0 ? 8 : coreX + 6, x1 = (qd & 1) == 0 ? coreX - 6 : width - 9;
            int y0 = (qd & 2) == 0 ? 8 : coreY + 6, y1 = (qd & 2) == 0 ? coreY - 6 : height - 9;
            if(x1 <= x0 || y1 <= y0) break;
            int cx = rand.random(x0, x1), cy = rand.random(y0, y1);
            if(Mathf.dst(cx, cy, coreX, coreY) < 34f) continue;
            patch(cx, cy, rand.random(5f, 8.5f), ore, 0.84f);
            have = countOverlay(ore);
        }
    }

    /** same idea for water: a dry map gets one more shallow lagoon until there is something to pump */
    private void ensureWater(int min, int coreX, int coreY){
        int have = 0, guard = 0;
        for(Tile tile : tiles){
            if(tile.floor().liquidDrop != null) have++;
        }
        while(have < min && guard++ < 12){
            int cx = rand.random(16, width - 17), cy = rand.random(16, height - 17);
            if(Mathf.dst(cx, cy, coreX, coreY) < 40f) continue;
            lagoon(cx, cy, rand.random(7, 11));
            have = 0;
            for(Tile tile : tiles){
                if(tile.floor().liquidDrop != null) have++;
            }
        }
    }


    // =================================================================================================
    // v8.1 natural terrain pipeline
    //
    // The v7.8/v8.0 maps were stamped: perfect circles for the core plate, the wave gates and the lakes,
    // straight corridors between them - the "spoon" shapes. This pipeline builds the map the way a real
    // landscape forms instead, then only lightly edits it:
    //
    //   1. elevation  : fractal Brownian motion with two levels of domain warping (Quilez) plus ridged
    //                   noise for mountain chains
    //   2. erosion    : particle ("droplet") hydraulic erosion - carries sediment downhill and deposits it,
    //                   which is what makes valleys and alluvial flats look real
    //   3. rivers     : D8 flow accumulation over the eroded height field; cells above a flow threshold
    //                   become streams and run into the basins
    //   4. climate    : independent warped moisture and heat fields -> biome per tile (Whittaker style)
    //   5. rock       : everything above the rock line becomes wall, then two cellular-automata passes
    //                   (B678/S345678) round the massifs off so no straight or circular edges survive
    //   6. editing    : the core clearing, the wave gates and the paths between them are noise-perturbed
    //                   blobs and wandering trails, never discs and never straight lines
    // =================================================================================================

    /** fractal Brownian motion, 0..1 */
    private float fbm(float x, float y, int octaves, float scale, int off){
        float sum = 0f, amp = 1f, norm = 0f, freq = scale;
        for(int i = 0; i < octaves; i++){
            sum += amp * (float)Simplex.noise2d((int)(seed + off + i * 31), 1, 0.5, freq, x, y);
            norm += amp;
            amp *= 0.5f;
            freq *= 2f;
        }
        return sum / norm;
    }

    /** ridged multifractal - sharp crests, the shape of a mountain chain */
    private float ridged(float x, float y, int octaves, float scale, int off){
        float sum = 0f, amp = 1f, norm = 0f, freq = scale;
        for(int i = 0; i < octaves; i++){
            float n = (float)Simplex.noise2d((int)(seed + off + i * 17), 1, 0.5, freq, x, y);
            n = 1f - Math.abs(n * 2f - 1f);
            sum += amp * n * n;
            norm += amp;
            amp *= 0.5f;
            freq *= 2f;
        }
        return sum / norm;
    }

    /** domain-warped fBm: the warp is what turns "noise blobs" into landforms with flow and direction */
    private float warpedFbm(float x, float y, int octaves, float scale, int off){
        float qx = fbm(x, y, 3, scale * 2f, off + 101) - 0.5f;
        float qy = fbm(x + 53.1f, y + 11.7f, 3, scale * 2f, off + 211) - 0.5f;
        float rx = fbm(x + 42f * qx, y + 42f * qy, 3, scale * 1.4f, off + 307) - 0.5f;
        float ry = fbm(x + 42f * qx + 17.3f, y + 42f * qy + 29.1f, 3, scale * 1.4f, off + 401) - 0.5f;
        return fbm(x + 34f * rx, y + 34f * ry, octaves, scale, off);
    }

    /**
     * Particle hydraulic erosion. Each droplet walks downhill with inertia, picks sediment up on steep
     * ground and drops it on flat ground; thousands of them cut valleys and lay down flats.
     */
    private void erode(float[] hm, int w, int h, int drops){
        float inertia = 0.06f, capacity = 5.2f, deposit = 0.22f, erodeRate = 0.28f, gravity = 3.2f, evaporate = 0.016f;
        for(int d = 0; d < drops; d++){
            float px = rand.random(1f, w - 2f), py = rand.random(1f, h - 2f);
            float dx = 0f, dy = 0f, speed = 1f, water = 1f, sediment = 0f;
            for(int step = 0; step < 36; step++){
                int ix = (int)px, iy = (int)py;
                if(ix < 1 || iy < 1 || ix >= w - 1 || iy >= h - 1) break;
                //gradient of the height field
                float hc = hm[ix + iy * w];
                float gx = hm[(ix + 1) + iy * w] - hm[(ix - 1) + iy * w];
                float gy = hm[ix + (iy + 1) * w] - hm[ix + (iy - 1) * w];
                dx = dx * inertia - gx * (1f - inertia);
                dy = dy * inertia - gy * (1f - inertia);
                float len = Mathf.len(dx, dy);
                if(len < 0.0001f) break;
                dx /= len;
                dy /= len;
                px += dx;
                py += dy;
                int nx = (int)px, ny = (int)py;
                if(nx < 1 || ny < 1 || nx >= w - 1 || ny >= h - 1) break;
                float hn = hm[nx + ny * w];
                float dh = hn - hc;
                float cap = Math.max(-dh, 0.0004f) * speed * water * capacity;
                if(dh > 0f || sediment > cap){
                    //uphill or overloaded: drop sediment here
                    float drop = dh > 0f ? Math.min(dh, sediment) : (sediment - cap) * deposit;
                    hm[ix + iy * w] += drop;
                    sediment -= drop;
                }else{
                    //downhill with room to carry: cut into the ground
                    float take = Math.min((cap - sediment) * erodeRate, -dh);
                    hm[ix + iy * w] -= take;
                    sediment += take;
                }
                speed = (float)Math.sqrt(Math.max(0f, speed * speed + -dh * gravity));
                water *= (1f - evaporate);
                if(water < 0.01f) break;
            }
        }
    }

    /** one box blur pass - thermal weathering, takes the noise edge off the erosion result */
    private void smooth(float[] hm, int w, int h, int passes){
        float[] tmp = new float[hm.length];
        for(int p = 0; p < passes; p++){
            for(int y = 0; y < h; y++){
                for(int x = 0; x < w; x++){
                    float sum = 0f;
                    int n = 0;
                    for(int oy = -1; oy <= 1; oy++){
                        for(int ox = -1; ox <= 1; ox++){
                            int cx = x + ox, cy = y + oy;
                            if(cx < 0 || cy < 0 || cx >= w || cy >= h) continue;
                            sum += hm[cx + cy * w];
                            n++;
                        }
                    }
                    tmp[x + y * w] = sum / n;
                }
            }
            System.arraycopy(tmp, 0, hm, 0, hm.length);
        }
    }

    /**
     * D8 flow accumulation: every cell sends its water to its lowest neighbour, processed from the highest
     * cell down, so the accumulated value is the size of the catchment above each cell. Rivers are the
     * cells whose catchment is large - exactly how real drainage networks are extracted from a DEM.
     */
    private float[] flowAccumulation(float[] hm, int w, int h){
        Integer[] order = new Integer[w * h];
        for(int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Float.compare(hm[b], hm[a]));
        float[] flow = new float[w * h];
        java.util.Arrays.fill(flow, 1f);
        for(int idx : order){
            int x = idx % w, y = idx / w;
            int best = -1;
            float bestH = hm[idx];
            for(int oy = -1; oy <= 1; oy++){
                for(int ox = -1; ox <= 1; ox++){
                    if(ox == 0 && oy == 0) continue;
                    int cx = x + ox, cy = y + oy;
                    if(cx < 0 || cy < 0 || cx >= w || cy >= h) continue;
                    float hv = hm[cx + cy * w];
                    if(hv < bestH){
                        bestH = hv;
                        best = cx + cy * w;
                    }
                }
            }
            if(best >= 0) flow[best] += flow[idx];
        }
        return flow;
    }

    /**
     * A clearing with a noise-perturbed outline - the shape a flat basin has, not a disc. Walls inside are
     * removed; the floor is only repainted where {@code floor} is given.
     */
    private void clearing(int cx, int cy, float radius, @Nullable Floor floor, int off){
        int r = (int)Math.ceil(radius * 1.45f);
        for(int x = -r; x <= r; x++){
            for(int y = -r; y <= r; y++){
                Tile tile = tiles.get(cx + x, cy + y);
                if(tile == null) continue;
                float dst = Mathf.dst(x, y);
                float ang = Mathf.atan2(x, y);
                //the radius wobbles with the angle and with position, so no two clearings look alike
                float wob = 1f
                    + (fbm(Mathf.cos(ang) * 24f + off, Mathf.sin(ang) * 24f, 3, 0.09f, off + 613) - 0.5f) * 0.55f
                    + (fbm(cx + x, cy + y, 2, 0.06f, off + 821) - 0.5f) * 0.35f;
                if(dst > radius * wob) continue;
                tile.setBlock(Blocks.air);
                if(floor != null && dst < radius * wob * 0.92f) tile.setFloor(floor);
            }
        }
    }

    /**
     * A wandering trail between two points: it steps towards the target but is pushed sideways by a noise
     * field, so the path bends the way a game trail or a dry river bed does. Only walls are removed.
     */
    private void trail(int x0, int y0, int x1, int y1, float radius, int off){
        float px = x0, py = y0;
        int guard = 0, max = (int)(Mathf.dst(x0, y0, x1, y1) * 3f) + 64;
        while(Mathf.dst(px, py, x1, y1) > 2f && guard++ < max){
            float toAng = Angles.angle(px, py, x1, y1);
            float bend = (fbm(px, py, 3, 0.035f, off + 977) - 0.5f) * 130f;
            //the closer to the target, the less it is allowed to wander
            float lead = Mathf.clamp(Mathf.dst(px, py, x1, y1) / 60f, 0.3f, 1f);
            float ang = toAng + bend * lead;
            px += Angles.trnsx(ang, 1f);
            py += Angles.trnsy(ang, 1f);
            float rr = radius + (fbm(px, py, 2, 0.08f, off + 1013) - 0.5f) * 1.6f;
            int ir = (int)Math.ceil(rr);
            for(int x = -ir; x <= ir; x++){
                for(int y = -ir; y <= ir; y++){
                    if(!Mathf.within(x, y, rr)) continue;
                    Tile tile = tiles.get((int)px + x, (int)py + y);
                    if(tile != null && tile.block().isStatic()) tile.setBlock(Blocks.air);
                }
            }
        }
    }

    /**
     * An ore vein drawn as a random walk ("Perlin worm") instead of a disc: veins follow the rock the way a
     * real seam does, and two veins of the same ore never look the same.
     */
    private int vein(int cx, int cy, Block ore, int length, float width0, int off){
        float px = cx, py = cy;
        float ang = rand.random(360f);
        int placed = 0;
        for(int i = 0; i < length; i++){
            ang += (fbm(px, py, 2, 0.05f, off + 1301) - 0.5f) * 90f;
            px += Angles.trnsx(ang, 1.1f);
            py += Angles.trnsy(ang, 1.1f);
            float rr = width0 * (0.6f + fbm(px, py, 2, 0.12f, off + 1409));
            int ir = (int)Math.ceil(rr);
            for(int x = -ir; x <= ir; x++){
                for(int y = -ir; y <= ir; y++){
                    if(!Mathf.within(x, y, rr)) continue;
                    Tile tile = tiles.get((int)px + x, (int)py + y);
                    if(tile == null || tile.floor().isLiquid || tile.block() != Blocks.air) continue;
                    if(tile.overlay() != Blocks.air) continue;
                    if(!rand.chance(0.86f)) continue;
                    tile.setOverlay(ore);
                    placed++;
                }
            }
        }
        return placed;
    }

    /** the flattest, driest spot near the middle of the map - where a base would actually be built */
    private int[] baseSite(float[] hm, int w, int h, float sea){
        int bx = w / 2, by = h / 2;
        float best = Float.MAX_VALUE;
        for(int y = h / 2 - 40; y <= h / 2 + 40; y += 3){
            for(int x = w / 2 - 40; x <= w / 2 + 40; x += 3){
                if(x < 30 || y < 30 || x >= w - 30 || y >= h - 30) continue;
                float hc = hm[x + y * w];
                if(hc < sea + 0.035f) continue;
                //roughness = mean absolute difference inside a small window
                float rough = 0f;
                int n = 0;
                for(int oy = -6; oy <= 6; oy += 2){
                    for(int ox = -6; ox <= 6; ox += 2){
                        float hv = hm[(x + ox) + (y + oy) * w];
                        rough += Math.abs(hv - hc);
                        n++;
                    }
                }
                rough /= n;
                float score = rough * 10f + Mathf.dst(x, y, w / 2f, h / 2f) / 400f;
                if(score < best){
                    best = score;
                    bx = x;
                    by = y;
                }
            }
        }
        return new int[]{bx, by};
    }

    /**
     * v7.9 wall ore, v8.0 adaptive: the seam noise decides <b>where</b> the veins run, but a fixed threshold
     * gave wildly different amounts per map (one of the 32 sectors ended up with 15 ore-bearing wall tiles,
     * which is not a resource). The faces are now ranked by their seam value and the top slice is taken, so
     * every sector gets a usable amount of wall ore while the veins still follow the rock.
     */
    private int wallOres(int coreX, int coreY){
        Block[] kinds = {AureliaTerra.wallOreLumenite, AureliaTerra.wallOreAurite, AureliaTerra.wallOreResonance};
        Seq<Tile> faces = new Seq<>();
        FloatSeq vals = new FloatSeq();
        for(Tile tile : tiles){
            Block block = tile.block();
            if(block == Blocks.air || !block.solid || block.synthetic() || tile.overlay() != Blocks.air) continue;
            //must be a face, not the inside of a massif
            boolean exposed = false;
            for(Point2 d : Geometry.d4){
                Tile n = tiles.get(tile.x + d.x, tile.y + d.y);
                if(n != null && !n.solid()){
                    exposed = true;
                    break;
                }
            }
            if(!exposed) continue;
            faces.add(tile);
            vals.add(Simplex.noise2d(seed + 611, 3, 0.5, 1 / 21f, tile.x, tile.y));
        }
        if(faces.isEmpty()) return 0;

        //take the richest slice of the exposed rock: ~7% of the faces, never fewer than 150 tiles
        int want = Math.min(faces.size, Math.max(150, (int)(faces.size * 0.07f)));
        float[] sorted = vals.toArray();
        java.util.Arrays.sort(sorted);
        float cut = sorted[Math.max(0, sorted.length - want)];

        int placed = 0;
        for(int i = 0; i < faces.size; i++){
            if(vals.get(i) < cut) continue;
            Tile tile = faces.get(i);
            float kind = Simplex.noise2d(seed + 733, 2, 0.5, 1 / 34f, tile.x, tile.y);
            tile.setOverlay(kinds[kind > 0.62f ? 2 : kind > 0.42f ? 1 : 0]);
            placed++;
        }
        return placed;
    }

    /**
     * v7.8: even ore distribution. Random scattering piles half the map's ore into whichever corner the random
     * number generator liked; this walks a grid over the map and drops one jittered patch per cell, so every
     * part of the map is worth settling and no quadrant is a desert.
     */
    private void spread(Block ore, int cells, float rmin, float rmax, float density){
        float cw = (width - 16f) / cells, ch = (height - 16f) / cells;
        for(int gx = 0; gx < cells; gx++){
            for(int gy = 0; gy < cells; gy++){
                int cx = (int)(8 + cw * (gx + 0.5f) + rand.range(cw * 0.3f));
                int cy = (int)(8 + ch * (gy + 0.5f) + rand.range(ch * 0.3f));
                patch(Mathf.clamp(cx, 6, width - 7), Mathf.clamp(cy, 6, height - 7), rand.random(rmin, rmax), ore, density);
            }
        }
    }

    /**
     * v7.8: no quadrant may be a desert. Counts the ore in each quarter of the map around the core and keeps
     * dropping patches into the weakest quarter until the poorest one holds at least 80% of the richest, so
     * every direction you expand in is worth the same.
     */
    private void balanceOre(int coreX, int coreY){
        Block[] kinds = {AureliaContent.oreAurite, AureliaContent.oreLumenite, AureliaContent.oreResonance};
        for(int pass = 0; pass < 80; pass++){
            int[] q = new int[4];
            for(Tile tile : tiles){
                if(tile.overlay() instanceof OreBlock) q[(tile.x < coreX ? 0 : 1) + (tile.y < coreY ? 0 : 2)]++;
            }
            int min = 0, max = 0;
            for(int i = 1; i < 4; i++){
                if(q[i] < q[min]) min = i;
                if(q[i] > q[max]) max = i;
            }
            if(q[min] >= q[max] * 0.8f) return;
            int lack = (int)(q[max] * 0.8f) - q[min];
            int patches = Mathf.clamp(lack / 70, 1, 6);
            int x0 = (min % 2 == 0) ? 6 : coreX, x1 = (min % 2 == 0) ? coreX : width - 7;
            int y0 = (min < 2) ? 6 : coreY, y1 = (min < 2) ? coreY : height - 7;
            for(int i = 0; i < patches; i++){
                for(int tries = 0; tries < 20; tries++){
                    int cx = rand.random(x0, Math.max(x0 + 1, x1)), cy = rand.random(y0, Math.max(y0 + 1, y1));
                    Tile tile = tiles.get(cx, cy);
                    if(tile == null || tile.floor().isLiquid || tile.block() != Blocks.air) continue;
                    patch(cx, cy, rand.random(3.4f, 6.4f), kinds[(pass + i) % kinds.length], 0.78f);
                    break;
                }
            }
        }
    }

    /**
     * v7.8: nothing may be walled in. Flood fills the walkable ground from the core; any pocket that is cut off
     * is either joined to the core with a corridor (if it is big enough to matter) or filled in with rock (if it
     * is a tiny hole), so no unit can ever spawn inside a sealed area.
     */
    private int openPockets(int coreX, int coreY){
        boolean[] seen = new boolean[width * height];
        IntQueue queue = new IntQueue();
        int joined = 0;
        //1. the main region, from the core outwards
        queue.addLast(coreX + coreY * width);
        seen[coreX + coreY * width] = true;
        flood(queue, seen, null);
        //2. everything else is a pocket
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                int pos = x + y * width;
                if(seen[pos] || !walkable(tiles.get(x, y))) continue;
                IntSeq region = new IntSeq();
                queue.clear();
                queue.addLast(pos);
                seen[pos] = true;
                flood(queue, seen, region);
                if(region.size >= 40){
                    //big enough to be worth playing in: carve a lane to the core
                    int mid = region.get(region.size / 2);
                    connect(mid % width, mid / width, coreX, coreY, 3);
                    joined++;
                }else{
                    //a hole in the rock: fill it so nothing can be dropped inside. Water is left alone - a
                    //sealed pond is scenery, not a trap, and filling it would eat the map's water.
                    for(int i = 0; i < region.size; i++){
                        Tile t = tiles.get(region.get(i) % width, region.get(i) / width);
                        if(t != null && t.overlay() != Blocks.spawn && !t.floor().isLiquid) t.setBlock(t.floor().wall);
                    }
                }
            }
        }
        return joined;
    }

    private void flood(IntQueue queue, boolean[] seen, @Nullable IntSeq out){
        while(!queue.isEmpty()){
            int pos = queue.removeFirst();
            if(out != null) out.add(pos);
            int cx = pos % width, cy = pos / width;
            for(Point2 d : Geometry.d4){
                int nx = cx + d.x, ny = cy + d.y;
                if(nx < 0 || ny < 0 || nx >= width || ny >= height || seen[nx + ny * width]) continue;
                if(!walkable(tiles.get(nx, ny))) continue;
                seen[nx + ny * width] = true;
                queue.addLast(nx + ny * width);
            }
        }
    }

    /**
     * v7.6: carves a walkable corridor between two points. Walls are removed and deep water - which ground units
     * cannot cross - is turned into shallow sand, so a wave spawned at one end can always walk to the other.
     */
    /**
     * v8.1: a lane between two points. It used to be a straight line with a sine bow, which is where the
     * "ruler road" look came from; it now wanders along a noise field exactly like {@link #trail}, only it
     * also makes deep water walkable so the lane is a lane for ground units too.
     */
    private void connect(int x0, int y0, int x1, int y1, int radius){
        float px = x0, py = y0;
        int guard = 0, max = (int)(Mathf.dst(x0, y0, x1, y1) * 4f) + 96;
        int off = (int)(x0 * 31 + y0 * 17 + seed);
        while(Mathf.dst(px, py, x1, y1) > 2f && guard++ < max){
            float toAng = Angles.angle(px, py, x1, y1);
            float bend = (fbm(px, py, 3, 0.03f, off + 1777) - 0.5f) * 120f;
            float lead = Mathf.clamp(Mathf.dst(px, py, x1, y1) / 40f, 0.25f, 1f);
            float ang = toAng + bend * lead;
            px += Angles.trnsx(ang, 1f);
            py += Angles.trnsy(ang, 1f);
            float rr = radius + (fbm(px, py, 2, 0.07f, off + 1801) - 0.5f) * 1.4f;
            int ir = (int)Math.ceil(rr);
            for(int x = -ir; x <= ir; x++){
                for(int y = -ir; y <= ir; y++){
                    if(!Mathf.within(x, y, rr)) continue;
                    Tile tile = tiles.get((int)px + x, (int)py + y);
                    if(tile == null || tile.block() == AureliaContent.aureliaCore) continue;
                    if(tile.block() != Blocks.air && tile.overlay() != Blocks.spawn) tile.setBlock(Blocks.air);
                    if(tile.floor().isDeep()) tile.setFloor(AureliaContent.auroraSandWater);
                }
            }
        }
    }

    /** v7.6: ground reachability flood fill, used to prove every wave approach can actually reach the core. */
    private boolean walkable(Tile tile){
        return tile != null && !tile.solid() && !tile.floor().isDeep();
    }

    private boolean reaches(int x0, int y0, int x1, int y1){
        boolean[] seen = new boolean[width * height];
        IntQueue queue = new IntQueue();
        queue.addLast(x0 + y0 * width);
        seen[x0 + y0 * width] = true;
        while(!queue.isEmpty()){
            int pos = queue.removeFirst();
            int cx = pos % width, cy = pos / width;
            if(Math.abs(cx - x1) <= 2 && Math.abs(cy - y1) <= 2) return true;
            for(Point2 d : Geometry.d4){
                int nx = cx + d.x, ny = cy + d.y;
                if(nx < 0 || ny < 0 || nx >= width || ny >= height || seen[nx + ny * width]) continue;
                Tile next = tiles.get(nx, ny);
                //the core itself is solid; treat its footprint as the goal rather than a wall
                if(!walkable(next) && !(Math.abs(nx - x1) <= 2 && Math.abs(ny - y1) <= 2)) continue;
                seen[nx + ny * width] = true;
                queue.addLast(nx + ny * width);
            }
        }
        return false;
    }

    private void placeStarterCore(int x, int y){
        Tile tile = tiles.getn(x, y);
        tile.setBlock(AureliaContent.aureliaCore, state.rules.defaultTeam);
        if(!(tile.build instanceof CoreBlock.CoreBuild core)){
            throw new IllegalStateException("Aurelia start failed to create its custom core");
        }
        state.teams.registerCore(core);
        //starting stock: Aurelia materials only
        core.items.add(AureliaContent.lumenite, 400);
        core.items.add(AureliaContent.aurite, 80);
    }

    /** stretch a field to 0..1 */
    private void normalize(float[] a){
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for(float v : a){
            if(v < min) min = v;
            if(v > max) max = v;
        }
        float span = Math.max(1e-5f, max - min);
        for(int i = 0; i < a.length; i++) a[i] = (a[i] - min) / span;
    }

    /** the value below which the given fraction of the field lies (sampled, so it stays cheap) */
    private float percentile(float[] a, float frac){
        int n = Math.min(a.length, 6000);
        float[] s = new float[n];
        int step = Math.max(1, a.length / n);
        for(int i = 0; i < n; i++) s[i] = a[Math.min(a.length - 1, i * step)];
        java.util.Arrays.sort(s);
        return s[Mathf.clamp((int)(frac * (n - 1)), 0, n - 1)];
    }

    /** the rock that belongs on top of a given ground - each biome keeps its own massif colour */
    private Block wallFor(Floor fl){
        if(fl == AureliaTerra.emberAsh || fl == AureliaTerra.plasmaVent) return AureliaTerra.emberWall;
        if(fl == AureliaTerra.basaltShelf || fl == AureliaTerra.auriteGravel) return AureliaTerra.basaltWall;
        if(fl == AureliaTerra.glassFlat || fl == AureliaTerra.frostCrust) return AureliaTerra.glassRidgeWall;
        if(fl == AureliaTerra.resonanceSilt || fl == AureliaContent.resonanceBed) return AureliaContent.resonanceWall;
        if(fl == AureliaContent.auroraSand) return AureliaContent.auroraDuneWall;
        if(fl == AureliaContent.lumenMoss) return AureliaContent.prismShaleWall;
        return AureliaContent.auroraWall;
    }

    /** true if a silt dune and open water already exist within {@code r} of the core site */
    private boolean nearFloorKind(int cx, int cy, int r, boolean needBoth){
        boolean sandy = false, wet = false;
        for(int x = -r; x <= r; x += 2){
            for(int y = -r; y <= r; y += 2){
                Tile tile = tiles.get(cx + x, cy + y);
                if(tile == null) continue;
                if(tile.floor().itemDrop == AureliaContent.silt) sandy = true;
                if(tile.floor().liquidDrop != null) wet = true;
            }
        }
        return needBoth ? (sandy && wet) : (sandy || wet);
    }

    /**
     * The climate pass produces every kind of ground on a normal seed, but an extreme seed can miss one
     * (an all-cold map has no ember ash). Any missing kind gets one irregular regional patch, placed on
     * ground that suits it, so the variety floor is met without the map looking stamped.
     */
    private void guaranteeFloors(int off){
        Block[] kinds = {AureliaTerra.glassFlat, AureliaTerra.emberAsh, AureliaTerra.frostCrust,
            AureliaTerra.auriteGravel, AureliaTerra.resonanceSilt, AureliaTerra.plasmaVent, AureliaTerra.basaltShelf};
        ObjectSet<Block> have = new ObjectSet<>();
        for(Tile tile : tiles){
            for(Block k : kinds){
                if(tile.floor() == k) have.add(k);
            }
        }
        int i = 0;
        for(Block k : kinds){
            if(have.contains(k)) continue;
            for(int tries = 0; tries < 60; tries++){
                int cx = rand.random(18, width - 19), cy = rand.random(18, height - 19);
                Tile tile = tiles.getn(cx, cy);
                if(tile.floor().isLiquid) continue;
                float r = k == AureliaTerra.plasmaVent ? 3.5f : 9f;
                clearing(cx, cy, r, (Floor)k, off + 1500 + (i++) * 53);
                break;
            }
        }
    }

    @Override
    protected void generate(){
        Preset pre = preset(sector.id);
        if(pre == null) pre = campaign[0];
        float waterLevel = pre.water, oreScale = pre.ore;
        int w = width, h = height, off = sector.id * 7919 + seed;

        // ---------------------------------------------------------------------------------------------
        // 1. elevation: domain-warped fBm continents with a ridged mountain system laid over them
        // ---------------------------------------------------------------------------------------------
        float[] hm = new float[w * h];
        //absolute frequencies: one unit of noise is ~130 tiles, so a map carries five or six real landforms
        //instead of a field of speckles. Everything below is in tiles.
        float scl = 0.0072f;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                float base = warpedFbm(x, y, 4, scl, off);
                float ridge = ridged(x, y, 3, scl * 1.7f, off + 55);
                //the ridge mask keeps the mountains in chains instead of sprinkling them everywhere
                float mask = Mathf.clamp((warpedFbm(x + 811f, y + 277f, 2, scl * 0.7f, off + 91) - 0.40f) * 2.8f);
                hm[x + y * w] = base * 0.78f + ridge * mask * 0.46f;
            }
        }
        normalize(hm);
        //2. hydraulic erosion: droplets with inertia cut the valleys and lay the flats
        erode(hm, w, h, (int)(w * h * 0.22f));
        smooth(hm, w, h, 2);
        normalize(hm);

        //3. drainage network from the eroded height field
        float[] flow = flowAccumulation(hm, w, h);
        float sea = Mathf.clamp(0.20f + (waterLevel - 1f) * 0.055f, 0.12f, 0.34f);
        float rockLine = percentile(hm, 1f - 0.26f);
        float riverLine = percentile(flow, 1f - 0.045f);

        // ---------------------------------------------------------------------------------------------
        // 4. rock mask + two cellular-automata smoothing passes. CA is what removes every straight and
        //    circular edge: a cell becomes rock if most of its neighbourhood is rock, which produces the
        //    rounded, branching massifs you see in a cave-generated world.
        // ---------------------------------------------------------------------------------------------
        boolean[] rock = new boolean[w * h];
        for(int i = 0; i < rock.length; i++) rock[i] = hm[i] > rockLine;
        boolean[] next = new boolean[w * h];
        for(int pass = 0; pass < 3; pass++){
            for(int y = 0; y < h; y++){
                for(int x = 0; x < w; x++){
                    int n = 0;
                    for(int oy = -1; oy <= 1; oy++){
                        for(int ox = -1; ox <= 1; ox++){
                            int cx = x + ox, cy = y + oy;
                            if(cx < 0 || cy < 0 || cx >= w || cy >= h){
                                n++;
                            }else if(rock[cx + cy * w]){
                                n++;
                            }
                        }
                    }
                    next[x + y * w] = n >= 5;
                }
            }
            System.arraycopy(next, 0, rock, 0, rock.length);
        }

        // ---------------------------------------------------------------------------------------------
        // 5. climate -> biome. Moisture and heat are their own warped noise fields, so the ground changes
        //    in regions the way a climate map does, not in random blotches.
        // ---------------------------------------------------------------------------------------------
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                int i = x + y * w;
                Tile tile = tiles.getn(x, y);
                float e = hm[i];
                tile.setOverlay(Blocks.air);
                if(e < sea){
                    //depth bands: only the middle of a basin is deep, the rim stays walkable
                    Floor fl = e < sea - 0.085f ? AureliaContent.deepAuroraWater :
                        e < sea - 0.032f ? AureliaContent.auroraWater : AureliaContent.auroraSandWater;
                    tile.setFloor(fl);
                    tile.setBlock(Blocks.air);
                    continue;
                }
                float m = warpedFbm(x + 1500f, y + 400f, 3, scl * 0.75f, off + 1700);
                float t = warpedFbm(x - 900f, y + 1900f, 3, scl * 0.62f, off + 2300) * 0.75f + (1f - e) * 0.25f;
                //wetness rises in the valleys and along the streams - water collects low down
                m = Mathf.clamp(m + (1f - Mathf.clamp((e - sea) * 2.6f)) * 0.22f + Mathf.clamp(flow[i] / (riverLine * 4f)) * 0.18f);

                Floor fl;
                if(e < sea + 0.022f){
                    fl = AureliaContent.auroraSand;                                   //beach
                }else if(flow[i] > riverLine && e < rockLine){
                    fl = AureliaContent.auroraSandWater;                              //stream bed
                }else if(t > 0.70f && m < 0.40f){
                    fl = rand.chance(0.035f) ? AureliaTerra.plasmaVent : AureliaTerra.emberAsh;
                }else if(t < 0.33f && m < 0.52f){
                    fl = AureliaTerra.frostCrust;
                }else if(m > 0.66f){
                    fl = AureliaTerra.resonanceSilt;
                }else if(e > rockLine - 0.09f){
                    fl = t > 0.5f ? AureliaTerra.basaltShelf : AureliaTerra.auriteGravel;
                }else if(m < 0.38f){
                    fl = AureliaTerra.glassFlat;
                }else if(m > 0.56f){
                    fl = AureliaContent.lumenMoss;
                }else{
                    fl = AureliaContent.auroraPlate;
                }
                tile.setFloor(fl);
                tile.setBlock(rock[i] ? wallFor(fl) : Blocks.air);
            }
        }
        //the streams must stay wet even where the climate pass painted over them
        for(int i = 0; i < w * h; i++){
            if(flow[i] > riverLine && hm[i] >= sea && !rock[i]){
                Tile tile = tiles.getn(i % w, i / w);
                tile.setBlock(Blocks.air);
                if(!tile.floor().isLiquid) tile.setFloor(AureliaContent.auroraSandWater);
            }
        }
        guaranteeFloors(off);

        // ---------------------------------------------------------------------------------------------
        // 6. editing pass: base, wave gates and trails. Every shape here is noise-perturbed.
        // ---------------------------------------------------------------------------------------------
        int[] site = baseSite(hm, w, h, sea);
        int spawnX = site[0], spawnY = site[1];
        clearing(spawnX, spawnY, 21f, null, off + 10);
        clearing(spawnX, spawnY, 16f, AureliaContent.auroraPlate, off + 20);

        int spawns = pre.spawns;
        float ringR = Math.min(w, h) * 0.37f;
        float base0 = rand.random(360f);
        int[][] approach = new int[spawns][2];
        for(int i = 0; i < spawns; i++){
            float ang = base0 + i * (360f / spawns) + rand.range(9f);
            float rr = ringR * rand.random(0.93f, 1.07f);
            //a gate never opens in deep water, and never closer than 30 % of the map to the core: the search
            //runs along the lane's own bearing and takes the first dry tile inside the fair band, so the
            //approaches vary with the landscape without any of them landing on the player's doorstep
            float minD = Math.min(w, h) * 0.30f, maxD = Math.min(w, h) * 0.44f;
            int ex = Mathf.clamp((int)(spawnX + Angles.trnsx(ang, rr)), 14, w - 15);
            int ey = Mathf.clamp((int)(spawnY + Angles.trnsy(ang, rr)), 14, h - 15);
            for(int t = 0; t <= 40; t++){
                //alternate outwards / inwards around the preferred radius
                float d = rr + ((t % 2 == 0) ? 1 : -1) * ((t + 1) / 2) * 2.5f;
                if(d < minD || d > maxD) continue;
                int tx = Mathf.clamp((int)(spawnX + Angles.trnsx(ang, d)), 14, w - 15);
                int ty = Mathf.clamp((int)(spawnY + Angles.trnsy(ang, d)), 14, h - 15);
                if(hm[tx + ty * w] >= sea + 0.01f){
                    ex = tx;
                    ey = ty;
                    break;
                }
            }
            //last resort: keep it inside the band even if the band is wet, the clearing dries it out anyway
            float dcur = Mathf.dst(ex, ey, spawnX, spawnY);
            if(dcur < minD || dcur > maxD){
                float d = Mathf.clamp(dcur, minD, maxD);
                ex = Mathf.clamp((int)(spawnX + Angles.trnsx(ang, d)), 14, w - 15);
                ey = Mathf.clamp((int)(spawnY + Angles.trnsy(ang, d)), 14, h - 15);
            }
            clearing(ex, ey, 8f, AureliaContent.auroraPlate, off + 100 + i * 7);
            for(int x = -3; x <= 3; x++){
                for(int y = -3; y <= 3; y++){
                    Tile g = tiles.get(ex + x, ey + y);
                    if(g != null && Mathf.within(x, y, 3f)) g.setFloor(AureliaContent.auroraPlate);
                }
            }
            tiles.getn(ex, ey).setOverlay(Blocks.spawn);
            approach[i][0] = ex;
            approach[i][1] = ey;
        }
        for(int i = 0; i < spawns; i++){
            trail(approach[i][0], approach[i][1], spawnX, spawnY, 2.6f, off + 300 + i * 13);
            int[] b = approach[(i + 1) % spawns];
            trail(approach[i][0], approach[i][1], b[0], b[1], 2.1f, off + 500 + i * 19);
        }

        // ---------------------------------------------------------------------------------------------
        // 7. resources. Ore is laid as wandering seams, never as discs, and the loop simply keeps adding
        //    seams until the map carries enough of each ore - a thin seed can no longer produce a poor map.
        // ---------------------------------------------------------------------------------------------
        //objectives: resonance beds out on their own ring, each one an irregular shelf
        float objR = Math.min(w, h) * 0.26f;
        int objOffset = rand.random(360);
        for(int i = 0; i < 4; i++){
            float ang = objOffset + i * 90f + rand.range(14f);
            int cx = Mathf.clamp((int)(spawnX + Angles.trnsx(ang, objR)), 20, w - 21);
            int cy = Mathf.clamp((int)(spawnY + Angles.trnsy(ang, objR)), 20, h - 21);
            clearing(cx, cy, 11f, AureliaContent.resonanceBed, off + 700 + i * 23);
            vein(cx, cy, AureliaContent.oreResonance, 46, 1.9f, off + 740 + i * 29);
            vein(cx, cy, AureliaContent.oreLumenite, 40, 2.0f, off + 760 + i * 31);
        }

        //starter resources: within one drill-run of the core, so the first minute is never a search
        float a0 = rand.random(360f);
        for(int i = 0; i < 4; i++){
            float ang = a0 + i * 90f + rand.range(18f);
            int sx = Mathf.clamp((int)(spawnX + Angles.trnsx(ang, rand.random(15f, 23f))), 8, w - 9);
            int sy = Mathf.clamp((int)(spawnY + Angles.trnsy(ang, rand.random(15f, 23f))), 8, h - 9);
            vein(sx, sy, i % 2 == 0 ? AureliaContent.oreLumenite : AureliaContent.oreAurite, 30, 2.1f, off + 900 + i * 37);
        }

        int veinA = 0, veinL = 0, veinR = 0, guard = 0;
        int wantA = (int)(1150 * oreScale), wantL = (int)(520 * oreScale), wantR = (int)(380 * oreScale);
        while((veinA < wantA || veinL < wantL || veinR < wantR) && guard++ < 220){
            int cx = rand.random(10, w - 11), cy = rand.random(10, h - 11);
            if(hm[cx + cy * w] < sea) continue;
            if(veinA < wantA) veinA += vein(cx, cy, AureliaContent.oreAurite, rand.random(34, 62), rand.random(1.7f, 2.6f), off + guard * 41);
            else if(veinL < wantL) veinL += vein(cx, cy, AureliaContent.oreLumenite, rand.random(28, 54), rand.random(1.6f, 2.4f), off + guard * 43);
            else veinR += vein(cx, cy, AureliaContent.oreResonance, rand.random(24, 46), rand.random(1.5f, 2.2f), off + guard * 47);
        }

        //a silt dune and a tidewater pond near home, if the climate did not already put them there
        if(!nearFloorKind(spawnX, spawnY, 44, true)){
            float ang = a0 + 135f;
            int px = Mathf.clamp((int)(spawnX + Angles.trnsx(ang, 30f)), 12, w - 13);
            int py = Mathf.clamp((int)(spawnY + Angles.trnsy(ang, 30f)), 12, h - 13);
            clearing(px, py, 8f, AureliaContent.auroraSand, off + 1100);
            clearing(px, py, 5f, AureliaContent.auroraSandWater, off + 1110);
            clearing(px, py, 2.4f, AureliaContent.auroraWater, off + 1120);
        }

        //no ore on liquid, no ore under the core plate edge
        for(Tile tile : tiles){
            if(tile.floor().isLiquid && tile.overlay() instanceof OreBlock ob && !ob.wallOre) tile.setOverlay(Blocks.air);
        }
        balanceOre(spawnX, spawnY);
        decoration(0.008f);

        //the landing pad itself: flat, dry, free of ore, and the core goes in the middle of it
        for(int x = -6; x <= 6; x++){
            for(int y = -6; y <= 6; y++){
                Tile tile = tiles.get(spawnX + x, spawnY + y);
                if(tile != null && Mathf.within(x, y, 6f)){
                    tile.setBlock(Blocks.air);
                    tile.setOverlay(Blocks.air);
                    if(tile.floor().isLiquid) tile.setFloor(AureliaContent.auroraPlate);
                }
            }
        }
        placeStarterCore(spawnX, spawnY);

        //v7.8: the wave gates are stamped last. Carving a road or dropping an ore patch on top of a gate tile
        //used to wipe the overlay, and a map with a missing gate spawns its waves on the core.
        for(int[] ap : approach){
            Tile gate = tiles.getn(ap[0], ap[1]);
            gate.setBlock(Blocks.air);
            gate.setOverlay(Blocks.spawn);
        }

        //and nothing is left sealed off
        int joined = openPockets(spawnX, spawnY);

        //v7.9: ore inside the rock, last of all, once every wall on the map is final
        int wallOre = wallOres(spawnX, spawnY);

        float threat = pre.threat;
        state.rules.waves = true;
        state.rules.winWave = pre.winWave;
        state.rules.waveSpacing = 60f * (sector.id == AureliaContent.aurelia.startSector ? 90f : 75f - threat * 15f);
        state.rules.initialWaveSpacing = 60f * 60f * (sector.id == AureliaContent.aurelia.startSector ? 3f : 2.5f);
        state.rules.env = sector.planet.defaultEnv;
        state.rules.spawns = AureliaWaves.generate(threat, new arc.math.Rand(sector.id + seed));

        //v8.0: guarantee the playability floor before anything is counted - a randomised sector that came out
        //poor in one resource is topped up instead of being thrown away
        ensureOre(AureliaContent.oreAurite, 760, spawnX, spawnY);
        ensureOre(AureliaContent.oreLumenite, 220, spawnX, spawnY);
        ensureOre(AureliaContent.oreResonance, 200, spawnX, spawnY);
        ensureWater(420, spawnX, spawnY);

        int beds = 0, lumenOre = 0, aurite = 0, spawnPoints = 0, waterTiles = 0, sandTiles = 0, resonanceOre = 0;
        //v7.9: how much of the new ground actually made it onto the map, and how many kinds of it
        ObjectSet<Block> newFloors = new ObjectSet<>();
        int newGround = 0;
        int[] quadrant = new int[4];
        for(Tile tile : tiles){
            if(tile.floor() == AureliaContent.resonanceBed) beds++;
            if(tile.overlay() == AureliaContent.oreLumenite) lumenOre++;
            if(tile.overlay() == AureliaContent.oreAurite) aurite++;
            if(tile.overlay() == AureliaContent.oreResonance) resonanceOre++;
            if(tile.overlay() == Blocks.spawn) spawnPoints++;
            if(tile.floor().liquidDrop != null) waterTiles++;
            for(Block nf : AureliaTerra.floors()){
                if(tile.floor() == nf){
                    newFloors.add(nf);
                    newGround++;
                    break;
                }
            }
            if(tile.floor().itemDrop == AureliaContent.silt) sandTiles++;
            if(tile.overlay() instanceof OreBlock){
                quadrant[(tile.x < spawnX ? 0 : 1) + (tile.y < spawnY ? 0 : 2)]++;
            }
        }
        for(int[] ap : approach){
            if(!reaches(ap[0], ap[1], spawnX, spawnY)){
                connect(ap[0], ap[1], spawnX, spawnY, 4);
                if(!reaches(ap[0], ap[1], spawnX, spawnY)){
                    throw new IllegalStateException("Aurelia wave approach " + ap[0] + "," + ap[1] + " cannot reach the core");
                }
            }
        }
        int minQuad = Math.min(Math.min(quadrant[0], quadrant[1]), Math.min(quadrant[2], quadrant[3]));
        int maxQuad = Math.max(Math.max(quadrant[0], quadrant[1]), Math.max(quadrant[2], quadrant[3]));
        if(tiles.getn(spawnX, spawnY).block() != AureliaContent.aureliaCore || beds < 150 || lumenOre < 60 || aurite < 600 || resonanceOre < 120
            || spawnPoints != spawns || waterTiles < 300 || sandTiles < 20 || minQuad < maxQuad * 0.45f
            || newFloors.size < 6 || wallOre < 20){
            throw new IllegalStateException("Aurelia generated an invalid campaign sector: core=" +
                tiles.getn(spawnX, spawnY).block().name + ", beds=" + beds + ", lumenite=" + lumenOre + ", aurite=" + aurite +
                ", resonance=" + resonanceOre + ", spawns=" + spawnPoints + "/" + spawns + ", water=" + waterTiles + ", sand=" + sandTiles +
                ", quadrants=" + quadrant[0] + "/" + quadrant[1] + "/" + quadrant[2] + "/" + quadrant[3] +
                ", new floors=" + newFloors.size + ", wall ore=" + wallOre);
        }
        Log.info("[blackhole/aurelia] campaign sector @ (@x@) generated: @ spawns in the fair band, about @ tiles out, @ aurite, @ lumenite, @ resonance, @ bed, @ water, @ silt, ore per quadrant @/@/@/@, @ pockets joined, @ new-ground tiles in @ kinds, @ wall ore, all connected.",
            sector.id, width, height, spawns, (int)ringR, aurite, lumenOre, resonanceOre, beds, waterTiles, sandTiles,
            quadrant[0], quadrant[1], quadrant[2], quadrant[3], joined, newGround, newFloors.size, wallOre);
    }
}
