package dev.hearthbound.npc.appearance;

/**
 * Visual style family for a villager.
 *
 * <p>Peasant is the default and always used for the first few settlers (normalization
 * phase). Citizen and Exotic unlock as the village grows — see {@link VillagerAppearance}
 * for the gate logic.
 *
 * <ul>
 *   <li>{@link #Peasant} — natural fabrics, earthy palette, no makeup or jewelry.</li>
 *   <li>{@link #Citizen} — richer clothing, glasses, earrings, makeup allowed.</li>
 *   <li>{@link #Exotic} — non-human eyes/ears/mouth, unusual hair colors. Rare.</li>
 * </ul>
 */
public enum StyleArchetype {
    Peasant,
    Citizen,
    Exotic
}
