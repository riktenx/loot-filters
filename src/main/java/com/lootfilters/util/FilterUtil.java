package com.lootfilters.util;

import com.lootfilters.LootFilter;
import com.lootfilters.LootFiltersConfig;
import com.lootfilters.MatcherConfig;
import com.lootfilters.rule.TextAccent;
import com.lootfilters.rule.ValueTier;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.lootfilters.util.TextUtil.quote;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static net.runelite.client.util.ColorUtil.colorToAlphaHexCode;

public class FilterUtil {
    private FilterUtil() {}

    /**
     * Wraps a user-defined loot filter with config defaults (highlight/hide, value tiers, etc.).
     */
    public static LootFilter withConfigMatchers(LootFilter filter, LootFiltersConfig config) {
        var matchersWithConfig = new ArrayList<MatcherConfig>();
        matchersWithConfig.add(MatcherConfig.showUnmatched(config.showUnmatchedItems()));

        matchersWithConfig.add(MatcherConfig.ownershipFilter(config.ownershipFilter()));
        matchersWithConfig.add(MatcherConfig.itemSpawnFilter(config.itemSpawnFilter()));

        matchersWithConfig.add(MatcherConfig.highlight(
                config.highlightedItems(), config.highlightColor(), config.highlightLootbeam(), config.highlightNotify()));
        matchersWithConfig.add(MatcherConfig.hide(config.hiddenItems()));

        if (config.itemValueRulesEnabled()) {
            var valueType = config.valueType();
            matchersWithConfig.add(MatcherConfig.valueTier(
                    config.enableInsaneItemValueTier(), config.insaneValue(), config.insaneValueColor(),
                    config.lootbeamTier().ordinal() >= ValueTier.INSANE.ordinal(),
                    config.notifyTier().ordinal() >= ValueTier.INSANE.ordinal(), valueType));
            matchersWithConfig.add(MatcherConfig.valueTier(
                    config.enableHighItemValueTier(), config.highValue(), config.highValueColor(),
                    config.lootbeamTier().ordinal() >= ValueTier.HIGH.ordinal(),
                    config.notifyTier().ordinal() >= ValueTier.HIGH.ordinal(), valueType));
            matchersWithConfig.add(MatcherConfig.valueTier(
                    config.enableMediumItemValueTier(), config.mediumValue(), config.mediumValueColor(),
                    config.lootbeamTier().ordinal() >= ValueTier.MEDIUM.ordinal(),
                    config.notifyTier().ordinal() >= ValueTier.MEDIUM.ordinal(), valueType));
            matchersWithConfig.add(MatcherConfig.valueTier(
                    config.enableLowItemValueTier(), config.lowValue(), config.lowValueColor(),
                    config.lootbeamTier().ordinal() >= ValueTier.LOW.ordinal(),
                    config.notifyTier().ordinal() >= ValueTier.LOW.ordinal(), valueType));
            matchersWithConfig.add(MatcherConfig.hiddenTier(config.hideTierEnabled(), config.hideTierValue(), config.hideTierShowUntradeable(), valueType));
        }

        matchersWithConfig.addAll(filter.getMatchers());

        matchersWithConfig = matchersWithConfig.stream()
                .map(it -> it.withDisplay(builder -> {
                    if (config.alwaysShowValue()) {
                        builder.showValue(true);
                    }
                    if (config.alwaysShowDespawn()) {
                        builder.showDespawn(true);
                    }
                    if (config.textAccent().ordinal() > TextAccent.USE_FILTER.ordinal()) {
                        builder.textAccent(config.textAccent());
                    }
                    if (config.highlightTiles()) {
                        builder.highlightTile(true);
                    }
                }))
                .collect(Collectors.toCollection(ArrayList::new));

        return new LootFilter(filter.getName(), filter.getDescription(), filter.getActivationArea(), matchersWithConfig);
    }

    /**
     * Captures the current config-based item matchers, exporting them to their own filter.
     */
    public static String configToFilterSource(LootFiltersConfig config, String name, String tutorialText) {
        var defines = "#define HCOLOR " + quote(colorToAlphaHexCode(config.highlightColor()));
        var meta = "meta {\n  name = " + quote(name) + ";\n}\n\n";
        var highlights = "";
        if (!config.highlightedItems().isBlank()) {
            highlights = stream(config.highlightedItems().split(","))
                    .map(it -> "HIGHLIGHT(" + quote(it) + ", HCOLOR)")
                    .collect(joining("\n"));
        }
        var hides = "";

        if (!config.hiddenItems().isBlank()) {
            hides = stream(config.hiddenItems().split(","))
                    .map(it -> "HIDE("+ quote(it) + ")")
                    .collect(joining("\n"));
        }

        return String.join("\n",
                defines,
                meta,
                tutorialText,
                highlights,
                hides);
    }
}
