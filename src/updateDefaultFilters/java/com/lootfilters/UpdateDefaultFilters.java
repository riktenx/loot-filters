package com.lootfilters;

import java.io.FileOutputStream;
import java.net.URL;

public class UpdateDefaultFilters {
	public static void main(String[] args) throws Exception {
		for (var filter : DefaultFilter.all()) {
			updateDefaultFilter(filter);
		}
	}

	private static void updateDefaultFilter(DefaultFilter filter) throws Exception {
		var url = new URL(filter.getUrl());
		try (var stream = url.openStream()) {
			var src = new String(stream.readAllBytes());
			LootFilter.fromSource(filter.getName(), src);

			try (var writer = new FileOutputStream("src/main/resources/com/lootfilters/" + filter.getFilename())) {
				writer.write(src.getBytes());
			}
		}
	}
}
