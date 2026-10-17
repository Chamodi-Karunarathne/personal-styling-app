package com.chamodi.styling.profile.appearance;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.chamodi.styling.profile.appearance.dto.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class AppearanceRequestJsonTests {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private String empty() { return mapper.writeValueAsString(new AppearanceWriteRequest(null, AppearanceDetails.empty())); }
    @Test void allNullFieldsAndFalseHaveDistinctMeanings() {
        assertEquals(AppearanceDetails.empty(), mapper.readValue(empty(), AppearanceWriteRequest.class).details());
        assertEquals(false, mapper.readValue(empty().replace("\"paletteSuggestions\":null", "\"paletteSuggestions\":false"), AppearanceWriteRequest.class).details().paletteSuggestions());
    }
    @ParameterizedTest @ValueSource(strings = { "heightCm", "garmentSizeNote", "bodyProportions", "skinTone", "skinUndertone", "paletteSuggestions", "hairColor", "hairTexture", "hairLength", "faceShape" })
    void omittedNullableFieldCannotSilentlyClearSavedDetails(String field) {
        var json = (ObjectNode) mapper.readTree(empty()); ((ObjectNode) json.get("details")).remove(field);
        assertThrows(JacksonException.class, () -> mapper.readValue(json.toString(), AppearanceWriteRequest.class));
    }
    @ParameterizedTest @ValueSource(strings = { "ownerId", "userId", "weight", "dateOfBirth", "age", "photo" })
    void unsupportedOrSensitiveFieldsCannotBeAccepted(String field) {
        var json = (ObjectNode) mapper.readTree(empty()); ((ObjectNode) json.get("details")).put(field, "unsupported");
        assertThrows(JacksonException.class, () -> mapper.readValue(json.toString(), AppearanceWriteRequest.class));
        var topLevel = (ObjectNode) mapper.readTree(empty()); topLevel.put(field, "unsupported"); final String body = topLevel.toString();
        assertThrows(JacksonException.class, () -> mapper.readValue(body, AppearanceWriteRequest.class));
    }
    @Test void missingVersionIsRejectedForWritesAndDeletes() {
        var json = (ObjectNode) mapper.readTree(empty()); json.remove("version");
        assertThrows(JacksonException.class, () -> mapper.readValue(json.toString(), AppearanceWriteRequest.class));
        assertThrows(JacksonException.class, () -> mapper.readValue("{}", AppearanceDeleteRequest.class));
    }
}
