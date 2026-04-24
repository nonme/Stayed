package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BlockCommand extends AbstractPlayerCommand {

    private final DefaultArg<String> blockTypeArg;

    public BlockCommand() {
        super("block", "Place a block in front of you");
        blockTypeArg = withDefaultArg("block", "Block type name", ArgTypes.STRING, "Rock_Chalk_Brick", "Rock_Chalk_Brick");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        String blockType = ctx.get(blockTypeArg);

        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            ctx.sendMessage(Message.raw("Could not get player position."));
            return;
        }

        Vector3d pos = transform.getPosition();
        Vector3d direction = transform.getTransform().getDirection();

        int x = (int) Math.floor(pos.getX() + direction.getX() * 3);
        int y = (int) Math.floor(pos.getY() + 1);
        int z = (int) Math.floor(pos.getZ() + direction.getZ() * 3);

        try {
            world.setBlock(x, y, z, blockType);
            ctx.sendMessage(Message.raw("Placed " + blockType + " at (" + x + ", " + y + ", " + z + ")"));
        } catch (Exception e) {
            ctx.sendMessage(Message.raw("Failed to place block: " + e.getMessage()));
        }
    }
}
