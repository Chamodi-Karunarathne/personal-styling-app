package com.chamodi.styling.profile.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.chamodi.styling.profile.dto.CoverageChoice;
import com.chamodi.styling.profile.dto.PreferenceCatalogResponse;
import com.chamodi.styling.profile.dto.PreferenceCatalogResponse.Group;
import com.chamodi.styling.profile.dto.PreferenceCatalogResponse.Option;
import com.chamodi.styling.profile.dto.ProfilePreferences;
import com.chamodi.styling.profile.entity.PreferenceSelection;
import com.chamodi.styling.profile.entity.StylingProfile;
import com.chamodi.styling.profile.exception.ProfileValidationException;
import com.chamodi.styling.profile.model.PreferenceCategory;
import com.chamodi.styling.profile.model.PreferenceStrength;

@Component
public class PreferenceCatalog {

	private final PreferenceCatalogResponse response;
	private final Map<PreferenceCategory, Group> groups = new EnumMap<>(PreferenceCategory.class);

	public PreferenceCatalog() {
		List<Option> colors = List.of(
				new Option("BLACK", "Black", "#262323"), new Option("WHITE", "White", "#ffffff"),
				new Option("GREY", "Grey", "#858282"), new Option("BEIGE", "Beige", "#d8c5ac"),
				new Option("BROWN", "Brown", "#77533d"), new Option("NAVY", "Navy", "#26354e"),
				new Option("BLUE", "Blue", "#527fb5"), new Option("GREEN", "Green", "#59755a"),
				new Option("RED", "Red", "#a73742"), new Option("PINK", "Pink", "#d49aa9"),
				new Option("PURPLE", "Purple", "#8b689b"), new Option("YELLOW", "Yellow", "#e5c563"),
				new Option("ORANGE", "Orange", "#cc8351"));
		add(PreferenceCategory.STYLES, "Clothing styles", false, List.of(
				new Option("CASUAL", "Casual"), new Option("MINIMAL", "Minimal"),
				new Option("SMART_CASUAL", "Smart casual"), new Option("FORMAL", "Formal"),
				new Option("SPORTY", "Sporty"), new Option("STREETWEAR", "Streetwear"), new Option("VINTAGE", "Vintage")));
		add(PreferenceCategory.PREFERRED_COLORS, "Preferred colors", false, colors);
		add(PreferenceCategory.DISLIKED_COLORS, "Disliked colors", false, colors);
		add(PreferenceCategory.FITS, "Fit preferences", false, List.of(
				new Option("RELAXED", "Relaxed"), new Option("REGULAR", "Regular"), new Option("CLOSE_FITTING", "Close-fitting")));
		add(PreferenceCategory.FOOTWEAR, "Footwear preferences", false, List.of(
				new Option("TRAINERS", "Trainers"), new Option("FLATS", "Flats"), new Option("SANDALS", "Sandals"),
				new Option("BOOTS", "Boots"), new Option("HEELS", "Heels")));
		add(PreferenceCategory.COMFORT, "Comfort priorities", false, List.of(
				new Option("BREATHABLE", "Lightweight / breathable"), new Option("EASY_MOVEMENT", "Easy movement"),
				new Option("SOFT_FABRICS", "Soft fabrics")));
		add(PreferenceCategory.COVERAGE, "Coverage preferences (optional)", true, List.of(
				new Option("COVER_SHOULDERS", "Covered shoulders"), new Option("COVER_KNEES", "Covered knees"),
				new Option("LONGER_SLEEVES", "Longer sleeves")));
		add(PreferenceCategory.COMMON_OCCASIONS, "Typical occasions", false, List.of(
				new Option("WORK_STUDY", "Work / study"), new Option("CASUAL_OUTINGS", "Casual outings"),
				new Option("FORMAL_EVENTS", "Formal events"), new Option("ACTIVE_OUTDOOR", "Active / outdoor activities")));
		add(PreferenceCategory.ACTIVITIES, "Typical activities", false, List.of(
				new Option("MOSTLY_SEATED", "Mostly seated"), new Option("WALKING", "Walking"), new Option("OUTDOOR_TIME", "Outdoor time")));
		add(PreferenceCategory.TRANSPORT_MODES, "Common transportation", false, List.of(
				new Option("WALKING", "Walking"), new Option("PUBLIC_TRANSPORT", "Public transport"),
				new Option("CAR", "Car"), new Option("CYCLING", "Cycling")));
		response = new PreferenceCatalogResponse(List.copyOf(groups.values()));
	}

	private void add(PreferenceCategory category, String label, boolean required, List<Option> options) {
		groups.put(category, new Group(category.key(), label, required, options));
	}

	public PreferenceCatalogResponse response() {
		return response;
	}

	public PreparedPreferences prepare(ProfilePreferences preferences) {
		Map<String, String> errors = new LinkedHashMap<>();
		Set<PreferenceCategory> answered = new LinkedHashSet<>();
		Set<PreferenceSelection> selections = new LinkedHashSet<>();
		for (PreferenceCategory category : PreferenceCategory.values()) {
			List<CoverageChoice> choices = choices(preferences, category);
			if (choices == null) {
				continue;
			}
			answered.add(category);
			Set<String> codes = new LinkedHashSet<>();
			Group group = groups.get(category);
			String field = "preferences." + category.key();
			for (CoverageChoice choice : choices) {
				if (choice == null || group.options().stream().noneMatch(option -> option.code().equals(choice.code()))) {
					errors.putIfAbsent(field, "Choose a supported option");
					continue;
				}
				if (!codes.add(choice.code())) {
					errors.putIfAbsent(field, "Choose each option only once");
				}
				if (choice.strength() == null || (!group.allowsRequired() && choice.strength() != PreferenceStrength.PREFERENCE)) {
					errors.putIfAbsent(field, "Choose Preferred or Required");
					continue;
				}
				selections.add(new PreferenceSelection(category, choice.code(), choice.strength()));
			}
		}
		if (preferences.preferredColors() != null && preferences.dislikedColors() != null
				&& preferences.preferredColors().stream().anyMatch(preferences.dislikedColors()::contains)) {
			errors.put("preferences.dislikedColors", "A color cannot be both preferred and disliked");
		}
		if (!errors.isEmpty()) {
			throw new ProfileValidationException(errors);
		}
		return new PreparedPreferences(answered, selections);
	}

	private List<CoverageChoice> choices(ProfilePreferences preferences, PreferenceCategory category) {
		if (category == PreferenceCategory.COVERAGE) {
			return preferences.coverage();
		}
		List<String> values = switch (category) {
			case STYLES -> preferences.styles();
			case PREFERRED_COLORS -> preferences.preferredColors();
			case DISLIKED_COLORS -> preferences.dislikedColors();
			case FITS -> preferences.fits();
			case FOOTWEAR -> preferences.footwear();
			case COMFORT -> preferences.comfort();
			case COMMON_OCCASIONS -> preferences.commonOccasions();
			case ACTIVITIES -> preferences.activities();
			case TRANSPORT_MODES -> preferences.transportModes();
			case COVERAGE -> throw new IllegalStateException("Coverage has structured choices");
		};
		return values == null ? null : values.stream().map(code -> new CoverageChoice(code, PreferenceStrength.PREFERENCE)).toList();
	}

	public ProfilePreferences preferences(StylingProfile profile) {
		return new ProfilePreferences(
				codes(profile, PreferenceCategory.STYLES), codes(profile, PreferenceCategory.PREFERRED_COLORS),
				codes(profile, PreferenceCategory.DISLIKED_COLORS), codes(profile, PreferenceCategory.FITS),
				codes(profile, PreferenceCategory.FOOTWEAR), codes(profile, PreferenceCategory.COMFORT),
				coverage(profile), codes(profile, PreferenceCategory.COMMON_OCCASIONS),
				codes(profile, PreferenceCategory.ACTIVITIES), codes(profile, PreferenceCategory.TRANSPORT_MODES));
	}

	private List<String> codes(StylingProfile profile, PreferenceCategory category) {
		if (!profile.getAnsweredCategories().contains(category)) {
			return null;
		}
		Set<String> selected = new LinkedHashSet<>();
		profile.getSelections().stream().filter(selection -> selection.getCategory() == category)
				.forEach(selection -> selected.add(selection.getCode()));
		// Return stable catalog order regardless of database collection ordering.
		return groups.get(category).options().stream().map(Option::code).filter(selected::contains).toList();
	}

	private List<CoverageChoice> coverage(StylingProfile profile) {
		List<String> selectedCodes = codes(profile, PreferenceCategory.COVERAGE);
		if (selectedCodes == null) {
			return null;
		}
		List<CoverageChoice> selected = new ArrayList<>();
		for (String code : selectedCodes) {
			profile.getSelections().stream().filter(choice -> choice.getCategory() == PreferenceCategory.COVERAGE
					&& choice.getCode().equals(code)).findFirst()
					.ifPresent(choice -> selected.add(new CoverageChoice(code, choice.getStrength())));
		}
		return List.copyOf(selected);
	}

	public record PreparedPreferences(Set<PreferenceCategory> answered, Set<PreferenceSelection> choices) {
	}
}
