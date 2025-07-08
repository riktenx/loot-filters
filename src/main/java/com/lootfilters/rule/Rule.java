package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;

public abstract class Rule {
    public abstract boolean test(LootFiltersPlugin plugin, PluginTileItem item);
}
