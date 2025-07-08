package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class ConstRule extends LeafRule {
    private final boolean target;

    public ConstRule(boolean target) {
        this.target = target;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        return target;
    }
}
