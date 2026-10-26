package blackhole;

import mindustry.type.*;

public class BHItems{
    public static Item degenerateMatter, hawkingDust, singularityCore;

    public static void load(){
        // 简并物质：被压缩到电子简并压支撑的超致密物质
        degenerateMatter = new Item("degenerate-matter", BHPal.degenerate){{
            hardness = 5;
            cost = 1.6f;
            explosiveness = 0.2f;
            charge = 0.35f;
            radioactivity = 0.15f;
        }};

        // 霍金尘埃：事件视界蒸发出的高能粒子凝结物
        hawkingDust = new Item("hawking-dust", BHPal.hawking){{
            hardness = 4;
            cost = 1.2f;
            charge = 0.6f;
            explosiveness = 0.35f;
        }};

        // 奇点核心：可搬运的微型黑洞封装体，本模组顶级材料
        singularityCore = new Item("singularity-core", BHPal.accretion){{
            hardness = 7;
            cost = 2.4f;
            explosiveness = 0.9f;
            charge = 1.1f;
            radioactivity = 0.4f;
        }};
    }
}
