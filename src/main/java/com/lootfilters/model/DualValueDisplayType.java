package com.lootfilters.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum DualValueDisplayType {
    COMPACT("compact"),
    VERBOSE("verbose");

    private final String label;

    @Override public String toString() { return label; }
}
