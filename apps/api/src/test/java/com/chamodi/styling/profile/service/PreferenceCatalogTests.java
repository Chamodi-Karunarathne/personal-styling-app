package com.chamodi.styling.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.chamodi.styling.profile.dto.CoverageChoice;
import com.chamodi.styling.profile.dto.ProfilePreferences;
import com.chamodi.styling.profile.exception.ProfileValidationException;
import com.chamodi.styling.profile.model.PreferenceCategory;
import com.chamodi.styling.profile.model.PreferenceStrength;

class PreferenceCatalogTests {

	private final PreferenceCatalog catalog = new PreferenceCatalog();

	@Test
	void exposesAllApprovedGroupsInStableOrderWithNamedColorSwatches() {
		assertEquals(Arrays.stream(PreferenceCategory.values()).map(PreferenceCategory::key).toList(),
				catalog.response().groups().stream().map(group -> group.key()).toList());
		assertEquals(10, catalog.response().groups().size());
		assertEquals(13, catalog.response().groups().get(1).options().size());
		assertEquals("#26354e", catalog.response().groups().get(1).options().get(5).swatch());
		assertEquals(List.of("coverage"), catalog.response().groups().stream()
				.filter(group -> group.allowsRequired()).map(group -> group.key()).toList());
	}

	@Test
	void distinguishesUnansweredFromExplicitNoPreference() {
		var unanswered = catalog.prepare(ProfilePreferences.unanswered());
		var noPreference = catalog.prepare(preferences(List.of(), null, null, null));
		assertTrue(unanswered.answered().isEmpty());
		assertTrue(noPreference.answered().contains(PreferenceCategory.STYLES));
		assertTrue(noPreference.choices().isEmpty());
	}

	@Test
	void rejectsUnsupportedValuesWithoutTrimmingOrNormalizingThem() {
		var exception = assertThrows(ProfileValidationException.class,
				() -> catalog.prepare(preferences(List.of(" casual "), null, null, null)));
		assertEquals("Choose a supported option", exception.getFieldErrors().get("preferences.styles"));
		assertFalse(exception.getMessage().contains("casual"));
	}

	@Test
	void rejectsDuplicateCodesEvenWhenCoverageStrengthsDiffer() {
		var exception = assertThrows(ProfileValidationException.class, () -> catalog.prepare(
				preferences(List.of("CASUAL", "CASUAL"), null, null, List.of(
						new CoverageChoice("COVER_SHOULDERS", PreferenceStrength.PREFERENCE),
						new CoverageChoice("COVER_SHOULDERS", PreferenceStrength.REQUIRED)))));
		assertEquals("Choose each option only once", exception.getFieldErrors().get("preferences.styles"));
		assertEquals("Choose each option only once", exception.getFieldErrors().get("preferences.coverage"));
	}

	@Test
	void rejectsContradictoryColorsAndMissingCoverageStrength() {
		var exception = assertThrows(ProfileValidationException.class, () -> catalog.prepare(
				preferences(null, List.of("NAVY"), List.of("NAVY"), List.of(new CoverageChoice("COVER_KNEES", null)))));
		assertTrue(exception.getFieldErrors().containsKey("preferences.dislikedColors"));
		assertTrue(exception.getFieldErrors().containsKey("preferences.coverage"));
	}

	@Test
	void storesOnlyExplicitCoverageRestrictionsAndKeepsOtherChoicesPreferred() {
		var prepared = catalog.prepare(preferences(List.of("CASUAL"), null, List.of("RED"),
				List.of(new CoverageChoice("COVER_KNEES", PreferenceStrength.REQUIRED))));
		assertEquals(1, prepared.choices().stream().filter(choice -> choice.getStrength() == PreferenceStrength.REQUIRED).count());
		assertTrue(prepared.choices().stream().filter(choice -> choice.getCategory() != PreferenceCategory.COVERAGE)
				.allMatch(choice -> choice.getStrength() == PreferenceStrength.PREFERENCE));
	}

	private ProfilePreferences preferences(List<String> styles, List<String> preferred, List<String> disliked,
			List<CoverageChoice> coverage) {
		return new ProfilePreferences(styles, preferred, disliked, null, null, null, coverage, null, null, null);
	}
}
