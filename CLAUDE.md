## Working assumptions

- **Server is always restarted after every code change.** Never assume a bug report reflects old code. If the user reports a problem, assume the latest code is running and debug from there.
- **Before fixing any bug or designing any feature — check how working mods solve it first.** Look in `MOD_NOTES.md`, `OTHER_MODS_EXAMPLES/`, and decompiled sources. Only then design the solution. Never reach for a patch before checking the reference implementations. This is not optional and does not require the user to ask.

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

| File | Purpose |
|---|---|
| `MEMORY.md` | Full project context — read first |
| `SPEC.md` | Game design spec — read for mod mechanics, systems, and design intent |
| `RESEARCH.md` | API research results |
| `BLOCKS.md` | All block IDs from Assets.zip — check here before using any block name |
| `ITEMS.md` | All item IDs — check here before using any item name |
| `MOD_NOTES.md` | Decompiled mod analysis (API patterns) |
| `CUSTOM_UI.md` | Custom UI reference — read before touching any `.ui` file or UI Java code |
| `NARRATIVE.md` | Tone and dialogue style guide — read before writing any NPC dialogue |
| `HytaleModding-site/` | Official modding wiki |
| `convergence/` | Gradle mod project (Java 25, Kotlin DSL) |

## Source structure (`convergence/src/main/`)

### Java (`dev.hearthbound`)

**Root**
- `HearthboundPlugin` — plugin entry point, registers all events, commands, systems

**`village/`** — data model
- `VillageData` — BSON component on player entity: village name, buildings list, villager summaries
- `VillagerData` — BSON component on NPC entity: race, profession, happiness, housing
- `BuildingRecord` — one built/in-progress building: type, anchor position, rotation, storage map, state
- `BuildingType` — string constants for building types + anchor block IDs
- `VillagerSummary` — lightweight villager snapshot stored in VillageData (works even when NPC chunk is unloaded)
- `VillageManager` — lifecycle: create/get VillageData, found village, add buildings, add villagers

**`building/`** — construction pipeline
- `BuildingSystem` — orchestrator: ghost preview → founding → resource construction flow
- `GhostPreview` — shows bounding-box debug shapes + Filter_Air_Block blocks at build site; clears on confirm
- `PrefabLoader` — loads `.prefab.json` → `List<BlockEntry>` aligned to anchor position
- `SiteClearer` — pre-construction pass: elf walks plan, breaks terrain blocks one by one
- `ResourceBlockPlacer` — places blocks one by one, consuming items from building storage; pauses on resource shortage
- `BlockPlacer` — low-level: places a single block into the world at correct rotation
- `BuildingLayout` — computes door/interior positions from prefab; cached per BuildingType
- `BuildingGenerator` — legacy programmatic layouts (replaced by prefabs, kept for fallback)
- `StorageChestReader` — reads `Stayed_Storage_Chest` container blocks near a building; returns item counts
- `WarehouseDepositor` — deposits/withdraws items from Warehouse chests
- `ResourceProducer` — drop tables + produce-chance constants for Farm/Sawmill/Mine

**`npc/`** — NPC management
- `ElfSage` — spawns elf with custom PlayerSkin (compound cosmetic IDs), stores/restores UUID
- `BuilderBehavior` — Frozen + packet rotation + `moveTo` for the elf while building
- `NpcManager` — `spawnNpc()` wrapper that fixes `PersistentModel scale=0` engine bug
- `NpcRegistry` — in-memory UUID → entry map; polls world until chunk loads, then calls NpcRestorer
- `NpcRestorer` — re-applies skin and Interactable component after NPC reloads from chunk
- `VillagerAppearance` — generates random villager PlayerSkin from archetype pools
- `VillagerScheduler` — role switching (idle/farmer/miner/lumberjack), day schedule, rotate-offset helper
- `VillagerNames` — random first/last name generation by race
- `ElfNpcComponent` — BSON marker component on elf entity: owner UUID + spawn position
- `HearthboundDataStore` — JSON file persistence for NpcRegistry data (survives server restarts)
- `appearance/BodyArchetype`, `StyleArchetype`, `CosmeticPools` — cosmetic ID pools per race/style

**`events/`** — event handlers
- `FoundingStoneHandler` — F on Founding Stone → TownHallPage (pre/post founding); Shift+F → native container
- `BrazierHandler` — F on Brazier (villager house anchor) → VillagerHousePage
- `LumbermillHandler` — F on Stayed_Lumbermill → SawmillPage
- `ScarecrowHandler` — F on Stayed_Scarecrow (farm anchor) → FarmPage
- `CounterHandler` — F on Stayed_Counter → (shop UI placeholder)
- `MineTorchHandler` — F on Stayed_Mine_Sign → MinePage
- `BlockPlaceHandler` — detects anchor block placement, triggers ghost preview
- `BlockBreakHandler` — detects anchor break, cancels ghost/construction
- `PlayerJoinHandler` — restores elf + ghost on player join
- `NpcChunkLoadHandler` — detects chunk load, notifies NpcRegistry to restore NPCs in that chunk
- `VillageTickHandler` — 5s periodic tick: resource production, villager scheduling, objective checks

**`ui/`** — UI pages + HUD
- `TownHallPage` — 3-tab Founding Stone UI: Village info / Construction / Storage hint
- `VillagerHousePage` — villager house management UI (Brazier F-key)
- `SawmillPage` / `FarmPage` / `MinePage` — resource building management UIs
- `WarehousePage` — warehouse deposit/withdraw UI
- `ElfDialogPage` — elf sage F-key dialog (branching conversation)
- `VillagerDialogPage` — generic villager F-key dialog
- `RescueDialogPage` — rescue quest dialog (trapped villager)
- `VillageHud` — top-bar HUD showing village name + resource counts
- `TestDialogPage` / `TestHud` — dev/debug UI pages
- `DialogEventData` — shared event data codec for dialog pages

**`quest/`**
- `RescueQuest1` — rescue quest: spawns trapped NPC, sets objectives, handles follower behavior

**`util/`**
- `TickScheduler` — schedules periodic tasks on the world thread (wraps `ScheduledExecutorService` + `world.execute()`)

**`commands/`** — `/hb` subcommands (all dev/debug)
- `HearthboundCommand` — root command group
- `SpawnCommand` / `NpcCommand` — spawn elf/villager NPCs
- `BuildCommand` / `FastBuildCommand` / `InstaBuildCommand` — trigger building at various speeds
- `ResetCommand` / `HardResetCommand` — wipe village data
- `DebugCommand` / `BlockCommand` / `DoorsCommand` — world inspection
- `SkinCommand` / `CosmeticsCommand` / `TestSkinTonesCommand` / `TestVillagersCommand` — skin testing
- `SaveCommand` / `LoadCommand` / `PrefabModeCommand` — prefab editing helpers
- Other minor debug commands (`DialogCommand`, `HudCommand`, `TimeCommand`, `QuestCommand`)

---

### Resources (`src/main/resources/`)

**`Common/UI/Custom/*.ui`** — UI layout files (DSL syntax, NOT JSON)
- `TownHall.ui`, `ElfDialog.ui`, `VillagerDialog.ui`, `VillagerHouse.ui`
- `Sawmill.ui`, `Farm.ui`, `Mine.ui`, `Warehouse.ui`
- `VillageHud.ui`, `TestHud.ui`, `TestDialog.ui`, `RescueDialog.ui`

**`Server/Prefabs/*.prefab.json`** — building blueprints
- `Townhall_lvl1_v1/v2`, `Townhall_lvl2_v1` — Town Hall variants
- `VillagerHouse_lvl1-4_v1` + `Alternative2` — human house tiers
- `Warehouse_lvl1-4_v1_Alternative` — warehouse tiers
- `Farm_lvl1_v1`, `Sawmill_lvl1_v1`, `Mine_lvl1_v1/lvl2_v1`, `GuardTower_lvl1_v1` — work buildings
- `VillagerHouse_lvl1_v1.prefab.json` also used for rescue camp (`VillagerResque_camp_v1`)

**`Server/NPC/Roles/*.json`** — NPC behavior trees
- `Elf_Sage_Villager.json` / `Elf_Sage_Wanderer.json` / `Elf_Sage_Builder.json` — elf roles
- `Villager_Human.json` + job variants (Farmer, Lumberjack, Miner, Eating, Traveling)
- `Villager_Rescue_Trapped.json` / `Villager_Rescue_Follower.json` — quest NPCs

**`Server/Item/Items/Stayed/*.json`** — custom block definitions
- `Stayed_Founding_Stone`, `Stayed_Brazier`, `Stayed_Scarecrow`, `Stayed_Lumbermill`
- `Stayed_Counter`, `Stayed_Mine_Sign`, `Stayed_Storage_Chest`

**`Server/Item/Interactions/`** — F-key interaction definitions (`Stayed_OpenUI`, `Stayed_RescueUI`, `Stayed_VillagerUI`)

**`Server/Item/RootInteractions/`** — root interaction bindings (`Stayed.json`, `StayedRescue.json`, `StayedVillager.json`)

**`Server/Models/Human/`** — villager model JSON for custom Human NPC skins (Farmer, Lumberjack, Miner)

**`Server/Objective/`** — rescue quest objectives, objective lines, reach-location markers

**`Common/Prefabs/town_hall_level1.json`** — legacy prefab (superseded by Server/Prefabs)

**`Server/Languages/en-US/server.lang`** — all user-facing strings (i18n keys)

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
