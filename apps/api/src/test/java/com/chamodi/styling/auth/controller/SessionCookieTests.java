package com.chamodi.styling.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import com.chamodi.styling.auth.config.PasswordEncoderConfig;
import com.chamodi.styling.auth.config.SecurityConfig;
import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.repository.UserRepository;
import com.chamodi.styling.auth.security.ApiSecurityErrorHandler;
import com.chamodi.styling.auth.security.RepositoryUserDetailsService;
import com.chamodi.styling.auth.service.AuthService;
import com.chamodi.styling.common.exception.GlobalExceptionHandler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// An embedded servlet container verifies cookies MockMvc does not issue itself.
// Database auto-configuration is excluded only in this isolated test application.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = SessionCookieTests.TestApplication.class,
        properties = { "server.servlet.session.cookie.secure=false", "app.security.frontend-origin=http://localhost:3000" })
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles("session-cookie-test")
class SessionCookieTests {

    private static final String EMAIL = "person@example.com";
    private static final String PASSWORD = "ExamplePassword123";

    @Configuration(proxyBeanMethods = false)
    @Profile("session-cookie-test")
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({ AuthController.class, SecurityConfig.class, PasswordEncoderConfig.class,
            RepositoryUserDetailsService.class, ApiSecurityErrorHandler.class, GlobalExceptionHandler.class })
    static class TestApplication {
    }

    @LocalServerPort
    private int port;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthService authService;

    private HttpClient client;
    private CookieManager cookies;
    private String passwordHash;

    @BeforeEach
    void createBrowserClientAndAccount() {
        cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(10)).build();
        passwordHash = passwordEncoder.encode(PASSWORD);
        User account = mock(User.class);
        when(account.getId()).thenReturn(7L);
        when(account.getFullName()).thenReturn("Example User");
        when(account.getEmail()).thenReturn(EMAIL);
        when(account.getPasswordHash()).thenReturn(passwordHash);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(account));
    }

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void containerIssuesHttpOnlyLaxCookieAndRestoresIdentityAcrossHttpRequests() throws Exception {
        HttpResponse<String> login = login(bootstrap());
        assertEquals(200, login.statusCode());
        assertSessionCookie(cookieHeader(login, "JSESSIONID"), false);
        assertSafeAccount(login);

        // No manually attached HttpSession: the HTTP client sends the issued cookie.
        HttpResponse<String> me = get("/api/auth/me");
        assertEquals(200, me.statusCode());
        assertSafeAccount(me);
    }

    @Test
    void reauthenticationRotatesContainerCookieAndLogoutExpiresIt(CapturedOutput output) throws Exception {
        HttpResponse<String> firstLogin = login(bootstrap());
        assertEquals(200, firstLogin.statusCode());
        String firstSessionId = sessionCookieValue();

        HttpResponse<String> secondLogin = login(bootstrap());
        assertEquals(200, secondLogin.statusCode());
        assertSessionCookie(cookieHeader(secondLogin, "JSESSIONID"), false);
        String secondSessionId = sessionCookieValue();
        // Boolean assertions keep session identifiers out of failure diagnostics too.
        assertTrue(!firstSessionId.equals(secondSessionId), "Authentication must rotate an existing session cookie");
        assertEquals(200, get("/api/auth/me").statusCode());

        JsonNode csrf = bootstrap();
        HttpResponse<String> logout = post("/api/auth/logout", "", csrf);
        assertEquals(204, logout.statusCode());
        assertTrue(logout.body().isEmpty());
        String expiredHeader = cookieHeader(logout, "JSESSIONID");
        assertSessionCookie(expiredHeader, true);
        assertTrue(cookies.getCookieStore().getCookies().stream()
                .noneMatch(cookie -> "JSESSIONID".equals(cookie.getName()) && !cookie.hasExpired()));

        HttpResponse<String> me = get("/api/auth/me");
        assertEquals(401, me.statusCode());
        assertEquals("Authentication is required", jsonMapper.readTree(me.body()).get("message").asString());
        assertFalse(output.getAll().contains(PASSWORD));
        assertFalse(output.getAll().contains(passwordHash));
        assertFalse(output.getAll().contains(firstSessionId));
        assertFalse(output.getAll().contains(secondSessionId));
    }

    private JsonNode bootstrap() throws Exception {
        HttpResponse<String> response = get("/api/auth/csrf");
        assertEquals(200, response.statusCode());
        // A safe request such as /me may already have refreshed the cookie after login.
        response.headers().allValues("Set-Cookie").stream()
                .filter(header -> header.startsWith("XSRF-TOKEN="))
                .forEach(header -> assertTrue(header.toLowerCase(Locale.ROOT).contains("samesite=lax")));
        assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        JsonNode csrf = jsonMapper.readTree(response.body());
        assertEquals("X-XSRF-TOKEN", csrf.get("headerName").asString());
        String token = csrf.get("token").asString();
        assertFalse(token.isBlank());
        assertTrue(cookies.getCookieStore().getCookies().stream()
                .anyMatch(cookie -> "XSRF-TOKEN".equals(cookie.getName())
                        && token.equals(cookie.getValue()) && !cookie.isHttpOnly()));
        return csrf;
    }

    private HttpResponse<String> login(JsonNode csrf) throws Exception {
        return post("/api/auth/login", jsonMapper.writeValueAsString(Map.of("email", EMAIL, "password", PASSWORD)), csrf);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body, JsonNode csrf) throws Exception {
        HttpRequest request = request(path).header("Content-Type", "application/json")
                .header(csrf.get("headerName").asString(), csrf.get("token").asString())
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(10));
    }

    private void assertSafeAccount(HttpResponse<String> response) throws Exception {
        JsonNode account = jsonMapper.readTree(response.body());
        assertEquals(3, account.size());
        assertEquals(7L, account.get("id").asLong());
        assertEquals("Example User", account.get("fullName").asString());
        assertEquals(EMAIL, account.get("email").asString());
        assertFalse(account.has("password"));
        assertFalse(account.has("passwordHash"));
        assertFalse(response.body().contains(PASSWORD));
        assertFalse(response.body().contains(passwordHash));
    }

    private String cookieHeader(HttpResponse<String> response, String name) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(header -> header.startsWith(name + "="))
                .findFirst().orElseThrow(() -> new AssertionError("Expected cookie was not issued"));
    }

    private String sessionCookieValue() {
        return cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> "JSESSIONID".equals(cookie.getName()) && !cookie.hasExpired())
                .map(HttpCookie::getValue).findFirst()
                .orElseThrow(() -> new AssertionError("Session cookie was not retained"));
    }

    private void assertSessionCookie(String header, boolean expired) {
        HttpCookie cookie = HttpCookie.parse(header).getFirst();
        assertEquals("JSESSIONID", cookie.getName());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.getSecure(), "Local HTTP must not issue a Secure-only session cookie");
        assertEquals("/", cookie.getPath());
        assertTrue(cookie.getDomain() == null || cookie.getDomain().isEmpty());
        assertTrue(header.toLowerCase(Locale.ROOT).contains("samesite=lax"));
        if (expired) {
            assertEquals(0L, cookie.getMaxAge());
        }
    }
}
