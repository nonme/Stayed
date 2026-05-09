package dev.hearthbound.test.engine;

/**
 * Outcome of one {@link TestStep} execution.
 *
 * Three flavours:
 *   <ul>
 *     <li>{@code pass}  — proceed to next step after the runner's default inter-step delay.</li>
 *     <li>{@code fail}  — abort the case; runner records the message and moves on to the next case.</li>
 *     <li>{@code wait}  — pass, but ask the runner to wait {@code waitMs} ms before the next step.</li>
 *   </ul>
 */
public final class StepResult {

    private final boolean passed;
    private final String message;
    private final long waitMs;

    private StepResult(boolean passed, String message, long waitMs) {
        this.passed = passed;
        this.message = message != null ? message : "";
        this.waitMs = waitMs;
    }

    public static StepResult pass() {
        return new StepResult(true, "", 0);
    }

    public static StepResult pass(String message) {
        return new StepResult(true, message, 0);
    }

    public static StepResult pass(String message, long waitMs) {
        return new StepResult(true, message, Math.max(0, waitMs));
    }

    public static StepResult fail(String message) {
        return new StepResult(false, message, 0);
    }

    /** Step succeeded; runner should wait the given delay before proceeding. */
    public static StepResult delay(long ms) {
        return new StepResult(true, "", Math.max(0, ms));
    }

    public boolean isPassed() { return passed; }
    public String getMessage() { return message; }
    public long getWaitMs() { return waitMs; }
}
