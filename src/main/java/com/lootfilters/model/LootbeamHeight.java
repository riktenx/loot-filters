package com.lootfilters.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum LootbeamHeight {
    NORMAL("normal", 1),
    TALL("tall", 2),
    TALLER("taller", 4),
    TALLEST("why?", 8);

    private final String name;
    @Getter
    private final int multiplier;

    @Override
    public String toString() {
        return name;
    }
}
