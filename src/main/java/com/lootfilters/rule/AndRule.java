package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.runelite.api.TileItem;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@ToString
public class AndRule extends Rule {
    private final List<Rule> rules;

    public AndRule(List<Rule> rules) {
        super("and");
        this.rules = rules;
    }

    public AndRule(Rule left, Rule right) {
        super("and");
        this.rules = List.of(left, right);
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, TileItem item) {
        return rules.stream().allMatch(it -> it.test(plugin, item));
    }
}
