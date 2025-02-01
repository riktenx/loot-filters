package com.lootfilters.rule;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ValueTier {
    NONE("none"),
    INSANE("insane"),
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
