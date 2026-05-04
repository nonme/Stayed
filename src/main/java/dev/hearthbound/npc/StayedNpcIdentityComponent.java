package dev.hearthbound.npc;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * BSON-persistent identity marker on every Stayed-managed NPC entity.
 *
 * The `npcId` is a stable plugin-level identity assigned when the NPC is first
 * spawned. It survives chunk save/load and is used to match an engine entity
 * back to its registry record without depending on the engine UUID (which
 * can change across respawns).
 */
public class StayedNpcIdentityComponent implements Component<EntityStore> {

    public static final BuilderCodec<StayedNpcIdentityComponent> CODEC = BuilderCodec
            .builder(StayedNpcIdentityComponent.class, StayedNpcIdentityComponent::new)
            .append(new KeyedCodec<>("NpcId", Codec.STRING),
                    StayedNpcIdentityComponent::setNpcId,
                    StayedNpcIdentityComponent::getNpcId)
            .add()
            .build();

    private static ComponentType<EntityStore, StayedNpcIdentityComponent> componentType;

    private String npcId = "";

    public StayedNpcIdentityComponent() {}

    public StayedNpcIdentityComponent(String npcId) {
        this.npcId = npcId != null ? npcId : "";
    }

    public static void register(ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(
                StayedNpcIdentityComponent.class, "HB_NPCID", CODEC);
    }

    public static ComponentType<EntityStore, StayedNpcIdentityComponent> getComponentType() {
        return componentType;
    }

    public String getNpcId() { return npcId; }
    public void setNpcId(String npcId) { this.npcId = npcId != null ? npcId : ""; }

    @Override
    public Component<EntityStore> clone() {
        return new StayedNpcIdentityComponent(npcId);
    }
}
