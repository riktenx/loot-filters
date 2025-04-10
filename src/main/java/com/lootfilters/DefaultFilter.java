package com.lootfilters;

import lombok.Value;

import java.util.List;

@Value
public class DefaultFilter {
    String name, url;

    public static final DefaultFilter FILTERSCAPE = new DefaultFilter(
            "[default: FilterScape]",
            "https://raw.githubusercontent.com/riktenx/filterscape/refs/heads/main/filterscape.rs2f"
    );
    public static final DefaultFilter JOESFILTER = new DefaultFilter(
            "[default: Joe's filter]",
            "https://raw.githubusercontent.com/typical-whack/loot-filters-modules/refs/heads/main/default-filter.rs2f"
    );

    public static List<DefaultFilter> all() {
        return List.of(FILTERSCAPE, JOESFILTER);
    }
}
