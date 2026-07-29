package dev.hearthbound.ui;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerData;
import dev.hearthbound.village.VillagerSummary;

/**
 * Founder's Almanac — player's portable settlement journal.
 *
 * Two tabs:
 *   Overview  — village name, population, building count, complaints from settlers.
 *   Buildings — catalog of buildings grouped by functional category, with a
 *               "Get Anchor" button that places the anchor block item in the
 *               player's inventory so they can begin construction in the world.
 *
 * The Almanac itself is purely informational + a dispenser of anchor items.
 * Founding a village, gathering resources and triggering construction still
 * happen via the Founding Stone / Town Hall pipeline.
 */
public class AlmanacPage extends InteractiveCustomUIPage<DialogEventData> {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("ui.almanac");

    // Catalog entries — one per building type the player can build.
    // Implemented entries dispense a real anchor block; planned entries show
    // a disabled card with "Coming soon" so the catalog stays complete.
    private record CatalogEntry(String type, String category, boolean implemented) {}

    private static final List<CatalogEntry> CATALOG = List.of(
            new CatalogEntry(BuildingType.TOWN_HALL,        BuildingType.CATEGORY_CIVIC,      true),
            new CatalogEntry(BuildingType.FARM,             BuildingType.CATEGORY_PRODUCTION, true),
            new CatalogEntry(BuildingType.WOODCUTTERS_HUT,  BuildingType.CATEGORY_PRODUCTION, true),
            new CatalogEntry(BuildingType.MINE,             BuildingType.CATEGORY_PRODUCTION, true),
            new CatalogEntry(BuildingType.SAWMILL,          BuildingType.CATEGORY_CRAFTING,   true),
            new CatalogEntry(BuildingType.FORGE,            BuildingType.CATEGORY_CRAFTING,   true),
            new CatalogEntry(BuildingType.TAVERN,           BuildingType.CATEGORY_SERVICES,   true),
            new CatalogEntry(BuildingType.HOUSE_HUMAN,      BuildingType.CATEGORY_HOUSING,    true),
            new CatalogEntry(BuildingType.GUARD_HOUSE,      BuildingType.CATEGORY_DEFENSE,    true),
            new CatalogEntry(BuildingType.WAREHOUSE,        BuildingType.CATEGORY_STORAGE,    true)
    );

    private final Ref<EntityStore> playerRef;
    private String activeTab = "overview";
    private String activeCategory = BuildingType.CATEGORY_CIVIC;

    public AlmanacPage(PlayerRef player, Ref<EntityStore> playerRef) {
        super(player, CustomPageLifetime.CanDismissOrCloseThroughInteraction, DialogEventData.CODEC);
        this.playerRef = playerRef;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder events, Store<EntityStore> store) {
        builder.append("Almanac.ui");

        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village != null) {
            refreshSummariesFromLiveEntities(store, village);
        }
        populateOverview(builder, village);
        populateBuildings(builder, events);
        applyTabState(builder);
        applyCategoryState(builder);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of(DialogEventData.ACTION_KEY, "close"), false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabOverview",
                EventData.of(DialogEventData.ACTION_KEY, "tab_overview"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabOverviewInactive",
                EventData.of(DialogEventData.ACTION_KEY, "tab_overview"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabBuildings",
                EventData.of(DialogEventData.ACTION_KEY, "tab_buildings"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabBuildingsInactive",
                EventData.of(DialogEventData.ACTION_KEY, "tab_buildings"), false);

        bindSubTab(events, "Civic",      BuildingType.CATEGORY_CIVIC);
        bindSubTab(events, "Production", BuildingType.CATEGORY_PRODUCTION);
        bindSubTab(events, "Crafting",   BuildingType.CATEGORY_CRAFTING);
        bindSubTab(events, "Services",   BuildingType.CATEGORY_SERVICES);
        bindSubTab(events, "Housing",    BuildingType.CATEGORY_HOUSING);
        bindSubTab(events, "Defense",    BuildingType.CATEGORY_DEFENSE);
        bindSubTab(events, "Storage",    BuildingType.CATEGORY_STORAGE);
    }

    private void bindSubTab(UIEventBuilder events, String idSuffix, String category) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SubTab" + idSuffix,
                EventData.of(DialogEventData.ACTION_KEY, "cat")
                        .append(DialogEventData.VALUE_KEY, category), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SubTab" + idSuffix + "Inactive",
                EventData.of(DialogEventData.ACTION_KEY, "cat")
                        .append(DialogEventData.VALUE_KEY, category), false);
    }

    // ───────────── Overview ─────────────

    /**
     * Pulls fresh hasHouse/hunger off every loaded villager entity and copies the
     * values into the matching VillagerSummary. Without this, the Overview tab can
     * show "{Name} has no home" right after a house is assigned because the periodic
     * sync in VillageTickHandler only fires every five seconds.
     *
     * <p>Villagers in unloaded chunks keep whatever was last persisted to BSON —
     * that is the same trade-off the rest of the UI accepts.
     */
    private void refreshSummariesFromLiveEntities(Store<EntityStore> store, VillageData village) {
        Archetype<EntityStore> query = Archetype.of(NPCEntity.getComponentType());
        store.forEachChunk(query, (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                try {
                    Ref<EntityStore> entityRef = chunk.getReferenceTo(i);
                    VillagerData data = store.getComponent(entityRef, VillagerData.getComponentType());
                    if (data == null) continue;
                    java.util.UUID uuid = NpcManager.extractUuid(store, entityRef);
                    if (uuid == null) continue;
                    VillagerSummary summary = VillageManager.get().findVillagerSummary(village, uuid);
                    if (summary == null) continue;
                    summary.setHasHouse(data.isHasHouse());
                    summary.setHunger(data.getHunger());
                } catch (Exception ignored) {}
            }
        });
    }

    private void populateOverview(UICommandBuilder builder, VillageData village) {
        if (village == null || !village.isFounded()) {
            builder.set("#AlmanacSubtitle.Text", "A blank page awaits your first settlement");
            builder.set("#OverviewVillageName.Text", "No village founded");
            builder.set("#OverviewPopulation.Text", "Population: —");
            builder.set("#OverviewBuildings.Text", "Buildings: —");
            renderEmptyComplaints(builder, "Found a village first using the Founding Stone.");
            return;
        }

        String name = village.getVillageName().isEmpty() ? "Unnamed Village" : village.getVillageName();
        builder.set("#AlmanacSubtitle.Text", "Records of " + name);
        builder.set("#OverviewVillageName.Text", name);

        int villagers = village.getVillagerCount();
        int elves = village.getElfId() != null ? 1 : 0;
        builder.set("#OverviewPopulation.Text", "Population: " + (villagers + elves)
                + "  (" + villagers + " settler" + (villagers == 1 ? "" : "s")
                + ", " + elves + " sage)");

        int total = village.getBuildings().size();
        int completed = 0;
        for (var b : village.getBuildings()) if (b.isCompleted()) completed++;
        builder.set("#OverviewBuildings.Text", "Buildings: " + completed + " complete / " + total + " total");

        populateComplaints(builder, village);
    }

    private void populateComplaints(UICommandBuilder builder, VillageData village) {
        builder.clear("#ComplaintsListContainer");
        int rowsAdded = 0;
        for (VillagerSummary v : village.getVillagers()) {
            List<String> complaints = collectComplaints(v);
            if (complaints.isEmpty()) continue;
            String name = v.getFullName();
            if (name == null || name.isBlank()) name = "A settler";
            for (String complaint : complaints) {
                appendComplaintRow(builder, name, complaint);
                rowsAdded++;
            }
        }
        if (rowsAdded == 0) {
            renderEmptyComplaints(builder, "All settlers are content for now.");
        }
    }

    private static List<String> collectComplaints(VillagerSummary v) {
        List<String> out = new ArrayList<>(2);
        if (!v.isHasHouse()) out.add("has no home and sleeps in the Town Hall");
        if (v.isStarving())  out.add("is starving");
        else if (v.isHungry()) out.add("is going hungry");
        return out;
    }

    private void appendComplaintRow(UICommandBuilder builder, String name, String complaint) {
        builder.appendInline("#ComplaintsListContainer",
                "Group { LayoutMode: Left; Anchor: (Height: 22, Bottom: 2); Padding: (Horizontal: 6); " +
                "  Label { Anchor: (Width: 140); Style: (HorizontalAlignment: Start, VerticalAlignment: Center, TextColor: #cdd8e0, FontSize: 11, RenderBold: true); Text: \"" + escape(name) + "\"; } " +
                "  Label { FlexWeight: 1; Style: (HorizontalAlignment: Start, VerticalAlignment: Center, TextColor: #c89b78, FontSize: 11); Text: \"" + escape(complaint) + "\"; } " +
                "}");
    }

    private void renderEmptyComplaints(UICommandBuilder builder, String message) {
        builder.clear("#ComplaintsListContainer");
        builder.appendInline("#ComplaintsListContainer",
                "Group { LayoutMode: Left; Anchor: (Height: 28); Padding: (Horizontal: 6, Vertical: 4); " +
                "  Label { FlexWeight: 1; Style: (HorizontalAlignment: Start, VerticalAlignment: Center, TextColor: #5a6878, FontSize: 11, RenderItalics: true); Text: \"" + escape(message) + "\"; } " +
                "}");
    }

    // ───────────── Buildings catalog ─────────────

    private void populateBuildings(UICommandBuilder builder, UIEventBuilder events) {
        builder.clear("#BuildingListContainer");
        int rowIndex = 0;
        for (CatalogEntry entry : CATALOG) {
            if (!entry.category().equals(activeCategory)) continue;
            appendCatalogRow(builder, events, rowIndex, entry);
            rowIndex++;
        }
        if (rowIndex == 0) {
            builder.appendInline("#BuildingListContainer",
                    "Group { LayoutMode: Left; Anchor: (Height: 32); Padding: (Horizontal: 8, Vertical: 4); " +
                    "  Label { FlexWeight: 1; Style: (HorizontalAlignment: Start, VerticalAlignment: Center, TextColor: #5a6878, FontSize: 11, RenderItalics: true); Text: \"No buildings in this category yet.\"; } " +
                    "}");
        }
    }

    private void appendCatalogRow(UICommandBuilder builder, UIEventBuilder events,
                                  int rowIndex, CatalogEntry entry) {
        String displayName = BuildingType.getDisplayName(entry.type());
        String description = BuildingType.getShortDescription(entry.type());

        String buttonStyle = entry.implemented()
                ? "TextButtonStyle(" +
                  "Default: (Background: #1e4a22, LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #a8d8ac, FontSize: 11, RenderBold: true))," +
                  "Hovered: (Background: #286030, LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #d4f0d8, FontSize: 11, RenderBold: true))," +
                  "Pressed: (Background: #143218, LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #a8d8ac, FontSize: 11, RenderBold: true))" +
                  ")"
                : "TextButtonStyle(" +
                  "Default: (Background: #1a1f28, LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #4a5868, FontSize: 11))," +
                  "Hovered: (Background: #1a1f28, LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #4a5868, FontSize: 11))," +
                  "Pressed: (Background: #1a1f28, LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #4a5868, FontSize: 11))" +
                  ")";
        String buttonLabel = entry.implemented() ? "Get Anchor" : "Coming soon";

        builder.appendInline("#BuildingListContainer",
                "Group { LayoutMode: Left; Anchor: (Height: 56, Bottom: 6); Padding: (Horizontal: 8, Vertical: 6); Background: #0d1320; OutlineColor: #1c2838(0.6); OutlineSize: 1; " +
                "  Group { FlexWeight: 1; LayoutMode: Top; Padding: (Horizontal: 6, Vertical: 2); " +
                "    Label { Style: (HorizontalAlignment: Start, TextColor: #e0d4b0, FontSize: 13, RenderBold: true); Text: \"" + escape(displayName) + "\"; } " +
                "    Label { Anchor: (Top: 2); Style: (HorizontalAlignment: Start, TextColor: #8a9aab, FontSize: 10, Wrap: true); Text: \"" + escape(description) + "\"; } " +
                "  } " +
                "  TextButton #AlmanacAnchorBtn { Anchor: (Width: 110, Height: 36); " +
                "    Style: " + buttonStyle + ";" +
                "    Text: \"" + buttonLabel + "\";" +
                "  } " +
                "}");

        if (entry.implemented()) {
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#BuildingListContainer[" + rowIndex + "] #AlmanacAnchorBtn",
                    EventData.of(DialogEventData.ACTION_KEY, "get_anchor")
                            .append(DialogEventData.VALUE_KEY, entry.type()), false);
        }
    }

    // ───────────── Tab state ─────────────

    private void applyTabState(UICommandBuilder builder) {
        boolean overview = "overview".equals(activeTab);
        boolean buildings = "buildings".equals(activeTab);
        builder.set("#TabOverview.Visible", overview);
        builder.set("#TabOverviewInactive.Visible", !overview);
        builder.set("#TabBuildings.Visible", buildings);
        builder.set("#TabBuildingsInactive.Visible", !buildings);
        builder.set("#PanelOverview.Visible", overview);
        builder.set("#PanelBuildings.Visible", buildings);
    }

    private void applyCategoryState(UICommandBuilder builder) {
        applySubTabState(builder, "Civic",      BuildingType.CATEGORY_CIVIC);
        applySubTabState(builder, "Production", BuildingType.CATEGORY_PRODUCTION);
        applySubTabState(builder, "Crafting",   BuildingType.CATEGORY_CRAFTING);
        applySubTabState(builder, "Services",   BuildingType.CATEGORY_SERVICES);
        applySubTabState(builder, "Housing",    BuildingType.CATEGORY_HOUSING);
        applySubTabState(builder, "Defense",    BuildingType.CATEGORY_DEFENSE);
        applySubTabState(builder, "Storage",    BuildingType.CATEGORY_STORAGE);
    }

    private void applySubTabState(UICommandBuilder builder, String idSuffix, String category) {
        boolean active = activeCategory.equals(category);
        builder.set("#SubTab" + idSuffix + ".Visible", active);
        builder.set("#SubTab" + idSuffix + "Inactive.Visible", !active);
    }

    // ───────────── Events ─────────────

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, DialogEventData data) {
        String action = data.getAction();
        if (action == null) return;

        switch (action) {
            case "close" -> close();

            case "tab_overview" -> {
                activeTab = "overview";
                UICommandBuilder b = new UICommandBuilder();
                applyTabState(b);
                VillageData v = VillageManager.get().getVillageData(store, playerRef);
                if (v != null) refreshSummariesFromLiveEntities(store, v);
                populateOverview(b, v);
                sendUpdate(b, false);
            }

            case "tab_buildings" -> {
                activeTab = "buildings";
                UICommandBuilder b = new UICommandBuilder();
                applyTabState(b);
                populateBuildings(b, new UIEventBuilder());
                sendUpdate(b, false);
            }

            case "cat" -> {
                String cat = data.getValue();
                if (cat == null || cat.isBlank()) return;
                activeCategory = cat;
                UICommandBuilder b = new UICommandBuilder();
                applyCategoryState(b);
                populateBuildings(b, new UIEventBuilder());
                sendUpdate(b, false);
            }

            case "get_anchor" -> {
                String type = data.getValue();
                if (type == null || type.isBlank()) return;
                String anchorId = BuildingType.getAnchorBlockId(type);
                if (anchorId == null) {
                    LOG.warn("get_anchor: no anchor block for type '" + type + "'");
                    return;
                }
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) return;

                var tx = player.getInventory().getCombinedHotbarFirst()
                        .addItemStack(new ItemStack(anchorId, 1));
                String hint = tx.succeeded()
                        ? "Added " + BuildingType.getDisplayName(type) + " anchor to your pack."
                        : "Inventory is full — make room and try again.";
                UICommandBuilder b = new UICommandBuilder();
                b.set("#AlmanacSubtitle.Text", hint);
                sendUpdate(b, false);
            }
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
