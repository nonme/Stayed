package dev.hearthbound.ui;

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
import dev.hearthbound.building.ResourceBlockPlacer;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final Logger LOGGER = Logger.getLogger(TownHallPage.class.getName());

    private static final String[] VILLAGE_NAMES = {
            "Hearthstone", "Oakvale", "Willowbrook", "Ashford",
            "Thornfield", "Ravenmoor", "Brighthollow", "Stonebridge",
            "Ferndale", "Maplecrest", "Windmere", "Duskwood"
    };

    private final Ref<EntityStore> playerRef;
    private final UUID ownerUuid;
    private final World world;
    private final int stoneX, stoneY, stoneZ;
    private boolean founded;
    private String activeTab = "village";
    private int nameIndex = 0;

    public TownHallPage(PlayerRef player, Ref<EntityStore> playerRef, World world,
                         int stoneX, int stoneY, int stoneZ) {
        super(player, CustomPageLifetime.CanDismissOrCloseThroughInteraction, DialogEventData.CODEC);
        this.playerRef = playerRef;
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
            populatePostFounding(builder, village);
        } else {
            populatePreFounding(builder);
        }

        // Tab buttons — bind all 4 variants (active + inactive for each tab)
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabVillage",
                EventData.of(DialogEventData.ACTION_KEY, "tab_village"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabVillageInactive",
                EventData.of(DialogEventData.ACTION_KEY, "tab_village"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabConstruction",
                EventData.of(DialogEventData.ACTION_KEY, "tab_construction"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabConstructionInactive",
                EventData.of(DialogEventData.ACTION_KEY, "tab_construction"), false);

        // Pre-founding: name picker + confirm
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NamePrevButton",
                EventData.of(DialogEventData.ACTION_KEY, "name_prev"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NameNextButton",
                EventData.of(DialogEventData.ACTION_KEY, "name_next"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmButton",
                EventData.of(DialogEventData.ACTION_KEY, "confirm"), false);

        // Post-founding: construction tab
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DepositButton",
                EventData.of(DialogEventData.ACTION_KEY, "deposit"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#StartBuildButton",
                EventData.of(DialogEventData.ACTION_KEY, "start_build"), false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of(DialogEventData.ACTION_KEY, "close"), false);
    }

    // ========== Pre-founding UI ==========

    private void setTabActive(UICommandBuilder builder, String tab) {
        boolean villageActive = "village".equals(tab);
        builder.set("#TabVillage.Visible", villageActive);
        builder.set("#TabVillageInactive.Visible", !villageActive);
        builder.set("#TabConstruction.Visible", !villageActive);
        builder.set("#TabConstructionInactive.Visible", villageActive);
    }

    private void populatePreFounding(UICommandBuilder builder) {
        builder.set("#VillageName.Text", "New Settlement");
        builder.set("#StageLabel.Text", "Choose a name for your village");

        builder.set("#PreFoundingInfo.Text",
                "Choose a name for your village and confirm to begin your journey. " +
                "The elf sage will join you to help build your settlement.");
        builder.set("#PreFoundingHint.Text",
                "You can break the Founding Stone and move it before confirming.");

        builder.set("#SelectedNameLabel.Text", VILLAGE_NAMES[nameIndex]);

        builder.set("#PreFoundingGroup.Visible", true);
        builder.set("#PostFoundingGroup.Visible", false);
        builder.set("#PanelConstruction.Visible", false);

        // Hide both construction tab buttons before founding
        builder.set("#TabConstruction.Visible", false);
        builder.set("#TabConstructionInactive.Visible", false);
        // Village tab — active by default
        builder.set("#TabVillage.Visible", true);
        builder.set("#TabVillageInactive.Visible", false);
    }

    // ========== Post-founding UI ==========

    private void populatePostFounding(UICommandBuilder builder, VillageData village) {
        String name = village.getVillageName().isEmpty() ? "Unnamed Village" : village.getVillageName();
        builder.set("#VillageName.Text", name);

        String stageName = switch (village.getStage()) {
            case VillageData.STAGE_FOUNDED -> "A new settlement grows...";
            case VillageData.STAGE_TOWN_HALL -> "The heart of the village stands";
            case VillageData.STAGE_WAREHOUSE -> "Supplies are flowing in";
            default -> "Not yet founded";
        };
        builder.set("#StageLabel.Text", stageName);

        builder.set("#PreFoundingGroup.Visible", false);
        builder.set("#PostFoundingGroup.Visible", true);
        setTabActive(builder, activeTab);
        builder.set("#PanelVillage.Visible", "village".equals(activeTab));
        builder.set("#PanelConstruction.Visible", "construction".equals(activeTab));

        // Buildings list
        StringBuilder buildings = new StringBuilder();
        for (BuildingRecord b : village.getBuildings()) {
            String status = b.isCompleted() ? "[Done]" : "[" + b.getBuildProgress() + "%]";
            buildings.append(status).append(" ").append(BuildingType.getDisplayName(b.getType())).append("\n");
        }
        builder.set("#BuildingsList.Text", buildings.isEmpty() ? "No buildings yet" : buildings.toString());
        boolean hasElf = village.getElfId() != null;
        int total = village.getVillagerCount() + (hasElf ? 1 : 0);
        builder.set("#PopulationCount.Text", "— " + total + " residents");
        StringBuilder pop = new StringBuilder();
        if (hasElf) pop.append(getElfName(village)).append(" (Elf Sage)\n");
        for (dev.hearthbound.village.VillagerSummary v : village.getVillagers()) {
            pop.append(v.getFullName()).append("\n");
        }
        builder.set("#PopulationInfo.Text", pop.isEmpty() ? "No residents yet" : pop.toString().stripTrailing());
        builder.set("#ContainerHint.Text", "Use the Construction tab to deposit and manage resources.");

        // Set up construction tab content
        populateConstructionTab(builder, village);
    }

    private void populateConstructionTab(UICommandBuilder builder, VillageData village) {
        BuildingRecord townHall = village.findBuilding(BuildingType.TOWN_HALL);
        boolean townHallBuilt = townHall != null && townHall.isCompleted();

        String nextBuildingType = townHallBuilt ? BuildingType.WAREHOUSE : BuildingType.TOWN_HALL;
        BuildingRecord activeRecord = BuildingSystem.get().getActiveRecord();
        boolean isBuildingThis = BuildingSystem.get().isBuilding()
                && activeRecord != null && nextBuildingType.equals(activeRecord.getType());
        boolean elfBusy = BuildingSystem.get().isBuilding() && !isBuildingThis;
        boolean constructionFinished = village.isConstructionStarted() && townHallBuilt && !BuildingSystem.get().isBuilding();

        builder.set("#ConstructionTitle.Text", BuildingType.getDisplayName(nextBuildingType));

        Map<String, Integer> required;
        if (isBuildingThis) {
            required = BuildingSystem.get().getRemainingResources();
            if (required == null) required = BuildingSystem.getRequiredResources(nextBuildingType);
        } else {
            required = BuildingSystem.getRequiredResources(nextBuildingType);
        }

        Map<String, Integer> have = readBuildingStorage(resolveTargetBuilding(village, false));
        boolean allSatisfied = renderResourceList(builder, required, have);

        applyConstructionState(builder, isBuildingThis, constructionFinished, allSatisfied, elfBusy);
    }

    /**
     * Returns the building the player is currently outfitting: Town Hall until it is built,
     * then the next staged building (Warehouse). Creates the next record on demand when
     * {@code createIfMissing} is true (used for deposit flow so resources have a home).
     */
    private BuildingRecord resolveTargetBuilding(VillageData village, boolean createIfMissing) {
        BuildingRecord townHall = village.findBuilding(BuildingType.TOWN_HALL);
        boolean townHallBuilt = townHall != null && townHall.isCompleted();
        if (!townHallBuilt) return townHall;

        BuildingRecord warehouse = village.findBuilding(BuildingType.WAREHOUSE);
        if (warehouse != null || !createIfMissing) return warehouse;

        warehouse = new BuildingRecord(BuildingType.WAREHOUSE, stoneX - 8, stoneY, stoneZ);
        village.addBuilding(warehouse);
        return warehouse;
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
        builder.clear("#ResourceListContainer");
        boolean allSatisfied = true;
        int rowIndex = 0;
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            String itemId = entry.getKey();
            int need = entry.getValue();
            int got = have.getOrDefault(itemId, 0);
            boolean satisfied = got >= need;
            if (!satisfied) allSatisfied = false;

            String countColor = satisfied ? "#78c880" : "#c87878";
            String nameColor = satisfied ? "#8ab8a0" : "#9ab0bc";
            String displayName = itemId.replace("_", " ");

            builder.appendInline("#ResourceListContainer",
                    "Group { LayoutMode: Left; Anchor: (Height: 32, Bottom: 2); Padding: (Horizontal: 4); " +
                    "  ItemIcon { Anchor: (Width: 24, Height: 24); } " +
                    "  Label { Anchor: (Left: 8); FlexWeight: 1; Style: (HorizontalAlignment: Start, VerticalAlignment: Center, TextColor: " + nameColor + ", FontSize: 11); Text: \"" + displayName + "\"; } " +
                    "  Label { Anchor: (Width: 60); Style: (HorizontalAlignment: End, VerticalAlignment: Center, TextColor: " + countColor + ", FontSize: 11, RenderBold: true); Text: \"" + got + " / " + need + "\"; } " +
                    "}");
            builder.set("#ResourceListContainer[" + rowIndex + "][0].ItemId", itemId);
            rowIndex++;
        }
        return allSatisfied;
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

    // ========== Event handling ==========

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, DialogEventData data) {
        String action = data.getAction();

        switch (action) {
            case "close" -> close();

            case "tab_village" -> {
                activeTab = "village";
                UICommandBuilder builder = new UICommandBuilder();
                builder.set("#PanelVillage.Visible", true);
                builder.set("#PanelConstruction.Visible", false);
                setTabActive(builder, "village");
                sendUpdate(builder, false);
            }

            case "tab_construction" -> {
                if (!founded) return;
                activeTab = "construction";
                UICommandBuilder builder = new UICommandBuilder();
                builder.set("#PanelVillage.Visible", false);
                builder.set("#PanelConstruction.Visible", true);
                setTabActive(builder, "construction");

                VillageData village = VillageManager.get().getVillageData(store, playerRef);
                if (village != null) {
                    populateConstructionTab(builder, village);
                }
                sendUpdate(builder, false);
            }

            case "name_prev" -> {
                nameIndex = (nameIndex - 1 + VILLAGE_NAMES.length) % VILLAGE_NAMES.length;
                UICommandBuilder b = new UICommandBuilder();
                b.set("#SelectedNameLabel.Text", VILLAGE_NAMES[nameIndex]);
                sendUpdate(b, false);
            }

            case "name_next" -> {
                nameIndex = (nameIndex + 1) % VILLAGE_NAMES.length;
                UICommandBuilder b = new UICommandBuilder();
                b.set("#SelectedNameLabel.Text", VILLAGE_NAMES[nameIndex]);
                sendUpdate(b, false);
            }

            case "confirm" -> {
                if (founded) return;

                String villageName = VILLAGE_NAMES[nameIndex];

                int rotation = BuildingSystem.get().getActiveRotation();

                // Point of no return
                BuildingSystem.get().confirmFounding(
                        store, playerRef, world, villageName,
                        stoneX, stoneY, stoneZ, rotation);

                founded = true;

                // Close UI — village is founded, player can reopen with F
                close();
            }

            case "deposit" -> {
                VillageData dv = VillageManager.get().getVillageData(store, playerRef);
                if (dv == null) return;

                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) return;

                BuildingRecord target = resolveTargetBuilding(dv, true);
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

                BuildingRecord target = resolveTargetBuilding(village, true);
                if (target == null) return;

                VillageManager.get().save(store, playerRef, village);

                BuildingSystem.get().startResourceBuilding(
                        store, playerRef, world, target, village.getRotation(), ownerUuid);

                UICommandBuilder builder = new UICommandBuilder();
                populateConstructionTab(builder, village);
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
            LOGGER.log(Level.WARNING, "Error depositing resources", e);
        }
        return totalDeposited;
    }

    @Override
    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
        // Ghost preview stays if not founded — player can come back
    }
}
