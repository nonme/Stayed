## Working assumptions

- **Server is always restarted after every code change.** Never assume a bug report reflects old code. If the user reports a problem, assume the latest code is running and debug from there.
- **Before fixing any bug or designing any feature — check how working mods solve it first.** Look in `ModsSamples/` (the three mods we borrow from most, see below), `MOD_NOTES.md`, and `OTHER_MODS_EXAMPLES/`. Only then design the solution. Never reach for a patch before checking the reference implementations. This is not optional and does not require the user to ask.

---

## Engineering principles

1. **Think before coding.** Consider multiple approaches and choose the one that is architecturally correct long-term — not the fastest to type.
2. **Surface uncertainty, don't hide it.** State assumptions explicitly before implementing. If the task has multiple valid interpretations, present them and ask — don't pick silently. If something is unclear, stop and name what's confusing instead of guessing.
3. **No hacks, no patches.** If the right fix requires changing three files, change three files. Quick workarounds compound into debt that is orders of magnitude more expensive to fix later.
4. **Self-review before finishing.** Ask: *Is this the right solution, or did I just paper over the problem? Will this break on edge cases?* If the honest answer is "a bit of a hack" — stop and do it properly.
5. **Correct > fast.** Speed of implementation never justifies an incorrect solution.
6. **Only change what was asked.** Don't refactor surrounding code, don't add features beyond the task.
7. **Compute, don't guess.** Any arithmetic, formula check, offset/size calculation, or unit conversion must be done with an inline script (`python3 -c "..."` or `node -e "..."` via Bash), not in your head. LLMs miscalculate silently, then spiral re-checking wrong numbers.
8. **Do not commit until the feature is tested — and never commit without explicit user approval.** "Tests pass" ≠ "feature works": passing unit tests, a green type-check, or your own smoke run is *not* sufficient evidence to commit. Before every commit, explicitly ask the user whether the feature has been tested enough and whether to commit now. Wait for a clear "yes" — silence or an unrelated reply is not approval.

---

## Coding standards

- **Single responsibility** — one function does one thing. If you need "and" to describe it, split it.
- **Guard clauses over nesting** — fail fast at the top, keep the happy path unindented.
- **Self-documenting names** — `get_active_accounts()` not `get_data()`. Verbs for functions, nouns for variables.
- **No abbreviations unless universal** — `count` not `cnt`, `index` not `idx` (except loop vars).
- **Separate concerns** — business logic separate from I/O. Pure functions are easier to test.
- **Exceptions, not return codes** — don't return `None` to signal failure; raise a specific exception.
- **Validate at boundaries** — validate inputs at system boundaries (user input, API response). Trust internal code.
- **Comments explain WHY, not WHAT** — flag non-obvious decisions and gotchas. No noise, no docstrings on obvious functions.
- **All comments in English.**


# Stayed — Hytale Mod

Hytale mod. Build a multi-race village with an elf NPC as the builder/advisor.

## Quick start

```bash
make build   # compile check
make dev     # start dev server
```

Client: Hytale (Flatpak). Connect to localhost.

Both targets auto-activate `sdk use java 25.0.2-open` via SDKMAN. Run from the repo root.

## Key files

Every doc in the repo, and what it is actually for. `git log` — not any of
these — is the source of truth for *what already works*; see MEMORY.md.

All paths below are relative to the mod repo — prefix them with `convergence/`
when working from the local workspace root.

| File | Purpose |
|---|---|
| `README.md` | Player-facing: what the mod is and how to play it. Start here if you've never seen the mod. |
| `MEMORY.md` | Durable technical context: stack, verified engine API patterns, environment gotchas. **Not** a status file. |
| `SPEC.md` | Game design spec — mechanics, systems, design intent. Read before changing gameplay. |
| `NARRATIVE.md` | Tone and dialogue style guide — read before writing any NPC line. |
| `BUILDING_GUIDE.md` | How to design a building that looks right in Hytale's voxel style (proportions, silhouettes, materials). Read before authoring a prefab. |
| `CUSTOM_UI.md` | Custom UI reference — read before touching any `.ui` file or UI Java code. |
| `BLOCKS.md` | Every block ID from Assets.zip. **Check here before using any block name** — a non-existent id crashes chunk load. |
| `ITEMS.md` | Every item ID, same rule. |
| `COOKING_RECIPES.md` | Vanilla Cooking Bench recipes extracted from the game assets — the data behind the Tavern / cook design. |
| `SKINS.md` | How villager appearance is generated — read before touching `VillagerAppearance` or `CosmeticPools`. |
| `RESEARCH.md` | Original API research (races, capabilities, engine limits). Historical but still the widest survey. |
| `MOD_NOTES.md` | Notes on ~15 decompiled mods — which mod solved what, with code snippets. |
| `ModsSamples/` | Actual sources of the three mods we borrow from most — see "Reference mods" above. |
| `KNOWN_ISSUES.md` | Open bugs with what was already ruled out. |
| `ANTI_PATTERNS.md` | Writing rules for docs and dialogue (banned AI-tell phrases, tone). |

Not in this repo, only in the local workspace: `HytaleModding-site/` (clone of
the official modding wiki) and `OTHER_MODS_EXAMPLES/` (~15 decompiled mods).

## Reference mods (`ModsSamples/`)

Three third-party mods whose code we lean on most, kept in-tree as reading
material (own packages only, shaded libs stripped; nothing here ships in the
jar). File-level pointers in `ModsSamples/README.md`, broader API notes for
these plus ~10 other mods in `MOD_NOTES.md`.

- **`HyCitizens/`** (1.7.0, real sources, source-available licence) — in-game NPC
  management plugin. Solved persistent NPC identity: identity lives in a BSON
  component on the entity (`CitizenNpcIdentityComponent`), restoration is driven
  by chunk pre-load events (`ChunkPreLoadListener`), duplicates are killed by a
  `RefSystem` on entity add (`DuplicateNPCPrevention`). Our `NpcRegistry` /
  `StayedNpcIdentityComponent` / `NpcChunkLoadHandler` / `DuplicateNpcPrevention`
  follow it directly — including the rule to pass **doubles** to
  `ChunkUtil.indexChunkFromBlock` so negative coordinates floor correctly.
  Also a large working custom-UI corpus.
- **`KyuubiSoftCore/`** (2.2.9, decompiled) — big "core" plugin (citizens, shops,
  dialogs, economy). Read it for anything that fights engine NPC motion:
  `CitizenRotationManager` is the packet-based rotation
  (`ModelTransform` → `TransformUpdate` → `EntityUpdates`) that
  `BuilderBehavior.lookAtBlock` copies; `CitizenService` handles the `Frozen`
  component and resets the motion controller by reflection;
  `CitizenAnimationManager` shows which animation slots actually render;
  `CitizenScheduleManager` is the same territory as our `VillagerScheduler`.
- **`FH_CompanionNPCs/`** (0.2.0, decompiled) — companion NPCs that farm, mine and
  haul. The closest existing thing to our working villagers. `FarmSystem` is the
  crop state machine (scan → pick target → seek → swing → apply), including the
  "try several watered block ids and verify the block changed" trick our watering
  uses, plus crop-stage parsing; `DepositSystem` is where the "deposit succeeded
  only if the transaction remainder is empty" rule comes from;
  `WorkPositioning` handles approaching and holding a work point;
  `CompanionEquipmentManager` is the held-tool swap our `HotbarUtil` does.

Licence note: HyCitizens is source-available (forks and private sharing fine,
redistribution needs the author's permission); the two decompiled mods carry no
redistribution licence. Local reference only — don't republish.

---

## How the main flow hangs together

Worth reading once before touching anything — most classes below only make sense
in this context.

1. **Anchor block.** Every building is represented by one custom block named
   after it (`Stayed_House`, `Stayed_Farm`, `Stayed_Warehouse`, …). The player
   gets it from Aelin's dialog or the Founder's Almanac and places it in the
   world. `BlockPlaceHandler` sees the placement and asks `BuildingSystem` for a
   ghost preview.
2. **Ghost preview.** `PrefabLoader` reads the building's `.prefab.json`,
   aligns it so the prefab's anchor block lands exactly on the placed block, and
   rotates it to the direction the player was facing. `GhostPreview` spawns one
   transient `BlockEntity` per block — nothing is written to the world, so
   cancelling costs nothing.
3. **Confirm.** Pressing F on the anchor opens that building's page (see
   `events/` → `ui/`). Confirming stores a `BuildingRecord` in `VillageData`.
   The Town Hall is special: confirming it *founds the village*.
4. **Resources.** Each `BuildingRecord` carries its own storage map. The page
   lists what the prefab needs; the player deposits from inventory (or conjures
   it in creative).
5. **Construction.** `SiteClearer` has Aelin break the terrain inside the
   footprint, then `ResourceBlockPlacer` places blocks one at a time, consuming
   the matching item per block and pausing when something runs out.
   `BuilderBehavior` freezes Aelin, turns him toward each block and puts it in
   his hand. On completion `BuildingSystem` re-places the prefab in one shot
   (fixes door rotation) and `PathwayBuilder` reroutes the road network.
6. **Life.** `VillageTickHandler` runs every 5s: assigns homeless villagers to
   houses and idle villagers to workplaces, advances hunger, rolls production
   into warehouse storage. `VillagerScheduler` moves each villager between home
   / work / meals and swaps their NPC role accordingly. Most of this works on
   `VillageData` alone, so it keeps running with chunks unloaded and is replayed
   on join (`runCatchUp`).
7. **Persistence.** NPC identity lives in a BSON component on the entity
   (`StayedNpcIdentityComponent`), mirrored in `NpcRegistry` and on disk in
   `mods/HearthboundData/data.json`. Chunk-load events restore skins, roles and
   F-key interactions; duplicates are killed on sight. This is the most
   defensive part of the codebase — see the `npc/` notes below.

---

## Source structure (`src/main/`)

### Java (`dev.hearthbound`)

**Root**
- `HearthboundPlugin` — entry point: installs the logger, registers components,
  events, ECS systems, commands and custom-UI page suppliers.

**`village/` — persistent data model (BSON)**
- `VillageData` — the village, stored on the *player* entity: name, stage,
  founding-stone position, owning world, building list, villager summaries,
  dialog/quest flags, laid pathway cells, `schemaVersion` + `runMigrations()`.
- `VillagerData` — per-villager component on the NPC entity: race, profession,
  hunger, housing flag, name.
- `VillagerSummary` — durable copy of a villager inside `VillageData`, so UIs can
  list residents (and their complaints) while their chunks are unloaded.
- `BuildingRecord` — one building: type, variant, anchor position, rotation,
  own storage map, completed flag, assigned villager.
- `BuildingType` — the single source of truth for buildings: type ids, anchor
  block ids, prefab names, anchor Y inside each prefab, stand/work points,
  display names, categories, short descriptions.
- `VillageManager` — lifecycle: get/create `VillageData`, found the village, add
  buildings and villagers, assign professions, reconcile stale NPC references,
  abstract (chunk-independent) hunger tick.
- `ResetVillageService` — shared implementation behind `/hb reset` and the tests,
  so both wipe state identically.

**`building/` — construction pipeline**
- `BuildingSystem` — orchestrator: ghost preview → founding → site clearing →
  resource construction → completion → repair.
- `PrefabLoader` — `.prefab.json` → `List<BlockEntry>` aligned to the anchor and
  rotated; also finds doors and reads the anchor's stored rotation.
- `GhostPreview` — transient `BlockEntity` markers + bounding box; never writes
  to the chunk.
- `SiteClearer` — pre-build pass: breaks terrain blocks inside the footprint one
  by one, with the elf swinging at each.
- `ResourceBlockPlacer` — places the plan block by block, consuming items from
  the building's storage, pausing and resuming on shortage.
- `BlockPlacer` — low-level single-block placement with correct rotation.
- `BuildingScanner` — compares world against plan: detects a manually built
  structure, and computes the repair cost of a damaged one.
- `BuildingLayout` — derives interior stand point, door/gate positions and their
  state blocks from the prefab; cached per type+variant.
- `DoorPrefabResolver` — maps a door/gate state-block id to its one-cell prefab.
  Placing a state variant any other way loses rotation.
- `PathwayBuilder` — road network: builds a graph over building door nodes, routes
  each edge with A*, converts natural ground to `Soil_Pathway` and remembers every
  converted cell so it can be undone.
- `TerrainBlender` — shared terrain smoothing/cleanup around placed prefabs.
- `FarmBounds` — the planting bounding box of a farm, derived from prefab geometry.
- `FarmScanner` — reads the farm bbox and classifies each cell into harvest / weed
  / till / replant / water targets.
- `StorageChestReader` — reads the physical `Stayed_Storage_Chest` containers of a
  warehouse.
- `WarehouseDepositor` — deposits/withdraws items; `*Abstract` variants work on
  `VillageData` storage and therefore with the warehouse unloaded.
- `ResourceProducer` — drop tables and per-tick produce chances for work buildings.
- `CraftabilityIndex` — indexes every item as craftable / has-a-source / neither
  (JET's logic), used to sanity-check what the player can actually obtain.
- `BuildingGenerator` — legacy programmatic layouts, kept only as a fallback.

**`npc/` — NPC identity, lifecycle and work**

The defensive half of the mod. The engine may reassign an entity's UUID on chunk
reload, spawn duplicates, or write a broken model scale — every class here exists
because of a bug that actually happened.

- `StayedNpcIdentityComponent` — BSON identity marker (`npcId`, a stable UUID
  string). The one thing that survives everything.
- `NpcRegistry` — in-memory registry indexed by `npcId` and by entity UUID, plus
  world scoping and pending-removal tombstones.
- `HearthboundDataStore` — persists the registry to
  `mods/HearthboundData/data.json` (atomic write).
- `StayedNpcSpawner` / `StayedRoleGenerator` / `StayedRoleNames` /
  `StayedRoleAssetPackManager` / `StayedSpawnRolePolicy` — every persistent NPC
  spawns with a *generated* role file whose name embeds its `npcId`, so identity
  can be recovered from the role name alone.
- `StayedRoleChangeApplier` — durable role swap: registry and disk first, then the
  live engine role, with retries until the generated role is indexed.
- `NpcChunkLoadHandler` (in `events/`) + `NpcRestorer` — on chunk load: rebind
  UUIDs, re-apply skin, model, `Interactable`/`Interactions`, profession item.
- `DuplicateNpcPrevention` — ECS system: at most one live entity per `npcId`.
- `NpcLiveEntityResolver` — find the live entity by scanning loaded NPCs instead of
  trusting a possibly stale UUID ref.
- `NpcMissingEntityRecovery` — last-resort respawn when a record's entity is gone
  while its chunk is loaded (cooldowned, one in flight).
- `NpcPositionTracker` — syncs live positions back into the registry so
  `chunkIndex` never goes stale.
- `NpcTeleporter` — move an NPC without disturbing role, leash or `Frozen` state
  (the "Recall" buttons).
- `NpcManager` — spawn wrapper + the `PersistentModel scale=0` engine-bug fix.
- `ElfSage` — Aelin: spawn, tent placement, role changes, sage skin.
- `ElfNpcComponent` — legacy elf marker, kept for old saves.
- `BuilderBehavior` — freeze + packet rotation + held item while building.
- `VillagerScheduler` — the daily loop: home / work / meals, role swaps, door and
  gate opening, guard patrol sessions, travel recovery.
- `GuardPatrolRoute` — pure route planner over the road graph.
- `VillagerTravelRecovery` — unsticks villagers that fail to reach a target.
- `FarmerWorkBehavior` — the farmer state machine (seek → animate → apply effect →
  deposit), including watering that verifies the block actually changed.
- `VillagerAppearance` + `appearance/{BodyArchetype, StyleArchetype, CosmeticPools}`
  — deterministic villager skins from curated ID pools, with single-field repair
  when the engine rejects a combination.
- `VillagerNames` — deterministic names from the same seed as the skin.
- `HotbarUtil` — swap the visible held item in slot 0.
- `StayedIntegrationTestNpcMarkerComponent` — marks test-spawned NPCs so cleanup
  can never touch a real one.

**`events/` — event handlers**
- `BlockPlaceHandler` / `BlockBreakHandler` — anchor placed → ghost preview;
  anchor broken → cancel ghost or construction.
- `FoundingStoneHandler` — F on the Founding Stone → `TownHallPage`
  (Shift+F falls through to the native container).
- One handler per building anchor, each cancelling the native container and
  opening its page. **The class names predate the anchor rename, the block ids
  are current:**

  | Handler | Anchor block | Opens |
  |---|---|---|
  | `BrazierHandler` | `Stayed_House` | `VillagerHousePage` |
  | `ScarecrowHandler` | `Stayed_Farm` | `FarmPage` |
  | `CounterHandler` | `Stayed_Warehouse` | `WarehousePage` |
  | `WoodcuttersHandler` | `Stayed_Woodcutters_Hut` | `WoodcuttersPage` |
  | `SawmillHandler` | `Stayed_Sawmill` | `SawmillPage` |
  | `MineTorchHandler` | `Stayed_Mine` | `MinePage` |
  | `TargetDummyHandler` | `Stayed_Guard_House` | `GuardHousePage` |
  | `ForgeHandler` | `Stayed_Forge` | `ForgePage` |
  | `TavernHandler` | `Stayed_Tavern` | `TavernPage` |

- `PlayerJoinHandler` — on join: spawn/restore Aelin, resume interrupted
  construction, replay missed village ticks, start the tick handler and position
  tracker, migrate legacy inventory items.
- `NpcChunkLoadHandler` — the main NPC restoration path (see `npc/` above).
- `VillageTickHandler` — the 5s village tick plus the offline catch-up replay.
- `InteractionDebugHandler` — dev tracing of interact/mouse/packet flow.

**`ui/` — pages and HUD**

All pages are `InteractiveCustomUIPage` + a `.ui` layout file of the same name.
- `TownHallPage` — Founding Stone: naming and founding, village info, construction,
  repair, population recall.
- `VillagerHousePage`, `WarehousePage`, `FarmPage`, `WoodcuttersPage`,
  `SawmillPage`, `MinePage`, `GuardHousePage`, `ForgePage`, `TavernPage` — one per
  building: info / construction / repair, deposit buttons, assigned worker.
- `AlmanacPage` — Founder's Almanac: village overview with settler complaints, and
  a building catalog by category that hands out anchor blocks.
- `ElfDialogPage` — Aelin's branching dialog (intro, quests, build menu).
- `VillagerDialogPage` — generic villager dialog.
- `RescueDialogPage` — trapped-villager rescue dialog and objective advance.
- `VillageHud` — top-left village status HUD.
- `DialogEventData` — shared event-data codec for all pages.
- `TestDialogPage`, `TestHud` — dev scratch pages.

**`quest/`**
- `RescueQuestManager` — all rescue variants: pick the next variant, find a site,
  place the prefab, spawn victim/enemies, handle follower conversion and cleanup.

**`util/`**
- `TickScheduler` — schedule work onto the world thread.
- `ItemDisplayName` — technical item id → localized player-facing name.
- `ResourceListRenderer` — the shared "icon + name + count" row used by every
  building UI.
- `log/` — the mod's logging layer (see MEMORY.md for usage): `Log` +
  `LogBuilder` (structured fields, throttling), `LogConfig` (per-category levels
  in `mods/HearthboundData/logging.json`), `ConsoleAggregator` (dedup),
  `ArchiveWriter` + `RingBuffer` (NDJSON archive), `LogDump` (bug-report zip),
  `Suppression`, `CallsiteResolver`, `LogEvent`, `LogLevel`.

**`commands/` — `/hb …`, all dev/debug**
- Root group: `HearthboundCommand`.
- Build: `BuildCommand`, `FastBuildCommand`, `InstaBuildCommand`,
  `PrefabModeCommand`, `SaveCommand`, `LoadCommand`, `PathwaysCommand`,
  `DoorsCommand`.
- Village/NPC: `SpawnCommand`, `SpawnVillagerCommand`, `NpcCommand`,
  `ResetCommand`, `QuestCommand`, `TimeCommand`.
- Appearance: `SkinCommand`, `CosmeticsCommand`, `TestSkinTonesCommand`,
  `TestVillagersCommand`.
- Inspection: `DebugCommand`, `BlockCommand`, `CropInfoCommand`,
  `FarmScanCommand`, `CraftabilityCommand`, `LogCommand`.
- Navigation/UI: `TpCommand`, `WarpCommand`, `HudCommand`, `DialogCommand`,
  `GiveAlmanacCommand`.
- Tests: `TestCommand` (`/hb test run|pkg|list`).

**`test/` — in-game integration test framework**

Not JUnit: these run inside a live server via `/hb test`, because the bugs worth
catching are chunk-load and restart bugs.
- `engine/` — `StayedIntegrationTestRunner`, `TestCase`, `TestPackage`, `TestStep`,
  `TestContext`, `TestLogger`, `TestReport`, `StepResult`, `TestCleanup`.
- `steps/` — ~30 reusable steps: setup/reset village, spawn villagers, nudge NPCs
  across chunk borders, teleport far and back, snapshot/diff registry state, force
  village ticks, assert housing/roles/positions/quest counts, restart snapshots.
- `packages/` — scenario bundles: `SmokePackage`, `ChunksPackage`,
  `HousingPackage`, `RolesPackage`, `RescuePackage`, `AelinPackage`,
  `RestartPackage`.
- `audit/` — `NpcRegistryInvariantAudit` + `NpcRegistrySelfCheck`: read-only scan
  of every NPC-lifecycle invariant, also run periodically in the background.

Plain unit tests that need no server live in `src/test/java` and are run by hand
with `javac`/`java` (each has a `main`).

---

### Resources (`src/main/resources/`)

**`manifest.json`** — mod id, version, entry point, pinned `ServerVersion`.

**`Common/UI/Custom/*.ui`** — UI layouts in the Hytale UI **DSL** (not JSON —
JSON crashes the client). One file per page: `TownHall`, `VillagerHouse`,
`Warehouse`, `Farm`, `Woodcutters`, `Sawmill`, `Mine`, `GuardHouse`, `Forge`,
`Tavern`, `Almanac`, `ElfDialog`, `VillagerDialog`, `RescueDialog`, `VillageHud`,
`TestDialog`, `TestHud`.

**`Server/Item/Items/Stayed/*.json`** — our custom blocks and items. One anchor
per building — `Stayed_Founding_Stone`, `Stayed_House`, `Stayed_Farm`,
`Stayed_Warehouse`, `Stayed_Woodcutters_Hut`, `Stayed_Sawmill`, `Stayed_Mine`,
`Stayed_Guard_House`, `Stayed_Forge`, `Stayed_Tavern` — plus
`Stayed_Storage_Chest` and the usable `Stayed_Founders_Almanac`. Anchors are
containers (`State.container` + `Open_Container`) so the F-key handler can
intercept the interaction and open our page instead.

**`Server/Item/Interactions/` + `Server/Item/RootInteractions/`** — F-key /
right-click interaction definitions and their bindings (`Stayed_OpenUI`,
`Stayed_VillagerUI`, `Stayed_RescueUI`, `Stayed_OpenAlmanac`).

**`Server/Item/Block/Hitboxes/`** — custom hitboxes for anchor blocks.

**`Server/NPC/Roles/*.json`** — behaviour trees. `Elf_Sage_{Wanderer,Villager,
Builder}` for Aelin; `Villager_Human` plus one variant per activity
(`Farmer`, `Lumberjack`, `Miner`, `Blacksmith`, `Guard`, `Eating`, `Traveling`);
`Villager_Rescue_{Trapped,Follower}` for the quest. These are *templates* —
persistent NPCs actually spawn with a generated copy whose name carries their
`npcId` (see `npc/StayedRoleGenerator`), written to
`mods/StayedGeneratedRoles/`.

**`Server/Prefabs/*.prefab.json`** — building blueprints exported from the
in-game Asset Editor. Current set used by `BuildingType.getPrefabName`:
`Townhall_lvl1_v3`, `VillagerHouse_lvl1_v2` + 3 alts, `Warehouse_lvl1_v2`,
`Farm_lvl1_v2`, `WoodcuttersHut_lvl1_v1`, `Sawmill_lvl1_v2`, `Mine_lvl1_v2`,
`GuardHouse_lvl1_v1`, `Forge_lvl1_v1`, `Tavern_lvl1_v1`. Also: rescue-quest sites
(`Villager_ResqueQuest_*`, `VillagerResque_camp_v1`), `Elf_tent`, one-cell
door/gate state prefabs (`door_*`, `gate_softwood_*`, `gate_hardwood_*`) used by
`DoorPrefabResolver`, and older `lvl1_v1` / `lvl2+` variants kept for the
levelling work that isn't wired up yet.

**`Server/Models/Human/*.json`** — model definitions for profession villagers.

**`Server/Objective/`** — rescue-quest objectives, objective lines and
reach-location markers (the quest arrow).

**`Server/Languages/en-US/server.lang`** — every user-facing string: item names,
interaction hints, NPC names. Never hardcode player-visible text elsewhere.

**`Common/Prefabs/`** — two legacy prefabs, superseded by `Server/Prefabs/`.

---

## Updating BLOCKS.md

After a game update (new blocks):

```bash
python3 gen_blocks.py
```

Reads from `~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/install/release/package/game/latest/Assets.zip` → `Server/Item/Items/<Category>/*.json`. Item ID = filename without `.json`.

If Assets.zip has moved — update the `ASSETS_ZIP` constant in `gen_blocks.py`.

## Git

Two separate git repos:

- `hytale-mod/` — the full local project (dev files, tools, this doc). Remote: none.
- `hytale-mod/convergence/` — the mod sources only. Remote: `git@github.com:nonme/Stayed.git`

All commits and pushes for the GitHub repo are done from inside `convergence/`:

```bash
cd convergence/
git add <files>
git commit -m "..."
git push origin main
```

Never commit or push from the `hytale-mod/` root for the GitHub repo.

## Contest rules

- AI-generated visual assets (textures, models) → **disqualification**
- AI-generated code → allowed

---

## Backward compatibility

**Default rule: every new version must be a drop-in replacement for the previous one.** Players who already have village data must be able to update the `.jar` without losing progress, resetting quests, or corrupting state. Backward compatibility is assumed unless explicitly stated otherwise; if an implementation breaks it, stop and notify the user before proceeding.

### How persistence works in this mod

There are three independent storage layers, each with different rules:

**Layer 1 — BSON components** (world data on entities)
Covers: `VillageData`, `VillagerData`, `BuildingRecord`, `VillagerSummary`, `ElfNpcComponent`.
When the engine loads an entity, it reads BSON and calls setters only for keys that exist in the saved data. Missing keys are silently skipped — the Java field retains its default value. This means:

- ✅ **Adding a new field to a CODEC** — always safe, as long as the field has a correct default value in Java (e.g. `= false`, `= 0`, `= ""`, `= new ArrayList()`). Old saves won't have the key; new code reads the default.
- ❌ **Renaming a BSON key** (e.g. `"RescueQuestStarted"` → `"RescueStarted"`) — old saves store data under the old key; the new code won't find it; the field silently resets to the Java default. This is data loss.
- ❌ **Changing a field's type** (e.g. `boolean` → `String`) — the BSON deserializer tries to read the old type into the new one; this crashes or silently corrupts depending on the codec implementation.
- ⚠️ **Removing a field from a CODEC** — old data in BSON is harmlessly ignored; no crash. But the data is gone on the next save cycle (orphaned). Only remove intentionally.
- ❌ **Renaming the component registration key** — `registerComponent(VillageData.class, "VillageData", CODEC)` — the string `"VillageData"` is the BSON lookup key. Rename it and all existing village data becomes invisible to the new code (the player's village is gone).
- ❌ **Changing a nested CODEC structure** (e.g. `BuildingRecord`) — same rules apply recursively. Renaming or retyping a field in `BuildingRecord.CODEC` loses data for all buildings stored in `VillageData.Buildings`.

**Layer 2 — JSON file** (`mods/HearthboundData/data.json`, via `HearthboundDataStore`)
Covers: `NpcRegistry` records — NPC UUIDs, role names, interaction types, skin seeds, chunk indices.
Parsed by Gson from plain POJOs. Missing JSON fields are `null`/`0` after deserialization.

- ✅ **Adding a new field to `PersistedRecord`** — Gson sets it to `null`/`0` for old saves; safe if the load code handles that (null-check or default).
- ❌ **Renaming a `PersistedRecord` field** — Gson won't map the old name to the new field; data is lost.
- ❌ **Renaming or adding a value to `NpcRegistry.InteractionType` enum** — the enum name is serialized as a string (`r.interaction = r.interaction.name()`). Adding a new value is safe for new records. **Renaming an existing value** (`RESCUE` → `RESCUE_QUEST`) causes `IllegalArgumentException: No enum constant` on load — all NPC records for that type are skipped with a warning, effectively losing those NPCs.
- ❌ **Removing an enum value** that exists in saved data — same crash as renaming.

**Layer 3 — NPC role names, prefab filenames, objective IDs** (strings referenced in code and stored in data)
These are string constants written into BSON (`VillagerData.roleName` via `NpcRegistry`), into `data.json` (`PersistedRecord.role`), and into `BuildingRecord` storage keys.

- ❌ **Renaming a role file** (e.g. `Villager_Human.json` → `Human_Villager.json`) — NPCs with the old role name in `data.json` will fail to restore. The NPC spawns again on next login but with no skin, interaction, or correct behavior.
- ❌ **Renaming a prefab file** (e.g. `VillagerHouse_lvl1_v1.prefab.json`) — existing buildings have the prefab name stored nowhere at runtime (only used during construction), so this is safe at runtime. But construction of new buildings with an old `BuildingType.getPrefabName()` value that now points to a deleted file will crash.
- ❌ **Renaming a `BuildingType` string constant** (e.g. `"house_human"` → `"human_house"`) — `BuildingRecord.type` is already saved in BSON for all existing buildings. A rename means `findBuilding("human_house")` will never match the old `"house_human"` records. All buildings of that type become invisible to the logic.
- ❌ **Renaming an Objective or ObjectiveLine ID** — the objective system stores UUIDs and references by string ID. Players mid-quest will have a stale objective that can never complete. Add new IDs; don't rename old ones.

### When breaking compatibility is unavoidable

Some changes genuinely require breaking. When that happens:

1. **Stop. Do not implement silently.** State explicitly what the break is, what data will be lost or corrupted, and how many players are affected.
2. **Propose a migration path** before writing any code — e.g. a one-time in-memory migration on first load that reads the old key and writes the new one, or a fallback default that makes the broken state recoverable in-game.
3. **Get explicit approval** before implementing. "This will lose rescue quest progress for existing players — proceed?" Wait for a clear yes.

### Migration pattern for additive changes that need backfill

When a new field is added but the zero-default is semantically wrong for players who already have data (e.g. adding `completedRescueQuests: List<String>` while some players already have `rescueQuestStarted = true`), add a one-time migration at the point of first use:

```java
// Example: infer completed quests from the old boolean flag on first access
if (village.isRescueQuestStarted() && village.getCompletedRescueQuests().isEmpty()) {
    village.getCompletedRescueQuests().add("rescue_1"); // treat old flag as quest 1 done
}
```

This is not a separate migration system — it lives in the feature code, runs once on first read, and becomes inert after the save cycle writes the populated list.
