package dev.hearthbound.test.packages;

import dev.hearthbound.test.engine.TestCase;
import dev.hearthbound.test.engine.TestPackage;
import dev.hearthbound.test.steps.AssertNpcPositionsSyncedStep;
import dev.hearthbound.test.steps.AssertIdentityRoleStep;
import dev.hearthbound.test.steps.AuditStep;
import dev.hearthbound.test.steps.CleanupTestNpcsStep;
import dev.hearthbound.test.steps.LogStep;
import dev.hearthbound.test.steps.LoadRestartSnapshotChunksStep;
import dev.hearthbound.test.steps.MoveNpcsToChunkStep;
import dev.hearthbound.test.steps.SaveRestartSnapshotStep;
import dev.hearthbound.test.steps.SetupVillageStep;
import dev.hearthbound.test.steps.SpawnVillagersStep;
import dev.hearthbound.test.steps.TeleportPlayerHomeStep;
import dev.hearthbound.test.steps.VerifyRestartSnapshotStep;
import dev.hearthbound.test.steps.WaitStep;

import java.util.List;

/**
 * Manual two-phase server restart persistence test.
 *
 * Run:
 *   /hb test run restart_prepare
 *   restart the server
 *   /hb test run restart_verify
 */
public final class RestartPackage {

    private RestartPackage() {}

    public static TestPackage build() {
        return new TestPackage("restart", List.of(
                buildPrepare(),
                buildVerify()));
    }

    private static TestCase buildPrepare() {
        return TestCase.of("restart_prepare",
                new LogStep("restart_prepare — create persisted test NPCs, save snapshot, then restart server manually"),
                new SetupVillageStep(),
                new SpawnVillagersStep(6),
                new WaitStep(3_000),
                new MoveNpcsToChunkStep(r -> "Villager_Human".equals(r.baseRoleName()), 2, 0, 16.0, 16.0),
                new WaitStep(7_000),
                new AssertNpcPositionsSyncedStep(r -> "Villager_Human".equals(r.baseRoleName())),
                new AssertIdentityRoleStep(),
                new AuditStep(true, "pre-restart"),
                new SaveRestartSnapshotStep());
    }

    private static TestCase buildVerify() {
        return TestCase.of("restart_verify",
                new LogStep("restart_verify — verify snapshot after a real server restart"),
                new VerifyRestartSnapshotStep(false, false),
                new LoadRestartSnapshotChunksStep(),
                new TeleportPlayerHomeStep(),
                new WaitStep(22_000),
                new VerifyRestartSnapshotStep(true, true),
                new AssertIdentityRoleStep(),
                new AuditStep(true, "post-restart"))
                .skipPreRunCleanup()
                .withTeardown(
                        new CleanupTestNpcsStep(),
                        new AuditStep(true, "post-cleanup"));
    }
}
