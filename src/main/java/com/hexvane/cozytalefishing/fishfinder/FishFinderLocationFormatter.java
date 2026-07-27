package com.hexvane.cozytalefishing.fishfinder;

import com.hexvane.cozytalefishing.fish.FishSpeciesMetadataFormatter;
import com.hexvane.cozytalefishing.fish.FishShadowSpawnHelper;
import com.hexvane.cozytalefishing.fish.FishableFluidRegistry;
import com.hexvane.cozytalefishing.fish.FishingSpawnRegionContext;
import com.hexvane.cozytalefishing.fish.SpawnLocationDisplayNames;
import com.hexvane.cozytalefishing.fish.SpawnProbeService;
import com.hexvane.cozytalefishing.fish.WaterBodyClassifier;
import com.hexvane.cozytalefishing.fish.WaterBodyType;
import com.hexvane.cozytalefishing.fish.WeatherDisplayNames;
import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Formats the probed water column location for the Fish Finder UI. */
public final class FishFinderLocationFormatter {
    private static final String LINE_PREFIX = "server.cozytalefishing.fishfinder.location.line.";

    private FishFinderLocationFormatter() {}

    @Nonnull
    public static String format(@Nonnull World world, @Nonnull SpawnProbeService.PlayerProbeResult location) {
        StringBuilder text = new StringBuilder();
        appendLine(text, "zone", formatZone(world, location));
        appendLine(text, "environment", formatEnvironment(location.environmentIndex()));
        appendLine(text, "biome", formatBiome(world, location));
        appendLine(text, "depth", formatDepth(location));
        appendLine(text, "water", formatWater(location));
        appendOptionalLine(text, "region", formatRegion(location.regionContext()));
        appendOptionalLine(text, "time", formatTime(location));
        appendOptionalLine(text, "weather", formatWeather(location));
        return text.toString();
    }

    @Nonnull
    private static String formatZone(
        @Nonnull World world,
        @Nonnull SpawnProbeService.PlayerProbeResult location
    ) {
        FishingSpawnRegionContext regionContext = location.regionContext();
        if (regionContext != null && regionContext.getEffectiveZonePrefix() != null) {
            return SpawnLocationDisplayNames.formatZone(regionContext.getEffectiveZonePrefix());
        }
        String worldZone = FishShadowSpawnHelper.getWorldZonePrefix(world, location.blockX(), location.blockZ());
        return worldZone != null ? SpawnLocationDisplayNames.formatZone(worldZone) : "—";
    }

    @Nonnull
    private static String formatEnvironment(int environmentIndex) {
        Environment environment = Environment.getAssetMap().getAsset(environmentIndex);
        if (environment == null || environment.getId() == null) {
            return "—";
        }
        return SpawnLocationDisplayNames.formatEnvironmentId(environment.getId());
    }

    @Nonnull
    private static String formatBiome(
        @Nonnull World world,
        @Nonnull SpawnProbeService.PlayerProbeResult location
    ) {
        String biome = WaterBodyClassifier.getBiomeName(world, location.blockX(), location.blockZ());
        FishingSpawnRegionContext regionContext = location.regionContext();
        if (regionContext != null && regionContext.getEffectiveBiome() != null) {
            biome = regionContext.getEffectiveBiome();
        }
        return biome != null && !biome.isBlank()
            ? SpawnLocationDisplayNames.formatBiomeId(biome)
            : "—";
    }

    @Nonnull
    private static String formatDepth(@Nonnull SpawnProbeService.PlayerProbeResult location) {
        return FishSpeciesMetadataFormatter.formatUnderground(location.probeResult().underground());
    }

    @Nonnull
    private static String formatWater(@Nonnull SpawnProbeService.PlayerProbeResult location) {
        SpawnProbeService.ProbeResult probe = location.probeResult();
        String fluidId = probe.columnFluidAssetId();
        if (fluidId != null && !fluidId.isBlank()) {
            FishableFluidRegistry.Entry entry = FishableFluidRegistry.entryForFluidId(fluidId);
            if (entry != null && entry.journalHabitatKey() != null && !entry.journalHabitatKey().isBlank()) {
                String localized = lookupJournalHabitat(entry.journalHabitatKey());
                if (localized != null) {
                    return localized;
                }
            }
            if (entry != null && entry.habitatId() != null && !entry.habitatId().isBlank()) {
                return entry.habitatId();
            }
            return fluidId;
        }

        WaterBodyType spawnBody = probe.spawnWaterBody();
        String spawnLabel = FishSpeciesMetadataFormatter.formatWaterBodyType(spawnBody);
        if (location.classifiedWaterBody() != location.effectiveWaterBody()
            && location.effectiveWaterBody() != spawnBody) {
            return spawnLabel
                + " ("
                + FishSpeciesMetadataFormatter.formatWaterBodyType(location.classifiedWaterBody())
                + ")";
        }
        return spawnLabel;
    }

    @Nullable
    private static String formatRegion(@Nullable FishingSpawnRegionContext regionContext) {
        if (regionContext == null) {
            return null;
        }
        String regionId = regionContext.getRegion().getId();
        if (regionId == null || regionId.isBlank()) {
            return null;
        }
        return SpawnLocationDisplayNames.formatSpawnId(regionId);
    }

    @Nonnull
    private static String formatTime(@Nonnull SpawnProbeService.PlayerProbeResult location) {
        WorldTimeResource worldTime = location.probeResult().spawnConditions().worldTime();
        return String.format(Locale.US, "%d:00", worldTime.getCurrentHour());
    }

    @Nonnull
    private static String formatWeather(@Nonnull SpawnProbeService.PlayerProbeResult location) {
        int weatherIndex = location.probeResult().spawnConditions().weatherIndex();
        if (weatherIndex == AssetMapWithIndexes.NOT_FOUND) {
            return "—";
        }
        Weather weather = Weather.getAssetMap().getAsset(weatherIndex);
        if (weather == null || weather.getId() == null) {
            return "—";
        }
        return WeatherDisplayNames.formatWeatherId(weather.getId());
    }

    private static void appendLine(@Nonnull StringBuilder text, @Nonnull String key, @Nonnull String value) {
        if (!text.isEmpty()) {
            text.append('\n');
        }
        text.append(formatLine(key, value));
    }

    private static void appendOptionalLine(
        @Nonnull StringBuilder text,
        @Nonnull String key,
        @Nullable String value
    ) {
        if (value == null || value.isBlank()) {
            return;
        }
        appendLine(text, key, value);
    }

    @Nonnull
    private static String formatLine(@Nonnull String key, @Nonnull String value) {
        String template = lookup(LINE_PREFIX + key);
        if (template == null) {
            return key + ": " + value;
        }
        return template.replace("{value}", value);
    }

    @Nullable
    private static String lookup(@Nonnull String key) {
        I18nModule i18n = I18nModule.get();
        if (i18n == null) {
            return null;
        }
        String resolved = i18n.getMessage(I18nModule.DEFAULT_LANGUAGE, key);
        if (resolved == null || resolved.isBlank() || resolved.equals(key)) {
            return null;
        }
        return resolved;
    }

    @Nullable
    private static String lookupJournalHabitat(@Nonnull String journalHabitatKey) {
        String localized = lookup(journalHabitatKey);
        if (localized != null) {
            return localized;
        }
        if (!journalHabitatKey.startsWith("server.")) {
            return lookup("server." + journalHabitatKey);
        }
        return null;
    }
}
