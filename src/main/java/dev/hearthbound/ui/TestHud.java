package dev.hearthbound.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class TestHud extends CustomUIHud {

    public TestHud(PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(UICommandBuilder builder) {
        builder.append("TestHud.ui");
        builder.set("#HudTitle.Text", "Hearthbound");
        builder.set("#HudText.Text", "Village: (none)");
    }

    public void updateText(String title, String text) {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#HudTitle.Text", title);
        builder.set("#HudText.Text", text);
        update(false, builder);
    }
}
