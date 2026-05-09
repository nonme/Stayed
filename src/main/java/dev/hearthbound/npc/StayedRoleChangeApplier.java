package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;
import dev.hearthbound.util.TickScheduler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Durable role-change path for registry-backed Stayed NPCs.
 *
 * Registry/data.json are updated before the live engine role is changed. If the
 * generated role has not been indexed yet, a retry task applies it later; if the
 * server stops first, chunk-load restoration sees the desired baseRoleName and
 * schedules the same application again.
 */
public final class StayedRoleChangeApplier {

    private static final Logger LOGGER = Logger.getLogger(StayedRoleChangeApplier.class.getName());
    private static final long RETRY_MS = 500L;
    private static final int MAX_RETRIES = 120;
    private static final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private static final Set<String> verifying = ConcurrentHashMap.newKeySet();

    private StayedRoleChangeApplier() {}

    public static boolean persistAndApply(Ref<EntityStore> ref, Store<EntityStore> store,
                                          World world, NpcRegistry.NpcRecord updated,
                                          boolean changeAppearance, String reason) {
        if (updated == null) return false;
        updated.refreshGeneratedRoleName();
        StayedRoleGenerator.get().generateRoleIfChanged(updated);
        NpcRegistry.get().updateRecord(updated);
        HearthboundDataStore.get().save();
        return applyOrSchedule(ref, store, world, updated, changeAppearance, reason);
    }

    public static boolean applyOrSchedule(Ref<EntityStore> ref, Store<EntityStore> store,
                                          World world, NpcRegistry.NpcRecord record,
                                          boolean changeAppearance, String reason) {
        if (record == null || world == null) return false;
        record.refreshGeneratedRoleName();
        String generatedRole = StayedRoleGenerator.get().generateRoleIfChanged(record);
        if (store == null) {
            schedule(record.npcId, generatedRole, world, changeAppearance, reason);
            return false;
        }

        Ref<EntityStore> liveRef = resolveLiveRef(ref, store, record);
        if (liveRef == null || !liveRef.isValid()) {
            schedule(record.npcId, generatedRole, world, changeAppearance, reason);
            return false;
        }

        NPCEntity npc = store.getComponent(liveRef, NPCEntity.getComponentType());
        if (npc == null || npc.getRole() == null) {
            schedule(record.npcId, generatedRole, world, changeAppearance, reason);
            return false;
        }

        String liveRole = npc.getRole().getRoleName();
        if (generatedRole.equals(liveRole)) {
            NpcRestorer.restoreAfterRoleChange(liveRef, world, record);
            return true;
        }

        int roleIndex = NPCPlugin.get().getIndex(generatedRole);
        if (roleIndex < 0) {
            LOGGER.info("[STAYED-ROLECHANGE] generated role not indexed yet; pending"
                    + " npcId=" + record.npcId
                    + " generatedRole=" + generatedRole
                    + " liveRole=" + liveRole
                    + " reason=" + reason);
            schedule(record.npcId, generatedRole, world, changeAppearance, reason);
            return false;
        }

        RoleChangeSystem.requestRoleChange(liveRef, npc.getRole(), roleIndex, changeAppearance, store);
        NpcRestorer.restoreAfterRoleChange(liveRef, world, record);
        scheduleVerification(record.npcId, generatedRole, world, changeAppearance, reason);
        LOGGER.info("[STAYED-ROLECHANGE] applied npcId=" + record.npcId
                + " liveRole=" + liveRole
                + " generatedRole=" + generatedRole
                + " reason=" + reason);
        return true;
    }

    private static Ref<EntityStore> resolveLiveRef(Ref<EntityStore> ref, Store<EntityStore> store,
                                                   NpcRegistry.NpcRecord record) {
        if (ref != null && ref.isValid()) return ref;
        return NpcLiveEntityResolver.findLiveNpcByRecord(store, record);
    }

    private static void schedule(String npcId, String generatedRole, World world,
                                 boolean changeAppearance, String reason) {
        if (npcId == null || npcId.isBlank() || world == null) return;
        String key = npcId + ":" + generatedRole;
        if (!inFlight.add(key)) return;

        int[] attempts = {0};
        ScheduledFuture<?>[] task = {null};
        task[0] = TickScheduler.getExecutor().scheduleAtFixedRate(() ->
            world.execute(() -> {
                try {
                    NpcRegistry.NpcRecord record = NpcRegistry.get().getRecordByNpcId(npcId);
                    if (record == null) {
                        cancel(key, task[0]);
                        return;
                    }
                    Store<EntityStore> liveStore = world.getEntityStore().getStore();
                    Ref<EntityStore> liveRef = NpcLiveEntityResolver.findLiveNpcByRecord(liveStore, record);
                    if (liveRef != null && liveRef.isValid()) {
                        boolean applied = applyOrSchedule(liveRef, liveStore, world, record,
                                changeAppearance, reason + ":retry");
                        NPCEntity npc = liveStore.getComponent(liveRef, NPCEntity.getComponentType());
                        String liveRole = npc != null && npc.getRole() != null
                                ? npc.getRole().getRoleName() : null;
                        if (applied || generatedRole.equals(liveRole)) {
                            cancel(key, task[0]);
                            return;
                        }
                    }
                    if (++attempts[0] >= MAX_RETRIES) {
                        LOGGER.warning("[STAYED-ROLECHANGE] pending role not applied before timeout"
                                + " npcId=" + npcId
                                + " generatedRole=" + generatedRole
                                + " reason=" + reason);
                        cancel(key, task[0]);
                    }
                } catch (RuntimeException e) {
                    LOGGER.warning("[STAYED-ROLECHANGE] retry failed npcId=" + npcId
                            + " generatedRole=" + generatedRole
                            + " reason=" + reason
                            + " error=" + e.getMessage());
                }
            }),
        RETRY_MS, RETRY_MS, TimeUnit.MILLISECONDS);
    }

    private static void scheduleVerification(String npcId, String generatedRole, World world,
                                             boolean changeAppearance, String reason) {
        if (npcId == null || npcId.isBlank() || generatedRole == null || world == null) return;
        String key = npcId + ":" + generatedRole;
        if (!verifying.add(key)) return;

        TickScheduler.getExecutor().schedule(() ->
            world.execute(() -> {
                verifying.remove(key);
                NpcRegistry.NpcRecord record = NpcRegistry.get().getRecordByNpcId(npcId);
                if (record == null) return;
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                Ref<EntityStore> liveRef = NpcLiveEntityResolver.findLiveNpcByRecord(liveStore, record);
                if (liveRef == null || !liveRef.isValid()) {
                    schedule(npcId, generatedRole, world, changeAppearance, reason + ":verify-missing");
                    return;
                }
                NPCEntity npc = liveStore.getComponent(liveRef, NPCEntity.getComponentType());
                String liveRole = npc != null && npc.getRole() != null
                        ? npc.getRole().getRoleName() : null;
                if (!generatedRole.equals(liveRole)) {
                    LOGGER.warning("[STAYED-ROLECHANGE] requested role did not settle; retrying"
                            + " npcId=" + npcId
                            + " generatedRole=" + generatedRole
                            + " liveRole=" + liveRole
                            + " reason=" + reason);
                    schedule(npcId, generatedRole, world, changeAppearance, reason + ":verify");
                }
            }),
        1500L, TimeUnit.MILLISECONDS);
    }

    private static void cancel(String key, ScheduledFuture<?> task) {
        inFlight.remove(key);
        if (task != null) task.cancel(false);
    }
}
