package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ItemQuantityRule extends ComparatorRule {
    public ItemQuantityRule(int value, Comparator cmp) {
        super(value, cmp);
    }

    @Override
    public int getLhs(LootFiltersPlugin plugin, PluginTileItem item) {
        return item.getQuantity();
    }
}
