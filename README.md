<p align="center">
  <img src="logo.jpg" alt="Stayed logo" width="256"/>
</p>

<h1 align="center">Stayed</h1>

<p align="center">A Hytale mod.</p>

---

## Concept

You arrive in an untamed world. Near spawn you find an old elven sage — Aelin — who has spent his life studying the architecture and cultures of every race in Orbis. He is frail, cannot fight or mine, but he knows how to build. Together you found a village.

Villagers are not tools. They are survivors — refugees who lost their homes to The Shattering. They have names, professions, and daily lives. They go to work in the morning, eat lunch at the warehouse, return home at night, and open the door themselves on the way out.

## How to play

**1. Find Aelin** near spawn and press F to talk to him. Ask about starting a settlement — he will give you a Founding Stone.

**2. Place the Founding Stone** on flat ground. A ghost preview of the Town Hall appears. If you want a different location, break the stone to cancel. When you're happy with the placement, press F on the stone and confirm.

> Place the stone 1 block above the ground — the building floor sits one block up.

**3. Deposit resources** into the stone's container via the GUI, then press "Start Construction". Aelin walks to the site and builds block by block, consuming materials as he goes.

> Use `/hb fastbuild` or `/hb instabuild` to speed up construction during testing.

**4. Rescue survivors.** After the Town Hall is built, talk to Aelin — *"We need settlers"*. Follow the quest marker to find a trapped villager, press F to invite them, then lead them back to the village.

**5. Build more.** Talk to Aelin → *"I want to build something"*. Recommended order: House → Farm → Warehouse → Sawmill → Mine. Each building unlocks new gameplay:

- **House** — villager moves in, starts following a day schedule
- **Farm / Sawmill / Mine** — assigns a worker, produces resources automatically into Warehouse chests
- **Warehouse** — villagers take lunch and dinner breaks to eat here

**6. Watch your village live.** Press F on any villager to see their stats. Unhappy villagers (no house, no food) are less productive. Villagers greet each other, open doors, and wander around their workplaces.

## Features

- Anchor block → ghost preview → block-by-block construction with resource consumption
- Elf sage with custom appearance, idle behavior, and branching dialogue
- Villager daily schedule: work, eat, rest — with door open/close animations
- Random villager appearance from archetype pools (hair, skin tone, clothing)
- Multi-building progression: Town Hall, Houses, Farm, Warehouse, Sawmill, Mine, Guard Tower
- Village HUD with live resource counts
- Full building rotation based on player facing direction at placement
- NPC persistence across server restarts

## Building from source

Requires Java 25 (OpenJDK 25.0.2 or JetBrains Runtime 25).

```bash
./gradlew build        # compile check
./gradlew devServer    # start local dev server
```

On Windows use `gradlew.bat`.

## Project structure

```
src/main/java/dev/hearthbound/
  HearthboundPlugin.java      — plugin entry point
  village/                    — data model (VillageData, VillagerData, BuildingRecord)
  building/                   — construction pipeline (ghost → site clear → block-by-block)
  npc/                        — NPC management, appearance, schedules, persistence
  events/                     — event handlers (anchor placement, F-key, chunk load)
  ui/                         — UI pages and HUD
  quest/                      — rescue quest
  commands/                   — /hb debug commands

src/main/resources/
  Common/UI/Custom/*.ui       — UI layout files
  Server/Prefabs/*.prefab.json — building blueprints
  Server/NPC/Roles/*.json     — NPC behavior trees
  Server/Item/                — custom block and interaction definitions
```

## What's coming

- Second rescue quest
- Guard Tower with patrol behavior
- Tavern — villagers gather to eat together
- Brickyard — clay and brick production
- Market — recruit villagers without questing
- Building leveling (prefabs up to lvl 3 already exist)

## License

MIT — see [LICENSE](LICENSE).
