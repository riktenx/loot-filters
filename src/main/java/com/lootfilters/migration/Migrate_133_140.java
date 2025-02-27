package com.lootfilters.migration;

import com.google.gson.reflect.TypeToken;
import com.lootfilters.LootFiltersPlugin;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class Migrate_133_140 {
    private final LootFiltersPlugin plugin;

    public static void run(LootFiltersPlugin plugin) {
        try {
            new Migrate_133_140(plugin).run();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Loot filters plugin: FAILED to migrate" +
                    " config-based filters to disk. Please contact the plugin maintainers on Discord or GitHub. Your" +
                    " existing filter data has NOT been removed.");
            log.warn("migrate filters to disk: {}{}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
    }

    public void run() throws Exception {
        var migrated = plugin.getConfigManager().getConfiguration(LootFiltersPlugin.CONFIG_GROUP, "Migrate_133_140");
        if (migrated != null) {
            return;
        }
        plugin.getConfigManager().setConfiguration(LootFiltersPlugin.CONFIG_GROUP, "Migrate_133_140", true);

        var toMigrate = getConfigUserFilters();
        if (toMigrate.isEmpty()) {
            return;
        }

        for (var i = 0; i < toMigrate.size(); ++i) {
            plugin.getFilterManager().saveNewFilter("migrated_filter_" + i, toMigrate.get(i));
        }
        plugin.reloadFilters();
        plugin.getPluginPanel().reflowFilterSelect(plugin.getParsedUserFilters(), plugin.getSelectedFilterName());
    }

    private List<String> getConfigUserFilters() {
        var cfg = plugin.getConfigManager().getConfiguration(LootFiltersPlugin.CONFIG_GROUP, "user-filters");
        if (cfg == null || cfg.isEmpty()) {
            return new ArrayList<>();
        }

        var type = new TypeToken<List<String>>() {
        }.getType();
        return plugin.getGson().fromJson(cfg, type);
    }
}
