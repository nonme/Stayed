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

public class TestDialogPage extends InteractiveCustomUIPage<DialogEventData> {

    private final String title;
    private final String message;

    public TestDialogPage(PlayerRef playerRef, String title, String message) {
        super(playerRef, CustomPageLifetime.CanDismiss, DialogEventData.CODEC);
        this.title = title;
        this.message = message;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder events, Store<EntityStore> store) {
        builder.append("TestDialog.ui");
        builder.set("#Title.Text", title);
        builder.set("#Message.Text", message);

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CloseButton",
                EventData.of(DialogEventData.ACTION_KEY, "close"),
                false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, DialogEventData data) {
        if ("close".equals(data.getAction())) {
            close();
        }
    }
}
