/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.webeditor;

import com.google.gson.JsonElement;
import java.util.Map;

public interface ModConfigProvider {
    public String getModId();

    public Map<String, JsonElement> exportConfigs();

    public Map<String, Map<String, String>> exportLocalization();

    public void importConfig(String var1, JsonElement var2);

    default public void importLocalization(String language, Map<String, String> translations) {
    }

    public String reload();
}

