package com.lootfilters.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ItemValueRulesMode {
    BEFORE_FILTER("before filter"),
    AFTER_FILTER("after filter"),
    OFF("off");

    private final String value;

    @Override
    public String toString() {
        return value;
    }
}
