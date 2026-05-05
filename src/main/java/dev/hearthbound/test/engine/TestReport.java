package dev.hearthbound.test.engine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate report for one {@link TestPackage} run. The runner builds this
 * incrementally as cases finish; a final {@link #summaryLine()} is sent to
 * chat and the full report is also dumped at the end of each test log.
 */
public final class TestReport {

    public static final class CaseOutcome {
        public final String caseName;
        public final boolean passed;
        public final String message;
        public final long durationMs;
        public final Path logFile;

        public CaseOutcome(String caseName, boolean passed, String message,
                           long durationMs, Path logFile) {
            this.caseName = caseName;
            this.passed = passed;
            this.message = message != null ? message : "";
            this.durationMs = durationMs;
            this.logFile = logFile;
        }
    }

    private final String packageName;
    private final List<CaseOutcome> outcomes = new ArrayList<>();
    private long startedAtMs = System.currentTimeMillis();

    public TestReport(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() { return packageName; }
    public List<CaseOutcome> getOutcomes() { return outcomes; }

    public void add(CaseOutcome outcome) { outcomes.add(outcome); }

    public int passedCount() {
        return (int) outcomes.stream().filter(o -> o.passed).count();
    }

    public int failedCount() {
        return outcomes.size() - passedCount();
    }

    public boolean allPassed() {
        return failedCount() == 0;
    }

    public long elapsedMs() {
        return System.currentTimeMillis() - startedAtMs;
    }

    public String summaryLine() {
        return packageName + ": " + passedCount() + "/" + outcomes.size() + " passed in "
                + (elapsedMs() / 1000) + "s";
    }

    public String summaryTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== TEST SUMMARY ===\n");
        sb.append(summaryLine()).append('\n');
        sb.append("--------------------\n");
        for (CaseOutcome outcome : outcomes) {
            sb.append(outcome.passed ? "PASS " : "FAIL ");
            sb.append(padRight(outcome.caseName, 34));
            sb.append(' ');
            sb.append(outcome.durationMs / 1000).append("s");
            if (!outcome.passed && !outcome.message.isBlank()) {
                sb.append(" — ").append(outcome.message);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String padRight(String value, int width) {
        String s = value != null ? value : "";
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }
}
