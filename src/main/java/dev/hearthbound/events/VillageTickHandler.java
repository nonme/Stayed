package dev.hearthbound.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.npc.NpcManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hearthbound.ui.VillageHud;
import dev.hearthbound.util.TickScheduler;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerData;
import it.unimi.dsi.fastutil.Pair;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

/**
 * Periodic handler for village lifecycle events:
 * - Spawning settlers after Town Hall is built
 * - Updating the village HUD
 */
public class VillageTickHandler {

    private static final Logger LOGGER = Logger.getLogger(VillageTickHandler.class.getName());
    private static final long TICK_INTERVAL_MS = 5000; // Check every 5 seconds
    private static final int SETTLER_SPAWN_TICKS = 20 * 60; // ~60 seconds at 20 ticks/sec
    private static final int MAX_VILLAGERS_STAGE_1 = 1; // One settler for Stage 1
    private static final String VILLAGER_ROLE = "Villager_Human";

    private ScheduledFuture<?> tickTask;
    private VillageHud hud;

    public void start(Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        stop(); // cancel any previous tick before starting a new one

        hud = new VillageHud(player, playerRef);

        tickTask = TickScheduler.runRepeating(world, TICK_INTERVAL_MS, TICK_INTERVAL_MS, () -> {
            // Always get the fresh store from the world — never use the captured store
            // from start(), as it may belong to a different world after reconnect.
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

        // Update HUD
        if (hud != null) {
            hud.refresh(store);
        }

        // Reapply persistent-but-not-yet-skinned villagers. This runs every tick so settlers
        // in chunks that load late (server restart, player roamed away and came back) eventually
        // get their skin rebuilt from the stored seed. villagerCount is a fallback index — the
        // original spawn index isn't stored on the entity, and using the current count keeps
        // legacy villagers eligible for the full archetype range instead of pinning them to
        // Peasant. Post-normalization the archetype gate is probabilistic anyway.
        restoreVillagerSkins(store, village.getVillagerCount());

        // Check if we should spawn a settler (persistent check via villagerCount)
        if (village.getStage() >= VillageData.STAGE_TOWN_HALL
                && village.getVillagerCount() < MAX_VILLAGERS_STAGE_1) {
            long elapsed = world.getTick() - village.getFoundedAtTick();
            if (elapsed > SETTLER_SPAWN_TICKS) {
                spawnSettler(store, playerRef, village, world);
            }
        }
    }

    /**
     * Finds villagers in loaded chunks that have {@link VillagerData} but no active
     * {@link com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent}
     * and reapplies the stored skin seed. Also fills in a seed + name for any legacy
     * villagers that predate the skin system.
     */
    private static void restoreVillagerSkins(Store<EntityStore> store, int fallbackVillagerIndex) {
        try {
            java.util.List<Ref<EntityStore>> needsSkin = new java.util.ArrayList<>();
            java.util.List<Long> seeds = new java.util.ArrayList<>();
            java.util.List<Ref<EntityStore>> needsSeed = new java.util.ArrayList<>();

            com.hypixel.hytale.component.Archetype<EntityStore> query =
                    com.hypixel.hytale.component.Archetype.of(VillagerData.getComponentType());
            store.forEachChunk(query, (chunk, buffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    try {
                        VillagerData vd = chunk.getComponent(i, VillagerData.getComponentType());
                        if (vd == null) continue;
                        Ref<EntityStore> ref = chunk.getReferenceTo(i);
                        if (ref == null) continue;

                        if (vd.getSkinSeed() == 0L) {
                            needsSeed.add(ref);
                            continue;
                        }

                        var skinComp = chunk.getComponent(i,
                                com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
                                        .getComponentType());
                        if (skinComp == null) {
                            needsSkin.add(ref);
                            seeds.add(vd.getSkinSeed());
                        }
                    } catch (Exception ignored) {}
                }
            });

            for (int i = 0; i < needsSkin.size(); i++) {
                dev.hearthbound.npc.VillagerAppearance.apply(
                        needsSkin.get(i), store, seeds.get(i), fallbackVillagerIndex);
            }

            for (Ref<EntityStore> ref : needsSeed) {
                long seed = java.util.concurrent.ThreadLocalRandom.current().nextLong();
                String[] name = dev.hearthbound.npc.VillagerNames.rollHumanName(seed);
                VillagerData vd = store.getComponent(ref, VillagerData.getComponentType());
                if (vd == null) continue;
                vd.setSkinSeed(seed);
                if (vd.getFirstName() == null || vd.getFirstName().isEmpty()) {
                    vd.setFirstName(name[0]);
                }
                if (vd.getLastName() == null || vd.getLastName().isEmpty()) {
                    vd.setLastName(name[1]);
                }
                store.putComponent(ref, VillagerData.getComponentType(), vd);
                dev.hearthbound.npc.VillagerAppearance.apply(ref, store, seed, fallbackVillagerIndex);
            }

            if (!needsSkin.isEmpty() || !needsSeed.isEmpty()) {
                LOGGER.info("Restored villager skins: " + needsSkin.size() +
                        " by seed, " + needsSeed.size() + " legacy (new seed assigned)");
            }
        } catch (Exception e) {
            LOGGER.warning("restoreVillagerSkins failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("removal")
    private void spawnSettler(Store<EntityStore> store, Ref<EntityStore> playerRef,
                              VillageData village, World world) {
        int thX = village.getFoundingStoneX();
        int thY = village.getFoundingStoneY();
        int thZ = village.getFoundingStoneZ();

        // Spawn settler offset from Town Hall
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

        // Index is the count *before* this villager — so the first settler is index 0.
        int villagerIndex = village.getVillagerCount();
        dev.hearthbound.npc.VillagerAppearance.apply(npcRef, store, skinSeed, villagerIndex);

        village.setVillagerCount(villagerIndex + 1);
        VillageManager.get().save(store, playerRef, village);

        LOGGER.info("Human settler spawned near Town Hall: " + villagerData.getFullName() +
                " (seed=" + skinSeed + ", total villagers=" + village.getVillagerCount() + ")");
    }
}
