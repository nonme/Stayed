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
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerSummary;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Farm management UI — opened when the player presses F on the Scarecrow anchor block.
 *
 * Two phases:
 * - Pre-confirm: ghost is showing, player hits "Confirm Placement" to lock in the site.
 * - Post-confirm: Info tab (farm status) + Construction tab (resources + build).
 */
public class FarmPage extends InteractiveCustomUIPage<DialogEventData> {

    private static final Logger LOGGER = Logger.getLogger(FarmPage.class.getName());

    private final Ref<EntityStore> playerRef;
    private final PlayerRef networkPlayerRef;
    private final UUID ownerUuid;
    private final World world;
    private final int scarecrowX, scarecrowY, scarecrowZ;

    private boolean confirmed;
    private String activeTab = "info";

    public FarmPage(PlayerRef player, Ref<EntityStore> playerRef, World world,
                    int scarecrowX, int scarecrowY, int scarecrowZ) {
        super(player, CustomPageLifetime.CanDismissOrCloseThroughInteraction, DialogEventData.CODEC);
        this.playerRef = playerRef;
        this.networkPlayerRef = player;
        this.ownerUuid = player.getUuid();
        this.world = world;
        this.scarecrowX = scarecrowX;
        this.scarecrowY = scarecrowY;
        this.scarecrowZ = scarecrowZ;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder events, Store<EntityStore> store) {
        builder.append("Farm.ui");

        BuildingRecord record = findFarmRecord(store);
        confirmed = record != null;

        populate(builder, store, record);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabInfo",
                EventData.of(DialogEventData.ACTION_KEY, "tab_info"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabInfoInactive",
                EventData.of(DialogEventData.ACTION_KEY, "tab_info"), false);
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

    private void populate(UICommandBuilder b, Store<EntityStore> store, BuildingRecord record) {
        if (!confirmed) {
            showConfirmPhase(b);
            return;
        }
        showPostConfirm(b, store, record);
    }

    private void showConfirmPhase(UICommandBuilder b) {
        b.set("#FarmSubtitle.Text", "Ghost preview");
        b.set("#PanelConfirm.Visible", true);
        b.set("#PanelInfo.Visible", false);
        b.set("#PanelConstruction.Visible", false);
        b.set("#TabInfo.Visible", false);
        b.set("#TabInfoInactive.Visible", false);
        b.set("#TabConstruction.Visible", false);
        b.set("#TabConstructionInactive.Visible", false);
        b.set("#ConfirmInfo.Text",
                "A preview of the farm is shown.\n" +
                "Place the scarecrow where you want it inside the farm, " +
                "then confirm to designate this as a building site.");
    }

    private void showPostConfirm(UICommandBuilder b, Store<EntityStore> store, BuildingRecord record) {
        b.set("#PanelConfirm.Visible", false);
        setTabActive(b, activeTab);
        b.set("#PanelInfo.Visible", "info".equals(activeTab));
        b.set("#PanelConstruction.Visible", "construction".equals(activeTab));

        String subtitle = (record != null && record.isCompleted()) ? "Built" : "Planned";
        b.set("#FarmSubtitle.Text", subtitle);

        populateInfoTab(b, store, record);
        populateConstructionTab(b, record);
    }

    private void populateInfoTab(UICommandBuilder b, Store<EntityStore> store, BuildingRecord record) {
        if (record == null || !record.isCompleted()) {
            b.set("#FarmStatusLabel.Text", "Not built yet.");
            b.set("#FarmStatusDetail.Text", "Build the farm first.");
            b.set("#FarmWorkerLabel.Text", "—");
            return;
        }

        b.set("#FarmStatusLabel.Text", "Operational");

        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        UUID workerUuid = record.getAssignedVillagerId();
        VillagerSummary worker = (village != null && workerUuid != null)
                ? VillageManager.get().findVillagerSummary(village, workerUuid) : null;

        if (worker != null) {
            b.set("#FarmWorkerLabel.Text", worker.getFullName());
            b.set("#FarmStatusDetail.Text", "Working the fields.");
        } else {
            b.set("#FarmWorkerLabel.Text", "No one assigned yet");
            b.set("#FarmStatusDetail.Text", "A villager will be assigned once they have a home.");
        }
    }

    private void setTabActive(UICommandBuilder b, String tab) {
        boolean infoActive = "info".equals(tab);
        b.set("#TabInfo.Visible", infoActive);
        b.set("#TabInfoInactive.Visible", !infoActive);
        b.set("#TabConstruction.Visible", !infoActive);
        b.set("#TabConstructionInactive.Visible", infoActive);
    }

    private void populateConstructionTab(UICommandBuilder b, BuildingRecord record) {
        boolean isBuilding = BuildingSystem.get().isBuilding()
                && record != null
                && BuildingType.FARM.equals(BuildingSystem.get().getActiveRecord() != null
                        ? BuildingSystem.get().getActiveRecord().getType() : "");
        boolean elfBusy = !isBuilding && BuildingSystem.get().isBuilding();

        Map<String, Integer> required = BuildingSystem.getRequiredResources(BuildingType.FARM);
        Map<String, Integer> have = readStorage(record);

        boolean allSatisfied = renderResourceList(b, required, have);
        boolean isCompleted = record != null && record.isCompleted();
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
                    "The Farm stands complete. Upgrades coming in a future update.");
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
                b.set("#PanelConstruction.Visible", false);
                setTabActive(b, "info");
                BuildingRecord record = findFarmRecord(store);
                populateInfoTab(b, store, record);
                sendUpdate(b, false);
            }

            case "tab_construction" -> {
                if (!confirmed) { sendUpdate(new UICommandBuilder(), false); return; }
                activeTab = "construction";
                UICommandBuilder b = new UICommandBuilder();
                b.set("#PanelInfo.Visible", false);
                b.set("#PanelConstruction.Visible", true);
                setTabActive(b, "construction");
                BuildingRecord record = findFarmRecord(store);
                populateConstructionTab(b, record);
                sendUpdate(b, false);
            }

            case "confirm" -> {
                if (confirmed) { sendUpdate(new UICommandBuilder(), false); return; }

                VillageData village = VillageManager.get().getOrCreateVillageData(store, playerRef);
                BuildingRecord record = new BuildingRecord(BuildingType.FARM, scarecrowX, scarecrowY, scarecrowZ);
                record.setRotation(BuildingSystem.get().getActiveRotation(store, playerRef));
                village.addBuilding(record);
                VillageManager.get().save(store, playerRef, village);

                BuildingSystem.get().clearGhostPreview(store, playerRef, world);

                confirmed = true;
                activeTab = "construction";

                UICommandBuilder b = new UICommandBuilder();
                showPostConfirm(b, store, record);
                sendUpdate(b, false);
            }

            case "deposit" -> {
                if (!confirmed) { sendUpdate(new UICommandBuilder(), false); return; }

                VillageData dv = VillageManager.get().getVillageData(store, playerRef);
                if (dv == null) { sendUpdate(new UICommandBuilder(), false); return; }

                BuildingRecord record = findFarmRecord(store);
                if (record == null) { sendUpdate(new UICommandBuilder(), false); return; }

                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) { sendUpdate(new UICommandBuilder(), false); return; }

                Map<String, Integer> required = BuildingSystem.get().isBuilding()
                        && BuildingType.FARM.equals(BuildingSystem.get().getActiveRecord() != null
                                ? BuildingSystem.get().getActiveRecord().getType() : "")
                        ? BuildingSystem.get().getRemainingResources()
                        : BuildingSystem.getRequiredResources(BuildingType.FARM);
                if (required == null) required = BuildingSystem.getRequiredResources(BuildingType.FARM);

                com.hypixel.hytale.protocol.GameMode gm = player.getGameMode();
                boolean isCreative = gm != null && "Creative".equals(gm.name());
                int deposited = isCreative
                        ? depositCreative(record, required)
                        : depositFromInventory(player, record, required);

                VillageManager.get().save(store, playerRef, dv);

                UICommandBuilder b = new UICommandBuilder();
                b.set("#DepositHint.Text", isCreative
                        ? (deposited > 0 ? "Creative: " + deposited + " items conjured." : "No resources needed.")
                        : (deposited > 0 ? "Deposited " + deposited + " items." : "No matching resources in inventory."));
                populateConstructionTab(b, record);
                sendUpdate(b, false);
            }

            case "start_build" -> {
                if (!confirmed) { sendUpdate(new UICommandBuilder(), false); return; }

                VillageData village = VillageManager.get().getVillageData(store, playerRef);
                if (village == null) { sendUpdate(new UICommandBuilder(), false); return; }

                BuildingRecord record = findFarmRecord(store);
                if (record == null) { sendUpdate(new UICommandBuilder(), false); return; }

                VillageManager.get().save(store, playerRef, village);

                BuildingSystem.get().startResourceBuilding(
                        store, playerRef, world, record, record.getRotation(), ownerUuid);

                UICommandBuilder b = new UICommandBuilder();
                populateConstructionTab(b, record);
                sendUpdate(b, false);
            }
        }
    }

    // ========== Helpers ==========

    private BuildingRecord findFarmRecord(Store<EntityStore> store) {
        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null) return null;
        for (BuildingRecord b : village.getBuildings()) {
            if (BuildingType.FARM.equals(b.getType())
                    && b.getPosX() == scarecrowX
                    && b.getPosY() == scarecrowY
                    && b.getPosZ() == scarecrowZ) {
                return b;
            }
        }
        return null;
    }

    private static Map<String, Integer> readStorage(BuildingRecord record) {
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
