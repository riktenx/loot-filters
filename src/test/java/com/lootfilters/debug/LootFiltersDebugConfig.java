package com.lootfilters.debug;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("loot-filters-debug")
public interface LootFiltersDebugConfig extends Config {
    @ConfigItem(
            keyName = "showOverlay",
            name = "Show overlay",
            description = ""
    )
    default boolean showOverlay() {
        return true;
    }
}
