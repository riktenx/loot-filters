package com.lootfilters;

import com.lootfilters.lang.CompileException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.lootfilters.util.TextUtil.quote;

@Slf4j
@RequiredArgsConstructor
public class LootFilterManager {
    private final LootFiltersPlugin plugin;

    private final ExecutorService httpDispatcher = Executors.newSingleThreadExecutor();

    @Getter
    private final List<LootFilter> defaultFilters = new ArrayList<>();

    public List<LootFilter> loadFilters() {
        var filters = new ArrayList<LootFilter>();
        var files = LootFiltersPlugin.FILTER_DIRECTORY.listFiles();
        for (var file : files) {
            String src;
            try {
                src = Files.readString(file.toPath());
            } catch (MalformedInputException e) {
                plugin.addChatMessage("Failed to load filter from " + quote(file.getName()) + " because it is not a valid text file.");
                log.warn("read file {}", file.getName(), e);
                continue;
            } catch (Exception e) {
                plugin.addChatMessage("Failed to load filter from " + quote(file.getName()) + ": " + e.getMessage());
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

        var hadErrors = filters.size() != files.length;
        if (plugin.getClient().getGameState() == GameState.LOGGED_IN || hadErrors) {
            plugin.addChatMessage(String.format("Loaded <col=%s>%d/%d</col> loot filters.",
                    hadErrors ? "FF0000" : "00FF00", filters.size(), files.length));
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

    public void updateFilter(String filename, String src) throws IOException {
        var file = new File(LootFiltersPlugin.FILTER_DIRECTORY, filename);
        if (!file.exists()) {
            throw new IOException("attempt to update nonexistent file " + quote(filename));
        }

        try (var writer = new FileWriter(file)) {
            writer.write(src);
        }
    }

    public void fetchDefaultFilters(Runnable onComplete) {
        httpDispatcher.execute(() -> {
            for (var filter : DefaultFilter.all()) {
                fetchDefaultFilter(filter);
            }
            onComplete.run();
        });
    }

    public void fetchDefaultFilter(DefaultFilter filter) {
        var req = new Request.Builder()
                .get()
                .url(filter.getUrl())
                .addHeader("User-Agent", "github.com/riktenx/loot-filters")
                .build();
        try (var resp = plugin.getOkHttpClient().newCall(req).execute()) {
            var src = resp.body().string();
            var parsed = LootFilter.fromSource(src);
            defaultFilters.add(parsed);
        } catch (Exception e) { // there could be an issue w/ a filter, but just keep going and let other fetches complete
            log.warn("Failed to fetch default filter {}", filter.getName(), e);
        }
    }

    private static String toFilename(String filterName) {
        return filterName.replaceAll("[^a-zA-Z0-9._-]", "_") + ".rs2f";
    }
}