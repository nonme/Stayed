package dev.hearthbound.test.packages;

import dev.hearthbound.npc.ElfSage;
import dev.hearthbound.test.engine.TestCase;
import dev.hearthbound.test.engine.TestPackage;
import dev.hearthbound.test.steps.AssertAelinStep;
import dev.hearthbound.test.steps.AuditStep;
import dev.hearthbound.test.steps.ConfirmFoundingStep;
import dev.hearthbound.test.steps.LogStep;
import dev.hearthbound.test.steps.MoveAelinStep;
import dev.hearthbound.test.steps.ResetVillageStep;
import dev.hearthbound.test.steps.TeleportPlayerFarStep;
import dev.hearthbound.test.steps.TeleportPlayerHomeStep;
import dev.hearthbound.test.steps.WaitStep;

import java.util.List;

/**
 * Special lifecycle coverage for Aelin. Aelin is registered as a Stayed NPC,
 * but his spawn/founding/role-change paths are unique and need direct tests.
 */
public final class AelinPackage {

    private AelinPackage() {}

    public static TestPackage build() {
        return new TestPackage("aelin", List.of(
                buildAelinSpawnReload(),
                buildAelinCrossChunk(),
                buildAelinChunkBorder(),
                buildAelinRoleReload(),
                buildAelinFoundingReload()));
    }

    private static TestCase buildAelinSpawnReload() {
        return TestCase.of("aelin_spawn_reload",
                new LogStep("aelin_spawn_reload — /hb reset path, then unload+reload spawn area"),
                new ResetVillageStep(),
                new WaitStep(10_000),
                new AssertAelinStep(ElfSage.ROLE_WANDERER),
                new AuditStep(true, "post-spawn"),
                new TeleportPlayerFarStep(),
                new WaitStep(60_000),
                new TeleportPlayerHomeStep(),
                new WaitStep(12_000),
                new AssertAelinStep(ElfSage.ROLE_WANDERER),
                new AuditStep(true, "post-reload"));
    }

    private static TestCase buildAelinCrossChunk() {
        return TestCase.of("aelin_cross_chunk",
                new LogStep("aelin_cross_chunk — move Aelin to another chunk center, then unload+reload"),
                new ResetVillageStep(),
                new WaitStep(10_000),
                new AssertAelinStep(ElfSage.ROLE_WANDERER),
                new MoveAelinStep(MoveAelinStep.Target.CHUNK_CENTER, 2, 0, ElfSage.ROLE_WANDERER),
                new WaitStep(7_000),
                new AssertAelinStep(ElfSage.ROLE_WANDERER),
                new AuditStep(true, "post-move"),
                new TeleportPlayerFarStep(),
                new WaitStep(60_000),
                new TeleportPlayerHomeStep(),
                new WaitStep(12_000),
                new AssertAelinStep(ElfSage.ROLE_WANDERER),
                new AuditStep(true, "post-reload"));
    }

    private static TestCase buildAelinChunkBorder() {
        return TestCase.of("aelin_chunk_border",
                new LogStep("aelin_chunk_border — move Aelin near chunk edge, then unload+reload"),
                new ResetVillageStep(),
                new WaitStep(10_000),
                new AssertAelinStep(ElfSage.ROLE_WANDERER),
                new MoveAelinStep(MoveAelinStep.Target.CHUNK_BORDER, 1, 1, ElfSage.ROLE_WANDERER),
                new WaitStep(7_000),
                new AssertAelinStep(ElfSage.ROLE_WANDERER),
                new AuditStep(true, "post-border-move"),
                new TeleportPlayerFarStep(),
                new WaitStep(60_000),
                new TeleportPlayerHomeStep(),
                new WaitStep(12_000),
                new AssertAelinStep(ElfSage.ROLE_WANDERER),
                new AuditStep(true, "post-reload"));
    }

    private static TestCase buildAelinRoleReload() {
        return TestCase.of("aelin_role_reload",
                new LogStep("aelin_role_reload — Wanderer -> Builder -> reload -> Villager -> reload"),
                new ResetVillageStep(),
                new WaitStep(10_000),
                new AssertAelinStep(ElfSage.ROLE_WANDERER),
                new MoveAelinStep(MoveAelinStep.Target.CHUNK_CENTER, 1, 0, ElfSage.ROLE_BUILDER),
                new WaitStep(7_000),
                new AssertAelinStep(ElfSage.ROLE_BUILDER),
                new TeleportPlayerFarStep(),
                new WaitStep(60_000),
                new TeleportPlayerHomeStep(),
                new WaitStep(12_000),
                new AssertAelinStep(ElfSage.ROLE_BUILDER),
                new MoveAelinStep(MoveAelinStep.Target.CHUNK_CENTER, 0, 1, ElfSage.ROLE_VILLAGER),
                new WaitStep(7_000),
                new AssertAelinStep(ElfSage.ROLE_VILLAGER),
                new TeleportPlayerFarStep(),
                new WaitStep(60_000),
                new TeleportPlayerHomeStep(),
                new WaitStep(12_000),
                new AssertAelinStep(ElfSage.ROLE_VILLAGER),
                new AuditStep(true, "post-role-reloads"));
    }

    private static TestCase buildAelinFoundingReload() {
        return TestCase.of("aelin_founding_reload",
                new LogStep("aelin_founding_reload — reset Aelin, confirm founding, reload village area"),
                new ResetVillageStep(),
                new WaitStep(10_000),
                new AssertAelinStep(ElfSage.ROLE_WANDERER),
                new ConfirmFoundingStep(8, 8),
                new WaitStep(7_000),
                new AssertAelinStep(ElfSage.ROLE_WANDERER),
                new AuditStep(true, "post-founding"),
                new TeleportPlayerFarStep(),
                new WaitStep(60_000),
                new TeleportPlayerHomeStep(),
                new WaitStep(12_000),
                new AssertAelinStep(ElfSage.ROLE_WANDERER),
                new AuditStep(true, "post-founding-reload"));
    }
}
