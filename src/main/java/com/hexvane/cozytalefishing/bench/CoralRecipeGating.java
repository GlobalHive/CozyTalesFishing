package com.hexvane.cozytalefishing.bench;

import com.hexvane.cozytalefishing.CozyTalesFishingPlugin;
import com.hexvane.cozytalefishing.fish.FishingModConfig;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Removes or restores this mod's coral and seaweed Fishing Bench recipes based on {@code EnableCoralRecipes}.
 */
public final class CoralRecipeGating {
    static final String CORAL_ID_PREFIX = "CozyFishing_Bench_17_Deco_Plant_Coral_";
    static final String SEAWEED_ID_PREFIX = "CozyFishing_Bench_17_Deco_Plant_Seaweed_";

    private static final Map<String, CraftingRecipe> CACHED = new LinkedHashMap<>();
    private static boolean applying;

    private CoralRecipeGating() {}

    static boolean isGatedRecipeId(@Nonnull String id) {
        return id.startsWith(CORAL_ID_PREFIX) || id.startsWith(SEAWEED_ID_PREFIX);
    }

    public static void onRecipesLoaded(
        @Nonnull LoadedAssetsEvent<String, CraftingRecipe, DefaultAssetMap<String, CraftingRecipe>> event
    ) {
        for (Map.Entry<String, CraftingRecipe> entry : event.getLoadedAssets().entrySet()) {
            if (isGatedRecipeId(entry.getKey())) {
                CACHED.put(entry.getKey(), entry.getValue());
            }
        }
        apply();
    }

    public static void apply() {
        if (applying) {
            return;
        }
        applying = true;
        try {
            cacheFromStore();
            if (FishingModConfig.get().isEnableCoralRecipes()) {
                restoreCachedRecipes();
            } else {
                removeCachedRecipes();
            }
        } finally {
            applying = false;
        }
    }

    private static void cacheFromStore() {
        for (Map.Entry<String, CraftingRecipe> entry : CraftingRecipe.getAssetMap().getAssetMap().entrySet()) {
            if (isGatedRecipeId(entry.getKey())) {
                CACHED.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void removeCachedRecipes() {
        if (CACHED.isEmpty()) {
            return;
        }
        List<String> ids = new ArrayList<>(CACHED.keySet());
        var removed = CraftingRecipe.getAssetStore().removeAssets(ids);
        if (!removed.isEmpty()) {
            var plugin = CozyTalesFishingPlugin.get();
            if (plugin != null) {
                plugin.getLogger().atInfo().log(
                    "Removed %d coral and seaweed Fishing Bench recipes (EnableCoralRecipes=false).",
                    removed.size()
                );
            }
        }
    }

    private static void restoreCachedRecipes() {
        DefaultAssetMap<String, CraftingRecipe> map = CraftingRecipe.getAssetMap();
        List<CraftingRecipe> missing = new ArrayList<>();
        for (CraftingRecipe recipe : CACHED.values()) {
            if (map.getAsset(recipe.getId()) == null) {
                missing.add(recipe);
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        CraftingRecipe.getAssetStore().loadAssets(DefaultAssetMap.DEFAULT_PACK_KEY, missing);
        var plugin = CozyTalesFishingPlugin.get();
        if (plugin != null) {
            plugin.getLogger().atInfo().log(
                "Restored %d coral and seaweed Fishing Bench recipes (EnableCoralRecipes=true).",
                missing.size()
            );
        }
    }
}
