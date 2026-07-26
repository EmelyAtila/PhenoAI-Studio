package com.phenoai.auth.service;

import com.phenoai.auth.domain.Role;
import com.phenoai.auth.domain.User;
import com.phenoai.auth.dto.LoginRequest;
import com.phenoai.auth.dto.RegisterRequest;
import com.phenoai.auth.repository.UserRepository;
import com.phenoai.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenBlacklistService tokenBlacklistService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtService, authenticationManager, passwordEncoder, tokenBlacklistService);
    }

    @Test
    void shouldRegisterNewUserSuccessfully() {
        RegisterRequest request = new RegisterRequest("new@user.com", "Senha123!");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        var response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenEmailAlreadyRegistered() {
        RegisterRequest request = new RegisterRequest("existing@user.com", "Senha123!");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Email já cadastrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenLoginCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest("user@user.com", "wrong-password");
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Email ou senha inválidos"));

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest("user@user.com", "Senha123!");
        User user = User.builder().email(request.email()).password("hashed").role(Role.RESEARCHER).build();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        var response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        String refreshToken = "valid-refresh-token";
        User user = User.builder().email("user@user.com").password("hashed").role(Role.RESEARCHER).build();
        when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtService.extractEmail(refreshToken)).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(refreshToken, user)).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");

        var response = authService.refresh(refreshToken);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo(refreshToken);
    }

    @Test
    void shouldThrowWhenTokenIsNotARefreshToken() {
        String accessToken = "access-token-used-as-refresh";
        when(jwtService.isRefreshToken(accessToken)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(accessToken))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Token inválido");
    }

    @Test
    void shouldLogoutSuccessfully() {
        String token = "valid-access-token";
        Date futureExpiry = new Date(System.currentTimeMillis() + 3_600_000);
        when(jwtService.extractExpiration(token)).thenReturn(futureExpiry);

        authService.logout(token);

        verify(tokenBlacklistService).blacklist(eq(token), anyLong());
    }

    @Test
    void shouldNotBlacklistWhenTokenAlreadyExpired() {
        String token = "already-expired-token";
        Date pastExpiry = new Date(System.currentTimeMillis() - 1_000);
        when(jwtService.extractExpiration(token)).thenReturn(pastExpiry);

        authService.logout(token);

        verify(tokenBlacklistService, never()).blacklist(any(), anyLong());
    }

    @Test
    void shouldThrowWhenRefreshTokenIsExpiredOrInvalid() {
        String refreshToken = "expired-refresh-token";
        User user = User.builder().email("user@user.com").password("hashed").role(Role.RESEARCHER).build();
        when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtService.extractEmail(refreshToken)).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(refreshToken, user)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(refreshToken))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Token expirado ou inválido");
    }
}