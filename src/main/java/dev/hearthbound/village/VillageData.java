package dev.hearthbound.village;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.array.IntArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent village data stored on the player entity ref.
 * One village per player for MVP.
 */
public class VillageData implements Component<EntityStore> {

    private static final ArrayCodec<BuildingRecord> BUILDINGS_ARRAY_CODEC =
            ArrayCodec.ofBuilderCodec(BuildingRecord.CODEC, BuildingRecord[]::new);

    private static final ArrayCodec<VillagerSummary> VILLAGERS_ARRAY_CODEC =
            ArrayCodec.ofBuilderCodec(VillagerSummary.CODEC, VillagerSummary[]::new);

    private static final ArrayCodec<String> STRING_ARRAY_CODEC =
            new ArrayCodec<>(Codec.STRING, String[]::new);

    private static final IntArrayCodec INT_ARRAY_CODEC = new IntArrayCodec();

    public static final BuilderCodec<VillageData> CODEC = BuilderCodec.builder(VillageData.class, VillageData::new)
            .append(new KeyedCodec<>("VillageName", Codec.STRING), VillageData::setVillageName, VillageData::getVillageName).add()
            .append(new KeyedCodec<>("Stage", Codec.INTEGER), VillageData::setStage, VillageData::getStage).add()
            .append(new KeyedCodec<>("FoundingStoneX", Codec.INTEGER), VillageData::setFoundingStoneX, VillageData::getFoundingStoneX).add()
            .append(new KeyedCodec<>("FoundingStoneY", Codec.INTEGER), VillageData::setFoundingStoneY, VillageData::getFoundingStoneY).add()
            .append(new KeyedCodec<>("FoundingStoneZ", Codec.INTEGER), VillageData::setFoundingStoneZ, VillageData::getFoundingStoneZ).add()
            .append(new KeyedCodec<>("FoundedAtTick", Codec.LONG), VillageData::setFoundedAtTick, VillageData::getFoundedAtTick).add()
            .append(new KeyedCodec<>("ElfId", Codec.UUID_STRING), VillageData::setElfId, VillageData::getElfId).add()
            .append(new KeyedCodec<>("ElfName", Codec.STRING), VillageData::setElfName, VillageData::getElfName).add()
            .append(new KeyedCodec<>("Buildings", BUILDINGS_ARRAY_CODEC), VillageData::setBuildingsArray, VillageData::getBuildingsArray).add()
            .append(new KeyedCodec<>("Villagers", VILLAGERS_ARRAY_CODEC), VillageData::setVillagersArray, VillageData::getVillagersArray).add()
            .append(new KeyedCodec<>("Rotation", Codec.INTEGER), VillageData::setRotation, VillageData::getRotation).add()
            .append(new KeyedCodec<>("ConstructionStarted", Codec.BOOLEAN), VillageData::setConstructionStarted, VillageData::isConstructionStarted).add()
            .append(new KeyedCodec<>("MetElf", Codec.BOOLEAN), VillageData::setMetElf, VillageData::isMetElf).add()
            .append(new KeyedCodec<>("FoundingStoneGiven", Codec.BOOLEAN), VillageData::setFoundingStoneGiven, VillageData::isFoundingStoneGiven).add()
            .append(new KeyedCodec<>("RescueQuestStarted", Codec.BOOLEAN), VillageData::setRescueQuestStarted, VillageData::isRescueQuestStarted).add()
            .append(new KeyedCodec<>("HouseBrazierGiven", Codec.BOOLEAN), VillageData::setHouseBrazierGiven, VillageData::isHouseBrazierGiven).add()
            .append(new KeyedCodec<>("HouseQuestOffered", Codec.BOOLEAN), VillageData::setHouseQuestOffered, VillageData::isHouseQuestOffered).add()
            .append(new KeyedCodec<>("FarmScarecrowGiven", Codec.BOOLEAN), VillageData::setFarmScarecrowGiven, VillageData::isFarmScarecrowGiven).add()
            .append(new KeyedCodec<>("FarmQuestOffered", Codec.BOOLEAN), VillageData::setFarmQuestOffered, VillageData::isFarmQuestOffered).add()
            .append(new KeyedCodec<>("WarehouseCounterGiven", Codec.BOOLEAN), VillageData::setWarehouseCounterGiven, VillageData::isWarehouseCounterGiven).add()
            .append(new KeyedCodec<>("WarehouseQuestOffered", Codec.BOOLEAN), VillageData::setWarehouseQuestOffered, VillageData::isWarehouseQuestOffered).add()
            .append(new KeyedCodec<>("SawmillQuestOffered", Codec.BOOLEAN), VillageData::setSawmillQuestOffered, VillageData::isSawmillQuestOffered).add()
            .append(new KeyedCodec<>("MineQuestOffered", Codec.BOOLEAN), VillageData::setMineQuestOffered, VillageData::isMineQuestOffered).add()
            .append(new KeyedCodec<>("RescueQuestTrapDone", Codec.BOOLEAN), VillageData::setRescueQuestTrapDone, VillageData::isRescueQuestTrapDone).add()
            .append(new KeyedCodec<>("RescueQuestHistory", STRING_ARRAY_CODEC), VillageData::setRescueQuestHistoryArray, VillageData::getRescueQuestHistoryArray).add()
            .append(new KeyedCodec<>("RescueQuestSiteX", INT_ARRAY_CODEC), VillageData::setRescueQuestSiteXArray, VillageData::getRescueQuestSiteXArray).add()
            .append(new KeyedCodec<>("RescueQuestSiteZ", INT_ARRAY_CODEC), VillageData::setRescueQuestSiteZArray, VillageData::getRescueQuestSiteZArray).add()
            .append(new KeyedCodec<>("SelectedHouseVariant", Codec.INTEGER), VillageData::setSelectedHouseVariant, VillageData::getSelectedHouseVariant).add()
            .build();

    private static ComponentType<EntityStore, VillageData> componentType;

    public static void register(ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(VillageData.class, "VillageData", CODEC);
    }

    public static ComponentType<EntityStore, VillageData> getComponentType() {
        return componentType;
    }

    // Village stages
    public static final int STAGE_NONE = 0;
    public static final int STAGE_FOUNDED = 1;
    public static final int STAGE_TOWN_HALL = 2;
    public static final int STAGE_WAREHOUSE = 3;

    private String villageName = "";
    private int stage = STAGE_NONE;
    private int foundingStoneX, foundingStoneY, foundingStoneZ;
    private long foundedAtTick = 0;
    private UUID elfId;
    private String elfName;
    private List<BuildingRecord> buildings = new ArrayList<>();
    private List<VillagerSummary> villagers = new ArrayList<>();
    private int rotation = 0; // 0=N, 1=E, 2=S, 3=W (matches VariantRotation NESW index)
    private boolean constructionStarted = false;
    private boolean metElf = false;
    private boolean foundingStoneGiven = false;
    private boolean rescueQuestStarted = false;
    private boolean houseBrazierGiven = false;
    private boolean houseQuestOffered = false;
    private boolean farmScarecrowGiven = false;
    private boolean farmQuestOffered = false;
    private boolean warehouseCounterGiven = false;
    private boolean warehouseQuestOffered = false;
    private boolean sawmillQuestOffered = false;
    private boolean mineQuestOffered = false;
    private boolean rescueQuestTrapDone = false;
    private List<String> rescueQuestHistory = new ArrayList<>();
    /**
     * Centers (block X/Z) of every rescue-quest structure already spawned for this village.
     * Used to reject new spawn candidates that would overlap an existing site.
     * Old saves with no field set deserialize to empty arrays — first quest still spawns normally.
     */
    private List<int[]> rescueQuestSites = new ArrayList<>();
    /**
     * Last house variant the player picked while placing a Brazier. Persists between
     * placements so re-placing a brazier shows the same variant the player liked.
     * 0 matches the original VillagerHouse_lvl1_v1 prefab — old saves with no field set
     * keep rendering the same building they always did.
     */
    private int selectedHouseVariant = 0;
    /**
     * Snapshot of blocks overwritten by the active ghost preview, keyed by "x,y,z" → original
     * block id ("Empty" for empty cells). Persisted so a server restart doesn't orphan the
     * ghost — the player can break the Founding Stone and we restore the original terrain.
     */
    private Map<String, String> pendingGhostSnapshot = new HashMap<>();

    public VillageData() {}

    // --- Village name ---
    public String getVillageName() { return villageName; }
    public void setVillageName(String villageName) { this.villageName = villageName; }

    // --- Stage ---
    public int getStage() { return stage; }
    public void setStage(int stage) { this.stage = stage; }
    public boolean isFounded() { return stage >= STAGE_FOUNDED; }

    // --- Founding stone position ---
    public int getFoundingStoneX() { return foundingStoneX; }
    public void setFoundingStoneX(int x) { this.foundingStoneX = x; }
    public int getFoundingStoneY() { return foundingStoneY; }
    public void setFoundingStoneY(int y) { this.foundingStoneY = y; }
    public int getFoundingStoneZ() { return foundingStoneZ; }
    public void setFoundingStoneZ(int z) { this.foundingStoneZ = z; }

    public void setFoundingStonePos(int x, int y, int z) {
        this.foundingStoneX = x;
        this.foundingStoneY = y;
        this.foundingStoneZ = z;
    }

    // --- Founded tick ---
    public long getFoundedAtTick() { return foundedAtTick; }
    public void setFoundedAtTick(long tick) { this.foundedAtTick = tick; }

    // --- Elf ---
    public UUID getElfId() { return elfId; }
    public void setElfId(UUID elfId) { this.elfId = elfId; }
    public String getElfName() { return elfName; }
    public void setElfName(String elfName) { this.elfName = elfName; }

    // --- Buildings (array codec bridge) ---
    public List<BuildingRecord> getBuildings() { return buildings; }

    private BuildingRecord[] getBuildingsArray() {
        return buildings.toArray(new BuildingRecord[0]);
    }

    private void setBuildingsArray(BuildingRecord[] arr) {
        this.buildings = arr != null ? new ArrayList<>(Arrays.asList(arr)) : new ArrayList<>();
    }

    public void addBuilding(BuildingRecord building) {
        buildings.add(building);
    }

    // --- Villagers ---
    public List<VillagerSummary> getVillagers() { return villagers; }

    public void addVillager(VillagerSummary summary) { villagers.add(summary); }

    public int getVillagerCount() { return villagers.size(); }

    private VillagerSummary[] getVillagersArray() { return villagers.toArray(new VillagerSummary[0]); }

    private void setVillagersArray(VillagerSummary[] arr) {
        this.villagers = arr != null ? new ArrayList<>(Arrays.asList(arr)) : new ArrayList<>();
    }

    // --- Rotation ---
    public int getRotation() { return rotation; }
    public void setRotation(int rotation) { this.rotation = rotation; }

    // --- Construction started ---
    public boolean isConstructionStarted() { return constructionStarted; }
    public void setConstructionStarted(boolean started) { this.constructionStarted = started; }

    // --- Elf meeting ---
    public boolean isMetElf() { return metElf; }
    public void setMetElf(boolean metElf) { this.metElf = metElf; }

    // --- Founding stone given ---
    public boolean isFoundingStoneGiven() { return foundingStoneGiven; }
    public void setFoundingStoneGiven(boolean given) { this.foundingStoneGiven = given; }

    // --- Rescue quest ---
    public boolean isRescueQuestStarted() { return rescueQuestStarted; }
    public void setRescueQuestStarted(boolean started) { this.rescueQuestStarted = started; }

    // --- House quest ---
    public boolean isHouseBrazierGiven() { return houseBrazierGiven; }
    public void setHouseBrazierGiven(boolean given) { this.houseBrazierGiven = given; }
    public boolean isHouseQuestOffered() { return houseQuestOffered; }
    public void setHouseQuestOffered(boolean offered) { this.houseQuestOffered = offered; }

    // --- Farm quest ---
    public boolean isFarmScarecrowGiven() { return farmScarecrowGiven; }
    public void setFarmScarecrowGiven(boolean given) { this.farmScarecrowGiven = given; }
    public boolean isFarmQuestOffered() { return farmQuestOffered; }
    public void setFarmQuestOffered(boolean offered) { this.farmQuestOffered = offered; }

    // --- Warehouse quest ---
    public boolean isWarehouseCounterGiven() { return warehouseCounterGiven; }
    public void setWarehouseCounterGiven(boolean given) { this.warehouseCounterGiven = given; }
    public boolean isWarehouseQuestOffered() { return warehouseQuestOffered; }
    public void setWarehouseQuestOffered(boolean offered) { this.warehouseQuestOffered = offered; }
    public boolean isSawmillQuestOffered() { return sawmillQuestOffered; }
    public void setSawmillQuestOffered(boolean offered) { this.sawmillQuestOffered = offered; }
    public boolean isMineQuestOffered() { return mineQuestOffered; }
    public void setMineQuestOffered(boolean offered) { this.mineQuestOffered = offered; }

    // --- Rescue quest rotation ---
    public boolean isRescueQuestTrapDone() { return rescueQuestTrapDone; }
    public void setRescueQuestTrapDone(boolean done) { this.rescueQuestTrapDone = done; }

    public List<String> getRescueQuestHistory() { return rescueQuestHistory; }

    private String[] getRescueQuestHistoryArray() {
        return rescueQuestHistory.toArray(new String[0]);
    }

    private void setRescueQuestHistoryArray(String[] arr) {
        this.rescueQuestHistory = arr != null ? new ArrayList<>(Arrays.asList(arr)) : new ArrayList<>();
    }

    public List<int[]> getRescueQuestSites() { return rescueQuestSites; }

    public void addRescueQuestSite(int x, int z) {
        rescueQuestSites.add(new int[]{x, z});
    }

    private int[] getRescueQuestSiteXArray() {
        int[] out = new int[rescueQuestSites.size()];
        for (int i = 0; i < out.length; i++) out[i] = rescueQuestSites.get(i)[0];
        return out;
    }

    private int[] getRescueQuestSiteZArray() {
        int[] out = new int[rescueQuestSites.size()];
        for (int i = 0; i < out.length; i++) out[i] = rescueQuestSites.get(i)[1];
        return out;
    }

    // Codec calls SiteX setter first, then SiteZ. SiteX seeds the list with x,0 placeholders;
    // SiteZ fills in z. If lengths differ (corrupt data), excess entries keep their 0 default.
    private void setRescueQuestSiteXArray(int[] arr) {
        this.rescueQuestSites = new ArrayList<>();
        if (arr == null) return;
        for (int x : arr) rescueQuestSites.add(new int[]{x, 0});
    }

    private void setRescueQuestSiteZArray(int[] arr) {
        if (arr == null) return;
        int n = Math.min(arr.length, rescueQuestSites.size());
        for (int i = 0; i < n; i++) rescueQuestSites.get(i)[1] = arr[i];
    }

    // --- Selected house variant ---
    public int getSelectedHouseVariant() { return selectedHouseVariant; }
    public void setSelectedHouseVariant(int variant) { this.selectedHouseVariant = variant; }

    // --- Pending ghost snapshot (persisted so ghost preview survives server restart) ---
    public Map<String, String> getPendingGhostSnapshot() { return pendingGhostSnapshot; }
    public void setPendingGhostSnapshot(Map<String, String> snapshot) {
        this.pendingGhostSnapshot = snapshot != null ? new HashMap<>(snapshot) : new HashMap<>();
    }
    /** Hands the map to the codec as a {@link HashMap} so encoding stays generic over Map types. */
    @SuppressWarnings("unchecked")
    public HashMap<String, String> getPendingGhostSnapshotForCodec() {
        return pendingGhostSnapshot instanceof HashMap<?, ?>
                ? (HashMap<String, String>) pendingGhostSnapshot
                : new HashMap<>(pendingGhostSnapshot);
    }

    public BuildingRecord findBuilding(String type) {
        for (BuildingRecord b : buildings) {
            if (type.equals(b.getType())) return b;
        }
        return null;
    }

    @Override
    public Component<EntityStore> clone() {
        VillageData copy = new VillageData();
        copy.villageName = this.villageName;
        copy.stage = this.stage;
        copy.foundingStoneX = this.foundingStoneX;
        copy.foundingStoneY = this.foundingStoneY;
        copy.foundingStoneZ = this.foundingStoneZ;
        copy.foundedAtTick = this.foundedAtTick;
        copy.elfId = this.elfId;
        copy.buildings = new ArrayList<>(this.buildings);
        copy.villagers = new ArrayList<>(this.villagers);
        copy.rotation = this.rotation;
        copy.constructionStarted = this.constructionStarted;
        copy.metElf = this.metElf;
        copy.foundingStoneGiven = this.foundingStoneGiven;
        copy.rescueQuestStarted = this.rescueQuestStarted;
        copy.houseBrazierGiven = this.houseBrazierGiven;
        copy.houseQuestOffered = this.houseQuestOffered;
        copy.farmScarecrowGiven = this.farmScarecrowGiven;
        copy.farmQuestOffered = this.farmQuestOffered;
        copy.warehouseCounterGiven = this.warehouseCounterGiven;
        copy.warehouseQuestOffered = this.warehouseQuestOffered;
        copy.sawmillQuestOffered = this.sawmillQuestOffered;
        copy.mineQuestOffered = this.mineQuestOffered;
        copy.rescueQuestTrapDone = this.rescueQuestTrapDone;
        copy.rescueQuestHistory = new ArrayList<>(this.rescueQuestHistory);
        copy.rescueQuestSites = new ArrayList<>();
        for (int[] site : this.rescueQuestSites) copy.rescueQuestSites.add(site.clone());
        copy.pendingGhostSnapshot = new HashMap<>(this.pendingGhostSnapshot);
        copy.selectedHouseVariant = this.selectedHouseVariant;
        return copy;
    }
}
