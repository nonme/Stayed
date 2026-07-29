package dev.hearthbound.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
/**
 * Dev teleport command:
 *   /hb tp random          — teleport to random coords (Y=130, dx,dz >= 1000 from current)
 *   /hb tp set <name>      — save current position under <name>
 *   /hb tp <name>          — teleport to saved <name>
 *   /hb tp list            — list saved positions
 *
 * Saved positions persist in mods/HearthboundData/tp_locations.json (per-world).
 */
public class TpCommand extends AbstractPlayerCommand {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("cmd.tp");
    private static final Path DATA_FILE = Paths.get("mods", "HearthboundData", "tp_locations.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final double MIN_RANDOM_DISTANCE = 1000.0;
    private static final double RANDOM_RANGE = 2000.0;
    private static final double RANDOM_Y = 130.0;

    private static Map<String, SavedLocation> cache = null;
    private static final Random RNG = new Random();

    private final RequiredArg<String> actionArg;
    private final OptionalArg<String> nameArg;

    public TpCommand() {
        super("tp", "Teleport: random | set <name> | <name> | list");
        actionArg = withRequiredArg("action", "random | set | list | <saved-name>", ArgTypes.STRING);
        nameArg = withOptionalArg("name", "Name (required for 'set')", ArgTypes.STRING);
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        String action = ctx.get(actionArg);
        String name = ctx.provided(nameArg) ? ctx.get(nameArg) : null;

        switch (action.toLowerCase()) {
            case "random" -> teleportRandom(ctx, store, playerRef, world);
            case "set" -> setLocation(ctx, store, playerRef, world, name);
            case "list" -> listLocations(ctx, world);
            default -> teleportToSaved(ctx, store, playerRef, world, action);
        }
    }

    private void teleportRandom(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            ctx.sendMessage(Message.raw("Could not get player position."));
            return;
        }
        Vector3d pos = transform.getPosition();

        // Each axis: sign random, magnitude in [MIN_RANDOM_DISTANCE, MIN_RANDOM_DISTANCE + RANDOM_RANGE]
        double dx = randomOffset();
        double dz = randomOffset();
        double tx = pos.getX() + dx;
        double tz = pos.getZ() + dz;

        Vector3d target = new Vector3d(tx, RANDOM_Y, tz);
        Vector3f rot = transform.getRotation();
        if (rot == null) rot = new Vector3f(0, 0, 0);
        final Vector3f finalRot = rot;

        world.execute(() -> {
            Teleport teleport = Teleport.createForPlayer(world, target, finalRot);
            store.addComponent(playerRef, Teleport.getComponentType(), teleport);
        });

        ctx.sendMessage(Message.raw(String.format("Teleporting to (%.0f, %.0f, %.0f) — Δ(%.0f, %.0f)",
                tx, RANDOM_Y, tz, dx, dz)));
    }

    private double randomOffset() {
        double magnitude = MIN_RANDOM_DISTANCE + RNG.nextDouble() * RANDOM_RANGE;
        return RNG.nextBoolean() ? magnitude : -magnitude;
    }

    private void setLocation(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, World world, String name) {
        if (name == null || name.isBlank()) {
            ctx.sendMessage(Message.raw("Usage: /hb tp set <name>"));
            return;
        }
        if (isReservedName(name)) {
            ctx.sendMessage(Message.raw("Name '" + name + "' is reserved (random/set/list)."));
            return;
        }
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            ctx.sendMessage(Message.raw("Could not get player position."));
            return;
        }
        Vector3d pos = transform.getPosition();
        Vector3f rot = transform.getRotation();

        SavedLocation loc = new SavedLocation();
        loc.world = world.getName();
        loc.x = pos.getX();
        loc.y = pos.getY();
        loc.z = pos.getZ();
        if (rot != null) {
            loc.yaw = rot.getX();
            loc.pitch = rot.getY();
            loc.roll = rot.getZ();
        }

        Map<String, SavedLocation> all = load();
        all.put(name, loc);
        save(all);

        ctx.sendMessage(Message.raw(String.format("Saved '%s' at (%.1f, %.1f, %.1f) in %s",
                name, loc.x, loc.y, loc.z, loc.world)));
    }

    private void teleportToSaved(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, World world, String name) {
        Map<String, SavedLocation> all = load();
        SavedLocation loc = all.get(name);
        if (loc == null) {
            ctx.sendMessage(Message.raw("No saved location '" + name + "'. Use /hb tp list."));
            return;
        }
        if (loc.world != null && !loc.world.equals(world.getName())) {
            ctx.sendMessage(Message.raw("Location '" + name + "' is in world '" + loc.world
                    + "' but you are in '" + world.getName() + "'. Use /hb warp first."));
            return;
        }

        Vector3d target = new Vector3d(loc.x, loc.y, loc.z);
        Vector3f rot = new Vector3f(loc.yaw, loc.pitch, loc.roll);

        world.execute(() -> {
            Teleport teleport = Teleport.createForPlayer(world, target, rot);
            store.addComponent(playerRef, Teleport.getComponentType(), teleport);
        });

        ctx.sendMessage(Message.raw(String.format("Teleporting to '%s' (%.1f, %.1f, %.1f)",
                name, loc.x, loc.y, loc.z)));
    }

    private void listLocations(CommandContext ctx, World world) {
        Map<String, SavedLocation> all = load();
        if (all.isEmpty()) {
            ctx.sendMessage(Message.raw("No saved locations."));
            return;
        }
        ctx.sendMessage(Message.raw("Saved locations (" + all.size() + "):"));
        for (Map.Entry<String, SavedLocation> e : all.entrySet()) {
            SavedLocation l = e.getValue();
            String marker = (l.world != null && !l.world.equals(world.getName())) ? " [other world]" : "";
            ctx.sendMessage(Message.raw(String.format("  %s — (%.0f, %.0f, %.0f) @ %s%s",
                    e.getKey(), l.x, l.y, l.z, l.world, marker)));
        }
    }

    private static boolean isReservedName(String name) {
        String n = name.toLowerCase();
        return n.equals("random") || n.equals("set") || n.equals("list");
    }

    private static synchronized Map<String, SavedLocation> load() {
        if (cache != null) return cache;
        if (!Files.exists(DATA_FILE)) {
            cache = new LinkedHashMap<>();
            return cache;
        }
        try (FileReader reader = new FileReader(DATA_FILE.toFile())) {
            Type type = new TypeToken<LinkedHashMap<String, SavedLocation>>() {}.getType();
            Map<String, SavedLocation> loaded = GSON.fromJson(reader, type);
            cache = (loaded != null) ? loaded : new LinkedHashMap<>();
        } catch (Exception e) {
            LOG.warn("TpCommand: failed to load locations: " + e.getMessage());
            cache = new LinkedHashMap<>();
        }
        return cache;
    }

    private static synchronized void save(Map<String, SavedLocation> data) {
        cache = data;
        try {
            Files.createDirectories(DATA_FILE.getParent());
            Path tmp = DATA_FILE.getParent().resolve("tp_locations.json.tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(data, writer);
            }
            Files.move(tmp, DATA_FILE,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            LOG.warn("TpCommand: failed to save locations: " + e.getMessage());
        }
    }

    private static final class SavedLocation {
        String world;
        double x, y, z;
        float yaw, pitch, roll;
    }
}
