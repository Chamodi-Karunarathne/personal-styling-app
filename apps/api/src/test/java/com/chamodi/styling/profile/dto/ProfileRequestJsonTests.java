package com.chamodi.styling.profile.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class ProfileRequestJsonTests {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private static final String EMPTY = """
        {"version":null,"intent":"SAVE","lastVisitedStep":"STYLE","preferences":{
          "styles":null,"preferredColors":null,"dislikedColors":null,"fits":null,
          "footwear":null,"comfort":null,"coverage":null,"commonOccasions":null,
          "activities":null,"transportModes":null}}
        """;

    @Test
    void explicitNullAnswersAndVersionAreValidAndDifferentFromNoPreference() {
        ProfileWriteRequest unanswered = mapper.readValue(EMPTY, ProfileWriteRequest.class);
        ProfileWriteRequest noPreference = mapper.readValue(EMPTY.replace("\"styles\":null", "\"styles\":[]"), ProfileWriteRequest.class);
        assertNull(unanswered.version());
        assertNull(unanswered.preferences().styles());
        assertEquals(0, noPreference.preferences().styles().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"styles", "preferredColors", "dislikedColors", "fits", "footwear", "comfort", "coverage", "commonOccasions", "activities", "transportModes"})
    void missingPreferenceGroupsCannotSilentlyClearStoredAnswers(String field) {
        ObjectNode json = (ObjectNode) mapper.readTree(EMPTY);
        ((ObjectNode) json.get("preferences")).remove(field);
        assertThrows(JacksonException.class, () -> mapper.readValue(json.toString(), ProfileWriteRequest.class));
    }

    @Test
    void missingVersionCannotAccidentallyBecomeFirstCreation() {
        ObjectNode json = (ObjectNode) mapper.readTree(EMPTY);
        json.remove("version");
        assertThrows(JacksonException.class, () -> mapper.readValue(json.toString(), ProfileWriteRequest.class));
    }

    @Test
    void unknownWorkflowIntentCannotReachTheService() {
        assertThrows(JacksonException.class, () -> mapper.readValue(EMPTY.replace("\"SAVE\"", "\"UNSUPPORTED\""), ProfileWriteRequest.class));
    }

    @Test
    void ownershipCannotBeSuppliedInTheRequest() {
        ObjectNode json = (ObjectNode) mapper.readTree(EMPTY);
        json.put("ownerId", 99);
        assertThrows(JacksonException.class, () -> mapper.readValue(json.toString(), ProfileWriteRequest.class));
    }

    @Test
    void unsupportedCategoriesCannotBeSilentlyIgnored() {
        ObjectNode json = (ObjectNode) mapper.readTree(EMPTY);
        ((ObjectNode) json.get("preferences")).put("unknownCategory", "CASUAL");
        assertThrows(JacksonException.class, () -> mapper.readValue(json.toString(), ProfileWriteRequest.class));
    }

    @Test
    void requestStringRepresentationDoesNotRevealPreferenceValues() {
        ProfileWriteRequest request = mapper.readValue(EMPTY.replace("\"styles\":null", "\"styles\":[\"CASUAL\"]"), ProfileWriteRequest.class);
        assertFalse(request.toString().contains("CASUAL"));
    }
}
