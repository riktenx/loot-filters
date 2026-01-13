package com.lootfilters;

import java.io.FileOutputStream;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

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

			try (
				var inner = new FileOutputStream("src/main/resources/com/lootfilters/" + filter.getFilename());
				var writer = new GZIPOutputStream(inner)
			) {
				writer.write(src.getBytes());
			}
		}
	}
}
