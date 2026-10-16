package com.chamodi.styling.profile.model;

public enum PreferenceCategory {
	STYLES("styles"), PREFERRED_COLORS("preferredColors"), DISLIKED_COLORS("dislikedColors"),
	FITS("fits"), FOOTWEAR("footwear"), COMFORT("comfort"), COVERAGE("coverage"),
	COMMON_OCCASIONS("commonOccasions"), ACTIVITIES("activities"), TRANSPORT_MODES("transportModes");

	private final String key;

	PreferenceCategory(String key) {
		this.key = key;
	}

	public String key() {
		return key;
	}
}
