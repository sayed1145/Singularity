# Singularity · 8.8-Release

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
> Releases stay downloadable in the meantime. The last one is **8.7.5**.

## Download

- **Binary (recommended):** [`Singularity-8.8-Release.jar`](https://github.com/sayed1145/Singularity/releases/tag/8.8-Release)
  — attached to the release. **One JAR for both desktop and Android** (it contains Java `.class` files for the
  desktop JVM **and** a `classes.dex` for Android/ART).
- **Source:** this repository, or GitHub's automatically generated source archives.

### Install

1. Download `Singularity-8.7.3.jar`.
2. Drop it into your Mindustry `mods` folder (desktop: `.../Mindustry/mods`; Android: `.../mindustry/mods`).
3. **Remove any older Singularity JAR** — this mod replaces the old version, it does not sit alongside it.
4. Requires **Mindustry v160.5** (that is what `minGameVersion` in `mod.hjson` says).

## Build from source

```bash
bash tools/setup_env.sh            # downloads the JDK, Mindustry.jar, server jar, R8 and android.jar
VENDOR=/path/to/vendor bash build.sh
```

Output: `dist/Singularity-8.7.3.jar`. `build.sh` compiles against `Mindustry.jar`, runs `d8` to produce
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

## 8.7.7

**All crawl, no dive.** The rim stays at 0.1 tiles/s — nothing about the outer approach changes — but the
innermost tile now caps at **0.4 tiles/s instead of 1**. From the far rim to the very edge of the horizon,
the whole absorption profile is one continuous slow ramp: every captured thing spends at least two and a half
seconds crossing a single tile, even at its fastest. Worst-case fall time is unchanged (290 s, rim-limited);
the mean capture-to-core time grows from 72 s to 130 s. Content totals frozen (610 / 45 / 110), headless
audit green, save/reload ok.

## 8.7.7

**All crawl, no dive.** The rim stays at 0.1 tiles/s; the innermost tile now caps at **0.4 tiles/s instead
of 1**. The full profile, rim to horizon, is one slow monotone ramp - even the fastest capture spends at
least 2.5 s per tile. Worst-case fall time is unchanged (290 s, rim-limited); mean capture-to-core time
grows from 72 s to 130 s. Content frozen (610 / 45 / 110), audit green. Full write-up:
[docs/REPORT-v8.7.7.md](docs/REPORT-v8.7.7.md) with eight Chinese academic charts under
[docs/v877/charts](docs/v877/charts).

## 8.7.6

**A gentler fall.** The 8.7.5 profile (1 → 3 tiles/s) still yanked anything crossing the inner ring at three
tiles a second. This release re-tunes the curve for readability instead of raw power: **0.1 tiles/s at the rim
— a slow visible crawl — ramping monotonically, tile by tile, to the unchanged 1-tile/s hard cap at the core**.
Nothing accelerates past the cap, and the outer crawl is fast enough that no capture is ever a permanent stall.
Content totals are frozen again (610 / 45 / 110) and the headless audit re-runs green, including the live pull
probe (unit dragged in from the outer ring and absorbed) and the full 8.7.x regression set.

## 8.7.5

*The 8.7.5 report, charts and demo video were retired per the no-old-artifacts policy and ship with the
8.7.5 release only.* Runtime audit history for that version was measured on a **real v160.5
dedicated server** (`bash tools/test_v875.sh`). The load-time self-check now asserts the OPPOSITE for the well:
if `blackhole-gravity-well` can be found at all, the server refuses to start.

| What | What changed |
|---|---|
| **Gravity well - deleted outright** | Not hidden, not disabled - **removed through the whole chain**: block class and build logic, registration, 3D model, tech node, star-marrow add-on cost, both localization bundles. Registered blocks 611 -> **610** (measured). The aegis lattice re-hangs directly under the arc-mortar node, so the tree never breaks. Wells in old saves simply disappear, as the engine does for any removed block. |
| **Black hole - one more nerf** | Absorption is now **1 tile/s at the outermost rim** (was 2) and a flat **3 tiles/s inside the innermost tile** (was 7). Dragging from the rim to the horizon of a fully grown field takes **16.0 s on average, 29.0 s at the worst** (8.7.4: 14 s). |
| **Black hole - nothing else touched** | The per-tick ramping tear at the horizon, buildings-only damage (never absorption), the growing field radius, bullet devouring and the travelling micro-hole all stay exactly as in 8.7.4 - "其他就不削了". |
| **Demo video** | At the time, `docs/v875/demo.mp4`: the real v160.5 desktop client (software-rendered under Xvfb, not a mocked animation) - an event horizon lobbing holes into an attacking wave while units crawl out of the 1-to-3-tiles/s pull. Not included in this package. |
| Version | `8.7.5` - `mod.hjson`, jar name `Singularity-8.7.5.jar`. |

## 8.7.4

*The 8.7.4 report and charts were retired with that version.* Every number below was measured on a **real
v160.5 dedicated server** (`bash tools/test_v874.sh`), not estimated. The mod also **checks itself** at load
time (`src/blackhole/V873Audit.java`): a wrong constant stops the server from starting.

| What | What changed |
|---|---|
| **Gravity well - overdrive deleted** | The 8.7.3 "overdrive field" is **gone**: the `speedBoost` field and its per-frame velocity loop were removed, and the self-check now refuses to start if the field ever comes back. The well has **zero effect on friendly units** - every scan it runs only ever looks at enemies. |
| **Gravity well - absorption profile** | **1 tile/s at the outermost rim**, rising linearly inwards, and a flat **maximum of 5 tiles/s inside the innermost tile** (8 px). 5 tiles/s is the absolute cap everywhere (8.7.3 reached 15). Worst case from rim to mouth: **15 s**. Anything faster than the local pull can still break out. |
| **Gravity well - no more one-shots** | The "instant kill at the centre" was three things at once: a 15 tiles/s core pull nothing could outrun, an arrival radius of 0 (victims were pinned flush against the well wall, standing inside the 24 px crush ring), and a flat 55/s grind that erased 110-health units in two seconds. |
| **Gravity well - rebuilt on the black hole's mechanism** | Arrival radius is now the crush ring itself (the hole's "horizon" treatment) - units get dragged to the ring, never shoved into the well; the ring shrank 24 -> **16 px (2 tiles)**; damage is the hole's per-tick `damagePierce` style, ramping **0.4x -> 1.0x** towards the centre, weakened 55 -> **40/s**; plus a fuse - **the crush can never exceed 10% of a unit's max health per second**, so even a unit parked at the exact centre survives at least 10 seconds and can fly out. Big bosses stay crush-immune, exactly like under the hole. |
| **Black hole - absorption profile** | **2 tiles/s at the outermost rim** (was 3), a flat **7 tiles/s inside the innermost tile** (was 20), same plateau normalization as the well. The field still grows with the horizon; buildings are still only damaged, never absorbed. |
| Version | `8.7.4` - `mod.hjson`, jar name `Singularity-8.7.4.jar`. |

## 8.7.3

Full report: [docs/REPORT-v8.7.3.md](docs/REPORT-v8.7.3.md) (Chinese, with six charts).
Runtime audit: [docs/v873/audit.txt](docs/v873/audit.txt) - every number below was measured on a **real
v160.5 dedicated server** (`bash tools/test_v873.sh`), not estimated. The mod also **checks itself** at
load time (`src/blackhole/V873Audit.java`): a wrong constant stops the server from starting.

| What | What changed |
|---|---|
| **Gravity well - now a slow, cyclic absorber** | It used to swallow units on contact. Now it only wakes up when a target **enters the field**, absorbs for **8 seconds**, and then **cools down** (no absorption while cooling, and none while idle). While it is active it keeps absorbing **every** unit that enters the field - including **flying units**, which the old version skipped entirely. |
| **Gravity well - absorption speed** | **4 tiles/s at the rim**, gradually rising towards the mouth, **15 tiles/s in the centre**. Dragging a unit in from the edge takes **more than a second and a half** - nothing is ever swallowed in one frame. |
| **Gravity well - escape is possible** | The field only carries the unit, it **no longer zeroes the unit's velocity** - the unit's own speed is added on top of the drag, so anything that moves **faster than the current absorption speed can break free**. |
| **Gravity well - overdrive** | The well is also an **overdrive field**: the **friendly** units around it are always **50% faster**, and that keeps working **during the cooldown** (the overdrive and the absorption cycle are independent). |
| **Gravity well - damage** | Crush damage 95 -> **55/s**, and it only starts once the unit has actually been dragged to the centre. |
| **Black hole - its own mechanism** | The field radius is **not a fixed number**, it **grows with the horizon** (up to about 29 tiles), and the absorption is **slow** as well: **3 tiles/s at the outer rim**, rising all the way to a full **20 tiles/s in the very middle**. A unit that outruns the absorption can break out. Buildings are **only damaged, never absorbed** (enemy buildings only - friendly buildings are never touched). |
| **Industrial waste - firing effects** | The muzzle and hit effects **lost the additive white-out**, the hit effect no longer has a white-hot core flash, both are far shorter lived, and the trail was shortened. Both effects are **pinned to the map layer** (`Layer.effect`), so they no longer wash out the map or the interface. |
| Version | `8.7.3` - `mod.hjson`, jar name `Singularity-8.7.3.jar`. |

## 8.7.2

Full report: [docs/REPORT-v8.7.2.md](docs/REPORT-v8.7.2.md) (Chinese, with six charts).
Headless audit: [docs/v872/audit.txt](docs/v872/audit.txt) - every number below was measured on a real
v160.5 dedicated server (`bash tools/test_v872.sh`), not estimated. The mod also checks itself while the
server loads (`src/blackhole/V872Audit.java`): a wrong number makes the server refuse to start.

| Item | What changed |
|---|---|
| **Industrial waste loading (a misread, now fixed)** | 8.7.1 turned "load x5" into `reloadMultiplier = 5` - in the Chinese panel that is the **rate of fire**, not the load. 8.7.2 sets `ammoMultiplier = 5` (**one unit of waste is five shells**, the panel's "load multiplier" really shows x5) and puts `reloadMultiplier` back to **1**: waste adds **no** extra fire rate any more. |
| Sky Net lumenite shell | Base damage **5 -> 13** (lead is priced as lumenite on Aurelia, so the Sky Net's lead shell is the lumenite shell). The glass shell stays at 21. |
| Lumen Accelerator | The **overdrive is back**: `speedBoost 1 -> 1.5`, the buildings around it get **+50%**. It still cannot be boosted itself (no booster slot, no pulse), and the panel shows the truth: **+50% / 150%**. |
| Pyroclast Titan - body | **Rolled back to the original**: the body effect layer 8.7 added (heat fins / ember halo / magma core, `PyroclastFx`) is deleted, and the Pyroclast is a plain `Walker3D` again. The **attack effects are untouched** (still the 6.1 `legacyBurst` / `legacyVent`). |
| Black Hole - gravity field | The black hole now has a **real gravity field**: `pullRadius 230 px ~ 29 tiles`, and units inside it are **carried** into the event horizon (velocity zeroed, lifted over obstacles) - the binding follows [wither-storm](https://github.com/sayed1145/wither-storm)'s `StormLift`, not a kick the units can shrug off. It also has a **splash attack**: it chews on the **enemy buildings** around it (friendly buildings are never touched). |
| Gravity Well | Binds units for real (same wither-storm style), range **14 -> 15 tiles** (112 -> 120 px), and it **only ever touches units - never buildings**. |
| Version | `8.7.2` - `mod.hjson`, jar name `Singularity-8.7.2.jar`. |

## 8.7.1

Full report: [docs/REPORT-v8.7.1.md](docs/REPORT-v8.7.1.md) (Chinese, with six charts).
Headless audit: [docs/v871/audit.txt](docs/v871/audit.txt) - every number below was measured on a real
v160.5 dedicated server (`bash tools/test_v871.sh`), not estimated. The mod also checks itself while the
server loads (`src/blackhole/V871Audit.java`): a wrong number makes the server refuse to start.

| Item | What changed |
|---|---|
| **Cargo station crash (Android)** | Tapping the Lumen Cargo Station used to kill the whole game: `IllegalArgumentException: max must be > min. min,max: 58.0, 8.0`. Arc's `Table.slider` signature is `slider(min, max, step, defvalue, listener)` - the **current value comes last** - but 8.7 passed the radius as the minimum and the lower bound as the maximum. The call is now `table.slider(8f, 400f, 1f, rangeTiles, ...)`, with a `min < max` guard around it. |
| Version | `8.7.1` - `mod.hjson`, jar name `Singularity-8.7.1.jar`. |
| Bastion Railgun | Range **32.5 -> 45 tiles** (260 -> 360 px). |
| Lumen Accelerator | **No boost at all any more**: `speedBoost = 1f`, and the panel lines `Stat.speedIncrease` / `Stat.boostEffect` plus the boost bar are removed, so it no longer advertises a boost it does not give. |
| Citadel | The spray rounds hit for **45** instead of 40 (both rapid-fire weapons). |
| Industrial waste shells | Split in two: the **Halo** keeps 40 damage, the **Sky Net** drops to **20**; both reload **x5** (was x1.5) and no longer reduce building damage (80% -> 100%). |
| Lumen Spark | Default damage **18 -> 36** (x2). |
| Glass shell | The Sky Net's metaglass shell and its five shards go **7 -> 21** (x3). |
| Pyroclast Titan | Attack range cut to **60 tiles** (the panel said 77.2 while the unit itself could only reach 20.5); the main shell's lifetime drops 84 -> 65.4 frames so gun, unit and panel agree. The 6.1 attack effects are untouched. |
| Lumen Hauler | **Removed** - unit, 3D model, tech tree, bundle text and sprites. Networks scale by adding hubs now. |
| Old projects | The RBMK project is **deleted** (not shipped); the previous enrichment-plant files moved into the release zip under `previous-work/`. |

## 8.7-release

Full report: [docs/REPORT-v8.7-release.md](docs/REPORT-v8.7-release.md) (Chinese, with seven charts).
Headless audit: [docs/v87/audit.txt](docs/v87/audit.txt) - every number below was measured on a real
v160.5 dedicated server (`bash tools/test_v87.sh`), not estimated.

| Item | What changed |
|---|---|
| Pyroclast Titan - attack effects | The 6.1 attack effects are back **verbatim**: `src/sixfold/LegacyEmber.java` is a character-for-character copy of the 6.1 `src/sixfold/EmberEffects.java` (`vent` / `burst` / `mote` - same colours, stroke counts, lengths, lifetimes and easing). The main gun uses `legacyBurst`, the flak and the sweepers use `legacyVent`. Only the **body** halo was re-made (`PyroclastFx.java`), because it has to fit the 8.x 3D hull. The v3.2 turrets keep their 7.1+ pseudo-3D effects. |
| Coolant recipes | The Horizon Assembler (0.12/t) and the Citadel Yard (0.3/t) drank **vanilla cryofluid**, which does not exist on Aurelia - the recipes were dead. They (and the Hawking Condenser) now drink **Aurora Coolant**. |
| Resonance Sifter | `drillTime 66 -> 50`: **3.64/s -> 4.8/s**, and with tide water **10.8/s**. It also got the 10-unit tank it never had, so its 1.5x booster (2.25x on the panel) actually works now. The auger, the prism harvester and the rift collector got their missing tanks too. |
| Lumen Extractor | The empty coolant tank is gone (`hasLiquids = false`) - this drill only eats ore. |
| Lumen Accelerator | No booster slot, no pulse boost: `speedBoostPhase 0.75 -> 0`. The panel used to claim **225%** while the real value was 150%; it now honestly shows **150%**. |
| Deleted content | **Supply Post, Buffer Post, Logistics Vault and Lattice Link are gone** - code, 3D models, tech tree, bundle text and sprites. The Anchor Spike now hangs on the Lattice Crawler instead. |
| Lumen Cargo Station | **100 of every item** (was 120 for everything combined; the total is now 2000 over 20 item types). Selecting it draws the transport radius and highlights every node inside it. The config panel has a radius slider (8-400 tiles, default 58 - unchanged), a **long-range** switch (600 tiles) and a **dispatch now** button. The settings are packed into one integer and survive a save/reload. |
| Event Horizon Cannon | Star Marrow used to slow the turret down (0.85x). It now **speeds it up by 200%** (`reloadMultiplier = 3.0`, i.e. x3 fire rate). |
| Ward Dome | A resonance shard now lifts the ceiling to **12000** (7000 + 5000) and the bank no longer collapses back to 7000 when the charge window ends - a `deepBank` flag is written to the save file. Regen is x2.5 while the deep bank is up. |
| Industrial Waste as ammo | Waste loads as a shell on the **Halo** and the **Sky Net**: 40 damage, 58 splash, **434 px (54 tiles) range**, homing 0.13, **reload x1.5**, one item = six shells, and it bursts in a **fission** effect. The Halo's description was rewritten (it still described the pre-Aurelia version). |
| Wall Bore | `drillTime 190 -> 96` -> **1.25 items/s** (was 0.63/s). It also got the 10-unit tank, so its 2.1x plasma boost works for the first time. |
| Citadel | Five damage values up: spray **30 -> 40**, collapsar **90 -> 110** (splash **140 -> 150**), shell **70 -> 80** (splash **55 -> 60**). The citadel got its own shell types, so the accretion cannon and the turrets keep theirs. |
| Units actually mine | `UnitType.mineItems` only ever listed the four vanilla ores, so mod units (the Mote, ...) had nothing to mine and the "mine" stance did nothing. Every mining unit now gets a list filtered by its `mineTier` - no ore has to be picked by hand. |

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
