package com.phenoai.auth.dto.response;

import com.phenoai.auth.domain.enums.Role;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String email,
        String name,
        Role role
) {}
