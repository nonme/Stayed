package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

public class DebugCommand extends AbstractCommandCollection {

    public DebugCommand() {
        super("debug", "Debug commands for development");
        addSubCommand(new ResetElfCommand());
        addSubCommand(new StatusCommand());
        addSubCommand(new GiveStoneCommand());
    }

    /** Resets only elf dialog flags so intro plays again. */
    private static class ResetElfCommand extends AbstractPlayerCommand {
        ResetElfCommand() { super("elf", "Reset elf dialog flags (metElf + foundingStoneGiven)"); }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
            VillageData village = VillageManager.get().getOrCreateVillageData(store, playerRef);
            village.setMetElf(false);
            village.setFoundingStoneGiven(false);
            VillageManager.get().save(store, playerRef, village);
            ctx.sendMessage(Message.raw("[debug] Elf flags reset — dialog starts from INTRO_1 again."));
        }
    }

    /** Prints current VillageData state. */
    private static class StatusCommand extends AbstractPlayerCommand {
        StatusCommand() { super("status", "Print current VillageData to chat"); }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
            VillageData v = store.getComponent(playerRef, VillageData.getComponentType());
            if (v == null) {
                ctx.sendMessage(Message.raw("[debug] No VillageData found."));
                return;
            }
            ctx.sendMessage(Message.raw(
                "[debug] stage=" + v.getStage() +
                " metElf=" + v.isMetElf() +
                " stoneGiven=" + v.isFoundingStoneGiven() +
                " founded=" + v.isFounded() +
                " name=\"" + v.getVillageName() + "\""
            ));
        }
    }

    /** Resets only foundingStoneGiven so stone can be re-given. */
    private static class GiveStoneCommand extends AbstractPlayerCommand {
        GiveStoneCommand() { super("givestone", "Reset foundingStoneGiven flag so elf gives stone again"); }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
            VillageData village = VillageManager.get().getOrCreateVillageData(store, playerRef);
            village.setFoundingStoneGiven(false);
            VillageManager.get().save(store, playerRef, village);
            ctx.sendMessage(Message.raw("[debug] foundingStoneGiven reset — talk to elf about settlement again."));
        }
    }
}
