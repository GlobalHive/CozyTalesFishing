# Cozy Tales: Fishing

A relaxed fishing mod for Hytale, part of the Cozy Tales series by Hexvane.

Watch for fish shadows in the water, cast your line, and reel in the catch. Explore ponds, rivers, oceans, and even lava for dozens of species, plus the odd bit of trash or treasure.

## Features

- **Fish shadows** appear in bodies of water. Cast near one, wait for the fish to take interest, then fight it in on the line.
- **Fishing rods** in several tiers, from wooden up through adamantite. Stronger rods handle tougher fights.
- **Bobbers** you equip on your rod. Spinners draw fish from farther away, trap bobbers help during the fight, and others favor quality catches, treasure, or trash.
- **Fishing tools**, including a Fish Finder to scan what can bite nearby, and a Fishing Journal to track discoveries, personal bests, and server leaderboards.
- **Fishing Bench** for crafting rods, bobbers, aquariums, and other gear. Upgrade the bench to unlock more recipes.
- **Fish Processor** to turn catches into raw fish meat and scales used for crafting.
- **Aquariums** in small, medium, and grand sizes. Place fish and decorations inside to show off your collection.
- **Fishing boat** for reaching open water.
- **Trash and treasure** while fishing, including sunken chests and messages in bottles that can hint at new species.

## Getting started

1. Craft or obtain a **Fishing Bench**, then craft a wooden fishing rod.
2. Find a body of water and look for **fish shadows** moving beneath the surface.
3. Hold primary to charge your cast, then release to throw the bobber near a shadow.
4. When the fish bites, hold primary to reel it in. Use secondary to recall your line early.
5. Process fish at the **Fish Processor** for meat and scales, then use the bench to craft better rods, bobbers, and aquariums.
6. Open the **Fishing Journal** to browse species you have found and check where they live.

Tip: Use the **Fish Finder** on water or lava to see which fish can be caught there right now. Equip bobbers on a rod with Use (F).

## Optional: Aetherhaven

This mod works on its own. If you also run **Aetherhaven**, you get extra content:

- A fisherman NPC with dialogue, tips, and related town content
- A fishing shop you can help establish
- Shop stock and prices for fishing gear
- Extra rewards from sunken treasure, such as gold coins and the occasional piece of jewelry

Aetherhaven is not required. Install it alongside Cozy Tales: Fishing only if you want that integration.

## Optional: QuestLines Claims

If you run **QuestLines Claims**, aquariums inside a claim follow the claim. The claim owner and players the claim trusts with containers can add to and take from them; everyone else is refused. Set `ClaimsIntegration` to `false` in `FishingModConfig.json` to turn this off.


### Special fishing spots

Sparkling schools can appear in loaded water near any player, including players without a rod equipped. Each spot spawns **4–6 fish total**, shared by everyone. Caught or escaped fish are not replaced. Wandering spot fish stay near the sparkles and do not despawn just because nobody is holding a rod.

Schools use the normal biome, region, time, and weather eligibility rules. Relative fish weights are multiplied by 0.35 / 1 / 3 / 5 / 8 for Common through Legendary. Treasure rolls (including messages in bottles) use the larger of 15% or three times the configured normal treasure chance, capped at 100%; ordinary trash and monster rolls are skipped.

A spot disappears when its supply is caught or lost, after ten minutes, when its water is removed, or after 45 seconds with no player within 80 blocks. Its remaining fish are removed too, including hooked fish; active fights are released. Spots are transient and do not survive world unload/restart.

`FishingModConfig.json` settings (defaults also apply to existing configs missing these fields):

| Setting | Default | Meaning |
| --- | --- | --- |
| `SpecialSpotsEnabled` | `true` | Enable natural special spots. |
| `SpecialSpotSpawnChance` | `0.12` | Chance per eligible player per check. |
| `SpecialSpotCheckIntervalSeconds` | `30` | Seconds between checks; minimum 5. |
| `SpecialSpotLifetimeSeconds` | `600` | Maximum spot lifetime; minimum 30. |

For frequent test spawns, set `SpecialSpotSpawnChance` to `1.0` and `SpecialSpotCheckIntervalSeconds` to `5.0`. This attempts a spot every five seconds when there is suitable water and no nearby active spot. Use `/cozyfish reloadconfig` or restart the server to apply changes. The normal defaults remain 0.12 and 30 seconds.

Candidates are 12–40 blocks from a player, require a water patch at least two blocks deep, and stay at least 64 blocks from other spots. At most eight spots can be active per world. Client playtesting should check sparkle visibility, catching the final fish, leaving/revisiting a spot, two players fishing together, and expiry during a fight.

### Boat hull clearance

The boat uses mod-scoped variants of the game's boat model. The mounted hull is raised 27 model units at scale 2, with a 1.5 block mount anchor. The parked hull retains its two model unit clearance adjustment. The mounted floor now sits 0.1875 blocks higher relative to the rider than the earlier 16 unit hull with its 1.0 block anchor. This follows the raised-model approach found in Boats 2.0.2; no water blocks are removed. Riding, turning, parking, and dismounting need a client visual check because server builds cannot validate water rendering or rider alignment.

### Fishing line beam renderer (Update 7)

`FishingModConfig.json` supports `FishingLineBeamEnabled` (default `false`) and
`FishingLineBeamAsset` (default `"Rope"`, a built-in game Beam asset ID, without `.json`).
Set the toggle to `true` to draw only a thin native beam, or `false` to draw only the
original segmented line. `/cozyfish reloadconfig` also switches active casts on their
next visual update. Unknown/empty beam IDs log a warning and draw no line in beam
mode; they never enable the segmented renderer as a fallback.

The beam is owned by the bobber and targets the player's animated `R-Attachment`.
It remains connected as entities move; movement does not replace the beam.
`FishingLineBeamOffsetX`, `FishingLineBeamOffsetY`, and `FishingLineBeamOffsetZ`
set the target offset in attachment-local block units. They default to X=0.0,
Y=1.75, Z=0.05; to adjust them in-game, edit the config and run
`/cozyfish reloadconfig`. Both camera views share the same offset.
`FishingLineBeamWidth` defaults to `0.10`. `FishingLineBeamTargetNode` defaults to
`"R-Attachment"`; legacy empty or `"Tip"` values automatically resolve to that node.
Settings changes replace only this line's beam. Bobber removal removes the beam.
This renders a straight line; the existing fishing simulation remains unchanged.
Check attachment alignment in first/third person, while reeling, and while mounted
in a client; server builds cannot validate rendered attachment positions.
