package dev.hearthbound.village;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Persistent data stored on each NPC entity that belongs to a village.
 * Tracks race, profession, happiness, housing status.
 */
public class VillagerData implements Component<EntityStore> {

    // Professions
    public static final String PROF_NONE = "none";
    public static final String PROF_LUMBERJACK = "lumberjack";
    public static final String PROF_MASON = "mason";
    public static final String PROF_FARMER = "farmer";

    // States (activity — orthogonal to housing status)
    public static final String STATE_IDLE = "idle";
    public static final String STATE_BUILDING = "building";
    public static final String STATE_WORKING = "working";

    // Races
    public static final String RACE_HUMAN = "human";
    public static final String RACE_KWEEBEC = "kweebec";
    public static final String RACE_TRORK = "trork";
    public static final String RACE_ELF = "elf";

    // Hunger thresholds (0 = full, 100 = very hungry)
    public static final int HUNGER_MODERATE = 40;
    public static final int HUNGER_SEVERE   = 70;

    // Happiness contributions
    public static final int HAPPINESS_BONUS_HOME      =  30;
    public static final int HAPPINESS_PENALTY_HUNGRY  = -10;
    public static final int HAPPINESS_PENALTY_STARVING = -30;

    public static final BuilderCodec<VillagerData> CODEC = BuilderCodec.builder(VillagerData.class, VillagerData::new)
            .append(new KeyedCodec<>("Race", Codec.STRING), VillagerData::setRace, VillagerData::getRace).add()
            .append(new KeyedCodec<>("Profession", Codec.STRING), VillagerData::setProfession, VillagerData::getProfession).add()
            .append(new KeyedCodec<>("State", Codec.STRING), VillagerData::setState, VillagerData::getState).add()
            .append(new KeyedCodec<>("Hunger", Codec.INTEGER), VillagerData::setHunger, VillagerData::getHunger).add()
            .append(new KeyedCodec<>("FirstName", Codec.STRING), VillagerData::setFirstName, VillagerData::getFirstName).add()
            .append(new KeyedCodec<>("LastName", Codec.STRING), VillagerData::setLastName, VillagerData::getLastName).add()
            .append(new KeyedCodec<>("SkinSeed", Codec.LONG), VillagerData::setSkinSeed, VillagerData::getSkinSeed).add()
            .append(new KeyedCodec<>("HasHouse", Codec.BOOLEAN), VillagerData::setHasHouse, VillagerData::isHasHouse).add()
            // Empty string = TRAP (legacy) or any pre-variant save — safe default.
            .append(new KeyedCodec<>("QuestVariant", Codec.STRING), VillagerData::setQuestVariant, VillagerData::getQuestVariant).add()
            // True once player reached the final dialog screen — reopen skips straight to Follow me.
            .append(new KeyedCodec<>("VillagerResqueDialogReachedFinal", Codec.BOOLEAN), VillagerData::setDialogReachedFinal, VillagerData::isDialogReachedFinal).add()
            .build();

    private static ComponentType<EntityStore, VillagerData> componentType;

    public static void register(ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(VillagerData.class, "VillagerData", CODEC);
    }

    public static ComponentType<EntityStore, VillagerData> getComponentType() {
        return componentType;
    }

    private String race = RACE_HUMAN;
    private String profession = PROF_NONE;
    private String state = STATE_IDLE;
    private boolean hasHouse = false;
    // Hunger: 0 = full, 100 = very hungry. Increases over time; reduced by farms (future).
    private int hunger = 0;
    private String firstName = "";
    private String lastName = "";
    // 0 = unseeded (legacy). A non-zero seed means the skin has been rolled and must be
    // reproducible across server restarts by feeding this seed back into Random.
    private long skinSeed = 0L;
    // Empty string = TRAP (legacy) or pre-variant save. CABIN/RUINS/CAMP stored by QuestVariant.name().
    private String questVariant = "";
    private boolean dialogReachedFinal = false;

    public VillagerData() {}

    public VillagerData(String race, String firstName, String lastName, long skinSeed) {
        this.race = race;
        this.firstName = firstName;
        this.lastName = lastName;
        this.skinSeed = skinSeed;
        // hasHouse stays false — newly created villager has no home yet
    }

    public boolean isHasHouse() { return hasHouse; }
    public void setHasHouse(boolean hasHouse) { this.hasHouse = hasHouse; }

    public String getRace() { return race; }
    public void setRace(String race) { this.race = race; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public int getHunger() { return hunger; }
    public void setHunger(int hunger) { this.hunger = Math.max(0, Math.min(100, hunger)); }

    public boolean isHungry()   { return hunger >= HUNGER_MODERATE; }
    public boolean isStarving() { return hunger >= HUNGER_SEVERE; }

    /**
     * Computed happiness from -100 to 100.
     * Base is 0; bonuses/penalties stack on top.
     */
    public int getHappiness() {
        int h = 0;
        if (hasHome()) h += HAPPINESS_BONUS_HOME;
        if (isStarving()) h += HAPPINESS_PENALTY_STARVING;
        else if (isHungry()) h += HAPPINESS_PENALTY_HUNGRY;
        return Math.max(-100, Math.min(100, h));
    }

    public String getHappinessLabel() {
        int h = getHappiness();
        if (h <= -50) return "Miserable";
        if (h <= -15) return "Unhappy";
        if (h <   15) return "Content";
        if (h <   50) return "Happy";
        return "Thriving";
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() {
        if (firstName.isEmpty() && lastName.isEmpty()) return "";
        if (firstName.isEmpty()) return lastName;
        if (lastName.isEmpty()) return firstName;
        return firstName + " " + lastName;
    }

    public long getSkinSeed() { return skinSeed; }
    public void setSkinSeed(long skinSeed) { this.skinSeed = skinSeed; }

    public String getQuestVariant() { return questVariant; }
    public void setQuestVariant(String questVariant) { this.questVariant = questVariant != null ? questVariant : ""; }

    public boolean isDialogReachedFinal() { return dialogReachedFinal; }
    public void setDialogReachedFinal(boolean dialogReachedFinal) { this.dialogReachedFinal = dialogReachedFinal; }

    public boolean hasHome() {
        return hasHouse;
    }

    public boolean canAssignProfession() {
        return hasHome() && PROF_NONE.equals(profession);
    }

    @Override
    public Component<EntityStore> clone() {
        VillagerData copy = new VillagerData();
        copy.race = this.race;
        copy.profession = this.profession;
        copy.state = this.state;
        copy.hasHouse = this.hasHouse;
        copy.hunger = this.hunger;
        copy.firstName = this.firstName;
        copy.lastName = this.lastName;
        copy.skinSeed = this.skinSeed;
        copy.questVariant = this.questVariant;
        // dialogReachedFinal intentionally not copied — follower/villager starts fresh
        return copy;
    }
}
