package com.lootfilters.ast.leaf;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.ast.LeafCondition;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString
public class ItemNotedCondition extends LeafCondition {
    private final boolean target;

    public ItemNotedCondition(boolean target) {
        this.target = target;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        return item.isNoted() == target;
    }
}
