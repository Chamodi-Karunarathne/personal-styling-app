package com.chamodi.styling.profile.appearance.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.profile.appearance.dto.AppearanceDetails;
import jakarta.persistence.*;

@Entity
@Table(name = "appearance_profiles")
public class AppearanceProfile {
    @Id @Column(name = "user_id") private Long userId;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Version private Long lockVersion;
    // Public revision survives the delete/recreate boundary; it is not an authentication token.
    @Column(nullable = false) private UUID revision;
    @Column(name = "height_cm") private Integer heightCm;
    @Column(name = "garment_size_note", length = 60) private String garmentSizeNote;
    @Column(name = "body_proportions", length = 30) private String bodyProportions;
    @Column(name = "skin_tone", length = 30) private String skinTone;
    @Column(name = "skin_undertone", length = 30) private String skinUndertone;
    @Column(name = "palette_suggestions") private Boolean paletteSuggestions;
    @Column(name = "hair_color", length = 30) private String hairColor;
    @Column(name = "hair_texture", length = 30) private String hairTexture;
    @Column(name = "hair_length", length = 30) private String hairLength;
    @Column(name = "face_shape", length = 30) private String faceShape;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected AppearanceProfile() {}
    public AppearanceProfile(User user) { this.user = user; this.userId = user.getId(); }
    public void apply(AppearanceDetails details, Instant now) {
        now = now.truncatedTo(ChronoUnit.MICROS);
        if (updatedAt != null && !now.isAfter(updatedAt)) now = updatedAt.plus(1, ChronoUnit.MICROS);
        heightCm = details.heightCm() == null ? null : details.heightCm().intValueExact();
        garmentSizeNote = details.garmentSizeNote() == null || details.garmentSizeNote().isBlank() ? null : details.garmentSizeNote().strip();
        bodyProportions = details.bodyProportions(); skinTone = details.skinTone(); skinUndertone = details.skinUndertone();
        paletteSuggestions = details.paletteSuggestions(); hairColor = details.hairColor(); hairTexture = details.hairTexture(); hairLength = details.hairLength(); faceShape = details.faceShape();
        revision = UUID.randomUUID(); if (createdAt == null) createdAt = now; updatedAt = now;
    }
    public AppearanceDetails details() { return new AppearanceDetails(heightCm == null ? null : BigDecimal.valueOf(heightCm), garmentSizeNote, bodyProportions, skinTone, skinUndertone, paletteSuggestions, hairColor, hairTexture, hairLength, faceShape); }
    public UUID getRevision() { return revision; }
    public Long getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    @Override public String toString() { return "AppearanceProfile[REDACTED]"; }
}
