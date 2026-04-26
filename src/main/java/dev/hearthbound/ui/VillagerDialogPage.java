package dev.hearthbound.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.village.VillagerData;

public class VillagerDialogPage extends InteractiveCustomUIPage<DialogEventData> {

    // Level 1 (Miserable) → red, Level 5 (Thriving) → bright green
    private static final String[] SEG_COLORS_ACTIVE = {
        "#8a1a1a", // Miserable
        "#8a5010", // Unhappy
        "#7a7010", // Content
        "#2a6830", // Happy
        "#1a8840"  // Thriving
    };
    private static final String SEG_COLOR_INACTIVE = "#111816";

    private final Ref<EntityStore> npcRef;

    public VillagerDialogPage(PlayerRef playerRef, Ref<EntityStore> npcRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, DialogEventData.CODEC);
        this.npcRef = npcRef;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder events,
                      Store<EntityStore> store) {
        builder.append("VillagerDialog.ui");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of(DialogEventData.ACTION_KEY, "close"), false);
        populate(builder, store);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, DialogEventData data) {
        if ("close".equals(data.getAction())) {
            close();
            return;
        }
        UICommandBuilder b = new UICommandBuilder();
        sendUpdate(b, false);
    }

    private void populate(UICommandBuilder b, Store<EntityStore> store) {
        VillagerData data = store.getComponent(npcRef, VillagerData.getComponentType());

        if (data == null) {
            b.set("#VillagerName.Text", "Unknown");
            b.set("#VillagerRace.Text", "");
            b.set("#HappinessLabel.Text", "Content");
            renderHappinessBar(b, 0);
            renderNeeds(b, false, false, false);
            return;
        }

        b.set("#VillagerName.Text", data.getFullName().isEmpty() ? "Villager" : data.getFullName());
        b.set("#VillagerRace.Text", capitalize(data.getRace()));
        b.set("#HappinessLabel.Text", data.getHappinessLabel());
        renderHappinessBar(b, data.getHappiness());
        renderNeeds(b, !data.hasHome(), data.isHungry(), data.isStarving());
    }

    private void renderHappinessBar(UICommandBuilder b, int happiness) {
        int level;
        if (happiness <= -50)      level = 1;
        else if (happiness <= -15) level = 2;
        else if (happiness <   15) level = 3;
        else if (happiness <   50) level = 4;
        else                       level = 5;

        for (int i = 1; i <= 5; i++) {
            b.set("#HappySeg" + i + ".Background",
                    i <= level ? SEG_COLORS_ACTIVE[i - 1] : SEG_COLOR_INACTIVE);
        }
    }

    private void renderNeeds(UICommandBuilder b, boolean noHome, boolean hungry, boolean starving) {
        boolean anyNeed = noHome || hungry;
        b.set("#NeedHome.Visible", noHome);
        b.set("#NeedHungry.Visible", hungry);
        if (hungry) {
            b.set("#NeedHungryText.Text", starving ? "Starving" : "Hungry");
        }
        b.set("#NeedNone.Visible", !anyNeed);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
