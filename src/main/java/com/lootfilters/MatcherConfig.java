package com.lootfilters;

import com.lootfilters.model.NamedQuantity;
import com.lootfilters.model.PluginTileItem;
import com.lootfilters.rule.AndRule;
import com.lootfilters.rule.Comparator;
import com.lootfilters.rule.ItemNameRule;
import com.lootfilters.rule.ItemQuantityRule;
import com.lootfilters.rule.ItemTradeableRule;
import com.lootfilters.rule.ItemValueRule;
import com.lootfilters.rule.OrRule;
import com.lootfilters.rule.Rule;
import com.lootfilters.rule.ValueType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.runelite.api.TileItem;
import net.runelite.api.Varbits;

import java.awt.Color;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class MatcherConfig {
    private final Rule rule;
    private final DisplayConfig display;
    private final boolean isTerminal;

    public MatcherConfig(Rule rule, DisplayConfig display) {
        this.rule = rule;
        this.display = display;
        this.isTerminal = true;
    }

    public MatcherConfig withDisplay(Consumer<DisplayConfig.DisplayConfigBuilder> consumer) {
        var builder = display.toBuilder();
        consumer.accept(builder);
        return new MatcherConfig(rule, builder.build(), isTerminal);
    }

    public static MatcherConfig ownershipFilter(boolean enabled) {
        var rule = new Rule("") {
            @Override public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
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
            @Override public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
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
            @Override public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
                return enabled;
            }
        };
        var display = DisplayConfig.builder()
                .textColor(Color.WHITE)
                .build();
        return new MatcherConfig(rule, display, false);
    }

    public static MatcherConfig valueTier(boolean enabled, int value, Color color, boolean showLootbeam, boolean notify, ValueType valueType) {
        var inner = new ItemValueRule(value, Comparator.GT_EQ, valueType);
        var rule = new Rule("") {
            @Override public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
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

    public static MatcherConfig hiddenTier(boolean enabled, int value, boolean showUntradeable, ValueType valueType) {
        var valueRule = new ItemValueRule(value, Comparator.LT, valueType);
        var inner = showUntradeable
                ? new AndRule(valueRule, new ItemTradeableRule(true))
                : valueRule;
        var rule = new Rule("") {
            @Override public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
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
                        .map(NamedQuantity::fromString)
                        .map(it -> new AndRule(new ItemNameRule(it.getName()), new ItemQuantityRule(it.getQuantity(), it.getComparator())))
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
                        .map(NamedQuantity::fromString)
                        .map(it -> new AndRule(new ItemNameRule(it.getName()), new ItemQuantityRule(it.getQuantity(), it.getComparator())))
                        .collect(Collectors.toList())
        );
        var display = DisplayConfig.builder()
                .hidden(true)
                .build();
        return new MatcherConfig(rule, display);
    }
}
