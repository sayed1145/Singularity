package blackhole;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import blackhole.g3d.*;
import blackhole.models.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/**
 * The campaign core, drawn as a 3D pearl landing platform with a floating lumen heart.
 *
 * <p>v7.1: the landing cut-scene spins {@code teamRegions[team]}; without a {@code -team} sprite that was the
 * "oh no" error region. The baker now writes {@code aurelia-core-team}, and {@link #load()} additionally replaces
 * any missing team region with an empty one. Every effect vanilla hardcodes in CoreBuild (landing dust,
 * incineration, destruction) is replaced by the mod's own; only the player respawn flash (a static remote
 * method) remains vanilla.
 */
public class AureliaCoreBlock extends CoreBlock{
    public BlockModel model;

    public AureliaCoreBlock(String name){
        super(name);
        buildType = AureliaCoreBuild::new;
        launchEffect = OwnFx.spawn;
    }

    @Override
    public void load(){
        super.load();
        model = Models.get(name);
        if(model != null) model.load();
        if(teamRegion == null || !teamRegion.found()){
            TextureRegion clear = Core.atlas.find("clear");
            teamRegion = clear;
            if(teamRegions != null) for(int i = 0; i < teamRegions.length; i++) teamRegions[i] = clear;
        }else if(teamRegions != null){
            for(int i = 0; i < teamRegions.length; i++){
                if(teamRegions[i] == null || !teamRegions[i].found()) teamRegions[i] = teamRegion;
            }
        }
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find(name + "-preview", region)};
    }

    @Override
    public void placeBegan(Tile tile, Block previous, Unit builder){
        super.placeBegan(tile, previous, builder);
        if(previous instanceof CoreBlock) OwnFx.spawn.at(tile.drawx(), tile.drawy(), 0f, AureliaFx.lumen);
    }

    public class AureliaCoreBuild extends CoreBuild{
        @Override
        public void draw(){
            //landing / launch sequences keep the vanilla thruster animation (drawn with the baked base sprite)
            if(thrusterTime > 0 || model == null){
                super.draw();
                return;
            }
            model.draw(this);
            drawTeamTop();
        }

        @Override
        public void drawLight(){
            super.drawLight();
            Drawf.light(x, y, 60f, AureliaFx.lumen, 0.45f);
        }

        @Override
        public void beginLaunch(boolean launching){
            if(!launching){
                //landing: vanilla shows no construct flash, only the fade-in and the (overridden) launchEffect
                super.beginLaunch(false);
                return;
            }
            //launching: same as vanilla but with the mod's own construct flash
            cloudSeed = Mathf.random(1f);
            OwnFx.spawn.at(x, y, 0f, AureliaFx.lumen);
            if(!headless){
                launchSound.at(Core.camera.position, 1f, launchSoundVolume);
                if(renderer.isLaunching()){
                    float margin = 30f;
                    arc.scene.ui.Image image = new arc.scene.ui.Image();
                    image.color.a = 0f;
                    image.touchable = arc.scene.event.Touchable.disabled;
                    image.setFillParent(true);
                    image.actions(arc.scene.actions.Actions.delay((launchDuration() - margin) / 60f),
                        arc.scene.actions.Actions.fadeIn(margin / 60f, arc.math.Interp.pow2In),
                        arc.scene.actions.Actions.delay(6f / 60f), arc.scene.actions.Actions.remove());
                    image.update(() -> {
                        image.toFront();
                        ui.loadfrag.toFront();
                        if(state.isMenu()) image.remove();
                    });
                    Core.scene.add(image);
                }
            }
        }

        @Override
        public void updateLaunch(){
            float in = renderer.getLandTimeIn() * launchDuration();
            float tsize = Mathf.sample(thrusterSizes, (in + 35f) / launchDuration());
            landParticleTimer += tsize * arc.util.Time.delta;
            if(landParticleTimer >= 1f){
                tile.getLinkedTiles(t -> {
                    if(Mathf.chance(0.4f)){
                        OwnFx.dust.at(t.worldx(), t.worldy(), angleTo(t.worldx(), t.worldy()) + Mathf.range(30f),
                            arc.util.Tmp.c1.set(t.floor().mapColor).mul(1.5f + Mathf.range(0.15f)));
                    }
                });
                landParticleTimer = 0f;
            }
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source){
            boolean incinerate = incinerateNonBuildable && !item.buildable;
            int realAmount = incinerate ? 0 : Math.min(amount, storageCapacity - items.get(item));
            noSleep();
            items.add(item, realAmount);
            if(team == state.rules.defaultTeam && state.isCampaign()){
                if(!incinerate) state.rules.sector.info.handleCoreItem(item, amount);
                if(realAmount == 0 && wasVisible) OwnFx.burn.at(x, y);
            }
        }

        @Override
        public void handleItem(Building source, Item item){
            boolean incinerate = incinerateNonBuildable && !item.buildable;
            if(team == state.rules.defaultTeam) state.stats.coreItemCount.increment(item);
            if(net.server() || !net.active()){
                if(team == state.rules.defaultTeam && state.isCampaign() && !incinerate){
                    state.rules.sector.info.handleCoreItem(item, 1);
                }
                if(items.get(item) >= storageCapacity || incinerate){
                    if(!noEffect) burnAt(source);
                    noEffect = false;
                }else{
                    items.add(item, 1);
                }
            }else if(((state.rules.coreIncinerates && items.get(item) >= storageCapacity) || incinerate) && !noEffect){
                burnAt(source);
                noEffect = false;
            }
        }

        void burnAt(Building source){
            if(source == null || !Mathf.chance(0.3)) return;
            Tile edge = Edges.getFacingEdge(source, this), edge2 = Edges.getFacingEdge(this, source);
            if(edge != null && edge2 != null && wasVisible){
                OwnFx.burn.at((edge.worldx() + edge2.worldx()) / 2f, (edge.worldy() + edge2.worldy()) / 2f);
            }
        }

        @Override
        public void onDestroyed(){
            //Building.onDestroyed with the mod's explosion effect (cores store no explosive goods in Aurelia)
            float power = 0f;
            if(block.consPower != null && block.consPower.buffered) power += this.power.status * block.consPower.capacity;
            Damage.dynamicExplosion(x, y, 0f, 0f, power, tilesize * block.size / 2f, state.rules.damageExplosions, OwnFx.explodeBig, block.baseShake);
            if(!headless) playDestroySound();
            OwnFx.explodeBig.at(x, y, 0f, team.color);
            OwnFx.shield.at(x, y, 40f + block.size * tilesize * 0.5f, team.color);

            if(state.isCampaign() && team == state.rules.waveTeam && team.cores().size <= 1 && spawner.getSpawns().size == 0 && state.rules.sector.planet.enemyCoreSpawnReplace){
                tile.setOverlayQuiet(Blocks.spawn);
                if(!spawner.getSpawns().contains(tile)) spawner.getSpawns().add(tile);
            }
            Events.fire(new CoreChangeEvent(this));
        }
    }
}
