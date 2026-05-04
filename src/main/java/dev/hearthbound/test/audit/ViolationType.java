package dev.hearthbound.test.audit;

/**
 * Kinds of NPC-lifecycle invariant violations detected by
 * {@link NpcRegistryInvariantAudit}. See TESTING.md §4 for full definitions.
 */
public enum ViolationType {
    /** Two or more loaded entities share the same {@code npcId}. */
    DUPLICATE_ID,
    /** Entity carries an {@code HB_NPCID} that has no record in NpcRegistry. */
    ORPHAN_ENTITY,
    /** Registry record claims a chunk that is loaded, but no entity in that chunk matches. */
    MISSING_ENTITY,
    /** Live entity's chunk does not match the chunk index recorded in NpcRegistry. */
    POSITION_DRIFT,
    /** VillageData references a villager UUID that no longer resolves to any registry record. */
    STALE_VILLAGE_REF,
    /** Two byEntityUuid entries share the same npcId — two records claim the same identity. */
    DUPLICATE_NPCID_IN_REGISTRY,
    /** byNpcId and byEntityUuid disagree on which record a given npcId/entityUuid resolves to. */
    INDEX_DESYNC,
}
