package dev.hearthbound.events;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.npc.ElfSage;
import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.npc.VillagerAppearance;
import dev.hearthbound.npc.VillagerNames;
import dev.hearthbound.quest.RescueQuest1;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Restores NPC skins, interactions, and spawns deferred NPCs whenever a chunk loads.
 *
 * ChunkPreLoadProcessEvent fires BEFORE BSON entities are deserialized into the chunk.
 * We delay 600ms (matching KyuubiSoft's pattern) so entities are actually present
 * in the store when we query them.
 *
 * This is the single authoritative place for "chunk just loaded → fix NPC state".
 * No polling loops, no retry timers elsewhere.
 */
public class NpcChunkLoadHandler {

    private static final Logger LOGGER = Logger.getLogger(NpcChunkLoadHandler.class.getName());

    // Must be long enough for BSON deserialization to complete after ChunkPreLoad.
    // KyuubiSoft uses 500ms; 600ms gives a small extra margin.
    private static final long RESTORE_DELAY_MS = 600;

    public static void onChunkLoad(ChunkPreLoadProcessEvent event) {
        WorldChunk chunk = event.getChunk();
        if (chunk == null) return;
        World world = chunk.getWorld();
        if (world == null) return;

        long chunkIndex = chunk.getIndex();

        dev.hearthbound.util.TickScheduler.runLater(world, RESTORE_DELAY_MS, () -> {
            try {
                Store<EntityStore> store = world.getEntityStore().getStore();
                restoreVillagerSkinsInChunk(store, chunkIndex);
                handleElfInChunk(store, world, chunkIndex);
            } catch (Exception e) {
                LOGGER.warning("NpcChunkLoadHandler failed for chunk " + chunkIndex + ": " + e.getMessage());
            }
        });
    }

    /**
     * For each villager NPC in this chunk that has VillagerData but no PlayerSkinComponent,
     * reapplies the stored skin seed. Also seeds legacy NPCs that predate the skin system.
     */
    private static void restoreVillagerSkinsInChunk(Store<EntityStore> store, long chunkIndex) {
        List<Ref<EntityStore>> needsSkin = new ArrayList<>();
        List<Long> seeds = new ArrayList<>();
        List<Ref<EntityStore>> needsSeed = new ArrayList<>();

        Archetype<EntityStore> query = Archetype.of(VillagerData.getComponentType());
        store.forEachChunk(query, (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                try {
                    Ref<EntityStore> ref = chunk.getReferenceTo(i);
                    if (ref == null) continue;

                    TransformComponent tc = chunk.getComponent(i, TransformComponent.getComponentType());
                    if (tc == null || tc.getPosition() == null) continue;
                    var pos = tc.getPosition();
                    if (ChunkUtil.indexChunkFromBlock((int) pos.x, (int) pos.z) != chunkIndex) continue;

                    VillagerData vd = chunk.getComponent(i, VillagerData.getComponentType());
                    if (vd == null) continue;

                    if (vd.getSkinSeed() == 0L) {
                        needsSeed.add(ref);
                        continue;
                    }

                    if (chunk.getComponent(i, PlayerSkinComponent.getComponentType()) == null) {
                        needsSkin.add(ref);
                        seeds.add(vd.getSkinSeed());
                    }
                } catch (Exception ignored) {}
            }
        });

        for (int i = 0; i < needsSkin.size(); i++) {
            Ref<EntityStore> ref = needsSkin.get(i);
            VillagerAppearance.apply(ref, store, seeds.get(i), 0);
            // Trapped villagers also need their rescue interaction restored.
            NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
            if (npcEntity != null && RescueQuest1.VICTIM_ROLE.equals(npcEntity.getRoleName())) {
                NpcManager.assignRescueInteraction(store, ref);
            }
        }

        for (Ref<EntityStore> ref : needsSeed) {
            long seed = ThreadLocalRandom.current().nextLong();
            String[] name = VillagerNames.rollHumanName(seed);
            VillagerData vd = store.getComponent(ref, VillagerData.getComponentType());
            if (vd == null) continue;
            vd.setSkinSeed(seed);
            if (vd.getFirstName() == null || vd.getFirstName().isEmpty()) vd.setFirstName(name[0]);
            if (vd.getLastName() == null || vd.getLastName().isEmpty()) vd.setLastName(name[1]);
            store.putComponent(ref, VillagerData.getComponentType(), vd);
            VillagerAppearance.apply(ref, store, seed, 0);
        }

        if (!needsSkin.isEmpty() || !needsSeed.isEmpty()) {
            LOGGER.info("Chunk " + chunkIndex + ": restored " + needsSkin.size()
                    + " villager skins, assigned " + needsSeed.size() + " new seeds");
        }
    }

    /**
     * Handles the elf sage for every online player when a chunk loads.
     *
     * Three cases per player:
     * 1. elfId stored, entity found in this chunk, skin missing → reattach
     * 2. elfId stored, entity not found (UUID stale/lost) → if THIS is the elf's home chunk, respawn
     * 3. No elfId at all → if THIS is the elf's home chunk, spawn fresh
     */
    private static void handleElfInChunk(Store<EntityStore> store, World world, long chunkIndex) {
        // Find all player entities that have VillageData
        Archetype<EntityStore> query = Archetype.of(VillageData.getComponentType());
        // Collect work outside the forEachChunk iterator to avoid concurrent modification
        List<Runnable> work = new ArrayList<>();

        store.forEachChunk(query, (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                try {
                    VillageData village = chunk.getComponent(i, VillageData.getComponentType());
                    if (village == null) continue;

                    // VillageData lives on the player entity. We need playerRef to spawn/save.
                    Ref<EntityStore> playerRef = chunk.getReferenceTo(i);
                    if (playerRef == null) continue;

                    // Skip NPC entities that happen to also carry VillagerData (villagers).
                    // Player entities have a Player component; NPC entities don't.
                    var playerComp = chunk.getComponent(i,
                            com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
                    if (playerComp == null) continue;

                    UUID elfId = village.getElfId();

                    // Compute the canonical home chunk for this player's elf
                    Vector3d elfHomePos = ElfSage.getWanderSpawnPosition(world);
                    if (elfHomePos == null) continue;
                    long elfHomeChunk = ChunkUtil.indexChunkFromBlock(
                            (int) elfHomePos.getX(), (int) elfHomePos.getZ());

                    if (elfId != null) {
                        Ref<EntityStore> elfRef = world.getEntityRef(elfId);
                        if (elfRef != null && elfRef.isValid()) {
                            // Entity loaded — check if it's in THIS chunk and needs skin
                            TransformComponent tc = store.getComponent(elfRef, TransformComponent.getComponentType());
                            if (tc == null || tc.getPosition() == null) continue;
                            var pos = tc.getPosition();
                            long elfChunk = ChunkUtil.indexChunkFromBlock((int) pos.x, (int) pos.z);
                            if (elfChunk != chunkIndex) continue;

                            if (store.getComponent(elfRef, PlayerSkinComponent.getComponentType()) == null) {
                                UUID capturedId = elfId;
                                work.add(() -> {
                                    LOGGER.info("Chunk " + chunkIndex + ": reattaching elf (UUID: " + capturedId + ")");
                                    ElfSage.reattach(elfRef, store, world);
                                });
                            }
                        } else if (elfHomeChunk == chunkIndex) {
                            // UUID is stale (entity gone) and this IS the home chunk — respawn
                            UUID capturedId = elfId;
                            work.add(() -> {
                                LOGGER.warning("Chunk " + chunkIndex + ": elf UUID " + capturedId
                                        + " stale, respawning in home chunk");
                                // Clear stale UUID so doSpawn saves the new one
                                VillageData liveVillage = VillageManager.get().getOrCreateVillageData(store, playerRef);
                                liveVillage.setElfId(null);
                                VillageManager.get().save(store, playerRef, liveVillage);
                                ElfSage.doSpawn(store, playerRef, world, elfHomePos);
                            });
                        }
                    } else if (elfHomeChunk == chunkIndex) {
                        // No elf yet — spawn one now that the home chunk is loaded
                        work.add(() -> {
                            LOGGER.info("Chunk " + chunkIndex + ": spawning elf for first time");
                            ElfSage.doSpawn(store, playerRef, world, elfHomePos);
                        });
                    }
                } catch (Exception e) {
                    LOGGER.warning("handleElfInChunk: error processing player entry: " + e.getMessage());
                }
            }
        });

        for (Runnable r : work) {
            try { r.run(); } catch (Exception e) {
                LOGGER.warning("handleElfInChunk work item failed: " + e.getMessage());
            }
        }
    }

}
