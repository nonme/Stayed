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
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;
import dev.hearthbound.npc.VillagerNames;
import dev.hearthbound.quest.RescueQuest1;
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
 *   INTRO         — villager describes the attack; player asks what happened to the others
 *   INTRO_2       — villager: ran, fell in the trap, alone since, nowhere left to go
 *   JOIN_PROPOSAL — player proposes joining the new settlement
 *   FAREWELL      — villager agrees, says they'll wait;
 *                   on dismiss: despawn trapped NPC, spawn follower, advance objective
 */
public class RescueDialogPage extends InteractiveCustomUIPage<DialogEventData> {

    private static final Logger LOGGER = Logger.getLogger(RescueDialogPage.class.getName());

    static final String OBJECTIVE_ID = "Objective_RescueTrap_Rescue";
    public static final String OBJECTIVE_RETURN_ID = "Objective_RescueTrap_Return";

    private static final String INTRO         = "intro";
    private static final String INTRO_2       = "intro_2";
    private static final String JOIN_PROPOSAL = "join_proposal";
    private static final String FAREWELL      = "farewell";

    private final Ref<EntityStore> npcRef;

    private String screen;
    private String npcName;
    private boolean agreedToJoin = false;

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
        // Fixed seed — same name every time the player opens this dialog.
        String[] name = VillagerNames.rollHumanName(0x7A2F_C301L);
        npcName = name[0] + " " + name[1];

        screen = INTRO;
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

        if ("close".equals(action)) {
            close();
            return;
        }

        String next = screen;

        switch (screen) {
            case INTRO         -> { if ("primary".equals(action)) next = INTRO_2; }
            case INTRO_2       -> { if ("primary".equals(action)) next = JOIN_PROPOSAL; }
            case JOIN_PROPOSAL -> {
                if ("choice1".equals(action)) {
                    agreedToJoin = true;
                    next = FAREWELL;
                } else if ("choice2".equals(action)) {
                    close();
                    return;
                }
            }
            case FAREWELL -> {
                if ("primary".equals(action)) {
                    if (agreedToJoin) {
                        spawnFollowerAndAdvanceObjective(ref, store);
                    }
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

    private void spawnFollowerAndAdvanceObjective(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        try {
            advanceRescueObjective(playerRef, store);
            spawnReturnMarker(playerRef, store);

            // Switch role in-place: Trapped → Follower. RoleChangeSystem preserves all
            // components not owned by the behavior tree (VillagerData, PlayerSkinComponent,
            // ModelComponent), so the NPC keeps its skin and position.
            NPCEntity npcEntity = store.getComponent(npcRef, NPCEntity.getComponentType());
            if (npcEntity == null) {
                LOGGER.warning("NPCEntity component missing on npcRef — cannot change role");
                return;
            }
            int followerRoleIndex = NPCPlugin.get().getIndex("Villager_Rescue_Follower");
            if (followerRoleIndex < 0) {
                LOGGER.warning("Role 'Villager_Rescue_Follower' not found");
                return;
            }
            // changeAppearance=false — keep the PlayerSkin we applied at spawn time.
            RoleChangeSystem.requestRoleChange(npcRef, npcEntity.getRole(), followerRoleIndex, false, store);
            RescueQuest1.registerFollower(npcRef);
            LOGGER.info("Role change queued: Trapped → Follower");

            // RoleChangeSystem processes the queue next tick. After that, remove the
            // rescue interaction (F-key hint) since followers don't have a dialog yet.
            var world = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore)
                    store.getExternalData()).getWorld();
            world.execute(() -> {
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                if (npcRef.isValid()) {
                    liveStore.tryRemoveComponent(npcRef,
                            com.hypixel.hytale.server.core.modules.interaction.Interactions.getComponentType());
                    liveStore.tryRemoveComponent(npcRef,
                            com.hypixel.hytale.server.core.modules.entity.component.Interactable.getComponentType());
                }
            });

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
        RescueQuest1.spawnReturnMarker(store, pos, playerRef);
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

                // complete() sends the reward packet, notifies the player, and advances the line.
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
