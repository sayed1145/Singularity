#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""v8.7.7 patch: keep the rim crawl at 0.1 tiles/s, but lower the innermost cap 1 -> 0.4 tiles/s;
audit + bundles + version bumped. Verbatim replacement, FAIL-loud on any miss."""
import sys
ROOT = "/home/user/Singularity-src/Singularity-8.7.3/source"

def patch(path, pairs):
    s = open(ROOT + "/" + path, encoding="utf-8").read()
    for i, (old, new) in enumerate(pairs):
        n = s.count(old)
        if n != 1:
            print(f"FAIL {path} #{i}: matched {n} times\n---\n{old[:200]}\n---"); sys.exit(1)
        s = s.replace(old, new)
    open(ROOT + "/" + path, "w", encoding="utf-8").write(s)
    print(f"patched {path} ({len(pairs)} replacements)")

# ---------------- BHBullets.java : core cap 1 -> 0.4 (rim unchanged) ----------------
patch("src/blackhole/BHBullets.java", [
(
"""        /** v8.7.6: 吸收速度剖面（格/秒）—— 最外缘 {@link #rimSpeed}，由外向内逐格递增，
         *  最里一格恒为 {@link #coreSpeed}；0.1 与 1 让整条剖面成为单调爬升的"爬行场"。 */
        public float rimSpeed = 0.1f, coreSpeed = 1f;""",
"""        /** v8.7.7: 吸收速度剖面（格/秒）—— 最外缘 {@link #rimSpeed} 维持 0.1 不变，由外向内逐格递增，
         *  最里一格恒为 {@link #coreSpeed}；0.1 与 0.4 让整条剖面从头到尾都是一场慢速爬行。 */
        public float rimSpeed = 0.1f, coreSpeed = 0.4f;"""
),
(
"""                //v8.7.6: 最外缘每秒 0.1 格，从外到内按归一化深度线性逐格递增，最里一格（8 px）恒为每秒 1 格。""",
"""                //v8.7.7: 最外缘每秒 0.1 格（不变），从外到内按归一化深度线性逐格递增，最里一格（8 px）恒为每秒 0.4 格。"""
),
])

# ---------------- V873Audit.java : expectations 0.1 / 0.4 ----------------
patch("src/blackhole/V873Audit.java", [
("* v8.7.6 self-check (it keeps every 8.7.x check that still applies, and proves the deletions).",
 "* v8.7.7 self-check (it keeps every 8.7.x check that still applies, and proves the deletions)."),
(
""" * check below proves nothing named \"gravity-well\" is registered any more. <b>8.7.6</b>: the black hole, the
 * mod's only remaining gravity source, is slowed again to a monotone 0.1 tiles/s at the outer rim ramping
 * tile-by-tile to a 1 tile/s plateau inside the innermost tile.""",
""" * check below proves nothing named \"gravity-well\" is registered any more. <b>8.7.6</b> slowed the core to
 * 1 tile/s; <b>8.7.7</b> drops it to 0.4 tiles/s while the outer rim stays at 0.1 tiles/s, so the entire
 * fall - horizon included - is one continuous crawl.""",
),
(
"""     * v8.7.6: the only gravity source left is the black hole, and it must stay a crawl: 0.1 tile/s at the
     * outer rim, ramping monotonically tile-by-tile to 1 tile/s inside the innermost tile, and nothing may
     * swallow a unit in under 1.5 seconds.""",
"""     * v8.7.7: the only gravity source left is the black hole, and it must stay a crawl: 0.1 tile/s at the
     * outer rim, ramping monotonically tile-by-tile to 0.4 tile/s inside the innermost tile, and nothing may
     * swallow a unit in under 1.5 seconds.""",
),
(
"""        if(g.rimSpeed != 0.1f || g.coreSpeed != 1f){
            throw new IllegalStateException("v8.7.6: the black hole absorption profile is wrong: rim=" +
                g.rimSpeed + " core=" + g.coreSpeed + " (expected 0.1 and 1 tiles/s)");
        }
        //v8.7.6: the profile normalises to the innermost tile, so 1 tile/s is the absolute maximum
        if(g.coreSpeed > 1f){
            throw new IllegalStateException("v8.7.6: the black hole core pull exceeded the 1 tiles/s maximum: " +
                g.coreSpeed);
        }""",
"""        if(g.rimSpeed != 0.1f || g.coreSpeed != 0.4f){
            throw new IllegalStateException("v8.7.7: the black hole absorption profile is wrong: rim=" +
                g.rimSpeed + " core=" + g.coreSpeed + " (expected 0.1 and 0.4 tiles/s)");
        }
        //v8.7.7: the profile normalises to the innermost tile, so 0.4 tiles/s is the absolute maximum
        if(g.coreSpeed > 0.4f){
            throw new IllegalStateException("v8.7.7: the black hole core pull exceeded the 0.4 tiles/s maximum: " +
                g.coreSpeed);
        }"""
),
(
"""            throw new IllegalStateException("v8.7.6: the black hole could still swallow a unit in under 1.5 s");""",
"""            throw new IllegalStateException("v8.7.7: the black hole could still swallow a unit in under 1.5 s");"""
),
("""        //v8.7.6: the gravity well must be gone - block, and every trace of it that could come back""",
 """        //v8.7.7: the gravity well must be gone - block, and every trace of it that could come back"""),
("""            throw new IllegalStateException("v8.7.6: the gravity well is still registered");""",
 """            throw new IllegalStateException("v8.7.7: the gravity well is still registered");"""),
("""        Log.info("[blackhole/v8.7.6] self-check passed: arc slider bounds, accelerator +50% (unboostable), \"""",
 """        Log.info("[blackhole/v8.7.7] self-check passed: arc slider bounds, accelerator +50% (unboostable), \""""),
('            "black hole crawling absorption 0.1-1 tiles/s ramping tile-by-tile to a 1-tile core, " +',
 '            "black hole crawling absorption 0.1-0.4 tiles/s ramping tile-by-tile to a 1-tile core capped at 0.4, " +'),
])

# ---------------- mod.hjson ----------------
patch("mod.hjson", [
('version: "8.7.6"', 'version: "8.7.7"'),
(
'''subtitle: "8.7.6 · 黑洞吸收削到爬行场：最外缘每秒0.1格、由外向内逐格递增、最里一格恒为每秒1格（线性单调剖面；伤害、范围、寿命、只伤建筑等其余机制全部不变；引力阱维持删除状态）"''',
'''subtitle: "8.7.7 · 黑洞内圈封顶再降至每秒0.4格：最外缘维持每秒0.1格、由外向内逐格线性递增、最里一格恒为每秒0.4格（全程爬行场；伤害、范围、寿命、只伤建筑等其余机制全部不变；引力阱维持删除状态）"'''
),
])

# ---------------- bundles: describe the 0.4 cap ----------------
patch("assets/bundles/bundle_zh_CN.properties", [
("吸收同样是慢的——最外缘 0.1 格/秒，由外向内逐格线性递增，最里一格恒为 1 格/秒，绝不一口吞人；单位跑得比吸收速度快就能挣脱。",
 "吸收同样是慢的——最外缘 0.1 格/秒，由外向内逐格线性递增，最里一格恒为 0.4 格/秒，绝不一口吞人；单位跑得比吸收速度快就能挣脱。"),
])
patch("assets/bundles/bundle.properties", [
("The absorption is slow as well - 0.1 tiles/s at the outer rim, ramping monotonically tile by tile to 1 tile/s inside the innermost tile - so nothing is ever swallowed in one frame, and a unit that moves faster than the absorption can break out.",
 "The absorption is slow as well - 0.1 tiles/s at the outer rim, ramping monotonically tile by tile to 0.4 tiles/s inside the innermost tile - so nothing is ever swallowed in one frame, and a unit that moves faster than the absorption can break out."),
])

print("ALL v8.7.7 CODE PATCHES APPLIED")
