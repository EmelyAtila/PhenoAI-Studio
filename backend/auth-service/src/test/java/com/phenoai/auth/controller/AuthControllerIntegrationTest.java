package com.phenoai.auth.controller;

import com.phenoai.auth.dto.request.LoginRequest;
import com.phenoai.auth.dto.request.LogoutRequest;
import com.phenoai.auth.dto.request.RefreshRequest;
import com.phenoai.auth.dto.request.RegisterRequest;
import com.phenoai.auth.dto.response.AuthResponse;
import com.phenoai.auth.dto.response.ErrorResponse;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("phenoai_test")
            .withUsername("phenoai")
            .withPassword("phenoai_secret");

    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "redis_test_secret");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "redis_test_secret");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void register_ValidRequest_Returns201WithTokens() {
        RegisterRequest request = new RegisterRequest("newuser@test.com", "Password123!", "New User");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().refreshToken()).isNotBlank();
        assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().expiresIn()).isEqualTo(3600L);
        assertThat(response.getBody().email()).isEqualTo("newuser@test.com");
    }

    @Test
    void register_DuplicateEmail_Returns409() {
        RegisterRequest request = new RegisterRequest("dup@test.com", "Password123!", "Dup User");
        restTemplate.postForEntity("/api/v1/auth/register", request, AuthResponse.class);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("CONFLICT");
    }

    @Test
    void register_InvalidEmail_Returns400() {
        RegisterRequest request = new RegisterRequest("not-an-email", "Password123!", "User");

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_ValidCredentials_Returns200WithTokens() {
        RegisterRequest reg = new RegisterRequest("login@test.com", "Password123!", "Login User");
        restTemplate.postForEntity("/api/v1/auth/register", reg, AuthResponse.class);

        LoginRequest login = new LoginRequest("login@test.com", "Password123!");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", login, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().refreshToken()).isNotBlank();
    }

    @Test
    void login_WrongPassword_Returns401() {
        RegisterRequest reg = new RegisterRequest("wrongpwd@test.com", "Password123!", "User");
        restTemplate.postForEntity("/api/v1/auth/register", reg, AuthResponse.class);

        LoginRequest login = new LoginRequest("wrongpwd@test.com", "WrongPassword!");
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", login, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_NonExistentUser_Returns401() {
        LoginRequest login = new LoginRequest("ghost@test.com", "Password123!");

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", login, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_ValidRefreshToken_Returns200WithNewTokens() {
        RegisterRequest reg = new RegisterRequest("refresh@test.com", "Password123!", "User");
        AuthResponse auth = restTemplate.postForEntity(
                "/api/v1/auth/register", reg, AuthResponse.class).getBody();

        RefreshRequest refreshReq = new RefreshRequest(auth.refreshToken());
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/refresh", refreshReq, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().refreshToken()).isNotBlank();
    }

    @Test
    void refresh_InvalidToken_Returns401() {
        RefreshRequest request = new RefreshRequest("completely-invalid-token");

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/refresh", request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_ValidTokens_Returns204() {
        RegisterRequest reg = new RegisterRequest("logout@test.com", "Password123!", "User");
        AuthResponse auth = restTemplate.postForEntity(
                "/api/v1/auth/register", reg, AuthResponse.class).getBody();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + auth.accessToken());
        LogoutRequest logoutReq = new LogoutRequest(auth.refreshToken());
        HttpEntity<LogoutRequest> entity = new HttpEntity<>(logoutReq, headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/auth/logout", HttpMethod.POST, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void refresh_AfterLogout_Returns401() {
        RegisterRequest reg = new RegisterRequest("logoutrefresh@test.com", "Password123!", "User");
        AuthResponse auth = restTemplate.postForEntity(
                "/api/v1/auth/register", reg, AuthResponse.class).getBody();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + auth.accessToken());
        HttpEntity<LogoutRequest> entity = new HttpEntity<>(new LogoutRequest(auth.refreshToken()), headers);
        restTemplate.exchange("/api/v1/auth/logout", HttpMethod.POST, entity, Void.class);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new RefreshRequest(auth.refreshToken()), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
