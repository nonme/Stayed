package dev.hearthbound.building;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDrop;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDropList;
import com.hypixel.hytale.server.core.asset.type.item.config.container.ItemDropContainer;

import java.io.BufferedWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
/**
 * Built at server start from two LoadedAssetsEvents (Item + ItemDropList).
 * Mirrors JET's logic exactly:
 *   CRAFTABLE   — has a Recipe in item JSON (inherited from parent if needed)
 *   HAS_SOURCE  — appears in at least one ItemDropList (dropped by mob/block/chest)
 *   NONE        — no recipe and no drop source ("Uncraftable — no source found" in JET)
 *
 * Use isFree(itemId) to decide whether a building block should cost resources.
 * Index is saved to mods/HearthboundData/craftability.json on demand via /hb craftability dump.
 */
public class CraftabilityIndex {

    public enum ObtainSource { CRAFTABLE, HAS_SOURCE, NONE }

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("build.craftability");
    private static final Path DUMP_FILE = Paths.get("mods", "HearthboundData", "craftability.json");

    // Reflected once — Item.recipeToGenerate has no public getter.
    private static final Field recipeField;

    static {
        Field f = null;
        try {
            f = Item.class.getDeclaredField("recipeToGenerate");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            LOG.warn("CraftabilityIndex: could not reflect Item.recipeToGenerate — CRAFTABLE classification disabled");
        }
        recipeField = f;
    }

    private static volatile Map<String, ObtainSource> index = Collections.emptyMap();
    private static volatile Set<String> itemsWithDropSource = Collections.emptySet();

    // --- Event handlers (register both in HearthboundPlugin.setup()) ---

    public static void onItemsLoaded(LoadedAssetsEvent<String, Item, DefaultAssetMap<String, Item>> event) {
        Map<String, Item> items = event.getAssetMap().getAssetMap();
        if (items.isEmpty()) {
            LOG.warn("CraftabilityIndex: Item LoadedAssetsEvent had no items");
            return;
        }
        rebuildIndex(items);
    }

    public static void onDropListsLoaded(LoadedAssetsEvent<String, ItemDropList, DefaultAssetMap<String, ItemDropList>> event) {
        Map<String, ItemDropList> dropLists = event.getAssetMap().getAssetMap();
        Set<String> withSource = new HashSet<>();
        List<ItemDrop> scratch = new ArrayList<>();

        for (ItemDropList dropList : dropLists.values()) {
            ItemDropContainer container = dropList.getContainer();
            if (container == null) continue;
            scratch.clear();
            container.getAllDrops(scratch);
            for (ItemDrop drop : scratch) {
                if (drop.getItemId() != null) withSource.add(drop.getItemId());
            }
        }

        itemsWithDropSource = Collections.unmodifiableSet(withSource);
        LOG.info("indexed " + withSource.size() + " items with drop sources");

        // Re-classify if items were already loaded
        if (!index.isEmpty()) {
            Map<String, Item> items = Item.getAssetMap().getAssetMap();
            if (!items.isEmpty()) rebuildIndex(items);
        }
    }

    private static void rebuildIndex(Map<String, Item> items) {
        Map<String, ObtainSource> result = new HashMap<>(items.size() * 2);
        int craftable = 0, hasSource = 0, none = 0;

        for (Map.Entry<String, Item> entry : items.entrySet()) {
            ObtainSource source = classify(entry.getKey(), entry.getValue());
            result.put(entry.getKey(), source);
            switch (source) {
                case CRAFTABLE -> craftable++;
                case HAS_SOURCE -> hasSource++;
                case NONE -> none++;
            }
        }

        index = Collections.unmodifiableMap(result);
        LOG.with("craftable", craftable)
           .with("hasSource", hasSource)
           .with("none", none)
           .info("CraftabilityIndex built");
    }

    private static ObtainSource classify(String itemId, Item item) {
        if (hasRecipe(item)) return ObtainSource.CRAFTABLE;
        if (itemsWithDropSource.contains(itemId)) return ObtainSource.HAS_SOURCE;
        return ObtainSource.NONE;
    }

    @SuppressWarnings("ConstantValue")
    private static boolean hasRecipe(Item item) {
        if (recipeField == null) return false;
        try {
            return recipeField.get(item) != null;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    // --- Public API ---

    public static ObtainSource getSource(String itemId) {
        return index.getOrDefault(itemId, ObtainSource.NONE);
    }

    /** True if this block should be free in construction (no player-accessible source). */
    public static boolean isFree(String itemId) {
        return getSource(itemId) == ObtainSource.NONE;
    }

    public static boolean isEmpty() {
        return index.isEmpty();
    }

    public static int size() {
        return index.size();
    }

    /**
     * Returns a human-readable display name for the given item ID.
     * Tries the game's i18n translation first; falls back to formatting the ID itself
     * (strip category prefix, replace underscores with spaces).
     */
    public static String getDisplayName(String itemId) {
        try {
            Item item = Item.getAssetMap().getAsset(itemId);
            if (item != null) {
                String key = item.getTranslationKey();
                if (key != null) {
                    String translated = com.hypixel.hytale.server.core.modules.i18n.I18nModule.get().getMessage("en-US", key);
                    if (translated != null && !translated.isEmpty()) {
                        return translated;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        // Fallback: strip the first category segment and prettify.
        String name = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
        int underscore = name.indexOf('_');
        if (underscore > 0) name = name.substring(underscore + 1);
        return name.replace("_", " ");
    }

    /** Writes a sorted JSON file: { "ItemId": "CRAFTABLE"|"HAS_SOURCE"|"NONE", ... } */
    public static void saveToDisk() {
        try {
            Files.createDirectories(DUMP_FILE.getParent());
            Path tmp = DUMP_FILE.getParent().resolve("craftability.json.tmp");

            Map<String, ObtainSource> sorted = new TreeMap<>(index);

            try (BufferedWriter w = Files.newBufferedWriter(tmp)) {
                w.write("{\n");
                int i = 0;
                int total = sorted.size();
                for (Map.Entry<String, ObtainSource> e : sorted.entrySet()) {
                    w.write("  \"" + e.getKey() + "\": \"" + e.getValue() + "\"");
                    if (++i < total) w.write(",");
                    w.write("\n");
                }
                w.write("}\n");
            }

            Files.move(tmp, DUMP_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            LOG.info("CraftabilityIndex saved to " + DUMP_FILE);
        } catch (Exception e) {
            LOG.warn("CraftabilityIndex: failed to save dump", e);
        }
    }
}
