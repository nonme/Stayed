package dev.hearthbound.test.steps;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.npc.ElfSage;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.StayedNpcIdentityComponent;
import dev.hearthbound.npc.StayedRoleNames;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.UUID;

/**
 * Explicit invariant check for the special Aelin NPC. General registry audit
 * catches duplicates/orphans; this step verifies the elf-specific identity,
 * role, interaction, appearance components, and position bookkeeping.
 */
public final class AssertAelinStep implements TestStep {

    private static final double DEFAULT_POSITION_TOLERANCE = 4.0;

    private final String expectedRole;
    private final boolean requireLiveEntity;
    private final boolean requireSameNpcId;
    private final String expectedNpcIdKey;
    private final boolean rememberNpcId;
    private final boolean requireNearRecordedPosition;

    public AssertAelinStep(String expectedRole) {
        this(expectedRole, true, true, "aelin:npcId", true, true);
    }

    public AssertAelinStep(String expectedRole, boolean requireLiveEntity,
                           boolean requireSameNpcId, String expectedNpcIdKey,
                           boolean rememberNpcId, boolean requireNearRecordedPosition) {
        this.expectedRole = expectedRole;
        this.requireLiveEntity = requireLiveEntity;
        this.requireSameNpcId = requireSameNpcId;
        this.expectedNpcIdKey = expectedNpcIdKey;
        this.rememberNpcId = rememberNpcId;
        this.requireNearRecordedPosition = requireNearRecordedPosition;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        VillageData village = VillageManager.get().getVillageData(ctx.getStore(), ctx.getPlayerRef());
        if (village == null) return StepResult.fail("VillageData missing");
        UUID elfId = village.getElfId();
        if (elfId == null) return StepResult.fail("VillageData.elfId missing");

        NpcRegistry.NpcRecord record = NpcRegistry.get().getRecord(elfId);
        if (record == null) return StepResult.fail("Aelin registry record missing for " + elfId);
        if (record.npcId == null || record.npcId.isBlank()) return StepResult.fail("Aelin npcId missing");
        if (record.interaction != NpcRegistry.InteractionType.ELF) {
            return StepResult.fail("Aelin interaction " + record.interaction + " != ELF");
        }
        if (expectedRole != null && !expectedRole.equals(record.baseRoleName())) {
            return StepResult.fail("Aelin registry role " + record.baseRoleName() + " != " + expectedRole);
        }

        String rememberedNpcId = ctx.get(expectedNpcIdKey);
        if (rememberedNpcId == null && rememberNpcId) {
            ctx.put(expectedNpcIdKey, record.npcId);
            rememberedNpcId = record.npcId;
        }
        if (requireSameNpcId && rememberedNpcId != null && !rememberedNpcId.equals(record.npcId)) {
            return StepResult.fail("Aelin npcId changed " + rememberedNpcId + " -> " + record.npcId);
        }

        Ref<EntityStore> ref = ctx.getWorld().getEntityRef(elfId);
        if ((ref == null || !ref.isValid()) && requireLiveEntity) {
            return StepResult.fail("Aelin live entity missing for " + elfId);
        }
        if (ref == null || !ref.isValid()) {
            ctx.getLogger().info("AssertAelin: registry-only npcId=" + record.npcId
                    + " role=" + record.roleName + " uuid=" + elfId);
            return StepResult.pass("registry-only");
        }

        StayedNpcIdentityComponent identity = ctx.getStore().getComponent(
                ref, StayedNpcIdentityComponent.getComponentType());
        if (identity == null || !record.npcId.equals(identity.getNpcId())) {
            return StepResult.fail("Aelin entity HB_NPCID mismatch");
        }

        NPCEntity npcEntity = ctx.getStore().getComponent(ref, NPCEntity.getComponentType());
        if (npcEntity == null) return StepResult.fail("Aelin NPCEntity missing");
        String liveBaseRole = StayedRoleNames.extractBaseRoleName(npcEntity.getRoleName());
        if (expectedRole != null && !expectedRole.equals(liveBaseRole)) {
            return StepResult.fail("Aelin live role " + liveBaseRole
                    + " (" + npcEntity.getRoleName() + ") != " + expectedRole);
        }

        if (ctx.getStore().getComponent(ref, Interactable.getComponentType()) == null) {
            return StepResult.fail("Aelin Interactable missing");
        }
        if (ctx.getStore().getComponent(ref, Interactions.getComponentType()) == null) {
            return StepResult.fail("Aelin Interactions missing");
        }
        if (ctx.getStore().getComponent(ref, PlayerSkinComponent.getComponentType()) == null) {
            return StepResult.fail("Aelin PlayerSkinComponent missing");
        }
        if (ctx.getStore().getComponent(ref, ModelComponent.getComponentType()) == null) {
            return StepResult.fail("Aelin ModelComponent missing");
        }
        if (ctx.getStore().getComponent(ref, PersistentModel.getComponentType()) == null) {
            return StepResult.fail("Aelin PersistentModel missing");
        }

        TransformComponent tc = ctx.getStore().getComponent(ref, TransformComponent.getComponentType());
        if (tc == null || tc.getPosition() == null) return StepResult.fail("Aelin transform missing");
        var pos = tc.getPosition();
        long liveChunk = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        if (record.chunkIndex != liveChunk) {
            return StepResult.fail("Aelin chunkIndex stale " + record.chunkIndex + " != live " + liveChunk);
        }
        if (requireNearRecordedPosition && record.hasPosition) {
            double dx = pos.x - record.lastX;
            double dy = pos.y - record.lastY;
            double dz = pos.z - record.lastZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > DEFAULT_POSITION_TOLERANCE) {
                return StepResult.fail("Aelin position drift " + String.format("%.2f", dist));
            }
        }

        ctx.getLogger().info("AssertAelin: npcId=" + record.npcId
                + " uuid=" + elfId + " role=" + record.roleName
                + " chunk=" + record.chunkIndex
                + " pos=" + (int) pos.x + "," + (int) pos.y + "," + (int) pos.z);
        return StepResult.pass();
    }

    @Override
    public String getName() {
        return "AssertAelin(" + (expectedRole != null ? expectedRole : "any") + ")";
    }
}
