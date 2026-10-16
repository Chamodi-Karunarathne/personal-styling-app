package com.chamodi.styling.profile.entity;

import java.util.Objects;

import com.chamodi.styling.profile.model.PreferenceCategory;
import com.chamodi.styling.profile.model.PreferenceStrength;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class PreferenceSelection {

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 30)
	private PreferenceCategory category;

	@Column(name = "option_code", nullable = false, length = 40)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(name = "strength", nullable = false, length = 20)
	private PreferenceStrength strength;

	protected PreferenceSelection() {
	}

	public PreferenceSelection(PreferenceCategory category, String code, PreferenceStrength strength) {
		this.category = category;
		this.code = code;
		this.strength = strength;
	}

	public PreferenceCategory getCategory() {
		return category;
	}

	public String getCode() {
		return code;
	}

	public PreferenceStrength getStrength() {
		return strength;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof PreferenceSelection selection && category == selection.category
				&& Objects.equals(code, selection.code) && strength == selection.strength;
	}

	@Override
	public int hashCode() {
		return Objects.hash(category, code, strength);
	}
}
