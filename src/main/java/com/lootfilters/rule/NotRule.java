package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString
public class NotRule extends Rule {
    private final Rule inner;

    public NotRule(Rule inner) {
        this.inner = inner;
    }

    @Override
    public boolean test(LootFiltersPlugin plugin, PluginTileItem item) {
        return !inner.test(plugin, item);
    }
}
