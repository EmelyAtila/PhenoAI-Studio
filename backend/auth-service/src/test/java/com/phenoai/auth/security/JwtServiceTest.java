package com.phenoai.auth.security;

import com.phenoai.auth.domain.Role;
import com.phenoai.auth.domain.User;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // precisa ter pelo menos 32 bytes (HS256 exige chave de 256 bits)
    private static final String SECRET = "test-secret-key-for-jwt-service-unit-tests-1234567890";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600L;
    private static final long REFRESH_TOKEN_EXPIRATION = 604800L;

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
        user = User.builder()
            .email("jwt@test.com")
            .password("hashed")
            .role(Role.RESEARCHER)
            .build();
    }

    @Test
    void shouldGenerateValidAccessToken() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractEmail(token)).isEqualTo(user.getEmail());
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
        assertThat(jwtService.isRefreshToken(token)).isFalse();
    }

    @Test
    void shouldGenerateValidRefreshToken() {
        String token = jwtService.generateRefreshToken(user);

        assertThat(jwtService.isRefreshToken(token)).isTrue();
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void shouldExposeConfiguredAccessTokenExpiration() {
        assertThat(jwtService.getAccessTokenExpiration()).isEqualTo(ACCESS_TOKEN_EXPIRATION);
    }

    @Test
    void shouldThrowWhenTokenIsExpired() {
        // expiration negativa gera um token já vencido no momento da criação,
        // evitando Thread.sleep para simular a passagem do tempo.
        // O parser do jjwt lança ExpiredJwtException antes de qualquer checagem
        // manual de expiração — isTokenValid não retorna false, ele propaga.
        JwtService expiredTokenService = new JwtService(SECRET, -10L, REFRESH_TOKEN_EXPIRATION);
        String expiredToken = expiredTokenService.generateAccessToken(user);

        assertThatThrownBy(() -> expiredTokenService.isTokenValid(expiredToken, user))
            .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shouldInvalidateTokenWhenEmailDoesNotMatchUser() {
        String token = jwtService.generateAccessToken(user);
        User otherUser = User.builder()
            .email("other@test.com")
            .password("hashed")
            .role(Role.RESEARCHER)
            .build();

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }
}
