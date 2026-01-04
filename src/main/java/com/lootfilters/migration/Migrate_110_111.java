package com.lootfilters.migration;

import com.lootfilters.DefaultFilter;
import com.lootfilters.LootFilter;
import com.lootfilters.LootFiltersPlugin;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import javax.swing.JOptionPane;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class Migrate_110_111 {
	private static final String NAME = "Migrate_110_111";

	private final LootFiltersPlugin plugin;

	public static void run(LootFiltersPlugin plugin) {
		try {
			new Migrate_110_111(plugin).run();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "<html>" +
				"[Loot Filters]: Encountered a problem when performing the v1.11 migration.<br/><br/>" +
				"Your settings and filters were not affected and the plugin should continue to operate as normal.<br/>" +
				"You may need to re-select your filter in the plugin panel.<br/><br/>" +
				"This message will not be shown again." +
				"</html>");
			log.error("migrate 1.10 -> 1.11: {}{}", e.getMessage(), Arrays.toString(e.getStackTrace()));
		}
	}

	public void run() throws Exception {
		var migrated = plugin.getConfigManager().getConfiguration(LootFiltersPlugin.CONFIG_GROUP, NAME);
		if (migrated != null) {
			return;
		}
		plugin.getConfigManager().setConfiguration(LootFiltersPlugin.CONFIG_GROUP, NAME, true);

		var selected = plugin.getSelectedFilter(); // this will be the old format i.e. the actual filter name
		if (selected == null || DefaultFilter.isDefaultFilter(selected)) {
			return;
		}

		// we are just going to do this one-off migration synchronously so plugin startUp() doesn't have to think about it
		plugin.getFilterManager().loadFiles();
		for (var filename : plugin.getFilterManager().getFilenames()) {
			if (DefaultFilter.isDefaultFilter(filename)) {
				continue;
			}

			var file = new File(LootFiltersPlugin.FILTER_DIRECTORY, filename);
			var src = Files.readString(file.toPath());
			var filter = LootFilter.fromSource(filename, src);
			if (filter.getName().equals(selected)) {
				log.info("migrate 1.10 -> 1.11: {} maps to {}", filter.getName(), filename);
				plugin.getConfigManager().setConfiguration(LootFiltersPlugin.CONFIG_GROUP, LootFiltersPlugin.SELECTED_FILTER_KEY, filename);
				return;
			}
		}
	}
}
