package dev.hearthbound.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

/**
 * Persistent HUD showing village status in the top-left corner.
 * Only visible when the player has founded a village.
 */
public class VillageHud extends CustomUIHud {

    private final Ref<EntityStore> entityRef;

    public VillageHud(PlayerRef playerRef, Ref<EntityStore> entityRef) {
        super(playerRef);
        this.entityRef = entityRef;
    }

    @Override
    protected void build(UICommandBuilder builder) {
        builder.append("VillageHud.ui");
        builder.set("#Root.Visible", false);
    }

    /**
     * Update HUD content. Call periodically from VillageTickHandler.
     */
    public void refresh(Store<EntityStore> store) {
        if (!entityRef.isValid()) return;
        VillageData village;
        try {
            village = VillageManager.get().getVillageData(store, entityRef);
        } catch (IllegalStateException e) {
            // store/ref mismatch during world transition — skip this tick
            return;
        }

        UICommandBuilder builder = new UICommandBuilder();

        if (village == null || !village.isFounded()) {
            builder.set("#Root.Visible", false);
            update(false, builder);
            return;
        }

        builder.set("#Root.Visible", true);

        String name = village.getVillageName().isEmpty() ? "Village" : village.getVillageName();
        builder.set("#VillageName.Text", name);

        long completedBuildings = village.getBuildings().stream()
                .filter(b -> b.isCompleted())
                .count();
        builder.set("#VillagerCount.Text", "Buildings: " + completedBuildings);

        if (BuildingSystem.get().isBuilding()) {
            String active = BuildingSystem.get().getActiveRecord() != null
                    ? BuildingType.getDisplayName(BuildingSystem.get().getActiveRecord().getType())
                    : "Unknown";
            String status = "Building: " + active + " " + BuildingSystem.get().getBuildProgress() + "%";
            if (BuildingSystem.get().isPaused()) {
                status += " (paused)";
            }
            builder.set("#BuildingStatus.Text", status);
        } else {
            builder.set("#BuildingStatus.Text", "");
        }

        update(false, builder);
    }
}
