package com.chamodi.styling.profile.appearance.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.chamodi.styling.profile.appearance.dto.AppearanceDetails;
import com.chamodi.styling.profile.appearance.dto.AppearanceResponse.Option;
import com.chamodi.styling.profile.exception.ProfileValidationException;

@Component
public class AppearanceCatalog {
    private final Map<String, List<Option>> options;
    public AppearanceCatalog() {
        Map<String, List<Option>> values = new LinkedHashMap<>();
        values.put("bodyProportions", List.of(new Option("BALANCED", "Similar torso and leg lengths"), new Option("LONGER_TORSO", "Longer torso"), new Option("LONGER_LEGS", "Longer legs"), new Option("UNSURE", "Unsure")));
        values.put("skinTone", List.of(new Option("VERY_LIGHT", "Very light"), new Option("LIGHT", "Light"), new Option("MEDIUM", "Medium"), new Option("DEEP", "Deep"), new Option("VERY_DEEP", "Very deep"), new Option("UNSURE", "Unsure")));
        values.put("skinUndertone", List.of(new Option("WARM", "Warm"), new Option("COOL", "Cool"), new Option("NEUTRAL", "Neutral"), new Option("UNSURE", "Unsure")));
        values.put("hairColor", List.of(new Option("BLACK", "Black"), new Option("BROWN", "Brown"), new Option("BLONDE", "Blonde"), new Option("RED", "Red"), new Option("GREY", "Grey"), new Option("WHITE", "White"), new Option("OTHER", "Another color"), new Option("UNSURE", "Unsure")));
        values.put("hairTexture", List.of(new Option("STRAIGHT", "Straight"), new Option("WAVY", "Wavy"), new Option("CURLY", "Curly"), new Option("COILY", "Coily"), new Option("UNSURE", "Unsure")));
        values.put("hairLength", List.of(new Option("SHAVED", "Shaved / very short"), new Option("SHORT", "Short"), new Option("MEDIUM", "Medium"), new Option("LONG", "Long"), new Option("UNSURE", "Unsure")));
        values.put("faceShape", List.of(new Option("OVAL", "Oval"), new Option("ROUND", "Round"), new Option("SQUARE", "Square"), new Option("OBLONG", "Oblong"), new Option("HEART", "Heart"), new Option("DIAMOND", "Diamond"), new Option("UNSURE", "Unsure"), new Option("NOT_SPECIFIED", "Prefer not to specify")));
        options = Collections.unmodifiableMap(values);
    }
    public Map<String, List<Option>> options() { return options; }
    public void validate(AppearanceDetails details) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("bodyProportions", details.bodyProportions()); values.put("skinTone", details.skinTone());
        values.put("skinUndertone", details.skinUndertone()); values.put("hairColor", details.hairColor());
        values.put("hairTexture", details.hairTexture()); values.put("hairLength", details.hairLength()); values.put("faceShape", details.faceShape());
        Map<String, String> errors = new LinkedHashMap<>();
        values.forEach((key, value) -> { if (value != null && options.get(key).stream().noneMatch(option -> option.code().equals(value))) errors.put("details." + key, "Choose a supported option"); });
        if (!errors.isEmpty()) throw new ProfileValidationException(errors);
    }
}
