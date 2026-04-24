package com.raulbolivar.fs.infrastructure.helpers.filters;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class RequiredRequestHeadersWebFilter implements WebFilter {

    public static final String SOURCE_BANK = "Source-Bank";
    public static final String APPLICATION_ID = "Application-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        List<String> missingHeaders = new ArrayList<>();

        if (isBlank(exchange.getRequest().getHeaders().getFirst(SOURCE_BANK))) {
            missingHeaders.add(SOURCE_BANK);
        }

        if (isBlank(exchange.getRequest().getHeaders().getFirst(APPLICATION_ID))) {
            missingHeaders.add(APPLICATION_ID);
        }

        if (!missingHeaders.isEmpty()) {
            String message = "Missing required header(s): " + String.join(", ", missingHeaders);
            return writeError(exchange, HttpStatus.BAD_REQUEST, message);
        }

        return chain.filter(exchange);
    }

    public Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }

        String safeMessage = message != null ? message : "Unexpected error";
        String body = "{\"error\":\"" + escapeJson(safeMessage) + "\"}";
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer dataBuffer = exchange.getResponse().bufferFactory().wrap(payload);
        return exchange.getResponse().writeWith(Mono.just(dataBuffer));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

