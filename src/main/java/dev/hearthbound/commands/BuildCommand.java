package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.building.BlockPlacer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BuildCommand extends AbstractPlayerCommand {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "hearthbound-builder");
        t.setDaemon(true);
        return t;
    });

    public BuildCommand() {
        super("build", "Build a small test structure block by block");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            ctx.sendMessage(Message.raw("Could not get player position."));
            return;
        }

        Vector3d pos = transform.getPosition();
        int baseX = (int) Math.floor(pos.getX()) + 3;
        int baseY = (int) Math.floor(pos.getY());
        int baseZ = (int) Math.floor(pos.getZ());

        List<BlockPlacer.BlockEntry> blocks = new ArrayList<>();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 3; z++) {
                    if (y == 0 || y == 2 || x == 0 || x == 2 || z == 0 || z == 2) {
                        blocks.add(new BlockPlacer.BlockEntry(
                                baseX + x, baseY + y, baseZ + z, "Rock_Chalk_Brick"));
                    }
                }
            }
        }

        BlockPlacer placer = new BlockPlacer(world, blocks, 200);
        placer.start(SCHEDULER);

        ctx.sendMessage(Message.raw("Building " + blocks.size() + " blocks..."));
    }
}
