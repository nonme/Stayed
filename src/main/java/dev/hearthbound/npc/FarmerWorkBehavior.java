package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.building.FarmBounds;
import dev.hearthbound.building.FarmScanner;
import dev.hearthbound.building.WarehouseDepositor;
import dev.hearthbound.util.TickScheduler;
import dev.hearthbound.village.BuildingRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Per-villager farming state machine. Stateless from BSON's perspective — runtime-only.
 *
 * Tick-driven (called from VillagerScheduler when a farmer has arrived at the farm and is
 * in ACTIVITY_WORKING). Each tick we either pick a new target via {@link FarmScanner}, walk
 * the NPC to it (by mutating {@code leashPoint}; the JSON role's Seek motion does the actual
 * pathfinding), or perform the action when the NPC has arrived.
 *
 * Priority: harvest (when warehouse exists) > weed > till > replant > water. Harvest first so
 * ripe crops don't sit overripe; weed second so saplings don't grow into trees that block
 * other tiles; till next so freshly-decayed plots become plantable before we look for replant.
 * Replant only fires when we remember which crop used to live on that tile — we never seed
 * a tile with an arbitrary default (no surprise wheat).
 */
public class FarmerWorkBehavior {

    private static final Logger LOGGER = Logger.getLogger(FarmerWorkBehavior.class.getName());

    /** Distance squared at which we consider the farmer "arrived" at the target cell.
     *  JSON role's Seek StopDistance is 1.0 in 3D; in 2D xz that lands the farmer ~1.5–2 blocks
     *  from the target column when standing one block above the soil, so we allow up to 2.5. */
    private static final double ARRIVAL_DIST_SQ = 2.5 * 2.5;

    /** Wait this long after arrival before swinging the tool — gives the NPC a moment to settle. */
    private static final long PRE_ANIM_DELAY_MS = 350;

    /** Wait this long after the swing before applying the world effect. */
    private static final long ANIM_TO_EFFECT_MS = 750;

    /** Hold the freeze a bit after the effect so the swing animation finishes cleanly. */
    private static final long POST_EFFECT_HOLD_MS = 200;

    /** If the farmer can't reach the target within this window, give up and pick another. */
    private static final long REACH_TIMEOUT_MS = 12000;

    private enum Phase { PICKING, SEEKING, ANIMATING, DONE }

    private static final class State {
        FarmScanner.Target target;
        Phase phase = Phase.PICKING;
        long phaseEnteredAt;
        String currentTool;  // last item we equipped (so we don't re-equip every tick)
        boolean frozen;      // whether we currently hold the Frozen component
        boolean animTriggered; // whether we've fired the swing animation for this target
        boolean effectApplied; // whether we've applied the world effect for this target

        // Idle wander: when chooseTarget returns null we drift the farmer to a random spot
        // inside the farm bbox so it looks alive. Re-rolled every WANDER_PERIOD_MS.
        long nextWanderAt;     // wall-clock time of the next leashpoint re-roll

        void enter(Phase next) {
            this.phase = next;
            this.phaseEnteredAt = System.currentTimeMillis();
            if (next != Phase.ANIMATING) {
                this.animTriggered = false;
                this.effectApplied = false;
            }
        }
    }

    /** How often the wander state picks a new leashpoint inside the farm bbox. */
    private static final long WANDER_PERIOD_MS = 4000;

    private final java.util.Random random = new java.util.Random();

    /** Internal-tick period. Short enough that the seek→swing→effect→pick cycle feels live. */
    private static final long INTERNAL_TICK_MS = 300;

    /** UUID-keyed runtime state. Cleared by {@link #clear(UUID)} when villager stops working. */
    private final Map<UUID, State> states = new HashMap<>();

    /** Per-villager farming context — refreshed each {@link #start} call. */
    private static final class Context {
        final World world;
        final UUID npcUuid;
        BuildingRecord farm;
        BuildingRecord warehouse;
        UUID ownerUuid;
        ScheduledFuture<?> task;

        Context(World world, UUID npcUuid, BuildingRecord farm, BuildingRecord warehouse, UUID ownerUuid) {
            this.world = world;
            this.npcUuid = npcUuid;
            this.farm = farm;
            this.warehouse = warehouse;
            this.ownerUuid = ownerUuid;
        }
    }

    /** Active per-farmer tickers. One entry per actively-working farmer. */
    private final Map<UUID, Context> contexts = new HashMap<>();

    /** UUIDs of villagers blocked on a target we couldn't deposit (warehouse full). Skip these silently. */
    private final Map<UUID, Long> backoffUntil = new HashMap<>();

    /**
     * Last known crop on each (farmId, x, z) plot column. Updated on harvest so a later REPLANT
     * pass on the same column re-seeds the same crop. Empty until we observe at least one harvest.
     * Runtime-only; if the server restarts before any harvest, REPLANT skips that column.
     */
    private final Map<String, String> lastCropAt = new HashMap<>();

    /**
     * Begin (or refresh) a farmer's own fast tick loop. Called each time VillagerScheduler
     * confirms the farmer is at the farm in WORKING activity. If a tick loop is already
     * running for this farmer, only the context (farm/warehouse/owner) is refreshed —
     * useful for warehouse-built-after-arrival or farm reassignment.
     *
     * The internal loop runs every {@value #INTERNAL_TICK_MS}ms regardless of village ticks,
     * so seek→swing→effect→pick happens on a 0.3s cadence rather than the 5s village tick.
     */
    public void start(UUID uuid, World world, BuildingRecord farm, BuildingRecord warehouse, UUID ownerUuid) {
        Context existing = contexts.get(uuid);
        if (existing != null) {
            existing.farm = farm;
            existing.warehouse = warehouse;
            existing.ownerUuid = ownerUuid;
            return;
        }

        Context ctx = new Context(world, uuid, farm, warehouse, ownerUuid);
        contexts.put(uuid, ctx);
        ctx.task = TickScheduler.runRepeating(world, INTERNAL_TICK_MS, INTERNAL_TICK_MS,
                () -> internalTick(ctx));
        LOGGER.info("Farmer " + uuid + " tick loop started @ farm "
                + farm.getPosX() + "," + farm.getPosY() + "," + farm.getPosZ());
    }

    /** Should be called when the villager leaves the farm (going home, eating, etc.). */
    public void clear(UUID uuid) {
        State s = states.remove(uuid);
        Context ctx = contexts.remove(uuid);
        if (ctx != null) {
            if (ctx.task != null) ctx.task.cancel(false);
            // Lazy unfreeze on clear — we may not have a valid ref here; the JSON role-swap
            // back to traveling/idle will reset the engine motion, and Frozen on a non-working
            // villager is otherwise harmless.
        }
        if (s != null) s.frozen = false;
    }

    public void clearAll() {
        for (Context ctx : contexts.values()) {
            if (ctx.task != null) ctx.task.cancel(false);
        }
        contexts.clear();
        states.clear();
        backoffUntil.clear();
        lastCropAt.clear();
    }

    /**
     * One iteration of the fast tick loop. Runs on the world thread (TickScheduler.runRepeating
     * wraps the body in {@code world.execute}). Re-resolves the live ref/store every tick — the
     * NPC entity may have been unloaded and re-loaded since the last iteration.
     */
    private void internalTick(Context ctx) {
        UUID uuid = ctx.npcUuid;
        Ref<EntityStore> ref = ctx.world.getEntityRef(uuid);
        if (ref == null || !ref.isValid()) {
            // Entity isn't loaded right now — wait for next tick. If this persists (e.g. the
            // villager despawned), VillagerScheduler will eventually call clear() when the
            // chunk fully unloads, stopping us cleanly.
            return;
        }
        Store<EntityStore> store = ref.getStore();

        Long backoff = backoffUntil.get(uuid);
        if (backoff != null) {
            if (System.currentTimeMillis() < backoff) return;
            backoffUntil.remove(uuid);
        }

        State state = states.computeIfAbsent(uuid, k -> new State());
        long now = System.currentTimeMillis();

        switch (state.phase) {
            case PICKING -> pickTarget(uuid, state, ref, store, ctx.world, ctx.farm, ctx.warehouse);
            case SEEKING -> tickSeeking(uuid, state, ref, store, now);
            case ANIMATING -> tickAnimating(uuid, state, ref, store, ctx.world, ctx.farm, ctx.warehouse,
                    ctx.ownerUuid, now);
            case DONE -> state.enter(Phase.PICKING);
        }
    }

    // ---------------------------------------------------------------------------------------

    private void pickTarget(UUID uuid, State state, Ref<EntityStore> ref, Store<EntityStore> store,
                            World world, BuildingRecord farm, BuildingRecord warehouse) {
        // Make sure we're not still frozen from a previous cycle (e.g. after switchRole back).
        unfreezeIfNeeded(state, ref, store);

        FarmScanner.Scan scan = FarmScanner.scan(world, farm);
        FarmScanner.Target target = chooseTarget(scan, warehouse != null, farm);
        if (target == null) {
            // Nothing to do — drift the farmer around the farm so it looks idle rather than
            // dead-frozen at the spot where the last task ended. Re-rolls leash every
            // WANDER_PERIOD_MS; the JSON Seek motion handles the actual movement. As soon as
            // a real target appears (next pickTarget call), we transition to SEEKING and the
            // wander timer is naturally ignored.
            wanderInsideFarm(uuid, state, ref, store, farm);
            return;
        }
        state.target = target;
        state.nextWanderAt = 0;
        state.enter(Phase.SEEKING);

        LOGGER.info(String.format("Farmer %s picked %s at %d,%d,%d%s",
                uuid, target.action(), target.x(), target.y(), target.z(),
                target.cropType() != null ? " (" + target.cropType() + ")" : ""));
    }

    /**
     * Move the leashpoint to a random spot inside the farm's tilled-soil bbox, but at most
     * once per WANDER_PERIOD_MS. The JSON role's Seek motion will path the farmer there.
     * If FarmBounds returns null (shouldn't happen for a built farm), we quietly skip.
     */
    private void wanderInsideFarm(UUID uuid, State state, Ref<EntityStore> ref, Store<EntityStore> store,
                                   BuildingRecord farm) {
        long now = System.currentTimeMillis();
        if (now < state.nextWanderAt) return;
        state.nextWanderAt = now + WANDER_PERIOD_MS;

        FarmBounds.Bounds b = FarmBounds.compute(farm);
        if (b == null) return;

        // The JSON role's Leash sensor only fires the Seek motion when the leashpoint is
        // farther than its Range (currently 2). If we drop the leash on a tile right next to
        // the farmer, Seek never engages and the farmer just stands. Pick a point that's at
        // least MIN_WANDER_DIST away from the farmer's current xz, retrying a few times if
        // a random roll lands too close. If the bbox is small enough that no point qualifies,
        // we fall through and place the leash on the farthest random pick we found.
        Vector3d pos = position(ref, store);
        double curX = pos != null ? pos.getX() : (b.minX() + b.maxX()) * 0.5;
        double curZ = pos != null ? pos.getZ() : (b.minZ() + b.maxZ()) * 0.5;
        final double MIN_WANDER_DIST_SQ = 9.0; // 3 blocks, comfortably outside Leash Range=2

        int bestX = b.minX(), bestZ = b.minZ();
        double bestDistSq = -1;
        for (int attempt = 0; attempt < 6; attempt++) {
            int rx = b.minX() + random.nextInt(b.maxX() - b.minX() + 1);
            int rz = b.minZ() + random.nextInt(b.maxZ() - b.minZ() + 1);
            double dx = (rx + 0.5) - curX;
            double dz = (rz + 0.5) - curZ;
            double d2 = dx * dx + dz * dz;
            if (d2 >= MIN_WANDER_DIST_SQ) {
                bestX = rx; bestZ = rz; bestDistSq = d2;
                break;
            }
            if (d2 > bestDistSq) {
                bestX = rx; bestZ = rz; bestDistSq = d2;
            }
        }

        // Use the farmer's own y for the leash — if it's stuck below the farm surface (e.g.
        // wandered into a 1-block decorative water pit), pinning the leash to b.maxY()+1 puts
        // the leashpoint above its head, and the Leash sensor's 3D distance check can keep it
        // "in range" so Seek never fires. Letting Y track the farmer means xz distance always
        // dominates and the pathfinder gets a chance to climb back out.
        double leashY = pos != null ? pos.getY() : b.maxY() + 1;
        setLeashPoint(ref, store, bestX + 0.5, leashY, bestZ + 0.5);
        LOGGER.info(String.format("Farmer %s wandering to %d,%d (distSq=%.2f)",
                uuid, bestX, bestZ, bestDistSq));
    }

    /**
     * Priority: harvest > weed > till > replant > water. Harvest is gated by warehouse —
     * without a place to store the drop we won't break ripe crops. Replant is gated by
     * remembered crop type — without a known previous crop we won't seed an empty tile.
     */
    private FarmScanner.Target chooseTarget(FarmScanner.Scan scan, boolean canStore, BuildingRecord farm) {
        if (canStore && !scan.harvest().isEmpty()) return scan.harvest().get(0);
        if (!scan.weed().isEmpty())                return scan.weed().get(0);
        if (!scan.till().isEmpty())                return scan.till().get(0);
        // Replant: only act on tiles whose previous crop we remember.
        for (FarmScanner.Target t : scan.replant()) {
            String remembered = lastCropAt.get(plotKey(farm, t.x(), t.z()));
            if (remembered != null) {
                return new FarmScanner.Target(t.x(), t.y(), t.z(), t.action(), remembered);
            }
        }
        if (!scan.water().isEmpty())               return scan.water().get(0);
        return null;
    }

    private void tickSeeking(UUID uuid, State state, Ref<EntityStore> ref, Store<EntityStore> store,
                             long now) {
        FarmScanner.Target t = state.target;
        if (t == null) { state.enter(Phase.PICKING); return; }

        // Farmer stands ON top of the soil (y+1), so leash must point at the farmer's foot
        // level — not at the block itself. Without this, JSON Seek treats the farmer as
        // "arrived" the moment 3D distance hits StopDistance=1.0 even if the block is still
        // a full tile away in xz. HARVEST is the exception: the crop block is at y+1, so
        // the farmer's feet and the crop share a y, no offset needed.
        double leashY = t.action() == FarmScanner.Action.HARVEST ? t.y() : t.y() + 1;
        setLeashPoint(ref, store, t.x() + 0.5, leashY, t.z() + 0.5);

        Vector3d pos = position(ref, store);
        if (pos == null) return;

        double dx = pos.getX() - (t.x() + 0.5);
        double dz = pos.getZ() - (t.z() + 0.5);
        double distSq = dx * dx + dz * dz;

        if (distSq <= ARRIVAL_DIST_SQ) {
            LOGGER.info(String.format(
                    "Farmer %s arrived at %s @ %d,%d,%d (pos=%.2f,%.2f,%.2f distSq=%.2f) → ANIMATING",
                    uuid, t.action(), t.x(), t.y(), t.z(),
                    pos.getX(), pos.getY(), pos.getZ(), distSq));
            state.enter(Phase.ANIMATING);
            return;
        }

        if (now - state.phaseEnteredAt > REACH_TIMEOUT_MS) {
            LOGGER.info(String.format(
                    "Farmer %s could not reach target %s @ %d,%d,%d — abandoning. "
                    + "farmerPos=(%.2f,%.2f,%.2f) leashedTo=(%.2f,%.2f,%.2f) distSq=%.2f arrivalSq=%.2f",
                    uuid, t.action(), t.x(), t.y(), t.z(),
                    pos.getX(), pos.getY(), pos.getZ(),
                    t.x() + 0.5, leashY, t.z() + 0.5,
                    distSq, ARRIVAL_DIST_SQ));
            state.target = null;
            state.enter(Phase.PICKING);
        }
    }

    private void tickAnimating(UUID uuid, State state, Ref<EntityStore> ref, Store<EntityStore> store,
                               World world, BuildingRecord farm, BuildingRecord warehouse,
                               UUID ownerUuid, long now) {
        FarmScanner.Target t = state.target;
        if (t == null) { state.enter(Phase.PICKING); return; }

        long elapsed = now - state.phaseEnteredAt;

        // Step 1: settle period before the swing.
        if (elapsed < PRE_ANIM_DELAY_MS) {
            return;
        }

        // Step 2: trigger the swing exactly once per target. VillagerScheduler ticks every
        // ~5s, so we can't gate this on a narrow time window — the tick that's "in window"
        // would almost never line up. A flag is the only reliable trigger.
        if (!state.animTriggered) {
            freezeIfNeeded(state, ref, store);
            equipToolFor(uuid, state, world, t.action());
            playAnimationFor(ref, store, uuid, ownerUuid, world, t);
            state.animTriggered = true;
            LOGGER.info(String.format("Farmer %s swung tool for %s @ %d,%d,%d",
                    uuid, t.action(), t.x(), t.y(), t.z()));
        }

        // Step 3: wait for the swing to play out before mutating the world.
        if (elapsed < PRE_ANIM_DELAY_MS + ANIM_TO_EFFECT_MS) {
            return;
        }

        // Step 4: apply the world effect exactly once per target.
        if (!state.effectApplied) {
            boolean ok = switch (t.action()) {
                case HARVEST -> applyHarvest(world, warehouse, t, uuid, farm);
                case WEED    -> applyWeed(world, t);
                case TILL    -> applyTill(world, t, uuid);
                case REPLANT -> applyReplant(world, t, uuid);
                case WATER   -> applyWater(world, t, uuid);
            };
            state.effectApplied = true;
            LOGGER.info(String.format("Farmer %s applied %s @ %d,%d,%d ok=%s",
                    uuid, t.action(), t.x(), t.y(), t.z(), ok));

            if (!ok && t.action() == FarmScanner.Action.HARVEST) {
                backoffUntil.put(uuid, System.currentTimeMillis() + 5000);
            }
        }

        // Step 5: hold the freeze briefly so the swing finishes visually.
        if (elapsed < PRE_ANIM_DELAY_MS + ANIM_TO_EFFECT_MS + POST_EFFECT_HOLD_MS) {
            return;
        }

        unfreezeIfNeeded(state, ref, store);
        state.target = null;
        state.enter(Phase.DONE);
    }

    // --- Actions ----------------------------------------------------------------------------

    private boolean applyHarvest(World world, BuildingRecord warehouse, FarmScanner.Target t, UUID uuid,
                                  BuildingRecord farm) {
        if (warehouse == null) return false;

        String crop = t.cropType() != null ? t.cropType() : "Wheat";
        String itemId = "Plant_Crop_" + crop + "_Item";
        boolean deposited = WarehouseDepositor.deposit(world, warehouse, itemId);
        if (!deposited) {
            LOGGER.info("Farmer " + uuid + ": deposit " + itemId + " failed (warehouse full?) — skipping harvest");
            return false;
        }

        try {
            world.breakBlock(t.x(), t.y(), t.z(), 0);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Farmer " + uuid + ": breakBlock failed", e);
            return false;
        }

        // Remember which crop lived on this column so a future REPLANT pass re-seeds the
        // same species rather than guessing.
        if (t.cropType() != null) {
            lastCropAt.put(plotKey(farm, t.x(), t.z()), t.cropType());
        }

        // Auto-replant the same crop on the tilled cell directly below.
        int soilY = t.y() - 1;
        if (t.cropType() != null && isTilledSoil(world, t.x(), soilY, t.z())) {
            try {
                world.setBlock(t.x(), t.y(), t.z(), "Plant_Crop_" + t.cropType() + "_Block");
            } catch (Exception e) {
                LOGGER.fine("Farmer " + uuid + ": auto-replant failed: " + e.getMessage());
            }
        }

        LOGGER.fine("Farmer " + uuid + " harvested " + itemId + " from "
                + t.x() + "," + t.y() + "," + t.z());
        return true;
    }

    private boolean applyWeed(World world, FarmScanner.Target t) {
        try {
            world.breakBlock(t.x(), t.y(), t.z(), 0);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Farmer: weed breakBlock failed at "
                    + t.x() + "," + t.y() + "," + t.z(), e);
            return false;
        }
    }

    private boolean applyTill(World world, FarmScanner.Target t, UUID uuid) {
        try {
            world.setBlock(t.x(), t.y(), t.z(), "Soil_Dirt_Tilled");
            LOGGER.fine("Farmer " + uuid + " tilled " + t.x() + "," + t.y() + "," + t.z());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Farmer " + uuid + ": till failed", e);
            return false;
        }
    }

    private boolean applyReplant(World world, FarmScanner.Target t, UUID uuid) {
        // chooseTarget already guarantees t.cropType() is non-null for REPLANT — it's pulled
        // from lastCropAt. If it's null something upstream skipped the gate; bail out rather
        // than seeding an arbitrary species.
        String cropType = t.cropType();
        if (cropType == null) {
            LOGGER.fine("Farmer " + uuid + ": replant target had no remembered crop — skipping");
            return false;
        }
        try {
            world.setBlock(t.x(), t.y() + 1, t.z(), "Plant_Crop_" + cropType + "_Block");
            LOGGER.fine("Farmer " + uuid + " replanted " + cropType + " at "
                    + t.x() + "," + (t.y() + 1) + "," + t.z());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Farmer " + uuid + ": replant failed", e);
            return false;
        }
    }

    /**
     * Switch the soil under (x,y,z) to its Watered state-variant. Tries the canonical id
     * first; if a read-back shows the block didn't actually change, walk a few candidate
     * forms (FH-style — see FH_CompanionNPCs FarmSystem.waterCropAt) before giving up.
     */
    private boolean applyWater(World world, FarmScanner.Target t, UUID uuid) {
        String[] candidates = new String[] {
            "Soil_Dirt_Tilled_State_Definitions_Watered",
            "*Soil_Dirt_Tilled_State_Definitions_Watered",
        };
        for (String candidate : candidates) {
            try {
                world.setBlock(t.x(), t.y(), t.z(), candidate);
            } catch (Exception e) {
                LOGGER.fine("Farmer " + uuid + ": water setBlock(" + candidate + ") threw: " + e.getMessage());
                continue;
            }
            // Verify the change took effect — a setBlock with an unknown id silently no-ops
            // on some engine builds.
            try {
                BlockType bt = world.getBlockType(t.x(), t.y(), t.z());
                String id = bt != null ? bt.getId() : null;
                if (id != null) {
                    String norm = id.startsWith("*") ? id.substring(1) : id;
                    if (norm.contains("Watered")) {
                        LOGGER.fine("Farmer " + uuid + " watered " + t.x() + "," + t.y() + "," + t.z()
                                + " (final id=" + id + ")");
                        return true;
                    }
                }
            } catch (Exception e) {
                LOGGER.fine("Farmer " + uuid + ": water verify failed: " + e.getMessage());
            }
        }
        LOGGER.info("Farmer " + uuid + ": water failed at "
                + t.x() + "," + t.y() + "," + t.z() + " — no candidate took effect");
        return false;
    }

    // --- Helpers ---------------------------------------------------------------------------

    private static String plotKey(BuildingRecord farm, int x, int z) {
        // Key per farm + column so two farms with overlapping columns (shouldn't happen,
        // but cheap to guard) don't share remembered crops.
        return farm.getPosX() + "," + farm.getPosY() + "," + farm.getPosZ() + ":" + x + "," + z;
    }

    private static boolean isTilledSoil(World world, int x, int y, int z) {
        try {
            BlockType bt = world.getBlockType(x, y, z);
            if (bt == null) return false;
            String id = bt.getId();
            if (id == null) return false;
            String norm = id.startsWith("*") ? id.substring(1) : id;
            return norm.equals("Soil_Dirt_Tilled")
                || norm.startsWith("Soil_Dirt_Tilled_State_Definitions_");
        } catch (Exception e) {
            return false;
        }
    }

    private static Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        return tc != null ? tc.getPosition() : null;
    }

    private static void setLeashPoint(Ref<EntityStore> ref, Store<EntityStore> store,
                                      double x, double y, double z) {
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc != null) npc.setLeashPoint(new Vector3d(x, y, z));
    }

    private void equipToolFor(UUID uuid, State state, World world, FarmScanner.Action action) {
        String desired = switch (action) {
            case WATER -> "Tool_Watering_Can";
            default    -> "Tool_Hoe_Crude";
        };
        // Read the live slot rather than trusting a cached value: the JSON role's
        // "From=[], To=[Idle]" StateTransition re-equips Tool_Hoe_Crude every time the
        // engine re-enters Idle (after switchRole, restore-from-chunk, etc), so our cached
        // currentTool drifts away from reality. Always sync to whatever is actually held.
        String actual = HotbarUtil.readSlot0(world, uuid);
        if (desired.equals(actual)) {
            state.currentTool = desired;
            return;
        }
        try {
            HotbarUtil.setSlot0(world, uuid, desired);
            state.currentTool = desired;
            LOGGER.info("Farmer " + uuid + " equipped " + desired
                    + " (was " + actual + ") for " + action);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Farmer " + uuid + ": equip " + desired + " failed", e);
        }
    }

    /**
     * Stop the JSON Seek motion + Walk animation while we play the tool swing — same pattern
     * used for the elf builder. Without this, the engine's Walk animation on the Movement
     * slot keeps overriding the Action-slot swing visually.
     */
    private static void freezeIfNeeded(State state, Ref<EntityStore> ref, Store<EntityStore> store) {
        if (state.frozen) return;
        try {
            store.addComponent(ref, Frozen.getComponentType(), Frozen.get());
            try {
                AnimationUtils.stopAnimation(ref, AnimationSlot.Movement, store);
            } catch (Exception ignored) { /* cosmetic only */ }
            state.frozen = true;
        } catch (Exception e) {
            LOGGER.fine("Farmer freeze failed: " + e.getMessage());
        }
    }

    private static void unfreezeIfNeeded(State state, Ref<EntityStore> ref, Store<EntityStore> store) {
        if (!state.frozen) return;
        try {
            store.tryRemoveComponent(ref, Frozen.getComponentType());
        } catch (Exception e) {
            LOGGER.fine("Farmer unfreeze failed: " + e.getMessage());
        }
        state.frozen = false;
    }

    /**
     * Play the tool swing animation and rotate the farmer toward the target block.
     *
     * For Player-appearance NPCs the Action slot is the right channel — AnimationUtils
     * skips the Model.animationSet check for Action and Emote slots and just sends the
     * packet, so item-specific animation files like Hoe/Till and Watering_Can/Water work.
     * If that doesn't render visually we fall back to FH_CompanionNPCs' approach: the
     * Status slot with the bare swing key (Mine / SwingDown / SwingLeft) — same swing
     * animation the engine uses when the player attacks with the equipped item, which
     * looks identical to a Hoe Till for an observer.
     *
     * No silent catches: every failure is logged at FINE so we can see why a swing
     * didn't render in the future.
     */
    private static void playAnimationFor(Ref<EntityStore> ref, Store<EntityStore> store,
                                          UUID npcUuid, UUID ownerUuid, World world,
                                          FarmScanner.Target t) {
        String animFile;
        String animName;
        switch (t.action()) {
            case WATER -> { animFile = "Watering_Can"; animName = "Water"; }
            default    -> { animFile = "Hoe";          animName = "Till";  }
        }

        boolean played = false;
        try {
            AnimationUtils.playAnimation(ref, AnimationSlot.Action, animFile, animName, false, store);
            played = true;
        } catch (Exception e) {
            LOGGER.log(Level.FINE,
                    "Farmer playAnimation(Action," + animFile + "," + animName + ") failed", e);
        }

        // FH-style retry: a generic Status-slot swing using the equipped item's attack
        // animation. Mine swings the held tool downward (looks correct for both Hoe Till
        // and Pickaxe Mine); SwingLeft/SwingDown are the sword fallbacks listed in
        // Hoe.json's Parent: Sword.
        if (!played) {
            String[] fallbackKeys = { "Mine", "SwingDown", "SwingLeft" };
            for (String key : fallbackKeys) {
                try {
                    AnimationUtils.playAnimation(ref, AnimationSlot.Status, key, false, store);
                    played = true;
                    break;
                } catch (Exception e) {
                    LOGGER.log(Level.FINE,
                            "Farmer playAnimation(Status," + key + ") failed", e);
                }
            }
        }

        if (!played) {
            LOGGER.fine("Farmer " + npcUuid + ": no animation channel accepted the swing");
        }

        // Look-at-block packet (owner-only). Reuses BuilderBehavior's rotation packet code.
        if (ownerUuid != null && npcUuid != null) {
            try {
                BuilderBehavior bb = new BuilderBehavior(world, npcUuid, ownerUuid);
                bb.lookAtBlock(t.x(), t.y(), t.z());
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Farmer lookAtBlock failed", e);
            }
        }
    }
}
