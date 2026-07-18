package com.lootfilters;

public class FilterNotFoundException extends RuntimeException {
    final String filterName;

    public FilterNotFoundException(String filterName) {
        super("Filter with file name " + filterName + " not found");
        this.filterName = filterName;
    }
}
