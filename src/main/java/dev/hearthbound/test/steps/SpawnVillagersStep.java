package dev.hearthbound.test.steps;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.npc.HearthboundDataStore;
import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.NpcRestorer;
import dev.hearthbound.npc.StayedIntegrationTestNpcMarkerComponent;
import dev.hearthbound.npc.VillagerAppearance;
import dev.hearthbound.npc.VillagerNames;
import dev.hearthbound.npc.appearance.BodyArchetype;
import dev.hearthbound.test.engine.StayedIntegrationTestRunner;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerData;
import dev.hearthbound.village.VillagerSummary;
import it.unimi.dsi.fastutil.Pair;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns N villagers around the player position. Mirrors the canonical spawn
 * path used by {@code SpawnVillagerCommand}: VillagerAppearance, VillagerNames,
 * VillageData summary, NpcRegistry record, and HearthboundDataStore flush.
 *
 * Every spawned NPC carries a {@link StayedIntegrationTestNpcMarkerComponent}
 * so {@link CleanupTestNpcsStep} can remove only test entities later.
 */
public final class SpawnVillagersStep implements TestStep {

    private static final String VILLAGER_ROLE = "Villager_Human";

    private final int count;
    private final double radiusBlocks;

    public SpawnVillagersStep(int count) { this(count, 5.0); }

    public SpawnVillagersStep(int count, double radiusBlocks) {
        this.count = count;
        this.radiusBlocks = radiusBlocks;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        VillageData village = VillageManager.get().getVillageData(
                ctx.getStore(), ctx.getPlayerRef());
        if (village == null || !village.isFounded()) {
            return StepResult.fail("village not founded");
        }

        TransformComponent transform = ctx.getStore().getComponent(
                ctx.getPlayerRef(), TransformComponent.getComponentType());
        if (transform == null || transform.getPosition() == null) {
            return StepResult.fail("no player transform");
        }
        Vector3d playerPos = transform.getPosition();

        int spawned = 0;
        for (int i = 0; i < count; i++) {
            UUID uuid = spawnOne(ctx, playerPos, village);
            if (uuid != null) {
                spawned++;
                StayedIntegrationTestRunner.rememberSpawnedNpc(ctx, uuid);
            }
        }
        HearthboundDataStore.get().save();
        ctx.getLogger().info("SpawnVillagers: spawned " + spawned + "/" + count);
        if (spawned < count) {
            return StepResult.fail("spawned only " + spawned + "/" + count);
        }
        return StepResult.pass(spawned + " villagers");
    }

    private UUID spawnOne(TestContext ctx, Vector3d playerPos, VillageData village) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double dx = (rng.nextDouble() - 0.5) * 2 * radiusBlocks;
        double dz = (rng.nextDouble() - 0.5) * 2 * radiusBlocks;
        Vector3d spawnPos = new Vector3d(
                playerPos.getX() + dx, playerPos.getY(), playerPos.getZ() + dz);

        long skinSeed = rng.nextLong();
        int villagerIndex = village.getVillagerCount();

        Set<String> takenNames = new HashSet<>();
        for (VillagerSummary s : village.getVillagers()) {
            if (s.getFirstName() != null && s.getLastName() != null) {
                takenNames.add(s.getFirstName() + " " + s.getLastName());
            }
        }

        BodyArchetype body = VillagerAppearance.predictBody(skinSeed);
        String[] nameParts = VillagerNames.rollHumanName(skinSeed, body, takenNames);
        String firstName = (nameParts != null && nameParts.length > 0) ? nameParts[0] : "Test";
        String lastName  = (nameParts != null && nameParts.length > 1) ? nameParts[1] : "Villager";

        Pair<Ref<EntityStore>, INonPlayerCharacter> result =
                NpcManager.spawnNpc(ctx.getStore(), spawnPos, new Vector3f(0, 0, 0), VILLAGER_ROLE);
        if (result == null || result.first() == null) {
            ctx.getLogger().warn("SpawnVillagers: spawnNpc returned null");
            return null;
        }
        Ref<EntityStore> npcRef = result.first();

        VillagerAppearance.apply(npcRef, ctx.getStore(), skinSeed, villagerIndex);

        VillagerData data = new VillagerData();
        data.setSkinSeed(skinSeed);
        data.setFirstName(firstName);
        data.setLastName(lastName);
        ctx.getStore().putComponent(npcRef, VillagerData.getComponentType(), data);

        ctx.getStore().putComponent(npcRef,
                StayedIntegrationTestNpcMarkerComponent.getComponentType(),
                new StayedIntegrationTestNpcMarkerComponent(ctx.getTestName()));

        UUID npcUuid = NpcManager.extractUuid(ctx.getStore(), npcRef);
        if (npcUuid == null) {
            ctx.getLogger().warn("SpawnVillagers: extractUuid returned null");
            return null;
        }

        VillagerSummary summary = new VillagerSummary(data);
        summary.setVillagerUuid(npcUuid);
        village.addVillager(summary);
        VillageManager.get().save(ctx.getStore(), ctx.getPlayerRef(), village);

        long chunkIndex = NpcManager.chunkIndexFor(spawnPos);
        NpcRegistry.NpcRecord rec = new NpcRegistry.NpcRecord(
                npcUuid, VILLAGER_ROLE, NpcRegistry.InteractionType.VILLAGER, skinSeed, chunkIndex);
        rec.setPosition(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        // Mark the registry record (and therefore data.json) so /hb test
        // cleanup can find leftover test NPCs after a server restart that
        // interrupted teardown — the entity-side marker component lives only
        // in loaded chunks, so we need this on the registry side too.
        rec.testMarker = ctx.getTestName();
        NpcRegistry.get().registerWithIdentity(ctx.getStore(), npcRef, rec);

        // Set leash + interaction on next world tick so role is fully settled
        double leashX = village.getFoundingStoneX() + 0.5;
        double leashY = village.getFoundingStoneY() + 0.1;
        double leashZ = village.getFoundingStoneZ() + 0.5;

        ctx.getWorld().execute(() -> {
            if (!npcRef.isValid()) return;
            var liveStore = ctx.getWorld().getEntityStore().getStore();
            var npcEntity = liveStore.getComponent(npcRef, NPCEntity.getComponentType());
            if (npcEntity != null) {
                npcEntity.setLeashPoint(new Vector3d(leashX, leashY, leashZ));
            }
            NpcRegistry.NpcRecord live = NpcRegistry.get().getRecord(npcUuid);
            if (live != null) {
                NpcRestorer.restoreAfterRoleChange(npcRef, ctx.getWorld(), live);
            }
        });

        ctx.getLogger().info("SpawnVillager: " + firstName + " " + lastName
                + " uuid=" + npcUuid + " npcId=" + rec.npcId);
        return npcUuid;
    }

    @Override
    public String getName() { return "SpawnVillagers(" + count + ")"; }
}
