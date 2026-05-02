package dev.hearthbound.util;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;

/**
 * Resolves user-facing item names from item IDs via the localization system.
 *
 * Why: many block IDs are technical (e.g. "Furniture_Village_Crate" → "Simple Wooden Crate"
 * in the asset's lang file). Splitting by underscore alone gives the wrong name.
 */
public final class ItemDisplayName {

    private ItemDisplayName() {}

    /**
     * Returns the localized display name for an item ID, falling back to a humanized version
     * of the ID if no translation is available.
     *
     * @param itemId   asset item ID, e.g. "Furniture_Village_Crate"
     * @param language player language code from PlayerRef.getLanguage(), or null for default
     */
    public static String resolve(String itemId, String language) {
        if (itemId == null || itemId.isEmpty()) return "";
        try {
            Item item = Item.getAssetMap().getAsset(itemId);
            if (item != null) {
                String key = item.getTranslationKey();
                if (key != null) {
                    String translated = I18nModule.get().getMessage(language, key);
                    if (translated != null && !translated.isEmpty() && !translated.startsWith("server.")) {
                        return translated;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall through to humanized fallback.
        }
        return humanize(itemId);
    }

    /** Humanizes an item ID by replacing underscores with spaces. */
    private static String humanize(String itemId) {
        return itemId.replace("_", " ");
    }
}
