package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinGradientSet;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class CosmeticsCommand extends AbstractPlayerCommand {

    private static final Logger LOGGER = Logger.getLogger(CosmeticsCommand.class.getName());

    public CosmeticsCommand() {
        super("cosmetics", "Dump all cosmetic IDs to file cosmetics_dump.txt");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        try {
            CosmeticsModule cosmetics = CosmeticsModule.get();
            if (cosmetics == null) {
                ctx.sendMessage(Message.raw("CosmeticsModule not available"));
                return;
            }
            CosmeticRegistry reg = cosmetics.getRegistry();
            if (reg == null) {
                ctx.sendMessage(Message.raw("CosmeticRegistry not available"));
                return;
            }

            Map<String, PlayerSkinGradientSet> gradientSets = reg.getGradientSets();

            Path outPath = Path.of("cosmetics_dump.txt");
            try (PrintWriter out = new PrintWriter(new FileWriter(outPath.toFile()))) {
                out.println("=== Hearthbound Cosmetics Dump ===");
                out.println("Compound format: PartId.TextureKey or PartId.TextureKey.VariantKey");
                out.println("Bare ID categories (no dot needed): Ears, Faces, Mouths");
                out.println();

                // Gradient sets overview
                out.println("=== Gradient Sets ===");
                if (gradientSets != null) {
                    for (var gsEntry : gradientSets.entrySet()) {
                        PlayerSkinGradientSet gs = gsEntry.getValue();
                        if (gs.getGradients() != null) {
                            out.println("  " + gsEntry.getKey() + ": " +
                                    String.join(", ", gs.getGradients().keySet()));
                        }
                    }
                }
                out.println();

                // Bare ID categories
                dumpCategory(out, "Ears (bare ID)", reg.getEars(), gradientSets, true);
                dumpCategory(out, "Faces (bare ID)", reg.getFaces(), gradientSets, true);
                dumpCategory(out, "Mouths (bare ID)", reg.getMouths(), gradientSets, true);

                // Compound ID categories
                dumpCategory(out, "Haircuts", reg.getHaircuts(), gradientSets, false);
                dumpCategory(out, "Eyes", reg.getEyes(), gradientSets, false);
                dumpCategory(out, "Eyebrows", reg.getEyebrows(), gradientSets, false);
                dumpCategory(out, "BodyCharacteristics", reg.getBodyCharacteristics(), gradientSets, false);
                dumpCategory(out, "FacialHair", reg.getFacialHairs(), gradientSets, false);
                dumpCategory(out, "Shoes", reg.getShoes(), gradientSets, false);
                dumpCategory(out, "Overtops", reg.getOvertops(), gradientSets, false);
                dumpCategory(out, "Undertops", reg.getUndertops(), gradientSets, false);
                dumpCategory(out, "Pants", reg.getPants(), gradientSets, false);
                dumpCategory(out, "Overpants", reg.getOverpants(), gradientSets, false);
                dumpCategory(out, "Underwear", reg.getUnderwear(), gradientSets, false);
                dumpCategory(out, "HeadAccessories", reg.getHeadAccessories(), gradientSets, false);
                dumpCategory(out, "FaceAccessories", reg.getFaceAccessories(), gradientSets, false);
                dumpCategory(out, "EarAccessories", reg.getEarAccessories(), gradientSets, false);
                dumpCategory(out, "SkinFeatures", reg.getSkinFeatures(), gradientSets, false);
                dumpCategory(out, "Gloves", reg.getGloves(), gradientSets, false);
                dumpCategory(out, "Capes", reg.getCapes(), gradientSets, false);
            }

            ctx.sendMessage(Message.raw("Dumped all cosmetics to: " + outPath.toAbsolutePath()));
            LOGGER.info("Cosmetics dump saved to: " + outPath.toAbsolutePath());

        } catch (Exception e) {
            ctx.sendMessage(Message.raw("Error: " + e.getMessage()));
            LOGGER.warning("Cosmetics command error: " + e.getMessage());
        }
    }

    private void dumpCategory(PrintWriter out, String name, Map<String, ?> parts,
                               Map<String, PlayerSkinGradientSet> gradientSets, boolean bareId) {
        out.println("--- " + name + " (" + (parts == null ? 0 : parts.size()) + " entries) ---");
        if (parts == null || parts.isEmpty()) {
            out.println("  (empty)");
            out.println();
            return;
        }

        for (var entry : parts.entrySet()) {
            String id = entry.getKey();
            Object value = entry.getValue();

            if (!(value instanceof PlayerSkinPart part)) {
                out.println("  " + id + " (type: " + value.getClass().getSimpleName() + ")");
                continue;
            }

            try {
                // Collect texture keys from gradient set and/or part textures
                List<String> textureKeys = new ArrayList<>();
                String gradientSetName = part.getGradientSet();

                if (gradientSetName != null && gradientSets != null) {
                    PlayerSkinGradientSet gs = gradientSets.get(gradientSetName);
                    if (gs != null && gs.getGradients() != null) {
                        textureKeys.addAll(gs.getGradients().keySet());
                    }
                }
                if (part.getTextures() != null) {
                    for (String key : part.getTextures().keySet()) {
                        if (!textureKeys.contains(key)) textureKeys.add(key);
                    }
                }

                var variants = part.getVariants();
                boolean hasVariants = variants != null && !variants.isEmpty();

                if (bareId) {
                    // Face, Ears, Mouth — just the ID
                    out.println("  " + id);
                } else if (textureKeys.isEmpty() && !hasVariants) {
                    out.println("  " + id + " (NO texture keys — may not work!)");
                } else if (!textureKeys.isEmpty() && !hasVariants) {
                    // Compact: show first 5 + count
                    String preview = String.join(", ", textureKeys.subList(0, Math.min(5, textureKeys.size())));
                    if (textureKeys.size() > 5) preview += ", ... (" + textureKeys.size() + " total)";
                    out.println("  " + id + " [" + preview + "]");
                    if (gradientSetName != null) {
                        out.println("    gradientSet: " + gradientSetName);
                    }
                } else if (textureKeys.isEmpty() && hasVariants) {
                    for (var vKey : variants.keySet()) {
                        out.println("  " + id + "." + vKey);
                    }
                } else {
                    // Both texture keys and variants
                    String preview = String.join(", ", textureKeys.subList(0, Math.min(5, textureKeys.size())));
                    if (textureKeys.size() > 5) preview += ", ... (" + textureKeys.size() + " total)";
                    out.println("  " + id + " [" + preview + "] x variants: " +
                            String.join(", ", variants.keySet()));
                }
            } catch (Exception ex) {
                out.println("  " + id + " (error: " + ex.getMessage() + ")");
            }
        }
        out.println();
    }
}
