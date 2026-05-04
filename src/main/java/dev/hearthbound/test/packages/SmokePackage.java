package dev.hearthbound.test.packages;

import dev.hearthbound.test.engine.TestCase;
import dev.hearthbound.test.engine.TestPackage;
import dev.hearthbound.test.steps.AuditStep;
import dev.hearthbound.test.steps.CleanupTestNpcsStep;
import dev.hearthbound.test.steps.LogStep;
import dev.hearthbound.test.steps.SetupVillageStep;
import dev.hearthbound.test.steps.SpawnVillagersStep;
import dev.hearthbound.test.steps.TeleportPlayerFarStep;
import dev.hearthbound.test.steps.TeleportPlayerHomeStep;
import dev.hearthbound.test.steps.WaitStep;

import java.util.List;

/**
 * Stage-1 smoke tests. If any of these fail, the framework itself is broken
 * — fix the framework before attempting harder packages.
 *
 * <ul>
 *   <li>{@code smoke_spawn} — minimal end-to-end: setup village, spawn 1, audit, cleanup, audit again.</li>
 *   <li>{@code smoke_batch} — spawn 5 at once, audit, cleanup, audit clean.</li>
 *   <li>{@code smoke_persistence} — spawn 3, far-teleport, wait for chunk unload, return, audit.</li>
 * </ul>
 */
public final class SmokePackage {

    private SmokePackage() {}

    public static TestPackage build() {
        return new TestPackage("smoke", List.of(
                buildSmokeSpawn(),
                buildSmokeBatch(),
                buildSmokePersistence()));
    }

    private static TestCase buildSmokeSpawn() {
        return TestCase.of("smoke_spawn",
                new LogStep("smoke_spawn — minimal end-to-end"),
                new SetupVillageStep(),
                new SpawnVillagersStep(1),
                new AuditStep(true, "post-spawn"))
                .withTeardown(
                        new CleanupTestNpcsStep(),
                        new AuditStep(true, "post-cleanup"));
    }

    private static TestCase buildSmokeBatch() {
        return TestCase.of("smoke_batch",
                new LogStep("smoke_batch — 5 villagers, audit, cleanup"),
                new SetupVillageStep(),
                new SpawnVillagersStep(5),
                new AuditStep(true, "post-spawn"))
                .withTeardown(
                        new CleanupTestNpcsStep(),
                        new AuditStep(true, "post-cleanup"));
    }

    private static TestCase buildSmokePersistence() {
        return TestCase.of("smoke_persistence",
                new LogStep("smoke_persistence — spawn, far-tp, return, audit"),
                new SetupVillageStep(),
                new SpawnVillagersStep(3),
                new AuditStep(true, "post-spawn"),
                new TeleportPlayerFarStep(),
                new WaitStep(60_000),
                new TeleportPlayerHomeStep(),
                new WaitStep(10_000),
                new AuditStep(true, "post-return"))
                .withTeardown(
                        new CleanupTestNpcsStep(),
                        new AuditStep(true, "post-cleanup"));
    }
}
