package com.chamodi.styling.profile.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record PreferenceCatalogResponse(List<Group> groups) {

	public record Group(String key, String label, boolean allowsRequired, List<Option> options) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Option(String code, String label, String swatch) {
		public Option(String code, String label) {
			this(code, label, null);
		}
	}
}
