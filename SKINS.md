# Villager Skin System

How NPC appearance is generated, where to change it, and what tripped us up the
first time. Read this before touching `VillagerAppearance` or `CosmeticPools`.

## Files

| File | Purpose |
|---|---|
| `convergence/src/main/java/dev/hearthbound/npc/VillagerAppearance.java` | Skin generation pipeline. Public API: `apply(ref, store, seed, villagerIndex)`. |
| `convergence/src/main/java/dev/hearthbound/npc/appearance/CosmeticPools.java` | Whitelists of allowed cosmetic IDs, palettes, helpers. **This is what you edit when designs change.** |
| `convergence/src/main/java/dev/hearthbound/npc/appearance/BodyArchetype.java` | `Masc` / `Fem` — which set of game cosmetic assets the body was authored against. Internal terminology, never surfaced in UI. |
| `convergence/src/main/java/dev/hearthbound/npc/appearance/StyleArchetype.java` | `Peasant` / `Citizen` / `Exotic` — visual style family. |
| `convergence/src/main/java/dev/hearthbound/npc/ElfSage.java` | Elf sage's hand-tuned skin (`createSageSkin`). Independent from villager generation. Don't break this — the sage's look is locked in. |
| `convergence/devserver/cosmetics_dump.txt` | Authoritative catalog of every cosmetic ID in the running game build. Refresh with `/hb cosmetics` after game updates. |

## Pipeline (high level)

1. `apply(ref, store, seed, villagerIndex)` is called once per villager. Seed is
   stored on the villager via `VillagerData.skinSeed` so the same skin rebuilds
   after a server restart.
2. `createHumanSkin(seed, villagerIndex)` → builds a `PlayerSkin`:
   - Roll body archetype (50/50 Masc/Fem from seed).
   - Roll style archetype via `rollStyle(rng, villagerIndex)`. **First 5 villagers
     are forced to Peasant** (normalisation phase). After that, Citizen/Exotic
     chances ramp slowly with the index.
   - Pick **one** hair color and reuse it across haircut + eyebrows + facial hair
     so a man's beard always matches his head.
   - Pick **one** fabric tone and steer every clothing slot toward it via
     `buildCompound`, which falls back to the closest available palette entry.
3. `createModel(skin, 1.0f)` → if it throws, `tryRepairSkin` swaps each field
   one at a time with a donor random skin until the model builds. Logs which
   field was at fault.
4. PlayerSkin + Model components are put on the entity, then
   `fixPersistentModelScale` rewrites `PersistentModel` with `scale=1.0f` so the
   NPC survives chunk reloads (engine bug — see MEMORY.md).

## Compound ID format (CRITICAL)

The cosmetics registry uses three different ID shapes:

- **Bare ID**: `Default`, `Face_Aged`, `Mouth_Thin`. Only `ears`, `face`, `mouth`.
- **Two-part**: `<PartId>.<TextureKey>` e.g. `Long.Black`, `Forest_Guardian.Brown`.
- **Three-part**: `<PartId>.<TextureKey>.<VariantKey>` e.g.
  `Dungarees.GreyLight.ShortDungareesRegular`, `Cape_Forest_Guardian.NoNeck`.

`buildCompound` in `VillagerAppearance` handles all three automatically. **If you
add an ID that has variants** (look for `x variants:` in the dump), the helper
will append the first variant key. Pure-cape entries listed as
`Cape_Foo.NoNeck` directly in the dump are full IDs — don't try to add a color.

## Editing whitelists — recipe

When you want to add or remove an outfit option:

1. Open `cosmetics_dump.txt` and find the ID. Check what category it lives in.
2. Open `CosmeticsPools.java`, find the matching list (e.g. `OVERTOPS_PEASANT_ANY`).
3. Add or remove the bare ID (e.g. `"Forest_Guardian"`). Don't add the texture key
   — `buildCompound` picks one based on the seed and steers toward
   `FABRIC_TONES_NATURAL`.
4. If unsure what the asset looks like, leave it out. A smaller pool of safe
   villagers beats a larger pool of clowns.

If a villager appears with a broken outfit, check the server log — `tryRepairSkin`
prints which field it had to swap and the full skin snapshot. Use that to figure
out which whitelist entry was the culprit.

## Reserved for future profession outfits

`HEAD_PEASANT_ANY`, `HEAD_CITIZEN_ANY`, `HEAD_CITIZEN_FEM` are intentionally
**empty** — every named hat (`Hard_Hat`, `StrawHat`, `CowboyHat`, etc.) is meant
to be assigned by profession ("the lumberjack wears a leather cap"), not by
ambient roll. Same for `Gloves_*`, `MiningGloves`, etc. See the comments above
each list — they name the assets we're saving.

When professions land:
- Plumb the role/profession into `applyOutfit` / `applyAccessories` (probably
  via a new parameter, since seed/index alone don't carry profession info).
- Override the relevant slots **after** the ambient pool has run, so non-uniform
  parts (face, ears, body shape) still vary from villager to villager.

## Skin tones (CRITICAL — easy to break)

The game's `Skin` gradient set has 47 entries indexed `01..53` (with gaps). Many
of them are **non-human colors** (green, pink, violet, saturated red/blue) that
exist for future races. We've manually curated a human-only subset:

- `SKIN_TONES_LIGHT`: `01 02 03 08 09 10 15` — pale through tan / asian-ish.
- `SKIN_TONES_DARK`: `04 05 06 07 11 12 13 14 16` — brown through near-black.
- Weighted **70/30 light/dark** in `applyHead`.

Everything else (17–53 minus the dark set above) is **off-limits for villagers**,
reserved for non-human races. Don't expand the human pool without visually
verifying — use `/hb testskin` to see all 47 tones in a grid.

## Hair colors

- `HAIR_COLORS_NATURAL`: realistic colors (black, brown shades, blond, red, grey, white).
- `HAIR_COLORS_EXOTIC`: dyed colors (pink, purple, blue, turquoise, etc.).

The Exotic archetype uses `HAIR_COLORS_EXOTIC` ~60% of the time, otherwise
natural. Peasant and Citizen are **always** natural.

The chosen color is reused for **haircut + eyebrows + facial hair** in the same
villager — never let them diverge, that's the "green hair + red beard" anti-pattern
we worked to eliminate.

## Capes

`skin.cape = null` for **every** villager. Capes are a Hytale Premium cosmetic
and we don't use them on NPCs. Don't add a cape pool unless the rules change.

## Debug commands

- `/hb cosmetics` — dump current cosmetics registry to `devserver/cosmetics_dump.txt`.
  Run after every game update to catch renamed/removed IDs.
- `/hb testvillagers <N>` — spawn N test villagers in a 5-per-row grid in front
  of you, indexed `0..N-1`. The index drives the archetype gate, so the first 5
  are guaranteed Peasants. No persistence; each invocation restarts at index 0.
- `/hb testskin` — spawn one frozen villager per skin tone (01..53) in an 8-per-row
  grid, with a chat report of which tone is at which (row, col). Use this when
  curating `SKIN_TONES_*` pools.

These commands spawn raw `Villager_Human` NPCs without `VillagerData`, so they
don't count toward `village.villagerCount` and don't trigger any village systems.
The frozen ones from `/hb testskin` survive restarts but won't move.

## Common failure modes

| Symptom | Cause | Fix |
|---|---|---|
| `Unknown <category>: Foo.Bar` from `validateSkin` | Two-part ID for an item that requires a third variant. | `buildCompound` should already auto-append the variant. If not, the part isn't in the registry under that ID — verify spelling against the dump. |
| `Index 1 out of bounds for length 1` from `createModel` | The engine choked on some part — usually a third-party variant we built incorrectly. | `tryRepairSkin` will identify the field and log it. Then either remove the ID from the whitelist or fix the variant logic. |
| Villager has bright random clothes | `tryRepairSkin` couldn't fix any single slot, fell back to full random donor. | Check the log for the snapshot. Probably two whitelisted IDs are simultaneously bad (rare). |
| Beard color ≠ hair color | Someone added separate color logic for `facialHair`. | All three (haircut, eyebrows, facialHair) MUST use the same `hairColor` variable in `applyHead`. |
| First villager looks exotic | Normalization broken — check `rollStyle`'s `villagerIndex < NORMALIZATION_THRESHOLD` branch. | Don't change `villagerIndex < 5` without a reason. |

## Things explicitly NOT to do

- Don't surface `BodyArchetype` or `StyleArchetype` in any user-facing string.
  These are internal grouping for engine assets, not categories the player should see.
- Don't call `generateRandomSkin` and ship the result without overrides — it
  produces visual noise (mismatched hair colors, costume tops, premium capes).
  Use it only as a base or a fallback donor.
- Don't add separate color rolls per slot. One `hairColor`, one `fabricTone`
  per villager. Coherence beats variety.
- Don't drop the seed from the API. Skins must be reproducible from
  `VillagerData.skinSeed` so they survive restarts without storing every ID.
