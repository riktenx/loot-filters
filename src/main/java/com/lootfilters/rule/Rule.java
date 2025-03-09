package com.lootfilters.rule;

import com.lootfilters.LootFiltersPlugin;
import com.lootfilters.model.PluginTileItem;

public abstract class Rule {
    protected final String discriminator; // serde discriminator

    protected Rule(String discriminator) {
        this.discriminator = discriminator;
    }

    public abstract boolean test(LootFiltersPlugin plugin, PluginTileItem item);
}
