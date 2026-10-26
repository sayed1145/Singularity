package astro.content;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import astro.g3d.*;
import mindustry.graphics.*;
import mindustry.world.blocks.units.*;

/**
 * The Detainer's unit factory with the warp-gate visuals of {@link FabricatorModel}.
 *
 * <p>This class only replaces how the block is drawn: the baked gate sprite, the live 3D rings, the light effects,
 * a translucent scan print of the finished unit while it leaves the block and a warp flash when it is released.
 * Production logic, costs, timing, payload handling and configuration are the untouched vanilla
 * {@link UnitFactory} behaviour; all animation state lives in draw() and is never saved or synchronised.
 */
public class AstroFabricator extends UnitFactory{

    public AstroFabricator(String name){
        super(name);
        //the rings, the hologram and the arrival flash extend beyond the 5x5 footprint (drawing only)
        clipSize = 220f;
    }

    public class AstroFabricatorBuild extends UnitFactoryBuild{
        /** visual state: smoothed charge / ring height, ring angle, flash after firing, scan print progress */
        public float vCharge, vLift, vSpin, vFlash, vGhost, vTime;
        public boolean vHadPayload;
        public float vLastX, vLastY;
        long vFrame = -1;

        void step(){
            long frame = Core.graphics == null ? vFrame + 1 : Core.graphics.getFrameId();
            float dt = frame != vFrame ? Time.delta : 0f;
            vFrame = frame;
            if(dt <= 0f) return;
            vTime += dt;
            boolean has = payload != null;
            float frac = currentPlan == -1 ? 0f : Mathf.clamp(fraction());
            float goal = has ? 1f : frac;
            vCharge += (goal - vCharge) * Mathf.clamp(0.09f * dt);
            vLift += (goal - vLift) * Mathf.clamp(0.035f * dt);
            vSpin = (vSpin + dt * (0.5f + 6.5f * vCharge * vCharge + 9f * vFlash)) % 360f;
            vFlash = Math.max(0f, vFlash - dt / 34f);
            if(has && !vHadPayload){
                //charge complete: the gate fires and the print of the Detainer starts
                vFlash = 1f;
                vGhost = 0f;
                AstroFx.warpOut.at(x, y, 0f, AstroFx.gold);
            }
            if(has){
                vLastX = payload.x();
                vLastY = payload.y();
                vGhost = Math.min(1f, vGhost + dt / 28f);
            }else if(vHadPayload){
                //the unit was released: it arrives for real with a warp flash
                vFlash = 1f;
                AstroFx.warpIn.at(vLastX, vLastY, 0f, AstroFx.gold);
            }
            vHadPayload = has;
        }

        @Override
        public void draw(){
            step();
            Draw.rect(region, x, y);
            Draw.rect(outRegion, x, y, rotdeg());

            float active = currentPlan != -1 && payload == null ? Mathf.clamp(speedScl) : 0f;
            FabricatorModel.draw(x, y, team.color, vCharge, vLift, vSpin, vFlash, active, vTime, Layer.blockOver + 0.12f);

            //the finished unit: a translucent scan print that completes while the payload moves out
            payRotation = rotdeg();
            Unit3DType<?> t = payload != null && payload.unit.type instanceof Unit3DType<?> u ? u : null;
            if(t != null){
                t.payloadAlpha = 0.28f + 0.3f * vGhost;
                t.payloadClipZ = vGhost < 0.96f ? -2f + 90f * vGhost : Float.MAX_VALUE;
                t.renderer.scanR = 1f; t.renderer.scanG = 0.78f; t.renderer.scanB = 0.35f;
            }
            Draw.z(Layer.blockOver);
            drawPayload();
            if(t != null){
                t.payloadAlpha = 1f;
                t.payloadClipZ = Float.MAX_VALUE;
            }

            Draw.z(Layer.blockOver + 0.1f);
            Draw.rect(topRegion, x, y);
            Draw.reset();
        }

        @Override
        public void drawLight(){
            super.drawLight();
            Drawf.light(x, y, 70f + 70f * vCharge + 60f * vFlash, AstroFx.gold, 0.3f + 0.45f * vCharge + 0.5f * vFlash);
        }
    }
}
