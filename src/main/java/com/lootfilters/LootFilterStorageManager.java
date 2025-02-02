package com.lootfilters;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.lootfilters.LootFiltersPlugin.FILTER_DIR;
import static com.lootfilters.LootFiltersPlugin.PLUGIN_DIR;

@Slf4j
@AllArgsConstructor
public class LootFilterStorageManager {
    private final LootFiltersPlugin plugin;

    public static java.io.File filterDirectory() {
        return new java.io.File(
                new java.io.File(RuneLite.RUNELITE_DIR, PLUGIN_DIR), FILTER_DIR
        );
    }

    public List<LootFilter> loadFilters() {
        var filters = new ArrayList<LootFilter>();
        for (var file : filterDirectory().listFiles()) {
            String src;
            try {
                src = Files.readString(file.toPath());
            } catch (Exception e) {
                log.warn(e.getMessage());
                continue;
            }

            LootFilter filter;
            try {
                filter = LootFilter.fromSource(src);
            } catch (Exception e) {
                log.warn(e.getMessage());
                continue;
            }

            filters.add(filter);
        }
        return filters;
    }

    public void saveNewFilter() throws IOException {
        // todo
    }
}
