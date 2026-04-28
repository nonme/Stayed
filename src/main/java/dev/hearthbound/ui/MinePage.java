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
 * Mine management UI — opened when the player presses F on the Mine Torch anchor block.
 */
public class MinePage extends InteractiveCustomUIPage<DialogEventData> {

    private static final Logger LOGGER = Logger.getLogger(MinePage.class.getName());

    private final Ref<EntityStore> playerRef;
    private final UUID ownerUuid;
    private final World world;
    private final int anchorX, anchorY, anchorZ;

    private boolean confirmed;
    private String activeTab = "info";

    public MinePage(PlayerRef player, Ref<EntityStore> playerRef, World world,
                    int anchorX, int anchorY, int anchorZ) {
        super(player, CustomPageLifetime.CanDismissOrCloseThroughInteraction, DialogEventData.CODEC);
        this.playerRef = playerRef;
        this.ownerUuid = player.getUuid();
        this.world = world;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder events, Store<EntityStore> store) {
        builder.append("Mine.ui");

        BuildingRecord record = findRecord(store);
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
        b.set("#BuildingSubtitle.Text", "Ghost preview");
        b.set("#PanelConfirm.Visible", true);
        b.set("#PanelInfo.Visible", false);
        b.set("#PanelConstruction.Visible", false);
        b.set("#TabInfo.Visible", false);
        b.set("#TabInfoInactive.Visible", false);
        b.set("#TabConstruction.Visible", false);
        b.set("#TabConstructionInactive.Visible", false);
        b.set("#ConfirmInfo.Text",
                "A preview of the mine is shown.\n" +
                "Place the mine torch where you want it inside the building, " +
                "then confirm to designate this as a building site.");
    }

    private void showPostConfirm(UICommandBuilder b, Store<EntityStore> store, BuildingRecord record) {
        b.set("#PanelConfirm.Visible", false);
        setTabActive(b, activeTab);
        b.set("#PanelInfo.Visible", "info".equals(activeTab));
        b.set("#PanelConstruction.Visible", "construction".equals(activeTab));

        String subtitle = (record != null && record.isCompleted()) ? "Built" : "Planned";
        b.set("#BuildingSubtitle.Text", subtitle);

        populateInfoTab(b, store, record);
        populateConstructionTab(b, record);
    }

    private void populateInfoTab(UICommandBuilder b, Store<EntityStore> store, BuildingRecord record) {
        if (record == null || !record.isCompleted()) {
            b.set("#BuildingStatusLabel.Text", "Not built yet.");
            b.set("#BuildingStatusDetail.Text", "Build the mine first.");
            b.set("#BuildingWorkerLabel.Text", "—");
            return;
        }

        b.set("#BuildingStatusLabel.Text", "Operational");

        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        UUID workerUuid = record.getAssignedVillagerId();
        VillagerSummary worker = (village != null && workerUuid != null)
                ? VillageManager.get().findVillagerSummary(village, workerUuid) : null;

        if (worker != null) {
            b.set("#BuildingWorkerLabel.Text", worker.getFullName());
            b.set("#BuildingStatusDetail.Text", "Mining stone and ore.");
        } else {
            b.set("#BuildingWorkerLabel.Text", "No one assigned yet");
            b.set("#BuildingStatusDetail.Text", "A villager will be assigned once they have a home.");
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
                && BuildingType.MINE.equals(BuildingSystem.get().getActiveRecord() != null
                        ? BuildingSystem.get().getActiveRecord().getType() : "");

        Map<String, Integer> required = BuildingSystem.getRequiredResources(BuildingType.MINE);
        Map<String, Integer> have = readStorage(record);

        boolean allSatisfied = renderResourceList(b, required, have);
        boolean isCompleted = record != null && record.isCompleted();
        applyConstructionState(b, isBuilding, isCompleted, allSatisfied);
    }

    private boolean renderResourceList(UICommandBuilder b,
                                        Map<String, Integer> required,
                                        Map<String, Integer> have) {
        b.clear("#ResourceListContainer");
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

            b.appendInline("#ResourceListContainer",
                    "Group { LayoutMode: Left; Anchor: (Height: 32, Bottom: 2); Padding: (Horizontal: 4); " +
                    "  ItemIcon { Anchor: (Width: 24, Height: 24); } " +
                    "  Label { Anchor: (Left: 8); FlexWeight: 1; Style: (HorizontalAlignment: Start, VerticalAlignment: Center, TextColor: " + nameColor + ", FontSize: 11); Text: \"" + displayName + "\"; } " +
                    "  Label { Anchor: (Width: 60); Style: (HorizontalAlignment: End, VerticalAlignment: Center, TextColor: " + countColor + ", FontSize: 11, RenderBold: true); Text: \"" + got + " / " + need + "\"; } " +
                    "}");
            b.set("#ResourceListContainer[" + rowIndex + "][0].ItemId", itemId);
            rowIndex++;
        }
        return allSatisfied;
    }

    private void applyConstructionState(UICommandBuilder b, boolean isBuilding,
                                         boolean isCompleted, boolean allSatisfied) {
        if (isCompleted) {
            b.set("#ConstructionStatus.Text", "Complete!");
            b.set("#StartBuildButton.Visible", false);
            b.set("#DepositButton.Visible", false);
            b.set("#BuildProgressLabel.Visible", false);
            b.set("#DepositHint.Text", "");
            return;
        }

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
                BuildingRecord record = findRecord(store);
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
                BuildingRecord record = findRecord(store);
                populateConstructionTab(b, record);
                sendUpdate(b, false);
            }

            case "confirm" -> {
                if (confirmed) { sendUpdate(new UICommandBuilder(), false); return; }

                VillageData village = VillageManager.get().getOrCreateVillageData(store, playerRef);
                BuildingRecord record = new BuildingRecord(BuildingType.MINE, anchorX, anchorY, anchorZ);
                record.setRotation(BuildingSystem.get().getActiveRotation());
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

                BuildingRecord record = findRecord(store);
                if (record == null) { sendUpdate(new UICommandBuilder(), false); return; }

                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) { sendUpdate(new UICommandBuilder(), false); return; }

                Map<String, Integer> required = BuildingSystem.get().isBuilding()
                        && BuildingType.MINE.equals(BuildingSystem.get().getActiveRecord() != null
                                ? BuildingSystem.get().getActiveRecord().getType() : "")
                        ? BuildingSystem.get().getRemainingResources()
                        : BuildingSystem.getRequiredResources(BuildingType.MINE);
                if (required == null) required = BuildingSystem.getRequiredResources(BuildingType.MINE);

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

                BuildingRecord record = findRecord(store);
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

    private BuildingRecord findRecord(Store<EntityStore> store) {
        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null) return null;
        for (BuildingRecord b : village.getBuildings()) {
            if (BuildingType.MINE.equals(b.getType())
                    && b.getPosX() == anchorX
                    && b.getPosY() == anchorY
                    && b.getPosZ() == anchorZ) {
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
