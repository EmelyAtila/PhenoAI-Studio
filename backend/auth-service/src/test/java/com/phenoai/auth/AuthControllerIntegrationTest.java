package com.phenoai.auth;

import com.phenoai.auth.dto.LoginRequest;
import com.phenoai.auth.dto.RegisterRequest;
import com.phenoai.shared.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.junit.jupiter.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRegisterAndReturnTokens() {
        RegisterRequest request = new RegisterRequest("integration@test.com", "Senha123!");

        var response = restTemplate.postForEntity("/auth/register", request, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().success()).isTrue();
    }

    @Test
    void shouldRejectDuplicateEmailOnRegister() {
        RegisterRequest request = new RegisterRequest("duplicate@test.com", "Senha123!");
        restTemplate.postForEntity("/auth/register", request, ApiResponse.class); // primeiro cadastro

        var response = restTemplate.postForEntity("/auth/register", request, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldLoginSuccessfullyAfterRegister() {
        RegisterRequest register = new RegisterRequest("login-ok@test.com", "Senha123!");
        restTemplate.postForEntity("/auth/register", register, ApiResponse.class);

        LoginRequest login = new LoginRequest("login-ok@test.com", "Senha123!");
        var response = restTemplate.postForEntity("/auth/login", login, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().success()).isTrue();
    }

    @Test
    void shouldRejectLoginWithWrongPassword() {
        RegisterRequest register = new RegisterRequest("login@test.com", "Senha123!");
        restTemplate.postForEntity("/auth/register", register, ApiResponse.class);

        LoginRequest login = new LoginRequest("login@test.com", "senha-errada");
        var response = restTemplate.postForEntity("/auth/login", login, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectRefreshWithMalformedToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer token-invalido");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        var response = restTemplate.exchange("/auth/refresh", HttpMethod.POST, request, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
