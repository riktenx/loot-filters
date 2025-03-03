package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.runelite.api.TileItem;

@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class ItemOwnershipRule extends Rule {
    private final Ownership ownership;

    public ItemOwnershipRule(int ownership) {
        super("item_ownership");
        this.ownership = Ownership.fromOrdinal(ownership);
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, TileItem item) {
        return item.getOwnership() == ownership.ordinal();
    }
}
