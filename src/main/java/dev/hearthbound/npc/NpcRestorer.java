package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
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

    public static void restore(Ref<EntityStore> ref, Store<EntityStore> store,
                               World world, NpcRegistry.NpcRecord record) {
        // Step 1: Interaction — always putComponent unconditionally so client gets the packet.
        // Always putComponent unconditionally so the client gets a fresh packet after chunk load
        // "already exists", because the client needs a fresh packet after chunk load.
        applyInteraction(ref, store, record.interaction);

        // Step 2: Skin — delayed so client receives entity spawn packet first.

        if (record.skinSeed != 0L) {
            TickScheduler.getExecutor().schedule(() ->
                world.execute(() -> {
                    if (!ref.isValid()) return;
                    VillagerAppearance.apply(ref, store, record.skinSeed, 0);
                }),
                SKIN_DELAY_MS, TimeUnit.MILLISECONDS
            );
            // Retry — engine timing can vary, a second attempt ensures skin is applied
            TickScheduler.getExecutor().schedule(() ->
                world.execute(() -> {
                    if (!ref.isValid()) return;
                    VillagerAppearance.apply(ref, store, record.skinSeed, 0);
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
            case NONE -> {
                // No interaction needed — don't touch the components
            }
        }
    }
}
