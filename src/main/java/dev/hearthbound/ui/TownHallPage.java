package dev.hearthbound.ui;

import java.util.Map;
import java.util.UUID;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

/**
 * Town Hall management page — the main UI for the Founding Stone.
 *
 * Two phases:
 * - Pre-founding: village name input + "Found Village" button
 * - Post-founding: village info (tab 1) + construction/resources (tab 2)
 *
 * Resource storage is per-building: each BuildingRecord has its own storage map.
 * Deposit transfers matching items from the player's inventory into the target
 * building's storage.
 */
public class TownHallPage extends InteractiveCustomUIPage<DialogEventData> {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("ui.townhall");
    // Suggested names: real GoT minor settlements + originals in the same vein
    private static final String[] VILLAGE_NAMES = {
            "Maidenpool", "Saltpans", "Wickenden", "Pinkmaiden",
            "Stony Sept", "Hayford", "Ryamsport", "Sow's Horn",
            "Ashford", "Greywater", "Ironpool", "Saltmere",
            "Westmark", "Dunwater", "Millhaven", "Thornwick",
    };

    private final Ref<EntityStore> playerRef;
    private final PlayerRef networkPlayerRef;
    private final UUID ownerUuid;
    private final World world;
    private final int stoneX, stoneY, stoneZ;
    private boolean founded;
    private String activeTab = "village";
    private String currentVillageName = VILLAGE_NAMES[(int) (Math.random() * VILLAGE_NAMES.length)];

    public TownHallPage(PlayerRef player, Ref<EntityStore> playerRef, World world,
                         int stoneX, int stoneY, int stoneZ) {
        super(player, CustomPageLifetime.CanDismissOrCloseThroughInteraction, DialogEventData.CODEC);
        this.playerRef = playerRef;
        this.networkPlayerRef = player;
        this.ownerUuid = player.getUuid();
        this.world = world;
        this.stoneX = stoneX;
        this.stoneY = stoneY;
        this.stoneZ = stoneZ;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder events, Store<EntityStore> store) {
        builder.append("TownHall.ui");

        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        founded = village != null && village.isFounded();
        if (founded) {
            int reconciled = VillageManager.get().reconcileNpcReferences(store, playerRef, village, world, false);
            if (reconciled > 0) {
                village = VillageManager.get().getVillageData(store, playerRef);
            }
        }

        if (founded) {
            populatePostFounding(builder, events, village);
        } else {
            populatePreFounding(builder);
        }

        // Tab buttons — every tab has an active and inactive variant; both bind the same action
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabVillage",
                EventData.of(DialogEventData.ACTION_KEY, "tab_village"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabVillageInactive",
                EventData.of(DialogEventData.ACTION_KEY, "tab_village"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabPopulation",
                EventData.of(DialogEventData.ACTION_KEY, "tab_population"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabPopulationInactive",
                EventData.of(DialogEventData.ACTION_KEY, "tab_population"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabConstruction",
                EventData.of(DialogEventData.ACTION_KEY, "tab_construction"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabConstructionInactive",
                EventData.of(DialogEventData.ACTION_KEY, "tab_construction"), false);

        // Pre-founding: name text field + suggest + confirm
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#VillageNameInput",
                EventData.of(DialogEventData.VILLAGE_NAME_KEY, "#VillageNameInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SuggestButton",
                EventData.of(DialogEventData.ACTION_KEY, "suggest"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmButton",
                EventData.of(DialogEventData.ACTION_KEY, "confirm"), false);

        // Post-founding: construction tab
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DepositButton",
                EventData.of(DialogEventData.ACTION_KEY, "deposit"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#StartBuildButton",
                EventData.of(DialogEventData.ACTION_KEY, "start_build"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DepositRepairButton",
                EventData.of(DialogEventData.ACTION_KEY, "deposit_repair"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#RepairButton",
                EventData.of(DialogEventData.ACTION_KEY, "repair"), false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of(DialogEventData.ACTION_KEY, "close"), false);
    }

    // ========== Pre-founding UI ==========

    private void setTabActive(UICommandBuilder builder, String tab) {
        boolean villageActive = "village".equals(tab);
        boolean populationActive = "population".equals(tab);
        boolean constructionActive = "construction".equals(tab);
        builder.set("#TabVillage.Visible", villageActive);
        builder.set("#TabVillageInactive.Visible", founded && !villageActive);
        // Population/Construction tabs only visible after founding
        builder.set("#TabPopulation.Visible", founded && populationActive);
        builder.set("#TabPopulationInactive.Visible", founded && !populationActive);
        builder.set("#TabConstruction.Visible", founded && constructionActive);
        builder.set("#TabConstructionInactive.Visible", founded && !constructionActive);
    }

    private void populatePreFounding(UICommandBuilder builder) {
        builder.set("#VillageName.Text", "New Settlement");
        builder.set("#StageLabel.Text", "Choose a name for your village");

        builder.set("#PreFoundingInfo.Text",
                "Choose a name for your village and confirm to begin your journey. " +
                "Aelin will join you to help build your settlement.");
        builder.set("#PreFoundingHint.Text",
                "You can break the Founding Stone and move it before confirming.");

        builder.set("#VillageNameInput.Value", currentVillageName);

        builder.set("#PreFoundingGroup.Visible", true);
        builder.set("#PostFoundingGroup.Visible", false);
        builder.set("#PanelConstruction.Visible", false);

        // Hide tab bar entirely before founding — no tabs needed with a single panel
        builder.set("#TabBar.Visible", false);
        builder.set("#TabSeparator.Visible", false);
    }

    // ========== Post-founding UI ==========

    private void populatePostFounding(UICommandBuilder builder, UIEventBuilder events, VillageData village) {
        String name = village.getVillageName().isEmpty() ? "Unnamed Village" : village.getVillageName();
        builder.set("#VillageName.Text", name);

        String stageName = switch (village.getStage()) {
            case VillageData.STAGE_FOUNDED -> "A new settlement grows...";
            case VillageData.STAGE_TOWN_HALL -> "The heart of the village stands";
            case VillageData.STAGE_WAREHOUSE -> "Supplies are flowing in";
            default -> "Not yet founded";
        };
        builder.set("#StageLabel.Text", stageName);

        builder.set("#TabBar.Visible", true);
        builder.set("#TabSeparator.Visible", true);
        builder.set("#PreFoundingGroup.Visible", false);
        builder.set("#PostFoundingGroup.Visible", true);
        setTabActive(builder, activeTab);
        builder.set("#PanelVillage.Visible", "village".equals(activeTab));
        builder.set("#PanelPopulation.Visible", "population".equals(activeTab));
        builder.set("#PanelConstruction.Visible", "construction".equals(activeTab));

        // Buildings list (Village tab)
        StringBuilder buildings = new StringBuilder();
        for (BuildingRecord b : village.getBuildings()) {
            String status = b.isCompleted() ? "[Done]" : "[" + b.getBuildProgress() + "%]";
            buildings.append(status).append(" ").append(BuildingType.getDisplayName(b.getType())).append("\n");
        }
        builder.set("#BuildingsList.Text", buildings.isEmpty() ? "No buildings yet" : buildings.toString());
        builder.set("#ContainerHint.Text", "Use the Construction tab to deposit and manage resources.");

        // Population tab content (rows + per-row Recall bindings)
        populatePopulationTab(builder, events, village);

        // Construction tab content
        populateConstructionTab(builder, village);
    }

    /**
     * Renders the Population tab: one row per resident with a Recall button.
     * Aelin appears first (she is registered separately from VillagerSummary).
     * Each Recall button is bound during build() with the resident's UUID — the
     * page must be reopened to see newly-recruited villagers.
     */
    private void populatePopulationTab(UICommandBuilder builder, UIEventBuilder events, VillageData village) {
        builder.clear("#PopulationListContainer");

        int rowIndex = 0;
        if (village.getElfId() != null) {
            String elfName = dev.hearthbound.npc.ElfSage.resolveElfName();
            dev.hearthbound.npc.NpcRegistry.NpcRecord record =
                    dev.hearthbound.npc.NpcRegistry.get().getRecord(village.getElfId());
            appendPopulationRow(builder, events, rowIndex, elfName, "Sage",
                    record != null && record.entityUuid != null ? record.entityUuid : village.getElfId(),
                    record != null);
            rowIndex++;
        }
        for (dev.hearthbound.village.VillagerSummary v : village.getVillagers()) {
            String role = roleLabel(v);
            dev.hearthbound.npc.NpcRegistry.NpcRecord record =
                    VillageManager.get().findVillagerRecord(village, v.getVillagerUuid());
            appendPopulationRow(builder, events, rowIndex, v.getFullName(), role,
                    record != null && record.entityUuid != null ? record.entityUuid : v.getVillagerUuid(),
                    record != null);
            rowIndex++;
        }
        builder.set("#PopulationCount.Text", "— " + rowIndex + " residents");
    }

    private static String roleLabel(dev.hearthbound.village.VillagerSummary v) {
        String prof = v.getProfession();
        if (prof == null || prof.isBlank()) return "Resident";
        return switch (prof) {
            case dev.hearthbound.village.VillagerData.PROF_FARMER -> "Farmer";
            case dev.hearthbound.village.VillagerData.PROF_LUMBERJACK -> "Lumberjack";
            case dev.hearthbound.village.VillagerData.PROF_MASON -> "Miner";
            case dev.hearthbound.village.VillagerData.PROF_GUARD -> "Guard";
            case dev.hearthbound.village.VillagerData.PROF_BLACKSMITH -> "Blacksmith";
            default -> "Resident";
        };
    }

    /**
     * Inserts one row into {@code #PopulationListContainer}. When {@code hasRecord}
     * is true the row carries a working Recall button bound to a "recall" action
     * carrying the NPC's UUID via {@code VALUE_KEY}; when false the resident is
     * a tombstone (registry record was lost) — the row is rendered with a
     * "(missing)" tag and no button, since teleport would silently fail and the
     * cleanup tick will eventually free the slot.
     *
     * <p>The inlined DSL fragment is parsed in isolation, so it cannot reference
     * named expressions from the host {@code .ui} document — every style must be
     * written out inline. The binding selector includes the container path plus
     * the row index so it resolves to the unique button in this row even though
     * sibling rows reuse the same {@code #RecallBtn} id.
     */
    private void appendPopulationRow(UICommandBuilder builder, UIEventBuilder events,
                                     int rowIndex, String name, String role,
                                     java.util.UUID npcUuid, boolean hasRecord) {
        if (hasRecord) {
            builder.appendInline("#PopulationListContainer",
                    "Group { LayoutMode: Left; Anchor: (Height: 32, Bottom: 4); Padding: (Horizontal: 6); " +
                    "  Label { FlexWeight: 1; Style: (HorizontalAlignment: Start, VerticalAlignment: Center, TextColor: #cdd8e0, FontSize: 12); Text: \"" + escape(name) + "\"; } " +
                    "  Label { Anchor: (Width: 90); Style: (HorizontalAlignment: Start, VerticalAlignment: Center, TextColor: #6a8898, FontSize: 10); Text: \"" + escape(role) + "\"; } " +
                    "  TextButton #RecallBtn { Anchor: (Width: 80, Height: 26); " +
                    "    Style: (" +
                    "      Default: (Background: #2a3f56, LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #b8d0e4, FontSize: 11))," +
                    "      Hovered: (Background: #3a526e, LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #d8e8f4, FontSize: 11))," +
                    "      Pressed: (Background: #1c2c40, LabelStyle: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #b8d0e4, FontSize: 11))" +
                    "    );" +
                    "    Text: \"Recall\";" +
                    "  } " +
                    "}");
            events.addEventBinding(CustomUIEventBindingType.Activating,
                    "#PopulationListContainer[" + rowIndex + "] #RecallBtn",
                    EventData.of(DialogEventData.ACTION_KEY, "recall")
                            .append(DialogEventData.VALUE_KEY, npcUuid.toString()),
                    false);
        } else {
            builder.appendInline("#PopulationListContainer",
                    "Group { LayoutMode: Left; Anchor: (Height: 32, Bottom: 4); Padding: (Horizontal: 6); " +
                    "  Label { FlexWeight: 1; Style: (HorizontalAlignment: Start, VerticalAlignment: Center, TextColor: #6a7882, FontSize: 12); Text: \"" + escape(name) + "\"; } " +
                    "  Label { Anchor: (Width: 90); Style: (HorizontalAlignment: Start, VerticalAlignment: Center, TextColor: #6a8898, FontSize: 10); Text: \"" + escape(role) + "\"; } " +
                    "  Label { Anchor: (Width: 80); Style: (HorizontalAlignment: Center, VerticalAlignment: Center, TextColor: #c87878, FontSize: 10, RenderItalics: true); Text: \"(missing)\"; } " +
                    "}");
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void populateConstructionTab(UICommandBuilder builder, VillageData village) {
        BuildingRecord townHall = village.findBuilding(BuildingType.TOWN_HALL);
        boolean townHallBuilt = townHall != null && townHall.isCompleted();

        // Town Hall is the only building managed from this page. Once it's done,
        // the tab becomes a placeholder for future upgrades — other buildings
        // have their own anchors and UIs.
        if (townHallBuilt) {
            populateTownHallCompleted(builder);
            renderRepairSection(builder, townHall);
            return;
        }

        BuildingRecord activeRecord = BuildingSystem.get().getActiveRecord();
        boolean isBuildingThis = BuildingSystem.get().isBuilding()
                && activeRecord != null && BuildingType.TOWN_HALL.equals(activeRecord.getType());
        boolean elfBusy = BuildingSystem.get().isBuilding() && !isBuildingThis;

        builder.set("#ConstructionTitle.Text", BuildingType.getDisplayName(BuildingType.TOWN_HALL));

        Map<String, Integer> required;
        if (isBuildingThis) {
            required = BuildingSystem.get().getRemainingResources();
            if (required == null) required = BuildingSystem.getRequiredResources(BuildingType.TOWN_HALL);
        } else {
            required = BuildingSystem.getRequiredResources(BuildingType.TOWN_HALL);
        }

        Map<String, Integer> have = readBuildingStorage(townHall);
        builder.set("#ResourceListContainer.Visible", true);
        boolean allSatisfied = renderResourceList(builder, required, have);

        applyConstructionState(builder, isBuildingThis, false, allSatisfied, elfBusy);
    }

    /** Town Hall is built — show an upgrade-coming-soon placeholder, hide all build controls. */
    private void populateTownHallCompleted(UICommandBuilder builder) {
        builder.set("#ConstructionTitle.Text", BuildingType.getDisplayName(BuildingType.TOWN_HALL));
        builder.set("#ConstructionStatus.Text",
                "The Town Hall stands complete. Upgrades coming in a future update.");
        builder.set("#ResourceListWrapper.Visible", false);
        builder.set("#ResourceHeader.Visible", false);
        builder.set("#ResourceListContainer.Visible", false);
        builder.set("#StartBuildButton.Visible", false);
        builder.set("#DepositButton.Visible", false);
        builder.set("#BuildProgressLabel.Visible", false);
        builder.set("#DepositHint.Text", "");
    }

    /**
     * Returns the Town Hall record. The Construction tab in this page only manages the
     * Town Hall; other buildings have their own anchors/pages (e.g. Warehouse via Counter).
     */
    private BuildingRecord resolveTargetBuilding(VillageData village) {
        return village.findBuilding(BuildingType.TOWN_HALL);
    }

    private static Map<String, Integer> readBuildingStorage(BuildingRecord record) {
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        if (record == null) return out;
        for (var entry : record.getStorage().object2IntEntrySet()) {
            out.put(entry.getKey(), entry.getIntValue());
        }
        return out;
    }

    /** Renders rows into #ResourceListContainer. Returns true when every requirement is met. */
    private boolean renderResourceList(UICommandBuilder builder,
                                       Map<String, Integer> required,
                                       Map<String, Integer> have) {
        String language = networkPlayerRef != null ? networkPlayerRef.getLanguage() : null;
        return dev.hearthbound.util.ResourceListRenderer.renderRequired(
                builder, "#ResourceListContainer", required, have, language);
    }

    /** Sets status text, button visibility and hints to match the current phase. */
    private void applyConstructionState(UICommandBuilder builder,
                                        boolean isBuilding,
                                        boolean constructionFinished,
                                        boolean allSatisfied,
                                        boolean elfBusy) {
        if (isBuilding) {
            builder.set("#ConstructionStatus.Text", "Under construction...");
            builder.set("#StartBuildButton.Visible", false);
            builder.set("#DepositButton.Visible", true);
            builder.set("#BuildProgressLabel.Visible", true);
            builder.set("#BuildProgressLabel.Text",
                    "Progress: " + BuildingSystem.get().getBuildProgress() + "%" +
                    (BuildingSystem.get().isPaused() ? " (waiting for resources)" : ""));
            builder.set("#DepositHint.Text", "Deposit more resources if construction pauses.");
            return;
        }

        builder.set("#BuildProgressLabel.Visible", false);

        if (constructionFinished) {
            builder.set("#ConstructionStatus.Text", "Complete!");
            builder.set("#StartBuildButton.Visible", false);
            builder.set("#DepositButton.Visible", false);
            builder.set("#DepositHint.Text", "");
            return;
        }

        if (elfBusy) {
            BuildingRecord activeRecord = BuildingSystem.get().getActiveRecord();
            String busyName = activeRecord != null ? BuildingType.getDisplayName(activeRecord.getType()) : "another building";
            String elfName = dev.hearthbound.npc.ElfSage.resolveElfName();
            builder.set("#ConstructionStatus.Text", elfName + " is busy building " + busyName + ".");
            builder.set("#DepositButton.Visible", !allSatisfied);
            builder.set("#StartBuildButton.Visible", allSatisfied);
            builder.set("#DepositHint.Text", allSatisfied
                    ? "Waiting for " + elfName + " to finish."
                    : "You can deposit resources in advance.");
            return;
        }

        if (allSatisfied) {
            builder.set("#ConstructionStatus.Text", "All resources gathered — ready to build.");
            builder.set("#StartBuildButton.Visible", true);
            builder.set("#DepositButton.Visible", false);
            builder.set("#DepositHint.Text", "Press Start Construction to begin.");
        } else {
            builder.set("#ConstructionStatus.Text", "Gather resources to begin construction.");
            builder.set("#StartBuildButton.Visible", false);
            builder.set("#DepositButton.Visible", true);
            builder.set("#DepositHint.Text", "Click Deposit to transfer matching resources from your inventory.");
        }
    }

    private void renderRepairSection(UICommandBuilder builder, BuildingRecord record) {
        if (record == null || !record.isCompleted()) {
            builder.set("#RepairSection.Visible", false);
            return;
        }
        builder.set("#RepairSection.Visible", true);
        Map<String, Integer> cost = BuildingSystem.getRepairCost(record, world);
        builder.clear("#RepairResourceContainer");
        if (cost.isEmpty()) {
            builder.set("#RepairStatus.Text", "Building is intact — no repairs needed.");
            builder.set("#RepairResourceContainer.Visible", false);
            builder.set("#DepositRepairButton.Visible", false);
            builder.set("#RepairButton.Visible", false);
            builder.set("#RepairHint.Text", "");
        } else {
            builder.set("#RepairStatus.Text", cost.size() + " block(s) need repair:");
            builder.set("#RepairResourceContainer.Visible", true);
            String language = networkPlayerRef != null ? networkPlayerRef.getLanguage() : null;
            Map<String, Integer> have = readBuildingStorage(record);
            boolean hasDeficit = cost.entrySet().stream()
                    .anyMatch(e -> have.getOrDefault(e.getKey(), 0) < e.getValue());
            dev.hearthbound.util.ResourceListRenderer.renderRequired(
                    builder, "#RepairResourceContainer", cost, have, language);
            builder.set("#DepositRepairButton.Visible", hasDeficit);
            builder.set("#RepairButton.Visible", !hasDeficit);
            builder.set("#RepairHint.Text", !hasDeficit
                    ? "All materials ready — press Repair to restore the building."
                    : "Deposit missing materials to repair.");
        }
    }

    // ========== Event handling ==========

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, DialogEventData data) {
        String action = data.getAction();

        // ValueChanged events carry no action — just update the stored name
        String incomingName = data.getVillageName();
        if (incomingName != null && !incomingName.isEmpty()) {
            currentVillageName = incomingName;
        }

        switch (action) {
            case "close" -> close();

            case "tab_village" -> {
                activeTab = "village";
                UICommandBuilder builder = new UICommandBuilder();
                builder.set("#PanelVillage.Visible", true);
                builder.set("#PanelPopulation.Visible", false);
                builder.set("#PanelConstruction.Visible", false);
                setTabActive(builder, "village");
                sendUpdate(builder, false);
            }

            case "tab_population" -> {
                if (!founded) return;
                activeTab = "population";
                UICommandBuilder builder = new UICommandBuilder();
                builder.set("#PanelVillage.Visible", false);
                builder.set("#PanelPopulation.Visible", true);
                builder.set("#PanelConstruction.Visible", false);
                setTabActive(builder, "population");
                // Rows were rendered in build(); just refresh the count in case it changed.
                VillageData v = VillageManager.get().getVillageData(store, playerRef);
                if (v != null) {
                    int total = v.getVillagerCount() + (v.getElfId() != null ? 1 : 0);
                    builder.set("#PopulationCount.Text", "— " + total + " residents");
                }
                sendUpdate(builder, false);
            }

            case "tab_construction" -> {
                if (!founded) return;
                activeTab = "construction";
                UICommandBuilder builder = new UICommandBuilder();
                builder.set("#PanelVillage.Visible", false);
                builder.set("#PanelPopulation.Visible", false);
                builder.set("#PanelConstruction.Visible", true);
                setTabActive(builder, "construction");

                VillageData village = VillageManager.get().getVillageData(store, playerRef);
                if (village != null) {
                    populateConstructionTab(builder, village);
                }
                sendUpdate(builder, false);
            }

            case "recall" -> {
                if (!founded) return;
                String uuidStr = data.getValue();
                if (uuidStr == null || uuidStr.isBlank()) return;
                java.util.UUID npcUuid;
                try {
                    npcUuid = java.util.UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    LOG.warn("recall: invalid uuid '" + uuidStr + "'");
                    return;
                }
                VillageData v = VillageManager.get().getVillageData(store, playerRef);
                if (v != null) {
                    int reconciled = VillageManager.get().reconcileNpcReferences(store, playerRef, v, world, false);
                    if (reconciled > 0) {
                        v = VillageManager.get().getVillageData(store, playerRef);
                    }
                }
                double[] stand = dev.hearthbound.building.BuildingLayout.townHallStandPoint(v);
                if (stand == null) {
                    LOG.warn("recall: town hall layout missing — village not founded?");
                    return;
                }
                dev.hearthbound.npc.NpcRegistry.NpcRecord record =
                        dev.hearthbound.npc.NpcRegistry.get().getRecord(npcUuid);
                if (record == null && v != null) {
                    record = VillageManager.get().findVillagerRecord(v, npcUuid);
                }
                boolean ok = record != null
                        ? dev.hearthbound.npc.NpcTeleporter.recall(world, record, stand[0], stand[1], stand[2])
                        : dev.hearthbound.npc.NpcTeleporter.recall(world, npcUuid, stand[0], stand[1], stand[2]);
                LOG.info("recall: " + npcUuid + " → town hall stand=("
                        + stand[0] + "," + stand[1] + "," + stand[2] + ") ok=" + ok);
            }

            case "suggest" -> {
                currentVillageName = VILLAGE_NAMES[(int) (Math.random() * VILLAGE_NAMES.length)];
                UICommandBuilder b = new UICommandBuilder();
                b.set("#VillageNameInput.Value", currentVillageName);
                sendUpdate(b, false);
            }

            case "confirm" -> {
                if (founded) return;

                String typed = data.getVillageName();
                String villageName = (typed != null && !typed.isBlank()) ? typed.strip() : currentVillageName;

                int rotation = BuildingSystem.get().getActiveRotation(store, playerRef);

                // Point of no return
                BuildingSystem.get().confirmFounding(
                        store, playerRef, world, villageName,
                        stoneX, stoneY, stoneZ, rotation);

                // Try to integrate a pre-built or pasted Town Hall before the elf starts
                // construction. confirmFounding has just added the Town Hall record to the
                // village — fetch it and run the scanner against it.
                VillageData dv = VillageManager.get().getVillageData(store, playerRef);
                if (dv != null) {
                    BuildingRecord townHall = dv.findBuilding(BuildingType.TOWN_HALL);
                    if (townHall != null) {
                        BuildingSystem.get().tryIntegrateExisting(
                                store, playerRef, world, townHall, rotation);
                    }
                }

                founded = true;

                // Close UI — village is founded, player can reopen with F
                close();
            }

            case "deposit" -> {
                VillageData dv = VillageManager.get().getVillageData(store, playerRef);
                if (dv == null) return;

                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) return;

                BuildingRecord target = resolveTargetBuilding(dv);
                if (target == null) return;

                Map<String, Integer> required = BuildingSystem.get().isBuilding()
                        ? BuildingSystem.get().getRemainingResources() : null;
                if (required == null) {
                    required = BuildingSystem.getRequiredResources(target.getType());
                }

                com.hypixel.hytale.protocol.GameMode gm = player.getGameMode();
                boolean isCreative = gm != null && "Creative".equals(gm.name());
                int deposited = isCreative
                        ? depositCreative(target, required)
                        : depositFromInventory(player, target, required);

                VillageManager.get().save(store, playerRef, dv);

                UICommandBuilder builder = new UICommandBuilder();
                if (deposited > 0) {
                    builder.set("#DepositHint.Text", isCreative
                            ? "Creative: " + deposited + " items conjured into storage."
                            : "Deposited " + deposited + " items into storage.");
                } else {
                    builder.set("#DepositHint.Text", isCreative
                            ? "No resources needed."
                            : "No matching resources found in your inventory.");
                }
                populateConstructionTab(builder, dv);
                sendUpdate(builder, false);
            }

            case "start_build" -> {
                VillageData village = VillageManager.get().getVillageData(store, playerRef);
                if (village == null) return;

                BuildingRecord target = resolveTargetBuilding(village);
                if (target == null) return;

                VillageManager.get().save(store, playerRef, village);

                BuildingSystem.get().startResourceBuilding(
                        store, playerRef, world, target, village.getRotation(), ownerUuid);

                UICommandBuilder builder = new UICommandBuilder();
                populateConstructionTab(builder, village);
                sendUpdate(builder, false);
            }

            case "deposit_repair" -> {
                if (!founded) return;
                VillageData dv = VillageManager.get().getVillageData(store, playerRef);
                if (dv == null) return;

                BuildingRecord target = resolveTargetBuilding(dv);
                if (target == null || !target.isCompleted()) return;

                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) return;

                Map<String, Integer> cost = BuildingSystem.getRepairCost(target, world);
                com.hypixel.hytale.protocol.GameMode gm = player.getGameMode();
                boolean isCreative = gm != null && "Creative".equals(gm.name());
                int deposited = isCreative
                        ? depositCreative(target, cost)
                        : depositFromInventory(player, target, cost);

                VillageManager.get().save(store, playerRef, dv);

                UICommandBuilder builder = new UICommandBuilder();
                builder.set("#RepairHint.Text", isCreative
                        ? (deposited > 0 ? "Creative: " + deposited + " items conjured." : "Nothing needed.")
                        : (deposited > 0 ? "Deposited " + deposited + " items." : "No matching resources in inventory."));
                populateConstructionTab(builder, dv);
                sendUpdate(builder, false);
            }

            case "repair" -> {
                if (!founded) return;
                VillageData dv = VillageManager.get().getVillageData(store, playerRef);
                if (dv == null) return;

                BuildingRecord target = resolveTargetBuilding(dv);
                if (target == null || !target.isCompleted()) return;

                BuildingSystem.get().startRepair(store, playerRef, world, target);

                UICommandBuilder builder = new UICommandBuilder();
                populateConstructionTab(builder, dv);
                sendUpdate(builder, false);
            }
        }
    }

    private String getElfName(VillageData village) {
        return dev.hearthbound.npc.ElfSage.resolveElfName();
    }

    /** Fills the building's storage to meet every requirement. Returns how many items were added. */
    private int depositCreative(BuildingRecord target, Map<String, Integer> required) {
        int total = 0;
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            String itemId = entry.getKey();
            int deficit = entry.getValue() - target.getResourceCount(itemId);
            if (deficit <= 0) continue;
            target.addResource(itemId, deficit);
            total += deficit;
        }
        return total;
    }

    /**
     * Transfers required items from the player's inventory into the building's storage.
     * Returns the total number of items moved.
     */
    private int depositFromInventory(Player player, BuildingRecord target, Map<String, Integer> required) {
        int totalDeposited = 0;
        try {
            ItemContainer playerInv = player.getInventory().getCombinedHotbarFirst();

            for (Map.Entry<String, Integer> entry : required.entrySet()) {
                String itemId = entry.getKey();
                int deficit = entry.getValue() - target.getResourceCount(itemId);
                if (deficit <= 0) continue;

                for (int i = 0; i < deficit; i++) {
                    ItemStack one = new ItemStack(itemId, 1);
                    var removeTx = playerInv.removeItemStack(one, true, true);
                    if (!removeTx.succeeded()) break;
                    target.addResource(itemId, 1);
                    totalDeposited++;
                }
            }
        } catch (Exception e) {
            LOG.warn("Error depositing resources", e);
        }
        return totalDeposited;
    }

    @Override
    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
        // Ghost preview stays if not founded — player can come back
    }
}
