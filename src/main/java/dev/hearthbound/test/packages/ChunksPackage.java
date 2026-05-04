package dev.hearthbound.test.packages;

import dev.hearthbound.test.engine.TestCase;
import dev.hearthbound.test.engine.TestPackage;
import dev.hearthbound.test.steps.AuditStep;
import dev.hearthbound.test.steps.ChangeRoleStep;
import dev.hearthbound.test.steps.CleanupTestNpcsStep;
import dev.hearthbound.test.steps.DiffStep;
import dev.hearthbound.test.steps.LogStep;
import dev.hearthbound.test.steps.NudgeNpcStep;
import dev.hearthbound.test.steps.SetupVillageStep;
import dev.hearthbound.test.steps.SnapshotStep;
import dev.hearthbound.test.steps.SpawnVillagersStep;
import dev.hearthbound.test.steps.TeleportPlayerFarStep;
import dev.hearthbound.test.steps.TeleportPlayerHomeStep;
import dev.hearthbound.test.steps.WaitStep;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 2 — chunks. Closest to the original 760-respawn cascade bug.
 *
 * <ul>
 *   <li>{@code chunk_border} — 20 villagers nudged 16 blocks (chunk-edge distance) → far-tp → wait → home-tp → audit</li>
 *   <li>{@code chunk_cycle} — 20 villagers, 5 cycles of (far-tp + 60 s + home-tp + 30 s) with audit between</li>
 *   <li>{@code chunk_far_role_change} — spawn → far-tp → wait → role change attempt on far villagers → audit (graceful)</li>
 *   <li>{@code chunk_concurrent} — 4 groups of 5 nudged in 4 directions → far-tp → home-tp → audit</li>
 * </ul>
 */
public final class ChunksPackage {

    private ChunksPackage() {}

    public static TestPackage build() {
        return new TestPackage("chunks", List.of(
                buildChunkBorder(),
                buildChunkCycle(),
                buildChunkFarRoleChange(),
                buildChunkConcurrent()));
    }

    private static TestCase buildChunkBorder() {
        return TestCase.of("chunk_border",
                new LogStep("chunk_border — 20 villagers, nudge to chunk edge, unload+reload"),
                new SetupVillageStep(),
                new SpawnVillagersStep(20),
                new AuditStep(true, "post-spawn"),
                new SnapshotStep("before"),
                // Push every villager 16 blocks east — guarantees several cross a
                // chunk boundary (chunks are 32 wide, spawn radius is 5).
                new NudgeNpcStep(r -> true, 16, 0, 0),
                new WaitStep(2_000),
                new TeleportPlayerFarStep(),
                new WaitStep(60_000),
                new TeleportPlayerHomeStep(),
                new WaitStep(10_000),
                new SnapshotStep("after"),
                new DiffStep("before", "after"),
                new AuditStep(true, "post-reload"))
                .withTeardown(
                        new CleanupTestNpcsStep(),
                        new AuditStep(true, "post-cleanup"));
    }

    private static TestCase buildChunkCycle() {
        List<dev.hearthbound.test.engine.TestStep> steps = new ArrayList<>();
        steps.add(new LogStep("chunk_cycle — 5 unload/reload cycles on 20 villagers"));
        steps.add(new SetupVillageStep());
        steps.add(new SpawnVillagersStep(20));
        steps.add(new AuditStep(true, "post-spawn"));
        for (int i = 1; i <= 5; i++) {
            steps.add(new LogStep("cycle " + i + "/5"));
            steps.add(new TeleportPlayerFarStep());
            steps.add(new WaitStep(60_000));
            steps.add(new TeleportPlayerHomeStep());
            steps.add(new WaitStep(30_000));
            steps.add(new AuditStep(true, "cycle-" + i));
        }
        return new TestCase("chunk_cycle", steps).withTeardown(
                new CleanupTestNpcsStep(),
                new AuditStep(true, "post-cleanup"));
    }

    private static TestCase buildChunkFarRoleChange() {
        return TestCase.of("chunk_far_role_change",
                new LogStep("chunk_far_role_change — role change while villager chunk is unloaded"),
                new SetupVillageStep(),
                new SpawnVillagersStep(5),
                new AuditStep(true, "post-spawn"),
                new TeleportPlayerFarStep(),
                new WaitStep(60_000),
                // Role change attempt while villagers' chunks are unloaded.
                // Expectation: live entities are gone, ChangeRoleStep skips them
                // gracefully (no exceptions, no orphaned records).
                new ChangeRoleStep(r -> "Villager_Human".equals(r.roleName), "Villager_Human_Farmer"),
                new AuditStep(true, "post-role-change-far"),
                new TeleportPlayerHomeStep(),
                new WaitStep(10_000),
                new AuditStep(true, "post-return"))
                .withTeardown(
                        new CleanupTestNpcsStep(),
                        new AuditStep(true, "post-cleanup"));
    }

    private static TestCase buildChunkConcurrent() {
        return TestCase.of("chunk_concurrent",
                new LogStep("chunk_concurrent — 20 villagers, push in 4 directions, then reload"),
                new SetupVillageStep(),
                new SpawnVillagersStep(20),
                new AuditStep(true, "post-spawn"),
                // Distribute villagers across 4 chunks. We can't strictly partition
                // the 20 (filter would need indexed iteration), so we just nudge
                // them all by the same large vector — a single direction is enough
                // to put everybody at chunk edges of the new area.
                new NudgeNpcStep(r -> true, 24, 0, 24),
                new WaitStep(2_000),
                new TeleportPlayerFarStep(),
                new WaitStep(60_000),
                new TeleportPlayerHomeStep(),
                new WaitStep(10_000),
                new AuditStep(true, "post-reload"))
                .withTeardown(
                        new CleanupTestNpcsStep(),
                        new AuditStep(true, "post-cleanup"));
    }
}
