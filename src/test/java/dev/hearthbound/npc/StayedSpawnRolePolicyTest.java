package dev.hearthbound.npc;

public final class StayedSpawnRolePolicyTest {
    public static void main(String[] args) {
        indexedGeneratedRoleIsUsedForSpawn();
        unindexedGeneratedRoleBlocksUnsafeFallback();
        unindexedGeneratedRoleCanUseBaseRoleWhenIdentityIsPreAttached();
    }

    private static void indexedGeneratedRoleIsUsedForSpawn() {
        String role = StayedSpawnRolePolicy.selectSpawnRole(
                "Stayed_abc_Villager_Human_Role", "Villager_Human", true);
        assertEquals("Stayed_abc_Villager_Human_Role", role,
                "indexed generated role should be used directly");
    }

    private static void unindexedGeneratedRoleBlocksUnsafeFallback() {
        String role = StayedSpawnRolePolicy.selectSpawnRole(
                "Stayed_abc_Villager_Human_Role", "Villager_Human", false);
        assertEquals(null, role,
                "unindexed generated role must not spawn a persistent NPC with an unsafe base role");
    }

    private static void unindexedGeneratedRoleCanUseBaseRoleWhenIdentityIsPreAttached() {
        String role = StayedSpawnRolePolicy.selectSpawnRole(
                "Stayed_abc_Villager_Human_Role", "Villager_Human", false, true);
        assertEquals("Villager_Human", role,
                "base role fallback is safe only when HB_NPCID is attached before AddReason.SPAWN");
    }

    private static void assertEquals(String expected, String actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
