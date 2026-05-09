package dev.hearthbound.npc;

final class StayedSpawnRolePolicy {
    private StayedSpawnRolePolicy() {}

    static String selectSpawnRole(String generatedRoleName, String baseRoleName, boolean generatedRoleIndexed) {
        return selectSpawnRole(generatedRoleName, baseRoleName, generatedRoleIndexed, false);
    }

    static String selectSpawnRole(String generatedRoleName, String baseRoleName,
                                  boolean generatedRoleIndexed,
                                  boolean identityAttachedBeforeSpawn) {
        if (generatedRoleIndexed) return generatedRoleName;
        return identityAttachedBeforeSpawn ? baseRoleName : null;
    }
}
