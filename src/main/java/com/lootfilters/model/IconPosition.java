package com.lootfilters.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum IconPosition {
	OUTSIDE("outside"),
	INSIDE("inside"),
	;

	private final String value;

	@Override
	public String toString() {
		return value;
	}
}
