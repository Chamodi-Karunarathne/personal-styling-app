package com.chamodi.styling.profile.appearance.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.chamodi.styling.auth.security.AccountPrincipal;
import com.chamodi.styling.profile.appearance.dto.*;
import com.chamodi.styling.profile.appearance.service.AppearanceProfileService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profile/appearance")
public class AppearanceProfileController {
    private final AppearanceProfileService profiles;
    public AppearanceProfileController(AppearanceProfileService profiles) { this.profiles = profiles; }
    @GetMapping public AppearanceResponse get(@AuthenticationPrincipal AccountPrincipal principal) { return profiles.get(principal.getId()); }
    @PutMapping public AppearanceResponse save(@AuthenticationPrincipal AccountPrincipal principal, @Valid @RequestBody AppearanceWriteRequest request) { return profiles.save(principal.getId(), request); }
    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AccountPrincipal principal, @Valid @RequestBody AppearanceDeleteRequest request) { profiles.delete(principal.getId(), request.version()); }
}
