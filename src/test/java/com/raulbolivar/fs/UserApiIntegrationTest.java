package com.raulbolivar.fs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class UserApiIntegrationTest {

    private static final String DB_NAME = "user-api-test-" + UUID.randomUUID();
    private static final Path SCRIPT_DIR = Path.of("build", "test-scripts", UUID.randomUUID().toString()).toAbsolutePath();
    private static final String AUTH_TOKEN = "test-token";
    private static final String SOURCE_BANK = "bank-test";
    private static final String APPLICATION_ID = "app-test";

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:" + DB_NAME + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        registry.add("user-mocks.scripts-dir", () -> SCRIPT_DIR.toString());
        registry.add("app-security.mock-bearer-token", () -> AUTH_TOKEN);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetStorage() throws IOException {
        webTestClient = webTestClient.mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + AUTH_TOKEN)
                .defaultHeader("Source-Bank", SOURCE_BANK)
                .defaultHeader("Application-Id", APPLICATION_ID)
                .build();

        jdbcTemplate.update("DELETE FROM user_mocks");
        if (Files.exists(SCRIPT_DIR)) {
            try (var walk = Files.walk(SCRIPT_DIR)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            if (!path.equals(SCRIPT_DIR)) {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                    // Best effort cleanup for test temp files.
                                }
                            }
                        });
            }
        }
        Files.createDirectories(SCRIPT_DIR);
    }

    @Test
    void shouldExecuteCrudFlowWithExpectedStatuses() {
        webTestClient.post()
                .uri("/api/user-mocks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "alpha-user",
                          "description": "created from test"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", "/api/user-mocks/\\d+")
                .expectBody()
                .jsonPath("$.id").isNumber()
                .jsonPath("$.name").isEqualTo("alpha-user");

        long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM user_mocks", Long.class);

        webTestClient.get()
                .uri("/api/user-mocks/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo((int) id)
                .jsonPath("$.name").isEqualTo("alpha-user");

        webTestClient.put()
                .uri("/api/user-mocks/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "alpha-user-updated",
                          "description": "updated"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo((int) id)
                .jsonPath("$.name").isEqualTo("alpha-user-updated");

        webTestClient.delete()
                .uri("/api/user-mocks/{id}", id)
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/user-mocks/{id}", id)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturnBadRequestForInvalidIdAndEmptySqlLoadPayload() {
        webTestClient.get()
                .uri("/api/user-mocks/not-a-number")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Path variable 'id' must be numeric");

        webTestClient.post()
                .uri("/api/user-mocks/load-sql")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("SQL script content is required");
    }

    @Test
    void shouldLoadSqlAndReloadScripts() throws IOException {
        webTestClient.post()
                .uri("/api/user-mocks/load-sql")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "sql": "INSERT INTO user_mocks (name, description, created_at, updated_at) VALUES ('sql-user', 'from api', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);",
                          "replaceScripted": true
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("loaded")
                .jsonPath("$.count").isEqualTo(1);

        Files.writeString(SCRIPT_DIR.resolve("001_reload.sql"), """
                INSERT INTO user_mocks (name, description, created_at, updated_at)
                VALUES ('reloaded-user', 'from script dir', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
                """);

        webTestClient.post()
                .uri("/api/user-mocks/reload")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("reloaded")
                .jsonPath("$.count").isEqualTo(1);

        webTestClient.get()
                .uri("/api/user-mocks")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.count").isEqualTo(1)
                .jsonPath("$.items[0].name").isEqualTo("reloaded-user");
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthorizationHeaderIsMissing() {
        webTestClient.mutate()
                .defaultHeaders(headers -> headers.remove(HttpHeaders.AUTHORIZATION))
                .build()
                .get()
                .uri("/api/user-mocks")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Missing Authorization header");
    }

    @Test
    void shouldReturnBadRequestWhenRequiredHeadersAreMissing() {
        webTestClient.mutate()
                .defaultHeaders(headers -> {
                    headers.remove("Source-Bank");
                    headers.remove("Application-Id");
                })
                .build()
                .get()
                .uri("/api/user-mocks")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Missing required header(s): Source-Bank, Application-Id");
    }
}

