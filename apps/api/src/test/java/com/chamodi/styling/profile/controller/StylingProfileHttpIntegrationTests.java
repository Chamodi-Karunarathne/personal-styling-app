package com.chamodi.styling.profile.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.chamodi.styling.auth.dto.LoginRequest;
import com.chamodi.styling.auth.dto.RegisterRequest;
import com.chamodi.styling.profile.dto.CoverageChoice;
import com.chamodi.styling.profile.dto.ProfilePreferences;
import com.chamodi.styling.profile.dto.ProfileWriteRequest;
import com.chamodi.styling.profile.model.PreferenceStrength;
import com.chamodi.styling.profile.model.StepCode;
import com.chamodi.styling.profile.model.WriteIntent;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Real HTTP, sessions, validation, service, and PostgreSQL; never use a developer/Neon database.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "spring.jpa.show-sql=false", "logging.level.org.springframework.web=INFO" })
@EnabledIfEnvironmentVariable(named = "PROFILE_INTEGRATION_TESTS", matches = "true")
class StylingProfileHttpIntegrationTests {
    @LocalServerPort private int port;
    @Autowired private JsonMapper mapper;

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry properties) {
        String url = System.getenv("PROFILE_TEST_DB_URL");
        if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/phase2_test")) {
            throw new IllegalStateException("HTTP integration requires a dedicated loopback phase2_test database");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> required("PROFILE_TEST_DB_USERNAME"));
        properties.add("spring.datasource.password", () -> required("PROFILE_TEST_DB_PASSWORD"));
    }
    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing isolated test setting: " + name);
        return value;
    }

    @Test
    void realSessionsIsolateOwnersAndRejectedSnapshotsNeverReplaceSavedChoices() throws Exception {
        Browser first = signedIn(); Browser second = signedIn();
        var empty = first.getProfile();
        assertTrue(empty.get("version").isNull());
        assertEquals("NOT_STARTED", empty.get("onboardingStatus").asString());
        var preferences = new ProfilePreferences(List.of(), null, null, null, null, null,
                List.of(new CoverageChoice("COVER_KNEES", PreferenceStrength.REQUIRED)), null, null, null);
        var save = first.put(null, WriteIntent.SAVE, preferences);
        assertEquals(200, save.statusCode());
        var saved = mapper.readTree(save.body());
        assertEquals(0, saved.get("preferences").get("styles").size());
        assertTrue(saved.get("preferences").get("preferredColors").isNull());
        assertEquals("REQUIRED", first.getProfile().get("preferences").get("coverage").get(0).get("strength").asString());
        assertEquals("NOT_STARTED", second.getProfile().get("onboardingStatus").asString());
        var stale = first.put(null, WriteIntent.COMPLETE, ProfilePreferences.unanswered());
        assertEquals(409, stale.statusCode());
        assertFalse(stale.body().contains("SQL"));
        var invalid = new ProfilePreferences(List.of("MINIMAL"), List.of("NAVY"), List.of("NAVY"), null, null, null, null, null, null, null);
        assertEquals(400, first.put(saved.get("version").asLong(), WriteIntent.COMPLETE, invalid).statusCode());
        assertEquals(saved, first.getProfile());
        assertEquals(401, new Browser().send("GET", "/api/profile/me", null, false).statusCode());
    }

    @Test
    void realSkipResumeCompleteAndEditsPreserveCompletionAndVersioning() throws Exception {
        Browser browser = signedIn();
        var skipped = mapper.readTree(browser.put(null, WriteIntent.SKIP, ProfilePreferences.unanswered()).body());
        assertEquals("SKIPPED", skipped.get("onboardingStatus").asString());
        var draft = mapper.readTree(browser.put(skipped.get("version").asLong(), WriteIntent.SAVE, ProfilePreferences.unanswered()).body());
        assertEquals("IN_PROGRESS", draft.get("onboardingStatus").asString());
        var completed = mapper.readTree(browser.put(draft.get("version").asLong(), WriteIntent.COMPLETE, ProfilePreferences.unanswered()).body());
        assertEquals("COMPLETED", completed.get("onboardingStatus").asString());
        var edit = browser.put(completed.get("version").asLong(), WriteIntent.SAVE, ProfilePreferences.unanswered());
        assertEquals(200, edit.statusCode());
        var after = browser.getProfile();
        assertEquals(completed.get("completedAt"), after.get("completedAt"));
        assertTrue(after.get("version").asLong() > completed.get("version").asLong());
        assertEquals(400, browser.put(after.get("version").asLong(), WriteIntent.SKIP, ProfilePreferences.unanswered()).statusCode());
    }

    @Test
    void realAppearanceLifecycleUsesOwnerSessionsAndKeepsStylingAndIdentitySeparate() throws Exception {
        Browser first = signedIn(); Browser second = signedIn();
        var style = first.getProfile();
        var empty = mapper.readTree(first.send("GET", "/api/profile/appearance", null, false).body());
        assertTrue(empty.get("version").isNull());
        Map<String, Object> details = new LinkedHashMap<>();
        List.of("heightCm", "garmentSizeNote", "bodyProportions", "skinTone", "skinUndertone",
                "paletteSuggestions", "hairColor", "hairTexture", "hairLength", "faceShape")
                .forEach(key -> details.put(key, null));
        details.put("heightCm", 172); details.put("paletteSuggestions", false);
        Map<String, Object> write = new LinkedHashMap<>(); write.put("version", null); write.put("details", details);
        assertEquals(403, first.send("PUT", "/api/profile/appearance", write, false).statusCode());
        var created = first.send("PUT", "/api/profile/appearance", write, true);
        assertEquals(200, created.statusCode());
        var saved = mapper.readTree(created.body());
        String version = saved.get("version").asString();
        assertFalse(saved.get("details").get("paletteSuggestions").asBoolean());
        assertEquals(172, saved.get("details").get("heightCm").asInt());
        assertTrue(mapper.readTree(second.send("GET", "/api/profile/appearance?userId=1", null, false).body()).get("version").isNull());
        assertEquals(204, second.send("DELETE", "/api/profile/appearance", Map.of("version", version), true).statusCode());
        assertEquals(saved, mapper.readTree(first.send("GET", "/api/profile/appearance", null, false).body()));
        assertEquals(409, first.send("PUT", "/api/profile/appearance", write, true).statusCode());
        details.put("heightCm", null); write.put("version", version);
        var cleared = first.send("PUT", "/api/profile/appearance", write, true);
        assertEquals(200, cleared.statusCode());
        var latest = mapper.readTree(cleared.body()); assertTrue(latest.get("details").get("heightCm").isNull());
        var delete = Map.of("version", latest.get("version").asString());
        assertEquals(403, first.send("DELETE", "/api/profile/appearance", delete, false).statusCode());
        assertEquals(204, first.send("DELETE", "/api/profile/appearance", delete, true).statusCode());
        assertEquals(204, first.send("DELETE", "/api/profile/appearance", delete, true).statusCode());
        assertTrue(mapper.readTree(first.send("GET", "/api/profile/appearance", null, false).body()).get("version").isNull());
        assertEquals(style, first.getProfile());
        var identity = mapper.readTree(first.send("GET", "/api/auth/me", null, false).body());
        assertEquals(3, identity.size()); assertFalse(identity.has("appearance")); assertFalse(identity.has("passwordHash"));
    }

    @Test
    void realHttpRequiresCsrfAndKeepsExactCredentialedPutCorsPolicy() throws Exception {
        Browser browser = signedIn();
        var input = new ProfileWriteRequest(null, WriteIntent.SAVE, StepCode.STYLE, ProfilePreferences.unanswered());
        assertEquals(403, browser.send("PUT", "/api/profile/me", input, false).statusCode());
        assertEquals("NOT_STARTED", browser.getProfile().get("onboardingStatus").asString());
        var allowed = browser.client.send(HttpRequest.newBuilder(uri("/api/profile/me"))
                .header("Origin", "http://localhost:3000").header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "content-type,x-xsrf-token")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, allowed.statusCode());
        assertEquals("http://localhost:3000", allowed.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        assertEquals("true", allowed.headers().firstValue("Access-Control-Allow-Credentials").orElseThrow());
        assertTrue(allowed.headers().firstValue("Access-Control-Allow-Methods").orElseThrow().contains("PUT"));
    }

    private Browser signedIn() throws Exception {
        Browser browser = new Browser();
        String email = "profile-http-" + UUID.randomUUID() + "@example.com";
        String password = "SyntheticHttpTest123";
        assertEquals(201, browser.send("POST", "/api/auth/register", new RegisterRequest("Profile HTTP Test", email, password), true).statusCode());
        assertEquals(200, browser.send("POST", "/api/auth/login", new LoginRequest(email, password), true).statusCode());
        return browser;
    }
    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
    private class Browser {
        private final HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(10)).build();
        private JsonNode getProfile() throws Exception {
            var response = send("GET", "/api/profile/me", null, false);
            assertEquals(200, response.statusCode()); return mapper.readTree(response.body());
        }
        private HttpResponse<String> put(Long version, WriteIntent intent, ProfilePreferences preferences) throws Exception {
            return send("PUT", "/api/profile/me", new ProfileWriteRequest(version, intent, StepCode.REVIEW, preferences), true);
        }
        private HttpResponse<String> send(String method, String path, Object body, boolean csrf) throws Exception {
            var request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15));
            if (csrf) {
                var bootstrap = client.send(HttpRequest.newBuilder(uri("/api/auth/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, bootstrap.statusCode());
                var token = mapper.readTree(bootstrap.body());
                request.header(token.get("headerName").asString(), token.get("token").asString());
            }
            if (body != null) request.header("Content-Type", "application/json");
            return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
        }
        @Override public String toString() { return "Browser[REDACTED]"; }
    }
}
