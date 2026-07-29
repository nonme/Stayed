package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import it.unimi.dsi.fastutil.Pair;

import java.util.UUID;
public final class StayedNpcSpawner {
    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("npc.spawner");
    private StayedNpcSpawner() {}

    public static Pair<Ref<EntityStore>, INonPlayerCharacter> spawnPersistent(
            Store<EntityStore> store,
            Vector3d position,
            Vector3f rotation,
            NpcRegistry.NpcRecord record
    ) {
        if (record == null) throw new IllegalArgumentException("record is required");
        World world = store != null && store.getExternalData() != null
                ? store.getExternalData().getWorld()
                : null;
        if (!record.hasWorld()) {
            record.setWorld(NpcRegistry.worldUuidOf(world), NpcRegistry.worldNameOf(world));
        }
        record.refreshGeneratedRoleName();
        String generatedRole = StayedRoleGenerator.get().generateRoleIfChanged(record);
        boolean generatedRoleIndexed = NPCPlugin.get().getIndex(generatedRole) >= 0;
        String spawnRole = StayedSpawnRolePolicy.selectSpawnRole(
                generatedRole, record.baseRoleName(), generatedRoleIndexed, true);
        if (!generatedRoleIndexed) {
            LOG.warn("[STAYED-SPAWN] generated role not indexed yet; spawning identity-tagged base role"
                    + " npcId=" + record.npcId
                    + " generatedRole=" + generatedRole
                    + " baseRole=" + record.baseRoleName());
        }
        int roleIndex = NPCPlugin.get().getIndex(spawnRole);
        Pair<Ref<EntityStore>, com.hypixel.hytale.server.npc.entities.NPCEntity> spawned =
                NPCPlugin.get().spawnEntity(store, roleIndex, position, rotation, null,
                        (npc, holder, s) -> holder.addComponent(
                                StayedNpcIdentityComponent.getComponentType(),
                                new StayedNpcIdentityComponent(record.npcId)),
                        null);
        Pair<Ref<EntityStore>, INonPlayerCharacter> result = spawned == null
                ? null
                : Pair.of(spawned.first(), spawned.second());
        if (result == null || result.first() == null) return result;

        Ref<EntityStore> ref = result.first();
        store.putComponent(ref, StayedNpcIdentityComponent.getComponentType(),
                new StayedNpcIdentityComponent(record.npcId));
        NpcManager.fixPersistentModelScale(store, ref);

        UUID uuid = NpcManager.extractUuid(store, ref);
        if (uuid != null) {
            record.entityUuid = uuid;
        }
        NpcRegistry.get().register(record);
        HearthboundDataStore.get().save();
        if (!generatedRoleIndexed) {
            StayedRoleChangeApplier.applyOrSchedule(ref, store, world, record,
                    false, "spawn-generated-role-pending");
        }
        LOG.info("[STAYED-SPAWN] npcId=" + record.npcId
                + " entityUuid=" + uuid
                + " baseRole=" + record.baseRoleName()
                + " generatedRole=" + generatedRole);
        return result;
    }
}
