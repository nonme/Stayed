package dev.hearthbound.village;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.ObjectMapCodec;
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
import java.util.function.Function;

/**
 * Persistent village data stored on the player entity ref.
 * One village per player for MVP.
 */
public class VillageData implements Component<EntityStore> {

    private static final ArrayCodec<BuildingRecord> BUILDINGS_ARRAY_CODEC =
            ArrayCodec.ofBuilderCodec(BuildingRecord.CODEC, BuildingRecord[]::new);

    private static final ObjectMapCodec<String, String, HashMap<String, String>> GHOST_SNAPSHOT_CODEC =
            new ObjectMapCodec<>(Codec.STRING, HashMap::new, Function.identity(), Function.identity(), false);

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
            .append(new KeyedCodec<>("VillagerCount", Codec.INTEGER), VillageData::setVillagerCount, VillageData::getVillagerCount).add()
            .append(new KeyedCodec<>("Rotation", Codec.INTEGER), VillageData::setRotation, VillageData::getRotation).add()
            .append(new KeyedCodec<>("ConstructionStarted", Codec.BOOLEAN), VillageData::setConstructionStarted, VillageData::isConstructionStarted).add()
            .append(new KeyedCodec<>("MetElf", Codec.BOOLEAN), VillageData::setMetElf, VillageData::isMetElf).add()
            .append(new KeyedCodec<>("FoundingStoneGiven", Codec.BOOLEAN), VillageData::setFoundingStoneGiven, VillageData::isFoundingStoneGiven).add()
            .append(new KeyedCodec<>("PendingGhostSnapshot", GHOST_SNAPSHOT_CODEC),
                    VillageData::setPendingGhostSnapshot, VillageData::getPendingGhostSnapshotForCodec).add()
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
    private int villagerCount = 0;
    private int rotation = 0; // 0=N, 1=E, 2=S, 3=W (matches VariantRotation NESW index)
    private boolean constructionStarted = false;
    private boolean metElf = false;
    private boolean foundingStoneGiven = false;
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

    // --- Villager count ---
    public int getVillagerCount() { return villagerCount; }
    public void setVillagerCount(int count) { this.villagerCount = count; }

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
        copy.villagerCount = this.villagerCount;
        copy.rotation = this.rotation;
        copy.constructionStarted = this.constructionStarted;
        copy.metElf = this.metElf;
        copy.foundingStoneGiven = this.foundingStoneGiven;
        copy.pendingGhostSnapshot = new HashMap<>(this.pendingGhostSnapshot);
        return copy;
    }
}
