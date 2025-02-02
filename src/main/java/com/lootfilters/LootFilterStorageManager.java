package com.lootfilters;

import net.runelite.client.RuneLite;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.lootfilters.LootFiltersPlugin.FILTER_DIR;
import static com.lootfilters.LootFiltersPlugin.PLUGIN_DIR;

public class LootFilterStorageManager {
    public static java.io.File filterDirectory() {
        return new java.io.File(
                new java.io.File(RuneLite.RUNELITE_DIR, PLUGIN_DIR), FILTER_DIR
        );
    }

    public List<LootFilter> loadFilters() throws IOException {
        var filters = new ArrayList<LootFilter>();
        for (var file : filterDirectory().listFiles()) {
            var src = Files.readString(file.toPath());
            var filter = LootFilter.fromSource(src);
            filters.add(filter);
        }
        return filters;
    }

    public void saveNewFilter() throws IOException {
        // todo
    }
}
