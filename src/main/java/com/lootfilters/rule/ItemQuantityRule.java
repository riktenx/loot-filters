package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.runelite.api.TileItem;

@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ItemQuantityRule extends ComparatorRule {
    public ItemQuantityRule(int value, Comparator cmp) {
        super("item_quantity", value, cmp);
    }

    @Override
    public int getLhs(LootFiltersPlugin plugin, TileItem item) {
        return item.getQuantity();
    }
}
