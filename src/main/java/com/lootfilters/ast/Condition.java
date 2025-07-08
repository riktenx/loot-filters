package com.lootfilters.ast;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;

public abstract class Condition {
    public abstract boolean test(LootFiltersPlugin plugin, PluginTileItem item);
}
