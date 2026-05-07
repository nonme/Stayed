package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.util.TickScheduler;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Teleports a managed NPC to a fixed world position without disturbing its
 * behavior tree, role, leash, Frozen state, or any other runtime invariant.
 *
 * <p>Used by player-facing "Recall" actions when an NPC has wandered into an
 * unreachable spot (cave, water pit) but is still alive and registered. The
 * NPC's own logic — {@code VillagerScheduler} for residents, {@code
 * BuilderBehavior} for the elf, {@code FarmerWorkBehavior} for farmers — picks
 * up from the new position on its next tick and re-derives whatever
 * leash/target/role it needs. We deliberately do not write {@code
 * setLeashPoint} here: a stale leash would tug the NPC back toward the recall
 * point for a frame before its real logic overwrites it, which is exactly the
 * stutter Recall is meant to fix.
 *
 * <p>Lookup follows the contract in {@code STAYED_NPC.md}:
 * <ul>
 *   <li>Resolve through the durable {@code npcId} via {@link
 *       NpcLiveEntityResolver#findLiveNpcByRecord} — {@code entityUuid} can
 *       have changed across respawn/restart and direct {@code
 *       world.getEntity(uuid)} would return null even when the NPC is alive.</li>
 *   <li>If the NPC's chunk is currently unloaded, force-load the record's last
 *       chunk and retry through the same resolver.</li>
 *   <li>After moving, ask {@code NpcPositionTracker} to sync immediately so a
 *       restart while the player is still at the recall site doesn't dump the
 *       NPC back at its pre-teleport position.</li>
 * </ul>
 *
 * <p>Returns immediately when the registry has no record for the UUID — that
 * is a strong signal of a corrupted village state (orphaned {@code
 * assignedVillagerId}) and is handled by {@code cleanupOrphanedAssignments}
 * elsewhere; we just log and skip.
 */
public final class NpcTeleporter {

    private static final Logger LOGGER = Logger.getLogger(NpcTeleporter.class.getName());
    private static final long FORCE_LOAD_DELAY_MS = 1500L;

    private NpcTeleporter() {}

    /**
     * Teleports the NPC identified by {@code knownEntityUuid} to {@code (x, y, z)}.
     * The UUID is treated as a hint — the actual lookup goes through {@link
     * NpcRegistry} so a stale post-respawn UUID still resolves to the right
     * record.
     *
     * @return {@code true} if the move was scheduled, {@code false} on lookup
     *     failure (no registry record / chunk load returned no live entity).
     *     A {@code true} return does not guarantee the chunk load succeeded
     *     yet; check the logs for "no live entity after force-load" follow-ups.
     */
    public static boolean recall(World world, UUID knownEntityUuid, double x, double y, double z) {
        if (knownEntityUuid == null) return false;

        NpcRegistry.NpcRecord record = NpcRegistry.get().getRecord(knownEntityUuid);
        if (record == null) {
            LOGGER.warning("NpcTeleporter.recall: no NpcRecord for uuid=" + knownEntityUuid
                    + " — orphaned village reference, cleanup will free it on the next tick");
            return false;
        }
        return recall(world, record, x, y, z);
    }

    /**
     * Same as {@link #recall(World, UUID, double, double, double)} but takes a
     * resolved {@link NpcRegistry.NpcRecord} directly, skipping the initial
     * UUID → record lookup.
     */
    public static boolean recall(World world, NpcRegistry.NpcRecord record,
                                 double x, double y, double z) {
        if (record == null) return false;
        if (record.broken) {
            LOGGER.warning("NpcTeleporter.recall: record " + record.npcId + " is broken — skipping");
            return false;
        }

        Store<EntityStore> store = world.getEntityStore().getStore();
        Ref<EntityStore> ref = NpcLiveEntityResolver.findLiveNpcByRecord(store, record);
        if (ref != null && ref.isValid()) {
            doMove(world, store, ref, record, x, y, z);
            return true;
        }

        // Live entity not in any loaded chunk. Force-load the chunk we last saw
        // it in and retry through the resolver — bindEntityUuid may have updated
        // the record's entityUuid by then, but findLiveNpcByRecord prefers the
        // durable npcId so we don't depend on that race.
        world.getChunkAsync(record.chunkIndex).thenRun(() -> world.execute(() ->
                TickScheduler.runLater(world, FORCE_LOAD_DELAY_MS, () -> {
                    Store<EntityStore> liveStore = world.getEntityStore().getStore();
                    Ref<EntityStore> reloaded = NpcLiveEntityResolver.findLiveNpcByRecord(liveStore, record);
                    if (reloaded == null || !reloaded.isValid()) {
                        LOGGER.warning("NpcTeleporter.recall: no live entity after force-load npcId="
                                + record.npcId + " — requesting guarded recovery at recall target");
                        NpcMissingEntityRecovery.request(world, record,
                                new NpcMissingEntityRecovery.Target(x, y, z), "recall");
                        return;
                    }
                    doMove(world, liveStore, reloaded, record, x, y, z);
                })));
        return true;
    }

    static void doMove(World world, Store<EntityStore> store, Ref<EntityStore> ref,
                       NpcRegistry.NpcRecord record, double x, double y, double z) {
        // The resolver already gave us a live ref keyed off npcId, so don't go
        // back through world.getEntity(record.entityUuid) — record.entityUuid
        // may not yet match the live UUIDComponent if bindEntityUuid hasn't
        // fired. Read the live UUID off the entity itself.
        var uuidComponent = store.getComponent(ref,
                com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
        UUID liveUuid = uuidComponent != null ? uuidComponent.getUuid() : null;
        Entity entity = liveUuid != null ? world.getEntity(liveUuid) : null;
        if (entity == null) {
            LOGGER.warning("NpcTeleporter.doMove: live ref has no resolvable Entity (npcId="
                    + record.npcId + ", liveUuid=" + liveUuid + ")");
            return;
        }
        removeStaleFrozen(store, ref, record, liveUuid);
        entity.moveTo(ref, x, y, z, store);
        LOGGER.fine("NpcTeleporter.recall: " + record.npcId + " (uuid=" + liveUuid
                + ") → (" + x + "," + y + "," + z + ")");

        // Persist the new position immediately. NpcPositionTracker normally
        // catches this within ~15 ms, but a server restart in that window
        // would otherwise drop the NPC back at its pre-teleport coordinates.
        NpcPositionTracker.requestSync(world);
    }

    private static void removeStaleFrozen(Store<EntityStore> store, Ref<EntityStore> ref,
                                          NpcRegistry.NpcRecord record, UUID liveUuid) {
        if (record.interaction != NpcRegistry.InteractionType.VILLAGER
                && record.interaction != NpcRegistry.InteractionType.RESCUE
                && record.interaction != NpcRegistry.InteractionType.FOLLOWER) {
            return;
        }
        try {
            if (store.getComponent(ref, Frozen.getComponentType()) == null) return;
            store.tryRemoveComponent(ref, Frozen.getComponentType());
            LOGGER.info("NpcTeleporter.recall: removed stale Frozen from npcId="
                    + record.npcId + " uuid=" + liveUuid);
        } catch (Exception e) {
            LOGGER.fine("NpcTeleporter.recall: unfreeze failed for npcId="
                    + record.npcId + ": " + e.getMessage());
        }
    }
}
