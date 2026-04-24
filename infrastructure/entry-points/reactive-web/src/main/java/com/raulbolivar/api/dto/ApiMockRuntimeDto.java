package com.raulbolivar.api.dto;

public record ApiMockRuntimeDto(
        String scriptsDirectory,
        boolean autoLoadScripts
) {
}

