package rbmk.world;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;
import java.util.*;

/** Dual-mode RBMK-inspired gameplay controller: guided load-following plus 16-bank/8-pump expert operation. */
public class RbmkReactor extends PowerGenerator{
    public static final int banks=16,pumps=8;
    public Item fuelItem; public Liquid coolant;
    public float fuelDuration=1080f,nominalCoolant=.055f;
    public int fuelTimer=timers++;

    public RbmkReactor(String name){
        super(name);
        hasItems=true;hasLiquids=true;hasPower=true;outputsPower=true;configurable=true;saveConfig=true;update=true;solid=true;
        emitLight=true;rebuildable=false;flags=arc.struct.EnumSet.of(BlockFlag.reactor,BlockFlag.generator);
        config(int[].class,(RbmkBuild b,int[] v)->b.applyConfig(v));
    }

    private static final Color lightColor=Color.valueOf("cdefff");
    @Override public void load(){super.load();rbmk.gfx.ReactorModel.instance.load();}
    @Override public arc.graphics.g2d.TextureRegion[] icons(){return new arc.graphics.g2d.TextureRegion[]{Core.atlas.find(name+"-preview",region)};}
    @Override public void setStats(){stats.timePeriod=fuelDuration;super.setStats();}
    @Override public void setBars(){
        super.setBars();
        addBar("rbmk-output",(RbmkBuild b)->new Bar(()->Core.bundle.format("bar.rbmk-output",Strings.fixed(b.productionEfficiency*powerProduction*60f,0)),()->b.productionEfficiency>1.65f?Color.valueOf("ffbd59"):Pal.powerBar,()->Mathf.clamp(b.productionEfficiency/2f)));
        addBar("reactor-power",(RbmkBuild b)->new Bar(()->Core.bundle.format("bar.rbmk-power",Strings.fixed(b.powerLevel*100f,0)),()->b.powerLevel>2.1f?Color.scarlet:Pal.powerBar,()->Mathf.clamp(b.powerLevel/2.2f)));
        addBar("reactor-temperature",(RbmkBuild b)->new Bar(()->Core.bundle.format("bar.rbmk-temperature",Strings.fixed(b.temperature*100f,0)),()->Color.valueOf("ff8a5b"),()->Mathf.clamp(b.temperature/1.15f)));
        addBar("reactor-pressure",(RbmkBuild b)->new Bar(()->Core.bundle.format("bar.rbmk-pressure",Strings.fixed(b.pressure*100f,0)),()->Color.valueOf("ffd37f"),()->Mathf.clamp(b.pressure/1.15f)));
        addBar("reactor-orm",(RbmkBuild b)->new Bar(()->Core.bundle.format("bar.rbmk-orm",Strings.fixed(b.orm,1)),()->b.orm<30f?Color.scarlet:Color.valueOf("8de3a7"),()->Mathf.clamp(b.orm/60f)));
        addBar("reactor-drum",(RbmkBuild b)->new Bar(()->Core.bundle.format("bar.rbmk-drum",Strings.fixed(b.drumLevel*100f,0)),()->(b.drumLevel<.2f||b.drumLevel>.85f)?Color.scarlet:Color.valueOf("8fdcf4"),()->Mathf.clamp(b.drumLevel)));
        addBar("reactor-water",(RbmkBuild b)->new Bar(()->Core.bundle.get("bar.rbmk-water"),()->Color.valueOf("b8ecff"),()->b.liquids.get(coolant)/liquidCapacity));
    }

    public class RbmkBuild extends GeneratorBuild{
        // v1-compatible fields first.
        public int rodSetpoint=48,coolantFlow=82;
        public float rodPosition=30f,powerLevel=.08f,temperature=.18f,pressure=.12f,voidFraction=.04f,xenon=.05f;
        public float tipPulse,dangerTime,operationTime;
        public boolean automatic=true,scrammed=false;
        // v2 operator station.
        public boolean expert=false;
        public int loadDemand=100,turbineValve=100,feedwater=90;
        public float drumLevel=.55f,leftLoopFlow=.88f,rightLoopFlow=.88f;
        public final int[] bankSet=new int[banks],pumpSet=new int[pumps];
        public final float[] bankActual=new float[banks];
        /** All 211 CPS rods remain individually addressable; the 16 visible drives are mobile-friendly group averages. */
        public final int[] rodSet=new int[211];
        public final float[] rodActual=new float[211];
        public int selectedRod,selectedVisualChannel;
        public float orm=48f,spatialPeak=1f;
        /** visual-only rotor phases (not saved) */
        public final float[] pumpPhase=new float[pumps];public float turbinePhase;

        public RbmkBuild(){Arrays.fill(bankSet,48);Arrays.fill(bankActual,30f);Arrays.fill(pumpSet,88);Arrays.fill(rodSet,48);Arrays.fill(rodActual,30f);}

        public void applyConfig(int[] v){
            if(v==null)return;
            if(v.length==3&&v[0]==211){int id=Mathf.clamp(v[1],0,210);rodSet[id]=Mathf.clamp(v[2],0,100);return;}
            if(v.length==3&&v[0]==212){int group=Mathf.clamp(v[1],0,banks-1),value=Mathf.clamp(v[2],0,100);setGroup(group,value);return;}
            if(v.length==3&&v[0]==213){setCpsGroup(Mathf.clamp(v[1],0,3),Mathf.clamp(v[2],0,100));return;}
            if(v.length<7)return;
            expert=v[1]!=0;automatic=v[2]!=0;
            if(v[3]!=0){scram();return;}
            rodSetpoint=Mathf.clamp(v[4],0,100);loadDemand=Mathf.clamp(v[5],25,200);turbineValve=Mathf.clamp(v[6],20,100);
            if(v.length>7)feedwater=Mathf.clamp(v[7],20,100);
            int o=8;for(int i=0;i<pumps&&o+i<v.length;i++)pumpSet[i]=Mathf.clamp(v[o+i],0,100);
        }
        private void setGroup(int group,int value){
            bankSet[group]=value;for(int i=group;i<rodSet.length;i+=banks)rodSet[i]=value;
        }
        /** CPS categories from public second-generation RBMK descriptions: SAR 24, ER 24, AC 24, MR 139. */
        private void setCpsGroup(int group,int value){
            int from=group==0?0:group==1?24:group==2?48:72;
            int to=group==0?24:group==1?48:group==2?72:211;
            for(int i=from;i<to;i++)rodSet[i]=value;
        }
        private float cpsAverage(int group){
            int from=group==0?0:group==1?24:group==2?48:72;
            int to=group==0?24:group==1?48:group==2?72:211;float sum=0f;
            for(int i=from;i<to;i++)sum+=rodSet[i];return sum/(to-from);
        }
        private String cpsClass(int rod){return rod<24?"SAR":rod<48?"ER":rod<72?"AC":"MR";}
        private void updateBankAverages(){
            Arrays.fill(bankActual,0f);
            for(int i=0;i<rodActual.length;i++)bankActual[i%banks]+=rodActual[i];
            for(int g=0;g<banks;g++){int count=(210-g)/banks+1;bankActual[g]/=count;bankSet[g]=Math.round(bankActual[g]);}
        }
        private int[] pack(int command){
            int[] v=new int[8+pumps+banks];v[0]=2;v[1]=expert?1:0;v[2]=automatic?1:0;v[3]=command;v[4]=rodSetpoint;v[5]=loadDemand;v[6]=turbineValve;v[7]=feedwater;
            System.arraycopy(pumpSet,0,v,8,pumps);System.arraycopy(bankSet,0,v,8+pumps,banks);return v;
        }
        private void send(){configure(pack(0));}

        @Override public void updateTile(){
            float dt=Math.min(delta(),4f);
            boolean fueled=items.get(fuelItem)>0;float water=liquids.get(coolant);boolean wet=water>.001f;

            if(scrammed){
                rodSetpoint=0;Arrays.fill(rodSet,0);for(int i=0;i<rodActual.length;i++)rodActual[i]=Mathf.approach(rodActual[i],0f,2.5f*dt);
                updateBankAverages();rodPosition=average(rodActual);if(rodPosition<.2f&&powerLevel<.03f)scrammed=false;
            }else if(!expert){
                float target=loadDemand/100f;
                if(automatic){
                    float feedback=1f+voidFraction*.42f;
                    float desired=8f+target*42f*(1f+xenon*1.35f)/feedback;
                    rodSetpoint=Mathf.clamp(Math.round(desired),12,88);
                }
                Arrays.fill(rodSet,rodSetpoint);
                for(int i=0;i<rodActual.length;i++)rodActual[i]=Mathf.approach(rodActual[i],rodSetpoint,(rodActual[i]>rodSetpoint?.34f:.10f)*dt);
                updateBankAverages();Arrays.fill(pumpSet,loadDemand>115?100:88);feedwater=92;turbineValve=100;
                rodPosition=average(rodActual);
            }else{
                // In expert mode, 'automatic' only drives the 24 AC rods; SAR/ER/MR remain operator-set.
                if(automatic){float error=loadDemand/100f-powerLevel;int ac=Mathf.clamp(Math.round(cpsAverage(2)+error*2.2f),0,100);setCpsGroup(2,ac);}
                for(int i=0;i<rodActual.length;i++)rodActual[i]=Mathf.approach(rodActual[i],rodSet[i],(rodActual[i]>rodSet[i]?.32f:.095f)*dt);
                updateBankAverages();rodPosition=average(rodActual);
            }

            for(int i=0;i<pumps;i++)pumpPhase[i]=(pumpPhase[i]+pumpSet[i]/100f*(wet?15f:4f)*dt)%360f;
            turbinePhase=(turbinePhase+(productionEfficiency*14f+(powerLevel>.02f?1.5f:0f))*dt)%360f;
            leftLoopFlow=(pumpSet[0]+pumpSet[1]+pumpSet[2]+pumpSet[3])/400f;
            rightLoopFlow=(pumpSet[4]+pumpSet[5]+pumpSet[6]+pumpSet[7])/400f;
            float flow=(leftLoopFlow+rightLoopFlow)*.5f,feed=feedwater/100f,turbine=turbineValve/100f;
            coolantFlow=(int)(flow*100f);
            float variance=0f;for(float r:rodActual){float d=r-rodPosition;variance+=d*d;}variance=(float)Math.sqrt(variance/rodActual.length)/100f;
            // Equivalent inserted rods across the 211-rod CPS; used as an operator-facing gameplay ORM.
            orm=(1f-rodPosition/100f)*211f;spatialPeak=1f+variance*1.9f;

            if(fueled&&wet){
                float coolantUse=(.018f+powerLevel*.028f)*(.55f+flow*.45f)*dt;
                liquids.remove(coolant,Math.min(water,coolantUse));
                float rodDrive=Math.max(0f,(rodPosition-8f)/42f);
                float voidFeedback=1f+voidFraction*(.38f+Mathf.clamp((rodPosition-58f)/35f)*.72f);
                float poison=1f/(1f+xenon*1.35f);
                float target=rodDrive*voidFeedback*poison*spatialPeak+tipPulse;
                powerLevel=Mathf.lerpDelta(powerLevel,target,.015f+Math.min(powerLevel,1.5f)*.010f);
                tipPulse=Mathf.approachDelta(tipPulse,0f,.012f);

                float steamTarget=Mathf.clamp(.05f+powerLevel*.32f-flow*.28f-feed*.12f+variance*.25f);
                voidFraction=Mathf.lerpDelta(voidFraction,steamTarget,.017f);
                float tempTarget=.14f+powerLevel*.34f+voidFraction*.18f-flow*.19f-feed*.08f+variance*.28f;
                temperature=Mathf.lerpDelta(temperature,tempTarget,.011f);
                float pressureTarget=.12f+powerLevel*.30f+voidFraction*.30f-turbine*.24f-feed*.08f;
                pressure=Mathf.lerpDelta(pressure,pressureTarget,.014f);
                // Drum-separator level: feedwater raises it, steam demand and low circulation draw it down.
                float drumTarget=Mathf.clamp(.52f+(feed-.90f)*.72f-(powerLevel-1f)*.09f-(1f-flow)*.18f);
                drumLevel=Mathf.lerpDelta(drumLevel,drumTarget,.008f);
                float xenonTarget=powerLevel<.55f?Mathf.clamp((.64f-powerLevel)*1.25f):Mathf.clamp(.15f-powerLevel*.055f);
                xenon=Mathf.lerpDelta(xenon,xenonTarget,.00055f);operationTime+=dt;
                if(timer(fuelTimer,fuelDuration/Math.max(.3f,powerLevel)))items.remove(fuelItem,1);
            }else{
                powerLevel=Mathf.approachDelta(powerLevel,0f,.007f);temperature=Mathf.approachDelta(temperature,.12f,wet?.005f:.0014f);
                pressure=Mathf.approachDelta(pressure,.08f,.004f);drumLevel=Mathf.approachDelta(drumLevel,.55f,.002f);voidFraction=Mathf.approachDelta(voidFraction,0f,.003f);xenon=Mathf.approachDelta(xenon,0f,.00012f);
            }

            productionEfficiency=(fueled&&wet&&!scrammed)?Mathf.clamp(powerLevel*turbine,0f,2f):0f;
            boolean danger=powerLevel>2.25f||temperature>1.08f||pressure>1.08f||drumLevel<.14f||drumLevel>.91f||spatialPeak>1.42f||(powerLevel>.8f&&!wet)||(powerLevel>1f&&(leftLoopFlow<.45f||rightLoopFlow<.45f))||(expert&&orm<30f);
            dangerTime=Mathf.clamp(dangerTime+(danger?dt:-dt*1.8f),0f,240f);
            if(dangerTime>120f){Events.fire(mindustry.game.EventType.Trigger.thoriumReactorOverheat);kill();}
            if(powerLevel>1.35f&&Mathf.chanceDelta((powerLevel-1.25f)*.035f))Fx.reactorsmoke.at(x+Mathf.range(size*3f),y+Mathf.range(size*3f));
        }

        private float average(int[] a){float s=0;for(int v:a)s+=v;return s/a.length;}
        private float average(float[] a){float s=0;for(float v:a)s+=v;return s/a.length;}
        public void scram(){
            if(!scrammed&&orm<15f&&voidFraction>.30f&&powerLevel>.5f)tipPulse=Math.max(tipPulse,.78f+voidFraction*.55f);
            scrammed=true;automatic=false;rodSetpoint=0;Arrays.fill(bankSet,0);Arrays.fill(rodSet,0);
        }

        @Override public void buildConfiguration(Table root){
            root.background(Styles.black6).margin(6f);
            root.pane(t->{
                t.defaults().pad(3f);t.label(()->"[accent]RBMK-1000 / UNIT CONTROL[]").colspan(2).left().row();
                t.label(()->expert?Core.bundle.get("ui.rbmk-expert-on"):Core.bundle.get("ui.rbmk-guided-on")).colspan(2).left().row();
                t.button(Core.bundle.get("ui.rbmk-guided"),()->{expert=false;automatic=true;send();}).height(42f).growX();
                t.button(Core.bundle.get("ui.rbmk-expert"),()->{expert=true;automatic=false;send();}).height(42f).growX().row();
                slider(t,"ui.rbmk-load",25,200,loadDemand,v->{loadDemand=v;send();});
                slider(t,"ui.rbmk-master",0,100,rodSetpoint,v->{rodSetpoint=v;send();});
                t.check(Core.bundle.get(expert?"ui.rbmk-local-auto":"ui.rbmk-auto"),automatic,v->{automatic=v;send();}).left().colspan(2).row();
                t.button("[scarlet]AZ-5 / SCRAM[]",()->configure(pack(1))).height(52f).growX().colspan(2).row();
                t.image().color(Pal.accent).height(2f).growX().colspan(2).padTop(5f).padBottom(5f).row();
                t.label(()->"[accent]"+Core.bundle.get("ui.rbmk-professional")+"[]").colspan(2).left().row();
                slider(t,"ui.rbmk-turbine",20,100,turbineValve,v->{turbineValve=v;send();});
                slider(t,"ui.rbmk-feedwater",20,100,feedwater,v->{feedwater=v;send();});
                for(int i=0;i<pumps;i++){final int id=i;slider(t,"ui.rbmk-pump",0,100,pumpSet[i],v->{pumpSet[id]=v;send();},i+1);}
                t.label(()->Core.bundle.get("ui.rbmk-cps-groups")).colspan(2).left().padTop(5f).row();
                slider(t,"ui.rbmk-sar",0,100,Math.round(cpsAverage(0)),v->configure(new int[]{213,0,v}));
                slider(t,"ui.rbmk-er",0,100,Math.round(cpsAverage(1)),v->configure(new int[]{213,1,v}));
                slider(t,"ui.rbmk-ac",0,100,Math.round(cpsAverage(2)),v->configure(new int[]{213,2,v}));
                slider(t,"ui.rbmk-mr",0,100,Math.round(cpsAverage(3)),v->configure(new int[]{213,3,v}));
                t.label(()->"[accent]"+Core.bundle.get("ui.rbmk-individual")+"[]").colspan(2).left().padTop(6f).row();
                Slider selector=new Slider(1,211,1,false),position=new Slider(0,100,1,false);
                selector.setValue(selectedRod+1);position.setValue(rodSet[selectedRod]);
                t.label(()->Core.bundle.format("ui.rbmk-selector",(int)selector.getValue(),cpsClass((int)selector.getValue()-1))).left();t.add(selector).width(235f).row();
                t.label(()->Core.bundle.format("ui.rbmk-selected-position",(int)position.getValue())).left();t.add(position).width(235f).row();
                selector.changed(()->{selectedRod=(int)selector.getValue()-1;position.setValue(rodSet[selectedRod]);});
                position.changed(()->configure(new int[]{211,selectedRod,(int)position.getValue()}));
                Slider visual=new Slider(1,709,1,false);visual.setValue(selectedVisualChannel+1);
                t.label(()->Core.bundle.format("ui.rbmk-visual-channel",(int)visual.getValue(),visualType((int)visual.getValue()-1))).left();t.add(visual).width(235f).row();
                visual.changed(()->selectedVisualChannel=(int)visual.getValue()-1);
                t.label(()->statusText()).wrap().width(400f).left().colspan(2).padTop(7f);
            }).width(440f).maxHeight(460f);
        }
        private interface IntChange{void get(int v);}
        private void slider(Table t,String key,int min,int max,int value,IntChange change,Object...args){
            Slider s=new Slider(min,max,1,false);s.setValue(value);
            t.label(()->Core.bundle.format(key,args.length==0?new Object[]{(int)s.getValue()}:new Object[]{args[0],(int)s.getValue()})).left();
            s.changed(()->change.get((int)s.getValue()));t.add(s).width(235f).row();
        }
        private String visualType(int channel){
            int mapped=rbmk.gfx.RodField.mappedControl(channel);
            return mapped<0?Core.bundle.get("ui.rbmk-fuel-channel"):"CPS #"+(mapped+1)+" / "+cpsClass(mapped);
        }
        private String statusText(){
            String st=dangerTime>0?"[scarlet]TRIP CONDITION[]":scrammed?"[orange]SCRAM[]":"[green]GRID SYNCHRONIZED[]";
            return st+"\nP "+Strings.fixed(powerLevel*100f,0)+"% | OUT "+Strings.fixed(productionEfficiency*powerProduction*60f,0)+"/s | T "+Strings.fixed(temperature*100f,0)+"% | VOID "+Strings.fixed(voidFraction*100f,0)+"% | DRUM "+Strings.fixed(drumLevel*100f,0)+"% | LOOP "+Strings.fixed(leftLoopFlow*100f,0)+"/"+Strings.fixed(rightLoopFlow*100f,0)+"% | Xe "+Strings.fixed(xenon*100f,0)+"% | ORM "+Strings.fixed(orm,1)+" | PEAK x"+Strings.fixed(spatialPeak,2);
        }

        @Override public int[] config(){return pack(0);}
        @Override public boolean acceptItem(Building source,Item item){return item==fuelItem&&items.get(item)<itemCapacity;}
        @Override public boolean acceptLiquid(Building source,Liquid liquid){return liquid==coolant&&liquids.get(liquid)<liquidCapacity;}
        @Override public double sense(LAccess sensor){if(sensor==LAccess.heat)return temperature;return super.sense(sensor);}
        @Override public void draw(){rbmk.gfx.ReactorModel.instance.draw(this);}
        @Override public void drawLight(){Drawf.light(x,y,86f+Mathf.absin(8f,6f),dangerTime>0?Color.scarlet:lightColor,.45f*Mathf.clamp(powerLevel));}
        @Override public boolean shouldExplode(){return true;}

        @Override public byte version(){return 4;}
        @Override public void write(Writes w){
            super.write(w);w.i(rodSetpoint);w.i(coolantFlow);w.f(rodPosition);w.f(powerLevel);w.f(temperature);w.f(pressure);w.f(voidFraction);w.f(xenon);w.f(tipPulse);w.f(dangerTime);w.bool(automatic);w.bool(scrammed);
            w.bool(expert);w.i(loadDemand);w.i(turbineValve);w.i(feedwater);for(int v:pumpSet)w.i(v);for(int v:bankSet)w.i(v);for(float v:bankActual)w.f(v);
            w.f(drumLevel);w.f(leftLoopFlow);w.f(rightLoopFlow);
            for(int v:rodSet)w.i(v);for(float v:rodActual)w.f(v);
        }
        @Override public void read(Reads r,byte revision){
            super.read(r,revision);rodSetpoint=r.i();coolantFlow=r.i();rodPosition=r.f();powerLevel=r.f();temperature=r.f();pressure=r.f();voidFraction=r.f();xenon=r.f();tipPulse=r.f();dangerTime=r.f();automatic=r.bool();scrammed=r.bool();
            if(revision>=2){expert=r.bool();loadDemand=r.i();turbineValve=r.i();feedwater=r.i();for(int i=0;i<pumps;i++)pumpSet[i]=r.i();for(int i=0;i<banks;i++)bankSet[i]=r.i();for(int i=0;i<banks;i++)bankActual[i]=r.f();}
            else{Arrays.fill(bankSet,rodSetpoint);Arrays.fill(bankActual,rodPosition);Arrays.fill(pumpSet,88);}
            if(revision>=4){drumLevel=r.f();leftLoopFlow=r.f();rightLoopFlow=r.f();}
            if(revision>=3){for(int i=0;i<rodSet.length;i++)rodSet[i]=r.i();for(int i=0;i<rodActual.length;i++)rodActual[i]=r.f();}
            else{for(int i=0;i<rodSet.length;i++){int g=i%banks;rodSet[i]=bankSet[g];rodActual[i]=bankActual[g];}}
        }
    }
}
