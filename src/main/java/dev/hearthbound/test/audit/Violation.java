package dev.hearthbound.test.audit;

import java.util.UUID;

/**
 * A single NPC-lifecycle invariant violation. Immutable.
 *
 * Either {@code npcId} or {@code entityUuid} (or both) may be null when the
 * violation is about a record/entity that doesn't have one yet.
 */
public final class Violation {

    private final ViolationType type;
    private final String npcId;
    private final UUID entityUuid;
    private final String details;

    public Violation(ViolationType type, String npcId, UUID entityUuid, String details) {
        this.type = type;
        this.npcId = npcId;
        this.entityUuid = entityUuid;
        this.details = details != null ? details : "";
    }

    public ViolationType getType() { return type; }
    public String getNpcId() { return npcId; }
    public UUID getEntityUuid() { return entityUuid; }
    public String getDetails() { return details; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.name());
        if (npcId != null) sb.append(" npcId=").append(npcId);
        if (entityUuid != null) sb.append(" entityUuid=").append(entityUuid);
        if (!details.isEmpty()) sb.append(" — ").append(details);
        return sb.toString();
    }
}
