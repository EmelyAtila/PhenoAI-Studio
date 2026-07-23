package com.phenoai.auth.controller;

import com.phenoai.auth.dto.AuthResponse;
import com.phenoai.auth.dto.LoginRequest;
import com.phenoai.auth.dto.RegisterRequest;
import com.phenoai.auth.service.AuthService;
import com.phenoai.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
        @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Usuário criado com sucesso", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
        @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
        @RequestHeader("Authorization") String authHeader
    ) {
        String refreshToken = authHeader.substring(7);
        AuthResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
