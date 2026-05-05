package dev.hearthbound.test.packages;

import dev.hearthbound.quest.RescueQuestManager;
import dev.hearthbound.test.engine.TestCase;
import dev.hearthbound.test.engine.TestPackage;
import dev.hearthbound.test.steps.AssertQuestNpcCountsStep;
import dev.hearthbound.test.steps.AuditStep;
import dev.hearthbound.test.steps.LogStep;
import dev.hearthbound.test.steps.ResetVillageStep;
import dev.hearthbound.test.steps.StartRescueQuestStep;
import dev.hearthbound.test.steps.TeleportPlayerFarStep;
import dev.hearthbound.test.steps.TeleportToRescueSiteStep;
import dev.hearthbound.test.steps.WaitStep;

import java.util.List;

/**
 * Rescue quest lifecycle tests. Focuses on the separate enemy/victim path,
 * especially disposable quest enemies that used to be outside the NPC registry.
 */
public final class RescuePackage {

    private RescuePackage() {}

    public static TestPackage build() {
        return new TestPackage("rescue", List.of(
                buildTrapSingle(),
                buildTrapRepeated(),
                buildCampSingle(),
                buildCampChunkReload()));
    }

    private static TestCase buildTrapSingle() {
        return TestCase.of("rescue_trap_single",
                new LogStep("rescue_trap_single — one trapped villager + one managed goblin"),
                new ResetVillageStep(),
                new StartRescueQuestStep(RescueQuestManager.QuestVariant.TRAP),
                // The trap goblin can fall onto spikes and die. Keep the
                // assertion as an upper bound: no pile-up, not guaranteed alive.
                new AssertQuestNpcCountsStep(1, 1, 1),
                new AuditStep(true, "post-rescue-trap"))
                .withTeardown(new ResetVillageStep(), new AuditStep(true, "post-reset"));
    }

    private static TestCase buildTrapRepeated() {
        return TestCase.of("rescue_trap_repeated",
                new LogStep("rescue_trap_repeated — repeated quest starts do not pile up goblins"),
                new ResetVillageStep(),
                new StartRescueQuestStep(RescueQuestManager.QuestVariant.TRAP),
                new AssertQuestNpcCountsStep(1, 1, 1),
                new StartRescueQuestStep(RescueQuestManager.QuestVariant.TRAP),
                new AssertQuestNpcCountsStep(1, 1, 1),
                new StartRescueQuestStep(RescueQuestManager.QuestVariant.TRAP),
                new AssertQuestNpcCountsStep(1, 1, 1),
                new AuditStep(true, "post-repeated-rescue-trap"))
                .withTeardown(new ResetVillageStep(), new AuditStep(true, "post-reset"));
    }

    private static TestCase buildCampSingle() {
        return TestCase.of("rescue_camp_single",
                new LogStep("rescue_camp_single — one trapped villager + three managed camp goblins"),
                new ResetVillageStep(),
                new StartRescueQuestStep(RescueQuestManager.QuestVariant.CAMP),
                new TeleportToRescueSiteStep(),
                new WaitStep(5_000),
                new AssertQuestNpcCountsStep(1, 3, 3),
                new AuditStep(true, "post-rescue-camp"))
                .withTeardown(new ResetVillageStep(), new AuditStep(true, "post-reset"));
    }

    private static TestCase buildCampChunkReload() {
        return TestCase.of("rescue_camp_chunk_reload",
                new LogStep("rescue_camp_chunk_reload — camp enemies survive chunk reload without duplicates"),
                new ResetVillageStep(),
                new StartRescueQuestStep(RescueQuestManager.QuestVariant.CAMP),
                new TeleportToRescueSiteStep(),
                new WaitStep(5_000),
                new AssertQuestNpcCountsStep(1, 3, 3),
                new TeleportPlayerFarStep(),
                new WaitStep(60_000),
                new TeleportToRescueSiteStep(),
                new WaitStep(15_000),
                new AssertQuestNpcCountsStep(1, 3, 3),
                new AuditStep(true, "post-rescue-camp-reload"))
                .withTeardown(new ResetVillageStep(), new AuditStep(true, "post-reset"));
    }
}
