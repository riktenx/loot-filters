package com.lootfilters.rule;

import com.lootfilters.lang.ParseException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TextAccent {
    USE_FILTER("use filter"),
    SHADOW("shadow"),
    OUTLINE("outline"),
    NONE("none");

    private final String value;

    public static TextAccent fromOrdinal(int o) {
        switch (o) {
            case 1: return SHADOW;
            case 2: return OUTLINE;
            case 3: return NONE;
            default: throw new ParseException("unrecognized TextAccent ordinal " + o);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
