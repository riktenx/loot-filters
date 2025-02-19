package com.lootfilters.lang;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@RequiredArgsConstructor
public class Location {
    public static Location UNKNOWN = new Location("unknown", 0, 0);
    public static String UNKNOWN_SOURCE_NAME = "unknown";

    String sourceName;
    int lineNumber;
    int charNumber;
    @NonFinal
    Location macroSourceLocation = null;
    @NonFinal
    String macroName = null;

    public Location withMacroName(String name) {
        var loc = new Location(sourceName, lineNumber, charNumber);
        loc.macroName = name;
        return loc;
    }

    public Location withMacroSourceLocation(Location macroSourceLocation) {
        var loc = new Location(sourceName, lineNumber, charNumber);
        loc.macroSourceLocation = macroSourceLocation;
        return loc;
    }

    @Override
    public String toString() {
        var base = String.format("Location(sourceName=%s, lineNumber=%d, charNumber=%d", sourceName, lineNumber, charNumber);
        if (macroSourceLocation == null) {
            return base + ")";
        } else {
            return base + ", macroSourceLocation=" + macroSourceLocation + ")";
        }
    }
}
