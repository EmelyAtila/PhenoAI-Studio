package com.phenoai.auth.dto.response;

import java.time.LocalDateTime;

public record ErrorResponse(
        String error,
        String message,
        int status,
        LocalDateTime timestamp
) {}
