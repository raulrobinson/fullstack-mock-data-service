package com.raulbolivar.fs.domain.model;

import java.time.Instant;

public record UserMockDefinition(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
