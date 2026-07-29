/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.codec.Codec
 *  com.hypixel.hytale.codec.KeyedCodec
 *  com.hypixel.hytale.codec.builder.BuilderCodec
 *  com.hypixel.hytale.codec.builder.BuilderCodec$Builder
 */
package com.yourname.companion.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class CompanionMenuEventData {
    public static final BuilderCodec<CompanionMenuEventData> CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(CompanionMenuEventData.class, CompanionMenuEventData::new).append(new KeyedCodec("Action", (Codec)Codec.STRING), (data, value) -> {
        data.action = value == null ? "" : value;
    }, data -> data.action == null ? "" : data.action).add()).append(new KeyedCodec("Tab", (Codec)Codec.STRING), (data, value) -> {
        data.tab = value == null ? "" : value;
    }, data -> data.tab == null ? "" : data.tab).add()).append(new KeyedCodec("Value", (Codec)Codec.STRING), (data, value) -> {
        data.value = value == null ? "" : value;
    }, data -> data.value == null ? "" : data.value).add()).build();
    public String action = "";
    public String tab = "";
    public String value = "";
}

