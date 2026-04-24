package com.raulbolivar.fs.infrastructure.helpers.errors;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record ApiError(
        String domain,
        String code,
        String message,
        int status,
        String path,
        String method,
        String traceId,
        String correlationId,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp
) {}
