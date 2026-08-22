package com.phenoai.user.dto;

import com.phenoai.user.domain.UserProfile;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String email,
    String name,
    String bio,
    String organization,
    String role,
    Instant createdAt,
    Instant updatedAt
) {
    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
            profile.getId(),
            profile.getEmail(),
            profile.getName(),
            profile.getBio(),
            profile.getOrganization(),
            profile.getRole().name(),
            profile.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }
}