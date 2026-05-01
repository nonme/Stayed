package dev.hearthbound.ui;

import java.util.UUID;
import java.util.logging.Logger;

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

import dev.hearthbound.quest.RescueQuestManager;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

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
    private static final String FOUNDING_STONE_ID = "Stayed_Founding_Stone";

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
    private static final String QUEST_FARM              = "quest_farm";
    private static final String QUEST_FARM_OFFER        = "quest_farm_offer";
    private static final String QUEST_WAREHOUSE         = "quest_warehouse";
    private static final String QUEST_WAREHOUSE_OFFER   = "quest_warehouse_offer";
    private static final String QUEST_SAWMILL           = "quest_sawmill";
    private static final String QUEST_SAWMILL_OFFER     = "quest_sawmill_offer";
    private static final String QUEST_MINE              = "quest_mine";
    private static final String QUEST_MINE_OFFER        = "quest_mine_offer";
    private static final String BUILD_MENU              = "build_menu";

    private static final String BRAZIER_ITEM_ID    = "Stayed_Brazier";
    private static final String SCARECROW_ITEM_ID  = "Stayed_Scarecrow";
    private static final String COUNTER_ITEM_ID    = "Stayed_Counter";
    private static final String LUMBERMILL_ITEM_ID = "Stayed_Lumbermill";
    private static final String MINE_SIGN_ITEM_ID  = "Stayed_Mine_Sign";

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
    private boolean farmQuestOffered;
    private boolean farmScarecrowGiven;
    private boolean warehouseQuestOffered;
    private boolean warehouseCounterGiven;
    private boolean sawmillQuestOffered;
    private boolean mineQuestOffered;
    private boolean houseBuilt;
    private boolean farmBuilt;
    private boolean warehouseBuilt;
    private boolean sawmillBuilt;
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
        farmQuestOffered        = village != null && village.isFarmQuestOffered();
        farmScarecrowGiven      = village != null && village.isFarmScarecrowGiven();
        warehouseQuestOffered   = village != null && village.isWarehouseQuestOffered();
        warehouseCounterGiven   = village != null && village.isWarehouseCounterGiven();
        sawmillQuestOffered     = village != null && village.isSawmillQuestOffered();
        mineQuestOffered        = village != null && village.isMineQuestOffered();
        villagerCount           = village != null ? village.getVillagerCount() : 0;
        houseBuilt     = isBuilt(village, dev.hearthbound.village.BuildingType.HOUSE_HUMAN);
        farmBuilt      = isBuilt(village, dev.hearthbound.village.BuildingType.FARM);
        warehouseBuilt = isBuilt(village, dev.hearthbound.village.BuildingType.WAREHOUSE);
        sawmillBuilt   = isBuilt(village, dev.hearthbound.village.BuildingType.SAWMILL);
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
            if (villagerCount >= 1 && !houseQuestOffered) {
                screen = QUEST_HOUSE;
            } else if (houseBuilt && !farmQuestOffered) {
                screen = QUEST_FARM;
            } else if (farmBuilt && !warehouseQuestOffered) {
                screen = QUEST_WAREHOUSE;
            } else if (warehouseBuilt && !sawmillQuestOffered) {
                screen = QUEST_SAWMILL;
            } else if (sawmillBuilt && !mineQuestOffered) {
                screen = QUEST_MINE;
            } else {
                screen = MENU;
            }
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
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnChoice5",
                EventData.of(DialogEventData.ACTION_KEY, "choice5"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnChoice6",
                EventData.of(DialogEventData.ACTION_KEY, "choice6"), false);
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
        b.set("#BtnChoice5.Visible", false);
        b.set("#BtnChoice6.Visible", false);
        b.set("#ChoiceContainer.Visible", false);

        switch (screen) {
            case INTRO_1 -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Oh. You found my fire.\n" +
                        "Grab a spot, if you want.\n" +
                        "Aelin. I was an Architect, back before all this.\n" +
                        "Who are you?");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "I am " + playerName + ".");
                b.set("#BtnChoice1.Visible", true);
                b.set("#BtnChoice2.Text", "I'm not in the habit of giving my name to strangers.");
                b.set("#BtnChoice2.Visible", true);
            }
            case INTRO_TRUST -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Fair enough.\n" +
                        "I am not going anywhere if you change your mind.");
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
                b.set("#DialogText.Text", "Ask. I have plenty of time.");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "What's your story?");
                b.set("#BtnChoice1.Visible", true);
                if (townHallBuilt) {
                    if (!rescueQuestStarted) {
                        b.set("#BtnChoice2.Text", "We need people.");
                    } else {
                        b.set("#BtnChoice2.Text", "Any news on the survivors?");
                    }
                    b.set("#BtnChoice2.Visible", true);
                    if (houseBuilt) {
                        b.set("#BtnChoice3.Text", "I want to build something.");
                        b.set("#BtnChoice3.Visible", true);
                    }
                } else if (!stoneGiven) {
                    b.set("#BtnChoice2.Text", "I want to start a settlement.");
                    b.set("#BtnChoice2.Visible", true);
                } else {
                    b.set("#BtnChoice2.Text", "About the Founding Stone...");
                    b.set("#BtnChoice2.Visible", true);
                }
                // "That's all for now." always dimmed in BtnChoice6
                b.set("#BtnChoice6.Text", "That's all for now.");
                b.set("#BtnChoice6.Visible", true);
            }
            case BUILD_MENU -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "What do you need?");
                b.set("#ChoiceContainer.Visible", true);
                // Show all options that have been unlocked (quest offered at least once).
                // House is always available (village needs many). Others appear once their
                // quest was offered — so the player can re-request a lost anchor.
                java.util.List<String[]> opts = new java.util.ArrayList<>();
                opts.add(new String[]{"house",     "A house for a settler."});
                if (farmQuestOffered)
                    opts.add(new String[]{"farm",      "A farm for food production."});
                if (warehouseQuestOffered)
                    opts.add(new String[]{"warehouse", "A warehouse for storage."});
                if (sawmillQuestOffered)
                    opts.add(new String[]{"sawmill",   "A sawmill."});
                if (mineQuestOffered)
                    opts.add(new String[]{"mine",      "A mine."});
                String[] btnIds = {"#BtnChoice1","#BtnChoice2","#BtnChoice3",
                                   "#BtnChoice4","#BtnChoice5"};
                for (int i = 0; i < opts.size() && i < btnIds.length; i++) {
                    b.set(btnIds[i] + ".Text", opts.get(i)[1]);
                    b.set(btnIds[i] + ".Visible", true);
                }
                // "Never mind" always in BtnChoice6 (dimmed style hardcoded in .ui)
                b.set("#BtnChoice6.Text", "Never mind.");
                b.set("#BtnChoice6.Visible", true);
            }
            case QUEST_WAREHOUSE -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "The farm is producing now. Good.\n" +
                        "But food sitting on the ground or scattered across chests is food that rots, " +
                        "gets stolen, or simply gets lost.\n" +
                        "Your farmer needs somewhere to put it. We should build a warehouse.");
                b.set("#BtnPrimary.Text", "What will that take?");
                b.set("#BtnPrimary.Visible", true);
            }
            case QUEST_WAREHOUSE_OFFER -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Somewhere central. People need to get to it easily.\n" +
                        "Place the counter where you want the front desk. I'll build around it.");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "Got it.");
                b.set("#BtnChoice1.Visible", true);
                b.set("#BtnChoice2.Text", "Not now.");
                b.set("#BtnChoice2.Visible", true);
            }
            case ANSWER_ELF -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Where do I start. Four centuries of building things for people who mostly didn't deserve them.\n" +
                        "Temples, keeps, granaries. A few things I actually believed in.\n" +
                        "Then the Sundering. City gone. I took my tools and walked until I stopped here.");
                b.set("#BtnPrimary.Text", "So you're still looking.");
                b.set("#BtnPrimary.Visible", true);
            }
            case ANSWER_ELF_2 -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "For a long time, yes. Land worth building on. Someone worth building with.\n" +
                        "At some point I stopped expecting either.\n" +
                        "Now I just... keep the tools close. Old habit, I suppose.");
                b.set("#BtnPrimary.Text", "Back");
                b.set("#BtnPrimary.Visible", true);
            }
            case ANSWER_STONE -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "I can put up walls. That's the part I know.\n" +
                        "But someone has to find the people. Bring the wood and stone. " +
                        "Make the calls when neither option is good.\n" +
                        "That part is yours.");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "Yes. Let's do it.");
                b.set("#BtnChoice1.Visible", true);
                b.set("#BtnChoice2.Text", "I need to think about it.");
                b.set("#BtnChoice2.Visible", true);
            }
            case ANSWER_STONE_OFFER -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Then find ground that works. Flat, water nearby, somewhere you can " +
                        "defend if it comes to that.\n" +
                        "Place the Founding Stone when you have the spot. " +
                        "I'll feel where it lands and come to you. " +
                        "Town Hall first. Everything else after.");
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
                            "Find a spot. Place the stone on the ground when you are ready. I will feel it.\n" +
                            "Town Hall first. Then the rest.");
                    b.set("#BtnPrimary.Text", "Right.");
                    b.set("#BtnPrimary.Visible", true);
                }
            }
            case ANSWER_REMIND_LOST -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Of course you did.\n" +
                        "Here. I carry spares for a reason.");
                b.set("#BtnPrimary.Text", "Thank you.");
                b.set("#BtnPrimary.Visible", true);
            }
            case POST_FOUNDED -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Stone is down. This place will do.\n" +
                        "Bring wood, rock, the rest. Set it all inside the stone.\n" +
                        "Once it is there, I will start on the Town Hall.");
                b.set("#BtnPrimary.Text", "On my way.");
                b.set("#BtnPrimary.Visible", true);
            }
            case QUEST_RESCUE -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "There are people still out in these woods. Some since the Sundering, nowhere left to go.\n" +
                        "I keep feeling like someone is out there. I cannot explain it.\n" +
                        "Find whoever needs help. Bring them back if they'll come.");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "I'll go look.");
                b.set("#BtnChoice1.Visible", true);
                b.set("#BtnChoice2.Text", "Not right now.");
                b.set("#BtnChoice2.Visible", true);
            }
            case QUEST_ACTIVE -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Not found them yet?\n" +
                        "Follow the marker. Come back when you do.");
                b.set("#BtnPrimary.Text", "Understood.");
                b.set("#BtnPrimary.Visible", true);
            }
            case QUEST_HOUSE -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "You brought someone back.\n" +
                        "They're sleeping outside. That holds for a night, maybe two.\n" +
                        "After that, they leave. Build them a house.");
                b.set("#BtnPrimary.Text", "What do I need?");
                b.set("#BtnPrimary.Visible", true);
            }
            case QUEST_HOUSE_OFFER -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "I'll build it. You find the spot.\n" +
                        "Put the brazier where the hearth should be. On the ground, inside where the walls will go.\n" +
                        "House goes up around it.");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "Got it.");
                b.set("#BtnChoice1.Visible", true);
                b.set("#BtnChoice2.Text", "Not now.");
                b.set("#BtnChoice2.Visible", true);
            }
            case QUEST_FARM -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "House is done.\n" +
                        "Food. One bad season and people leave. We've seen it before.\n" +
                        "We need a farm.");
                b.set("#BtnPrimary.Text", "How do we set it up?");
                b.set("#BtnPrimary.Visible", true);
            }
            case QUEST_FARM_OFFER -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Flat ground, near water if you can find it.\n" +
                        "Plant the scarecrow at the center. I'll lay everything out from there.");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "Got it.");
                b.set("#BtnChoice1.Visible", true);
                b.set("#BtnChoice2.Text", "Not now.");
                b.set("#BtnChoice2.Visible", true);
            }
            case QUEST_SAWMILL -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Warehouse is up.\n" +
                        "We can't keep hauling wood ourselves. We need people whose job that is.\n" +
                        "Sawmill.");
                b.set("#BtnPrimary.Text", "What do we need to build it?");
                b.set("#BtnPrimary.Visible", true);
            }
            case QUEST_SAWMILL_OFFER -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Find open ground. Workers need room.\n" +
                        "Set the stump where you want the center. I'll build out from there.");
                b.set("#ChoiceContainer.Visible", true);
                b.set("#BtnChoice1.Text", "Got it.");
                b.set("#BtnChoice1.Visible", true);
                b.set("#BtnChoice2.Text", "Not now.");
                b.set("#BtnChoice2.Visible", true);
            }
            case QUEST_MINE -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Sawmill's been running.\n" +
                        "Wood we have. Stone we don't. Not enough of it anyway.\n" +
                        "We need a mine. People to dig full time.");
                b.set("#BtnPrimary.Text", "How do we start?");
                b.set("#BtnPrimary.Visible", true);
            }
            case QUEST_MINE_OFFER -> {
                b.set("#SpeakerName.Text", "Aelin");
                b.set("#DialogText.Text",
                        "Pick somewhere away from the village. This will leave a big hole.\n" +
                        "Put the sign where you want the entrance. I'll dig from there.");
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
                    case "choice3" -> { if (townHallBuilt && houseBuilt) next = BUILD_MENU; }
                    case "choice6" -> { close(); return; }
                }
            }

            case BUILD_MENU -> {
                // Rebuild same ordered list as render to map choice index → building
                java.util.List<String> buildOpts = new java.util.ArrayList<>();
                buildOpts.add("house");
                if (farmQuestOffered)      buildOpts.add("farm");
                if (warehouseQuestOffered) buildOpts.add("warehouse");
                if (sawmillQuestOffered)   buildOpts.add("sawmill");
                if (mineQuestOffered)      buildOpts.add("mine");
                int choiceIdx = switch (action) {
                    case "choice1" -> 0;
                    case "choice2" -> 1;
                    case "choice3" -> 2;
                    case "choice4" -> 3;
                    case "choice5" -> 4;
                    case "choice6" -> -1; // Never mind
                    default -> -1;
                };
                if (choiceIdx >= 0 && choiceIdx < buildOpts.size()) {
                    switch (buildOpts.get(choiceIdx)) {
                        case "house"     -> { giveBrazier(ref, store);    close(); return; }
                        case "farm"      -> { giveScarecrow(ref, store);  close(); return; }
                        case "warehouse" -> { giveCounter(ref, store);    close(); return; }
                        case "sawmill"   -> { giveLumbermill(ref, store); close(); return; }
                        case "mine"      -> { giveMineSign(ref, store);   close(); return; }
                    }
                }
                next = MENU;
            }

            case QUEST_WAREHOUSE -> { if ("primary".equals(action)) next = QUEST_WAREHOUSE_OFFER; }

            case QUEST_WAREHOUSE_OFFER -> {
                if ("choice1".equals(action)) {
                    giveCounter(ref, store);
                    close();
                    return;
                } else if ("choice2".equals(action)) {
                    markWarehouseQuestOffered(ref, store);
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

            case QUEST_FARM -> { if ("primary".equals(action)) next = QUEST_FARM_OFFER; }

            case QUEST_FARM_OFFER -> {
                if ("choice1".equals(action)) {
                    giveScarecrow(ref, store);
                    close();
                    return;
                } else if ("choice2".equals(action)) {
                    // Mark offered so auto-open doesn't re-trigger next visit
                    markFarmQuestOffered(ref, store);
                    next = MENU;
                }
            }

            case QUEST_SAWMILL -> { if ("primary".equals(action)) next = QUEST_SAWMILL_OFFER; }

            case QUEST_SAWMILL_OFFER -> {
                if ("choice1".equals(action)) {
                    giveLumbermill(ref, store);
                    close();
                    return;
                } else if ("choice2".equals(action)) {
                    markSawmillQuestOffered(ref, store);
                    next = MENU;
                }
            }

            case QUEST_MINE -> { if ("primary".equals(action)) next = QUEST_MINE_OFFER; }

            case QUEST_MINE_OFFER -> {
                if ("choice1".equals(action)) {
                    giveMineSign(ref, store);
                    close();
                    return;
                } else if ("choice2".equals(action)) {
                    markMineQuestOffered(ref, store);
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

        VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
        RescueQuestManager.QuestVariant variant = RescueQuestManager.pickNextVariant(village);
        RescueQuestManager.recordVariantPlayed(village, variant);

        // Mark quest as started immediately so repeated dialog opens show QUEST_ACTIVE
        rescueQuestStarted = true;
        village.setRescueQuestStarted(true);
        VillageManager.get().save(store, ref, village);

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            LOGGER.warning("launchRescueQuest: player has no TransformComponent");
            return;
        }

        RescueQuestManager.startForPlayer(
                cachedWorld, store, ref, cachedPlayer, cachedPlayerUuid,
                transform.getPosition(), variant,
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

    private void giveScarecrow(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            var tx = player.getInventory().getCombinedHotbarFirst()
                    .addItemStack(new ItemStack(SCARECROW_ITEM_ID, 1));
            if (tx.succeeded()) {
                farmScarecrowGiven = true;
                farmQuestOffered = true;
                VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
                village.setFarmScarecrowGiven(true);
                village.setFarmQuestOffered(true);
                VillageManager.get().save(store, ref, village);
                LOGGER.info("Gave Scarecrow to " + playerName);
            } else {
                LOGGER.warning("Could not give Scarecrow to " + playerName + " — inventory full?");
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to give Scarecrow: " + e.getMessage());
        }
    }

    private void markFarmQuestOffered(Ref<EntityStore> ref, Store<EntityStore> store) {
        farmQuestOffered = true;
        VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
        village.setFarmQuestOffered(true);
        VillageManager.get().save(store, ref, village);
    }

    private void giveCounter(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            var tx = player.getInventory().getCombinedHotbarFirst()
                    .addItemStack(new ItemStack(COUNTER_ITEM_ID, 1));
            if (tx.succeeded()) {
                warehouseCounterGiven = true;
                warehouseQuestOffered = true;
                VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
                village.setWarehouseCounterGiven(true);
                village.setWarehouseQuestOffered(true);
                VillageManager.get().save(store, ref, village);
                LOGGER.info("Gave Warehouse Counter to " + playerName);
            } else {
                LOGGER.warning("Could not give Warehouse Counter to " + playerName + " — inventory full?");
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to give Warehouse Counter: " + e.getMessage());
        }
    }

    private void giveLumbermill(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            var tx = player.getInventory().getCombinedHotbarFirst()
                    .addItemStack(new ItemStack(LUMBERMILL_ITEM_ID, 1));
            if (tx.succeeded()) {
                sawmillQuestOffered = true;
                VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
                village.setSawmillQuestOffered(true);
                VillageManager.get().save(store, ref, village);
                LOGGER.info("Gave Lumbermill to " + playerName);
            } else {
                LOGGER.warning("Could not give Lumbermill to " + playerName + " — inventory full?");
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to give Lumbermill: " + e.getMessage());
        }
    }

    private void markSawmillQuestOffered(Ref<EntityStore> ref, Store<EntityStore> store) {
        sawmillQuestOffered = true;
        VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
        village.setSawmillQuestOffered(true);
        VillageManager.get().save(store, ref, village);
    }

    private void giveMineSign(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            var tx = player.getInventory().getCombinedHotbarFirst()
                    .addItemStack(new ItemStack(MINE_SIGN_ITEM_ID, 1));
            if (tx.succeeded()) {
                mineQuestOffered = true;
                VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
                village.setMineQuestOffered(true);
                VillageManager.get().save(store, ref, village);
                LOGGER.info("Gave Mine Sign to " + playerName);
            } else {
                LOGGER.warning("Could not give Mine Sign to " + playerName + " — inventory full?");
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to give Mine Sign: " + e.getMessage());
        }
    }

    private void markMineQuestOffered(Ref<EntityStore> ref, Store<EntityStore> store) {
        mineQuestOffered = true;
        VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
        village.setMineQuestOffered(true);
        VillageManager.get().save(store, ref, village);
    }

    private void markWarehouseQuestOffered(Ref<EntityStore> ref, Store<EntityStore> store) {
        warehouseQuestOffered = true;
        VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
        village.setWarehouseQuestOffered(true);
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

    private static boolean isBuilt(VillageData village, String type) {
        if (village == null) return false;
        dev.hearthbound.village.BuildingRecord b = village.findBuilding(type);
        return b != null && b.isCompleted();
    }

    private String nameReaction(String name) {
        char first = Character.toLowerCase(name.isEmpty() ? 'a' : name.charAt(0));
        if ("aeiou".indexOf(first) >= 0) {
            return name + ".\n" +
                   "I knew an Outlander general whose name began the same way. " +
                   "Difficult person. Built three keeps nobody asked for.\n" +
                   "Two of them are still standing. Make of that what you will.";
        } else {
            return name + ".\n" +
                   "I have spent enough years in places where names take longer to say " +
                   "than the conversation that follows. Yours suits you.\n" +
                   "This land has no name yet. That part is yours, if you want it.";
        }
    }
}
