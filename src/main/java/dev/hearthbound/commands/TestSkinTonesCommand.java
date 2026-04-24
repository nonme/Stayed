package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.npc.NpcManager;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Spawns one neutral Villager_Human per skin tone id (01..53) arranged in a grid
 * so a human can visually identify which tone numbers look "human" and which
 * belong in Exotic (green / grey / unnatural). Each NPC's chat line reports its
 * grid position and the tone id — read them off the map to build a whitelist.
 *
 * <p>We iterate the full 01..53 range even though some indices are missing from
 * the registry; missing ones are simply skipped and reported in the summary.
 */
public class TestSkinTonesCommand extends AbstractPlayerCommand {

    private static final String VILLAGER_ROLE = "Villager_Human";
    private static final double SPACING = 2.5;
    private static final int PER_ROW = 8;
    // Long-lived seed so every NPC has the same neutral body/hair/outfit and
    // only the skin tone varies. Must be one that yields a plain-looking base.
    private static final long NEUTRAL_SEED = 12345L;

    public TestSkinTonesCommand() {
        super("testskin", "Spawn one villager per skin tone (01..53) to preview the palette");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                           PlayerRef player, World world) {
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            ctx.sendMessage(Message.raw("Could not get player position."));
            return;
        }
        Vector3d base = transform.getPosition();

        CosmeticsModule cosmetics = CosmeticsModule.get();
        if (cosmetics == null) {
            ctx.sendMessage(Message.raw("CosmeticsModule not available"));
            return;
        }

        List<List<String>> rows = new ArrayList<>();
        int placed = 0, skipped = 0;
        for (int tone = 1; tone <= 53; tone++) {
            String toneKey = String.format("%02d", tone);

            // Build a minimal valid skin with only the body characteristic varying.
            PlayerSkin skin = cosmetics.generateRandomSkin(new Random(NEUTRAL_SEED));
            skin.bodyCharacteristic = "Default." + toneKey;
            skin.cape = null;

            Model model;
            try {
                model = cosmetics.createModel(skin, 1.0f);
            } catch (Exception e) {
                // The Skin gradient set has 47 entries, not 53 — numbers outside the
                // set throw InvalidSkinException. Count them but don't spam chat.
                skipped++;
                continue;
            }
            if (model == null) {
                skipped++;
                continue;
            }

            int col = placed % PER_ROW;
            int row = placed / PER_ROW;
            Vector3d pos = new Vector3d(
                    base.getX() + 3.0 + col * SPACING,
                    base.getY(),
                    base.getZ() + 3.0 + row * SPACING);

            Pair<Ref<EntityStore>, INonPlayerCharacter> result =
                    NpcManager.spawnNpc(store, pos, new Vector3f(0, 0, 0), VILLAGER_ROLE);
            if (result == null) {
                skipped++;
                continue;
            }
            Ref<EntityStore> npcRef = result.first();
            store.putComponent(npcRef, PlayerSkinComponent.getComponentType(), new PlayerSkinComponent(skin));
            store.putComponent(npcRef, ModelComponent.getComponentType(), new ModelComponent(model));

            // Match the PersistentModel fix used elsewhere so these NPCs survive a reload.
            PersistentModel pm = store.getComponent(npcRef, PersistentModel.getComponentType());
            if (pm != null) {
                pm.setModelReference(new Model.ModelReference(
                        model.getModelAssetId(), 1.0f,
                        model.getRandomAttachmentIds(),
                        model.getAnimationSetMap() == null));
            }

            // Freeze so they don't wander out of their grid cell. Frozen alone keeps
            // the behaviour tree still but the walk animation can linger, so also
            // stop the Movement animation slot (pattern from BuilderBehavior).
            store.addComponent(npcRef, Frozen.getComponentType(), Frozen.get());
            AnimationUtils.stopAnimation(npcRef, AnimationSlot.Movement, store);

            // Record this tone in its row for the final report.
            while (rows.size() <= row) rows.add(new ArrayList<>());
            rows.get(row).add(toneKey);

            placed++;
        }

        ctx.sendMessage(Message.raw("Placed " + placed + " skin tones, skipped " + skipped + "."));
        for (int r = 0; r < rows.size(); r++) {
            ctx.sendMessage(Message.raw("Row " + r + ": " + String.join(" ", rows.get(r))));
        }
    }
}
