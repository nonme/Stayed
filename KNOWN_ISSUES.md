# Known Issues

## Villager rescue victim appears naked on spawn (intermittent)

**Frequency:** ~1 in 4–15 spawns, not reproducible on demand. Not observed with `/hb test villagers`.

**Symptoms:** The rescue victim NPC shows as a naked Player model (no clothes, no hair).
Server logs confirm `putComponent done` and `skinCheck=ok` — the skin IS applied on the
server side. The client renders the entity naked anyway.

**What was ruled out:**
- `Appearance: Outlander` in the role JSON → fixed (now `"Player"`)
- `MouthWheat` / `EyePatch` bare IDs without color suffix → fixed
- `createModel` throwing → logs confirm it succeeds every time
- `PlayerSkinComponent` missing after `putComponent` → `getComponent` immediately
  after confirms it is present

**Fix attempted (session 10):** Deferred `VillagerAppearance.apply()` to the next tick
via a nested `world.execute()` inside `RescueQuest1.spawn()`. The hypothesis is that
the engine sends the entity snapshot to the client during the same tick as `spawnNPC()`,
before `PlayerSkinComponent` is written. Deferring to the next tick should guarantee the
spawn packet is sent first. **Still needs in-game validation** — the bug is rare enough
that absence of reports ≠ confirmed fixed.

---

## Follower NPC loses skin after server restart

**Status: FIXED (session 10)**

**Root cause:** `PlayerSkinComponent` is not persisted to BSON. On reload, the NPC has
`VillagerData` (with `skinSeed`) but no skin component. `VillageTickHandler.restoreVillagerSkins`
rebuilds it, but previously only ran when `village.isFounded()` — which is false during
the rescue quest test flow (no village founded yet).

**Fix:** Moved `restoreVillagerSkins()` call above the `isFounded()` guard so it runs
for all players regardless of village state.
