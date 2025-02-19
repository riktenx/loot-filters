package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.runelite.api.ItemID;
import net.runelite.api.TileItem;

@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ItemValueRule extends ComparatorRule {
    public ItemValueRule(int value, Comparator cmp) {
       super("item_value", value, cmp);
    }

    @Override
    public int getLhs(LootFiltersPlugin plugin, TileItem item) {
        switch (item.getId()) {
            case ItemID.COINS_995:
                return item.getQuantity();
            case ItemID.PLATINUM_TOKEN:
                return item.getQuantity() * 1000;
            default:
                return getValue(plugin, item) * item.getQuantity();
        }
    }

    private int getValue(LootFiltersPlugin plugin, TileItem item) {
        var ge = plugin.getItemManager().getItemPrice(item.getId());
        var ha = plugin.getItemManager().getItemComposition(item.getId()).getHaPrice();
        switch (plugin.getConfig().valueType()) {
            case HIGHEST: return Math.max(ge, ha);
            case GE: return ge;
            default: return ha;
        }
    }
}
