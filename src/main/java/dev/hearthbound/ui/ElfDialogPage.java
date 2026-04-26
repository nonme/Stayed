package dev.hearthbound.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.quest.RescueQuest1;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Dialog with the elf sage Aelin.
 *
 * Screens:
 *   INTRO_1..3    — first-meeting linear intro (3 slides)
 *   NAME_CONFIRM  — elf reacts to player's nick
 *   MENU          — choice menu (repeatable)
 *   ANSWER_ELF    — "Tell me about yourself" answer
 *   ANSWER_STONE  — "How to found a settlement" + give stone
 *   ANSWER_REMIND — "Remind me about the stone" (after stone given)
 *   POST_FOUNDED  — comment after village founded (pre-Town Hall)
 *   QUEST_RESCUE  — elf proposes scouting for survivors (stage >= TOWN_HALL, quest not started)
 *   QUEST_ACTIVE  — read-only: quest is already in progress
 */
public class ElfDialogPage extends InteractiveCustomUIPage<DialogEventData> {

    private static final Logger LOGGER = Logger.getLogger(ElfDialogPage.class.getName());
    private static final String FOUNDING_STONE_ID = "Hearthbound_Founding_Stone";

    // Screen constants
    private static final String INTRO_1             = "intro_1";
    private static final String INTRO_TRUST         = "intro_trust";
    private static final String NAME_CONFIRM        = "name_confirm";
    private static final String MENU                = "menu";
    private static final String ANSWER_ELF          = "answer_elf";
    private static final String ANSWER_ELF_2        = "answer_elf_2";
    private static final String ANSWER_STONE        = "answer_stone";
    private static final String ANSWER_STONE_OFFER  = "answer_stone_offer";
    private static final String ANSWER_REMIND       = "answer_remind";
    private static final String ANSWER_REMIND_LOST  = "answer_remind_lost";
    private static final String POST_FOUNDED        = "post_founded";
    private static final String QUEST_RESCUE        = "quest_rescue";
    private static final String QUEST_ACTIVE        = "quest_active";
    private static final String QUEST_HOUSE         = "quest_house";
    private static final String QUEST_HOUSE_OFFER   = "quest_house_offer";
    private static final String BUILD_MENU          = "build_menu";

    private static final String BRAZIER_ITEM_ID = "Hearthbound_Brazier";

    private String screen;
    private String playerName;

    // Snapshot of village state at dialog open
    private boolean metElf;
    private boolean stoneGiven;
    private boolean founded;
    private boolean townHallBuilt;
    private boolean rescueQuestStarted;
    private boolean hasStoneInInventory;
    private boolean houseQuestOffered;
    private boolean houseBrazierGiven;
    private int villagerCount;

    // Cached for quest launch (captured at build time)
    private Ref<EntityStore> cachedPlayerRef;
    private Player cachedPlayer;
    private UUID cachedPlayerUuid;
    private World cachedWorld;

    @SuppressWarnings("removal")
    public ElfDialogPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, DialogEventData.CODEC);
    }

    // -------------------------------------------------------------------------
    // Build (called once on open)
    // -------------------------------------------------------------------------

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder events,
                      Store<EntityStore> store) {

        Player player = store.getComponent(ref, Player.getComponentType());
        playerName = (player != null) ? player.getDisplayName() : "Stranger";

        VillageData village = VillageManager.get().getVillageData(store, ref);
        metElf             = village != null && village.isMetElf();
        stoneGiven         = village != null && village.isFoundingStoneGiven();
        founded            = village != null && village.isFounded();
        townHallBuilt      = village != null && village.getStage() >= VillageData.STAGE_TOWN_HALL;
        rescueQuestStarted = village != null && village.isRescueQuestStarted();
        houseQuestOffered  = village != null && village.isHouseQuestOffered();
        houseBrazierGiven  = village != null && village.isHouseBrazierGiven();
        villagerCount      = village != null ? village.getVillagerCount() : 0;
        hasStoneInInventory = player != null && player.getInventory().getCombinedHotbarFirst()
                .countItemStacks(s -> FOUNDING_STONE_ID.equals(s.getItemId())) > 0;

        // Cache for async quest launch
        cachedPlayerRef  = ref;
        cachedPlayer     = player;
        cachedWorld      = ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore)
                store.getExternalData()).getWorld();
        UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
        cachedPlayerUuid = (uuidComp != null) ? uuidComp.getUuid() : null;

        // Choose starting screen
        if (!founded) {
            screen = !metElf ? INTRO_1 : MENU;
        } else if (townHallBuilt) {
            // Auto-show house quest offer if villager arrived and it hasn't been offered yet
            screen = (villagerCount >= 1 && !houseQuestOffered) ? QUEST_HOUSE : MENU;
        } else {
            screen = POST_FOUNDED;
        }

        builder.append("ElfDialog.ui");
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
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnChoice3",
                EventData.of(DialogEventData.ACTION_KEY, "choice3"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnChoice4",
                EventData.of(DialogEventData.ACTION_KEY, "choice4"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnClose",
                EventData.of(DialogEventData.ACTION_KEY, "close"), false);
    }

    // -------------------------------------------------------------------------
    // Render — sets UI state for current screen
    // -------------------------------------------------------------------------

    private void render(UICommandBuilder b) {
        // Hide everything first
        b.set("#BtnPrimary.Visible", false);
        b.set("#BtnChoice1.Visible", false);
        b.set("#BtnChoice2.Visible", false);
        b.set("#BtnChoice3.Visible", false);
        b.set("#BtnChoice4.Visible", false);
        b.set("#ChoiceContainer.Visible", false);

        switch (screen) {
            case INTRO_1 -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Someone who walks into a stranger's camp and says hello. " +
                        "Either you are brave or you have not learned caution yet.\n" +
                        "Aelin. Architect — or used to be, before the Sundering took most of " +
                        "what that word meant to me.\n" +
                        "And you are...?");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "I am " + playerName + ".");
                b.set("#BtnChoice1.Visible", true);
                b.set("#BtnChoice2.Text", "Why should I trust a stranger by a fire?");
                b.set("#BtnChoice2.Visible", true);
            }
            case INTRO_TRUST -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "You should not. I would think less of you if you did.\n" +
                        "I watched three civilizations hand their trust to the wrong people. " +
                        "Two of them are gone now.\n" +
                        "I am not asking for trust. Just your name.");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "I am " + playerName + ".");
                b.set("#BtnChoice1.Visible", true);
            }
            case NAME_CONFIRM -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text", nameReaction(playerName));
                b.set("#BtnPrimary.Text", "Likewise.");
                b.set("#BtnPrimary.Visible", true);
            }
            case MENU -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text", "Ask. I have time — more than I would like.");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "What's your story?");
                b.set("#BtnChoice1.Visible", true);
                if (townHallBuilt) {
                    if (!rescueQuestStarted) {
                        b.set("#BtnChoice2.Text", "We need settlers.");
                    } else {
                        b.set("#BtnChoice2.Text", "Any news on the survivors?");
                    }
                    b.set("#BtnChoice4.Text", "I want to build something.");
                    b.set("#BtnChoice4.Visible", true);
                } else if (!stoneGiven) {
                    b.set("#BtnChoice2.Text", "I want to start a settlement.");
                } else {
                    b.set("#BtnChoice2.Text", "About the Founding Stone...");
                }
                b.set("#BtnChoice2.Visible", true);
                b.set("#BtnChoice3.Text", "That's all for now.");
                b.set("#BtnChoice3.Visible", true);
            }
            case BUILD_MENU -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "What do you need built?\n" +
                        "Tell me, and I will give you an anchor to mark the site.");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "A house for a settler.");
                b.set("#BtnChoice1.Visible", true);
                b.set("#BtnChoice2.Text", "Never mind.");
                b.set("#BtnChoice2.Visible", true);
            }
            case ANSWER_ELF -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Four centuries of walking Orbis. Kweebec treehouses, Trork mountain keeps, " +
                        "human towns that burned down and got rebuilt in the same spot twice.\n" +
                        "I designed temples for gods nobody worships anymore. " +
                        "Granaries for kings who taxed themselves into collapse.\n" +
                        "Then the Sundering. My city went with it. " +
                        "I packed what I could carry and started walking again.");
                b.set("#BtnPrimary.Text", "So you're still looking.");
                b.set("#BtnPrimary.Visible", true);
            }
            case ANSWER_ELF_2 -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "For land worth building on. Someone worth building with.\n" +
                        "Have not found either yet — but I have not stopped, either.\n" +
                        "That is why I am still carrying my tools, " + playerName + ". " +
                        "An architect without a site is just someone with heavy bags.");
                b.set("#BtnPrimary.Text", "Back");
                b.set("#BtnPrimary.Visible", true);
            }
            case ANSWER_STONE -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "A settlement needs someone to build it and someone to pay for it. " +
                        "I can build. Can you carry the weight of the second part?\n" +
                        "Gather people, gather resources, make decisions when they are bad choices " +
                        "on both sides. That is what founding something actually means.\n" +
                        "If that sounds like what you want — I can help you start.");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "Yes. Let's do it.");
                b.set("#BtnChoice1.Visible", true);
                b.set("#BtnChoice2.Text", "I need to think about it.");
                b.set("#BtnChoice2.Visible", true);
            }
            case ANSWER_STONE_OFFER -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Then find ground that works — flat, water nearby, somewhere you can " +
                        "defend if it comes to that.\n" +
                        "Place the Founding Stone when you have the spot. " +
                        "I will see where it lands and come to you. " +
                        "We put up the Town Hall first — everything else comes after that.");
                b.set("#BtnPrimary.Text", "Understood.");
                b.set("#BtnPrimary.Visible", true);
            }
            case ANSWER_REMIND -> {
                b.set("#SpeakerName.Text", "Aelin");
                if (!hasStoneInInventory) {
                    b.set("#DialogText.Text",
                            "The stone. Right.\n" +
                            "Do you still have it, or did something happen to it?");
                    b.set("#ChoiceContainer.Visible", true);
                    b.set("#BtnChoice1.Text", "I have it somewhere.");
                    b.set("#BtnChoice1.Visible", true);
                    b.set("#BtnChoice2.Text", "I lost it.");
                    b.set("#BtnChoice2.Visible", true);
                } else {
                    b.set("#DialogText.Text",
                            "Find a spot. Place the stone on the ground — not in a chest, " +
                            "not in your pack, on the ground. I will feel it.\n" +
                            "Town Hall first. Then the rest.");
                    b.set("#BtnPrimary.Text", "Right.");
                    b.set("#BtnPrimary.Visible", true);
                }
            }
            case ANSWER_REMIND_LOST -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Of course you did.\n" +
                        "I have spares. Occupational habit — I stopped trusting people " +
                        "to keep things after the third time.\n" +
                        "Here.");
                b.set("#BtnPrimary.Text", "Thank you.");
                b.set("#BtnPrimary.Visible", true);
            }
            case POST_FOUNDED -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Stone is down. That is the easy part.\n" +
                        "Bring what we need to the stone — wood, rock, the rest — and put it inside. " +
                        "Once it is all there, I will start on the Town Hall.");
                b.set("#BtnPrimary.Text", "On my way.");
                b.set("#BtnPrimary.Visible", true);
            }
            case QUEST_RESCUE -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "There are people in these woods who lost everything in the Sundering — " +
                        "villages burned, families scattered. Some of them are still out there, " +
                        "trying to survive alone.\n" +
                        "A settlement like ours needs people. " +
                        "Go find one of those survivors. Offer them a place here.\n" +
                        "I can feel something to the east — movement, a small fire. " +
                        "I would start there.");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "I'll go look.");
                b.set("#BtnChoice1.Visible", true);
                b.set("#BtnChoice2.Text", "Not right now.");
                b.set("#BtnChoice2.Visible", true);
            }
            case QUEST_ACTIVE -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Still searching? " +
                        "Follow the marker — that is where I felt the presence.\n" +
                        "Come back once you've found them.");
                b.set("#BtnPrimary.Text", "Understood.");
                b.set("#BtnPrimary.Visible", true);
            }
            case QUEST_HOUSE -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "You brought someone back.\n" +
                        "Good. But a person who sleeps outside is not a settler — they are a guest " +
                        "waiting to leave.\n" +
                        "They need a house. A place that is theirs. That is what makes them stay.");
                b.set("#BtnPrimary.Text", "What do I need?");
                b.set("#BtnPrimary.Visible", true);
            }
            case QUEST_HOUSE_OFFER -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "I will build it. You just need to find the right spot.\n" +
                        "Place the brazier where you want the hearth to be — inside the footprint, " +
                        "on the ground. The house goes around it.\n" +
                        "Use the brazier to manage construction once the site is chosen.");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "Got it.");
                b.set("#BtnChoice1.Visible", true);
                b.set("#BtnChoice2.Text", "Not now.");
                b.set("#BtnChoice2.Visible", true);
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
            case INTRO_1 -> {
                if ("choice1".equals(action)) next = NAME_CONFIRM;
                else if ("choice2".equals(action)) next = INTRO_TRUST;
            }
            case INTRO_TRUST -> {
                if ("choice1".equals(action)) next = NAME_CONFIRM;
            }
            case NAME_CONFIRM -> { if ("primary".equals(action)) next = MENU; }

            case MENU -> {
                switch (action) {
                    case "choice1" -> next = ANSWER_ELF;
                    case "choice2" -> {
                        if (townHallBuilt) {
                            next = rescueQuestStarted ? QUEST_ACTIVE : QUEST_RESCUE;
                        } else {
                            next = stoneGiven ? ANSWER_REMIND : ANSWER_STONE;
                        }
                    }
                    case "choice3" -> { close(); return; }
                    case "choice4" -> next = BUILD_MENU;
                }
            }

            case BUILD_MENU -> {
                if ("choice1".equals(action)) {
                    giveBrazier(ref, store);
                    close();
                    return;
                } else if ("choice2".equals(action)) {
                    next = MENU;
                }
            }

            case ANSWER_ELF   -> { if ("primary".equals(action)) next = ANSWER_ELF_2; }
            case ANSWER_ELF_2 -> { if ("primary".equals(action)) next = MENU; }

            case ANSWER_STONE -> {
                if ("choice1".equals(action)) next = ANSWER_STONE_OFFER;
                else if ("choice2".equals(action)) next = MENU;
            }

            case ANSWER_STONE_OFFER -> {
                if ("primary".equals(action)) {
                    giveFoundingStone(ref, store);
                    next = MENU;
                }
            }

            case ANSWER_REMIND -> {
                if ("primary".equals(action)) next = MENU;
                else if ("choice1".equals(action)) next = MENU;
                else if ("choice2".equals(action)) next = ANSWER_REMIND_LOST;
            }

            case ANSWER_REMIND_LOST -> {
                if ("primary".equals(action)) {
                    giveFoundingStone(ref, store);
                    next = MENU;
                }
            }

            case POST_FOUNDED -> { if ("primary".equals(action)) { close(); return; } }

            case QUEST_RESCUE -> {
                if ("choice1".equals(action)) {
                    launchRescueQuest(ref, store);
                    close();
                    return;
                } else if ("choice2".equals(action)) {
                    next = MENU;
                }
            }

            case QUEST_ACTIVE -> { if ("primary".equals(action)) { close(); return; } }

            case QUEST_HOUSE -> { if ("primary".equals(action)) next = QUEST_HOUSE_OFFER; }

            case QUEST_HOUSE_OFFER -> {
                if ("choice1".equals(action)) {
                    giveBrazier(ref, store);
                    close();
                    return;
                } else if ("choice2".equals(action)) {
                    // Mark offered anyway so auto-open doesn't re-trigger next visit
                    markHouseQuestOffered(ref, store);
                    next = MENU;
                }
            }
        }

        // Persist metElf on first conversation reaching MENU
        if (MENU.equals(next) && !metElf) {
            metElf = true;
            saveFlags(ref, store);
        }

        screen = next;
        UICommandBuilder builder = new UICommandBuilder();
        render(builder);
        sendUpdate(builder, false);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void launchRescueQuest(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (cachedWorld == null || cachedPlayerUuid == null) {
            LOGGER.warning("launchRescueQuest: missing cached world or UUID");
            return;
        }

        // Mark quest as started immediately so repeated dialog opens show QUEST_ACTIVE
        rescueQuestStarted = true;
        VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
        village.setRescueQuestStarted(true);
        VillageManager.get().save(store, ref, village);

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            LOGGER.warning("launchRescueQuest: player has no TransformComponent");
            return;
        }

        RescueQuest1.startForPlayer(
                cachedWorld, store, ref, cachedPlayer, cachedPlayerUuid,
                transform.getPosition(),
                spawned -> {
                    if (spawned == null) {
                        LOGGER.warning("launchRescueQuest: startForPlayer failed");
                    }
                });
    }

    private void giveFoundingStone(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            var tx = player.getInventory().getCombinedHotbarFirst()
                    .addItemStack(new ItemStack(FOUNDING_STONE_ID, 1));
            if (tx.succeeded()) {
                hasStoneInInventory = true;
                if (!stoneGiven) {
                    stoneGiven = true;
                    saveFlags(ref, store);
                }
                LOGGER.info("Gave Founding Stone to " + playerName);
            } else {
                LOGGER.warning("Could not give Founding Stone to " + playerName + " — inventory full?");
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to give Founding Stone: " + e.getMessage());
        }
    }

    private void giveBrazier(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            var tx = player.getInventory().getCombinedHotbarFirst()
                    .addItemStack(new ItemStack(BRAZIER_ITEM_ID, 1));
            if (tx.succeeded()) {
                houseBrazierGiven = true;
                houseQuestOffered = true;
                VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
                village.setHouseBrazierGiven(true);
                village.setHouseQuestOffered(true);
                VillageManager.get().save(store, ref, village);
                LOGGER.info("Gave Brazier to " + playerName);
            } else {
                LOGGER.warning("Could not give Brazier to " + playerName + " — inventory full?");
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to give Brazier: " + e.getMessage());
        }
    }

    private void markHouseQuestOffered(Ref<EntityStore> ref, Store<EntityStore> store) {
        houseQuestOffered = true;
        VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
        village.setHouseQuestOffered(true);
        VillageManager.get().save(store, ref, village);
    }

    private void saveFlags(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
            village.setMetElf(metElf);
            village.setFoundingStoneGiven(stoneGiven);
            VillageManager.get().save(store, ref, village);
        } catch (Exception e) {
            LOGGER.warning("Failed to save elf dialog flags: " + e.getMessage());
        }
    }

    private String nameReaction(String name) {
        char first = Character.toLowerCase(name.isEmpty() ? 'a' : name.charAt(0));
        if ("aeiou".indexOf(first) >= 0) {
            return name + ".\n" +
                   "I knew an Outlander general with a name that started the same way. " +
                   "Difficult person. Built three keeps nobody commissioned.\n" +
                   "Two of them are still standing. Make of that what you will.";
        } else {
            return name + ".\n" +
                   "Short. I have spent enough years in places where names take longer to say " +
                   "than the conversation that follows. Yours will do fine.\n" +
                   "This land does not have a name yet. That part is yours to fix, if you want it.";
        }
    }
}
