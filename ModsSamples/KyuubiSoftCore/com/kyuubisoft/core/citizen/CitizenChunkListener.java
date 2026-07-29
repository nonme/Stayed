/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.math.util.ChunkUtil
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.server.core.HytaleServer
 *  com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class CitizenChunkListener {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Citizens");
    private static final long CHUNK_LOAD_TIMEOUT_MS = 12000L;
    private static final long CHUNK_POLL_INTERVAL_MS = 200L;
    private static final long MIN_SPAWN_AGE_MS = 8000L;
    private static final long STALE_CLEANUP_INTERVAL_MS = 5000L;
    private static final long UUID_RETRY_DELAY_MS = 3000L;
    private final CitizenService citizenService;
    private final Set<String> pendingSpawns = ConcurrentHashMap.newKeySet();
    private final Set<String> citizensPendingNpcResolution = ConcurrentHashMap.newKeySet();
    private final Map<String, Map<Long, List<CitizenData>>> worldChunkIndex = new ConcurrentHashMap<String, Map<Long, List<CitizenData>>>();
    private volatile long cachedGeneration = -1L;
    private volatile long lastStaleCleanup = 0L;

    public CitizenChunkListener(CitizenService citizenService) {
        this.citizenService = citizenService;
    }

    public boolean isPendingSpawn(String citizenId) {
        return this.pendingSpawns.contains(citizenId);
    }

    public void triggerSpawnForChunk(CitizenData citizen, World world, long chunkIndex) {
        if (this.pendingSpawns.add(citizen.id)) {
            this.scheduleSpawnOrReattach(citizen, world, chunkIndex);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void onChunkPreLoad(World world, long chunkIndex) {
        Map index;
        if (this.citizenService.isReloading()) {
            return;
        }
        Map<String, Map<Long, List<CitizenData>>> map = this.worldChunkIndex;
        synchronized (map) {
            long currentGen = this.citizenService.getCitizenGeneration();
            if (currentGen != this.cachedGeneration) {
                this.worldChunkIndex.clear();
                this.cachedGeneration = currentGen;
            }
            index = this.worldChunkIndex.computeIfAbsent(world.getName(), k -> this.buildWorldIndex(world));
        }
        List chunkCitizens = (List)index.get(chunkIndex);
        if (chunkCitizens != null) {
            for (CitizenData citizen : chunkCitizens) {
                boolean hasValidRef;
                long timeSinceCreation = System.currentTimeMillis() - citizen.createdAt;
                if (timeSinceCreation > 0L && timeSinceCreation < 8000L || this.pendingSpawns.contains(citizen.id) || this.citizenService.isCitizenSpawning(citizen.id) || (hasValidRef = citizen.entityRef != null && citizen.entityRef.isValid())) continue;
                if (citizen.entityRef != null) {
                    citizen.entityRef = null;
                }
                this.pendingSpawns.add(citizen.id);
                this.scheduleSpawnOrReattach(citizen, world, chunkIndex);
            }
        }
    }

    private Map<Long, List<CitizenData>> buildWorldIndex(World world) {
        HashMap<Long, List<CitizenData>> index = new HashMap<Long, List<CitizenData>>();
        for (CitizenData citizen : this.citizenService.getAllCitizens()) {
            if (!CitizenService.matchesWorld(citizen.worldName, world.getName()) || citizen.hideNpc) continue;
            Vector3d pos = this.citizenService.resolvePosition(citizen, world);
            long ci = ChunkUtil.indexChunkFromBlock((int)((int)pos.x), (int)((int)pos.z));
            index.computeIfAbsent(ci, k -> new ArrayList()).add(citizen);
        }
        LOGGER.fine("Built chunk index for world '" + world.getName() + "': " + index.size() + " chunks, " + index.values().stream().mapToInt(List::size).sum() + " citizens");
        return index;
    }

    private void scheduleSpawnOrReattach(CitizenData citizen, World world, long chunkIndex) {
        boolean[] completed = new boolean[]{false};
        boolean[] scheduled = new boolean[]{false};
        long startTime = System.currentTimeMillis();
        ScheduledFuture[] taskRef = new ScheduledFuture[]{null};
        taskRef[0] = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                if (completed[0]) {
                    taskRef[0].cancel(false);
                    return;
                }
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= 12000L) {
                    taskRef[0].cancel(false);
                    WorldChunk inMemory = world.getChunkIfInMemory(chunkIndex);
                    if (inMemory != null) {
                        world.loadChunkIfInMemory(chunkIndex);
                        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(() -> {
                            try {
                                WorldChunk loaded = world.getChunkIfLoaded(chunkIndex);
                                if (loaded == null) {
                                    this.pendingSpawns.remove(citizen.id);
                                    return;
                                }
                                this.resolveAndSpawn(citizen, world);
                            }
                            catch (Exception e) {
                                this.pendingSpawns.remove(citizen.id);
                                LOGGER.warning("Error in forced-load spawn for " + citizen.id + ": " + e.getMessage());
                            }
                        }), 1L, TimeUnit.SECONDS);
                    } else {
                        this.pendingSpawns.remove(citizen.id);
                        LOGGER.fine("Chunk load timeout for citizen: " + citizen.id + " (chunk not in memory)");
                    }
                    return;
                }
                if (scheduled[0]) {
                    return;
                }
                scheduled[0] = true;
                world.execute(() -> {
                    try {
                        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
                        if (chunk == null) {
                            scheduled[0] = false;
                            return;
                        }
                        completed[0] = true;
                        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(() -> {
                            try {
                                this.resolveAndSpawn(citizen, world);
                            }
                            catch (Exception e) {
                                this.pendingSpawns.remove(citizen.id);
                                LOGGER.warning("Error spawning/reattaching citizen " + citizen.id + ": " + e.getMessage());
                            }
                        }), 1L, TimeUnit.SECONDS);
                    }
                    catch (Exception e) {
                        this.pendingSpawns.remove(citizen.id);
                        LOGGER.warning("Error spawning/reattaching citizen " + citizen.id + ": " + e.getMessage());
                    }
                });
            }
            catch (Exception e) {
                taskRef[0].cancel(false);
                this.pendingSpawns.remove(citizen.id);
            }
        }, 0L, 200L, TimeUnit.MILLISECONDS);
    }

    private void resolveAndSpawn(CitizenData citizen, World world) {
        boolean refUsable;
        Ref existingRef = null;
        if (citizen.spawnedEntityUUID != null) {
            existingRef = world.getEntityRef(citizen.spawnedEntityUUID);
        }
        if (refUsable = this.checkRefUsable((Ref<EntityStore>)existingRef, citizen.id)) {
            this.citizenService.reattachCitizen(citizen, world, (Ref<EntityStore>)existingRef);
            this.cleanupAndRelease(citizen, world);
        } else if (citizen.spawnedEntityUUID != null) {
            if (!this.citizensPendingNpcResolution.add(citizen.id)) {
                this.pendingSpawns.remove(citizen.id);
                return;
            }
            LOGGER.fine("ChunkListener: UUID exists but entity not found for " + citizen.id + ", retrying in 3000ms");
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(() -> {
                try {
                    Ref retryRef = world.getEntityRef(citizen.spawnedEntityUUID);
                    boolean retryUsable = this.checkRefUsable((Ref<EntityStore>)retryRef, citizen.id);
                    if (retryUsable) {
                        this.citizenService.reattachCitizen(citizen, world, (Ref<EntityStore>)retryRef);
                    } else {
                        LOGGER.info("ChunkListener: UUID retry failed for " + citizen.id + ", spawning fresh");
                        citizen.spawnedEntityUUID = null;
                        citizen.entityRef = null;
                        this.citizenService.spawnCitizen(citizen, world);
                    }
                    this.cleanupAndRelease(citizen, world);
                }
                catch (Exception e) {
                    this.pendingSpawns.remove(citizen.id);
                    LOGGER.warning("Error in UUID retry for " + citizen.id + ": " + e.getMessage());
                }
                finally {
                    this.citizensPendingNpcResolution.remove(citizen.id);
                }
            }), 3000L, TimeUnit.MILLISECONDS);
        } else {
            this.citizenService.spawnCitizen(citizen, world);
            this.cleanupAndRelease(citizen, world);
        }
    }

    private boolean checkRefUsable(Ref<EntityStore> ref, String citizenId) {
        if (ref == null || !ref.isValid()) {
            return false;
        }
        try {
            Store testStore = ref.getStore();
            if (testStore != null) {
                testStore.getComponent(ref, NetworkId.getComponentType());
                return true;
            }
        }
        catch (Exception e) {
            LOGGER.warning("ChunkListener: Stale ref for " + citizenId + ", will respawn fresh");
        }
        return false;
    }

    private void cleanupAndRelease(CitizenData citizen, World world) {
        if (this.citizenService.getStaleUUIDCount() > 0) {
            this.citizenService.cleanupStaleEntities(world);
        }
        this.pendingSpawns.remove(citizen.id);
    }
}

