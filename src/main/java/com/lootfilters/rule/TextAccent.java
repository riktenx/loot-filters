package com.lootfilters.rule;

import com.lootfilters.lang.ParseException;

public enum TextAccent {
    USE_FILTER, SHADOW, OUTLINE;

    public static TextAccent fromOrdinal(int o) {
        switch (o) {
            case 1: return SHADOW;
            case 2: return OUTLINE;
            default: throw new ParseException("unrecognized TextAccent ordinal " + o);
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case USE_FILTER: return "use filter";
            case SHADOW: return "shadow";
            default: return "outline";
        }
    }
}
