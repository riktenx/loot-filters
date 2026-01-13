package com.lootfilters;

import com.lootfilters.lang.Sources;
import java.util.List;
import lombok.Getter;

@Getter
public enum DefaultFilter {
	RIKTEN(
		"[default: Rikten's filter]",
		"https://raw.githubusercontent.com/riktenx/filterscape/refs/heads/main/default.rs2f"
	),
	JOE(
		"[default: Joe's filter]",
		"https://raw.githubusercontent.com/typical-whack/loot-filters-modules/refs/heads/main/default-filter.rs2f"
	),
	;

	private final String name, filename, url;

	DefaultFilter(String name, String url) {
		this.name = name;
		this.filename = toFilename(name);
		this.url = url;
	}

    public static List<DefaultFilter> all() {
        return List.of(values());
    }

	public static DefaultFilter byName(String name) {
		return all().stream()
			.filter(it -> it.name.equals(name))
			.findFirst()
			.orElse(null);
	}

	public static boolean isDefault(String name) {
		return byName(name) != null;
	}

	public static LootFilter loadByName(String name) {
		var filter = byName(name);
		if (filter == null) {
			return LootFilter.Nop;
		}

		try {
			var src = Sources.loadScriptResource(DefaultFilter.class, filter.filename);
			return LootFilter.fromSource(name, src);
		} catch (Exception e) {
			return LootFilter.Nop;
		}
	}

	private static String toFilename(String name) {
		return name
			.toLowerCase()
			.replace(" ", "_")
			.replaceAll("[^a-z]", "") + ".rs2f";
	}
}
