package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import lombok.EqualsAndHashCode;
import net.runelite.api.TileItem;

@EqualsAndHashCode(callSuper = false)
public class ItemNotedRule extends Rule {
    private final boolean target;

    public ItemNotedRule(boolean target) {
        super("item_noted");
        this.target = target;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, TileItem item) {
        var comp = plugin.getItemManager().getItemComposition(item.getId());

        boolean isNote = comp.getNote() != -1;
        return target == isNote;
    }
}
