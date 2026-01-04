package com.lootfilters;

import com.lootfilters.lang.Sources;
import java.util.List;

public class DefaultFilter {
    public static final String RIKTENS = "[default: Rikten's filter]";
    public static final String JOES = "[default: Joe's filter]";

    public static List<String> defaultFilters() {
        return List.of(RIKTENS, JOES);
    }

	public static boolean isDefaultFilter(String name) {
		return name != null && defaultFilters().contains(name);
	}

	public static LootFilter loadDefaultFilter(String name) {
		var resource = name.equals(RIKTENS) ? "default-riktens.rs2f" : "default-joes.rs2f";
		try {
			var src = Sources.loadScriptResource(DefaultFilter.class, resource);
			return LootFilter.fromSource(name, src);
		} catch (Exception e) {
			return LootFilter.Nop;
		}
	}
}
