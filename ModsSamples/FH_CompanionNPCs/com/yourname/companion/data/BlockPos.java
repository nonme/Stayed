/*
 * Decompiled with CFR 0.152.
 */
package com.yourname.companion.data;

import java.util.Objects;

public final class BlockPos {
    public String worldId;
    public int x;
    public int y;
    public int z;

    public BlockPos() {
    }

    public BlockPos(String worldId, int x, int y, int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BlockPos)) {
            return false;
        }
        BlockPos other = (BlockPos)obj;
        return this.x == other.x && this.y == other.y && this.z == other.z && Objects.equals(this.worldId, other.worldId);
    }

    public int hashCode() {
        return Objects.hash(this.worldId, this.x, this.y, this.z);
    }
}

