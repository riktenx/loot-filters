package com.lootfilters.migration;

import com.lootfilters.DefaultFilter;
import com.lootfilters.LootFiltersPlugin;

public class Migrate_1105_1106 {
	private static final String NAME = "Migrate_1105_1106";
	private static final String OLD_DEFAULT_NAME = "[default: FilterScape]";

	public static void run(LootFiltersPlugin plugin) {
		var migrated = plugin.getConfigManager().getConfiguration(LootFiltersPlugin.CONFIG_GROUP, NAME);
		if (migrated != null) {
			return;
		}
		plugin.getConfigManager().setConfiguration(LootFiltersPlugin.CONFIG_GROUP, NAME, true);

		var preferredDefault = plugin.getConfig().getPreferredDefault();
		if (preferredDefault != null && preferredDefault.equals(OLD_DEFAULT_NAME)) {
			plugin.getConfig().setPreferredDefault(DefaultFilter.RIKTENS);
		}
	}
}
