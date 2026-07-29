/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.protocol.AnimationSlot
 *  com.hypixel.hytale.protocol.ItemResourceType
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
 *  com.hypixel.hytale.server.core.asset.type.item.config.Item
 *  com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality
 *  com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
 *  com.hypixel.hytale.server.core.command.system.AbstractCommand
 *  com.hypixel.hytale.server.core.command.system.CommandContext
 *  com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
 *  com.hypixel.hytale.server.core.command.system.basecommands.CommandBase
 *  com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry
 *  com.hypixel.hytale.server.core.cosmetics.CosmeticsModule
 *  com.hypixel.hytale.server.core.cosmetics.Emote
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.WorldConfig
 *  com.hypixel.hytale.server.npc.NPCPlugin
 */
package com.kyuubisoft.core.commands;

import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.ItemResourceType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.cosmetics.Emote;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.kyuubisoft.core.CorePlugin;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import com.kyuubisoft.core.commands.DevExportService;
import java.lang.invoke.CallSite;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class DevExportCommand
extends AbstractCommandCollection {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private final CorePlugin plugin;
    private final DevExportService exportService;

    protected boolean canGeneratePermission() {
        return false;
    }

    public DevExportCommand(CorePlugin plugin) {
        super("ksdev", "Developer data export tools");
        this.requirePermission("ks.admin.dev");
        this.plugin = plugin;
        this.exportService = new DevExportService(plugin.getDataDirectory());
        this.addAliases(new String[]{"dev"});
        this.addSubCommand((AbstractCommand)new ItemsCommand(this));
        this.addSubCommand((AbstractCommand)new BlocksCommand(this));
        this.addSubCommand((AbstractCommand)new EntitiesCommand(this));
        this.addSubCommand((AbstractCommand)new CitizensCommand(this));
        this.addSubCommand((AbstractCommand)new WorldsCommand(this));
        this.addSubCommand((AbstractCommand)new EmotesCommand(this));
        this.addSubCommand((AbstractCommand)new AllCommand(this));
    }

    private boolean checkPermission(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            return true;
        }
        Player player = (Player)ctx.senderAs(Player.class);
        if (!player.hasPermission("dev.admin")) {
            ctx.sendMessage(Message.raw((String)"\u00a7cNo permission. Required: dev.admin"));
            return false;
        }
        return true;
    }

    private void sendResult(CommandContext ctx, String type, int count, Path file) {
        if (file != null) {
            ctx.sendMessage(Message.raw((String)("\u00a7aExported \u00a7f" + count + " " + type + "\u00a7a to \u00a7f" + String.valueOf(file.getFileName()))));
        } else {
            ctx.sendMessage(Message.raw((String)("\u00a7cFailed to export " + type + "!")));
        }
    }

    private ExportResult exportItems() {
        try {
            ArrayList<Map> items = new ArrayList<Map>();
            for (Item item : Item.getAssetMap().getAssetMap().values()) {
                if (item.getId() == null || item.getId().toString().isBlank()) continue;
                try {
                    int qi = item.getQualityIndex();
                    ItemQuality quality = (ItemQuality)ItemQuality.getAssetMap().getAsset(qi);
                    if (quality != null && quality.isHiddenFromSearch()) {
                        continue;
                    }
                }
                catch (Exception qi) {
                    // empty catch block
                }
                LinkedHashMap<String, Object> entry = new LinkedHashMap<String, Object>();
                String id = item.getId().toString();
                entry.put("id", id);
                entry.put("categories", item.getCategories() != null ? Arrays.asList(item.getCategories()) : List.of());
                entry.put("consumable", item.isConsumable());
                entry.put("maxStack", item.getMaxStack());
                entry.put("qualityIndex", item.getQualityIndex());
                entry.put("itemLevel", item.getItemLevel());
                entry.put("maxDurability", item.getMaxDurability());
                entry.put("hasBlock", item.hasBlockType());
                try {
                    entry.put("blockId", item.getBlockId());
                }
                catch (Exception e2) {
                    entry.put("blockId", null);
                }
                entry.put("isTool", item.getTool() != null);
                entry.put("isWeapon", item.getWeapon() != null);
                entry.put("isArmor", item.getArmor() != null);
                try {
                    entry.put("translationKey", item.getTranslationKey());
                }
                catch (Exception e3) {
                    entry.put("translationKey", null);
                }
                try {
                    ItemResourceType[] resourceTypes = item.getResourceTypes();
                    if (resourceTypes != null && resourceTypes.length > 0) {
                        ArrayList<CallSite> rtList = new ArrayList<CallSite>();
                        for (ItemResourceType rt : resourceTypes) {
                            rtList.add((CallSite)((Object)(rt.id + ":" + rt.quantity)));
                        }
                        entry.put("resourceTypes", rtList);
                    } else {
                        entry.put("resourceTypes", List.of());
                    }
                }
                catch (Exception e4) {
                    entry.put("resourceTypes", List.of());
                }
                items.add(entry);
            }
            items.sort(Comparator.comparing(e -> (String)e.get("id")));
            Path file = this.exportService.export("items", items.size(), items);
            return new ExportResult(items.size(), file);
        }
        catch (Exception e5) {
            LOGGER.warning("Failed to export items: " + e5.getMessage());
            return new ExportResult(0, null);
        }
    }

    private ExportResult exportBlocks() {
        try {
            ArrayList<Map> blocks = new ArrayList<Map>();
            for (BlockType block : BlockType.getAssetMap().getAssetMap().values()) {
                String id = block.getId();
                if (id == null || id.isBlank() || id.startsWith("*") || id.contains("State_Definitions") || id.contains("_Definitions_")) continue;
                LinkedHashMap<String, Object> entry = new LinkedHashMap<String, Object>();
                entry.put("id", id);
                entry.put("group", block.getGroup());
                try {
                    entry.put("material", block.getMaterial() != null ? block.getMaterial().name() : null);
                }
                catch (Exception e2) {
                    entry.put("material", null);
                }
                try {
                    entry.put("opacity", block.getOpacity() != null ? block.getOpacity().name() : null);
                }
                catch (Exception e3) {
                    entry.put("opacity", null);
                }
                entry.put("isDoor", block.isDoor());
                entry.put("isTrigger", block.isTrigger());
                entry.put("hasItem", block.getItem() != null);
                blocks.add(entry);
            }
            blocks.sort(Comparator.comparing(e -> (String)e.get("id")));
            Path file = this.exportService.export("blocks", blocks.size(), blocks);
            return new ExportResult(blocks.size(), file);
        }
        catch (Exception e4) {
            LOGGER.warning("Failed to export blocks: " + e4.getMessage());
            return new ExportResult(0, null);
        }
    }

    private ExportResult exportEntities() {
        try {
            ArrayList npcRoles;
            ArrayList entityTypes = new ArrayList(ModelAsset.getAssetMap().getAssetMap().keySet());
            Collections.sort(entityTypes, String.CASE_INSENSITIVE_ORDER);
            try {
                npcRoles = NPCPlugin.get().getRoleTemplateNames(false);
                if (npcRoles == null) {
                    npcRoles = new ArrayList();
                }
                Collections.sort(npcRoles, String.CASE_INSENSITIVE_ORDER);
            }
            catch (Exception e) {
                npcRoles = new ArrayList();
            }
            int total = entityTypes.size() + npcRoles.size();
            LinkedHashMap<String, Object> envelope = new LinkedHashMap<String, Object>();
            envelope.put("_type", "entities");
            envelope.put("_entityTypeCount", entityTypes.size());
            envelope.put("_npcRoleCount", npcRoles.size());
            envelope.put("_exportedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            envelope.put("entityTypes", entityTypes);
            envelope.put("npcRoles", npcRoles);
            Path file = this.exportService.exportRaw("entities", envelope);
            return new ExportResult(total, file);
        }
        catch (Exception e) {
            LOGGER.warning("Failed to export entities: " + e.getMessage());
            return new ExportResult(0, null);
        }
    }

    private ExportResult exportCitizens() {
        try {
            CitizenService service = this.plugin.getCitizenService();
            if (service == null) {
                return new ExportResult(0, null);
            }
            ArrayList<Map> citizens = new ArrayList<Map>();
            for (CitizenData c : service.getAllCitizens()) {
                LinkedHashMap<String, Object> entry = new LinkedHashMap<String, Object>();
                entry.put("id", c.id);
                entry.put("name", c.getDisplayName());
                entry.put("group", c.group);
                entry.put("world", c.worldName);
                entry.put("position", String.format("%.1f, %.1f, %.1f", c.posX, c.posY, c.posZ));
                entry.put("isPlayerModel", c.isPlayerModel);
                entry.put("entityTypeId", c.entityTypeId);
                entry.put("skinUsername", c.skinUsername);
                entry.put("fKeyEnabled", c.fKeyInteractionEnabled);
                entry.put("shopId", c.shopId);
                entry.put("movementType", c.movementType);
                entry.put("movementRadius", Float.valueOf(c.movementRadius));
                entry.put("attitude", c.attitude);
                entry.put("spawned", c.spawnedEntityUUID != null);
                citizens.add(entry);
            }
            citizens.sort(Comparator.comparing(e -> (String)e.get("id")));
            Path file = this.exportService.export("citizens", citizens.size(), citizens);
            return new ExportResult(citizens.size(), file);
        }
        catch (Exception e2) {
            LOGGER.warning("Failed to export citizens: " + e2.getMessage());
            return new ExportResult(0, null);
        }
    }

    private ExportResult exportWorlds() {
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return new ExportResult(0, null);
            }
            ArrayList<Map> worlds = new ArrayList<Map>();
            for (World world : universe.getWorlds().values()) {
                LinkedHashMap<String, Object> entry = new LinkedHashMap<String, Object>();
                entry.put("name", world.getName());
                entry.put("playerCount", world.getPlayerCount());
                entry.put("isAlive", world.isAlive());
                entry.put("isTicking", world.isTicking());
                try {
                    WorldConfig config = world.getWorldConfig();
                    if (config != null) {
                        entry.put("displayName", config.getDisplayName());
                        entry.put("isPvpEnabled", config.isPvpEnabled());
                        entry.put("gameMode", config.getGameMode() != null ? config.getGameMode().name() : null);
                        entry.put("seed", config.getSeed());
                        entry.put("isFallDamageEnabled", config.isFallDamageEnabled());
                        entry.put("isSpawningNPC", config.isSpawningNPC());
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
                worlds.add(entry);
            }
            worlds.sort(Comparator.comparing(e -> (String)e.get("name")));
            Path file = this.exportService.export("worlds", worlds.size(), worlds);
            return new ExportResult(worlds.size(), file);
        }
        catch (Exception e2) {
            LOGGER.warning("Failed to export worlds: " + e2.getMessage());
            return new ExportResult(0, null);
        }
    }

    private ExportResult exportEmotes() {
        try {
            ArrayList<Map> emotes = new ArrayList<Map>();
            try {
                CosmeticRegistry registry = CosmeticsModule.get().getRegistry();
                AnimationSlot[] emoteMap = registry.getEmotes();
                if (emoteMap != null) {
                    for (Map.Entry e2 : emoteMap.entrySet()) {
                        LinkedHashMap entry = new LinkedHashMap();
                        entry.put("key", e2.getKey());
                        Emote emote = (Emote)e2.getValue();
                        entry.put("id", emote.getId());
                        entry.put("name", emote.getName());
                        entry.put("animation", emote.getAnimation());
                        emotes.add(entry);
                    }
                }
            }
            catch (Exception e3) {
                LOGGER.warning("Could not read emotes: " + e3.getMessage());
            }
            emotes.sort(Comparator.comparing(e -> (String)e.get("key")));
            ArrayList<String> slots = new ArrayList<String>();
            for (AnimationSlot slot : AnimationSlot.values()) {
                slots.add(slot.name());
            }
            LinkedHashMap<String, Object> envelope = new LinkedHashMap<String, Object>();
            envelope.put("_type", "emotes");
            envelope.put("_emoteCount", emotes.size());
            envelope.put("_exportedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            envelope.put("emotes", emotes);
            envelope.put("animationSlots", slots);
            Path file = this.exportService.exportRaw("emotes", envelope);
            return new ExportResult(emotes.size(), file);
        }
        catch (Exception e4) {
            LOGGER.warning("Failed to export emotes: " + e4.getMessage());
            return new ExportResult(0, null);
        }
    }

    private class ItemsCommand
    extends CommandBase {
        final /* synthetic */ DevExportCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        ItemsCommand(DevExportCommand devExportCommand) {
            DevExportCommand devExportCommand2 = devExportCommand;
            Objects.requireNonNull(devExportCommand2);
            this.this$0 = devExportCommand2;
            super("items", "Export all items");
            this.requirePermission("ks.admin.dev");
        }

        protected void executeSync(CommandContext ctx) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            ExportResult result = this.this$0.exportItems();
            this.this$0.sendResult(ctx, "items", result.count, result.path);
        }
    }

    private class BlocksCommand
    extends CommandBase {
        final /* synthetic */ DevExportCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        BlocksCommand(DevExportCommand devExportCommand) {
            DevExportCommand devExportCommand2 = devExportCommand;
            Objects.requireNonNull(devExportCommand2);
            this.this$0 = devExportCommand2;
            super("blocks", "Export all blocks");
            this.requirePermission("ks.admin.dev");
        }

        protected void executeSync(CommandContext ctx) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            ExportResult result = this.this$0.exportBlocks();
            this.this$0.sendResult(ctx, "blocks", result.count, result.path);
        }
    }

    private class EntitiesCommand
    extends CommandBase {
        final /* synthetic */ DevExportCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        EntitiesCommand(DevExportCommand devExportCommand) {
            DevExportCommand devExportCommand2 = devExportCommand;
            Objects.requireNonNull(devExportCommand2);
            this.this$0 = devExportCommand2;
            super("entities", "Export all entity types and NPC roles");
            this.requirePermission("ks.admin.dev");
        }

        protected void executeSync(CommandContext ctx) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            ExportResult result = this.this$0.exportEntities();
            this.this$0.sendResult(ctx, "entities", result.count, result.path);
        }
    }

    private class CitizensCommand
    extends CommandBase {
        final /* synthetic */ DevExportCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        CitizensCommand(DevExportCommand devExportCommand) {
            DevExportCommand devExportCommand2 = devExportCommand;
            Objects.requireNonNull(devExportCommand2);
            this.this$0 = devExportCommand2;
            super("citizens", "Export all configured citizens");
            this.requirePermission("ks.admin.dev");
        }

        protected void executeSync(CommandContext ctx) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            ExportResult result = this.this$0.exportCitizens();
            this.this$0.sendResult(ctx, "citizens", result.count, result.path);
        }
    }

    private class WorldsCommand
    extends CommandBase {
        final /* synthetic */ DevExportCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        WorldsCommand(DevExportCommand devExportCommand) {
            DevExportCommand devExportCommand2 = devExportCommand;
            Objects.requireNonNull(devExportCommand2);
            this.this$0 = devExportCommand2;
            super("worlds", "Export all worlds");
            this.requirePermission("ks.admin.dev");
        }

        protected void executeSync(CommandContext ctx) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            ExportResult result = this.this$0.exportWorlds();
            this.this$0.sendResult(ctx, "worlds", result.count, result.path);
        }
    }

    private class EmotesCommand
    extends CommandBase {
        final /* synthetic */ DevExportCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        EmotesCommand(DevExportCommand devExportCommand) {
            DevExportCommand devExportCommand2 = devExportCommand;
            Objects.requireNonNull(devExportCommand2);
            this.this$0 = devExportCommand2;
            super("emotes", "Export all emotes and animation slots");
            this.requirePermission("ks.admin.dev");
        }

        protected void executeSync(CommandContext ctx) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            ExportResult result = this.this$0.exportEmotes();
            this.this$0.sendResult(ctx, "emotes", result.count, result.path);
        }
    }

    private class AllCommand
    extends CommandBase {
        final /* synthetic */ DevExportCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        AllCommand(DevExportCommand devExportCommand) {
            DevExportCommand devExportCommand2 = devExportCommand;
            Objects.requireNonNull(devExportCommand2);
            this.this$0 = devExportCommand2;
            super("all", "Export all game data at once");
            this.requirePermission("ks.admin.dev");
        }

        protected void executeSync(CommandContext ctx) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            ctx.sendMessage(Message.raw((String)"\u00a77Exporting all game data..."));
            ExportResult items = this.this$0.exportItems();
            this.this$0.sendResult(ctx, "items", items.count, items.path);
            ExportResult blocks = this.this$0.exportBlocks();
            this.this$0.sendResult(ctx, "blocks", blocks.count, blocks.path);
            ExportResult entities = this.this$0.exportEntities();
            this.this$0.sendResult(ctx, "entities", entities.count, entities.path);
            ExportResult citizens = this.this$0.exportCitizens();
            this.this$0.sendResult(ctx, "citizens", citizens.count, citizens.path);
            ExportResult worlds = this.this$0.exportWorlds();
            this.this$0.sendResult(ctx, "worlds", worlds.count, worlds.path);
            ExportResult emotes = this.this$0.exportEmotes();
            this.this$0.sendResult(ctx, "emotes", emotes.count, emotes.path);
            int total = items.count + blocks.count + entities.count + citizens.count + worlds.count + emotes.count;
            ctx.sendMessage(Message.raw((String)("\u00a7a\u00a7lDone! \u00a7f" + total + " entries\u00a7a exported to \u00a7fdev-export/")));
        }
    }

    private record ExportResult(int count, Path path) {
    }
}

