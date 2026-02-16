package com.lootfilters;

import com.lootfilters.lang.CompileException;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.lootfilters.util.TextUtil.quote;

@Slf4j
@Singleton
public class LootFilterManager {
	@Inject
	private LootFiltersPlugin plugin;

	@Inject
	private LootFiltersConfig config;

	@Getter
	private final List<String> filenames = new ArrayList<>();

	@Getter
	private LootFilter loadedFilter;

	public static String toFilename(String filterName) {
		return filterName.replaceAll("[^a-zA-Z0-9._-]", "_") + ".rs2f";
	}

	public CompletableFuture<LootFilter> startUp() {
		return reload();
	}

	public void shutDown() {
		filenames.clear();
		loadedFilter = null;
	}

	public CompletableFuture<LootFilter> reload() {
		loadFiles();
		return loadFilter();
	}

    public void loadFiles() {
        var next = Arrays.stream(LootFiltersPlugin.FILTER_DIRECTORY.listFiles())
                .filter(it -> !it.getName().startsWith("."))
				.map(it -> it.getName())
                .collect(Collectors.toList());

		filenames.clear();
		if (config.showDefaultFilters()) {
			filenames.addAll(DefaultFilter.all().stream()
				.map(DefaultFilter::getName)
				.collect(Collectors.toList()));
		}
		filenames.addAll(next);
    }

	public CompletableFuture<LootFilter> loadFilter() {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return doLoadFilter();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	private LootFilter doLoadFilter() throws IOException
	{
		var selected = plugin.getSelectedFilter();
		if (selected == null) {
			return saveLoaded(LootFilter.Nop);
		}
		if (DefaultFilter.isDefault(selected)) {
			return saveLoaded(DefaultFilter.loadByName(selected));
		}

		var file = new File(LootFiltersPlugin.FILTER_DIRECTORY, selected);
		var src = Files.readString(file.toPath());
		var filter = LootFilter.fromSource(file.getName(), src);

		return saveLoaded(filter);
	}

	public void createFilter(String name, String src) throws IOException {
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

	private LootFilter saveLoaded(LootFilter filter) {
		loadedFilter = filter;
		return filter;
	}
}