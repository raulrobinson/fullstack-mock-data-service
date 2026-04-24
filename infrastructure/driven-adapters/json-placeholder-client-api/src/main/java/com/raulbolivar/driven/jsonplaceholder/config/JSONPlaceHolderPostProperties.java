package com.raulbolivar.driven.jsonplaceholder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.client-api.jsonplaceholder.post")
public record JSONPlaceHolderPostProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration responseTimeout,
        Duration readTimeout,
        Duration writeTimeout,
        Integer maxConnections,
        Duration pendingAcquireTimeout
) {
}
