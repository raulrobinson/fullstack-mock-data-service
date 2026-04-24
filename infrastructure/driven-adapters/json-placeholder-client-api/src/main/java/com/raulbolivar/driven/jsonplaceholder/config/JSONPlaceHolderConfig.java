package com.raulbolivar.driven.jsonplaceholder.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.*;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableConfigurationProperties({
        JSONPlaceHolderPostProperties.class,
        JSONPlaceHolderUserProperties.class
})
@RequiredArgsConstructor
public class JSONPlaceHolderConfig {

    private final JSONPlaceHolderPostProperties postProperties;
    private final JSONPlaceHolderUserProperties userProperties;

    @Bean
    public WebClient jsonPlaceHolderUserWebClient() {

        // 🔥 Pool de conexiones (MUY IMPORTANTE)
        ConnectionProvider connectionProvider = ConnectionProvider.builder("jsonplaceholder-pool")
                .maxConnections(userProperties.maxConnections())
                .pendingAcquireTimeout(userProperties.pendingAcquireTimeout())
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) userProperties.connectTimeout().toMillis())
                .responseTimeout(userProperties.responseTimeout())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(
                                userProperties.readTimeout().toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(
                                userProperties.writeTimeout().toMillis(), TimeUnit.MILLISECONDS))
                );

        return WebClient.builder()
                .baseUrl(userProperties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))

                // 🔥 Headers globales
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")

                // 🔥 Filtros de observabilidad
                .filter(logRequest())
                .filter(logResponse())
                .filter(correlationIdFilter())

                // 🔥 Manejo global de errores HTTP
                .filter(errorHandlingFilter())

                .build();
    }

    @Bean
    public WebClient jsonPlaceHolderPostWebClient() {

        // 🔥 Pool de conexiones (MUY IMPORTANTE)
        ConnectionProvider connectionProvider = ConnectionProvider.builder("jsonplaceholder-pool")
                .maxConnections(postProperties.maxConnections())
                .pendingAcquireTimeout(postProperties.pendingAcquireTimeout())
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) postProperties.connectTimeout().toMillis())
                .responseTimeout(postProperties.responseTimeout())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(
                                postProperties.readTimeout().toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(
                                postProperties.writeTimeout().toMillis(), TimeUnit.MILLISECONDS))
                );

        return WebClient.builder()
                .baseUrl(postProperties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))

                // 🔥 Headers globales
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")

                // 🔥 Filtros de observabilidad
                .filter(logRequest())
                .filter(logResponse())
                .filter(correlationIdFilter())

                // 🔥 Manejo global de errores HTTP
                .filter(errorHandlingFilter())

                .build();
    }

    // =========================================================
    // 🔎 OBSERVABILIDAD
    // =========================================================

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.info("➡️ REQUEST: {} {}", request.method(), request.url());
            request.headers().forEach((name, values) ->
                    values.forEach(value -> log.debug("➡️ HEADER: {}={}", name, value))
            );
            return reactor.core.publisher.Mono.just(request);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.info("⬅️ RESPONSE: status={}", response.statusCode());
            return reactor.core.publisher.Mono.just(response);
        });
    }

    // =========================================================
    // 🔗 CORRELATION ID (CLAVE EN MICROSERVICIOS)
    // =========================================================

    private ExchangeFilterFunction correlationIdFilter() {
        return (request, next) -> {
            String correlationId = UUID.randomUUID().toString();

            ClientRequest newRequest = ClientRequest.from(request)
                    .header("X-Correlation-Id", correlationId)
                    .build();

            return next.exchange(newRequest);
        };
    }

    // =========================================================
    // ❌ MANEJO CENTRALIZADO DE ERRORES
    // =========================================================

    private ExchangeFilterFunction errorHandlingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {

            if (response.statusCode().isError()) {
                return response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("❌ ERROR RESPONSE: status={} body={}",
                                    response.statusCode(), body);

                            return reactor.core.publisher.Mono.error(
                                    new RuntimeException("External API error: " + body)
                            );
                        });
            }

            return reactor.core.publisher.Mono.just(response);
        });
    }
}
