package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinGradientSet;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import dev.hearthbound.npc.appearance.BodyArchetype;
import dev.hearthbound.npc.appearance.CosmeticPools;
import dev.hearthbound.npc.appearance.StyleArchetype;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
/**
 * Generates and applies a deterministic PlayerSkin for a human villager.
 *
 * <p>The skin is built from whitelists in {@link CosmeticPools} rather than the
 * engine's {@code generateRandomSkin}, which mixes every asset uniformly and
 * produces visual noise (green hair + red beard + yellow pants). Pipeline:
 * <ol>
 *   <li>Derive {@link BodyArchetype} and {@link StyleArchetype} from {@code (seed, villagerIndex)}.
 *       The archetype gate normalises the first few settlers to plain Peasants and only
 *       opens up Citizen / Exotic rolls as the village grows.</li>
 *   <li>Pick one hair color and reuse it for haircut + eyebrows + facial hair, so a
 *       villager never has mismatched colors across those three.</li>
 *   <li>For every outfit slot, ask the cosmetic registry which texture keys a given
 *       part supports and steer toward a natural fabric tone via {@link #buildCompound}.</li>
 *   <li>Call {@code createModel} and put PlayerSkin + Model components. Fix
 *       PersistentModel scale=0 so the NPC survives server restarts.</li>
 * </ol>
 *
 * <p>Each villager persists only its {@code skinSeed}; feeding the same seed and
 * index back produces the same skin. No need to store the rolled IDs themselves.
 */
public final class VillagerAppearance {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("npc.skin");
    /** Below this villager index the archetype is forced to Peasant. */
    private static final int NORMALIZATION_THRESHOLD = 5;

    private VillagerAppearance() {}

    // ---------------------------------------------------------------
    // Public entry points
    // ---------------------------------------------------------------

    public static void apply(Ref<EntityStore> npcRef, Store<EntityStore> store, long seed, int villagerIndex) {
        try {
            CosmeticsModule cosmetics = CosmeticsModule.get();
            if (cosmetics == null) {
                LOG.warn("CosmeticsModule not available, skipping villager skin");
                return;
            }

            UUIDComponent uuidComp = store.getComponent(npcRef, UUIDComponent.getComponentType());
            String entityId = (uuidComp != null) ? uuidComp.getUuid().toString().substring(0, 8) : "no-uuid";

            PlayerSkin skin = createHumanSkin(seed, villagerIndex);
            if (skin == null) {
                LOG.warn("[skin] createHumanSkin returned null — entity=" + entityId);
                return;
            }

            Model model = safeCreateModel(cosmetics, skin);
            if (model == null) {
                LOG.warn("[skin] createModel failed, attempting repair — entity=" + entityId);
                model = tryRepairSkin(cosmetics, skin, seed, villagerIndex);
            }
            if (model == null) {
                LOG.warn("[skin] fallback also failed — entity=" + entityId + " leaving default appearance");
                return;
            }

            store.putComponent(npcRef, PlayerSkinComponent.getComponentType(), new PlayerSkinComponent(skin));

            PlayerSkinComponent check = store.getComponent(npcRef, PlayerSkinComponent.getComponentType());
            if (check == null) {
                LOG.warn("[skin] PlayerSkinComponent missing after putComponent — entity=" + entityId);
            }

            store.putComponent(npcRef, ModelComponent.getComponentType(), new ModelComponent(model));
            fixPersistentModelScale(npcRef, store, model);
        } catch (Exception e) {
            LOG.warn("Failed to apply villager appearance", e);
        }
    }

    public static PlayerSkin createHumanSkin(long seed, int villagerIndex) {
        CosmeticsModule cosmetics = CosmeticsModule.get();
        if (cosmetics == null) {
            LOG.warn("CosmeticsModule not available — returning null skin");
            return null;
        }
        CosmeticRegistry reg = cosmetics.getRegistry();
        Random rng = new Random(seed);

        BodyArchetype body = rng.nextBoolean() ? BodyArchetype.Masc : BodyArchetype.Fem;
        StyleArchetype style = rollStyle(rng, villagerIndex);
        String hairColor = pickHairColor(rng, style);
        String fabricTone = CosmeticPools.pick(rng, CosmeticPools.FABRIC_TONES_NATURAL);

        // generateRandomSkin fills every slot with a valid ID so createModel doesn't
        // choke on an unset field. We then overwrite the slots we actually care about.
        PlayerSkin skin = cosmetics.generateRandomSkin(new Random(seed));

        applyHead(skin, rng, reg, body, style, hairColor);
        applyOutfit(skin, rng, reg, body, style, fabricTone);
        applyAccessories(skin, rng, reg, body, style, fabricTone);

        skin.cape = null; // capes are a premium cosmetic; never used for NPCs.

        return skin;
    }

    // ---------------------------------------------------------------
    // Archetype gates
    // ---------------------------------------------------------------

    /** Debug helper: same body roll as {@link #createHumanSkin} for a given seed. */
    public static BodyArchetype predictBody(long seed) {
        return new Random(seed).nextBoolean() ? BodyArchetype.Masc : BodyArchetype.Fem;
    }

    /** Debug helper: same style roll as {@link #createHumanSkin} for a given (seed, index). */
    public static StyleArchetype predictStyle(long seed, int villagerIndex) {
        Random rng = new Random(seed);
        rng.nextBoolean(); // consume body pick so the state matches createHumanSkin
        return rollStyle(rng, villagerIndex);
    }

    private static StyleArchetype rollStyle(Random rng, int villagerIndex) {
        if (villagerIndex < NORMALIZATION_THRESHOLD) return StyleArchetype.Peasant;

        // Ramp Citizen / Exotic chances as the village grows so late-game villages
        // have a bit of variety without flooding the first settlers with clowns.
        int steps = villagerIndex - NORMALIZATION_THRESHOLD;
        double citizenChance = Math.min(0.15, 0.01 * (steps + 1));
        double exoticChance = Math.min(0.10, 0.005 * (steps + 1));

        double roll = rng.nextDouble();
        if (roll < citizenChance) return StyleArchetype.Citizen;
        if (roll < citizenChance + exoticChance) return StyleArchetype.Exotic;
        return StyleArchetype.Peasant;
    }

    private static String pickHairColor(Random rng, StyleArchetype style) {
        if (style == StyleArchetype.Exotic && rng.nextInt(100) < 60) {
            return CosmeticPools.pick(rng, CosmeticPools.HAIR_COLORS_EXOTIC);
        }
        return CosmeticPools.pick(rng, CosmeticPools.HAIR_COLORS_NATURAL);
    }

    // ---------------------------------------------------------------
    // Slot application
    // ---------------------------------------------------------------

    private static void applyHead(PlayerSkin skin, Random rng, CosmeticRegistry reg,
                                   BodyArchetype body, StyleArchetype style, String hairColor) {
        // Skin tone — weighted 70/30 between light and dark human tones.
        // Exotic / non-human tones (green, pink, purple, saturated red/blue) are
        // not exposed to villagers; they are reserved for future custom races.
        String skinTone = rng.nextInt(100) < 70
                ? CosmeticPools.pick(rng, CosmeticPools.SKIN_TONES_LIGHT)
                : CosmeticPools.pick(rng, CosmeticPools.SKIN_TONES_DARK);

        // Muscular body ~20% for Masc only.
        String bodyPart = (body == BodyArchetype.Masc && rng.nextInt(100) < 20
                && reg.getBodyCharacteristics().containsKey("Muscular"))
                ? "Muscular"
                : "Default";
        skin.bodyCharacteristic = buildCompound(reg.getBodyCharacteristics(), reg, bodyPart, skinTone);

        // Ears — humans by default, small elf ears allowed only in Exotic.
        if (style == StyleArchetype.Exotic && rng.nextInt(100) < 30) {
            skin.ears = CosmeticPools.pick(rng, CosmeticPools.EARS_EXOTIC);
        } else {
            skin.ears = CosmeticPools.EARS_HUMAN;
        }

        // Eyes.
        List<String> eyePool = (style == StyleArchetype.Exotic && rng.nextInt(100) < 50)
                ? CosmeticPools.EYES_EXOTIC
                : CosmeticPools.EYES_NORMAL;
        skin.eyes = buildCompound(reg.getEyes(), reg, CosmeticPools.pick(rng, eyePool), "Brown", "Green", "Blue");

        // Haircut — pool depends on body + style.
        List<String> haircutPool = haircutPoolFor(body, style);
        skin.haircut = buildCompound(reg.getHaircuts(), reg, CosmeticPools.pick(rng, haircutPool), hairColor);

        // Eyebrows — Masc allows thick brows, Fem only thin/medium. Exotic inherits from body.
        List<String> browPool = (body == BodyArchetype.Masc)
                ? CosmeticPools.join(CosmeticPools.EYEBROWS_ANY, CosmeticPools.EYEBROWS_MASC_ONLY)
                : CosmeticPools.EYEBROWS_ANY;
        skin.eyebrows = buildCompound(reg.getEyebrows(), reg, CosmeticPools.pick(rng, browPool), hairColor);

        // Mouth.
        skin.mouth = pickMouth(rng, body, style);

        // Face.
        skin.face = pickFace(rng, body, style);

        // Facial hair — Masc only, 70% chance, and share hairColor so the beard matches.
        if (body == BodyArchetype.Masc && rng.nextInt(100) < 70) {
            List<String> beardPool = rng.nextInt(100) < 30
                    ? CosmeticPools.FACIAL_HAIR_LIGHT
                    : CosmeticPools.FACIAL_HAIR_FULL;
            skin.facialHair = buildCompound(reg.getFacialHairs(), reg,
                    CosmeticPools.pick(rng, beardPool), hairColor);
        } else {
            skin.facialHair = null;
        }
    }

    private static List<String> haircutPoolFor(BodyArchetype body, StyleArchetype style) {
        if (style == StyleArchetype.Exotic) {
            // Exotic pulls from its own pool plus unisex — still avoids body-specific cuts.
            return CosmeticPools.join(CosmeticPools.HAIRCUTS_EXOTIC, CosmeticPools.HAIRCUTS_UNISEX);
        }
        List<String> bodySpecific = (body == BodyArchetype.Masc)
                ? CosmeticPools.HAIRCUTS_MASC
                : CosmeticPools.HAIRCUTS_FEM;
        return CosmeticPools.join(bodySpecific, CosmeticPools.HAIRCUTS_UNISEX);
    }

    private static String pickMouth(Random rng, BodyArchetype body, StyleArchetype style) {
        if (style == StyleArchetype.Exotic && rng.nextInt(100) < 40) {
            return CosmeticPools.pick(rng, CosmeticPools.MOUTHS_EXOTIC);
        }
        if (style == StyleArchetype.Citizen && body == BodyArchetype.Fem && rng.nextInt(100) < 25) {
            return CosmeticPools.MOUTH_MAKEUP;
        }
        return CosmeticPools.pick(rng, CosmeticPools.MOUTHS_NORMAL);
    }

    private static String pickFace(Random rng, BodyArchetype body, StyleArchetype style) {
        if (style == StyleArchetype.Citizen && rng.nextInt(100) < 40) {
            return CosmeticPools.pick(rng, CosmeticPools.FACES_CITIZEN);
        }
        // Scars are rare, as a flavour hit on weathered Peasants only.
        if (style == StyleArchetype.Peasant && rng.nextInt(100) < 5) {
            return CosmeticPools.FACE_SCAR;
        }
        // Stubble only when we didn't roll a full beard — caller doesn't know yet,
        // but Face_Stubble under a beard still reads as fine, so don't bother coordinating.
        if (body == BodyArchetype.Masc && rng.nextInt(100) < 15) {
            return CosmeticPools.FACE_STUBBLE;
        }
        return CosmeticPools.pick(rng, CosmeticPools.FACES_NORMAL);
    }

    private static void applyOutfit(PlayerSkin skin, Random rng, CosmeticRegistry reg,
                                     BodyArchetype body, StyleArchetype style, String fabricTone) {
        // Underwear — hard gate on body archetype.
        List<String> underwearPool = (body == BodyArchetype.Masc)
                ? CosmeticPools.UNDERWEAR_MASC
                : CosmeticPools.UNDERWEAR_FEM;
        skin.underwear = buildCompound(reg.getUnderwear(), reg,
                CosmeticPools.pick(rng, underwearPool), fabricTone);

        // Undertop (shirt).
        List<String> undertopPool;
        if (style == StyleArchetype.Citizen) {
            undertopPool = CosmeticPools.UNDERTOPS_CITIZEN_ANY;
        } else {
            undertopPool = (body == BodyArchetype.Fem)
                    ? CosmeticPools.join(CosmeticPools.UNDERTOPS_PEASANT_ANY, CosmeticPools.UNDERTOPS_PEASANT_FEM)
                    : CosmeticPools.UNDERTOPS_PEASANT_ANY;
        }
        skin.undertop = buildCompound(reg.getUndertops(), reg,
                CosmeticPools.pick(rng, undertopPool), fabricTone);

        // Overtop (jacket / vest / dress). ~70% of peasants wear one.
        boolean wantOvertop = (style == StyleArchetype.Citizen) || rng.nextInt(100) < 70;
        if (wantOvertop) {
            List<String> overtopPool;
            if (style == StyleArchetype.Citizen) {
                overtopPool = CosmeticPools.OVERTOPS_CITIZEN_ANY;
            } else {
                overtopPool = (body == BodyArchetype.Fem)
                        ? CosmeticPools.join(CosmeticPools.OVERTOPS_PEASANT_ANY, CosmeticPools.OVERTOPS_PEASANT_FEM)
                        : CosmeticPools.OVERTOPS_PEASANT_ANY;
            }
            skin.overtop = buildCompound(reg.getOvertops(), reg,
                    CosmeticPools.pick(rng, overtopPool), fabricTone);
        } else {
            skin.overtop = null;
        }

        // Pants / skirts.
        List<String> pantsPool;
        if (style == StyleArchetype.Citizen) {
            pantsPool = CosmeticPools.join(CosmeticPools.PANTS_PEASANT_ANY, CosmeticPools.PANTS_CITIZEN_ANY);
        } else {
            pantsPool = (body == BodyArchetype.Fem)
                    ? CosmeticPools.join(CosmeticPools.PANTS_PEASANT_ANY, CosmeticPools.PANTS_PEASANT_FEM)
                    : CosmeticPools.PANTS_PEASANT_ANY;
        }
        skin.pants = buildCompound(reg.getPants(), reg,
                CosmeticPools.pick(rng, pantsPool), fabricTone);

        // Shoes.
        List<String> shoesPool = (style == StyleArchetype.Citizen)
                ? CosmeticPools.join(CosmeticPools.SHOES_PEASANT, CosmeticPools.SHOES_CITIZEN)
                : CosmeticPools.SHOES_PEASANT;
        skin.shoes = buildCompound(reg.getShoes(), reg,
                CosmeticPools.pick(rng, shoesPool), fabricTone);

        // Peasants go without gloves or overpants; citizens too, unless we add
        // profession-specific logic later.
        skin.gloves = null;
        skin.overpants = null;
    }

    private static void applyAccessories(PlayerSkin skin, Random rng, CosmeticRegistry reg,
                                          BodyArchetype body, StyleArchetype style, String fabricTone) {
        // Head accessory — currently only flowers for fem villagers. Hat / cap pools
        // are intentionally empty; they'll come back through profession outfits.
        List<String> headPool;
        if (style == StyleArchetype.Citizen) {
            headPool = (body == BodyArchetype.Fem)
                    ? CosmeticPools.join(CosmeticPools.HEAD_CITIZEN_ANY, CosmeticPools.HEAD_CITIZEN_FEM)
                    : CosmeticPools.HEAD_CITIZEN_ANY;
        } else {
            headPool = (body == BodyArchetype.Fem)
                    ? CosmeticPools.join(CosmeticPools.HEAD_PEASANT_ANY, CosmeticPools.HEAD_PEASANT_FEM)
                    : CosmeticPools.HEAD_PEASANT_ANY;
        }
        int hatChance = (style == StyleArchetype.Citizen) ? 50 : 25;
        if (!headPool.isEmpty() && rng.nextInt(100) < hatChance) {
            skin.headAccessory = buildCompound(reg.getHeadAccessories(), reg,
                    CosmeticPools.pick(rng, headPool), fabricTone);
        } else {
            skin.headAccessory = null;
        }

        // Face accessory — peasants may chew wheat; citizens may wear glasses.
        if (style == StyleArchetype.Peasant && rng.nextInt(100) < 8) {
            skin.faceAccessory = CosmeticPools.FACE_ACC_WHEAT;
        } else if (style == StyleArchetype.Citizen && rng.nextInt(100) < 30) {
            skin.faceAccessory = buildCompound(reg.getFaceAccessories(), reg,
                    CosmeticPools.pick(rng, CosmeticPools.FACE_ACC_CITIZEN), fabricTone);
        } else if (style == StyleArchetype.Exotic && rng.nextInt(100) < 10) {
            skin.faceAccessory = CosmeticPools.FACE_ACC_EYEPATCH;
        } else {
            skin.faceAccessory = null;
        }

        // Ear accessory — citizens only, ~20% chance.
        if (style == StyleArchetype.Citizen && rng.nextInt(100) < 20) {
            // Ear accessories have Left/Right/Both variants; buildCompoundWithVariant handles that.
            skin.earAccessory = buildCompoundWithVariant(reg.getEarAccessories(), reg,
                    CosmeticPools.pick(rng, CosmeticPools.EAR_ACC_CITIZEN), "Both");
        } else {
            skin.earAccessory = null;
        }
    }

    // ---------------------------------------------------------------
    // Apply-time plumbing
    // ---------------------------------------------------------------

    /**
     * After ModelComponent is put, the engine may write PersistentModel with scale=0,
     * which crashes chunk loading on restart. Rewriting it with scale=1 keeps the NPC loadable.
     */
    private static void fixPersistentModelScale(Ref<EntityStore> npcRef, Store<EntityStore> store, Model model) {
        try {
            PersistentModel pm = store.getComponent(npcRef, PersistentModel.getComponentType());
            if (pm != null) {
                pm.setModelReference(new Model.ModelReference(
                        model.getModelAssetId(), 1.0f,
                        model.getRandomAttachmentIds(),
                        model.getAnimationSetMap() == null));
            }
        } catch (Exception pmEx) {
            LOG.warn("Could not fix PersistentModel for villager: " + pmEx.getMessage());
        }
    }

    private static Model safeCreateModel(CosmeticsModule cosmetics, PlayerSkin skin) {
        try {
            return cosmetics.createModel(skin, 1.0f);
        } catch (Exception e) {
            LOG.warn("createModel threw: " + e.getMessage());
            return null;
        }
    }

    /**
     * Field-by-field repair for a rejected skin. Goes through every slot, replaces
     * its value with the donor skin's value (known valid because it came from
     * {@code generateRandomSkin}), and retries {@code createModel}. When the swap
     * fixes the model, logs which field was at fault and returns the rebuilt model
     * — only one slot has changed, the other 19 remain as the whitelist chose.
     *
     * <p>If no single-field swap repairs it (multiple bad slots, or the problem
     * is in a field we don't enumerate), falls back to the donor skin entirely.
     */
    private static Model tryRepairSkin(CosmeticsModule cosmetics, PlayerSkin skin, long seed, int villagerIndex) {
        PlayerSkin donor = cosmetics.generateRandomSkin(new Random(seed ^ 0x5a5a5a5aL));
        if (cosmetics.getRegistry().getEars().containsKey(CosmeticPools.EARS_HUMAN)) {
            donor.ears = CosmeticPools.EARS_HUMAN;
        }
        donor.cape = null;

        String snapshot = snapshotSkin(skin);

        // Each entry: (field name, swap-in-donor-value function).
        SkinField[] fields = {
                new SkinField("bodyCharacteristic", s -> s.bodyCharacteristic = donor.bodyCharacteristic),
                new SkinField("underwear",          s -> s.underwear          = donor.underwear),
                new SkinField("face",               s -> s.face               = donor.face),
                new SkinField("eyes",               s -> s.eyes               = donor.eyes),
                new SkinField("ears",               s -> s.ears               = donor.ears),
                new SkinField("mouth",              s -> s.mouth              = donor.mouth),
                new SkinField("facialHair",         s -> s.facialHair         = donor.facialHair),
                new SkinField("haircut",            s -> s.haircut            = donor.haircut),
                new SkinField("eyebrows",           s -> s.eyebrows           = donor.eyebrows),
                new SkinField("pants",              s -> s.pants              = donor.pants),
                new SkinField("overpants",          s -> s.overpants          = donor.overpants),
                new SkinField("undertop",           s -> s.undertop           = donor.undertop),
                new SkinField("overtop",            s -> s.overtop            = donor.overtop),
                new SkinField("shoes",              s -> s.shoes              = donor.shoes),
                new SkinField("headAccessory",      s -> s.headAccessory      = donor.headAccessory),
                new SkinField("faceAccessory",      s -> s.faceAccessory      = donor.faceAccessory),
                new SkinField("earAccessory",       s -> s.earAccessory       = donor.earAccessory),
                new SkinField("skinFeature",        s -> s.skinFeature        = donor.skinFeature),
                new SkinField("gloves",             s -> s.gloves             = donor.gloves),
        };

        for (SkinField field : fields) {
            PlayerSkin probe = cloneSkin(skin);
            field.apply(probe);
            Model model = safeCreateModel(cosmetics, probe);
            if (model != null) {
                LOG.warn("Villager skin repaired by replacing '" + field.name + "' (seed="
                        + seed + ", idx=" + villagerIndex + "); original=" + snapshot);
                // Mutate the caller's skin so the PlayerSkinComponent stored on the NPC
                // reflects the repair.
                field.apply(skin);
                return model;
            }
        }

        LOG.warn("No single-field repair worked (seed=" + seed + ", idx=" + villagerIndex
                + "); skin=" + snapshot + " — using donor skin");
        copySkin(donor, skin);
        return safeCreateModel(cosmetics, skin);
    }

    private static PlayerSkin cloneSkin(PlayerSkin src) {
        PlayerSkin copy = new PlayerSkin();
        copySkin(src, copy);
        return copy;
    }

    private static void copySkin(PlayerSkin src, PlayerSkin dst) {
        dst.bodyCharacteristic = src.bodyCharacteristic;
        dst.underwear          = src.underwear;
        dst.face               = src.face;
        dst.eyes               = src.eyes;
        dst.ears               = src.ears;
        dst.mouth              = src.mouth;
        dst.facialHair         = src.facialHair;
        dst.haircut            = src.haircut;
        dst.eyebrows           = src.eyebrows;
        dst.pants              = src.pants;
        dst.overpants          = src.overpants;
        dst.undertop           = src.undertop;
        dst.overtop            = src.overtop;
        dst.shoes              = src.shoes;
        dst.headAccessory      = src.headAccessory;
        dst.faceAccessory      = src.faceAccessory;
        dst.earAccessory       = src.earAccessory;
        dst.skinFeature        = src.skinFeature;
        dst.gloves             = src.gloves;
        dst.cape               = src.cape;
    }

    private static String snapshotSkin(PlayerSkin s) {
        return "{body=" + s.bodyCharacteristic
                + " haircut=" + s.haircut
                + " eyes=" + s.eyes
                + " face=" + s.face
                + " mouth=" + s.mouth
                + " ears=" + s.ears
                + " eyebrows=" + s.eyebrows
                + " facialHair=" + s.facialHair
                + " undertop=" + s.undertop
                + " overtop=" + s.overtop
                + " pants=" + s.pants
                + " overpants=" + s.overpants
                + " shoes=" + s.shoes
                + " underwear=" + s.underwear
                + " gloves=" + s.gloves
                + " headAcc=" + s.headAccessory
                + " faceAcc=" + s.faceAccessory
                + " earAcc=" + s.earAccessory
                + " skinFeature=" + s.skinFeature
                + "}";
    }

    private record SkinField(String name, java.util.function.Consumer<PlayerSkin> mutator) {
        void apply(PlayerSkin skin) { mutator.accept(skin); }
    }

    // ---------------------------------------------------------------
    // Compound ID builders — shared with ElfSage, copied rather than imported
    // to keep the two NPC appearance systems independent.
    // ---------------------------------------------------------------

    private static String buildCompound(Map<String, ?> categoryMap, CosmeticRegistry reg,
                                         String partId, String... preferredKeys) {
        if (categoryMap == null || !categoryMap.containsKey(partId)) {
            return partId;
        }
        Object value = categoryMap.get(partId);
        if (!(value instanceof PlayerSkinPart part)) return partId;

        List<String> textureKeys = getTextureKeys(part, reg);
        if (textureKeys.isEmpty()) return partId;

        String textureKey = textureKeys.get(0);
        for (String pref : preferredKeys) {
            if (pref == null) continue;
            boolean found = false;
            for (String key : textureKeys) {
                if (key.equalsIgnoreCase(pref)) { textureKey = key; found = true; break; }
            }
            if (found) break;
            for (String key : textureKeys) {
                if (key.toLowerCase().contains(pref.toLowerCase())) { textureKey = key; found = true; break; }
            }
            if (found) break;
        }

        // Some parts require a third-level variant (e.g. Dungarees.GreyLight.ShortDungareesRegular).
        // If we leave it out, validateSkin throws "Unknown pants: Dungarees.GreyLight". The variant
        // choice is structural (cut of the garment, not its color) so we pick the first one
        // deterministically — the seed-driven randomness is already carried by the texture key.
        String variant = firstVariantKey(part);
        if (variant != null) {
            return partId + "." + textureKey + "." + variant;
        }
        return partId + "." + textureKey;
    }

    private static String firstVariantKey(PlayerSkinPart part) {
        try {
            Map<String, ?> variants = part.getVariants();
            if (variants == null || variants.isEmpty()) return null;
            return variants.keySet().iterator().next();
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildCompoundWithVariant(Map<String, ?> categoryMap, CosmeticRegistry reg,
                                                    String partId, String variantKey) {
        if (categoryMap == null || !categoryMap.containsKey(partId)) return partId;
        Object value = categoryMap.get(partId);
        if (!(value instanceof PlayerSkinPart part)) return partId;

        List<String> textureKeys = getTextureKeys(part, reg);
        if (textureKeys.isEmpty()) return partId + "." + variantKey;
        return partId + "." + textureKeys.get(0) + "." + variantKey;
    }

    private static List<String> getTextureKeys(PlayerSkinPart part, CosmeticRegistry reg) {
        List<String> keys = new ArrayList<>();
        String gsName = part.getGradientSet();
        if (gsName != null) {
            Map<String, PlayerSkinGradientSet> gradientSets = reg.getGradientSets();
            if (gradientSets != null) {
                PlayerSkinGradientSet gs = gradientSets.get(gsName);
                if (gs != null && gs.getGradients() != null) {
                    keys.addAll(gs.getGradients().keySet());
                }
            }
        }
        if (part.getTextures() != null) {
            for (String key : part.getTextures().keySet()) {
                if (!keys.contains(key)) keys.add(key);
            }
        }
        return keys;
    }
}
