package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.runelite.api.TileItem;

import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class ItemIdRule extends Rule {
    private final List<Integer> ids;

    public ItemIdRule(List<Integer> ids) {
        super("item_id");
        this.ids = ids;
    }

    public ItemIdRule(int id) {
        super("item_id");
        this.ids = List.of(id);
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, TileItem item) {
        return ids.stream().anyMatch(it -> item.getId() == it);
    }
}
