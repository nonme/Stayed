# Cooking Bench Recipes

Reference for all food items craftable on Hytale's vanilla **Cooking Bench**
(`Bench_Cooking`, bench id `Cookingbench`). Source for our Tavern + Cook
profession design.

## Source

All recipes extracted from the official Hytale assets:

- Archive: `~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/install/release/package/game/latest/Assets.zip`
- Bench definition: `Server/Item/Items/Bench/Bench_Cooking.json`
- Food items: `Server/Item/Items/Food/*.json` — each food carries its own
  `Recipe` block; the `BenchRequirement.Id == "Cookingbench"` is what marks a
  recipe as belonging to this bench (as opposed to Campfire/Workbench).
- Resource groups (`Vegetables`, `Fruits`, etc.): defined on parent templates
  `Server/Item/Items/Plant/Crop/_Template/Template_Crop_Item.json` (→ `Vegetables`)
  and `Server/Item/Items/Plant/Fruit/Template_Fruit.json` (→ `Fruits`).
  Children inherit `ResourceTypes` via the `Parent` field.

Extraction was done in one pass on 2026-05-16; rerun if a game update lands.

## Game-wide food tier system

Hytale tags every consumable food with a tier (T1/T2/T3) via the
`Interactions.Secondary` field, e.g. `Root_Secondary_Consume_Food_T2`. The
effect applied on consumption (`Food_Instant_Heal_T2`, `Food_Instant_Heal_T3`,
etc.) scales with the tier. We mirror this with our hunger system: T3 satiates
much more than T1.

| Tier | Hytale Secondary             | Healing effect            | Our planned hunger restore |
|------|------------------------------|---------------------------|----------------------------|
| T1   | `…Consume_Food_T1`           | `Food_Instant_Heal_T1`    | small                      |
| T2   | `…Consume_Food_T2`           | `Food_Instant_Heal_T2`    | medium                     |
| T3   | `…Consume_Food_T3`           | `Food_Instant_Heal_T3`    | large                      |

## Bench categories

`Bench_Cooking` declares three tabs on its UI:

- **Prepared** — assemblies (salads, kebabs) that need no fire
- **Baked** — bread, pies, popcorn — require `Fuel`
- **Ingredients** — intermediate items (e.g. Cheese from Milk)

## Recipes on the Cooking Bench

| Output                | Cat.        | Tier | Quality   | Inputs                                                                                       | Knowledge req. |
|-----------------------|-------------|------|-----------|----------------------------------------------------------------------------------------------|----------------|
| `Food_Bread`          | Baked       | T2   | Uncommon  | 1 `Ingredient_Dough` + 3 `Fuel`                                                              | no             |
| `Food_Popcorn`        | Baked       | T3   | Uncommon  | 2 `Plant_Crop_Corn_Item` + 1 `Ingredient_Salt` + 3 `Fuel`                                    | no             |
| `Food_Pie_Apple`      | Baked       | T3   | Rare      | 1 `Ingredient_Dough` + 3 `Plant_Fruit_Apple` + 1 `Ingredient_Spices` + 3 `Fuel`              | **yes**        |
| `Food_Pie_Meat`       | Baked       | T3   | Rare      | 1 `Ingredient_Dough` + 1 `Meats` + 1 `Ingredient_Spices` + 1 `Ingredient_Salt` + 3 `Fuel`    | **yes**        |
| `Food_Pie_Pumpkin`    | Baked       | T3   | Rare      | 1 `Ingredient_Dough` + 1 `Plant_Crop_Pumpkin_Item` + 1 `Ingredient_Spices` + 3 `Fuel`        | **yes**        |
| `Food_Salad_Berry`    | Prepared    | T2   | Uncommon  | 1 `Plant_Crop_Lettuce_Item` + 5 `Plant_Fruit_Berries_Red`                                    | no             |
| `Food_Salad_Mushroom` | Prepared    | T2   | Uncommon  | 1 `Plant_Crop_Lettuce_Item` + 3 `Mushrooms`                                                  | no             |
| `Food_Salad_Caesar`   | Prepared    | T3   | Rare      | 1 `Plant_Crop_Lettuce_Item` + 1 `Food_Cheese` + 1 `Meats` + 1 `Ingredient_Salt` + 1 `Ingredient_Spices` | **yes** |
| `Food_Kebab_Vegetable`| Prepared    | T2   | Uncommon  | 1 `Ingredient_Stick` + 4 `Vegetables`                                                        | no             |
| `Food_Kebab_Meat`     | Prepared    | T2   | Uncommon  | 1 `Ingredient_Stick` + 4 `Meats`                                                             | no             |
| `Food_Kebab_Fruit`    | Prepared    | T2   | Uncommon  | 1 `Ingredient_Stick` + 4 `Fruits`                                                            | no             |
| `Food_Kebab_Mushroom` | Prepared    | T2   | Uncommon  | 1 `Ingredient_Stick` + 3 `Mushrooms`                                                         | no             |
| `Food_Cheese`         | Ingredients | T3   | Uncommon  | 1 `Milk_Bucket` (returns 1 `Container_Bucket`)                                               | no             |

> **`KnowledgeRequired: true`** means the player must learn the recipe from a
> physical recipe item (e.g. `Recipe_Food_Pie_Meat`, found in
> `Server/Item/Items/Recipe/Food/`). In our mod we can either ignore this
> (cook knows all recipes once tavern is built) or use it as a progression
> gate (cook only knows T2 by default, learns T3 via quest/discovery).

## Campfire recipes (for reference, NOT cooking bench)

Tier-1 "just cook over fire" recipes — bench id `Campfire`, type `Processing`:

| Output                  | Inputs                       | Time |
|-------------------------|------------------------------|------|
| `Food_Vegetable_Cooked` | 1 `Vegetables`               | 2s   |
| `Food_Wildmeat_Cooked`  | 1 `Meats`                    | 2s   |
| `Food_Fish_Grilled`     | 1 `Food_Fish_Raw`            | 2s   |

These exist as a fallback path: even before the tavern is built, raw meat/veg
could be turned into T1 cooked food via a campfire. We could either include a
campfire mechanic later or skip this tier.

## Resource group mapping (what counts as `Vegetables` / `Meats` / etc.)

Recipes that take `ResourceTypeId: "X"` accept *any* item tagged with that
resource type. Verified mappings from the assets:

- **`Vegetables`** — every `Plant_Crop_*_Item` inherits from
  `Template_Crop_Item`, which sets `ResourceTypes: [{Id: Vegetables}]`. So
  Carrot, Lettuce, Wheat, Corn, Pumpkin, Potato, Tomato, Onion, Aubergine,
  Cauliflower, Chilli, Rice, Turnip all count.
- **`Fruits`** — every `Plant_Fruit_*` inherits from `Template_Fruit` →
  `ResourceTypes: [{Id: Fruits}]`. Apple, Berries_Red, Mango, Coconut, Azure,
  Pinkberry, Spiral, Windwillow.
- **`Meats`** — `Food_Beef_Raw`, `Food_Chicken_Raw`, `Food_Pork_Raw`,
  `Food_Wildmeat_Raw` all declare `ResourceTypes: [{Id: Meats}]` directly.
- **`Mushrooms`** — verify via template lookup if/when used (TODO).
- **`Fuel`** — coal/wood/sticks; verify when wiring up baked recipes.
- **`Milk_Bucket`** — only one source item.

## Intermediate ingredients

A few recipes reference items that are themselves crafted, not harvested:

- `Ingredient_Dough` — used in Bread + all Pies. Source recipe not yet found
  (likely Wheat → Flour → Dough chain on Farming or Cooking bench; investigate
  when we wire up T2 baked).
- `Ingredient_Salt`, `Ingredient_Spices` — used in Popcorn, Caesar, several
  pies. Source not yet found (may be Mining/Farming drops). Investigate when
  needed.
- `Ingredient_Stick` — used in all kebabs. Likely trivial Workbench recipe;
  for our cook the simplest path is to treat the stick as either auto-supplied
  or sourced from Wood via a sub-recipe.
- `Food_Cheese` — used in Caesar; itself a Cookingbench recipe (Milk_Bucket).

When implementing the cook profession we have two options for these:

1. **Strict**: cook needs the actual `Ingredient_Dough`/`Salt`/`Spices`/`Stick`
   items on the warehouse — full chain. This means we also need to either
   include their source recipes or hand-wave Dough = Wheat in code.
2. **Simplified**: cook abstracts these away. E.g. for our purposes
   "1 Bread = 1 Wheat + 3 Fuel" (we skip the Dough step). Less faithful but
   matches the village-sim feel where the cook "just makes bread from grain".

Decision pending — see Tavern design doc.

## How our Tavern + Cook profession uses these

(Design pending; this section documents intent, not implementation.)

- **Sources**: cook pulls ingredients **from the Warehouse**, cooks them at the
  Tavern's cooking bench, and stores the output back. (User confirmed
  2026-05-16: cook uses warehouse stock; cooked dishes can optionally be
  returned to warehouse.)
- **Storage**: cooked dishes live in the Tavern's storage (chest near the
  cooking bench). Hungry villagers prefer the Tavern over the Warehouse for
  food — they eat cooked dishes there, which restore more hunger than raw
  produce.
- **Tiers**: at Tavern lvl 1, cook makes the no-`KnowledgeRequired` recipes
  (Bread, all Kebabs, Salad_Berry, Salad_Mushroom, Popcorn, Cheese). T3
  recipes that require knowledge (Pies, Caesar) gate behind future content.
- **Hunger restore**: T1 (raw produce eaten directly) << T2 (cooked dish) <
  T3 (premium dish). Exact numbers TBD; see `VillagerData.HUNGER_*` constants.
