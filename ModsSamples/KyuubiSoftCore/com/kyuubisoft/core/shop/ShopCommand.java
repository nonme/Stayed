/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.command.system.AbstractCommand
 *  com.hypixel.hytale.server.core.command.system.CommandContext
 *  com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
 *  com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.shop;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.shop.ShopConfig;
import com.kyuubisoft.core.shop.ShopService;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Logger;

public class ShopCommand
extends AbstractCommandCollection {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Shop");

    protected boolean canGeneratePermission() {
        return false;
    }

    public ShopCommand() {
        super("ksshop", "Shop system");
        this.addSubCommand((AbstractCommand)new OpenCommand(this));
        this.addSubCommand((AbstractCommand)new SellCommand(this));
        this.addSubCommand((AbstractCommand)new ListCommand(this));
    }

    private static String parseArgAfter(String inputString, String keyword) {
        String[] parts = inputString.trim().split("\\s+");
        for (int i = 0; i < parts.length; ++i) {
            if (!keyword.equalsIgnoreCase(parts[i]) || i + 1 >= parts.length) continue;
            return parts[i + 1];
        }
        return null;
    }

    private class OpenCommand
    extends AbstractPlayerCommand {
        protected boolean canGeneratePermission() {
            return false;
        }

        OpenCommand(ShopCommand shopCommand) {
            Objects.requireNonNull(shopCommand);
            super("open", "Open a shop");
            this.setAllowsExtraArguments(true);
        }

        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
            Player player = (Player)ctx.senderAs(Player.class);
            if (!player.hasPermission("ks.shop.user.use", true)) {
                player.sendMessage(Message.raw((String)"No permission.").color("#FF5555"));
                return;
            }
            CoreI18n i18n = CoreI18n.getInstance();
            ShopService shopService = ShopService.getInstance();
            if (shopService == null) {
                ctx.sendMessage(Message.raw((String)i18n.get("shop.error.not_available")));
                return;
            }
            String shopId = ShopCommand.parseArgAfter(ctx.getInputString(), "open");
            if (shopId == null) {
                ctx.sendMessage(Message.raw((String)i18n.get("shop.usage.open")));
                return;
            }
            ShopConfig config = shopService.getShop(shopId);
            if (config == null) {
                ctx.sendMessage(Message.raw((String)("Shop '" + shopId + "' not found.")).color("#FF5555"));
                return;
            }
            shopService.openShop(player, playerRef, ref, store, shopId);
        }
    }

    private class SellCommand
    extends AbstractPlayerCommand {
        protected boolean canGeneratePermission() {
            return false;
        }

        SellCommand(ShopCommand shopCommand) {
            Objects.requireNonNull(shopCommand);
            super("sell", "Open a shop in sell mode");
            this.setAllowsExtraArguments(true);
        }

        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
            Player player = (Player)ctx.senderAs(Player.class);
            if (!player.hasPermission("ks.shop.user.use", true)) {
                player.sendMessage(Message.raw((String)"No permission.").color("#FF5555"));
                return;
            }
            CoreI18n i18n = CoreI18n.getInstance();
            ShopService shopService = ShopService.getInstance();
            if (shopService == null) {
                ctx.sendMessage(Message.raw((String)i18n.get("shop.error.not_available")));
                return;
            }
            String shopId = ShopCommand.parseArgAfter(ctx.getInputString(), "sell");
            if (shopId == null) {
                ctx.sendMessage(Message.raw((String)"Usage: /ksshop sell <shopId>"));
                return;
            }
            ShopConfig config = shopService.getShop(shopId);
            if (config == null) {
                ctx.sendMessage(Message.raw((String)("Shop '" + shopId + "' not found.")).color("#FF5555"));
                return;
            }
            if (!config.sellEnabled) {
                ctx.sendMessage(Message.raw((String)i18n.get("shop.sell.error.disabled")).color("#FF5555"));
                return;
            }
            shopService.openSellShop(player, playerRef, ref, store, shopId, null);
        }
    }

    private class ListCommand
    extends AbstractPlayerCommand {
        protected boolean canGeneratePermission() {
            return false;
        }

        ListCommand(ShopCommand shopCommand) {
            Objects.requireNonNull(shopCommand);
            super("list", "List available shops");
        }

        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
            Player player = (Player)ctx.senderAs(Player.class);
            if (!player.hasPermission("ks.shop.user.use", true)) {
                player.sendMessage(Message.raw((String)"No permission.").color("#FF5555"));
                return;
            }
            CoreI18n i18n = CoreI18n.getInstance();
            ShopService shopService = ShopService.getInstance();
            if (shopService == null) {
                ctx.sendMessage(Message.raw((String)i18n.get("shop.error.not_available")));
                return;
            }
            Collection<ShopConfig> shops = shopService.getAllShops();
            if (shops.isEmpty()) {
                ctx.sendMessage(Message.raw((String)i18n.get("shop.list.empty")));
            } else {
                ctx.sendMessage(Message.raw((String)i18n.get("shop.list.header", shops.size())));
                for (ShopConfig shop : shops) {
                    String title = shop.title != null ? i18n.get(shop.title) : shop.id;
                    ctx.sendMessage(Message.raw((String)("  - " + shop.id + " (" + title + ", " + shop.items.size() + " items)")));
                }
            }
        }
    }
}

