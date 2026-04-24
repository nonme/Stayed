package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
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
    public static final String ROLE_BUILDER = "Elf_Sage_Builder";
    public static final String ROLE_VILLAGER = "Elf_Sage_Villager";
    /** Set of every role that represents the elf sage, used for orphan detection. */
    private static final java.util.Set<String> SAGE_ROLES =
            java.util.Set.of(ROLE_WANDERER, ROLE_BUILDER, ROLE_VILLAGER);
    private static final double SPAWN_OFFSET = 25.0; // blocks from world spawn

    /**
     * Spawns the elf sage near the world spawn point.
     * Only spawns if the village doesn't already have an elf.
     */
    public static void spawnIfNeeded(Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        VillageData village = VillageManager.get().getOrCreateVillageData(store, playerRef);

        // Already has an elf — ensure interaction is assigned (retry if chunk not loaded yet)
        if (village.getElfId() != null) {
            LOGGER.fine("Elf already exists: " + village.getElfId());
            restoreInteractionWithRetry(store, playerRef, world, village.getElfId(), 3);
            return;
        }

        Vector3d spawnPos = getElfSpawnPosition(world);
        if (spawnPos == null) {
            LOGGER.warning("Could not determine world spawn position for elf placement");
            return;
        }

        // Remove any orphaned Elf_Sage NPCs from prior sessions/hardresets before spawning
        // a fresh one. Without this, every hardreset stacks another elf at the same spot,
        // and legacy ones (no appearance applied) show as generic Outlander skins.
        purgeOrphanedElfSages(store, world, spawnPos, 8.0, village.getElfId());

        // Drop the wanderer's tent first so we can align the elf with its campfire.
        Vector3d anchorPos = placeWandererTent(world, store, spawnPos);
        if (anchorPos != null) {
            spawnPos = anchorPos;
        }

        Pair<Ref<EntityStore>, INonPlayerCharacter> result = NpcManager.spawnNpc(
                store, spawnPos, new Vector3f(0, 0, 0), ROLE_WANDERER);

        if (result != null) {
            Ref<EntityStore> elfRef = result.first();
            INonPlayerCharacter npc = result.second();
            if (npc instanceof Entity entity) {
                UUID elfUuid = entity.getUuid();
                village.setElfId(elfUuid);
                VillageManager.get().save(store, playerRef, village);
                applySageAppearance(elfRef, store);
                LOGGER.info("Elf sage spawned at " + spawnPos + " (UUID: " + elfUuid + ")");
            } else {
                LOGGER.warning("Spawned elf NPC is not an Entity, cannot get UUID");
            }
        } else {
            LOGGER.warning("Failed to spawn elf sage");
        }
    }

    /**
     * Swaps the player's elf sage to a different role/position. Despawns the current
     * elf (if any), spawns a fresh one with {@code role}, applies the sage appearance,
     * and updates {@code VillageData.elfId} to the new UUID so future lookups resolve.
     *
     * <p>Use cases: pre-building → builder (Elf_Sage_Builder) at safe position; on build
     * complete → villager (Elf_Sage_Villager) inside the Town Hall. Wanderer→wanderer
     * teleports (e.g. confirmFounding → walk to door) go through the same path for
     * consistency.
     */
    public static void respawnAs(Store<EntityStore> store, Ref<EntityStore> playerRef, World world,
                                  String role, Vector3d position, Vector3f rotation) {
        VillageData village = VillageManager.get().getOrCreateVillageData(store, playerRef);

        // Kill the old elf if it's still around.
        UUID oldId = village.getElfId();
        if (oldId != null) {
            Entity old = world.getEntity(oldId);
            if (old != null) old.remove();
        }

        Pair<Ref<EntityStore>, INonPlayerCharacter> result = NpcManager.spawnNpc(
                store, position, rotation, role);
        if (result == null) {
            LOGGER.warning("respawnAs: failed to spawn role=" + role + " at " + position);
            return;
        }
        Ref<EntityStore> elfRef = result.first();
        INonPlayerCharacter npc = result.second();
        if (!(npc instanceof Entity entity)) {
            LOGGER.warning("respawnAs: spawned NPC is not an Entity");
            return;
        }

        UUID newId = entity.getUuid();
        village.setElfId(newId);
        VillageManager.get().save(store, playerRef, village);
        applySageAppearance(elfRef, store);
        LOGGER.info("Elf respawned as " + role + " at " + position + " (UUID: " + newId + ")");
    }

    /**
     * Places the wanderer's tent prefab near the given spawn point and returns the
     * position where the elf should stand — one block away from the campfire that the
     * prefab contains. Returns {@code null} if placement failed; callers should fall back
     * to the original spawn point in that case.
     */
    private static Vector3d placeWandererTent(World world, Store<EntityStore> store, Vector3d desiredSpawn) {
        try {
            com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection selection =
                    com.hypixel.hytale.server.core.prefab.PrefabStore.get()
                            .getAssetPrefabFromAnyPack("Elf_tent.prefab.json");

            // Find the campfire's prefab-local coords so we can anchor the whole tent around it.
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
            selection.placeNoReturn(world, campfirePos, store);
            LOGGER.info("Elf tent placed, campfire at " + campfirePos);

            // Stand one block west of the campfire (the tent's open face), same Y so the elf
            // is planted on the solid block rather than next to it.
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

    /**
     * Removes every elf-sage NPC (any role variant) within {@code radius} blocks of {@code center},
     * except the one whose UUID matches {@code preserveUuid} (use {@code null} to skip no one).
     *
     * <p>We collect UUIDs during the ECS query and despawn them afterwards — removing
     * entities while iterating a chunk query is unsafe, and {@code Entity.remove()} is the
     * supported way to despawn regardless of chunk/load state.
     */
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
                Entity e = world.getEntity(uuid);
                if (e != null) {
                    e.remove();
                }
            }
            if (!doomed.isEmpty()) {
                LOGGER.info("Purged " + doomed.size() + " orphaned Elf_Sage NPC(s) near " + center);
            }
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.WARNING, "purgeOrphanedElfSages failed", e);
        }
    }

    /**
     * Restores the Interactions component on an existing NPC after world reload.
     * Retries up to maxRetries times with a 3-second delay if the chunk isn't loaded yet.
     * On final failure, clears the stale UUID and respawns.
     */
    private static void restoreInteractionWithRetry(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                                     World world, UUID elfUuid, int retriesLeft) {
        Ref<EntityStore> elfRef = world.getEntityRef(elfUuid);
        if (elfRef != null && elfRef.isValid()) {
            NpcManager.assignInteraction(store, elfRef);
            applySageAppearance(elfRef, store);
            LOGGER.info("Restored interaction and appearance for elf sage (UUID: " + elfUuid + ")");
        } else if (retriesLeft > 0) {
            LOGGER.fine("Elf chunk not loaded yet, retrying in 3s (retries left: " + retriesLeft + ")");
            dev.hearthbound.util.TickScheduler.runLater(world, 3000, () -> {
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                restoreInteractionWithRetry(liveStore, playerRef, world, elfUuid, retriesLeft - 1);
            });
        } else {
            LOGGER.warning("Elf sage entity not found after retries: " + elfUuid + " — clearing UUID and respawning");
            VillageData village = VillageManager.get().getOrCreateVillageData(store, playerRef);
            village.setElfId(null);
            VillageManager.get().save(store, playerRef, village);
            spawnIfNeeded(store, playerRef, world);
        }
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

    /**
     * Build the elf sage's PlayerSkin using the CosmeticRegistry API
     * to construct valid compound IDs (PartId.TextureKey).
     */
    public static PlayerSkin createSageSkin() {
        CosmeticsModule cosmetics = CosmeticsModule.get();
        CosmeticRegistry reg = cosmetics.getRegistry();

        // Start with random base for safety
        PlayerSkin skin = cosmetics.generateRandomSkin(new Random(42));

        // Bare ID categories (face, ears, mouth) — no compound format needed
        skin.ears = "Elf_Ears_Small";
        skin.face = "Face_Aged";
        skin.mouth = "Mouth_Thin";
        skin.facialHair = null;

        // Compound ID categories — build PartId.TextureKey from registry
        // Skin gradient uses numbered tones (01-53). Pick a light/medium tone explicitly.
        skin.bodyCharacteristic = buildCompound(reg.getBodyCharacteristics(), reg, "Default", "01", "02", "03");
        skin.haircut = buildCompound(reg.getHaircuts(), reg, "ElfBackBun", "White", "Grey");
        skin.eyes = buildCompound(reg.getEyes(), reg, "Almond_Eyes", "Green", "Blue");
        skin.eyebrows = buildCompound(reg.getEyebrows(), reg, "Thin", "White", "Grey");

        // Clothing
        skin.undertop = buildCompound(reg.getUndertops(), reg, "Forest_Guardian_LongShirt", "Green", "Brown");
        skin.overtop = buildCompound(reg.getOvertops(), reg, "RobeOvertops", "Green", "Brown");
        skin.pants = buildCompound(reg.getPants(), reg, "Forest_Guardian", "Green", "Brown");
        skin.shoes = buildCompound(reg.getShoes(), reg, "Forest_Guardian_Boots", "Brown", "Green");
        skin.gloves = buildCompound(reg.getGloves(), reg, "Gloves_Medium_Featherbound", "Green", "Brown");

        // No cape or accessories
        skin.cape = null;
        skin.headAccessory = null;
        skin.faceAccessory = null;
        skin.earAccessory = null;

        LOGGER.info("Sage skin built: hair=" + skin.haircut + " body=" + skin.bodyCharacteristic +
                " eyes=" + skin.eyes + " overtop=" + skin.overtop + " cape=" + skin.cape);

        return skin;
    }

    /**
     * Build a compound ID (PartId.TextureKey) by finding a matching texture key
     * from the part's gradient set or textures map.
     * Tries preferred keys in order, falls back to first available.
     */
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

        // Try preferred keys
        for (String pref : preferredKeys) {
            for (String key : textureKeys) {
                if (key.equalsIgnoreCase(pref)) {
                    return partId + "." + key;
                }
            }
            // Try partial match (e.g. "White" matches "WhiteLight")
            for (String key : textureKeys) {
                if (key.toLowerCase().contains(pref.toLowerCase())) {
                    return partId + "." + key;
                }
            }
        }

        // Fall back to first available
        return partId + "." + textureKeys.get(0);
    }

    /**
     * Build a compound ID with a structural variant: PartId.TextureKey.VariantKey
     * or PartId.VariantKey if no texture keys exist.
     */
    private static String buildCompoundWithVariant(Map<String, ?> categoryMap, CosmeticRegistry reg,
                                                    String partId, String variantKey) {
        if (categoryMap == null || !categoryMap.containsKey(partId)) return partId;

        Object value = categoryMap.get(partId);
        if (!(value instanceof PlayerSkinPart part)) return partId;

        List<String> textureKeys = getTextureKeys(part, reg);
        if (textureKeys.isEmpty()) {
            return partId + "." + variantKey;
        }
        return partId + "." + textureKeys.get(0) + "." + variantKey;
    }

    /**
     * Get all available texture keys for a part — from gradient set first, then textures map.
     */
    private static List<String> getTextureKeys(PlayerSkinPart part, CosmeticRegistry reg) {
        List<String> keys = new ArrayList<>();

        String gsName = part.getGradientSet();
        if (gsName != null) {
            Map<String, PlayerSkinGradientSet> gradientSets = reg.getGradientSets();
            if (gradientSets != null) {
                PlayerSkinGradientSet gs = gradientSets.get(gsName);
                if (gs != null && gs.getGradients() != null) {
                    keys.addAll(gs.getGradients().keySet());
                }
            }
        }

        if (part.getTextures() != null) {
            for (String key : part.getTextures().keySet()) {
                if (!keys.contains(key)) keys.add(key);
            }
        }

        return keys;
    }

    /**
     * Apply the sage's unique appearance via PlayerSkin API.
     * Uses generateRandomSkin() as a valid base, then overlays elf-specific values.
     * Requires "Appearance": "Player" in the Role JSON.
     */
    public static void applySageAppearance(Ref<EntityStore> elfRef, Store<EntityStore> store) {
        try {
            CosmeticsModule cosmetics = CosmeticsModule.get();
            if (cosmetics == null) {
                LOGGER.warning("CosmeticsModule not available");
                return;
            }

            // Step 1: try with our custom skin
            PlayerSkin skin = createSageSkin();
            Model model = tryCreateModel(cosmetics, skin);

            // Step 2: if custom fails, try pure random (no overlays)
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

            // Fix PersistentModel immediately after ModelComponent update — engine may write scale=0
    
            try {
                PersistentModel pm = store.getComponent(elfRef, PersistentModel.getComponentType());
                if (pm != null) {
                    pm.setModelReference(new Model.ModelReference(
                            model.getModelAssetId(), 1.0f,
                            model.getRandomAttachmentIds(),
                            model.getAnimationSetMap() == null));
                    LOGGER.info("Fixed PersistentModel after skin apply: modelId=" + model.getModelAssetId());
                }
            } catch (Exception pmEx) {
                LOGGER.warning("Could not fix PersistentModel after skin apply: " + pmEx.getMessage());
            }

            LOGGER.info("Applied sage appearance to elf NPC");
        } catch (Exception e) {
            LOGGER.warning("Failed to apply sage appearance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Model tryCreateModel(CosmeticsModule cosmetics, PlayerSkin skin) {
        try {
            LOGGER.info("Attempting createModel with skin: ears=" + skin.ears +
                    " haircut=" + skin.haircut + " eyes=" + skin.eyes +
                    " face=" + skin.face + " mouth=" + skin.mouth +
                    " eyebrows=" + skin.eyebrows + " facialHair=" + skin.facialHair +
                    " body=" + skin.bodyCharacteristic + " undertop=" + skin.undertop +
                    " overtop=" + skin.overtop + " pants=" + skin.pants +
                    " shoes=" + skin.shoes + " cape=" + skin.cape +
                    " gloves=" + skin.gloves + " headAcc=" + skin.headAccessory);
            return cosmetics.createModel(skin, 1.0f);
        } catch (Exception e) {
            LOGGER.warning("createModel failed: " + e.getMessage());
            return null;
        }
    }

    /** Public alias so external callers (commands) can compute the canonical wanderer spawn. */
    public static Vector3d getWanderSpawnPosition(World world) {
        return getElfSpawnPosition(world);
    }

    /**
     * Calculate elf spawn position: world spawn + offset in X direction.
     */
    private static Vector3d getElfSpawnPosition(World world) {
        ISpawnProvider spawnProvider = world.getWorldConfig().getSpawnProvider();
        Transform[] spawnPoints = spawnProvider.getSpawnPoints();

        if (spawnPoints == null || spawnPoints.length == 0) {
            return null;
        }

        Vector3d worldSpawn = spawnPoints[0].getPosition();
        return new Vector3d(
                worldSpawn.getX() + SPAWN_OFFSET,
                worldSpawn.getY(),
                worldSpawn.getZ() + SPAWN_OFFSET / 2
        );
    }
}
