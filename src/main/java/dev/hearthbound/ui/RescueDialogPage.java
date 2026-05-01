package dev.hearthbound.ui;

import com.hypixel.hytale.builtin.adventure.objectives.Objective;
import com.hypixel.hytale.builtin.adventure.objectives.ObjectivePlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerConfigData;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.npc.HearthboundDataStore;
import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.VillagerNames;
import dev.hearthbound.quest.RescueQuestManager;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerData;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Dialog shown when the player presses F on a rescue-quest trapped villager.
 *
 * Screens:
 *   INTRO         — villager reacts to player's presence
 *   INTRO_2       — what happened / backstory
 *   INTRO_3       — villager's situation / segue to the settlement offer
 *   JOIN_PROPOSAL — villager responds to the offer
 *   FAREWELL      — villager agrees;
 *                   on dismiss: despawn trapped NPC, spawn follower, advance objective
 *
 * Dialog content varies per QuestVariant stored in VillagerData.questVariant.
 * Empty / unknown questVariant falls back to TRAP dialog (backward compat, 2 intro screens).
 */
public class RescueDialogPage extends InteractiveCustomUIPage<DialogEventData> {

    private static final Logger LOGGER = Logger.getLogger(RescueDialogPage.class.getName());

    static final String OBJECTIVE_ID = "Objective_RescueTrap_Rescue";
    public static final String OBJECTIVE_RETURN_ID = "Objective_RescueTrap_Return";

    private static final String INTRO         = "intro";
    private static final String INTRO_2       = "intro_2";
    private static final String INTRO_3       = "intro_3";
    private static final String JOIN_PROPOSAL = "join_proposal";
    private static final String FAREWELL      = "farewell";

    private final Ref<EntityStore> npcRef;

    private String screen;
    private String npcName;
    private String questVariant;
    private boolean agreedToJoin = false;
    private boolean reopened = false;

    @SuppressWarnings("removal")
    public RescueDialogPage(PlayerRef playerRef, Ref<EntityStore> npcRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, DialogEventData.CODEC);
        this.npcRef = npcRef;
    }

    // -------------------------------------------------------------------------
    // Build
    // -------------------------------------------------------------------------

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder events,
                      Store<EntityStore> store) {
        VillagerData villagerData = store.getComponent(npcRef, VillagerData.getComponentType());
        questVariant = (villagerData != null && villagerData.getQuestVariant() != null)
                ? villagerData.getQuestVariant() : "";

        String[] name = VillagerNames.rollHumanName(0x7A2F_C301L);
        npcName = name[0] + " " + name[1];

        // Skip to final screen if player already reached it in a previous session.
        reopened = villagerData != null && villagerData.isDialogReachedFinal();
        screen = reopened ? INTRO_3 : INTRO;
        agreedToJoin = false;

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
            case INTRO         -> renderIntro(b);
            case INTRO_2       -> renderIntro2(b);
            case INTRO_3       -> renderIntro3(b);
            case JOIN_PROPOSAL -> renderJoinProposal(b);
            case FAREWELL      -> renderFarewell(b);
        }
    }

    private void renderIntro(UICommandBuilder b) {
        b.set("#SpeakerName.Text", npcName);
        b.set("#BtnPrimary.Visible", true);

        switch (questVariant) {
            case "CABIN" -> {
                b.set("#DialogText.Text",
                        "Keep your voice down. There might be more of them outside.");
                b.set("#BtnPrimary.Text", "How long have you been in here?");
            }
            case "RUINS" -> {
                b.set("#DialogText.Text",
                        "You climbed up. Good. I used a rope to get up here. Wanted to see if there was anything left worth finding.\n" +
                        "And then skeletons came up out of the ground and started shooting at me. There was nothing up here. I hadn't even touched anything.");
                b.set("#BtnPrimary.Text", "How long have you been up here?");
            }
            case "CAMP" -> {
                b.set("#DialogText.Text",
                        "They killed my father. Then put me in here. A week ago.");
                b.set("#BtnPrimary.Text", "What happened?");
            }
            default -> {
                // TRAP — original dialog, no INTRO_3
                b.set("#DialogText.Text",
                        "Careful — spikes at the bottom. I didn't see them until I was already in.\n" +
                        "Goblins. Three nights ago. They came at night, no warning.\n" +
                        "By the time I understood what was happening, the fires were already past saving.");
                b.set("#BtnPrimary.Text", "What happened to the others?");
            }
        }
    }

    private void renderIntro2(UICommandBuilder b) {
        b.set("#SpeakerName.Text", npcName);
        b.set("#BtnPrimary.Visible", true);

        switch (questVariant) {
            case "CABIN" -> {
                b.set("#DialogText.Text",
                        "Two days. I stopped here for the night and they showed up before dawn.\n" +
                        "I came from a settlement three days east. People left one by one. Bad harvests. By spring it was just empty buildings.");
                b.set("#BtnPrimary.Text", "I'm putting together a settlement. You could come.");
            }
            case "RUINS" -> {
                b.set("#DialogText.Text",
                        "Since yesterday morning. Been moving around for about a year, looking for clues why everything suddenly went bad.\n" +
                        "Nearly died twice doing it. I think I've had enough.");
                b.set("#BtnPrimary.Text", "I'm putting together a settlement. You could come.");
            }
            case "CAMP" -> {
                b.set("#DialogText.Text",
                        "They came into our village. Most people ran. One got away right past me. I wasn't fast enough.");
                b.set("#BtnPrimary.Text", "Someone made it. They're at my settlement.");
            }
            default -> {
                // TRAP
                b.set("#DialogText.Text",
                        "I don't know. I ran.\n" +
                        "They set this trap on the main trail out — knew people would flee that way. " +
                        "I've been down here since. The one watching me comes around every few hours.\n" +
                        "There is no village to go back to.");
                b.set("#BtnPrimary.Text", "I'm building a settlement nearby. You could come.");
            }
        }
    }

    private void renderIntro3(UICommandBuilder b) {
        b.set("#SpeakerName.Text", npcName);
        b.set("#ChoiceContainer.Visible", true);
        b.set("#BtnChoice1.Visible", true);
        b.set("#BtnChoice2.Visible", true);
        b.set("#BtnChoice1.Text", "Follow me.");
        b.set("#BtnChoice2.Text", "I'll come back for you.");

        if (reopened) {
            b.set("#DialogText.Text", "Are we going?");
        } else {
            switch (questVariant) {
                case "CABIN" -> b.set("#DialogText.Text", "Where is it?");
                case "RUINS" -> b.set("#DialogText.Text", "Really? That would be good. I've seen enough ruins.");
                case "CAMP"  -> b.set("#DialogText.Text", "Who?\nGet me out of here. Tell me on the way.");
                default      -> b.set("#DialogText.Text",
                        "A settlement.\n" +
                        "I know how to work. I'm not looking for charity. " +
                        "I'll carry my weight, whatever needs doing.\n" +
                        "If you mean it, then yes. I'll come.");
            }
        }
    }

    private void renderJoinProposal(UICommandBuilder b) {
        // Unused for new variants — kept for any future use or edge cases.
    }

    private void renderFarewell(UICommandBuilder b) {
        // Unused — removed in favour of direct choice in INTRO_3.
    }

    // -------------------------------------------------------------------------
    // Event handler
    // -------------------------------------------------------------------------

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, DialogEventData data) {
        String action = data.getAction();

        if ("close".equals(action)) {
            close();
            return;
        }

        String next = screen;

        switch (screen) {
            case INTRO   -> { if ("primary".equals(action)) next = INTRO_2; }
            case INTRO_2 -> {
                if ("primary".equals(action)) {
                    next = INTRO_3;
                    markDialogReachedFinal(store);
                }
            }
            case INTRO_3 -> {
                if ("choice1".equals(action)) {
                    spawnFollowerAndAdvanceObjective(ref, store);
                    close();
                    return;
                } else if ("choice2".equals(action)) {
                    close();
                    return;
                }
            }
        }

        screen = next;
        UICommandBuilder builder = new UICommandBuilder();
        render(builder);
        sendUpdate(builder, false);
    }

    // -------------------------------------------------------------------------
    // Quest progression
    // -------------------------------------------------------------------------

    private void markDialogReachedFinal(Store<EntityStore> store) {
        VillagerData data = store.getComponent(npcRef, VillagerData.getComponentType());
        if (data == null) return;
        data.setDialogReachedFinal(true);
        store.putComponent(npcRef, VillagerData.getComponentType(), data);
    }

    private void spawnFollowerAndAdvanceObjective(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        try {
            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent npcTransform =
                    store.getComponent(npcRef, com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            com.hypixel.hytale.math.vector.Vector3d spawnPos =
                    (npcTransform != null) ? npcTransform.getPosition() : null;

            VillagerData victimData = store.getComponent(npcRef, VillagerData.getComponentType());
            long skinSeed = (victimData != null) ? victimData.getSkinSeed() : 0L;

            UUID oldUuid = NpcManager.extractUuid(store, npcRef);
            if (oldUuid != null) {
                NpcRegistry.get().unregister(oldUuid);
            }

            advanceRescueObjective(playerRef, store);
            spawnReturnMarker(playerRef, store);

            store.removeEntity(npcRef, com.hypixel.hytale.component.RemoveReason.REMOVE);

            if (spawnPos == null) {
                LOGGER.warning("spawnFollowerAndAdvanceObjective: no transform on trapped NPC");
                return;
            }

            com.hypixel.hytale.math.vector.Vector3d followerPos =
                    new com.hypixel.hytale.math.vector.Vector3d(spawnPos.getX(), spawnPos.getY() + 0.1, spawnPos.getZ());
            var follower = NpcManager.spawnNpc(store, followerPos,
                    new com.hypixel.hytale.math.vector.Vector3f(0, 0, 0), "Villager_Rescue_Follower");

            if (follower == null) {
                LOGGER.warning("spawnFollowerAndAdvanceObjective: failed to spawn follower NPC");
                return;
            }

            Ref<EntityStore> followerRef = follower.first();

            if (victimData != null) {
                store.putComponent(followerRef, VillagerData.getComponentType(), victimData);
            }

            dev.hearthbound.npc.VillagerAppearance.apply(followerRef, store, skinSeed, 0);

            UUID followerUuid = NpcManager.extractUuid(store, followerRef);
            if (followerUuid != null) {
                long chunkIndex = NpcManager.chunkIndexFor(followerPos);
                NpcRegistry.get().register(new NpcRegistry.NpcRecord(
                        followerUuid, "Villager_Rescue_Follower",
                        NpcRegistry.InteractionType.FOLLOWER, skinSeed, chunkIndex));
                HearthboundDataStore.get().save();
            }

            RescueQuestManager.registerFollower(followerRef);
            LOGGER.info("Spawned rescue follower NPC at " + followerPos);

        } catch (Exception e) {
            LOGGER.warning("spawnFollowerAndAdvanceObjective failed: " + e.getMessage());
        }
    }

    private void spawnReturnMarker(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null || !village.isFounded()) {
            LOGGER.warning("spawnReturnMarker: no founded village, skipping marker");
            return;
        }
        var pos = new com.hypixel.hytale.math.vector.Vector3d(
                village.getFoundingStoneX() + 0.5,
                village.getFoundingStoneY() + 1.0,
                village.getFoundingStoneZ() + 0.5);
        RescueQuestManager.spawnReturnMarker(store, pos, playerRef);
        LOGGER.info("Spawned Village_Return_Marker at founding stone " + pos);
    }

    private void advanceRescueObjective(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        try {
            Player player = store.getComponent(playerRef, Player.getComponentType());
            if (player == null) return;

            PlayerConfigData config = player.getPlayerConfigData();
            if (config == null) return;

            Set<UUID> activeObjectives = config.getActiveObjectiveUUIDs();
            if (activeObjectives == null || activeObjectives.isEmpty()) return;

            ObjectivePlugin plugin = ObjectivePlugin.get();
            for (UUID objectiveUuid : activeObjectives) {
                Objective objective = plugin.getObjectiveDataStore().getObjective(objectiveUuid);
                if (objective == null) continue;
                if (!OBJECTIVE_ID.equals(objective.getObjectiveId())) continue;
                objective.complete(store);
                LOGGER.info("Advanced rescue objective: " + OBJECTIVE_ID);
                return;
            }
            LOGGER.warning("advanceRescueObjective: objective not found among active objectives");
        } catch (Exception e) {
            LOGGER.warning("advanceRescueObjective failed: " + e.getMessage());
        }
    }
}
