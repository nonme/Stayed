package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class WarpCommand extends AbstractPlayerCommand {

    private static final String REAL_WORLD = "real_world";
    private static final String FLAT_WORLD = "flat_world";

    private final RequiredArg<String> worldArg;

    public WarpCommand() {
        super("warp", "Teleport between real_world and flat_world");
        worldArg = withRequiredArg("world", "World name: real or flat", ArgTypes.STRING);
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        String arg = ctx.get(worldArg).toLowerCase();
        String targetName = switch (arg) {
            case "real", "real_world" -> REAL_WORLD;
            case "flat", "flat_world" -> FLAT_WORLD;
            default -> null;
        };

        if (targetName == null) {
            ctx.sendMessage(Message.raw("Usage: /hb warp <real|flat>"));
            return;
        }

        if (world.getName().equals(targetName)) {
            ctx.sendMessage(Message.raw("Already in " + targetName + "."));
            return;
        }

        World targetWorld = Universe.get().getWorld(targetName);
        if (targetWorld == null) {
            ctx.sendMessage(Message.raw("World not found: " + targetName));
            return;
        }

        world.execute(() -> {
            Teleport teleport = Teleport.createForPlayer(targetWorld, new Vector3d(0.5, 81.0, 0.5), new Vector3f(0, 0, 0));
            store.addComponent(playerRef, Teleport.getComponentType(), teleport);
        });
    }
}
