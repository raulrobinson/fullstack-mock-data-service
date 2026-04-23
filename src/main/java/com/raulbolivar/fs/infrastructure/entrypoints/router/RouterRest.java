package com.raulbolivar.fs.infrastructure.entrypoints.router;

import com.raulbolivar.fs.infrastructure.entrypoints.handlers.UserHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@RequiredArgsConstructor
public class RouterRest {

    private final UserHandler userHandler;

    @Bean
    RouterFunction<ServerResponse> apiRoutes() {

        return RouterFunctions.route()
                .path("/api", b -> b

                        // ── API Mocks ───────────────────────────────────
                        .GET("/user-mocks", userHandler::list)
                        .GET("/user-mocks/{id}", userHandler::get)
                        .POST("/user-mocks", userHandler::create)
                        .PUT("/user-mocks/{id}", userHandler::update)
                        .DELETE("/user-mocks/{id}", userHandler::delete)
                        .POST("/user-mocks/reload", userHandler::reload)
                        .POST("/user-mocks/load-sql", userHandler::loadSql)
                )
                .build();
    }        
}
