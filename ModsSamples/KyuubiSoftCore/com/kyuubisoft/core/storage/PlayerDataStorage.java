/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.storage;

import java.util.UUID;

public interface PlayerDataStorage {
    public String loadJson(String var1, UUID var2);

    public void saveJson(String var1, UUID var2, String var3, String var4);

    public void delete(String var1, UUID var2);

    public boolean exists(String var1, UUID var2);

    public void ensureTableExists(String var1);

    public void shutdown();

    public String getTypeName();
}

