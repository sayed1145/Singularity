# Singularity v8.5-beta — repair and validation report

**Target:** Mindustry **v160.5** · **Universal artifact:** `Singularity-v8.5-beta.jar` · **Upstream baseline:** `sayed1145/Singularity` commit `f72cacf179f2bd024be343dd89dc54ef0ee056b7`.

## Scope completed

| Requested repair | v8.5-beta implementation | Runtime validation |
|---|---|---|
| Aether-alloy / Aether Press loop | Aether Press no longer costs Aegis Alloy; Aurelia tree order is **Hawking Dust → Aether Press → Aegis Alloy**. | Official server checks the build bill and both parent links. |
| Signal Core / Signal Encoder loop | Tree order is **Aegis Alloy → Signal Encoder → Signal Core**. | Official server checks both parent links. |
| Hawking Condenser coolant | The pre-init liquid consumer is rebound from vanilla cryofluid to `blackhole-aurora-coolant` at 0.12/tick once Aurelia content exists. | Official server inspects the live `ConsumeLiquid`. |
| Silicon research on Aurelia | Explicit legacy research costs were cleaned; final Aurelia tree additionally removes silicon defensively before pricing nodes. Production/build bills are remapped independently to Aurelia items. | Every loaded Aurelia research node is asserted non-silicon. |
| Horizon Assembler / Singularity Yard free unlocks | Both receive explicit nonempty research bills containing only mod materials. Horizon follows Singularity Core; Yard follows Horizon Assembler. | Official server requires a mod-only cost and verifies parents. |
| Lumen Incinerator / Inverted Lumen Sorter detail visuals | Added authored PNG normal, preview, icon and HD textures; replaced font-glyph UI icons and procedural plan rendering with atlas sprites. World machinery remains animated. | Archive audit verifies all six required normal/preview/icon PNGs; desktop footage shows both actual detail panels. |

## Build and compatibility

- `mod.hjson`: version `8.5-beta`, `minGameVersion: "160.5"`.
- Build script compiles Java 17 desktop classes, packages assets, runs D8 with Android API 35, then emits one JAR carrying both desktop `.class` files and Android `classes.dex`.
- Final JAR audit: **806 desktop classes**, **1,220,484-byte DEX**, CRC pass, source/class/asset byte match, no injected tests, and raster utility assets present.
- Rebuild:

```bash
VENDOR=/path/to/vendor bash tools/setup_env.sh v160.5
VENDOR=/path/to/vendor SKIP_BAKE=1 bash build.sh
VENDOR=/path/to/vendor python3 tests/run.py --mode integration --jar dist/Singularity-v8.5-beta.jar
VENDOR=/path/to/vendor python3 tests/audit_release.py dist/Singularity-v8.5-beta.jar
```

## Validation evidence

- **Official v160.5 headless server:** PASS, **7,389 assertions** — `v85/HEADLESS-TEST-SUMMARY.md`, `v85/integration.log`.
- **Universal JAR audit:** PASS — `v85/release-audit.json`.
- **Actual official desktop-client video and screenshots:** `v85/evidence/` and `v85/DESKTOP-CAPTURE.md`.
- SHA-256 values for final binary and evidence: `v85/SHA256SUMS.txt`.

## Delivery note

The supplied archive was a Godot project containing an explicitly non-buildable source snapshot. This release was reconstructed from the snapshot’s documented upstream source commit. The source/report package therefore includes the modified upstream tree, generated release reports and the universal JAR; it does not overwrite or alter the supplied attachment.
