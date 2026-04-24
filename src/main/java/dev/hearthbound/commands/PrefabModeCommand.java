package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.building.BuildingSystem;

/**
 * Toggles prefab-authoring mode for the caller. While active, placing or breaking a
 * Founding Stone does NOT trigger ghost preview / village founding / village reset —
 * the block behaves like any inert asset, which is what we want when dropping it into
 * a prefab selection inside the editor.
 */
public class PrefabModeCommand extends AbstractPlayerCommand {

    public PrefabModeCommand() {
        super("prefabmode", "Toggle prefab authoring mode (suppresses Founding Stone ghost preview)");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                           PlayerRef player, World world) {
        boolean enabled = BuildingSystem.get().togglePrefabAuthoring(player.getUuid());
        ctx.sendMessage(Message.raw(enabled
                ? "Prefab authoring mode ON — Founding Stone placements/breaks will be ignored."
                : "Prefab authoring mode OFF — Founding Stone is back to normal."));
    }
}
