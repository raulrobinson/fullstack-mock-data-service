package com.raulbolivar.api.dto;

import java.time.Instant;

public record UserMockDto(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
