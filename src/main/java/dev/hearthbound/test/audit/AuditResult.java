package dev.hearthbound.test.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated outcome of one {@link NpcRegistryInvariantAudit#run} call.
 *
 * Empty violation list = clean. Otherwise the audit failed and every entry
 * names a specific invariant breach.
 */
public final class AuditResult {

    private final List<Violation> violations;
    private final long durationNanos;

    public AuditResult(List<Violation> violations, long durationNanos) {
        this.violations = Collections.unmodifiableList(new ArrayList<>(violations));
        this.durationNanos = durationNanos;
    }

    public boolean isClean() { return violations.isEmpty(); }
    public List<Violation> getViolations() { return violations; }
    public int getViolationCount() { return violations.size(); }
    public long getDurationNanos() { return durationNanos; }

    public Map<ViolationType, Integer> countsByType() {
        Map<ViolationType, Integer> counts = new EnumMap<>(ViolationType.class);
        for (Violation v : violations) {
            counts.merge(v.getType(), 1, Integer::sum);
        }
        return counts;
    }

    /** One-line summary suitable for chat. */
    public String summary() {
        if (violations.isEmpty()) {
            return "audit clean (" + (durationNanos / 1_000_000) + "ms)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(violations.size()).append(" violation(s): ");
        boolean first = true;
        for (Map.Entry<ViolationType, Integer> e : countsByType().entrySet()) {
            if (!first) sb.append(", ");
            sb.append(e.getKey().name()).append("×").append(e.getValue());
            first = false;
        }
        return sb.toString();
    }
}
