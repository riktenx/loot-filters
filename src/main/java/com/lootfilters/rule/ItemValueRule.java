package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ItemValueRule extends ComparatorRule {
    private final ValueType valueType;

    public ItemValueRule(int value, Comparator cmp, ValueType valueType) {
       super("item_value", value, cmp);
       this.valueType = valueType;
    }

    @Override
    public int getLhs(LootFiltersPlugin plugin, PluginTileItem item) {
        return getValue(item) * item.getQuantity();
    }

    private int getValue(PluginTileItem item) {
        switch (valueType) {
            case HIGHEST: return Math.max(item.getGePrice(), item.getHaPrice());
            case GE: return item.getGePrice();
            default: return item.getHaPrice();
        }
    }
}
