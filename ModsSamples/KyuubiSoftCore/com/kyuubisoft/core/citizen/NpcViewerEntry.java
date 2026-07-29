/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.citizen;

import java.util.UUID;

public record NpcViewerEntry(UUID entityUUID, String npcTypeId, String roleName, double posX, double posY, double posZ, String citizenId, String citizenName, NpcCategory category) {
    public String getDisplayName() {
        if (this.citizenName != null && !this.citizenName.isEmpty()) {
            return this.citizenName;
        }
        if (this.npcTypeId != null && !this.npcTypeId.isEmpty()) {
            return this.npcTypeId;
        }
        if (this.entityUUID != null) {
            return this.entityUUID.toString().substring(0, 8);
        }
        return "Unknown";
    }

    public String getPositionString() {
        return String.format("%.0f, %.0f, %.0f", this.posX, this.posY, this.posZ);
    }

    public String getShortUUID() {
        return this.entityUUID != null ? this.entityUUID.toString().substring(0, 8) : "---";
    }

    public static enum NpcCategory {
        REGISTERED,
        ORPHANED,
        WORLD;

    }
}

