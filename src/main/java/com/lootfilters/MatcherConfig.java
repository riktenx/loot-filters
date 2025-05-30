package com.lootfilters;

import com.lootfilters.model.NamedQuantity;
import com.lootfilters.model.PluginTileItem;
import com.lootfilters.model.SoundProvider;
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
                .textColor(java.awt.Color.WHITE)
                .build();
        return new MatcherConfig(rule, display, false);
    }

    public static MatcherConfig highlight(LootFiltersConfig config) {
        var rawNames = config.highlightedItems();
        var rule = new OrRule(
                Arrays.stream(rawNames.split(","))
                        .map(NamedQuantity::fromString)
                        .map(it -> new AndRule(new ItemNameRule(it.getName()), new ItemQuantityRule(it.getQuantity(), it.getComparator())))
                        .collect(Collectors.toList())
        );

        SoundProvider sound = null;
        var configSound = config.highlightSound();
        try {
            var soundId = Integer.parseInt(configSound);
            sound = new SoundProvider.SoundEffect(soundId);
        } catch (NumberFormatException e) {
            if (!configSound.isBlank()) {
                sound = new SoundProvider.File(configSound);
            }
        }
        var display = DisplayConfig.builder()
                .textColor(config.highlightColor())
                .showLootbeam(config.highlightLootbeam())
                .notify(config.highlightNotify())
                .backgroundColor(config.higlightBackgroundColor())
                .borderColor(config.highlightBorderColor())
                .lootbeamColor(config.highlightLootbeamColor())
                .menuTextColor(config.highlightMenuTextColor())
                .menuSort(config.highlightMenuSort())
                .sound(sound)
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
