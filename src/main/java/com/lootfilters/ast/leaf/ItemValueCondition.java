package com.lootfilters.ast.leaf;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.ast.Comparator;
import com.lootfilters.ast.ComparatorCondition;
import com.lootfilters.ast.ValueType;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ItemValueCondition extends ComparatorCondition {
    private final ValueType valueType;

    public ItemValueCondition(int value, Comparator cmp, ValueType valueType) {
       super(value, cmp);
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
