package com.lootfilters.util;

import com.lootfilters.LootFilter;
import com.lootfilters.LootFiltersConfig;
import com.lootfilters.FilterRule;
import com.lootfilters.model.TextAccent;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.lootfilters.util.TextUtil.quote;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static net.runelite.client.util.ColorUtil.colorToAlphaHexCode;

public class FilterUtil {
    private FilterUtil() {
    }

    /**
     * Wraps a user-defined loot filter with config defaults (highlight/hide, value tiers, etc.).
     */
    public static LootFilter withConfigRules(LootFilter filter, LootFiltersConfig config) {
        var withConfig = new ArrayList<FilterRule>();

        withConfig.add(FilterRule.highlight(config));
        withConfig.add(FilterRule.hide(config.hiddenItems()));

        withConfig.addAll(filter.getRules());

        withConfig = withConfig.stream()
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

        return new LootFilter(filter.getName(), filter.getDescription(), filter.getActivationArea(), withConfig);
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
                    .map(it -> "HIDE(" + quote(it) + ")")
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
