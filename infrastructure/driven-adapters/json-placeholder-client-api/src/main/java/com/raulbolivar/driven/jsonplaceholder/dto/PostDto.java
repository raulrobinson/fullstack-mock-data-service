package com.raulbolivar.driven.jsonplaceholder.dto;

import lombok.Builder;

@Builder
public record PostDto(
        Long userId,
        Long id,
        String title,
        String body
) {}
