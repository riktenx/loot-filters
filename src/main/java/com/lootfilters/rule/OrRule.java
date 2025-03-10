package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@ToString
public class OrRule extends Rule {
    private final List<Rule> rules;

    public OrRule(List<Rule> rules) {
        super("or");
        this.rules = rules;
    }

    public OrRule(Rule left, Rule right) {
        super("or");
        this.rules = List.of(left, right);
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        return rules.stream().anyMatch(it -> it.test(plugin, item));
    }
}
