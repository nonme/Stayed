/*
 * Decompiled with CFR 0.152.
 */
package com.yourname.companion.api;

import com.yourname.companion.api.CommandRegistry;
import com.yourname.companion.api.ConfigProvider;
import com.yourname.companion.api.Logger;

public interface PluginContext {
    public Logger logger();

    public ConfigProvider config();

    public CommandRegistry commands();
}

