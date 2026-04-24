package com.raulbolivar.fs.infrastructure.helpers.filters;

import com.raulbolivar.fs.infrastructure.helpers.context.ReactorContextKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class AppWebFilter implements WebFilter {

    public static final String HEADER = ReactorContextKeys.CORRELATION_ID;

    @Value("${dashboard.api.key}")
    private String apiKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String correlationId = extractCorrelationId(exchange);

        if (correlationId == null) {
            correlationId = generateCorrelationId();
        }

        addCorrelationIdToResponse(exchange, correlationId);

        return chain.filter(exchange);
    }

    private String extractCorrelationId(ServerWebExchange exchange) {
        return exchange.getRequest()
                .getHeaders()
                .getFirst(HEADER);
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    private void addCorrelationIdToResponse(ServerWebExchange exchange, String correlationId) {
        exchange.getResponse()
                .getHeaders()
                .add(HEADER, correlationId);
    }
}