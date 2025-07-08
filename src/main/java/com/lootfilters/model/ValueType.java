package com.lootfilters.model;

public enum ValueType {
    HIGHEST, GE, HA;

    @Override
    public String toString() {
        switch (this) {
            case HIGHEST: return "highest";
            case GE: return "grand exchange";
            default: return "high alchemy";
        }
    }
}
