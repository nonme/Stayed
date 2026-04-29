package dev.hearthbound.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class DialogEventData {

    public static final String ACTION_KEY = "Action";
    public static final String VALUE_KEY = "Value";
    public static final String VILLAGE_NAME_KEY = "@VillageName";

    public static final BuilderCodec<DialogEventData> CODEC = BuilderCodec.builder(DialogEventData.class, DialogEventData::new)
            .append(new KeyedCodec<>(ACTION_KEY, Codec.STRING), DialogEventData::setAction, DialogEventData::getAction).add()
            .append(new KeyedCodec<>(VALUE_KEY, Codec.STRING), DialogEventData::setValue, DialogEventData::getValue).add()
            .append(new KeyedCodec<>(VILLAGE_NAME_KEY, Codec.STRING), DialogEventData::setVillageName, DialogEventData::getVillageName).add()
            .build();

    private String action = "";
    private String value = "";
    private String villageName = "";

    public DialogEventData() {}

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getVillageName() { return villageName; }
    public void setVillageName(String villageName) { this.villageName = villageName; }
}
