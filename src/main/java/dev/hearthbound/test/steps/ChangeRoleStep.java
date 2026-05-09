package dev.hearthbound.test.steps;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.StayedRoleChangeApplier;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Issues a role change for every record matching {@code filter}. Mirrors the
 * canonical durable role-change path from ElfSage.respawnAs / RescueDialogPage:
 * registry first, then live role application when the generated role is indexed.
 */
public final class ChangeRoleStep implements TestStep {

    private final Predicate<NpcRegistry.NpcRecord> filter;
    private final String newRole;

    public ChangeRoleStep(Predicate<NpcRegistry.NpcRecord> filter, String newRole) {
        this.filter = filter;
        this.newRole = newRole;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        List<NpcRegistry.NpcRecord> targets = new ArrayList<>();
        for (NpcRegistry.NpcRecord r : NpcRegistry.get().allRecords()) {
            if (r.entityUuid != null && filter.test(r)) targets.add(r);
        }

        int changed = 0;
        for (NpcRegistry.NpcRecord r : targets) {
            Ref<EntityStore> ref = ctx.getWorld().getEntityRef(r.entityUuid);
            if (ref == null || !ref.isValid()) continue;

            var npcEntity = ctx.getStore().getComponent(ref, NPCEntity.getComponentType());
            if (npcEntity == null) continue;

            // Persist new role on the registry record (kept under the same npcId).
            NpcRegistry.NpcRecord updated = new NpcRegistry.NpcRecord(
                    r.npcId, r.entityUuid, newRole, r.interaction, r.skinSeed, r.chunkIndex);
            if (r.hasPosition) updated.setPosition(r.lastX, r.lastY, r.lastZ);
            StayedRoleChangeApplier.persistAndApply(ref, ctx.getStore(), ctx.getWorld(), updated,
                    false, "test-change-role");
            changed++;
        }

        ctx.getLogger().info("ChangeRole: " + changed + "/" + targets.size() + " → " + newRole);
        return StepResult.pass(changed + " changed", changed > 0 ? 3_000L : 0L);
    }

    @Override
    public String getName() { return "ChangeRole(" + newRole + ")"; }
}
