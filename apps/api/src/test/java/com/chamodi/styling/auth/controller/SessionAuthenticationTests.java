package com.chamodi.styling.auth.controller;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.chamodi.styling.auth.config.PasswordEncoderConfig;
import com.chamodi.styling.auth.config.SecurityConfig;
import com.chamodi.styling.auth.dto.RegisterRequest;
import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.repository.UserRepository;
import com.chamodi.styling.auth.security.ApiSecurityErrorHandler;
import com.chamodi.styling.auth.security.RepositoryUserDetailsService;
import com.chamodi.styling.auth.service.AuthService;
import com.chamodi.styling.common.exception.GlobalExceptionHandler;

import jakarta.servlet.http.Cookie;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, PasswordEncoderConfig.class, RepositoryUserDetailsService.class,
		ApiSecurityErrorHandler.class, GlobalExceptionHandler.class })
@ExtendWith(OutputCaptureExtension.class)
class SessionAuthenticationTests {

	private static final String LOGIN_PATH = "/api/auth/login";
	private static final String REGISTER_PATH = "/api/auth/register";
	private static final String ME_PATH = "/api/auth/me";
	private static final String LOGOUT_PATH = "/api/auth/logout";
	private static final String CSRF_PATH = "/api/auth/csrf";
	private static final String CSRF_COOKIE = "XSRF-TOKEN";
	private static final String CSRF_HEADER = "X-XSRF-TOKEN";
	private static final String EMAIL = "person@example.com";
	private static final String PASSWORD = "  CorrectHorseBatteryStaple123  ";
	private static final String REGISTER_JSON = """
			{"fullName":"Example User","email":"person@example.com","password":"ExamplePassword123"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private AuthService authService;

	private String passwordHash;

	@BeforeEach
	void setUpPasswordHash() {
		passwordHash = passwordEncoder.encode(PASSWORD);
	}

	@Test
	void bootstrapsBrowserReadableCsrfCookieAndMatchingRawHeaderToken() throws Exception {
		CsrfToken token = csrf(null);

		assertFalse(token.cookie().isHttpOnly());
		assertFalse(token.cookie().getSecure());
		assertEquals("/", token.cookie().getPath());
		assertFalse(token.cookie().getValue().isBlank());
		verifyNoInteractions(userRepository, authService);
	}

	@Test
	void loginVerifiesBcryptAndNormalizesEmailWithoutTrimmingPassword() throws Exception {
		knownAccount();
		MvcResult result = performLogin("  Person@Example.COM  ", PASSWORD, null, csrf(null))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", aMapWithSize(3)))
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.fullName").value("Example User"))
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andReturn();

		assertNotNull(result.getRequest().getSession(false));
		verify(userRepository).findByEmail(EMAIL);
		verifyNoInteractions(authService);
	}

	@Test
	void persistsAuthenticationForMeWithoutRetainingPasswordOrHashInSession() throws Exception {
		MockHttpSession session = authenticatedSession();
		SecurityContext context = (SecurityContext) session
				.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
		assertNotNull(context);
		Authentication authentication = context.getAuthentication();
		assertTrue(authentication.isAuthenticated());
		assertNull(authentication.getCredentials());
		assertNull(((UserDetails) authentication.getPrincipal()).getPassword());
		assertFalse(authentication.getPrincipal().toString().contains(passwordHash));

		mockMvc.perform(get(ME_PATH).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", aMapWithSize(3)))
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void rotatesExistingSessionIdOnSuccessfulLoginAndKeepsAuthentication() throws Exception {
		knownAccount();
		MockHttpSession existingSession = new MockHttpSession();
		String oldId = existingSession.getId();

		MvcResult login = performLogin(EMAIL, PASSWORD, existingSession, csrf(existingSession))
				.andExpect(status().isOk()).andReturn();
		MockHttpSession authenticatedSession = (MockHttpSession) login.getRequest().getSession(false);
		assertNotNull(authenticatedSession);
		assertNotEquals(oldId, authenticatedSession.getId());

		mockMvc.perform(get(ME_PATH).session(authenticatedSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL));
	}

	@Test
	void wrongPasswordReturnsGenericFailureAndDoesNotAuthenticate() throws Exception {
		knownAccount();
		MvcResult failure = assertError(performLogin(EMAIL, PASSWORD.trim(), null, csrf(null)),
				401, "Invalid email or password", LOGIN_PATH).andReturn();

		MockHttpSession session = (MockHttpSession) failure.getRequest().getSession(false);
		if (session != null) {
			assertNull(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
		}
	}

	@Test
	void unknownEmailReturnsSameGenericFailureAsWrongPassword() throws Exception {
		assertError(performLogin("unknown@example.com", PASSWORD, null, csrf(null)),
				401, "Invalid email or password", LOGIN_PATH);
		verify(userRepository).findByEmail("unknown@example.com");
	}

	@Test
	void invalidEmailIsValidatedBeforeAccountLookup() throws Exception {
		performLogin("invalid-email", PASSWORD, null, csrf(null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors.email").value("Email must be valid"));
		verifyNoInteractions(userRepository, authService);
	}

	@Test
	void blankPasswordIsValidatedBeforeAccountLookup() throws Exception {
		performLogin(EMAIL, "   ", null, csrf(null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.password").value("Password is required"));
		verifyNoInteractions(userRepository, authService);
	}

	@Test
	void registrationAcceptsSeventyTwoAsciiPasswordBytes() throws Exception {
		assertRegistrationAcceptsPassword("a".repeat(72));
	}

	@Test
	void registrationAcceptsExactlySeventyTwoUtf8BytesIncludingEmoji() throws Exception {
		assertRegistrationAcceptsPassword("a".repeat(68) + "\uD83D\uDE42");
	}

	@Test
	void loginAcceptsSeventyTwoAsciiPasswordBytesWithExistingBcryptHash() throws Exception {
		assertLoginAcceptsPassword("a".repeat(72));
	}

	@Test
	void loginAcceptsExactlySeventyTwoUtf8BytesIncludingEmojiWithExistingBcryptHash() throws Exception {
		assertLoginAcceptsPassword("a".repeat(68) + "\uD83D\uDE42");
	}

	@Test
	void registrationRejectsSeventyThreeAsciiPasswordBytesBeforeServiceCall() throws Exception {
		assertPasswordByteLimitFailure(REGISTER_PATH, "a".repeat(73));
	}

	@Test
	void registrationRejectsSeventyThreeUtf8BytesDespiteFewerThanSeventyTwoCharacters(CapturedOutput output)
			throws Exception {
		String password = "a".repeat(69) + "\uD83D\uDE42";
		assertPasswordByteLimitFailure(REGISTER_PATH, password);
		assertFalse(output.getAll().contains(password));
	}

	@Test
	void loginRejectsSeventyThreeAsciiPasswordBytesBeforeAccountLookup() throws Exception {
		assertPasswordByteLimitFailure(LOGIN_PATH, "a".repeat(73));
	}

	@Test
	void loginRejectsSuffixBeyondUnicodeBcryptByteBoundaryBeforeAccountLookup(CapturedOutput output) throws Exception {
		String acceptedPassword = "a".repeat(68) + "\uD83D\uDE42";
		String passwordWithSuffix = acceptedPassword + "x";
		assertPasswordByteLimitFailure(LOGIN_PATH, passwordWithSuffix);
		assertFalse(output.getAll().contains(passwordWithSuffix));
	}

	@Test
	void unauthenticatedMeReturnsSafeJsonWithoutLoginRedirect() throws Exception {
		assertError(mockMvc.perform(get(ME_PATH)), 401, "Authentication is required", ME_PATH)
				.andExpect(header().doesNotExist(HttpHeaders.LOCATION));
	}

	@Test
	void meAlwaysReturnsSessionAccountEvenWhenClientSuppliesAnotherId() throws Exception {
		mockMvc.perform(get(ME_PATH).session(authenticatedSession()).param("id", "999"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.email").value(EMAIL));
	}

	@Test
	void loginWithoutCsrfIsRejectedBeforeAccountLookup() throws Exception {
		assertError(mockMvc.perform(post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(EMAIL, PASSWORD))), 403, "Invalid or missing CSRF token", LOGIN_PATH);
		verifyNoInteractions(userRepository, authService);
	}

	@Test
	void loginWithMismatchedCsrfHeaderIsRejected() throws Exception {
		CsrfToken token = csrf(null);
		assertError(mockMvc.perform(post(LOGIN_PATH).cookie(token.cookie())
				.header(CSRF_HEADER, "incorrect-token").contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(EMAIL, PASSWORD))), 403, "Invalid or missing CSRF token", LOGIN_PATH);
		verifyNoInteractions(userRepository, authService);
	}

	@Test
	void csrfCookieAloneCannotAuthorizeLogin() throws Exception {
		CsrfToken token = csrf(null);
		assertError(mockMvc.perform(post(LOGIN_PATH).cookie(token.cookie())
				.contentType(MediaType.APPLICATION_JSON).content(loginJson(EMAIL, PASSWORD))),
				403, "Invalid or missing CSRF token", LOGIN_PATH);
		verifyNoInteractions(userRepository, authService);
	}

	@Test
	void csrfHeaderAloneCannotAuthorizeLogin() throws Exception {
		CsrfToken token = csrf(null);
		assertError(mockMvc.perform(post(LOGIN_PATH).header(CSRF_HEADER, token.cookie().getValue())
				.contentType(MediaType.APPLICATION_JSON).content(loginJson(EMAIL, PASSWORD))),
				403, "Invalid or missing CSRF token", LOGIN_PATH);
		verifyNoInteractions(userRepository, authService);
	}

	@Test
	void registerRemainsPublicButRequiresCsrf() throws Exception {
		assertError(mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content(REGISTER_JSON)),
				403, "Invalid or missing CSRF token", REGISTER_PATH);
		verifyNoInteractions(authService);
	}

	@Test
	void browserCookieAndHeaderPreserveRegistrationCreatedContract() throws Exception {
		User account = account();
		when(authService.register(any(RegisterRequest.class))).thenReturn(account);
		CsrfToken token = csrf(null);
		mockMvc.perform(withCsrf(post(REGISTER_PATH), token).contentType(MediaType.APPLICATION_JSON)
				.content(REGISTER_JSON))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$", aMapWithSize(3)))
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
		verify(authService).register(new RegisterRequest("Example User", EMAIL, "ExamplePassword123"));
	}

	@Test
	void loginClearsOldCsrfCookieAndRequiresFreshHeaderForNewCookie() throws Exception {
		knownAccount();
		CsrfToken oldToken = csrf(null);
		MvcResult login = performLogin(EMAIL, PASSWORD, null, oldToken)
				.andExpect(status().isOk())
				.andExpect(cookie().maxAge(CSRF_COOKIE, 0)).andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		CsrfToken freshToken = csrf(session);
		assertNotEquals(oldToken.cookie().getValue(), freshToken.cookie().getValue());

		assertError(mockMvc.perform(post(LOGOUT_PATH).session(session).cookie(freshToken.cookie())
				.header(CSRF_HEADER, oldToken.cookie().getValue())),
				403, "Invalid or missing CSRF token", LOGOUT_PATH);
		mockMvc.perform(get(ME_PATH).session(session)).andExpect(status().isOk());
	}

	@Test
	void logoutInvalidatesSessionExpiresCookiesAndRemovesAuthentication() throws Exception {
		MockHttpSession session = authenticatedSession();
		CsrfToken token = csrf(session);
		mockMvc.perform(withCsrf(post(LOGOUT_PATH).session(session), token))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""))
				.andExpect(cookie().maxAge("JSESSIONID", 0))
				.andExpect(cookie().httpOnly("JSESSIONID", true))
				.andExpect(cookie().maxAge(CSRF_COOKIE, 0));

		assertTrue(session.isInvalid());
		assertError(mockMvc.perform(get(ME_PATH)), 401, "Authentication is required", ME_PATH);
	}

	@Test
	void logoutRequiresAuthenticationEvenWithValidCsrf() throws Exception {
		assertError(mockMvc.perform(withCsrf(post(LOGOUT_PATH), csrf(null))),
				401, "Authentication is required", LOGOUT_PATH);
	}

	@Test
	void logoutWithoutCsrfDoesNotInvalidateAuthenticatedSession() throws Exception {
		MockHttpSession session = authenticatedSession();
		assertError(mockMvc.perform(post(LOGOUT_PATH).session(session)),
				403, "Invalid or missing CSRF token", LOGOUT_PATH);
		assertFalse(session.isInvalid());
		mockMvc.perform(get(ME_PATH).session(session)).andExpect(status().isOk());
	}

	@Test
	void otherApiPathsAreProtectedByDefault() throws Exception {
		assertError(mockMvc.perform(get("/api/unimplemented")),
				401, "Authentication is required", "/api/unimplemented");
	}

	@Test
	void authenticatedUnexpectedRoutesCannotProduceSuccessfulResponses() throws Exception {
		mockMvc.perform(get("/api/unimplemented").session(authenticatedSession()))
				.andExpect(result -> assertTrue(Set.of(403, 404).contains(result.getResponse().getStatus())));
	}

	@Test
	void permitsExactDevelopmentOriginPreflightWithCredentialsAndCsrfHeader() throws Exception {
		mockMvc.perform(options(LOGIN_PATH).header(HttpHeaders.ORIGIN, "http://localhost:3000")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-xsrf-token"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("x-xsrf-token")));
		verifyNoInteractions(userRepository, authService);
	}

	@Test
	void rejectsUnconfiguredOriginPreflight() throws Exception {
		mockMvc.perform(options(LOGIN_PATH).header(HttpHeaders.ORIGIN, "https://untrusted.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-xsrf-token"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	@Test
	void realBrowserLoginIncludesCredentialedCorsResponse() throws Exception {
		knownAccount();
		CsrfToken token = csrf(null);
		mockMvc.perform(withCsrf(post(LOGIN_PATH), token)
				.header(HttpHeaders.ORIGIN, "http://localhost:3000")
				.contentType(MediaType.APPLICATION_JSON).content(loginJson(EMAIL, PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	@Test
	void doesNotLogLoginPasswordHashOrSessionCookie(CapturedOutput output) throws Exception {
		knownAccount();
		MvcResult login = performLogin(EMAIL, PASSWORD, null, csrf(null))
				.andExpect(status().isOk()).andReturn();
		String sessionId = login.getRequest().getSession(false).getId();
		assertFalse(output.getAll().contains(PASSWORD));
		assertFalse(output.getAll().contains(passwordHash));
		assertFalse(output.getAll().contains("JSESSIONID=" + sessionId));
	}

	private void assertRegistrationAcceptsPassword(String password) throws Exception {
		User account = account();
		when(authService.register(any(RegisterRequest.class))).thenReturn(account);
		mockMvc.perform(withCsrf(post(REGISTER_PATH), csrf(null)).contentType(MediaType.APPLICATION_JSON)
				.content(REGISTER_JSON.replace("ExamplePassword123", password)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$", aMapWithSize(3)))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
		verify(authService).register(new RegisterRequest("Example User", EMAIL, password));
		verifyNoInteractions(userRepository);
	}

	private void assertLoginAcceptsPassword(String password) throws Exception {
		passwordHash = passwordEncoder.encode(password);
		knownAccount();
		performLogin(EMAIL, password, null, csrf(null))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", aMapWithSize(3)))
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
		verify(userRepository).findByEmail(EMAIL);
		verifyNoInteractions(authService);
	}

	private void assertPasswordByteLimitFailure(String path, String password) throws Exception {
		String body = REGISTER_PATH.equals(path)
				? REGISTER_JSON.replace("ExamplePassword123", password) : loginJson(EMAIL, password);
		mockMvc.perform(withCsrf(post(path), csrf(null)).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$", aMapWithSize(6)))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.path").value(path))
				.andExpect(jsonPath("$.fieldErrors.password").value("Password must be at most 72 UTF-8 bytes"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
		verifyNoInteractions(userRepository, authService);
	}

	private CsrfToken csrf(MockHttpSession session) throws Exception {
		MockHttpServletRequestBuilder request = get(CSRF_PATH);
		if (session != null) {
			request.session(session);
		}
		MvcResult result = mockMvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.headerName").value(CSRF_HEADER))
				.andReturn();
		Cookie csrfCookie = result.getResponse().getCookie(CSRF_COOKIE);
		assertNotNull(csrfCookie);
		assertEquals("Lax", csrfCookie.getAttribute("SameSite"));
		mockMvc.perform(get(CSRF_PATH).cookie(csrfCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value(csrfCookie.getValue()));
		return new CsrfToken(csrfCookie);
	}

	private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request, CsrfToken token) {
		return request.cookie(token.cookie()).header(CSRF_HEADER, token.cookie().getValue());
	}

	private ResultActions performLogin(String email, String password, MockHttpSession session, CsrfToken csrf)
			throws Exception {
		MockHttpServletRequestBuilder request = withCsrf(post(LOGIN_PATH), csrf)
				.contentType(MediaType.APPLICATION_JSON).content(loginJson(email, password));
		if (session != null) {
			request.session(session);
		}
		return mockMvc.perform(request);
	}

	private String loginJson(String email, String password) {
		return """
				{"email":"%s","password":"%s"}
				""".formatted(email, password);
	}

	private User account() {
		User account = mock(User.class);
		when(account.getId()).thenReturn(7L);
		when(account.getFullName()).thenReturn("Example User");
		when(account.getEmail()).thenReturn(EMAIL);
		when(account.getPasswordHash()).thenReturn(passwordHash);
		return account;
	}

	private void knownAccount() {
		User account = account();
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(account));
	}

	private MockHttpSession authenticatedSession() throws Exception {
		knownAccount();
		MvcResult result = performLogin(EMAIL, PASSWORD, null, csrf(null))
				.andExpect(status().isOk()).andReturn();
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		assertNotNull(session);
		return session;
	}

	private ResultActions assertError(ResultActions result, int expectedStatus, String message, String path)
			throws Exception {
		return result.andExpect(status().is(expectedStatus))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", aMapWithSize(6)))
				.andExpect(jsonPath("$.status").value(expectedStatus))
				.andExpect(jsonPath("$.message").value(message))
				.andExpect(jsonPath("$.path").value(path))
				.andExpect(jsonPath("$.fieldErrors", aMapWithSize(0)))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.stackTrace").doesNotExist());
	}

	private record CsrfToken(Cookie cookie) {
	}
}
