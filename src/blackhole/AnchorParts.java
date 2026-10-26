package blackhole;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Nullable;
import blackhole.models.Models;
import blackhole.g3d.BlockModel;
import arc.graphics.g2d.TextureRegion;
import mindustry.content.Fx;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.pattern.ShootPattern;
import mindustry.world.blocks.defense.turrets.PowerTurret;

/**
 * v8.5.2：专门用来打拘留者的那套兵器 —— 一门炮塔 + 一个单位。
 *
 * <h2>为什么需要它</h2>
 * 拘留者的力场会把飞过来的<b>可吸收</b>弹丸直接抓走（官方 {@code DetainerType#catchBullet}），
 * 吸收的能量还会变成它自己的能量与反射弹 —— 打得越凶，它越强。
 *
 * <h2>它做了什么</h2>
 * 只有一件事：弹药的官方标志位 {@link BulletType#absorbable} 设为 {@code false}。
 * 官方 {@code DetainerType.fieldAndClaws} 里的判定是
 * <pre>
 * if(!b.type.absorbable){ ...只抬起护盾...; return; }
 * </pre>
 * 所以这套兵器的弹丸<b>不会被爪子抓走、也不会被转化成它的能量</b>，仅此而已。
 *
 * <h2>它没有做什么（平衡）</h2>
 * <ul>
 *   <li>不加伤害、不加射程、不加速度 —— 数值和同级普通炮塔一个量级；</li>
 *   <li>不"针对"拘留者：它不认识拘留者，打任何单位都是同一发弹丸、同一个伤害；</li>
 *   <li>拘留者的<b>圣盾（aegis）照常生效</b>：护盾是按"这类弹丸能不能伤到我"抬起来的，
 *       与能不能吸收无关，所以这套兵器打拘留者依然要先把盾打掉，不是免费击杀。</li>
 * </ul>
 */
public final class AnchorParts{
    private AnchorParts(){}

    // =====================================================================================================
    // 新的开炮方式
    // =====================================================================================================

    /**
     * 钉刺齐射：一轮里的每一发<b>沿炮管依次后移、依次击发</b>，而不是并排喷出去。
     *
     * <p>官方的两种基本花样是：{@code ShootAlternate} 左右分炮管（横向错开，同一刻出膛），
     * {@code ShootSpread} 扇形散布（角度错开）。这里是第三种：
     * <b>纵向排队</b> —— 第 i 发的出膛点比上一发更靠后 {@code backoff} 个世界单位，
     * 并且晚 {@code spacing} 刻击发，于是同一轮在炮口排成一条纵向的队列依次飞出，
     * 落点是一条紧密的线而不是一片扇面。
     *
     * <p>纯几何：只改出膛点与击发时刻，不改弹丸本身，服务端与客户端行为一致。
     */
    public static class SpearPattern extends ShootPattern{
        /** 同一轮里，后一发比前一发沿炮管向后多少世界单位。 */
        public float backoff = 5.5f;
        /** 相邻两发之间隔多少刻。 */
        public float spacing = 2.5f;
        /** 很小的一点横向抖动，0 就是严格一条直线。 */
        public float jitter = 0f;

        @Override
        public void shoot(int totalShots, BulletHandler handler, @Nullable Runnable barrelIncrementer){
            for(int i = 0; i < shots; i++){
                float y = -i * backoff;
                float x = jitter <= 0f ? 0f : Mathf.range(jitter) * i;
                handler.shoot(x, y, 0f, firstShotDelay + spacing * i, null);
                if(barrelIncrementer != null) barrelIncrementer.run();
            }
        }
    }

    // =====================================================================================================
    // 弹药：唯一的区别就是 absorbable = false
    // =====================================================================================================

    /** 钉刺弹。除了 {@code absorbable = false} 与 {@code reflectable = false}，就是一发普通的动能弹。 */
    public static BulletType spike(float speed, float damage, float lifetime){
        return new BasicBulletType(speed, damage){{
            this.lifetime = lifetime;
            width = 6f;
            height = 14f;
            //the whole point: the Detainer's claws cannot catch it, so it cannot be turned into energy
            absorbable = false;
            //and it must not be bounced back at its owner either - balanced against everything else
            reflectable = false;
            frontColor = Color.white;
            backColor = trailColor = hitColor = AureliaFx.resonance;
            trailLength = 14;
            trailWidth = 2.2f;
            hitEffect = despawnEffect = AureliaFx.lumenHit;
            shootEffect = Fx.none;
            smokeEffect = Fx.none;
        }};
    }

    // =====================================================================================================
    // 炮塔
    // =====================================================================================================

    /** 锚钉炮 (3x3)：官方 {@link PowerTurret} 的全部行为，只换掉弹药与开火方式，绘制交给 3D 模型。 */
    public static class AnchorSpike extends PowerTurret{
        public BlockModel model;

        public AnchorSpike(String name){
            super(name);
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
    }
}
