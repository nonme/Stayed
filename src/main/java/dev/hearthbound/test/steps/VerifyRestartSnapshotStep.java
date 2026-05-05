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
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.NpcLiveEntityResolver;
import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.npc.StayedNpcIdentityComponent;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

import java.util.ArrayList;
import java.util.List;

/**
 * Verifies a snapshot written by restart_prepare. The first pass can be
 * registry-only immediately after restart; the second pass should run after
 * teleporting to the saved home position and requires loaded entities.
 */
public final class VerifyRestartSnapshotStep implements TestStep {

    private final boolean requireLiveEntities;
    private final boolean deleteSnapshotOnPass;

    public VerifyRestartSnapshotStep(boolean requireLiveEntities, boolean deleteSnapshotOnPass) {
        this.requireLiveEntities = requireLiveEntities;
        this.deleteSnapshotOnPass = deleteSnapshotOnPass;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        try {
            RestartSnapshotStore.Snapshot snapshot = RestartSnapshotStore.load();
            ctx.put(TeleportPlayerFarStep.HOME_POS_KEY,
                    new double[]{ snapshot.homeX, snapshot.homeY, snapshot.homeZ });

            List<String> errors = new ArrayList<>();
            int checked = 0;
            for (RestartSnapshotStore.Record expected : snapshot.records) {
                if (expected.npcId == null || expected.npcId.isBlank()) {
                    errors.add("snapshot record missing npcId");
                    continue;
                }
                NpcRegistry.NpcRecord live = NpcRegistry.get().getRecordByNpcId(expected.npcId);
                if (live == null) {
                    errors.add("missing npcId " + expected.npcId);
                    continue;
                }
                checked++;
                if (!expected.role.equals(live.roleName)) {
                    errors.add(expected.npcId + " role " + live.roleName + " != " + expected.role);
                }
                if (!expected.interaction.equals(live.interaction.name())) {
                    errors.add(expected.npcId + " interaction " + live.interaction + " != " + expected.interaction);
                }
                if (expected.chunkIndex != live.chunkIndex) {
                    errors.add(expected.npcId + " chunk " + live.chunkIndex + " != " + expected.chunkIndex);
                }
                if (expected.hasPosition != live.hasPosition) {
                    errors.add(expected.npcId + " hasPosition " + live.hasPosition + " != " + expected.hasPosition);
                }

                if (requireLiveEntities) {
                    verifyLiveEntity(ctx, expected, live, errors);
                }
            }

            if (!errors.isEmpty()) {
                for (String error : errors) ctx.getLogger().warn("RestartVerify: " + error);
                return StepResult.fail(errors.size() + " restart mismatch(es)");
            }
            if (deleteSnapshotOnPass) RestartSnapshotStore.delete();
            ctx.getLogger().info("RestartVerify: checked " + checked
                    + " record(s), liveRequired=" + requireLiveEntities);
            return StepResult.pass(checked + " checked");
        } catch (Exception e) {
            return StepResult.fail("verify restart snapshot failed: " + e.getMessage());
        }
    }

    private static void verifyLiveEntity(TestContext ctx, RestartSnapshotStore.Record expected,
                                         NpcRegistry.NpcRecord live, List<String> errors) {
        if (live.entityUuid == null) {
            errors.add(expected.npcId + " entityUuid missing");
            return;
        }
        Ref<EntityStore> ref = ctx.getWorld().getEntityRef(live.entityUuid);
        if (ref == null || !ref.isValid()) {
            ref = NpcLiveEntityResolver.findLiveNpcByRecord(ctx.getStore(), live);
            if (ref != null && ref.isValid()) {
                java.util.UUID resolvedUuid = NpcManager.extractUuid(ctx.getStore(), ref);
                if (resolvedUuid != null && !resolvedUuid.equals(live.entityUuid)) {
                    NpcRegistry.get().bindEntityUuid(live.npcId, resolvedUuid);
                    live = NpcRegistry.get().getRecordByNpcId(live.npcId);
                }
            }
        }
        if (ref == null || !ref.isValid()) {
            errors.add(expected.npcId + " live entity missing uuid=" + live.entityUuid);
            return;
        }
        StayedNpcIdentityComponent identity = ctx.getStore().getComponent(
                ref, StayedNpcIdentityComponent.getComponentType());
        if (identity == null || !expected.npcId.equals(identity.getNpcId())) {
            errors.add(expected.npcId + " HB_NPCID mismatch on live entity");
        }
        TransformComponent tc = ctx.getStore().getComponent(ref, TransformComponent.getComponentType());
        if (tc == null || tc.getPosition() == null) {
            errors.add(expected.npcId + " transform missing");
        } else {
            long liveChunk = ChunkUtil.indexChunkFromBlock(tc.getPosition().x, tc.getPosition().z);
            if (liveChunk != live.chunkIndex) {
                errors.add(expected.npcId + " live chunk " + liveChunk + " != registry " + live.chunkIndex);
            }
        }

        if (live.interaction != NpcRegistry.InteractionType.NONE
                && live.interaction != NpcRegistry.InteractionType.FOLLOWER) {
            if (ctx.getStore().getComponent(ref, Interactable.getComponentType()) == null) {
                errors.add(expected.npcId + " Interactable missing");
            }
            if (ctx.getStore().getComponent(ref, Interactions.getComponentType()) == null) {
                errors.add(expected.npcId + " Interactions missing");
            }
        }
        if (ctx.getStore().getComponent(ref, PlayerSkinComponent.getComponentType()) == null) {
            errors.add(expected.npcId + " PlayerSkinComponent missing");
        }
        if (ctx.getStore().getComponent(ref, ModelComponent.getComponentType()) == null) {
            errors.add(expected.npcId + " ModelComponent missing");
        }
        if (ctx.getStore().getComponent(ref, PersistentModel.getComponentType()) == null) {
            errors.add(expected.npcId + " PersistentModel missing");
        }
    }

    @Override
    public String getName() {
        return "VerifyRestartSnapshot(" + (requireLiveEntities ? "live" : "registry") + ")";
    }
}
