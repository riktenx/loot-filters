package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString
public class ItemNotedRule extends Rule {
    private final boolean target;

    public ItemNotedRule(boolean target) {
        super("item_noted");
        this.target = target;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        var comp = plugin.getItemManager().getItemComposition(item.getId());

        boolean isNote = comp.getNote() != -1;
        return target == isNote;
    }
}
