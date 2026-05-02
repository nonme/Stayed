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
import dev.hearthbound.building.StorageChestReader;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Warehouse management UI — opened when the player presses F on the Counter anchor block.
 * Tabs: Info | Storage (live chest contents) | Construction
 */
public class WarehousePage extends InteractiveCustomUIPage<DialogEventData> {

    private static final Logger LOGGER = Logger.getLogger(WarehousePage.class.getName());
    // Scan radius around the counter anchor when reading storage chests.
    private static final int STORAGE_SCAN_RADIUS = 20;

    private final Ref<EntityStore> playerEntityRef;
    private final PlayerRef networkPlayerRef;
    private final UUID ownerUuid;
    private final World world;
    private final int counterX, counterY, counterZ;

    private boolean confirmed;
    private String activeTab = "info";

    public WarehousePage(PlayerRef player, Ref<EntityStore> playerEntityRef, World world,
                         int counterX, int counterY, int counterZ) {
        super(player, CustomPageLifetime.CanDismissOrCloseThroughInteraction, DialogEventData.CODEC);
        this.playerEntityRef = playerEntityRef;
        this.networkPlayerRef = player;
        this.ownerUuid = player.getUuid();
        this.world = world;
        this.counterX = counterX;
        this.counterY = counterY;
        this.counterZ = counterZ;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder events, Store<EntityStore> store) {
        builder.append("Warehouse.ui");

        BuildingRecord record = findRecord(store);
        confirmed = record != null;

        populate(builder, record);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabInfo",
                EventData.of(DialogEventData.ACTION_KEY, "tab_info"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabInfoInactive",
                EventData.of(DialogEventData.ACTION_KEY, "tab_info"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabStorage",
                EventData.of(DialogEventData.ACTION_KEY, "tab_storage"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabStorageInactive",
                EventData.of(DialogEventData.ACTION_KEY, "tab_storage"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabConstruction",
                EventData.of(DialogEventData.ACTION_KEY, "tab_construction"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabConstructionInactive",
                EventData.of(DialogEventData.ACTION_KEY, "tab_construction"), false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmButton",
                EventData.of(DialogEventData.ACTION_KEY, "confirm"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DepositButton",
                EventData.of(DialogEventData.ACTION_KEY, "deposit"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#StartBuildButton",
                EventData.of(DialogEventData.ACTION_KEY, "start_build"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of(DialogEventData.ACTION_KEY, "close"), false);
    }

    // ========== Render ==========

    private void populate(UICommandBuilder b, BuildingRecord record) {
        if (!confirmed) {
            showConfirmPhase(b);
            return;
        }
        showPostConfirm(b, record);
    }

    private void showConfirmPhase(UICommandBuilder b) {
        b.set("#WarehouseSubtitle.Text", "Ghost preview");
        b.set("#PanelConfirm.Visible", true);
        b.set("#PanelInfo.Visible", false);
        b.set("#PanelStorage.Visible", false);
        b.set("#PanelConstruction.Visible", false);
        b.set("#TabInfo.Visible", false);
        b.set("#TabInfoInactive.Visible", false);
        b.set("#TabStorage.Visible", false);
        b.set("#TabStorageInactive.Visible", false);
        b.set("#TabConstruction.Visible", false);
        b.set("#TabConstructionInactive.Visible", false);
        b.set("#ConfirmInfo.Text",
                "A preview of the warehouse is shown.\n" +
                "Position the counter where you want it, " +
                "then confirm to designate this as a building site.");
    }

    private void showPostConfirm(UICommandBuilder b, BuildingRecord record) {
        b.set("#PanelConfirm.Visible", false);
        setTabActive(b, activeTab);
        b.set("#PanelInfo.Visible", "info".equals(activeTab));
        b.set("#PanelStorage.Visible", "storage".equals(activeTab));
        b.set("#PanelConstruction.Visible", "construction".equals(activeTab));

        String subtitle = (record != null && record.isCompleted()) ? "Built" : "Planned";
        b.set("#WarehouseSubtitle.Text", subtitle);

        populateInfoTab(b, record);
        if ("storage".equals(activeTab)) {
            populateStorageTab(b, record);
        }
        populateConstructionTab(b, record);
    }

    private void setTabActive(UICommandBuilder b, String tab) {
        b.set("#TabInfo.Visible", "info".equals(tab));
        b.set("#TabInfoInactive.Visible", !"info".equals(tab));
        b.set("#TabStorage.Visible", "storage".equals(tab));
        b.set("#TabStorageInactive.Visible", !"storage".equals(tab));
        b.set("#TabConstruction.Visible", "construction".equals(tab));
        b.set("#TabConstructionInactive.Visible", !"construction".equals(tab));
    }

    private void populateInfoTab(UICommandBuilder b, BuildingRecord record) {
        if (record == null || !record.isCompleted()) {
            b.set("#WarehouseStatusLabel.Text", "Not built yet.");
            b.set("#WarehouseStatusDetail.Text", "Build the warehouse first.");
            return;
        }
        b.set("#WarehouseStatusLabel.Text", "Operational");
        b.set("#WarehouseStatusDetail.Text", "The warehouse is storing your village's resources.");
    }

    private void populateStorageTab(UICommandBuilder b, BuildingRecord record) {
        Map<String, Integer> contents = StorageChestReader.readAll(world, record);

        b.clear("#StorageListContainer");

        if (contents.isEmpty()) {
            b.appendInline("#StorageListContainer",
                    "Label { Anchor: (Full: 0); Style: (HorizontalAlignment: Center, VerticalAlignment: Center, " +
                    "TextColor: #3e5060, FontSize: 11); Text: \"No items in storage\"; }");
            return;
        }

        String storageLanguage = networkPlayerRef != null ? networkPlayerRef.getLanguage() : null;
        dev.hearthbound.util.ResourceListRenderer.renderInventory(
                b, "#StorageListContainer", contents, storageLanguage);
    }

    private void populateConstructionTab(UICommandBuilder b, BuildingRecord record) {
        boolean isBuilding = BuildingSystem.get().isBuilding()
                && record != null
                && BuildingType.WAREHOUSE.equals(BuildingSystem.get().getActiveRecord() != null
                        ? BuildingSystem.get().getActiveRecord().getType() : "");

        Map<String, Integer> required = BuildingSystem.getRequiredResources(BuildingType.WAREHOUSE);
        Map<String, Integer> have = buildingStorageMap(record);

        boolean allSatisfied = renderResourceList(b, required, have);
        boolean isCompleted = record != null && record.isCompleted();
        boolean elfBusy = !isBuilding && BuildingSystem.get().isBuilding();
        applyConstructionState(b, isBuilding, isCompleted, allSatisfied, elfBusy);
    }

    private boolean renderResourceList(UICommandBuilder b,
                                        Map<String, Integer> required,
                                        Map<String, Integer> have) {
        String language = networkPlayerRef != null ? networkPlayerRef.getLanguage() : null;
        return dev.hearthbound.util.ResourceListRenderer.renderRequired(
                b, "#ResourceListContainer", required, have, language);
    }

    private void applyConstructionState(UICommandBuilder b, boolean isBuilding,
                                         boolean isCompleted, boolean allSatisfied, boolean elfBusy) {
        if (isCompleted) {
            b.set("#ConstructionStatus.Text",
                    "The Warehouse stands complete. Upgrades coming in a future update.");
            b.set("#ResourceListContainer.Visible", false);
            b.set("#StartBuildButton.Visible", false);
            b.set("#DepositButton.Visible", false);
            b.set("#BuildProgressLabel.Visible", false);
            b.set("#DepositHint.Text", "");
            return;
        }
        b.set("#ResourceListContainer.Visible", true);

        if (isBuilding) {
            b.set("#ConstructionStatus.Text", "Under construction...");
            b.set("#StartBuildButton.Visible", false);
            b.set("#DepositButton.Visible", true);
            b.set("#BuildProgressLabel.Visible", true);
            b.set("#BuildProgressLabel.Text",
                    "Progress: " + BuildingSystem.get().getBuildProgress() + "%" +
                    (BuildingSystem.get().isPaused() ? " (waiting for resources)" : ""));
            b.set("#DepositHint.Text", "Deposit more resources if construction pauses.");
            return;
        }

        b.set("#BuildProgressLabel.Visible", false);

        if (elfBusy) {
            BuildingRecord activeRecord = BuildingSystem.get().getActiveRecord();
            String busyName = activeRecord != null ? BuildingType.getDisplayName(activeRecord.getType()) : "another building";
            String elfName = dev.hearthbound.npc.ElfSage.resolveElfName();
            b.set("#ConstructionStatus.Text", elfName + " is busy building " + busyName + ".");
            b.set("#DepositButton.Visible", !allSatisfied);
            b.set("#StartBuildButton.Visible", allSatisfied);
            b.set("#DepositHint.Text", allSatisfied
                    ? "Waiting for " + elfName + " to finish."
                    : "You can deposit resources in advance.");
            return;
        }

        if (allSatisfied) {
            b.set("#ConstructionStatus.Text", "All resources gathered — ready to build.");
            b.set("#StartBuildButton.Visible", true);
            b.set("#DepositButton.Visible", false);
            b.set("#DepositHint.Text", "Press Start Construction to begin.");
        } else {
            b.set("#ConstructionStatus.Text", "Gather resources to begin construction.");
            b.set("#StartBuildButton.Visible", false);
            b.set("#DepositButton.Visible", true);
            b.set("#DepositHint.Text", "Click Deposit to transfer matching resources from your inventory.");
        }
    }

    // ========== Event handling ==========

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, DialogEventData data) {
        String action = data.getAction();

        switch (action) {
            case "close" -> close();

            case "tab_info" -> {
                if (!confirmed) { sendUpdate(new UICommandBuilder(), false); return; }
                activeTab = "info";
                UICommandBuilder b = new UICommandBuilder();
                b.set("#PanelInfo.Visible", true);
                b.set("#PanelStorage.Visible", false);
                b.set("#PanelConstruction.Visible", false);
                setTabActive(b, "info");
                populateInfoTab(b, findRecord(store));
                sendUpdate(b, false);
            }

            case "tab_storage" -> {
                if (!confirmed) { sendUpdate(new UICommandBuilder(), false); return; }
                activeTab = "storage";
                UICommandBuilder b = new UICommandBuilder();
                b.set("#PanelInfo.Visible", false);
                b.set("#PanelStorage.Visible", true);
                b.set("#PanelConstruction.Visible", false);
                setTabActive(b, "storage");
                populateStorageTab(b, findRecord(store));
                sendUpdate(b, false);
            }

            case "tab_construction" -> {
                if (!confirmed) { sendUpdate(new UICommandBuilder(), false); return; }
                activeTab = "construction";
                UICommandBuilder b = new UICommandBuilder();
                b.set("#PanelInfo.Visible", false);
                b.set("#PanelStorage.Visible", false);
                b.set("#PanelConstruction.Visible", true);
                setTabActive(b, "construction");
                populateConstructionTab(b, findRecord(store));
                sendUpdate(b, false);
            }

            case "confirm" -> {
                if (confirmed) { sendUpdate(new UICommandBuilder(), false); return; }

                VillageData village = VillageManager.get().getOrCreateVillageData(store, playerEntityRef);
                BuildingRecord record = new BuildingRecord(BuildingType.WAREHOUSE, counterX, counterY, counterZ);
                record.setRotation(BuildingSystem.get().getActiveRotation(store, playerEntityRef));
                village.addBuilding(record);
                VillageManager.get().save(store, playerEntityRef, village);

                BuildingSystem.get().clearGhostPreview(store, playerEntityRef, world);

                confirmed = true;
                activeTab = "construction";

                UICommandBuilder b = new UICommandBuilder();
                showPostConfirm(b, record);
                sendUpdate(b, false);
            }

            case "deposit" -> {
                if (!confirmed) { sendUpdate(new UICommandBuilder(), false); return; }

                VillageData dv = VillageManager.get().getVillageData(store, playerEntityRef);
                if (dv == null) { sendUpdate(new UICommandBuilder(), false); return; }

                BuildingRecord record = findRecord(store);
                if (record == null) { sendUpdate(new UICommandBuilder(), false); return; }

                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) { sendUpdate(new UICommandBuilder(), false); return; }

                Map<String, Integer> required = BuildingSystem.get().isBuilding()
                        && BuildingType.WAREHOUSE.equals(BuildingSystem.get().getActiveRecord() != null
                                ? BuildingSystem.get().getActiveRecord().getType() : "")
                        ? BuildingSystem.get().getRemainingResources()
                        : BuildingSystem.getRequiredResources(BuildingType.WAREHOUSE);
                if (required == null) required = BuildingSystem.getRequiredResources(BuildingType.WAREHOUSE);

                com.hypixel.hytale.protocol.GameMode gm = player.getGameMode();
                boolean isCreative = gm != null && "Creative".equals(gm.name());
                int deposited = isCreative
                        ? depositCreative(record, required)
                        : depositFromInventory(player, record, required);

                VillageManager.get().save(store, playerEntityRef, dv);

                UICommandBuilder b = new UICommandBuilder();
                b.set("#DepositHint.Text", isCreative
                        ? (deposited > 0 ? "Creative: " + deposited + " items conjured." : "No resources needed.")
                        : (deposited > 0 ? "Deposited " + deposited + " items." : "No matching resources in inventory."));
                populateConstructionTab(b, record);
                sendUpdate(b, false);
            }

            case "start_build" -> {
                if (!confirmed) { sendUpdate(new UICommandBuilder(), false); return; }

                VillageData village = VillageManager.get().getVillageData(store, playerEntityRef);
                if (village == null) { sendUpdate(new UICommandBuilder(), false); return; }

                BuildingRecord record = findRecord(store);
                if (record == null) { sendUpdate(new UICommandBuilder(), false); return; }

                VillageManager.get().save(store, playerEntityRef, village);

                BuildingSystem.get().startResourceBuilding(
                        store, playerEntityRef, world, record, record.getRotation(), ownerUuid);

                UICommandBuilder b = new UICommandBuilder();
                populateConstructionTab(b, record);
                sendUpdate(b, false);
            }
        }
    }

    // ========== Helpers ==========

    private BuildingRecord findRecord(Store<EntityStore> store) {
        VillageData village = VillageManager.get().getVillageData(store, playerEntityRef);
        if (village == null) return null;
        for (BuildingRecord b : village.getBuildings()) {
            if (BuildingType.WAREHOUSE.equals(b.getType())
                    && b.getPosX() == counterX
                    && b.getPosY() == counterY
                    && b.getPosZ() == counterZ) {
                return b;
            }
        }
        return null;
    }

    private static Map<String, Integer> buildingStorageMap(BuildingRecord record) {
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        if (record == null) return out;
        for (var entry : record.getStorage().object2IntEntrySet()) {
            out.put(entry.getKey(), entry.getIntValue());
        }
        return out;
    }

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

    private int depositFromInventory(Player player, BuildingRecord target, Map<String, Integer> required) {
        int totalDeposited = 0;
        try {
            ItemContainer playerInv = player.getInventory().getCombinedHotbarFirst();
            for (Map.Entry<String, Integer> entry : required.entrySet()) {
                String itemId = entry.getKey();
                int deficit = entry.getValue() - target.getResourceCount(itemId);
                if (deficit <= 0) continue;
                for (int i = 0; i < deficit; i++) {
                    var removeTx = playerInv.removeItemStack(new ItemStack(itemId, 1), true, true);
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
}
