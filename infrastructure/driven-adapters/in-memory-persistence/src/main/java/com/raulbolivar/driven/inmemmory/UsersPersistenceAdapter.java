package com.raulbolivar.driven.inmemmory;

import com.raulbolivar.model.UserMockDefinition;
import com.raulbolivar.ports.IUsersGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsersPersistenceAdapter implements IUsersGateway {

    private final JdbcTemplate jdbcTemplate;

    @Value("${user-mocks.scripts-dir:./third-parties/users-mocks/sql}")
    private String scriptsDir;

    private final RowMapper<UserMockDefinition> rowMapper = (rs, rowNum) ->
            new UserMockDefinition(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    readInstant(rs.getObject("created_at", LocalDateTime.class)),
                    readInstant(rs.getObject("updated_at", LocalDateTime.class))
            );

    @Override
    public Mono<List<UserMockDefinition>> list() {
        return Mono.fromCallable(() -> jdbcTemplate.query("""
                        SELECT id, name, description, created_at, updated_at
                        FROM user_mocks
                        ORDER BY priority ASC, id ASC
                        """, rowMapper))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UserMockDefinition> findById(Long id) {
        return Mono.fromCallable(() -> {
                    List<UserMockDefinition> result = jdbcTemplate.query("""
                                    SELECT id, name, description, created_at, updated_at
                                    FROM user_mocks
                                    WHERE id = ?
                                    """,
                            rowMapper,
                            id);
                    if (result.isEmpty()) {
                        throw new NoSuchElementException("User mock not found: " + id);
                    }
                    return result.getFirst();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UserMockDefinition> create(UserMockDefinition definition) {
        return Mono.fromCallable(() -> {
                    Instant now = Instant.now();
                    LocalDateTime nowLocal = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
                    jdbcTemplate.update("""
                                    INSERT INTO user_mocks (
                                        name, description, created_at, updated_at
                                    ) VALUES (?, ?, ?, ?)
                                    """,
                            definition.name(),
                            definition.description(),
                            nowLocal,
                            nowLocal
                    );

                    Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM user_mocks", Long.class);
                    if (id == null) {
                        throw new IllegalStateException("Failed to create user mock");
                    }
                    return findByIdRequired(id);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UserMockDefinition> update(Long id, UserMockDefinition definition) {
        return Mono.fromCallable(() -> {
                    findByIdRequired(id);

                    int updated = jdbcTemplate.update("""
                                    UPDATE user_mocks
                                    SET name = ?,
                                        description = ?,
                                        updated_at = ?
                                    WHERE id = ?
                                    """,
                            definition.name(),
                            definition.description(),
                            LocalDateTime.now(ZoneOffset.UTC),
                            id
                    );

                    if (updated == 0) {
                        throw new NoSuchElementException("User mock not found: " + id);
                    }
                    return findByIdRequired(id);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> delete(Long id) {
        return Mono.fromRunnable(() -> {
                    int updated = jdbcTemplate.update("DELETE FROM user_mocks WHERE id = ?", id);
                    if (updated == 0) {
                        throw new NoSuchElementException("User mock not found: " + id);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public Mono<Integer> reloadScriptedMocks() {
        return Mono.fromCallable(() -> {
                    Path dir = Path.of(scriptsDir);
                    if (!Files.exists(dir)) {
                        Files.createDirectories(dir);
                        return jdbcTemplate
                                .queryForObject("SELECT COUNT(*) FROM user_mocks", Integer.class);
                    }

                    jdbcTemplate
                            .update("DELETE FROM user_mocks WHERE source = 'script'");

                    try (Stream<Path> stream = Files.list(dir)) {
                        List<Path> scripts = stream
                                .filter(path -> path.getFileName().toString().endsWith(".sql"))
                                .sorted()
                                .toList();

                        for (Path script : scripts) {
                            long baselineMaxId = currentMaxId();
                            String sql = Files.readString(script, StandardCharsets.UTF_8).trim();
                            if (!sql.isBlank()) {
                                jdbcTemplate.execute(sql);
                                markRecentRowsAsScript(baselineMaxId);
                                log.info("[USER-MOCKS] Executed script {}", script.getFileName());
                            }
                        }
                    }

                    Integer count = jdbcTemplate
                            .queryForObject("SELECT COUNT(*) FROM user_mocks", Integer.class);
                    return count != null ? count : 0;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Integer> loadMocksFromSql(String sqlScript, boolean replaceScripted) {
        return Mono.fromCallable(() -> {
                    if (sqlScript == null || sqlScript.isBlank()) {
                        throw new IllegalArgumentException("SQL script content is required");
                    }

                    if (replaceScripted) {
                        jdbcTemplate
                                .update("DELETE FROM user_mocks WHERE source = 'script'");
                    }

                    DataSource dataSource = jdbcTemplate.getDataSource();
                    if (dataSource == null) {
                        throw new IllegalStateException("DataSource is not available");
                    }

                    Connection connection = DataSourceUtils.getConnection(dataSource);
                    try {
                        long baselineMaxId = currentMaxId();
                        ScriptUtils.executeSqlScript(connection, new ByteArrayResource(sqlScript.getBytes(StandardCharsets.UTF_8)));
                        markRecentRowsAsScript(baselineMaxId);
                    } finally {
                        DataSourceUtils.releaseConnection(connection, dataSource);
                    }

                    Integer count = jdbcTemplate
                            .queryForObject("SELECT COUNT(*) FROM user_mocks", Integer.class);
                    return count != null ? count : 0;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private UserMockDefinition findByIdRequired(Long id) {
        List<UserMockDefinition> result = jdbcTemplate.query("""
                        SELECT id, name, description, created_at, updated_at
                        FROM user_mocks
                        WHERE id = ?
                        """,
                rowMapper,
                id);
        if (result.isEmpty()) {
            throw new NoSuchElementException("User mock not found: " + id);
        }
        return result.getFirst();
    }

    private long currentMaxId() {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM user_mocks", Long.class);
        return maxId != null ? maxId : 0L;
    }

    private void markRecentRowsAsScript(long baselineMaxId) {
        jdbcTemplate.update("UPDATE user_mocks SET source = 'script' WHERE id > ?", baselineMaxId);
    }

    private Instant readInstant(LocalDateTime value) {
        return value != null ? value.toInstant(ZoneOffset.UTC) : null;
    }
}
