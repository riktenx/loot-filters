package com.lootfilters.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ValueDisplayType {
    HIGHEST("highest"),
    GE("grand exchange"),
    HA("high alchemy"),
    BOTH("both");

    private final String label;

    @Override public String toString() { return label; }
}
