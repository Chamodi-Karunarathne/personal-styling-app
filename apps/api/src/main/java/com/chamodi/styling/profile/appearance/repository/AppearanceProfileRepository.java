package com.chamodi.styling.profile.appearance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.chamodi.styling.profile.appearance.entity.AppearanceProfile;

public interface AppearanceProfileRepository extends JpaRepository<AppearanceProfile, Long> {}
