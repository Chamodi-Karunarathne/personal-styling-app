package com.chamodi.styling.profile.appearance;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.*;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import com.chamodi.styling.auth.config.*;
import com.chamodi.styling.auth.controller.AuthController;
import com.chamodi.styling.auth.repository.UserRepository;
import com.chamodi.styling.auth.security.*;
import com.chamodi.styling.auth.service.AuthService;
import com.chamodi.styling.common.exception.GlobalExceptionHandler;
import com.chamodi.styling.profile.appearance.controller.AppearanceProfileController;
import com.chamodi.styling.profile.appearance.dto.*;
import com.chamodi.styling.profile.appearance.exception.AppearanceConflictException;
import com.chamodi.styling.profile.appearance.service.*;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@WebMvcTest({ AppearanceProfileController.class, AuthController.class })
@Import({ SecurityConfig.class, PasswordEncoderConfig.class, RepositoryUserDetailsService.class, ApiSecurityErrorHandler.class, GlobalExceptionHandler.class })
@ExtendWith(OutputCaptureExtension.class)
class AppearanceControllerTests {
    private static final String PATH = "/api/profile/appearance";
    @Autowired private MockMvc mvc;
    @Autowired private JsonMapper mapper;
    @MockitoBean private AppearanceProfileService service;
    @MockitoBean private UserRepository users;
    @MockitoBean private AuthService auth;
    @Test void anonymousGetIsSafe401() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Authentication is required")); verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings = { "PUT", "DELETE" })
    void anonymousWritesWithCsrfStillRequireAuthentication(String method) throws Exception {
        mvc.perform(csrf(request(method)).contentType(MediaType.APPLICATION_JSON).content(body(method)))
            .andExpect(status().isUnauthorized()); verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings = { "PUT", "DELETE" })
    void modifyingMethodsRequireCsrfBeforeCallingService(String method) throws Exception {
        mvc.perform(owner(request(method)).contentType(MediaType.APPLICATION_JSON).content(body(method)))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("Invalid or missing CSRF token")); verifyNoInteractions(service);
    }
    @Test void getUsesPrincipalOwnerAndReturnsOnlyAppearanceContract() throws Exception {
        when(service.get(7L)).thenReturn(response());
        mvc.perform(owner(get(PATH).param("userId", "99"))).andExpect(status().isOk())
            .andExpect(jsonPath("$", aMapWithSize(4))).andExpect(jsonPath("$.details", aMapWithSize(10)))
            .andExpect(jsonPath("$.userId").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.options.skinUndertone[3].code").value("UNSURE"));
        verify(service).get(7L); verify(service, never()).get(99L);
    }
    @Test void validPutDelegatesWithoutChangingOwnerOrAuthentication() throws Exception {
        when(service.save(eq(7L), any())).thenReturn(response());
        mvc.perform(owner(csrf(put(PATH))).contentType(MediaType.APPLICATION_JSON).content(body("PUT")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.details.heightCm").value(nullValue())).andExpect(jsonPath("$.email").doesNotExist());
        verify(service).save(7L, new AppearanceWriteRequest(null, AppearanceDetails.empty()));
    }
    @Test void deleteUsesObservedVersionAndReturnsNoBody() throws Exception {
        UUID version = UUID.randomUUID();
        mvc.perform(owner(csrf(delete(PATH))).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(new AppearanceDeleteRequest(version))))
            .andExpect(status().isNoContent()).andExpect(content().string("")); verify(service).delete(7L, version);
    }
    @ParameterizedTest @ValueSource(strings = { "49", "261", "170.5" })
    void invalidHeightHasStructuredFieldValidation(String height) throws Exception {
        mvc.perform(owner(csrf(put(PATH))).contentType(MediaType.APPLICATION_JSON).content(body("PUT").replace("\"heightCm\":null", "\"heightCm\":" + height)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors['details.heightCm']").isString()); verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings = { "age", "weight", "photo", "dateOfBirth", "ownerId" })
    void unapprovedCollectionFieldsAreRejected(String field) throws Exception {
        var body = (ObjectNode) mapper.readTree(body("PUT")); ((ObjectNode) body.get("details")).put(field, "not-supported");
        mvc.perform(owner(csrf(put(PATH))).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Request body must be valid JSON")); verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings = { "PUT", "DELETE" })
    void missingVersionCannotSilentlyOverrideCurrentResource(String method) throws Exception {
        var body = (ObjectNode) mapper.readTree(body(method)); body.remove("version");
        mvc.perform(owner(csrf(request(method))).contentType(MediaType.APPLICATION_JSON).content(body.toString())).andExpect(status().isBadRequest()); verifyNoInteractions(service);
    }
    @Test void conflictAndUnexpectedErrorsAreSafeAndDistinct() throws Exception {
        when(service.save(eq(7L), any())).thenThrow(new AppearanceConflictException());
        mvc.perform(owner(csrf(put(PATH))).contentType(MediaType.APPLICATION_JSON).content(body("PUT")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Your personal details changed elsewhere. Reload before continuing"))
            .andExpect(jsonPath("$.stackTrace").doesNotExist());
        doThrow(new DataIntegrityViolationException("private-sql-details")).when(service).delete(eq(7L), any());
        mvc.perform(owner(csrf(delete(PATH))).contentType(MediaType.APPLICATION_JSON).content(body("DELETE")))
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
    @Test void deleteCorsPreflightRetainsExactOriginAndCredentials() throws Exception {
        mvc.perform(options(PATH).header("Origin", "http://localhost:3000").header("Access-Control-Request-Method", "DELETE")
            .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true")).andExpect(header().string("Access-Control-Allow-Methods", containsString("DELETE")));
        mvc.perform(options(PATH).header("Origin", "https://untrusted.example").header("Access-Control-Request-Method", "DELETE"))
            .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
    @Test void authenticationResponseNeverContainsAppearanceFields() throws Exception {
        mvc.perform(owner(get("/api/auth/me"))).andExpect(status().isOk()).andExpect(jsonPath("$", aMapWithSize(3)))
            .andExpect(jsonPath("$.details").doesNotExist()).andExpect(jsonPath("$.skinTone").doesNotExist());
    }
    @Test void successfulAndRejectedAppearanceBodiesAreNotLogged(CapturedOutput output) throws Exception {
        when(service.save(eq(7L), any())).thenReturn(response());
        String note = "PrivateSizingProbe";
        mvc.perform(owner(csrf(put(PATH))).contentType(MediaType.APPLICATION_JSON).content(body("PUT").replace("\"garmentSizeNote\":null", "\"garmentSizeNote\":\"" + note + "\""))).andExpect(status().isOk());
        mvc.perform(owner(csrf(put(PATH))).contentType(MediaType.APPLICATION_JSON).content(body("PUT").replace("\"garmentSizeNote\":null", "\"garmentSizeNote\":\"" + note.repeat(5) + "\""))).andExpect(status().isBadRequest());
        assertFalse(output.getAll().contains(note));
    }
    private AppearanceResponse response() { return new AppearanceResponse(null, AppearanceDetails.empty(), null, new AppearanceCatalog().options()); }
    private String body(String method) { return mapper.writeValueAsString(method.equals("DELETE") ? new AppearanceDeleteRequest(null) : new AppearanceWriteRequest(null, AppearanceDetails.empty())); }
    private MockHttpServletRequestBuilder request(String method) { return method.equals("PUT") ? put(PATH) : delete(PATH); }
    private MockHttpServletRequestBuilder owner(MockHttpServletRequestBuilder request) {
        var principal = new AccountPrincipal(7L, "Example User", "user@example.com", null);
        return request.with(authentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of())));
    }
    private MockHttpServletRequestBuilder csrf(MockHttpServletRequestBuilder request) throws Exception {
        var bootstrap = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
        Cookie cookie = bootstrap.getCookie("XSRF-TOKEN"); return request.cookie(cookie).header("X-XSRF-TOKEN", cookie.getValue());
    }
}
