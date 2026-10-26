package blackhole;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;

import static mindustry.Vars.*;

/**
 * 真·屏幕空间引力透镜（v7.2：完整回退到 v6.1 的实现）。
 *
 * 架构与官方 Shaders.ShockwaveShader 一致：
 *   preDraw  -> 把整个游戏画面渲染进 FrameBuffer
 *   postDraw -> 用透镜着色器把 buffer blit 回屏幕
 *
 * 因为重采样的是**已经渲染完成的整幅画面**，所以地形、建筑、单位、子弹、
 * 甚至其它特效都会被真实拉弯，而不是在上面盖一张贴图。
 *
 * ── 关键时序问题（v2.0 的 bug，v3 已修复）────────────────────────────
 * 黑洞是在自己的 draw() 里调用 add() 注册的，而 draw() 发生在 preDraw 之后。
 * 如果 preDraw 里用「本帧 data 是否为空」来决定要不要开 FrameBuffer，
 * 那每帧判断时 data 永远是空的 —— buffer 从不启用，blit 从不发生，
 * 于是画面完全不扭曲，只剩贴图特效。
 *
 * 修法：用 pending（上一帧收集的）/ active（本帧正在用的）双缓冲。
 * preDraw 依据**上一帧**是否有黑洞来决定开不开 buffer，黑洞存在期间
 * 每帧都会重新注册，所以稳定生效；黑洞消失后最多多渲染一帧，无副作用。
 *
 * ── v7.2 保留的两条保护（不改变 v6.1 的画面）──────────────────────────
 * 1. 图形设置「bh-lens」可以关闭透镜（关闭时不开 FrameBuffer，零开销）；
 * 2. 像素化（pixelate）模式下跳过：像素化自己也用 FrameBuffer 包住整帧，而它在 Layer.end 结束缓冲时
 *    会解绑栈顶（即本透镜的）缓冲，导致透镜画面被画进像素化缓冲而永远不显示。
 */
public class BHShaders{
    public static LensShader lens;

    public static void init(){
        if(headless) return;
        try{
            lens = new LensShader();
            Log.info("[blackhole] 引力透镜着色器编译成功。");
        }catch(Throwable t){
            Log.err("[blackhole] 引力透镜着色器加载失败，退化为无扭曲模式", t);
            lens = null;
        }
    }

    /** 图形设置里的透镜开关（默认开）。 */
    public static boolean enabled(){
        return Core.settings == null || Core.settings.getBool("bh-lens", true);
    }

    /** 本帧注册一个引力透镜源。任何黑洞在 draw/update 里调用即可。 */
    public static void addLens(float x, float y, float radius, float strength){
        if(lens != null && !headless && enabled() && !(renderer != null && renderer.pixelate)) LensShader.add(x, y, radius, strength);
    }

    public static class LensShader extends Shader{
        /** 同屏最多处理的黑洞数量 */
        public static final int max = 12;
        /** 每个黑洞占用的浮点数：x, y, radius, strength */
        static final int stride = 4;

        /** 本帧正在收集的黑洞（世界坐标） */
        private static final FloatSeq pending = new FloatSeq();
        /** 供着色器读取的、已定稿的一组黑洞 */
        private static final FloatSeq active = new FloatSeq();
        private static final float[] none = {0f, 0f, 0f, 0f};

        private final FrameBuffer buffer = new FrameBuffer();
        private boolean capturing;

        public LensShader(){
            super(vert, frag);

            Events.run(Trigger.preDraw, () -> {
                // 用上一帧定稿的数据判断——因为本帧的 add() 还没发生
                capturing = active.size > 0 && isCompiled() && enabled() && !(renderer != null && renderer.pixelate);
                if(capturing){
                    buffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
                    buffer.begin(Color.clear);
                }
            });

            Events.run(Trigger.postDraw, () -> {
                if(capturing){
                    capturing = false;
                    buffer.end();
                    Draw.blend(Blending.disabled);
                    buffer.blit(this);
                    Draw.blend();
                }

                // 本帧收集完毕 -> 定稿，供下一帧使用
                active.clear();
                active.addAll(pending);
                pending.clear();
            });
        }

        /**
         * 注册一个引力透镜。
         * @param x,y      世界坐标
         * @param radius   事件视界半径（世界单位）
         * @param strength 透镜强度倍率，1 = 标准
         */
        public static void add(float x, float y, float radius, float strength){
            if(radius <= 0.01f || strength <= 0.001f) return;
            if(pending.size / stride >= max) return;

            // 屏幕外的直接丢弃，省 uniform 名额和像素开销
            float pad = radius * 9f;
            var cam = Core.camera;
            if(cam == null) return;
            if(x < cam.position.x - cam.width / 2f - pad || x > cam.position.x + cam.width / 2f + pad ||
               y < cam.position.y - cam.height / 2f - pad || y > cam.position.y + cam.height / 2f + pad){
                return;
            }

            pending.addAll(x, y, radius, strength);
        }

        /** 当前是否真的在做透镜捕获（供调试/统计用）。 */
        public static int activeCount(){
            return active.size / stride;
        }

        /** 本帧已登记、下一帧生效的透镜数（测试用）。 */
        public static int pendingCount(){
            return pending.size / stride;
        }

        @Override
        public void apply(){
            int count = active.size / stride;
            setUniformi("u_count", count);

            var cam = Core.camera;
            setUniformf("u_campos", cam.position.x - cam.width / 2f, cam.position.y - cam.height / 2f);
            setUniformf("u_camsize", cam.width, cam.height);
            setUniformf("u_time", Time.time);

            if(count <= 0){
                // 仍需给数组一个值，避免驱动读到未初始化内存
                setUniform4fv("u_holes", none, 0, 4);
                return;
            }

            setUniform4fv("u_holes", active.items, 0, count * stride);
        }
    }

    // ------------------------------------------------------------------
    // 着色器源码（内联，避免与官方 shaders/ 目录同名冲突）
    // ------------------------------------------------------------------

    static final String vert = """
        attribute vec4 a_position;
        attribute vec2 a_texCoord0;
        varying vec2 v_texCoords;
        void main(){
            v_texCoords = a_texCoord0;
            gl_Position = a_position;
        }
        """;

    /**
     * 引力透镜片元着色器。
     *
     * 对每个像素累加所有黑洞造成的光线偏折，再用偏折后的 UV 采样原画面。
     * 偏折沿用史瓦西度规弱场近似 α ≈ 2Rs/b，接近光子球时非线性放大，
     * 视界内则完全取黑。另外叠加吸积盘的多普勒增亮与光子环。
     */
    static final String frag = """
        // Arc Shader 会按运行平台自动注入 GLES 精度声明和 GL3 兼容语法。
        // 不要在此处添加平台条件预处理指令，否则桌面版会拒绝编译整个透镜着色器。
        #define MAX 12

        uniform sampler2D u_texture;
        uniform vec2 u_campos;
        uniform vec2 u_camsize;
        uniform float u_time;
        uniform int u_count;
        uniform vec4 u_holes[MAX];

        varying vec2 v_texCoords;

        void main(){
            vec2 uv = v_texCoords;
            vec2 world = u_campos + uv * u_camsize;

            vec2 offset = vec2(0.0);
            float dark = 0.0;
            float ring = 0.0;
            float disk = 0.0;

            for(int i = 0; i < MAX; i++){
                if(i >= u_count) break;

                vec4 h = u_holes[i];
                vec2 d = world - h.xy;
                float rs = h.z;
                float strength = h.w;
                float dist = length(d);

                float reach = rs * 9.0;
                if(dist > reach || dist < 0.0001) continue;

                vec2 dir = d / dist;

                // 光线偏折：弱场近似，近光子球非线性放大
                float b = max(dist, rs * 0.55);
                float defl = (2.0 * rs * rs) / (b * b) * rs * 1.55 * strength;

                float photon = rs * 1.5;
                if(dist < photon * 2.2){
                    float t = 1.0 - clamp((dist - photon) / (photon * 1.2), 0.0, 1.0);
                    defl *= 1.0 + t * t * 3.6;
                }

                float fade = 1.0 - smoothstep(reach * 0.55, reach, dist);
                defl *= fade;

                // 轻微的帧拖拽（Lense-Thirring）：偏折带一点切向分量，
                // 让画面在被吸入时同时产生旋转感，而不是纯径向塌陷
                vec2 tangent = vec2(-dir.y, dir.x);
                offset -= dir * defl;
                offset += tangent * defl * 0.42 * strength;

                // 视界内部：全黑
                dark = max(dark, 1.0 - smoothstep(rs * 0.92, rs * 1.06, dist));

                // 光子环
                float rw = rs * 0.16;
                float ang = atan(d.y, d.x);
                float shimmer = 0.82 + 0.18 * sin(ang * 3.0 + u_time * 0.09)
                                     + 0.10 * sin(ang * 7.0 - u_time * 0.05);
                ring += (1.0 - smoothstep(0.0, rw, abs(dist - photon))) * fade * strength * shimmer;

                // 吸积盘：视界外一圈热物质，带相对论性多普勒增亮（一侧更亮）
                float dr = dist - rs * 1.9;
                float band = exp(-dr * dr / (rs * rs * 1.1));
                float doppler = 0.55 + 0.45 * sin(ang + u_time * 0.02);
                disk += band * fade * strength * doppler;
            }

            vec2 duv = uv + offset / u_camsize;
            duv = clamp(duv, vec2(0.0005), vec2(0.9995));

            vec4 color = texture2D(u_texture, duv);

            // 视界吞噬一切光线
            color.rgb *= (1.0 - clamp(dark, 0.0, 1.0));

            // 吸积盘辉光（被引力聚焦的热物质）
            disk = clamp(disk, 0.0, 1.0);
            color.rgb += vec3(1.0, 0.52, 0.16) * disk * 0.30;

            // 光子环
            ring = clamp(ring, 0.0, 1.0);
            color.rgb += vec3(1.0, 0.74, 0.40) * ring * 0.90;

            gl_FragColor = color;
        }
        """;
}
