package com.lootfilters.ast;

import com.lootfilters.lang.ParseException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum FontType {
    USE_FILTER("use filter"),
    NORMAL("small"),
    LARGER("regular"),
    BOLD("bold");

    private final String value;

    public static FontType fromOrdinal(int o) {
        switch (o) {
            case 1: return NORMAL;
            case 2: return LARGER;
            case 3: return BOLD;
            default:
                throw new ParseException("unrecognized FontType ordinal " + o);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
