package com.lootfilters;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.lootfilters.util.TextUtil.quote;

@Slf4j
@RequiredArgsConstructor
public class LootFilterManager {
    public static final String DEFAULT_FILTER_NAME = "[default]";

    private final LootFiltersPlugin plugin;

    @Getter
    @Setter
    private LootFilter defaultFilter = null;

    public List<LootFilter> loadFilters() {
        var filters = new ArrayList<LootFilter>();
        var files = LootFiltersPlugin.FILTER_DIRECTORY.listFiles();
        for (var file : files) {
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
                filter.setFilename(file.getName());
                if (filter.getName() == null || filter.getName().isBlank()) {
                    filter.setName(file.getName());
                }
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
        plugin.addChatMessage(String.format("Loaded <col=%s>%d/%d</col> loot filters.",
                filters.size() == files.length ? "00FF00" : "FF0000", filters.size(), files.length));
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

    public void updateFilter(String filename, String src) throws IOException {
        var file = new File(LootFiltersPlugin.FILTER_DIRECTORY, filename);
        if (!file.exists()) {
            throw new IOException("attempt to update nonexistent file " + quote(filename));
        }

        try (var writer = new FileWriter(file)) {
            writer.write(src);
        }
    }

    public void fetchDefaultFilter(Runnable onComplete) {
        var req = new Request.Builder()
                .get()
                .url("https://raw.githubusercontent.com/riktenx/filterscape/refs/heads/main/filterscape.rs2f")
                .addHeader("User-Agent", "github.com/riktenx/loot-filters")
                .build();
        plugin.getOkHttpClient().newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.warn("Failed to fetch default filter", e);
                plugin.addChatMessage("Failed to fetch default filter: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    defaultFilter = LootFilter.fromSource(response.body().string()); // string() DOES close the response body
                } catch (Exception e) {
                    log.warn("Failed to load default filter", e);
                    plugin.addChatMessage("Failed to load default filter: " + e.getMessage());
                    return;
                }

                onComplete.run();
            }
        });
    }

    private static String toFilename(String filterName) {
        return filterName.replaceAll("[^a-zA-Z0-9._-]", "_") + ".rs2f";
    }
}