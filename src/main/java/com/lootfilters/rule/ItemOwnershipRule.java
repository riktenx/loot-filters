package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class ItemOwnershipRule extends LeafRule {
    private final Ownership ownership;

    public ItemOwnershipRule(int ownership) {
        super("item_ownership");
        this.ownership = Ownership.fromOrdinal(ownership);
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        return item.getOwnership() == ownership.ordinal();
    }
}
