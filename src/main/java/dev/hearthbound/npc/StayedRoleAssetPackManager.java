package dev.hearthbound.npc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.HytaleServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
public final class StayedRoleAssetPackManager {
    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("npc.role.assetpack");
    private static final String MOD_ID = "hearthbound:StayedGeneratedRoles";
    private static final Path ROOT = Paths.get("mods", "StayedGeneratedRoles");
    private static final Path ROLES = ROOT.resolve(Paths.get("Server", "NPC", "Roles"));
    private static final Path MANIFEST = ROOT.resolve("manifest.json");

    private StayedRoleAssetPackManager() {}

    public static Path rolesPath() {
        return ROLES;
    }

    public static void setup() {
        try {
            Files.createDirectories(ROLES);
            boolean createdManifest = ensureManifest();
            ensureEnabledWhenNeeded(Paths.get("config.json"));
            if (createdManifest) {
                LOG.warn("Created StayedGeneratedRoles asset pack; restarting server so Hytale indexes generated roles.");
                HytaleServer.get().shutdownServer();
            }
        } catch (IOException e) {
            LOG.warn("StayedRoleAssetPackManager setup failed: " + e.getMessage());
        }
    }

    private static boolean ensureManifest() throws IOException {
        if (Files.exists(MANIFEST)) return false;
        String content = "{\n" +
                "  \"Group\": \"hearthbound\",\n" +
                "  \"Name\": \"StayedGeneratedRoles\",\n" +
                "  \"Version\": \"1.0.0\",\n" +
                "  \"ServerVersion\": \"2026.03.26-89796e57b\",\n" +
                "  \"Description\": \"Generated identity-bearing NPC roles for Stayed.\",\n" +
                "  \"Authors\": [{ \"Name\": \"Stayed\" }],\n" +
                "  \"Dependencies\": {},\n" +
                "  \"OptionalDependencies\": {},\n" +
                "  \"LoadBefore\": {},\n" +
                "  \"DisabledByDefault\": false,\n" +
                "  \"IncludesAssetPack\": false,\n" +
                "  \"SubPlugins\": []\n" +
                "}\n";
        Files.writeString(MANIFEST, content, StandardCharsets.UTF_8);
        return true;
    }

    private static void ensureEnabledWhenNeeded(Path configPath) throws IOException {
        if (!Files.exists(configPath)) return;
        JsonObject config = JsonParser.parseString(Files.readString(configPath, StandardCharsets.UTF_8)).getAsJsonObject();
        boolean defaultModsEnabled = config.has("DefaultModsEnabled")
                && config.get("DefaultModsEnabled").isJsonPrimitive()
                && config.get("DefaultModsEnabled").getAsBoolean();
        if (defaultModsEnabled) return;

        JsonObject mods = config.has("Mods") && config.get("Mods").isJsonObject()
                ? config.getAsJsonObject("Mods")
                : new JsonObject();
        if (mods.has(MOD_ID)) return;

        JsonObject entry = new JsonObject();
        entry.addProperty("Enabled", true);
        mods.add(MOD_ID, entry);
        config.add("Mods", mods);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(configPath, gson.toJson(config), StandardCharsets.UTF_8);
    }
}
