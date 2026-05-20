package com.lootfilters;

import com.lootfilters.lang.CompileException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.FileManager;

import static com.lootfilters.util.TextUtil.quote;

@Slf4j
@Singleton
public class LootFilterManager {
	@Inject
	private LootFiltersPlugin plugin;

	@Inject
	private LootFiltersConfig config;

	@Inject
	private FileManager fileManager;

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
		try (var stream = fileManager.walk("filters")) {
			var next = stream
				.filter(e -> !e.isDirectory())
				.filter(e -> !e.getPath().startsWith("."))
				.map(e -> e.getPath())
				.collect(Collectors.toList());

			filenames.clear();
			if (config.showDefaultFilters()) {
				filenames.addAll(DefaultFilter.all().stream()
					.map(DefaultFilter::getName)
					.collect(Collectors.toList()));
			}
			filenames.addAll(next);
		} catch (IOException e) {
			log.error("load filter files", e);
		}
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

		String src;
		try (var is = fileManager.read("filters", selected)) {
			src = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		var filter = LootFilter.fromSource(selected, src);

		return saveLoaded(filter);
	}

	public void createFilter(String name, String src) throws IOException {
        var sanitized = toFilename(name);
        if (fileManager.exists("filters", sanitized)) {
            throw new IOException("could not create file " + sanitized);
        }
        try (var writer = new OutputStreamWriter(fileManager.write("filters", sanitized), StandardCharsets.UTF_8)) {
            writer.write(src);
        }
    }

    public void updateFilter(String filename, String src) throws IOException {
        if (!fileManager.exists("filters", filename)) {
            throw new IOException("attempt to update nonexistent file " + quote(filename));
        }
        try (var writer = new OutputStreamWriter(fileManager.write("filters", filename), StandardCharsets.UTF_8)) {
            writer.write(src);
        }
    }

	private LootFilter saveLoaded(LootFilter filter) {
		loadedFilter = filter;
		return filter;
	}
}