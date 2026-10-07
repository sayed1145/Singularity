# Singularity (奇点)

**Industrial Junk Beta** — a large content mod for [Mindustry](https://github.com/Anuken/Mindustry) v160.5,
built around one planet: **Aurelia**.

Everything in this jar — seven merged content packs, every block, unit, item and liquid — lives on Aurelia,
appears in its tech tree, and is re-priced in Aurelia resources. All block and unit models are **generated
procedurally at runtime in 3D**: there are no hand-drawn sprites for machines, turrets rotate freely, nothing
floats and nothing overlaps.

* Version: **8.3-beta**
* Target game: **Mindustry v160.5** (`minGameVersion: 160.5`)
* One jar for **desktop and Android**
* Licence: **GPL-3.0** (see `LICENSE`)

中文说明见 [README_zh.md](README_zh.md)。

---

## Credits

| Who | Role |
|-----|------|
| **sayed1145** | Human author — the idea, the concrete architecture design, balance direction, feature specs, and all real-device testing (Android, low-spec hardware). Every design call in this mod is his. |
| **NLM** | AI engineering and implementation — writes the code to the given design, builds the toolchain, runs the headless test suites. Assistive role. |

Built on Anuken's Mindustry; vanilla classes and behaviour referenced under the game's own licence.

---

## What is in it

**One planet, one tree.** Aurelia has its own terrain generator (domain-warped fBm + hydraulic erosion +
flow-accumulation rivers), 32 naturally generated campaign sectors, its own ores, floors, walls and weather.
Vanilla content is hidden there; mod content is hidden everywhere else.

**Seven packs merged:** the Singularity line (black hole, graviton and Hawking chain), the Aether titan
matrix, the Aurelia expedition, the Detainer, the RBMK-style white reactor, voxel industry and voxel
frontier. Only recipes were changed when merging — attributes and stats are untouched.

**Logistics without belts.** A drone network with Factorio-style node roles (active provider, passive
provider, requester, storage, buffer), hunger-ranked scheduling, wireless liquid relays, overflow and
underflow gates, liquid tanks and a logistics vault.

### New in 8.3-beta

| Area | Change |
|------|--------|
| **The waste cycle** | Industrial waste is now a **by-product**: the silt kiln, the plasma mixer and the prism press each drop one unit on top of their normal output. Waste is worth nothing by itself - it has to walk a **six-machine chain**: reclaimer (waste → swarf + dust) → swarf furnace (swarf → aurite) and leach tower (dust + water → lumen slurry) → crystalliser (slurry → lumenite) → resynthesiser (slurry + lumenite + aurite → resonance shard) → prism reformer (shard + glass + slurry → prism alloy). Every step loses mass, so the loop can never out-run the factories feeding it. A **waste silo** (900, waste only) buffers the line. |
| **Tier-3 core: Aurelia Citadel** | 6x6, built over the Bastion: more storage, a higher unit cap, heavy armour, and it launches the new **Citadel Pilot** - a twin-ducted-fan lifter with a cutter head, printable in the Aurelia Fabricator. |
| **Core mining tiers** | No core can mine every ore grade any more. The lumen and bastion pilots work ground ore (the bastion is the fastest miner), the citadel pilot is the only one that can cut **wall seams**, and **no pilot of any tier can mine resonance** - that always needs a tier-4 drill. |
| **Ward Dome** | A new 3x3 area shield with its own code and its own drawing: a thin double rim, a slow seam, hex ticks and small impact rings at `Layer.shields` - no bloom, no vanilla shield shader, no light pollution. It is bounded by design: a capped bank, and when the bank empties the dome **breaks** and stays down 14 s before returning empty. Aurora coolant widens it and speeds the rebuild; a resonance shard deepens the bank for 22 s. |
| **Ground well** | 2.5x the old rate (7.00/s → 17.50/s) and a base efficiency of 0.4, so it now pumps on **every** floor - damp ground is simply better. |
| **Detainer panel restored** | The 7.4 merge imported only `astro/content` and `astro/g3d`, which silently dropped the pilot panel, the settings page, the hot keys, the warp target picker and the tactical brain. `astro/ui`, `astro/ai` and the command block are back and wired in. Only the **names** changed: Tactical Command Core, the *Detainer pilot panel*, Lumen Superfactory - mechanics untouched. |

### New in 8.2-beta

| Area | Change |
|------|--------|
| **Sector launch** | Launching to another sector asked for **vanilla** items, because `LaunchLoadoutDialog` falls back to the core-shard loadout when a modded core has no registered loadout schematic. The Aurelia core now registers its own, and the saved launch payload is stripped of non-Aurelia items on every Aurelia world load. Launch cost is 100 % Aurelia resources. |
| **Controllable drones** | `Unit.isCommandable()` only accepts a `CommandAI`, so the hauler AI now extends it. Lumen drones and lumen haulers can be taken over, box-selected, commanded and driven by logic; when the order finishes they rejoin the logistics network by themselves. Since they can be flown, they are now targetable and hittable. |
| **Strict conservation** | Every scheduled haul is clamped by the destination's real `acceptStack()`; a hub that is removed hands its drones' cargo to the nearest accepting building before despawning them; request pads never dump, which kills the pad↔vault shuttle loop; a 60-second watchdog returns a stuck job while keeping the cargo. Measured: zero item drift over a 30 s run, zero loss on hub removal, dispatch count flat (no infinite loop). |
| **Two overdrive projectors** | **Lumen Accelerator** (2×2, 11 tiles, ×1.5) and **Prism Overcharger** (3×3, 24 tiles, ×2.1, ×2.6 with resonance shards). Same base class as the vanilla projector, so they do not stack past the ceiling. |
| **Titan unit: Prism Monolith** | Not another spider. An armoured slab riding three lift pods, one vented prism lance on the spine, two shoulder air pods. The lance fires **four shots, then vents for six seconds** — a hard upper bound on its damage. 24 000 HP, armour 15, still below the Detainer. |
| **Credits & description** | `mod.hjson` carries both authors; the description is exactly two lines: `工业垃圾测试版` / `Industrial Junk Beta`. |

---

## Building

Requirements: JDK 17, the Mindustry jar and the Android SDK platform jar (the setup script fetches them).

```bash
bash tools/setup_env.sh     # fetches Mindustry.jar, server-release.jar, r8.jar, android.jar, JDK 17
chmod +x *.sh
./build.sh                  # bakes the 3D models, compiles, dexes, writes dist/Singularity-v8.3-beta.jar
```

Test suites (all run against the official headless server):

```bash
./cmdtest.sh      # v8.2: launch loadout, controllable drones, conservation, projectors, titan
./logitest.sh     # v8.1: logistics network, liquids, gates
./test_server.sh  # full content / campaign / balance audit
./maptest.sh      # campaign terrain
./diag.sh         # quick smoke test
```

## Installing

Drop `Singularity-v8.3-beta.jar` into `Mindustry/mods/` (desktop) or import it from the in-game mod browser's
"Import file" on Android. The same jar works on both.

## Licence

GPL-3.0-or-later. See [`LICENSE`](LICENSE).
