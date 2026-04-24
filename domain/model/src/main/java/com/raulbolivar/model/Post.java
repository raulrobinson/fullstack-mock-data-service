package com.raulbolivar.model;

public record Post(
        Long userId,
        Long id,
        String title,
        String body
) {}
