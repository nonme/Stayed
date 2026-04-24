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

    // States
    public static final String STATE_IDLE = "idle";
    public static final String STATE_HOMELESS = "homeless";
    public static final String STATE_BUILDING = "building";
    public static final String STATE_WORKING = "working";

    // Races
    public static final String RACE_HUMAN = "human";
    public static final String RACE_KWEEBEC = "kweebec";
    public static final String RACE_TRORK = "trork";
    public static final String RACE_ELF = "elf";

    public static final BuilderCodec<VillagerData> CODEC = BuilderCodec.builder(VillagerData.class, VillagerData::new)
            .append(new KeyedCodec<>("Race", Codec.STRING), VillagerData::setRace, VillagerData::getRace).add()
            .append(new KeyedCodec<>("Profession", Codec.STRING), VillagerData::setProfession, VillagerData::getProfession).add()
            .append(new KeyedCodec<>("State", Codec.STRING), VillagerData::setState, VillagerData::getState).add()
            .append(new KeyedCodec<>("Happiness", Codec.INTEGER), VillagerData::setHappiness, VillagerData::getHappiness).add()
            .append(new KeyedCodec<>("FirstName", Codec.STRING), VillagerData::setFirstName, VillagerData::getFirstName).add()
            .append(new KeyedCodec<>("LastName", Codec.STRING), VillagerData::setLastName, VillagerData::getLastName).add()
            .append(new KeyedCodec<>("SkinSeed", Codec.LONG), VillagerData::setSkinSeed, VillagerData::getSkinSeed).add()
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
    private int happiness = 50;
    private String firstName = "";
    private String lastName = "";
    // 0 = unseeded (legacy). A non-zero seed means the skin has been rolled and must be
    // reproducible across server restarts by feeding this seed back into Random.
    private long skinSeed = 0L;

    public VillagerData() {}

    public VillagerData(String race, String firstName, String lastName, long skinSeed) {
        this.race = race;
        this.firstName = firstName;
        this.lastName = lastName;
        this.skinSeed = skinSeed;
        this.state = STATE_HOMELESS;
    }

    public String getRace() { return race; }
    public void setRace(String race) { this.race = race; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public int getHappiness() { return happiness; }
    public void setHappiness(int happiness) { this.happiness = Math.max(0, Math.min(100, happiness)); }

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

    public boolean hasHome() {
        return !STATE_HOMELESS.equals(state);
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
        copy.happiness = this.happiness;
        copy.firstName = this.firstName;
        copy.lastName = this.lastName;
        copy.skinSeed = this.skinSeed;
        return copy;
    }
}
