package com.lootfilters.ast.leaf;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.ast.LeafCondition;
import com.lootfilters.model.Ownership;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class ItemOwnershipCondition extends LeafCondition {
    private final Ownership ownership;

    public ItemOwnershipCondition(int ownership) {
        this.ownership = Ownership.fromOrdinal(ownership);
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        return item.getOwnership() == ownership.ordinal();
    }
}
