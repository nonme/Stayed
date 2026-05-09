package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinGradientSet;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.building.TerrainBlender;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.npc.HearthboundDataStore;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages the elf sage NPC — spawning, movement, and state.
 */
public class ElfSage {

    private static final Logger LOGGER = Logger.getLogger(ElfSage.class.getName());
    public static final String ROLE_WANDERER = "Elf_Sage_Wanderer";
    public static final String ROLE_BUILDER  = "Elf_Sage_Builder";
    public static final String ROLE_VILLAGER = "Elf_Sage_Villager";
    private static final java.util.Set<String> SAGE_ROLES =
            java.util.Set.of(ROLE_WANDERER, ROLE_BUILDER, ROLE_VILLAGER);
    private static final double SPAWN_OFFSET = 25.0;

    /**
     * Called on player join. Ensures the elf sage exists and is registered in NpcRegistry.
     *
     * If elfId is stored and entity is in the UUID registry → restore immediately (player
     * joined while elf's chunk was already loaded).
     * If elfId is stored but entity not in registry → NpcRegistry will restore it via
     * polling when the elf's chunk loads (NpcChunkLoadHandler).
     * If no elfId → doSpawn.
     */
    public static void spawnIfNeeded(Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        VillageData village = VillageManager.get().getOrCreateVillageData(store, playerRef);

        if (village.getElfId() != null) {
            UUID elfId = village.getElfId();
            // NpcRegistry is already populated from disk by HearthboundDataStore.loadAndPopulateRegistry()
            // at plugin startup. If the elf's chunk is already loaded, restore immediately.
            // Otherwise NpcChunkLoadHandler will restore via polling when the chunk loads.
            Ref<EntityStore> elfRef = world.getEntityRef(elfId);
            if (elfRef != null && elfRef.isValid()) {
                NpcRegistry.NpcRecord record = NpcRegistry.get().getRecord(elfId);
                if (record == null) {
                    // Chunk was loaded but record missing from registry (shouldn't happen normally).
                    // Rebuild a minimal record so restore works.
                    record = buildRecordFromLiveEntity(elfId, elfRef, store);
                }
                LOGGER.info("Elf already loaded on join, restoring immediately (UUID: " + elfId + ")");
                NpcRestorer.restore(elfRef, store, world, record);
            } else {
                LOGGER.info("Elf chunk not yet loaded on join (UUID: " + elfId
                        + "); NpcChunkLoadHandler will restore when chunk loads");
            }
            return;
        }

        // No elf recorded yet — spawn now.
        Vector3d spawnPos = getElfSpawnPosition(world);
        if (spawnPos == null) {
            LOGGER.warning("Could not determine world spawn position for elf placement");
            return;
        }
        doSpawn(store, playerRef, world, spawnPos);
    }

    private static NpcRegistry.NpcRecord buildRecordFromLiveEntity(UUID uuid,
                                                                     Ref<EntityStore> ref,
                                                                     Store<EntityStore> store) {
        long chunkIndex = 0L;
        try {
            var tc = store.getComponent(ref,
                    com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (tc != null && tc.getPosition() != null) {
                var pos = tc.getPosition();
                chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
            }
        } catch (Exception ignored) {}
        return new NpcRegistry.NpcRecord(uuid, ROLE_WANDERER, NpcRegistry.InteractionType.ELF, 0L, chunkIndex);
    }

    /**
     * Swaps the elf sage to a different role and (optionally) repositions it.
     *
     * Uses {@link com.hypixel.hytale.server.npc.systems.RoleChangeSystem#requestRoleChange}
     * so the same engine entity stays alive — same UUID, same npcId, same component
     * stack. The Interactions component is reset by the role change and re-applied
     * via {@link NpcRestorer#restoreAfterRoleChange}.
     *
     * If no elf currently exists in the registry the call falls through to a
     * fresh {@link #doSpawn}.
     */
    public static void respawnAs(Store<EntityStore> store, Ref<EntityStore> playerRef, World world,
                                  String role, Vector3d position, Vector3f rotation) {
        VillageData village = VillageManager.get().getOrCreateVillageData(store, playerRef);

        UUID oldId = village.getElfId();
        NpcRegistry.NpcRecord record = oldId != null ? NpcRegistry.get().getRecord(oldId) : null;
        Ref<EntityStore> elfRef = oldId != null ? world.getEntityRef(oldId) : null;

        if (record != null && (elfRef == null || !elfRef.isValid())) {
            NpcRegistry.NpcRecord updated = new NpcRegistry.NpcRecord(
                    record.npcId, record.entityUuid, role, NpcRegistry.InteractionType.ELF,
                    record.skinSeed, position != null ? NpcManager.chunkIndexFor(position) : record.chunkIndex);
            updated.copyBasePositionFrom(record);
            if (position != null) {
                updated.setPosition(position.getX(), position.getY(), position.getZ());
            } else if (record.hasPosition) {
                updated.setPosition(record.lastX, record.lastY, record.lastZ);
            }
            NpcRegistry.get().updateRecord(updated);
            HearthboundDataStore.get().save();
            LOGGER.info("respawnAs: elf chunk unloaded (oldId=" + oldId
                    + "), requesting recovery as " + role);
            NpcMissingEntityRecovery.request(world, updated,
                    position != null
                            ? new NpcMissingEntityRecovery.Target(position.getX(), position.getY(), position.getZ())
                            : null,
                    "elf-respawn-as-unloaded");
            return;
        }

        if (record == null || elfRef == null || !elfRef.isValid()) {
            LOGGER.info("respawnAs: no recorded elf to role-change (oldId=" + oldId
                    + "), falling back to fresh spawn");
            doSpawn(store, playerRef, world, position);
            return;
        }

        var npcEntity = store.getComponent(elfRef,
                com.hypixel.hytale.server.npc.entities.NPCEntity.getComponentType());
        if (npcEntity == null) {
            LOGGER.warning("respawnAs: NPCEntity null on elf ref " + elfRef);
            return;
        }

        String currentBaseRole = StayedRoleNames.extractBaseRoleName(npcEntity.getRoleName());
        boolean roleActuallyChanges = !role.equals(currentBaseRole);
        NpcRegistry.NpcRecord updated = new NpcRegistry.NpcRecord(
                record.npcId, oldId, role, NpcRegistry.InteractionType.ELF,
                record.skinSeed, record.chunkIndex);
        if (record.hasPosition) updated.setPosition(record.lastX, record.lastY, record.lastZ);

        // Move the entity if a non-null position was supplied. Skipping the move
        // when the elf is already where we want them avoids unnecessary chunk
        // boundary churn.
        if (position != null) {
            com.hypixel.hytale.server.core.entity.Entity entity = world.getEntity(oldId);
            if (entity != null) {
                entity.moveTo(elfRef, position.getX(), position.getY(), position.getZ(), store);
            }
            updated.chunkIndex = NpcManager.chunkIndexFor(position);
            updated.setPosition(position.getX(), position.getY(), position.getZ());
        }

        if (roleActuallyChanges) {
            StayedRoleChangeApplier.persistAndApply(elfRef, store, world, updated,
                    false, "elf-respawn-as");
        } else {
            NpcRegistry.get().updateRecord(updated);
            HearthboundDataStore.get().save();
            NpcRestorer.restore(elfRef, store, world, updated);
        }

        LOGGER.info("Elf role-changed to " + role + " (UUID: " + oldId + ", npcId=" + updated.npcId + ")");
    }

    /**
     * Places the wanderer's tent prefab near the given spawn point and returns the
     * position where the elf should stand — one block away from the campfire that the
     * prefab contains. Returns null if placement failed.
     */
    // Tent footprint offsets from the campfire anchor (2,1,1 in prefab-local coords).
    // Block extents: X[-5..5], Z[-3..4], maxY=4. Relative to campfire: X[-7..3], Z[-4..3], maxY=3.
    // maxPrefabY=4 (+1 clearFloating safety margin so the thatched roof is not trimmed).
    private static final int[] TENT_FOOTPRINT = {-7, 3, -4, 3, 5};

    private static Vector3d placeWandererTent(World world, Store<EntityStore> store, Vector3d desiredSpawn) {
        try {
            com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection selection =
                    com.hypixel.hytale.server.core.prefab.PrefabStore.get()
                            .getAssetPrefabFromAnyPack("Elf_tent.prefab.json");

            int[] campfireLocal = findCampfireLocal(selection);
            if (campfireLocal == null) {
                LOGGER.warning("Elf_tent prefab has no Bench_Campfire, skipping tent placement");
                return null;
            }
            selection.setAnchor(campfireLocal[0], campfireLocal[1], campfireLocal[2]);

            int campfireWorldX = (int) Math.floor(desiredSpawn.getX());
            int campfireWorldY = (int) Math.floor(desiredSpawn.getY());
            int campfireWorldZ = (int) Math.floor(desiredSpawn.getZ());
            com.hypixel.hytale.math.vector.Vector3i campfirePos =
                    new com.hypixel.hytale.math.vector.Vector3i(campfireWorldX, campfireWorldY, campfireWorldZ);

            // blendTerrain expects groundY = Y of the surface block (not standing position).
            // Campfire is at local Y=1 in the prefab; prefab floor (Y=0) sits at campfireWorldY-1.
            // surfaceY() returns solidGroundAt+1 (standing pos), so the surface block is at campfireWorldY-1.
            int blendGroundY = campfireWorldY - 1;

            TerrainBlender.clearVegetation(world, campfireWorldX, blendGroundY, campfireWorldZ, TENT_FOOTPRINT);
            selection.placeNoReturn(world, campfirePos, store);
            TerrainBlender.blendTerrain(world, campfireWorldX, blendGroundY, campfireWorldZ, TENT_FOOTPRINT);
            TerrainBlender.clearFloating(world, campfireWorldX, blendGroundY, campfireWorldZ, TENT_FOOTPRINT);
            TerrainBlender.restoreGrassDecor(world, campfireWorldX, blendGroundY, campfireWorldZ, TENT_FOOTPRINT);

            LOGGER.info("Elf tent placed, campfire at " + campfirePos);
            return new Vector3d(campfireWorldX - 0.5, campfireWorldY, campfireWorldZ + 0.5);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.WARNING, "Failed to place Elf_tent prefab", e);
            return null;
        }
    }

    private static int[] findCampfireLocal(
            com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection selection) {
        var assetMap = com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType.getAssetMap();
        int[] found = {Integer.MIN_VALUE, 0, 0};
        selection.forEachBlock((bx, by, bz, holder) -> {
            if (found[0] != Integer.MIN_VALUE) return;
            var bt = assetMap.getAsset(holder.blockId());
            if (bt == null) return;
            if ("Bench_Campfire".equals(bt.getId())) {
                found[0] = bx;
                found[1] = by;
                found[2] = bz;
            }
        });
        return found[0] == Integer.MIN_VALUE ? null : found;
    }

    public static void purgeOrphanedElfSages(Store<EntityStore> store, World world,
                                              Vector3d center, double radius, UUID preserveUuid) {
        try {
            java.util.List<UUID> doomed = new java.util.ArrayList<>();
            double r2 = radius * radius;
            com.hypixel.hytale.component.Archetype<EntityStore> query =
                    com.hypixel.hytale.component.Archetype.of(
                            com.hypixel.hytale.server.npc.entities.NPCEntity.getComponentType());
            store.forEachChunk(query, (chunk, buffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    try {
                        com.hypixel.hytale.server.npc.entities.NPCEntity npc =
                                chunk.getComponent(i,
                                        com.hypixel.hytale.server.npc.entities.NPCEntity.getComponentType());
                        if (npc == null) continue;
                        if (!SAGE_ROLES.contains(npc.getRoleName())) continue;

                        com.hypixel.hytale.server.core.entity.UUIDComponent uc =
                                chunk.getComponent(i,
                                        com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
                        UUID uuid = uc != null ? uc.getUuid() : null;
                        if (uuid == null) continue;
                        if (uuid.equals(preserveUuid)) continue;

                        com.hypixel.hytale.server.core.modules.entity.component.TransformComponent tc =
                                chunk.getComponent(i,
                                        com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
                                                .getComponentType());
                        if (tc == null || tc.getPosition() == null) continue;
                        Vector3d p = tc.getPosition();
                        double dx = p.x - center.x;
                        double dy = p.y - center.y;
                        double dz = p.z - center.z;
                        if (dx * dx + dy * dy + dz * dz <= r2) {
                            doomed.add(uuid);
                        }
                    } catch (Exception ignored) {}
                }
            });

            for (UUID uuid : doomed) {
                NpcRegistry.get().unregister(uuid);
                Entity e = world.getEntity(uuid);
                if (e != null) e.remove();
            }
            if (!doomed.isEmpty()) {
                LOGGER.info("Purged " + doomed.size() + " orphaned Elf_Sage NPC(s) near " + center);
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.WARNING, "purgeOrphanedElfSages failed", e);
        }
    }

    /**
     * Spawns a fresh elf sage. Loads the ring of chunks around world spawn (excluding
     * the spawn chunk itself), finds the flattest spot for the tent, then spawns.
     */
    public static void doSpawn(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                World world, Vector3d spawnPos) {
        // spawnPos is world spawn XZ (Y=0). Collect the 8 surrounding chunk indices,
        // skipping the spawn chunk itself so the elf never lands on the player spawn.
        long spawnChunk = ChunkUtil.indexChunkFromBlock(spawnPos.getX(), spawnPos.getZ());
        int spawnChunkX = ChunkUtil.xOfChunkIndex(spawnChunk);
        int spawnChunkZ = ChunkUtil.zOfChunkIndex(spawnChunk);

        java.util.List<long[]> candidateChunks = new java.util.ArrayList<>();
        for (int dcx = -1; dcx <= 1; dcx++) {
            for (int dcz = -1; dcz <= 1; dcz++) {
                if (dcx == 0 && dcz == 0) continue; // skip spawn chunk
                candidateChunks.add(new long[]{ ChunkUtil.indexChunk(spawnChunkX + dcx, spawnChunkZ + dcz) });
            }
        }

        java.util.List<java.util.concurrent.CompletableFuture<?>> futures = new java.util.ArrayList<>();
        for (long[] entry : candidateChunks) {
            futures.add(world.getChunkAsync(entry[0]).toCompletableFuture());
        }

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                .thenAccept(ignored -> world.execute(() -> {
            Store<EntityStore> liveStore = world.getEntityStore().getStore();

            VillageData liveVillage = VillageManager.get().getOrCreateVillageData(liveStore, playerRef);
            if (liveVillage.getElfId() != null) {
                // elfId is recorded — elf was already spawned. If the chunk isn't loaded yet,
                // NpcChunkLoadHandler will restore it. Don't spawn a duplicate.
                LOGGER.fine(() -> "doSpawn: elfId already set (" + liveVillage.getElfId() + "), skipping");
                return;
            }

            // Find the flattest tent spot across all loaded candidate chunks.
            int bestX = (int) spawnPos.getX() + (int) SPAWN_OFFSET;
            int bestZ = (int) spawnPos.getZ() + (int) (SPAWN_OFFSET / 2);
            int bestVariance = Integer.MAX_VALUE;
            for (long[] entry : candidateChunks) {
                long chunkIdx = entry[0];
                int chunkX = ChunkUtil.xOfChunkIndex(chunkIdx);
                int chunkZ = ChunkUtil.zOfChunkIndex(chunkIdx);
                // Sample on a 2-block grid in the inner ±5 area of the chunk.
                int chunkWorldX = ChunkUtil.worldCoordFromLocalCoord(chunkX, 8);
                int chunkWorldZ = ChunkUtil.worldCoordFromLocalCoord(chunkZ, 8);
                for (int dx = -5; dx <= 5; dx += 2) {
                    for (int dz = -5; dz <= 5; dz += 2) {
                        int cx = chunkWorldX + dx;
                        int cz = chunkWorldZ + dz;
                        int v = tentVariance(world, cx, cz);
                        if (v < bestVariance) {
                            bestVariance = v;
                            bestX = cx;
                            bestZ = cz;
                        }
                    }
                }
            }
            int bestY = surfaceY(world, bestX, bestZ);
            Vector3d tentPos = new Vector3d(bestX, bestY, bestZ);
            LOGGER.info("Tent spot chosen: " + tentPos + " variance=" + bestVariance);

            purgeOrphanedElfSages(liveStore, world, tentPos, 8.0, null);

            Vector3d finalPos = tentPos;
            Vector3d anchorPos = placeWandererTent(world, liveStore, tentPos);
            if (anchorPos != null) finalPos = anchorPos;

            String npcId = UUID.randomUUID().toString();
            long elfChunk = NpcManager.chunkIndexFor(finalPos);
            NpcRegistry.NpcRecord record = new NpcRegistry.NpcRecord(
                    npcId, null, ROLE_WANDERER, NpcRegistry.InteractionType.ELF, 0L, elfChunk);
            record.setPosition(finalPos.getX(), finalPos.getY(), finalPos.getZ());

            Pair<Ref<EntityStore>, INonPlayerCharacter> result = StayedNpcSpawner.spawnPersistent(
                    liveStore, finalPos, new Vector3f(0, 0, 0), record);
            if (result == null) {
                LOGGER.warning("doSpawn: failed to spawn elf sage");
                return;
            }

            Ref<EntityStore> elfRef = result.first();
            INonPlayerCharacter npc = result.second();
            if (!(npc instanceof Entity entity)) {
                LOGGER.warning("doSpawn: spawned NPC is not an Entity");
                return;
            }

            @SuppressWarnings("removal")
            UUID elfUuid = entity.getUuid();
            liveVillage.setElfId(elfUuid);
            VillageManager.get().save(liveStore, playerRef, liveVillage);

            HearthboundDataStore.get().save();

            // Entity is live on this thread — restore immediately (interaction + skin with delay).
            NpcRestorer.restore(elfRef, liveStore, world, record);

            LOGGER.info("Elf sage spawned at " + finalPos + " (UUID: " + elfUuid + ")");
        }));
    }

    private static final String ELF_NAME_KEY = "server.npcRoles.Elf_Sage.name";

    public static String resolveElfName() {
        try {
            I18nModule i18n = I18nModule.get();
            if (i18n == null) {
                LOGGER.warning("resolveElfName: I18nModule is null");
                return "Elf";
            }
            String name = i18n.getMessage("en-US", ELF_NAME_KEY);
            if (name != null && !name.isBlank() && !name.equals(ELF_NAME_KEY)) {
                return name;
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to resolve elf name: " + e.getMessage());
        }
        return "Elf";
    }

    public static PlayerSkin createSageSkin() {
        CosmeticsModule cosmetics = CosmeticsModule.get();
        CosmeticRegistry reg = cosmetics.getRegistry();

        PlayerSkin skin = cosmetics.generateRandomSkin(new Random(42));

        skin.ears       = "Elf_Ears_Small";
        skin.face       = "Face_Aged";
        skin.mouth      = "Mouth_Thin";
        skin.facialHair = null;

        skin.bodyCharacteristic = buildCompound(reg.getBodyCharacteristics(), reg, "Default", "01", "02", "03");
        skin.haircut    = buildCompound(reg.getHaircuts(),  reg, "ElfBackBun", "White", "Grey");
        skin.eyes       = buildCompound(reg.getEyes(),     reg, "Almond_Eyes", "Green", "Blue");
        skin.eyebrows   = buildCompound(reg.getEyebrows(), reg, "Thin", "White", "Grey");

        skin.undertop = buildCompound(reg.getUndertops(), reg, "Forest_Guardian_LongShirt", "Green", "Brown");
        skin.overtop  = buildCompound(reg.getOvertops(),  reg, "RobeOvertops", "Green", "Brown");
        skin.pants    = buildCompound(reg.getPants(),     reg, "Forest_Guardian", "Green", "Brown");
        skin.shoes    = buildCompound(reg.getShoes(),     reg, "Forest_Guardian_Boots", "Brown", "Green");
        skin.gloves   = buildCompound(reg.getGloves(),    reg, "Gloves_Medium_Featherbound", "Green", "Brown");

        skin.cape            = null;
        skin.headAccessory   = null;
        skin.faceAccessory   = null;
        skin.earAccessory    = null;

        LOGGER.fine("Sage skin built: hair=" + skin.haircut + " body=" + skin.bodyCharacteristic
                + " eyes=" + skin.eyes + " overtop=" + skin.overtop);
        return skin;
    }

    private static String buildCompound(Map<String, ?> categoryMap, CosmeticRegistry reg,
                                         String partId, String... preferredKeys) {
        if (categoryMap == null || !categoryMap.containsKey(partId)) {
            LOGGER.warning("Part not found in registry: " + partId);
            return partId;
        }
        Object value = categoryMap.get(partId);
        if (!(value instanceof PlayerSkinPart part)) return partId;

        List<String> textureKeys = getTextureKeys(part, reg);
        if (textureKeys.isEmpty()) {
            LOGGER.warning("No texture keys for: " + partId);
            return partId;
        }
        for (String pref : preferredKeys) {
            for (String key : textureKeys) {
                if (key.equalsIgnoreCase(pref)) return partId + "." + key;
            }
            for (String key : textureKeys) {
                if (key.toLowerCase().contains(pref.toLowerCase())) return partId + "." + key;
            }
        }
        return partId + "." + textureKeys.get(0);
    }

    private static List<String> getTextureKeys(PlayerSkinPart part, CosmeticRegistry reg) {
        List<String> keys = new ArrayList<>();
        String gsName = part.getGradientSet();
        if (gsName != null) {
            Map<String, PlayerSkinGradientSet> gradientSets = reg.getGradientSets();
            if (gradientSets != null) {
                PlayerSkinGradientSet gs = gradientSets.get(gsName);
                if (gs != null && gs.getGradients() != null) keys.addAll(gs.getGradients().keySet());
            }
        }
        if (part.getTextures() != null) {
            for (String key : part.getTextures().keySet()) {
                if (!keys.contains(key)) keys.add(key);
            }
        }
        return keys;
    }

    public static void applySageAppearance(Ref<EntityStore> elfRef, Store<EntityStore> store) {
        try {
            CosmeticsModule cosmetics = CosmeticsModule.get();
            if (cosmetics == null) {
                LOGGER.warning("CosmeticsModule not available");
                return;
            }

            PlayerSkin skin = createSageSkin();
            Model model = tryCreateModel(cosmetics, skin);

            if (model == null) {
                LOGGER.warning("Custom skin failed, trying pure random skin");
                skin = cosmetics.generateRandomSkin(new Random());
                model = tryCreateModel(cosmetics, skin);
            }

            if (model == null) {
                LOGGER.warning("Even random skin failed — cannot apply appearance");
                return;
            }

            store.putComponent(elfRef, PlayerSkinComponent.getComponentType(), new PlayerSkinComponent(skin));
            store.putComponent(elfRef, ModelComponent.getComponentType(), new ModelComponent(model));

            try {
                PersistentModel pm = store.getComponent(elfRef, PersistentModel.getComponentType());
                if (pm != null) {
                    pm.setModelReference(new Model.ModelReference(
                            model.getModelAssetId(), 1.0f,
                            model.getRandomAttachmentIds(),
                            model.getAnimationSetMap() == null));
                }
            } catch (Exception pmEx) {
                LOGGER.warning("Could not fix PersistentModel after skin apply: " + pmEx.getMessage());
            }

            LOGGER.fine("Applied sage appearance to elf NPC");
        } catch (Exception e) {
            LOGGER.warning("Failed to apply sage appearance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Model tryCreateModel(CosmeticsModule cosmetics, PlayerSkin skin) {
        try {
            return cosmetics.createModel(skin, 1.0f);
        } catch (Exception e) {
            LOGGER.warning("createModel failed: " + e.getMessage());
            return null;
        }
    }

    public static Vector3d getWanderSpawnPosition(World world) {
        return getElfSpawnPosition(world);
    }

    private static Vector3d getElfSpawnPosition(World world) {
        ISpawnProvider spawnProvider = world.getWorldConfig().getSpawnProvider();
        Transform[] spawnPoints = spawnProvider.getSpawnPoints();
        if (spawnPoints == null || spawnPoints.length == 0) return null;
        // Return only XZ — Y is resolved after chunk load via findFlatSpot.
        Vector3d worldSpawn = spawnPoints[0].getPosition();
        return new Vector3d(worldSpawn.getX(), 0, worldSpawn.getZ());
    }

    // Scoring footprint matches TENT_FOOTPRINT with +1 margin on each side.
    // Campfire-relative: X[-8..4], Z[-5..4].
    private static final int TENT_SCORE_MIN_X = -8;
    private static final int TENT_SCORE_MAX_X =  4;
    private static final int TENT_SCORE_MIN_Z = -5;
    private static final int TENT_SCORE_MAX_Z =  4;

    // Returns total Y-variance of the tent footprint centered at campfire position (cx, cz).
    // Returns Integer.MAX_VALUE if any column contains water/lava or is unloaded.
    // Lower variance = flatter and drier = better placement spot.
    private static int tentVariance(World world, int cx, int cz) {
        int centerY = surfaceY(world, cx, cz);
        if (centerY == -1) return Integer.MAX_VALUE;
        int variance = 0;
        for (int fx = TENT_SCORE_MIN_X; fx <= TENT_SCORE_MAX_X; fx++) {
            for (int fz = TENT_SCORE_MIN_Z; fz <= TENT_SCORE_MAX_Z; fz++) {
                int y = surfaceY(world, cx + fx, cz + fz);
                if (y == -1) return Integer.MAX_VALUE;
                variance += Math.abs(y - centerY);
            }
        }
        return variance;
    }

    // Standing-position Y for a column: TerrainBlender.solidGroundAt + 1.
    // Returns -1 if the column contains fluid or is unloaded.
    private static int surfaceY(World world, int x, int z) {
        var chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
        int topY = chunk != null ? chunk.getHeight(x & 31, z & 31) : 64;
        int ground = TerrainBlender.solidGroundAt(world, x, topY, z);
        return ground == -1 ? -1 : ground + 1;
    }
}
