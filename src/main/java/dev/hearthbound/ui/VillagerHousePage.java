package dev.hearthbound.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.math.vector.Vector3d;
import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.NpcRespawner;
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
 * House management UI — opened when the player presses F on the Brazier anchor block.
 *
 * Two phases:
 * - Pre-confirm: ghost is showing, player hits "Confirm Placement" to lock in the site.
 * - Post-confirm: Residents tab (who lives here) + Construction tab (resources + build).
 */
public class VillagerHousePage extends InteractiveCustomUIPage<DialogEventData> {

    private static final Logger LOGGER = Logger.getLogger(VillagerHousePage.class.getName());

    private final Ref<EntityStore> playerRef;
    private final PlayerRef networkPlayerRef;
    private final UUID ownerUuid;
    private final World world;
    private final int brazierX, brazierY, brazierZ;

    private boolean confirmed;
    private String activeTab = "residents";

    public VillagerHousePage(PlayerRef player, Ref<EntityStore> playerRef, World world,
                              int brazierX, int brazierY, int brazierZ) {
        super(player, CustomPageLifetime.CanDismissOrCloseThroughInteraction, DialogEventData.CODEC);
        this.playerRef = playerRef;
        this.networkPlayerRef = player;
        this.ownerUuid = player.getUuid();
        this.world = world;
        this.brazierX = brazierX;
        this.brazierY = brazierY;
        this.brazierZ = brazierZ;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder events, Store<EntityStore> store) {
        builder.append("VillagerHouse.ui");

        BuildingRecord record = findHouseRecord(store);
        confirmed = record != null;

        populate(builder, store, record);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabResidents",
                EventData.of(DialogEventData.ACTION_KEY, "tab_residents"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabResidentsInactive",
                EventData.of(DialogEventData.ACTION_KEY, "tab_residents"), false);
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
        events.addEventBinding(CustomUIEventBindingType.Activating, "#RecallButton",
                EventData.of(DialogEventData.ACTION_KEY, "recall"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#VariantPrev",
                EventData.of(DialogEventData.ACTION_KEY, "variant_prev"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#VariantNext",
                EventData.of(DialogEventData.ACTION_KEY, "variant_next"), false);
    }

    // ========== Render ==========

    private void populate(UICommandBuilder b, Store<EntityStore> store, BuildingRecord record) {
        if (!confirmed) {
            showConfirmPhase(b, store);
            return;
        }
        showPostConfirm(b, store, record);
    }

    private void showConfirmPhase(UICommandBuilder b, Store<EntityStore> store) {
        b.set("#HouseSubtitle.Text", "Ghost preview");
        b.set("#PanelConfirm.Visible", true);
        b.set("#PanelResidents.Visible", false);
        b.set("#PanelConstruction.Visible", false);
        b.set("#TabResidents.Visible", false);
        b.set("#TabResidentsInactive.Visible", false);
        b.set("#TabConstruction.Visible", false);
        b.set("#TabConstructionInactive.Visible", false);
        b.set("#ConfirmInfo.Text",
                "A preview of the house is shown.\n" +
                "Place the brazier where you want the fireplace to be inside the house, " +
                "then confirm to designate this as a building site.");
        renderVariantSwitcher(b, store);
    }

    /** Updates the variant switcher label to match the player's currently-active preview. */
    private void renderVariantSwitcher(UICommandBuilder b, Store<EntityStore> store) {
        int variant = BuildingSystem.get().getActiveVariant(store, playerRef);
        b.set("#VariantName.Text", BuildingType.getHouseVariantName(variant)
                + " (" + (variant + 1) + " / " + BuildingType.getHouseVariantCount() + ")");
    }

    private void showPostConfirm(UICommandBuilder b, Store<EntityStore> store, BuildingRecord record) {
        b.set("#PanelConfirm.Visible", false);
        setTabActive(b, activeTab);
        b.set("#PanelResidents.Visible", "residents".equals(activeTab));
        b.set("#PanelConstruction.Visible", "construction".equals(activeTab));

        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        String subtitle = (record != null && record.isCompleted()) ? "Built" : "Planned";
        b.set("#HouseSubtitle.Text", subtitle);

        populateResidentsTab(b, store, village, record);
        populateConstructionTab(b, record);
    }

    private void populateResidentsTab(UICommandBuilder b, Store<EntityStore> store,
                                       VillageData village, BuildingRecord record) {
        if (record == null || !record.isCompleted()) {
            b.set("#ResidentName.Text", "No one yet");
            b.set("#ResidentStatus.Text", "Build the house first.");
            b.set("#RecallButton.Visible", false);
            return;
        }

        UUID assignedId = record.getAssignedVillagerId();
        if (assignedId == null) {
            b.set("#ResidentName.Text", "Unoccupied");
            b.set("#ResidentStatus.Text", "Waiting for a villager to move in.");
            b.set("#RecallButton.Visible", false);
            return;
        }

        String name = "Unknown";
        if (village != null) {
            for (VillagerSummary v : village.getVillagers()) {
                if (assignedId.equals(v.getVillagerUuid())) {
                    name = v.getFullName();
                    break;
                }
            }
        }
        b.set("#ResidentName.Text", name);
        b.set("#ResidentStatus.Text", "Resident");
        b.set("#RecallButton.Visible", true);
    }

    private void populateConstructionTab(UICommandBuilder b, BuildingRecord record) {
        BuildingRecord active = BuildingSystem.get().getActiveRecord();
        boolean isBuilding = BuildingSystem.get().isBuilding()
                && record != null && record == active;
        boolean elfBusy = BuildingSystem.get().isBuilding() && !isBuilding;

        int variant = record != null ? record.getVariant() : 0;
        Map<String, Integer> required = BuildingSystem.getRequiredResources(BuildingType.HOUSE_HUMAN, variant);
        Map<String, Integer> have = readStorage(record);

        boolean allSatisfied = renderResourceList(b, required, have);

        boolean isCompleted = record != null && record.isCompleted();
        applyConstructionState(b, isBuilding, isCompleted, allSatisfied, elfBusy);
    }

    private void setTabActive(UICommandBuilder b, String tab) {
        boolean residentsActive = "residents".equals(tab);
        b.set("#TabResidents.Visible", residentsActive);
        b.set("#TabResidentsInactive.Visible", !residentsActive);
        b.set("#TabConstruction.Visible", !residentsActive);
        b.set("#TabConstructionInactive.Visible", residentsActive);
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

            case "tab_residents" -> {
                if (!confirmed) { UICommandBuilder b = new UICommandBuilder(); sendUpdate(b, false); return; }
                activeTab = "residents";
                UICommandBuilder b = new UICommandBuilder();
                b.set("#PanelResidents.Visible", true);
                b.set("#PanelConstruction.Visible", false);
                setTabActive(b, "residents");
                sendUpdate(b, false);
            }

            case "tab_construction" -> {
                if (!confirmed) { UICommandBuilder b = new UICommandBuilder(); sendUpdate(b, false); return; }
                activeTab = "construction";
                UICommandBuilder b = new UICommandBuilder();
                b.set("#PanelResidents.Visible", false);
                b.set("#PanelConstruction.Visible", true);
                setTabActive(b, "construction");
                BuildingRecord record = findHouseRecord(store);
                populateConstructionTab(b, record);
                sendUpdate(b, false);
            }

            case "confirm" -> {
                if (confirmed) { UICommandBuilder b = new UICommandBuilder(); sendUpdate(b, false); return; }

                VillageData village = VillageManager.get().getOrCreateVillageData(store, playerRef);
                BuildingRecord record = new BuildingRecord(BuildingType.HOUSE_HUMAN, brazierX, brazierY, brazierZ);
                record.setRotation(BuildingSystem.get().getActiveRotation(store, playerRef));
                // Pin the chosen variant to the record so construction always uses the prefab
                // the player saw in the ghost preview, even if they later cycle the global
                // selectedHouseVariant for the next house.
                int chosenVariant = BuildingSystem.get().getActiveVariant(store, playerRef);
                record.setVariant(chosenVariant);
                village.addBuilding(record);
                // Persist the choice so re-placing a brazier later shows the same variant.
                village.setSelectedHouseVariant(chosenVariant);
                VillageManager.get().save(store, playerRef, village);

                // Clear the ghost preview now that site is locked
                BuildingSystem.get().clearGhostPreview(store, playerRef, world);

                confirmed = true;
                activeTab = "construction";

                UICommandBuilder b = new UICommandBuilder();
                showPostConfirm(b, store, record);
                sendUpdate(b, false);
            }

            case "variant_prev" -> handleVariantCycle(store, -1);
            case "variant_next" -> handleVariantCycle(store, +1);

            case "deposit" -> {
                if (!confirmed) { UICommandBuilder b = new UICommandBuilder(); sendUpdate(b, false); return; }

                VillageData dv = VillageManager.get().getVillageData(store, playerRef);
                if (dv == null) { UICommandBuilder b = new UICommandBuilder(); sendUpdate(b, false); return; }

                BuildingRecord record = findHouseRecord(store);
                if (record == null) { UICommandBuilder b = new UICommandBuilder(); sendUpdate(b, false); return; }

                Player player = store.getComponent(ref, Player.getComponentType());
                if (player == null) { UICommandBuilder b = new UICommandBuilder(); sendUpdate(b, false); return; }

                int recordVariant = record.getVariant();
                Map<String, Integer> required = BuildingSystem.get().isBuilding()
                        && BuildingType.HOUSE_HUMAN.equals(BuildingSystem.get().getActiveRecord() != null
                                ? BuildingSystem.get().getActiveRecord().getType() : "")
                        ? BuildingSystem.get().getRemainingResources()
                        : BuildingSystem.getRequiredResources(BuildingType.HOUSE_HUMAN, recordVariant);
                if (required == null) required = BuildingSystem.getRequiredResources(BuildingType.HOUSE_HUMAN, recordVariant);

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

            case "recall" -> {
                if (!confirmed) { sendUpdate(new UICommandBuilder(), false); return; }
                BuildingRecord record = findHouseRecord(store);
                if (record == null || !record.isCompleted()) { sendUpdate(new UICommandBuilder(), false); return; }
                UUID assignedId = record.getAssignedVillagerId();
                if (assignedId == null) { sendUpdate(new UICommandBuilder(), false); return; }

                recallVillager(assignedId, record);

                UICommandBuilder b = new UICommandBuilder();
                b.set("#ResidentStatus.Text", "Recalled to house.");
                sendUpdate(b, false);
            }

            case "start_build" -> {
                if (!confirmed) { UICommandBuilder b = new UICommandBuilder(); sendUpdate(b, false); return; }
                if (BuildingSystem.get().isBuilding()) { UICommandBuilder b = new UICommandBuilder(); sendUpdate(b, false); return; }

                VillageData village = VillageManager.get().getVillageData(store, playerRef);
                if (village == null) { UICommandBuilder b = new UICommandBuilder(); sendUpdate(b, false); return; }

                BuildingRecord record = findHouseRecord(store);
                if (record == null) { UICommandBuilder b = new UICommandBuilder(); sendUpdate(b, false); return; }

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

    /**
     * Cycles the active ghost preview to the next/prev house variant, persists the choice
     * in VillageData (so re-placing brazier later remembers it), and refreshes the UI label.
     */
    private void handleVariantCycle(Store<EntityStore> store, int delta) {
        if (confirmed) {
            // Variant is locked once the placement is confirmed — silently ignore.
            sendUpdate(new UICommandBuilder(), false);
            return;
        }
        int newVariant = BuildingSystem.get().cycleHouseVariant(store, playerRef, world, delta);
        if (newVariant < 0) {
            // Preview was somehow gone — ignore.
            sendUpdate(new UICommandBuilder(), false);
            return;
        }

        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village != null) {
            village.setSelectedHouseVariant(newVariant);
            VillageManager.get().save(store, playerRef, village);
        }

        UICommandBuilder b = new UICommandBuilder();
        b.set("#VariantName.Text", BuildingType.getHouseVariantName(newVariant)
                + " (" + (newVariant + 1) + " / " + BuildingType.getHouseVariantCount() + ")");
        sendUpdate(b, false);
    }

    /** Finds the house_human BuildingRecord anchored at the brazier's position. */
    private BuildingRecord findHouseRecord(Store<EntityStore> store) {
        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null) return null;
        for (BuildingRecord b : village.getBuildings()) {
            if (BuildingType.HOUSE_HUMAN.equals(b.getType())
                    && b.getPosX() == brazierX
                    && b.getPosY() == brazierY
                    && b.getPosZ() == brazierZ) {
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

    private void recallVillager(UUID villagerUuid, BuildingRecord house) {
        // Stand one block in front of the brazier, inside the house. Brazier is always
        // placed with its open face toward the interior, so its world rotation tells us
        // which direction is "into the room".
        double[] stand = BuildingType.getInteriorStandPoint(
                house.getPosX(), house.getPosY(), house.getPosZ(), house.getRotation());
        double standX = stand[0], standY = stand[1], standZ = stand[2];
        LOGGER.info("recallVillager: brazier=(" + house.getPosX() + "," + house.getPosY() + "," + house.getPosZ()
                + ") rotation=" + house.getRotation() + " → stand=(" + standX + "," + standY + "," + standZ + ")");

        Entity entity = world.getEntity(villagerUuid);
        if (entity != null) {
            doMoveTo(villagerUuid, standX, standY, standZ);
            return;
        }

        // Entity not in any loaded chunk. Force-load the last-known chunk, then
        // either moveTo (entity returned) or respawn-on-the-spot (entity gone).
        NpcRegistry.NpcRecord record = NpcRegistry.get().getRecord(villagerUuid);
        if (record == null) {
            LOGGER.warning("recallVillager: no NpcRecord for uuid=" + villagerUuid);
            return;
        }
        world.getChunkAsync(record.chunkIndex).thenRun(() -> world.execute(() -> {
            // Give the chunk ~1.5s to deserialise its entities before checking.
            dev.hearthbound.util.TickScheduler.runLater(world, 1500L, () -> {
                Entity reloaded = world.getEntity(villagerUuid);
                if (reloaded != null) {
                    doMoveTo(villagerUuid, standX, standY, standZ);
                    return;
                }
                // Entity is gone — respawn at the recall point. The new entity gets a
                // new UUID; NpcRespawner rewrites the village data so the resident is
                // still tied to this house.
                LOGGER.warning("recallVillager: entity not in chunk after force-load — respawning at brazier");
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                NpcRegistry.NpcRecord liveRecord = NpcRegistry.get().getRecord(villagerUuid);
                if (liveRecord == null) return;
                NpcRespawner.respawn(world, liveStore, playerRef, liveRecord,
                                new Vector3d(standX, standY, standZ))
                        .thenAccept(result -> {
                            if (!result.ok) {
                                LOGGER.warning("recallVillager: respawn failed: " + result.reason);
                                NpcRegistry.get().recordRespawnFailure(villagerUuid);
                            }
                        });
            });
        }));
    }

    private void doMoveTo(UUID villagerUuid, double x, double y, double z) {
        Store<EntityStore> liveStore = world.getEntityStore().getStore();
        Entity entity = world.getEntity(villagerUuid);
        if (entity == null) return;
        Ref<EntityStore> ref = world.getEntityRef(villagerUuid);
        if (ref == null || !ref.isValid()) return;
        entity.moveTo(ref, x, y, z, liveStore);
        NPCEntity npcEntity = liveStore.getComponent(ref, NPCEntity.getComponentType());
        if (npcEntity != null) {
            npcEntity.setLeashPoint(new Vector3d(x, y, z));
        }
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
