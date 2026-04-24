package com.raulbolivar.helper.errors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raulbolivar.exception.BusinessException;
import com.raulbolivar.helper.filters.AppWebFilter;
import com.raulbolivar.helper.tracing.TraceIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.time.Instant;

@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalErrorHandler implements WebExceptionHandler {

    private static final String DEFAULT_DOMAIN = "system";
    private static final String DEFAULT_ERROR_CODE = "INTERNAL_ERROR";
    private static final String DEFAULT_ERROR_MESSAGE = "Unexpected server error";

    private final ObjectMapper mapper;
    private final TraceIdUtil traceIdUtil;

    @Override
    public @NonNull Mono<Void> handle(@NonNull ServerWebExchange exchange,
                                      @NonNull Throwable ex) {

        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        ApiError apiError = buildApiError(exchange, ex);

        logError(apiError);

        configureResponse(exchange);

        return writeErrorResponse(exchange, apiError);
    }

    private ApiError buildApiError(ServerWebExchange exchange, Throwable ex) {

        HttpStatus status = determineHttpStatus(ex);
        ErrorDetails errorDetails = extractErrorDetails(ex);

        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        String traceId = traceIdUtil.getTraceId();
        String correlationId = extractCorrelationId(exchange);

        return new ApiError(
                errorDetails.domain(),
                errorDetails.code(),
                errorDetails.message(),
                status.value(),
                path,
                method,
                traceId,
                correlationId,
                Instant.now()
        );
    }

    private HttpStatus determineHttpStatus(Throwable ex) {

        if (ex instanceof ResponseStatusException rse) {
            return rse.getStatusCode() instanceof HttpStatus hs ? hs : HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (ex instanceof BusinessException) {
            return HttpStatus.NOT_FOUND;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private ErrorDetails extractErrorDetails(Throwable ex) {

        if (ex instanceof ResponseStatusException rse) {
            return new ErrorDetails("http", rse.getStatusCode().toString(), rse.getReason() != null ? rse.getReason() : rse.getMessage());
        }

        if (ex instanceof BusinessException business) {
            return new ErrorDetails(
                    business.getDomain(),
                    business.getCode(),
                    business.getMessage()
            );
        }

        return new ErrorDetails(
                DEFAULT_DOMAIN,
                DEFAULT_ERROR_CODE,
                DEFAULT_ERROR_MESSAGE
        );
    }

    private String extractCorrelationId(ServerWebExchange exchange) {
        return exchange.getRequest()
                .getHeaders()
                .getFirst(AppWebFilter.HEADER);
    }

    private void configureResponse(ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, ApiError apiError) {

        exchange.getResponse().setStatusCode(HttpStatus.valueOf(apiError.status()));

        return Mono.fromCallable(() -> mapper.writeValueAsBytes(apiError))
                .flatMap(bytes ->
                        exchange.getResponse().writeWith(
                                Mono.just(exchange.getResponse()
                                        .bufferFactory()
                                        .wrap(bytes))
                        )
                );
    }

    private void logError(ApiError error) {

        boolean is5xx = error.status() >= 500;

        if (is5xx) {
            log.error("""
                    API ERROR
                    domain={}
                    code={}
                    message={}
                    path={}
                    method={}
                    traceId={}
                    correlationId={}
                    """,
                    error.domain(), error.code(), error.message(),
                    error.path(), error.method(), error.traceId(), error.correlationId()
            );
        } else {
            log.warn("[HTTP {}] {} {}", error.status(), error.method(), error.path());
        }
    }

    private record ErrorDetails(String domain, String code, String message) {}
}