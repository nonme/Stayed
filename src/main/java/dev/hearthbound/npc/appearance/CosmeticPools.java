package dev.hearthbound.npc.appearance;

import java.util.List;
import java.util.Random;

/**
 * Whitelists of cosmetic asset IDs grouped by archetype.
 *
 * <p>Every ID here must exist in the Hytale cosmetics registry — see
 * {@code devserver/cosmetics_dump.txt} for the authoritative catalog. When the
 * registry changes (game update), re-run {@code /hb cosmetics} in-game and
 * reconcile any entries that became invalid.
 *
 * <p>The lists are intentionally conservative: anything whose look we cannot
 * predict from the ID alone, or that reads as costume/event/profession-specific,
 * is left out. A smaller pool of safe-looking NPCs beats a larger pool of clowns.
 *
 * <p>Future professions will take precedence over the Peasant/Citizen split —
 * e.g. a lumberjack gets a lumberjack outfit regardless of style archetype.
 * That wiring lives elsewhere; this class only provides the ambient random pool.
 */
public final class CosmeticPools {

    private CosmeticPools() {}

    // ---------------------------------------------------------------
    // Haircuts
    // ---------------------------------------------------------------
    // Naming convention check: we only keep cuts whose style is readable from the
    // ID. Samurai / VikingWarrior / Witch / Berserker and similar themed cuts are
    // excluded — we cannot tell what they look like without loading the model.
    // Variant-based haircuts (Dreadlocks.*.Dreadlocks01) are excluded too — our
    // buildCompound helper only produces two-part IDs.

    /** Short, plain cuts that read as generic male or unisex. */
    public static final List<String> HAIRCUTS_MASC = List.of(
            "BuzzCut",
            "GenericShort",
            "Simple",
            "Messy",
            "MessyMop",
            "MessyBobcut",
            "Rustic",
            "Balding",
            "Lazy",
            "Cowlick",
            "Fringe",
            "Bangs",
            "ShortDreads",
            "Undercut"
    );

    /** Long or tied cuts that read as generic female. */
    public static final List<String> HAIRCUTS_FEM = List.of(
            "Long",
            "LongBangs",
            "LongCurly",
            "LongTied",
            "WavyLong",
            "MorningLong",
            "Bun",
            "StraightHairBun",
            "SideBuns",
            "Braid",
            "BraidDouble",
            "ThickBraid",
            "Pigtails",
            "SmallPigtails",
            "LongPigtails",
            "PonyTail",
            "SidePonytail",
            "WidePonytail",
            "Bangs",
            "CentrePart",
            "MidSinglePart",
            "DoublePart"
    );

    /** Cuts that work for any body archetype without reading gendered. */
    public static final List<String> HAIRCUTS_UNISEX = List.of(
            "GenericMedium",
            "GenericLong",
            "BobCut",
            "Curly",
            "CurlyShort",
            "MediumCurly",
            "Morning",
            "Windswept",
            "Cornrows",
            "BantuKnot"
    );

    /** Exotic cuts — colorful dyes and sharp silhouettes. Only in Exotic archetype. */
    public static final List<String> HAIRCUTS_EXOTIC = List.of(
            "Mohawk",
            "RaiderMohawk",
            "SpikyMohawl",
            "SpikedUp",
            "Emo",
            "EmoBangs",
            "EmoWavy",
            "CuteEmoBangs",
            "ElfBackBun",
            "MaleElf",
            "FrizzyLong",
            "FrizzyVolume"
    );

    // ---------------------------------------------------------------
    // Eyebrows
    // ---------------------------------------------------------------

    /** Thick/heavy brows — only on Masc bodies. */
    public static final List<String> EYEBROWS_MASC_ONLY = List.of(
            "Bushy",
            "Thick",
            "Square",
            "Heavy"
    );

    /** Brows that work on any body. */
    public static final List<String> EYEBROWS_ANY = List.of(
            "Medium",
            "Thin",
            "RoundThin",
            "SmallRound",
            "BushyThin",
            "Plucked",
            "Shaved",
            "Serious",
            "Angry",
            "Large"
    );

    // ---------------------------------------------------------------
    // Eyes
    // ---------------------------------------------------------------

    public static final List<String> EYES_NORMAL = List.of(
            "Plain_Eyes",
            "Plain_Square_Eyes",
            "Large_Eyes",
            "Medium_Eyes",
            "Square_Eyes",
            "Almond_Eyes"
    );

    public static final List<String> EYES_EXOTIC = List.of(
            "Cat_Eyes",
            "Demonic_Eyes",
            "Goat_Eyes",
            "Reptile_Eyes"
    );

    // ---------------------------------------------------------------
    // Mouths
    // ---------------------------------------------------------------
    // Mouth_Long excluded per design review (looks off).

    public static final List<String> MOUTHS_NORMAL = List.of(
            "Mouth_Default",
            "Mouth_Thin",
            "Mouth_Tiny"
    );

    public static final String MOUTH_MAKEUP = "Mouth_Makeup";

    public static final List<String> MOUTHS_EXOTIC = List.of(
            "Mouth_Vampire",
            "Mouth_Orc",
            "Mouth_Cute"
    );

    // ---------------------------------------------------------------
    // Ears
    // ---------------------------------------------------------------

    public static final String EARS_HUMAN = "Default";

    /** Small elf ears can read as feline when paired with Cat_Eyes — used in Exotic only. */
    public static final List<String> EARS_EXOTIC = List.of(
            "Elf_Ears",
            "Elf_Ears_Small"
    );

    // ---------------------------------------------------------------
    // Faces
    // ---------------------------------------------------------------

    public static final List<String> FACES_NORMAL = List.of(
            "Face_Neutral",
            "Face_Aged",
            "Face_Older2",
            "Face_Sunken",
            "Face_Tired_Eyes",
            "Face_Neutral_Freckles",
            "Face_Almond_Eyes"
    );

    /** Lightly stubbled face — alternative to an actual beard. Masc only. */
    public static final String FACE_STUBBLE = "Face_Stubble";

    /** Rare addition for weathered villagers. */
    public static final String FACE_SCAR = "Face_Scar";

    /** Citizens may have makeup faces. */
    public static final List<String> FACES_CITIZEN = List.of(
            "Face_MakeUp",
            "Face_MakeUp_Older",
            "Face_MakeUp_HeavyEyeliner",
            "Face_MakeUp_6",
            "Face_MakeUp_Highlight",
            "Face_MakeUp_Freckles",
            "Face_Makeup_Blush",
            "Face_Make_Up_2"
    );

    // ---------------------------------------------------------------
    // Facial hair (Masc only)
    // ---------------------------------------------------------------

    /** Light stubble-type facial hair — used when we want a beard but keep it subtle. */
    public static final List<String> FACIAL_HAIR_LIGHT = List.of(
            "ThinGoatee",
            "SoulPatch",
            "Short_Trimmed",
            "Trimmed",
            "Moustache",
            "TwirlyMoustache",
            "Goatee"
    );

    /** Full beards — the majority of bearded men. */
    public static final List<String> FACIAL_HAIR_FULL = List.of(
            "Medium",
            "Groomed",
            "Groomed_Large",
            "Beard_Large",
            "Beard_Anchor",
            "Chin_Curtain",
            "Handlebar",
            "Hip",
            "GoateeLong",
            "CurlyLongBeard",
            "WavyLongBeard",
            "Soldier",
            "Stylish",
            "Moustache_Split",
            "Moustache_SplitLong",
            "PirateGoatee",
            "PirateBeard",
            "VikingBeard",
            "VikingBeard2",
            "DoubleBraid",
            "TripleBraid"
    );

    // ---------------------------------------------------------------
    // Underwear (hard gate on archetype)
    // ---------------------------------------------------------------

    public static final List<String> UNDERWEAR_MASC = List.of("Boxer", "Suit");
    public static final List<String> UNDERWEAR_FEM = List.of("Bra", "Bandeau");

    // ---------------------------------------------------------------
    // Undertops (shirts)
    // ---------------------------------------------------------------

    public static final List<String> UNDERTOPS_PEASANT_ANY = List.of(
            "LongSleeveShirt",
            "VNeck_Shirt",
            "Wide_Neck_Shirt",
            "Short_Sleeves_Shirt",
            "RibbedLongShirt",
            "LongSleevePeasantTop",
            "Crinkled_Top",
            "Belt_Shirt",
            "Stylish_Belt_Shirt",
            "StripedLong",
            "DoubleShirt",
            "VikingShirt",
            "FarmerTop",
            "Mercenary_Top"
    );

    public static final List<String> UNDERTOPS_PEASANT_FEM = List.of(
            "Flowy_Shirt",
            "Frilly_Shirt"
    );

    public static final List<String> UNDERTOPS_CITIZEN_ANY = List.of(
            "LongSleeveShirt_ButtonUp",
            "LongSleeveShirt_GoldTrim",
            "SmartShirt",
            "TieShirt",
            "ColouredStripes",
            "ColouredSleeves",
            "PastelFade"
    );

    // ---------------------------------------------------------------
    // Overtops (jackets / vests / tunics)
    // ---------------------------------------------------------------

    public static final List<String> OVERTOPS_PEASANT_ANY = List.of(
            "RobeOvertops",
            "Tunic_Long",
            "Tunic_Weathered",
            "Tunic_Villager",
            "FarmerVest",
            "LeatherVest",
            "ForestVest",
            "RaggedVest",
            "VikingVest",
            "MiniLeather",
            "StitchedShirt",
            "BulkyShirt_RuralShirt",
            "BulkyShirt_StomachWrap",
            "BulkyShirtLong",
            "BulkyShirt_Scarf",
            "MessyShirt",
            "PlainJersey",
            "Wool_Jersey",
            "LooseSweater",
            "PlainHoodie",
            "Polarneck",
            "Scarf",
            "Scarf_Large"
    );

    public static final List<String> OVERTOPS_PEASANT_FEM = List.of(
            "SleevedDress",
            "SleevedDresswJersey",
            "SimpleDress",
            "Farmer_Dress",
            "Adventurer_Dress",
            "FlowyHalf"
    );

    public static final List<String> OVERTOPS_CITIZEN_ANY = List.of(
            "Coat",
            "TrenchCoat",
            "LongBeltedJacket",
            "JacketLong",
            "Jacket",
            "StylishJacket",
            "Fancy_Coat",
            "Suit_Jacket",
            "DoubleButtonJacket",
            "LongCardigan",
            "Merchant_Tunic",
            "FurLinedJacket",
            "Winter_Jacket",
            "PuffyJacket",
            "JacketShort",
            "BulkyShirt_FancyWaistcoat",
            "GoldtrimJacket"
    );

    // ---------------------------------------------------------------
    // Pants / skirts
    // ---------------------------------------------------------------

    public static final List<String> PANTS_PEASANT_ANY = List.of(
            "Pants_Slim",
            "Pants_Slim_Faded",
            "Pants_Straight_WreckedJeans",
            "Jeans",
            "JeansStrapped",
            "Slim_Short",
            "Shorty_Rotten",
            "Shorty_Mossy",
            "Bermuda_Rolled",
            "Forest_Bermuda",
            "Villager_Bermuda",
            "ExplorerShorts",
            "StripedPants",
            "SurvivorPants",
            "BulkySuede",
            "ApprenticePants",
            "LeatherPants",
            "Explorer_Trousers",
            "Dungarees"
    );

    public static final List<String> PANTS_PEASANT_FEM = List.of(
            "Skirt",
            "Skirt_Savanna",
            "SimpleSkirt",
            "DenimSkirt",
            "Long_Dress",
            "Frilly_Skirt",
            "Crinkled_Skirt"
    );

    public static final List<String> PANTS_CITIZEN_ANY = List.of(
            "PinstripeTrousers",
            "Colored_Trousers",
            "ColouredKhaki"
    );

    // ---------------------------------------------------------------
    // Shoes
    // ---------------------------------------------------------------

    public static final List<String> SHOES_PEASANT = List.of(
            "BasicShoes",
            "BasicShoes_Buckle",
            "BasicShoes_Strap",
            "BasicShoes_Sandals",
            "BasicBoots",
            "LeatherBoots",
            "SnowBoots",
            "MinerBoots",
            "ScavenverLeatherBoots",
            "Wellies",
            "ThickSandals",
            "BasicSandals",
            "Boots_Thick",
            "Boots_Long",
            "HiBoots"
    );

    public static final List<String> SHOES_CITIZEN = List.of(
            "BasicShoes_Shiny",
            "Flats_Styled",
            "SlipOns",
            "FashionableBoots",
            "Merchant_Boots"
    );

    // ---------------------------------------------------------------
    // Head accessories
    // ---------------------------------------------------------------
    // Peasants: rare hats (~25%). Citizens: more often (~50%). Flowers on Fem only.

    /**
     * Currently unused for ambient spawns — hats are reserved for future profession
     * outfits (strawhat = farmer, bandana = sailor, etc.). Left as a reference list
     * to reach for when profession-specific skins come online.
     */
    public static final List<String> HEAD_PEASANT_ANY = List.of();

    public static final List<String> HEAD_PEASANT_FEM = List.of(
            "Ribbon",
            "HairRose",
            "HairDaisy",
            "HairPeony",
            "HairHibiscus",
            "HeadDaliah",
            "FlowerCrown"
    );

    /** Same reasoning as {@link #HEAD_PEASANT_ANY} — reserved for profession outfits. */
    public static final List<String> HEAD_CITIZEN_ANY = List.of();

    public static final List<String> HEAD_CITIZEN_FEM = List.of();

    // ---------------------------------------------------------------
    // Face accessories (glasses — Citizen only, mostly)
    // ---------------------------------------------------------------

    public static final String FACE_ACC_WHEAT = "MouthWheat";

    public static final List<String> FACE_ACC_CITIZEN = List.of(
            "Glasses",
            "RoundGlasses",
            "GlassesTiny",
            "BusinessGlasses",
            "Glasses_Monocle",
            "LargeGlasses"
    );

    public static final String FACE_ACC_EYEPATCH = "EyePatch";

    // ---------------------------------------------------------------
    // Ear accessories (Citizen only)
    // ---------------------------------------------------------------

    public static final List<String> EAR_ACC_CITIZEN = List.of(
            "EarHoops",
            "DoubleEarrings",
            "SimpleEarring",
            "SilverHoopsBead"
    );

    // ---------------------------------------------------------------
    // Hair colors (used for haircut + eyebrows + facial hair — always one color)
    // ---------------------------------------------------------------

    /** Natural hair colors used in Peasant and Citizen archetypes. */
    public static final List<String> HAIR_COLORS_NATURAL = List.of(
            "Black",
            "PitchBlack",
            "BrownDark",
            "BrownDarker",
            "BrownSemiDark",
            "Brown",
            "BrownSemiLight",
            "BrownLight",
            "Blond",
            "BlondCaramel",
            "BlondSand",
            "BlondPlatinum",
            "Copper",
            "Red",
            "RedDark",
            "Grey",
            "GreyAsh",
            "White"
    );

    /** Dyed / unnatural hair colors — Exotic only. */
    public static final List<String> HAIR_COLORS_EXOTIC = List.of(
            "Pink",
            "PinkBerry",
            "Bubblegum",
            "Purple",
            "GreyPurple",
            "Lavender",
            "Blue",
            "BlueLight",
            "BlueDark",
            "Blue_Anthracite",
            "Turquoise",
            "Green"
    );

    // ---------------------------------------------------------------
    // Skin tones — curated subset of the 01..53 Skin gradient.
    // ---------------------------------------------------------------
    // The full range contains unnatural colors (green, pink, purple, saturated red/blue)
    // reserved for future non-human races. Only the entries that read as realistic
    // human skin are exposed to the villager generator, split into a lighter group
    // (pale / tan / asian-ish) and a darker group (brown / black / african-ish).
    // Visual reference was captured via /hb testskin — see session notes.

    /** Pale to medium-tan tones. */
    public static final List<String> SKIN_TONES_LIGHT = List.of(
            "01", "02", "03", "08", "09", "10", "15"
    );

    /** Brown to dark tones. */
    public static final List<String> SKIN_TONES_DARK = List.of(
            "04", "05", "06", "07", "11", "12", "13", "14", "16"
    );

    // ---------------------------------------------------------------
    // Generic palette targets — natural tones we try to steer clothing toward
    // when the game item has multiple color options. buildCompound() will pick
    // the closest match from whatever palette the item uses.
    // ---------------------------------------------------------------

    public static final List<String> FABRIC_TONES_NATURAL = List.of(
            "Brown",
            "BrownDark",
            "Green",
            "Blue",
            "BlueDark",
            "Grey",
            "Black",
            "White",
            "Beige"
    );

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    public static <T> T pick(Random rng, List<T> pool) {
        return pool.get(rng.nextInt(pool.size()));
    }

    /**
     * Concatenates pools into one list for a single random pick. The input lists
     * are not copied — callers must not mutate the result.
     */
    @SafeVarargs
    public static <T> List<T> join(List<T>... pools) {
        java.util.List<T> all = new java.util.ArrayList<>();
        for (List<T> p : pools) all.addAll(p);
        return all;
    }
}
