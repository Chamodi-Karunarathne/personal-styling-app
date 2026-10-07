package com.chamodi.styling.auth.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chamodi.styling.auth.dto.RegisterRequest;
import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.exception.EmailAlreadyExistsException;
import com.chamodi.styling.auth.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User register(RegisterRequest request) {
		String fullName = request.fullName().trim();
		String email = request.email().trim().toLowerCase(Locale.ROOT);

		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyExistsException();
		}

		String passwordHash = passwordEncoder.encode(request.password());
		User user = new User(fullName, email, passwordHash);

		return userRepository.save(user);
	}

}
