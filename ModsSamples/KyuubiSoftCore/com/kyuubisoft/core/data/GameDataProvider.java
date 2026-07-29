/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.builtin.tagset.config.NPCGroup
 *  com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
 *  com.hypixel.hytale.server.core.asset.type.item.config.Item
 *  com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality
 *  com.hypixel.hytale.server.npc.NPCPlugin
 */
package com.kyuubisoft.core.data;

import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.npc.NPCPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GameDataProvider {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private static Set<String> blockGroupsCache = null;
    private static List<String> blockIdsCache = null;
    private static Map<String, List<String>> blocksByGroupCache = null;
    private static List<String> itemIdsCache = null;
    private static Map<String, String[]> itemDetailCache = null;
    private static List<String> npcRoleNamesCache = null;
    private static Set<String> npcGroupsCache = null;
    private static Map<String, String> blockToItemCache = null;

    public static Set<String> getAllBlockGroups() {
        if (blockGroupsCache != null) {
            return blockGroupsCache;
        }
        try {
            blockGroupsCache = BlockType.getAssetMap().getAssetMap().values().stream().map(BlockType::getGroup).filter(Objects::nonNull).filter(Predicate.not(String::isBlank)).collect(Collectors.toCollection(TreeSet::new));
            LOGGER.info("Loaded " + blockGroupsCache.size() + " block groups");
            return blockGroupsCache;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load block groups: " + e.getMessage());
            return GameDataProvider.getDefaultBlockGroups();
        }
    }

    public static List<String> getAllBlockIds() {
        if (blockIdsCache != null) {
            return blockIdsCache;
        }
        try {
            blockIdsCache = BlockType.getAssetMap().getAssetMap().values().stream().map(BlockType::getId).filter(Objects::nonNull).filter(Predicate.not(String::isBlank)).filter(id -> !id.startsWith("*")).filter(id -> !id.contains("State_Definitions")).filter(id -> !id.contains("_Definitions_")).sorted().collect(Collectors.toList());
            LOGGER.info("Loaded " + blockIdsCache.size() + " block IDs");
            return blockIdsCache;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load block IDs: " + e.getMessage());
            return new ArrayList<String>();
        }
    }

    public static List<String> getBlockIdsByGroup(String group) {
        if (blocksByGroupCache == null) {
            GameDataProvider.buildBlocksByGroupCache();
        }
        return blocksByGroupCache.getOrDefault(group, new ArrayList());
    }

    private static void buildBlocksByGroupCache() {
        blocksByGroupCache = new HashMap<String, List<String>>();
        try {
            BlockType.getAssetMap().getAssetMap().values().forEach(blockType -> {
                String group = blockType.getGroup();
                String id = blockType.getId();
                if (group != null && !group.isBlank() && id != null && !id.isBlank()) {
                    blocksByGroupCache.computeIfAbsent(group, k -> new ArrayList()).add(id);
                }
            });
            blocksByGroupCache.values().forEach(Collections::sort);
        }
        catch (Exception e) {
            LOGGER.warning("Failed to build blocks-by-group cache: " + e.getMessage());
        }
    }

    private static Set<String> getDefaultBlockGroups() {
        return new TreeSet<String>(Arrays.asList("Stone", "Dirt", "Sand", "Gravel", "Wood", "Leaves", "Grass", "Ore", "OreMithril", "OreIron", "OreGold", "OreCopper", "Mud", "Soil", "Snow", "Moss", "Cover", "Plant", "Flower", "Brick", "Plank", "Glass", "Wool", "Clay"));
    }

    public static List<String> getAllItemIds() {
        if (itemIdsCache != null) {
            return itemIdsCache;
        }
        try {
            itemDetailCache = new HashMap<String, String[]>();
            itemIdsCache = Item.getAssetMap().getAssetMap().values().stream().filter(item -> item.getId() != null && !item.getId().toString().isBlank()).filter(item -> {
                try {
                    int qualityIndex = item.getQualityIndex();
                    ItemQuality quality = (ItemQuality)ItemQuality.getAssetMap().getAsset(qualityIndex);
                    return quality == null || !quality.isHiddenFromSearch();
                }
                catch (Exception e) {
                    return true;
                }
            }).peek(item -> {
                String id = item.getId().toString();
                String[] cats = item.getCategories();
                String cat = cats != null && cats.length > 0 ? cats[0] : "";
                itemDetailCache.put(id, new String[]{id, cat});
            }).map(item -> item.getId().toString()).sorted().collect(Collectors.toList());
            LOGGER.info("Loaded " + itemIdsCache.size() + " item IDs");
            return itemIdsCache;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load item IDs: " + e.getMessage());
            itemIdsCache = new ArrayList<String>();
            return itemIdsCache;
        }
    }

    public static String getItemCategory(String itemId) {
        if (itemDetailCache == null) {
            GameDataProvider.getAllItemIds();
        }
        String[] detail = itemDetailCache != null ? itemDetailCache.get(itemId) : null;
        return detail != null ? detail[1] : "";
    }

    public static List<String> getAllNPCRoleNames() {
        if (npcRoleNamesCache != null) {
            return npcRoleNamesCache;
        }
        try {
            ArrayList rawNames = NPCPlugin.get().getRoleTemplateNames(false);
            if (rawNames == null) {
                rawNames = new ArrayList();
            }
            npcRoleNamesCache = rawNames.stream().filter(name -> !name.toLowerCase().contains("test")).collect(Collectors.toList());
            LOGGER.info("Loaded " + npcRoleNamesCache.size() + " NPC role names");
            return npcRoleNamesCache;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load NPC role names: " + e.getMessage());
            return new ArrayList<String>();
        }
    }

    public static Set<String> getAllNPCGroups() {
        if (npcGroupsCache != null) {
            return npcGroupsCache;
        }
        try {
            npcGroupsCache = NPCGroup.getAssetMap().getAssetMap().values().stream().map(NPCGroup::getId).map(Object::toString).filter(Objects::nonNull).filter(id -> !id.toLowerCase().contains("test")).collect(Collectors.toCollection(TreeSet::new));
            LOGGER.info("Loaded " + npcGroupsCache.size() + " NPC groups");
            return npcGroupsCache;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load NPC groups: " + e.getMessage());
            return new TreeSet<String>();
        }
    }

    public static Map<String, String> getZoneTypes() {
        LinkedHashMap<String, String> zones = new LinkedHashMap<String, String>();
        zones.put("spawn", "Spawn Zone");
        zones.put("village", "Village");
        zones.put("dungeon", "Dungeon");
        zones.put("cave", "Cave");
        zones.put("forest", "Forest");
        zones.put("desert", "Desert");
        zones.put("ocean", "Ocean");
        zones.put("mountain", "Mountain");
        zones.put("ruins", "Ruins");
        zones.put("camp", "Camp");
        return zones;
    }

    public static Map<String, String> getBlockToItemMap() {
        if (blockToItemCache != null) {
            return blockToItemCache;
        }
        blockToItemCache = new LinkedHashMap<String, String>();
        HashSet<String> itemIdSet = new HashSet<String>(GameDataProvider.getAllItemIds());
        HashMap<String, String> shortToFull = new HashMap<String, String>();
        for (String itemId : GameDataProvider.getAllItemIds()) {
            String shortName = itemId.contains(":") ? itemId.substring(itemId.indexOf(58) + 1) : itemId;
            shortToFull.putIfAbsent(shortName.toLowerCase(), itemId);
        }
        for (String blockId : GameDataProvider.getAllBlockIds()) {
            if (itemIdSet.contains(blockId)) {
                blockToItemCache.put(blockId, blockId);
                continue;
            }
            String match = (String)shortToFull.get(blockId.toLowerCase());
            blockToItemCache.put(blockId, match);
        }
        return blockToItemCache;
    }

    public static Map<String, String> getObjectiveTypes() {
        LinkedHashMap<String, String> types = new LinkedHashMap<String, String>();
        types.put("blocks_mined", "Blocks Mined");
        types.put("blocks_chopped", "Blocks Chopped (Wood)");
        types.put("blocks_dug", "Blocks Dug (Dirt/Sand)");
        types.put("blocks_placed", "Blocks Placed");
        types.put("kills", "Kills (NPCs/Mobs)");
        types.put("items_crafted", "Items Crafted");
        types.put("items_collected", "Items Collected");
        types.put("items_consumed", "Items Consumed");
        types.put("distance_traveled", "Distance Traveled");
        types.put("zones_discovered", "Zones Discovered");
        types.put("interact_npc", "Interact with NPC");
        types.put("deliver", "Deliver Items");
        types.put("player_kills", "Player Kills");
        types.put("chat_messages", "Chat Messages");
        types.put("playtime_minutes", "Playtime (Minutes)");
        types.put("blocks_harvested", "Blocks Harvested");
        types.put("damage_dealt", "Damage Dealt");
        types.put("damage_taken", "Damage Taken");
        types.put("flowers_picked", "Flowers Picked");
        types.put("command_executed", "Command Executed");
        types.put("level_reached", "Level Reached");
        types.put("complete_hub_quests", "Complete Hub Quests");
        return types;
    }

    public static List<String> getTargetModesForType(String objectiveType) {
        if (objectiveType == null) {
            return Arrays.asList("any");
        }
        return switch (objectiveType) {
            case "blocks_mined", "blocks_chopped", "blocks_dug", "blocks_placed" -> Arrays.asList("any", "block", "group");
            case "kills" -> Arrays.asList("any", "npc", "npc_group");
            case "items_crafted", "items_collected", "items_consumed", "deliver" -> Arrays.asList("any", "item", "item_group");
            case "zones_discovered" -> Arrays.asList("zone");
            case "interact_npc" -> Arrays.asList("npc", "interaction");
            case "command_executed" -> Arrays.asList("any");
            case "level_reached", "playtime_minutes", "distance_traveled", "damage_dealt", "damage_taken", "player_kills", "chat_messages", "complete_hub_quests" -> Collections.singletonList("any");
            default -> Arrays.asList("any");
        };
    }

    public static void clearCache() {
        blockGroupsCache = null;
        blockIdsCache = null;
        blocksByGroupCache = null;
        blockToItemCache = null;
        npcRoleNamesCache = null;
        npcGroupsCache = null;
        itemIdsCache = null;
        itemDetailCache = null;
        LOGGER.info("GameDataProvider cache cleared");
    }
}

