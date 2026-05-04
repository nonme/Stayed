package dev.hearthbound.test.engine;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;
import java.util.Map;

/**
 * Carries everything a {@link TestStep} needs: world, the player running the
 * test, the entity store, a logger that writes to the per-test log file, and
 * a free-form scratch map for sharing values between steps in the same case
 * (e.g. snapshots, NPC UUIDs spawned earlier in the case).
 *
 * Snapshots live here rather than on the runner so each case has its own
 * isolated namespace.
 */
public final class TestContext {

    private final World world;
    private final Store<EntityStore> store;
    private final Ref<EntityStore> playerRef;
    private final PlayerRef player;
    private final TestLogger logger;
    private final String testName;
    private final Map<String, Object> scratch = new HashMap<>();

    public TestContext(World world, Store<EntityStore> store, Ref<EntityStore> playerRef,
                       PlayerRef player, TestLogger logger, String testName) {
        this.world = world;
        this.store = store;
        this.playerRef = playerRef;
        this.player = player;
        this.logger = logger;
        this.testName = testName;
    }

    public World getWorld() { return world; }
    public Store<EntityStore> getStore() { return store; }
    public Ref<EntityStore> getPlayerRef() { return playerRef; }
    public PlayerRef getPlayer() { return player; }
    public TestLogger getLogger() { return logger; }
    public String getTestName() { return testName; }

    public void put(String key, Object value) { scratch.put(key, value); }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) { return (T) scratch.get(key); }

    public boolean has(String key) { return scratch.containsKey(key); }
}
