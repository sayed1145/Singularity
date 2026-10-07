# Singularity · 8.4-beta

An upgrade of the supplied **8.3-beta** source package for **Mindustry v160.5**.
The universal JAR contains desktop classes and Android DEX. Internal mod ID: `blackhole`.

**[中文完整说明](README_zh.md)** · [Test/change report](docs/REPORT-v8.4-beta.md) · [Offline model preview](docs/v84/models.html)

## Distribution

- **Source and reports:** this repository and GitHub's automatically generated source archives. They contain **no JAR, DEX or compiled classes**.
- **Binary download:** [Singularity-v8.4-beta.jar — Release 8.4-beta](https://github.com/sayed1145/Singularity/releases/tag/8.4-beta).
- The JAR SHA-256 is recorded in the release notes and `docs/v84/release-audit.json`. Download it separately when using a source archive without rebuilding.

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

## Install / migrate

Back up saves **and campaign data**. Replace the old JAR; do not load both versions. Import `Singularity-v8.4-beta.jar` through Mindustry's mod menu and restart. The ZIP is a source/report package.

Old commander buildings become air through the engine's missing-block loader; no hidden replacement block or refund is installed. Intact v8.3 domes receive a one-time full-shield repair; existing rebuild timers are retained. New-format saves preserve exact shield damage.

Active old campaign saves receive an idempotent boss schedule after `SectorInfo` restoration, preserving progress/resources. Captured sectors stay captured. Old terrain and player buildings are not excavated. Unsafe natural boss approaches use a safe existing gate or a shielded flying fallback with a comparable nominal health budget.

## Build and test

```bash
export VENDOR=/path/to/external/toolchain
bash tools/setup_env.sh v160.5
SKIP_BAKE=1 bash build.sh
# dist/Singularity-v8.4-beta.jar
bash test_server.sh --mode integration
bash test_server.sh --mode migration --baseline /path/to/original-v8.3.zip
```

Use the original baseline JAR or its ZIP (a `.zip.txt` extension is also supported) for the optional migration test. JDK 17, official desktop/server 160.5, R8 and an Android platform JAR are external dependencies. Python 3 drives the server tests. Without `SKIP_BAKE=1`, the build re-bakes existing model assets; the two live-only utilities are explicitly excluded from baking.

The released JAR never includes test instrumentation. Reports include raw server logs, all-sector CSV, generated save fixtures, geometry tests and artifact hashes. Obsolete release reports, map snapshots and restoration-era test drivers were removed.

**Limits:** no desktop GPU/Android hardware run or full-length campaign balance playthrough was performed. Headless/render-path checks are not a universal bug-free guarantee.

## Credits / licence

Original package attribution retained: **sayed1145** — concept, architecture and test direction; **NLM** — AI-assisted engineering. No new real-device testing by the original author is implied.

GPL-3.0, see [LICENSE](LICENSE). Official behavior references are pinned to Mindustry v160.5.
