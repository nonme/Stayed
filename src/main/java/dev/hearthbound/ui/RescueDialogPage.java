package dev.hearthbound.ui;

import java.util.Set;
import java.util.UUID;
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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.StayedRoleChangeApplier;
import dev.hearthbound.npc.VillagerNames;
import dev.hearthbound.quest.RescueQuestManager;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerData;

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

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("ui.rescuedialog");
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
                        "Goblins. Three nights ago. They came at night, we had no warning.\n" +
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
                        "I've been wandering through the forest. Then I saw food just... hanging there. Don't look at me like that, I was starving.\n" +
                        "I've been down here since. The one watching me comes around every few hours.\n" +
                        "I don't know what to do.");
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
                        "I know how to work. " +
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
            advanceRescueObjective(playerRef, store);
            spawnReturnMarker(playerRef, store);

            UUID rescueUuid = NpcManager.extractUuid(store, npcRef);
            if (rescueUuid == null) {
                LOG.warn("spawnFollowerAndAdvanceObjective: rescue NPC has no UUID");
                return;
            }
            NpcRegistry.NpcRecord record = NpcRegistry.get().getRecord(rescueUuid);
            if (record == null) {
                LOG.warn("spawnFollowerAndAdvanceObjective: no registry record for rescue NPC " + rescueUuid);
                return;
            }

            // In-place role change: same entity, same engine UUID, same npcId.
            // The Trapped behaviour tree is replaced by Follower; everything else
            // (skin, position, VillagerData) stays put.
            var npcEntity = store.getComponent(npcRef,
                    com.hypixel.hytale.server.npc.entities.NPCEntity.getComponentType());
            if (npcEntity == null) {
                LOG.warn("spawnFollowerAndAdvanceObjective: NPCEntity null on rescue NPC");
                return;
            }
            NpcRegistry.NpcRecord updated = new NpcRegistry.NpcRecord(
                    record.npcId, rescueUuid, "Villager_Rescue_Follower",
                    NpcRegistry.InteractionType.FOLLOWER, record.skinSeed, record.chunkIndex);
            if (record.hasPosition) updated.setPosition(record.lastX, record.lastY, record.lastZ);

            Player playerEntity = store.getComponent(playerRef, Player.getComponentType());
            World world = playerEntity != null ? playerEntity.getWorld() : null;
            StayedRoleChangeApplier.persistAndApply(npcRef, store, world, updated,
                    false, "rescue-victim-to-follower");

            RescueQuestManager.registerFollower(npcRef);
            LOG.info("Rescue NPC role-changed to follower (UUID: " + rescueUuid + ")");

        } catch (Exception e) {
            LOG.warn("spawnFollowerAndAdvanceObjective failed: " + e.getMessage());
        }
    }

    private void spawnReturnMarker(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null || !village.isFounded()) {
            LOG.warn("spawnReturnMarker: no founded village, skipping marker");
            return;
        }
        var pos = new com.hypixel.hytale.math.vector.Vector3d(
                village.getFoundingStoneX() + 0.5,
                village.getFoundingStoneY() + 1.0,
                village.getFoundingStoneZ() + 0.5);
        RescueQuestManager.spawnReturnMarker(store, pos, playerRef);
        LOG.info("Spawned Village_Return_Marker at founding stone " + pos);
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
                LOG.info("Advanced rescue objective: " + OBJECTIVE_ID);
                return;
            }
            LOG.warn("advanceRescueObjective: objective not found among active objectives");
        } catch (Exception e) {
            LOG.warn("advanceRescueObjective failed: " + e.getMessage());
        }
    }
}
