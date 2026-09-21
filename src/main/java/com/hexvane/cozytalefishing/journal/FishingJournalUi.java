package com.hexvane.cozytalefishing.journal;

import com.hexvane.cozytalefishing.fish.FishRarity;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.asset.util.ColorParseUtil;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FishingJournalUi {
    private static final Value<String> DEFAULT_TEXT_TOOLTIP_STYLE = Value.ref("Common.ui", "DefaultTextTooltipStyle");
    /** Original nine-slice borders are preserved in the power-of-two @2x textures. */
    private static final int QUALITY_SLOT_BORDER = 24;
    /** Custom UI paths resolve from the Custom root, not common-asset UI/ prefixes. */
    private static final String QUALITY_SLOT_UI_ROOT = "CozyTalesFishing/ItemQualities/Slots/";

    private FishingJournalUi() {}

    public static void applyStaticLabels(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.set("#JournalTitle.TextSpans", Message.translation("server.cozytalefishing.journal.title"));
        commandBuilder.set("#FilterTitle.TextSpans", Message.translation("server.cozytalefishing.journal.filter_title"));
        commandBuilder.set("#GridTitle.TextSpans", Message.translation("server.cozytalefishing.journal.grid_title"));
        commandBuilder.set("#DetailTitle.TextSpans", Message.translation("server.cozytalefishing.journal.detail_title"));
        commandBuilder.set("#HabitatHeading.TextSpans", Message.translation("server.cozytalefishing.journal.habitat_heading"));
        commandBuilder.set("#ConditionsHeading.TextSpans", Message.translation("server.cozytalefishing.journal.conditions_heading"));
        commandBuilder.set("#RecordHeading.TextSpans", Message.translation("server.cozytalefishing.journal.record_heading"));
        commandBuilder.set("#CatchCountHeading.TextSpans", Message.translation("server.cozytalefishing.journal.catch_count_heading"));
        commandBuilder.set("#UndiscoveredHint.TextSpans", Message.translation("server.cozytalefishing.journal.undiscovered_hint"));
        commandBuilder.set("#HintedHint.TextSpans", Message.translation("server.cozytalefishing.journal.hinted_hint"));
        commandBuilder.set(
            "#LeaderboardColRank.TextSpans",
            Message.translation("server.cozytalefishing.journal.leaderboard.col.rank")
        );
        commandBuilder.set(
            "#LeaderboardColAngler.TextSpans",
            Message.translation("server.cozytalefishing.journal.leaderboard.col.angler")
        );
        commandBuilder.set(
            "#LeaderboardColScore.TextSpans",
            Message.translation("server.cozytalefishing.journal.leaderboard.col.score")
        );
    }

    public static boolean isKnownItemId(@Nullable String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        return Item.getAssetMap().getAsset(itemId.trim()) != null;
    }

    public static void setFishIcon(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull String assetImageSelector,
        @Nonnull String itemId
    ) {
        if (!isKnownItemId(itemId)) {
            return;
        }
        Item item = Item.getAssetMap().getAsset(itemId.trim());
        commandBuilder.set(assetImageSelector + ".AssetPath", ItemAssetImagePath.forItem(item, itemId));
    }

    public static void applyQualityBackground(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull String groupSelector,
        @Nonnull FishRarity rarity
    ) {
        commandBuilder.setObject(groupSelector + ".Background", qualitySlotBackground(qualityFor(rarity)));
    }

    public static void applyRarityLabel(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull String labelSelector,
        @Nonnull FishRarity rarity
    ) {
        ItemQuality quality = qualityFor(rarity);
        commandBuilder.set(labelSelector + ".TextSpans", rarityMessage(quality));
        commandBuilder.set(labelSelector + ".Style.TextColor", ColorParseUtil.colorToHexString(quality.getTextColor()));
    }

    @Nonnull
    private static ItemQuality qualityFor(@Nonnull FishRarity rarity) {
        ItemQuality quality = ItemQuality.getAssetMap().getAsset(rarity.name());
        return quality != null ? quality : ItemQuality.DEFAULT_ITEM_QUALITY;
    }

    @Nonnull
    private static Message rarityMessage(@Nonnull ItemQuality quality) {
        String localizationKey = quality.getLocalizationKey();
        if (localizationKey != null && !localizationKey.isBlank()) {
            return Message.translation(localizationKey);
        }
        return Message.raw(quality.getId());
    }

    @Nonnull
    private static PatchStyle qualitySlotBackground(@Nonnull ItemQuality quality) {
        return new PatchStyle()
            .setTexturePath(Value.of(qualitySlotUiTexturePath(quality.getSlotTexture())))
            .setBorder(Value.of(QUALITY_SLOT_BORDER));
    }

    @Nonnull
    private static String qualitySlotUiTexturePath(@Nullable String slotTexture) {
        if (slotTexture == null || slotTexture.isBlank()) {
            slotTexture = ItemQuality.DEFAULT_ITEM_QUALITY.getSlotTexture();
        }

        String normalized = slotTexture.trim().replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);

        // ItemQuality slot textures live outside Custom/; ship copies under CozyTalesFishing/ItemQualities/.
        if (normalized.startsWith("UI/ItemQualities/")) {
            fileName = normalized.substring("UI/ItemQualities/".length());
        } else if (normalized.startsWith("ItemQualities/")) {
            fileName = normalized.substring("ItemQualities/".length());
        }

        if (fileName.startsWith("Slots/")) {
            fileName = fileName.substring("Slots/".length());
        }

        // Engine resolves @2x variants automatically; paths must omit the suffix.
        if (fileName.contains("@2x.png")) {
            fileName = fileName.replace("@2x.png", ".png");
        }

        return QUALITY_SLOT_UI_ROOT + fileName;
    }

    public static void setGridCellTooltip(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull String buttonSelector,
        @Nonnull String displayName,
        @Nonnull JournalEntryState state
    ) {
        commandBuilder.set(buttonSelector + ".TextTooltipStyle", DEFAULT_TEXT_TOOLTIP_STYLE);
        switch (state) {
            case DISCOVERED -> commandBuilder.set(buttonSelector + ".TooltipTextSpans", Message.raw(displayName));
            case HINTED -> commandBuilder.set(
                buttonSelector + ".TooltipTextSpans",
                Message.translation("server.cozytalefishing.journal.hinted_tooltip")
            );
            case UNDISCOVERED -> commandBuilder.set(
                buttonSelector + ".TooltipTextSpans",
                Message.translation("server.cozytalefishing.journal.undiscovered_tooltip")
            );
        }
    }
}
