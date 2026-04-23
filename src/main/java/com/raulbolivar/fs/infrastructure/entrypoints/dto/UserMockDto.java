package com.raulbolivar.fs.infrastructure.entrypoints.dto;

import java.time.Instant;

public record UserMockDto(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
