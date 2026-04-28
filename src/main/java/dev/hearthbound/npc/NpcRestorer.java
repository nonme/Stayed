package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import dev.hearthbound.util.TickScheduler;

/**
 * Applies skin and interaction to an NPC entity that has just appeared in the world
 * after chunk load. Called by NpcRegistry once polling confirms the entity exists.
 *
 * Applies skin and interaction to an NPC entity after chunk load.
 * Called by NpcRegistry once polling confirms the entity exists.
 */
public final class NpcRestorer {

    private static final Logger LOGGER = Logger.getLogger(NpcRestorer.class.getName());

    // Delay between entity appearing in registry and skin apply.
    // Skin apply is delayed so the client receives the entity spawn packet first, then retried.
    private static final long SKIN_DELAY_MS  = 500;
    private static final long SKIN_RETRY_MS  = 2000;

    private NpcRestorer() {}

    /**
     * Like restore(), but defers applyInteraction() by SKIN_DELAY_MS + retry.
     * Use after RoleChangeSystem.requestRoleChange() — the role is applied asynchronously
     * and resets Interactions; immediate applyInteraction() would be overwritten.
     */
    public static void restoreAfterRoleChange(Ref<EntityStore> ref, World world,
                                              NpcRegistry.NpcRecord record) {
        TickScheduler.getExecutor().schedule(() ->
            world.execute(() -> {
                if (!ref.isValid()) return;
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                applyInteraction(ref, liveStore, record.interaction);
            }),
            SKIN_DELAY_MS, TimeUnit.MILLISECONDS
        );
        // Retry — RoleChangeSystem timing can vary
        TickScheduler.getExecutor().schedule(() ->
            world.execute(() -> {
                if (!ref.isValid()) return;
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                applyInteraction(ref, liveStore, record.interaction);
            }),
            SKIN_DELAY_MS + SKIN_RETRY_MS, TimeUnit.MILLISECONDS
        );

        // Skin — same schedule as regular restore()
        scheduleSkins(ref, world, record);
    }

    public static void restore(Ref<EntityStore> ref, Store<EntityStore> store,
                               World world, NpcRegistry.NpcRecord record) {
        // Step 1: Interaction — always putComponent unconditionally so client gets the packet.
        // Always putComponent unconditionally so the client gets a fresh packet after chunk load
        // "already exists", because the client needs a fresh packet after chunk load.
        applyInteraction(ref, store, record.interaction);

        // Step 2: Skin — delayed so client receives entity spawn packet first.
        scheduleSkins(ref, world, record);
    }

    private static void scheduleSkins(Ref<EntityStore> ref, World world, NpcRegistry.NpcRecord record) {

        if (record.skinSeed != 0L) {
            TickScheduler.getExecutor().schedule(() ->
                world.execute(() -> {
                    if (!ref.isValid()) return;
                    Store<EntityStore> liveStore = world.getEntityStore().getStore();
                    VillagerAppearance.apply(ref, liveStore, record.skinSeed, 0);
                    equipProfessionItem(ref, liveStore, record.roleName);
                }),
                SKIN_DELAY_MS, TimeUnit.MILLISECONDS
            );
            TickScheduler.getExecutor().schedule(() ->
                world.execute(() -> {
                    if (!ref.isValid()) return;
                    Store<EntityStore> liveStore = world.getEntityStore().getStore();
                    VillagerAppearance.apply(ref, liveStore, record.skinSeed, 0);
                    equipProfessionItem(ref, liveStore, record.roleName);
                }),
                SKIN_DELAY_MS + SKIN_RETRY_MS, TimeUnit.MILLISECONDS
            );
        } else if (record.interaction == NpcRegistry.InteractionType.ELF) {
            // Elf uses its own appearance logic (PlayerSkin, not seed-based)
            TickScheduler.getExecutor().schedule(() ->
                world.execute(() -> {
                    if (!ref.isValid()) return;
                    ElfSage.applySageAppearance(ref, world.getEntityStore().getStore());
                }),
                SKIN_DELAY_MS, TimeUnit.MILLISECONDS
            );
            TickScheduler.getExecutor().schedule(() ->
                world.execute(() -> {
                    if (!ref.isValid()) return;
                    ElfSage.applySageAppearance(ref, world.getEntityStore().getStore());
                }),
                SKIN_DELAY_MS + SKIN_RETRY_MS, TimeUnit.MILLISECONDS
            );
        }
    }

    public static void equipProfessionItem(Ref<EntityStore> ref, Store<EntityStore> store, String roleName) {
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null) {
            LOGGER.warning("equipProfessionItem: NPCEntity null for " + roleName);
            return;
        }
        // Use live role name — the NPC may have had its role changed to a profession role
        // after the registry record was created (e.g. Villager_Human → Villager_Human_Farmer).
        String liveRole = npc.getRoleName();
        String effectiveRole = liveRole != null ? liveRole : roleName;
        String itemId = switch (effectiveRole) {
            case "Villager_Human_Farmer"    -> "Tool_Hoe_Crude";
            case "Villager_Human_Lumberjack" -> "Weapon_Axe_Crude";
            case "Villager_Human_Miner"      -> "Tool_Pickaxe_Crude";
            default -> null;
        };
        // Always clear slot 0 first, then equip profession item if any
        npc.getInventory().getHotbar().setItemStackForSlot((short) 0, null);
        if (itemId != null) {
            npc.getInventory().getHotbar().setItemStackForSlot((short) 0, new ItemStack(itemId));
        }
    }

    private static void applyInteraction(Ref<EntityStore> ref, Store<EntityStore> store,
                                         NpcRegistry.InteractionType type) {
        switch (type) {
            case ELF -> {
                store.putComponent(ref, Interactable.getComponentType(), Interactable.INSTANCE);
                Interactions interactions = new Interactions();
                interactions.setInteractionId(InteractionType.Use, "Hearthbound");
                interactions.setInteractionHint("server.interactionHints.talk");
                store.putComponent(ref, Interactions.getComponentType(), interactions);
            }
            case RESCUE -> {
                store.putComponent(ref, Interactable.getComponentType(), Interactable.INSTANCE);
                Interactions interactions = new Interactions();
                interactions.setInteractionId(InteractionType.Use, "HearthboundRescue");
                interactions.setInteractionHint("server.interactionHints.talk");
                store.putComponent(ref, Interactions.getComponentType(), interactions);
            }
            case VILLAGER -> {
                store.putComponent(ref, Interactable.getComponentType(), Interactable.INSTANCE);
                Interactions interactions = new Interactions();
                interactions.setInteractionId(InteractionType.Use, "HearthboundVillager");
                interactions.setInteractionHint("server.interactionHints.talk");
                store.putComponent(ref, Interactions.getComponentType(), interactions);
            }
            case FOLLOWER, NONE -> {
                // No interaction — follower has agreed to join, no dialog needed
            }
        }
    }
}
