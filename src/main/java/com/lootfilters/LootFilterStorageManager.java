package com.lootfilters;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.lootfilters.LootFiltersPlugin.FILTER_DIR;
import static com.lootfilters.LootFiltersPlugin.PLUGIN_DIR;
import static com.lootfilters.util.TextUtil.quote;

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
            if (filters.stream().anyMatch(it -> it.getName().equals(filter.getName()))) {
                log.warn("Duplicate filters found with name " + quote(filter.getName())
                        + ". Only the first one was loaded.");
                continue;
            }

            filters.add(filter);
        }
        return filters;
    }

    public void saveNewFilter(String name, String src) throws IOException {
        var sanitized = toFilename(name);
        var newFile = new File(filterDirectory(), toFilename(name));
        if (!newFile.createNewFile()) {
            throw new IOException("could not create file " + sanitized);
        }

        try (var writer = new FileWriter(newFile)) {
            writer.write(src);
        }
    }

    private static String toFilename(String filterName) {
        return filterName.replaceAll("[^a-zA-Z0-9._-]", "_") + ".rs2f";
    }
}
