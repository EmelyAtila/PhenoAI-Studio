package com.phenoai.auth.controller;

import com.phenoai.auth.dto.request.LoginRequest;
import com.phenoai.auth.dto.request.LogoutRequest;
import com.phenoai.auth.dto.request.RefreshRequest;
import com.phenoai.auth.dto.request.RegisterRequest;
import com.phenoai.auth.dto.response.AuthResponse;
import com.phenoai.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints de autenticação do PhenoAI Studio")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Cadastrar novo usuário")
    @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso")
    @ApiResponse(responseCode = "409", description = "Email já cadastrado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Realizar login")
    @ApiResponse(responseCode = "200", description = "Login realizado com sucesso")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Renovar access token usando refresh token")
    @ApiResponse(responseCode = "200", description = "Token renovado com sucesso")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado")
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @Operation(summary = "Realizar logout — invalida os tokens")
    @ApiResponse(responseCode = "204", description = "Logout realizado com sucesso")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request,
                       @RequestBody(required = false) LogoutRequest logoutRequest) {
        String accessToken = extractBearerToken(request);
        String refreshToken = logoutRequest != null ? logoutRequest.refreshToken() : null;
        authService.logout(accessToken, refreshToken);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
