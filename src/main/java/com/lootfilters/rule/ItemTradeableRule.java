package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.runelite.api.ItemID;

@EqualsAndHashCode(callSuper = false)
@ToString
public class ItemTradeableRule extends Rule {
    private final boolean target;

    public ItemTradeableRule(boolean target) {
        super("item_tradeable");
        this.target = target;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        if (item.getId() == ItemID.COINS_995 || item.getId() == ItemID.PLATINUM_TOKEN) {
            return target;
        }

        var comp = plugin.getItemManager().getItemComposition(item.getId());
        var linkedComp = plugin.getItemManager().getItemComposition(comp.getLinkedNoteId());
        return target
                ? comp.isTradeable() || linkedComp.isTradeable()
                : !comp.isTradeable() && !linkedComp.isTradeable();
    }
}
