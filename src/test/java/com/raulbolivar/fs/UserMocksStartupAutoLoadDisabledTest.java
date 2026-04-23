package com.raulbolivar.fs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UserMocksStartupAutoLoadDisabledTest {

    private static final String DB_NAME = "startup-disabled-" + UUID.randomUUID();

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:" + DB_NAME + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        registry.add("user-mocks.auto-load-scripts", () -> false);
        registry.add("user-mocks.scripts-dir", () -> "./third-parties/users-mocks/sql");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldNotLoadSeedScriptsWhenAutoLoadIsDisabled() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_mocks", Integer.class);
        assertThat(count).isNotNull();
        assertThat(count).isZero();
    }
}

