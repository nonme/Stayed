package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import dev.hearthbound.npc.NpcManager;

public class SpawnCommand extends AbstractPlayerCommand {

    private final DefaultArg<String> npcTypeArg;

    public SpawnCommand() {
        super("spawn", "Spawn an NPC at your position");
        npcTypeArg = withDefaultArg("type", "NPC role name", ArgTypes.STRING, "Kweebec_Sapling", "Kweebec_Sapling");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        String npcType = ctx.get(npcTypeArg);

        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            ctx.sendMessage(Message.raw("Could not get player position."));
            return;
        }

        Vector3d pos = transform.getPosition();
        Vector3d spawnPos = new Vector3d(pos.getX() + 2, pos.getY(), pos.getZ());

        try {
            var result = NpcManager.spawnNpc(store, spawnPos, new Vector3f(0, 0, 0), npcType);
            ctx.sendMessage(Message.raw("Spawned " + npcType + " at (" +
                    (int) spawnPos.getX() + ", " + (int) spawnPos.getY() + ", " + (int) spawnPos.getZ() + ")"));
        } catch (Exception e) {
            ctx.sendMessage(Message.raw("Failed to spawn NPC: " + e.getMessage()));
        }
    }
}
