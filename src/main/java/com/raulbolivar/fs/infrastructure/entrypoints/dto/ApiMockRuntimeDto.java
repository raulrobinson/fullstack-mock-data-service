package com.raulbolivar.fs.infrastructure.entrypoints.dto;

public record ApiMockRuntimeDto(
        //String baseUrl,
        //Integer port,
        String scriptsDirectory,
        boolean autoLoadScripts
) {
}

