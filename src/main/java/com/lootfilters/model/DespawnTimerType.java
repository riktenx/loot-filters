package com.lootfilters.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum DespawnTimerType {
    TICKS("ticks"),
    SECONDS("seconds"),
    PIE("pie");

    private final String label;

    @Override
    public String toString() {
        return label;
    }
}
