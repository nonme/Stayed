package dev.hearthbound.test.steps;

import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compares two snapshots (a, b) previously captured by {@link SnapshotStep}
 * and logs the differences to the test log. Always passes — diffs are
 * informational. A test that wants "no changes" enforcement should pair this
 * with an AuditStep or a custom assertion step.
 */
public final class DiffStep implements TestStep {

    private final String a;
    private final String b;

    public DiffStep(String a, String b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        RegistrySnapshot snapA = ctx.get(SnapshotStep.KEY_PREFIX + a);
        RegistrySnapshot snapB = ctx.get(SnapshotStep.KEY_PREFIX + b);
        if (snapA == null) return StepResult.fail("snapshot a not found: " + a);
        if (snapB == null) return StepResult.fail("snapshot b not found: " + b);

        List<String> changes = new ArrayList<>();

        Set<String> npcIdsAll = new HashSet<>();
        npcIdsAll.addAll(snapA.npcsByNpcId.keySet());
        npcIdsAll.addAll(snapB.npcsByNpcId.keySet());
        for (String id : npcIdsAll) {
            var ea = snapA.npcsByNpcId.get(id);
            var eb = snapB.npcsByNpcId.get(id);
            if (ea == null) { changes.add("npc added: " + id + " (" + eb.roleName + ")"); continue; }
            if (eb == null) { changes.add("npc removed: " + id + " (" + ea.roleName + ")"); continue; }
            if (!java.util.Objects.equals(ea.entityUuid, eb.entityUuid)) {
                changes.add("npc " + id + " entityUuid: " + ea.entityUuid + " → " + eb.entityUuid);
            }
            if (!java.util.Objects.equals(ea.roleName, eb.roleName)) {
                changes.add("npc " + id + " role: " + ea.roleName + " → " + eb.roleName);
            }
            if (ea.chunkIndex != eb.chunkIndex) {
                changes.add("npc " + id + " chunk: " + ea.chunkIndex + " → " + eb.chunkIndex);
            }
        }

        Set<String> bldKeysAll = new HashSet<>();
        bldKeysAll.addAll(snapA.buildingsByKey.keySet());
        bldKeysAll.addAll(snapB.buildingsByKey.keySet());
        for (String k : bldKeysAll) {
            var ba = snapA.buildingsByKey.get(k);
            var bb = snapB.buildingsByKey.get(k);
            if (ba == null) { changes.add("building added: " + k); continue; }
            if (bb == null) { changes.add("building removed: " + k); continue; }
            if (!java.util.Objects.equals(ba.assignedVillager, bb.assignedVillager)) {
                changes.add("building " + k + " assigned: "
                        + ba.assignedVillager + " → " + bb.assignedVillager);
            }
            if (ba.completed != bb.completed) {
                changes.add("building " + k + " completed: " + ba.completed + " → " + bb.completed);
            }
        }

        if (changes.isEmpty()) {
            ctx.getLogger().info("Diff[" + a + "→" + b + "]: no changes");
        } else {
            ctx.getLogger().info("Diff[" + a + "→" + b + "]: " + changes.size() + " change(s)");
            for (String c : changes) ctx.getLogger().info("  " + c);
        }
        return StepResult.pass(changes.size() + " changes");
    }

    @Override
    public String getName() { return "Diff(" + a + "→" + b + ")"; }
}
