package com.raulbolivar.helper.filters;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.raulbolivar.helper.Constants.APPLICATION_ID;
import static com.raulbolivar.helper.Constants.SOURCE_BANK;

@Component
public class RequiredRequestHeadersWebFilter implements WebFilter {

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange,
                                      @NonNull WebFilterChain chain) {
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

    public Mono<Void> writeError(ServerWebExchange exchange,
                                 HttpStatus status,
                                 String message) {
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

