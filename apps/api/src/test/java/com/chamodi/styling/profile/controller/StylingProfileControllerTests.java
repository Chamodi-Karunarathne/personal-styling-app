package com.chamodi.styling.profile.controller;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.chamodi.styling.auth.config.PasswordEncoderConfig;
import com.chamodi.styling.auth.config.SecurityConfig;
import com.chamodi.styling.auth.controller.AuthController;
import com.chamodi.styling.auth.repository.UserRepository;
import com.chamodi.styling.auth.security.AccountPrincipal;
import com.chamodi.styling.auth.security.ApiSecurityErrorHandler;
import com.chamodi.styling.auth.security.RepositoryUserDetailsService;
import com.chamodi.styling.auth.service.AuthService;
import com.chamodi.styling.common.exception.GlobalExceptionHandler;
import com.chamodi.styling.profile.dto.ProfilePreferences;
import com.chamodi.styling.profile.dto.ProfileResponse;
import com.chamodi.styling.profile.dto.ProfileWriteRequest;
import com.chamodi.styling.profile.exception.ProfileConflictException;
import com.chamodi.styling.profile.exception.ProfileValidationException;
import com.chamodi.styling.profile.model.OnboardingStatus;
import com.chamodi.styling.profile.model.StepCode;
import com.chamodi.styling.profile.model.WriteIntent;
import com.chamodi.styling.profile.service.PreferenceCatalog;
import com.chamodi.styling.profile.service.StylingProfileService;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@WebMvcTest({ StylingProfileController.class, AuthController.class })
@Import({ SecurityConfig.class, PasswordEncoderConfig.class, RepositoryUserDetailsService.class,
		ApiSecurityErrorHandler.class, GlobalExceptionHandler.class })
@ExtendWith(OutputCaptureExtension.class)
class StylingProfileControllerTests {

	private static final String PROFILE = "/api/profile/me";
	private static final String OPTIONS = "/api/profile/options";
	private static final String BODY = """
			{"version":null,"intent":"SAVE","lastVisitedStep":"STYLE","preferences":{
			"styles":null,"preferredColors":null,"dislikedColors":null,"fits":null,
			"footwear":null,"comfort":null,"coverage":null,"commonOccasions":null,
			"activities":null,"transportModes":null}}
			""";

	@Autowired private MockMvc mvc;
	@Autowired private JsonMapper mapper;
	@MockitoBean private StylingProfileService profiles;
	@MockitoBean private UserRepository users;
	@MockitoBean private AuthService auth;

	@ParameterizedTest
	@ValueSource(strings = { PROFILE, OPTIONS })
	void anonymousProfileReadsReturnSafe401WithoutCallingService(String path) throws Exception {
		mvc.perform(get(path))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Authentication is required"))
				.andExpect(jsonPath("$.path").value(path))
				.andExpect(header().doesNotExist(HttpHeaders.LOCATION));
		verifyNoInteractions(profiles);
	}

	@Test
	void anonymousPutWithValidCsrfStillRequiresAuthentication() throws Exception {
		mvc.perform(withCsrf(put(PROFILE)).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication is required"));
		verifyNoInteractions(profiles);
	}

	@Test
	void anonymousPutWithoutCsrfIsRejectedBeforeAuthentication() throws Exception {
		mvc.perform(put(PROFILE).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Invalid or missing CSRF token"));
		verifyNoInteractions(profiles);
	}

	@Test
	void authenticatedCatalogExposesApprovedGroupsAndColorMetadata() throws Exception {
		when(profiles.options()).thenReturn(new PreferenceCatalog().response());
		mvc.perform(authenticated(get(OPTIONS)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.groups", hasSize(10)))
				.andExpect(jsonPath("$.groups[0].key").value("styles"))
				.andExpect(jsonPath("$.groups[0].options[0].code").value("CASUAL"))
				.andExpect(jsonPath("$.groups[0].options[0].swatch").doesNotExist())
				.andExpect(jsonPath("$.groups[1].options[5].swatch").value("#26354e"))
				.andExpect(jsonPath("$.groups[6].allowsRequired").value(true))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
		verify(profiles).options();
	}

	@Test
	void missingProfileReturnsVirtualNotStartedWithoutAnOwnerSelector() throws Exception {
		when(profiles.get(7L)).thenReturn(ProfileResponse.notStarted());
		mvc.perform(authenticated(get(PROFILE).param("userId", "8")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", aMapWithSize(7)))
				.andExpect(jsonPath("$.appearanceStepPending").value(false))
				.andExpect(jsonPath("$.version").value(nullValue()))
				.andExpect(jsonPath("$.onboardingStatus").value("NOT_STARTED"))
				.andExpect(jsonPath("$.lastVisitedStep").value("STYLE"))
				.andExpect(jsonPath("$.preferences", aMapWithSize(10)))
				.andExpect(jsonPath("$.preferences.styles").value(nullValue()))
				.andExpect(jsonPath("$.updatedAt").value(nullValue()))
				.andExpect(jsonPath("$.userId").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
		verify(profiles).get(7L);
		verify(profiles, never()).get(8L);
	}

	@Test
	void validPutUsesPrincipalOwnershipRealCsrfAndSafeResponse() throws Exception {
		var response = new ProfileResponse(0L, OnboardingStatus.IN_PROGRESS, StepCode.STYLE,
				ProfilePreferences.unanswered(), null, Instant.parse("2026-10-16T00:00:00Z"));
		when(profiles.save(eq(7L), any())).thenReturn(response);
		mvc.perform(authenticated(withCsrf(put(PROFILE))).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", aMapWithSize(7)))
				.andExpect(jsonPath("$.version").value(0))
				.andExpect(jsonPath("$.onboardingStatus").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.preferences.styles").value(nullValue()))
				.andExpect(jsonPath("$.userId").doesNotExist())
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
		verify(profiles).save(7L, new ProfileWriteRequest(null, WriteIntent.SAVE, StepCode.STYLE, ProfilePreferences.unanswered()));
	}

	@Test
	void missingCsrfDoesNotReachProfileService() throws Exception {
		mvc.perform(authenticated(put(PROFILE)).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Invalid or missing CSRF token"));
		verifyNoInteractions(profiles);
	}

	@Test
	void mismatchedCsrfCookieAndHeaderDoNotReachProfileService() throws Exception {
		Csrf token = csrf();
		mvc.perform(authenticated(put(PROFILE)).cookie(token.cookie()).header(token.header(), "not-the-cookie-token")
				.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Invalid or missing CSRF token"));
		verifyNoInteractions(profiles);
	}

	@Test
	void negativeVersionReturnsFieldValidation400BeforeService() throws Exception {
		assertFieldError(BODY.replace("\"version\":null", "\"version\":-1"), "version", "Version must be nonnegative");
	}

	@Test
	void missingIntentReturnsFieldValidation400BeforeService() throws Exception {
		assertFieldError(BODY.replace("\"intent\":\"SAVE\"", "\"intent\":null"), "intent", "Choose a save action");
	}

	@Test
	void missingStepReturnsFieldValidation400BeforeService() throws Exception {
		assertFieldError(BODY.replace("\"lastVisitedStep\":\"STYLE\"", "\"lastVisitedStep\":null"),
				"lastVisitedStep", "Choose a valid step");
	}

	@Test
	void missingPreferenceSnapshotReturnsFieldValidation400BeforeService() throws Exception {
		ObjectNode body = (ObjectNode) mapper.readTree(BODY);
		body.putNull("preferences");
		assertFieldError(body.toString(), "preferences", "Preferences are required");
	}

	@ParameterizedTest
	@ValueSource(strings = { "styles", "preferredColors", "dislikedColors", "fits", "footwear", "comfort",
			"coverage", "commonOccasions", "activities", "transportModes" })
	void missingSnapshotGroupCannotSilentlyClearAnswers(String field) throws Exception {
		ObjectNode body = (ObjectNode) mapper.readTree(BODY);
		((ObjectNode) body.get("preferences")).remove(field);
		assertMalformed(body.toString());
	}

	@Test
	void missingVersionCannotSilentlyCreateProfile() throws Exception {
		ObjectNode body = (ObjectNode) mapper.readTree(BODY);
		body.remove("version");
		assertMalformed(body.toString());
	}

	@Test
	void unknownIntentIsRejectedAtHttpBoundary() throws Exception {
		assertMalformed(BODY.replace("\"SAVE\"", "\"UNSUPPORTED\""));
	}

	@Test
	void unknownStepIsRejectedAtHttpBoundary() throws Exception {
		assertMalformed(BODY.replace("\"STYLE\"", "\"UNSUPPORTED\""));
	}

	@Test
	void unknownOwnerFieldCannotSelectAnotherAccount() throws Exception {
		ObjectNode body = (ObjectNode) mapper.readTree(BODY);
		body.put("userId", 8);
		assertMalformed(body.toString());
	}

	@Test
	void unknownPreferenceCategoryIsRejectedWithBootMapperConfiguration() throws Exception {
		ObjectNode body = (ObjectNode) mapper.readTree(BODY);
		((ObjectNode) body.get("preferences")).putArray("unknownCategory").add("UNSUPPORTED");
		assertMalformed(body.toString());
	}

	@Test
	void coverageRequiresExplicitKnownStrength() throws Exception {
		ObjectNode body = (ObjectNode) mapper.readTree(BODY);
		((ObjectNode) body.get("preferences")).putArray("coverage").addObject().put("code", "COVER_KNEES");
		mvc.perform(authenticated(withCsrf(put(PROFILE))).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors['preferences.coverage[0].strength']").value("Choose Preferred or Required"));
		verifyNoInteractions(profiles);
	}

	@Test
	void serviceCatalogValidationUsesExistingSafeFieldErrorFormat() throws Exception {
		when(profiles.save(eq(7L), any())).thenThrow(new ProfileValidationException(
				Map.of("preferences.dislikedColors", "A color cannot be both preferred and disliked")));
		mvc.perform(authenticated(withCsrf(put(PROFILE))).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$", aMapWithSize(6)))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors['preferences.dislikedColors']").value("A color cannot be both preferred and disliked"))
				.andExpect(jsonPath("$.stackTrace").doesNotExist());
	}

	@Test
	void genuineVersionConflictReturns409WithoutDuplicateEmailMessage() throws Exception {
		when(profiles.save(eq(7L), any())).thenThrow(new ProfileConflictException());
		mvc.perform(authenticated(withCsrf(put(PROFILE))).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$", aMapWithSize(6)))
				.andExpect(jsonPath("$.message").value("Your preferences changed elsewhere. Reload before saving again"))
				.andExpect(jsonPath("$.fieldErrors", aMapWithSize(0)))
				.andExpect(jsonPath("$.stackTrace").doesNotExist())
				.andExpect(jsonPath("$.exception").doesNotExist());
	}

	@Test
	void unrelatedDatabaseFailuresStaySafe500AndAreNotConflicts() throws Exception {
		when(profiles.save(eq(7L), any())).thenThrow(new DataIntegrityViolationException("private-database-details"));
		String response = mvc.perform(authenticated(withCsrf(put(PROFILE))).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.message").value("An unexpected error occurred"))
				.andExpect(jsonPath("$.stackTrace").doesNotExist())
				.andReturn().getResponse().getContentAsString();
		assertFalse(response.contains("private-database-details"));
	}

	@Test
	void supportedPutPreflightAllowsOnlyTheConfiguredCredentialedOrigin() throws Exception {
		mvc.perform(options(PROFILE).header(HttpHeaders.ORIGIN, "http://localhost:3000")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-xsrf-token"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PUT")));
		verifyNoInteractions(profiles);
	}

	@Test
	void untrustedOriginCannotPreflightProfileWrites() throws Exception {
		mvc.perform(options(PROFILE).header(HttpHeaders.ORIGIN, "https://untrusted.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		verifyNoInteractions(profiles);
	}

	@Test
	void submittedPreferenceValuesAreNotWrittenToLogs(CapturedOutput output) throws Exception {
		when(profiles.save(eq(7L), any())).thenReturn(new ProfileResponse(0L, OnboardingStatus.IN_PROGRESS,
				StepCode.STYLE, ProfilePreferences.unanswered(), null, Instant.now()));
		mvc.perform(authenticated(withCsrf(put(PROFILE))).contentType(MediaType.APPLICATION_JSON)
				.content(BODY.replace("\"styles\":null", "\"styles\":[\"STREETWEAR\"]")))
				.andExpect(status().isOk());
		assertFalse(output.getAll().contains("STREETWEAR"));
	}

	private void assertFieldError(String body, String field, String message) throws Exception {
		mvc.perform(authenticated(withCsrf(put(PROFILE))).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors['" + field + "']").value(message));
		verifyNoInteractions(profiles);
	}

	private void assertMalformed(String body) throws Exception {
		mvc.perform(authenticated(withCsrf(put(PROFILE))).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request body must be valid JSON"))
				.andExpect(jsonPath("$.stackTrace").doesNotExist());
		verifyNoInteractions(profiles);
	}

	private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
		AccountPrincipal principal = new AccountPrincipal(7L, "Profile User", "profile@example.com", null);
		return request.with(authentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of())));
	}

	private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception {
		Csrf token = csrf();
		return request.cookie(token.cookie()).header(token.header(), token.value());
	}

	private Csrf csrf() throws Exception {
		var response = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
		var json = mapper.readTree(response.getContentAsString());
		return new Csrf(response.getCookie("XSRF-TOKEN"), json.get("headerName").asString(), json.get("token").asString());
	}

	private record Csrf(Cookie cookie, String header, String value) {
		@Override public String toString() { return "Csrf[REDACTED]"; }
	}
}
