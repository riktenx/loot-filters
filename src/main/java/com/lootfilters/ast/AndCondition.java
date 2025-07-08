package com.lootfilters.ast;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@ToString
public class AndCondition extends Condition {
    private final List<Condition> rules;

    public AndCondition(List<Condition> rules) {
        this.rules = rules;
    }

    public AndCondition(Condition left, Condition right) {
        this.rules = List.of(left, right);
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        return rules.stream().allMatch(it -> it.test(plugin, item));
    }
}
