package dev.hearthbound.npc.appearance;

/**
 * Describes which set of game cosmetic assets a villager's body was built for.
 *
 * <p>Purely internal — never surfaced in UI or in dialog text. Some Hytale cosmetic
 * assets (bras, facial hair, certain haircuts like MaleElf or Pigtails) are
 * authored against one body geometry and look broken or nonsensical when mixed
 * with another. This enum names the two asset families so we can keep outfits
 * visually coherent; it is not a social category.
 */
public enum BodyArchetype {
    Masc,
    Fem
}
