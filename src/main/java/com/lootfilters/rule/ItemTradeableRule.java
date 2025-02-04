package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import lombok.EqualsAndHashCode;
import net.runelite.api.TileItem;

@EqualsAndHashCode(callSuper = false)
public class ItemTradeableRule extends Rule {
    private final boolean target;

    public ItemTradeableRule(boolean target) {
        super("item_tradeable");
        this.target = target;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, TileItem item) {
        boolean tradeable = plugin.getItemManager().getItemComposition(item.getId()).isTradeable();
        return tradeable == target;
    }
}
