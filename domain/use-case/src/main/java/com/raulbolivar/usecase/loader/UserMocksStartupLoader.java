package com.raulbolivar.usecase.loader;

import com.raulbolivar.model.user.User;
import com.raulbolivar.ports.IJSONPlaceHolderUserGateway;
import com.raulbolivar.usecase.UserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserMocksStartupLoader implements ApplicationRunner {

    private final UserUseCase userUseCase;
    private final IJSONPlaceHolderUserGateway jsonPlaceHolderUserGateway;

    @Value("${user-mocks.auto-load-scripts:true}")
    private boolean autoLoadScripts;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoLoadScripts) {
            log.info("[API-MOCKS] Auto load disabled. Skipping JSONPlaceholder startup load.");
            return;
        }

        try {
            Integer count = jsonPlaceHolderUserGateway.getUsers()
                    .collectList()
                    .flatMap(this::replaceScriptUsersInH2)
                    .block();

            log.info("[API-MOCKS] Startup JSONPlaceholder load complete. Total records in H2: {}", count);
        } catch (RuntimeException error) {
            log.error("[API-MOCKS] Startup JSONPlaceholder load failed", error);
            throw error;
        }
    }

    private Mono<Integer> replaceScriptUsersInH2(List<User> users) {
        if (users == null || users.isEmpty()) {
            log.warn("[API-MOCKS] JSONPlaceholder returned 0 users. Keeping current H2 data.");
            return Mono.just(0);
        }

        String sql = buildInsertScript(users);
        if (sql.isBlank()) {
            log.warn("[API-MOCKS] No valid JSONPlaceholder users to insert. Keeping current H2 data.");
            return Mono.just(0);
        }

        // replaceScripted=true elimina los registros source='script' antes de recargar.
        return userUseCase.loadSqlScript(sql, true);
    }

    private String buildInsertScript(List<User> users) {
        StringBuilder sql = new StringBuilder();

        for (User user : users) {
            String name = escapeSql(user.name());
            if (name.isBlank()) {
                continue;
            }

            String username = escapeSql(user.username());
            String email = escapeSql(user.email());
            String description = "username=" + username + ", email=" + email;

            sql.append("INSERT INTO user_mocks ")
                    .append("(name, description, priority, source, created_at, updated_at) VALUES (")
                    .append("'").append(name).append("', ")
                    .append("'").append(description).append("', ")
                    .append("100, 'script', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP")
                    .append(");")
                    .append(System.lineSeparator());
        }

        return sql.toString();
    }

    private String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''").trim();
    }
}
