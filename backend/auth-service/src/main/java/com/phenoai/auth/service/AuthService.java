package com.phenoai.auth.service;

import com.phenoai.auth.domain.User;
import com.phenoai.auth.dto.AuthResponse;
import com.phenoai.auth.dto.LoginRequest;
import com.phenoai.auth.dto.RegisterRequest;
import com.phenoai.auth.repository.UserRepository;
import com.phenoai.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        User user = User.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .build();

        userRepository.save(user);

        return AuthResponse.of(
            jwtService.generateAccessToken(user),
            jwtService.generateRefreshToken(user),
            jwtService.getAccessTokenExpiration()
        );
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
            .orElseThrow();

        return AuthResponse.of(
            jwtService.generateAccessToken(user),
            jwtService.generateRefreshToken(user),
            jwtService.getAccessTokenExpiration()
        );
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Token inválido");
        }

        String email = jwtService.extractEmail(refreshToken);

        User user = userRepository.findByEmail(email)
            .orElseThrow();

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new IllegalArgumentException("Token expirado ou inválido");
        }

        return AuthResponse.of(
            jwtService.generateAccessToken(user),
            refreshToken,
            jwtService.getAccessTokenExpiration()
        );
    }
}
