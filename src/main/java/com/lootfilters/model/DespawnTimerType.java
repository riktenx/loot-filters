package com.lootfilters.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum DespawnTimerType {
    TICKS("ticks"),
    SECONDS("seconds"),
    PIE("pie"),
    BAR("bar");
    private final String label;

    @Override
    public String toString() {
        return label;
    }
}
