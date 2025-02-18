package com.lootfilters;

import com.lootfilters.rule.AndRule;
import com.lootfilters.rule.Comparator;
import com.lootfilters.rule.ItemNameRule;
import com.lootfilters.rule.ItemTradeableRule;
import com.lootfilters.rule.ItemValueRule;
import com.lootfilters.rule.OrRule;
import com.lootfilters.rule.Rule;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.runelite.api.TileItem;
import net.runelite.api.Varbits;

import java.awt.Color;
import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class MatcherConfig {
    private final Rule rule;
    private final DisplayConfig display;
    private final boolean isTerminal;

    public MatcherConfig(Rule rule, DisplayConfig display) {
        this.rule = rule;
        this.display = display;
        this.isTerminal = true;
    }

    public static MatcherConfig ownershipFilter(boolean enabled) {
        var rule = new Rule("") {
            @Override public boolean test(LootFiltersPlugin plugin, TileItem item) {
                var accountType = plugin.getClient().getVarbitValue(Varbits.ACCOUNT_TYPE);
                return enabled && accountType != 0 && item.getOwnership() == TileItem.OWNERSHIP_OTHER;
            }
        };
        var display = DisplayConfig.builder()
                .hidden(true)
                .build();
        return new MatcherConfig(rule, display);
    }

    public static MatcherConfig itemSpawnFilter(boolean enabled) {
        var rule = new Rule("") {
            @Override public boolean test(LootFiltersPlugin plugin, TileItem item) {
                return enabled && item.getOwnership() == TileItem.OWNERSHIP_NONE;
            }
        };
        var display = DisplayConfig.builder()
                .hidden(true)
                .build();
        return new MatcherConfig(rule, display);
    }

    public static MatcherConfig showUnmatched(boolean enabled) {
        var rule = new Rule("") {
            @Override public boolean test(LootFiltersPlugin plugin, TileItem item) {
                return enabled;
            }
        };
        var display = DisplayConfig.builder()
                .textColor(Color.WHITE)
                .build();
        return new MatcherConfig(rule, display, false);
    }

    public static MatcherConfig valueTier(boolean enabled, int value, Color color, boolean showLootbeam, boolean notify) {
        var inner = new ItemValueRule(value, Comparator.GT_EQ);
        var rule = new Rule("") {
            @Override public boolean test(LootFiltersPlugin plugin, TileItem item) {
                return enabled && inner.test(plugin, item);
            }
        };
        var display = DisplayConfig.builder()
                .textColor(color)
                .showLootbeam(showLootbeam)
                .notify(notify)
                .build();
        return new MatcherConfig(rule, display);
    }

    public static MatcherConfig hiddenTier(boolean enabled, int value, boolean showUntradeable) {
        var valueRule = new ItemValueRule(value, Comparator.LT);
        var inner = showUntradeable
                ? new AndRule(valueRule, new ItemTradeableRule(true))
                : valueRule;
        var rule = new Rule("") {
            @Override public boolean test(LootFiltersPlugin plugin, TileItem item) {
                return enabled && inner.test(plugin, item);
            }
        };
        var display = DisplayConfig.builder()
                .hidden(true)
                .build();
        return new MatcherConfig(rule, display);
    }

    public static MatcherConfig highlight(String rawNames, Color color, boolean showLootbeam, boolean notify) {
        var rule = new OrRule(
                Arrays.stream(rawNames.split(","))
                        .map(ItemNameRule::new)
                        .collect(Collectors.toList())
        );
        var display = DisplayConfig.builder()
                .textColor(color)
                .showLootbeam(showLootbeam)
                .notify(notify)
                .build();
        return new MatcherConfig(rule, display);
    }

    public static MatcherConfig hide(String rawNames) {
        var rule = new OrRule(
                Arrays.stream(rawNames.split(","))
                        .map(ItemNameRule::new)
                        .collect(Collectors.toList())
        );
        var display = DisplayConfig.builder()
                .hidden(true)
                .build();
        return new MatcherConfig(rule, display);
    }
}
