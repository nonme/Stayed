package dev.hearthbound.test.steps;

import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import dev.hearthbound.quest.RescueQuestManager;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

/**
 * Starts a rescue quest through the same manager path used by /hb quest and
 * ElfDialogPage, then waits for async chunk search / prefab placement to finish.
 */
public final class StartRescueQuestStep implements TestStep {

    private final RescueQuestManager.QuestVariant variant;
    private final long waitMs;

    public StartRescueQuestStep(RescueQuestManager.QuestVariant variant) {
        this(variant, 20_000L);
    }

    public StartRescueQuestStep(RescueQuestManager.QuestVariant variant, long waitMs) {
        this.variant = variant;
        this.waitMs = waitMs;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        TransformComponent transform = ctx.getStore().getComponent(
                ctx.getPlayerRef(), TransformComponent.getComponentType());
        if (transform == null || transform.getPosition() == null) {
            return StepResult.fail("no player transform");
        }
        UUIDComponent uuid = ctx.getStore().getComponent(
                ctx.getPlayerRef(), UUIDComponent.getComponentType());
        if (uuid == null || uuid.getUuid() == null) {
            return StepResult.fail("no player UUID");
        }

        VillageData village = VillageManager.get().getOrCreateVillageData(
                ctx.getStore(), ctx.getPlayerRef());
        RescueQuestManager.recordVariantPlayed(village, variant);
        village.setRescueQuestStarted(true);
        VillageManager.get().save(ctx.getStore(), ctx.getPlayerRef(), village);

        Player player = ctx.getStore().getComponent(ctx.getPlayerRef(), Player.getComponentType());
        RescueQuestManager.startForPlayer(
                ctx.getWorld(),
                ctx.getStore(),
                ctx.getPlayerRef(),
                player,
                uuid.getUuid(),
                transform.getPosition(),
                variant,
                spawned -> {
                    if (spawned != null) {
                        ctx.put("lastRescueVictimPos", spawned.victimPos());
                        ctx.put("lastRescueEnemyPos", spawned.guardPos());
                        ctx.getLogger().info("StartRescueQuest: spawned " + variant
                                + " victim=" + spawned.victimPos()
                                + " enemy=" + spawned.guardPos());
                    } else {
                        ctx.put("lastRescueFailed", Boolean.TRUE);
                        ctx.getLogger().warn("StartRescueQuest: spawn failed for " + variant);
                    }
                });

        ctx.getLogger().info("StartRescueQuest: requested " + variant + ", waitMs=" + waitMs);
        return StepResult.delay(waitMs);
    }

    @Override
    public String getName() {
        return "StartRescueQuest(" + variant.name().toLowerCase() + ")";
    }
}
