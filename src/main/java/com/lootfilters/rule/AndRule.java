package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@ToString
public class AndRule extends Rule {
    private final List<Rule> rules;

    public AndRule(List<Rule> rules) {
        this.rules = rules;
    }

    public AndRule(Rule left, Rule right) {
        this.rules = List.of(left, right);
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        return rules.stream().allMatch(it -> it.test(plugin, item));
    }
}
