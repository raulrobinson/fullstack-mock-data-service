package com.raulbolivar.ports;

import com.raulbolivar.model.UserMockDefinition;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IUsersGateway {

    Mono<List<UserMockDefinition>> list();

    Mono<UserMockDefinition> findById(Long id);

    Mono<UserMockDefinition> create(UserMockDefinition definition);

    Mono<UserMockDefinition> update(Long id, UserMockDefinition definition);

    Mono<Void> delete(Long id);

    Mono<Integer> reloadScriptedMocks();

    Mono<Integer> loadMocksFromSql(String sqlScript, boolean replaceScripted);
}
