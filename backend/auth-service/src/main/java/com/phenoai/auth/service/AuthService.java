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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationSeconds;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(Role.RESEARCHER)
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        String tokenKey = "refresh:" + request.refreshToken();
        String userId = redisTemplate.opsForValue().get(tokenKey);

        if (userId == null) {
            throw new InvalidTokenException("Refresh token inválido ou expirado");
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new InvalidTokenException("Usuário não encontrado"));

        redisTemplate.delete(tokenKey);
        return buildAuthResponse(user);
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && jwtTokenProvider.isValid(accessToken)) {
            long ttl = jwtTokenProvider.getTokenRemainingSeconds(accessToken);
            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                        "blacklist:" + accessToken, "1", Duration.ofSeconds(ttl));
            }
        }
        if (refreshToken != null) {
            redisTemplate.delete("refresh:" + refreshToken);
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                "refresh:" + refreshToken,
                user.getId().toString(),
                Duration.ofSeconds(refreshTokenExpirationSeconds));

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole());
    }
}
