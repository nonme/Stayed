package dev.hearthbound.util;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import java.util.Map;

/**
 * Shared renderer for the "icon + name + count" item rows used in every building UI.
 * Centralizes the inline DSL so all panels look identical and so display names are
 * resolved through the localization system in one place.
 */
public final class ResourceListRenderer {

    // Row colors picked once so all panels stay visually consistent.
    private static final String COLOR_COUNT_OK     = "#78c880";
    private static final String COLOR_COUNT_MISS   = "#c87878";
    private static final String COLOR_NAME_OK      = "#8ab8a0";
    private static final String COLOR_NAME_MISS    = "#9ab0bc";
    private static final String COLOR_NAME_NEUTRAL = "#9ab0bc";
    private static final String COLOR_QTY_NEUTRAL  = "#78c880";

    private ResourceListRenderer() {}

    /**
     * Renders a "required vs have" list (deposit progress in construction panels).
     * Returns true if every required item is satisfied — caller uses this to enable
     * the Start Construction button.
     */
    public static boolean renderRequired(UICommandBuilder b, String containerId,
                                          Map<String, Integer> required,
                                          Map<String, Integer> have,
                                          String language) {
        b.clear(containerId);
        boolean allSatisfied = true;
        int rowIndex = 0;
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            String itemId = entry.getKey();
            int need = entry.getValue();
            int got = have.getOrDefault(itemId, 0);
            boolean satisfied = got >= need;
            if (!satisfied) allSatisfied = false;

            String countColor = satisfied ? COLOR_COUNT_OK : COLOR_COUNT_MISS;
            String nameColor  = satisfied ? COLOR_NAME_OK  : COLOR_NAME_MISS;
            String displayName = ItemDisplayName.resolve(itemId, language);

            appendRow(b, containerId, rowIndex, itemId, displayName, nameColor,
                    got + " / " + need, countColor);
            rowIndex++;
        }
        return allSatisfied;
    }

    /**
     * Renders a flat "icon + name + qty" inventory list (warehouse storage view, etc.).
     * No satisfied/missing logic — every row uses neutral colors.
     */
    public static void renderInventory(UICommandBuilder b, String containerId,
                                        Map<String, Integer> items,
                                        String language) {
        b.clear(containerId);
        int rowIndex = 0;
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String itemId = entry.getKey();
            int qty = entry.getValue();
            String displayName = ItemDisplayName.resolve(itemId, language);
            appendRow(b, containerId, rowIndex, itemId, displayName, COLOR_NAME_NEUTRAL,
                    Integer.toString(qty), COLOR_QTY_NEUTRAL);
            rowIndex++;
        }
    }

    private static void appendRow(UICommandBuilder b, String containerId, int rowIndex,
                                   String itemId, String displayName, String nameColor,
                                   String countText, String countColor) {
        b.appendInline(containerId,
                "Group { LayoutMode: Left; Anchor: (Height: 32, Bottom: 2); Padding: (Horizontal: 4); " +
                "  ItemIcon { Anchor: (Width: 24, Height: 24); } " +
                "  Label { Anchor: (Left: 8); FlexWeight: 1; Style: (HorizontalAlignment: Start, VerticalAlignment: Center, TextColor: " + nameColor + ", FontSize: 11); Text: \"" + displayName + "\"; } " +
                "  Label { Anchor: (Width: 60); Style: (HorizontalAlignment: End, VerticalAlignment: Center, TextColor: " + countColor + ", FontSize: 11, RenderBold: true); Text: \"" + countText + "\"; } " +
                "}");
        b.set(containerId + "[" + rowIndex + "][0].ItemId", itemId);
    }
}
