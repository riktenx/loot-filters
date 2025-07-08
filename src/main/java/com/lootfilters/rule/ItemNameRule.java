package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

import static com.lootfilters.util.TextUtil.isInfixWildcard;

@EqualsAndHashCode(callSuper = false)
@ToString
public class ItemNameRule extends LeafRule {
    private final List<String> names;

    public ItemNameRule(List<String> names) {
        this.names = names;
    }

    public ItemNameRule(String name) {
        this.names = List.of(name);
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        return names.stream().anyMatch(it -> test(item.getName(), it));
    }

    private boolean test(String name, String target) {
        if (target.equals("*")) {
            return true;
        } else if (target.startsWith("*") && target.endsWith("*")) {
            return name.toLowerCase().contains(target.toLowerCase().substring(1, target.length() - 1));
        } else if (target.startsWith("*")) {
            return name.toLowerCase().endsWith(target.toLowerCase().substring(1));
        } else if (target.endsWith("*")) {
            return name.toLowerCase().startsWith(target.toLowerCase().substring(0, target.length() - 1));
        } else if (isInfixWildcard(target)) {
            var lowercase = name.toLowerCase();
            var index = target.indexOf('*');
            var before = target.substring(0, index).toLowerCase();
            var after = target.substring(index + 1).toLowerCase();
            return lowercase.startsWith(before) && lowercase.endsWith(after);
        }
        return name.equalsIgnoreCase(target);
    }
}
