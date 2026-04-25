package dev.hearthbound.events;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;
import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.quest.RescueQuest1;
import dev.hearthbound.ui.VillageHud;
import dev.hearthbound.util.TickScheduler;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerData;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

/**
 * Periodic handler for village lifecycle events:
 * - Spawning settlers after Town Hall is built
 * - Converting rescue followers who reach the founding stone into villagers
 * - Updating the village HUD
 *
 * NPC skin restoration on chunk load is handled by NpcChunkLoadHandler
 * (ChunkPreLoadProcessEvent), not here.
 */
public class VillageTickHandler {

    private static final Logger LOGGER = Logger.getLogger(VillageTickHandler.class.getName());
    private static final long TICK_INTERVAL_MS = 5000;
    private static final int SETTLER_SPAWN_TICKS = 20 * 60;
    private static final int MAX_VILLAGERS_STAGE_1 = 1;
    private static final String VILLAGER_ROLE = "Villager_Human";
    private static final String FOLLOWER_ROLE = "Villager_Rescue_Follower";
    private static final double FOLLOWER_ARRIVAL_RADIUS = 5.0;

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

        checkFollowerArrival(store, playerRef, village, world);

        if (village.getStage() >= VillageData.STAGE_TOWN_HALL
                && village.getVillagerCount() < MAX_VILLAGERS_STAGE_1) {
            long elapsed = world.getTick() - village.getFoundedAtTick();
            if (elapsed > SETTLER_SPAWN_TICKS) {
                spawnSettler(store, playerRef, village, world);
            }
        }
    }

    /**
     * Scans all NPCs with the follower role and checks if any have reached the founding stone.
     * Converts the first arriving follower to a full villager.
     */
    private void checkFollowerArrival(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                       VillageData village, World world) {
        double stoneX = village.getFoundingStoneX();
        double stoneY = village.getFoundingStoneY();
        double stoneZ = village.getFoundingStoneZ();

        Archetype<EntityStore> query = Archetype.of(NPCEntity.getComponentType());
        List<Ref<EntityStore>> arrivals = new ArrayList<>();

        store.forEachChunk(query, (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                try {
                    NPCEntity npcEntity = chunk.getComponent(i, NPCEntity.getComponentType());
                    if (npcEntity == null || !FOLLOWER_ROLE.equals(npcEntity.getRoleName())) continue;

                    TransformComponent tc = chunk.getComponent(i, TransformComponent.getComponentType());
                    if (tc == null || tc.getPosition() == null) continue;

                    Vector3d pos = tc.getPosition();
                    double dx = pos.getX() - stoneX;
                    double dy = pos.getY() - stoneY;
                    double dz = pos.getZ() - stoneZ;
                    double distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq <= FOLLOWER_ARRIVAL_RADIUS * FOLLOWER_ARRIVAL_RADIUS) {
                        Ref<EntityStore> ref = chunk.getReferenceTo(i);
                        if (ref != null) arrivals.add(ref);
                    }
                } catch (Exception ignored) {}
            }
        });

        for (Ref<EntityStore> followerRef : arrivals) {
            convertFollowerToVillager(store, playerRef, followerRef, world);
        }
    }

    private void convertFollowerToVillager(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                            Ref<EntityStore> followerRef, World world) {
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

            VillageData village = VillageManager.get().getOrCreateVillageData(store, playerRef);
            village.setVillagerCount(village.getVillagerCount() + 1);
            VillageManager.get().save(store, playerRef, village);

            LOGGER.info("Rescue follower converted to villager (total: " + village.getVillagerCount() + ")");
        } catch (Exception e) {
            LOGGER.warning("convertFollowerToVillager failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("removal")
    private void spawnSettler(Store<EntityStore> store, Ref<EntityStore> playerRef,
                              VillageData village, World world) {
        int thX = village.getFoundingStoneX();
        int thY = village.getFoundingStoneY();
        int thZ = village.getFoundingStoneZ();

        Vector3d spawnPos = new Vector3d(thX + 15, thY, thZ + 10);

        Pair<Ref<EntityStore>, INonPlayerCharacter> result = NpcManager.spawnNpc(
                store, spawnPos, new Vector3f(0, 0, 0), VILLAGER_ROLE);

        if (result == null) {
            LOGGER.warning("Failed to spawn settler");
            return;
        }

        Ref<EntityStore> npcRef = result.first();
        long skinSeed = java.util.concurrent.ThreadLocalRandom.current().nextLong();
        String[] name = dev.hearthbound.npc.VillagerNames.rollHumanName(skinSeed);

        VillagerData villagerData = new VillagerData(VillagerData.RACE_HUMAN, name[0], name[1], skinSeed);
        villagerData.setHappiness(50);
        store.putComponent(npcRef, VillagerData.getComponentType(), villagerData);

        java.util.UUID npcUuid = NpcManager.extractUuid(store, npcRef);
        if (npcUuid != null) {
            long chunkIndex = NpcManager.chunkIndexFor(spawnPos);
            dev.hearthbound.npc.NpcRegistry.NpcRecord record = new dev.hearthbound.npc.NpcRegistry.NpcRecord(
                    npcUuid, VILLAGER_ROLE, dev.hearthbound.npc.NpcRegistry.InteractionType.NONE, skinSeed, chunkIndex);
            dev.hearthbound.npc.NpcRegistry.get().register(record);
            dev.hearthbound.npc.NpcRestorer.restore(npcRef, store, world, record);
        } else {
            // Fallback: apply skin directly if UUID extraction failed
            int villagerIndex = village.getVillagerCount();
            dev.hearthbound.npc.VillagerAppearance.apply(npcRef, store, skinSeed, villagerIndex);
        }

        village.setVillagerCount(village.getVillagerCount() + 1);
        VillageManager.get().save(store, playerRef, village);

        LOGGER.info("Human settler spawned near Town Hall: " + villagerData.getFullName() +
                " (seed=" + skinSeed + ", total villagers=" + village.getVillagerCount() + ")");
    }
}
