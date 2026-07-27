package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Builds fish item stacks whose vanilla quality tier matches species rarity. */
public final class FishItemStackFactory {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String GENERATED_STATE_ID_PREFIX = "*";

    private FishItemStackFactory() {}

    @Nonnull
    public static ItemStack forSpecies(@Nonnull FishSpeciesAsset species) {
        return forItemId(species.getItemId(), species.getRarity());
    }

    @Nonnull
    public static ItemStack forItemId(@Nonnull String itemId) {
        FishSpeciesAsset species = FishSpeciesRegistry.getSpeciesByItemId(itemId);
        if (species != null) {
            return forSpecies(species);
        }
        return new ItemStack(itemId, 1);
    }

    @Nonnull
    public static ItemStack forItemId(@Nonnull String itemId, @Nonnull FishRarity rarity) {
        ItemStack stack = new ItemStack(itemId, 1);
        Item item = stack.getItem();
        if (item == null) {
            return stack;
        }

        String targetQuality = rarity.name();
        String baseQuality = qualityId(item);
        if (baseQuality != null && baseQuality.equalsIgnoreCase(targetQuality)) {
            return stack;
        }

        if (item.getItemIdForState(targetQuality) != null) {
            return stack.withState(targetQuality);
        }

        String generatedStateId = GENERATED_STATE_ID_PREFIX + itemId + "_State_" + targetQuality;
        if (Item.getAssetMap().getAsset(generatedStateId) != null) {
            stack = new ItemStack(generatedStateId, 1);
        }

        warnIfQualityMismatch(itemId, targetQuality, stack);
        return stack;
    }

    private static void warnIfQualityMismatch(
        @Nonnull String itemId,
        @Nonnull String targetQuality,
        @Nonnull ItemStack stack
    ) {
        Item resolvedItem = stack.getItem();
        if (resolvedItem == null) {
            return;
        }
        String resolvedQuality = qualityId(resolvedItem);
        if (resolvedQuality != null && !resolvedQuality.equalsIgnoreCase(targetQuality)) {
            LOGGER.atWarning().log(
                "Fish item %s resolved to quality %s but species rarity is %s",
                itemId,
                resolvedQuality,
                targetQuality
            );
        }
    }

    @Nullable
    private static String qualityId(@Nonnull Item item) {
        ItemQuality quality = ItemQuality.getAssetMap().getAsset(item.getQualityIndex());
        return quality != null ? quality.getId() : null;
    }
}
