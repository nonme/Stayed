/*
 * Decompiled with CFR 0.152.
 */
package com.yourname.companion.data;

import com.yourname.companion.data.BlockPos;
import com.yourname.companion.data.CompanionMode;
import com.yourname.companion.data.EquipmentSlot;
import com.yourname.companion.data.FallenCompanion;
import com.yourname.companion.data.FollowMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CompanionRecord {
    public String uniqueId;
    public String name;
    public String appearanceModelId;
    public String savedPlayerSkinToken;
    public String savedModelAssetId;
    public String savedModelComponentJson;
    public String savedPersistentModelJson;
    public String appearanceSource;
    public long appearanceLastUpdatedUtc;
    public long skinSeed;
    public String sourceRoleName;
    public CompanionMode mode = CompanionMode.FIGHTER;
    public FollowMode followMode = FollowMode.FOLLOW;
    public int combatLevel = 1;
    public int combatKills = 0;
    public boolean lootModeEnabled = false;
    public int farmLevel = 1;
    public int farmHarvests = 0;
    public boolean farmAutoResume = false;
    public List<BlockPos> linkedChests = new ArrayList<BlockPos>();
    public Map<String, Integer> farmCollectedByCrop = new HashMap<String, Integer>();
    public Map<String, Integer> farmEternalSeedsGrantedByCrop = new HashMap<String, Integer>();
    public BlockPos farmAreaTopLeft;
    public BlockPos farmAreaBottomRight;
    public boolean farmDepositPending = false;
    public String farmStatusText;
    public int mineLevel = 1;
    public int mineBlocks = 0;
    public String equippedWeapon;
    public String equippedOffhand;
    public String equippedHelmet;
    public String equippedChest;
    public String equippedLegs;
    public String equippedBoots;
    public Map<String, Integer> savedInventory = new LinkedHashMap<String, Integer>();
    public List<String[]> savedInventoryStacks = new ArrayList<String[]>();
    public boolean startingGearGiven = false;
    public transient String entityId;
    public boolean active = false;
    public boolean fallen = false;
    public int deathCount = 0;
    public String deathCause;
    public long deathTime;
    public BlockPos lastKnownLocation;

    public CompanionRecord() {
    }

    public CompanionRecord(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getDisplayName() {
        return this.name != null && !this.name.isBlank() ? this.name : "Unnamed Companion";
    }

    public String getDisplayName(String ownerUsername) {
        if (this.name != null && !this.name.isBlank()) {
            return this.name;
        }
        return ownerUsername + "'s Companion";
    }

    public String getStatusTag() {
        if (this.fallen) {
            return "FALLEN";
        }
        if (this.active) {
            return "ACTIVE";
        }
        return "STORED";
    }

    public String getStatusSummary() {
        String roleName = switch (this.mode) {
            default -> throw new MatchException(null, null);
            case CompanionMode.FIGHTER -> "Fighter";
            case CompanionMode.FARMER -> "Farmer";
            case CompanionMode.MINER -> "Miner";
        };
        String stanceName = switch (this.followMode) {
            default -> throw new MatchException(null, null);
            case FollowMode.FOLLOW -> "Follow";
            case FollowMode.STAY -> "Stay";
            case FollowMode.PATROL -> "Patrol";
            case FollowMode.FREE -> "Free";
        };
        String inv = this.savedInventory != null && !this.savedInventory.isEmpty() ? " (" + this.savedInventory.size() + " item type(s))" : "";
        String base = "[" + this.getStatusTag() + "] " + this.getDisplayName() + "  Combat Lv" + this.combatLevel + " | " + roleName + "+" + stanceName + inv;
        if (this.fallen && this.deathCause != null) {
            base = base + " -- Died: " + this.deathCause;
        }
        return base;
    }

    public void normalizeFarmAreaPersistence() {
        if (this.farmAreaTopLeft == null || this.farmAreaBottomRight == null) {
            return;
        }
        String worldId = this.farmAreaTopLeft.worldId != null && !this.farmAreaTopLeft.worldId.isBlank() ? this.farmAreaTopLeft.worldId : this.farmAreaBottomRight.worldId;
        int minX = Math.min(this.farmAreaTopLeft.x, this.farmAreaBottomRight.x);
        int minY = Math.min(this.farmAreaTopLeft.y, this.farmAreaBottomRight.y);
        int minZ = Math.min(this.farmAreaTopLeft.z, this.farmAreaBottomRight.z);
        int maxX = Math.max(this.farmAreaTopLeft.x, this.farmAreaBottomRight.x);
        int maxY = Math.max(this.farmAreaTopLeft.y, this.farmAreaBottomRight.y);
        int maxZ = Math.max(this.farmAreaTopLeft.z, this.farmAreaBottomRight.z);
        this.farmAreaTopLeft = new BlockPos(worldId, minX, minY, minZ);
        this.farmAreaBottomRight = new BlockPos(worldId, maxX, maxY, maxZ);
    }

    public String getEquipped(EquipmentSlot slot) {
        return switch (slot) {
            default -> throw new MatchException(null, null);
            case EquipmentSlot.WEAPON -> this.equippedWeapon;
            case EquipmentSlot.OFFHAND -> this.equippedOffhand;
            case EquipmentSlot.HELMET -> this.equippedHelmet;
            case EquipmentSlot.CHESTPLATE -> this.equippedChest;
            case EquipmentSlot.LEGGINGS -> this.equippedLegs;
            case EquipmentSlot.BOOTS -> this.equippedBoots;
        };
    }

    public void setEquipped(EquipmentSlot slot, String assetId) {
        switch (slot) {
            case WEAPON: {
                this.equippedWeapon = assetId;
                break;
            }
            case OFFHAND: {
                this.equippedOffhand = assetId;
                break;
            }
            case HELMET: {
                this.equippedHelmet = assetId;
                break;
            }
            case CHESTPLATE: {
                this.equippedChest = assetId;
                break;
            }
            case LEGGINGS: {
                this.equippedLegs = assetId;
                break;
            }
            case BOOTS: {
                this.equippedBoots = assetId;
            }
        }
    }

    public static CompanionRecord fromFallen(FallenCompanion fc) {
        CompanionRecord r = new CompanionRecord(fc.companionUniqueId != null ? fc.companionUniqueId : UUID.randomUUID().toString());
        r.name = fc.name;
        r.appearanceModelId = fc.appearanceModelId;
        r.normalizeAppearancePersistence();
        r.skinSeed = 0L;
        r.combatLevel = fc.combatLevel;
        r.combatKills = fc.combatKills;
        r.farmLevel = fc.farmLevel;
        r.farmHarvests = fc.farmHarvests;
        r.mode = fc.mode;
        r.followMode = fc.followMode;
        r.farmAutoResume = fc.farmAutoResume;
        r.farmAreaTopLeft = fc.farmAreaTopLeft;
        r.farmAreaBottomRight = fc.farmAreaBottomRight;
        r.linkedChests.clear();
        if (fc.linkedChests != null) {
            r.linkedChests.addAll(fc.linkedChests);
        }
        r.normalizeFarmAreaPersistence();
        r.deathCount = 1;
        r.deathCause = fc.deathCause;
        r.deathTime = fc.deathTime;
        r.fallen = true;
        r.active = false;
        if (fc.savedInventory != null) {
            r.savedInventory.putAll(fc.savedInventory);
        }
        r.savedInventoryStacks.clear();
        return r;
    }

    public void normalizeAppearancePersistence() {
        if (this.savedPlayerSkinToken != null || this.savedModelAssetId != null) {
            return;
        }
        if (this.appearanceModelId == null || this.appearanceModelId.isBlank()) {
            return;
        }
        String token = this.appearanceModelId.trim();
        String lower = token.toLowerCase();
        if (lower.startsWith("psk|")) {
            this.savedPlayerSkinToken = token;
            this.appearanceSource = "PLAYER_SKIN";
            this.appearanceLastUpdatedUtc = System.currentTimeMillis();
            return;
        }
        if (lower.startsWith("pmod|")) {
            String modelId = token.substring(5);
            if (!this.isFallbackModel(modelId)) {
                this.savedModelAssetId = modelId;
                this.appearanceSource = "MODEL_ASSET";
                this.appearanceLastUpdatedUtc = System.currentTimeMillis();
            }
            return;
        }
        if (!this.isFallbackModel(token)) {
            this.savedModelAssetId = token;
            this.appearanceSource = "MODEL_ASSET";
            this.appearanceLastUpdatedUtc = System.currentTimeMillis();
        }
    }

    private boolean isFallbackModel(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase().trim();
        return lower.equals("playertestmodel_v") || lower.equals("playertestmodel_g");
    }
}

