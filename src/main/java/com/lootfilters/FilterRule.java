package com.lootfilters;

import com.lootfilters.model.NamedQuantity;
import com.lootfilters.model.SoundProvider;
import com.lootfilters.ast.AndCondition;
import com.lootfilters.ast.leaf.ItemNameCondition;
import com.lootfilters.ast.leaf.ItemQuantityCondition;
import com.lootfilters.ast.OrCondition;
import com.lootfilters.ast.Condition;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class FilterRule {
    private final Condition cond;
    private final DisplayConfig.Builder display;
    private final boolean isTerminal;
    private final int sourceLine;

    public FilterRule withDisplay(Consumer<DisplayConfig.Builder> consumer) {
        consumer.accept(display);
        return new FilterRule(cond, display, isTerminal, sourceLine);
    }

    public static FilterRule highlight(LootFiltersConfig config) {
        var rawNames = config.highlightedItems();
        var rule = new OrCondition(
                Arrays.stream(rawNames.split(","))
                        .map(NamedQuantity::fromString)
                        .map(it -> new AndCondition(new ItemNameCondition(it.getName()), new ItemQuantityCondition(it.getQuantity(), it.getComparator())))
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
                .sound(sound);
        return new FilterRule(rule, display, true, -3);
    }

    public static FilterRule hide(String rawNames) {
        var rule = new OrCondition(
                Arrays.stream(rawNames.split(","))
                        .map(NamedQuantity::fromString)
                        .map(it -> new AndCondition(new ItemNameCondition(it.getName()), new ItemQuantityCondition(it.getQuantity(), it.getComparator())))
                        .collect(Collectors.toList())
        );
        var display = DisplayConfig.builder()
                .hidden(true);
        return new FilterRule(rule, display, true, -4);
    }
}
