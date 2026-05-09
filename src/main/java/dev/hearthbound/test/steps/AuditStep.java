package dev.hearthbound.test.steps;

import dev.hearthbound.test.audit.AuditResult;
import dev.hearthbound.test.audit.NpcRegistryInvariantAudit;
import dev.hearthbound.test.audit.Violation;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.npc.HearthboundDataStore;
import dev.hearthbound.npc.NpcPositionTracker;

/**
 * Runs {@link NpcRegistryInvariantAudit}. If {@code failOnError} is true,
 * any violation aborts the case; otherwise violations are logged and the
 * step still passes.
 */
public final class AuditStep implements TestStep {

    private final boolean failOnError;
    private final String label;

    public AuditStep() { this(true, "audit"); }
    public AuditStep(boolean failOnError) { this(failOnError, "audit"); }
    public AuditStep(boolean failOnError, String label) {
        this.failOnError = failOnError;
        this.label = label;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        boolean changed = NpcPositionTracker.syncLoadedNow(ctx.getWorld(), ctx.getStore());
        if (changed) HearthboundDataStore.get().markDirty();
        AuditResult result = NpcRegistryInvariantAudit.run(ctx.getWorld(), ctx.getStore());
        ctx.getLogger().info(label + ": " + result.summary());
        for (Violation v : result.getViolations()) {
            ctx.getLogger().warn(label + " violation: " + v);
        }
        if (!result.isClean() && failOnError) {
            return StepResult.fail(result.summary());
        }
        return StepResult.pass(result.summary());
    }

    @Override
    public String getName() { return "Audit(" + label + ")"; }
}
