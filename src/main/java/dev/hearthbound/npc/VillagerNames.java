package dev.hearthbound.npc;

import java.util.Random;

/**
 * Deterministic first/last name picker for human villagers. Seeded so regenerating from a
 * saved {@code skinSeed} produces the same name as the first roll.
 *
 * <p>The surname pool leans on historical settler naming: patronymic ("Halladottir",
 * "MacLeod"), trait-nicknames ("Coldfoot", "Quickstep"), and family lines common in
 * 18–19th c. Scots-Irish and Scandinavian North American settlers ("Anderson", "Doherty").
 * No occupational names yet — professions aren't assigned, so a "Fletcher" without a bow
 * reads as noise.
 */
public final class VillagerNames {

    private static final String[] HUMAN_FIRST = {
            "Ada", "Alden", "Anya", "Bram", "Cass", "Corin", "Della", "Eadric",
            "Elin", "Fen", "Gale", "Halden", "Hester", "Ingrid", "Joran", "Kira",
            "Lena", "Mira", "Niall", "Odric", "Pell", "Rowan", "Sable", "Tamsin",
            "Ulric", "Vasha", "Wenna", "Yorick"
    };

    private static final String[] HUMAN_LAST = {
            // Nordic patronymic — short, rolls well off the tongue
            "Arinsson", "Halladottir", "Brinjar", "Sigridsdottir", "Torvald", "Eiriksen",

            // Gaelic / Scots-Irish family lines that travelled to North America
            "MacLeod", "MacRae", "Doherty", "Callaghan", "Rourke", "Kerrigan",
            "Anderson", "Harrow", "Blackwood", "Donnelly",

            // English-settler family lines — ordinary sounding, not invented
            "Ashford", "Whitlock", "Elmore", "Hollister", "Dunmore", "Tilden",

            // Trait nicknames — earned reputations
            "Coldfoot", "Brightcup", "Softhand", "Grimbeard", "Longtongue", "Quickstep"
    };

    private VillagerNames() {}

    /**
     * Rolls both names from a single seed. Same seed always produces the same pair,
     * which is how skins and names stay consistent across server restarts.
     */
    public static String[] rollHumanName(long seed) {
        Random rng = new Random(seed);
        String first = HUMAN_FIRST[rng.nextInt(HUMAN_FIRST.length)];
        String last = HUMAN_LAST[rng.nextInt(HUMAN_LAST.length)];
        return new String[]{first, last};
    }
}
