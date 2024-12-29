package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.Comparator;
import lombok.EqualsAndHashCode;
import net.runelite.api.TileItem;

@EqualsAndHashCode(callSuper = false)
public class ItemValueRule extends ComparatorRule {
    public ItemValueRule(LootFiltersPlugin plugin, int value, Comparator cmp) {
       super(plugin, "item_value", value, cmp);
    }

    @Override
    public int getLhs(TileItem item) {
        return plugin.getItemManager().getItemPrice(item.getId());
    }
}
