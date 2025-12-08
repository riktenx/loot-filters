package com.lootfilters.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum LootbeamHeight {
    NORMAL(1),
    TALL(2),
    TALLER(4),
    ;

    @Getter
    private final int multiplier;
}
