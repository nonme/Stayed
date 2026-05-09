package dev.hearthbound.test.steps;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.StayedNpcIdentityComponent;
import dev.hearthbound.npc.StayedRoleNames;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AssertIdentityRoleStep implements TestStep {
    @Override
    public StepResult execute(TestContext ctx) {
        List<String> errors = new ArrayList<>();
        var store = ctx.getStore();
        Archetype<EntityStore> query = Archetype.of(NPCEntity.getComponentType());
        store.forEachChunk(query, (chunk, buf) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
                if (npc == null || npc.getRole() == null) continue;

                UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
                UUID uuid = uuidComponent != null ? uuidComponent.getUuid() : null;
                StayedNpcIdentityComponent identity = store.getComponent(
                        ref, StayedNpcIdentityComponent.getComponentType());
                String componentNpcId = identity != null ? identity.getNpcId() : null;
                String roleName = npc.getRole().getRoleName();
                String roleNpcId = StayedRoleNames.extractNpcId(roleName);

                boolean isStayed = componentNpcId != null
                        || roleNpcId != null
                        || (uuid != null && NpcRegistry.get().getRecord(uuid) != null);
                if (!isStayed) continue;

                String expectedNpcId = componentNpcId != null ? componentNpcId : roleNpcId;
                if (expectedNpcId == null && uuid != null) {
                    NpcRegistry.NpcRecord record = NpcRegistry.get().getRecord(uuid);
                    expectedNpcId = record != null ? record.npcId : null;
                }
                if (expectedNpcId == null) {
                    errors.add("Stayed NPC has no npcId role/component uuid=" + uuid + " role=" + roleName);
                    continue;
                }
                if (!expectedNpcId.equals(roleNpcId)) {
                    errors.add("Stayed NPC role does not contain npcId expected=" + expectedNpcId
                            + " roleNpcId=" + roleNpcId + " role=" + roleName + " uuid=" + uuid);
                }
            }
        });
        return errors.isEmpty()
                ? StepResult.pass("identity roles ok")
                : StepResult.fail(String.join("; ", errors));
    }

    @Override
    public String getName() {
        return "AssertIdentityRole";
    }
}
