package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.runelite.api.TileItem;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@ToString
public class ItemNameRule extends Rule {
    private final List<String> names;

    public ItemNameRule(List<String> names) {
        super("item_name");
        this.names = names;
    }

    public ItemNameRule(String name) {
        super("item_name");
        this.names = List.of(name);
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, TileItem item) {
        var itemName = plugin.getItemName(item.getId());
        return names.stream().anyMatch(it -> test(itemName, it));
    }

    private boolean test(String name, String target) {
        if (target.startsWith("*") && target.endsWith("*")) {
            return name.toLowerCase().contains(target.toLowerCase().substring(1, target.length() - 1));
        } else if (target.startsWith("*")) {
            return name.toLowerCase().endsWith(target.toLowerCase().substring(1));
        } else if (target.endsWith("*")) {
            return name.toLowerCase().startsWith(target.toLowerCase().substring(0, target.length() - 1));
        }
        return name.equalsIgnoreCase(target);
    }
}
