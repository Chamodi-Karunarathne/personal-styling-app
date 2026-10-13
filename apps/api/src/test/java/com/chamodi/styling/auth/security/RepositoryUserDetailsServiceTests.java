package com.chamodi.styling.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RepositoryUserDetailsServiceTests {

	@Mock
	private UserRepository userRepository;

	private RepositoryUserDetailsService userDetailsService;

	@BeforeEach
	void setUp() {
		userDetailsService = new RepositoryUserDetailsService(userRepository);
	}

	@Test
	void loadsAccountByTrimmedLowercaseEmail() {
		User user = storedUser("person@example.com");
		when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.of(user));

		AccountPrincipal principal = assertInstanceOf(AccountPrincipal.class,
				userDetailsService.loadUserByUsername("  Person@Example.COM  "));

		verify(userRepository).findByEmail("person@example.com");
		assertEquals(42L, principal.getId());
		assertEquals("Example User", principal.getFullName());
		assertEquals("person@example.com", principal.getEmail());
		assertEquals("person@example.com", principal.getUsername());
		assertEquals("stored-password-hash", principal.getPassword());
	}

	@Test
	void normalizesEmailIndependentlyOfTheDefaultLocale() {
		User user = storedUser("indigo@example.com");
		when(userRepository.findByEmail("indigo@example.com")).thenReturn(Optional.of(user));
		Locale previousLocale = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag("tr-TR"));

			AccountPrincipal principal = assertInstanceOf(AccountPrincipal.class,
					userDetailsService.loadUserByUsername("  INDIGO@EXAMPLE.COM  "));

			assertEquals("indigo@example.com", principal.getEmail());
			verify(userRepository).findByEmail("indigo@example.com");
		} finally {
			Locale.setDefault(previousLocale);
		}
	}

	@Test
	void rejectsAnUnknownEmailWithoutIncludingItInTheFailureMessage() {
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
				() -> userDetailsService.loadUserByUsername("  Missing@Example.COM  "));

		verify(userRepository).findByEmail("missing@example.com");
		assertFalse(exception.getMessage().contains("missing@example.com"));
	}

	private User storedUser(String email) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(42L);
		when(user.getFullName()).thenReturn("Example User");
		when(user.getEmail()).thenReturn(email);
		when(user.getPasswordHash()).thenReturn("stored-password-hash");
		return user;
	}

}
