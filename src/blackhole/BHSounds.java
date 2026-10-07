package blackhole;

import arc.audio.*;
import mindustry.gen.*;

/**
 * 音效集中映射。
 *
 * 说明：视觉特效（BHFx）全部为本模组自制，不引用任何原版 Fx。
 * 音频则复用游戏内置音源 —— 打包自制音频会让 jar 体积暴涨，
 * 且安卓端解码开销更大。这里集中成一层，方便统一调音。
 */
public class BHSounds{
    public static Sound
        pulse    = Sounds.shootBreach,       // 轻武器脉冲
        thump    = Sounds.shootTank,         // 中口径实弹
        lance    = Sounds.shootMeltdown,     // 穿透光束
        beam     = Sounds.beamMeltdown,      // 持续激光循环
        collapse = Sounds.shootOmura,        // 奇点发射
        charge   = Sounds.chargeLancer,      // 蓄力
        rift     = Sounds.shootSpectre;      // 撕裂/空间类
}
