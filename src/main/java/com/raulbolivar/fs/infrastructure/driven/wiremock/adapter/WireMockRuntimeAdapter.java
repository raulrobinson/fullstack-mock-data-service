package com.raulbolivar.fs.infrastructure.driven.wiremock.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WireMockRuntimeAdapter {//implements IApiMockRuntimeGateway {

    @Value("${api-mocks.wiremock-port:18080}")
    private int wireMockPort;

    private final ObjectMapper objectMapper;
}
