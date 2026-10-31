# Changelog

## [1.5.1] - 10/31/2026

### Added

- **Land claims** With QuestLines Claims installed, only the claim owner and players the claim trusts with containers can add to or take from an aquarium in a claim. Turn it off with `ClaimsIntegration` in the config.

### Fixed

- **World crash** Fixed a crash that could stop the world with a "Store is currently processing!" error.
- **Fish shadows** Fish shadows are no longer saved with the world.

## [1.5.0] - 10/12/2026

### Changed

- **Hytale Update 7** Cozy Tales: Fishing now runs on Hytale Update 7 (0.7.0).

## [1.4.0] - 9/21/2026

### Added

- **Special fishing spots** Sparkling fishing spots with four to six catches, rarer fish, and better chances of treasure and messages in bottles. Spots can appear without holding a rod.

### Changed

- **Boat water clearance** Raised boat hulls to improve clearance above the water.

### Fixed

- **Stray Line Entities** Fixed issue where the fishing line didn't get fully cleaned up.
- **Texture sizes** Corrected texture sizes that caused client warnings.
- **Item templates** Fixed missing model warnings for bobber and monster catch templates.
- **Fisherman shop quest** Reed gives a fishing shop plot token matching the town building style instead of a blueprint.

### Fixed
## [1.3.0] - 8/27/2026

### Added

- **Stormwind Fishmonger** — Stormwind-style variant of the Fisherman's Shop for towns using the Stormwind building set.
- **Boat speed** — Server owners can change how fast fishing boats move in `FishingModConfig`.
- **Coral and seaweed recipes** — Coral and seaweed crafting at the Fishing Bench can be turned off in `FishingModConfig`.
- **Reed Castwell calendar** — Birthday on Summer 20, Wintertide gift lines, festival greetings for every Aetherhaven holiday, and a Wintertide present.

### Changed

- **Hytale Update 6** — Targets server `>=0.6.0-pre.0 <0.7.0` and rebuilds against the new protocol.
- **Chunk and block APIs** — Boat place/remove, aquarium block entities, and fish spawn environment reads use section-based access instead of deprecated `World`/`WorldChunk` helpers.
- **NPC boat mount** — Updated to the Update 6 `ExecutionSupport` NPC action API.
- **Asset registration** — Custom fish and aquarium asset stores register through the plugin asset registry so they unload cleanly on plugin reload.

### Fixed

- **Aquarium decorations** — Block-item decorations sit on the tank floor again after Update 6 moved BlockEntity visual centers down by half a block.
- **Bobber equip page** — Clicking a bobber slot after the fishing rod is moved or dropped no longer crashes the world tick.

## [1.2.0] - 7/27/2026

### Added

- **Fish Finder location details** — See zone, environment, depth, water type, time, and weather for the water you are scanning.
- **Rarity frames in the journal and Fish Finder** — Fish entries now show colored rarity backgrounds that match their tier.

### Fixed

- **Catch rarity** — Caught fish now show the correct rarity on the item.
- **Underground fishing** — Cave and underground water is detected correctly, including in large caverns.
- **Time-of-day spawns** — Night and day fish now match the clock shown in-game (for example, night fish appear at 18:00, not later).
- **Fish Finder crash** — Fixed a crash when opening the Fish Finder in some locations.
- **Frostgill shadow** — Frostgill now uses a properly sized shadow for a large fish.

### Changed

- **Coastal ocean spawns** — Zone 2 shores have fewer overlapping species, making rare catches like Man o' War easier to find.
- **Snapjaw** — Can appear in any underground Zone 3 water at night, not just ponds.

## [1.1.0] - 7/22/2026

### Added

- **Mod fishable fluids** — Other mods can register custom fluids via `CozyTalesFishing/Config/FishableFluids` JSON (or `CozyTalesFishingIntegration.registerFishableFluid`). Species in any pack can use a `Fluid` spawn rule. See [docs/mod-fish-and-fluids.md](docs/mod-fish-and-fluids.md).
- **MMO Skill Tree integration** — Journal fish award FISHING XP via `MMOSkillTreeAPI` (optional dep `Ziggfreed:MMOSkillTree`, plugin classloader bridge). XP scales by rarity plus first-catch bonus. **Fishing Luck** (FISHING-scoped `LUCK_PCT`) adds treasure shadow spawn chance and can grant extra journal fish on the same catch; tunable in `FishingModConfig`.
- **Fishing rod salvage** — All five Cozy fishing rods can be salvaged at the Salvage Bench (outputs match vanilla metal pickaxe salvage per tier; wooden rod matches crude shortbow-style salvage).

### Fixed

- **Aquarium sizing** — Fish may be placed in any aquarium at or above their minimum `AquariumSize` tier (small → medium → grand). Oversized fish still require a larger tank.
- **Aetherhaven Reed quests** — Shop and house quest objectives use proper progression kinds (`plot_blueprint_*`, `construction_built`, `assign_house_resident`, `dialogue_turn_in`) so dialogue turn-in completes after the shop is built or Reed is assigned to a house.

## [1.0.1] - 7/11/2026
### Changed

- **Reed Castwell dialogue** — Reed is now written as a castaway who washed ashore after losing his boat. First-meeting intro, greetings, quest lines, tips, and gifts reflect that story; quest and guide text match.

### Fixed

- **Fish Processor crafting tier** — The Fish Processor recipe at the Fishing Bench now requires bench tier 2 instead of tier 3.
