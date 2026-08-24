package com.phenoai.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank @Size(max = 150)
    String name,

    @Size(max = 1000)
    String bio,

    @Size(max = 255)
    String organization
) {}
