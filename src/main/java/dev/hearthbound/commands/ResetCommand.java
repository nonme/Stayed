package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.village.VillageData;

import java.util.UUID;

public class ResetCommand extends AbstractPlayerCommand {

    public ResetCommand() {
        super("reset", "Reset village data to start fresh");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        // Stop any active building and clear ghost preview entities
        BuildingSystem.get().reset(store);

        // Despawn elf if exists
        VillageData oldData = store.getComponent(playerRef, VillageData.getComponentType());
        if (oldData != null && oldData.getElfId() != null) {
            UUID elfUuid = oldData.getElfId();
            Entity elf = world.getEntity(elfUuid);
            if (elf != null) {
                elf.remove();
                ctx.sendMessage(Message.raw("Elf sage removed."));
            }
        }

        VillageData data = new VillageData();
        store.putComponent(playerRef, VillageData.getComponentType(), data);
        ctx.sendMessage(Message.raw("Village data reset! Place a Founding Stone to start again."));
    }
}
