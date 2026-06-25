package com.phenoai.auth.security;

import com.phenoai.auth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
        @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(User user) {
        return buildToken(user, accessTokenExpiration, "access");
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshTokenExpiration, "refresh");
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, User user) {
        return extractEmail(token).equals(user.getEmail()) && !isTokenExpired(token);
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractClaims(token).get("type", String.class));
    }

    private String buildToken(User user, long expirationSeconds, String type) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(user.getEmail())
            .claim("role", user.getRole().name())
            .claim("type", type)
            .issuedAt(new Date(now))
            .expiration(new Date(now + expirationSeconds * 1000))
            .signWith(secretKey)
            .compact();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }
}
