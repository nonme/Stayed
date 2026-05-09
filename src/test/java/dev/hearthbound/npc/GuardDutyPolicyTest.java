package dev.hearthbound.npc;

public final class GuardDutyPolicyTest {
    public static void main(String[] args) {
        guardPatrolStartsOnlyAfterArrivingAtGuardhouse();
        guardWeaponIsVisibleOnlyWhileGuarding();
    }

    private static void guardPatrolStartsOnlyAfterArrivingAtGuardhouse() {
        assertFalse(VillagerScheduler.shouldStartGuardPatrol(true, false),
                "guard should walk to the guardhouse before patrol starts");
        assertTrue(VillagerScheduler.shouldStartGuardPatrol(true, true),
                "guard should start patrol after reaching the guardhouse");
        assertFalse(VillagerScheduler.shouldStartGuardPatrol(false, true),
                "non-guard targets should not start guard patrol");
    }

    private static void guardWeaponIsVisibleOnlyWhileGuarding() {
        assertTrue(VillagerScheduler.shouldGuardWeaponBeVisible(true),
                "guard should hold a weapon while patrolling");
        assertFalse(VillagerScheduler.shouldGuardWeaponBeVisible(false),
                "guard should hide weapon outside patrol");
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
