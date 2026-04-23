package com.raulbolivar.fs.application.usecase;

import com.raulbolivar.fs.domain.model.ApiMockRuntimeInfo;
import com.raulbolivar.fs.domain.model.UserMockDefinition;
import com.raulbolivar.fs.domain.ports.IApiMockRuntimeGateway;
import com.raulbolivar.fs.domain.ports.IUsersGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserUseCase {

    private final IUsersGateway persistenceGateway;
    //private final IApiMockRuntimeGateway runtimeGateway;

    @Value("${user-mocks.scripts-dir:./third-parties/users-mocks/sql}")
    private String scriptsDir;

    @Value("${user-mocks.auto-load-scripts:true}")
    private boolean autoLoadScripts;

    public Mono<List<UserMockDefinition>> list() {
        return persistenceGateway.list()
                .doOnSuccess(list -> log.info("[API-MOCKS] Loaded {} mock definitions", list.size()))
                .doOnError(error -> log.error("[API-MOCKS] Error listing mocks: {}", error.getMessage()));
    }

    public Mono<UserMockDefinition> get(Long id) {
        return persistenceGateway.findById(id)
                .doOnSuccess(mock -> log.info("[API-MOCKS] Loaded mock {}", id))
                .doOnError(error -> log.error("[API-MOCKS] Error loading mock {}: {}", id, error.getMessage()));
    }

    public Mono<UserMockDefinition> create(UserMockDefinition definition) {
        UserMockDefinition normalized = normalizeForSave(definition);
        validate(normalized);

        return persistenceGateway.create(normalized)
                .flatMap(saved -> syncRuntime().thenReturn(saved))
                .doOnSuccess(saved -> log.info("[API-MOCKS] Created mock {}", saved.id()))
                .doOnError(error -> log.error("[API-MOCKS] Error creating mock: {}", error.getMessage()));
    }

    public Mono<UserMockDefinition> update(Long id, UserMockDefinition definition) {
        UserMockDefinition normalized = normalizeForSave(definition);
        validate(normalized);

        return persistenceGateway.update(id, normalized)
                .flatMap(updated -> syncRuntime().thenReturn(updated))
                .doOnSuccess(updated -> log.info("[API-MOCKS] Updated mock {}", id))
                .doOnError(error -> log.error("[API-MOCKS] Error updating mock {}: {}", id, error.getMessage()));
    }

    public Mono<Void> delete(Long id) {
        return persistenceGateway.delete(id)
                .then(syncRuntime())
                .doOnSuccess(ignored -> log.info("[API-MOCKS] Deleted mock {}", id))
                .doOnError(error -> log.error("[API-MOCKS] Error deleting mock {}: {}", id, error.getMessage()));
    }

    public Mono<Integer> reloadScriptedMocks() {
        return persistenceGateway.reloadScriptedMocks()
                .flatMap(count -> syncRuntime().thenReturn(count))
                .doOnSuccess(count -> log.info("[API-MOCKS] Reloaded {} scripted mock records", count))
                .doOnError(error -> log.error("[API-MOCKS] Error reloading scripted mocks: {}", error.getMessage()));
    }

    public Mono<Integer> loadSqlScript(String sqlScript, boolean replaceScripted) {
        String normalized = trimToEmpty(sqlScript);
        if (normalized.isBlank()) {
            return Mono.error(new IllegalArgumentException("SQL script content is required"));
        }

        return persistenceGateway.loadMocksFromSql(normalized, replaceScripted)
                .flatMap(count -> syncRuntime().thenReturn(count))
                .doOnSuccess(count -> log.info("[API-MOCKS] Loaded SQL script into H2. Total records: {}", count))
                .doOnError(error -> log.error("[API-MOCKS] Error loading SQL script: {}", error.getMessage()));
    }

    public Mono<ApiMockRuntimeInfo> runtimeInfo() {
        return Mono.just(new ApiMockRuntimeInfo(
                //runtimeGateway.baseUrl(),
                //runtimeGateway.port(),
                scriptsDir,
                autoLoadScripts
        ));
    }

    public Mono<Void> syncRuntime() {
        return persistenceGateway.list()
                .map(List::size)
                .then();
                //.flatMap(runtimeGateway::syncMocks)
                //.doOnSuccess(ignored -> log.info("[API-MOCKS] WireMock runtime synchronized"))
                //.doOnError(error -> log.error("[API-MOCKS] Error synchronizing runtime: {}", error.getMessage()));
    }

    private UserMockDefinition normalizeForSave(UserMockDefinition definition) {
        return new UserMockDefinition(
                definition.id(),
                trimToEmpty(definition.name()),
                trimToNull(definition.description()),
                definition.createdAt(),
                definition.updatedAt()
        );
    }

    private void validate(UserMockDefinition definition) {
        if (definition.name().isBlank()) {
            throw new IllegalArgumentException("Mock name is required");
        }
    }

    private String ensureLeadingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
