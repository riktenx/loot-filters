package com.lootfilters.model;

import com.lootfilters.ast.Comparator;
import lombok.Value;

import static java.lang.Integer.parseInt;

@Value
public class NamedQuantity {
    String name;
    Comparator comparator;
    int quantity;

    public static NamedQuantity fromString(String str) {
        if (str.contains(">")) {
            return fromString(str, ">");
        } else if (str.contains("<")) {
            return fromString(str, "<");
        }
        return new NamedQuantity(str, Comparator.GT, 0);
    }

    private static NamedQuantity fromString(String str, String cmp) {
        var parts = str.split(cmp);
        if (parts.length != 2) {
            return new NamedQuantity(str, Comparator.GT, 0);
        }

        int quantity;
        try {
            quantity = parseInt(parts[1].trim());
        } catch (Exception ignored) {
            return new NamedQuantity(str, Comparator.GT, 0);
        }

        return new NamedQuantity(parts[0].trim(), Comparator.fromString(cmp), quantity);
    }
}
