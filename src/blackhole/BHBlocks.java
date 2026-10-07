package blackhole;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.pattern.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.units.*;
import mindustry.entities.part.*;
import mindustry.entities.part.DrawPart.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

public class BHBlocks{
    public static Block
        gravitonPress, hawkingCondenser, singularityForge, accretionFoundry,
        eventHorizon, accretionCannon, hawkingEmitter, frameDragger, quasarLance,
        horizonAssembler, citadelYard;

    public static void load(){

        // ---------------- 生产链 ----------------

        gravitonPress = new GenericCrafter("graviton-press"){{
            requirements(Category.crafting, ItemStack.with(Items.titanium, 80, Items.lead, 100, Items.silicon, 60));
            size = 2;
            health = 220;
            craftTime = 75f;
            hasPower = true;
            hasItems = true;
            itemCapacity = 20;
            consumePower(2.6f);
            consumeItems(ItemStack.with(Items.titanium, 2, Items.graphite, 2));
            outputItem = new ItemStack(BHItems.degenerateMatter, 1);
            craftEffect = BHFx.horizonPulse;
            updateEffect = BHFx.pressCrush;
            drawer = new DrawMulti(new DrawDefault(), new DrawGravityWell(0.45f, 9f));
        }};

        hawkingCondenser = new GenericCrafter("hawking-condenser"){{
            requirements(Category.crafting, ItemStack.with(Items.plastanium, 60, Items.titanium, 120, BHItems.degenerateMatter, 40));
            size = 3;
            health = 420;
            craftTime = 100f;
            hasPower = true;
            itemCapacity = 24;
            consumePower(5.5f);
            consumeItems(ItemStack.with(BHItems.degenerateMatter, 2, Items.thorium, 1));
            consumeLiquid(Liquids.cryofluid, 0.12f);
            outputItem = new ItemStack(BHItems.hawkingDust, 1);
            craftEffect = BHFx.condenseMist;
            updateEffect = BHFx.condenseMist;
            drawer = new DrawMulti(new DrawDefault(), new DrawGravityWell(0.6f, 14f));
            liquidCapacity = 40f;
        }};

        /** 奇点熔炉：悬浮真黑洞，并对周围画面产生真实引力透镜 */
        singularityForge = new GenericCrafter("singularity-forge"){{
            requirements(Category.crafting, ItemStack.with(
                Items.surgeAlloy, 150, Items.phaseFabric, 120, BHItems.hawkingDust, 80, BHItems.degenerateMatter, 200));
            size = 4;
            health = 900;
            craftTime = 180f;
            hasPower = true;
            itemCapacity = 30;
            consumePower(16f);
            consumeItems(ItemStack.with(BHItems.hawkingDust, 3, BHItems.degenerateMatter, 4, Items.surgeAlloy, 1));
            outputItem = new ItemStack(BHItems.singularityCore, 1);
            craftEffect = BHFx.forgeBloom;
            // v5 安全视觉：移除了工厂黑洞/透镜/强光，改为受全局帧预算限制的
            // 约束棱镜阵列。再多工厂也不会叠加屏幕扭曲或形成光污染。
            drawer = new DrawMulti(new DrawDefault(), new DrawContainmentArray());
            lightRadius = 0f;
            clipSize = 170f;
        }};

        /** 吸积铸造厂 4x4：程序化 3D 铸造流水线（浇注 -> 冲压 -> 机械臂取件 -> 滚道出料），模型见 FoundryModel */
        accretionFoundry = new GenericCrafter("accretion-foundry"){{
            requirements(Category.crafting, ItemStack.with(Items.titanium, 220, Items.silicon, 160, Items.plastanium, 60, BHItems.degenerateMatter, 80));
            size = 4;
            health = 1100;
            //one craft = one full animation cycle of FoundryModel (FoundryModel.craftTicks)
            craftTime = 120f;
            hasPower = true;
            hasItems = true;
            itemCapacity = 30;
            consumePower(6f);
            consumeItems(ItemStack.with(Items.titanium, 3, Items.graphite, 2));
            outputItem = new ItemStack(BHItems.degenerateMatter, 3);
            craftEffect = BHFx.pressCrush;
            //replaced by the 3D drawer in Models.install()
            drawer = new DrawDefault();
        }};

        // ---------------- 炮塔（全部重做）----------------

        /** 1. 吸积炮 3x3 —— 双管速射，弹链换弹药 */
        accretionCannon = new ItemTurret("accretion-cannon"){{
            requirements(Category.turret, ItemStack.with(
                Items.titanium, 180, Items.silicon, 140, BHItems.degenerateMatter, 90));
            ammo(
                BHItems.degenerateMatter, BHBullets.accretionSpray,
                BHItems.hawkingDust, BHBullets.microHole
            );
            size = 3;
            health = 900;
            range = 235f;
            reload = 13f;
            recoil = 2.2f;
            shootCone = 24f;
            inaccuracy = 3f;
            rotateSpeed = 5.5f;
            maxAmmo = 30;
            shoot = new ShootAlternate(7f);
            shootSound = BHSounds.rift;
            targetAir = targetGround = true;
            coolant = consumeCoolant(0.2f);
            shootEffect = BHFx.muzzleFlash;
            smokeEffect = BHFx.muzzleDust;
            heatColor = BHPal.accretion;
            drawer = new DrawTurret(){{
                parts.add(
                    // 双管随后坐前后滑动
                    new RegionPart("-barrel-l"){{
                        progress = PartProgress.recoil;
                        moveY = -3.4f;
                        mirror = false;
                        under = true;
                        heatColor = BHPal.accretion;
                        heatProgress = PartProgress.heat;
                    }},
                    new RegionPart("-barrel-r"){{
                        progress = PartProgress.recoil;
                        moveY = -3.4f;
                        mirror = false;
                        under = true;
                        heatColor = BHPal.accretion;
                        heatProgress = PartProgress.heat;
                    }},
                    // 蓄能环：预热时张开
                    new RegionPart("-ring"){{
                        progress = PartProgress.warmup;
                        moveRot = 40f;
                        mirror = false;
                        under = true;
                        outline = false;
                    }}
                );
            }};
        }};

        /** 2. 参考系拖曳炮 2x2 —— 廉价高射速入门炮 */
        frameDragger = new ItemTurret("frame-dragger"){{
            requirements(Category.turret, ItemStack.with(
                Items.titanium, 70, Items.silicon, 50, Items.graphite, 60));
            ammo(
                Items.graphite, BHBullets.frameDragShot,
                BHItems.degenerateMatter, BHBullets.accretionSpray
            );
            size = 2;
            health = 380;
            range = 180f;
            reload = 9f;
            recoil = 1.4f;
            shootCone = 28f;
            inaccuracy = 5f;
            rotateSpeed = 8f;
            maxAmmo = 24;
            shoot = new ShootSpread(2, 5f);
            shootSound = BHSounds.pulse;
            shootEffect = BHFx.muzzleFlash;
            smokeEffect = BHFx.muzzleDust;
            targetAir = targetGround = true;
            heatColor = BHPal.degenerate;
            drawer = new DrawTurret(){{
                parts.add(
                    new RegionPart("-spin"){{
                        progress = PartProgress.warmup;
                        moveRot = 360f;
                        mirror = false;
                        under = true;
                        outline = false;
                    }},
                    new RegionPart("-barrel"){{
                        progress = PartProgress.recoil;
                        moveY = -2.2f;
                        mirror = false;
                        under = true;
                        heatColor = BHPal.degenerate;
                        heatProgress = PartProgress.heat;
                    }}
                );
            }};
        }};

        /** 3. 霍金发射器 3x3 —— 穿透光束 */
        hawkingEmitter = new PowerTurret("hawking-emitter"){{
            requirements(Category.turret, ItemStack.with(
                Items.surgeAlloy, 120, Items.phaseFabric, 90, BHItems.hawkingDust, 120));
            shootType = BHBullets.hawkingLance;
            size = 3;
            health = 1100;
            range = 252f;
            reload = 80f;
            recoil = 3f;
            shootCone = 12f;
            rotateSpeed = 3.2f;
            shake = 2.4f;
            consumePower(11f);
            shootSound = BHSounds.lance;
            chargeSound = BHSounds.charge;
            shootEffect = BHFx.hawkingCharge;
            smokeEffect = BHFx.muzzleDust;
            targetAir = targetGround = true;
            heatColor = BHPal.hawking;
            coolant = consumeCoolant(0.3f);
            drawer = new DrawTurret(){{
                parts.add(
                    // 聚焦叶片：蓄力时向外张开
                    new RegionPart("-blade"){{
                        progress = PartProgress.charge;
                        mirror = true;
                        under = true;
                        moveX = 2.6f;
                        moveRot = -22f;
                        heatColor = BHPal.hawking;
                        heatProgress = PartProgress.charge;
                    }},
                    new RegionPart("-emitter"){{
                        progress = PartProgress.recoil;
                        moveY = -2.8f;
                        mirror = false;
                        under = true;
                        heatColor = BHPal.hawking;
                        heatProgress = PartProgress.heat;
                    }}
                );
            }};
        }};

        /** 4. 类星体喷流 4x4 —— 持续灼烧激光 */
        quasarLance = new ContinuousTurret("quasar-lance"){{
            requirements(Category.turret, ItemStack.with(
                Items.surgeAlloy, 220, Items.phaseFabric, 160, BHItems.hawkingDust, 150, BHItems.degenerateMatter, 180));
            shootType = BHBullets.quasarBeam;
            size = 4;
            health = 2200;
            range = 215f;
            reload = 110f;
            recoil = 0f;
            shootCone = 8f;
            rotateSpeed = 2.1f;
            shake = 1.6f;
            consumePower(26f);
            shootSound = BHSounds.beam;
            loopSound = BHSounds.beam;
            loopSoundVolume = 1.4f;
            aimChangeSpeed = 2.4f;
            coolant = consumeCoolant(0.4f);
            targetAir = targetGround = true;
            heatColor = BHPal.accretion;
            shootWarmupSpeed = 0.06f;
            minWarmup = 0.8f;
            shootEffect = BHFx.quasarBurn;
            smokeEffect = BHFx.muzzleDust;
            drawer = new DrawTurret(){{
                parts.add(
                    new RegionPart("-ring"){{
                        progress = PartProgress.warmup;
                        moveRot = -300f;
                        mirror = false;
                        under = true;
                        outline = false;
                    }},
                    new RegionPart("-vent"){{
                        progress = PartProgress.warmup;
                        mirror = true;
                        under = true;
                        moveX = 3.2f;
                        heatColor = BHPal.accretion;
                        heatProgress = PartProgress.warmup;
                    }},
                    new RegionPart("-nozzle"){{
                        progress = PartProgress.warmup;
                        moveY = 3.6f;
                        mirror = false;
                        under = true;
                        heatColor = BHPal.core;
                        heatProgress = PartProgress.warmup;
                    }}
                );
            }};
        }};

        /**
         * 5. 事件视界炮 5x5 —— 全新机制
         * 不再是"打一发爆一下"，而是在落点【种下一颗持续存在并不断扩大的真黑洞】，
         * 期间持续吞噬单位与弹幕，并对那片区域的画面产生真实引力透镜扭曲。
         */
        eventHorizon = new ItemTurret("event-horizon"){
            {
                requirements(Category.turret, ItemStack.with(
                    Items.surgeAlloy, 400, Items.phaseFabric, 350, BHItems.singularityCore, 120, BHItems.hawkingDust, 250));
                ammo(BHItems.singularityCore, BHBullets.collapsarShell);
                size = 5;
                health = 4200;
                range = 400f;
                reload = 60f * 9f;   // 冷却很长：一次能造成 11 秒的区域封锁
                recoil = 6f;
                shootCone = 6f;
                rotateSpeed = 1.5f;
                shake = 8f;
                maxAmmo = 12;
                ammoPerShot = 2;
                shootWarmupSpeed = 0.035f;
                minWarmup = 0.92f;
                consumePower(28f);
                shootSound = BHSounds.collapse;
                chargeSound = BHSounds.charge;
                shootEffect = BHFx.collapse;
                targetAir = targetGround = true;
                coolant = consumeCoolant(0.5f);
                smokeEffect = BHFx.muzzleDust;
                heatColor = BHPal.accretion;
                drawer = new DrawTurret(){{
                    parts.add(
                        // 三层约束环，预热时反向旋转
                        new RegionPart("-ring1"){{
                            progress = PartProgress.warmup;
                            moveRot = 300f;
                            mirror = false;
                            under = true;
                            outline = false;
                        }},
                        new RegionPart("-ring2"){{
                            progress = PartProgress.warmup;
                            moveRot = -220f;
                            mirror = false;
                            under = true;
                            outline = false;
                        }},
                        // 装填臂：后坐时向外张
                        new RegionPart("-arm"){{
                            progress = PartProgress.recoil;
                            mirror = true;
                            under = true;
                            moveX = 3.4f;
                            moveRot = 18f;
                            heatColor = BHPal.core;
                            heatProgress = PartProgress.heat;
                        }},
                        new RegionPart("-core"){{
                            progress = PartProgress.charge;
                            mirror = false;
                            under = false;
                            outline = false;
                            heatColor = BHPal.core;
                            heatProgress = PartProgress.charge;
                        }}
                    );
                }};
                unitSort = UnitSorts.strongest;
                lightRadius = 140f;
            }

            @Override
            public void init(){
                super.init();
                clipSize = Math.max(clipSize, 900f);
            }
        };

        // ---------------- 单位工厂 ----------------

        horizonAssembler = new blackhole.models.Assembler3D("horizon-assembler"){{
            requirements(Category.units, ItemStack.with(
                Items.silicon, 260, Items.titanium, 200, BHItems.degenerateMatter, 150, BHItems.hawkingDust, 60));
            size = 3;
            health = 720;
            consumePower(6.5f);
            plans.add(
                new UnitPlan(BHUnits.lensing, 60f * 30f,
                    ItemStack.with(Items.silicon, 60, BHItems.degenerateMatter, 40)),
                new UnitPlan(BHUnits.ergosphere, 60f * 55f,
                    ItemStack.with(Items.silicon, 120, BHItems.hawkingDust, 50, BHItems.degenerateMatter, 70))
            );
            consumeLiquid(Liquids.cryofluid, 0.12f);
        }};

        /**
         * 奇点船坞 5x5 —— 专门用于建造地面重型载具与移动作战平台。
         * 与视界装配厂分开，是因为平台体积太大，需要独立的重型船坞。
         */
        citadelYard = new blackhole.models.Assembler3D("citadel-yard"){{
            requirements(Category.units, ItemStack.with(
                Items.silicon, 700, Items.titanium, 560, Items.surgeAlloy, 260,
                BHItems.degenerateMatter, 420, BHItems.singularityCore, 60));
            size = 5;
            health = 2600;
            consumePower(22f);
            plans.add(
                new UnitPlan(BHUnits.accretor, 60f * 45f,
                    ItemStack.with(Items.silicon, 140, Items.titanium, 120, BHItems.degenerateMatter, 90)),
                new UnitPlan(BHUnits.citadel, 60f * 190f,
                    ItemStack.with(Items.silicon, 620, Items.surgeAlloy, 240,
                        BHItems.degenerateMatter, 400, BHItems.singularityCore, 55))
            );
            consumeLiquid(Liquids.cryofluid, 0.3f);
        }};
    }

    /** 方块上的引力井涟漪。 */
    public static class DrawGravityWell extends DrawBlock{
        public float intensity, radius;

        public DrawGravityWell(float intensity, float radius){
            this.intensity = intensity;
            this.radius = radius;
        }

        @Override
        public void draw(Building build){
            float warm = build.warmup();
            if(warm <= 0.01f) return;
            Draw.z(Layer.blockAdditive);
            Draw.blend(Blending.additive);
            for(int i = 0; i < 3; i++){
                float f = (Time.time / 70f + i / 3f) % 1f;
                Draw.color(BHPal.degenerate, intensity * warm * (1f - f));
                Lines.stroke(1.4f * (1f - f) * warm);
                Lines.circle(build.x, build.y, radius * (0.25f + f * 0.9f));
            }
            Draw.blend();
            Draw.reset();
            Draw.z(Layer.block);
        }
    }

    /**
     * 奇点锻炉的安全替代视觉。
     *
     * 旧绘制器会在每个锻炉上注册全屏透镜并绘制高亮黑洞：大规模摆放时既会
     * 累积后处理开销，也会出现刺眼的橙白光斑。这个版本只绘制两张自制的低亮度
     * 贴图，不调用 BHShaders / BlackHoleRenderer / Drawf.light；同时每帧全局最多
     * 允许 18 座锻炉播放旋转动画，超出的锻炉保留普通静态建筑贴图。
     */
    public static class DrawContainmentArray extends DrawBlock{
        private static final int animationBudget = 18;
        private static long budgetFrame = -1L;
        private static int animatedThisFrame;
        private TextureRegion array, core;

        @Override
        public void load(Block block){
            array = Core.atlas.find(block.name + "-array");
            core = Core.atlas.find(block.name + "-core");
        }

        @Override
        public void draw(Building build){
            float warm = Mathf.clamp(build.warmup());
            if(warm <= 0.01f) return;

            long frame = Core.graphics.getFrameId();
            if(frame != budgetFrame){
                budgetFrame = frame;
                animatedThisFrame = 0;
            }
            if(animatedThisFrame++ >= animationBudget) return;

            // Camera cull is intentionally repeated here because a large world can hold
            // many active factories even when their block draw calls are requested.
            var cam = Core.camera;
            if(Math.abs(build.x - cam.position.x) > cam.width * 0.5f + 100f ||
                Math.abs(build.y - cam.position.y) > cam.height * 0.5f + 100f) return;

            float z = Draw.z();
            if(array != null && array.found()){
                Draw.z(z + 0.01f);
                Draw.color(BHPal.hawking, BHPal.degenerate, 0.45f);
                Draw.alpha(0.22f + warm * 0.32f);
                Draw.rect(array, build.x, build.y, Time.time * (0.55f + warm * 0.85f));
            }
            if(core != null && core.found()){
                float pulse = 0.45f + Mathf.absin(Time.time + build.id * 9f, 34f, 0.28f);
                Draw.z(z + 0.011f);
                Draw.color(BHPal.hawking);
                Draw.alpha(pulse * warm * 0.42f);
                Draw.rect(core, build.x, build.y, -Time.time * 0.32f);
            }
            Draw.reset();
            Draw.z(z);
        }
    }

    /** 方块中心的真 3D 黑洞 + 真实屏幕扭曲。保留给武器弹体使用，不再用于工厂。 */
    public static class DrawBlackHole extends DrawBlock{
        public float radius, tilt, lensStrength;

        public DrawBlackHole(float radius, float tilt, float lensStrength){
            this.radius = radius;
            this.tilt = tilt;
            this.lensStrength = lensStrength;
        }

        @Override
        public void draw(Building build){
            float warm = Mathf.clamp(build.warmup());
            if(warm <= 0.02f) return;

            float r = radius * warm;
            {
                BHShaders.addLens(build.x, build.y, r, lensStrength * warm);
            }

            Draw.z(Layer.effect - 0.5f);
            BlackHoleRenderer.draw(build.x, build.y, r, tilt, Time.time + build.id * 11f, warm);
            Draw.z(Layer.block);
            Draw.reset();
        }

        @Override
        public void drawLight(Building build){
            Drawf.light(build.x, build.y, radius * 7f * build.warmup(), BHPal.accretion, 0.7f * build.warmup());
        }
    }
}
