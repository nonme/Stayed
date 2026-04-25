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

import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

/**
 * Periodic handler for village lifecycle events:
 * - Spawning settlers after Town Hall is built
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

        if (village.getStage() >= VillageData.STAGE_TOWN_HALL
                && village.getVillagerCount() < MAX_VILLAGERS_STAGE_1) {
            long elapsed = world.getTick() - village.getFoundedAtTick();
            if (elapsed > SETTLER_SPAWN_TICKS) {
                spawnSettler(store, playerRef, village, world);
            }
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

        int villagerIndex = village.getVillagerCount();
        dev.hearthbound.npc.VillagerAppearance.apply(npcRef, store, skinSeed, villagerIndex);

        village.setVillagerCount(villagerIndex + 1);
        VillageManager.get().save(store, playerRef, village);

        LOGGER.info("Human settler spawned near Town Hall: " + villagerData.getFullName() +
                " (seed=" + skinSeed + ", total villagers=" + village.getVillagerCount() + ")");
    }
}
