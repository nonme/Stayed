package dev.hearthbound.village;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.UUID;

/**
 * Persistent summary of a villager stored inside VillageData.
 * Kept here so the Town Hall UI can list all residents even when their NPC chunks are unloaded.
 */
public class VillagerSummary {

    public static final BuilderCodec<VillagerSummary> CODEC =
            BuilderCodec.builder(VillagerSummary.class, VillagerSummary::new)
                    .append(new KeyedCodec<>("FirstName", Codec.STRING), VillagerSummary::setFirstName, VillagerSummary::getFirstName).add()
                    .append(new KeyedCodec<>("LastName", Codec.STRING), VillagerSummary::setLastName, VillagerSummary::getLastName).add()
                    .append(new KeyedCodec<>("Race", Codec.STRING), VillagerSummary::setRace, VillagerSummary::getRace).add()
                    .append(new KeyedCodec<>("Profession", Codec.STRING), VillagerSummary::setProfession, VillagerSummary::getProfession).add()
                    .append(new KeyedCodec<>("VillagerUuid", Codec.UUID_STRING), VillagerSummary::setVillagerUuid, VillagerSummary::getVillagerUuid).add()
                    .append(new KeyedCodec<>("SkinSeed", Codec.LONG), VillagerSummary::setSkinSeed, VillagerSummary::getSkinSeed).add()
                    // Mirrors of VillagerData fields kept in summary so the Almanac and other UIs can
                    // surface complaints (no house / hungry / starving) without loading every NPC chunk.
                    // Defaults are intentionally "content": false/0 means an old save shows no complaints
                    // until the next VillageTickHandler tick refreshes the mirrors.
                    .append(new KeyedCodec<>("HasHouse", Codec.BOOLEAN), VillagerSummary::setHasHouse, VillagerSummary::isHasHouse).add()
                    .append(new KeyedCodec<>("Hunger", Codec.INTEGER), VillagerSummary::setHunger, VillagerSummary::getHunger).add()
                    .build();

    private String firstName = "";
    private String lastName = "";
    private String race = VillagerData.RACE_HUMAN;
    private String profession = VillagerData.PROF_NONE;
    private UUID villagerUuid;
    private long skinSeed = 0L;
    private boolean hasHouse = false;
    private int hunger = 0;

    public VillagerSummary() {}

    public VillagerSummary(VillagerData data) {
        this.firstName = data.getFirstName();
        this.lastName = data.getLastName();
        this.race = data.getRace();
        this.profession = data.getProfession();
        this.skinSeed = data.getSkinSeed();
        this.hasHouse = data.isHasHouse();
        this.hunger = data.getHunger();
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getRace() { return race; }
    public void setRace(String race) { this.race = race; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    public UUID getVillagerUuid() { return villagerUuid; }
    public void setVillagerUuid(UUID villagerUuid) { this.villagerUuid = villagerUuid; }

    public long getSkinSeed() { return skinSeed; }
    public void setSkinSeed(long skinSeed) { this.skinSeed = skinSeed; }

    public boolean isHasHouse() { return hasHouse; }
    public void setHasHouse(boolean hasHouse) { this.hasHouse = hasHouse; }

    public int getHunger() { return hunger; }
    public void setHunger(int hunger) { this.hunger = Math.max(0, Math.min(100, hunger)); }

    public boolean isHungry()   { return hunger >= VillagerData.HUNGER_MODERATE; }
    public boolean isStarving() { return hunger >= VillagerData.HUNGER_SEVERE; }

    public String getFullName() {
        if (firstName.isEmpty() && lastName.isEmpty()) return "Unknown";
        if (firstName.isEmpty()) return lastName;
        if (lastName.isEmpty()) return firstName;
        return firstName + " " + lastName;
    }
}
