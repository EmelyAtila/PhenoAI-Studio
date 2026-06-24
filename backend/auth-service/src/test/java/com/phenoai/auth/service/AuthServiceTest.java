package com.phenoai.auth.service;

import com.phenoai.auth.domain.entity.User;
import com.phenoai.auth.domain.enums.Role;
import com.phenoai.auth.dto.request.LoginRequest;
import com.phenoai.auth.dto.request.RefreshRequest;
import com.phenoai.auth.dto.request.RegisterRequest;
import com.phenoai.auth.dto.response.AuthResponse;
import com.phenoai.auth.exception.EmailAlreadyExistsException;
import com.phenoai.auth.exception.InvalidCredentialsException;
import com.phenoai.auth.exception.InvalidTokenException;
import com.phenoai.auth.repository.UserRepository;
import com.phenoai.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationSeconds", 604800L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void register_Success_ReturnsAuthResponse() {
        RegisterRequest request = new RegisterRequest("user@example.com", "Password123!", "Test User");
        UUID userId = UUID.randomUUID();
        User savedUser = User.builder()
                .id(userId).email("user@example.com")
                .password("$2a$12$hashed").name("Test User")
                .role(Role.RESEARCHER).build();

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateAccessToken(savedUser)).thenReturn("access-jwt");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-jwt");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.role()).isEqualTo(Role.RESEARCHER);
        assertThat(response.refreshToken()).isNotBlank();
        verify(valueOperations).set(startsWith("refresh:"), eq(userId.toString()), any());
    }

    @Test
    void register_DuplicateEmail_ThrowsEmailAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest("dup@example.com", "Pass123!", "User");
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("dup@example.com");
    }

    @Test
    void login_Success_ReturnsAuthResponse() {
        LoginRequest request = new LoginRequest("user@example.com", "Password123!");
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId).email("user@example.com")
                .password("$2a$12$hashed").name("Test User")
                .role(Role.RESEARCHER).enabled(true).build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "$2a$12$hashed")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access-jwt");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-jwt");
        assertThat(response.email()).isEqualTo("user@example.com");
    }

    @Test
    void login_UserNotFound_ThrowsInvalidCredentialsException() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("notfound@example.com", "pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_WrongPassword_ThrowsInvalidCredentialsException() {
        User user = User.builder().email("u@e.com").password("$2a$12$hashed").enabled(true).build();
        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$12$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("u@e.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_DisabledUser_ThrowsInvalidCredentialsException() {
        User user = User.builder().email("u@e.com").password("$2a$12$hashed").enabled(false).build();
        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("u@e.com", "pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_ValidToken_ReturnsNewAuthResponse() {
        String refreshToken = "valid-refresh-uuid";
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId).email("user@example.com").name("User")
                .role(Role.RESEARCHER).build();

        when(valueOperations.get("refresh:" + refreshToken)).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("new-access-jwt");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.refresh(new RefreshRequest(refreshToken));

        assertThat(response.accessToken()).isEqualTo("new-access-jwt");
        verify(redisTemplate).delete("refresh:" + refreshToken);
    }

    @Test
    void refresh_InvalidToken_ThrowsInvalidTokenException() {
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("invalid-token")))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void logout_ValidTokens_BlacklistsAccessAndDeletesRefresh() {
        String accessToken = "valid-jwt";
        String refreshToken = "valid-refresh";

        when(jwtTokenProvider.isValid(accessToken)).thenReturn(true);
        when(jwtTokenProvider.getTokenRemainingSeconds(accessToken)).thenReturn(1800L);

        authService.logout(accessToken, refreshToken);

        verify(valueOperations).set(eq("blacklist:" + accessToken), eq("1"), any());
        verify(redisTemplate).delete("refresh:" + refreshToken);
    }

    @Test
    void logout_NullTokens_DoesNotThrow() {
        authService.logout(null, null);
        verifyNoInteractions(jwtTokenProvider);
    }
}
