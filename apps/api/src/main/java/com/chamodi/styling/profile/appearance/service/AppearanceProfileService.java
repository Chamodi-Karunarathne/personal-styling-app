package com.chamodi.styling.profile.appearance.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chamodi.styling.auth.repository.UserRepository;
import com.chamodi.styling.profile.appearance.dto.*;
import com.chamodi.styling.profile.appearance.entity.AppearanceProfile;
import com.chamodi.styling.profile.appearance.exception.AppearanceConflictException;
import com.chamodi.styling.profile.appearance.repository.AppearanceProfileRepository;
import com.chamodi.styling.profile.exception.ProfileValidationException;
import jakarta.validation.Validator;

@Service
public class AppearanceProfileService {
    private final AppearanceProfileRepository profiles;
    private final UserRepository users;
    private final AppearanceCatalog catalog;
    private final Validator validator;
    public AppearanceProfileService(AppearanceProfileRepository profiles, UserRepository users, AppearanceCatalog catalog, Validator validator) {
        this.profiles = profiles; this.users = users; this.catalog = catalog; this.validator = validator;
    }
    @Transactional(readOnly = true)
    public AppearanceResponse get(Long ownerId) {
        return profiles.findById(ownerId).map(this::response).orElseGet(() -> new AppearanceResponse(null, AppearanceDetails.empty(), null, catalog.options()));
    }
    @Transactional
    public AppearanceResponse save(Long ownerId, AppearanceWriteRequest request) {
        if (request == null) throw new ProfileValidationException(Map.of("details", "Personal details are required"));
        Map<String, String> errors = new LinkedHashMap<>();
        validator.validate(request).forEach(violation -> errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        if (!errors.isEmpty()) throw new ProfileValidationException(errors);
        catalog.validate(request.details());
        AppearanceProfile profile = profiles.findById(ownerId).orElse(null);
        checkVersion(profile, request.version());
        if (profile == null) profile = new AppearanceProfile(users.findById(ownerId).orElseThrow(() -> new IllegalStateException("Authenticated account is unavailable")));
        profile.apply(request.details(), Instant.now());
        try { return response(profiles.saveAndFlush(profile)); }
        catch (OptimisticLockingFailureException failure) { throw new AppearanceConflictException(); }
        catch (DataIntegrityViolationException failure) {
            for (Throwable cause = failure.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation && "23505".equals(violation.getSQLState())
                        && "appearance_profiles_pkey".equalsIgnoreCase(violation.getConstraintName())) throw new AppearanceConflictException();
            }
            throw failure;
        }
    }
    @Transactional
    public void delete(Long ownerId, UUID expectedVersion) {
        AppearanceProfile profile = profiles.findById(ownerId).orElse(null);
        if (profile == null) return; // Repeating deletion of an absent resource is successful.
        checkVersion(profile, expectedVersion);
        try { profiles.delete(profile); profiles.flush(); }
        catch (OptimisticLockingFailureException failure) { throw new AppearanceConflictException(); }
    }
    private void checkVersion(AppearanceProfile profile, UUID expected) {
        if (profile == null ? expected != null : !Objects.equals(profile.getRevision(), expected)) throw new AppearanceConflictException();
    }
    private AppearanceResponse response(AppearanceProfile profile) { return new AppearanceResponse(profile.getRevision(), profile.details(), profile.getUpdatedAt(), catalog.options()); }
}
