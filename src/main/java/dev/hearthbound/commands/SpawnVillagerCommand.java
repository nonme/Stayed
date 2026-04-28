package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.NpcRestorer;
import dev.hearthbound.npc.VillagerAppearance;
import dev.hearthbound.npc.VillagerNames;
import dev.hearthbound.npc.HearthboundDataStore;
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
 * Spawns a fully-registered villager, equivalent to what happens after a successful rescue quest.
 * The villager gets a random appearance, is added to VillageData, and wanders around the
 * founding stone — no quest flow required.
 *
 * Usage: /hb spawnvillager
 */
public class SpawnVillagerCommand extends AbstractPlayerCommand {

    private static final String VILLAGER_ROLE = "Villager_Human";

    public SpawnVillagerCommand() {
        super("spawnvillager", "Spawn a fully-registered test villager (skips rescue quest)");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                           PlayerRef player, World world) {
        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null || !village.isFounded()) {
            ctx.sendMessage(Message.raw("No founded village found. Found a village first."));
            return;
        }

        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            ctx.sendMessage(Message.raw("Could not get player position."));
            return;
        }
        Vector3d playerPos = transform.getPosition();
        Vector3d spawnPos = new Vector3d(playerPos.getX() + 2.0, playerPos.getY(), playerPos.getZ() + 2.0);

        long skinSeed = ThreadLocalRandom.current().nextLong();
        int villagerIndex = village.getVillagerCount();

        // Collect taken names to avoid duplicates
        Set<String> takenNames = new HashSet<>();
        for (VillagerSummary s : village.getVillagers()) {
            if (s.getFirstName() != null && s.getLastName() != null) {
                takenNames.add(s.getFirstName() + " " + s.getLastName());
            }
        }

        dev.hearthbound.npc.appearance.BodyArchetype body = VillagerAppearance.predictBody(skinSeed);
        String[] nameParts = VillagerNames.rollHumanName(skinSeed, body, takenNames);
        String firstName = (nameParts != null && nameParts.length > 0) ? nameParts[0] : "Unknown";
        String lastName  = (nameParts != null && nameParts.length > 1) ? nameParts[1] : "";

        Pair<Ref<EntityStore>, INonPlayerCharacter> result =
                NpcManager.spawnNpc(store, spawnPos, new Vector3f(0, 0, 0), VILLAGER_ROLE);
        if (result == null) {
            ctx.sendMessage(Message.raw("Failed to spawn villager NPC."));
            return;
        }

        Ref<EntityStore> npcRef = result.first();
        VillagerAppearance.apply(npcRef, store, skinSeed, villagerIndex);

        // Write VillagerData so the summary can be built correctly
        VillagerData villagerData = new VillagerData();
        villagerData.setSkinSeed(skinSeed);
        villagerData.setFirstName(firstName);
        villagerData.setLastName(lastName);
        store.putComponent(npcRef, VillagerData.getComponentType(), villagerData);

        UUID npcUuid = NpcManager.extractUuid(store, npcRef);

        VillagerSummary summary = new VillagerSummary(villagerData);
        summary.setVillagerUuid(npcUuid);
        village.addVillager(summary);
        VillageManager.get().save(store, playerRef, village);

        if (npcUuid != null) {
            long chunkIndex = NpcManager.chunkIndexFor(spawnPos);
            NpcRegistry.get().register(new NpcRegistry.NpcRecord(
                    npcUuid, VILLAGER_ROLE, NpcRegistry.InteractionType.VILLAGER, skinSeed, chunkIndex));
            HearthboundDataStore.get().save();
        }

        // Set leash point and re-apply interaction after role is settled
        double leashX = village.getFoundingStoneX() + 0.5;
        double leashY = village.getFoundingStoneY() + 0.1;
        double leashZ = village.getFoundingStoneZ() + 0.5;

        world.execute(() -> {
            if (npcUuid == null || !npcRef.isValid()) return;
            Store<EntityStore> liveStore = world.getEntityStore().getStore();

            var npcEntity = liveStore.getComponent(npcRef, NPCEntity.getComponentType());
            if (npcEntity != null) {
                npcEntity.setLeashPoint(new Vector3d(leashX, leashY, leashZ));
            }

            NpcRegistry.NpcRecord liveRecord = NpcRegistry.get().getRecord(npcUuid);
            if (liveRecord != null) {
                NpcRestorer.restoreAfterRoleChange(npcRef, world, liveRecord);
            }
        });

        ctx.sendMessage(Message.raw(
                "Spawned villager: " + firstName + " " + lastName
                        + " (total: " + village.getVillagerCount() + ")"));
    }
}
