package dev.hearthbound.npc;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * BSON-persistent marker on the elf sage NPC entity.
 * Survives chunk unload/reload, letting us find the elf by scanning entities
 * instead of relying on the UUID registry (which requires the chunk to be loaded first).
 */
public class ElfNpcComponent implements Component<EntityStore> {

    public static final BuilderCodec<ElfNpcComponent> CODEC = BuilderCodec
            .builder(ElfNpcComponent.class, ElfNpcComponent::new)
            .append(new KeyedCodec<>("ElfOwner", Codec.STRING),
                    ElfNpcComponent::setOwnerUuid,
                    ElfNpcComponent::getOwnerUuid)
            .add()
            .build();

    private static ComponentType<EntityStore, ElfNpcComponent> componentType;

    /** Owner player UUID as string — identifies which player this elf belongs to. */
    private String ownerUuid = "";

    public ElfNpcComponent() {}

    public ElfNpcComponent(String ownerUuid) {
        this.ownerUuid = ownerUuid != null ? ownerUuid : "";
    }

    public static void register(ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(ElfNpcComponent.class, "HB_ELF", CODEC);
    }

    public static ComponentType<EntityStore, ElfNpcComponent> getComponentType() {
        return componentType;
    }

    public String getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(String ownerUuid) { this.ownerUuid = ownerUuid != null ? ownerUuid : ""; }

    @Override
    public Component<EntityStore> clone() {
        return new ElfNpcComponent(ownerUuid);
    }
}
