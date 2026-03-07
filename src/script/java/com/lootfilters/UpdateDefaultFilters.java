package com.lootfilters;

import java.io.FileOutputStream;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

public class UpdateDefaultFilters {
	public static void main(String[] args) throws Exception {
		for (var filter : DefaultFilter.all()) {
			updateDefaultFilter(filter);
		}

		//good for benchmarking but we don't currently include it as a default
		//downloadStornsIronFilter();
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

	private static void downloadStornsIronFilter() throws Exception {
		var url = new URL("https://raw.githubusercontent.com/Storn42/Iron-Filter/refs/heads/main/Iron-Filter.rs2f");
		try (var stream = url.openStream()) {
			var src = new String(stream.readAllBytes());

			try (
				var inner = new FileOutputStream("src/main/resources/com/lootfilters/stornsironfilter.rs2f.gz");
				var writer = new GZIPOutputStream(inner)
			) {
				writer.write(src.getBytes());
			}
		}
	}
}
