# ModsSamples — reference implementations

Third-party Hytale mods we borrow patterns from. **Read before designing or
fixing anything in the NPC / building / UI layers** — every one of these solved
problems we hit later, and our architecture already follows them in places.

Only each mod's own package is kept; shaded libraries (jsoup, mysql, hikari,
protobuf) were stripped as noise.

Deeper API notes on these and ~10 other mods live in `MOD_NOTES.md`.

| Directory | Version | Origin |
|---|---|---|
| `HyCitizens/` | 1.7.0 | Real sources — <https://github.com/ElectroGamesDev/HyCitizens> (source-available licence, see `HyCitizens/LICENSE`) |
| `KyuubiSoftCore/` | 2.2.9 | Decompiled from the CurseForge jar with `cfr` |
| `FH_CompanionNPCs/` | 0.2.0 | Decompiled from the CurseForge jar with `cfr` |

None of this is our code and none of it ships in the mod jar — it sits outside
`src/` purely as reading material.

---

## HyCitizens — persistent NPC identity and lifecycle

A full NPC (Citizen) management plugin: place, configure and script NPCs from
an in-game UI. Solved the same core problem we have — *how does an NPC survive
chunk unloads, server restarts and engine-assigned UUID changes?* Our
`NpcRegistry` / `StayedNpcIdentityComponent` / `NpcChunkLoadHandler` stack is
modelled on it.

What to read:

- `components/CitizenNpcIdentityComponent.java` — identity lives in a BSON
  component on the entity, not in a UUID map. This is the pattern behind
  `StayedNpcIdentityComponent`.
- `listeners/ChunkPreLoadListener.java` — chunk-event-driven restoration:
  collect candidates from both the saved chunk index and the base chunk index,
  never trust a single saved chunk. Also the source of the
  `ChunkUtil.indexChunkFromBlock(double, double)` rule — pass doubles so the
  engine floors negative coordinates itself instead of int-casting them
  towards zero.
- `listeners/DuplicateNPCPrevention.java` — `RefSystem`-based dedup on entity
  add; our `DuplicateNpcPrevention` is the same shape.
- `managers/CitizensManager.java` — record store, position sync, rebinding an
  entity UUID to a stable id.
- `roles/`, `interactions/`, `actions/` — in-place role swap and F-key
  interaction wiring.
- `ui/` — a large, working custom-UI corpus (admin pages, editors) if you need
  a second opinion on `.ui` layout.

## KyuubiSoftCore — making an NPC hold still, turn and animate

Large "core" plugin (citizens, shops, dialogs, economy, quests, web editor).
Useful to us for everything that fights the engine's own NPC motion.

What to read:

- `citizen/CitizenRotationManager.java` — packet-based NPC rotation:
  `Direction` → `ModelTransform` → `TransformUpdate` → `EntityUpdates` written
  straight to the player's packet handler. This is exactly what
  `BuilderBehavior.lookAtBlock` does when the elf turns to the block it is
  placing.
- `citizen/CitizenService.java` — `Frozen` component handling plus resetting
  the engine motion controller by reflection. Relevant whenever a frozen NPC
  keeps sliding or keeps its walk animation.
- `citizen/CitizenAnimationManager.java` / `CitizenEmoteManager.java` —
  `PlayAnimation` slot usage and which slots actually render.
- `citizen/CitizenChunkListener.java` — a second take on chunk-load
  restoration, worth comparing against HyCitizens'.
- `citizen/CitizenSkinManager.java`, `CitizenAppearanceOverrideManager.java` —
  `PlayerSkin` / model overrides on NPCs.
- `citizen/CitizenScheduleManager.java` — day-schedule driven behaviour, the
  same territory as our `VillagerScheduler`.
- `ui/`, `kslang/`, `i18n/` — page/HUD builders and localisation plumbing.

## FH_CompanionNPCs — NPCs that actually do work

Companion NPCs that farm, mine, fight and haul loot for the player. The
closest existing thing to our working villagers, and the source of several
concrete fixes in `FarmerWorkBehavior` and `WarehouseDepositor`.

What to read:

- `systems/FarmSystem.java` — the crop state machine: scan the plot, pick a
  target, seek it, swing, apply the effect. Includes the trick our watering
  code copies — try several candidate "watered" block ids and verify the block
  actually changed, because `setBlock` with an unknown id silently no-ops.
  Also crop stage parsing (`extractStageIndex`, `expectedMaxStageIndexForCrop`)
  and replant/seed handling.
- `systems/DepositSystem.java` — treat a deposit as successful only when the
  transaction remainder is empty; `WarehouseDepositor.tryDepositAt` follows
  this.
- `systems/MinerSystem.java` — the mining equivalent, useful when the miner
  profession gets real work.
- `systems/WorkPositioning.java` — approaching and holding a work point
  (arrival tolerance, re-issuing a route), the problem our farmer's
  `SEEKING` / `REACH_TIMEOUT_MS` logic solves.
- `systems/CompanionEquipmentManager.java` — swapping the visible held tool per
  task, same job as our `HotbarUtil`.
- `systems/CompanionItemUseHandler.java`, `CombatAssist.java`,
  `LootSystem.java` — item use, combat assist and pickup behaviour.

---

## Licence note

`HyCitizens/LICENSE` is source-available: viewing, forking and private sharing
are allowed, redistribution outside GitHub forks needs the author's written
permission. The two decompiled mods carry no redistribution licence at all.
Keep this directory as local reading material; don't republish it.
