package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.ArrayList;
import java.util.List;

public class NpcCommand extends AbstractCommandCollection {

    public NpcCommand() {
        super("npc", "NPC management commands");
        addSubCommand(new ClearCommand());
    }

    private static class ClearCommand extends AbstractWorldCommand {

        ClearCommand() {
            super("clear", "Remove all NPC entities from the world");
        }

        @Override
        protected void execute(CommandContext ctx, World world, Store<EntityStore> store) {
            List<Ref<EntityStore>> toRemove = new ArrayList<>();
            store.forEachChunk(NPCEntity.getComponentType(), (chunk, cb) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    toRemove.add(chunk.getReferenceTo(i));
                }
            });
            for (Ref<EntityStore> ref : toRemove) {
                store.removeEntity(ref, RemoveReason.REMOVE);
            }
            ctx.sendMessage(Message.raw("Removed " + toRemove.size() + " NPC(s)"));
        }
    }
}
