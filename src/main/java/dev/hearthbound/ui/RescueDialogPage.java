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
import dev.hearthbound.npc.VillagerNames;

/**
 * Dialog shown when the player presses F on a rescue-quest trapped villager.
 *
 * Screens:
 *   INTRO         — villager describes the attack; player asks what happened to the others
 *   INTRO_2       — villager: ran, fell in the trap, alone since, nowhere left to go
 *   JOIN_PROPOSAL — player proposes joining the new settlement
 *   FAREWELL      — villager agrees, says they'll wait
 */
public class RescueDialogPage extends InteractiveCustomUIPage<DialogEventData> {

    private static final String INTRO         = "intro";
    private static final String INTRO_2       = "intro_2";
    private static final String JOIN_PROPOSAL = "join_proposal";
    private static final String FAREWELL      = "farewell";

    private String screen;
    private String npcName;

    @SuppressWarnings("removal")
    public RescueDialogPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, DialogEventData.CODEC);
    }

    // -------------------------------------------------------------------------
    // Build
    // -------------------------------------------------------------------------

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder events,
                      Store<EntityStore> store) {
        // Fixed seed so the name is stable across multiple F presses on the same NPC.
        String[] name = VillagerNames.rollHumanName(0x7A2F_C301L);
        npcName = name[0] + " " + name[1];

        screen = INTRO;

        builder.append("RescueDialog.ui");
        bindEvents(events);
        render(builder);
    }

    private void bindEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnPrimary",
                EventData.of(DialogEventData.ACTION_KEY, "primary"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnChoice1",
                EventData.of(DialogEventData.ACTION_KEY, "choice1"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnChoice2",
                EventData.of(DialogEventData.ACTION_KEY, "choice2"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnClose",
                EventData.of(DialogEventData.ACTION_KEY, "close"), false);
    }

    // -------------------------------------------------------------------------
    // Render
    // -------------------------------------------------------------------------

    private void render(UICommandBuilder b) {
        b.set("#BtnPrimary.Visible", false);
        b.set("#BtnChoice1.Visible", false);
        b.set("#BtnChoice2.Visible", false);
        b.set("#BtnChoice3.Visible", false);
        b.set("#ChoiceContainer.Visible", false);

        switch (screen) {
            case INTRO -> {
                b.set("#SpeakerName.Text", npcName);
                b.set("#DialogText.Text",
                        "Please — be careful. The spikes. They covered the pit with branches " +
                        "and I didn't see it until I was already falling.\n" +
                        "Goblins. There were maybe a dozen of them. They hit our village at night — " +
                        "no warning. The fires took everything before anyone could do anything.");
                b.set("#BtnPrimary.Text", "What happened to the others?");
                b.set("#BtnPrimary.Visible", true);
            }
            case INTRO_2 -> {
                b.set("#SpeakerName.Text", npcName);
                b.set("#DialogText.Text",
                        "I don't know. I ran.\n" +
                        "The goblins must have set this trap on the trail out, knowing people would flee. " +
                        "I've been down here since. That guard up top checks in occasionally.\n" +
                        "I have no village to go back to. No family I know made it out. " +
                        "I don't even know where I'd go if you pulled me out of here.");
                b.set("#BtnPrimary.Text", "I'm building a settlement nearby. You could come.");
                b.set("#BtnPrimary.Visible", true);
            }
            case JOIN_PROPOSAL -> {
                b.set("#SpeakerName.Text", npcName);
                b.set("#DialogText.Text",
                        "A settlement.\n" +
                        "I know how to work. I'm not looking for charity — " +
                        "I'll carry my weight, whatever needs doing.\n" +
                        "If you mean it, then yes. I'll come.");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "I mean it. I'll get you out.");
                b.set("#BtnChoice1.Visible", true);
                b.set("#BtnChoice2.Text", "Come back to this when things are ready.");
                b.set("#BtnChoice2.Visible", true);
            }
            case FAREWELL -> {
                b.set("#SpeakerName.Text", npcName);
                b.set("#DialogText.Text",
                        "I'll be here. Not like I'm going anywhere.\n" +
                        "Thank you. I was starting to think no one was coming.");
                b.set("#BtnPrimary.Text", "Hold on.");
                b.set("#BtnPrimary.Visible", true);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Event handler
    // -------------------------------------------------------------------------

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, DialogEventData data) {
        String action = data.getAction();
        if ("close".equals(action)) { close(); return; }

        String next = screen;

        switch (screen) {
            case INTRO         -> { if ("primary".equals(action)) next = INTRO_2; }
            case INTRO_2       -> { if ("primary".equals(action)) next = JOIN_PROPOSAL; }
            case JOIN_PROPOSAL -> {
                if ("choice1".equals(action)) next = FAREWELL;
                else if ("choice2".equals(action)) { close(); return; }
            }
            case FAREWELL      -> { if ("primary".equals(action)) { close(); return; } }
        }

        screen = next;
        UICommandBuilder builder = new UICommandBuilder();
        render(builder);
        sendUpdate(builder, false);
    }
}
