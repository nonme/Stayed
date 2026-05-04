package dev.hearthbound.test.packages;

import dev.hearthbound.test.engine.TestCase;
import dev.hearthbound.test.engine.TestPackage;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.test.steps.AuditStep;
import dev.hearthbound.test.steps.ChangeRoleStep;
import dev.hearthbound.test.steps.CleanupTestNpcsStep;
import dev.hearthbound.test.steps.DiffStep;
import dev.hearthbound.test.steps.LogStep;
import dev.hearthbound.test.steps.SetupVillageStep;
import dev.hearthbound.test.steps.SnapshotStep;
import dev.hearthbound.test.steps.SpawnVillagersStep;
import dev.hearthbound.test.steps.TeleportPlayerFarStep;
import dev.hearthbound.test.steps.TeleportPlayerHomeStep;
import dev.hearthbound.test.steps.WaitStep;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 2 — roles. Validates RoleChangeSystem.requestRoleChange via the
 * canonical path used by ElfSage.respawnAs / RescueDialogPage.
 *
 * <ul>
 *   <li>{@code role_basic} — spawn 1, change role, audit, change back, audit</li>
 *   <li>{@code role_mass} — 30 villagers, 3 role changes each, audit between</li>
 *   <li>{@code role_rescue_to_follower} — Trapped → Follower → Villager (the path that originally produced 760 respawns)</li>
 *   <li>{@code role_during_reload} — role change while chunk in flight: spawn → role change → far-tp before settle → wait → home-tp → audit</li>
 * </ul>
 */
public final class RolesPackage {

    private RolesPackage() {}

    private static final String VILLAGER     = "Villager_Human";
    private static final String FARMER       = "Villager_Human_Farmer";
    private static final String LUMBERJACK   = "Villager_Human_Lumberjack";
    private static final String MINER        = "Villager_Human_Miner";
    private static final String RESCUE_TRAP  = "Villager_Rescue_Trapped";
    private static final String RESCUE_FOLLOW = "Villager_Rescue_Follower";

    public static TestPackage build() {
        return new TestPackage("roles", List.of(
                buildRoleBasic(),
                buildRoleMass(),
                buildRoleRescueToFollower(),
                buildRoleDuringReload()));
    }

    private static TestCase buildRoleBasic() {
        return TestCase.of("role_basic",
                new LogStep("role_basic — Villager → Farmer → Villager"),
                new SetupVillageStep(),
                new SpawnVillagersStep(1),
                new AuditStep(true, "post-spawn"),
                new SnapshotStep("before"),
                new ChangeRoleStep(r -> VILLAGER.equals(r.roleName), FARMER),
                new WaitStep(2_000),
                new AuditStep(true, "post-to-farmer"),
                new ChangeRoleStep(r -> FARMER.equals(r.roleName), VILLAGER),
                new WaitStep(2_000),
                new AuditStep(true, "post-back-to-villager"),
                new SnapshotStep("after"),
                new DiffStep("before", "after"))
                .withTeardown(
                        new CleanupTestNpcsStep(),
                        new AuditStep(true, "post-cleanup"));
    }

    private static TestCase buildRoleMass() {
        List<TestStep> steps = new ArrayList<>();
        steps.add(new LogStep("role_mass — 30 villagers, 3 role rotations each"));
        steps.add(new SetupVillageStep());
        steps.add(new SpawnVillagersStep(30));
        steps.add(new AuditStep(true, "post-spawn"));

        // Cycle through Farmer → Lumberjack → Miner → Villager. Three full
        // role changes per villager exercises the role-change path enough to
        // surface any leak/orphan that builds up over repeated calls.
        for (int i = 0; i < 3; i++) {
            steps.add(new ChangeRoleStep(r -> VILLAGER.equals(r.roleName), FARMER));
            steps.add(new WaitStep(1_000));
            steps.add(new AuditStep(true, "to-farmer-" + i));
            steps.add(new ChangeRoleStep(r -> FARMER.equals(r.roleName), LUMBERJACK));
            steps.add(new WaitStep(1_000));
            steps.add(new AuditStep(true, "to-lumber-" + i));
            steps.add(new ChangeRoleStep(r -> LUMBERJACK.equals(r.roleName), MINER));
            steps.add(new WaitStep(1_000));
            steps.add(new AuditStep(true, "to-miner-" + i));
            steps.add(new ChangeRoleStep(r -> MINER.equals(r.roleName), VILLAGER));
            steps.add(new WaitStep(1_000));
            steps.add(new AuditStep(true, "to-villager-" + i));
        }
        return new TestCase("role_mass", steps).withTeardown(
                new CleanupTestNpcsStep(),
                new AuditStep(true, "post-cleanup"));
    }

    private static TestCase buildRoleRescueToFollower() {
        // The original cascade was a Rescue_Trapped → Rescue_Follower →
        // Villager_Human chain triggered by the RescueDialogPage "agree" button.
        // We can't drive the dialog programmatically (no UI scripting), so we
        // reproduce the same chain via direct ChangeRoleStep calls.
        return TestCase.of("role_rescue_to_follower",
                new LogStep("role_rescue_to_follower — Trapped → Follower → Villager"),
                new SetupVillageStep(),
                new SpawnVillagersStep(3),
                new AuditStep(true, "post-spawn"),
                // Send all spawned villagers through the rescue chain.
                new ChangeRoleStep(r -> VILLAGER.equals(r.roleName), RESCUE_TRAP),
                new WaitStep(2_000),
                new AuditStep(true, "post-to-trapped"),
                new ChangeRoleStep(r -> RESCUE_TRAP.equals(r.roleName), RESCUE_FOLLOW),
                new WaitStep(2_000),
                new AuditStep(true, "post-to-follower"),
                new ChangeRoleStep(r -> RESCUE_FOLLOW.equals(r.roleName), VILLAGER),
                new WaitStep(2_000),
                new AuditStep(true, "post-back-to-villager"))
                .withTeardown(
                        new CleanupTestNpcsStep(),
                        new AuditStep(true, "post-cleanup"));
    }

    private static TestCase buildRoleDuringReload() {
        return TestCase.of("role_during_reload",
                new LogStep("role_during_reload — role change immediately before chunk unload"),
                new SetupVillageStep(),
                new SpawnVillagersStep(5),
                new AuditStep(true, "post-spawn"),
                // Issue role change, then immediately tp away — chunk unloads
                // mid-restore. On return the audit must still be clean.
                new ChangeRoleStep(r -> VILLAGER.equals(r.roleName), FARMER),
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
