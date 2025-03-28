package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString
public class ItemStackableRule extends Rule {
    private final boolean target;

    public ItemStackableRule(boolean target) {
        super("item_stackable");
        this.target = target;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        var comp = plugin.getItemManager().getItemComposition(item.getId());

        return target == comp.isStackable();
    }
}
