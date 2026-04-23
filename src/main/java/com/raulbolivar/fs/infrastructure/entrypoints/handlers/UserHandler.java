package com.raulbolivar.fs.infrastructure.entrypoints.handlers;

import com.raulbolivar.fs.application.usecase.UserUseCase;
import com.raulbolivar.fs.infrastructure.entrypoints.dto.UserMockDto;
import com.raulbolivar.fs.infrastructure.entrypoints.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserHandler {

    private final UserUseCase useCase;
    private final UserMapper mapper;

    public Mono<ServerResponse> list(ServerRequest request) {
        return Mono.zip(useCase.list(), useCase.runtimeInfo())
                .flatMap(tuple -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(
                                "items", mapper.toDtoList(tuple.getT1()),
                                "count", tuple.getT1().size(),
                                "runtime", mapper.toDto(tuple.getT2())
                        )))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> get(ServerRequest request) {
        return parseId(request)
                .flatMap(useCase::get)
                .map(mapper::toDto)
                .flatMap(dto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(dto))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(UserMockDto.class)
                .map(mapper::toDomain)
                .flatMap(useCase::create)
                .map(mapper::toDto)
                .flatMap(dto -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Location", "/api/user-mocks/" + dto.id())
                        .bodyValue(dto))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        return parseId(request)
                .flatMap(id -> request.bodyToMono(UserMockDto.class)
                        .map(mapper::toDomain)
                        .flatMap(dto -> useCase.update(id, dto))
                        .map(mapper::toDto)
                        .flatMap(dto -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(dto)))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        return parseId(request)
                .flatMap(id -> useCase.delete(id)
                        .then(ServerResponse.noContent().build()))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> reload(ServerRequest request) {
        return useCase.reloadScriptedMocks()
                .flatMap(count -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("status", "reloaded", "count", count)))
                .onErrorResume(this::handleError);
    }

    public Mono<ServerResponse> loadSql(ServerRequest request) {
        return request.bodyToMono(Map.class)
                .defaultIfEmpty(Map.of())
                .flatMap(body -> {
                    String sqlScript = body.get("scriptSql") instanceof String value ? value : "";
                    if (sqlScript.isBlank() && body.get("sql") instanceof String fallback) {
                        sqlScript = fallback;
                    }
                    if (sqlScript.isBlank() && body.get("script") instanceof String extraFallback) {
                        sqlScript = extraFallback;
                    }

                    boolean replaceScripted = true;
                    if (body.get("replaceScripted") instanceof Boolean raw) {
                        replaceScripted = raw;
                    }

                    return useCase.loadSqlScript(sqlScript, replaceScripted)
                            .flatMap(count -> ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(Map.of("status", "loaded", "count", count)));
                })
                .onErrorResume(this::handleError);
    }

    private Mono<Long> parseId(ServerRequest request) {
        return Mono.fromCallable(() -> Long.parseLong(request.pathVariable("id")))
                .onErrorMap(NumberFormatException.class,
                        error -> new IllegalArgumentException("Path variable 'id' must be numeric"));
    }

    private Mono<ServerResponse> handleError(Throwable error) {
        if (error instanceof UnsupportedMediaTypeStatusException mediaTypeError) {
            return ServerResponse.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(errorBody(mediaTypeError.getMessage()));
        }

        if (error instanceof ServerWebInputException) {
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(errorBody("Invalid request body"));
        }

        if (error instanceof DecodingException) {
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(errorBody("Invalid JSON request body"));
        }

        if (error instanceof IllegalArgumentException) {
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(errorBody(error.getMessage()));
        }

        if (error instanceof NoSuchElementException) {
            return ServerResponse.status(404)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(errorBody(error.getMessage()));
        }

        return ServerResponse.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorBody(error.getMessage()));
    }

    private Map<String, String> errorBody(String message) {
        return Map.of("error", message != null ? message : "Unexpected error");
    }
}
