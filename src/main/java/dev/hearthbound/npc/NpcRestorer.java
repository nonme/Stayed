package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import dev.hearthbound.util.TickScheduler;

/**
 * Re-applies skin and F-key interaction to an NPC after chunk-load or after a
 * role change. Skin application is delayed and retried because:
 *
 *   - On chunk load, the client must receive the entity-spawn packet before any
 *     skin packet, otherwise the skin is silently dropped.
 *   - After {@link com.hypixel.hytale.server.npc.systems.RoleChangeSystem#requestRoleChange},
 *     the engine clears the {@link Interactions} component asynchronously, so a
 *     synchronous re-apply gets overwritten.
 */
public final class NpcRestorer {

    private static final Logger LOGGER = Logger.getLogger(NpcRestorer.class.getName());

    private static final long APPLY_DELAY_MS = 500;
    private static final long APPLY_RETRY_MS = 2_000;
    private static final long APPLY_LATE_RETRY_MS = 5_000;

    private NpcRestorer() {}

    /**
     * Apply both interaction (immediately) and skin (delayed) — typical
     * chunk-load restoration entry point.
     */
    public static void restore(Ref<EntityStore> ref, Store<EntityStore> store,
                               World world, NpcRegistry.NpcRecord record) {
        if (record == null) return;
        applyInteraction(ref, store, record.interaction);
        scheduleInteractionRetries(ref, world, record);
        scheduleSkin(ref, world, record);
    }

    /**
     * Apply interaction (delayed + retried) and skin — used after a role change
     * where the engine resets {@link Interactions} asynchronously.
     */
    public static void restoreAfterRoleChange(Ref<EntityStore> ref, World world,
                                              NpcRegistry.NpcRecord record) {
        if (record == null) return;
        scheduleInteractionRetries(ref, world, record);
        scheduleSkin(ref, world, record);
    }

    private static void scheduleInteractionRetries(Ref<EntityStore> ref, World world,
                                                   NpcRegistry.NpcRecord record) {
        scheduleInteraction(ref, world, record, APPLY_DELAY_MS);
        scheduleInteraction(ref, world, record, APPLY_DELAY_MS + APPLY_RETRY_MS);
        scheduleInteraction(ref, world, record, APPLY_DELAY_MS + APPLY_LATE_RETRY_MS);
    }

    private static void scheduleInteraction(Ref<EntityStore> ref, World world,
                                            NpcRegistry.NpcRecord record, long delayMs) {
        TickScheduler.getExecutor().schedule(() ->
            world.execute(() -> {
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                // RoleChangeSystem can replace the underlying entity with a fresh
                // Ref index; the captured ref goes invalid even though the NPC is
                // still loaded under the same npcId. Mirror HyCitizens'
                // findExistingCitizenNpcRef and re-resolve every retry.
                Ref<EntityStore> liveRef = (ref != null && ref.isValid())
                        ? ref : NpcLiveEntityResolver.findLiveNpcByRecord(liveStore, record);
                if (liveRef == null || !liveRef.isValid()) {
                    LOGGER.info("[ROLEBIND-APPLY] npcId=" + record.npcId
                            + " delayMs=" + delayMs + " SKIP: no live ref");
                    return;
                }
                applyInteraction(liveRef, liveStore, record.interaction);
                NPCEntity npc = liveStore.getComponent(liveRef, NPCEntity.getComponentType());
                String live = (npc != null && npc.getRole() != null)
                        ? npc.getRole().getRoleName() : "?";
                LOGGER.info("[ROLEBIND-APPLY] npcId=" + record.npcId
                        + " delayMs=" + delayMs
                        + " liveRole=" + live
                        + " applied=" + record.interaction);
            }),
            delayMs, TimeUnit.MILLISECONDS);
    }

    private static void scheduleSkin(Ref<EntityStore> ref, World world, NpcRegistry.NpcRecord record) {
        Runnable apply = () -> world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> liveRef = (ref != null && ref.isValid())
                    ? ref : NpcLiveEntityResolver.findLiveNpcByRecord(store, record);
            if (liveRef == null || !liveRef.isValid()) return;
            if (record.skinSeed != 0L) {
                VillagerAppearance.apply(liveRef, store, record.skinSeed, 0);
                equipProfessionItem(liveRef, store, record.roleName);
            } else if (record.interaction == NpcRegistry.InteractionType.ELF) {
                ElfSage.applySageAppearance(liveRef, store);
            }
        });
        TickScheduler.getExecutor().schedule(apply, APPLY_DELAY_MS, TimeUnit.MILLISECONDS);
        TickScheduler.getExecutor().schedule(apply, APPLY_DELAY_MS + APPLY_RETRY_MS, TimeUnit.MILLISECONDS);
        TickScheduler.getExecutor().schedule(apply, APPLY_DELAY_MS + APPLY_LATE_RETRY_MS, TimeUnit.MILLISECONDS);
    }

    public static void equipProfessionItem(Ref<EntityStore> ref, Store<EntityStore> store, String roleName) {
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null) {
            LOGGER.warning("equipProfessionItem: NPCEntity null for " + roleName);
            return;
        }
        // Use live role name — may differ from record.roleName if a profession swap happened.
        String liveRole = npc.getRoleName();
        String effectiveRole = liveRole != null ? liveRole : roleName;
        String itemId = switch (effectiveRole) {
            case "Villager_Human_Farmer"     -> "Tool_Hoe_Crude";
            case "Villager_Human_Lumberjack" -> "Weapon_Axe_Crude";
            case "Villager_Human_Miner"      -> "Tool_Pickaxe_Crude";
            case "Villager_Human_Guard"      -> "Weapon_Sword_Crude";
            case "Villager_Human_Blacksmith" -> "Tool_Hammer_Crude";
            default -> null;
        };
        npc.getInventory().getHotbar().setItemStackForSlot((short) 0, null);
        if (itemId != null) {
            npc.getInventory().getHotbar().setItemStackForSlot((short) 0, new ItemStack(itemId));
        }
    }

    private static void applyInteraction(Ref<EntityStore> ref, Store<EntityStore> store,
                                         NpcRegistry.InteractionType type) {
        switch (type) {
            case ELF -> putInteraction(ref, store, "Stayed");
            case RESCUE -> putInteraction(ref, store, "StayedRescue");
            case VILLAGER -> putInteraction(ref, store, "StayedVillager");
            case FOLLOWER, NONE -> { /* no F-key interaction */ }
        }
    }

    private static void putInteraction(Ref<EntityStore> ref, Store<EntityStore> store,
                                        String interactionId) {
        store.putComponent(ref, Interactable.getComponentType(), Interactable.INSTANCE);
        Interactions interactions = new Interactions();
        interactions.setInteractionId(InteractionType.Use, interactionId);
        interactions.setInteractionHint("server.interactionHints.talk");
        store.putComponent(ref, Interactions.getComponentType(), interactions);
    }
}
