package com.chamodi.styling.profile.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.profile.model.OnboardingStatus;
import com.chamodi.styling.profile.model.PreferenceCategory;
import com.chamodi.styling.profile.model.StepCode;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "styling_profiles")
public class StylingProfile {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Version
	private Long version;

	@Enumerated(EnumType.STRING)
	@Column(name = "onboarding_status", nullable = false, length = 20)
	private OnboardingStatus onboardingStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "last_visited_step", nullable = false, length = 20)
	private StepCode lastVisitedStep;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@ElementCollection
	@CollectionTable(name = "styling_profile_answers", joinColumns = @JoinColumn(name = "profile_id"),
			uniqueConstraints = @UniqueConstraint(name = "uk_profile_answer", columnNames = { "profile_id", "category" }))
	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 30)
	private Set<PreferenceCategory> answeredCategories = new LinkedHashSet<>();

	@ElementCollection
	@CollectionTable(name = "styling_profile_choices", joinColumns = @JoinColumn(name = "profile_id"),
			uniqueConstraints = @UniqueConstraint(name = "uk_profile_choice", columnNames = { "profile_id", "category", "option_code" }))
	private Set<PreferenceSelection> selections = new LinkedHashSet<>();

	protected StylingProfile() {
	}

	public StylingProfile(User user) {
		this.user = user;
		this.userId = user.getId();
	}

	// Called only after the service validates the full snapshot and its state transition.
	public void apply(OnboardingStatus status, StepCode step, Set<PreferenceCategory> answered,
			Set<PreferenceSelection> choices, Instant savedAt) {
		// PostgreSQL timestamps use microsecond precision; responses and reloads must agree.
		savedAt = savedAt.truncatedTo(ChronoUnit.MICROS);
		if (updatedAt != null && !savedAt.isAfter(updatedAt)) {
			savedAt = updatedAt.plus(1, ChronoUnit.MICROS);
		}
		onboardingStatus = status;
		lastVisitedStep = step;
		answeredCategories.clear();
		answeredCategories.addAll(answered);
		selections.clear();
		selections.addAll(choices);
		if (status == OnboardingStatus.COMPLETED && completedAt == null) {
			completedAt = savedAt;
		}
		if (createdAt == null) {
			createdAt = savedAt;
		}
		// Dirty the owner even for collection-only/no-op saves so every accepted write advances @Version.
		updatedAt = savedAt;
	}

	public Long getUserId() { return userId; }
	public Long getVersion() { return version; }
	public OnboardingStatus getOnboardingStatus() { return onboardingStatus; }
	public StepCode getLastVisitedStep() { return lastVisitedStep; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Instant getCompletedAt() { return completedAt; }
	public Set<PreferenceCategory> getAnsweredCategories() { return Collections.unmodifiableSet(answeredCategories); }
	public Set<PreferenceSelection> getSelections() { return Collections.unmodifiableSet(selections); }
}
