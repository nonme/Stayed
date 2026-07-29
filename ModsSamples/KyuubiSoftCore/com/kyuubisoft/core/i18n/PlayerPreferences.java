/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.i18n;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PlayerPreferences {
    private String languageOverride;
    private Map<String, String> modLanguageOverrides;

    public String getLanguageOverride() {
        return this.languageOverride;
    }

    public void setLanguageOverride(String languageOverride) {
        this.languageOverride = languageOverride;
    }

    public boolean hasLanguageOverride() {
        return this.languageOverride != null && !this.languageOverride.isEmpty();
    }

    public String getModLanguageOverride(String modId) {
        if (this.modLanguageOverrides == null) {
            return null;
        }
        return this.modLanguageOverrides.get(modId);
    }

    public void setModLanguageOverride(String modId, String langCode) {
        if (this.modLanguageOverrides == null) {
            this.modLanguageOverrides = new HashMap<String, String>();
        }
        this.modLanguageOverrides.put(modId, langCode);
    }

    public void clearModLanguageOverride(String modId) {
        if (this.modLanguageOverrides != null) {
            this.modLanguageOverrides.remove(modId);
            if (this.modLanguageOverrides.isEmpty()) {
                this.modLanguageOverrides = null;
            }
        }
    }

    public Map<String, String> getModLanguageOverrides() {
        if (this.modLanguageOverrides == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(this.modLanguageOverrides);
    }
}

