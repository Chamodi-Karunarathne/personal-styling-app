package com.chamodi.styling.auth.security;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chamodi.styling.auth.repository.UserRepository;

@Service
public class RepositoryUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public RepositoryUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) {
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		return userRepository.findByEmail(normalizedEmail)
				.map(user -> new AccountPrincipal(user.getId(), user.getFullName(), user.getEmail(), user.getPasswordHash()))
				.orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
	}

}
