# Changelog

## [1.2.1] - Unreleased

### Added

- **Stormwind Fishmonger** — Stormwind-style variant of the Fisherman's Shop for towns using the Stormwind building set.

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
