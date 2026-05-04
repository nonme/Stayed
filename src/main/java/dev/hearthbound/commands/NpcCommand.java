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
import dev.hearthbound.test.audit.AuditResult;
import dev.hearthbound.test.audit.NpcRegistryInvariantAudit;
import dev.hearthbound.test.audit.Violation;

import java.util.ArrayList;
import java.util.List;

public class NpcCommand extends AbstractCommandCollection {

    public NpcCommand() {
        super("npc", "NPC management commands");
        addSubCommand(new ClearCommand());
        addSubCommand(new AuditCommand());
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

    /**
     * One-shot NPC-registry invariant audit. Prints a one-line summary and one
     * line per violation. Same checks as {@code NpcRegistrySelfCheck}, on demand.
     */
    private static class AuditCommand extends AbstractWorldCommand {

        AuditCommand() {
            super("audit", "Run the NPC-registry invariant audit and print violations");
        }

        @Override
        protected void execute(CommandContext ctx, World world, Store<EntityStore> store) {
            AuditResult result = NpcRegistryInvariantAudit.run(world, store);
            ctx.sendMessage(Message.raw("Audit: " + result.summary()));
            for (Violation v : result.getViolations()) {
                ctx.sendMessage(Message.raw("  " + v));
            }
        }
    }
}
