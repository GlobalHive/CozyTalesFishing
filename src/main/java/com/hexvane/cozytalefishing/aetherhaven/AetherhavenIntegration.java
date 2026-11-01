package com.hexvane.cozytalefishing.aetherhaven;

import com.hexvane.cozytalefishing.CozyTalesFishingPlugin;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;

/**
 * Soft Aetherhaven hooks. Resolved reflectively so standalone builds do not require Aetherhaven classes.
 */
public final class AetherhavenIntegration {
    /** Chance to also roll unidentified jewelry when opening sunken treasure with Aetherhaven present. */
    private static final double SUNKEN_JEWELRY_CHANCE = 0.08;

    private static Method pluginGetter;
    private static Method configGetter;
    private static Method jewelryRoll;
    private static String goldCoinItemId;
    private static boolean enabled;
    private static boolean loggedMissing;

    private AetherhavenIntegration() {}

    /** Resolves Aetherhaven on plugin setup; no-ops when the plugin is not loaded. */
    public static void setup() {
        enabled = false;
        try {
            Class<?> pluginClass = Class.forName("com.hexvane.aetherhaven.AetherhavenPlugin");
            pluginGetter = pluginClass.getMethod("get");
            configGetter = pluginClass.getMethod("getConfig");
            goldCoinItemId = (String) Class.forName("com.hexvane.aetherhaven.AetherhavenConstants")
                .getField("ITEM_GOLD_COIN").get(null);
            Class<?> lootClass = Class.forName("com.hexvane.aetherhaven.jewelry.JewelryChestLoot");
            Object ah = pluginGetter.invoke(null);
            if (ah == null) {
                logMissing("Aetherhaven not present; skipping soft integration.");
                return;
            }
            Object config = readConfig(ah);
            jewelryRoll = null;
            for (Method method : lootClass.getMethods()) {
                if (method.getName().equals("rollForChest") && Modifier.isStatic(method.getModifiers())
                    && method.getParameterCount() == 2
                    && method.getParameterTypes()[0].isInstance(ThreadLocalRandom.current())
                    && method.getParameterTypes()[1].isInstance(config)
                    && ItemStack.class.isAssignableFrom(method.getReturnType())) {
                    jewelryRoll = method;
                    break;
                }
            }
            if (jewelryRoll == null) throw new NoSuchMethodException("JewelryChestLoot.rollForChest");
            enabled = true;
            CozyTalesFishingPlugin plugin = CozyTalesFishingPlugin.get();
            if (plugin != null) {
                plugin.getLogger().atInfo().log("Aetherhaven soft integration enabled.");
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            logMissing("Aetherhaven classes unavailable; skipping soft integration.");
        }
    }

    private static Object readConfig(Object plugin) throws ReflectiveOperationException {
        Object config = configGetter.invoke(plugin);
        return config.getClass().getMethod("get").invoke(config);
    }

    private static void logMissing(@Nonnull String message) {
        if (loggedMissing) {
            return;
        }
        loggedMissing = true;
        CozyTalesFishingPlugin plugin = CozyTalesFishingPlugin.get();
        if (plugin != null) {
            plugin.getLogger().atInfo().log(message);
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Extra sunken treasure loot when Aetherhaven is loaded: gold coins often, jewelry rarely.
     * Returns an empty list when Aetherhaven is absent.
     */
    @Nonnull
    public static List<ItemStack> rollSunkenTreasureBonus() {
        if (!enabled) {
            return List.of();
        }
        try {
            Object ah = pluginGetter.invoke(null);
            if (ah == null) {
                return List.of();
            }
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            List<ItemStack> bonus = new ArrayList<>(2);
            int coins = 1 + rnd.nextInt(4);
            bonus.add(new ItemStack(goldCoinItemId, coins));
            if (rnd.nextDouble() < SUNKEN_JEWELRY_CHANCE) {
                ItemStack jewelry = (ItemStack) jewelryRoll.invoke(null, rnd, readConfig(ah));
                if (jewelry != null && !jewelry.isEmpty()) {
                    bonus.add(jewelry);
                }
            }
            return bonus;
        } catch (ReflectiveOperationException | LinkageError e) {
            enabled = false;
            logMissing("Aetherhaven integration unavailable; skipping treasure bonuses: " + e.getClass().getSimpleName());
            return List.of();
        }
    }
}
