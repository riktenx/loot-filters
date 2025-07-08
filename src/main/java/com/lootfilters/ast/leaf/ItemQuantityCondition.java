package com.lootfilters.ast.leaf;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.Comparator;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ItemQuantityCondition extends ComparatorCondition {
    public ItemQuantityCondition(int value, Comparator cmp) {
        super(value, cmp);
    }

    @Override
    public int getLhs(LootFiltersPlugin plugin, PluginTileItem item) {
        return item.getQuantity();
    }
}
