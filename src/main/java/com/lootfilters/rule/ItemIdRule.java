package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.runelite.api.TileItem;

@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class ItemIdRule extends Rule {
    private final int id;

    public ItemIdRule(int id) {
        super("item_id");
        this.id = id;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, TileItem item) {
        return item.getId() == id;
    }
}
