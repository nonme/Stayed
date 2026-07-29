/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.reward;

public enum RewardType {
    ITEM("item"),
    COMMAND("command"),
    LOOTBAG("lootbag"),
    MMO_XP("mmo_xp"),
    RPG_XP("rpg_xp"),
    CURRENCY("currency"),
    ENDLESS_XP("endless_xp"),
    QUEST_TOKENS("quest_tokens"),
    TITLE("title"),
    ACHIEVEMENT("achievement"),
    LEVELING_XP("leveling_xp");

    private final String key;

    private RewardType(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public static RewardType fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (RewardType t : RewardType.values()) {
            if (!t.key.equals(key)) continue;
            return t;
        }
        return null;
    }
}

