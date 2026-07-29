package dev.hearthbound.village;

import dev.hearthbound.events.VillageTickHandler;

public final class VillageCatchUpTest {
    public static void main(String[] args) {
        newVillageHasNoSimulationTimestamp();
        catchUpSkipsUninitializedSaves();
        catchUpCountsFiveSecondTicksAndCapsHugeDeltas();
    }

    private static void newVillageHasNoSimulationTimestamp() {
        VillageData village = new VillageData();
        assertEquals(0L, village.getLastSimTimeEpochMs(), "old or new villages should default to no catch-up timestamp");

        village.setLastSimTimeEpochMs(1234L);

        assertEquals(1234L, village.getLastSimTimeEpochMs(), "timestamp should round-trip in memory");
    }

    private static void catchUpSkipsUninitializedSaves() {
        assertEquals(0, VillageTickHandler.catchUpTicksDue(0L, 10_000L), "uninitialized timestamp should skip catch-up");
        assertEquals(0, VillageTickHandler.catchUpTicksDue(10_000L, 10_000L), "no elapsed time should skip catch-up");
        assertEquals(0, VillageTickHandler.catchUpTicksDue(10_000L, 5_000L), "backwards clock should skip catch-up");
    }

    private static void catchUpCountsFiveSecondTicksAndCapsHugeDeltas() {
        assertEquals(3, VillageTickHandler.catchUpTicksDue(1_000L, 16_000L), "15 seconds should produce three ticks");

        long hugeDeltaNow = 1_000L + (VillageTickHandler.MAX_CATCH_UP_TICKS + 500L) * 5_000L;
        assertEquals(VillageTickHandler.MAX_CATCH_UP_TICKS,
                VillageTickHandler.catchUpTicksDue(1_000L, hugeDeltaNow),
                "huge deltas should be capped");
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + "\nexpected: " + expected + "\nactual:   " + actual);
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + "\nexpected: " + expected + "\nactual:   " + actual);
        }
    }
}
