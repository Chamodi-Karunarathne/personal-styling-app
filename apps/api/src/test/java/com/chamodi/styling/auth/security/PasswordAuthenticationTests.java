package com.chamodi.styling.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chamodi.styling.auth.config.PasswordEncoderConfig;
import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PasswordAuthenticationTests {

	@Mock
	private UserRepository userRepository;

	private PasswordEncoder passwordEncoder;
	private ProviderManager authenticationManager;

	@BeforeEach
	void setUp() {
		passwordEncoder = new PasswordEncoderConfig().passwordEncoder();
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(
				new RepositoryUserDetailsService(userRepository));
		provider.setPasswordEncoder(passwordEncoder);
		authenticationManager = new ProviderManager(provider);
	}

	@Test
	void authenticatesRegistrationCompatibleBcryptHashesAndErasesSessionCredentials() {
		String rawPassword = "  ExamplePassword123  ";
		String passwordHash = passwordEncoder.encode(rawPassword);
		User storedUser = new User("Example User", "person@example.com", passwordHash);
		when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.of(storedUser));

		Authentication authentication = authenticate("  Person@Example.COM  ", rawPassword);

		assertTrue(authentication.isAuthenticated());
		assertEquals("person@example.com", authentication.getName());
		AccountPrincipal principal = assertInstanceOf(AccountPrincipal.class, authentication.getPrincipal());
		assertEquals("Example User", principal.getFullName());
		assertEquals("person@example.com", principal.getEmail());
		assertNull(authentication.getCredentials());
		assertNull(principal.getPassword());
		assertEquals(passwordHash, storedUser.getPasswordHash());
		assertFalse(authentication.toString().contains(rawPassword));
		assertFalse(authentication.toString().contains(passwordHash));
		verify(userRepository).findByEmail("person@example.com");
	}

	@Test
	void returnsTheSameGenericFailureForWrongPasswordsAndUnknownAccounts() {
		String passwordHash = passwordEncoder.encode("CorrectPassword123");
		when(userRepository.findByEmail("known@example.com")).thenReturn(
				Optional.of(new User("Example User", "known@example.com", passwordHash)));
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		BadCredentialsException wrongPassword = assertThrows(BadCredentialsException.class,
				() -> authenticate("known@example.com", "WrongPassword123"));
		BadCredentialsException unknownAccount = assertThrows(BadCredentialsException.class,
				() -> authenticate("missing@example.com", "WrongPassword123"));

		assertEquals(wrongPassword.getMessage(), unknownAccount.getMessage());
		assertFalse(wrongPassword.getMessage().contains("known@example.com"));
		assertFalse(unknownAccount.getMessage().contains("missing@example.com"));
		assertFalse(wrongPassword.getMessage().contains(passwordHash));
	}

	@Test
	void erasingPrincipalCredentialsKeepsOnlyTheAccountIdentity() {
		AccountPrincipal principal = new AccountPrincipal(
				42L, "Example User", "person@example.com", "stored-password-hash");

		principal.eraseCredentials();

		assertNull(principal.getPassword());
		assertEquals(42L, principal.getId());
		assertEquals("Example User", principal.getFullName());
		assertEquals("person@example.com", principal.getEmail());
		assertEquals("person@example.com", principal.getUsername());
	}

	@Test
	void principalStringRepresentationDoesNotExposeThePasswordHash() {
		String passwordHash = passwordEncoder.encode("ExamplePassword123");
		AccountPrincipal principal = new AccountPrincipal(
				42L, "Example User", "person@example.com", passwordHash);

		assertFalse(principal.toString().contains(passwordHash));
		assertFalse(principal.toString().contains("ExamplePassword123"));
	}

	private Authentication authenticate(String email, String password) {
		return authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(email, password));
	}

}
