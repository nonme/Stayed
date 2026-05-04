package dev.hearthbound.npc;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * BSON-persistent marker on every NPC spawned by the integration test framework.
 *
 * Cleanup walks loaded chunks and removes only entities carrying this marker —
 * never real player NPCs. Persisting in BSON means a test that is interrupted
 * by a server restart can still be cleaned up next session via
 * {@code /hb test cleanup}.
 *
 * Holds the test name that spawned the NPC for diagnostic logs.
 */
public class StayedIntegrationTestNpcMarkerComponent implements Component<EntityStore> {

    public static final BuilderCodec<StayedIntegrationTestNpcMarkerComponent> CODEC = BuilderCodec
            .builder(StayedIntegrationTestNpcMarkerComponent.class,
                    StayedIntegrationTestNpcMarkerComponent::new)
            .append(new KeyedCodec<>("TestName", Codec.STRING),
                    StayedIntegrationTestNpcMarkerComponent::setTestName,
                    StayedIntegrationTestNpcMarkerComponent::getTestName)
            .add()
            .build();

    private static ComponentType<EntityStore, StayedIntegrationTestNpcMarkerComponent> componentType;

    private String testName = "";

    public StayedIntegrationTestNpcMarkerComponent() {}

    public StayedIntegrationTestNpcMarkerComponent(String testName) {
        this.testName = testName != null ? testName : "";
    }

    public static void register(ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(
                StayedIntegrationTestNpcMarkerComponent.class, "HB_TEST_NPC", CODEC);
    }

    public static ComponentType<EntityStore, StayedIntegrationTestNpcMarkerComponent> getComponentType() {
        return componentType;
    }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName != null ? testName : ""; }

    @Override
    public Component<EntityStore> clone() {
        return new StayedIntegrationTestNpcMarkerComponent(testName);
    }
}
