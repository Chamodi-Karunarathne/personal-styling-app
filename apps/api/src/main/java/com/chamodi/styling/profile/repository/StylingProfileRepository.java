package com.chamodi.styling.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chamodi.styling.profile.entity.StylingProfile;

public interface StylingProfileRepository extends JpaRepository<StylingProfile, Long> {
}
