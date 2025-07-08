package com.lootfilters.ast;

import com.lootfilters.lang.ParseException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Ownership {
    NONE("none"),
    SELF("self"),
    OTHER("other"),
    GROUP("group");

    private final String value;

    public static Ownership fromOrdinal(int o) {
        switch (o) {
            case 0: return NONE;
            case 1: return SELF;
            case 2: return OTHER;
            case 3: return GROUP;
            default: throw new ParseException("Ownership ordinal out of bounds [0-3]: " + o);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
