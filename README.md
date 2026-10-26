# Singularity · 8.6-release

An upgrade of the supplied **8.3-beta** source package for **Mindustry v160.5**.
The universal JAR contains desktop classes **and** Android DEX. Internal mod ID: `blackhole`.

**[中文完整说明](README_zh.md)** · [Changelog / reports](docs/) · [License: GPLv3](LICENSE)

> ## ⚠ Maintenance status: paused (not abandoned)
>
> The original author (**sayed1145**) is **temporarily not maintaining this project**.
> The repository is **not archived** and **Issues stay open** — bug reports and questions are still welcome,
> but please do not expect fixes or releases on a schedule.
>
> During the pause:
> - no new features, balance passes or bug fixes are planned;
> - Issues and Pull Requests remain enabled, and may be picked up later;
> - the code is published as-is, so anyone who wants to continue it may fork freely under the GPLv3.
>
> Releases stay downloadable in the meantime. The last one is **8.6-release**.

## Download

- **Binary (recommended):** [`Singularity-8.6-release.jar`](https://github.com/sayed1145/Singularity/releases/tag/8.6-release)
  — attached to the release. **One JAR for both desktop and Android** (it contains Java `.class` files for the
  desktop JVM **and** a `classes.dex` for Android/ART).
- **Source:** this repository, or GitHub's automatically generated source archives.

### Install

1. Download `Singularity-8.6-release.jar`.
2. Drop it into your Mindustry `mods` folder (desktop: `.../Mindustry/mods`; Android: `.../mindustry/mods`).
3. **Remove any older Singularity JAR** — this mod replaces the old version, it does not sit alongside it.
4. Requires **Mindustry v160.5** (that is what `minGameVersion` in `mod.hjson` says).

## Build from source

```bash
bash tools/setup_env.sh            # downloads the JDK, Mindustry.jar, server jar, R8 and android.jar
VENDOR=/path/to/vendor bash build.sh
```

Output: `dist/Singularity-8.6-release.jar`. `build.sh` compiles against `Mindustry.jar`, runs `d8` to produce
`classes.dex`, and packages both into one JAR.

## License

**GPLv3** — see [LICENSE](LICENSE).

## Changes

- **Voxel Forge:** existing Aurora Coolant, 4.8/sec; crafting time/output unchanged.
- **Water Treatment:** existing Tidewater, 21.6/sec → demineralized water, 19.2/sec.
- **Campaign:** 32 sectors. Starting sector 10 remains boss-free; all other 31 have a final boss. Twenty sectors have multiple encounters. Bosses have vanilla `StatusEffects.boss`, finite wave ranges and one explicit spawn gate.
- **Maps:** wider curved approaches, dry 7×7 gate staging areas, bounded ordinary unit counts scaled by gate count. The final encounter occurs before the engine's capture threshold, not one wave after it.
- **Tactical Commander:** deleted registration, implementation, dedicated AI package, tech node and localization. The separate Detainer unit and its UI remain.
- **Lumen Incinerator:** official `Incinerator` behavior, 2×2 footprint, 30 power/sec; destroys surplus items and incinerable liquids after warm-up.
- **Inverted Lumen Sorter:** official `Sorter`, `invert=true`; selected items go sideways, others straight. Configuration, serialization, alternating sides and chain prevention remain vanilla.
- **Ward Dome:** full 7000 shield on placement; 120/sec regeneration; full recovery after 14 powered seconds when broken. Uses official shield damage and crash-explosion contracts. Current/max shield and rebuild state are visible in the normal selected-block panel.

## Rendering

Both new utilities draw their entire completed body and placement preview as live procedural 3D geometry. No new body textures, baked body layers or sprite fallback at far zoom. Rotor clearance, finite/projected coordinates, animation and zero region draws are checked automatically. Engine trash/filter glyphs are used only for UI/construction identification.

Existing content retains its original asset pipeline; this release does not claim every pre-existing model is entirely live-rendered. The included animation preview is a software rasterization of recorded in-game draw calls, not a real-device screenshot or FPS measurement.

## 8.5.2-beta

Full report: [docs/REPORT-v8.5.2-beta.md](docs/REPORT-v8.5.2-beta.md).

| Item | What changed |
|---|---|
| Waves | Empty waves fixed. The spawner now slices the eligible roster into each wave by weight instead of multiplying group totals (which overflowed); every wave gets at least one unit, and credit weight is converted as a percentage. |
| Waste Transmuter | New 4x4 machine: **waste + Lumen Slurry + power** -> a random basic ore by weight (including Aurite, Lumenite and Silt), at most two kinds per craft. The panel lists the probabilities. |
| Star Marrow | No longer only for aether alloy: it is now also a **build cost** on load-bearing and armour blocks, several turrets and factories. No new ground ore - adding one breaks the whole tech tree. |
| Lumen Liquid Bridge | Rebuilt as a round valve body on a flange, with a sight glass, pipe stubs and a handwheel that turns only while liquid moves, plus a real 3D tube between the two ends. No longer mistakable for the flat-topped, belted Lumen Bridge. |
| Liquid Relay | Links can now be pinned by hand like the Skyline Cableway (up to 6, tap again to release); clearing the selection returns to automatic interconnection. Old saves are unaffected. |
| Anchor Spike / Spike Warden | A new turret and a new unit. Their rounds carry the engine's non-absorbable flag, so the Detainer's claws cannot catch them and cannot turn them into its own energy or reflected shots. Damage, range and reload stay in line with any other weapon this size and the Detainer's aegis still works - nothing is targeted. |
| Lattice Crawler | Upper body rebuilt (flat top, no clashing parts) and its own muzzle/smoke effects shrunk to one third. |
| Citadel Pilot | Model rebuilt, and the front spinner fixed: it was tumbling end over end around the wrong axis instead of spinning about its own drum axis. |
| Performance | Performance only - not one pixel changed: every baked sprite of all 100+ models is **byte-identical** to 8.5.1. The mod now has its own settings page holding the new performance options plus the old gravitational-lensing toggle. |


## 8.5.3-beta

Full report: [docs/REPORT-v8.5.3-beta.md](docs/REPORT-v8.5.3-beta.md).

| Item | What changed |
|---|---|
| Quasar Lance | **Muzzle alignment.** It is a continuous-beam turret, and the engine re-derives the beam origin from `(shootX, shootY)` every tick; `shootY = 0` pins the origin to the ground, so the beam looked like it came out of the floor under the turret. New `ContinuousTurret3D`: the muzzle's Z height is projected back to the screen through the camera perspective, and the start and end points move together, so range and heading are untouched. The muzzle height is read straight off the 3D model, so future barrel edits follow automatically. |
| Waves - every wave | Offline simulation over 8 configurations: **zero empty waves**. |
| Waves - every unit | Roster of 30 (28 hand-tuned + 2 auto-appended) plus an automatic scan so **units added later are never missed**. Only three kinds are excluded: non-attacking units, naval units (the Tide line) and the player chassis (one per core). The Detainer keeps its own separate group (exactly 2 per map). |
| Waves - high tier | T4 frequency **-25%** (spacing x1.33), T5 -47.5% (spacing x1.90); T3 stretched a little too. |
| Waves - difficulty halved | Measured average units per wave, 8.5.2 -> 8.5.3: beginner map 9 -> **4** (-56%), endgame map 31 -> **12** (-61%); peak 90 -> 38. **Still hard** - it just no longer crushes you in the opening waves. |
| Waves - beginner maps | Maps with threat < 0.34 (the first few campaign sectors) **generate no T5 groups at all** - T4 is the ceiling. The higher the threat, the earlier and denser T5 arrives. |
| Attributes | **Not one attribute was touched.** Only spawn frequency and spawn configuration. |

## 8.6-release

Full report: [docs/REPORT-v8.6-release.md](docs/REPORT-v8.6-release.md).
Combined all-versions PDF (8.5 -> 8.6): [docs/Singularity-8.5-8.6-release-中文汇总.pdf](docs/Singularity-8.5-8.6-release-%E4%B8%AD%E6%96%87%E6%B1%87%E6%80%BB.pdf) (7 pages, embedded CJK font).

| Item | What changed |
|---|---|
| Anchor Spike (3x3 turret) | Reload **84 -> 67.2** ticks (fire rate x1.25), damage **34 -> 153** (x4.5). **Build cost unchanged.** Range, health, power draw and the spear-volley firing pattern untouched. |
| Spike Warden - three guns | Main gun reload **72 -> 60** (x1.2), damage **27 -> 162** (x6). Two new variant guns fire **2.25x as fast** as the main gun for **58** and **84** damage - different from each other, both inside the requested 42-97 band. All three rounds keep `absorbable = false`, so the Detainer's claws still cannot catch any of them. Combined single-target DPS **805.5** (was 67.5). |
| Spike Warden - serpentine movement | The treads are gone. Now a vanilla **`CrawlUnit`** - the engine's only true "snake" - with a wedge head, six armoured sections and a row of belly scutes that take turns gripping the ground. **Two modelling references**: the engine's own `UnitType.drawCrawl` formula, plus real serpentine locomotion (a lateral wave travelling head to tail). |
| Spike Warden - health | **2350 -> 12000** (requested band was 8000-15000). Armor and speed untouched. |
| Spike Warden - cost | Priced as a **T5**: **58 s -> 130 s**, and it now needs **concordCores**. For scale: resonanceSiege 105 s, auroraHeavy 120 s, prismMonolith 150 s, bastionWarden 160 s. |
| Bug found and fixed along the way | The Spike Warden's **production plan has been broken since v8.5.2**: `AureliaFrontier.load()` built the plan before `AnchorContent.load()` created the unit, so `UnitPlan.unit` was always null (the engine dereferences it all over `UnitFactory`). The unit could never actually be built. The plan is now registered after the unit exists. |

The serpent body is **integrated along the spine**: each section's yaw is computed first, then the rig walks forward one `spacing` per step, turning on the shortest-arc bisector. Consecutive sections are therefore always exactly one step apart and the body cannot pull apart. The first version placed each section independently and measured gaps up to **8.5 design units** (about 1 tile) at the tail; `tools/preview/src/preview/SerpentProbe.java` now sweeps 720 wave phases x 13 bend angles x 4 headings and reports the worst plate-edge gap as **-0.047** (negative = overlapping) with no NaN and no self-intersection. See `docs/v86/serpent-probe.txt`.

## 8.5.6-beta

Full report: [docs/REPORT-v8.5.6-beta.md](docs/REPORT-v8.5.6-beta.md).
Combined all-versions PDF (8.5 -> 8.5.6): [docs/Singularity-8.5-8.5.6-beta-中文汇总.pdf](docs/Singularity-8.5-8.5.6-beta-%E4%B8%AD%E6%96%87%E6%B1%87%E6%80%BB.pdf) (5 pages, embedded CJK font).

| Item | What changed |
|---|---|
| Quasar beam - sound stops on time | The loop kept playing long after the beam was out and even after the turret was destroyed. Three causes stack: `shouldActiveSound()` only checks *there is still a bullet*, and `ContinuousTurret.updateBullet()` pins `bullet.time` while `isShooting` (lifetime 80, so the bullet lingers for dozens of ticks); `SoundLoop.fadeSpeed` is hardcoded `0.05`, so it needs another 20 ticks to actually stop; and `SoundLoop` is only driven from `updateTile()`, so once the entity stops updating nothing stops it. Fixed by adding *is actually firing* to the condition and calling `soundLoop.stop()` immediately, plus safety stops in `remove()` / `onDestroyed()`. **Damage, range, power, fire rate and beam length are untouched.** |
| Star marrow ammo - 2 turrets only | Marrow now loads only on **`accretionCannon`** (165 + 90 splash, unchanged) and **`eventHorizon`** (new `marrowLanceHeavy`, **320** + 190 splash). `frameDragger` and `shardVolley` dropped. Same ballistics as before - speed, range, 4 shots per item, effects and the brand are identical; only the warhead charge differs. |
| Singularity core shell | `collapsarShell` damage **120 -> 150**, splash **180 -> 210**. Radius 60 px, speed, lifetime, ballistics, black-hole mechanics and effects unchanged. Only referenced by the event horizon cannon, so nothing else moves. |
| Map size 494x494 .. 600x600 | Campaign sectors were 272..348, now **494..600**; non-campaign default **494**. 494^2 = 244,036 tiles (3.3x) up to 600^2 = 360,000 (3.0x). Ore vein targets, `ensureOre` / `ensureWater` floors and the attempt cap are all scaled by `areaScale(w,h) = (w*h)/(310*310)`, otherwise ore would be too sparse to open on. Measured on **all 32 campaign sectors**: sizes all within 494..600, **0 maps missing an ore**, **0 maps with no spawn gate**, 1.7-3.5 s per map. A single 594x594 map generates fine at `-Xmx320m`. |
| Spawn strength x1.25, count x1.2 | `unitScaling` is *waves needed per +1 unit*, so smaller = faster growth; growth x1.25 means multiplying it by 1/1.25 = 0.8, ramped in **linearly with threat**. `growthScale(d) = 1f / Mathf.lerp(1f, 1.25f, clamp(d))` - d=0 (beginner) multiplies by 1.0, d=1 (endgame) by 0.8. **The slope changes, not the starting point.** `COUNT_BOOST = 1.2f` applies to `g.max`, `g.unitAmount` and the `AureliaCampaign.balance()` cap. Measured: beginner **x1.000**, endgame **x1.193**. |
| Coverage verified | Roster 30 slots, **31 distinct units appear** (31 > 30 because Detainer joins as a separate boss group at threat >= 0.55) -> **no unit is left out**. **0 empty waves** across 8 threat levels / 296 waves. |

## 8.5.5-beta

Full report: [docs/REPORT-v8.5.5-beta.md](docs/REPORT-v8.5.5-beta.md).
Chinese summary PDF: [docs/Singularity-8.5.5-beta-中文汇总.pdf](docs/Singularity-8.5.5-beta-%E4%B8%AD%E6%96%87%E6%B1%87%E6%80%BB.pdf) (4 pages, embedded CJK font).

| Item | What changed |
|---|---|
| Quasar Lance - fixed | It could not fire at all and showed no firing effect. The cause was **muzzle geometry, not damage or power**: the lift used a *world* Y against a *turret-relative* `camCy`, throwing the beam 268 tiles away. Rewrote `ContinuousTurret3D.updateBullet` with the correct relative-offset projection, measure the beam length **from the muzzle** (so it no longer grows every tick), and replay a new muzzle effect. Damage, range, power and fire rate untouched. |
| Lattice Crawler - effects rewritten | The 8.5.4 "1/3 size" could never have worked: `detainImpact` uses `e.rotation` (a 0-360 heading) **as a radius term**, so the real peak radius was ~390 px, or ~130 px after /3 - and it *varied with the direction the shell flew*. New `CrawlerFx` is written from scratch with rotation-independent constants. Measured peak radius: impact **129.7 -> 9.6 px (13.5x smaller)**, muzzle 2.0 -> 1.6, smoke 3.9 -> 2.8, shard hit 14.0 -> 1.7. |
| Star marrow as ammo | Registered on **4 turrets** (Accretion Cannon, Frame Dragger, Event Horizon, shardVolley). Damage **165** + 90 splash in a 26 px radius, range 300 px, 4 shots per unit of marrow. |
| Marrow brand (buff) | New status effect, 6 s: ~45 dmg/s burn (~270 total), -38% speed, -28% damage, -35% reload. Total expected damage per shot ~525. |
| Marrow attack effects | Entirely original (`MarrowFx`): a collapsing six-point star muzzle, an ember wake, an eight-point burst with a lingering dark scar on impact, and rising embers while the brand burns. Measured peak radii 5.4 - 31.1 px, all rotation-independent. |
| How the numbers were measured | Two independent methods: a headless recording `Batch` that collects every vertex across the full lifetime at 5 different headings, and a PNG rasteriser that measures the lit pixels. They agree (9.6 vs 10.6 px for the crawler impact). See `docs/fx-sizes-8.5.5.txt` and `docs/quasar-geom-8.5.5.txt`. |

> Reference docs claimed `StatusEffect.armorMultiplier` exists. It does **not** in v160.5 -
> `javap` on the actual jar shows only `damage`, `intervalDamage`, `speedMultiplier`,
> `damageMultiplier`, `reloadMultiplier` and friends. The brand uses fields that really exist.

## 8.5.4-beta

Full report: [docs/REPORT-v8.5.4-beta.md](docs/REPORT-v8.5.4-beta.md).
Chinese summary PDF of **everything from 8.5 through 8.5.4-beta**:
[docs/Singularity-8.5-8.5.4-beta-zh.pdf](docs/Singularity-8.5-8.5.4-beta-%E4%B8%AD%E6%96%87%E6%B1%87%E6%80%BB.pdf) (4 pages, embedded CJK font).

| Item | What changed |
|---|---|
| Lattice Crawler - impact explosion | Shrunk to **1/3**. It was using `detainImpact`, an effect written for the Anchor Spike's large-calibre shell (26-tile splash); on a stubby mortar the fireball was nearly as wide as the hull. New `detainImpactSmall` divides every length by 3 and leaves blending, colours, duration and timing untouched. **This is not the 8.5.2 change** - that one shrank the muzzle flash; this one is the shell's impact blast. |
| Lattice Crawler - shell speed | Speed **x1.25** (2.6 -> 3.25) with lifetime /1.25 (84 -> 67), so reach stays 217.75 ~= 215 - **faster shells, same range**, easier to hit moving targets, no change in unit strength. |
| Waste Transmuter | **Twice as fast** (110 -> 55 ticks) and now **outputs star marrow**: Lumenite 36% / Aurite 28% / Silt 23% / Resonance Shard 8% / **Star Marrow 5%**. Up to two rolls, so ~9.75% of crafts yield marrow. Prism Glass, Prism Alloy and Concord Core are deliberately excluded - those are finished materials. |
| Detainer | Production cost gained **10 star marrow**, and the 5x5 fabricator itself costs 6 more. Note: v160.5 `UnitType` has no `requirements` field at all - a unit's production cost lives entirely in the factory's `UnitPlan`. The vanilla-material line was the *fabricator block's* build cost, and it had already been converted to Aurelia materials. |
| Star marrow, round two | **22 blocks and 13 unit production plans** now use star marrow (6 in 8.5.3). One-off build costs only - no new consumers, outputs, power draw or research costs. **No new ground ore**, so existing saves need no restart. |

## Install / migrate

Back up saves **and campaign data**. Replace the old JAR; do not load both versions. Import `Singularity-v8.5-beta.jar` through Mindustry's mod menu and restart. The ZIP is a source/report package.

Old commander buildings become air through the engine's missing-block loader; no hidden replacement block or refund is installed. Intact v8.3 domes receive a one-time full-shield repair; existing rebuild timers are retained. New-format saves preserve exact shield damage.

Active old campaign saves receive an idempotent boss schedule after `SectorInfo` restoration, preserving progress/resources. Captured sectors stay captured. Old terrain and player buildings are not excavated. Unsafe natural boss approaches use a safe existing gate or a shielded flying fallback with a comparable nominal health budget.

## Build and test

```bash
export VENDOR=/path/to/external/toolchain
bash tools/setup_env.sh v160.5
SKIP_BAKE=1 bash build.sh
# dist/Singularity-v8.5-beta.jar
bash test_server.sh --mode integration
bash test_server.sh --mode migration --baseline /path/to/original-v8.3.zip
```

Use the original baseline JAR or its ZIP (a `.zip.txt` extension is also supported) for the optional migration test. JDK 17, official desktop/server 160.5, R8 and an Android platform JAR are external dependencies. Python 3 drives the server tests. Without `SKIP_BAKE=1`, the build re-bakes existing model assets; the two live-only utilities are explicitly excluded from baking.

The released JAR never includes test instrumentation. Reports include raw server logs, all-sector CSV, generated save fixtures, geometry tests and artifact hashes. Obsolete release reports, map snapshots and restoration-era test drivers were removed.

**Limits:** no desktop GPU/Android hardware run or full-length campaign balance playthrough was performed. Headless/render-path checks are not a universal bug-free guarantee.

## Credits / licence

Original package attribution retained: **sayed1145** — concept, architecture and test direction; **NLM** — AI-assisted engineering. No new real-device testing by the original author is implied.

GPL-3.0, see [LICENSE](LICENSE). Official behavior references are pinned to Mindustry v160.5.
