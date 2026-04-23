package com.raulbolivar.fs.domain.ports;

import com.raulbolivar.fs.domain.model.UserMockDefinition;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IApiMockRuntimeGateway {

    Mono<Void> syncMocks(List<UserMockDefinition> definitions);

    String baseUrl();

    int port();
}
