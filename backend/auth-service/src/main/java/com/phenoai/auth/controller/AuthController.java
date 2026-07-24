package com.phenoai.auth.controller;

import com.phenoai.auth.dto.AuthResponse;
import com.phenoai.auth.dto.LoginRequest;
import com.phenoai.auth.dto.RegisterRequest;
import com.phenoai.auth.service.AuthService;
import com.phenoai.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Cadastro, login e renovação de tokens JWT")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Cadastra um novo usuário")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Email já cadastrado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Payload inválido (validação de campos)")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(
        @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Usuário criado com sucesso", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica um usuário e retorna os tokens JWT")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Email ou senha inválidos"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Payload inválido (validação de campos)")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
        @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o access token a partir de um refresh token válido")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token renovado com sucesso"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh token ausente, malformado, inválido ou expirado")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
        @RequestHeader("Authorization") String authHeader
    ) {
        String refreshToken = authHeader.substring(7);
        AuthResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
