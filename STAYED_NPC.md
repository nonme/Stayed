# Stayed NPC Guide

This document describes the current contract for Hearthbound/Stayed-managed NPCs.
Follow it when adding new NPC flows, moving existing NPCs, or changing rescue,
village, Aelin, cleanup, or persistence code.

## Core Contract

Stayed NPC persistence has two identities:

- `npcId`: stable plugin identity. This is the primary identity for our code.
  It is stored on the entity as `StayedNpcIdentityComponent` / `HB_NPCID` and in
  `NpcRegistry.NpcRecord.npcId`.
- `entityUuid`: current engine entity UUID. This can change after respawn,
  recovery, or engine deserialization. Do not use it as durable identity.

Every NPC that should survive chunk unload/reload, server restart, cleanup, or
duplicate prevention must:

- have a `NpcRegistry.NpcRecord`;
- carry `HB_NPCID` on the live entity;
- be registered through `NpcRegistry.get().registerWithIdentity(store, ref, record)`;
- have a meaningful `InteractionType` unless it is intentionally disposable.

The registry is persisted to `mods/HearthboundData/data.json` by
`HearthboundDataStore`. Dirty data is flushed every 5 seconds and again during
plugin/server shutdown. Writes are atomic through `data.json.tmp` + atomic move.

## Managed vs Unmanaged NPCs

Use managed NPCs for:

- Aelin / elf sage;
- rescued villagers;
- village residents, builders, followers, and workers;
- rescue victims;
- quest enemies only when we need duplicate prevention or cleanup bookkeeping.

Leave entities unmanaged when they are normal game NPCs/mobs and Hearthbound
does not need to restore, clean up, deduplicate, or attach interactions to them.
Unmanaged entities must not carry `HB_NPCID`.

`NpcRegistry.InteractionType` means:

- `ELF`: Aelin interaction and appearance restoration.
- `RESCUE`: rescue victim interaction.
- `VILLAGER`: regular village resident interaction.
- `FOLLOWER`: managed entity with no F-key interaction.
- `NONE`: managed only for bookkeeping/cleanup. These records are not resurrected
  by `NpcChunkLoadHandler`.

Use `NONE` carefully. It is appropriate for disposable quest enemies such as
rescue goblins. It is not appropriate for a villager the player owns.

## Spawning

Do this for every new managed NPC:

1. Spawn the engine NPC normally.
2. Read its current `UUIDComponent`.
3. Create a `NpcRegistry.NpcRecord` with the correct role, interaction, skin
   seed, and chunk.
4. Set `record.setPosition(x, y, z)` as soon as the spawn position is known.
5. Call `NpcRegistry.get().registerWithIdentity(store, ref, record)`.
6. Call `HearthboundDataStore.get().markDirty()` or `save()` if the NPC must be
   persisted immediately.
7. Apply appearance/interaction through the normal restoration path when needed.

Do not spawn a persistent NPC and then only store its `entityUuid` somewhere
else. If it does not have `HB_NPCID`, chunk-load recovery cannot reliably match
the entity back to the registry.

## Moving and Teleporting

For loaded, registered NPCs, normal movement should be picked up automatically.
`NpcPositionTracker` runs every 15 ms on the world thread, reads live
`TransformComponent`, and updates the record's last position and chunk when the
NPC moved enough to matter:

- at least 1 block on X or Z;
- or at least 2 blocks on Y.

That means walking across chunk boundaries, patrol movement, and ordinary
teleports should normally be captured without manual registry edits.

For large scripted movement or tests where the next assertion depends on the
new position immediately:

- from async callbacks, call `NpcPositionTracker.requestSync(world)`;
- only from the world thread, call `NpcPositionTracker.syncLoadedNow(world, store)`;
- if `syncLoadedNow` returns `true`, call `HearthboundDataStore.get().markDirty()`.

Do not call `syncLoadedNow` from chunk preload async threads. It reads the live
entity `Store`, and the engine asserts if this happens off the world thread.
Use `requestSync(world)` instead.

Moving an unloaded NPC is different. Updating only `NpcRecord.lastX/Y/Z` changes
our fallback restore position, but it does not move the sleeping entity already
stored in a chunk. Prefer loading the entity's chunk, moving the live entity, and
letting `NpcPositionTracker` persist the result. Only edit registry position
directly when you intentionally want to change the future respawn/fallback anchor.

## Role Changes

Role changes can reset skin, inventory, and interaction components. After a role
change:

1. Keep the same `npcId`.
2. Update the registry through `NpcRegistry.get().updateRecord(newRecord)`.
3. Preserve or copy base position from the old record.
4. Call `NpcRestorer.restoreAfterRoleChange(ref, world, record)`.
5. Mark the data store dirty.

Do not create a fresh record with a fresh `npcId` for the same villager. That is
how duplicate/orphan paths start.

## Chunk Load and Restart Behavior

`NpcChunkLoadHandler` is the main restore path. On chunk preload it:

- sanitizes broken persistent models;
- queues pending removals for deletion;
- scans chunk entity holders;
- reads `HB_NPCID`;
- matches entities to `NpcRegistry` by `npcId`;
- rebinds `entityUuid` if the engine UUID changed;
- removes entities that carry `HB_NPCID` but have no registry record;
- restores skin and interaction;
- schedules a fresh spawn only after trying to resolve an existing live entity.

Restore candidates are collected from:

- the record's current saved chunk;
- the record's base/home chunk;
- entities already present in the loading chunk.

Fresh spawn is not allowed for `InteractionType.NONE`; those entities are
disposable bookkeeping records.

Use `NpcLiveEntityResolver.findLiveNpcByRecord(store, record)` when a direct
`world.getEntityRef(record.entityUuid)` lookup may be stale around chunk load,
chunk transfer, or restart boundaries.

`NpcMissingEntityRecovery` is the last-resort restore path for records that are
already known to be missing while their saved chunk is loaded. It is used by
Recall and by the periodic self-check. It must always resolve by `npcId` before
spawning, load both the current and base chunks, keep only one in-flight recovery
per `npcId`, and rate-limit spawn attempts. Do not add other ad hoc respawn
paths; route them through this service so duplicate prevention stays centralized.

## Deletion and Cleanup

For managed NPC deletion:

- remove the live entity when it is loaded;
- unregister the record by `npcId` or `entityUuid`;
- if the physical entity may be unloaded, call
  `NpcRegistry.get().markForRemoval(entityUuid, chunkIndex)`;
- mark dirty or save after mutating registry/removal state.

Pending removals are persisted. This is intentional: reset/test cleanup can
delete the registry while the physical entity is asleep in an unloaded chunk, and
the tombstone must survive restart.

`/hb reset` goes through `ResetVillageService` and is the reference path for a
full village/NPC cleanup.

## Do Not Do This

- Do not treat `entityUuid` as stable long-term identity.
- Do not spawn a persistent NPC without `registerWithIdentity`.
- Do not manually write `HB_NPCID` without also creating/updating the registry
  record.
- Do not remove registry records without handling the live entity or pending
  removal.
- Do not read the live entity `Store` from async chunk preload callbacks.
- Do not resurrect `InteractionType.NONE` records.
- Do not bypass `NpcRestorer` after role changes or chunk recovery if the NPC
  needs skin or F-key interaction.
- Do not use ad hoc cleanup that only edits `VillageData`; villagers also need
  registry and entity cleanup.

## Testing Checklist

After changes touching NPC spawn, movement, chunk persistence, rescue quests, or
cleanup, run:

```text
/hb test all
```

This runs the non-restart packages and prints a summary table at the end.

Restart persistence is manual:

```text
/hb test run restart_prepare
```

Then restart the server and run:

```text
/hb test run restart_verify
```

For focused checks, use:

```text
/hb test pkg smoke
/hb test pkg housing
/hb test pkg chunks
/hb test pkg roles
/hb test pkg aelin
/hb test pkg rescue
```

The important invariant is always the same: no duplicate managed NPCs, no
orphan entities carrying `HB_NPCID` without a registry record, no missing live
entity for a loaded registered persistent NPC, and skin/interaction restored
after chunk reload and restart.
