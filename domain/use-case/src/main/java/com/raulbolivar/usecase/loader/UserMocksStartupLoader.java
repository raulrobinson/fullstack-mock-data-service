package com.raulbolivar.usecase.loader;

import com.raulbolivar.usecase.UserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserMocksStartupLoader implements ApplicationRunner {

    private final UserUseCase userUseCase;

    @Value("${user-mocks.auto-load-scripts:true}")
    private boolean autoLoadScripts;

    @Value("${user-mocks.scripts-dir:./third-parties/users-mocks/sql}")
    private String scriptsDir;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoLoadScripts) {
            log.info("[API-MOCKS] Auto load disabled. Skipping script reload from {}", scriptsDir);
            return;
        }

        try {
            Integer count = userUseCase.reloadScriptedMocks().block();
            log.info("[API-MOCKS] Startup script reload complete. Total records: {} (dir: {})", count, scriptsDir);
        } catch (RuntimeException error) {
            log.error("[API-MOCKS] Startup script reload failed from {}", scriptsDir, error);
            throw error;
        }
    }
}

