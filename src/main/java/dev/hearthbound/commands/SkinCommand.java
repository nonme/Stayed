package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.npc.ElfSage;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * /hb skin [slot] [value] — Apply or test PlayerSkin on the elf NPC.
 * /hb skin — applies default elf sage look
 * /hb skin ears Ears2 — changes ears only (for testing)
 * /hb skin haircut ElfBackBun — changes haircut only
 */
public class SkinCommand extends AbstractPlayerCommand {

    private static final Logger LOGGER = Logger.getLogger(SkinCommand.class.getName());

    private final DefaultArg<String> slotArg;
    private final DefaultArg<String> valueArg;

    // Current elf skin state (persists during session for incremental changes)
    private static PlayerSkin currentSkin = null;

    public SkinCommand() {
        super("skin", "Apply PlayerSkin to elf NPC");
        slotArg = withDefaultArg("slot", "Skin slot (apply/ears/haircut/eyes/face/mouth/eyebrows/body/shoes/overtop/undertop/pants/headacc/faceacc/earacc/cape/gloves/skinfeature/facialhair)", ArgTypes.STRING, "apply", "apply");
        valueArg = withDefaultArg("value", "Value for the slot", ArgTypes.STRING, "", "");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        String slot = ctx.get(slotArg).toLowerCase();
        String value = ctx.get(valueArg);

        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null || village.getElfId() == null) {
            ctx.sendMessage(Message.raw("No elf NPC found. Spawn one first."));
            return;
        }

        UUID elfUuid = village.getElfId();
        Ref<EntityStore> elfRef = world.getEntityRef(elfUuid);
        if (elfRef == null || !elfRef.isValid()) {
            ctx.sendMessage(Message.raw("Elf entity not found in world."));
            return;
        }

        try {
            CosmeticsModule cosmetics = CosmeticsModule.get();
            if (cosmetics == null) {
                ctx.sendMessage(Message.raw("CosmeticsModule not available"));
                return;
            }

            // Initialize skin if needed
            if (currentSkin == null) {
                currentSkin = createDefaultElfSkin();
            }

            if (slot.equals("apply")) {
                // Apply default or current skin
                currentSkin = createDefaultElfSkin();
            } else if (slot.equals("reset")) {
                currentSkin = createDefaultElfSkin();
                ctx.sendMessage(Message.raw("Skin reset to defaults."));
            } else if (value.isEmpty()) {
                ctx.sendMessage(Message.raw("Usage: /hb skin " + slot + " <value>"));
                return;
            } else {
                // Modify specific slot
                modifySlot(currentSkin, slot, value);
                ctx.sendMessage(Message.raw("Set " + slot + " = " + value));
            }

            // Apply skin to elf
            applySkin(elfRef, currentSkin, cosmetics);
            ctx.sendMessage(Message.raw("Skin applied to elf sage."));

        } catch (Exception e) {
            ctx.sendMessage(Message.raw("Error: " + e.getMessage()));
            LOGGER.warning("Skin command error: " + e.getMessage());
        }
    }

    private static PlayerSkin createDefaultElfSkin() {
        return ElfSage.createSageSkin();
    }

    private static void modifySlot(PlayerSkin skin, String slot, String value) {
        switch (slot) {
            case "body" -> skin.bodyCharacteristic = value;
            case "underwear" -> skin.underwear = value;
            case "face" -> skin.face = value;
            case "eyes" -> skin.eyes = value;
            case "ears" -> skin.ears = value;
            case "mouth" -> skin.mouth = value;
            case "facialhair" -> skin.facialHair = value;
            case "haircut" -> skin.haircut = value;
            case "eyebrows" -> skin.eyebrows = value;
            case "pants" -> skin.pants = value;
            case "overpants" -> skin.overpants = value;
            case "undertop" -> skin.undertop = value;
            case "overtop" -> skin.overtop = value;
            case "shoes" -> skin.shoes = value;
            case "headacc" -> skin.headAccessory = value;
            case "faceacc" -> skin.faceAccessory = value;
            case "earacc" -> skin.earAccessory = value;
            case "skinfeature" -> skin.skinFeature = value;
            case "gloves" -> skin.gloves = value;
            case "cape" -> skin.cape = value;
            default -> throw new IllegalArgumentException("Unknown slot: " + slot);
        }
    }

    private static void applySkin(Ref<EntityStore> elfRef, PlayerSkin skin, CosmeticsModule cosmetics) {
        // validateSkin() throws on unknown IDs — build a safe fallback if it fails
        PlayerSkin safeSkin;
        try {
            cosmetics.validateSkin(skin);
            safeSkin = skin;
        } catch (Exception e) {
            // If validation fails, generate a random base and overlay our values
            safeSkin = cosmetics.generateRandomSkin(new java.util.Random());
            if (skin.ears != null) safeSkin.ears = skin.ears;
            if (skin.haircut != null) safeSkin.haircut = skin.haircut;
            if (skin.overtop != null) safeSkin.overtop = skin.overtop;
            if (skin.undertop != null) safeSkin.undertop = skin.undertop;
            if (skin.shoes != null) safeSkin.shoes = skin.shoes;
            if (skin.pants != null) safeSkin.pants = skin.pants;
            if (skin.eyes != null) safeSkin.eyes = skin.eyes;
            if (skin.face != null) safeSkin.face = skin.face;
            if (skin.mouth != null) safeSkin.mouth = skin.mouth;
            if (skin.eyebrows != null) safeSkin.eyebrows = skin.eyebrows;
            if (skin.bodyCharacteristic != null) safeSkin.bodyCharacteristic = skin.bodyCharacteristic;
            if (skin.facialHair != null) safeSkin.facialHair = skin.facialHair;
            if (skin.headAccessory != null) safeSkin.headAccessory = skin.headAccessory;
            if (skin.faceAccessory != null) safeSkin.faceAccessory = skin.faceAccessory;
            if (skin.earAccessory != null) safeSkin.earAccessory = skin.earAccessory;
            if (skin.skinFeature != null) safeSkin.skinFeature = skin.skinFeature;
            if (skin.gloves != null) safeSkin.gloves = skin.gloves;
            if (skin.cape != null) safeSkin.cape = skin.cape;
            LOGGER.info("Used random base skin with overlayed elf values");
        }

        Model model = cosmetics.createModel(safeSkin, 1.0f);
        if (model == null) {
            throw new RuntimeException("createModel returned null");
        }

        Store<EntityStore> elfStore = elfRef.getStore();
        elfStore.putComponent(elfRef, PlayerSkinComponent.getComponentType(), new PlayerSkinComponent(safeSkin));
        elfStore.putComponent(elfRef, ModelComponent.getComponentType(), new ModelComponent(model));

        LOGGER.info("Applied PlayerSkin to elf NPC");
    }

    public static PlayerSkin getCurrentSkin() {
        return currentSkin;
    }
}
