package com.lootfilters;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.lootfilters.util.TextUtil.quote;

@Slf4j
@AllArgsConstructor
public class LootFilterManager {
    private final LootFiltersPlugin plugin;

    public List<LootFilter> loadFilters() {
        var filters = new ArrayList<LootFilter>();
        for (var file : LootFiltersPlugin.FILTER_DIRECTORY.listFiles()) {
            String src;
            try {
                src = Files.readString(file.toPath());
            } catch (Exception e) {
                log.warn("read file {}", file.getName(), e);
                continue;
            }

            LootFilter filter;
            try {
                filter = LootFilter.fromSourcesWithPreamble(Map.of(file.getName(), src));
            } catch (Exception e) {
                plugin.addChatMessage("Failed to load filter from " + file.getName() + ": " + e.getMessage());
                log.warn("parse file {}", file.getName(), e);
                continue;
            }
            if (filters.stream().anyMatch(it -> it.getName().equals(filter.getName()))) {
                log.warn("Duplicate filters found with name {}. Only the first one was loaded.", quote(filter.getName()));
                continue;
            }

            filters.add(filter);
        }
        return filters;
    }

    public void saveNewFilter(String name, String src) throws IOException {
        var sanitized = toFilename(name);
        var newFile = new File(LootFiltersPlugin.FILTER_DIRECTORY, toFilename(name));
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