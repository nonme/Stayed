package dev.hearthbound.events;

import com.hypixel.hytale.builtin.adventure.objectives.Objective;
import com.hypixel.hytale.builtin.adventure.objectives.ObjectivePlugin;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;
import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.quest.RescueQuest1;
import dev.hearthbound.ui.RescueDialogPage;
import dev.hearthbound.ui.VillageHud;
import dev.hearthbound.util.TickScheduler;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerData;
import dev.hearthbound.village.VillagerSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

/**
 * Periodic handler for village lifecycle events:
 * - Converting any live rescue followers into villagers (no proximity check —
 *   a follower has already agreed to join, position doesn't matter)
 * - Updating the village HUD
 *
 * NPC skin restoration on chunk load is handled by NpcChunkLoadHandler.
 */
public class VillageTickHandler {

    private static final Logger LOGGER = Logger.getLogger(VillageTickHandler.class.getName());
    private static final long TICK_INTERVAL_MS = 5000;
    private static final String VILLAGER_ROLE = "Villager_Human";
    private static final String FOLLOWER_ROLE = "Villager_Rescue_Follower";

    private ScheduledFuture<?> tickTask;
    private VillageHud hud;

    public void start(Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        stop();

        hud = new VillageHud(player, playerRef);

        tickTask = TickScheduler.runRepeating(world, TICK_INTERVAL_MS, TICK_INTERVAL_MS, () -> {
            Store<EntityStore> liveStore = world.getEntityStore().getStore();
            tick(liveStore, playerRef, world);
        });

        LOGGER.info("VillageTickHandler started");
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel(false);
            tickTask = null;
        }
    }

    public VillageHud getHud() {
        return hud;
    }

    private void tick(Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        if (!playerRef.isValid()) {
            stop();
            return;
        }

        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null || !village.isFounded()) return;

        if (hud != null) {
            hud.refresh(store);
        }

        convertAllFollowers(store, playerRef, village, world);
    }

    private void convertAllFollowers(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                     VillageData village, World world) {
        // Don't convert while the player is still walking back — wait for Return objective to finish
        if (isReturnObjectiveActive(store, playerRef)) return;

        Archetype<EntityStore> query = Archetype.of(NPCEntity.getComponentType());
        List<Ref<EntityStore>> followers = new ArrayList<>();

        store.forEachChunk(query, (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                try {
                    NPCEntity npcEntity = chunk.getComponent(i, NPCEntity.getComponentType());
                    if (npcEntity != null && FOLLOWER_ROLE.equals(npcEntity.getRoleName())) {
                        followers.add(chunk.getReferenceTo(i));
                    }
                } catch (Exception ignored) {}
            }
        });

        for (Ref<EntityStore> followerRef : followers) {
            convertFollowerToVillager(store, playerRef, followerRef, village, world);
        }
    }

    /** Returns true while Objective_RescueTrap_Return is still in the player's active set. */
    private boolean isReturnObjectiveActive(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) return false;
        var activeUuids = player.getPlayerConfigData().getActiveObjectiveUUIDs();
        if (activeUuids == null || activeUuids.isEmpty()) return false;
        for (UUID uuid : activeUuids) {
            Objective obj = ObjectivePlugin.get().getObjectiveDataStore().getObjective(uuid);
            if (obj != null && RescueDialogPage.OBJECTIVE_RETURN_ID.equals(obj.getObjectiveId())) {
                return true;
            }
        }
        return false;
    }

    private void convertFollowerToVillager(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                           Ref<EntityStore> followerRef, VillageData village, World world) {
        try {
            NPCEntity npcEntity = store.getComponent(followerRef, NPCEntity.getComponentType());
            if (npcEntity == null) return;

            int villagerRoleIndex = NPCPlugin.get().getIndex(VILLAGER_ROLE);
            if (villagerRoleIndex < 0) {
                LOGGER.warning("convertFollowerToVillager: role '" + VILLAGER_ROLE + "' not found");
                return;
            }

            // changeAppearance=false — keep the PlayerSkin assigned at rescue time
            RoleChangeSystem.requestRoleChange(followerRef, npcEntity.getRole(), villagerRoleIndex, false, store);
            RescueQuest1.unregisterNpc(followerRef);

            // Save village data immediately — before the deferred moveTo
            VillagerData villagerData = store.getComponent(followerRef, VillagerData.getComponentType());
            UUID followerUuid = NpcManager.extractUuid(store, followerRef);
            VillagerSummary summary = villagerData != null
                    ? new VillagerSummary(villagerData)
                    : new VillagerSummary();
            summary.setVillagerUuid(followerUuid);
            village.addVillager(summary);
            VillageManager.get().save(store, playerRef, village);

            LOGGER.info(() -> "Rescue follower converted to villager (total: " + village.getVillagerCount() + ")");

            // On the next tick (after RoleChangeSystem applies the new role):
            // - if the villager is far from the village, teleport them next to the player
            //   (player just returned, so they're standing in open space — safe to land near)
            // - always update leashPoint to the founding stone so WanderInCircle stays in village
            double leashX = village.getFoundingStoneX() + 0.5;
            double leashY = village.getFoundingStoneY() + 0.1;
            double leashZ = village.getFoundingStoneZ() + 0.5;

            world.execute(() -> {
                if (followerUuid == null) return;
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                Entity entity = world.getEntity(followerUuid);
                if (entity == null || !followerRef.isValid()) return;

                var tcType = com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType();
                var followerTc = liveStore.getComponent(followerRef, tcType);
                boolean farFromVillage = true;
                if (followerTc != null) {
                    var pos = followerTc.getPosition();
                    double dx = pos.getX() - leashX;
                    double dz = pos.getZ() - leashZ;
                    farFromVillage = dx * dx + dz * dz > 40.0 * 40.0;
                }

                if (farFromVillage) {
                    // Land next to the player — they're standing in open space near the village
                    var playerTc = liveStore.getComponent(playerRef, tcType);
                    if (playerTc != null) {
                        var playerPos = playerTc.getPosition();
                        entity.moveTo(followerRef,
                                playerPos.getX() + 2.0, playerPos.getY(), playerPos.getZ() + 2.0,
                                liveStore);
                        LOGGER.fine("Teleported new villager next to player (was far from village)");
                    }
                }

                // Always anchor wander around the founding stone
                NPCEntity liveNpcEntity = liveStore.getComponent(followerRef, NPCEntity.getComponentType());
                if (liveNpcEntity != null) {
                    liveNpcEntity.setLeashPoint(new Vector3d(leashX, leashY, leashZ));
                }
            });

        } catch (Exception e) {
            LOGGER.warning("convertFollowerToVillager failed: " + e.getMessage());
        }
    }
}
