/*
 * Decompiled with CFR 0.152.
 */
package com.yourname.companion.storage;

import com.yourname.companion.data.CompanionMode;
import com.yourname.companion.data.CompanionRecord;
import com.yourname.companion.data.PlayerCompanionData;
import com.yourname.companion.runtime.CompanionRuntimeState;
import com.yourname.companion.runtime.PlayerRuntimeState;
import com.yourname.companion.storage.CompanionDataStore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CompanionManager {
    private final CompanionDataStore dataStore;
    private final Map<UUID, PlayerCompanionData> persistentData = new HashMap<UUID, PlayerCompanionData>();
    private final Map<String, CompanionRuntimeState> runtimeData = new HashMap<String, CompanionRuntimeState>();
    private final Map<UUID, PlayerRuntimeState> playerRuntimeData = new HashMap<UUID, PlayerRuntimeState>();
    private final Map<UUID, Set<String>> pendingResummon = new HashMap<UUID, Set<String>>();

    public CompanionManager(CompanionDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void load() {
        this.persistentData.clear();
        this.pendingResummon.clear();
        this.persistentData.putAll(this.dataStore.loadAll());
        for (Map.Entry<UUID, PlayerCompanionData> entry : this.persistentData.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerCompanionData data = entry.getValue();
            if (data == null) continue;
            data.ownerId = playerId;
            if (data.companions == null) {
                data.companions = new ArrayList<CompanionRecord>();
            }
            HashSet<String> seenIds = new HashSet<String>();
            ArrayList<CompanionRecord> deduped = new ArrayList<CompanionRecord>();
            for (CompanionRecord c : data.companions) {
                if (c == null) continue;
                if (c.uniqueId == null || c.uniqueId.isBlank()) {
                    c.uniqueId = UUID.randomUUID().toString();
                }
                if (!seenIds.add(c.uniqueId)) continue;
                deduped.add(c);
            }
            data.companions = deduped;
            for (CompanionRecord c : data.companions) {
                c.normalizeAppearancePersistence();
                c.normalizeFarmAreaPersistence();
                if (c.active) {
                    this.pendingResummon.computeIfAbsent(playerId, k -> new HashSet()).add(c.uniqueId);
                    c.active = false;
                }
                c.entityId = null;
            }
            if (data.selectedCompanionId == null || data.findCompanion(data.selectedCompanionId) != null) continue;
            data.selectedCompanionId = null;
        }
    }

    public Set<String> consumePendingResummons(UUID playerId) {
        Set<String> ids = this.pendingResummon.remove(playerId);
        return ids != null ? ids : Set.of();
    }

    public void markPendingResummon(UUID playerId, String companionUniqueId) {
        if (playerId == null || companionUniqueId == null || companionUniqueId.isBlank()) {
            return;
        }
        this.pendingResummon.computeIfAbsent(playerId, k -> new HashSet()).add(companionUniqueId);
    }

    public void save() throws IOException {
        this.snapshotRuntimeToPersistentRecords();
        this.dataStore.saveAll(this.persistentData);
    }

    public PlayerCompanionData getOrCreate(UUID ownerId) {
        PlayerCompanionData data = this.persistentData.get(ownerId);
        if (data == null) {
            data = new PlayerCompanionData(ownerId);
            this.persistentData.put(ownerId, data);
        }
        return data;
    }

    public CompanionRuntimeState getRuntime(String companionUniqueId) {
        CompanionRuntimeState state = this.runtimeData.get(companionUniqueId);
        if (state == null) {
            state = new CompanionRuntimeState();
            this.hydrateRuntimeFromPersistentRecord(this.findCompanionRecord(companionUniqueId), state);
            this.runtimeData.put(companionUniqueId, state);
        }
        return state;
    }

    public PlayerCompanionData get(UUID ownerId) {
        return this.persistentData.get(ownerId);
    }

    public void resetRuntime(String companionUniqueId) {
        CompanionRuntimeState state = this.runtimeData.get(companionUniqueId);
        if (state == null) {
            this.runtimeData.put(companionUniqueId, new CompanionRuntimeState());
            this.clearPersistedWorkState(this.findCompanionRecord(companionUniqueId));
            return;
        }
        state.reset();
        this.clearPersistedWorkState(this.findCompanionRecord(companionUniqueId));
    }

    public void clearRuntime(String companionUniqueId) {
        this.snapshotRuntimeToPersistentRecord(this.findCompanionRecord(companionUniqueId), this.runtimeData.get(companionUniqueId));
        this.runtimeData.remove(companionUniqueId);
    }

    public void clearAllRuntimes(UUID ownerId) {
        PlayerCompanionData data = this.persistentData.get(ownerId);
        if (data == null) {
            return;
        }
        for (CompanionRecord c : data.companions) {
            this.snapshotRuntimeToPersistentRecord(c, this.runtimeData.get(c.uniqueId));
            this.runtimeData.remove(c.uniqueId);
        }
    }

    public void snapshotRuntimeToPersistentRecords() {
        for (PlayerCompanionData data : this.persistentData.values()) {
            if (data == null || data.companions == null) continue;
            for (CompanionRecord companion : data.companions) {
                if (companion == null || companion.uniqueId == null) continue;
                this.snapshotRuntimeToPersistentRecord(companion, this.runtimeData.get(companion.uniqueId));
            }
        }
    }

    private void snapshotRuntimeToPersistentRecord(CompanionRecord companion, CompanionRuntimeState runtime) {
        if (companion == null || runtime == null) {
            return;
        }
        companion.farmDepositPending = runtime.farmDepositPending;
        companion.farmStatusText = runtime.farmStatusText;
    }

    private void hydrateRuntimeFromPersistentRecord(CompanionRecord companion, CompanionRuntimeState runtime) {
        if (companion == null || runtime == null) {
            return;
        }
        runtime.farmDepositPending = companion.farmDepositPending;
        runtime.farmStatusText = companion.farmStatusText;
        if (companion.mode == CompanionMode.FARMER && companion.farmAutoResume) {
            runtime.commandActive = true;
            runtime.lastFarmScanTick = -1000000L;
            if (runtime.farmStatusText == null || runtime.farmStatusText.isBlank()) {
                runtime.farmStatusText = "Resuming farm";
            }
        }
    }

    private void clearPersistedWorkState(CompanionRecord companion) {
        if (companion == null) {
            return;
        }
        companion.farmDepositPending = false;
        companion.farmStatusText = null;
    }

    private CompanionRecord findCompanionRecord(String companionUniqueId) {
        if (companionUniqueId == null || companionUniqueId.isBlank()) {
            return null;
        }
        for (PlayerCompanionData data : this.persistentData.values()) {
            if (data == null || data.companions == null) continue;
            for (CompanionRecord companion : data.companions) {
                if (companion == null || !companionUniqueId.equals(companion.uniqueId)) continue;
                return companion;
            }
        }
        return null;
    }

    public PlayerRuntimeState getPlayerRuntime(UUID ownerId) {
        PlayerRuntimeState state = this.playerRuntimeData.get(ownerId);
        if (state == null) {
            state = new PlayerRuntimeState();
            this.playerRuntimeData.put(ownerId, state);
        }
        return state;
    }

    public void resetPlayerRuntime(UUID ownerId) {
        this.playerRuntimeData.put(ownerId, new PlayerRuntimeState());
    }

    public Set<UUID> getAllActiveCompanionEntityIds() {
        HashSet<UUID> ids = new HashSet<UUID>();
        for (PlayerCompanionData data : this.persistentData.values()) {
            for (CompanionRecord c : data.companions) {
                if (!c.active || c.entityId == null) continue;
                try {
                    ids.add(UUID.fromString(c.entityId));
                }
                catch (IllegalArgumentException illegalArgumentException) {}
            }
        }
        return ids;
    }

    public UUID findOwnerByCompanionEntityId(UUID entityId) {
        if (entityId == null) {
            return null;
        }
        String entityText = entityId.toString();
        for (Map.Entry<UUID, PlayerCompanionData> entry : this.persistentData.entrySet()) {
            PlayerCompanionData data = entry.getValue();
            if (data == null || data.companions == null) continue;
            for (CompanionRecord c : data.companions) {
                if (c == null || !c.active || c.entityId == null || !entityText.equalsIgnoreCase(c.entityId)) continue;
                return entry.getKey();
            }
        }
        return null;
    }

    public void markLaunchSuppressionByEntityId(UUID entityId, long durationMs) {
        if (entityId == null || durationMs <= 0L) {
            return;
        }
        String entityText = entityId.toString();
        long until = System.currentTimeMillis() + durationMs;
        for (Map.Entry<UUID, PlayerCompanionData> entry : this.persistentData.entrySet()) {
            CompanionRecord record;
            PlayerCompanionData data = entry.getValue();
            if (data == null || data.companions == null || (record = data.findCompanionByEntityId(entityText)) == null) continue;
            CompanionRuntimeState runtime = this.getRuntime(record.uniqueId);
            if (runtime.launchSuppressUntilMs < until) {
                runtime.launchSuppressUntilMs = until;
            }
            return;
        }
    }

    public void markReactiveAttackerByEntityId(UUID companionEntityId, UUID attackerEntityId, long durationMs) {
        if (companionEntityId == null || attackerEntityId == null || durationMs <= 0L) {
            return;
        }
        String companionEntityText = companionEntityId.toString();
        long until = System.currentTimeMillis() + durationMs;
        for (Map.Entry<UUID, PlayerCompanionData> entry : this.persistentData.entrySet()) {
            CompanionRecord record;
            PlayerCompanionData data = entry.getValue();
            if (data == null || data.companions == null || (record = data.findCompanionByEntityId(companionEntityText)) == null) continue;
            CompanionRuntimeState runtime = this.getRuntime(record.uniqueId);
            runtime.reactiveAttackerId = attackerEntityId.toString();
            runtime.reactiveAttackerUntilMs = until;
            return;
        }
    }

    public int markOwnerCompanionsReactiveAttacker(UUID ownerId, UUID attackerEntityId, long durationMs) {
        if (ownerId == null || attackerEntityId == null || durationMs <= 0L) {
            return 0;
        }
        PlayerCompanionData data = this.persistentData.get(ownerId);
        if (data == null || data.companions == null) {
            return 0;
        }
        long until = System.currentTimeMillis() + durationMs;
        String attackerText = attackerEntityId.toString();
        int marked = 0;
        for (CompanionRecord record : data.companions) {
            if (record == null || !record.active || record.fallen || record.uniqueId == null) continue;
            CompanionRuntimeState runtime = this.getRuntime(record.uniqueId);
            runtime.reactiveAttackerId = attackerText;
            runtime.reactiveAttackerUntilMs = until;
            runtime.lastCombatScanTick = -1000000L;
            ++marked;
        }
        return marked;
    }

    public boolean isNameTaken(String name, String excludeCompanionId) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT).trim();
        for (PlayerCompanionData data : this.persistentData.values()) {
            for (CompanionRecord c : data.companions) {
                if (c.uniqueId != null && c.uniqueId.equals(excludeCompanionId) || c.name == null || !c.name.toLowerCase(Locale.ROOT).trim().equals(lower)) continue;
                return true;
            }
        }
        return false;
    }
}

