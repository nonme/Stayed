package dev.hearthbound.village;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.Object2IntMapCodec;
import com.hypixel.hytale.math.vector.Vector3i;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class BuildingRecord {

    private static final Object2IntMapCodec<String> STORAGE_CODEC =
            new Object2IntMapCodec<>(Codec.STRING, Object2IntOpenHashMap::new, false);

    public static final BuilderCodec<BuildingRecord> CODEC = BuilderCodec.builder(BuildingRecord.class, BuildingRecord::new)
            .append(new KeyedCodec<>("Type", Codec.STRING), BuildingRecord::setType, BuildingRecord::getType).add()
            .append(new KeyedCodec<>("PosX", Codec.INTEGER), BuildingRecord::setPosX, BuildingRecord::getPosX).add()
            .append(new KeyedCodec<>("PosY", Codec.INTEGER), BuildingRecord::setPosY, BuildingRecord::getPosY).add()
            .append(new KeyedCodec<>("PosZ", Codec.INTEGER), BuildingRecord::setPosZ, BuildingRecord::getPosZ).add()
            .append(new KeyedCodec<>("Rotation", Codec.INTEGER), BuildingRecord::setRotation, BuildingRecord::getRotation).add()
            .append(new KeyedCodec<>("BuildProgress", Codec.INTEGER), BuildingRecord::setBuildProgress, BuildingRecord::getBuildProgress).add()
            .append(new KeyedCodec<>("Completed", Codec.BOOLEAN), BuildingRecord::setCompleted, BuildingRecord::isCompleted).add()
            .append(new KeyedCodec<>("AssignedVillager", Codec.UUID_STRING), BuildingRecord::setAssignedVillagerId, BuildingRecord::getAssignedVillagerId).add()
            .append(new KeyedCodec<>("Storage", STORAGE_CODEC), BuildingRecord::setStorage, BuildingRecord::getStorage).add()
            .append(new KeyedCodec<>("Variant", Codec.INTEGER), BuildingRecord::setVariant, BuildingRecord::getVariant).add()
            .append(new KeyedCodec<>("TestMarker", Codec.STRING), BuildingRecord::setTestMarker, BuildingRecord::getTestMarker).add()
            .build();

    private String type = "";
    private int posX, posY, posZ;
    private int rotation = 0;
    private int buildProgress = 0;
    private boolean completed = false;
    private java.util.UUID assignedVillagerId;
    private Object2IntMap<String> storage = new Object2IntOpenHashMap<>();
    /**
     * Prefab variant for buildings that have multiple alternatives (currently HOUSE_HUMAN).
     * 0 = original prefab, matches old saves where this field is absent.
     */
    private int variant = 0;

    /**
     * Non-null only on buildings created by the integration test framework
     * (FakeBuildingStep). Lets cleanup steps remove just the test buildings
     * without touching real player constructions.
     */
    private String testMarker = null;

    public BuildingRecord() {}

    public BuildingRecord(String type, int posX, int posY, int posZ) {
        this.type = type;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getPosX() { return posX; }
    public void setPosX(int posX) { this.posX = posX; }
    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; }
    public int getPosZ() { return posZ; }
    public void setPosZ(int posZ) { this.posZ = posZ; }

    public int getRotation() { return rotation; }
    public void setRotation(int rotation) { this.rotation = rotation; }

    public Vector3i getPosition() { return new Vector3i(posX, posY, posZ); }

    public int getBuildProgress() { return buildProgress; }
    public void setBuildProgress(int buildProgress) { this.buildProgress = buildProgress; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public java.util.UUID getAssignedVillagerId() { return assignedVillagerId; }
    public void setAssignedVillagerId(java.util.UUID id) { this.assignedVillagerId = id; }

    public Object2IntMap<String> getStorage() { return storage; }
    public void setStorage(Object2IntMap<String> storage) {
        this.storage = storage != null ? new Object2IntOpenHashMap<>(storage) : new Object2IntOpenHashMap<>();
    }

    public int getVariant() { return variant; }
    public void setVariant(int variant) { this.variant = variant; }

    public String getTestMarker() { return testMarker; }
    public void setTestMarker(String testMarker) { this.testMarker = testMarker; }

    /** Adds {@code amount} of {@code itemId} to the local storage. Returns the new total. */
    public int addResource(String itemId, int amount) {
        if (amount <= 0) return storage.getInt(itemId);
        int updated = storage.getInt(itemId) + amount;
        storage.put(itemId, updated);
        return updated;
    }

    /** Removes up to {@code amount} of {@code itemId}. Returns the amount actually removed. */
    public int removeResource(String itemId, int amount) {
        if (amount <= 0) return 0;
        int have = storage.getInt(itemId);
        int taken = Math.min(have, amount);
        int remaining = have - taken;
        if (remaining == 0) storage.removeInt(itemId);
        else storage.put(itemId, remaining);
        return taken;
    }

    public int getResourceCount(String itemId) {
        return storage.getInt(itemId);
    }
}
