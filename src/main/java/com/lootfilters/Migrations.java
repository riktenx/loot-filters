package com.lootfilters;

import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JOptionPane;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

@Slf4j
public class Migrations {
    private Migrations() {}

    // we can create a more formal abstraction for these if we end up doing lots of migrations, hopefully things don't
    // turn out that way though
    public static void run(LootFiltersPlugin plugin) {
        try {
            new Migrate_688d8d9_ConfigToDisk(plugin).run();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Loot filters plugin: FAILED to migrate" +
                    " config-based filters to disk. Please contact the plugin maintainers. Your existing filter data" +
                    " has NOT been removed.");
            log.warn("{}{}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
    }

    @AllArgsConstructor
    private static class Migrate_688d8d9_ConfigToDisk {
        public static final String LEGACY_USER_FILTERS_KEY = "user-filters";

        private final LootFiltersPlugin plugin;

        public void run() throws Exception {
            var migrated = plugin.getConfigManager()
                    .getConfiguration(LootFiltersPlugin.CONFIG_GROUP, this.getClass().getName());
            if (migrated != null) {
                return;
            }
            plugin.getConfigManager()
                    .setConfiguration(LootFiltersPlugin.CONFIG_GROUP, this.getClass().getName(), true);

            var toMigrate = getConfigUserFilters();
            if (toMigrate.isEmpty()) {
                return;
            }

            for (var i = 0; i < toMigrate.size(); ++i) {
                plugin.getStorageManager().saveNewFilter("migrated_filter_" + i, toMigrate.get(i));
            }
            plugin.reloadFilters();
            plugin.getPluginPanel().reflowFilterSelect(plugin.getParsedUserFilters(), plugin.getSelectedFilterName());
        }

        private List<String> getConfigUserFilters() {
            var cfg = plugin.getConfigManager()
                    .getConfiguration(LootFiltersPlugin.CONFIG_GROUP, LEGACY_USER_FILTERS_KEY);
            if (cfg == null || cfg.isEmpty()) {
                return emptyList();
            }

            var type = new TypeToken<List<String>>(){}.getType();
            return plugin.getGson().fromJson(cfg, type);
        }
    }
}
