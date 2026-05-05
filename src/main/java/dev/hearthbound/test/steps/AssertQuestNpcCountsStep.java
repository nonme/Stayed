package dev.hearthbound.test.steps;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.StayedNpcIdentityComponent;
import dev.hearthbound.quest.RescueQuestManager;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

/**
 * Checks both registry and loaded ECS counts for rescue victims/enemies. This
 * catches the historical failure mode where repeated rescue starts left piles
 * of goblins/wolves outside the registry.
 */
public final class AssertQuestNpcCountsStep implements TestStep {

    private final int expectedRescueRecords;
    private final int maxQuestEnemyRecords;
    private final int maxLoadedManagedQuestEnemies;
    private final int maxLoadedUnmanagedQuestEnemiesNearSite;

    public AssertQuestNpcCountsStep(int expectedRescueRecords,
                                    int maxQuestEnemyRecords,
                                    int maxLoadedManagedQuestEnemies) {
        this(expectedRescueRecords, maxQuestEnemyRecords, maxLoadedManagedQuestEnemies, 0);
    }

    public AssertQuestNpcCountsStep(int expectedRescueRecords,
                                    int maxQuestEnemyRecords,
                                    int maxLoadedManagedQuestEnemies,
                                    int maxLoadedUnmanagedQuestEnemiesNearSite) {
        this.expectedRescueRecords = expectedRescueRecords;
        this.maxQuestEnemyRecords = maxQuestEnemyRecords;
        this.maxLoadedManagedQuestEnemies = maxLoadedManagedQuestEnemies;
        this.maxLoadedUnmanagedQuestEnemiesNearSite = maxLoadedUnmanagedQuestEnemiesNearSite;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        int rescueRecords = 0;
        int questEnemyRecords = 0;
        for (NpcRegistry.NpcRecord record : NpcRegistry.get().allRecords()) {
            if (record.interaction == NpcRegistry.InteractionType.RESCUE) {
                rescueRecords++;
            } else if (RescueQuestManager.isQuestEnemyRecord(record)) {
                questEnemyRecords++;
            }
        }

        Vector3d siteCenter = ctx.get("lastRescueEnemyPos");
        if (siteCenter == null) siteCenter = ctx.get("lastRescueVictimPos");
        final Vector3d finalSiteCenter = siteCenter;
        final double siteRadiusSq = 128.0 * 128.0;

        int[] loadedManagedEnemies = {0};
        int[] loadedUnmanagedEnemiesNearSite = {0};
        Archetype<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> npcQuery =
                Archetype.of(NPCEntity.getComponentType());
        ctx.getStore().forEachChunk(npcQuery, (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                var ref = chunk.getReferenceTo(i);
                NPCEntity npc = ctx.getStore().getComponent(ref, NPCEntity.getComponentType());
                if (npc == null || !RescueQuestManager.isQuestEnemyRole(npc.getRoleName())) {
                    continue;
                }
                StayedNpcIdentityComponent identity = ctx.getStore().getComponent(
                        ref, StayedNpcIdentityComponent.getComponentType());
                if (identity != null && identity.getNpcId() != null && !identity.getNpcId().isBlank()) {
                    loadedManagedEnemies[0]++;
                } else if (finalSiteCenter != null && isNearSite(ctx, ref, finalSiteCenter, siteRadiusSq)) {
                    loadedUnmanagedEnemiesNearSite[0]++;
                }
            }
        });

        ctx.getLogger().info("QuestNpcCounts: rescueRecords=" + rescueRecords
                + " questEnemyRecords=" + questEnemyRecords
                + " loadedManagedEnemies=" + loadedManagedEnemies[0]
                + " loadedUnmanagedEnemiesNearSite=" + loadedUnmanagedEnemiesNearSite[0]);

        if (rescueRecords != expectedRescueRecords) {
            return StepResult.fail("rescue records " + rescueRecords
                    + " != " + expectedRescueRecords);
        }
        if (questEnemyRecords > maxQuestEnemyRecords) {
            return StepResult.fail("quest enemy records " + questEnemyRecords
                    + " > " + maxQuestEnemyRecords);
        }
        if (loadedManagedEnemies[0] > maxLoadedManagedQuestEnemies) {
            return StepResult.fail("loaded managed quest enemies " + loadedManagedEnemies[0]
                    + " > " + maxLoadedManagedQuestEnemies);
        }
        if (loadedUnmanagedEnemiesNearSite[0] > maxLoadedUnmanagedQuestEnemiesNearSite) {
            return StepResult.fail("loaded unmanaged quest enemies near site "
                    + loadedUnmanagedEnemiesNearSite[0]
                    + " > " + maxLoadedUnmanagedQuestEnemiesNearSite);
        }
        return StepResult.pass("rescue=" + rescueRecords
                + " enemies=" + questEnemyRecords
                + " loadedEnemies=" + loadedManagedEnemies[0]
                + " unmanagedNearSite=" + loadedUnmanagedEnemiesNearSite[0]);
    }

    private static boolean isNearSite(TestContext ctx,
                                      com.hypixel.hytale.component.Ref<
                                              com.hypixel.hytale.server.core.universe.world.storage.EntityStore> ref,
                                      Vector3d siteCenter,
                                      double radiusSq) {
        TransformComponent transform = ctx.getStore().getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getPosition() == null) return false;
        Vector3d pos = transform.getPosition();
        double dx = pos.getX() - siteCenter.getX();
        double dz = pos.getZ() - siteCenter.getZ();
        return dx * dx + dz * dz <= radiusSq;
    }

    @Override
    public String getName() {
        return "AssertQuestNpcCounts";
    }
}
