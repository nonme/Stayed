package dev.hearthbound.npc;

public final class VillagerSchedulerTravelTest {
    public static void main(String[] args) {
        stuckTravelActivitiesRecallHome();
        nonTravelActivitiesDoNotRecallHome();
    }

    private static void stuckTravelActivitiesRecallHome() {
        assertTrue(VillagerTravelRecovery.shouldRecallHomeOnTravelStuck(VillagerScheduler.ACTIVITY_GOING_TO_WORK),
                "stuck villager going to work should recall home");
        assertTrue(VillagerTravelRecovery.shouldRecallHomeOnTravelStuck(VillagerScheduler.ACTIVITY_GOING_HOME),
                "stuck villager going home should recall home");
        assertTrue(VillagerTravelRecovery.shouldRecallHomeOnTravelStuck(VillagerScheduler.ACTIVITY_GOING_TO_EAT),
                "stuck villager going to eat should recall home");
    }

    private static void nonTravelActivitiesDoNotRecallHome() {
        assertFalse(VillagerTravelRecovery.shouldRecallHomeOnTravelStuck(VillagerScheduler.ACTIVITY_WORKING),
                "working villager should not recall home");
        assertFalse(VillagerTravelRecovery.shouldRecallHomeOnTravelStuck(VillagerScheduler.ACTIVITY_EATING),
                "eating villager should not recall home");
        assertFalse(VillagerTravelRecovery.shouldRecallHomeOnTravelStuck(VillagerScheduler.ACTIVITY_RESTING),
                "resting villager should not recall home");
        assertFalse(VillagerTravelRecovery.shouldRecallHomeOnTravelStuck(VillagerScheduler.ACTIVITY_PATROLLING),
                "patrolling guard should keep patrol recovery behavior");
        assertFalse(VillagerTravelRecovery.shouldRecallHomeOnTravelStuck(null),
                "missing activity should not recall home");
    }

    private static void assertTrue(boolean actual, String message) {
        if (!actual) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean actual, String message) {
        if (actual) {
            throw new AssertionError(message);
        }
    }
}
