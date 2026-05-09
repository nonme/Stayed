package dev.hearthbound.npc;

final class VillagerTravelRecovery {
    private VillagerTravelRecovery() {}

    static boolean shouldRecallHomeOnTravelStuck(String activity) {
        return VillagerScheduler.ACTIVITY_GOING_TO_WORK.equals(activity)
                || VillagerScheduler.ACTIVITY_GOING_HOME.equals(activity)
                || VillagerScheduler.ACTIVITY_GOING_TO_EAT.equals(activity);
    }
}
