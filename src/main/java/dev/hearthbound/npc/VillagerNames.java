package dev.hearthbound.npc;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import dev.hearthbound.npc.appearance.BodyArchetype;

/**
 * Deterministic first/last name picker for human villagers. Seeded so regenerating from a
 * saved {@code skinSeed} produces the same name as the first roll.
 *
 * <p>First names are split by BodyArchetype so a visually masculine villager never gets
 * a feminine name and vice versa. The surname pool is shared — family lines don't care
 * about body type. Surnames lean on historical settler naming: patronymic ("Halladottir",
 * "MacLeod"), trait-nicknames ("Coldfoot", "Quickstep"), and family lines common in
 * 18–19th c. Scots-Irish and Scandinavian North American settlers ("Anderson", "Doherty").
 */
public final class VillagerNames {

    private static final String[] MASC_FIRST = {
            "Alden", "Bram", "Corin", "Eadric", "Fen", "Halden",
            "Joran", "Niall", "Odric", "Pell", "Rowan", "Ulric", "Yorick"
    };

    private static final String[] FEM_FIRST = {
            "Ada", "Anya", "Cass", "Della", "Elin", "Gale", "Hester",
            "Ingrid", "Kira", "Mira", "Sable", "Tamsin", "Vasha", "Wenna"
    };

    // Shared surnames — family lines and nicknames that work for any body archetype.
    private static final String[] LAST_SHARED = {
            // Gaelic / Scots-Irish family lines
            "MacLeod", "MacRae", "Doherty", "Callaghan", "Rourke", "Kerrigan",
            "Anderson", "Harrow", "Blackwood", "Donnelly",
            // English-settler family lines
            "Ashford", "Whitlock", "Elmore", "Hollister", "Dunmore", "Tilden",
            // Trait nicknames (earned, not gendered)
            "Coldfoot", "Brightcup", "Softhand", "Longtongue", "Quickstep"
    };

    // Nordic patronymics (-son / -sen) — only for Masc archetype.
    private static final String[] LAST_MASC = {
            "Arinsson", "Brinjar", "Torvald", "Eiriksen"
    };

    // Nordic matronymics (-dottir) — only for Fem archetype.
    private static final String[] LAST_FEM = {
            "Halladottir", "Sigridsdottir", "Thoradottir"
    };

    private VillagerNames() {}

    /**
     * Rolls a full name from a single seed, matching the given BodyArchetype and avoiding
     * full names (first + last) already present in the village.
     *
     * <p>Strategy: pick a first name for the archetype, then scan the last-name pool for a
     * combo not yet taken. If the first+last space is exhausted, try other first names.
     * Last resort: append " II" to guarantee uniqueness.
     *
     * @param takenFullNames set of "First Last" strings already in the village
     */
    public static String[] rollHumanName(long seed, BodyArchetype body, Set<String> takenFullNames) {
        Random rng = new Random(seed);
        String[] firstPool = (body == BodyArchetype.Masc) ? MASC_FIRST : FEM_FIRST;
        String[] lastPool = lastPoolFor(body);

        Set<String> takenLast = new HashSet<>();
        for (String full : takenFullNames) {
            int sp = full.lastIndexOf(' ');
            if (sp >= 0) takenLast.add(full.substring(sp + 1));
        }

        int firstStart = rng.nextInt(firstPool.length);
        for (int fi = 0; fi < firstPool.length; fi++) {
            String first = firstPool[(firstStart + fi) % firstPool.length];
            int lastStart = rng.nextInt(lastPool.length);
            for (int li = 0; li < lastPool.length; li++) {
                String last = lastPool[(lastStart + li) % lastPool.length];
                if (!takenLast.contains(last) && !takenFullNames.contains(first + " " + last)) {
                    return new String[]{first, last};
                }
            }
        }
        // Every archetype combo with a unique surname taken — fall back to combined first pool.
        String[] combined = combinedFirst();
        int cStart = rng.nextInt(combined.length);
        for (int fi = 0; fi < combined.length; fi++) {
            String first = combined[(cStart + fi) % combined.length];
            int lastStart = rng.nextInt(lastPool.length);
            for (int li = 0; li < lastPool.length; li++) {
                String last = lastPool[(lastStart + li) % lastPool.length];
                if (!takenLast.contains(last) && !takenFullNames.contains(first + " " + last)) {
                    return new String[]{first, last};
                }
            }
        }
        // Entire name space exhausted (village > ~400 residents) — suffix as last resort.
        String first = firstPool[new Random(seed).nextInt(firstPool.length)];
        String last = lastPool[new Random(seed).nextInt(lastPool.length)];
        return new String[]{first + " II", last};
    }

    /** Legacy overload — no village context (e.g. name preview UI). Uses shared surnames only. */
    public static String[] rollHumanName(long seed) {
        Random rng = new Random(seed);
        String[] combined = combinedFirst();
        String first = combined[rng.nextInt(combined.length)];
        String last = LAST_SHARED[rng.nextInt(LAST_SHARED.length)];
        return new String[]{first, last};
    }

    // 30% chance to use an archetype-specific patronymic/matronymic, rest shared.
    private static String[] lastPoolFor(BodyArchetype body) {
        String[] specific = (body == BodyArchetype.Masc) ? LAST_MASC : LAST_FEM;
        String[] pool = new String[specific.length + LAST_SHARED.length];
        System.arraycopy(LAST_SHARED, 0, pool, 0, LAST_SHARED.length);
        System.arraycopy(specific, 0, pool, LAST_SHARED.length, specific.length);
        return pool;
    }

    private static String[] combinedFirst() {
        String[] result = new String[MASC_FIRST.length + FEM_FIRST.length];
        System.arraycopy(MASC_FIRST, 0, result, 0, MASC_FIRST.length);
        System.arraycopy(FEM_FIRST, 0, result, MASC_FIRST.length, FEM_FIRST.length);
        return result;
    }
}
