package com.lootfilters.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum FontMode {
    RUNELITE("runelite"),
    PLUGIN("plugin");

    private final String value;

    @Override
    public String toString() {
        return value;
    }
}
