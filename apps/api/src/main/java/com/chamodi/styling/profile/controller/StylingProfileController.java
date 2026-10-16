package com.chamodi.styling.profile.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chamodi.styling.auth.security.AccountPrincipal;
import com.chamodi.styling.profile.dto.PreferenceCatalogResponse;
import com.chamodi.styling.profile.dto.ProfileResponse;
import com.chamodi.styling.profile.dto.ProfileWriteRequest;
import com.chamodi.styling.profile.service.StylingProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profile")
public class StylingProfileController {

	private final StylingProfileService profiles;

	public StylingProfileController(StylingProfileService profiles) {
		this.profiles = profiles;
	}

	@GetMapping("/options")
	public PreferenceCatalogResponse options() {
		return profiles.options();
	}

	@GetMapping("/me")
	public ProfileResponse me(@AuthenticationPrincipal AccountPrincipal principal) {
		return profiles.get(principal.getId());
	}

	@PutMapping("/me")
	public ProfileResponse save(@AuthenticationPrincipal AccountPrincipal principal,
			@Valid @RequestBody ProfileWriteRequest request) {
		return profiles.save(principal.getId(), request);
	}
}
