package com.lootfilters.rule;

import com.lootfilters.lang.ParseException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum FontType {
    USE_FILTER("use filter"),
    NORMAL("normal"),
    LARGER("larger");

    private final String value;

    public static FontType fromOrdinal(int o) {
        switch (o) {
            case 1: return NORMAL;
            case 2: return LARGER;
            default: throw new ParseException("unrecognized FontType ordinal " + o);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
