package com.raulbolivar.ports;

import com.raulbolivar.model.UserMockDefinition;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IApiMockRuntimeGateway {

    Mono<Void> syncMocks(List<UserMockDefinition> definitions);

    String baseUrl();

    int port();
}
