package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import lombok.EqualsAndHashCode;
import net.runelite.api.ItemID;
import net.runelite.api.TileItem;

@EqualsAndHashCode(callSuper = false)
public class ItemStackableRule extends Rule {
    private final boolean target;

    public ItemStackableRule(boolean target) {
        super("item_stackable");
        this.target = target;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, TileItem item) {
        if (item.getId() == ItemID.COINS_995 || item.getId() == ItemID.PLATINUM_TOKEN) {
            return target;
        }

        var comp = plugin.getItemManager().getItemComposition(item.getId());

        return target == comp.isStackable();
    }
}
